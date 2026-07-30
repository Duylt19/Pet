# 02 — Product and UX Specification

> **PLANNED — NOT IMPLEMENTED**

## Product statement

Battery Status Capsule cho phép user tạo một thanh thông tin nhỏ, dễ thương khi dùng app
khác. User có thể chọn đặt capsule dưới status bar hoặc dùng Accessibility status-cover
mode để che trực quan vùng status bar. Capsule ưu tiên pin, charging và thời gian; các
module còn lại là tùy chọn.

## Terminology bắt buộc

- Dùng `Battery Status Capsule`, `Battery bar` hoặc bản dịch tự nhiên `Thanh trạng thái pin`.
- Có thể gọi UI là `Che thanh trạng thái`/`Status-cover mode`, nhưng không tuyên bố app
  sửa hoặc thay thế SystemUI thật.
- `Data label` là label trang trí; không gọi 6G–9G là mạng thật.
- `Animation` là asset trang trí, không được mô tả như dữ liệu hoạt động của thiết bị.

## User stories

1. User xem theme theo category, favorite hoặc unlock một theme.
2. User mở editor, thay nền, màu, kích thước, emoji và battery style.
3. User bật/tắt từng status component và xem preview ngay.
4. User chọn display mode, bấm Apply; app lưu configuration, chạy đúng disclosure/special
   access flow và start/update capsule.
5. User quay lại editor; applied configuration được khôi phục chính xác.
6. User stop capsule từ Home hoặc ongoing notification mà không stop pet ngoài ý muốn.
7. Premium user dùng asset premium không cần Rewarded.

## Primary flow

```text
Home bottom nav → Battery Catalog
  ├─ free/owned theme → Full Editor(theme draft)
  ├─ rewarded theme → Rewarded → Full Editor
  ├─ premium theme → Premium → return → Full Editor
  └─ Favorites / View all

Full Editor
  ├─ edit common settings inline
  ├─ open Component Editor → Done → update parent draft
  └─ Apply
       ├─ below-bar + no overlay access → overlay settings → resume → apply
       ├─ cover-bar + Accessibility off → disclosure/consent → accessibility settings
       ├─ FGS path + API 33+ notification missing → request → apply
       └─ capability ready → persist + start/update selected runtime backend
```

## Draft/apply semantics

- `appliedConfig`: configuration authoritative cho runtime.
- `draftConfig`: bản copy chỉnh trong editor.
- Thay đổi draft chỉ update in-app preview.
- `Done` ở component editor commit vào draft của full editor, không tự persist runtime.
- `Apply` validate, persist atomically rồi update runtime.
- Back khi draft khác applied config hiển thị confirm Discard/Keep editing.
- Process death trong editor restore draft bằng `SavedStateHandle`; không persist một config
  chưa Apply thành runtime.

## Enable/disable semantics

- `Apply` lần đầu có thể bật capsule sau permission flow.
- Battery Catalog/Home hiển thị switch riêng cho capsule.
- Pet và capsule là hai feature flag độc lập; chỉ below-bar backend dùng chung FGS host.
- FGS notification có action `Stop all foreground overlays`; nếu framework hỗ trợ nhiều
  action rõ ràng, thêm `Stop pets` và `Stop battery bar`.
- Cover-only mode có Stop trong Home/Catalog/Accessibility service settings; không tạo FGS
  chỉ để có notification.
- Không auto-start sau boot trong MVP.

## Default configuration

```text
height: 32dp
left/right margin: 12dp
display mode: BELOW_SYSTEM_BAR until owner approves cover-mode release
background: built-in sky capsule
icon/text tint: automatic contrast
left group: time + one emoji
right group: connectivity icon + battery percentage + battery icon
date, emotion, animation, hotspot, airplane, bell: disabled
capsule enabled: false until explicit Apply/Start
```

## Component visibility behavior

| Component | Default | Runtime rule |
|---|---:|---|
| Time | On | Update mỗi phút và khi timezone/time đổi |
| Date | Off | Localized format, update lúc ngày đổi |
| Emoji | On | Decorative, static trong MVP |
| Battery icon | On | Luôn hiện |
| Battery percent | On | Giá trị thật 0–100 |
| Charging | On | Chỉ hiện khi charging/full |
| Wi‑Fi | Auto | Hiện khi default network dùng Wi‑Fi |
| Signal | Auto | Hiện khi default network dùng cellular |
| Data label | Off | User-selected label; không claim exact generation |
| Airplane | Auto | Chỉ hiện khi airplane mode bật |
| Bell | Auto | Hiện khi silent/vibrate theo chosen behavior |
| Hotspot | Off | Decorative/manual cho API <36; real callback là enhancement |
| Animation | Off | Decorative asset, chạy bằng bounded low-FPS clock |
| Emotion | Off | Decorative, always-on khi enabled |

## Display mode behavior

| Mode | Label | Behavior |
|---|---|---|
| `BELOW_SYSTEM_BAR` | Dưới thanh hệ thống | Application overlay ở top safe inset |
| `COVER_SYSTEM_BAR` | Che thanh hệ thống | Accessibility overlay trên status region |

- Cover mode là visual replacement; native status bar vẫn tồn tại phía dưới.
- Chuyển mode là explicit Apply, không tự đổi khi quyền mất.
- Accessibility bị tắt: dừng cover backend, giữ config và hiển thị CTA bật lại/chuyển mode.
- Lock screen/landscape hide mặc định trong MVP cover mode.

## Empty/error/offline states

- Catalog cache empty + offline: built-in starter themes vẫn hiện.
- Remote asset download fail: giữ selection/runtime hiện tại, cho Retry.
- Asset invalid/hash mismatch: reject, không apply.
- Overlay revoked: below-bar backend remove capsule ngay; config vẫn được lưu.
- Accessibility disabled: cover backend remove capsule ngay; config/mode vẫn được lưu và
  UI hiển thị `Needs accessibility`.
- Notification denied API 33+ trên FGS path: vẫn có thể start theo platform behavior nhưng
  UI phải giải thích notification có thể chỉ xuất hiện trong task manager.
- Missing device signal API: dùng connected/disconnected fallback, không hiển thị dữ liệu giả.

## Accessibility/localization

- Minimum touch target 48dp cho control trong app; overlay không có touch target.
- Không truyền tải state chỉ bằng màu; selected card có border + check + semantics.
- Preview có content description tổng hợp, ví dụ “Pin 73 phần trăm, đang sạc, Wi‑Fi”.
- Mọi title/CTA/category dùng string resources và locale fallback English.
- RTL: group left/right đổi theo layout direction nhưng battery group vẫn có preset cho
  user muốn giữ physical side; cần test Arabic/Hebrew.
- Font date/time phải legible ở text scale 1.3×; nếu overflow dùng policy ưu tiên, không
  scale chữ xuống dưới minimum.

## Explicit non-goals MVP

- Không chỉnh sửa SystemUI hoặc native status icons.
- Không dùng Accessibility để đọc node/content, theo dõi app usage, automation, gesture
  dispatch hoặc global actions.
- Không dùng hidden/privileged telephony, tethering hoặc SystemUI API.
- Không boot auto-start.
- Không cho remote pack chứa code, font, SVG/XML hoặc Lottie JSON chưa kiểm soát.
- Không copy asset/category branding/character từ screenshot.
