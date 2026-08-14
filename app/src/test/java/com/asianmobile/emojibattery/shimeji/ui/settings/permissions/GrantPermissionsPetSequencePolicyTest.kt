package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            GrantPermissionsTarget.NOTIFICATION,
            GrantPermissionsUiState(
                isNotificationRowVisible = true
            ).nextPetPermissionTarget(emptySet())
        )
        assertEquals(
            GrantPermissionsTarget.OVERLAY,
            GrantPermissionsUiState(
                isNotificationGranted = true,
                isNotificationRowVisible = true
            )
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

    @Test
    fun `sequence is complete only after mandatory and relevant optional steps`() {
        val state = readyMandatoryState(
            isBatteryRowVisible = true,
            isAutoStartRowVisible = true
        )

        assertFalse(state.hasCompletedPetPermissionSequence(emptySet()))
        assertTrue(
            state.hasCompletedPetPermissionSequence(
                setOf(
                    GrantPermissionsTarget.BATTERY_OPTIMIZATION,
                    GrantPermissionsTarget.VENDOR_AUTO_START
                )
            )
        )
        assertFalse(
            GrantPermissionsUiState(isOverlayGranted = false)
                .hasCompletedPetPermissionSequence(emptySet())
        )
    }

    @Test
    fun `screen only exposes permissions that still need action`() {
        val complete = GrantPermissionsUiState(
            isAccessibilityEnabled = true,
            isOverlayGranted = true,
            isNotificationGranted = true,
            isNotificationRowVisible = true,
            isBatteryOptimizationIgnored = true,
            isBatteryRowVisible = true
        )

        assertFalse(complete.needsRequiredCard(GrantPermissionsTarget.ACCESSIBILITY))
        assertFalse(complete.needsRequiredCard(GrantPermissionsTarget.OVERLAY))
        assertFalse(complete.needsOverlayPermission)
        assertFalse(complete.needsNotificationPermission)
        assertFalse(complete.needsBatteryOptimizationExemption)
        assertFalse(
            complete.hasStabilityPermissionToRequest(GrantPermissionsTarget.ACCESSIBILITY)
        )
    }

    @Test
    fun `notification is not requested on Android versions where the permission does not exist`() {
        val state = GrantPermissionsUiState(
            isOverlayGranted = true,
            isNotificationGranted = true,
            isNotificationRowVisible = false
        )

        assertFalse(state.needsNotificationPermission)
        assertFalse(state.needsRequiredCard(GrantPermissionsTarget.OVERLAY))
    }

    @Test
    fun `pet hero represents overlay and does not duplicate its permission row`() {
        val onlyNotificationMissing = GrantPermissionsUiState(
            isOverlayGranted = true,
            isNotificationGranted = false,
            isNotificationRowVisible = true
        )

        assertFalse(onlyNotificationMissing.needsRequiredCard(GrantPermissionsTarget.OVERLAY))
        assertTrue(onlyNotificationMissing.needsNotificationPermission)
    }

    @Test
    fun `completed overlay flow keeps a success card instead of rendering an empty list`() {
        val complete = readyMandatoryState()

        assertTrue(
            complete.shouldShowOverlayCompletionCard(GrantPermissionsTarget.OVERLAY)
        )
        assertFalse(
            complete.shouldShowOverlayCompletionCard(GrantPermissionsTarget.ACCESSIBILITY)
        )
    }

    @Test
    fun `overlay completion waits while a relevant permission still needs action`() {
        assertFalse(
            GrantPermissionsUiState(
                isOverlayGranted = false,
                isNotificationGranted = true
            ).shouldShowOverlayCompletionCard(GrantPermissionsTarget.OVERLAY)
        )
        assertFalse(
            readyMandatoryState(isBatteryRowVisible = true)
                .shouldShowOverlayCompletionCard(GrantPermissionsTarget.OVERLAY)
        )
        assertFalse(
            GrantPermissionsUiState(
                isOverlayGranted = true,
                isNotificationGranted = false,
                isNotificationRowVisible = true
            ).shouldShowOverlayCompletionCard(GrantPermissionsTarget.OVERLAY)
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
