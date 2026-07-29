package com.asianmobile.emojibattery.shimeji.pet.settings

import com.asianmobile.emojibattery.shimeji.data.model.PetPositionFraction
import com.asianmobile.emojibattery.shimeji.data.model.PetSlotPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmMovementInsets
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBounds
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
        assertEquals(50, policy.sanitizeSizePercent(10))
        assertEquals(80, policy.sanitizeSizePercent(75))
        assertEquals(120, policy.sanitizeSizePercent(119))
        assertEquals(130, policy.sanitizeSizePercent(125))
        assertEquals(150, policy.sanitizeSizePercent(999))
        assertEquals(
            listOf(50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150),
            (50..150 step 10).map(policy::sanitizeSizePercent)
        )
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
    fun `swarm count respects device budget`() {
        assertEquals(1, policy.sanitizeSwarmCount(0, maxPets = 12))
        assertEquals(6, policy.sanitizeSwarmCount(12, maxPets = 6))
        assertEquals(12, policy.sanitizeSwarmCount(99, maxPets = 12))
    }

    @Test
    fun `larger swarms progressively reduce shared render rate`() {
        assertEquals(24, policy.targetSwarmFramesPerSecond(3, 30))
        assertEquals(20, policy.targetSwarmFramesPerSecond(4, 30))
        assertEquals(16, policy.targetSwarmFramesPerSecond(7, 30))
        assertEquals(16, policy.targetSwarmFramesPerSecond(12, 24))
    }

    @Test
    fun `mixed profile always restores one visible active pet`() {
        val hiddenSlots = List(3) { PetSlotPreferences(isEnabled = false) }

        val restored = policy.ensureMixedPetVisible(hiddenSlots, petCount = 2)

        assertEquals(listOf(true, false, false), restored.map { it.isEnabled })
        assertEquals(
            listOf(false, false, false),
            policy.ensureMixedPetVisible(hiddenSlots, petCount = 0).map { it.isEnabled }
        )
    }

    @Test
    fun `swarm variation is deterministic bounded and optional`() {
        val varied = List(8) { index ->
            policy.swarmVariationPercent(
                basePercent = 100,
                instanceIndex = index,
                seed = 42,
                minimumPercent = 50,
                maximumPercent = 150,
                stepPercent = 10,
                enabled = true
            )
        }

        assertEquals(varied, List(8) { index ->
            policy.swarmVariationPercent(100, index, 42, 50, 150, 10, true)
        })
        assertEquals(true, varied.all { it in 50..150 && it % 10 == 0 })
        assertEquals(
            100,
            policy.swarmVariationPercent(100, 5, 42, 50, 150, 10, false)
        )
    }

    @Test
    fun `swarm movement area applies sanitized edge percentages`() {
        val constrained = policy.constrainSwarmBounds(
            bounds = PetBounds(left = 0f, top = 0f, right = 1000f, bottom = 2000f),
            insets = PetSwarmMovementInsets(
                topPercent = 7,
                bottomPercent = 100,
                leftPercent = 11,
                rightPercent = -5
            )
        )

        assertEquals(
            PetBounds(left = 100f, top = 100f, right = 1000f, bottom = 1400f),
            constrained
        )
    }

    @Test
    fun `running session cannot overwrite positions after reset`() {
        assertEquals(true, policy.shouldPersistPositions(4, 4))
        assertEquals(false, policy.shouldPersistPositions(4, 5))
    }

    @Test
    fun `position reset changes are isolated to active slots`() {
        assertEquals(
            listOf(1),
            policy.changedPositionResetSlots(
                previousRevisions = listOf(3, 4, 5),
                currentRevisions = listOf(3, 5, 9),
                petCount = 2
            )
        )
        assertEquals(
            listOf(0, 1),
            policy.changedPositionResetSlots(
                previousRevisions = emptyList(),
                currentRevisions = listOf(1, 2, 3),
                petCount = 2
            )
        )
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
            listOf(
                PetPositionFraction(0f, 0.25f),
                PetPositionFraction(0.75f, 1f),
                null
            ),
            decoded
        )
    }

    @Test
    fun `malformed positions are ignored`() {
        assertEquals(
            listOf(null, PetPositionFraction(0.5f, 0.5f), null),
            codec.decode("broken;0.5,0.5;NaN,1")
        )
    }

    @Test
    fun `empty position slot survives round trip`() {
        val positions = listOf(
            PetPositionFraction(0.2f, 0.3f),
            null,
            PetPositionFraction(0.8f, 0.9f)
        )

        assertEquals(positions, codec.decode(codec.encode(positions)))
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

class PetSlotValueCodecTest {
    private val codec = PetSlotValueCodec()

    @Test
    fun `slot values preserve independent order`() {
        assertEquals(
            listOf(75, 100, 150),
            codec.decodeInts(codec.encodeInts(listOf(75, 100, 150)), fallback = 100)
        )
        assertEquals(
            listOf(true, false, true),
            codec.decodeBooleans(
                codec.encodeBooleans(listOf(true, false, true)),
                fallback = true
            )
        )
        assertEquals(
            listOf("hello\nthere", "", "third"),
            codec.decodeStrings(
                codec.encodeStrings(listOf("hello\nthere", "", "third"))
            )
        )
    }

    @Test
    fun `missing and corrupt slot values use defaults`() {
        assertEquals(listOf(100, 100, 100), codec.decodeInts("broken", fallback = 100))
        assertEquals(
            listOf(false, false, false),
            codec.decodeBooleans("[]", fallback = false)
        )
    }
}
