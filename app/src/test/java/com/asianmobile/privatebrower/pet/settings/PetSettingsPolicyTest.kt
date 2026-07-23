package com.asianmobile.privatebrower.pet.settings

import com.asianmobile.privatebrower.data.model.PetPositionFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class PetSettingsPolicyTest {
    private val policy = PetSettingsPolicy()

    @Test
    fun `pet count respects device budget`() {
        assertEquals(1, policy.sanitizePetCount(0, maxPets = 3))
        assertEquals(2, policy.sanitizePetCount(3, maxPets = 2))
        assertEquals(3, policy.sanitizePetCount(10, maxPets = 3))
    }

    @Test
    fun `size and speed snap to bounded product steps`() {
        assertEquals(75, policy.sanitizeSizePercent(10))
        assertEquals(125, policy.sanitizeSizePercent(119))
        assertEquals(150, policy.sanitizeSizePercent(999))
        assertEquals(50, policy.sanitizeSpeedPercent(0))
        assertEquals(100, policy.sanitizeSpeedPercent(110))
        assertEquals(150, policy.sanitizeSpeedPercent(999))
    }

    @Test
    fun `three pets degrade render rate to shared 24 fps`() {
        assertEquals(30, policy.targetFramesPerSecond(2, 30))
        assertEquals(24, policy.targetFramesPerSecond(3, 30))
        assertEquals(24, policy.targetFramesPerSecond(2, 24))
    }

    @Test
    fun `running session cannot overwrite positions after reset`() {
        assertEquals(true, policy.shouldPersistPositions(4, 4))
        assertEquals(false, policy.shouldPersistPositions(4, 5))
    }
}

class PetPositionCodecTest {
    private val codec = PetPositionCodec()

    @Test
    fun `positions round trip and clamp to normalized bounds`() {
        val decoded = codec.decode(
            codec.encode(
                listOf(
                    PetPositionFraction(-1f, 0.25f),
                    PetPositionFraction(0.75f, 2f)
                )
            )
        )

        assertEquals(
            listOf(PetPositionFraction(0f, 0.25f), PetPositionFraction(0.75f, 1f)),
            decoded
        )
    }

    @Test
    fun `malformed positions are ignored`() {
        assertEquals(
            listOf(PetPositionFraction(0.5f, 0.5f)),
            codec.decode("broken;0.5,0.5;NaN,1")
        )
    }
}

class PetSelectionCodecTest {
    private val codec = PetSelectionCodec()

    @Test
    fun `pack selections preserve slot order and duplicates`() {
        val selections = listOf("pack.cat@1", "pack.dog@2", "pack.cat@1")

        assertEquals(selections, codec.decode(codec.encode(selections)))
    }

    @Test
    fun `pack selections drop blanks and cap persisted slots`() {
        assertEquals(
            listOf("pack.one@1", "pack.two@1", "pack.three@1"),
            codec.decode(" pack.one@1 \n\npack.two@1\npack.three@1\npack.four@1")
        )
    }

    @Test
    fun `legacy selection is materialized before one slot changes`() {
        val migrated = codec.materialize(listOf("pack.old@1"))

        assertEquals(
            listOf("pack.new@1", "pack.old@1", "pack.old@1"),
            codec.replace(migrated, slotIndex = 0, key = "pack.new@1")
        )
    }
}
