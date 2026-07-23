package com.asianmobile.privatebrower.pet.engine

import kotlin.math.abs
import kotlin.math.max

enum class PetSocialScene {
    GREETING,
    PLAY_CHASE,
    SHOW_AND_REACT,
    REST_TOGETHER,
    COPYCAT,
    DUET_DANCE
}

data class PetSocialSnapshot(
    val id: Int,
    val state: PetState
)

sealed interface PetSocialDirective {
    val petId: Int

    data class Face(
        override val petId: Int,
        val direction: PetDirection
    ) : PetSocialDirective

    data class StartCombo(
        override val petId: Int,
        val comboId: PetComboId,
        val direction: PetDirection
    ) : PetSocialDirective
}

data class PetSocialConfig(
    val initialDelayMillis: Long = 6_000,
    val interactionCooldownMillis: Long = 10_000,
    val retryDelayMillis: Long = 3_000,
    val approachTimeoutMillis: Long = 12_000,
    val performanceTimeoutMillis: Long = 45_000,
    val meetDistanceInPetWidths: Float = 1.35f,
    val floorToleranceInPetHeights: Float = 0.45f,
    val verticalToleranceInPetHeights: Float = 0.75f
) {
    init {
        require(initialDelayMillis >= 0) { "initial social delay must not be negative" }
        require(interactionCooldownMillis >= 0) { "social cooldown must not be negative" }
        require(retryDelayMillis >= 0) { "social retry delay must not be negative" }
        require(approachTimeoutMillis > 0) { "approach timeout must be positive" }
        require(performanceTimeoutMillis > 0) { "performance timeout must be positive" }
        require(meetDistanceInPetWidths > 0f) { "meet distance must be positive" }
        require(floorToleranceInPetHeights >= 0f) { "floor tolerance must not be negative" }
        require(verticalToleranceInPetHeights >= 0f) {
            "vertical tolerance must not be negative"
        }
    }
}

