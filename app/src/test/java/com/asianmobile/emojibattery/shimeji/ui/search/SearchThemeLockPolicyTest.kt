package com.asianmobile.emojibattery.shimeji.ui.search

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchThemeLockPolicyTest {

    @Test
    fun premiumTheme_isLockedForFreeUser() {
        assertTrue(isSearchThemeLocked(premiumTheme, false, emptySet()))
    }

    @Test
    fun premiumTheme_isUnlockedForPremiumUser() {
        assertFalse(isSearchThemeLocked(premiumTheme, true, emptySet()))
    }

    @Test
    fun premiumTheme_isUnlockedAfterReward() {
        assertFalse(isSearchThemeLocked(premiumTheme, false, setOf(premiumTheme.id)))
    }

    @Test
    fun freeTheme_neverShowsCrown() {
        assertFalse(
            isSearchThemeLocked(
                premiumTheme.copy(entitlement = BatteryThemeEntitlement.FREE),
                false,
                emptySet()
            )
        )
    }

    private companion object {
        val premiumTheme = BatteryThemeEntry(
            id = 7,
            name = "Premium theme",
            categoryId = 1,
            categoryName = "Cute",
            entitlement = BatteryThemeEntitlement.PREMIUM,
            thumbnailPath = null,
            batteryPath = null,
            emojiPath = null,
            assetsReady = true
        )
    }
}
