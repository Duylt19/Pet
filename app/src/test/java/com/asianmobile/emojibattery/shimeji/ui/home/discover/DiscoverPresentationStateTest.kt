package com.asianmobile.emojibattery.shimeji.ui.home.discover

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_DISCOVER_TRENDING_PET_IDS
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogUiState
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverPresentationStateTest {

    @Test
    fun `trending battery themes follow curated ids and skip unavailable assets`() {
        val themeOne = theme(1, BatteryThemeEntitlement.FREE)
        val themeTwo = theme(2, BatteryThemeEntitlement.FREE)
        val incompleteThemeSix = theme(
            id = 6,
            entitlement = BatteryThemeEntitlement.FREE,
            assetsReady = false
        )

        val result = discoverTrendingBatteryThemes(
            themes = listOf(themeTwo, incompleteThemeSix, BUILT_IN_BATTERY_THEME, themeOne),
            trendingIds = listOf(1, 6, 2)
        )

        assertEquals(listOf(1, 2), result.map(BatteryThemeEntry::id))
    }

    @Test
    fun `trending pets follow curated ids rather than catalog order`() {
        val result = discoverTrendingPets(
            pets = listOf(ownerPet(42), ownerPet(30), ownerPet(9999), ownerPet(8)),
            trendingIds = listOf(8, 30, 404, 42)
        )

        assertEquals(listOf(8, 30, 42), result.map(OwnerPetCatalogEntry::id))
    }

    @Test
    fun `curated trending lists preserve the product ranking`() {
        assertEquals(34, DEFAULT_DISCOVER_TRENDING_PET_IDS.size)
        assertEquals(8, DEFAULT_DISCOVER_TRENDING_PET_IDS.first())
        assertEquals(2002, DEFAULT_DISCOVER_TRENDING_PET_IDS.last())
        assertEquals(34, DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS.size)
        assertEquals(1, DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS.first())
        assertEquals(919, DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS.last())
    }

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

    private fun theme(
        id: Int,
        entitlement: BatteryThemeEntitlement,
        assetsReady: Boolean = true
    ) = BatteryThemeEntry(
        id = id,
        name = "Theme $id",
        categoryId = 1,
        categoryName = "Trending",
        entitlement = entitlement,
        thumbnailPath = "thumb/$id.webp",
        batteryPath = "battery/$id.webp",
        emojiPath = "emoji/$id.webp",
        assetsReady = assetsReady
    )

    private fun ownerPet(id: Int) = OwnerPetCatalogEntry(
        id = id,
        name = "Pet $id",
        category = "Category",
        author = null,
        thumbnailPath = null,
        hasLocalArchive = false
    )
}
