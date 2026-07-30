# 11 — Reference Flow Redesign

## Mục tiêu

Flow cũ đưa toàn bộ toggle, palette, asset picker và slider vào một card dài. Người dùng
khó phân biệt thiết lập cấp thanh với thiết lập của từng thành phần, phải cuộn nhiều và
không biết thay đổi nào đã hoàn tất. Flow mới giữ một draft duy nhất nhưng chia UI thành
màn tổng quan và editor con, dựa trên 14 screenshot reference owner cung cấp.

## Phân nhóm screenshot

| Ảnh | Vai trò trong flow | Ánh xạ hiện tại |
|---|---|---|
| 1 | Discovery theo category, chọn mẫu pin | `BatteryCatalogScreen` |
| 2–4 | Tổng quan Customize, preview, size, appearance, component list, Apply cố định | `BatteryEditorScreen` — `OVERVIEW` |
| 5 | Editor biểu tượng pin/sạc | Editor con `BATTERY`; charge style chi tiết thuộc phase data tiếp theo |
| 6 | Editor hoạt ảnh trang trí | Hiện rõ trạng thái `Sắp có`; cần animation runtime riêng |
| 7 | Editor mobile data | `Sắp có`; cần connectivity state thật |
| 8 | Editor signal | `Sắp có`; cần telephony capability/policy |
| 9 | Editor Wi‑Fi | `Sắp có`; cần connectivity state thật |
| 10 | Editor cảm xúc | Editor con `EMOJI`, dùng 20 emotion đã audit |
| 11 | Editor hotspot | `Sắp có`; cần API/OEM fallback |
| 12 | Editor ngày và giờ/font | `Sắp có`; time cơ bản vẫn có quick toggle |
| 13 | Editor chuông | `Sắp có`; cần ringer state |
| 14 | Editor chế độ máy bay | `Sắp có`; cần airplane state |

Không tạo control giả cho component chưa có dữ liệu platform. Các component đó xuất hiện
disabled trong overview để người dùng hiểu phạm vi hiện tại và không nhầm rằng Apply sẽ
kích hoạt chúng.

## Flow triển khai

```text
Battery styles
    → chọn theme
        → Customize status bar / Overview
            ├─ Quick controls: time, percentage
            ├─ Size → Done
            ├─ Appearance → Done
            ├─ Emoji & emotion → Done
            ├─ Battery icon → Done
            ├─ More components / disabled roadmap
            └─ Apply
                ├─ Accessibility đã bật → lưu toàn bộ draft
                └─ chưa bật → disclosure → Android Accessibility Settings → lưu
```

Editor con là page nội bộ của cùng destination/ViewModel, không phải route độc lập. Vì
vậy draft không bị ghi sớm vào DataStore, Back/Done không làm mất các thay đổi đã chỉnh
trong session và chỉ Apply mới cập nhật overlay.

## Quy tắc UX

- Preview luôn nằm trước control của page hiện tại.
- Overview chỉ chứa toggle nhanh, summary và entry point; không chứa picker dài.
- Back ở editor con tương đương Done và quay về overview.
- Apply chỉ xuất hiện ở overview, cố định phía dưới.
- Disable chỉ xuất hiện khi overlay đang được áp dụng.
- Component chưa có runtime state phải disabled và ghi rõ `Sắp có`.
- Accessibility disclosure vẫn chỉ xuất hiện khi user chủ động Apply.

## Phase tiếp theo

1. Charge icon styles và charging-state mapping.
2. Date/time format + font catalog.
3. Wi‑Fi/signal/mobile data với capability fallback.
4. Ringer/airplane/hotspot state.
5. GIF/Lottie decoration với shared frame clock và performance budget.
