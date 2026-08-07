package com.asianmobile.emojibattery.shimeji.ui.favoriterecent

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val FavouriteRecentRobotoRegular = FontFamily.Default
private val FavouriteRecentRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val FavouriteRecentRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

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
        onToggleFavorite = viewModel::toggleFavorite
    )
}

@Composable
internal fun FavouriteRecentContent(
    uiState: FavouriteRecentUiState,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onSelectTab: (FavouriteRecentTab) -> Unit,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    showNativeAd: Boolean = true
) {
    val themes = uiState.visibleThemes
    val usesCompactHeader = themes.isNotEmpty()

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (usesCompactHeader) {
                FavouriteRecentCompactHeader(onBack = onBack, onPremium = onPremium)
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
            } else {
                FavouriteRecentEmptyHeader(onBack = onBack, onPremium = onPremium)
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            }

            FavouriteRecentTabs(
                selectedTab = uiState.selectedTab,
                onSelectTab = onSelectTab
            )

            when {
                uiState.isLoading && themes.isEmpty() -> Spacer(Modifier.weight(1f))
                themes.isEmpty() -> {
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._37sdp)))
                    FavouriteRecentEmptyState()
                    Spacer(Modifier.weight(1f))
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

        if (showNativeAd) {
            NativeAdInternal(
                screenCode = SCREEN_HOME,
                adTypeOverride = AdType.HEIGHT_222,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun FavouriteRecentEmptyHeader(onBack: () -> Unit, onPremium: () -> Unit) {
    FavouriteRecentTopActions(onBack = onBack, onPremium = onPremium)
    Text(
        text = stringResource(R.string.favourite_recent_title),
        color = colorResource(R.color.colors_212327),
        fontFamily = FavouriteRecentRobotoSemiBold,
        fontSize = dimensionResource(SspR.dimen._18ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._25ssp).value.sp,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._25sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
    )
}

@Composable
private fun FavouriteRecentCompactHeader(onBack: () -> Unit, onPremium: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FavouriteRecentBackButton(onClick = onBack)
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
        Text(
            text = stringResource(R.string.favourite_recent_title_populated),
            color = colorResource(R.color.colors_212327),
            fontFamily = FavouriteRecentRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        FavouriteRecentPremiumButton(onClick = onPremium)
    }
}

@Composable
private fun FavouriteRecentTopActions(onBack: () -> Unit, onPremium: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FavouriteRecentBackButton(onClick = onBack)
        Spacer(Modifier.weight(1f))
        FavouriteRecentPremiumButton(onClick = onPremium)
    }
}

@Composable
private fun FavouriteRecentBackButton(onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.ic_favorite_recent_back),
        contentDescription = stringResource(R.string.favourite_recent_back),
        tint = Color.Unspecified,
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._22sdp))
            .clip(CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FavouriteRecentPremiumButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(dimensionResource(SdpR.dimen._6sdp), CircleShape)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colorResource(R.color.colors_FFB65B),
                        colorResource(R.color.colors_FF6B80),
                        colorResource(R.color.colors_FF57EE)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._6sdp),
                vertical = dimensionResource(SdpR.dimen._5sdp)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.img_home_crown),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
        Text(
            text = stringResource(R.string.discover_pro),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FavouriteRecentRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
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
