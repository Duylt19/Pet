package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.util.Log
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.data.remote.PetServerConfig
import com.asianmobile.emojibattery.shimeji.data.repository.RemoteConfigRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

@Singleton
class FirebaseRemoteConfigRepository @Inject constructor() : RemoteConfigRepository {
    override fun hasPetServerToken(): Boolean =
        SafeRemoteConfig.getSensitiveString(PetServerConfig.REMOTE_CONFIG_TOKEN_KEY)
            .isNotBlank()

    override suspend fun refresh(): Boolean = try {
        withTimeout(REMOTE_CONFIG_TIMEOUT_MILLIS) {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0L)
                .build()
            remoteConfig.setConfigSettingsAsync(settings).await()
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()
            remoteConfig.fetchAndActivate().await()
            true
        }
    } catch (error: TimeoutCancellationException) {
        Log.w(TAG, "Remote Config refresh timed out")
        false
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.w(TAG, "Unable to refresh Remote Config", error)
        false
    }

    private companion object {
        const val TAG = "RemoteConfigRepository"
        const val REMOTE_CONFIG_TIMEOUT_MILLIS = 5_000L
    }
}
