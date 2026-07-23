# Implementation Progress — Cute Pet Base

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Làm sạch feature Private Browser | Done | Browser/search/clear/storage/download/media/Room/service đã xóa |
| Base onboarding | Done | Splash, Language, Intro, product permission flow |
| Product Home | Done | Overlay access + Start/Stop pet + Settings/Premium |
| Settings/Premium infrastructure | Done | Giữ để tái sử dụng và cập nhật sau |
| Chuẩn hóa tài liệu cho AI agent | Done | Docs phản ánh source và base contract |
| Product foundation cleanup | Done | Branding/copy active đã chuyển sang Cute Pet; giữ package legacy |
| Pure Kotlin pet engine | Done | Deterministic state machine, frame timeline, drag/fling và bounds |
| Overlay foreground service | Done | specialUse FGS, một small-window pet, notification Stop, 30 FPS |
| Product Home/permission flow | Done | Special access + notification permission + Start/Stop policy |
| Pixel 3 XL device verification | Done | API 31 start/stop, notification, launcher overlay, drag/fling, revoke cleanup |
| Pet catalog + validated pack v1 | Done | Secure ZIP import, repository/cache, detail/select, sprite overlay verified on API 31 |
| Competitor technical audit | Done | Catalog/pack/engine/overlay/lifecycle/business/security evidence documented without copying decompiled implementation code |
| Living pet behavior graph | Done | Weighted seeded scheduling, anti-repeat memory, look-up/dangle/jump, timed wall/ceiling/creep exits, gravity fall and full legacy frame rhythm covered by JVM tests |
| Spatial skill choreography V3.2 | Done | 35 paced combos: wall/ceiling parkour, aerial/skill/dance stories, collision-driven transitions and social duet roles |
| Spatial behavior balance V3.3 | Done | Reduced ground basics, two-story climb quota and longer wall/ceiling dwell |
| Wall-to-wall traversal V3.4 | Done | Screen-relative leap, mirrored airborne travel, opposite-wall catch and continued climb choreography |
| Upward wall-to-wall rise V3.5 | Done | Ballistic upward launch with FLUNG pose, higher opposite-wall catch and independent downward/upward combo weights |
| Multi-pet stability V3.6 | Done | Initial social ownership/facing and grounded overlap policy; superseded by opt-in interaction V3.14 |
| Pet speech and dialogue V3.7 | Done | Frame 34–36 TALK pose, localized bubble and tap/skill/social lines; speech ownership superseded by V3.14 |
| Pet message personalization V3.8 | Done | Editable 30-line list, Unicode-safe persistence, random anti-repeat, 48 fallback lines, broader triggers and slower 4.5–8.5s pacing |
| TALK box attachment V3.9 | Done | Frame 34–36 box follows original -64px IE offset, mirrors in front of pet and cannot outlive its TALK pose |
| Pose-gated speech choreography V3.10 | Done | All messages require TALK, combo tone is context-only, silent combos stay silent and carried box is rectangular without a tail |
| Synchronized TALK lifecycle V3.11 | Done | Bubble and frame share TALK enter/exit transitions; independent reading timer removed so neither can finish before the other |
| Adaptive speech layout V3.12 | Done | Glyph-aware 80–260dp × 48–112dp box, balanced four-line wrapping, viewport clamp and explicit 80-code-point Settings limit |
| Stationary/moving speech V3.13 | Done | TALK holds frame 34; TALK_WALK uses 34/35/34/36 at walking speed, legacy packs normalize without reinstall and bubble lifecycle covers both |
| Independent multi-pet interaction V3.14 | Done | Autonomous movers pass through, social invitations are probabilistic/interruptible, each TALK pet owns a box and off-ground speech is rejected |
| Display-off render suspension | Done | Shared clock pauses during doze and resumes without catch-up; service/window cleanup verified on API 31 |
| Owner pet data snapshot | Done | 1,026 packs + thumbnails and 180 custom assets cloned at a pinned commit with CRC/SHA-256 inventory |
| Local owner pet catalog | Done | 1,026 pets, 268 categories, search/thumbnail/Set, on-demand legacy normalization and device-local sync |
| Đổi namespace/application ID | Deferred | Vẫn là `com.asianmobile.privatebrower` theo yêu cầu owner |

## Nguyên tắc cập nhật

Khi hoàn thành một milestone sản phẩm mới, cập nhật file này và `docs/09_IMPLEMENTATION_ROADMAP.md` trong cùng commit. Không phục hồi milestone hoặc trạng thái của Private Browser như feature hiện tại.
