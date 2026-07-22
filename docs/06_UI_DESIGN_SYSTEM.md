# 06 — UI Design System

Tổng hợp design tokens, typography, spacing, shape, component tái dùng và icon cho toàn app.

---

## 1. Color Palette

### 1.1. Brand & Primary

| Token | Hex | Dùng cho |
|-------|-----|----------|
| `colorPrimary` | `#7C5BFB` | Primary purple — primary text accent, selected indicator |
| `colors_5B6FFB` | `#5B6FFB` | Gradient start (Set as default button) |
| `colors_7C5BFB` | `#7C5BFB` | Gradient end |
| `colors_8B5CF6` | `#8B5CF6` | Bookmarks button purple |
| `colors_F5F3FF` | `#F5F3FF` | Light purple bg (selected tab indicator background) |

**Primary gradient definition (dùng trong code):**
```kotlin
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(
        colorResource(R.color.colors_5B6FFB),
        colorResource(R.color.colors_7C5BFB),
    )
)
```

### 1.2. Semantic

| Token | Hex | Dùng |
|-------|-----|------|
| `colors_22C55E` | `#22C55E` | Success / Fast downloading icon bg |
| `colors_FB923C` | `#FB923C` | Warning / Easy to use icon bg |
| `colors_EF4444` | `#EF4444` | Error / Destructive (Clear history) |
| `colors_FB2C36` | `#FB2C36` | Red action (close all tabs) |

### 1.3. Neutral

| Token | Hex | Dùng |
|-------|-----|------|
| `colors_FFFFFF` | `#FFFFFF` | App background light |
| `colors_F7F8FA` | `#F7F8FA` | Card / section background |
| `colors_F2F2F7` | `#F2F2F7` | Search bar background |
| `colors_EEEEEE` | `#EEEEEE` | Divider |
| `colors_E5E7EB` | `#E5E7EB` | Tab card border |
| `colors_B8B8B8` | `#B8B8B8` | Tertiary text |
| `colors_808080` | `#808080` | Secondary text / icon default |
| `colors_4A4A4A` | `#4A4A4A` | Subhead text |
| `colors_1F1F1F` | `#1F1F1F` | Body text dark |
| `colors_000000` | `#000000` | Title / heading |

### 1.4. Storage Bar Colors (Files tab)

| Token | Hex | Mục |
|-------|-----|-----|
| `colors_F59E0B` | `#F59E0B` | Images segment |
| `colors_EF4444` | `#EF4444` | Video segment |
| `colors_8B5CF6` | `#8B5CF6` | Music segment |
| `colors_D1D5DB` | `#D1D5DB` | Empty (unused storage) |

### 1.5. Icon Backgrounds (Set as default onboarding)

| Token | Hex | Icon |
|-------|-----|------|
| `colors_E0E7FF` | `#E0E7FF` | Private browsing (mask) — light purple bg |
| `colors_DCFCE7` | `#DCFCE7` | Fast downloading (arrow) — light green bg |
| `colors_FFEDD5` | `#FFEDD5` | Easy to use (thumbs up) — light orange bg |

Foreground icon: `colorPrimary`, `colors_22C55E`, `colors_FB923C` tương ứng.

---

## 2. Typography

App dùng default Inter (Compose Material 3 default `FontFamily.SansSerif`). V1 không custom font.

### Scale (đã chia 1.3 từ Figma → ssp tương ứng)

| Vai trò | sp (sau chia 1.3) | Figma original | Weight | Dùng |
|---------|---|---|--------|------|
| Display | 22ssp | 28sp | Bold | Onboarding header ("Set as the default browser") |
| Title L | 17ssp | 22sp | Bold | Screen header trong top bar |
| Title M | 14ssp | 18sp | SemiBold | Section title ("General", "Other Settings") |
| Body L | 12ssp | 16sp | Regular | Body text, button label |
| Body M | 11ssp | 14sp | Regular | Description, list item |
| Caption | 10ssp | 13sp | Regular | Helper text, version |
| Tiny | 9ssp | 12sp | Regular | Disclaimer, "Ad" badge |

