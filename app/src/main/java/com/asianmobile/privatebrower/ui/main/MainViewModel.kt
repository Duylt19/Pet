package com.asianmobile.privatebrower.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.ads.R
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.utils.DefaultBrowserHelper
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Trạng thái UI tổng hợp cho màn hình chính, giúp giảm thiểu recomposition.
 */
data class MainUiState(
    val isLanguageCompleted: Boolean? = null,
    val isIntroCompleted: Boolean? = null,
    val isPermissionCompleted: Boolean? = null,
    val isDefaultBrowserAccepted: Boolean? = null,
    val isAlreadyDefaultBrowser: Boolean = false,
    val isLoading: Boolean = true
) {
    /** Tính toán màn hình tiếp theo dựa trên các trạng thái hoàn thành */
    fun getNextScreen() =
        when {
            isLanguageCompleted != true -> Routes.LANGUAGE
            isIntroCompleted != true -> Routes.INTRO
            isDefaultBrowserAccepted != true && !isAlreadyDefaultBrowser -> Routes.SET_DEFAULT_BROWSER
            isPermissionCompleted != true -> Routes.PERMISSION
            else -> Routes.HOME
        }

    fun getNextScreenAfterDefaultBrowser() =
        if (isPermissionCompleted == true) Routes.HOME else Routes.PERMISSION

    /** Kiểm tra xem tất cả dữ liệu khởi tạo đã sẵn sàng chưa */
    fun isReady(): Boolean = isLanguageCompleted != null &&
            isIntroCompleted != null &&
            isPermissionCompleted != null &&
            isDefaultBrowserAccepted != null
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val defaultBrowserHelper: DefaultBrowserHelper
) : ViewModel() {

    /** Gom nhóm các flow từ DataStore thành một state duy nhất */
    internal val uiState: StateFlow<MainUiState> = combine(
        dataStoreManager.isLanguageCompleted,
        dataStoreManager.isIntroCompleted,
        dataStoreManager.isPermissionCompleted,
        dataStoreManager.isDefaultBrowserAccepted
    ) { language, intro, permission, defaultAccepted ->
        MainUiState(
            isLanguageCompleted = language,
            isIntroCompleted = intro,
            isPermissionCompleted = permission,
            isDefaultBrowserAccepted = defaultAccepted,
            isAlreadyDefaultBrowser = defaultBrowserHelper.isDefaultBrowser(),
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

    internal fun completeIntro() =
        viewModelScope.launch { dataStoreManager.saveIntroCompleted(true) }

    internal fun completeLanguage() =
        viewModelScope.launch { dataStoreManager.saveLanguageCompleted(true) }

    internal fun completePermission() =
        viewModelScope.launch { dataStoreManager.savePermissionCompleted(true) }

    internal fun getNextScreenAfterIntro(): String =
        if (uiState.value.isDefaultBrowserAccepted == true || defaultBrowserHelper.isDefaultBrowser()) {
            Routes.PERMISSION
        } else {
            Routes.SET_DEFAULT_BROWSER
        }

    internal fun getNextScreenAfterDefaultBrowser(): String =
        uiState.value.getNextScreenAfterDefaultBrowser()
}
