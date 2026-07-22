# S09 — Settings

## Visual Reference

- Screenshot: [Screenshot_20260608-095125.png](../assets/screenshots/Screenshot_20260608-095125.png)

## Mục Đích

Trung tâm setting của app: 2 section (General + Other Settings) với 7 entry + footer brand/version.

## Vị Trí Trong Navigation

- Route: `Routes.SETTINGS`
- Vào từ: Home drawer (Settings) hoặc từ BrowserWebView menu
- Ra đến:
  - Set As Default Browser → OS dialog
  - Search engine row → SearchEnginePicker bottom sheet (inline)
  - Clear History → AlertDialog inline
  - Language → LANGUAGE_SETTINGS
  - Send Feedback → mailto intent
  - Share app → ACTION_SEND
  - Privacy Policy → PRIVACY_POLICY
- Back behavior: pop về Home

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ←  Settings                        │  <- top bar
├─────────────────────────────────────┤
│  General                            │  <- section header
│  ┌────────────────────────────────┐ │
│  │ 🎭 Set As Default Browser   > │ │
│  │ ─────────                      │ │  <- divider
│  │ G  Google                   > │ │  <- show current engine
│  │ ─────────                      │ │
│  │ 🗑 Clear History            > │ │
│  └────────────────────────────────┘ │
├─────────────────────────────────────┤
│  Other Settings                     │
│  ┌────────────────────────────────┐ │
│  │ 🌐 Language                 > │ │
│  │ ─────────                      │ │
│  │ 💬 Send Feedback            > │ │
│  │ ─────────                      │ │
│  │ 📤 Share app                > │ │
│  │ ─────────                      │ │
│  │ ⚠️ Privacy Policy           > │ │
│  └────────────────────────────────┘ │
│                                     │
│  ─── flex spacer ───                │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ 🎭  Private Browser:          │  │  <- footer
│  │     Safe & Secure   Version 1.1.9│
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Specs:**

- Top bar: AppHeaderBar back + "Settings" title (left-aligned theo screenshot — không center)
- Section header: Title M `colors_808080` padding bottom `_4sdp` padding horizontal `_18sdp`
- Section card:
  - Background white
  - Border `colors_EEEEEE` 1dp
  - Radius `_9sdp`
  - Padding inner horizontal `_12sdp`
  - Margin horizontal `_18sdp`
- SettingsRow:
  - Height `_36sdp` (~48dp)
  - Icon size `_18sdp` (~24dp)
  - Icon spacing right `_12sdp`
  - Title Body L
  - Trailing chevron `_12sdp` `colors_B8B8B8`
  - Hoặc trailing text (search engine display name)
- Divider giữa rows: `colors_EEEEEE` 1dp, padding horizontal `_18sdp`
- Footer:
  - Padding bottom `_18sdp`
  - App icon 36sdp
  - Title "Private Browser: Safe & Secure" Body L Bold
  - Version "Version 1.1.9" Caption `colors_808080`
  - Layout: Row icon + Column(title + version)

## States

| State | Display |
|-------|---------|
| Default | Render 2 section + footer |
| Search engine row | Trailing text = current engine name |
| Set As Default row | Trailing: chevron HOẶC text "Already default" nếu là default |
| Clear history dialog | AlertDialog show |
| Search engine picker | ModalBottomSheet show |

## ViewModel Contract

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val searchEngineRepository: SearchEngineRepository,
    private val defaultBrowserHelper: DefaultBrowserHelper,
    private val clearBrowsingDataUseCase: ClearBrowsingDataUseCase,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    data class UiState(
        val currentSearchEngine: SearchEngine = SearchEngine.GOOGLE,
        val isDefaultBrowser: Boolean = false,
        val showSearchEngineSheet: Boolean = false,
        val showClearHistoryDialog: Boolean = false,
        val versionName: String = BuildConfig.VERSION_NAME,
    )

    val uiState: StateFlow<UiState>

    fun onSetDefaultClicked(context: Context, launcher: ActivityResultLauncher<Intent>)
    fun onSearchEngineClicked()
    fun onSearchEngineSelected(engine: SearchEngine)
    fun onDismissSearchEngineSheet()
    fun onClearHistoryClicked()
    fun onConfirmClearHistory(includeBookmarks: Boolean)
    fun onDismissClearHistoryDialog()
    fun onLanguageClicked(navController: NavController)
    fun onFeedbackClicked(context: Context)
    fun onShareClicked(context: Context)
    fun onPrivacyPolicyClicked(navController: NavController)
}
```

## Resources

```xml
<string name="settings_title">Settings</string>
<string name="settings_section_general">General</string>
<string name="settings_section_other">Other Settings</string>
<string name="settings_set_default_title">Set As Default Browser</string>
<string name="settings_already_default">Already default</string>
<string name="settings_search_engine_title">Search Engine</string>
<string name="settings_clear_history_title">Clear History</string>
<string name="settings_language_title">Language</string>
<string name="settings_feedback_title">Send Feedback</string>
<string name="settings_share_title">Share app</string>
<string name="settings_privacy_title">Privacy Policy</string>
<string name="settings_app_name">Private Browser: Safe &amp; Secure</string>
<string name="settings_version_label">Version %1$s</string>
```

Icons:
- `ic_set_default_mask`, `ic_google_g`, `ic_trash`
- `ic_lang_globe`, `ic_chat_bubble`, `ic_share`, `ic_shield_warning`

## Ads

- KHÔNG banner trên Settings (theo screenshot)
- Interstitial khi back về Home (`navigateWithAd` ngược)
- Hoặc khi navigate from Settings sang sub-screen (LanguageSettings, PrivacyPolicy): `navigateWithAd`

## Edge Cases & Accessibility

- Set As Default đã là default → row vẫn hiển thị nhưng trailing text "Already default", tap không làm gì
- Search engine icon (Google G) multi-color → tint Color.Unspecified
- Clear history confirm dialog destructive → "Clear" button color red
- Feedback intent không có app → toast "No email app installed"
- contentDescription cho mọi icon row
- Footer version dynamic từ BuildConfig

## Acceptance Criteria

- [ ] Layout match screenshot #7
- [ ] 7 row đầy đủ
- [ ] Search engine row hiển thị current engine name
- [ ] Set As Default trigger OS dialog
- [ ] Clear History → confirm → clear thực sự
- [ ] Language → nav LanguageSettings
- [ ] Feedback → mailto
- [ ] Share → system share
- [ ] Privacy Policy → nav PrivacyPolicy
- [ ] Footer version đúng

## Liên Quan

- [F07_SEARCH_ENGINE.md](../features/F07_SEARCH_ENGINE.md)
- [F08_SET_DEFAULT_BROWSER.md](../features/F08_SET_DEFAULT_BROWSER.md)
- [F09_CLEAR_HISTORY.md](../features/F09_CLEAR_HISTORY.md)
- [F11_SHARE_FEEDBACK.md](../features/F11_SHARE_FEEDBACK.md)
- [S11_SEARCH_ENGINE_PICKER.md](S11_SEARCH_ENGINE_PICKER.md)
- [S12_LANGUAGE_SETTINGS.md](S12_LANGUAGE_SETTINGS.md)
- [S14_PRIVACY_POLICY.md](S14_PRIVACY_POLICY.md)
