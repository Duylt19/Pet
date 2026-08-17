# 12 — Release Readiness Ledger

Ngày cập nhật: 2026-07-30.

Tài liệu này tách “đã có trong source” khỏi “đã có evidence để phát hành”. Không dùng việc
compile thành công để thay thế device test, license approval hoặc Play review.

## Scope đã khóa

- Product mode: `COVER_SYSTEM_BAR` bằng `TYPE_ACCESSIBILITY_OVERLAY`.
- Không triển khai below-bar FGS/backend trong release scope hiện tại.
- Window non-touchable/non-focusable, portrait-only, ẩn ở screen-off/keyguard.
- Không đọc accessibility node, không gesture/click/type/scroll.

## Repository evidence

| Gate | Trạng thái | Evidence/command |
|---|---|---|
| Debug Kotlin compile | Automated | `./gradlew compileDebugKotlin` |
| Release Kotlin compile | Automated | `./gradlew compileReleaseKotlin` |
| JVM regression | Automated | `./gradlew testDebugUnitTest` |
| Whitespace/patch integrity | Automated | `git diff --check` |
| Snapshot schema/hash/size/dimension | Automated debug tooling | `./gradlew auditDebugBatterySnapshot` |
| Release rejects `REVIEW_REQUIRED` catalog | Implemented fail-closed | `HybridBatteryCatalogRepository` parser/source policy |
| Release runtime switch | Owner-enabled | `BuildConfig.BATTERY_STATUS_ENABLED=true`; service attach vẫn phụ thuộc user setting, Accessibility grant và runtime visibility policy |
| Narrow-width overlap prevention | JVM covered | `BatteryStatusLayoutPolicyTest` |
| RTL leading/trailing mirror | JVM policy + Canvas implementation | `BatteryStatusPhysicalSides` |
| Draft process-death serialization | JVM covered | `BatteryDraftCodecTest` |
| Accessibility metadata minimum | Source-reviewed | `battery_accessibility_service.xml` |

## External gates — chưa được phép đánh dấu Done

| Gate | Owner/evidence cần có | Trạng thái 2026-07-30 |
|---|---|---|
| Asset ownership/license | Inventory, license link/file, approver, approval date | Runtime catalog được owner yêu cầu publish với trạng thái `APPROVED`; hồ sơ nguồn/license vẫn cần lưu ngoài source |
| OEM/API matrix | API 24/28/31/33/35/36 + Pixel/Samsung/Xiaomi/Oppo class | Blocked — ADB không khả dụng trong môi trường verify |
| Cutout/privacy indicator | Video/screenshot per supported device | Pending |
| Notification shade/status swipe | Touch-through recording per device | Pending |
| TalkBack/large text/RTL visual | Manual accessibility report | Pending |
| CPU/memory/FPS | Perfetto/Memory Profiler report theo budget doc 08 | Pending |
| Play Accessibility declaration | Approved disclosure, justification, demo video | Pending |
| Privacy Policy/Data Safety | Owner/legal review | Pending |
| Remote production catalog | Owner endpoint, TLS/host policy, ETag/TTL, kill switch | Implemented — private GitHub server, Remote Config token, ETag/TTL/backoff và verified lazy asset cache |
| Rewarded unlock | Reuses approved Rewarded unit; earned/unavailable/dismissed contract documented in docs/07 | Implemented; device ad-SDK validation pending |
| Native/banner placement | Product + ads approval before any new placement | Not approved; not implemented |

## Release decision

Runtime đã được owner bật trong release để QA end-to-end. Điều này không đồng nghĩa feature đã
Play-ready; các external gate áp dụng ở trên vẫn cần evidence trước khi phát hành Store.
Nếu Play/accessibility gate không đạt, Battery entry phải bị loại khỏi release thay vì
chuyển âm thầm sang một overlay mode khác.
