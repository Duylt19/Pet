# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Android **Cute Pet / Shimeji** app (floating on-screen pets + battery status overlay). This is
no longer the Private Browser codebase it was forked from.

- Display name `Cute Pet`; canonical namespace/applicationId `com.asianmobile.emojibattery.shimeji`.
- `rootProject.name = "PrivateBrowser"`, the `Theme.PrivateBrowser` style and Firebase project
  `privatebrower-7168d` are legacy identifiers kept **on purpose** — never infer the package from
  them and never rename them as a side effect. Read `docs/PACKAGE_IDENTITY.md` before touching
  app identity, Firebase config or app-specific storage paths.
- Browser, tabs, search engine, bookmarks/history, downloads, media viewer, Room and the old
  foreground service are deleted. Some empty package directories (`ui/browser`, `ui/bookmarks`,
  `data/database`, …) still linger — do not treat them as existing capabilities. Re-adding
  anything similar is a **new feature**: contract, DI, permission, manifest, tests, docs.

## Working agreement

- Talk to the owner in **Vietnamese**; code, identifiers and commit messages in **English**
  (`Handle`/`Fix`/`Update`/`Refactor`/`Remove` prefixes).
- Markdown under `docs/` is part of the implementation. When source and docs disagree, source
  wins and docs must be repaired in the same change. `.agents/skills/android_developer/SKILL.md`
  §11 has the change → document mapping table.
- Read order for anything non-trivial: `.agents/AGENTS.md` →
  `.agents/skills/android_developer/SKILL.md` → `docs/PACKAGE_IDENTITY.md` → `docs/README.md` →
  the topical file under `docs/`.
- Figma work follows `.agents/skills/figma_to_compose/SKILL.md`; file key/node ID come from the
  URL the owner gives per task, token only from `$FIGMA_ACCESS_TOKEN`.

## Commands

```bash
./gradlew compileDebugKotlin                 # compile check — the default verification
./gradlew testDebugUnitTest                  # all unit tests (:app + :ads)
./gradlew :app:testDebugUnitTest --tests "*PetEngineTest"        # one test class
./gradlew :app:testDebugUnitTest --tests "*PetEngineTest.walks*" # one test method
./gradlew updateDebugScreenshotTest          # regenerate Compose preview golden images
./gradlew validateDebugScreenshotTest        # verify UI against goldens
python3 -m unittest tools.tests.test_battery_data_snapshot       # tests for the python tools
```

Do **not** run `assembleDebug`/`assembleRelease` just to check that code compiles; assemble only
when an APK is actually needed.

Screenshot tests live in `app/src/screenshotTest/kotlin` with references under
`app/src/screenshotTestDebug/reference/`. Only refresh a reference after the new UI has been
compared against Figma. `adquality-sdk` is excluded from the render classpath during
`*ScreenshotTest*` tasks only (its generated `R` metadata breaks Layoutlib) — normal builds keep it.

## Architecture

Two modules: `:app` (shell + all features) and `:ads` (ad SDKs, Firebase Remote Config, ad
composables/utilities). Never put product logic in `:ads`; never call an ad SDK directly from a
feature.

Layering — `Composable → ViewModel → (UseCase) → Repository interface → impl/DataStore/platform`.
UI never touches DataStore, network or services directly; ViewModels never hold `Activity`,
`View` or `NavController`. Repository interfaces live in `data/repository/`, implementations in
`data/repository/impl/`, bound in `di/DataModule.kt`.

Each screen is a `XScreen.kt` + `XViewModel.kt` + `XUiState.kt` triple under
`ui/<feature>/`; immutable `UiState`, expose `StateFlow`, collect with
`collectAsStateWithLifecycle()`.

### Navigation

Single Activity (`MainActivity`) → `navigation/NavGraph.kt`. `Routes` holds every route constant
plus the typed route builders (`Routes.petCatalog(target, slotIndex)`, `Routes.batteryEditor(id)`,
…). `NavExtensions.kt` provides `safeNavigate()`/`safePopBackStack()` (double-tap guard) and
`navigateWithAd()` for interstitial-gated destinations. Onboarding steps pop with
`inclusive = true`. The four bottom tabs (`HomeTab` in `ui/component/HomeChrome.kt`) map to
`home` / `battery_catalog` / `pet_store` / `settings` via `homeTabForRoute`/`routeForHomeTab`.
Flow: Splash → Language → Intro → Permission → Home.

Adding/removing a route also means updating `docs/04_NAVIGATION_FLOW.md`,
`docs/screens/README.md`, `ScreenName` in `utils/AnalyticsHelper.kt` (visible screens use
`TrackScreenView(ScreenName.X)`) and the navigation tests.

