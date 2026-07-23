---
name: android-developer
description: Quy tắc kiến trúc, coding, UI và verification cho Cute Pet Android base project.
---

# Android Developer Skill — Cute Pet Base

Agent làm việc trong repository này PHẢI tuân thủ các contract dưới đây. Mục tiêu là dùng base sạch để phát triển nhiều sản phẩm mới mà vẫn giữ cấu trúc và phương thức đã ổn định từ project trước.

## 0. Trạng thái project

- Display name: `Cute Pet`.
- Namespace/application ID tạm thời: `com.asianmobile.privatebrower`.
- Không tự đổi package, namespace, root project name hoặc provider authority nếu owner chưa yêu cầu.
- Flow hiện tại: Splash → Language → Intro → Permission → Home.
- Home điều khiển Start/Stop session 1–3 pet khác nhau và có action Catalog/Settings/Premium.
- Browser, search/clear-browsing, broad storage access, tab manager, bookmark/history, download, media/file manager, Room và foreground service cũ đã bị xóa.
- Permission xử lý overlay special access + notification permission và cho phép Skip.
- Pet overlay mới nằm trong `pet/overlay`: một `specialUse` foreground service, 1–3 small `TYPE_APPLICATION_OVERLAY` window có pack riêng theo slot và notification bắt buộc.
- Không viết code/docs dựa trên giả định các module đã xóa vẫn tồn tại.

## 1. Giao tiếp và làm rõ yêu cầu

- Tất cả trao đổi với owner bằng Tiếng Việt.
- Code, identifier và commit message bằng English.
- Dịch nội dung tự nhiên theo ngữ cảnh, không dịch từng từ máy móc.
- Nếu UI, flow, logic hoặc architecture chưa rõ và lựa chọn có thể đổi kết quả đáng kể, phải hỏi owner trước khi thực hiện.
- Không tự mở rộng phạm vi hoặc phục hồi feature cũ.

## 2. Tooling và verification

- Tìm file/text bằng `rg`/`rg --files`; đọc bằng `sed`/`nl`.
- Sửa file thủ công bằng `apply_patch`.
- Kiểm tra compile bằng `./gradlew compileDebugKotlin`.
- Chạy unit test bằng `./gradlew testDebugUnitTest`.
- Không chạy `assembleDebug`/`assembleRelease` chỉ để kiểm tra compile. Chỉ assemble khi cần APK hoặc owner yêu cầu.
- Sau khi hoàn tất, chạy `git diff --check`, tự review diff và tạo commit.
- Commit message English, rõ nghĩa; dùng các prefix như `Handle`, `Fix`, `Update`, `Refactor`, `Remove`.

## 3. Architecture contract

### 3.1. Module

- `:app`: application shell, feature UI, domain/data integration.
- `:ads`: ads SDK, remote config và các ad composable/utilities.
- Không đưa business feature của app vào `:ads`.
- Chỉ tạo module mới khi có ranh giới build/reuse rõ ràng; không tách module chỉ vì feature lớn.

### 3.2. Layer và dependency direction

```text
Composable UI
    ↓ state/events
ViewModel
    ↓
UseCase (khi nghiệp vụ đủ phức tạp hoặc dùng lại)
    ↓
Repository interface
    ↓
Repository implementation / local / network / platform API
```

- UI không truy cập DataStore, database, network hoặc service trực tiếp.
- ViewModel không giữ `Activity`, `View`, `NavController` hoặc Composable state.
- Repository interface đặt ở `data/repository/`; implementation đặt ở `data/repository/impl/` theo convention hiện tại.
- Use case đặt ở `data/usecase/` trong base hiện tại. Nếu sau này tách domain layer, phải thực hiện nhất quán và cập nhật docs.
- Platform API cần `Context` phải dùng `@ApplicationContext` nếu lifetime vượt screen.

### 3.3. Package-by-feature cho UI

Mỗi screen mới mặc định có ba file:

```text
ui/newfeature/
├── NewFeatureScreen.kt
├── NewFeatureViewModel.kt
└── NewFeatureUiState.kt
```

```kotlin
data class NewFeatureUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NewFeatureViewModel @Inject constructor(
    private val repository: NewFeatureRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewFeatureUiState())
    val uiState: StateFlow<NewFeatureUiState> = _uiState.asStateFlow()
}
```

