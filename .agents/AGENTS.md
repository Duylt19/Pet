# Project Rules — Emoji Battery/Shimeji Android App

## Trạng thái bắt buộc phải hiểu

- Đây là ứng dụng Emoji Battery/Shimeji đang phát triển, không phải codebase Private Browser.
- App name hiện tại là `Emoji Battery: Cute Pet`.
- Package/application ID canonical: `com.asianmobile.emojibattery.shimeji`.
- `PrivateBrowser` trong `rootProject.name`, `Theme.PrivateBrowser` và Firebase project ID
  `privatebrower-7168d` là legacy identifier còn giữ có chủ đích; chúng không phải package name.
- Đọc `docs/PACKAGE_IDENTITY.md` trước khi sửa app identity, Firebase hoặc app-specific storage.
- Flow hiện tại: Splash → Language → Intro → Permission → Home; Home Start/Stop Mixed/Swarm tối đa 12 pet và mở Catalog/Settings/Premium.
- Không tham chiếu các class/module browser, search/clear-browsing, broad storage, download, media, Room hoặc service cũ vì chúng đã bị xóa.
- Permission request overlay special access/notification; pet chạy bằng `specialUse` foreground service và small overlay window.

## Cách làm việc

- Giao tiếp với owner bằng Tiếng Việt; code và commit message bằng English.
- Trước khi sửa, đọc `.agents/skills/android_developer/SKILL.md` và docs liên quan.
- Dùng `rg` để tìm source, `apply_patch` để sửa file.
- Giữ Single-Activity + MVVM + Hilt + Flow và package-by-feature.
- Khi thay đổi source tree, navigation, dependency, flow hoặc convention, cập nhật Markdown trong cùng commit.
- Verify bằng `./gradlew compileDebugKotlin` và `./gradlew testDebugUnitTest`; không assemble nếu không cần APK.
- Tự tạo commit sau khi hoàn tất.

## Figma

- Không có Figma file key cố định cho sản phẩm mới.
- Lấy file key/node ID từ URL do owner cung cấp cho từng task.
- Token chỉ lấy từ biến môi trường `FIGMA_ACCESS_TOKEN`; không ghi token vào `.md`, source, command output hoặc Git history.
- Khi token/file/design thiếu hoặc không hợp lệ, dừng và yêu cầu owner cung cấp dữ liệu đúng.

## Source of truth

- Package identity: `docs/PACKAGE_IDENTITY.md`
- Architecture: `docs/02_ARCHITECTURE.md`
- Navigation: `docs/04_NAVIGATION_FLOW.md`
- Coding rules: `.agents/skills/android_developer/SKILL.md`
- Current screens: `docs/screens/README.md`
- Current capabilities: `docs/features/README.md`