### Pet runtime (`pet/`)

- `pet/engine/` — pure Kotlin state machine, timeline, geometry, crowd/social direction. No
  Android framework imports, so it is directly unit-testable and most tests live here.
- `pet/overlay/` — `PetOverlay` (special-access check + start/stop), `PetOverlayService`
  (`specialUse` foreground service, mandatory notification), `PetOverlayController` (bounded
  window list, one shared adaptive frame clock), `PetOverlayView`, `PetSpeechBubbleView`.
- `pet/pack/` — pack-v1 schema, manifest parser, security validator, ZIP installer, sprite cache.
- `pet/settings/`, `pet/speech/` — pure policy objects (session budget, layout, speech placement).

Two session modes: **Mixed** 1–12 different pets (slots 1–3 free, 4–12 unlocked sequentially via
rewarded ads) and **Swarm** 1–12 copies of one pack. Behavioural invariants (FPS budgets, speech
windows, reconciliation rules) are specified in `docs/features/PET_OVERLAY.md` — read it before
changing controller/engine behaviour.

### Battery status (`battery/`)

Opt-in status-bar cover drawn by `StatusBarAccessibilityService` into a
`TYPE_ACCESSIBILITY_OVERLAY`; no node retrieval or automation. Gated by
`BuildConfig.BATTERY_STATUS_ENABLED` (true in debug, false in release). Debug builds package an
audited catalog from `private_data/battery-apk-1.0.2/` via the `auditDebugBatterySnapshot` /
`prepareDebugBatteryAssets` / `prepareDebugBatteryResources` Gradle tasks (they need `python3`
and silently skip when the snapshot directory is absent). `private_data/` is git-ignored and
never copied into the Android source tree.

### Remote catalogs (`data/remote/`)

**This app is a client of a private content server.** Pets, battery themes and My Pet Room
backgrounds are not in the APK — they are published to a separate repository and fetched at
runtime. The server lives at `Asian-Mobile-Inc/Server-Emoji-Battery-Shimeji-Pet-AM` (usually
cloned next to this project, e.g. `../Server-Emoji-Battery-Shimeji-Pet-AM`), and the app reads
its `master` branch over `raw.githubusercontent.com`.

| Catalog | Client config | Server file |
|---|---|---|
| Pets | `PetServerConfig` | `json/pets.json` + `data/<id>.zip` + `thumb/<id>.png` |
| Battery | `BatteryServerConfig` | `json/batteries.json` + `battery/**` |
| Rooms | `RoomServerConfig` | `json/rooms.json` + `room/bg|thumb/BG_<id>.png` |

Every catalog follows the same contract: the repo is **private**, so requests carry
`Authorization: Bearer <token>` where the token comes from the Firebase Remote Config key
`github_token_pet_server` (default in source must stay empty). Reads are cache-first with 24h
TTL revalidation, ETag and rate-limit backoff; every downloaded asset is verified against the
byte size and SHA-256 the catalog declares before it is used. Release only accepts `APPROVED`
catalog entries; debug keeps the packaged snapshot as fallback.

Adding or changing catalog content is a **change in the server repository**, not here: build it
with that repo's `tools/*_pipeline.py`, which recomputes size/SHA-256 and runs the same
validation as its CI. `PetRoomBundledBackground` is the one exception — room `1` also ships in
the APK so the room is never empty offline, and it must stay listed in `json/rooms.json` as the
catalog's `defaultRoomId`.

## UI conventions

- No hardcoded user-facing strings/colors: `res/values/strings.xml` keys as
  `<feature>_<purpose>`, colors as `colors_<HEX>` in `colors.xml`.
- Sizing uses Intuit SDP/SSP: `dimensionResource(com.intuit.sdp.R.dimen._Xsdp)` /
  `...ssp.R.dimen._Xssp`. Local fixed values (padding, icon size, radius, text) = Figma px ÷ 1.3
  rounded to the nearest resource. Viewport-relative widths (dialogs, sheets, cards) are **not**
  divided — keep the Figma ratio via `Modifier.fillMaxWidth(nodeWidth / frameWidth)`.
- Modifier order: `size → shadow → clip → background → border → clickable → padding`.
- Icon naming: monochrome `ic_<name>.xml`, multicolour `ic_logo_<name>.xml`, bitmaps
  `img_<name>.webp/png`. No `.svg` in `res/`.

## Before finishing

`./gradlew compileDebugKotlin` + `./gradlew testDebugUnitTest` + `git diff --check`, docs updated
in the same change, then a clear English commit. Never commit real tokens — Remote Config
defaults in `ads/src/main/res/xml/remote_config_defaults.xml` must stay empty in source.
