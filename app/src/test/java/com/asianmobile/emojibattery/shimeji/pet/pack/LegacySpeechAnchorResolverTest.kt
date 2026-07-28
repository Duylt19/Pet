package com.asianmobile.emojibattery.shimeji.pet.pack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacySpeechAnchorResolverTest {
    @Test
    fun `anchor follows the median opaque point on the left holding edge`() {
        val opaque = buildSet {
            for (y in 60..70) {
                add(20 to y)
                add(21 to y)
            }
        }

        val anchor = LegacySpeechAnchorResolver.resolve(128, 128) { x, y ->
            x to y in opaque
        }

        assertEquals(PetPackAnchor(0.15625f, 0.5078125f), anchor)
    }

    @Test
    fun `extreme transparent edge noise is clamped to safe holding range`() {
        val anchor = LegacySpeechAnchorResolver.resolve(128, 128) { x, y ->
            x == 0 && y == 2
        }

        assertEquals(PetPackAnchor(0.05f, 0.25f), anchor)
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

        assertEquals(PetPackAnchor(0.25f, 0.5f), anchor)
    }
}
