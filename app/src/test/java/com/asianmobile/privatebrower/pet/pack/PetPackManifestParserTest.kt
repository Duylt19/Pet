package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PetPackManifestParserTest {
    private val parser = PetPackManifestParser()

    @Test
    fun `parse reads versioned clips frame rect anchor and interaction`() {
        val manifest = parser.parse(validManifestJson())

        assertEquals(PET_PACK_SCHEMA_VERSION, manifest.schemaVersion)
        assertEquals("demo.orange-cat", manifest.id)
        assertEquals(3, manifest.clips.size)
        assertEquals(PetAction.TAPPED, manifest.interaction.tapAction)
        assertEquals(PetPackAnchor(0.5f, 1f), manifest.anchor)
        assertEquals(120L, manifest.clips.getValue(PetAction.WALK).frames.first().durationMillis)
    }

    @Test
    fun `parse rejects unknown action`() {
        val invalid = validManifestJson().replace("\"walk\"", "\"teleport\"")

        assertThrows(PetPackFormatException::class.java) {
            parser.parse(invalid)
        }
    }

    @Test
    fun `parse rejects missing required object`() {
        val invalid = validManifestJson().replace("\"anchor\"", "\"removedAnchor\"")

        assertThrows(PetPackFormatException::class.java) {
            parser.parse(invalid)
        }
    }

    @Test
    fun `parse rejects duplicate clip actions`() {
        val invalid = validManifestJson().replaceFirst("\"action\": \"walk\"", "\"action\": \"idle\"")

        assertThrows(PetPackFormatException::class.java) {
            parser.parse(invalid)
        }
    }
}

internal fun validManifestJson(frameFile: String = "sprites/cat.png"): String =
    """
    {
      "schemaVersion": 1,
      "id": "demo.orange-cat",
      "version": 1,
      "name": "Orange Cat",
      "author": "Cute Pet",
      "canvas": { "width": 128, "height": 128, "defaultScale": 1.0 },
      "anchor": { "x": 0.5, "y": 1.0 },
      "interaction": { "tapAction": "tapped" },
      "clips": [
        {
          "action": "idle",
          "loop": true,
          "frames": [
            { "file": "$frameFile", "rect": { "x": 0, "y": 0, "width": 128, "height": 128 }, "durationMs": 180 }
          ]
        },
        {
          "action": "walk",
          "loop": true,
          "frames": [
            { "file": "$frameFile", "rect": { "x": 0, "y": 0, "width": 128, "height": 128 }, "durationMs": 120, "velocity": { "x": 42, "y": 0 } }
          ]
        },
        {
          "action": "tapped",
          "loop": false,
          "nextAction": "idle",
          "frames": [
            { "file": "$frameFile", "rect": { "x": 0, "y": 0, "width": 128, "height": 128 }, "durationMs": 100 }
          ]
        }
      ]
    }
    """.trimIndent()
