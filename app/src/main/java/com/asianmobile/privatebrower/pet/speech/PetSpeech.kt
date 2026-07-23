package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetComboId
import com.asianmobile.privatebrower.pet.engine.PetEffect
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
        val line: PetSpeechLine,
        val durationMillis: Long,
        val attachment: PetSpeechAttachment = PetSpeechAttachment.OVERHEAD
    ) : PetSpeechDirective

    data class Hide(val petId: Int) : PetSpeechDirective
}

/**
 * Serializes speech across all overlay pets so bubbles read as intentional dialogue instead of
 * simultaneous notification spam. User reactions can interrupt ambient speech; social replies wait
 * until the first speaker finishes.
 */
class PetSpeechDirector(
    private val catalog: PetSpeechCatalog,
    seed: Int = 0
) {
    private val random = Random(seed)
    private val pending = ArrayDeque<PendingSpeech>()
    private val cooldownByPet = mutableMapOf<Int, Long>()
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
            directives += cancelTalkWindowSpeech(petId)
        }

        transition.effects.forEach { effect ->
            when (effect) {
                PetEffect.Tapped -> directives += request(
                    petId = petId,
                    tone = PetSpeechTone.AFFECTION,
                    priority = SpeechPriority.USER
                )

                PetEffect.ShowcaseStarted -> directives += request(
                    petId = petId,
                    tone = PetSpeechTone.CELEBRATION,
                    priority = SpeechPriority.USER
                )

                is PetEffect.ComboStarted -> directives += onComboStarted(petId, effect.comboId)
                is PetEffect.ActionChanged,
                is PetEffect.ComboCompleted -> Unit
            }
        }

        if (previousState.action != PetAction.TALK &&
            transition.state.action == PetAction.TALK
        ) {
            directives += request(
                petId = petId,
                tone = PetSpeechTone.CHATTER,
                priority = SpeechPriority.AMBIENT,
                attachment = PetSpeechAttachment.TALK_WINDOW
            )
        }
        return directives
    }

    fun advance(elapsedMillis: Long): List<PetSpeechDirective> {
        if (elapsedMillis <= 0) return emptyList()
        cooldownByPet.replaceAll { _, remaining -> (remaining - elapsedMillis).coerceAtLeast(0) }
        pending.forEach { queued ->
            queued.delayMillis = (queued.delayMillis - elapsedMillis).coerceAtLeast(0)
        }

        val current = active ?: return startNext()
        current.remainingMillis -= elapsedMillis
        if (current.remainingMillis > 0) return emptyList()

        active = null
        cooldownByPet[current.petId] = SPEAKER_COOLDOWN_MILLIS
        return buildList {
            add(PetSpeechDirective.Hide(current.petId))
            addAll(startNext())
        }
    }

    fun reset() {
        active = null
        pending.clear()
        cooldownByPet.clear()
        actionByPet.clear()
        lastLineText = null
    }

    private fun onComboStarted(
        petId: Int,
        comboId: PetComboId
    ): List<PetSpeechDirective> = when (comboId) {
        PetComboId.SOCIAL_HELLO -> request(
            petId,
            PetSpeechTone.SOCIAL_HELLO,
            SpeechPriority.SOCIAL
        )

        PetComboId.SOCIAL_HELLO_REPLY -> request(
            petId,
            PetSpeechTone.SOCIAL_REPLY,
            SpeechPriority.SOCIAL,
            delayMillis = SOCIAL_REPLY_DELAY_MILLIS
        )

        PetComboId.CURIOUS_SCOUT,
        PetComboId.COZY_BREAK,
        PetComboId.CLUMSY_RECOVERY,
        PetComboId.DAYDREAM,
        PetComboId.BUSY_PATROL,
        PetComboId.PEEK_AND_DASH,
        PetComboId.SLOW_MORNING,
        PetComboId.BRAVE_EXPLORER,
        PetComboId.SOCIAL_REST_A,
        PetComboId.SOCIAL_REST_B,
        PetComboId.SOCIAL_COPYCAT_A,
        PetComboId.SOCIAL_COPYCAT_B -> request(
            petId,
            PetSpeechTone.CHATTER,
            SpeechPriority.AMBIENT
        )

        PetComboId.NINJA_SKILL,
        PetComboId.BATTLE_DANCE,
        PetComboId.MAGIC_RITUAL,
        PetComboId.ACROBATIC_FINALE,
        PetComboId.WALL_PARKOUR,
        PetComboId.CEILING_EXPEDITION,
        PetComboId.WALL_DIVE,
        PetComboId.WALL_TO_WALL_LEAP,
        PetComboId.WALL_TO_WALL_RISE,
        PetComboId.SKY_DIVER -> request(
            petId,
            PetSpeechTone.SKILL,
            SpeechPriority.AMBIENT
        )

        PetComboId.HAPPY_ZOOMIES,
        PetComboId.TINY_PERFORMANCE,
        PetComboId.CHEERFUL_ENCORE,
        PetComboId.SOCIAL_CHASE_LEADER,
        PetComboId.SOCIAL_CHASE_FOLLOWER,
        PetComboId.SOCIAL_SHOW_OFF,
        PetComboId.SOCIAL_ADMIRE,
        PetComboId.SOCIAL_DUET_A,
        PetComboId.SOCIAL_DUET_B -> request(
            petId,
            PetSpeechTone.CELEBRATION,
            SpeechPriority.AMBIENT
        )

        else -> emptyList()
    }

    private fun request(
        petId: Int,
        tone: PetSpeechTone,
        priority: SpeechPriority,
        delayMillis: Long = 0,
        attachment: PetSpeechAttachment = PetSpeechAttachment.OVERHEAD
    ): List<PetSpeechDirective> {
        val available = catalog.lines(tone)
        if (available.isEmpty()) return emptyList()
        if (priority != SpeechPriority.USER &&
            cooldownByPet.getOrDefault(petId, 0) > 0
        ) {
            return emptyList()
        }
        if (active?.let { it.petId == petId && it.line.tone == tone } == true ||
            pending.any { it.petId == petId && it.tone == tone }
        ) {
            return emptyList()
        }

        val queued = PendingSpeech(petId, tone, priority, delayMillis, attachment)
        if (priority == SpeechPriority.USER) {
            pending.removeIf { it.petId == petId }
            val interrupted = active
            active = null
            pending.addFirst(queued)
            return buildList {
                if (interrupted != null) add(PetSpeechDirective.Hide(interrupted.petId))
                addAll(startNext())
            }
        }

        if (pending.size >= MAX_PENDING_SPEECH) return emptyList()
        pending.addLast(queued)
        return startNext()
    }

    private fun startNext(): List<PetSpeechDirective> {
        if (active != null) return emptyList()
        pending.removeIf { queued ->
            queued.attachment == PetSpeechAttachment.TALK_WINDOW &&
                actionByPet[queued.petId] != PetAction.TALK
        }
        val next = pending
            .filter { it.delayMillis == 0L }
            .maxByOrNull(PendingSpeech::priority)
            ?: return emptyList()
        pending.remove(next)
        val line = chooseLine(next.tone) ?: return startNext()
        val duration = readingDurationMillis(line.text)
        active = ActiveSpeech(next.petId, line, duration, next.attachment)
        return listOf(
            PetSpeechDirective.Show(next.petId, line, duration, next.attachment)
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
        cooldownByPet[petId] = SPEAKER_COOLDOWN_MILLIS
        return buildList {
            add(PetSpeechDirective.Hide(current.petId))
            addAll(startNext())
        }
    }

    private fun cancelTalkWindowSpeech(petId: Int): List<PetSpeechDirective> {
        pending.removeIf { queued ->
            queued.petId == petId &&
                queued.attachment == PetSpeechAttachment.TALK_WINDOW
        }
        val current = active?.takeIf { speech ->
            speech.petId == petId &&
                speech.attachment == PetSpeechAttachment.TALK_WINDOW
        } ?: return emptyList()
        active = null
        cooldownByPet[petId] = SPEAKER_COOLDOWN_MILLIS
        return buildList {
            add(PetSpeechDirective.Hide(current.petId))
            addAll(startNext())
        }
    }

    private fun readingDurationMillis(text: String): Long {
        val readableCharacters = text.codePointCount(0, text.length)
        return (BASE_READING_MILLIS + readableCharacters * MILLIS_PER_CHARACTER)
            .coerceIn(MIN_READING_MILLIS, MAX_READING_MILLIS)
    }

    private enum class SpeechPriority {
        AMBIENT,
        SOCIAL,
        USER
    }

    private data class PendingSpeech(
        val petId: Int,
        val tone: PetSpeechTone,
        val priority: SpeechPriority,
        var delayMillis: Long,
        val attachment: PetSpeechAttachment
    )

    private data class ActiveSpeech(
        val petId: Int,
        val line: PetSpeechLine,
        var remainingMillis: Long,
        val attachment: PetSpeechAttachment
    )

    private companion object {
        val INTERRUPTING_ACTIONS = setOf(PetAction.DRAGGED, PetAction.FLUNG)
        const val BASE_READING_MILLIS = 3_400L
        const val MILLIS_PER_CHARACTER = 90L
        const val MIN_READING_MILLIS = 4_500L
        const val MAX_READING_MILLIS = 8_500L
        const val SPEAKER_COOLDOWN_MILLIS = 18_000L
        const val SOCIAL_REPLY_DELAY_MILLIS = 2_000L
        const val MAX_PENDING_SPEECH = 4
    }
}
