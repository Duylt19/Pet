package com.asianmobile.emojibattery.shimeji.ui.petstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.ui.component.RewardOfferSheetSurface

private val previewPets = listOf(
    OwnerPetCatalogEntry(1, "Cattey", "Cat", null, null, false),
    OwnerPetCatalogEntry(2, "Bunny", "Rabbit", null, null, false),
    OwnerPetCatalogEntry(3, "Bunny", "Rabbit", null, null, false),
    OwnerPetCatalogEntry(4, "Cattey", "Cat", null, null, false),
    OwnerPetCatalogEntry(5, "Bunny", "Rabbit", null, null, false),
    OwnerPetCatalogEntry(6, "Bunny", "Rabbit", null, null, false)
)

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun PetStorePetTabScreenshotTest() {
    PreviewStore(PetStoreTab.PETS)
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun PetStoreFoodTabScreenshotTest() {
    PreviewStore(PetStoreTab.FOOD)
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 180)
@Composable
fun PetStoreFoodCardScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        FoodCard(
            food = PetStoreFood(
                id = "beef_stew",
                name = "Beef Stew",
                energyValue = 25,
                imageRes = R.drawable.img_pet_store_food_beef_stew
            ),
            onClick = {},
            modifier = Modifier.fillMaxWidth(104f / 360f)
        )
    }
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 300)
@Composable
fun PetStoreRewardSheetScreenshotTest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000).copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        RewardOfferSheetSurface {
            PetRewardSheetContent(
                pet = previewPets.first(),
                isDownloading = false,
                message = null,
                onPremium = {},
                onReward = {},
                showNativeAd = false
            )
        }
    }
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun PetStoreUnlockRevealScreenshotTest() {
    Box(modifier = Modifier.fillMaxSize()) {
        PreviewStore(PetStoreTab.PETS)
        PetUnlockRevealContent(
            pet = previewPets.first(),
            pack = null,
            onContinue = {},
            lightingProgress = 0.45f
        )
    }
}

@PreviewTest
@Preview(widthDp = 360, heightDp = 800)
@Composable
fun PetStoreFoodUnlockRevealScreenshotTest() {
    Box(modifier = Modifier.fillMaxSize()) {
        PreviewStore(PetStoreTab.FOOD)
        FoodUnlockRevealContent(
            food = PetStoreFood(
                id = "beef_stew",
                name = "Beef Stew",
                energyValue = 25,
                imageRes = R.drawable.img_pet_store_food_beef_stew
            ),
            onContinue = {},
            lightingProgress = 0.45f
        )
    }
}

@Composable
private fun PreviewStore(tab: PetStoreTab) {
    PetStoreContent(
        state = PetStoreUiState(
            pets = previewPets,
            installedPackKeys = setOf(previewPets[2].installedPackKey),
            selectedTab = tab,
            isLoading = false
        ),
        onSearch = {},
        onPremium = {},
        onToggle = {},
        onTab = {},
        onPet = {},
        onFood = {}
    )
}
