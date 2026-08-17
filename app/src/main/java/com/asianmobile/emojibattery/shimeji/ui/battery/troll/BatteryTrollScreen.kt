package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import com.asianmobile.emojibattery.shimeji.ads.config.DIALOG_BATTERY_TROLL_REWARD
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollBatteryOrientation
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.remote.BatteryTrollServerConfig
import com.asianmobile.emojibattery.shimeji.ui.battery.editor.StatusBarEditorWallpaper
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomePremiumButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.PetPremiumBadge
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOfferSheet
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardUnlockActions
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

// Figma: the 96 unit thumbnail inside the 101.33 unit tile, so the art keeps its white frame.
private const val TROLL_THUMBNAIL_FRACTION = 96f / 101.33f
// Figma `8326:8469` → `Frame 2147223483` is 110x110 and holds the `248 3` art at 84x84.
private const val TROLL_REWARD_PREVIEW_FRACTION = 84f / 110f

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
        onWatchReward = viewModel::requestRewardUnlock
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
            // The host hides the system navigation bar and renders the destination ad below
            // this Scaffold. Reserving Scaffold's default bottom inset therefore creates an
            // empty strip between the grid and the ad on some Android 10 Samsung devices.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    // Figma leaves 8 units between the title block and the first grid row.
                    top = dimensionResource(SdpR.dimen._6sdp),
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
                        BatteryTrollEmpty(error = state.error, onRetry = onRetry)
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
                fontFamily = RobotoFontFamily,
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
                    painter = painterResource(R.drawable.ic_favorite_recent_back),
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
            // No Figma frame draws a lock layer on a troll tile, but the owner asked a troll
            // theme to signal lock exactly like a battery style does, so the badge keeps the
            // geometry `BatteryCatalogContent.BatteryLandingThemeCard` uses.
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
                dimensionResource(SdpR.dimen._1sdp),
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
        fontFamily = RobotoFontFamily,
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
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    RewardUnlockActions(
        premiumText = stringResource(R.string.battery_reward_get_premium),
        rewardText = stringResource(
            if (isLoading) R.string.battery_reward_loading else R.string.battery_reward_watch
        ),
        onPremium = onPremium,
        onReward = onWatchReward,
        enabled = !isLoading,
        rewardIconRes = if (isLoading) null else R.drawable.ic_pet_store_reward_video
    )
    nativeAdContent()
}

@Composable
private fun BatteryTrollRewardNativeAd() {
    NativeAdInternal(
        screenCode = DIALOG_BATTERY_TROLL_REWARD,
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

/**
 * The three catalog failures are not interchangeable, so the empty grid must not collapse them
 * into one message. `DISTRIBUTION_NOT_APPROVED` means the server has not published the catalog
 * yet — in a release build `BatteryTrollDistributionPolicy` rejects it no matter how often the
 * user taps — so that case is the only one without a retry affordance.
 *
 * Pure so the mapping is unit-testable without rendering the grid.
 */
internal fun batteryTrollEmptyMessageRes(error: BatteryTrollCatalogError?): Int = when (error) {
    BatteryTrollCatalogError.CATALOG_UNAVAILABLE -> R.string.battery_troll_error_offline
    BatteryTrollCatalogError.CATALOG_INVALID -> R.string.battery_troll_error_invalid
    BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED -> R.string.battery_troll_unpublished
    null -> R.string.battery_troll_empty
}

/** Retrying only ever helps when the catalog itself is reachable and publishable. */
internal fun batteryTrollCanRetry(error: BatteryTrollCatalogError?): Boolean =
    error != BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED

@Composable
private fun BatteryTrollEmpty(error: BatteryTrollCatalogError?, onRetry: () -> Unit) {
    val messageRes = batteryTrollEmptyMessageRes(error)
    val canRetry = batteryTrollCanRetry(error)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(SdpR.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Text(
            text = stringResource(messageRes),
            color = colorResource(R.color.colors_6F7073),
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (canRetry) {
            Text(
                text = stringResource(R.string.battery_troll_retry),
                color = colorResource(R.color.colors_FB3675),
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
                    .border(
                        dimensionResource(SdpR.dimen._1sdp),
                        colorResource(R.color.colors_FB3675),
                        RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
                    )
                    .clickable(onClick = onRetry)
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._18sdp),
                        vertical = dimensionResource(SdpR.dimen._8sdp)
                    )
            )
        }
    }
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
            // The published catalog numbers the level assets `_1` to `_5`, not `_0` to `_4`.
            emojiPaths = List(5) { level -> "emoji/TROLL_${index + 1}_${level + 1}.webp" },
            batteryPaths = List(5) { level -> "battery/TROLL_${index + 1}_${level + 1}.webp" }
        )
    }
    return BatteryTrollUiState(trolls = trolls, isLoading = false)
}

/** Empty grid for one of the catalog failures; `null` is "catalog fine, just nothing in it". */
internal fun previewBatteryTrollErrorState(
    error: BatteryTrollCatalogError?
): BatteryTrollUiState = BatteryTrollUiState(isLoading = false, error = error)

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun BatteryTrollUnpublishedPreview() {
    BatteryTrollContent(
        state = previewBatteryTrollErrorState(
            BatteryTrollCatalogError.DISTRIBUTION_NOT_APPROVED
        ),
        onBack = {},
        onPremium = {},
        onTroll = {},
        onRetry = {},
        onDismissReward = {},
        onWatchReward = {},
        nativeAdContent = {}
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun BatteryTrollOfflinePreview() {
    BatteryTrollContent(
        state = previewBatteryTrollErrorState(BatteryTrollCatalogError.CATALOG_UNAVAILABLE),
        onBack = {},
        onPremium = {},
        onTroll = {},
        onRetry = {},
        onDismissReward = {},
        onWatchReward = {},
        nativeAdContent = {}
    )
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
        nativeAdContent = {}
    )
}
