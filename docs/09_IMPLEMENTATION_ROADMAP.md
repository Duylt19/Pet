# 09 — Cute Pet Implementation Roadmap

Roadmap này theo dõi quá trình phát triển app Cute Pet/Shimeji chạy nổi trên màn hình.
Mỗi phase là một commit độc lập, phải compile/test/docs pass trước khi chuyển phase.

## Quyết định đã chốt

- Dùng namespace/application ID canonical `com.asianmobile.emojibattery.shimeji`.
- Giữ module `:ads`, chưa thêm placement mới.
- MVP dùng asset pet demo do project sở hữu; snapshot pet được owner ủy quyền đã được import
  vào private GitHub static server bằng catalog version/SHA-256; không copy code decompile.
- Chưa dùng Room; persistence nhỏ dùng DataStore.
- Runtime tối đa 3 pet theo device budget, không auto-start sau boot.

## Phase 0 — Product foundation cleanup — Done

- Xóa search engine, clear browsing data, default-browser helper và broad storage permission legacy.
- Thu gọn Permission thành product-neutral shell; Settings chỉ giữ language/share/rate/feedback/version.
- Đổi active splash/intro/premium/share/feedback copy sang Cute Pet.
- Locale chưa có bản dịch Cute Pet fallback về English thay vì hiện branding browser cũ.

Definition of done: Manifest không còn storage permission; source active không phụ thuộc browser/storage capability; compile và unit test pass.

## Phase 1 — Pure Kotlin pet engine — Done

- Model immutable cho pet pose, direction, action, position, velocity và screen bounds.
- Reducer/state machine nhận event tick/tap/drag/fling/bounds-change và trả state/effect xác định.
- Frame timeline hỗ trợ duration theo frame, loop/non-loop và action transition.
- Constraint/clamp dùng playground cục bộ đã chuẩn hóa inset, cho phép edge overflow có chủ đích và vẫn an toàn khi đổi orientation/inset.
- Unit test transition, frame timing, drag/fling và bounds; tuyệt đối không phụ thuộc Android UI.

Definition of done: engine chạy deterministic trong JVM tests và không import `android.*`.

## Phase 2 — One-pet overlay foreground service — Done

- Khai báo `SYSTEM_ALERT_WINDOW`, foreground-service permission/type phù hợp và service `exported=false`.
- Tạo notification channel + ongoing notification có Stop action; service gọi `startForeground` đúng thời hạn.
- Mỗi pet là một `TYPE_APPLICATION_OVERLAY` window trong MVP; window trong suốt và chỉ chiếm pet bounds.
- Render clock duy nhất khoảng 30 FPS; decode/cache asset ngoài frame loop; không tạo thread/window theo frame.
- Gesture adapter chuyển tap/drag/fling thành engine events, update position bằng `WindowManager.updateViewLayout`.
- Dừng sạch renderer, window và notification khi user stop hoặc service destroy.

Definition of done: một pet hiển thị ổn định trên app khác, drag/tap/fling được, không rò window/service sau stop.

## Phase 3 — Product Home and overlay permission flow — Done

- Thay Home placeholder bằng trạng thái pet selected/running/stopped và CTA Start/Stop.
- Kiểm tra `Settings.canDrawOverlays`; mở `ACTION_MANAGE_OVERLAY_PERMISSION` theo package URI và refresh ở `ON_RESUME`.
- Giải thích rõ lý do overlay + foreground notification; Start chỉ được phép sau khi special access được cấp.
- Handle revoked permission, process restart và service start failure bằng UiState rõ ràng.
- Chưa tự auto-start sau boot.

Definition of done: first-run → permission → start pet → stop pet là một vertical slice hoàn chỉnh, có analytics và test policy/state.

## Phase 4 — Pet catalog and validated asset packs — Done

