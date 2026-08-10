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
    SWARM_REWARD_NOT_EARNED
}

sealed interface HomeEffect {
    data object OpenOverlaySettings : HomeEffect
    data object RequestNotificationPermission : HomeEffect
    data object ShowSwarmRewardedAd : HomeEffect
}

enum class HomePetCommand {
    /** Nothing to put on screen yet, so nothing is worth asking for. */
    CHOOSE_PET,
    OPEN_OVERLAY_SETTINGS,
    REQUEST_NOTIFICATION_PERMISSION,
    START,
    STOP
}

/**
 * What tapping the floating-pet switch should do next.
 *
 * The order matters twice over. Selection comes before every permission: asking a user to allow
 * drawing over other apps and then telling them they have no pet spends two system screens on
 * nothing. And the notification permission is asked **once** — it is not required for the overlay,
 * the service starts either way, and a permanently denied permission returns from its launcher
 * without showing a dialog, so re-asking on every retry is an unbreakable loop.
 */
object HomePetPolicy {
    fun nextCommand(
        hasChosenPet: Boolean,
        overlayGranted: Boolean,
        notificationPermissionRequired: Boolean,
        notificationGranted: Boolean,
        notificationAlreadyAsked: Boolean,
        isPetRunning: Boolean
    ): HomePetCommand = when {
        isPetRunning -> HomePetCommand.STOP
        !hasChosenPet -> HomePetCommand.CHOOSE_PET
        !overlayGranted -> HomePetCommand.OPEN_OVERLAY_SETTINGS
        notificationPermissionRequired && !notificationGranted && !notificationAlreadyAsked ->
            HomePetCommand.REQUEST_NOTIFICATION_PERMISSION
        else -> HomePetCommand.START
    }
}
