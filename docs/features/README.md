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
| Pure pet engine | `pet/engine` | Active, JVM-tested |
| One-pet overlay | `pet/overlay` | Active platform layer; chưa có Home/Permission entry |

Không có browser core, search/clear-browsing, broad storage access, tabs, bookmarks/history, download manager, file/media manager hoặc database. Background component duy nhất là `PetOverlayService` do user chủ động start, `START_NOT_STICKY` và không có boot receiver.

Chi tiết platform contract: [PET_OVERLAY.md](PET_OVERLAY.md). Không tái sử dụng spec Private Browser cũ.
