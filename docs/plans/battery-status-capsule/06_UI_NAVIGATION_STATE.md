# 06 — UI, Navigation and State Contract

> **PLANNED — NOT IMPLEMENTED**

## Routes

| Route | Screen | Contract |
|---|---|---|
| `battery_catalog` | Battery Catalog | Theme discovery, favorite, entitlement và runtime toggle |
| `status_capsule_editor?themeKey={themeKey}` | Full Editor | Draft configuration + shared preview + Apply |
| `status_component/{componentType}` | Component Editor | Edit một component trong parent draft |
| `status_assets/{assetKind}` | Asset Catalog | View all/search/category cho background/icon/emoji |

Arguments:

- `themeKey` URI-encode và optional.
- `componentType` parse enum an toàn; unknown pop back với error.
- `assetKind` parse enum an toàn.
- Không truyền full config qua route; editor-scoped draft repository/ViewModel ownership.

## Home integration

Current Home bottom navigation:

```text
Pet Home (selected) | Pet Catalog | Settings
```

Target:

```text
Pet Home | Pet Catalog | Battery
```

Settings tiếp tục mở từ header gear, nơi hiện đã có lối vào. Battery Catalog có cùng header
actions Favorites/Premium/Settings theo visual được owner duyệt.

## Screen/file contract

```text
ui/battery/catalog/
├── BatteryCatalogScreen.kt
├── BatteryCatalogViewModel.kt
└── BatteryCatalogUiState.kt

ui/battery/editor/
├── StatusCapsuleEditorScreen.kt
├── StatusCapsuleEditorViewModel.kt
└── StatusCapsuleEditorUiState.kt

ui/battery/component/
├── StatusComponentEditorScreen.kt
├── StatusComponentEditorViewModel.kt
└── StatusComponentEditorUiState.kt

ui/battery/assets/
├── StatusAssetCatalogScreen.kt
├── StatusAssetCatalogViewModel.kt
└── StatusAssetCatalogUiState.kt
```

Shared UI nằm trong `ui/battery/component` trước; chỉ move sang `ui/component` khi có
consumer ngoài battery feature.

## Battery Catalog

### UiState

```kotlin
data class BatteryCatalogUiState(
    val isLoading: Boolean = false,
    val categories: List<CategoryUiState> = emptyList(),
    val sections: List<ThemeSectionUiState> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val unlockedIds: Set<String> = emptySet(),
    val selectedThemeKey: String? = null,
    val isCapsuleRunning: Boolean = false,
    val displayMode: CapsuleDisplayMode = CapsuleDisplayMode.BELOW_SYSTEM_BAR,
    val runtimeCapability: CapsuleRuntimeCapability = CapsuleRuntimeCapability.READY,
    val isPremium: Boolean = false,
    val error: CatalogError? = null
)
```

### Events/effects

- `onThemeClicked`;
- `onFavoriteToggle`;
- `onViewAll`;
- `onCapsuleToggle`;
- `onPremium`;
- `onSettings`;
- effect `ShowRewarded(themeKey)`;
- effect `OpenOverlayPermission`;
- effect `ShowAccessibilityDisclosure`;
- effect `OpenAccessibilitySettings`;
- effect `RequestNotificationPermission`;
- effect `NavigateToEditor(themeKey)`.

### UI states

- loading skeleton;
- cache content + background refresh;
- content;
- offline with cached content;
- error with built-in themes;
- category empty;
- download progress/error;
- locked/premium/owned/selected card.

## Full Editor

### Layout order

```text
Top app bar
Sticky/shared preview
Scrollable content:
  Status and geometry
  Interface/background
  Emoji
  Battery
  Custom components
Sticky Apply CTA
```

Preview nằm dưới app bar, không giả status bar hệ thống. CTA không che item cuối; content
bottom padding gồm CTA + navigation bar + optional ad (nếu sau này được duyệt).

Editor có display mode selector:

- `Dưới thanh hệ thống`: giải thích cần quyền Display over other apps;
- `Che thanh hệ thống`: giải thích dùng Accessibility để đặt capsule trên status bar;
- chọn cover mode không mở Settings ngay; chỉ Apply mới chạy disclosure/consent flow;
- trạng thái service disabled hiển thị CTA `Bật lại` và option `Dùng chế độ bên dưới`;
- preview cover mode có frame minh họa native status bar bị che, nhưng luôn gắn label
  `Bản xem trước`, không giả system permission/UI.

