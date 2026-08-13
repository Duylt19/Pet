package com.asianmobile.emojibattery.shimeji.ui.battery.editor

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryAnimationType
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntitlement
import com.asianmobile.emojibattery.shimeji.data.model.BatteryThemeEntry
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomePremiumButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.PetPremiumBadge
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val StatusBarRobotoRegular = FontFamily(Font(R.font.roboto_regular))
private val StatusBarRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val StatusBarRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))
private val StatusBarColorWheelBrush = Brush.sweepGradient(
    (0 until 360 step 15).map { hue -> Color.hsv(hue.toFloat(), 0.7f, 0.92f) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BatteryEditorFigmaContent(
    state: BatteryEditorUiState,
    page: BatteryEditorPage,
    onBack: () -> Unit,
    onOpenPage: (BatteryEditorPage) -> Unit,
    onPremium: () -> Unit,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onBackgroundDecoration: (Int) -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit,
    onApply: () -> Unit,
    showEmbeddedPreview: Boolean = true
) {
    val isOverview = page == BatteryEditorPage.OVERVIEW
    val scrollBehavior = if (isOverview) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        null
    }
    Box(Modifier.fillMaxSize()) {
        StatusBarEditorWallpaper()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (scrollBehavior != null) {
                        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        Modifier
                    }
                ),
            containerColor = Color.Transparent,
            topBar = {
                if (isOverview) {
                    EditorLargeTopBar(
                        onBack = onBack,
                        onPremium = onPremium,
                        collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f,
                        scrollBehavior = requireNotNull(scrollBehavior)
                    )
                } else {
                    EditorCompactTopBar(
                        title = pickerTitle(page),
                        onBack = onBack,
                        onPremium = onPremium
                    )
                }
            },
            bottomBar = {
                if (isOverview) {
                    StatusBarApplyPanel(
                        enabled = state.isThemeAvailable &&
                            state.assetSelectionInProgress == null &&
                            state.backgroundSelectionInProgress == null,
                        onApply = onApply
                    )
                }
            }
        ) { innerPadding ->
            if (isOverview) {
                StatusBarOverview(
                    state = state,
                    innerPadding = innerPadding,
                    onOpenPage = onOpenPage,
                    onSelectTheme = onSelectTheme,
                    onBackgroundColor = onBackgroundColor,
                    onBackgroundDecoration = onBackgroundDecoration,
                    onConfig = onConfig,
                    showEmbeddedPreview = showEmbeddedPreview
                )
            } else {
                StatusBarPicker(
                    state = state,
                    page = page,
                    innerPadding = innerPadding,
                    onPremium = onPremium,
                    onSelectTheme = onSelectTheme,
                    onBackgroundDecoration = onBackgroundDecoration,
                    showEmbeddedPreview = showEmbeddedPreview
                )
            }
        }
    }
}

@Composable
internal fun StatusBarEditorWallpaper() {
    Image(
        painter = painterResource(R.drawable.img_home_wallpaper),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorLargeTopBar(
    onBack: () -> Unit,
    onPremium: () -> Unit,
    collapsedFraction: Float,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    val expandedSize = dimensionResource(SspR.dimen._18ssp).value.sp
    val collapsedSize = dimensionResource(SspR.dimen._15ssp).value.sp
    val titleSize = (
        expandedSize.value + (collapsedSize.value - expandedSize.value) * collapsedFraction
        ).sp
    LargeTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.battery_editor_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = StatusBarRobotoSemiBold,
                fontSize = titleSize,
                lineHeight = (
                    dimensionResource(SspR.dimen._24ssp).value +
                        (dimensionResource(SspR.dimen._22ssp).value -
                            dimensionResource(SspR.dimen._24ssp).value) * collapsedFraction
                    ).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = { EditorBackButton(onBack) },
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
private fun EditorCompactTopBar(
    title: String,
    onBack: () -> Unit,
    onPremium: () -> Unit
) {
    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._43sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorBackButton(onBack)
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
            Text(
                text = title,
                color = colorResource(R.color.colors_212327),
                fontFamily = StatusBarRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp
            )
            Spacer(Modifier.weight(1f))
            HomePremiumButton(
                onClick = onPremium,
                modifier = Modifier.padding(end = dimensionResource(SdpR.dimen._12sdp))
            )
        }
    }
}

@Composable
private fun EditorBackButton(onBack: () -> Unit) {
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
            tint = Color.Unspecified,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
        )
    }
}

