package com.asianmobile.emojibattery.shimeji.pet.overlay

/** A vendor's own auto-start or protected-apps screen, addressed by component. */
data class PetVendorPowerScreen(
    val packageName: String,
    val className: String
)

/**
 * Several vendors keep their own allowlist beside Android's battery-optimisation exemption, and
 * only that list decides whether a foreground service survives. The platform exemption does not
 * touch it and there is no API to read or set it, so the best the app can do is take the user to
 * the right screen.
 *
 * These components are not API. They move between ROM versions, so every entry is resolved
 * against the package manager before it is offered and the app falls back to the next candidate,
 * or shows nothing at all. Each package here must also appear in the manifest `<queries>` block,
 * or API 30+ reports them all as missing.
 */
object PetVendorPowerSettings {
    val CANDIDATES: List<PetVendorPowerScreen> = listOf(
        // MIUI / HyperOS: explicit Background autostart management.
        PetVendorPowerScreen(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        ),
        // EMUI / MagicOS: startup manager on newer builds, protected apps on older ones.
        PetVendorPowerScreen(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        ),
        PetVendorPowerScreen(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
        ),
        PetVendorPowerScreen(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        ),
        // ColorOS, and the older Oppo security centre.
        PetVendorPowerScreen(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ),
        PetVendorPowerScreen(
            "com.coloros.safecenter",
            "com.coloros.safecenter.startupapp.StartupAppListActivity"
        ),
        PetVendorPowerScreen(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        ),
        // Funtouch OS / OriginOS.
        PetVendorPowerScreen(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ),
        PetVendorPowerScreen(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        ),
        // OxygenOS.
        PetVendorPowerScreen(
            "com.oneplus.security",
            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
        ),
        // Flyme.
        PetVendorPowerScreen("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
        // ZenUI.
        PetVendorPowerScreen(
            "com.asus.mobilemanager",
            "com.asus.mobilemanager.autostart.AutoStartActivity"
        ),
        // EUI.
        PetVendorPowerScreen(
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.AutobootManageActivity"
        )
    )
}
