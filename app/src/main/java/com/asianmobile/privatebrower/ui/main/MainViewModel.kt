package com.asianmobile.privatebrower.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.ads.R
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.navigation.Routes
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class MainUiState(
    val isLanguageCompleted: Boolean? = null,
    val isIntroCompleted: Boolean? = null,
    val isPermissionCompleted: Boolean? = null,
    val isLoading: Boolean = true
) {
    fun getNextScreen(): String = when {
        isLanguageCompleted != true -> Routes.LANGUAGE
        isIntroCompleted != true -> Routes.INTRO
        isPermissionCompleted != true -> Routes.PERMISSION
        else -> Routes.HOME
    }

    fun isReady(): Boolean =
        isLanguageCompleted != null &&
            isIntroCompleted != null &&
            isPermissionCompleted != null
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    internal val uiState: StateFlow<MainUiState> = combine(
        dataStoreManager.isLanguageCompleted,
        dataStoreManager.isIntroCompleted,
        dataStoreManager.isPermissionCompleted
    ) { language, intro, permission ->
        MainUiState(
            isLanguageCompleted = language,
            isIntroCompleted = intro,
            isPermissionCompleted = permission,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState()
    )

    internal fun refreshRemoteConfig(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0L)
                    .build()

                remoteConfig.setConfigSettingsAsync(configSettings).await()
                remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()

                withTimeout(5_000) {
                    remoteConfig.fetchAndActivate().await()
                }
            }
            onComplete(result.isSuccess)
        }
    }

    internal fun completeLanguage() {
        viewModelScope.launch { dataStoreManager.saveLanguageCompleted(true) }
    }

    internal fun completeIntro() {
        viewModelScope.launch { dataStoreManager.saveIntroCompleted(true) }
    }

    internal fun completePermission() {
        viewModelScope.launch { dataStoreManager.savePermissionCompleted(true) }
    }
}
