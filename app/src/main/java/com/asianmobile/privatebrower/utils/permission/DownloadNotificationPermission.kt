package com.asianmobile.privatebrower.utils.permission

import android.os.Build
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

object DownloadNotificationPermissionRequests {
    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    val requests: Flow<Unit> = requestChannel.receiveAsFlow()

    fun onDownloadStarted() {
        requestChannel.trySend(Unit)
    }
}

object DownloadNotificationPermissionPolicy {
    fun shouldRequest(
        sdkInt: Int,
        isGranted: Boolean,
        requestCount: Int
    ): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        !isGranted &&
        requestCount == 0
}
