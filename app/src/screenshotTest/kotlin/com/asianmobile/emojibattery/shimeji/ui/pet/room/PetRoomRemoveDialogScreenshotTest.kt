package com.asianmobile.emojibattery.shimeji.ui.pet.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R

@PreviewTest
@Preview(name = "Delete pet dialog", widthDp = 360, heightDp = 240)
@Composable
fun PetRoomRemoveDialogScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_6F7073)),
        contentAlignment = Alignment.Center
    ) {
        PetRoomRemoveDialogCard(
            pet = PetRoomPetUiState(
                petId = 1,
                packKey = "pack-1",
                name = "Catty",
                breed = "Cat",
                thumbnailPath = null
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
