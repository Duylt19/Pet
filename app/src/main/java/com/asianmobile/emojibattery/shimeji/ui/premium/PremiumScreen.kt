package com.asianmobile.emojibattery.shimeji.ui.premium

import android.app.Activity
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import android.content.Intent
import com.asianmobile.emojibattery.shimeji.utils.ToastHelper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.theme.RobotoFontFamily
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.asianmobile.emojibattery.shimeji.constant.BASE_MONTHLY_COST_ID
import com.asianmobile.emojibattery.shimeji.constant.BASE_WEEKLY_COST_ID
import com.asianmobile.emojibattery.shimeji.constant.BASE_YEARLY_COST_ID

import com.intuit.sdp.R as R_dimen
import com.intuit.ssp.R as R_dimen_ssp

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 4/6/2026
 */

enum class StartPremiumIndexes() {
    ONBOARDING_FIRST,  // Hiện sau màn Intro
    SPLASH_RETURN, // Xuất hiện từ phiên thứ 2 (sau khi vào được Home) sau màn SplashScreen
    IN_APP
}

@Preview
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel(),
    startByIndex: StartPremiumIndexes = StartPremiumIndexes.IN_APP,
    onClose: (StartPremiumIndexes) -> Unit = {},
    buyPremiumSuccess: (StartPremiumIndexes) -> Unit = {}
) {
    TrackScreenView(ScreenName.PREMIUM)
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

        val purchaseSuccess by viewModel.purchaseSuccess.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.setLoading(false)
                    viewModel.checkExistingPurchases()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val priceWeeklyState by viewModel.priceWeeklyCost.collectAsState()
        val priceWeekly = priceWeeklyState.takeIf { it.isNotEmpty() }
            ?: ""

        val priceMonthlyState by viewModel.priceMonthlyCost.collectAsState()
        val priceMonthly = priceMonthlyState.takeIf { it.isNotEmpty() }
            ?: ""

        val priceYearlyState by viewModel.priceYearlyCost.collectAsState()
        val priceYearly = priceYearlyState.takeIf { it.isNotEmpty() }
            ?: ""

        val priceWeekInMonth by viewModel.priceWeekInMonthCost.collectAsState()
        val priceWeekInYear by viewModel.priceWeekInYearCost.collectAsState()

        LaunchedEffect(purchaseSuccess) {
            if (purchaseSuccess) {
                SharedPreferencesUtils.setIsPremium(context, true)
                SharedPreferencesUtils.setIsEnableAds(context, false)
                buyPremiumSuccess(startByIndex)
            }
        }

        BackHandler {
            onClose(startByIndex)
        }

        val scrollState = rememberScrollState()
        var selectedPackage by remember { mutableIntStateOf(1) }
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")

        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.08f, // độ phóng to
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scaleAnim"
        )
        val shape = RoundedCornerShape(dimensionResource(id = R_dimen.dimen._20sdp))

        val brush = Brush.verticalGradient(
            colors = listOf(
                colorResource(id = R.color.blue_C8E6FF),
                colorResource(id = R.color.white),
                colorResource(id = R.color.white)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_premium))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._8sdp)))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._17sdp)))
                    Text(
                        text = stringResource(id = R.string.premium_title).uppercase(),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorResource(id = R.color.blue_37AEFE),
                                    colorResource(id = R.color.blue_0570FC)
                                )
                            ),
                            fontSize = dimensionResource(id = R_dimen_ssp.dimen._25ssp).value.sp,
                            fontFamily = FontFamily(Font(com.asianmobile.emojibattery.shimeji.ads.R.font.sf_pro_heavy)),
                            fontWeight = FontWeight.W900
                        )
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_home_premium),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dimensionResource(id = R_dimen.dimen._24sdp))
                            .offset(
                                x = (-dimensionResource(id = R_dimen.dimen._11sdp)),
                                y = (-dimensionResource(id = R_dimen.dimen._13sdp))
                            )
                            .rotate(29f)
                    )
                }

                Text(
                    text = stringResource(id = R.string.premium_subtitle),
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorResource(id = R.color.orange_FFB700),
                                colorResource(id = R.color.orange_FF6B27)
                            )
                        ),
                        fontSize = dimensionResource(id = R_dimen_ssp.dimen._16ssp).value.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    ),
                    modifier = Modifier.padding(bottom = dimensionResource(id = R_dimen.dimen._16sdp))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(id = R_dimen.dimen._16sdp),
                            vertical = dimensionResource(id = R_dimen.dimen._6sdp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.premium_all_features),
                        fontSize = dimensionResource(id = R_dimen_ssp.dimen._12ssp).value.sp,
                        color = colorResource(id = R.color.gray_4D4D4D),
                        fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(id = R.string.premium_free),
                        fontSize = dimensionResource(id = R_dimen_ssp.dimen._12ssp).value.sp,
                        color = colorResource(id = R.color.gray_4D4D4D),
                        fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._40sdp))
                    )
                    Text(
                        text = stringResource(id = R.string.premium_pro),
                        fontSize = dimensionResource(id = R_dimen_ssp.dimen._12ssp).value.sp,
                        color = colorResource(id = R.color.blue_007BFD),
                        fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._40sdp))
                    )
                }

                FeatureRow(
                    icon = R.drawable.ic_premium_quick_recovery,
                    title = stringResource(id = R.string.premium_private_browsing),
                    freeChecked = true
                )
                FeatureRow(
                    icon = R.drawable.ic_premium_message_recovery,
                    title = stringResource(id = R.string.premium_unlimited_downloads),
                    freeChecked = true
                )
                FeatureRow(
                    icon = R.drawable.ic_premium_remove_ads,
                    title = stringResource(id = R.string.premium_remove_ads),
                    freeChecked = false
                )
                FeatureRow(
                    icon = R.drawable.ic_premium_deep_scan,
                    title = stringResource(id = R.string.premium_faster_downloads),
                    freeChecked = false
                )
                FeatureRow(
                    icon = R.drawable.ic_premium_file_vault,
                    title = stringResource(id = R.string.premium_secure_file_vault),
                    freeChecked = false
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._13sdp)))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R_dimen.dimen._13sdp)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    PackageCard(
                        title = stringResource(id = R.string.premium_weekly),
                        price = priceWeekly,
                        desc = null,
                        isSelected = selectedPackage == 0,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.tagPlanId = BASE_WEEKLY_COST_ID
                            selectedPackage = 0
                        }
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._9sdp)))
                    PackageCard(
                        title = stringResource(id = R.string.premium_monthly),
                        price = priceMonthly,
                        desc = priceWeekInMonth?.let {
                            stringResource(id = R.string.premium_desc_in_week, it)
                        } ?: "",
                        isSelected = selectedPackage == 1,
                        saleIcon = R.drawable.ic_premium_sales_monthly,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.tagPlanId = BASE_MONTHLY_COST_ID
                            selectedPackage = 1
                        }
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._9sdp)))
                    PackageCard(
                        title = stringResource(id = R.string.premium_yearly),
                        price = priceYearly,
                        desc = priceWeekInYear?.let {
                            stringResource(id = R.string.premium_desc_in_week, it)
                        } ?: "",
                        isSelected = selectedPackage == 2,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.tagPlanId = BASE_YEARLY_COST_ID
                            selectedPackage = 2
                        }
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._16sdp)))

                Box(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = R_dimen.dimen._16sdp))
                        .fillMaxWidth()
                        .height(dimensionResource(id = R_dimen.dimen._40sdp))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(shape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorResource(id = R.color.blue_37AEFE),
                                    colorResource(id = R.color.blue_007BFD) // thêm màu thứ 2
                                )
                            ),
                            shape = shape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = true,
                                color = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            if (!Utils.isNetworkAvailable(context)) {
                                ToastHelper.show(
                                    context,
                                    context.getString(R.string.please_check_your_internet_connection_and_try_again)
                                )
                                return@clickable
                            }
                            activity?.let {
                                viewModel.onClickBuyPremium(it)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.premium_subscribe_now),
                        color = colorResource(id = R.color.white),
                        fontSize = dimensionResource(id = R_dimen_ssp.dimen._14ssp).value.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_medium))
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._7sdp)))

                Text(
                    text = stringResource(id = R.string.premium_privacy_policy),
                    color = colorResource(id = R.color.gray_666666),
                    textDecoration = TextDecoration.Underline,
                    fontSize = dimensionResource(id = R_dimen_ssp.dimen._9ssp).value.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    modifier = Modifier
                        .padding(bottom = dimensionResource(id = R_dimen.dimen._4sdp))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        context.getString(R.string.privacy_policy_default_url).toUri()
                                    )
                                )
                            }
                        }
                )

                Text(
                    text = stringResource(id = R.string.premium_disclaimer),
                    color = colorResource(id = R.color.gray_666666),
                    fontSize = dimensionResource(id = R_dimen_ssp.dimen._9ssp).value.sp,
                    lineHeight = dimensionResource(id = R_dimen_ssp.dimen._11ssp).value.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R_dimen.dimen._12sdp))
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._14sdp)))
            }

            Icon(
                painter = painterResource(R.drawable.ic_back_premium),
                contentDescription = stringResource(id = R.string.back),
                tint = Color(0xFF0D0D0D),
                modifier = Modifier
                    .padding(dimensionResource(id = R_dimen.dimen._13sdp))
                    .size(dimensionResource(R_dimen.dimen._26sdp))
                    .padding(dimensionResource(id = R_dimen.dimen._5sdp))
                    .clickable { onClose(startByIndex) }
                    .align(alignment = Alignment.TopEnd)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {}, // block interact
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
}

