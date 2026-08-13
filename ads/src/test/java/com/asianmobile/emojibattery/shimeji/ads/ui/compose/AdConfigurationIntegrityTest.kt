package com.asianmobile.emojibattery.shimeji.ads.ui.compose

import com.asianmobile.emojibattery.shimeji.ads.config.DISTANCE_TIME_SHOW_OTHER_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.DISTANCE_TIME_SHOW_SAME_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_BANNER_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_INTER_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_NATIVE
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_OPEN_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_REWARDED_ADS
import com.asianmobile.emojibattery.shimeji.ads.config.NUMBER_CLICK_ADS_TO_LIMIT
import com.asianmobile.emojibattery.shimeji.ads.config.RULE_SHOW_INTER
import com.asianmobile.emojibattery.shimeji.ads.config.SHOW_INTER_LAUNCHER
import com.asianmobile.emojibattery.shimeji.ads.config.TIME_CLICK_ACTION
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdConfigurationIntegrityTest {

    @Test
    fun `every active ad unit string has a non blank value`() {
        val stringsXml = resourceFile("src/main/res/values/strings.xml").readText()
        val allAdUnitValues = Regex(
            pattern = """<string\s+name="(id_emoji_battery_[^"]+)"[^>]*>(.*?)</string>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        ).findAll(stringsXml).associate { match ->
            match.groupValues[1] to match.groupValues[2].trim()
        }
        val invalidAdResourceNames = Regex("<string\\s+name=\"(id_[^\"]+)\"")
            .findAll(stringsXml)
            .map { it.groupValues[1] }
            .filterNot { it.startsWith("id_emoji_battery_") }
            .toSet()

        val activeAdUnitNames = setOf(
            "id_emoji_battery_pub",
            "id_emoji_battery_inter_test",
            "id_emoji_battery_banner_test",
            "id_emoji_battery_open_ads_test",
            "id_emoji_battery_rewarded_test",
            "id_emoji_battery_native_test",
            "id_emoji_battery_inter",
            "id_emoji_battery_banner",
            "id_emoji_battery_open_ads",
            "id_emoji_battery_rewarded",
            "id_emoji_battery_inter_splash",
            "id_emoji_battery_native_language",
            "id_emoji_battery_native_language_second",
            "id_emoji_battery_native_intro",
            "id_emoji_battery_native_intro_second",
            "id_emoji_battery_native_permission",
            "id_emoji_battery_native_grant_permissions",
            "id_emoji_battery_native_accessibility_disclosure",
            "id_emoji_battery_native_overlay_permission",
            "id_emoji_battery_native_search",
            "id_emoji_battery_native_favourite_recent",
            "id_emoji_battery_native_battery_catalog",
            "id_emoji_battery_native_battery_editor",
            "id_emoji_battery_native_battery_reward",
            "id_emoji_battery_native_battery_discard",
            "id_emoji_battery_native_pet_reward",
            "id_emoji_battery_native_food_reward",
            "id_emoji_battery_native_battery_troll_reward",
            "id_emoji_battery_native_exit_dialog"
        )

        val missingNames = activeAdUnitNames - allAdUnitValues.keys
        assertTrue("Missing active ad unit strings: $missingNames", missingNames.isEmpty())
        val blankNames = activeAdUnitNames.filter { allAdUnitValues[it].isNullOrBlank() }
        assertTrue("Blank ad unit strings: $blankNames", blankNames.isEmpty())
        assertTrue(
            "Ad ID resources without Emoji Battery prefix: $invalidAdResourceNames",
            invalidAdResourceNames.isEmpty()
        )
        assertFalse(stringsXml.contains("id_private_browser_"))
        assertFalse(stringsXml.contains("name=\"id_pub\""))
    }

    @Test
    fun `all active ad remote config keys have defaults`() {
        val defaultsXml = resourceFile("src/main/res/xml/remote_config_defaults.xml").readText()
        val defaultKeys = Regex(
            pattern = """<entry>\s*<key>([^<]+)</key>\s*<value>.*?</value>\s*</entry>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        ).findAll(defaultsXml).map { it.groupValues[1].trim() }.toSet()

        val requiredKeys = NativeAdPlacementCatalog.all.mapTo(mutableSetOf()) {
            it.remoteConfigKey
        }.apply {
            addAll(
                setOf(
                    IS_SHOW_BANNER_ADS,
                    IS_SHOW_NATIVE,
                    IS_SHOW_OPEN_ADS,
                    IS_SHOW_INTER_ADS,
                    IS_SHOW_REWARDED_ADS,
                    SHOW_INTER_LAUNCHER,
                    DISTANCE_TIME_SHOW_SAME_ADS,
                    DISTANCE_TIME_SHOW_OTHER_ADS,
                    TIME_CLICK_ACTION,
                    RULE_SHOW_INTER,
                    NUMBER_CLICK_ADS_TO_LIMIT
                )
            )
        }

        val missingKeys = requiredKeys - defaultKeys
        assertTrue("Missing Remote Config defaults: $missingKeys", missingKeys.isEmpty())
    }

    private fun resourceFile(relativePath: String): File =
        sequenceOf(File(relativePath), File("ads/$relativePath"))
            .firstOrNull(File::isFile)
            ?: error("Cannot find $relativePath from ${File(".").absolutePath}")
}
