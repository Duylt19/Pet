# Cute Pet — Android Shimeji App

Cute Pet là ứng dụng Android pet animation chạy nổi trên màn hình theo mô hình Shimeji.
Project kế thừa app shell ổn định gồm onboarding, quảng cáo, billing, localization,
settings và các quy ước kiến trúc hiện có.

> Trạng thái nguồn chuẩn: tài liệu phải khớp với code hiện tại. Không xem lịch sử Private Browser là chức năng còn tồn tại.

## Trạng thái hiện tại

- App display name: `Cute Pet`.
- Namespace/application ID: `com.asianmobile.emojibattery.shimeji`.
- Root Gradle project name hiện vẫn là `PrivateBrowser` (không ảnh hưởng package cài đặt).
- Flow tạm thời: Splash → Language → Intro → Discover Home; bước Permission onboarding đang
  bị tắt bằng policy nhưng toàn bộ route/class vẫn được giữ để bật lại.
- Permission giải thích/request overlay special access và notification permission; user vẫn có
  thể Skip khi bước này được bật lại.
- Discover Home tổng hợp Emoji Battery, pet/battery catalog và 4-tab navigation. Discover chỉ
  giữ toggle Emoji Battery; pet nổi được quản lý trong My Pet Room/Shimeji Pets.
- My Pet Room là phòng in-app: pet đã sở hữu đi lại trong phòng, sheet ba tab My Pet/Food/Room
  quản lý roster, cho ăn và đổi background. Chọn pet nào hiện trên màn hình bằng toggle
  `Pet on screen` trong panel chi tiết từng pet.
- Shimeji Pets tải 1.062 owner pet (gồm 36 pack WC 2026
  với 864 frame gốc)
  từ private GitHub
  static server, đọc cache trước và revalidate theo TTL 24 giờ + ETag/rate-limit backoff,
  tải/verify ZIP theo SHA-256 khi user bấm Set và vẫn hỗ trợ import pack `.zip` schema v1
  được validate an toàn.
- Mine là hub app/support; pet đã sở hữu và trạng thái hiện trên màn hình được quản lý trong
  My Pet Room, không còn flow Catalog/Detail/Customize màu xanh cũ.
- Battery tab mở catalog/editor và một Accessibility status-cover overlay opt-in.
  Catalog 898 theme, 38 nền, 100 emotion và 26 animation tải từ cùng private GitHub static
  server với Pet; JSON cache/revalidate và asset được tải, verify SHA-256 theo nhu cầu.
  Debug vẫn giữ packaged snapshot fallback, còn release chỉ nhận catalog `APPROVED`.
- Discover Home, My Pet Room, Shimeji Pets và Splash/App Open Welcome Back dùng pink/white
  Figma direction; Language/Intro/Premium giữ visual hiện tại. Permission giữ nguyên source/UI
  nhưng hiện không nằm trong flow onboarding.
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
├── navigation/                  # AppNavGraph + nested HomeNavGraph + route policies
├── pet/
│   ├── engine/                 # Pure Kotlin state machine/timeline/geometry
│   ├── overlay/                # FGS, WindowManager adapter, Canvas/sprite pet view
│   └── pack/                   # Pack schema, secure installer, repository/cache
├── battery/
│   ├── overlay/                # Accessibility status-cover window/renderer
│   └── settings/               # Battery config sanitization
├── ui/
│   ├── app/                     # MainViewModel/app-level presentation
│   ├── onboarding/              # splash → language → intro → permission
│   ├── home/                    # Home shell/chrome + Discover tab
│   ├── battery/                 # catalog → favourite/recent → editor
│   ├── pet/                     # Shimeji Pets + My Pet Room
│   ├── settings/                # Mine + permission management
│   ├── search/
│   ├── premium/
│   └── shared/                  # component/theme thật sự dùng cross-feature
└── utils/
```

Bản đồ package và route chi tiết: [UI structure](docs/UI_STRUCTURE.md).

## Thêm feature mới

1. Chọn domain trước, sau đó tạo
   `ui/<domain>/<feature>/<Feature>Screen.kt`, `<Feature>ViewModel.kt`, `<Feature>UiState.kt`.
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