@Composable
private fun StatusBarOverview(
    state: BatteryEditorUiState,
    innerPadding: PaddingValues,
    onOpenPage: (BatteryEditorPage) -> Unit,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onBackgroundDecoration: (Int) -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit,
    showEmbeddedPreview: Boolean
) {
    var activeColorTarget by remember { mutableStateOf<StatusBarColorTarget?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
    ) {
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        if (showEmbeddedPreview) {
            BatteryPreview(state = state, page = BatteryEditorPage.OVERVIEW)
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._12sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            item {
                EditorDesignSection(title = stringResource(R.string.battery_editor_template)) {
                    TemplatePickerRow(
                        title = stringResource(R.string.battery_editor_battery_picker),
                        titleIcon = R.drawable.ic_statusbar_template_battery,
                        state = state,
                        component = BatteryThemeComponent.BATTERY,
                        selectedThemeId = state.config.selectedBatteryThemeId,
                        onMore = { onOpenPage(BatteryEditorPage.BATTERY_TEMPLATES) },
                        onSelectTheme = onSelectTheme
                    )
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                    TemplatePickerRow(
                        title = stringResource(R.string.battery_editor_emoji_picker),
                        titleIcon = R.drawable.img_statusbar_template_emoji,
                        state = state,
                        component = BatteryThemeComponent.EMOJI,
                        selectedThemeId = state.config.selectedEmojiThemeId,
                        onMore = { onOpenPage(BatteryEditorPage.EMOJI_TEMPLATES) },
                        onSelectTheme = onSelectTheme
                    )
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                    AnimationPickerRow(
                        state = state,
                        onMore = { onOpenPage(BatteryEditorPage.ANIMATION) },
                        onConfig = onConfig
                    )
                }
            }
            item {
                EditorDesignSection(title = stringResource(R.string.battery_editor_background)) {
                    DesignRowHeader(
                        title = stringResource(R.string.battery_editor_color),
                        titleIcon = R.drawable.img_statusbar_color_palette
                    )
                    StatusBarColorPalette(
                        selected = BatteryBackgroundSelectionPolicy.activeColor(state.config),
                        onSelected = onBackgroundColor,
                        onCustomClick = { activeColorTarget = StatusBarColorTarget.BACKGROUND }
                    )
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                    DesignRowHeader(
                        title = stringResource(R.string.battery_editor_theme_picker),
                        titleEmoji = stringResource(R.string.battery_editor_theme_icon),
                        onMore = { onOpenPage(BatteryEditorPage.BACKGROUND_THEMES) }
                    )
                    BackgroundThemeRow(
                        backgrounds = state.backgrounds,
                        selectedId = state.config.backgroundDecorationId,
                        loadingId = state.backgroundSelectionInProgress,
                        onSelected = onBackgroundDecoration
                    )
                }
            }
            item {
                EditorDesignSection(title = stringResource(R.string.battery_editor_percentage)) {
                    DesignRowHeader(title = stringResource(R.string.battery_editor_color))
                    StatusBarColorPalette(
                        selected = state.config.percentColorArgb,
                        onSelected = { color ->
                            onConfig(state.config.copy(percentColorArgb = color))
                        },
                        onCustomClick = { activeColorTarget = StatusBarColorTarget.PERCENTAGE }
                    )
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
                    DesignSlider(
                        label = stringResource(R.string.battery_editor_size_label),
                        value = state.config.percentSizeDp,
                        range = 10f..32f,
                        onValueChange = { value ->
                            onConfig(state.config.copy(percentSizeDp = value))
                        }
                    )
                }
            }
            item {
                EditorDesignSection(title = stringResource(R.string.battery_editor_layout_size)) {
                    DesignSlider(
                        label = stringResource(R.string.battery_editor_emoji_battery),
                        value = state.config.emojiSizeDp,
                        range = 12f..48f,
                        onValueChange = { value ->
                            onConfig(
                                state.config.copy(
                                    emojiSizeDp = value,
                                    batterySizeDp = value
                                )
                            )
                        }
                    )
                    DesignSlider(
                        label = stringResource(R.string.battery_editor_height),
                        value = state.config.barHeightDp,
                        range = state.barHeightRange.minimumDp..state.barHeightRange.maximumDp,
                        onValueChange = { value ->
                            onConfig(state.config.copy(barHeightDp = value))
                        }
                    )
                    DesignSlider(
                        label = stringResource(R.string.battery_editor_left_margin),
                        value = state.config.leftPaddingDp,
                        range = 0f..48f,
                        onValueChange = { value ->
                            onConfig(state.config.copy(leftPaddingDp = value))
                        }
                    )
                    DesignSlider(
                        label = stringResource(R.string.battery_editor_right_margin),
                        value = state.config.rightPaddingDp,
                        range = 0f..48f,
                        onValueChange = { value ->
                            onConfig(state.config.copy(rightPaddingDp = value))
                        }
                    )
                }
            }
            item {
                EditorDesignSection(title = stringResource(R.string.battery_editor_customize_icon)) {
                    StatusBarComponentGrid(onOpenPage)
                }
            }
        }
    }
    activeColorTarget?.let { target ->
        val selectedColor = when (target) {
            StatusBarColorTarget.BACKGROUND -> state.config.backgroundColorArgb
            StatusBarColorTarget.PERCENTAGE -> state.config.percentColorArgb
        }
        StatusBarColorPickerSheet(
            selectedColor = selectedColor,
            onColorChange = { color ->
                when (target) {
                    StatusBarColorTarget.BACKGROUND -> onBackgroundColor(color)
                    StatusBarColorTarget.PERCENTAGE -> {
                        onConfig(state.config.copy(percentColorArgb = color))
                    }
                }
            },
            onDismiss = { activeColorTarget = null }
        )
    }
}

