# 07 — Ads and Premium Integration

## Boundary

Module `:ads` sở hữu SDK integration, remote config, ad loading và ad UI/utilities. Feature trong `:app` chỉ gọi public API của module; không khởi tạo SDK adapter trực tiếp.

Mọi Android string resource chứa publisher/ad-unit ID dùng prefix
`id_emoji_battery_`; không giữ identifier legacy `id_private_browser_` hoặc `id_pub`.
Đổi tên resource không được tự ý đổi giá trị AdMob production/test bên trong.

AdMob app ID và ad-unit ID là cấu hình public được lưu ở `ads/src/main/res/values/strings.xml`.
Credential nhạy cảm như `app_password_mail` không được đặt trong resource/default XML; source chỉ
giữ key rỗng và giá trị production phải được cấp từ Firebase Remote Config.

## Base behavior còn giữ

- Splash khởi tạo consent/config liên quan.
- App Open Ad dùng Welcome Back pastel cover trong lúc chuyển sang quảng cáo. Đây là transient
  Compose content thuộc `:ads`, không phải navigation destination; Premium/ad-suppression và
  lifecycle show/dismiss hiện tại vẫn là boundary authoritative.
- Intro page 1 mount/load native placement `SCREEN_INTRO`, page 3 dùng `SCREEN_INTRO_SECOND`,
  còn page 2 không có ads. Mỗi placement chỉ tạo request khi pager đã settle đúng page; page
  được HorizontalPager pre-compose ngoài viewport không load sớm. Quay lại page đã xem tái sử
  dụng instance đã load trong cùng Intro lifecycle.
- Navigation có `navigateWithAd()` cho interstitial-aware transition.
- MainActivity quản lý App Open Ads theo lifecycle.
- Premium dùng BillingClient và `StartPremiumIndexes` để biết entry source.
- Native Ad templates dùng light pink-white surface theo Figma node `8047:2973`; các
  biến thể height/item/collapsible chia sẻ cùng background, border, text và CTA palette.
  Footer của native collapsible luôn dùng vector attribution `ic_ads_logo_collapse` ngay trước
  headline ở cả trạng thái expanded và collapsed; body căn theo đầu badge để attribution không
  làm lệch cột nội dung và badge không biến thành placeholder trắng khi shimmer đang chạy.
- Home shell trong `AppNavGraph` sở hữu đúng một `BannerAd` cho placement
  `home_mode_bottom`, nằm dưới bottom navigation. Banner giữ nguyên composition/ViewModel
  khi chuyển giữa Discover, Battery, Pet Store và Mine nên không request/reload lại theo tab.
  Battery category rời Home holder để dùng native riêng ở đáy. Settings khi chạy trong shell
  không render thêm native ad để tránh hai placement xếp chồng. Hero Battery Troll là asset
  presentational; slot promo thấp hơn dùng banner SDK `discover_inline`.
- Grant Permissions dùng native placement `screen_grant_permissions`, ghim cố định dưới danh
  sách quyền chứ không cuộn theo. Mọi row rời sang màn hệ thống đều tắt `needShowOpenAds` trong
  `openSettings()` — quay lại sau khi vừa cấp quyền mà ăn app-open ad là trả giá cho đúng hành
  động mình vừa yêu cầu user làm. Đặt trong helper chứ không ở từng call site để không có row
  nào lọt.
- Accessibility disclosure trên mọi feature dùng placement `dialog_accessibility_disclosure`
  với `AdType.HEIGHT_222`. Native nằm sát đáy sheet theo
  Figma; nếu placement không load/đã Premium thì slot collapse. Sau consent, màn How to use không
  thêm placement mới; CTA Settings dùng cùng launcher contract tắt App Open Ad trước khi rời app.
- Overlay disclosure dùng placement `dialog_overlay_permission`, template `AdType.HEIGHT_222`.
  Sheet dùng chung cho onboarding Permission, Grant
  Permissions và switch Pet Store; native collapse theo policy chung khi không có ad/Premium.
- Battery Troll không tạo thêm banner placement. Grid theme dùng lại banner inline
  `battery_category_inline` cho slot 328×50 ở đầu lưới; reward sheet dùng
  `RewardOfferSheet` + native riêng `dialog_battery_troll_reward`/`HEIGHT_222` để Remote Config
  và báo cáo native không lẫn với Battery Styles. Nếu sau này cần đo riêng banner Troll thì thêm
  `BANNER_BATTERY_TROLL_INLINE` trong `:ads` — đó là một product decision.
  Màn Customize không có native ad: giống Full/Component Editor, không chen quảng cáo vào
  thao tác tinh chỉnh và không che preview/Apply. Nó dùng chung banner đáy
  `battery_editor_bottom` do shell sở hữu (`showBatteryEditorBottomBanner` nhận cả route
  `battery_troll_customize/*`), đúng như Figma vẽ banner collapsed 50px dưới nút Apply.
  Grid theme thì nằm trong `showHomeBottomBanner` nên giữ banner shell của Home. Hai predicate
  loại trừ nhau — có test chặn để một màn không render hai banner chồng nhau.
- Search dùng native placement `screen_search` ở đáy màn hình và banner SDK
  `search_inline` trong content theo Figma; cả hai vẫn tuân theo remote key, frequency/ad-free
  policy và failure fallback chung của module ads.
- Bottom sheet `Apps that hide icons` dùng native `dialog_apps_hidden`/`HEIGHT_222` nằm dưới
  danh sách app. Slot collapse khi ads bị tắt, user ad-free hoặc load fail. Placement có screen
  code, Remote Config key và resource ID riêng; trong v1 resource này tạm dùng chung AdMob unit
  `9967933431` với nhóm reward/exit để sau này đổi ID mà không sửa UI.
