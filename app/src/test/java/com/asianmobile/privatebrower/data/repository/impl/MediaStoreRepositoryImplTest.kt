package com.asianmobile.privatebrower.data.repository.impl

import android.provider.MediaStore
import com.asianmobile.privatebrower.data.repository.MediaSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreRepositoryImplTest {

    @Test
    fun `files category excludes media mime types`() {
        assertTrue(isMediaMimeType("image/jpeg"))
        assertTrue(isMediaMimeType("video/mp4"))
        assertTrue(isMediaMimeType("audio/mpeg"))
        assertFalse(isMediaMimeType("application/pdf"))
        assertFalse(isMediaMimeType("application/octet-stream"))
    }

    @Test
    fun `files category keeps documents subtitles and other files`() {
        val excludedTypes = fileCategoryExcludedMediaTypes()

        assertArrayEquals(
            intArrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
            ),
            excludedTypes
        )
        assertFalse(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE in excludedTypes)
        assertFalse(MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT in excludedTypes)
        assertFalse(MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE in excludedTypes)
    }

    @Test
    fun `Android 11 media store delete can fall back to system consent`() {
        assertTrue(
            shouldUseMediaStoreDeleteRequest(
                sdkInt = 30,
                source = MediaSource.MEDIA_STORE,
                uriAuthority = "media"
            )
        )
        assertTrue(
            shouldUseMediaStoreDeleteRequest(
                sdkInt = 36,
                source = MediaSource.MEDIA_STORE,
                uriAuthority = "media"
            )
        )
    }

    @Test
    fun `legacy and non media store files never use Android 11 delete request`() {
        assertFalse(
            shouldUseMediaStoreDeleteRequest(
                sdkInt = 29,
                source = MediaSource.MEDIA_STORE,
                uriAuthority = "media"
            )
        )
        assertFalse(
            shouldUseMediaStoreDeleteRequest(
                sdkInt = 36,
                source = MediaSource.APP_DOWNLOAD,
                uriAuthority = "media"
            )
        )
        assertFalse(
            shouldUseMediaStoreDeleteRequest(
                sdkInt = 36,
                source = MediaSource.MEDIA_STORE,
                uriAuthority = "com.asianmobile.privatebrower.fileprovider"
            )
        )
    }
}
