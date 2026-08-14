package com.asianmobile.emojibattery.shimeji.ui.battery.favoriterecent

import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouriteRecentThemePolicyTest {
    @Test
    fun `favorite screen keeps ready favorites in catalog order`() {
        val themes = listOf(
            theme(id = 1, assetsReady = true),
            theme(id = 2, assetsReady = false),
            theme(id = 3, assetsReady = true)
        )

        val result = favouriteThemeUiStates(themes, favoriteThemeIds = setOf(3, 2, 1))

        assertEquals(listOf(1, 3), result.map(FavouriteRecentThemeUiState::id))
        assertTrue(result.all(FavouriteRecentThemeUiState::isFavorite))
    }

    @Test
    fun `favorite screen excludes themes outside favorite set`() {
        val result = favouriteThemeUiStates(
            themes = listOf(theme(1), theme(2)),
            favoriteThemeIds = setOf(2)
        )

        assertEquals(listOf(2), result.map(FavouriteRecentThemeUiState::id))
    }

    @Test
    fun `recent screen keeps MRU order and current favorite state`() {
        val result = recentThemeUiStates(
            themes = listOf(theme(1), theme(2), theme(3)),
            recentThemeIds = listOf(3, 1, 2),
            favoriteThemeIds = setOf(1)
        )

        assertEquals(listOf(3, 1, 2), result.map(FavouriteRecentThemeUiState::id))
        assertEquals(listOf(false, true, false), result.map(FavouriteRecentThemeUiState::isFavorite))
    }

    @Test
    fun `recent screen ignores missing and unavailable themes`() {
        val result = recentThemeUiStates(
            themes = listOf(theme(1), theme(2, assetsReady = false)),
            recentThemeIds = listOf(9, 2, 1),
            favoriteThemeIds = emptySet()
        )

        assertEquals(listOf(1), result.map(FavouriteRecentThemeUiState::id))
    }

    private fun theme(id: Int, assetsReady: Boolean = true) = BatteryThemeEntry(
        id = id,
        name = "Theme $id",
        categoryId = 1,
        categoryName = "Cute",
        entitlement = BatteryThemeEntitlement.FREE,
        thumbnailPath = null,
        batteryPath = null,
        emojiPath = null,
        assetsReady = assetsReady
    )
}
