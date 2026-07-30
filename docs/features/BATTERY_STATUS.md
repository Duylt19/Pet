# Battery Status Capsule

## Trạng thái

Vertical slice hiện đã có trong source:

- Home bottom navigation mở `BatteryCatalogScreen`.
- Catalog local chuẩn hóa, search, category, Free/Premium, favorite và built-in fallback.
- Editor dùng flow overview → editor con Size/Appearance/Emoji/Battery, có preview xuyên
  suốt, Apply cố định, 20 nền và 20 emotion đã audit từ snapshot.
- Apply lưu DataStore; nếu chưa bật service, app luôn hiện disclosure trước khi mở
  Accessibility Settings.
- `StatusBarAccessibilityService` vẽ một `TYPE_ACCESSIBILITY_OVERLAY` full-width,
  non-touchable ở cạnh trên, cập nhật pin/charging/time và dùng theme/nền/emotion đã chọn.
- Service ẩn khi màn hình khóa, màn hình tắt hoặc portrait không còn hiệu lực; không
  auto-start sau boot.

Đây chưa phải release-complete: cần asset ownership approval, device/OEM matrix, Play
Accessibility declaration và UX validation trước khi bật catalog ngoài debug.

## Luồng người dùng

```text
Home → Battery styles → chọn theme → Customize status bar
                                      ├─ overview + preview
                                      ├─ editor con → Done → overview
                                      └─ Apply
                                          ├─ service đã bật → persist + render
                                          └─ chưa bật → disclosure
                                              → Accessibility Settings
                                              → quay lại → persist + render
```

Premium theme chuyển sang Premium khi entitlement chưa có. Theme thiếu hoặc sai checksum
không được áp dụng. Built-in `Cute Mint` không phụ thuộc file ngoài và luôn khả dụng.

## Boundary dữ liệu

`BatteryCatalogRepository` expose `BatteryCatalogSnapshot`; UI không đọc file trực tiếp.
`LocalBatteryCatalogRepository` ưu tiên catalog được sync vào:

```text
externalFilesDir/battery_catalog/
├── catalog.json
├── thumb/<id>.png
├── battery/<id>.png
├── emoji/<id>.png
├── background/template_color_<id>.png
└── emotion/cute_emotion_<id>.png
```

Debug build tự chạy audit snapshot riêng, tạo catalog và đóng gói 898 theme cùng 20 nền,
20 emotion vào `assets/battery_catalog/`. Nếu external catalog không có hoặc thuộc schema
cũ, repository dùng bản packaged này. Vì vậy cài debug APK không còn phụ thuộc bước ADB
thủ công. Release không đóng gói snapshot `REVIEW_REQUIRED`.

Mỗi asset phải:

1. Có relative path đúng loại và ID.
2. Nằm canonical bên trong root catalog.
3. Khớp byte size.
4. Khớp SHA-256.

Catalog `REVIEW_REQUIRED` chỉ được load khi `BuildConfig.DEBUG`; release chỉ nhận
`APPROVED`. Nếu catalog thiếu/sai/chưa duyệt, repository trả built-in fallback cùng error
typed, không làm crash UI hoặc overlay đang chạy.

Ảnh `photo` tổng hợp từ snapshot không thuộc runtime contract. Runtime chỉ dùng thumbnail,
battery, emoji, background và emotion.

## Persistence

`BatterySettingsRepository` lưu:

| Field | Ý nghĩa |
|---|---|
| `enabled` | User đã Apply và muốn render |
| `selectedThemeId` | ID theme; `0` là built-in |
| `displayMode` | `COVER_SYSTEM_BAR` hoặc `BELOW_SYSTEM_BAR` |
| `showTime`, `showPercentage` | Thành phần hiển thị |
| `barHeightDp`, `horizontalPaddingDp` | Hình học capsule |
| `emojiSizeDp`, `batterySizeDp` | Kích thước asset |
| `backgroundColorArgb`, `foregroundColorArgb` | Màu renderer |
| `backgroundDecorationId` | Nền đóng gói đã chọn; `0` là nền màu phẳng |
| `showEmotion`, `emotionDecorationId` | Hiện/ẩn và chọn emotion trang trí |
| `privacyReserveDp` | Khoảng trống bên phải cho privacy/system indicators |
| `favoriteThemeIds` | Favorite local theo theme ID |

`BatterySettingsPolicy` clamp toàn bộ geometry và loại favorite ID âm để dữ
liệu DataStore lỗi không đi thẳng vào `WindowManager`.

## Accessibility và privacy

Service dùng Accessibility vì `TYPE_APPLICATION_OVERLAY` thông thường không thể thay thế
trực quan vùng SystemUI status bar. Đây vẫn chỉ là lớp phủ; app không sửa SystemUI.

Các guardrail bắt buộc:

- `canRetrieveWindowContent=false`; service không đọc node/text.
- Không gọi global action, gesture dispatch, click, type hoặc scroll.
- `onAccessibilityEvent` bỏ qua event; metadata không đăng ký event type.
- Window `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`, không chặn thao tác.
- Chừa khoảng phải cho system/privacy indicators.
- Chỉ mở Settings sau disclosure chủ động; không tự bật service.
- User có thể tắt service bất cứ lúc nào trong Android Settings.

Play release cần khai báo `isAccessibilityTool=false`, video demo, disclosure trong app,
Privacy Policy/Data Safety phù hợp và chứng minh chức năng cốt lõi. Nếu review không đạt,
không ship cover mode.

## Runtime

Service combine config + catalog bằng Flow. Asset được decode ngoài main thread và cache
theo khóa theme/nền/emotion; battery/time receiver chỉ invalidate view. Renderer không tạo
clock liên tục. Một window duy nhất được add/update/remove theo state.

Giới hạn hiện tại:

- Portrait only.
- Không render trên keyguard/screen-off.
- Cover behavior và notification-shade layering khác nhau theo OEM.
- Time và battery thật; theme emoji/nền/emotion là trang trí. Các component Wi‑Fi, signal,
  hotspot, airplane, ringer, date và per-icon editor trong screenshot vẫn thuộc phase sau.
- Overview hiển thị rõ các component phase sau ở trạng thái disabled; app không tạo control
  giả khi chưa có nguồn trạng thái platform tương ứng.
- Không có boot receiver và không tự hướng user quay lại Settings nếu họ disable service.

## Test gate

Trước release phải kiểm tra tối thiểu:

- Android 7–16; phone có/không notch, gesture/3-button navigation.
- Pixel, Samsung, Xiaomi/Redmi, Oppo/Realme nếu nằm trong device support.
- Enable/disable/revoke service, process death, app update.
- Screen off/on, keyguard, rotate, locale/timezone, 0/100%, charging.
- Mở notification shade, camera/microphone privacy indicator và system warning.
- TalkBack cùng tồn tại, large font, RTL và contrast.
- Không chặn touch/status gestures; không leak window/receiver/bitmap.
- Release build từ chối catalog `REVIEW_REQUIRED`.

Chi tiết research/phase gốc nằm tại
[`../plans/battery-status-capsule/README.md`](../plans/battery-status-capsule/README.md).
