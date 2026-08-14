# 06 — UI Design System Contract

Base giữ infrastructure/theme và component pattern. Product screens từ Home trở đi dùng
visual system Cute Pet. Splash, App Open Welcome Back và Language đã theo Figma;
Intro và Premium vẫn giữ UI hiện tại. Permission giữ nguyên UI/source nhưng tạm không nằm trong
flow onboarding cho tới khi policy first-permission được bật lại.

## Cute Pet product direction

- Discover Home và Mine dùng nền trắng/gradient pastel, primary pink `#FB3675`, card trắng
  và typography Roboto theo Figma. My Pet mode dùng nền xanh-trắng nhẹ, primary teal và
  segmented control rõ selection; Catalog vẫn dùng cozy palette hiện tại.
- Pet thumbnail thật là visual chính; icon notification chỉ là fallback khi pack chưa có ảnh.
- Corner radius lớn 16–24 sdp, card rõ hierarchy nhưng ít chrome và không dùng dark utility
  dashboard cho product screens.
- Discover là landing tổng hợp; My Pet là pet room và session control; Catalog ưu tiên
  discovery bằng grid; Mine là app/support hub; Customize biểu diễn một hồ sơ pet độc lập.
- Shared primitives nằm ở `ui/shared/component/CutePetComponents.kt`; component dark cũ không được
  dùng cho product screen mới nếu không có lý do tương thích.

Intro và Premium cố ý chưa đổi trong refresh hiện tại. Permission hiện là destination inactive.

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
  pink + tick trắng. Shadow dùng `#666666` 40% với elevation `12sdp` để tách card rõ hơn
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
- Rate feedback dialog dùng IME-aware window: khi chọn `Other`, ô nhập tự focus và bàn phím
  làm co vùng dialog thay vì che dialog. Nếu chiều cao còn lại không đủ, chỉ danh sách option
  cuộn bên trong card; title, ô nhập `Other`, `Send` và `Maybe later` luôn ghim cố định. Dialog
  giữ khoảng cách với cạnh trên và bàn phím;
- toggle điều khiển cùng `BatteryStatusConfig.enabled` và Accessibility gate với Discover;
  Language, Rate, Share, Contact và Privacy giữ flow thật. My Pet mở route `my_pet`,
- Favourite & Recent là destination riêng: hai tab cố định, lưới ba cột dùng favorite battery
  state thật và native ad cố định cuối màn. Recent giữ empty state cho tới khi product định nghĩa
  action tạo lịch sử, thứ tự MRU và giới hạn retention; Apps that hide icons giữ callback chờ
  feature riêng.
- Favourite & Recent dùng một Material large app bar `exitUntilCollapsed` cho cả hai tab và mọi
  trạng thái dữ liệu: title 24px khi expanded, chuyển thành 20px inline khi collapsed. Tab bar
  luôn nằm ngay dưới app bar và cả empty state lẫn grid đều phát nested scroll; header không còn
  bị khóa expanded/collapsed dựa trên việc tab có item hay không. Icon back/heart/history và empty
  illustration giữ vector; thumbnail theme tiếp tục load từ battery catalog thay vì đóng gói sáu
  bitmap demo.

Discover Home contract:

- route `home` là root sau onboarding và hiển thị dữ liệu thật từ owner/battery catalog;
- toggle chính điều khiển `BatteryStatusConfig.enabled`, có disclosure và Accessibility gate;
  Discover và Battery cùng tính trạng thái switch từ config đã lưu + trạng thái Accessibility
  hiện tại mỗi lần app resume, nên việc cấp hoặc thu hồi quyền không làm hai tab lệch nhau;
- Home shell có bốn tab Discover/Battery/Shimeji Pets/Mine. `HomeBottomNavigation` cố định
  trên bottom banner hiện có; từng screen không tự tạo lại bottom chrome. Battery selected dùng
  glyph filled 24×24 export từ frame Figma `8017:3666`, không tái sử dụng icon outline unselected;
- Discover, Shimeji Pets và Mine dùng chung `HomeHeader` và `HomeEnableCard`: header `43sdp`, search
  `25sdp`, enable card `37sdp`, switch `34×18sdp`. Discover chỉ render Battery enable card;
  pet switch được quản lý ở flow pet. Không copy component rồi đổi metric riêng;
- Discover hero dùng composite `Battery Troll` PNG @3x tại tỉ lệ `328×100px`; banner thấp hơn
  dùng placement SDK thật `discover_inline`, không đóng gói creative quảng cáo mẫu;
- Thứ tự nhóm đầu sau hero là `Trending Emoji Battery` → `Shimeji Pets` → inline banner;
  `Status bar themes` tiếp tục nằm ngay sau banner, đúng hierarchy node Figma `8015:1035`;
