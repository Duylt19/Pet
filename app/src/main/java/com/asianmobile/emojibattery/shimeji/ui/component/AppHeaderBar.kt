package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

sealed class AppHeaderLeading {
    object Hamburger : AppHeaderLeading()
    object Back : AppHeaderLeading()
    object Close : AppHeaderLeading()
    object Settings : AppHeaderLeading()
}

@Composable
fun AppHeaderBar(
    title: String,
    leadingIcon: AppHeaderLeading? = AppHeaderLeading.Hamburger,
    onLeadingClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val fontSemiBold = FontFamily(Font(R.font.roboto_semibold))
    val bgColor = colorResource(R.color.colors_FFF9F4)
    val contentColor = colorResource(R.color.colors_2F2440)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._43sdp))
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            contentAlignment = Alignment.Center
        ) {
            // Centered Title
            Text(
                text = title,
                fontFamily = fontSemiBold,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                color = contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Leading and Trailing Icons Container
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon
                if (leadingIcon != null) {
                    val iconRes = when (leadingIcon) {
                        AppHeaderLeading.Hamburger -> R.drawable.ic_hamburger_list
                        AppHeaderLeading.Back -> R.drawable.ic_arrow_back
                        AppHeaderLeading.Close -> R.drawable.ic_close_x
                        AppHeaderLeading.Settings -> R.drawable.ic_settings_outline
                    }

                    Box(
                        modifier = Modifier
                            .size(dimensionResource(SdpR.dimen._34sdp))
                            .clip(CircleShape)
                            .background(colorResource(R.color.colors_FFFFFB))
                            .clickable(onClick = onLeadingClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f))

                // Trailing content
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing
                )
            }
        }
    }
}
