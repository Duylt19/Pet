package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetClip
import com.asianmobile.privatebrower.pet.engine.PetFrame

fun PetPackManifest.toEngineClips(speedMultiplier: Float = 1f): Map<PetAction, PetClip> {
    val safeMultiplier = speedMultiplier.coerceIn(MIN_SPEED_MULTIPLIER, MAX_SPEED_MULTIPLIER)
    val idleFrames = clips.getValue(PetAction.IDLE).frames
    return PetAction.entries.associateWith { action ->
        val source = clips[action]
        if (source != null) {
            source.toEngineClip(safeMultiplier)
        } else {
            PetClip(
                action = action,
                frames = idleFrames.mapIndexed { index, frame ->
                    PetFrame(
                        index,
                        (frame.durationMillis / safeMultiplier).toLong().coerceAtLeast(MIN_FRAME_MILLIS),
                        frame.velocity * safeMultiplier
                    )
                },
                loops = action != PetAction.TAPPED,
                nextAction = if (action == PetAction.TAPPED) PetAction.IDLE else null
            )
        }
    }
}

private fun PetPackClip.toEngineClip(speedMultiplier: Float): PetClip = PetClip(
    action = action,
    frames = frames.mapIndexed { index, frame ->
        PetFrame(
            index = index,
            durationMillis = (frame.durationMillis / speedMultiplier).toLong()
                .coerceAtLeast(MIN_FRAME_MILLIS),
            velocity = frame.velocity * speedMultiplier
        )
    },
    loops = loops,
    nextAction = nextAction
)

private const val MIN_SPEED_MULTIPLIER = 0.5f
private const val MAX_SPEED_MULTIPLIER = 1.5f
private const val MIN_FRAME_MILLIS = 16L
