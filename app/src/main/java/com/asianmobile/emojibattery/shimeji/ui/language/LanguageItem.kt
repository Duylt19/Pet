package com.asianmobile.emojibattery.shimeji.ui.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as R_dimen
import com.intuit.ssp.R as R_dimen_ssp

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/11/2026
 */
@Composable
internal fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val itemShape = RoundedCornerShape(dimensionResource(R_dimen.dimen._12sdp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensionResource(R_dimen.dimen._9sdp),
                shape = itemShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(itemShape)
            .background(colorResource(R.color.colors_212327))
            .clickable(onClick = onClick)
            .padding(dimensionResource(R_dimen.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R_dimen.dimen._9sdp))
    ) {
        AsyncImage(
            model = language.flag,
            contentDescription = language.name,
            modifier = Modifier
                .size(dimensionResource(R_dimen.dimen._25sdp))
                .clip(CircleShape)
                .border(1.dp, colorResource(R.color.colors_424447), CircleShape),
            contentScale = ContentScale.Crop
        )

        Text(
            text = language.name,
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontSize = dimensionResource(id = R_dimen_ssp.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(id = R_dimen_ssp.dimen._18ssp).value.sp,
            color = colorResource(R.color.white),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(
                id = if (isSelected) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected
            ),
            contentDescription = null,
            tint = if (isSelected) {
                colorResource(R.color.colors_3369FD)
            } else {
                colorResource(R.color.colors_424447)
            },
            modifier = Modifier.size(dimensionResource(R_dimen.dimen._18sdp))
        )
    }
}
