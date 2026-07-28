package com.asianmobile.emojibattery.shimeji

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.asianmobile.emojibattery.shimeji.ads.BuildConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.data.remote.PetServerConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application(),
    Application.ActivityLifecycleCallbacks,
    coil.ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        // Adjust initialization
        initAdjust()
    }

    override fun newImageLoader(): coil.ImageLoader {
        return coil.ImageLoader.Builder(this)
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val builder = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                        if (PetServerConfig.isPetServerUrl(
                                host = original.url.host,
                                encodedPath = original.url.encodedPath
                            )
                        ) {
                            SafeRemoteConfig.getSensitiveString(
                                PetServerConfig.REMOTE_CONFIG_TOKEN_KEY
                            ).trim().takeIf(String::isNotEmpty)?.let { token ->
                                builder.header("Authorization", "Bearer $token")
                            }
                        }
                        chain.proceed(builder.build())
                    }
                    .build()
            }
            .build()
    }

    private fun initAdjust() {
        val appToken = getString(R.string.adjust_token)
        val environment = if (BuildConfig.DEBUG) {
            AdjustConfig.ENVIRONMENT_SANDBOX
        } else {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        }
        val config = AdjustConfig(this, appToken, environment)
        config.setLogLevel(if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.ERROR)

        config.enablePreinstallTracking()
        Adjust.initSdk(config)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

