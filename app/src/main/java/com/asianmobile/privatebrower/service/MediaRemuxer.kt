package com.asianmobile.privatebrower.service

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Shared remuxer: copies the encoded video/audio samples from one or more input files into a
 * single playable container — **no re-encode**. Used by every download engine that ends in a
 * mux step (HLS TS/fMP4, DASH separate tracks, MSE capture).
 *
 * Container is chosen from the codecs so more sources produce a valid file instead of failing:
 *  - H.264/H.265 video (+ AAC audio) → **MP4**
 *  - VP8/VP9 video (+ Opus/Vorbis audio) → **WebM**
 *  - When the audio codec doesn't fit the container the video codec dictates, the audio track is
 *    dropped (video-only) as a last resort rather than failing outright.
 *
 * Inputs may each carry a video track, an audio track, or both. The first video track and first
 * audio track found across all inputs are muxed.
 *
 * MPEG-TS AAC arrives as multi-frame ADTS payloads; when muxing such audio into MP4 it is split
 * back into per-sample raw AAC via [AacReframer] (fMP4/DASH raw AAC is left untouched).
 */
class MediaRemuxer {

    data class Result(
        val success: Boolean,
        /** The file actually written — its extension may differ from the requested one. */
        val outputFile: File? = null,
        val message: String? = null,
    )

    /** One extractor with a per-input-track plan and a copy-buffer size. */
    private class Source(
        val extractor: MediaExtractor,
        /** input track index -> plan for that track */
        val trackPlan: Map<Int, TrackPlan>,
        val bufferSize: Int,
    )

    private class TrackPlan(val outTrackIndex: Int, val reframeAac: Boolean)

    fun remux(inputs: List<File>, requestedOutput: File): Result {
        val usable = inputs.filter { it.exists() && it.length() > 0 }
        if (usable.isEmpty()) return Result(false, message = "No input to mux")

        // Probe codecs to decide the output container before creating the muxer.
        val plan = probeTracks(usable) ?: return Result(false, message = "No A/V tracks found")
        val outputFormat = if (plan.container == RemuxContainer.WEBM) {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        } else {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }
        val output = retargetExtension(requestedOutput, plan.container.extension)

        val muxer = MediaMuxer(output.absolutePath, outputFormat)
        val sources = mutableListOf<Source>()
        val reframers = HashMap<Int, AacReframer>()
        try {
            var videoTaken = false
            var audioTaken = !plan.includeAudio
            usable.forEach { file ->
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)
                val trackPlan = HashMap<Int, TrackPlan>()
                var maxBuf = 1 shl 20
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    val isVideo = mime.startsWith("video/")
                    val isAudio = mime.startsWith("audio/")
                    val take = when {
                        isVideo && !videoTaken -> true
                        isAudio && !audioTaken && plan.includeAudio -> true
                        else -> false
                    }
                    if (!take) continue
                    extractor.selectTrack(i)
                    val outIdx = muxer.addTrack(format)
                    val reframe = plan.container == RemuxContainer.MP4 &&
                        mime == MediaFormat.MIMETYPE_AUDIO_AAC
                    trackPlan[i] = TrackPlan(outIdx, reframe)
                    if (reframe) {
                        val rate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        } else 44100
                        reframers[outIdx] = AacReframer(rate)
                    }
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxBuf = maxOf(maxBuf, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                    if (isVideo) videoTaken = true
                    if (isAudio) audioTaken = true
                }
                if (trackPlan.isNotEmpty()) {
                    sources.add(Source(extractor, trackPlan, maxBuf.coerceAtMost(12 shl 20)))
                } else {
                    extractor.release()
                }
            }
            if (sources.isEmpty()) return Result(false, message = "No muxable tracks")

