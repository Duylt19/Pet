package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR

/** Brand-styled replacement for platform Material message dialogs. */
@Composable
fun AppMessageDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(SdpR.dimen._28sdp)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)),
                color = colorResource(R.color.colors_FFFFFF),
                shadowElevation = dimensionResource(SdpR.dimen._8sdp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = dimensionResource(SdpR.dimen._18sdp),
                        top = dimensionResource(SdpR.dimen._18sdp),
                        end = dimensionResource(SdpR.dimen._18sdp),
                        bottom = dimensionResource(SdpR.dimen._8sdp)
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(SdpR.dimen._9sdp)
                    )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(R.color.colors_212327),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(R.color.colors_6F7073)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorResource(R.color.colors_FB3675)
                            )
                        ) {
                            Text(
                                text = confirmText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
