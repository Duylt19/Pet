package com.asianmobile.privatebrower.ui.home.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.usecase.ClearBrowsingDataOptions
import com.asianmobile.privatebrower.data.usecase.BrowsingDataScope
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearHistorySheet(
    options: ClearBrowsingDataOptions,
    isClearing: Boolean,
    profileIsolationSupported: Boolean,
    onOptionsChanged: (ClearBrowsingDataOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val confirmEnabled = options.hasSelection && !isClearing

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = dimensionResource(SdpR.dimen._24sdp),
            topEnd = dimensionResource(SdpR.dimen._24sdp)
        ),
        containerColor = colorResource(R.color.colors_212327),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._9sdp),
                    vertical = dimensionResource(SdpR.dimen._15sdp)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            Text(
                text = stringResource(R.string.settings_clear_history_title),
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                color = colorResource(R.color.colors_333538),
                thickness = dimensionResource(SdpR.dimen._1sdp)
            )

            ClearHistoryScopeSelector(
                selected = options.scope,
                enabled = !isClearing,
                onSelected = { onOptionsChanged(options.forScope(it)) }
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
            ) {
                if (options.scope != BrowsingDataScope.PRIVATE) {
                    ClearHistoryOptionRow(
                        label = R.string.clear_history_site_data,
                        checked = options.clearCookies,
                        enabled = !isClearing,
                        onCheckedChange = {
                            onOptionsChanged(options.copy(clearCookies = it))
                        }
                    )
                }
                ClearHistoryOptionRow(
                    label = R.string.clear_history_open_tabs,
                    checked = options.clearOpenTabs,
                    enabled = !isClearing,
                    onCheckedChange = {
                        onOptionsChanged(options.copy(clearOpenTabs = it))
                    }
                )
                if (options.scope != BrowsingDataScope.PRIVATE) {
                    ClearHistoryOptionRow(
                        label = R.string.clear_history_history,
                        checked = options.clearHistory,
                        enabled = !isClearing,
                        onCheckedChange = {
                            onOptionsChanged(options.copy(clearHistory = it))
                        }
                    )
                }
                if (options.scope != BrowsingDataScope.PRIVATE) {
                    ClearHistoryOptionRow(
                        label = R.string.clear_history_cache,
                        checked = options.clearCache,
                        enabled = !isClearing,
                        onCheckedChange = {
                            onOptionsChanged(options.copy(clearCache = it))
                        }
                    )
                }
            }

            if (
                options.clearCookies &&
                options.scope != BrowsingDataScope.NORMAL &&
                profileIsolationSupported
            ) {
                ClearHistoryNote(text = stringResource(R.string.clear_history_private_tabs_note))
            }
            if (
                options.clearCookies &&
                options.scope != BrowsingDataScope.ALL &&
                !profileIsolationSupported
            ) {
                ClearHistoryNote(text = stringResource(R.string.clear_history_profile_limited_note))
            }
            if (options.hasSharedCacheScope) {
                ClearHistoryNote(text = stringResource(R.string.clear_history_cache_shared_note))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
            ) {
                ClearHistoryButton(
                    label = R.string.common_cancel_label,
                    backgroundColor = R.color.colors_424447,
                    enabled = !isClearing,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                ClearHistoryButton(
                    label = R.string.clear_history_confirm,
                    backgroundColor = R.color.colors_3369FD,
                    enabled = confirmEnabled,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClearHistoryScopeSelector(
    selected: BrowsingDataScope,
    enabled: Boolean,
    onSelected: (BrowsingDataScope) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(R.color.colors_333538),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
            )
            .padding(dimensionResource(SdpR.dimen._3sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
    ) {
        BrowsingDataScope.entries.forEach { scope ->
            val label = when (scope) {
                BrowsingDataScope.NORMAL -> R.string.clear_history_scope_normal
                BrowsingDataScope.PRIVATE -> R.string.clear_history_scope_private
                BrowsingDataScope.ALL -> R.string.clear_history_scope_all
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(SdpR.dimen._31sdp))
                    .background(
                        color = if (selected == scope) {
                            colorResource(R.color.colors_424447)
                        } else {
                            colorResource(R.color.transparent)
                        },
                        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._7sdp))
                    )
                    .clickable(enabled = enabled) { onSelected(scope) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(label),
                    color = if (selected == scope) {
                        colorResource(R.color.colors_FFFFFF)
                    } else {
                        colorResource(R.color.colors_9B9C9E)
                    },
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                )
            }
        }
    }
}

@Composable
private fun ClearHistoryNote(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = colorResource(R.color.colors_9B9C9E),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
    )
}

@Composable
private fun ClearHistoryOptionRow(
    label: Int,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._40sdp))
            .background(
                color = colorResource(R.color.colors_333538),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
            )
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(label),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
        )
        ClearHistoryCheckbox(checked = checked)
    }
}

@Composable
private fun ClearHistoryCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp))
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .background(
                color = if (checked) {
                    colorResource(R.color.colors_3369FD)
                } else {
                    colorResource(R.color.transparent)
                },
                shape = shape
            )
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = if (checked) {
                    colorResource(R.color.colors_3369FD)
                } else {
                    colorResource(R.color.colors_6F7073)
                },
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check_white),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._14sdp))
            )
        }
    }
}

@Composable
private fun ClearHistoryButton(
    label: Int,
    backgroundColor: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .alpha(if (enabled) 1f else 0.5f)
            .background(
                color = colorResource(backgroundColor),
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(label),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
        )
    }
}
