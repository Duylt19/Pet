package com.asianmobile.emojibattery.shimeji.pet.speech

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAnimationCursor
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBounds
import com.asianmobile.emojibattery.shimeji.pet.engine.PetComboCatalog
import com.asianmobile.emojibattery.shimeji.pet.engine.PetComboId
import com.asianmobile.emojibattery.shimeji.pet.engine.PetDirection
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngine
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngineConfig
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEffect
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEvent
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetState
import com.asianmobile.emojibattery.shimeji.pet.engine.PetTransition
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector
import com.asianmobile.emojibattery.shimeji.pet.engine.isSpeechAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSpeechDirectorTest {
    private val catalog = PetSpeechCatalog(
        PetSpeechTone.entries.associateWith { tone -> listOf("${tone.name} one", "${tone.name} two") }
    )

    @Test
    fun `tap effect does not show text until affection combo enters talk pose`() {
        val director = PetSpeechDirector(catalog, seed = 3)
        val walking = state(PetAction.WALK)
        val tapped = walking.copy(
            action = PetAction.TAPPED,
            activeComboId = PetComboId.USER_AFFECTION
        )

        assertTrue(
            director.onTransition(
                petId = 1,
                previousState = walking,
                transition = PetTransition(tapped, listOf(PetEffect.Tapped))
            ).isEmpty()
        )
        val shown = director.onTransition(
            petId = 1,
            previousState = tapped,
            transition = PetTransition(tapped.copy(action = PetAction.TALK))
        )

        val directive = shown.single() as PetSpeechDirective.Show
        assertEquals(PetSpeechTone.AFFECTION, directive.line.tone)
    }

    @Test
    fun `combo started effect cannot show text over a non talk frame`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)

        val directives = director.onTransition(
            petId = 1,
            previousState = idle,
            transition = PetTransition(
                state = idle.copy(activeComboId = PetComboId.WALL_PARKOUR),
                effects = listOf(PetEffect.ComboStarted(PetComboId.WALL_PARKOUR))
            )
        )

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `direct talk pose uses chatter and triggers only once while active`() {
        val director = PetSpeechDirector(catalog)
        val walking = state(PetAction.WALK)
        val talking = walking.copy(action = PetAction.TALK)

        val first = director.onTransition(2, walking, PetTransition(talking))
        val repeated = director.onTransition(2, talking, PetTransition(talking))

        assertEquals(
            PetSpeechTone.CHATTER,
            (first.single() as PetSpeechDirective.Show).line.tone
        )
        assertTrue(repeated.isEmpty())
    }

    @Test
    fun `speech closes as soon as pet leaves talk pose`() {
        val director = PetSpeechDirector(catalog)
        val walking = state(PetAction.WALK)
        val talking = walking.copy(action = PetAction.TALK)

        director.onTransition(2, walking, PetTransition(talking))
        val directives = director.onTransition(
            2,
            talking,
            PetTransition(talking.copy(action = PetAction.WINK))
        )

        assertEquals(listOf(PetSpeechDirective.Hide(2)), directives)
    }

    @Test
    fun `moving talk shares one bubble lifecycle with stationary talk`() {
        val director = PetSpeechDirector(catalog)
        val walking = state(PetAction.WALK)
        val movingTalk = walking.copy(
            action = PetAction.TALK_WALK,
            activeComboId = PetComboId.CURIOUS_SCOUT
        )
        val stationaryTalk = movingTalk.copy(action = PetAction.TALK)

        val shown = director.onTransition(2, walking, PetTransition(movingTalk))
        val poseChanged = director.onTransition(
            2,
            movingTalk,
            PetTransition(stationaryTalk)
        )
        val hidden = director.onTransition(
            2,
            stationaryTalk,
            PetTransition(stationaryTalk.copy(action = PetAction.WINK))
        )

        assertEquals(
            PetSpeechTone.CHATTER,
            (shown.single() as PetSpeechDirective.Show).line.tone
        )
        assertTrue(poseChanged.isEmpty())
        assertEquals(listOf(PetSpeechDirective.Hide(2)), hidden)
    }

    @Test
    fun `bubble lifecycle matches the complete engine talk beat`() {
        val director = PetSpeechDirector(catalog)
        val engine = PetEngine(PetEngineConfig(maxTickMillis = 100L))
        var current = engine.initialState(
            bounds = PetBounds(0f, 0f, 1_000f, 2_000f),
            size = PetSize(100f, 100f),
            position = PetVector(400f, 1_900f)
        )
        val tapped = engine.reduce(current, PetEvent.Tap)
        director.onTransition(1, current, tapped)
        current = tapped.state
        var showCount = 0
        var hideCount = 0
        var talkTickCount = 0
        var totalTickCount = 0

        while (hideCount == 0 && totalTickCount < 200) {
            val previous = current
            val transition = engine.reduce(previous, PetEvent.Tick(100L))
            val directives = director.onTransition(1, previous, transition)
            directives.forEach { directive ->
                when (directive) {
                    is PetSpeechDirective.Show -> {
                        assertEquals(PetAction.TALK, transition.state.action)
                        showCount += 1
                    }

                    is PetSpeechDirective.Hide -> {
                        assertEquals(PetAction.TALK, previous.action)
                        assertTrue(transition.state.action != PetAction.TALK)
                        hideCount += 1
                    }
                }
            }
            current = transition.state
            if (current.action == PetAction.TALK) talkTickCount += 1
            totalTickCount += 1
        }

        assertEquals(1, showCount)
        assertEquals(1, hideCount)
        assertTrue(talkTickCount in 90..110)
    }

    @Test
    fun `two talking pets receive independent speech sessions`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)
        val ambientTalk = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.CHATTER
        )
        val userTalk = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.USER_AFFECTION
        )

        val ambientShown = director.onTransition(
            1,
            idle,
            PetTransition(ambientTalk)
        )
        val userShown = director.onTransition(
            2,
            idle,
            PetTransition(userTalk)
        )

        assertEquals(1, ambientShown.size)
        assertEquals(1, userShown.size)
        assertEquals(1, (ambientShown.single() as PetSpeechDirective.Show).petId)
        assertEquals(2, (userShown.single() as PetSpeechDirective.Show).petId)

        val ambientEnded = director.onTransition(
            1,
            ambientTalk,
            PetTransition(ambientTalk.copy(action = PetAction.WINK))
        )

        assertEquals(listOf(PetSpeechDirective.Hide(1)), ambientEnded)
        assertTrue(
            director.onTransition(2, userTalk, PetTransition(userTalk)).isEmpty()
        )
    }

    @Test
    fun `ending one speech session never closes another pet bubble`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)
        val greeting = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.SOCIAL_HELLO
        )
        val reply = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.SOCIAL_HELLO_REPLY
        )

        director.onTransition(1, idle, PetTransition(greeting))
        assertEquals(
            2,
            (director.onTransition(2, idle, PetTransition(reply))
                .single() as PetSpeechDirective.Show).petId
        )

        val replyEnded = director.onTransition(
            2,
            reply,
            PetTransition(reply.copy(action = PetAction.WINK))
        )

        assertEquals(listOf(PetSpeechDirective.Hide(2)), replyEnded)
        assertTrue(
            director.onTransition(1, greeting, PetTransition(greeting)).isEmpty()
        )
    }

    @Test
    fun `every speaking combo resolves its intended tone only on talk pose`() {
        val expected = mapOf(
            PetComboId.USER_AFFECTION to PetSpeechTone.AFFECTION,
            PetComboId.USER_SHOWCASE to PetSpeechTone.CELEBRATION,
            PetComboId.SOCIAL_HELLO to PetSpeechTone.SOCIAL_HELLO,
            PetComboId.SOCIAL_HELLO_REPLY to PetSpeechTone.SOCIAL_REPLY,
            PetComboId.CURIOUS_SCOUT to PetSpeechTone.CHATTER,
            PetComboId.MAGIC_RITUAL to PetSpeechTone.SKILL,
            PetComboId.SOCIAL_SHOW_OFF to PetSpeechTone.CELEBRATION
        )

        expected.forEach { (comboId, tone) ->
            val director = PetSpeechDirector(catalog)
            val idle = state(PetAction.IDLE)
            val speechAction = PetComboCatalog.definition(comboId)
                ?.beats
                ?.single { it.action.isSpeechAction }
                ?.action
                ?: error("$comboId has no speech action")
            val talking = idle.copy(action = speechAction, activeComboId = comboId)

            val directive = director.onTransition(1, idle, PetTransition(talking))
                .single() as PetSpeechDirective.Show

            assertEquals(comboId.name, tone, directive.line.tone)
        }
    }

    @Test
    fun `physical only combos stay silent even if talk is forced`() {
        val silentCombos = PetComboId.entries.toSet() - PetComboSpeechPolicy.speakingComboIds
        val idle = state(PetAction.IDLE)

        silentCombos.forEach { comboId ->
            val talking = idle.copy(action = PetAction.TALK, activeComboId = comboId)
            assertTrue(
                comboId.name,
                PetSpeechDirector(catalog)
                    .onTransition(1, idle, PetTransition(talking))
                    .isEmpty()
            )
        }
    }

    @Test
    fun `catalog speech beats exactly match speaking policy and remain readable`() {
        PetComboId.entries.forEach { comboId ->
            val speechBeats = PetComboCatalog.definition(comboId)
                ?.beats
                .orEmpty()
                .filter { it.action.isSpeechAction }

            if (comboId in PetComboSpeechPolicy.speakingComboIds) {
                assertEquals(comboId.name, 1, speechBeats.size)
                assertEquals(
                    comboId.name,
                    9_000L..11_000L,
                    speechBeats.single().durationMillis
                )
            } else {
                assertTrue(comboId.name, speechBeats.isEmpty())
            }
        }
    }

    @Test
    fun `random selection avoids immediately repeating a custom chatter line`() {
        val customCatalog = PetSpeechCatalog(
            PetSpeechTone.entries.associateWith { tone ->
                if (tone == PetSpeechTone.CHATTER) listOf("Custom one", "Custom two") else emptyList()
            }
        )
        val director = PetSpeechDirector(customCatalog, seed = 2)
        val walking = state(PetAction.WALK)
        val talking = walking.copy(action = PetAction.TALK)

        val first = director.onTransition(1, walking, PetTransition(talking))
            .single() as PetSpeechDirective.Show
        director.onTransition(
            1,
            talking,
            PetTransition(talking.copy(action = PetAction.IDLE))
        )
        val second = director.onTransition(2, walking, PetTransition(talking))
            .single() as PetSpeechDirective.Show

        assertTrue(first.line.text in setOf("Custom one", "Custom two"))
        assertTrue(first.line.text != second.line.text)
    }

    @Test
    fun `social greeting and reply can render at the same time`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)
        val greetingState = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.SOCIAL_HELLO
        )
        val replyState = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.SOCIAL_HELLO_REPLY
        )

        val greeting = director.onTransition(1, idle, PetTransition(greetingState))
        val reply = director.onTransition(2, idle, PetTransition(replyState))

        assertEquals(
            PetSpeechTone.SOCIAL_HELLO,
            (greeting.single() as PetSpeechDirective.Show).line.tone
        )
        assertEquals(
            PetSpeechTone.SOCIAL_REPLY,
            (reply.single() as PetSpeechDirective.Show).line.tone
        )
        val greetingEnded = director.onTransition(
            1,
            greetingState,
            PetTransition(greetingState.copy(action = PetAction.WINK))
        )
        assertEquals(listOf(PetSpeechDirective.Hide(1)), greetingEnded)
        assertTrue(
            director.onTransition(2, replyState, PetTransition(replyState)).isEmpty()
        )
    }

    @Test
    fun `dragging a speaking pet removes only its own bubble`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)
        val talking = idle.copy(
            action = PetAction.TALK,
            activeComboId = PetComboId.USER_AFFECTION
        )
        director.onTransition(4, idle, PetTransition(talking))

        val directives = director.onTransition(
            4,
            talking,
            PetTransition(talking.copy(action = PetAction.DRAGGED))
        )

        assertEquals(listOf(PetSpeechDirective.Hide(4)), directives)
    }

    private fun state(action: PetAction) = PetState(
        position = PetVector.Zero,
        velocity = PetVector.Zero,
        size = PetSize(100f, 100f),
        bounds = PetBounds(0f, 0f, 1_000f, 2_000f),
        action = action,
        direction = PetDirection.RIGHT,
        animationCursor = PetAnimationCursor()
    )
}