- Trending Emoji Battery và Shimeji Pets lấy ranking lần lượt từ
  `BatteryCatalogSnapshot.trendingEmojiThemeIds` và `OwnerPetCatalogSnapshot.trendingPetIds`;
  catalog cũ dùng fallback product đi kèm app. UI giữ đúng thứ tự, lấy toàn bộ item tìm thấy và
  bỏ qua ID chưa tồn tại/asset chưa sẵn sàng thay vì tự chèn item khác. Battery Themes vẫn dùng
  favorite state thật.
- Search field theo Figma `8287:6560`: khi query khác rỗng hiện clear icon 16px ở trailing;
  clear chỉ xoá query và giữ focus, còn tap ngắn ngoài toàn bộ field mới clear focus + ẩn IME.
  Gesture cuộn không được xem là outside tap.
- Discover section Emoji/DIY Battery theo Figma `8019:1628` và `8019:1689` dùng text thuần
  `Emoji`/`DIY Battery` cùng artwork 16px thật ở leading; không ghép emoji vào string.

Language loading contract theo Figma `8421:9356`:

- loading phủ scrim đen `60%` toàn màn và dùng text trắng; Android 12/API 31 trở lên blur
  content phía sau `8dp`, API thấp hơn không dùng blur và chỉ giữ scrim tối tương đương Figma.

Battery catalog contract theo Figma `8102:2729` và `8286:5017`:

- category có slug `trending` dùng chung ordered `trendingEmojiThemeIds` với section Trending
  Emoji Battery của Discover ở cả landing lẫn màn More; không lọc lại theo `categoryId` cũ.
  Category khác tiếp tục giữ membership và thứ tự theme từ catalog;
- landing dùng shared `HomeHeader`/`HomeEnableCard`, promo composite PNG @3x, category section
  dạng carousel và card `110×110px`; preview runtime dùng chung tỉ lệ `65%` và
  `ContentScale.Fit` của Discover vì asset dữ liệu thật có bounds khác mock Figma, favorite ở
  top-end, crown Premium ở top-start;
- crown Premium của Discover, Shimeji Pets, Search và Battery dùng chung `PetPremiumBadge`: nền
  `#FFEA89` 50% và crown artwork theo tỉ lệ `18/24`, không dựng badge riêng theo từng feature.
  Search hiện crown khi theme có entitlement Premium, user chưa Premium và ID chưa được mở bằng
  reward; entitlement được đọc lại khi Search resume;
- hai nhóm Trending trên Discover render toàn bộ ranking curated; các nhóm Status Bar, Emoji và
  Battery component còn lại lấy tối đa 16 preview. Shimeji Pets dùng một hàng ngang; Battery,
  Status Bar, Emoji và Battery component dùng các cột hai item trong `LazyRow`, vì vậy danh sách
  luôn có thể kéo ngang mà không dựng hoặc tải trước toàn bộ catalog;
- category header dùng trực tiếp emoji ở đầu `category.name`; không ghép thêm drawable/icon riêng
  để tránh hiển thị trùng khi catalog cập nhật tên category;
- More mở child route có header Back/title, inline banner SDK thật và grid ba cột. PRO action được
  ẩn tập trung bởi `PremiumUiPolicy` trong v1. Card detail giữ
  tỷ lệ vuông, preview `74/101.333`, selected dùng `#FFEBF1` + stroke `#FB3675`, không hiện heart;
- inline banner chỉ giữ grid item khi placement đủ điều kiện và chưa load fail. Khi SDK/config/
  ad-free policy không cho hiển thị hoặc load fail, xóa cả holder lẫn grid item để card đầu tiên
  dồn lên ngay dưới header, không để lại vùng trắng;
- landing giữ Home bottom navigation; detail ẩn navigation nhưng giữ cùng bottom banner holder.
  DIY FAB và Lottie star bling dùng lại component Discover.

Customize Status Bar theo Figma `8227:4332`, `8345:6256`, `8240:7335`, `8240:7466`,
`8227:6510`, `8155:4852`, `8345:6797`, `8227:6044`, `8240:8590`, `8345:7719`:

- overview dùng Material large app bar `exitUntilCollapsed`: title lớn khi expanded và title
  inline khi collapsed; Back luôn pinned, còn PRO action tạm ẩn trên toàn bộ app bar v1;
- preview `328×50px` được ghim ngay dưới app bar khi Accessibility chưa cấp hoặc feature đang
  tắt. Khi Accessibility đã cấp và status bar thật đang hoạt động, preview nhúng được ẩn để
  tránh hiển thị trùng. Khi preview nhúng đang hiện, Charge/Airplane/Ringer/Hotspot lấy trạng
  thái hệ thống thật; riêng màn option đang chỉnh sẽ mô phỏng trạng thái active để user luôn
  nhìn thấy style vừa chọn. Preview dùng cùng thứ tự vật lý với renderer thật: trailing LTR là
  Hotspot → Signal/Data → Wifi → Percentage → Battery/Emoji pair → Charge; Battery và Emoji
  chồng cùng tâm và mọi component cách nhau 4dp;
