# 06 — UI Design System Contract

Base giữ infrastructure/theme và component pattern. Product screens từ Home trở đi dùng
visual system Cute Pet. Splash, App Open Welcome Back và Language đã theo Figma;
Intro, Permission và Premium vẫn giữ UI hiện tại cho tới task update riêng.

## Cute Pet product direction

- Discover Home và Mine dùng nền trắng/gradient pastel, primary pink `#FB3675`, card trắng
  và typography Roboto theo Figma. My Pet mode dùng nền xanh-trắng nhẹ, primary teal và
  segmented control rõ selection; Catalog vẫn dùng cozy palette hiện tại.
- Pet thumbnail thật là visual chính; icon notification chỉ là fallback khi pack chưa có ảnh.
- Corner radius lớn 16–24 sdp, card rõ hierarchy nhưng ít chrome và không dùng dark utility
  dashboard cho product screens.
- Discover là landing tổng hợp; My Pet là pet room và session control; Catalog ưu tiên
  discovery bằng grid; Mine là app/support hub; Customize biểu diễn một hồ sơ pet độc lập.
- Shared primitives nằm ở `ui/component/CutePetComponents.kt`; component dark cũ không được
  dùng cho product screen mới nếu không có lý do tương thích.

Các màn Intro, Permission và Premium cố ý chưa đổi trong refresh hiện tại.

Splash và App Open Welcome Back theo Figma node `8088:12715`/`8088:12986`:

- dùng chung wallpaper pastel riêng, Nunito Black 34/40px, gradient `#FF96B8 → #FF417E`,
  viền trắng và shadow `#FF0044` 60%;
- Splash giữ hero thỏ + battery, progress indeterminate có stroke ngoài cố định và fill
  grow/shrink tuần hoàn, ad disclosure và banner SDK thật; không đóng gói creative quảng cáo
  mẫu hoặc status bar iPhone từ Figma;
- Welcome Back là Compose cover trước App Open Ad trong module `:ads`, không phải route;
  bunny GIF chạy bằng Coil và lifecycle quảng cáo hiện tại không thay đổi.

Language theo Figma node `8421:9725`:

- nền trắng, top bar `56px`, title Inter SemiBold 20/30 và action màu primary pink
  `#FB3675`; biến thể Settings giữ nút Back, còn onboarding không thêm Back;
- item `328×56px`, pill trắng, flag tròn `32px`, label Roboto Medium 16/24 và radio
  pink + tick trắng. Shadow dùng `#666666` 20% với elevation `12sdp` để tách card rõ hơn
  trên nền trắng theo yêu cầu product;
- giữ nguyên tập/thứ tự 11 locale, trạng thái ban đầu chưa chọn, confirm chỉ hiện sau khi
  user chọn và toàn bộ persistence/navigation/restart hiện có;
- native ad tiếp tục dùng placement thật `SCREEN_LANGUAGE`/`SCREEN_LANGUAGE_SECOND`, không
  đóng gói creative quảng cáo mẫu từ Figma; loading overlay vẫn chặn tương tác trong lúc ad load.

Mine visual contract theo Figma node `8080:4828`:

- route `settings` là tab Mine của Home shell, dùng lại `HomeHeader`, `HomeEnableCard`,
  wallpaper, bottom navigation và banner placement chung; screen không tự tạo bottom chrome;
- thứ tự content là enable card, Premium banner `328×100`, hai shortcut `158×70`, GENERAL
  và OTHER. Card setting rộng `328/360`, radius 16px, nền trắng và shadow 12%;
- label dùng Roboto Medium 14/20, subtitle Roboto Regular 12/16, icon vector 24px màu
  `#FB3675`, divider `#F2F2F2`; hai shortcut illustration là raster phức tạp PNG @3x;
- toggle điều khiển cùng `BatteryStatusConfig.enabled` và Accessibility gate với Discover;
  Language, Rate, Share, Contact và Privacy giữ flow thật. My Pet mở route `my_pet`,
