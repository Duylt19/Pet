package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PetAnimationTimelineTest {
    @Test
    fun `advance consumes multiple frames and preserves remaining frame time`() {
        val timeline = PetAnimationTimeline(DemoPetAnimation.clips())

        val advance = timeline.advance(
            action = PetAction.IDLE,
            cursor = PetAnimationCursor(),
            elapsedMillis = 450
        )

        assertEquals(PetAction.IDLE, advance.action)
        assertEquals(PetAnimationCursor(frameIndex = 2, elapsedInFrameMillis = 90), advance.cursor)
    }

    @Test
    fun `non-looping action transitions and applies leftover time to next action`() {
        val idle = PetClip(
            action = PetAction.IDLE,
            frames = listOf(
                PetFrame(index = 0, durationMillis = 100, velocity = PetVector(x = 4f)),
                PetFrame(index = 1, durationMillis = 100, velocity = PetVector(x = 4f))
            ),
            loops = true
        )
        val tapped = PetClip(
            action = PetAction.TAPPED,
            frames = listOf(
                PetFrame(index = 0, durationMillis = 100, velocity = PetVector(x = 10f)),
                PetFrame(index = 1, durationMillis = 100, velocity = PetVector(x = 20f))
            ),
            loops = false,
            nextAction = PetAction.IDLE
        )
        val timeline = PetAnimationTimeline(
            mapOf(PetAction.IDLE to idle, PetAction.TAPPED to tapped)
        )

        val advance = timeline.advance(
            action = PetAction.TAPPED,
            cursor = PetAnimationCursor(),
            elapsedMillis = 350
        )

        assertEquals(PetAction.IDLE, advance.action)
        assertEquals(PetAnimationCursor(frameIndex = 1, elapsedInFrameMillis = 50), advance.cursor)
        assertEquals(3.6f, advance.displacement.x, FLOAT_TOLERANCE)
        assertEquals(listOf(PetAction.TAPPED to PetAction.IDLE), advance.actionTransitions)
    }

    @Test
    fun `managed combo transition does not leak leftover movement into fallback action`() {
        val clips = DemoPetAnimation.clips()
        val timeline = PetAnimationTimeline(clips)

        val advance = timeline.advance(
            action = PetAction.SPECIAL,
            cursor = PetAnimationCursor(),
            elapsedMillis = 1_800,
            stopAtActionTransition = true
        )

        assertEquals(PetAction.WALK, advance.action)
        assertEquals(PetAnimationCursor(), advance.cursor)
        assertEquals(0f, advance.displacement.x, FLOAT_TOLERANCE)
        assertEquals(listOf(PetAction.SPECIAL to PetAction.WALK), advance.actionTransitions)
    }

    @Test
    fun `partitioned timeline produces the same cursor and displacement`() {
        val timeline = PetAnimationTimeline(DemoPetAnimation.clips())
        val whole = timeline.advance(
            action = PetAction.WALK,
            cursor = PetAnimationCursor(),
            elapsedMillis = 470
        )
        var cursor = PetAnimationCursor()
        var displacement = PetVector.Zero
        repeat(47) {
            val step = timeline.advance(PetAction.WALK, cursor, elapsedMillis = 10)
            cursor = step.cursor
            displacement += step.displacement
        }

        assertEquals(whole.cursor, cursor)
        assertEquals(whole.displacement.x, displacement.x, FLOAT_TOLERANCE)
        assertEquals(whole.displacement.y, displacement.y, FLOAT_TOLERANCE)
    }

    @Test
    fun `stationary talk holds one frame while walking talk animates and moves`() {
        val clips = DemoPetAnimation.clips()
        val stillTalk = clips.getValue(PetAction.TALK)
        val walkingTalk = clips.getValue(PetAction.TALK_WALK)
        val timeline = PetAnimationTimeline(clips)

        val stillAdvance = timeline.advance(
            action = PetAction.TALK,
            cursor = PetAnimationCursor(),
            elapsedMillis = 1_000
        )
        val walkingAdvance = timeline.advance(
            action = PetAction.TALK_WALK,
            cursor = PetAnimationCursor(),
            elapsedMillis = 1_000
        )

        assertEquals(1, stillTalk.frames.size)
        assertEquals(4, walkingTalk.frames.size)
        assertEquals(PetAnimationCursor(frameIndex = 0, elapsedInFrameMillis = 40), stillAdvance.cursor)
        assertEquals(0f, stillAdvance.displacement.x, FLOAT_TOLERANCE)
        assertEquals(24f, walkingAdvance.displacement.x, FLOAT_TOLERANCE)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
