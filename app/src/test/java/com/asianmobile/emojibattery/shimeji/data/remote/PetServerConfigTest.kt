package com.asianmobile.emojibattery.shimeji.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetServerConfigTest {
    @Test
    fun `authentication is scoped to the configured raw repository`() {
        assertTrue(
            PetServerConfig.isPetServerUrl(
                host = "raw.githubusercontent.com",
                encodedPath = "/Asian-Mobile-Inc/Server-Emoji-Battery-Shimeji-Pet-AM/master/thumb/42.png"
            )
        )
        assertFalse(
            PetServerConfig.isPetServerUrl(
                host = "raw.githubusercontent.com",
                encodedPath = "/another-owner/another-repository/master/thumb/42.png"
            )
        )
        assertFalse(
            PetServerConfig.isPetServerUrl(
                host = "example.com",
                encodedPath = PetServerConfig.RAW_REPOSITORY_PATH
            )
        )
    }
}
