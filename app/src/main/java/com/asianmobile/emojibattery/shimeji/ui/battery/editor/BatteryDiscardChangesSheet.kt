package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardGradientButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOfferSheet
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOutlineButton
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val DiscardRobotoRegular = FontFamily(Font(R.font.roboto_regular))
private val DiscardRobotoMedium = FontFamily(Font(R.font.roboto_medium))

@Composable
internal fun BatteryDiscardChangesSheet(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    RewardOfferSheet(onDismiss = onDismiss) {
        BatteryDiscardChangesSheetContent(
            onCancel = onDismiss,
            onExit = onExit
        )
    }
}

@Composable
internal fun BatteryDiscardChangesSheetContent(
    onCancel: () -> Unit,
    onExit: () -> Unit,
    nativeAdContent: @Composable () -> Unit = { BatteryDiscardNativeAd() }
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(312f / 336f)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            Text(
                text = stringResource(R.string.battery_discard_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = DiscardRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.battery_discard_message),
                color = colorResource(R.color.colors_6F7073),
                fontFamily = DiscardRobotoRegular,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            RewardOutlineButton(
                text = stringResource(R.string.battery_discard_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            RewardGradientButton(
                text = stringResource(R.string.battery_discard_exit),
                onClick = onExit,
                modifier = Modifier.weight(1f)
            )
        }
        nativeAdContent()
    }
}

@Composable
private fun BatteryDiscardNativeAd() {
    NativeAdInternal(
        screenCode = SCREEN_HOME,
        instanceKey = "battery_editor_discard",
        adTypeOverride = AdType.HEIGHT_222,
        modifier = Modifier.fillMaxWidth()
    )
}
