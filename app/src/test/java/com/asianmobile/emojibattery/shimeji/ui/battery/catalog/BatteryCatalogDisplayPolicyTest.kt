package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_THEME
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class BatteryCatalogDisplayPolicyTest {
    private val policy = BatteryCatalogDisplayPolicy()
    private val batteryTheme = theme(11, "Battery")
    private val emojiTheme = theme(12, "Emoji")
    private val emotion = BatteryDecorationEntry(
        id = 7,
        name = "emotion_7",
        assetPath = "emotion/7.png",
        type = BatteryDecorationType.EMOTION
    )
    private val animation = BatteryAnimationEntry(
        id = 8,
        name = "cute_8.json",
        assetPath = "animation/cute_8.json",
        type = BatteryAnimationType.LOTTIE
    )
    private val catalog = BatteryCatalogSnapshot(
        themes = listOf(BUILT_IN_BATTERY_THEME, batteryTheme, emojiTheme),
        emotions = listOf(emotion),
        animations = listOf(animation),
        isLoading = false
    )

    @Test
    fun firstUse_hasNoCurrentCardAndDoesNotExposeBuiltInPlaceholder() {
        assertNull(
            policy.currentStyle(
                catalog,
                BatteryStatusConfig(enabled = false, hasApplied = false)
            )
        )

        val visible = policy.filterThemes(catalog.themes, categoryId = null, query = "")

        assertEquals(listOf(batteryTheme, emojiTheme), visible)
        assertEquals(
            emptyList<BatteryCatalogCategory>(),
            policy.filterCategories(listOf(BUILT_IN_BATTERY_CATEGORY))
        )
    }

    @Test
    fun appliedConfig_resolvesCurrentMixedBatteryAndEmoji() {
        val current = policy.currentStyle(
            catalog,
            BatteryStatusConfig(
                enabled = false,
                hasApplied = true,
                selectedBatteryThemeId = batteryTheme.id,
                selectedEmojiThemeId = emojiTheme.id,
                emotionDecorationId = emotion.id,
                animationAssetName = animation.name
            )
        )

        assertSame(batteryTheme, current?.batteryTheme)
        assertSame(emojiTheme, current?.emojiTheme)
        assertEquals(emotion.assetPath, current?.emotionPath)
        assertEquals(animation, current?.animation)
        assertEquals(
            true,
            BatteryCatalogUiState(currentStyle = current).showCurrentStyle
        )
        assertEquals(
            false,
            BatteryCatalogUiState(
                currentStyle = current,
                selectedCategoryId = 1
            ).showCurrentStyle
        )
        assertEquals(
            false,
            BatteryCatalogUiState(
                currentStyle = current,
                searchQuery = "battery"
            ).showCurrentStyle
        )
    }

    @Test
    fun disabledDecorations_areNotIncludedInCurrentPreview() {
        val current = policy.currentStyle(
            catalog,
            BatteryStatusConfig(
                hasApplied = true,
                emotionDecorationId = emotion.id,
                animationAssetName = animation.name,
                showEmotion = false,
                showAnimation = false
            )
        )

        assertNull(current?.emotionPath)
        assertNull(current?.animation)
    }

    @Test
    fun catalogFiltering_keepsCategoryAndSearchBehavior() {
        assertEquals(
            listOf(emojiTheme),
            policy.filterThemes(catalog.themes, categoryId = 1, query = "emoji")
        )
        assertEquals(
            emptyList<BatteryThemeEntry>(),
            policy.filterThemes(catalog.themes, categoryId = 2, query = "")
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
    }

    private fun theme(id: Int, name: String) = BatteryThemeEntry(
        id = id,
        name = name,
        categoryId = 1,
        categoryName = "Trending",
        entitlement = BatteryThemeEntitlement.FREE,
        thumbnailPath = "thumb/$id.png",
        batteryPath = "battery/$id.png",
        emojiPath = "emoji/$id.png",
        assetsReady = true
    )
}
