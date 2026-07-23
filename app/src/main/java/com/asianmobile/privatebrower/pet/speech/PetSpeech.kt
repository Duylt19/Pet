package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetComboId
import com.asianmobile.privatebrower.pet.engine.PetState
import com.asianmobile.privatebrower.pet.engine.PetTransition
import java.util.ArrayDeque
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
 * Serializes pose-gated speech across all overlay pets. A message may only start while its owner is
 * rendering [PetAction.TALK], so text can never float over an unrelated movement or skill frame.
 */
class PetSpeechDirector(
    private val catalog: PetSpeechCatalog,
    seed: Int = 0
) {
    private val random = Random(seed)
    private val pending = ArrayDeque<PendingSpeech>()
    private val actionByPet = mutableMapOf<Int, PetAction>()
    private var lastLineText: String? = null
    private var active: ActiveSpeech? = null

    fun onTransition(
        petId: Int,
        previousState: PetState,
        transition: PetTransition
    ): List<PetSpeechDirective> {
        val directives = mutableListOf<PetSpeechDirective>()
        actionByPet[petId] = transition.state.action
        if (transition.state.action in INTERRUPTING_ACTIONS) {
            directives += cancel(petId)
            return directives
        }
        if (previousState.action == PetAction.TALK &&
            transition.state.action != PetAction.TALK
        ) {
            directives += cancel(petId)
        }

        if (previousState.action != PetAction.TALK &&
            transition.state.action == PetAction.TALK
        ) {
            PetComboSpeechPolicy.cueFor(transition.state.activeComboId)?.let { cue ->
                directives += request(petId, cue.tone, cue.priority)
            }
        }
        return directives
    }

    fun reset() {
        active = null
        pending.clear()
        actionByPet.clear()
        lastLineText = null
    }

    private fun request(
        petId: Int,
        tone: PetSpeechTone,
        priority: PetSpeechPriority
    ): List<PetSpeechDirective> {
        val available = catalog.lines(tone)
        if (available.isEmpty()) return emptyList()
        if (active?.let { it.petId == petId && it.line.tone == tone } == true ||
            pending.any { it.petId == petId && it.tone == tone }
        ) {
            return emptyList()
        }

        val queued = PendingSpeech(petId, tone, priority)
        if (priority == PetSpeechPriority.USER) {
            pending.removeIf { it.petId == petId }
            pending.addFirst(queued)
            return startNext()
        }

        if (pending.size >= MAX_PENDING_SPEECH) return emptyList()
        pending.addLast(queued)
        return startNext()
    }

    private fun startNext(): List<PetSpeechDirective> {
        if (active != null) return emptyList()
        pending.removeIf { queued -> actionByPet[queued.petId] != PetAction.TALK }
        val next = pending
            .maxByOrNull(PendingSpeech::priority)
            ?: return emptyList()
        pending.remove(next)
        val line = chooseLine(next.tone) ?: return startNext()
        active = ActiveSpeech(next.petId, line)
        return listOf(
            PetSpeechDirective.Show(next.petId, line)
        )
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
        pending.removeIf { it.petId == petId }
        val current = active?.takeIf { it.petId == petId } ?: return emptyList()
        active = null
        return buildList {
            add(PetSpeechDirective.Hide(current.petId))
            addAll(startNext())
        }
    }

    private data class PendingSpeech(
        val petId: Int,
        val tone: PetSpeechTone,
        val priority: PetSpeechPriority
    )

    private data class ActiveSpeech(
        val petId: Int,
        val line: PetSpeechLine
    )

    private companion object {
        val INTERRUPTING_ACTIONS = setOf(PetAction.DRAGGED, PetAction.FLUNG)
        const val MAX_PENDING_SPEECH = 4
    }
}

internal enum class PetSpeechPriority {
    AMBIENT,
    SOCIAL,
    USER
}

internal data class PetSpeechCue(
    val tone: PetSpeechTone,
    val priority: PetSpeechPriority
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
            PetSpeechTone.AFFECTION,
            PetSpeechPriority.USER
        )

        PetComboId.USER_SHOWCASE -> cue(
            PetSpeechTone.CELEBRATION,
            PetSpeechPriority.USER
        )

        PetComboId.SOCIAL_HELLO -> cue(
            PetSpeechTone.SOCIAL_HELLO,
            PetSpeechPriority.SOCIAL
        )

        PetComboId.SOCIAL_HELLO_REPLY -> cue(
            PetSpeechTone.SOCIAL_REPLY,
            PetSpeechPriority.SOCIAL
        )

        PetComboId.SOCIAL_SHOW_OFF,
        PetComboId.SOCIAL_ADMIRE -> cue(
            PetSpeechTone.CELEBRATION,
            PetSpeechPriority.SOCIAL
        )

        PetComboId.CURIOUS_SCOUT,
        PetComboId.COZY_BREAK,
        PetComboId.CLUMSY_RECOVERY,
        PetComboId.DAYDREAM -> cue(PetSpeechTone.CHATTER)

        PetComboId.WALL_PARKOUR,
        PetComboId.CEILING_EXPEDITION,
        PetComboId.WALL_DIVE,
        PetComboId.WALL_TO_WALL_LEAP,
        PetComboId.WALL_TO_WALL_RISE,
        PetComboId.SKY_DIVER,
        PetComboId.NINJA_SKILL,
        PetComboId.BATTLE_DANCE,
        PetComboId.MAGIC_RITUAL,
        PetComboId.ACROBATIC_FINALE -> cue(PetSpeechTone.SKILL)

        PetComboId.HAPPY_ZOOMIES,
        PetComboId.TINY_PERFORMANCE,
        PetComboId.CHEERFUL_ENCORE -> cue(PetSpeechTone.CELEBRATION)

        PetComboId.SHY_SNEAK,
        PetComboId.BUSY_PATROL,
        PetComboId.PEEK_AND_DASH,
        PetComboId.SLOW_MORNING,
        PetComboId.BRAVE_EXPLORER,
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

    private fun cue(
        tone: PetSpeechTone,
        priority: PetSpeechPriority = PetSpeechPriority.AMBIENT
    ) = PetSpeechCue(tone, priority)
}
