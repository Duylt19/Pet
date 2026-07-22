# S05 — Set Default Browser (Onboarding)

## Visual Reference

- Screenshot: [docs/assets/screenshots/Screenshot_20260608-094929.png](../assets/screenshots/Screenshot_20260608-094929.png)
- Figma: TODO

## Mục Đích

Bước trước Permission, mời user đặt Private Browser làm default. Hỗ trợ cả "Set as default" và "Later".

## Vị Trí Trong Navigation

- Route: `Routes.SET_DEFAULT_BROWSER`
- Vào từ: INTRO/PREMIUM onboarding (khi app chưa là default và `IS_DEFAULT_BROWSER_PROMPTED == false`)
- Ra đến: PERMISSION (cả 2 button đều set `IS_DEFAULT_BROWSER_PROMPTED = true`)
- Back behavior: bị chặn trong onboarding

## Layout Breakdown (từ trên xuống)

```
┌─────────────────────────────────────┐
│                                     │  <- statusbar inset
│                                     │
│   Set as the default browser        │  <- Display 22ssp Bold, center
│   for a better experience!          │  <- Body L colors_808080, center
│                                     │
│   ─── Spacer 30sdp ───              │
│                                     │
│   ┌─┐                               │
│   │🎭 │  Private browsing            │  <- icon bg colors_E0E7FF, primary tint
│   └─┘  Browse in incognito mode to  │     Title M Bold + Body M secondary
│        ensure privacy and security. │
│                                     │
│   ─── 16sdp gap ───                 │
│                                     │
│   ┌─┐                               │
│   │⬇️ │  Fast downloading            │  <- icon bg colors_DCFCE7, green tint
│   └─┘  A faster and more stable     │
│        downloading experience.      │
│                                     │
│   ─── 16sdp gap ───                 │
│                                     │
│   ┌─┐                               │
│   │👍│  Easy to use                  │  <- icon bg colors_FFEDD5, orange tint
│   └─┘  Easy to get started and      │
│        simple to operate.           │
│                                     │
│   ─── flex spacer ───               │
│                                     │
│   ┌──────────────────────────────┐  │
│   │     Set as default           │  │  <- PrimaryGradientButton
│   └──────────────────────────────┘  │
│                                     │
│             Later                   │  <- SecondaryTextButton
│                                     │
│   [Native Ad bottom — small layout] │
└─────────────────────────────────────┘
```

**Specs từng phần:**

- Padding horizontal toàn screen: `_18sdp` (24dp)
- Header padding top: `_18sdp` (24dp safe area from status bar)
- Mỗi benefit row:
  - Icon container: 40sdp (~52dp Figma) circle, background `colors_E0E7FF`/`colors_DCFCE7`/`colors_FFEDD5`
  - Icon foreground: 22sdp (~28dp), tint tương ứng
  - Text bên phải: gap `_12sdp` từ icon
  - Title M Bold (`colors_000000`)
  - Description Body M (`colors_808080`), maxLines 2
- "Set as default" button: full width, height `_42sdp` (~56dp)
- "Later": chỉ text, gap `_9sdp` từ button
- Native ad: dưới cùng, full width, layout small

## States

| State | Display |
|-------|---------|
| Default chưa set | Header + 3 benefits + Set/Later button + Native ad |
| Đã là default | Screen này KHÔNG hiển thị (MainViewModel skip step) |
| Đang request OS dialog | Disabled buttons, không loading indicator (OS dialog tự overlay) |

## ViewModel Contract

```kotlin
@HiltViewModel
class SetDefaultBrowserViewModel @Inject constructor(
    private val defaultBrowserHelper: DefaultBrowserHelper,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _navigateEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateEvent: SharedFlow<Unit> = _navigateEvent.asSharedFlow()

    fun onSetDefaultClicked(launcher: ActivityResultLauncher<Intent>, context: Context)
    fun onRoleResult(granted: Boolean)
    fun onLaterClicked()
}
```

Composable:
```kotlin
@Composable
fun SetDefaultBrowserScreen(onCompleted: () -> Unit, viewModel: SetDefaultBrowserViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) {
        viewModel.onRoleResult(it.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(Unit) { viewModel.navigateEvent.collect { onCompleted() } }

    Column(...) { /* layout above */ }
}
```

Chi tiết implementation: [F08_SET_DEFAULT_BROWSER.md](../features/F08_SET_DEFAULT_BROWSER.md).

## Resources

```xml
<!-- strings -->
<string name="setdefault_header_title">Set as the default browser</string>
<string name="setdefault_header_subtitle">for a better experience!</string>
<string name="setdefault_benefit_private_title">Private browsing</string>
<string name="setdefault_benefit_private_desc">Browse in incognito mode to ensure privacy and security.</string>
<string name="setdefault_benefit_download_title">Fast downloading</string>
<string name="setdefault_benefit_download_desc">A faster and more stable downloading experience.</string>
<string name="setdefault_benefit_easy_title">Easy to use</string>
<string name="setdefault_benefit_easy_desc">Easy to get started and simple to operate.</string>
<string name="setdefault_set_button_label">Set as default</string>
<string name="setdefault_later_button_label">Later</string>
<string name="setdefault_fallback_toast">Open Settings > Apps > Default apps to set</string>

<!-- colors -->
<color name="colors_E0E7FF">#E0E7FF</color>
<color name="colors_DCFCE7">#DCFCE7</color>
<color name="colors_FFEDD5">#FFEDD5</color>
<color name="colors_22C55E">#22C55E</color>
<color name="colors_FB923C">#FB923C</color>

<!-- drawables -->
<!-- ic_set_default_mask.xml — icon mặt nạ -->
<!-- ic_download_arrow.xml -->
<!-- ic_thumbs_up.xml -->
```

## Ads

- Native bottom: ad unit `R.string.native_id_setdefault` (~120sdp height)

## Edge Cases & Accessibility

- API 29+: tap "Set as default" → `RoleManager.createRequestRoleIntent(ROLE_BROWSER)`
- API 24-28: mở Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
- API < 24: mở app details settings
- OEM khoá role (Samsung, Huawei) → catch ActivityNotFoundException → toast hướng dẫn
- User huỷ OS dialog → vẫn nav Home
- contentDescription icon, button
- Min touch target 48dp cho cả 2 button

## Acceptance Criteria

- [ ] Layout pixel-perfect với screenshot #1
- [ ] 3 benefit row đúng icon + bg color
- [ ] Tap "Set as default" → OS dialog hiện (hoặc fallback)
- [ ] Tap "Later" → nav Home, set prompted=true
- [ ] Tap back hardware → ko làm gì hoặc tắt app (chọn behavior)
- [ ] Restart app sau prompted: không thấy screen này nữa
- [ ] Native ad load, layout không jump khi load

## Liên Quan

- [F08_SET_DEFAULT_BROWSER.md](../features/F08_SET_DEFAULT_BROWSER.md)
- [S04_PERMISSION.md](S04_PERMISSION.md) — previous
- [S06_HOME_CONTAINER.md](S06_HOME_CONTAINER.md) — next
