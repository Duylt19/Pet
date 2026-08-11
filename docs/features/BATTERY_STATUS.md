# Battery Status Capsule

## Trạng thái

Vertical slice hiện đã có trong source:

- Home bottom navigation mở `BatteryCatalogScreen`.
- Catalog local chuẩn hóa, Search từ Home header, category, Free/Premium, favorite và built-in
  fallback. Landing nhóm theme thật thành carousel theo category; More mở grid ba cột của
  category bằng ID canonical. Built-in ID `0` chỉ là runtime fallback, không phải catalog item.
- Category name được pipeline gắn emoji theo slug trong khi giữ nguyên ID/slug canonical;
  `categoryName` của tất cả theme được sinh cùng display name để parser validation khớp.
- Promo Customize Status Bar và DIY FAB mở editor bằng config hiện tại. Theme đang áp dụng được
  đánh dấu bằng nền/stroke hồng trong category detail; không còn card `Current` demo ở landing.
- Theme Premium mở bottom sheet full-width có preview, `Unlimited`, `Get it free` và native ad;
  earned reward mở khóa vĩnh viễn đúng theme ID trên thiết bị rồi tự mở editor. Premium bypass
  toàn bộ theme gate.
- Chọn một theme trong catalog khởi tạo đúng cặp pet + pin của theme đó. Editor có hai
  picker category độc lập để mix pet của theme A với pin của theme B; entitlement
  Rewarded/Premium được kiểm tra cho từng lựa chọn.
- Editor dùng Material app bar `exitUntilCollapsed`, preview nhúng luôn hiển thị và các
  library Battery/Emoji/Theme là child destination dùng chung ViewModel/draft. Battery/Emoji
  dùng grid ba cột; Theme dùng grid hai cột từ catalog runtime.
- Overview có đủ picker Battery/Emoji/Animation, Color/Theme và color picker HSV + opacity.
  Preset, slider và color picker đều cập nhật draft ngay; overlay thật chỉ nhận thay đổi khi
  feature đang bật.
- Catalog luôn cho phép mở editor để thử bằng preview nhúng. Overlay status bar thật chỉ nhận
  draft live khi feature đã được bật; nếu feature đang tắt thì editor không tự bật overlay dù
  Accessibility đã được cấp. Apply vẫn yêu cầu Accessibility trước khi bật.
- `StatusBarAccessibilityService` vẽ một `TYPE_ACCESSIBILITY_OVERLAY` full-width,
  non-touchable ở cạnh trên; cập nhật pin, charging, time/date, network, airplane, ringer,
  hotspot và dùng theme/nền/emotion/animation đã chọn.
- Background màu/ảnh và cả hai nhóm content phủ/dùng đúng toàn bộ chiều ngang window,
  không bo góc. Khoảng `privacyReserveDp` được giữ lại chỉ để tương thích dữ liệu cũ và
  không còn tác động lên renderer.
- Mặc định `barHeightDp` được lấy từ status-bar inset thật của thiết bị và nằm chính giữa
  slider. Range đối xứng thông thường là 50%–150% chiều cao hệ thống, nên user có thể
  giảm hoặc tăng bar thay vì bị giới hạn tối đa bằng status bar OEM. Cỡ chữ thời gian
  giữ cố định ở 16dp và không thay đổi theo slider chiều cao.
- Service ẩn khi màn hình khóa, màn hình tắt hoặc portrait không còn hiệu lực; không
  auto-start sau boot.
- Mine có bottom sheet `Apps that hide icons` lấy danh sách app launchable qua package
  visibility intent query. Package được chọn chỉ lưu cục bộ; khi Accessibility báo app đó
  đang foreground, service tháo battery window và tự gắn lại khi user chuyển sang app khác.
  Service chỉ dùng package name của window event cho rule này, không đọc node/content và
  không ghi hay gửi lịch sử app foreground. Accessibility disclosure nêu rõ boundary này.
- Pet và pin được renderer như một pair: cùng anchor ở cụm battery phía trailing, pin
  vẽ trước và pet vẽ chồng lên trên theo hai kích thước độc lập. Màn hình hẹp tự bỏ date,
  emotion/animation trước khi bỏ status cốt lõi; nhóm leading/trailing được mirror đúng
  trong RTL mà không lật ngược chữ hoặc bitmap.
