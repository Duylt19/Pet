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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
private const val INTRO_PAGE_ONE_COPY_TOP = 466f
private const val INTRO_PAGE_TWO_COPY_TOP = 624f
private const val INTRO_PAGE_THREE_COPY_TOP = 466f

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

    // Track only after the pager settles so an aborted swipe cannot emit a page the user did
    // not actually enter.
    TrackScreenView(introPageScreenName(pagerState.settledPage))

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
    ) { pageIndex ->
        IntroPageContent(
            pageIndex = pageIndex,
            currentPage = currentPage,
            showNativeAd = shouldLoadIntroNativeAd(
                pageIndex = pageIndex,
                settledPage = pagerState.settledPage,
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
                NativeAdInternal(
                    screenCode = SCREEN_INTRO_SECOND,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

internal fun introPageScreenName(pageIndex: Int): ScreenName = when (pageIndex) {
    0 -> ScreenName.INTRO_PAGE_1
    1 -> ScreenName.INTRO_PAGE_2
    else -> ScreenName.INTRO_PAGE_3
}

internal fun shouldLoadIntroNativeAd(
    pageIndex: Int,
    settledPage: Int,
): Boolean = pageIndex == introPages.lastIndex && settledPage == introPages.lastIndex

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
            0 -> IntroCopyAndControls(
                page = page,
                currentPage = currentPage,
                sideActionText = stringResource(R.string.next),
                onActionClick = onActionClick,
                layoutScale = compactHeightScale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = referenceUnit * INTRO_PAGE_ONE_COPY_TOP * compactHeightScale,
                    ),
            )

            1 -> MiddleIntroPageContent(
                page = page,
                currentPage = currentPage,
                onNextClick = onActionClick,
                layoutScale = compactHeightScale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = referenceUnit * INTRO_PAGE_TWO_COPY_TOP * compactHeightScale,
                    ),
            )

            else -> if (showNativeAd) {
                NativeIntroPageContent(
                    page = page,
                    currentPage = currentPage,
                    onActionClick = onActionClick,
                    adContent = adContent,
                    layoutScale = compactHeightScale,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else {
                IntroCopyAndControls(
                    page = page,
                    currentPage = currentPage,
                    sideActionText = stringResource(R.string.start),
                    onActionClick = onActionClick,
                    layoutScale = compactHeightScale,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            y = referenceUnit * INTRO_PAGE_THREE_COPY_TOP * compactHeightScale,
                        ),
                )
            }
        }
    }
}

@Composable
private fun NativeIntroPageContent(
    page: IntroPage,
    currentPage: Int,
    onActionClick: () -> Unit,
    adContent: @Composable () -> Unit,
    layoutScale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        IntroCopyAndControls(
            page = page,
            currentPage = currentPage,
            sideActionText = stringResource(R.string.start),
            onActionClick = onActionClick,
            layoutScale = layoutScale,
        )
        adContent()
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
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._86sdp) * layoutScale),
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
                .height(dimensionResource(R_sdp.dimen._46sdp) * layoutScale)
                .padding(
                    start = dimensionResource(R_sdp.dimen._24sdp) * layoutScale,
                    end = dimensionResource(R_sdp.dimen._24sdp) * layoutScale,
                ),
            color = colorResource(R.color.colors_333538),
            fontFamily = nunitoBlackFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (dimensionResource(R_ssp.dimen._17ssp).value * layoutScale).sp,
            lineHeight = (dimensionResource(R_ssp.dimen._23ssp).value * layoutScale).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
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
                width = dimensionResource(R_sdp.dimen._2sdp) * layoutScale,
                color = colorResource(R.color.colors_FF5D7D),
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
