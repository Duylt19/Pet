# Private Browser — Agent Context & Project Guidelines

## Tổng Quan

Project **Private Browser: Safe & Secure** (`com.asianmobile.privatebrower`) — trình duyệt web Android tập trung vào privacy, tải video, và multi-tab management.

> **Trạng thái:** ✅ Hoàn thành 12/12 milestones. Xem [IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md).

## Mục Đích Của Tài Liệu Này

Tài liệu này giúp agent AI và developer hiểu rằng:
1. **Project kế thừa kiến trúc base code** từ FileRecovery → đã được customize hoàn toàn cho Private Browser.
2. **Mọi pattern, convention, và kiến trúc đã được thiết lập** — developer cần tuân thủ, KHÔNG được thay đổi kiến trúc hoặc tạo pattern mới mà không có sự đồng ý.
3. **Đọc docs/ folder trước khi code** — đặc biệt `docs/08_AGENT_CODING_GUIDELINES.md`.

### Legacy đã loại bỏ

- Giữ nguyên flow khởi động `Splash → Language → Intro → Set Default Browser → Permission`.
- Không khôi phục các màn cũ của base app: Photo Recovery Get Started, IPTV License Agreement,
  LiveTV banner/channel/playlist UI và asset `channels_curated.json`.
- Không thêm lại ad placement Favorite/Playlist/Channel của LiveTV; ads hợp lệ phải nằm trong
  `NativeAdPlacementCatalog` và gắn với màn Private Browser đang tồn tại.

## Nguồn Gốc Từ Base Project

### Những Gì Được Giữ Lại (kế thừa)

| Flow | Mô tả | Package |
|------|--------|---------|
| **Splash** | Khởi động, init ads, billing check | `ui/splash/` |
| **Language** | Chọn 1/11 ngôn ngữ | `ui/language/` |
| **Intro** | Onboarding giới thiệu app | `ui/intro/` |
| **Permission** | Yêu cầu quyền | `ui/permission/` |
| **Premium** | In-app purchase (Go Ad-Free) | `ui/premium/` |
| **Ads** | Banner, Native, Interstitial, OpenAd | `:ads` module |
| **DataStore** | Preferences manager | `data/local/` |
| **Theme** | Material 3 Compose theme | `ui/theme/` |

### Những Gì Đã Thêm Mới (Private Browser specific)

| Feature | Package | Milestone |
|---------|---------|-----------|
| **BrowserEngine + WebView** | `data/browser/`, `ui/browser/` | M4 |
| **TabManager (Normal/Incognito)** | `data/browser/`, `ui/home/tabstab/` | M5 |
| **VideoSniffer + Download** | `data/browser/`, `service/`, `ui/browser/`, `ui/home/progresstab/` | M8 |
| **Bookmarks + History** | `data/database/`, `ui/bookmarks/` | M6 |
| **File Manager** | `ui/home/filestab/` | M7 |
| **Settings** | `ui/home/settings/` | M9 |
| **Set Default Browser** | `ui/setdefault/` | M10 |
| **Search Engine Picker** | `ui/searchengine/`, `data/repository/` | M9 |
| **Room Database** | `data/database/` (4 entities, 4 DAOs) | M1 |
| **Quick Access Shortcuts** | `ui/home/browsertab/` | M3 |
| **Deep Link Handling** | `MainActivity.kt` | M12 |

### Package Name

```
App:     com.asianmobile.privatebrower
Ads:     com.asianmobile.privatebrower.ads
```

## Cấu Trúc Project

```
PrivateBrower/
├── app/                        # Main application module
│   └── src/main/java/com/asianmobile/privatebrower/
│       ├── BaseApplication.kt
│       ├── MainActivity.kt     # Single Activity (singleTop, deep link)
│       ├── data/
│       │   ├── browser/        # BrowserEngine, TabManager, VideoSniffer, DetectedVideo
│       │   ├── database/       # Room DB dev v1, entities and DAOs
│       │   ├── local/          # DataStoreManager (21 preference keys)
│       │   ├── model/          # 7 domain models
│       │   ├── repository/     # 6 repositories (interfaces + impl/)
│       │   └── usecase/        # ClearBrowsingDataUseCase
│       ├── di/                 # 6 Hilt modules
│       ├── navigation/         # NavGraph (13 routes) + NavExtensions
│       ├── service/            # DownloadForegroundService (OkHttp, resume, concurrent)
│       ├── ui/                 # 17 screen packages
│       │   ├── home/           # 4-tab container (Browser/Tabs/Files/Downloads)
│       │   ├── browser/        # WebView, VideoSelectBottomSheet, VideoDownloadFab
│       │   ├── bookmarks/      # Bookmarks & History
│       │   ├── settings/       # App settings
│       │   └── component/      # 12 reusable composables
│       └── utils/
│
├── ads/                        # Ads module (isolated)
├── docs/                       # 📖 Full documentation (38 files)
│   ├── README.md               # Index
│   ├── 01-09_*.md              # Foundation docs
│   ├── features/               # 11 feature specs
│   └── screens/                # 17 screen specs
│
├── .agents/skills/android_developer/
│   └── SKILL.md                # Agent coding guidelines (Section 10: Download)
├── IMPLEMENTATION_PROGRESS.md  # M1-M12 tracking
└── gradle/libs.versions.toml   # Dependency versions
```

