package com.asianmobile.emojibattery.shimeji.ui.home.settings

data class SettingsUiState(
    val versionName: String = "",
    val petCount: Int = 1,
    val maxPets: Int = 1,
    val petSlots: List<SettingsPetSlotUiState> = emptyList()
) {
    val canAddPet: Boolean
        get() = petCount < maxPets
}

data class SettingsPetSlotUiState(
    val slotIndex: Int,
    val name: String,
    val previewImagePath: String?,
    val sizePercent: Int,
    val speedPercent: Int,
    val messagesEnabled: Boolean,
    val interactionEnabled: Boolean
)
