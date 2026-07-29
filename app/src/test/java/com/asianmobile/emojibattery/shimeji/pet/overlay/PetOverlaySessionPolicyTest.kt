package com.asianmobile.emojibattery.shimeji.pet.overlay

import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class PetOverlaySessionPolicyTest {
    private val activeSwarm = PetPreferences(
        displayMode = PetDisplayMode.SWARM,
        swarm = PetSwarmPreferences(
            packKey = "owner.shimeji.42@7",
            count = 6,
            sizePercent = 80,
            speedPercent = 100
        )
    )

    @Test
    fun `swarm count change updates runtime without rebuilding session`() {
        val update = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm.copy(
                swarm = activeSwarm.swarm.copy(count = 9)
            )
        )

        assertEquals(PetOverlaySessionUpdate.SWARM_RUNTIME, update)
    }

    @Test
    fun `unchanged swarm keeps current controller`() {
        val update = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm
        )

        assertEquals(PetOverlaySessionUpdate.NONE, update)
    }

    @Test
    fun `swarm character rebuilds while size and speed update live`() {
        val characterUpdate = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm.copy(
                swarm = activeSwarm.swarm.copy(packKey = "owner.shimeji.99@7")
            )
        )
        val sizeUpdate = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm.copy(
                swarm = activeSwarm.swarm.copy(sizePercent = 100)
            )
        )
        val speedUpdate = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm.copy(
                swarm = activeSwarm.swarm.copy(speedPercent = 150)
            )
        )
        val randomizationUpdate = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm.copy(
                swarm = activeSwarm.swarm.copy(randomizeSizeAndSpeed = true)
            )
        )

        assertEquals(PetOverlaySessionUpdate.REBUILD, characterUpdate)
        assertEquals(PetOverlaySessionUpdate.SWARM_RUNTIME, sizeUpdate)
        assertEquals(PetOverlaySessionUpdate.SWARM_RUNTIME, speedUpdate)
        assertEquals(PetOverlaySessionUpdate.SWARM_RUNTIME, randomizationUpdate)
    }

    @Test
    fun `mode change rebuilds while mixed roster change reconciles instances`() {
        val mixed = PetPreferences(displayMode = PetDisplayMode.MIXED, petCount = 2)
        val modeUpdate = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = mixed
        )
        val rosterUpdate = PetOverlaySessionPolicy.resolveUpdate(
            active = mixed.overlaySessionSignature(),
            preferences = mixed.copy(petCount = 3)
        )

        assertEquals(PetOverlaySessionUpdate.REBUILD, modeUpdate)
        assertEquals(PetOverlaySessionUpdate.MIXED_ROSTER, rosterUpdate)
    }

    @Test
    fun `mixed character replacement reconciles only changed roster entries`() {
        val mixed = PetPreferences(
            displayMode = PetDisplayMode.MIXED,
            petCount = 2
        )
        val updatedSlots = mixed.petSlots.toMutableList().apply {
            this[1] = this[1].copy(packKey = "owner.shimeji.77@7")
        }

        val update = PetOverlaySessionPolicy.resolveUpdate(
            active = mixed.overlaySessionSignature(),
            preferences = mixed.copy(petSlots = updatedSlots)
        )

        assertEquals(PetOverlaySessionUpdate.MIXED_ROSTER, update)
    }

    @Test
    fun `mixed roster matcher preserves surviving pets across remove add and replace`() {
        val removeMiddle = PetRosterReconciliationPolicy.retainedIndexes(
            existingPackKeys = listOf("cat", "dog", "fox"),
            requestedPackKeys = listOf("cat", "fox")
        )
        val addPet = PetRosterReconciliationPolicy.retainedIndexes(
            existingPackKeys = listOf("cat", "dog"),
            requestedPackKeys = listOf("cat", "dog", "fox")
        )
        val replacePet = PetRosterReconciliationPolicy.retainedIndexes(
            existingPackKeys = listOf("cat", "dog", "fox"),
            requestedPackKeys = listOf("cat", "bird", "fox")
        )

        assertEquals(listOf(0, 2), removeMiddle)
        assertEquals(listOf(0, 1, null), addPet)
        assertEquals(listOf(0, null, 2), replacePet)
    }

    @Test
    fun `mixed roster matcher consumes duplicate pets in stable order`() {
        val retained = PetRosterReconciliationPolicy.retainedIndexes(
            existingPackKeys = listOf("cat", "cat", "dog"),
            requestedPackKeys = listOf("cat", "dog")
        )

        assertEquals(listOf(0, 2), retained)
    }
}
