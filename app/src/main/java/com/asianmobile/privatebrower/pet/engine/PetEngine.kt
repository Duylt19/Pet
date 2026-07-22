package com.asianmobile.privatebrower.pet.engine

import kotlin.math.max

data class PetEngineConfig(
    val clips: Map<PetAction, PetClip> = DemoPetAnimation.clips(),
    val tapAction: PetAction = PetAction.TAPPED,
    val maxTickMillis: Long = 250,
    val maxFlingSpeed: Float = 2_500f,
    val flingDeceleration: Float = 3_500f,
    val flingStopSpeed: Float = 24f,
    val supportedActions: Set<PetAction> = clips.keys,
    val autonomousIntervalMillis: Long = 4_000,
    val autonomousActions: List<PetAction> = listOf(
        PetAction.SIT,
        PetAction.WINK,
        PetAction.TRIP,
        PetAction.SPECIAL,
        PetAction.SPECIAL_2,
        PetAction.CREEP
    )
) {
    init {
        require(clips.keys.containsAll(PetAction.entries)) {
            "engine configuration must provide a clip for every pet action"
        }
        require(tapAction in clips) { "tapAction must reference a configured clip" }
        require(maxTickMillis > 0) { "maxTickMillis must be positive" }
        require(maxFlingSpeed > 0f) { "maxFlingSpeed must be positive" }
        require(flingDeceleration > 0f) { "flingDeceleration must be positive" }
        require(flingStopSpeed >= 0f) { "flingStopSpeed must not be negative" }
        require(flingStopSpeed < maxFlingSpeed) {
            "flingStopSpeed must be lower than maxFlingSpeed"
        }
        require(autonomousIntervalMillis > 0) {
            "autonomousIntervalMillis must be positive"
        }
        require(supportedActions.all(clips::containsKey)) {
            "supported actions must reference configured clips"
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
            changeAction(state, preferredAction(PetAction.FALL, PetAction.IDLE))
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
            action = config.tapAction,
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

            val collisionAction = collisionAction(
                action = PetAction.FLUNG,
                requested = requestedPosition,
                constrained = constrainedPosition,
                bounds = updatedState.bounds
            )
            if (collisionAction != null) {
                val collided = changeAction(
                    state = updatedState.copy(velocity = PetVector.Zero),
                    action = collisionAction
                )
                return collided.copy(effects = effects + collided.effects)
            }

            if (constrainedVelocity.magnitude <= config.flingStopSpeed) {
                val stopped = changeAction(
                    state = updatedState.copy(velocity = PetVector.Zero),
                    action = preferredAction(PetAction.FALL, PetAction.IDLE)
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
        val hitHorizontalEdge = constrainedPosition.x != requestedPosition.x
        val direction = if (updatedState.action == PetAction.WALK && hitHorizontalEdge) {
            updatedState.direction.opposite()
        } else {
            updatedState.direction
        }
        updatedState = updatedState.copy(
            position = constrainedPosition,
            velocity = PetVector.Zero,
            direction = direction
        )
        val collisionAction = collisionAction(
            action = updatedState.action,
            requested = requestedPosition,
            constrained = constrainedPosition,
            bounds = updatedState.bounds
        )
        if (collisionAction != null) {
            val collided = changeAction(updatedState, collisionAction)
            return collided.copy(effects = effects + collided.effects)
        }
        return applyAutonomousBehavior(
            state = updatedState,
            elapsedMillis = elapsedMillis,
            effects = effects,
            previousAction = state.action
        )
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
                animationCursor = PetAnimationCursor(),
                autonomousElapsedMillis = if (action == PetAction.WALK) {
                    state.autonomousElapsedMillis
                } else {
                    0
                }
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

    private fun collisionAction(
        action: PetAction,
        requested: PetVector,
        constrained: PetVector,
        bounds: PetBounds
    ): PetAction? {
        val hitLeft = requested.x < constrained.x
        val hitRight = requested.x > constrained.x
        val hitTop = requested.y < constrained.y
        val hitBottom = requested.y > constrained.y
        return when (action) {
            PetAction.FALL -> if (hitBottom) preferredAction(PetAction.BOUNCE, PetAction.WALK) else null
            PetAction.WALK,
            PetAction.CREEP -> if (hitLeft || hitRight) {
                preferredActionOrNull(PetAction.CLIMB_WALL)
            } else {
                null
            }
            PetAction.CLIMB_WALL -> if (hitTop || constrained.y <= bounds.top) {
                preferredAction(PetAction.CLIMB_CEILING, PetAction.FALL, PetAction.WALK)
            } else {
                null
            }
            PetAction.CLIMB_CEILING -> if (hitLeft || hitRight) {
                preferredAction(PetAction.FALL, PetAction.WALK)
            } else {
                null
            }
            PetAction.FLUNG -> when {
                hitBottom -> preferredAction(PetAction.BOUNCE, PetAction.WALK)
                hitTop -> preferredAction(PetAction.CLIMB_CEILING, PetAction.FALL, PetAction.WALK)
                hitLeft || hitRight -> preferredAction(PetAction.CLIMB_WALL, PetAction.FALL, PetAction.WALK)
                else -> null
            }
            else -> null
        }
    }

    private fun applyAutonomousBehavior(
        state: PetState,
        elapsedMillis: Long,
        effects: List<PetEffect>,
        previousAction: PetAction
    ): PetTransition {
        if (state.action == PetAction.IDLE && previousAction == PetAction.IDLE) {
            val total = state.autonomousElapsedMillis + elapsedMillis
            if (total >= config.autonomousIntervalMillis && PetAction.WALK in config.supportedActions) {
                val changed = changeAction(
                    state.copy(autonomousElapsedMillis = 0),
                    PetAction.WALK
                )
                return changed.copy(effects = effects + changed.effects)
            }
            return PetTransition(state.copy(autonomousElapsedMillis = total), effects)
        }
        if (state.action != PetAction.WALK || previousAction != PetAction.WALK) {
            return PetTransition(state.copy(autonomousElapsedMillis = 0), effects)
        }
        val eligible = config.autonomousActions.filter { action ->
            action in config.supportedActions && action != PetAction.WALK
        }
        if (eligible.isEmpty()) {
            return PetTransition(state.copy(autonomousElapsedMillis = 0), effects)
        }
        val total = state.autonomousElapsedMillis + elapsedMillis
        if (total < config.autonomousIntervalMillis) {
            return PetTransition(state.copy(autonomousElapsedMillis = total), effects)
        }
        val next = eligible[state.autonomousStep.mod(eligible.size)]
        val changed = changeAction(
            state.copy(
                autonomousElapsedMillis = 0,
                autonomousStep = state.autonomousStep + 1
            ),
            next
        )
        return changed.copy(effects = effects + changed.effects)
    }

    private fun preferredActionOrNull(vararg actions: PetAction): PetAction? =
        actions.firstOrNull { it in config.supportedActions }

    private fun preferredAction(vararg actions: PetAction): PetAction =
        preferredActionOrNull(*actions) ?: PetAction.IDLE

    private data class FlingAdvance(
        val velocity: PetVector,
        val displacement: PetVector
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000f
    }
}
