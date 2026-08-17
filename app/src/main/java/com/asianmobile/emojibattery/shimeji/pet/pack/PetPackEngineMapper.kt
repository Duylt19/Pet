package com.asianmobile.emojibattery.shimeji.pet.pack

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetClip
import com.asianmobile.emojibattery.shimeji.pet.engine.PetFrame
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector

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
        if (clips.keys.hasCompactLegacyActionProfile()) {
            addAll(COMPACT_LEGACY_DERIVED_ACTIONS)
        }
        if (PetAction.WINK in clips) add(PetAction.EMOTE)
        if (PetAction.DANGLE in clips) add(PetAction.FLOOR_PLAY)
        if (PetAction.CREEP in clips) add(PetAction.SPRAWL)
        if (PetAction.CLIMB_WALL in clips) add(PetAction.HOLD_WALL)
        if (PetAction.CLIMB_CEILING in clips) add(PetAction.HOLD_CEILING)
    }
}

internal fun PetPackManifest.usesDerivedCeilingVisual(): Boolean =
    id.startsWith(OWNER_SHIMEJI_PACK_PREFIX) &&
        PetAction.CLIMB_CEILING !in clips &&
        clips.keys.hasCompactLegacyActionProfile()

internal fun <T> Map<PetAction, List<T>>.normalizedRuntimeVisualFrames(
    packId: String
): Map<PetAction, List<T>> {
    if (!packId.startsWith(OWNER_SHIMEJI_PACK_PREFIX)) return this
    return buildMap {
        putAll(this@normalizedRuntimeVisualFrames)
        if (keys.hasCompactLegacyActionProfile()) {
            get(PetAction.DRAGGED)?.let { dragged ->
                putIfAbsent(PetAction.JUMP, dragged.take(1))
                putIfAbsent(PetAction.FLUNG, dragged)
                get(PetAction.BOUNCE)?.let { bounce ->
                    putIfAbsent(PetAction.CREEP, bounce)
                    putIfAbsent(PetAction.FLOOR_PLAY, bounce)
                    bounce.lastOrNull()?.let { frame ->
                        putIfAbsent(PetAction.SPRAWL, listOf(frame))
                    }
                    putIfAbsent(PetAction.TRIP, dragged.take(2) + bounce)
                }
            }
            get(PetAction.CLIMB_WALL)?.let { wall ->
                putIfAbsent(PetAction.CLIMB_CEILING, wall)
                (wall.getOrNull(OWNER_HOLD_WALL_FRAME_INDEX) ?: wall.lastOrNull())
                    ?.let { frame ->
                        putIfAbsent(PetAction.HOLD_CEILING, listOf(frame))
                    }
            }
            get(PetAction.SPECIAL)?.let { special ->
                putIfAbsent(PetAction.SIT, special.drop(1).take(1).ifEmpty { special.take(1) })
                putIfAbsent(PetAction.LOOK_UP, special.takeLast(2))
            }
            get(PetAction.SPECIAL_2)?.distinct()?.let { special ->
                putIfAbsent(PetAction.TAPPED, special.take(2))
                putIfAbsent(PetAction.EMOTE, special.takeLast(2))
            }
        }
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
            ?.let { frame -> putIfAbsent(PetAction.HOLD_CEILING, listOf(frame)) }
    }
}

