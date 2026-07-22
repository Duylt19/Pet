package com.asianmobile.privatebrower.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun BrowserMorePopup(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    isBookmarked: Boolean,
    canBookmark: Boolean,
    onDismiss: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onReloadClick: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onHistory: () -> Unit,
    onBookmarkPage: () -> Unit,
    onDownloads: () -> Unit,
    onFindInPage: () -> Unit,
    onShare: () -> Unit,
    onToggleDesktopSite: () -> Unit,
    onSettings: () -> Unit,
    onHelpFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontMedium = FontFamily(Font(R.font.inter_medium))

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = modifier.width(dimensionResource(SdpR.dimen._178sdp)),
        offset = DpOffset(
            x = 0.dp,
            y = -dimensionResource(SdpR.dimen._8sdp)
        ),
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ),
        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)),
        containerColor = colorResource(R.color.colors_FFFFFF),
        tonalElevation = 0.dp,
        shadowElevation = dimensionResource(SdpR.dimen._8sdp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(SdpR.dimen._9sdp))
        ) {
            // Top Row — Quick Action Icons
            TopActionRow(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isBookmarked = isBookmarked,
                canBookmark = canBookmark,
                onBackClick = {
                    onBackClick()
                    onDismiss()
                },
                onForwardClick = {
                    onForwardClick()
                    onDismiss()
                },
                onBookmarkClick = {
                    onBookmarkClick()
                    onDismiss()
                },
                onDownloadClick = {
                    onDownloadClick()
                    onDismiss()
                },
                onReloadClick = {
                    onReloadClick()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))

            // Menu Items (scrollable)
            Column(
                modifier = Modifier
                    .padding(vertical = dimensionResource(SdpR.dimen._2sdp)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
            ) {
                // New Tab
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_new_tab,
                    text = stringResource(R.string.browser_menu_new_tab),
                    fontFamily = fontMedium,
                    onClick = {
                        onNewTab()
                        onDismiss()
                    }
                )

                // New Incognito Tab
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_incognito,
                    text = stringResource(R.string.browser_menu_new_incognito_tab),
                    fontFamily = fontMedium,
                    onClick = {
                        onNewIncognitoTab()
                        onDismiss()
                    }
                )

                // Divider
                PopupDivider()

                // History
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_history,
                    text = stringResource(R.string.browser_menu_history),
                    fontFamily = fontMedium,
                    onClick = {
                        onHistory()
                        onDismiss()
                    }
                )

                // Bookmark
                PopupMenuItem(
                    iconRes = if (isBookmarked) {
                        R.drawable.ic_star_filled
                    } else {
                        R.drawable.ic_menu_star
                    },
                    text = stringResource(
                        if (isBookmarked) {
                            R.string.browser_remove_bookmark
                        } else {
                            R.string.browser_add_bookmark
                        }
                    ),
                    fontFamily = fontMedium,
                    isEnabled = canBookmark,
                    isSelected = isBookmarked,
                    onClick = {
                        onBookmarkPage()
                        onDismiss()
                    }
                )

                // Downloads
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_downloads_item,
                    text = stringResource(R.string.browser_menu_downloads),
                    fontFamily = fontMedium,
                    onClick = {
                        onDownloads()
                        onDismiss()
                    }
                )

                // Divider
                PopupDivider()

                // Find in Page
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_search,
                    text = stringResource(R.string.browser_menu_find_in_page),
                    fontFamily = fontMedium,
                    onClick = {
                        onFindInPage()
                        onDismiss()
                    }
                )

                // Share
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_share,
                    text = stringResource(R.string.browser_menu_share),
                    fontFamily = fontMedium,
                    onClick = {
                        onShare()
                        onDismiss()
                    }
                )

                // Desktop Site (with checkbox)
                DesktopSiteMenuItem(
                    isChecked = isDesktopMode,
                    fontFamily = fontMedium,
                    onClick = {
                        onToggleDesktopSite()
                        onDismiss()
                    }
                )

                // Divider
                PopupDivider()

                // Settings
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_settings,
                    text = stringResource(R.string.browser_menu_settings),
                    fontFamily = fontMedium,
                    onClick = {
                        onSettings()
                        onDismiss()
                    }
                )

                // Help & Feedback
                PopupMenuItem(
                    iconRes = R.drawable.ic_menu_help,
                    text = stringResource(R.string.browser_menu_help_feedback),
                    fontFamily = fontMedium,
                    onClick = {
                        onHelpFeedback()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun TopActionRow(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    canBookmark: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onReloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopActionIcon(
            iconRes = R.drawable.ic_menu_arrow_left,
            contentDescription = stringResource(R.string.browser_back),
            isEnabled = canGoBack,
            onClick = onBackClick
        )
        TopActionIcon(
            iconRes = R.drawable.ic_menu_arrow_right,
            contentDescription = stringResource(R.string.browser_forward),
            isEnabled = canGoForward,
            onClick = onForwardClick
        )
        TopActionIcon(
            iconRes = if (isBookmarked) R.drawable.ic_star_filled else R.drawable.ic_menu_star,
            contentDescription = stringResource(
                if (isBookmarked) R.string.browser_remove_bookmark else R.string.browser_add_bookmark
            ),
            isEnabled = canBookmark,
            isSelected = isBookmarked,
            onClick = onBookmarkClick
        )
        TopActionIcon(
            iconRes = R.drawable.ic_menu_download,
            contentDescription = stringResource(R.string.browser_menu_download_action),
            isEnabled = true,
            onClick = onDownloadClick
        )
        TopActionIcon(
            iconRes = R.drawable.ic_menu_reload,
            contentDescription = stringResource(R.string.browser_menu_reload),
            isEnabled = true,
            onClick = onReloadClick
        )
    }
}

@Composable
private fun TopActionIcon(
    iconRes: Int,
    contentDescription: String,
    isEnabled: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = when {
        !isEnabled -> colorResource(R.color.colors_B3B3B3)
        isSelected -> colorResource(R.color.colors_FFFFFF)
        else -> colorResource(R.color.colors_005DFD)
    }
    val backgroundColor = if (isSelected) {
        colorResource(R.color.colors_005DFD)
    } else {
        colorResource(R.color.colors_EBF0FF)
    }

    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._25sdp))
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )
    }
}

