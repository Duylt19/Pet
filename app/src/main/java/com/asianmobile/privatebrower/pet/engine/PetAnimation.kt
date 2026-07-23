package com.asianmobile.privatebrower.pet.engine

import kotlin.math.min

enum class PetAction {
    IDLE,
    WALK,
    RUN,
    FALL,
    BOUNCE,
    CLIMB_WALL,
    CLIMB_DOWN,
    CLIMB_CEILING,
    SIT,
    WINK,
    LOOK_UP,
    DANGLE,
    CREEP,
    TRIP,
    JUMP,
    TALK,
    TALK_WALK,
    SPECIAL,
    SPECIAL_2,
    TAPPED,
    DRAGGED,
    FLUNG
}

data class PetFrame(
    val index: Int,
    val durationMillis: Long,
    val velocity: PetVector = PetVector.Zero
) {
    init {
        require(index >= 0) { "frame index must not be negative" }
        require(durationMillis > 0) { "frame duration must be positive" }
    }
}

data class PetClip(
    val action: PetAction,
    val frames: List<PetFrame>,
    val loops: Boolean,
    val nextAction: PetAction? = null
) {
    init {
        require(frames.isNotEmpty()) { "clip must contain at least one frame" }
        require(!loops || nextAction == null) {
            "a looping clip cannot have a next action"
        }
        require(loops || nextAction != action) {
            "a non-looping clip cannot transition directly to itself"
        }
    }
}

data class PetAnimationCursor(
    val frameIndex: Int = 0,
    val elapsedInFrameMillis: Long = 0
)

data class PetTimelineAdvance(
    val action: PetAction,
    val cursor: PetAnimationCursor,
    val displacement: PetVector,
    val actionTransitions: List<Pair<PetAction, PetAction>>
)

class PetAnimationTimeline(
    private val clips: Map<PetAction, PetClip>
) {
    init {
        require(clips[PetAction.IDLE]?.loops == true) {
            "an infinite looping IDLE clip is required"
        }
        clips.forEach { (action, clip) ->
            require(action == clip.action) { "clip key and action must match" }
        }
    }

    fun advance(
        action: PetAction,
        cursor: PetAnimationCursor,
        elapsedMillis: Long,
        stopAtActionTransition: Boolean = false
    ): PetTimelineAdvance {
        require(elapsedMillis >= 0) { "elapsedMillis must not be negative" }

        var currentAction = action
        var currentCursor = normalizedCursor(clipFor(currentAction), cursor)
        var remainingMillis = elapsedMillis
        var displacement = PetVector.Zero
        val transitions = mutableListOf<Pair<PetAction, PetAction>>()
        var steps = 0

        while (remainingMillis > 0) {
            check(++steps <= MAX_ADVANCE_STEPS) {
                "animation advance exceeded the safety step limit"
            }
            val clip = clipFor(currentAction)
            val frame = clip.frames[currentCursor.frameIndex]
            val availableMillis = frame.durationMillis - currentCursor.elapsedInFrameMillis
            val consumedMillis = min(remainingMillis, availableMillis)
            displacement += frame.velocity * (consumedMillis / MILLIS_PER_SECOND)
            remainingMillis -= consumedMillis

            val elapsedInFrame = currentCursor.elapsedInFrameMillis + consumedMillis
            if (elapsedInFrame < frame.durationMillis) {
                currentCursor = currentCursor.copy(elapsedInFrameMillis = elapsedInFrame)
                break
            }

            val nextFrameIndex = currentCursor.frameIndex + 1
            when {
                nextFrameIndex < clip.frames.size -> {
                    currentCursor = PetAnimationCursor(frameIndex = nextFrameIndex)
                }

                clip.loops -> {
                    currentCursor = PetAnimationCursor()
                }

                else -> {
                    val nextAction = clip.nextAction ?: PetAction.IDLE
                    transitions += currentAction to nextAction
                    currentAction = nextAction
                    currentCursor = PetAnimationCursor()
                    if (stopAtActionTransition) {
                        remainingMillis = 0
                    }
                }
            }
        }

        return PetTimelineAdvance(
            action = currentAction,
            cursor = currentCursor,
            displacement = displacement,
            actionTransitions = transitions
        )
    }

    private fun clipFor(action: PetAction): PetClip =
        clips[action] ?: checkNotNull(clips[PetAction.IDLE])

    private fun normalizedCursor(
        clip: PetClip,
        cursor: PetAnimationCursor
    ): PetAnimationCursor {
        val frameIndex = cursor.frameIndex.coerceIn(0, clip.frames.lastIndex)
        val frame = clip.frames[frameIndex]
        return PetAnimationCursor(
            frameIndex = frameIndex,
            elapsedInFrameMillis = cursor.elapsedInFrameMillis.coerceIn(
                minimumValue = 0,
                maximumValue = frame.durationMillis - 1
            )
        )
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000f
        const val MAX_ADVANCE_STEPS = 10_000
    }
}

