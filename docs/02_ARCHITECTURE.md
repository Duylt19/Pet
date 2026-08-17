# 02 — Architecture Contract

## Tổng quan

```text
:app
  MainActivity + AppNavGraph
            ↓
  Feature Screen / ViewModel / UiState
            ↓
        Use case (optional)
            ↓
      Repository interface
            ↓
  Repository implementation / DataStore / platform API

:ads
  Ads SDK + remote config + ad UI/utilities
```

Dependency đi từ presentation xuống data boundary. Composable không truy cập storage/network/service trực tiếp.

## Cấu trúc hiện hành

```text
com.asianmobile.emojibattery.shimeji/
├── BaseApplication.kt
├── MainActivity.kt
├── constant/
├── data/
│   ├── local/                  # DataStoreManager
│   ├── model/                  # Domain/data models nhỏ
│   ├── remote/                 # Private GitHub raw client/config + integrity download
│   ├── repository/             # Interface
│   │   └── impl/               # DataStore + remote/cache owner catalog implementations
│   └── usecase/                # Nghiệp vụ tái sử dụng/testable
├── di/                         # Hilt modules đang có dependency thật
├── navigation/                 # Routes, AppNavGraph, HomeNavGraph, safe navigation/policy
├── pet/
│   ├── engine/                 # Kotlin thuần, không phụ thuộc Android framework
│   ├── overlay/                # Android service/window/input/render adapter
│   ├── pack/                   # Schema/parser/validator/installer/repository/cache
│   ├── settings/               # Pure policy cho budget/vị trí/session settings
│   └── speech/                 # Pure speech catalog + per-pet pose-gated sessions
├── battery/
│   ├── overlay/                # Accessibility window, renderer và platform capability
│   └── settings/               # Pure config sanitization policy
├── ui/
│   ├── app/                    # App-level presentation state
│   ├── onboarding/             # Splash, Language, Intro, Permission
│   ├── home/                   # Shell + entry screen của 4 top-level tab
│   │   ├── discover/
│   │   ├── battery/
│   │   ├── pet/
│   │   ├── mine/
│   │   └── shell/
│   ├── battery/                # Catalog flow dùng lại, category, collection, editor, troll
│   ├── pet/                    # Store flow dùng lại và My Pet Room
│   ├── permissions/            # Grant dashboard + Accessibility how-to dùng toàn app
│   ├── search/
│   ├── premium/
│   └── shared/                 # Component và theme dùng chung
└── utils/                      # Platform/helper cross-feature nhỏ
```

Xem [UI_STRUCTURE.md](UI_STRUCTURE.md) để tra route ↔ package và ownership của từng
flow. Entry screen phản ánh surface mà user đang thấy; logic/presentation flow được nhiều
surface dùng lại vẫn thuộc domain package, không bị kéo vào `ui/home` chỉ vì Home gọi nó.

## Feature template

Mỗi screen mới mặc định:

```text
ui/<domain>/<feature>/
├── FeatureScreen.kt
├── FeatureViewModel.kt
└── FeatureUiState.kt
```

- Screen collect `StateFlow` lifecycle-aware, render state, gửi callback/action.
- ViewModel chứa orchestration/business logic, dùng `viewModelScope`.
- UiState immutable và đủ biểu diễn loading/content/empty/error.
- Component tái sử dụng trong feature đặt cùng package; dùng
  `ui/shared/component` chỉ khi thật sự cross-feature.
- Test source và screenshot test phải mirror package của source để tìm feature nhanh và
  tiếp tục truy cập được các policy/composable `internal`.
- Bốn entry screen gắn trực tiếp với bottom navigation là ngoại lệ có chủ đích và nằm tại
  `ui/home/<tab>`. Catalog/store flow mà Discover/Search cũng sử dụng vẫn ở `ui/battery/catalog`
  và `ui/pet/store`; Home entry chỉ collect state, tracking và compose flow đó.

## Data boundary

