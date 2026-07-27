package com.asianmobile.emojibattery.shimeji.ui.catalog

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackSource
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView

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
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = colorResource(R.color.white)
                )
            }
            Text(
                text = pack?.manifest?.name ?: stringResource(R.string.pet_detail_unavailable),
                color = colorResource(R.color.white),
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
        }
        if (pack == null) {
            Text(
                text = stringResource(R.string.pet_detail_unavailable),
                color = colorResource(R.color.colors_9B9C9E),
                modifier = Modifier.padding(24.dp)
            )
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            PetPackThumbnail(pack = pack, modifier = Modifier.size(220.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                text = pack.manifest.name,
                color = colorResource(R.color.white),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.pet_detail_by_author,
                    pack.manifest.author ?: stringResource(R.string.pet_catalog_unknown_author)
                ),
                color = colorResource(R.color.colors_9B9C9E)
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PetDetailMetric(stringResource(R.string.pet_detail_version), pack.manifest.version.toString())
                PetDetailMetric(stringResource(R.string.pet_detail_actions), pack.manifest.clips.size.toString())
                PetDetailMetric(
                    stringResource(R.string.pet_detail_source),
                    if (pack.source is PetPackSource.BuiltIn) {
                        stringResource(R.string.pet_detail_builtin)
                    } else {
                        stringResource(R.string.pet_detail_installed)
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.select(pack.key) },
                enabled = uiState.selectedKey != pack.key,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colors_3369FD),
                    contentColor = colorResource(R.color.white),
                    disabledContainerColor = colorResource(R.color.colors_00C950),
                    disabledContentColor = colorResource(R.color.white)
                )
            ) {
                Text(
                    if (uiState.selectedKey == pack.key) {
                        stringResource(R.string.pet_catalog_selected)
                    } else {
                        stringResource(
                            R.string.pet_detail_select_for_slot,
                            uiState.targetSlotIndex + 1
                        )
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PetDetailMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = colorResource(R.color.white), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = colorResource(R.color.colors_9B9C9E), fontSize = 12.sp)
    }
}
