package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_PERMISSION
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun GrantPermissionDialog(
    onGrantPermission: () -> Unit,
    onMaybeLater: () -> Unit
) {
    var isConsentGranted by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onMaybeLater,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        HideDialogNavigationBar()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f))
                .statusBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMaybeLater
                )
        ) {
            GrantPermissionDialogContent(
                isConsentGranted = isConsentGranted,
                onConsentChanged = { isConsentGranted = it },
                onGrantPermission = onGrantPermission,
                onMaybeLater = onMaybeLater,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .heightIn(max = maxHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }
    }
}

@Composable
internal fun GrantPermissionDialogContent(
    isConsentGranted: Boolean,
    onConsentChanged: (Boolean) -> Unit,
    onGrantPermission: () -> Unit,
    onMaybeLater: () -> Unit,
    modifier: Modifier = Modifier,
    nativeAdContent: @Composable () -> Unit = { AccessibilityDisclosureNativeAd() }
) {
    val bodyColor = colorResource(R.color.colors_6F7073)
    val headingColor = colorResource(R.color.colors_212327)
    val intro = stringResource(R.string.accessibility_disclosure_intro)
    val useHeading = stringResource(R.string.accessibility_disclosure_use_heading)
    val useItems = stringResource(R.string.accessibility_disclosure_use_items)
    val visualOnly = stringResource(R.string.accessibility_disclosure_visual_only)
    val noAccessHeading = stringResource(R.string.accessibility_disclosure_no_access_heading)
    val noAccessItems = stringResource(R.string.accessibility_disclosure_no_access_items)
    val conclusion = stringResource(R.string.accessibility_disclosure_conclusion)
    val disclosure = buildAnnotatedString {
        append(intro)
        append("\n\n")
        append(useHeading)
        append("\n")
        append(useItems)
        append("\n")
        append(visualOnly)
        append("\n\n")
        pushStyle(SpanStyle(color = headingColor, fontWeight = FontWeight.Medium))
        append(noAccessHeading)
        pop()
        append("\n")
        append(noAccessItems)
        append("\n\n")
        append(conclusion)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._28sdp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = dimensionResource(SdpR.dimen._25sdp),
                        height = dimensionResource(SdpR.dimen._3sdp)
                    )
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_C8C8C9))
            )
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

        Text(
            text = stringResource(R.string.accessibility_disclosure_title),
            color = headingColor,
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(312f / 328f)
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
        )

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))

        Text(
            text = disclosure,
            color = bodyColor,
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
                .verticalScroll(rememberScrollState())
        )

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isConsentGranted,
                        role = Role.Checkbox,
                        onValueChange = onConsentChanged
                    ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._5sdp)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        if (isConsentGranted) {
                            R.drawable.ic_rate_checkbox_checked
                        } else {
                            R.drawable.ic_rate_checkbox_unchecked
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                )
                Text(
                    text = stringResource(R.string.accessibility_disclosure_consent),
                    color = colorResource(R.color.colors_FB3675),
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
            ) {
                RewardGradientButton(
                    text = stringResource(R.string.accessibility_disclosure_allow),
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canGrantAccessibilityPermission(isConsentGranted),
                    retainEnabledStyleWhenDisabled = true
                )
                RewardOutlineButton(
                    text = stringResource(R.string.accessibility_disclosure_close),
                    onClick = onMaybeLater,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        nativeAdContent()
    }
}

internal fun canGrantAccessibilityPermission(isConsentGranted: Boolean): Boolean =
    isConsentGranted

@Composable
private fun AccessibilityDisclosureNativeAd() {
    NativeAdInternal(
        screenCode = SCREEN_PERMISSION,
        instanceKey = "accessibility_disclosure",
        adTypeOverride = AdType.HEIGHT_222,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun GrantPermissionDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        GrantPermissionDialogContent(
            isConsentGranted = true,
            onConsentChanged = {},
            onGrantPermission = {},
            onMaybeLater = {},
            nativeAdContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(SdpR.dimen._171sdp))
                        .background(colorResource(R.color.colors_E6E6E6))
                )
            }
        )
    }
}
