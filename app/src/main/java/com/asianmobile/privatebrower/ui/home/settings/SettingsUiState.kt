package com.asianmobile.privatebrower.ui.home.settings

data class SettingsUiState(
    val versionName: String = "",
    val petCount: Int = 1,
    val maxPets: Int = 1,
    val sizePercent: Int = 100,
    val speedPercent: Int = 100,
    val soundEnabled: Boolean = false,
    val messagesEnabled: Boolean = true,
    val customMessages: List<String> = emptyList(),
    val interactionEnabled: Boolean = true,
    val petSlots: List<SettingsPetSlotUiState> = emptyList()
)

data class SettingsPetSlotUiState(
    val slotIndex: Int,
    val name: String
)
