# S12 — Language Settings

## Visual Reference

- Tương tự [S02_LANGUAGE.md](S02_LANGUAGE.md) nhưng có back arrow + tiêu đề "Language"

## Mục Đích

Cho user đổi ngôn ngữ app trong khi đã onboarded. Lưu DataStore + recreate Activity để apply.

## Vị Trí Trong Navigation

- Route: `Routes.LANGUAGE_SETTINGS`
- Vào từ: Settings > Language row
- Ra: back về Settings; sau Apply → recreate Activity → back to Settings hoặc Home tuỳ flow

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ←  Language                        │  <- top bar back + title
├─────────────────────────────────────┤
│  ┌────────────────────────────────┐ │
│  │ 🇬🇧 English             [●]   │ │  <- selected
│  │ 🇻🇳 Tiếng Việt          [○]   │
│  │ 🇪🇸 Español             [○]   │
│  │ ... (11 items)                 │
│  └────────────────────────────────┘ │
├─────────────────────────────────────┤
│  [Apply - PrimaryGradientButton]    │  <- chỉ enabled khi đổi khác current
├─────────────────────────────────────┤
│  [Sticky banner ad]                 │
└─────────────────────────────────────┘
```

## States

| State | Display |
|-------|---------|
| Đang chọn = current | Apply button disabled |
| Đang chọn khác current | Apply button enabled |
| Tap Apply | Save → recreate Activity |

## ViewModel Contract

Reuse `LanguageViewModel` từ S02 hoặc tách `LanguageSettingsViewModel`:

```kotlin
@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val languageHelper: LanguageHelper,
) : ViewModel() {

    data class UiState(
        val languages: List<LanguageOption> = LanguageOption.ALL,
        val currentCode: String = "en",
        val selectedCode: String = "en",
    )

    val uiState: StateFlow<UiState>
    fun onLanguageSelected(code: String)
    fun onApply(activity: Activity)
}
```

`onApply`:
1. Save `KEY_LANGUAGE = selected`
2. `languageHelper.applyLocale(activity, selected)`
3. `activity.recreate()`

## Resources

Reuse strings của S02 + thêm:
```xml
<string name="language_settings_title">Language</string>
<string name="language_settings_apply_button">Apply</string>
```

## Ads

- Sticky banner inherit hoặc dedicated `R.string.banner_id_language_settings`
- Interstitial khi từ Settings vào (navigateWithAd)

## Edge Cases & Accessibility

- Khi recreate → restore route LANGUAGE_SETTINGS (nếu không pop) hoặc về Settings/Home
- RTL languages handle layout direction
- contentDescription cho cờ + radio
- Apply button disabled khi không đổi → tránh recreate vô ích

## Acceptance Criteria

- [ ] Layout tương tự S02 với back arrow + title
- [ ] Apply button enabled khi đổi
- [ ] Tap Apply → recreate apply locale
- [ ] Sau recreate, app reflect language mới
- [ ] Back về Settings không mất state

## Liên Quan

- [S02_LANGUAGE.md](S02_LANGUAGE.md)
- [S09_SETTINGS.md](S09_SETTINGS.md)
