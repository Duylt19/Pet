package com.asianmobile.emojibattery.shimeji.pet.speech

import com.asianmobile.emojibattery.shimeji.pet.engine.PetDirection
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector
import org.junit.Assert.assertEquals
import org.junit.Test

class PetSpeechPlacementPolicyTest {
    private val petSize = PetSize(120f, 120f)

    @Test
    fun `message box stays inside both horizontal viewport edges`() {
        val left = resolve(PetVector(-40f, 500f), PetDirection.LEFT)
        val right = resolve(PetVector(1_020f, 500f), PetDirection.RIGHT)

        assertEquals(6, left.x)
        assertEquals(854, right.x)
    }

    @Test
    fun `top and bottom message box positions are clamped safely`() {
        val top = resolve(PetVector(300f, -30f))
        val bottom = resolve(PetVector(300f, 1_900f))

        assertEquals(6, top.y)
        assertEquals(1_830, bottom.y)
    }

    @Test
    fun `left facing pet carries rectangular box in front of its talk frame`() {
        val placement = resolve(
            position = PetVector(400f, 800f),
            direction = PetDirection.LEFT
        )

        assertEquals(240, placement.x)
        assertEquals(776, placement.y)
    }

    @Test
    fun `mirrored talk frame moves rectangular box to the right`() {
        val placement = resolve(
            position = PetVector(400f, 800f),
            direction = PetDirection.RIGHT
        )

        assertEquals(460, placement.x)
        assertEquals(776, placement.y)
    }

    @Test
    fun `per pet attachment positions the box at the holding hand`() {
        val attachment = PetSpeechAttachment(x = 0.15f, y = 0.69f)

        val left = resolve(
            position = PetVector(400f, 800f),
            direction = PetDirection.LEFT,
            attachment = attachment,
            attachmentOverlap = 3
        )
        val right = resolve(
            position = PetVector(400f, 800f),
            direction = PetDirection.RIGHT,
            attachment = attachment,
            attachmentOverlap = 3
        )

        assertEquals(201, left.x)
        assertEquals(499, right.x)
        assertEquals(799, left.y)
        assertEquals(799, right.y)
    }

    @Test
    fun `attachment maps from a rectangular canvas into square overlay space`() {
        val attachment = PetSpeechAttachmentPolicy.resolve(
            canvasWidth = 64,
            canvasHeight = 128,
            imageAnchorX = 0.5f,
            imageAnchorY = 1f,
            speechAnchorX = 0.25f,
            speechAnchorY = 0.75f
        )

        assertEquals(0.375f, attachment.x)
        assertEquals(0.75f, attachment.y)
    }

    private fun resolve(
        position: PetVector,
        direction: PetDirection = PetDirection.LEFT,
        attachment: PetSpeechAttachment = PetSpeechAttachment.Default,
        attachmentOverlap: Int = 0
    ) = PetSpeechPlacementPolicy.resolve(
        petPosition = position,
        petSize = petSize,
        viewportWidth = 1_080,
        viewportHeight = 1_920,
        bubbleWidth = 220,
        bubbleHeight = 84,
        margin = 6,
        direction = direction,
        attachment = attachment,
        attachmentOverlap = attachmentOverlap
    )
}
