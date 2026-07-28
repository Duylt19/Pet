package com.asianmobile.emojibattery.shimeji.pet.speech

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackAnchor

object OwnerPetSpeechAnchorPolicy {
    fun enrich(
        pack: PetPack,
        catalog: OwnerPetCatalogSnapshot
    ): PetPack {
        val petId = OwnerPetCatalogEntry.installedPetId(pack.manifest.id) ?: return pack
        val resolvedAnchor = if (catalog.entries.isNotEmpty()) {
            catalog.entries
                .firstOrNull { it.id == petId }
                ?.speechAnchor
                ?.let { PetPackAnchor(x = it.x, y = it.y) }
        } else if (pack.manifest.version >= FIRST_SERVER_ANCHOR_PACK_VERSION) {
            pack.manifest.speechAnchor
        } else {
            null
        }
        if (pack.manifest.speechAnchor == resolvedAnchor) return pack
        return pack.copy(
            manifest = pack.manifest.copy(speechAnchor = resolvedAnchor)
        )
    }

    private const val FIRST_SERVER_ANCHOR_PACK_VERSION = 7
}
