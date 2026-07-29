package com.asianmobile.emojibattery.shimeji.pet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import com.asianmobile.emojibattery.shimeji.data.model.PetPerformanceBudget
import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.data.model.PetPositionFraction
import com.asianmobile.emojibattery.shimeji.data.model.PetPreferences
import com.asianmobile.emojibattery.shimeji.data.model.PetSlotPreferences
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBehaviorProfile
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBehaviorProfiles
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBounds
import com.asianmobile.emojibattery.shimeji.pet.engine.PetCrowdResolver
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngine
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngineConfig
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEvent
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSocialDirective
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSocialDirector
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSocialSnapshot
import com.asianmobile.emojibattery.shimeji.pet.engine.PetState
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackVisual
import com.asianmobile.emojibattery.shimeji.pet.pack.toEngineClips
import com.asianmobile.emojibattery.shimeji.pet.pack.toEngineSupportedActions
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSessionLayout
import com.asianmobile.emojibattery.shimeji.pet.settings.PetSettingsPolicy
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechAttachment
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechAttachmentPolicy
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechDirective
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechDirector
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechLine
import com.asianmobile.emojibattery.shimeji.pet.speech.PetSpeechPlacementPolicy
import kotlin.math.roundToInt

internal data class PetOverlayAsset(
    val pack: PetPack,
    val visual: PetPackVisual
)

