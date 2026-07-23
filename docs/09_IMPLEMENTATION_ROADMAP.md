# 09 — Cute Pet Implementation Roadmap

Roadmap này chuyển base hiện tại thành app pet animation chạy nổi trên màn hình. Mỗi phase là một commit độc lập, phải compile/test/docs pass trước khi chuyển phase.

## Quyết định đã chốt

- Giữ namespace/application ID legacy `com.asianmobile.privatebrower` cho đến khi owner yêu cầu đổi.
- Giữ module `:ads`, chưa thêm placement mới.
- MVP dùng asset pet demo do project sở hữu; snapshot pet được owner ủy quyền nằm ngoài Git và chưa được nối trực tiếp vào runtime; không copy code decompile.
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

Verified trên Pixel 3 XL / API 31 với 3 instance `Sunny Cat`: một foreground service + ba bounded overlay window, shared bitmap/clock, drag/stop/restart khôi phục vị trí chuẩn hóa, pack/count giữ qua process restart và cleanup không để lại service/window. Thiết bị thường giới hạn 3 pet/30 FPS (24 FPS khi chạy 3); low-RAM giới hạn 2 pet/24 FPS. Sound preference đã persist nhưng schema pack v1 chưa nhận audio. Boot auto-start vẫn chưa được thêm.

## Phase 6 — Monetization, performance and release policy

Before release hardening, complete the clean-room parity items confirmed by the local competitor audit:

- [Done] Pause the render clock while the display is off and resume without a large catch-up tick.
- [Done] Extend autonomous behavior through explicit pack actions and boundary transitions without assuming the competitor's numbered-frame format.
- [Done] Add Living Behavior scheduling: weighted seeded decisions, anti-repeat memory, variable action timing, wall jump/drop, ceiling drop, timed creep, gravity fall and velocity-aware drag release.
- [Done] Add Living Behavior V2: run, controlled wall descent, sequence routines, partial-Special import and double-tap showcase.
- [Done] Add Living Behavior V3: 25 ordered combo IDs, pack-aware degradation, combo anti-repeat and paired social scenes with approach/facing/chase coordination.
- [Done] Pace Living Behavior V3.1 with once/sustained story beats, 5–12 second breathing room, long rest/performance durations and delayed social call-and-response.
- [Done] Add Living Behavior V3.2: 35 combo IDs with wall/ceiling parkour, boosted aerial/skill/dance choreography, collision-driven spatial beats, required-action guards and paired duet dance.
- [Done] Rebalance Living Behavior V3.3: reduce the autonomous pool to 14 distinct stories, guarantee climb after two non-climb stories, extend wall/ceiling dwell and reduce ground-only social occupancy.
- Add multi-pack session selection before considering swarm mode; keep the current device performance budget authoritative.
- Treat tap popup, boot restart and remote catalog as separate product/policy decisions, not implicit APK parity.
- Use only owner-authorized pack metadata and assets. The APK's 991-entry bundled catalog remains analysis evidence; the separately authorized upstream snapshot contains 1,026 catalog packs. Local device sync is enabled for validation, while production distribution must use a provenance-preserving server import.

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

The owner-authorized upstream data snapshot is complete and kept outside Git under `private_data/`. Server migration must consume the generated SHA-256 manifest/inventory and normalize the 78 reported naming/content/frame-contract exceptions without mutating the pinned source snapshot.

The local catalog vertical slice is verified with all 1,026 metadata/archive/thumbnail records on Pixel 3 XL / API 31: category/search filtering, on-demand Set, DataStore selection and sprite overlay all work. `OwnerPetCatalogRepository` remains the replacement boundary for the production server source.
