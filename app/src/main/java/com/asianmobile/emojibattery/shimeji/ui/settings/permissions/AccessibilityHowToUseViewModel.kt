package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccessibilityHowToUseViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AccessibilityHowToUseUiState())
    val uiState = _uiState.asStateFlow()
}
