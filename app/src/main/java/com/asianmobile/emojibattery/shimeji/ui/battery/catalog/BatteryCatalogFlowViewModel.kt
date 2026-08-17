package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import android.content.Context
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryCatalogRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Destination-scoped owner for the reusable Battery unlock and permission flow.
 *
 * Discover and Search each receive their own instance. The Home Battery tab uses
 * [com.asianmobile.emojibattery.shimeji.ui.home.battery.BatteryHomeViewModel] instead, so a
 * feature surface never depends on another surface's ViewModel.
 */
@HiltViewModel
class BatteryCatalogFlowViewModel @Inject constructor(
    @ApplicationContext context: Context,
    catalogRepository: BatteryCatalogRepository,
    settingsRepository: BatterySettingsRepository
) : BatteryCatalogViewModel(context, catalogRepository, settingsRepository)
