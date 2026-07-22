# 03 — Tech Stack

`gradle/libs.versions.toml` và các `build.gradle.kts` là nguồn version chính xác. Tài liệu này mô tả vai trò, không thay thế version catalog.

| Nhóm | Công nghệ hiện tại | Vai trò |
|---|---|---|
| Language/build | Kotlin, KSP, Java 17 | Source và code generation |
| Android | compile/target SDK 36, min SDK 24 | Platform baseline |
| UI | Jetpack Compose, Material 3 | Declarative UI |
| App architecture | Single-Activity, MVVM | App shell và feature state |
| Navigation | Navigation Compose | Route graph/back stack |
| DI | Dagger Hilt | Dependency graph |
| Async/state | Coroutines, Flow | Async work và reactive state |
| Preferences | DataStore Preferences | Onboarding/settings state |
| Image | Coil Compose | Image loading khi cần |
| Animation | Lottie Compose | Splash/onboarding animation |
| Monetization | `:ads`, Google BillingClient | Ads và premium |
| Observability | Firebase Analytics/Crashlytics/Remote Config | Tracking/config/crash |
| Responsive sizing | Intuit SDP/SSP | Mapping design hiện tại |
| Feedback | Android Mail/Activation | Rate feedback email |
| Screen overlay | `WindowManager`, custom Canvas `View`, Choreographer | One-pet transparent overlay + 30 FPS clock |
| Long-running pet | Android foreground service `specialUse` | User-visible lifetime + ongoing notification |
| Pet packs | Platform ZIP/JSON/Bitmap APIs, JVM `org.json` test artifact | Secure import, validation, sprite preload/cache |

## Không còn trong app module

- Room database/compiler.
- AndroidX WebKit.
- OkHttp/network layer của browser.
- Coil Video decoder.
- Download/remux service dependencies.

Không thêm lại dependency chỉ vì từng tồn tại. Mọi dependency mới phải có call site thật, version catalog entry và lý do kiến trúc rõ.

## Verification

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
```
