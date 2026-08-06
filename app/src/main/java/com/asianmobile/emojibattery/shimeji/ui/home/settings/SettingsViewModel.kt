package com.asianmobile.emojibattery.shimeji.ui.home.settings

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.utils.FeedbackLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val batterySettingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionName = BuildConfig.VERSION_NAME,
            isAccessibilityEnabled = BatteryAccessibility.isEnabled(context)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var enableBatteryAfterAccessibility = false
    private var configuredBatteryEnabled = false

    init {
        viewModelScope.launch {
            batterySettingsRepository.config.collect { config ->
                configuredBatteryEnabled = config.enabled
                val accessibilityEnabled = BatteryAccessibility.isEnabled(context)
                _uiState.update {
                    it.copy(
                        isBatteryEnabled = config.enabled && accessibilityEnabled,
                        isAccessibilityEnabled = accessibilityEnabled
                    )
                }
            }
        }
    }

    fun onBatteryToggle() {
        if (!BatteryAccessibility.isEnabled(context)) {
            requestBatteryAccessibility(enableAfterGrant = true)
            return
        }
        batterySettingsRepository.setEnabled(!_uiState.value.isBatteryEnabled)
    }

    fun requestBatteryAccessibility(enableAfterGrant: Boolean) {
        enableBatteryAfterAccessibility = enableAfterGrant
        _effects.trySend(SettingsEffect.RequestBatteryAccessibility)
    }

    fun refreshAccessibility() {
        val enabled = BatteryAccessibility.isEnabled(context)
        _uiState.update {
            it.copy(
                isBatteryEnabled = configuredBatteryEnabled && enabled,
                isAccessibilityEnabled = enabled
            )
        }
        if (enabled && enableBatteryAfterAccessibility) {
            enableBatteryAfterAccessibility = false
            batterySettingsRepository.setEnabled(true)
        }
    }

    fun cancelPendingBatteryEnable() {
        enableBatteryAfterAccessibility = false
    }

    fun onContactClicked(context: Context) {
        FeedbackLauncher.launch(context)
    }

    fun onPrivacyClicked(context: Context) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            context.getString(R.string.privacy_policy_default_url).toUri()
        )
        runCatching { context.startActivity(intent) }
    }

    internal fun sendRateFeedback(
        context: Context,
        feedbackState: RateAppUiState
    ) {
        if (!feedbackState.canSendFeedback()) return

        val snapshot = feedbackState.copy(
            feedbackOptions = feedbackState.feedbackOptions.map { it.copy() }
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                RateFeedbackEmailSender.send(context.applicationContext, snapshot)
            }
        }
    }

    fun onShareClicked(context: Context) {
        val playStoreUrl =
            "https://play.google.com/store/apps/details?id=${context.packageName}"
        val shareText = context.getString(R.string.share_app_message, playStoreUrl)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_app_chooser_title))
        )
    }
}
