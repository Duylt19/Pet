package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetVector
import kotlin.math.roundToInt

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
        margin: Int
    ): PetSpeechPlacement {
        require(viewportWidth > 0 && viewportHeight > 0) { "viewport must be positive" }
        require(bubbleWidth > 0 && bubbleHeight > 0) { "bubble size must be positive" }
        require(margin >= 0) { "margin must not be negative" }

        val maximumX = (viewportWidth - bubbleWidth - margin).coerceAtLeast(margin)
        val petCenterX = petPosition.x + petSize.width / 2f
        val bubbleX = (petCenterX - bubbleWidth / 2f)
            .roundToInt()
            .coerceIn(margin, maximumX)
        val aboveY = petPosition.y.roundToInt() - bubbleHeight
        val tailAtTop = aboveY < margin
        val requestedY = if (tailAtTop) {
            (petPosition.y + petSize.height * BELOW_PET_OFFSET).roundToInt()
        } else {
            aboveY
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
}
