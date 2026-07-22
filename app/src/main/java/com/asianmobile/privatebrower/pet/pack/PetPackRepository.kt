package com.asianmobile.privatebrower.pet.pack

import android.content.Context
import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetVector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PetPackRepository {
    val packs: StateFlow<List<PetPack>>
    val selectedPack: StateFlow<PetPack>
    fun find(key: String): PetPack?
    fun select(key: String): Boolean
    fun refresh(preferredKey: String? = null)
}

@Singleton
class FilePetPackRepository @Inject constructor(
    @param:ApplicationContext context: Context,
    private val diskLoader: PetPackDiskLoader
) : PetPackRepository {
    private val storageRoot = PetPackInstaller.packStorageRoot(context)
    private val builtIn = builtInPetPack()
    private val _packs = MutableStateFlow(listOf(builtIn))
    override val packs: StateFlow<List<PetPack>> = _packs.asStateFlow()
    private val _selectedPack = MutableStateFlow(builtIn)
    override val selectedPack: StateFlow<PetPack> = _selectedPack.asStateFlow()

    init {
        refresh()
    }

    override fun find(key: String): PetPack? = _packs.value.firstOrNull { it.key == key }

    override fun select(key: String): Boolean {
        val pack = find(key) ?: return false
        _selectedPack.value = pack
        return true
    }

    @Synchronized
    override fun refresh(preferredKey: String?) {
        val installedRoot = File(storageRoot, "installed")
        val installed = installedRoot.listFiles().orEmpty()
            .filter(File::isDirectory)
            .flatMap { idDirectory -> idDirectory.listFiles().orEmpty().filter(File::isDirectory) }
            .mapNotNull { directory -> runCatching { diskLoader.load(directory) }.getOrNull() }
            .sortedWith(compareBy({ it.manifest.name.lowercase() }, { -it.manifest.version }))
        val updated = listOf(builtIn) + installed
        _packs.value = updated
        val requestedKey = preferredKey ?: _selectedPack.value.key
        _selectedPack.value = updated.firstOrNull { it.key == requestedKey } ?: builtIn
    }

    private fun builtInPetPack(): PetPack {
        fun frame(duration: Long, velocityX: Float = 0f) = PetPackFrame(
            file = "built-in",
            rect = PetPackFrameRect(0, 0, 128, 128),
            durationMillis = duration,
            velocity = PetVector(x = velocityX)
        )
        val clips = listOf(
            PetPackClip(PetAction.IDLE, true, null, List(4) { frame(180) }),
            PetPackClip(PetAction.WALK, true, null, List(4) { frame(120, 42f) }),
            PetPackClip(PetAction.TAPPED, false, PetAction.IDLE, List(3) { frame(100) }),
            PetPackClip(PetAction.DRAGGED, true, null, listOf(frame(200))),
            PetPackClip(PetAction.FLUNG, true, null, List(2) { frame(100) })
        ).associateBy(PetPackClip::action)
        return PetPack(
            manifest = PetPackManifest(
                schemaVersion = PET_PACK_SCHEMA_VERSION,
                id = "builtin.orange-cat",
                version = 1,
                name = "Orange Cat",
                author = "Cute Pet",
                canvas = PetPackCanvas(128, 128, 1f),
                anchor = PetPackAnchor(0.5f, 1f),
                interaction = PetPackInteraction(PetAction.TAPPED),
                clips = clips
            ),
            source = PetPackSource.BuiltIn
        )
    }
}
