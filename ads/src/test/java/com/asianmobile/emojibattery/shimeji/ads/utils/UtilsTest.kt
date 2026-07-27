package com.asianmobile.emojibattery.shimeji.ads.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTest {

    @Test
    fun `ad click count below limit has not reached limit`() {
        assertFalse(Utils.hasReachedAdClickLimit(adClickCount = 4, clickLimit = 5L))
    }

    @Test
    fun `ad click count equal to limit has reached limit`() {
        assertTrue(Utils.hasReachedAdClickLimit(adClickCount = 5, clickLimit = 5L))
    }

    @Test
    fun `ad click count above limit has reached limit`() {
        assertTrue(Utils.hasReachedAdClickLimit(adClickCount = 6, clickLimit = 5L))
    }
}
