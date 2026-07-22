package com.asianmobile.privatebrower.ui.home.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.BuildConfig
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.repository.PetSettingsRepository
import com.asianmobile.privatebrower.pet.settings.PetSettingsPolicy
import com.asianmobile.privatebrower.utils.FeedbackLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val petSettingsRepository: PetSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionName = BuildConfig.VERSION_NAME,
            maxPets = petSettingsRepository.performanceBudget.maxPets
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            petSettingsRepository.preferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        petCount = preferences.petCount,
                        sizePercent = preferences.sizePercent,
                        speedPercent = preferences.speedPercent,
                        soundEnabled = preferences.soundEnabled,
                        interactionEnabled = preferences.interactionEnabled
                    )
                }
            }
        }
    }

    fun decreasePetCount() = petSettingsRepository.updatePetCount(_uiState.value.petCount - 1)

    fun increasePetCount() = petSettingsRepository.updatePetCount(_uiState.value.petCount + 1)

    fun decreaseSize() = petSettingsRepository.updateSizePercent(
        _uiState.value.sizePercent - PetSettingsPolicy.SIZE_STEP_PERCENT
    )

    fun increaseSize() = petSettingsRepository.updateSizePercent(
        _uiState.value.sizePercent + PetSettingsPolicy.SIZE_STEP_PERCENT
    )

    fun decreaseSpeed() = petSettingsRepository.updateSpeedPercent(
        _uiState.value.speedPercent - PetSettingsPolicy.SPEED_STEP_PERCENT
    )

    fun increaseSpeed() = petSettingsRepository.updateSpeedPercent(
        _uiState.value.speedPercent + PetSettingsPolicy.SPEED_STEP_PERCENT
    )

    fun setSoundEnabled(enabled: Boolean) = petSettingsRepository.updateSoundEnabled(enabled)

    fun setInteractionEnabled(enabled: Boolean) =
        petSettingsRepository.updateInteractionEnabled(enabled)

    fun onFeedbackClicked(context: Context) {
        FeedbackLauncher.launch(context)
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