@Composable
private fun EditorDesignSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = StatusBarRobotoSemiBold,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._18sdp),
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)),
                    ambientColor = colorResource(R.color.colors_1F666666),
                    spotColor = colorResource(R.color.colors_1F666666)
                )
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                .background(colorResource(R.color.colors_FFFFFF))
                .padding(dimensionResource(SdpR.dimen._12sdp))
        ) {
            content()
        }
    }
}

@Composable
private fun TemplatePickerRow(
    title: String,
    @DrawableRes titleIcon: Int,
    state: BatteryEditorUiState,
    component: BatteryThemeComponent,
    selectedThemeId: Int,
    onMore: () -> Unit,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit
) {
    DesignRowHeader(title = title, titleIcon = titleIcon, onMore = onMore)
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        items(
            BatteryEditorThemeDisplayPolicy.selectableThemes(
                themes = state.themes,
                trendingThemeIds = state.trendingEmojiThemeIds
            ),
            key = { it.id }
        ) { theme ->
            val locked = theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
                !state.isPremium && theme.id !in state.config.rewardUnlockedThemeIds
            val isPending = state.assetSelectionInProgress ==
                BatteryEditorThemeSelection(theme.id, component)
            TemplateOption(
                theme = theme,
                component = component,
                selected = selectedThemeId == theme.id || isPending,
                locked = locked,
                enabled = state.assetSelectionInProgress == null,
                onClick = { onSelectTheme(theme, component) }
            )
        }
    }
}

