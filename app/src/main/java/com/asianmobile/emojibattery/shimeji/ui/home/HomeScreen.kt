package com.asianmobile.emojibattery.shimeji.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
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
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.PetDisplayMode
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.catalog.PetCatalogTarget
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetIconAction
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetPrimaryButton
import com.asianmobile.emojibattery.shimeji.ui.component.CutePetTitleFont
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun HomeScreen(
    onNavigateToCatalog: (PetCatalogTarget, Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSwarmCustomization: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    TrackScreenView(ScreenName.HOME)

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onNotificationPermissionResult()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(context) {
        RewardedVideoAds.getInstance().loadRewardedVideo(context.applicationContext)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.OpenOverlaySettings -> {
                    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
                    runCatching {
                        overlaySettingsLauncher.launch(PetOverlay.permissionIntent(context))
                    }.onFailure {
                        overlaySettingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        )
                    }
                }
                HomeEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        viewModel.onNotificationPermissionResult()
                    }
                }
                HomeEffect.ShowSwarmRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onSwarmRewardResult(
                            RewardedAdResult.UNAVAILABLE.shouldContinueFlow
                        )
                    } else {
                        RewardedVideoAds.getInstance().showRewardedAd(activity) { result ->
                            viewModel.onSwarmRewardResult(result.shouldContinueFlow)
                        }
                    }
                }
            }
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onGlobalToggle = viewModel::onPetButtonClicked,
        onModeSelected = viewModel::selectMode,
        onMixedPetVisibilityToggle = viewModel::toggleMixedPet,
        onSwarmCountChanged = viewModel::updateSwarmCount,
        onRemoveSwarmPet = viewModel::clearSwarmPet,
        onUnlockSwarm = viewModel::requestSwarmUnlock,
        onDismissMessage = viewModel::clearMessage,
        onNavigateToCatalog = onNavigateToCatalog,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPremium = onNavigateToPremium,
        onNavigateToSwarmCustomization = onNavigateToSwarmCustomization
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onGlobalToggle: () -> Unit,
    onModeSelected: (PetDisplayMode) -> Unit,
    onMixedPetVisibilityToggle: (Int) -> Unit,
    onSwarmCountChanged: (Int) -> Unit,
    onRemoveSwarmPet: () -> Unit,
    onUnlockSwarm: () -> Unit,
    onDismissMessage: () -> Unit,
    onNavigateToCatalog: (PetCatalogTarget, Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSwarmCustomization: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_F4F8FC))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._16sdp))
        ) {
            HomeHeader(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPremium = onNavigateToPremium
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._14sdp)))
            GlobalEnableCard(
                uiState = uiState,
                onToggle = onGlobalToggle
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._14sdp)))
            ModeSelector(
                selectedMode = uiState.displayMode,
                onModeSelected = onModeSelected
            )
            uiState.message?.let { message ->
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
                HomeMessageCard(message)
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(MESSAGE_DURATION_MILLIS)
                    onDismissMessage()
                }
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._16sdp)))
            when (uiState.displayMode) {
                PetDisplayMode.MIXED -> MixedModeContent(
                    uiState = uiState,
                    onVisibilityToggle = onMixedPetVisibilityToggle,
                    onOpenPet = { slotIndex ->
                        onNavigateToCatalog(PetCatalogTarget.MIXED, slotIndex)
                    }
                )
                PetDisplayMode.SWARM -> SwarmModeContent(
                    uiState = uiState,
                    onUnlock = onUnlockSwarm,
                    onPremium = onNavigateToPremium,
                    onChoosePet = {
                        onNavigateToCatalog(PetCatalogTarget.SWARM, 0)
                    },
                    onCountChanged = onSwarmCountChanged,
                    onRemove = onRemoveSwarmPet,
                    onEdit = onNavigateToSwarmCustomization
                )
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._20sdp)))
        }

        HomeBottomNavigation(
            uiState = uiState,
            onOpenPets = {
                val target = if (uiState.displayMode == PetDisplayMode.SWARM) {
                    PetCatalogTarget.SWARM
                } else {
                    PetCatalogTarget.MIXED
                }
                onNavigateToCatalog(target, 0)
            },
            onOpenSettings = onNavigateToSettings
        )
        BannerAd(
            modifier = Modifier.fillMaxWidth(),
            adPosition = HOME_MODE_BANNER_POSITION
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun HomeHeader(
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._56sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_brand_name),
            color = colorResource(R.color.colors_12B890),
            fontFamily = CutePetTitleFont,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(SspR.dimen._22ssp).value.sp,
            modifier = Modifier.weight(1f)
        )
        CutePetIconAction(
            iconRes = R.drawable.ic_crown_premium,
            contentDescription = stringResource(R.string.home_open_premium),
            onClick = onNavigateToPremium
        )
        Spacer(Modifier.size(dimensionResource(SdpR.dimen._8sdp)))
        CutePetIconAction(
            iconRes = R.drawable.ic_settings_outline,
            contentDescription = stringResource(R.string.home_open_settings),
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun GlobalEnableCard(
    uiState: HomeUiState,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(colorResource(R.color.colors_D8F4EE))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_12B890),
                RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))
            )
            .clickable(enabled = uiState.actionsEnabled, onClick = onToggle)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._16sdp),
                vertical = dimensionResource(SdpR.dimen._12sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                if (uiState.isPetRunning) {
                    R.string.home_mode_enabled
                } else {
                    R.string.home_mode_enable
                }
            ),
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = uiState.isPetRunning,
            onCheckedChange = { onToggle() },
            enabled = uiState.actionsEnabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorResource(R.color.colors_12B890),
                uncheckedTrackColor = colorResource(R.color.colors_9297A5)
            )
        )
    }
}

