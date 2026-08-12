package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_BATTERY_CATEGORY_INLINE
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollServerConfig
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.StatusBarEditorWallpaper
import com.asianmobile.emojibattery.shimeji.ui.shared.component.HomePremiumButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.PetPremiumBadge
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardGradientButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOfferSheet
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOutlineButton
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val TrollRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val TrollRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))

// Figma: the 96 unit thumbnail inside the 101.33 unit tile, so the art keeps its white frame.
private const val TROLL_THUMBNAIL_FRACTION = 96f / 101.33f
// Figma: the reward sheet preview is a 74 unit thumbnail inside the 85 unit pink tile.
private const val TROLL_REWARD_PREVIEW_FRACTION = 74f / 85f

@Composable
fun BatteryTrollScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomize: (Int) -> Unit,
    onPremium: () -> Unit,
    viewModel: BatteryTrollViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.BATTERY_TROLL)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInlineBannerVisible by remember { mutableStateOf(true) }

    val requiresRewardAd = !state.isPremium && state.trolls.any { troll ->
        troll.entitlement == BatteryTrollEntitlement.PREMIUM &&
            troll.id !in state.rewardUnlockedTrollIds
    }
    LaunchedEffect(context, requiresRewardAd) {
        if (requiresRewardAd) {
            RewardedVideoAds.getInstance().loadRewardedVideo(context.applicationContext)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BatteryTrollEffect.OpenCustomize -> onNavigateToCustomize(effect.trollId)

                BatteryTrollEffect.ShowRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onRewardResult(RewardedAdResult.UNAVAILABLE.shouldContinueFlow)
                    } else {
                        RewardedVideoAds.getInstance().showRewardedAd(activity) { result ->
                            viewModel.onRewardResult(result.shouldContinueFlow)
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEntitlement()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BatteryTrollContent(
        state = state,
        onBack = onNavigateBack,
        onPremium = onPremium,
        onTroll = viewModel::requestTroll,
        onRetry = viewModel::refresh,
        onDismissReward = viewModel::dismissUnlockDialog,
        onWatchReward = viewModel::requestRewardUnlock,
        isInlineBannerVisible = isInlineBannerVisible,
        bannerAdContent = {
            BannerAd(
                adPosition = BANNER_BATTERY_CATEGORY_INLINE,
                onVisibilityChanged = { isInlineBannerVisible = it }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BatteryTrollContent(
    state: BatteryTrollUiState,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onTroll: (BatteryTrollEntry) -> Unit,
    onRetry: () -> Unit,
    onDismissReward: () -> Unit,
    onWatchReward: () -> Unit,
    isInlineBannerVisible: Boolean = true,
    bannerAdContent: @Composable () -> Unit = { BatteryTrollInlineBannerAd() },
    nativeAdContent: @Composable () -> Unit = { BatteryTrollRewardNativeAd() }
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Box(Modifier.fillMaxSize()) {
        StatusBarEditorWallpaper()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                BatteryTrollTopBar(
                    onBack = onBack,
                    onPremium = onPremium,
                    collapsedFraction = scrollBehavior.state.collapsedFraction,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._12sdp)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                )
            ) {
                if (isInlineBannerVisible) {
                    item(
                        key = "battery_troll_inline_banner",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            bannerAdContent()
                        }
                    }
                }
                when {
                    state.isLoading && state.trolls.isEmpty() -> item(
                        key = "battery_troll_loading",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        BatteryTrollLoading()
                    }

                    state.trolls.isEmpty() -> item(
                        key = "battery_troll_empty",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        BatteryTrollEmpty(onRetry = onRetry)
                    }

                    else -> items(state.trolls, key = BatteryTrollEntry::id) { troll ->
                        BatteryTrollCard(
                            troll = troll,
                            selected = troll.id == state.selectedTrollId,
                            premium = troll.entitlement == BatteryTrollEntitlement.PREMIUM &&
                                !state.isPremium &&
                                troll.id !in state.rewardUnlockedTrollIds,
                            onClick = { onTroll(troll) }
                        )
                    }
                }
            }
        }
    }

    val pendingTroll = state.trolls.firstOrNull { it.id == state.pendingUnlockTrollId }
    if (pendingTroll != null) {
        RewardOfferSheet(
            onDismiss = if (state.isRewardInProgress) ({}) else onDismissReward
        ) {
            BatteryTrollRewardSheetContent(
                troll = pendingTroll,
                isLoading = state.isRewardInProgress,
                rewardNotEarned = state.message == BatteryTrollMessage.REWARD_NOT_EARNED,
                onWatchReward = onWatchReward,
                onPremium = onPremium,
                nativeAdContent = nativeAdContent
            )
        }
    }
}

/**
 * Figma `8315:7971` expands to 100 units (56 bar + 44 title row) and collapses the 24/32 title
 * into the 56 unit bar as 20/28. Same geometry as the Battery editor bar, so it is built the
 * same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatteryTrollTopBar(
    onBack: () -> Unit,
    onPremium: () -> Unit,
    collapsedFraction: Float,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val expandedSize = dimensionResource(SspR.dimen._18ssp).value
    val collapsedSize = dimensionResource(SspR.dimen._15ssp).value
    val expandedLineHeight = dimensionResource(SspR.dimen._24ssp).value
    val collapsedLineHeight = dimensionResource(SspR.dimen._22ssp).value
    LargeTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.battery_troll_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = TrollRobotoSemiBold,
                fontWeight = FontWeight.SemiBold,
                fontSize = (
                    expandedSize + (collapsedSize - expandedSize) * collapsedFraction
                    ).sp,
                lineHeight = (
                    expandedLineHeight +
                        (collapsedLineHeight - expandedLineHeight) * collapsedFraction
                    ).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = dimensionResource(SdpR.dimen._6sdp))
                    .size(dimensionResource(SdpR.dimen._32sdp))
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.back),
                    tint = colorResource(R.color.colors_212327),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
                )
            }
        },
        actions = {
            HomePremiumButton(
                onClick = onPremium,
                modifier = Modifier.padding(end = dimensionResource(SdpR.dimen._12sdp))
            )
        },
        collapsedHeight = dimensionResource(SdpR.dimen._43sdp),
        expandedHeight = dimensionResource(SdpR.dimen._77sdp),
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun BatteryTrollCard(
    troll: BatteryTrollEntry,
    selected: Boolean,
    premium: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(
                colorResource(if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF)
            )
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(if (selected) R.color.colors_FB3675 else R.color.colors_DEDEDF),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BatteryTrollThumbnail(
            troll = troll,
            modifier = Modifier.fillMaxSize(TROLL_THUMBNAIL_FRACTION)
        )
        if (premium) {
            val lockedLabel = stringResource(R.string.battery_troll_theme_locked)
            PetPremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(SdpR.dimen._5sdp))
                    .size(dimensionResource(SdpR.dimen._18sdp))
                    .semantics { contentDescription = lockedLabel }
            )
        }
    }
}

@Composable
private fun BatteryTrollThumbnail(troll: BatteryTrollEntry, modifier: Modifier) {
    AsyncImage(
        model = BatteryTrollServerConfig.resolve(troll.thumbnailPath),
        contentDescription = troll.name,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.High,
        modifier = modifier
    )
}

@Composable
internal fun BatteryTrollRewardSheetContent(
    troll: BatteryTrollEntry,
    isLoading: Boolean,
    rewardNotEarned: Boolean,
    onWatchReward: () -> Unit,
    onPremium: () -> Unit,
    nativeAdContent: @Composable () -> Unit = { BatteryTrollRewardNativeAd() }
) {
    val previewShape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._85sdp))
            .clip(previewShape)
            .background(colorResource(R.color.colors_FFFBFC))
            .border(
                dimensionResource(SdpR.dimen._2sdp),
                colorResource(R.color.colors_FEC1D4),
                previewShape
            ),
        contentAlignment = Alignment.Center
    ) {
        BatteryTrollThumbnail(
            troll = troll,
            modifier = Modifier.fillMaxSize(TROLL_REWARD_PREVIEW_FRACTION)
        )
    }
    Text(
        text = stringResource(R.string.battery_reward_unlock_title),
        color = colorResource(R.color.colors_212327),
        fontFamily = TrollRobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(312f / 336f)
    )
    if (rewardNotEarned) {
        Text(
            text = stringResource(R.string.battery_reward_not_earned),
            color = colorResource(R.color.colors_FB3675),
            fontFamily = TrollRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        RewardOutlineButton(
            text = stringResource(R.string.battery_reward_get_premium),
            onClick = onPremium,
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            iconRes = R.drawable.img_pet_store_premium_crown
        )
        RewardGradientButton(
            text = stringResource(
                if (isLoading) R.string.battery_reward_loading
                else R.string.battery_reward_watch
            ),
            onClick = onWatchReward,
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            iconRes = if (isLoading) null else R.drawable.ic_pet_store_reward_video
        )
    }
    nativeAdContent()
}

@Composable
private fun BatteryTrollInlineBannerAd() {
    BannerAd(adPosition = BANNER_BATTERY_CATEGORY_INLINE)
}

@Composable
private fun BatteryTrollRewardNativeAd() {
    NativeAdInternal(
        screenCode = SCREEN_HOME,
        instanceKey = "battery_troll_reward_sheet",
        adTypeOverride = AdType.HEIGHT_222,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BatteryTrollLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._115sdp)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colorResource(R.color.colors_FB3675))
    }
}

@Composable
private fun BatteryTrollEmpty(onRetry: () -> Unit) {
    Text(
        text = stringResource(R.string.battery_troll_empty),
        color = colorResource(R.color.colors_6F7073),
        fontFamily = TrollRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry)
            .padding(dimensionResource(SdpR.dimen._24sdp))
    )
}

/** Figma places a 328x50 banner as the first grid child; the stub keeps that footprint. */
@Composable
internal fun BatteryTrollBannerPreviewSlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._38sdp))
            .background(colorResource(R.color.colors_FFEBF1))
    )
}

internal fun previewBatteryTrollState(): BatteryTrollUiState {
    val trolls = List(6) { index ->
        BatteryTrollEntry(
            id = index + 1,
            name = "Troll Theme ${index + 1}",
            slug = "troll-${index + 1}",
            order = index + 1,
            entitlement = if (index % 2 == 0) {
                BatteryTrollEntitlement.PREMIUM
            } else {
                BatteryTrollEntitlement.FREE
            },
            batteryOrientation = BatteryTrollBatteryOrientation.PORTRAIT,
            thumbnailPath = "thumb/TROLL_${index + 1}.webp",
            emojiPaths = List(5) { level -> "emoji/TROLL_${index + 1}_$level.webp" },
            batteryPaths = List(5) { level -> "battery/TROLL_${index + 1}_$level.webp" }
        )
    }
    return BatteryTrollUiState(trolls = trolls, isLoading = false)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BatteryTrollContentPreview() {
    BatteryTrollContent(
        state = previewBatteryTrollState(),
        onBack = {},
        onPremium = {},
        onTroll = {},
        onRetry = {},
        onDismissReward = {},
        onWatchReward = {},
        bannerAdContent = { BatteryTrollBannerPreviewSlot() },
        nativeAdContent = {}
    )
}
