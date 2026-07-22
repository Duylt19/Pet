# Pet Overlay — Current Platform Contract

## Trạng thái

Phase platform đã hoàn tất nhưng chưa có entry từ Home/Permission. `PetOverlay.start(context)` chỉ start khi `Settings.canDrawOverlays(context)` trả `true`; product UX cấp quyền và Start/Stop được nối ở Phase 3.

## Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `PetOverlay` | Check special access, tạo settings intent, start/stop service |
| `PetOverlayRuntime` | Process-local `StateFlow<Boolean>` cho trạng thái running |
| `PetOverlayService` | Promote foreground, notification/channel, lifecycle cleanup |
| `PetOverlayController` | Một overlay window, usable bounds, engine dispatch, 30 FPS clock |
| `PetOverlayView` | Code-native demo cat, tap/drag/fling input, không giữ business state |

## Manifest và policy

- `SYSTEM_ALERT_WINDOW`: tạo `TYPE_APPLICATION_OVERLAY`; user phải cấp qua system settings.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`: service target SDK 36 khai báo `specialUse` và property giải thích use case.
- `POST_NOTIFICATIONS`: Phase 3 phải request theo UX phù hợp trên API 33+; foreground service vẫn luôn tạo notification/channel.
- Service `exported=false`, trả `START_NOT_STICKY`, không có boot receiver và không tự restart.
- Play Console phải khai báo/review foreground-service type trước release.

Nguồn platform: [Android foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [launch foreground service](https://developer.android.com/develop/background-work/services/fgs/launch), [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY), [overlay special access](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context)).

## Runtime invariants

- Window trong suốt có kích thước 112dp, chỉ bắt touch trong hitbox pet; không dùng full-screen overlay.
- Một `Choreographer.FrameCallback` trên main thread, giới hạn update khoảng 30 FPS.
- Frame loop chỉ reduce engine + invalidate/update layout; không decode bitmap, parse file hoặc tạo thread.
- Tap/drag/fling đều được chuyển thành `PetEvent`; position luôn clamp theo usable system-bar/cutout bounds.
- Stop action, `onDestroy` và lỗi add window đều remove callback/window và reset runtime state.

## Chưa thuộc phase này

- Overlay permission/notification permission UI.
- Home Start/Stop state và error messaging.
- Asset pack/catalog, persistence, multi-pet hoặc auto-start after boot.
