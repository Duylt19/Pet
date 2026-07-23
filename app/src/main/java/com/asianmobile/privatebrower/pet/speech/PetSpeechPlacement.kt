package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetDirection
import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetVector
import kotlin.math.roundToInt

data class PetSpeechPlacement(
    val x: Int,
    val y: Int
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
        direction: PetDirection = PetDirection.LEFT
    ): PetSpeechPlacement {
        require(viewportWidth > 0 && viewportHeight > 0) { "viewport must be positive" }
        require(bubbleWidth > 0 && bubbleHeight > 0) { "bubble size must be positive" }
        require(margin >= 0) { "margin must not be negative" }

        val petCenterX = petPosition.x + petSize.width / 2f
        val requestedX = when (direction) {
            PetDirection.LEFT -> petCenterX - bubbleWidth
            PetDirection.RIGHT -> petCenterX
        }
        val maximumX = (viewportWidth - bubbleWidth - margin).coerceAtLeast(margin)
        val bubbleX = requestedX
            .roundToInt()
            .coerceIn(margin, maximumX)
        val talkWindowBottom = petPosition.y +
            petSize.height * TALK_WINDOW_BOTTOM_FROM_PET_TOP
        val requestedY = (talkWindowBottom - bubbleHeight).roundToInt()
        val maximumY = (viewportHeight - bubbleHeight - margin).coerceAtLeast(margin)
        return PetSpeechPlacement(
            x = bubbleX,
            y = requestedY.coerceIn(margin, maximumY)
        )
    }

    // Shimeji-EE WalkWithIE: ImageAnchorY=128 and IeOffsetY=-64.
    private const val TALK_WINDOW_BOTTOM_FROM_PET_TOP = 0.5f
}