Cách dùng:
```kotlin
Text(
    text = stringResource(R.string.setdefault_header_text),
    fontSize = with(LocalDensity.current) {
        dimensionResource(com.intuit.ssp.R.dimen._22ssp).value.sp
    },
    fontWeight = FontWeight.Bold,
    color = colorResource(R.color.colors_000000),
)
```

**Hoặc** define `Typography` trong `theme/Type.kt`:

```kotlin
@Composable
fun appTypography() = Typography(
    headlineLarge = TextStyle(fontSize = ssp(22), fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = ssp(17), fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = ssp(14), fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = ssp(12)),
    bodyMedium = TextStyle(fontSize = ssp(11)),
    labelSmall = TextStyle(fontSize = ssp(10)),
)

@Composable
private fun ssp(value: Int): TextUnit {
    val res = context.resources.getIdentifier("_${value}ssp", "dimen", "com.intuit.ssp")
    return dimensionResource(res).value.sp
}
```

---

## 3. Spacing

Tất cả spacing dùng **sdp** (chia 1.3 từ Figma):

| Token | Figma dp | sdp |
|-------|----------|-----|
| Spacing 4 | 4 | `_3sdp` |
| Spacing 8 | 8 | `_6sdp` |
| Spacing 12 | 12 | `_9sdp` |
| Spacing 16 | 16 | `_12sdp` |
| Spacing 20 | 20 | `_16sdp` |
| Spacing 24 | 24 | `_18sdp` |
| Spacing 32 | 32 | `_24sdp` |
| Spacing 40 | 40 | `_30sdp` |
| Spacing 48 | 48 | `_36sdp` |

Cách dùng:
```kotlin
Modifier.padding(
    horizontal = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
    vertical = dimensionResource(com.intuit.sdp.R.dimen._9sdp),
)
```

---

## 4. Shape & Corner Radius

| Component | Radius (sdp) |
|-----------|--------------|
| Card | `_9sdp` (12dp Figma) |
| Button pill | full circle (height/2) — dùng `CircleShape` hoặc `RoundedCornerShape(50)` |
| Bottom sheet top corner | `_12sdp` (16dp Figma) |
| Search bar | `_18sdp` (24dp Figma — gần như pill) |
| Tab card | `_12sdp` |
| Settings card | `_9sdp` |
| Quick access item chip | `_18sdp` (pill) |
| Icon background circle | `CircleShape` |

---

## 5. Elevation & Shadow

V1 dùng flat design — minimal shadow. Chỉ có:

- Floating bottom nav: `elevation = _3sdp`
- Bottom sheet: handled by Material 3 default
- Tab card đang active: subtle shadow `_2sdp`

---

## 6. Reusable Composables (ui/component/)

Đặt trong `app/src/main/java/com/asianmobile/privatebrower/ui/component/`. Mỗi file 1 component public.

### 6.1. `PrimaryGradientButton.kt`

```kotlin
@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

- Height `_42sdp` (56dp Figma)
- Background: PrimaryGradient brush
- Shape: pill (`RoundedCornerShape(50)`)
- Text: white, Body L, SemiBold
- Disabled: alpha 0.5

### 6.2. `SecondaryTextButton.kt`

Text-only button, color `colors_808080`, Body L. Dùng cho "Later", "Skip".

### 6.3. `SearchBar.kt`

```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String = stringResource(R.string.home_search_placeholder_text),
    leadingIconRes: Int = R.drawable.ic_google_g,
    modifier: Modifier = Modifier,
)
```

- Background: `colors_F2F2F7`
- Shape: pill `_18sdp`
- Height: `_30sdp` (40dp Figma)
- Leading: 24dp G icon (`ic_google_g` hoặc icon engine đang chọn)
- Placeholder: "Search or type URL"
- IME action: Search → call onSubmit

### 6.4. `BookmarksButton.kt`

Pill button full width, purple `colors_8B5CF6`, icon "R" trong circle trắng + text "Bookmarks" trắng.

### 6.5. `QuickAccessItem.kt`

```kotlin
@Composable
fun QuickAccessItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- Row: circle icon (40dp, colored) + label (Body L black) + Spacer + Chevron right
- Background pill `_18sdp`, light border `colors_EEEEEE`
- Padding horizontal `_9sdp` vertical `_6sdp`

### 6.6. `BottomNavItem.kt`