@Composable
fun FeatureRow(icon: Int, title: String, freeChecked: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R_dimen.dimen._16sdp),
                    vertical = dimensionResource(id = R_dimen.dimen._5sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R_dimen.dimen._17sdp))
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._7sdp)))
            Text(
                text = title,
                fontSize = dimensionResource(id = R_dimen_ssp.dimen._12ssp).value.sp,
                color = colorResource(id = R.color.gray_4D4D4D),
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._40sdp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = if (freeChecked) R.drawable.ic_premium_checked else R.drawable.ic_premium_close),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R_dimen.dimen._18sdp))
                )
            }
            Box(
                modifier = Modifier.width(dimensionResource(id = R_dimen.dimen._40sdp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_premium_checked),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R_dimen.dimen._18sdp))
                )
            }
        }
}

@Composable
fun PackageCard(
    title: String,
    price: String,
    desc: String?,
    isSelected: Boolean,
    saleIcon: Int? = null,
    modifier: Modifier,
    onClick: () -> Unit
) {
        val interactionSource = remember { MutableInteractionSource() }
        val borderColor =
            if (isSelected) colorResource(id = R.color.blue_007BFD) else colorResource(id = R.color.gray_E0E0E0)
        val bgColor =
            if (isSelected) colorResource(id = R.color.blue_F0F8FF) else colorResource(id = R.color.white)

        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.06f else 0.94f,
            label = "scale"
        )

        Box(
            modifier = modifier
                .zIndex(if (isSelected) 1f else 0f)
                .scale(scale)
                .aspectRatio(0.86f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = dimensionResource(id = R_dimen.dimen._8sdp))
                    .border(
                        1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(dimensionResource(id = R_dimen.dimen._8sdp))
                    )
                    .background(
                        color = bgColor,
                        shape = RoundedCornerShape(dimensionResource(id = R_dimen.dimen._8sdp))
                    )
                    .padding(dimensionResource(id = R_dimen.dimen._4sdp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._3sdp)))
                AutoResizeText(
                    text = title,
                    fontSize = dimensionResource(id = if (isSelected) R_dimen_ssp.dimen._12ssp else R_dimen_ssp.dimen._10ssp).value.sp,
                    color = colorResource(id = R.color.black_171717),
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._10sdp)))
                AutoResizeText(
                    text = price,
                    fontSize = dimensionResource(id = if (isSelected) R_dimen_ssp.dimen._13ssp else R_dimen_ssp.dimen._11ssp).value.sp,
                    color = colorResource(id = R.color.black_171717),
                    fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R_dimen.dimen._7sdp)))
                AutoResizeText(
                    text = desc ?: "",
                    fontSize = dimensionResource(id = if (isSelected) R_dimen_ssp.dimen._9ssp else R_dimen_ssp.dimen._8ssp).value.sp,
                    color = colorResource(id = R.color.gray_8A8A8A),
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    textAlign = TextAlign.Center
                )
            }
            if (saleIcon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = saleIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .width(dimensionResource(id = R_dimen.dimen._65sdp))
                            .height(dimensionResource(id = R_dimen.dimen._18sdp)),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = stringResource(id = R.string.premium_sale_79).uppercase(),
                        color = colorResource(id = R.color.white),
                        fontSize = dimensionResource(id = R_dimen_ssp.dimen._8ssp).value.sp,
                        fontFamily = RobotoFontFamily,
                        modifier = Modifier,
                        lineHeight = dimensionResource(id = R_dimen_ssp.dimen._8ssp).value.sp
                    )
                }
            }
        }
}

@Composable
fun AutoResizeText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier
) {
    var multiplier by remember(text, fontSize) { mutableFloatStateOf(1f) }
    Text(
        text = text,
        fontSize = fontSize * multiplier,
        color = color,
        fontFamily = fontFamily,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.fillMaxWidth(),
        onTextLayout = {
            if (it.hasVisualOverflow && multiplier > 0.3f) {
                multiplier *= 0.9f
            }
        },
        lineHeight = fontSize
    )
}
