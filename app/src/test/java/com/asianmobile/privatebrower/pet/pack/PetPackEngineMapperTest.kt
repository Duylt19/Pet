package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetVector
import org.junit.Assert.assertEquals
import org.junit.Test

class PetPackEngineMapperTest {
    @Test
    fun `speed multiplier adjusts duration and scripted velocity once at session start`() {
        val clips = manifest().toEngineClips(speedMultiplier = 1.5f)
        val walkFrame = clips.getValue(PetAction.WALK).frames.single()

        assertEquals(80L, walkFrame.durationMillis)
        assertEquals(PetVector(60f, 15f), walkFrame.velocity)
    }

    @Test
    fun `speed multiplier is clamped to supported product range`() {
        val slow = manifest().toEngineClips(speedMultiplier = 0.01f)
            .getValue(PetAction.WALK).frames.single()
        val fast = manifest().toEngineClips(speedMultiplier = 9f)
            .getValue(PetAction.WALK).frames.single()

        assertEquals(240L, slow.durationMillis)
        assertEquals(80L, fast.durationMillis)
    }

    private fun manifest(): PetPackManifest {
        fun clip(action: PetAction) = PetPackClip(
            action = action,
            loops = true,
            nextAction = null,
            frames = listOf(
                PetPackFrame(
                    file = "pet.png",
                    rect = PetPackFrameRect(0, 0, 16, 16),
                    durationMillis = 120L,
                    velocity = PetVector(40f, 10f)
                )
            )
        )
        return PetPackManifest(
            schemaVersion = 1,
            id = "test.pet",
            version = 1,
            name = "Test",
            author = null,
            canvas = PetPackCanvas(16, 16, 1f),
            anchor = PetPackAnchor(0.5f, 1f),
            interaction = PetPackInteraction(PetAction.TAPPED),
            clips = mapOf(
                PetAction.IDLE to clip(PetAction.IDLE),
                PetAction.WALK to clip(PetAction.WALK)
            )
        )
    }
}
