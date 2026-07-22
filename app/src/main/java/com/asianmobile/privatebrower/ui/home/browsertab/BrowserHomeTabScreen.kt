package com.asianmobile.privatebrower.ui.home.browsertab

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.config.SCREEN_HOME
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.navigation.Routes
import com.asianmobile.privatebrower.ui.component.AppHeaderBar
import com.asianmobile.privatebrower.ui.component.AppHeaderLeading
import com.asianmobile.privatebrower.ui.component.SearchBar
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView
import com.intuit.sdp.R as SdpR

@Composable
fun BrowserHomeTabScreen(
    onNavigate: (String) -> Unit,
    isVisible: Boolean = true,
    viewModel: BrowserHomeTabViewModel = hiltViewModel()
) {
    TrackScreenView(ScreenName.HOME_BROWSER, isVisible = isVisible)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spokenText.isNotEmpty()) {
                viewModel.onVoiceSearchResult(spokenText)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.home_voice_search_no_result),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    LaunchedEffect(viewModel.navigateEvent) {
        viewModel.navigateEvent.collect { route ->
            onNavigate(route)
        }
    }

    // Refresh premium status when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.checkPremiumStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
    ) {
        // 0. App Header Bar
        AppHeaderBar(
            title = stringResource(R.string.home_header_title),
            leadingIcon = AppHeaderLeading.Settings,
            onLeadingClick = { onNavigate(Routes.SETTINGS) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
                .padding(bottom = dimensionResource(SdpR.dimen._16sdp))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

            // 1. Search Bar with default / selected engine icon
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onSubmit = viewModel::onSearchSubmit,
                leadingIconRes = uiState.searchEngine.iconRes,
                showMic = true,
                onMicClick = {
                    val intent = createHomeVoiceSearchIntent(context)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
                        runCatching { voiceSearchLauncher.launch(intent) }
                            .onFailure { showVoiceSearchUnavailable(context) }
                    } else {
                        showVoiceSearchUnavailable(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

            // 2. Private Browsing info card
            PrivateBrowsingCard(
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Native Ad Card (Hide if user has Premium)
            if (!uiState.isPremium) {
                Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._9sdp)))
                NativeAdInternal(
                    screenCode = SCREEN_HOME,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._9sdp)))

            // 4. Popular Sites grid (4 columns, 2 rows)
            PopularSitesSection(
                sites = PopularSites.DEFAULTS,
                onSiteClick = { site -> viewModel.onPopularSiteClicked(site) }
            )
        }
    }
}

private fun createHomeVoiceSearchIntent(context: Context): Intent {
    val languagePreferences = context.getSharedPreferences(
        "language_cache",
        Context.MODE_PRIVATE
    )
    val language = languagePreferences.getString("key_language", "en") ?: "en"
    val country = languagePreferences.getString("country_language", "US") ?: "US"
    val localeTag = "$language-$country"

    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            context.getString(R.string.home_voice_search_prompt)
        )
    }
}

private fun showVoiceSearchUnavailable(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.home_voice_search_unavailable),
        Toast.LENGTH_SHORT
    ).show()
}
