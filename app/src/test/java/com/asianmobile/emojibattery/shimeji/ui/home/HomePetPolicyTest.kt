package com.asianmobile.emojibattery.shimeji.ui.home

import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePetPolicyTest {
    @Test
    fun `running pet always stops without requesting permissions`() {
        val command = HomePetPolicy.nextCommand(
            overlayGranted = false,
            notificationPermissionRequired = true,
            notificationGranted = false,
            isPetRunning = true
        )

        assertEquals(HomePetCommand.STOP, command)
    }

    @Test
    fun `overlay access is required before notification permission`() {
        val command = HomePetPolicy.nextCommand(
            overlayGranted = false,
            notificationPermissionRequired = true,
            notificationGranted = false,
            isPetRunning = false
        )

        assertEquals(HomePetCommand.OPEN_OVERLAY_SETTINGS, command)
    }

    @Test
    fun `notification permission is requested after overlay access`() {
        val command = HomePetPolicy.nextCommand(
            overlayGranted = true,
            notificationPermissionRequired = true,
            notificationGranted = false,
            isPetRunning = false
        )

        assertEquals(HomePetCommand.REQUEST_NOTIFICATION_PERMISSION, command)
    }

    @Test
    fun `pet starts when required access is ready`() {
        val command = HomePetPolicy.nextCommand(
            overlayGranted = true,
            notificationPermissionRequired = true,
            notificationGranted = true,
            isPetRunning = false
        )

        assertEquals(HomePetCommand.START, command)
    }

    @Test
    fun `pet starts without notification runtime permission on older Android`() {
        val command = HomePetPolicy.nextCommand(
            overlayGranted = true,
            notificationPermissionRequired = false,
            notificationGranted = false,
            isPetRunning = false
        )

        assertEquals(HomePetCommand.START, command)
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