- Favourite & Recent là destination riêng: hai tab cố định, lưới ba cột dùng favorite battery
  state thật và native ad cố định cuối màn. Recent giữ empty state cho tới khi product định nghĩa
  action tạo lịch sử, thứ tự MRU và giới hạn retention; Apps that hide icons giữ callback chờ
  feature riêng.
- Favourite & Recent bám đúng hai header state trong Figma: empty dùng title 24px ở dòng riêng;
  populated dùng title 20px inline. Icon back/heart/history và empty illustration giữ vector;
  thumbnail theme tiếp tục load từ battery catalog thay vì đóng gói sáu bitmap demo.

Discover Home contract:

- route `home` là root sau onboarding và hiển thị dữ liệu thật từ owner/battery catalog;
- toggle chính điều khiển `BatteryStatusConfig.enabled`, có disclosure và Accessibility gate;
- Home shell có bốn tab Discover/Battery/Pet Store/Mine. `HomeBottomNavigation` cố định
  trên bottom banner hiện có; từng screen không tự tạo lại bottom chrome;
- Discover, Pet Store và Mine dùng chung `HomeHeader` và `HomeEnableCard`: header `43sdp`, search
  `25sdp`, enable card `37sdp`, switch `34×18sdp`. Discover chỉ render Battery enable card;
  pet switch được quản lý ở flow pet. Không copy component rồi đổi metric riêng;
- Discover hero dùng composite `Battery Troll` PNG @3x tại tỉ lệ `328×100px`; promo creative
  `360×50px` thấp hơn là presentational slot riêng, không gọi ads SDK;
- Battery Themes dùng favorite state thật; Trending hiện dùng thứ tự catalog cho tới khi
  server có ranking riêng.

Battery catalog contract theo Figma `8102:2729` và `8286:5017`:

- landing dùng shared `HomeHeader`/`HomeEnableCard`, promo composite PNG @3x, category section
  dạng carousel và card `110×110px`; preview runtime dùng chung tỉ lệ `65%` và
  `ContentScale.Fit` của Discover vì asset dữ liệu thật có bounds khác mock Figma, favorite ở
  top-end, crown Premium ở top-start;
- crown Premium của Pet Store, Search và Battery dùng chung `PetPremiumBadge`: nền
  `#FFEA89` 50% và crown artwork theo tỉ lệ `18/24`, không dựng badge riêng theo từng feature.
  Search hiện crown khi theme có entitlement Premium, user chưa Premium và ID chưa được mở bằng
  reward; entitlement được đọc lại khi Search resume;
- category header dùng trực tiếp emoji ở đầu `category.name`; không ghép thêm drawable/icon riêng
  để tránh hiển thị trùng khi catalog cập nhật tên category;
- More mở child route có header Back/title/PRO, inline banner và grid ba cột. Card detail giữ
  tỷ lệ vuông, preview `74/101.333`, selected dùng `#FFEBF1` + stroke `#FB3675`, không hiện heart;
- landing giữ Home bottom navigation; detail ẩn navigation nhưng giữ cùng bottom banner holder.
  DIY FAB và Lottie star bling dùng lại component Discover.

Customize Status Bar theo Figma `8227:4332`, `8345:6256`, `8240:7335`, `8240:7466`,
`8227:6510`, `8155:4852`, `8345:6797`, `8227:6044`, `8240:8590`, `8345:7719`:

- overview dùng Material large app bar `exitUntilCollapsed`: title lớn khi expanded và title
  inline khi collapsed; Back và PRO luôn pinned;
- preview `328×50px` luôn được ghim ngay dưới app bar trên overview và mọi màn More; chỉ phần
  option bên dưới cuộn. Overlay thật chỉ live-update khi feature đã bật; trạng thái tắt chỉ
  dùng preview nhúng. Preview dùng cùng thứ tự vật lý với renderer thật: trailing LTR là
  Hotspot → Signal/Data → Wifi → Percentage → Battery/Emoji pair → Charge; Battery và Emoji
  chồng cùng tâm và mọi component cách nhau 4dp;
