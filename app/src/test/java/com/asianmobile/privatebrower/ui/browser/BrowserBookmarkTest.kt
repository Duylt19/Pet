package com.asianmobile.privatebrower.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserBookmarkTest {

    @Test
    fun isBookmarkableUrl_acceptsHttpPages() {
        assertTrue(isBookmarkableUrl("https://example.com/path?q=browser#section"))
        assertTrue(isBookmarkableUrl("http://localhost:8080/page"))
    }

    @Test
    fun isBookmarkableUrl_rejectsInternalAndMalformedPages() {
        assertFalse(isBookmarkableUrl(""))
        assertFalse(isBookmarkableUrl("about:blank"))
        assertFalse(isBookmarkableUrl("data:text/html,hello"))
        assertFalse(isBookmarkableUrl("chrome-error://chromewebdata/"))
        assertFalse(isBookmarkableUrl("https:///missing-host"))
    }

    @Test
    fun faviconUrlFor_usesThePageOriginWithoutLeakingThePath() {
        assertEquals(
            "https://example.com:8443/favicon.ico",
            faviconUrlFor("https://example.com:8443/account/private?q=1")
        )
        assertEquals(
            "http://localhost/favicon.ico",
            faviconUrlFor("http://localhost/home")
        )
        assertNull(faviconUrlFor("about:blank"))
    }
}
