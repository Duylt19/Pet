# Cute Pet — Android Shimeji App

Cute Pet là ứng dụng Android pet animation chạy nổi trên màn hình theo mô hình Shimeji.
Project kế thừa app shell ổn định gồm onboarding, quảng cáo, billing, localization,
settings và các quy ước kiến trúc hiện có.

> Trạng thái nguồn chuẩn: tài liệu phải khớp với code hiện tại. Không xem lịch sử Private Browser là chức năng còn tồn tại.

## Trạng thái hiện tại

- App display name: `Cute Pet`.
- Namespace/application ID: `com.asianmobile.emojibattery.shimeji`.
- Root Gradle project name hiện vẫn là `PrivateBrowser` (không ảnh hưởng package cài đặt).
- Flow: Splash → Language → Intro → Permission → Home.
- Permission giải thích/request overlay special access và notification permission; user vẫn có thể Skip.
- Home điều khiển Start/Stop Mixed 1–12 pet hoặc Swarm 1–12 bản sao, đồng thời mở Catalog,
  Settings và Premium. Ba slot Mixed đầu miễn phí; slot 4–12 mở khóa tuần tự bằng Rewarded.
- Mỗi slot pet chọn được character riêng; Catalog tải 1.026 owner pet từ private GitHub
  static server, đọc cache trước và revalidate theo TTL 24 giờ + ETag/rate-limit backoff,
  tải/verify ZIP theo SHA-256 khi user bấm Set và vẫn hỗ trợ import pack `.zip` schema v1
  được validate an toàn.
- Settings là pet roster + app/support; mỗi slot mở một hồ sơ Customize Pet riêng cho
  character, size, speed, touch, speech, custom messages và position.
- Product UI từ Home trở đi dùng cozy light design: pet room, discovery grid, friendly
  detail, My Pet Family và per-pet profile; onboarding/Premium giữ visual hiện tại.
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
Thông tin package canonical và các tên legacy còn giữ: [docs/PACKAGE_IDENTITY.md](docs/PACKAGE_IDENTITY.md).

## Source tree hiện tại

```text
app/src/main/java/com/asianmobile/emojibattery/shimeji/
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
│   ├── overlay/                # FGS, WindowManager adapter, Canvas/sprite pet view
│   └── pack/                   # Pack schema, secure installer, repository/cache
├── ui/
│   ├── component/
│   ├── splash/
│   ├── language/
│   ├── intro/
│   ├── permission/
│   ├── home/
│   ├── catalog/
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
- [Package identity](docs/PACKAGE_IDENTITY.md)
- [Architecture contract](docs/02_ARCHITECTURE.md)
- [Navigation flow](docs/04_NAVIGATION_FLOW.md)
- [Agent coding guidelines](docs/08_AGENT_CODING_GUIDELINES.md)
- [Current progress](IMPLEMENTATION_PROGRESS.md)
