# Current Screens

| Route | Package/screen | Contract hiện tại |
|---|---|---|
| `splash` | `ui/splash/SplashScreen` | Figma pastel startup, billing/consent/banner và điều hướng bước tiếp theo |
| `language` | `ui/language/LanguageScreen` | Chọn language trong onboarding |
| `language_settings` | `ui/language/LanguageScreen` | Chọn language từ Settings rồi restart app shell |
| `intro` | `ui/intro/IntroScreen` | Pager ba trang, có native ads theo config |
| `permission` | `ui/permission/PermissionScreen` | Overlay special access + notification permission, có Continue/Skip |
| `home` | `ui/discover/DiscoverScreen` | Tab Discover: Emoji Battery toggle, Battery Troll hero và pet/battery previews; không còn pet switch/quick-action frame; bottom chrome do Home shell sở hữu |
| `search` | `ui/search/SearchScreen` | Figma Search: input/chip tìm kiếm, banner, lưới battery theme và native ad cố định cuối màn |
| `favourite_recent` | `ui/favoriterecent/FavouriteRecentScreen` | Favourite battery theme thật, Recent empty pending MRU contract, header/tab/grid và native ad theo Figma |
| `my_pet` | `ui/petroom/PetRoomScreen` | My Pet Room: scene phòng full-screen, top bar biển gỗ + music, shortcut Pet Store, sheet ba tab My Pet/Food/Room. Tab My Pet liệt kê pet đã sở hữu (pack đã cài) kèm ô `+` mở Pet Store; chạm một pet mở panel chi tiết (tên, breed, ngày nhận nuôi, toggle Pet on screen, thanh Energy); tab Food tiêu một phần ăn để hồi Energy và nút `+` quay về Pet Store; tab Room chọn background từ catalog và persist. Xoá pet bằng nút X trên card kèm dialog xác nhận; pet bị xoá mất luôn slot overlay. Pet nổi tạm tắt khi màn này mở |
| `grant_permissions` | `ui/permission/GrantPermissionsScreen` | Ba nhóm quyền theo Figma: accessibility (badge Required/Allowed + Go to Settings), overlay và ignore battery optimization, notification. Mọi mục chỉ mở system surface tương ứng và đọc lại trạng thái ở `ON_RESUME`; battery optimization dùng `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` thay vì quyền `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` bị Play hạn chế, và row đó chỉ hiện khi exemption thật sự có tác dụng (xem `PET_OVERLAY.md` cho đủ sáu tín hiệu); ROM có allowlist riêng của hãng thì có thêm row Allow auto-start dẫn tới đúng màn đó. Mọi intent đều mở qua `openSettings()` với fallback, vì màn hệ thống có thể không tồn tại trên ROM rút gọn và activity vendor resolve được vẫn có thể không `exported` |
| `pet_catalog/{target}/{slotIndex}` | `ui/catalog/PetCatalogScreen` | Lưới GitHub raw/cache; Mixed slot 4–12 có Rewarded gate tuần tự, Premium bypass |
| `pet_store` | `ui/petstore/PetStoreScreen` | Tab Pet Store: shared Home header/switch, pet/food tabs, reward sheet, verified download, Lottie unlock reveal chạy special movement clip của pack, name/toast; food persistence TODO sau My Pet |
| `pet_detail/{target}/{slotIndex}/{packKey}` | `ui/catalog/PetDetailScreen` | Hero preview và select action đúng mode/slot |
| `settings` | `ui/home/settings/SettingsScreen` | Tab Mine theo Figma: shared Home chrome, battery toggle, shortcuts, General và Other actions |
| `pet_customization/{slotIndex}` | `ui/home/settings/PetCustomizationScreen` | Hồ sơ cute-pet độc lập: character, size, speed, touch, speech, custom messages, position và remove |
| `swarm_customization` | `ui/home/swarm/SwarmCustomizationScreen` | Edit Swarm riêng: character, count, size/speed, random variation và movement insets |
| `battery_catalog` | `ui/battery/catalog/BatteryCatalogScreen` | Tab Battery theo Figma: shared Home header/toggle, promo Customize Status Bar, carousel theo category, favorite/Premium, native ad sau Trending và Accessibility gate |
| `battery_category/{categoryId}` | `ui/battery/catalog/BatteryCategoryScreen` | Child route từ More: Back/title/PRO, inline banner, grid ba cột, selected theme và crown Premium; dùng chung catalog ViewModel |
| `battery_editor/{themeId}` | `ui/battery/editor/BatteryEditorScreen` | Overview khởi tạo cặp pet+pin; picker category đổi hai asset độc lập, Rewarded/Premium theo component, live preview, Apply cố định và disclosure |
| `battery_editor_component/{themeId}/{page}` | `ui/battery/editor/BatteryEditorScreen` | Destination editor riêng theo component, dùng ViewModel/draft của overview; Back/Done phục hồi đúng vị trí cuộn và có screen tracking độc lập |
| `premium/{startByIndex}` | `ui/premium/PremiumScreen` | Subscription UI, close behavior theo entry source |

## Visual scope hiện tại

- Discover, Battery, Battery category và Mine dùng wallpaper trắng với gradient pastel, primary pink `#FB3675`, Roboto,
  horizontal catalog rows và rounded cards theo Figma node `8015:1035`. My Pet và các màn
  product cũ tiếp tục dùng cozy palette hiện tại cho tới task refresh riêng.
- Splash và App Open Welcome Back đã theo Figma pastel. Language/Language Settings, Intro,
  Permission và Premium giữ nguyên visual hiện tại; chúng sẽ được update trong các task riêng.

## Screen implementation contract

- Screen mới có `Screen`, `ViewModel`, `UiState` trừ trường hợp presentational page rất nhỏ đã được owner chấp thuận.
- Screen nhận navigation callback, không tự sở hữu NavController.
- Phải biểu diễn loading/content/empty/error nếu data feature có các trạng thái đó.
- User-facing resources, analytics và accessibility là một phần acceptance criteria.
- Khi screen thay đổi route/entry/exit/back behavior, cập nhật file này và `../04_NAVIGATION_FLOW.md`.
