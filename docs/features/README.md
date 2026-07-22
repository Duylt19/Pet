# Current Base Capabilities

Base hiện chỉ giữ các capability hỗ trợ sau:

| Capability | Source chính | Trạng thái |
|---|---|---|
| Onboarding persistence | `DataStoreManager`, `MainViewModel` | Active |
| Language selection | `ui/language`, `LanguageUtil` | Active |
| Permission shell | `ui/permission` | Active, chưa request overlay special access |
| Rating/feedback | Settings + feedback helpers | Active |
| Premium/billing | `ui/premium`, billing infrastructure | Active |
| Ads/remote config | module `:ads` | Active |
| Pure pet engine | `pet/engine` | Active, JVM-tested; chưa nối Android overlay |

Không có browser core, search/clear-browsing, broad storage access, tabs, bookmarks/history, download manager, file/media manager, database hoặc background service.

Khi domain feature Cute Pet được chốt, tạo spec mới theo tên feature thực tế và cập nhật index này. Không tái sử dụng spec Private Browser cũ.
