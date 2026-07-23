package com.asianmobile.privatebrower.ui.catalog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.data.model.OwnerPetCatalogEntry
import com.asianmobile.privatebrower.data.model.OwnerPetCatalogError
import com.asianmobile.privatebrower.pet.pack.PetPack
import com.asianmobile.privatebrower.pet.pack.PetPackSource
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
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
        onOpenPack = onOpenPack,
        onSearchQueryChanged = viewModel::updateSearchQuery,
        onSelectCategory = viewModel::selectCategory,
        onSetPet = viewModel::setOwnerPet,
        onRetry = viewModel::refreshCatalog
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
    onOpenPack: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSetPet: (Int) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CatalogHeader(
            isInstalling = uiState.isInstalling,
            onBack = onBack,
            onImport = onImport
        )
        Text(
            text = stringResource(
                R.string.pet_catalog_target_slot,
                uiState.targetSlotIndex + 1
            ),
            color = colorResource(R.color.colors_9B9C9E),
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            modifier = Modifier.padding(
                horizontal = dimensionResource(SdpR.dimen._18sdp),
                vertical = dimensionResource(SdpR.dimen._4sdp)
            )
        )
        uiState.message?.let { message ->
            Text(
                text = catalogMessageText(message),
                color = colorResource(R.color.colors_C0D1FE),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._18sdp),
                    vertical = dimensionResource(SdpR.dimen._3sdp)
                )
            )
        }
        when {
            uiState.isLoading -> CatalogLoading()
            uiState.catalogError != null -> CatalogError(
                error = uiState.catalogError,
                localRootPath = uiState.localRootPath,
                onRetry = onRetry
            )
            else -> CatalogList(
                uiState = uiState,
                onOpenPack = onOpenPack,
                onSearchQueryChanged = onSearchQueryChanged,
                onSelectCategory = onSelectCategory,
                onSetPet = onSetPet
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    isInstalling: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(SdpR.dimen._8sdp),
                vertical = dimensionResource(SdpR.dimen._5sdp)
            ),
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
            fontSize = dimensionResource(SspR.dimen._17ssp).value.sp,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onImport,
            enabled = !isInstalling,
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_3369FD),
                contentColor = colorResource(R.color.white)
            )
        ) {
            if (isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._16sdp)),
                    strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                    color = colorResource(R.color.white)
                )
            } else {
                Text(
                    text = stringResource(R.string.pet_catalog_import),
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp
                )
            }
        }
    }
}

