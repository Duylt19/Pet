# 08 — Agent Coding Guidelines

Quy tắc **BẮT BUỘC** khi viết code cho project. AI Agent và developer **phải** tuân thủ 100%. Mọi PR vi phạm sẽ bị reject.

> Tài liệu này tổng hợp + mở rộng từ [`.agents/skills/android_developer/SKILL.md`](../.agents/skills/android_developer/SKILL.md). Khi có mâu thuẫn → SKILL.md ưu tiên.

---

## 1. Communication

- **Giao tiếp với user**: tiếng Việt. Tự nhiên, sát nghĩa, không word-by-word.
- **Code, biến, function, commit message**: tiếng Anh chuẩn.
- **Comments trong code**: tiếng Anh, ngắn gọn (xem rule 13).
- **Khi không chắc về yêu cầu UI/logic**: DỪNG, hỏi user trước. Không đoán mò.

---

## 2. Build Verification

| Command | Thời gian | Khi nào dùng |
|---------|-----------|--------------|
| `./gradlew compileDebugKotlin` | ~15-20s | **Default** — verify code compile được |
| `./gradlew compileReleaseKotlin` | ~30-60s | Verify ProGuard rules ok |
| `./gradlew assembleDebug` | 4-5 phút | Chỉ khi cần APK test trên device |
| `./gradlew assembleRelease` | 5-7 phút | Chỉ khi build release thật |

**❌ TUYỆT ĐỐI KHÔNG** dùng `assembleDebug/Release` để check compile lỗi nhanh — quá chậm.

---

## 3. String Resources (CRITICAL)

- ❌ Không hardcode chuỗi: `Text("Hello")` → SAI
- ✅ Dùng `stringResource(R.string.xxx)`: `Text(stringResource(R.string.home_search_placeholder_text))` → ĐÚNG
- Mọi string mới phải thêm vào `res/values/strings.xml` trước khi dùng
- Naming: `snake_case`, format `<screen>_<purpose>_text` hoặc `<screen>_<purpose>_label`
- Ví dụ:
  ```xml
  <string name="home_search_placeholder_text">Search or type URL</string>
  <string name="setdefault_button_label">Set as default</string>
  <string name="settings_clear_history_title">Clear History</string>
  ```

**Exception:** ad unit IDs (`translatable="false"`) và technical strings không user-facing — vẫn để strings.xml.

---

## 4. Color Resources (CRITICAL)

- ❌ Không hardcode hex: `Color(0xFF7C5BFB)` hoặc `Color.Black` cho UI element → SAI
- ✅ Dùng `colorResource(R.color.xxx)` → ĐÚNG
- Naming: `colors_<HEX>` (vd `colors_7C5BFB`). Trừ primary có thể đặt `colorPrimary`, `colorSecondary`.

```xml
<color name="colors_7C5BFB">#7C5BFB</color>
<color name="colors_F2F2F7">#F2F2F7</color>
<color name="colorPrimary">#7C5BFB</color>  <!-- exception -->
```

```kotlin
Text(color = colorResource(R.color.colors_7C5BFB))
```

**Exception cho gradient:** brush phải tạo ở code Compose, nhưng từng màu trong brush vẫn từ `colorResource`.

---

## 5. Dimension Rule (sdp/ssp)

Mọi dp/sp lấy từ Figma → **chia 1.3** → quy về sdp/ssp gần nhất:

| Figma (dp) | sdp |
|------------|-----|
| 4 | `_3sdp` |
| 8 | `_6sdp` |
| 12 | `_9sdp` |
| 16 | `_12sdp` |
| 20 | `_16sdp` |
| 24 | `_18sdp` |
| 32 | `_24sdp` |
| 48 | `_36sdp` |

```kotlin
import com.intuit.sdp.R as SdpR

Modifier.padding(dimensionResource(SdpR.dimen._12sdp))
```

Cho font: dùng `_<n>ssp`. Xem [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md).

**Tránh dùng `12.dp`, `14.sp` trực tiếp.**

---

## 6. Screen File Pattern (3-File Rule)

Mỗi screen có chính xác **3 file**:

```
ui/<feature>/
├── <Feature>Screen.kt        # Composable UI
├── <Feature>ViewModel.kt     # @HiltViewModel + business logic
└── <Feature>UiState.kt       # data class immutable
```

Ví dụ:
```
ui/bookmarks/
├── BookmarksScreen.kt
├── BookmarksViewModel.kt
└── BookmarksUiState.kt
```

**Nếu screen có sub-components phức tạp** (vd Tabs tab có TabCard, TabActionsRow):
- Đặt private composables trong cùng `<Feature>Screen.kt`
- Nếu reusable cho screen khác → move sang `ui/component/`

---

## 7. ViewModel Pattern

