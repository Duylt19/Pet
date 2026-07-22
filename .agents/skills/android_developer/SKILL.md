---
name: android-developer
description: Developer guidelines, architecture rules, and UI patterns for the Private Browser Android app.
---

# Android Developer Agent Skill: Private Browser App

As an AI Android Developer working on this project, you MUST strictly adhere to the following coding conventions, architectural structure, and UI patterns.

## 0. Giao Tiếp (Communication)
- **TẤT CẢ** các giao tiếp, giải thích, và bình luận với người dùng trong suốt project này **PHẢI** được viết bằng **Tiếng Việt**.
- Code, tên biến, commit message vẫn sử dụng tiếng Anh theo chuẩn quốc tế, nhưng khi chat/giải thích với người dùng thì dùng tiếng Việt.
- **Quy tắc khi dịch (Translation):** Hãy dịch sát nghĩa của câu, làm cho câu linh hoạt và tự nhiên. Tuyệt đối KHÔNG dịch từng từ (word-by-word) giống như máy móc, khiến văn bản trở nên cứng nhắc, khó hiểu, vô nghĩa và không phù hợp với ngữ cảnh thực tế của ứng dụng.

## 0.1. Build & Verification Rules
- **KHÔNG** chạy `assembleRelease` hoặc `assembleDebug` để kiểm tra lỗi compile — các lệnh này mất 4-5 phút vì phải chạy R8, shrink resources, và đóng gói APK.
- **Ưu tiên dùng** `./gradlew compileDebugKotlin` (~15-20 giây) để verify lỗi compile nhanh nhất trên Codex/Linux.
- Có thể dùng `./gradlew compileReleaseKotlin` (~30-60 giây) nếu cần kiểm tra cả ProGuard rules.
- Nếu chạy trên Windows native thì dùng lệnh tương đương `.\gradlew.bat compileDebugKotlin`, nhưng trong workspace Codex hiện tại dùng `./gradlew`.
- Chỉ chạy `assembleRelease` hoặc `assembleDebug` khi cần build APK thực tế để test trên thiết bị hoặc khi người dùng yêu cầu.

## 0.2. Nguyên Tắc Làm Việc (Work Principles)
- **TÍNH KHÁCH QUAN:** TUYỆT ĐỐI không tự ý quyết định hoặc đoán mò ý định của người dùng khi gặp vấn đề chưa rõ ràng về UI, Logic hoặc Architecture.
- **HỎI KHI CHƯA RÕ:** Nếu có bất kỳ điểm nào trong yêu cầu hoặc trong Figma mà bạn cảm thấy mơ hồ, **BẮT BUỘC** phải dừng lại và đặt câu hỏi để làm rõ với người dùng trước khi thực hiện.
- **THỰC THI CHÍNH XÁC:** Chỉ thực hiện thay đổi khi đã nắm rõ yêu cầu và sự đồng ý của người dùng.

## 0.3. Codex Tooling Notes
- Project này đã chuyển sang Codex. Khi đọc file hoặc tìm code, dùng `rg`, `sed`, `nl`, `ls`, `wc` qua shell thay cho các tool cũ như `view_file` hoặc `grep_search`.
- Khi sửa file thủ công, dùng `apply_patch`. Không dùng lệnh shell ghi file kiểu `cat > file`.
- Khi làm UI từ Figma, ưu tiên MCP tools hiện tại: `mcp__figma.get_design_context`, `mcp__figma.get_screenshot`, `mcp__figma.get_metadata`, `mcp__figma.get_variable_defs`.
- Nếu MCP không trả được SVG frame hoàn chỉnh cho icon/logo, dùng Figma REST API với token/file key trong `.agents/AGENTS.md`; không in token ra chat hoặc log.

## 0.4. Room Database Policy (BẮT BUỘC)
- Trong giai đoạn development hiện tại, giữ `PrivateBrowserDatabase` ở `version = 1`.
- Khi schema thay đổi, dùng destructive fallback; **KHÔNG** tăng version và **KHÔNG** thêm migration/auto-migration.
- Chỉ sau khi owner xác nhận version 2 đã là baseline ổn định mới bắt đầu duy trì migration cho các version tiếp theo.

## 0.5. All Files Access / Vault Policy (BẮT BUỘC)
- `MANAGE_EXTERNAL_STORAGE` là nguồn quyền storage duy nhất trên API 30+ cho Photos, Videos, Audio, Files và roadmap Vault mã hóa/khóa file, thư mục; không tự ý loại bỏ với lý do dư thừa.
- Trên API 30+, đây là special app access: mở `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`, fallback sang `Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`, rồi kiểm tra bằng `Environment.isExternalStorageManager()`; không đưa vào runtime permission launcher.
- Không khai báo hoặc request `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED` trên API 30+.
- Trên Android 10 (API 29), bật `android:requestLegacyExternalStorage="true"` và khai báo/request cả `READ_EXTERNAL_STORAGE` lẫn `WRITE_EXTERNAL_STORAGE` với `maxSdkVersion=29`. Chỉ coi broad legacy storage đã được cấp khi cả hai quyền đều granted và `Environment.isExternalStorageLegacy()` trả về `true`; không mở màn All files access vì API 29 chưa có `MANAGE_EXTERNAL_STORAGE`.
- Trên API 28 trở xuống, tiếp tục dùng cặp quyền storage runtime cũ.
- Khi quyền được cấp, Photos/Videos/Audio query MediaStore và Files query `MediaStore.Files`. Khi bị từ chối, các màn Media Library/Files/Vault hiển thị trạng thái yêu cầu All Files Access; Browser, Download và Storage Access Framework vẫn phải tiếp tục hoạt động.
- Delete ở tab Files trên API 30+ đã cấp quyền phải đi qua batch delete trực tiếp trong ViewModel; không đưa Files vào state machine `MediaStore.createDeleteRequest()`. Chỉ giữ consent/retry cho media và Android cũ cần tương thích.
- API 29+ vẫn publish download qua `MediaStore.Downloads`; không thay toàn bộ storage flow hiện tại bằng direct file access chỉ vì đã có All files access.
- API 33+ xin `POST_NOTIFICATIONS` đúng ngữ cảnh khi user bắt đầu hoặc resume download đầu tiên, chỉ một lần. User từ chối thì download/foreground service vẫn tiếp tục và app không tự động hỏi lặp lại.
- Trước khi phát hành Google Play, file management/Vault phải là core functionality thực sự và app phải hoàn tất Permissions Declaration cho All files access.

