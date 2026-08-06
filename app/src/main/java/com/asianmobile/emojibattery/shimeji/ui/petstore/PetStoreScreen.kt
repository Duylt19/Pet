package com.asianmobile.emojibattery.shimeji.ui.petstore

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.ads.config.SCREEN_HOME
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.AdType
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.component.HomeEnableCard
import com.asianmobile.emojibattery.shimeji.ui.component.HomeHeader
import com.asianmobile.emojibattery.shimeji.ui.component.PinkLoveSticker
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlin.math.cos
import kotlin.math.sin

private val StoreRoboto = FontFamily.SansSerif
private val StoreRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val StoreRobotoSemiBold = FontFamily(Font(R.font.roboto_600))
private const val PET_CARD_WIDTH_PX = 104f
private const val PET_CARD_HEIGHT_PX = 142f
private const val PET_CARD_IMAGE_AREA_HEIGHT_PX = 90f
private const val PET_CARD_IMAGE_SIZE_PX = 64f
private const val PET_SHADOW_WIDTH_PX = 58f
private const val PET_SHADOW_HEIGHT_PX = 12f
private const val REWARD_SHEET_CONTENT_WIDTH_PX = 336f
private const val REWARD_PET_CARD_WIDTH_PX = 124f
private const val REWARD_PET_IMAGE_SIZE_PX = 70f
private const val REWARD_TAPE_WIDTH_PX = 52f
private const val REWARD_TAPE_HEIGHT_PX = 42f
@Composable
fun PetStoreScreen(
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onViewPet: () -> Unit,
    viewModel: PetStoreViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    TrackScreenView(ScreenName.PET_STORE)

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refreshPermissions() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onNotificationPermissionResult() }

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
                    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
                    runCatching { overlayLauncher.launch(PetOverlay.permissionIntent(context)) }
                        .onFailure {
                            overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        }
                }
                PetStoreEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onNotificationPermissionResult()
                    }
                }
            }
        }
    }

    PetStoreContent(
        state = state,
        onSearch = onSearch,
        onPremium = onPremium,
        onToggle = viewModel::togglePetOverlay,
        onTab = viewModel::selectTab,
        onPet = viewModel::selectPet,
        onFood = viewModel::selectFood
    )

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
        NewItemReveal(
            title = stringResource(R.string.pet_store_new_pet),
            imageModel = pet.thumbnailPath ?: R.drawable.img_home_brand_bunny,
            onContinue = viewModel::continueAfterReveal
        )
    }
    state.revealedFood?.let { food ->
        NewItemReveal(
            title = stringResource(R.string.pet_store_new_food),
            imageModel = food.imageRes,
            quantity = "x1",
            onContinue = viewModel::continueAfterFoodReveal
        )
    }
    state.namingPet?.let { pet ->
        PetNameDialog(pet = pet, onSave = viewModel::savePetName)
    }
    state.joinedPetName?.let { name ->
        StoreToast(
            text = stringResource(R.string.pet_store_joined, name),
            action = stringResource(R.string.pet_store_view),
            onDismiss = viewModel::dismissJoinedToast,
            onAction = {
                viewModel.dismissJoinedToast()
                onViewPet()
            }
        )
    }
    state.acquiredFood?.let { food ->
        StoreToast(
            text = stringResource(R.string.pet_store_food_received, food.name),
            action = null,
            onDismiss = viewModel::dismissFoodToast,
            onAction = {}
        )
    }
    state.message?.takeIf { state.selectedPet == null }?.let { message ->
        StoreToast(
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
    onToggle: () -> Unit,
    onTab: (PetStoreTab) -> Unit,
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
            checked = state.isPetRunning,
            onCheckedChange = onToggle
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
                .height(dimensionResource(SdpR.dimen._77sdp))
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)))
                .background(colorResource(R.color.colors_FFEBF1)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.pet_store_banner_placeholder),
                color = colorResource(R.color.colors_212327),
                fontFamily = StoreRobotoSemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = dimensionResource(SspR.dimen._16ssp).value.sp
            )
        }
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))
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
                PetStoreTab.PETS -> PetGrid(state, onPet)
                PetStoreTab.FOOD -> FoodGrid(onFood)
            }
        }
    }
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
            selectedImageRes = R.drawable.img_pet_store_tab_pet_selected,
            unselectedImageRes = R.drawable.img_pet_store_tab_pet_unselected,
            label = stringResource(R.string.pet_store_tab_pets),
            onTab = onTab,
            modifier = Modifier.weight(1f)
        )
        StoreTab(
            tab = PetStoreTab.FOOD,
            selected = selected,
            selectedImageRes = R.drawable.img_pet_store_tab_food_selected,
            unselectedImageRes = R.drawable.img_pet_store_food_tab,
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
    selectedImageRes: Int,
    unselectedImageRes: Int,
    label: String,
    onTab: (PetStoreTab) -> Unit,
    modifier: Modifier
) {
    val active = tab == selected
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Row(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._37sdp))
            .clip(shape)
            .background(
                colorResource(if (active) R.color.colors_FFEBF1 else R.color.colors_F2F2F2)
            )
            .border(
                width = dimensionResource(
                    if (active) SdpR.dimen._2sdp else SdpR.dimen._1sdp
                ),
                color = colorResource(
                    if (active) R.color.colors_FB3675 else R.color.colors_C8C8C9
                ),
                shape = shape
            )
            .clickable(enabled = !active) { onTab(tab) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                if (active) selectedImageRes else unselectedImageRes
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._3sdp)))
        Text(
            text = label,
            color = colorResource(
                if (active) R.color.colors_FB3675 else R.color.colors_6F7073
            ),
            fontFamily = StoreRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
        )
    }
}

