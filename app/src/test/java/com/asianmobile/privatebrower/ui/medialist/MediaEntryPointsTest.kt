package com.asianmobile.privatebrower.ui.medialist

import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_AUDIO
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_DOCUMENTS
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_PHOTOS
import com.asianmobile.privatebrower.ads.config.SCREEN_FILES_VIDEOS
import com.asianmobile.privatebrower.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaEntryPointsTest {

    @Test
    fun mediaDestinations_areIndependent() {
        val routes = setOf(
            Routes.MEDIA_PHOTOS,
            Routes.MEDIA_VIDEOS,
            Routes.MEDIA_AUDIO,
            Routes.MEDIA_DOCUMENTS
        )

        assertEquals(4, routes.size)
    }

    @Test
    fun mediaAdIdentities_areIndependent() {
        val screenCodes = setOf(
            SCREEN_FILES_PHOTOS,
            SCREEN_FILES_VIDEOS,
            SCREEN_FILES_AUDIO,
            SCREEN_FILES_DOCUMENTS
        )

        assertEquals(4, screenCodes.size)
    }
}
