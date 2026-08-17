package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryCatalogDisplayPolicyTest {
    private val policy = BatteryCatalogDisplayPolicy()
    private val batteryTheme = theme(11, "Battery")
    private val emojiTheme = theme(12, "Emoji")

    @Test
    fun catalog_doesNotExposeBuiltInPlaceholder() {
        val visible = policy.filterThemes(
            listOf(BUILT_IN_BATTERY_THEME, batteryTheme, emojiTheme),
            categoryId = null,
            query = ""
        )

        assertEquals(listOf(batteryTheme, emojiTheme), visible)
        assertEquals(
            emptyList<BatteryCatalogCategory>(),
            policy.filterCategories(listOf(BUILT_IN_BATTERY_CATEGORY))
        )
    }

    @Test
    fun catalogFiltering_keepsCategoryAndSearchBehavior() {
        assertEquals(
            listOf(emojiTheme),
            policy.filterThemes(
                listOf(BUILT_IN_BATTERY_THEME, batteryTheme, emojiTheme),
                categoryId = 1,
                query = "emoji"
            )
        )
        assertEquals(
            emptyList<BatteryThemeEntry>(),
            policy.filterThemes(
                listOf(BUILT_IN_BATTERY_THEME, batteryTheme, emojiTheme),
                categoryId = 2,
                query = ""
            )
        )
    }

    @Test
    fun displayName_normalizesServerSeparatorsCasingAndCamelCase() {
        assertEquals(
            "Battery Icon Anime 01",
            batteryThemeDisplayName("BatteryIcon_Anime_01")
        )
        assertEquals(
            "Battery Cartoon 03",
            batteryThemeDisplayName("battery_cartoon_03")
        )
        assertEquals("WC 2026", batteryThemeDisplayName("WC_2026"))
        assertEquals("🔥 Trending", batteryCategoryDisplayName("  🔥 Trending  "))
    }

    @Test
    fun sections_groupNonBuiltInThemesByCategoryInCategoryOrder() {
        val trending = BatteryCatalogCategory(1, "🔥 Trending", "trending", 1)
        val empty = BatteryCatalogCategory(2, "Empty", "empty", 2)

        val sections = policy.sections(
            categories = listOf(BUILT_IN_BATTERY_CATEGORY, trending, empty),
            themes = listOf(BUILT_IN_BATTERY_THEME, batteryTheme, emojiTheme),
            trendingThemeIds = listOf(batteryTheme.id, emojiTheme.id)
        )

        assertEquals(listOf(trending), sections.map(BatteryCatalogSection::category))
        assertEquals(listOf(batteryTheme, emojiTheme), sections.single().themes)
    }

    @Test
    fun sections_useSharedCuratedOrderForTrendingAndCategoryOrderElsewhere() {
        val trending = BatteryCatalogCategory(1, "🔥 Trending", "trending", 1)
        val cute = BatteryCatalogCategory(2, "Cute", "cute", 2)
        val trendingTheme = theme(11, "Trending")
        val cuteThemeOne = theme(21, "Cute one", categoryId = cute.id)
        val cuteThemeTwo = theme(22, "Cute two", categoryId = cute.id)

        val sections = policy.sections(
            categories = listOf(trending, cute),
            themes = listOf(trendingTheme, cuteThemeOne, cuteThemeTwo),
            trendingThemeIds = listOf(cuteThemeTwo.id, 404, trendingTheme.id, cuteThemeOne.id)
        )

        assertEquals(
            listOf(cuteThemeTwo, trendingTheme, cuteThemeOne),
            sections.first { it.category == trending }.themes
        )
        assertEquals(
            listOf(cuteThemeOne, cuteThemeTwo),
            sections.first { it.category == cute }.themes
        )
    }

    @Test
    fun sections_hideTrendingWhenServerPublishesAnEmptyCuration() {
        val trending = BatteryCatalogCategory(1, "🔥 Trending", "trending", 1)
        val cute = BatteryCatalogCategory(2, "Cute", "cute", 2)
        val cuteTheme = theme(21, "Cute", categoryId = cute.id)

        val sections = policy.sections(
            categories = listOf(trending, cute),
            themes = listOf(batteryTheme, cuteTheme),
            trendingThemeIds = emptyList()
        )

        assertEquals(listOf(cute), sections.map(BatteryCatalogSection::category))
        assertEquals(listOf(cuteTheme), sections.single().themes)
    }

    private fun theme(
        id: Int,
        name: String,
        categoryId: Int = 1
    ) = BatteryThemeEntry(
        id = id,
        name = name,
        categoryId = categoryId,
        categoryName = "Trending",
        entitlement = BatteryThemeEntitlement.FREE,
        thumbnailPath = "thumb/$id.png",
        batteryPath = "battery/$id.png",
        emojiPath = "emoji/$id.png",
        assetsReady = true
    )
}
