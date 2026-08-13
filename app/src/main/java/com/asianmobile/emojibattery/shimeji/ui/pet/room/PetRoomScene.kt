package com.asianmobile.emojibattery.shimeji.ui.pet.room

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetClip
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackVisual
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomFloor
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomRest
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomWanderState
import com.asianmobile.emojibattery.shimeji.pet.room.PetRoomWanderer
import kotlin.math.roundToInt

/**
 * Draws the owned pets wandering the room floor. One shared frame clock advances every pet, the
 * way the overlay controller does, so a pet costs a step and a draw rather than its own timer.
 */
@Composable
fun PetRoomScene(
    pets: List<PetRoomSceneEntry>,
    modifier: Modifier = Modifier,
    onPetTapped: (String) -> Unit = {}
) {
    var sceneSize by remember { mutableStateOf(IntSize.Zero) }
    val runtimes = remember(pets, sceneSize) {
        if (sceneSize == IntSize.Zero) {
            emptyList()
        } else {
            val floor = PetRoomFloor(
                left = sceneSize.width * FLOOR_SIDE_MARGIN,
                right = sceneSize.width * (1f - FLOOR_SIDE_MARGIN),
                top = sceneSize.height * FLOOR_TOP_RATIO,
                bottom = sceneSize.height * FLOOR_BOTTOM_RATIO
            )
            pets.mapIndexed { index, entry ->
                entry.toRuntime(index, pets.size, floor)
            }
        }
    }
    var frame by remember { mutableStateOf(0L) }

    LaunchedEffect(runtimes) {
        if (runtimes.isEmpty()) return@LaunchedEffect
        var previousNanos = withFrameNanos { it }
        while (true) {
            val nanos = withFrameNanos { it }
            val elapsedMillis = ((nanos - previousNanos) / NANOS_PER_MILLI)
                .coerceIn(0L, MAX_TICK_MILLIS)
            previousNanos = nanos
            if (elapsedMillis <= 0L) continue
            runtimes.forEach { it.advance(elapsedMillis) }
            frame = nanos
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { sceneSize = it }
            .pointerInput(runtimes) {
                detectTapGestures { offset ->
                    // Front pets are drawn last, so they also win the tap.
                    runtimes.sortedBy { it.state.y }
                        .lastOrNull { it.contains(offset) }
                        ?.let { onPetTapped(it.packKey) }
                }
            }
    ) {
        // Read the frame stamp so every clock tick recomposes the draw pass.
        @Suppress("UNUSED_EXPRESSION")
        frame
        // Painter's order by depth: a pet standing further back cannot cover one in front.
        runtimes.sortedBy { it.state.y }.forEach { drawPet(it) }
    }
}

private fun DrawScope.drawPet(runtime: PetRoomSceneRuntime) {
    val visual = runtime.visual ?: return
    val frames = visual.frames[runtime.action]
        ?: visual.frames[PetAction.IDLE]
        ?: return
    if (frames.isEmpty()) return
    val frame = frames[runtime.frameIndex.coerceIn(0, frames.lastIndex)]

    val bounds = runtime.bounds()
    val fitScale = minOf(
        bounds.width / visual.canvas.width,
        bounds.height / visual.canvas.height
    )
    val drawWidth = visual.canvas.width * fitScale
    val drawHeight = visual.canvas.height * fitScale
    val left = bounds.centerX - visual.anchor.x * drawWidth
    val top = bounds.bottom - visual.anchor.y * drawHeight

    val draw = {
        drawImage(
            image = frame.bitmap.asImageBitmap(),
            srcOffset = IntOffset(frame.source.left, frame.source.top),
            srcSize = IntSize(frame.source.width(), frame.source.height()),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
            filterQuality = FilterQuality.High
        )
    }
    // Pack sprites are drawn facing left, the way the overlay treats them, so walking right is
    // the mirrored case.
    if (runtime.state.facingRight) {
        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(bounds.centerX, bounds.bottom)) { draw() }
    } else {
        draw()
    }
}