@Composable
private fun PetGrid(state: PetStoreUiState, onPet: (OwnerPetCatalogEntry) -> Unit) {
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
        items(state.pets, key = OwnerPetCatalogEntry::id) { pet ->
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
                Image(
                    painter = painterResource(R.drawable.img_pet_store_premium_crown),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(dimensionResource(SdpR.dimen._5sdp))
                        .size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(dimensionResource(SdpR.dimen._18sdp)),
                    color = colorResource(R.color.colors_FB3675),
                    strokeWidth = dimensionResource(SdpR.dimen._2sdp)
                )
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
private fun FoodGrid(onFood: (PetStoreFood) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = dimensionResource(SdpR.dimen._12sdp), end = dimensionResource(SdpR.dimen._12sdp), bottom = dimensionResource(SdpR.dimen._12sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
    ) {
        items(PET_STORE_FOOD, key = PetStoreFood::id) { food -> FoodCard(food) { onFood(food) } }
    }
}

@Composable
private fun FoodCard(food: PetStoreFood, onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._102sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_FFECD4), shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(dimensionResource(SdpR.dimen._69sdp)), contentAlignment = Alignment.Center) {
            Image(painterResource(food.imageRes), food.name, Modifier.size(dimensionResource(SdpR.dimen._54sdp)))
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(dimensionResource(SdpR.dimen._5sdp)).clip(CircleShape).background(colorResource(R.color.colors_FFF1B2)).padding(horizontal = dimensionResource(SdpR.dimen._4sdp), vertical = dimensionResource(SdpR.dimen._1sdp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("●", color = Color(0xFFFFB32C), fontSize = 7.sp)
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._2sdp)))
                Text(food.coinCost.toString(), color = colorResource(R.color.colors_A54905), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp)
            }
            Text("x1", color = Color.White, fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = dimensionResource(SdpR.dimen._15sdp)).clip(CircleShape).background(colorResource(R.color.colors_8D6037)).border(1.dp, Color.White, CircleShape).padding(horizontal = dimensionResource(SdpR.dimen._5sdp)))
        }
        Text(food.name, color = colorResource(R.color.colors_212327), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PetRewardSheet(pet: OwnerPetCatalogEntry, isDownloading: Boolean, message: String?, onDismiss: () -> Unit, onPremium: () -> Unit, onReward: () -> Unit) {
    StoreRewardSheet(onDismiss) {
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
    RewardPetPreview(pet)
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
        StoreOutlineButton(
            text = stringResource(R.string.pet_store_unlimited),
            onClick = onPremium,
            modifier = Modifier.weight(1f),
            enabled = !isDownloading,
            iconRes = R.drawable.img_pet_store_premium_crown
        )
        StoreGradientButton(
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
            screenCode = SCREEN_HOME,
            adTypeOverride = AdType.HEIGHT_222,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FoodRewardSheet(food: PetStoreFood, onDismiss: () -> Unit, onPremium: () -> Unit, onAcquire: () -> Unit) {
    StoreRewardSheet(onDismiss) {
        RewardFoodPreview(food)
        Text(stringResource(R.string.pet_store_food_reward_title), color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._14ssp).value.sp, lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            StoreOutlineButton(stringResource(R.string.pet_store_unlimited), onPremium, Modifier.weight(1f), iconRes = R.drawable.img_pet_store_premium_crown)
            StoreGradientButton(stringResource(R.string.pet_store_get_free), onAcquire, Modifier.weight(1f), iconRes = R.drawable.ic_pet_store_reward_video)
        }
        NativeAdInternal(screenCode = SCREEN_HOME, adTypeOverride = AdType.HEIGHT_222, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StoreRewardSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            StoreRewardSheetSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                content = content
            )
        }
    }
}

@Composable
internal fun StoreRewardSheetSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = dimensionResource(SdpR.dimen._18sdp),
                    topEnd = dimensionResource(SdpR.dimen._18sdp)
                )
            )
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                start = dimensionResource(SdpR.dimen._9sdp),
                end = dimensionResource(SdpR.dimen._9sdp),
                top = dimensionResource(SdpR.dimen._15sdp),
                bottom = dimensionResource(SdpR.dimen._9sdp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
        content = content
    )
}

