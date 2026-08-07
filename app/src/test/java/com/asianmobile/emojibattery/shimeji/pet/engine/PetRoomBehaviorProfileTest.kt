package com.asianmobile.emojibattery.shimeji.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomBehaviorProfileTest {
    private val climbing = setOf(
        PetAction.CLIMB_WALL,
        PetAction.CLIMB_DOWN,
        PetAction.CLIMB_CEILING,
        PetAction.HOLD_WALL,
        PetAction.HOLD_CEILING,
        PetAction.DANGLE
    )

    @Test
    fun `room pets never leave the floor`() {
        assertTrue(PetBehaviorProfiles.ROOM.blockedActions.containsAll(climbing))
        assertEquals(0, PetBehaviorProfiles.ROOM.wallJumpChancePercent)
        assertEquals(0, PetBehaviorProfiles.ROOM.wallDescendChancePercent)
    }

    @Test
    fun `room pets stay silent because the room has no speech bubble`() {
        assertTrue(PetAction.TALK in PetBehaviorProfiles.ROOM.blockedActions)
        assertTrue(PetAction.TALK_WALK in PetBehaviorProfiles.ROOM.blockedActions)
    }

    @Test
    fun `room combos never schedule a climb`() {
        val climbCombos = setOf(
            PetComboId.WALL_PARKOUR,
            PetComboId.CEILING_EXPEDITION,
            PetComboId.WALL_DIVE,
            PetComboId.WALL_TO_WALL_LEAP,
            PetComboId.WALL_TO_WALL_RISE,
            PetComboId.SKY_DIVER
        )

        val scheduled = PetBehaviorProfiles.ROOM.autonomousComboRules.map(PetComboRule::comboId)

        assertTrue(scheduled.isNotEmpty())
        assertFalse(scheduled.any { it in climbCombos })
    }

    @Test
    fun `the room never forces a climb after a run of ground combos`() {
        assertEquals(Int.MAX_VALUE, PetBehaviorProfiles.ROOM.maxNonClimbCombosBeforeClimb)
    }

    @Test
    fun `the overlay profile keeps its climbing repertoire`() {
        assertFalse(PetBehaviorProfile().blockedActions.containsAll(climbing))
    }
}
