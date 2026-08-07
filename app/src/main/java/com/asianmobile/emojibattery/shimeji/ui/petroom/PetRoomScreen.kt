package com.asianmobile.emojibattery.shimeji.ui.petroom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.care.PetEnergyLevel
import com.asianmobile.emojibattery.shimeji.pet.care.PetEnergyPolicy
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.ToastHelper
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
fun PetRoomScreen(
    onNavigateBack: () -> Unit = {},
    onOpenPetStore: () -> Unit = {},
    viewModel: PetRoomViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.MY_PET)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    LaunchedEffect(uiState.message) {
        if (uiState.message == PetRoomMessage.ROOM_DOWNLOAD_FAILED) {
            ToastHelper.show(context, context.getString(R.string.pet_room_download_failed))
            viewModel.dismissMessage()
        }
    }

    PetRoomContent(
        uiState = uiState,
        scene = scene,
        onNavigateBack = onNavigateBack,
        onOpenPetStore = onOpenPetStore,
        onToggleMusic = viewModel::toggleMusic,
        onToggleSheet = viewModel::toggleSheet,
        onSelectTab = viewModel::selectTab,
        onSelectRoom = viewModel::selectRoom,
        onOpenPet = viewModel::openPet,
        onCloseDetail = viewModel::closeDetail,
        onToggleOnScreen = viewModel::toggleOnScreen,
        onFeed = viewModel::feed,
        onPetTapped = viewModel::openPetByPackKey,
        onRemovePet = viewModel::requestRemovePet,
        onConfirmRemovePet = viewModel::confirmRemovePet,
        onCancelRemovePet = viewModel::cancelRemovePet,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onSettingsSpeedChange = viewModel::updateSettingsSpeed,
        onSettingsSizeChange = viewModel::updateSettingsSize,
        onSaveSettings = viewModel::saveSettings
    )
}

@Composable
private fun PetRoomContent(
    uiState: PetRoomUiState,
    scene: List<PetRoomSceneEntry>,
    onNavigateBack: () -> Unit,
    onOpenPetStore: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleSheet: () -> Unit,
    onSelectTab: (PetRoomTab) -> Unit,
    onSelectRoom: (Int) -> Unit,
    onOpenPet: (Int) -> Unit = {},
    onCloseDetail: () -> Unit = {},
    onToggleOnScreen: () -> Unit = {},
    onFeed: (String) -> Unit = {},
    onPetTapped: (String) -> Unit = {},
    onRemovePet: (Int) -> Unit = {},
    onConfirmRemovePet: () -> Unit = {},
    onCancelRemovePet: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onSettingsSpeedChange: (Int) -> Unit = {},
    onSettingsSizeChange: (Int) -> Unit = {},
    onSaveSettings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_F7F0E7))
    ) {
        uiState.backgroundPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: uiState.backgroundRes?.let { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        PetRoomScene(
            pets = scene,
            modifier = Modifier.fillMaxSize(),
            onPetTapped = onPetTapped
        )

        Column(modifier = Modifier.fillMaxSize()) {
            PetRoomTopBar(
                title = uiState.detail?.name ?: stringResource(R.string.pet_room_title),
                isMusicOn = uiState.isMusicOn,
                onNavigateBack = onNavigateBack,
                onToggleMusic = onToggleMusic,
                modifier = Modifier.statusBarsPadding()
            )
            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            RoundIconButton(
                iconRes = R.drawable.ic_pet_room_settings,
                contentDescription = stringResource(R.string.pet_room_settings),
                iconSize = SdpR.dimen._15sdp,
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = dimensionResource(SdpR.dimen._12sdp))
            )
            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            PetStoreShortcut(
                onClick = onOpenPetStore,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = dimensionResource(SdpR.dimen._6sdp))
            )
            Spacer(modifier = Modifier.weight(1f))
            SheetToggle(
                isExpanded = uiState.isSheetExpanded,
                onClick = onToggleSheet,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(
                        end = dimensionResource(SdpR.dimen._12sdp),
                        bottom = dimensionResource(SdpR.dimen._8sdp)
                    )
            )
            PetRoomSheet(
                uiState = uiState,
                onSelectTab = onSelectTab,
                onSelectRoom = onSelectRoom,
                onAddPet = onOpenPetStore,
                onOpenPet = onOpenPet,
                onCloseDetail = onCloseDetail,
                onToggleOnScreen = onToggleOnScreen,
                onFeed = onFeed,
                onRemovePet = onRemovePet
            )
        }

        uiState.settings?.let { settings ->
            PetRoomSettingsDialog(
                settings = settings,
                onSpeedChange = onSettingsSpeedChange,
                onSizeChange = onSettingsSizeChange,
                onSave = onSaveSettings,
                onDismiss = onCloseSettings
            )
        }

        uiState.petPendingRemoval?.let { pet ->
            PetRoomRemoveDialog(
                pet = pet,
                onConfirm = onConfirmRemovePet,
                onDismiss = onCancelRemovePet
            )
        }
    }
}

