package com.asianmobile.privatebrower.data.usecase

import com.asianmobile.privatebrower.data.browser.BrowserEngine
import com.asianmobile.privatebrower.data.browser.TabManager
import com.asianmobile.privatebrower.data.repository.HistoryRepository
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
    private val historyRepository: HistoryRepository,
    private val tabManager: TabManager,
    private val browserEngine: BrowserEngine
) {
    val supportsProfileIsolation: Boolean
        get() = tabManager.supportsProfileIsolation()

    suspend operator fun invoke(options: ClearBrowsingDataOptions): ClearBrowsingDataResult {
        val includesNormal = options.scope != BrowsingDataScope.PRIVATE
        val includesPrivate = options.scope != BrowsingDataScope.NORMAL
        val profileIsolationSupported = supportsProfileIsolation
        val limitedIsolation = options.hasLimitedProfileIsolation(profileIsolationSupported)

        withContext(Dispatchers.Main.immediate) {
            when (options.openTabReloadScope(profileIsolationSupported)) {
                OpenTabReloadScope.NONE -> Unit
                OpenTabReloadScope.NORMAL -> {
                    tabManager.invalidateOpenTabsForReload(isIncognito = false)
                }
                OpenTabReloadScope.PRIVATE -> {
                    tabManager.invalidateOpenTabsForReload(isIncognito = true)
                }
                OpenTabReloadScope.ALL -> tabManager.invalidateOpenTabsForReload()
            }
            if (options.clearHistory && includesNormal) {
                tabManager.clearNavigationHistory(
                    isIncognito = if (options.scope == BrowsingDataScope.ALL) null else false
                )
            }
            if (options.clearCache) tabManager.clearWebViewCache()
        }

        if (options.clearHistory && includesNormal) {
            withContext(Dispatchers.IO) { historyRepository.deleteAll() }
        }

        if (options.clearCookies) {
            if (profileIsolationSupported) {
                if (includesNormal) browserEngine.clearProfileBrowsingData(isIncognito = false)
                if (includesPrivate) browserEngine.clearProfileBrowsingData(isIncognito = true)
            } else {
                // Older providers share one store. Clear it once and keep both tab groups alive.
                browserEngine.clearProfileBrowsingData(isIncognito = false)
            }
        }

        if (options.clearOpenTabs) {
            withContext(Dispatchers.Main.immediate) {
                if (options.shouldCloseOpenTabs(isIncognito = false)) {
                    tabManager.closeAllInMode(isIncognito = false)
                }
                if (options.shouldCloseOpenTabs(isIncognito = true)) {
                    tabManager.closeAllInMode(isIncognito = true)
                }
            }
        }

        return ClearBrowsingDataResult(profileIsolationLimited = limitedIsolation)
    }
}
