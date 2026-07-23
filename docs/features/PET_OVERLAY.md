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
| `PetSpeechBubbleView` | Bubble non-touchable, tối đa một window tạm thời cho toàn scene |

## Manifest và policy

- `SYSTEM_ALERT_WINDOW`: tạo `TYPE_APPLICATION_OVERLAY`; user phải cấp qua system settings.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`: service target SDK 36 khai báo `specialUse` và property giải thích use case.
- `POST_NOTIFICATIONS`: Permission/Home request trên API 33+; denial không block FGS start, foreground service vẫn luôn tạo notification/channel.
- Service `exported=false`, trả `START_NOT_STICKY`, không có boot receiver và không tự restart.
- Play Console phải khai báo/review foreground-service type trước release.

Nguồn platform: [Android foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [launch foreground service](https://developer.android.com/develop/background-work/services/fgs/launch), [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY), [overlay special access](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context)).

## Runtime invariants

- Window trong suốt có kích thước 64–196dp theo pack/setting, chỉ bắt touch trong hitbox pet; không dùng full-screen overlay.
- Khi pet nói, controller tạo tối đa một window 220×84dp non-touchable; window bị remove
  khi hết thời gian đọc, drag/fling, Stop hoặc service destroy.
- 1–3 instance dùng chung đúng một `Choreographer.FrameCallback` trên main thread; 30 FPS mặc định, 24 FPS cho 3 pet hoặc low-RAM budget.
- Frame loop chỉ reduce engine + invalidate/update layout; không decode bitmap, parse file hoặc tạo thread.
- Mọi instance dùng chung visual đã preload; mỗi instance chỉ giữ engine state/view/layout params riêng.
- Tap/drag/fling đều được chuyển thành `PetEvent`. Hệ tọa độ overlay chỉ fit status bar/display cutout một lần, không trừ navigation bar ở đáy; vì vậy đáy pet chạm đáy màn hình vật lý thay vì dừng phía trên thanh điều hướng.
- Playground cho phép cửa sổ pet tràn `1/3` chiều rộng qua mép trái/phải và `1/3` chiều rộng qua mép trên, còn mép dưới không tràn. `FLAG_LAYOUT_NO_LIMITS` là bắt buộc để WindowManager không clamp lại cửa sổ nhỏ; hit target vẫn chỉ bằng đúng kích thước pet.
- Sprite pack dùng quy ước frame gốc quay sang trái. Renderer mirror ngang khi engine đi sang phải và chỉ thêm squash/stretch/lean nhẹ quanh bottom anchor cho motion nhanh, va chạm và Special; pose climb wall/ceiling không bị xoay sai hướng.
- Speech chỉ tồn tại trong frame TALK 34–36: box chữ nhật góc vuông dùng contract
  `WalkWithIE`, bám đáy ở `anchorY - 0,5 × petHeight`, nằm trước hướng nhìn và mirror
  theo direction. Solo TALK quay vào tâm viewport để giữ box đọc được mà không phá anchor;
  social TALK giữ facing với pet kia. Box không bo góc, không có tail tam giác và không
  còn overhead fallback.
- Living Behavior dùng weighted scheduler với khoảng chờ biến thiên, continue/turn-around decisions, recent-action memory và deterministic seed riêng cho từng instance. Vì vậy nhiều pet không chạy đồng bộ nhưng mọi transition vẫn tái lập được trong JVM test.
- Sau mỗi shared tick, crowd resolver tách các pet cùng mặt sàn với khoảng cách 5% canvas; pet tự chủ quay ra ngoài khi va nhau, social combo giữ facing do director quyết định, còn pet đang bay/leo/drag được phép đi qua mà không bị correction.
- State graph hỗ trợ `fall → bounce → walk`, run/creep có timeout, leo lên/leo xuống, cùng routine như `sit → wink`, `trip → sit` và `special → special-2 → wink`. Wall timeout chọn jump/descend/fall; pet tới mép trần có thể leo xuống thay vì luôn rơi. Pack v1 cũ chỉ tham gia action thật sự khai báo và vẫn fallback walk/idle an toàn.
- Fall dùng gravity/terminal velocity thay cho tốc độ dọc cố định. Thả kéo nhẹ phát `DragEnd → Fall`; chỉ thao tác vượt system minimum-fling velocity mới vào physics fling.
- Stop chuẩn hóa vị trí 0–1 vào DataStore; Start sau process/orientation change restore và clamp theo usable bounds mới.
- Default multi-pet layout giãn ngang 1,05 pet-width; vị trí restore trùng nhau được crowd resolver tách sau khi pet đáp xuống.
- Stop action, `onDestroy` và lỗi add window đều remove callback/toàn bộ window và reset runtime state.
- Dynamic `SCREEN_OFF`/`SCREEN_ON` receiver dừng và nối lại shared frame clock; resume reset mốc tick để không chạy bù toàn bộ thời gian màn hình tắt.

## Device verification

- Google Pixel 3 XL (`crosshatch`), Android 12 / API 31: verified start/stop, foreground notification, render over launcher, drag/fling and permission revocation cleanup.
- Cùng thiết bị đã verified 3 `Sunny Cat` window, một service/shared clock, selection/count/position qua force-stop/relaunch và clean stop không fatal/OOM/window leak.
- Extended built-in behavior verified trên cùng thiết bị: initial fall, bottom landing, horizontal/autonomous movement, stable overlay position trong doze và tiếp tục di chuyển sau wake.
- Living Behavior revision 3 verified với owner pet `Pikachu`: manifest chứa run/climb-down và đủ hai Special; double-tap trên overlay chạy lần lượt hình Special/Special 2 thực tế, không fatal/OOM. Import partial-Special của `Levi Ackerman` được khóa bằng JVM test với sequence frame `1, 40`.
- Edge contract verified trên cùng thiết bị với pet 392 px: parent overlay bắt đầu tại status bar `y=171`, mép trái đạt `x=-131`, mép phải đạt `x=1179` và mép trên đạt `y=-131`; cửa sổ được phép tràn ra ngoài display thay vì bị WindowManager clamp. Playground bottom là `2789`, tương ứng đáy vật lý `2960`, nên không còn bị navigation bar 168 px đẩy lên.
- Overlay window remained 112dp and touch did not block the rest of the launcher.
- No fatal exception was recorded during the full flow; service, window and notification were all removed after Stop/revocation.
- Pet Speech V3.7 initial slice đã verified `Natsu` conversion revision 4 và lifecycle
  non-touchable window 220×84dp; trigger tap trực tiếp của revision này đã được thay thế
  bởi pose-gated choreography V3.10 bên dưới.
- Pet Speech V3.10 verified trên cùng thiết bị: tap không mở text trong TAPPED/IDLE,
  window chỉ xuất hiện cùng frame TALK có tay đưa ra; box 220×84dp là hình chữ nhật
  không tail. Ở mép trái, pet window `[-98, 196]` có anchor X `49` và speech window bắt
  đầu đúng X `49`, xác nhận inward-facing giữ attachment chính xác. Box tự remove khi
  TALK kết thúc. Clean Stop còn 0 overlay/speech window, 0 service và không có
  fatal/window leak trong logcat.

## Chưa thuộc runtime hiện tại

- Auto-start after boot; chỉ xem xét opt-in sau quyết định product/policy.
- Sound playback; preference đã dành sẵn nhưng pack schema v1 cố ý chỉ cho image metadata.
- Cần mở rộng verification matrix sang API 33+, nhiều OEM, rotation/cutout và process death trước release.
