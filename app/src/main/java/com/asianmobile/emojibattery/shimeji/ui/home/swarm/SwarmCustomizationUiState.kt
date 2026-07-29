package com.asianmobile.emojibattery.shimeji.ui.home.swarm

import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmMovementInsets

data class SwarmCustomizationUiState(
    val name: String = "",
    val author: String = "",
    val previewImagePath: String? = null,
    val count: Int = 1,
    val maxCount: Int = 12,
    val sizePercent: Int = 80,
    val speedPercent: Int = 100,
    val randomizeSizeAndSpeed: Boolean = false,
    val constrainMovementArea: Boolean = false,
    val movementInsets: PetSwarmMovementInsets = PetSwarmMovementInsets()
) {
    val hasSelectedPet: Boolean
        get() = name.isNotBlank()
}
