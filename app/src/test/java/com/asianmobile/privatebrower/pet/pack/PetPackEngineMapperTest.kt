package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `legacy manifest receives safe runtime fall and climb fallback clips`() {
        val clips = manifest().toEngineClips()

        assertEquals(PetVector(0f, 220f), clips.getValue(PetAction.FALL).frames.first().velocity)
        assertEquals(PetVector(0f, -36f), clips.getValue(PetAction.CLIMB_WALL).frames.first().velocity)
        assertEquals(PetVector(36f, 0f), clips.getValue(PetAction.CLIMB_CEILING).frames.first().velocity)
        assertEquals(PetAction.WALK, clips.getValue(PetAction.SIT).nextAction)
    }

    @Test
    fun `legacy multi frame talk is split into still and moving runtime clips`() {
        val base = manifest()
        val legacyTalk = PetPackClip(
            action = PetAction.TALK,
            loops = true,
            nextAction = null,
            frames = List(4) { index ->
                PetPackFrame(
                    file = "talk-$index.png",
                    rect = PetPackFrameRect(0, 0, 16, 16),
                    durationMillis = 240L,
                    velocity = PetVector.Zero
                )
            }
        )
        val legacy = base.copy(clips = base.clips + (PetAction.TALK to legacyTalk))
        val clips = legacy.toEngineClips()

        assertEquals(1, clips.getValue(PetAction.TALK).frames.size)
        assertEquals(4, clips.getValue(PetAction.TALK_WALK).frames.size)
        assertTrue(clips.getValue(PetAction.TALK).frames.all { it.velocity == PetVector.Zero })
        assertTrue(
            clips.getValue(PetAction.TALK_WALK).frames.all {
                it.velocity == PetVector(x = 24f)
            }
        )
        assertTrue(PetAction.TALK_WALK in legacy.toEngineSupportedActions())
    }

    @Test
    fun `single frame talk does not pretend the pack supports moving speech`() {
        val base = manifest()
        val stillTalk = PetPackClip(
            action = PetAction.TALK,
            loops = true,
            nextAction = null,
            frames = listOf(
                PetPackFrame(
                    file = "talk.png",
                    rect = PetPackFrameRect(0, 0, 16, 16),
                    durationMillis = 240L,
                    velocity = PetVector.Zero
                )
            )
        )
        val stillOnly = base.copy(clips = base.clips + (PetAction.TALK to stillTalk))

        assertTrue(PetAction.TALK in stillOnly.toEngineSupportedActions())
        assertFalse(PetAction.TALK_WALK in stillOnly.toEngineSupportedActions())
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