@Composable
private fun ModeSelector(
    selectedMode: PetDisplayMode,
    onModeSelected: (PetDisplayMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(dimensionResource(SdpR.dimen._4sdp))
    ) {
        ModeTab(
            text = stringResource(R.string.home_mode_swarm),
            selected = selectedMode == PetDisplayMode.SWARM,
            onClick = { onModeSelected(PetDisplayMode.SWARM) },
            modifier = Modifier.weight(1f)
        )
        ModeTab(
            text = stringResource(R.string.home_mode_mixed),
            selected = selectedMode == PetDisplayMode.MIXED,
            onClick = { onModeSelected(PetDisplayMode.MIXED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._17sdp)))
            .background(
                colorResource(
                    if (selected) R.color.colors_D8F4EE else R.color.colors_FFFFFF
                )
            )
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(SdpR.dimen._11sdp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colorResource(
                if (selected) R.color.colors_12B890 else R.color.colors_9297A5
            ),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MixedModeContent(
    uiState: HomeUiState,
    onVisibilityToggle: (Int) -> Unit,
    onOpenPet: (Int) -> Unit
) {
    ModeSectionHeading(
        title = stringResource(R.string.home_mode_mixed_title),
        description = stringResource(R.string.home_mode_mixed_description)
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        (0 until uiState.maxMixedPets)
            .chunked(MIXED_GRID_COLUMN_COUNT)
            .forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(SdpR.dimen._8sdp)
                    )
                ) {
                    rowSlots.forEach { slotIndex ->
                        val pet = uiState.mixedPets.getOrNull(slotIndex)
                        if (pet == null) {
                            AddMixedPetCard(
                                slotNumber = slotIndex + 1,
                                isLocked = slotIndex >= uiState.mixedUnlockedSlotCount,
                                enabled = slotIndex == uiState.petCount,
                                onClick = { onOpenPet(slotIndex) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            MixedPetCard(
                                pet = pet,
                                onVisibilityToggle = { onVisibilityToggle(slotIndex) },
                                onClick = { onOpenPet(slotIndex) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(MIXED_GRID_COLUMN_COUNT - rowSlots.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
    }
}

@Composable
private fun MixedPetCard(
    pet: HomeMixedPetUiState,
    onVisibilityToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp))
    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .alpha(if (pet.isEnabled) 1f else 0.55f)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(
                    if (pet.isEnabled) R.color.colors_12B890 else R.color.colors_9297A5
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._7sdp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
            PetPreview(
                previewPath = pet.previewPath,
                contentDescription = pet.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Text(
                text = pet.name,
                color = colorResource(R.color.colors_2F2440),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(
            onClick = onVisibilityToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(dimensionResource(SdpR.dimen._28sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
        ) {
            Icon(
                painter = painterResource(
                    if (pet.isEnabled) {
                        R.drawable.ic_visibility_on
                    } else {
                        R.drawable.ic_visibility_off
                    }
                ),
                contentDescription = stringResource(
                    if (pet.isEnabled) {
                        R.string.home_mode_pet_visible
                    } else {
                        R.string.home_mode_pet_hidden
                    },
                    pet.name
                ),
                tint = colorResource(
                    if (pet.isEnabled) R.color.colors_12B890 else R.color.colors_9297A5
                ),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._17sdp))
            )
        }
    }
}

@Composable
private fun AddMixedPetCard(
    slotNumber: Int,
    isLocked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp))
    Column(
        modifier = modifier
            .aspectRatio(0.82f)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(
                    if (enabled) R.color.colors_12B890 else R.color.colors_9297A5
                ),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._8sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(
                if (isLocked) R.drawable.ic_lock_fill else R.drawable.ic_plus
            ),
            contentDescription = null,
            tint = colorResource(
                if (enabled) R.color.colors_12B890 else R.color.colors_9297A5
            ),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._30sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._7sdp)))
        Text(
            text = stringResource(
                if (isLocked) {
                    R.string.home_mode_unlock_pet_slot
                } else {
                    R.string.home_mode_add_pet_slot
                },
                slotNumber
            ),
            color = colorResource(
                if (enabled) R.color.colors_12B890 else R.color.colors_9297A5
            ),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SwarmModeContent(
    uiState: HomeUiState,
    onUnlock: () -> Unit,
    onPremium: () -> Unit,
    onChoosePet: () -> Unit,
    onCountChanged: (Int) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    ModeSectionHeading(
        title = stringResource(R.string.home_mode_swarm_title),
        description = stringResource(R.string.home_mode_swarm_description)
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
    when {
        !uiState.swarmUnlocked -> SwarmLockedCard(
            onUnlock = onUnlock,
            onPremium = onPremium
        )
        uiState.swarmPackName == null -> SwarmEmptyCard(onClick = onChoosePet)
        else -> SwarmConfiguredCard(
            name = uiState.swarmPackName,
            previewPath = uiState.swarmPreviewPath,
            count = uiState.swarmCount,
            maxCount = uiState.maxSwarmPets,
            onChoosePet = onChoosePet,
            onCountChanged = onCountChanged,
            onRemove = onRemove,
            onEdit = onEdit
        )
    }
}

@Composable
private fun ModeSectionHeading(title: String, description: String) {
    Text(
        text = title,
        color = colorResource(R.color.colors_2F2440),
        fontFamily = CutePetTitleFont,
        fontWeight = FontWeight.Bold,
        fontSize = dimensionResource(SspR.dimen._20ssp).value.sp
    )
    Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
    Text(
        text = description,
        color = colorResource(R.color.colors_776D84),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._13ssp).value.sp
    )
}

@Composable
private fun SwarmLockedCard(
    onUnlock: () -> Unit,
    onPremium: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_D8F4EE))
            .padding(dimensionResource(SdpR.dimen._18sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock_fill),
            contentDescription = null,
            tint = colorResource(R.color.colors_12B890),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._42sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
        Text(
            text = stringResource(R.string.home_mode_swarm_locked_title),
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._14ssp).value.sp
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
        Text(
            text = stringResource(R.string.home_mode_swarm_locked_description),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._13sdp)))
        CutePetPrimaryButton(
            text = stringResource(R.string.home_mode_swarm_watch_reward),
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.home_mode_swarm_premium_hint),
            color = colorResource(R.color.colors_12B890),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                .clickable(onClick = onPremium)
                .padding(dimensionResource(SdpR.dimen._8sdp))
        )
    }
}

@Composable
private fun SwarmEmptyCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._170sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_D8F4EE))
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._18sdp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._54sdp))
                .clip(CircleShape)
                .border(
                    dimensionResource(SdpR.dimen._2sdp),
                    colorResource(R.color.colors_FFFFFF),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._28sdp))
            )
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
        Text(
            text = stringResource(R.string.home_mode_swarm_empty_title),
            color = colorResource(R.color.colors_2F2440),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
        )
        Text(
            text = stringResource(R.string.home_mode_swarm_empty_description),
            color = colorResource(R.color.colors_776D84),
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SwarmConfiguredCard(
    name: String,
    previewPath: String?,
    count: Int,
    maxCount: Int,
    onChoosePet: () -> Unit,
    onCountChanged: (Int) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._20sdp)))
            .background(colorResource(R.color.colors_D8F4EE))
            .clickable(onClick = onEdit)
            .padding(dimensionResource(SdpR.dimen._14sdp))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetPreview(
                previewPath = previewPath,
                contentDescription = name,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._92sdp))
            )
            Spacer(Modifier.size(dimensionResource(SdpR.dimen._12sdp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = colorResource(R.color.colors_2F2440),
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.home_mode_swarm_change),
                    color = colorResource(R.color.colors_12B890),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                        .clickable(onClick = onChoosePet)
                        .padding(vertical = dimensionResource(SdpR.dimen._5sdp))
                )
                Text(
                    text = stringResource(R.string.home_mode_swarm_edit),
                    color = colorResource(R.color.colors_2F2440),
                    fontFamily = FontFamily(Font(R.font.inter_medium)),
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                        .clickable(onClick = onEdit)
                        .padding(vertical = dimensionResource(SdpR.dimen._3sdp))
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FFFFFF))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(R.string.home_mode_swarm_remove),
                    tint = colorResource(R.color.colors_E45D6A)
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
                .background(colorResource(R.color.colors_FFFFFF))
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._8sdp),
                    vertical = dimensionResource(SdpR.dimen._4sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountButton(
                iconRes = R.drawable.ic_remove,
                contentDescription = stringResource(R.string.home_mode_swarm_decrease),
                enabled = count > 1,
                onClick = { onCountChanged(count - 1) }
            )
            Text(
                text = stringResource(R.string.home_mode_swarm_count, count),
                color = colorResource(R.color.colors_12B890),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            CountButton(
                iconRes = R.drawable.ic_plus,
                contentDescription = stringResource(R.string.home_mode_swarm_increase),
                enabled = count < maxCount,
                onClick = { onCountChanged(count + 1) }
            )
        }
    }
}

