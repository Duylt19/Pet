package com.asianmobile.emojibattery.shimeji.ui.home.discover

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.BANNER_DISCOVER_INLINE
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibilityRecovery
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogFlowHost
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogUiState
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryCatalogViewModel
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryThemeAccess
import com.asianmobile.emojibattery.shimeji.ui.battery.catalog.BatteryThemeAccessPolicy
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreFlowHost
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreUiState
import com.asianmobile.emojibattery.shimeji.ui.pet.store.PetStoreViewModel
import com.asianmobile.emojibattery.shimeji.ui.shared.component.GrantPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.shared.component.CATALOG_ITEM_PREVIEW_FRACTION
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeEnableCard
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeHeader
import com.asianmobile.emojibattery.shimeji.ui.shared.component.PetPremiumBadge
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val DiscoverRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val DiscoverRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))

@Composable
fun DiscoverScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToBatteryTroll: () -> Unit,
    onNavigateToPetStore: () -> Unit,
    onNavigateToMyPet: () -> Unit,
    onNavigateToGrantPermissions: () -> Unit,
    onOpenBatteryTheme: (Int) -> Unit,
    onCustomizeStatusBar: () -> Unit,
    accessibilityHowToUseResult: Boolean? = null,
    onAccessibilityHowToUseResultConsumed: () -> Unit = {},
    onNavigateToAccessibilityHowToUse: () -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel(),
    batteryCatalogViewModel: BatteryCatalogViewModel = hiltViewModel(),
    petStoreViewModel: PetStoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val batteryCatalogState by batteryCatalogViewModel.uiState.collectAsStateWithLifecycle()
    val petStoreState by petStoreViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    TrackScreenView(ScreenName.DISCOVER)

    LaunchedEffect(accessibilityHowToUseResult) {
        accessibilityHowToUseResult?.let { permissionGranted ->
            if (permissionGranted) {
                viewModel.refreshAccessibility()
            } else {
                viewModel.cancelPendingBatteryEnable()
            }
            onAccessibilityHowToUseResultConsumed()
        }
    }

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
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibility()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val presentedState = remember(uiState, batteryCatalogState, petStoreState) {
        discoverPresentationState(uiState, batteryCatalogState, petStoreState)
    }
    val requestTheme: (Int) -> Unit = { themeId ->
        batteryCatalogState.themes.firstOrNull { it.id == themeId }
            ?.let(batteryCatalogViewModel::requestTheme)
    }

    BatteryCatalogFlowHost(
        state = batteryCatalogState,
        viewModel = batteryCatalogViewModel,
        onOpenTheme = onOpenBatteryTheme,
        onNavigateToPremium = onNavigateToPremium
    ) {
        PetStoreFlowHost(
            state = petStoreState,
            viewModel = petStoreViewModel,
            onPremium = onNavigateToPremium,
            onViewPet = onNavigateToMyPet,
            onNavigateToGrantPermissions = onNavigateToGrantPermissions
        ) {
            DiscoverContent(
                uiState = presentedState,
                onSearch = onNavigateToSearch,
                onPremium = onNavigateToPremium,
                onBatteryToggle = viewModel::onBatteryToggle,
                onBattery = onNavigateToBattery,
                onBatteryTroll = onNavigateToBatteryTroll,
                onPetStore = onNavigateToPetStore,
                onOpenPet = { packKey ->
                    petStoreState.pets.firstOrNull { it.installedPackKey == packKey }
                        ?.let(petStoreViewModel::selectPet)
                },
                onOpenTheme = requestTheme,
                onToggleFavorite = viewModel::toggleFavorite,
                onCustomizeStatusBar = onCustomizeStatusBar,
                onDismissRecovery = viewModel::dismissAccessibilityRecovery
            )
        }
    }

    if (showAccessibilityDisclosure) {
        GrantPermissionDialog(
            onGrantPermission = {
                showAccessibilityDisclosure = false
                onNavigateToAccessibilityHowToUse()
            },
            onMaybeLater = {
                showAccessibilityDisclosure = false
                viewModel.cancelPendingBatteryEnable()
            }
        )
    }
}

internal fun discoverPresentationState(
    state: DiscoverUiState,
    batteryState: BatteryCatalogUiState,
    petState: PetStoreUiState
): DiscoverUiState {
    val accessPolicy = BatteryThemeAccessPolicy()
    val themeById = batteryState.themes.associateBy { it.id }
    fun isThemeLocked(themeId: Int): Boolean = themeById[themeId]?.let { theme ->
        accessPolicy.resolve(
            theme = theme,
            isPremium = batteryState.isPremium,
            rewardUnlockedThemeIds = batteryState.rewardUnlockedThemeIds
        ) == BatteryThemeAccess.REWARD_OR_PREMIUM
    } == true

    return state.copy(
        trendingPets = state.trendingPets.map { pet ->
            pet.copy(isLocked = pet.packKey !in petState.installedPackKeys)
        },
        batteryThemes = state.batteryThemes.map { theme ->
            theme.copy(isLocked = isThemeLocked(theme.id))
        },
        emojiThemes = state.emojiThemes.map { asset ->
            asset.copy(isLocked = isThemeLocked(asset.id))
        },
        batteryIcons = state.batteryIcons.map { asset ->
            asset.copy(isLocked = isThemeLocked(asset.id))
        }
    )
}

