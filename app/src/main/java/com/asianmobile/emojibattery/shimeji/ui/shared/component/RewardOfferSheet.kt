package com.asianmobile.emojibattery.shimeji.ui.shared.component

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val RewardButtonFont = FontFamily(Font(R.font.roboto_medium))

@Composable
internal fun RewardOfferSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        HideDialogNavigationBar()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            RewardOfferSheetSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                content = content
            )
        }
    }
}

@Composable
internal fun RewardOfferSheetSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                start = dimensionResource(SdpR.dimen._9sdp),
                end = dimensionResource(SdpR.dimen._9sdp),
                top = dimensionResource(SdpR.dimen._15sdp),
                bottom = dimensionResource(SdpR.dimen._9sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        content = content
    )
}

@Composable
internal fun RewardGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    retainEnabledStyleWhenDisabled: Boolean = false,
    iconRes: Int? = null
) {
    val enabledGradient = Brush.horizontalGradient(
        listOf(
            colorResource(R.color.colors_C95DFF),
            colorResource(R.color.colors_FB54BB)
        )
    )
    val disabledColor = colorResource(R.color.colors_C8C8C9)
    val background = if (enabled || retainEnabledStyleWhenDisabled) {
        enabledGradient
    } else {
        Brush.horizontalGradient(listOf(disabledColor, disabledColor))
    }
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._38sdp))
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        RewardButtonContent(
            text = text,
            iconRes = iconRes,
            textColor = colorResource(R.color.colors_FFFFFF)
        )
    }
}

@Composable
internal fun RewardOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null
) {
    val enabledGradient = Brush.horizontalGradient(
        listOf(
            colorResource(R.color.colors_C95DFF),
            colorResource(R.color.colors_FB54BB)
        )
    )
    val disabledColor = colorResource(R.color.colors_C8C8C9)
    val border = if (enabled) {
        enabledGradient
    } else {
        Brush.horizontalGradient(listOf(disabledColor, disabledColor))
    }
    val textStyle = if (enabled) {
        TextStyle(brush = enabledGradient)
    } else {
        TextStyle(color = disabledColor)
    }
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._38sdp))
            .clip(CircleShape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(dimensionResource(SdpR.dimen._1sdp), border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        RewardButtonContent(text = text, iconRes = iconRes, textStyle = textStyle)
    }
}

@Composable
private fun RewardButtonContent(
    text: String,
    iconRes: Int?,
    textColor: Color = Color.Unspecified,
    textStyle: TextStyle = TextStyle.Default
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        iconRes?.let { drawable ->
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
        Text(
            text = text,
            color = textColor,
            style = textStyle,
            fontFamily = RewardButtonFont,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
    }
}

@Composable
@Suppress("DEPRECATION")
internal fun HideDialogNavigationBar() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window == null) {
            onDispose {}
        } else {
            val decorView = window.decorView
            val controller = WindowInsetsControllerCompat(window, decorView)

            fun hideNavigationBar() {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.navigationBarColor = AndroidColor.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                controller.isAppearanceLightNavigationBars = false
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }

            val focusListener =
                android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) hideNavigationBar()
                }
            hideNavigationBar()
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

            onDispose {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                }
            }
        }
    }
}
