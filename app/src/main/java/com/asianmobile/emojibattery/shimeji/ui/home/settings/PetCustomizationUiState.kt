package com.asianmobile.emojibattery.shimeji.ui.home.settings

data class PetCustomizationUiState(
    val slotIndex: Int = 0,
    val petCount: Int = 1,
    val name: String = "",
    val author: String = "",
    val previewImagePath: String? = null,
    val sizePercent: Int = 100,
    val speedPercent: Int = 100,
    val messagesEnabled: Boolean = true,
    val customMessages: List<String> = emptyList(),
    val interactionEnabled: Boolean = true
) {
    val isActive: Boolean
        get() = slotIndex in 0 until petCount

    val canRemove: Boolean
        get() = isActive && petCount > 1
}
