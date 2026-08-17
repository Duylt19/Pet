package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.asianmobile.emojibattery.shimeji.data.local.dataStore
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomRepository
import com.asianmobile.emojibattery.shimeji.data.repository.PetRoomRepository.Companion.NO_ROOM_SELECTED
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class DataStorePetRoomRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PetRoomRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selected = MutableStateFlow(NO_ROOM_SELECTED)
    override val selectedRoomId: StateFlow<Int> = selected.asStateFlow()
    private val music = MutableStateFlow(false)
    override val isMusicOn: StateFlow<Boolean> = music.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collect { preferences ->
                selected.value = preferences[SELECTED_ROOM_ID]?.takeIf { it > 0 }
                    ?: NO_ROOM_SELECTED
                music.value = preferences[MUSIC_ON] ?: false
            }
        }
    }

    override suspend fun selectRoom(roomId: Int) {
        if (roomId <= 0) return
        context.dataStore.edit { preferences -> preferences[SELECTED_ROOM_ID] = roomId }
    }

    override suspend fun setMusicOn(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[MUSIC_ON] = enabled }
    }

    private companion object {
        val SELECTED_ROOM_ID = intPreferencesKey("pet_room_selected_id")
        val MUSIC_ON = booleanPreferencesKey("pet_room_music_on")
    }
}
