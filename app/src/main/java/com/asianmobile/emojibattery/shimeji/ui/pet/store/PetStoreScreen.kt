package com.asianmobile.emojibattery.shimeji.ui.pet.store

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieConstants
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.asianmobile.emojibattery.shimeji.ads.config.DIALOG_FOOD_REWARD
import com.asianmobile.emojibattery.shimeji.ads.config.DIALOG_PET_REWARD
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.pack.PetBitmapCache
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackVisual
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeEnableCard
import com.asianmobile.emojibattery.shimeji.ui.home.shell.HomeHeader
import com.asianmobile.emojibattery.shimeji.ui.pet.PetFamilyCapacityDialog
import com.asianmobile.emojibattery.shimeji.ui.shared.component.AppActionToast
import com.asianmobile.emojibattery.shimeji.ui.shared.component.OverlayPermissionDialog
import com.asianmobile.emojibattery.shimeji.ui.shared.component.PetPremiumBadge
import com.asianmobile.emojibattery.shimeji.ui.shared.component.PinkLoveSticker
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardGradientButton
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOfferSheet
import com.asianmobile.emojibattery.shimeji.ui.shared.component.RewardOutlineButton
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private val StoreRoboto = RobotoFontFamily
private val StoreRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val StoreRobotoSemiBold = FontFamily(Font(R.font.roboto_semibold))
private const val PET_CARD_WIDTH_PX = 104f
private const val PET_CARD_HEIGHT_PX = 142f
private const val PET_CARD_IMAGE_AREA_HEIGHT_PX = 90f
private const val PET_PREMIUM_BADGE_PX = 24f
private const val PET_CARD_IMAGE_SIZE_PX = 64f
private const val PET_SHADOW_WIDTH_PX = 58f
private const val PET_SHADOW_HEIGHT_PX = 12f
private const val FOOD_CARD_WIDTH_PX = 104f
private const val FOOD_CARD_HEIGHT_PX = 122f
private const val FOOD_CARD_IMAGE_AREA_HEIGHT_PX = 90f
private const val FOOD_CARD_IMAGE_SIZE_PX = 70f
private const val REWARD_SHEET_CONTENT_WIDTH_PX = 336f
private const val REWARD_PET_CARD_WIDTH_PX = 124f
private const val REWARD_PET_IMAGE_SIZE_PX = 70f
private const val REWARD_TAPE_WIDTH_PX = 52f
private const val REWARD_TAPE_HEIGHT_PX = 42f
private const val UNLOCK_FRAME_WIDTH_PX = 360f
private const val UNLOCK_LIGHTING_SIZE_PX = 310f
private const val UNLOCK_HERO_SIZE_PX = 174f
private const val PET_UNLOCK_TITLE_WIDTH_PX = 156f
private const val FOOD_UNLOCK_TITLE_WIDTH_PX = 189f
private const val UNLOCK_TITLE_HEIGHT_PX = 41f
private const val FOOD_QUANTITY_WIDTH_PX = 52f
private const val FOOD_QUANTITY_HEIGHT_PX = 34f
private const val FOOD_QUANTITY_X_IN_HERO_PX = 110f
private const val FOOD_QUANTITY_Y_IN_HERO_PX = 124f

@Composable
fun PetStoreScreen(
    requestedTab: PetStoreTab? = null,
    onRequestedTabConsumed: () -> Unit = {},
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onViewPet: () -> Unit,
    onNavigateToGrantPermissions: () -> Unit,
    viewModel: PetStoreViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackScreenView(ScreenName.PET_STORE)

    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            viewModel.selectTab(it)
            onRequestedTabConsumed()
        }
    }

    PetStoreFlowHost(
        state = state,
        viewModel = viewModel,
        onPremium = onPremium,
        onViewPet = onViewPet,
        onNavigateToGrantPermissions = onNavigateToGrantPermissions
    ) {
        PetStoreContent(
            state = state,
            onSearch = onSearch,
            onPremium = onPremium,
            onOpenMyPet = onViewPet,
            onToggle = viewModel::togglePetOverlay,
            onTab = viewModel::selectTab,
            onCategory = viewModel::selectCategory,
            onPet = viewModel::selectPet,
            onFood = viewModel::selectFood
        )
    }
}

/**
 * Owns the reward/download/reveal surfaces shared by the Shimeji Pets tab and Discover cards.
 * The caller owns only the browsing UI and navigation callbacks.
 */
