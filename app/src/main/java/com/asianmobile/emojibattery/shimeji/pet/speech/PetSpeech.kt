package com.asianmobile.emojibattery.shimeji.pet.speech

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetComboId
import com.asianmobile.emojibattery.shimeji.pet.engine.PetState
import com.asianmobile.emojibattery.shimeji.pet.engine.PetTransition
import com.asianmobile.emojibattery.shimeji.pet.engine.isSpeechAction
import kotlin.random.Random

enum class PetSpeechTone {
    AFFECTION,
    CHATTER,
    SOCIAL_HELLO,
    SOCIAL_REPLY,
    SKILL,
    CELEBRATION
}

data class PetSpeechLine(
    val text: String,
    val tone: PetSpeechTone
)

class PetSpeechCatalog(
    linesByTone: Map<PetSpeechTone, List<String>>
) {
    private val lines = PetSpeechTone.entries.associateWith { tone ->
        linesByTone[tone].orEmpty()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .map { text -> PetSpeechLine(text, tone) }
    }

    internal fun lines(tone: PetSpeechTone): List<PetSpeechLine> = lines.getValue(tone)
}

sealed interface PetSpeechDirective {
    data class Show(
        val petId: Int,
        val line: PetSpeechLine
    ) : PetSpeechDirective

    data class Hide(val petId: Int) : PetSpeechDirective
}

/**
 * Owns one pose-gated speech session per pet. Different pets may speak simultaneously, while each
 * bubble remains strictly tied to its owner's stationary or moving speech action.
 */
class PetSpeechDirector(
    private val catalog: PetSpeechCatalog,
    seed: Int = 0
) {
    private val random = Random(seed)
    private val activeByPet = mutableMapOf<Int, PetSpeechLine>()
    private var lastLineText: String? = null

    fun onTransition(
        petId: Int,
        previousState: PetState,
        transition: PetTransition
    ): List<PetSpeechDirective> {
        val directives = mutableListOf<PetSpeechDirective>()
        if (transition.state.action in INTERRUPTING_ACTIONS) {
            directives += cancel(petId)
            return directives
        }
        if (previousState.action.isSpeechAction &&
            !transition.state.action.isSpeechAction
        ) {
            directives += cancel(petId)
        }

        if (!previousState.action.isSpeechAction &&
            transition.state.action.isSpeechAction
        ) {
            PetComboSpeechPolicy.cueFor(transition.state.activeComboId)?.let { cue ->
                directives += request(petId, cue.tone)
            }
        }
        return directives
    }

    fun reset() {
        activeByPet.clear()
        lastLineText = null
    }

    private fun request(
        petId: Int,
        tone: PetSpeechTone
    ): List<PetSpeechDirective> {
        if (petId in activeByPet) return emptyList()
        val available = catalog.lines(tone)
        if (available.isEmpty()) return emptyList()
        val line = chooseLine(tone) ?: return emptyList()
        activeByPet[petId] = line
        return listOf(PetSpeechDirective.Show(petId, line))
    }

    private fun chooseLine(tone: PetSpeechTone): PetSpeechLine? {
        val lines = catalog.lines(tone)
        if (lines.isEmpty()) return null
        val candidates = lines.filterNot { it.text == lastLineText }.ifEmpty { lines }
        return candidates[random.nextInt(candidates.size)].also { selected ->
            lastLineText = selected.text
        }
    }

    private fun cancel(petId: Int): List<PetSpeechDirective> {
        if (activeByPet.remove(petId) == null) return emptyList()
        return listOf(PetSpeechDirective.Hide(petId))
    }

    private companion object {
        val INTERRUPTING_ACTIONS = setOf(PetAction.DRAGGED, PetAction.FLUNG)
    }
}

internal data class PetSpeechCue(
    val tone: PetSpeechTone
)

/**
 * Defines which choreographed combos are allowed to speak and which vocabulary they use.
 * Physical-only combos deliberately return null and never open a message window.
 */
internal object PetComboSpeechPolicy {
    val speakingComboIds: Set<PetComboId> = PetComboId.entries
        .filterTo(mutableSetOf()) { cueFor(it) != null }

    fun cueFor(comboId: PetComboId?): PetSpeechCue? = when (comboId) {
        null,
        PetComboId.CHATTER -> cue(PetSpeechTone.CHATTER)

        PetComboId.USER_AFFECTION -> cue(
            PetSpeechTone.AFFECTION
        )

        PetComboId.USER_SHOWCASE -> cue(
            PetSpeechTone.CELEBRATION
        )

        PetComboId.SOCIAL_HELLO -> cue(
            PetSpeechTone.SOCIAL_HELLO
        )

        PetComboId.SOCIAL_HELLO_REPLY -> cue(
            PetSpeechTone.SOCIAL_REPLY
        )

        PetComboId.SOCIAL_SHOW_OFF,
        PetComboId.SOCIAL_ADMIRE -> cue(
            PetSpeechTone.CELEBRATION
        )

        PetComboId.CURIOUS_SCOUT,
        PetComboId.COZY_BREAK,
        PetComboId.CLUMSY_RECOVERY -> cue(PetSpeechTone.CHATTER)

        PetComboId.MAGIC_RITUAL -> cue(PetSpeechTone.SKILL)

        PetComboId.TINY_PERFORMANCE,
        PetComboId.CHEERFUL_ENCORE -> cue(PetSpeechTone.CELEBRATION)

        PetComboId.HAPPY_ZOOMIES,
        PetComboId.SHY_SNEAK,
        PetComboId.BUSY_PATROL,
        PetComboId.PEEK_AND_DASH,
        PetComboId.SLOW_MORNING,
        PetComboId.BRAVE_EXPLORER,
        PetComboId.DAYDREAM,
        PetComboId.WALL_PARKOUR,
        PetComboId.CEILING_EXPEDITION,
        PetComboId.WALL_DIVE,
        PetComboId.WALL_TO_WALL_LEAP,
        PetComboId.WALL_TO_WALL_RISE,
        PetComboId.SKY_DIVER,
        PetComboId.NINJA_SKILL,
        PetComboId.BATTLE_DANCE,
        PetComboId.ACROBATIC_FINALE,
        PetComboId.SOCIAL_APPROACH,
        PetComboId.SOCIAL_CHASE_LEADER,
        PetComboId.SOCIAL_CHASE_FOLLOWER,
        PetComboId.SOCIAL_REST_A,
        PetComboId.SOCIAL_REST_B,
        PetComboId.SOCIAL_COPYCAT_A,
        PetComboId.SOCIAL_COPYCAT_B,
        PetComboId.SOCIAL_DUET_A,
        PetComboId.SOCIAL_DUET_B -> null
    }

    private fun cue(tone: PetSpeechTone) = PetSpeechCue(tone)
}
