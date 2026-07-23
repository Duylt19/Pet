package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetEngineTest {
    private val bounds = PetBounds(left = 0f, top = 0f, right = 100f, bottom = 100f)
    private val size = PetSize(width = 20f, height = 20f)

    @Test
    fun `tap reaction includes a calm recovery before its affectionate wink`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(bounds, size, position = PetVector(10f, 80f))

        val tapped = engine.reduce(initial, PetEvent.Tap)
        val recovering = engine.reduce(tapped.state, PetEvent.Tick(elapsedMillis = 300))
        val talking = advanceUntil(engine, recovering.state) { it.action == PetAction.TALK }
        val wink = advanceUntil(engine, talking) { it.action == PetAction.WINK }

        assertEquals(PetAction.TAPPED, tapped.state.action)
        assertTrue(tapped.effects.contains(PetEffect.Tapped))
        assertEquals(PetAction.IDLE, recovering.state.action)
        assertEquals(PetAction.TALK, talking.action)
        assertEquals(PetAction.WINK, wink.action)
        assertTrue(
            recovering.effects.contains(
                PetEffect.ActionChanged(PetAction.TAPPED, PetAction.IDLE)
            )
        )
    }

    @Test
    fun `solo talk faces into viewport so carried box remains aligned and readable`() {
        val engine = engine(maxTickMillis = 1_000)
        val leftPet = engine.initialState(
            bounds,
            size,
            position = PetVector(0f, 80f),
            direction = PetDirection.LEFT
        )
        val rightPet = engine.initialState(
            bounds,
            size,
            position = PetVector(80f, 80f),
            direction = PetDirection.RIGHT
        )

        val leftTalking = advanceUntil(engine, engine.reduce(leftPet, PetEvent.Tap).state) {
            it.action == PetAction.TALK
        }
        val rightTalking = advanceUntil(engine, engine.reduce(rightPet, PetEvent.Tap).state) {
            it.action == PetAction.TALK
        }

        assertEquals(PetDirection.RIGHT, leftTalking.direction)
        assertEquals(PetDirection.LEFT, rightTalking.direction)
    }

    @Test
    fun `stationary talk holds position while walking talk moves and turns at edge`() {
        val engine = engine(maxTickMillis = 100)
        val still = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(40f, 80f),
            action = PetAction.TALK,
            direction = PetDirection.RIGHT
        )
        val moving = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(70f, 80f),
            action = PetAction.TALK_WALK,
            direction = PetDirection.RIGHT
        )

        val stillAdvanced = engine.reduce(still, PetEvent.Tick(1_000)).state
        var movingAdvanced = moving
        repeat(10) {
            movingAdvanced = engine.reduce(movingAdvanced, PetEvent.Tick(100)).state
        }

        assertEquals(still.position, stillAdvanced.position)
        assertEquals(0, stillAdvanced.frameIndex)
        assertTrue(movingAdvanced.position.x < bounds.right - size.width)
        assertEquals(PetDirection.LEFT, movingAdvanced.direction)
        assertEquals(PetAction.TALK_WALK, movingAdvanced.action)
    }

    @Test
    fun `showcase uses anticipation pauses and sustained special performances`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(bounds, size, position = PetVector(0f, 80f))

        val started = engine.reduce(initial, PetEvent.Showcase)
        val firstSpecial = advanceUntil(engine, started.state) {
            it.action == PetAction.SPECIAL
        }
        val recovery = advanceUntil(engine, firstSpecial) { it.action == PetAction.IDLE }
        val secondSpecial = advanceUntil(engine, recovery) {
            it.action == PetAction.SPECIAL_2
        }

        assertEquals(PetAction.SIT, started.state.action)
        assertEquals(PetAction.SPECIAL, firstSpecial.action)
        assertEquals(PetAction.IDLE, recovery.action)
        assertEquals(PetAction.SPECIAL_2, secondSpecial.action)
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
        val initial = engine.initialState(bounds, size, position = PetVector(0f, 80f))

        val tapped = engine.reduce(initial, PetEvent.Tap)

        assertEquals(PetAction.WALK, tapped.state.action)
        assertTrue(tapped.effects.contains(PetEffect.Tapped))
    }

    @Test
    fun `tap and ground social combo cannot interrupt a wall climb`() {
        val engine = engine()
        val climbing = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(80f, 30f),
            action = PetAction.CLIMB_WALL,
            direction = PetDirection.RIGHT
        )

        val tapped = engine.reduce(climbing, PetEvent.Tap)
        val social = engine.reduce(
            climbing,
            PetEvent.StartCombo(PetComboId.SOCIAL_HELLO, PetDirection.LEFT)
        )

        assertEquals(climbing, tapped.state)
        assertEquals(climbing, social.state)
        assertTrue(tapped.effects.isEmpty())
        assertTrue(social.effects.isEmpty())
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
    fun `parkour combo runs toward nearest wall and preserves its climb story on impact`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(75f, 40f),
            action = PetAction.WALK,
            direction = PetDirection.LEFT
        )

        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_PARKOUR)
        )
        val climbing = engine.reduce(started.state, PetEvent.Tick(200))

        assertEquals(PetDirection.RIGHT, started.state.direction)
        assertEquals(PetAction.RUN, started.state.action)
        assertEquals(PetAction.CLIMB_WALL, climbing.state.action)
        assertEquals(PetComboId.WALL_PARKOUR, climbing.state.activeComboId)
        assertEquals(PetAction.DANGLE, climbing.state.pendingComboBeats.first().action)
    }

    @Test
    fun `ceiling expedition continues from wall to ceiling without cancelling combo`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(75f, 40f),
            action = PetAction.WALK
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.CEILING_EXPEDITION)
        )
        val onWall = engine.reduce(started.state, PetEvent.Tick(200)).state

        val onCeiling = engine.reduce(
            onWall.copy(position = PetVector(80f, 1f)),
            PetEvent.Tick(100)
        )

        assertEquals(PetAction.CLIMB_CEILING, onCeiling.state.action)
        assertEquals(PetDirection.LEFT, onCeiling.state.direction)
        assertEquals(PetComboId.CEILING_EXPEDITION, onCeiling.state.activeComboId)
        assertEquals(PetAction.DANGLE, onCeiling.state.pendingComboBeats.first().action)
        assertTrue(onCeiling.state.comboBeatTargetMillis >= 12_000L)
    }

    @Test
    fun `parkour holds a wall dangle before jumping back into the screen`() {
        val engine = engine(maxTickMillis = 1_000)
        val largeBounds = PetBounds(0f, 0f, 2_000f, 2_000f)
        val initial = engine.initialState(
            bounds = largeBounds,
            size = size,
            position = PetVector(1_975f, 1_500f),
            action = PetAction.WALK
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_PARKOUR)
        )
        val climbing = engine.reduce(started.state, PetEvent.Tick(200)).state

        val dangling = advanceUntil(engine, climbing) { it.action == PetAction.DANGLE }
        val jumped = advanceUntil(engine, dangling) { it.action == PetAction.JUMP }

        assertEquals(1_980f, dangling.position.x, FLOAT_TOLERANCE)
        assertTrue(dangling.comboBeatTargetMillis >= 6_000L)
        assertEquals(PetDirection.LEFT, jumped.direction)
        assertEquals(PetComboId.WALL_PARKOUR, jumped.activeComboId)
    }

    @Test
    fun `ceiling climb timeout is derived from screen distance and boosted pack velocity`() {
        val engine = engine(maxTickMillis = 1_000)
        val tallBounds = PetBounds(0f, 0f, 100f, 2_500f)
        val initial = engine.initialState(
            bounds = tallBounds,
            size = size,
            position = PetVector(75f, 2_000f),
            action = PetAction.WALK
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.CEILING_EXPEDITION)
        )

        val onWall = engine.reduce(started.state, PetEvent.Tick(200)).state

        assertEquals(PetAction.CLIMB_WALL, onWall.action)
        assertEquals(PetBeatCompletion.COLLISION, onWall.activeComboBeat?.completion)
        assertTrue(onWall.comboBeatTargetMillis > 22_000L)
        assertTrue(onWall.comboBeatTargetMillis < 30_000L)
    }

    @Test
    fun `wall dive jumps inward after holding its climb beat`() {
        val engine = engine(maxTickMillis = 1_000)
        val largeBounds = PetBounds(0f, 0f, 1_000f, 1_000f)
        val initial = engine.initialState(
            bounds = largeBounds,
            size = size,
            position = PetVector(975f, 600f),
            action = PetAction.WALK,
            direction = PetDirection.LEFT
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_DIVE)
        )
        val climbing = engine.reduce(started.state, PetEvent.Tick(200)).state

        val jumped = advanceUntil(engine, climbing) { it.action == PetAction.JUMP }

        assertEquals(PetAction.CLIMB_WALL, climbing.action)
        assertEquals(PetDirection.RIGHT, climbing.direction)
        assertEquals(PetDirection.LEFT, jumped.direction)
        assertEquals(PetComboId.WALL_DIVE, jumped.activeComboId)
    }

    @Test
    fun `wall dive jumps inward when the wall ends before its timed climb does`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(75f, 40f),
            action = PetAction.WALK
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_DIVE)
        )
        val climbing = engine.reduce(started.state, PetEvent.Tick(200)).state

        val jumped = engine.reduce(
            climbing.copy(position = PetVector(80f, 1f)),
            PetEvent.Tick(100)
        )

        assertEquals(PetAction.JUMP, jumped.state.action)
        assertEquals(PetDirection.LEFT, jumped.state.direction)
        assertEquals(PetComboId.WALL_DIVE, jumped.state.activeComboId)
    }

    @Test
    fun `wall to wall leap crosses the screen and catches the opposite wall`() {
        val engine = engine(maxTickMillis = 1_000)
        val tallBounds = PetBounds(0f, 0f, 1_000f, 3_000f)
        val initial = engine.initialState(
            bounds = tallBounds,
            size = size,
            position = PetVector(975f, 2_500f),
            action = PetAction.WALK,
            direction = PetDirection.LEFT
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_TO_WALL_LEAP)
        )
        val firstClimb = engine.reduce(started.state, PetEvent.Tick(200)).state
        val takeoffPose = advanceUntil(engine, firstClimb) {
            it.action == PetAction.DANGLE
        }
        val takeoff = advanceUntil(engine, takeoffPose) { it.action == PetAction.JUMP }
        val crossing = advanceUntil(engine, takeoff) {
            it.action == PetAction.FALL &&
                it.activeComboBeat?.crossScreenDurationMillis != null
        }

        val oppositeWall = advanceUntil(engine, crossing) {
            it.action == PetAction.CLIMB_WALL
        }

        assertEquals(PetDirection.LEFT, crossing.direction)
        assertTrue(crossing.comboBeatTargetMillis in 4_000L..5_000L)
        assertEquals(0f, oppositeWall.position.x, FLOAT_TOLERANCE)
        assertEquals(PetDirection.LEFT, oppositeWall.direction)
        assertEquals(PetComboId.WALL_TO_WALL_LEAP, oppositeWall.activeComboId)
        assertEquals(PetAction.DANGLE, oppositeWall.pendingComboBeats.first().action)
    }

    @Test
    fun `wall to wall leap mirrors the crossing from the left wall`() {
        val engine = engine(maxTickMillis = 1_000)
        val tallBounds = PetBounds(0f, 0f, 1_000f, 3_000f)
        val initial = engine.initialState(
            bounds = tallBounds,
            size = size,
            position = PetVector(5f, 2_500f),
            action = PetAction.WALK,
            direction = PetDirection.RIGHT
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_TO_WALL_LEAP)
        )
        val firstClimb = engine.reduce(started.state, PetEvent.Tick(200)).state
        val takeoffPose = advanceUntil(engine, firstClimb) {
            it.action == PetAction.DANGLE
        }
        val takeoff = advanceUntil(engine, takeoffPose) { it.action == PetAction.JUMP }
        val crossing = advanceUntil(engine, takeoff) {
            it.action == PetAction.FALL &&
                it.activeComboBeat?.crossScreenDurationMillis != null
        }

        val oppositeWall = advanceUntil(engine, crossing) {
            it.action == PetAction.CLIMB_WALL
        }

        assertEquals(PetDirection.RIGHT, crossing.direction)
        assertEquals(980f, oppositeWall.position.x, FLOAT_TOLERANCE)
        assertEquals(PetDirection.RIGHT, oppositeWall.direction)
        assertEquals(PetComboId.WALL_TO_WALL_LEAP, oppositeWall.activeComboId)
    }

    @Test
    fun `wall to wall rise moves upward before catching the opposite wall`() {
        val engine = engine(maxTickMillis = 1_000)
        val tallBounds = PetBounds(0f, 0f, 1_000f, 3_000f)
        val initial = engine.initialState(
            bounds = tallBounds,
            size = size,
            position = PetVector(975f, 2_500f),
            action = PetAction.WALK,
            direction = PetDirection.LEFT
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.WALL_TO_WALL_RISE)
        )
        val firstClimb = engine.reduce(started.state, PetEvent.Tick(200)).state
        val takeoffPose = advanceUntil(engine, firstClimb) {
            it.action == PetAction.DANGLE
        }
        val takeoff = advanceUntil(engine, takeoffPose) { it.action == PetAction.JUMP }
        val crossing = advanceUntil(engine, takeoff) {
            it.action == PetAction.FLUNG &&
                it.activeComboBeat?.crossScreenLaunchVelocityY != null
        }
        val rising = engine.reduce(crossing, PetEvent.Tick(100)).state
        val oppositeWall = advanceUntil(engine, rising) {
            it.action == PetAction.CLIMB_WALL
        }

        assertEquals(PetDirection.LEFT, crossing.direction)
        assertEquals(PetAction.FLUNG, crossing.action)
        assertTrue(rising.velocity.y < 0f)
        assertTrue(rising.position.y < crossing.position.y)
        assertEquals(0f, oppositeWall.position.x, FLOAT_TOLERANCE)
        assertTrue(oppositeWall.position.y < crossing.position.y)
        assertEquals(PetDirection.LEFT, oppositeWall.direction)
        assertEquals(PetComboId.WALL_TO_WALL_RISE, oppositeWall.activeComboId)
        assertEquals(PetAction.DANGLE, oppositeWall.pendingComboBeats.first().action)
    }

    @Test
    fun `sky diver keeps its landing bounce inside the combo`() {
        val engine = engine(maxTickMillis = 1_000)
        val tallBounds = PetBounds(0f, 0f, 100f, 1_000f)
        val initial = engine.initialState(
            bounds = tallBounds,
            size = size,
            position = PetVector(20f, 100f),
            action = PetAction.WALK
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.SKY_DIVER)
        )
        val falling = advanceUntil(engine, started.state) { it.action == PetAction.FALL }

        val landed = engine.reduce(
            falling.copy(position = PetVector(20f, 975f)),
            PetEvent.Tick(100)
        )

        assertEquals(PetAction.BOUNCE, landed.state.action)
        assertEquals(PetComboId.SKY_DIVER, landed.state.activeComboId)
        assertEquals(PetAction.SIT, landed.state.pendingComboBeats.first().action)
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
    fun `two non-climb stories force the next autonomous choice into a climb habitat`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 1_000,
                behaviorProfile = behaviorProfile(
                    groundDelayMillis = 100L..100L,
                    maxNonClimbCombosBeforeClimb = 2,
                    autonomousComboRules = listOf(
                        PetComboRule(PetComboId.CURIOUS_SCOUT, 10_000),
                        PetComboRule(PetComboId.WALL_PARKOUR, 1)
                    )
                )
            )
        )
        val walking = engine.initialState(
            bounds = PetBounds(0f, 0f, 1_000f, 1_000f),
            size = size,
            position = PetVector(500f, 980f),
            action = PetAction.WALK
        ).copy(nonClimbComboStreak = 2)

        val selected = engine.reduce(walking, PetEvent.Tick(100))

        assertEquals(PetComboId.WALL_PARKOUR, selected.state.activeComboId)
        assertEquals(PetComboHabitat.WALL, PetComboCatalog.definition(
            checkNotNull(selected.state.activeComboId)
        )?.habitat)
        assertEquals(0, selected.state.nonClimbComboStreak)
    }

    @Test
    fun `climb quota falls back to compatible ground story for a pack without climb frames`() {
        val supportedWithoutClimb = PetAction.entries.toSet() - PetAction.CLIMB_WALL
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 1_000,
                supportedActions = supportedWithoutClimb,
                behaviorProfile = behaviorProfile(
                    groundDelayMillis = 100L..100L,
                    maxNonClimbCombosBeforeClimb = 2,
                    autonomousComboRules = listOf(
                        PetComboRule(PetComboId.CURIOUS_SCOUT, 1),
                        PetComboRule(PetComboId.WALL_PARKOUR, 10_000)
                    )
                )
            )
        )
        val walking = engine.initialState(
            bounds = PetBounds(0f, 0f, 1_000f, 1_000f),
            size = size,
            action = PetAction.WALK
        ).copy(nonClimbComboStreak = 2)

        val selected = engine.reduce(walking, PetEvent.Tick(100))

        assertEquals(PetComboId.CURIOUS_SCOUT, selected.state.activeComboId)
        assertEquals(3, selected.state.nonClimbComboStreak)
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
            activeComboBeat = null,
            comboBeatElapsedMillis = 0,
            comboBeatTargetMillis = 0,
            pendingComboBeats = emptyList(),
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
    fun `combo holds looping beats before advancing to the next story beat`() {
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
            position = PetVector(0f, 980f),
            action = PetAction.WALK
        )

        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.HAPPY_ZOOMIES)
        )
        val stillAnticipating = engine.reduce(started.state, PetEvent.Tick(1_000)).state
        val wink = advanceUntil(engine, stillAnticipating) { it.action == PetAction.WINK }
        val run = advanceUntil(engine, wink) { it.action == PetAction.RUN }
        var stillRunning = run
        repeat(3) {
            stillRunning = engine.reduce(stillRunning, PetEvent.Tick(1_000)).state
        }

        assertEquals(PetComboId.HAPPY_ZOOMIES, started.state.activeComboId)
        assertEquals(PetAction.IDLE, started.state.action)
        assertEquals(PetAction.IDLE, stillAnticipating.action)
        assertEquals(PetAction.WINK, wink.action)
        assertEquals(PetAction.RUN, run.action)
        assertEquals(PetAction.RUN, stillRunning.action)
        assertEquals(PetComboId.HAPPY_ZOOMIES, stillRunning.activeComboId)
    }

    @Test
    fun `external social combo faces peer and completes as one sequence`() {
        val engine = PetEngine(
            PetEngineConfig(
                maxTickMillis = 2_000,
                behaviorProfile = behaviorProfile(idleDurationMillis = 100L..100L)
            )
        )
        val initial = engine.initialState(
            bounds,
            size,
            position = PetVector(0f, 80f),
            direction = PetDirection.RIGHT
        )

        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.SOCIAL_HELLO, PetDirection.LEFT)
        )
        val waiting = engine.reduce(started.state, PetEvent.Tick(1_000)).state
        val wink = advanceUntil(engine, waiting) { it.action == PetAction.WINK }
        val sitting = advanceUntil(engine, wink) { it.action == PetAction.SIT }
        var state = sitting
        var completed: PetTransition? = null
        repeat(300) {
            val transition = engine.reduce(state, PetEvent.Tick(100))
            state = transition.state
            if (transition.effects.contains(
                    PetEffect.ComboCompleted(PetComboId.SOCIAL_HELLO)
                )
            ) {
                completed = transition
                return@repeat
            }
        }

        assertEquals(PetDirection.LEFT, started.state.direction)
        assertEquals(PetAction.TALK, started.state.action)
        assertEquals(PetAction.TALK, waiting.action)
        assertEquals(PetAction.WINK, wink.action)
        assertEquals(PetAction.SIT, sitting.action)
        assertEquals(null, completed?.state?.activeComboId)
        assertTrue(completed != null)
    }

    @Test
    fun `long sitting beat repeats its pose instead of flashing into the next action`() {
        val engine = engine(maxTickMillis = 2_500)
        val initial = engine.initialState(
            bounds,
            size,
            position = PetVector(0f, 80f),
            action = PetAction.WALK
        )
        val started = engine.reduce(
            initial,
            PetEvent.StartCombo(PetComboId.SOCIAL_REST_A)
        )

        val afterOneSitClip = engine.reduce(started.state, PetEvent.Tick(2_400)).state

        assertEquals(PetAction.SIT, started.state.action)
        assertTrue(started.state.comboBeatTargetMillis >= 10_000L)
        assertEquals(PetAction.SIT, afterOneSitClip.action)
        assertEquals(PetComboId.SOCIAL_REST_A, afterOneSitClip.activeComboId)
        assertEquals(2_400L, afterOneSitClip.comboBeatElapsedMillis)
    }

    @Test
    fun `special performance repeats seamlessly until its sustained beat is complete`() {
        val engine = engine(maxTickMillis = 1_000)
        val initial = engine.initialState(
            bounds,
            size,
            position = PetVector(0f, 80f),
            action = PetAction.WALK
        )
        val showcase = engine.reduce(initial, PetEvent.Showcase).state
        val special = advanceUntil(engine, showcase) { it.action == PetAction.SPECIAL }

        val afterOneSpecialClip = engine.reduce(special, PetEvent.Tick(880)).state

        assertTrue(special.comboBeatTargetMillis >= 4_500L)
        assertEquals(PetAction.SPECIAL, afterOneSpecialClip.action)
        assertEquals(PetComboId.USER_SHOWCASE, afterOneSpecialClip.activeComboId)
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
        maxNonClimbCombosBeforeClimb: Int = 2,
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
        maxNonClimbCombosBeforeClimb = maxNonClimbCombosBeforeClimb,
        autonomousComboRules = autonomousComboRules
    )

    private fun advanceUntil(
        engine: PetEngine,
        initial: PetState,
        condition: (PetState) -> Boolean
    ): PetState {
        var state = initial
        repeat(MAX_ADVANCE_TICKS) {
            if (condition(state)) return state
            state = engine.reduce(state, PetEvent.Tick(ADVANCE_STEP_MILLIS)).state
        }
        error("pet state did not reach the expected combo beat")
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
        const val ADVANCE_STEP_MILLIS = 100L
        const val MAX_ADVANCE_TICKS = 600
    }
}
