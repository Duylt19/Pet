package com.asianmobile.emojibattery.shimeji.ui.battery.catalog

import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.BatteryCatalogError
import com.asianmobile.emojibattery.shimeji.data.model.BUILT_IN_BATTERY_CATEGORY_ID
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun BatteryCatalogScreen(
    onBack: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: BatteryCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requiresRewardAd = !state.isPremium && state.themes.any { theme ->
        theme.assetsReady &&
            theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
            theme.id !in state.rewardUnlockedThemeIds
    }
    TrackScreenView(ScreenName.BATTERY_CATALOG)
    LaunchedEffect(context, requiresRewardAd) {
        if (requiresRewardAd) {
            RewardedVideoAds.getInstance().loadRewardedVideo(context.applicationContext)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BatteryCatalogEffect.OpenTheme -> onOpenTheme(effect.themeId)
                BatteryCatalogEffect.ShowRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onRewardResult(
                            RewardedAdResult.UNAVAILABLE.shouldContinueFlow
                        )
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
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshEntitlement()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BatteryCatalogContent(
        state = state,
        onBack = onBack,
        onCategory = viewModel::selectCategory,
        onSearch = viewModel::updateSearchQuery,
        onFavorite = viewModel::toggleFavorite,
        onTheme = viewModel::requestTheme,
        onRetry = viewModel::refresh
    )
    val pendingTheme = state.themes.firstOrNull { it.id == state.pendingUnlockThemeId }
    if (pendingTheme != null) {
        BatteryRewardUnlockDialog(
            themeName = pendingTheme.name,
            isLoading = state.isRewardInProgress,
            rewardNotEarned = state.message == BatteryCatalogMessage.REWARD_NOT_EARNED,
            onDismiss = viewModel::dismissUnlockDialog,
            onWatchReward = viewModel::requestRewardUnlock,
            onPremium = onNavigateToPremium
        )
    } else if (state.message == BatteryCatalogMessage.THEME_UNAVAILABLE) {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text(stringResource(R.string.battery_theme_unavailable_title)) },
            text = { Text(stringResource(R.string.battery_theme_unavailable_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text(stringResource(R.string.common_done))
                }
            }
        )
    }
}

@Composable
private fun BatteryCatalogContent(
    state: BatteryCatalogUiState,
    onBack: () -> Unit,
    onCategory: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onFavorite: (Int) -> Unit,
    onTheme: (BatteryThemeEntry) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
    ) {
        CutePetTopBar(title = stringResource(R.string.battery_catalog_title), onBack = onBack)
        Text(
            text = stringResource(R.string.battery_catalog_subtitle),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        )
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.battery_search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._16sdp),
                    vertical = dimensionResource(SdpR.dimen._8sdp)
                )
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._16sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._7sdp))
        ) {
            item {
                CategoryChip(
                    label = stringResource(R.string.battery_catalog_all),
                    selected = state.selectedCategoryId == null,
                    onClick = { onCategory(null) }
                )
            }
            items(state.categories.size) { index ->
                val category = state.categories[index]
                CategoryChip(
                    label = if (category.id == BUILT_IN_BATTERY_CATEGORY_ID) {
                        stringResource(R.string.battery_builtin_category)
                    } else {
                        category.name
                    },
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onCategory(category.id) }
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
        if (state.error != null) {
            Text(
                text = stringResource(R.string.battery_catalog_local_data_hint),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRetry)
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._16sdp),
                        vertical = dimensionResource(SdpR.dimen._5sdp)
                    )
            )
        }
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.colors_12B890))
            }
            state.visibleThemes.isEmpty() -> EmptyCatalog(state.error, onRetry)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._16sdp),
                    end = dimensionResource(SdpR.dimen._16sdp),
                    bottom = dimensionResource(SdpR.dimen._20sdp)
                ),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._10sdp)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._10sdp))
            ) {
                items(state.visibleThemes, key = BatteryThemeEntry::id) { theme ->
                    ThemeCard(
                        theme = theme,
                        favorite = theme.id in state.favoriteThemeIds,
                        locked = theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
                            !state.isPremium &&
                            theme.id !in state.rewardUnlockedThemeIds,
                        onFavorite = { onFavorite(theme.id) },
                        onClick = { onTheme(theme) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun BatteryRewardUnlockDialog(
    themeName: String,
    isLoading: Boolean,
    rewardNotEarned: Boolean,
    onDismiss: () -> Unit,
    onWatchReward: () -> Unit,
    onPremium: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = {
            Text(stringResource(R.string.battery_reward_unlock_title, themeName))
        },
        text = {
            Column {
                Text(stringResource(R.string.battery_reward_unlock_message))
                if (rewardNotEarned) {
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
                    Text(
                        text = stringResource(R.string.battery_reward_not_earned),
                        color = colorResource(R.color.colors_E45D6A)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onWatchReward,
                enabled = !isLoading
            ) {
                Text(
                    stringResource(
                        if (isLoading) {
                            R.string.battery_reward_loading
                        } else {
                            R.string.battery_reward_watch
                        }
                    )
                )
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onPremium,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.battery_reward_get_premium))
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = colorResource(if (selected) R.color.colors_FFFFFF else R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_semibold)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
            .background(
                colorResource(if (selected) R.color.colors_12B890 else R.color.colors_FFFFFF)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._8sdp)
            )
    )
}

@Composable
private fun ThemeCard(
    theme: BatteryThemeEntry,
    favorite: Boolean,
    locked: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val displayName = if (theme.isBuiltIn) {
        stringResource(R.string.battery_builtin_theme)
    } else {
        theme.name
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = colorResource(R.color.colors_EDE4FF),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp))
            )
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._8sdp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._105sdp)),
            contentAlignment = Alignment.Center
        ) {
            if (theme.thumbnailPath != null) {
                AsyncImage(
                    model = theme.thumbnailPath,
                    contentDescription = displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BuiltInThemePreview()
            }
            IconButton(
                onClick = onFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(dimensionResource(SdpR.dimen._28sdp))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FFFFFF))
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.battery_favorite_action),
                    tint = colorResource(
                        if (favorite) R.color.colors_E45D6A else R.color.colors_776D84
                    )
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                modifier = Modifier.weight(1f)
            )
            if (locked) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = stringResource(R.string.battery_premium_theme),
                    tint = colorResource(R.color.colors_FE9D00),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._16sdp))
                )
            }
        }
    }
}

@Composable
private fun BuiltInThemePreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(colorResource(R.color.colors_12B890))
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.battery_preview_time),
            color = colorResource(R.color.colors_FFFFFF)
        )
        Text(
            text = stringResource(R.string.battery_preview_percentage),
            color = colorResource(R.color.colors_FFFFFF)
        )
    }
}

@Composable
private fun EmptyCatalog(error: BatteryCatalogError?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onRetry)
            .padding(dimensionResource(SdpR.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (error == null) {
                stringResource(R.string.battery_catalog_empty)
            } else {
                stringResource(R.string.battery_catalog_local_data_hint)
            },
            color = colorResource(R.color.colors_776D84),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = stringResource(R.string.battery_retry),
            color = colorResource(R.color.colors_12B890)
        )
    }
}
