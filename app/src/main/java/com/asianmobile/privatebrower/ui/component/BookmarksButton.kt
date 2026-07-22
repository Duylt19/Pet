package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun BookmarksButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontBold = FontFamily(Font(R.font.inter_bold))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._30sdp))
            .clip(RoundedCornerShape(percent = 50))
            .background(colorResource(R.color.colors_8B5CF6))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmarks_pill),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
            Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
            Text(
                text = stringResource(R.string.home_bookmarks_button_label),
                fontFamily = fontBold,
                fontWeight = FontWeight.Bold,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                color = Color.White
            )
        }
    }
}
