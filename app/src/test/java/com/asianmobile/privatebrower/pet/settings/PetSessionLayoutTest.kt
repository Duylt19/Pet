package com.asianmobile.privatebrower.pet.settings

import com.asianmobile.privatebrower.data.model.PetPositionFraction
import com.asianmobile.privatebrower.pet.engine.PetBounds
import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSessionLayoutTest {
    private val layout = PetSessionLayout()
    private val bounds = PetBounds(left = 0f, top = 20f, right = 500f, bottom = 900f)
    private val size = PetSize(width = 100f, height = 100f)

    @Test
    fun `saved normalized positions restore across usable bounds`() {
        val positions = layout.resolvePositions(
            count = 2,
            bounds = bounds,
            size = size,
            saved = listOf(
                PetPositionFraction(0f, 0f),
                PetPositionFraction(1f, 1f)
            ),
            marginPixels = 20f
        )

        assertEquals(PetVector(0f, 20f), positions[0])
        assertEquals(PetVector(400f, 800f), positions[1])
    }

    @Test
    fun `missing positions receive distinct clamped defaults`() {
        val positions = layout.resolvePositions(
            count = 3,
            bounds = bounds,
            size = size,
            saved = emptyList(),
            marginPixels = 20f
        )

        assertEquals(3, positions.distinct().size)
        positions.sortedBy(PetVector::x).zipWithNext().forEach { (left, right) ->
            assertTrue(right.x >= left.x + size.width)
        }
        positions.forEach { position ->
            assertEquals(position, bounds.clampTopLeft(position, size))
        }
    }

    @Test
    fun `normalize and restore round trip position`() {
        val original = PetVector(120f, 410f)
        val normalized = layout.normalize(original, bounds, size)
        val restored = layout.resolvePositions(
            count = 1,
            bounds = bounds,
            size = size,
            saved = listOf(normalized),
            marginPixels = 0f
        ).single()

        assertEquals(original.x, restored.x, 0.001f)
        assertEquals(original.y, restored.y, 0.001f)
    }
}