@Composable
private fun CountButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(dimensionResource(SdpR.dimen._36sdp))
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = colorResource(
                if (enabled) R.color.colors_2F2440 else R.color.colors_9297A5
            ),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
    }
}

@Composable
private fun PetPreview(
    previewPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .background(colorResource(R.color.colors_FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        if (previewPath != null) {
            AsyncImage(
                model = previewPath,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(SdpR.dimen._5sdp))
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_notification_pet),
                contentDescription = contentDescription,
                tint = colorResource(R.color.pet_demo_fur),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(SdpR.dimen._13sdp))
            )
        }
    }
}

@Composable
private fun HomeMessageCard(message: HomeMessage) {
    val text = when (message) {
        HomeMessage.PET_START_FAILED -> stringResource(R.string.home_pet_start_failed)
        HomeMessage.KEEP_ONE_MIXED_PET_VISIBLE ->
            stringResource(R.string.home_mode_keep_one_visible)
        HomeMessage.SELECT_SWARM_PET ->
            stringResource(R.string.home_mode_select_swarm_pet)
        HomeMessage.SWARM_REWARD_NOT_EARNED ->
            stringResource(R.string.home_mode_reward_not_earned)
    }
    Text(
        text = text,
        color = colorResource(R.color.colors_E45D6A),
        fontFamily = FontFamily(Font(R.font.inter_medium)),
        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp)))
            .background(colorResource(R.color.colors_FFE8EF))
            .padding(dimensionResource(SdpR.dimen._10sdp))
    )
}

