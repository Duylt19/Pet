package com.asianmobile.emojibattery.shimeji.ui.language

import android.app.Activity
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.IS_SHOW_NATIVE_LANGUAGE_SECOND
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_LANGUAGE
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_LANGUAGE_SECOND
import com.asianmobile.emojibattery.shimeji.ads.data.CheckShowAdsUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.utils.SafeRemoteConfig
import com.asianmobile.emojibattery.shimeji.ads.utils.Utils
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp

@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel = hiltViewModel(),
    isSettings: Boolean = false,
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {}
) {

    TrackScreenView(
        if (isSettings) ScreenName.LANGUAGE_SETTINGS else ScreenName.LANGUAGE_ONBOARDING
    )
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable {
        mutableStateOf("")
    }
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
    val context = LocalContext.current
    val checkShowNative = remember {
        context.findActivity()?.let {
            CheckShowAdsUtil.checkShowNativeAd(it)
        } ?: run { false }
    }

    LanguageContent(
        languages = languages,
        selectedKey = selectedKey,
        showConfirm = isShowCheckedLanguage,
        isSettings = isSettings,
        isLoading = !loadAdsComplete,
        isSupportBlur = isSupportBlur,
        onLanguageSelected = { language ->
            if (language.key != selectedKey) {
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
        },
        onConfirm = { viewModel.updateLanguage(onConfirm) },
        onBack = onBack,
        adContent = {
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
        },
        loadingContent = {
            LanguageLoadingContent(
                animLoading = animLoading,
                animLoadingProgress = animLoadingProgress,
            )
        },
    )
}

@Composable
internal fun LanguageContent(
    languages: List<Language>,
    selectedKey: String,
    showConfirm: Boolean,
    isSettings: Boolean,
    isLoading: Boolean,
    isSupportBlur: Boolean,
    onLanguageSelected: (Language) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    adContent: @Composable () -> Unit,
    loadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val blurRadius = if (isLoading && isSupportBlur) 8.dp else 0.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.colors_FFFFFF))
                        .statusBarsPadding()
                        .height(dimensionResource(R_sdp.dimen._43sdp))
                        .padding(horizontal = dimensionResource(R_sdp.dimen._12sdp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSettings) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = colorResource(R.color.colors_FB3675),
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
                        color = colorResource(R.color.colors_FB3675)
                    )

                    AnimatedVisibility(
                        visible = showConfirm,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_checked_language),
                            contentDescription = stringResource(R.string.confirm),
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier
                                .size(dimensionResource(R_sdp.dimen._22sdp))
                                .clip(CircleShape)
                                .clickable(onClick = onConfirm)
                                .padding(dimensionResource(R_sdp.dimen._2sdp))
                        )
                    }
                }
            },
            containerColor = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.colors_FFFFFF))
                    .padding(top = padding.calculateTopPadding())
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            top = dimensionResource(R_sdp.dimen._2sdp),
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
                            onClick = { onLanguageSelected(language) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(dimensionResource(R_sdp.dimen._13sdp)))
                    }
                }

                adContent()
            }
        }

        if (isLoading) {
            val overlayColor = if (isSupportBlur) {
                colorResource(R.color.colors_FFFFFF).copy(alpha = 0.35f)
            } else {
                colorResource(R.color.colors_FFFFFF).copy(alpha = 0.92f)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlayColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                loadingContent()
            }
        }
    }
}

@Composable
private fun LanguageLoadingContent(
    animLoading: com.airbnb.lottie.LottieComposition?,
    animLoadingProgress: Float,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
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
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontSize = dimensionResource(id = R_ssp.dimen._13ssp).value.sp,
            color = colorResource(R.color.colors_262626),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = dimensionResource(R_sdp.dimen._30sdp))
        )
    }
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun LanguageContentPreview() {
    val languages = LocalContext.current.mockData()
    LanguageContent(
        languages = languages,
        selectedKey = languages.first().key,
        showConfirm = true,
        isSettings = false,
        isLoading = false,
        isSupportBlur = false,
        onLanguageSelected = {},
        onConfirm = {},
        onBack = {},
        adContent = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R_sdp.dimen._171sdp))
                    .background(colorResource(R.color.colors_E5E5E5)),
            )
        },
        loadingContent = {},
    )
}

fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