- Khi mở editor của component phụ thuộc trạng thái thiết bị, preview dùng sample state có
  chủ đích để luôn hiện đúng Airplane/Hotspot/Ringer/Charging đang chỉnh trên status bar.
  Component focus
  được giữ qua width policy; Wi-Fi/Signal/Charge dùng đúng vector, còn Date cập nhật ngay
  format, bundled font, size và color từ draft.
- Config chưa lưu lựa chọn icon dùng Wi‑Fi style 2 và Hotspot style 3 theo default UX;
  Signal, Airplane và Ringer giữ style 1. Lựa chọn đã persist của user không bị ghi đè.

Đây chưa phải release-complete: cần asset ownership approval, device/OEM matrix, Play
Accessibility declaration và UX validation trước khi bật catalog ngoài debug.
`BuildConfig.BATTERY_STATUS_ENABLED=false` ở release là hard kill switch hiện tại: ẩn
Battery entry và ngăn service attach window cho tới khi các gate được duyệt.

## Luồng người dùng

```text
Home → Battery styles → chọn theme → Customize status bar
                                      ├─ khởi tạo pet + pin cùng item
                                      ├─ đổi pet, pin, animation, nền và màu trong draft
                                      ├─ feature đang bật → live preview trên status bar
                                      ├─ feature đang tắt → chỉ preview nhúng
                                      ├─ destination editor con → Done/Back → overview giữ scroll
                                      └─ Apply → nếu thiếu quyền thì disclosure → persist + render
```

Premium theme chưa mở hiển thị dialog Rewarded/Premium. Theme thiếu hoặc sai checksum
không được áp dụng. Built-in `Cute Mint` không phụ thuộc file ngoài, luôn khả dụng như
runtime fallback nhưng không được render thành catalog card.

## Boundary dữ liệu

`BatteryCatalogRepository` expose `BatteryCatalogSnapshot`; UI không đọc file trực tiếp.
`HybridBatteryCatalogRepository` ưu tiên private GitHub catalog:

```text
raw.githubusercontent.com/.../master/
├── json/batteries.json
└── battery/
    ├── thumb/<id>.png
    ├── battery/<id>.png
    ├── emoji/<id>.png
    ├── background/template_color_<id>.png
    ├── emotion/emotion_<id>.png
    └── animation/<name>.gif|json
```

Catalog JSON được cache app-private, revalidate theo TTL 24 giờ, ETag và rate-limit
backoff giống Pet. Thumbnail/preview dùng URL GitHub qua Coil; asset renderer chỉ tải khi
được chọn, verify size + SHA-256 rồi cache app-private. Token private repo dùng chung
Firebase Remote Config key `github_token_pet_server`, không hardcode trong source.

Trong Customize, card Pet luôn load trực tiếp `emojiPath` và card Pin luôn load trực tiếp
`batteryPath` qua Coil. Thumbnail tổng hợp của catalog không được dùng làm placeholder cho
component vì nó chứa cả pet lẫn pin và gây nội dung sai/chớp khi scroll nhanh; loading,
empty hoặc error dùng vector đúng loại cho tới khi component thật `Success`. Một lần chọn
remote chỉ được ghi vào draft sau khi đúng asset component đã materialize và verify thành
công. Trong lúc tải, đúng card đang chọn nhận overlay `Loading…`; indicator có một delay
ngắn để thao tác lấy từ cache không chớp. Các lựa chọn và hành động Apply tạm khóa, nhưng
Apply giữ nguyên màu/nội dung và không mang loading state. Nếu thiết bị offline mà asset
chưa có trong verified cache, lựa chọn cũ được giữ nguyên và UI hiện lỗi có thể thử lại;
không có trạng thái `Selected` giả.

Khi remote/cache không dùng được, repository thử catalog ở
`externalFilesDir/battery_catalog/`; Debug tiếp tục có packaged snapshot làm fallback.
Release chỉ nhận remote/external catalog `APPROVED`, cuối cùng luôn còn built-in
`Cute Mint`.

Mỗi asset phải:

