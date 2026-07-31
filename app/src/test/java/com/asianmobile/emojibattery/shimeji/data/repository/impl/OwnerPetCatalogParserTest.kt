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
                  },
                  "speechAnchor": {
                    "x": 0.421875,
                    "y": 0.671875
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
        assertEquals(
            OwnerPetCatalogSpeechAnchorRecord(0.421875f, 0.671875f),
            document.records.single().speechAnchor
        )
    }

    @Test
    fun `parseDocument accepts football supplement records from the server catalog`() {
        val document = parser.parseDocument(
            """
            {
              "schemaVersion": 1,
              "catalogVersion": "2026-07-31-football-1",
              "source": {
                "commit": "ed39a3d61e1a733b3f21cf6575650a17f359127f",
                "supplements": [{"id":"wc-2026","petCount":48}]
              },
              "petCount": 1,
              "categories": [{"name":"WC 2026","petCount":1}],
              "pets": [
                {
                  "id": 2004,
                  "name": "Argentina",
                  "category": "WC 2026",
                  "author": null,
                  "archive": {
                    "path": "data/2004.zip",
                    "sizeBytes": 1234,
                    "sha256": "${"a".repeat(64)}"
                  },
                  "thumbnail": {
                    "path": "thumb/2004.png",
                    "sizeBytes": 321,
                    "sha256": "${"b".repeat(64)}"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val footballPet = document.records.single()
        assertEquals("2026-07-31-football-1", document.catalogVersion)
        assertEquals(2004, footballPet.id)
        assertEquals("Argentina", footballPet.name)
        assertEquals("WC 2026", footballPet.category)
        assertEquals(null, footballPet.speechAnchor)
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
        assertEquals(null, document.records.single().speechAnchor)
    }

    @Test
    fun `parseDocument rejects invalid speech anchor metadata`() {
        listOf(
            """{"x":-0.1,"y":0.5}""",
            """{"x":0.5,"y":1.1}""",
            """{"x":"0.5","y":0.5}""",
            """null"""
        ).forEach { speechAnchor ->
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
                          "speechAnchor": $speechAnchor
                        }
                      ]
                    }
                    """.trimIndent()
                )
            }
        }
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