@Composable
private fun PetRoomTopBar(
    title: String,
    isMusicOn: Boolean,
    onNavigateBack: () -> Unit,
    onToggleMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._49sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            iconRes = R.drawable.ic_pet_room_back,
            contentDescription = stringResource(R.string.pet_room_back),
            iconSize = SdpR.dimen._22sdp,
            onClick = onNavigateBack
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.img_pet_room_sign),
                    contentDescription = null,
                    modifier = Modifier.width(dimensionResource(SdpR.dimen._137sdp))
                )
                // The exported sign carries no text, so the plate and title are drawn here and
                // the title can become the pet name without printing over the artwork.
                Box(
                    modifier = Modifier
                        .width(dimensionResource(SdpR.dimen._105sdp))
                        .height(dimensionResource(SdpR.dimen._20sdp))
                        .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                        .background(colorResource(R.color.colors_8F6250))
                        .border(
                            1.dp,
                            colorResource(R.color.colors_8B5748),
                            RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = colorResource(R.color.colors_FFFFFF),
                        fontWeight = FontWeight.Medium,
                        fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(SdpR.dimen._4sdp)
                        )
                    )
                }
            }
        }
        RoundIconButton(
            iconRes = if (isMusicOn) {
                R.drawable.ic_pet_room_music_on
            } else {
                R.drawable.ic_pet_room_music_off
            },
            contentDescription = stringResource(
                if (isMusicOn) R.string.pet_room_music_on else R.string.pet_room_music_off
            ),
            iconSize = SdpR.dimen._18sdp,
            onClick = onToggleMusic,
            borderRes = R.color.colors_FEC1D4
        )
    }
}

/** Figma wraps every room action in a 32px white rounded square with the icon centred. */
@Composable
private fun RoundIconButton(
    iconRes: Int,
    contentDescription: String?,
    iconSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    rotationDegrees: Float = 0f,
    backgroundRes: Int = R.color.colors_FFFFFF,
    borderRes: Int = R.color.colors_C8C8C9
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._25sdp))
            .clip(shape)
            .background(colorResource(backgroundRes))
            .border(1.dp, colorResource(borderRes), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(dimensionResource(iconSize))
                .rotate(rotationDegrees)
        )
    }
}

@Composable
private fun PetStoreShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.img_pet_room_store_shortcut),
        contentDescription = stringResource(R.string.pet_room_open_store),
        modifier = modifier
            .width(dimensionResource(SdpR.dimen._38sdp))
            .aspectRatio(STORE_SHORTCUT_ASPECT_RATIO)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SheetToggle(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RoundIconButton(
        iconRes = R.drawable.ic_pet_room_collapse,
        contentDescription = stringResource(
            if (isExpanded) R.string.pet_room_collapse_sheet else R.string.pet_room_expand_sheet
        ),
        iconSize = SdpR.dimen._18sdp,
        onClick = onClick,
        modifier = modifier,
        tint = colorResource(R.color.colors_725938),
        rotationDegrees = if (isExpanded) 0f else 180f,
        backgroundRes = R.color.colors_F7F0E7,
        borderRes = R.color.colors_DCCAB1
    )
}

