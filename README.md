# Private Browser: Safe & Secure — Android App

Ứng dụng trình duyệt web riêng tư (**Private Browser**) được xây dựng trên nền tảng **Clean Architecture + MVVM**, sử dụng **Jetpack Compose** (Material 3) và 100% **Kotlin**.

> **Trạng thái:** ✅ Hoàn thành 12/12 milestones (M1-M12). Xem [IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md).

---

## 1. Cấu Trúc Thư Mục (Package Structure)

Mã nguồn chính nằm trong `app/src/main/java/com/asianmobile/privatebrower/`:

```
com.asianmobile.privatebrower
├── BaseApplication.kt              # Application class (Hilt entry point)
├── MainActivity.kt                 # Single Activity (singleTop, deep link handler)
├── components/                     # AppComponents.kt
├── constant/                       # Constant.kt
├── data/
│   ├── browser/                    # BrowserEngine, TabManager, VideoSniffer, DetectedVideo
│   ├── database/                   # Room DB v2 (4 entities, 4 DAOs, autoMigration)
│   ├── local/                      # DataStoreManager (21 keys)
│   ├── model/                      # Bookmark, DownloadItem, DownloadStatus, HistoryItem, QuickAccessShortcut, SearchEngine, Tab
│   ├── repository/                 # 6 repositories (Preferences, Tab, SearchEngine, Bookmark, History, Download)
│   ├── usecase/                    # ClearBrowsingDataUseCase
│   └── util/                       # Data utilities
├── di/                             # 6 Hilt modules (App, Browser, Data, Gson, Network, Repository)
├── navigation/
│   ├── NavGraph.kt                 # Routes object (13 routes) + AppNavGraph
│   └── NavExtensions.kt           # navigateWithAd, safeNavigate, safePopBackStack
├── service/
│   └── DownloadForegroundService.kt # Download service (OkHttp streaming, resume, concurrent, CDN auth)
├── ui/
│   ├── splash/                     # Splash screen
│   ├── language/                   # Language selection (11 languages)
│   ├── intro/                      # Onboarding screens
│   ├── permission/                 # Permission request
│   ├── setdefault/                 # Set Default Browser (RoleManager API 29+)
│   ├── home/                       # Home container (4 tabs via HorizontalPager)
│   │   ├── HomeScreen.kt          # Scaffold + Drawer + Pager
│   │   ├── HomeViewModel.kt
│   │   ├── browsertab/            # Browser tab (search + bookmarks + quick access)
│   │   ├── tabstab/               # Tabs tab (Normal/Incognito)
│   │   ├── filestab/              # Files tab (file manager + storage card)
│   │   ├── progresstab/           # Downloads tab (filter, thumbnails, progress, open file)
│   │   ├── settings/              # Settings screen
│   │   └── component/             # HomeBottomNavBar, etc.
│   ├── browser/                    # BrowserWebViewScreen, VideoSelectBottomSheet, VideoDownloadFab
│   ├── bookmarks/                  # Bookmarks & History screen
│   ├── searchengine/               # Search engine picker bottom sheet
│   ├── privacypolicy/              # Privacy policy WebView
│   ├── premium/                    # Premium/subscription (Go Ad-Free)
│   ├── component/                  # 12 reusable composables (AppHeaderBar, SearchBar, etc.)
│   ├── customview/                 # Custom views
│   ├── main/                       # MainViewModel (onboarding state)
│   └── theme/                      # Material 3 Theme
└── utils/                          # Analytics, browser, file and vault helpers
```

**Module quảng cáo** (`ads/`):
```
com.asianmobile.privatebrower.ads
├── config/                         # Screen codes, ad unit IDs
├── data/                           # SharedPreferencesUtils
├── ui/
│   ├── compose/                    # BannerAd, NativeAdInternal (Composables)
│   ├── interstitial/               # InterstitialUtil, InterstitialLauncherUtil
│   ├── openads/                    # App Open Ads
│   └── rewarded/                   # Rewarded Ads
└── utils/                          # SafeRemoteConfig, helpers
```

---

## 2. Công Nghệ (Tech Stack)

| Thành phần | Công nghệ | Version |
|---|---|---|
| **Ngôn ngữ** | Kotlin 100% | 2.2.0 |
| **UI Framework** | Jetpack Compose (Material 3) | BoM 2024.12.01 |
| **Kiến trúc** | Single-Activity, Clean Architecture + MVVM | — |
| **DI** | Dagger Hilt | 2.57.1 |
| **Navigation** | Navigation Compose | 2.8.5 |
| **Database** | Room | 2.7.1 |
| **Preferences** | DataStore | 1.0.0 |
| **Image Loading** | Coil + Glide | 2.7.0 / 5.0.5 |
| **Animations** | Lottie Compose | 6.6.2 |
| **WebView** | AndroidX WebKit | 1.16.0 |
| **In-App Purchase** | Google BillingClient | 8.0.0 |
| **Firebase** | Analytics + Crashlytics + RemoteConfig | BoM 34.3.0 |
| **Ads** | GMA, AppLovin, etc. | Mediation |
| **Sizing** | sdp / ssp (Intuit) | Scalable dimensions |
| **Dependency Mgmt** | Version Catalog | `gradle/libs.versions.toml` |

---

## 3. Luồng Hoạt Động (App Flow)

```
Splash → Language → Intro → [Premium?] → SetDefaultBrowser → Permission → Home
```

