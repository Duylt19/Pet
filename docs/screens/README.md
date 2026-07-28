# Current Screens

| Route | Package/screen | Contract hiện tại |
|---|---|---|
| `splash` | `ui/splash/SplashScreen` | Startup/ads/billing animation và điều hướng bước tiếp theo |
| `language` | `ui/language/LanguageScreen` | Chọn language trong onboarding |
| `language_settings` | `ui/language/LanguageScreen` | Chọn language từ Settings rồi restart app shell |
| `intro` | `ui/intro/IntroScreen` | Pager ba trang, có native ads theo config |
| `permission` | `ui/permission/PermissionScreen` | Overlay special access + notification permission, có Continue/Skip |
| `home` | `ui/home/HomeScreen` | Cozy pet room, thumbnail đội pet, permission-aware Start/Stop và entry Catalog/Settings/Premium |
| `pet_catalog/{slotIndex}` | `ui/catalog/PetCatalogScreen` | Lưới 2 cột GitHub raw/cache, 1.026 pet, 268 categories, authenticated thumbnail và download/verify/Set đúng slot |
| `pet_detail/{slotIndex}/{packKey}` | `ui/catalog/PetDetailScreen` | Hero preview, metadata thân thiện và select action đúng slot |
| `settings` | `ui/home/settings/SettingsScreen` | My Pet Family roster, commit-on-selection Add flow và app/support |
| `pet_customization/{slotIndex}` | `ui/home/settings/PetCustomizationScreen` | Hồ sơ cute-pet độc lập: character, size, speed, touch, speech, custom messages, position và remove |
| `premium/{startByIndex}` | `ui/premium/PremiumScreen` | Subscription UI, close behavior theo entry source |

## Visual scope hiện tại

- Product screens từ Home trở đi dùng light cozy palette, rounded cards, thumbnail pet thật
  và purple/coral accents để tạo cảm giác companion thay vì utility dashboard.
- Splash, Language/Language Settings, Intro, Permission và Premium giữ nguyên visual hiện
  tại theo quyết định owner; chúng sẽ được update trong các task riêng.

## Screen implementation contract

- Screen mới có `Screen`, `ViewModel`, `UiState` trừ trường hợp presentational page rất nhỏ đã được owner chấp thuận.
- Screen nhận navigation callback, không tự sở hữu NavController.
- Phải biểu diễn loading/content/empty/error nếu data feature có các trạng thái đó.
- User-facing resources, analytics và accessibility là một phần acceptance criteria.
- Khi screen thay đổi route/entry/exit/back behavior, cập nhật file này và `../04_NAVIGATION_FLOW.md`.
