package com.asianmobile.privatebrower.ui.browser

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.MutableContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil
import com.asianmobile.privatebrower.data.browser.TabSession
import com.asianmobile.privatebrower.data.browser.asBrowserThemedContext
import com.asianmobile.privatebrower.utils.dialog.PermissionRequireDialog
import kotlinx.coroutines.launch

internal data class PendingGeolocationRequest(
    val origin: String,
    val callback: GeolocationPermissions.Callback
)

internal enum class BrowserAppSettingsTarget {
    WEB_CAPTURE,
    GEOLOCATION
}

private val BROWSER_NATIVE_HANDLED_INSETS =
    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

/**
 * Hosts the long-lived WebView owned by a tab without recreating or reloading it.
 *
 * The native browser shell has already positioned this container below its controls, so system
 * bar and display-cutout insets are zeroed here before Chromium exposes them as CSS safe areas.
 */
@Composable
internal fun BrowserTabWebViewHost(
    session: TabSession,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context -> BrowserWebViewContainer(context) },
        modifier = modifier,
        onRelease = BrowserWebViewContainer::releaseWebView,
        update = { container -> container.showWebView(session.webView) }
    )
}

private class BrowserWebViewContainer(context: Context) : FrameLayout(context) {
    private var hostedWebView: WebView? = null

    init {
        clipChildren = true
        clipToPadding = true
        setPadding(0, 0, 0, 0)
        setBackgroundColor(ContextCompat.getColor(context, R.color.colors_1C1C1D))

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(BROWSER_NATIVE_HANDLED_INSETS, Insets.NONE)
                .build()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
    }

    fun showWebView(webView: WebView) {
        if (hostedWebView === webView && webView.parent === this) return

        releaseWebView()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(0, 0, 0, 0)
            overScrollMode = View.OVER_SCROLL_NEVER
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = true
            isScrollbarFadingEnabled = true
        }
        addView(webView)
        hostedWebView = webView
        ViewCompat.requestApplyInsets(this)
    }

    fun releaseWebView() {
        hostedWebView?.takeIf { it.parent === this }?.let(::removeView)
        hostedWebView = null
    }
}

@Stable
class BrowserWebHostState internal constructor() {
    internal var filePathCallback: ValueCallback<Array<Uri>>? = null
    internal var pendingWebPermission by mutableStateOf<PermissionRequest?>(null)
    internal var pendingGeolocation by mutableStateOf<PendingGeolocationRequest?>(null)
    internal var appSettingsTarget by mutableStateOf<BrowserAppSettingsTarget?>(null)
    internal var appSettingsOpened by mutableStateOf(false)
    internal var customViewCallback: WebChromeClient.CustomViewCallback? = null
    var customView by mutableStateOf<View?>(null)
        private set

    internal var approveWebPermissionHandler: (() -> Unit)? = null
    internal var approveGeolocationHandler: (() -> Unit)? = null

    fun denyWebPermission() {
        pendingWebPermission?.let { request -> runCatching { request.deny() } }
        pendingWebPermission = null
    }

    fun denyGeolocation() {
        pendingGeolocation?.let { request ->
            request.callback.invoke(request.origin, false, false)
        }
        pendingGeolocation = null
    }

    fun approveWebPermission() {
        approveWebPermissionHandler?.invoke()
    }

    fun approveGeolocation() {
        approveGeolocationHandler?.invoke()
    }

    internal fun requestAppSettings(target: BrowserAppSettingsTarget) {
        appSettingsTarget = target
        appSettingsOpened = false
    }

    internal fun dismissAppSettingsPrompt() {
        when (appSettingsTarget) {
            BrowserAppSettingsTarget.WEB_CAPTURE -> denyWebPermission()
            BrowserAppSettingsTarget.GEOLOCATION -> denyGeolocation()
            null -> Unit
        }
        appSettingsTarget = null
        appSettingsOpened = false
    }

    internal fun resolveAfterAppSettings(context: android.content.Context) {
        if (!appSettingsOpened) return
        when (appSettingsTarget) {
            BrowserAppSettingsTarget.WEB_CAPTURE -> {
                pendingWebPermission?.let { request ->
                    grantSupportedWebResources(request, context, emptyMap())
                }
                pendingWebPermission = null
            }
            BrowserAppSettingsTarget.GEOLOCATION -> {
                pendingGeolocation?.let { request ->
                    val allowed = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    request.callback.invoke(request.origin, allowed, false)
                }
                pendingGeolocation = null
            }
            null -> Unit
        }
        appSettingsTarget = null
        appSettingsOpened = false
    }

    internal fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback
    }

    fun hideCustomView(): Boolean {
        if (customView == null) return false
        (customView?.parent as? ViewGroup)?.removeView(customView)
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        return true
    }

    internal fun cancelPendingRequests() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        denyWebPermission()
        denyGeolocation()
        appSettingsTarget = null
        appSettingsOpened = false
        hideCustomView()
    }
}

