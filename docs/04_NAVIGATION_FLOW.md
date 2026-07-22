# 04 — Navigation Flow

## 1. Sơ Đồ Navigation Tổng

```
┌─────────────┐
│   Splash    │  init ads consent + RC + billing
└──────┬──────┘
       │
       │ getNextScreen()
       ▼
   ┌───────────────────────────────────────────────┐
   │ Onboarding (chỉ session 1, hoặc bước thiếu)   │
   │                                               │
   │   Language → Intro → SetDefault → Permission  │
   └───────────────────────┬───────────────────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │      Home       │  ← startDestination từ session 2+
                  │ (4 bottom tabs) │
                  └────┬───┬───┬───┬┘
                       │   │   │   │
       ┌───────────────┘   │   │   └───────────────────────┐
       │                   │   │                           │
       ▼                   ▼   ▼                           ▼
   ┌────────┐         ┌────────┐ ┌───────┐         ┌───────────┐
   │ Home   │         │  Tabs  │ │ Files │         │ Progress  │
   │  tab   │         │  tab   │ │  tab  │         │   tab     │
   └───┬────┘         └───┬────┘ └───┬───┘         └─────┬─────┘
       │                  │          │                   │
       │ tap search       │ tap tab/+│ tap category      │
       │ tap shortcut     │ More >   │                   │
       │                  │ Select   │                   │
       ▼                  ▼          ▼                   ▼
  BrowserWebView    BrowserWebView   (internal)     HowToDownload modal
                         │
                         └── TabSelection (full-screen, no ad)
       │
       │ tap hamburger (any Home tab) → drawer:
       ├─── Bookmarks/History
       ├─── Settings
       │        ├── Set As Default Browser → OS dialog
       │        ├── Search Engine → BottomSheet picker
       │        ├── Clear History → confirm dialog
       │        ├── Language → LanguageSettings
       │        ├── Send Feedback → mailto Intent
       │        ├── Share App → Intent.ACTION_SEND
       │        └── Privacy Policy → BrowserWebView (URL config)
       │
       └─── Premium (entry điểm có thể từ Splash hoặc Settings)
```

---

## 2. Routes Constants

Định nghĩa bên trong `NavGraph.kt` (`app/src/main/java/com/asianmobile/privatebrower/navigation/NavGraph.kt`), không phải file riêng.

```kotlin
object Routes {
    // Onboarding
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val INTRO = "intro"
    const val PERMISSION = "permission"
    const val SET_DEFAULT_BROWSER = "set_default_browser"   // NEW

    // Main
    const val HOME = "home"

    // Browser
    const val BROWSER_WEBVIEW = "browser_webview"           // NEW (with args)
    const val TAB_SELECTION = "tab_selection"
    const val BOOKMARKS_HISTORY = "bookmarks_history"       // NEW

    // Settings & misc
    const val SETTINGS = "settings"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val PRIVACY_POLICY = "privacy_policy"             // NEW
    const val PREMIUM = "premium"
}
```

**Route với arguments:**

### PREMIUM — `{startByIndex}`

| Argument | Type | Mô tả |
|----------|------|--------|
| `startByIndex` | `StringType` | Enum `StartPremiumIndexes`: `ONBOARDING_FIRST`, `SPLASH_RETURN`, `IN_APP` |

### BROWSER_WEBVIEW — `?url={url}&incognito={incognito}`

| Argument | Type | Default | Mô tả |
|----------|------|---------|--------|
| `url` | `String?` (nullable) | `null` | URL cần load |
| `incognito` | `Boolean` | `false` | Chế độ ẩn danh |

```kotlin
// Define
const val BROWSER_WEBVIEW_BASE = "browser_webview"
const val BROWSER_WEBVIEW_ARG_URL = "url"
const val BROWSER_WEBVIEW_ARG_INCOGNITO = "incognito"
const val BROWSER_WEBVIEW_ROUTE =
    "$BROWSER_WEBVIEW_BASE?$BROWSER_WEBVIEW_ARG_URL={url}&$BROWSER_WEBVIEW_ARG_INCOGNITO={incognito}"

// Helper
fun buildBrowserWebViewRoute(url: String, incognito: Boolean = false): String {
    val encoded = Uri.encode(url)
    return "$BROWSER_WEBVIEW_BASE?$BROWSER_WEBVIEW_ARG_URL=$encoded&$BROWSER_WEBVIEW_ARG_INCOGNITO=$incognito"
}

// Composable destination
composable(
    route = BROWSER_WEBVIEW_ROUTE,
    arguments = listOf(
        navArgument(BROWSER_WEBVIEW_ARG_URL) { type = NavType.StringType; nullable = true; defaultValue = null },
        navArgument(BROWSER_WEBVIEW_ARG_INCOGNITO) { type = NavType.BoolType; defaultValue = false },
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = "http://{any}" },
        navDeepLink { uriPattern = "https://{any}" },
    )
) { backStackEntry ->
    val url = backStackEntry.arguments?.getString(BROWSER_WEBVIEW_ARG_URL)
    val incognito = backStackEntry.arguments?.getBoolean(BROWSER_WEBVIEW_ARG_INCOGNITO) ?: false
    BrowserWebViewScreen(initialUrl = url, isIncognito = incognito, onBack = { ... })
}
```

