package com.asianmobile.emojibattery.shimeji.ui.catalog

import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogError
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackSource
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetCard
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetPrimaryButton
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTitleFont
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTopBar
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun PetCatalogScreen(
    onBack: () -> Unit,
    onOpenPack: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: PetCatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::install)
    }
    TrackScreenView(ScreenName.PET_CATALOG)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshEntitlement()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(context, uiState.requiresMixedSlotReward) {
        if (uiState.requiresMixedSlotReward) {
            RewardedVideoAds.getInstance().loadRewardedVideo(context.applicationContext)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PetCatalogEffect.ShowMixedSlotRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onMixedSlotRewardResult(
                            RewardedAdResult.UNAVAILABLE.shouldContinueFlow
                        )
                    } else {
                        RewardedVideoAds.getInstance().showRewardedAd(activity) { result ->
                            viewModel.onMixedSlotRewardResult(result.shouldContinueFlow)
                        }
                    }
                }
            }
        }
    }

    PetCatalogContent(
        uiState = uiState,
        onBack = onBack,
        onImport = { picker.launch(arrayOf("application/zip", "application/octet-stream")) },
        onOpenPack = onOpenPack,
        onSearchQueryChanged = viewModel::updateSearchQuery,
        onSelectCategory = viewModel::selectCategory,
        onSetPet = viewModel::setOwnerPet,
        onRetry = viewModel::refreshCatalog,
        onUnlockMixedSlot = viewModel::requestMixedSlotUnlock,
        onNavigateToPremium = onNavigateToPremium
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
    onRetry: () -> Unit,
    onUnlockMixedSlot: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFF9F4))
            .navigationBarsPadding()
    ) {
        CutePetTopBar(
            title = stringResource(R.string.pet_catalog_title),
            onBack = onBack,
            trailing = {
                if (!uiState.requiresMixedSlotReward) {
                    RefreshButton(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRetry
                    )
                    Spacer(Modifier.size(dimensionResource(SdpR.dimen._4sdp)))
                    ImportButton(
                        isInstalling = uiState.isInstalling,
                        onImport = onImport
                    )
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        ) {
            Text(
                text = stringResource(R.string.pet_catalog_heading),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = CutePetTitleFont,
                fontSize = dimensionResource(SspR.dimen._20ssp).value.sp
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.target == PetCatalogTarget.SWARM) {
                        stringResource(R.string.pet_catalog_swarm_badge)
                    } else {
                        stringResource(
                            R.string.pet_catalog_slot_badge,
                            uiState.targetSlotIndex + 1
                        )
                    },
                    color = colorResource(R.color.colors_5D46D7),
                    fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
                        .background(colorResource(R.color.colors_EDE4FF))
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._9sdp),
                            vertical = dimensionResource(SdpR.dimen._5sdp)
                        )
                )
                Spacer(Modifier.size(dimensionResource(SdpR.dimen._7sdp)))
                Text(
                    text = stringResource(R.string.pet_catalog_import_hint),
                    color = colorResource(R.color.colors_776D84),
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp
                )
            }
            uiState.message?.let { message ->
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
                CatalogMessage(message)
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
        }

        when {
            uiState.requiresMixedSlotReward -> MixedSlotRewardGate(
                slotNumber = uiState.targetSlotIndex + 1,
                onUnlock = onUnlockMixedSlot,
                onPremium = onNavigateToPremium
            )
            uiState.isLoading -> CatalogLoading()
            uiState.catalogError != null -> CatalogError(
                error = uiState.catalogError,
                localRootPath = uiState.localRootPath,
                onRetry = onRetry
            )
            else -> CatalogGrid(
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
private fun MixedSlotRewardGate(
    slotNumber: Int,
    onUnlock: () -> Unit,
    onPremium: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(SdpR.dimen._20sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock_fill),
            contentDescription = null,
            tint = colorResource(R.color.colors_12B890),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._48sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Text(
            text = stringResource(R.string.pet_catalog_mixed_slot_locked_title, slotNumber),
            color = colorResource(R.color.colors_2F2440),
            fontFamily = CutePetTitleFont,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._18ssp).value.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Text(
            text = stringResource(R.string.pet_catalog_mixed_slot_locked_description),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
        CutePetPrimaryButton(
            text = stringResource(R.string.pet_catalog_mixed_slot_watch_reward),
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.pet_catalog_mixed_slot_premium_hint),
            color = colorResource(R.color.colors_12B890),
            fontFamily = FontFamily(Font(R.font.roboto_medium)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                .clickable(onClick = onPremium)
                .padding(dimensionResource(SdpR.dimen._10sdp))
        )
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, onRefresh: () -> Unit) {
    IconButton(
        onClick = onRefresh,
        enabled = !isRefreshing
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(SdpR.dimen._16sdp)),
                strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                color = colorResource(R.color.colors_7B61FF)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.pet_catalog_refresh)
            )
        }
    }
}

