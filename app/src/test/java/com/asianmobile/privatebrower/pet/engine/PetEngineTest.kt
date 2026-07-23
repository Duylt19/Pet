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
    fun `tap uses pack configured interaction action`() {
        val engine = PetEngine(PetEngineConfig(tapAction = PetAction.WALK))
        val initial = engine.initialState(bounds, size)

        val tapped = engine.reduce(initial, PetEvent.Tap)

        assertEquals(PetAction.WALK, tapped.state.action)
        assertTrue(tapped.effects.contains(PetEffect.Tapped))
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
        assertEquals(PetAction.FALL, settled.state.action)
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
    fun `release below fling threshold starts falling`() {
        val engine = engine(flingStopSpeed = 24f)
        val dragged = engine.reduce(
            engine.initialState(bounds, size, position = PetVector(10f, 10f)),
            PetEvent.DragStart
        ).state

        val released = engine.reduce(dragged, PetEvent.Fling(PetVector(x = 10f)))

        assertEquals(PetAction.FALL, released.state.action)
        assertEquals(PetVector.Zero, released.state.velocity)
    }

    @Test
    fun `walking pet keeps edge direction when entering wall climb`() {
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
        assertEquals(PetDirection.RIGHT, advanced.state.direction)
        assertEquals(PetAction.CLIMB_WALL, advanced.state.action)
    }

    @Test
    fun `wall climber turns inward when reaching the ceiling`() {
        val engine = engine(maxTickMillis = 1_000)
        val climbingRightWall = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(80f, 1f),
            action = PetAction.CLIMB_WALL,
            direction = PetDirection.RIGHT
        )

        val atCeiling = engine.reduce(climbingRightWall, PetEvent.Tick(elapsedMillis = 100))

        assertEquals(PetVector(80f, 0f), atCeiling.state.position)
        assertEquals(PetAction.CLIMB_CEILING, atCeiling.state.action)
        assertEquals(PetDirection.LEFT, atCeiling.state.direction)
    }

    @Test
    fun `falling pet bounces after reaching the bottom edge`() {
        val engine = engine(maxTickMillis = 1_000)
        val falling = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(20f, 75f),
            action = PetAction.FALL
        )

        val landed = engine.reduce(falling, PetEvent.Tick(elapsedMillis = 100))

        assertEquals(PetVector(20f, 80f), landed.state.position)
        assertEquals(PetAction.BOUNCE, landed.state.action)
        assertTrue(
            landed.effects.contains(PetEffect.ActionChanged(PetAction.FALL, PetAction.BOUNCE))
        )
    }

    @Test
    fun `walking selects a supported weighted behavior after its scheduled delay`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 3_000,
                behaviorProfile = behaviorProfile(
                    groundDelayMillis = 100L..100L,
                    autonomousRules = listOf(PetBehaviorRule(PetAction.SIT, 1))
                )
            )
        )
        val walking = engine.initialState(
            bounds = PetBounds(0f, 0f, 1_000f, 1_000f),
            size = size,
            action = PetAction.WALK
        )

        val selected = engine.reduce(walking, PetEvent.Tick(100))

        assertEquals(PetAction.SIT, selected.state.action)
        assertTrue(
            selected.effects.contains(PetEffect.ActionChanged(PetAction.WALK, PetAction.SIT))
        )
    }

    @Test
    fun `same behavior seed produces the same schedule`() {
        val config = PetEngineConfig(
            behaviorSeed = 42,
            behaviorProfile = behaviorProfile(
                groundDelayMillis = 100L..10_000L,
                autonomousRules = listOf(PetBehaviorRule(PetAction.SIT, 1))
            )
        )
        val firstEngine = PetEngine(config)
        val secondEngine = PetEngine(config)
        val firstInitial = firstEngine.initialState(bounds, size, action = PetAction.WALK)
        val secondInitial = secondEngine.initialState(bounds, size, action = PetAction.WALK)

        val first = firstEngine.reduce(firstInitial, PetEvent.Tick(50)).state
        val second = secondEngine.reduce(secondInitial, PetEvent.Tick(50)).state

        assertEquals(first.actionTargetMillis, second.actionTargetMillis)
        assertEquals(first.behaviorSequence, second.behaviorSequence)
    }

    @Test
    fun `recent behavior memory prevents immediate action repetition`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 3_000,
                behaviorSeed = 9,
                behaviorProfile = behaviorProfile(
                    groundDelayMillis = 100L..100L,
                    recentActionMemory = 1,
                    autonomousRules = listOf(
                        PetBehaviorRule(PetAction.SIT, 1),
                        PetBehaviorRule(PetAction.WINK, 1)
                    )
                )
            )
        )
        var state = engine.initialState(
            PetBounds(0f, 0f, 1_000f, 1_000f),
            size,
            action = PetAction.WALK
        )
        state = engine.reduce(state, PetEvent.Tick(100)).state
        val firstAction = state.action
        val firstDuration = if (firstAction == PetAction.SIT) 2_400L else 520L
        state = engine.reduce(state, PetEvent.Tick(firstDuration)).state
        state = engine.reduce(state, PetEvent.Tick(100)).state

        assertTrue(firstAction == PetAction.SIT || firstAction == PetAction.WINK)
        assertTrue(state.action == PetAction.SIT || state.action == PetAction.WINK)
        assertTrue(firstAction != state.action)
    }

    @Test
    fun `idle resumes walking after its seeded duration`() {
        val engine = PetEngine(
            PetEngineConfig(
                behaviorProfile = behaviorProfile(idleDurationMillis = 500L..500L)
            )
        )
        val idle = engine.initialState(bounds, size, action = PetAction.IDLE)

        val waiting = engine.reduce(idle, PetEvent.Tick(250)).state
        val walking = engine.reduce(waiting, PetEvent.Tick(250))

        assertEquals(PetAction.IDLE, waiting.action)
        assertEquals(PetAction.WALK, walking.state.action)
        assertTrue(
            walking.effects.contains(PetEffect.ActionChanged(PetAction.IDLE, PetAction.WALK))
        )
    }

    @Test
    fun `creep returns to walking after its behavior timeout`() {
        val engine = PetEngine(
            PetEngineConfig(
                behaviorProfile = behaviorProfile(creepDurationMillis = 100L..100L)
            )
        )
        val creeping = engine.initialState(
            PetBounds(0f, 0f, 1_000f, 1_000f),
            size,
            action = PetAction.CREEP
        )

        val timedOut = engine.reduce(creeping, PetEvent.Tick(100))

        assertEquals(PetAction.WALK, timedOut.state.action)
    }

    @Test
    fun `wall climber can jump inward after its behavior timeout`() {
        val engine = PetEngine(
            PetEngineConfig(
                behaviorProfile = behaviorProfile(
                    wallDurationMillis = 100L..100L,
                    wallJumpChancePercent = 100
                )
            )
        )
        val climbing = engine.initialState(
            PetBounds(0f, 0f, 1_000f, 1_000f),
            size,
            position = PetVector(980f, 500f),
            action = PetAction.CLIMB_WALL,
            direction = PetDirection.RIGHT
        )

        val jumped = engine.reduce(climbing, PetEvent.Tick(100))

        assertEquals(PetAction.JUMP, jumped.state.action)
        assertEquals(PetDirection.LEFT, jumped.state.direction)
    }

    @Test
    fun `ceiling climber drops after its behavior timeout`() {
        val engine = PetEngine(
            PetEngineConfig(
                behaviorProfile = behaviorProfile(ceilingDurationMillis = 100L..100L)
            )
        )
        val climbing = engine.initialState(
            PetBounds(0f, 0f, 1_000f, 1_000f),
            size,
            position = PetVector(500f, 0f),
            action = PetAction.CLIMB_CEILING
        )

        val dropped = engine.reduce(climbing, PetEvent.Tick(100))

        assertEquals(PetAction.FALL, dropped.state.action)
    }

    @Test
    fun `fall accelerates until collision instead of using constant speed`() {
        val engine = engine(maxTickMillis = 1_000)
        val falling = engine.initialState(
            PetBounds(0f, 0f, 1_000f, 10_000f),
            size,
            position = PetVector(10f, 10f),
            action = PetAction.FALL
        )

        val advanced = engine.reduce(falling, PetEvent.Tick(100)).state

        assertEquals(210f, advanced.velocity.y, FLOAT_TOLERANCE)
        assertEquals(26.5f, advanced.position.y, FLOAT_TOLERANCE)
    }

    @Test
    fun `legacy pack without extended actions keeps safe walk and idle behavior`() {
        val legacyActions = setOf(
            PetAction.IDLE,
            PetAction.WALK,
            PetAction.TAPPED,
            PetAction.DRAGGED,
            PetAction.FLUNG
        )
        val engine = PetEngine(
            PetEngineConfig(
                supportedActions = legacyActions,
                behaviorProfile = behaviorProfile(groundDelayMillis = 100L..100L)
            )
        )
        val walking = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(75f, 10f),
            action = PetAction.WALK
        )

        val atEdge = engine.reduce(walking, PetEvent.Tick(200)).state
        val dragged = engine.reduce(atEdge, PetEvent.DragStart).state
        val released = engine.reduce(dragged, PetEvent.DragEnd).state

        assertEquals(PetAction.WALK, atEdge.action)
        assertEquals(PetDirection.LEFT, atEdge.direction)
        assertEquals(PetAction.IDLE, released.action)
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

    private fun behaviorProfile(
        groundDelayMillis: LongRange = 100L..100L,
        idleDurationMillis: LongRange = 100L..100L,
        creepDurationMillis: LongRange = 100L..100L,
        wallDurationMillis: LongRange = 100L..100L,
        ceilingDurationMillis: LongRange = 100L..100L,
        wallJumpChancePercent: Int = 70,
        recentActionMemory: Int = 2,
        autonomousRules: List<PetBehaviorRule> = emptyList()
    ) = PetBehaviorProfile(
        groundDelayMillis = groundDelayMillis,
        idleDurationMillis = idleDurationMillis,
        creepDurationMillis = creepDurationMillis,
        wallDurationMillis = wallDurationMillis,
        ceilingDurationMillis = ceilingDurationMillis,
        continueWalkWeight = if (autonomousRules.isEmpty()) 1 else 0,
        turnAroundWeight = 0,
        wallJumpChancePercent = wallJumpChancePercent,
        recentActionMemory = recentActionMemory,
        autonomousRules = autonomousRules
    )

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
