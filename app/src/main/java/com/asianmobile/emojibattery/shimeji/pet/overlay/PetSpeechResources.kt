package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.content.Context
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechCatalog
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechTone

internal fun Context.petSpeechCatalog(customMessages: List<String>): PetSpeechCatalog {
    if (customMessages.isNotEmpty()) {
        return PetSpeechCatalog(
            PetSpeechTone.entries.associateWith { customMessages }
        )
    }
    return PetSpeechCatalog(
        mapOf(
            PetSpeechTone.AFFECTION to strings(
                R.string.pet_speech_affection_1,
                R.string.pet_speech_affection_2,
                R.string.pet_speech_affection_3,
                R.string.pet_speech_affection_4,
                R.string.pet_speech_affection_5,
                R.string.pet_speech_affection_6,
                R.string.pet_speech_affection_7,
                R.string.pet_speech_affection_8
            ),
            PetSpeechTone.CHATTER to strings(
                R.string.pet_speech_chatter_1,
                R.string.pet_speech_chatter_2,
                R.string.pet_speech_chatter_3,
                R.string.pet_speech_chatter_4,
                R.string.pet_speech_chatter_5,
                R.string.pet_speech_chatter_6,
                R.string.pet_speech_chatter_7,
                R.string.pet_speech_chatter_8
            ),
            PetSpeechTone.SOCIAL_HELLO to strings(
                R.string.pet_speech_social_hello_1,
                R.string.pet_speech_social_hello_2,
                R.string.pet_speech_social_hello_3,
                R.string.pet_speech_social_hello_4,
                R.string.pet_speech_social_hello_5,
                R.string.pet_speech_social_hello_6,
                R.string.pet_speech_social_hello_7,
                R.string.pet_speech_social_hello_8
            ),
            PetSpeechTone.SOCIAL_REPLY to strings(
                R.string.pet_speech_social_reply_1,
                R.string.pet_speech_social_reply_2,
                R.string.pet_speech_social_reply_3,
                R.string.pet_speech_social_reply_4,
                R.string.pet_speech_social_reply_5,
                R.string.pet_speech_social_reply_6,
                R.string.pet_speech_social_reply_7,
                R.string.pet_speech_social_reply_8
            ),
            PetSpeechTone.SKILL to strings(
                R.string.pet_speech_skill_1,
                R.string.pet_speech_skill_2,
                R.string.pet_speech_skill_3,
                R.string.pet_speech_skill_4,
                R.string.pet_speech_skill_5,
                R.string.pet_speech_skill_6,
                R.string.pet_speech_skill_7,
                R.string.pet_speech_skill_8
            ),
            PetSpeechTone.CELEBRATION to strings(
                R.string.pet_speech_celebration_1,
                R.string.pet_speech_celebration_2,
                R.string.pet_speech_celebration_3,
                R.string.pet_speech_celebration_4,
                R.string.pet_speech_celebration_5,
                R.string.pet_speech_celebration_6,
                R.string.pet_speech_celebration_7,
                R.string.pet_speech_celebration_8
            )
        )
    )
}

private fun Context.strings(vararg ids: Int): List<String> = ids.map(::getString)