@Composable
private fun ImportButton(isInstalling: Boolean, onImport: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)))
            .background(colorResource(R.color.colors_FFF0D6))
            .clickable(enabled = !isInstalling, onClick = onImport)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._10sdp),
                vertical = dimensionResource(SdpR.dimen._7sdp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isInstalling) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp)),
                strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                color = colorResource(R.color.colors_7B61FF)
            )
        } else {
            Text(
                text = stringResource(R.string.pet_catalog_import),
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
        }
    }
}

@Composable
private fun CatalogGrid(
    uiState: PetCatalogUiState,
    onOpenPack: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSetPet: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CatalogSearch(
            query = uiState.searchQuery,
            onQueryChanged = onSearchQueryChanged
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._16sdp)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
            modifier = Modifier.padding(vertical = dimensionResource(SdpR.dimen._7sdp))
        ) {
            items(
                count = uiState.categories.size,
                key = { index -> uiState.categories[index].name ?: ALL_CATEGORY_KEY }
            ) { index ->
                val category = uiState.categories[index]
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
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp)),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colorResource(R.color.colors_FFFFFB),
                        labelColor = colorResource(R.color.colors_776D84),
                        selectedContainerColor = colorResource(R.color.colors_EDE4FF),
                        selectedLabelColor = colorResource(R.color.colors_5D46D7)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = colorResource(R.color.colors_E9DFEF),
                        selectedBorderColor = colorResource(R.color.colors_7B61FF)
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
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.roboto_medium)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            modifier = Modifier.padding(
                start = dimensionResource(SdpR.dimen._16sdp),
                end = dimensionResource(SdpR.dimen._16sdp),
                bottom = dimensionResource(SdpR.dimen._7sdp)
            )
        )
        if (uiState.visiblePets.isEmpty()) {
            CatalogEmpty()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimensionResource(SdpR.dimen._16sdp),
                    end = dimensionResource(SdpR.dimen._16sdp),
                    bottom = dimensionResource(SdpR.dimen._18sdp)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(SdpR.dimen._9sdp)
                )
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
private fun CatalogSearch(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.pet_catalog_search_hint),
                fontFamily = FontFamily(Font(R.font.roboto_regular))
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.pet_catalog_clear_search)
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorResource(R.color.colors_FFFFFB),
            unfocusedContainerColor = colorResource(R.color.colors_FFFFFB),
            focusedTextColor = colorResource(R.color.colors_2F2440),
            unfocusedTextColor = colorResource(R.color.colors_2F2440),
            focusedIndicatorColor = colorResource(R.color.colors_7B61FF),
            unfocusedIndicatorColor = colorResource(R.color.colors_E9DFEF),
            focusedLeadingIconColor = colorResource(R.color.colors_7B61FF),
            unfocusedLeadingIconColor = colorResource(R.color.colors_776D84),
            focusedTrailingIconColor = colorResource(R.color.colors_7B61FF),
            unfocusedTrailingIconColor = colorResource(R.color.colors_776D84),
            focusedPlaceholderColor = colorResource(R.color.colors_776D84),
            unfocusedPlaceholderColor = colorResource(R.color.colors_776D84)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
    )
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
    CutePetCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._112sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
                .background(
                    colorResource(
                        when (pet.id % 3) {
                            0 -> R.color.colors_F7F0FF
                            1 -> R.color.colors_FFF0D6
                            else -> R.color.colors_E7F7F1
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (pet.thumbnailPath != null) {
                AsyncImage(
                    model = pet.thumbnailPath.takeIf { it.startsWith("http") }
                        ?: File(pet.thumbnailPath),
                    contentDescription = stringResource(
                        R.string.pet_catalog_pet_image,
                        pet.name
                    ),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(SdpR.dimen._6sdp))
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_pet),
                    contentDescription = null,
                    tint = colorResource(R.color.pet_demo_fur),
                    modifier = Modifier.padding(dimensionResource(SdpR.dimen._18sdp))
                )
            }
            if (isSelected) {
                Text(
                    text = stringResource(R.string.pet_catalog_selected_badge),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    fontSize = dimensionResource(SspR.dimen._7ssp).value.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(dimensionResource(SdpR.dimen._7sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                        .background(colorResource(R.color.colors_39B87A))
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._7sdp),
                            vertical = dimensionResource(SdpR.dimen._4sdp)
                        )
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = pet.name,
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.roboto_semibold)),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = pet.category,
            color = colorResource(R.color.colors_7B61FF),
            fontFamily = FontFamily(Font(R.font.roboto_medium)),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._7sdp)))
        Button(
            onClick = onSet,
            enabled = pet.hasLocalArchive && !isSelected && !isAnotherPreparing && !isPreparing,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._34sdp)),
            shape = RoundedCornerShape(dimensionResource(SdpR.dimen._11sdp)),
            contentPadding = PaddingValues(horizontal = dimensionResource(SdpR.dimen._7sdp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.colors_7B61FF),
                disabledContainerColor = colorResource(
                    if (isSelected) R.color.colors_E7F7F1 else R.color.colors_E9DFEF
                ),
                contentColor = colorResource(R.color.colors_FFFFFF),
                disabledContentColor = colorResource(
                    if (isSelected) R.color.colors_39B87A else R.color.colors_776D84
                )
            )
        ) {
            if (isPreparing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._14sdp)),
                    strokeWidth = dimensionResource(SdpR.dimen._2sdp),
                    color = colorResource(R.color.colors_FFFFFF)
                )
            } else {
                Text(
                    text = when {
                        isSelected -> stringResource(R.string.pet_catalog_selected)
                        !pet.hasLocalArchive -> stringResource(R.string.pet_catalog_not_synced)
                        else -> stringResource(R.string.pet_catalog_set)
                    },
                    fontFamily = FontFamily(Font(R.font.roboto_semibold)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
                )
            }
        }
    }
}

