# Pet Overlay — Current Platform Contract

## Trạng thái

Platform và product vertical slice đã hoàn tất. `PetOverlay.start(context)` chỉ start khi `Settings.canDrawOverlays(context)` trả `true`; mọi request access phải qua shared overlay disclosure rồi biến thể overlay của Grant Permissions trước khi mở system settings, còn My Pet/Pet Store điều khiển Start/Stop.

## Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `PetOverlay` | Check special access, tạo settings intent, start/stop service |
| `PetOverlayRuntime` | Process-local running/active-count flows |
| `PetOverlayService` | Promote foreground, notification/channel, lifecycle cleanup |
| `PetOverlayController` | Danh sách bounded window/state machine và một shared frame clock |
| `PetOverlayView` | Code-native demo cat, tap/drag/fling input, không giữ business state |
| `PetSpeechBubbleView` | Bubble non-touchable; mỗi pet đang TALK sở hữu một window tạm thời riêng |

## Manifest và policy

- `SYSTEM_ALERT_WINDOW`: tạo `TYPE_APPLICATION_OVERLAY`; user phải cấp qua system settings.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`: service target SDK 36 khai báo `specialUse` và property giải thích use case.
- `POST_NOTIFICATIONS`: gate product bắt buộc của flow Pet on Screen trên API 33+; dưới API 33
  gate tự đạt. Home request runtime permission đúng một lần sau khi top-level tab xuất hiện và
  full-screen ad đã đóng; trạng thái đã hỏi được persist. Foreground service vẫn luôn tạo
  notification/channel theo contract Android.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: chỉ dùng khi runtime signals xác nhận thiết bị cần
  exemption; mở dialog trực tiếp cho package và fallback về settings list nếu ROM không hỗ trợ.
  Trước khi phát hành Google Play phải review/khai báo use case theo power-management policy.
- Service `exported=false`, trả `START_NOT_STICKY`, không có boot receiver và không tự restart.
- Play Console phải khai báo/review foreground-service type trước release.

## Ownership của switch pet

Discover không còn render switch pet. User quản lý pet nổi từ My Pet Room hoặc Pet Store;
`HomePetPolicy` chỉ còn phục vụ flow Home cũ và không được nối lại vào Discover.

## Thứ tự khi một flow pet yêu cầu Start/Stop

Thứ tự gate bắt buộc cho mọi entry point bật pet:

1. đang chạy → `STOP`, không hỏi gì thêm;
2. **chưa chọn pet nào → `CHOOSE_PET`** (toast dẫn sang My Pet Room). Phải đứng **trước** mọi
   permission: hỏi xong hai màn hệ thống rồi mới báo "chưa chọn pet" là bắt user trả giá cho
   việc không xảy ra;
3. chưa có overlay access → `OPEN_OVERLAY_SETTINGS`. Đây là quyền **bắt buộc**;
4. API 33+ chưa có `POST_NOTIFICATIONS` → mở biến thể Pet on Screen của Grant Permissions.
   Runtime prompt chỉ được thử khi app chưa từng hỏi; lần tiếp theo dẫn tới App Notification
   Settings để tránh vòng lặp launcher trả kết quả ngay sau khi user đã từ chối;
5. còn lại → `START`.

Grant Permissions mở theo thứ tự Overlay → Notification → Battery Optimization → Auto Start.
Overlay và Notification chỉ đi tiếp khi đã cấp; nếu user quay lại mà chưa cấp thì dừng chuỗi để
không ném họ trở lại cùng system surface. Ngay khi hai quyền bắt buộc đạt, pet start lập tức; hai
bước ổn định sau đó là optional và nếu bị bỏ qua cũng không lặp vô hạn.

Battery-optimisation exemption không chặn pet start: nó là bước optional tiếp theo, chỉ có ý
nghĩa trên máy giết foreground service và chỉ hiện khi `PetBatteryOptimizationPolicy.reasonFor`
trả về nguyên nhân còn cần xử lý. Dialog package-scoped được thử trước, rồi mới fallback settings.

Row đó hiện khi **bất kỳ** tín hiệu nào sau đây đúng: `isBackgroundRestricted`, standby bucket
`RESTRICTED`, một lần chết process ngoài ý muốn,
**ROM có màn power manager riêng resolve được**, hoặc brand nằm trong danh sách vendor. Thứ tự
đó cũng là thứ tự ưu tiên của `reasonFor()`: platform nói thẳng trước, rồi cái đã ghi nhận được,
rồi cái máy này thật sự ship, và brand cuối cùng vì nó là suy đoán duy nhất.

Grant đơn lẻ không khiến một stock Android device trở thành thiết bị cần exemption. Trên ROM đã
được policy xác nhận, dashboard Mine vẫn giữ row sau grant để thể hiện trạng thái; flow Pet thì
ẩn vì không còn gì cần xin.

Hai tín hiệu cuối tồn tại vì `isBackgroundRestricted` là API 28+, còn standby bucket và
`getHistoricalProcessExitReasons` là API 30+ — dưới các mức đó không có gì đo được, mà
`minSdk = 24`. `hasVendorPowerScreen` chạy ở mọi API và là *đo đạc* chứ không phải đoán, nên nó
gánh phần lớn khoảng trống này; brand list chỉ còn cho ROM giấu power manager khỏi `<queries>`.

Brand được so khớp **theo từ**, trên cả `Build.MANUFACTURER` lẫn `Build.BRAND`: Transsion khai
`INFINIX MOBILITY LIMITED` / `TECNO MOBILE LIMITED`, MIUI khai manufacturer `Xiaomi` với brand
`Redmi`/`POCO`, nên so bằng chuỗi nguyên sẽ trượt đúng những ROM danh sách này sinh ra để bắt.

Nguồn platform: [Android foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [launch foreground service](https://developer.android.com/develop/background-work/services/fgs/launch), [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY), [overlay special access](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context)).

## Runtime invariants

- Window trong suốt có kích thước 48–144dp theo pack/setting; owner pet ở 100% dùng
  84dp. Window chỉ bắt touch trong hitbox pet, không dùng full-screen overlay.
- Khi pet nói, controller tạo tối đa một window non-touchable thích ứng cho chính pet đó
  80–220dp × 48–112dp. Width lấy từ glyph/dòng thực tế và usable viewport; câu vừa đủ dài
  cũng được wrap để giữ tỷ lệ width/height trong khoảng 1,65–2,6 thay vì kéo thành một
  dòng quá rộng. Height tăng tới bốn dòng, sau đó ellipsis. Window bị remove đúng khi
  action rời cả `TALK` và `TALK_WALK`, drag/fling, Stop hoặc service destroy. Không có
  timer speech độc lập với engine. Hai pet cùng TALK có hai window/lifecycle độc lập.
- Mixed dùng 1–12 slot, mỗi slot có visibility riêng; instance hidden không tick, không
  tham gia social/crowd và không giữ speech window. Visibility thay đổi trực tiếp, không
  cần Stop/Start.
- Mixed Add/Remove/character replacement được reconcile theo `pack.key`: pet còn tồn tại
  giữ nguyên view, engine, action, animation cursor và vị trí kể cả khi Remove làm slot
  phía sau dịch index. Chỉ pack mới tạo instance mới; pack bị xóa mới remove window.
  Duplicate pack được ghép theo thứ tự ổn định. Speech window/director nhẹ được dựng lại
  theo ID mới, còn pet bitmap/window không bị reset.
- Swarm dùng 1–12 instance cùng pack (low-RAM tối đa 6). Controller không khởi tạo
  social director/crowd resolver và engine loại `TALK`/`TALK_WALK` khỏi tập action thật
  sự hỗ trợ, kể cả khi pack dùng TALK cho tap hoặc khai báo TALK làm `nextAction`. Vì vậy
  Swarm không có pet-to-pet scene, overlap correction, speech action hay bubble window.
  4–6 pet tối đa 20 FPS, 7–12 pet tối đa 16 FPS.
- Swarm dùng behavior profile riêng: khoảng nghỉ trên sàn giảm còn 0,9–2,4 giây, idle
  còn 0,8–1,8 giây, wall jump tăng từ 55% lên 90%, và stunt chiếm phần lớn tổng weight.
  Hai story nhảy qua lại giữa tường chiếm ít nhất 25% raw scheduler weight; sau tối đa một
  story không leo, pack có đủ action sẽ ưu tiên wall/ceiling/aerial story. Pack ít frame
  vẫn fallback sang zoomies/patrol/dash/explorer mà không giả action.
- Swarm size/speed lấy từ base profile; khi randomization bật, mỗi instance nhận variation
  deterministic tối đa ±2 step nên cùng index không đổi variation giữa các lần update.
  Size thay đổi dispatch `SizeChanged` để giữ surface/tâm hiện tại; speed chỉ thay engine
  timeline/config rồi tiếp tục cùng `PetState`, action, combo và animation cursor ở tick
  kế tiếp. Optional movement area inset 0–30% cập nhật `PetBounds` tại chỗ. Inset được
  tính trên viewport trước khi bù edge-overflow theo size, nên giá trị 0% giữ đúng hành vi
  chạm mép và không phát sinh lề ngang tỷ lệ với kích thước pet. Khi slider làm bounds
  co hoặc giãn, controller remap vị trí chuẩn hóa qua bounds mới; kéo spacing giảm vì vậy
  đưa pet trở lại vùng tương ứng trước đó thay vì giữ tọa độ đã bị clamp vào trong.
- Thay đổi riêng Swarm count được reconcile incremental trong controller: tăng count chỉ
  tạo engine/view cho các index mới, giảm count chỉ remove các index cuối. Pet đang tồn
  tại giữ nguyên `PetState`, action, animation cursor, vị trí và window; visual đã preload
  được dùng lại. Controller chỉ tính lại shared FPS budget, không stop/start service hay
  reset cả đàn. Chỉ đổi mode hoặc Swarm character mới rebuild để thay asset an toàn.
- Mỗi instance được thêm live lấy 12 vị trí ngẫu nhiên trong vùng movement đang áp dụng
  và chọn ứng viên xa các pet hiện có nhất. Vùng spawn loại phần screen-edge overflow nên
  pet mới luôn xuất hiện đầy đủ trên màn hình; khi movement constraint bật, vị trí random
  tuân thủ trực tiếp bốn inset. Initial Start vẫn dùng layout ổn định hiện tại.
- Mọi instance dùng chung đúng một `Choreographer.FrameCallback` trên main thread.
- Frame loop chỉ reduce engine + invalidate/update layout; không decode bitmap, parse file hoặc tạo thread.
- Mỗi slot resolve pack/visual/engine config riêng, gồm size, speed, touch flag và speech
  catalog/toggle; slot trùng pack dùng chung bitmap cache đã preload. Tất cả instance vẫn
  dùng chung một clock/service.
- Service observe toàn bộ profile active trong DataStore. Khi một slot đổi size, controller
  update đúng window ngay lập tức, giữ chân/tâm hoặc edge attachment theo surface hiện tại,
  rồi cập nhật bounds, social geometry và speech placement từ cùng `PetState`. Khi speed
  đổi, controller thay timeline/config của đúng engine nhưng tái sử dụng nguyên state đang
  chạy; frame timing và scripted velocity mới có hiệu lực ở tick kế tiếp.
- Touch toggle đổi trực tiếp `FLAG_NOT_TOUCHABLE`. Messages và custom-message list thay
  speech director/window của đúng slot; bubble đang hiện được đóng để catalog mới có hiệu
  lực ở lần TALK kế tiếp. Reset revision đưa instance về default position ngay.
- Đổi mode hoặc Swarm pack làm service preload visual rồi rebuild controller an toàn
  trong cùng foreground session. Swarm count/size/speed/randomization/movement bounds và
  Mixed roster dùng đường incremental riêng nên không Stop/Start service hoặc reset pet.
- Tap/drag/fling đều được chuyển thành `PetEvent`. Hệ tọa độ overlay chỉ fit status bar/display cutout một lần, không trừ navigation bar ở đáy; vì vậy đáy pet chạm đáy màn hình vật lý thay vì dừng phía trên thanh điều hướng.
- Playground cho phép cửa sổ pet tràn `1/3` chiều rộng qua mép trái/phải và `1/3` chiều rộng qua mép trên, còn mép dưới không tràn. `FLAG_LAYOUT_NO_LIMITS` là bắt buộc để WindowManager không clamp lại cửa sổ nhỏ; hit target vẫn chỉ bằng đúng kích thước pet.
- Sprite pack dùng quy ước frame gốc quay sang trái. Renderer mirror ngang khi engine đi
  sang phải và chỉ thêm squash/stretch/lean nhẹ quanh bottom anchor cho locomotion/va
  chạm; Special dùng nguyên sprite, không scale luân phiên gây flicker. Pose climb
  wall/ceiling không bị xoay sai hướng.
- Owner pack tách visual đứng/ngồi ở runtime: `IDLE` dùng frame đứng đầu tiên của `WALK`
  nhưng engine giữ zero velocity, còn frame 11 chỉ xuất hiện khi action thật sự là `SIT`.
  Pack ngoài prefix `owner.shimeji.` giữ nguyên visual IDLE do manifest khai báo.
- Owner pack còn tách đúng ngữ nghĩa pose: `EMOTE` dùng frame 15/17, `FLOOR_PLAY` dùng
  31/32 trên sàn, `SPRAWL` dùng pose nằm cuối creep, còn `HOLD_WALL`/`HOLD_CEILING` giữ
  frame 13/23 đúng surface. Các alias này chỉ tồn tại ở runtime nên không đổi schema pack.
- Speech chỉ tồn tại trong `TALK` đứng yên một frame 34 hoặc `TALK_WALK` di chuyển bằng
  34/35/34/36. Box chữ nhật góc vuông dùng contract `WalkWithIE`, bám đáy ở
  `anchorY - 0,5 × petHeight`, nằm trước hướng nhìn, mirror theo direction và follow
  shared tick khi pet đi. Solo speech quay vào tâm viewport; social TALK giữ facing với
  pet kia. Sprite mirror và box placement cùng đọc `PetState.direction`, nên không có hai
  nguồn hướng riêng. Box không bo góc, không có tail tam giác và không còn overhead
  fallback.
- Living Behavior dùng weighted scheduler với khoảng chờ biến thiên, continue/turn-around decisions, recent-action memory và deterministic seed riêng cho từng instance. Vì vậy nhiều pet không chạy đồng bộ nhưng mọi transition vẫn tái lập được trong JVM test.
- Pet đang `WALK`/`RUN`/`CREEP`/`TALK_WALK` được phép đi xuyên nhau và không bị đổi hướng
  bởi crowd resolver. Resolver chỉ sửa overlap sâu trên 55% canvas khi cả hai pet đã dừng
  ở pose nghỉ, không can thiệp cặp social và không biến pet khác thành “bức tường”.
- Social director chỉ ghép pet rảnh trên sàn, ở cách nhau tối đa 4,5 pet-width. Mỗi lần
  đủ điều kiện có 35% xác suất nhận lời; từ chối sẽ giữ khoảng riêng 18 giây, hoàn tất
  social cooldown 45 giây. Tap/drag/fling/combo khác làm mất ownership và hủy session
  ngay, không tự ép pet quay lại social approach.
- Tap, showcase và combo habitat `GROUND` bị từ chối khi pet đang leo, treo hoặc bay.
  Mọi transition vào `TALK` còn có guard cuối trong engine: nếu không đứng trên sàn thì
  combo bị hủy về physics `FALL`/ground fallback thay vì render frame nói trên tường.
- State graph hỗ trợ `fall → bounce → walk`, run/creep có timeout, leo lên/leo xuống và
  các routine có anticipation/action/recovery như `sit → floor-play → idle`,
  `run → trip → sprawl` và `look → special → idle → emote`. Wall/ceiling collision có
  thể vào pose hold đúng surface trước bước nhảy tiếp theo; pet tới mép trần có thể leo
  xuống thay vì luôn rơi. Xác suất của action thiếu không bị dồn nhầm sang `CLIMB_DOWN`;
  mọi exit khỏi wall climb tự động quay pet vào tâm viewport, kể cả pack có frame leo nhưng
  thiếu frame nhảy. Sau khi leo xuống/rơi và chạm sàn pet không đi ngược lại mép cũ để mắc
  trong vòng lặp leo ngắn. Pack v1 cũ chỉ tham gia action thật sự khai báo và vẫn fallback
  walk/idle an toàn.
- Story beat biểu diễn dùng playback `PLAY_ONCE`: Special chạy trọn một lượt gần 3 giây,
  rồi combo chuyển sang beat idle/look/emote recovery 3–6 giây. Không giữ frame cuối vì
  frame 41/46 của nhiều model là motion blur, portal, clone hoặc sprite chỉ còn một phần.
  Beat locomotion/pose thường vẫn repeat; combo chỉ chuyển sau target duration hoặc
  collision tương ứng.
- Fall dùng gravity/terminal velocity thay cho tốc độ dọc cố định. Thả kéo nhẹ phát `DragEnd → Fall`; chỉ thao tác vượt system minimum-fling velocity mới vào physics fling.
- Stop chuẩn hóa vị trí 0–1 vào DataStore; Start sau process/orientation change restore và
  clamp theo usable bounds mới. Reset position/revision theo slot cập nhật instance ngay
  và ngăn position snapshot cũ ghi đè reset hoặc reorder.
- Default multi-pet layout giãn ngang 1,05 pet-width; vị trí restore trùng sâu được sửa
  sau khi cả hai pet đã đáp và cùng ở pose nghỉ.
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
- Frame-semantic choreography V3.17 verified trên Pixel 3 XL với ba `Satoru Gojo`: các
  capture theo mốc ghi nhận ground, wall-climb, skill và speech diễn ra độc lập; kết thúc
  vẫn có đúng ba window 238×238 px, một foreground service và không fatal/ANR/window error.
- Pet Speech V3.7 initial slice đã verified `Natsu` conversion revision 4 và lifecycle
  non-touchable window 220×84dp; trigger tap trực tiếp của revision này đã được thay thế
  bởi pose-gated choreography V3.10 bên dưới.
- Pet Speech V3.10 verified trên cùng thiết bị: tap không mở text trong TAPPED/IDLE,
  window chỉ xuất hiện cùng frame TALK có tay đưa ra; box 220×84dp là hình chữ nhật
  không tail. Ở mép trái, pet window `[-98, 196]` có anchor X `49` và speech window bắt
  đầu đúng X `49`, xác nhận inward-facing giữ attachment chính xác. Box tự remove khi
  TALK kết thúc. Clean Stop còn 0 overlay/speech window, 0 service và không có
  fatal/window leak trong logcat.
- Pet Speech V3.12 verified với custom text ngắn: window co theo glyph còn 345×168 px
  (~98,6×48dp) thay vì fixed 770×294 px; chu kỳ speech tồn tại 10.052 ms (~10,05 giây)
  rồi remove ở cuối TALK. Settings hiển thị rõ counter 14/80 code point; sau smoke test
  đã Stop sạch và restore cấu hình ba pet.
- Pet Speech V3.13 verified với owner pack revision 4 hiện có: moving speech đổi X khoảng
  40 px/s ở speed 150%, còn tap speech giữ nguyên window qua năm mẫu 400 ms. Hai capture
  cách nhau một giây trong stationary beat có SHA-256 giống hệt, xác nhận frame 34 không
  còn bước chân. Session test đã được cleanup và restore ba pet/size 75%.
- Multi-pet V3.14 verified trên cùng thiết bị với ba pet/size 75%/speed 150%: hai mover
  khởi động overlap sâu tại X 964/998 px nhưng tiếp tục hành trình độc lập, không bị
  crowd resolver ép quay đầu. Tap trong wall traversal không tạo speech window và pet
  tiếp tục đổi từ wall phải sang wall trái. Một poll runtime ghi nhận đồng thời
  `Cute Pet speech 1` và `Cute Pet speech 2`, xác nhận controller giữ hai window theo
  owner ID. Không có fatal/window error; force-stop kết thúc với 0 overlay, 0 speech
  window và 0 service.
- Semantic cadence V3.15 đã cài đè và smoke-test với ba `Satoru Gojo` ở speed 150%:
  selection/count được giữ, service tạo đúng ba overlay 238×238 px, autonomous
  movement/speech tiếp tục qua launcher và log không có fatal, bad-token hoặc OOM.
- Standing/rest V3.16 đã verified tiếp trên cùng thiết bị: owner pet ở bottom hiển thị
  pose đứng khi window giữ nguyên X, không còn dùng frame 11 ngồi cho mọi IDLE. Ba overlay
  tiếp tục chạy sau cài đè và không có fatal/window error.
- Live Customize V3.18 verified trên Pixel 3 XL với roster Nanami/Pein/Gojo: touch toggle
  thêm/gỡ `NOT_TOUCHABLE`, messages/custom list remove/recreate speech window, reset đưa
  Nanami về default X trong cùng session, character replacement đổi window identity,
  Remove giảm 3→2 và Add tăng 2→3 overlay/speech window mà foreground service không dừng.
  Roster/profile ban đầu đã được restore; clean Stop còn 0 service/window và log không có
  fatal, bad-token hoặc replacement error.

## Chưa thuộc runtime hiện tại

- Auto-start after boot; chỉ xem xét opt-in sau quyết định product/policy.
- Sound playback; preference đã dành sẵn nhưng pack schema v1 cố ý chỉ cho image metadata.
- Cần mở rộng verification matrix sang API 33+, nhiều OEM, rotation/cutout và process death trước release.
