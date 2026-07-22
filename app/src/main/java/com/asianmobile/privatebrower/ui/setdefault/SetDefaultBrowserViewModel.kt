package com.asianmobile.privatebrower.ui.setdefault

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.repository.PreferencesRepository
import com.asianmobile.privatebrower.utils.DefaultBrowserHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetDefaultBrowserViewModel @Inject constructor(
    private val defaultBrowserHelper: DefaultBrowserHelper,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetDefaultBrowserUiState())
    val uiState: StateFlow<SetDefaultBrowserUiState> = _uiState.asStateFlow()

    private val _navigateEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateEvent: SharedFlow<Unit> = _navigateEvent.asSharedFlow()

    init {
        _uiState.value = SetDefaultBrowserUiState(
            isAlreadyDefault = defaultBrowserHelper.isDefaultBrowser()
        )
    }

    fun onSetDefaultClicked(
        launcher: ActivityResultLauncher<Intent>,
        context: Context
    ) {
        val intent = defaultBrowserHelper.createRequestIntent()
        try {
            launcher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.setdefault_fallback_toast, Toast.LENGTH_LONG).show()
            continueForCurrentSession()
        }
    }

    fun onRoleResult(granted: Boolean) {
        if (granted || defaultBrowserHelper.isDefaultBrowser()) {
            markAcceptedAndNavigate()
        } else {
            continueForCurrentSession()
        }
    }

    fun onLaterClicked() {
        continueForCurrentSession()
    }

    private fun markAcceptedAndNavigate() = viewModelScope.launch {
        preferencesRepository.setDefaultBrowserAccepted(true)
        _navigateEvent.tryEmit(Unit)
    }

    private fun continueForCurrentSession() {
        _navigateEvent.tryEmit(Unit)
    }
}
