package com.asianmobile.emojibattery.shimeji.ui.pet.store

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStorePolicyTest {
    private val pet = OwnerPetCatalogEntry(42, "Bunny", "Rabbit", null, null, false)
    private val categorizedPets = listOf(
        OwnerPetCatalogEntry(1, "Cattey", " Cat ", null, null, false),
        OwnerPetCatalogEntry(2, "Bunny", "Rabbit", null, null, false),
        OwnerPetCatalogEntry(3, "Mochi", "cat", null, null, false)
    )

    @Test
    fun `only installed owner pack is unlocked`() {
        assertFalse(PetStorePolicy.isUnlocked(pet, emptySet()))
        assertTrue(PetStorePolicy.isUnlocked(pet, setOf(pet.installedPackKey)))
    }

    @Test
    fun `store tab navigation values are stable and safely parsed`() {
        assertEquals("pets", PetStoreTab.PETS.navigationValue)
        assertEquals("food", PetStoreTab.FOOD.navigationValue)
        assertEquals(PetStoreTab.FOOD, PetStoreTab.fromNavigationValue("FOOD"))
        assertEquals(null, PetStoreTab.fromNavigationValue("unknown"))
    }

    @Test
    fun `pet start blocker distinguishes no pets from all inactive`() {
        assertEquals(PetStartBlocker.NO_OWNED_PETS, PetStorePolicy.startBlocker(0, 0))
        assertEquals(PetStartBlocker.NO_ACTIVE_PETS, PetStorePolicy.startBlocker(2, 0))
        assertEquals(null, PetStorePolicy.startBlocker(2, 1))
    }

    @Test
    fun `unlock requests overlay before any remaining permission`() {
        assertEquals(
            PetUnlockActivation.REQUEST_OVERLAY,
            PetStorePolicy.activationAfterUnlock(
                overlayGranted = false,
                notificationGranted = false
            )
        )
    }

    @Test
    fun `unlock requests remaining permission when overlay is already granted`() {
        assertEquals(
            PetUnlockActivation.REQUEST_REMAINING_PERMISSIONS,
            PetStorePolicy.activationAfterUnlock(
                overlayGranted = true,
                notificationGranted = false
            )
        )
    }

    @Test
    fun `unlock starts pet immediately when mandatory permissions are granted`() {
        assertEquals(
            PetUnlockActivation.START_PET,
            PetStorePolicy.activationAfterUnlock(
                overlayGranted = true,
                notificationGranted = true
            )
        )
    }

    @Test
    fun `owned pet count only includes installed catalog pets`() {
        assertEquals(
            1,
            PetStorePolicy.ownedPetCount(
                categorizedPets,
                setOf(categorizedPets.first().installedPackKey, "unknown-pack")
            )
        )
    }

    @Test
    fun `name is trimmed bounded and falls back to catalog name`() {
        assertEquals("Mochi", PetStorePolicy.normalizedName("  Mochi  ", pet.name))
        assertEquals("Bunny", PetStorePolicy.normalizedName("   ", pet.name))
        assertEquals(24, PetStorePolicy.normalizedName("x".repeat(40), pet.name).length)
    }

    @Test
    fun `categories preserve catalog order and remove case insensitive duplicates`() {
        assertEquals(listOf("Cat", "Rabbit"), PetStorePolicy.categories(categorizedPets))
    }

    @Test
    fun `category selection retains a valid request and falls back to first category`() {
        assertEquals(
            "Rabbit",
            PetStorePolicy.selectedCategory(categorizedPets, requestedCategory = "rabbit")
        )
        assertEquals(
            "Cat",
            PetStorePolicy.selectedCategory(categorizedPets, requestedCategory = "Bird")
        )
    }

    @Test
    fun `pet filtering follows normalized selected category`() {
        assertEquals(
            listOf(1, 3),
            PetStorePolicy.petsInCategory(categorizedPets, category = "CAT").map { it.id }
        )
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
