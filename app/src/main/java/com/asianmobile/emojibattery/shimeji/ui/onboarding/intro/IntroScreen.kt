@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.asianmobile.emojibattery.shimeji.ui.onboarding.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_INTRO_SECOND
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import kotlinx.coroutines.launch
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp

private val nunitoBlackFontFamily = FontFamily(
    Font(R.font.nunito_black, FontWeight.Black),
)

private val robotoMediumFontFamily = FontFamily(
    Font(R.font.roboto_medium, FontWeight.Medium),
)

private val robotoSemiBoldFontFamily = FontFamily(
    Font(R.font.roboto_semibold, FontWeight.SemiBold),
)

private const val INTRO_DESIGN_WIDTH = 360f
private const val INTRO_DESIGN_HEIGHT = 800f
private const val INTRO_PAGE_TWO_COPY_BOTTOM = 41f

private data class IntroPage(
    val titleRes: Int,
    val imageRes: Int,
    val imageAspectRatio: Float,
)

private val introPages = listOf(
    IntroPage(
        titleRes = R.string.intro_title_1,
        imageRes = R.drawable.img_intro1,
        imageAspectRatio = 360f / 534f,
    ),
    IntroPage(
        titleRes = R.string.intro_title_2,
        imageRes = R.drawable.img_intro2,
        imageAspectRatio = 360f / 670f,
    ),
    IntroPage(
        titleRes = R.string.intro_title_3,
        imageRes = R.drawable.img_intro3,
        imageAspectRatio = 360f / 500f,
    ),
)

@Composable
fun IntroScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    var hasVisitedLastPage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == introPages.lastIndex) {
            hasVisitedLastPage = true
        }
    }

    HorizontalPager(
        state = pagerState,
        // Intro has only three pages. Keeping them composed preserves each loaded native view
        // when the user swipes away and back instead of briefly showing an empty slot.
        beyondViewportPageCount = introPages.lastIndex,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
    ) { pageIndex ->
        // All three pages are intentionally preloaded to preserve their native ads. Attach the
        // tracker to each page but enable only the settled page, so composition is never mistaken
        // for an actual screen view and an aborted swipe keeps the previous page visible.
        TrackScreenView(
            screen = introPageScreenName(pageIndex),
            isVisible = isIntroPageVisible(
                pageIndex = pageIndex,
                settledPage = pagerState.settledPage,
            ),
        )
        val nativeAdScreenCode = introNativeAdScreenCode(pageIndex)
        IntroPageContent(
            pageIndex = pageIndex,
            currentPage = currentPage,
            showNativeAd = shouldKeepIntroNativeAdMounted(
                pageIndex = pageIndex,
                hasVisitedLastPage = hasVisitedLastPage,
            ),
            onActionClick = {
                if (pageIndex == introPages.lastIndex) {
                    onFinish()
                } else {
                    coroutineScope.launch {
                        runCatching {
                            pagerState.animateScrollToPage(pageIndex + 1)
                        }
                    }
                }
            },
            adContent = {
                nativeAdScreenCode?.let { screenCode ->
                    NativeAdInternal(
                        screenCode = screenCode,
                        modifier = Modifier.fillMaxWidth(),
                        // Keep the native slot understandable while the SDK is loading. The
                        // shared ad layout owns the shimmer and still collapses after a failure.
                        showLoadingPlaceholder = true,
                    )
                }
            },
        )
    }
}

internal fun introPageScreenName(pageIndex: Int): ScreenName = when (pageIndex) {
    0 -> ScreenName.INTRO_PAGE_1
    1 -> ScreenName.INTRO_PAGE_2
    else -> ScreenName.INTRO_PAGE_3
}

internal fun isIntroPageVisible(pageIndex: Int, settledPage: Int): Boolean =
    pageIndex == settledPage

internal fun shouldKeepIntroNativeAdMounted(
    pageIndex: Int,
    hasVisitedLastPage: Boolean,
): Boolean = when (pageIndex) {
    0 -> true
    introPages.lastIndex -> hasVisitedLastPage
    else -> false
}

