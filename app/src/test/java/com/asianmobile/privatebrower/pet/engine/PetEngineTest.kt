package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetEngineTest {
    private val bounds = PetBounds(left = 0f, top = 0f, right = 100f, bottom = 100f)
    private val size = PetSize(width = 20f, height = 20f)

    @Test
    fun `tap restarts tapped animation and returns to idle after clip completes`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(bounds, size, position = PetVector(10f, 10f))

        val tapped = engine.reduce(initial, PetEvent.Tap)
        val completed = engine.reduce(tapped.state, PetEvent.Tick(elapsedMillis = 300))

        assertEquals(PetAction.TAPPED, tapped.state.action)
        assertTrue(tapped.effects.contains(PetEffect.Tapped))
        assertEquals(PetAction.IDLE, completed.state.action)
        assertTrue(
            completed.effects.contains(
                PetEffect.ActionChanged(PetAction.TAPPED, PetAction.IDLE)
            )
        )
    }

    @Test
    fun `drag only moves during dragged action and remains inside usable bounds`() {
        val engine = engine()
        val initial = engine.initialState(bounds, size, position = PetVector(50f, 50f))
        val ignored = engine.reduce(initial, PetEvent.DragBy(PetVector(20f, 20f)))
        val dragging = engine.reduce(initial, PetEvent.DragStart)
        val movedRight = engine.reduce(dragging.state, PetEvent.DragBy(PetVector(100f, 100f)))
        val movedLeft = engine.reduce(movedRight.state, PetEvent.DragBy(PetVector(-200f, 0f)))

        assertEquals(initial, ignored.state)
        assertEquals(PetVector(80f, 80f), movedRight.state.position)
        assertEquals(PetDirection.RIGHT, movedRight.state.direction)
        assertEquals(PetVector(0f, 80f), movedLeft.state.position)
        assertEquals(PetDirection.LEFT, movedLeft.state.direction)
    }

    @Test
    fun `fling uses constant deceleration and settles back to idle`() {
        val engine = engine(
            maxTickMillis = 1_000,
            flingDeceleration = 100f,
            flingStopSpeed = 1f
        )
        val initial = engine.initialState(bounds, size, position = PetVector(10f, 10f))
        val flung = engine.reduce(initial, PetEvent.Fling(PetVector(x = 100f)))

        val settled = engine.reduce(flung.state, PetEvent.Tick(elapsedMillis = 1_000))

        assertEquals(PetVector(60f, 10f), settled.state.position)
        assertEquals(PetVector.Zero, settled.state.velocity)
        assertEquals(PetAction.IDLE, settled.state.action)
    }

    @Test
    fun `fling integration is stable across tick partitioning`() {
        val largeBounds = PetBounds(0f, 0f, 10_000f, 10_000f)
        val engine = engine(
            maxTickMillis = 1_000,
            flingDeceleration = 100f,
            flingStopSpeed = 1f
        )
        val initial = engine.initialState(largeBounds, size, PetVector(100f, 100f))
        val flung = engine.reduce(initial, PetEvent.Fling(PetVector(x = 500f))).state
        val singleTick = engine.reduce(flung, PetEvent.Tick(elapsedMillis = 400)).state
        var partitioned = flung
        repeat(4) {
            partitioned = engine.reduce(partitioned, PetEvent.Tick(elapsedMillis = 100)).state
        }

        assertEquals(singleTick.position.x, partitioned.position.x, FLOAT_TOLERANCE)
        assertEquals(singleTick.velocity.x, partitioned.velocity.x, FLOAT_TOLERANCE)
    }

    @Test
    fun `walking pet turns around when reaching a horizontal edge`() {
        val engine = engine(maxTickMillis = 1_000)
        val walking = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(75f, 10f),
            action = PetAction.WALK,
            direction = PetDirection.RIGHT
        )

        val advanced = engine.reduce(walking, PetEvent.Tick(elapsedMillis = 200))

        assertEquals(80f, advanced.state.position.x, FLOAT_TOLERANCE)
        assertEquals(PetDirection.LEFT, advanced.state.direction)
    }

    @Test
    fun `bounds change immediately constrains existing position`() {
        val engine = engine()
        val initial = engine.initialState(bounds, size, position = PetVector(70f, 70f))
        val smallerBounds = PetBounds(left = 10f, top = 10f, right = 60f, bottom = 50f)

        val updated = engine.reduce(initial, PetEvent.BoundsChanged(smallerBounds))

        assertEquals(PetVector(40f, 30f), updated.state.position)
        assertEquals(smallerBounds, updated.state.bounds)
    }

    @Test
    fun `large delayed tick is capped to prevent animation catch-up storms`() {
        val engine = engine(maxTickMillis = 100)
        val walking = engine.initialState(
            bounds = PetBounds(0f, 0f, 1_000f, 1_000f),
            size = size,
            action = PetAction.WALK
        )

        val updated = engine.reduce(walking, PetEvent.Tick(elapsedMillis = 10_000))

        assertEquals(4.2f, updated.state.position.x, FLOAT_TOLERANCE)
        assertEquals(100L, updated.state.animationCursor.elapsedInFrameMillis)
    }

    private fun engine(
        maxTickMillis: Long = 250,
        flingDeceleration: Float = 3_500f,
        flingStopSpeed: Float = 24f
    ) = PetEngine(
        PetEngineConfig(
            maxTickMillis = maxTickMillis,
            maxFlingSpeed = 2_500f,
            flingDeceleration = flingDeceleration,
            flingStopSpeed = flingStopSpeed
        )
    )

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