- More Battery/Emoji mở grid ba cột với artwork 73.03% item; More Theme mở grid hai cột từ
  background catalog runtime. Tất cả child giữ chung draft và Back không tự Apply;
- Template có đủ Battery, Emoji và Animation; header dùng icon Figma 16px và chevron vector
  14px. Color có custom wheel, bảy preset và ba theme preview; custom wheel mở HSV/opacity
  sheet, cập nhật trực tiếp cùng draft/live-preview policy;
- card dùng shadow token `#6666661F`, y=8, blur=24; slider dùng Roboto Medium 16/24 cho
  label và 14/20 cho value. Slider dùng Material 3 interaction/semantics giống Pet Settings,
  track hồng không tick và vùng điều khiển cao 48px theo Figma; không tự vẽ thumb dạng thanh.
  Mười icon Customize đều dùng VectorDrawable Figma màu `#FB3675`;
- toàn màn dùng Roboto local đúng weight: top bar/section/Apply là SemiBold 600, row/slider/grid
  là Medium 500 và More là Regular 400. Top bar collapsed 20/28, expanded 24/32; không dùng
  SansSerif synthetic hoặc Roboto Condensed để giả SemiBold;
- Apply là Roboto SemiBold 18/26 và nằm trong panel sticky phía trên banner editor dùng chung;
- Back khi draft chưa Apply mở discard sheet full-width theo node `8345:7719`: Cancel giữ draft,
  Exit restore config đã lưu rồi pop; sheet dùng native `HEIGHT_222`.

Pet Store visual contract:

- My Pet hero dùng nguyên node Figma `8403:6520` dưới dạng PNG @3x `984×399`; bitmap có
  transparency và nhân vật nhô khỏi nền nên render `328×133px` không clip thêm. Chạm toàn bộ
  banner mở My Pet Room;
- Pet/Food selector dùng bốn image-fill state riêng từ Figma (`selected`/`unselected`);
  đây là raster artwork nhiều màu nên lưu PNG @3x trong `drawable-nodpi`, không thay bằng
  emoji hoặc icon navigation;
- pet card giữ tỷ lệ `104/142`, image area `104/90`, thumbnail theo tỷ lệ item và crown
  premium 20px tại top-end;
- food card giữ tỷ lệ `104/122`, image area `104/90`, artwork `70/104`; badge giá tại
  `(6,6)` dùng coin artwork PNG @3x, badge số lượng tại `(62,66)`, title Roboto 12/16;
- reward sheet dùng Roboto Medium cho title và action; gradient và stroke nút là
  `#C95DFF → #FB54BB`. Selected Pet Store, video và tape giữ asset vector gốc. Pet Store và
  Battery dùng chung `RewardOfferSheet` full-width; Battery có preview `110×110px` và native
  slot `336×222px` theo Figma `8145:4924`.
  Reward dialog dùng immersive navigation trên chính dialog window để system navigation
  không chiếm vùng CTA; đóng dialog không thay đổi immersive policy của Home activity.
- unlock-success overlay của Pet và Food dùng chung frame `360×800`: nền đen 50%, Lottie
  lighting chiếm `310/360` chiều rộng tại `y=244`, hero chiếm `174/310` vùng lighting.
  Title tại `y=237` dùng nguyên artwork Figma PNG @3x, không dựng lại bằng Compose Text:
  Pet dùng node `8175:3957` rộng `156/360`, Food dùng node `8175:3962` rộng `189/360`.
  SVG của các title có mask/filter không được Android VectorDrawable hỗ trợ. Food quantity
  là pill `52×34`, nền `#8D6037`, stroke trắng 1px tại `(203,436)`; artwork món ăn dùng
  PNG transparent @3x. CTA dùng Roboto Medium 20/28 tại `y=554`. Pet vừa cài chạy clip
  `SPECIAL` thật của pack; chỉ fallback `SPECIAL_2`, rồi thumbnail tĩnh khi pack không
  cung cấp skill đặc biệt.

