package com.asianmobile.privatebrower.ui.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.pet.pack.PetPack
import com.asianmobile.privatebrower.pet.pack.PetPackSource
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun PetCatalogScreen(
    onBack: () -> Unit,
    onOpenPack: (String) -> Unit,
    viewModel: PetCatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::install)
    }
    TrackScreenView(ScreenName.PET_CATALOG)

    PetCatalogContent(
        uiState = uiState,
        onBack = onBack,
        onImport = { picker.launch(arrayOf("application/zip", "application/octet-stream")) },
        onOpenPack = onOpenPack
    )
    if (uiState.message != null) {
        LaunchedEffect(uiState.message) {
            delay(MESSAGE_DURATION_MILLIS)
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun PetCatalogContent(
    uiState: PetCatalogUiState,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onOpenPack: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = stringResource(R.string.pet_catalog_title),
                color = colorResource(R.color.white),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = 22.sp,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onImport,
                enabled = !uiState.isInstalling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colors_3369FD),
                    contentColor = colorResource(R.color.white)
                )
            ) {
                if (uiState.isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colorResource(R.color.white)
                    )
                } else {
                    Text(stringResource(R.string.pet_catalog_import))
                }
            }
        }
        Text(
            text = stringResource(R.string.pet_catalog_subtitle),
            color = colorResource(R.color.colors_9B9C9E),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        uiState.message?.let { message ->
            Text(
                text = catalogMessageText(message),
                color = colorResource(R.color.colors_C0D1FE),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.packs, key = PetPack::key) { pack ->
                PetPackCard(
                    pack = pack,
                    isSelected = pack.key == uiState.selectedKey,
                    onClick = { onOpenPack(pack.key) }
                )
            }
        }
    }
}

@Composable
private fun catalogMessageText(message: PetCatalogMessage): String = when (message) {
    is PetCatalogMessage.Installed -> stringResource(
        R.string.pet_catalog_install_success,
        message.name
    )
    is PetCatalogMessage.Selected -> stringResource(
        R.string.pet_catalog_select_success,
        message.name
    )
    is PetCatalogMessage.Rejected -> stringResource(
        R.string.pet_catalog_install_rejected,
        message.reason
    )
    is PetCatalogMessage.Failed -> message.reason
}

@Composable
private fun PetPackCard(pack: PetPack, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(
                    if (isSelected) R.color.colors_4254A6 else R.color.colors_212327
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetPackThumbnail(pack = pack, modifier = Modifier.size(76.dp))
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pack.manifest.name,
                color = colorResource(R.color.white),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pack.manifest.author ?: stringResource(R.string.pet_catalog_unknown_author),
                color = colorResource(R.color.colors_9B9C9E),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isSelected) {
                    stringResource(R.string.pet_catalog_selected)
                } else {
                    stringResource(R.string.pet_catalog_view_details)
                },
                color = colorResource(R.color.colors_C0D1FE),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
internal fun PetPackThumbnail(pack: PetPack, modifier: Modifier = Modifier) {
    val installed = pack.source as? PetPackSource.Installed
    val firstFrame = pack.manifest.clips.values.firstOrNull()?.frames?.firstOrNull()
    val image = if (installed != null && firstFrame != null) {
        File(installed.directoryPath, firstFrame.file)
    } else {
        null
    }
    Box(
        modifier = modifier.background(
            colorResource(R.color.colors_161718),
            RoundedCornerShape(16.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = pack.manifest.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = pack.manifest.name,
                tint = colorResource(R.color.pet_demo_fur),
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }
    }
}

private const val MESSAGE_DURATION_MILLIS = 4_000L