@Composable
private fun AnimationPickerRow(
    state: BatteryEditorUiState,
    onMore: () -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit
) {
    DesignRowHeader(
        title = stringResource(R.string.battery_component_animation),
        titleIcon = R.drawable.img_statusbar_template_animation,
        onMore = onMore
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
        items(state.animations, key = { it.id }) { animation ->
            AnimationTemplateOption(
                animation = animation,
                selected = state.config.showAnimation &&
                    state.config.animationAssetName == animation.name,
                onClick = {
                    onConfig(
                        state.config.copy(
                            showAnimation = true,
                            animationAssetName = animation.name
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun DesignRowHeader(
    title: String,
    @DrawableRes titleIcon: Int? = null,
    titleEmoji: String? = null,
    onMore: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            titleIcon != null -> Image(
                painter = painterResource(titleIcon),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
            titleEmoji != null -> Text(
                text = titleEmoji,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
            )
        }
        if (titleIcon != null || titleEmoji != null) {
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
        }
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = StatusBarRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
        Spacer(Modifier.weight(1f))
        if (onMore != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                    .clickable(onClick = onMore)
                    .padding(vertical = dimensionResource(SdpR.dimen._2sdp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.discover_more),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = StatusBarRobotoRegular,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._2sdp)))
                Icon(
                    painter = painterResource(R.drawable.ic_statusbar_more),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._11sdp))
                )
            }
        }
    }
}

@Composable
private fun AnimationTemplateOption(
    animation: BatteryAnimationEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._46sdp))
            .clip(shape)
            .background(
                colorResource(if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF)
            )
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(if (selected) R.color.colors_FB3675 else R.color.colors_FFEBF1),
                shape
            )
            .semantics {
                this.selected = selected
                contentDescription = animation.name
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BatteryAnimationAsset(
            animation = animation,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._38sdp))
        )
    }
}

@Composable
internal fun BatteryAnimationAsset(
    animation: BatteryAnimationEntry,
    modifier: Modifier = Modifier
) {
    if (animation.type == BatteryAnimationType.LOTTIE && animation.assetPath.isNotBlank()) {
        val spec = remember(animation.assetPath) {
            when {
                animation.assetPath.startsWith(ANDROID_ASSET_URI_PREFIX) -> {
                    LottieCompositionSpec.Asset(
                        animation.assetPath.removePrefix(ANDROID_ASSET_URI_PREFIX)
                    )
                }
                animation.assetPath.startsWith("http://") ||
                    animation.assetPath.startsWith("https://") -> {
                    LottieCompositionSpec.Url(animation.assetPath)
                }
                else -> LottieCompositionSpec.File(animation.assetPath)
            }
        }
        val compositionResult = rememberLottieComposition(spec)
        val composition by compositionResult
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = modifier
            )
        } else {
            Image(
                painter = painterResource(R.drawable.img_statusbar_template_animation),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
    } else {
        AsyncImage(
            model = animation.assetPath,
            fallback = painterResource(R.drawable.img_statusbar_template_animation),
            error = painterResource(R.drawable.img_statusbar_template_animation),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

@Composable
private fun TemplateOption(
    theme: BatteryThemeEntry,
    component: BatteryThemeComponent,
    selected: Boolean,
    locked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
    val fallbackPainter = painterResource(
        if (component == BatteryThemeComponent.BATTERY) {
            R.drawable.ic_battery_status
        } else {
            R.drawable.img_statusbar_template_emoji
        }
    )
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._46sdp))
            .clip(shape)
            .background(
                colorResource(
                    if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF
                )
            )
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(
                    if (selected) R.color.colors_FB3675 else R.color.colors_FFEBF1
                ),
                shape
            )
            .semantics {
                this.selected = selected
                contentDescription = theme.name
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = when (component) {
                BatteryThemeComponent.BATTERY -> theme.batteryPath
                BatteryThemeComponent.EMOJI -> theme.emojiPath
            },
            placeholder = fallbackPainter,
            fallback = fallbackPainter,
            error = fallbackPainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._38sdp))
        )
        if (locked) {
            PetPremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(SdpR.dimen._3sdp))
                    .size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
    }
}

