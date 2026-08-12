package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import android.graphics.Bitmap
import com.asianmobile.emojibattery.shimeji.ui.pet.room.PetRoomSettingsUiState

data class SettingsUiState(
    val versionName: String = "",
    val isBatteryEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isAppsHiddenSheetVisible: Boolean = false,
    val isInstalledAppsLoading: Boolean = false,
    val installedAppsLoadFailed: Boolean = false,
    val installedApps: List<InstalledAppUiState> = emptyList(),
    val hiddenAppPackages: Set<String> = emptySet(),
    val petSettings: PetRoomSettingsUiState? = null
)

data class InstalledAppUiState(
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
    val isHidden: Boolean
)

sealed interface SettingsEffect {
    data object RequestBatteryAccessibility : SettingsEffect
}
