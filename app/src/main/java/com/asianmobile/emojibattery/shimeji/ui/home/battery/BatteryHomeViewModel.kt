package com.asianmobile.emojibattery.shimeji.ui.home.battery

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Owns state and actions for the Battery top-level Home tab and its category detail. */
@HiltViewModel
class BatteryHomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    catalogRepository: BatteryCatalogRepository,
    settingsRepository: BatterySettingsRepository
) : BatteryCatalogViewModel(context, catalogRepository, settingsRepository) {
    val uiState: StateFlow<BatteryHomeUiState> = catalogState
        .map(::BatteryHomeUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BatteryHomeUiState(catalogState.value)
        )
}
