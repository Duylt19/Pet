package com.asianmobile.emojibattery.shimeji.ui.pet

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
fun PetFamilyCapacityDialog(
    onDismiss: () -> Unit,
    onManagePets: (() -> Unit)? = null
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
                PetFamilyCapacityDialogCard(
                    onDismiss = onDismiss,
                    onManagePets = onManagePets
                )
            }
        }
    }
}

@Composable
private fun PetFamilyCapacityDialogCard(
    onDismiss: () -> Unit,
    onManagePets: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(PET_CAPACITY_DIALOG_WIDTH_FRACTION)
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
                text = stringResource(R.string.pet_family_full_title),
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(SspR.dimen._16ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    R.string.pet_family_full_message,
                    PetFamilyCapacityPolicy.MAX_OWNED_PETS
                ),
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
            if (onManagePets != null) {
                CapacityDialogButton(
                    labelRes = R.string.pet_family_full_not_now,
                    backgroundRes = R.color.colors_F2F2F2,
                    labelColorRes = R.color.colors_6F7073,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                CapacityDialogButton(
                    labelRes = R.string.pet_family_full_manage,
                    backgroundRes = R.color.colors_FB3675,
                    labelColorRes = R.color.colors_FFFFFF,
                    onClick = onManagePets,
                    modifier = Modifier.weight(1f)
                )
            } else {
                CapacityDialogButton(
                    labelRes = R.string.pet_family_full_confirm,
                    backgroundRes = R.color.colors_FB3675,
                    labelColorRes = R.color.colors_FFFFFF,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CapacityDialogButton(
    labelRes: Int,
    backgroundRes: Int,
    labelColorRes: Int,
    onClick: () -> Unit,
    modifier: Modifier
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

private const val PET_CAPACITY_DIALOG_WIDTH_FRACTION = 328f / 360f

@Preview(showBackground = true, backgroundColor = 0xFF9B9C9E, widthDp = 360, heightDp = 440)
@Composable
private fun PetFamilyCapacityDialogPreview() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PetFamilyCapacityDialogCard(onDismiss = {}, onManagePets = {})
    }
}
