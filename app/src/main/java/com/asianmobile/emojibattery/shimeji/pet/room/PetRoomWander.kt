package com.asianmobile.emojibattery.shimeji.pet.room

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/**
 * The walkable floor of a room, seen in perspective: the back edge is narrower than the front,
 * so pets that wander upstage also move inward instead of sliding along a wall.
 */
data class PetRoomFloor(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val backInsetFraction: Float = DEFAULT_BACK_INSET
) {
    private val width: Float get() = (right - left).coerceAtLeast(1f)
    private val depthSpan: Float get() = (bottom - top).coerceAtLeast(1f)

    /** 0 at the back wall, 1 at the front of the room. */
    fun depthAt(y: Float): Float = ((y - top) / depthSpan).coerceIn(0f, 1f)

    fun leftAt(y: Float): Float = left + width * backInsetFraction * (1f - depthAt(y))

    fun rightAt(y: Float): Float = right - width * backInsetFraction * (1f - depthAt(y))

    /** Pets further back read as further away, so they are drawn smaller. */
    fun scaleAt(y: Float): Float =
        BACK_SCALE + (FRONT_SCALE - BACK_SCALE) * depthAt(y)

    fun clampX(x: Float, y: Float): Float = x.coerceIn(leftAt(y), rightAt(y))

    fun clampY(y: Float): Float = y.coerceIn(top, bottom)

    private companion object {
        const val DEFAULT_BACK_INSET = 0.14f
        const val BACK_SCALE = 0.78f
        const val FRONT_SCALE = 1f
    }
}

data class PetRoomWanderState(
    val x: Float,
    val y: Float,
    val targetX: Float,
    val targetY: Float,
    val facingRight: Boolean,
    val isWalking: Boolean,
    val phaseRemainingMillis: Long
)

/**
 * Moves one pet around the floor: pick a spot, stroll to it, stand for a while, pick another.
 * Deterministic for a given seed so the room looks the same after a recomposition.
 */
class PetRoomWanderer(
    seed: Long,
    private val floor: PetRoomFloor,
    private val walkSpeedPerSecond: Float
) {
    private val random = Random(seed)

    fun initial(index: Int, count: Int): PetRoomWanderState {
        // Spread the first arrivals across the floor instead of stacking them on one line.
        val column = (index + 0.5f) / count.coerceAtLeast(1)
        val y = floor.clampY(floor.top + (floor.bottom - floor.top) * random.nextFloat())
        val x = floor.clampX(floor.leftAt(y) + (floor.rightAt(y) - floor.leftAt(y)) * column, y)
        return PetRoomWanderState(
            x = x,
            y = y,
            targetX = x,
            targetY = y,
            facingRight = random.nextBoolean(),
            isWalking = false,
            phaseRemainingMillis = randomPauseMillis()
        )
    }

    fun advance(state: PetRoomWanderState, elapsedMillis: Long): PetRoomWanderState {
        if (elapsedMillis <= 0L) return state
        return if (state.isWalking) walk(state, elapsedMillis) else pause(state, elapsedMillis)
    }

    private fun pause(state: PetRoomWanderState, elapsedMillis: Long): PetRoomWanderState {
        val remaining = state.phaseRemainingMillis - elapsedMillis
        if (remaining > 0L) return state.copy(phaseRemainingMillis = remaining)
        val targetY = floor.clampY(
            state.y + (random.nextFloat() * 2f - 1f) * (floor.bottom - floor.top) * STEP_DEPTH
        )
        val leftBound = floor.leftAt(targetY)
        val rightBound = floor.rightAt(targetY)
        val targetX = (leftBound + (rightBound - leftBound) * random.nextFloat())
        return state.copy(
            targetX = targetX,
            targetY = targetY,
            facingRight = if (abs(targetX - state.x) < FACING_EPSILON) {
                state.facingRight
            } else {
                targetX > state.x
            },
            isWalking = true,
            phaseRemainingMillis = 0L
        )
    }

    private fun walk(state: PetRoomWanderState, elapsedMillis: Long): PetRoomWanderState {
        val dx = state.targetX - state.x
        val dy = state.targetY - state.y
        val distance = hypot(dx, dy)
        val step = walkSpeedPerSecond * elapsedMillis / MILLIS_PER_SECOND
        if (distance <= step || distance <= ARRIVE_EPSILON) {
            return state.copy(
                x = state.targetX,
                y = state.targetY,
                isWalking = false,
                phaseRemainingMillis = randomPauseMillis()
            )
        }
        val nextY = floor.clampY(state.y + dy / distance * step)
        val nextX = floor.clampX(state.x + dx / distance * step, nextY)
        return state.copy(x = nextX, y = nextY)
    }

    private fun randomPauseMillis(): Long =
        random.nextLong(MIN_PAUSE_MILLIS, MAX_PAUSE_MILLIS)

    private companion object {
        const val MILLIS_PER_SECOND = 1_000f
        const val MIN_PAUSE_MILLIS = 1_200L
        const val MAX_PAUSE_MILLIS = 4_500L
        const val ARRIVE_EPSILON = 0.5f
        const val FACING_EPSILON = 1f

        /** How far up- or downstage one stroll may go, as a share of the floor depth. */
        const val STEP_DEPTH = 0.55f
    }
}
