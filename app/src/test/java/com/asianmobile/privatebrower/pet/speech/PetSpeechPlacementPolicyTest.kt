package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetDirection
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

    @Test
    fun `talk window follows legacy shime35 attachment on the left`() {
        val placement = resolve(
            position = PetVector(400f, 800f),
            direction = PetDirection.LEFT,
            attachment = PetSpeechAttachment.TALK_WINDOW
        )

        assertFalse(placement.tailAtTop)
        assertEquals(240, placement.x)
        assertEquals(776, placement.y)
        assertEquals(220f, placement.tailCenterX)
    }

    @Test
    fun `mirrored talk window moves in front of a right facing pet`() {
        val placement = resolve(
            position = PetVector(400f, 800f),
            direction = PetDirection.RIGHT,
            attachment = PetSpeechAttachment.TALK_WINDOW
        )

        assertFalse(placement.tailAtTop)
        assertEquals(460, placement.x)
        assertEquals(776, placement.y)
        assertEquals(0f, placement.tailCenterX)
    }

    private fun resolve(
        position: PetVector,
        direction: PetDirection = PetDirection.LEFT,
        attachment: PetSpeechAttachment = PetSpeechAttachment.OVERHEAD
    ) = PetSpeechPlacementPolicy.resolve(
        petPosition = position,
        petSize = petSize,
        viewportWidth = 1_080,
        viewportHeight = 1_920,
        bubbleWidth = 220,
        bubbleHeight = 84,
        margin = 6,
        direction = direction,
        attachment = attachment
    )
}
