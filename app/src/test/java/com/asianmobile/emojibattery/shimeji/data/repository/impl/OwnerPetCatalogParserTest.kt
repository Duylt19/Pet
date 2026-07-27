package com.asianmobile.emojibattery.shimeji.data.repository.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
}