### UiState

```kotlin
data class StatusCapsuleEditorUiState(
    val appliedConfig: StatusCapsuleConfig,
    val draftConfig: StatusCapsuleConfig,
    val previewStatus: DeviceStatusPreview,
    val availableAssets: EditorAssetsUiState,
    val isApplying: Boolean = false,
    val isDirty: Boolean = false,
    val validationErrors: List<EditorValidationError> = emptyList(),
    val runtimeCapability: CapsuleRuntimeCapability = CapsuleRuntimeCapability.READY,
    val pendingPermissionFlow: PendingCapsulePermission? = null,
    val applyError: ApplyError? = null
)
```

Slider UI giữ optimistic local state nhưng dispatch snapped value vào ViewModel. Không ghi
DataStore trên mỗi drag. Apply chỉ enabled khi config valid, dirty và không applying.

## Shared preview component

`StatusCapsulePreview` nhận:

- sanitized config;
- deterministic or live `DeviceStatusPreview`;
- available width;
- asset resolver;
- content description.

Cùng layout policy được dùng ở Compose preview và overlay renderer. Pixel output khác
framework nhưng rect/module visibility phải khớp qua shared pure layout model.

Default sample:

```text
12:30 · emoji · cellular · 4G label · Wi‑Fi · 73% · charging battery
```

Preview mode có toggle `Use live data` trong debug/internal build; release mặc định live
data khi available.

## Component Editor

Shell chung:

- Back;
- centered title;
- Done;
- preview;
- white card;
- optional enabled toggle;
- size;
- tint palette/custom color;
- component-specific content.

Renderer theo `componentType`:

| Type | Extra controls |
|---|---|
| Battery | style grid, show percent, percent size/tint, charging behavior |
| Date/time | 12/24 system/override, date toggle, localized format, bundled font |
| Emotion/emoji | category + asset grid |
| Animation | enable, validated asset, bounded animation speed |
| Data label | label 2G–9G, with copy explaining decorative/custom |
| Wi‑Fi/signal/hotspot/bell/airplane/charging | style grid và visibility behavior |

`Done` chỉ update editor draft. Back khi component dirty hỏi discard nếu parent draft chưa
nhận thay đổi.

## Asset Catalog

- Search normalized Unicode.
- Category rail và Favorites.
- 2-column grid phone, adaptive 3–6 columns tablet/landscape.
- Download/progress/retry.
- Selecting asset returns stable key to parent draft.
- No commit-on-open; selection explicit.

## Design tokens

- Tái sử dụng Cute Pet light/cozy palette.
- User-facing string trong resources.
- Color token trong `colors.xml`; custom selected color là validated data, ngoại lệ có chủ đích.
- Sizing dùng SDP/SSP theo project; screenshot raster không được đoán thành exact Figma px.
- Major screen/component có Preview với fake state.
- Modifier interactive: size → shadow → clip → background → border → clickable → padding.

## Color picker

Palette preset:

- automatic contrast;
- black;
- white;
- yellow;
- blue;
- cyan;
- coral;
- purple;
- green.

Eyedropper trong screenshot được thay bằng `Custom color` dialog/picker có:

- HSV/RGB or hex entry;
- alpha disabled cho text/icon;
- contrast preview;
- invalid input error;
- recent colors local-only.

## Back stack

- Battery Catalog pop về Home.
- Full Editor pop về Catalog; dirty state confirm.
- Component Editor/Asset Catalog pop về Full Editor và giữ draft.
- Premium mở từ locked theme quay lại đúng Catalog/editor intent sau entitlement refresh.
- Overlay settings/notification permission quay lại pending Apply an toàn; callback consume một lần.

## Analytics screen names dự kiến

- `battery_catalog`;
- `status_capsule_editor`;
- `status_component_editor`;
- `status_asset_catalog`.

`component_type`, `asset_kind`, `theme_entitlement` là event parameter allowlist; không tạo
dynamic screen name.
