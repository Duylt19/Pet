package com.asianmobile.privatebrower.data.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoConsolidateTest {

    private fun video(url: String, mime: String? = "video/mp4") =
        DetectedVideo(url = url, mimeType = mime, refererUrl = null, headers = emptyMap())

    private val base = "https://cdn2.tuoitre.vn/471/2026/7/20/ban-sua-xe-ba-gac-178452757.mp4"

    @Test
    fun `same video across progressive and hls renditions collapses to one`() {
        val list = listOf(
            video(base), // progressive (from JS meta)
            video("https://cdn2.tuoitre.vn/.hls/471/2026/7/20/ban-sua-xe-ba-gac-178452757.mp4.mobile.m3u8?v=1"),
            video("https://cdn2.tuoitre.vn/.hls/471/2026/7/20/ban-sua-xe-ba-gac-178452757.mp4.index.480.m3u8?v=1"),
            video("https://cdn2.tuoitre.vn/.hls/471/2026/7/20/ban-sua-xe-ba-gac-178452757.mp4.index.720.m3u8?v=1"),
        )

        val out = list.consolidateSameVideo()

        assertEquals(1, out.size)
        // Progressive file is preferred over the HLS playlists.
        assertEquals(base, out.single().url)
    }

    @Test
    fun `hls-only video keeps the master over per-quality variants`() {
        val list = listOf(
            video("https://cdn.example.com/x.mp4.index.480.m3u8"),
            video("https://cdn.example.com/x.mp4.mobile.m3u8"),
            video("https://cdn.example.com/x.mp4.index.720.m3u8"),
        )

        val out = list.consolidateSameVideo()

        assertEquals(1, out.size)
        assertTrue(out.single().url.contains("mobile.m3u8"))
    }

    @Test
    fun `distinct videos are never merged`() {
        val list = listOf(
            video("https://cdn.example.com/a.mp4"),
            video("https://cdn.example.com/b.mp4"),
        )

        assertEquals(2, list.consolidateSameVideo().size)
    }

    @Test
    fun `urls without an embedded media name are keyed individually`() {
        val list = listOf(
            video("https://cdn.example.com/playlist1.m3u8"),
            video("https://cdn.example.com/playlist2.m3u8"),
        )

        assertEquals(2, list.consolidateSameVideo().size)
    }

    @Test
    fun `24h master and quality playlists collapse to the master`() {
        val master = "https://cdn.24h.com.vn/upload/3-2026/videoclip/2026-07-21/" +
            "1784593152-chay-xe-khach-khien-7-nguoi-tu-vong-o-dong-nai.m3u8"
        val list = listOf(
            video(master, "application/vnd.apple.mpegurl"),
            video(master.replace(".m3u8", "_480p.m3u8"), "application/vnd.apple.mpegurl"),
            video(master.replace(".m3u8", "_720p.m3u8"), "application/vnd.apple.mpegurl")
        )

        val out = list.consolidateSameVideo()

        assertEquals(1, out.size)
        assertEquals(master, out.single().url)
    }

    @Test
    fun `signed query changes do not duplicate the same hls rendition`() {
        val first = video(
            "https://cdn.example.com/news/clip_720p.m3u8?token=old",
            "application/vnd.apple.mpegurl"
        )
        val second = video(
            "https://cdn.example.com/news/clip_720p.m3u8?token=new",
            "application/vnd.apple.mpegurl"
        )

        assertEquals(1, listOf(first, second).consolidateSameVideo().size)
    }

    @Test
    fun `same filename on different hosts stays distinct`() {
        val list = listOf(
            video("https://cdn-a.example.com/video/clip_720p.m3u8"),
            video("https://cdn-b.example.com/video/clip_720p.m3u8")
        )

        assertEquals(2, list.consolidateSameVideo().size)
    }

    @Test
    fun `same filename in unrelated directories on one host stays distinct`() {
        val list = listOf(
            video("https://cdn.example.com/article-a/clip.m3u8"),
            video("https://cdn.example.com/article-b/clip.m3u8")
        )

        assertEquals(2, list.consolidateSameVideo().size)
    }

    @Test
    fun `content metadata enriches only its matching media family`() {
        val contentMaster = video("https://cdn.example.com/article/main-video.m3u8")
        val contentVariant = video("https://cdn.example.com/article/main-video_720p.m3u8")
        val advertisement = video("https://cdn.example.com/ads/banner.mp4")

        val enriched = listOf(contentMaster, contentVariant, advertisement).enrichMediaFamily(
            mediaKey = contentVariant.mediaIdentityKey(),
            title = "Article title",
            posterUrl = "https://example.com/article.jpg"
        )

        assertTrue(enriched[0].isPageContent)
        assertTrue(enriched[1].isPageContent)
        assertEquals("Article title", enriched[0].customName)
        assertFalse(enriched[2].isPageContent)
        assertEquals(null, enriched[2].customName)
        assertEquals(null, enriched[2].thumbnailUrl)
    }

    @Test
    fun `multiple videos on one page keep independent titles and posters`() {
        val first = video("https://cdn.example.com/article/first.mp4")
        val second = video("https://cdn.example.com/article/second.mp4")
        val firstKey = first.mediaIdentityKey()
        val secondKey = second.mediaIdentityKey()

        val metadata = emptyMap<String, MediaDisplayMetadata>()
            .withMediaMetadata(firstKey, "https://example.com/first.jpg", "First story")
            .withMediaMetadata(secondKey, "https://example.com/second.jpg", "Second story")
        val enriched = listOf(first, second)
            .enrichMediaFamily(firstKey, metadata[firstKey]?.title, metadata[firstKey]?.posterUrl)
            .enrichMediaFamily(secondKey, metadata[secondKey]?.title, metadata[secondKey]?.posterUrl)

        assertEquals("First story", enriched[0].customName)
        assertEquals("https://example.com/first.jpg", enriched[0].thumbnailUrl)
        assertEquals("Second story", enriched[1].customName)
        assertEquals("https://example.com/second.jpg", enriched[1].thumbnailUrl)
    }

    @Test
    fun `advertisement tombstone removes the whole rendition family only`() {
        val adMaster = video("https://ads.example.com/creative/banner.m3u8")
        val adVariant = video("https://ads.example.com/creative/banner_720p.m3u8")
        val content = video("https://cdn.example.com/article/main.m3u8")

        val remaining = listOf(adMaster, adVariant, content).removeMediaFamily(
            canonicalUrl = canonicalMediaUrl(adVariant.url),
            mediaKey = adVariant.mediaIdentityKey()
        )

        assertEquals(listOf(content), remaining)
    }

    @Test
    fun `weak direct request cannot evict a real mse capture`() {
        val capture = DetectedVideo(
            url = "mse-capture://capture-1",
            mimeType = "video/mp4",
            refererUrl = null,
            headers = emptyMap(),
            captureId = "capture-1",
            isPageContent = true
        )
        val weakDirect = video("https://cdn.example.com/banner.mp4")

        assertEquals(2, listOf(capture, weakDirect).preferDirectOverCapture().size)

        val confirmedDirect = weakDirect.copy(isPageContent = true)
        val preferred = listOf(capture, confirmedDirect).preferDirectOverCapture()
        assertEquals(listOf(confirmedDirect), preferred)
    }
}
