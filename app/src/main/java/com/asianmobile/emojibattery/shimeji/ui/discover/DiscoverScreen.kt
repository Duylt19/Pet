package com.asianmobile.emojibattery.shimeji.ui.discover

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val DiscoverRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val DiscoverRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

@Composable
fun DiscoverScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToMyPet: () -> Unit,
    onNavigateToPetStore: () -> Unit,
    onNavigateToMine: () -> Unit,
    onOpenPet: (String) -> Unit,
    onOpenBatteryTheme: (Int) -> Unit,
    onCustomizeStatusBar: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshAccessibility()
    }

    TrackScreenView(ScreenName.HOME)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DiscoverEffect.RequestBatteryAccessibility -> {
                    showAccessibilityDisclosure = true
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccessibility()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DiscoverContent(
        uiState = uiState,
        onSearch = onNavigateToSearch,
        onPremium = onNavigateToPremium,
        onBatteryToggle = viewModel::onBatteryToggle,
        onBattery = onNavigateToBattery,
        onMyPet = onNavigateToMyPet,
        onPetStore = onNavigateToPetStore,
        onMine = onNavigateToMine,
        onOpenPet = onOpenPet,
        onOpenTheme = onOpenBatteryTheme,
        onToggleFavorite = viewModel::toggleFavorite,
        onCustomizeStatusBar = onCustomizeStatusBar
    )

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = {
                showAccessibilityDisclosure = false
                viewModel.cancelPendingBatteryEnable()
            },
            title = { Text(stringResource(R.string.battery_accessibility_title)) },
            text = { Text(stringResource(R.string.battery_accessibility_disclosure)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDisclosure = false
                        accessibilityLauncher.launch(BatteryAccessibility.settingsIntent())
                    }
                ) {
                    Text(stringResource(R.string.battery_open_accessibility_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDisclosure = false
                        viewModel.cancelPendingBatteryEnable()
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DiscoverContent(
    uiState: DiscoverUiState,
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onBatteryToggle: () -> Unit,
    onBattery: () -> Unit,
    onMyPet: () -> Unit,
    onPetStore: () -> Unit,
    onMine: () -> Unit,
    onOpenPet: (String) -> Unit,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onCustomizeStatusBar: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._600sdp))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = dimensionResource(SdpR.dimen._24sdp)
                    )
                ) {
                    item { DiscoverHeader(onSearch = onSearch, onPremium = onPremium) }
                    item {
                        BatteryEnableCard(
                            enabled = uiState.isBatteryEnabled,
                            onToggle = onBatteryToggle
                        )
                    }
                    item {
                        QuickActions(
                            onBattery = onBattery,
                            onMyPet = onMyPet,
                            onCustomizeStatusBar = onCustomizeStatusBar
                        )
                    }
                    item { HeroBanner() }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
                        TrendingPetsSection(
                            pets = uiState.trendingPets,
                            isLoading = uiState.isLoading,
                            onMore = onPetStore,
                            onOpenPet = onOpenPet
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        BatteryThemesSection(
                            themes = uiState.batteryThemes,
                            onMore = onBattery,
                            onOpenTheme = onOpenTheme,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        Image(
                            painter = painterResource(R.drawable.img_home_promo_banner),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensionResource(SdpR.dimen._38sdp))
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        StatusBarThemesSection(
                            assets = uiState.statusBarThemes,
                            onMore = onCustomizeStatusBar,
                            onOpen = onCustomizeStatusBar
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        ComponentAssetsSection(
                            title = stringResource(R.string.discover_emoji_title),
                            assets = uiState.emojiThemes,
                            fallbackRes = R.drawable.img_home_brand_bunny,
                            onMore = onBattery,
                            onOpen = onBattery
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        ComponentAssetsSection(
                            title = stringResource(R.string.discover_battery_title),
                            assets = uiState.batteryIcons,
                            fallbackRes = R.drawable.img_home_battery_heading,
                            onMore = onBattery,
                            onOpen = onBattery
                        )
                    }
                }
                Image(
                    painter = painterResource(R.drawable.img_home_diy),
                    contentDescription = stringResource(R.string.discover_customize_status_bar),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = dimensionResource(SdpR.dimen._11sdp),
                            bottom = dimensionResource(SdpR.dimen._8sdp)
                        )
                        .size(dimensionResource(SdpR.dimen._62sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._15sdp)))
                        .clickable(onClick = onCustomizeStatusBar)
                )
            }
            DiscoverBottomNavigation(
                onBattery = onBattery,
                onPetStore = onPetStore,
                onMine = onMine
            )
            BannerAd(
                modifier = Modifier.fillMaxWidth(),
                adPosition = HOME_BOTTOM_BANNER_POSITION
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DiscoverHeader(onSearch: () -> Unit, onPremium: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.discover_brand_name),
            color = colorResource(R.color.colors_212327),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            fontSize = dimensionResource(SspR.dimen._17ssp).value.sp,
            maxLines = 1
        )
        Image(
            painter = painterResource(R.drawable.img_home_brand_bunny),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._25sdp))
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._25sdp))
                .shadow(dimensionResource(SdpR.dimen._6sdp), CircleShape)
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_home_search),
                contentDescription = stringResource(R.string.discover_search),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
            )
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
        Row(
            modifier = Modifier
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._6sdp),
                    shape = CircleShape
                )
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
                .clickable(onClick = onPremium)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._6sdp),
                    vertical = dimensionResource(SdpR.dimen._5sdp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Image(
                painter = painterResource(R.drawable.img_home_crown),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
            )
            Text(
                text = stringResource(R.string.discover_pro),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = DiscoverRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
    }
}

