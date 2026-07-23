package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetCrowdResolverTest {
    private val bounds = PetBounds(0f, 0f, 500f, 100f)
    private val size = PetSize(100f, 100f)
    private val engine = PetEngine()
    private val resolver = PetCrowdResolver()

    @Test
    fun `overlapping grounded pets are separated inside screen bounds`() {
        val states = listOf(
            grounded(x = 350f),
            grounded(x = 360f),
            grounded(x = 370f)
        )

        val resolved = resolver.resolve(states).sortedBy { it.position.x }

        resolved.zipWithNext().forEach { (left, right) ->
            assertTrue(
                right.position.x >= left.position.x + left.size.width * 1.05f - 0.01f
            )
        }
        resolved.forEach { state ->
            assertEquals(state.position, state.bounds.clampTopLeft(state.position, state.size))
        }
    }

    @Test
    fun `autonomous movers turn outward after crowd collision`() {
        val resolved = resolver.resolve(
            listOf(
                grounded(x = 120f, action = PetAction.RUN, direction = PetDirection.RIGHT),
                grounded(x = 180f, action = PetAction.WALK, direction = PetDirection.LEFT)
            )
        )

        assertEquals(PetDirection.LEFT, resolved[0].direction)
        assertEquals(PetDirection.RIGHT, resolved[1].direction)
    }

    @Test
    fun `social movers keep director facing while personal space is restored`() {
        val resolved = resolver.resolve(
            listOf(
                grounded(
                    x = 120f,
                    action = PetAction.RUN,
                    direction = PetDirection.RIGHT,
                    comboId = PetComboId.SOCIAL_APPROACH
                ),
                grounded(
                    x = 180f,
                    action = PetAction.RUN,
                    direction = PetDirection.LEFT,
                    comboId = PetComboId.SOCIAL_APPROACH
                )
            )
        )

        assertEquals(PetDirection.RIGHT, resolved[0].direction)
        assertEquals(PetDirection.LEFT, resolved[1].direction)
        assertTrue(resolved[1].position.x >= resolved[0].position.x + size.width * 1.05f - 0.01f)
    }

    @Test
    fun `airborne pets can cross without ground crowd correction`() {
        val first = grounded(x = 100f, action = PetAction.FLUNG)
        val second = grounded(x = 120f, action = PetAction.FALL)

        val resolved = resolver.resolve(listOf(first, second))

        assertEquals(listOf(first, second), resolved)
    }

    private fun grounded(
        x: Float,
        action: PetAction = PetAction.IDLE,
        direction: PetDirection = PetDirection.RIGHT,
        comboId: PetComboId? = null
    ): PetState = engine.initialState(
        bounds = bounds,
        size = size,
        position = PetVector(x, 0f),
        action = action,
        direction = direction
    ).copy(activeComboId = comboId)
}
