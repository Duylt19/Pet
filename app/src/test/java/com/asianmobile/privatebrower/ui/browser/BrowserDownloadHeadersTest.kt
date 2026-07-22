package com.asianmobile.privatebrower.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BrowserDownloadHeadersTest {

    @Test
    fun `active WebView user agent is added when intercepted headers omit it`() {
        val headers = mapOf("Referer" to "https://example.com/watch")

        val result = headers.withUserAgentIfMissing("PrivateBrowser/1.0")

        assertEquals("PrivateBrowser/1.0", result["User-Agent"])
        assertEquals("https://example.com/watch", result["Referer"])
    }

    @Test
    fun `intercepted user agent is preserved case insensitively`() {
        val headers = mapOf("user-agent" to "MediaPlayer/2.0")

        val result = headers.withUserAgentIfMissing("PrivateBrowser/1.0")

        assertSame(headers, result)
        assertEquals("MediaPlayer/2.0", result["user-agent"])
    }

    @Test
    fun `blank active user agent does not change headers`() {
        val headers = mapOf("Referer" to "https://example.com/watch")

        assertSame(headers, headers.withUserAgentIfMissing(" "))
    }
}
