package com.asianmobile.emojibattery.shimeji.ui.permissions

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.battery.overlay.BatteryAccessibility
import com.asianmobile.emojibattery.shimeji.ui.shared.component.rememberAccessibilitySettingsLauncher
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val AccessibilityRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val AccessibilityRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))

@Composable
fun AccessibilityHowToUseScreen(
    onNavigateBack: () -> Unit,
    onPermissionGranted: () -> Unit,
    viewModel: AccessibilityHowToUseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openAccessibilitySettings = rememberAccessibilitySettingsLauncher {
        if (BatteryAccessibility.isEnabled(context)) onPermissionGranted()
    }

    TrackScreenView(ScreenName.ACCESSIBILITY_HOW_TO_USE)
    BackHandler(onBack = onNavigateBack)

    AccessibilityHowToUseContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onGoToSettings = openAccessibilitySettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccessibilityHowToUseContent(
    uiState: AccessibilityHowToUseUiState,
    onNavigateBack: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF))
    ) {
        Image(
            painter = painterResource(R.drawable.img_accessibility_how_to_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(360f / 800f)
        )
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                AccessibilityHowToUseTopBar(
                    collapsedFraction = scrollBehavior.state.collapsedFraction,
                    onNavigateBack = onNavigateBack,
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                AccessibilityHowToUseActionBar(onGoToSettings)
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    top = innerPadding.calculateTopPadding() +
                        dimensionResource(SdpR.dimen._6sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    bottom = innerPadding.calculateBottomPadding() +
                        dimensionResource(SdpR.dimen._12sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._18sdp)
                )
            ) {
                items(
                    items = uiState.steps,
                    key = AccessibilityHowToStep::name
                ) { step ->
                    AccessibilityHowToUseStep(step)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibilityHowToUseTopBar(
    collapsedFraction: Float,
    onNavigateBack: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    val expandedTitleSize = dimensionResource(SspR.dimen._18ssp).value.sp
    val collapsedTitleSize = dimensionResource(SspR.dimen._14ssp).value.sp
    val titleSize = (
        expandedTitleSize.value +
            (collapsedTitleSize.value - expandedTitleSize.value) * collapsedFraction
        ).sp

    // A transparent app bar lets lazy-list rows remain visible while they scroll underneath it.
    // Keep the same pastel treatment as the wallpaper, but make this layer opaque so the title
    // and navigation icon never collide visually with the instructions on compact devices.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colorResource(R.color.colors_FFE8F8),
                        colorResource(R.color.colors_F6FBFF),
                        colorResource(R.color.colors_EAF7FF)
                    )
                )
            )
    ) {
        LargeTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.accessibility_how_to_use_title),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = AccessibilityRobotoSemiBold,
                    fontSize = titleSize,
                    lineHeight = dimensionResource(SspR.dimen._24ssp).value.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._43sdp))
                        .clickable(
                            role = Role.Button,
                            onClick = onNavigateBack
                        ),
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
            collapsedHeight = dimensionResource(SdpR.dimen._43sdp),
            expandedHeight = dimensionResource(SdpR.dimen._77sdp),
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
private fun AccessibilityHowToUseStep(step: AccessibilityHowToStep) {
    val spec = step.visualSpec()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
            verticalAlignment = Alignment.Top
        ) {
            AccessibilityStepNumber(step.ordinal + 1)
            AccessibilityStepText(spec)
        }
        Image(
            painter = painterResource(spec.imageRes),
            contentDescription = stringResource(
                R.string.accessibility_how_to_step_image_description,
                step.ordinal + 1
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(
                width = spec.imageWidth,
                height = spec.imageHeight
            )
        )
    }
}

@Composable
private fun AccessibilityStepNumber(number: Int) {
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .shadow(
                elevation = dimensionResource(SdpR.dimen._3sdp),
                shape = CircleShape,
                ambientColor = colorResource(R.color.colors_FB3675),
                spotColor = colorResource(R.color.colors_FB3675)
            )
            .background(colorResource(R.color.colors_FB3675), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = AccessibilityRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
    }
}

