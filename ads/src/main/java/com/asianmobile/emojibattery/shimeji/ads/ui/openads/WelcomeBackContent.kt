package com.asianmobile.emojibattery.shimeji.ads.ui.openads

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.asianmobile.emojibattery.shimeji.ads.R
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp

@Composable
fun WelcomeBackContent(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        Image(
            painter = painterResource(R.drawable.img_onboarding_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(screenHeight * (43f / 800f))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WelcomeBackPet(
                frameSize = screenWidth * (150f / 360f),
                imageSize = screenWidth * (171f / 360f),
                imageOffsetX = -(screenWidth * (8.3f / 360f)),
                imageOffsetY = -(screenWidth * (18.4f / 360f)),
            )
            Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._6sdp)))
            WelcomeBackTitle(
                text = stringResource(R.string.welcome_back),
                modifier = Modifier.fillMaxWidth(353f / 360f),
            )
        }
    }
}

@Composable
private fun WelcomeBackPet(
    frameSize: androidx.compose.ui.unit.Dp,
    imageSize: androidx.compose.ui.unit.Dp,
    imageOffsetX: androidx.compose.ui.unit.Dp,
    imageOffsetY: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val previewPainter = painterResource(R.drawable.img_welcome_back_pet_preview)
    Box(
        modifier = modifier
            .requiredSize(frameSize)
            .clip(RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalInspectionMode.current) {
            Image(
                painter = previewPainter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(imageSize)
                    .offset(
                        x = imageOffsetX,
                        y = imageOffsetY,
                    ),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.img_welcome_back_pet)
                    .crossfade(false)
                    .build(),
                placeholder = previewPainter,
                error = previewPainter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(imageSize)
                    .offset(
                        x = imageOffsetX,
                        y = imageOffsetY,
                    ),
            )
        }
    }
}

@Composable
private fun WelcomeBackTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.20f to colorResource(R.color.colors_FF96B8),
            0.69231f to colorResource(R.color.colors_FF417E),
        ),
    )
    val fontSize = dimensionResource(R_ssp.dimen._26ssp).value.sp
    val lineHeight = dimensionResource(R_ssp.dimen._31ssp).value.sp
    val letterSpacing = dimensionResource(R_ssp.dimen._1ssp).value.sp
    val titleFont = FontFamily(Font(R.font.nunito_black, FontWeight.Black))
    val outlineWidth = with(LocalDensity.current) {
        dimensionResource(R_sdp.dimen._1sdp).toPx()
    }
    val shadowBlur = with(LocalDensity.current) {
        dimensionResource(R_sdp.dimen._3sdp).toPx()
    }
    val sharedStyle = TextStyle(
        fontFamily = titleFont,
        fontWeight = FontWeight.Black,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        textAlign = TextAlign.Center,
    )

    Box(
        modifier = modifier.height(dimensionResource(R_sdp.dimen._31sdp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = sharedStyle.copy(
                color = colorResource(R.color.colors_FFFFFF),
                shadow = Shadow(
                    color = colorResource(R.color.colors_FF0044).copy(alpha = 0.60f),
                    offset = Offset.Zero,
                    blurRadius = shadowBlur,
                ),
                drawStyle = Stroke(width = outlineWidth),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = text,
            style = sharedStyle.copy(brush = gradient),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WelcomeBackContentPreview() {
    WelcomeBackContent()
}
