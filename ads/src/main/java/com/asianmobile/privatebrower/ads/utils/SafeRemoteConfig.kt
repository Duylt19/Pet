package com.asianmobile.privatebrower.ads.utils

import com.asianmobile.privatebrower.ads.config.RC_PREMIUM_ONBOARDING_FIRST
import com.asianmobile.privatebrower.ads.config.RC_PREMIUM_SPLASH_RETURN
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Safe wrapper for FirebaseRemoteConfig that handles cases where
 * Firebase hasn't been initialized yet (e.g., during early Compose composition).
 */
object SafeRemoteConfig {

    private fun getInstance(): FirebaseRemoteConfig? = try {
        val instance = FirebaseRemoteConfig.getInstance()
        instance
    } catch (e: Exception) {
        android.util.Log.e("SafeRemoteConfig", "FirebaseRemoteConfig.getInstance() failed: ${e.message}")
        null
    }

    fun getBoolean(key: String, defaultValue: Boolean = true): Boolean {
        val value = getInstance()?.getBoolean(key) ?: defaultValue
        android.util.Log.d("SafeRemoteConfig", "getBoolean: $key = $value")
        return value
    }

    fun getString(key: String, defaultValue: String = ""): String {
        val value = getInstance()?.getString(key) ?: defaultValue
        android.util.Log.d("SafeRemoteConfig", "getString: $key = $value")
        return value
    }

    fun getSensitiveString(key: String, defaultValue: String = ""): String {
        val value = getInstance()?.getString(key) ?: defaultValue
        val state = if (value.isBlank()) "[EMPTY]" else "[REDACTED]"
        android.util.Log.d("SafeRemoteConfig", "getSensitiveString: $key = $state")
        return value
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        val instance = getInstance()
        if (instance == null) {
            android.util.Log.e("SafeRemoteConfig", "getLong: $key fallback to default $defaultValue (Instance NULL)")
            return defaultValue
        }
        val value = instance.getLong(key)
        android.util.Log.d("SafeRemoteConfig", "getLong: $key = $value")
        return value
    }

    fun isShowPremiumOnboardingFirst(): Boolean =
        runCatching {
            SafeRemoteConfig.getBoolean(RC_PREMIUM_ONBOARDING_FIRST)
        }.getOrDefault(false)

    fun isShowPremiumSplashReturn(): Boolean =
        runCatching {
            SafeRemoteConfig.getBoolean(RC_PREMIUM_SPLASH_RETURN)
        }.getOrDefault(false)
}

