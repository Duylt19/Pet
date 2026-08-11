# 04 — Navigation Flow

## Routes hiện tại

| Route | Screen | Ghi chú |
|---|---|---|
| `splash` | Splash | Entry mặc định |
| `language` | Language onboarding | First-run |
| `language_settings` | Language settings | Mở từ Settings |
| `intro` | Intro pager | First-run |
| `permission` | Permission | Request overlay/notification, có Continue/Skip |
| `home` | Discover | Tab 1 của Home shell: battery toggle, Battery Troll hero và catalog preview |
| `search` | Search | Tìm pet hoặc battery theme; pet mở Pet Store, theme mở Status Bar Editor |
| `favourite_recent` | Favourite & Recent | Favourite battery theme đã lưu; Recent giữ empty state cho tới khi có contract MRU |
| `grant_permissions` | Grant Permission | Destination độc lập, **không phải** tab Home: `homeTabForRoute` trả `null` nên bottom navigation ẩn. Lối vào duy nhất là row trong Mine, không có interstitial; Back pop về Mine. Khác hẳn `permission` (bước onboarding) |
| `my_pet` | My Pet Room | Scene phòng in-app + sheet ba tab; Back pop về màn trước, shortcut mở tab Pet Store |
| `pet_store` | Pet Store | Tab 3 của Home shell: duyệt pet/food, Rewarded/Premium gate, download/verify chỉ để mở khóa |
| `settings` | Mine | Tab 4 của Home shell: Emoji Battery toggle, shortcuts và app/support hub |
| `battery_catalog` | Battery Styles | Tab 2 của Home shell: catalog local + category/favorite/Premium gate; editor luôn mở được với preview nhúng |
| `battery_category/{categoryId}` | Battery category | Child destination từ action More: grid ba cột của category, Back về đúng vị trí Battery Styles |
| `battery_editor/{themeId}` | Customize Status Bar | Overview khởi tạo cặp pet+pin, cho phép đổi hai phần độc lập, giữ draft và live preview qua Accessibility |
| `battery_editor_component/{themeId}/{page}` | Battery editor child | Library Battery/Emoji/Theme và từng status component; dùng chung ViewModel/draft với overview |
| `premium/{startByIndex}` | Premium | Typed source behavior |

## Flow

```text
Splash
  └─ next onboarding step hoặc Home

Language ──confirm──> Intro
Intro ──finish──> Premium(onboarding, optional) ──close──> Permission
Intro ──finish──> Permission
Permission ──continue/skip──> Discover Home

Home shell tabs: Discover ⇄ Battery Styles ⇄ Pet Store ⇄ Mine/Settings

Discover ──Emoji Battery toggle(no access)──> Accessibility disclosure/settings ──back──> enable battery overlay
Discover ──Battery/Theme/Emoji──> Battery Styles hoặc Customize Status Bar
Discover ──Search──> Search ──theme──> Customize Status Bar
Search ──pet──> Pet Store
Discover ──Pet Store──> Pet Store ──Rewarded/Premium──> Download/verify/unlock ──> bật ở slot Mixed trống đầu tiên
Discover ──Trending pet──> Pet Store
Discover ──Mine──> Mine
My Pet ──pet card──> detail panel ──Pet on screen──> Pet overlay foreground service
My Pet ──Add/Food+──> Pet Store
Mine ──My Pet──> My Pet
Mine ──Favourite & Recent──> Favourite & Recent ──favourite theme──> Customize Status Bar
Mine ──Language──> Language Settings
Mine ──Emoji Battery toggle──> Accessibility disclosure/settings
Mine ──Grant Permission──> Grant Permissions ──Accessibility chưa cấp──> consent disclosure ──Settings
                                      └─ quyền đã cấp/permission khác ──> system surface tương ứng ──back──> đọc lại trạng thái
Mine ──Rate/Share/Contact/Privacy──> action tương ứng
Discover/My Pet ──Settings──> Mine ──Language──> Language Settings
Discover/My Pet ──Premium──> Premium(in-app)
Discover/My Pet ──Battery──> Battery Styles ──More──> Battery category ──theme──> Customize Battery Bar
Customize Battery Bar ──Apply khi chưa có quyền──> Accessibility disclosure/settings
  └─ theme ID khởi tạo cả pet + pin; editor có thể mix hai theme khác nhau
Customize Battery Bar ──locked pet/pin──> Rewarded hoặc Premium ──return──> chọn component
Customize Battery Bar ──component option──> Component Editor ──Done/Back──> đúng scroll offset của overview
Customize Battery Bar ──Apply(service on)──> persist config + accessibility overlay
```