/**
 * Sits above the enable card rather than replacing it: the toggle is still the control, this only
 * explains why it went back to off on its own. Tapping the action runs the ordinary enable path,
 * which already asks for Accessibility and shows the disclosure before any hand-off to Settings.
 */
@Composable
internal fun BatteryAccessibilityRecoveryCard(
    recovery: BatteryAccessibilityRecovery,
    onTurnBackOn: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    val pink = colorResource(R.color.colors_FB3675)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                top = dimensionResource(SdpR.dimen._9sdp),
                end = dimensionResource(SdpR.dimen._12sdp)
            )
            .clip(shape)
            .background(pink.copy(alpha = 0.08f))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = pink.copy(alpha = 0.4f),
                shape = shape
            )
            .padding(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (recovery) {
                        BatteryAccessibilityRecovery.APP_CLOSED ->
                            R.string.battery_accessibility_recovery_app_closed

                        BatteryAccessibilityRecovery.DEVICE_KILLED ->
                            R.string.battery_accessibility_recovery_device_killed

                        // NONE never renders; the caller checks before composing this.
                        else -> R.string.battery_accessibility_recovery_unknown
                    }
                ),
                color = colorResource(R.color.colors_212327),
                fontFamily = DiscoverRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._14ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            Text(
                text = stringResource(R.string.battery_accessibility_recovery_action),
                color = pink,
                fontFamily = DiscoverRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                modifier = Modifier
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onTurnBackOn)
                    .padding(vertical = dimensionResource(SdpR.dimen._2sdp))
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(
                R.string.battery_accessibility_recovery_dismiss
            ),
            tint = colorResource(R.color.colors_6F7073),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._15sdp))
                .clickable(role = Role.Button, onClick = onDismiss)
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
    onBatteryTroll: () -> Unit,
    onPetStore: () -> Unit,
    onOpenPet: (String) -> Unit,
    onOpenTheme: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onCustomizeStatusBar: () -> Unit,
    onDismissRecovery: () -> Unit
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
                    item { HomeHeader(onSearch = onSearch, onPremium = onPremium) }
                    if (uiState.accessibilityRecovery != BatteryAccessibilityRecovery.NONE) {
                        item {
                            BatteryAccessibilityRecoveryCard(
                                recovery = uiState.accessibilityRecovery,
                                onTurnBackOn = onBatteryToggle,
                                onDismiss = onDismissRecovery
                            )
                        }
                    }
                    item {
                        HomeEnableCard(
                            text = stringResource(
                                if (uiState.isBatteryEnabled) {
                                    R.string.discover_battery_enabled
                                } else {
                                    R.string.discover_battery_enable_prompt
                                }
                            ),
                            checked = uiState.isBatteryEnabled,
                            onCheckedChange = onBatteryToggle
                        )
                    }
                    item { DiscoverBatteryTrollBanner(onClick = onBatteryTroll) }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
                        BatteryThemesSection(
                            themes = uiState.batteryThemes,
                            onMore = onBattery,
                            onOpenTheme = onOpenTheme,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        TrendingPetsSection(
                            pets = uiState.trendingPets,
                            isLoading = uiState.isLoading,
                            onMore = onPetStore,
                            onOpenPet = onOpenPet
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            BannerAd(
                                adPosition = BANNER_DISCOVER_INLINE,
                                showContainerShadow = false
                            )
                        }
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
                            titleIcon = R.drawable.img_statusbar_template_emoji,
                            assets = uiState.emojiThemes,
                            fallbackRes = R.drawable.img_home_brand_bunny,
                            onMore = onBattery,
                            onOpen = { asset -> onOpenTheme(asset.id) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
                        ComponentAssetsSection(
                            title = stringResource(R.string.discover_battery_title),
                            titleIcon = R.drawable.ic_statusbar_template_battery,
                            assets = uiState.batteryIcons,
                            fallbackRes = R.drawable.ic_home_battery,
                            onMore = onBattery,
                            onOpen = { asset -> onOpenTheme(asset.id) }
                        )
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
internal fun HomeDiyFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sparkleProgress: Float? = null
) {
    val asyncStarBlingComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.anim_home_star_bling)
    )
    val context = LocalContext.current
    val starBlingComposition = if (sparkleProgress == null) {
        asyncStarBlingComposition
    } else {
        remember(context) {
            LottieCompositionFactory.fromRawResSync(
                context,
                R.raw.anim_home_star_bling
            ).value
        }
    }
    val fabShape = RoundedCornerShape(dimensionResource(SdpR.dimen._15sdp))
    Box(
        modifier = modifier.size(dimensionResource(SdpR.dimen._62sdp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_home_diy),
            contentDescription = stringResource(R.string.discover_customize_status_bar),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._9sdp),
                    shape = fabShape,
                    ambientColor = colorResource(R.color.colors_80000000),
                    spotColor = colorResource(R.color.colors_80000000)
                )
                .clip(fabShape)
                .clickable(onClick = onClick)
        )

        val sparkleModifier = Modifier
            .align(Alignment.Center)
            .requiredSize(
                width = dimensionResource(SdpR.dimen._100sdp),
                height = dimensionResource(SdpR.dimen._45sdp)
            )

        if (sparkleProgress == null) {
            LottieAnimation(
                composition = starBlingComposition,
                iterations = LottieConstants.IterateForever,
                modifier = sparkleModifier
            )
        } else {
            LottieAnimation(
                composition = starBlingComposition,
                progress = { sparkleProgress },
                modifier = sparkleModifier
            )
        }
    }
}

