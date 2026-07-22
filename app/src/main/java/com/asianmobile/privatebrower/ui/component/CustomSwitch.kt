package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.asianmobile.privatebrower.R

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color = colorResource(id = R.color.blue_007BFD)
) {
    val trackWidth = dimensionResource(com.intuit.sdp.R.dimen._38sdp)
    val trackHeight = dimensionResource(com.intuit.sdp.R.dimen._21sdp)
    val thumbSize = dimensionResource(com.intuit.sdp.R.dimen._17sdp)
    val padding = dimensionResource(com.intuit.sdp.R.dimen._2sdp)

    val trackColor = if (checked) checkedColor else colorResource(R.color.gray_CCCCCC)
    val alignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._11sdp)))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = padding),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .background(Color.White, CircleShape)
        )
    }
}