## Download & Video Detection

Tính năng tự động phát hiện video khi duyệt web:

```
WebView → VideoSniffer (detect video, filter images/HLS)
  → FAB tím (gradient, pulse, badge)
  → VideoSelectBottomSheet (grid thumbnails via Coil VideoFrameDecoder)
  → DownloadRepository.enqueue() + headers/cookies (JSON serialized)
  → DownloadForegroundService (OkHttp streaming, resume, CDN auth)
  → Downloads tab (thumbnails, progress, click → FileProvider → player)
```

Key points:
- **CDN Auth**: Headers + cookies từ WebView session → lưu DB → truyền vào OkHttp request
- **Thumbnails**: DOM poster/`og:image` async, fallback `VideoFrameDecoder`, cuối cùng `ic_video_file`
- **Filtering**: Reject images và media segments; hỗ trợ direct video + HLS playlist phù hợp
- **DB**: Development version 1 + destructive fallback; chưa quản lý migration

> 📖 Chi tiết: [docs/features/F05_DOWNLOAD_MANAGER.md](docs/features/F05_DOWNLOAD_MANAGER.md) | [SKILL.md Section 10](.agents/skills/android_developer/SKILL.md)

## Navigation Flow

```
                    ┌─────────┐
                    │  Splash │
                    └────┬────┘
                         │
              ┌──────────┼──────────────┐
              │          │              │
         Session 1  Premium Return  Skip to Home
              │     (session 2+)    (session 2+)
              ▼          ▼              ▼
        ┌──────────┐ ┌─────────┐  ┌──────┐
        │ Language  │ │ Premium │  │ Home │
        └────┬─────┘ └────┬────┘  └──────┘
             │             │
             ▼             │
        ┌──────────┐       │
        │  Intro   │       │
        └────┬─────┘       │
             │             │
      ┌──────┼──────┐      │
      │             │      │
      ▼             ▼      │
┌──────────┐  ┌─────────┐  │
│Permission│  │ Premium │  │
└────┬─────┘  │Onboard  │  │
     │        └────┬────┘  │
     ▼             │       │
┌──────────────┐   │       │
│SetDefault    │   │       │
│Browser       │   │       │
└────┬─────────┘   │       │
     │             │       │
     ▼             ▼       │
   ┌───────────────────┐   │
   │       Home        │◄──┘
   │  4 tabs: Browser  │
   │  Tabs/Files/Prog  │
   └────────┬──────────┘
            │
   ┌────────┼────────────────────┐
   │        │        │           │
   ▼        ▼        ▼           ▼
Settings  Browser  Bookmarks  Premium
          WebView  History    (in-app)
```

### Routes (13 routes trong NavGraph.kt)

```kotlin
object Routes {
    const val SPLASH = "splash"
    const val INTRO = "intro"
    const val LANGUAGE = "language"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val PERMISSION = "permission"
    const val HOME = "home"
    const val PREMIUM = "premium"                       // {startByIndex}
    const val SETTINGS = "settings"
    const val SET_DEFAULT_BROWSER = "set_default_browser"
    const val BROWSER_WEBVIEW = "browser_webview"       // ?url={url}&incognito={incognito}
    const val BOOKMARKS_HISTORY = "bookmarks_history"
    const val PRIVACY_POLICY = "privacy_policy"         // ?url={url}
}
```

## Quy Tắc Quan Trọng (PHẢI TUÂN THỦ)

> **ĐỌC KỸ FILE `docs/08_AGENT_CODING_GUIDELINES.md` VÀ `.agents/skills/android_developer/SKILL.md` TRƯỚC KHI BẮT ĐẦU CODE.**