@Composable
internal fun PetStoreFlowHost(
    state: PetStoreUiState,
    viewModel: PetStoreViewModel,
    onPremium: () -> Unit,
    onViewPet: () -> Unit,
    onNavigateToGrantPermissions: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showOverlayPermissionDisclosure by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
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
                PetStoreEffect.ShowRewardedAd -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        viewModel.onRewardResult(RewardedAdResult.UNAVAILABLE.shouldContinueFlow)
                    } else {
                        RewardedVideoAds.getInstance().showRewardedAd(activity) { result ->
                            viewModel.onRewardResult(result.shouldContinueFlow)
                        }
                    }
                }
                PetStoreEffect.OpenPremium -> onPremium()
                PetStoreEffect.OpenOverlaySettings -> {
                    showOverlayPermissionDisclosure = true
                }
                PetStoreEffect.OpenGrantPermissions -> onNavigateToGrantPermissions()
            }
        }
    }

    content()

    if (showOverlayPermissionDisclosure) {
        OverlayPermissionDialog(
            onAllowAccess = {
                showOverlayPermissionDisclosure = false
                onNavigateToGrantPermissions()
            },
            onNotNow = { showOverlayPermissionDisclosure = false }
        )
    }

    state.selectedPet?.let { pet ->
        PetRewardSheet(
            pet = pet,
            isDownloading = state.downloadingPetId == pet.id,
            message = state.message,
            onDismiss = viewModel::dismissRewardSheet,
            onPremium = viewModel::requestUnlimited,
            onReward = viewModel::requestPetReward
        )
    }
    state.selectedFood?.let { food ->
        FoodRewardSheet(
            food = food,
            onDismiss = viewModel::dismissRewardSheet,
            onPremium = onPremium,
            onAcquire = viewModel::acquireFoodPreview
        )
    }
    state.revealedPet?.let { pet ->
        PetUnlockReveal(
            pet = pet,
            pack = state.revealedPetPack,
            onContinue = viewModel::continueAfterReveal
        )
    }
    state.revealedFood?.let { food ->
        FoodUnlockReveal(
            food = food,
            onContinue = viewModel::continueAfterFoodReveal
        )
    }
    state.namingPet?.let { pet ->
        PetNameDialog(pet = pet, onSave = viewModel::savePetName)
    }
    state.joinedPetName?.takeUnless { showOverlayPermissionDisclosure }?.let { name ->
        AppActionToast(
            text = stringResource(R.string.pet_store_joined, name),
            action = stringResource(R.string.pet_store_view),
            onDismiss = viewModel::dismissJoinedToast,
            onAction = {
                viewModel.dismissJoinedToast()
                onViewPet()
            },
            leadingImageModel = state.joinedPetThumbnailPath
                ?: R.drawable.img_pink_love_sticker_preview
        )
    }
    state.acquiredFood?.let { food ->
        AppActionToast(
            text = stringResource(R.string.pet_store_food_received, food.name),
            action = null,
            onDismiss = viewModel::dismissFoodToast,
            onAction = {},
            leadingImageModel = food.imageRes
        )
    }
    state.petStartBlocker?.let { blocker ->
        PetStartAvailabilityDialog(
            blocker = blocker,
            onDismiss = viewModel::dismissPetStartBlocker,
            onPrimaryAction = {
                viewModel.dismissPetStartBlocker()
                when (blocker) {
                    PetStartBlocker.NO_OWNED_PETS -> viewModel.selectTab(PetStoreTab.PETS)
                    PetStartBlocker.NO_ACTIVE_PETS -> onViewPet()
                }
            }
        )
    }
    if (state.isPetCapacityDialogVisible) {
        PetFamilyCapacityDialog(
            onDismiss = viewModel::dismissPetCapacityDialog,
            onManagePets = {
                viewModel.dismissPetCapacityDialog()
                onViewPet()
            }
        )
    }
    state.message?.takeIf { state.selectedPet == null }?.let { message ->
        AppActionToast(
            text = message,
            action = null,
            onDismiss = viewModel::dismissMessage,
            onAction = {}
        )
    }
}