System bar:

- app chạy edge-to-edge, status bar và navigation bar luôn trong suốt. **Mọi màn** dùng
  `isAppearanceLightStatusBars = true` — "light bar" nghĩa là nền sáng nên hệ thống vẽ đồng hồ
  và icon **màu tối**, đủ tương phản với tông trắng chủ đạo của app;
- `MainActivity.applyLightSystemBars()` là **nơi duy nhất** quyết định hình thức system bar.
  Không màn nào override; helper `TransparentStatusBarEffect` cũ đã bị xoá. Màn nào muốn icon
  sáng thì phải đủ tối để đọc được icon sáng, và không màn nào còn như vậy trong thiết kế mới;
- helper được gọi lại ở `onCreate`, `onResume` và mỗi lần đổi window focus, vì màn system
  settings, full-screen ad hay system dialog trả window về kèm appearance của chính nó;
- **nợ đã biết**: onboarding Permission (`ui/permission/PermissionScreen`)
  vẫn còn nền `#161718`, nên icon tối chìm trên màn đó cho tới khi UI được
  dựng lại theo tông trắng. Language đã dùng nền trắng; Splash/Welcome Back dùng wallpaper pastel và Intro dùng
  `img_splash_bg` sáng nên đọc bình thường.

Switch dùng chung toàn app theo Figma node `8080:7307` (bật) và `8080:7343` (tắt):

- `ui/component/AppSwitch.kt` là switch duy nhất của app; mọi toggle dùng nó, không màn nào
  tự vẽ bản riêng. Track `44×24` radius 12, núm `20×20` thụt vào `2`, núm **luôn trắng**;
  chỉ track đổi màu: bật `#FB3675`, tắt `#C8C8C9`;
- inset của núm phải **tính ra từ `(trackHeight - knobSize) / 2`**, không được khai báo thành
  dimension riêng. Mỗi giá trị sdp làm tròn theo bucket của chính nó, nên `_2sdp` cạnh
  `_18sdp`/`_15sdp` lệch khỏi tâm — bản cũ chừa `2` phía trên nhưng chỉ `1` phía dưới nên núm
  bị đẩy lên. Cùng lý do, vị trí bật là `trackWidth - inset - knobSize` chứ không phải
  `_17sdp`, và núm căn `Alignment.CenterStart` rồi chỉ offset theo trục ngang;
- có golden `AppSwitchStatesScreenshotTest` chụp cả hai state, vì trước đó switch không được
  phủ gì nên đã lệch design mà không ai phát hiện. Kiểm tra bằng cách đo lề núm trong golden:
  lề trên phải bằng lề dưới, và lề trái khi tắt phải bằng lề phải khi bật.

Grant Permissions contract theo Figma node `8080:9754`:

- route `grant_permissions` mở từ Mine. Nền **trắng phẳng** — lớp wallpaper trong design bị tắt,
  card trắng nổi lên bằng shadow chứ không bằng đổi màu nền;
- top nav `360×56` có back 28 và **một** title duy nhất `Grant Permission` Roboto 600 20/28.
  Node PRO pill và heading lớn `Grant Permissions` đều là layer ẩn trong Figma nên không dựng;
- ba nhóm đánh số bằng chip tròn `24×24` nền `#FB3675` (số Roboto 500 16/24), cách chip 8px:
  Necessary, Enhanced Stability, Recommend. Khoảng cách heading → card và card → card là 12px,
  giữa hai nhóm là 20px;
- card trắng `328×?` radius 16, padding trong 16, shadow `DROP_SHADOW r=9 offset=(0,0) a=0.17`.
  Compose đổ bóng xuống dưới theo elevation chứ không blur đều như Figma, nên copy đúng alpha
  thì card trắng trên nền trắng gần như mất viền — dùng `shadow(_8sdp, #212327 alpha 0.30)`
  để bóng đọc được; icon quyền `34×34` radius 10 với gradient riêng
  từng quyền, cách text 8px, text cách switch 8px;
