package com.asianmobile.emojibattery.shimeji.ui.settings.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetBackgroundRestrictionReader
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetBatteryOptimizationPolicy
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.pet.overlay.shouldOfferVendorAllowlist
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GrantPermissionsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val restrictionReader: PetBackgroundRestrictionReader
) : ViewModel() {
    private val _uiState = MutableStateFlow(GrantPermissionsUiState())
    val uiState: StateFlow<GrantPermissionsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<GrantPermissionsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        refresh()
    }

    /** Every permission here is granted on a system screen, so re-read them on every resume. */
    fun refresh() {
        val signals = restrictionReader.read()
        _uiState.value = GrantPermissionsUiState(
            isAccessibilityEnabled = BatteryAccessibility.isEnabled(context),
            isOverlayGranted = PetOverlay.canDraw(context),
            isBatteryOptimizationIgnored = signals.isAlreadyIgnoringOptimization,
            isBatteryRowVisible = PetBatteryOptimizationPolicy.isExemptionRelevant(signals),
            isAutoStartRowVisible = signals.shouldOfferVendorAllowlist(),
            isNotificationGranted = isNotificationGranted(),
            isNotificationRowVisible = NOTIFICATION_PERMISSION_EXISTS
        )
    }

    /** Resolved at tap time, so a ROM update between load and tap cannot send a dead intent. */
    fun vendorAutoStartIntent() = restrictionReader.vendorPowerIntent()

    fun onTargetClicked(target: GrantPermissionsTarget) {
        val state = _uiState.value
        val effect = when (target) {
            GrantPermissionsTarget.ACCESSIBILITY ->
                accessibilityTargetEffect(state.isAccessibilityEnabled)

            GrantPermissionsTarget.OVERLAY ->
                GrantPermissionsEffect.OpenOverlaySettings.takeUnless { state.isOverlayGranted }

            GrantPermissionsTarget.VENDOR_AUTO_START ->
                GrantPermissionsEffect.OpenVendorAutoStartSettings

            // Always opens the list: the user may be turning it off again.
            GrantPermissionsTarget.BATTERY_OPTIMIZATION ->
                GrantPermissionsEffect.OpenBatteryOptimizationSettings

            // Once denied, the runtime prompt never shows again, so send the user to settings.
            GrantPermissionsTarget.NOTIFICATION -> when {
                state.isNotificationGranted -> GrantPermissionsEffect.OpenAppNotificationSettings
                hasAskedForNotification -> GrantPermissionsEffect.OpenAppNotificationSettings
                else -> GrantPermissionsEffect.RequestNotificationPermission
            }
        } ?: return
        if (effect == GrantPermissionsEffect.RequestNotificationPermission) {
            hasAskedForNotification = true
        }
        viewModelScope.launch { _effects.send(effect) }
    }

    private var hasAskedForNotification = false


    private fun isNotificationGranted(): Boolean = !NOTIFICATION_PERMISSION_EXISTS ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        val NOTIFICATION_PERMISSION_EXISTS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
