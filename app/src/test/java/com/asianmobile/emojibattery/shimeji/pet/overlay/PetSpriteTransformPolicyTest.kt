package com.asianmobile.emojibattery.shimeji.pet.overlay

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import org.junit.Assert.assertEquals
import org.junit.Test

class PetSpriteTransformPolicyTest {
    @Test
    fun `derived ceiling inverts regular mirror for rotated native facing`() {
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
    fun `derived ceiling applies screen space mirror for opposite direction`() {
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
