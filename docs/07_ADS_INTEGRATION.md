# 07 — Ads and Premium Integration

## Boundary

Module `:ads` sở hữu SDK integration, remote config, ad loading và ad UI/utilities. Feature trong `:app` chỉ gọi public API của module; không khởi tạo SDK adapter trực tiếp.

## Base behavior còn giữ

- Splash khởi tạo consent/config liên quan.
- Intro có native placement theo config hiện có.
- Navigation có `navigateWithAd()` cho interstitial-aware transition.
- MainActivity quản lý App Open Ads theo lifecycle.
- Premium dùng BillingClient và `StartPremiumIndexes` để biết entry source.

## Pet Swarm Rewarded unlock

- Home preload Rewarded khi screen vào composition.
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

## Rules

- Rewarded trả ba trạng thái `EARNED`, `DISMISSED`, `UNAVAILABLE`: `EARNED` và
  `UNAVAILABLE` tiếp tục flow, riêng `DISMISSED` dừng để không thưởng khi user đóng
  quảng cáo sớm.
- Tránh chồng App Open Ads với interstitial/premium/full-screen flow.
- Không thêm placement mới nếu chưa có product/UX decision.
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