@Composable
fun rememberBrowserWebHostState(
    activeSession: TabSession?,
    shouldOpenAppSettings: suspend (permission: String, canShowRationale: Boolean) -> Boolean,
    onPermissionsRequested: suspend (Collection<String>) -> Unit
): BrowserWebHostState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { BrowserWebHostState() }
    val coroutineScope = rememberCoroutineScope()

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = state.filePathCallback
        state.filePathCallback = null
        callback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
    }

    val webPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val request = state.pendingWebPermission ?: return@rememberLauncherForActivityResult
        if (hasGrantedCaptureResource(request, context, results)) {
            grantSupportedWebResources(request, context, results)
            state.pendingWebPermission = null
        } else {
            coroutineScope.launch {
                val permissions = androidPermissionsFor(request)
                val shouldUseSettings = permissions.isNotEmpty() && permissions.all { permission ->
                    shouldOpenAppSettings(
                        permission,
                        canShowPermissionRationale(context, permission)
                    )
                }
                if (shouldUseSettings && state.pendingWebPermission === request) {
                    state.requestAppSettings(BrowserAppSettingsTarget.WEB_CAPTURE)
                } else {
                    runCatching { request.deny() }
                    if (state.pendingWebPermission === request) {
                        state.pendingWebPermission = null
                    }
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val request = state.pendingGeolocation ?: return@rememberLauncherForActivityResult
        val allowed = isPermissionGranted(
            context = context,
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            results = results
        ) || isPermissionGranted(
            context = context,
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            results = results
        )
        if (allowed) {
            request.callback.invoke(request.origin, true, false)
            state.pendingGeolocation = null
        } else {
            coroutineScope.launch {
                val permissions = listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                val shouldUseSettings = permissions.all { permission ->
                    shouldOpenAppSettings(
                        permission,
                        canShowPermissionRationale(context, permission)
                    )
                }
                if (shouldUseSettings && state.pendingGeolocation === request) {
                    state.requestAppSettings(BrowserAppSettingsTarget.GEOLOCATION)
                } else {
                    request.callback.invoke(request.origin, false, false)
                    if (state.pendingGeolocation === request) {
                        state.pendingGeolocation = null
                    }
                }
            }
        }
    }

    state.approveWebPermissionHandler = {
        val request = state.pendingWebPermission
        if (request != null) {
            coroutineScope.launch {
                val missingPermissions = androidPermissionsFor(request)
                    .filter { permission ->
                        ContextCompat.checkSelfPermission(context, permission) !=
                            PackageManager.PERMISSION_GRANTED
                    }
                if (missingPermissions.isEmpty()) {
                    grantSupportedWebResources(request, context, emptyMap())
                    state.pendingWebPermission = null
                    return@launch
                }
                val blockedPermissions = missingPermissions.filter { permission ->
                    shouldOpenAppSettings(
                        permission,
                        canShowPermissionRationale(context, permission)
                    )
                }
                val requestablePermissions = missingPermissions - blockedPermissions.toSet()
                if (requestablePermissions.isEmpty()) {
                    state.requestAppSettings(BrowserAppSettingsTarget.WEB_CAPTURE)
                } else if (state.pendingWebPermission === request) {
                    onPermissionsRequested(requestablePermissions)
                    webPermissionLauncher.launch(requestablePermissions.toTypedArray())
                }
            }
        }
    }
    state.approveGeolocationHandler = {
        val request = state.pendingGeolocation
        if (request != null) {
            val alreadyAllowed = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (alreadyAllowed) {
                request.callback.invoke(request.origin, true, false)
                state.pendingGeolocation = null
            } else {
                coroutineScope.launch {
                    val missingPermissions = listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ).filter { permission ->
                        ContextCompat.checkSelfPermission(context, permission) !=
                            PackageManager.PERMISSION_GRANTED
                    }
                    val blockedPermissions = missingPermissions.filter { permission ->
                        shouldOpenAppSettings(
                            permission,
                            canShowPermissionRationale(context, permission)
                        )
                    }
                    val requestablePermissions = missingPermissions - blockedPermissions.toSet()
                    if (requestablePermissions.isEmpty()) {
                        state.requestAppSettings(BrowserAppSettingsTarget.GEOLOCATION)
                    } else if (state.pendingGeolocation === request) {
                        onPermissionsRequested(requestablePermissions)
                        locationPermissionLauncher.launch(requestablePermissions.toTypedArray())
                    }
                }
            }
        }
    }

    DisposableEffect(activeSession, lifecycleOwner, context) {
        val session = activeSession
        if (session == null) {
            onDispose { }
        } else {
            val webView = session.webView
            val chromeClient = session.chromeClient
            val contextWrapper = webView.context as? MutableContextWrapper
            contextWrapper?.baseContext = context.asBrowserThemedContext()

            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                webView.onResume()
            } else {
                webView.onPause()
            }

            chromeClient.fileChooserHandler = { callback, params ->
                state.filePathCallback?.onReceiveValue(null)
                state.filePathCallback = callback
                try {
                    fileChooserLauncher.launch(params.createIntent())
                    true
                } catch (_: Exception) {
                    state.filePathCallback?.onReceiveValue(null)
                    state.filePathCallback = null
                    true
                }
            }
            chromeClient.permissionRequestHandler = { request ->
                val captureResources = request.resources.filter(::isCaptureResource)
                if (captureResources.isEmpty()) {
                    val protectedMedia = request.resources.filter {
                        it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                    }.toTypedArray()
                    if (protectedMedia.isEmpty()) request.deny() else request.grant(protectedMedia)
                } else {
                    state.denyWebPermission()
                    state.pendingWebPermission = request
                }
            }
            chromeClient.permissionRequestCanceledHandler = { request ->
                if (state.pendingWebPermission === request) {
                    state.pendingWebPermission = null
                }
            }
            chromeClient.geolocationPromptHandler = { origin, callback ->
                state.denyGeolocation()
                state.pendingGeolocation = PendingGeolocationRequest(origin, callback)
            }
            chromeClient.geolocationPromptHiddenHandler = state::denyGeolocation
            chromeClient.showCustomViewHandler = state::showCustomView
            chromeClient.hideCustomViewHandler = { state.hideCustomView() }

            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        state.resolveAfterAppSettings(context)
                        webView.onResume()
                    }
                    Lifecycle.Event.ON_PAUSE -> webView.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                chromeClient.clearUiHandlers()
                webView.onPause()
                contextWrapper?.baseContext =
                    context.applicationContext.asBrowserThemedContext()
                state.cancelPendingRequests()
            }
        }
    }

    return state
}

