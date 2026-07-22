# 07 — Ads Integration

Quy tắc và vị trí tất cả ad types trong app.

---

## 1. Ad Types Sẵn Có (Module `:ads`)

| Type | Composable / Util | File |
|------|-------------------|------|
| Banner | `BannerAdComposable` | `ads/ui/compose/BannerAdComposable.kt` |
| Native (full) | `NativeAdComposable` | `ads/ui/compose/NativeAdComposable.kt` |
| Native (intro full) | `NativeIntroFullAdComposable` | `ads/ui/compose/NativeIntroFullAdComposable.kt` |
| Interstitial | `InterstitialUtil.getInstance()` | `ads/ui/interstitial/InterstitialUtil.kt` |
| App Open | `AppOpenManager` | `ads/ui/openads/AppOpenManager.kt` |
| Rewarded | `RewardedVideoAds` | `ads/ui/rewarded/RewardedVideoAds.kt` |

---

## 2. Ad Placement Matrix

Matrix này phân biệt rõ placement có ads và screen có chủ đích **không ads**. Không chèn
ads vào mọi screen một cách máy móc nếu làm gián đoạn browsing, playback, purchase hoặc legal.

| Screen/content đang visible | Banner | Native | Interstitial | OpenAd |
|---|---|---|---|---|
| Splash | bottom | - | - | không show khi route Splash |
| Language onboarding/settings | - | bottom | transition theo frequency cap | cho phép |
| Intro page 1/page 3 | - | bottom | khi rời onboarding | cho phép |
| Intro page 2 | - | - | - | cho phép |
| Permission | - | bottom | khi continue/skip | cho phép |
| Set default browser | - | bottom | khi continue | cho phép |
| Home Browser | Home sticky | card sau private info | theo navigation action | cho phép |
| Tabs Normal/Private/Search | Home sticky | - | theo navigation action | cho phép |
| Downloads All/Active/Completed | Home sticky | item đầu list | theo navigation action | cho phép |
| Bookmarks/History trong Home | Home sticky | sau bookmark thứ 2 | theo navigation action | cho phép |
| Files Home | Home sticky | sau category grid | theo navigation action | cho phép |
| Bookmarks/History standalone | bottom | sau bookmark thứ 2 | enter/browser action | cho phép |
| Photo/Video/Audio/Documents list | bottom | inline trong list/grid | enter/back | cho phép |
| Settings | - | bottom | enter/back/sub-screen | cho phép |
| Exit dialog | - | trong dialog | - | - |
| How to download | bottom | - | enter | cho phép |
| Browser WebView | - | - | transition theo rule | **OFF** |
| Media viewer/PiP | - | - | - | **OFF** |
| Premium | - | - | - | **OFF** |
| Privacy policy | - | - | - | **OFF** |
| Search engine sheet/dialog/popup | - | - | - | kế thừa route cha |

---

## 3. Nguồn Dữ Liệu Placement

Native ads chỉ được khai báo trong `NativeAdPlacementCatalog`. Mỗi entry gồm bốn thành phần:

1. `screenCode`: identity ổn định dùng cho ViewModel key và analytics placement.
2. `adType`: layout native cần render.
3. `remoteConfigKey`: kill switch riêng cho placement.
4. `adUnitResId`: resource production riêng cho placement.

Các placement Favorite, Playlist, Channel List, Add Playlist và Player Search thuộc base LiveTV
đã bị loại bỏ. Không khôi phục các key/ID này nếu app không có destination Private Browser tương ứng.

Không thêm `when(screenCode)` mới ở Composable. Catalog là mapping duy nhất.

## 4. Ad Unit ID Convention

ID khai báo trong `strings.xml` để swap dễ giữa debug/release:

```xml
<!-- res/values/strings.xml -->
<string name="banner_id_home" translatable="false">ca-app-pub-...</string>
<string name="native_id_language" translatable="false">ca-app-pub-...</string>
<string name="native_id_setdefault" translatable="false">ca-app-pub-...</string>
<string name="native_id_files" translatable="false">ca-app-pub-...</string>
<string name="native_id_home_card" translatable="false">ca-app-pub-...</string>
<string name="interstitial_id_main" translatable="false">ca-app-pub-...</string>
<string name="open_ad_id" translatable="false">ca-app-pub-...</string>
```

`res/values/strings.xml` chứa test IDs, `res/values/strings.xml` flavor release chứa production. Setup qua build flavors hoặc Firebase RC.

---

## 5. Cách Sử Dụng

### 5.1. Banner (Sticky bottom Home)

```kotlin
BannerAd(
    modifier = Modifier.fillMaxWidth(),
    adPosition = BANNER_HOME_BOTTOM
)
```

Đặt trong Scaffold `bottomBar = { Column { BottomNavBar(); BannerAd(...) } }`.

### 5.2. Native

```kotlin
NativeAdInternal(
    screenCode = SCREEN_SET_DEFAULT,
    modifier = Modifier.fillMaxWidth()
)
```

`layout_native_ad_*.xml` đã có sẵn trong `:ads` resources.

### 5.3. Interstitial (với navigateWithAd helper)

```kotlin
navigateWithAd(context, Routes.SETTINGS) {
    navController.safeNavigate(Routes.SETTINGS, ignoreDebounce = true)
}
```

Helper internally:
```kotlin
fun navigateWithAd(context: Context, placement: String, onNavigate: () -> Unit) {
    val activity = context.findActivity() ?: return onNavigate()
    InterstitialUtil.getInstance().openAd?.needShowOpenAds = false
    InterstitialUtil.getInstance().showInterstitialAd(activity, placement) {
        onNavigate()
    }
}
```

### 5.4. App Open Ads

