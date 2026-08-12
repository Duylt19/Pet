package com.asianmobile.emojibattery.shimeji.data.remote

import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollDistributionStatus
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertThrows

class BatteryTrollCatalogParserTest {
    private val parser = BatteryTrollCatalogParser()

    @Test
    fun `parses the published Battery Troll catalog`() {
        val document = parser.parse(catalog())

        assertEquals("troll-webp-2026-08-12-v1", document.catalogVersion)
        assertEquals("2026-08-12T09:00:00.000Z", document.capturedAt)
        assertEquals(BatteryTrollDistributionStatus.APPROVED, document.distributionStatus)
        assertEquals(2, document.trolls.size)
        val troll = document.trolls.first()
        assertEquals(1, troll.id)
        assertEquals("Spider Hero", troll.name)
        assertEquals("troll_1", troll.slug)
        assertEquals(BatteryTrollEntitlement.FREE, troll.entitlement)
        assertEquals(
            BatteryTrollBatteryOrientation.LANDSCAPE,
            troll.batteryOrientation
        )
        assertEquals("thumb/TROLL_1.webp", troll.thumbnail.path)
        assertEquals(5, troll.emoji.size)
        assertEquals(5, troll.battery.size)
        assertEquals("emoji/TROLL_1_1.webp", troll.emoji.first().path)
        assertEquals("emoji/TROLL_1_5.webp", troll.emoji.last().path)
        assertEquals("battery/TROLL_1_1.webp", troll.battery.first().path)
        assertEquals(11, troll.assets.size)
    }

    @Test
    fun `sorts trolls by their published order`() {
        val json = mutate { root ->
            root.getJSONArray("trolls").getJSONObject(0).put("order", 5)
        }

        assertEquals(listOf(2, 1), parser.parse(json).trolls.map { it.id })
    }

    @Test
    fun `ignores unknown fields`() {
        val json = mutate { root ->
            root.put("previewChannel", "beta")
            root.getJSONArray("trolls").getJSONObject(0).put("tags", JSONArray(listOf("hero")))
        }

        assertEquals(2, parser.parse(json).trolls.size)
    }

    @Test
    fun `reports a review required catalog without approving it`() {
        val json = mutate { root ->
            root.getJSONObject("source").put("distributionStatus", "REVIEW_REQUIRED")
        }

        assertEquals(
            BatteryTrollDistributionStatus.REVIEW_REQUIRED,
            parser.parse(json).distributionStatus
        )
    }

    @Test
    fun `rejects a newer schema version cleanly`() {
        val json = mutate { root -> root.put("schemaVersion", 2) }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a troll with too few emoji frames`() {
        val json = mutate { root ->
            val emoji = root.getJSONArray("trolls")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONArray("emoji")
            emoji.remove(4)
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a troll with too many battery frames`() {
        val json = mutate { root ->
            val battery = root.getJSONArray("trolls")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONArray("battery")
            battery.put(JSONObject(asset("battery/TROLL_1_6.webp")))
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an unknown entitlement`() {
        val json = mutate { root ->
            root.getJSONArray("trolls").getJSONObject(0).put("entitlement", "TRIAL")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an unknown battery orientation`() {
        val json = mutate { root ->
            root.getJSONArray("trolls").getJSONObject(0).put("batteryOrientation", "DIAGONAL")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an unknown distribution status`() {
        val json = mutate { root ->
            root.getJSONObject("source").put("distributionStatus", "DRAFT")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a troll count that does not match`() {
        val json = mutate { root -> root.put("trollCount", 10) }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects duplicate troll identifiers`() {
        val json = mutate { root ->
            root.getJSONArray("trolls").getJSONObject(1).put("id", 1)
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects duplicate troll slugs`() {
        val json = mutate { root ->
            root.getJSONArray("trolls").getJSONObject(1).put("slug", "troll_1")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a thumbnail outside the thumb directory`() {
        val json = mutate { root ->
            root.getJSONArray("trolls")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONObject("thumbnail")
                .put("path", "battery/TROLL_1.webp")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an escaping asset path`() {
        val json = mutate { root ->
            root.getJSONArray("trolls")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONArray("emoji")
                .getJSONObject(0)
                .put("path", "../../json/pets.json")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an asset without a usable hash`() {
        val json = mutate { root ->
            root.getJSONArray("trolls")
                .getJSONObject(0)
                .getJSONObject("assets")
                .getJSONObject("thumbnail")
                .put("sha256", "")
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects an empty catalog`() {
        val json = mutate { root ->
            root.put("trolls", JSONArray())
            root.put("trollCount", 0)
        }

        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse(json) }
    }

    @Test
    fun `rejects a malformed document`() {
        assertThrows(BatteryTrollCatalogParseException::class.java) { parser.parse("not json") }
    }

    private fun mutate(block: (JSONObject) -> Unit): String =
        JSONObject(catalog()).also(block).toString()

    private fun catalog(): String = """
        {
          "schemaVersion": 1,
          "catalogVersion": "troll-webp-2026-08-12-v1",
          "capturedAt": "2026-08-12T09:00:00.000Z",
          "source": {
            "designFile": "hjefC57z0ysLDHdP60VqMK",
            "distributionStatus": "APPROVED"
          },
          "trollCount": 2,
          "trolls": [
            ${troll(id = 1, name = "Spider Hero", order = 0, entitlement = "FREE")},
            ${troll(id = 2, name = "Ghost Pilot", order = 1, entitlement = "PREMIUM")}
          ]
        }
    """.trimIndent()

    private fun troll(
        id: Int,
        name: String,
        order: Int,
        entitlement: String,
        orientation: String = "LANDSCAPE"
    ): String = """
        {
          "id": $id,
          "name": "$name",
          "slug": "troll_$id",
          "order": $order,
          "entitlement": "$entitlement",
          "batteryOrientation": "$orientation",
          "assets": {
            "thumbnail": ${asset("thumb/TROLL_$id.webp")},
            "emoji": [
              ${(1..5).joinToString(",") { asset("emoji/TROLL_${id}_$it.webp") }}
            ],
            "battery": [
              ${(1..5).joinToString(",") { asset("battery/TROLL_${id}_$it.webp") }}
            ]
          }
        }
    """.trimIndent()

    private fun asset(path: String): String = """
        {
          "path": "$path",
          "sizeBytes": 24680,
          "sha256": "$ASSET_SHA",
          "width": 320,
          "height": 120
        }
    """.trimIndent()

    private companion object {
        const val ASSET_SHA =
            "71e08043b5418444f6ecf46e72158354c8fa449dd36f0c48fb990544c4f37ad0"
    }
}
