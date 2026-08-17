package com.asianmobile.emojibattery.shimeji.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.local.DataStoreManager
import com.asianmobile.emojibattery.shimeji.data.repository.RemoteConfigRepository
import com.asianmobile.emojibattery.shimeji.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isLanguageCompleted: Boolean? = null,
    val isIntroCompleted: Boolean? = null,
    val isPermissionCompleted: Boolean? = null,
    val isLoading: Boolean = true
) {
    fun getNextScreen(): String = when {
        isLanguageCompleted != true -> Routes.LANGUAGE
        isIntroCompleted != true -> Routes.INTRO
        IS_FIRST_PERMISSION_ONBOARDING_ENABLED && isPermissionCompleted != true ->
            Routes.PERMISSION
        else -> Routes.HOME_GRAPH
    }

    fun isReady(): Boolean =
        isLanguageCompleted != null &&
            isIntroCompleted != null &&
            (!IS_FIRST_PERMISSION_ONBOARDING_ENABLED || isPermissionCompleted != null)
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val remoteConfigRepository: RemoteConfigRepository,
) : ViewModel() {

    private val _hasRequestedHomeNotificationThisSession = MutableStateFlow(false)
    internal val hasRequestedHomeNotificationThisSession: StateFlow<Boolean> =
        _hasRequestedHomeNotificationThisSession.asStateFlow()

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
            onComplete(remoteConfigRepository.refresh())
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

    internal fun markNotificationPermissionRequested() {
        if (_hasRequestedHomeNotificationThisSession.value) return
        _hasRequestedHomeNotificationThisSession.value = true
        viewModelScope.launch {
            dataStoreManager.saveNotificationPermissionRequested(true)
        }
    }
}