@Composable
private fun AccessibilityStepText(spec: AccessibilityHowToStepVisualSpec) {
    val accentColor = colorResource(R.color.colors_FB3675)
    val prefix = stringResource(spec.prefixRes)
    val accent = spec.accentRes?.let { stringResource(it) }.orEmpty()
    val suffix = spec.suffixRes?.let { stringResource(it) }.orEmpty()
    Text(
        text = buildAnnotatedString {
            append(prefix)
            if (accent.isNotEmpty()) {
                withStyle(SpanStyle(color = accentColor)) { append(accent) }
            }
            append(suffix)
        },
        modifier = Modifier.fillMaxWidth(),
        color = colorResource(R.color.colors_0D0D0D),
        fontFamily = AccessibilityRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
    )
}

@Composable
private fun AccessibilityHowToStep.visualSpec(): AccessibilityHowToStepVisualSpec = when (this) {
    AccessibilityHowToStep.OPEN_INSTALLED_APPS -> AccessibilityHowToStepVisualSpec(
        prefixRes = R.string.accessibility_how_to_step_1_prefix,
        accentRes = R.string.accessibility_how_to_step_1_accent,
        suffixRes = R.string.accessibility_how_to_step_1_suffix,
        imageRes = R.drawable.img_accessibility_how_to_step_1,
        imageWidth = dimensionResource(SdpR.dimen._103sdp),
        imageHeight = dimensionResource(SdpR.dimen._71sdp)
    )

    AccessibilityHowToStep.SELECT_EMOJI_BATTERY -> AccessibilityHowToStepVisualSpec(
        prefixRes = R.string.accessibility_how_to_step_2_prefix,
        accentRes = R.string.accessibility_how_to_step_2_accent,
        imageRes = R.drawable.img_accessibility_how_to_step_2,
        imageWidth = dimensionResource(SdpR.dimen._103sdp),
        imageHeight = dimensionResource(SdpR.dimen._71sdp)
    )

    AccessibilityHowToStep.ENABLE_SERVICE -> AccessibilityHowToStepVisualSpec(
        prefixRes = R.string.accessibility_how_to_step_3,
        imageRes = R.drawable.img_accessibility_how_to_step_3,
        imageWidth = dimensionResource(SdpR.dimen._103sdp),
        imageHeight = dimensionResource(SdpR.dimen._71sdp)
    )

    AccessibilityHowToStep.CONFIRM_ACCESS -> AccessibilityHowToStepVisualSpec(
        prefixRes = R.string.accessibility_how_to_step_4_prefix,
        accentRes = R.string.accessibility_how_to_step_4_accent,
        suffixRes = R.string.accessibility_how_to_step_4_suffix,
        imageRes = R.drawable.img_accessibility_how_to_step_4,
        imageWidth = dimensionResource(SdpR.dimen._95sdp),
        imageHeight = dimensionResource(SdpR.dimen._99sdp)
    )
}

private data class AccessibilityHowToStepVisualSpec(
    @param:StringRes val prefixRes: Int,
    @param:StringRes val accentRes: Int? = null,
    @param:StringRes val suffixRes: Int? = null,
    @param:DrawableRes val imageRes: Int,
    val imageWidth: Dp,
    val imageHeight: Dp
)

@Composable
private fun AccessibilityHowToUseActionBar(onGoToSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(SdpR.dimen._9sdp),
                ambientColor = colorResource(R.color.colors_1F666666),
                spotColor = colorResource(R.color.colors_1F666666)
            )
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._77sdp)))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    )
                )
                .clickable(
                    role = Role.Button,
                    onClick = onGoToSettings
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.accessibility_how_to_go_to_settings),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = AccessibilityRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 882, showBackground = true)
@Composable
private fun AccessibilityHowToUsePreview() {
    AccessibilityHowToUseContent(
        uiState = AccessibilityHowToUseUiState(),
        onNavigateBack = {},
        onGoToSettings = {}
    )
}