- More Battery/Emoji mở grid ba cột với artwork 73.03% item; More Theme mở grid hai cột từ
  background catalog runtime. Wallpaper của overview và mọi child phủ toàn viewport bằng crop
  căn top; không để lộ nền app legacy màu tối ở đáy trên thiết bị có tỷ lệ màn hình cao hơn
  frame Figma 360×800;
- hàng Battery/Emoji ở overview và grid More ưu tiên theme theo ordered
  `trendingEmojiThemeIds`; theme còn lại tiếp tục theo catalog order để editor vẫn cho phép chọn
  toàn bộ library. ID ranking thiếu bị bỏ qua và built-in ID `0` không xuất hiện;
- Animation/Wi-Fi/Signal/Mobile Data dùng chung shell hồng, preview sticky,
  switch, slider/color/style grid và native collapsible với sáu option screen còn lại. Tất cả
  child giữ chung draft; mỗi thay đổi cập nhật preview ngay và Back giữ nguyên draft để overview
  phản ánh lựa chọn mới;
- Grid Animation lazy theo từng hàng bốn item để chỉ chạy animation đang nằm trong viewport.
  Lottie server render từ file cache đã materialize; khi đang tải/parse hoặc gặp lỗi, card dùng
  illustration Animation thay vì để trống;
- Hàng Theme ở overview hiển thị trước năm background. Nếu background đang chọn nằm ngoài năm
  item đầu, hàng giữ bốn item đầu và đưa item đang chọn vào vị trí thứ năm;
- Background Color và Theme là hai mode loại trừ nhau: chọn Color đặt decoration ID về `0` và
  chỉ hiện viền selected ở palette; chọn Theme chỉ hiện selected ở theme, không tiếp tục tô màu
  nền phía dưới asset;
- Emotion dùng màn pack theo Figma `8404:6277`: nhóm Classic giữ 20 item cũ và tám card mới,
  mỗi card preview 5×2 item; chạm pack/item
  mở detail `8404:7179` với slider Size, switch và grid ba cột. Khi cần preview
  nhúng, nó được ghim dưới top bar ở cả hai màn. 80 frame art là PNG @3x vì nguồn Figma là raster/image-fill;
  card, selected stroke, shadow và background được dựng bằng Compose.
- Emotion pack và detail dùng cùng native collapsible holder ở app shell. Lần đầu vào flow sẽ
  request/bind native; chuyển pack → detail giữ nguyên Activity-scoped key nên không reload ad.
- Template có đủ Battery, Emoji và Animation; header dùng icon Figma 16px và chevron vector
  14px. Color có custom wheel, bảy preset và ba theme preview; custom wheel mở trực tiếp một
  chế độ HSV/opacity duy nhất, không dùng segmented Grid/Sliders, và cập nhật liên tục cùng
  draft/live-preview policy;
- Picker Battery/Emoji chỉ hiển thị theme catalog thật; built-in ID `0` là fallback renderer
  và không tạo card placeholder. Mặc định chọn theme ID `1`; placeholder tải/lỗi dùng icon
  Battery/Emoji có độ tương phản đúng tông thay vì asset xám/trắng cũ;
- card dùng shadow token `#6666661F`, y=8, blur=24; slider dùng Roboto Medium 16/24 cho
  label và 14/20 cho value. Slider dùng Material 3 interaction/semantics giống Pet Settings,
  track hồng không tick và vùng điều khiển cao 48px theo Figma; không tự vẽ thumb dạng thanh.
  Mười icon Customize đều dùng VectorDrawable Figma màu `#FB3675`;
- toàn màn dùng Roboto local đúng weight: top bar/section/action là SemiBold 600, row/slider/grid
  là Medium 500 và More là Regular 400. Top bar collapsed 20/28, expanded 24/32; không dùng
  SansSerif synthetic hoặc Roboto Condensed để giả SemiBold;
- Các editor con dùng chung `AppSwitch` (ON hồng, OFF xám, thumb trắng), không có footer/CTA
  Done. Control cập nhật draft và preview ngay; chỉ CTA Apply của overview mới persist cấu hình;
- Back ở overview khi draft chưa Apply mở discard sheet full-width theo node `8345:7719`:
  Cancel giữ draft, Exit restore config đã lưu rồi pop; sheet dùng native `HEIGHT_222`.

Pet Store visual contract:

