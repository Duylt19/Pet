package com.asianmobile.privatebrower.pet.speech

import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetAnimationCursor
import com.asianmobile.privatebrower.pet.engine.PetBounds
import com.asianmobile.privatebrower.pet.engine.PetComboId
import com.asianmobile.privatebrower.pet.engine.PetDirection
import com.asianmobile.privatebrower.pet.engine.PetEffect
import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetState
import com.asianmobile.privatebrower.pet.engine.PetTransition
import com.asianmobile.privatebrower.pet.engine.PetVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSpeechDirectorTest {
    private val catalog = PetSpeechCatalog(
        PetSpeechTone.entries.associateWith { tone -> listOf("${tone.name} one", "${tone.name} two") }
    )

    @Test
    fun `tap starts an immediate readable reaction and later hides it`() {
        val director = PetSpeechDirector(catalog, seed = 3)
        val state = state(PetAction.WALK)
        val shown = director.onTransition(
            petId = 1,
            previousState = state,
            transition = PetTransition(
                state.copy(action = PetAction.TAPPED),
                listOf(PetEffect.Tapped)
            )
        )

        assertEquals(1, shown.size)
        val directive = shown.single() as PetSpeechDirective.Show
        assertEquals(PetSpeechTone.AFFECTION, directive.line.tone)
        assertEquals(PetSpeechAttachment.OVERHEAD, directive.attachment)
        assertTrue(directive.durationMillis >= 4_500L)
        assertTrue(director.advance(2_000).isEmpty())
        assertEquals(listOf(PetSpeechDirective.Hide(1)), director.advance(10_000))
    }

    @Test
    fun `talk pose triggers chatter only once while the action remains active`() {
        val director = PetSpeechDirector(catalog)
        val walking = state(PetAction.WALK)
        val talking = walking.copy(action = PetAction.TALK)

        val first = director.onTransition(2, walking, PetTransition(talking))
        val repeated = director.onTransition(2, talking, PetTransition(talking))

        val directive = first.single() as PetSpeechDirective.Show
        assertEquals(PetSpeechTone.CHATTER, directive.line.tone)
        assertEquals(PetSpeechAttachment.TALK_WINDOW, directive.attachment)
        assertTrue(repeated.isEmpty())
    }

    @Test
    fun `talk window speech closes when pet leaves the carrying pose`() {
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
    fun `queued talk window speech is discarded after carrying pose ends`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)
        director.onTransition(
            1,
            idle,
            PetTransition(idle, listOf(PetEffect.ComboStarted(PetComboId.SOCIAL_HELLO)))
        )
        val talking = idle.copy(action = PetAction.TALK)
        assertTrue(director.onTransition(2, idle, PetTransition(talking)).isEmpty())

        val leftTalk = director.onTransition(
            2,
            talking,
            PetTransition(talking.copy(action = PetAction.WINK))
        )

        assertTrue(leftTalk.isEmpty())
        assertEquals(listOf(PetSpeechDirective.Hide(1)), director.advance(10_000L))
    }

    @Test
    fun `ambient combos expose chatter skill and celebration speech`() {
        val state = state(PetAction.IDLE)

        val chatter = PetSpeechDirector(catalog).onTransition(
            1,
            state,
            PetTransition(state, listOf(PetEffect.ComboStarted(PetComboId.DAYDREAM)))
        )
        val skill = PetSpeechDirector(catalog).onTransition(
            2,
            state,
            PetTransition(state, listOf(PetEffect.ComboStarted(PetComboId.WALL_PARKOUR)))
        )
        val celebration = PetSpeechDirector(catalog).onTransition(
            3,
            state,
            PetTransition(state, listOf(PetEffect.ComboStarted(PetComboId.SOCIAL_DUET_A)))
        )

        assertEquals(PetSpeechTone.CHATTER, (chatter.single() as PetSpeechDirective.Show).line.tone)
        assertEquals(PetSpeechTone.SKILL, (skill.single() as PetSpeechDirective.Show).line.tone)
        assertEquals(
            PetSpeechTone.CELEBRATION,
            (celebration.single() as PetSpeechDirective.Show).line.tone
        )
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
        director.advance(20_000L)
        val second = director.onTransition(2, walking, PetTransition(talking))
            .single() as PetSpeechDirective.Show

        assertTrue(first.line.text in setOf("Custom one", "Custom two"))
        assertTrue(first.line.text != second.line.text)
    }

    @Test
    fun `social reply waits until greeting bubble finishes`() {
        val director = PetSpeechDirector(catalog)
        val state = state(PetAction.IDLE)
        val greeting = director.onTransition(
            1,
            state,
            PetTransition(state, listOf(PetEffect.ComboStarted(PetComboId.SOCIAL_HELLO)))
        )
        val reply = director.onTransition(
            2,
            state,
            PetTransition(state, listOf(PetEffect.ComboStarted(PetComboId.SOCIAL_HELLO_REPLY)))
        )

        assertEquals(PetSpeechTone.SOCIAL_HELLO, (greeting.single() as PetSpeechDirective.Show).line.tone)
        assertTrue(reply.isEmpty())
        val switched = director.advance(10_000)
        assertEquals(PetSpeechDirective.Hide(1), switched.first())
        assertEquals(PetSpeechTone.SOCIAL_REPLY, (switched.last() as PetSpeechDirective.Show).line.tone)
    }

    @Test
    fun `dragging a speaking pet removes its bubble and queued lines`() {
        val director = PetSpeechDirector(catalog)
        val idle = state(PetAction.IDLE)
        director.onTransition(
            4,
            idle,
            PetTransition(idle.copy(action = PetAction.TAPPED), listOf(PetEffect.Tapped))
        )

        val directives = director.onTransition(
            4,
            idle,
            PetTransition(idle.copy(action = PetAction.DRAGGED))
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
