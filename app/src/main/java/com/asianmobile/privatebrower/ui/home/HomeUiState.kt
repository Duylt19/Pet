package com.asianmobile.privatebrower.ui.home

data class HomeUiState(
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val isPetRunning: Boolean = false,
    val isStartingPet: Boolean = false,
    val message: HomeMessage? = null
) {
    val actionsEnabled: Boolean
        get() = !isStartingPet
}

enum class HomeMessage {
    PET_START_FAILED
}

sealed interface HomeEffect {
    data object OpenOverlaySettings : HomeEffect
    data object RequestNotificationPermission : HomeEffect
}

enum class HomePetCommand {
    OPEN_OVERLAY_SETTINGS,
    REQUEST_NOTIFICATION_PERMISSION,
    START,
    STOP
}

object HomePetPolicy {
    fun nextCommand(
        overlayGranted: Boolean,
        notificationPermissionRequired: Boolean,
        notificationGranted: Boolean,
        isPetRunning: Boolean
    ): HomePetCommand = when {
        isPetRunning -> HomePetCommand.STOP
        !overlayGranted -> HomePetCommand.OPEN_OVERLAY_SETTINGS
        notificationPermissionRequired && !notificationGranted ->
            HomePetCommand.REQUEST_NOTIFICATION_PERMISSION
        else -> HomePetCommand.START
    }
}
