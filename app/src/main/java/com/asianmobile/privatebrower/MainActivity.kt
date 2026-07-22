package com.asianmobile.privatebrower

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.asianmobile.privatebrower.navigation.AppNavGraph
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.ui.component.ExitDialog
import com.asianmobile.privatebrower.ui.main.MainViewModel
import com.asianmobile.privatebrower.ui.theme.BaseAppTheme
import com.asianmobile.privatebrower.utils.LanguageUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var currentRoute: String = ""
    private var showExitDialog by mutableStateOf(false)
    private var isExitingApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
