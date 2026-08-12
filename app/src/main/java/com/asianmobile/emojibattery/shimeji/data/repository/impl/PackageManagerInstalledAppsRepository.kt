package com.asianmobile.emojibattery.shimeji.data.repository.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import com.asianmobile.emojibattery.shimeji.data.model.InstalledApp
import com.asianmobile.emojibattery.shimeji.data.repository.InstalledAppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PackageManagerInstalledAppsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : InstalledAppsRepository {

    override suspend fun getLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        queryLauncherActivities(packageManager, launcherIntent)
            .asSequence()
            .filter { it.activityInfo?.packageName != context.packageName }
            .distinctBy { it.activityInfo?.packageName }
            .mapNotNull { info -> info.toInstalledApp(packageManager) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, InstalledApp::label))
            .toList()
    }

    private fun ResolveInfo.toInstalledApp(packageManager: PackageManager): InstalledApp? {
        val packageName = activityInfo?.packageName?.trim().orEmpty()
        if (packageName.isEmpty()) return null
        val label = loadLabel(packageManager).toString().trim().ifEmpty { packageName }
        val icon = runCatching {
            loadIcon(packageManager).toBitmap(
                width = APP_ICON_BITMAP_SIZE,
                height = APP_ICON_BITMAP_SIZE
            )
        }.getOrNull()
        return InstalledApp(packageName = packageName, label = label, icon = icon)
    }

    @Suppress("DEPRECATION")
    private fun queryLauncherActivities(
        packageManager: PackageManager,
        intent: Intent
    ): List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        )
    } else {
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }

    private companion object {
        const val APP_ICON_BITMAP_SIZE = 96
    }
}
