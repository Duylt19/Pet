package com.asianmobile.emojibattery.shimeji.ui.shared.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionDisclosureBottomSheetTest {
    @Test
    fun `short swipe returns the expanded sheet instead of dismissing it`() {
        assertFalse(
            shouldAllowPermissionSheetDismiss(
                isExpanded = true,
                currentOffsetPx = 180f,
                expandedOffsetPx = 100f,
                sheetHeightPx = 800f
            )
        )
    }

    @Test
    fun `deep swipe dismisses the sheet`() {
        assertTrue(
            shouldAllowPermissionSheetDismiss(
                isExpanded = true,
                currentOffsetPx = 300f,
                expandedOffsetPx = 100f,
                sheetHeightPx = 800f
            )
        )
    }

    @Test
    fun `explicit dismiss at expanded position remains allowed`() {
        assertTrue(
            shouldAllowPermissionSheetDismiss(
                isExpanded = true,
                currentOffsetPx = 100f,
                expandedOffsetPx = 100f,
                sheetHeightPx = 800f
            )
        )
    }
}