@Composable
private fun PetRoomSheet(
    uiState: PetRoomUiState,
    onSelectTab: (PetRoomTab) -> Unit,
    onSelectRoom: (Int) -> Unit,
    onAddPet: () -> Unit,
    onOpenPet: (Int) -> Unit,
    onCloseDetail: () -> Unit,
    onToggleOnScreen: () -> Unit,
    onFeed: (String) -> Unit,
    onRemovePet: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PetRoomTabStrip(selectedTab = uiState.selectedTab, onSelectTab = onSelectTab)
        if (uiState.isSheetExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._151sdp))
                    .clip(
                        RoundedCornerShape(
                            topStart = dimensionResource(SdpR.dimen._9sdp),
                            topEnd = dimensionResource(SdpR.dimen._9sdp)
                        )
                    )
                    .background(colorResource(R.color.colors_F7F0E7))
                    .border(
                        width = 1.dp,
                        color = colorResource(R.color.colors_8F6250),
                        shape = RoundedCornerShape(
                            topStart = dimensionResource(SdpR.dimen._9sdp),
                            topEnd = dimensionResource(SdpR.dimen._9sdp)
                        )
                    )
                    .navigationBarsPadding()
            ) {
                val detail = uiState.detail
                when {
                    detail != null && uiState.selectedTab != PetRoomTab.FOOD ->
                        PetDetailPanel(
                            detail = detail,
                            onBack = onCloseDetail,
                            onToggleOnScreen = onToggleOnScreen
                        )

                    uiState.selectedTab == PetRoomTab.MY_PET -> MyPetTabContent(
                        pets = uiState.pets,
                        onAddPet = onAddPet,
                        onOpenPet = onOpenPet,
                        onRemovePet = onRemovePet
                    )

                    uiState.selectedTab == PetRoomTab.FOOD -> FoodTabContent(
                        foods = uiState.foods,
                        onFeed = onFeed,
                        onAddFood = onAddPet
                    )

                    else -> RoomTabContent(uiState = uiState, onSelectRoom = onSelectRoom)
                }
            }
        }
    }
}

@Composable
private fun PetRoomTabStrip(
    selectedTab: PetRoomTab,
    onSelectTab: (PetRoomTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(TAB_STRIP_WIDTH_RATIO)
            .height(dimensionResource(SdpR.dimen._31sdp)),
        verticalAlignment = Alignment.Bottom
    ) {
        PetRoomTab.entries.forEach { tab ->
            PetRoomTabItem(
                tab = tab,
                isSelected = tab == selectedTab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PetRoomTabItem(
    tab: PetRoomTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedShape = RoundedCornerShape(
        topStart = dimensionResource(SdpR.dimen._12sdp),
        topEnd = dimensionResource(SdpR.dimen._12sdp)
    )
    val shape = RoundedCornerShape(
        topStart = dimensionResource(SdpR.dimen._9sdp),
        topEnd = dimensionResource(SdpR.dimen._9sdp)
    )
    Box(
        modifier = modifier
            .height(
                dimensionResource(if (isSelected) SdpR.dimen._31sdp else SdpR.dimen._25sdp)
            )
            .clip(if (isSelected) selectedShape else shape)
            .background(colorResource(R.color.colors_F7F0E7))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(SELECTED_TAB_INNER_RATIO)
                    .fillMaxHeight()
                    .padding(top = dimensionResource(SdpR.dimen._2sdp))
                    .clip(selectedShape)
                    .background(colorResource(R.color.colors_E4CCB1))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(SELECTED_TAB_DASH_RATIO)
                    .fillMaxHeight()
                    .padding(top = dimensionResource(SdpR.dimen._4sdp))
                    .dashedBorder(
                        color = colorResource(R.color.colors_B69B7D),
                        shape = shape,
                        dash = TAB_DASH
                    )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp)),
            modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._4sdp))
        ) {
            Icon(
                painter = painterResource(tab.iconRes()),
                contentDescription = null,
                tint = colorResource(R.color.colors_725938),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._14sdp))
            )
            Text(
                text = stringResource(tab.labelRes()),
                color = colorResource(R.color.colors_725938),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MyPetTabContent(
    pets: List<PetRoomPetUiState>,
    onAddPet: () -> Unit,
    onOpenPet: (Int) -> Unit,
    onRemovePet: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ROOM_GRID_COLUMNS),
        contentPadding = PaddingValues(dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = ADD_PET_CARD_KEY) { AddPetCard(onClick = onAddPet) }
        items(pets, key = PetRoomPetUiState::packKey) { pet ->
            PetCard(
                pet = pet,
                onClick = { onOpenPet(pet.petId) },
                onRemove = { onRemovePet(pet.petId) }
            )
        }
    }
}

