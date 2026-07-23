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
| Pure pet engine | `pet/engine` | Active, JVM-tested; weighted behavior + multi-action routines |
| Multi-pet overlay | `pet/overlay` | Active, 1–3 pack khác nhau theo slot + tối đa một speech window cho mỗi pet với shared clock/service |
| Validated pet packs | `pet/pack`, `ui/catalog` | Active, schema v1 + secure import + preview/select |
| Owner pet catalog | `OwnerPetCatalogRepository`, `ui/catalog` | Active local test source, 1,026 pets + search/category/on-demand Set |
| Pet speech and dialogue | `pet/speech`, transient speech overlay | Active, localized/custom reactions + lifecycle độc lập theo pet |
| Pet settings persistence | `PetSettingsRepository`, DataStore | Active, selection theo slot/count/size/speed/message list/interaction/position/reset guard |

Không có browser core, search/clear-browsing, broad storage access, tabs, bookmarks/history, download manager, file/media manager hoặc database. Background component duy nhất là `PetOverlayService` do user chủ động start, `START_NOT_STICKY` và không có boot receiver.

Chi tiết: [PET_OVERLAY.md](PET_OVERLAY.md), [PET_BEHAVIOR_V2.md](PET_BEHAVIOR_V2.md), [PET_SPEECH.md](PET_SPEECH.md), [PET_PACKS.md](PET_PACKS.md), [PET_SETTINGS.md](PET_SETTINGS.md), [OWNER_PET_CATALOG.md](OWNER_PET_CATALOG.md). Không tái sử dụng spec Private Browser cũ.
