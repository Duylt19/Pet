package com.asianmobile.emojibattery.shimeji.pet.room

import kotlin.math.roundToInt

/**
 * The room draws a pet at the same size the overlay would, so the same setting means the same
 * thing in both places. Kept beside the room rather than inside the overlay controller because
 * both now need it, and it is pure arithmetic worth testing on its own.
 */
object PetRoomSizePolicy {
    const val PET_SIZE_DP = 84
    const val MIN_PET_SIZE_DP = 48
    const val MAX_PET_SIZE_DP = 144

    /** Matches `PetOverlayController.petSizePixels`. */
    fun petSizeDp(packDefaultScale: Float, sizePercent: Int): Int =
        (PET_SIZE_DP * packDefaultScale * sizePercent / 100f)
            .roundToInt()
            .coerceIn(MIN_PET_SIZE_DP, MAX_PET_SIZE_DP)

    fun petSizePixels(packDefaultScale: Float, sizePercent: Int, density: Float): Float =
        petSizeDp(packDefaultScale, sizePercent) * density

    /** Speed is a multiplier in the overlay too, where it scales the clip timeline. */
    fun speedMultiplier(speedPercent: Int): Float =
        (speedPercent.coerceIn(MIN_SPEED_PERCENT, MAX_SPEED_PERCENT)) / 100f

    private const val MIN_SPEED_PERCENT = 50
    private const val MAX_SPEED_PERCENT = 150
}
