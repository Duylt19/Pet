package com.asianmobile.emojibattery.shimeji.pet.engine

object PetGesturePolicy {
    fun releaseEvent(velocity: PetVector, minimumFlingVelocity: Float): PetEvent {
        require(minimumFlingVelocity > 0f) { "minimum fling velocity must be positive" }
        return if (velocity.magnitude >= minimumFlingVelocity) {
            PetEvent.Fling(velocity)
        } else {
            PetEvent.DragEnd
        }
    }
}
