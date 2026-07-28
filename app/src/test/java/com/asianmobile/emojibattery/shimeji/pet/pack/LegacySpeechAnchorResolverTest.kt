package com.asianmobile.emojibattery.shimeji.pet.pack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacySpeechAnchorResolverTest {
    @Test
    fun `anchor follows the corner where the hand enters the window notch`() {
        val opaque = buildSet {
            for (y in 20..120) {
                for (x in 54..100) add(x to y)
            }
            for (y in 86..90) {
                for (x in 34..53) add(x to y)
            }
        }

        val anchor = LegacySpeechAnchorResolver.resolve(128, 128) { x, y ->
            x to y in opaque
        }

        assertEquals(PetPackAnchor(0.421875f, 0.671875f), anchor)
    }

    @Test
    fun `missing notch falls back to center x and hand edge y`() {
        val anchor = LegacySpeechAnchorResolver.resolve(128, 128) { x, y ->
            x in 20..21 && y in 60..70
        }

        assertEquals(PetPackAnchor(0.5f, 0.5078125f), anchor)
    }

    @Test
    fun `extreme transparent edge noise keeps safe fallback anchor`() {
        val anchor = LegacySpeechAnchorResolver.resolve(128, 128) { x, y ->
            x == 0 && y == 2
        }

        assertEquals(PetPackAnchor(0.5f, 0.25f), anchor)
    }

    @Test
    fun `fully transparent frame has no speech anchor`() {
        val anchor = LegacySpeechAnchorResolver.resolve(128, 128) { _, _ -> false }

        assertNull(anchor)
    }

    @Test
    fun `argb resolver ignores near transparent antialias noise`() {
        val anchor = LegacySpeechAnchorResolver.resolveArgb(128, 128) { x, y ->
            when {
                x == 0 && y == 0 -> 0x0F000000
                x == 32 && y == 64 -> 0x10000000
                else -> 0
            }
        }

        assertEquals(PetPackAnchor(0.5f, 0.5f), anchor)
    }
}
