# 05 — Data and State Model

## DataStore hiện tại

`DataStoreManager` quản lý:

| Key | Kiểu | Vai trò |
|---|---|---|
| `is_language_completed` | Boolean | Hoàn thành chọn language onboarding |
| `is_intro_completed` | Boolean | Hoàn thành intro |
| `is_permission_completed` | Boolean | Hoàn thành/skip Permission; key được giữ nguyên nhưng tạm không tham gia onboarding khi `IS_FIRST_PERMISSION_ONBOARDING_ENABLED=false` |
| `key_language` | String | Language code |
| `country_language` | String | Region code |
| `pet_selected_pack_keys` | String | 12 pack key độc lập theo slot, newline-delimited |
| `pet_selected_pack_key` | String | Legacy/mirror slot 1 để migrate dữ liệu cũ |
| `pet_count` | Int | Số instance, clamp theo device budget |
| `pet_store_custom_names` | JSON String | Tên pet đặt từ Pet Store theo owner pet ID; pack đã cài là source of truth mở khóa |
| `pet_display_mode` | String enum | `MIXED` hoặc `SWARM`; hai mode loại trừ nhau |
| `pet_slot_enabled` | JSON String | 12 trạng thái visible độc lập của Mixed |
| `pet_mixed_reward_unlocked_slot_count` | Int | Capacity Mixed đã mở, mặc định 3 và clamp 3–12 |
| `pet_room_selected_id` | Int | Room background user chọn cho My Pet Room; `0` nghĩa là dùng `defaultRoomId` của catalog |
| `pet_room_music_on` | Boolean | Trạng thái nhạc nền My Pet Room; nhạc chỉ phát khi màn đang resume |
| `pet_care_energy` | JSON String | Năng lượng từng pet sau lần cho ăn: `percent` và `updatedAt`; pet chưa từng ăn dùng `pet_care_adopted_at` làm mốc 100% ban đầu |
| `pet_care_adopted_at` | JSON String | Ngày nhận nuôi từng pet; ghi một lần, cài lại pack không reset |
| `pet_food_inventory` | JSON String | Số phần ăn còn giữ theo food ID, clamp 0–99 |
| `pet_swarm_pack_key` | String | Pack được nhân bản trong Swarm |
| `pet_swarm_count` | Int | Số instance Swarm, 1–12 hoặc tối đa 6 trên low-RAM |
| `pet_swarm_reward_unlocked` | Boolean | Rewarded unlock vĩnh viễn trên device |
| `pet_swarm_size_percent`, `pet_swarm_speed_percent` | Int | Base size/speed của Swarm |
| `pet_swarm_randomize_size_speed` | Boolean | Tạo variation deterministic theo instance |
| `pet_swarm_constrain_movement_area` | Boolean | Bật vùng di chuyển tùy chỉnh |
| `pet_swarm_inset_*_percent` | Int | Top/bottom/left/right 0–30%, bước 5% |
| `pet_slot_size_percents` | JSON String | 12 mức size độc lập 50–150%, bước 10% |
| `pet_slot_speed_percents` | JSON String | 12 mức speed độc lập 50–150%, bước 25% |
| `pet_size_percent`, `pet_speed_percent` | Int | Legacy global fallback cho migration |
| `pet_sound_enabled` | Boolean | Opt-in âm thanh khi schema pack hỗ trợ |
| `pet_slot_messages_enabled` | JSON String | Toggle speech độc lập theo slot |
| `pet_slot_custom_messages` | JSON String | 12 message catalog encoded độc lập |
| `pet_slot_interaction_enabled` | JSON String | Toggle tap/drag/fling độc lập theo slot |
| `pet_messages_enabled`, `pet_custom_messages`, `pet_interaction_enabled` | mixed | Legacy global fallback cho migration |
| `pet_last_positions` | String | Đúng 12 record nullable, tọa độ chuẩn hóa 0–1 theo slot |
| `pet_position_reset_revisions` | JSON String | 12 reset revision độc lập |
| `pet_position_reset_revision` | Int | Legacy global fallback cho migration |
| `battery_status_enabled` | Boolean | User đã Apply battery overlay |
| `battery_status_has_applied` | Boolean | Đã từng Apply; điều khiển card Current độc lập với trạng thái bật/tắt |
| `battery_status_selected_theme_id` | Int | Style gốc/legacy theme ID; mặc định user-visible là `1`, `0` chỉ là renderer fallback |
| `battery_status_selected_battery_theme_id` | Int | Theme ID cung cấp asset pin |
| `battery_status_selected_emoji_theme_id` | Int | Theme ID cung cấp asset pet/emoji |
| `battery_status_display_mode` | String enum | Cover hoặc below-system-bar |
| `battery_status_show_time`, `battery_status_show_percentage`, `battery_status_show_animation`, `battery_status_show_date_time` | Boolean | Component visibility |
| `battery_status_*_dp` | Float | Bar/padding/emoji/icon/privacy-reserve geometry |
| `battery_status_*_color` | Int ARGB | Renderer background/foreground |
| `battery_status_animation_asset_name` | String | Một trong 21 GIF hoặc 5 Lottie đã audit |
| `battery_status_data_type` | String enum | Nhãn 2G–9G do user chọn |
| `battery_status_charge_icon_index` | Int | Charge vector 1–12 |
| `battery_status_{wifi,signal,airplane,hotspot,ringer}_icon_style_index` | Int | Family icon 1–4 của từng status component |
| `battery_status_date_format`, `battery_status_date_time_font` | String enum | Định dạng và bundled font ngày |
| `battery_status_background_decoration_id_v2` | Int | Background v2 ID; `0` kích hoạt màu phẳng, ID khác `0` kích hoạt duy nhất theme asset và giữ `backgroundColorArgb` như lựa chọn màu gần nhất; mặc định `1` |
| `battery_status_show_emotion` | Boolean | Hiện emotion trang trí |
| `battery_status_emotion_decoration_id` | Int | Emotion asset ID |
| `battery_status_hidden_app_packages` | String set | Package của app mà user chọn để tạm ẩn Emoji Battery; lưu cục bộ trên thiết bị |
| `battery_status_favorite_theme_ids` | String set | Favorite local |
| `battery_troll_mode` | String enum | `REAL` giữ mức pin thật, `FAKE` ghi số giả lên status bar |
| `battery_troll_fake_percent` | Int | Số phần trăm giả, clamp 0–999 |
| `battery_troll_theme_id` | Int | Theme troll đang dùng; `0` nghĩa là không dùng, artwork quay về theme battery thường |
| `battery_troll_emoji_level_index`, `battery_troll_battery_level_index` | Int | Chỉ số 0–4 trong năm asset của theme, 0 = đầy … 4 = cạn |
| `battery_troll_random_artwork` | Boolean | `true` thì emoji và pin tự xoay vòng theo chu kỳ và user không chọn tay được nữa |

