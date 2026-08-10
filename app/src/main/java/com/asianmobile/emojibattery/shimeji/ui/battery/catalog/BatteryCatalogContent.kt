package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_BATTERY_CATEGORY_INLINE
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogCategory
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.ui.component.CATALOG_ITEM_PREVIEW_FRACTION
import com.asianmobile.emojibattery.shimeji.ui.component.HomeEnableCard
import com.asianmobile.emojibattery.shimeji.ui.component.HomeHeader
import com.asianmobile.emojibattery.shimeji.ui.component.HomePremiumButton
import com.asianmobile.emojibattery.shimeji.ui.component.PetPremiumBadge
import com.asianmobile.emojibattery.shimeji.ui.component.RewardGradientButton
import com.asianmobile.emojibattery.shimeji.ui.component.RewardOfferSheet
import com.asianmobile.emojibattery.shimeji.ui.component.RewardOutlineButton
import com.asianmobile.emojibattery.shimeji.ui.discover.HomeDiyFab
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private const val BATTERY_DETAIL_PREVIEW_FRACTION = 0.7303f

private val BatteryRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val BatteryRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

@Composable
internal fun BatteryCatalogContent(
    state: BatteryCatalogUiState,
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onBatteryToggle: () -> Unit,
    onCustomizeStatusBar: () -> Unit,
    onOpenCategory: (Int) -> Unit,
    onFavorite: (Int) -> Unit,
    onTheme: (BatteryThemeEntry) -> Unit,
    onRetry: () -> Unit,
    nativeAdContent: @Composable () -> Unit = { BatteryCatalogNativeAd() }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        BatteryWallpaper()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HomeHeader(onSearch = onSearch, onPremium = onPremium)
            HomeEnableCard(
                text = stringResource(
                    if (state.isBatteryEnabled) {
                        R.string.discover_battery_enabled
                    } else {
                        R.string.discover_battery_enable_prompt
                    }
                ),
                checked = state.isBatteryEnabled,
                onCheckedChange = onBatteryToggle
            )
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = dimensionResource(SdpR.dimen._5sdp),
                        bottom = dimensionResource(SdpR.dimen._62sdp)
                    )
                ) {
                    item(key = "battery_customize_banner") {
                        Image(
                            painter = painterResource(R.drawable.img_battery_customize_banner),
                            contentDescription = stringResource(
                                R.string.discover_customize_status_bar
                            ),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
                                .height(dimensionResource(SdpR.dimen._62sdp))
                                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                                .clickable(onClick = onCustomizeStatusBar)
                        )
                    }

                    when {
                        state.isLoading && state.sections.isEmpty() -> item {
                            BatteryCatalogLoading()
                        }

                        state.sections.isEmpty() -> item {
                            BatteryCatalogEmpty(onRetry = onRetry)
                        }

                        else -> state.sections.forEachIndexed { index, section ->
                            item(key = "battery_section_${section.category.id}") {
                                Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                                BatteryCatalogSection(
                                    section = section,
                                    favoriteThemeIds = state.favoriteThemeIds,
                                    rewardUnlockedThemeIds = state.rewardUnlockedThemeIds,
                                    isPremium = state.isPremium,
                                    onMore = { onOpenCategory(section.category.id) },
                                    onFavorite = onFavorite,
                                    onTheme = onTheme
                                )
                            }
                            if (index == 0) {
                                item(key = "battery_native_ad") {
                                    Spacer(
                                        Modifier.height(dimensionResource(SdpR.dimen._15sdp))
                                    )
                                    Box(
                                        modifier = Modifier.padding(
                                            horizontal = dimensionResource(SdpR.dimen._12sdp)
                                        )
                                    ) {
                                        nativeAdContent()
                                    }
                                }
                            }
                        }
                    }
                }
                HomeDiyFab(
                    onClick = onCustomizeStatusBar,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = dimensionResource(SdpR.dimen._11sdp),
                            bottom = dimensionResource(SdpR.dimen._8sdp)
                        )
                )
            }
        }
    }
}

@Composable
private fun BatteryCatalogSection(
    section: BatteryCatalogSection,
    favoriteThemeIds: Set<Int>,
    rewardUnlockedThemeIds: Set<Int>,
    isPremium: Boolean,
    onMore: () -> Unit,
    onFavorite: (Int) -> Unit,
    onTheme: (BatteryThemeEntry) -> Unit
) {
    Column {
        BatterySectionHeader(
            title = batteryCategoryDisplayName(section.category.name),
            onMore = onMore
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            items(section.themes.size, key = { section.themes[it].id }) { index ->
                val theme = section.themes[index]
                BatteryLandingThemeCard(
                    theme = theme,
                    favorite = theme.id in favoriteThemeIds,
                    premium = theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
                        !isPremium && theme.id !in rewardUnlockedThemeIds,
                    onFavorite = { onFavorite(theme.id) },
                    onClick = { onTheme(theme) }
                )
            }
        }
    }
}