@Composable
private fun StoreGradientButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, iconRes: Int? = null) {
    val rewardGradient = Brush.horizontalGradient(
        listOf(colorResource(R.color.colors_C95DFF), colorResource(R.color.colors_FB54BB))
    )
    val disabled = colorResource(R.color.colors_C8C8C9)
    Box(modifier.height(dimensionResource(SdpR.dimen._38sdp)).clip(CircleShape).background(if (enabled) rewardGradient else Brush.horizontalGradient(listOf(disabled, disabled))).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            iconRes?.let { Image(painterResource(it), null, modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))) }
            Text(text, color = colorResource(R.color.colors_FFFFFF), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._12ssp).value.sp, lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp)
        }
    }
}

@Composable
private fun StoreOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, iconRes: Int? = null) {
    val rewardGradient = Brush.horizontalGradient(
        listOf(colorResource(R.color.colors_C95DFF), colorResource(R.color.colors_FB54BB))
    )
    val disabled = colorResource(R.color.colors_C8C8C9)
    val textStyle = if (enabled) {
        TextStyle(brush = rewardGradient)
    } else {
        TextStyle(color = disabled)
    }
    Box(modifier.height(dimensionResource(SdpR.dimen._38sdp)).clip(CircleShape).background(colorResource(R.color.colors_FFFFFF)).border(dimensionResource(SdpR.dimen._1sdp), if (enabled) rewardGradient else Brush.linearGradient(listOf(disabled, disabled)), CircleShape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            iconRes?.let { Image(painterResource(it), null, modifier = Modifier.size(dimensionResource(SdpR.dimen._18sdp))) }
            Text(text, style = textStyle, fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._12ssp).value.sp, lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp)
        }
    }
}

@Composable
private fun RewardPetPreview(pet: OwnerPetCatalogEntry) {
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
                Text(food.coinCost.toString(), color = colorResource(R.color.colors_A54905), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp)
            }
            Text("x1", color = Color.White, fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = dimensionResource(SdpR.dimen._14sdp)).clip(CircleShape).background(colorResource(R.color.colors_8D6037)).border(1.dp, Color.White, CircleShape).padding(horizontal = dimensionResource(SdpR.dimen._5sdp)))
        }
        Text(food.name, color = colorResource(R.color.colors_212327), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp)
    }
}

@Composable
private fun NewItemReveal(title: String, imageModel: Any, quantity: String? = null, onContinue: () -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).clickable(onClick = onContinue), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dimensionResource(SdpR.dimen._238sdp)), contentAlignment = Alignment.Center) {
                RevealRays()
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._10sdp))) {
                    Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = dimensionResource(SspR.dimen._21ssp).value.sp, fontStyle = FontStyle.Italic, modifier = Modifier.clip(RoundedCornerShape(dimensionResource(SdpR.dimen._7sdp))).background(colorResource(R.color.colors_FB3675)).padding(horizontal = dimensionResource(SdpR.dimen._6sdp), vertical = dimensionResource(SdpR.dimen._2sdp)))
                    Box {
                        AsyncImage(model = imageModel, contentDescription = title, contentScale = ContentScale.Fit, modifier = Modifier.size(dimensionResource(SdpR.dimen._134sdp)))
                        quantity?.let { Text(it, color = Color.White, fontFamily = StoreRobotoSemiBold, modifier = Modifier.align(Alignment.BottomEnd).clip(CircleShape).background(colorResource(R.color.colors_8D6037)).padding(horizontal = dimensionResource(SdpR.dimen._6sdp))) }
                    }
                    Text(stringResource(R.string.pet_store_tap_continue), color = Color.White, fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp)
                }
            }
        }
    }
}

