package com.asianmobile.privatebrower.pet.engine

enum class PetDirection {
    LEFT,
    RIGHT
}

data class PetState(
    val position: PetVector,
    val velocity: PetVector,
    val size: PetSize,
    val bounds: PetBounds,
    val action: PetAction,
    val direction: PetDirection,
    val animationCursor: PetAnimationCursor
) {
    val frameIndex: Int
        get() = animationCursor.frameIndex
}

sealed interface PetEvent {
    data class Tick(val elapsedMillis: Long) : PetEvent
    data object Tap : PetEvent
    data object DragStart : PetEvent
    data class DragBy(val delta: PetVector) : PetEvent
    data object DragEnd : PetEvent
    data class Fling(val velocity: PetVector) : PetEvent
    data class BoundsChanged(val bounds: PetBounds) : PetEvent
}

sealed interface PetEffect {
    data class ActionChanged(
        val from: PetAction,
        val to: PetAction
    ) : PetEffect

    data object Tapped : PetEffect
}

data class PetTransition(
    val state: PetState,
    val effects: List<PetEffect> = emptyList()
)
