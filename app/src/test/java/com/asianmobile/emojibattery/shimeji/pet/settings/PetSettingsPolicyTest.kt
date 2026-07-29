package com.asianmobile.emojibattery.shimeji.pet.settings

import com.asianmobile.emojibattery.shimeji.data.model.PetPositionFraction
import com.asianmobile.emojibattery.shimeji.data.model.PetSlotPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSwarmMovementInsets
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBounds
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PetSettingsPolicyTest {
    private val policy = PetSettingsPolicy()

    @Test
    fun `pet count respects device budget`() {
        assertEquals(1, policy.sanitizePetCount(0, maxPets = 3))
        assertEquals(2, policy.sanitizePetCount(3, maxPets = 2))
        assertEquals(12, policy.sanitizePetCount(20, maxPets = 12))
    }

    @Test
    fun `mixed rewarded capacity always keeps three free slots and caps at twelve`() {
        assertEquals(3, policy.sanitizeMixedRewardUnlockedSlotCount(0))
        assertEquals(4, policy.sanitizeMixedRewardUnlockedSlotCount(4))
        assertEquals(12, policy.sanitizeMixedRewardUnlockedSlotCount(99))
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
    fun `larger mixed sessions use the same progressive frame budget`() {
        assertEquals(30, policy.targetFramesPerSecond(2, 30))
        assertEquals(24, policy.targetFramesPerSecond(3, 30))
        assertEquals(20, policy.targetFramesPerSecond(6, 30))
        assertEquals(16, policy.targetFramesPerSecond(12, 30))
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
            ),
            petSize = PetSize(width = 90f, height = 90f)
        )

        assertEquals(
            PetBounds(left = 70f, top = 70f, right = 1030f, bottom = 1400f),
            constrained
        )
    }

    @Test
    fun `zero swarm movement insets retain normal screen edge overflow`() {
        val constrained = policy.constrainSwarmBounds(
            bounds = PetBounds(left = 0f, top = 0f, right = 1000f, bottom = 2000f),
            insets = PetSwarmMovementInsets(),
            petSize = PetSize(width = 300f, height = 300f)
        )

        assertEquals(
            PetBounds(left = -100f, top = -100f, right = 1100f, bottom = 2000f),
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
            listOf(PetPositionFraction(0f, 0.25f), PetPositionFraction(0.75f, 1f)),
            decoded.take(2)
        )
        assertEquals(12, decoded.size)
        assertEquals(true, decoded.drop(2).all { it == null })
    }

    @Test
    fun `malformed positions are ignored`() {
        val decoded = codec.decode("broken;0.5,0.5;NaN,1")

        assertEquals(listOf(null, PetPositionFraction(0.5f, 0.5f), null), decoded.take(3))
        assertEquals(12, decoded.size)
        assertEquals(true, decoded.drop(3).all { it == null })
    }

    @Test
    fun `empty position slot survives round trip`() {
        val positions = listOf(
            PetPositionFraction(0.2f, 0.3f),
            null,
            PetPositionFraction(0.8f, 0.9f)
        )

        val decoded = codec.decode(codec.encode(positions))

        assertEquals(positions, decoded.take(positions.size))
        assertEquals(12, decoded.size)
        assertEquals(true, decoded.drop(positions.size).all { it == null })
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
        val encoded = (1..13).joinToString("\n") { index -> "pack.$index@1" }

        assertEquals(
            (1..12).map { index -> "pack.$index@1" },
            codec.decode(encoded)
        )
    }

    @Test
    fun `legacy selection is materialized before one slot changes`() {
        val migrated = codec.materialize(listOf("pack.old@1"))

        val replaced = codec.replace(migrated, slotIndex = 0, key = "pack.new@1")

        assertEquals(12, replaced.size)
        assertEquals("pack.new@1", replaced.first())
        assertEquals(true, replaced.drop(1).all { it == "pack.old@1" })
    }
}

class PetSlotValueCodecTest {
    private val codec = PetSlotValueCodec()

    @Test
    fun `slot values preserve independent order`() {
        val ints = codec.decodeInts(codec.encodeInts(listOf(75, 100, 150)), fallback = 100)
        val booleans = codec.decodeBooleans(
            codec.encodeBooleans(listOf(true, false, true)),
            fallback = true
        )
        val strings = codec.decodeStrings(
            codec.encodeStrings(listOf("hello\nthere", "", "third"))
        )

        assertEquals(listOf(75, 100, 150), ints.take(3))
        assertEquals(true, ints.drop(3).all { it == 100 })
        assertEquals(listOf(true, false, true), booleans.take(3))
        assertEquals(true, booleans.drop(3).all { it })
        assertEquals(listOf("hello\nthere", "", "third"), strings.take(3))
        assertEquals(true, strings.drop(3).all(String::isEmpty))
    }

    @Test
    fun `missing and corrupt slot values use defaults`() {
        assertEquals(List(12) { 100 }, codec.decodeInts("broken", fallback = 100))
        assertEquals(List(12) { false }, codec.decodeBooleans("[]", fallback = false))
    }
}
