package com.asianmobile.privatebrower.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MpdTemplateTest {

    @Test
    fun `expands representation id and number`() {
        val out = expandTemplate("\$RepresentationID\$/seg-\$Number\$.m4s", "video1", 800000, 5, 0)
        assertEquals("video1/seg-5.m4s", out)
    }

    @Test
    fun `expands zero-padded number`() {
        val out = expandTemplate("chunk_\$Number%05d\$.m4s", "v", 0, 42, 0)
        assertEquals("chunk_00042.m4s", out)
    }

    @Test
    fun `expands bandwidth and time`() {
        val out = expandTemplate("\$Bandwidth\$/\$Time\$.m4s", "v", 1200000, 0, 123456)
        assertEquals("1200000/123456.m4s", out)
    }

    @Test
    fun `escaped dollar is literal`() {
        val out = expandTemplate("price\$\$/\$Number\$", "v", 0, 1, 0)
        assertEquals("price\$/1", out)
    }

    @Test
    fun `iso duration parses hours minutes seconds`() {
        assertEquals(3661.5, parseIso8601Duration("PT1H1M1.5S"), 0.001)
    }

    @Test
    fun `iso duration parses seconds only`() {
        assertEquals(634.566, parseIso8601Duration("PT634.566S"), 0.001)
    }

    @Test
    fun `iso duration handles missing value`() {
        assertEquals(0.0, parseIso8601Duration(null), 0.001)
        assertEquals(0.0, parseIso8601Duration(""), 0.001)
    }
}
