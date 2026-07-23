package com.asianmobile.privatebrower.pet.engine

import kotlin.math.max

/**
 * Repairs deep overlap only after autonomous pets have both settled into resting poses.
 *
 * Moving pets intentionally do not participate: they may cross each other unless a
 * [PetSocialDirector] session has explicitly paired them. This keeps visual overlap from
 * becoming an invisible wall or a source of forced direction changes.
 */
class PetCrowdResolver(
    private val minimumGapInPetWidths: Float = 0.05f,
    private val overlapRepairThresholdInPetWidths: Float = 0.55f,
    private val floorToleranceInPetHeights: Float = 0.45f
) {
    init {
        require(minimumGapInPetWidths >= 0f) { "minimum pet gap must not be negative" }
        require(overlapRepairThresholdInPetWidths in 0f..1f) {
            "overlap repair threshold must be between zero and one pet width"
        }
        require(floorToleranceInPetHeights >= 0f) {
            "crowd floor tolerance must not be negative"
        }
    }

    fun resolve(states: List<PetState>): List<PetState> {
        if (states.size < 2) return states
        val resolved = states.toMutableList()
        val resting = states.indices.filter { index -> states[index].usesRestingPersonalSpace() }
        if (resting.size < 2) return states

        repeat(resting.size) {
            val ordered = resting.sortedWith(
                compareBy<Int> { index -> centerX(resolved[index]) }
                    .thenBy { index -> index }
            )
            collisionClusters(ordered, resolved).forEach { cluster ->
                resolveCluster(cluster, resolved)
            }
        }
        return resolved
    }

    private fun collisionClusters(
        ordered: List<Int>,
        states: List<PetState>
    ): List<List<Int>> {
        val clusters = mutableListOf<MutableList<Int>>()
        ordered.forEach { index ->
            val current = clusters.lastOrNull()
            if (current == null) {
                clusters += mutableListOf(index)
                return@forEach
            }
            val previous = states[current.last()]
            val next = states[index]
            val sharedWidth = max(previous.size.width, next.size.width)
            val overlap = previous.position.x + previous.size.width - next.position.x
            val overlaps = overlap >
                sharedWidth * overlapRepairThresholdInPetWidths + POSITION_TOLERANCE
            if (overlaps) {
                current += index
            } else {
                clusters += mutableListOf(index)
            }
        }
        return clusters.filter { cluster -> cluster.size > 1 }
    }

    private fun resolveCluster(
        cluster: List<Int>,
        states: MutableList<PetState>
    ) {
        val clusterStates = cluster.map(states::get)
        val leftBound = clusterStates.maxOf { state -> state.bounds.left }
        val rightBound = clusterStates.minOf { state -> state.bounds.right }
        val widths = clusterStates.sumOf { state -> state.size.width.toDouble() }.toFloat()
        val desiredGaps = clusterStates.zipWithNext().sumOf { (left, right) ->
            (max(left.size.width, right.size.width) * minimumGapInPetWidths).toDouble()
        }.toFloat()
        val availableWidth = (rightBound - leftBound).coerceAtLeast(0f)
        val gapScale = if (desiredGaps <= 0f || widths + desiredGaps <= availableWidth) {
            1f
        } else {
            ((availableWidth - widths) / desiredGaps).coerceIn(0f, 1f)
        }
        val span = (widths + desiredGaps * gapScale).coerceAtMost(availableWidth)
        val averageCenter = clusterStates.map(::centerX).average().toFloat()
        val maximumStart = (rightBound - span).coerceAtLeast(leftBound)
        var cursor = (averageCenter - span * 0.5f).coerceIn(leftBound, maximumStart)

        cluster.forEachIndexed { position, stateIndex ->
            val state = states[stateIndex]
            states[stateIndex] = state.separated(cursor)
            if (position < cluster.lastIndex) {
                val next = states[cluster[position + 1]]
                cursor += state.size.width +
                    max(state.size.width, next.size.width) * minimumGapInPetWidths * gapScale
            }
        }
    }

    private fun PetState.usesRestingPersonalSpace(): Boolean =
        action in RESTING_ACTIONS &&
            activeComboId !in SOCIAL_COMBO_IDS &&
            isGroundedSurface(floorToleranceInPetHeights)

    private fun PetState.separated(x: Float): PetState {
        val correctedPosition = bounds.clampTopLeft(position.copy(x = x), size)
        return copy(position = correctedPosition)
    }

    private fun centerX(state: PetState): Float = state.position.x + state.size.width * 0.5f

    private companion object {
        const val POSITION_TOLERANCE = 0.01f

        val RESTING_ACTIONS = setOf(
            PetAction.IDLE,
            PetAction.SIT,
            PetAction.WINK,
            PetAction.EMOTE,
            PetAction.LOOK_UP,
            PetAction.DANGLE,
            PetAction.FLOOR_PLAY,
            PetAction.SPRAWL,
            PetAction.TALK,
            PetAction.SPECIAL,
            PetAction.SPECIAL_2
        )
        val SOCIAL_COMBO_IDS = setOf(
            PetComboId.SOCIAL_APPROACH,
            PetComboId.SOCIAL_HELLO,
            PetComboId.SOCIAL_HELLO_REPLY,
            PetComboId.SOCIAL_CHASE_LEADER,
            PetComboId.SOCIAL_CHASE_FOLLOWER,
            PetComboId.SOCIAL_SHOW_OFF,
            PetComboId.SOCIAL_ADMIRE,
            PetComboId.SOCIAL_REST_A,
            PetComboId.SOCIAL_REST_B,
            PetComboId.SOCIAL_COPYCAT_A,
            PetComboId.SOCIAL_COPYCAT_B,
            PetComboId.SOCIAL_DUET_A,
            PetComboId.SOCIAL_DUET_B
        )
    }
}
