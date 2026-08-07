package com.asianmobile.emojibattery.shimeji.ui.petroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
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

    PetRoomContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onOpenPetStore = onOpenPetStore,
        onToggleMusic = viewModel::toggleMusic,
        onToggleSheet = viewModel::toggleSheet,
        onSelectTab = viewModel::selectTab,
        onSelectRoom = viewModel::selectRoom
    )
}

@Composable
private fun PetRoomContent(
    uiState: PetRoomUiState,
    onNavigateBack: () -> Unit,
    onOpenPetStore: () -> Unit,
    onToggleMusic: () -> Unit,
    onToggleSheet: () -> Unit,
    onSelectTab: (PetRoomTab) -> Unit,
    onSelectRoom: (Int) -> Unit
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
        }

        Column(modifier = Modifier.fillMaxSize()) {
            PetRoomTopBar(
                isMusicOn = uiState.isMusicOn,
                onNavigateBack = onNavigateBack,
                onToggleMusic = onToggleMusic,
                modifier = Modifier.statusBarsPadding()
            )
            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
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
                onAddPet = onOpenPetStore
            )
        }
    }
}

@Composable
private fun PetRoomTopBar(
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
                Text(
                    text = stringResource(R.string.pet_room_title),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontWeight = FontWeight.Medium,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            onClick = onToggleMusic
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
    rotationDegrees: Float = 0f
) {
    Box(
        modifier = modifier
            .size(dimensionResource(SdpR.dimen._25sdp))
            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
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
        rotationDegrees = if (isExpanded) 0f else 180f
    )
}

@Composable
private fun PetRoomSheet(
    uiState: PetRoomUiState,
    onSelectTab: (PetRoomTab) -> Unit,
    onSelectRoom: (Int) -> Unit,
    onAddPet: () -> Unit
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
                when (uiState.selectedTab) {
                    PetRoomTab.MY_PET -> MyPetTabContent(
                        pets = uiState.pets,
                        onAddPet = onAddPet
                    )

                    PetRoomTab.ROOM -> RoomTabContent(
                        uiState = uiState,
                        onSelectRoom = onSelectRoom
                    )

                    // Food arrives with the inventory phase.
                    PetRoomTab.FOOD -> Unit
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
                    .border(1.dp, colorResource(R.color.colors_B69B7D), shape)
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
    onAddPet: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ROOM_GRID_COLUMNS),
        contentPadding = PaddingValues(dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = ADD_PET_CARD_KEY) { AddPetCard(onClick = onAddPet) }
        items(pets, key = PetRoomPetUiState::packKey) { pet -> PetCard(pet = pet) }
    }
}

@Composable
private fun AddPetCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._82sdp))
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
private fun PetCard(pet: PetRoomPetUiState) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._82sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(2.dp, colorResource(R.color.colors_FFECD4), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._54sdp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensionResource(SdpR.dimen._5sdp))
                    .width(dimensionResource(SdpR.dimen._45sdp))
                    .height(dimensionResource(SdpR.dimen._9sdp))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_000000).copy(alpha = PET_SHADOW_ALPHA))
            )
            pet.thumbnailPath?.let { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._38sdp))
                )
            }
        }
        Text(
            text = pet.name,
            color = colorResource(R.color.colors_212327),
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.pet_room_breed, pet.breed),
            color = colorResource(R.color.colors_FDA3C0),
            fontSize = dimensionResource(SspR.dimen._6ssp).value.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
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
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
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
            .height(dimensionResource(SdpR.dimen._94sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(
                width = if (room.isSelected) 2.dp else 1.dp,
                color = colorResource(
                    if (room.isSelected) R.color.colors_FB3675 else R.color.colors_FFECD4
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        room.thumbnailPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = room.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        }
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
private const val ADD_PET_CARD_KEY = "add_pet"
private const val PET_SHADOW_ALPHA = 0.05f

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PetRoomScreenPreview() {
    PetRoomContent(
        uiState = PetRoomUiState(
            selectedTab = PetRoomTab.ROOM,
            isRoomCatalogLoading = false,
            rooms = List(4) { index ->
                PetRoomThumbnailUiState(
                    id = index + 1,
                    name = "Room ${index + 1}",
                    thumbnailPath = null,
                    isSelected = index == 0
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
