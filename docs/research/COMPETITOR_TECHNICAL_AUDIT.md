# Competitor Technical Audit — Anime Shimeji Screen Pets 1.0.8

## Scope and evidence

This document records a clean-room technical analysis of the locally supplied XAPK. It is an engineering input, not a source or asset import plan.

- Input XAPK: `Anime+Shimeji+Screen+Pets_1.0.8_APKPure.xapk`
- Package: `com.cle.anime.shimeji.pet.onscreen`
- Version: `1.0.8` (`versionCode 8`)
- Base APK: 70,714,898 bytes
- Splits: `arm64_v8a` and `mdpi`
- SDK contract: min SDK 24, target SDK 36
- Analysis sources: decoded resources, manifest, DEX inspection and a second JADX 1.5.5 pass over the base APK

JADX reported 95 decompilation errors across 15,801 APK classes. Core product classes were recovered and cross-checked against resources and call sites, but decompiled control flow must still be treated as evidence rather than original source.

## Ownership boundary

The supplied app contains a catalog dominated by anime, game and internet characters. At the start of this audit, its redistribution scope had not been established, so no binary assets were imported into the Android app or committed to source control. On 2026-07-22, the project owner represented that the demo app and its complete pet dataset are owner-controlled and fully authorized for this project. Based on that authorization, the upstream data repository was captured separately under ignored `private_data/` storage with a pinned commit and SHA-256 inventory.

This authorization applies to the requested data snapshot. Decompiled implementation code, branding, translated copy, ad configuration, product IDs, credentials and backend identity remain excluded from Cute Pet. The reusable implementation output of this audit remains limited to behavior contracts, file-format observations and platform architecture.

## Executive architecture

```text
Bundled pets.json catalog
        │
        ├── thumbnail: remote /thumb/<id>.png
        └── animation: remote /data/<id>.zip
                         │
                         └── cache/data_<id>/shime1.png ... shime46.png

Home / Detail / Preview
        │ selected IDs + settings in SharedPreferences
        ▼
PetsService (foreground specialUse)
        │ one shared ~30 FPS render thread
        ├── MascotView + overlay window #1
        ├── MascotView + overlay window #2
        └── ...
             │
             └── Mascot state machine → frame index, dx, dy, direction
```

The core feature does not use a game engine. It is a custom Java/Kotlin animation state machine, transparent `SurfaceView` rendering, one small `WindowManager` overlay per pet and one shared render thread.

## Inventory

### Package and resources

- The supplied first-pass export contains 17,437 Java files because it includes dependencies and generated code.
- The recovered app namespace contains 141 Java files; product code is concentrated in application/base, UI, database, notification, billing and `utils/data_pets` packages.
- The export contains 1,560 XML, 256 PNG, 24 WebP, 4 MP4, 4 TTF, 13 JSON and 8 native `.so` files.
- Native libraries in the arm64 split belong to advertising/measurement SDKs. No native library is required by the pet engine.
- `assets/dic` is an obfuscated/shield-related blob and is not needed for feature parity.

### Android components

Product-facing components recovered from the manifest:

- Activities: splash, language, onboarding, overlay permission, main, pet detail, hungry and take-care.
- Foreground service: `PetsService`, declared as `specialUse`.
- Boot receiver: starts the pet service after `BOOT_COMPLETED` when the saved enable flag and overlay access are both present.
- File provider: supports file sharing/import plumbing.
- WorkManager workers: delayed pet refresh and marketing notification scheduling.

The competitor service and boot receiver are exported. Cute Pet keeps service entry points non-exported and will only add boot behavior as explicit opt-in after a separate policy decision.

## Catalog and persistent data

### Bundled catalog

`res/raw/pets.json` is the only product catalog database required at first launch.

| Property | Observed value |
|---|---:|
| Records | 991 |
| Unique IDs | 991 |
| ID range | 3–1045 |
| Fields | `id`, `name`, `category`, `author` |
| Categories | 267 |
| Authors | 532 |