class PetSocialDirector(
    private val config: PetSocialConfig = PetSocialConfig(),
    private val sceneOffset: Int = 0
) {
    private var cooldownMillis = config.initialDelayMillis
    private var sceneCursor = sceneOffset.mod(PetSocialScene.entries.size)
    private var session: SocialSession? = null

    fun reset() {
        cooldownMillis = config.initialDelayMillis
        sceneCursor = sceneOffset.mod(PetSocialScene.entries.size)
        session = null
    }

    fun update(
        pets: List<PetSocialSnapshot>,
        elapsedMillis: Long
    ): List<PetSocialDirective> {
        if (elapsedMillis <= 0) return emptyList()
        cooldownMillis = (cooldownMillis - elapsedMillis).coerceAtLeast(0)

        val activeSession = session
        if (activeSession == null) {
            if (cooldownMillis > 0) return emptyList()
            return beginSession(pets)
        }

        val first = pets.firstOrNull { it.id == activeSession.firstPetId }
        val second = pets.firstOrNull { it.id == activeSession.secondPetId }
        if (first == null || second == null || !canContinue(first.state) ||
            !canContinue(second.state)
        ) {
            endSession(config.retryDelayMillis)
            return emptyList()
        }

        return when (activeSession.phase) {
            SocialPhase.APPROACHING -> updateApproach(activeSession, first, second, elapsedMillis)
            SocialPhase.PERFORMING -> updatePerformance(
                activeSession,
                first,
                second,
                elapsedMillis
            )
        }
    }

    private fun beginSession(pets: List<PetSocialSnapshot>): List<PetSocialDirective> {
        val candidates = pets.filter { isAvailable(it.state) }
        val pair = closestPair(candidates) ?: run {
            cooldownMillis = config.retryDelayMillis
            return emptyList()
        }
        val scene = PetSocialScene.entries[sceneCursor]
        sceneCursor = (sceneCursor + 1) % PetSocialScene.entries.size
        val newSession = SocialSession(
            firstPetId = pair.first.id,
            secondPetId = pair.second.id,
            scene = scene,
            phase = SocialPhase.APPROACHING
        )
        session = newSession
        return approachDirectives(pair.first, pair.second, forceCombo = true)
    }

    private fun updateApproach(
        current: SocialSession,
        first: PetSocialSnapshot,
        second: PetSocialSnapshot,
        elapsedMillis: Long
    ): List<PetSocialDirective> {
        val elapsed = current.elapsedMillis + elapsedMillis
        val meetDistance = max(first.state.size.width, second.state.size.width) *
            config.meetDistanceInPetWidths
        if (horizontalCenterDistance(first.state, second.state) <= meetDistance) {
            val performing = current.copy(
                phase = SocialPhase.PERFORMING,
                elapsedMillis = 0
            )
            session = performing
            return performanceDirectives(performing.scene, first, second)
        }
        if (elapsed >= config.approachTimeoutMillis) {
            endSession(config.retryDelayMillis)
            return emptyList()
        }

        session = current.copy(elapsedMillis = elapsed)
        val shouldRestartCombo = first.state.activeComboId != PetComboId.SOCIAL_APPROACH ||
            second.state.activeComboId != PetComboId.SOCIAL_APPROACH
        return approachDirectives(first, second, forceCombo = shouldRestartCombo)
    }

    private fun updatePerformance(
        current: SocialSession,
        first: PetSocialSnapshot,
        second: PetSocialSnapshot,
        elapsedMillis: Long
    ): List<PetSocialDirective> {
        val elapsed = current.elapsedMillis + elapsedMillis
        val (firstCombo, secondCombo) = comboPair(current.scene)
        val completed = elapsed >= MIN_PERFORMANCE_MILLIS &&
            first.state.activeComboId != firstCombo &&
            second.state.activeComboId != secondCombo
        if (completed || elapsed >= config.performanceTimeoutMillis) {
            endSession(config.interactionCooldownMillis)
            return emptyList()
        }

        session = current.copy(elapsedMillis = elapsed)
        return facingDirectives(current.scene, first, second)
    }

    private fun approachDirectives(
        first: PetSocialSnapshot,
        second: PetSocialSnapshot,
        forceCombo: Boolean
    ): List<PetSocialDirective> {
        val firstDirection = directionToward(first.state, second.state)
        val secondDirection = directionToward(second.state, first.state)
        return if (forceCombo) {
            listOf(
                PetSocialDirective.StartCombo(
                    first.id,
                    PetComboId.SOCIAL_APPROACH,
                    firstDirection
                ),
                PetSocialDirective.StartCombo(
                    second.id,
                    PetComboId.SOCIAL_APPROACH,
                    secondDirection
                )
            )
        } else {
            listOf(
                PetSocialDirective.Face(first.id, firstDirection),
                PetSocialDirective.Face(second.id, secondDirection)
            )
        }
    }

    private fun performanceDirectives(
        scene: PetSocialScene,
        first: PetSocialSnapshot,
        second: PetSocialSnapshot
    ): List<PetSocialDirective> {
        val (firstCombo, secondCombo) = comboPair(scene)
        val (firstDirection, secondDirection) = performanceDirections(scene, first, second)
        return listOf(
            PetSocialDirective.StartCombo(first.id, firstCombo, firstDirection),
            PetSocialDirective.StartCombo(second.id, secondCombo, secondDirection)
        )
    }

    private fun facingDirectives(
        scene: PetSocialScene,
        first: PetSocialSnapshot,
        second: PetSocialSnapshot
    ): List<PetSocialDirective> {
        val (firstDirection, secondDirection) = performanceDirections(scene, first, second)
        return listOf(
            PetSocialDirective.Face(first.id, firstDirection),
            PetSocialDirective.Face(second.id, secondDirection)
        )
    }

    private fun performanceDirections(
        scene: PetSocialScene,
        first: PetSocialSnapshot,
        second: PetSocialSnapshot
    ): Pair<PetDirection, PetDirection> {
        if (scene != PetSocialScene.PLAY_CHASE) {
            return directionToward(first.state, second.state) to
                directionToward(second.state, first.state)
        }
        val averageCenter = (centerX(first.state) + centerX(second.state)) * 0.5f
        val screenCenter = (first.state.bounds.left + first.state.bounds.right) * 0.5f
        val chaseDirection = if (averageCenter <= screenCenter) {
            PetDirection.RIGHT
        } else {
            PetDirection.LEFT
        }
        return chaseDirection to chaseDirection
    }

    private fun comboPair(scene: PetSocialScene): Pair<PetComboId, PetComboId> = when (scene) {
        PetSocialScene.GREETING ->
            PetComboId.SOCIAL_HELLO to PetComboId.SOCIAL_HELLO_REPLY
        PetSocialScene.PLAY_CHASE ->
            PetComboId.SOCIAL_CHASE_LEADER to PetComboId.SOCIAL_CHASE_FOLLOWER
        PetSocialScene.SHOW_AND_REACT ->
            PetComboId.SOCIAL_SHOW_OFF to PetComboId.SOCIAL_ADMIRE
        PetSocialScene.REST_TOGETHER ->
            PetComboId.SOCIAL_REST_A to PetComboId.SOCIAL_REST_B
        PetSocialScene.COPYCAT ->
            PetComboId.SOCIAL_COPYCAT_A to PetComboId.SOCIAL_COPYCAT_B
        PetSocialScene.DUET_DANCE ->
            PetComboId.SOCIAL_DUET_A to PetComboId.SOCIAL_DUET_B
    }

    private fun closestPair(
        pets: List<PetSocialSnapshot>
    ): Pair<PetSocialSnapshot, PetSocialSnapshot>? {
        var closest: Pair<PetSocialSnapshot, PetSocialSnapshot>? = null
        var closestDistance = Float.MAX_VALUE
        pets.forEachIndexed { index, first ->
            for (secondIndex in index + 1 until pets.size) {
                val second = pets[secondIndex]
                val maxVerticalDistance = max(first.state.size.height, second.state.size.height) *
                    config.verticalToleranceInPetHeights
                if (abs(centerY(first.state) - centerY(second.state)) > maxVerticalDistance) {
                    continue
                }
                val distance = horizontalCenterDistance(first.state, second.state)
                if (distance < closestDistance) {
                    closestDistance = distance
                    closest = first to second
                }
            }
        }
        return closest
    }

    private fun isAvailable(state: PetState): Boolean =
        state.activeComboId == null &&
            canContinue(state) &&
            abs(state.position.y - (state.bounds.bottom - state.size.height)) <=
            state.size.height * config.floorToleranceInPetHeights

    private fun canContinue(state: PetState): Boolean = state.action !in INTERRUPTING_ACTIONS

    private fun directionToward(from: PetState, to: PetState): PetDirection =
        if (centerX(to) < centerX(from)) PetDirection.LEFT else PetDirection.RIGHT

    private fun horizontalCenterDistance(first: PetState, second: PetState): Float =
        abs(centerX(first) - centerX(second))

    private fun centerX(state: PetState): Float = state.position.x + state.size.width * 0.5f

    private fun centerY(state: PetState): Float = state.position.y + state.size.height * 0.5f

    private fun endSession(nextDelayMillis: Long) {
        session = null
        cooldownMillis = nextDelayMillis
    }

    private data class SocialSession(
        val firstPetId: Int,
        val secondPetId: Int,
        val scene: PetSocialScene,
        val phase: SocialPhase,
        val elapsedMillis: Long = 0
    )

    private enum class SocialPhase {
        APPROACHING,
        PERFORMING
    }

    private companion object {
        const val MIN_PERFORMANCE_MILLIS = 500L
        val INTERRUPTING_ACTIONS = setOf(
            PetAction.DRAGGED,
            PetAction.FLUNG,
            PetAction.FALL,
            PetAction.CLIMB_WALL,
            PetAction.CLIMB_DOWN,
            PetAction.CLIMB_CEILING,
            PetAction.JUMP
        )
    }
}
