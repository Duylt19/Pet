package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Commits a child editor's working values into the overview draft. Persisting the draft remains the
 * responsibility of the overview Apply action.
 */
@Composable
internal fun BatteryEditorDonePanel(onDone: () -> Unit) {
    val shape = RoundedCornerShape(
        topStart = dimensionResource(SdpR.dimen._18sdp),
        topEnd = dimensionResource(SdpR.dimen._18sdp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimensionResource(SdpR.dimen._4sdp), shape)
            .clip(shape)
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
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    )
                )
                .semantics { role = Role.Button }
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.common_done),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