- card accessibility có badge `Required` `#FFECEC`/`#F04438` hoặc `Allowed`
  `#E6F9EF`/`#00C062` (Roboto 500 10/14, padding 10×4), minh hoạ hai bước và CTA
  `Go to Settings` gradient `#C95DFF → #FB54BB` cao 40; các card còn lại dùng `AppSwitch`
  chung với Home;
- ảnh minh hoạ hai bước export theo **render bounds `296×96`**, không phải layout bounds
  `296×92`: hai khung điện thoại tràn khỏi frame 4px, export theo layout bounds thì đáy bị
  cắt. Compose dựng bằng `aspectRatio(296f/96f)`;
- row Ignore Battery Optimization **không biến mất sau khi cấp**: nó hiện khi exemption có ý
  nghĩa với máy này, còn switch phản ánh trạng thái, nên user vẫn thấy và gỡ lại được;
- không quyền nào được cấp trong app: mỗi mục chỉ mở system surface tương ứng rồi đọc lại
  trạng thái ở `ON_RESUME`. Ignore Battery Optimization mở
  `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` thay vì hộp thoại một chạm, vì hộp thoại đó
  cần `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — quyền Play chỉ cấp cho nhóm use case hẹp mà
  app này không thuộc về;
- row Ignore Battery Optimization **chỉ hiện khi exemption thật sự làm được gì**. Trên máy
  không giết foreground service nó vô nghĩa với app này: exemption chỉ mở network + partial
  wake lock trong Doze, mà `PetOverlayService` không dùng cả hai và còn tự `pauseRendering()`
  khi `ACTION_SCREEN_OFF`; Doze cũng không dừng một foreground service.
  `PetBackgroundRestrictionReader` đọc bốn tín hiệu, `PetBatteryOptimizationPolicy` quyết định:
  `isBackgroundRestricted()` (API 28+), standby bucket `RESTRICTED` (API 30+),
  `getHistoricalProcessExitReasons()` cho thấy process từng chết lúc đang chạy foreground
  service vì `REASON_SIGNALED`/`LOW_MEMORY`/`OTHER` (API 30+), và cuối cùng mới tới danh sách
  vendor. Ba tín hiệu đầu là đo thật nên bắt được cả ROM tuỳ biến lẫn hãng mới; danh sách
  vendor chỉ dùng cho máy API < 30 hoặc lần chạy đầu chưa có sự cố nào. Row ẩn khi đã cấp;
- native ad ghim cố định dưới cùng màn, ngoài `LazyColumn`, nên nó không cuộn cùng danh sách;
- row **Allow auto-start** hiện khi ROM có allowlist riêng của hãng (`PetVendorPowerSettings`
  resolve component qua `PackageManager`, package khai trong `<queries>` để API 30+ nhìn thấy).
  Đây là ask tách biệt với battery exemption: cấp cái này không cấp cái kia, nên row vẫn hiện
  ngay cả khi user đã cấp exemption. Không API nào đọc được trạng thái allowlist, nên row dùng
  mũi tên thay switch và intent được resolve lại đúng lúc chạm;
- disclosure Accessibility (`GrantPermissionDialog`) theo Figma `8437:7570` và `8437:9099`
  là bottom sheet full-width bo hai góc trên 24px, scrim 50%, handle `32×4`, title Roboto
  SemiBold 18/26 và body Roboto Regular 14/20. Nội dung dài là phần duy nhất được cuộn; hàng
  consent, nút `Allow`/`Close` và native ad luôn cố định. Checkbox dùng đúng hai vector của rate
  flow; `Allow` chỉ chuyển sang Android Accessibility Settings sau khi user đã tick consent;
- toàn bộ Discover, Battery Catalog, Mine, Status Bar Editor và Grant Permissions dùng chung
  disclosure này. Khi quyền đã bật, row Accessibility trong Grant Permissions mở thẳng Settings
  để user quản lý/tắt quyền, không hỏi consent lại.

Input text contract:

- mọi Compose `BasicTextField` và Material `TextField`/`OutlinedTextField` dùng cursor
  `#FB3675`; Android theme cũng đặt `colorAccent` cùng màu để native input không rơi về accent
  tím hoặc màu mặc định của hệ thống;