@Composable
private fun RevealRays() {
    Canvas(Modifier.fillMaxSize()) {
        val center = center
        val radius = size.minDimension / 2f
        repeat(12) { index ->
            val start = Math.toRadians((index * 30f - 7f).toDouble())
            val end = Math.toRadians((index * 30f + 7f).toDouble())
            val path = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + cos(start).toFloat() * radius, center.y + sin(start).toFloat() * radius)
                lineTo(center.x + cos(end).toFloat() * radius, center.y + sin(end).toFloat() * radius)
                close()
            }
            drawPath(path, Color(0x55FFE756))
        }
    }
}

@Composable
private fun PetNameDialog(pet: OwnerPetCatalogEntry, onSave: (String) -> Unit) {
    var name by remember(pet.id) { mutableStateOf(pet.name) }
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
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.pet_store_name_hint)) },
                shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp)),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = colorResource(R.color.colors_FB3675), unfocusedIndicatorColor = colorResource(R.color.colors_C8C8C9)),
                modifier = Modifier.fillMaxWidth().height(dimensionResource(SdpR.dimen._42sdp))
            )
            StoreGradientButton(stringResource(R.string.pet_store_save), { onSave(name) }, Modifier.width(dimensionResource(SdpR.dimen._155sdp)), enabled = name.isNotBlank())
        }
    }
}

@Composable
private fun StoreToast(text: String, action: String?, onDismiss: () -> Unit, onAction: () -> Unit) {
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(3_000)
        onDismiss()
    }
    Box(Modifier.fillMaxSize().padding(bottom = dimensionResource(SdpR.dimen._91sdp)), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier.shadow(dimensionResource(SdpR.dimen._6sdp), CircleShape).clip(CircleShape).background(Color.White).padding(horizontal = dimensionResource(SdpR.dimen._12sdp), vertical = dimensionResource(SdpR.dimen._8sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
        ) {
            PinkLoveSticker(Modifier.size(dimensionResource(SdpR.dimen._20sdp)))
            Text(text, color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp, maxLines = 1)
            action?.let { Text(it, color = colorResource(R.color.colors_FB3675), fontFamily = StoreRobotoSemiBold, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp, modifier = Modifier.clickable(onClick = onAction)) }
        }
    }
}

private val PET_STORE_FOOD = listOf(
    PetStoreFood("beef_stew", "Beef Stew", 25, R.drawable.img_pet_store_food_beef_stew),
    PetStoreFood("grilled_salmon", "Grilled Salmon", 30, R.drawable.img_pet_store_food_grilled_salmon),
    PetStoreFood("meatball_pasta", "Meatball Pasta", 25, R.drawable.img_pet_store_food_meatball_pasta),
    PetStoreFood("vegetable_rice", "Vegetable Rice", 20, R.drawable.img_pet_store_food_vegetable_rice),
    PetStoreFood("fruit_bowl", "Fruit Bowl", 15, R.drawable.img_pet_store_food_fruit_bowl),
    PetStoreFood("roast_chicken", "Roast Chicken", 30, R.drawable.img_pet_store_food_roast_chicken),
    PetStoreFood("fried_egg", "Fried Egg", 15, R.drawable.img_pet_store_food_fried_egg),
    PetStoreFood("steak", "Steak", 35, R.drawable.img_pet_store_food_steak),
    PetStoreFood("vegetables", "Vegetables", 10, R.drawable.img_pet_store_food_vegetables),
    PetStoreFood("pet_treats", "Pet Treats", 10, R.drawable.img_pet_store_food_pet_treats)
)

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
        onToggle = {},
        onTab = {},
        onPet = {},
        onFood = {}
    )
}
