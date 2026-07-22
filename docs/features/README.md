# Current Base Capabilities

Base hiện chỉ giữ các capability hỗ trợ sau:

| Capability | Source chính | Trạng thái |
|---|---|---|
| Onboarding persistence | `DataStoreManager`, `MainViewModel` | Active |
| Language selection | `ui/language`, `LanguageUtil` | Active |
| Product permission | `ui/permission` | Active overlay special access + API 33 notification request, có Skip |
| Rating/feedback | Settings + feedback helpers | Active |
| Premium/billing | `ui/premium`, billing infrastructure | Active |
| Ads/remote config | module `:ads` | Active |
| Pure pet engine | `pet/engine` | Active, JVM-tested |
| One-pet overlay | `pet/overlay` | Active, được điều khiển từ Home |
| Validated pet packs | `pet/pack`, `ui/catalog` | Active, schema v1 + secure import + preview/select |

Không có browser core, search/clear-browsing, broad storage access, tabs, bookmarks/history, download manager, file/media manager hoặc database. Background component duy nhất là `PetOverlayService` do user chủ động start, `START_NOT_STICKY` và không có boot receiver.

Chi tiết: [PET_OVERLAY.md](PET_OVERLAY.md), [PET_PACKS.md](PET_PACKS.md). Không tái sử dụng spec Private Browser cũ.
