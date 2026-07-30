# 07 — Monetization, Analytics and Policy

> **CURRENT CONTRACT — Premium + per-theme Rewarded gate đã triển khai**

## Entitlement

```kotlin
enum class AssetEntitlement {
    FREE,
    REWARDED,
    PREMIUM
}
```

- Free: dùng ngay.
- Rewarded: unlock stable asset/theme ID trên device.
- Premium: entitlement hiện tại bypass mọi cosmetic lock.
- Applied premium asset vẫn render nếu billing refresh tạm lỗi; chỉ thu hồi sau khi
  entitlement source xác nhận không còn quyền theo policy billing hiện hành.
- Nếu pack bị server remove, installed asset đã licensed/owned không tự xóa giữa session;
  catalog có thể đánh dấu unavailable cho selection mới.

## Rewarded behavior hiện tại

Tái sử dụng `RewardedAdResult`:

- `EARNED`: persist unlock và tiếp tục.
- `DISMISSED`: không unlock.
- `UNAVAILABLE`: theo precedent hiện tại của app, tiếp tục/unlock để lỗi inventory không
  chặn UX.

Callback consume đúng một lần; preload lại sau dismiss/fail. Premium không gọi Rewarded.
Unlock persist theo stable theme ID; click Premium theme chưa mở luôn qua dialog có
Rewarded/Premium/Cancel.

## Ads placement proposal

Để giữ chất lượng editor, không copy toàn bộ ad density trong screenshot.

| Surface | Placement | Default |
|---|---|---|
| Battery Catalog | Native item sau section đầu | Remote-config controlled |
| Battery Catalog | Bottom banner | Remote-config controlled |
| View all asset catalog | Optional native item sau N rows | Off mặc định |
| Full Editor | None | Không che preview/Apply |
| Component Editor | None | Không chen ad vào thao tác tinh chỉnh |
| Overlay capsule | Never | Cấm |

Mọi placement mới cần:

- screen code;
- remote-config toggle;
- ad unit resource;
- premium bypass;
- loading/fail collapse không để blank gap;
- App Open suppression quanh Rewarded/Premium/system permission flow;
- cập nhật `docs/07_ADS_INTEGRATION.md` khi implement.

Proposed constants:

```text
SCREEN_BATTERY_CATALOG
SCREEN_STATUS_ASSET_CATALOG
BANNER_BATTERY_CATALOG_BOTTOM
IS_SHOW_NATIVE_BATTERY_CATALOG
IS_SHOW_NATIVE_STATUS_ASSET_CATALOG
```

Không tái sử dụng placement legacy Browser.

## Analytics events

### Screen views

Theo `ScreenName` trong UI/navigation spec.

### Product events

| Event | Parameters allowlist |
|---|---|
| `status_theme_view` | theme_id, entitlement, category_id |
| `status_theme_favorite` | theme_id, is_favorite |
| `status_theme_unlock_start` | theme_id, method |
| `status_theme_unlock_result` | theme_id, result |
| `status_editor_open` | source, theme_id |
| `status_component_open` | component_type |
| `status_config_apply` | enabled_component_count, source_theme, capsule_height_bucket, display_mode |
| `status_overlay_start_result` | result, capability_state, display_mode |
| `status_overlay_stop` | source |
| `status_asset_download_result` | asset_kind, result, size_bucket |
| `status_catalog_refresh_result` | source, result, cache_age_bucket |

Không log:

- exact time/date;
- battery percentage;
- network name/SSID;
- phone/subscription info;
- custom color history;
- remote token/URL;
- device status sequence;
- text/user content.

## Funnel

```text
Battery tab open
  → theme viewed
  → theme selected/unlocked
  → editor opened
  → component changed
  → Apply
  → permission granted
  → capsule running
  → day-1 return / stop
```

Dashboard phải phân biệt fail do permission, asset, service start và user dismiss; không
gộp tất cả thành drop-off.

## Overlay disclosure

Trước lần Start `BELOW_SYSTEM_BAR` đầu:

- giải thích capsule nằm trên app khác nhưng dưới status bar thật;
- minh họa vị trí;
- nói rõ non-touchable và cách Stop;
- giải thích ongoing notification;
- link Privacy Policy;
- không đặt CTA gây hiểu nhầm system permission.

Existing Permission screen có thể thêm một capability section; không request lại nếu overlay
đã granted.

## Accessibility disclosure and Play declaration

`COVER_SYSTEM_BAR` dùng AccessibilityService nên có disclosure/consent tách biệt ngay trước
khi mở Accessibility Settings:

- nói rõ service chỉ đặt thanh trang trí lên status region;
- nói rõ không đọc nội dung màn hình, không thao tác thay user, không thu thập/chia sẻ
  accessibility data;
- nêu cách tắt và hậu quả: cover mode dừng;
- nút đồng ý/từ chối rõ, không pre-check, không gộp vào Terms/Privacy;
- Privacy Policy/Data Safety và Play listing mô tả cùng một use case.

Cute Pet không phải accessibility tool:

- `isAccessibilityTool=false`;
- phải hoàn thành Accessibility declaration và review video;
- justification phải nêu `TYPE_APPLICATION_OVERLAY` nằm dưới status bar nên không đáp ứng
  lựa chọn visual cover do user chủ động yêu cầu;
- không dùng node retrieval, gesture dispatch, global action, package tracking hoặc
  automation;
- không che camera/microphone privacy indicator, system warning hoặc notification controls;
- full-width opaque parity bị xem là high-risk cho tới khi có policy/device evidence;
- remote kill switch có thể ẩn cover mode nếu review thất bại.

Chi tiết:
[Accessibility status-cover mode](10_ACCESSIBILITY_STATUS_COVER.md).

## Foreground-service Play declaration

Khi migrate shared host cho pet/below-bar mode:

- manifest `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` mô tả cả animated pets và user-configured
  below-system-bar battery status capsule;
- Play Console description nêu user tự Start, visual luôn perceptible và Stop được;
- demo video: grant overlay → Apply capsule → chuyển app → capsule hiện → Stop notification;
- mô tả impact nếu service bị gián đoạn;
- Data Safety/Privacy rà lại `ACCESS_NETWORK_STATE` và remote asset downloads.

Cover mode do AccessibilityService sở hữu không được lấy làm lý do giữ một FGS riêng.
Google Play yêu cầu FGS feature có lợi ích core, user initiated/perceptible, stop được và
không thể defer mà vẫn đáp ứng trải nghiệm. Cả FGS và Accessibility release gate phải kiểm
tra policy hiện hành, không dựa duy nhất vào tài liệu kế hoạch này.

## Content policy

- Không dùng logo/character/football club/tournament/brand chưa được cấp phép.
- Không dùng screenshot đối thủ trong app listing.
- Category name generic không đồng nghĩa quyền dùng asset trong category.
- Ads/remote content cũng thuộc phạm vi Play policy và content rating.
