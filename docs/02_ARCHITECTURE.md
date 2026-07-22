# 02 — Architecture

## 1. Tổng Quan

**Private Browser** áp dụng:

- **Single-Activity Architecture** — Toàn app chỉ có 1 `MainActivity`, mọi screen là Composable trong NavHost
- **Clean Architecture** (3 lớp: UI / Domain / Data)
- **MVVM** — Mỗi screen có ViewModel quản lý state
- **Unidirectional Data Flow (UDF)** — State chảy 1 chiều từ ViewModel xuống UI; UI gửi event lên ViewModel
- **Reactive** — State expose qua `StateFlow`, UI thu qua `collectAsStateWithLifecycle()`

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose Screens + Components)│
│  - <Feature>Screen.kt                            │
│  - Reusable composables in ui/component/         │
└─────────────────────────┬───────────────────────┘
                          │ collectAsState / events
┌─────────────────────────▼───────────────────────┐
│  Presentation Layer (ViewModel + UiState)        │
│  - <Feature>ViewModel.kt (@HiltViewModel)        │
│  - <Feature>UiState.kt (data class)              │
└─────────────────────────┬───────────────────────┘
                          │ suspend / Flow
┌─────────────────────────▼───────────────────────┐
│  Domain Layer (UseCases — optional cho v1)       │
│  - Interface Repository                          │
│  - UseCase class (cho logic phức tạp)            │
└─────────────────────────┬───────────────────────┘
                          │
┌─────────────────────────▼───────────────────────┐
│  Data Layer (Repository impl + DataSource)       │
│  - Local: Room DAO, DataStore                    │
│  - Remote: Retrofit (nếu có), WebView            │
│  - Browser: BrowserEngine, TabManager            │
└─────────────────────────────────────────────────┘
```

---

## 2. Multi-Module Structure

```
PrivateBrower/
├── app/                  # Main module — UI, ViewModel, Navigation, DI
│   └── src/main/java/com/asianmobile/privatebrower/
│       ├── BaseApplication.kt
│       ├── MainActivity.kt
│       ├── components/           # AppComponents.kt
│       ├── constant/              # Constant.kt
│       ├── data/
│       │   ├── browser/           # BrowserEngine, TabManager, VideoSniffer (NEW)
│       │   ├── database/          # Room DB, DAOs, Entities (NEW)
│       │   ├── local/             # DataStoreManager (extended)
│       │   ├── model/             # Bookmark, DownloadItem, DownloadStatus, HistoryItem, QuickAccessShortcut, SearchEngine, Tab
│       │   ├── repository/        # PreferencesRepository, TabRepository, SearchEngineRepository, BookmarkRepository, HistoryRepository, DownloadRepository
│       │   ├── usecase/           # ClearBrowsingDataUseCase
│       │   └── util/
│       ├── di/
│       ├── navigation/
│       │   ├── NavGraph.kt
│       │   └── NavExtensions.kt
│       ├── service/
│       │   └── DownloadForegroundService.kt   # (NEW)
│       ├── ui/
│       │   ├── splash/
│       │   ├── language/
│       │   ├── intro/
│       │   ├── permission/
│       │   ├── setdefault/       # (NEW)
│       │   ├── home/
│       │   │   ├── HomeScreen.kt           # container 4 tab
│       │   │   ├── HomeViewModel.kt
│       │   │   ├── browsertab/             # (NEW)
│       │   │   ├── tabstab/                # (NEW)
│       │   │   ├── filestab/               # (NEW)
│       │   │   ├── progresstab/            # (NEW)
│       │   │   └── settings/               # (extended)
│       │   ├── browser/          # BrowserWebViewScreen (extended)
│       │   ├── bookmarks/        # (NEW)
│       │   ├── howtodownload/    # (NEW)
│       │   ├── searchengine/     # picker bottom sheet (NEW)
│       │   ├── privacypolicy/    # (NEW)
│       │   ├── premium/
│       │   ├── component/        # reusable composables
│       │   ├── customview/
│       │   ├── main/             # MainViewModel
│       │   └── theme/
│       └── utils/
│
├── ads/                  # Ads module (đã có sẵn)
│   └── src/main/java/com/asianmobile/privatebrower/ads/
│       ├── config/
│       ├── customview/
│       ├── data/
│       ├── listener/
│       ├── tracking/
│       ├── ui/
│       │   ├── compose/          # BannerAdComposable, NativeAdComposable
│       │   ├── dialog/
│       │   ├── interstitial/     # InterstitialUtil, InterstitialLauncherUtil
│       │   ├── openads/          # AppOpenManager
│       │   └── rewarded/
│       └── utils/
│
├── docs/                 # ← documentation (this folder)
└── ...
```

**Dependency direction:**
- `:app` → `:ads` ✅
- `:ads` → `:app` ❌ (cấm — ads module độc lập)

---

## 3. State Pattern Chi Tiết

Mỗi screen có 3 file (rule bắt buộc):

### Ví dụ: `ui/bookmarks/`

**BookmarksUiState.kt**
```kotlin
data class BookmarksUiState(
    val isLoading: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val historyItems: List<HistoryItem> = emptyList(),
    val selectedTab: BookmarksTab = BookmarksTab.BOOKMARKS,
    val searchQuery: String = "",
    val errorMessage: String? = null,
)

