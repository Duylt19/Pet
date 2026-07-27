package com.asianmobile.emojibattery.shimeji.ui.home.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.BuildConfig
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.repository.PetSettingsRepository
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackRepository
import com.asianmobile.emojibattery.shimeji.utils.FeedbackLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val petSettingsRepository: PetSettingsRepository,
    private val petPackRepository: PetPackRepository
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
            combine(
                petSettingsRepository.preferences,
                petPackRepository.selectedPacks
            ) { preferences, selectedPacks ->
                preferences to selectedPacks
            }.collect { (preferences, selectedPacks) ->
                _uiState.update {
                    it.copy(
                        petCount = preferences.petCount,
                        petSlots = List(preferences.petCount) { slotIndex ->
                            val pack = selectedPacks.getOrNull(slotIndex)
                                ?: selectedPacks.first()
                            val slot = preferences.slot(slotIndex)
                            SettingsPetSlotUiState(
                                slotIndex = slotIndex,
                                name = pack.manifest.name,
                                previewImagePath = pack.previewImagePath(),
                                sizePercent = slot.sizePercent,
                                speedPercent = slot.speedPercent,
                                messagesEnabled = slot.messagesEnabled,
                                interactionEnabled = slot.interactionEnabled
                            )
                        }
                    )
                }
            }
        }
    }

    fun nextPetSlotForAdd(): Int? {
        val state = _uiState.value
        if (!state.canAddPet) return null
        return state.petCount
    }

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
