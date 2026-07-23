package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetDirection
import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetVector
import kotlin.math.roundToInt

enum class PetSpeechAttachment {
    OVERHEAD,
    TALK_WINDOW
}

data class PetSpeechPlacement(
    val x: Int,
    val y: Int,
    val tailAtTop: Boolean,
    val tailCenterX: Float
)

object PetSpeechPlacementPolicy {
    fun resolve(
        petPosition: PetVector,
        petSize: PetSize,
        viewportWidth: Int,
        viewportHeight: Int,
        bubbleWidth: Int,
        bubbleHeight: Int,
        margin: Int,
        direction: PetDirection = PetDirection.LEFT,
        attachment: PetSpeechAttachment = PetSpeechAttachment.OVERHEAD
    ): PetSpeechPlacement {
        require(viewportWidth > 0 && viewportHeight > 0) { "viewport must be positive" }
        require(bubbleWidth > 0 && bubbleHeight > 0) { "bubble size must be positive" }
        require(margin >= 0) { "margin must not be negative" }

        val petCenterX = petPosition.x + petSize.width / 2f
        val requestedX = when (attachment) {
            PetSpeechAttachment.OVERHEAD -> petCenterX - bubbleWidth / 2f
            PetSpeechAttachment.TALK_WINDOW -> when (direction) {
                PetDirection.LEFT -> petCenterX - bubbleWidth
                PetDirection.RIGHT -> petCenterX
            }
        }
        val maximumX = (viewportWidth - bubbleWidth - margin).coerceAtLeast(margin)
        val bubbleX = requestedX
            .roundToInt()
            .coerceIn(margin, maximumX)
        val (requestedY, tailAtTop) = when (attachment) {
            PetSpeechAttachment.OVERHEAD -> {
                val aboveY = petPosition.y.roundToInt() - bubbleHeight
                if (aboveY < margin) {
                    (petPosition.y + petSize.height * BELOW_PET_OFFSET).roundToInt() to true
                } else {
                    aboveY to false
                }
            }

            PetSpeechAttachment.TALK_WINDOW -> {
                val talkWindowBottom = petPosition.y +
                    petSize.height * TALK_WINDOW_BOTTOM_FROM_PET_TOP
                (talkWindowBottom - bubbleHeight).roundToInt() to false
            }
        }
        val maximumY = (viewportHeight - bubbleHeight - margin).coerceAtLeast(margin)
        return PetSpeechPlacement(
            x = bubbleX,
            y = requestedY.coerceIn(margin, maximumY),
            tailAtTop = tailAtTop,
            tailCenterX = petCenterX - bubbleX
        )
    }

    private const val BELOW_PET_OFFSET = 0.68f
    // Shimeji-EE WalkWithIE: ImageAnchorY=128 and IeOffsetY=-64.
    private const val TALK_WINDOW_BOTTOM_FROM_PET_TOP = 0.5f
}