@Composable
private fun AddPetCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PET_CARD_ASPECT_RATIO)
            .clip(shape)
            .background(colorResource(R.color.colors_FFECD4))
            .border(1.dp, colorResource(R.color.colors_8F6250), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._37sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
                .border(1.dp, colorResource(R.color.colors_D3BEA2), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pet_room_add),
                contentDescription = stringResource(R.string.pet_room_add_pet),
                tint = colorResource(R.color.colors_D3BEA2),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._31sdp))
            )
        }
    }
}

@Composable
private fun PetCard(
    pet: PetRoomPetUiState,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PET_CARD_ASPECT_RATIO)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(2.dp, colorResource(R.color.colors_FFECD4), shape)
            .clickable(onClick = onClick)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(PET_CARD_IMAGE_WEIGHT)
        ) {
            val unit = maxWidth / FIGMA_CARD_WIDTH
            pet.thumbnailPath?.let { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(unit * 50f)
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_pet_room_delete),
                contentDescription = stringResource(R.string.pet_room_remove_pet),
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(unit * 8f)
                    .size(unit * 16f)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(PET_CARD_TEXT_WEIGHT)
                .padding(top = dimensionResource(SdpR.dimen._2sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = pet.name,
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.pet_room_breed, pet.breed),
                color = colorResource(R.color.colors_FDA3C0),
                fontSize = dimensionResource(SspR.dimen._6ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._8ssp).value.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PetDetailPanel(
    detail: PetRoomDetailUiState,
    onBack: () -> Unit,
    onToggleOnScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(SdpR.dimen._9sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._18sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                    .background(colorResource(R.color.colors_FFFFFF))
                    .border(
                        1.dp,
                        colorResource(R.color.colors_C8C8C9),
                        RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp))
                    )
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pet_room_back),
                    contentDescription = stringResource(R.string.pet_room_back),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.pet_room_on_screen),
                color = colorResource(R.color.colors_FB3675),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
            )
            Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
            OnScreenToggle(checked = detail.isOnScreen, onClick = onToggleOnScreen)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(SdpR.dimen._60sdp))
                    .background(colorResource(R.color.colors_FFFFFF), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                detail.thumbnailPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._46sdp))
                    )
                }
                Image(
                    painter = painterResource(R.drawable.img_pet_room_tape),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = -dimensionResource(SdpR.dimen._8sdp),
                            y = -dimensionResource(SdpR.dimen._6sdp)
                        )
                        .width(dimensionResource(SdpR.dimen._34sdp))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                DetailRow(R.string.pet_room_field_name, detail.name)
                DetailRow(R.string.pet_room_field_breed, detail.breed)
                DetailRow(R.string.pet_room_field_adopted, detail.adoptedOn)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .height(dimensionResource(SdpR.dimen._18sdp))
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                    .background(colorResource(R.color.colors_8F6250))
                    .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.img_pet_room_energy_icon),
                    contentDescription = null,
                    modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp))
                )
                Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
                Text(
                    text = stringResource(R.string.pet_room_energy),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
                )
            }
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colorResource(R.color.colors_8F6250)
            )
        }

        EnergyBar(percent = detail.energyPercent)
    }
}

@Composable
private fun DetailRow(labelRes: Int, value: String) {
    Column(modifier = Modifier.padding(bottom = dimensionResource(SdpR.dimen._8sdp))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(labelRes),
                color = colorResource(R.color.colors_8F6250),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                modifier = Modifier.width(dimensionResource(SdpR.dimen._54sdp))
            )
            Text(
                text = value,
                color = colorResource(R.color.colors_212327),
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DashedRule(
            color = colorResource(R.color.colors_8F6250),
            modifier = Modifier.padding(top = dimensionResource(SdpR.dimen._2sdp))
        )
    }
}

@Composable
private fun OnScreenToggle(checked: Boolean, onClick: () -> Unit) {
    val track = colorResource(if (checked) R.color.colors_FB3675 else R.color.colors_C8C8C9)
    Box(
        modifier = Modifier
            .width(dimensionResource(SdpR.dimen._34sdp))
            .height(dimensionResource(SdpR.dimen._18sdp))
            .clip(CircleShape)
            .background(track)
            .clickable(onClick = onClick)
            .padding(dimensionResource(SdpR.dimen._2sdp)),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._15sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
        )
    }
}