`AppOpenManager` start trong `BaseApplication.onCreate()`. Auto-show khi app return foreground, **trừ khi**:
- Đang show interstitial (`needShowOpenAds = false`)
- Đang ở Premium screen
- Đang ở BrowserWebView (tránh ngắt user đang đọc page)
- User là premium

---

## 6. Thêm Một Ads Mới Cần Những Gì

### Native placement

1. Tạo ad unit Native trên AdMob. Trong lúc chờ ID riêng có thể tạm dùng chung unit, nhưng vẫn
   phải tạo resource name riêng cho placement.
2. Thêm `SCREEN_*` và `IS_SHOW_NATIVE_*` trong `ads/config/Constant.kt`.
3. Thêm production ID trong `ads/src/main/res/values/strings.xml`.
4. Thêm remote-config default trong `ads/src/main/res/xml/remote_config_defaults.xml`.
5. Thêm **một** entry vào `NativeAdPlacementCatalog` với screen code, ad type, RC key và ID.
6. Đặt `NativeAdInternal(screenCode = SCREEN_*)` tại vị trí UI đã được design phê duyệt.
7. Thêm screen code vào `NativeAdPlacementCatalogTest`, chạy test và compile.
8. Tạo/publish cùng key trên Firebase Remote Config để có thể tắt placement mà không release app.

### Banner placement

1. Tạo một `BANNER_*` code ổn định trong `Constant.kt`.
2. Truyền code qua `BannerAd(adPosition = BANNER_*)`; không dùng chung chuỗi `bottom` cho mọi nơi.
3. Nếu cần ad unit hoặc kill switch riêng, mở rộng banner catalog trước khi đặt UI.
4. Kiểm tra banner fail thì collapse, premium thì không load và không còn khoảng trống.

### Interstitial/App Open/Rewarded

1. Không gọi SDK trực tiếp từ UI; dùng `navigateWithAd`, `InterstitialUtil`, `AppOpenManager`
   hoặc `RewardedVideoAds`.
2. Truyền placement/destination ổn định để event click có `ad_placement` và `ad_format`.
3. Xác nhận frequency cap, consent, premium và danh sách route cấm trước khi enable.

## 7. Quy Tắc Bắt Buộc

1. ✅ **Mỗi lần show Interstitial** → set `needShowOpenAds = false`. `MainActivity.onStart` re-enable.
2. ✅ **Premium user**: tất cả ads ẩn. Check `BillingHelper.isPremium` từ Singleton hoặc Repository.
3. ✅ **Native fail**: composable hide container, không reserve space trống. Banner fail: collapse height = 0.
4. ✅ **Frequency cap** cho Interstitial: minimum 30s giữa 2 lần show (đã handle trong `InterstitialUtil`).
5. ❌ **KHÔNG** show Interstitial khi user vừa thực hiện thao tác phá huỷ (delete bookmark, clear history) — gây cảm giác "ăn cắp" hành động.
6. ❌ **KHÔNG** show OpenAd khi đang download (progress active) — gây ngắt download.
7. ❌ **KHÔNG** show ads trên SearchEnginePicker bottom sheet (UX gọn).

---

## 8. Premium Gating

App có Premium IAP. Khi user upgrade:

```kotlin
// In all Composable that show ads:
val isPremium by billingHelper.isPremium.collectAsStateWithLifecycle()
if (!isPremium) {
    BannerAd(...)
}
```

Hoặc wrap thành composable `AdContainer { ad content }` tự check premium.

---

## 9. Consent (GDPR/CCPA)

Init trong Splash (đã có sẵn `MainActivity` / Splash logic). UMP SDK của Google. Form hiện 1 lần đầu hoặc khi user vào EU region.

---

## 10. Test IDs

Trong build debug:
```
Banner: ca-app-pub-3940256099942544/6300978111
Interstitial: ca-app-pub-3940256099942544/1033173712
Rewarded: ca-app-pub-3940256099942544/5224354917
Native Advanced: ca-app-pub-3940256099942544/2247696110
App Open: ca-app-pub-3940256099942544/3419835294
```

---

## 11. Tracking

Click ads gui event `admob_ad_click` kem hai parameter on dinh:

- `ad_placement`: screen code/banner position/destination.
- `ad_format`: `native`, `native_item`, `banner`, `interstitial`, `app_open`, `rewarded`.

Paid impression/revenue gui qua Adjust trong `Tracking.kt`; impression cua Google Ads van do SDK
ghi nhan. Khong tao event name dong theo placement vi se lam vo schema dashboard.

---

## 12. Ad Loading Strategy

| Type | Khi nào load | Khi nào show |
|------|--------------|--------------|
| Splash OpenAd | App start | Hiển thị overlay Splash |
| Interstitial main | Splash load done → preload | Khi `navigateWithAd` |
| Banner | Khi composable hiện | Auto-refresh interval 30-60s |
| Native | Khi composable hiện (lazy) | Sau load done |
| OpenAd (return) | Background | Khi app return foreground |

---

## 13. Edge Cases

1. **No internet**: ads silent fail, composable hide. Không show error.
2. **Slow network**: Interstitial timeout 5s → skip ad, navigate luôn.
3. **User huỷ Premium**: `isPremium` flow emit false → ads tự hiện lại.
4. **Activity recreate (Language change)**: ad instances re-create từ đầu, không leak.

---

## 14. Compliance

- App phải có Privacy Policy URL (xem S14)
- Yêu cầu user đồng ý license (đã có flow `IS_LICENSE_AGREED`)
- Trong setting có link mở Privacy Policy
- Khi user > 13 tuổi (theo COPPA / Children's Online Privacy Protection Act): app **không** target children → khai báo trong AdMob console & Play Console
