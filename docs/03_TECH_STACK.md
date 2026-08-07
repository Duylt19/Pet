# 03 — Tech Stack

`gradle/libs.versions.toml` và các `build.gradle.kts` là nguồn version chính xác. Tài liệu này mô tả vai trò, không thay thế version catalog.

| Nhóm | Công nghệ hiện tại | Vai trò |
|---|---|---|
| Language/build | Kotlin, KSP, Java 17 | Source và code generation |
| Android | compile/target SDK 36, min SDK 24 | Platform baseline |
| UI | Jetpack Compose, Material 3 | Declarative UI |
| UI screenshot test | Compose Preview Screenshot Testing | Host-side Layoutlib golden test, không cần emulator |
| App architecture | Single-Activity, MVVM | App shell và feature state |
| Navigation | Navigation Compose | Route graph/back stack |
| DI | Dagger Hilt | Dependency graph |
| Async/state | Coroutines, Flow | Async work và reactive state |
| Preferences | DataStore Preferences | Onboarding/settings state |
| Image | Coil Compose + Coil GIF decoders | Image loading và animated GIF resource trên API 24+ |
| Pet catalog network | `HttpURLConnection`, Coil OkHttp transport | Private GitHub raw JSON/ZIP/thumbnail với token Remote Config |
| Room catalog network | `HttpURLConnection` | `json/rooms.json` + background/thumbnail từ cùng private server; room `1` đóng gói sẵn trong APK làm fallback offline |
| Animation | Lottie Compose | Splash/onboarding animation |
| Monetization | `:ads`, Google BillingClient | Ads và premium |
| Observability | Firebase Analytics/Crashlytics/Remote Config | Tracking/config/crash |
| Responsive sizing | Intuit SDP/SSP | Mapping design hiện tại |
| Feedback | Android Mail/Activation | Rate feedback email |
| Screen overlay | `WindowManager`, custom Canvas `View`, Choreographer | Mixed 1–12 hoặc Swarm 1–12 windows + shared adaptive FPS clock |
| Battery status cover | `AccessibilityService`, `TYPE_ACCESSIBILITY_OVERLAY`, custom Canvas `View` | Opt-in non-touchable status capsule; no node retrieval/automation |
| Long-running pet | Android foreground service `specialUse` | User-visible lifetime + ongoing notification |
| Pet packs | Platform ZIP/JSON/Bitmap APIs, JVM `org.json` test artifact | Secure import, validation, sprite preload/cache |

## Không còn trong app module

- Room database/compiler.
- AndroidX WebKit.
- OkHttp/network layer của browser. Coil vẫn sở hữu transport ảnh; pet catalog dùng
  client giới hạn riêng, không phục hồi browser networking.
- Coil Video decoder.
- Download/remux service dependencies.

Không thêm lại dependency chỉ vì từng tồn tại. Mọi dependency mới phải có call site thật, version catalog entry và lý do kiến trúc rõ.

## Verification

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew updateDebugScreenshotTest
./gradlew validateDebugScreenshotTest
```

Screenshot test nằm trong `app/src/screenshotTest`; chạy host-side bằng một Gradle worker để
giữ tải máy thấp. Reference image chỉ cập nhật khi UI mới đã được đối chiếu với Figma.
Riêng lúc chạy screenshot task, `adquality-sdk` được loại khỏi render classpath vì metadata
`R` của SDK không tương thích Layoutlib; dependency này vẫn giữ nguyên trong build app bình thường.

## Firebase sau khi đổi package

Hai file `google-services.json` đã được đồng bộ package để build nhận diện
`com.asianmobile.emojibattery.shimeji`. Trước khi phát hành, owner phải đăng ký Android app
với package mới trong Firebase Console và thay các file này bằng config được Firebase tạo chính
thức; không dùng lâu dài `mobilesdk_app_id` của app registration cũ.

Firebase Remote Config production phải khai báo `github_token_pet_server`. Default trong
source luôn rỗng; token cần quyền read-only Contents cho đúng private server repository và
phải rotate ngoài source code.

Xem thêm [PACKAGE_IDENTITY.md](PACKAGE_IDENTITY.md) để phân biệt package canonical với
Firebase project ID và các resource name legacy còn được giữ.
