package com.asianmobile.emojibattery.shimeji.ui.pet.store

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStorePolicyTest {
    private val pet = OwnerPetCatalogEntry(42, "Bunny", "Rabbit", null, null, false)

    @Test
    fun `only installed owner pack is unlocked`() {
        assertFalse(PetStorePolicy.isUnlocked(pet, emptySet()))
        assertTrue(PetStorePolicy.isUnlocked(pet, setOf(pet.installedPackKey)))
    }

    @Test
    fun `name is trimmed bounded and falls back to catalog name`() {
        assertEquals("Mochi", PetStorePolicy.normalizedName("  Mochi  ", pet.name))
        assertEquals("Bunny", PetStorePolicy.normalizedName("   ", pet.name))
        assertEquals(24, PetStorePolicy.normalizedName("x".repeat(40), pet.name).length)
    }

    @Test
    fun `unlock reveal prefers the primary special movement skill`() {
        assertEquals(
            PetAction.SPECIAL,
            PetStorePolicy.specialSkillAction(setOf(PetAction.SPECIAL_2, PetAction.SPECIAL))
        )
        assertEquals(
            PetAction.SPECIAL_2,
            PetStorePolicy.specialSkillAction(setOf(PetAction.IDLE, PetAction.SPECIAL_2))
        )
        assertEquals(null, PetStorePolicy.specialSkillAction(setOf(PetAction.IDLE)))
    }
}
