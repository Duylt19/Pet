package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

enum class HomeTab {
    DISCOVER,
    BATTERY,
    PET_STORE,
    MINE
}

private val HomeRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val HomeRobotoSemiBold = FontFamily(Font(R.font.roboto_600))

@Composable
fun HomeHeader(
    onSearch: () -> Unit,
    onPremium: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.discover_brand_name),
            color = colorResource(R.color.colors_212327),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            fontSize = dimensionResource(SspR.dimen._17ssp).value.sp,
            maxLines = 1
        )
        PinkLoveSticker(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._25sdp))
                .scale(1.3f)
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._25sdp))
                .shadow(dimensionResource(SdpR.dimen._6sdp), CircleShape)
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_home_search),
                contentDescription = stringResource(R.string.discover_search),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
            )
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
        Row(
            modifier = Modifier
                .shadow(dimensionResource(SdpR.dimen._6sdp), CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_FFB65B),
                            colorResource(R.color.colors_FF6B80),
                            colorResource(R.color.colors_FF57EE)
                        )
                    )
                )
                .clickable(onClick = onPremium)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._6sdp),
                    vertical = dimensionResource(SdpR.dimen._5sdp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Image(
                painter = painterResource(R.drawable.img_home_crown),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
            )
            Text(
                text = stringResource(R.string.discover_pro),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = HomeRobotoSemiBold,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
        }
    }
}

@Composable
fun HomeEnableCard(
    text: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    val pink = colorResource(R.color.colors_FB3675)
    val violet = colorResource(R.color.colors_C95DFF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._9sdp)
            )
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(pink.copy(alpha = 0.12f), violet.copy(alpha = 0.12f))
                )
            )
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                brush = Brush.horizontalGradient(listOf(violet, pink)),
                shape = shape
            )
            .then(
                // The switch keeps its own hit area; the rest of the card is the entry point.
                if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
            )
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = colorResource(R.color.colors_212327),
            fontFamily = HomeRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun HomeBottomNavigation(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimensionResource(SdpR.dimen._6sdp))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(horizontal = dimensionResource(SdpR.dimen._6sdp))
    ) {
        HomeBottomItem(HomeTab.DISCOVER, R.drawable.ic_home_discover_unselected, stringResource(R.string.discover_tab_discover), selectedTab, onTabSelected, Modifier.weight(1f), R.drawable.ic_home_discover)
        HomeBottomItem(HomeTab.BATTERY, R.drawable.ic_home_battery, stringResource(R.string.discover_tab_battery), selectedTab, onTabSelected, Modifier.weight(1f))
        HomeBottomItem(HomeTab.PET_STORE, R.drawable.ic_home_pet_store, stringResource(R.string.discover_tab_pet_store), selectedTab, onTabSelected, Modifier.weight(1f), R.drawable.ic_home_pet_store_selected)
        HomeBottomItem(
            HomeTab.MINE,
            R.drawable.ic_home_mine,
            stringResource(R.string.discover_tab_mine),
            selectedTab,
            onTabSelected,
            Modifier.weight(1f),
            R.drawable.ic_home_mine_selected
        )
    }
}

@Composable
private fun HomeBottomItem(
    tab: HomeTab,
    iconRes: Int,
    label: String,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier,
    selectedIconRes: Int = iconRes
) {
    val selected = tab == selectedTab
    Column(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._62sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
            .clickable(enabled = !selected) { onTabSelected(tab) }
            .padding(
                top = dimensionResource(SdpR.dimen._9sdp),
                bottom = dimensionResource(SdpR.dimen._12sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    colorResource(
                        if (selected) R.color.colors_FFEBF1 else R.color.colors_FFFFFF
                    )
                )
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._15sdp),
                    vertical = dimensionResource(SdpR.dimen._3sdp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (selected) selectedIconRes else iconRes),
                contentDescription = label,
                tint = colorResource(
                    if (selected) R.color.colors_FB3675 else R.color.colors_6F7073
                ),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
            )
        }
        Text(
            text = label,
            color = colorResource(
                if (selected) R.color.colors_FB3675 else R.color.colors_6F7073
            ),
            fontFamily = if (selected) HomeRobotoSemiBold else HomeRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            maxLines = 1
        )
    }
}
