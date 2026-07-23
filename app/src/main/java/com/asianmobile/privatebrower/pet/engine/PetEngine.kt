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
    val behaviorProfile: PetBehaviorProfile = PetBehaviorProfile(),
    val behaviorSeed: Long = 0,
    val fallGravity: Float = 900f,
    val initialFallSpeed: Float = 120f,
    val terminalFallSpeed: Float = 900f
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
        require(supportedActions.all(clips::containsKey)) {
            "supported actions must reference configured clips"
        }
        require(fallGravity > 0f) { "fall gravity must be positive" }
        require(initialFallSpeed >= 0f) { "initial fall speed must not be negative" }
        require(terminalFallSpeed > initialFallSpeed) {
            "terminal fall speed must exceed initial fall speed"
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
                action = preferredAction(PetAction.FALL, PetAction.IDLE)
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
        val actionChangedByTimeline = animation.action != state.action
        var updatedState = state.copy(
            action = animation.action,
            animationCursor = animation.cursor,
            actionElapsedMillis = if (actionChangedByTimeline) {
                0
            } else {
                state.actionElapsedMillis + elapsedMillis
            },
            actionTargetMillis = if (actionChangedByTimeline) 0 else state.actionTargetMillis
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

        if (updatedState.action == PetAction.FALL) {
            return advanceFall(updatedState, scriptedDisplacement, elapsedMillis, effects)
        }

        val requestedPosition = updatedState.position + scriptedDisplacement
        val constrainedPosition = updatedState.bounds.clampTopLeft(
            requestedPosition,
            updatedState.size
        )
        val hitHorizontalEdge = constrainedPosition.x != requestedPosition.x
        val collisionAction = collisionAction(
            action = updatedState.action,
            requested = requestedPosition,
            constrained = constrainedPosition,
            bounds = updatedState.bounds
        )
        val direction = directionAfterCollision(
            action = updatedState.action,
            direction = updatedState.direction,
            hitHorizontalEdge = hitHorizontalEdge,
            collisionAction = collisionAction
        )
        updatedState = updatedState.copy(
            position = constrainedPosition,
            velocity = PetVector.Zero,
            direction = direction
        )
        if (collisionAction != null) {
            val collided = changeAction(updatedState, collisionAction)
            return collided.copy(effects = effects + collided.effects)
        }
        return applyLivingBehavior(
            state = updatedState,
            effects = effects
        )
    }

    private fun advanceFall(
        state: PetState,
        scriptedDisplacement: PetVector,
        elapsedMillis: Long,
        effects: List<PetEffect>
    ): PetTransition {
        val seconds = elapsedMillis / MILLIS_PER_SECOND
        val oldSpeed = state.velocity.y.coerceAtLeast(config.initialFallSpeed)
        val newSpeed = (oldSpeed + config.fallGravity * seconds)
            .coerceAtMost(config.terminalFallSpeed)
        val fallDistance = (oldSpeed + newSpeed) * 0.5f * seconds
        val requestedPosition = state.position + PetVector(
            x = scriptedDisplacement.x,
            y = fallDistance
        )
        val constrainedPosition = state.bounds.clampTopLeft(requestedPosition, state.size)
        val collisionAction = collisionAction(
            action = PetAction.FALL,
            requested = requestedPosition,
            constrained = constrainedPosition,
            bounds = state.bounds
        )
        val falling = state.copy(
            position = constrainedPosition,
            velocity = PetVector(y = newSpeed)
        )
        if (collisionAction == null) return PetTransition(falling, effects)

        val collided = changeAction(
            state = falling.copy(velocity = PetVector.Zero),
            action = collisionAction
        )
        return collided.copy(effects = effects + collided.effects)
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
                actionElapsedMillis = 0,
                actionTargetMillis = 0
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

    private fun directionAfterCollision(
        action: PetAction,
        direction: PetDirection,
        hitHorizontalEdge: Boolean,
        collisionAction: PetAction?
    ): PetDirection = when {
        action == PetAction.CLIMB_WALL && collisionAction == PetAction.CLIMB_CEILING -> {
            direction.opposite()
        }
        action == PetAction.WALK && hitHorizontalEdge &&
            collisionAction != PetAction.CLIMB_WALL -> direction.opposite()
        else -> direction
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

    private fun applyLivingBehavior(
        state: PetState,
        effects: List<PetEffect>
    ): PetTransition = when (state.action) {
        PetAction.IDLE -> timedTransition(
            state,
            config.behaviorProfile.idleDurationMillis,
            preferredAction(PetAction.WALK, PetAction.IDLE),
            effects,
            IDLE_DURATION_SALT
        )

        PetAction.WALK -> applyGroundBehavior(state, effects)
        PetAction.CREEP -> timedTransition(
            state,
            config.behaviorProfile.creepDurationMillis,
            preferredAction(PetAction.WALK, PetAction.IDLE),
            effects,
            CREEP_DURATION_SALT
        )

        PetAction.CLIMB_WALL -> applyWallTimeout(state, effects)
        PetAction.CLIMB_CEILING -> timedTransition(
            state,
            config.behaviorProfile.ceilingDurationMillis,
            preferredAction(PetAction.FALL, PetAction.WALK),
            effects,
            CEILING_DURATION_SALT
        )

        else -> PetTransition(state, effects)
    }

    private fun applyGroundBehavior(
        state: PetState,
        effects: List<PetEffect>
    ): PetTransition {
        val scheduled = ensureActionTarget(
            state,
            config.behaviorProfile.groundDelayMillis,
            GROUND_DELAY_SALT
        )
        if (scheduled.actionElapsedMillis < scheduled.actionTargetMillis) {
            return PetTransition(scheduled, effects)
        }

        val supportedRules = config.behaviorProfile.autonomousRules.filter { rule ->
            rule.action in config.supportedActions && rule.action != PetAction.WALK
        }
        val freshRules = supportedRules.filterNot { it.action in scheduled.recentAutonomousActions }
        val eligibleRules = freshRules.ifEmpty { supportedRules }
        val totalWeight = config.behaviorProfile.continueWalkWeight +
            config.behaviorProfile.turnAroundWeight + eligibleRules.sumOf(PetBehaviorRule::weight)
        if (totalWeight <= 0) {
            return PetTransition(scheduled.resetActionTimer(), effects)
        }

        val draw = draw(scheduled, 0 until totalWeight, GROUND_CHOICE_SALT)
        var cursor = draw.value
        if (cursor < config.behaviorProfile.continueWalkWeight) {
            return PetTransition(draw.state.resetActionTimer(), effects)
        }
        cursor -= config.behaviorProfile.continueWalkWeight
        if (cursor < config.behaviorProfile.turnAroundWeight) {
            return PetTransition(
                draw.state.copy(direction = draw.state.direction.opposite()).resetActionTimer(),
                effects
            )
        }
        cursor -= config.behaviorProfile.turnAroundWeight
        val selected = eligibleRules.first { rule ->
            if (cursor < rule.weight) {
                true
            } else {
                cursor -= rule.weight
                false
            }
        }.action
        val recent = (listOf(selected) + draw.state.recentAutonomousActions)
            .distinct()
            .take(config.behaviorProfile.recentActionMemory)
        val changed = changeAction(
            draw.state.copy(recentAutonomousActions = recent),
            selected
        )
        return changed.copy(effects = effects + changed.effects)
    }

    private fun applyWallTimeout(
        state: PetState,
        effects: List<PetEffect>
    ): PetTransition {
        val scheduled = ensureActionTarget(
            state,
            config.behaviorProfile.wallDurationMillis,
            WALL_DURATION_SALT
        )
        if (scheduled.actionElapsedMillis < scheduled.actionTargetMillis) {
            return PetTransition(scheduled, effects)
        }
        val chance = draw(scheduled, 0 until PERCENT_MAX, WALL_EXIT_SALT)
        val canJump = PetAction.JUMP in config.supportedActions
        val nextAction = if (
            canJump && chance.value < config.behaviorProfile.wallJumpChancePercent
        ) {
            PetAction.JUMP
        } else {
            preferredAction(PetAction.FALL, PetAction.WALK)
        }
        val exiting = if (nextAction == PetAction.JUMP) {
            chance.state.copy(direction = chance.state.direction.opposite())
        } else {
            chance.state
        }
        val changed = changeAction(exiting, nextAction)
        return changed.copy(effects = effects + changed.effects)
    }

    private fun timedTransition(
        state: PetState,
        durationRange: LongRange,
        nextAction: PetAction,
        effects: List<PetEffect>,
        salt: Long
    ): PetTransition {
        val scheduled = ensureActionTarget(state, durationRange, salt)
        if (scheduled.actionElapsedMillis < scheduled.actionTargetMillis ||
            nextAction == scheduled.action
        ) {
            return PetTransition(scheduled, effects)
        }
        val changed = changeAction(scheduled, nextAction)
        return changed.copy(effects = effects + changed.effects)
    }

    private fun ensureActionTarget(
        state: PetState,
        durationRange: LongRange,
        salt: Long
    ): PetState = if (state.actionTargetMillis > 0) {
        state
    } else {
        val draw = draw(state, durationRange, salt)
        draw.state.copy(actionTargetMillis = draw.value)
    }

    private fun draw(state: PetState, range: IntRange, salt: Long): RandomDraw<Int> {
        require(!range.isEmpty()) { "random range must not be empty" }
        val span = range.last.toLong() - range.first + 1
        val value = range.first + (mixedRandom(state.behaviorSequence, salt) % span).toInt()
        return RandomDraw(value, state.copy(behaviorSequence = state.behaviorSequence + 1))
    }

    private fun draw(state: PetState, range: LongRange, salt: Long): RandomDraw<Long> {
        val span = range.last - range.first + 1
        val value = range.first + mixedRandom(state.behaviorSequence, salt) % span
        return RandomDraw(value, state.copy(behaviorSequence = state.behaviorSequence + 1))
    }

    private fun mixedRandom(sequence: Long, salt: Long): Long {
        var value = config.behaviorSeed xor (sequence * RANDOM_SEQUENCE_MULTIPLIER) xor salt
        value = value xor (value shl 13)
        value = value xor (value ushr 7)
        value = value xor (value shl 17)
        return value and Long.MAX_VALUE
    }

    private fun PetState.resetActionTimer(): PetState = copy(
        actionElapsedMillis = 0,
        actionTargetMillis = 0
    )

    private fun preferredActionOrNull(vararg actions: PetAction): PetAction? =
        actions.firstOrNull { it in config.supportedActions }

    private fun preferredAction(vararg actions: PetAction): PetAction =
        preferredActionOrNull(*actions) ?: PetAction.IDLE

    private data class FlingAdvance(
        val velocity: PetVector,
        val displacement: PetVector
    )

    private data class RandomDraw<T>(
        val value: T,
        val state: PetState
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000f
        const val PERCENT_MAX = 100
        const val RANDOM_SEQUENCE_MULTIPLIER = 1_103_515_245L
        const val GROUND_DELAY_SALT = 0x101L
        const val GROUND_CHOICE_SALT = 0x102L
        const val IDLE_DURATION_SALT = 0x201L
        const val CREEP_DURATION_SALT = 0x301L
        const val WALL_DURATION_SALT = 0x401L
        const val WALL_EXIT_SALT = 0x402L
        const val CEILING_DURATION_SALT = 0x501L
    }
}
