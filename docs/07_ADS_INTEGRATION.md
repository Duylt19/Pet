# 07 — Ads and Premium Integration

## Boundary

Module `:ads` sở hữu SDK integration, remote config, ad loading và ad UI/utilities. Feature trong `:app` chỉ gọi public API của module; không khởi tạo SDK adapter trực tiếp.

## Base behavior còn giữ

- Splash khởi tạo consent/config liên quan.
- Intro có native placement theo config hiện có.
- Navigation có `navigateWithAd()` cho interstitial-aware transition.
- MainActivity quản lý App Open Ads theo lifecycle.
- Premium dùng BillingClient và `StartPremiumIndexes` để biết entry source.

## Rules

- Ad load/show fail không được chặn navigation hoặc action chính.
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
