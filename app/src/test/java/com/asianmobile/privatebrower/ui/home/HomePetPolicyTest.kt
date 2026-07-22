package com.asianmobile.privatebrower.ui.home

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
}
