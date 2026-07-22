package com.asianmobile.privatebrower.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeIconTest {

    @Test
    fun `figma file variants are classified separately`() {
        assertKind("application/pdf", "pdf", FileTypeKind.PDF)
        assertKind("application/msword", "doc", FileTypeKind.DOC)
        assertKind("application/vnd.ms-excel", "xls", FileTypeKind.XLS)
        assertKind("application/zip", "zip", FileTypeKind.ZIP)
        assertKind("application/vnd.rar", "rar", FileTypeKind.RAR)
        assertKind("text/plain", "txt", FileTypeKind.TXT)
        assertKind("application/x-msdownload", "exe", FileTypeKind.EXE)
        assertKind("application/vnd.android.package-archive", "apk", FileTypeKind.APK)
    }

    @Test
    fun `additional common download types receive useful holders`() {
        assertKind("application/vnd.ms-powerpoint", "pptx", FileTypeKind.PPT)
        assertKind("application/json", "json", FileTypeKind.CODE)
        assertKind("application/epub+zip", "epub", FileTypeKind.EBOOK)
        assertKind("application/octet-stream", "xapk", FileTypeKind.APK)
        assertKind("application/octet-stream", "7z", FileTypeKind.ZIP)
    }

    @Test
    fun `media types keep real thumbnail categories`() {
        assertKind("video/mp4", "bin", FileTypeKind.VIDEO)
        assertKind("image/webp", "bin", FileTypeKind.IMAGE)
        assertKind("audio/mpeg", "bin", FileTypeKind.AUDIO)
    }

    @Test
    fun `mime parameters and mixed case extensions are normalized`() {
        assertKind("application/pdf; charset=binary", ".PDF", FileTypeKind.PDF)
        assertKind("APPLICATION/VND.RAR", "RAR", FileTypeKind.RAR)
    }

    @Test
    fun `unknown formats use generic holder`() {
        assertKind("application/octet-stream", "unknown", FileTypeKind.OTHER)
        assertKind("", "", FileTypeKind.OTHER)
    }

    private fun assertKind(mimeType: String, extension: String, expected: FileTypeKind) {
        assertEquals(expected, fileTypeKind(mimeType, extension))
    }
}