@Composable
internal fun PetStoreContent(
    state: PetStoreUiState,
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onOpenMyPet: () -> Unit,
    onToggle: () -> Unit,
    onTab: (PetStoreTab) -> Unit,
    onCategory: (String) -> Unit,
    onPet: (OwnerPetCatalogEntry) -> Unit,
    onFood: (PetStoreFood) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFFEFF9), Color(0xFFF1FAFF))
                )
            )
            .statusBarsPadding()
    ) {
        HomeHeader(onSearch = onSearch, onPremium = onPremium)
        HomeEnableCard(
            text = stringResource(R.string.pet_store_enable_pet),
            checked = state.isPetOnScreenEnabled,
            onCheckedChange = onToggle,
            bottomPadding = dimensionResource(SdpR.dimen._10sdp),
            switchInteractive = !state.isPetOnScreenStarting
        )
        PetStoreMyPetBanner(onClick = onOpenMyPet)
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._13sdp)))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(
                    elevation = dimensionResource(SdpR.dimen._8sdp),
                    shape = RoundedCornerShape(
                        topStart = dimensionResource(SdpR.dimen._18sdp),
                        topEnd = dimensionResource(SdpR.dimen._18sdp)
                    )
                )
                .clip(
                    RoundedCornerShape(
                        topStart = dimensionResource(SdpR.dimen._18sdp),
                        topEnd = dimensionResource(SdpR.dimen._18sdp)
                    )
                )
                .background(colorResource(R.color.colors_FFFFFF))
        ) {
            StoreTabs(state.selectedTab, onTab)
            when (state.selectedTab) {
                PetStoreTab.PETS -> {
                    val categories = PetStorePolicy.categories(state.pets)
                    val selectedCategory = PetStorePolicy.selectedCategory(
                        pets = state.pets,
                        requestedCategory = state.selectedCategory
                    )
                    PetCategoryTabs(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategory = onCategory
                    )
                    PetGrid(
                        state = state,
                        pets = PetStorePolicy.petsInCategory(
                            pets = state.pets,
                            category = selectedCategory
                        ),
                        onPet = onPet
                    )
                }
                PetStoreTab.FOOD -> FoodGrid(onFood)
            }
        }
    }
}

@Composable
internal fun PetStoreMyPetBanner(onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.img_pet_store_my_pet_banner),
        contentDescription = stringResource(R.string.pet_store_banner_placeholder),
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxWidth()
            // The Figma frame is 328x120, while its centered 3px stroke expands the exported
            // bitmap bounds to 334x126. Position the painted bounds at x=13 instead of squeezing
            // them into the inner frame at x=16, which would shift and distort this banner.
            .padding(horizontal = dimensionResource(SdpR.dimen._10sdp))
            .aspectRatio(334f / 126f)
            .clickable(role = Role.Button, onClick = onClick)
    )
}

@Composable
private fun StoreTabs(selected: PetStoreTab, onTab: (PetStoreTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        StoreTab(
            tab = PetStoreTab.PETS,
            selected = selected,
            imageRes = R.drawable.img_pet_store_tab_pet_selected,
            label = stringResource(R.string.pet_store_tab_pets),
            onTab = onTab,
            modifier = Modifier.weight(1f)
        )
        StoreTab(
            tab = PetStoreTab.FOOD,
            selected = selected,
            imageRes = R.drawable.img_pet_store_tab_food_selected,
            label = stringResource(R.string.pet_store_tab_food),
            onTab = onTab,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StoreTab(
    tab: PetStoreTab,
    selected: PetStoreTab,
    imageRes: Int,
    label: String,
    onTab: (PetStoreTab) -> Unit,
    modifier: Modifier
) {
    val active = tab == selected
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Row(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._34sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFEBF1))
            .then(
                if (active) {
                    Modifier.border(
                        width = dimensionResource(SdpR.dimen._1sdp),
                        color = colorResource(R.color.colors_FB3675),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !active) { onTab(tab) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
        Text(
            text = label,
            color = colorResource(R.color.colors_FB3675),
            fontFamily = StoreRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
    }
}

@Composable
private fun PetCategoryTabs(
    categories: List<String>,
    selectedCategory: String?,
    onCategory: (String) -> Unit
) {
    if (categories.isEmpty()) return
    val listState = rememberLazyListState()
    val selectedIndex = categories.indexOfFirst {
        it.equals(selectedCategory, ignoreCase = true)
    }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._32sdp)),
        contentPadding = PaddingValues(
            start = dimensionResource(SdpR.dimen._12sdp),
            end = dimensionResource(SdpR.dimen._12sdp),
            bottom = dimensionResource(SdpR.dimen._12sdp)
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._15sdp))
    ) {
        rowItems(categories, key = { it.lowercase() }) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._2sdp)))
                    .clickable(enabled = !isSelected) { onCategory(category) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = category,
                    color = colorResource(
                        if (isSelected) R.color.colors_FB3675 else R.color.colors_212327
                    ),
                    fontFamily = if (isSelected) StoreRobotoMedium else StoreRoboto,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(SdpR.dimen._1sdp)
                    )
                )
                Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(SdpR.dimen._2sdp))
                            .clip(CircleShape)
                            .background(colorResource(R.color.colors_FB3675))
                    )
                }
            }
        }
    }
}

@Composable
private fun PetGrid(
    state: PetStoreUiState,
    pets: List<OwnerPetCatalogEntry>,
    onPet: (OwnerPetCatalogEntry) -> Unit
) {
    if (state.isLoading && state.pets.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(R.color.colors_FB3675))
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = dimensionResource(SdpR.dimen._12sdp), end = dimensionResource(SdpR.dimen._12sdp), bottom = dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        items(pets, key = OwnerPetCatalogEntry::id) { pet ->
            PetCard(
                pet = pet,
                displayName = state.customNames[pet.id] ?: pet.name,
                isUnlocked = PetStorePolicy.isUnlocked(pet, state.installedPackKeys),
                isDownloading = state.downloadingPetId == pet.id,
                onClick = { onPet(pet) }
            )
        }
    }
}

