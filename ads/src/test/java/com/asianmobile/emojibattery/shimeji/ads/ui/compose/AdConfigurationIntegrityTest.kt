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
import org.junit.Assert.assertTrue
import org.junit.Test

class AdConfigurationIntegrityTest {

    @Test
    fun `every active ad unit string has a non blank value`() {
        val stringsXml = resourceFile("src/main/res/values/strings.xml").readText()
        val allAdUnitValues = Regex(
            pattern = """<string\s+name="(id_(?:pub|private_browser_[^"]+))"[^>]*>(.*?)</string>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        ).findAll(stringsXml).associate { match ->
            match.groupValues[1] to match.groupValues[2].trim()
        }

        val activeAdUnitNames = setOf(
            "id_pub",
            "id_private_browser_inter_test",
            "id_private_browser_banner_test",
            "id_private_browser_open_ads_test",
            "id_private_browser_rewarded_test",
            "id_private_browser_native_test",
            "id_private_browser_inter",
            "id_private_browser_banner",
            "id_private_browser_open_ads",
            "id_private_browser_rewarded",
            "id_private_browser_inter_splash",
            "id_private_browser_native_language",
            "id_private_browser_native_language_second",
            "id_private_browser_native_intro",
            "id_private_browser_native_intro_second",
            "id_private_browser_native_permission",
            "id_private_browser_native_home",
            "id_private_browser_native_exit_dialog"
        )

        val missingNames = activeAdUnitNames - allAdUnitValues.keys
        assertTrue("Missing active ad unit strings: $missingNames", missingNames.isEmpty())
        val blankNames = activeAdUnitNames.filter { allAdUnitValues[it].isNullOrBlank() }
        assertTrue("Blank ad unit strings: $blankNames", blankNames.isEmpty())
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