1. Có relative path đúng loại và ID.
2. URL thuộc đúng private GitHub server hoặc file nằm canonical trong local root.
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
| `selectedThemeId` | ID style gốc đã mở editor; giữ để migration/analytics |
| `selectedBatteryThemeId` | Theme cung cấp asset pin; fallback từ `selectedThemeId` |
| `selectedEmojiThemeId` | Theme cung cấp asset pet/emoji; fallback từ `selectedThemeId` |
| `displayMode` | Migration legacy; build hiện tại sanitize về `COVER_SYSTEM_BAR` |
| `showTime`, `showPercentage` | Thành phần hiển thị |
| `showAnimation`, `animationAssetName`, `animationSizeDp` | Hoạt ảnh GIF/Lottie |
| `barHeightDp`, `leftPaddingDp`, `rightPaddingDp` | Chiều cao window động theo status bar thiết bị và padding content full-width |
| `emojiSizeDp`, `batterySizeDp`, `percentSizeDp` | Kích thước asset/pin |
| `backgroundColorArgb`, `foregroundColorArgb` | Màu renderer |
| `backgroundDecorationId` | Nền đóng gói đã chọn; `0` là nền màu phẳng |
| `showEmotion`, `emotionDecorationId` | Hiện/ẩn và chọn emotion trang trí |
| `wifi/data/signal/airplane/hotspot/ringer/charge *SizeDp/*ColorArgb` | Tùy chỉnh độc lập từng status component |
| `dataType`, `chargeIconIndex` | Nhãn mạng 2G–9G và một trong 12 icon sạc |
| `showDateTime`, `dateFormat`, `dateTimeFont`, `dateTimeSizeDp`, `dateTimeColorArgb` | Ngày/giờ và 6 font bundled |
| `privacyReserveDp` | Field tương thích dữ liệu cũ; renderer full-width hiện tại bỏ qua |
| `favoriteThemeIds` | Favorite local theo theme ID |
| `rewardUnlockedThemeIds` | Theme Premium đã mở khóa bằng Rewarded trên thiết bị |

`BatterySettingsPolicy` clamp toàn bộ geometry và loại ID âm để dữ liệu DataStore lỗi
không đi thẳng vào `WindowManager`. DataStore cũ chưa có hai component ID sẽ migrate cả
hai từ `selectedThemeId`. `BatteryDraftCodec` schema 2 lưu bản nháp versioned trong
`SavedStateHandle` và vẫn decode schema 1 theo cùng quy tắc; DataStore chỉ thay đổi khi
user bấm Apply.

## Accessibility và privacy

Service dùng Accessibility vì `TYPE_APPLICATION_OVERLAY` thông thường không thể thay thế
trực quan vùng SystemUI status bar. Đây vẫn chỉ là lớp phủ; app không sửa SystemUI.

Các guardrail bắt buộc:

- `canRetrieveWindowContent=false`; service không đọc node/text.
- Không gọi global action, gesture dispatch, click, type hoặc scroll.
- `onAccessibilityEvent` bỏ qua event; metadata không đăng ký event type.
- Window `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`, không chặn thao tác.
- Trước lần chuyển sang Accessibility Settings để cấp quyền, mọi entry point hiển thị cùng
  disclosure theo Figma `8437:7570`/`8437:9099`: giải thích phạm vi sử dụng, cam kết dữ liệu,
  checkbox explicit consent, Allow/Close và native permission placement. Allow bị chặn cho tới
  khi consent được chọn; quyền đã bật thì màn Grant Permissions cho phép mở thẳng Settings để
  quản lý hoặc tắt service.
- Content dùng toàn bộ chiều ngang overlay; `rightPaddingDp` là khoảng cách mép phải do
  user kiểm soát.
- Chỉ mở Settings sau disclosure chủ động; không tự bật service.
- User có thể tắt service bất cứ lúc nào trong Android Settings.

### Lưu ý khi deploy debug

Quyền Accessibility thuộc `Settings.Secure`, app không thể tự cấp, backup hoặc phục hồi.
Source không gọi `disableSelf()` và không ghi/xóa danh sách service đã bật.

- Package update cùng `applicationId`, component name và signing key chỉ rebind service,
  bình thường không mất quyền.
- Uninstall/reinstall, clear app data, đổi application ID/signature hoặc force-stop package
  có thể làm Android xóa service khỏi enabled list.
- Android Studio có tùy chọn **Force stop running application before launching activity**.
  Khi test feature này nên bỏ chọn tùy chọn đó; hoặc dùng `./gradlew installDebug` rồi mở
  app từ launcher trên thiết bị. Không dùng `adb uninstall`, `pm clear` hoặc
  `am force-stop` giữa các lần test nếu muốn giữ quyền.

