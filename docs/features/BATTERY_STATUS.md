# Battery Status Capsule

## Trạng thái

Vertical slice hiện đã có trong source:

- Home bottom navigation mở `BatteryCatalogScreen`.
- Catalog local chuẩn hóa, Search từ Home header, category, Free/Premium, favorite và built-in
  fallback. Landing nhóm theme thật thành carousel theo category; More mở grid ba cột của
  category bằng ID canonical. Built-in ID `0` chỉ là runtime fallback, không phải catalog item.
- Fresh config chọn theme catalog ID `1` cho cả Battery và Emoji. ID `0` từ DataStore/draft
  debug cũ được migrate sang `1`; Emotion, Animation, Mobile Data label và Hotspot mặc định tắt.
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
- Editor dùng Material app bar `exitUntilCollapsed`; preview nhúng chỉ hiển thị khi Accessibility
  chưa cấp hoặc feature đang tắt. Khi status bar thật đã hoạt động, preview nhúng được ẩn. Các
  library Battery/Emoji/Theme là child destination dùng chung ViewModel/draft. Battery/Emoji
  dùng grid ba cột; overview và grid ưu tiên shared `trendingEmojiThemeIds`, sau đó giữ toàn bộ
  theme còn lại theo catalog order. Theme dùng grid hai cột từ catalog runtime.
- Preview nhúng theo dõi broadcast pin/sạc, airplane, ringer và hotspot ngay cả khi
  Accessibility service chưa chạy. Ở overview icon conditional chỉ hiện khi switch bật và trạng
  thái hệ thống đang active; trong màn option tương ứng policy mô phỏng active để kiểm tra style.
- Overview có đủ picker Battery/Emoji/Animation, Color/Theme và color picker HSV + opacity.
  Preset, slider và color picker đều cập nhật draft ngay; overlay thật chỉ nhận thay đổi khi
  feature đang bật.
- Các màn option dùng slider và palette hai hàng dùng toàn bộ chiều ngang khả dụng; sáu màu
  trong mỗi hàng được dàn đều giữa hai padding mép để không co cụm trên viewport rộng.
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
- Service ẩn khi màn hình khóa, màn hình tắt, portrait không còn hiệu lực hoặc App Open /
  Interstitial / Rewarded fullscreen ad đang hiển thị; sau dismiss/fail overlay chỉ gắn lại nếu
  các điều kiện config hiện hành vẫn hợp lệ. Service không auto-start sau boot.
- Mine có bottom sheet `Apps that hide icons` lấy danh sách app launchable qua package
  visibility intent query. Package được chọn chỉ lưu cục bộ; khi Accessibility báo app đó
  đang foreground, service tháo battery window và tự gắn lại khi user chuyển sang app khác.
  Danh sách app cuộn độc lập; khi list đã ở đầu, sheet nhận gesture kéo xuống và chỉ dismiss
  sau khi kéo tối thiểu 25% chiều cao, cùng contract với các permission disclosure sheet.
  Service chỉ dùng package name của window event cho rule này, không đọc node/content và
  không ghi hay gửi lịch sử app foreground. Accessibility disclosure nêu rõ boundary này.
- Pet và pin được renderer như một pair: cùng anchor ở cụm battery phía trailing, pin
  vẽ trước và pet vẽ chồng lên trên theo hai kích thước độc lập. Màn hình hẹp tự bỏ date,
  emotion/animation trước khi bỏ status cốt lõi; nhóm leading/trailing được mirror đúng
  trong RTL mà không lật ngược chữ hoặc bitmap.
- Khi mở editor của component phụ thuộc trạng thái thiết bị, preview dùng sample state có
  chủ đích cho Airplane/Hotspot/Charging. Riêng Ringer luôn phản ánh mode thật vì NORMAL không
  có icon, còn VIBRATE và SILENT dùng hai asset khác nhau.
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
                                      ├─ child edit → cập nhật draft/preview ngay; Back giữ thay đổi
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
    ├── thumb/<id>.webp|png
    ├── battery/<id>.webp|png
    ├── emoji/<id>.webp|png
    ├── background/<name>.webp|png
    ├── background_preview/<name>.webp|png
    ├── emotion/<name>.webp|png
    ├── emotion_preview/<name>.webp|png
    └── animation/<name>.gif|json
