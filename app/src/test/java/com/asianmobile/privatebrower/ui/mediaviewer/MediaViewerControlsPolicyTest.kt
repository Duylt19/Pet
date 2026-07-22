package com.asianmobile.privatebrower.ui.mediaviewer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaViewerControlsPolicyTest {

    @Test
    fun `image controls auto hide without playback`() {
        assertTrue(
            shouldAutoHideMediaViewerControls(
                kind = MediaViewerKind.IMAGE,
                isPlaying = false
            )
        )
    }

    @Test
    fun `video and audio controls auto hide only while playing`() {
        assertFalse(shouldAutoHideMediaViewerControls(MediaViewerKind.VIDEO, isPlaying = false))
        assertTrue(shouldAutoHideMediaViewerControls(MediaViewerKind.VIDEO, isPlaying = true))
        assertFalse(shouldAutoHideMediaViewerControls(MediaViewerKind.AUDIO, isPlaying = false))
        assertTrue(shouldAutoHideMediaViewerControls(MediaViewerKind.AUDIO, isPlaying = true))
    }

    @Test
    fun `unsupported file controls stay visible`() {
        assertFalse(
            shouldAutoHideMediaViewerControls(
                kind = MediaViewerKind.OTHER,
                isPlaying = false
            )
        )
    }
}