1. ❌ **KHÔNG** hardcode string → dùng `stringResource(R.string.xxx)`
2. ❌ **KHÔNG** hardcode color hex → dùng `colorResource(R.color.xxx)`
3. ✅ Đặt tên color: `colors_[mã hex]` (trừ colorPrimary)
4. ✅ Dùng sdp/ssp: giá trị Figma ÷ 1.3 → `dimensionResource(SdpR.dimen._Xsdp)`
5. ✅ Mỗi màn hình = 3 files: Screen + ViewModel + UiState
6. ✅ ViewModel phải có `@HiltViewModel` + `@Inject constructor()`
7. ✅ Navigate bằng `safeNavigate()` hoặc `navigateWithAd()`
8. ✅ Modifier order: `.size().shadow().clip().background().border().clickable().padding()`
9. ✅ Giao tiếp bằng Tiếng Việt, code bằng Tiếng Anh
10. ✅ Commit theo pattern: `Handle feature X`, `Fix bug crash feature X`
11. ⚠️ **ROOM DATABASE POLICY (BẮT BUỘC):** Trong giai đoạn development, luôn giữ `PrivateBrowserDatabase` ở `version = 1` và dùng destructive fallback khi schema thay đổi. **KHÔNG** tự tăng version, **KHÔNG** tạo migration/auto-migration khi thêm, xóa hoặc sửa column. Chỉ sau khi owner xác nhận version 2 đã được chốt làm baseline ổn định thì mới bắt đầu viết migration cho các lần tăng version tiếp theo.
12. ⚠️ **PERMISSION POLICY:** Không chỉ dựa vào `shouldShowRequestPermissionRationale()`. Media và quyền website camera/mic/location phải lưu lịch sử request; sau hai lần system request không còn hiển thị thì hướng user sang App Settings. `POST_NOTIFICATIONS` chỉ xin một lần theo ngữ cảnh khi user bắt đầu/resume download đầu tiên trên API 33+; từ chối không được chặn download hoặc gây popup lặp lại.
13. ⚠️ **ALL FILES ACCESS / VAULT POLICY:** `MANAGE_EXTERNAL_STORAGE` là nguồn quyền storage duy nhất trên API 30+ cho Photos, Videos, Audio, Files và Vault; không khai báo hoặc request `READ_MEDIA_*`/`READ_MEDIA_VISUAL_USER_SELECTED`. Mở special-access Settings bằng `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` (fallback `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`) và kiểm tra bằng `Environment.isExternalStorageManager()`, không request như runtime permission. Trên Android 10 (API 29), bật `android:requestLegacyExternalStorage="true"` và khai báo/request cả `READ_EXTERNAL_STORAGE` lẫn `WRITE_EXTERNAL_STORAGE` với `maxSdkVersion=29`; chỉ coi broad legacy storage đã granted khi cả hai quyền được cấp và `Environment.isExternalStorageLegacy()` trả về `true`, không mở All files access vì API 29 chưa hỗ trợ `MANAGE_EXTERNAL_STORAGE`. Khi user từ chối, các màn Media Library/Files/Vault hiển thị trạng thái yêu cầu quyền; Browser, Download và SAF vẫn hoạt động. API 29+ vẫn publish download qua `MediaStore.Downloads`. Trước khi phát hành Google Play phải hoàn tất Permissions Declaration và chứng minh file management/Vault là core functionality.

## Tham Khảo

| Tài liệu | Đường dẫn |
|-----------|-----------|
| **Docs Index** | [docs/README.md](docs/README.md) |
| **Download Feature** | [docs/features/F05_DOWNLOAD_MANAGER.md](docs/features/F05_DOWNLOAD_MANAGER.md) |
| **Agent Skill** | [.agents/skills/android_developer/SKILL.md](.agents/skills/android_developer/SKILL.md) |
| **Coding Rules** | [docs/08_AGENT_CODING_GUIDELINES.md](docs/08_AGENT_CODING_GUIDELINES.md) |
| **Progress** | [IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md) |
| **Roadmap** | [docs/09_IMPLEMENTATION_ROADMAP.md](docs/09_IMPLEMENTATION_ROADMAP.md) |
| **Architecture** | [docs/02_ARCHITECTURE.md](docs/02_ARCHITECTURE.md) |
| **Data Model** | [docs/05_DATA_MODEL.md](docs/05_DATA_MODEL.md) |
| **Navigation** | [docs/04_NAVIGATION_FLOW.md](docs/04_NAVIGATION_FLOW.md) |
