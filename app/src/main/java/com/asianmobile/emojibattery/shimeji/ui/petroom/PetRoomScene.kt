package com.asianmobile.emojibattery.shimeji.ui.petroom

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetBounds
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEngine
import com.asianmobile.emojibattery.shimeji.pet.engine.PetEvent
import com.asianmobile.emojibattery.shimeji.pet.engine.PetSize
import com.asianmobile.emojibattery.shimeji.pet.engine.PetState
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackVisual
import kotlin.math.roundToInt

/**
 * Draws the owned pets inside the room artwork. One shared frame clock ticks every engine, the
 * same way the overlay controller does, so adding a pet costs a reducer call rather than a
 * timer.
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
            pets.mapIndexed { index, entry -> entry.toRuntime(index, sceneSize) }
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
            runtimes.forEach { runtime ->
                runtime.state = runtime.engine
                    .reduce(runtime.state, PetEvent.Tick(elapsedMillis))
                    .state
            }
            frame = nanos
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { sceneSize = it }
            .pointerInput(runtimes) {
                detectTapGestures { offset ->
                    // Topmost pet wins so a tap never opens the one drawn underneath.
                    runtimes.lastOrNull { it.contains(offset) }?.let { onPetTapped(it.packKey) }
                }
            }
    ) {
        // Read the frame stamp so every clock tick recomposes the draw pass.
        @Suppress("UNUSED_EXPRESSION")
        frame
        runtimes.forEach { runtime -> drawPet(runtime) }
    }
}

private fun DrawScope.drawPet(runtime: PetRoomSceneRuntime) {
    val visual = runtime.visual ?: return
    val state = runtime.state
    val frames = visual.frames[state.action]
        ?: visual.frames[state.action.roomFallback()]
        ?: visual.frames[PetAction.IDLE]
        ?: return
    if (frames.isEmpty()) return
    val frame = frames[state.frameIndex.coerceIn(0, frames.lastIndex)]

    val boxWidth = state.size.width
    val boxHeight = state.size.height
    val fitScale = minOf(boxWidth / visual.canvas.width, boxHeight / visual.canvas.height)
    val drawWidth = visual.canvas.width * fitScale
    val drawHeight = visual.canvas.height * fitScale
    val anchorX = state.position.x + boxWidth * 0.5f
    val anchorY = state.position.y + boxHeight
    val left = anchorX - visual.anchor.x * drawWidth
    val top = anchorY - visual.anchor.y * drawHeight

    drawImage(
        image = frame.bitmap.asImageBitmap(),
        srcOffset = IntOffset(frame.source.left, frame.source.top),
        srcSize = IntSize(frame.source.width(), frame.source.height()),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt())
    )
}

/** The room blocks the climbing repertoire, so those frames never need a fallback here. */
private fun PetAction.roomFallback(): PetAction = when (this) {
    PetAction.RUN, PetAction.CREEP -> PetAction.WALK
    PetAction.EMOTE -> PetAction.WINK
    PetAction.FLOOR_PLAY, PetAction.SPRAWL -> PetAction.SIT
    else -> PetAction.IDLE
}

private fun PetRoomSceneEntry.toRuntime(index: Int, sceneSize: IntSize): PetRoomSceneRuntime {
    val petSize = (sceneSize.width * PET_WIDTH_RATIO).coerceAtMost(MAX_PET_SIZE_PX)
    val size = PetSize(petSize, petSize)
    val bounds = PetBounds(
        left = 0f,
        top = sceneSize.height * FLOOR_TOP_RATIO,
        right = sceneSize.width.toFloat(),
        bottom = sceneSize.height * FLOOR_BOTTOM_RATIO
    )
    val engine = PetEngine(engineConfig)
    val slot = (index + 1f) / (SPAWN_SLOTS + 1f)
    val position = PetVector(
        x = sceneSize.width * slot - petSize * 0.5f,
        y = bounds.bottom - petSize
    )
    return PetRoomSceneRuntime(
        packKey = packKey,
        engine = engine,
        state = engine.initialState(bounds = bounds, size = size, position = position),
        visual = visual as? PetPackVisual.Sprite
    )
}

private class PetRoomSceneRuntime(
    val packKey: String,
    val engine: PetEngine,
    var state: PetState,
    val visual: PetPackVisual.Sprite?
) {
    fun contains(offset: Offset): Boolean {
        val left = state.position.x
        val top = state.position.y
        return offset.x >= left && offset.x <= left + state.size.width &&
            offset.y >= top && offset.y <= top + state.size.height
    }
}

private const val NANOS_PER_MILLI = 1_000_000L
private const val MAX_TICK_MILLIS = 250L
private const val PET_WIDTH_RATIO = 0.23f
private const val MAX_PET_SIZE_PX = 320f
private const val SPAWN_SLOTS = 4f

// Figma places the pets on the rug band of the room artwork.
private const val FLOOR_TOP_RATIO = 0.52f
private const val FLOOR_BOTTOM_RATIO = 0.72f