@Composable
private fun PopupMenuItem(
    iconRes: Int,
    text: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = when {
        !isEnabled -> colorResource(R.color.colors_B3B3B3)
        isSelected -> colorResource(R.color.colors_005DFD)
        else -> colorResource(R.color.colors_0D0D0D)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(SdpR.dimen._40sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(
                vertical = dimensionResource(SdpR.dimen._9sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            },
            color = contentColor
        )
    }
}

@Composable
private fun DesktopSiteMenuItem(
    isChecked: Boolean,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(SdpR.dimen._40sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu_desktop),
            contentDescription = stringResource(R.string.browser_menu_desktop_site),
            tint = colorResource(R.color.colors_0D0D0D),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
        )

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

        Text(
            text = stringResource(R.string.browser_menu_desktop_site),
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            },
            color = colorResource(R.color.colors_0D0D0D),
            modifier = Modifier.weight(1f)
        )

        // Custom checkbox: the Material3 Checkbox has a fixed intrinsic box + a 48dp minimum
        // interactive size, so forcing it to _15sdp clipped its right edge. This draws exactly
        // at the intended size.
        val checkShape = RoundedCornerShape(dimensionResource(SdpR.dimen._4sdp))
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._16sdp))
                .clip(checkShape)
                .background(
                    if (isChecked) colorResource(R.color.colors_005DFD) else Color.Transparent
                )
                .border(
                    width = 1.5.dp,
                    color = colorResource(
                        if (isChecked) R.color.colors_005DFD else R.color.colors_B3B3B3
                    ),
                    shape = checkShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_white),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._10sdp))
                )
            }
        }
    }
}

@Composable
private fun PopupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = dimensionResource(SdpR.dimen._2sdp)),
        thickness = 1.dp,
        color = colorResource(R.color.colors_E6E6E6)
    )
}