Language được mirror sang SharedPreferences `language_cache` để có thể đọc sớm khi attach locale trước khi DataStore async emit.

`PetPreferences.petSlots` luôn materialize thành 12 `PetSlotPreferences`. Mỗi record sở hữu
`packKey`, size, speed, messages, custom messages, interaction và `isEnabled`; `petCount`
quyết định số slot Mixed đã cấu hình, còn `isEnabled` quyết định slot nào thật sự xuất
hiện. Fresh install có `petCount=0`, 12 slot rỗng/inactive và không tự chọn Orange Cat.
Mọi slot, kể cả pet cuối cùng, đều có thể chuyển sang inactive; khi không còn pet active,
overlay không tạo window nào và service tự dừng.
Ba slot đầu miễn phí. Slot 4–12 mở tuần tự sau earned Rewarded callback khi quảng cáo có
sẵn; nếu Rewarded unavailable thì flow tiếp tục để lỗi quảng cáo không chặn tính năng.
Premium bypass toàn bộ gate. Capacity đã mở được persist độc lập với roster nên remove pet
không làm user phải xem lại quảng cáo của slot đó.
`PetSwarmPreferences` tách riêng pack/count/unlock, size/speed và movement area nên không
ghi đè hồ sơ Mixed. Random variation dùng pack key + instance index làm seed ổn định;
không persist một record riêng cho từng bản sao.

Custom messages vẫn là dữ liệu nhỏ: tối đa 30 câu, 80 Unicode code point/câu.
`PetMessageListPolicy` chuẩn hóa khoảng trắng, bỏ câu rỗng/trùng và cắt theo code point.
Mỗi slot lưu catalog riêng; danh sách rỗng của slot đó dùng catalog có sẵn.

## Pet engine model

- `PetState`: position, velocity, size, usable bounds, action/direction, animation cursor, action timer, deterministic behavior sequence, recent-action memory và pending routine immutable.
- `PetEvent`: tick, tap, drag start/by/end, fling và bounds change.
- `PetTransition`: state mới + effect action/tap/showcase/combo start-complete.
- `PetClip`/`PetFrame`: action timeline version-independent với frame duration và scripted velocity.
- `PetBounds`: clamp top-left position theo kích thước pet, kể cả pet lớn hơn usable area.

