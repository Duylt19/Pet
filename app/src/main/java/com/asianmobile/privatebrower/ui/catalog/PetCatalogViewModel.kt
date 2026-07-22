package com.asianmobile.privatebrower.ui.catalog

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.pet.pack.PetPackInstallResult
import com.asianmobile.privatebrower.pet.pack.PetPackInstaller
import com.asianmobile.privatebrower.pet.pack.PetPackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PetCatalogViewModel @Inject constructor(
    private val repository: PetPackRepository,
    private val installer: PetPackInstaller
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PetCatalogUiState(
            packs = repository.packs.value,
            selectedKey = repository.selectedPack.value.key
        )
    )
    val uiState: StateFlow<PetCatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.packs, repository.selectedPack) { packs, selected ->
                packs to selected.key
            }.collect { (packs, selectedKey) ->
                _uiState.update { it.copy(packs = packs, selectedKey = selectedKey) }
            }
        }
    }

    fun install(uri: Uri) {
        if (_uiState.value.isInstalling) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true, message = null) }
            when (val result = installer.install(uri)) {
                is PetPackInstallResult.Installed -> {
                    repository.refresh(preferredKey = result.pack.key)
                    _uiState.update {
                        it.copy(
                            isInstalling = false,
                            message = PetCatalogMessage.Installed(result.pack.manifest.name)
                        )
                    }
                }
                is PetPackInstallResult.Rejected -> _uiState.update {
                    it.copy(
                        isInstalling = false,
                        message = PetCatalogMessage.Rejected(result.reason)
                    )
                }
                is PetPackInstallResult.Failed -> _uiState.update {
                    it.copy(
                        isInstalling = false,
                        message = PetCatalogMessage.Failed(result.reason)
                    )
                }
            }
        }
    }

    fun select(key: String) {
        if (repository.select(key)) {
            val name = repository.find(key)?.manifest?.name ?: return
            _uiState.update { it.copy(message = PetCatalogMessage.Selected(name)) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