@Composable
private fun BatterySectionHeader(title: String, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._18sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_battery_category_placeholder),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._14sdp))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = BatteryRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onMore)
                .padding(start = dimensionResource(SdpR.dimen._5sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.discover_more),
                color = colorResource(R.color.colors_212327),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._2sdp)))
            Image(
                painter = painterResource(R.drawable.ic_home_chevron),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._11sdp))
            )
        }
    }
}

@Composable
private fun BatteryLandingThemeCard(
    theme: BatteryThemeEntry,
    favorite: Boolean,
    premium: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._85sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_DEDEDF),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BatteryThemePreview(
            theme = theme,
            modifier = Modifier.fillMaxSize(CATALOG_ITEM_PREVIEW_FRACTION)
        )
        if (premium) {
            PetPremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(SdpR.dimen._5sdp))
                    .size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(SdpR.dimen._5sdp))
                .size(dimensionResource(SdpR.dimen._18sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_1A000000))
                .clickable(onClick = onFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_recent_heart),
                contentDescription = stringResource(R.string.discover_favorite_theme),
                tint = colorResource(
                    if (favorite) R.color.colors_FB3675 else R.color.colors_FFFFFF
                ),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
        }
    }
}

@Composable
internal fun BatteryCategoryContent(
    category: BatteryCatalogCategory?,
    themes: List<BatteryThemeEntry>,
    selectedThemeId: Int,
    isPremium: Boolean,
    rewardUnlockedThemeIds: Set<Int>,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onTheme: (BatteryThemeEntry) -> Unit,
    inlineBannerContent: @Composable () -> Unit = {
        BannerAd(adPosition = BANNER_BATTERY_CATEGORY_INLINE)
    }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        BatteryWallpaper()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            BatteryCategoryHeader(
                title = category?.name?.let(::batteryCategoryDisplayName).orEmpty(),
                onBack = onBack,
                onPremium = onPremium
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    top = dimensionResource(SdpR.dimen._9sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = dimensionResource(SdpR.dimen._12sdp)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._12sdp)
                )
            ) {
                item(
                    key = "battery_category_inline_banner",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(SdpR.dimen._38sdp))
                            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp))),
                        contentAlignment = Alignment.Center
                    ) {
                        inlineBannerContent()
                    }
                }
                items(themes, key = BatteryThemeEntry::id) { theme ->
                    BatteryDetailThemeCard(
                        theme = theme,
                        selected = theme.id == selectedThemeId,
                        premium = theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
                            !isPremium && theme.id !in rewardUnlockedThemeIds,
                        onClick = { onTheme(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryCategoryHeader(
    title: String,
    onBack: () -> Unit,
    onPremium: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = dimensionResource(SdpR.dimen._65sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_recent_back),
                contentDescription = stringResource(R.string.battery_category_back),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._22sdp))
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
            Text(
                text = title,
                color = colorResource(R.color.colors_212327),
                fontFamily = BatteryRobotoSemiBold,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            HomePremiumButton(onClick = onPremium)
        }
    }
}

@Composable
private fun BatteryDetailThemeCard(
    theme: BatteryThemeEntry,
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
                colorResource(
                    if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF
                )
            )
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(
                    if (selected) R.color.colors_FB3675 else R.color.colors_DEDEDF
                ),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BatteryThemePreview(
            theme = theme,
            modifier = Modifier.fillMaxSize(BATTERY_DETAIL_PREVIEW_FRACTION)
        )
        if (premium) {
            PetPremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(SdpR.dimen._5sdp))
                    .size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
    }
}

