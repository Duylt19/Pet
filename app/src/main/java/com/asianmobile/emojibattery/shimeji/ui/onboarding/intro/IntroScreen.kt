@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.asianmobile.emojibattery.shimeji.ui.onboarding.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

private data class IntroPage(
    val titleRes: Int,
    val imageRes: Int,
    val imageAspectRatio: Float,
    val nativeScreenCode: String? = null,
)

private val introPages = listOf(
    IntroPage(
        titleRes = R.string.intro_title_1,
        imageRes = R.drawable.img_intro1,
        imageAspectRatio = 360f / 534f,
        nativeScreenCode = SCREEN_INTRO,
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
        nativeScreenCode = SCREEN_INTRO_SECOND,
    ),
)

@Composable
fun IntroScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    TrackScreenView(
        when (currentPage) {
            0 -> ScreenName.INTRO_PAGE_1
            1 -> ScreenName.INTRO_PAGE_2
            else -> ScreenName.INTRO_PAGE_3
        },
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
    ) { pageIndex ->
        IntroPageContent(
            pageIndex = pageIndex,
            currentPage = currentPage,
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
            adContent = { screenCode ->
                NativeAdInternal(
                    screenCode = screenCode,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
internal fun IntroPageContent(
    pageIndex: Int,
    currentPage: Int,
    onActionClick: () -> Unit,
    adContent: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = introPages[pageIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
    ) {
        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .aspectRatio(page.imageAspectRatio),
            contentScale = ContentScale.FillBounds,
        )

        if (page.nativeScreenCode == null) {
            MiddleIntroPageContent(
                page = page,
                currentPage = currentPage,
                onNextClick = onActionClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            NativeIntroPageContent(
                page = page,
                pageIndex = pageIndex,
                currentPage = currentPage,
                onActionClick = onActionClick,
                adContent = adContent,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun NativeIntroPageContent(
    page: IntroPage,
    pageIndex: Int,
    currentPage: Int,
    onActionClick: () -> Unit,
    adContent: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        IntroCopyAndControls(
            page = page,
            currentPage = currentPage,
            sideActionText = if (pageIndex == introPages.lastIndex) {
                stringResource(R.string.start)
            } else {
                stringResource(R.string.next)
            },
            onActionClick = onActionClick,
        )
        adContent(requireNotNull(page.nativeScreenCode))
    }
}

@Composable
private fun MiddleIntroPageContent(
    page: IntroPage,
    currentPage: Int,
    onNextClick: () -> Unit,
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
        )
        IntroPrimaryButton(onClick = onNextClick)
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))
    }
}

@Composable
private fun IntroCopyAndControls(
    page: IntroPage,
    currentPage: Int,
    sideActionText: String?,
    onActionClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._86sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))
        Text(
            text = stringResource(page.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R_sdp.dimen._46sdp))
                .padding(
                    start = dimensionResource(R_sdp.dimen._24sdp),
                    end = dimensionResource(R_sdp.dimen._24sdp),
                ),
            color = colorResource(R.color.colors_333538),
            fontFamily = nunitoBlackFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = dimensionResource(R_ssp.dimen._17ssp).value.sp,
            lineHeight = dimensionResource(R_ssp.dimen._23ssp).value.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))

        if (sideActionText == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._22sdp)),
                contentAlignment = Alignment.Center,
            ) {
                PageIndicators(currentPage = currentPage)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._22sdp))
                    .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageIndicators(currentPage = currentPage)
                Text(
                    text = sideActionText,
                    color = colorResource(R.color.colors_FB3675),
                    fontFamily = robotoSemiBoldFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = dimensionResource(R_ssp.dimen._15ssp).value.sp,
                    lineHeight = dimensionResource(R_ssp.dimen._22ssp).value.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = dimensionResource(R_sdp.dimen._3sdp)),
                )
            }
        }
    }
}

@Composable
private fun IntroPrimaryButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R_sdp.dimen._15sdp))
            .height(dimensionResource(R_sdp.dimen._37sdp))
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.colors_FB54BB),
                        colorResource(R.color.colors_FF5D7D),
                    ),
                ),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.next),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = robotoMediumFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(R_ssp.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(R_ssp.dimen._22ssp).value.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicators(
    currentPage: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._6sdp)),
    ) {
        introPages.indices.forEach { index ->
            PageIndicator(isActive = currentPage == index)
        }
    }
}

@Composable
private fun PageIndicator(
    isActive: Boolean,
) {
    val width by animateDpAsState(
        targetValue = if (isActive) {
            dimensionResource(R_sdp.dimen._18sdp)
        } else {
            dimensionResource(R_sdp.dimen._8sdp)
        },
        animationSpec = tween(300),
        label = "indicator_width",
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(dimensionResource(R_sdp.dimen._8sdp))
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
        onActionClick = {},
        adContent = { IntroPreviewAdPlaceholder() },
    )
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun IntroPageTwoPreview() {
    IntroPageContent(
        pageIndex = 1,
        currentPage = 1,
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
        onActionClick = {},
        adContent = { IntroPreviewAdPlaceholder() },
    )
}
