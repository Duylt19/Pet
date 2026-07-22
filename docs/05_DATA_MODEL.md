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

Language được mirror sang SharedPreferences `language_cache` để có thể đọc sớm khi attach locale trước khi DataStore async emit.

## Pet engine model

- `PetState`: position, velocity, size, usable bounds, action, direction và animation cursor immutable.
- `PetEvent`: tick, tap, drag start/by/end, fling và bounds change.
- `PetTransition`: state mới + effect (`ActionChanged`, `Tapped`).
- `PetClip`/`PetFrame`: action timeline version-independent với frame duration và scripted velocity.
- `PetBounds`: clamp top-left position theo kích thước pet, kể cả pet lớn hơn usable area.

Các model nằm trong `pet/engine`, là Kotlin thuần và không chứa bitmap/view/context. Asset-pack metadata production sẽ được map sang các model này ở phase catalog/installer.

## Overlay runtime state

`PetOverlayRuntime.isRunning` là process-local `StateFlow<Boolean>`, không phải persisted preference. Service dùng `START_NOT_STICKY` và không có boot receiver nên trạng thái running không được restore sau process death/reboot ở MVP. `HomeUiState` kết hợp runtime state với overlay/notification permission snapshot.

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
