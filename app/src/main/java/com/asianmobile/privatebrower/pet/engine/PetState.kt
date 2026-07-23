package com.asianmobile.privatebrower.pet.engine

import kotlin.math.abs

enum class PetDirection {
    LEFT,
    RIGHT
}

fun PetDirection.requiresMirror(nativeDirection: PetDirection): Boolean = this != nativeDirection

data class PetState(
    val position: PetVector,
    val velocity: PetVector,
    val size: PetSize,
    val bounds: PetBounds,
    val action: PetAction,
    val direction: PetDirection,
    val animationCursor: PetAnimationCursor,
    val actionElapsedMillis: Long = 0,
    val actionTargetMillis: Long = 0,
    val behaviorSequence: Long = 0,
    val nonClimbComboStreak: Int = 0,
    val activeComboId: PetComboId? = null,
    val recentComboIds: List<PetComboId> = emptyList(),
    val activeComboBeat: PetComboBeat? = null,
    val comboBeatElapsedMillis: Long = 0,
    val comboBeatTargetMillis: Long = 0,
    val isHoldingComboBeatFrame: Boolean = false,
    val pendingComboBeats: List<PetComboBeat> = emptyList()
) {
    val frameIndex: Int
        get() = animationCursor.frameIndex
}

sealed interface PetEvent {
    data class Tick(val elapsedMillis: Long) : PetEvent
    data object Tap : PetEvent
    data object Showcase : PetEvent
    data object DragStart : PetEvent
    data class DragBy(val delta: PetVector) : PetEvent
    data object DragEnd : PetEvent
    data class Fling(val velocity: PetVector) : PetEvent
    data class BoundsChanged(val bounds: PetBounds) : PetEvent
    data class Face(val direction: PetDirection) : PetEvent
    data class StartCombo(
        val comboId: PetComboId,
        val direction: PetDirection? = null
    ) : PetEvent
}

sealed interface PetEffect {
    data class ActionChanged(
        val from: PetAction,
        val to: PetAction
    ) : PetEffect

    data object Tapped : PetEffect
    data object ShowcaseStarted : PetEffect
    data class ComboStarted(val comboId: PetComboId) : PetEffect
    data class ComboCompleted(val comboId: PetComboId) : PetEffect
}

data class PetTransition(
    val state: PetState,
    val effects: List<PetEffect> = emptyList()
)

internal fun PetState.isGroundedSurface(
    toleranceInPetHeights: Float = 0.2f
): Boolean {
    if (action !in GROUND_SURFACE_ACTIONS) return false
    val floorY = bounds.bottom - size.height
    return abs(position.y - floorY) <= size.height * toleranceInPetHeights
}

private val GROUND_SURFACE_ACTIONS = setOf(
    PetAction.IDLE,
    PetAction.WALK,
    PetAction.RUN,
    PetAction.CREEP,
    PetAction.SIT,
    PetAction.WINK,
    PetAction.LOOK_UP,
    PetAction.BOUNCE,
    PetAction.TRIP,
    PetAction.TALK,
    PetAction.TALK_WALK,
    PetAction.SPECIAL,
    PetAction.SPECIAL_2,
    PetAction.TAPPED
)