- `UiState` là immutable data class.
- Expose `StateFlow`, không expose `MutableStateFlow`.
- Collect trong Compose bằng `collectAsStateWithLifecycle()`.
- User action gọi method ViewModel hoặc callback rõ nghĩa (`onRetry`, `onSave`, `onBack`).
- One-off event dùng event stream/channel phù hợp; không nhét event đã consume vào state mà không có cơ chế reset rõ ràng.
- Composable cấp screen nhận navigation callback; không truyền `NavController` sâu xuống component.

### 3.4. Data và dependency injection

- Preference nhỏ, onboarding flag và lựa chọn user dùng DataStore.
- Dữ liệu quan hệ/offline lớn chỉ thêm Room khi có nhu cầu sản phẩm thật; kèm entity, DAO, database, schema policy và test.
- Network chỉ thêm client/API/repository khi cần; timeout, error mapping và security phải được thiết kế rõ.
- Long-running task chỉ thêm Android Service/WorkManager khi lifecycle yêu cầu; phải cập nhật manifest, permission, notification behavior, DI, test và docs.
- Interface → implementation bind bằng Hilt `@Binds`; object cần xây dựng dùng `@Provides`.
- Không tạo DI module rỗng hoặc giữ dependency không sử dụng.

## 4. Navigation contract

- Route constant nằm trong `Routes` ở `navigation/NavGraph.kt`.
- Destination và transition flow nằm trong `AppNavGraph`.
- Dùng `safeNavigate()`/`safePopBackStack()` để tránh double click.
- Dùng `navigateWithAd()` khi destination cần interstitial theo policy.
- Onboarding phải `popUpTo(..., inclusive = true)` để Back không quay lại bước đã hoàn thành.
- Argument phải typed bằng `navArgument`; builder route phải encode dữ liệu chuỗi/URL.
- Khi thêm/xóa route phải cập nhật:
  - `docs/04_NAVIGATION_FLOW.md`
  - `docs/screens/README.md`
  - `ScreenName` analytics nếu đó là screen nhìn thấy
  - navigation/unit tests liên quan.

## 5. UI và resource contract

### 5.1. String

- Không hardcode user-facing text trong Composable.
- Khai báo trong `res/values/strings.xml`, key `snake_case` theo dạng `<feature>_<purpose>`.
- Dùng `stringResource()`; nội dung có tham số dùng placeholder resource.

### 5.2. Color

- Không hardcode hex hoặc dùng màu tùy tiện trong feature UI.
- Khai báo trong `colors.xml`, convention `colors_<HEX>`; semantic theme color là ngoại lệ có chủ đích.
- Dùng `colorResource()` hoặc token từ Material theme nếu token đó đã được định nghĩa chính thức.

### 5.3. Spacing và typography

- Project dùng Intuit SDP/SSP cho UI theo design hiện tại.
- Khi implement Figma: giá trị Figma px chia `1.3`, làm tròn về resource sdp/ssp gần nhất.
- Dùng `dimensionResource(com.intuit.sdp.R.dimen._Xsdp)` và `dimensionResource(com.intuit.ssp.R.dimen._Xssp)`.
- Font phải lấy từ `res/font`/theme; không tạo `FontFamily` trùng lặp nếu có thể tái sử dụng.

### 5.4. Composable

- Screen chịu trách nhiệm state collection và orchestration; component nhỏ ưu tiên stateless.
- Side effect dùng `LaunchedEffect`, `DisposableEffect` hoặc ViewModel; không chạy side effect trong body tùy tiện.
- Modifier interactive chuẩn:

```text
size → shadow → clip → background → border → clickable → padding
```

- `clip(shape)` phải đứng trước `clickable()` nếu ripple cần theo shape.
- Major screen/component nên có Preview khi dependency cho phép.
- Thêm `contentDescription` cho action icon; decorative image dùng `null`.

## 6. Figma workflow

- Phải dùng screenshot/design context từ Figma trước khi code UI; không đoán layout từ tên node.
- So sánh hierarchy, alignment, spacing, size, color, typography, corner radius và layer order.
- File key/node ID lấy từ URL owner cung cấp, không dùng key legacy mặc định.
- Token chỉ đọc từ biến môi trường `FIGMA_ACCESS_TOKEN`; tuyệt đối không ghi credential vào repository hoặc log.
- Export icon từ frame container, không export riêng path con làm mất viewBox/padding.
- Icon đơn sắc: `ic_<name>.xml`; logo/vector nhiều màu: `ic_logo_<name>.xml`; bitmap: `img_<name>.webp/png`.
- Không đặt `.svg` trực tiếp trong Android `res/`.
- Chỉ dùng `Box` bọc icon khi design có background shape thật; không bọc chỉ để giả padding đã có trong frame.