Các model nằm trong `pet/engine`, là Kotlin thuần và không chứa bitmap/view/context. Asset-pack metadata production sẽ được map sang các model này ở phase catalog/installer.

## Overlay runtime state

`PetOverlayRuntime.isRunning` và `activePetCount` là process-local `StateFlow`, không phải
persisted preference. `activePetCount` là số slot Mixed visible hoặc swarm count theo mode
hiện hành. Service dùng `START_NOT_STICKY` và không có boot receiver nên trạng thái running
không được restore sau process death/reboot.

## Pet pack model

- `PetPackManifest` là schema v1 versioned gồm identity, canvas, anchor, interaction và action clips/frame metadata.
- `PetPackRepository.packs/selectedPacks` là `StateFlow`; selection có pet thiếu slot được
  materialize một lần từ slot 1 thành 12 giá trị độc lập. Slot rỗng giữ nguyên rỗng. Sentinel
  debug v1 `builtin.orange-cat@1` được migrate về slot rỗng, không trở thành pet của user.
- Installed source chỉ trỏ tới app-private directory sau khi secure installer validate và atomic promote.
- Pet Store coi `installedPackKey` xuất hiện trong `PetPackRepository.packs` là đã mở khóa.
  Sau khi install thành công, repository ghi atomically pack key, `slotEnabled=true` và
  `petCount` vào slot Mixed trống đầu tiên để pet mới mặc định hiện trên màn hình. Roster đầy
  thì không evict selection hiện có; Swarm không thay đổi. Food hiện chỉ có model presentation;
  inventory/coin persistence được bổ sung cùng flow My Pet.
- Pack của controller là snapshot theo từng rebuild. Khi selected key/count thay đổi,
  service preload visual rồi thay controller ngay trong foreground session; invalid/missing
  key bị loại khỏi roster user. Renderer vẫn giữ visual built-in nội bộ làm guard cuối cùng,
  nhưng guard đó không được persist hoặc tính là pet đã sở hữu.
- Android bitmap/`File` không đi vào pure engine state. Manifest được map sang `PetClip`; renderer giữ `PetPackVisual` đã preload.

## Owner catalog model

- `OwnerPetCatalogEntry`: owner ID, name, category, author, thumbnail source, archive URL,
  byte size, SHA-256 và optional `speechAnchor` chuẩn hóa.
- `OwnerPetCatalogSnapshot`: immutable loading/content/error state cùng server catalog version.
- `OwnerPetCatalogRepository`: boundary dùng chung cho UI; production implementation dùng
  private GitHub raw + app-private catalog cache + on-demand verified archive cache.
- Catalog cache gồm `pets.json` cuối hợp lệ và `metadata.json` chứa ETag, thời điểm validation
  gần nhất và rate-limit retry deadline. Cache được đọc trước network; TTL 24 giờ giới hạn
  mỗi device tối đa một catalog revalidation/ngày, còn `304 Not Modified` tránh tải lại body.
  `403`/`429` giữ catalog cũ và chặn retry đến `Retry-After`/`X-RateLimit-Reset` (tối đa 24 giờ).
- Raw ZIP chỉ được normalize khi user bấm `Set`; normalization hiện tạo immutable
  revision `owner.shimeji.<id>@7`, thêm `TALK` từ frame 34–36 khi đủ dữ liệu và copy
  optional `speechAnchor` đã audit từ server vào manifest. Server đánh dấu 631/1.062 pet
  có điểm khuyết tin cậy; 431 pet không có metadata giữ attachment mặc định `(0.5, 0.5)`.
  Trong số unsupported có 36 pack WC 2026 gốc dùng contract 24 frame không có TALK.
  Runtime không dò pixel hoặc tự đoán attachment. Khi Start, owner pack revision cũ được
  enrich trong memory bằng catalog cache theo pet ID; vì vậy không cần download hoặc `Set`
  lại. Catalog authoritative cũng loại anchor heuristic cũ khỏi pet unsupported. Khi map
  vào engine, raw TALK bốn frame được tách tương thích thành TALK đứng yên một frame và
  TALK_WALK bốn frame; manifest app-private không bị mutate.
- Catalog 1.062 item không dùng Room: metadata parse một lần vào memory, filter bằng pure
  policy; binary nằm ngoài APK và chỉ ZIP được chọn mới tải về. Cache JSON cuối hợp lệ dùng
  khi offline; ZIP cache vẫn phải qua secure installer trước khi trở thành installed pack.

## Battery catalog và config

- `BatteryCatalogSnapshot` gồm category/theme, 38 background, 20 emotion server trong
  nhóm Classic + 80 emotion server thuộc tám pack mới, 26 animation,
  entitlement, remote/cache/local asset path, distribution status và typed error;
  built-in theme ID `0` luôn có như fallback runtime nhưng không xuất hiện trong picker.
