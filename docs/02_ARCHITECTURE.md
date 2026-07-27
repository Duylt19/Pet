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
│   ├── repository/             # Interface
│   │   └── impl/               # DataStore + local owner catalog implementations
│   └── usecase/                # Nghiệp vụ tái sử dụng/testable
├── di/                         # Hilt modules đang có dependency thật
├── navigation/                 # Routes, NavGraph, safe navigation
├── pet/
│   ├── engine/                 # Kotlin thuần, không phụ thuộc Android framework
│   ├── overlay/                # Android service/window/input/render adapter
│   ├── pack/                   # Schema/parser/validator/installer/repository/cache
│   ├── settings/               # Pure policy cho budget/vị trí/session settings
│   └── speech/                 # Pure speech catalog + per-pet pose-gated sessions
├── ui/
│   ├── component/              # Shared stateless UI
│   ├── splash/
│   ├── language/
│   ├── intro/
│   ├── permission/
│   ├── home/                   # Home + settings
│   ├── catalog/                # Pack catalog/detail/import/select
│   ├── premium/
│   ├── main/
│   └── theme/
└── utils/                      # Platform/helper cross-feature nhỏ
```

## Feature template

Mỗi screen mới mặc định:

```text
ui/feature/
├── FeatureScreen.kt
├── FeatureViewModel.kt
└── FeatureUiState.kt
```

- Screen collect `StateFlow` lifecycle-aware, render state, gửi callback/action.
- ViewModel chứa orchestration/business logic, dùng `viewModelScope`.
- UiState immutable và đủ biểu diễn loading/content/empty/error.
- Component tái sử dụng trong feature đặt cùng package; dùng `ui/component` chỉ khi thật sự cross-feature.

## Data boundary

- Interface repository cho phép thay data source và test ViewModel/use case.
- `OwnerPetCatalogRepository` giữ Catalog UI độc lập với local test source hiện tại và backend owner-controlled sau này; local binary nằm trong app-specific external storage, còn pack được chọn được normalize/cài vào app-private storage.
- Implementation không leak entity/SDK object lên UI nếu model đó không thuộc UI contract.
- Use case không bắt buộc cho CRUD một dòng; dùng khi logic phối hợp nhiều nguồn, có policy hoặc cần reuse/test riêng.
- DataStore cho key-value nhỏ; Room chỉ thêm lại khi có requirement về dữ liệu quan hệ/offline.
- Service/WorkManager chỉ dùng khi công việc phải sống ngoài lifecycle UI.

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
- Session hỗ trợ 1–3 pet khác nhau trên thiết bị thường, 1–2 pet trên low-RAM device; 3 pet hạ shared clock xuống 24 FPS. Không thêm Room; `PetSlotPreferences` giữ selection/size/speed/touch/speech theo slot, còn last position/reset revision cũng được persist theo đúng slot trong DataStore.

## DI

- `@Binds` cho interface → implementation.
- `@Provides` cho object cần factory/configuration.
- Scope phải phản ánh lifetime (`@Singleton` chỉ khi thực sự app-wide).
- Không giữ module rỗng, binding cũ hoặc dependency không dùng.

## Navigation boundary

- `AppNavGraph` sở hữu NavController và route wiring.
- Feature Screen nhận callback như `onBack`, `onOpenSettings`; không nhận NavController nếu không có lý do đặc biệt.
- Back-stack behavior là một phần contract và phải được document/test.

## Cách mở rộng base

1. Chốt requirement/domain boundary.
2. Tạo feature UI contract.
3. Thêm model/repository/use case tối thiểu cần thiết.
4. Wiring Hilt và navigation.
5. Thêm resources/analytics/tests.
6. Cập nhật docs cùng commit.
