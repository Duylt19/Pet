# S14 — Privacy Policy

## Visual Reference

- Không có screenshot riêng — chỉ là WebView fullscreen với URL Privacy Policy

## Mục Đích

Hiển thị nội dung Privacy Policy của app trong WebView nội bộ (không mở Chrome external).

## Vị Trí Trong Navigation

- Route: `Routes.PRIVACY_POLICY?url=<encoded>`
- Vào từ: Settings > Privacy Policy
- Ra: back về Settings

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ←  Privacy Policy                  │  <- top bar back + title
├─────────────────────────────────────┤
│  ▰▰▰▰▰▰▰▰▰▱  (progress khi load)   │
├─────────────────────────────────────┤
│                                     │
│       [WebView fullscreen]          │
│       Load Privacy Policy URL       │
│                                     │
└─────────────────────────────────────┘
```

**Specs:**

- Top bar: AppHeaderBar back arrow + "Privacy Policy" title
- LinearProgressIndicator dưới top bar (chỉ khi loading)
- WebView fill rest
- KHÔNG có URL bar (user không edit)
- KHÔNG có menu (no add bookmark, share)

## States

| State | Display |
|-------|---------|
| Loading | Progress bar + WebView blank |
| Loaded | WebView render content |
| Error no internet | Error page (default WebView) hoặc custom "Cannot load Privacy Policy" |
| Empty URL | Show fallback hardcoded URL |

## ViewModel Contract

```kotlin
@HiltViewModel
class PrivacyPolicyViewModel @Inject constructor(
    private val remoteConfig: SafeRemoteConfig,
) : ViewModel() {

    data class UiState(
        val url: String = "",
        val progress: Int = 0,
        val isLoading: Boolean = false,
    )

    val uiState: StateFlow<UiState>

    fun loadUrl(urlFromNav: String)
    fun onProgressChanged(progress: Int)
}
```

`loadUrl`:
- Nếu `urlFromNav` rỗng → load `remoteConfig.getString("privacy_policy_url").ifBlank { "https://privatebrowser.example.com/privacy" }` (TODO confirm URL chính thức)

## Resources

```xml
<string name="privacy_policy_title">Privacy Policy</string>
<string name="privacy_policy_fallback_url" translatable="false">https://privatebrowser.example.com/privacy</string>
<string name="privacy_policy_error_message">Unable to load Privacy Policy. Please check your internet connection.</string>
```

## Ads

- KHÔNG ads (trang legal)
- OpenAd OFF
- Interstitial KHÔNG

## Edge Cases & Accessibility

- URL không reachable → WebView render error page
- Click link bên trong privacy policy → mở link đó trong cùng WebView (hoặc Chrome Custom Tabs nếu là external)
- Back press: pop về Settings
- WebView JS enabled (nội dung tĩnh không cần nhưng vẫn enable)
- contentDescription cho back button

## Acceptance Criteria

- [ ] URL load đúng từ Remote Config hoặc fallback
- [ ] Progress bar hiển thị khi loading
- [ ] Back về Settings
- [ ] No ads
- [ ] WebView không có URL bar

## Liên Quan

- [F01_BROWSER_CORE.md](../features/F01_BROWSER_CORE.md) — reuse engine
- [F11_SHARE_FEEDBACK.md](../features/F11_SHARE_FEEDBACK.md) — trigger từ Settings
- [S09_SETTINGS.md](S09_SETTINGS.md)