@Composable
private fun CatalogList(
    uiState: PetCatalogUiState,
    onOpenPack: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSetPet: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.pet_catalog_search_hint)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.pet_catalog_clear_search)
                        )
                    }
                }
            } else {
                null
            },
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorResource(R.color.colors_212327),
                unfocusedContainerColor = colorResource(R.color.colors_212327),
                focusedTextColor = colorResource(R.color.white),
                unfocusedTextColor = colorResource(R.color.white),
                focusedIndicatorColor = colorResource(R.color.colors_3369FD),
                unfocusedIndicatorColor = colorResource(R.color.colors_4D4D4D),
                focusedLeadingIconColor = colorResource(R.color.colors_C0D1FE),
                unfocusedLeadingIconColor = colorResource(R.color.colors_9B9C9E),
                focusedTrailingIconColor = colorResource(R.color.colors_C0D1FE),
                unfocusedTrailingIconColor = colorResource(R.color.colors_9B9C9E),
                focusedPlaceholderColor = colorResource(R.color.colors_9B9C9E),
                unfocusedPlaceholderColor = colorResource(R.color.colors_9B9C9E)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._18sdp))
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._18sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._7sdp)),
            modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._7sdp))
        ) {
            items(uiState.categories, key = { it.name ?: ALL_CATEGORY_KEY }) { category ->
                val selected = uiState.selectedCategory == category.name
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category.name) },
                    label = {
                        Text(
                            text = if (category.name == null) {
                                stringResource(R.string.pet_catalog_all_category, category.count)
                            } else {
                                stringResource(
                                    R.string.pet_catalog_category_with_count,
                                    category.name,
                                    category.count
                                )
                            },
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colorResource(R.color.colors_212327),
                        labelColor = colorResource(R.color.colors_B3B3B3),
                        selectedContainerColor = colorResource(R.color.colors_4254A6),
                        selectedLabelColor = colorResource(R.color.white)
                    )
                )
            }
        }
        Text(
            text = pluralStringResource(
                R.plurals.pet_catalog_result_count,
                uiState.visiblePets.size,
                uiState.visiblePets.size
            ),
            color = colorResource(R.color.colors_9B9C9E),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            modifier = Modifier.padding(
                start = dimensionResource(SdpR.dimen._18sdp),
                end = dimensionResource(SdpR.dimen._18sdp),
                bottom = dimensionResource(SdpR.dimen._5sdp)
            )
        )
        if (uiState.visiblePets.isEmpty()) {
            CatalogEmpty()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._18sdp),
                    end = dimensionResource(SdpR.dimen._18sdp),
                    bottom = dimensionResource(SdpR.dimen._18sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
            ) {
                items(uiState.visiblePets, key = OwnerPetCatalogEntry::id) { pet ->
                    val isInstalled = uiState.packs.any { it.key == pet.installedPackKey }
                    OwnerPetCard(
                        pet = pet,
                        isSelected = pet.installedPackKey == uiState.selectedKey,
                        isPreparing = pet.id == uiState.preparingPetId,
                        isAnotherPreparing = uiState.preparingPetId != null &&
                            pet.id != uiState.preparingPetId,
                        onClick = {
                            if (isInstalled) onOpenPack(pet.installedPackKey) else onSetPet(pet.id)
                        },
                        onSet = { onSetPet(pet.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerPetCard(
    pet: OwnerPetCatalogEntry,
    isSelected: Boolean,
    isPreparing: Boolean,
    isAnotherPreparing: Boolean,
    onClick: () -> Unit,
    onSet: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(
                colorResource(if (isSelected) R.color.colors_4254A6 else R.color.colors_212327)
            )
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._10sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._62sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
                .background(colorResource(R.color.colors_161718)),
            contentAlignment = Alignment.Center
        ) {
            if (pet.thumbnailPath != null) {
                AsyncImage(
                    model = File(pet.thumbnailPath),
                    contentDescription = pet.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(dimensionResource(SdpR.dimen._4sdp))
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_pet),
                    contentDescription = pet.name,
                    tint = colorResource(R.color.pet_demo_fur),
                    modifier = Modifier.padding(dimensionResource(SdpR.dimen._8sdp))
                )
            }
        }
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._10sdp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pet.name,
                color = colorResource(R.color.white),
                fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pet.category,
                color = colorResource(R.color.colors_C0D1FE),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pet.author ?: stringResource(R.string.pet_catalog_unknown_author),
                color = colorResource(R.color.colors_9B9C9E),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = onSet,
            enabled = pet.hasLocalArchive && !isSelected && !isAnotherPreparing && !isPreparing,
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._10sdp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_3369FD),
                disabledContainerColor = colorResource(
                    if (isSelected) R.color.colors_00C950 else R.color.colors_3D3D3D
                ),
                contentColor = colorResource(R.color.white),
                disabledContentColor = colorResource(R.color.white)
            )
        ) {
            if (isPreparing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp)),
                    strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                    color = colorResource(R.color.white)
                )
            } else {
                Text(
                    text = when {
                        isSelected -> stringResource(R.string.pet_catalog_selected)
                        !pet.hasLocalArchive -> stringResource(R.string.pet_catalog_not_synced)
                        else -> stringResource(R.string.pet_catalog_set)
                    },
                    fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                )
            }
        }
    }
}

@Composable
private fun CatalogLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colorResource(R.color.colors_3369FD))
    }
}

@Composable
private fun CatalogError(
    error: OwnerPetCatalogError,
    localRootPath: String,
    onRetry: () -> Unit
) {
    val message = when (error) {
        OwnerPetCatalogError.LOCAL_CATALOG_MISSING -> stringResource(
            R.string.pet_catalog_local_missing,
            localRootPath
        )
        OwnerPetCatalogError.LOCAL_CATALOG_INVALID -> stringResource(
            R.string.pet_catalog_local_invalid
        )
        OwnerPetCatalogError.LOCAL_STORAGE_UNAVAILABLE -> stringResource(
            R.string.pet_catalog_storage_unavailable
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(SdpR.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = colorResource(R.color.colors_B3B3B3),
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.pet_catalog_retry))
        }
    }
}

@Composable
private fun CatalogEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.pet_catalog_no_results),
            color = colorResource(R.color.colors_9B9C9E)
        )
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
    is PetCatalogMessage.Failed -> stringResource(R.string.pet_catalog_prepare_failed)
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
            RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = pack.manifest.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(dimensionResource(SdpR.dimen._5sdp))
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = pack.manifest.name,
                tint = colorResource(R.color.pet_demo_fur),
                modifier = Modifier.fillMaxSize().padding(dimensionResource(SdpR.dimen._7sdp))
            )
        }
    }
}

private const val ALL_CATEGORY_KEY = "__all__"
private const val MESSAGE_DURATION_MILLIS = 4_000L
