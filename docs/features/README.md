# Current Base Capabilities

Base hiện chỉ giữ các capability hỗ trợ sau:

| Capability | Source chính | Trạng thái |
|---|---|---|
| Onboarding persistence | `DataStoreManager`, `MainViewModel` | Active |
| Language selection | `ui/language`, `LanguageUtil` | Active |
| Permission request shell | `ui/permission`, `utils/permission` | Active, cần đánh giá lại theo product |
| Search engine preference | `SearchEngineRepository`, picker sheet | Active trong Settings, có thể thay sau |
| Clear browsing data sample | `ClearBrowsingDataUseCase`, clear sheet | Active trong Settings, không có Room/browser engine |
| Rating/feedback | Settings + feedback helpers | Active |
| Premium/billing | `ui/premium`, billing infrastructure | Active |
| Ads/remote config | module `:ads` | Active |

Không có browser core, tabs, bookmarks/history, download manager, file/media manager, database hoặc background service.

Khi domain feature Cute Pet được chốt, tạo spec mới theo tên feature thực tế và cập nhật index này. Không tái sử dụng spec Private Browser cũ.
