package com.asianmobile.privatebrower.data.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearBrowsingDataOptionsTest {

    @Test
    fun `open tabs are not cleared by default`() {
        assertFalse(ClearBrowsingDataOptions().clearOpenTabs)
    }

    @Test
    fun `private scope removes automatically managed data options`() {
        val options = ClearBrowsingDataOptions().forScope(BrowsingDataScope.PRIVATE)

        assertTrue(options.clearHistory)
        assertFalse(options.clearCookies)
        assertFalse(options.clearCache)
        assertFalse(options.hasSelection)
    }

    @Test
    fun `private scope preserves explicit open tabs choice`() {
        val options = ClearBrowsingDataOptions(
            clearCookies = true,
            clearOpenTabs = true,
            clearCache = true
        ).forScope(BrowsingDataScope.PRIVATE)

        assertFalse(options.clearCookies)
        assertFalse(options.clearCache)
        assertTrue(options.clearOpenTabs)
        assertTrue(options.hasSelection)
    }

    @Test
    fun `private history alone is not a valid selection`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.PRIVATE,
            clearCookies = false,
            clearOpenTabs = false,
            clearHistory = true,
            clearCache = false
        )

        assertFalse(options.hasSelection)
    }

    @Test
    fun `changing scope preserves every explicit option`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.NORMAL,
            clearCookies = true,
            clearOpenTabs = true,
            clearHistory = false,
            clearCache = false
        ).forScope(BrowsingDataScope.ALL)

        assertTrue(options.clearCookies)
        assertTrue(options.clearOpenTabs)
        assertFalse(options.clearHistory)
        assertFalse(options.clearCache)
    }

    @Test
    fun `scoped cache clear is identified as shared WebView cache`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.NORMAL,
            clearCache = true
        )

        assertTrue(options.hasSharedCacheScope)
        assertFalse(options.copy(scope = BrowsingDataScope.ALL).hasSharedCacheScope)
    }

    @Test
    fun `scoped site data only reports limitation without profile support`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.PRIVATE,
            clearCookies = true,
            clearHistory = false,
            clearCache = false
        )

        assertTrue(options.hasLimitedProfileIsolation(supportsProfileIsolation = false))
        assertFalse(options.hasLimitedProfileIsolation(supportsProfileIsolation = true))
    }

    @Test
    fun `cache invalidates every open tab because WebView cache is shared`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.NORMAL,
            clearCookies = false,
            clearCache = true
        )

        assertEquals(
            OpenTabReloadScope.ALL,
            options.openTabReloadScope(supportsProfileIsolation = true)
        )
    }

    @Test
    fun `normal site data invalidates normal tabs with profile isolation`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.NORMAL,
            clearCookies = true,
            clearCache = false
        )

        assertEquals(
            OpenTabReloadScope.NORMAL,
            options.openTabReloadScope(supportsProfileIsolation = true)
        )
    }

    @Test
    fun `shared site data invalidates every tab without profile isolation`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.NORMAL,
            clearCookies = true,
            clearCache = false
        )

        assertEquals(
            OpenTabReloadScope.ALL,
            options.openTabReloadScope(supportsProfileIsolation = false)
        )
    }

    @Test
    fun `private site data reloads private tabs without closing them`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.PRIVATE,
            clearCookies = true,
            clearOpenTabs = false,
            clearCache = false
        )

        assertEquals(
            OpenTabReloadScope.PRIVATE,
            options.openTabReloadScope(supportsProfileIsolation = true)
        )
        assertFalse(options.shouldCloseOpenTabs(isIncognito = true))
    }

    @Test
    fun `private open tabs close only when explicitly selected`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.PRIVATE,
            clearCookies = true,
            clearOpenTabs = true,
            clearCache = false
        )

        assertTrue(options.shouldCloseOpenTabs(isIncognito = true))
        assertFalse(options.shouldCloseOpenTabs(isIncognito = false))
    }

    @Test
    fun `all site data reloads both isolated profiles`() {
        val options = ClearBrowsingDataOptions(
            scope = BrowsingDataScope.ALL,
            clearCookies = true,
            clearOpenTabs = false,
            clearCache = false
        )

        assertEquals(
            OpenTabReloadScope.ALL,
            options.openTabReloadScope(supportsProfileIsolation = true)
        )
    }
}