- dialog đặt tên pet hiển thị tên gợi ý ban đầu bằng `#6F7073`; sau lần chỉnh sửa đầu tiên text
  chuyển sang màu chính `#212327`. Placeholder rỗng dùng `#9B9C9E`, vì vậy vẫn phân biệt rõ
  suggestion, text user nhập và placeholder.

My Pet Room contract theo Figma node `8177:3972`, `8185:4332`, `8191:5950`:

- route `my_pet` là scene phòng full-screen, background lấy từ room catalog (`bg/BG_<id>.png`)
  và vẽ `ContentScale.Crop`; không dùng Home chrome, không có bottom navigation;
- top bar `360×64` gồm back `32×32`, biển gỗ `178×40` (`img_pet_room_sign`) mang title, và
  music toggle `32×32` hai state. Mọi action là nút trắng bo góc `32×32`, icon canh giữa;
- shortcut Pet Store `50×68` nằm mép phải dưới top bar; chevron `32×32` ngay trên sheet
  thu/mở sheet và xoay 180° khi đã thu;
- sheet `360×236` = tab strip `346×40` + body `360×196` nền `#F7F0E7`, viền `#8F6250`,
  radius trên 12px. Tab được chọn cao `40` radius 16 với hai lớp `#E4CCB1` (108) và viền nét
  đứt `#B69B7D` (102); tab thường cao `32` radius 12. Label Roboto Medium 14/20 `#725938`,
  icon 18px cùng màu;
- card grid ba cột: card room/food `104×122`, card pet `104×106` nền `#FFFEF9` viền `#FFECD4`
  2px radius 16; ô add `104×106` nền `#FFECD4` viền `#8F6250` với vòng tròn nét đứt `#D3BEA2`;
- pet đã sở hữu đi lại trong scene bằng `PetRoomWander`, không dùng `PetEngine`: engine overlay
  dựng cho góc nhìn ngang nên trọng lực dồn mọi pet về một đường sàn. Phòng nhìn từ phía trước
  nên sàn là hình thang phối cảnh `0.50–0.72` chiều cao scene, mép sau hẹp hơn 14%; pet chọn một
  điểm bất kỳ trên sàn, đi tới, nghỉ rồi đi tiếp. Pet ở xa vẽ nhỏ hơn (0,78–1,0) và thứ tự vẽ
  theo chiều sâu. Sprite pack vẽ mặt sang **trái**, nên đi sang phải mới lật gương — cùng quy
  ước với `PetSpriteTransformPolicy` của overlay;
- kiểu nghỉ (đứng/ngồi/nằm/nghịch/biểu cảm) chỉ được chọn trong số action pack thật sự có frame;
  ngồi và nằm kéo dài 4–9 giây, các kiểu khác 1,2–4,5 giây. Một frame clock chung tick mọi pet;
- pet nổi bị tắt khi vào My Pet Room và bật lại khi rời màn, vì phòng đã hiển thị chính các pet
  đó; chỉ khôi phục nếu overlay đang chạy lúc user vào phòng;
- card trong sheet giữ tỉ lệ Figma bằng `aspectRatio` (`104/106` cho pet và ô add, `104/122` cho
  food và room), và mọi kích thước bên trong card suy ra từ bề rộng card đã đo
  (`maxWidth / 104`) chứ không dùng sdp cố định — ô lưới rộng hơn 104px trên phần lớn thiết bị;
- food card: ảnh món `70` canh đáy vùng ảnh `104×90`, pill giá `32×18` nền `#FFF1B2` chữ
  `#A54905`, pill số lượng `26×18` nền `#8D6037` viền trắng bám góc dưới phải ảnh, nút `+` là
  vòng tròn `20×20` nền `#E1CCB9` viền nét đứt `#D3BEA2`, tên món Roboto Regular 12/16;
