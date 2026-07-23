package com.asianmobile.privatebrower.pet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetDirection
import com.asianmobile.privatebrower.pet.engine.PetEvent
import com.asianmobile.privatebrower.pet.engine.PetGesturePolicy
import com.asianmobile.privatebrower.pet.engine.PetState
import com.asianmobile.privatebrower.pet.engine.PetVector
import com.asianmobile.privatebrower.pet.engine.requiresMirror
import com.asianmobile.privatebrower.pet.pack.PetPackVisual
import kotlin.math.hypot

internal class PetOverlayView(
    context: Context,
    private val visual: PetPackVisual,
    private val onEvent: (PetEvent) -> Unit
) : View(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val minimumFlingVelocity =
        ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()
    private val doubleTapTimeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong()
    private var petState: PetState? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var isDragging = false
    private var lastTapEventTimeMillis: Long? = null
    private var velocityTracker: VelocityTracker? = null

    private val furPaint = fillPaint(R.color.pet_demo_fur)
    private val darkFurPaint = fillPaint(R.color.pet_demo_fur_dark)
    private val muzzlePaint = fillPaint(R.color.pet_demo_muzzle)
    private val earPaint = fillPaint(R.color.pet_demo_ear)
    private val inkPaint = fillPaint(R.color.pet_demo_ink)
    private val shadowPaint = fillPaint(R.color.pet_demo_shadow)
    private val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pet_demo_fur_dark)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 8f
        strokeCap = Paint.Cap.ROUND
    }

    init {
        isClickable = true
        contentDescription = context.getString(R.string.pet_overlay_content_description)
    }

    fun render(state: PetState) {
        petState = state
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val state = petState ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        when (val currentVisual = visual) {
            PetPackVisual.CodeNative -> drawCodeNative(canvas, viewWidth, viewHeight, state)
            is PetPackVisual.Sprite -> drawSpriteVisual(
                canvas = canvas,
                width = viewWidth,
                height = viewHeight,
                state = state,
                visual = currentVisual
            )
        }
    }

    private fun drawCodeNative(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        state: PetState
    ) {
        val saveCount = canvas.save()
        val frameBob = if (state.frameIndex % 2 == 0) 0f else viewHeight * 0.025f
        canvas.translate(0f, frameBob)
        if (state.direction.requiresMirror(PetDirection.RIGHT)) {
            canvas.scale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
        }
        when (state.action) {
            PetAction.TAPPED -> canvas.scale(
                1.06f,
                0.94f,
                viewWidth / 2f,
                viewHeight * 0.72f
            )
            PetAction.DRAGGED -> canvas.rotate(-7f, viewWidth / 2f, viewHeight / 2f)
            PetAction.FLUNG -> canvas.rotate(12f, viewWidth / 2f, viewHeight / 2f)
            PetAction.FALL -> canvas.rotate(6f, viewWidth / 2f, viewHeight / 2f)
            PetAction.BOUNCE,
            PetAction.TRIP -> canvas.scale(1.04f, 0.92f, viewWidth / 2f, viewHeight * 0.75f)
            PetAction.CLIMB_WALL,
            PetAction.CLIMB_DOWN -> canvas.rotate(-4f, viewWidth / 2f, viewHeight / 2f)
            PetAction.CLIMB_CEILING -> canvas.rotate(180f, viewWidth / 2f, viewHeight / 2f)
            PetAction.SIT -> canvas.translate(0f, viewHeight * 0.04f)
            PetAction.LOOK_UP -> canvas.translate(0f, -viewHeight * 0.02f)
            PetAction.DANGLE -> canvas.translate(0f, viewHeight * 0.06f)
            PetAction.JUMP -> canvas.rotate(-8f, viewWidth / 2f, viewHeight / 2f)
            PetAction.WINK,
            PetAction.RUN,
            PetAction.CREEP,
            PetAction.TALK,
            PetAction.SPECIAL,
            PetAction.SPECIAL_2,
            PetAction.IDLE,
            PetAction.WALK -> Unit
        }

        drawTail(canvas, viewWidth, viewHeight, state.frameIndex)
        drawBody(canvas, viewWidth, viewHeight)
        drawHead(canvas, viewWidth, viewHeight, state)
        canvas.restoreToCount(saveCount)
    }

    private fun drawSpriteVisual(
        canvas: Canvas,
        width: Float,
        height: Float,
        state: PetState,
        visual: PetPackVisual.Sprite
    ) {
        val saveCount = canvas.save()
        applySpriteMotion(canvas, width, height, state)
        if (state.direction.requiresMirror(PetDirection.LEFT)) {
            canvas.scale(-1f, 1f, width / 2f, height / 2f)
        }
        drawSprite(canvas, width, height, state, visual)
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        addRawMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val distanceFromDown = hypot(event.rawX - downRawX, event.rawY - downRawY)
                if (!isDragging && distanceFromDown > touchSlop) {
                    isDragging = true
                    lastTapEventTimeMillis = null
                    onEvent(PetEvent.DragStart)
                }
                if (isDragging) {
                    onEvent(
                        PetEvent.DragBy(
                            PetVector(
                                x = event.rawX - lastRawX,
                                y = event.rawY - lastRawY
                            )
                        )
                    )
                }
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(MILLIS_PER_SECOND)
                    val velocity = PetVector(
                        x = velocityTracker?.xVelocity ?: 0f,
                        y = velocityTracker?.yVelocity ?: 0f
                    )
                    onEvent(PetGesturePolicy.releaseEvent(velocity, minimumFlingVelocity))
                } else {
                    performClick()
                    val previousTap = lastTapEventTimeMillis
                    val isDoubleTap = previousTap != null &&
                        event.eventTime - previousTap in 0..doubleTapTimeoutMillis
                    if (isDoubleTap) {
                        lastTapEventTimeMillis = null
                        onEvent(PetEvent.Showcase)
                    } else {
                        lastTapEventTimeMillis = event.eventTime
                        onEvent(PetEvent.Tap)
                    }
                }
                recycleVelocityTracker()
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) onEvent(PetEvent.DragEnd)
                recycleVelocityTracker()
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        recycleVelocityTracker()
        super.onDetachedFromWindow()
    }

    private fun drawBody(canvas: Canvas, width: Float, height: Float) {
        canvas.drawOval(
            width * 0.24f,
            height * 0.87f,
            width * 0.84f,
            height * 0.98f,
            shadowPaint
        )
        canvas.drawOval(
            width * 0.23f,
            height * 0.48f,
            width * 0.82f,
            height * 0.91f,
            furPaint
        )
        canvas.drawOval(
            width * 0.35f,
            height * 0.68f,
            width * 0.71f,
            height * 0.89f,
            muzzlePaint
        )
        canvas.drawOval(
            width * 0.27f,
            height * 0.84f,
            width * 0.43f,
            height * 0.96f,
            darkFurPaint
        )
        canvas.drawOval(
            width * 0.62f,
            height * 0.84f,
            width * 0.78f,
            height * 0.96f,
            darkFurPaint
        )
    }

    private fun drawHead(canvas: Canvas, width: Float, height: Float, state: PetState) {
        val leftEar = Path().apply {
            moveTo(width * 0.26f, height * 0.30f)
            lineTo(width * 0.28f, height * 0.08f)
            lineTo(width * 0.45f, height * 0.24f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(width * 0.58f, height * 0.22f)
            lineTo(width * 0.78f, height * 0.07f)
            lineTo(width * 0.76f, height * 0.33f)
            close()
        }
        canvas.drawPath(leftEar, furPaint)
        canvas.drawPath(rightEar, furPaint)

        val leftEarInner = Path().apply {
            moveTo(width * 0.30f, height * 0.25f)
            lineTo(width * 0.31f, height * 0.14f)
            lineTo(width * 0.40f, height * 0.24f)
            close()
        }
        val rightEarInner = Path().apply {
            moveTo(width * 0.63f, height * 0.23f)
            lineTo(width * 0.74f, height * 0.14f)
            lineTo(width * 0.72f, height * 0.28f)
            close()
        }
        canvas.drawPath(leftEarInner, earPaint)
        canvas.drawPath(rightEarInner, earPaint)
        canvas.drawOval(
            width * 0.22f,
            height * 0.19f,
            width * 0.80f,
            height * 0.66f,
            furPaint
        )

        val eyesClosed = state.action == PetAction.TAPPED ||
            state.action == PetAction.WINK ||
            (state.action == PetAction.IDLE && state.frameIndex == 3)
        if (eyesClosed) {
            inkPaint.strokeWidth = width * 0.025f
            inkPaint.style = Paint.Style.STROKE
            canvas.drawLine(width * 0.36f, height * 0.40f, width * 0.43f, height * 0.40f, inkPaint)
            canvas.drawLine(width * 0.59f, height * 0.40f, width * 0.66f, height * 0.40f, inkPaint)
            inkPaint.style = Paint.Style.FILL
        } else {
            canvas.drawCircle(width * 0.40f, height * 0.40f, width * 0.027f, inkPaint)
            canvas.drawCircle(width * 0.62f, height * 0.40f, width * 0.027f, inkPaint)
        }
        canvas.drawOval(
            width * 0.43f,
            height * 0.43f,
            width * 0.60f,
            height * 0.57f,
            muzzlePaint
        )
        val nose = Path().apply {
            moveTo(width * 0.47f, height * 0.47f)
            lineTo(width * 0.55f, height * 0.47f)
            lineTo(width * 0.51f, height * 0.52f)
            close()
        }
        canvas.drawPath(nose, inkPaint)
    }

    private fun drawTail(canvas: Canvas, width: Float, height: Float, frameIndex: Int) {
        val lift = if (frameIndex % 2 == 0) 0.44f else 0.36f
        val path = Path().apply {
            moveTo(width * 0.76f, height * 0.70f)
            cubicTo(
                width * 0.98f,
                height * 0.72f,
                width * 0.96f,
                height * lift,
                width * 0.86f,
                height * lift
            )
        }
        canvas.drawPath(path, tailPaint)
    }

    private fun drawSprite(
        canvas: Canvas,
        width: Float,
        height: Float,
        state: PetState,
        visual: PetPackVisual.Sprite
    ) {
        val fallbackAction = when (state.action) {
            PetAction.WALK,
            PetAction.RUN,
            PetAction.CREEP,
            PetAction.CLIMB_WALL,
            PetAction.CLIMB_DOWN,
            PetAction.CLIMB_CEILING -> PetAction.WALK
            else -> PetAction.IDLE
        }
        val clipFrames = visual.frames[state.action]
            ?: visual.frames[fallbackAction]
            ?: visual.frames[PetAction.IDLE]
            ?: return
        if (clipFrames.isEmpty()) return
        val frame = clipFrames[state.frameIndex.coerceIn(0, clipFrames.lastIndex)]
        val fitScale = minOf(
            width / visual.canvas.width,
            height / visual.canvas.height
        )
        val drawWidth = visual.canvas.width * fitScale
        val drawHeight = visual.canvas.height * fitScale
        val anchorX = width * 0.5f
        val anchorY = height
        val destination = RectF(
            anchorX - visual.anchor.x * drawWidth,
            anchorY - visual.anchor.y * drawHeight,
            anchorX + (1f - visual.anchor.x) * drawWidth,
            anchorY + (1f - visual.anchor.y) * drawHeight
        )
        canvas.drawBitmap(frame.bitmap, frame.source, destination, null)
    }

    private fun applySpriteMotion(
        canvas: Canvas,
        width: Float,
        height: Float,
        state: PetState
    ) {
        val alternating = if (state.frameIndex % 2 == 0) -1f else 1f
        when (state.action) {
            PetAction.WALK -> canvas.scale(
                1f + alternating * 0.012f,
                1f - alternating * 0.012f,
                width / 2f,
                height
            )
            PetAction.RUN -> {
                canvas.rotate(alternating * 2.5f, width / 2f, height)
                canvas.scale(1.035f, 0.965f, width / 2f, height)
            }
            PetAction.JUMP -> canvas.rotate(-4f, width / 2f, height)
            PetAction.FALL -> canvas.rotate(4f, width / 2f, height)
            PetAction.BOUNCE,
            PetAction.TRIP -> canvas.scale(1.05f, 0.93f, width / 2f, height)
            PetAction.DRAGGED -> canvas.rotate(alternating * 4f, width / 2f, 0f)
            PetAction.FLUNG -> canvas.rotate(alternating * 7f, width / 2f, height / 2f)
            PetAction.SPECIAL,
            PetAction.SPECIAL_2 -> canvas.scale(
                1f + alternating * 0.018f,
                1f + alternating * 0.018f,
                width / 2f,
                height
            )
            else -> Unit
        }
    }

    private fun addRawMovement(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            recycleVelocityTracker()
            velocityTracker = VelocityTracker.obtain()
        }
        val rawEvent = MotionEvent.obtain(event)
        rawEvent.setLocation(event.rawX, event.rawY)
        velocityTracker?.addMovement(rawEvent)
        rawEvent.recycle()
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun fillPaint(colorResource: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, colorResource)
        style = Paint.Style.FILL
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000
    }
}
