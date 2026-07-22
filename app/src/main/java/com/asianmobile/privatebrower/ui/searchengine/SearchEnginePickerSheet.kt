package com.asianmobile.privatebrower.ui.searchengine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.asianmobile.privatebrower.data.model.SearchEngine
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEnginePickerSheet(
    selected: SearchEngine,
    onSelect: (SearchEngine) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    horizontal = dimensionResource(SdpR.dimen._15sdp),
                    vertical = dimensionResource(SdpR.dimen._15sdp)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            Text(
                text = stringResource(R.string.settings_default_browser_title),
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

            Column(modifier = Modifier.fillMaxWidth()) {
                SearchEngine.entries.forEachIndexed { index, engine ->
                    SearchEngineRow(
                        engine = engine,
                        isSelected = engine == selected,
                        onClick = { onSelect(engine) }
                    )
                    if (index < SearchEngine.entries.lastIndex) {
                        HorizontalDivider(
                            color = colorResource(R.color.colors_333538),
                            thickness = dimensionResource(SdpR.dimen._1sdp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEngineRow(
    engine: SearchEngine,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._31sdp))
                .background(
                    color = colorResource(R.color.colors_FFFFFF),
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(engine.iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._2sdp))
        ) {
            Text(
                text = engine.displayName,
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_medium)),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
            )
            Text(
                text = engine.homeUrl,
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
        }

        SearchEngineRadio(selected = isSelected)
    }
}

@Composable
private fun SearchEngineRadio(selected: Boolean) {
    val selectedColor = colorResource(R.color.colors_3369FD)
    val unselectedColor = colorResource(R.color.colors_6F7073)
    Box(
        modifier = Modifier
            .size(dimensionResource(SdpR.dimen._18sdp))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = if (selected) selectedColor else unselectedColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._10sdp))
                    .background(selectedColor, CircleShape)
            )
        }
    }
}
