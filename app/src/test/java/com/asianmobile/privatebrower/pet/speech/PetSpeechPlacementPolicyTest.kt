package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSpeechPlacementPolicyTest {
    private val petSize = PetSize(120f, 120f)

    @Test
    fun `bubble stays inside both horizontal viewport edges`() {
        val left = resolve(PetVector(-40f, 500f))
        val right = resolve(PetVector(1_020f, 500f))

        assertEquals(6, left.x)
        assertEquals(854, right.x)
        assertTrue(left.tailCenterX < right.tailCenterX)
    }

    @Test
    fun `top pet puts bubble below and reverses tail`() {
        val placement = resolve(PetVector(300f, -30f))

        assertTrue(placement.tailAtTop)
        assertEquals(52, placement.y)
    }

    @Test
    fun `ground pet puts bubble above and clamps bottom safely`() {
        val placement = resolve(PetVector(300f, 1_700f))

        assertFalse(placement.tailAtTop)
        assertEquals(1_616, placement.y)
    }

    private fun resolve(position: PetVector) = PetSpeechPlacementPolicy.resolve(
        petPosition = position,
        petSize = petSize,
        viewportWidth = 1_080,
        viewportHeight = 1_920,
        bubbleWidth = 220,
        bubbleHeight = 84,
        margin = 6
    )
}
