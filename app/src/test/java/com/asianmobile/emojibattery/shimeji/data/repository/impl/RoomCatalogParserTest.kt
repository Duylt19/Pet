package com.asianmobile.emojibattery.shimeji.data.repository.impl

import com.asianmobile.emojibattery.shimeji.data.model.PetRoomEntitlement
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCatalogParserTest {
    private val parser = RoomCatalogParser()

    @Test
    fun `parses the published room catalog`() {
        val document = parser.parse(catalog())

        assertEquals("room-2026-08-06", document.catalogVersion)
        assertEquals(4, document.defaultRoomId)
        assertEquals(2, document.rooms.size)
        assertTrue(document.distributionApproved)
        val room = document.rooms.last()
        assertEquals("Green Living Room", room.name)
        assertEquals("bg_4", room.slug)
        assertEquals(PetRoomEntitlement.FREE, room.entitlement)
        assertEquals("bg/BG_4.png", room.background.path)
        assertEquals("thumb/BG_4.png", room.thumbnail.path)
        assertEquals(1080, room.background.width)
        assertEquals(240, room.thumbnail.width)
    }

    @Test
    fun `parses WebP room assets`() {
        val document = parser.parse(catalog().replace(".png", ".webp"))

        assertEquals("bg/BG_4.webp", document.rooms.last().background.path)
        assertEquals("thumb/BG_4.webp", document.rooms.last().thumbnail.path)
    }

    @Test
    fun `reports a review required catalog as unapproved`() {
        val json = mutate { root ->
            root.getJSONObject("source").put("distributionStatus", "REVIEW_REQUIRED")
        }

        assertEquals(false, parser.parse(json).distributionApproved)
    }

    @Test
    fun `rejects an unknown schema version`() {
        val json = mutate { root -> root.put("schemaVersion", 2) }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a room count that does not match`() {
        val json = mutate { root -> root.put("roomCount", 9) }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a default room that is absent from the catalog`() {
        val json = mutate { root -> root.put("defaultRoomId", 77) }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an asset path that does not match its room`() {
        val json = mutate { root ->
            root.getJSONArray("rooms")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONObject("background")
                .put("path", "bg/BG_9.png")
        }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a preview that is not lighter than its background`() {
        val json = mutate { root ->
            root.getJSONArray("rooms")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONObject("thumbnail")
                .put("sizeBytes", 9_000_000L)
        }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a preview that is not preview sized`() {
        val json = mutate { root ->
            root.getJSONArray("rooms")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONObject("thumbnail")
                .put("width", 1080)
                .put("height", 2400)
        }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects duplicate room identifiers`() {
        val json = mutate { root ->
            val rooms = root.getJSONArray("rooms")
            rooms.getJSONObject(1).put("id", 1)
        }

        assertThrows(RoomCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a malformed document`() {
        assertThrows(RoomCatalogParseException::class.java) { parser.parse("not json") }
    }

    private fun mutate(block: (JSONObject) -> Unit): String =
        JSONObject(catalog()).also(block).toString()

    private fun catalog(): String = """
        {
          "schemaVersion": 1,
          "catalogVersion": "room-2026-08-06",
          "capturedAt": "2026-08-06T12:59:19.332Z",
          "source": {
            "designFile": "hjefC57z0ysLDHdP60VqMK",
            "distributionStatus": "APPROVED"
          },
          "roomCount": 2,
          "defaultRoomId": 4,
          "rooms": [
            {
              "id": 1,
              "name": "Cozy Living Room",
              "slug": "bg_1",
              "entitlement": "FREE",
              "assets": {
                "background": {
                  "path": "bg/BG_1.png",
                  "sizeBytes": 1232820,
                  "sha256": "$BACKGROUND_SHA",
                  "width": 841,
                  "height": 1871
                },
                "thumbnail": {
                  "path": "thumb/BG_1.png",
                  "sizeBytes": 60823,
                  "sha256": "$THUMBNAIL_SHA",
                  "width": 240,
                  "height": 534
                }
              }
            },
            {
              "id": 4,
              "name": "Green Living Room",
              "slug": "bg_4",
              "entitlement": "FREE",
              "assets": {
                "background": {
                  "path": "bg/BG_4.png",
                  "sizeBytes": 2334961,
                  "sha256": "$BACKGROUND_SHA",
                  "width": 1080,
                  "height": 2400
                },
                "thumbnail": {
                  "path": "thumb/BG_4.png",
                  "sizeBytes": 43182,
                  "sha256": "$THUMBNAIL_SHA",
                  "width": 240,
                  "height": 533
                }
              }
            }
          ]
        }
    """.trimIndent()

    private companion object {
        const val BACKGROUND_SHA =
            "71e08043b5418444f6ecf46e72158354c8fa449dd36f0c48fb990544c4f37ad0"
        const val THUMBNAIL_SHA =
            "b0fe7e66deba7579477e4c6beb9afd55a1c977ff7b722c5bac5f863f5e43ab97"
    }
}
