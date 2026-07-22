package com.asianmobile.privatebrower.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        val IS_INTRO_COMPLETED = booleanPreferencesKey("is_intro_completed")
        val IS_LANGUAGE_COMPLETED = booleanPreferencesKey("is_language_completed")
        val IS_PERMISSION_COMPLETED = booleanPreferencesKey("is_permission_completed")
        val COUNTRY_LANGUAGE = stringPreferencesKey("country_language")
        val KEY_LANGUAGE = stringPreferencesKey("key_language")
        val SELECTED_SEARCH_ENGINE = stringPreferencesKey("selected_search_engine")
    }

    val isIntroCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_INTRO_COMPLETED] ?: false
    }

    val isLanguageCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LANGUAGE_COMPLETED] ?: false
    }

    val isPermissionCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PERMISSION_COMPLETED] ?: false
    }

    val selectedSearchEngine: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_SEARCH_ENGINE] ?: "google"
    }

    suspend fun saveIntroCompleted(completed: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[IS_INTRO_COMPLETED] = completed
        }
    }

    suspend fun saveLanguageCompleted(completed: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[IS_LANGUAGE_COMPLETED] = completed
        }
    }

    suspend fun savePermissionCompleted(completed: Boolean) {
        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[IS_PERMISSION_COMPLETED] = completed
        }
    }

    suspend fun saveLanguage(key: String, country: String) {
        context.getSharedPreferences("language_cache", Context.MODE_PRIVATE)
            .edit()
            .putString("key_language", key)
            .putString("country_language", country)
            .apply()

        context.dataStore.edit { preferences: MutablePreferences ->
            preferences[KEY_LANGUAGE] = key
            preferences[COUNTRY_LANGUAGE] = country
        }
    }

    fun runtimePermissionRequestCounts(
        permissions: Collection<String>
    ): Flow<Map<String, Int>> = context.dataStore.data.map { preferences ->
        permissions.associateWith { permission ->
            preferences[runtimePermissionRequestCountKey(permission)] ?: 0
        }
    }

    suspend fun markRuntimePermissionsRequested(permissions: Collection<String>) {
        if (permissions.isEmpty()) return
        context.dataStore.edit { preferences ->
            permissions.forEach { permission ->
                val key = runtimePermissionRequestCountKey(permission)
                preferences[key] = (preferences[key] ?: 0) + 1
            }
        }
    }

    suspend fun saveSelectedSearchEngine(engine: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_SEARCH_ENGINE] = engine
        }
    }

    private fun runtimePermissionRequestCountKey(permission: String) = intPreferencesKey(
        "runtime_permission_request_count_${permission.substringAfterLast('.').lowercase()}"
    )
}