## 1. Technology Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Dependency Injection:** Dagger Hilt
- **Navigation:** Navigation Compose
- **Asynchronous Programming:** Kotlin Coroutines & Flow
- **Local Storage:** DataStore Preferences
- **Image Loading:** Coil
- **Animations:** Lottie Compose
- **In-App Purchase:** Google BillingClient

## 2. Project Architecture & Packaging
Dự án Private Browser được xây dựng dựa trên kiến trúc base project **FileRecovery** (Single-Activity, Clean Architecture + MVVM). Cần tuân thủ nghiêm ngặt cấu trúc và quy tắc code đã được thiết lập sẵn.

### 2.1. Multi-Module Structure
- **`:app`** - Chứa toàn bộ features và UI chính.
- **`:ads`** - Module riêng biệt chứa các tích hợp mạng quảng cáo (Admob, Pangle, Mintegral, etc.).

### 2.2. Package Structure
```
com.asianmobile.privatebrower
├── BaseApplication.kt          # Application class (Hilt entry point)
├── MainActivity.kt             # Single Activity (entry point, singleTop)
├── components/                 # AppComponents.kt
├── constant/                   # Constant.kt
├── data/
│   ├── browser/               # BrowserEngine, TabManager, VideoSniffer, DetectedVideo
│   ├── database/              # Room DB development v1, entities and DAOs
│   ├── local/                 # DataStoreManager
│   ├── model/                 # Bookmark, DownloadItem, DownloadStatus, HistoryItem, QuickAccessShortcut, SearchEngine, Tab
│   ├── repository/            # 6 repositories: Preferences, Tab, SearchEngine, Bookmark, History, Download
│   ├── usecase/               # ClearBrowsingDataUseCase
│   └── util/                  # Data utilities
├── di/                         # 6 Hilt modules: App, Browser, Data, Gson, Network, Repository
├── navigation/
│   ├── NavGraph.kt             # Navigation graph (Routes object + AppNavGraph)
│   └── NavExtensions.kt        # navigateWithAd, safeNavigate, safePopBackStack
├── service/
│   └── DownloadForegroundService.kt  # Download service (OkHttp streaming, resume, concurrent, headers/cookies)
├── ui/
│   ├── splash/                 # Splash screen
│   ├── intro/                  # Intro/Onboarding screens
│   ├── language/               # Language selection
│   ├── permission/             # Permission request
│   ├── setdefault/             # Set Default Browser (RoleManager)
│   ├── home/                   # Home screen container (4 tabs)
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── browsertab/         # Browser tab (search + bookmarks + quick access)
│   │   ├── tabstab/            # Tabs tab (Normal/Incognito)
│   │   ├── filestab/           # Files tab (file manager)
│   │   ├── progresstab/        # Downloads tab (tab filter, active/completed items, video thumbnails)
│   │   ├── settings/           # Settings screen
│   │   └── component/          # Home-specific components (BottomNavBar, etc.)
│   ├── browser/               # BrowserWebViewScreen, VideoSelectBottomSheet, VideoDownloadFab
│   ├── bookmarks/             # Bookmarks & History screen
│   ├── searchengine/          # Search engine picker bottom sheet
│   ├── privacypolicy/         # Privacy policy WebView
│   ├── premium/               # Premium/subscription
│   ├── component/             # 12 reusable composables (AppHeaderBar, SearchBar, etc.)
│   ├── customview/            # Custom views
│   ├── main/                  # MainViewModel (onboarding state)
│   └── theme/                 # App theme
└── utils/                      # AnalyticsHelper, DefaultBrowserHelper, FileUtils, etc.
```

**Ads module:**
```
com.asianmobile.privatebrower.ads
├── config/                     # Constant.kt (screen codes, ad IDs)
├── customview/
├── data/
├── listener/
├── tracking/
├── ui/
│   ├── compose/                # BannerAd, NativeAdInternal composables
│   ├── dialog/
│   ├── interstitial/           # InterstitialUtil, InterstitialLauncherUtil
│   ├── openads/                # App Open Ads
│   └── rewarded/               # Rewarded Ads
└── utils/
```

### 2.3. MVVM Setup for Each Screen
Khi tạo một màn hình mới, **LUÔN** tạo 3 file trong feature package (ví dụ: `ui/newfeature`):

1. **`[Feature]Screen.kt`**: Contains the Composable UI.
2. **`[Feature]ViewModel.kt`**: Contains business logic. Must be annotated with `@HiltViewModel` and use `@Inject constructor()`.
3. **`[Feature]UiState.kt`**: A Kotlin `data class` representing the UI state.

**Example ViewModel Pattern:**
```kotlin
@HiltViewModel
class NewFeatureViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(NewFeatureUiState())
    val uiState: StateFlow<NewFeatureUiState> = _uiState.asStateFlow()
}
```

## 3. Coding Rules & Constraints

### 3.1. String Resources (CRITICAL)
- **TUYỆT ĐỐI KHÔNG** được hardcode chuỗi văn bản trực tiếp trong Composable. Ví dụ sai: `Text("Hello")`.
- **TẤT CẢ** các chuỗi văn bản (label, title, button text, description, error message,...) **PHẢI** được khai báo trong `res/values/strings.xml`, sau đó dùng `stringResource()` để lấy ra trong Compose.
- Nếu một chuỗi chưa có trong `strings.xml`, **PHẢI** thêm nó vào file đó trước khi sử dụng.

**Quy tắc đặt tên key trong `strings.xml`**: dùng `snake_case`, rõ nghĩa, theo dạng `[màn hình]_[vị trí/mục đích]`. Ví dụ:
```xml
<string name="home_search_placeholder_text">Search or type URL</string>
<string name="settings_clear_history_title">Clear History</string>
```

**Cách dùng trong Composable:**
```kotlin
import androidx.compose.ui.res.stringResource
import com.asianmobile.privatebrower.R

Text(text = stringResource(id = R.string.home_search_placeholder_text))
```

### 3.2. Color Resources (CRITICAL)
- **TUYỆT ĐỐI KHÔNG** được hardcode hex color trực tiếp trong Composable (ví dụ: `Color(0xFF000000)` hoặc `Color.Black` khi code UI element).
- **TẤT CẢ** màu sắc **PHẢI** được khai báo trong `res/values/colors.xml` và truy xuất qua `colorResource()`.

