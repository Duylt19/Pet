package com.asianmobile.privatebrower.pet.engine

import kotlin.math.abs
import kotlin.math.ceil
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
        PetEvent.Showcase -> onShowcase(state)
        PetEvent.DragStart -> changeAction(
            state = state.cancelRoutine().copy(velocity = PetVector.Zero),
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
        is PetEvent.Face -> PetTransition(state.copy(direction = event.direction))
        is PetEvent.StartCombo -> onStartCombo(state, event)
    }

    private fun onTap(state: PetState): PetTransition {
        if (!state.isGroundedSurface()) return PetTransition(state)
        val followUp = preferredActionOrNull(PetAction.WINK, PetAction.LOOK_UP)
            ?.takeUnless { it == config.tapAction }
        val tapBeat = PetComboBeat(
            action = config.tapAction,
            durationMillis = if (config.clips.getValue(config.tapAction).loops) {
                TAP_LOOP_DURATION
            } else {
                null
            }
        )
        val transition = startRoutine(
            state = state.copy(velocity = PetVector.Zero),
            beats = listOfNotNull(
                tapBeat,
                PetComboBeat(PetAction.IDLE, TAP_RECOVERY_DURATION)
                    .takeIf { PetAction.IDLE in config.supportedActions },
                PetComboBeat(PetAction.TALK, PET_TALK_BEAT_DURATION_MILLIS)
                    .takeIf { PetAction.TALK in config.supportedActions },
                followUp?.let(::PetComboBeat)
            ),
            comboId = PetComboId.USER_AFFECTION,
            restartFirstAnimation = true
        )
        return transition.copy(effects = transition.effects + PetEffect.Tapped)
    }

    private fun onShowcase(state: PetState): PetTransition {
        if (!state.isGroundedSurface()) return PetTransition(state)
        val definition = PetComboCatalog.supportedDefinition(
            PetComboId.USER_SHOWCASE,
            config.supportedActions
        ) ?: return onTap(state)

        val transition = startRoutine(
            state = state.copy(velocity = PetVector.Zero),
            beats = definition.beats,
            comboId = PetComboId.USER_SHOWCASE,
            restartFirstAnimation = true
        )
        return transition.copy(effects = transition.effects + PetEffect.ShowcaseStarted)
    }

    private fun onStartCombo(
        state: PetState,
        event: PetEvent.StartCombo
    ): PetTransition {
        if (state.action in USER_CONTROLLED_ACTIONS) return PetTransition(state)
        val definition = PetComboCatalog.supportedDefinition(
            id = event.comboId,
            supportedActions = config.supportedActions
        ) ?: return PetTransition(state)
        if (definition.habitat == PetComboHabitat.GROUND && !state.isGroundedSurface()) {
            return PetTransition(state)
        }
        val directedState = event.direction?.let { state.copy(direction = it) }
            ?: state.withComboStartDirection(definition.startDirection)
        return startRoutine(
            state = directedState.copy(velocity = PetVector.Zero),
            beats = definition.beats,
            comboId = definition.id,
            restartFirstAnimation = true
        )
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
                state = state.cancelRoutine().copy(velocity = PetVector.Zero),
                action = preferredAction(PetAction.FALL, PetAction.IDLE)
            )
        }
        val direction = when {
            velocity.x > 0f -> PetDirection.RIGHT
            velocity.x < 0f -> PetDirection.LEFT
            else -> state.direction
        }
        return changeAction(
            state = state.cancelRoutine().copy(
                velocity = velocity,
                direction = direction
            ),
            action = PetAction.FLUNG,
            restartAnimation = true
        )
    }

    private fun onTick(state: PetState, requestedElapsedMillis: Long): PetTransition {
        if (requestedElapsedMillis <= 0 || state.action == PetAction.DRAGGED) {
            return PetTransition(state)
        }

        val elapsedMillis = requestedElapsedMillis.coerceAtMost(config.maxTickMillis)
        if (state.isHoldingComboBeatFrame) {
            return advanceHeldComboBeat(state, elapsedMillis)
        }
        val animation = timeline.advance(
            action = state.action,
            cursor = state.animationCursor,
            elapsedMillis = elapsedMillis,
            stopAtActionTransition = state.activeComboBeat != null
        )
        val beat = state.activeComboBeat
        val clipDisplacement = if (beat?.crossScreenDurationMillis == null) {
            animation.displacement.withDirection(state.direction) *
                (beat?.motionMultiplier ?: 1f)
        } else {
            PetVector.Zero
        }
        val scriptedDisplacement = clipDisplacement +
            crossScreenDisplacement(state, beat, elapsedMillis)
        val actionChangedByTimeline = animation.action != state.action
        val beatElapsedMillis = if (state.activeComboBeat == null) {
            0
        } else {
            state.comboBeatElapsedMillis + elapsedMillis
        }
        val holdSustainedBeat = actionChangedByTimeline &&
            state.activeComboBeat?.playback == PetBeatPlayback.HOLD_LAST_FRAME &&
            beatElapsedMillis < state.comboBeatTargetMillis
        val repeatSustainedBeat = actionChangedByTimeline &&
            !holdSustainedBeat &&
            state.activeComboBeat?.isSustained == true &&
            beatElapsedMillis < state.comboBeatTargetMillis
        val nextBeat = if (actionChangedByTimeline && !repeatSustainedBeat && !holdSustainedBeat) {
            state.pendingComboBeats.firstOrNull()
        } else {
            null
        }
        val effects = mutableListOf<PetEffect>()
        var updatedState = when {
            holdSustainedBeat -> {
                val clip = config.clips.getValue(state.action)
                val lastFrameIndex = clip.frames.lastIndex
                val lastFrame = clip.frames[lastFrameIndex]
                state.copy(
                    animationCursor = PetAnimationCursor(
                        frameIndex = lastFrameIndex,
                        elapsedInFrameMillis = lastFrame.durationMillis - 1
                    ),
                    actionElapsedMillis = state.actionElapsedMillis + elapsedMillis,
                    comboBeatElapsedMillis = beatElapsedMillis,
                    isHoldingComboBeatFrame = true
                )
            }

            repeatSustainedBeat -> state.copy(
                animationCursor = PetAnimationCursor(),
                actionElapsedMillis = 0,
                actionTargetMillis = 0,
                comboBeatElapsedMillis = beatElapsedMillis
            )

            nextBeat != null -> {
                val scheduled = scheduleComboBeat(
                    state = state,
                    beat = nextBeat,
                    pendingBeats = state.pendingComboBeats.drop(1)
                )
                val changed = changeAction(
                    state = scheduled,
                    action = nextBeat.action,
                    restartAnimation = true
                )
                effects += changed.effects
                changed.state
            }

            else -> state.copy(
                action = animation.action,
                animationCursor = animation.cursor,
                actionElapsedMillis = if (actionChangedByTimeline) {
                    0
                } else {
                    state.actionElapsedMillis + elapsedMillis
                },
                actionTargetMillis = if (actionChangedByTimeline) 0 else state.actionTargetMillis,
                comboBeatElapsedMillis = beatElapsedMillis
            )
        }
        if (!repeatSustainedBeat && !holdSustainedBeat && nextBeat == null) {
            effects += animation.actionTransitions.map { (from, to) ->
                PetEffect.ActionChanged(from, to)
            }
        }
        if (actionChangedByTimeline && !repeatSustainedBeat && !holdSustainedBeat &&
            nextBeat == null &&
            updatedState.activeComboId != null
        ) {
            val completedCombo = checkNotNull(updatedState.activeComboId)
            updatedState = updatedState.clearComboRuntime()
            effects += PetEffect.ComboCompleted(completedCombo)
        }

        if (updatedState.activeComboBeat?.crossScreenLaunchVelocityY != null) {
            return advanceBallisticFlight(
                updatedState,
                scriptedDisplacement,
                elapsedMillis,
                effects
            )
        }

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
                    state = updatedState.cancelRoutine().copy(velocity = PetVector.Zero),
                    action = collisionAction
                )
                return collided.copy(effects = effects + collided.effects)
            }

            if (constrainedVelocity.magnitude <= config.flingStopSpeed) {
                val stopped = changeAction(
                    state = updatedState.cancelRoutine().copy(velocity = PetVector.Zero),
                    action = preferredAction(PetAction.FALL, PetAction.IDLE)
                )
                return stopped.copy(effects = effects + stopped.effects)
            }
            return PetTransition(updatedState, effects)
        }

        if (updatedState.action == PetAction.FALL) {
            return advanceBallisticFlight(
                updatedState,
                scriptedDisplacement,
                elapsedMillis,
                effects
            )
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
        )?.let { defaultAction -> updatedState.comboCollisionAction(defaultAction) }
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
            return transitionAfterCollision(updatedState, collisionAction, effects)
        }
        return applyLivingBehavior(
            state = updatedState,
            effects = effects
        )
    }

    private fun advanceBallisticFlight(
        state: PetState,
        scriptedDisplacement: PetVector,
        elapsedMillis: Long,
        effects: List<PetEffect>
    ): PetTransition {
        val seconds = elapsedMillis / MILLIS_PER_SECOND
        val oldSpeed = if (state.activeComboBeat?.crossScreenLaunchVelocityY != null) {
            state.velocity.y
        } else {
            state.velocity.y.coerceAtLeast(config.initialFallSpeed)
        }
        val newSpeed = (oldSpeed + config.fallGravity * seconds)
            .coerceAtMost(config.terminalFallSpeed)
        val fallDistance = (oldSpeed + newSpeed) * 0.5f * seconds
        val requestedPosition = state.position + PetVector(
            x = scriptedDisplacement.x,
            y = fallDistance
        )
        val constrainedPosition = state.bounds.clampTopLeft(requestedPosition, state.size)
        val collisionAction = state.comboFlightCollisionAction(
            defaultAction = collisionAction(
                action = PetAction.FALL,
                requested = requestedPosition,
                constrained = constrainedPosition,
                bounds = state.bounds
            ),
            requested = requestedPosition,
            constrained = constrainedPosition
        )
        val falling = state.copy(
            position = constrainedPosition,
            velocity = PetVector(y = newSpeed)
        )
        if (collisionAction == null) return PetTransition(falling, effects)

        return transitionAfterCollision(falling, collisionAction, effects)
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
        val speechRejected = action.isSpeechAction && !state.isGroundedSurface()
        val transitionState = if (speechRejected) state.cancelRoutine() else state
        val resolvedAction = if (speechRejected) {
            preferredAction(PetAction.FALL, PetAction.WALK, PetAction.IDLE)
        } else {
            action
        }
        if (transitionState.action == resolvedAction && !restartAnimation) {
            return PetTransition(transitionState)
        }
        val directedState = if (resolvedAction.isSpeechAction &&
            !transitionState.action.isSpeechAction &&
            transitionState.activeComboId !in SOCIAL_SPEECH_COMBOS
        ) {
            transitionState.faceViewportCenter()
        } else {
            transitionState
        }
        return PetTransition(
            state = directedState.copy(
                action = resolvedAction,
                animationCursor = PetAnimationCursor(),
                actionElapsedMillis = 0,
                actionTargetMillis = 0,
                isHoldingComboBeatFrame = false
            ),
            effects = if (state.action == resolvedAction) {
                emptyList()
            } else {
                listOf(PetEffect.ActionChanged(state.action, resolvedAction))
            }
        )
    }

    private fun PetState.faceViewportCenter(): PetState {
        val petCenterX = position.x + size.width / 2f
        val viewportCenterX = (bounds.left + bounds.right) / 2f
        val talkDirection = if (petCenterX <= viewportCenterX) {
            PetDirection.RIGHT
        } else {
            PetDirection.LEFT
        }
        return copy(direction = talkDirection)
    }

    private fun startRoutine(
        state: PetState,
        beats: List<PetComboBeat>,
        comboId: PetComboId,
        restartFirstAnimation: Boolean = false
    ): PetTransition {
        val supported = beats.filter { it.action in config.supportedActions }
        val first = supported.firstOrNull() ?: PetComboBeat(
            preferredAction(PetAction.WALK, PetAction.IDLE),
            config.behaviorProfile.groundDelayMillis
        )
        val recentCombos = (listOf(comboId) + state.recentComboIds)
            .distinct()
            .take(config.behaviorProfile.recentComboMemory)
        val habitat = PetComboCatalog.definition(comboId)?.habitat ?: PetComboHabitat.GROUND
        val nonClimbComboStreak = if (habitat.isClimb) {
            0
        } else if (state.nonClimbComboStreak == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            state.nonClimbComboStreak + 1
        }
        val scheduled = scheduleComboBeat(
            state = state.copy(
                activeComboId = comboId,
                recentComboIds = recentCombos,
                nonClimbComboStreak = nonClimbComboStreak,
                behaviorSequence = state.behaviorSequence + 1
            ),
            beat = first,
            pendingBeats = supported.drop(1)
        )
        val changed = changeAction(
            state = scheduled,
            action = first.action,
            restartAnimation = restartFirstAnimation
        )
        return changed.copy(effects = changed.effects + PetEffect.ComboStarted(comboId))
    }

    private fun scheduleComboBeat(
        state: PetState,
        beat: PetComboBeat,
        pendingBeats: List<PetComboBeat>
    ): PetState {
        val directedState = when (beat.directionChange) {
            PetBeatDirectionChange.KEEP -> state
            PetBeatDirectionChange.REVERSE -> state.copy(direction = state.direction.opposite())
        }
        val launchedState = beat.crossScreenLaunchVelocityY?.let { velocityY ->
            directedState.copy(velocity = PetVector(y = velocityY))
        } ?: directedState
        val duration = beat.durationMillis
        val scheduled = when {
            beat.completion == PetBeatCompletion.COLLISION -> RandomDraw(
                collisionBeatTimeoutMillis(launchedState, beat),
                launchedState
            )
            duration == null -> RandomDraw(0L, launchedState)
            else -> draw(launchedState, duration, COMBO_BEAT_DURATION_SALT)
        }
        return scheduled.state.copy(
            activeComboBeat = beat,
            comboBeatElapsedMillis = 0,
            comboBeatTargetMillis = scheduled.value,
            isHoldingComboBeatFrame = false,
            pendingComboBeats = pendingBeats
        )
    }

    private fun advanceHeldComboBeat(
        state: PetState,
        elapsedMillis: Long
    ): PetTransition {
        val elapsed = state.comboBeatElapsedMillis + elapsedMillis
        val held = state.copy(
            actionElapsedMillis = state.actionElapsedMillis + elapsedMillis,
            comboBeatElapsedMillis = elapsed
        )
        if (elapsed < state.comboBeatTargetMillis) {
            return PetTransition(held)
        }
        return advanceComboBeatOrFallback(
            state = held,
            fallbackAction = preferredAction(PetAction.WALK, PetAction.IDLE),
            effects = emptyList()
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

    private fun PetState.withComboStartDirection(
        startDirection: PetComboStartDirection
    ): PetState = when (startDirection) {
        PetComboStartDirection.KEEP -> this
        PetComboStartDirection.REVERSE -> copy(direction = direction.opposite())
        PetComboStartDirection.NEAREST_WALL -> {
            val maximumX = (bounds.right - size.width).coerceAtLeast(bounds.left)
            val distanceToLeft = position.x - bounds.left
            val distanceToRight = maximumX - position.x
            copy(
                direction = if (distanceToLeft <= distanceToRight) {
                    PetDirection.LEFT
                } else {
                    PetDirection.RIGHT
                }
            )
        }
    }

    private fun transitionAfterCollision(
        state: PetState,
        collisionAction: PetAction,
        effects: List<PetEffect>
    ): PetTransition {
        val nextBeat = state.pendingComboBeats.firstOrNull()
        if (nextBeat?.action == collisionAction) {
            val scheduled = scheduleComboBeat(
                state = state.copy(velocity = PetVector.Zero),
                beat = nextBeat,
                pendingBeats = state.pendingComboBeats.drop(1)
            )
            val changed = changeAction(
                state = scheduled,
                action = collisionAction,
                restartAnimation = true
            )
            return changed.copy(effects = effects + changed.effects)
        }

        val collided = changeAction(
            state = state.cancelRoutine().copy(velocity = PetVector.Zero),
            action = collisionAction
        )
        return collided.copy(effects = effects + collided.effects)
    }

    private fun PetState.comboCollisionAction(defaultAction: PetAction): PetAction {
        val nextAction = pendingComboBeats.firstOrNull()?.action
        return when {
            nextAction == PetAction.JUMP &&
                (action == PetAction.CLIMB_WALL || action == PetAction.CLIMB_CEILING) -> {
                PetAction.JUMP
            }
            nextAction == PetAction.DANGLE && action == PetAction.CLIMB_WALL -> PetAction.DANGLE
            else -> defaultAction
        }
    }

    private fun PetState.comboFlightCollisionAction(
        defaultAction: PetAction?,
        requested: PetVector,
        constrained: PetVector
    ): PetAction? {
        val crossingToWall = activeComboBeat?.crossScreenDurationMillis != null &&
            pendingComboBeats.firstOrNull()?.action == PetAction.CLIMB_WALL
        val hitHorizontalEdge = requested.x != constrained.x
        return if (crossingToWall && hitHorizontalEdge) {
            PetAction.CLIMB_WALL
        } else {
            defaultAction
        }
    }

    private fun crossScreenDisplacement(
        state: PetState,
        beat: PetComboBeat?,
        elapsedMillis: Long
    ): PetVector {
        val durationMillis = beat?.crossScreenDurationMillis ?: return PetVector.Zero
        val travelWidth = (state.bounds.right - state.bounds.left - state.size.width)
            .coerceAtLeast(0f)
        val velocity = travelWidth / (durationMillis / MILLIS_PER_SECOND)
        return PetVector(x = velocity * (elapsedMillis / MILLIS_PER_SECOND))
            .withDirection(state.direction)
    }

    private fun collisionBeatTimeoutMillis(state: PetState, beat: PetComboBeat): Long {
        val action = beat.action
        val horizontal = beat.crossScreenDurationMillis != null ||
            action in GROUND_MOVEMENT_ACTIONS ||
            action == PetAction.CLIMB_CEILING
        val distance = if (horizontal) {
            val maximumX = (state.bounds.right - state.size.width)
                .coerceAtLeast(state.bounds.left)
            when (state.direction) {
                PetDirection.LEFT -> state.position.x - state.bounds.left
                PetDirection.RIGHT -> maximumX - state.position.x
            }
        } else {
            state.position.y - state.bounds.top
        }.coerceAtLeast(0f)
        val speed = beat.crossScreenDurationMillis?.let { durationMillis ->
            val travelWidth = (state.bounds.right - state.bounds.left - state.size.width)
                .coerceAtLeast(0f)
            travelWidth / (durationMillis / MILLIS_PER_SECOND)
        } ?: (config.clips.getValue(action).frames.maxOf { frame ->
            abs(if (horizontal) frame.velocity.x else frame.velocity.y)
        } * beat.motionMultiplier)
        if (speed <= 0f) return MIN_COLLISION_BEAT_TIMEOUT_MILLIS
        val travelMillis = ceil(distance / speed * MILLIS_PER_SECOND).toLong()
        return (travelMillis + COLLISION_BEAT_GRACE_MILLIS).coerceIn(
            MIN_COLLISION_BEAT_TIMEOUT_MILLIS,
            MAX_COLLISION_BEAT_TIMEOUT_MILLIS
        )
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
        action in GROUND_MOVEMENT_ACTIONS && hitHorizontalEdge &&
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
            PetAction.RUN,
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
            PetAction.CLIMB_DOWN -> if (hitBottom || constrained.y >= bounds.bottom) {
                preferredAction(PetAction.WALK, PetAction.IDLE)
            } else {
                null
            }
            PetAction.CLIMB_CEILING -> if (hitLeft || hitRight) {
                preferredAction(PetAction.CLIMB_DOWN, PetAction.FALL, PetAction.WALK)
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
    ): PetTransition {
        val comboBeat = state.activeComboBeat
        if (comboBeat != null && comboBeat.action == state.action &&
            config.clips.getValue(state.action).loops
        ) {
            return applyLoopingComboBeat(state, effects)
        }
        return when (state.action) {
            PetAction.IDLE -> timedTransition(
                state,
                config.behaviorProfile.idleDurationMillis,
                preferredAction(PetAction.WALK, PetAction.IDLE),
                effects,
                IDLE_DURATION_SALT
            )

            PetAction.WALK -> applyGroundBehavior(state, effects)
            PetAction.RUN -> timedTransition(
                state,
                config.behaviorProfile.runDurationMillis,
                preferredAction(PetAction.WALK, PetAction.IDLE),
                effects,
                RUN_DURATION_SALT
            )
            PetAction.CREEP -> timedTransition(
                state,
                config.behaviorProfile.creepDurationMillis,
                preferredAction(PetAction.WALK, PetAction.IDLE),
                effects,
                CREEP_DURATION_SALT
            )

            PetAction.CLIMB_WALL -> applyWallTimeout(state, effects)
            PetAction.CLIMB_DOWN -> timedTransition(
                state,
                config.behaviorProfile.wallDurationMillis,
                preferredAction(PetAction.FALL, PetAction.WALK),
                effects,
                CLIMB_DOWN_DURATION_SALT
            )
            PetAction.CLIMB_CEILING -> timedTransition(
                state,
                config.behaviorProfile.ceilingDurationMillis,
                preferredAction(PetAction.FALL, PetAction.WALK),
                effects,
                CEILING_DURATION_SALT
            )

            else -> PetTransition(state, effects)
        }
    }

    private fun applyLoopingComboBeat(
        state: PetState,
        effects: List<PetEffect>
    ): PetTransition {
        if (state.activeComboBeat?.completion == PetBeatCompletion.COLLISION) {
            if (state.comboBeatElapsedMillis < state.comboBeatTargetMillis) {
                return PetTransition(state, effects)
            }
            val fallback = changeAction(
                state = state.cancelRoutine(),
                action = preferredAction(PetAction.WALK, PetAction.IDLE)
            )
            return fallback.copy(effects = effects + fallback.effects)
        }
        if (state.comboBeatTargetMillis > 0 &&
            state.comboBeatElapsedMillis < state.comboBeatTargetMillis
        ) {
            return PetTransition(state, effects)
        }
        return advanceComboBeatOrFallback(
            state = state,
            fallbackAction = preferredAction(PetAction.WALK, PetAction.IDLE),
            effects = effects
        )
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

        val supportedRules = config.behaviorProfile.autonomousComboRules.mapNotNull { rule ->
            PetComboCatalog.supportedDefinition(rule.comboId, config.supportedActions)
                ?.let { definition -> rule to definition }
        }
        val climbRules = supportedRules.filter { (_, definition) ->
            definition.habitat.isClimb
        }
        val habitatRules = if (scheduled.nonClimbComboStreak >=
            config.behaviorProfile.maxNonClimbCombosBeforeClimb && climbRules.isNotEmpty()
        ) {
            climbRules
        } else {
            supportedRules
        }
        val freshRules = habitatRules.filterNot { (rule, _) ->
            rule.comboId in scheduled.recentComboIds
        }
        val eligibleRules = freshRules.ifEmpty { habitatRules }
        val totalWeight = eligibleRules.sumOf { (rule, _) -> rule.weight }
        if (totalWeight <= 0) {
            return PetTransition(scheduled.resetActionTimer(), effects)
        }

        val draw = draw(scheduled, 0 until totalWeight, GROUND_CHOICE_SALT)
        var cursor = draw.value
        val (_, selected) = eligibleRules.first { (rule, _) ->
            if (cursor < rule.weight) {
                true
            } else {
                cursor -= rule.weight
                false
            }
        }
        val directedState = draw.state.withComboStartDirection(selected.startDirection)
        val changed = startRoutine(
            state = directedState,
            beats = selected.beats,
            comboId = selected.id
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
        val jumpThreshold = config.behaviorProfile.wallJumpChancePercent
        val descendThreshold = jumpThreshold + config.behaviorProfile.wallDescendChancePercent
        val nextAction = when {
            canJump && chance.value < jumpThreshold -> PetAction.JUMP
            PetAction.CLIMB_DOWN in config.supportedActions && chance.value < descendThreshold -> {
                PetAction.CLIMB_DOWN
            }
            else -> preferredAction(PetAction.FALL, PetAction.WALK)
        }
        val exiting = if (nextAction == PetAction.JUMP) {
            chance.state.copy(direction = chance.state.direction.opposite())
        } else {
            chance.state
        }
        val changed = changeAction(exiting.cancelRoutine(), nextAction)
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
        return advanceComboBeatOrFallback(
            state = scheduled,
            fallbackAction = nextAction,
            effects = effects
        )
    }

    private fun advanceComboBeatOrFallback(
        state: PetState,
        fallbackAction: PetAction,
        effects: List<PetEffect>
    ): PetTransition {
        val nextBeat = state.pendingComboBeats.firstOrNull()
        if (nextBeat != null) {
            val scheduled = scheduleComboBeat(
                state = state,
                beat = nextBeat,
                pendingBeats = state.pendingComboBeats.drop(1)
            )
            val changed = changeAction(
                state = scheduled,
                action = nextBeat.action,
                restartAnimation = true
            )
            return changed.copy(effects = effects + changed.effects)
        }

        val completed = completeCombo(state, effects)
        val changed = changeAction(completed.state, fallbackAction)
        return changed.copy(effects = completed.effects + changed.effects)
    }

    private fun completeCombo(
        state: PetState,
        effects: List<PetEffect>
    ): PetTransition {
        val comboId = state.activeComboId ?: return PetTransition(state, effects)
        return PetTransition(
            state = state.clearComboRuntime(),
            effects = effects + PetEffect.ComboCompleted(comboId)
        )
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

    private fun PetState.cancelRoutine(): PetState = clearComboRuntime()

    private fun PetState.clearComboRuntime(): PetState = copy(
        activeComboId = null,
        activeComboBeat = null,
        comboBeatElapsedMillis = 0,
        comboBeatTargetMillis = 0,
        isHoldingComboBeatFrame = false,
        pendingComboBeats = emptyList()
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
        val SOCIAL_SPEECH_COMBOS = setOf(
            PetComboId.SOCIAL_HELLO,
            PetComboId.SOCIAL_HELLO_REPLY,
            PetComboId.SOCIAL_SHOW_OFF,
            PetComboId.SOCIAL_ADMIRE
        )
        const val MILLIS_PER_SECOND = 1_000f
        const val PERCENT_MAX = 100
        const val RANDOM_SEQUENCE_MULTIPLIER = 1_103_515_245L
        const val GROUND_DELAY_SALT = 0x101L
        const val GROUND_CHOICE_SALT = 0x102L
        const val IDLE_DURATION_SALT = 0x201L
        const val RUN_DURATION_SALT = 0x202L
        const val CREEP_DURATION_SALT = 0x301L
        const val WALL_DURATION_SALT = 0x401L
        const val WALL_EXIT_SALT = 0x402L
        const val CLIMB_DOWN_DURATION_SALT = 0x403L
        const val CEILING_DURATION_SALT = 0x501L
        const val COMBO_BEAT_DURATION_SALT = 0x601L
        const val COLLISION_BEAT_GRACE_MILLIS = 3_000L
        const val MIN_COLLISION_BEAT_TIMEOUT_MILLIS = 5_000L
        const val MAX_COLLISION_BEAT_TIMEOUT_MILLIS = 90_000L
        val TAP_LOOP_DURATION = 800L..1_200L
        val TAP_RECOVERY_DURATION = 1_500L..2_500L
        val GROUND_MOVEMENT_ACTIONS = setOf(
            PetAction.WALK,
            PetAction.RUN,
            PetAction.CREEP,
            PetAction.TALK_WALK
        )
        val USER_CONTROLLED_ACTIONS = setOf(
            PetAction.DRAGGED,
            PetAction.FLUNG,
            PetAction.FALL
        )
    }
}
