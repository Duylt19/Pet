package com.asianmobile.privatebrower.ui.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.data.SharedPreferencesUtils
import com.asianmobile.privatebrower.ads.config.BANNER_HOME_BOTTOM
import com.asianmobile.privatebrower.ads.ui.compose.BannerAd
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.ui.component.ExitDialog
import com.asianmobile.privatebrower.ui.home.browsertab.BrowserHomeTabScreen
import com.asianmobile.privatebrower.ui.home.component.HomeBottomNavBar
import com.asianmobile.privatebrower.ui.home.filestab.FilesTabScreen
import com.asianmobile.privatebrower.ui.bookmarks.BookmarksScreen
import com.asianmobile.privatebrower.ui.home.progresstab.ProgressTabScreen
import com.asianmobile.privatebrower.ui.home.tabstab.TabsTabScreen
import com.asianmobile.privatebrower.ui.home.tabstab.TabMode
import com.asianmobile.privatebrower.ui.home.tabstab.TabsTabViewModel
import com.asianmobile.privatebrower.ui.bookmarks.BookmarksViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    nextScreen: (String) -> Unit,
    savedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
    onExitApp: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabsViewModel: TabsTabViewModel = hiltViewModel()
    val tabsUiState by tabsViewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = pagerState.currentPage
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    var isTabsSearchActive by remember { mutableStateOf(false) }
    var isBookmarksHistorySearchActive by remember { mutableStateOf(false) }
    val bookmarksViewModel: BookmarksViewModel = hiltViewModel()
    val isFullScreenSearchActive =
        (selectedTab == 1 && isTabsSearchActive) ||
            (selectedTab == 3 && isBookmarksHistorySearchActive)

    // Listen for targetTab from navigation (BrowserScreen, or the download notification).
    // Collect the state flow so it also reacts to values set AFTER first composition (e.g.
    // tapping the notification while Home is already the current screen).
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<Int?>("targetTab", null)?.collect { targetTab ->
            if (targetTab != null) {
                pagerState.scrollToPage(targetTab)
                savedStateHandle.remove<Int>("targetTab")
            }
        }
    }

    LaunchedEffect(selectedTab) {
        viewModel.setSelectedTab(selectedTab)
    }

    LaunchedEffect(Unit) {
        val isPremium = SharedPreferencesUtils.getIsPremium(context)
        viewModel.updatePremiumStatus(isPremium)
    }

    fun applyStatusBarForSelectedTab() {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
    }

    LaunchedEffect(selectedTab) {
        applyStatusBarForSelectedTab()
    }

    DisposableEffect(lifecycleOwner, selectedTab) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                applyStatusBarForSelectedTab()
                val isPremium = SharedPreferencesUtils.getIsPremium(context)
                viewModel.updatePremiumStatus(isPremium)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = true) {
        when {
            selectedTab > 0 -> {
                coroutineScope.launch {
                    pagerState.scrollToPage(0)
                }
            }
            else -> {
                showExitDialog = true
            }
        }
    }

    if (showExitDialog) {
        ExitDialog(
            onDismissRequest = { showExitDialog = false },
            onExit = onExitApp
        )
    }

    Scaffold(
        topBar = { },
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            Column {
                if (!isFullScreenSearchActive) {
                    HomeBottomNavBar(
                        selectedTab = selectedTab,
                        tabCount = tabsUiState.tabs.size,
                        onTabClick = { index ->
                            coroutineScope.launch {
                                if (pagerState.currentPage != index) {
                                    pagerState.scrollToPage(index)
                                }
                            }
                        }
                    )
                }
                if (!uiState.isPremium) {
                    BannerAd(
                        modifier = Modifier.fillMaxWidth(),
                        adPosition = BANNER_HOME_BOTTOM
                    )
                }
            }
        },
        containerColor = colorResource(R.color.colors_161718)
    ) { innerPadding ->
        val imeBottomPadding = WindowInsets.ime
            .asPaddingValues()
            .calculateBottomPadding()
        val contentBottomPadding = if (selectedTab == 1 && isTabsSearchActive) {
            (innerPadding.calculateBottomPadding() - imeBottomPadding).coerceAtLeast(0.dp)
        } else {
            innerPadding.calculateBottomPadding()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = contentBottomPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusProperties { canFocus = pagerState.currentPage == page }
                ) {
                    when (page) {
                        0 -> BrowserHomeTabScreen(
                            onNavigate = { nextScreen(it) },
                            isVisible = selectedTab == 0
                        )
                        1 -> TabsTabScreen(
                            onNavigate = { nextScreen(it) },
                            isVisible = selectedTab == 1,
                            onSearchModeChanged = { isActive ->
                                isTabsSearchActive = isActive
                            },
                            onNavigateToSelection = { mode ->
                                nextScreen(
                                    "${Routes.TAB_SELECTION}?incognito=${mode == TabMode.INCOGNITO}"
                                )
                            },
                            viewModel = tabsViewModel
                        )
                        2 -> ProgressTabScreen(
                            isVisible = selectedTab == 2,
                            onNavigateToHome = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            },
                            onNavigate = { nextScreen(it) },
                            isPremium = uiState.isPremium
                        )
                        3 -> BookmarksScreen(
                            isVisible = selectedTab == 3,
                            onNavigateToBrowser = { url ->
                                val encodedUrl = android.net.Uri.encode(url)
                                nextScreen("${Routes.BROWSER_WEBVIEW}?url=$encodedUrl")
                            },
                            viewModel = bookmarksViewModel,
                            onNavigateToSettings = { nextScreen(Routes.SETTINGS) },
                            onSearchModeChanged = { isActive ->
                                isBookmarksHistorySearchActive = isActive
                            }
                        )
                        4 -> FilesTabScreen(
                            onNavigate = { nextScreen(it) },
                            isVisible = selectedTab == 4
                        )
                    }
                }
            }
        }
    }
}
