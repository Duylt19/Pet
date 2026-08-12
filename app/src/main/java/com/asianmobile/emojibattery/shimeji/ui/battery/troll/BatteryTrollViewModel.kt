package com.asianmobile.emojibattery.shimeji.ui.battery.troll

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.emojibattery.shimeji.ads.data.SharedPreferencesUtils
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollCatalogSnapshot
import com.asianmobile.emojibattery.shimeji.data.model.BatteryTrollEntry
import com.asianmobile.emojibattery.shimeji.data.repository.BatterySettingsRepository
import com.asianmobile.emojibattery.shimeji.data.repository.BatteryTrollCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the Battery Troll Themes grid. It mirrors `BatteryCatalogViewModel` on purpose: the
 * owner asked a troll theme to unlock exactly the way a battery style does, so the reward flow,
 * the premium re-check on resume and the pending-unlock bookkeeping are the same shape here.
 *
 * Reward unlocks are held in memory only — `BatteryStatusConfig` persists reward unlocks for the
 * battery catalog (`rewardUnlockedThemeIds`) and has no equivalent field for troll ids, so a
 * rewarded unlock survives the screen but not the process.
 */
@HiltViewModel
class BatteryTrollViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val catalogRepository: BatteryTrollCatalogRepository,
    private val settingsRepository: BatterySettingsRepository
) : ViewModel() {
    private val accessPolicy = BatteryTrollAccessPolicy()
    private val _uiState = MutableStateFlow(BatteryTrollUiState())
    val uiState: StateFlow<BatteryTrollUiState> = _uiState.asStateFlow()
    private val _effects = Channel<BatteryTrollEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(catalogRepository.snapshot, settingsRepository.config) { catalog, config ->
                catalog to config
            }.collect { (catalog, config) ->
                _uiState.update { current -> reduce(current, catalog, config) }
            }
        }
    }

    fun refresh() {
        refreshEntitlement()
        viewModelScope.launch { catalogRepository.refresh() }
    }

    /** Premium is bought outside this screen, so it is re-read every time the screen resumes. */
    fun refreshEntitlement() {
        val premium = SharedPreferencesUtils.getIsPremium(context)
        val pendingTrollId = _uiState.value.pendingUnlockTrollId
        _uiState.update {
            it.copy(
                isPremium = premium,
                pendingUnlockTrollId = if (premium) null else it.pendingUnlockTrollId,
                isRewardInProgress = if (premium) false else it.isRewardInProgress,
                message = if (premium) null else it.message
            )
        }
        if (premium && pendingTrollId != null) {
            emit(BatteryTrollEffect.OpenCustomize(pendingTrollId))
        }
    }

    fun requestTroll(troll: BatteryTrollEntry) {
        val state = _uiState.value
        when (accessPolicy.resolve(troll, state.isPremium, state.rewardUnlockedTrollIds)) {
            BatteryTrollAccess.OPEN -> emit(BatteryTrollEffect.OpenCustomize(troll.id))
            BatteryTrollAccess.REWARD_OR_PREMIUM -> _uiState.update {
                it.copy(
                    pendingUnlockTrollId = troll.id,
                    isRewardInProgress = false,
                    message = null
                )
            }
        }
    }

    fun dismissUnlockDialog() {
        if (_uiState.value.isRewardInProgress) return
        _uiState.update { it.copy(pendingUnlockTrollId = null, message = null) }
    }

    fun requestRewardUnlock() {
        val state = _uiState.value
        val troll = state.trolls.firstOrNull { it.id == state.pendingUnlockTrollId } ?: return
        if (state.isRewardInProgress ||
            accessPolicy.resolve(troll, state.isPremium, state.rewardUnlockedTrollIds) !=
            BatteryTrollAccess.REWARD_OR_PREMIUM
        ) {
            return
        }
        _uiState.update { it.copy(isRewardInProgress = true, message = null) }
        emit(BatteryTrollEffect.ShowRewardedAd)
    }

    fun onRewardResult(canContinue: Boolean) {
        val state = _uiState.value
        val trollId = state.pendingUnlockTrollId
        if (!state.isRewardInProgress || trollId == null) return
        if (!canContinue) {
            _uiState.update {
                it.copy(
                    isRewardInProgress = false,
                    message = BatteryTrollMessage.REWARD_NOT_EARNED
                )
            }
            return
        }
        settingsRepository.unlockTrollByReward(trollId)
        _uiState.update {
            it.copy(
                rewardUnlockedTrollIds = it.rewardUnlockedTrollIds + trollId,
                pendingUnlockTrollId = null,
                isRewardInProgress = false,
                message = null
            )
        }
        emit(BatteryTrollEffect.OpenCustomize(trollId))
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun reduce(
        current: BatteryTrollUiState,
        catalog: BatteryTrollCatalogSnapshot,
        config: BatteryStatusConfig
    ): BatteryTrollUiState = current.copy(
        trolls = catalog.trolls,
        selectedTrollId = config.trollThemeId,
        // Persisted, not held in memory: a reward the user watched an ad for must survive
        // process death, otherwise they pay twice for the same troll.
        rewardUnlockedTrollIds = config.rewardUnlockedTrollIds,
        isPremium = SharedPreferencesUtils.getIsPremium(context),
        pendingUnlockTrollId = current.pendingUnlockTrollId
            ?.takeIf { id -> catalog.trolls.any { it.id == id } },
        isLoading = catalog.isLoading,
        error = catalog.error
    )

    private fun emit(effect: BatteryTrollEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
