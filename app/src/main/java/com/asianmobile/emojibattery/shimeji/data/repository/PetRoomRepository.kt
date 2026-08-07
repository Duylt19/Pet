package com.asianmobile.emojibattery.shimeji.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Persistent My Pet Room preferences that are independent of the remote catalog. */
interface PetRoomRepository {
    /** Catalog room ID chosen by the user, or [NO_ROOM_SELECTED] while the default applies. */
    val selectedRoomId: StateFlow<Int>

    suspend fun selectRoom(roomId: Int)

    /** Whether the user left room music on. */
    val isMusicOn: StateFlow<Boolean>

    suspend fun setMusicOn(enabled: Boolean)

    companion object {
        const val NO_ROOM_SELECTED = 0
    }
}