**Naming Convention:** Khi đặt tên cho các màu mới, bắt buộc sử dụng cấu trúc `colors_mã màu` (ví dụ: `colors_0D0D0D` cho màu `#0D0D0D`, `colors_FF0000` cho màu đỏ). **Ngoại lệ:** Đối với các màu chủ đạo đặc biệt của dự án (màu primary), không bắt buộc tuân theo quy tắc này, có thể đặt tên theo chức năng (ví dụ: `colorPrimary`).

**Ví dụ trong `colors.xml`:**
```xml
<color name="colors_0D0D0D">#0D0D0D</color>
<color name="colors_007BFD">#007BFD</color>
<color name="colors_808080">#808080</color>
<color name="colorPrimary">#2196F3</color>  <!-- Ngoại lệ: màu primary -->
```

**Cách dùng trong Composable:**
```kotlin
import androidx.compose.ui.res.colorResource
import com.asianmobile.privatebrower.R

Text(color = colorResource(id = R.color.colors_0D0D0D))
```

### 3.3. Navigation
- Defined in `navigation/NavGraph.kt`.
- `Routes` object contains all constant string keys for destinations.
- Use `navController.navigate(Routes.DESTINATION)`. Handle `popUpTo` logic to prevent backing into Splash or Intro screens.
- Safe navigation via `navController.safeNavigate()` and `navController.safePopBackStack()` defined in `NavExtensions.kt`.

### 3.4. Coding Standards
- No Java code. Only Kotlin.
- Leverage Compose cleanly with specific `Modifier` chains.
- Keep Composables pure and extract side effects into `LaunchedEffect` or `ViewModel` events.
- Add descriptive `@Preview` functions for all major screens and UI components.

## 4. App Flow (Navigation Flow)

Luồng chính của ứng dụng đi theo thứ tự:

```
Splash → Language → Intro → [Premium?] → Permission → SetDefaultBrowser → Home
```

Chi tiết:
1. **Splash**: Hiển thị logo, init ads consent, kiểm tra billing, load remote config. Session 2 trở đi có thể hiện Premium Splash Return.
2. **Language**: Cho người dùng chọn ngôn ngữ. Sau khi chọn sẽ `recreate()` Activity.
3. **Intro**: Onboarding screens giới thiệu app. Có thể show Premium Onboarding First nếu remote config bật.
4. **Permission**: Yêu cầu quyền truy cập storage.
5. **SetDefaultBrowser**: Hỏi user set app làm default browser (RoleManager API 29+).
6. **Home**: Màn hình chính với 4 tabs (Browser/Tabs/Files/Progress), drawer navigation, và banner ad.
7. **Premium**: Màn hình mua premium, được gọi từ nhiều nơi (onboarding, splash return, in-app drawer).

**MainViewModel** quản lý trạng thái onboarding qua DataStore:
- `completeLanguage()`, `completeIntro()`, `completePermission()`
- `uiState.getNextScreen(hasPermission)` xác định màn tiếp theo dựa trên trạng thái đã hoàn thành.

## 5. Ads Integration Guide

Module `:ads` cung cấp các Composable và Utility class sẵn dùng.

### 5.1. Native Ads
```kotlin
import com.asianmobile.privatebrower.ads.ui.compose.NativeAdInternal
import com.asianmobile.privatebrower.ads.config.SCREEN_HOME // screen code constant

NativeAdInternal(
    screenCode = SCREEN_HOME,
    modifier = Modifier.fillMaxWidth()
)
```

### 5.2. Banner Ads
```kotlin
import com.asianmobile.privatebrower.ads.ui.compose.BannerAd

BannerAd(
    modifier = Modifier.fillMaxWidth()
)
```

### 5.3. Interstitial Ads
```kotlin
import com.asianmobile.privatebrower.ads.ui.interstitial.InterstitialUtil

InterstitialUtil.getInstance().showInterstitialAd(activity) {
    // Callback sau khi ad đóng hoặc fail → navigate tiếp
    navController.navigate(Routes.NEXT_SCREEN)
}
```

### 5.4. App Open Ads
App Open Ads hiển thị khi user mở/quay lại app. Quản lý qua `InterstitialUtil.getInstance().openAd`.

**Quy tắc quan trọng:** Khi hiển thị Interstitial hoặc navigate sang màn mới, **PHẢI** tắt App Open Ads để tránh xung đột:
```kotlin
InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
```
*`MainActivity.onStart()` sẽ tự re-enable `needShowOpenAds = true`.*

### 5.5. Navigate with Interstitial Ad Pattern
Trong `NavGraph.kt` có hàm `navigateWithAd()` để kết hợp hiển thị interstitial trước khi navigate:
```kotlin
navigateWithAd(context) {
    navController.safeNavigate(Routes.DESTINATION, ignoreDebounce = true) {
        popUpTo(Routes.CURRENT) { inclusive = true }
    }
}
```

## 6. Git Commit Protocol
- Sau khi hoàn thành code, **PHẢI** tự động tạo commit.
- Sử dụng tiếng Anh, rõ ràng, mạch lạc.
- **Pattern commit message:**
  - `Handle feature [Tên Feature]`: Khi hoàn thành tính năng mới.
  - `Handle UI [Tên Component/Screen]`: Khi hoàn thiện giao diện.
  - `Fix bug crash feature [Tên Feature]`: Khi sửa lỗi crash.
  - `Fix bug UI [Tên Component/Screen]`: Khi sửa lỗi giao diện.
  - Các tiền tố khác: `Update`, `Refactor`, `Remove`.

  *(Ví dụ: `Handle feature Channel List`, `Handle UI Home Screen`, `Fix bug crash feature Player`)*

## 7. Figma Icon & Image Export

Khi cần export icon/image từ Figma design để dùng trong app, **PHẢI** sử dụng pipeline sau:

### 7.1. Tool đã cài đặt
- **`svg2vectordrawable`** (CLI: `s2v`) — đã cài global qua npm
- **Script tự động:** `.agents/skills/android_developer/scripts/svg_to_drawable.js`

### 7.1.1. Phân Loại Asset: Vector Icon vs Image/Logo (CRITICAL)

> ⚠️ **TRƯỚC KHI EXPORT**, phải xác định asset thuộc loại nào để chọn đúng phương pháp.

