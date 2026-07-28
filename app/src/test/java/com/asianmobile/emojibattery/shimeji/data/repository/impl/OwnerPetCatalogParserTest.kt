package com.asianmobile.emojibattery.shimeji.data.repository.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerPetCatalogParserTest {
    private val parser = OwnerPetCatalogParser()

    @Test
    fun `parse returns owner catalog metadata`() {
        val records = parser.parse(
            """
            [
              {"id":42,"name":"Pikachu","category":"Pokemon","author":"Creator"},
              {"id":43,"name":"Cat","category":"Animals"}
            ]
            """.trimIndent()
        )

        assertEquals(2, records.size)
        assertEquals(42, records.first().id)
        assertEquals("Pokemon", records.first().category)
        assertEquals(null, records.last().author)
    }

    @Test
    fun `parse rejects duplicate IDs`() {
        assertThrows(OwnerPetCatalogParseException::class.java) {
            parser.parse(
                """
                [
                  {"id":42,"name":"One","category":"A"},
                  {"id":42,"name":"Two","category":"B"}
                ]
                """.trimIndent()
            )
        }
    }

    @Test
    fun `parseDocument reads versioned server catalog and integrity metadata`() {
        val document = parser.parseDocument(
            """
            {
              "schemaVersion": 1,
              "catalogVersion": "2026-07-22",
              "petCount": 1,
              "categories": [{"name":"Pokemon","petCount":1}],
              "pets": [
                {
                  "id": 42,
                  "name": "Pikachu",
                  "category": "Pokemon",
                  "author": "Creator",
                  "archive": {
                    "path": "data/42.zip",
                    "sizeBytes": 1234,
                    "sha256": "${"a".repeat(64)}"
                  },
                  "thumbnail": {
                    "path": "thumb/42.png",
                    "sizeBytes": 321,
                    "sha256": "${"b".repeat(64)}"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("2026-07-22", document.catalogVersion)
        assertEquals(1, document.records.size)
        assertEquals("Creator", document.records.single().author)
        assertEquals("data/42.zip", document.records.single().archive?.path)
        assertEquals(1234L, document.records.single().archive?.sizeBytes)
        assertEquals("thumb/42.png", document.records.single().thumbnail?.path)
    }

    @Test
    fun `parseDocument keeps JSON null author absent`() {
        val document = parser.parseDocument(
            """
            {
              "schemaVersion": 1,
              "catalogVersion": "v1",
              "petCount": 1,
              "pets": [{"id":42,"name":"Pikachu","category":"Pokemon","author":null}]
            }
            """.trimIndent()
        )

        assertEquals(null, document.records.single().author)
    }

    @Test
    fun `parseDocument rejects count mismatch and unsafe paths`() {
        val countError = assertThrows(OwnerPetCatalogParseException::class.java) {
            parser.parseDocument(
                """
                {
                  "schemaVersion": 1,
                  "catalogVersion": "v1",
                  "petCount": 2,
                  "pets": [{"id":42,"name":"Pikachu","category":"Pokemon"}]
                }
                """.trimIndent()
            )
        }
        assertTrue(countError.message.orEmpty().contains("count"))

        assertThrows(OwnerPetCatalogParseException::class.java) {
            parser.parseDocument(
                """
                {
                  "schemaVersion": 1,
                  "catalogVersion": "v1",
                  "petCount": 1,
                  "pets": [
                    {
                      "id": 42,
                      "name": "Pikachu",
                      "category": "Pokemon",
                      "archive": {
                        "path": "../42.zip",
                        "sizeBytes": 1234,
                        "sha256": "${"a".repeat(64)}"
                      },
                      "thumbnail": {
                        "path": "thumb/42.png",
                        "sizeBytes": 321,
                        "sha256": "${"b".repeat(64)}"
                      }
                    }
                  ]
                }
                """.trimIndent()
            )
        }
    }
}
