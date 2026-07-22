package com.asianmobile.privatebrower.ui.home.browsertab

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asianmobile.privatebrower.ads.data.SharedPreferencesUtils
import com.asianmobile.privatebrower.data.model.QuickAccessShortcut
import com.asianmobile.privatebrower.data.model.QuickAccessShortcuts
import com.asianmobile.privatebrower.data.model.SearchEngine
import com.asianmobile.privatebrower.data.repository.SearchEngineRepository
import com.asianmobile.privatebrower.data.util.UrlBuilder
import com.asianmobile.privatebrower.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserHomeTabViewModel @Inject constructor(
    private val searchEngineRepository: SearchEngineRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isPremium = MutableStateFlow(false)

    private val _navigateEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateEvent: SharedFlow<String> = _navigateEvent.asSharedFlow()
    private var pendingVoiceSearchJob: Job? = null

    init {
        checkPremiumStatus()
    }

    fun checkPremiumStatus() {
        _isPremium.value = SharedPreferencesUtils.getIsPremium(context)
    }

    val uiState: StateFlow<BrowserHomeTabUiState> = combine(
        _searchQuery,
        searchEngineRepository.observeCurrent(),
        _isPremium
    ) { query, engine, premium ->
        BrowserHomeTabUiState(
            searchQuery = query,
            searchEngine = engine,
            shortcuts = QuickAccessShortcuts.DEFAULTS,
            isPremium = premium
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BrowserHomeTabUiState()
    )

    fun onSearchQueryChanged(query: String) {
        pendingVoiceSearchJob?.cancel()
        _searchQuery.value = query
    }

    fun onSearchSubmit() {
        pendingVoiceSearchJob?.cancel()
        viewModelScope.launch {
            submitSearch(_searchQuery.value)
        }
    }

    fun onVoiceSearchResult(result: String) {
        val query = result.trim()
        if (query.isEmpty()) return

        pendingVoiceSearchJob?.cancel()
        _searchQuery.value = query
        pendingVoiceSearchJob = viewModelScope.launch {
            delay(VOICE_SEARCH_SUBMIT_DELAY_MS)
            if (_searchQuery.value == query) {
                submitSearch(query)
            }
        }
    }

    private suspend fun submitSearch(query: String) {
        val url = UrlBuilder.buildUrl(query, uiState.value.searchEngine)
        val encodedUrl = Uri.encode(url)
        _navigateEvent.emit(
            "${Routes.BROWSER_WEBVIEW}?url=$encodedUrl&incognito=false"
        )
    }

    fun onShortcutClicked(shortcut: QuickAccessShortcut) {
        val encodedUrl = Uri.encode(shortcut.url)
        val route = "${Routes.BROWSER_WEBVIEW}?url=$encodedUrl&incognito=false"
        
        viewModelScope.launch {
            _navigateEvent.emit(route)
        }
    }

    fun onBookmarksClicked() {
        viewModelScope.launch {
            _navigateEvent.emit(Routes.BOOKMARKS_HISTORY)
        }
    }

    fun onPopularSiteClicked(site: PopularSite) {
        val encodedUrl = Uri.encode(site.url)
        val route = "${Routes.BROWSER_WEBVIEW}?url=$encodedUrl&incognito=false"

        viewModelScope.launch {
            _navigateEvent.emit(route)
        }
    }

    private companion object {
        const val VOICE_SEARCH_SUBMIT_DELAY_MS = 1_000L
    }
}
