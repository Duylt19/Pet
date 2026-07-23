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
    if (id.startsWith(OWNER_SHIMEJI_PACK_PREFIX)) {
        if (PetAction.WINK in clips) add(PetAction.EMOTE)
        if (PetAction.DANGLE in clips) add(PetAction.FLOOR_PLAY)
        if (PetAction.CREEP in clips) add(PetAction.SPRAWL)
        if (PetAction.CLIMB_WALL in clips) add(PetAction.HOLD_WALL)
        if (PetAction.CLIMB_CEILING in clips) add(PetAction.HOLD_CEILING)
    }
}

internal fun <T> Map<PetAction, List<T>>.normalizedRuntimeVisualFrames(
    packId: String
): Map<PetAction, List<T>> {
    if (!packId.startsWith(OWNER_SHIMEJI_PACK_PREFIX)) return this
    return buildMap {
        putAll(this@normalizedRuntimeVisualFrames)
        get(PetAction.WALK)?.firstOrNull()?.let { frame ->
            put(PetAction.IDLE, listOf(frame))
        }
        get(PetAction.WINK)?.let { put(PetAction.EMOTE, it) }
        get(PetAction.DANGLE)?.let { put(PetAction.FLOOR_PLAY, it) }
        get(PetAction.CREEP)?.lastOrNull()?.let { frame ->
            put(PetAction.SPRAWL, listOf(frame))
        }
        get(PetAction.CLIMB_WALL)?.getOrNull(OWNER_HOLD_WALL_FRAME_INDEX)?.let { frame ->
            put(PetAction.HOLD_WALL, listOf(frame))
        }
        get(PetAction.CLIMB_CEILING)
            ?.getOrNull(OWNER_HOLD_CEILING_FRAME_INDEX)
            ?.let { frame -> put(PetAction.HOLD_CEILING, listOf(frame)) }
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

        PetAction.EMOTE -> clips[PetAction.WINK]?.derivedAction(PetAction.EMOTE)
        PetAction.FLOOR_PLAY -> clips[PetAction.DANGLE]?.derivedAction(PetAction.FLOOR_PLAY)
        PetAction.SPRAWL -> clips[PetAction.CREEP]
            ?.frames
            ?.lastOrNull()
            ?.copy(velocity = PetVector.Zero)
            ?.let { frame ->
                PetPackClip(
                    action = PetAction.SPRAWL,
                    loops = true,
                    nextAction = null,
                    frames = listOf(frame)
                )
            }
        PetAction.HOLD_WALL -> clips[PetAction.CLIMB_WALL]
            ?.stationaryFrameAction(PetAction.HOLD_WALL, OWNER_HOLD_WALL_FRAME_INDEX)
        PetAction.HOLD_CEILING -> clips[PetAction.CLIMB_CEILING]
            ?.stationaryFrameAction(PetAction.HOLD_CEILING, OWNER_HOLD_CEILING_FRAME_INDEX)

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

private fun PetPackClip.derivedAction(action: PetAction): PetPackClip = copy(action = action)

private fun PetPackClip.stationaryFrameAction(
    action: PetAction,
    frameIndex: Int
): PetPackClip? = frames.getOrNull(frameIndex)?.let { frame ->
    PetPackClip(
        action = action,
        loops = true,
        nextAction = null,
        frames = listOf(frame.copy(velocity = PetVector.Zero))
    )
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
        PetAction.HOLD_WALL,
        PetAction.HOLD_CEILING,
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
            PetAction.HOLD_WALL,
            PetAction.HOLD_CEILING,
            PetAction.SPRAWL,
            PetAction.FLOOR_PLAY,
            PetAction.EMOTE -> PetVector.Zero
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
        PetAction.EMOTE,
        PetAction.LOOK_UP,
        PetAction.DANGLE,
        PetAction.FLOOR_PLAY,
        PetAction.SPRAWL,
        PetAction.HOLD_WALL,
        PetAction.HOLD_CEILING,
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
        PetAction.EMOTE -> frames.withDurations(OWNER_EMOTE_DURATIONS)
        PetAction.FLOOR_PLAY -> frames.withDurations(OWNER_FLOOR_PLAY_DURATIONS)
        PetAction.SPRAWL -> frames.withDurations(OWNER_SPRAWL_DURATIONS)
        PetAction.HOLD_WALL,
        PetAction.HOLD_CEILING -> frames.withDurations(OWNER_SURFACE_HOLD_DURATIONS)
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
private const val OWNER_HOLD_WALL_FRAME_INDEX = 3
private const val OWNER_HOLD_CEILING_FRAME_INDEX = 2
private val OWNER_IDLE_DURATIONS = listOf(900L)
private val OWNER_BOUNCE_DURATIONS = listOf(220L, 280L)
private val OWNER_WINK_DURATIONS = listOf(350L, 550L)
private val OWNER_EMOTE_DURATIONS = listOf(420L, 680L)
private val OWNER_FLOOR_PLAY_DURATIONS = listOf(420L, 900L, 420L, 900L)
private val OWNER_SPRAWL_DURATIONS = listOf(1_200L)
private val OWNER_SURFACE_HOLD_DURATIONS = listOf(900L)
private val OWNER_TRIP_DURATIONS = listOf(180L, 220L, 180L, 240L)
private val OWNER_JUMP_DURATIONS = listOf(300L)
private val OWNER_SPECIAL_DURATIONS = listOf(420L, 480L, 560L, 680L, 860L)
private val OWNER_SPECIAL_2_DURATIONS = listOf(420L, 480L, 560L, 680L, 860L)
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