- Định nghĩa schema versioned cho pack manifest, action clips, frame rect/duration, anchor và interaction metadata.
- Installer copy pack vào app-private storage theo staging → validate → atomic promote; chống path traversal/zip bomb/file type sai.
- Repository expose built-in/demo pack và installed packs; cache metadata/bitmap theo budget.
- Catalog/pet detail/preview UI; lỗi pack không làm crash overlay đang chạy.

Definition of done: cài và chọn được pack hợp lệ, reject pack lỗi an toàn, renderer không parse disk data mỗi frame.

Verified trên Pixel 3 XL / API 31 với `Sunny Cat` sample pack: system picker → secure install → catalog/detail/select → sprite overlay → drag/fling → clean stop. Parser/validator/archive guardrails có JVM tests; bitmap được preload qua bounded `LruCache` trước frame loop.

## Phase 5 — Multiple pets and persistence — Done

- Mở rộng service session từ một pet lên danh sách instance, nhưng vẫn dùng một render clock/thread.
- Persist selected pack, size, speed, sound, last safe position và user setting bằng DataStore.
- Settings cho behavior/interaction và giới hạn số pet theo performance budget.
- Boot auto-start chỉ thêm dưới dạng opt-in rõ ràng sau khi policy/product quyết định; nếu có phải xử lý Android version restrictions.

Definition of done: multi-pet không nhân thread tuyến tính, restore an toàn, setting có unit test và degradation policy.

Verified trên Pixel 3 XL / API 31 với 3 instance `Sunny Cat`: một foreground service + ba bounded overlay window, shared bitmap/clock, drag/stop/restart khôi phục vị trí chuẩn hóa, pack/count giữ qua process restart và cleanup không để lại service/window. Settings hiện đã refactor thành `PetSlotPreferences`: character/size/speed/touch/speech/messages/position độc lập, Add commit sau selection và Remove shift an toàn. Thiết bị thường giới hạn 3 pet/30 FPS (24 FPS khi chạy 3); low-RAM giới hạn 2 pet/24 FPS. Sound vẫn hidden/reserved vì schema pack v1 chưa nhận audio. Boot auto-start vẫn chưa được thêm.

Size UX dùng 11 nấc `50–150%`, mốc 100% giảm từ 112dp xuống 84dp và service resize đúng
pet đang chạy ngay khi settings đổi, đồng thời giữ surface attachment và speech placement.
Speed giữ năm nấc `50/75/100/125/150%` và thay engine timeline live theo đúng slot mà
không reset action, combo, vị trí hoặc animation cursor.

## Phase 6 — Monetization, performance and release policy

- [Done] Redesign all in-app product screens after onboarding around a cozy Cute Pet flow:
  Home pet room → Catalog discovery → Detail confirmation → My Pet Family → per-pet
  customization. Keep Splash, Language, Intro, Permission and Premium unchanged for their
  dedicated follow-up redesigns.
- [Done] Add mutually exclusive Mixed/Pet Swarm modes on Home. Mixed supports live
  per-pet visibility; Swarm repeats one selected pack up to the device budget, unlocks
  only after a real Rewarded callback for free users, and bypasses Rewarded for Premium.
- [Done] Add a dedicated Pet Swarm editor with live count, base size/speed, deterministic
  per-instance variation and optional four-edge movement constraints.
- [Done] Add a Swarm-only runtime profile: block TALK/TALK_WALK at engine level, remove
  social/crowd coordination entirely and favor short-delay wall, ceiling and aerial
  stunt stories with safe pack-aware degradation.
- [Done] Reconcile live Swarm count changes incrementally: append/remove only the changed
  overlay instances, preserve every surviving engine/window state and recalculate the
  shared FPS budget without rebuilding the foreground session.
- [Done] Randomize each live-added Swarm pet inside the safe movement area and choose the
  best-spaced candidate, while leaving initial session layout and surviving pets untouched.
- [Done] Reconcile Mixed Add/Remove/character changes by pack identity: retain and reindex
  surviving instances, replace only changed assets and handle duplicate packs stably
  without rebuilding the foreground session.

