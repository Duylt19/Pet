package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetClip
import com.asianmobile.privatebrower.pet.engine.PetFrame
import com.asianmobile.privatebrower.pet.engine.PetVector

fun PetPackManifest.toEngineClips(speedMultiplier: Float = 1f): Map<PetAction, PetClip> {
    val safeMultiplier = speedMultiplier.coerceIn(MIN_SPEED_MULTIPLIER, MAX_SPEED_MULTIPLIER)
    val idleFrames = clips.getValue(PetAction.IDLE).frames
    val walkFrames = clips.getValue(PetAction.WALK).frames
    return PetAction.entries.associateWith { action ->
        val source = normalizedSourceClip(action)
        if (source != null) {
            source.toEngineClip(safeMultiplier)
        } else {
            fallbackClip(action, idleFrames, walkFrames, safeMultiplier)
        }
    }
}

internal fun PetPackManifest.toEngineSupportedActions(): Set<PetAction> = buildSet {
    addAll(clips.keys)
    if (PetAction.TALK_WALK !in clips &&
        clips[PetAction.TALK]?.frames.orEmpty().size > 1
    ) {
        add(PetAction.TALK_WALK)
    }
}

private fun PetPackManifest.normalizedSourceClip(action: PetAction): PetPackClip? =
    when (action) {
        PetAction.TALK -> clips[PetAction.TALK]?.let { clip ->
            clip.copy(frames = clip.frames.take(1))
        }

        PetAction.TALK_WALK -> clips[PetAction.TALK_WALK]
            ?: clips[PetAction.TALK]
                ?.takeIf { clip -> clip.frames.size > 1 }
                ?.let { legacyTalk ->
                    legacyTalk.copy(
                        action = PetAction.TALK_WALK,
                        frames = legacyTalk.frames.map { frame ->
                            frame.copy(
                                velocity = frame.velocity.takeUnless {
                                    it == PetVector.Zero
                                } ?: PetVector(x = TALK_WALK_VELOCITY)
                            )
                        }
                    )
                }

        else -> clips[action]
    }

private fun fallbackClip(
    action: PetAction,
    idleFrames: List<PetPackFrame>,
    walkFrames: List<PetPackFrame>,
    speedMultiplier: Float
): PetClip {
    val sourceFrames = when (action) {
        PetAction.WALK,
        PetAction.RUN,
        PetAction.CREEP,
        PetAction.CLIMB_WALL,
        PetAction.CLIMB_DOWN,
        PetAction.CLIMB_CEILING,
        PetAction.JUMP,
        PetAction.TALK_WALK -> walkFrames
        else -> idleFrames
    }
    val frames = sourceFrames.mapIndexed { index, frame ->
        val fallbackVelocity = when (action) {
            PetAction.FALL -> PetVector(y = FALL_VELOCITY)
            PetAction.CLIMB_WALL -> PetVector(y = -CLIMB_VELOCITY)
            PetAction.CLIMB_DOWN -> PetVector(y = CLIMB_VELOCITY)
            PetAction.CLIMB_CEILING -> PetVector(x = CLIMB_VELOCITY)
            PetAction.CREEP -> PetVector(x = CREEP_VELOCITY)
            PetAction.RUN -> PetVector(x = RUN_VELOCITY)
            PetAction.JUMP -> PetVector(x = JUMP_HORIZONTAL_VELOCITY, y = JUMP_VERTICAL_VELOCITY)
            PetAction.TALK_WALK -> PetVector(x = TALK_WALK_VELOCITY)
            else -> frame.velocity
        }
        PetFrame(
            index,
            (frame.durationMillis / speedMultiplier).toLong().coerceAtLeast(MIN_FRAME_MILLIS),
            fallbackVelocity * speedMultiplier
        )
    }
    val isOneShot = action in ONE_SHOT_FALLBACK_ACTIONS
    return PetClip(
        action = action,
        frames = frames,
        loops = !isOneShot,
        nextAction = if (isOneShot) PetAction.WALK else null
    )
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
private const val FALL_VELOCITY = 220f
private const val CLIMB_VELOCITY = 36f
private const val CREEP_VELOCITY = 16f
private const val RUN_VELOCITY = 82f
private const val JUMP_HORIZONTAL_VELOCITY = 110f
private const val JUMP_VERTICAL_VELOCITY = -80f
private const val TALK_WALK_VELOCITY = 24f
private val ONE_SHOT_FALLBACK_ACTIONS = setOf(
    PetAction.BOUNCE,
    PetAction.SIT,
    PetAction.WINK,
    PetAction.LOOK_UP,
    PetAction.DANGLE,
    PetAction.TRIP,
    PetAction.JUMP,
    PetAction.SPECIAL,
    PetAction.SPECIAL_2,
    PetAction.TAPPED
)
