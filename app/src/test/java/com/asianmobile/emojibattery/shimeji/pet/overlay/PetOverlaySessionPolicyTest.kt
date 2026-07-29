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
    fun `swarm count change updates instances without rebuilding session`() {
        val update = PetOverlaySessionPolicy.resolveUpdate(
            active = activeSwarm.overlaySessionSignature(),
            preferences = activeSwarm.copy(
                swarm = activeSwarm.swarm.copy(count = 9)
            )
        )

        assertEquals(PetOverlaySessionUpdate.SWARM_COUNT, update)
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
    fun `swarm character or runtime profile change still rebuilds safely`() {
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

        assertEquals(PetOverlaySessionUpdate.REBUILD, characterUpdate)
        assertEquals(PetOverlaySessionUpdate.REBUILD, sizeUpdate)
    }

    @Test
    fun `mode or mixed roster change requires rebuild`() {
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
        assertEquals(PetOverlaySessionUpdate.REBUILD, rosterUpdate)
    }
}