| Tiêu chí | Vector Icon | Image / App Logo |
|---|---|---|
| **Số màu** | 1-2 màu (mono/dual-tone) | Nhiều màu, gradient phức tạp |
| **Cấu trúc Figma** | Frame → Vector/Path đơn giản | Frame → nhiều layer, mask, gradient |
| **Ví dụ** | Tab icons, action buttons, nav icons | App logos (Google, Instagram...), photos |
| **Tên layer** | `solar:home-2-bold`, `eva:mic-fill` | `App logo 088`, `icons8-x 1` |
| **Cần tint?** | ✅ Có (theme/state) | ❌ Không (giữ multi-color) |
| **Export** | SVG → VectorDrawable (Section 7.3) | Multi-layer VectorDrawable hoặc PNG (Section 7.5) |
| **Compose** | `Icon()` + `tint` | `Image()` + `ContentScale.Fit` + `clip()` |
| **Đặt tên** | `ic_<name>.xml` | `ic_logo_<name>.xml` hoặc `img_logo_<name>.png` |

**Cách nhận biết nhanh:**
1. `get_screenshot` → multi-color hoặc app logo → **Image**
2. `get_metadata` → nhiều child layers (mask, gradient, clip-path) → **Image**
3. `get_design_context` trả về > 2 SVG fragments → **Image**
4. Tên layer chứa "App logo", "logo", tên app → **Image**

### 7.2. Quy Tắc Xác Định Node Export (CRITICAL)

> ⚠️ **QUAN TRỌNG:** Designer **LUÔN** đặt icon bên trong 1 frame (container) nhằm đồng bộ kích thước.
> Khi export icon, **BẮT BUỘC** export cả **frame bên ngoài** (node cha chứa icon), **KHÔNG** export riêng vector/path con bên trong.

**Cấu trúc icon phổ biến trong Figma:**
```
[Frame 24×24 hoặc 32×32]     ← ✅ EXPORT NODE NÀY (frame container)
  └── [Vector/Group]          ← ❌ KHÔNG export riêng node này
```

**Lý do:** Frame đóng vai trò viewBox, đảm bảo:
- Kích thước đồng nhất cho tất cả icon (24dp, 32dp, 42dp...)
- Padding/gap nội bộ giữa vector path và viền icon đã được designer chuẩn hóa
- Khi code, chỉ cần **1 `Modifier.size()` duy nhất** = kích thước frame (chia 1.3)

**Cách nhận biết frame container:**
1. Dùng `get_metadata` → tìm node có `name` mô tả icon (vd: `solar:home-2-bold`, `eva:mic-fill`)
2. Node đó thường là frame có kích thước cố định (24×24, 32×32, 42×42...)
3. Các node con bên trong là vector/group chứa path thực tế

### 7.3. Quy Trình Export Icon (CRITICAL — Áp dụng cho TẤT CẢ icons/logos)

> ⚠️ **KHÔNG dùng `svg_to_drawable.js` script** — script gây lỗi:
> - Icon dùng **stroke** → mất stroke, icon rỗng
> - **Viewport không vuông** → icon méo khi render
> - **Path format sai** → Android không parse → hiện icon lỗi (Android robot)
> 
> **→ Dùng Figma REST API** export SVG + user convert bằng **Android Studio Vector Asset**.

> ⚠️ Figma MCP (`get_design_context`) tách frame thành fragments — **KHÔNG** cho 1 SVG đầy đủ.
> **→ Dùng Figma REST API** để export frame tổng thành 1 SVG hoàn chỉnh.

**Bảng quy đổi kích thước:**
| Frame Figma | width/height dp (÷1.3) | viewportWidth/Height |
|---|---|---|
| 24×24 | 18dp | 24 |
| 28×28 | 22dp | 28 |
| 32×32 | 25dp | 32 |
| 42×42 | 32dp | 42 |
| 56×56 | 43dp | 56 |

### 7.4. Export — Chi tiết quy trình

#### Figma Access Token (CRITICAL — Kiểm tra TRƯỚC KHI export)

**BẮT BUỘC** kiểm tra trước khi gọi Figma REST API:
1. **Chưa có token** → **DỪNG LẠI**, hỏi user cung cấp token
2. **Đã có token** → gọi API, kiểm tra response

**Xử lý lỗi:**
| HTTP Status | Nguyên nhân | Hành động |
|---|---|---|
| `403` | Token hết hạn / không quyền | Hỏi user token mới |
| `401` | Token không hợp lệ | Hỏi user kiểm tra lại |
| `404` | File key / node ID sai | Kiểm tra Figma URL |
| `429` | Rate limit | Chờ rồi thử lại |

> ⚠️ **KHÔNG** tiếp tục export nếu thiếu token hoặc token lỗi.
> **KHÔNG** cố dùng MCP fragments để thay thế.

**Quy trình (sau khi có token hợp lệ):**

**Bước 1:** Dùng **Figma REST API** export SVG:
```bash
TOKEN="<FIGMA_ACCESS_TOKEN>"
FILE_KEY="<FILE_KEY>"          # Từ URL: figma.com/design/<fileKey>/...
NODE_IDS="11010:180,11010:194" # Export nhiều node cùng lúc

curl -sS -H "X-Figma-Token: $TOKEN" \
  "https://api.figma.com/v1/images/$FILE_KEY?ids=$NODE_IDS&format=svg" \
  -o /tmp/figma_images.json

node -e 'const j=require("/tmp/figma_images.json"); if (j.err) { console.error(j.err); process.exit(1) }'

# Download SVG — tên TRÙNG với XML placeholder
SVG_URL=$(node -e 'const j=require("/tmp/figma_images.json"); console.log(j.images["11010:180"])')
curl -L "$SVG_URL" -o ".agents/resources/svg/ic_logo_instagram.svg"
```

**Bước 2:** Tạo file **placeholder VectorDrawable rỗng** trong `drawable/` (để code compile):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="43dp"
    android:height="43dp"
    android:viewportWidth="56"
    android:viewportHeight="56">
    <!-- PLACEHOLDER: Convert từ .agents/resources/svg/ bằng Android Studio -->
    <!-- File > New > Vector Asset > Local File > chọn SVG tương ứng -->
</vector>
```
> **width/height** = kích thước Figma frame ÷ 1.3
> **viewportWidth/Height** = kích thước Figma frame gốc

**Bước 3:** Thông báo cho user convert bằng Android Studio:
```
📋 CẦN CONVERT THỦ CÔNG bằng Android Studio:
1. Chuột phải drawable/ → New → Vector Asset
2. Asset Type: Local file (SVG, PSD)
3. Path: .agents/resources/svg/<tên>.svg
4. Name: <tên> (trùng placeholder)
5. Size: <width>dp × <height>dp
6. Next → Finish → Overwrite placeholder
```

**Thư mục SVG:** `.agents/resources/svg/` — chứa SVG gốc + `README.md` hướng dẫn convert.

**Quy tắc code cho app logos (sau khi convert):**
```kotlin
// ✅ ĐÚNG — Image composable, KHÔNG tint, clip rounded corners
Image(
    painter = painterResource(R.drawable.ic_logo_google),
    contentDescription = "Google",
    contentScale = ContentScale.Fit,
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._43sdp))
        .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
)

