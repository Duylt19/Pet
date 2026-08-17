package com.asianmobile.emojibattery.shimeji.pet.speech

import com.asianmobile.emojibattery.shimeji.pet.engine.PetDirection
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector
import kotlin.math.roundToInt

data class PetSpeechPlacement(
    val x: Int,
    val y: Int
)

data class PetSpeechAttachment(
    val x: Float,
    val y: Float
) {
    init {
        require(x in 0f..1f && y in 0f..1f) { "speech attachment must be normalized" }
    }

    companion object {
        val Default = PetSpeechAttachment(0.5f, 0.5f)
    }
}

object PetSpeechAttachmentPolicy {
    fun resolve(
        canvasWidth: Int,
        canvasHeight: Int,
        imageAnchorX: Float,
        imageAnchorY: Float,
        speechAnchorX: Float,
        speechAnchorY: Float
    ): PetSpeechAttachment {
        require(canvasWidth > 0 && canvasHeight > 0) { "canvas must be positive" }
        val largestSide = maxOf(canvasWidth, canvasHeight).toFloat()
        val drawWidthFraction = canvasWidth / largestSide
        val drawHeightFraction = canvasHeight / largestSide
        val destinationLeft = 0.5f - imageAnchorX * drawWidthFraction
        val destinationTop = 1f - imageAnchorY * drawHeightFraction
        return PetSpeechAttachment(
            x = (destinationLeft + speechAnchorX * drawWidthFraction).coerceIn(0f, 1f),
            y = (destinationTop + speechAnchorY * drawHeightFraction).coerceIn(0f, 1f)
        )
    }
}

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
        attachment: PetSpeechAttachment = PetSpeechAttachment.Default,
        attachmentOverlap: Int = 0
    ): PetSpeechPlacement {
        require(viewportWidth > 0 && viewportHeight > 0) { "viewport must be positive" }
        require(bubbleWidth > 0 && bubbleHeight > 0) { "bubble size must be positive" }
        require(margin >= 0) { "margin must not be negative" }
        require(attachmentOverlap >= 0) { "attachment overlap must not be negative" }

        val anchorX = petPosition.x + petSize.width * when (direction) {
            PetDirection.LEFT -> attachment.x
            PetDirection.RIGHT -> 1f - attachment.x
        }
        val requestedX = when (direction) {
            PetDirection.LEFT -> anchorX - bubbleWidth + attachmentOverlap
            PetDirection.RIGHT -> anchorX - attachmentOverlap
        }
        val maximumX = (viewportWidth - bubbleWidth - margin).coerceAtLeast(margin)
        val bubbleX = requestedX
            .roundToInt()
            .coerceIn(margin, maximumX)
        val talkWindowBottom = petPosition.y + petSize.height * attachment.y
        val requestedY = (talkWindowBottom - bubbleHeight).roundToInt()
        val maximumY = (viewportHeight - bubbleHeight - margin).coerceAtLeast(margin)
        return PetSpeechPlacement(
            x = bubbleX,
            y = requestedY.coerceIn(margin, maximumY)
        )
    }
}
