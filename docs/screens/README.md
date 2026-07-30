# Current Screens

| Route | Package/screen | Contract hiện tại |
|---|---|---|
| `splash` | `ui/splash/SplashScreen` | Startup/ads/billing animation và điều hướng bước tiếp theo |
| `language` | `ui/language/LanguageScreen` | Chọn language trong onboarding |
| `language_settings` | `ui/language/LanguageScreen` | Chọn language từ Settings rồi restart app shell |
| `intro` | `ui/intro/IntroScreen` | Pager ba trang, có native ads theo config |
| `permission` | `ui/permission/PermissionScreen` | Overlay special access + notification permission, có Continue/Skip |
| `home` | `ui/home/HomeScreen` | Mixed/Swarm loại trừ nhau, 12 Mixed slot, global enable, visibility từng pet, Rewarded/Premium |
| `pet_catalog/{target}/{slotIndex}` | `ui/catalog/PetCatalogScreen` | Lưới GitHub raw/cache; Mixed slot 4–12 có Rewarded gate tuần tự, Premium bypass |
| `pet_detail/{target}/{slotIndex}/{packKey}` | `ui/catalog/PetDetailScreen` | Hero preview và select action đúng mode/slot |
| `settings` | `ui/home/settings/SettingsScreen` | My Pet Family roster, commit-on-selection Add flow và app/support |
| `pet_customization/{slotIndex}` | `ui/home/settings/PetCustomizationScreen` | Hồ sơ cute-pet độc lập: character, size, speed, touch, speech, custom messages, position và remove |
| `swarm_customization` | `ui/home/swarm/SwarmCustomizationScreen` | Edit Swarm riêng: character, count, size/speed, random variation và movement insets |
| `battery_catalog` | `ui/battery/catalog/BatteryCatalogScreen` | Search/category/theme/favorite/Premium; disclosure + Accessibility gate trước khi mở editor |
| `battery_editor/{themeId}` | `ui/battery/editor/BatteryEditorScreen` | Theme khởi tạo cặp pet+pin; picker category đổi hai asset độc lập, Rewarded/Premium theo component, live preview trên status bar; editor component dùng chung draft, Apply cố định và disclosure |
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
