package com.asianmobile.privatebrower.ui.home.settings

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.BuildConfig
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.model.SearchEngine
import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import com.asianmobile.privatebrower.data.usecase.ClearBrowsingDataUseCase
import com.asianmobile.privatebrower.data.usecase.ClearBrowsingDataOptions
import com.asianmobile.privatebrower.utils.DefaultBrowserHelper
import com.asianmobile.privatebrower.utils.FeedbackLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val searchEngineRepository: SearchEngineRepository,
    private val defaultBrowserHelper: DefaultBrowserHelper,
    private val clearBrowsingDataUseCase: ClearBrowsingDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionName = BuildConfig.VERSION_NAME,
            profileIsolationSupported = clearBrowsingDataUseCase.supportsProfileIsolation
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSearchEngine()
        checkDefaultBrowser()
    }

    private fun observeSearchEngine() {
        viewModelScope.launch {
            searchEngineRepository.observeCurrent().collect { engine ->
                _uiState.update { it.copy(currentSearchEngine = engine) }
            }
        }
    }

    private fun checkDefaultBrowser() {
        _uiState.update { it.copy(isDefaultBrowser = defaultBrowserHelper.isDefaultBrowser()) }
    }

    fun refreshDefaultBrowserStatus() {
        checkDefaultBrowser()
    }

    fun onSetDefaultClicked(): Intent {
        return defaultBrowserHelper.createRequestIntent()
    }

    fun onSearchEngineClicked() {
        _uiState.update { it.copy(showSearchEngineSheet = true) }
    }

    fun onSearchEngineSelected(engine: SearchEngine) {
        viewModelScope.launch {
            searchEngineRepository.setCurrent(engine)
            _uiState.update { it.copy(showSearchEngineSheet = false) }
        }
    }

    fun onDismissSearchEngineSheet() {
        _uiState.update { it.copy(showSearchEngineSheet = false) }
    }

    fun onClearHistoryClicked() {
        _uiState.update {
            it.copy(
                showClearHistorySheet = true,
                clearHistoryOptions = ClearBrowsingDataOptions()
            )
        }
    }

    fun onClearHistoryOptionsChanged(options: ClearBrowsingDataOptions) {
        _uiState.update { it.copy(clearHistoryOptions = options) }
    }

    fun onConfirmClearHistory(context: Context) {
        val options = _uiState.value.clearHistoryOptions
        if (!options.hasSelection || _uiState.value.isClearingBrowsingData) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingBrowsingData = true) }
            val appContext = context.applicationContext
            try {
                val result = clearBrowsingDataUseCase(options)
                _uiState.update {
                    it.copy(showClearHistorySheet = false, isClearingBrowsingData = false)
                }
                val message = if (result.profileIsolationLimited) {
                    R.string.clear_history_profile_limited_toast
                } else {
                    R.string.clear_history_done_toast
                }
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Failed to clear browsing data", error)
                _uiState.update { it.copy(isClearingBrowsingData = false) }
                Toast.makeText(
                    appContext,
                    R.string.clear_history_failed_toast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onDismissClearHistorySheet() {
        if (_uiState.value.isClearingBrowsingData) return
        _uiState.update { it.copy(showClearHistorySheet = false) }
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

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
