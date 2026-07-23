package com.asianmobile.privatebrower.pet.pack

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetFrame
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
    fun `expressive timing resists global speed while motion still scales fully`() {
        val base = manifest()
        val wink = PetPackClip(
            action = PetAction.WINK,
            loops = false,
            nextAction = PetAction.WALK,
            frames = listOf(frame("wink.png", durationMillis = 120L))
        )
        val fast = base.copy(clips = base.clips + (PetAction.WINK to wink))
            .toEngineClips(speedMultiplier = 1.5f)

        assertEquals(106L, fast.getValue(PetAction.WINK).frames.single().durationMillis)
        assertEquals(
            PetVector(60f, 15f),
            fast.getValue(PetAction.WINK).frames.single().velocity
        )
        assertEquals(80L, fast.getValue(PetAction.WALK).frames.single().durationMillis)
    }

    @Test
    fun `owner shimeji clips use calm idle emotion and one way skill timing`() {
        val base = manifest()
        val owner = base.copy(
            id = "owner.shimeji.4",
            clips = base.clips + mapOf(
                PetAction.IDLE to clip(
                    PetAction.IDLE,
                    listOf("idle.png", "emotion-a.png", "idle.png", "emotion-b.png"),
                    loops = true
                ),
                PetAction.WINK to clip(
                    PetAction.WINK,
                    listOf("emotion-a.png", "emotion-b.png")
                ),
                PetAction.DANGLE to clip(
                    PetAction.DANGLE,
                    listOf("floor-a.png", "floor-b.png")
                ),
                PetAction.CREEP to clip(
                    PetAction.CREEP,
                    listOf("creep-a.png", "sprawl.png"),
                    loops = true
                ),
                PetAction.CLIMB_WALL to clip(
                    PetAction.CLIMB_WALL,
                    listOf("wall-a.png", "wall-b.png", "wall-c.png", "wall-grip.png"),
                    loops = true
                ),
                PetAction.CLIMB_CEILING to clip(
                    PetAction.CLIMB_CEILING,
                    listOf("ceiling-a.png", "ceiling-b.png", "ceiling-grip.png"),
                    loops = true
                ),
                PetAction.SPECIAL to clip(
                    PetAction.SPECIAL,
                    listOf("s1.png", "s2.png", "s3.png", "s4.png", "s5.png")
                ),
                PetAction.SPECIAL_2 to clip(
                    PetAction.SPECIAL_2,
                    listOf(
                        "x1.png",
                        "x2.png",
                        "x3.png",
                        "x4.png",
                        "x5.png",
                        "x4.png",
                        "x3.png",
                        "x2.png"
                    )
                )
            )
        )
        val fast = owner.toEngineClips(speedMultiplier = 1.5f)

        assertEquals(1, fast.getValue(PetAction.IDLE).frames.size)
        assertEquals(800L, fast.getValue(PetAction.IDLE).frames.single().durationMillis)
        assertEquals(
            listOf(311L, 488L),
            fast.getValue(PetAction.WINK).frames.map(PetFrame::durationMillis)
        )
        assertEquals(5, fast.getValue(PetAction.SPECIAL).frames.size)
        assertEquals(
            listOf(373L, 426L, 497L, 604L, 764L),
            fast.getValue(PetAction.SPECIAL).frames.map(PetFrame::durationMillis)
        )
        assertEquals(5, fast.getValue(PetAction.SPECIAL_2).frames.size)
        assertEquals(
            listOf(373L, 426L, 497L, 604L, 764L),
            fast.getValue(PetAction.SPECIAL_2).frames.map(PetFrame::durationMillis)
        )
        assertEquals(PetVector.Zero, fast.getValue(PetAction.SPRAWL).frames.single().velocity)
        assertEquals(PetVector.Zero, fast.getValue(PetAction.HOLD_WALL).frames.single().velocity)
        assertEquals(
            PetVector.Zero,
            fast.getValue(PetAction.HOLD_CEILING).frames.single().velocity
        )
        assertTrue(
            owner.toEngineSupportedActions().containsAll(
                setOf(
                    PetAction.EMOTE,
                    PetAction.FLOOR_PLAY,
                    PetAction.SPRAWL,
                    PetAction.HOLD_WALL,
                    PetAction.HOLD_CEILING
                )
            )
        )
    }

    @Test
    fun `owner shimeji idle renders a standing frame without moving`() {
        val frames = mapOf(
            PetAction.IDLE to listOf("sit", "wink"),
            PetAction.WALK to listOf("stand", "step"),
            PetAction.WINK to listOf("emotion-a", "emotion-b"),
            PetAction.DANGLE to listOf("floor-a", "floor-b"),
            PetAction.CREEP to listOf("creep", "sprawl"),
            PetAction.CLIMB_WALL to listOf("wall-a", "wall-b", "wall-c", "wall-grip"),
            PetAction.CLIMB_CEILING to listOf("ceiling-a", "ceiling-b", "ceiling-grip")
        )

        val owner = frames.normalizedRuntimeVisualFrames("owner.shimeji.4")
        val external = frames.normalizedRuntimeVisualFrames("sample.external")

        assertEquals(listOf("stand"), owner.getValue(PetAction.IDLE))
        assertEquals(listOf("stand", "step"), owner.getValue(PetAction.WALK))
        assertEquals(listOf("emotion-a", "emotion-b"), owner.getValue(PetAction.EMOTE))
        assertEquals(listOf("floor-a", "floor-b"), owner.getValue(PetAction.FLOOR_PLAY))
        assertEquals(listOf("sprawl"), owner.getValue(PetAction.SPRAWL))
        assertEquals(listOf("wall-grip"), owner.getValue(PetAction.HOLD_WALL))
        assertEquals(listOf("ceiling-grip"), owner.getValue(PetAction.HOLD_CEILING))
        assertEquals(listOf("sit", "wink"), external.getValue(PetAction.IDLE))
        assertFalse(PetAction.EMOTE in external)
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

    private fun clip(
        action: PetAction,
        files: List<String>,
        loops: Boolean = false
    ) = PetPackClip(
        action = action,
        loops = loops,
        nextAction = if (loops) null else PetAction.WALK,
        frames = files.map { file -> frame(file) }
    )

    private fun frame(
        file: String,
        durationMillis: Long = 180L
    ) = PetPackFrame(
        file = file,
        rect = PetPackFrameRect(0, 0, 16, 16),
        durationMillis = durationMillis,
        velocity = PetVector(40f, 10f)
    )
}
