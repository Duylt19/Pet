package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetComboCatalogTest {
    @Test
    fun `catalog keeps the full ordered story when pack supports every action`() {
        val combo = PetComboCatalog.supportedDefinition(
            PetComboId.HAPPY_ZOOMIES,
            PetAction.entries.toSet()
        )

        assertEquals(
            listOf(
                PetAction.IDLE,
                PetAction.WINK,
                PetAction.RUN,
                PetAction.IDLE
            ),
            combo?.actions
        )
    }

    @Test
    fun `catalog degrades a combo to actions actually provided by the pack`() {
        val supported = setOf(PetAction.IDLE, PetAction.WALK, PetAction.TAPPED)

        val compatible = PetComboCatalog.supportedDefinition(
            PetComboId.SLOW_MORNING,
            supported
        )
        val incompatible = PetComboCatalog.supportedDefinition(
            PetComboId.TINY_PERFORMANCE,
            supported
        )

        assertEquals(listOf(PetAction.IDLE, PetAction.WALK), compatible?.actions)
        assertNull(incompatible)
    }

    @Test
    fun `catalog exposes many solo and paired stories without adjacent empty steps`() {
        val ids = PetComboId.entries
        val resolved = ids.mapNotNull { id ->
            PetComboCatalog.supportedDefinition(id, PetAction.entries.toSet())
        }

        assertTrue(resolved.size >= 20)
        assertTrue(resolved.all { it.actions.size >= 2 })
        assertTrue(
            resolved.flatMap(PetComboDefinition::beats)
                .filter(PetComboBeat::isSustained)
                .all { beat -> checkNotNull(beat.durationMillis).first >= 1_500L }
        )
    }
}
