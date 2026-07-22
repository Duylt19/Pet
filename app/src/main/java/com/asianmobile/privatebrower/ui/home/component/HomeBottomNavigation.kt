package com.asianmobile.privatebrower.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as R_sdp
import com.intuit.ssp.R as R_ssp

/**
 * Home Bottom Navigation Bar — Figma component "Bottom navigation"
 * 5 tabs: Home, Tab, Downloads, Bookmark, Folder
 *
 * Figma specs:
 * - Container: Row, fill #161718, stroke top #212327 1px, height hug
 * - Each tab item: Column, center-aligned, gap 4px (~3sdp), size 72×64 (~55×49 sdp)
 * - Icon: 24×24 (~18sdp)
 * - Text unselected: Inter Medium 500, 10px/14px (~8ssp), color #9B9C9E
 * - Text selected: Inter SemiBold 600, 10px/14px (~8ssp), color #FFFFFF
 * - Selected indicator: Rectangle 72×3 (~55×2 sdp), fill #FFFFFF, borderRadius 0 0 16 16
 *   positioned absolute at top-left of tab item
 * - Tab icon: copy-outline with dynamic tab count number overlaid
 *   - Unselected: icon tint #9B9C9E, number fill #9B9C9E
 *   - Selected: icon tint #FFFFFF, number fill #212327
 */
@Composable
fun HomeBottomNavBar(
    selectedTab: Int,
    tabCount: Int,
    onTabClick: (Int) -> Unit
) {
    val bgColor = colorResource(R.color.colors_161718)
    val borderColor = colorResource(R.color.colors_212327)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Top border line — 1px stroke #212327
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = listOf(
                TabInfo(
                    title = stringResource(R.string.tab_home),
                    iconDefault = R.drawable.ic_tab_home,
                    iconSelected = R.drawable.ic_tab_home_bold,
                    isTabIcon = false
                ),
                TabInfo(
                    title = stringResource(R.string.tab_tab),
                    iconDefault = R.drawable.ic_tab_copy,
                    iconSelected = R.drawable.ic_tab_copy_bold,
                    isTabIcon = true
                ),
                TabInfo(
                    title = stringResource(R.string.tab_progress),
                    iconDefault = R.drawable.ic_tab_download_circle,
                    iconSelected = R.drawable.ic_tab_download_circle_bold,
                    isTabIcon = false
                ),
                TabInfo(
                    title = stringResource(R.string.tab_bookmark),
                    iconDefault = R.drawable.ic_tab_bookmark,
                    iconSelected = R.drawable.ic_tab_bookmark_bold,
                    isTabIcon = false
                ),
                TabInfo(
                    title = stringResource(R.string.tab_folder),
                    iconDefault = R.drawable.ic_tab_folder,
                    iconSelected = R.drawable.ic_tab_folder_bold,
                    isTabIcon = false
                )
            )

            tabs.forEachIndexed { index, tab ->
                BottomNavItem(
                    title = tab.title,
                    iconDefault = tab.iconDefault,
                    iconSelected = tab.iconSelected,
                    isSelected = selectedTab == index,
                    isTabIcon = tab.isTabIcon,
                    tabCount = tabCount,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabClick(index) }
                        )
                )
            }
        }
    }
}

private data class TabInfo(
    val title: String,
    val iconDefault: Int,
    val iconSelected: Int,
    val isTabIcon: Boolean
)

@Composable
private fun BottomNavItem(
    title: String,
    iconDefault: Int,
    iconSelected: Int,
    isSelected: Boolean,
    isTabIcon: Boolean,
    tabCount: Int,
    modifier: Modifier = Modifier
) {
    val activeColor = colorResource(R.color.colors_FFFFFF)
    val inactiveColor = colorResource(R.color.colors_9B9C9E)
    val contentColor = if (isSelected) activeColor else inactiveColor
    val icon = if (isSelected) iconSelected else iconDefault
    val fontFamily = if (isSelected) {
        FontFamily(Font(R.font.inter_semibold))
    } else {
        FontFamily(Font(R.font.inter_medium))
    }
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

    // Figma: each tab item is 72×64 (~55×49 sdp), column, center, gap 4px (~3sdp)
    Box(
        modifier = modifier.height(dimensionResource(R_sdp.dimen._49sdp)),
        contentAlignment = Alignment.Center
    ) {
        // Selected indicator — absolute top, 72×3 (~55×2 sdp), borderRadius 0 0 16 16
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(dimensionResource(R_sdp.dimen._55sdp))
                    .height(dimensionResource(R_sdp.dimen._2sdp))
                    .background(
                        color = activeColor,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = dimensionResource(R_sdp.dimen._12sdp),
                            bottomEnd = dimensionResource(R_sdp.dimen._12sdp)
                        )
                    )
            )
        }

        // Icon + label column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R_sdp.dimen._3sdp))
        ) {
            if (isTabIcon) {
                // Tab icon: copy-outline + dynamic tab count number overlay
                TabIconWithCount(
                    iconRes = icon,
                    count = tabCount,
                    isSelected = isSelected
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(dimensionResource(R_sdp.dimen._18sdp))
                )
            }

            Text(
                text = title,
                fontSize = dimensionResource(R_ssp.dimen._8ssp).value.sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                color = contentColor
            )
        }
    }
}

/**
 * Tab icon with dynamic tab count number overlay.
 *
 * Figma layout (24×24 viewport):
 * - Copy outline stroke at full icon size
 * - Number text positioned at center of front rectangle
 *   (front rect: x=2.25→18, y=6→21.75, center ≈ 10.1, 13.9)
 *
 * Color behavior:
 * - Unselected: icon tint #9B9C9E, number fill #9B9C9E
 * - Selected: icon tint #FFFFFF, number fill #212327 (dark on white)
 */
@Composable
private fun TabIconWithCount(
    iconRes: Int,
    count: Int,
    isSelected: Boolean
) {
    // Figma: selected number is dark (#212327) on white-filled icon,
    // unselected number is grey (#9B9C9E) on transparent outline icon
    val numberColor = if (isSelected) {
        colorResource(R.color.colors_212327)
    } else {
        colorResource(R.color.colors_9B9C9E)
    }

    val iconSize = dimensionResource(R_sdp.dimen._18sdp)

    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        // Copy icon — colors baked in XML (grey stroke for unselected, white fill for selected)
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize)
        )

        // Dynamic tab count number — centered in front rectangle
        // Figma front rect: (2.25,6)→(18,21.75) in 24×24 viewport
        // Center: (10.125, 13.875), icon center: (12, 12)
        // Offset: (-1.875, +1.875) × (18/24 scale) = (-1.4dp, +1.4dp)
        Text(
            text = count.toString(),
            fontSize = dimensionResource(R_ssp.dimen._7ssp).value.sp,
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontWeight = FontWeight.SemiBold,
            color = numberColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(
                x = (-1.75).dp,
                y = 1.5.dp
            )
        )
    }
}
