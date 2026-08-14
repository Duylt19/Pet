package com.asianmobile.emojibattery.shimeji.battery.settings

/**
 * The stored answer to "does the user want the status bar on right now".
 *
 * [enabled] is the user's setting, not the permission: `BatteryAccessibility.isEnabled()` is what
 * the system allows, and the two are deliberately separate everywhere in this feature.
 *
 * [pendingAccessibilityGrant] marks an [enabled] that was written *before* the Accessibility
 * permission existed. It is the only thing that makes the optimistic write safe: without it,
 * nothing could tell an intent the user never completed apart from a bar the platform took away.
 */
data class BatteryEnableIntent(
    val enabled: Boolean,
    val pendingAccessibilityGrant: Boolean = false
)

/**
 * Decides what the app stores when the user asks for the status bar, and what it stores again once
 * the permission is observable.
 *
 * The bar can only be drawn by `StatusBarAccessibilityService`, which attaches as soon as it is
 * bound and the stored config says `enabled`. Storing the intent *after* the user comes back from
 * system Accessibility settings therefore means the bar never appears while they are still looking
 * at the switch they just flipped. So the intent is committed **before** the hand-off, and this
 * policy owns the two halves of that trade:
 *
 * - [requestEnable] — what to store at hand-off time.
 * - [settle] — what to store the next time the permission state can be read: returning from
 *   settings, any resume, a cold start after the process was killed in settings, or the service
 *   itself being bound.
 *
 * Nothing here is Android-specific on purpose; the caller supplies the permission state.
 */
object BatteryEnableIntentPolicy {
    /**
     * The intent to persist the moment the user asks for the bar.
     *
     * [BatteryEnableIntent.pendingAccessibilityGrant] is set only when this request is what turned
     * the bar on. An intent that was already stored and confirmed — a user whose bar is on but
     * whose permission a force-stop stripped — is left unmarked, so declining in settings can never
     * switch off a bar the user had chosen earlier.
     *
     * Idempotent on purpose: the hand-off writes at more than one step (the toggle, then the
     * disclosure's Allow), and a request that is already pending has to stay pending, or the second
     * write would quietly make the first one irreversible.
     */
    fun requestEnable(
        stored: BatteryEnableIntent,
        isAccessibilityGranted: Boolean
    ): BatteryEnableIntent = BatteryEnableIntent(
        enabled = true,
        pendingAccessibilityGrant = !isAccessibilityGranted &&
            (!stored.enabled || stored.pendingAccessibilityGrant)
    )

    /**
     * An explicit on/off from a user who can already see the bar settles the question outright:
     * there is nothing optimistic left to take back.
     */
    fun setEnabled(enabled: Boolean): BatteryEnableIntent =
        BatteryEnableIntent(enabled = enabled, pendingAccessibilityGrant = false)

    /**
     * What to persist once the permission state is observable again, or null when the stored
     * intent already tells the truth and must not be touched.
     *
     * Granted confirms the optimistic write; still missing takes it back, so a user who walked
     * into settings and changed their mind is not left with a stored "on" they never completed.
     */
    fun settle(
        stored: BatteryEnableIntent,
        isAccessibilityGranted: Boolean
    ): BatteryEnableIntent? = when {
        !stored.pendingAccessibilityGrant -> null
        isAccessibilityGranted -> stored.copy(pendingAccessibilityGrant = false)
        else -> BatteryEnableIntent(enabled = false, pendingAccessibilityGrant = false)
    }
}
