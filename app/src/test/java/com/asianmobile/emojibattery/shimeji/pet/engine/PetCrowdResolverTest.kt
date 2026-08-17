package com.asianmobile.emojibattery.shimeji.pet.engine

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
    fun `nearby resting pets are not magnetically pushed apart`() {
        val states = listOf(
            grounded(x = 100f, action = PetAction.SIT),
            grounded(x = 170f, action = PetAction.IDLE)
        )

        val resolved = resolver.resolve(states)

        assertEquals(states, resolved)
    }

    @Test
    fun `autonomous movers pass through each other without forced position or direction`() {
        val states = listOf(
            grounded(
                x = 120f,
                action = PetAction.TALK_WALK,
                direction = PetDirection.RIGHT
            ),
            grounded(x = 180f, action = PetAction.WALK, direction = PetDirection.LEFT)
        )
        val resolved = resolver.resolve(
            states
        )

        assertEquals(states, resolved)
    }

    @Test
    fun `social pair keeps director owned position and facing`() {
        val states = listOf(
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
        val resolved = resolver.resolve(states)

        assertEquals(states, resolved)
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
