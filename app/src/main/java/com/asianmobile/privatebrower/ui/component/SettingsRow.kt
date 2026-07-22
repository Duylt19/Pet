package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

sealed class SettingsTrailing {
    object Chevron : SettingsTrailing()
    data class TextTrailing(val value: String) : SettingsTrailing()
    data class SwitchTrailing(
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingsTrailing()
    data class Custom(val content: @Composable () -> Unit) : SettingsTrailing()
}

@Composable
fun SettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    trailing: SettingsTrailing = SettingsTrailing.Chevron,
    renderIconAsImage: Boolean = false,
    onClick: () -> Unit
) {
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontRegular = FontFamily(Font(R.font.inter_regular))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (renderIconAsImage) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._9sdp)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorResource(R.color.colors_FFFFFF),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                fontFamily = fontMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colorResource(R.color.colors_9B9C9E),
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    fontFamily = fontRegular
                )
            }
        }

        when (trailing) {
            is SettingsTrailing.Chevron -> {
                Icon(
                    painter = painterResource(R.drawable.ic_setting_chevron_right_v2),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
            is SettingsTrailing.TextTrailing -> {
                Text(
                    text = trailing.value,
                    color = colorResource(R.color.colors_A6A7B1),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    fontFamily = fontRegular
                )
                Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
                Icon(
                    painter = painterResource(R.drawable.ic_setting_chevron_right_v2),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
            is SettingsTrailing.SwitchTrailing -> {
                val thumbOffset by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (trailing.checked) {
                        dimensionResource(SdpR.dimen._17sdp)
                    } else {
                        dimensionResource(SdpR.dimen._2sdp)
                    },
                    animationSpec = androidx.compose.animation.core.tween(200),
                    label = "switchThumb"
                )
                val switchShape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                val trackModifier = if (trailing.checked) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                colorResource(R.color.colors_1E86F6),
                                colorResource(R.color.colors_0D45ED)
                            )
                        ),
                        shape = switchShape
                    )
                } else {
                    Modifier.background(
                        color = colorResource(R.color.colors_4D4D4D),
                        shape = switchShape
                    )
                }

                Box(
                    modifier = Modifier
                        .width(dimensionResource(SdpR.dimen._34sdp))
                        .height(dimensionResource(SdpR.dimen._18sdp))
                        .clip(switchShape)
                        .then(trackModifier)
                        .clickable { trailing.onCheckedChange(!trailing.checked) }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = thumbOffset)
                            .align(Alignment.CenterStart)
                            .size(dimensionResource(SdpR.dimen._15sdp))
                            .clip(CircleShape)
                            .background(colorResource(R.color.colors_FFFFFF))
                    )
                }
            }
            is SettingsTrailing.Custom -> trailing.content()
        }
    }
}