- action toast theo Figma node `8080:3070` là pill trắng wrap theo nội dung và không vượt quá
  `305/360` viewport, padding
  `12×8px`, thumbnail pet/food thật `24×24px`, text Roboto Regular 14/20 và action Roboto
  Medium 14/20. Shadow dùng `#666666` 40%, blur 12px; nội dung dài chỉ ellipsize phần message,
  không được đẩy hoặc cắt action `View`. Toast tự đóng sau ba giây;
- Pet Home hero dùng nguyên node Figma `8634:9046` dưới dạng PNG @3x `1002×378`. Frame nominal
  là `328×120px`, nhưng stroke 3px tràn ra ngoài tạo painted bounds `334×126px`; Compose đặt theo
  painted bounds tại x=13px và giữ aspect ratio, không ép bitmap vào frame nominal hoặc clip thêm.
  Chạm toàn bộ banner mở My Pet Room;
- Pet/Food selector theo Figma `8075:1848`: cả hai state dùng nền `#FFEBF1`, chữ Roboto Medium
  `#FB3675` và giữ icon raster có màu. Selected thêm viền hồng 1px; unselected không viền,
  không chuyển nền/chữ/icon sang xám. Tab cao 44px, radius 12px, chia đều trong row gap 8px;
- pet category theo Figma node `8287:4824` là `LazyRow` cao 40px, padding ngang 16px và gap
  20px. Category lấy động từ catalog, loại trùng không phân biệt hoa thường và giữ thứ tự server;
  active dùng Roboto Medium 14/20 màu `#FB3675` kèm underline, inactive dùng Roboto Regular
  `#212327`. Label đo theo full content width, không dùng minimum intrinsic width vì tên nhiều từ
  sẽ bị cắt; category được chọn tự cuộn vào viewport đầy đủ. Chọn category chỉ lọc pet grid,
  không làm thay đổi trạng thái unlock;
- pet card giữ tỷ lệ `104/142`, image area `104/90`, thumbnail theo tỷ lệ item và crown
  premium 20px tại top-end;
- food card giữ tỷ lệ `104/122`, image area `104/90`, artwork `70/104`; badge giá tại
  `(6,6)` dùng coin artwork PNG @3x, badge số lượng tại `(62,66)`, title Roboto 12/16;
- reward sheet dùng Roboto Medium cho title và action. V1 ẩn action `Unlimited` cùng crown và
  để CTA xem Rewarded gradient `#C95DFF → #FB54BB` chiếm toàn bộ chiều ngang; policy dùng chung
  cho Pet, Food, Battery và Battery Troll. Selected Pet Store, video và tape giữ asset vector gốc. Pet Store và
  Battery dùng chung `RewardOfferSheet` full-width; Battery có preview `110×110px` và native
  slot `336×222px` theo Figma `8145:4924`.
  Trong lúc pet pack đang download/verify, preview pet phủ scrim 28% + progress trắng, CTA đổi
  sang `Downloading…`, và mọi action/dismiss unlock bị khóa để không tạo request trùng.
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
- **nợ đã biết**: onboarding Permission (`ui/onboarding/permission/PermissionScreen`)
  vẫn còn nền `#161718`, nên icon tối chìm trên màn đó cho tới khi UI được
  dựng lại theo tông trắng. Language đã dùng nền trắng; Splash/Welcome Back dùng wallpaper pastel và Intro dùng
  ba hero composite pastel của Intro sáng nên đọc bình thường.

Intro onboarding theo Figma nodes `8088:13113`, `8088:13148`, `8088:13201`:

- mỗi trang dùng một asset composite lossless @3x (`img_intro1/2/3.webp`) export từ parent
  `Mask group`; asset chỉ chứa phone mockup, character, glow, overlay và fade phức tạp;
- title, indicator, Next/Start, action button và native ad vẫn là UI thật. Không export toàn
  frame vì sẽ bake status bar iPhone, text và ad creative mẫu vào bitmap;
- riêng page 2 dùng Next outline theo node `8446:11010`: width bằng `320/360` viewport,
  nền trắng, viền `#FF5D7D` 2px và text Roboto Medium `20/28` màu `#FB3675`;
- page 1/3 giữ placement `SCREEN_INTRO`/`SCREEN_INTRO_SECOND` cao 222; page 2 không có ad.
  Pager, analytics và completion flow không thay đổi.

Overlay permission disclosure theo Figma node `8436:5998`:

- `ui/shared/component/OverlayPermissionDialog.kt` là bottom sheet dùng chung trước mọi request
  `ACTION_MANAGE_OVERLAY_PERMISSION`; scrim/Back/`Not now` chỉ đóng sheet. `Allow Access` từ
  onboarding/Discover/Pet Store mở biến thể overlay của Grant Permissions; CTA `Go to Settings`
  tại đó mới mở system special-access settings. Swipe chỉ dismiss sau khi kéo tối thiểu 25% chiều
  cao sheet;
