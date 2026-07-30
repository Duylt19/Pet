# 05 — Data, Catalog and Asset Contract

> **PLANNED — NOT IMPLEMENTED**

## Data ownership

| Data | Owner | Persistence |
|---|---|---|
| Applied capsule config | `StatusCapsuleSettingsRepository` | Dedicated Preferences DataStore |
| Editor draft | ViewModel + `SavedStateHandle` | Process/state restore only |
| Favorites | Settings repository | DataStore stable ID set |
| Reward unlocks | Settings repository | DataStore stable ID set |
| Premium | Existing billing infrastructure | Existing entitlement source |
| Catalog JSON/metadata | Catalog repository | App-private files |
| Downloaded packs | Catalog installer | App-private immutable version dir |
| Device status | Device repository | Memory only |
| Runtime running state | `OverlayHostRuntime` | Process-local Flow |

Không dùng Room cho static catalog MVP. Chỉ thêm database khi có search/history/offline
query đủ lớn và product requirement rõ.

## Applied configuration model

Persist model dùng stable key/string, không dùng Android resource ID:

```kotlin
data class StatusCapsuleConfig(
    val schemaVersion: Int = 1,
    val enabled: Boolean = false,
    val themeKey: String = "builtin.sky@1",
    val geometry: CapsuleGeometry = CapsuleGeometry(),
    val appearance: CapsuleAppearance = CapsuleAppearance(),
    val time: TimeComponentConfig = TimeComponentConfig(),
    val date: DateComponentConfig = DateComponentConfig(),
    val emoji: AssetComponentConfig = AssetComponentConfig(),
    val battery: BatteryComponentConfig = BatteryComponentConfig(),
    val airplane: IndicatorComponentConfig = IndicatorComponentConfig(),
    val ringer: IndicatorComponentConfig = IndicatorComponentConfig(),
    val wifi: IndicatorComponentConfig = IndicatorComponentConfig(),
    val signal: IndicatorComponentConfig = IndicatorComponentConfig(),
    val dataLabel: DataLabelComponentConfig = DataLabelComponentConfig(),
    val hotspot: IndicatorComponentConfig = IndicatorComponentConfig(),
    val animation: AnimationComponentConfig = AnimationComponentConfig(),
    val charging: IndicatorComponentConfig = IndicatorComponentConfig(),
    val emotion: AssetComponentConfig = AssetComponentConfig()
)
```

### Value bounds

| Field | Range/default |
|---|---|
| Capsule height | 24–48dp, step 2, default 32 |
| Left/right margin | 0–24dp, step 2, default 12 |
| Content padding | 4–12dp |
| Corner radius | Theme-derived, clamp 8–24dp |
| Indicator size | 12–28dp |
| Emoji/emotion size | 12–36dp |
| Battery percent text | 10–24sp-equivalent |
| Time/date text | 10–22sp-equivalent |
| Color | Opaque/safe ARGB string, parser validated |
| Module list | Fixed known enum only; unknown ignored |

Repository sanitizes NaN/out-of-range/unknown enum/invalid color and materializes missing
fields từ default khi schema cũ được đọc.

## Suggested DataStore keys

Dedicated file: `battery_status_preferences`.

```text
status_capsule_config_v1
status_capsule_favorite_theme_ids
status_capsule_unlocked_asset_ids
status_capsule_catalog_last_selected_category
status_capsule_catalog_etag
status_capsule_catalog_validated_at
status_capsule_catalog_retry_after
```

Config JSON/newline set phải có deterministic codec và unit test Unicode/corrupt input.
Không lưu bitmap, URL có token hoặc runtime device status vào DataStore.

## Catalog domain

```kotlin
data class StatusThemeCatalog(
    val schemaVersion: Int,
    val catalogVersion: Long,
    val categories: List<StatusThemeCategory>,
    val themes: List<StatusThemeEntry>
)

data class StatusThemeEntry(
    val id: String,
    val version: Int,
    val name: String,
    val categoryIds: List<String>,
    val thumbnail: RemoteAsset,
    val archive: RemoteAsset,
    val entitlement: AssetEntitlement,
    val tags: List<String>
)

enum class AssetEntitlement { FREE, REWARDED, PREMIUM }
```

