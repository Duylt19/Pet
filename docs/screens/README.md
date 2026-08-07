# Current Screens

| Route | Package/screen | Contract hiện tại |
|---|---|---|
| `splash` | `ui/splash/SplashScreen` | Startup/ads/billing animation và điều hướng bước tiếp theo |
| `language` | `ui/language/LanguageScreen` | Chọn language trong onboarding |
| `language_settings` | `ui/language/LanguageScreen` | Chọn language từ Settings rồi restart app shell |
| `intro` | `ui/intro/IntroScreen` | Pager ba trang, có native ads theo config |
| `permission` | `ui/permission/PermissionScreen` | Overlay special access + notification permission, có Continue/Skip |
| `home` | `ui/discover/DiscoverScreen` | Tab Discover: Emoji Battery toggle, floating-pet toggle (overlay special access + notification gate), quick actions và pet/battery previews; bottom chrome do Home shell sở hữu |
| `search` | `ui/search/SearchScreen` | Figma Search: input/chip tìm kiếm, banner, lưới battery theme và native ad cố định cuối màn |
| `my_pet` | `ui/petroom/PetRoomScreen` | My Pet Room: scene phòng full-screen, top bar biển gỗ + music, shortcut Pet Store, sheet ba tab My Pet/Food/Room. Tab My Pet liệt kê pet đã sở hữu (pack đã cài) kèm ô `+` mở Pet Store; tab Room chọn background từ catalog và persist; tab Food thuộc phase inventory |
| `pet_catalog/{target}/{slotIndex}` | `ui/catalog/PetCatalogScreen` | Lưới GitHub raw/cache; Mixed slot 4–12 có Rewarded gate tuần tự, Premium bypass |
| `pet_store` | `ui/petstore/PetStoreScreen` | Tab Pet Store: shared Home header/switch, pet/food tabs, reward sheet, verified download, Lottie unlock reveal chạy special movement clip của pack, name/toast; food persistence TODO sau My Pet |
| `pet_detail/{target}/{slotIndex}/{packKey}` | `ui/catalog/PetDetailScreen` | Hero preview và select action đúng mode/slot |
| `settings` | `ui/home/settings/SettingsScreen` | Tab Mine theo Figma: shared Home chrome, battery toggle, shortcuts, General và Other actions |
| `pet_customization/{slotIndex}` | `ui/home/settings/PetCustomizationScreen` | Hồ sơ cute-pet độc lập: character, size, speed, touch, speech, custom messages, position và remove |
| `swarm_customization` | `ui/home/swarm/SwarmCustomizationScreen` | Edit Swarm riêng: character, count, size/speed, random variation và movement insets |
| `battery_catalog` | `ui/battery/catalog/BatteryCatalogScreen` | Search/category/theme/favorite/Premium; disclosure + Accessibility gate trước khi mở editor |
| `battery_editor/{themeId}` | `ui/battery/editor/BatteryEditorScreen` | Overview khởi tạo cặp pet+pin; picker category đổi hai asset độc lập, Rewarded/Premium theo component, live preview, Apply cố định và disclosure |
| `battery_editor_component/{themeId}/{page}` | `ui/battery/editor/BatteryEditorScreen` | Destination editor riêng theo component, dùng ViewModel/draft của overview; Back/Done phục hồi đúng vị trí cuộn và có screen tracking độc lập |
| `premium/{startByIndex}` | `ui/premium/PremiumScreen` | Subscription UI, close behavior theo entry source |

## Visual scope hiện tại

- Discover và Mine dùng wallpaper trắng với gradient pastel, primary pink `#FB3675`, Roboto,
  horizontal catalog rows và rounded cards theo Figma node `8015:1035`. My Pet và các màn
  product cũ tiếp tục dùng cozy palette hiện tại cho tới task refresh riêng.
- Splash, Language/Language Settings, Intro, Permission và Premium giữ nguyên visual hiện
  tại theo quyết định owner; chúng sẽ được update trong các task riêng.

## Screen implementation contract

- Screen mới có `Screen`, `ViewModel`, `UiState` trừ trường hợp presentational page rất nhỏ đã được owner chấp thuận.
- Screen nhận navigation callback, không tự sở hữu NavController.
- Phải biểu diễn loading/content/empty/error nếu data feature có các trạng thái đó.
- User-facing resources, analytics và accessibility là một phần acceptance criteria.
- Khi screen thay đổi route/entry/exit/back behavior, cập nhật file này và `../04_NAVIGATION_FLOW.md`.