@Composable
private fun CatalogMessage(message: PetCatalogMessage) {
    Text(
        text = catalogMessageText(message),
        color = colorResource(R.color.colors_5D46D7),
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
            .background(colorResource(R.color.colors_EDE4FF))
            .padding(dimensionResource(SdpR.dimen._9sdp))
    )
}

@Composable
private fun CatalogLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colorResource(R.color.colors_7B61FF))
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
        OwnerPetCatalogError.REMOTE_CATALOG_UNAVAILABLE -> stringResource(
            R.string.pet_catalog_server_unavailable
        )
        OwnerPetCatalogError.REMOTE_CATALOG_INVALID -> stringResource(
            R.string.pet_catalog_server_invalid
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(SdpR.dimen._24sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_notification_pet),
            contentDescription = null,
            tint = colorResource(R.color.colors_FF7A9E),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._52sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Text(
            text = message,
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        CutePetPrimaryButton(
            text = stringResource(R.string.pet_catalog_retry),
            onClick = onRetry
        )
    }
}

@Composable
private fun CatalogEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.pet_catalog_no_results),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.roboto_regular))
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
    PetCatalogMessage.RewardNotEarned ->
        stringResource(R.string.home_mode_reward_not_earned)
    PetCatalogMessage.PreviousSlotRequired ->
        stringResource(R.string.pet_catalog_mixed_slot_previous_required)
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
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._24sdp)))
            .background(colorResource(R.color.colors_F7F0FF)),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = pack.manifest.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(SdpR.dimen._8sdp))
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = pack.manifest.name,
                tint = colorResource(R.color.pet_demo_fur),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(SdpR.dimen._14sdp))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogEmptyPreview() {
    CatalogEmpty()
}

private const val ALL_CATEGORY_KEY = "__all__"
private const val MESSAGE_DURATION_MILLIS = 4_000L