@Composable
private fun EnergyBar(percent: Int) {
    val level = PetEnergyPolicy.level(percent)
    val gradient = when (level) {
        PetEnergyLevel.GOOD -> listOf(R.color.colors_94DF37, R.color.colors_47B321)
        PetEnergyLevel.MEDIUM -> listOf(R.color.colors_FFDF50, R.color.colors_EDB90E)
        PetEnergyLevel.LOW -> listOf(R.color.colors_FF4E4E, R.color.colors_BF3535)
    }.map { colorResource(it) }
    val borderColor = colorResource(
        when (level) {
            PetEnergyLevel.GOOD -> R.color.colors_368619
            PetEnergyLevel.MEDIUM -> R.color.colors_B28B0A
            PetEnergyLevel.LOW -> R.color.colors_8F2828
        }
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._32sdp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._25sdp))
                .clip(CircleShape)
                .background(colorResource(R.color.colors_E4DBD1))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(PetEnergyPolicy.fillFraction(percent))
                .height(dimensionResource(SdpR.dimen._25sdp))
                .clip(CircleShape)
                .background(Brush.horizontalGradient(gradient))
                .border(1.dp, borderColor, CircleShape)
        )
        Text(
            text = if (PetEnergyPolicy.isMax(percent)) {
                stringResource(R.string.pet_room_energy_max)
            } else {
                stringResource(R.string.pet_room_energy_value, percent, PetEnergyPolicy.MAX_ENERGY)
            },
            color = colorResource(R.color.colors_FFFFFF),
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Image(
            painter = painterResource(R.drawable.img_pet_room_energy_bowl),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._32sdp))
        )
    }
}

@Composable
private fun FoodTabContent(
    foods: List<PetRoomFoodUiState>,
    onFeed: (String) -> Unit,
    onAddFood: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ROOM_GRID_COLUMNS),
        contentPadding = PaddingValues(dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        modifier = Modifier.fillMaxSize()
    ) {
        items(foods, key = PetRoomFoodUiState::id) { food ->
            FoodCard(food = food, onFeed = { onFeed(food.id) }, onAdd = onAddFood)
        }
    }
}

@Composable
private fun FoodCard(food: PetRoomFoodUiState, onFeed: () -> Unit, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TALL_CARD_ASPECT_RATIO)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(2.dp, colorResource(R.color.colors_FFECD4), shape)
            .clickable(onClick = onFeed)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(FOOD_CARD_IMAGE_WEIGHT)
        ) {
            // The grid decides how wide a card is, so every inset is a share of that width
            // rather than a fixed dp, and the Figma proportions hold on any screen.
            val unit = maxWidth / FIGMA_CARD_WIDTH
            Image(
                painter = painterResource(food.imageRes),
                contentDescription = food.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = unit * 1f)
                    .size(unit * 70f)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(unit * 6f)
                    .height(unit * 18f)
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FFF1B2))
                    .padding(horizontal = unit * 4f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(unit * 2f)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_pet_room_energy_icon),
                    contentDescription = null,
                    modifier = Modifier.height(unit * 12f)
                )
                Text(
                    text = food.energyValue.toString(),
                    color = colorResource(R.color.colors_A54905),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(unit * 6f)
                    .size(unit * 20f)
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_E1CCB9))
                    .dashedBorder(
                        color = colorResource(R.color.colors_D3BEA2),
                        shape = CircleShape,
                        dash = TAB_DASH
                    )
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pet_room_add),
                    contentDescription = stringResource(R.string.pet_room_add_food),
                    tint = colorResource(R.color.colors_FFFFFF),
                    modifier = Modifier.size(unit * 17f)
                )
            }
            // Figma hangs the portion pill on the bottom-right corner of the food artwork.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = unit * 16f, bottom = unit * 6f)
                    .height(unit * 18f)
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_8D6037))
                    .border(1.dp, colorResource(R.color.colors_FFFFFF), CircleShape)
                    .padding(horizontal = unit * 7f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.pet_room_food_portions, food.portions),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(FOOD_CARD_TEXT_WEIGHT)
                .padding(top = dimensionResource(SdpR.dimen._3sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = food.name,
                color = colorResource(R.color.colors_212327),
                fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RoomTabContent(
    uiState: PetRoomUiState,
    onSelectRoom: (Int) -> Unit
) {
    if (uiState.roomCatalogFailed) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.pet_room_rooms_unavailable),
                color = colorResource(R.color.colors_725938),
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(ROOM_GRID_COLUMNS),
        contentPadding = PaddingValues(dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.rooms, key = PetRoomThumbnailUiState::id) { room ->
            RoomCard(room = room, onClick = { onSelectRoom(room.id) })
        }
    }
}