internal class PetOverlayController(
    context: Context,
    assets: List<PetOverlayAsset>,
    private val preferences: PetPreferences,
    performanceBudget: PetPerformanceBudget,
    private val windowManager: WindowManager =
        context.getSystemService(WindowManager::class.java),
    private val choreographer: Choreographer = Choreographer.getInstance()
) {
    private val appContext = context.applicationContext
    private val settingsPolicy = PetSettingsPolicy()
    private val sessionLayout = PetSessionLayout()
    private val isSwarm = preferences.displayMode == PetDisplayMode.SWARM
    private val petCount = if (isSwarm) {
        settingsPolicy.sanitizeSwarmCount(
            preferences.swarm.count,
            performanceBudget.maxSwarmPets
        )
    } else {
        settingsPolicy.sanitizePetCount(
            preferences.petCount,
            performanceBudget.maxPets
        )
    }
    private val budgetFramesPerSecond = performanceBudget.targetFramesPerSecond
    private val maxSwarmPets = performanceBudget.maxSwarmPets
    private val availableAssets = assets.also {
        require(it.isNotEmpty()) { "At least one pet asset is required" }
    }
    private val selectedAssets = MutableList(petCount) { index ->
        availableAssets.getOrNull(index) ?: availableAssets.first()
    }
    private var targetFrameNanos = targetFrameNanos(petCount)
    private val socialDirector = if (isSwarm) {
        null
    } else {
        PetSocialDirector(
            sceneOffset = selectedAssets.fold(1) { hash, asset ->
                31 * hash + asset.pack.manifest.id.hashCode()
            }
        )
    }
    private val crowdResolver = PetCrowdResolver().takeUnless { isSwarm }
    private val speechSettings = selectedAssets.mapIndexed { index, _ ->
        val slot = runtimeSlot(index)
        index to SpeechSettings(
            enabled = !isSwarm && slot.messagesEnabled,
            customMessages = slot.customMessages
        )
    }.toMap().toMutableMap()
    private val speechDirectors = selectedAssets.mapIndexedNotNull { index, asset ->
        val settings = speechSettings.getValue(index)
        if (!settings.enabled) return@mapIndexedNotNull null
        index to createSpeechDirector(
            pack = asset.pack,
            index = index,
            customMessages = settings.customMessages
        )
    }.toMap().toMutableMap()
    private val instances = mutableListOf<PetInstance>()
    private val speechWindows = mutableMapOf<Int, SpeechWindow>()
    private var isRendering = false
    private var lastTickNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRendering) return
            if (lastTickNanos == 0L) lastTickNanos = frameTimeNanos
            val elapsedNanos = frameTimeNanos - lastTickNanos
            if (elapsedNanos >= targetFrameNanos) {
                lastTickNanos = frameTimeNanos
                val elapsedMillis = elapsedNanos / NANOS_PER_MILLISECOND
                val visibleInstances = instances.filter(PetInstance::isVisible)
                socialDirector
                    ?.update(
                        pets = visibleInstances.map { instance ->
                            PetSocialSnapshot(instance.id, instance.state)
                        },
                        elapsedMillis = elapsedMillis
                    )
                    ?.forEach(::dispatchSocialDirective)
                val event = PetEvent.Tick(elapsedMillis)
                visibleInstances.forEach { dispatch(it, event) }
                resolveCrowdSpacing(visibleInstances)
            }
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (instances.isNotEmpty()) return
        socialDirector?.reset()
        speechDirectors.values.forEach(PetSpeechDirector::reset)

        try {
            selectedAssets.forEachIndexed { index, asset ->
                instances += createInstance(index, asset)
            }
            speechDirectors.keys.forEach { petId ->
                instances
                    .firstOrNull { it.id == petId && it.isVisible }
                    ?.let(::addSpeechWindow)
            }
            instances.forEach { instance ->
                windowManager.addView(instance.view, instance.params)
            }
        } catch (error: RuntimeException) {
            removeAllViews()
            throw error
        }

        isRendering = true
        lastTickNanos = 0L
        choreographer.postFrameCallback(frameCallback)
    }

    fun updateSwarmCount(requestedCount: Int) {
        if (!isSwarm) return
        val targetCount = settingsPolicy.sanitizeSwarmCount(
            requestedCount,
            maxSwarmPets
        )
        if (targetCount == instances.size) return

        if (targetCount < instances.size) {
            while (instances.size > targetCount) {
                removeLastInstance()
            }
        } else {
            val sharedAsset = selectedAssets.first()
            while (instances.size < targetCount) {
                val index = instances.size
                val instance = createInstance(
                    index = index,
                    asset = sharedAsset,
                    randomSpawn = true
                )
                try {
                    windowManager.addView(instance.view, instance.params)
                    selectedAssets += sharedAsset
                    instances += instance
                } catch (error: RuntimeException) {
                    runCatching { windowManager.removeViewImmediate(instance.view) }
                    throw error
                }
            }
        }
        targetFrameNanos = targetFrameNanos(instances.size)
    }

    fun stop(): List<PetPositionFraction> {
        val positions = currentPositions()
        isRendering = false
        choreographer.removeFrameCallback(frameCallback)
        removeAllViews()
        socialDirector?.reset()
        speechDirectors.values.forEach(PetSpeechDirector::reset)
        lastTickNanos = 0L
        return positions
    }

    fun currentPositions(): List<PetPositionFraction> = instances.map { instance ->
        sessionLayout.normalize(
            position = instance.state.position,
            bounds = instance.state.bounds,
            size = instance.state.size
        )
    }

    fun pauseRendering() {
        if (!isRendering) return
        isRendering = false
        choreographer.removeFrameCallback(frameCallback)
        lastTickNanos = 0L
    }

    fun resumeRendering() {
        if (isRendering || instances.isEmpty()) return
        lastTickNanos = 0L
        isRendering = true
        choreographer.postFrameCallback(frameCallback)
    }

    fun onBoundsChanged() {
        instances.toList().forEach { instance ->
            val bounds = currentPlaygroundBounds(instance.state.size)
            render(instance, instance.engine.reduce(instance.state, PetEvent.BoundsChanged(bounds)).state)
        }
        speechWindows.values.toList().forEach(::updateSpeechPosition)
    }

    fun updateSizePercents(sizePercents: List<Int>) {
        if (isSwarm) return
        instances.toList().forEach { instance ->
            val percent = sizePercents.getOrNull(instance.id) ?: return@forEach
            val pack = selectedAssets[instance.id].pack
            val sizePixels = petSizePixels(pack, percent)
            if (instance.state.size.width.roundToInt() == sizePixels) return@forEach
            val size = PetSize(sizePixels.toFloat(), sizePixels.toFloat())
            val bounds = currentPlaygroundBounds(size)
            dispatch(instance, PetEvent.SizeChanged(size = size, bounds = bounds))
        }
    }

    fun updateSpeedPercents(speedPercents: List<Int>) {
        if (isSwarm) return
        instances.forEach { instance ->
            val speedPercent = speedPercents.getOrNull(instance.id) ?: return@forEach
            if (instance.speedPercent == speedPercent) return@forEach
            val pack = selectedAssets[instance.id].pack
            instance.engine = createEngine(
                pack = pack,
                index = instance.id,
                speedPercent = speedPercent
            )
            instance.speedPercent = speedPercent
        }
    }

    fun updateInteractionSettings(slotPreferences: List<PetSlotPreferences>) {
        if (isSwarm) return
        instances.forEach { instance ->
            val interactionEnabled = slotPreferences
                .getOrNull(instance.id)
                ?.interactionEnabled
                ?: return@forEach
            if (instance.interactionEnabled == interactionEnabled) return@forEach
            instance.interactionEnabled = interactionEnabled
            instance.params.flags = if (interactionEnabled) {
                instance.params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                instance.params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            if (instance.view.isAttachedToWindow) {
                windowManager.updateViewLayout(instance.view, instance.params)
            }
        }
    }

    fun updateSpeechSettings(slotPreferences: List<PetSlotPreferences>) {
        if (isSwarm) return
        instances.forEach { instance ->
            val slot = slotPreferences.getOrNull(instance.id) ?: return@forEach
            val requested = SpeechSettings(
                enabled = slot.messagesEnabled,
                customMessages = slot.customMessages
            )
            if (speechSettings[instance.id] == requested) return@forEach

            speechDirectors.remove(instance.id)?.reset()
            removeSpeechWindow(instance.id)
            speechSettings[instance.id] = requested
            if (requested.enabled && instance.isVisible) {
                speechDirectors[instance.id] = createSpeechDirector(
                    pack = selectedAssets[instance.id].pack,
                    index = instance.id,
                    customMessages = requested.customMessages
                ).also(PetSpeechDirector::reset)
                addSpeechWindow(instance)
            }
        }
    }

    fun updateVisibilitySettings(slotPreferences: List<PetSlotPreferences>) {
        if (isSwarm) return
        instances.forEach { instance ->
            val requestedVisible = slotPreferences
                .getOrNull(instance.id)
                ?.isEnabled
                ?: return@forEach
            if (instance.isVisible == requestedVisible) return@forEach
            instance.isVisible = requestedVisible
            instance.view.visibility = if (requestedVisible) View.VISIBLE else View.INVISIBLE
            speechDirectors.remove(instance.id)?.reset()
            removeSpeechWindow(instance.id)
            if (requestedVisible) {
                val speech = speechSettings[instance.id]
                if (speech?.enabled == true) {
                    speechDirectors[instance.id] = createSpeechDirector(
                        pack = selectedAssets[instance.id].pack,
                        index = instance.id,
                        customMessages = speech.customMessages
                    ).also(PetSpeechDirector::reset)
                    addSpeechWindow(instance)
                }
            }
        }
        socialDirector?.reset()
    }

    fun resetPositions(slotIndexes: List<Int>) {
        slotIndexes.forEach { slotIndex ->
            val instance = instances.firstOrNull { it.id == slotIndex } ?: return@forEach
            val pack = selectedAssets[instance.id].pack
            val position = sessionLayout.resolvePosition(
                index = instance.id,
                bounds = instance.state.bounds,
                size = instance.state.size,
                saved = emptyList(),
                marginPixels = appContext.dpToPixels(START_MARGIN_DP).toFloat()
            )
            val action = when {
                PetAction.FALL in pack.manifest.clips -> PetAction.FALL
                PetAction.WALK in pack.manifest.clips -> PetAction.WALK
                else -> PetAction.IDLE
            }
            speechDirectors[instance.id]?.reset()
            hideSpeech(instance.id)
            render(
                instance = instance,
                updatedState = instance.engine.initialState(
                    bounds = instance.state.bounds,
                    size = instance.state.size,
                    position = position,
                    action = action,
                    direction = instance.state.direction
                )
            )
        }
    }

    private fun dispatch(instance: PetInstance, event: PetEvent) {
        if (instance !in instances || !instance.isVisible) return
        val previousState = instance.state
        val transition = instance.engine.reduce(previousState, event)
        render(instance, transition.state)
        speechDirectors[instance.id]
            ?.onTransition(instance.id, previousState, transition)
            ?.forEach(::applySpeechDirective)
    }

    private fun dispatchSocialDirective(directive: PetSocialDirective) {
        val instance = instances.firstOrNull { it.id == directive.petId } ?: return
        val event = when (directive) {
            is PetSocialDirective.Face -> PetEvent.Face(directive.direction)
            is PetSocialDirective.StartCombo -> PetEvent.StartCombo(
                comboId = directive.comboId,
                direction = directive.direction
            )
        }
        dispatch(instance, event)
    }

    private fun resolveCrowdSpacing(visibleInstances: List<PetInstance>) {
        val resolver = crowdResolver ?: return
        val resolvedStates = resolver.resolve(
            visibleInstances.map(PetInstance::state)
        )
        visibleInstances.zip(resolvedStates).forEach { (instance, resolvedState) ->
            if (instance.state != resolvedState) {
                render(instance, resolvedState)
            }
        }
    }

    private fun render(instance: PetInstance, updatedState: PetState) {
        val previousState = instance.state
        instance.state = updatedState
        instance.view.render(updatedState)

        if (previousState.size != updatedState.size) {
            instance.params.width = updatedState.size.width.roundToInt()
            instance.params.height = updatedState.size.height.roundToInt()
        }
        if (previousState.position != updatedState.position) {
            instance.params.x = updatedState.position.x.roundToInt()
            instance.params.y = updatedState.position.y.roundToInt()
        }
        if (previousState.position != updatedState.position ||
            previousState.size != updatedState.size
        ) {
            if (instance.view.isAttachedToWindow) {
                windowManager.updateViewLayout(instance.view, instance.params)
            }
        }
        val speechPlacementChanged =
            previousState.position != updatedState.position ||
                previousState.size != updatedState.size ||
                previousState.bounds != updatedState.bounds ||
                previousState.direction != updatedState.direction ||
                previousState.action != updatedState.action
        if (speechPlacementChanged) {
            speechWindows[instance.id]?.let(::updateSpeechPosition)
        }
    }

    private fun applySpeechDirective(directive: PetSpeechDirective) {
        when (directive) {
            is PetSpeechDirective.Show -> showSpeech(
                petId = directive.petId,
                line = directive.line
            )

            is PetSpeechDirective.Hide -> {
                hideSpeech(directive.petId)
            }
        }
    }

    private fun showSpeech(
        petId: Int,
        line: PetSpeechLine
    ) {
        val instance = instances.firstOrNull { it.id == petId } ?: return
        val window = speechWindows[petId] ?: return
        window.line = line
        window.view.measureBox(
            text = line.text,
            maximumWidthPixels = speechMaximumWidth(instance)
        ).let { size ->
            window.params.width = size.width
            window.params.height = size.height
        }
        updateSpeechPosition(window, instance)
        window.view.visibility = View.VISIBLE
    }

    private fun addSpeechWindow(instance: PetInstance) {
        if (speechWindows.containsKey(instance.id)) return
        val view = PetSpeechBubbleView(appContext).apply {
            visibility = View.INVISIBLE
        }
        val params = createSpeechLayoutParams(instance.id, width = 1, height = 1)
        val window = SpeechWindow(instance.id, view, params, line = null)
        try {
            windowManager.addView(view, params)
            speechWindows[instance.id] = window
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to prepare pet speech bubble", error)
        }
    }

    private fun removeSpeechWindow(petId: Int) {
        val window = speechWindows.remove(petId) ?: return
        runCatching { windowManager.removeViewImmediate(window.view) }
    }

    private fun updateSpeechPosition(window: SpeechWindow) {
        if (window.line == null) return
        val instance = instances.firstOrNull { it.id == window.petId } ?: return
        updateSpeechPosition(window, instance)
    }

    private fun updateSpeechPosition(window: SpeechWindow, instance: PetInstance) {
        val line = window.line ?: return
        val state = instance.state
        val edgeOverflow = state.size.width / SCREEN_EDGE_OVERFLOW_DIVISOR
        val viewportWidth = (state.bounds.right - edgeOverflow).roundToInt()
        val viewportHeight = state.bounds.bottom.roundToInt()
        val placement = PetSpeechPlacementPolicy.resolve(
            petPosition = state.position,
            petSize = state.size,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            bubbleWidth = window.params.width,
            bubbleHeight = window.params.height,
            margin = appContext.dpToPixels(SPEECH_MARGIN_DP),
            direction = state.direction,
            attachment = instance.speechAttachment,
            attachmentOverlap = appContext.dpToPixels(SPEECH_ATTACHMENT_OVERLAP_DP)
        )
        window.params.x = placement.x
        window.params.y = placement.y
        window.view.render(line)
        if (window.view.isAttachedToWindow) {
            windowManager.updateViewLayout(window.view, window.params)
        }
    }

    private fun hideSpeech(petId: Int) {
        val window = speechWindows[petId] ?: return
        window.line = null
        window.view.visibility = View.INVISIBLE
    }

    private fun removeAllSpeechWindows() {
        speechWindows.values.forEach { window ->
            runCatching { windowManager.removeViewImmediate(window.view) }
        }
        speechWindows.clear()
    }

    private fun createLayoutParams(
        state: PetState,
        index: Int,
        slotPreferences: PetSlotPreferences
    ) = WindowManager.LayoutParams(
        state.size.width.roundToInt(),
        state.size.height.roundToInt(),
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            if (slotPreferences.interactionEnabled) {
                0
            } else {
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            },
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setFitInsetsTypes(
                WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
            )
        }
        x = state.position.x.roundToInt()
        y = state.position.y.roundToInt()
        title = "$OVERLAY_WINDOW_TITLE ${index + 1}"
    }

    private fun createSpeechLayoutParams(
        petId: Int,
        width: Int,
        height: Int
    ) = WindowManager.LayoutParams(
        width,
        height,
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setFitInsetsTypes(
                WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
            )
        }
        title = "$SPEECH_WINDOW_TITLE ${petId + 1}"
    }

    private fun speechMaximumWidth(instance: PetInstance): Int {
        val edgeOverflow = instance.state.size.width / SCREEN_EDGE_OVERFLOW_DIVISOR
        val viewportWidth = (instance.state.bounds.right - edgeOverflow).roundToInt()
        val margin = appContext.dpToPixels(SPEECH_MARGIN_DP)
        return (viewportWidth - margin * 2).coerceAtLeast(1)
    }

    private fun speechAttachment(pack: PetPack): PetSpeechAttachment {
        val speechAnchor = pack.manifest.speechAnchor
            ?: return PetSpeechAttachment.Default
        val canvas = pack.manifest.canvas
        val imageAnchor = pack.manifest.anchor
        return PetSpeechAttachmentPolicy.resolve(
            canvasWidth = canvas.width,
            canvasHeight = canvas.height,
            imageAnchorX = imageAnchor.x,
            imageAnchorY = imageAnchor.y,
            speechAnchorX = speechAnchor.x,
            speechAnchorY = speechAnchor.y
        )
    }

    private fun removeAllViews() {
        removeAllSpeechWindows()
        instances.forEach { instance ->
            runCatching { windowManager.removeViewImmediate(instance.view) }
        }
        instances.clear()
    }

    private fun createInstance(
        index: Int,
        asset: PetOverlayAsset,
        randomSpawn: Boolean = false
    ): PetInstance {
        val pack = asset.pack
        val slotPreferences = runtimeSlot(index)
        val petSizePixels = petSizePixels(pack, slotPreferences)
        val size = PetSize(petSizePixels.toFloat(), petSizePixels.toFloat())
        val bounds = currentPlaygroundBounds(size)
        val position = if (randomSpawn) {
            sessionLayout.randomPosition(
                bounds = currentSpawnBounds(size, bounds),
                size = size,
                seed = nextSpawnSeed(index, pack),
                occupiedPositions = instances.map { instance -> instance.state.position }
            )
        } else {
            sessionLayout.resolvePosition(
                index = index,
                bounds = bounds,
                size = size,
                saved = preferences.lastPositions,
                marginPixels = appContext.dpToPixels(START_MARGIN_DP).toFloat()
            )
        }
        val engine = createEngine(
            pack = pack,
            index = index,
            speedPercent = slotPreferences.speedPercent
        )
        val initialState = engine.initialState(
            bounds = bounds,
            size = size,
            position = position,
            action = if (PetAction.FALL in pack.manifest.clips) {
                PetAction.FALL
            } else {
                PetAction.WALK
            }
        )
        lateinit var instance: PetInstance
        val view = PetOverlayView(appContext, asset.visual) { event ->
            dispatch(instance, event)
        }.apply {
            render(initialState)
            visibility = if (slotPreferences.isEnabled) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }
        val params = createLayoutParams(initialState, index, slotPreferences)
        instance = PetInstance(
            id = index,
            engine = engine,
            view = view,
            params = params,
            speechAttachment = speechAttachment(pack),
            speedPercent = slotPreferences.speedPercent,
            interactionEnabled = slotPreferences.interactionEnabled,
            isVisible = slotPreferences.isEnabled,
            state = initialState
        )
        return instance
    }

    private fun removeLastInstance() {
        val instance = instances.removeAt(instances.lastIndex)
        selectedAssets.removeAt(selectedAssets.lastIndex)
        speechDirectors.remove(instance.id)?.reset()
        speechSettings.remove(instance.id)
        removeSpeechWindow(instance.id)
        runCatching { windowManager.removeViewImmediate(instance.view) }
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun currentPlaygroundBounds(size: PetSize): PetBounds {
        val baseBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val windowBounds = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
            )
            PetBounds(
                left = 0f,
                top = 0f,
                right = (windowBounds.width() - insets.left - insets.right).toFloat(),
                bottom = (windowBounds.height() - insets.top - insets.bottom).toFloat()
            )
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val displaySize = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(displaySize)
            PetBounds(
                left = 0f,
                top = 0f,
                right = displaySize.x.toFloat(),
                bottom = (displaySize.y - appContext.systemBarSize("status_bar_height")).toFloat()
            )
        }
        return if (isSwarm && preferences.swarm.constrainMovementArea) {
            settingsPolicy.constrainSwarmBounds(
                baseBounds,
                preferences.swarm.movementInsets
            )
        } else {
            baseBounds.expandedForScreenEdges(size)
        }
    }

    private fun currentSpawnBounds(
        size: PetSize,
        movementBounds: PetBounds
    ): PetBounds {
        if (isSwarm && preferences.swarm.constrainMovementArea) return movementBounds
        val overflow = size.width / SCREEN_EDGE_OVERFLOW_DIVISOR
        return PetBounds(
            left = movementBounds.left + overflow,
            top = movementBounds.top + overflow,
            right = movementBounds.right - overflow,
            bottom = movementBounds.bottom
        )
    }

    private fun nextSpawnSeed(index: Int, pack: PetPack): Long =
        System.nanoTime() xor
            pack.manifest.id.hashCode().toLong() xor
            ((index + 1L) * PET_BEHAVIOR_SEED_STEP)

    private fun Context.dpToPixels(dp: Int): Int =
        (dp * resources.displayMetrics.density).roundToInt()

    private fun petSizePixels(
        pack: PetPack,
        slotPreferences: PetSlotPreferences
    ): Int = petSizePixels(pack, slotPreferences.sizePercent)

    private fun petSizePixels(
        pack: PetPack,
        sizePercent: Int
    ): Int = appContext.dpToPixels(
        (PET_SIZE_DP * pack.manifest.canvas.defaultScale * sizePercent / 100f)
            .roundToInt()
            .coerceIn(MIN_PET_SIZE_DP, MAX_PET_SIZE_DP)
    )

    private fun createEngine(
        pack: PetPack,
        index: Int,
        speedPercent: Int
    ): PetEngine = PetEngine(
        PetEngineConfig(
            clips = pack.manifest.toEngineClips(speedPercent / 100f),
            tapAction = pack.manifest.interaction.tapAction,
            supportedActions = pack.manifest.toEngineSupportedActions(),
            behaviorProfile = if (isSwarm) {
                PetBehaviorProfiles.SWARM
            } else {
                PetBehaviorProfile()
            },
            behaviorSeed = pack.manifest.id.hashCode().toLong() xor
                ((index + 1L) * PET_BEHAVIOR_SEED_STEP)
        )
    )

    private fun createSpeechDirector(
        pack: PetPack,
        index: Int,
        customMessages: List<String>
    ): PetSpeechDirector = PetSpeechDirector(
        catalog = appContext.petSpeechCatalog(customMessages),
        seed = 31 * pack.manifest.id.hashCode() + index
    )

    @Suppress("DiscouragedApi")
    private fun Context.systemBarSize(resourceName: String): Int {
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return if (resourceId == 0) 0 else resources.getDimensionPixelSize(resourceId)
    }

    private fun targetFrameNanos(count: Int): Long {
        val framesPerSecond = if (isSwarm) {
            settingsPolicy.targetSwarmFramesPerSecond(count, budgetFramesPerSecond)
        } else {
            settingsPolicy.targetFramesPerSecond(count, budgetFramesPerSecond)
        }
        return NANOS_PER_SECOND / framesPerSecond
    }

    private data class PetInstance(
        val id: Int,
        var engine: PetEngine,
        val view: PetOverlayView,
        val params: WindowManager.LayoutParams,
        val speechAttachment: PetSpeechAttachment,
        var speedPercent: Int,
        var interactionEnabled: Boolean,
        var isVisible: Boolean,
        var state: PetState
    )

    private data class SpeechSettings(
        val enabled: Boolean,
        val customMessages: List<String>
    )

    private data class SpeechWindow(
        val petId: Int,
        val view: PetSpeechBubbleView,
        val params: WindowManager.LayoutParams,
        var line: PetSpeechLine?
    )

    private companion object {
        const val PET_SIZE_DP = 84
        const val MIN_PET_SIZE_DP = 48
        const val MAX_PET_SIZE_DP = 144
        const val START_MARGIN_DP = 20
        const val OVERLAY_WINDOW_TITLE = "Cute Pet overlay"
        const val SPEECH_WINDOW_TITLE = "Cute Pet speech"
        const val SPEECH_MARGIN_DP = 6
        const val SPEECH_ATTACHMENT_OVERLAP_DP = 3
        const val SCREEN_EDGE_OVERFLOW_DIVISOR = 3f
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val PET_BEHAVIOR_SEED_STEP = 1_103_515_245L
        const val TAG = "PetOverlayController"
        const val SWARM_SIZE_VARIATION_SALT = 0x2B7E
        const val SWARM_SPEED_VARIATION_SALT = 0x61D3
    }

    private fun runtimeSlot(index: Int): PetSlotPreferences = if (isSwarm) {
        val swarm = preferences.swarm
        val seed = swarm.packKey.hashCode()
        PetSlotPreferences(
            packKey = swarm.packKey,
            sizePercent = settingsPolicy.swarmVariationPercent(
                basePercent = swarm.sizePercent,
                instanceIndex = index,
                seed = seed xor SWARM_SIZE_VARIATION_SALT,
                minimumPercent = PetSettingsPolicy.MIN_SIZE_PERCENT,
                maximumPercent = PetSettingsPolicy.MAX_SIZE_PERCENT,
                stepPercent = PetSettingsPolicy.SIZE_STEP_PERCENT,
                enabled = swarm.randomizeSizeAndSpeed
            ),
            speedPercent = settingsPolicy.swarmVariationPercent(
                basePercent = swarm.speedPercent,
                instanceIndex = index,
                seed = seed xor SWARM_SPEED_VARIATION_SALT,
                minimumPercent = PetSettingsPolicy.MIN_SPEED_PERCENT,
                maximumPercent = PetSettingsPolicy.MAX_SPEED_PERCENT,
                stepPercent = PetSettingsPolicy.SPEED_STEP_PERCENT,
                enabled = swarm.randomizeSizeAndSpeed
            ),
            messagesEnabled = false,
            interactionEnabled = true,
            isEnabled = true
        )
    } else {
        preferences.slot(index)
    }
}
