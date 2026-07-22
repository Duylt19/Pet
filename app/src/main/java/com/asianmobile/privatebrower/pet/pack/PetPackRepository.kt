package com.asianmobile.privatebrower.pet.pack

import android.content.Context
import com.asianmobile.privatebrower.data.repository.PetSettingsRepository
import com.asianmobile.privatebrower.pet.engine.PetAction
import com.asianmobile.privatebrower.pet.engine.PetVector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface PetPackRepository {
    val packs: StateFlow<List<PetPack>>
    val selectedPack: StateFlow<PetPack>
    fun find(key: String): PetPack?
    fun select(key: String): Boolean
    fun refresh(preferredKey: String? = null)
}

@Singleton
class FilePetPackRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val diskLoader: PetPackDiskLoader,
    private val settingsRepository: PetSettingsRepository
) : PetPackRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storageRoot = PetPackInstaller.packStorageRoot(context)
    private val builtIn = builtInPetPack()
    private val _packs = MutableStateFlow(listOf(builtIn))
    override val packs: StateFlow<List<PetPack>> = _packs.asStateFlow()
    private val _selectedPack = MutableStateFlow(builtIn)
    override val selectedPack: StateFlow<PetPack> = _selectedPack.asStateFlow()

    init {
        refresh()
        scope.launch {
            settingsRepository.preferences
                .map { it.selectedPackKey }
                .distinctUntilChanged()
                .collect(::selectFromPreferences)
        }
    }

    override fun find(key: String): PetPack? = _packs.value.firstOrNull { it.key == key }

    override fun select(key: String): Boolean {
        val pack = find(key) ?: return false
        _selectedPack.value = pack
        settingsRepository.updateSelectedPack(pack.key)
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
        val requestedKey = preferredKey
            ?: settingsRepository.preferences.value.selectedPackKey
            .takeIf { it != builtIn.key }
            ?: _selectedPack.value.key
        val selected = updated.firstOrNull { it.key == requestedKey } ?: builtIn
        _selectedPack.value = selected
        if (preferredKey != null || selected.key != requestedKey) {
            settingsRepository.updateSelectedPack(selected.key)
        }
    }

    private fun selectFromPreferences(key: String) {
        find(key)?.let { _selectedPack.value = it }
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
