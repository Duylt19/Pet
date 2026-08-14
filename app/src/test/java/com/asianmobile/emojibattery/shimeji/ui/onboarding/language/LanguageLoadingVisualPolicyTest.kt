package com.asianmobile.emojibattery.shimeji.ui.onboarding.language

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageLoadingVisualPolicyTest {
    @Test
    fun `disabled placement neither loads ad nor shows loading layer`() {
        val shouldLoadAd = shouldLoadLanguageNativeAd(
            isNativeEligible = true,
            isPlacementEnabled = false,
        )

        assertEquals(false, shouldLoadAd)
        assertEquals(
            false,
            shouldShowLanguageAdLoading(
                shouldLoadAd = shouldLoadAd,
                isLoadComplete = false,
            )
        )
    }

    @Test
    fun `ineligible native policy suppresses enabled placement`() {
        assertEquals(
            false,
            shouldLoadLanguageNativeAd(
                isNativeEligible = false,
                isPlacementEnabled = true,
            )
        )
    }

    @Test
    fun `eligible enabled placement shows loading until callback completes`() {
        val shouldLoadAd = shouldLoadLanguageNativeAd(
            isNativeEligible = true,
            isPlacementEnabled = true,
        )

        assertEquals(
            true,
            shouldShowLanguageAdLoading(
                shouldLoadAd = shouldLoadAd,
                isLoadComplete = false,
            )
        )
        assertEquals(
            false,
            shouldShowLanguageAdLoading(
                shouldLoadAd = shouldLoadAd,
                isLoadComplete = true,
            )
        )
    }

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