object DemoPetAnimation {
    fun clips(): Map<PetAction, PetClip> = listOf(
        PetClip(
            action = PetAction.IDLE,
            frames = List(4) { index -> PetFrame(index = index, durationMillis = 180) },
            loops = true
        ),
        PetClip(
            action = PetAction.WALK,
            frames = List(4) { index ->
                PetFrame(
                    index = index,
                    durationMillis = 120,
                    velocity = PetVector(x = 42f)
                )
            },
            loops = true
        ),
        PetClip(
            action = PetAction.RUN,
            frames = List(4) { index ->
                PetFrame(
                    index = index,
                    durationMillis = 80,
                    velocity = PetVector(x = 82f)
                )
            },
            loops = true
        ),
        PetClip(
            action = PetAction.FALL,
            frames = listOf(
                PetFrame(index = 0, durationMillis = 120, velocity = PetVector(y = 220f))
            ),
            loops = true
        ),
        PetClip(
            action = PetAction.BOUNCE,
            frames = List(2) { index -> PetFrame(index = index, durationMillis = 110) },
            loops = false,
            nextAction = PetAction.WALK
        ),
        PetClip(
            action = PetAction.CLIMB_WALL,
            frames = List(4) { index ->
                PetFrame(index = index, durationMillis = 120, velocity = PetVector(y = -36f))
            },
            loops = true
        ),
        PetClip(
            action = PetAction.CLIMB_DOWN,
            frames = List(4) { index ->
                PetFrame(index = index, durationMillis = 120, velocity = PetVector(y = 36f))
            },
            loops = true
        ),
        PetClip(
            action = PetAction.CLIMB_CEILING,
            frames = List(4) { index ->
                PetFrame(index = index, durationMillis = 120, velocity = PetVector(x = 36f))
            },
            loops = true
        ),
        oneShot(PetAction.SIT, frameCount = 1, frameDurationMillis = 2_400),
        oneShot(PetAction.WINK, frameCount = 2, frameDurationMillis = 260),
        oneShot(PetAction.LOOK_UP, frameCount = 1, frameDurationMillis = 1_200),
        oneShot(PetAction.DANGLE, frameCount = 4, frameDurationMillis = 320),
        PetClip(
            action = PetAction.CREEP,
            frames = List(4) { index ->
                PetFrame(index = index, durationMillis = 180, velocity = PetVector(x = 16f))
            },
            loops = true
        ),
        oneShot(PetAction.TRIP, frameCount = 4, frameDurationMillis = 140),
        PetClip(
            action = PetAction.TALK,
            frames = listOf(PetFrame(index = 0, durationMillis = 240)),
            loops = true
        ),
        PetClip(
            action = PetAction.TALK_WALK,
            frames = List(4) { index ->
                PetFrame(
                    index = index,
                    durationMillis = 240,
                    velocity = PetVector(x = 24f)
                )
            },
            loops = true
        ),
        PetClip(
            action = PetAction.JUMP,
            frames = listOf(
                PetFrame(
                    index = 0,
                    durationMillis = 220,
                    velocity = PetVector(x = 110f, y = -80f)
                )
            ),
            loops = false,
            nextAction = PetAction.FALL
        ),
        oneShot(PetAction.SPECIAL, frameCount = 4, frameDurationMillis = 220),
        oneShot(PetAction.SPECIAL_2, frameCount = 8, frameDurationMillis = 160),
        PetClip(
            action = PetAction.TAPPED,
            frames = List(3) { index -> PetFrame(index = index, durationMillis = 100) },
            loops = false,
            nextAction = PetAction.IDLE
        ),
        PetClip(
            action = PetAction.DRAGGED,
            frames = listOf(PetFrame(index = 0, durationMillis = 200)),
            loops = true
        ),
        PetClip(
            action = PetAction.FLUNG,
            frames = List(2) { index -> PetFrame(index = index, durationMillis = 100) },
            loops = true
        )
    ).associateBy(PetClip::action)

    private fun oneShot(
        action: PetAction,
        frameCount: Int,
        frameDurationMillis: Long
    ) = PetClip(
        action = action,
        frames = List(frameCount) { index ->
            PetFrame(index = index, durationMillis = frameDurationMillis)
        },
        loops = false,
        nextAction = PetAction.WALK
    )
}

internal val PetAction.isSpeechAction: Boolean
    get() = this == PetAction.TALK || this == PetAction.TALK_WALK
