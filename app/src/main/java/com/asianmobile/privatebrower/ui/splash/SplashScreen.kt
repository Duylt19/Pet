package com.asianmobile.privatebrower.ui.splash

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.ads.config.BANNER_SPLASH_BOTTOM
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.data.SharedPreferencesUtils
import com.asianmobile.privatebrower.ads.data.SharedPreferencesUtils.setClicksAdsLimit
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialLauncherUtil
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.ads.ui.compose.BannerAd
import com.asianmobile.privatebrower.ui.component.TransparentStatusBarEffect
import com.asianmobile.privatebrower.ui.main.MainViewModel
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
    TransparentStatusBarEffect(useDarkIcons = false)

    TrackScreenView(ScreenName.SPLASH)
    val context = LocalContext.current
    val animatedState = rememberAnimatedTextState()
    LaunchedEffect(Unit) {
        animatedState.start()
    }

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

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.img_splash_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(0.3f))
                    SplashLogo()
                    Spacer(modifier = Modifier.height(dimensionResource(id = R_sdp.dimen._9sdp)))
                    JumpAnimatedText(
                        state = animatedState,
                        text = stringResource(R.string.splash_title),
                        style = TextStyle(
                            fontSize = dimensionResource(id = R_ssp.dimen._22ssp).value.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_600)),
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic,
                            color = colorResource(R.color.colors_FFFFFF),
                        ),
                        textAlign = TextAlign.Center,
                        animateOnMount = true,
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(id = R_sdp.dimen._7sdp))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    val splashLoadingAnim by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.splash_loading)
                    )
                    val splashLoadingProgress by animateLottieCompositionAsState(
                        splashLoadingAnim,
                        iterations = LottieConstants.IterateForever
                    )
                    LottieAnimation(
                        composition = splashLoadingAnim,
                        progress = { splashLoadingProgress },
                        modifier = Modifier.size(dimensionResource(id = R_sdp.dimen._48sdp))
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R_sdp.dimen._3sdp)))

                    Text(
                        text = stringResource(R.string.this_action_can_contain_ads),
                        fontSize = dimensionResource(id = R_ssp.dimen._11ssp).value.sp,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        color = colorResource(R.color.colors_FFFFFF),
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(id = R_sdp.dimen._13sdp))
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R_sdp.dimen._18sdp)))
                }

                // Banner ad at bottom of splash
                BannerAd(
                    modifier = Modifier.fillMaxWidth(),
                    adPosition = BANNER_SPLASH_BOTTOM
                )
            }
        }
    }
}

@Composable
private fun SplashLogo(
    modifier: Modifier = Modifier,
) {
    val logoShape = RoundedCornerShape(dimensionResource(id = R_sdp.dimen._18sdp))
    val shadowColor = colorResource(R.color.colors_000000).copy(alpha = 0.25f)

    Box(
        modifier = modifier.size(dimensionResource(id = R_sdp.dimen._92sdp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    x = dimensionResource(id = R_sdp.dimen._2sdp),
                    y = dimensionResource(id = R_sdp.dimen._3sdp),
                )
                .blur(
                    radius = dimensionResource(id = R_sdp.dimen._6sdp),
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(shadowColor, logoShape),
        )
        Image(
            painter = painterResource(R.drawable.ic_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(logoShape),
        )
    }
}
