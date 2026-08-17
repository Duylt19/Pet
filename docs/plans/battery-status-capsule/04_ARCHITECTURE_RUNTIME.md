# 04 — Architecture and Runtime

> **REFERENCE TARGET — current vertical slice chỉ triển khai Accessibility backend**

## Target dependency flow

```text
Compose screen
  ↓ state/events
BatteryCatalogViewModel / StatusCapsuleEditorViewModel
  ↓
StatusCapsuleSettingsRepository / StatusThemeCatalogRepository
  ↓
DataStore / app-private catalog cache / owner-controlled remote source

Applied StatusCapsuleConfig
  ↓ StateFlow
StatusCapsuleRuntimeCoordinator
  ├─ BelowSystemBarBackend → OverlayHostService
  │    ├─ PetOverlayController
  │    └─ ApplicationOverlayCapsuleController
  └─ CoverSystemBarBackend → StatusBarAccessibilityService
       └─ AccessibilityOverlayCapsuleController

Shared by both capsule backends
  ├─ DeviceStatusRepository
  ├─ StatusCapsuleRenderer/View
  └─ OverlayFrameClock (only when animation requires it)
```

UI không truy cập DataStore, `WindowManager`, `BatteryManager` hoặc service trực tiếp.

## Target package tree

```text
com.asianmobile.emojibattery.shimeji/
├── data/
│   ├── model/
│   │   ├── StatusCapsuleConfig.kt
│   │   ├── StatusThemeCatalog.kt
│   │   └── DeviceStatus.kt
│   ├── repository/
│   │   ├── StatusCapsuleSettingsRepository.kt
│   │   ├── StatusThemeCatalogRepository.kt
│   │   └── DeviceStatusRepository.kt
│   └── repository/impl/
│       ├── DataStoreStatusCapsuleSettingsRepository.kt
│       ├── CachedStatusThemeCatalogRepository.kt
│       └── AndroidDeviceStatusRepository.kt
├── overlay/
│   ├── OverlayHostService.kt
│   ├── OverlayHostRuntime.kt
│   ├── OverlayFeatureController.kt
│   └── OverlayFrameClock.kt
├── battery/
│   ├── catalog/              # parser, validator, installer/cache
│   └── overlay/
│       ├── StatusCapsuleRuntimeCoordinator.kt
│       ├── StatusCapsuleWindowBackend.kt
│       ├── ApplicationOverlayCapsuleBackend.kt
│       ├── AccessibilityCapsuleBackend.kt
│       ├── StatusBarAccessibilityService.kt
│       ├── StatusCapsuleLayoutPolicy.kt
│       ├── StatusCapsuleRenderer.kt
│       └── StatusCapsuleView.kt
└── ui/battery/
    ├── catalog/
    ├── editor/
    ├── component/
    └── assets/
```

Business feature vẫn nằm trong `:app`; không đưa battery logic vào `:ads`.

## Runtime backend decision

```kotlin
interface StatusCapsuleWindowBackend {
    val capability: StateFlow<CapsuleBackendCapability>
    fun start(config: StatusCapsuleConfig)
    fun update(config: StatusCapsuleConfig)
    fun stop()
}
```

`StatusCapsuleRuntimeCoordinator` chọn đúng một backend theo `CapsuleDisplayMode`:

- `BELOW_SYSTEM_BAR` → application overlay trong shared foreground host;
- `COVER_SYSTEM_BAR` → accessibility overlay trong `StatusBarAccessibilityService`.

Không được add `TYPE_ACCESSIBILITY_OVERLAY` từ `OverlayHostService`: window privilege và
lifecycle thuộc AccessibilityService. Hai backend dùng chung renderer/layout/data nhưng
không dùng chung WindowManager owner.

## Shared foreground overlay host

### Vì sao không tạo `StatusCapsuleService` riêng

- Hai FGS tạo hai ongoing notification và tăng Play review surface.
- Pet/capsule dùng cùng overlay permission, screen state và foreground lifetime.
- Stop một service có thể gây trải nghiệm khó hiểu nếu notification còn lại vẫn chạy.
- Shared host cho phép một notification mô tả đúng feature nào active.

