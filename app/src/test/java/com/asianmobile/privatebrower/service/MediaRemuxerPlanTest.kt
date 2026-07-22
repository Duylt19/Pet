package com.asianmobile.privatebrower.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRemuxerPlanTest {

    @Test
    fun `h264 plus aac goes to mp4 with audio`() {
        val plan = chooseRemuxContainer("video/avc", "audio/mp4a-latm")!!
        assertEquals(RemuxContainer.MP4, plan.container)
        assertTrue(plan.includeAudio)
        assertNull(plan.note)
    }

    @Test
    fun `vp9 plus opus goes to webm with audio`() {
        val plan = chooseRemuxContainer("video/x-vnd.on2.vp9", "audio/opus")!!
        assertEquals(RemuxContainer.WEBM, plan.container)
        assertTrue(plan.includeAudio)
        assertNull(plan.note)
    }

    @Test
    fun `vp9 video with aac audio keeps webm but drops the incompatible audio`() {
        val plan = chooseRemuxContainer("video/x-vnd.on2.vp9", "audio/mp4a-latm")!!
        assertEquals(RemuxContainer.WEBM, plan.container)
        assertFalse(plan.includeAudio)
        assertTrue(plan.note!!.contains("video-only"))
    }

    @Test
    fun `h264 video with opus audio keeps mp4 but drops the incompatible audio`() {
        val plan = chooseRemuxContainer("video/avc", "audio/opus")!!
        assertEquals(RemuxContainer.MP4, plan.container)
        assertFalse(plan.includeAudio)
    }

    @Test
    fun `video only h264 stays mp4`() {
        val plan = chooseRemuxContainer("video/avc", null)!!
        assertEquals(RemuxContainer.MP4, plan.container)
        assertFalse(plan.includeAudio)
        assertNull(plan.note)
    }

    @Test
    fun `audio only opus stays webm`() {
        val plan = chooseRemuxContainer(null, "audio/opus")!!
        assertEquals(RemuxContainer.WEBM, plan.container)
        assertTrue(plan.includeAudio)
    }

    @Test
    fun `no tracks returns null`() {
        assertNull(chooseRemuxContainer(null, null))
    }
}
