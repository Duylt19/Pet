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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.rotate
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
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.BannerAd
import com.asianmobile.emojibattery.shimeji.ads.ui.compose.NativeAdInternal
import com.asianmobile.emojibattery.shimeji.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedAdResult
import com.asianmobile.emojibattery.shimeji.ads.ui.rewarded.RewardedVideoAds
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.pet.overlay.PetOverlay
import com.asianmobile.emojibattery.shimeji.ui.component.PinkLoveSticker
import com.asianmobile.emojibattery.shimeji.utils.ScreenName
import com.asianmobile.emojibattery.shimeji.utils.TrackScreenView
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlin.math.cos
import kotlin.math.sin

private const val PET_STORE_BOTTOM_BANNER_POSITION = "home_mode_bottom"
private val StoreRoboto = FontFamily.SansSerif
private val StoreRobotoMedium = FontFamily(Font(R.font.roboto_medium))
private val StoreRobotoSemiBold = FontFamily(Font(R.font.roboto_600))
private val StoreButtonGradient = Brush.horizontalGradient(
    listOf(Color(0xFFFFB65B), Color(0xFFFF6B80), Color(0xFFFF57EE))
)

@Composable
fun PetStoreScreen(
    onSearch: () -> Unit,
    onPremium: () -> Unit,
    onDiscover: () -> Unit,
    onBattery: () -> Unit,
    onMine: () -> Unit,
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
        onFood = viewModel::selectFood,
        onDiscover = onDiscover,
        onBattery = onBattery,
        onMine = onMine
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
    onFood: (PetStoreFood) -> Unit,
    onDiscover: () -> Unit,
    onBattery: () -> Unit,
    onMine: () -> Unit
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
        StoreHeader(onSearch, onPremium)
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            StoreEnableCard(state.isPetRunning, onToggle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(SdpR.dimen._84sdp))
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
        StoreBottomNavigation(onDiscover, onBattery, onMine)
        BannerAd(
            modifier = Modifier.fillMaxWidth(),
            adPosition = PET_STORE_BOTTOM_BANNER_POSITION
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun StoreHeader(onSearch: () -> Unit, onPremium: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._46sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.discover_brand_name),
            color = colorResource(R.color.colors_212327),
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            fontSize = dimensionResource(SspR.dimen._17ssp).value.sp
        )
        PinkLoveSticker(Modifier.size(dimensionResource(SdpR.dimen._25sdp)))
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._31sdp))
                .shadow(dimensionResource(SdpR.dimen._4sdp), CircleShape)
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_home_search),
                contentDescription = stringResource(R.string.discover_search),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._17sdp))
            )
        }
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._8sdp)))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(StoreButtonGradient)
                .clickable(onClick = onPremium)
                .padding(horizontal = dimensionResource(SdpR.dimen._8sdp), vertical = dimensionResource(SdpR.dimen._6sdp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._3sdp))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_premium),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(SdpR.dimen._16sdp))
            )
            Text("PRO", color = Color.White, fontFamily = StoreRobotoSemiBold, fontSize = dimensionResource(SspR.dimen._12ssp).value.sp)
        }
    }
}

@Composable
private fun StoreEnableCard(enabled: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._42sdp))
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0x22FF57EE), Color(0x2293E9FF))))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_FB3675), shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = dimensionResource(SdpR.dimen._10sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.pet_store_enable_pet),
            color = colorResource(R.color.colors_212327),
            fontFamily = StoreRobotoMedium,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            modifier = Modifier.weight(1f)
        )
        StoreSwitch(enabled)
    }
}

@Composable
private fun StoreSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = dimensionResource(SdpR.dimen._35sdp), height = dimensionResource(SdpR.dimen._20sdp))
            .clip(CircleShape)
            .background(if (checked) colorResource(R.color.colors_FB3675) else colorResource(R.color.colors_C8C8C9))
            .padding(dimensionResource(SdpR.dimen._2sdp))
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(dimensionResource(SdpR.dimen._16sdp))
                .clip(CircleShape)
                .background(Color.White)
        )
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
        StoreTab(PetStoreTab.PETS, selected, R.drawable.ic_home_pet_store, stringResource(R.string.pet_store_tab_pets), onTab, Modifier.weight(1f))
        StoreTab(PetStoreTab.FOOD, selected, R.drawable.img_pet_store_food_tab, stringResource(R.string.pet_store_tab_food), onTab, Modifier.weight(1f))
    }
}

