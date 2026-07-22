package com.asianmobile.privatebrower.utils.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object AllFilesAccess {

    fun isSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.R

    fun isGranted(): Boolean =
        isSupported() && Environment.isExternalStorageManager()

    fun settingsIntent(context: Context): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        val appSettingsIntent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            packageUri
        )
        if (appSettingsIntent.resolveActivity(context.packageManager) != null) {
            return appSettingsIntent
        }

        val allFilesIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        if (allFilesIntent.resolveActivity(context.packageManager) != null) {
            return allFilesIntent
        }

        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }
}

object LegacyStorageAccess {

    fun isSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt <= Build.VERSION_CODES.Q

    fun isGranted(context: Context): Boolean {
        if (!isSupported()) return false
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
            !Environment.isExternalStorageLegacy()
        ) {
            return false
        }

        return arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}

object BroadStorageAccess {

    fun isGranted(context: Context): Boolean =
        if (AllFilesAccess.isSupported()) {
            AllFilesAccess.isGranted()
        } else {
            LegacyStorageAccess.isGranted(context)
        }
}

fun Context.canShowPermissionRationale(permissions: Array<String>): Boolean {
    val activity = findActivity() ?: return false
    return permissions.any { permission ->
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