Play release cần khai báo `isAccessibilityTool=false`, video demo, disclosure trong app,
Privacy Policy/Data Safety phù hợp và chứng minh chức năng cốt lõi. Nếu review không đạt,
không ship cover mode.

## Runtime

Service combine applied config + editor preview session + catalog bằng Flow. Preview
session process-local chỉ tồn tại khi editor visible và không ghi DataStore. Applied config
`enabled=false` luôn thắng preview; khi editor đóng, service quay về applied config. Hai bitmap
pet/pin được resolve độc lập, decode ngoài main thread và cache theo khóa asset.
Battery/time/system receiver và một `ConnectivityManager.NetworkCallback` theo dõi các
network đủ điều kiện, cache `NetworkCapabilities` theo từng `Network`, rồi
cập nhật immutable `BatteryDeviceState`. Cách này không phụ thuộc duy nhất vào default
network: khi Wi‑Fi là default app vẫn có thể nhận đúng cellular network còn tồn tại.
Renderer chỉ animate asset đã chọn; một window duy nhất được add/update/remove theo state.
Trong editor, `focusedComponent` được truyền tới renderer thật. Component đang chỉnh sửa
được đánh dấu required trong width policy để không bị ẩn do thiếu chỗ; Date vẫn tôn trọng
`showDateTime`, nhưng khi đã bật thì thay đổi size, color, format và font cập nhật live.
Các trường `dateTimeSizeDp`, `dateTimeColorArgb` và `dateTimeFont` là style dùng chung cho
cả giờ và ngày; `showTime` điều khiển giờ, còn `showDateTime` chỉ điều khiển phần ngày.
Wi‑Fi, signal, airplane, hotspot và ringer có picker 4 family icon độc lập, cập nhật live
qua editor preview session và được persist khi Apply. Ringer lưu một family nhưng map đúng
hai biến thể vibrate/silent. Trạng thái lỗi/off/limited tiếp tục dùng vector cảnh báo riêng
để style tùy chọn không làm sai thông tin thiết bị.

### Ma trận trạng thái hệ thống

| Component | State được phân biệt | Cách hiển thị |
|---|---|---|
| Pin | unavailable, unknown, discharging, plugged-not-charging, charging, full | Phần trăm luôn lấy từ sticky `ACTION_BATTERY_CHANGED`; trạng thái sạc chỉ dùng charge asset, không chèn thêm `⚡` vào phần trăm |
| Nguồn sạc | none, AC, USB, wireless, dock, unknown | Lưu trong `BatteryPowerState` để description và behavior không suy diễn từ một boolean |
| Wi‑Fi | disabled, disconnected, limited/captive, validated | Icon off, warning hoặc connected riêng |
| Cellular | disabled, disconnected, limited, validated | Chỉ render khi limited/validated; airplane luôn khóa cellular |
| Chuông | normal, vibrate, silent | Normal không chiếm chỗ; vibrate và silent có icon riêng. Preview giữ mode đang hoạt động, chỉ dùng silent mẫu khi mode thực là normal |
| Hotspot | unknown, disabled, disabling, enabling, enabled, failed | Ẩn khi unknown/disabled; pending, enabled và error có icon riêng |
| Máy bay | on/off | Đọc `Settings.Global.AIRPLANE_MODE_ON`, icon chỉ hiện khi on |

`contentDescription` của overlay ghép mức pin, trạng thái sạc, Wi‑Fi, cellular,
airplane, ringer và hotspot đang hoạt động thay vì chỉ đọc phần trăm pin.

Giới hạn hiện tại:

- Portrait only.
- Product scope hiện tại là cover-only. `BELOW_SYSTEM_BAR` cũ được migrate về cover để
  Accessibility window không vô tình che nội dung app.
- Không render trên keyguard/screen-off.
- Cover behavior và notification-shade layering khác nhau theo OEM.
- Wi‑Fi/cellular lấy từ tất cả network callback đủ điều kiện. `VALIDATED` nghĩa Android
  đã xác thực Internet; connected nhưng captive/unvalidated được hiển thị limited. Signal vẫn là
  trạng thái kết nối coarse, không đọc cường độ radio vì app không xin phone/location
  permission.
- Hotspot theo broadcast best-effort của OEM; Android không có API public ổn định để app
  thường truy vấn tethering hiện tại. Trước broadcast đầu tiên trạng thái là `UNKNOWN`
  và không hiển thị icon để tránh báo sai.
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