The first JSON element is skipped by the catalog loader. The list is reordered by unlock timestamp: unlocked items first, sorted by saved unlock time, then locked items.

Room duplicates a small subset of catalog metadata in `db_shimeji_pets`:

```text
PetsEntity(id: Int, name: String, category: String, author: String)
```

It uses destructive migration. No evidence shows that Room is essential to overlay rendering. Cute Pet should continue using its validated pack repository and only add a database when a real searchable/offline catalog requires it.

### Preferences

The competitor uses one untyped SharedPreferences file named `data_my_app_shared_preference`.

| Concern | Key shape / default |
|---|---|
| First open | `isFirstOpen=true` |
| Language | `getLanguageCode=""` |
| Selected slot | `getStateMain_<0..8>=-1` |
| Selected display name | `getNameShimeji_<slot>=""` |
| Active normal count | `getNumberShimeji=1` |
| Size | `getSize=1.6` |
| Speed | `getSpeed=3.5` |
| Service enabled | `enableService_shimeji=false` |
| Download flag | `isDownloaded_<petId>=false` |
| Unlock timestamp | `getUnlock_<petId>=0` |
| Swarm mode | `isSwarmMode=false` |
| Swarm count per type | `getPetCountPerType=8` |
| VIP/swarm slot | `getVipPetPosition_<slot>=-1` |
| Change interval | `getTimeChange=5` minutes |

Other keys are UI counters, first-use dialogs, rating prompts, ad state and cancellation counters. Cute Pet already models durable user settings with typed DataStore and sanitizes corrupt/out-of-range values; that contract is safer than copying these keys.

## Remote asset contract

### Download layout

The catalog derives two public URLs from a hardcoded raw-content base:

```text
/thumb/<petId>.png
/data/<petId>.zip
```

Downloaded ZIP content is expanded into:

```text
cacheDir/data_<petId>/
  shime1.png
  shime2.png
  ...
  shime46.png
```

Missing numbered frames are logged and tolerated. There is no per-pack manifest, version, checksum, signature, author/license metadata or explicit frame dimension contract. Frame semantics are globally hardcoded by index.

### Competitor pipeline weaknesses

- ZIP entries are joined directly to the target directory without canonical-path containment: Zip Slip is possible.
- There is no archive, entry, decoded-pixel or expansion-ratio budget.
- A fixed temporary filename is reused.
- Download completion is treated as sufficient; no hash/signature/version validation exists.
- Download state is a boolean preference and can diverge from files on disk.
- Product traffic allows cleartext at application level even though the observed catalog endpoint is HTTPS.

Cute Pet pack v1 already improves all of these areas with staging, traversal prevention, file/type/size/pixel budgets, validation and atomic promotion. The competitor transport/backend must not be reused.

## Sprite and animation contract

### Global frame map

The competitor assumes every compatible pet uses the same numbered PNG semantics. Zero-based frame indices below correspond to `shime<index+1>.png`.

| Behavior | Frame indices |
|---|---|
| Walk | `0, 1, 0, 2` |
| Falling | `3` |
| Dragging | `6, 4, 5, 7, 5` |
| Sit | `10` |
| Wall climb | `13, 13, 11, 12, 12, 12, 11, 13` |
| Wink | `14, 16` |
| Bounce | `17, 18` |
| Trip | `18, 17, 19, 19` |
| Creep | `19, 19, 20, 20, 20` |
| Jump | `21` |
| Ceiling climb | `24, 24, 22, 23, 23, 23, 22, 24` |
| Sit and look up | `25` |
| Sit with legs down | `30` |
| Sit and dangle | alternating `30, 31` |
| Special 1 | `0, 37, 38, 39` plus a generated final frame call |
| Special 2 | `41, 42, 43, 44, 45, 44, 43, 42` |

The mapping demonstrates why importing the competitor's raw ZIPs into Cute Pet pack v1 would be incorrect even aside from licensing: their format has implicit global meaning, while ours has explicit clips, source rectangles, timing, velocity and interaction metadata.

