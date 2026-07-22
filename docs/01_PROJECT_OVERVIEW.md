# 01 — Project Overview

## Identity hiện tại

| Thuộc tính | Giá trị |
|---|---|
| Display name | Cute Pet |
| Vai trò repository | Android base project cho sản phẩm mới |
| Namespace/application ID | `com.asianmobile.privatebrower` (legacy, đổi sau) |
| UI | Jetpack Compose Material 3 |
| Architecture | Single-Activity + MVVM + Hilt + Flow |

Cute Pet chưa có domain feature chính thức. Không suy luận sản phẩm từ tên app và không tự thêm pet care, game, browser hoặc media feature nếu chưa có requirement/design.

## Mục tiêu của base

- Tái sử dụng app shell và flow onboarding ổn định.
- Giữ cách tổ chức feature, state, DI, navigation, resource, ads và billing.
- Cho phép thay domain layer mà không viết lại infrastructure chung.
- Giảm tối đa class/dependency từ sản phẩm cũ gây hiểu sai hoặc tăng maintenance.

## Những gì đang được giữ

- Splash và startup orchestration.
- Language onboarding + language settings.
- Intro/onboarding pages.
- Permission step để cập nhật theo nhu cầu sản phẩm mới.
- Home placeholder với Settings/Premium.
- Settings, search engine picker, clear browsing data mẫu và feedback/rating.
- Premium/billing, ads, analytics, remote config, theme và reusable components.

## Những gì đã bị xóa

- Browser engine/WebView feature.
- Normal/private tab manager.
- Bookmark/history UI và Room persistence.
- Download manager, video sniffing/remuxing và foreground service.
- File/media tabs và viewer.
- Room database/schema, network modules và dependencies chỉ phục vụ feature cũ.

## Quyết định deferred

- Package/application ID và root Gradle project name chưa đổi.
- UI/branding resource cũ ngoài `app_name` có thể được thay bằng design Cute Pet sau.
- Permission policy phải được đánh giá lại khi domain feature được chốt.
- Search engine/clear browsing helper có thể bị thay hoặc xóa khi Settings mới được thiết kế.