enum class BookmarksTab { BOOKMARKS, HISTORY }
```

**BookmarksViewModel.kt**
```kotlin
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        observeBookmarks()
        observeHistory()
    }

    fun onTabSelected(tab: BookmarksTab) { _uiState.update { it.copy(selectedTab = tab) } }
    fun onSearchQueryChanged(q: String) { _uiState.update { it.copy(searchQuery = q) } }
    fun onDeleteBookmark(id: Long) { viewModelScope.launch { bookmarkRepository.delete(id) } }
    fun onClearHistory() { viewModelScope.launch { historyRepository.clearAll() } }

    private fun observeBookmarks() { /* collect Flow → update state */ }
    private fun observeHistory() { /* collect Flow → update state */ }
}
```

**BookmarksScreen.kt**
```kotlin
@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // ... compose UI dựa trên state, gọi viewModel.onXxx()
}
```

**Rule:** UiState **immutable**, ViewModel **không** expose `MutableStateFlow` ra ngoài.

---

## 4. Dependency Injection (Hilt)

Modules trong `app/di/`:

| Module | Component | Bind |
|--------|-----------|------|
| `AppModule` | `SingletonComponent` | Empty placeholder (chưa provide gì) |
| `BrowserModule` | `SingletonComponent` | `BrowserEngine`, `TabManager` |
| `DataModule` | `SingletonComponent` | `DataStoreManager`, `PrivateBrowserDatabase` (Room), từng DAO |
| `GsonModule` | `SingletonComponent` | `Gson` |
| `NetworkModule` | `SingletonComponent` | `OkHttpClient`, `Retrofit` |
| `RepositoryModule` | `SingletonComponent` | Bind 6 repository interface → impl: Preferences, Tab, SearchEngine, Bookmark, History, Download |

**Convention:**
- `@Singleton` cho repository, manager, database
- `@ViewModelScoped` cho usecases nếu cần per-VM lifecycle
- Tránh `@Provides` cho composable; chỉ bind layer dưới UI

---

## 5. Navigation

- 1 `NavHost` duy nhất trong `MainActivity`
- Tất cả route khai báo trong `Routes` object (xem [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md))
- Helper extension trong `navigation/NavExtensions.kt`:
  - `safeNavigate(route, ignoreDebounce, builder)` — debounce 500ms
  - `safePopBackStack(ignoreDebounce)`
  - `navigateWithAd(context, onNavigate)` — show interstitial trước
- Deep link cho `Intent.ACTION_VIEW` (khi app là default browser)

---

## 6. Folder Convention Chi Tiết

**Quy tắc đặt file:**

| Loại | Vị trí | Naming |
|------|--------|--------|
| Screen feature | `ui/<feature>/` | `<Feature>Screen.kt`, `<Feature>ViewModel.kt`, `<Feature>UiState.kt` |
| Reusable composable | `ui/component/` | `<Name>Composable.kt` hoặc `<Name>.kt` |
| Custom drawing | `ui/customview/` | `<Name>.kt` |
| Domain model | `data/model/` | Tên danh từ singular (`Bookmark`, `Tab`) |
| Repository interface | `data/repository/` | `<Name>Repository.kt` (interface trong cùng file impl, hoặc tách 2 file `Impl` suffix) |
| Repository impl | `data/repository/impl/` hoặc cùng file với interface | `<Name>RepositoryImpl.kt` |
| Room entity | `data/database/entity/` | `<Name>Entity.kt` |
| Room DAO | `data/database/dao/` | `<Name>Dao.kt` |
| Database class | `data/database/` | `PrivateBrowserDatabase.kt` |
| DataStore | `data/local/` | `DataStoreManager.kt` |
| Service | `service/` | `<Name>Service.kt` |
| DI module | `di/` | `<Name>Module.kt` |
| Util | `utils/` | `<Name>Util.kt` hoặc `<Name>Helper.kt` |

---

## 7. Dependency Direction Rules

1. ✅ `ui/` → `data/` qua interface
2. ❌ `data/` → `ui/` (cấm tuyệt đối)
3. ✅ `ui/<feature>/` → `ui/component/` (shared)
4. ❌ `ui/<feature>/` → `ui/<otherFeature>/` (cấm — share qua `ui/component/`)
5. ✅ `ui/` → `:ads` qua Composable từ ads module
6. ❌ `:ads` → `:app` (cấm)
7. ✅ ViewModel → Repository qua DI
8. ❌ Composable → Repository trực tiếp (cấm — phải qua ViewModel)

---

## 8. Threading & Coroutines

- **UI thread**: chỉ collect state, render. KHÔNG IO.
- **viewModelScope**: launch suspend từ user action
- **IO dispatcher** (`@IoDispatcher`): Room queries, file ops, network
- **Default dispatcher**: parsing JSON, decode bitmap

Quy ước:
```kotlin
suspend fun loadBookmarks(): List<Bookmark> = withContext(ioDispatcher) {
    dao.getAll().map { it.toDomain() }
}
```

---

## 9. Error Handling Convention

- Repository trả `Result<T>` hoặc throw — ưu tiên throw + catch ở ViewModel
- ViewModel catch → set `errorMessage` trong UiState
- UI render lỗi qua Snackbar hoặc inline message
- **Không** swallow exception âm thầm
- Network/IO timeout → log Crashlytics + show "Something went wrong"

---

## 10. Testing Strategy (định hướng v1)

V1 ưu tiên integration test thủ công, viết unit test cho:

- **Domain logic** — URL parsing, search engine URL builder, video URL detection regex
- **Repository** — Bookmark dedupe logic, history upsert
- **ViewModel** — State transitions (dùng `MainCoroutineRule`, `Turbine`)

Compose UI test: chỉ critical screen (Home, BrowserWebView), dùng `composeTestRule`.

---

## 11. Build Verification

- `./gradlew compileDebugKotlin` (~15s) — verify compile
- `./gradlew compileReleaseKotlin` (~30s) — verify ProGuard rules
- `./gradlew assembleDebug` — chỉ khi cần APK test trên device (4-5 phút)
- Tuyệt đối không tự ý chạy `assembleRelease` để check syntax

Xem [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md) — section build.
