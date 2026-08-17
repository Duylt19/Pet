package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BatteryCatalogParserTest {
    private val parser = BatteryCatalogParser()

    @Test
    fun parse_reads_normalized_catalog() {
        val document = parser.parse(validCatalog())

        assertEquals("snapshot-1", document.catalogVersion)
        assertEquals(
            DEFAULT_DISCOVER_TRENDING_EMOJI_THEME_IDS,
            document.trendingEmojiThemeIds
        )
        assertEquals(BatteryCatalogDistributionStatus.REVIEW_REQUIRED, document.distributionStatus)
        assertEquals(1, document.categories.size)
        assertEquals(7, document.themes.single().id)
        assertEquals(BatteryThemeEntitlement.FREE, document.themes.single().entitlement)
        assertEquals("battery/7.png", document.themes.single().battery.path)
        assertEquals("background/template_color_01.png", document.backgrounds.single().asset.path)
        assertEquals(
            "background_preview/template_color_01.png",
            document.backgrounds.single().preview?.path
        )
        assertEquals(0, document.backgrounds.single().order)
        assertEquals("emotion/emotion_01.png", document.emotions.single().asset.path)
        assertEquals("emotion/emotion_01.png", document.emotions.single().preview?.path ?: document.emotions.single().asset.path)
        assertEquals("classic", document.emotionGroups.single().key)
        assertEquals("animation/cute_1.json", document.animations.single().asset.path)
    }

    @Test
    fun parse_reads_optional_trending_order_and_preserves_explicit_empty_list() {
        val curated = JSONObject(validCatalog())
            .put("trendingEmojiThemeIds", JSONArray().put(919).put(-1).put(919).put(7))
        val hidden = JSONObject(validCatalog())
            .put("trendingEmojiThemeIds", JSONArray())

        assertEquals(listOf(919, 7), parser.parse(curated.toString()).trendingEmojiThemeIds)
        assertEquals(emptyList<Int>(), parser.parse(hidden.toString()).trendingEmojiThemeIds)
    }

    @Test
    fun parse_rejects_mismatched_asset_path() {
        assertThrows(BatteryCatalogParseException::class.java) {
            parser.parse(validCatalog().replace("battery/7.png", "battery/8.png"))
        }
    }

    @Test
    fun parse_accepts_lossless_webp_for_static_battery_images() {
        val document = parser.parse(
            validCatalog()
                .replace("thumb/7.png", "thumb/7.webp")
                .replace("battery/7.png", "battery/7.webp")
                .replace("emoji/7.png", "emoji/7.webp")
                .replace(
                    "background/template_color_01.png",
                    "background/template_color_01.webp"
                )
                .replace(
                    "background_preview/template_color_01.png",
                    "background_preview/template_color_01.webp"
                )
                .replace("emotion/emotion_01.png", "emotion/emotion_01.webp")
        )

        assertEquals("thumb/7.webp", document.themes.single().thumbnail.path)
        assertEquals("battery/7.webp", document.themes.single().battery.path)
        assertEquals("emoji/7.webp", document.themes.single().emoji.path)
        assertEquals(
            "background/template_color_01.webp",
            document.backgrounds.single().asset.path
        )
        assertEquals("emotion/emotion_01.webp", document.emotions.single().asset.path)
    }

    @Test
    fun parse_accepts_webp_emotion_group_background() {
        val root = JSONObject(validCatalog())
            .put("emotionCount", 1)
            .put("emotionGroupCount", 1)
            .put(
                "emotionGroups",
                JSONArray().put(
                    JSONObject()
                        .put("key", "classic")
                        .put("order", 0)
                        .put("emotionIds", JSONArray().put(1))
                        .put(
                            "background",
                            JSONObject()
                                .put("path", "emotion_group/classic.webp")
                                .put("sizeBytes", 8)
                                .put("sha256", "2".repeat(64))
                                .put("width", 656)
                                .put("height", 270)
                        )
                )
            )

        assertEquals(
            "emotion_group/classic.webp",
            parser.parse(root.toString()).emotionGroups.single().background?.path
        )
    }

    @Test
    fun parse_rejects_unsupported_static_image_extension() {
        assertThrows(BatteryCatalogParseException::class.java) {
            parser.parse(validCatalog().replace("battery/7.png", "battery/7.jpg"))
        }
    }

    @Test
    fun parse_rejects_unknown_category() {
        assertThrows(BatteryCatalogParseException::class.java) {
            parser.parse(validCatalog().replace("\"categoryId\": 3", "\"categoryId\": 4"))
        }
    }

    @Test
    fun parse_rejects_animation_path_escape() {
        assertThrows(BatteryCatalogParseException::class.java) {
            parser.parse(
                validCatalog().replace(
                    "animation/cute_1.json",
                    "animation/../cute_1.json"
                )
            )
        }
    }

    @Test
    fun parse_reads_emotion_preview_and_group_metadata() {
        val updated = validCatalog()
            .replace(
                "\"emotions\": [",
                "\"emotionCount\": 1,\n  \"emotionGroupCount\": 1,\n" +
                    "  \"emotionGroups\": [{\"key\":\"classic\",\"order\":0," +
                    "\"emotionIds\":[1]}],\n  \"emotions\": ["
            )
            .replace(
                "\"name\": \"emotion_01\",",
                "\"name\": \"emotion_01\",\n              \"groupKey\": \"classic\"," +
                    "\n              \"order\": 0,\n              \"preview\": {" +
                    "\n                \"path\": \"emotion_preview/emotion_01.png\"," +
                    "\n                \"sizeBytes\": 7,\n                \"sha256\": \"${"1".repeat(64)}\"," +
                    "\n                \"width\": 72,\n                \"height\": 72\n              },"
            )

        val document = parser.parse(updated)

        assertEquals("emotion_preview/emotion_01.png", document.emotions.single().preview?.path)
        assertEquals("classic", document.emotions.single().groupKey)
        assertEquals(listOf(1), document.emotionGroups.single().emotionIds)
    }

    @Test
    fun parse_sorts_backgrounds_by_explicit_order() {
        val root = JSONObject(validCatalog())
        val template = root.getJSONArray("backgrounds").getJSONObject(0)
        val first = JSONObject(template.toString()).apply {
            put("id", 2)
            put("name", "template_color_02")
            put("order", 1)
            getJSONObject("asset").put("path", "background/template_color_02.png")
            getJSONObject("preview").put(
                "path",
                "background_preview/template_color_02.png"
            )
        }
        val second = JSONObject(template.toString()).apply {
            put("id", 1)
            put("order", 0)
        }
        root.put("backgroundCount", 2)
        root.put("backgrounds", JSONArray().put(first).put(second))

        val document = parser.parse(root.toString())

        assertEquals(listOf(1, 2), document.backgrounds.map { it.id })
    }

    private fun validCatalog(): String = """
        {
          "schemaVersion": 1,
          "catalogVersion": "snapshot-1",
          "capturedAt": "2026-07-30T03:19:48.346Z",
          "source": {
            "distributionStatus": "REVIEW_REQUIRED"
          },
          "categoryCount": 1,
          "themeCount": 1,
          "categories": [
            {"id": 3, "name": "Cute", "slug": "cute", "priority": 0}
          ],
          "themes": [
            {
              "id": 7,
              "name": "Theme 7",
              "categoryId": 3,
              "categoryName": "Cute",
              "entitlement": "FREE",
              "assets": {
                "thumbnail": {
                  "path": "thumb/7.png",
                  "sizeBytes": 10,
                  "sha256": "${"a".repeat(64)}",
                  "width": 200,
                  "height": 200
                },
                "battery": {
                  "path": "battery/7.png",
                  "sizeBytes": 11,
                  "sha256": "${"b".repeat(64)}",
                  "width": 200,
                  "height": 200
                },
                "emoji": {
                  "path": "emoji/7.png",
                  "sizeBytes": 12,
                  "sha256": "${"c".repeat(64)}",
                  "width": 200,
                  "height": 200
                }
              }
            }
          ],
          "backgroundCount": 1,
          "backgrounds": [
            {
              "id": 1,
              "name": "template_color_01",
              "order": 0,
              "preview": {
                "path": "background_preview/template_color_01.png",
                "sizeBytes": 7,
                "sha256": "${"1".repeat(64)}",
                "width": 360,
                "height": 40
              },
              "asset": {
                "path": "background/template_color_01.png",
                "sizeBytes": 13,
                "sha256": "${"d".repeat(64)}",
                "width": 1080,
                "height": 120
              }
            }
          ],
          "emotions": [
            {
              "id": 1,
              "name": "emotion_01",
              "asset": {
                "path": "emotion/emotion_01.png",
                "sizeBytes": 14,
                "sha256": "${"e".repeat(64)}",
                "width": 200,
                "height": 200
              }
            }
          ],
          "animations": [
            {
              "id": 1,
              "name": "cute_1.json",
              "type": "LOTTIE",
              "asset": {
                "path": "animation/cute_1.json",
                "sizeBytes": 15,
                "sha256": "${"f".repeat(64)}",
                "width": 200,
                "height": 200
              }
            }
          ]
        }
    """.trimIndent()
}