@Composable
private fun StatusBarColorPalette(
    selected: Int?,
    onSelected: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    val colors = listOf(
        colorResource(R.color.colors_FFFFFF),
        colorResource(R.color.colors_000000),
        colorResource(R.color.colors_545454),
        colorResource(R.color.colors_FFCFCF),
        colorResource(R.color.colors_FFE5C7),
        colorResource(R.color.colors_FFF6A2),
        colorResource(R.color.colors_B7FCC6)
    )
    val presetArgb = remember(colors) { colors.map(Color::toArgb) }
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
        item(key = "custom_color") {
            val isSelected = selected != null && selected !in presetArgb
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._22sdp))
                    .clip(CircleShape)
                    .background(StatusBarColorWheelBrush)
                    .border(
                        dimensionResource(SdpR.dimen._1sdp),
                        colorResource(
                            if (isSelected) R.color.colors_FB3675 else R.color.colors_FFEBF1
                        ),
                        CircleShape
                    )
                    .semantics { this.selected = isSelected }
                    .clickable(onClick = onCustomClick)
            )
        }
        items(colors) { color ->
            val argb = color.toArgb()
            val isSelected = selected == argb
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._22sdp))
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        dimensionResource(SdpR.dimen._1sdp),
                        colorResource(
                            if (isSelected) R.color.colors_FB3675 else R.color.colors_FFEBF1
                        ),
                        CircleShape
                    )
                    .semantics { this.selected = isSelected }
                    .clickable { onSelected(argb) }
            )
        }
    }
}

@Composable
private fun BackgroundThemeRow(
    backgrounds: List<BatteryDecorationEntry>,
    selectedId: Int,
    loadingId: Int?,
    onSelected: (Int) -> Unit
) {
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
    val previewBackgrounds = remember(backgrounds, selectedId) {
        statusBarBackgroundPreviewItems(backgrounds, selectedId)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
        items(previewBackgrounds, key = { it.id }) { background ->
            BackgroundThemeOption(
                background = background,
                selected = selectedId == background.id,
                loading = loadingId == background.id,
                modifier = Modifier
                    .width(dimensionResource(SdpR.dimen._77sdp))
                    .height(dimensionResource(SdpR.dimen._38sdp)),
                onClick = { onSelected(background.id) }
            )
        }
    }
}

internal fun statusBarBackgroundPreviewItems(
    backgrounds: List<BatteryDecorationEntry>,
    selectedId: Int
): List<BatteryDecorationEntry> {
    val initial = backgrounds.take(INLINE_BACKGROUND_PREVIEW_COUNT)
    if (initial.any { it.id == selectedId }) return initial
    val selected = backgrounds.firstOrNull { it.id == selectedId } ?: return initial
    return backgrounds.take(INLINE_BACKGROUND_PREVIEW_COUNT - 1) + selected
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesignSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val coercedValue = value.coerceIn(range)
    val pink = colorResource(R.color.colors_FB3675)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        Text(
            text = label,
            color = colorResource(R.color.colors_212327),
            fontFamily = StatusBarRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = coercedValue,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = pink,
                    activeTrackColor = pink,
                    inactiveTrackColor = colorResource(R.color.colors_FFEBF1),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(SdpR.dimen._38sdp))
            )
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._12sdp)))
            Text(
                text = stringResource(R.string.battery_editor_dp_value, value.toInt()),
                color = colorResource(R.color.colors_212327),
                fontFamily = StatusBarRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(dimensionResource(SdpR.dimen._25sdp))
            )
        }
    }
}