Before release hardening, complete the clean-room parity items confirmed by the local competitor audit:

- [Done] Pause the render clock while the display is off and resume without a large catch-up tick.
- [Done] Extend autonomous behavior through explicit pack actions and boundary transitions without assuming the competitor's numbered-frame format.
- [Done] Add Living Behavior scheduling: weighted seeded decisions, anti-repeat memory, variable action timing, wall jump/drop, ceiling drop, timed creep, gravity fall and velocity-aware drag release.
- [Done] Add Living Behavior V2: run, controlled wall descent, sequence routines, partial-Special import and double-tap showcase.
- [Done] Add Living Behavior V3: 25 ordered combo IDs, pack-aware degradation, combo anti-repeat and paired social scenes with approach/facing/chase coordination.
- [Done] Pace Living Behavior V3.1 with once/sustained story beats, 5–12 second breathing room, long rest/performance durations and delayed social call-and-response.
- [Done] Add Living Behavior V3.2: 35 combo IDs with wall/ceiling parkour, boosted aerial/skill/dance choreography, collision-driven spatial beats, required-action guards and paired duet dance.
- [Done] Rebalance Living Behavior V3.3: reduce the autonomous pool to distinct high-value stories, guarantee climb after two non-climb stories, extend wall/ceiling dwell and reduce ground-only social occupancy.
- [Done] Add Living Behavior V3.4 wall-to-wall traversal with screen-relative airborne motion, opposite-wall catch, mirrored choreography and safe collision fallback.
- [Done] Add Living Behavior V3.5 upward wall-to-wall traversal with a ballistic launch arc, higher opposite-wall catch and a separately weighted downward variant.
- [Done] Stabilize multi-pet behavior V3.6 with initial social ownership, facing dead-zone and grounded overlap handling; V3.14 replaces forced collision turn-away.
- [Done] Add Pet Speech V3.7: forensic frame 34–36 mapping, sustained TALK combo,
  localized bubble, tap/skill/social dialogue triggers and user-visible message toggle;
  V3.14 replaces scene-wide serialization with per-pet sessions.
- [Done] Add Pet Speech V3.8 personalization and pacing: editable 30-line custom
  catalog, Unicode-safe DataStore codec, global random anti-repeat, 48 localized
  fallback lines, broader context-aware combo triggers and 4.5–8.5 second reading time.
- [Done] Fix Pet Speech V3.9 TALK attachment: reproduce the original frame 34–36
  `IeOffsetX=0/IeOffsetY=-64` carried-window geometry, mirror the box with pet direction,
  keep it synchronized with pet movement and discard text when its owner leaves TALK.
- [Done] Add per-pet TALK holding anchors: audit frame 34 server-side, persist metadata
  for 631 supported pets in owner revision 7, keep default placement for 395 unsupported
  pets, enrich previously installed owner revisions from catalog at Start, mirror with
  direction and render the sprite above the speech box.
- [Done] Redesign Pet Speech V3.10 choreography: gate every message behind the actual
  TALK frame, add one deliberate 9–11 second speech beat only to speaking combos, keep
  physical combos silent and render a sharp rectangular carried box without a tail.
- [Done] Synchronize Pet Speech V3.11 lifecycle: remove the independent reading timer
  and bind bubble Show/Hide directly to the engine TALK enter/exit transitions.
- [Done] Add Pet Speech V3.12 adaptive layout: glyph-aware single-line width,
  balanced multi-line sizing, four-line/viewport bounds and an explicit 80-code-point
  custom-message limit.
- [Done] Split Pet Speech V3.13 poses: stationary TALK holds frame 34, moving
  TALK_WALK keeps the 34/35/34/36 gait, legacy packs normalize at runtime and both
  actions share one bubble lifecycle.
- [Done] Redesign multi-pet interaction V3.14: autonomous movers pass through instead
  of blocking/turning each other, social invitations use chance/range/cooldown and release
  on interruption, speech windows are keyed by pet ID, and TALK is ground-surface gated.