@Composable
private fun RoomCard(room: PetRoomThumbnailUiState, onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TALL_CARD_ASPECT_RATIO)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            // Figma marks the active room with a 3px pink edge, not the 2px cream one.
            .border(
                width = if (room.isSelected) 3.dp else 2.dp,
                color = colorResource(
                    if (room.isSelected) R.color.colors_FB3675 else R.color.colors_FFECD4
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        room.thumbnailRes?.let { res ->
            Image(
                painter = painterResource(res),
                contentDescription = room.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        } ?: room.thumbnailPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = room.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        }
        when {
            room.isDownloading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(colorResource(R.color.colors_000000).copy(alpha = SCRIM_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorResource(R.color.colors_FFFFFF),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._21sdp))
                )
            }

            room.needsDownload -> Icon(
                painter = painterResource(R.drawable.ic_download_arrow),
                contentDescription = stringResource(R.string.pet_room_download),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimensionResource(SdpR.dimen._3sdp))
                    .size(dimensionResource(SdpR.dimen._15sdp))
            )
        }
        if (room.isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_pet_room_selected),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimensionResource(SdpR.dimen._3sdp))
                    .size(dimensionResource(SdpR.dimen._21sdp))
            )
        }
    }
}

/** Figma draws several room borders with a dash pattern; Modifier.border only strokes solid. */
private fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    dash: Dp,
    gap: Dp = dash
): Modifier = drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
        )
    )
}

/** Figma underlines every detail row with a 7/7 dashed rule. */
@Composable
private fun DashedRule(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(DETAIL_RULE_DASH_PX, DETAIL_RULE_DASH_PX)
            )
        )
    }
}

private fun PetRoomTab.iconRes(): Int = when (this) {
    PetRoomTab.MY_PET -> R.drawable.ic_pet_room_tab_pet
    PetRoomTab.FOOD -> R.drawable.ic_pet_room_tab_food
    PetRoomTab.ROOM -> R.drawable.ic_pet_room_tab_room
}

private fun PetRoomTab.labelRes(): Int = when (this) {
    PetRoomTab.MY_PET -> R.string.pet_room_tab_pet
    PetRoomTab.FOOD -> R.string.pet_room_tab_food
    PetRoomTab.ROOM -> R.string.pet_room_tab_room
}

// Figma: tab strip 346 of the 360 frame; selected tab inner plates 108 and 102 of 114.
private const val TAB_STRIP_WIDTH_RATIO = 346f / 360f
private const val SELECTED_TAB_INNER_RATIO = 108f / 114f
private const val SELECTED_TAB_DASH_RATIO = 102f / 114f
private const val STORE_SHORTCUT_ASPECT_RATIO = 50f / 74.33f
private const val ROOM_GRID_COLUMNS = 3
private const val SCRIM_ALPHA = 0.45f
// Figma: pet and add cards are 104x106, food and room cards 104x122.
private const val PET_CARD_ASPECT_RATIO = 104f / 106f
private const val TALL_CARD_ASPECT_RATIO = 104f / 122f
private const val PET_CARD_IMAGE_WEIGHT = 70f / 106f
private const val PET_CARD_TEXT_WEIGHT = 36f / 106f
private const val FOOD_CARD_IMAGE_WEIGHT = 90f / 122f
private const val FOOD_CARD_TEXT_WEIGHT = 32f / 122f
private const val FIGMA_CARD_WIDTH = 104f
private val TAB_DASH = 3.dp
private const val DETAIL_RULE_DASH_PX = 5.4f
private const val ADD_PET_CARD_KEY = "add_pet"

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PetRoomScreenPreview() {
    PetRoomContent(
        scene = emptyList(),
        uiState = PetRoomUiState(
            selectedTab = PetRoomTab.ROOM,
            isRoomCatalogLoading = false,
            rooms = List(4) { index ->
                PetRoomThumbnailUiState(
                    id = index + 1,
                    name = "Room ${index + 1}",
                    thumbnailPath = null,
                    thumbnailRes = null,
                    isSelected = index == 0,
                    needsDownload = index > 0,
                    isDownloading = false
                )
            }
        ),
        onNavigateBack = {},
        onOpenPetStore = {},
        onToggleMusic = {},
        onToggleSheet = {},
        onSelectTab = {},
        onSelectRoom = {}
    )
}
