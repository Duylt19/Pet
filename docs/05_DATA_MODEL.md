# 05 — Data and State Model

## DataStore hiện tại

`DataStoreManager` quản lý:

| Key | Kiểu | Vai trò |
|---|---|---|
| `is_language_completed` | Boolean | Hoàn thành chọn language onboarding |
| `is_intro_completed` | Boolean | Hoàn thành intro |
| `is_permission_completed` | Boolean | Hoàn thành/skip permission step |
| `key_language` | String | Language code |
| `country_language` | String | Region code |
| `pet_selected_pack_keys` | String | Ba pack key độc lập theo slot, newline-delimited |
| `pet_selected_pack_key` | String | Legacy/mirror slot 1 để migrate dữ liệu cũ |
| `pet_count` | Int | Số instance, clamp theo device budget |
| `pet_display_mode` | String enum | `MIXED` hoặc `SWARM`; hai mode loại trừ nhau |
| `pet_slot_enabled` | JSON String | Ba trạng thái visible độc lập của Mixed |
| `pet_swarm_pack_key` | String | Pack được nhân bản trong Swarm |
| `pet_swarm_count` | Int | Số instance Swarm, 1–12 hoặc tối đa 6 trên low-RAM |
| `pet_swarm_reward_unlocked` | Boolean | Rewarded unlock vĩnh viễn trên device |
| `pet_swarm_size_percent`, `pet_swarm_speed_percent` | Int | Base size/speed của Swarm |
| `pet_swarm_randomize_size_speed` | Boolean | Tạo variation deterministic theo instance |
| `pet_swarm_constrain_movement_area` | Boolean | Bật vùng di chuyển tùy chỉnh |
| `pet_swarm_inset_*_percent` | Int | Top/bottom/left/right 0–30%, bước 5% |
| `pet_slot_size_percents` | JSON String | Ba mức size độc lập 50–150%, bước 10% |
| `pet_slot_speed_percents` | JSON String | Ba mức speed độc lập 50–150%, bước 25% |
| `pet_size_percent`, `pet_speed_percent` | Int | Legacy global fallback cho migration |
| `pet_sound_enabled` | Boolean | Opt-in âm thanh khi schema pack hỗ trợ |
| `pet_slot_messages_enabled` | JSON String | Toggle speech độc lập theo slot |
| `pet_slot_custom_messages` | JSON String | Ba message catalog encoded độc lập |
| `pet_slot_interaction_enabled` | JSON String | Toggle tap/drag/fling độc lập theo slot |
| `pet_messages_enabled`, `pet_custom_messages`, `pet_interaction_enabled` | mixed | Legacy global fallback cho migration |
| `pet_last_positions` | String | Đúng 3 record nullable, tọa độ chuẩn hóa 0–1 theo slot |
| `pet_position_reset_revisions` | JSON String | Ba reset revision độc lập |
| `pet_position_reset_revision` | Int | Legacy global fallback cho migration |

Language được mirror sang SharedPreferences `language_cache` để có thể đọc sớm khi attach locale trước khi DataStore async emit.

`PetPreferences.petSlots` luôn materialize thành ba `PetSlotPreferences`. Mỗi record sở hữu
`packKey`, size, speed, messages, custom messages, interaction và `isEnabled`; `petCount`
quyết định số slot Mixed đã cấu hình, còn `isEnabled` quyết định slot nào thật sự xuất
hiện. Mixed luôn giữ tối thiểu một pet visible; global Start/Stop là cách tắt toàn bộ.
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
- `PetPackRepository.packs/selectedPacks` là `StateFlow`; selection thiếu slot được materialize một lần từ slot 1 thành ba giá trị độc lập, và built-in Orange Cat luôn là fallback khi key không còn hợp lệ.
- Installed source chỉ trỏ tới app-private directory sau khi secure installer validate và atomic promote.
- Pack của controller là snapshot theo từng rebuild. Khi selected key/count thay đổi,
  service preload visual rồi thay controller ngay trong foreground session; invalid/missing
  key vẫn fallback built-in và không đưa file chưa validate vào renderer.
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
  optional `speechAnchor` đã audit từ server vào manifest. Server đánh dấu 631/1.026 pet
  có điểm khuyết tin cậy; 395 pet không có metadata giữ attachment mặc định `(0.5, 0.5)`.
  Runtime không dò pixel hoặc tự đoán attachment. Khi Start, owner pack revision cũ được
  enrich trong memory bằng catalog cache theo pet ID; vì vậy không cần download hoặc `Set`
  lại. Catalog authoritative cũng loại anchor heuristic cũ khỏi pet unsupported. Khi map
  vào engine, raw TALK bốn frame được tách tương thích thành TALK đứng yên một frame và
  TALK_WALK bốn frame; manifest app-private không bị mutate.
- Catalog 1.026 item không dùng Room: metadata parse một lần vào memory, filter bằng pure
  policy; binary nằm ngoài APK và chỉ ZIP được chọn mới tải về. Cache JSON cuối hợp lệ dùng
  khi offline; ZIP cache vẫn phải qua secure installer trước khi trở thành installed pack.

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
