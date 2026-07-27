package com.asianmobile.emojibattery.shimeji.pet.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class PetMessageListPolicyTest {
    private val policy = PetMessageListPolicy()

    @Test
    fun `messages are trimmed flattened deduplicated and empty lines removed`() {
        assertEquals(
            listOf("Hello pet", "Second message"),
            policy.sanitize(
                listOf(
                    "  Hello   pet ",
                    "",
                    "Second\nmessage",
                    "Hello pet"
                )
            )
        )
    }

    @Test
    fun `list and individual message lengths are bounded without splitting emoji`() {
        val oversizedEmoji = "🐾".repeat(PetMessageListPolicy.MAX_MESSAGE_CODE_POINTS + 5)
        val sanitized = policy.sanitize(
            listOf(oversizedEmoji) +
                List(PetMessageListPolicy.MAX_CUSTOM_MESSAGES + 5) { index -> "Message $index" }
        )

        assertEquals(PetMessageListPolicy.MAX_CUSTOM_MESSAGES, sanitized.size)
        assertEquals(
            PetMessageListPolicy.MAX_MESSAGE_CODE_POINTS,
            sanitized.first().codePointCount(0, sanitized.first().length)
        )
        assertEquals("🐾", sanitized.first().takeLast(2))
    }

    @Test
    fun `codec round trips the sanitized list`() {
        val messages = listOf("First message", "Second message")

        assertEquals(messages, policy.decode(policy.encode(messages)))
    }
}
