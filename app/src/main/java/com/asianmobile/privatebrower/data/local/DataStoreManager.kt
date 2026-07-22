package com.asianmobile.privatebrower.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val IS_INTRO_COMPLETED = booleanPreferencesKey("is_intro_completed")
        private val IS_LANGUAGE_COMPLETED = booleanPreferencesKey("is_language_completed")
        private val IS_PERMISSION_COMPLETED = booleanPreferencesKey("is_permission_completed")
        private val PASS_WORD_VAULT = stringPreferencesKey("pass_word_vault")
        private val EMAIL_RECOVERY_PASSWORD = stringPreferencesKey("email_recovery_password")
        private val QUESTION_RECOVERY_ID = stringPreferencesKey("question_recovery_id")
        private val QUESTION_RECOVERY_ANSWER = stringPreferencesKey("question_recovery_answer")
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val COUNTRY_LANGUAGE = stringPreferencesKey("country_language")
        private val KEY_LANGUAGE = stringPreferencesKey("key_language")
        val NOTIFICATIONS_JSON = stringPreferencesKey("notifications_json")
        private val LAST_KNOWN_MEDIA_COUNT = intPreferencesKey("last_known_media_count")
        private val IS_DEFAULT_BROWSER_ACCEPTED = booleanPreferencesKey("is_default_browser_accepted")
        private val SELECTED_SEARCH_ENGINE = stringPreferencesKey("selected_search_engine")
        private val IS_INCOGNITO_DEFAULT = booleanPreferencesKey("is_incognito_default")
        private val LAST_USED_TAB_ID = longPreferencesKey("last_used_tab_id")
        private val SESSION_COUNT = intPreferencesKey("session_count")
    }

    val isIntroCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[IS_INTRO_COMPLETED] ?: false
        }

    val isLanguageCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[IS_LANGUAGE_COMPLETED] ?: false
        }

    val isPermissionCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[IS_PERMISSION_COMPLETED] ?: false
        }

    val passWordVault: Flow<String> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[PASS_WORD_VAULT] ?: ""
        }

    val emailRecoveryPassword: Flow<String> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[EMAIL_RECOVERY_PASSWORD] ?: ""
        }

    val countryLanguage: Flow<String> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[COUNTRY_LANGUAGE] ?: ""
        }

    val keyLanguage: Flow<String> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[KEY_LANGUAGE] ?: ""
        }

    val keyAndCountry: Flow<Pair<String, String>> =
        countryLanguage.combine(keyLanguage) { country, key ->
            country to key
        }

    val questionRecoveryId: Flow<String> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[QUESTION_RECOVERY_ID] ?: ""
        }

    val questionRecoveryAnswer: Flow<String> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[QUESTION_RECOVERY_ANSWER] ?: ""
        }

    suspend fun saveIntroCompleted(completed: Boolean) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[IS_INTRO_COMPLETED] = completed
        }
    }

    suspend fun saveLanguageCompleted(completed: Boolean) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[IS_LANGUAGE_COMPLETED] = completed
        }
    }

    suspend fun savePermissionCompleted(completed: Boolean) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[IS_PERMISSION_COMPLETED] = completed
        }
    }

    fun runtimePermissionRequestCounts(
        permissions: Collection<String>
    ): Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        permissions.associateWith { permission ->
            prefs[runtimePermissionRequestCountKey(permission)] ?: 0
        }
    }

    suspend fun runtimePermissionRequestCount(permission: String): Int {
        val key = runtimePermissionRequestCountKey(permission)
        return context.dataStore.data.first()[key] ?: 0
    }

    suspend fun markRuntimePermissionsRequested(permissions: Collection<String>) {
        if (permissions.isEmpty()) return
        context.dataStore.edit { prefs ->
            permissions.forEach { permission ->
                val key = runtimePermissionRequestCountKey(permission)
                prefs[key] = (prefs[key] ?: 0) + 1
            }
        }
    }

    val notificationsJson: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[NOTIFICATIONS_JSON] ?: "" }

    suspend fun saveNotificationsJson(json: String) {
        context.dataStore.edit { prefs -> prefs[NOTIFICATIONS_JSON] = json }
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { prefs: Preferences ->
            prefs[IS_DARK_MODE] ?: false
        }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[IS_DARK_MODE] = enabled
        }
    }

    suspend fun savePassWordVault(password: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[PASS_WORD_VAULT] = password
        }
    }

    suspend fun saveEmailRecoveryPassword(email: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[EMAIL_RECOVERY_PASSWORD] = email
        }
    }

    suspend fun saveLanguage(key: String, country: String) {
        // Lưu vào SharedPreferences làm cache cho startup (truy cập đồng bộ nhanh)
        context.getSharedPreferences("language_cache", Context.MODE_PRIVATE).edit()
            .putString("key_language", key)
            .putString("country_language", country)
            .apply()

        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[KEY_LANGUAGE] = key
            prefs[COUNTRY_LANGUAGE] = country
        }
    }

    private fun runtimePermissionRequestCountKey(permission: String) = intPreferencesKey(
        "runtime_permission_request_count_${permission.substringAfterLast('.').lowercase()}"
    )

    suspend fun saveQuestionRecoveryId(id: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[QUESTION_RECOVERY_ID] = id
        }
    }

    suspend fun saveQuestionRecoveryAnswer(answer: String) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[QUESTION_RECOVERY_ANSWER] = answer
        }
    }

    val lastKnownMediaCount: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[LAST_KNOWN_MEDIA_COUNT] ?: -1 }

    suspend fun saveLastKnownMediaCount(count: Int) {
        context.dataStore.edit { prefs -> prefs[LAST_KNOWN_MEDIA_COUNT] = count }
    }

    val isDefaultBrowserAccepted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_DEFAULT_BROWSER_ACCEPTED] ?: false }

    suspend fun saveDefaultBrowserAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_DEFAULT_BROWSER_ACCEPTED] = accepted }
    }

    val selectedSearchEngine: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[SELECTED_SEARCH_ENGINE] ?: "google" }

    suspend fun saveSelectedSearchEngine(engine: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_SEARCH_ENGINE] = engine }
    }

    val isIncognitoDefault: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_INCOGNITO_DEFAULT] ?: false }

    suspend fun saveIncognitoDefault(incognito: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_INCOGNITO_DEFAULT] = incognito }
    }

    val lastUsedTabId: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[LAST_USED_TAB_ID] ?: -1L }

    suspend fun saveLastUsedTabId(tabId: Long) {
        context.dataStore.edit { prefs -> prefs[LAST_USED_TAB_ID] = tabId }
    }

    val sessionCount: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[SESSION_COUNT] ?: 0 }

    suspend fun saveSessionCount(count: Int) {
        context.dataStore.edit { prefs -> prefs[SESSION_COUNT] = count }
    }

    suspend fun incrementSessionCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[SESSION_COUNT] ?: 0
            prefs[SESSION_COUNT] = current + 1
        }
    }
}
