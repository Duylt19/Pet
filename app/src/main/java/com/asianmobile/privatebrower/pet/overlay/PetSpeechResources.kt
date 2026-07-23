package com.asianmobile.privatebrower.pet.overlay

import android.content.Context
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.pet.speech.PetSpeechCatalog
import com.asianmobile.privatebrower.pet.speech.PetSpeechTone

internal fun Context.petSpeechCatalog() = PetSpeechCatalog(
    mapOf(
        PetSpeechTone.AFFECTION to strings(
            R.string.pet_speech_affection_1,
            R.string.pet_speech_affection_2,
            R.string.pet_speech_affection_3,
            R.string.pet_speech_affection_4
        ),
        PetSpeechTone.CHATTER to strings(
            R.string.pet_speech_chatter_1,
            R.string.pet_speech_chatter_2,
            R.string.pet_speech_chatter_3,
            R.string.pet_speech_chatter_4
        ),
        PetSpeechTone.SOCIAL_HELLO to strings(
            R.string.pet_speech_social_hello_1,
            R.string.pet_speech_social_hello_2,
            R.string.pet_speech_social_hello_3,
            R.string.pet_speech_social_hello_4
        ),
        PetSpeechTone.SOCIAL_REPLY to strings(
            R.string.pet_speech_social_reply_1,
            R.string.pet_speech_social_reply_2,
            R.string.pet_speech_social_reply_3,
            R.string.pet_speech_social_reply_4
        ),
        PetSpeechTone.SKILL to strings(
            R.string.pet_speech_skill_1,
            R.string.pet_speech_skill_2,
            R.string.pet_speech_skill_3,
            R.string.pet_speech_skill_4
        ),
        PetSpeechTone.CELEBRATION to strings(
            R.string.pet_speech_celebration_1,
            R.string.pet_speech_celebration_2,
            R.string.pet_speech_celebration_3,
            R.string.pet_speech_celebration_4
        )
    )
)

private fun Context.strings(vararg ids: Int): List<String> = ids.map(::getString)
