package com.asianmobile.emojibattery.shimeji.ui.home.legacy

import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePetPolicyTest {
    private fun command(
        hasChosenPet: Boolean = true,
        overlayGranted: Boolean = true,
        notificationPermissionRequired: Boolean = true,
        notificationGranted: Boolean = true,
        notificationAlreadyAsked: Boolean = false,
        isPetRunning: Boolean = false
    ) = HomePetPolicy.nextCommand(
        hasChosenPet = hasChosenPet,
        overlayGranted = overlayGranted,
        notificationPermissionRequired = notificationPermissionRequired,
        notificationGranted = notificationGranted,
        notificationAlreadyAsked = notificationAlreadyAsked,
        isPetRunning = isPetRunning
    )

    @Test
    fun `running pet always stops without requesting permissions`() {
        assertEquals(
            HomePetCommand.STOP,
            command(
                hasChosenPet = false,
                overlayGranted = false,
                notificationGranted = false,
                isPetRunning = true
            )
        )
    }

    @Test
    fun `nothing is asked for until a pet has been chosen`() {
        // Otherwise the user spends two system screens granting access for a pet that then
        // never appears, and only afterwards learns they had to pick one.
        assertEquals(
            HomePetCommand.CHOOSE_PET,
            command(hasChosenPet = false, overlayGranted = false, notificationGranted = false)
        )
    }

    @Test
    fun `overlay access is required before notification permission`() {
        assertEquals(
            HomePetCommand.OPEN_OVERLAY_SETTINGS,
            command(overlayGranted = false, notificationGranted = false)
        )
    }

    @Test
    fun `notification permission is requested after overlay access`() {
        assertEquals(
            HomePetCommand.REQUEST_NOTIFICATION_PERMISSION,
            command(notificationGranted = false)
        )
    }

    @Test
    fun `the notification permission is only ever asked for once`() {
        // A permanently denied permission returns from its launcher without showing anything,
        // so asking again on the retry would spin forever. The overlay does not need it.
        assertEquals(
            HomePetCommand.START,
            command(notificationGranted = false, notificationAlreadyAsked = true)
        )
    }

    @Test
    fun `pet starts when required access is ready`() {
        assertEquals(HomePetCommand.START, command())
    }

    @Test
    fun `pet starts without notification runtime permission on older Android`() {
        assertEquals(
            HomePetCommand.START,
            command(notificationPermissionRequired = false, notificationGranted = false)
        )
    }

    @Test
    fun `mixed mode is runnable when at least one pet is visible`() {
        val state = HomeUiState(
            displayMode = PetDisplayMode.MIXED,
            mixedPets = listOf(
                HomeMixedPetUiState(0, "Cat", null, false),
                HomeMixedPetUiState(1, "Dog", null, true)
            )
        )

        assertEquals(true, state.hasRunnableSelection)
    }

    @Test
    fun `swarm requires both unlock and selected pet`() {
        val locked = HomeUiState(
            displayMode = PetDisplayMode.SWARM,
            swarmUnlocked = false,
            swarmPackName = "Cat"
        )
        val empty = locked.copy(swarmUnlocked = true, swarmPackName = null)
        val ready = empty.copy(swarmPackName = "Cat")

        assertEquals(false, locked.hasRunnableSelection)
        assertEquals(false, empty.hasRunnableSelection)
        assertEquals(true, ready.hasRunnableSelection)
    }
}
