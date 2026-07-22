# Pet Overlay — Current Platform Contract

## Trạng thái

Platform và product vertical slice đã hoàn tất. `PetOverlay.start(context)` chỉ start khi `Settings.canDrawOverlays(context)` trả `true`; Permission giải thích/request access và Home điều khiển Start/Stop.

## Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `PetOverlay` | Check special access, tạo settings intent, start/stop service |
| `PetOverlayRuntime` | Process-local running/active-count flows |
| `PetOverlayService` | Promote foreground, notification/channel, lifecycle cleanup |
| `PetOverlayController` | Danh sách bounded window/state machine và một shared frame clock |
| `PetOverlayView` | Code-native demo cat, tap/drag/fling input, không giữ business state |

## Manifest và policy

- `SYSTEM_ALERT_WINDOW`: tạo `TYPE_APPLICATION_OVERLAY`; user phải cấp qua system settings.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`: service target SDK 36 khai báo `specialUse` và property giải thích use case.
- `POST_NOTIFICATIONS`: Permission/Home request trên API 33+; denial không block FGS start, foreground service vẫn luôn tạo notification/channel.
- Service `exported=false`, trả `START_NOT_STICKY`, không có boot receiver và không tự restart.
- Play Console phải khai báo/review foreground-service type trước release.

Nguồn platform: [Android foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [launch foreground service](https://developer.android.com/develop/background-work/services/fgs/launch), [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY), [overlay special access](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context)).

## Runtime invariants

- Window trong suốt có kích thước 64–196dp theo pack/setting, chỉ bắt touch trong hitbox pet; không dùng full-screen overlay.
- 1–3 instance dùng chung đúng một `Choreographer.FrameCallback` trên main thread; 30 FPS mặc định, 24 FPS cho 3 pet hoặc low-RAM budget.
- Frame loop chỉ reduce engine + invalidate/update layout; không decode bitmap, parse file hoặc tạo thread.
- Mọi instance dùng chung visual đã preload; mỗi instance chỉ giữ engine state/view/layout params riêng.
- Tap/drag/fling đều được chuyển thành `PetEvent`; position luôn clamp theo usable system-bar/cutout bounds.
- Stop chuẩn hóa vị trí 0–1 vào DataStore; Start sau process/orientation change restore và clamp theo usable bounds mới.
- Stop action, `onDestroy` và lỗi add window đều remove callback/toàn bộ window và reset runtime state.

## Device verification

- Google Pixel 3 XL (`crosshatch`), Android 12 / API 31: verified start/stop, foreground notification, render over launcher, drag/fling and permission revocation cleanup.
- Cùng thiết bị đã verified 3 `Sunny Cat` window, một service/shared clock, selection/count/position qua force-stop/relaunch và clean stop không fatal/OOM/window leak.
- Overlay window remained 112dp and touch did not block the rest of the launcher.
- No fatal exception was recorded during the full flow; service, window and notification were all removed after Stop/revocation.

## Chưa thuộc runtime hiện tại

- Auto-start after boot; chỉ xem xét opt-in sau quyết định product/policy.
- Sound playback; preference đã dành sẵn nhưng pack schema v1 cố ý chỉ cho image metadata.
- Cần mở rộng verification matrix sang API 33+, nhiều OEM, rotation/cutout và process death trước release.
