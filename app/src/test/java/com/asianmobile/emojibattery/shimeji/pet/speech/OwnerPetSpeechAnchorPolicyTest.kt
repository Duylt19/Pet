package com.asianmobile.emojibattery.shimeji.pet.speech

import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogEntry
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.OwnerPetSpeechAnchor
import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPack
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackAnchor
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackCanvas
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackInteraction
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackManifest
import com.asianmobile.emojibattery.shimeji.pet.pack.PetPackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OwnerPetSpeechAnchorPolicyTest {
    @Test
    fun `legacy owner pack receives canonical catalog anchor without reinstall`() {
        val pack = pack(id = "owner.shimeji.558", version = 4, speechAnchor = null)
        val catalog = catalog(
            id = 558,
            speechAnchor = OwnerPetSpeechAnchor(0.421875f, 0.671875f)
        )

        val enriched = OwnerPetSpeechAnchorPolicy.enrich(pack, catalog)

        assertEquals(PetPackAnchor(0.421875f, 0.671875f), enriched.manifest.speechAnchor)
        assertEquals(pack.key, enriched.key)
        assertEquals(pack.source, enriched.source)
    }

    @Test
    fun `unsupported owner pet removes an old heuristic anchor`() {
        val pack = pack(
            id = "owner.shimeji.42",
            version = 6,
            speechAnchor = PetPackAnchor(0.5f, 0.25f)
        )

        val enriched = OwnerPetSpeechAnchorPolicy.enrich(
            pack = pack,
            catalog = catalog(id = 42, speechAnchor = null)
        )

        assertNull(enriched.manifest.speechAnchor)
    }

    @Test
    fun `revision seven keeps installed server anchor while catalog is unavailable`() {
        val anchor = PetPackAnchor(0.49f, 0.5f)
        val pack = pack(id = "owner.shimeji.556", version = 7, speechAnchor = anchor)

        val enriched = OwnerPetSpeechAnchorPolicy.enrich(
            pack = pack,
            catalog = OwnerPetCatalogSnapshot(isLoading = true)
        )

        assertSame(pack, enriched)
    }

    @Test
    fun `non owner pack is never changed by owner catalog`() {
        val pack = pack(
            id = "custom.pet",
            version = 1,
            speechAnchor = PetPackAnchor(0.2f, 0.7f)
        )

        val enriched = OwnerPetSpeechAnchorPolicy.enrich(
            pack = pack,
            catalog = catalog(
                id = 558,
                speechAnchor = OwnerPetSpeechAnchor(0.421875f, 0.671875f)
            )
        )

        assertSame(pack, enriched)
    }

    private fun catalog(
        id: Int,
        speechAnchor: OwnerPetSpeechAnchor?
    ) = OwnerPetCatalogSnapshot(
        entries = listOf(
            OwnerPetCatalogEntry(
                id = id,
                name = "Pet $id",
                category = "Test",
                author = null,
                thumbnailPath = null,
                hasLocalArchive = true,
                speechAnchor = speechAnchor
            )
        ),
        isLoading = false
    )

    private fun pack(
        id: String,
        version: Int,
        speechAnchor: PetPackAnchor?
    ) = PetPack(
        manifest = PetPackManifest(
            schemaVersion = 1,
            id = id,
            version = version,
            name = id,
            author = null,
            canvas = PetPackCanvas(128, 128, 1f),
            anchor = PetPackAnchor(0.5f, 1f),
            interaction = PetPackInteraction(PetAction.IDLE),
            clips = emptyMap(),
            speechAnchor = speechAnchor
        ),
        source = PetPackSource.Installed("/tmp/$id/$version")
    )
}
