package com.asianmobile.emojibattery.shimeji.ui.onboarding.language

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageLoadingVisualPolicyTest {
    @Test
    fun `supported api blurs content while loading`() {
        assertEquals(
            8.dp,
            languageLoadingBlurRadius(
                isLoading = true,
                isSupportBlur = true,
            )
        )
    }

    @Test
    fun `unsupported api keeps content sharp while loading`() {
        assertEquals(
            0.dp,
            languageLoadingBlurRadius(
                isLoading = true,
                isSupportBlur = false,
            )
        )
    }

    @Test
    fun `loaded content is never blurred`() {
        assertEquals(
            0.dp,
            languageLoadingBlurRadius(
                isLoading = false,
                isSupportBlur = true,
            )
        )
    }

    @Test
    fun `loading scrim matches figma opacity`() {
        assertEquals(0.6f, LANGUAGE_LOADING_SCRIM_ALPHA)
    }
}
