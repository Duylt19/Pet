# Cute Pet — Android Base Project

Cute Pet là base Android đang được chuyển thành ứng dụng pet animation chạy nổi trên màn hình. Base giữ flow onboarding, quảng cáo, billing, localization, settings và các quy ước kiến trúc hiện có.

> Trạng thái nguồn chuẩn: tài liệu phải khớp với code hiện tại. Không xem lịch sử Private Browser là chức năng còn tồn tại.

## Trạng thái hiện tại

- App display name: `Cute Pet`.
- Namespace/application ID tạm giữ: `com.asianmobile.privatebrower`.
- Root Gradle project name tạm giữ: `PrivateBrowser`.
- Flow: Splash → Language → Intro → Permission → Home.
- Permission giải thích/request overlay special access và notification permission; user vẫn có thể Skip.
- Home điều khiển Start/Stop demo pet, đồng thời giữ lối vào Settings và Premium.
- Browser, search engine, clear browsing data, storage permission, download, media, Room và service cũ đã bị xóa.

## Kiến trúc bắt buộc

- Kotlin, Jetpack Compose Material 3.
- Single-Activity với Navigation Compose.
- MVVM theo feature: `Screen` + `ViewModel` + `UiState`.
- Hilt cho dependency injection.
- Coroutines/Flow cho state và tác vụ bất đồng bộ.
- Repository interface/implementation cho nguồn dữ liệu; use case cho nghiệp vụ liên feature hoặc có thể kiểm thử độc lập.
- DataStore cho preference nhỏ. Chỉ thêm Room/network/service khi feature thực sự cần.
- Module `:ads` tiếp tục tách riêng khỏi `:app`.

Chi tiết contract: [docs/02_ARCHITECTURE.md](docs/02_ARCHITECTURE.md) và [android_developer/SKILL.md](.agents/skills/android_developer/SKILL.md).

## Source tree hiện tại

```text
app/src/main/java/com/asianmobile/privatebrower/
├── BaseApplication.kt
├── MainActivity.kt
├── constant/
├── data/
│   ├── local/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── di/
├── navigation/
├── pet/
│   ├── engine/                 # Pure Kotlin state machine/timeline/geometry
│   └── overlay/                # FGS, WindowManager adapter, Canvas pet view
├── ui/
│   ├── component/
│   ├── splash/
│   ├── language/
│   ├── intro/
│   ├── permission/
│   ├── home/
│   ├── premium/
│   ├── main/
│   └── theme/
└── utils/
```

## Thêm feature mới

1. Tạo `ui/<feature>/<Feature>Screen.kt`, `<Feature>ViewModel.kt`, `<Feature>UiState.kt`.
2. Định nghĩa model/repository/use case trong `data/` nếu feature có dữ liệu hoặc nghiệp vụ riêng.
3. Bind/provide dependency trong `di/`; không khởi tạo repository/service trực tiếp trong Composable.
4. Thêm route vào `Routes` và destination vào `AppNavGraph`.
5. Thêm string/color/dimension/drawable qua resource, không hardcode trong UI.
6. Thêm analytics screen name và test phù hợp.
7. Cập nhật tài liệu trong cùng commit.
8. Chạy `./gradlew compileDebugKotlin` và `./gradlew testDebugUnitTest`.

## Build nhanh

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
```

Không dùng `assembleDebug`/`assembleRelease` chỉ để kiểm tra compile.

## Tài liệu

- [Documentation index](docs/README.md)
- [Base architecture](docs/02_ARCHITECTURE.md)
- [Navigation flow](docs/04_NAVIGATION_FLOW.md)
- [Agent coding guidelines](docs/08_AGENT_CODING_GUIDELINES.md)
- [Current progress](IMPLEMENTATION_PROGRESS.md)
