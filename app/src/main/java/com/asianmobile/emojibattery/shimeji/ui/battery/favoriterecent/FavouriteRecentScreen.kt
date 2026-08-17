package com.asianmobile.emojibattery.shimeji.ui.battery.favoriterecent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_FAVOURITE_RECENT
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomePremiumButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AsyncContentState
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AsyncContentStatePanel
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val FavouriteRecentRobotoRegular = RobotoFontFamily
private val FavouriteRecentRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val FavouriteRecentRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))

@Composable
fun FavouriteRecentScreen(
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    viewModel: FavouriteRecentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrackScreenView(ScreenName.FAVOURITE_RECENT)
    FavouriteRecentContent(
        uiState = uiState,
        onBack = onBack,
        onPremium = onPremium,
        onSelectTab = viewModel::selectTab,
        onOpenTheme = onOpenTheme,
        onToggleFavorite = viewModel::toggleFavorite,
        onRetry = viewModel::retry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavouriteRecentContent(
    uiState: FavouriteRecentUiState,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onSelectTab: (FavouriteRecentTab) -> Unit,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRetry: () -> Unit = {},
    showNativeAd: Boolean = true
) {
    val themes = uiState.visibleThemes
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val expandedContentGap = dimensionResource(SdpR.dimen._6sdp)
    val collapsedContentGap = dimensionResource(SdpR.dimen._9sdp)
    val contentGap = expandedContentGap +
        (collapsedContentGap - expandedContentGap) * collapsedFraction

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        Image(
            painter = painterResource(R.drawable.img_home_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                FavouriteRecentLargeTopBar(
                    onBack = onBack,
                    onPremium = onPremium,
                    collapsedFraction = collapsedFraction,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(Modifier.height(contentGap))
                FavouriteRecentTabs(
                    selectedTab = uiState.selectedTab,
                    onSelectTab = onSelectTab
                )

                when {
                    uiState.isLoading && themes.isEmpty() -> {
                        AsyncContentStatePanel(
                            state = AsyncContentState.LOADING,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    uiState.selectedTab == FavouriteRecentTab.FAVOURITE &&
                        uiState.catalogLoadFailed && themes.isEmpty() -> {
                        AsyncContentStatePanel(
                            state = AsyncContentState.LOAD_FAILED,
                            modifier = Modifier.weight(1f),
                            onRetry = onRetry
                        )
                    }
                    themes.isEmpty() -> {
                        FavouriteRecentEmptyBody(
                            showEmptyState = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                        FavouriteRecentGrid(
                            themes = themes,
                            onOpenTheme = onOpenTheme,
                            onToggleFavorite = onToggleFavorite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (showNativeAd) {
            NativeAdInternal(
                screenCode = SCREEN_FAVOURITE_RECENT,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavouriteRecentLargeTopBar(
    onBack: () -> Unit,
    onPremium: () -> Unit,
    collapsedFraction: Float,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    val expandedSize = dimensionResource(SspR.dimen._18ssp).value.sp
    val collapsedSize = dimensionResource(SspR.dimen._15ssp).value.sp
    val expandedLineHeight = dimensionResource(SspR.dimen._25ssp).value.sp
    val collapsedLineHeight = dimensionResource(SspR.dimen._22ssp).value.sp
    LargeTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.favourite_recent_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = FavouriteRecentRobotoSemiBold,
                fontSize = (
                    expandedSize.value +
                        (collapsedSize.value - expandedSize.value) * collapsedFraction
                    ).sp,
                lineHeight = (
                    expandedLineHeight.value +
                        (collapsedLineHeight.value - expandedLineHeight.value) * collapsedFraction
                    ).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = { FavouriteRecentBackButton(onClick = onBack) },
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
private fun FavouriteRecentBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = dimensionResource(SdpR.dimen._6sdp))
            .size(dimensionResource(SdpR.dimen._32sdp))
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_favorite_recent_back),
            contentDescription = stringResource(R.string.favourite_recent_back),
            tint = Color.Unspecified,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
        )
    }
}

@Composable
private fun FavouriteRecentTabs(
    selectedTab: FavouriteRecentTab,
    onSelectTab: (FavouriteRecentTab) -> Unit
) {
    val outerShape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
            .height(dimensionResource(SdpR.dimen._37sdp))
            .shadow(
                elevation = dimensionResource(SdpR.dimen._9sdp),
                shape = outerShape,
                clip = false,
                ambientColor = colorResource(R.color.gray_666666).copy(alpha = 0.12f),
                spotColor = colorResource(R.color.gray_666666).copy(alpha = 0.12f)
            )
            .clip(outerShape)
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(dimensionResource(SdpR.dimen._3sdp))
    ) {
        FavouriteRecentTabItem(
            tab = FavouriteRecentTab.FAVOURITE,
            selected = selectedTab == FavouriteRecentTab.FAVOURITE,
            iconRes = R.drawable.ic_favorite_recent_heart,
            textRes = R.string.favourite_recent_favourite_tab,
            onClick = onSelectTab,
            modifier = Modifier.weight(1f)
        )
        FavouriteRecentTabItem(
            tab = FavouriteRecentTab.RECENT,
            selected = selectedTab == FavouriteRecentTab.RECENT,
            iconRes = R.drawable.ic_favorite_recent_history,
            textRes = R.string.favourite_recent_recent_tab,
            onClick = onSelectTab,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FavouriteRecentTabItem(
    tab: FavouriteRecentTab,
    selected: Boolean,
    iconRes: Int,
    textRes: Int,
    onClick: (FavouriteRecentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = colorResource(
        if (selected) R.color.colors_FB3675 else R.color.colors_6F7073
    )
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(
                if (selected) colorResource(R.color.colors_FFEBF1) else Color.Transparent
            )
            .clickable { onClick(tab) },
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(SdpR.dimen._6sdp),
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
        Text(
            text = stringResource(textRes),
            color = contentColor,
            fontFamily = FavouriteRecentRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
    }
}

@Composable
private fun FavouriteRecentEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.img_favorite_recent_empty),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._95sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Text(
            text = stringResource(R.string.favourite_recent_empty_title),
            color = colorResource(R.color.colors_FB3675),
            fontFamily = FavouriteRecentRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._2sdp)))
        Text(
            text = stringResource(R.string.favourite_recent_empty_subtitle),
            color = colorResource(R.color.colors_6F7073),
            fontFamily = FavouriteRecentRobotoRegular,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
        )
    }
}

@Composable
private fun FavouriteRecentEmptyBody(
    showEmptyState: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._177sdp))
    ) {
        item {
            if (showEmptyState) {
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._37sdp)))
                FavouriteRecentEmptyState()
            } else {
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._1sdp)))
            }
        }
    }
}

@Composable
private fun FavouriteRecentGrid(
    themes: List<FavouriteRecentThemeUiState>,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._177sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        items(items = themes, key = FavouriteRecentThemeUiState::id) { theme ->
            FavouriteRecentThemeCard(
                theme = theme,
                onOpen = { onOpenTheme(theme.id) },
                onToggleFavorite = { onToggleFavorite(theme.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FavouriteRecentThemeCard(
    theme: FavouriteRecentThemeUiState,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = colorResource(R.color.colors_DEDEDF),
                shape = shape
            )
            .clickable(onClick = onOpen)
    ) {
        AsyncImage(
            model = theme.thumbnailPath ?: R.drawable.ic_home_battery,
            contentDescription = theme.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(90f / 104f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = dimensionResource(SdpR.dimen._5sdp),
                    end = dimensionResource(SdpR.dimen._5sdp)
                )
                .size(dimensionResource(SdpR.dimen._18sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.1f))
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_recent_heart),
                contentDescription = stringResource(R.string.battery_favorite_action),
                tint = if (theme.isFavorite) {
                    colorResource(R.color.colors_FB3675)
                } else {
                    colorResource(R.color.colors_FFFFFF)
                },
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
        }
    }
}
