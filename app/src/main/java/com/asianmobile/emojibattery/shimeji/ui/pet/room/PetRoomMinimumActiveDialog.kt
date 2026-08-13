package com.asianmobile.emojibattery.shimeji.ui.pet.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.component.DismissibleDialogBackdrop
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PetRoomMinimumActiveDialog(
    petName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DismissibleDialogBackdrop(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
                contentAlignment = Alignment.Center
            ) {
                PetRoomMinimumActiveDialogCard(
                    petName = petName,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
internal fun PetRoomMinimumActiveDialogCard(
    petName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(MINIMUM_ACTIVE_DIALOG_WIDTH_FRACTION)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._14sdp),
                vertical = dimensionResource(SdpR.dimen._18sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._5sdp))
        ) {
            Text(
                text = stringResource(R.string.pet_room_minimum_active_title),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._16ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.pet_room_minimum_active_message, petName),
                color = colorResource(R.color.colors_6F7073),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._16ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FB3675))
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.pet_room_minimum_active_confirm),
                color = colorResource(R.color.colors_FFFFFF),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
            )
        }
    }
}

private const val MINIMUM_ACTIVE_DIALOG_WIDTH_FRACTION = 328f / 360f

@Preview(showBackground = true, backgroundColor = 0xFF9B9C9E, widthDp = 360, heightDp = 440)
@Composable
private fun PetRoomMinimumActiveDialogPreview() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PetRoomMinimumActiveDialogCard(
            petName = "Cattey",
            onDismiss = {}
        )
    }
}