### Migration target

`PetOverlayService` được tổng quát hóa thành `OverlayHostService` với controller độc lập
cho pet và `BELOW_SYSTEM_BAR`. Migration phải giữ:

- `START_NOT_STICKY`;
- `exported=false`;
- foreground promotion đúng deadline;
- `specialUse` declaration;
- screen-off pause;
- overlay revoke cleanup;
- safe Start/Stop;
- position persistence và toàn bộ pet session contract hiện tại.

Không rewrite pet engine/controller trong cùng phase nếu không cần. Service host chỉ thay
ownership/lifecycle boundary.

## Foreground overlay feature lifecycle

```kotlin
interface OverlayFeatureController {
    val isActive: Boolean
    fun start()
    fun pause()
    fun resume()
    fun onConfigurationChanged()
    fun stop()
}
```

Host nhận explicit action:

- `START_PETS`;
- `STOP_PETS`;
- `START_STATUS_CAPSULE`;
- `STOP_STATUS_CAPSULE`;
- `STOP_ALL`.

Service tiếp tục chạy khi ít nhất một foreground-overlay feature active. Accessibility
cover mode không giữ host này sống nếu không có pet. Stop capsule không remove pet window;
Stop pet không remove capsule backend đang thuộc AccessibilityService. Runtime expose:

```text
isServiceRunning
arePetsRunning
activePetCount
isStatusCapsuleRunning
statusCapsuleDisplayMode
statusCapsuleCapability
```

## Accessibility service lifecycle

`StatusBarAccessibilityService`:

- được Android bind sau khi user bật trong Accessibility Settings;
- khai báo `BIND_ACCESSIBILITY_SERVICE`, `isAccessibilityTool=false`,
  `canRetrieveWindowContent=false`;
- không inspect node/window content, không dispatch gesture/global action;
- chỉ add/update/remove top-bounded `TYPE_ACCESSIBILITY_OVERLAY`;
- collect applied config khi `COVER_SYSTEM_BAR` được chọn;
- remove window trong `onInterrupt()`, `onUnbind()` và khi config disabled;
- báo capability `NEEDS_ACCESSIBILITY` khi service không enabled;
- hide trên keyguard và landscape trong MVP.

Service không tự bật được và app không được giả lập permission dialog. Full contract nằm
trong [Accessibility status-cover mode](10_ACCESSIBILITY_STATUS_COVER.md).

## Notification contract

- Title/content thay đổi theo active foreground set: Pets, below-bar Battery bar, hoặc cả hai.
- Ongoing, low importance, no badge.
- Content intent về Home/Battery Catalog.
- Action tối thiểu `Stop all`.
- Nếu notification layout/action budget cho phép: `Stop pets`, `Stop battery bar`.
- Không đưa quảng cáo, upsell hoặc sensitive device state vào notification.

Accessibility cover mode không cần giữ một FGS chỉ để sở hữu accessibility window. App
vẫn phải có control Stop rõ trong Home/Catalog và service description; notification riêng
chỉ thêm nếu có product/policy reason, không tạo duplicate ongoing notification mặc định.

## Below-system-bar window

Một window dùng type helper chung với pet:

- API 26+: `TYPE_APPLICATION_OVERLAY`;
- API 24–25: legacy `TYPE_PHONE`.

Window có:

- transparent parent;
- content chỉ cao bằng capsule;
- `FLAG_NOT_FOCUSABLE`;
- `FLAG_NOT_TOUCHABLE`;
- `FLAG_NOT_TOUCH_MODAL`;
- hardware accelerated khi ổn định trên device matrix;
- gravity top/start;
- width theo current display bounds, visual content có side margins;
- Y lấy từ runtime system-bar/display-cutout insets, không dùng constant pixel.

### Position policy

1. Lấy `WindowMetrics` current bounds.
2. Lấy top safe inset từ root window insets/display cutout.
3. Trong normal mode, đặt capsule ngay dưới status bar.
4. Khi top inset bằng 0 (fullscreen), clamp vào cutout safe inset nếu có.
5. Landscape MVP: hide mặc định hoặc dùng compact layout sau owner decision.
6. Multi-window/desktop: chỉ render khi usable width đạt minimum.

