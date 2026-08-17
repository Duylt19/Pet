package com.asianmobile.emojibattery.shimeji.pet.room

import com.asianmobile.emojibattery.shimeji.R

/**
 * One room ships inside the APK so the room is never empty: on a first run, offline, or while
 * the catalog is still loading there is always a background to draw. Every other room downloads
 * on demand.
 */
object PetRoomBundledBackground {
    const val ROOM_ID = 1

    val backgroundRes: Int = R.drawable.img_pet_room_bg_1
    val thumbnailRes: Int = R.drawable.img_pet_room_bg_1_thumb

    fun isBundled(roomId: Int): Boolean = roomId == ROOM_ID

    fun backgroundResOrNull(roomId: Int): Int? =
        backgroundRes.takeIf { isBundled(roomId) }

    fun thumbnailResOrNull(roomId: Int): Int? =
        thumbnailRes.takeIf { isBundled(roomId) }
}
