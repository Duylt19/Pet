package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetClip
import com.asianmobile.privatebrower.pet.engine.PetFrame

fun PetPackManifest.toEngineClips(): Map<PetAction, PetClip> {
    val idleFrames = clips.getValue(PetAction.IDLE).frames
    return PetAction.entries.associateWith { action ->
        val source = clips[action]
        if (source != null) {
            source.toEngineClip()
        } else {
            PetClip(
                action = action,
                frames = idleFrames.mapIndexed { index, frame ->
                    PetFrame(index, frame.durationMillis, frame.velocity)
                },
                loops = action != PetAction.TAPPED,
                nextAction = if (action == PetAction.TAPPED) PetAction.IDLE else null
            )
        }
    }
}

private fun PetPackClip.toEngineClip(): PetClip = PetClip(
    action = action,
    frames = frames.mapIndexed { index, frame ->
        PetFrame(
            index = index,
            durationMillis = frame.durationMillis,
            velocity = frame.velocity
        )
    },
    loops = loops,
    nextAction = nextAction
)
