package com.asianmobile.privatebrower.pet.pack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.asianmobile.privatebrower.data.model.OwnerPetCatalogEntry
import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetVector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

class LegacyShimejiPackInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val diskLoader: PetPackDiskLoader
) {
    fun install(entry: OwnerPetCatalogEntry, archive: File): PetPackInstallResult {
        if (archive.length() !in 1..PetPackArchiveExtractor.MAX_ARCHIVE_BYTES) {
            return PetPackInstallResult.Rejected("Local archive exceeds the supported size")
        }
        val root = PetPackInstaller.packStorageRoot(context)
        val destination = PetPackInstaller.installedDirectory(
            root = root,
            id = OwnerPetCatalogEntry.installedPackId(entry.id),
            version = PACK_VERSION
        )
        if (destination.exists()) {
            return try {
                PetPackInstallResult.Installed(diskLoader.load(destination))
            } catch (error: PetPackInstallException) {
                PetPackInstallResult.Rejected(error.message ?: "Installed local pack is invalid")
            }
        }

        val staging = File(root, "$STAGING_DIRECTORY/${UUID.randomUUID()}")
        val unpacked = File(staging, UNPACKED_DIRECTORY)
        return try {
            if (!unpacked.mkdirs()) throw PetPackInstallException("Unable to create staging area")
            val assets = extractFrames(archive, unpacked)
            val manifest = buildManifest(entry, assets)
            File(unpacked, PET_PACK_MANIFEST_FILE).writeText(
                manifest.toJson().toString(),
                Charsets.UTF_8
            )
            val stagedPack = diskLoader.load(unpacked)
            val parent = destination.parentFile
                ?: throw PetPackInstallException("Invalid install destination")
            if (!parent.mkdirs() && !parent.isDirectory) {
                throw PetPackInstallException("Unable to create install destination")
            }
            if (!unpacked.renameTo(destination)) {
                throw PetPackInstallException("Unable to atomically promote local pet pack")
            }
            PetPackInstallResult.Installed(
                stagedPack.copy(source = PetPackSource.Installed(destination.canonicalPath))
            )
        } catch (error: PetPackInstallException) {
            PetPackInstallResult.Rejected(error.message ?: "Local pet pack was rejected")
        } catch (error: IOException) {
            PetPackInstallResult.Rejected("Local pet archive is malformed or incomplete")
        } catch (error: SecurityException) {
            PetPackInstallResult.Failed("Local pet archive cannot be read")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            PetPackInstallResult.Failed("Unable to prepare this local pet")
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun extractFrames(archive: File, destination: File): Map<Int, LegacyFrameAsset> {
        ZipFile(archive).use { zip ->
            val entries = zip.entries().toList()
            if (entries.isEmpty() || entries.size > PetPackArchiveExtractor.MAX_ARCHIVE_ENTRIES) {
                throw PetPackInstallException("Local archive has an invalid entry count")
            }
            if (entries.any { !isSafeLegacyEntry(it) }) {
                throw PetPackInstallException("Local archive contains an unsafe path")
            }
            val selectedNames = LegacyShimejiFrameSelector.select(entries.map(ZipEntry::getName))
                .filterKeys { it in REQUIRED_FRAME_RANGE }
            if (selectedNames.isEmpty()) {
                throw PetPackInstallException("Local archive has no numbered PNG frames")
            }
            val entriesByName = entries.associateBy(ZipEntry::getName)
            var totalBytes = 0L
            val assets = linkedMapOf<Int, LegacyFrameAsset>()
            selectedNames.toSortedMap().forEach { (number, sourceName) ->
                val sourceEntry = entriesByName.getValue(sourceName)
                val target = File(destination, "frames/shime$number.png")
                target.parentFile?.let { parent ->
                    if (!parent.mkdirs() && !parent.isDirectory) {
                        throw PetPackInstallException("Unable to create frame directory")
                    }
                }
                var entryBytes = 0L
                zip.getInputStream(sourceEntry).use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(COPY_BUFFER_BYTES)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            entryBytes += count
                            totalBytes += count
                            if (entryBytes > PetPackArchiveExtractor.MAX_SINGLE_ENTRY_BYTES ||
                                totalBytes > PetPackArchiveExtractor.MAX_UNPACKED_BYTES
                            ) {
                                throw PetPackInstallException("Local archive exceeds extraction limits")
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                normalizeImageToPng(target)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(target.absolutePath, options)
                if (options.outWidth !in 1..PetPackDiskLoader.MAX_IMAGE_SIDE ||
                    options.outHeight !in 1..PetPackDiskLoader.MAX_IMAGE_SIDE ||
                    options.outMimeType != "image/png"
                ) {
                    throw PetPackInstallException("Local frame dimensions or type are invalid")
                }
                assets[number] = LegacyFrameAsset(
                    file = "frames/${target.name}",
                    width = options.outWidth,
                    height = options.outHeight
                )
            }
            return assets
        }
    }

    private fun buildManifest(
        entry: OwnerPetCatalogEntry,
        assets: Map<Int, LegacyFrameAsset>
    ): PetPackManifest {
        val fallbackNumber = assets.keys.minOrNull()
            ?: throw PetPackInstallException("Local archive has no usable frames")
        fun frame(
            requestedNumber: Int,
            duration: Long,
            velocityX: Float = 0f,
            velocityY: Float = 0f
        ): PetPackFrame {
            val asset = assets[requestedNumber] ?: assets.getValue(fallbackNumber)
            return PetPackFrame(
                file = asset.file,
                rect = PetPackFrameRect(0, 0, asset.width, asset.height),
                durationMillis = duration,
                velocity = PetVector(velocityX, velocityY)
            )
        }
        fun clip(
            action: PetAction,
            numbers: List<Int>,
            loops: Boolean,
            nextAction: PetAction? = null,
            duration: Long = 130L,
            velocityX: Float = 0f,
            velocityY: Float = 0f
        ) = PetPackClip(
            action = action,
            loops = loops,
            nextAction = nextAction,
            frames = numbers.map { frame(it, duration, velocityX, velocityY) }
        )

        val clips = listOf(
            clip(PetAction.IDLE, listOf(11, 15, 11, 17), loops = true, duration = 220L),
            clip(PetAction.WALK, listOf(1, 2, 1, 3), loops = true, velocityX = 42f),
            clip(PetAction.FALL, listOf(4), loops = true, velocityY = 220f),
            clip(PetAction.BOUNCE, listOf(18, 19), false, PetAction.WALK),
            clip(PetAction.CLIMB_WALL, listOf(12, 13, 14), true, velocityY = -36f),
            clip(PetAction.CLIMB_CEILING, listOf(23, 24, 25), true, velocityX = 36f),
            clip(PetAction.SIT, listOf(11, 31, 32), false, PetAction.WALK, duration = 260L),
            clip(PetAction.WINK, listOf(15, 17), false, PetAction.WALK, duration = 240L),
            clip(PetAction.CREEP, listOf(20, 21), true, velocityX = 16f),
            clip(PetAction.TRIP, listOf(18, 19, 20), false, PetAction.WALK),
            clip(PetAction.SPECIAL, listOf(1, 38, 39, 40, 41), false, PetAction.WALK, 180L),
            clip(
                PetAction.SPECIAL_2,
                (42..46).toList(),
                false,
                PetAction.WALK,
                duration = 180L
            ),
            clip(PetAction.TAPPED, listOf(15, 17), false, PetAction.IDLE, duration = 180L),
            clip(PetAction.DRAGGED, listOf(5, 6, 7, 8), loops = true),
            clip(PetAction.FLUNG, listOf(22), loops = true)
        ).associateBy(PetPackClip::action)
        val canvasWidth = assets.values.maxOf(LegacyFrameAsset::width)
        val canvasHeight = assets.values.maxOf(LegacyFrameAsset::height)
        val largestSide = maxOf(canvasWidth, canvasHeight)
        return PetPackManifest(
            schemaVersion = PET_PACK_SCHEMA_VERSION,
            id = OwnerPetCatalogEntry.installedPackId(entry.id),
            version = PACK_VERSION,
            name = entry.name.take(PetPackValidator.MAX_NAME_LENGTH),
            author = entry.author?.take(PetPackValidator.MAX_AUTHOR_LENGTH),
            canvas = PetPackCanvas(
                width = canvasWidth.coerceAtMost(PetPackValidator.MAX_CANVAS_SIDE),
                height = canvasHeight.coerceAtMost(PetPackValidator.MAX_CANVAS_SIDE),
                defaultScale = (TARGET_CANVAS_SIDE / largestSide.toFloat()).coerceIn(
                    PetPackValidator.MIN_DEFAULT_SCALE,
                    PetPackValidator.MAX_DEFAULT_SCALE
                )
            ),
            anchor = PetPackAnchor(0.5f, 1f),
            interaction = PetPackInteraction(PetAction.TAPPED),
            clips = clips
        )
    }

    private fun isSafeLegacyEntry(entry: ZipEntry): Boolean {
        val name = entry.name
        return !entry.isDirectory &&
            name.isNotBlank() &&
            name.length <= PetPackArchiveExtractor.MAX_ENTRY_PATH_LENGTH &&
            '/' !in name &&
            '\\' !in name &&
            '\u0000' !in name &&
            name !in setOf(".", "..")
    }

    private fun File.hasPngSignature(): Boolean = inputStream().buffered().use { input ->
        val actual = ByteArray(PNG_SIGNATURE.size)
        input.read(actual) == actual.size && actual.contentEquals(PNG_SIGNATURE)
    }

    private fun normalizeImageToPng(target: File) {
        if (target.hasPngSignature()) return
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.absolutePath, bounds)
        if (bounds.outWidth !in 1..PetPackDiskLoader.MAX_IMAGE_SIDE ||
            bounds.outHeight !in 1..PetPackDiskLoader.MAX_IMAGE_SIDE ||
            bounds.outMimeType != "image/gif"
        ) {
            throw PetPackInstallException("Local frame contains an unsupported image type")
        }
        val bitmap = BitmapFactory.decodeFile(target.absolutePath)
            ?: throw PetPackInstallException("Local frame image cannot be decoded")
        try {
            FileOutputStream(target, false).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                    throw PetPackInstallException("Local frame image cannot be normalized")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val PACK_VERSION = 1
        const val STAGING_DIRECTORY = ".legacy-staging"
        const val UNPACKED_DIRECTORY = "unpacked"
        const val COPY_BUFFER_BYTES = 16 * 1024
        const val TARGET_CANVAS_SIDE = 128f
        const val PNG_QUALITY = 100
        val REQUIRED_FRAME_RANGE = 1..46
        val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A
        )
    }
}

data class LegacyFrameAsset(
    val file: String,
    val width: Int,
    val height: Int
)

object LegacyShimejiFrameSelector {
    private val framePattern = Regex("^shime(\\d+).*\\.png$", RegexOption.IGNORE_CASE)

    fun select(names: List<String>): Map<Int, String> = names
        .mapNotNull { name ->
            val match = framePattern.matchEntire(name) ?: return@mapNotNull null
            val number = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            FrameCandidate(number, name, priority(number, name))
        }
        .groupBy(FrameCandidate::number)
        .mapValues { (_, candidates) ->
            candidates.minWith(compareBy(FrameCandidate::priority, FrameCandidate::name)).name
        }

    private fun priority(number: Int, name: String): Int = when {
        name == "shime$number.png" -> 0
        name.equals("shime$number.png", ignoreCase = true) -> 1
        else -> 2
    }

    private data class FrameCandidate(
        val number: Int,
        val name: String,
        val priority: Int
    )
}

private fun PetPackManifest.toJson(): JSONObject = JSONObject().apply {
    put("schemaVersion", schemaVersion)
    put("id", id)
    put("version", version)
    put("name", name)
    author?.let { put("author", it) }
    put("canvas", JSONObject().apply {
        put("width", canvas.width)
        put("height", canvas.height)
        put("defaultScale", canvas.defaultScale.toDouble())
    })
    put("anchor", JSONObject().apply {
        put("x", anchor.x.toDouble())
        put("y", anchor.y.toDouble())
    })
    put("interaction", JSONObject().put("tapAction", interaction.tapAction.jsonName()))
    put("clips", JSONArray().apply {
        clips.values.forEach { clip ->
            put(JSONObject().apply {
                put("action", clip.action.jsonName())
                put("loop", clip.loops)
                clip.nextAction?.let { put("nextAction", it.jsonName()) }
                put("frames", JSONArray().apply {
                    clip.frames.forEach { frame ->
                        put(JSONObject().apply {
                            put("file", frame.file)
                            put("rect", JSONObject().apply {
                                put("x", frame.rect.x)
                                put("y", frame.rect.y)
                                put("width", frame.rect.width)
                                put("height", frame.rect.height)
                            })
                            put("durationMs", frame.durationMillis)
                            put("velocity", JSONObject().apply {
                                put("x", frame.velocity.x.toDouble())
                                put("y", frame.velocity.y.toDouble())
                            })
                        })
                    }
                })
            })
        }
    })
}

private fun PetAction.jsonName(): String = name.lowercase()
