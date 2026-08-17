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
