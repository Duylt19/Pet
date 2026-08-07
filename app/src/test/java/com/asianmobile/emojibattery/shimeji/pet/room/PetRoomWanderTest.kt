package com.asianmobile.emojibattery.shimeji.pet.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomWanderTest {
    private val floor = PetRoomFloor(left = 0f, right = 1000f, top = 500f, bottom = 800f)

    @Test
    fun `the back of the room is narrower than the front`() {
        assertTrue(floor.leftAt(floor.top) > floor.leftAt(floor.bottom))
        assertTrue(floor.rightAt(floor.top) < floor.rightAt(floor.bottom))
    }

    @Test
    fun `pets further back are drawn smaller`() {
        assertTrue(floor.scaleAt(floor.top) < floor.scaleAt(floor.bottom))
        assertEquals(1f, floor.scaleAt(floor.bottom), 0.001f)
    }

    @Test
    fun `a pet wanders in both directions instead of one line`() {
        val wanderer = PetRoomWanderer(seed = 7L, floor = floor, walkSpeedPerSecond = 200f)
        var state = wanderer.initial(index = 0, count = 1)
        val startY = state.y
        var sawDifferentY = false

        repeat(600) {
            state = wanderer.advance(state, elapsedMillis = 16L)
            if (kotlin.math.abs(state.y - startY) > 1f) sawDifferentY = true
        }

        assertTrue("pet never changed depth", sawDifferentY)
    }

    @Test
    fun `a pet never leaves the floor`() {
        val wanderer = PetRoomWanderer(seed = 11L, floor = floor, walkSpeedPerSecond = 400f)
        var state = wanderer.initial(index = 0, count = 3)

        repeat(2_000) {
            state = wanderer.advance(state, elapsedMillis = 16L)
            assertTrue(state.y in floor.top..floor.bottom)
            assertTrue(state.x >= floor.leftAt(state.y) - 0.01f)
            assertTrue(state.x <= floor.rightAt(state.y) + 0.01f)
        }
    }

    @Test
    fun `walking and standing alternate`() {
        val wanderer = PetRoomWanderer(seed = 3L, floor = floor, walkSpeedPerSecond = 200f)
        var state = wanderer.initial(index = 0, count = 1)
        var sawWalking = false
        var sawStanding = false

        repeat(1_500) {
            state = wanderer.advance(state, elapsedMillis = 16L)
            if (state.isWalking) sawWalking = true else sawStanding = true
        }

        assertTrue(sawWalking)
        assertTrue(sawStanding)
    }

    @Test
    fun `a pet faces the way it walks`() {
        val wanderer = PetRoomWanderer(seed = 5L, floor = floor, walkSpeedPerSecond = 200f)
        var state = wanderer.initial(index = 0, count = 1)

        repeat(1_000) {
            val previous = state
            state = wanderer.advance(state, elapsedMillis = 16L)
            if (state.isWalking && previous.isWalking && state.x != previous.x) {
                assertEquals(state.x > previous.x, state.facingRight)
            }
        }
    }

    @Test
    fun `pets start spread across the floor rather than stacked`() {
        val wanderer = PetRoomWanderer(seed = 2L, floor = floor, walkSpeedPerSecond = 200f)

        val first = wanderer.initial(index = 0, count = 3)
        val last = wanderer.initial(index = 2, count = 3)

        assertNotEquals(first.x, last.x)
    }

    @Test
    fun `a zero length tick changes nothing`() {
        val wanderer = PetRoomWanderer(seed = 1L, floor = floor, walkSpeedPerSecond = 200f)
        val state = wanderer.initial(index = 0, count = 1)

        assertEquals(state, wanderer.advance(state, elapsedMillis = 0L))
    }
}