@Composable
private fun StatusBarComponentGrid(onOpenPage: (BatteryEditorPage) -> Unit) {
    val destinations = listOf(
        FigmaComponentDestination(R.string.battery_component_airplane, BatteryEditorPage.AIRPLANE, R.drawable.ic_statusbar_custom_airplane),
        FigmaComponentDestination(R.string.battery_component_ringer, BatteryEditorPage.RINGER, R.drawable.ic_statusbar_custom_ringer),
        FigmaComponentDestination(R.string.battery_component_date_short, BatteryEditorPage.DATE_TIME, R.drawable.ic_statusbar_custom_date),
        FigmaComponentDestination(R.string.battery_component_hotspot, BatteryEditorPage.HOTSPOT, R.drawable.ic_statusbar_custom_hotspot),
        FigmaComponentDestination(R.string.battery_component_emotion, BatteryEditorPage.EMOJI, R.drawable.ic_statusbar_custom_emotion),
        FigmaComponentDestination(R.string.battery_component_wifi, BatteryEditorPage.WIFI, R.drawable.ic_statusbar_custom_wifi),
        FigmaComponentDestination(R.string.battery_component_signal, BatteryEditorPage.SIGNAL, R.drawable.ic_statusbar_custom_signal),
        FigmaComponentDestination(R.string.battery_component_data_short, BatteryEditorPage.DATA, R.drawable.ic_statusbar_custom_data),
        FigmaComponentDestination(R.string.battery_component_charge_short, BatteryEditorPage.CHARGE, R.drawable.ic_statusbar_custom_charge),
        FigmaComponentDestination(R.string.battery_component_clock, BatteryEditorPage.CLOCK, R.drawable.ic_statusbar_custom_clock)
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))) {
        destinations.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._15sdp))
            ) {
                rowItems.forEach { destination ->
                    FigmaComponentTile(
                        destination = destination,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenPage(destination.page) }
                    )
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FigmaComponentTile(
    destination: FigmaComponentDestination,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_FFEBF1)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(destination.icon),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._28sdp))
            )
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Text(
            text = stringResource(destination.label),
            color = colorResource(R.color.colors_212327),
            fontFamily = StatusBarRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusBarApplyPanel(enabled: Boolean, onApply: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._4sdp),
                shape = RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                ),
                ambientColor = Color(0x1A000000),
                spotColor = Color(0x1A000000)
            )
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                top = dimensionResource(SdpR.dimen._18sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                bottom = dimensionResource(SdpR.dimen._9sdp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._38sdp))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    ),
                    alpha = if (enabled) 1f else 0.45f
                )
                .clickable(enabled = enabled, onClick = onApply),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.battery_apply),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = StatusBarRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp
            )
        }
    }
}

