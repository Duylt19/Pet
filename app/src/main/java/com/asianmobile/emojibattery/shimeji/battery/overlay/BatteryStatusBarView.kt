@file:Suppress("DEPRECATION")

package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
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

data class BatteryAnimatedAsset(
    val movie: Movie? = null,
    val lottieComposition: LottieComposition? = null
)

class BatteryStatusBarView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val robotoMediumTypeface = ResourcesCompat.getFont(context, R.font.roboto_medium)
        ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val destination = RectF()
    private val drawableCache = mutableMapOf<String, Drawable?>()
    private val layoutPolicy = BatteryStatusLayoutPolicy()
    private var config = BatteryStatusConfig()
    private var deviceState = BatteryDeviceState()
    private var focusedComponent: BatteryStatusComponent? = null
    private var powerState = BatteryPowerState()
    private var emoji: Bitmap? = null
    private var battery: Bitmap? = null
    private var background: Bitmap? = null
    private var emotion: Bitmap? = null
    private var animation: BatteryAnimatedAsset? = null
    private var animationStartedAt = SystemClock.uptimeMillis()
    private var timeText = ""
    private var dateText = ""
    private var percentageText = ""
    private var cachedLayoutWidth = Float.NaN
    private var cachedLayout: BatteryStatusLayoutResult? = null
    private val lottieDrawable = LottieDrawable().apply {
        repeatCount = LottieDrawable.INFINITE
        callback = this@BatteryStatusBarView
    }
    private val lottieCompositionLifecycle = BatteryLottieCompositionLifecycle<LottieComposition>(
        cancelAnimation = lottieDrawable::cancelAnimation,
        clearComposition = lottieDrawable::clearComposition,
        setComposition = { composition -> lottieDrawable.setComposition(composition) },
        playAnimation = lottieDrawable::playAnimation,
        isAnimating = lottieDrawable::isAnimating
    )

    fun render(
        config: BatteryStatusConfig,
        deviceState: BatteryDeviceState,
        focusedComponent: BatteryStatusComponent?,
        powerState: BatteryPowerState,
        emoji: Bitmap?,
        battery: Bitmap?,
        background: Bitmap?,
        emotion: Bitmap?,
        animation: BatteryAnimatedAsset?
    ) {
        this.config = config
        this.deviceState = deviceState
        this.focusedComponent = focusedComponent
        this.powerState = powerState.copy(level = powerState.level.coerceIn(0, 100))
        this.emoji = emoji
        this.battery = battery
        this.background = background
        this.emotion = emotion
        timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
        dateText = if (config.showDateTime) {
            SimpleDateFormat(config.dateFormat.pattern, Locale.getDefault()).format(Date())
        } else {
            ""
        }
        percentageText = if (this.powerState.present) {
            context.getString(
                R.string.battery_overlay_percentage,
                this.powerState.level
            )
        } else {
            context.getString(R.string.battery_overlay_unavailable_short)
        }
        cachedLayout = null
        if (this.animation !== animation) {
            animationStartedAt = SystemClock.uptimeMillis()
        }
        this.animation = animation
        lottieCompositionLifecycle.update(
            composition = animation?.lottieComposition,
            shouldPlay = config.showAnimation
        )
        contentDescription = buildStatusDescription()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val horizontalBounds = resolveBatteryStatusHorizontalBounds(
            widthPx = width.toFloat()
        )
        val contentRight = horizontalBounds.contentRightPx
        drawBackground(canvas, horizontalBounds.backgroundRightPx)

        val centerY = height / 2f
        val leftPadding = config.leftPaddingDp * density
        val rightPadding = config.rightPaddingDp * density
        val gap = BATTERY_STATUS_COMPONENT_GAP_DP * density
        val availableWidth = contentRight - leftPadding - rightPadding
        val layout = cachedLayout
            ?.takeIf { cachedLayoutWidth == availableWidth }
            ?: resolveLayout(
                availableWidth = availableWidth,
                gap = gap,
                timeText = timeText,
                dateText = dateText,
                percentageText = percentageText
            ).also {
                cachedLayoutWidth = availableWidth
                cachedLayout = it
            }

        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val physicalSides = BatteryStatusPhysicalSides.resolve(isRtl)
        drawLeadingGroup(
            canvas = canvas,
            anchor = if (isRtl) contentRight - rightPadding else leftPadding,
            centerY = centerY,
            fromLeft = physicalSides.leadingFromLeft,
            gap = gap,
            layout = layout,
            timeText = timeText,
            dateText = dateText
        )
        drawTrailingGroup(
            canvas = canvas,
            anchor = if (isRtl) leftPadding else contentRight - rightPadding,
            centerY = centerY,
            fromLeft = physicalSides.trailingFromLeft,
            gap = gap,
            layout = layout,
            percentageText = percentageText
        )
    }

    private fun drawLeadingGroup(
        canvas: Canvas,
        anchor: Float,
        centerY: Float,
        fromLeft: Boolean,
        gap: Float,
        layout: BatteryStatusLayoutResult,
        timeText: String,
        dateText: String
    ) {
        var cursor = anchor
        if (layout.shows(BatteryStatusComponent.TIME)) {
            cursor = drawText(
                canvas,
                timeText,
                cursor,
                centerY,
                config.dateTimeSizeDp,
                config.dateTimeColorArgb,
                dateTypeface(),
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.DATE)) {
            cursor = drawText(
                canvas,
                dateText,
                cursor,
                centerY,
                config.dateTimeSizeDp,
                config.dateTimeColorArgb,
                dateTypeface(),
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.AIRPLANE)) {
            cursor = drawStatusIcon(
                canvas,
                BatterySystemStatusPolicy.airplaneIcon(config.airplaneIconStyleIndex),
                cursor,
                centerY,
                config.airplaneSizeDp,
                config.airplaneColorArgb,
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.RINGER)) {
            BatterySystemStatusPolicy.ringerIcon(
                deviceState.ringer,
                config.ringerIconStyleIndex
            )?.let { icon ->
                cursor = drawStatusIcon(
                    canvas,
                    icon,
                    cursor,
                    centerY,
                    config.ringerSizeDp,
                    config.ringerColorArgb,
                    fromLeft
                ).afterGap(gap, fromLeft)
            }
        }
        if (layout.shows(BatteryStatusComponent.ANIMATION)) {
            cursor = drawAnimation(canvas, cursor, centerY, fromLeft)
                .afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.EMOTION)) {
            emotion?.let {
                drawBitmap(canvas, it, cursor, centerY, config.emojiSizeDp, fromLeft)
            }
        }
    }

    private fun drawTrailingGroup(
        canvas: Canvas,
        anchor: Float,
        centerY: Float,
        fromLeft: Boolean,
        gap: Float,
        layout: BatteryStatusLayoutResult,
        percentageText: String
    ) {
        var cursor = anchor
        if (layout.shows(BatteryStatusComponent.CHARGE)) {
            cursor = drawStatusIcon(
                canvas,
                "charge_%02d".format(config.chargeIconIndex),
                cursor,
                centerY,
                config.chargeSizeDp,
                config.chargeColorArgb,
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.BATTERY)) {
            cursor = drawBatteryPair(canvas, cursor, centerY, fromLeft)
            cursor = cursor.afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.PERCENTAGE)) {
            cursor = drawText(
                canvas,
                percentageText,
                cursor,
                centerY,
                config.percentSizeDp,
                config.percentColorArgb,
                robotoMediumTypeface,
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.WIFI)) {
            cursor = drawStatusIcon(
                canvas,
                BatterySystemStatusPolicy.wifiIcon(
                    deviceState.wifi,
                    config.wifiIconStyleIndex
                ),
                cursor,
                centerY,
                config.wifiSizeDp,
                config.wifiColorArgb,
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.CELLULAR)) {
            cursor = drawText(
                canvas,
                config.dataType.label,
                cursor,
                centerY,
                config.dataSizeDp,
                config.dataColorArgb,
                robotoMediumTypeface,
                fromLeft
            ).afterGap(gap, fromLeft)
            cursor = drawStatusIcon(
                canvas,
                BatterySystemStatusPolicy.cellularIcon(
                    deviceState.cellular,
                    config.signalIconStyleIndex
                ),
                cursor,
                centerY,
                config.signalSizeDp,
                config.signalColorArgb,
                fromLeft
            ).afterGap(gap, fromLeft)
        }
        if (layout.shows(BatteryStatusComponent.HOTSPOT)) {
            BatterySystemStatusPolicy.hotspotIcon(
                deviceState.hotspot,
                config.hotspotIconStyleIndex
            )?.let { icon ->
                drawStatusIcon(
                    canvas,
                    icon,
                    cursor,
                    centerY,
                    config.hotspotSizeDp,
                    config.hotspotColorArgb,
                    fromLeft
                )
            }
        }
    }

    private fun resolveLayout(
        availableWidth: Float,
        gap: Float,
        timeText: String,
        dateText: String,
        percentageText: String
    ): BatteryStatusLayoutResult {
        val items = buildList {
            if (config.showTime) {
                add(
                    layoutItem(
                        BatteryStatusComponent.TIME,
                        measuredTextWidth(
                            timeText,
                            config.dateTimeSizeDp,
                            dateTypeface()
                        ),
                        gap,
                        priority = 100,
                        required = focusedComponent == BatteryStatusComponent.DATE
                    )
                )
            }
            if (config.showDateTime) {
                add(
                    layoutItem(
                        BatteryStatusComponent.DATE,
                        measuredTextWidth(dateText, config.dateTimeSizeDp, dateTypeface()),
                        gap,
                        priority = 20,
                        required = focusedComponent == BatteryStatusComponent.DATE
                    )
                )
            }
            if (deviceState.airplaneMode) {
                add(
                    layoutItem(
                        BatteryStatusComponent.AIRPLANE,
                        config.airplaneSizeDp * density,
                        gap,
                        priority = 65,
                        required = focusedComponent == BatteryStatusComponent.AIRPLANE
                    )
                )
            }
            if (
                BatterySystemStatusPolicy.ringerIcon(
                    deviceState.ringer,
                    config.ringerIconStyleIndex
                ) != null
            ) {
                add(
                    layoutItem(
                        BatteryStatusComponent.RINGER,
                        config.ringerSizeDp * density,
                        gap,
                        priority = 60,
                        required = focusedComponent == BatteryStatusComponent.RINGER
                    )
                )
            }
            if (config.showAnimation && animation != null) {
                add(
                    layoutItem(
                        BatteryStatusComponent.ANIMATION,
                        config.animationSizeDp * density,
                        gap,
                        priority = 40,
                        required = focusedComponent == BatteryStatusComponent.ANIMATION
                    )
                )
            }
            if (config.showEmotion && emotion != null) {
                add(
                    layoutItem(
                        BatteryStatusComponent.EMOTION,
                        config.emojiSizeDp * density,
                        gap,
                        priority = 30
                    )
                )
            }
            if (powerState.isCharging) {
                add(
                    layoutItem(
                        BatteryStatusComponent.CHARGE,
                        config.chargeSizeDp * density,
                        gap,
                        priority = 85,
                        required = focusedComponent == BatteryStatusComponent.CHARGE
                    )
                )
            }
            add(
                layoutItem(
                    BatteryStatusComponent.BATTERY,
                    maxOf(
                        config.batterySizeDp,
                        if (emoji != null) config.emojiSizeDp else 0f
                    ) * density,
                    gap,
                    priority = 110,
                    required = true
                )
            )
            if (config.showPercentage) {
                add(
                    layoutItem(
                        BatteryStatusComponent.PERCENTAGE,
                        measuredTextWidth(
                            percentageText,
                            config.percentSizeDp,
                            robotoMediumTypeface
                        ),
                        gap,
                        priority = 95
                    )
                )
            }
            add(
                layoutItem(
                    BatteryStatusComponent.WIFI,
                    config.wifiSizeDp * density,
                    gap,
                    priority = 90,
                    required = focusedComponent == BatteryStatusComponent.WIFI
                )
            )
            if (
                deviceState.cellular in setOf(
                    BatteryConnectivityState.CONNECTED,
                    BatteryConnectivityState.LIMITED
                ) &&
                !deviceState.airplaneMode
            ) {
                val cellularWidth = config.signalSizeDp * density +
                    measuredTextWidth(
                        config.dataType.label,
                        config.dataSizeDp,
                        robotoMediumTypeface
                    ) + gap
                add(
                    layoutItem(
                        BatteryStatusComponent.CELLULAR,
                        cellularWidth,
                        gap,
                        priority = 70,
                        required = focusedComponent == BatteryStatusComponent.CELLULAR
                    )
                )
            }
            if (
                BatterySystemStatusPolicy.hotspotIcon(
                    deviceState.hotspot,
                    config.hotspotIconStyleIndex
                ) != null
            ) {
                add(
                    layoutItem(
                        BatteryStatusComponent.HOTSPOT,
                        config.hotspotSizeDp * density,
                        gap,
                        priority = 55,
                        required = focusedComponent == BatteryStatusComponent.HOTSPOT
                    )
                )
            }
        }
        return layoutPolicy.resolve(availableWidth, items)
    }

    private fun layoutItem(
        component: BatteryStatusComponent,
        contentWidth: Float,
        gap: Float,
        priority: Int,
        required: Boolean = false
    ) = BatteryStatusLayoutItem(
        component = component,
        width = contentWidth + gap,
        priority = priority,
        required = required
    )

    private fun measuredTextWidth(value: String, sizeDp: Float, typeface: Typeface): Float {
        prepareText(sizeDp, config.foregroundColorArgb, typeface, Paint.Align.LEFT)
        return paint.measureText(value)
    }

    private fun drawBackground(canvas: Canvas, backgroundRight: Float) {
        paint.color = config.backgroundColorArgb
        canvas.drawRect(0f, 0f, backgroundRight, height.toFloat(), paint)
        background?.let { bitmap ->
            destination.set(0f, 0f, backgroundRight, height.toFloat())
            canvas.drawBitmap(bitmap, null, destination, paint)
        }
    }

    private fun drawAnimation(
        canvas: Canvas,
        anchor: Float,
        centerY: Float,
        fromLeft: Boolean
    ): Float {
        val size = config.animationSizeDp * density
        val left = if (fromLeft) anchor else anchor - size
        val rect = RectF(left, centerY - size / 2f, left + size, centerY + size / 2f)
        animation?.lottieComposition?.let {
            lottieDrawable.bounds = Rect(
                rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()
            )
            lottieDrawable.draw(canvas)
            return if (fromLeft) rect.right else rect.left
        }
        animation?.movie?.let { movie ->
            val duration = movie.duration().takeIf { it > 0 } ?: 1000
            movie.setTime(((SystemClock.uptimeMillis() - animationStartedAt) % duration).toInt())
            val save = canvas.save()
            canvas.translate(rect.left, rect.top)
            canvas.scale(size / movie.width().coerceAtLeast(1), size / movie.height().coerceAtLeast(1))
            movie.draw(canvas, 0f, 0f)
            canvas.restoreToCount(save)
            postInvalidateDelayed(GIF_FRAME_DELAY_MS)
        }
        return if (fromLeft) rect.right else rect.left
    }

    private fun drawBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        anchor: Float,
        centerY: Float,
        sizeDp: Float,
        fromLeft: Boolean
    ): Float {
        val size = sizeDp * density
        val left = if (fromLeft) anchor else anchor - size
        destination.set(left, centerY - size / 2f, left + size, centerY + size / 2f)
        canvas.drawBitmap(bitmap, null, destination, paint)
        return if (fromLeft) left + size else left
    }

    private fun drawBatteryPair(
        canvas: Canvas,
        anchor: Float,
        centerY: Float,
        fromLeft: Boolean
    ): Float {
        val pairSize = maxOf(
            config.batterySizeDp,
            if (emoji != null) config.emojiSizeDp else 0f
        ) * density
        val pairLeft = if (fromLeft) anchor else anchor - pairSize
        val pairCenterX = pairLeft + pairSize / 2f
        battery?.let { bitmap ->
            drawBitmapCentered(
                canvas = canvas,
                bitmap = bitmap,
                centerX = pairCenterX,
                centerY = centerY,
                sizeDp = config.batterySizeDp
            )
        } ?: run {
            val batteryWidth = config.batterySizeDp * density
            drawBuiltInBattery(
                canvas = canvas,
                anchor = pairCenterX - batteryWidth / 2f,
                centerY = centerY,
                fromLeft = true
            )
        }
        emoji?.let { bitmap ->
            drawBitmapCentered(
                canvas = canvas,
                bitmap = bitmap,
                centerX = pairCenterX,
                centerY = centerY,
                sizeDp = config.emojiSizeDp
            )
        }
        return if (fromLeft) pairLeft + pairSize else pairLeft
    }

    private fun drawBitmapCentered(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        sizeDp: Float
    ) {
        val size = sizeDp * density
        destination.set(
            centerX - size / 2f,
            centerY - size / 2f,
            centerX + size / 2f,
            centerY + size / 2f
        )
        canvas.drawBitmap(bitmap, null, destination, paint)
    }

    private fun drawText(
        canvas: Canvas,
        value: String,
        anchor: Float,
        centerY: Float,
        sizeDp: Float,
        color: Int,
        typeface: Typeface,
        fromLeft: Boolean
    ): Float = if (fromLeft) {
        drawTextFromLeft(canvas, value, anchor, centerY, sizeDp, color, typeface)
    } else {
        drawTextFromRight(canvas, value, anchor, centerY, sizeDp, color, typeface)
    }

    private fun Float.afterGap(gap: Float, fromLeft: Boolean): Float =
        if (fromLeft) this + gap else this - gap

    private fun drawTextFromLeft(
        canvas: Canvas,
        value: String,
        left: Float,
        centerY: Float,
        sizeDp: Float,
        color: Int,
        typeface: Typeface = robotoMediumTypeface
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
        color: Int,
        typeface: Typeface = robotoMediumTypeface
    ): Float {
        prepareText(sizeDp, color, typeface, Paint.Align.RIGHT)
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
        return if (id == 0) robotoMediumTypeface
        else ResourcesCompat.getFont(context, id) ?: robotoMediumTypeface
    }

    private fun drawBuiltInBattery(
        canvas: Canvas,
        anchor: Float,
        centerY: Float,
        fromLeft: Boolean
    ): Float {
        val widthPx = config.batterySizeDp * density
        val heightPx = widthPx * 0.48f
        val right = if (fromLeft) anchor + widthPx else anchor
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
        val fillRight = left + (widthPx - 7f * density) * powerState.level / 100f
        canvas.drawRoundRect(
            left + 3f * density,
            top + 3f * density,
            fillRight.coerceAtLeast(left + 3f * density),
            top + heightPx - 3f * density,
            2f,
            2f,
            paint
        )
        return if (fromLeft) right else left
    }

    private fun buildStatusDescription(): String {
        if (!powerState.present) {
            return context.getString(R.string.battery_overlay_unavailable)
        }
        val states = buildList {
            add(
                context.getString(
                    when (powerState.chargeState) {
                        BatteryChargeState.CHARGING ->
                            R.string.battery_overlay_state_charging
                        BatteryChargeState.FULL ->
                            R.string.battery_overlay_state_full
                        BatteryChargeState.NOT_CHARGING ->
                            R.string.battery_overlay_state_not_charging
                        BatteryChargeState.DISCHARGING ->
                            R.string.battery_overlay_state_discharging
                        BatteryChargeState.UNKNOWN ->
                            R.string.battery_overlay_state_unknown
                    }
                )
            )
            val powerSource = when (powerState.plugType) {
                BatteryPlugType.AC -> R.string.battery_overlay_power_ac
                BatteryPlugType.USB -> R.string.battery_overlay_power_usb
                BatteryPlugType.WIRELESS -> R.string.battery_overlay_power_wireless
                BatteryPlugType.DOCK -> R.string.battery_overlay_power_dock
                BatteryPlugType.NONE,
                BatteryPlugType.UNKNOWN -> null
            }
            powerSource?.let { add(context.getString(it)) }
            add(
                context.getString(
                    when (deviceState.wifi) {
                        BatteryConnectivityState.CONNECTED ->
                            R.string.battery_overlay_wifi_connected
                        BatteryConnectivityState.LIMITED ->
                            R.string.battery_overlay_wifi_limited
                        BatteryConnectivityState.DISABLED ->
                            R.string.battery_overlay_wifi_disabled
                        BatteryConnectivityState.DISCONNECTED ->
                            R.string.battery_overlay_wifi_disconnected
                    }
                )
            )
            if (deviceState.airplaneMode) {
                add(context.getString(R.string.battery_overlay_airplane_enabled))
            } else if (
                deviceState.cellular == BatteryConnectivityState.CONNECTED ||
                deviceState.cellular == BatteryConnectivityState.LIMITED
            ) {
                add(
                    context.getString(
                        if (deviceState.cellular == BatteryConnectivityState.CONNECTED) {
                            R.string.battery_overlay_cellular_connected
                        } else {
                            R.string.battery_overlay_cellular_limited
                        }
                    )
                )
            }
            when (deviceState.ringer) {
                BatteryRingerState.VIBRATE ->
                    add(context.getString(R.string.battery_overlay_ringer_vibrate))
                BatteryRingerState.SILENT ->
                    add(context.getString(R.string.battery_overlay_ringer_silent))
                BatteryRingerState.NORMAL -> Unit
            }
            if (deviceState.hotspot == BatteryHotspotState.ENABLED) {
                add(context.getString(R.string.battery_overlay_hotspot_enabled))
            }
        }
        return context.getString(
            R.string.battery_overlay_detailed_description,
            powerState.level,
            states.joinToString(separator = ", ")
        )
    }

    override fun onDetachedFromWindow() {
        lottieCompositionLifecycle.reset()
        animation = null
        emoji = null
        battery = null
        background = null
        emotion = null
        cachedLayout = null
        super.onDetachedFromWindow()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        cachedLayout = null
        super.onRtlPropertiesChanged(layoutDirection)
    }

    private companion object {
        const val GIF_FRAME_DELAY_MS = 66L
    }
}
