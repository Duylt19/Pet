package com.asianmobile.emojibattery.shimeji.ui.home.battery

import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogUiState

/** Complete render state owned by the Battery tab in the Home shell. */
data class BatteryHomeUiState(
    val catalog: BatteryCatalogUiState = BatteryCatalogUiState()
)