```

Catalog JSON được cache app-private, revalidate theo TTL 24 giờ, ETag và rate-limit
backoff giống Pet. Thumbnail/preview dùng URL GitHub qua Coil; asset renderer chỉ tải khi
được chọn, verify size + SHA-256 rồi cache app-private. Token private repo dùng chung
Firebase Remote Config key `github_token_pet_server`, không hardcode trong source.

Nhánh `emotion/` trong snapshot remote là nhóm Classic 20 item và tiếp tục giữ ID `1..20`.
UI/runtime bổ sung 80 PNG @3x đã export từ Figma trên private project server với ID
`21..100`; bảy background pack cũng nằm trong Battery catalog server. APK chỉ đọc preview
nhẹ và tải full asset theo lựa chọn của user.
Repository ghép Classic trước tám nhóm bundled, không thay thế dữ liệu legacy.

Background catalog v2 có 38 item: 18 frame Figma mới dùng ID `1..18` và luôn đứng trước,
20 nền cũ dùng ID `19..38`. Grid tải preview nhẹ; full lossless WebP/PNG chỉ được
download/verify khi
chọn. Frame ID `1` còn được đóng gói ở
`drawable-nodpi/img_battery_background_default.png`, nên default vẫn hiển thị khi offline.
ID `0` tiếp tục là nền màu phẳng.

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
| `backgroundColorArgb`, `foregroundColorArgb` | Màu renderer; background color chỉ active khi decoration ID bằng `0` |
| `backgroundDecorationId` | Nền đóng gói đã chọn; `0` là mode màu phẳng. Color và theme loại trừ nhau khi render |
| `showEmotion`, `emotionDecorationId` | Hiện/ẩn và chọn một trong 100 emotion server: 20 Classic + 80 thuộc tám pack mới |
| `wifi/data/signal/airplane/hotspot/ringer/charge *SizeDp/*ColorArgb` | Tùy chỉnh độc lập từng status component |
| `showWifi`, `showSignal`, `showData` | Bật/tắt độc lập Wi-Fi, cột sóng và nhãn loại mạng |
| `dataSizeDp`, `dataColorArgb`, `chargeIconIndex` | Style nhãn mạng thật và icon sạc |
| `showDateTime`, `dateFormat`, `dateTimeFont`, `dateTimeSizeDp`, `dateTimeColorArgb` | Ngày/giờ và 6 font bundled |
| `privacyReserveDp` | Field tương thích dữ liệu cũ; renderer full-width hiện tại bỏ qua |
| `favoriteThemeIds` | Favorite local theo theme ID |
| `rewardUnlockedThemeIds` | Theme Premium đã mở khóa bằng Rewarded trên thiết bị |

Picker Animation stream GIF theo viewport và materialize Lottie server thành file local trước
khi tạo composition. Cách này giữ đủ item cuối danh sách, tránh chạy đồng thời toàn bộ 26
animation và vẫn có fallback nhìn thấy được khi asset đang tải hoặc không parse được.

`BatterySettingsPolicy` clamp toàn bộ geometry, normalize theme user-visible tối thiểu về ID
`1` và loại ID âm để dữ liệu DataStore lỗi
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
- Metadata chỉ đăng ký `typeWindowStateChanged`; `onAccessibilityEvent` chỉ lấy
  `event.packageName` để áp dụng hidden-app list, không đọc gì khác từ event.
- Metadata khai cả `android:summary` lẫn `android:description`. Android hiển thị `summary`
  ngay dưới tên service trong danh sách Accessibility (ghép `"trạng thái/summary"`), còn
  `description` chỉ hiện ở màn chi tiết của service. Thiếu `summary` thì mục của app trong
  danh sách chỉ có mỗi chữ "Đang tắt", không giải thích được vì sao cần bật.
- Window `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`, không chặn thao tác.
- Trước lần chuyển sang Accessibility Settings để cấp quyền, mọi entry point hiển thị cùng
  disclosure theo Figma `8437:7570`/`8437:9099`: giải thích phạm vi sử dụng, cam kết dữ liệu,
  checkbox explicit consent, Allow/Close và native permission placement. Allow bị chặn cho tới
  khi consent được chọn; sau đó màn How to use Figma `8442:9525` hướng dẫn bốn bước trước khi CTA
  mở Settings. Quyền đã bật thì màn Grant Permissions cho phép mở thẳng Settings để quản lý hoặc
  tắt service.
- Content dùng toàn bộ chiều ngang overlay; `rightPaddingDp` là khoảng cách mép phải do
  user kiểm soát.
- Chỉ mở Settings sau disclosure chủ động; không tự bật service.
- User có thể tắt service bất cứ lúc nào trong Android Settings.

### Giữ process sống

`StatusBarAccessibilityService` khai `android:foregroundServiceType="specialUse"` và tự
`startForeground()` **chính nó** khi thanh pin đang bật. Đây là cách rẻ nhất để tránh force-stop:
cùng service, cùng process, không đẻ thêm service hay `android:process` riêng — nên DataStore
vẫn một tiến trình và không phải đổi sang store đa tiến trình.

- Điều kiện là `BuildConfig.BATTERY_STATUS_ENABLED && config.enabled`, tức **ý định đã lưu**, chứ
  không phải cửa sổ có đang gắn hay không. `updateOverlay()` gỡ overlay khi khoá màn hình, khi ở
  app bị loại trừ hoặc khi xoay ngang — đúng những lúc ROM đi dọn process. Hạ foreground ở đó là
  tự mở cửa cho nó.
- `syncForegroundState()` được gọi đầu `updateOverlay()` chứ không phải ở collector config, để lần
  promote bị platform từ chối (service rebind lúc app đang ở background) còn được thử lại ở window
  change hoặc lần mở khoá kế tiếp.
- Promote được bọc `runCatching`: bị từ chối thì thanh pin vẫn vẽ bình thường từ cửa sổ
  accessibility, chỉ mất lớp bảo vệ khỏi bị reclaim. Không được để nó kéo overlay chết theo.
- Notification dùng channel riêng `battery_status_overlay`, `IMPORTANCE_LOW`, và chỉ tồn tại khi
  thanh pin bật. Tắt thanh pin là `stopForeground(STOP_FOREGROUND_REMOVE)`.

### Khi quyền bị thu hồi ngoài ý muốn

Android **xoá** service khỏi `enabled_accessibility_services` mỗi khi package bị force-stop —
`AccessibilityManagerService.onPackagesForceStoppedLocked` ghi thẳng thay đổi đó vào Settings.
Đây là hành vi AOSP, không phải riêng ROM nào; nhiều ROM (MIUI/HyperOS…) force-stop khi user
xoá app khỏi màn đa nhiệm, nên thanh pin đang chạy có thể biến mất chỉ vì một cú vuốt. App
không thể ngăn, cũng không có API xin lại quyền, nên hợp đồng là **phát hiện và nói ra**:

- `batteryAccessibilityRecovery()` so `config.enabled` (ý định đã lưu, sống sót qua cú kill) với
  `BatteryAccessibility.isEnabled()` (thứ hệ thống cho phép lúc này). Hai cái lệch nhau là tín
  hiệu duy nhất cần; user tự tắt thì `config.enabled` cũng false nên không thể nhầm.
- Nguyên nhân lấy từ `PetBackgroundRestrictionReader.lastOverlayKill()` (`ApplicationExitInfo`,
  API 30+): `USER` → user đóng app, khác `USER` → thiết bị giết, không có bản ghi → không đổ lỗi.
  Đọc **một lần** cho mỗi ViewModel vì nó mô tả cái chết đã xảy ra và là một binder call.
- Discover hiện `BatteryAccessibilityRecoveryCard` phía trên enable card. Action chạy đúng luồng
  bật bình thường nên disclosure vẫn hiện trước khi sang Settings. Dismiss chỉ kéo dài trong
  vòng đời ViewModel — lần thu hồi sau vẫn phải nói được.

`BatteryAccessibility.detailsSettingsIntent()` mở thẳng trang của service thay vì danh sách
Accessibility. Action `android.settings.ACCESSIBILITY_DETAILS_SETTINGS` **không phải public API**
(`android.provider.Settings` chỉ export danh sách), nên mọi call site phải đi qua
`launchFirstAvailable(deepLink, settingsIntent, appDetails)` để tự rơi xuống fallback trên ROM
không có màn đó. AOSP `AccessibilityDetailsSettingsFragment` đọc `Intent.EXTRA_COMPONENT_NAME`
rồi `unflattenFromString`, nên component phải được truyền dạng flatten.

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
Date dùng `dateTimeSizeDp`, `dateTimeColorArgb`, `dateTimeFont`; Clock dùng size/màu riêng và
Roboto Medium. `showTime` và `showDateTime` điều khiển độc lập. Airplane, hotspot, ringer và
charge có switch riêng được persist; trạng thái OFF thắng cả preview focus và device state.
Emotion group/detail dùng preview focus `EMOTION`, nhưng `showEmotion=false` vẫn thắng focus.
Khi bật, item đang chọn được đánh dấu required trong width policy để user luôn quan sát được
thay đổi ở preview hẹp. Grid dùng ảnh preview server nhẹ; leaf full chỉ được chọn sau khi
download/verify/cache thành công, rồi runtime và preview thật cùng resolve leaf ID đó.
Các family/icon data có sẵn vẫn được chọn bằng grid theo card Charge và cập nhật live qua
editor preview session. Ringer lưu một family nhưng map đúng hai biến thể vibrate/silent.

### Ma trận trạng thái hệ thống

| Component | State được phân biệt | Cách hiển thị |
|---|---|---|
| Pin | unavailable, unknown, discharging, plugged-not-charging, charging, full | Phần trăm luôn lấy từ sticky `ACTION_BATTERY_CHANGED`; trạng thái sạc chỉ dùng charge asset, không chèn thêm `⚡` vào phần trăm |
| Nguồn sạc | none, AC, USB, wireless, dock, unknown | Lưu trong `BatteryPowerState` để description và behavior không suy diễn từ một boolean |
| Wi‑Fi | disabled, disconnected, limited/captive, validated | Icon off, warning hoặc connected riêng |
| Cellular | disabled, disconnected, limited, validated | Chỉ render khi limited/validated; airplane luôn khóa cellular |
| Chuông | normal, vibrate, silent | Normal không chiếm chỗ; vibrate và silent có icon riêng. Preview luôn giữ mode thật và đổi đúng icon khi user thay đổi chuông hệ thống |
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
- Android 11+ lấy nhãn hiển thị mạng thật từ `TelephonyDisplayInfo` (G/E/2G/3G/H/H+/4G/
  4G+/5G/5G+), gồm cả override 5G NSA theo carrier policy, không cần quyền điện thoại nguy hiểm.
  Android 10 trở xuống không suy diễn nhãn giả khi không có API đủ tin cậy; signal vẫn hiển thị
  theo connectivity. Riêng preview của màn Mobile Data dùng sample `5G` khi thiết bị không trả
  badge để user vẫn quan sát được size/color; runtime overlay không dùng sample này. Trường
  `dataType` cũ chỉ còn để decode tương thích draft/DataStore v1.
- Không có boot receiver và không tự hướng user quay lại Settings nếu họ disable service.

## Battery Troll

Battery Troll là **một chế độ của chính service này**, không phải overlay hay activity riêng.
Đọc mục này trước khi sửa `StatusBarAccessibilityService` hoặc `BatteryStatusBarView`.

- **Chỉ con số bị làm giả.** `trollMode = FAKE` đổi chuỗi phần trăm sang `trollFakePercent`
  (0–999). `powerState.level` vẫn `coerceIn(0, 100)` và vẫn điều khiển độ đầy icon pin.
  Tách hai thứ này là lý do 999% là trò đùa chứ không phải bug render. Layout được đo lại mỗi
  `render()` (`cachedLayout = null`) nên chuỗi ba chữ số không phá cơ chế rớt component
  theo priority.
- **Một thanh, một config, Apply sau thắng.** Battery Troll và Customize Status Bar ghi vào
  cùng một `BatteryStatusConfig` — cố ý, vì chỉ có đúng một thanh status bar. Để điều đó
  không thành bẫy, Apply bên editor gọi `BatteryTrollPolicy.releaseOverride()` để trả lại
  quyền vẽ: `trollThemeId` về `0` và `trollMode` về `REAL`. Thiếu bước này thì troll đã apply
  trước đó tiếp tục đè lên lựa chọn emoji/pin của editor, và Apply bên editor trông như không
  có tác dụng gì. Các mức level và switch random/emoji **được giữ lại**, để user quay về
  Battery Troll vẫn thấy đúng lựa chọn cũ.
- **Artwork.** `trollThemeId != 0` thì emoji và pin lấy từ `BatteryTrollCatalogRepository`
  theo chỉ số mức (0 = đầy … 4 = cạn) thay vì từ battery catalog. Quyết định chọn path nằm ở
  `BatteryTrollAssetPolicy` (Kotlin thuần, có test); service chỉ materialize/decode.
- **Hai frame là hai lớp của một bức vẽ, không phải hai icon.** Vị trí nhân vật so với vỏ pin
  chỉ được mã hoá bằng chỗ nó nằm trong canvas của chính nó, nên hai lớp **bắt buộc** vẽ cùng
  một scale, nếu không offset co giãn theo và nhân vật trôi khỏi vỏ. `batteryTrollEmojiSizeDp()`
  suy scale đó từ `emojiCanvasPx / batteryCanvasPx` mà catalog công bố; `emojiSizeDp` của user
  **không** dùng ở chế độ troll vì slider đó dành cho việc trộn pet của theme A với pin của
  theme B, điều troll không bao giờ làm. Catalog không công bố canvas thì mới rơi về slider.
  Luật này áp cho cả overlay thật lẫn `BatteryStatusPreviewCard`, nên preview không thể hiển
  thị theo quy tắc mà thanh thật không theo.
- **Yêu cầu với pipeline export.** Vì vị trí chỉ sống trong canvas, pipeline **không được**
  trim rồi căn giữa từng frame emoji. Bản catalog hiện tại đã bị: tâm nội dung trùng tâm canvas
  ±1px ở toàn bộ troll và toàn bộ mức, nên mọi mức đều vẽ nhân vật ở giữa vỏ pin và bản dựng
  của designer (nhân vật ngồi *trên* pin ở mức đầy, chui *vào trong* ở mức cạn) không thể tái
  tạo được từ dữ liệu. Cách sửa: export emoji trên **đúng canvas của pin** với nhân vật đã đặt
  sẵn vị trí từng mức. Khi hai canvas bằng nhau, scale trên bằng `1.0` và app dựng lại bản
  thiết kế nguyên vẹn mà không cần đổi thêm dòng code nào.
- **Fallback bắt buộc.** Theme đã chọn nhưng vắng mặt trong catalog — chưa tải, offline, hoặc
  bị gỡ trên server — phải rơi về theme battery thường. Không bao giờ để slot trống vì lý do này.
- **`trollShowEmoji = false` là một lựa chọn, không phải lỗi.** Lúc đó troll vẫn sở hữu slot
  emoji và cố ý để trống. Không được fallback về emoji của theme thường, nếu không nhân vật cũ
  sẽ lặng lẽ quay lại đúng lúc user vừa tắt nó đi.
- **Random artwork** tự hẹn lần vẽ kế tiếp đúng mốc chu kỳ (`BATTERY_TROLL_RANDOM_ROTATION_MS`),
  suy ra từ thời gian còn lại chứ không hardcode. Emoji và pin xoay **đồng bộ**: nhân vật khóc
  cạnh viên pin đầy trông như lỗi chứ không phải trò đùa.
- **Mọi `postDelayed` trong service phải dùng `Runnable` field cố định**, không dùng
  `::render`. Method reference tạo instance mới mỗi lần nên `removeCallbacks` không huỷ được,
  và callback còn treo sau khi overlay detach là rò pin. Cả `trollRotationTick` lẫn
  `assetRetryTick` đều bị huỷ trong `removeOverlay()`.

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
