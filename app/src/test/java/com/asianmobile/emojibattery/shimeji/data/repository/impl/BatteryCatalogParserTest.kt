package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BatteryCatalogParserTest {
    private val parser = BatteryCatalogParser()

    @Test
    fun parse_reads_normalized_catalog() {
        val document = parser.parse(validCatalog())

        assertEquals("snapshot-1", document.catalogVersion)
        assertEquals(BatteryCatalogDistributionStatus.REVIEW_REQUIRED, document.distributionStatus)
        assertEquals(1, document.categories.size)
        assertEquals(7, document.themes.single().id)
        assertEquals(BatteryThemeEntitlement.FREE, document.themes.single().entitlement)
        assertEquals("battery/7.png", document.themes.single().battery.path)
        assertEquals("background/template_color_01.png", document.backgrounds.single().asset.path)
        assertEquals("emotion/emotion_01.png", document.emotions.single().asset.path)
    }

    @Test
    fun parse_rejects_mismatched_asset_path() {
        assertThrows(BatteryCatalogParseException::class.java) {
            parser.parse(validCatalog().replace("battery/7.png", "battery/8.png"))
        }
    }

    @Test
    fun parse_rejects_unknown_category() {
        assertThrows(BatteryCatalogParseException::class.java) {
            parser.parse(validCatalog().replace("\"categoryId\": 3", "\"categoryId\": 4"))
        }
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
          "backgrounds": [
            {
              "id": 1,
              "name": "template_color_01",
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
          ]
        }
    """.trimIndent()
}
