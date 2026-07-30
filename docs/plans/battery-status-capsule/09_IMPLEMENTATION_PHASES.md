# 09 — Implementation Phases

> **IMPLEMENTATION IN PROGRESS**

Source ngày 2026-07-30 đã hoàn thành vertical slice debug: normalized local catalog,
generated debug assets, DataStore config, Catalog/Editor và Accessibility cover backend.
Các phase bên dưới vẫn là release roadmap; trạng thái thực tế được ghi ở từng phase.

Mỗi phase là một commit/PR độc lập, compile/test/docs pass trước phase sau. Không đánh dấu
`IMPLEMENTATION_PROGRESS.md` Done cho đến khi device verification tương ứng hoàn tất.

## Phase 0 — Product decisions and asset provenance

**Status: Partial.** Owner đã chọn Accessibility cover direction và Home Battery entry.
Raw snapshot vẫn `REVIEW_REQUIRED`; Figma/asset-license inventory và release policy approval
chưa hoàn tất.

### Scope

- Owner duyệt `Battery Status Capsule` và release scope:
  `BELOW_SYSTEM_BAR`, `COVER_SYSTEM_BAR` hoặc dual mode.
- Duyệt Home bottom-nav change.
- Chốt MVP components, landscape behavior và ad placement.
- Chuẩn bị một built-in theme, battery icon set, emoji và background do project sở hữu.
- Tạo license/provenance inventory.

### Deliverables

- Approved wireframe/Figma.
- Asset manifest/checksum.
- Final strings/terminology và Accessibility disclosure draft nếu ship cover mode.
- Decision log cho Rewarded unavailable policy.

### Definition of done

Không còn quyết định sản phẩm làm thay đổi architecture hoặc permission surface. Cover
mode chưa được code nếu owner chưa duyệt policy/disclosure direction.

## Phase 1 — Domain, policies and local persistence

**Status: Implemented for current vertical slice.** Model/repository/parser/DataStore,
sanitization policy, Hilt và JVM tests đã có. Layout priority/RTL migration test nâng cao
còn thuộc hardening.

### Scope

- Models `StatusCapsuleConfig`, catalog, device state.
- `CapsuleDisplayMode`, runtime capability và backend selection policy.
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

**Status: Partial.** Catalog/category/favorite/Premium gate, editor cho theme/màu/kích
thước/background/emotion, navigation, Home entry và analytics đã có. Generic per-component
editor cho trạng thái hệ thống, dirty-draft confirmation, Compose/golden/RTL tests chưa có.

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

## Phase 3 — Standard overlay host and basic runtime

**Status: Superseded for current slice.** Owner ưu tiên Accessibility cover. Chưa migrate
`PetOverlayService` hoặc triển khai standard `TYPE_APPLICATION_OVERLAY` below-bar backend.

### Scope

- Migrate `PetOverlayService` lifecycle thành `OverlayHostService`.
- Giữ pet behavior/regression.
- Add `ApplicationOverlayCapsuleController` one-window renderer.
- Add `StatusCapsuleRuntimeCoordinator` và `ApplicationOverlayCapsuleBackend`.
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

## Phase 4 — Accessibility status-cover backend

**Status: Implemented in debug, device/policy verification pending.** Service metadata,
disclosure, non-touchable window, battery/time renderer, keyguard/screen-off/landscape hide
đã có. OEM matrix, notification-shade/privacy-indicator proof và Play release evidence chưa
hoàn tất.

### Entry gate

- Owner chọn ship cover/dual mode.
- Disclosure/consent copy và Privacy impact được duyệt.
- Play policy justification được review nội bộ.

### Scope

- `StatusBarAccessibilityService`.
- Minimal service metadata: no node retrieval/automation, `isAccessibilityTool=false`.
- `AccessibilityCapsuleBackend` dùng renderer/layout chung.
- Enable/decline/disable flow và runtime capability.
- Top/cutout positioning, non-touchable swipe-through.
- Keyguard/landscape hide, notification shade best-effort behavior.
- Backend switch invariant: exactly one capsule window.

### Tests

- API/OEM layer proof trên device matrix.
- Status swipe, notification shade, keyguard và secure dialogs.
- Camera/microphone privacy indicator và system warning remain visible.
- Service enable/disable/unbind/process restart.
- No accessibility content/data/gesture APIs.

### Definition of done

Cover mode che trực quan phần được phép của status bar trên supported device matrix, không
chặn system gesture/privacy indicator, không đọc accessibility content và release evidence
sẵn sàng cho Play declaration.

Nếu gate không đạt, phase bị loại khỏi release và below-bar mode vẫn hoạt động.

## Phase 5 — Full device-status components

**Status: Partial.** Current runtime có time, percentage, charging, theme battery/emoji
và background/emotion trang trí. Trạng thái thiết bị còn lại chưa triển khai.

### Scope

- Connectivity transport, airplane, ringer, date.
- Wi‑Fi/signal connected-state styles.
- Decorative animation assets.
- Emotion/emoji animation theo trạng thái.
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

## Phase 6 — Remote catalog and secure asset packs

**Status: Local tooling implemented; production source not started.** Snapshot tool audit
size/hash/dimension, generated debug APK assets và optional ADB sync đã có. Owner
endpoint/download/cache/kill switch cần asset approval trước.

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

## Phase 7 — Premium, Rewarded and approved ads

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

## Phase 8 — Release hardening

### Scope

- OEM/API/cutout/orientation matrix.
- Accessibility/localization.
- Performance/battery profiling.
- Play FGS/overlay disclosure, Data Safety và asset licensing.
- Accessibility declaration/video/policy approval nếu cover mode enabled.
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
  ↓
Phase 8
```

Phase 4 là optional release branch nếu chỉ ship below-bar mode. Phase 6 có thể bắt đầu
server tooling sau Phase 1 schema lock, nhưng app integration không merge trước Phase 3
runtime fallback ổn định.