@Composable
private fun BatteryEnableCard(enabled: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    val pink = colorResource(R.color.colors_FB3675)
    val violet = colorResource(R.color.colors_C95DFF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._9sdp)
            )
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(pink.copy(alpha = 0.12f), violet.copy(alpha = 0.12f))
                )
            )
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                brush = Brush.horizontalGradient(listOf(violet, pink)),
                shape = shape
            )
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                if (enabled) {
                    R.string.discover_battery_enabled
                } else {
                    R.string.discover_battery_enable_prompt
                }
            ),
            color = colorResource(R.color.colors_212327),
            fontFamily = DiscoverRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        DiscoverSwitch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun DiscoverSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) {
            dimensionResource(SdpR.dimen._17sdp)
        } else {
            dimensionResource(SdpR.dimen._2sdp)
        },
        label = "discoverSwitch"
    )
    val knobYOffset = dimensionResource(SdpR.dimen._2sdp)
    Box(
        modifier = Modifier
            .size(
                width = dimensionResource(SdpR.dimen._34sdp),
                height = dimensionResource(SdpR.dimen._18sdp)
            )
            .clip(CircleShape)
            .background(
                colorResource(
                    if (checked) R.color.colors_F1E0FF else R.color.colors_C8C8C9
                )
            )
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange
            )
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(knobOffset.roundToPx(), knobYOffset.roundToPx())
                }
                .size(dimensionResource(SdpR.dimen._15sdp))
                .clip(CircleShape)
                .background(
                    colorResource(
                        if (checked) R.color.colors_B06EFF else R.color.colors_FFFFFF
                    )
                )
        )
    }
}

@Composable
private fun QuickActions(
    onBattery: () -> Unit,
    onMyPet: () -> Unit,
    onCustomizeStatusBar: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(
            horizontal = dimensionResource(SdpR.dimen._12sdp),
            vertical = dimensionResource(SdpR.dimen._6sdp)
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        item {
            QuickActionCard(
                title = stringResource(R.string.discover_quick_battery),
                imageRes = R.drawable.img_home_quick_battery,
                onClick = onBattery
            )
        }
        item {
            QuickActionCard(
                title = stringResource(R.string.discover_quick_my_pet),
                imageRes = R.drawable.img_home_quick_pet,
                onClick = onMyPet
            )
        }
        item {
            QuickActionCard(
                title = stringResource(R.string.discover_customize_status_bar),
                imageRes = R.drawable.img_home_quick_customize,
                onClick = onCustomizeStatusBar
            )
        }
    }
}

@Composable
private fun QuickActionCard(title: String, imageRes: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .size(
                width = dimensionResource(SdpR.dimen._100sdp),
                height = dimensionResource(SdpR.dimen._42sdp)
            )
            .shadow(dimensionResource(SdpR.dimen._6sdp), shape)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = DiscoverRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(dimensionResource(SdpR.dimen._51sdp))
                .padding(start = dimensionResource(SdpR.dimen._9sdp))
        )
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = -dimensionResource(SdpR.dimen._2sdp))
                .size(dimensionResource(SdpR.dimen._38sdp))
        )
    }
}

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._9sdp)
            )
            .height(dimensionResource(SdpR.dimen._77sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .background(colorResource(R.color.colors_FFEBF1)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.discover_banner_placeholder),
            color = colorResource(R.color.colors_000000),
            fontFamily = DiscoverRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
        )
    }
}

