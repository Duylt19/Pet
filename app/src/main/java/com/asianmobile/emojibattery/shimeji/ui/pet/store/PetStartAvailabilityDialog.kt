package com.asianmobile.emojibattery.shimeji.ui.pet.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun PetStartAvailabilityDialog(
    blocker: PetStartBlocker,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit
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
                PetStartAvailabilityDialogCard(
                    blocker = blocker,
                    onDismiss = onDismiss,
                    onPrimaryAction = onPrimaryAction
                )
            }
        }
    }
}

@Composable
private fun PetStartAvailabilityDialogCard(
    blocker: PetStartBlocker,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val titleRes = when (blocker) {
        PetStartBlocker.NO_OWNED_PETS -> R.string.pet_store_no_pets_title
        PetStartBlocker.NO_ACTIVE_PETS -> R.string.pet_store_no_active_pets_title
    }
    val bodyRes = when (blocker) {
        PetStartBlocker.NO_OWNED_PETS -> R.string.pet_store_no_pets_body
        PetStartBlocker.NO_ACTIVE_PETS -> R.string.pet_store_no_active_pets_body
    }
    val actionRes = when (blocker) {
        PetStartBlocker.NO_OWNED_PETS -> R.string.pet_store_browse_pets
        PetStartBlocker.NO_ACTIVE_PETS -> R.string.pet_store_open_my_pet
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(PET_START_DIALOG_WIDTH_FRACTION)
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
                text = stringResource(titleRes),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._16ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(bodyRes),
                color = colorResource(R.color.colors_6F7073),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._16ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
        ) {
            PetStartDialogButton(
                labelRes = R.string.pet_store_dialog_not_now,
                backgroundRes = R.color.colors_F2F2F2,
                labelColorRes = R.color.colors_6F7073,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            PetStartDialogButton(
                labelRes = actionRes,
                backgroundRes = R.color.colors_FB3675,
                labelColorRes = R.color.colors_FFFFFF,
                onClick = onPrimaryAction,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PetStartDialogButton(
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
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            textAlign = TextAlign.Center
        )
    }
}

private const val PET_START_DIALOG_WIDTH_FRACTION = 328f / 360f

@Preview(showBackground = true, widthDp = 360, heightDp = 440)
@Composable
private fun PetStartAvailabilityDialogPreview() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PetStartAvailabilityDialogCard(
            blocker = PetStartBlocker.NO_ACTIVE_PETS,
            onDismiss = {},
            onPrimaryAction = {}
        )
    }
}
