@file:Suppress("DEPRECATION")

package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BatteryDeviceState(
    val wifiConnected: Boolean = false,
    val cellularConnected: Boolean = false,
    val signalLevel: Int = 0,
    val airplaneMode: Boolean = false,
    val hotspotEnabled: Boolean = false,
    val ringerMuted: Boolean = false
)

data class BatteryAnimatedAsset(
    val movie: Movie? = null,
    val lottieComposition: LottieComposition? = null
)

class BatteryStatusBarView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val destination = RectF()
    private val clipPath = Path()
    private val drawableCache = mutableMapOf<String, Drawable?>()
    private var config = BatteryStatusConfig()
    private var deviceState = BatteryDeviceState()
    private var level = 100
    private var charging = false
    private var emoji: Bitmap? = null
    private var battery: Bitmap? = null
    private var background: Bitmap? = null
    private var emotion: Bitmap? = null
    private var animation: BatteryAnimatedAsset? = null
    private var animationStartedAt = SystemClock.uptimeMillis()
    private val lottieDrawable = LottieDrawable().apply {
        repeatCount = LottieDrawable.INFINITE
        callback = this@BatteryStatusBarView
    }

    fun render(
        config: BatteryStatusConfig,
        deviceState: BatteryDeviceState,
        level: Int,
        charging: Boolean,
        emoji: Bitmap?,
        battery: Bitmap?,
        background: Bitmap?,
        emotion: Bitmap?,
        animation: BatteryAnimatedAsset?
    ) {
        this.config = config
        this.deviceState = deviceState
        this.level = level.coerceIn(0, 100)
        this.charging = charging
        this.emoji = emoji
        this.battery = battery
        this.background = background
        this.emotion = emotion
        if (this.animation !== animation) {
            this.animation = animation
            animationStartedAt = SystemClock.uptimeMillis()
            lottieDrawable.cancelAnimation()
            lottieDrawable.composition = animation?.lottieComposition
            if (config.showAnimation && animation?.lottieComposition != null) {
                lottieDrawable.playAnimation()
            }
        }
        contentDescription = context.getString(R.string.battery_overlay_description, this.level)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f
        val privacyReserve = config.privacyReserveDp * density
        val backgroundRight = (width - privacyReserve).coerceAtLeast(height.toFloat())
        drawBackground(canvas, backgroundRight, radius)

        val centerY = height / 2f
        val leftPadding = config.leftPaddingDp * density
        val rightPadding = config.rightPaddingDp * density
        var left = leftPadding
        val gap = 4f * density
        val maxLeft = backgroundRight * 0.53f

        if (config.showTime) {
            left = drawTextFromLeft(
                canvas,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),
                left,
                centerY,
                config.barHeightDp * 0.42f,
                config.foregroundColorArgb,
                Typeface.DEFAULT_BOLD
            ) + gap
        }
        if (config.showDateTime && left < maxLeft) {
            val formatter = SimpleDateFormat(config.dateFormat.pattern, Locale.getDefault())
            left = drawTextFromLeft(
                canvas,
                formatter.format(Date()),
                left,
                centerY,
                config.dateTimeSizeDp,
                config.dateTimeColorArgb,
                dateTypeface()
            ) + gap
        }
        if (deviceState.airplaneMode && left < maxLeft) {
            left = drawStatusIcon(
                canvas, "ic_air_plane", left, centerY,
                config.airplaneSizeDp, config.airplaneColorArgb, fromLeft = true
            ) + gap
        }
        if (deviceState.ringerMuted && left < maxLeft) {
            left = drawStatusIcon(
                canvas, "ic_ringer0", left, centerY,
                config.ringerSizeDp, config.ringerColorArgb, fromLeft = true
            ) + gap
        }
        if (config.showAnimation && left < maxLeft) {
            left = drawAnimation(canvas, left, centerY) + gap
        }
        emoji?.let {
            val size = config.emojiSizeDp * density
            destination.set(left, centerY - size / 2f, left + size, centerY + size / 2f)
            canvas.drawBitmap(it, null, destination, paint)
            left += size + gap
        }
        if (config.showEmotion && left < maxLeft) {
            emotion?.let {
                val size = config.emojiSizeDp * density
                destination.set(left, centerY - size / 2f, left + size, centerY + size / 2f)
                canvas.drawBitmap(it, null, destination, paint)
            }
        }

        var right = backgroundRight - rightPadding
        if (charging) {
            right = drawStatusIcon(
                canvas,
                "charge_%02d".format(config.chargeIconIndex),
                right,
                centerY,
                config.chargeSizeDp,
                config.chargeColorArgb,
                fromLeft = false
            ) - gap
        }
        battery?.let {
            val size = config.batterySizeDp * density
            destination.set(right - size, centerY - size / 2f, right, centerY + size / 2f)
            canvas.drawBitmap(it, null, destination, paint)
            right -= size + gap
        } ?: run {
            drawBuiltInBattery(canvas, right, centerY)
            right -= config.batterySizeDp * density + gap
        }
        if (config.showPercentage) {
            right = drawTextFromRight(
                canvas,
                context.getString(
                    if (charging) R.string.battery_overlay_charging_percentage
                    else R.string.battery_overlay_percentage,
                    level
                ),
                right,
                centerY,
                config.percentSizeDp,
                config.percentColorArgb
            ) - gap
        }
        right = drawStatusIcon(
            canvas,
            if (deviceState.wifiConnected) "ic_wifi" else "ic_wifi0",
            right,
            centerY,
            config.wifiSizeDp,
            config.wifiColorArgb,
            fromLeft = false
        ) - gap
        if (deviceState.cellularConnected && !deviceState.airplaneMode) {
            right = drawTextFromRight(
                canvas,
                config.dataType.label,
                right,
                centerY,
                config.dataSizeDp,
                config.dataColorArgb
            ) - gap
            right = drawStatusIcon(
                canvas, "ic_signal", right, centerY,
                config.signalSizeDp, config.signalColorArgb, fromLeft = false
            ) - gap
        }
        if (deviceState.hotspotEnabled) {
            drawStatusIcon(
                canvas, "ic_hostpot", right, centerY,
                config.hotspotSizeDp, config.hotspotColorArgb, fromLeft = false
            )
        }
    }

    private fun drawBackground(canvas: Canvas, backgroundRight: Float, radius: Float) {
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
    }

    private fun drawAnimation(canvas: Canvas, left: Float, centerY: Float): Float {
        val size = config.animationSizeDp * density
        val rect = RectF(left, centerY - size / 2f, left + size, centerY + size / 2f)
        animation?.lottieComposition?.let {
            lottieDrawable.bounds = Rect(
                rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()
            )
            lottieDrawable.draw(canvas)
            return rect.right
        }
        animation?.movie?.let { movie ->
            val duration = movie.duration().takeIf { it > 0 } ?: 1000
            movie.setTime(((SystemClock.uptimeMillis() - animationStartedAt) % duration).toInt())
            val save = canvas.save()
            canvas.translate(rect.left, rect.top)
            canvas.scale(size / movie.width().coerceAtLeast(1), size / movie.height().coerceAtLeast(1))
            movie.draw(canvas, 0f, 0f)
            canvas.restoreToCount(save)
            postInvalidateOnAnimation()
        }
        return rect.right
    }

    private fun drawTextFromLeft(
        canvas: Canvas,
        value: String,
        left: Float,
        centerY: Float,
        sizeDp: Float,
        color: Int,
        typeface: Typeface = Typeface.DEFAULT_BOLD
    ): Float {
        prepareText(sizeDp, color, typeface, Paint.Align.LEFT)
        canvas.drawText(value, left, textBaseline(centerY), paint)
        return left + paint.measureText(value)
    }

    private fun drawTextFromRight(
        canvas: Canvas,
        value: String,
        right: Float,
        centerY: Float,
        sizeDp: Float,
        color: Int
    ): Float {
        prepareText(sizeDp, color, Typeface.DEFAULT_BOLD, Paint.Align.RIGHT)
        canvas.drawText(value, right, textBaseline(centerY), paint)
        return right - paint.measureText(value)
    }

    private fun prepareText(sizeDp: Float, color: Int, typeface: Typeface, align: Paint.Align) {
        paint.style = Paint.Style.FILL
        paint.textSize = sizeDp * density
        paint.color = color
        paint.typeface = typeface
        paint.textAlign = align
    }

    private fun textBaseline(centerY: Float): Float =
        centerY - (paint.descent() + paint.ascent()) / 2f

    private fun drawStatusIcon(
        canvas: Canvas,
        name: String,
        anchor: Float,
        centerY: Float,
        sizeDp: Float,
        color: Int,
        fromLeft: Boolean
    ): Float {
        val size = sizeDp * density
        val left = if (fromLeft) anchor else anchor - size
        val drawable = drawableCache.getOrPut(name) {
            val id = resources.getIdentifier(name, "drawable", context.packageName)
            if (id == 0) null else ResourcesCompat.getDrawable(resources, id, context.theme)
        }
        if (drawable != null) {
            val tinted = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(tinted, color)
            tinted.setBounds(
                left.toInt(),
                (centerY - size / 2f).toInt(),
                (left + size).toInt(),
                (centerY + size / 2f).toInt()
            )
            tinted.draw(canvas)
        } else {
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawCircle(left + size / 2f, centerY, size / 3f, paint)
        }
        return if (fromLeft) left + size else left
    }

    private fun dateTypeface(): Typeface {
        val id = resources.getIdentifier(
            config.dateTimeFont.resourceName,
            "font",
            context.packageName
        )
        return if (id == 0) Typeface.DEFAULT_BOLD
        else ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT_BOLD
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
        canvas.drawRect(
            right - 3f * density,
            centerY - 3f * density,
            right,
            centerY + 3f * density,
            paint
        )
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
    }

    override fun onDetachedFromWindow() {
        lottieDrawable.cancelAnimation()
        lottieDrawable.composition = null
        animation = null
        emoji = null
        battery = null
        background = null
        emotion = null
        super.onDetachedFromWindow()
    }
}