### TAB_SELECTION - `?incognito={incognito}`

| Argument | Type | Default | Mo ta |
|----------|------|---------|-------|
| `incognito` | `Boolean` | `false` | Chon tap Normal hay Private cua mode dang xem |

Day la route full-screen rieng. Bam Back quay lai dung trang Tabs trong `HomeScreen`; navigation noi bo nay khong hien interstitial.

### PRIVACY_POLICY — `?url={url}`

| Argument | Type | Default | Mô tả |
|----------|------|---------|--------|
| `url` | `String` | `""` | URL trang privacy policy |

---

## 3. Start Destination Logic

File: `app/src/main/java/com/asianmobile/privatebrower/ui/main/MainViewModel.kt`

Splash gọi `nextScreenAfterSplash` để quyết định màn hình tiếp theo. Các đích có thể: `LANGUAGE`, `INTRO`, `PERMISSION`, `SET_DEFAULT_BROWSER`, `HOME`, hoặc `PREMIUM`.

```kotlin
fun getNextScreen(): String = when {
    !isLanguageCompleted -> Routes.LANGUAGE
    !isIntroCompleted -> Routes.INTRO
    !isDefaultBrowserPrompted && !isAlreadyDefaultBrowser -> Routes.SET_DEFAULT_BROWSER
    !isPermissionCompleted -> Routes.PERMISSION
    else -> Routes.HOME
}
```

**Flag `isDefaultBrowserPrompted`** — lưu trong DataStore, set `true` khi user bấm "Set as default" hoặc "Later". Không quan tâm user có thực sự đặt default hay không (vì có thể setting bị OEM khoá).

**Navigation flow chi tiết:**

```
SPLASH → nextScreenAfterSplash (LANGUAGE, INTRO, PERMISSION, SET_DEFAULT_BROWSER, HOME, hoặc PREMIUM)
LANGUAGE → recreate Activity → INTRO
INTRO → PREMIUM (nếu onboarding config) hoặc SET_DEFAULT_BROWSER
SET_DEFAULT_BROWSER → PERMISSION
PERMISSION → HOME
HOME → SETTINGS, BROWSER_WEBVIEW, TAB_SELECTION, BOOKMARKS_HISTORY, PREMIUM, LANGUAGE_SETTINGS, PRIVACY_POLICY
BOOKMARKS_HISTORY → BROWSER_WEBVIEW (mở URL)
```

---

## 4. Bottom Sheets / Dialogs / Modals (Không Phải Route)

Render bên trong screen chứa, **không** push vào back stack:

| Component | Trigger | File |
|-----------|---------|------|
| Search Engine Picker (ModalBottomSheet) | Settings > Search engine row | `ui/searchengine/SearchEnginePickerSheet.kt` |
| Clear History Confirm (AlertDialog) | Settings > Clear History | inline trong `SettingsScreen` |
| Tab Close Confirm (AlertDialog) | Tabs tab > trash (close all) | inline trong `TabsTabScreen` |
| Exit App Confirm | back press ở Home | inline trong `MainActivity` |
| Hamburger Drawer (ModalNavigationDrawer) | tap hamburger ở Home top bar | inline trong `HomeScreen` |

**HowToDownload** là exception: tuy là modal full-screen với X close, nó vẫn là 1 route riêng (ngắn gọn, có thể `popBackStack` từ X) → cân nhắc làm `ModalBottomSheet` với `skipPartiallyExpanded = true` hoặc `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`. **Mặc định: dùng `Dialog` fullscreen, không thêm route.**

---

## 5. Back Stack Rules