@Composable
private fun PetCard(
    pet: OwnerPetCatalogEntry,
    displayName: String,
    isUnlocked: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PET_CARD_WIDTH_PX / PET_CARD_HEIGHT_PX)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_FFECD4),
                shape
            )
            .clickable(enabled = !isDownloading, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PET_CARD_WIDTH_PX / PET_CARD_IMAGE_AREA_HEIGHT_PX)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensionResource(SdpR.dimen._5sdp))
                    .fillMaxWidth(PET_SHADOW_WIDTH_PX / PET_CARD_WIDTH_PX)
                    .aspectRatio(PET_SHADOW_WIDTH_PX / PET_SHADOW_HEIGHT_PX)
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_000000).copy(alpha = 0.05f))
            )
            if (pet.thumbnailPath == null) {
                Image(
                    painter = painterResource(R.drawable.img_home_brand_bunny),
                    contentDescription = displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = dimensionResource(SdpR.dimen._12sdp))
                        .fillMaxWidth(PET_CARD_IMAGE_SIZE_PX / PET_CARD_WIDTH_PX)
                        .aspectRatio(1f)
                )
            } else {
                AsyncImage(
                    model = pet.thumbnailPath,
                    contentDescription = displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = dimensionResource(SdpR.dimen._12sdp))
                        .fillMaxWidth(PET_CARD_IMAGE_SIZE_PX / PET_CARD_WIDTH_PX)
                        .aspectRatio(1f)
                )
            }
            if (!isUnlocked) {
                PetPremiumBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(dimensionResource(SdpR.dimen._6sdp))
                        .fillMaxWidth(PET_PREMIUM_BADGE_PX / PET_CARD_WIDTH_PX)
                )
            }
            if (isDownloading) {
                PetDownloadingOverlay(modifier = Modifier.fillMaxSize())
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(SdpR.dimen._3sdp),
                    bottom = dimensionResource(SdpR.dimen._9sdp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayName,
                color = colorResource(R.color.colors_212327),
                fontFamily = StoreRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.pet_store_breed, pet.category),
                color = colorResource(R.color.colors_FDA3C0),
                fontFamily = StoreRoboto,
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BoxScope.PetDownloadingOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pet-download")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pet-download-rotation"
    )
    Box(
        modifier = modifier
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 0.72f },
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._22sdp))
                .rotate(rotation),
            color = colorResource(R.color.colors_FFFFFF),
            strokeWidth = dimensionResource(SdpR.dimen._2sdp)
        )
    }
}

@Composable
private fun FoodGrid(onFood: (PetStoreFood) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = dimensionResource(SdpR.dimen._12sdp), end = dimensionResource(SdpR.dimen._12sdp), bottom = dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        items(PET_FOOD_CATALOG, key = PetStoreFood::id) { food -> FoodCard(food) { onFood(food) } }
    }
}