@Composable
private fun StatusBarPicker(
    state: BatteryEditorUiState,
    page: BatteryEditorPage,
    innerPadding: PaddingValues,
    onPremium: () -> Unit,
    onSelectTheme: (BatteryThemeEntry, BatteryThemeComponent) -> Unit,
    onBackgroundDecoration: (Int) -> Unit,
    showEmbeddedPreview: Boolean
) {
    val columns = if (page == BatteryEditorPage.BACKGROUND_THEMES) 2 else 3
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
    ) {
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        if (showEmbeddedPreview) {
            BatteryPreview(state = state, page = page)
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = dimensionResource(SdpR.dimen._12sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            if (page == BatteryEditorPage.BACKGROUND_THEMES) {
                items(state.backgrounds.size, key = { state.backgrounds[it].id }) { index ->
                    val background = state.backgrounds[index]
                    // TODO: replace this policy when background entitlement is catalog data.
                    val locked = index >= FIGMA_FREE_BACKGROUND_COUNT && !state.isPremium
                    BackgroundThemeOption(
                        background = background,
                        selected = state.config.backgroundDecorationId == background.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(SdpR.dimen._46sdp)),
                        showSelectionBorder = true,
                        locked = locked,
                        loading = state.backgroundSelectionInProgress == background.id,
                        onClick = {
                            if (locked) onPremium() else onBackgroundDecoration(background.id)
                        }
                    )
                }
            } else {
                val component = if (page == BatteryEditorPage.BATTERY_TEMPLATES) {
                    BatteryThemeComponent.BATTERY
                } else {
                    BatteryThemeComponent.EMOJI
                }
                val selectedId = if (component == BatteryThemeComponent.BATTERY) {
                    state.config.selectedBatteryThemeId
                } else {
                    state.config.selectedEmojiThemeId
                }
                items(
                    BatteryEditorThemeDisplayPolicy.selectableThemes(
                        themes = state.themes,
                        trendingThemeIds = state.trendingEmojiThemeIds
                    ),
                    key = { it.id }
                ) { theme ->
                    val locked = theme.entitlement == BatteryThemeEntitlement.PREMIUM &&
                        !state.isPremium && theme.id !in state.config.rewardUnlockedThemeIds
                    val isPending = state.assetSelectionInProgress ==
                        BatteryEditorThemeSelection(theme.id, component)
                    PickerThemeCard(
                        theme = theme,
                        component = component,
                        selected = selectedId == theme.id || isPending,
                        locked = locked,
                        enabled = state.assetSelectionInProgress == null,
                        onClick = { onSelectTheme(theme, component) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerThemeCard(
    theme: BatteryThemeEntry,
    component: BatteryThemeComponent,
    selected: Boolean,
    locked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    val fallbackPainter = painterResource(
        if (component == BatteryThemeComponent.BATTERY) {
            R.drawable.ic_battery_status
        } else {
            R.drawable.img_statusbar_template_emoji
        }
    )
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
            .semantics {
                this.selected = selected
                contentDescription = theme.name
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = if (component == BatteryThemeComponent.BATTERY) {
                theme.batteryPath
            } else {
                theme.emojiPath
            },
            placeholder = fallbackPainter,
            fallback = fallbackPainter,
            error = fallbackPainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(PICKER_ART_FRACTION)
        )
        if (locked) {
            PetPremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(SdpR.dimen._6sdp))
                    .size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
    }
}

@Composable
private fun BackgroundThemeOption(
    background: BatteryDecorationEntry,
    selected: Boolean,
    modifier: Modifier,
    showSelectionBorder: Boolean = true,
    locked: Boolean = false,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = modifier
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                if (showSelectionBorder && selected) {
                    colorResource(R.color.colors_FB3675)
                } else {
                    Color.Transparent
                },
                shape
            )
            .semantics { this.selected = selected }
            .clickable(enabled = !loading, onClick = onClick)
    ) {
        if (background.assetPath.isNotBlank()) {
            AsyncImage(
                model = background.pickerPath,
                contentDescription = background.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (locked) {
            PetPremiumBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(SdpR.dimen._5sdp))
                    .size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
        if (loading) {
            CircularProgressIndicator(
                color = colorResource(R.color.colors_FB3675),
                strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
    }
}

@Composable
private fun pickerTitle(page: BatteryEditorPage): String = when (page) {
    BatteryEditorPage.BATTERY_TEMPLATES -> stringResource(R.string.battery_editor_battery_picker)
    BatteryEditorPage.EMOJI_TEMPLATES -> stringResource(R.string.battery_editor_emoji_picker)
    BatteryEditorPage.BACKGROUND_THEMES -> stringResource(R.string.battery_editor_theme_picker)
    else -> stringResource(R.string.battery_editor_title)
}

private data class FigmaComponentDestination(
    @param:androidx.annotation.StringRes val label: Int,
    val page: BatteryEditorPage,
    @param:DrawableRes val icon: Int
)

private enum class StatusBarColorTarget {
    BACKGROUND,
    PERCENTAGE
}

private const val PICKER_ART_FRACTION = 0.7303f
private const val FIGMA_FREE_BACKGROUND_COUNT = 5
private const val INLINE_BACKGROUND_PREVIEW_COUNT = 5
private const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"
