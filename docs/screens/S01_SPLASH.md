# S01 — Splash

## Visual Reference

- Screenshot: chưa có (không nằm trong demo set). Tham khảo splash project gốc FileRecovery
- Figma node: TODO

## Mục Đích

Màn hình khởi động: hiển thị brand identity + chạy init tasks (ads consent, billing check, remote config, language load) trước khi điều hướng tới screen phù hợp.

## Vị Trí Trong Navigation

- Route: `Routes.SPLASH`
- Vào từ: app cold start (launcher) hoặc Intent.ACTION_VIEW deep link
- Ra đến: tuỳ `MainViewModel.getNextScreen()` — `LANGUAGE` / `INTRO` / `PERMISSION` / `SET_DEFAULT_BROWSER` / `HOME` / `PREMIUM` (session 2+ với premium splash return)
- Back behavior: tắt app

## Layout Breakdown (top → bottom)

```
┌─────────────────────────────┐
│                             │
│                             │
│      [Logo App center]      │   <- 100sdp x 100sdp, ic_set_default_mask
│                             │
│      Private Browser        │   <- Title L bold colors_000000
│      Safe & Secure          │   <- Body M colors_808080
│                             │
│      [Loading dots]         │   <- CircularProgressIndicator hoặc Lottie
│                             │
│                             │
└─────────────────────────────┘
```

- Background: white (`colors_FFFFFF`) hoặc gradient nhẹ purple
- Logo: center vertical
- Dưới logo: tên app + tagline
- Bottom: progress indicator (small) + version text (Caption `colors_B8B8B8`)

## States

| State | Hiển thị |
|-------|---------|
| Loading | Logo + spinner |
| Success → navigate | Fade out 200ms |
| Init fail (RC timeout 5s) | Tiếp tục với default config |

## ViewModel Contract

```kotlin
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val mainViewModel: MainViewModel,
    private val preferencesRepository: PreferencesRepository,
    private val billingHelper: BillingHelper,
    private val adsManager: AdsManager,
    private val remoteConfig: SafeRemoteConfig,
) : ViewModel() {

    val navigateEvent: SharedFlow<String>  // emit route to navigate
    fun initialize(hasPermission: Boolean)
}
```

Trong `initialize`:
1. Increment session count
2. Init ads consent (UMP form nếu cần)
3. Wait remote config (timeout 5s)
4. Check billing → set isPremium
5. Preload InterstitialUtil
6. Call `mainViewModel.getNextScreen()` → emit navigate event theo các cờ onboarding trong DataStore

## Resources Cần Thêm

```xml
<!-- strings -->
<string name="splash_app_name">Private Browser</string>
<string name="splash_tagline">Safe &amp; Secure</string>
<string name="splash_loading_text">Loading...</string>

<!-- colors -->
<color name="colors_FFFFFF">#FFFFFF</color>

<!-- drawables -->
<!-- ic_set_default_mask.xml (app logo) -->

<!-- lottie -->
<!-- res/raw/splash_loading.json (optional) -->
```

## Ads

- **App Open Ads**: show overlay khi splash kết thúc (session 1 skip, session 2+ show)
- **Interstitial preload**: tải sẵn ở splash, dùng cho transitions sau
- **No banner/native**: splash không render ads inline

## Edge Cases & Accessibility

- App locale chưa được set: dùng device locale
- Remote config timeout 5s → fallback defaults, vẫn navigate
- Billing check fail → coi như non-premium
- contentDescription cho logo: "Private Browser logo"
- Min display time: 1.5s (tránh chớp nhanh)
- Max display time: 7s (force navigate)

## Acceptance Criteria

- [ ] Layout center, logo + tagline + spinner
- [ ] Init tasks hoàn tất < 5s
- [ ] Navigate đúng theo `getNextScreen`
- [ ] Không crash khi không có internet
- [ ] Back button không hoạt động (block back)
- [ ] APK launch → splash hiện < 1s

## Liên Quan

- [MainViewModel logic](../02_ARCHITECTURE.md)
- Session count: [05_DATA_MODEL.md](../05_DATA_MODEL.md)
