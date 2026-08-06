package com.asianmobile.emojibattery.shimeji.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val SearchRobotoRegular = FontFamily.Default
private val SearchRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val SearchRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

@Composable
fun SearchScreen(
    onCancel: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrackScreenView(ScreenName.SEARCH)
    SearchContent(
        uiState = uiState,
        onQueryChanged = viewModel::updateQuery,
        onCancel = onCancel,
        onOpenTheme = onOpenTheme,
        onToggleFavorite = viewModel::toggleFavorite
    )
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
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
            SearchHeader(
                query = uiState.query,
                onQueryChanged = onQueryChanged,
                onCancel = onCancel
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = dimensionResource(SdpR.dimen._177sdp)
                )
            ) {
                item {
                    MostSearchedSection(onSearch = onQueryChanged)
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
                }
                item {
                    RecommendedSection(
                        uiState = uiState,
                        onOpenTheme = onOpenTheme,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }

        NativeAdInternal(
            screenCode = SCREEN_HOME,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._51sdp))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._9sdp)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(dimensionResource(SdpR.dimen._32sdp))
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._6sdp),
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
                .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_search),
                contentDescription = null,
                tint = colorResource(R.color.colors_212327),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = colorResource(R.color.colors_212327),
                    fontFamily = SearchRobotoRegular,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
                ),
                cursorBrush = SolidColor(colorResource(R.color.colors_FB3675)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_battery_hint),
                                color = colorResource(R.color.colors_C8C8C9),
                                fontFamily = SearchRobotoRegular,
                                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = stringResource(R.string.search_cancel),
            color = colorResource(R.color.colors_212327),
            fontFamily = SearchRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._4sdp)))
                .clickable(onClick = onCancel)
                .padding(vertical = dimensionResource(SdpR.dimen._4sdp))
        )
    }
}

@Composable
private fun MostSearchedSection(onSearch: (String) -> Unit) {
    val tags = listOf(
        stringResource(R.string.search_tag_anime),
        stringResource(R.string.search_tag_sanrio),
        stringResource(R.string.search_tag_kpop),
        stringResource(R.string.search_tag_cute),
        stringResource(R.string.search_tag_cute),
        stringResource(R.string.search_tag_animal)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._6sdp)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        SearchSectionTitle(text = stringResource(R.string.search_most_searched))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            tags.forEach { tag ->
                SearchTag(label = tag, onClick = { onSearch(tag) })
            }
        }
    }
}

@Composable
private fun SearchTag(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = colorResource(R.color.colors_212327),
        fontFamily = SearchRobotoRegular,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
        modifier = Modifier
            .height(dimensionResource(SdpR.dimen._25sdp))
            .shadow(
                elevation = dimensionResource(SdpR.dimen._6sdp),
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(colorResource(R.color.colors_FFFFFF))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._4sdp)
            )
    )
}

@Composable
private fun RecommendedSection(
    uiState: SearchUiState,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._6sdp)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        SearchSectionTitle(text = stringResource(R.string.search_recommended))
        Image(
            painter = painterResource(R.drawable.img_home_promo_banner),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._38sdp))
        )
        when {
            uiState.isLoading && uiState.recommendedThemes.isEmpty() -> {
                SearchMessage {
                    CircularProgressIndicator(
                        color = colorResource(R.color.colors_FB3675),
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._24sdp))
                    )
                }
            }
            uiState.hasError -> {
                SearchMessage {
                    Text(
                        text = stringResource(R.string.search_load_error),
                        color = colorResource(R.color.colors_212327),
                        fontFamily = SearchRobotoRegular,
                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
                    )
                }
            }
            uiState.recommendedThemes.isEmpty() -> {
                SearchMessage {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        color = colorResource(R.color.colors_212327),
                        fontFamily = SearchRobotoRegular,
                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
                    )
                }
            }
            else -> {
                uiState.recommendedThemes.chunked(SEARCH_COLUMN_COUNT).forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
                    ) {
                        rowThemes.forEach { theme ->
                            SearchThemeCard(
                                theme = theme,
                                onOpen = { onOpenTheme(theme.id) },
                                onFavorite = { onToggleFavorite(theme.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(SEARCH_COLUMN_COUNT - rowThemes.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(text: String) {
    Text(
        text = text,
        color = colorResource(R.color.colors_212327),
        fontFamily = SearchRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SearchThemeCard(
    theme: SearchThemeUiState,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_DEDEDF),
                shape
            )
            .clickable(onClick = onOpen)
    ) {
        AsyncImage(
            model = theme.thumbnailPath ?: R.drawable.ic_home_battery,
            contentDescription = theme.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.95f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(SdpR.dimen._6sdp))
                .size(dimensionResource(SdpR.dimen._18sdp))
                .clip(CircleShape)
                .background(
                    colorResource(
                        if (theme.isFavorite) R.color.colors_FFEBF1 else R.color.colors_F0F0F0
                    )
                )
                .clickable(onClick = onFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (theme.isFavorite) {
                        R.drawable.ic_favorite_filled
                    } else {
                        R.drawable.ic_favorite_outline
                    }
                ),
                contentDescription = stringResource(R.string.discover_favorite_theme),
                tint = colorResource(
                    if (theme.isFavorite) R.color.colors_FB3675 else R.color.colors_C8C8C9
                ),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
        }
    }
}

@Composable
private fun SearchMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._78sdp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SearchContentPreview() {
    SearchContent(
        uiState = SearchUiState(
            isLoading = false,
            recommendedThemes = List(6) { index ->
                SearchThemeUiState(
                    id = index,
                    name = "Theme ${index + 1}",
                    category = "Cute",
                    thumbnailPath = null,
                    isFavorite = index == 0
                )
            }
        ),
        onQueryChanged = {},
        onCancel = {},
        onOpenTheme = {},
        onToggleFavorite = {}
    )
}

private const val SEARCH_COLUMN_COUNT = 3