private fun PetPackManifest.normalizedSourceClip(action: PetAction): PetPackClip? {
    val compactLegacyProfile = id.startsWith(OWNER_SHIMEJI_PACK_PREFIX) &&
        clips.keys.hasCompactLegacyActionProfile()
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

        PetAction.JUMP -> clips[PetAction.JUMP]
            ?: clips[PetAction.DRAGGED]
                ?.takeIf { compactLegacyProfile }
                ?.compactJump()
        PetAction.FLUNG -> clips[PetAction.FLUNG]
            ?: clips[PetAction.DRAGGED]
                ?.takeIf { compactLegacyProfile }
                ?.compactFlung()
        PetAction.CREEP -> clips[PetAction.CREEP]
            ?: clips[PetAction.BOUNCE]
                ?.takeIf { compactLegacyProfile }
                ?.compactCreep()
        PetAction.CLIMB_CEILING -> clips[PetAction.CLIMB_CEILING]
            ?: clips[PetAction.CLIMB_WALL]
                ?.takeIf { compactLegacyProfile }
                ?.compactCeilingClimb()
        PetAction.HOLD_CEILING -> clips[PetAction.CLIMB_CEILING]
            ?.stationaryFrameAction(PetAction.HOLD_CEILING, OWNER_HOLD_CEILING_FRAME_INDEX)
            ?: clips[PetAction.CLIMB_WALL]
                ?.takeIf { compactLegacyProfile }
                ?.compactCeilingHold()
        PetAction.TRIP -> clips[PetAction.TRIP]
            ?: clips[PetAction.DRAGGED]
                ?.takeIf { compactLegacyProfile }
                ?.compactTrip(clips.getValue(PetAction.BOUNCE))
        PetAction.SIT -> clips[PetAction.SIT]
            ?: clips[PetAction.SPECIAL]
                ?.takeIf { compactLegacyProfile }
                ?.compactPose(PetAction.SIT, takeLast = false)
        PetAction.LOOK_UP -> clips[PetAction.LOOK_UP]
            ?: clips[PetAction.SPECIAL]
                ?.takeIf { compactLegacyProfile }
                ?.compactPose(PetAction.LOOK_UP)
        PetAction.TAPPED -> clips[PetAction.TAPPED]
            ?: clips[PetAction.SPECIAL_2]
                ?.takeIf { compactLegacyProfile }
                ?.compactPose(PetAction.TAPPED, takeLast = false)
        PetAction.EMOTE -> clips[PetAction.WINK]?.derivedAction(PetAction.EMOTE)
            ?: clips[PetAction.SPECIAL_2]
                ?.takeIf { compactLegacyProfile }
                ?.compactPose(PetAction.EMOTE)
        PetAction.FLOOR_PLAY -> clips[PetAction.DANGLE]?.derivedAction(PetAction.FLOOR_PLAY)
            ?: clips[PetAction.BOUNCE]
                ?.takeIf { compactLegacyProfile }
                ?.compactFloorPlay()
        PetAction.SPRAWL -> (clips[PetAction.CREEP]
            ?: clips[PetAction.BOUNCE]?.takeIf { compactLegacyProfile })
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

private fun PetPackClip.compactJump(): PetPackClip = PetPackClip(
    action = PetAction.JUMP,
    loops = false,
    nextAction = PetAction.FALL,
    frames = frames.take(1).map { frame ->
        frame.copy(velocity = PetVector(JUMP_HORIZONTAL_VELOCITY, JUMP_VERTICAL_VELOCITY))
    }
)

private fun PetPackClip.compactFlung(): PetPackClip = PetPackClip(
    action = PetAction.FLUNG,
    loops = true,
    nextAction = null,
    frames = frames.map { frame -> frame.copy(velocity = PetVector.Zero) }
)

private fun PetPackClip.compactCreep(): PetPackClip = PetPackClip(
    action = PetAction.CREEP,
    loops = true,
    nextAction = null,
    frames = frames.map { frame -> frame.copy(velocity = PetVector(x = CREEP_VELOCITY)) }
)

private fun PetPackClip.compactCeilingClimb(): PetPackClip = PetPackClip(
    action = PetAction.CLIMB_CEILING,
    loops = true,
    nextAction = null,
    frames = frames.map { frame ->
        frame.copy(velocity = PetVector(x = CLIMB_VELOCITY))
    }
)

private fun PetPackClip.compactCeilingHold(): PetPackClip? =
    (frames.getOrNull(OWNER_HOLD_WALL_FRAME_INDEX) ?: frames.lastOrNull())
        ?.let { frame ->
            PetPackClip(
                action = PetAction.HOLD_CEILING,
                loops = true,
                nextAction = null,
                frames = listOf(frame.copy(velocity = PetVector.Zero))
            )
        }

private fun PetPackClip.compactTrip(bounce: PetPackClip): PetPackClip = PetPackClip(
    action = PetAction.TRIP,
    loops = false,
    nextAction = PetAction.WALK,
    frames = (frames.take(2) + bounce.frames).map { frame ->
        frame.copy(velocity = PetVector.Zero)
    }
)

private fun PetPackClip.compactPose(
    action: PetAction,
    takeLast: Boolean = true
): PetPackClip {
    val distinctFrames = frames.distinctBy(PetPackFrame::file)
    val selectedFrames = if (takeLast) {
        distinctFrames.takeLast(COMPACT_POSE_FRAME_COUNT)
    } else {
        distinctFrames.drop(1).take(COMPACT_POSE_FRAME_COUNT)
            .ifEmpty { distinctFrames.take(COMPACT_POSE_FRAME_COUNT) }
    }
    return PetPackClip(
        action = action,
        loops = false,
        nextAction = PetAction.WALK,
        frames = selectedFrames.map { frame -> frame.copy(velocity = PetVector.Zero) }
    )
}

private fun PetPackClip.compactFloorPlay(): PetPackClip = PetPackClip(
    action = PetAction.FLOOR_PLAY,
    loops = true,
    nextAction = null,
    frames = frames.map { frame -> frame.copy(velocity = PetVector.Zero) }
)

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
private const val COMPACT_POSE_FRAME_COUNT = 2
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
private val COMPACT_LEGACY_REQUIRED_ACTIONS = setOf(
    PetAction.DRAGGED,
    PetAction.FALL,
    PetAction.BOUNCE,
    PetAction.CLIMB_WALL,
    PetAction.CLIMB_DOWN,
    PetAction.SPECIAL,
    PetAction.SPECIAL_2
)
private val COMPACT_LEGACY_DERIVED_ACTIONS = setOf(
    PetAction.JUMP,
    PetAction.FLUNG,
    PetAction.CREEP,
    PetAction.CLIMB_CEILING,
    PetAction.HOLD_CEILING,
    PetAction.TRIP,
    PetAction.SIT,
    PetAction.LOOK_UP,
    PetAction.TAPPED,
    PetAction.EMOTE,
    PetAction.FLOOR_PLAY,
    PetAction.SPRAWL
)

private fun Set<PetAction>.hasCompactLegacyActionProfile(): Boolean =
    PetAction.JUMP !in this && containsAll(COMPACT_LEGACY_REQUIRED_ACTIONS)
