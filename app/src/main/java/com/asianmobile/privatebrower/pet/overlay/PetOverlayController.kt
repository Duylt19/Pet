package com.asianmobile.privatebrower.pet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetBounds
import com.asianmobile.privatebrower.pet.engine.PetEngine
import com.asianmobile.privatebrower.pet.engine.PetEvent
import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetState
import com.asianmobile.privatebrower.pet.engine.PetVector
import kotlin.math.roundToInt

internal class PetOverlayController(
    context: Context,
    private val windowManager: WindowManager =
        context.getSystemService(WindowManager::class.java),
    private val choreographer: Choreographer = Choreographer.getInstance(),
    private val engine: PetEngine = PetEngine()
) {
    private val appContext = context.applicationContext
    private val petSizePixels = appContext.dpToPixels(PET_SIZE_DP)
    private var overlayView: PetOverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var state: PetState? = null
    private var isRendering = false
    private var lastTickNanos = 0L

    private val frameCallback: Choreographer.FrameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isRendering) return
                if (lastTickNanos == 0L) lastTickNanos = frameTimeNanos
                val elapsedNanos = frameTimeNanos - lastTickNanos
                if (elapsedNanos >= TARGET_FRAME_NANOS) {
                    lastTickNanos = frameTimeNanos
                    dispatch(PetEvent.Tick(elapsedNanos / NANOS_PER_MILLISECOND))
                }
                choreographer.postFrameCallback(this)
            }
        }

    fun start() {
        if (overlayView != null) return

        val bounds = currentUsableBounds()
        val size = PetSize(petSizePixels.toFloat(), petSizePixels.toFloat())
        val initialPosition = PetVector(
            x = bounds.right - size.width - appContext.dpToPixels(START_MARGIN_DP),
            y = bounds.bottom - size.height - appContext.dpToPixels(START_MARGIN_DP)
        )
        val initialState = engine.initialState(
            bounds = bounds,
            size = size,
            position = initialPosition,
            action = PetAction.WALK
        )
        val view = PetOverlayView(appContext, ::dispatch).apply {
            render(initialState)
        }
        val params = createLayoutParams(initialState)

        windowManager.addView(view, params)
        overlayView = view
        layoutParams = params
        state = initialState
        isRendering = true
        lastTickNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        isRendering = false
        choreographer.removeFrameCallback(frameCallback)
        overlayView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        overlayView = null
        layoutParams = null
        state = null
        lastTickNanos = 0L
    }

    fun onBoundsChanged() {
        val currentState = state ?: return
        render(engine.reduce(currentState, PetEvent.BoundsChanged(currentUsableBounds())).state)
    }

    private fun dispatch(event: PetEvent) {
        val currentState = state ?: return
        render(engine.reduce(currentState, event).state)
    }

    private fun render(updatedState: PetState) {
        val previousState = state
        state = updatedState
        overlayView?.render(updatedState)
        if (previousState?.position == updatedState.position) return

        val params = layoutParams ?: return
        val view = overlayView ?: return
        params.x = updatedState.position.x.roundToInt()
        params.y = updatedState.position.y.roundToInt()
        if (view.isAttachedToWindow) {
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun createLayoutParams(state: PetState) = WindowManager.LayoutParams(
        petSizePixels,
        petSizePixels,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = state.position.x.roundToInt()
        y = state.position.y.roundToInt()
        title = OVERLAY_WINDOW_TITLE
    }

    private fun currentUsableBounds(): PetBounds {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val windowBounds = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return PetBounds(
                left = (windowBounds.left + insets.left).toFloat(),
                top = (windowBounds.top + insets.top).toFloat(),
                right = (windowBounds.right - insets.right).toFloat(),
                bottom = (windowBounds.bottom - insets.bottom).toFloat()
            )
        }

        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val displaySize = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(displaySize)
        return PetBounds(
            left = 0f,
            top = appContext.systemBarSize("status_bar_height").toFloat(),
            right = displaySize.x.toFloat(),
            bottom = (
                displaySize.y - appContext.systemBarSize("navigation_bar_height")
                ).toFloat()
        )
    }

    private fun Context.dpToPixels(dp: Int): Int =
        (dp * resources.displayMetrics.density).roundToInt()

    @Suppress("DiscouragedApi")
    private fun Context.systemBarSize(resourceName: String): Int {
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return if (resourceId == 0) 0 else resources.getDimensionPixelSize(resourceId)
    }

    private companion object {
        const val PET_SIZE_DP = 112
        const val START_MARGIN_DP = 20
        const val OVERLAY_WINDOW_TITLE = "Cute Pet overlay"
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val TARGET_FRAME_NANOS = 33_333_333L
    }
}