- Normalized schema v1 chỉ giữ relative path, byte size, SHA-256 và dimension. Ảnh tĩnh
  Battery chấp nhận pixel-exact lossless WebP hoặc PNG; GIF/Lottie giữ nguyên.
  `HybridBatteryCatalogRepository` đọc cache trước, revalidate private GitHub catalog
  theo TTL/ETag/backoff, materialize asset theo nhu cầu và chặn path escape,
  size/hash mismatch hoặc release catalog chưa `APPROVED`.
- GIF trong picker được Coil stream/cache theo viewport. Lottie remote được materialize và
  kiểm tra hash vào cache Battery catalog trước khi parse; UI dùng asset Animation mặc định
  trong lúc tải hoặc khi composition lỗi nên không tạo item trắng.
- Emotion legacy giữ nguyên ID `1..20` để DataStore hiện có không đổi nghĩa. Emotion mới là
  asset server ổn định với ID `21..100`, mỗi pack 10 item theo thứ tự Emoji, Cony,
  Kiiroitori, Molang, Mochi, Tobi, Keroppi và Pochacco. Group chỉ là taxonomy UI;
  persistence tiếp tục lưu leaf `emotionDecorationId`, không lưu group đang browse. Picker
  chỉ tải preview nhẹ; khi chọn mới tải full lossless WebP/PNG, kiểm tra size/SHA-256 và
  cache app-private.
  Background nhóm cũng đọc từ server; release APK không đóng gói 100 emotion này.
- Background v2 ưu tiên 18 frame Figma ở ID `1..18`; 20 nền cũ được re-index thành
  `19..38`. Picker chỉ tải `background_preview`, còn full lossless WebP/PNG chỉ materialize
  sau khi chọn.
  ID `1` có bản giống byte trong `drawable-nodpi` để fresh install và fallback offline vẫn
  có nền mặc định. Vì catalog còn ở debug v1, key DataStore và draft schema được tăng version
  để reset lựa chọn cũ thay vì âm thầm đổi nghĩa ID.
- `BatteryStatusConfig` là persistent source of truth. `BatterySettingsPolicy` clamp
  geometry/color/favorite/reward unlock trước khi ghi và sau khi decode DataStore.
- Wi‑Fi, signal, airplane, hotspot và ringer mỗi nhóm persist `iconStyleIndex` độc lập
  trong khoảng 1–4. Config mới mặc định Wi‑Fi dùng style 2, hotspot dùng style 3; signal,
  airplane và ringer dùng style 1. Runtime vẫn lấy trạng thái thật từ Android; style chỉ
  chọn family drawable. Wi‑Fi/signal off hoặc limited và hotspot pending/error ưu tiên
  icon trạng thái chuyên biệt để không làm sai nghĩa hệ thống.
- Catalog theme là cặp mặc định. Khi mở editor từ một theme, `selectedThemeId`,
  `selectedBatteryThemeId` và `selectedEmojiThemeId` cùng nhận ID đó. Sau đó hai component
  ID được chỉnh độc lập; runtime vẽ pet chồng lên pin tại cùng trailing anchor.
- Fresh config chọn theme server ID `1` cho cả Battery và Emoji. DataStore/draft debug cũ
  lưu ID `0` được normalize sang `1`; khi catalog chưa sẵn sàng renderer vẫn dùng built-in
  ID `0` nội bộ. Emotion, Animation, nhãn Mobile Data và Hotspot mặc định tắt để status bar
  ban đầu không bị chồng nhiều decoration.
- Migration không cần DataStore transaction riêng: nếu hai key component chưa tồn tại,
  repository dùng `battery_status_selected_theme_id` cho cả hai. Draft schema 1 cũng được
  decode theo quy tắc này; schema 2 persist rõ hai ID, schema 3 thêm các status icon style
  và schema 4 giữ trạng thái `hasApplied` trong editor draft.
- `BatteryEditorPreviewSession` là state process-local, không persistent. Nó chỉ bridge
  draft đang edit sang Accessibility service; owner token ngăn editor cũ ghi/clear preview
  của editor mới.
- `battery_status_reward_unlocked_theme_ids` là tập ID theme Premium đã được mở khóa bằng
  Rewarded trên thiết bị. ID `0`/âm bị loại; Premium subscription bypass gate nhưng không
  sửa/xóa tập unlock này. Tập unlock là monotonic: `applyConfig` luôn merge với dữ liệu
  hiện có để một editor draft cũ hoặc hai DataStore edit gần nhau không thể thu hồi reward.
