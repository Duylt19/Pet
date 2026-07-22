# F10 — Quick Access (Social Shortcuts)

Grid 8 shortcut social media trên Home Browser tab.

---

## 1. Shortcut List

Từ screenshot #2 (Home tab) — observe 8 items: Fb, Ins, Tic, Whats, Tw, Vieo, Thre, Daimo.

| # | Label hiển thị | URL | Icon (drawable) | Background color |
|---|----------------|-----|-----------------|------------------|
| 1 | Fb | `https://m.facebook.com/watch/` | `ic_shortcut_facebook` | Blue `#1877F2` |
| 2 | Ins | `https://www.instagram.com/explore/` | `ic_shortcut_instagram` | Gradient pink/purple |
| 3 | Tic | `https://www.tiktok.com/foryou` | `ic_shortcut_tiktok` | Black `#000000` |
| 4 | Whats | `https://web.whatsapp.com` | `ic_shortcut_whatsapp` | Green `#25D366` |
| 5 | Tw | `https://x.com` | `ic_shortcut_x` | Black `#000000` |
| 6 | Vieo | `https://vimeo.com` (TODO confirm — có thể là "Vivo" hoặc "Video") | `ic_shortcut_vimeo` | Blue `#1AB7EA` |
| 7 | Thre | `https://www.threads.net` | `ic_shortcut_threads` | Black `#000000` |
| 8 | Daimo | `https://www.dailymotion.com` (TODO confirm — Dailymotion) | `ic_shortcut_dailymotion` | Blue `#0066DC` |

> **⚠️ TODO confirm với user:** Label "Vieo" và "Daimo" bị cắt trên screenshot. Cần verify với designer. Mặc định dự đoán Vimeo và Dailymotion.

---

## 2. Data Class & List

File: `data/model/QuickAccessShortcut.kt`

```kotlin
data class QuickAccessShortcut(
    val id: String,
    val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val url: String,
)

object QuickAccessShortcuts {
    val DEFAULTS: List<QuickAccessShortcut> = listOf(
        QuickAccessShortcut("fb", R.string.quick_access_fb_label, R.drawable.ic_shortcut_facebook, "https://m.facebook.com/watch/"),
        QuickAccessShortcut("ins", R.string.quick_access_ins_label, R.drawable.ic_shortcut_instagram, "https://www.instagram.com/explore/"),
        QuickAccessShortcut("tic", R.string.quick_access_tic_label, R.drawable.ic_shortcut_tiktok, "https://www.tiktok.com/foryou"),
        QuickAccessShortcut("whats", R.string.quick_access_whats_label, R.drawable.ic_shortcut_whatsapp, "https://web.whatsapp.com"),
        QuickAccessShortcut("tw", R.string.quick_access_tw_label, R.drawable.ic_shortcut_x, "https://x.com"),
        QuickAccessShortcut("vieo", R.string.quick_access_vieo_label, R.drawable.ic_shortcut_vimeo, "https://vimeo.com"),
        QuickAccessShortcut("thre", R.string.quick_access_thre_label, R.drawable.ic_shortcut_threads, "https://www.threads.net"),
        QuickAccessShortcut("daimo", R.string.quick_access_daimo_label, R.drawable.ic_shortcut_dailymotion, "https://www.dailymotion.com"),
    )
}
```

---

## 3. Strings

```xml
<string name="quick_access_fb_label">Fb</string>
<string name="quick_access_ins_label">Ins</string>
<string name="quick_access_tic_label">Tic</string>
<string name="quick_access_whats_label">Whats</string>
<string name="quick_access_tw_label">Tw</string>
<string name="quick_access_vieo_label">Vieo</string>
<string name="quick_access_thre_label">Thre</string>
<string name="quick_access_daimo_label">Daimo</string>
```

---

## 4. UI — Grid 2 Cột

```kotlin
@Composable
fun QuickAccessGrid(
    shortcuts: List<QuickAccessShortcut>,
    onItemClick: (QuickAccessShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
    ) {
        items(shortcuts, key = { it.id }) { item ->
            QuickAccessItem(
                iconRes = item.iconRes,
                label = stringResource(item.labelRes),
                onClick = { onItemClick(item) },
            )
        }
    }
}
```

**Lưu ý:** Trong Home Browser tab dùng LazyColumn → grid bên trong sẽ infinite height. Cần dùng `Modifier.heightIn(max = ...)` hoặc dùng `Column` + chunked thay vì `LazyVerticalGrid`. Pattern an toàn:

```kotlin
shortcuts.chunked(2).forEach { pair ->
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
    ) {
        pair.forEach { item ->
            QuickAccessItem(
                modifier = Modifier.weight(1f),
                iconRes = item.iconRes,
                label = stringResource(item.labelRes),
                onClick = { onItemClick(item) },
            )
        }
        if (pair.size == 1) Spacer(Modifier.weight(1f))   // căn cột
    }
    Spacer(Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
}
```

---

## 5. Action

```kotlin
fun onShortcutClicked(shortcut: QuickAccessShortcut) {
    navigateWithAd(context) {
        navController.safeNavigate(buildBrowserWebViewRoute(shortcut.url))
    }
}
```

---

## 6. Composable Item

Xem [06_UI_DESIGN_SYSTEM.md](../06_UI_DESIGN_SYSTEM.md) section 6.5 — `QuickAccessItem`. Cấu trúc:

```kotlin
@Composable
fun QuickAccessItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._18sdp)))
            .background(colorResource(R.color.colors_FFFFFF))
            .border(1.dp, colorResource(R.color.colors_EEEEEE), RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._18sdp)))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(com.intuit.sdp.R.dimen._9sdp),
                vertical = dimensionResource(com.intuit.sdp.R.dimen._6sdp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._24sdp))
        )
        Spacer(Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        // chevron tùy chọn — screenshot không rõ, mặc định KHÔNG chevron, chỉ pill
    }
}
```

---

## 7. Customization (v2)

V1: hardcode 8 shortcut.

V2 idea:
- User long-press shortcut → menu Edit/Remove
- "Add shortcut" button → tab nhập URL + chọn icon từ favicon
- Drag to reorder
- Sync qua Firebase RC để control central list theo region

---

## 8. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| Icon drawable load fail | Coil fallback default ic_shortcut_default |
| URL site đổi (vd Twitter → X) | Update URL trong `QuickAccessShortcuts.DEFAULTS` |
| Site bị block ở region | Vẫn cho phép tap, WebView báo error |
| Tap quá nhanh | `navigateWithAd` đã có debounce |

---

## 9. Liên Quan

- [S06a_HOME_BROWSER_TAB.md](../screens/S06a_HOME_BROWSER_TAB.md)
- [F01_BROWSER_CORE.md](F01_BROWSER_CORE.md) — receives URL
