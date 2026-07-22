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
    fun `screen edge bounds allow one third pet overflow except at bottom`() {
        val screen = PetBounds(left = 0f, top = 0f, right = 300f, bottom = 600f)

        val expanded = screen.expandedForScreenEdges(PetSize(width = 90f, height = 120f))

        assertEquals(PetBounds(left = -30f, top = -30f, right = 330f, bottom = 600f), expanded)
        assertEquals(
            PetVector(x = 240f, y = 480f),
            expanded.clampTopLeft(PetVector(x = 1_000f, y = 1_000f), PetSize(90f, 120f))
        )
    }

    @Test
    fun `direction mirrors only when it differs from sprite native direction`() {
        assertEquals(false, PetDirection.LEFT.requiresMirror(PetDirection.LEFT))
        assertEquals(true, PetDirection.RIGHT.requiresMirror(PetDirection.LEFT))
        assertEquals(false, PetDirection.RIGHT.requiresMirror(PetDirection.RIGHT))
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