// ❌ SAI — Icon composable + tint sẽ phá hủy multi-color
Icon(
    painter = painterResource(R.drawable.ic_logo_google),
    tint = Color.White  // MẤT HẾT MÀU GỐC!
)
```

### 7.6. Quy tắc đặt tên
- **Vector icon đơn giản:** Prefix `ic_` (tự động thêm bởi script), `snake_case`
  - Ví dụ: `ic_tab_home.xml`, `ic_nav_back.xml`, `ic_close.xml`
- **App logo / vector phức tạp:** Prefix `ic_logo_`, `snake_case`
  - Ví dụ: `ic_logo_google.xml`, `ic_logo_instagram.xml`, `ic_logo_facebook.xml`
- **Image (PNG/WebP):** Prefix `img_`, `snake_case`
  - Ví dụ: `img_home_banner.webp`, `img_onboarding_1.png`

### 7.7. Lưu ý quan trọng
- Script `svg_to_drawable.js` tự động fix CSS `var()` colors, percentage dimensions
- **KHÔNG** lưu file `.svg` vào `res/` — Android chỉ chấp nhận `.xml` hoặc `.png`
- Luôn verify output XML bằng preview — nếu lỗi → chuyển sang quy trình Android Studio (Section 7.5)
- Nếu icon có tint đơn sắc, dùng `--tint` để Android tự tint thay vì hardcode color trong path

## 8. Figma-to-Code UI Update Workflow

Khi update UI theo Figma design, tuân thủ quy trình sau:

### 8.0. Phân Tích Bắt Buộc (Mandatory Analysis Protocol)

**QUY TẮC TỐI THƯỢNG:** Khi nhận được yêu cầu sửa lỗi giao diện (Bug Fix) hoặc update UI theo Figma, AI **BẮT BUỘC** phải thực hiện các bước phân tích sau đây trước khi bắt đầu bất kỳ hành động code nào. Tuyệt đối **KHÔNG ĐƯỢC ĐOÁN MÒ (NO GUESSING)**.

#### Bước 1: Làm rõ yêu cầu (Requirements Clarification)
- Phải đọc kỹ yêu cầu của người dùng để hiểu rõ mục tiêu cuối cùng.
- Nếu yêu cầu còn mơ hồ, **PHẢI** đặt câu hỏi để làm rõ trước khi thực hiện.

#### Bước 2: Download/Screenshot ảnh Figma — BẮT BUỘC (CRITICAL)

> ⚠️ **TUYỆT ĐỐI KHÔNG** bắt đầu code fix khi chưa xem ảnh Figma.
> **PHẢI** download/screenshot design Figma để có hình ảnh cụ thể trước khi phân tích.
> Việc chỉ đọc design context text mà không xem ảnh dẫn đến hiểu sai vị trí, layout, alignment.

**Quy trình lấy ảnh Figma:**

1. **Export ảnh PNG từ Figma REST API** cho node liên quan:
```bash
# Export ảnh design (scale=2 cho rõ nét)
wget --header="X-FIGMA-TOKEN: <TOKEN>" -O - \
  "https://api.figma.com/v1/images/<FILE_KEY>?ids=<NODE_IDS>&format=png&scale=2"
