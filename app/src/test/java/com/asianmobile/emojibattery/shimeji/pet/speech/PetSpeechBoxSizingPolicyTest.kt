package com.asianmobile.emojibattery.shimeji.pet.speech

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSpeechBoxSizingPolicyTest {
    private val constraints = PetSpeechBoxConstraints(
        minimumWidth = 80,
        maximumWidth = 260,
        widthStep = 8,
        minimumHeight = 48,
        maximumHeight = 112,
        horizontalPadding = 14,
        verticalPadding = 10,
        maximumLines = 4,
        minimumAspectRatio = 1.65f
    )

    @Test
    fun `very short text keeps a compact minimum box`() {
        assertEquals(
            PetSpeechBoxSize(width = 80, height = 48),
            resolve("Hi")
        )
    }

    @Test
    fun `single line box follows the measured glyph width`() {
        assertEquals(
            PetSpeechBoxSize(width = 101, height = 48),
            resolve("Hello pet")
        )
    }

    @Test
    fun `explicit line break grows height and keeps the box balanced`() {
        assertEquals(
            PetSpeechBoxSize(width = 96, height = 56),
            resolve("Hello\npet")
        )
    }

    @Test
    fun `long text chooses the smallest balanced box that fits four lines`() {
        val text = "A".repeat(80)
        val size = resolve(text)
        val metrics = measureMonospace(
            text = text,
            contentWidth = size.width - constraints.horizontalPadding * 2
        )

        assertEquals(PetSpeechBoxSize(width = 192, height = 92), size)
        assertTrue(metrics.lineCount <= constraints.maximumLines)
        assertTrue(size.width.toFloat() / size.height >= constraints.minimumAspectRatio)
    }

    @Test
    fun `unfittable text falls back to maximum box for render ellipsis`() {
        assertEquals(
            PetSpeechBoxSize(width = 260, height = 112),
            resolve("A".repeat(500))
        )
    }

    @Test
    fun `narrow viewport clamps the box width`() {
        val narrowConstraints = constraints.copy(
            minimumWidth = 40,
            maximumWidth = 60
        )

        val size = PetSpeechBoxSizingPolicy.resolve(
            constraints = narrowConstraints,
            measureText = { width -> measureMonospace("Hi", width) }
        )

        assertEquals(PetSpeechBoxSize(width = 45, height = 48), size)
        assertTrue(size.width <= narrowConstraints.maximumWidth)
    }

    private fun resolve(text: String): PetSpeechBoxSize =
        PetSpeechBoxSizingPolicy.resolve(
            constraints = constraints,
            measureText = { width -> measureMonospace(text, width) }
        )

    private fun measureMonospace(
        text: String,
        contentWidth: Int
    ): PetSpeechTextMetrics {
        var totalLines = 0
        var maximumUsedWidth = 0
        text.split('\n').forEach { explicitLine ->
            val codePoints = explicitLine.codePointCount(0, explicitLine.length)
            val pixelWidth = codePoints * CHARACTER_WIDTH
            val wrappedLines = max(
                1,
                ceil(pixelWidth.toDouble() / contentWidth.coerceAtLeast(1)).toInt()
            )
            totalLines += wrappedLines
            maximumUsedWidth = max(
                maximumUsedWidth,
                min(contentWidth, pixelWidth)
            )
        }
        return PetSpeechTextMetrics(
            usedWidth = maximumUsedWidth,
            height = totalLines * LINE_HEIGHT,
            lineCount = totalLines
        )
    }

    private companion object {
        const val CHARACTER_WIDTH = 8
        const val LINE_HEIGHT = 18
    }
}
