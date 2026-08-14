# Package Identity — Emoji Battery/Shimeji

Tài liệu này là nguồn chuẩn cho application identity sau lần đổi package ngày 2026-07-27.

## Canonical identity

| Thành phần | Giá trị canonical |
|---|---|
| App display name | `Emoji Battery` |
| Application ID | `com.asianmobile.emojibattery.shimeji` |
| App namespace | `com.asianmobile.emojibattery.shimeji` |
| Ads namespace | `com.asianmobile.emojibattery.shimeji.ads` |
| App source root | `app/src/main/java/com/asianmobile/emojibattery/shimeji/` |
| App test root | `app/src/test/java/com/asianmobile/emojibattery/shimeji/` |
| Ads source root | `ads/src/main/java/com/asianmobile/emojibattery/shimeji/` |
| Legacy debug catalog root | `/sdcard/Android/data/com.asianmobile.emojibattery.shimeji/files/pet_catalog/` |
| Production catalog cache | `files/pet_catalog/pets.json` under canonical app sandbox |
| Feedback email | `feedback@asianmobile.ltd` |
| Privacy policy | `https://sites.google.com/view/sanya-studio/home` |
| More apps | `https://play.google.com/store/apps/developer?id=Sanya.Studio` |

Mọi package declaration, import, fully qualified custom view, ProGuard rule, tool default và
tài liệu mới phải dùng identity canonical ở trên.

## Legacy identifier được giữ có chủ đích

Các tên sau chưa được đổi vì chúng không phải Android package name:

| Identifier | Vị trí | Ý nghĩa |
|---|---|---|
| `PrivateBrowser` | `settings.gradle.kts` | Tên Gradle root project nội bộ |
| `Theme.PrivateBrowser` | `themes.xml`, manifest | Tên Android style resource |
| `privatebrower-7168d` | `google-services.json` | Firebase project ID/storage bucket hiện tại |

Agent không được suy ra package từ các identifier này và không tự đổi chúng trong một task
không liên quan. Nếu owner muốn xóa toàn bộ legacy branding, phải thực hiện thành task riêng,
rà Gradle project name, style references, Firebase Console và release configuration.

## Firebase

`google-services.json` hiện đã có `package_name` mới để Gradle build được. Tuy nhiên
`mobilesdk_app_id` vẫn thuộc Android app registration cũ trong Firebase project hiện tại.

Trước release:

1. Đăng ký Android app `com.asianmobile.emojibattery.shimeji` trong Firebase Console.
2. Tải `google-services.json` chính thức mới.
3. Thay config trong module `:app`; rà lại file cùng tên trong `:ads`.
4. Xác minh Analytics, Crashlytics và Remote Config trên thiết bị thật.
5. Rà API-key restrictions, SHA fingerprints và Facebook/Adjust dashboard nếu chúng khóa theo package.

Mail app password là credential production của feedback flow. Giá trị này chỉ được cấu hình ở
Firebase Remote Config key `app_password_mail`; không ghi vào Android resource, default XML,
tài liệu hoặc Git history.

Không chỉnh tay `mobilesdk_app_id` hoặc API key để giả lập Firebase registration mới.

## Dữ liệu và cài đặt

Android xem package mới là một ứng dụng độc lập:

- DataStore/app-private files của package cũ không tự chuyển sang package mới.
- Catalog debug từng sync dưới `/Android/data/<package-cũ>/` không tự xuất hiện. Production
  catalog được fetch/cache lại từ private GitHub server trong sandbox package canonical.
- Hai package có thể từng tồn tại song song trên thiết bị cho đến khi app cũ được uninstall.

## Checklist cho agent

- Không tạo source mới dưới `com/asianmobile/privatebrower`.
- Không thêm import hoặc XML custom view dùng package cũ.
- Không dùng `PrivateBrowser` hoặc Firebase project ID làm namespace.
- Khi đổi package lần nữa, cập nhật source/test path, `:app`, `:ads`, XML, ProGuard, tools,
  Google Services, external data docs và toàn bộ agent documentation trong cùng commit.
- Tối thiểu chạy `./gradlew compileDebugKotlin`, `./gradlew testDebugUnitTest`,
  `git diff --check` và quét package cũ trước khi commit.