            muxer.start()
            val info = MediaCodec.BufferInfo()
            // Per-output-track PTS shift so the first sample starts at 0. Codec priming (Opus
            // pre-skip, MP4 edit lists) yields negative sample times, and the native WebM muxer
            // aborts the whole process on a negative timestamp — normalize to keep it non-negative.
            val timeOffsets = HashMap<Int, Long>()
            sources.forEach { source ->
                val buffer = ByteBuffer.allocate(source.bufferSize)
                while (true) {
                    val size = source.extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    val trackPlan = source.trackPlan[source.extractor.sampleTrackIndex]
                    if (trackPlan != null) {
                        val reframer = reframers[trackPlan.outTrackIndex]
                        if (reframer != null && isAdts(buffer, size)) {
                            reframer.writeFrames(muxer, trackPlan.outTrackIndex, buffer, size, info)
                        } else {
                            val rawTime = source.extractor.sampleTime
                            val offset = timeOffsets.getOrPut(trackPlan.outTrackIndex) {
                                if (rawTime < 0) -rawTime else 0L
                            }
                            info.offset = 0
                            info.size = size
                            info.presentationTimeUs = (rawTime + offset).coerceAtLeast(0L)
                            info.flags = sampleFlags(source.extractor.sampleFlags)
                            muxer.writeSampleData(trackPlan.outTrackIndex, buffer, info)
                        }
                    }
                    source.extractor.advance()
                }
            }
            muxer.stop()
        } catch (e: Exception) {
            output.delete()
            return Result(false, message = e.message ?: "Remux failed")
        } finally {
            runCatching { muxer.release() }
            sources.forEach { runCatching { it.extractor.release() } }
        }
        return if (output.length() > 0) {
            Result(true, outputFile = output, message = plan.note)
        } else {
            output.delete()
            Result(false, message = "Empty output")
        }
    }

    /** Inspect all inputs' first video + first audio track, then pick a container. */
    private fun probeTracks(inputs: List<File>): RemuxPlan? {
        var videoMime: String? = null
        var audioMime: String? = null
        inputs.forEach { file ->
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.absolutePath)
                for (i in 0 until extractor.trackCount) {
                    val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && videoMime == null) videoMime = mime
                    if (mime.startsWith("audio/") && audioMime == null) audioMime = mime
                }
            } catch (_: Exception) {
                // Ignore an unreadable input; other inputs may still be usable.
            } finally {
                extractor.release()
            }
        }
        return chooseRemuxContainer(videoMime, audioMime)
    }

    private fun sampleFlags(extractorFlags: Int): Int =
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            MediaCodec.BUFFER_FLAG_KEY_FRAME
        } else 0

    /** True when a sample begins with an ADTS sync word (0xFFFx) — i.e. TS-style AAC framing. */
    private fun isAdts(buffer: ByteBuffer, size: Int): Boolean {
        if (size < 7) return false
        val b0 = buffer.get(0).toInt() and 0xFF
        val b1 = buffer.get(1).toInt() and 0xFF
        return b0 == 0xFF && (b1 and 0xF0) == 0xF0
    }

    private fun retargetExtension(file: File, extension: String): File {
        if (file.extension.equals(extension, ignoreCase = true)) return file
        return File(file.parentFile, "${file.nameWithoutExtension}.$extension")
    }

    /**
     * MediaExtractor hands back MPEG-TS AAC as whole PES payloads — several ADTS frames per
     * sample. MP4 expects one raw AAC access unit per sample, so split each payload into its
     * ADTS frames, strip the 7/9-byte ADTS header, and re-timestamp at 1024 samples per frame.
     * Without this the audio track has ~5x too few samples and stalls playback when unmuted.
     */
    private class AacReframer(private val sampleRate: Int) {
        private var frameIndex = 0L

        fun writeFrames(
            muxer: MediaMuxer,
            outTrack: Int,
            buffer: ByteBuffer,
            size: Int,
            info: MediaCodec.BufferInfo,
        ) {
            val data = ByteArray(size)
            buffer.position(0)
            buffer.get(data, 0, size)
            val rate = if (sampleRate > 0) sampleRate else 44100
            var pos = 0
            while (pos + 7 <= size) {
                val b0 = data[pos].toInt() and 0xFF
                val b1 = data[pos + 1].toInt() and 0xFF
                if (b0 != 0xFF || (b1 and 0xF0) != 0xF0) {
                    pos++
                    continue
                }
                val headerLen = if (data[pos + 1].toInt() and 0x01 == 1) 7 else 9
                val frameLen = ((data[pos + 3].toInt() and 0x03) shl 11) or
                    ((data[pos + 4].toInt() and 0xFF) shl 3) or
                    ((data[pos + 5].toInt() and 0xE0) shr 5)
                if (frameLen < headerLen || pos + frameLen > size) break
                val rawLen = frameLen - headerLen
                if (rawLen > 0) {
                    val frame = ByteBuffer.allocate(rawLen)
                    frame.put(data, pos + headerLen, rawLen)
                    frame.position(0)
                    info.offset = 0
                    info.size = rawLen
                    info.presentationTimeUs = frameIndex * 1024L * 1_000_000L / rate
                    info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                    muxer.writeSampleData(outTrack, frame, info)
                    frameIndex++
                }
                pos += frameLen
            }
        }
    }
}

/** Output container for a remux. Extension mirrors the container. */
internal enum class RemuxContainer(val extension: String) { MP4("mp4"), WEBM("webm") }

/** The container + whether the audio track fits it, decided purely from codecs (testable). */
internal data class RemuxPlan(
    val container: RemuxContainer,
    val includeAudio: Boolean,
    val note: String?,
)

/**
 * Choose an output container from the (nullable) video + audio codec MIME types. VP8/VP9 → WebM,
 * everything else → MP4. When the audio codec can't ride in the chosen container it's dropped
 * (video-only) rather than failing. Returns null when there's neither a video nor audio track.
 */
internal fun chooseRemuxContainer(videoMime: String?, audioMime: String?): RemuxPlan? {
    if (videoMime == null && audioMime == null) return null
    val container = when {
        videoMime != null -> if (isWebmVideoMime(videoMime)) RemuxContainer.WEBM else RemuxContainer.MP4
        isWebmAudioMime(audioMime!!) -> RemuxContainer.WEBM
        else -> RemuxContainer.MP4
    }
    val audioFits = audioMime?.let { audioFitsContainer(container, it) } ?: false
    val note = if (audioMime != null && !audioFits) {
        "Audio codec $audioMime incompatible with ${container.name}; saved video-only"
    } else null
    return RemuxPlan(container, includeAudio = audioFits, note = note)
}

internal fun isWebmVideoMime(mime: String): Boolean =
    mime == "video/x-vnd.on2.vp8" || mime == "video/x-vnd.on2.vp9"

internal fun isWebmAudioMime(mime: String): Boolean =
    mime == "audio/opus" || mime == "audio/vorbis"

private fun audioFitsContainer(container: RemuxContainer, mime: String): Boolean = when (container) {
    RemuxContainer.WEBM -> isWebmAudioMime(mime)
    // MP4 reliably carries AAC across all our min-SDK levels; other codecs are best avoided.
    RemuxContainer.MP4 -> mime == "audio/mp4a-latm"
}