- 898-theme raw snapshot nằm trong `private_data/`, không thuộc source/release artifact.
  Debug build audit và copy snapshot vào generated assets; release không đóng gói catalog
  `REVIEW_REQUIRED`. Xem
  [`tools/BATTERY_DATA_SNAPSHOT.md`](tools/BATTERY_DATA_SNAPSHOT.md).

## Pet room catalog

- `PetRoomCatalogSnapshot` gồm danh sách `PetRoomEntry` (id, name, slug, entitlement,
  background path, thumbnail path), `defaultRoomId`, trạng thái loading và typed error.
  `resolveRoom()` trả về room đang chọn, rồi `defaultRoomId`, rồi room đầu tiên — nên
  catalog đổi hoặc room bị gỡ vẫn không để My Pet Room không có nền.
- Catalog `json/rooms.json` dùng schema v1 riêng, chỉ chứa relative path, byte size,
  SHA-256 và dimension. `RemotePetRoomCatalogRepository` đọc cache trước, revalidate theo
  cùng `PetCatalogRefreshPolicy` (TTL 24h + ETag + rate-limit backoff) như pet/battery,
  materialize asset theo nhu cầu và verify size/SHA-256 trước khi dùng.
- Mỗi room có đúng hai asset: `bg/BG_<id>.webp` full-resolution và `thumb/BG_<id>.webp` bản
  preview nhẹ. `RoomCatalogParser` từ chối catalog nếu thumbnail không nhỏ và nhẹ hơn
  background, nên grid Room không thể vô tình tải ảnh full-size. Release chỉ chấp nhận
  catalog `APPROVED`; debug chấp nhận cả `REVIEW_REQUIRED`.
- Selection persist qua `PetRoomRepository`; catalog và selection tách nhau để đổi catalog
  không làm mất lựa chọn của user.
- Room `1` được đóng gói trong APK (`PetRoomBundledBackground`) và là `defaultRoomId` của
  catalog, nên phòng luôn có nền kể cả lần chạy đầu hoặc khi offline. Background của room
  khác chỉ tải khi user chạm vào card đó, verify SHA-256 xong mới được áp dụng; thumbnail
  vẫn tải sẵn vì chỉ vài chục KB.

## Battery Troll catalog

- Catalog remote thứ tư: `json/battery-troll.json` + asset root `troll/`, đọc qua
  `BatteryTrollServerConfig` với đúng token, TTL 24h, ETag, backoff và verify size/SHA-256
  như ba catalog kia. Release chỉ chấp nhận `APPROVED`; debug chấp nhận cả
  `REVIEW_REQUIRED`. **Không có bundled fallback** — thiếu mạng thì snapshot rỗng kèm
  typed error, vì Battery Troll là feature tuỳ chọn chứ không phải nền tảng của màn nào.
- `BatteryTrollEntry` gồm `id, name, slug, order, entitlement, batteryOrientation` và đúng
  **11 asset**: một thumbnail, năm emoji và năm mức pin. Parser từ chối catalog nếu một
  trong hai mảng không đúng năm phần tử, vì `BatteryTrollPolicy` đánh chỉ số 0–4 và một
  mảng ngắn hơn sẽ thành crash lúc vẽ chứ không phải lỗi hiển thị.
- `batteryOrientation` tồn tại vì theme 4 có vỏ pin dọc trong khi chín theme còn lại nằm
  ngang; renderer cần biết trước để bố trí, không suy ra từ ảnh lúc chạy.
- Hàng preview ghép sẵn trong Figma **không** được ship. Runtime tự chồng
  `emoji[index]` lên `battery[index]`, nên hai lớp luôn cùng một mức và không phải tải
  thêm 50 ảnh.
- Config troll nằm chung trong `BatteryStatusConfig` chứ không có store riêng: Battery
  Troll chỉ đổi *số nào được viết* và *artwork nào được vẽ*, không đổi cách status bar
  được gắn.

## Không có database

Ứng dụng hiện không có Room, entity, DAO hay schema. Khi feature mới cần database:

1. Xác định data ownership, retention và migration policy.
2. Thêm version catalog/build dependency.
3. Tạo entity/DAO/database/repository boundary.
4. Thêm migration/schema test phù hợp trước khi release.
5. Cập nhật file này và architecture docs.

## State rule

- Persistent state ở data layer.
- Screen state ở immutable `UiState` trong ViewModel.
- Ephemeral Compose-only state chỉ dùng cho chi tiết hiển thị cục bộ không cần business ownership.
- Không lưu Activity/NavController/Composable object vào DataStore hoặc ViewModel.
