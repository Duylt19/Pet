package com.asianmobile.privatebrower.ui.privacypolicy

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ui.component.AppHeaderBar
import com.asianmobile.privatebrower.ui.component.AppHeaderLeading
import com.asianmobile.privatebrower.utils.ScreenName
import com.asianmobile.privatebrower.utils.TrackScreenView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyPolicyScreen(
    url: String,
    onBack: () -> Unit
) {
    TrackScreenView(ScreenName.PRIVACY_POLICY)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_F8FAFF))
            .statusBarsPadding()
    ) {
        AppHeaderBar(
            title = stringResource(R.string.privacy_policy_title),
            leadingIcon = AppHeaderLeading.Back,
            onLeadingClick = onBack
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    loadUrl(url)
                }
            }
        )
    }
}