@Composable
private fun StoreTab(tab: PetStoreTab, selected: PetStoreTab, imageRes: Int, label: String, onTab: (PetStoreTab) -> Unit, modifier: Modifier) {
    val active = tab == selected
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._10sdp))
    Row(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._35sdp))
            .clip(shape)
            .background(colorResource(if (active) R.color.colors_FFEBF1 else R.color.colors_F2F2F2))
            .border(dimensionResource(if (active) SdpR.dimen._2sdp else SdpR.dimen._1sdp), colorResource(if (active) R.color.colors_FB3675 else R.color.colors_C8C8C9), shape)
            .clickable { onTab(tab) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(imageRes), null, Modifier.size(dimensionResource(SdpR.dimen._18sdp)), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._5sdp)))
        Text(label, color = colorResource(if (active) R.color.colors_FB3675 else R.color.colors_6F7073), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._12ssp).value.sp)
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
private fun PetCard(pet: OwnerPetCatalogEntry, displayName: String, isUnlocked: Boolean, isDownloading: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._119sdp))
            .clip(shape)
            .background(colorResource(R.color.colors_FFFEF9))
            .border(dimensionResource(SdpR.dimen._1sdp), colorResource(R.color.colors_FFECD4), shape)
            .clickable(enabled = !isDownloading, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(dimensionResource(SdpR.dimen._75sdp)), contentAlignment = Alignment.Center) {
            if (pet.thumbnailPath == null) {
                Image(
                    painter = painterResource(R.drawable.img_home_brand_bunny),
                    contentDescription = displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._54sdp))
                )
            } else {
                AsyncImage(
                    model = pet.thumbnailPath,
                    contentDescription = displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._54sdp))
                )
            }
            if (!isUnlocked) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_premium),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.align(Alignment.TopEnd).padding(dimensionResource(SdpR.dimen._5sdp)).size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
            if (isDownloading) {
                CircularProgressIndicator(Modifier.size(dimensionResource(SdpR.dimen._24sdp)), color = colorResource(R.color.colors_FB3675), strokeWidth = 2.dp)
            }
        }
        Text(displayName, color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.pet_store_breed, pet.category), color = colorResource(R.color.colors_FDA3C0), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun StoreBottomNavigation(onDiscover: () -> Unit, onBattery: () -> Unit, onMine: () -> Unit) {
    Row(Modifier.fillMaxWidth().shadow(dimensionResource(SdpR.dimen._5sdp)).background(Color.White).padding(horizontal = dimensionResource(SdpR.dimen._6sdp))) {
        StoreBottomItem(R.drawable.ic_home_discover, stringResource(R.string.discover_tab_discover), false, onDiscover, Modifier.weight(1f))
        StoreBottomItem(R.drawable.ic_home_battery, stringResource(R.string.discover_tab_battery), false, onBattery, Modifier.weight(1f))
        StoreBottomItem(R.drawable.ic_home_pet_store, stringResource(R.string.discover_tab_pet_store), true, {}, Modifier.weight(1f))
        StoreBottomItem(R.drawable.ic_home_mine, stringResource(R.string.discover_tab_mine), false, onMine, Modifier.weight(1f))
    }
}

@Composable
private fun StoreBottomItem(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.height(dimensionResource(SdpR.dimen._62sdp)).clickable(onClick = onClick).padding(top = dimensionResource(SdpR.dimen._9sdp), bottom = dimensionResource(SdpR.dimen._9sdp)), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.clip(CircleShape).background(if (selected) colorResource(R.color.colors_FFEBF1) else Color.White).padding(horizontal = dimensionResource(SdpR.dimen._15sdp), vertical = dimensionResource(SdpR.dimen._3sdp))) {
            Image(painterResource(iconRes), label, Modifier.size(dimensionResource(SdpR.dimen._18sdp)))
        }
        Text(label, color = colorResource(if (selected) R.color.colors_FB3675 else R.color.colors_6F7073), fontFamily = if (selected) StoreRobotoSemiBold else StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp)
    }
}

