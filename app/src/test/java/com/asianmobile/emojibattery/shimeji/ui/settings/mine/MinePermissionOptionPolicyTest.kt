package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import org.junit.Assert.assertFalse
import org.junit.Test

class MinePermissionOptionPolicyTest {
    @Test
    fun `grant permission option is temporarily hidden`() {
        assertFalse(IS_MINE_GRANT_PERMISSION_VISIBLE)
    }
}
