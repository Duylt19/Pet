# S11 — Search Engine Picker (Bottom Sheet)

## Visual Reference

- Screenshot: [Screenshot_20260608-095415.png](../assets/screenshots/Screenshot_20260608-095415.png)

## Mục Đích

Bottom sheet cho user chọn 1 trong 6 search engines mặc định. Selection lưu DataStore và áp dụng ngay vào SearchBar Home tab.

## Vị Trí Trong Navigation

- KHÔNG phải route. ModalBottomSheet trong Settings screen.
- Trigger từ: Settings > Search Engine row, BrowserWebView menu > Change search engine

## Layout Breakdown

```
┌─────────────────────────────────────┐
│                                     │  <- bg đen 50% alpha (scrim)
│                                     │
│  (Settings content phía sau mờ)     │
│                                     │
├─────────────────────────────────────┤   <- top corner rounded _12sdp
│       ━━━                          │   <- drag handle
│                                     │
│  Set default search engine          │  <- Title L Bold
│                                     │
│  ┌────────────────────────────────┐ │
│  │ G  Google                  ✓  │ │  <- selected: border purple + check
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ B  Bing                    ○  │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ Y! Yahoo                   ○  │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ 🦆 DuckDuckGo              ○  │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ Y  Yandex                  ○  │ │
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ CC Coc Coc                 ○  │ │
│  └────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

**Specs:**

- Sheet shape: top corners radius `_12sdp`, bottom flat
- Drag handle: 36dp x 4dp pill `colors_B8B8B8` centered top
- Padding container `_18sdp`
- Title spacing bottom `_12sdp`
- Engine row:
  - Height `_36sdp` (~48dp)
  - Background `colors_F2F2F7`
  - Radius `_9sdp`
  - Padding horizontal `_12sdp`
  - Border: selected → `colorPrimary` 1.5dp; unselected → `colors_EEEEEE` 1dp
  - Icon left `_18sdp` (color unspecified — multi-color logos)
  - Name center-left Body L
  - Right: check icon (selected) hoặc radio empty circle
- Row spacing `_6sdp`

## States

| State | Display |
|-------|---------|
| Sheet visible | Render với current selection highlighted |
| Tap row | Update selection → save → dismiss sheet |
| Swipe down | Dismiss |
| Tap scrim | Dismiss |
| Back press | Dismiss |

## ViewModel Contract

```kotlin
// Reuse SettingsViewModel
fun onSearchEngineClicked() { _uiState.update { it.copy(showSearchEngineSheet = true) } }
fun onSearchEngineSelected(engine: SearchEngine) = viewModelScope.launch {
    searchEngineRepository.setCurrent(engine)
    _uiState.update { it.copy(showSearchEngineSheet = false) }
}
fun onDismissSearchEngineSheet() { _uiState.update { it.copy(showSearchEngineSheet = false) } }
```

Composable:
```kotlin
if (state.showSearchEngineSheet) {
    SearchEnginePickerSheet(
        selected = state.currentSearchEngine,
        onSelect = viewModel::onSearchEngineSelected,
        onDismiss = viewModel::onDismissSearchEngineSheet,
    )
}
```

## Resources

```xml
<string name="search_engine_picker_title">Set default search engine</string>
<string name="search_engine_name_google">Google</string>
<string name="search_engine_name_bing">Bing</string>
<string name="search_engine_name_yahoo">Yahoo</string>
<string name="search_engine_name_duckduckgo">DuckDuckGo</string>
<string name="search_engine_name_yandex">Yandex</string>
<string name="search_engine_name_coccoc">Coc Coc</string>
```

Drawables (multi-color, không tint):
- `ic_google_g.xml`
- `ic_bing_b.xml`
- `ic_yahoo_y.xml`
- `ic_duckduckgo.xml`
- `ic_yandex.xml`
- `ic_coccoc.xml`
- `ic_check.xml`, `ic_radio_empty.xml`

## Ads

- KHÔNG ads trong bottom sheet

## Edge Cases & Accessibility

- Engine icon load fail → fallback `ic_search_default`
- Tap selected row lần nữa → vẫn dismiss (no-op selection)
- Sheet hiện trong Settings → vẫn render banner ad của Settings (nếu có)
- contentDescription: "Set Google as default search engine, selected"
- Min touch target 48dp toàn row

## Acceptance Criteria

- [ ] Layout match screenshot #9
- [ ] 6 engine rows
- [ ] Selected row có border purple + check
- [ ] Tap row → save + dismiss
- [ ] Drag/swipe dismiss
- [ ] Selection apply ngay vào Home SearchBar leading icon
- [ ] Icon multi-color giữ nguyên màu

## Liên Quan

- [F07_SEARCH_ENGINE.md](../features/F07_SEARCH_ENGINE.md)
- [S09_SETTINGS.md](S09_SETTINGS.md)
- [S06a_HOME_BROWSER_TAB.md](S06a_HOME_BROWSER_TAB.md)