# Download ảnh về thư mục artifacts
wget -O <artifact_dir>/figma_reference_<component>.png "<URL_TRẢ_VỀ>"
```

2. **Hoặc dùng Figma MCP `mcp__figma.get_screenshot`** nếu có:
```
Tool: mcp__figma.get_screenshot
Params: { "fileKey": "<FILE_KEY>", "nodeId": "<NODE_ID>", "maxDimension": 2048 }
```
Tool trả về screenshot URL và curl instruction; tải ảnh về artifact bằng `curl -L "<URL>" -o <artifact_dir>/figma_reference_<component>.png`.

3. **Embed ảnh vào artifact** để so sánh trực quan với screenshot app:
```markdown
## So sánh Figma vs App
![Figma design](absolute/path/to/figma_reference.png)
![App screenshot](absolute/path/to/app_screenshot.png)
```

**Nếu KHÔNG thể download ảnh** (API lỗi, token hết hạn...):
- Dùng `mcp__figma.get_design_context` để lấy thông tin chi tiết layout
- **PHẢI** phân tích kỹ output: position, size, gap, alignment, colors, layer order
- **KHÔNG** dựa vào trí nhớ hoặc giả định về design

#### Bước 3: So sánh Screenshot App vs Figma — Pixel-level (CRITICAL)

**BẮT BUỘC** đặt cạnh nhau và liệt kê **TỪNG ĐIỂM** khác biệt dưới dạng bảng:

| Thuộc tính | Figma Design | App hiện tại | Cần sửa? |
|---|---|---|---|
| Vị trí icon X | Trên cùng, căn giữa (Column, alignItems: center) | Overlay góc phải thumbnail (Box, align: TopEnd) | ✅ |
| Màu nền popup | #333538 (dark) | Trắng (Material3 default) | ✅ |
| Khoảng cách items | gap: 12px (~9sdp) | gap: 6sdp | ✅ |
| Font weight title | SemiBold 600 | Medium 500 | ✅ |

> ⚠️ **KHÔNG** chỉ nhìn sơ qua — PHẢI phân tích chi tiết:
> - **Position/Alignment**: top/center/bottom, start/center/end, overlay vs stacked
> - **Layout structure**: Column vs Row vs Box, children order
> - **Spacing**: gap, padding, margin (Figma px ÷ 1.3 = sdp)
> - **Size**: width, height, aspect ratio của từng element
> - **Colors**: background, text, icon tint, border, shadow
> - **Typography**: font family, weight, size, line height
> - **Corner radius**: border radius value
> - **Layer order**: z-index, overlay vs sequential in layout

#### Bước 4: Phân tích Code hiện tại (Deep Code Analysis)
- Sử dụng `rg`, `sed`, `nl` hoặc file reads trong Codex để tìm đoạn code đang quản lý UI đó.
- Phân tích code: đơn vị (dp/sdp), padding/margin, layout structure (Column/Row/Box).
- **Đối chiếu thông số code với Figma:**
  - "Figma yêu cầu gap 16px (~12sdp) nhưng code hiện tại đang dùng 8sdp"
  - "Figma layout = Column(alignItems: center) nhưng code dùng Box(align: TopEnd)"
  - "Figma font weight SemiBold nhưng code đang dùng Medium"
- Xác định nguyên nhân gốc rễ gây ra sự sai lệch.

#### Bước 5: Lập Kế Hoạch (Planning)
- Trình bày rõ kết quả so sánh và phân tích cho người dùng trước khi sửa.
- Đề xuất phương án sửa đổi cụ thể (Sửa file nào, dòng nào, giá trị mới là gì).
- **CHỜ XÁC NHẬN** trước khi thực hiện (nếu thay đổi lớn).
- Nếu chỉ fix nhỏ rõ ràng → có thể tiến hành luôn nhưng PHẢI giải thích.

Việc bỏ qua bước phân tích này sẽ dẫn đến kết quả UI không chính xác và lãng phí thời gian build/verify.

### 8.1. Thu thập thông tin từ Figma
1. Dùng `mcp__figma.get_screenshot` để xem tổng quan design
2. Dùng `mcp__figma.get_design_context` để lấy chi tiết: colors, fonts, spacing, layout
3. Dùng `mcp__figma.get_metadata` nếu cần xem cấu trúc node tree
4. Dùng `mcp__figma.get_variable_defs` nếu design sử dụng Figma variables

### 8.2. Mapping Design Tokens
Khi đọc design context từ Figma, mapping sang Android conventions:
- **Font Figma → Font Android:** Map `Inter` font family → font files trong `res/font/`. Nếu chưa có thì thêm mới.
- **Color Figma → Color Android:** Thêm color mới vào `colors.xml` theo naming `colors_[HEX]`
- **UI sizing rule (sdp/ssp):** Tất cả các giá trị dp/sp từ Figma **BẮT BUỘC** phải chia cho **1.3** để quy đổi sang sdp/ssp. Ví dụ: Figma 13dp → `_10sdp`, 13sp → `_10ssp`, 16dp → `_12sdp`. Dùng `dimensionResource(com.intuit.sdp.R.dimen._Xsdp)` cho kích thước/padding và `dimensionResource(com.intuit.ssp.R.dimen._Xssp)` cho typo.
- **Border Radius → RoundedCornerShape:** `RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._Xsdp))` 

### 8.3. Export assets
- **Icons:** Dùng pipeline ở mục 7 (SVG → VectorDrawable). **Luôn export frame node** (Section 7.2)
- **Images (PNG/WebP):** Download từ screenshot/export URL của Figma MCP hoặc REST API, lưu vào `res/drawable-nodpi/`
- **Lottie animations:** Lưu vào `res/raw/`

### 8.4. Icon Sizing & Layout Pattern (CRITICAL)

> ⚠️ Vì icon đã được export cả **frame bên ngoài** (Section 7.2), drawable đã bao gồm
> viewBox đúng kích thước frame với padding nội bộ chuẩn. Do đó:
> - ❌ **KHÔNG CẦN** wrap `Icon` trong `Box` chỉ để tạo gap/padding nội bộ
> - ❌ **KHÔNG CẦN** set 2 kích thước khác nhau (container vs inner icon) cho mục đích gap
> - ✅ **CHỈ CẦN** 1 `Modifier.size()` duy nhất = kích thước frame (đã chia 1.3)
> - ✅ **CHỈ CẦN** quan tâm padding/spacing giữa icon và các view xung quanh

#### Khi nào CẦN Box wrapper:
Chỉ khi Figma design có **background shape nhìn thấy** (circle, rounded rect) phía sau icon:
```
[Container Circle/Shape]     ← background bán trong suốt → CẦN Box
  └── [Icon Frame]           ← frame đã export → Icon với 1 size
```

#### Khi nào KHÔNG CẦN Box:
Khi icon standalone (không có background shape nhìn thấy):
```
[Icon Frame]                 ← frame đã export → Icon trực tiếp, 1 size
```

**Pattern 1: Icon CÓ background — CẦN Box:**
```kotlin
Box(
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._26sdp))  // Background size
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.85f))
        .clickable(onClick = onAction),
    contentAlignment = Alignment.Center
) {
    Icon(
        painter = painterResource(R.drawable.ic_favorite_outline),
        contentDescription = stringResource(R.string.channel_card_favorite),
        tint = colorResource(R.color.gray_808080),
        // Kích thước = frame size (đã chia 1.3) — drawable đã có gap nội bộ
        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
    )
}
```

**Pattern 2: Icon KHÔNG CÓ background — KHÔNG cần Box:**
```kotlin
// Frame đã export bao gồm viewBox chuẩn → chỉ cần 1 size duy nhất
Icon(
    painter = painterResource(R.drawable.ic_tab_home),
    contentDescription = stringResource(R.string.tab_home),
    tint = colorResource(R.color.colors_FFFFFF),
    modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._18sdp)) // = frame 24px / 1.3
)
```

**Bảng quyết định:**

| Figma Design | Android Code | Sizing |
|---|---|---|
| Icon + background shape nhìn thấy | `Box(size=bg)` + `Icon(size=frame)` | Box = bg size, Icon = frame/1.3 |
| Icon không background, clickable | `Icon(size=frame)` + `.clip().clickable()` | 1 size = frame/1.3 |
| Icon không clickable (hiển thị) | `Icon(size=frame)` | 1 size = frame/1.3 |

**Mapping Figma layers → Compose:**

| Figma Layer | Compose Code | Thuộc tính |
|---|---|---|
| Circle/Ellipse background | `Box` + `clip(CircleShape)` + `background()` | size, color, alpha |
| RoundedRect background | `Box` + `clip(RoundedCornerShape())` + `background()` | size, radius, color |
| Icon Frame (exported) | `Icon` + `painterResource()` | drawable, tint, 1 size |
| Gradient background | `Box` + `background(Brush.xxxGradient())` | colors, direction |

### 8.5. Ripple Effect — Modifier Order (CRITICAL)

Ripple effect trong Compose **BỊ ẢNH HƯỞNG BỞI THỨ TỰ MODIFIER**. Nếu đặt sai thứ tự, ripple sẽ có hình vuông thay vì theo shape của component.

**QUY TẮC BẮT BUỘC:** `.clip(shape)` phải đặt **TRƯỚC** `.clickable()` trong modifier chain.

```kotlin
// ❌ SAI — ripple hình vuông
Modifier
    .size(26.sdp)
    .background(Color.White, CircleShape)  // vẽ hình tròn nhưng không clip
    .clickable(onClick = action)            // ripple không biết shape → vuông

