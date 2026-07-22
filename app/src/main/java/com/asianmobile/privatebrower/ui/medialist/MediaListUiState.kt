package com.asianmobile.privatebrower.ui.medialist

import android.os.Build
import com.asianmobile.privatebrower.ui.permission.PermissionPolicy

enum class MediaAccessState {
    CHECKING,
    GRANTED,
    DENIED
}

internal fun mediaLibraryPermissions(): Array<String> =
    PermissionPolicy.onboardingStoragePermissions(Build.VERSION.SDK_INT)