@Composable
internal fun DiscoverBatteryTrollBanner(onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.img_discover_battery_troll_banner),
        contentDescription = stringResource(R.string.discover_battery_troll_banner),
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._9sdp)
            )
            .height(dimensionResource(SdpR.dimen._77sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .clickable(role = Role.Button, onClick = onClick)
    )
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
            title = stringResource(R.string.discover_shimeji_pets),
            onMore = onMore
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
internal fun TrendingPetCard(pet: DiscoverPetUiState, onClick: () -> Unit) {
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
            HomePreviewImage(
                model = pet.thumbnailPath,
                fallbackRes = R.drawable.img_home_brand_bunny,
                contentDescription = pet.name,
                modifier = Modifier.fillMaxSize(CATALOG_ITEM_PREVIEW_FRACTION)
            )
            if (pet.isLocked) {
                PetPremiumBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(dimensionResource(SdpR.dimen._5sdp))
                        .size(dimensionResource(SdpR.dimen._18sdp))
                )
            }
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
            title = stringResource(R.string.discover_trending_battery_themes),
            onMore = onMore,
            underline = true
        )
        val columns = if (themes.isEmpty()) {
            List(BATTERY_THEME_PLACEHOLDER_COUNT) { null }.chunked(PREVIEW_COLUMN_ITEM_COUNT)
        } else {
            themes.map { it as DiscoverThemeUiState? }.chunked(PREVIEW_COLUMN_ITEM_COUNT)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            items(columns) { column ->
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
                    column.forEach { theme ->
                        BatteryThemeCard(
                            theme = theme,
                            onOpen = { theme?.let { onOpenTheme(it.id) } },
                            onFavorite = { theme?.let { onToggleFavorite(it.id) } },
                            modifier = Modifier.size(dimensionResource(SdpR.dimen._85sdp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BatteryThemeCard(
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
        HomePreviewImage(
            model = theme?.thumbnailPath,
            fallbackRes = R.drawable.ic_home_battery,
            contentDescription = theme?.name,
            modifier = if (theme != null) {
                Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(CATALOG_ITEM_PREVIEW_FRACTION)
            } else {
                Modifier
                    .align(Alignment.Center)
                    .size(dimensionResource(SdpR.dimen._24sdp))
            }
        )
        if (theme?.isLocked == true) {
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
            List(4) { null }.chunked(PREVIEW_COLUMN_ITEM_COUNT)
        } else {
            assets.map { it as DiscoverAssetUiState? }.chunked(PREVIEW_COLUMN_ITEM_COUNT)
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
    @DrawableRes titleIcon: Int? = null,
    assets: List<DiscoverAssetUiState>,
    fallbackRes: Int,
    onMore: () -> Unit,
    onOpen: (DiscoverAssetUiState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        SectionHeader(title = title, titleIcon = titleIcon, onMore = onMore)
        val display = if (assets.isEmpty()) {
            List(8) { null }
        } else {
            assets.map { it as DiscoverAssetUiState? }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            items(display.chunked(PREVIEW_COLUMN_ITEM_COUNT)) { column ->
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
                    column.forEach { asset ->
                        ComponentAssetCard(
                            asset = asset,
                            fallbackRes = fallbackRes,
                            onClick = { asset?.let(onOpen) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ComponentAssetCard(
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
        HomePreviewImage(
            model = asset?.assetPath,
            fallbackRes = fallbackRes,
            contentDescription = asset?.name,
            modifier = Modifier.fillMaxSize(CATALOG_ITEM_PREVIEW_FRACTION)
        )
        if (asset?.isLocked == true) {
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
private fun HomePreviewImage(
    model: String?,
    fallbackRes: Int,
    contentDescription: String?,
    modifier: Modifier
) {
    if (model == null) {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier = modifier
        )
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    @DrawableRes titleIcon: Int? = null,
    onMore: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (titleIcon != null) {
                Image(
                    painter = painterResource(titleIcon),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
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
        onBatteryTroll = {},
        onPetStore = {},
        onOpenPet = {},
        onOpenTheme = {},
        onToggleFavorite = {},
        onCustomizeStatusBar = {},
        onDismissRecovery = {}
    )
}

private const val BATTERY_THEME_PLACEHOLDER_COUNT = 6
private const val PREVIEW_COLUMN_ITEM_COUNT = 2