@Composable
private fun HomeBottomNavigation(
    uiState: HomeUiState,
    onOpenPets: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._16sdp),
                vertical = dimensionResource(SdpR.dimen._8sdp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
    ) {
        BottomNavigationItem(
            iconRes = R.drawable.ic_notification_pet,
            label = stringResource(R.string.home_mode_tab),
            selected = true,
            onClick = {},
            modifier = Modifier.weight(1.25f)
        )
        BottomNavigationItem(
            iconRes = R.drawable.ic_plus,
            label = stringResource(R.string.home_mode_pets_tab),
            selected = false,
            onClick = onOpenPets,
            modifier = Modifier.weight(1f)
        )
        BottomNavigationItem(
            iconRes = R.drawable.ic_settings_outline,
            label = stringResource(R.string.home_mode_settings_tab),
            selected = false,
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomNavigationItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp)))
            .background(
                colorResource(
                    if (selected) R.color.colors_12B890 else R.color.colors_FFFFFF
                )
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._10sdp),
                vertical = dimensionResource(SdpR.dimen._9sdp)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = colorResource(
                if (selected) R.color.colors_FFFFFF else R.color.colors_9297A5
            ),
            modifier = Modifier.size(dimensionResource(SdpR.dimen._20sdp))
        )
        if (selected) {
            Spacer(Modifier.size(dimensionResource(SdpR.dimen._7sdp)))
            Text(
                text = label,
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreenContent(
        uiState = HomeUiState(
            overlayGranted = true,
            displayMode = PetDisplayMode.MIXED,
            mixedPets = listOf(
                HomeMixedPetUiState(0, "Nanami Kento", null, true),
                HomeMixedPetUiState(1, "Pain", null, false)
            ),
            petCount = 2
        ),
        onGlobalToggle = {},
        onModeSelected = {},
        onMixedPetVisibilityToggle = {},
        onSwarmCountChanged = {},
        onRemoveSwarmPet = {},
        onUnlockSwarm = {},
        onDismissMessage = {},
        onNavigateToCatalog = { _, _ -> },
        onNavigateToSettings = {},
        onNavigateToPremium = {},
        onNavigateToSwarmCustomization = {}
    )
}

private const val MESSAGE_DURATION_MILLIS = 3_500L
private const val HOME_MODE_BANNER_POSITION = "home_mode_bottom"
private const val MIXED_GRID_COLUMN_COUNT = 3