- Interface repository cho phép thay data source và test ViewModel/use case.
- `OwnerPetCatalogRepository` giữ Pet Store/Search/Discover độc lập với network/cache. Production
  implementation đọc cache trước, revalidate private GitHub raw theo TTL 24 giờ + ETag,
  tôn trọng rate-limit retry deadline, cache JSON/metadata trong app-private storage, tải
  ZIP on-demand vào cache và chỉ chuyển sang installer sau khi size/SHA-256 khớp. Pack
  được chọn tiếp tục normalize/cài vào app-private storage.
- Coil chỉ gắn GitHub `Authorization` cho đúng host + repository path của Pet; token đọc
  bằng sensitive Remote Config key và không được log/commit.
- Implementation không leak entity/SDK object lên UI nếu model đó không thuộc UI contract.
- Use case không bắt buộc cho CRUD một dòng; dùng khi logic phối hợp nhiều nguồn, có policy hoặc cần reuse/test riêng.
- DataStore cho key-value nhỏ; Room chỉ thêm lại khi có requirement về dữ liệu quan hệ/offline.
- Service/WorkManager chỉ dùng khi công việc phải sống ngoài lifecycle UI.
- `BatteryCatalogRepository` đọc catalog chuẩn hóa từ app-specific external files, kiểm tra
  canonical path/size/SHA-256 và luôn có built-in fallback. Snapshot chưa duyệt chỉ load
  trong debug.
- `BatterySettingsRepository` persist config nhỏ bằng DataStore.

## Pet engine boundary

- Pet engine là Kotlin thuần: immutable `PetState` + `PetEvent` → `PetTransition`/`PetEffect`; không phụ thuộc `View`, `WindowManager` hoặc `Context`.
- `PetAnimationTimeline` tiêu thụ frame duration độc lập tick partition, cộng scripted velocity và chuyển non-loop clip sang action kế tiếp.
- `PetEngine` xử lý tap/drag/fling/bounds, giới hạn delayed tick để tránh catch-up storm và dùng constant deceleration cho fling.
- Android overlay adapter sở hữu `WindowManager.LayoutParams`, gesture input, render clock và foreground-service lifecycle.
- `PetOverlayService` là `specialUse` foreground service `exported=false`; `PetOverlayController` sở hữu một window/state machine/pack visual cho mỗi slot nhưng chỉ một `Choreographer` loop dùng chung.
- `PetOverlayView` chỉ vẽ/touch; mọi state transition vẫn đi qua `PetEngine`.
- `PetSpeechDirector` tiêu thụ effect/action transition bằng Kotlin thuần; Android adapter
  render tối đa một transient, non-touchable speech window cho mỗi pet và không tạo clock
  riêng.
- Asset pack được parse/validate thành model nội bộ trước khi engine sử dụng; renderer không đọc JSON/storage mỗi frame.
- Pack installer chỉ promote version hợp lệ từ random staging directory; repository luôn giữ built-in fallback.
- Bitmap cache decode asset trước khi render loop bắt đầu và có memory budget 4–24 MiB. Frame
  được decode đúng pixel nguồn bằng ARGB_8888, không density-scale; overlay và My Pet Room dùng
  bitmap filtering khi phải scale sprite lên kích thước hiển thị. Service resolve/decode toàn bộ
  sprite trên background dispatcher; main thread chỉ tạo và cập nhật overlay window.
- Session Mixed hỗ trợ 1–12 pet khác nhau trên mọi device budget; shared clock hạ từ
  30/24 FPS xuống 20 FPS khi có 4–6 pet và 16 FPS khi có 7–12 pet. Không thêm Room;
  `PetSlotPreferences` giữ selection/size/speed/touch/speech theo slot, còn last
  position/reset revision cũng được persist theo đúng slot trong DataStore.

## Battery accessibility overlay boundary

- `StatusBarAccessibilityService` chỉ sở hữu một `TYPE_ACCESSIBILITY_OVERLAY` full-width,
  non-touchable; không dùng window-content, gesture hoặc global-action API.
- Service combine repository Flow, decode/cache bitmap/GIF/Lottie ngoài main thread và
  render pin/time/date/network/airplane/ringer/hotspot. Network dùng callback, GIF/Lottie
  mới chạy frame khi user bật animation.
- Accessibility cover là opt-in sau disclosure. Service ẩn trên keyguard, screen-off và
  landscape; không có boot receiver — service còn trong enabled list thì hệ thống tự bind lại
  lúc boot.