```kotlin
@Composable
fun BottomNavItem(
    iconRes: Int,
    label: String,
    badge: Int? = null,
    selected: Boolean,
    onClick: () -> Unit,
)
```

- Vertical layout: icon trên + label dưới (Caption)
- Selected → icon tint `colorPrimary` + label color `colorPrimary` + indicator line trên (4dp)
- Badge: small purple circle với số trên top-right icon (chỉ tab Tabs)

### 6.7. `EmptyState.kt`

```kotlin
@Composable
fun EmptyState(
    illustrationRes: Int,
    message: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

- Center column: illustration (folder face) `_75sdp` + Spacer `_6sdp` + message (Body L, `colors_808080`)
- Nếu có CTA: thêm Spacer `_18sdp` + `PrimaryGradientButton`

### 6.8. `SettingsRow.kt`

```kotlin
@Composable
fun SettingsRow(
    iconRes: Int,
    title: String,
    trailing: SettingsTrailing = SettingsTrailing.Chevron,
    onClick: () -> Unit,
)

sealed class SettingsTrailing {
    object Chevron : SettingsTrailing()
    data class Text(val value: String) : SettingsTrailing()
    data class Custom(val content: @Composable () -> Unit) : SettingsTrailing()
}
```

- Row height `_36sdp` (48dp)
- Icon `_18sdp` (24dp) tint primary text
- Title Body L black
- Trailing: chevron right `_12sdp` `colors_B8B8B8` hoặc text/custom

### 6.9. `SettingsSection.kt`

```kotlin
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
)
```

- Section title (Title M, `colors_808080`) padding bottom `_4sdp`
- Card container (background `colors_FFFFFF`, radius `_9sdp`, border `colors_EEEEEE` 1dp)
- Rows trong content, divider tự thêm giữa rows

### 6.10. `TabCard.kt`

```kotlin
@Composable
fun TabCard(
    thumbnailPath: String?,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- Width 80% screen, height 60% screen
- Container card radius `_9sdp`, border khi active = primary 2dp
- Top: title bar với close X icon
- Body: Coil load thumbnail bitmap from file path; fallback placeholder

### 6.11. `StorageBar.kt`

```kotlin
@Composable
fun StorageBar(
    segments: List<StorageSegment>,    // each has color + percent
    modifier: Modifier = Modifier,
)

data class StorageSegment(val colorRes: Int, val labelRes: Int, val percent: Float)
```

- Height `_4sdp`
- Row of `Box` weighted by percent, colored by segment
- Above bar: text "X.X GB / Y.Y GB" (Body M) + "Internal Storage" (Caption `colors_808080`)
- Below bar: legend (dot + label per segment)

### 6.12. `AppHeaderBar.kt`

```kotlin
@Composable
fun AppHeaderBar(
    title: String,
    leadingIcon: AppHeaderLeading = AppHeaderLeading.Hamburger,
    onLeadingClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
)

sealed class AppHeaderLeading {
    object Hamburger : AppHeaderLeading()   // ic_hamburger
    object Back : AppHeaderLeading()        // ic_arrow_back
    object Close : AppHeaderLeading()       // ic_close_x
}
```

- Height `_36sdp`
- Background transparent (over screen bg)
- Title centered, Title L Bold
- Leading icon left, 24dp tint primary text

### 6.13. `SegmentedTabRow.kt`

Cho Tabs tab (Normal/Incognito) và Bookmarks/History.

```kotlin
@Composable
fun SegmentedTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
)
```

- Pill container `colors_F2F2F7` radius `_18sdp`
- Selected pill background white + shadow nhẹ
- Tab label Body L, selected color primary text + bold, unselected `colors_808080`

---

## 7. Iconography

Tất cả icons là VectorDrawable XML trong `res/drawable/`, naming `ic_*`. Đa số 24dp viewport, tint set runtime.

### Danh sách icon cần thêm

| Icon name | Mô tả | Nguồn gợi ý |
|-----------|-------|-------------|
| `ic_hamburger` | 3 đường ngang | Material Symbols: `menu` |
| `ic_arrow_back` | Mũi tên trái | Material Symbols: `arrow_back` |
| `ic_close_x` | X | Material Symbols: `close` |
| `ic_chevron_right` | Mũi tên phải nhỏ | Material Symbols: `chevron_right` |
| `ic_plus` | Dấu cộng | Material Symbols: `add` |
| `ic_trash` | Thùng rác | Material Symbols: `delete` |
| `ic_check` | Tích | Material Symbols: `check_circle` |
| `ic_search_google` | Logo Google G | custom (multi-color) |
| `ic_google_g`, `ic_bing_b`, `ic_yahoo_y`, `ic_duckduckgo`, `ic_yandex`, `ic_coccoc` | Logo search engine | Custom svg |
| `ic_bookmarks_pill` | Chữ "R" trong circle | custom |
| `ic_tab_home`, `ic_tab_tabs`, `ic_tab_files`, `ic_tab_progress` | Bottom nav icons (outline 24dp) | Material outline |
| `ic_image_category`, `ic_video_category`, `ic_music_category` | Square gradient icons | Custom |
| `ic_folder_empty` | Folder mặt cười rỗng | Custom (đối chiếu screenshot) |
| `ic_download_arrow` | Mũi tên xuống trong circle | Material `download` |
| `ic_set_default_mask` | Mặt nạ (logo app) | App brand asset |
| `ic_thumbs_up` | 👍 | Material `thumb_up` |
| `ic_lang_globe` | Globe | Material `language` |
| `ic_chat_bubble` | Bong bóng chat | Material `chat_bubble` |
| `ic_share` | Share | Material `share` |
| `ic_shield_warning` | Khiên cảnh báo (privacy) | Material `policy` |
| `ic_pencil` | Pencil icon (edit tabs) | Material `edit` |
| `ic_more_vert` | 3 dot dọc (WebView menu) | Material `more_vert` |
| `ic_refresh` | Refresh | Material `refresh` |
| `ic_forward` | Forward arrow | Material `arrow_forward` |
| `ic_lock_secure` | Lock (URL https) | Material `lock` |
| `ic_incognito_mask` | Mặt nạ incognito | Material `visibility_off` hoặc custom |

### Quy trình thêm icon

Xem [SKILL.md mục 7](../.agents/skills/android_developer/SKILL.md) — pipeline `svg2vectordrawable`:

```bash
cmd /c "node .agents/skills/android_developer/scripts/svg_to_drawable.js <svg_url> tab_home --size 24"
```

### Multi-color icons (search engines)

Icon search engine có nhiều màu cố định (Google G màu xanh đỏ vàng) — **không tint**, để màu gốc. Khi đặt vào Icon composable, dùng `Icon(painter = painterResource(...), contentDescription = ..., tint = Color.Unspecified)`.

---

## 8. Dark Mode

V1 chỉ light mode. Setup vẫn để mở rộng:

- `theme/Color.kt` định nghĩa cả `LightColors` và `DarkColors`
- `BaseAppTheme` luôn dùng `LightColors` cho v1 (force light)
- Status bar: `WindowInsetsControllerCompat.isAppearanceLightStatusBars = true`

V2: implement Switch trong Settings, persist `IS_DARK_MODE` → switch palette.

---

## 9. Accessibility

- **Touch target tối thiểu 48dp** — wrap clickable Icon size nhỏ trong `Box` 48dp + click hoặc dùng IconButton (chỉ khi không sai size visual)
- **contentDescription** bắt buộc cho mọi `Icon`, `Image` interactive
- **TalkBack labels** cho bottom nav tabs (vd "Home tab, selected, 1 of 4")
- **Color contrast** WCAG AA cho text + bg (verify khi thay đổi color)

---

## 10. Component Library Mapping

Bảng nhanh: Composable nào dùng ở screen nào.

| Composable | Dùng trong screen |
|------------|-------------------|
| `PrimaryGradientButton` | S05, S10, S06c, S06d |
| `SearchBar` | S06a |
| `BookmarksButton` | S06a |
| `QuickAccessItem` | S06a |
| `BottomNavItem` | S06 (container) |
| `EmptyState` | S06d, S08 |
| `SettingsRow` | S09 |
| `SettingsSection` | S09 |
| `TabCard` | S06b |
| `StorageBar` | S06c |
| `AppHeaderBar` | S06, S08, S09, S10, S12 |
| `SegmentedTabRow` | S06b, S08 |
