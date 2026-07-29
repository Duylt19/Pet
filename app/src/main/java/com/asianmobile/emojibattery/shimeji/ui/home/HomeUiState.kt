package com.asianmobile.emojibattery.shimeji.ui.home

import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode

data class HomeUiState(
    val overlayGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val notificationPermissionRequired: Boolean = false,
    val isPetRunning: Boolean = false,
    val isStartingPet: Boolean = false,
    val displayMode: PetDisplayMode = PetDisplayMode.MIXED,
    val mixedPets: List<HomeMixedPetUiState> = emptyList(),
    val petCount: Int = 1,
    val maxMixedPets: Int = 12,
    val mixedUnlockedSlotCount: Int = 3,
    val swarmUnlocked: Boolean = false,
    val isPremium: Boolean = false,
    val swarmPackName: String? = null,
    val swarmPreviewPath: String? = null,
    val swarmCount: Int = 1,
    val maxSwarmPets: Int = 12,
    val message: HomeMessage? = null
) {
    val actionsEnabled: Boolean
        get() = !isStartingPet

    val hasRunnableSelection: Boolean
        get() = when (displayMode) {
            PetDisplayMode.MIXED -> mixedPets.any(HomeMixedPetUiState::isEnabled)
            PetDisplayMode.SWARM -> swarmUnlocked && swarmPackName != null
        }
}

data class HomeMixedPetUiState(
    val slotIndex: Int,
    val name: String,
    val previewPath: String?,
    val isEnabled: Boolean
)

enum class HomeMessage {
    PET_START_FAILED,
    KEEP_ONE_MIXED_PET_VISIBLE,
    SELECT_SWARM_PET,
    SWARM_REWARD_NOT_AVAILABLE
}

sealed interface HomeEffect {
    data object OpenOverlaySettings : HomeEffect
    data object RequestNotificationPermission : HomeEffect
    data object ShowSwarmRewardedAd : HomeEffect
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
