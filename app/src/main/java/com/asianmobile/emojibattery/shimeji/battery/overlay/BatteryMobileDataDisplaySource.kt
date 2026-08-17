@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.asianmobile.emojibattery.shimeji.battery.overlay

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BatteryMobileDataBadge(val label: String) {
    G("G"),
    EDGE("E"),
    G2("2G"),
    G3("3G"),
    H("H"),
    H_PLUS("H+"),
    G4("4G"),
    G4_PLUS("4G+"),
    G5("5G"),
    G5_PLUS("5G+")
}

internal object BatteryMobileDataDisplayMapper {
    fun map(networkType: Int, overrideNetworkType: Int): BatteryMobileDataBadge? {
        when (overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED ->
                return BatteryMobileDataBadge.G5_PLUS
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE ->
                return BatteryMobileDataBadge.G5
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO ->
                return BatteryMobileDataBadge.G4_PLUS
        }
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> BatteryMobileDataBadge.G
            TelephonyManager.NETWORK_TYPE_EDGE -> BatteryMobileDataBadge.EDGE
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> BatteryMobileDataBadge.G2
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> BatteryMobileDataBadge.G3
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA -> BatteryMobileDataBadge.H
            TelephonyManager.NETWORK_TYPE_HSPAP -> BatteryMobileDataBadge.H_PLUS
            TelephonyManager.NETWORK_TYPE_LTE -> BatteryMobileDataBadge.G4
            TelephonyManager.NETWORK_TYPE_NR -> BatteryMobileDataBadge.G5
            else -> null
        }
    }
}

@Singleton
class BatteryMobileDataMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    private val _badge = MutableStateFlow<BatteryMobileDataBadge?>(null)
    val badge: StateFlow<BatteryMobileDataBadge?> = _badge.asStateFlow()

    @Suppress("LeakingThis")
    private val source = BatteryMobileDataDisplaySource(context.applicationContext) {
        _badge.value = it
    }.also(BatteryMobileDataDisplaySource::start)
}

/**
 * Observes the carrier display technology used by Android's own status UI.
 *
 * Android 11+ exposes TelephonyDisplayInfo without a dangerous phone-state permission for apps
 * compiled against Android 12 or newer. Older releases deliberately return no badge instead of
 * asking for READ_PHONE_STATE for a cosmetic status-bar label.
 */
internal class BatteryMobileDataDisplaySource(
    private val context: Context,
    private val onChanged: (BatteryMobileDataBadge?) -> Unit
) {
    private var manager: TelephonyManager? = null
    private var callback: TelephonyCallback? = null
    private var listener: PhoneStateListener? = null

    fun start() {
        stop()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onChanged(null)
            return
        }
        val base = context.getSystemService(TelephonyManager::class.java) ?: run {
            onChanged(null)
            return
        }
        val activeSubscriptionId = SubscriptionManager.getActiveDataSubscriptionId()
        val selected = if (activeSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            runCatching { base.createForSubscriptionId(activeSubscriptionId) }.getOrDefault(base)
        } else {
            base
        }
        manager = selected
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val displayCallback = object : TelephonyCallback(),
                    TelephonyCallback.DisplayInfoListener {
                    override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                        onChanged(
                            BatteryMobileDataDisplayMapper.map(
                                info.networkType,
                                info.overrideNetworkType
                            )
                        )
                    }
                }
                callback = displayCallback
                selected.registerTelephonyCallback(context.mainExecutor, displayCallback)
            } else {
                val displayListener = object : PhoneStateListener(context.mainExecutor) {
                    override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                        onChanged(
                            BatteryMobileDataDisplayMapper.map(
                                info.networkType,
                                info.overrideNetworkType
                            )
                        )
                    }
                }
                listener = displayListener
                selected.listen(
                    displayListener,
                    PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
                )
            }
        }.onFailure { onChanged(null) }
    }

    fun stop() {
        val currentManager = manager
        callback?.let { currentCallback ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && currentManager != null) {
                runCatching { currentManager.unregisterTelephonyCallback(currentCallback) }
            }
        }
        listener?.let { currentListener ->
            if (currentManager != null) {
                runCatching { currentManager.listen(currentListener, PhoneStateListener.LISTEN_NONE) }
            }
        }
        callback = null
        listener = null
        manager = null
    }
}