// ❌ SAI — clickable trước clip
Modifier
    .size(26.sdp)
    .clickable(onClick = action)            // ripple tạo ở đây → vuông
    .clip(CircleShape)                      // clip SAU → không ảnh hưởng ripple

// ✅ ĐÚNG — clip trước clickable
Modifier
    .size(26.sdp)
    .clip(CircleShape)                      // clip TRƯỚC → ripple bị giới hạn trong circle
    .background(Color.White.copy(alpha = 0.85f))
    .clickable(onClick = action)            // ripple hình tròn ✓
```

**Thứ tự modifier chuẩn cho mọi interactive component:**
```
.size()          → kích thước
.shadow()        → shadow (nếu có, phải trước clip)
.clip(shape)     → clip shape (CRITICAL: trước clickable)
.background()    → màu nền
.border()        → viền (nếu có)
.clickable()     → click + ripple (SẼ BỊ CLIP theo shape phía trên)
.padding()       → padding nội dung bên trong
```

**Áp dụng cho từng loại component:**

| Component | Shape | Cách handle |
|---|---|---|
| **Icon có background tròn** | `CircleShape` | `Box` + `.clip(CircleShape).clickable()` + `Icon` bên trong |
| **Icon KHÔNG có background** | — | `IconButton(onClick)` (tự xử lý ripple tròn) |
| **Card** | `RoundedCornerShape(Xsdp)` | `.clip(RoundedCornerShape(...)).clickable()` |
| **Tab item** | `RoundedCornerShape(Xsdp)` | `.clip(RoundedCornerShape(...)).clickable()` |
| **List item (full width)** | Không cần clip | `.clickable()` trực tiếp |
| **Settings row** | Không clip (ripple vuông OK) | `.clickable()` trước `.padding()` |

### 8.6. Clickable Icon Pattern (CRITICAL)

Khi Figma có icon clickable, chọn pattern dựa trên **có/không có background layer**:

> ⚠️ **KHÔNG dùng `IconButton`** khi cần kích thước chính xác theo Figma!
> `IconButton` có **minimum touch target = 48dp** (Material 3 mặc định), sẽ phình to hơn design.

> **LƯU Ý:** Icon đã export cả frame → drawable đã có viewBox chuẩn với padding nội bộ.
> Chỉ cần **1 `Modifier.size()` duy nhất** = kích thước frame (chia 1.3).
> **KHÔNG CẦN** wrap Box chỉ để tạo gap giữa icon và container.

**Trường hợp 1: Icon CÓ background (circle/shape) — CẦN Box**
```kotlin
// Box chỉ cần khi có background NHÌN THẤY
Box(
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._26sdp))  // Background size
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.85f))
        .clickable(onClick = onFavoriteClick),
    contentAlignment = Alignment.Center
) {
    Icon(
        painter = painterResource(R.drawable.ic_heart_outline),
        contentDescription = stringResource(R.string.favorite),
        tint = colorResource(R.color.gray_808080),
        // 1 size duy nhất = frame size (chia 1.3) — drawable đã có gap nội bộ
        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
    )
}
```

**Trường hợp 2: Icon KHÔNG CÓ background — KHÔNG cần Box**
```kotlin
// ❌ SAI — clickable trước clip → ripple vuông
Icon(
    painter = painterResource(R.drawable.ic_profile),
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
        .clickable(onClick = onProfileClick)  // ripple vuông!
)

// ❌ SAI — IconButton phình to 48dp, phá layout
IconButton(onClick = onProfileClick) {
    Icon(...)  // touch area = 48dp → spacing sai!
}

// ❌ SAI — Box wrapper không cần thiết (chỉ để tạo gap)
Box(modifier = Modifier.size(_26sdp)) {
    Icon(modifier = Modifier.size(_14sdp))  // 2 size khác nhau chỉ để gap → THỪA
}

