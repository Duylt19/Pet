package com.asianmobile.privatebrower.pet.engine

import kotlin.math.abs
import kotlin.math.max

class PetCrowdResolver(
    private val minimumGapInPetWidths: Float = 0.05f,
    private val floorToleranceInPetHeights: Float = 0.45f
) {
    init {
        require(minimumGapInPetWidths >= 0f) { "minimum pet gap must not be negative" }
        require(floorToleranceInPetHeights >= 0f) {
            "crowd floor tolerance must not be negative"
        }
    }

    fun resolve(states: List<PetState>): List<PetState> {
        if (states.size < 2) return states
        val resolved = states.toMutableList()
        val grounded = states.indices.filter { index -> states[index].usesFloorPersonalSpace() }
        if (grounded.size < 2) return states

        repeat(grounded.size) {
            val ordered = grounded.sortedWith(
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
            val gap = max(previous.size.width, next.size.width) * minimumGapInPetWidths
            val overlaps = next.position.x <
                previous.position.x + previous.size.width + gap - POSITION_TOLERANCE
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
        val clusterCenter = cursor + span * 0.5f

        cluster.forEachIndexed { position, stateIndex ->
            val state = states[stateIndex]
            val outwardDirection = if (
                centerX(state) < clusterCenter ||
                (centerX(state) == clusterCenter && position < cluster.size / 2)
            ) {
                PetDirection.LEFT
            } else {
                PetDirection.RIGHT
            }
            states[stateIndex] = state.separated(cursor, outwardDirection)
            if (position < cluster.lastIndex) {
                val next = states[cluster[position + 1]]
                cursor += state.size.width +
                    max(state.size.width, next.size.width) * minimumGapInPetWidths * gapScale
            }
        }
    }

    private fun PetState.usesFloorPersonalSpace(): Boolean {
        if (action in AIRBORNE_OR_CONTROLLED_ACTIONS) return false
        val floorY = bounds.bottom - size.height
        return abs(position.y - floorY) <= size.height * floorToleranceInPetHeights
    }

    private fun PetState.separated(
        x: Float,
        outwardDirection: PetDirection
    ): PetState {
        val correctedPosition = bounds.clampTopLeft(position.copy(x = x), size)
        val correctedDirection = if (
            action in GROUND_MOVEMENT_ACTIONS && activeComboId !in SOCIAL_COMBO_IDS
        ) {
            outwardDirection
        } else {
            direction
        }
        return copy(position = correctedPosition, direction = correctedDirection)
    }

    private fun centerX(state: PetState): Float = state.position.x + state.size.width * 0.5f

    private companion object {
        const val POSITION_TOLERANCE = 0.01f

        val AIRBORNE_OR_CONTROLLED_ACTIONS = setOf(
            PetAction.FALL,
            PetAction.JUMP,
            PetAction.FLUNG,
            PetAction.DRAGGED,
            PetAction.CLIMB_WALL,
            PetAction.CLIMB_DOWN,
            PetAction.CLIMB_CEILING,
            PetAction.DANGLE
        )
        val GROUND_MOVEMENT_ACTIONS = setOf(
            PetAction.WALK,
            PetAction.RUN,
            PetAction.CREEP,
            PetAction.TALK_WALK
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