- hero `img_overlay_permission_hero.png` được export nguyên group 158×100 để giữ phone mockup,
  pets và bubble; title/body là text thật, action dùng shared gradient/outline buttons;
- native `HEIGHT_222` nằm sát đáy và collapse khi placement không render.

Switch dùng chung toàn app theo Figma node `8080:7307` (bật) và `8080:7343` (tắt):

- `ui/shared/component/AppSwitch.kt` là switch duy nhất của app; mọi toggle dùng nó, không màn nào
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

Grant Permissions contract theo dashboard Mine Figma node `8080:7477`, card nền tảng node
`8080:9754` và biến thể Pet on Screen `8591:7325`:

- option Grant Permissions trong Mine được bật và mở dashboard trạng thái quyền. Nền **trắng
  phẳng** — lớp wallpaper trong design bị tắt,
  card trắng nổi lên bằng shadow chứ không bằng đổi màu nền;
- app bar dùng `LargeTopAppBar` và `exitUntilCollapsed`: trạng thái mở có back + title lớn
  `Grant Permissions`, khi list cuộn thì title thu về toolbar một dòng giống Customize Status
  Bar/Accessibility How to use; native ad vẫn ghim đáy, ngoài list;
- dashboard Mine có hai nhóm `Battery Emoji` và `Pet on Screen`: giữ lại các quyền áp dụng trên
  thiết bị dù đã cấp hay chưa để user đọc và quản lý trạng thái. Battery Optimization chỉ được
  xem là áp dụng nếu restriction/vendor policy xác nhận ROM cần nó; trạng thái grant đơn lẻ trên
  stock Android không làm row xuất hiện. Biến thể Pet on Screen có hai nhóm: Necessary
  chứa Draw over other apps + Notification access; Enhanced Stability chứa Ignore Battery
  Optimization + Auto Start. Khoảng cách heading → card và card → card là 12px, giữa hai nhóm
  là 20px;
- card trắng `328×?` radius 16, padding trong 16, shadow `DROP_SHADOW r=9 offset=(0,0) a=0.17`.
  Compose đổ bóng xuống dưới theo elevation chứ không blur đều như Figma, nên copy đúng alpha
  thì card trắng trên nền trắng gần như mất viền — dùng `shadow(_8sdp, #212327 alpha 0.30)`
  để bóng đọc được; icon quyền `34×34` radius 10 với gradient riêng
  từng quyền, cách text 8px, text cách switch 8px;
- card bắt buộc đổi theo entry point. Mine dùng Accessibility và minh hoạ hai bước; flow Pet on
  Screen dùng artwork composite lossless @3x `img_grant_permission_pet_on_screen.png`
  (`474×300`, render `158×100`) export từ node `8606:8690`, thay cho ảnh cũ bị mờ/lệch. Icon
  Draw over apps dùng đúng node `8591:7226`: glyph hai ô vuông chồng nhau `22×22` trên
  container `34×34`, radius 10, gradient `#15EDB8 → #0EB7AD`. Badge `Required` dùng
  `#FFECEC`/`#F04438`, `Allowed` dùng
  `#E6F9EF`/`#00C062` (Roboto 500 10/14, padding 10×4), minh hoạ hai bước và CTA
  `Go to Settings` gradient `#C95DFF → #FB54BB` cao 40; các card còn lại dùng `AppSwitch`
  chung với Home;
- ở biến thể Pet on Screen, hero `Allow Pets to Show on Screen` chính là item Draw over other
  apps nên không render thêm row Overlay trùng lặp. Khi Overlay đã cấp nhưng Notification còn
  thiếu, hero biến mất và chỉ còn row Notification;
- Auto Start dùng đúng node `8593:7617`: container `34×34`, radius 10, gradient
  `#8580FD → #615AD9`; glyph energy-saving leaf `22×22` export từ node `8606:8531`, không dùng
  lại icon Battery Optimization;
- CTA Pet on Screen điều phối system surface đúng thứ tự từ trên xuống: Overlay → Notification
  → Battery Optimization (nếu có ý nghĩa) → Auto Start (nếu ROM có màn tương ứng). Hai bước
  đầu là bắt buộc; ngay khi cả hai được cấp, pet foreground service được start, còn hai bước ổn
  định là optional và không chặn pet. Dưới API 33 không có runtime notification permission nên
  bước Notification được xem là đã thỏa;
- ảnh minh hoạ hai bước export theo **render bounds `296×96`**, không phải layout bounds
  `296×92`: hai khung điện thoại tràn khỏi frame 4px, export theo layout bounds thì đáy bị
  cắt. Compose dựng bằng `aspectRatio(296f/96f)`;
