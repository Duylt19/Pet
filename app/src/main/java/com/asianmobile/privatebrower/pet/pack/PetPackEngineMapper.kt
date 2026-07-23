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
        val timingMultiplier = action.timingMultiplier(safeMultiplier)
        if (source != null) {
            source.toEngineClip(
                timingMultiplier = timingMultiplier,
                motionMultiplier = safeMultiplier
            )
        } else {
            fallbackClip(
                action = action,
                idleFrames = idleFrames,
                walkFrames = walkFrames,
                timingMultiplier = timingMultiplier,
                motionMultiplier = safeMultiplier
            )
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

private fun PetPackManifest.normalizedSourceClip(action: PetAction): PetPackClip? {
    val normalized = when (action) {
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
    return normalized?.let { clip ->
        if (id.startsWith(OWNER_SHIMEJI_PACK_PREFIX)) {
            clip.normalizedOwnerShimejiTiming()
        } else {
            clip
        }
    }
}

private fun fallbackClip(
    action: PetAction,
    idleFrames: List<PetPackFrame>,
    walkFrames: List<PetPackFrame>,
    timingMultiplier: Float,
    motionMultiplier: Float
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
            (frame.durationMillis / timingMultiplier).toLong().coerceAtLeast(MIN_FRAME_MILLIS),
            fallbackVelocity * motionMultiplier
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

private fun PetPackClip.toEngineClip(
    timingMultiplier: Float,
    motionMultiplier: Float
): PetClip = PetClip(
    action = action,
    frames = frames.mapIndexed { index, frame ->
        PetFrame(
            index = index,
            durationMillis = (frame.durationMillis / timingMultiplier).toLong()
                .coerceAtLeast(MIN_FRAME_MILLIS),
            velocity = frame.velocity * motionMultiplier
        )
    },
    loops = loops,
    nextAction = nextAction
)

private fun PetAction.timingMultiplier(speedMultiplier: Float): Float {
    val influence = when (this) {
        PetAction.WALK,
        PetAction.RUN,
        PetAction.CREEP,
        PetAction.CLIMB_WALL,
        PetAction.CLIMB_DOWN,
        PetAction.CLIMB_CEILING,
        PetAction.TALK_WALK -> FULL_SPEED_INFLUENCE

        PetAction.BOUNCE,
        PetAction.TRIP,
        PetAction.JUMP,
        PetAction.DRAGGED -> PHYSICS_SPEED_INFLUENCE

        PetAction.IDLE,
        PetAction.FALL,
        PetAction.SIT,
        PetAction.WINK,
        PetAction.LOOK_UP,
        PetAction.DANGLE,
        PetAction.TALK,
        PetAction.SPECIAL,
        PetAction.SPECIAL_2,
        PetAction.TAPPED,
        PetAction.FLUNG -> EXPRESSIVE_SPEED_INFLUENCE
    }
    return 1f + (speedMultiplier - 1f) * influence
}

private fun PetPackClip.normalizedOwnerShimejiTiming(): PetPackClip {
    val normalizedFrames = when (action) {
        PetAction.IDLE -> frames.take(1).withDurations(OWNER_IDLE_DURATIONS)
        PetAction.BOUNCE -> frames.withDurations(OWNER_BOUNCE_DURATIONS)
        PetAction.WINK -> frames.withDurations(OWNER_WINK_DURATIONS)
        PetAction.TRIP -> frames.withDurations(OWNER_TRIP_DURATIONS)
        PetAction.JUMP -> frames.withDurations(OWNER_JUMP_DURATIONS)
        PetAction.SPECIAL -> frames.withDurations(OWNER_SPECIAL_DURATIONS)
        PetAction.SPECIAL_2 -> frames
            .distinctBy(PetPackFrame::file)
            .withDurations(OWNER_SPECIAL_2_DURATIONS)
        PetAction.TAPPED -> frames.withDurations(OWNER_TAPPED_DURATIONS)
        else -> frames
    }
    return copy(
        frames = normalizedFrames,
        loops = if (action == PetAction.IDLE) true else loops,
        nextAction = if (action == PetAction.IDLE) null else nextAction
    )
}

private fun List<PetPackFrame>.withDurations(durations: List<Long>): List<PetPackFrame> =
    mapIndexed { index, frame ->
        frame.copy(durationMillis = durations.getOrElse(index) { durations.last() })
    }

private const val MIN_SPEED_MULTIPLIER = 0.5f
private const val MAX_SPEED_MULTIPLIER = 1.5f
private const val MIN_FRAME_MILLIS = 16L
private const val FULL_SPEED_INFLUENCE = 1f
private const val PHYSICS_SPEED_INFLUENCE = 0.5f
private const val EXPRESSIVE_SPEED_INFLUENCE = 0.25f
private const val OWNER_SHIMEJI_PACK_PREFIX = "owner.shimeji."
private const val FALL_VELOCITY = 220f
private const val CLIMB_VELOCITY = 36f
private const val CREEP_VELOCITY = 16f
private const val RUN_VELOCITY = 82f
private const val JUMP_HORIZONTAL_VELOCITY = 110f
private const val JUMP_VERTICAL_VELOCITY = -80f
private const val TALK_WALK_VELOCITY = 24f
private val OWNER_IDLE_DURATIONS = listOf(900L)
private val OWNER_BOUNCE_DURATIONS = listOf(220L, 280L)
private val OWNER_WINK_DURATIONS = listOf(350L, 550L)
private val OWNER_TRIP_DURATIONS = listOf(180L, 220L, 180L, 240L)
private val OWNER_JUMP_DURATIONS = listOf(300L)
private val OWNER_SPECIAL_DURATIONS = listOf(320L, 380L, 420L, 500L, 800L)
private val OWNER_SPECIAL_2_DURATIONS = listOf(320L, 360L, 420L, 520L, 800L)
private val OWNER_TAPPED_DURATIONS = listOf(350L, 550L)
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