@Composable
internal fun FoodCard(
    food: PetStoreFood,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(FOOD_CARD_WIDTH_PX / FOOD_CARD_HEIGHT_PX)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(
                dimensionResource(SdpR.dimen._1sdp),
                colorResource(R.color.colors_FFECD4),
                shape
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FOOD_CARD_WIDTH_PX / FOOD_CARD_IMAGE_AREA_HEIGHT_PX)
        ) {
            val itemWidth = maxWidth
            val imageAreaHeight = maxHeight
            Image(
                painter = painterResource(food.imageRes),
                contentDescription = food.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = imageAreaHeight * (19f / FOOD_CARD_IMAGE_AREA_HEIGHT_PX))
                    .fillMaxWidth(FOOD_CARD_IMAGE_SIZE_PX / FOOD_CARD_WIDTH_PX)
                    .aspectRatio(1f)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = itemWidth * (6f / FOOD_CARD_WIDTH_PX),
                        y = imageAreaHeight * (6f / FOOD_CARD_IMAGE_AREA_HEIGHT_PX)
                    )
                    .width(itemWidth * (32f / FOOD_CARD_WIDTH_PX))
                    .height(imageAreaHeight * (18f / FOOD_CARD_IMAGE_AREA_HEIGHT_PX))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_FFF1B2))
                    .padding(horizontal = itemWidth * (4f / FOOD_CARD_WIDTH_PX)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.img_pet_store_coin),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(itemWidth * (9f / FOOD_CARD_WIDTH_PX))
                        .height(imageAreaHeight * (12f / FOOD_CARD_IMAGE_AREA_HEIGHT_PX))
                )
                Spacer(Modifier.width(itemWidth * (2f / FOOD_CARD_WIDTH_PX)))
                Text(
                    text = food.energyValue.toString(),
                    color = colorResource(R.color.colors_A54905),
                    fontFamily = StoreRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = itemWidth * (62f / FOOD_CARD_WIDTH_PX),
                        y = imageAreaHeight * (66f / FOOD_CARD_IMAGE_AREA_HEIGHT_PX)
                    )
                    .width(itemWidth * (25f / FOOD_CARD_WIDTH_PX))
                    .height(imageAreaHeight * (18f / FOOD_CARD_IMAGE_AREA_HEIGHT_PX))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_8D6037))
                    .border(
                        dimensionResource(SdpR.dimen._1sdp),
                        colorResource(R.color.colors_FFFFFF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.pet_store_food_quantity, 1),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = StoreRobotoMedium,
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(SdpR.dimen._3sdp),
                    bottom = dimensionResource(SdpR.dimen._9sdp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = food.name,
                color = colorResource(R.color.colors_212327),
                fontFamily = StoreRoboto,
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
private fun PetRewardSheet(pet: OwnerPetCatalogEntry, isDownloading: Boolean, message: String?, onDismiss: () -> Unit, onPremium: () -> Unit, onReward: () -> Unit) {
    RewardOfferSheet(onDismiss) {
        PetRewardSheetContent(
            pet = pet,
            isDownloading = isDownloading,
            message = message,
            onPremium = onPremium,
            onReward = onReward,
            showNativeAd = true
        )
    }
}

@Composable
internal fun ColumnScope.PetRewardSheetContent(
    pet: OwnerPetCatalogEntry,
    isDownloading: Boolean,
    message: String?,
    onPremium: () -> Unit,
    onReward: () -> Unit,
    showNativeAd: Boolean
) {
    RewardPetPreview(pet = pet, isDownloading = isDownloading)
    Text(
        text = stringResource(R.string.pet_store_unlock_title),
        color = colorResource(R.color.colors_212327),
        fontFamily = StoreRobotoMedium,
        fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
        lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
        textAlign = TextAlign.Center
    )
    message?.let {
        Text(
            text = it,
            color = colorResource(R.color.colors_FB3675),
            fontFamily = StoreRoboto,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
            textAlign = TextAlign.Center
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))
    ) {
        RewardOutlineButton(
            text = stringResource(R.string.pet_store_unlimited),
            onClick = onPremium,
            modifier = Modifier.weight(1f),
            enabled = !isDownloading,
            iconRes = R.drawable.img_pet_store_premium_crown
        )
        RewardGradientButton(
            text = if (isDownloading) {
                stringResource(R.string.pet_store_downloading)
            } else {
                stringResource(R.string.pet_store_get_free)
            },
            onClick = onReward,
            modifier = Modifier.weight(1f),
            enabled = !isDownloading,
            iconRes = if (isDownloading) null else R.drawable.ic_pet_store_reward_video
        )
    }
    if (showNativeAd) {
        NativeAdInternal(
            screenCode = DIALOG_PET_REWARD,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FoodRewardSheet(food: PetStoreFood, onDismiss: () -> Unit, onPremium: () -> Unit, onAcquire: () -> Unit) {
    RewardOfferSheet(onDismiss) {
        RewardFoodPreview(food)
        Text(stringResource(R.string.pet_store_food_reward_title), color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._14ssp).value.sp, lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            RewardOutlineButton(stringResource(R.string.pet_store_unlimited), onPremium, Modifier.weight(1f), iconRes = R.drawable.img_pet_store_premium_crown)
            RewardGradientButton(stringResource(R.string.pet_store_get_free), onAcquire, Modifier.weight(1f), iconRes = R.drawable.ic_pet_store_reward_video)
        }
        NativeAdInternal(
            screenCode = DIALOG_FOOD_REWARD,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
@Suppress("DEPRECATION")
private fun HideDialogNavigationBar() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window == null) {
            onDispose {}
        } else {
            val decorView = window.decorView
            val controller = WindowInsetsControllerCompat(window, decorView)
            fun hideNavigationBar() {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.navigationBarColor = AndroidColor.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                controller.isAppearanceLightNavigationBars = false
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }

            val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus) hideNavigationBar()
            }
            hideNavigationBar()
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

            onDispose {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                }
            }
        }
    }
}

