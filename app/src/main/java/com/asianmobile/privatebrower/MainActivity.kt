package com.asianmobile.privatebrower

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.asianmobile.privatebrower.ads.data.SharedPreferencesUtils
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialLauncherUtil
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.ads.utils.AdOverlayState
import com.asianmobile.privatebrower.data.local.DataStoreManager
import com.asianmobile.privatebrower.navigation.AppNavGraph
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.ui.component.ExitDialog
import com.asianmobile.privatebrower.ui.main.MainViewModel
import com.asianmobile.privatebrower.ui.theme.BaseAppTheme
import com.asianmobile.privatebrower.utils.LanguageUtil
import com.asianmobile.privatebrower.utils.permission.DownloadNotificationPermissionPolicy
import com.asianmobile.privatebrower.utils.permission.DownloadNotificationPermissionRequests
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val HOME_TAB_DOWNLOADS = 2
    }

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private val mainViewModel: MainViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    private var currentRoute: String = ""
    private var showExitDialog by mutableStateOf(false)
    private var isExitingApp = false
    var pendingHomeTab by mutableStateOf<Int?>(null)
        private set

    fun clearPendingHomeTab() {
        pendingHomeTab = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        applyDarkSystemBars()

        setContent {
            val mainUiState by mainViewModel.uiState.collectAsState()
            val startDestination = remember {
                if (intent.getBooleanExtra("skip_splash", false)) {
                    Routes.HOME
                } else {
                    Routes.SPLASH
                }
            }

            BaseAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (mainUiState.isReady()) {
                        val nextScreen = remember(mainUiState) {
                            mainUiState.getNextScreen()
                        }
                        AppNavGraph(
                            startDestination = startDestination,
                            nextScreenAfterSplash = nextScreen,
                            viewModel = mainViewModel,
                            onExitApp = ::exitApp,
                            onDestinationChanged = { route ->
                                currentRoute = route.orEmpty()
                            }
                        )
                    }
                }

                if (showExitDialog) {
                    ExitDialog(
                        onDismissRequest = { showExitDialog = false },
                        onExit = ::exitApp
                    )
                }
            }
        }

        setupAdOverlay()
        observeDownloadNotificationPermissionRequests()
        onBackPressedDispatcher.addCallback(this) {
            if (currentRoute == Routes.HOME) {
                showExitDialog = true
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        hideSystemNavigationBar()
        setupImmersiveReHide()
    }

    private fun handleNotificationIntent(intent: Intent) {
        if (intent.getBooleanExtra("navigate_to_downloads", false)) {
            pendingHomeTab = HOME_TAB_DOWNLOADS
        }
    }

    @Suppress("DEPRECATION")
    private fun setupImmersiveReHide() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                window.decorView.postDelayed({
                    if (hasWindowFocus()) hideSystemNavigationBar()
                }, 300)
            }
        }
    }

    private fun setupAdOverlay() {
        window.setBackgroundDrawableResource(android.R.color.black)
        val contentFrame = findViewById<ViewGroup>(android.R.id.content)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AdOverlayState.isAdShowing.collect { isShowing ->
                    for (index in 0 until contentFrame.childCount) {
                        contentFrame.getChildAt(index).visibility =
                            if (isShowing) View.INVISIBLE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun observeDownloadNotificationPermissionRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DownloadNotificationPermissionRequests.requests.collect {
                    requestDownloadNotificationPermissionIfNeeded()
                }
            }
        }
    }

    private suspend fun requestDownloadNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isGranted = ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (isGranted) return

        val requestCount = dataStoreManager.runtimePermissionRequestCount(permission)
        if (
            !DownloadNotificationPermissionPolicy.shouldRequest(
                sdkInt = Build.VERSION.SDK_INT,
                isGranted = isGranted,
                requestCount = requestCount
            )
        ) {
            return
        }

        dataStoreManager.markRuntimePermissionsRequested(listOf(permission))
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
        notificationPermissionLauncher.launch(permission)
    }

    private fun exitApp() {
        if (isExitingApp) return
        isExitingApp = true
        showExitDialog = false
        finishAffinity()
    }

    private fun hideSystemNavigationBar() {
        applyDarkSystemBars()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    @Suppress("DEPRECATION")
    private fun applyDarkSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemNavigationBar()
    }

    override fun onResume() {
        super.onResume()
        applyDarkSystemBars()
        if (
            AdOverlayState.isAdShowing.value &&
            !InterstitialUtil.getInstance().isShowing &&
            !InterstitialLauncherUtil.getInstance().isShowing &&
            InterstitialUtil.getInstance().openAd?.isShowing != true
        ) {
            AdOverlayState.hide()
        }
    }

    override fun onStart() {
        super.onStart()
        val shouldSuppressOpenAd =
            SharedPreferencesUtils.getIsPremium(this) ||
                currentRoute.startsWith(Routes.PREMIUM)

        if (
            !shouldSuppressOpenAd &&
            currentRoute.isNotEmpty() &&
            currentRoute != Routes.SPLASH &&
            !InterstitialUtil.getInstance().isShowing &&
            !InterstitialLauncherUtil.getInstance().isShowing &&
            InterstitialUtil.getInstance().openAd?.needShowOpenAds == true
        ) {
            InterstitialUtil.getInstance().openAd?.showAdIfAvailable(this, null)
        }
        InterstitialUtil.getInstance().openAd?.needShowOpenAds = !shouldSuppressOpenAd
    }

    override fun attachBaseContext(newBase: Context?) {
        val (key, country) = newBase?.let(::getLanguageSync) ?: ("" to "")
        val localizedContext = newBase?.let {
            LanguageUtil.updateBaseContextLocale(it, key, country)
        }
        super.attachBaseContext(localizedContext)
    }

    private fun getLanguageSync(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences("language_cache", MODE_PRIVATE)
        val key = prefs.getString("key_language", "").orEmpty()
        val country = prefs.getString("country_language", "").orEmpty()
        return key to country
    }
}