- riêng biến thể Pet on Screen chỉ render permission còn thiếu và thật sự tồn tại/có ý nghĩa
  trên thiết bị: Overlay hoặc Notification đã cấp sẽ biến mất; Notification không tồn tại dưới API 33;
  Battery Optimization đã exemption hoặc không có restriction signal cũng không hiện. Section
  không còn row sẽ được ẩn cùng heading;
- Notification là option đầu tiên ở cả dashboard Mine và biến thể Pet on Screen. Chuỗi xin quyền
  Pet cũng theo đúng thứ tự UI: Notification → Overlay → Battery Optimization → Auto Start;
- Ignore Battery Optimization chưa cấp sẽ thử mở dialog package-scoped bằng
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` trước; ROM không có activity tương ứng thì
  fallback sang `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, cuối cùng mới tới App Details.
  Nếu dashboard đọc được trạng thái đã cấp, chạm row mở danh sách hệ thống để user có thể quản lý
  hoặc thu hồi thay vì gọi lại request dialog.
  Manifest vì vậy khai báo `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Đây là request chịu policy
  Google Play: trước release phải xác nhận Pet on Screen là core function bị ảnh hưởng và phần
  store listing/declaration mô tả đúng use case;
- row Ignore Battery Optimization **chỉ hiện khi exemption thật sự làm được gì**. Trên máy
  không giết foreground service nó vô nghĩa với app này: exemption chỉ mở network + partial
  wake lock trong Doze, mà `PetOverlayService` không dùng cả hai và còn tự `pauseRendering()`
  khi `ACTION_SCREEN_OFF`; Doze cũng không dừng một foreground service.
  `PetBackgroundRestrictionReader` đọc ba tín hiệu, `PetBatteryOptimizationPolicy` quyết định:
  `isBackgroundRestricted()` (API 28+), standby bucket `RESTRICTED` (API 30+),
  `getHistoricalProcessExitReasons()` cho thấy process từng chết lúc đang chạy foreground
  service vì `REASON_SIGNALED`/`LOW_MEMORY`/`OTHER` (API 30+). Không fallback theo API level,
  manufacturer/brand hoặc sự tồn tại của vendor settings: các dữ liệu đó không chứng minh Android
  battery exemption là cần thiết. Samsung SM-J730G API 28 mặc định vì thế không hiện row; API 28
  chỉ đóng góp `isBackgroundRestricted()` khi platform thực sự trả `true`. Trong flow Pet, row
  biến mất sau khi grant; trong dashboard Mine, row còn lại trên đúng thiết bị đó để phản ánh
  trạng thái và cho phép quản lý;
- native ad ghim cố định dưới cùng màn, ngoài `LazyColumn`, nên nó không cuộn cùng danh sách;
- row **Allow auto-start** hiện khi ROM có allowlist riêng của hãng (`PetVendorPowerSettings`
  resolve component qua `PackageManager`, package khai trong `<queries>` để API 30+ nhìn thấy).
  Candidate chỉ gồm activity có semantics explicit như AutoStart/Startup/Protect/ChainLaunch;
  generic Samsung Battery, ASUS Device Manager, HTC landing page và Evenwell battery page bị loại
  vì tồn tại activity không đồng nghĩa thiết bị có một quyền Auto Start cần user chấp thuận.
  Đây là ask tách biệt với battery exemption: cấp cái này không cấp cái kia, nên row vẫn hiện
  ngay cả khi user đã cấp exemption. Không API nào đọc được trạng thái allowlist, nên row dùng
  mũi tên thay switch và intent được resolve lại đúng lúc chạm. Vì không đọc được grant state,
  đây là row duy nhất không thể tự ẩn chính xác sau khi user quay lại;
- runtime Notification prompt không còn phụ thuộc onboarding Permission đang tắt: trên API 33+
  Home shell hỏi đúng một lần khi top-level Home đầu tiên xuất hiện, chỉ sau khi full-screen ad đã
  đóng. DataStore giữ việc prompt đã từng hiển thị; nếu user từ chối, thao tác tại dashboard mở
  App Notification Settings thay vì spam lại prompt mỗi lần vào Home;
- disclosure Accessibility (`GrantPermissionDialog`) theo Figma `8437:7570` và `8437:9099`
  là bottom sheet full-width bo hai góc trên 24px, scrim 50%, handle `32×4`, title Roboto
  SemiBold 18/26 và body Roboto Regular 14/20. Nội dung dài là phần duy nhất được cuộn; hàng
  consent, nút `Allow`/`Close` và native ad luôn cố định. Checkbox dùng đúng hai vector của rate
  flow: frame luôn là `20×20`, phần nền `16.25×16.25` nằm giữa với inset `1.875` và tick giữ
  nguyên offset Figma để không lệch hoặc nhảy khi đổi state. Back, scrim và `Close` đóng ngay,
  còn swipe phải kéo tối thiểu 25% chiều cao sheet;
  `Allow` chỉ chuyển sang màn `Accessibility How to use` sau khi user đã tick consent;
- state `Allow` theo Figma `8437:7772`/`8437:9110`: enabled dùng gradient
  `#C95DFF → #FB54BB` opacity 100%; disabled giữ nguyên gradient/chữ trắng với opacity 30%.
  Disabled vẫn nhận tap để hiện toast yêu cầu đồng ý điều khoản, nhưng không mở Accessibility
  Settings;
