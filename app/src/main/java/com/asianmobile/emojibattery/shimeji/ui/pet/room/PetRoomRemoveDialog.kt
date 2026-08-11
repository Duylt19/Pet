package com.asianmobile.emojibattery.shimeji.ui.pet.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.component.DismissibleDialogBackdrop
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/**
 * Removing a pet deletes its pack and its energy, so it asks first. The layout follows the
 * permission dialog the app already uses, so confirmations look the same everywhere.
 */
@Composable
fun PetRoomRemoveDialog(
    pet: PetRoomPetUiState,
    onConfirm: () -> Unit,
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
                PetRoomRemoveDialogCard(
                    pet = pet,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
internal fun PetRoomRemoveDialogCard(
    pet: PetRoomPetUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(REMOVE_DIALOG_WIDTH_FRACTION)
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._18sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        pet.thumbnailPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._63sdp))
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Text(
                text = stringResource(R.string.pet_room_remove_title),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._21ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.pet_room_remove_message, pet.name),
                color = colorResource(R.color.colors_6F7073),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            RemoveDialogButton(
                labelRes = R.string.pet_room_remove_cancel,
                backgroundRes = R.color.colors_F2F2F2,
                labelColorRes = R.color.colors_6F7073,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            RemoveDialogButton(
                labelRes = R.string.pet_room_remove_confirm,
                backgroundRes = R.color.colors_FB3675,
                labelColorRes = R.color.colors_FFFFFF,
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RemoveDialogButton(
    labelRes: Int,
    backgroundRes: Int,
    labelColorRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(CircleShape)
            .background(colorResource(backgroundRes))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(labelRes),
            color = colorResource(labelColorRes),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
        )
    }
}

private const val REMOVE_DIALOG_WIDTH_FRACTION = 328f / 360f

@Preview(showBackground = true, backgroundColor = 0xFF9B9C9E, widthDp = 360, heightDp = 440)
@Composable
private fun PetRoomRemoveDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        contentAlignment = Alignment.Center
    ) {
        PetRoomRemoveDialogCard(
            pet = PetRoomPetUiState(
                petId = 1,
                packKey = "pack-1",
                name = "Cattey",
                breed = "Cat",
                thumbnailPath = null
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
