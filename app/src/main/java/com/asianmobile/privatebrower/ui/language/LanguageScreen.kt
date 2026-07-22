package com.asianmobile.privatebrower.ui.language

import android.app.Activity
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.asianmobile.privatebrower.utils.ScreenName
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.IS_SHOW_NATIVE_LANGUAGE_SECOND
import com.asianmobile.privatebrower.ads.config.SCREEN_LANGUAGE
import com.asianmobile.privatebrower.ads.config.SCREEN_LANGUAGE_SECOND
import com.asianmobile.privatebrower.ads.data.CheckShowAdsUtil
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ads.utils.SafeRemoteConfig
import com.asianmobile.privatebrower.ads.utils.Utils
import com.asianmobile.privatebrower.ui.component.TransparentStatusBarEffect
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp

@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel = hiltViewModel(),
    isSettings: Boolean = false,
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    TransparentStatusBarEffect(useDarkIcons = false)

    TrackScreenView(
        if (isSettings) ScreenName.LANGUAGE_SETTINGS else ScreenName.LANGUAGE_ONBOARDING
    )
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable {
        mutableStateOf("")
    }
    val listState = rememberLazyListState()
    var isShowCheckedLanguage by remember { mutableStateOf(false) }
    var loadAdsComplete by remember { mutableStateOf(false) }

    val animLoading by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.anim_loading_language)
    )
    val animLoadingProgress by animateLottieCompositionAsState(
        animLoading,
        iterations = LottieConstants.IterateForever
    )
    var reloadCounter by remember { mutableIntStateOf(0) }
    val isSupportBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurRadius = if (!loadAdsComplete && isSupportBlur) 8.dp else 0.dp
    val context = LocalContext.current
    val checkShowNative = remember {
        context.findActivity()?.let {
            CheckShowAdsUtil.checkShowNativeAd(it)
        } ?: run { false }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.colors_161718))
                        .statusBarsPadding()
                        .height(dimensionResource(R_sdp.dimen._43sdp))
                        .padding(horizontal = dimensionResource(R_sdp.dimen._18sdp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSettings) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = colorResource(R.color.white),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(dimensionResource(R_sdp.dimen._25sdp))
                                .clip(CircleShape)
                                .clickable(onClick = onBack)
                                .padding(dimensionResource(R_sdp.dimen._3sdp))
                        )
                    }

                    Text(
                        text = stringResource(R.string.language),
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = dimensionResource(id = R_ssp.dimen._15ssp).value.sp,
                        lineHeight = dimensionResource(id = R_ssp.dimen._22ssp).value.sp,
                        color = colorResource(R.color.white)
                    )

                    AnimatedVisibility(
                        visible = isShowCheckedLanguage,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_checked_language),
                            contentDescription = stringResource(R.string.confirm),
                            tint = colorResource(R.color.colors_3369FD),
                            modifier = Modifier
                                .size(dimensionResource(R_sdp.dimen._22sdp))
                                .clickable {
                                    viewModel.updateLanguage(onConfirm)
                                }
                        )
                    }
                }
            },
            containerColor = colorResource(R.color.colors_161718),
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.colors_161718))
                    .padding(top = padding.calculateTopPadding())
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            top = dimensionResource(R_sdp.dimen._6sdp),
                            start = dimensionResource(R_sdp.dimen._12sdp),
                            end = dimensionResource(R_sdp.dimen._12sdp)
                        ),
                    // Figma: gap 12px → 12/1.3 ≈ 9 → _9sdp
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._9sdp))
                ) {
                    items(
                        items = languages,
                        key = { it.id },
                        contentType = { "language" }
                    ) { language ->
                        val selected = language.key == selectedKey
                        LanguageItem(
                            language = language,
                            isSelected = selected,
                            onClick = {
                                if (language.key == selectedKey) return@LanguageItem
                                viewModel.languageSelected = language
                                selectedKey = language.key
                                isShowCheckedLanguage = true
                                if (
                                    loadAdsComplete &&
                                    Utils.isNetworkAvailable(context) &&
                                    SafeRemoteConfig.getBoolean(IS_SHOW_NATIVE_LANGUAGE_SECOND) &&
                                    checkShowNative
                                ) {
                                    reloadCounter++
                                    loadAdsComplete = false
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._13sdp)))
                    }
                }

                if (!isShowCheckedLanguage) {
                    NativeAdInternal(
                        screenCode = SCREEN_LANGUAGE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        loadAdsComplete = true
                    }
                } else {
                    NativeAdInternal(
                        screenCode = SCREEN_LANGUAGE_SECOND,
                        modifier = Modifier.fillMaxWidth(),
                        reloadKey = reloadCounter
                    ) {
                        loadAdsComplete = true
                    }
                }
            }
        }

        if (!loadAdsComplete) {
            val overlayColor = if (isSupportBlur) {
                colorResource(R.color.colors_161718).copy(alpha = 0.35f)
            } else {
                colorResource(R.color.colors_161718).copy(alpha = 0.92f)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlayColor)
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                LottieAnimation(
                    composition = animLoading,
                    progress = { animLoadingProgress },
                    modifier = Modifier
                        .height(dimensionResource(R_sdp.dimen._95sdp))
                        .width(dimensionResource(R_sdp.dimen._95sdp))
                )
                Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._3sdp)))
                Text(
                    text = stringResource(R.string.setting_up_your_app_experience_please_wait_a_moment),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = dimensionResource(id = R_ssp.dimen._13ssp).value.sp,
                    color = colorResource(R.color.white),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = dimensionResource(R_sdp.dimen._30sdp))
                )
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
