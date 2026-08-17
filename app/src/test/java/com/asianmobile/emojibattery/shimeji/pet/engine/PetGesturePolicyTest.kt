package com.asianmobile.emojibattery.shimeji.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PetGesturePolicyTest {
    @Test
    fun `slow release drops pet without starting fling physics`() {
        assertEquals(
            PetEvent.DragEnd,
            PetGesturePolicy.releaseEvent(
                velocity = PetVector(x = 20f, y = -10f),
                minimumFlingVelocity = 50f
            )
        )
    }

    @Test
    fun `fast release preserves velocity for fling physics`() {
        val velocity = PetVector(x = 60f, y = 80f)

        assertEquals(
            PetEvent.Fling(velocity),
            PetGesturePolicy.releaseEvent(velocity, minimumFlingVelocity = 50f)
        )
    }
}