Stable pack key: `<id>@<version>`. Same ID/version is immutable.

## Proposed remote shape

Endpoint/repository cụ thể phải được owner duyệt. Boundary đề xuất:

```text
status-capsule/
├── json/catalog.json
├── packs/<themeId>-<version>.zip
└── thumb/<themeId>-<version>.webp
```

Nếu dùng private GitHub raw:

- token tiếp tục chỉ lấy từ Remote Config;
- interceptor chỉ gắn token cho exact allowlisted host/repository/path;
- cache-first + TTL 24h + ETag + rate-limit backoff như pet catalog;
- battery catalog có repository/interface riêng, không nhét vào `OwnerPetCatalogRepository`.

## Pack schema v1

```text
manifest.json
backgrounds/
icons/
emoji/
emotion/
animation/
```

Manifest đề xuất:

```json
{
  "schemaVersion": 1,
  "id": "cute.sky",
  "version": 1,
  "name": "Sky Friends",
  "defaultConfig": {
    "heightDp": 32,
    "backgroundAssetId": "sky_blue",
    "iconTint": "#101828"
  },
  "assets": [
    {
      "id": "sky_blue",
      "kind": "BACKGROUND",
      "path": "backgrounds/sky_blue.webp",
      "width": 1200,
      "height": 128,
      "stretchInsets": [220, 0, 220, 0],
      "sha256": "..."
    }
  ]
}
```

`stretchInsets` xác định vùng giữa có thể stretch; hai đầu trang trí không bị méo.

## Supported asset kinds

| Kind | Format | Runtime |
|---|---|---|
| Background | PNG/WebP RGBA | Nine-slice/stretch policy |
| Tintable icon | Monochrome alpha-mask PNG/WebP | Runtime tint |
| Multicolor icon/emoji | PNG/WebP RGBA | No tint |
| Animation | Bounded sprite sheet + frame metadata | Shared clock |
| Thumbnail | WebP/PNG | Coil/app UI only |

Remote pack không chứa:

- executable/code;
- font;
- SVG/XML;
- arbitrary Lottie JSON;
- audio/video;
- URL/network metadata ngoài catalog;
- script/shader.

Built-in VectorDrawable và fonts vẫn được compile trong APK.

## Installer security

Pipeline:

```text
verified download
  → random staging archive
  → safe unzip
  → parse manifest
  → validate path/type/size/hash/pixel/stretch/frame budgets
  → decode/preflight required assets
  → atomic promote to files/status_capsule/packs/<id>/<version>
```

Initial budgets:

- archive tối đa 10 MiB;
- unpacked tối đa 20 MiB;
- tối đa 128 entries;
- một entry tối đa 6 MiB;
- expansion ratio tối đa 100×;
- manifest tối đa 128 KiB;
- image tối đa 2048×2048;
- tổng decoded pixel tối đa 8M;
- animation tối đa 60 frame, 15 FPS, 6 giây loop;
- reject path traversal, backslash, NUL, duplicate/case-collision và symlink.

Các budget phải được profile trước khi lock production.

## Cache

- Built-in starter theme luôn là fallback.
- Catalog JSON cache riêng, archive download cache riêng, installed pack riêng.
- Bitmap cache clamp theo memory class; key gồm pack/id/version/density/size.
- Decode/pre-scale ngoài draw loop.
- Invalid/missing selected key fallback built-in và persist sanitized config.
- LRU archive cache có size cap; không xóa installed pack đang được applied.

## Content and licensing

Mỗi asset batch cần:

- owner/source;
- license/redistribution scope;
- creation/import date;
- source checksum;
- generated catalog checksum.

Không dùng anime/game/football logo, mascot hoặc character chỉ vì chúng xuất hiện trong
screenshot. Trademark/category marketing phải qua owner/legal approval.
