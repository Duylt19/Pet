package com.asianmobile.privatebrower.pet.speech

class PetMessageListPolicy {
    fun sanitize(messages: List<String>): List<String> {
        val unique = linkedSetOf<String>()
        messages.forEach { raw ->
            val normalized = WHITESPACE.replace(raw, " ").trim()
            if (normalized.isNotEmpty()) {
                unique += normalized.takeCodePoints(MAX_MESSAGE_CODE_POINTS)
            }
        }
        return unique.take(MAX_CUSTOM_MESSAGES)
    }

    fun encode(messages: List<String>): String = sanitize(messages).joinToString("\n")

    fun decode(encoded: String): List<String> = sanitize(encoded.lineSequence().toList())

    private fun String.takeCodePoints(maxCodePoints: Int): String {
        if (codePointCount(0, length) <= maxCodePoints) return this
        return substring(0, offsetByCodePoints(0, maxCodePoints))
    }

    companion object {
        const val MAX_CUSTOM_MESSAGES = 30
        const val MAX_MESSAGE_CODE_POINTS = 120
        private val WHITESPACE = Regex("\\s+")
    }
}