internal fun introNativeAdScreenCode(pageIndex: Int): String? = when (pageIndex) {
    0 -> SCREEN_INTRO
    introPages.lastIndex -> SCREEN_INTRO_SECOND
    else -> null
}

internal fun introCompactHeightScale(
    widthDp: Float,
    heightDp: Float,
): Float {
    if (widthDp <= 0f || heightDp <= 0f) return 1f
    val designHeightAtCurrentWidth = widthDp * INTRO_DESIGN_HEIGHT / INTRO_DESIGN_WIDTH
    return (heightDp / designHeightAtCurrentWidth).coerceIn(0.72f, 1f)
}

@Composable
internal fun IntroPageContent(
    pageIndex: Int,
    currentPage: Int,
    showNativeAd: Boolean,
    onActionClick: () -> Unit,
    adContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = introPages[pageIndex]

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
    ) {
        val compactHeightScale = introCompactHeightScale(
            widthDp = maxWidth.value,
            heightDp = maxHeight.value,
        )
        val referenceUnit = maxWidth / INTRO_DESIGN_WIDTH
        val nativeAdControlInset = dimensionResource(R_sdp.dimen._171sdp) +
            dimensionResource(R_sdp.dimen._6sdp)

        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height((maxWidth / page.imageAspectRatio) * compactHeightScale),
            contentScale = ContentScale.FillBounds,
        )

        when (pageIndex) {
            0 -> {
                IntroCopyAndControls(
                    page = page,
                    currentPage = currentPage,
                    sideActionText = stringResource(R.string.next),
                    onActionClick = onActionClick,
                    layoutScale = compactHeightScale,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Figma leaves 8 px between the control row and the 222 px native.
                        // Anchor to the fixed ad slot instead of scaling a top coordinate; the
                        // latter makes the gap grow on compact/tall Android viewports.
                        .offset(y = -nativeAdControlInset),
                )
                if (showNativeAd) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    ) {
                        adContent()
                    }
                }
            }

            1 -> MiddleIntroPageContent(
                page = page,
                currentPage = currentPage,
                onNextClick = onActionClick,
                layoutScale = compactHeightScale,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = referenceUnit * INTRO_PAGE_TWO_COPY_BOTTOM * compactHeightScale,
                    ),
            )

            else -> {
                IntroCopyAndControls(
                    page = page,
                    currentPage = currentPage,
                    sideActionText = stringResource(R.string.start),
                    onActionClick = onActionClick,
                    layoutScale = compactHeightScale,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -nativeAdControlInset),
                )
                if (showNativeAd) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    ) {
                        adContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun MiddleIntroPageContent(
    page: IntroPage,
    currentPage: Int,
    onNextClick: () -> Unit,
    layoutScale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IntroCopyAndControls(
            page = page,
            currentPage = currentPage,
            sideActionText = null,
            onActionClick = onNextClick,
            layoutScale = layoutScale,
        )
        Spacer(
            modifier = Modifier.height(
                dimensionResource(R_sdp.dimen._10sdp) * layoutScale,
            ),
        )
        IntroPrimaryButton(
            onClick = onNextClick,
            layoutScale = layoutScale,
        )
        Spacer(
            modifier = Modifier.height(
                dimensionResource(R_sdp.dimen._2sdp) * layoutScale,
            ),
        )
    }
}

