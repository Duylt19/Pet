package com.asianmobile.emojibattery.shimeji.ui.home.discover

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogUiState
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverPresentationStateTest {

    @Test
    fun `locked crowns follow the shared battery and pet ownership state`() {
        val state = DiscoverUiState(
            trendingPets = listOf(
                DiscoverPetUiState("installed", "Installed", "Cat", null),
                DiscoverPetUiState("locked", "Locked", "Cat", null)
            ),
            batteryThemes = listOf(
                DiscoverThemeUiState(1, "Premium", null, false),
                DiscoverThemeUiState(2, "Reward unlocked", null, false),
                DiscoverThemeUiState(3, "Free", null, false)
            ),
            emojiThemes = listOf(DiscoverAssetUiState(1, "Emoji", "emoji.webp")),
            batteryIcons = listOf(DiscoverAssetUiState(2, "Battery", "battery.webp"))
        )
        val batteryState = BatteryCatalogUiState(
            themes = listOf(
                theme(1, BatteryThemeEntitlement.PREMIUM),
                theme(2, BatteryThemeEntitlement.PREMIUM),
                theme(3, BatteryThemeEntitlement.FREE)
            ),
            rewardUnlockedThemeIds = setOf(2),
            isPremium = false
        )
        val result = discoverPresentationState(
            state = state,
            batteryState = batteryState,
            petState = PetStoreUiState(installedPackKeys = setOf("installed"))
        )

        assertFalse(result.trendingPets[0].isLocked)
        assertTrue(result.trendingPets[1].isLocked)
        assertTrue(result.batteryThemes[0].isLocked)
        assertFalse(result.batteryThemes[1].isLocked)
        assertFalse(result.batteryThemes[2].isLocked)
        assertTrue(result.emojiThemes.single().isLocked)
        assertFalse(result.batteryIcons.single().isLocked)
    }

    private fun theme(id: Int, entitlement: BatteryThemeEntitlement) = BatteryThemeEntry(
        id = id,
        name = "Theme $id",
        categoryId = 1,
        categoryName = "Trending",
        entitlement = entitlement,
        thumbnailPath = "thumb/$id.webp",
        batteryPath = "battery/$id.webp",
        emojiPath = "emoji/$id.webp",
        assetsReady = true
    )
}
