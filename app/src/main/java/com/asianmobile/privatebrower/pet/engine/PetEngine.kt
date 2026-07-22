package com.asianmobile.privatebrower.pet.engine

import kotlin.math.max

data class PetEngineConfig(
    val clips: Map<PetAction, PetClip> = DemoPetAnimation.clips(),
    val maxTickMillis: Long = 250,
    val maxFlingSpeed: Float = 2_500f,
    val flingDeceleration: Float = 3_500f,
    val flingStopSpeed: Float = 24f
) {
    init {
        require(clips.keys.containsAll(PetAction.entries)) {
            "engine configuration must provide a clip for every pet action"
        }
        require(maxTickMillis > 0) { "maxTickMillis must be positive" }
        require(maxFlingSpeed > 0f) { "maxFlingSpeed must be positive" }
        require(flingDeceleration > 0f) { "flingDeceleration must be positive" }
        require(flingStopSpeed >= 0f) { "flingStopSpeed must not be negative" }
        require(flingStopSpeed < maxFlingSpeed) {
            "flingStopSpeed must be lower than maxFlingSpeed"
        }
    }
}

class PetEngine(
    private val config: PetEngineConfig = PetEngineConfig()
) {
    private val timeline = PetAnimationTimeline(config.clips)

    fun initialState(
        bounds: PetBounds,
        size: PetSize,
        position: PetVector = PetVector(bounds.left, bounds.top),
        action: PetAction = PetAction.IDLE,
        direction: PetDirection = PetDirection.RIGHT
    ): PetState = PetState(
        position = bounds.clampTopLeft(position, size),
        velocity = PetVector.Zero,
        size = size,
        bounds = bounds,
        action = action,
        direction = direction,
        animationCursor = PetAnimationCursor()
    )

    fun reduce(state: PetState, event: PetEvent): PetTransition = when (event) {
        is PetEvent.Tick -> onTick(state, event.elapsedMillis)
        PetEvent.Tap -> onTap(state)
        PetEvent.DragStart -> changeAction(
            state = state.copy(velocity = PetVector.Zero),
            action = PetAction.DRAGGED,
            restartAnimation = true
        )

        is PetEvent.DragBy -> onDragBy(state, event.delta)
        PetEvent.DragEnd -> if (state.action == PetAction.DRAGGED) {
            changeAction(state, PetAction.IDLE)
        } else {
            PetTransition(state)
        }

        is PetEvent.Fling -> onFling(state, event.velocity)
        is PetEvent.BoundsChanged -> PetTransition(
            state.copy(
                bounds = event.bounds,
                position = event.bounds.clampTopLeft(state.position, state.size)
            )
        )
    }

    private fun onTap(state: PetState): PetTransition {
        if (state.action == PetAction.DRAGGED) return PetTransition(state)
        val transition = changeAction(
            state = state.copy(velocity = PetVector.Zero),
            action = PetAction.TAPPED,
            restartAnimation = true
        )
        return transition.copy(effects = transition.effects + PetEffect.Tapped)
    }

    private fun onDragBy(state: PetState, delta: PetVector): PetTransition {
        if (state.action != PetAction.DRAGGED) return PetTransition(state)
        val direction = when {
            delta.x > 0f -> PetDirection.RIGHT
            delta.x < 0f -> PetDirection.LEFT
            else -> state.direction
        }
        return PetTransition(
            state.copy(
                position = state.bounds.clampTopLeft(state.position + delta, state.size),
                direction = direction
            )
        )
    }

    private fun onFling(state: PetState, requestedVelocity: PetVector): PetTransition {
        val velocity = requestedVelocity.limitedTo(config.maxFlingSpeed)
        if (velocity.magnitude <= config.flingStopSpeed) {
            return changeAction(
                state = state.copy(velocity = PetVector.Zero),
                action = PetAction.IDLE
            )
        }
        val direction = when {
            velocity.x > 0f -> PetDirection.RIGHT
            velocity.x < 0f -> PetDirection.LEFT
            else -> state.direction
        }
        return changeAction(
            state = state.copy(velocity = velocity, direction = direction),
            action = PetAction.FLUNG,
            restartAnimation = true
        )
    }

    private fun onTick(state: PetState, requestedElapsedMillis: Long): PetTransition {
        if (requestedElapsedMillis <= 0 || state.action == PetAction.DRAGGED) {
            return PetTransition(state)
        }

        val elapsedMillis = requestedElapsedMillis.coerceAtMost(config.maxTickMillis)
        val animation = timeline.advance(
            action = state.action,
            cursor = state.animationCursor,
            elapsedMillis = elapsedMillis
        )
        val scriptedDisplacement = animation.displacement.withDirection(state.direction)
        val effects = animation.actionTransitions.map { (from, to) ->
            PetEffect.ActionChanged(from, to)
        }.toMutableList<PetEffect>()
        var updatedState = state.copy(
            action = animation.action,
            animationCursor = animation.cursor
        )

        if (updatedState.action == PetAction.FLUNG) {
            val fling = advanceFling(updatedState.velocity, elapsedMillis)
            val requestedPosition = updatedState.position + scriptedDisplacement + fling.displacement
            val constrainedPosition = updatedState.bounds.clampTopLeft(
                requestedPosition,
                updatedState.size
            )
            val constrainedVelocity = PetVector(
                x = if (constrainedPosition.x != requestedPosition.x) 0f else fling.velocity.x,
                y = if (constrainedPosition.y != requestedPosition.y) 0f else fling.velocity.y
            )
            updatedState = updatedState.copy(
                position = constrainedPosition,
                velocity = constrainedVelocity
            )

            if (constrainedVelocity.magnitude <= config.flingStopSpeed) {
                val stopped = changeAction(
                    state = updatedState.copy(velocity = PetVector.Zero),
                    action = PetAction.IDLE
                )
                return stopped.copy(effects = effects + stopped.effects)
            }
            return PetTransition(updatedState, effects)
        }

        val requestedPosition = updatedState.position + scriptedDisplacement
        val constrainedPosition = updatedState.bounds.clampTopLeft(
            requestedPosition,
            updatedState.size
        )
        val direction = if (
            updatedState.action == PetAction.WALK &&
            constrainedPosition.x != requestedPosition.x
        ) {
            updatedState.direction.opposite()
        } else {
            updatedState.direction
        }
        updatedState = updatedState.copy(
            position = constrainedPosition,
            velocity = PetVector.Zero,
            direction = direction
        )
        return PetTransition(updatedState, effects)
    }

    private fun advanceFling(
        velocity: PetVector,
        elapsedMillis: Long
    ): FlingAdvance {
        val speed = velocity.magnitude
        if (speed == 0f) return FlingAdvance(PetVector.Zero, PetVector.Zero)

        val seconds = elapsedMillis / MILLIS_PER_SECOND
        val newSpeed = max(0f, speed - config.flingDeceleration * seconds)
        val direction = velocity * (1f / speed)
        val distance = (speed + newSpeed) * 0.5f * seconds
        return FlingAdvance(
            velocity = direction * newSpeed,
            displacement = direction * distance
        )
    }

    private fun changeAction(
        state: PetState,
        action: PetAction,
        restartAnimation: Boolean = false
    ): PetTransition {
        if (state.action == action && !restartAnimation) return PetTransition(state)
        return PetTransition(
            state = state.copy(
                action = action,
                animationCursor = PetAnimationCursor()
            ),
            effects = if (state.action == action) {
                emptyList()
            } else {
                listOf(PetEffect.ActionChanged(state.action, action))
            }
        )
    }

    private fun PetVector.withDirection(direction: PetDirection): PetVector = when (direction) {
        PetDirection.LEFT -> copy(x = -x)
        PetDirection.RIGHT -> this
    }

    private fun PetDirection.opposite(): PetDirection = when (this) {
        PetDirection.LEFT -> PetDirection.RIGHT
        PetDirection.RIGHT -> PetDirection.LEFT
    }

    private data class FlingAdvance(
        val velocity: PetVector,
        val displacement: PetVector
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000f
    }
}
