@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.asianmobile.emojibattery.shimeji.ui.intro

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val robotoMediumFontFamily = FontFamily(
    Font(R.font.roboto_medium, FontWeight.Medium)
)
private val interRegularFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal)
)
private val interSemiBoldFontFamily = FontFamily(
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

private data class IntroPage(
    val titleRes: Int,
    val highlightedTitleRes: Int,
    val messageRes: Int,
    val imageRes: Int,
    val nativeScreenCode: String? = null
)

private val introPages = listOf(
    IntroPage(
        titleRes = R.string.intro_title_1,
        highlightedTitleRes = R.string.intro_title_highlight_1,
        messageRes = R.string.intro_message_1,
        imageRes = R.drawable.img_intro1,
        nativeScreenCode = SCREEN_INTRO
    ),
    IntroPage(
        titleRes = R.string.intro_title_2,
        highlightedTitleRes = R.string.intro_title_highlight_2,
        messageRes = R.string.intro_message_2,
        imageRes = R.drawable.img_intro2
    ),
    IntroPage(
        titleRes = R.string.intro_title_3,
        highlightedTitleRes = R.string.intro_title_highlight_3,
        messageRes = R.string.intro_message_3,
        imageRes = R.drawable.img_intro3,
        nativeScreenCode = SCREEN_INTRO_SECOND
    )
)

@Composable
fun IntroScreen(
    onFinish: () -> Unit
) {

    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    TrackScreenView(
        when (currentPage) {
            0 -> ScreenName.INTRO_PAGE_1
            1 -> ScreenName.INTRO_PAGE_2
            else -> ScreenName.INTRO_PAGE_3
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
    ) {
        Image(
            painter = painterResource(R.drawable.img_splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = introPages[pageIndex]
            IntroPageContent(
                page = page,
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
                }
            )
        }
    }
}

@Composable
private fun IntroPageContent(
    page: IntroPage,
    pageIndex: Int,
    currentPage: Int,
    onActionClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(page.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopCenter
            )
        }

        if (page.nativeScreenCode == null) {
            MiddleIntroPageContent(
                page = page,
                currentPage = currentPage,
                onNextClick = onActionClick
            )
        } else {
            NativeIntroPageContent(
                page = page,
                pageIndex = pageIndex,
                currentPage = currentPage,
                onActionClick = onActionClick
            )
        }
    }
}

@Composable
private fun NativeIntroPageContent(
    page: IntroPage,
    pageIndex: Int,
    currentPage: Int,
    onActionClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._12sdp)))
    IntroTextContent(page = page)
    Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._8sdp)))
    SidePageControls(
        currentPage = currentPage,
        isLastPage = pageIndex == introPages.lastIndex,
        onActionClick = onActionClick
    )
    NativeAdInternal(
        screenCode = requireNotNull(page.nativeScreenCode),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MiddleIntroPageContent(
    page: IntroPage,
    currentPage: Int,
    onNextClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._15sdp)))
    IntroTextContent(page = page)
    Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._9sdp)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp))
    ) {
        PageIndicators(currentPage = currentPage)
    }
    Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._25sdp)))
    IntroPrimaryButton(onClick = onNextClick)
    Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._15sdp)))
}

@Composable
private fun IntroTextContent(page: IntroPage) {
    val title = stringResource(page.titleRes)
    val highlightedTitle = stringResource(page.highlightedTitleRes)
    val highlightStart = title.indexOf(highlightedTitle)
    val highlightedColor = colorResource(R.color.colors_3369FD)
    val styledTitle = buildAnnotatedString {
        append(title)
        if (highlightStart >= 0) {
            addStyle(
                style = SpanStyle(color = highlightedColor),
                start = highlightStart,
                end = highlightStart + highlightedTitle.length
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._83sdp))
            .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp))
    ) {
        Text(
            text = styledTitle,
            fontFamily = robotoMediumFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(R_ssp.dimen._18ssp).value.sp,
            lineHeight = dimensionResource(R_ssp.dimen._25ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._3sdp)))
        Text(
            text = stringResource(page.messageRes),
            fontFamily = interRegularFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = dimensionResource(R_ssp.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(R_ssp.dimen._15ssp).value.sp,
            color = colorResource(R.color.colors_9B9C9E),
            maxLines = 2
        )
    }
}

@Composable
private fun SidePageControls(
    currentPage: Int,
    isLastPage: Boolean,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R_sdp.dimen._34sdp))
            .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIndicators(currentPage = currentPage)

        Box(
            modifier = Modifier
                .height(dimensionResource(R_sdp.dimen._34sdp))
                .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._6sdp)))
                .clickable(onClick = onActionClick)
                .padding(horizontal = dimensionResource(R_sdp.dimen._3sdp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLastPage) {
                    stringResource(R.string.start)
                } else {
                    stringResource(R.string.next)
                },
                color = colorResource(R.color.colors_FFFFFF),
                fontSize = dimensionResource(R_ssp.dimen._15ssp).value.sp,
                lineHeight = dimensionResource(R_ssp.dimen._22ssp).value.sp,
                fontFamily = interSemiBoldFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IntroPrimaryButton(onClick: () -> Unit) {
    val glowColor = colorResource(R.color.colors_FFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp))
            .height(dimensionResource(R_sdp.dimen._37sdp))
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._9sdp)))
            .background(colorResource(R.color.colors_3369FD))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to glowColor.copy(alpha = 0.55f),
                        0.45f to glowColor.copy(alpha = 0.22f),
                        1f to glowColor.copy(alpha = 0f)
                    ),
                    center = Offset(
                        x = size.width / 2f,
                        y = size.height
                    ),
                    radius = size.width * 0.34f
                )
            )
        }

        Text(
            text = stringResource(R.string.next),
            fontFamily = interSemiBoldFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(R_ssp.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(R_ssp.dimen._20ssp).value.sp,
            color = colorResource(R.color.colors_FFFFFF),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicators(currentPage: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp))
    ) {
        introPages.indices.forEach { index ->
            PageIndicator(isActive = currentPage == index)
        }
    }
}

@Composable
private fun PageIndicator(isActive: Boolean) {
    val width by animateDpAsState(
        targetValue = if (isActive) {
            dimensionResource(R_sdp.dimen._25sdp)
        } else {
            dimensionResource(R_sdp.dimen._6sdp)
        },
        animationSpec = tween(300),
        label = "indicator_width"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(dimensionResource(R_sdp.dimen._6sdp))
            .clip(RoundedCornerShape(dimensionResource(R_sdp.dimen._3sdp)))
            .background(
                if (isActive) {
                    colorResource(R.color.colors_3369FD)
                } else {
                    colorResource(R.color.colors_FFFFFF).copy(alpha = 0.3f)
                }
            )
    )
}
