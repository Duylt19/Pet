package com.asianmobile.emojibattery.shimeji.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizedStringContractTest {

    @Test
    fun `supported locales preserve keys format arguments and brand boundaries`() {
        val resources = resourceRoot()
        val defaults = readStrings(resources.resolve("values/strings.xml"))
            .filterValues { it.isTranslatable }

        SUPPORTED_LOCALES.forEach { locale ->
            val localized = readStrings(resources.resolve("values-$locale/strings.xml"))
            assertEquals("String keys differ for $locale", defaults.keys, localized.keys)
            defaults.forEach { (name, source) ->
                val translation = requireNotNull(localized[name])
                assertEquals(
                    "Format arguments differ for $locale/$name",
                    formatArguments(source.value),
                    formatArguments(translation.value)
                )
                assertTrue(
                    "Brand is joined to adjacent words in $locale/$name: ${translation.value}",
                    hasValidBrandBoundaries(translation.value)
                )
            }
        }
    }

    @Test
    fun `Vietnamese Discover title keeps the brand separated`() {
        val strings = readStrings(resourceRoot().resolve("values-vi/strings.xml"))
        assertEquals(
            "🔥 Đang thịnh hành Emoji Battery",
            strings.getValue("discover_trending_battery_themes").value
        )
    }

    @Test
    fun `Vietnamese Shimeji labels keep readable word boundaries`() {
        val strings = readStrings(resourceRoot().resolve("values-vi/strings.xml"))
        SHIMEJI_LABEL_KEYS.forEach { key ->
            assertTrue(
                "$key has joined words: ${strings.getValue(key).value}",
                strings.getValue(key).value.contains("Shimeji Thú cưng")
            )
        }
    }

    @Test
    fun `Vietnamese status bar labels use sentence case and contextual wording`() {
        val strings = readStrings(resourceRoot().resolve("values-vi/strings.xml"))
        val expectedLabels = mapOf(
            "premium_free" to "Miễn phí",
            "premium_pro" to "Pro",
            "premium_weekly" to "Hàng tuần",
            "premium_monthly" to "Hàng tháng",
            "favourite_recent_favourite_tab" to "Yêu thích",
            "battery_editor_template" to "Mẫu",
            "battery_editor_theme_picker" to "Chủ đề",
            "battery_component_signal" to "Tín hiệu",
            "battery_component_data_short" to "Dữ liệu",
            "battery_component_charge_short" to "Sạc",
            "battery_component_ringer" to "Chế độ âm thanh",
            "battery_background_solid" to "Màu trơn",
            "battery_troll_custom" to "Tùy chỉnh",
            "pet_room_tab_room" to "Phòng",
            "battery_emotion_group_classic" to "Cổ điển",
            "battery_emotion_group_kiiroitori" to "Kiiroitori",
            "battery_emotion_group_molang" to "Molang",
            "battery_emotion_group_tobi" to "Tobi",
        )

        expectedLabels.forEach { (key, expected) ->
            assertEquals("Unexpected Vietnamese label for $key", expected, strings.getValue(key).value)
        }
    }

    @Test
    fun `Vietnamese battery overlay terminology is contextual and readable`() {
        val strings = readStrings(resourceRoot().resolve("values-vi/strings.xml"))
        val expectedStates = mapOf(
            "battery_overlay_state_charging" to "Đang sạc",
            "battery_overlay_state_full" to "Đã sạc đầy",
            "battery_overlay_state_not_charging" to "Đã cắm nguồn nhưng không sạc",
            "battery_overlay_state_discharging" to "Đang sử dụng pin",
            "battery_overlay_state_unknown" to "Trạng thái sạc không xác định",
            "battery_overlay_power_ac" to "Nguồn AC",
            "battery_overlay_power_usb" to "Nguồn USB",
            "battery_overlay_power_wireless" to "Sạc không dây",
            "battery_overlay_power_dock" to "Nguồn từ đế sạc",
            "battery_overlay_wifi_limited" to "Kết nối Wi-Fi bị hạn chế",
            "battery_overlay_wifi_disabled" to "Wi-Fi đã tắt",
            "battery_overlay_wifi_disconnected" to "Wi-Fi đã ngắt kết nối",
            "battery_overlay_cellular_connected" to "Đã kết nối dữ liệu di động",
            "battery_overlay_cellular_limited" to "Kết nối dữ liệu di động bị hạn chế",
            "battery_overlay_airplane_enabled" to "Đã bật chế độ máy bay",
            "battery_overlay_ringer_vibrate" to "Chế độ rung",
            "battery_overlay_ringer_silent" to "Chế độ im lặng",
            "battery_overlay_hotspot_enabled" to "Điểm phát sóng đang bật",
        )

        expectedStates.forEach { (key, expected) ->
            assertEquals("Unexpected Vietnamese overlay text for $key", expected, strings.getValue(key).value)
        }
    }

    @Test
    fun `Vietnamese platform names are separated from surrounding words`() {
        val strings = readStrings(resourceRoot().resolve("values-vi/strings.xml"))
        val joinedPlatformName = Regex("(?:Android|Google Play)\\p{L}")

        strings.forEach { (key, value) ->
            assertTrue(
                "Platform name is joined to adjacent text in $key: ${value.value}",
                !joinedPlatformName.containsMatchIn(value.value)
            )
        }
    }

    @Test
    fun `Vietnamese standalone text starts with sentence case`() {
        val strings = readStrings(resourceRoot().resolve("values-vi/strings.xml"))
        val lowercaseQuantityKeys = setOf(
            "pet_store_food_quantity",
            "pet_room_food_portions",
        )

        strings
            .filterKeys { it !in lowercaseQuantityKeys }
            .forEach { (key, value) ->
                val firstCharacter = value.value.trimStart().firstOrNull()
                assertTrue(
                    "Vietnamese text starts with lowercase in $key: ${value.value}",
                    firstCharacter == null || !firstCharacter.isLowerCase()
                )
            }
    }

    @Test
    fun `accessibility service labels start with the canonical app name`() {
        val resources = resourceRoot()
        val appName = readStrings(resources.resolve("values/strings.xml"))
            .getValue("app_name")
            .value
        val expectedPrefix = "$appName — "

        (listOf("") + SUPPORTED_LOCALES).forEach { locale ->
            val directory = if (locale.isEmpty()) "values" else "values-$locale"
            val label = readStrings(resources.resolve("$directory/strings.xml"))
                .getValue("battery_accessibility_service_label")
                .value
            assertTrue(
                "Accessibility label does not start with app name in $directory: $label",
                label.startsWith(expectedPrefix)
            )
        }
    }

    private fun readStrings(file: File): LinkedHashMap<String, StringValue> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }
        val nodes = factory.newDocumentBuilder().parse(file).getElementsByTagName("string")
        return LinkedHashMap<String, StringValue>().apply {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                put(
                    element.getAttribute("name"),
                    StringValue(
                        value = element.textContent,
                        isTranslatable = element.getAttribute("translatable") != "false"
                    )
                )
            }
        }
    }

    private fun formatArguments(value: String): List<String> = FORMAT_ARGUMENT
        .findAll(value)
        .map(MatchResult::value)
        .sorted()
        .toList()

    private fun hasValidBrandBoundaries(value: String): Boolean = BRAND
        .findAll(value)
        .all { match ->
            val before = value.getOrNull(match.range.first - 1)
            val after = value.getOrNull(match.range.last + 1)
            (before == null || before.isWhitespace() || before in OPEN_BOUNDARIES) &&
                (after == null || after.isWhitespace() || after in CLOSE_BOUNDARIES)
        }

    private fun resourceRoot(): File =
        sequenceOf(File("src/main/res"), File("app/src/main/res"))
            .firstOrNull(File::isDirectory)
            ?: error("Cannot find app resources from ${File(".").absolutePath}")

    private data class StringValue(val value: String, val isTranslatable: Boolean)

    private companion object {
        val SUPPORTED_LOCALES = listOf("af", "ar", "de", "es", "fr", "ha", "hi", "pt", "vi", "zh")
        val FORMAT_ARGUMENT = Regex("%(?:\\d+\\$)?[sdf]")
        val BRAND = Regex("Emoji Battery|Shimeji")
        val SHIMEJI_LABEL_KEYS = listOf(
            "discover_shimeji_pets",
            "discover_tab_pet_store",
            "pet_store_tab_pets",
            "pet_room_open_store",
            "search_tab_pets",
        )
        val OPEN_BOUNDARIES = setOf('"', '“', '‘', '(')
        val CLOSE_BOUNDARIES = setOf('.', ',', '،', '。', ':', ';', '!', '?', '"', '”', '’', ')')
    }
}