@Composable
private fun IntroCopyAndControls(
    page: IntroPage,
    currentPage: Int,
    sideActionText: String?,
    onActionClick: () -> Unit,
    layoutScale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier.height(
                dimensionResource(R_sdp.dimen._6sdp) * layoutScale,
            ),
        )
        Text(
            text = stringResource(page.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = dimensionResource(R_sdp.dimen._46sdp) * layoutScale,
                ),
            color = colorResource(R.color.colors_333538),
            fontFamily = nunitoBlackFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (dimensionResource(R_ssp.dimen._17ssp).value * layoutScale).sp,
            lineHeight = (dimensionResource(R_ssp.dimen._23ssp).value * layoutScale).sp,
            textAlign = TextAlign.Center,
            maxLines = 4,
        )
        Spacer(
            modifier = Modifier.height(
                dimensionResource(R_sdp.dimen._6sdp) * layoutScale,
            ),
        )

        if (sideActionText == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._22sdp) * layoutScale),
                contentAlignment = Alignment.Center,
            ) {
                PageIndicators(
                    currentPage = currentPage,
                    layoutScale = layoutScale,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._22sdp) * layoutScale)
                    .padding(
                        horizontal = dimensionResource(R_sdp.dimen._12sdp) * layoutScale,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageIndicators(
                    currentPage = currentPage,
                    layoutScale = layoutScale,
                )
                Text(
                    text = sideActionText,
                    color = colorResource(R.color.colors_FB3675),
                    fontFamily = robotoSemiBoldFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (dimensionResource(R_ssp.dimen._15ssp).value * layoutScale).sp,
                    lineHeight = (dimensionResource(R_ssp.dimen._22ssp).value * layoutScale).sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onActionClick)
                        .padding(
                            horizontal = dimensionResource(R_sdp.dimen._3sdp) * layoutScale,
                        ),
                )
            }
        }
        Spacer(
            modifier = Modifier.height(
                dimensionResource(R_sdp.dimen._6sdp) * layoutScale,
            ),
        )
    }
}

@Composable
private fun IntroPrimaryButton(
    onClick: () -> Unit,
    layoutScale: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(320f / 360f)
            .height(dimensionResource(R_sdp.dimen._37sdp) * layoutScale)
            .clip(CircleShape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = dimensionResource(R_sdp.dimen._1sdp) * layoutScale,
                brush = Brush.verticalGradient(
                    listOf(
                        colorResource(R.color.colors_FF5D7D),
                        colorResource(R.color.colors_FB54BB),
                    ),
                ),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.next),
            color = colorResource(R.color.colors_FB3675),
            fontFamily = robotoMediumFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (dimensionResource(R_ssp.dimen._15ssp).value * layoutScale).sp,
            lineHeight = (dimensionResource(R_ssp.dimen._22ssp).value * layoutScale).sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicators(
    currentPage: Int,
    layoutScale: Float,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R_sdp.dimen._6sdp) * layoutScale,
        ),
    ) {
        introPages.indices.forEach { index ->
            PageIndicator(
                isActive = currentPage == index,
                layoutScale = layoutScale,
            )
        }
    }
}

@Composable
private fun PageIndicator(
    isActive: Boolean,
    layoutScale: Float,
) {
    val width by animateDpAsState(
        targetValue = if (isActive) {
            dimensionResource(R_sdp.dimen._18sdp) * layoutScale
        } else {
            dimensionResource(R_sdp.dimen._8sdp) * layoutScale
        },
        animationSpec = tween(300),
        label = "indicator_width",
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(dimensionResource(R_sdp.dimen._8sdp) * layoutScale)
            .clip(CircleShape)
            .background(
                colorResource(
                    if (isActive) {
                        R.color.colors_FB3675
                    } else {
                        R.color.colors_FDA3C0
                    },
                ),
            ),
    )
}

@Composable
private fun IntroPreviewAdPlaceholder() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._171sdp))
            .background(colorResource(R.color.colors_E5E5E5)),
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun IntroPageOnePreview() {
    IntroPageContent(
        pageIndex = 0,
        currentPage = 0,
        showNativeAd = false,
        onActionClick = {},
        adContent = {},
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun IntroPageTwoPreview() {
    IntroPageContent(
        pageIndex = 1,
        currentPage = 1,
        showNativeAd = false,
        onActionClick = {},
        adContent = {},
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun IntroPageThreePreview() {
    IntroPageContent(
        pageIndex = 2,
        currentPage = 2,
        showNativeAd = true,
        onActionClick = {},
        adContent = { IntroPreviewAdPlaceholder() },
    )
}