```kotlin
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init { observeData() }

    // Public action methods — UI calls these
    fun onTabSelected(tab: BookmarksTab) { _uiState.update { it.copy(selectedTab = tab) } }
    fun onDeleteBookmark(id: Long) { viewModelScope.launch { bookmarkRepository.delete(id) } }

    // Private observers
    private fun observeData() { /* ... */ }
}
```

**Bắt buộc:**
- `@HiltViewModel` + `@Inject constructor()`
- Expose `StateFlow<UiState>`, **không** expose `MutableStateFlow`
- Public method là action (verb-style: `onClickX`, `loadY`, `submit`)
- Coroutine launch trong `viewModelScope`

---

## 8. Modifier Order (CRITICAL — Ripple bugs)

Thứ tự chuẩn:
```kotlin
Modifier
    .size(...)         // 1. kích thước
    .shadow(...)       // 2. shadow (nếu có)
    .clip(shape)       // 3. clip — TRƯỚC clickable
    .background(...)   // 4. nền
    .border(...)       // 5. viền
    .clickable(...)    // 6. click (ripple bị clip theo shape)
    .padding(...)      // 7. padding nội dung
```

**Hậu quả nếu sai thứ tự:** ripple hình vuông thay vì shape, click area lệch, shadow biến mất.

---

## 9. Clickable Icon Pattern

❌ **KHÔNG dùng `IconButton`** khi cần kích thước chính xác (min touch target 48dp khiến UI phình to).

```kotlin
// ✅ Icon có background
Box(
    modifier = Modifier
        .size(dimensionResource(SdpR.dimen._19sdp))    // 26dp Figma
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.85f))
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    Icon(
        painter = painterResource(R.drawable.ic_heart),
        contentDescription = stringResource(R.string.favorite),
        tint = colorResource(R.color.colors_808080),
        modifier = Modifier.size(dimensionResource(SdpR.dimen._10sdp))
    )
}

// ✅ Icon không background, standalone clickable
Icon(
    painter = painterResource(R.drawable.ic_profile),
    contentDescription = stringResource(R.string.profile),
    tint = Color.White,
    modifier = Modifier
        .size(dimensionResource(SdpR.dimen._21sdp))     // 28dp Figma
        .clip(CircleShape)                              // BEFORE clickable
        .clickable(onClick = onClick)
)
```

---

## 10. Navigation

- Dùng `safeNavigate(route)` thay `navigate(route)` — có debounce
- Dùng `safePopBackStack()` thay `popBackStack()` — check previousBackStackEntry
- `navigateWithAd(context, onNavigate)` cho các điểm có interstitial (xem [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md))
- `popUpTo(X) { inclusive = true }` khi cần pop back stack (vd onboarding done → Home)
- Route constants trong `Routes` object — không hardcode string ở composable

---

## 11. Ads Rules

1. Khi show Interstitial: `InterstitialUtil.getInstance().openAd?.needShowOpenAds = false`
2. Premium user: ẩn tất cả ads
3. Native fail → composable hide container (height = 0)
4. Không show ad sau hành động phá huỷ (delete/clear)
5. Không show OpenAd khi đang download

Chi tiết: [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md).

---

## 12. WebView Security

```kotlin
webView.settings.apply {
    javaScriptEnabled = true                // BẮT BUỘC cho web hiện đại
    domStorageEnabled = true
    useWideViewPort = true
    loadWithOverviewMode = true

    // Security hardening
    allowFileAccess = false                  // Tránh access file://
    allowContentAccess = false               // Tránh content://
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false

    // UX
    builtInZoomControls = true
    displayZoomControls = false
    setSupportMultipleWindows(true)

    userAgentString = mobileUA               // Default mobile
}

// Cookies
CookieManager.getInstance().apply {
    setAcceptCookie(!isIncognito)
    setAcceptThirdPartyCookies(webView, !isIncognito)
}

// Safe Browsing (AndroidX WebKit)
if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
    WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, true)
}
```

---

## 13. Comments

**Default: KHÔNG viết comment.**

Chỉ viết khi giải thích WHY mà code không tự nói được:
- Workaround cho bug cụ thể (kèm link issue)
- Constraint ẩn (vd "OEM Samsung khoá API X, fallback bằng Y")
- Magic number không obvious

Không viết:
- `// Set the title` (vô nghĩa, code đã rõ)
- `// TODO: implement` không kèm context
- Tham chiếu task PR ("added for ticket XYZ")
- KDoc cho mọi function (chỉ public API thực sự cần doc)

---

## 14. Figma → Code Workflow (Mandatory Analysis)

Khi nhận yêu cầu UI từ Figma hoặc screenshot:

