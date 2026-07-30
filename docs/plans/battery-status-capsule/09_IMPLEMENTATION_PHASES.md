# 09 — Implementation Phases

> **PLANNED — NOT IMPLEMENTED**

Mỗi phase là một commit/PR độc lập, compile/test/docs pass trước phase sau. Không đánh dấu
`IMPLEMENTATION_PROGRESS.md` Done cho đến khi device verification tương ứng hoàn tất.

## Phase 0 — Product decisions and asset provenance

### Scope

- Owner duyệt `Battery Status Capsule` và vị trí dưới system status bar.
- Duyệt Home bottom-nav change.
- Chốt MVP components, landscape behavior và ad placement.
- Chuẩn bị một built-in theme, battery icon set, emoji và background do project sở hữu.
- Tạo license/provenance inventory.

### Deliverables

- Approved wireframe/Figma.
- Asset manifest/checksum.
- Final strings/terminology.
- Decision log cho Rewarded unavailable policy.

### Definition of done

Không còn quyết định sản phẩm làm thay đổi architecture hoặc permission surface.

## Phase 1 — Domain, policies and local persistence

### Scope

- Models `StatusCapsuleConfig`, catalog, device state.
- Pure sanitization/layout/entitlement/draft policies.
- Dedicated settings repository + DataStore.
- Built-in local catalog parser.
- Hilt bindings.

### Tests

- Config default/migration/corrupt input.
- All value bounds.
- Layout priority/narrow width/RTL.
- Draft/apply transitions.
- Entitlement.

### Docs

- Update `02_ARCHITECTURE.md`, `05_DATA_MODEL.md`, `03_TECH_STACK.md` nếu dependency đổi.

### Definition of done

Config round-trip deterministic; no Android View/service required; JVM tests pass.

## Phase 2 — Catalog and editor UI with deterministic preview

### Scope

- Battery Catalog + Favorites + built-in theme states.
- Full Editor.
- Generic Component Editor and Asset Catalog.
- Shared Compose preview using pure layout policy.
- Navigation/routes/back dirty-state.
- Home bottom navigation.
- Analytics screen names.
- No production overlay start yet; Apply persists config and shows preview-only milestone.

### Tests

- Compose states, navigation, accessibility, RTL, large font.
- Screenshot/golden for reference states.

### Docs

- Update navigation, current screens only for screens actually shipped, UI design and tracking.

### Definition of done

User can configure every MVP field, process-death restore draft và Apply local config.

## Phase 3 — Shared overlay host and basic runtime

### Scope

- Migrate `PetOverlayService` lifecycle thành `OverlayHostService`.
- Giữ pet behavior/regression.
- Add `StatusCapsuleController` one-window renderer.
- Implement time, battery percentage/icon and charging state.
- Permission/notification pending Apply flow.
- Independent Pet/Capsule Start/Stop + dynamic notification.
- Screen-off pause and revoke cleanup.

### Tests

- Shared host state machine.
- Battery/time mapping.
- Window lifecycle/touch-through.
- Pet-only/capsule-only/both/stop-one.
- Pixel 3 XL smoke test.

### Docs

- Update architecture, overlay feature docs, permission, navigation and Play declaration draft.

### Definition of done

Capsule chạy ổn định qua app khác, dữ liệu pin/thời gian thật, không block touch, pet không
regression và Stop sạch.

## Phase 4 — Full device-status components

### Scope

- Connectivity transport, airplane, ringer, date.
- Wi‑Fi/signal connected-state styles.
- Decorative animation assets.
- Emotion/emoji.
- Data label manual semantics.
- API 36 hotspot callback + honest fallback.
- Shared frame clock cho animation assets.

### Tests

- Callback registration/cleanup.
- All known/unknown state fallbacks.
- Connectivity transitions.
- Timezone/locale/date rollover.
- CPU/memory/FPS profile.

### Definition of done

Mọi component trong MVP có real-data/decorative semantics đúng tài liệu và không cần
sensitive permission mới.

## Phase 5 — Remote catalog and secure asset packs

### Scope

- Owner-controlled endpoint.
- Cache-first catalog, TTL/ETag/rate-limit.
- Authenticated thumbnails if private.
- Verified on-demand download.
- Secure pack installer + bitmap cache.
- Category/search/favorites/view-all.
- Remote kill switch.

### Tests

- Parser/cache/network error matrix.
- Hash/size/security corpus.
- Offline starter/cached catalog.

### Definition of done

Remote content fail không phá applied capsule; invalid pack không vào renderer.

## Phase 6 — Premium, Rewarded and approved ads

### Scope

- Free/Rewarded/Premium entitlement.
- Rewarded exactly-once flow.
- Premium return/resume.
- Approved native/banner placements in `:ads`.
- App Open suppression and analytics funnel.

### Tests

- Earned/dismissed/unavailable.
- Premium bypass/refresh/billing failure.
- Ad fail collapse/no navigation dead-end.

### Docs

- Update `07_ADS_INTEGRATION.md`, remote config inventory và tracking.

### Definition of done

Monetization không chặn free starter experience, không xuất hiện trong overlay và không
phá Apply/navigation.

## Phase 7 — Release hardening

### Scope

- OEM/API/cutout/orientation matrix.
- Accessibility/localization.
- Performance/battery profiling.
- Play FGS/overlay disclosure, Data Safety và asset licensing.
- ProGuard/release build.
- Staged rollout/kill switch/monitoring.

### Definition of done

- Device matrix recorded.
- No fatal/ANR/window leak.
- Performance budget met.
- Policy declaration/video complete.
- Source, tests, docs và release configuration đồng bộ.

## Dependency graph

```text
Phase 0
  ↓
Phase 1
  ↓
Phase 2
  ↓
Phase 3
  ↓
Phase 4
  ↓
Phase 5
  ↓
Phase 6
  ↓
Phase 7
```

Phase 5 có thể bắt đầu server tooling sau Phase 1 schema lock, nhưng app integration không
merge trước Phase 3 runtime fallback ổn định.