- [Done] Add semantic action cadence V3.15: movement keeps full speed control, physics
  reactions use partial influence, expressive/skill frames use only 25% influence; owner
  Special clips originally played once with a held endpoint; V3.17 supersedes that endpoint
  policy after cross-pack frame inspection.
- [Done] Rebalance standing/rest V3.16: render owner IDLE with the standing walk frame at
  zero velocity, reserve long SIT for four meaningful autonomous stories and reduce social
  SIT to two roles in V3.17.
- [Done] Add frame-semantic choreography V3.17: audit 1,026 owner packs against upstream
  Shimeji actions, derive emote/floor-play/sprawl/wall-hold/ceiling-hold poses, play skills
  once with explicit recovery, use energy-aware combo transitions, and keep physical
  wall/aerial stories silent.
- [Done] Make Customize Pet live V3.18: size/speed, touch flags, speech toggle/catalog,
  reset position, character replacement and roster removal update the running foreground
  session without requiring a manual Stop/Start.
- [Done] Add multi-pack session selection before considering swarm mode: 1–3 typed slots can select different packs while the current device performance budget remains authoritative.
- Treat tap popup and boot restart as separate product/policy decisions, not implicit APK parity.
- [Done] Import the 1.026-pack owner snapshot into the private GitHub static server with
  versioned catalog, relative raw paths, byte sizes and SHA-256; fetch/cache metadata,
  authenticate thumbnails and download/verify only the selected ZIP.

- Map entitlement free/premium lên catalog/slot/animation; billing failure không phá pet đang chạy.
- Chỉ thêm ads placement khi có screen code/policy được owner duyệt; không đặt ad trong overlay.
- Profile CPU, memory, bitmap cache, jank, battery trên nhiều API/device/orientation.
- Audit Play policy cho overlay/foreground-service disclosure, notification, privacy, data safety và asset licensing.
- Accessibility/localization/process-death/revoked-permission/release-ProGuard test matrix.

Definition of done: release candidate đạt performance budget, policy checklist, localized UX và test matrix đã ghi nhận.

## Guardrails

- Không copy source decompile, credential, ad configuration hoặc branding. Dữ liệu pet chỉ đi qua snapshot được owner ủy quyền, checksum và quy trình import có provenance.
- Không dùng periodic WorkManager cho animation hoặc service keep-alive.
- Không mở full-screen overlay nếu pet chỉ cần một vùng nhỏ; window phải khớp hit target để không chặn app bên dưới.
- Không thêm Room/network/boot receiver trước khi requirement của phase tương ứng thật sự cần.

Technical evidence and the feature-parity matrix are recorded in [`research/COMPETITOR_TECHNICAL_AUDIT.md`](research/COMPETITOR_TECHNICAL_AUDIT.md).

The first parity slice is verified on Pixel 3 XL / API 31: falling reaches the bottom, then resumes horizontal/autonomous motion; the overlay position remains stable while the device is dozing and advances again after wake. Force-stop removes both foreground service and overlay window without a fatal error.

Edge parity is also verified on that device: status/cutout coordinates are applied exactly once, navigation-bar inset no longer shortens the bottom playground, side/top windows can extend one third of the pet size outside the display, and legacy sprites mirror only when moving right.

The owner-authorized upstream source snapshot remains pinned outside the app repository under
`private_data/`. The private server import contains only runtime catalog/ZIP/thumbnail files,
preserves source commit metadata and calculates a SHA-256 contract without copying nested Git
history. Existing on-demand normalization continues to handle the 78 reported exceptions
without mutating the pinned source snapshot.

The local catalog vertical slice was verified with all 1.026 records on Pixel 3 XL / API 31.
Production now uses `RemoteOwnerPetCatalogRepository`; the same boundary preserves
category/search filtering, on-demand Set, DataStore selection and sprite overlay behavior.
