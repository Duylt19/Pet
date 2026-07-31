package com.asianmobile.emojibattery.shimeji.pet.overlay

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import org.junit.Assert.assertEquals
import org.junit.Test

class PetSpriteTransformPolicyTest {
    @Test
    fun `left wall contact rotates clockwise onto the top edge`() {
        assertEquals(90f, DERIVED_CEILING_ROTATION_DEGREES, 0f)
    }

    @Test
    fun `clockwise wall rotation faces right without mirror`() {
        assertEquals(
            PetSpriteTransformPolicy.DERIVED_CEILING,
            spriteTransformPolicy(
                usesDerivedCeilingVisual = true,
                action = PetAction.CLIMB_CEILING,
                shouldMirror = true
            )
        )
    }

    @Test
    fun `clockwise wall rotation mirrors in screen space to face left`() {
        assertEquals(
            PetSpriteTransformPolicy.DERIVED_CEILING_MIRRORED,
            spriteTransformPolicy(
                usesDerivedCeilingVisual = true,
                action = PetAction.HOLD_CEILING,
                shouldMirror = false
            )
        )
    }

    @Test
    fun `regular sprite keeps existing motion then mirror order`() {
        assertEquals(
            PetSpriteTransformPolicy.REGULAR_MIRRORED,
            spriteTransformPolicy(
                usesDerivedCeilingVisual = false,
                action = PetAction.WALK,
                shouldMirror = true
            )
        )
    }
}
