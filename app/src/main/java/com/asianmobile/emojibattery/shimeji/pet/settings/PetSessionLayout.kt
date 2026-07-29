package com.asianmobile.emojibattery.shimeji.pet.settings

import com.asianmobile.emojibattery.shimeji.data.model.PetPositionFraction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBounds
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector
import kotlin.random.Random

class PetSessionLayout {
    fun resolvePositions(
        count: Int,
        bounds: PetBounds,
        size: PetSize,
        saved: List<PetPositionFraction?>,
        marginPixels: Float
    ): List<PetVector> = List(count.coerceAtLeast(0)) { index ->
        resolvePosition(index, bounds, size, saved, marginPixels)
    }

    fun resolvePosition(
        index: Int,
        bounds: PetBounds,
        size: PetSize,
        saved: List<PetPositionFraction?>,
        marginPixels: Float
    ): PetVector =
        saved.getOrNull(index)?.let { restore(it, bounds, size) }
            ?: defaultPosition(index, bounds, size, marginPixels)

    fun randomPosition(
        bounds: PetBounds,
        size: PetSize,
        seed: Long,
        occupiedPositions: List<PetVector>
    ): PetVector {
        val maximumX = (bounds.right - size.width).coerceAtLeast(bounds.left)
        val maximumY = (bounds.bottom - size.height).coerceAtLeast(bounds.top)
        val random = Random(seed)
        val candidates = List(RANDOM_SPAWN_CANDIDATES) {
            PetVector(
                x = random.nextCoordinate(bounds.left, maximumX),
                y = random.nextCoordinate(bounds.top, maximumY)
            )
        }
        return candidates.maxBy { candidate ->
            occupiedPositions.minOfOrNull { occupied ->
                candidate.distanceSquaredTo(occupied)
            } ?: Float.MAX_VALUE
        }
    }

    fun normalize(position: PetVector, bounds: PetBounds, size: PetSize): PetPositionFraction {
        val availableWidth = (bounds.right - bounds.left - size.width).coerceAtLeast(1f)
        val availableHeight = (bounds.bottom - bounds.top - size.height).coerceAtLeast(1f)
        return PetPositionFraction(
            x = ((position.x - bounds.left) / availableWidth).coerceIn(0f, 1f),
            y = ((position.y - bounds.top) / availableHeight).coerceIn(0f, 1f)
        )
    }

    private fun restore(
        fraction: PetPositionFraction,
        bounds: PetBounds,
        size: PetSize
    ): PetVector = bounds.clampTopLeft(
        PetVector(
            x = bounds.left + fraction.x.coerceIn(0f, 1f) *
                (bounds.right - bounds.left - size.width).coerceAtLeast(0f),
            y = bounds.top + fraction.y.coerceIn(0f, 1f) *
                (bounds.bottom - bounds.top - size.height).coerceAtLeast(0f)
        ),
        size
    )

    private fun defaultPosition(
        index: Int,
        bounds: PetBounds,
        size: PetSize,
        marginPixels: Float
    ): PetVector {
        val horizontalOffset = index * (size.width * HORIZONTAL_SPACING_FACTOR)
        val verticalOffset = index * (size.height * VERTICAL_SPACING_FACTOR)
        return bounds.clampTopLeft(
            PetVector(
                x = bounds.right - size.width - marginPixels - horizontalOffset,
                y = bounds.bottom - size.height - marginPixels - verticalOffset
            ),
            size
        )
    }

    private companion object {
        const val HORIZONTAL_SPACING_FACTOR = 1.05f
        const val VERTICAL_SPACING_FACTOR = 0.45f
        const val RANDOM_SPAWN_CANDIDATES = 12
    }
}

private fun Random.nextCoordinate(minimum: Float, maximum: Float): Float =
    if (maximum <= minimum) minimum else minimum + nextFloat() * (maximum - minimum)

private fun PetVector.distanceSquaredTo(other: PetVector): Float {
    val deltaX = x - other.x
    val deltaY = y - other.y
    return deltaX * deltaX + deltaY * deltaY
}
