# 03 — Tech Stack

## 1. Tổng Quan Stack

| Layer | Library | Phiên bản (từ `libs.versions.toml`) | Mục đích |
|-------|---------|-------------------------------------|----------|
| Language | Kotlin | 2.2.0 | Ngôn ngữ chính, 100% Kotlin |
| Build | AGP (Android Gradle Plugin) | 8.7.3 | Build system |
| UI | Jetpack Compose Material3 | BoM 2024.12.01 | Declarative UI |
| Navigation | navigation-compose | 2.8.5 | Routing |
| DI | Hilt | 2.57.1 | Dependency injection |
| KSP | kotlin-ksp | 2.3.3 | Symbol processing (Hilt, Room) |
| Async | Coroutines + Flow | (built-in) | Reactive async |
| DB | Room (runtime, ktx, compiler) | 2.7.1 | SQLite ORM |
| Prefs | androidx.datastore:datastore-preferences | 1.0.0 | Key-value storage |
| Network | Retrofit + OkHttp (+ logging) | 2.11.0 / 4.12.0 | HTTP (optional) |
| Image | Coil compose + coil-video | 2.7.0 | Load favicon, thumbnail |
| Image | Glide | 5.0.5 | Image loading (legacy) |
| Anim | Lottie + lottie-compose | 6.6.2 | Splash, empty states |
| Player | Media3 ExoPlayer + media3-ui + media3-hls | 1.5.1 | Preview video (v2) |
| Sizing | sdp + ssp (intuit) | (latest) | Responsive dimensions |
| WebView | androidx.webkit | 1.12.1 | Modern WebView APIs |
| Lifecycle | lifecycle-viewmodel-compose | 2.10.0 | ViewModel + Compose |
| Firebase | config + analytics + crashlytics | BoM 34.3.0 | RC + analytics |
| Billing | com.android.billingclient:billing-ktx | 8.0.0 | Premium IAP |
| Ads | GMA, AppLovin, etc. | 0.24.0-beta01 / 13.5.1 | Ad mediation |

---

## 2. Thư Viện Đã Thêm (từ M1 Foundation)

Hai dependencies sau đã được thêm vào `libs.versions.toml` khi triển khai M1:

### a) `androidx.webkit:webkit` ✅

**Lý do:** WebView cũ không expose Safe Browsing, dark mode, force-dark, cookie management API hiện đại.

```toml
# gradle/libs.versions.toml
[versions]
webkit = "1.12.1"

[libraries]
androidx-webkit = { group = "androidx.webkit", name = "webkit", version.ref = "webkit" }
```

### b) `androidx.activity:activity-ktx` cho RoleManager ✅

Đã có sẵn thông qua compose dependencies.

---

## 3. Bộ Thư Viện Đã Có (Highlight)

### UI

```kotlin
// Compose BoM
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.navigation.compose)
```

### DI

```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.androidx.hilt.navigation.compose)
```

### Database

```kotlin
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
```

### Sizing (sdp/ssp)

```kotlin
implementation(libs.intuit.sdp)
implementation(libs.intuit.ssp)
```

Cách dùng:
```kotlin
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

Modifier.padding(dimensionResource(SdpR.dimen._12sdp))
Text(fontSize = dimensionResource(SspR.dimen._14ssp).value.sp)
```

**Lưu ý:** sdp/ssp chỉ có sẵn 1..100 và giá trị bội số. Nếu cần `_13sdp` mà thư viện không có → dùng giá trị gần nhất (`_12sdp` hoặc `_14sdp`).

---

## 4. Convention Khi Thêm Lib Mới

1. Thêm version vào `[versions]` trong `libs.versions.toml`
2. Thêm alias vào `[libraries]`
3. Tham chiếu trong `app/build.gradle.kts` qua `libs.xxx`
4. Update `docs/03_TECH_STACK.md` (file này) — thêm dòng vào bảng + giải thích lý do
5. Run `./gradlew compileDebugKotlin` để verify

---

## 5. ProGuard / R8 Rules

Khi thêm lib có annotation/reflection (Retrofit, Gson, Room) — verify rules trong `app/proguard-rules.pro`:

- Room: tự generated, không cần rule
- Gson + data class: cần `-keepclasseswithmembers class * { @com.google.gson.annotations.SerializedName <fields>; }` hoặc dùng `@Keep`
- WebView JS interface: `@JavascriptInterface` + `-keep`
- Hilt: tự handle

---

## 6. Versioning & Release

- **versionCode**: integer, tăng monotonic mỗi release (vd 119 = 1.1.9)
- **versionName**: semver `MAJOR.MINOR.PATCH`
- Khi release: bump cả 2 trong `app/build.gradle.kts`

---

## 7. CI / Local Build Commands

```bash
# Verify compile nhanh (15s)
./gradlew compileDebugKotlin

# Verify với ProGuard
./gradlew compileReleaseKotlin

# Build APK debug
./gradlew assembleDebug

# Build APK release (signed)
./gradlew assembleRelease

# Run lint
./gradlew lint

# Test unit
./gradlew testDebugUnitTest
```

**KHÔNG** dùng `assembleRelease/Debug` để check syntax — quá chậm. Xem [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md).
