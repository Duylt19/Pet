package com.asianmobile.privatebrower.pet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import com.asianmobile.privatebrower.data.model.PetPerformanceBudget
import com.asianmobile.privatebrower.data.model.PetPositionFraction
import com.asianmobile.privatebrower.data.model.PetPreferences
import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetBounds
import com.asianmobile.privatebrower.pet.engine.PetEngine
import com.asianmobile.privatebrower.pet.engine.PetEngineConfig
import com.asianmobile.privatebrower.pet.engine.PetEvent
import com.asianmobile.privatebrower.pet.engine.PetSize
import com.asianmobile.privatebrower.pet.engine.PetState
import com.asianmobile.privatebrower.pet.pack.PetPack
import com.asianmobile.privatebrower.pet.pack.PetPackVisual
import com.asianmobile.privatebrower.pet.pack.toEngineClips
import com.asianmobile.privatebrower.pet.settings.PetSessionLayout
import com.asianmobile.privatebrower.pet.settings.PetSettingsPolicy
import kotlin.math.roundToInt

internal class PetOverlayController(
    context: Context,
    private val pack: PetPack,
    private val visual: PetPackVisual,
    private val preferences: PetPreferences,
    performanceBudget: PetPerformanceBudget,
    private val windowManager: WindowManager =
        context.getSystemService(WindowManager::class.java),
    private val choreographer: Choreographer = Choreographer.getInstance()
) {
    private val appContext = context.applicationContext
    private val settingsPolicy = PetSettingsPolicy()
    private val sessionLayout = PetSessionLayout()
    private val petCount = settingsPolicy.sanitizePetCount(
        preferences.petCount,
        performanceBudget.maxPets
    )
    private val targetFrameNanos = NANOS_PER_SECOND / settingsPolicy.targetFramesPerSecond(
        petCount,
        performanceBudget.targetFramesPerSecond
    )
    private val petSizePixels = appContext.dpToPixels(
        (PET_SIZE_DP * pack.manifest.canvas.defaultScale * preferences.sizePercent / 100f)
            .roundToInt()
            .coerceIn(MIN_PET_SIZE_DP, MAX_PET_SIZE_DP)
    )
    private val engineConfig = PetEngineConfig(
        clips = pack.manifest.toEngineClips(preferences.speedPercent / 100f),
        tapAction = pack.manifest.interaction.tapAction
    )
    private val instances = mutableListOf<PetInstance>()
    private var isRendering = false
    private var lastTickNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRendering) return
            if (lastTickNanos == 0L) lastTickNanos = frameTimeNanos
            val elapsedNanos = frameTimeNanos - lastTickNanos
            if (elapsedNanos >= targetFrameNanos) {
                lastTickNanos = frameTimeNanos
                val event = PetEvent.Tick(elapsedNanos / NANOS_PER_MILLISECOND)
                instances.toList().forEach { dispatch(it, event) }
            }
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (instances.isNotEmpty()) return

        val bounds = currentUsableBounds()
        val size = PetSize(petSizePixels.toFloat(), petSizePixels.toFloat())
        val positions = sessionLayout.resolvePositions(
            count = petCount,
            bounds = bounds,
            size = size,
            saved = preferences.lastPositions,
            marginPixels = appContext.dpToPixels(START_MARGIN_DP).toFloat()
        )

        try {
            positions.forEachIndexed { index, position ->
                val engine = PetEngine(engineConfig)
                val initialState = engine.initialState(
                    bounds = bounds,
                    size = size,
                    position = position,
                    action = PetAction.WALK
                )
                lateinit var instance: PetInstance
                val view = PetOverlayView(appContext, visual) { event -> dispatch(instance, event) }
                    .apply { render(initialState) }
                val params = createLayoutParams(initialState, index)
                instance = PetInstance(engine, view, params, initialState)
                windowManager.addView(view, params)
                instances += instance
            }
        } catch (error: RuntimeException) {
            removeAllViews()
            throw error
        }

        isRendering = true
        lastTickNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop(): List<PetPositionFraction> {
        val positions = instances.map { instance ->
            sessionLayout.normalize(
                position = instance.state.position,
                bounds = instance.state.bounds,
                size = instance.state.size
            )
        }
        isRendering = false
        choreographer.removeFrameCallback(frameCallback)
        removeAllViews()
        lastTickNanos = 0L
        return positions
    }

    fun onBoundsChanged() {
        val bounds = currentUsableBounds()
        instances.toList().forEach { instance ->
            render(instance, instance.engine.reduce(instance.state, PetEvent.BoundsChanged(bounds)).state)
        }
    }

    private fun dispatch(instance: PetInstance, event: PetEvent) {
        if (instance !in instances) return
        render(instance, instance.engine.reduce(instance.state, event).state)
    }

    private fun render(instance: PetInstance, updatedState: PetState) {
        val previousPosition = instance.state.position
        instance.state = updatedState
        instance.view.render(updatedState)
        if (previousPosition == updatedState.position) return

        instance.params.x = updatedState.position.x.roundToInt()
        instance.params.y = updatedState.position.y.roundToInt()
        if (instance.view.isAttachedToWindow) {
            windowManager.updateViewLayout(instance.view, instance.params)
        }
    }

    private fun createLayoutParams(state: PetState, index: Int) = WindowManager.LayoutParams(
        petSizePixels,
        petSizePixels,
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            if (preferences.interactionEnabled) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = state.position.x.roundToInt()
        y = state.position.y.roundToInt()
        title = "$OVERLAY_WINDOW_TITLE ${index + 1}"
    }

    private fun removeAllViews() {
        instances.forEach { instance ->
            runCatching { windowManager.removeViewImmediate(instance.view) }
        }
        instances.clear()
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
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
            bottom = (displaySize.y - appContext.systemBarSize("navigation_bar_height")).toFloat()
        )
    }

    private fun Context.dpToPixels(dp: Int): Int =
        (dp * resources.displayMetrics.density).roundToInt()

    @Suppress("DiscouragedApi")
    private fun Context.systemBarSize(resourceName: String): Int {
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return if (resourceId == 0) 0 else resources.getDimensionPixelSize(resourceId)
    }

    private data class PetInstance(
        val engine: PetEngine,
        val view: PetOverlayView,
        val params: WindowManager.LayoutParams,
        var state: PetState
    )

    private companion object {
        const val PET_SIZE_DP = 112
        const val MIN_PET_SIZE_DP = 64
        const val MAX_PET_SIZE_DP = 196
        const val START_MARGIN_DP = 20
        const val OVERLAY_WINDOW_TITLE = "Cute Pet overlay"
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
