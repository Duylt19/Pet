package com.asianmobile.emojibattery.shimeji.ui.pet.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR

@PreviewTest
@Preview(name = "Pet Room focus arrow", widthDp = 64, heightDp = 64)
@Composable
fun PetRoomFocusArrowScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // A non-white surface catches accidental opaque backgrounds in exported PNG assets.
            .background(colorResource(R.color.colors_FFE5C7)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_pet_room_focus_arrow),
            contentDescription = null,
            modifier = Modifier.size(
                width = dimensionResource(SdpR.dimen._43sdp),
                height = dimensionResource(SdpR.dimen._46sdp)
            )
        )
    }
}
