# 08 — Test and Release Plan

> **RELEASE PLAN — chưa hoàn tất device/policy matrix**

Trạng thái evidence hiện tại được ghi tại
[12_RELEASE_READINESS.md](12_RELEASE_READINESS.md); checklist bên dưới là gate chuẩn,
không phải tuyên bố đã chạy trên thiết bị.

## Test pyramid

### Pure JVM

- Config sanitization/migration/corrupt JSON.
- Draft vs applied state transition.
- Geometry bounds/step snapping.
- Color parsing/automatic contrast.
- Module visibility and priority under narrow width.
- RTL/physical-side policy.
- Date/time formatter by locale/12–24 hour.
- Battery percentage/status mapping.
- Connection transport mapping/fallback.
- Data label never represented as measured network generation when manual.
- Catalog parser, entitlement, search/category/favorites.
- Archive traversal/type/size/pixel/animation validation.
- Rewarded result exactly-once policy.
- Shared service active-feature state machine.
- Runtime backend selection/switch/exactly-one-active invariant.
- Accessibility capability and consent state machine.

### Repository/integration

- DataStore default, persistence, migration and concurrent update.
- Cache-first catalog + 200/304/403/429/offline.
- Download size/hash mismatch.
- Atomic install and immutable version.
- Device repository fake receiver/callback registration and cleanup.
- Service keeps pet alive when capsule stops and vice versa.

### Compose

- Catalog loading/content/offline/error/locked/premium/selected.
- Full editor dirty/applying/error and sticky CTA padding.
- Component shell for every enum.
- Dynamic grid at compact/medium/expanded width.
- Text scale 1.0/1.3/2.0.
- RTL.
- Screen reader semantics and selected state.
- Permission return resumes pending Apply once.
- Accessibility decline/enable/disable return flow không loop hoặc auto-fallback.

### Instrumentation/device

- Add/update/remove one non-touchable capsule window.
- Verify app below vẫn nhận touch trên Android 12+.
- `TYPE_ACCESSIBILITY_OVERLAY` visually covers status bar trên device matrix.
- Status bar swipe-down vẫn hoạt động trong cover mode.
- Notification shade/keyguard/system dialog behavior và best-effort hide.
- Camera/microphone privacy indicator, screen recording/casting và notification warning
  không bị capsule che trên từng OEM/API được support.
- Accessibility enable, disable, service interrupt/unbind và app process restart.
- Switch below-bar ↔ cover-bar không tạo duplicate window.
- Real battery level/charging plug/unplug.
- Minute/timezone/locale change.
- Wi‑Fi ↔ cellular ↔ offline.
- Airplane/ringer transitions.
- Decorative animation start/pause/resume/frame pacing.
- Rotation, cutout, fullscreen, split-screen, desktop/windowed mode.
- Screen off/on no catch-up.
- Overlay permission revoke.
- Accessibility revoked/disabled.
- Notification denied API 33+.
- Process kill, force-stop and app update.
- Pet-only, capsule-only, both, stop-one, stop-all.

## Device matrix

| API | Device/profile | Focus |
|---:|---|---|
| 24 | Emulator/physical if available | min SDK, legacy overlay type behavior |
| 28 | Cutout emulator | display cutout |
| 31 | Pixel 3 XL current device | overlay touch opacity, existing pet regression |
| 33 | Android 13 | notification permission |
| 35 | Android 15 | edge-to-edge/window metrics |
| 36 | Android 16 | target SDK behavior, public tethering callback |

Thêm ít nhất:

- one Samsung;
- one Xiaomi/Oppo/Vivo class OEM;
- low-RAM device;
- tablet/large screen;
- gesture và 3-button navigation.

## Visual verification

Golden/reference state:

```text
12:30, 73%, charging, Wi‑Fi + cellular sample, sky background,
default emoji, left/right margins 12dp, height 32dp
```

Capture:

- Compose preview;
- in-app editor;
- launcher/app overlay;
- light/dark app background;
- left/right cutout;
- long localized date;
- maximum enabled modules.

Không dùng screenshot-only để xác nhận data lifecycle/service cleanup.

## Performance targets

Initial release budget trên Pixel 3 XL/API 31:

- static capsule không giữ continuous frame callback;
- no allocation trong `onDraw` steady state;
- static idle CPU target dưới 1% process average sau warm-up;
- animated capsule target dưới 3% process average ở 12–15 FPS;
- static decoded asset memory delta dưới 12 MiB;
- animated pack memory delta dưới 20 MiB;
- update latency battery/network dưới 1 giây sau system callback;
- no ANR, `BadTokenException`, window leak hoặc callback leak.

Target là release gate cần đo/profiling; nếu device không đạt phải giảm asset/FPS trước khi
ship, không nới budget âm thầm.

## Security tests

- Zip Slip/backslash/NUL/absolute path.
- Duplicate/case-collision/symlink.
- Zip bomb/entry count/ratio.
- Oversized/corrupt/mislabeled image.
- Invalid stretch insets/frame metadata.
- SHA-256/size mismatch.
- JSON nesting/string/count bounds.
- Catalog path/host allowlist và token non-leak.
- No remote code/font/SVG/XML/Lottie.
- Logs and analytics contain no token/device status.

## Regression for existing product

- Mixed 0–12 (including an empty/inactive roster) and Swarm behavior remain stable.
- Pet shared FPS/position/speech/gesture unchanged.
- Current onboarding permission/back stack unchanged until explicitly updated.
- Existing ads/premium/App Open suppression unchanged.
- `compileDebugKotlin`, all debug unit tests and pet device smoke test pass after shared
  service migration.

## Release checklist

- Owner approves product name/position/visual and asset licenses.
- Play FGS declaration/video updated.
- Accessibility declaration, disclosure/consent, Privacy Policy và review video approved
  nếu ship cover mode.
- Manifest disclosure text reviewed.
- Privacy/Data Safety reviewed.
- Ads placement/remote config approved.
- Localization complete for supported locales.
- Accessibility pass.
- OEM/cutout/layering/touch-through matrix recorded cho từng display mode.
- Battery/performance profile recorded.
- Server catalog integrity report archived.
- Rollout has kill switch for remote catalog and capsule runtime.

## Rollback

- Remote Config can hide Battery entry and disable remote catalog/ads.
- Applied config remains on device but service refuses capsule start when runtime kill
  switch is off.
- Pet overlay remains operational.
- App update can fall back to built-in theme without deleting user favorites/unlocks.
