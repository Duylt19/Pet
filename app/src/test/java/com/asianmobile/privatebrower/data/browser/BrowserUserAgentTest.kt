package com.asianmobile.privatebrower.data.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserUserAgentTest {

    @Test
    fun mobileUserAgent_keepsProviderVersionAndRemovesWebViewMarkers() {
        val defaultUserAgent =
            "Mozilla/5.0 (Linux; Android 15; Pixel 9 Build/AP3A; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
                "Chrome/140.0.7339.51 Mobile Safari/537.36"

        val result = mobileUserAgent(defaultUserAgent)

        assertTrue(result.contains("Android 15"))
        assertTrue(result.contains("Chrome/140.0.7339.51"))
        assertTrue(result.contains("Mobile Safari"))
        assertFalse(result.contains("; wv"))
        assertFalse(result.contains("Version/4.0"))
    }

    @Test
    fun mobileUserAgent_leavesAnExistingMobileUserAgentStable() {
        val expectedUserAgent =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"

        assertEquals(expectedUserAgent, mobileUserAgent(expectedUserAgent))
    }

    @Test
    fun desktopUserAgent_keepsProviderChromeVersionAndRemovesWebViewMobileMarkers() {
        val defaultUserAgent =
            "Mozilla/5.0 (Linux; Android 15; Pixel 9 Build/AP3A; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
                "Chrome/140.0.7339.51 Mobile Safari/537.36"

        val result = desktopUserAgent(defaultUserAgent)

        assertTrue(result.contains("(X11; Linux x86_64)"))
        assertTrue(result.contains("Chrome/140.0.7339.51"))
        assertFalse(result.contains("Android"))
        assertFalse(result.contains("; wv"))
        assertFalse(result.contains("Version/4.0"))
        assertFalse(result.contains(" Mobile Safari"))
    }

    @Test
    fun desktopUserAgent_leavesAnExistingDesktopUserAgentStable() {
        val expectedUserAgent =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

        assertEquals(expectedUserAgent, desktopUserAgent(expectedUserAgent))
    }
}