## 7. Ads, billing và app shell

- Tái sử dụng API trong module `:ads`; không gọi SDK rải rác ở feature.
- Interstitial phải có callback tiếp tục flow khi ad đóng hoặc load fail.
- Tắt App Open Ads khi đang hiển thị full-screen ad hoặc thực hiện navigation nhạy cảm theo pattern hiện có.
- Premium entry phải truyền `StartPremiumIndexes` phù hợp để close behavior đúng với onboarding/in-app/splash return.
- Billing state không đặt trong Composable; dùng ViewModel/infrastructure hiện có.
- Ads placement mới phải có screen code/policy rõ và cập nhật `docs/07_ADS_INTEGRATION.md`.

## 8. Permission và platform policy

- Màn Permission không request storage; overlay dùng `ACTION_MANAGE_OVERLAY_PERMISSION`, còn notification dùng runtime permission trên API 33+.
- Không thêm permission vì “có thể cần sau”. Chỉ khai báo/request khi feature hiện tại cần và có UX giải thích.
- Runtime permission phải xét API level, denial/rationale/permanent denial và đường dẫn App Settings.
- Special access không được request như runtime permission.
- Khi thêm service, notification, camera, microphone, location hoặc storage capability, cập nhật manifest, UI states, test matrix và docs trong cùng change.

## 9. Analytics và error handling

- Screen nhìn thấy phải dùng `TrackScreenView(ScreenName.X)`.
- `ScreenName.value` lowercase snake_case, ổn định và unique.
- Không log PII, token, URL nhạy cảm hoặc nội dung user.
- Lỗi data/domain được map sang state hoặc one-off message; không để exception platform thoát lên Composable.
- Không dùng `runCatching` để nuốt lỗi quan trọng mà không log/biểu diễn trạng thái.

## 10. Test contract

- Pure policy, mapper, state transition và use case phải có unit test khi có nhánh logic đáng kể.
- Repository implementation dùng fake/mock boundary phù hợp.
- UI/navigation critical flow thêm Compose/instrumentation test khi cần; không thay thế toàn bộ bằng snapshot thủ công.
- Khi xóa feature, xóa test chỉ phục vụ feature đó và giữ test cho contract còn tồn tại.
- Tối thiểu trước commit: compile debug Kotlin + debug unit tests + `git diff --check`.

## 11. Documentation contract

Markdown là một phần của implementation, không phải ghi chú tùy chọn.

Khi thay đổi các nội dung sau, PHẢI cập nhật docs trong cùng commit:

| Thay đổi | Tài liệu tối thiểu |
|---|---|
| App identity/package | `README.md`, `01_PROJECT_OVERVIEW.md`, agent rules |
| Source tree/layer/module | `02_ARCHITECTURE.md`, `README.md` |
| Dependency/SDK | `03_TECH_STACK.md` |
| Route/onboarding/back behavior | `04_NAVIGATION_FLOW.md`, `screens/README.md` |
| DataStore/database/model | `05_DATA_MODEL.md` |
| UI token/component convention | `06_UI_DESIGN_SYSTEM.md` |
| Ads placement/policy | `07_ADS_INTEGRATION.md` |
| Agent convention | file skill này và `08_AGENT_CODING_GUIDELINES.md` |
| Milestone/status | `09_IMPLEMENTATION_ROADMAP.md`, `IMPLEMENTATION_PROGRESS.md` |
| Screen analytics | `10_SCREEN_TRACKING.md` |

- Xóa doc mô tả file/feature đã xóa; không để “historical spec” lẫn với current source of truth.
- Nếu cần lưu lịch sử thiết kế, đặt rõ dưới `docs/archive/` và ghi `ARCHIVED — NOT CURRENT` ở đầu file.
- Link Markdown phải hợp lệ và tương đối trong repository.

## 12. Definition of done

- Requirement đã được đáp ứng, không còn code/dependency/resource hiển nhiên không dùng trong phạm vi thay đổi.
- Architecture contract được giữ hoặc thay đổi đã được owner xác nhận.
- Source, tests và docs đồng bộ.
- Compile/test pass.
- Không có secret trong working tree hoặc commit history sẽ push.
- Commit đã được tạo với message rõ nghĩa.