| Từ | Tới | Pop behavior |
|----|-----|--------------|
| Splash | (next screen) | `popUpTo(SPLASH) { inclusive = true }` |
| Language | Intro | `popUpTo(LANGUAGE) { inclusive = true }` |
| Intro | SetDefaultBrowser | `popUpTo(INTRO) { inclusive = true }` |
| SetDefaultBrowser | Permission | `popUpTo(SET_DEFAULT_BROWSER) { inclusive = true }` |
| Permission | Home | `popUpTo(PERMISSION) { inclusive = true }` |
| Home | BrowserWebView | normal push |
| Home/Tabs | TabSelection | normal push, khong interstitial |
| TabSelection | back | popBackStack ve Home/Tabs |
| BrowserWebView | back | popBackStack — **không pop Home** |
| Home → Settings | back | popBackStack |
| Any screen | Premium | normal push, popBackStack về screen trước |

**Trong Home (back press):**
- Nếu drawer mở → đóng drawer
- Nếu đang ở tab không phải Home tab (bottom nav) → switch về Home tab
- Nếu ở Home tab → show ExitDialog

---

## 6. Deep Link Handling

App đăng ký intent-filter trong `AndroidManifest.xml` cho MainActivity:

```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Default browser -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="http" />
        <data android:scheme="https" />
    </intent-filter>
</activity>
```

`MainActivity` xử lý intent qua biến `pendingDeepLinkUrl`. Khi app ở màn HOME, URL sẽ được navigate đến `BROWSER_WEBVIEW`:
```kotlin
intent?.data?.let { uri ->
    if (uri.scheme in setOf("http", "https")) {
        // Navigate đến BROWSER_WEBVIEW khi đã ở HOME
        pendingDeepLinkUrl = uri.toString()
    }
}
```

---

## 7. Interstitial Insertion Points

Theo matrix trong [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md). Cụ thể vị trí gọi `navigateWithAd`:

| Action | Show ad? |
|--------|----------|
| SetDefaultBrowser → Permission → Home (sau onboarding) | ✅ |
| Home Browser tab → BrowserWebView (tap search/shortcut) | ✅ |
| Home drawer → Bookmarks | ✅ |
| Home drawer → Settings | ✅ |
| Settings → LanguageSettings | ✅ |
| Settings → Privacy Policy | ✅ |
| BrowserWebView → back về Home | ❌ (chỉ pop, không show ad — nếu show sẽ phiền) |
| Tab tab → switch tab | ❌ |
| Search engine picker tap row | ❌ (chỉ bottom sheet) |

**Mỗi lần show interstitial:**
```kotlin
InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
// MainActivity.onStart() sẽ tự re-enable lại
```

---

## 8. Animation Transition (Optional)

Compose Navigation 2.7+ hỗ trợ `enterTransition` / `exitTransition`. V1 dùng default fade. Có thể custom cho:

- Bottom sheet picker: `slideInVertically + slideOutVertically`
- Modal HowToDownload: `fadeIn + fadeOut`
- Drawer: `slideInHorizontally + slideOutHorizontally`

V1 ưu tiên default, polish v2.

---

## 9. Helpers (đã có / cần extend)

File: `navigation/NavExtensions.kt`

```kotlin
// Show interstitial trước khi navigate, debounce 500ms
fun navigateWithAd(context: Context, onNavigate: () -> Unit)

// Wrap navigate trong runCatching, có debounce
fun NavController.safeNavigate(route: String, ignoreDebounce: Boolean = false, builder: NavOptionsBuilder.() -> Unit = {})

// Safe pop back stack
fun NavController.safePopBackStack(ignoreDebounce: Boolean = false): Boolean
```

Thêm helper mới (cho route có args):
```kotlin
fun NavController.navigateToBrowserWebView(url: String, incognito: Boolean = false) {
    safeNavigate(buildBrowserWebViewRoute(url, incognito))
}
```

---

## 10. Edge Cases

1. **Activity recreate (Language change)**: Lưu current route vào `SavedStateHandle`, restore sau recreate
2. **Process death**: NavHost tự restore back stack từ saved state (nếu các argument là parcelable)
3. **App restart sau crash**: Splash chạy lại, getNextScreen() đọc DataStore → đúng startDestination
4. **Deep link khi app đã chạy**: `MainActivity.onNewIntent` handle URI → push BrowserWebView lên top
5. **Back button quá nhanh (double tap)**: `safePopBackStack` có debounce 500ms — tránh pop nhầm 2 screen