@Composable
private fun RewardPetPreview(
    pet: OwnerPetCatalogEntry,
    isDownloading: Boolean
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Box(
        modifier = Modifier
            .fillMaxWidth(REWARD_PET_CARD_WIDTH_PX / REWARD_SHEET_CONTENT_WIDTH_PX)
            .aspectRatio(REWARD_PET_CARD_WIDTH_PX / PET_CARD_HEIGHT_PX)
    ) {
        Column(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(colorResource(R.color.colors_FFFEF9))
                .border(
                    dimensionResource(SdpR.dimen._1sdp),
                    colorResource(R.color.colors_FFECD4),
                    shape
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        REWARD_PET_CARD_WIDTH_PX / PET_CARD_IMAGE_AREA_HEIGHT_PX
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = dimensionResource(SdpR.dimen._5sdp))
                        .fillMaxWidth(PET_SHADOW_WIDTH_PX / REWARD_PET_CARD_WIDTH_PX)
                        .aspectRatio(PET_SHADOW_WIDTH_PX / PET_SHADOW_HEIGHT_PX)
                        .clip(CircleShape)
                        .background(colorResource(R.color.colors_000000).copy(alpha = 0.05f))
                )
                if (pet.thumbnailPath == null) {
                    Image(
                        painter = painterResource(R.drawable.img_home_brand_bunny),
                        contentDescription = pet.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = dimensionResource(SdpR.dimen._8sdp))
                            .fillMaxWidth(
                                REWARD_PET_IMAGE_SIZE_PX / REWARD_PET_CARD_WIDTH_PX
                            )
                            .aspectRatio(1f)
                    )
                } else {
                    AsyncImage(
                        model = pet.thumbnailPath,
                        contentDescription = pet.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = dimensionResource(SdpR.dimen._8sdp))
                            .fillMaxWidth(
                                REWARD_PET_IMAGE_SIZE_PX / REWARD_PET_CARD_WIDTH_PX
                            )
                            .aspectRatio(1f)
                    )
                }
            }
            Text(
                text = pet.name,
                color = colorResource(R.color.colors_212327),
                fontFamily = StoreRobotoMedium,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._15ssp).value.sp
            )
            Text(
                text = stringResource(R.string.pet_store_breed, pet.category),
                color = colorResource(R.color.colors_FDA3C0),
                fontFamily = StoreRoboto,
                fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._12ssp).value.sp
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_pet_store_reward_tape),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = -dimensionResource(SdpR.dimen._11sdp),
                    y = -dimensionResource(SdpR.dimen._5sdp)
                )
                .fillMaxWidth(REWARD_TAPE_WIDTH_PX / REWARD_PET_CARD_WIDTH_PX)
                .aspectRatio(REWARD_TAPE_WIDTH_PX / REWARD_TAPE_HEIGHT_PX)
        )
        if (isDownloading) {
            PetDownloadingOverlay(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
            )
        }
    }
}

@Composable
private fun RewardFoodPreview(food: PetStoreFood) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = Modifier.width(dimensionResource(SdpR.dimen._95sdp)).height(dimensionResource(SdpR.dimen._94sdp)).clip(shape).background(colorResource(R.color.colors_FFFEF9)).border(1.dp, colorResource(R.color.colors_FFECD4), shape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(dimensionResource(SdpR.dimen._69sdp)), contentAlignment = Alignment.Center) {
            Image(painterResource(food.imageRes), food.name, Modifier.size(dimensionResource(SdpR.dimen._54sdp)))
            Row(Modifier.align(Alignment.TopStart).padding(dimensionResource(SdpR.dimen._5sdp)).clip(CircleShape).background(colorResource(R.color.colors_FFF1B2)).padding(horizontal = dimensionResource(SdpR.dimen._4sdp)), verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = Color(0xFFFFB32C), fontSize = 7.sp)
                Text(food.energyValue.toString(), color = colorResource(R.color.colors_A54905), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp)
            }
            Text("x1", color = Color.White, fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = dimensionResource(SdpR.dimen._14sdp)).clip(CircleShape).background(colorResource(R.color.colors_8D6037)).border(1.dp, Color.White, CircleShape).padding(horizontal = dimensionResource(SdpR.dimen._5sdp)))
        }
        Text(food.name, color = colorResource(R.color.colors_212327), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp)
    }
}

@Composable
private fun PetUnlockReveal(
    pet: OwnerPetCatalogEntry,
    pack: PetPack?,
    onContinue: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        HideDialogNavigationBar()
        PetUnlockRevealContent(
            pet = pet,
            pack = pack,
            onContinue = onContinue
        )
    }
}

@Composable
internal fun PetUnlockRevealContent(
    pet: OwnerPetCatalogEntry,
    pack: PetPack?,
    onContinue: () -> Unit,
    lightingProgress: Float? = null
) {
    StoreUnlockRevealContent(
        titleRes = R.drawable.img_pet_unlock_new_pet,
        titleContentDescription = stringResource(R.string.pet_store_new_pet),
        titleWidthPx = PET_UNLOCK_TITLE_WIDTH_PX,
        onContinue = onContinue,
        lightingProgress = lightingProgress
    ) {
        PetSpecialSkillPreview(
            pet = pet,
            pack = pack,
            modifier = Modifier
                .fillMaxWidth(UNLOCK_HERO_SIZE_PX / UNLOCK_LIGHTING_SIZE_PX)
                .aspectRatio(1f)
        )
    }
}

