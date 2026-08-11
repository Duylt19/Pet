package com.asianmobile.emojibattery.shimeji.ui.pet.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackSource
import com.asianmobile.emojibattery.shimeji.ui.shared.component.CutePetCard
import com.asianmobile.emojibattery.shimeji.ui.shared.component.CutePetPrimaryButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.CutePetTitleFont
import com.asianmobile.emojibattery.shimeji.ui.shared.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PetDetailScreen(
    packKey: String,
    onBack: () -> Unit,
    viewModel: PetCatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pack = uiState.packs.firstOrNull { it.key == packKey }
    TrackScreenView(ScreenName.PET_DETAIL)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
            .navigationBarsPadding()
    ) {
        CutePetTopBar(
            title = pack?.manifest?.name ?: stringResource(R.string.pet_detail_unavailable),
            onBack = onBack
        )
        if (pack == null) {
            UnavailablePet()
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._20sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            PetPackThumbnail(
                pack = pack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._220sdp))
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            Text(
                text = stringResource(R.string.pet_detail_heading, pack.manifest.name),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = CutePetTitleFont,
                fontWeight = FontWeight.Bold,
                fontSize = dimensionResource(SspR.dimen._21ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
            Text(
                text = stringResource(
                    R.string.pet_detail_description,
                    pack.manifest.clips.size
                ),
                color = colorResource(R.color.colors_776D84),
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
            Text(
                text = stringResource(
                    R.string.pet_detail_by_author,
                    pack.manifest.author ?: stringResource(R.string.pet_catalog_unknown_author)
                ),
                color = colorResource(R.color.colors_7B61FF),
                fontFamily = FontFamily(Font(R.font.roboto_medium)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            CutePetCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PetDetailMetric(
                        label = stringResource(R.string.pet_detail_actions),
                        value = pack.manifest.clips.size.toString(),
                        tintColorRes = R.color.colors_EDE4FF
                    )
                    PetDetailMetric(
                        label = stringResource(R.string.pet_detail_version),
                        value = pack.manifest.version.toString(),
                        tintColorRes = R.color.colors_FFF0D6
                    )
                    PetDetailMetric(
                        label = stringResource(R.string.pet_detail_source),
                        value = if (pack.source is PetPackSource.BuiltIn) {
                            stringResource(R.string.pet_detail_builtin)
                        } else {
                            stringResource(R.string.pet_detail_installed)
                        },
                        tintColorRes = R.color.colors_E7F7F1
                    )
                }
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._18sdp)))
            CutePetPrimaryButton(
                text = if (uiState.requiresMixedSlotReward) {
                    stringResource(R.string.pet_detail_unlock_slot_first)
                } else if (uiState.selectedKey == pack.key) {
                    stringResource(R.string.pet_catalog_selected)
                } else if (uiState.target == PetCatalogTarget.SWARM) {
                    stringResource(R.string.pet_detail_select_for_swarm)
                } else {
                    stringResource(
                        R.string.pet_detail_select_for_slot,
                        uiState.targetSlotIndex + 1
                    )
                },
                onClick = {
                    if (viewModel.select(pack.key)) onBack()
                },
                enabled = !uiState.requiresMixedSlotReward && uiState.selectedKey != pack.key,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._24sdp)))
        }
    }
}

@Composable
private fun PetDetailMetric(label: String, value: String, tintColorRes: Int) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._13sdp)))
            .background(colorResource(tintColorRes))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._10sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.roboto_bold)),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._2sdp)))
        Text(
            text = label,
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontSize = dimensionResource(SspR.dimen._7ssp).value.sp
        )
    }
}

@Composable
private fun UnavailablePet() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(SdpR.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.pet_detail_unavailable),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UnavailablePetPreview() {
    UnavailablePet()
}