Sau khi pet được verify/cài thành công, Pet Store hiển thị unlock-success overlay và chạy
clip movement `SPECIAL` của chính pack vừa cài (`SPECIAL_2`/thumbnail là fallback), rồi mới
chuyển sang bước đặt tên khi user chạm Continue. Pet mới được bật atomically ở slot Mixed
trống đầu tiên để switch `Pet on screen` mặc định ON; flow không đổi cấu hình Swarm và không
thay pet khác nếu toàn bộ roster Mixed đã đầy.

## Back stack

- Splash, Language, Intro và Permission được remove khỏi stack sau khi hoàn tất bước tương ứng.
- Discover là root sau onboarding. Battery Styles, Pet Store và Settings là top-level tab
  của cùng Home shell. Mỗi lần đổi tab dùng `saveState/restoreState` và `launchSingleTop`,
  vì vậy ViewModel, scroll và navigation state của tab được giữ lại.
- Search, My Pet và Premium là destination con và pop về destination đã mở chúng;
  My Pet không thay thế root Discover.
- Search `Cancel`/Back pop về Discover; chọn theme mở Battery Editor và Back quay lại Search.
- Pet Store là top-level tab; chọn Discover chuyển tab về root thay vì tạo thêm route.
  Pet tải từ Store được cài/mở khóa và bật ở slot Mixed trống đầu tiên. Nếu roster đã đầy,
  ownership vẫn được giữ nhưng không thay selection hiện có. `View` sau khi đặt tên mở My Pet
  như một destination con; Pet Store không đổi cấu hình Swarm.
- Battery category dùng chung catalog ViewModel; Editor mở từ category sẽ pop về category,
  sau đó Back pop về đúng scroll của Battery Styles. Battery Editor mở trực tiếp từ landing
  pop về Battery Styles; Battery Styles là top-level Home tab. Premium mở từ
  catalog hoặc picker component trong editor rồi quay lại đúng destination theo back stack;
  editor refresh entitlement và hoàn tất pending component selection khi resume.
- Mỗi Battery component editor là một destination nằm trên overview. Nó dùng ViewModel của
  overview để giữ nguyên draft/live preview; Back hoặc Done chỉ pop destination con, vì vậy
  overview phục hồi đúng scroll offset và không khởi tạo lại catalog/picker.
- Theme selection trong Battery Styles mở editor ngay cả khi chưa có Accessibility. Preview
  nhúng vẫn hoạt động; Apply mới hiện disclosure và chỉ bật overlay khi quyền hợp lệ.
- Mọi action xin Accessibility trong Discover, Battery Styles, Mine, Status Bar Editor và Grant
  Permissions dùng cùng bottom-sheet disclosure. `Allow` không mở Settings cho tới khi checkbox
  consent được chọn; launcher tắt App Open Ad trước khi rời app và trạng thái được đọc lại khi về.
- Premium onboarding close/success đi tiếp Permission.
- Premium splash-return close/success đi Home.
- Language settings restart activity với `skip_splash=true` sau confirm.

## Onboarding state

`MainViewModel` kết hợp các Flow trong `DataStoreManager` để xác định màn tiếp theo. Khi thêm một onboarding step mới phải cập nhật state, route, popUpTo behavior, process-death behavior và docs này.

## Navigation rules

- Route constant chỉ định nghĩa trong `Routes`.
- Dùng `safeNavigate`/`safePopBackStack`.
- Full-screen ad transition dùng `navigateWithAd` theo policy.
- Bottom navigation và placement `home_mode_bottom` do Home shell trong `AppNavGraph` sở
  hữu. Child `battery_category/{categoryId}` ẩn bottom navigation nhưng tiếp tục dùng cùng
  banner holder để không request/reload banner khi đi từ Battery Styles sang category.
- String argument phải encode; enum argument phải parse an toàn với fallback.
- Không phục hồi route Private Browser cũ nếu chưa có feature spec mới.
- Overlay permission được mở qua `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`; đây là special access, không phải runtime permission dialog.
- Notification permission chỉ request trên API 33+; denial không ngăn FGS chạy nhưng notification có thể chỉ hiện trong system task manager.
- Discover refresh Accessibility ở `ON_RESUME`; intent bật battery được tiếp tục sau khi user
  cấp service. My Pet refresh overlay permission ở `ON_RESUME`; nếu overlay bị thu hồi khi
  service đang chạy, app stop service.