### State machine

Each animation owns a frame list `(frameIndex, dx, dy, durationTicks)`, current tick and frame cursor, direction, boundary reaction, optional random timeout, and transition-on-end/transition-on-boundary functions.

```text
Falling → Bounce → Walk
Walk ──edge──→ WallClimb ──top──→ CeilingClimb ──timeout──→ Falling
WallClimb ──timeout──→ Jump or Falling
CeilingClimb ──edge──→ Descend → Walk/WallClimb
Walk ──random──→ Sit / Stand / Wink / Dangle / LookUp / Trip / Creep / Special
Any passive one-shot → Walk
Drag release → Falling
Fling collision → wall climb / ceiling climb / bounce; otherwise Falling
```

Important probabilities/timing:

- Walk schedules an alternate behavior after 10–69 ticks.
- Wall climb schedules jump/fall after 50–249 ticks; jump has 70% probability.
- Ceiling climb schedules falling after 30–199 ticks.
- Walk's alternate behavior is chosen uniformly from the eligible list, with special clips omitted when unavailable.
- Tick loop sleeps about 30 ms; sprite duration and timeout values are ticks, not milliseconds.

Movement is applied as frame `dx/dy × (savedSpeed + 0.9)`, converted to integer pixels and clamped. Direction is rendered by horizontal bitmap mirroring.

### Preview modes

`MascotConfig` has four modes:

- `0`: normal interactive pet, initially falling.
- `1`: looping preview of Special 1.
- `2`: looping preview of Special 2.
- `3`: simplified/scattered or non-interactive variant using `Z*` animation classes.

The detail screen previews action groups separately. The two special availability flags determine whether special states enter the random pool.

## Overlay runtime

### Service and windows

- API 26+: `TYPE_APPLICATION_OVERLAY`; older devices: legacy phone window type.
- Window uses translucent pixel format, top/start gravity, wrap-content size and non-focusable/non-modal layout flags.
- One overlay window and one `SurfaceView` are created per pet.
- One service-owned render thread updates all pets.
- The service returns `START_STICKY` and recreates the selected session on start.
- A process-local readiness preference and local broadcast expose service readiness.

Normal mode reads up to nine selected slots, shuffles them and uses the configured count. Swarm mode repeats selected VIP types by a per-type count. Pet creation is staggered by 300 ms.

### Render loop and performance

- Target state/render cadence: roughly 33 ms (about 30 FPS).
- `WindowManager.updateViewLayout` is throttled to over 40 ms.
- Layout updates are skipped unless movement exceeds about 3 px or size changes.
- Pending window positions are batched onto the main thread.
- Sprite sets are cached by pet ID.
- `SCREEN_OFF` pauses the render loop and `SCREEN_ON` resumes it.

This validates Cute Pet's shared-clock/shared-visual design. Cute Pet uses `Choreographer`, bounded bitmap cache and a 24 FPS degradation tier, avoiding the competitor's raw thread and `SurfaceView.lockCanvas()` complexity.

### Gestures and popup

- Drag stores original position and follows raw screen coordinates.
- Release transitions from dragging to falling.
- Fling uses Android `Scroller`; collision selects the next state.
- Single tap opens a separate small control overlay with Home and Settings actions.
- Double tap invokes a second callback.
- The control overlay is removed automatically after five seconds.

## Lifecycle, work and notifications

- A dynamic screen receiver pauses animation while the display is off.
- Boot receiver attempts to restart pets only when the saved service flag is enabled and overlay access still exists.
- A delayed one-time worker restarts an already-running service after `getTimeChange` minutes, then reschedules itself.
- A separate one-time worker schedules daily marketing notifications with one of ten predefined creative variants.
- Hungry opens an immersive promotional/interstitial screen; take-care is an unfinished static activity. No durable hunger simulation or care domain model was found.

