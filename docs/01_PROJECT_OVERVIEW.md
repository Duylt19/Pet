# 01 — Project Overview

## Identity hiện tại

| Thuộc tính | Giá trị |
|---|---|
| Display name | Cute Pet |
| Vai trò repository | Ứng dụng Android pet overlay/Shimeji |
| Namespace/application ID | `com.asianmobile.emojibattery.shimeji` |
| UI | Jetpack Compose Material 3 |
| Architecture | Single-Activity + MVVM + Hilt + Flow |

Cute Pet có domain chính thức: một hoặc nhiều pet animation có thể hiển thị và tương tác
trên các ứng dụng khác bằng Android overlay. Mixed hỗ trợ tối đa 3 pet khác nhau; Swarm
hỗ trợ tối đa 12 bản sao cùng pet (6 trên low-RAM) theo performance budget.

## Kiến trúc kế thừa

- Tái sử dụng app shell và flow onboarding ổn định.
- Giữ cách tổ chức feature, state, DI, navigation, resource, ads và billing.
- Cho phép mở rộng domain pet mà không viết lại infrastructure chung.
- Giảm tối đa class/dependency từ sản phẩm cũ gây hiểu sai hoặc tăng maintenance.

## Những gì đang được giữ

- Splash và startup orchestration.
- Language onboarding + language settings.
- Intro/onboarding pages.
- Permission UX cho overlay special access và notification permission, có Skip.
- Home điều khiển hai mode loại trừ nhau: Mixed 1–3 pet với visibility riêng và Swarm
  1–12 bản sao, cùng Start/Stop/Catalog/Settings/Premium.
- Settings quản lý roster 1–3 pet; mỗi slot có màn Customize Pet riêng cho
  character/size/speed/position/interaction/speech. Language/share/rate/feedback/version
  nằm ở khu vực app-wide, không trộn với pet profile.
- Premium/billing, ads, analytics, remote config, theme và reusable components.

## Những gì đã bị xóa

- Browser engine/WebView feature.
- Normal/private tab manager.
- Bookmark/history UI và Room persistence.
- Download manager, video sniffing/remuxing và foreground service.
- File/media tabs và viewer.
- Room database/schema, network modules và dependencies chỉ phục vụ feature cũ.
- Search engine preference, clear browsing data và broad storage permission.

## Quyết định deferred

- Root Gradle project name vẫn là `PrivateBrowser`; đây chỉ là tên build nội bộ và không ảnh hưởng package cài đặt.
- UI visual final, icon và pet asset production chờ design/asset chính thức; vertical slice chức năng đã hoạt động.
- Overlay permission, foreground-service notification và policy disclosure phải được triển khai/test trước release.
- Không tự khởi động pet sau boot trong MVP; chỉ bổ sung khi user chủ động bật trong Settings.
