# S02 — Language Selection

## Visual Reference

- Screenshot: không nằm trong demo set (kế thừa từ base FileRecovery)
- Figma: TODO

## Mục Đích

Cho user chọn ngôn ngữ app khi onboarding lần đầu. Hỗ trợ 11 ngôn ngữ (theo `strings.xml` base).

## Vị Trí Trong Navigation

- Route: `Routes.LANGUAGE`
- Vào từ: SPLASH (session 1) khi `isLanguageCompleted == false`
- Ra đến: INTRO
- Back behavior: tắt app (không cho back về Splash)

## Layout Breakdown

```
┌─────────────────────────────┐
│   Choose Language           │   <- Title L bold, padding top 24sdp
├─────────────────────────────┤
│  ┌───────────────────────┐  │
│  │ 🇬🇧 English      [●]  │  │   <- selected row: border purple
│  │ 🇻🇳 Tiếng Việt   [○]  │
│  │ 🇪🇸 Español      [○]  │
│  │ ... (11 items)        │
│  └───────────────────────┘  │
├─────────────────────────────┤
│  [PrimaryGradientButton]    │   <- "Continue"
├─────────────────────────────┤
│  [Native Ad full bottom]    │
└─────────────────────────────┘
```

## Languages (11)

| Code | Display Name (native) |
|------|----------------------|
| en | English |
| vi | Tiếng Việt |
| es | Español |
| pt | Português |
| de | Deutsch |
| ar | العربية |
| fr | Français |
| hi | हिन्दी |
| ha | Hausa |
| af | Afrikaans |
| zh | 中文 |

Mỗi item có cờ quốc gia (drawable `flag_<code>.xml`) hoặc emoji.

## States

| State | Display |
|-------|---------|
| First load | Auto select theo device locale, nếu không match → "English" |
| Selected | Row có border `colorPrimary`, radio filled |
| Continue tap | Set `isLanguageCompleted = true`, save `KEY_LANGUAGE`, `recreate()` Activity để apply locale, nav INTRO |

## ViewModel Contract

```kotlin
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val languageHelper: LanguageHelper,
) : ViewModel() {

    data class UiState(
        val languages: List<LanguageOption> = LanguageOption.ALL,
        val selectedCode: String = "en",
    )

    val uiState: StateFlow<UiState>

    fun onLanguageSelected(code: String)
    fun onContinue(activity: Activity)
}
```

## Resources

```xml
<string name="language_title">Choose Language</string>
<string name="language_continue_button">Continue</string>
<string name="language_native_english">English</string>
<!-- + 10 string khác cho các language native name -->
```

## Ads

- Native full bottom (`native_id_language`)

## Edge Cases & Accessibility

- RTL languages (Arabic, Hebrew): layout tự handle qua `LayoutDirection`
- Long name (vd "Português" wrap): row min height 48dp, ellipsis nếu cần
- contentDescription cho cờ: "X flag"
- Khi user back → app exit (không cho back về splash)
- Activity recreate khi đổi language → save scroll state qua `rememberSaveable`

## Acceptance Criteria

- [ ] 11 ngôn ngữ hiển thị
- [ ] Default selected = device locale hoặc en
- [ ] Tap Continue → recreate + chuyển INTRO
- [ ] App restart sau Continue: không quay lại Language screen
- [ ] RTL render đúng cho Arabic

## Liên Quan

- [S12_LANGUAGE_SETTINGS.md](S12_LANGUAGE_SETTINGS.md) — same screen accessed from Settings