The pet-change worker is effectively periodic work implemented as self-rescheduling one-time work. Cute Pet must not use WorkManager as an animation keep-alive mechanism.

## UI and business flows

```text
Splash → Language → Onboarding → Overlay permission → Main
Main tabs: Home / Add collection / Settings
Catalog item → unlock or download → Detail → select into slot
Home → choose single/multiple/swarm → enable service
Pet tap → floating Home/Settings popup
```

Home supports nine normal selection slots, count selection, size/speed controls, single versus multiple pet dialogs, swarm mode and service enable/disable. Remote config exposes a pet-number cap of 12, although normal selection storage contains nine slots.

The app combines subscriptions/one-time billing, rewarded unlocks, app-open, interstitial, native and banner advertising. Remote Config controls many placement switches and delays across splash, language, intro, permission, home, detail, download and multiple-pet surfaces. No ads are rendered inside the pet overlay itself.

## Security, privacy and quality findings

| Severity | Finding | Clean-room decision |
|---|---|---|
| High | ZIP extraction permits path traversal and has no resource budgets | Keep Cute Pet's validated atomic installer |
| High | Foreground service and boot receiver are exported | Keep internal components `exported=false` |
| High | Character catalog/assets require explicit redistribution authority | Owner authorization recorded for the 2026-07-22 snapshot; keep provenance and licensing metadata with server imports |
| Medium | `allowBackup=true` and cleartext traffic enabled globally | Do not inherit; minimize backup/network surface |
| Medium | Sticky service/boot behavior can surprise users and faces modern Android restrictions | Explicit start now; boot only as disclosed opt-in |
| Medium | Download booleans can diverge from filesystem | Repository validates disk state as source of truth |
| Medium | Bitmap cache is unbounded and not recycled by memory budget | Keep bounded `LruCache` and preload outside frame loop |
| Medium | Exceptions are frequently swallowed | Map/log errors and surface recoverable UI state |
| Low | Room uses destructive migration for a trivial catalog copy | Avoid database until product data needs it |
| Low | Hungry/take-care feature is mostly promotional/unfinished | Do not treat it as core parity |

Credential-bearing configuration, ad unit values and service-specific identifiers were inspected only to classify dependencies. They are intentionally omitted from this document and must not enter the Cute Pet repository.

## Clean capability mapping

| Competitor capability | Cute Pet status | Decision |
|---|---|---|
| Small transparent overlay window | Implemented | Keep |
| Shared render clock for multiple pets | Implemented | Keep |
| Drag, tap and fling | Implemented | Keep |
| Persistent count/size/speed/interaction/position | Implemented | Keep typed DataStore |
| Validated importable pet packs | Implemented, stronger | Keep pack v1 |
| Display-off render suspension | Missing | Implement cleanly |
| Autonomous fall/bounce/climb/passive behaviors | Partial | Extend engine and pack schema compatibly |
| Tap control popup | Missing | Add after owner-facing UX is defined |
| Double-tap action | Implemented | Use runtime showcase routine and skip unsupported pack actions |
| Multiple different selected pack types | Missing | Add repository/session model before swarm mode |
| Nine slots / large swarm | Intentionally absent | Preserve device performance budget; do not clone limits blindly |
| Remote catalog/download | Missing by design | Build only against the owner-controlled backend and authorized snapshot |
| Boot restart | Deferred | Explicit opt-in plus Android policy review only |
| Ads/billing entitlement | Phase 6 | Use Cute Pet policy/config, never competitor IDs |
| Hungry/take-care | Not implemented | Low-value/non-core until product requirement exists |

## Confidence and open evidence

High confidence: package/manifest, catalog counts, preference keys, remote path shape, numbered frame contract, overlay type/flags, shared render loop, state names/transitions, screen-off behavior and download weaknesses.

Medium confidence: exact probability distributions after obfuscation, all special-frame tails, some ad/navigation branches and billing error paths.

Low product value: shield blob internals, third-party SDK implementation code and unfinished take-care content. These do not block a clean implementation of the core pet experience.
