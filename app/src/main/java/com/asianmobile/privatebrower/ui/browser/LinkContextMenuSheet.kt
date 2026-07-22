package com.asianmobile.privatebrower.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R

/**
 * Long-press context menu for an image and/or link: save the image/file, or copy addresses.
 * Kept intentionally small — a column of tappable rows in a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    info: LinkContextInfo,
    onSaveImage: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorResource(R.color.colors_161920),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            info.imageUrl?.let { image ->
                Text(
                    text = image,
                    color = colorResource(R.color.gray_808080),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                )
                MenuRow(
                    iconRes = R.drawable.ic_download_arrow,
                    label = stringResource(R.string.browser_ctx_save_image),
                ) { onSaveImage(image) }
                MenuRow(
                    iconRes = R.drawable.ic_tab_copy,
                    label = stringResource(R.string.browser_ctx_copy_image),
                ) { onCopy(image) }
            }
            info.linkUrl?.let { link ->
                MenuRow(
                    iconRes = R.drawable.ic_tab_copy,
                    label = stringResource(R.string.browser_ctx_copy_link),
                ) { onCopy(link) }
            }
        }
    }
}

@Composable
private fun MenuRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(R.color.colors_FFFFFF),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = colorResource(R.color.colors_FFFFFF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