- toàn bộ Discover, Battery Catalog, Mine, Status Bar Editor và Grant Permissions dùng chung
  disclosure này. Khi quyền đã bật, row Accessibility trong Grant Permissions mở thẳng Settings
  để user quản lý/tắt quyền, không hỏi consent lại.
- màn `Accessibility How to use` theo Figma `8442:9525`: wallpaper pastel riêng, bốn bước với
  ảnh thao tác export theo từng group, số thứ tự hồng, keyword hồng và CTA gradient
  `#C95DFF → #FB54BB` ghim đáy. App bar dùng cùng `exitUntilCollapsed` contract của Customize
  Status Bar. CTA mở Settings; chỉ khi service đã bật mới pop về source để source tiếp tục action.

Input text contract:

- mọi Compose `BasicTextField` và Material `TextField`/`OutlinedTextField` dùng cursor
  `#FB3675`; Android theme cũng đặt `colorAccent` cùng màu để native input không rơi về accent
  tím hoặc màu mặc định của hệ thống;
- dialog đặt tên pet hiển thị tên gợi ý ban đầu bằng `#6F7073`; sau lần chỉnh sửa đầu tiên text
  chuyển sang màu chính `#212327`. Placeholder rỗng dùng `#9B9C9E`, vì vậy vẫn phân biệt rõ
  suggestion, text user nhập và placeholder.

My Pet Room contract theo Figma node `8177:3972`, `8185:4332`, `8185:4379`, `8191:5950`:

- feedback trong Pet Room dùng chung Compose overlay, không dùng Android system toast. Theo node
  `8634:7995`, pill wrap theo nội dung và không vượt quá `272/360` viewport, nền `#F7F0E7`,
  viền `#725938` 1px, shadow item `#6666661F`, padding `12×10px` và Roboto Medium 14/20.
  Message food dùng icon thức ăn `18×18px`, message khác dùng icon thông tin; nội dung dài tối đa
  hai dòng. Pill nằm ngay trên sheet ở cả trạng thái mở/thu và tự đóng sau ba giây;
- route `my_pet` là scene phòng full-screen, background lấy từ room catalog (`bg/BG_<id>.webp`)
  và vẽ `ContentScale.Crop`; không dùng Home chrome, không có bottom navigation;
- top bar `360×64` gồm back `32×32`, biển gỗ `178×40` (`img_pet_room_sign`) mang title, và
  Settings `32×32`. Music toggle `32×32` hai state nằm ngay dưới top bar ở mép phải. Mọi
  action là nút trắng bo góc `32×32`, icon canh giữa;
- shortcut Pet Store `50×68` nằm dưới Music ở mép phải; chevron `32×32` ngay trên sheet thu/mở
  sheet và xoay 180° khi đã thu;
- sheet `360×236` gồm tab strip full width chia đều ba phần My Pet/Food/Room và body `360×196`
  nền `#F7F0E7`, viền `#8F6250`,
  radius trên 12px. Tab được chọn cao `40` radius 16 với hai lớp `#E4CCB1` (108) và viền nét
  đứt `#B69B7D` (102); tab thường cao `32` radius 12. Label Roboto Medium 14/20 `#725938`,
  icon 18px cùng màu;
- card grid ba cột: card room/food `104×122`, card pet `104×106` nền `#FFFEF9` viền `#FFECD4`
  2px radius 16; ô add `104×106` nền `#FFECD4` viền `#8F6250` với vòng tròn nét đứt `#D3BEA2`;
- pet luôn hiển thị pill trạng thái ở góc trên trái card: `Active` dùng nền `#FFF1B2`, chữ
  Roboto Medium 8/12 `#A54905`; `Inactive` dùng nền `#F0F0F0`, chữ `#6F7073`. Cả hai giữ
  padding ngang 6px/dọc 2px và offset 6px;
- roster vẫn liệt kê mọi pet đã sở hữu, nhưng scene phòng chỉ dựng pet `Active`. Đổi switch
  cập nhật cả overlay slot và scene; `Inactive` biến mất khỏi sảnh nhưng vẫn có card để bật lại;
- shortcut Shimeji Pets và ô `+` pet mở đúng tab Pets của top-level Shimeji Pets. Nút `+` trên
  từng food không điều hướng khỏi phòng: nó mở `dialog_food_reward` cho đúng món, chạy
  Rewarded/Premium gate, cộng inventory rồi hiển thị reveal và toast xác nhận ngay tại Pet Room;
