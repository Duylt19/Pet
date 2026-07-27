package com.asianmobile.emojibattery.shimeji.ui.language

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/9/2026
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val application: Application,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _languages = MutableStateFlow<List<Language>>(listOf())
    val languages: StateFlow<List<Language>> = _languages
    var languageSelected: Language? = null

    init {
        loadLanguage()
    }

    internal fun loadLanguage() {
        _languages.value = application.mockData()
    }

    internal fun updateLanguage(callback: () -> Unit) {
        viewModelScope.launch {
            languageSelected?.let { language ->
                dataStoreManager.saveLanguage(language.key, language.country)
                withContext(Dispatchers.Main) {
                    callback()
                }
            }
        }
    }
}