@Composable
fun BrowserWebsitePermissionPrompts(state: BrowserWebHostState) {
    val context = LocalContext.current
    if (state.appSettingsTarget != null && !state.appSettingsOpened) {
        PermissionRequireDialog(
            onDismissRequest = state::dismissAppSettingsPrompt,
            onConfirm = {
                state.appSettingsOpened = true
                openAppSettings(context)
            }
        )
        return
    }

    state.pendingWebPermission?.let { request ->
        val site = request.origin.host ?: request.origin.toString()
        val hasCamera = PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources
        val hasMicrophone = PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources
        val resourceName = stringResource(
            when {
                hasCamera && hasMicrophone -> R.string.browser_permission_camera_and_microphone
                hasCamera -> R.string.browser_permission_camera
                else -> R.string.browser_permission_microphone
            }
        )
        WebsitePermissionDialog(
            message = stringResource(
                R.string.browser_website_permission_message,
                site,
                resourceName
            ),
            onAllow = state::approveWebPermission,
            onDeny = state::denyWebPermission
        )
    }

    state.pendingGeolocation?.let { request ->
        val site = Uri.parse(request.origin).host ?: request.origin
        WebsitePermissionDialog(
            message = stringResource(R.string.browser_location_permission_message, site),
            onAllow = state::approveGeolocation,
            onDeny = state::denyGeolocation
        )
    }
}

@Composable
private fun WebsitePermissionDialog(
    message: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(text = stringResource(R.string.browser_website_permission_title)) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(text = stringResource(R.string.browser_permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(text = stringResource(R.string.browser_permission_block))
            }
        }
    )
}

@Composable
fun BrowserFullscreenContent(
    state: BrowserWebHostState,
    modifier: Modifier = Modifier
) {
    val customView = state.customView ?: return
    AndroidView(
        factory = {
            (customView.parent as? ViewGroup)?.removeView(customView)
            customView
        },
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_000000))
    )
}

private fun androidPermissionsFor(request: PermissionRequest): List<String> {
    return buildList {
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) {
            add(Manifest.permission.CAMERA)
        }
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) {
            add(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private fun grantSupportedWebResources(
    request: PermissionRequest,
    context: android.content.Context,
    results: Map<String, Boolean>
) {
    val grantedResources = request.resources.filter { resource ->
        when (resource) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> isPermissionGranted(
                context,
                Manifest.permission.CAMERA,
                results
            )
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> isPermissionGranted(
                context,
                Manifest.permission.RECORD_AUDIO,
                results
            )
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> true
            else -> false
        }
    }.toTypedArray()

    if (grantedResources.isEmpty()) {
        request.deny()
    } else {
        request.grant(grantedResources)
    }
}

private fun hasGrantedCaptureResource(
    request: PermissionRequest,
    context: android.content.Context,
    results: Map<String, Boolean>
): Boolean = request.resources.any { resource ->
    when (resource) {
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> isPermissionGranted(
            context,
            Manifest.permission.CAMERA,
            results
        )
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> isPermissionGranted(
            context,
            Manifest.permission.RECORD_AUDIO,
            results
        )
        else -> false
    }
}

private fun isPermissionGranted(
    context: android.content.Context,
    permission: String,
    results: Map<String, Boolean>
): Boolean {
    return results[permission] == true || ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun isCaptureResource(resource: String): Boolean {
    return resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
        resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE
}

private fun canShowPermissionRationale(context: Context, permission: String): Boolean {
    val activity = context.findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openAppSettings(context: android.content.Context) {
    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    )
}