- room card selected dùng viền `#FB3675` 3px kèm badge check `28×28`; unselected viền `#FFECD4`
  2px. Card pet có nút xoá `16×16` tại `(8,8)` góc trên phải và xoá phải qua dialog xác nhận
  dựng theo `GrantPermissionDialog`;
- panel chi tiết pet thay body sheet: hàng back `24` + nhãn `Pet on screen` `#FB3675` + toggle
  `44×24`; khối info `336×78` với thumbnail `78×78` (ảnh `60×60`, băng dính `44×35`) và ba dòng
  label `#8F6250` 11/16 · value `#212327` 12/16 ngăn bằng divider nét đứt; khối Energy có chip
  `77×24` nền `#8F6250` và thanh `336×42`. Thanh dùng ba gradient theo mức: `#94DF37→#47B321`,
  `#FFDF50→#EDB90E`, `#FF4E4E→#BF3535`;
- Energy tụt 1%/phút kể cả khi app đóng và chỉ hồi khi cho ăn; food card tiêu một phần, nút `+`
  đưa về Pet Store vì đó là nơi nhận thêm food bằng Rewarded;
- chạm một pet trong scene mở đúng panel của pet đó; pet vẽ trên cùng thắng nên tap không mở
  nhầm con nằm dưới. Title bar đổi thành tên pet khi panel mở;
- nút music phát `res/raw/bgm_pet_room.ogg` lặp, persist trạng thái và chỉ phát khi màn đang
  resume; rời màn là pause, ViewModel bị clear thì release decoder;
- Swarm tạm ẩn khỏi navigation trong v1; `ui/home/HomeScreen` và `swarm_customization` giữ
  nguyên code cho bản sau.

My Pet mode contract (ẩn trong v1, giữ cho bản sau):

- `Pet Swarm` và `Mixed Mode` là segmented control loại trừ nhau;
- global switch điều khiển foreground overlay, không dùng để thay pet selection;
- Mixed hiển thị lưới 3 cột × tối đa 4 hàng cho 12 slot; pet đã cấu hình giữ card hiện
  tại, chỉ slot trống kế tiếp có thể thao tác để roster luôn liên tục;
- slot Mixed 1–3 miễn phí; slot 4–12 có trạng thái khóa và Catalog Rewarded gate. Earned
  callback mở đúng slot hiện tại khi ad hiển thị được; unavailable tiếp tục flow, còn
  dismiss sớm dừng lại. Premium bypass gate;
- Mixed dùng icon mắt trực tiếp trên từng card để hiện/ẩn ngay khi overlay đang chạy;
- không cho ẩn pet Mixed cuối cùng, vì global switch đã đảm nhiệm trường hợp không hiện pet;
- Swarm locked hiển thị CTA Rewarded và Premium; Premium bypass Rewarded;
- Swarm unlocked hiển thị một pack, stepper count và Change/Remove.
- Tap Swarm card mở `swarm_customization`; screen riêng giữ teal hierarchy của Home,
  identity card ở đầu, setup/movement sections và CTA Done cố định. Mọi slider/toggle
  persist và cập nhật runtime ngay, Done chỉ đóng màn chứ không phải bước commit.

Mỗi card phải cho user thấy nhanh character, size, speed và trạng thái tương tác; option
pet không được lặp ở app-wide Settings hoặc ghi vào global state.

## Resource rules

- User-facing text: `strings.xml`, key `<feature>_<purpose>`.
- Color: `colors.xml`, key `colors_<HEX>` trừ semantic theme token có chủ đích.
- Drawable: `ic_` cho icon, `ic_logo_` cho logo vector, `img_` cho bitmap.
- Asset từ Figma phải ưu tiên SVG và convert thành Android `VectorDrawable`; không export
  icon đơn giản thành PNG. Chỉ dùng bitmap khi node có image fill/raster, SVG không được hỗ
  trợ hoặc quá phức tạp để render ổn định trên Android; bitmap fallback phải export `PNG @3x`.
