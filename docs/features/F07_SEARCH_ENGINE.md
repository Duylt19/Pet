# F07 — Search Engine

6 search engines, switchable, URL builder, autocomplete URL/query detection.

---

## 1. Enum

File: `data/model/SearchEngine.kt`

```kotlin
enum class SearchEngine(
    val id: String,
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val queryUrlTemplate: String,
    val homeUrl: String,
) {
    GOOGLE("google", "Google", R.drawable.ic_google_g, "https://www.google.com/search?q=%s", "https://www.google.com"),
    BING("bing", "Bing", R.drawable.ic_bing_b, "https://www.bing.com/search?q=%s", "https://www.bing.com"),
    YAHOO("yahoo", "Yahoo", R.drawable.ic_yahoo_y, "https://search.yahoo.com/search?p=%s", "https://www.yahoo.com"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", R.drawable.ic_duckduckgo, "https://duckduckgo.com/?q=%s", "https://duckduckgo.com"),
    YANDEX("yandex", "Yandex", R.drawable.ic_yandex, "https://yandex.com/search/?text=%s", "https://yandex.com"),
    COC_COC("coccoc", "Coc Coc", R.drawable.ic_coccoc, "https://coccoc.com/search?query=%s", "https://coccoc.com");

    companion object {
        fun fromId(id: String): SearchEngine = values().firstOrNull { it.id == id } ?: GOOGLE
    }
}
```

---

## 2. URL Builder

File: `data/util/UrlBuilder.kt`

```kotlin
object UrlBuilder {
    private val URL_REGEX = Regex(
        "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=#]*)?\$",
        RegexOption.IGNORE_CASE,
    )

    fun buildUrl(input: String, engine: SearchEngine): String {
        val trimmed = input.trim()
        return when {
            trimmed.isBlank() -> engine.homeUrl
            isLikelyUrl(trimmed) -> normalizeUrl(trimmed)
            else -> engine.queryUrlTemplate.format(Uri.encode(trimmed))
        }
    }

    private fun isLikelyUrl(input: String): Boolean {
        if (input.contains(" ")) return false
        return URL_REGEX.matches(input) ||
            input.startsWith("http://") ||
            input.startsWith("https://") ||
            input.startsWith("about:") ||
            input.startsWith("javascript:") ||
            (input.contains(".") && !input.endsWith("."))
    }

    private fun normalizeUrl(input: String): String =
        if (input.startsWith("http://") || input.startsWith("https://")) input
        else "https://$input"
}
```

Test cases:
| Input | Output |
|-------|--------|
| `google.com` | `https://google.com` |
| `https://x.com` | `https://x.com` |
| `cats` | `https://www.google.com/search?q=cats` (engine GOOGLE) |
| `dogs cats` | `https://www.google.com/search?q=dogs%20cats` |
| `1.2.3.4` | `https://1.2.3.4` |
| ` ` (empty) | engine.homeUrl |

---

## 3. Repository

File: `data/repository/SearchEngineRepositoryImpl.kt`

```kotlin
class SearchEngineRepositoryImpl @Inject constructor(
    private val dataStore: DataStoreManager,
) : SearchEngineRepository {

    override fun observeCurrent(): Flow<SearchEngine> =
        dataStore.selectedSearchEngine.map { SearchEngine.fromId(it) }

    override suspend fun setCurrent(engine: SearchEngine) {
        dataStore.setSelectedSearchEngine(engine.id)
    }
}
```

---

## 4. UI Bottom Sheet

File: `ui/searchengine/SearchEnginePickerSheet.kt`