private fun PetRoomSceneEntry.toRuntime(
    index: Int,
    count: Int,
    floor: PetRoomFloor
): PetRoomSceneRuntime {
    val sprite = visual as? PetPackVisual.Sprite
    val wanderer = PetRoomWanderer(
        seed = packKey.hashCode().toLong(),
        floor = floor,
        walkSpeedPerSecond = petSizePx * WALK_SPEED_PER_PET_WIDTH * speedMultiplier,
        rests = sprite.availableRests()
    )
    return PetRoomSceneRuntime(
        packKey = packKey,
        floor = floor,
        petSize = petSizePx,
        wanderer = wanderer,
        state = wanderer.initial(index, count),
        visual = sprite,
        clips = engineConfig.clips
    )
}

private class PetRoomSceneRuntime(
    val packKey: String,
    private val floor: PetRoomFloor,
    private val petSize: Float,
    private val wanderer: PetRoomWanderer,
    state: PetRoomWanderState,
    val visual: PetPackVisual.Sprite?,
    private val clips: Map<PetAction, PetClip>
) {
    var state: PetRoomWanderState = state
        private set
    var frameIndex: Int = 0
        private set

    private var clipElapsedMillis = 0L

    val action: PetAction
        get() = if (state.isWalking) {
            firstAvailable(PetAction.WALK, PetAction.RUN) ?: PetAction.IDLE
        } else {
            when (state.rest) {
                PetRoomRest.STAND -> PetAction.IDLE
                PetRoomRest.SIT -> firstAvailable(PetAction.SIT)
                PetRoomRest.LIE -> firstAvailable(PetAction.SPRAWL, PetAction.DANGLE)
                PetRoomRest.PLAY -> firstAvailable(PetAction.FLOOR_PLAY, PetAction.TRIP)
                PetRoomRest.EMOTE -> firstAvailable(PetAction.EMOTE, PetAction.WINK)
            } ?: PetAction.IDLE
        }

    private fun firstAvailable(vararg actions: PetAction): PetAction? =
        actions.firstOrNull { visual?.frames?.get(it)?.isNotEmpty() == true }

    fun advance(elapsedMillis: Long) {
        val previousAction = action
        state = wanderer.advance(state, elapsedMillis)
        if (action != previousAction) {
            clipElapsedMillis = 0L
            frameIndex = 0
            return
        }
        val clip = clips[action] ?: return
        clipElapsedMillis += elapsedMillis
        var duration = clip.frames[frameIndex.coerceIn(0, clip.frames.lastIndex)].durationMillis
        while (clipElapsedMillis >= duration) {
            clipElapsedMillis -= duration
            frameIndex = (frameIndex + 1) % clip.frames.size
            duration = clip.frames[frameIndex].durationMillis
        }
    }

    fun bounds(): PetRoomSpriteBounds {
        val scale = floor.scaleAt(state.y)
        val height = petSize * scale
        return PetRoomSpriteBounds(
            centerX = state.x,
            bottom = state.y,
            width = height,
            height = height
        )
    }

    fun contains(offset: Offset): Boolean {
        val bounds = bounds()
        return offset.x >= bounds.centerX - bounds.width / 2f &&
            offset.x <= bounds.centerX + bounds.width / 2f &&
            offset.y >= bounds.bottom - bounds.height &&
            offset.y <= bounds.bottom
    }
}

private data class PetRoomSpriteBounds(
    val centerX: Float,
    val bottom: Float,
    val width: Float,
    val height: Float
)

/** A pack only rests in ways it has frames for; the rest of the repertoire is skipped. */
private fun PetPackVisual.Sprite?.availableRests(): List<PetRoomRest> {
    if (this == null) return listOf(PetRoomRest.STAND)
    fun has(vararg actions: PetAction) = actions.any { frames[it]?.isNotEmpty() == true }
    return buildList {
        add(PetRoomRest.STAND)
        if (has(PetAction.SIT)) add(PetRoomRest.SIT)
        if (has(PetAction.SPRAWL, PetAction.DANGLE)) add(PetRoomRest.LIE)
        if (has(PetAction.FLOOR_PLAY, PetAction.TRIP)) add(PetRoomRest.PLAY)
        if (has(PetAction.EMOTE, PetAction.WINK)) add(PetRoomRest.EMOTE)
    }
}

private const val NANOS_PER_MILLI = 1_000_000L
private const val MAX_TICK_MILLIS = 250L
private const val WALK_SPEED_PER_PET_WIDTH = 0.55f
private const val FLOOR_SIDE_MARGIN = 0.06f

// Figma places the pets on the rug band of the room artwork.
private const val FLOOR_TOP_RATIO = 0.5f
private const val FLOOR_BOTTOM_RATIO = 0.72f
