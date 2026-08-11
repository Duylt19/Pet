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
├── navigation/                 # Routes, NavGraph, safe navigation
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
│   ├── home/                   # Discover
│   ├── battery/                # Catalog, Favourite/Recent, Editor
│   ├── pet/                    # Pet Store và My Pet Room
│   ├── settings/               # Mine và permission management
│   ├── search/
│   ├── premium/
│   └── shared/                 # Component và theme dùng chung
└── utils/                      # Platform/helper cross-feature nhỏ
```

Xem [UI_STRUCTURE.md](UI_STRUCTURE.md) để tra route ↔ package và ownership của từng
flow. Cây package presentation phải phản ánh domain sản phẩm, không phản ánh thứ tự
lịch sử mà file được tạo.

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
- Bitmap cache decode asset trước khi render loop bắt đầu và có memory budget 4–24 MiB.
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
  landscape; không có boot receiver.
- `COVER_SYSTEM_BAR` là lớp phủ best-effort theo OEM, không sửa SystemUI. Release vẫn cần
  Play policy/device-matrix gate.

## DI

- `@Binds` cho interface → implementation.
- `@Provides` cho object cần factory/configuration.
- Scope phải phản ánh lifetime (`@Singleton` chỉ khi thực sự app-wide).
- Không giữ module rỗng, binding cũ hoặc dependency không dùng.

## Navigation boundary

- `AppNavGraph` sở hữu NavController và route wiring.
- Bốn top-level route `home`, `battery_catalog`, `pet_store`, `settings` nằm trong một
  Home shell dùng chung. Shell sở hữu bottom navigation và banner; feature screen chỉ sở
  hữu content để đổi tab không dispose/reload quảng cáo.
- Chuyển Home tab dùng `popUpTo(home) + saveState/restoreState + launchSingleTop` để giữ
  ViewModel, scroll và state của từng route.
- Feature Screen nhận callback như `onBack`, `onOpenSettings`; không nhận NavController nếu không có lý do đặc biệt.
- Back-stack behavior là một phần contract và phải được document/test.

## Cách mở rộng ứng dụng

1. Chốt requirement/domain boundary.
2. Tạo feature UI contract.
3. Thêm model/repository/use case tối thiểu cần thiết.
4. Wiring Hilt và navigation.
5. Thêm resources/analytics/tests.
6. Cập nhật docs cùng commit.
