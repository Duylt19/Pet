# Cute Pet Base — Agent Context

Đây là base project Android cho sản phẩm mới, không còn là ứng dụng Private Browser hoàn chỉnh. Tên package legacy `com.asianmobile.privatebrower` được giữ tạm thời và chỉ đổi khi owner yêu cầu.

## Nguồn hướng dẫn

Đọc theo thứ tự:

1. `.agents/AGENTS.md`
2. `.agents/skills/android_developer/SKILL.md`
3. `docs/README.md`
4. Các file foundation liên quan trong `docs/`

Nếu tài liệu và source khác nhau, source hiện tại là bằng chứng thực thi; agent phải cập nhật tài liệu trong cùng thay đổi để khôi phục tính nhất quán.

## Base được giữ lại

- Single-Activity, Compose, MVVM, Hilt, Flow.
- Splash, Language, Intro, Permission, Home, Settings, Premium.
- Home placeholder có hai nút Settings/Premium.
- DataStore onboarding/language/permission/search engine.
- Ads module, billing, analytics, remote config và localization.
- Search engine picker, clear browsing data tối giản và feedback/rating trong Settings để làm mẫu tích hợp.

## Không còn tồn tại

Không giả định hoặc tham chiếu như source hiện hành tới BrowserEngine, TabManager, WebView browser screen, bookmarks/history database, download service, media/file tabs, Room schema hoặc foreground service cũ.

Muốn thêm lại một capability tương tự phải xem đó là feature mới: thiết kế contract, dependency, permission, manifest, test và docs từ đầu.

## Quy tắc phát triển

- Mỗi screen mới dùng bộ ba `Screen`/`ViewModel`/`UiState`.
- UI nhận state và callback; navigation nằm ở NavGraph, nghiệp vụ nằm ở ViewModel/use case.
- Data access qua repository interface; implementation đặt dưới `data/repository/impl`.
- String/color/spacing dùng Android resources; UI từ Figma quy đổi px ÷ 1.3 sang sdp/ssp theo guideline.
- Cập nhật routes, analytics, tests và Markdown khi thay đổi flow/architecture.
- Không lưu token, API key hoặc credential thật trong repository.
- Sau thay đổi chạy compile + unit test và tạo commit tiếng Anh rõ nghĩa.
