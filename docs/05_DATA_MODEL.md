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
| `selected_search_engine` | String | Search engine trong Settings |
| `runtime_permission_request_count_<permission>` | Int | Số lần request từng runtime permission |

Language được mirror sang SharedPreferences `language_cache` để có thể đọc sớm khi attach locale trước khi DataStore async emit.

## Model/repository hiện tại

- `SearchEngine`: danh sách lựa chọn search engine còn được Settings sử dụng.
- `SearchEngineRepository`: contract quan sát/lưu lựa chọn.
- `SearchEngineRepositoryImpl`: implementation dựa trên DataStore.
- `ClearBrowsingDataUseCase`: clear CookieManager/WebStorage/WebView cache-history mẫu; không phụ thuộc browser engine hoặc Room.

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
