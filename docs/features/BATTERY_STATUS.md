# Battery Status Capsule

## Trạng thái

Vertical slice hiện đã có trong source:

- Home bottom navigation mở `BatteryCatalogScreen`.
- Catalog local chuẩn hóa, search, category, Free/Premium, favorite và built-in fallback.
- Editor dùng overview → editor con Size/Appearance/Emoji/Battery và 9 status component,
  có preview xuyên suốt, Apply cố định, cảnh báo bỏ draft, phục hồi draft sau process death,
  20 nền, 20 emotion và 26 animation đã audit.
- Apply lưu DataStore; nếu chưa bật service, app luôn hiện disclosure trước khi mở
  Accessibility Settings.
- `StatusBarAccessibilityService` vẽ một `TYPE_ACCESSIBILITY_OVERLAY` full-width,
  non-touchable ở cạnh trên; cập nhật pin, charging, time/date, network, airplane, ringer,
  hotspot và dùng theme/nền/emotion/animation đã chọn.
- Service ẩn khi màn hình khóa, màn hình tắt hoặc portrait không còn hiệu lực; không
  auto-start sau boot.
- Preview và Canvas runtime dùng chung layout priority. Màn hình hẹp tự bỏ date,
  emotion/animation trước khi bỏ status cốt lõi; nhóm leading/trailing được mirror đúng
  trong RTL mà không lật ngược chữ hoặc bitmap.

Đây chưa phải release-complete: cần asset ownership approval, device/OEM matrix, Play
Accessibility declaration và UX validation trước khi bật catalog ngoài debug.
`BuildConfig.BATTERY_STATUS_ENABLED=false` ở release là hard kill switch hiện tại: ẩn
Battery entry và ngăn service attach window cho tới khi các gate được duyệt.

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
├── emotion/cute_emotion_<id>.png
└── animation/<name>.gif|json
```

Debug build tự audit và đóng gói 898 theme, 20 nền, 20 emotion, 21 GIF, 5 Lottie vào
`assets/battery_catalog/`; 12 vector charge, status vector và 6 font được generate vào
debug resources. External catalog thiếu/sai thì repository dùng packaged catalog. Release
không đóng gói snapshot `REVIEW_REQUIRED`.

Mỗi asset phải:

1. Có relative path đúng loại và ID.
2. Nằm canonical bên trong root catalog.
3. Khớp byte size.
4. Khớp SHA-256.

Catalog `REVIEW_REQUIRED` chỉ được load khi `BuildConfig.DEBUG`; release chỉ nhận
`APPROVED`. Nếu catalog thiếu/sai/chưa duyệt, repository trả built-in fallback cùng error
typed, không làm crash UI hoặc overlay đang chạy.

Ảnh `photo` tổng hợp từ snapshot không thuộc runtime contract.

## Persistence

`BatterySettingsRepository` lưu:

| Field | Ý nghĩa |
|---|---|
| `enabled` | User đã Apply và muốn render |
| `selectedThemeId` | ID theme; `0` là built-in |
| `displayMode` | Migration legacy; build hiện tại sanitize về `COVER_SYSTEM_BAR` |
| `showTime`, `showPercentage` | Thành phần hiển thị |
| `showAnimation`, `animationAssetName`, `animationSizeDp` | Hoạt ảnh GIF/Lottie |
| `barHeightDp`, `leftPaddingDp`, `rightPaddingDp` | Hình học capsule |
| `emojiSizeDp`, `batterySizeDp`, `percentSizeDp` | Kích thước asset/pin |
| `backgroundColorArgb`, `foregroundColorArgb` | Màu renderer |
| `backgroundDecorationId` | Nền đóng gói đã chọn; `0` là nền màu phẳng |
| `showEmotion`, `emotionDecorationId` | Hiện/ẩn và chọn emotion trang trí |
| `wifi/data/signal/airplane/hotspot/ringer/charge *SizeDp/*ColorArgb` | Tùy chỉnh độc lập từng status component |
| `dataType`, `chargeIconIndex` | Nhãn mạng 2G–9G và một trong 12 icon sạc |
| `showDateTime`, `dateFormat`, `dateTimeFont`, `dateTimeSizeDp`, `dateTimeColorArgb` | Ngày/giờ và 6 font bundled |
| `privacyReserveDp` | Khoảng trống bên phải cho privacy/system indicators |
| `favoriteThemeIds` | Favorite local theo theme ID |

`BatterySettingsPolicy` clamp toàn bộ geometry và loại favorite ID âm để dữ
liệu DataStore lỗi không đi thẳng vào `WindowManager`. `BatteryDraftCodec` lưu bản nháp
versioned trong `SavedStateHandle`; DataStore chỉ thay đổi khi user bấm Apply.

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

Service combine config + catalog bằng Flow. Bitmap/GIF/Lottie được decode ngoài main
thread và cache theo khóa asset. Battery/time/system receiver và
`ConnectivityManager.NetworkCallback` cập nhật một immutable `BatteryDeviceState`.
Renderer chỉ animate asset đã chọn; một window duy nhất được add/update/remove theo state.

Giới hạn hiện tại:

- Portrait only.
- Product scope hiện tại là cover-only. `BELOW_SYSTEM_BAR` cũ được migrate về cover để
  Accessibility window không vô tình che nội dung app.
- Không render trên keyguard/screen-off.
- Cover behavior và notification-shade layering khác nhau theo OEM.
- Wi‑Fi/cellular lấy từ active network capability. Signal là trạng thái kết nối coarse,
  không đọc cường độ radio vì app không xin phone/location permission.
- Hotspot theo broadcast best-effort của OEM; Android không có API public ổn định để app
  thường truy vấn tethering hiện tại.
- Nhãn data 2G–9G là style do user chọn, không tự suy luận generation của nhà mạng.
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
- JVM test layout narrow-width/required priority/RTL side mapping và draft JSON
  corrupt/round-trip.

Chi tiết research/phase gốc nằm tại
[`../plans/battery-status-capsule/README.md`](../plans/battery-status-capsule/README.md).
