package com.asianmobile.privatebrower.ui.setdefault

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ads.config.SCREEN_SET_DEFAULT
import com.asianmobile.privatebrower.ui.component.TransparentStatusBarEffect
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun SetDefaultBrowserScreen(
    onCompleted: () -> Unit,
    viewModel: SetDefaultBrowserViewModel = hiltViewModel()
) {
    TransparentStatusBarEffect(useDarkIcons = false)

    TrackScreenView(ScreenName.SET_DEFAULT_BROWSER)
    val context = LocalContext.current

    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onRoleResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateEvent.collect { onCompleted() }
    }

    BackHandler {
        (context as? Activity)?.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    top = dimensionResource(SdpR.dimen._18sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._18sdp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.setdefault_header_title),
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontSemiBold,
                color = colorResource(R.color.colors_FFFFFF),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.setdefault_header_subtitle),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                fontFamily = fontRegular,
                color = colorResource(R.color.colors_9B9C9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._3sdp))
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = dimensionResource(SdpR.dimen._12sdp),
                end = dimensionResource(SdpR.dimen._12sdp)
            ),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(SdpR.dimen._6sdp)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "private_browser") {
                BenefitCard(
                    iconRes = R.drawable.img_setdefault_icon_private,
                    badgeColor = colorResource(R.color.colors_4F46E5),
                    borderColor = colorResource(R.color.colors_4D5856D6),
                    gradientStartColor = colorResource(R.color.colors_1A5856D6),
                    title = stringResource(R.string.setdefault_benefit_private_title),
                    description = stringResource(R.string.setdefault_benefit_private_desc),
                    fontSemiBold = fontSemiBold,
                    fontRegular = fontRegular
                )
            }

            item(key = "fast_download") {
                BenefitCard(
                    iconRes = R.drawable.img_setdefault_icon_download,
                    badgeColor = colorResource(R.color.colors_16A34A),
                    borderColor = colorResource(R.color.colors_4D34C759),
                    gradientStartColor = colorResource(R.color.colors_1A34C759),
                    title = stringResource(R.string.setdefault_benefit_download_title),
                    description = stringResource(R.string.setdefault_benefit_download_desc),
                    fontSemiBold = fontSemiBold,
                    fontRegular = fontRegular
                )
            }

            item(key = "easy_to_use") {
                BenefitCard(
                    iconRes = R.drawable.img_setdefault_icon_easy,
                    badgeColor = colorResource(R.color.colors_EA580C),
                    borderColor = colorResource(R.color.colors_4DFF9500),
                    gradientStartColor = colorResource(R.color.colors_1AFF9500),
                    title = stringResource(R.string.setdefault_benefit_easy_title),
                    description = stringResource(R.string.setdefault_benefit_easy_desc),
                    fontSemiBold = fontSemiBold,
                    fontRegular = fontRegular
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    top = dimensionResource(SdpR.dimen._12sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._18sdp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(SdpR.dimen._17sdp))
                    .height(dimensionResource(SdpR.dimen._37sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                    .background(colorResource(R.color.colors_3369FD))
                    .clickable {
                        viewModel.onSetDefaultClicked(launcher, context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.setdefault_set_button_label),
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = fontMedium,
                    color = colorResource(R.color.colors_FFFFFF)
                )
            }

            Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

            Text(
                text = stringResource(R.string.setdefault_later_button_label),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                fontFamily = fontRegular,
                color = colorResource(R.color.colors_9B9C9E),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._12sdp),
                        vertical = dimensionResource(SdpR.dimen._6sdp)
                    )
                    .then(
                        Modifier.noRippleClickable { viewModel.onLaterClicked() }
                    )
            )
        }

        // Native ad bottom
        NativeAdInternal(
            screenCode = SCREEN_SET_DEFAULT,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BenefitCard(
    iconRes: Int,
    badgeColor: Color,
    borderColor: Color,
    gradientStartColor: Color,
    title: String,
    description: String,
    fontSemiBold: FontFamily,
    fontRegular: FontFamily
) {
    val cardShape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStartColor, colorResource(R.color.colors_33000000))
                )
            )
            .border(width = 1.dp, color = borderColor, shape = cardShape)
            .padding(dimensionResource(SdpR.dimen._13sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._34sdp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontSemiBold,
                color = colorResource(R.color.colors_FFFFFF)
            )
            Text(
                text = description,
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                fontFamily = fontRegular,
                color = colorResource(R.color.colors_9B9C9E),
                maxLines = 2,
                modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._2sdp))
            )
        }

        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._18sdp))
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_setdefault_check),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._11sdp))
            )
        }
    }
}

// Extension to create non-ripple clickable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        )
    )