@Composable
private fun StoreUnlockRevealContent(
    titleRes: Int,
    titleContentDescription: String,
    titleWidthPx: Float,
    onContinue: () -> Unit,
    lightingProgress: Float? = null,
    hero: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onContinue
            )
    ) {
        val frameHeight = maxHeight
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = frameHeight * (244f / 800f))
                .fillMaxWidth(UNLOCK_LIGHTING_SIZE_PX / UNLOCK_FRAME_WIDTH_PX)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            StoreUnlockLighting(
                progress = lightingProgress,
                modifier = Modifier.fillMaxSize()
            )
            hero()
        }

        Image(
            painter = painterResource(titleRes),
            contentDescription = titleContentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = frameHeight * (237f / 800f))
                .fillMaxWidth(titleWidthPx / UNLOCK_FRAME_WIDTH_PX)
                .aspectRatio(titleWidthPx / UNLOCK_TITLE_HEIGHT_PX)
        )

        Text(
            text = stringResource(R.string.pet_store_tap_continue),
            color = colorResource(R.color.colors_FFFFFF),
            fontFamily = StoreRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = frameHeight * (554f / 800f))
        )
    }
}

@Composable
private fun StoreUnlockLighting(progress: Float?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val composition = remember(context) {
        LottieCompositionFactory.fromRawResSync(
            context,
            R.raw.anim_pet_unlock_lighting
        ).value
    }
    if (progress == null) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = modifier
        )
    } else {
        LottieAnimation(
            composition = composition,
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
        )
    }
}