@Composable
private fun BatteryThemePreview(
    theme: BatteryThemeEntry,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val displayName = batteryThemeDisplayName(theme.name)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (theme.thumbnailPath == null) {
            Image(
                painter = painterResource(R.drawable.ic_home_battery),
                contentDescription = displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = theme.thumbnailPath,
                contentDescription = displayName,
                contentScale = contentScale,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun BatteryRewardUnlockSheet(
    theme: BatteryThemeEntry,
    isLoading: Boolean,
    rewardNotEarned: Boolean,
    onDismiss: () -> Unit,
    onWatchReward: () -> Unit,
    onPremium: () -> Unit
) {
    RewardOfferSheet(onDismiss = if (isLoading) ({}) else onDismiss) {
        BatteryRewardUnlockSheetContent(
            theme = theme,
            isLoading = isLoading,
            rewardNotEarned = rewardNotEarned,
            onWatchReward = onWatchReward,
            onPremium = onPremium
        )
    }
}

@Composable
internal fun BatteryRewardUnlockSheetContent(
    theme: BatteryThemeEntry,
    isLoading: Boolean,
    rewardNotEarned: Boolean,
    onWatchReward: () -> Unit,
    onPremium: () -> Unit,
    nativeAdContent: @Composable () -> Unit = { BatteryRewardNativeAd() }
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
        BatteryThemePreview(
            theme = theme,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._74sdp))
        )
    }
    Text(
        text = stringResource(R.string.battery_reward_unlock_title),
        color = colorResource(R.color.colors_212327),
        fontFamily = BatteryRobotoMedium,
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
            fontFamily = BatteryRobotoMedium,
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
private fun BatteryRewardNativeAd() {
    NativeAdInternal(
        screenCode = SCREEN_HOME,
        instanceKey = "battery_reward_sheet",
        adTypeOverride = AdType.HEIGHT_222,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BatteryCatalogNativeAd() {
    NativeAdInternal(
        screenCode = SCREEN_HOME,
        adTypeOverride = AdType.HEIGHT_150,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BatteryCatalogLoading() {
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
private fun BatteryCatalogEmpty(onRetry: () -> Unit) {
    Text(
        text = stringResource(R.string.battery_catalog_empty),
        color = colorResource(R.color.colors_6F7073),
        fontFamily = BatteryRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry)
            .padding(dimensionResource(SdpR.dimen._24sdp))
    )
}

@Composable
private fun BatteryWallpaper() {
    Image(
        painter = painterResource(R.drawable.img_home_wallpaper),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._600sdp))
    )
}

@Composable
internal fun BatteryAdPreviewSlot(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._115sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_DEDEDF),
                RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
            )
            .padding(dimensionResource(SdpR.dimen._6sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._5sdp))
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(colorResource(R.color.colors_DEDEDF))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._5sdp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(dimensionResource(SdpR.dimen._15sdp))
                    .background(colorResource(R.color.colors_DEDEDF))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._30sdp))
                    .background(colorResource(R.color.colors_F0F0F0))
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._34sdp))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FB3675))
            )
        }
    }
}

@Composable
internal fun BatteryInlineBannerPreviewSlot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFEBF1))
    )
}

@Composable
internal fun BatteryRewardAdPreviewSlot() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._171sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
            .background(colorResource(R.color.colors_FEFEFE))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_E5E5E5),
                RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
            )
            .padding(dimensionResource(SdpR.dimen._9sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            Box(
                Modifier
                    .size(dimensionResource(SdpR.dimen._37sdp))
                    .background(colorResource(R.color.colors_DEDEDF))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(dimensionResource(SdpR.dimen._12sdp))
                        .background(colorResource(R.color.colors_DEDEDF))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.35f)
                        .height(dimensionResource(SdpR.dimen._9sdp))
                        .background(colorResource(R.color.colors_F0F0F0))
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colorResource(R.color.colors_DEDEDF))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FB3675))
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BatteryCatalogContentPreview() {
    val state = previewBatteryCatalogState()
    BatteryCatalogContent(
        state = state,
        onSearch = {},
        onPremium = {},
        onBatteryToggle = {},
        onCustomizeStatusBar = {},
        onOpenCategory = {},
        onFavorite = {},
        onTheme = {},
        onRetry = {},
        nativeAdContent = { BatteryAdPreviewSlot() }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BatteryCategoryContentPreview() {
    val state = previewBatteryCatalogState()
    val section = state.sections.first()
    val detailThemes = previewBatteryDetailThemes(section)
    BatteryCategoryContent(
        category = section.category,
        themes = detailThemes,
        selectedThemeId = detailThemes[1].id,
        isPremium = false,
        rewardUnlockedThemeIds = emptySet(),
        onBack = {},
        onPremium = {},
        onTheme = {},
        inlineBannerContent = { BatteryInlineBannerPreviewSlot() }
    )
}

internal fun previewBatteryDetailThemes(section: BatteryCatalogSection): List<BatteryThemeEntry> =
    List(18) { index ->
        section.themes[index % section.themes.size].copy(id = 100 + index)
    }

internal fun previewBatteryCatalogState(): BatteryCatalogUiState {
    val category = BatteryCatalogCategory(1, "🔥 Trending", "trending", 1)
    val themes = List(6) { index ->
        BatteryThemeEntry(
            id = index + 1,
            name = "Battery Theme ${index + 1}",
            categoryId = category.id,
            categoryName = category.name,
            entitlement = if (index % 2 == 0) {
                BatteryThemeEntitlement.PREMIUM
            } else {
                BatteryThemeEntitlement.FREE
            },
            thumbnailPath = null,
            batteryPath = null,
            emojiPath = null,
            assetsReady = true
        )
    }
    val section = BatteryCatalogSection(category, themes)
    return BatteryCatalogUiState(
        themes = themes,
        categories = listOf(category),
        sections = listOf(
            section,
            section.copy(category = category.copy(id = 2, name = "Cute")),
            section.copy(category = category.copy(id = 3, name = "Anime"))
        ),
        selectedThemeId = themes[1].id,
        favoriteThemeIds = setOf(themes[0].id, themes[3].id),
        isLoading = false
    )
}
