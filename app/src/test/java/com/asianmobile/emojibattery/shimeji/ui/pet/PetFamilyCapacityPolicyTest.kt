package com.asianmobile.emojibattery.shimeji.ui.pet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFamilyCapacityPolicyTest {
    @Test
    fun `family accepts pets below the five pet limit`() {
        assertFalse(PetFamilyCapacityPolicy.isFull(4))
    }

    @Test
    fun `family is full at or above the five pet limit`() {
        assertTrue(PetFamilyCapacityPolicy.isFull(5))
        assertTrue(PetFamilyCapacityPolicy.isFull(6))
    }
}
