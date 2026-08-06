package com.asianmobile.emojibattery.shimeji.ui.petstore

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry

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
