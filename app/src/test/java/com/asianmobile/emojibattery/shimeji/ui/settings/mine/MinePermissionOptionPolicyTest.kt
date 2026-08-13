package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import org.junit.Assert.assertTrue
import org.junit.Test

class MinePermissionOptionPolicyTest {
    @Test
    fun `grant permission dashboard is visible from Mine`() {
        assertTrue(IS_MINE_GRANT_PERMISSION_VISIBLE)
    }
}
