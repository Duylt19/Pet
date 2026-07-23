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
- Tap/drag/fling đều được chuyển thành `PetEvent`. Hệ tọa độ overlay chỉ fit status bar/display cutout một lần, không trừ navigation bar ở đáy; vì vậy đáy pet chạm đáy màn hình vật lý thay vì dừng phía trên thanh điều hướng.
- Playground cho phép cửa sổ pet tràn `1/3` chiều rộng qua mép trái/phải và `1/3` chiều rộng qua mép trên, còn mép dưới không tràn. `FLAG_LAYOUT_NO_LIMITS` là bắt buộc để WindowManager không clamp lại cửa sổ nhỏ; hit target vẫn chỉ bằng đúng kích thước pet.
- Sprite pack dùng quy ước frame gốc quay sang trái. Renderer chỉ mirror ngang khi engine đi sang phải; không thêm rotate/scale/bob lên các frame action vì climb wall/ceiling đã có pose riêng trong asset.
- Living Behavior dùng weighted scheduler với khoảng chờ biến thiên, continue/turn-around decisions, recent-action memory và deterministic seed riêng cho từng instance. Vì vậy nhiều pet không chạy đồng bộ nhưng mọi transition vẫn tái lập được trong JVM test.
- State graph hỗ trợ `fall → bounce → walk`, passive `idle/sit/wink/look-up/dangle/trip/special`, timed creep, wall climb timeout thành jump/fall và ceiling climb timeout thành fall. Pack v1 cũ chỉ tham gia action thật sự khai báo và vẫn fallback walk/idle an toàn.
- Fall dùng gravity/terminal velocity thay cho tốc độ dọc cố định. Thả kéo nhẹ phát `DragEnd → Fall`; chỉ thao tác vượt system minimum-fling velocity mới vào physics fling.
- Stop chuẩn hóa vị trí 0–1 vào DataStore; Start sau process/orientation change restore và clamp theo usable bounds mới.
- Stop action, `onDestroy` và lỗi add window đều remove callback/toàn bộ window và reset runtime state.
- Dynamic `SCREEN_OFF`/`SCREEN_ON` receiver dừng và nối lại shared frame clock; resume reset mốc tick để không chạy bù toàn bộ thời gian màn hình tắt.

## Device verification

- Google Pixel 3 XL (`crosshatch`), Android 12 / API 31: verified start/stop, foreground notification, render over launcher, drag/fling and permission revocation cleanup.
- Cùng thiết bị đã verified 3 `Sunny Cat` window, một service/shared clock, selection/count/position qua force-stop/relaunch và clean stop không fatal/OOM/window leak.
- Extended built-in behavior verified trên cùng thiết bị: initial fall, bottom landing, horizontal/autonomous movement, stable overlay position trong doze và tiếp tục di chuyển sau wake.
- Living Behavior revision 2 verified với owner pet `Levi Ackerman`: manifest chỉ expose các action đủ frame; runtime ghi nhận mid-screen turn, passive pause/resume, wall climb timeout → fall, landing và climb retry mà không fatal/OOM.
- Edge contract verified trên cùng thiết bị với pet 392 px: parent overlay bắt đầu tại status bar `y=171`, mép trái đạt `x=-131`, mép phải đạt `x=1179` và mép trên đạt `y=-131`; cửa sổ được phép tràn ra ngoài display thay vì bị WindowManager clamp. Playground bottom là `2789`, tương ứng đáy vật lý `2960`, nên không còn bị navigation bar 168 px đẩy lên.
- Overlay window remained 112dp and touch did not block the rest of the launcher.
- No fatal exception was recorded during the full flow; service, window and notification were all removed after Stop/revocation.

## Chưa thuộc runtime hiện tại

- Auto-start after boot; chỉ xem xét opt-in sau quyết định product/policy.
- Sound playback; preference đã dành sẵn nhưng pack schema v1 cố ý chỉ cho image metadata.
- Cần mở rộng verification matrix sang API 33+, nhiều OEM, rotation/cutout và process death trước release.