- bật Pet on Screen khi chưa sở hữu pet hoặc khi toàn bộ pet Inactive hiển thị dialog riêng. CTA
  lần lượt dẫn tới tab Pets hoặc My Pet Room; permission flow chỉ chạy khi có pet Active;
- panel detail và tên pet trên app bar chỉ hiển thị ở tab My Pet. Pet được chọn vẫn được giữ khi
  chuyển sang Food để mọi card food feed đúng pet đó; tab Room render độc lập, không bị detail
  che và cũng không xóa selection. Chỉ action Back trong detail hoặc xóa chính pet mới bỏ chọn;
- pet Active đang được chọn có focus arrow theo Figma node `8698:6492`, export PNG @3x với nền
  trong suốt để giữ gradient, inner shadow và drop shadow. Painted asset `56×59px` render khoảng
  `43×46sdp`, canh giữa và chạm ngay phía trên sprite; arrow được vẽ trong cùng scene Canvas nên
  đi theo pet khi pet wander và vẫn chỉ đúng target khi user chuyển sang Food;
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
  2px. Room chưa cache có icon download; ngay khi tap, chỉ card đó phủ scrim tối + progress
  indeterminate và khóa double tap cho tới khi background được cache rồi selected. Download lỗi
  trả card về icon download và không đổi room hiện tại. Card pet có nút xoá `16×16` tại `(8,8)`
  góc trên phải và xoá phải qua dialog xác nhận dựng theo `GrantPermissionDialog`;
- panel chi tiết pet thay body sheet: hàng back `24` + nhãn động `Active` màu `#FB3675` hoặc
  `Inactive` màu `#6F7073` + toggle
  `44×24`; khối info `336×78` với thumbnail `78×78` (ảnh `60×60`, băng dính `44×35`) và ba dòng
  label `#8F6250` 11/16 · value `#212327` 12/16 ngăn bằng divider nét đứt; khối Energy có chip
  `77×24` nền `#8F6250` và thanh `336×42`. Thanh dùng ba gradient theo mức: `#94DF37→#47B321`,
  `#FFDF50→#EDB90E`, `#FF4E4E→#BF3535`;
- tắt pet `Active` cuối cùng không ghi state `Inactive`; app giữ switch ON và hiện dialog giải
  thích Pet on Screen cần tối thiểu một pet. Energy không khóa thao tác Active/Inactive. Khi đủ
  12 slot đã gán, bật pet chưa có slot hiển thị toast thay vì im lặng;
- Energy tụt 1%/phút kể cả khi app đóng và chỉ hồi khi cho ăn. Pet chưa có record thức ăn dùng
  thời điểm nhận nuôi làm mốc 100%, không dùng thời điểm UI vừa đọc; khi panel đang mở, ticker
  cập nhật giá trị mỗi giây để mốc phút mới hiện ngay. Tap thân Food card tiêu một phần cho pet
  đang chọn; nút `+` mở reward flow tại chỗ. Trong lúc rewarded fullscreen, lifecycle Pet Room
  không khởi động lại overlay pet trên quảng cáo; callback lặp/double tap không thể cộng hai lần;
- chạm một pet trong scene mở đúng panel của pet đó; pet vẽ trên cùng thắng nên tap không mở
  nhầm con nằm dưới. Title bar đổi thành tên pet khi panel mở;
- nút music phát `res/raw/bgm_pet_room.ogg` lặp, persist trạng thái và chỉ phát khi màn đang
  resume; rời màn là pause, ViewModel bị clear thì release decoder;
## Resource rules

- User-facing text: `strings.xml`, key `<feature>_<purpose>`.
- Color: `colors.xml`, key `colors_<HEX>` trừ semantic theme token có chủ đích.
- Drawable: `ic_` cho icon, `ic_logo_` cho logo vector, `img_` cho bitmap.
- Asset từ Figma phải ưu tiên SVG và convert thành Android `VectorDrawable`; không export
  icon đơn giản thành PNG. Chỉ dùng bitmap khi node có image fill/raster, SVG không được hỗ
  trợ hoặc quá phức tạp để render ổn định trên Android; bitmap fallback phải export `PNG @3x`.
- Bitmap Figma đặt trong `drawable-nodpi` và luôn có kích thước hiển thị rõ trong Compose để
  Android không dùng kích thước pixel gốc làm layout size.
- Typography mặc định toàn app dùng `RobotoFontFamily` từ `ui/shared/theme/Type.kt`, gồm Regular
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
- Shared component chỉ đặt ở `ui/shared/component` khi có ít nhất hai consumer hoặc có contract reusable rõ.
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
