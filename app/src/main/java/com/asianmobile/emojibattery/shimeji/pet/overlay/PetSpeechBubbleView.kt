package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechBoxConstraints
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechBoxSize
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechBoxSizingPolicy
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechLine
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechTextMetrics
import kotlin.math.ceil
import kotlin.math.roundToInt

internal class PetSpeechBubbleView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colors_161718)
        alpha = 242
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colors_C0D1FE)
        alpha = 220
        style = Paint.Style.STROKE
        strokeWidth = 1.25f * density
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colors_FFFFFF)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            resources.displayMetrics
        )
        textAlign = Paint.Align.LEFT
    }
    private var line: PetSpeechLine? = null

    fun measureBox(
        text: String,
        maximumWidthPixels: Int
    ): PetSpeechBoxSize {
        val defaultMinimumWidth = dp(MINIMUM_WIDTH_DP)
        val safeMaximumWidth = maximumWidthPixels
            .coerceAtLeast(1)
            .coerceAtMost(dp(MAXIMUM_WIDTH_DP))
        val horizontalPadding = dp(HORIZONTAL_TEXT_INSET_DP)
            .coerceAtMost((safeMaximumWidth - 1) / 2)
        val minimumWidth = defaultMinimumWidth
            .coerceAtLeast(horizontalPadding * 2 + 1)
            .coerceAtMost(safeMaximumWidth)
        return PetSpeechBoxSizingPolicy.resolve(
            constraints = PetSpeechBoxConstraints(
                minimumWidth = minimumWidth,
                maximumWidth = safeMaximumWidth,
                widthStep = dp(WIDTH_STEP_DP),
                minimumHeight = dp(MINIMUM_HEIGHT_DP),
                maximumHeight = dp(MAXIMUM_HEIGHT_DP),
                horizontalPadding = horizontalPadding,
                verticalPadding = dp(VERTICAL_TEXT_INSET_DP),
                maximumLines = MAX_TEXT_LINES,
                minimumAspectRatio = MINIMUM_ASPECT_RATIO
            ),
            measureText = { contentWidth -> measureText(text, contentWidth) }
        )
    }

    fun render(line: PetSpeechLine) {
        this.line = line
        contentDescription = line.text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentLine = line ?: return
        val bubble = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRect(bubble, bubblePaint)
        val borderInset = borderPaint.strokeWidth / 2f
        canvas.drawRect(
            RectF(
                borderInset,
                borderInset,
                width - borderInset,
                height - borderInset
            ),
            borderPaint
        )

        val horizontalInset = dp(HORIZONTAL_TEXT_INSET_DP)
            .coerceAtMost((width - 1) / 2)
            .toFloat()
        val verticalInset = dp(VERTICAL_TEXT_INSET_DP).toFloat()
        val textWidth = (bubble.width() - horizontalInset * 2).toInt().coerceAtLeast(1)
        val textLayout = buildTextLayout(
            text = currentLine.text,
            contentWidth = textWidth,
            maximumLines = MAX_TEXT_LINES
        )
        val availableTextHeight = bubble.height() - verticalInset * 2
        val textY = bubble.top + verticalInset +
            ((availableTextHeight - textLayout.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(bubble.left + horizontalInset, textY)
        textLayout.draw(canvas)
        canvas.restore()
    }

    private fun measureText(
        text: String,
        contentWidth: Int
    ): PetSpeechTextMetrics {
        val layout = buildTextLayout(text, contentWidth)
        val usedWidth = (0 until layout.lineCount)
            .maxOfOrNull { lineIndex -> ceil(layout.getLineWidth(lineIndex)).toInt() }
            ?: 0
        return PetSpeechTextMetrics(
            usedWidth = usedWidth.coerceAtMost(contentWidth),
            height = layout.height.coerceAtLeast(1),
            lineCount = layout.lineCount.coerceAtLeast(1)
        )
    }

    private fun buildTextLayout(
        text: String,
        contentWidth: Int,
        maximumLines: Int? = null
    ): StaticLayout {
        val builder = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, contentWidth.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
        if (maximumLines != null) {
            builder
                .setEllipsize(TextUtils.TruncateAt.END)
                .setEllipsizedWidth(contentWidth)
                .setMaxLines(maximumLines)
        }
        return builder.build()
    }

    private fun dp(value: Float): Int = (value * density).roundToInt().coerceAtLeast(1)

    private companion object {
        const val MINIMUM_WIDTH_DP = 80f
        const val MAXIMUM_WIDTH_DP = 260f
        const val WIDTH_STEP_DP = 8f
        const val MINIMUM_HEIGHT_DP = 48f
        const val MAXIMUM_HEIGHT_DP = 112f
        const val HORIZONTAL_TEXT_INSET_DP = 14f
        const val VERTICAL_TEXT_INSET_DP = 10f
        const val MAX_TEXT_LINES = 4
        const val MINIMUM_ASPECT_RATIO = 1.65f
    }
}