@Composable
private fun PetSpecialSkillPreview(
    pet: OwnerPetCatalogEntry,
    pack: PetPack?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var visual by remember(pack?.key) { mutableStateOf<PetPackVisual?>(null) }
    val availableActions = remember(pack) {
        pack?.manifest?.clips
            ?.filterValues { clip -> clip.frames.isNotEmpty() }
            ?.keys
            .orEmpty()
    }
    val specialAction = remember(availableActions) {
        PetStorePolicy.specialSkillAction(availableActions)
    }

    LaunchedEffect(pack?.key) {
        visual = pack?.let { installedPack ->
            withContext(Dispatchers.IO) {
                PetBitmapCache(context.applicationContext).prepare(installedPack)
            }
        }
    }

    val sprite = visual as? PetPackVisual.Sprite
    val specialFrames = specialAction?.let { sprite?.frames?.get(it) }.orEmpty()
    if (specialAction != null && specialFrames.isNotEmpty()) {
        PetSpecialSkillSprite(
            frames = specialFrames,
            durationsMillis = pack?.manifest?.clips
                ?.get(specialAction)
                ?.frames
                ?.map { it.durationMillis }
                .orEmpty(),
            modifier = modifier
        )
    } else {
        if (pet.thumbnailPath != null) {
            AsyncImage(
                model = pet.thumbnailPath,
                contentDescription = pet.name,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        } else {
            Image(
                painter = painterResource(R.drawable.img_home_brand_bunny),
                contentDescription = pet.name,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PetSpecialSkillSprite(
    frames: List<com.asianmobile.emojibattery.shimeji.pet.pack.PetSpriteFrame>,
    durationsMillis: List<Long>,
    modifier: Modifier = Modifier
) {
    var frameIndex by remember(frames) { mutableIntStateOf(0) }
    LaunchedEffect(frames, durationsMillis) {
        frameIndex = 0
        while (frames.isNotEmpty()) {
            val currentIndex = frameIndex % frames.size
            delay(
                durationsMillis
                    .getOrElse(currentIndex) { 180L }
                    .coerceIn(80L, 2_000L)
            )
            frameIndex = (currentIndex + 1) % frames.size
        }
    }

    val frame = frames.getOrNull(frameIndex) ?: return
    val bitmap = remember(frame.bitmap) { frame.bitmap.asImageBitmap() }
    Canvas(modifier = modifier) {
        val source = frame.source
        if (source.width() <= 0 || source.height() <= 0) return@Canvas
        val scale = min(size.width / source.width(), size.height / source.height())
        val destinationWidth = (source.width() * scale).roundToInt()
        val destinationHeight = (source.height() * scale).roundToInt()
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(source.left, source.top),
            srcSize = IntSize(source.width(), source.height()),
            dstOffset = IntOffset(
                x = ((size.width - destinationWidth) / 2f).roundToInt(),
                y = ((size.height - destinationHeight) / 2f).roundToInt()
            ),
            dstSize = IntSize(destinationWidth, destinationHeight)
        )
    }
}

@Composable
private fun FoodUnlockReveal(food: PetStoreFood, onContinue: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        HideDialogNavigationBar()
        FoodUnlockRevealContent(food = food, onContinue = onContinue)
    }
}

@Composable
internal fun FoodUnlockRevealContent(
    food: PetStoreFood,
    onContinue: () -> Unit,
    lightingProgress: Float? = null
) {
    StoreUnlockRevealContent(
        titleRes = R.drawable.img_food_unlock_new_food,
        titleContentDescription = stringResource(R.string.pet_store_new_food),
        titleWidthPx = FOOD_UNLOCK_TITLE_WIDTH_PX,
        onContinue = onContinue,
        lightingProgress = lightingProgress
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(UNLOCK_HERO_SIZE_PX / UNLOCK_LIGHTING_SIZE_PX)
                .aspectRatio(1f)
        ) {
            Image(
                painter = painterResource(food.imageRes),
                contentDescription = food.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * (FOOD_QUANTITY_X_IN_HERO_PX / UNLOCK_HERO_SIZE_PX),
                        y = maxHeight * (FOOD_QUANTITY_Y_IN_HERO_PX / UNLOCK_HERO_SIZE_PX)
                    )
                    .width(maxWidth * (FOOD_QUANTITY_WIDTH_PX / UNLOCK_HERO_SIZE_PX))
                    .height(maxHeight * (FOOD_QUANTITY_HEIGHT_PX / UNLOCK_HERO_SIZE_PX))
                    .clip(CircleShape)
                    .background(colorResource(R.color.colors_8D6037))
                    .border(
                        width = dimensionResource(SdpR.dimen._1sdp),
                        color = colorResource(R.color.colors_FFFFFF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.pet_store_food_quantity, 1),
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = StoreRobotoSemiBold,
                    fontSize = dimensionResource(SspR.dimen._18ssp).value.sp,
                    lineHeight = dimensionResource(SspR.dimen._25ssp).value.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PetNameDialog(pet: OwnerPetCatalogEntry, onSave: (String) -> Unit) {
    var name by remember(pet.id) { mutableStateOf(pet.name) }
    var isSuggestedName by remember(pet.id) { mutableStateOf(true) }
    val inputTextColor = colorResource(
        if (isSuggestedName) R.color.colors_6F7073 else R.color.colors_212327
    )
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth(320f / 360f).clip(RoundedCornerShape(dimensionResource(SdpR.dimen._18sdp))).background(Color.White).padding(horizontal = dimensionResource(SdpR.dimen._12sdp), vertical = dimensionResource(SdpR.dimen._18sdp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
        ) {
            AsyncImage(model = pet.thumbnailPath ?: R.drawable.img_home_brand_bunny, contentDescription = pet.name, contentScale = ContentScale.Fit, modifier = Modifier.size(dimensionResource(SdpR.dimen._105sdp)))
            Text(stringResource(R.string.pet_store_name_title), color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoSemiBold, fontSize = dimensionResource(SspR.dimen._16ssp).value.sp, textAlign = TextAlign.Center)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 24) {
                        name = it
                        isSuggestedName = false
                    }
                },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.pet_store_name_hint)) },
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = inputTextColor,
                    unfocusedTextColor = inputTextColor,
                    cursorColor = colorResource(R.color.colors_FB3675),
                    errorCursorColor = colorResource(R.color.colors_FB3675),
                    focusedPlaceholderColor = colorResource(R.color.colors_9B9C9E),
                    unfocusedPlaceholderColor = colorResource(R.color.colors_9B9C9E),
                    focusedIndicatorColor = colorResource(R.color.colors_FB3675),
                    unfocusedIndicatorColor = colorResource(R.color.colors_C8C8C9)
                ),
                modifier = Modifier.fillMaxWidth().height(dimensionResource(SdpR.dimen._42sdp))
            )
            RewardGradientButton(stringResource(R.string.pet_store_save), { onSave(name) }, Modifier.width(dimensionResource(SdpR.dimen._155sdp)), enabled = name.isNotBlank())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PetStorePreview() {
    PetStoreContent(
        state = PetStoreUiState(
            isLoading = false,
            pets = listOf(
                OwnerPetCatalogEntry(1, "Cattey", "Cat", null, null, false),
                OwnerPetCatalogEntry(2, "Bunny", "Rabbit", null, null, false),
                OwnerPetCatalogEntry(3, "Bunny", "Rabbit", null, null, false)
            )
        ),
        onSearch = {},
        onPremium = {},
        onOpenMyPet = {},
        onToggle = {},
        onTab = {},
        onCategory = {},
        onPet = {},
        onFood = {}
    )
}
