package com.asianmobile.privatebrower.utils.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R

@Preview
@Composable
fun PermissionRequireDialog(
    title: String = stringResource(R.string.permission_required),
    message: String = stringResource(R.string.this_feature_requires_permissions_to_function_please_grant_permissions_in_app_settings),
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = colorResource(R.color.white),
        title = {
            Text(
                text = title,
                fontFamily = FontFamily(Font(R.font.be_vietnam_pro_medium)),
                fontSize = dimensionResource(id = com.intuit.ssp.R.dimen._15ssp).value.sp,
                color = colorResource(R.color.blue_007BFD),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                fontFamily = FontFamily(Font(R.font.be_vietnam_pro_regular)),
                fontSize = dimensionResource(id = com.intuit.ssp.R.dimen._12ssp).value.sp,
                color = colorResource(R.color.gray_4D4D4D)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings),
                    fontFamily = FontFamily(Font(R.font.be_vietnam_pro_regular)),
                    fontSize = dimensionResource(id = com.intuit.ssp.R.dimen._13ssp).value.sp,
                    color = colorResource(R.color.blue_007BFD)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = FontFamily(Font(R.font.be_vietnam_pro_regular)),
                    fontSize = dimensionResource(id = com.intuit.ssp.R.dimen._13ssp).value.sp,
                    color = colorResource(R.color.gray_4D4D4D)
                )
            }
        }
    )
}

