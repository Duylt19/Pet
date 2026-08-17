package com.asianmobile.emojibattery.shimeji.pet.engine

import kotlin.math.hypot

data class PetVector(
    val x: Float = 0f,
    val y: Float = 0f
) {
    val magnitude: Float
        get() = hypot(x, y)

    operator fun plus(other: PetVector): PetVector = PetVector(x + other.x, y + other.y)

    operator fun times(scale: Float): PetVector = PetVector(x * scale, y * scale)

    fun limitedTo(maxMagnitude: Float): PetVector {
        require(maxMagnitude >= 0f) { "maxMagnitude must not be negative" }
        val currentMagnitude = magnitude
        if (currentMagnitude == 0f || currentMagnitude <= maxMagnitude) return this
        return this * (maxMagnitude / currentMagnitude)
    }

    companion object {
        val Zero = PetVector()
    }
}

data class PetSize(
    val width: Float,
    val height: Float
) {
    init {
        require(width >= 0f) { "width must not be negative" }
        require(height >= 0f) { "height must not be negative" }
    }
}

data class PetBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(right >= left) { "right must be greater than or equal to left" }
        require(bottom >= top) { "bottom must be greater than or equal to top" }
    }

    fun clampTopLeft(position: PetVector, petSize: PetSize): PetVector {
        val maximumX = (right - petSize.width).coerceAtLeast(left)
        val maximumY = (bottom - petSize.height).coerceAtLeast(top)
        return PetVector(
            x = position.x.coerceIn(left, maximumX),
            y = position.y.coerceIn(top, maximumY)
        )
    }

    fun expandedForScreenEdges(petSize: PetSize): PetBounds {
        val overflow = petSize.width / EDGE_OVERFLOW_DIVISOR
        return PetBounds(
            left = left - overflow,
            top = top - overflow,
            right = right + overflow,
            bottom = bottom
        )
    }

    private companion object {
        const val EDGE_OVERFLOW_DIVISOR = 3f
    }
}
