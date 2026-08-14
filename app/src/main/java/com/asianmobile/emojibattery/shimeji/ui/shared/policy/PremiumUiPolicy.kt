package com.asianmobile.emojibattery.shimeji.ui.shared.policy

/**
 * Premium purchase entry points are intentionally hidden for the v1 launch.
 *
 * Keep the underlying entitlement/navigation code intact so the product can restore the UI in
 * one place after billing is approved for a later release.
 */
internal object PremiumUiPolicy {
    const val isPremiumEntryVisible: Boolean = false
}
