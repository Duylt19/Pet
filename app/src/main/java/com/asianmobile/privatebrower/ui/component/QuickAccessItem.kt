package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.model.QuickAccessShortcut
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun QuickAccessItem(
    shortcut: QuickAccessShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontRegular = FontFamily(Font(R.font.inter_regular))

    // Background color of the icon container circle
    val iconBackgroundModifier = if (shortcut.id == "ins") {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF833AB4),
                    Color(0xFFFD1D1D),
                    Color(0xFFFCAF45)
                )
            )
        )
    } else {
        val color = try {
            Color(android.graphics.Color.parseColor(shortcut.brandColorHex))
        } catch (e: Exception) {
            colorResource(R.color.colorPrimary)
        }
        Modifier.background(color)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._36sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                width = 1.dp,
                color = colorResource(R.color.colors_EEEEEE),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand-colored circle container for SVG logo
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._24sdp))
                .clip(CircleShape)
                .then(iconBackgroundModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(shortcut.iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._9sdp)))

        Text(
            text = stringResource(shortcut.labelRes),
            fontFamily = fontRegular,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            color = colorResource(R.color.colors_000000),
            modifier = Modifier.weight(1f)
        )
    }
}
