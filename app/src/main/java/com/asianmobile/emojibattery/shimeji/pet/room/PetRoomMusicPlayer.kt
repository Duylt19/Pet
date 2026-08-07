package com.asianmobile.emojibattery.shimeji.pet.room

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.asianmobile.emojibattery.shimeji.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looping room music. The player is created on demand and released whenever playback stops, so
 * a user who never turns music on never pays for a decoder.
 */
@Singleton
class PetRoomMusicPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var player: MediaPlayer? = null

    @Synchronized
    fun play() {
        if (player?.isPlaying == true) return
        val existing = player
        if (existing != null) {
            runCatching { existing.start() }.onFailure { release() }
            if (player != null) return
        }
        player = runCatching {
            MediaPlayer.create(context, R.raw.bgm_pet_room)?.apply {
                isLooping = true
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                start()
            }
        }.getOrNull()
    }

    /** Keeps the decoder so returning to the room resumes instead of restarting the track. */
    @Synchronized
    fun pause() {
        runCatching { player?.takeIf(MediaPlayer::isPlaying)?.pause() }
    }

    @Synchronized
    fun release() {
        runCatching { player?.release() }
        player = null
    }
}
