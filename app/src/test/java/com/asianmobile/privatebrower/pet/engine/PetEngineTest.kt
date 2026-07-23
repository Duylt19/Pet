package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetEngineTest {
    private val bounds = PetBounds(left = 0f, top = 0f, right = 100f, bottom = 100f)
    private val size = PetSize(width = 20f, height = 20f)

    @Test
    fun `tap restarts tapped animation then follows with an affectionate reaction`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(bounds, size, position = PetVector(10f, 10f))

        val tapped = engine.reduce(initial, PetEvent.Tap)
        val completed = engine.reduce(tapped.state, PetEvent.Tick(elapsedMillis = 300))

        assertEquals(PetAction.TAPPED, tapped.state.action)
        assertTrue(tapped.effects.contains(PetEffect.Tapped))
        assertEquals(PetAction.WINK, completed.state.action)
        assertTrue(
            completed.effects.contains(
                PetEffect.ActionChanged(PetAction.TAPPED, PetAction.WINK)
            )
        )
    }

    @Test
    fun `showcase plays every supported special as one routine`() {
        val engine = engine(maxTickMillis = 2_000)
        val initial = engine.initialState(bounds, size)

        val started = engine.reduce(initial, PetEvent.Showcase)
        val secondSpecial = engine.reduce(started.state, PetEvent.Tick(880))
        val wink = engine.reduce(secondSpecial.state, PetEvent.Tick(1_280))
        val lookUp = engine.reduce(wink.state, PetEvent.Tick(520))

        assertEquals(PetAction.SPECIAL, started.state.action)
        assertEquals(PetAction.SPECIAL_2, secondSpecial.state.action)
        assertEquals(PetAction.WINK, wink.state.action)
        assertEquals(PetAction.LOOK_UP, lookUp.state.action)
        assertTrue(started.effects.contains(PetEffect.ShowcaseStarted))
    }

    @Test
    fun `run is a short faster ground action`() {
        val engine = engine(maxTickMillis = 1_000)
        val largeBounds = PetBounds(0f, 0f, 10_000f, 10_000f)
        val running = engine.initialState(largeBounds, size, action = PetAction.RUN)
        val walking = engine.initialState(largeBounds, size, action = PetAction.WALK)

        val advancedRun = engine.reduce(running, PetEvent.Tick(100)).state
        val advancedWalk = engine.reduce(walking, PetEvent.Tick(100)).state

        assertTrue(advancedRun.position.x > advancedWalk.position.x)
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
    fun `walking selects a supported combo after its scheduled delay`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 3_000,
                behaviorProfile = behaviorProfile(
                    groundDelayMillis = 100L..100L,
                    autonomousComboRules = listOf(
                        PetComboRule(PetComboId.TINY_PERFORMANCE, 1)
                    )
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
        assertEquals(PetComboId.TINY_PERFORMANCE, selected.state.activeComboId)
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
                autonomousComboRules = listOf(
                    PetComboRule(PetComboId.TINY_PERFORMANCE, 1)
                )
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
    fun `recent combo memory prevents immediate story repetition`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 3_000,
                behaviorSeed = 9,
                behaviorProfile = behaviorProfile(
                    groundDelayMillis = 100L..100L,
                    recentComboMemory = 1,
                    autonomousComboRules = listOf(
                        PetComboRule(PetComboId.TINY_PERFORMANCE, 1),
                        PetComboRule(PetComboId.DAYDREAM, 1)
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
        val firstCombo = state.activeComboId
        state = state.copy(
            action = PetAction.WALK,
            activeComboId = null,
            pendingRoutineActions = emptyList(),
            actionElapsedMillis = 0,
            actionTargetMillis = 0
        )
        state = engine.reduce(state, PetEvent.Tick(100)).state

        assertTrue(
            firstCombo == PetComboId.TINY_PERFORMANCE || firstCombo == PetComboId.DAYDREAM
        )
        assertTrue(
            state.activeComboId == PetComboId.TINY_PERFORMANCE ||
                state.activeComboId == PetComboId.DAYDREAM
        )
        assertTrue(firstCombo != state.activeComboId)
    }

    @Test
    fun `combo advances looping actions before choosing another combo`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 1_000,
                behaviorProfile = behaviorProfile(
                    runDurationMillis = 100L..100L,
                    groundDelayMillis = 100L..100L
                )
            )
        )
        val initial = engine.initialState(
            PetBounds(0f, 0f, 10_000f, 1_000f),
            size,
            action = PetAction.WALK
        )

        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.HAPPY_ZOOMIES)
        )
        val run = engine.reduce(started.state, PetEvent.Tick(520))
        val idle = engine.reduce(run.state, PetEvent.Tick(100))

        assertEquals(PetComboId.HAPPY_ZOOMIES, started.state.activeComboId)
        assertEquals(PetAction.RUN, run.state.action)
        assertEquals(PetAction.IDLE, idle.state.action)
        assertEquals(PetComboId.HAPPY_ZOOMIES, idle.state.activeComboId)
    }

    @Test
    fun `external social combo faces peer and completes as one sequence`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 2_000,
                behaviorProfile = behaviorProfile(idleDurationMillis = 100L..100L)
            )
        )
        val initial = engine.initialState(bounds, size, direction = PetDirection.RIGHT)

        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.SOCIAL_HELLO, PetDirection.LEFT)
        )
        val wink = engine.reduce(started.state, PetEvent.Tick(100))
        val lookUp = engine.reduce(wink.state, PetEvent.Tick(520))
        val finalWink = engine.reduce(lookUp.state, PetEvent.Tick(1_200))
        val completed = engine.reduce(finalWink.state, PetEvent.Tick(520))

        assertEquals(PetDirection.LEFT, started.state.direction)
        assertEquals(PetAction.IDLE, started.state.action)
        assertEquals(PetAction.WINK, wink.state.action)
        assertEquals(PetAction.LOOK_UP, lookUp.state.action)
        assertEquals(PetAction.WINK, finalWink.state.action)
        assertEquals(null, completed.state.activeComboId)
        assertTrue(
            completed.effects.contains(PetEffect.ComboCompleted(PetComboId.SOCIAL_HELLO))
        )
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
    fun `wall climber can reverse into a controlled descent`() {
        val engine = PetEngine(
            PetEngineConfig(
                behaviorProfile = behaviorProfile(
                    wallDurationMillis = 100L..100L,
                    wallJumpChancePercent = 0,
                    wallDescendChancePercent = 100
                )
            )
        )
        val climbing = engine.initialState(
            PetBounds(0f, 0f, 1_000f, 1_000f),
            size,
            position = PetVector(980f, 500f),
            action = PetAction.CLIMB_WALL
        )

        val descending = engine.reduce(climbing, PetEvent.Tick(100))

        assertEquals(PetAction.CLIMB_DOWN, descending.state.action)
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
        runDurationMillis: LongRange = 100L..100L,
        creepDurationMillis: LongRange = 100L..100L,
        wallDurationMillis: LongRange = 100L..100L,
        ceilingDurationMillis: LongRange = 100L..100L,
        wallJumpChancePercent: Int = 70,
        wallDescendChancePercent: Int = 0,
        recentComboMemory: Int = 2,
        autonomousComboRules: List<PetComboRule> = listOf(
            PetComboRule(PetComboId.CURIOUS_SCOUT, 1)
        )
    ) = PetBehaviorProfile(
        groundDelayMillis = groundDelayMillis,
        idleDurationMillis = idleDurationMillis,
        runDurationMillis = runDurationMillis,
        creepDurationMillis = creepDurationMillis,
        wallDurationMillis = wallDurationMillis,
        ceilingDurationMillis = ceilingDurationMillis,
        wallJumpChancePercent = wallJumpChancePercent,
        wallDescendChancePercent = wallDescendChancePercent,
        recentComboMemory = recentComboMemory,
        autonomousComboRules = autonomousComboRules
    )

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
