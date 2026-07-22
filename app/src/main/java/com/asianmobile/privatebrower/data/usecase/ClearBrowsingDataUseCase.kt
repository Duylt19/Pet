package com.asianmobile.privatebrower.data.usecase

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class BrowsingDataScope {
    NORMAL,
    PRIVATE,
    ALL
}

internal enum class OpenTabReloadScope {
    NONE,
    NORMAL,
    PRIVATE,
    ALL
}

data class ClearBrowsingDataOptions(
    val scope: BrowsingDataScope = BrowsingDataScope.NORMAL,
    val clearCookies: Boolean = false,
    val clearOpenTabs: Boolean = false,
    val clearHistory: Boolean = true,
    val clearCache: Boolean = true
) {
    val hasSelection: Boolean
        get() = clearCookies || clearOpenTabs || clearCache ||
            (clearHistory && scope != BrowsingDataScope.PRIVATE)

    fun forScope(newScope: BrowsingDataScope): ClearBrowsingDataOptions =
        if (newScope == BrowsingDataScope.PRIVATE) {
            copy(
                scope = newScope,
                clearCookies = false,
                clearCache = false
            )
        } else {
            copy(scope = newScope)
        }

    fun hasLimitedProfileIsolation(supportsProfileIsolation: Boolean): Boolean =
        clearCookies &&
            scope != BrowsingDataScope.ALL &&
            !supportsProfileIsolation

    val hasSharedCacheScope: Boolean
        get() = clearCache && scope != BrowsingDataScope.ALL

    internal fun openTabReloadScope(supportsProfileIsolation: Boolean): OpenTabReloadScope =
        when {
            clearCache -> OpenTabReloadScope.ALL
            clearCookies && !supportsProfileIsolation -> OpenTabReloadScope.ALL
            clearCookies && scope == BrowsingDataScope.NORMAL -> OpenTabReloadScope.NORMAL
            clearCookies && scope == BrowsingDataScope.PRIVATE -> OpenTabReloadScope.PRIVATE
            clearCookies && scope == BrowsingDataScope.ALL -> OpenTabReloadScope.ALL
            else -> OpenTabReloadScope.NONE
        }

    internal fun shouldCloseOpenTabs(isIncognito: Boolean): Boolean =
        clearOpenTabs && when (scope) {
            BrowsingDataScope.NORMAL -> !isIncognito
            BrowsingDataScope.PRIVATE -> isIncognito
            BrowsingDataScope.ALL -> true
        }
}

data class ClearBrowsingDataResult(
    val profileIsolationLimited: Boolean = false
)

class ClearBrowsingDataUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val supportsProfileIsolation: Boolean
        get() = false

    suspend operator fun invoke(options: ClearBrowsingDataOptions): ClearBrowsingDataResult {
        withContext(Dispatchers.Main.immediate) {
            if (options.clearCookies) {
                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
            }
            if (options.clearCache || options.clearHistory) {
                WebStorage.getInstance().deleteAllData()
                WebView(context).apply {
                    clearCache(true)
                    clearHistory()
                    destroy()
                }
            }
        }

        return ClearBrowsingDataResult(
            profileIsolationLimited = options.hasLimitedProfileIsolation(
                supportsProfileIsolation = false
            )
        )
    }
}