1. **Làm rõ yêu cầu** — nếu mơ hồ, hỏi user
2. **So sánh hình ảnh thực tế và Figma** — list cụ thể điểm khác
3. **Phân tích code hiện tại** — Read file, đối chiếu thông số (dp/color/text)
4. **Lập kế hoạch** — trình bày plan rõ trước khi sửa

Xem chi tiết [SKILL.md mục 8](../.agents/skills/android_developer/SKILL.md) và doc screen tương ứng trong `screens/`.

**Layer pattern (icon + background):**
- ❌ Không export gộp icon + bg thành 1 file drawable
- ✅ Chỉ export path icon thành VectorDrawable; background dùng Compose (`Box` + `Shape` + `background()`)

---

## 15. Commit Message Convention

Format chuẩn:
```
<Verb> <category> <feature/screen name>
```

Verbs hợp lệ:
- `Handle feature <X>` — hoàn thành tính năng
- `Handle UI <X>` — hoàn thiện giao diện
- `Fix bug crash feature <X>` — sửa lỗi crash
- `Fix bug UI <X>` — sửa lỗi UI
- `Update <X>` — cải thiện
- `Refactor <X>` — refactor không đổi behavior
- `Remove <X>` — xoá

Examples:
- ✅ `Handle feature Browser Core WebView config`
- ✅ `Handle UI Settings screen`
- ✅ `Fix bug crash feature Download M3U8 parser`
- ❌ `update stuff` (vague)
- ❌ `fix` (no context)

---

## 16. KHÔNG Đoán Mò (Tính Khách Quan)

- Khi gặp UI / logic / architecture chưa rõ → **DỪNG** + hỏi user
- Khi gặp ambiguous code → đọc thêm file liên quan, đối chiếu doc, không tự suy diễn
- Không tự ý quyết định đặt tên, kích thước, behavior khi chưa có nguồn (Figma / spec / user instruction)
- **Khi đã thực hiện thay đổi: kiểm tra lại đúng thiết kế trước khi commit**

---

## 17. Edge-To-Edge Layout

`MainActivity` đã `enableEdgeToEdge()`. Mỗi screen tự handle window insets:

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.systemBars,
    topBar = { ... },
    bottomBar = { ... },
) { padding ->
    Column(modifier = Modifier.padding(padding)) { ... }
}
```

Hoặc dùng `Modifier.systemBarsPadding()` cho fullscreen layout.

---

## 18. Quy Trình Khi Implement Feature Mới

Khi nhận task implement feature mới hoặc screen mới:

1. **Đọc doc liên quan**: `screens/Sxx_*.md` + `features/Fyy_*.md`
2. **Đọc `08_AGENT_CODING_GUIDELINES.md` (file này)** — refresh quy tắc
3. **Đọc code base liên quan** — files trong package tương ứng
4. **Tạo 3 file** Screen/ViewModel/UiState
5. **Thêm route** trong `Routes` + `NavGraph`
6. **Thêm strings/colors/drawables** mới (nếu cần)
7. **Compile check**: `./gradlew compileDebugKotlin`
8. **Test build APK**: `./gradlew assembleDebug` (chỉ khi cần test device)
9. **Commit** theo pattern mục 15

---

## 19. Checklist Trước Commit

- [ ] Không hardcode string, color, dp/sp
- [ ] 3-file pattern Screen/ViewModel/UiState
- [ ] `@HiltViewModel` + `@Inject constructor`
- [ ] Modifier order: clip TRƯỚC clickable
- [ ] Touch target ≥ 48dp (hoặc hợp lý cho screen)
- [ ] `contentDescription` cho icon clickable
- [ ] Route chạy đúng (back stack ok)
- [ ] Ad đúng matrix
- [ ] Compile pass
- [ ] Commit message đúng format
- [ ] Không thêm file không liên quan
- [ ] Không tự ý xoá tính năng khác

---

## 20. Anti-Patterns Cần Tránh

| Anti-pattern | Đúng |
|--------------|------|
| `Text("OK")` | `Text(stringResource(R.string.common_ok_label))` |
| `Color(0xFF000000)` | `colorResource(R.color.colors_000000)` |
| `Modifier.padding(16.dp)` | `Modifier.padding(dimensionResource(SdpR.dimen._12sdp))` |
| `IconButton` cho icon nhỏ chính xác | `Icon` + `Modifier.size().clip().clickable()` |
| `ViewModel(context = LocalContext.current)` | `hiltViewModel()` |
| Composable gọi Repository trực tiếp | Composable → ViewModel → Repository |
| Mutate trực tiếp `_uiState.value.list.add(x)` | `_uiState.update { it.copy(list = it.list + x) }` |
| `runBlocking { }` trong UI | `viewModelScope.launch { }` |
| `try {} catch (e: Exception) {}` swallow | log + set error state |
| Comment "// Set title" | xóa, code tự nói |
