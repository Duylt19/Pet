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
| `pet_size_percent` | Int | 75–150%, bước 25% |
| `pet_speed_percent` | Int | 50–150%, bước 25% |
| `pet_sound_enabled` | Boolean | Opt-in âm thanh khi schema pack hỗ trợ |
| `pet_messages_enabled` | Boolean | Cho phép reaction/chat/dialogue bubble |
| `pet_custom_messages` | String | Danh sách lời thoại tùy chỉnh, mỗi record một dòng |
| `pet_interaction_enabled` | Boolean | Cho phép tap/drag/fling |
| `pet_last_positions` | String | Tối đa 3 cặp tọa độ chuẩn hóa 0–1 |
| `pet_position_reset_revision` | Int | Guard để Reset position không bị session cũ ghi đè khi Stop |

Language được mirror sang SharedPreferences `language_cache` để có thể đọc sớm khi attach locale trước khi DataStore async emit.

`pet_custom_messages` là dữ liệu nhỏ có giới hạn nên vẫn dùng Preferences DataStore:
tối đa 30 câu, 80 Unicode code point/câu. `PetMessageListPolicy` chuẩn hóa khoảng
trắng, bỏ câu rỗng/trùng, cắt theo code point để không làm vỡ emoji và dùng chuỗi rỗng
để biểu diễn fallback về catalog có sẵn. Khi danh sách có dữ liệu, nó thay catalog mặc
định cho mọi tone ở session pet kế tiếp.

## Pet engine model

- `PetState`: position, velocity, size, usable bounds, action/direction, animation cursor, action timer, deterministic behavior sequence, recent-action memory và pending routine immutable.
- `PetEvent`: tick, tap, drag start/by/end, fling và bounds change.
- `PetTransition`: state mới + effect action/tap/showcase/combo start-complete.
- `PetClip`/`PetFrame`: action timeline version-independent với frame duration và scripted velocity.
- `PetBounds`: clamp top-left position theo kích thước pet, kể cả pet lớn hơn usable area.

Các model nằm trong `pet/engine`, là Kotlin thuần và không chứa bitmap/view/context. Asset-pack metadata production sẽ được map sang các model này ở phase catalog/installer.

## Overlay runtime state

`PetOverlayRuntime.isRunning` và `activePetCount` là process-local `StateFlow`, không phải persisted preference. Service dùng `START_NOT_STICKY` và không có boot receiver nên trạng thái running không được restore sau process death/reboot. `HomeUiState` kết hợp runtime state, persisted settings và permission snapshot.

## Pet pack model

- `PetPackManifest` là schema v1 versioned gồm identity, canvas, anchor, interaction và action clips/frame metadata.
- `PetPackRepository.packs/selectedPacks` là `StateFlow`; selection thiếu slot được materialize một lần từ slot 1 thành ba giá trị độc lập, và built-in Orange Cat luôn là fallback khi key không còn hợp lệ.
- Installed source chỉ trỏ tới app-private directory sau khi secure installer validate và atomic promote.
- Pack đang chạy vẫn là snapshot theo từng slot. Selection/settings mới chỉ áp dụng ở lần Start tiếp theo để không mutate renderer giữa session.
- Android bitmap/`File` không đi vào pure engine state. Manifest được map sang `PetClip`; renderer giữ `PetPackVisual` đã preload.

## Owner catalog model

- `OwnerPetCatalogEntry`: owner ID, name, category, author, optional local thumbnail path và archive availability.
- `OwnerPetCatalogSnapshot`: immutable loading/content/error state cùng app-specific local root.
- `OwnerPetCatalogRepository`: boundary dùng chung cho local test source hiện tại và network/cache source tương lai.
- Raw ZIP chỉ được normalize khi user bấm `Set`; normalization hiện tạo immutable
  revision `owner.shimeji.<id>@4`, thêm `TALK` từ frame 34–36 khi đủ dữ liệu và persist
  qua selection của slot đích trong `pet_selected_pack_keys`. Revision cũ đã cài vẫn đọc
  được; thao tác `Set` mới chọn revision 4. Khi map vào engine, raw TALK bốn frame được tách tương thích thành TALK
  đứng yên một frame và TALK_WALK bốn frame; manifest app-private không bị mutate.
- Catalog 1,026 item không dùng Room trong local test: metadata parse một lần vào memory, filter 1,026 record bằng pure policy; binary vẫn nằm ngoài APK/Git.

## Không có database

Base hiện không có Room, entity, DAO hay schema. Khi feature mới cần database:

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