- Battery landing dùng native placement `screen_battery_catalog` với template `HEIGHT_150` sau
  section đầu tiên. Category detail không còn banner inline ở đầu grid và cũng không dùng Home
  banner. Một holder native `screen_battery_category`/`HEIGHT_222` nằm ngoài `NavHost` ở đáy;
  resource ID riêng hiện tạm dùng chung AdMob unit với Battery catalog để có thể tách sau này.
  Khi native không đủ điều kiện hoặc load fail, holder collapse hoàn toàn.
- Customize Status Bar và các child library dùng holder shell nằm ngoài NavHost. Overview,
  Emotion group/detail cùng mười editor option dùng một native `COLLAPSE_SMALL`; các library
  Battery/Emoji/Theme vẫn giữ banner `battery_editor_bottom`. Hai loại loại trừ nhau nên không
  double-render ad và Apply luôn reflow ngay phía trên chiều cao collapsed/expanded thực tế.
- Overview Customize Status Bar dùng placement riêng `screen_customize_status_bar`, Remote Config
  `is_show_native_customize_status_bar` và ad-unit resource
  `id_emoji_battery_native_customize_status_bar`. Các màn con option/emotion/detail dùng
  `screen_battery_editor`; mỗi `NavBackStackEntry` mới truyền một `reloadKey` mới để hủy ad cũ và
  request native mới thay vì rebind lại cùng ad object.
- Các banner inline `discover_inline`, `search_inline` và `battery_category_inline` (chỉ Battery
  Troll) có ViewModel key riêng để không dùng chung ad object với banner shell. Holder căn giữa creative SDK 320×50,
  dùng nền trắng và shimmer `#E6E6E6`; khi ads bị tắt/Premium/load fail thì slot collapse, không
  thay thế bằng ảnh creative mẫu.

## Battery style Rewarded unlock

- Theme `FREE`, theme đã reward-unlock và toàn bộ theme của user Premium mở trực tiếp.
- Chạm theme `PREMIUM` chưa mở sẽ hiện bottom sheet với preview, một CTA Rewarded full-width và
  native `HEIGHT_222`; `Unlimited`/Premium entry tạm ẩn trong v1. Đóng bằng Back hoặc chạm scrim khi chưa loading.
  Sheet dùng placement `dialog_battery_reward`, tách khỏi native `HEIGHT_150` của landing.
- Rewarded chỉ được preload khi free user còn ít nhất một theme Premium chưa mở; Premium
  không tạo ad request. `EARNED` persist đúng theme ID vào
  `battery_status_reward_unlocked_theme_ids` rồi tự mở editor; `DISMISSED` giữ dialog và
  yêu cầu xem hết video.
- `UNAVAILABLE` tiếp tục/unlock theo fallback Rewarded chung hiện tại để lỗi SDK/inventory
  không tạo dead-end.
- Callback chỉ được consume khi đúng dialog đang pending và đang chờ reward; callback lặp
  không thể unlock hoặc navigate lần hai.
- Premium bypass Rewarded vẫn được giữ ở domain để tương thích entitlement, nhưng v1 không hiển
  thị PRO trên app bar hoặc `Unlimited` trong reward sheet.

## Rules

- Rewarded trả ba trạng thái `EARNED`, `DISMISSED`, `UNAVAILABLE`: `EARNED` và
  `UNAVAILABLE` tiếp tục flow, riêng `DISMISSED` dừng để không thưởng khi user đóng
  quảng cáo sớm.
- `AdOverlayState` phản ánh lifecycle callback thật của App Open, Interstitial và Rewarded,
  không tự reset theo timeout. Trong suốt fullscreen ad, MainActivity ẩn content phía sau và
  `StatusBarAccessibilityService` tháo custom status-bar overlay để overlay không che creative
  hoặc nút Close; callback dismiss/fail gắn lại overlay theo config hiện hành.
- Tránh chồng App Open Ads với interstitial/premium/full-screen flow.
- Không thêm placement mới nếu chưa có product/UX decision.
- Battery Rewarded là unlock trigger đã được owner duyệt. Editor có bottom banner đã được
  Figma chỉ định; reward sheet dùng `dialog_battery_reward`, còn discard-changes sheet dùng
  `dialog_battery_discard`; cả hai có template native `HEIGHT_222` riêng.
- Pet Store reward sheet dùng `dialog_pet_reward`; Food reward sheet dùng `dialog_food_reward`
  và được tái sử dụng nguyên placement khi mở từ Pet Store hoặc nút `+` trong My Pet Room — đây
  là cùng một dialog/intent nhận food nên không tạo ID hay Remote Config mới. Trong Pet Room,
  `Get it free` dùng Rewarded chung và Premium bypass video; `DISMISSED` giữ sheet để retry,
  `UNAVAILABLE` tiếp tục theo fallback chung, và callback/double tap chỉ cộng một portion. Khi
  Rewarded fullscreen mở từ Pet Room, room không restore floating-pet overlay lên trên creative.
  Favourite & Recent
  dùng `screen_favourite_recent`. Mỗi placement có Remote Config và string ad-unit riêng dù
  production ID hiện có thể đang dùng chung trong AdMob.
- Banner wrapper phát trạng thái visibility cho placement inline cần layout động. Battery More
  xóa toàn bộ grid item khi banner không đủ điều kiện hoặc load fail; không giữ placeholder 50dp.
- Screen code phải là constant trong ads config, không hardcode rải rác.
- Premium user/ad-free policy phải được kiểm tra ở integration boundary chung.
- Khi xóa screen, xóa placement/config không còn consumer.

## Khi thêm feature mới

Document rõ:

- Placement type và vị trí.
- Trigger/frequency cap.
- Loading/failed fallback.
- Premium behavior.
- Navigation continuation callback.
- Analytics event liên quan.