1. **Splash** — Init ads consent, billing check, remote config. Session 2+ có thể show Premium Splash Return.
2. **Language** — Chọn 1/11 ngôn ngữ → `recreate()` Activity.
3. **Intro** — Onboarding giới thiệu app. Có thể show Premium Onboarding First.
4. **Permission** — Yêu cầu quyền storage.
5. **SetDefaultBrowser** — Hỏi set app làm default browser (RoleManager API 29+).
6. **Home** — 4 tabs: Browser / Tabs / Files / Downloads. Drawer navigation.
7. **Premium** — Go Ad-Free via Google Billing.

**Deep link:** `http/https` URLs → `pendingDeepLinkUrl` → BROWSER_WEBVIEW.

---

## 3.1. Download & Video Detection Flow

```
User duyệt web → WebView loads resources
  → VideoSniffer intercepts → detect video URLs (filter images/HLS)
  → FAB tím xuất hiện (gradient, pulse animation, badge count)
  → User click FAB → VideoSelectBottomSheet (grid thumbnails)
  → Select videos → Download
  → DownloadRepository.enqueue() + headers/cookies
  → DownloadForegroundService (OkHttp streaming)
  → Single notification (progress) → Completed notification
  → Downloads tab: thumbnails + progress + click to open
```

> 📖 Chi tiết: [docs/features/F05_DOWNLOAD_MANAGER.md](docs/features/F05_DOWNLOAD_MANAGER.md)

---

## 4. Routes (NavGraph.kt)

```kotlin
object Routes {
    const val SPLASH = "splash"
    const val INTRO = "intro"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val PREMIUM = "premium"                       // arg: {startByIndex}
    const val SETTINGS = "settings"
    const val LICENSE_AGREEMENT_SETTINGS = "license_agreement_settings"
    const val SET_DEFAULT_BROWSER = "set_default_browser"
    const val BROWSER_WEBVIEW = "browser_webview"       // args: ?url={url}&incognito={incognito}
    const val BOOKMARKS_HISTORY = "bookmarks_history"
    const val PRIVACY_POLICY = "privacy_policy"         // arg: ?url={url}
}
```

---

## 5. Cách Thêm Tính Năng Mới

### Bước 1: Tạo feature package (3 files)
```
ui/newfeature/
├── NewFeatureScreen.kt        # UI Composable
├── NewFeatureViewModel.kt     # @HiltViewModel + business logic
└── NewFeatureUiState.kt       # data class cho UI state
```

### Bước 2: Thêm Route vào `Routes` object trong NavGraph.kt

### Bước 3: Thêm composable vào AppNavGraph

### Bước 4: Thêm Resources (strings.xml, colors.xml)
```xml
<string name="new_feature_title">New Feature</string>
<color name="colors_FF5722">#FF5722</color>
```

### Bước 5: Navigate
```kotlin
navigateWithAd(context) {
    navController.safeNavigate(Routes.NEW_FEATURE, ignoreDebounce = true)
}
```

---

## 6. Quy Tắc Code Quan Trọng

| # | Quy tắc | Ví dụ đúng |
|---|---------|-----------|
| 1 | ❌ Không hardcode string | `stringResource(R.string.xxx)` |
| 2 | ❌ Không hardcode color hex | `colorResource(R.color.xxx)` |
| 3 | ✅ Đặt tên color: `colors_[hex]` | `colors_7C5BFB`, `colors_1F1F1F` |
| 4 | ✅ Dùng sdp/ssp (Figma ÷ 1.3) | `dimensionResource(SdpR.dimen._12sdp)` |
| 5 | ✅ Mỗi màn hình = 3 files | Screen + ViewModel + UiState |
| 6 | ✅ ViewModel bắt buộc Hilt | `@HiltViewModel` + `@Inject` |
| 7 | ✅ Safe Navigation | `safeNavigate()`, `safePopBackStack()` |
| 8 | ✅ Modifier order | `.size().shadow().clip().background().border().clickable().padding()` |
| 9 | ✅ Giao tiếp Tiếng Việt | Code bằng Tiếng Anh |

> 📖 Chi tiết đầy đủ: [SKILL.md](.agents/skills/android_developer/SKILL.md) | [docs/08_AGENT_CODING_GUIDELINES.md](docs/08_AGENT_CODING_GUIDELINES.md)

---

## 7. Git Commit Convention

```
Handle feature [Feature Name]          # Hoàn thành tính năng mới
Handle UI [Screen/Component Name]      # Hoàn thiện giao diện
Fix bug crash feature [Feature Name]   # Sửa lỗi crash
Fix bug UI [Screen/Component Name]     # Sửa lỗi UI
Update / Refactor / Remove             # Các trường hợp khác
```

---

## 8. Build & Run

```bash
# Compile check nhanh (~15-20s) — ƯU TIÊN DÙNG
./gradlew compileDebugKotlin

# Compile check ProGuard (~30-60s)
./gradlew compileReleaseKotlin

# Build APK debug (~4-5 min)
./gradlew assembleDebug

# Build APK release (~5-7 min)
./gradlew assembleRelease
```

> ❌ **KHÔNG** dùng `assembleDebug/Release` để check syntax — quá chậm!

---

## 9. Tài Liệu Tham Khảo

- **[docs/](docs/README.md)** — Bộ tài liệu đầy đủ (10 foundation + 11 features + 17 screens)
- **[docs/features/F05_DOWNLOAD_MANAGER.md](docs/features/F05_DOWNLOAD_MANAGER.md)** — Download & Video Detection (chi tiết kiến trúc, code, flow)
- **[SKILL.md](.agents/skills/android_developer/SKILL.md)** — Quy tắc code và hướng dẫn cho AI agent (Section 10: Download)
- **[IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md)** — Tracking tiến độ M1-M12
- **[CLAUDE.md](CLAUDE.md)** — Agent context & project guidelines
# Pet
