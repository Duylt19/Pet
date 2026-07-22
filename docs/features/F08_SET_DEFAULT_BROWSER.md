# F08 — Set Default Browser

Đặt app làm default browser của Android, qua RoleManager (API 29+) hoặc Settings intent fallback.

---

## 1. Manifest Declaration

`AndroidManifest.xml` MainActivity phải có intent-filter cho HTTP/HTTPS:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop">

    <!-- Launcher -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Browser default -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="http" />
        <data android:scheme="https" />
    </intent-filter>

    <!-- Custom scheme (deeplink) -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="privatebrowser" android:host="open" />
    </intent-filter>
</activity>
```

`android:autoVerify="true"` không bắt buộc cho default browser nhưng giúp deep link work với app links.

---

## 2. Helper Class

File: `utils/DefaultBrowserHelper.kt`

```kotlin
class DefaultBrowserHelper @Inject constructor(@ApplicationContext private val context: Context) {

    fun isDefaultBrowser(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    /** Returns intent to request default browser role. Caller must startActivity (or startActivityForResult on Activity). */
    fun createRequestIntent(): Intent? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // API 29+
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                } else null
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                // API 24-28: open default apps settings
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            }
            else -> {
                // API 21-23: fallback open app settings
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        }
    }
}
```

---

## 3. UI Trigger

### 3.1. Onboarding screen (S05)

```kotlin
@Composable
fun SetDefaultBrowserScreen(
    onCompleted: () -> Unit,
    viewModel: SetDefaultBrowserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onRoleResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateEvent.collect { onCompleted() }
    }

    Column(...) {
        // Header, benefits...
        PrimaryGradientButton(
            text = stringResource(R.string.setdefault_set_button_label),
            onClick = {
                viewModel.onSetDefaultClicked(launcher, context, activity)
            },
        )
        SecondaryTextButton(
            text = stringResource(R.string.setdefault_later_button_label),
            onClick = { viewModel.onLaterClicked() },
        )
    }
}
```

### 3.2. ViewModel

```kotlin
@HiltViewModel
class SetDefaultBrowserViewModel @Inject constructor(
    private val defaultBrowserHelper: DefaultBrowserHelper,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _navigateEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateEvent: SharedFlow<Unit> = _navigateEvent.asSharedFlow()

    fun onSetDefaultClicked(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
        activity: Activity?,
    ) {
        val intent = defaultBrowserHelper.createRequestIntent()
        if (intent == null) {
            // Already default OR API too low
            markPromptedAndNavigate()
            return
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            // OEM may block — fallback toast
            Toast.makeText(context, R.string.setdefault_fallback_toast, Toast.LENGTH_LONG).show()
            markPromptedAndNavigate()
        }
    }

    fun onRoleResult(granted: Boolean) {
        // Granted or not, mark prompted and continue
        markPromptedAndNavigate()
    }

    fun onLaterClicked() {
        markPromptedAndNavigate()
    }

    private fun markPromptedAndNavigate() = viewModelScope.launch {
        preferencesRepository.setDefaultBrowserPrompted(true)
        _navigateEvent.tryEmit(Unit)
    }
}
```

### 3.3. Settings row

```kotlin
SettingsRow(
    iconRes = R.drawable.ic_set_default_mask,
    title = stringResource(R.string.settings_set_default_title),
    trailing = if (isDefault) SettingsTrailing.Text(stringResource(R.string.settings_already_default))
               else SettingsTrailing.Chevron,
    onClick = {
        if (!isDefault) {
            val intent = defaultBrowserHelper.createRequestIntent() ?: return@SettingsRow
            settingsLauncher.launch(intent)
        }
    }
)
```

---

## 4. UI States

### Trên onboarding screen:

| State | Display |
|-------|---------|
| Default chưa được set | Header + 3 benefits + 2 buttons |
| Đã là default | Auto-skip — `MainViewModel.getNextScreen` không return SET_DEFAULT_BROWSER nếu `isDefaultBrowser()` true |

### Trong Settings:

| isDefault | Row hiển thị |
|-----------|--------------|
| true | "Set As Default Browser" + trailing text "Already default" (color secondary) |
| false | "Set As Default Browser" + chevron, clickable |

---

## 5. Deep Link Handling

Khi user mở URL từ app khác (vd tap link trong Messages):

```kotlin
// MainActivity.onCreate / onNewIntent
private fun handleIntent(intent: Intent?) {
    val uri = intent?.data ?: return
    if (uri.scheme in setOf("http", "https")) {
        val url = uri.toString()
        // Wait for NavController ready
        pendingDeepLink = url
    }
}

// Apply pending deep link after NavHost set up:
LaunchedEffect(pendingDeepLink) {
    pendingDeepLink?.let { url ->
        navController.navigate(buildBrowserWebViewRoute(url))
        pendingDeepLink = null
    }
}
```

---

## 6. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| API 21-23 (no ROLE_BROWSER) | Mở Settings app, user phải set manual |
| OEM khoá role (Samsung One UI < 4) | Catch ActivityNotFoundException, show toast hướng dẫn |
| User huỷ dialog OS | `onRoleResult(false)` — vẫn nav Home, set prompted=true |
| Đã là default → tap "Set as default" | Helper detect → skip dialog, navigate luôn |
| User set default ngoài app (Settings) rồi quay lại | `isDefaultBrowser()` true → Settings row update |
| Multi-user device | Role per user-profile, app vẫn handle bình thường |

---

## 7. Test Cases

1. **Fresh install:** Onboarding → SetDefault → tap "Set as default" → OS dialog → confirm → app là default
2. **Skip:** Onboarding → SetDefault → tap "Later" → vào Home không bị default
3. **Settings entry:** Settings → "Set As Default Browser" → OS dialog
4. **Đã default:** Onboarding skip SetDefault screen; Settings row hiện "Already default"
5. **Deep link:** Mở link từ Messages → app tự mở BrowserWebView

---

## 8. Manifest Notes

- `android:exported="true"` BẮT BUỘC khi target API 31+ cho activity có intent-filter
- `android:launchMode="singleTop"` để intent mới gọi `onNewIntent` thay vì tạo activity mới

---

## 9. Liên Quan

- [S05_SET_DEFAULT_BROWSER.md](../screens/S05_SET_DEFAULT_BROWSER.md)
- [S09_SETTINGS.md](../screens/S09_SETTINGS.md)
- [04_NAVIGATION_FLOW.md](../04_NAVIGATION_FLOW.md) — deep link