## Cover-system-bar window

Window do `StatusBarAccessibilityService` tạo:

- API 24+: `TYPE_ACCESSIBILITY_OVERLAY`;
- `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE | FLAG_NOT_TOUCH_MODAL`;
- gravity top/start;
- visual bounds bằng status region, không phải full screen;
- Y bắt đầu từ display top/cutout policy để che trực quan native status bar;
- width theo display bounds, xử lý cutout bằng safe content padding thay vì đẩy toàn
  capsule xuống dưới status bar;
- hide mặc định ở keyguard/landscape;
- best-effort pause/hide khi notification shade/system panel mở;
- secure system overlay và OEM behavior luôn được ưu tiên hơn visual parity.

Coordinator phải stop window backend cũ trước khi start backend mới để không có hai capsule
chồng nhau.

## Renderer choice

Overlay dùng custom Android `View`/Canvas, không đặt Compose composition vào service:

- đo/layout nhẹ;
- render bitmap/vector mask đã preload;
- update event-driven;
- dễ giữ một window và kiểm soát allocation;
- không tạo recomposition/CoroutineScope trong draw loop.

`onDraw` chỉ đọc immutable `StatusCapsuleRenderModel`. Mapping config/device state sang
render model xảy ra trước draw.

## Layout policy

Capsule có ba vùng:

```text
[left modules] [flexible spacer] [right modules]
```

Default:

- left: time, date, emoji/emotion;
- right: airplane/ringer, animation, signal/data/Wi‑Fi, battery percent/icon/charging.

Mỗi module có `priority` và `minimumWidth`. Khi thiếu chỗ:

1. giữ battery icon + percent;
2. giữ time;
3. giữ active connection;
4. bỏ decorative emotion/animation;
5. rút gọn date;
6. bỏ optional labels theo priority;
7. không scale chữ/icon dưới minimum accessibility.

Layout policy thuần Kotlin nhận width, config và measured intrinsic sizes, trả danh sách
module/rect; phải có exhaustive unit tests.

## Device status repository

`AndroidDeviceStatusRepository` đăng ký callback/receiver chỉ khi capsule active:

- battery sticky/dynamic receiver;
- time tick/time/timezone/locale changes;
- `ConnectivityManager.NetworkCallback`;
- airplane mode;
- ringer mode;
- API 36 tethering callback nếu component enabled.

Repository expose immutable `StateFlow<DeviceStatus>`. Mọi receiver/callback được unregister
trong `close()`/service cleanup. Exception platform được map sang `UNKNOWN`, không crash service.

## Clock and invalidation

- Static capsule: không giữ Choreographer callback; chỉ invalidate khi config/device state đổi.
- Time: update mỗi phút.
- Battery/connectivity: callback-driven.
- Animated decorative asset: subscribe shared `OverlayFrameClock`, tối đa 12–15 FPS.
- Nếu pet đang chạy, clock chọn FPS cao nhất cần thiết và dispatch delta cho subscriber.
- Nếu chỉ capsule static chạy, CPU phải gần idle.

## Live update contract

Service collect **applied configuration**. Apply mới:

1. repository sanitize/validate;
2. persist atomic value;
3. emit `StateFlow`;
4. controller preload replacement assets;
5. swap render model/window size;
6. giữ window cũ nếu replacement fail;
7. log error không chứa asset URL/token.

Không remove/add window cho mỗi slider change trong editor vì slider chỉ sửa draft.

## Failure and cleanup

- Overlay permission revoke: stop/remove mọi overlay window và reset runtime state.
- Accessibility disable/unbind: remove accessibility window, emit `NEEDS_ACCESSIBILITY`,
  giữ applied config và không tự đổi display mode.
- Invalid config: sanitize về built-in default; không crash.
- Missing asset: fallback per-component hoặc full built-in theme.
- Add/update window error: remove capsule window; pet controller vẫn sống nếu healthy.
- Service destroy: unregister callbacks, cancel jobs, stop clock, recycle non-cache owner
  assets và remove notification.
- Screen off: pause pet clock và capsule animation; battery/time state
  được refresh lại khi screen on, không catch up frame.
