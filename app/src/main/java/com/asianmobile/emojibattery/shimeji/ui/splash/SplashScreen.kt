package com.asianmobile.emojibattery.shimeji.ui.splash

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_SPLASH_BOTTOM
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils.setClicksAdsLimit
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialLauncherUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ui.main.MainViewModel
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp

/**
 * SplashScreen handled initial ad consent and Splash Interstitial Ad.
 */
@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    skipLauncherAd: Boolean = false,
    onNextScreen: () -> Unit
) {

    TrackScreenView(ScreenName.SPLASH)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        setClicksAdsLimit(context)
        val billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .setListener { _, _ -> }
            .build()
        Log.e("TAG", "onBillingSetupFinished: 000000000", )
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {}
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS).build()
                    ) { billingResult1: BillingResult, list: List<Purchase> ->
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                            SharedPreferencesUtils.setIsPremium(context, list.isNotEmpty())
                            SharedPreferencesUtils.setIsEnableAds(context, !list.isNotEmpty())
                        }
                        billingClient.endConnection()
                    }
                }
            }
        })

        (context as? Activity)?.let { activity ->
            activity.application?.let { application ->
                viewModel.refreshRemoteConfig {
                    InterstitialUtil.getInstance()
                        .requestConsentForm(activity, application) {
                            onNextScreen()
                            if (!skipLauncherAd) {
                                InterstitialLauncherUtil.getInstance()
                                    .showInterstitialAdLauncher(activity) {
                                    }
                            }
                        }
                }
            } ?: onNextScreen()
        } ?: onNextScreen()
    }

    SplashContent(
        banner = {
            BannerAd(
                modifier = Modifier.fillMaxWidth(),
                adPosition = BANNER_SPLASH_BOTTOM,
            )
        },
    )
}

@Composable
internal fun SplashContent(
    modifier: Modifier = Modifier,
    banner: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.img_onboarding_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val contentHeight = maxHeight

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentHeight * (131f / 750f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_splash_battery_pet),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxWidth(286f / 360f)
                            .aspectRatio(286f / 278f),
                    )
                    SplashGradientTitle(
                        text = stringResource(R.string.splash_title),
                        modifier = Modifier.fillMaxWidth(353f / 360f),
                    )
                }

                SplashLoadingFooter(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = dimensionResource(R_sdp.dimen._15sdp)),
                )
            }

            banner()
        }
    }
}

@Composable
private fun SplashGradientTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.20f to colorResource(R.color.colors_FF96B8),
            0.69231f to colorResource(R.color.colors_FF417E),
        ),
    )
    val titleFont = FontFamily(Font(R.font.nunito_black, FontWeight.Black))
    val fontSize = dimensionResource(R_ssp.dimen._26ssp).value.sp
    val lineHeight = dimensionResource(R_ssp.dimen._31ssp).value.sp
    val letterSpacing = dimensionResource(R_ssp.dimen._1ssp).value.sp
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
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = text,
            style = sharedStyle.copy(brush = gradient),
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SplashLoadingFooter(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(280f / 360f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val progressShape = RoundedCornerShape(dimensionResource(R_sdp.dimen._77sdp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R_sdp.dimen._7sdp))
                .clip(progressShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_FF417E),
                            colorResource(R.color.colors_FF96B8),
                        ),
                    ),
                )
                .border(
                    width = dimensionResource(R_sdp.dimen._1sdp),
                    color = colorResource(R.color.colors_FFFFFF),
                    shape = progressShape,
                ),
        )
        Text(
            text = stringResource(R.string.this_action_can_contain_ads),
            color = colorResource(R.color.colors_000000),
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontSize = dimensionResource(R_ssp.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(R_ssp.dimen._18ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R_sdp.dimen._3sdp)),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SplashContentPreview() {
    SplashContent(
        banner = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._50sdp))
                    .background(colorResource(R.color.colors_FFFFFF)),
            )
        },
    )
}
