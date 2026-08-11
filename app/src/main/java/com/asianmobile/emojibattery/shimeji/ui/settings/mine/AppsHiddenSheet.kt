package com.asianmobile.emojibattery.shimeji.ui.settings.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppSwitch
import com.asianmobile.emojibattery.shimeji.ui.shared.component.HideDialogNavigationBar
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

private val AppsHiddenRobotoMedium = FontFamily(Font(R.font.roboto_medium))

@Composable
fun AppsHiddenSheet(
    state: SettingsUiState,
    onToggleApp: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        HideDialogNavigationBar()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AppsHiddenSheetContent(
                apps = state.installedApps,
                isLoading = state.isInstalledAppsLoading,
                loadFailed = state.installedAppsLoadFailed,
                onToggleApp = onToggleApp,
                onRetry = onRetry,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
            )
        }
    }
}

@Composable
internal fun AppsHiddenSheetContent(
    apps: List<InstalledAppUiState>,
    isLoading: Boolean,
    loadFailed: Boolean,
    onToggleApp: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(726f / 800f)
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Box(
            modifier = Modifier
                .size(
                    width = dimensionResource(SdpR.dimen._25sdp),
                    height = dimensionResource(SdpR.dimen._3sdp)
                )
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._2sdp)))
                .background(colorResource(R.color.colors_C8C8C9))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp),
                    vertical = dimensionResource(SdpR.dimen._12sdp)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Text(
                text = stringResource(R.string.mine_apps_hidden_title),
                color = colorResource(R.color.colors_212327),
                fontFamily = AppsHiddenRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
            )
            Text(
                text = stringResource(R.string.mine_apps_hidden_subtitle),
                color = colorResource(R.color.colors_6F7073),
                fontFamily = RobotoFontFamily,
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp
            )
        }

        when {
            isLoading -> AppsHiddenCenteredMessage(modifier = Modifier.weight(1f)) {
                CircularProgressIndicator(
                    color = colorResource(R.color.colors_FB3675),
                    strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._24sdp))
                )
            }
            loadFailed -> AppsHiddenCenteredMessage(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.mine_apps_hidden_load_failed),
                    color = colorResource(R.color.colors_6F7073),
                    fontFamily = RobotoFontFamily,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.mine_apps_hidden_retry),
                    color = colorResource(R.color.colors_FB3675),
                    fontFamily = AppsHiddenRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                        .clickable(onClick = onRetry)
                        .padding(dimensionResource(SdpR.dimen._9sdp))
                )
            }
            apps.isEmpty() -> AppsHiddenCenteredMessage(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.mine_apps_hidden_empty),
                    color = colorResource(R.color.colors_6F7073),
                    fontFamily = RobotoFontFamily,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
            ) {
                items(items = apps, key = InstalledAppUiState::packageName) { app ->
                    AppsHiddenRow(app = app, onToggle = { onToggleApp(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun AppsHiddenCenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun AppsHiddenRow(
    app: InstalledAppUiState,
    onToggle: () -> Unit
) {
    val switchDescription = stringResource(
        R.string.mine_apps_hidden_switch_content_description,
        app.label
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(SdpR.dimen._31sdp))
                .semantics { contentDescription = switchDescription }
                .toggleable(
                    value = app.isHidden,
                    role = Role.Switch,
                    onValueChange = { onToggle() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._31sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._31sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                        .background(colorResource(R.color.colors_FFEBF1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = colorResource(R.color.colors_FB3675),
                        fontFamily = AppsHiddenRobotoMedium,
                        fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
                    )
                }
            }
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
            Text(
                text = app.label,
                color = colorResource(R.color.colors_212327),
                fontFamily = AppsHiddenRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            AppSwitch(
                checked = app.isHidden,
                onCheckedChange = onToggle,
                interactive = false
            )
        }
        HorizontalDivider(
            color = colorResource(R.color.colors_E6E6E6),
            modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._9sdp))
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AppsHiddenSheetContentPreview() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppsHiddenSheetContent(
            apps = listOf(
                InstalledAppUiState("com.facebook.katana", "Facebook", null, true),
                InstalledAppUiState("com.instagram.android", "Instagram", null, false),
                InstalledAppUiState("com.google.android.youtube", "YouTube", null, true)
            ),
            isLoading = false,
            loadFailed = false,
            onToggleApp = {},
            onRetry = {}
        )
    }
}