@Composable
private fun PetRewardSheet(pet: OwnerPetCatalogEntry, isDownloading: Boolean, message: String?, onDismiss: () -> Unit, onPremium: () -> Unit, onReward: () -> Unit) {
    StoreRewardSheet(onDismiss) {
        RewardPetPreview(pet)
        Text(stringResource(R.string.pet_store_unlock_title), color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoSemiBold, fontSize = dimensionResource(SspR.dimen._14ssp).value.sp, textAlign = TextAlign.Center)
        message?.let { Text(it, color = colorResource(R.color.colors_FB3675), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._9ssp).value.sp, textAlign = TextAlign.Center) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            StoreOutlineButton(stringResource(R.string.pet_store_unlimited), onPremium, Modifier.weight(1f), enabled = !isDownloading, iconRes = R.drawable.ic_home_premium)
            StoreGradientButton(if (isDownloading) stringResource(R.string.pet_store_downloading) else stringResource(R.string.pet_store_get_free), onReward, Modifier.weight(1f), enabled = !isDownloading, iconRes = if (isDownloading) null else R.drawable.ic_video)
        }
        NativeAdInternal(screenCode = SCREEN_HOME, adTypeOverride = AdType.HEIGHT_222, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FoodRewardSheet(food: PetStoreFood, onDismiss: () -> Unit, onPremium: () -> Unit, onAcquire: () -> Unit) {
    StoreRewardSheet(onDismiss) {
        RewardFoodPreview(food)
        Text(stringResource(R.string.pet_store_food_reward_title), color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoSemiBold, fontSize = dimensionResource(SspR.dimen._14ssp).value.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
            StoreOutlineButton(stringResource(R.string.pet_store_unlimited), onPremium, Modifier.weight(1f), iconRes = R.drawable.ic_home_premium)
            StoreGradientButton(stringResource(R.string.pet_store_get_free), onAcquire, Modifier.weight(1f), iconRes = R.drawable.ic_video)
        }
        NativeAdInternal(screenCode = SCREEN_HOME, adTypeOverride = AdType.HEIGHT_222, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StoreRewardSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = dimensionResource(SdpR.dimen._18sdp), topEnd = dimensionResource(SdpR.dimen._18sdp))).background(Color.White).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}).padding(horizontal = dimensionResource(SdpR.dimen._9sdp), vertical = dimensionResource(SdpR.dimen._12sdp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
                content = content
            )
        }
    }
}

@Composable
private fun StoreGradientButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, iconRes: Int? = null) {
    Box(modifier.height(dimensionResource(SdpR.dimen._38sdp)).clip(CircleShape).background(if (enabled) StoreButtonGradient else Brush.horizontalGradient(listOf(Color.LightGray, Color.LightGray))).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._5sdp))) {
            iconRes?.let { Icon(painterResource(it), null, tint = Color.Unspecified, modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))) }
            Text(text, color = Color.White, fontFamily = StoreRobotoSemiBold, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp)
        }
    }
}

@Composable
private fun StoreOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, iconRes: Int? = null) {
    Box(modifier.height(dimensionResource(SdpR.dimen._38sdp)).clip(CircleShape).background(Color.White).border(dimensionResource(SdpR.dimen._1sdp), if (enabled) StoreButtonGradient else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)), CircleShape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._5sdp))) {
            iconRes?.let { Icon(painterResource(it), null, tint = Color.Unspecified, modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))) }
            Text(text, color = if (enabled) colorResource(R.color.colors_FB3675) else Color.Gray, fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp)
        }
    }
}

@Composable
private fun RewardPetPreview(pet: OwnerPetCatalogEntry) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
    Column(
        modifier = Modifier.width(dimensionResource(SdpR.dimen._95sdp)).height(dimensionResource(SdpR.dimen._109sdp)).clip(shape).background(colorResource(R.color.colors_FFFEF9)).border(1.dp, colorResource(R.color.colors_FFECD4), shape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(dimensionResource(SdpR.dimen._72sdp)), contentAlignment = Alignment.Center) {
            AsyncImage(model = pet.thumbnailPath ?: R.drawable.img_home_brand_bunny, contentDescription = pet.name, contentScale = ContentScale.Fit, modifier = Modifier.size(dimensionResource(SdpR.dimen._55sdp)))
            Box(Modifier.align(Alignment.TopStart).offset(x = -dimensionResource(SdpR.dimen._5sdp), y = dimensionResource(SdpR.dimen._6sdp)).rotate(-32f).size(width = dimensionResource(SdpR.dimen._38sdp), height = dimensionResource(SdpR.dimen._14sdp)).background(Color(0xAA9BEAF7)))
        }
        Text(pet.name, color = colorResource(R.color.colors_212327), fontFamily = StoreRobotoMedium, fontSize = dimensionResource(SspR.dimen._10ssp).value.sp)
        Text(stringResource(R.string.pet_store_breed, pet.category), color = colorResource(R.color.colors_FDA3C0), fontFamily = StoreRoboto, fontSize = dimensionResource(SspR.dimen._8ssp).value.sp)
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
        onSearch = {}, onPremium = {}, onToggle = {}, onTab = {}, onPet = {}, onFood = {}, onDiscover = {}, onBattery = {}, onMine = {}
    )
}
