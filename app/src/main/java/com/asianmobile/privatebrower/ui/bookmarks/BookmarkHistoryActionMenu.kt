package com.asianmobile.privatebrower.ui.bookmarks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

internal data class BookmarkHistoryMenuAction(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    val onClick: () -> Unit
)

@Composable
internal fun BookmarkHistoryActionMenu(
    expanded: Boolean,
    actions: List<BookmarkHistoryMenuAction>,
    onDismissRequest: () -> Unit
) {
    if (!expanded) return

    val verticalOffset = dimensionResource(SdpR.dimen._21sdp)
    val popupOffset = with(LocalDensity.current) {
        IntOffset(x = 0, y = verticalOffset.roundToPx())
    }

    Popup(
        alignment = Alignment.TopEnd,
        offset = popupOffset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.width(dimensionResource(SdpR.dimen._132sdp)),
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)),
            color = colorResource(R.color.colors_333538),
            shadowElevation = dimensionResource(SdpR.dimen._6sdp)
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(SdpR.dimen._3sdp)),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._3sdp)
                )
            ) {
                actions.forEach { action ->
                    BookmarkHistoryMenuRow(
                        action = action,
                        onClick = {
                            onDismissRequest()
                            action.onClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkHistoryMenuRow(
    action: BookmarkHistoryMenuAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._34sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
            .clickable(onClick = onClick)
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(action.iconRes),
            contentDescription = null,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

        Text(
            text = stringResource(action.labelRes),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._15ssp).toSp()
            },
            letterSpacing = 0.sp,
            maxLines = 1
        )
    }
}
