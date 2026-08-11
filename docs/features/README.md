# Current Product Capabilities

Ứng dụng hiện có các capability sau:

| Capability | Source chính | Trạng thái |
|---|---|---|
| Onboarding persistence | `DataStoreManager`, `MainViewModel` | Active |
| Language selection | `ui/onboarding/language`, `LanguageUtil` | Active |
| Product permission | `ui/onboarding/permission` | Active overlay special access + API 33 notification request, có Skip |
| Rating/feedback | Settings + feedback helpers | Active |
| Premium/billing | `ui/premium`, billing infrastructure | Active |
| Ads/remote config | module `:ads` | Active |
| Pure pet engine | `pet/engine` | Active, JVM-tested; weighted behavior + multi-action routines |
| Multi-pet overlay | `pet/overlay` | Active, Mixed 1–12 pack/visibility độc lập hoặc Swarm 1–12 bản sao cùng pack, shared clock/service |
| Validated pet packs | `pet/pack`, `ui/pet/catalog` | Active, schema v1 + secure import + preview/select |
| Owner pet catalog | `OwnerPetCatalogRepository`, `data/remote`, `ui/pet/catalog` | Active private GitHub raw source, 1.062 pets including 36 original WC 2026 packs/864 frames + cached metadata + authenticated thumbnail + verified on-demand ZIP Set |
| Pet speech and dialogue | `pet/speech`, transient speech overlay | Active, localized/custom reactions + lifecycle độc lập theo pet |
| Pet settings persistence | `PetSettingsRepository`, DataStore | Active, `PetSlotPreferences` độc lập cho selection/size/speed/message list/interaction và position/reset guard theo slot |
| Battery status capsule | `BatteryCatalogRepository`, `ui/battery`, `battery/overlay` | Debug vertical slice active; Accessibility cover + local audited catalog, release còn policy/license/device gate |

Không có browser core, search/clear-browsing, broad storage access, tabs, bookmarks/history,
download manager, file/media manager hoặc database. `PetOverlayService` do user chủ động
start, dùng `START_NOT_STICKY`; `StatusBarAccessibilityService` chỉ chạy sau khi user tự bật
trong system settings. Cả hai đều không có boot receiver.

Chi tiết: [PET_OVERLAY.md](PET_OVERLAY.md), [PET_BEHAVIOR_V2.md](PET_BEHAVIOR_V2.md), [PET_SPEECH.md](PET_SPEECH.md), [PET_PACKS.md](PET_PACKS.md), [PET_SETTINGS.md](PET_SETTINGS.md), [OWNER_PET_CATALOG.md](OWNER_PET_CATALOG.md), [BATTERY_STATUS.md](BATTERY_STATUS.md). Không tái sử dụng spec Private Browser cũ.