@Composable
private fun TrendingPetsSection(
    pets: List<DiscoverPetUiState>,
    isLoading: Boolean,
    onMore: () -> Unit,
    onOpenPet: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        SectionHeader(
            title = stringResource(R.string.discover_trending_pets),
            onMore = onMore,
            underline = true
        )
        if (isLoading && pets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._118sdp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorResource(R.color.colors_FB3675),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._24sdp))
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
            ) {
                items(pets, key = DiscoverPetUiState::packKey) { pet ->
                    TrendingPetCard(pet = pet, onClick = { onOpenPet(pet.packKey) })
                }
            }
        }
    }
}

@Composable
private fun TrendingPetCard(pet: DiscoverPetUiState, onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = Modifier
            .size(
                width = dimensionResource(SdpR.dimen._92sdp),
                height = dimensionResource(SdpR.dimen._118sdp)
            )
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_DEDEDF), shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._77sdp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = pet.thumbnailPath ?: R.drawable.img_home_brand_bunny,
                contentDescription = pet.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._62sdp))
            )
        }
        Text(
            text = pet.name,
            color = colorResource(R.color.colors_212327),
            fontFamily = DiscoverRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimensionResource(SdpR.dimen._4sdp))
        )
        Text(
            text = stringResource(R.string.discover_pet_category, pet.category),
            color = colorResource(R.color.colors_FDA3C0),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BatteryThemesSection(
    themes: List<DiscoverThemeUiState>,
    onMore: () -> Unit,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        SectionHeader(
            title = stringResource(R.string.discover_battery_themes),
            leadingIconRes = R.drawable.img_home_battery_heading,
            onMore = onMore
        )
        val slots = List(BATTERY_THEME_SLOT_COUNT) { index -> themes.getOrNull(index) }
        slots.chunked(BATTERY_THEME_COLUMN_COUNT).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
            Row(
                modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
            ) {
                row.forEach { theme ->
                    BatteryThemeCard(
                        theme = theme,
                        onOpen = { theme?.let { onOpenTheme(it.id) } },
                        onFavorite = { theme?.let { onToggleFavorite(it.id) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryThemeCard(
    theme: DiscoverThemeUiState?,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._85sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_DEDEDF), shape)
            .clickable(enabled = theme != null, onClick = onOpen)
    ) {
        AsyncImage(
            model = theme?.thumbnailPath ?: R.drawable.img_home_battery_heading,
            contentDescription = theme?.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.86f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(SdpR.dimen._5sdp))
                .size(dimensionResource(SdpR.dimen._18sdp))
                .clip(CircleShape)
                .background(
                    colorResource(
                        if (theme?.isFavorite == true) {
                            R.color.colors_FFEBF1
                        } else {
                            R.color.colors_F0F0F0
                        }
                    )
                )
                .clickable(enabled = theme != null, onClick = onFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (theme?.isFavorite == true) {
                        R.drawable.ic_favorite_filled
                    } else {
                        R.drawable.ic_favorite_outline
                    }
                ),
                contentDescription = stringResource(R.string.discover_favorite_theme),
                tint = colorResource(
                    if (theme?.isFavorite == true) {
                        R.color.colors_FB3675
                    } else {
                        R.color.colors_C8C8C9
                    }
                ),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
        }
    }
}

@Composable
private fun StatusBarThemesSection(
    assets: List<DiscoverAssetUiState>,
    onMore: () -> Unit,
    onOpen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        SectionHeader(
            title = stringResource(R.string.discover_status_bar_themes),
            onMore = onMore
        )
        val columns = if (assets.isEmpty()) {
            List(4) { null }.chunked(2)
        } else {
            assets.map { it as DiscoverAssetUiState? }.chunked(2)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            items(columns) { column ->
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
                    column.forEach { asset ->
                        StatusBarThemeCard(asset = asset, onClick = onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBarThemeCard(asset: DiscoverAssetUiState?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .size(
                width = dimensionResource(SdpR.dimen._177sdp),
                height = dimensionResource(SdpR.dimen._69sdp)
            )
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_DEDEDF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (asset == null) {
            Box(
                modifier = Modifier
                    .size(
                        width = dimensionResource(SdpR.dimen._155sdp),
                        height = dimensionResource(SdpR.dimen._20sdp)
                    )
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                    .background(colorResource(R.color.colors_FFEBF1))
            )
        } else {
            AsyncImage(
                model = asset.assetPath,
                contentDescription = asset.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._28sdp))
                    .padding(horizontal = dimensionResource(SdpR.dimen._10sdp))
            )
        }
    }
}

@Composable
private fun ComponentAssetsSection(
    title: String,
    assets: List<DiscoverAssetUiState>,
    fallbackRes: Int,
    onMore: () -> Unit,
    onOpen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        SectionHeader(title = title, onMore = onMore)
        val display = if (assets.isEmpty()) {
            List(8) { null }
        } else {
            assets.map { it as DiscoverAssetUiState? }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            items(display.chunked(2)) { column ->
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
                    column.forEach { asset ->
                        ComponentAssetCard(asset = asset, fallbackRes = fallbackRes, onClick = onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentAssetCard(
    asset: DiscoverAssetUiState?,
    fallbackRes: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._69sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_DEDEDF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = asset?.assetPath ?: fallbackRes,
            contentDescription = asset?.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._54sdp))
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onMore: () -> Unit,
    leadingIconRes: Int? = null,
    underline: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                dimensionResource(
                    if (underline) SdpR.dimen._25sdp else SdpR.dimen._18sdp
                )
            )
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.Top
    ) {
        if (leadingIconRes != null) {
            Image(
                painter = painterResource(leadingIconRes),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
        }
        Box {
            Text(
                text = title,
                color = colorResource(R.color.colors_212327),
                fontFamily = DiscoverRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
            )
            if (underline) {
                Image(
                    painter = painterResource(R.drawable.img_home_trending_underline),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .padding(start = dimensionResource(SdpR.dimen._15sdp))
                        .offset(y = dimensionResource(SdpR.dimen._18sdp))
                        .size(
                            width = dimensionResource(SdpR.dimen._75sdp),
                            height = dimensionResource(SdpR.dimen._9sdp)
                        )
                )
            }
        }
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
private fun DiscoverBottomNavigation(
    onBattery: () -> Unit,
    onPetStore: () -> Unit,
    onMine: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimensionResource(SdpR.dimen._6sdp))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(horizontal = dimensionResource(SdpR.dimen._6sdp))
    ) {
        DiscoverBottomItem(
            iconRes = R.drawable.ic_home_discover,
            label = stringResource(R.string.discover_tab_discover),
            selected = true,
            onClick = {},
            modifier = Modifier.weight(1f)
        )
        DiscoverBottomItem(
            iconRes = R.drawable.ic_home_battery,
            label = stringResource(R.string.discover_tab_battery),
            selected = false,
            onClick = onBattery,
            modifier = Modifier.weight(1f)
        )
        DiscoverBottomItem(
            iconRes = R.drawable.ic_home_pet_store,
            label = stringResource(R.string.discover_tab_pet_store),
            selected = false,
            onClick = onPetStore,
            modifier = Modifier.weight(1f)
        )
        DiscoverBottomItem(
            iconRes = R.drawable.ic_home_mine,
            label = stringResource(R.string.discover_tab_mine),
            selected = false,
            onClick = onMine,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DiscoverBottomItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._62sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .clickable(onClick = onClick)
            .padding(
                top = dimensionResource(SdpR.dimen._9sdp),
                bottom = dimensionResource(SdpR.dimen._12sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (selected) {
                        colorResource(R.color.colors_FFEBF1)
                    } else {
                        colorResource(R.color.colors_FFFFFF)
                    }
                )
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._15sdp),
                    vertical = dimensionResource(SdpR.dimen._3sdp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
        Text(
            text = label,
            color = colorResource(
                if (selected) R.color.colors_FB3675 else R.color.colors_6F7073
            ),
            fontFamily = if (selected) DiscoverRobotoSemiBold else DiscoverRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DiscoverContentPreview() {
    DiscoverContent(
        uiState = DiscoverUiState(
            isLoading = false,
            trendingPets = listOf(
                DiscoverPetUiState("cat", "Cattey", "Cat", null),
                DiscoverPetUiState("bunny", "Bunny", "Rabbit", null)
            ),
            batteryThemes = List(6) { index ->
                DiscoverThemeUiState(index, "Theme $index", null, index == 0)
            }
        ),
        onSearch = {},
        onPremium = {},
        onBatteryToggle = {},
        onBattery = {},
        onMyPet = {},
        onPetStore = {},
        onMine = {},
        onOpenPet = {},
        onOpenTheme = {},
        onToggleFavorite = {},
        onCustomizeStatusBar = {}
    )
}

private const val HOME_BOTTOM_BANNER_POSITION = "home_mode_bottom"
private const val BATTERY_THEME_SLOT_COUNT = 6
private const val BATTERY_THEME_COLUMN_COUNT = 3