- Bitmap Figma đặt trong `drawable-nodpi` và luôn có kích thước hiển thị rõ trong Compose để
  Android không dùng kích thước pixel gốc làm layout size.
- Typography mặc định toàn app dùng `RobotoFontFamily` từ `ui/theme/Type.kt`, gồm Regular
  400, Medium 500, SemiBold 600 và Bold 700; mọi Material 3 text role đều phải kế thừa family
  này. Chỉ giữ font riêng cho artwork/promo đã được Figma chỉ định và lựa chọn font ngày giờ
  do user cấu hình. Native-ad typography tiếp tục thuộc design contract riêng của module ads.
- Font: tái sử dụng `res/font` và theme; không khai báo trùng trong từng component.
- Không hardcode string/hex color trong Composable.

## Native ad palette

- Native Ads dùng light surface theo Figma node `8047:2973`: nền `#FEFEFE`, viền và
  loading placeholder `#E6E6E6`, nội dung `#000000`.
- Badge `Ad` và CTA dùng gradient ngang `#FF5D7D` → `#FB54BB`; CTA giữ chữ trắng.
- Tất cả Native Ad template trong module `:ads` dùng chung palette này; thay đổi visual
  không được làm thay đổi placement, loading callback hoặc premium/ad-free policy.

## Sizing

Design hiện dùng SDP/SSP. Phải phân loại kích thước trước khi mapping:

- Kích thước cục bộ/cố định như padding, spacing, icon/image, height, radius và typography: `Android sdp/ssp ≈ Figma px ÷ 1.3`.
- Kích thước phụ thuộc viewport như chiều rộng dialog, bottom sheet hoặc card căn theo screen/frame: giữ tỷ lệ Figma `nodeWidth / frameWidth` và dùng `fillMaxWidth(fraction)`. Chỉ dùng `fillMaxHeight(fraction)` khi design xác định rõ tỷ lệ chiều cao theo viewport.

Dùng `dimensionResource` từ `com.intuit.sdp`/`com.intuit.ssp` cho nhóm kích thước cục bộ, làm tròn về resource gần nhất và đối chiếu screenshot. Ví dụ Rate dialog rộng `312px` trong frame `360px` dùng `fillMaxWidth(312f / 360f)`; dialog cảm ơn rộng `320px` dùng `fillMaxWidth(320f / 360f)`, không đổi thành `_240sdp`/`_246sdp`.

## Component hierarchy

- Screen: collect state, effect và wiring action.
- Section/component: stateless nếu có thể.
- Shared component chỉ đặt ở `ui/component` khi có ít nhất hai consumer hoặc có contract reusable rõ.
- Shared Home chrome (`HomeHeader`, `HomeEnableCard`, `HomeBottomNavigation`) do shell/feature
  gọi theo đúng ownership: screen sở hữu header/card, `AppNavGraph` sở hữu bottom navigation.
- Feature-only component giữ cạnh feature để tránh global component folder phình to.

## Modifier và interaction

```text
size → shadow → clip → background → border → clickable → padding
```

- `clip` trước `clickable` để ripple đúng shape.
- Không bọc icon trong Box chỉ để tạo padding nội bộ nếu drawable frame đã có viewBox chuẩn.
- Action icon có content description; decorative image dùng `null`.
- Touch target và visual size phải được cân bằng; khi cần pixel-match Figma vẫn phải đảm bảo accessibility.

## Figma implementation

1. Lấy screenshot và design context.
2. Phân tích hierarchy/alignment/spacing/color/type/radius/layer order.
3. So sánh với code hiện tại.
4. Mapping token vào resource.
5. Implement theo state contract, không nhét logic vào UI.
6. Preview/screenshot và compile verify.

Dialog xin quyền dùng custom Compose card theo Figma thay vì `AlertDialog` mặc định. Golden
image của component được render host-side từ `screenshotTest`, không cần khởi động AVD.
