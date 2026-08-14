package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.pet.room.PetRoomFoodToastPill
import com.intuit.sdp.R as SdpR

@PreviewTest
@Preview(name = "Pet Store joined toast", widthDp = 360, heightDp = 120)
@Composable
fun PetStoreJoinedToastScreenshotTest() {
    AppActionToast(
        text = "Cattey has joined your home! 💕",
        action = "View",
        onDismiss = {},
        onAction = {},
        leadingImageModel = R.drawable.img_home_brand_bunny,
        bottomPaddingRes = SdpR.dimen._12sdp
    )
}

@PreviewTest
@Preview(name = "Pet Room out of food toast", widthDp = 360, heightDp = 96)
@Composable
fun PetRoomFoodToastScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        PetRoomFoodToastPill(text = "Out of food. Please add more food.")
    }
}
