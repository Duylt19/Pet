# 07 — Ads and Premium Integration

## Boundary

Module `:ads` sở hữu SDK integration, remote config, ad loading và ad UI/utilities. Feature trong `:app` chỉ gọi public API của module; không khởi tạo SDK adapter trực tiếp.

## Base behavior còn giữ

- Splash khởi tạo consent/config liên quan.
- App Open Ad dùng Welcome Back pastel cover trong lúc chuyển sang quảng cáo. Đây là transient
  Compose content thuộc `:ads`, không phải navigation destination; Premium/ad-suppression và
  lifecycle show/dismiss hiện tại vẫn là boundary authoritative.
- Intro có native placement theo config hiện có.
- Navigation có `navigateWithAd()` cho interstitial-aware transition.
- MainActivity quản lý App Open Ads theo lifecycle.
- Premium dùng BillingClient và `StartPremiumIndexes` để biết entry source.
- Native Ad templates dùng light pink-white surface theo Figma node `8047:2973`; các
  biến thể height/item/collapsible chia sẻ cùng background, border, text và CTA palette.
- Home shell trong `AppNavGraph` sở hữu đúng một `BannerAd` cho placement
  `home_mode_bottom`, nằm dưới bottom navigation. Banner giữ nguyên composition/ViewModel
  khi chuyển giữa Discover, Battery, Pet Store và Mine nên không request/reload lại theo tab.
  Battery category ẩn bottom navigation nhưng vẫn giữ cùng holder/banner key khi mở từ Battery.
  Banner chỉ dispose khi đi khỏi toàn bộ nhóm này. Settings khi chạy trong shell
  không render thêm native ad để tránh hai placement xếp chồng. Hero Battery Troll là asset
  presentational; slot promo thấp hơn dùng banner SDK `discover_inline`.
- Grant Permissions dùng lại native placement `screen_permission` của màn Permission onboarding,
  ghim cố định dưới danh sách quyền chứ không cuộn theo. Không thêm placement mới vì hai màn
  cùng một ngữ cảnh xin quyền. Mọi row rời sang màn hệ thống đều tắt `needShowOpenAds` trong
  `openSettings()` — quay lại sau khi vừa cấp quyền mà ăn app-open ad là trả giá cho đúng hành
  động mình vừa yêu cầu user làm. Đặt trong helper chứ không ở từng call site để không có row
  nào lọt.
- Accessibility disclosure trên mọi feature cũng dùng placement `screen_permission` với
  `AdType.HEIGHT_222` và `instanceKey=accessibility_disclosure`. Native nằm sát đáy sheet theo
  Figma; nếu placement không load/đã Premium thì slot collapse. Mọi launcher Accessibility dùng
  cùng contract tắt App Open Ad trước khi rời app.
- Overlay disclosure dùng cùng placement `screen_permission`, template `AdType.HEIGHT_222` và
  `instanceKey=overlay_permission_disclosure`. Sheet dùng chung cho onboarding Permission, Grant
  Permissions và switch Pet Store; native collapse theo policy chung khi không có ad/Premium.
- Search tái sử dụng native placement `screen_home` ở đáy màn hình và banner SDK
  `search_inline` trong content theo Figma; cả hai vẫn tuân theo remote key, frequency/ad-free
  policy và failure fallback chung của module ads.
- Battery landing tái sử dụng native placement `screen_home` với template `HEIGHT_150` sau
  section đầu tiên. Category detail có banner inline `battery_category_inline`; creative do SDK
  tải, không đóng gói ảnh quảng cáo mẫu trong Figma. Bottom banner vẫn là holder của shell.
- Customize Status Bar và các child library dùng holder shell nằm ngoài NavHost. Overview và
  Battery/Emoji/Theme giữ banner `battery_editor_bottom`; Emotion group/detail cùng mười editor
  option thay slot đó bằng một native `COLLAPSE_SMALL` dùng chung, nên không double-render ad và Apply luôn reflow
  ngay phía trên chiều cao collapsed/expanded thực tế.
- Native editor dùng chung `instanceKey=battery_editor_collapsible`. Khi vào Emotion lần đầu,
  holder load/bind theo placement `screen_home`; push group → detail giữ cùng Compose slot và
  Activity ViewModel nên không request lại. Rời flow rồi quay lại ưu tiên rebind cache còn hợp lệ.
- Các banner inline `discover_inline`, `search_inline` và `battery_category_inline` có ViewModel
  key riêng để không dùng chung ad object với banner shell. Holder căn giữa creative SDK 320×50,
  dùng nền trắng và shimmer `#E6E6E6`; khi ads bị tắt/Premium/load fail thì slot collapse, không
  thay thế bằng ảnh creative mẫu.

## Battery style Rewarded unlock

- Theme `FREE`, theme đã reward-unlock và toàn bộ theme của user Premium mở trực tiếp.
- Chạm theme `PREMIUM` chưa mở sẽ hiện bottom sheet với preview, hai action `Unlimited` và
  `Get it free`, cùng native `HEIGHT_222`; đóng bằng Back hoặc chạm scrim khi chưa loading.
  Sheet dùng placement `screen_home` nhưng có `instanceKey` riêng để không tranh ad object
  với native `HEIGHT_150` của landing.
- Rewarded chỉ được preload khi free user còn ít nhất một theme Premium chưa mở; Premium
  không tạo ad request. `EARNED` persist đúng theme ID vào
  `battery_status_reward_unlocked_theme_ids` rồi tự mở editor; `DISMISSED` giữ dialog và
  yêu cầu xem hết video.
- `UNAVAILABLE` tiếp tục/unlock theo fallback Rewarded chung hiện tại để lỗi SDK/inventory
  không tạo dead-end.
- Callback chỉ được consume khi đúng dialog đang pending và đang chờ reward; callback lặp
  không thể unlock hoặc navigate lần hai.
- Premium bypass Rewarded. Khi quay lại Catalog sau mua Premium, pending theme tự mở.

## Rules

- Rewarded trả ba trạng thái `EARNED`, `DISMISSED`, `UNAVAILABLE`: `EARNED` và
  `UNAVAILABLE` tiếp tục flow, riêng `DISMISSED` dừng để không thưởng khi user đóng
  quảng cáo sớm.
- Tránh chồng App Open Ads với interstitial/premium/full-screen flow.
- Không thêm placement mới nếu chưa có product/UX decision.
- Battery Rewarded là unlock trigger đã được owner duyệt. Editor có bottom banner đã được
  Figma chỉ định; reward sheet và discard-changes sheet dùng native `HEIGHT_222`, mỗi sheet có
  `instanceKey` riêng để không dùng chung ad object với placement khác trong cùng back stack.
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
