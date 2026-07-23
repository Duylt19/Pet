package com.asianmobile.privatebrower.pet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.pet.speech.PetSpeechLine

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
    private var tailAtTop = false
    private var tailCenterX = 0f

    fun render(
        line: PetSpeechLine,
        tailAtTop: Boolean,
        tailCenterX: Float
    ) {
        this.line = line
        this.tailAtTop = tailAtTop
        this.tailCenterX = tailCenterX
        contentDescription = line.text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentLine = line ?: return
        val tailHeight = TAIL_HEIGHT_DP * density
        val horizontalInset = HORIZONTAL_INSET_DP * density
        val bubbleTop = if (tailAtTop) tailHeight else 0f
        val bubbleBottom = if (tailAtTop) height.toFloat() else height - tailHeight
        val bubble = RectF(
            horizontalInset,
            bubbleTop,
            width - horizontalInset,
            bubbleBottom
        )
        val radius = CORNER_RADIUS_DP * density
        canvas.drawRoundRect(bubble, radius, radius, bubblePaint)
        canvas.drawRoundRect(bubble, radius, radius, borderPaint)

        val clampedTailX = tailCenterX.coerceIn(
            bubble.left + radius,
            bubble.right - radius
        )
        val tailHalfWidth = TAIL_HALF_WIDTH_DP * density
        val tail = Path().apply {
            if (tailAtTop) {
                moveTo(clampedTailX - tailHalfWidth, bubble.top + borderPaint.strokeWidth)
                lineTo(clampedTailX, 0f)
                lineTo(clampedTailX + tailHalfWidth, bubble.top + borderPaint.strokeWidth)
            } else {
                moveTo(clampedTailX - tailHalfWidth, bubble.bottom - borderPaint.strokeWidth)
                lineTo(clampedTailX, height.toFloat())
                lineTo(clampedTailX + tailHalfWidth, bubble.bottom - borderPaint.strokeWidth)
            }
            close()
        }
        canvas.drawPath(tail, bubblePaint)
        canvas.drawPath(tail, borderPaint)

        val textInset = TEXT_INSET_DP * density
        val textWidth = (bubble.width() - textInset * 2).toInt().coerceAtLeast(1)
        val textLayout = StaticLayout.Builder
            .obtain(currentLine.text, 0, currentLine.text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(MAX_TEXT_LINES)
            .build()
        val availableTextHeight = bubble.height() - textInset * 2
        val textY = bubble.top + textInset +
            ((availableTextHeight - textLayout.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(bubble.left + textInset, textY)
        textLayout.draw(canvas)
        canvas.restore()
    }

    private companion object {
        const val HORIZONTAL_INSET_DP = 3f
        const val TEXT_INSET_DP = 10f
        const val CORNER_RADIUS_DP = 14f
        const val TAIL_HEIGHT_DP = 10f
        const val TAIL_HALF_WIDTH_DP = 8f
        const val MAX_TEXT_LINES = 3
    }
}
