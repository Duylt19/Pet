# Current Screens

| Route | Package/screen | Contract hiện tại |
|---|---|---|
| `splash` | `ui/splash/SplashScreen` | Startup/ads/billing animation và điều hướng bước tiếp theo |
| `language` | `ui/language/LanguageScreen` | Chọn language trong onboarding |
| `language_settings` | `ui/language/LanguageScreen` | Chọn language từ Settings rồi restart app shell |
| `intro` | `ui/intro/IntroScreen` | Pager ba trang, có native ads theo config |
| `permission` | `ui/permission/PermissionScreen` | Permission step có Continue/Skip |
| `home` | `ui/home/HomeScreen` | Placeholder: chỉ hai nút Settings/Premium |
| `settings` | `ui/home/settings/SettingsScreen` | Base settings actions, language/search/clear/rate/feedback |
| `premium/{startByIndex}` | `ui/premium/PremiumScreen` | Subscription UI, close behavior theo entry source |

## Screen implementation contract

- Screen mới có `Screen`, `ViewModel`, `UiState` trừ trường hợp presentational page rất nhỏ đã được owner chấp thuận.
- Screen nhận navigation callback, không tự sở hữu NavController.
- Phải biểu diễn loading/content/empty/error nếu data feature có các trạng thái đó.
- User-facing resources, analytics và accessibility là một phần acceptance criteria.
- Khi screen thay đổi route/entry/exit/back behavior, cập nhật file này và `../04_NAVIGATION_FLOW.md`.
