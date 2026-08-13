package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrantPermissionsPetSequencePolicyTest {
    @Test
    fun `mandatory permissions are overlay and notification`() {
        assertTrue(
            GrantPermissionsUiState(
                isOverlayGranted = true,
                isNotificationGranted = true
            ).hasMandatoryPetPermissions
        )
    }

    @Test
    fun `sequence follows the visible rows from top to bottom`() {
        assertEquals(
            GrantPermissionsTarget.OVERLAY,
            GrantPermissionsUiState().nextPetPermissionTarget(emptySet())
        )
        assertEquals(
            GrantPermissionsTarget.NOTIFICATION,
            GrantPermissionsUiState(isOverlayGranted = true)
                .nextPetPermissionTarget(emptySet())
        )
        assertEquals(
            GrantPermissionsTarget.BATTERY_OPTIMIZATION,
            readyMandatoryState(isBatteryRowVisible = true)
                .nextPetPermissionTarget(emptySet())
        )
        assertEquals(
            GrantPermissionsTarget.VENDOR_AUTO_START,
            readyMandatoryState(isAutoStartRowVisible = true)
                .nextPetPermissionTarget(emptySet())
        )
    }

    @Test
    fun `declined optional rows are attempted only once`() {
        val state = readyMandatoryState(
            isBatteryRowVisible = true,
            isAutoStartRowVisible = true
        )

        assertEquals(
            GrantPermissionsTarget.VENDOR_AUTO_START,
            state.nextPetPermissionTarget(setOf(GrantPermissionsTarget.BATTERY_OPTIMIZATION))
        )
        assertNull(
            state.nextPetPermissionTarget(
                setOf(
                    GrantPermissionsTarget.BATTERY_OPTIMIZATION,
                    GrantPermissionsTarget.VENDOR_AUTO_START
                )
            )
        )
    }

    private fun readyMandatoryState(
        isBatteryRowVisible: Boolean = false,
        isAutoStartRowVisible: Boolean = false
    ) = GrantPermissionsUiState(
        isOverlayGranted = true,
        isNotificationGranted = true,
        isBatteryRowVisible = isBatteryRowVisible,
        isAutoStartRowVisible = isAutoStartRowVisible
    )
}