```kotlin
@Composable
fun SearchEnginePickerSheet(
    selected: SearchEngine,
    onSelect: (SearchEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
            topEnd = dimensionResource(com.intuit.sdp.R.dimen._12sdp),
        ),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(com.intuit.sdp.R.dimen._12sdp))) {
            Text(
                text = stringResource(R.string.search_engine_picker_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))

            SearchEngine.values().forEach { engine ->
                SearchEngineRow(
                    engine = engine,
                    isSelected = engine == selected,
                    onClick = { onSelect(engine); onDismiss() }
                )
                Spacer(Modifier.height(dimensionResource(com.intuit.sdp.R.dimen._6sdp)))
            }
        }
    }
}

@Composable
private fun SearchEngineRow(engine: SearchEngine, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) colorResource(R.color.colorPrimary) else colorResource(R.color.colors_EEEEEE)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
            .background(colorResource(R.color.colors_F2F2F7))
            .border(1.dp, borderColor, RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
            .clickable(onClick = onClick)
            .padding(dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(engine.iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
        )
        Spacer(Modifier.width(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
        Text(engine.displayName, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(painterResource(R.drawable.ic_check), null, tint = colorResource(R.color.colorPrimary))
        } else {
            Icon(painterResource(R.drawable.ic_radio_empty), null, tint = colorResource(R.color.colors_B8B8B8))
        }
    }
}
```

Xem layout chính xác: [S11_SEARCH_ENGINE_PICKER.md](../screens/S11_SEARCH_ENGINE_PICKER.md).

---

## 5. Integration

### 5.1. Home Browser tab — SearchBar leading icon

SearchBar leading icon = `engine.iconRes` thay vì hardcode Google G:

```kotlin
val currentEngine by viewModel.uiState.map { it.searchEngine }.collectAsStateWithLifecycle(SearchEngine.GOOGLE)

SearchBar(
    query = state.searchQuery,
    onQueryChange = viewModel::onSearchQueryChanged,
    onSubmit = { viewModel.onSearchSubmit() },
    leadingIconRes = currentEngine.iconRes,
)
```

### 5.2. Submit search

```kotlin
fun onSearchSubmit() {
    val url = UrlBuilder.buildUrl(_uiState.value.searchQuery, _uiState.value.searchEngine)
    _navigationEvent.tryEmit(NavigationEvent.OpenBrowser(url))
}
```

### 5.3. Settings row trailing

```kotlin
SettingsRow(
    iconRes = currentEngine.iconRes,
    title = stringResource(R.string.settings_search_engine_title),
    trailing = SettingsTrailing.Text(currentEngine.displayName),  // hoặc Chevron + Text
    onClick = { showPickerSheet = true },
)
```

---

## 6. Default Engine

- Default: **GOOGLE** (theo phổ biến)
- Có thể override theo country (RC):
  - VN → COC_COC
  - RU → YANDEX
  - Khác → GOOGLE

Logic trong `MainViewModel.init`:
```kotlin
init {
    viewModelScope.launch {
        if (!preferencesRepository.isSearchEngineSet()) {
            val defaultByCountry = remoteConfig.getString("default_search_engine_${country()}")
            if (defaultByCountry.isNotBlank()) {
                searchEngineRepository.setCurrent(SearchEngine.fromId(defaultByCountry))
            }
        }
    }
}
```

---

## 7. Edge Cases

| Trường hợp | Xử lý |
|-----------|-------|
| Engine icon load fail | Coil/Painter fallback ic_search_default |
| User chọn engine không hỗ trợ regional | Vẫn cho phép (vd Yandex ngoài Nga vẫn ok) |
| URL có ký tự đặc biệt | `Uri.encode` xử lý |
| Empty query submit | Mở engine homeUrl (xem URL builder) |
| User paste URL có space (vd "google.com /search") | Treat as query, encode space |
| Coc Coc URL outdated (chuyển server) | Update template trong enum, không thay đổi logic |

---

## 8. Liên Quan

- [S06a_HOME_BROWSER_TAB.md](../screens/S06a_HOME_BROWSER_TAB.md)
- [S07_BROWSER_WEBVIEW.md](../screens/S07_BROWSER_WEBVIEW.md)
- [S09_SETTINGS.md](../screens/S09_SETTINGS.md)
- [S11_SEARCH_ENGINE_PICKER.md](../screens/S11_SEARCH_ENGINE_PICKER.md)
