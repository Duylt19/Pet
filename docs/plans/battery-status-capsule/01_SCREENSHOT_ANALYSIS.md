# 01 — Screenshot Analysis

> **REFERENCE RESEARCH — xem current source tại
> [`../../features/BATTERY_STATUS.md`](../../features/BATTERY_STATUS.md)**

## Phạm vi reference

14 ảnh đều có kích thước 1440×2960. Đây là screenshot runtime của một app khác, được dùng
để phân tích information architecture và capability, không phải nguồn pixel/asset để sao
chép. Emoji Battery sẽ giữ cozy visual system hiện tại và chỉ học cấu trúc thao tác.

## Screen 1 — Battery catalog

### Thành phần nhìn thấy

- System status bar thật vẫn nằm trên cùng.
- Header gồm brand, Favorites, Premium và Settings.
- Pager indicator ba trang/banner.
- Nhiều section category: Trending, Football, WC 2026; mỗi section có `Xem thêm`.
- Horizontal theme card: thumbnail, favorite, lock/unlock CTA.
- Native ad chen giữa section và banner ad phía dưới bottom navigation.
- Bottom navigation ba item; Battery đang selected.

### Contract đề xuất

- Header tái sử dụng `Emoji Battery`, Premium và Settings; Favorites chỉ xuất hiện khi catalog
  có dữ liệu.
- Category được localize hoàn toàn; không trộn tiếng Anh/Vietnamese như reference.
- Theme card có bốn trạng thái: free, rewarded locked, premium locked, selected.
- Grid/horizontal list phải có padding cuối để card không bị cắt vô nghĩa.
- Native/banners chỉ là placement đề xuất; không đưa ads vào overlay hoặc che CTA.
- Status bar thật trong screenshot catalog/editor không chứng minh behavior sau khi Apply.
  Việc reference yêu cầu Accessibility là evidence mạnh hơn cho runtime overlay che vùng
  status bar khi user rời app; xem
  [Accessibility status-cover mode](10_ACCESSIBILITY_STATUS_COVER.md).

## Screen 2–4 — Full editor

Full editor có preview capsule cố định phía trên, nội dung cuộn và CTA `Áp dụng` cố định.

### Geometry

- Chiều cao capsule.
- Lề trái và phải độc lập.
- Background theme và background color.
- Icon/content tint.

### Main content

- Background gallery có `Xem thêm`.
- Emoji: category, selected asset, size.
- Battery: icon style, icon size, percentage text size/color.
- Custom status components:
  - airplane mode;
  - ringer/bell;
  - date and time;
  - hotspot;
  - emotion;
  - Wi‑Fi;
  - cellular signal;
  - mobile data label;
  - decorative animation;
  - charging.

### Vấn đề cần sửa so với reference

- Các label dưới grid bị cắt ở cạnh trái/phải; Emoji Battery dùng grid responsive 2–4 cột và
  text tối đa hai dòng.
- Nhiều slider không nói rõ đơn vị. UI mới luôn hiển thị `dp`, `%` hoặc mô tả semantic.
- `Áp dụng` phải phân biệt draft với configuration đang chạy; Back khi dirty có confirm.
- Preview dùng state thật khi có, nhưng phải có deterministic sample state cho Preview/test.

## Screen 5 — Charging component editor

- Header Back / title / Done.
- Shared capsule preview.
- Charging indicator size và tint.
- Charging style grid 4×3.

Đề xuất bổ sung:

- Toggle enable.
- Behavior: replace battery icon, add bolt beside battery, hoặc keep normal battery style.
- Chỉ hiện khi `BatteryManager` báo charging/full.
- Accessibility label cho từng icon style.

Battery icon/percentage vẫn được chỉnh inline trong section Pin của Full Editor như Screen 3.

## Screen 6 — Animation editor

- Toggle enable.
- Size slider.
- Style gallery lớn.

`Hoạt ảnh` là decorative animation/character đặt trong capsule, không phải network activity
hay dữ liệu cảm biến. Runtime chỉ chạy asset đã validate bằng shared low-FPS clock.

## Screen 7 — Mobile data editor

- Size, tint.
- Label 2G–9G.

6G–9G không phải network generation Android có thể báo trong contract hiện tại. UI Cute
Pet đổi tên thành **Data label** và coi lựa chọn này là text style do user chọn. Nếu phase
sau đọc được `TelephonyDisplayInfo`, chỉ map 2G/3G/4G/5G; giá trị khác dùng fallback.

## Screen 8–9, 11, 13–14 — Simple indicator editors

Signal, Wi‑Fi, hotspot, bell và airplane cùng một shell:

- title;
- preview;
- enable/visibility behavior;
- size;
- tint;
- optional style gallery;
- Done.

Shell phải tái sử dụng component/policy thay vì tạo năm screen copy-paste.

## Screen 10 — Emotion editor

- Toggle enable.
- 20 emotion assets theo grid.

Đây là decorative module, không suy luận trạng thái cảm xúc hoặc dữ liệu người dùng.
Asset cần provenance/licensing và có fallback built-in.

## Screen 12 — Date and time editor

- Toggle hiển thị ngày.
- Text size/tint.
- Date format choices.
- Font style choices.

Đề xuất tách:

- Time: system 12/24-hour preference hoặc user override.
- Date: localized preset; không hardcode chuỗi tiếng Việt.
- Font: chỉ font đã bundle/licensed, không tải font tùy ý từ catalog.

## Screen hierarchy suy ra

```text
Battery Catalog
  ├─ Favorites / category view-all
  ├─ Unlock theme
  └─ Full Editor
       ├─ Geometry
       ├─ Interface/background
       ├─ Emoji
       ├─ Battery
       └─ Component Editor(type)
            ├─ simple indicator shell
            ├─ battery
            ├─ date/time
            ├─ animation
            ├─ emotion
            └─ mobile data label
```

## Runtime hypothesis sau khi biết permission flow

Reference nhiều khả năng không sửa SystemUI mà:

1. chạy một `AccessibilityService`;
2. add `TYPE_ACCESSIBILITY_OVERLAY` trong top status region;
3. render clock, pin, connectivity và asset custom lên trên status bar thật;
4. để touch/swipe đi xuyên qua bằng non-touchable flags.

Kết quả nhìn như “thay status bar”, nhưng status bar hệ thống vẫn hoạt động bên dưới. Đây
là hypothesis có cơ sở từ permission + Android layer policy, không phải reverse-engineering
source của app reference.

## Visual direction cho Emoji Battery

- Nền `colors_F4F8FC`, card trắng, primary teal và typography hiện tại của Emoji Battery.
- Radius lớn 16–24 sdp, shadow nhẹ, hierarchy rõ.
- Capsule preview là một component dùng chung giữa Catalog detail, Full Editor và
  Component Editor.
- Screenshot không phải Figma; trước khi code pixel-final cần owner duyệt wireframe/Figma
  riêng. Không suy ra exact spacing/color từ ảnh raster.
