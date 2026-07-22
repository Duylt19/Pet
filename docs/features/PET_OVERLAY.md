# Pet Overlay — Current Platform Contract

## Trạng thái

Platform và product vertical slice đã hoàn tất. `PetOverlay.start(context)` chỉ start khi `Settings.canDrawOverlays(context)` trả `true`; Permission giải thích/request access và Home điều khiển Start/Stop.

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
- `POST_NOTIFICATIONS`: Permission/Home request trên API 33+; denial không block FGS start, foreground service vẫn luôn tạo notification/channel.
- Service `exported=false`, trả `START_NOT_STICKY`, không có boot receiver và không tự restart.
- Play Console phải khai báo/review foreground-service type trước release.

Nguồn platform: [Android foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [launch foreground service](https://developer.android.com/develop/background-work/services/fgs/launch), [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY), [overlay special access](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context)).

## Runtime invariants

- Window trong suốt có kích thước 112dp, chỉ bắt touch trong hitbox pet; không dùng full-screen overlay.
- Một `Choreographer.FrameCallback` trên main thread, giới hạn update khoảng 30 FPS.
- Frame loop chỉ reduce engine + invalidate/update layout; không decode bitmap, parse file hoặc tạo thread.
- Tap/drag/fling đều được chuyển thành `PetEvent`; position luôn clamp theo usable system-bar/cutout bounds.
- Stop action, `onDestroy` và lỗi add window đều remove callback/window và reset runtime state.

## Device verification

- Google Pixel 3 XL (`crosshatch`), Android 12 / API 31: verified start/stop, foreground notification, render over launcher, drag/fling and permission revocation cleanup.
- Overlay window remained 112dp and touch did not block the rest of the launcher.
- No fatal exception was recorded during the full flow; service, window and notification were all removed after Stop/revocation.

## Chưa thuộc MVP hiện tại

- Selection persistence, multi-pet hoặc auto-start after boot.
- Cần mở rộng verification matrix sang API 33+, nhiều OEM, rotation/cutout và process death trước release.
