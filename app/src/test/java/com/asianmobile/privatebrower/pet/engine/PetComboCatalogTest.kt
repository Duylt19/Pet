package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetComboCatalogTest {
    @Test
    fun `catalog keeps the full ordered story when pack supports every action`() {
        val combo = PetComboCatalog.supportedDefinition(
            PetComboId.HAPPY_ZOOMIES,
            PetAction.entries.toSet()
        )

        assertEquals(
            listOf(
                PetAction.IDLE,
                PetAction.WINK,
                PetAction.RUN,
                PetAction.WALK,
                PetAction.IDLE
            ),
            combo?.actions
        )
    }

    @Test
    fun `catalog degrades a combo to actions actually provided by the pack`() {
        val supported = setOf(PetAction.IDLE, PetAction.WALK, PetAction.TAPPED)

        val compatible = PetComboCatalog.supportedDefinition(
            PetComboId.SLOW_MORNING,
            supported
        )
        val incompatible = PetComboCatalog.supportedDefinition(
            PetComboId.TINY_PERFORMANCE,
            supported
        )

        assertEquals(listOf(PetAction.IDLE, PetAction.WALK), compatible?.actions)
        assertNull(incompatible)
    }

    @Test
    fun `spatial combo is rejected instead of losing a required choreography action`() {
        val missingWallClimb = PetAction.entries.toSet() - PetAction.CLIMB_WALL

        val combo = PetComboCatalog.supportedDefinition(
            PetComboId.WALL_PARKOUR,
            missingWallClimb
        )

        assertNull(combo)
    }

    @Test
    fun `upward wall crossing requires flung pose without removing downward crossing`() {
        val withoutFlung = PetAction.entries.toSet() - PetAction.FLUNG

        val downward = PetComboCatalog.supportedDefinition(
            PetComboId.WALL_TO_WALL_LEAP,
            withoutFlung
        )
        val upward = PetComboCatalog.supportedDefinition(
            PetComboId.WALL_TO_WALL_RISE,
            withoutFlung
        )

        assertTrue(downward != null)
        assertNull(upward)
    }

    @Test
    fun `chatter requires a real talk pose and sustains it long enough to read`() {
        val supported = PetComboCatalog.supportedDefinition(
            PetComboId.CHATTER,
            PetAction.entries.toSet()
        )
        val withoutTalk = PetComboCatalog.supportedDefinition(
            PetComboId.CHATTER,
            PetAction.entries.toSet() - PetAction.TALK
        )
        val talkBeat = supported?.beats?.single { it.action == PetAction.TALK }

        assertEquals(9_000L..11_000L, talkBeat?.durationMillis)
        assertNull(withoutTalk)
    }

    @Test
    fun `speech combos distinguish stationary and walking talk beats`() {
        val chatterSpeech = PetComboCatalog.definition(PetComboId.CHATTER)
            ?.beats
            ?.single { it.action.isSpeechAction }
        val scoutSpeech = PetComboCatalog.definition(PetComboId.CURIOUS_SCOUT)
            ?.beats
            ?.single { it.action.isSpeechAction }

        assertEquals(PetAction.TALK, chatterSpeech?.action)
        assertEquals(PetAction.TALK_WALK, scoutSpeech?.action)
        assertEquals(PET_TALK_BEAT_DURATION_MILLIS, chatterSpeech?.durationMillis)
        assertEquals(PET_TALK_BEAT_DURATION_MILLIS, scoutSpeech?.durationMillis)
    }

    @Test
    fun `speech beats never follow a climbing hanging or airborne pose`() {
        val unsafeSpeechPredecessors = setOf(
            PetAction.CLIMB_WALL,
            PetAction.CLIMB_DOWN,
            PetAction.CLIMB_CEILING,
            PetAction.DANGLE,
            PetAction.JUMP,
            PetAction.FALL,
            PetAction.FLUNG,
            PetAction.DRAGGED
        )

        PetComboId.entries.forEach { comboId ->
            val beats = PetComboCatalog.definition(comboId)?.beats.orEmpty()
            beats.forEachIndexed { index, beat ->
                if (!beat.action.isSpeechAction) return@forEachIndexed
                if (index == 0) {
                    assertEquals(PetComboId.SOCIAL_HELLO, comboId)
                    return@forEachIndexed
                }
                assertTrue(
                    "$comboId enters speech directly from ${beats[index - 1].action}",
                    beats[index - 1].action !in unsafeSpeechPredecessors
                )
            }
        }
    }

    @Test
    fun `autonomous profile keeps only distinct ground basics and gives climb meaningful weight`() {
        val rules = PetBehaviorProfile().autonomousComboRules
        val retiredGroundBasics = setOf(
            PetComboId.SHY_SNEAK,
            PetComboId.BUSY_PATROL,
            PetComboId.PEEK_AND_DASH,
            PetComboId.SLOW_MORNING,
            PetComboId.BRAVE_EXPLORER,
            PetComboId.CHEERFUL_ENCORE
        )
        val climbWeight = rules.filter { rule ->
            PetComboCatalog.definition(rule.comboId)?.habitat?.isClimb == true
        }.sumOf(PetComboRule::weight)

        assertEquals(17, rules.size)
        assertTrue(rules.any { it.comboId == PetComboId.CHATTER })
        assertTrue(rules.none { it.comboId in retiredGroundBasics })
        assertTrue(climbWeight * 4 >= rules.sumOf(PetComboRule::weight))
    }

    @Test
    fun `catalog marks spatial and aerial stories with their actual habitat`() {
        assertEquals(
            PetComboHabitat.WALL,
            PetComboCatalog.definition(PetComboId.WALL_PARKOUR)?.habitat
        )
        assertEquals(
            PetComboHabitat.CEILING,
            PetComboCatalog.definition(PetComboId.CEILING_EXPEDITION)?.habitat
        )
        assertEquals(
            PetComboHabitat.AERIAL,
            PetComboCatalog.definition(PetComboId.NINJA_SKILL)?.habitat
        )
        assertEquals(
            PetComboHabitat.WALL,
            PetComboCatalog.definition(PetComboId.WALL_TO_WALL_LEAP)?.habitat
        )
        assertEquals(
            PetComboHabitat.WALL,
            PetComboCatalog.definition(PetComboId.WALL_TO_WALL_RISE)?.habitat
        )
    }

    @Test
    fun `skill performances play once and hold their final frame`() {
        val performanceActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2)
        val performanceBeats = PetComboId.entries.flatMap { comboId ->
            val definition = PetComboCatalog.definition(comboId) ?: return@flatMap emptyList()
            definition.beats
                .filter { beat -> beat.action in performanceActions }
                .map { beat -> definition to beat }
        }

        assertTrue(performanceBeats.isNotEmpty())
        assertTrue(
            performanceBeats.all { (definition, beat) ->
                beat.playback == PetBeatPlayback.HOLD_LAST_FRAME &&
                    beat.action in definition.requiredActions
            }
        )
    }

    @Test
    fun `ninja skill lands before performing and requires every critical action`() {
        val combo = PetComboCatalog.definition(PetComboId.NINJA_SKILL)
        val fallIndex = combo?.actions?.indexOf(PetAction.FALL) ?: -1

        assertTrue(fallIndex >= 0)
        assertEquals(PetAction.BOUNCE, combo?.actions?.get(fallIndex + 1))
        assertEquals(PetAction.SPECIAL, combo?.actions?.get(fallIndex + 2))
        assertTrue(
            combo?.requiredActions?.containsAll(
                setOf(PetAction.BOUNCE, PetAction.SPECIAL)
            ) == true
        )
    }

    @Test
    fun `social duet roles reserve non overlapping skill turns`() {
        val first = PetComboCatalog.definition(PetComboId.SOCIAL_DUET_A)?.beats.orEmpty()
        val second = PetComboCatalog.definition(PetComboId.SOCIAL_DUET_B)?.beats.orEmpty()
        val firstEnd = first.first().durationMillis!!.last +
            first[1].durationMillis!!.last
        val secondStart = second.first().durationMillis!!.first
        val secondEnd = second.first().durationMillis!!.last +
            second[1].durationMillis!!.last
        val firstSecondStart = first.first().durationMillis!!.first +
            first[1].durationMillis!!.first +
            first[2].durationMillis!!.first
        val firstSecondEnd = first.first().durationMillis!!.last +
            first[1].durationMillis!!.last +
            first[2].durationMillis!!.last +
            first[3].durationMillis!!.last
        val secondSecondStart = second.first().durationMillis!!.first +
            second[1].durationMillis!!.first +
            second[2].durationMillis!!.first

        assertTrue(firstEnd <= secondStart)
        assertTrue(secondEnd <= firstSecondStart)
        assertTrue(firstSecondEnd <= secondSecondStart)
    }

    @Test
    fun `wall to wall variants distinguish falling and rising screen crossing beats`() {
        val fallingCrossing = PetComboCatalog.definition(PetComboId.WALL_TO_WALL_LEAP)
            ?.beats
            .orEmpty()
            .filter { beat -> beat.crossScreenDurationMillis != null }
        val risingCrossing = PetComboCatalog.definition(PetComboId.WALL_TO_WALL_RISE)
            ?.beats
            .orEmpty()
            .filter { beat -> beat.crossScreenDurationMillis != null }

        assertEquals(1, fallingCrossing.size)
        assertEquals(1, risingCrossing.size)
        assertEquals(PetAction.FALL, fallingCrossing.single().action)
        assertEquals(PetAction.FLUNG, risingCrossing.single().action)
        assertEquals(PetBeatCompletion.COLLISION, fallingCrossing.single().completion)
        assertEquals(1_100L, fallingCrossing.single().crossScreenDurationMillis)
        assertNull(fallingCrossing.single().crossScreenLaunchVelocityY)
        assertEquals(-700f, risingCrossing.single().crossScreenLaunchVelocityY)
    }

    @Test
    fun `catalog exposes many solo and paired stories without adjacent empty steps`() {
        val ids = PetComboId.entries
        val resolved = ids.mapNotNull { id ->
            PetComboCatalog.supportedDefinition(id, PetAction.entries.toSet())
        }

        assertTrue(resolved.size >= 37)
        assertTrue(resolved.all { it.actions.size >= 2 })
        assertTrue(
            resolved.flatMap(PetComboDefinition::beats)
                .filter { beat -> beat.durationMillis != null }
                .all { beat -> checkNotNull(beat.durationMillis).first >= 1_500L }
        )
    }
}