// ✅ ĐÚNG — 1 size duy nhất, clip trước clickable
Icon(
    painter = painterResource(R.drawable.ic_profile),
    contentDescription = stringResource(R.string.profile),
    tint = Color.White,
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._18sdp)) // = frame 24px / 1.3
        .clip(CircleShape)
        .clickable(onClick = onProfileClick)
)
```

**Bảng quyết định:**

| Figma Design | Android Code | Sizing |
|---|---|---|
| Icon + background shape nhìn thấy | `Box(size=bg)` + `Icon(size=frame)` | Box = bg size, Icon = frame/1.3 |
| Icon không background, clickable | `Icon(size=frame)` + `.clip().clickable()` | 1 size = frame/1.3 |
| Icon không clickable (hiển thị) | `Icon(size=frame)` trực tiếp | 1 size = frame/1.3 |

## 10. Feature: Download & Video Detection

### 10.1. Tổng quan kiến trúc

Tính năng download bao gồm 2 luồng chính:
1. **Standard Download**: WebView detect download link → `onDownloadStart` → enqueue → `DownloadForegroundService`
2. **Video Detection**: `VideoSniffer` intercept resource requests → detect video URLs → FAB + Bottom Sheet → enqueue → `DownloadForegroundService`

```
┌─ BrowserScreen ─────────────────────────────────────────────┐
│  WebView ← VideoSniffer intercepts resource requests        │
│  VideoDownloadFab (gradient, pulse, badge)                  │
│  VideoSelectBottomSheet (grid thumbnails, select, download) │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌─ DownloadRepository ────────────────────────────────────────┐
│  enqueue(fileName, url, path, mimeType, headers)            │
│  → DownloadEntity + requestHeaders (JSON serialized)        │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─ DownloadForegroundService ─────────────────────────────────┐
│  OkHttp GET + stored headers + CookieManager + User-Agent   │
│  → Stream bytes → Write to /Downloads/PrivateBrowser/       │
│  → Update DB progress every 500ms (Room Flow → UI reactive) │
│  → Single foreground notification + completion notification  │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─ ProgressTabScreen (Downloads tab) ─────────────────────────┐
│  Tab filter: All / Downloading / Completed                  │
│  Active: Coil thumbnail + progress bar + pause/resume/cancel│
│  Completed: Coil thumbnail + click → FileProvider → player  │
└─────────────────────────────────────────────────────────────┘
```

### 10.2. Key Components

#### Data Layer

| File | Vai trò |
|---|---|
| `data/database/entity/DownloadEntity.kt` | Room entity: fileName, url, path, mimeType, sizeBytes, downloadedBytes, status (PENDING/RUNNING/PAUSED/COMPLETED/FAILED), requestHeaders (JSON) |
| `data/database/dao/DownloadDao.kt` | Room DAO: CRUD + observeActive/observeCompleted (Flow), updateProgress, updateStatus, updateSize, updatePath, updateCompletedAt |
| `data/database/PrivateBrowserDatabase.kt` | Room DB development version 1 + destructive fallback; chưa quản lý migration |
| `data/repository/DownloadRepository.kt` | Interface: enqueue(headers), observeAll/Active/Completed, updateProgress, cancel, delete |
| `data/repository/impl/DownloadRepositoryImpl.kt` | Implementation: serialize headers Map → JSON string |

#### Video Detection

| File | Vai trò |
|---|---|
| `data/browser/VideoSniffer.kt` | Intercept WebView requests, detect direct video/HLS, filter images/segments, merge DOM poster/title, deduplicate và expose `detectedVideos` |
| `data/browser/DetectedVideo.kt` | Data class: url, mimeType, displayName, fileExtension, isHls, headers (original request headers) |

**VideoSniffer detection logic:**
- ✅ Detect: `.mp4`, `.webm`, `.mov`, `.avi`, `.mkv`, `.flv`, `video/*` mime, Facebook CDN (`"video" + "fbcdn"`)
- ❌ Filter: `.jpg/.png/.gif/.webp/.svg/.ico/.bmp`, `image/*` mime, `.m3u8` (HLS playlist), `.ts` segments (HLS chunks)

#### Service

| File | Vai trò |
|---|---|
| `service/DownloadForegroundService.kt` | Foreground service: OkHttp streaming download, HTTP Range resume, concurrent downloads (ConcurrentHashMap), headers/cookies/UA passthrough, single notification + completion notification with PendingIntent |

**Download request headers flow:**
1. `VideoSniffer` captures original request `headers` from WebView
2. `BrowserViewModel.downloadVideos()` passes `headers` to `DownloadRepository.enqueue()`
3. `DownloadRepositoryImpl` serializes `Map<String,String>` → JSON string → `requestHeaders` column
4. `DownloadForegroundService` reads entity → deserializes JSON → adds to OkHttp `Request.Builder`
5. Service also adds: `CookieManager.getCookie(url)` + mobile Chrome `User-Agent`

#### UI Components

| File | Vai trò |
|---|---|
| `ui/browser/VideoSelectBottomSheet.kt` | ModalBottomSheet: "Select items" header, "Select All" toggle, LazyVerticalGrid 3 cột với `VideoThumbnail` (Coil VideoFrameDecoder), download button |
| `ui/browser/BrowserScreen.kt` | `VideoDownloadFab` composable: gradient `#5B6FFB→#7C5BFB`, pulse animation (infinite scale 1.0→1.15), badge count, positioned `BottomEnd` |
| `ui/home/progresstab/ProgressTabScreen.kt` | Tab filter (All/Downloading/Completed), `ActiveDownloadItem` (progress bar, pause/resume/cancel), `CompletedDownloadItem` (click → FileProvider → video player), `DownloadVideoThumbnail` (Coil) |
| `ui/home/progresstab/ProgressTabViewModel.kt` | Combine observeActive + observeCompleted flows, `onOpenFile()` via FileProvider |

#### Video Thumbnails

Sử dụng **Coil `coil-video`** (`VideoFrameDecoder`) — extract frame tại t=0 mà KHÔNG download full video:

```kotlin
// Build request with VideoFrameDecoder
coil.request.ImageRequest.Builder(context)
    .data(videoUrlOrFilePath)
    .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
    .size(128)  // Small thumbnail
    .memoryCacheKey("thumb_${url.hashCode()}")
    .build()
```

| Vị trí | Data source | Fallback |
|---|---|---|
| VideoSelectBottomSheet | Video URL + cookies | `ic_video_file` icon |
| ProgressTab - Active | Video URL | `ic_video_file` icon |
| ProgressTab - Completed | Local file path (ưu tiên, nhanh hơn) | `ic_video_file` icon |

### 10.3. Database Schema

```sql
CREATE TABLE downloads (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    url TEXT NOT NULL,
    path TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size_bytes INTEGER DEFAULT 0,
    downloaded_bytes INTEGER DEFAULT 0,
    status TEXT NOT NULL,          -- PENDING/RUNNING/PAUSED/COMPLETED/FAILED
    error_message TEXT,
    created_at INTEGER,
    completed_at INTEGER,
    request_headers TEXT DEFAULT '', -- JSON: {"Cookie":"...", "Referer":"..."}
    audio_url TEXT DEFAULT '',
    thumbnail_url TEXT DEFAULT ''
);
```

### 10.4. Notification Behavior

- **During download**: Single foreground notification (`NOTIFICATION_ID = 1001`) with progress bar, clickable → opens Downloads tab
- **On completion**: Non-ongoing notification (`COMPLETED_NOTIFICATION_BASE + id`) with `setAutoCancel(true)`, clickable → opens Downloads tab
- **When all done**: `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`

### 10.5. Dependencies (đã có trong project)

```toml
# gradle/libs.versions.toml
coil-compose = { module = "io.coil-kt:coil-compose", version = "2.7.0" }
coil-video = { module = "io.coil-kt:coil-video", version = "2.7.0" }
```

### 10.6. Files tham chiếu

Khi sửa tính năng download, cần xem xét các files sau:

**Data:**
- `data/database/entity/DownloadEntity.kt`
- `data/database/dao/DownloadDao.kt`
- `data/database/PrivateBrowserDatabase.kt`
- `data/repository/DownloadRepository.kt`
- `data/repository/impl/DownloadRepositoryImpl.kt`
- `data/browser/VideoSniffer.kt`

**Service:**
- `service/DownloadForegroundService.kt`

**UI:**
- `ui/browser/VideoSelectBottomSheet.kt`
- `ui/browser/BrowserScreen.kt` (VideoDownloadFab)
- `ui/browser/BrowserViewModel.kt` (downloadVideos, showVideoSheet)
- `ui/browser/BrowserUiState.kt` (detectedVideos, showVideoSheet)
- `ui/home/progresstab/ProgressTabScreen.kt`
- `ui/home/progresstab/ProgressTabViewModel.kt`
- `ui/home/progresstab/ProgressTabUiState.kt`

**DI:**
- `di/BrowserModule.kt` (TabManager with DownloadRepository)

**Resources:**
- `res/drawable/ic_video_file.xml`
- `res/drawable/ic_check_white.xml`
- `res/drawable/ic_download_arrow.xml`
