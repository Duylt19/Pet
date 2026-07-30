# 07 — Monetization, Analytics and Policy

> **PLANNED — NOT IMPLEMENTED**

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

## Rewarded behavior đề xuất

Tái sử dụng `RewardedAdResult`:

- `EARNED`: persist unlock và tiếp tục.
- `DISMISSED`: không unlock.
- `UNAVAILABLE`: theo precedent hiện tại của app, tiếp tục/unlock để lỗi inventory không
  chặn UX. Owner có thể đổi policy này trước implementation, nhưng phải áp dụng nhất quán
  và có unit test.

Callback consume đúng một lần; preload lại sau dismiss/fail. Premium không gọi Rewarded.

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
| `status_config_apply` | enabled_component_count, source_theme, capsule_height_bucket |
| `status_overlay_start_result` | result, permission_state |
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

Trước lần Start đầu:

- giải thích capsule nằm trên app khác nhưng dưới status bar thật;
- minh họa vị trí;
- nói rõ non-touchable và cách Stop;
- giải thích ongoing notification;
- link Privacy Policy;
- không đặt CTA gây hiểu nhầm system permission.

Existing Permission screen có thể thêm một capability section; không request lại nếu overlay
đã granted.

## Foreground-service Play declaration

Khi migrate shared host:

- manifest `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` mô tả cả animated pets và user-configured
  battery status capsule;
- Play Console description nêu user tự Start, visual luôn perceptible và Stop được;
- demo video: grant overlay → Apply capsule → chuyển app → capsule hiện → Stop notification;
- mô tả impact nếu service bị gián đoạn;
- Data Safety/Privacy rà lại `ACCESS_NETWORK_STATE` và remote asset downloads.

Google Play yêu cầu FGS feature có lợi ích core, user initiated/perceptible, stop được và
không thể defer mà vẫn đáp ứng trải nghiệm. Release gate phải kiểm tra policy hiện hành,
không dựa duy nhất vào tài liệu kế hoạch này.

## Content policy

- Không dùng logo/character/football club/tournament/brand chưa được cấp phép.
- Không dùng screenshot đối thủ trong app listing.
- Category name generic không đồng nghĩa quyền dùng asset trong category.
- Ads/remote content cũng thuộc phạm vi Play policy và content rating.
