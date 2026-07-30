package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import java.text.DateFormat
import java.util.Date

class BatteryStatusBarView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val destination = RectF()
    private val clipPath = Path()
    private var config = BatteryStatusConfig()
    private var level = 100
    private var charging = false
    private var emoji: Bitmap? = null
    private var battery: Bitmap? = null
    private var background: Bitmap? = null
    private var emotion: Bitmap? = null

    fun render(
        config: BatteryStatusConfig,
        level: Int,
        charging: Boolean,
        emoji: Bitmap?,
        battery: Bitmap?,
        background: Bitmap?,
        emotion: Bitmap?
    ) {
        this.config = config
        this.level = level.coerceIn(0, 100)
        this.charging = charging
        this.emoji = emoji
        this.battery = battery
        this.background = background
        this.emotion = emotion
        contentDescription = context.getString(
            com.asianmobile.emojibattery.shimeji.R.string.battery_overlay_description,
            this.level
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f
        val privacyReserve = config.privacyReserveDp * density
        val backgroundRight = (width - privacyReserve).coerceAtLeast(height.toFloat())
        paint.color = config.backgroundColorArgb
        canvas.drawRoundRect(0f, 0f, backgroundRight, height.toFloat(), radius, radius, paint)
        background?.let { bitmap ->
            val saveCount = canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(
                RectF(0f, 0f, backgroundRight, height.toFloat()),
                radius,
                radius,
                Path.Direction.CW
            )
            canvas.clipPath(clipPath)
            destination.set(0f, 0f, backgroundRight, height.toFloat())
            canvas.drawBitmap(bitmap, null, destination, paint)
            canvas.restoreToCount(saveCount)
        }

        val centerY = height / 2f
        val padding = config.horizontalPaddingDp * density
        var left = padding
        paint.color = config.foregroundColorArgb
        paint.textSize = (config.barHeightDp * 0.42f) * density
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.LEFT
        val baseline = centerY - (paint.descent() + paint.ascent()) / 2f
        if (config.showTime) {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
            canvas.drawText(time, left, baseline, paint)
            left += paint.measureText(time) + 6f * density
        }
        emoji?.let {
            val size = config.emojiSizeDp * density
            destination.set(left, centerY - size / 2f, left + size, centerY + size / 2f)
            canvas.drawBitmap(it, null, destination, paint)
            left += size + 4f * density
        }
        if (config.showEmotion) {
            emotion?.let {
                val size = config.emojiSizeDp * density
                destination.set(left, centerY - size / 2f, left + size, centerY + size / 2f)
                canvas.drawBitmap(it, null, destination, paint)
            }
        }

        var right = backgroundRight - padding
        battery?.let {
            val size = config.batterySizeDp * density
            destination.set(right - size, centerY - size / 2f, right, centerY + size / 2f)
            canvas.drawBitmap(it, null, destination, paint)
            right -= size + 5f * density
        } ?: run {
            drawBuiltInBattery(canvas, right, centerY)
            right -= config.batterySizeDp * density + 5f * density
        }
        if (config.showPercentage) {
            paint.textAlign = Paint.Align.RIGHT
            val value = context.getString(
                if (charging) {
                    com.asianmobile.emojibattery.shimeji.R.string
                        .battery_overlay_charging_percentage
                } else {
                    com.asianmobile.emojibattery.shimeji.R.string.battery_overlay_percentage
                },
                level
            )
            canvas.drawText(value, right, baseline, paint)
        }
    }

    private fun drawBuiltInBattery(canvas: Canvas, right: Float, centerY: Float) {
        val widthPx = config.batterySizeDp * density
        val heightPx = widthPx * 0.48f
        val left = right - widthPx
        val top = centerY - heightPx / 2f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = config.foregroundColorArgb
        canvas.drawRoundRect(left, top, right - 3f * density, top + heightPx, 3f, 3f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawRect(right - 3f * density, centerY - 3f * density, right, centerY + 3f * density, paint)
        val fillRight = left + (widthPx - 7f * density) * level / 100f
        canvas.drawRoundRect(
            left + 3f * density,
            top + 3f * density,
            fillRight.coerceAtLeast(left + 3f * density),
            top + heightPx - 3f * density,
            2f,
            2f,
            paint
        )
        paint.style = Paint.Style.FILL
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        emoji = null
        battery = null
        background = null
        emotion = null
    }
}
