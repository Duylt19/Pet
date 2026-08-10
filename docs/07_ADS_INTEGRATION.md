# 07 — Ads and Premium Integration

## Boundary

Module `:ads` sở hữu SDK integration, remote config, ad loading và ad UI/utilities. Feature trong `:app` chỉ gọi public API của module; không khởi tạo SDK adapter trực tiếp.

## Base behavior còn giữ

- Splash khởi tạo consent/config liên quan.
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
  không render thêm native ad để tránh hai placement xếp chồng. Hero placeholder và promo
  creative giữa content là presentational Figma assets, không gọi ads SDK.
- Grant Permissions dùng lại native placement `screen_permission` của màn Permission onboarding,
  ghim cố định dưới danh sách quyền chứ không cuộn theo. Không thêm placement mới vì hai màn
  cùng một ngữ cảnh xin quyền.
- Search tái sử dụng native placement `screen_home` ở đáy màn hình theo Figma; placement
  vẫn tuân theo remote key, frequency/ad-free policy và failure fallback chung của module ads.
- Battery landing tái sử dụng native placement `screen_home` với template `HEIGHT_150` sau
  section đầu tiên. Category detail có banner inline `battery_category_inline`; creative do SDK
  tải, không đóng gói ảnh quảng cáo mẫu trong Figma. Bottom banner vẫn là holder của shell.
- Banner holder dùng nền trắng và shimmer `#E6E6E6` để phần dư quanh creative 320×50
  hòa vào surface 360px của Figma thay vì lộ dải nền tối.

## Pet Swarm Rewarded unlock

- My Pet preload Rewarded khi screen vào composition.
- Khi Rewarded hiển thị được, user free chỉ unlock sau reward callback thật; đóng quảng cáo
  sớm không unlock. Nếu SDK/ad inventory không sẵn sàng, limit hoặc show fail thì flow tiếp
  tục ngay để lỗi quảng cáo không chặn tính năng.
- Callback được consume đúng một lần. Sau dismiss/fail, SDK preload lượt kế tiếp.
- Unlock được persist trên device bằng `pet_swarm_reward_unlocked`.
- Premium được xem là unlocked ngay và không cần mở Rewarded.
- Rewarded chỉ mở khóa mode; user vẫn chủ động chọn pet và bật global overlay.

## Mixed slot Rewarded unlock

- Slot 1–3 miễn phí. Slot 4–12 mở tuần tự; mỗi slot cần earned callback nếu Rewarded có
  thể hiển thị, còn unavailable tiếp tục ngay.
- Catalog là enforcement boundary dùng chung cho entry từ Home, Settings và deep route;
  khi chưa mở, `Set`, import và chuẩn bị pack đều bị chặn.
- Capacity được persist bằng `pet_mixed_reward_unlocked_slot_count`, mặc định 3 và clamp
  trong khoảng 3–12. Xóa pet không thu hồi capacity đã mở.
- Đóng quảng cáo sớm trước callback không mở slot. Nếu SDK/ad inventory không sẵn sàng,
  limit hoặc show fail thì mở slot và tiếp tục flow ngay; user vẫn không thể bỏ qua slot
  trước để mở slot sau.
- Premium bypass toàn bộ gate. Catalog kiểm tra lại entitlement ở `ON_RESUME` để áp dụng
  ngay sau khi user mua Premium.

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
- Battery Rewarded là unlock trigger đã được owner duyệt. Catalog/editor không thêm banner
  riêng; reward sheet chỉ dùng native `HEIGHT_222` được mô tả ở trên.
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