- Cùng service đó tự `startForeground()` (`specialUse`) khi thanh pin đang bật, để force-stop
  không cướp mất quyền Accessibility. Điều kiện là config đã lưu chứ không phải window đang gắn.
- Danh sách exclusion do `BatterySettingsRepository.hiddenAppPackages` persist cục bộ.
  Accessibility event chỉ cung cấp foreground package để áp rule show/hide; service vẫn không
  retrieve window content, node tree, gesture hoặc global action.
- Battery Troll là một chế độ của chính service này, không phải overlay riêng. Khi
  `trollThemeId` khác `0`, emoji và pin lấy từ `BatteryTrollCatalogRepository` theo chỉ số
  mức pin thay vì từ battery catalog; mọi thành phần còn lại của thanh giữ nguyên. Quyết
  định chọn path nằm ở `BatteryTrollAssetPolicy` (Kotlin thuần), còn service chỉ
  materialize/decode. Theme troll đã chọn nhưng chưa có trong catalog — chưa tải, offline,
  hoặc bị gỡ trên server — phải rơi về theme battery thường chứ không được để thanh trống.
- Chế độ Fake chỉ đổi **chuỗi phần trăm**; `powerState.level` vẫn bị clamp 0–100 và vẫn
  điều khiển độ đầy icon. Tách hai thứ này là lý do 999% là trò đùa chứ không phải bug.
- Random artwork tự hẹn lại lần vẽ kế tiếp đúng mốc chu kỳ bằng một `Runnable` cố định.
  Không dùng `::render` cho `postDelayed` vì mỗi lần tham chiếu tạo một instance mới nên
  `removeCallbacks` sẽ không huỷ được, và callback còn treo sau khi overlay detach là rò pin.
- `COVER_SYSTEM_BAR` là lớp phủ best-effort theo OEM, không sửa SystemUI. Release vẫn cần
  Play policy/device-matrix gate.

## DI

- `@Binds` cho interface → implementation.
- `@Provides` cho object cần factory/configuration.
- Scope phải phản ánh lifetime (`@Singleton` chỉ khi thực sự app-wide).
- Không giữ module rỗng, binding cũ hoặc dependency không dùng.

## Navigation boundary

- `AppNavGraph` sở hữu root NavController cho onboarding và mọi màn độc lập. `home_graph` là một
  root destination opaque; bên trong `HomeRoute` có NavController thứ hai chỉ dành cho bốn tab
  `discover`, `battery_catalog`, `pet_store`, `settings`.
- `ui/home/shell` sở hữu bottom navigation và đúng một banner của Home. Chuyển tab dùng
  `popUpTo(discover) + saveState/restoreState + launchSingleTop`, nên không dispose/reload banner
  và vẫn giữ ViewModel/scroll của từng tab.
- Search, My Pet, category, editor, permission, premium và các màn còn lại là destination của
  root NavHost, không nằm trong Home NavHost. Destination có quảng cáo đáy tự sở hữu ad slot trong
  composition của chính nó; mở một entry mới sẽ có ViewModel/ad lifecycle mới.
- Root NavHost không dùng cross-fade. Mỗi destination có nền opaque để Navigation không giữ hai
  layout nhìn thấy cùng lúc hoặc đổi chiều cao màn cũ khi ad của màn mới được mount.
- Feature Screen nhận callback như `onBack`, `onOpenSettings`; không nhận NavController nếu không có lý do đặc biệt.
- `ui/home/{discover,battery,pet,mine}` là bốn entry ngang hàng. Permission dashboard không thuộc
  Mine dù Mine có link tới nó; package owner là `ui/permissions` vì Discover, editor và pet flow
  cũng có thể mở cùng destination.
- Back-stack behavior là một phần contract và phải được document/test.

## Cách mở rộng ứng dụng

1. Chốt requirement/domain boundary.
2. Tạo feature UI contract.
3. Thêm model/repository/use case tối thiểu cần thiết.
4. Wiring Hilt và navigation.
5. Thêm resources/analytics/tests.
6. Cập nhật docs cùng commit.
