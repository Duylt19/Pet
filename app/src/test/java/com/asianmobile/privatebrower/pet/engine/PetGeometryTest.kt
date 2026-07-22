package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PetGeometryTest {
    @Test
    fun `top-left position is constrained by pet size`() {
        val bounds = PetBounds(left = 10f, top = 20f, right = 110f, bottom = 220f)
        val size = PetSize(width = 30f, height = 50f)

        assertEquals(PetVector(10f, 20f), bounds.clampTopLeft(PetVector(-50f, -10f), size))
        assertEquals(PetVector(80f, 170f), bounds.clampTopLeft(PetVector(500f, 500f), size))
    }

    @Test
    fun `oversized pet anchors to bounds origin`() {
        val bounds = PetBounds(left = 5f, top = 7f, right = 25f, bottom = 27f)

        val position = bounds.clampTopLeft(
            position = PetVector(100f, 100f),
            petSize = PetSize(width = 40f, height = 50f)
        )

        assertEquals(PetVector(5f, 7f), position)
    }

    @Test
    fun `velocity is limited without changing direction`() {
        val limited = PetVector(x = 300f, y = 400f).limitedTo(maxMagnitude = 100f)

        assertEquals(60f, limited.x, FLOAT_TOLERANCE)
        assertEquals(80f, limited.y, FLOAT_TOLERANCE)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
