package com.asianmobile.emojibattery.shimeji.pet.room

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.asianmobile.emojibattery.shimeji.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Short interface sounds. SoundPool rather than MediaPlayer because these fire on taps: it keeps
 * the samples decoded and can overlap them, so a fast tap does not cut the previous one off.
 */
@Singleton
class PetRoomSoundPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var pool: SoundPool? = null
    private var clickId = 0
    private var flipId = 0
    private val loaded = mutableSetOf<Int>()

    @Synchronized
    fun prepare() {
        if (pool != null) return
        val created = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // USAGE_MEDIA, not SONIFICATION: sonification plays on the system stream,
                    // which most phones keep muted, so the taps were silent while the room
                    // music was audible.
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        created.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(this) { loaded.add(sampleId) }
        }
        clickId = created.load(context, R.raw.sfx_pet_room_click, 1)
        flipId = created.load(context, R.raw.sfx_pet_room_flip, 1)
        pool = created
    }

    /** Every tap in the room that is not a tab. */
    fun playClick() = play(Sample.CLICK)

    /** Switching between My Pet, Food and Room. */
    fun playFlip() = play(Sample.FLIP)

    @Synchronized
    fun release() {
        pool?.release()
        pool = null
        loaded.clear()
    }

    @Synchronized
    private fun play(sample: Sample) {
        // A tap can land before the room reported it resumed, so build the pool on demand
        // rather than dropping the sound and every sound after it.
        prepare()
        val sampleId = when (sample) {
            Sample.CLICK -> clickId
            Sample.FLIP -> flipId
        }
        // Loading is asynchronous; a tap during that window stays silent instead of crackling.
        if (sampleId == 0 || sampleId !in loaded) return
        pool?.play(sampleId, VOLUME, VOLUME, 1, 0, 1f)
    }

    private enum class Sample { CLICK, FLIP }

    private companion object {
        const val MAX_STREAMS = 4
        const val VOLUME = 1f
    }
}
