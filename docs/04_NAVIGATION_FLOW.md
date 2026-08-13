# 04 — Navigation Flow

## Routes hiện tại

| Route | Screen | Ghi chú |
|---|---|---|
| `splash` | Splash | Entry mặc định |
| `language` | Language onboarding | First-run |
| `language_settings` | Language settings | Mở từ Settings |
| `intro` | Intro pager | First-run |
| `permission` | Permission | Route/class được giữ nhưng tạm không nằm trong onboarding; request overlay/notification, có Continue/Skip |
| `discover` | Discover | Tab 1 của Home shell: battery toggle, Battery Troll hero và catalog preview |
| `search` | Search | Tìm pet hoặc battery theme; pet mở Shimeji Pets, theme mở Status Bar Editor |
| `favourite_recent` | Favourite & Recent | Favourite battery theme đã lưu; Recent giữ empty state cho tới khi có contract MRU |
| `grant_permissions?requiredTarget={accessibility\|overlay}` | Grant Permissions | Destination độc lập, **không phải** tab Home: `homeTabForRoute` trả `null` nên bottom navigation ẩn. Entry từ Mine mở dashboard trạng thái quyền (`accessibility` mặc định); `overlay` là flow Pet on Screen sau shared Draw over apps disclosure, với Overlay + Notification bắt buộc và hai bước ổn định optional. Back pop về đúng feature source. Khác hẳn `permission` (bước onboarding) |
| `accessibility_how_to_use` | Accessibility How to use | Hướng dẫn bốn bước sau consent và trước Android Accessibility Settings; app bar `exitUntilCollapsed`, CTA cố định dưới đáy |
| `my_pet` | My Pet Room | Scene phòng in-app + sheet ba tab; Back pop về màn trước, shortcut mở tab Shimeji Pets |
| `pet_store` | Shimeji Pets | Tab 3 của Home shell: duyệt pet/food, Rewarded/Premium gate, download/verify chỉ để mở khóa |
| `settings` | Mine | Tab 4 của Home shell: Emoji Battery toggle, shortcuts, app-exclusion sheet, shared pet-settings dialog và app/support hub |
| `battery_catalog` | Battery Styles | Tab 2 của Home shell: catalog local + category/favorite/Premium gate; editor luôn mở được với preview nhúng |
| `battery_category/{categoryId}` | Battery category | Child destination từ action More: grid ba cột của category, Back về đúng vị trí Battery Styles |
| `battery_editor/{themeId}` | Customize Status Bar | Overview khởi tạo cặp pet+pin, cho phép đổi hai phần độc lập, giữ draft và live preview qua Accessibility |
| `battery_editor_component/{themeId}/{page}` | Battery editor child | Library Battery/Emoji/Theme và từng status component; dùng chung ViewModel/draft với overview |
| `battery_editor_emotion_detail/{themeId}/{groupKey}` | Emotion group detail | Grid 10 emotion của một pack; dùng đúng ViewModel/draft của overview và preview ghim |
| `battery_troll` | Battery Troll Themes | Child destination từ hero Battery Troll ở Discover, **không phải** tab Home nên bottom navigation ẩn. Grid ba cột theme troll lấy từ catalog remote thứ tư; theme PREMIUM đi qua reward sheet dùng chung với Battery Styles |
| `battery_troll_customize/{trollId}` | Battery Troll Customize | Chọn Fake/Real, phần trăm giả, cỡ chữ, emoji và mức pin của theme; Apply ghi vào cùng `BatteryStatusConfig` của status-bar cover. Back khi còn thay đổi chưa lưu sẽ hiện `BatteryDiscardChangesSheet` |
| `premium/{startByIndex}` | Premium | Typed source behavior |

Trên API 33+, lần đầu một trong bốn top-level Home tab thật sự xuất hiện, app request
`POST_NOTIFICATIONS` nếu chưa cấp. Prompt chờ full-screen ad đóng và được đánh dấu trong
DataStore trước khi launch để không lặp khi đổi tab/recreate; nếu user từ chối, dashboard Grant
Permissions dẫn tới App Notification Settings thay vì tự hỏi lại ở mỗi lần mở Home.

## Flow

```text
Splash
  └─ next onboarding step hoặc Home

Language ──confirm──> Intro
Intro ──finish──> Premium(onboarding, optional) ──close──> Discover Home
Intro ──finish──> Discover Home

Permission (tạm inactive) ──continue/skip──> Discover Home

Home shell tabs: Discover ⇄ Battery Styles ⇄ Shimeji Pets ⇄ Mine/Settings

Discover ──Emoji Battery toggle(no access)──> Accessibility disclosure ──How to use──> Settings ──back──> enable battery overlay
Discover ──Battery/Theme/Emoji──> Battery Styles hoặc Customize Status Bar
Discover ──Search──> Search ──theme──> Customize Status Bar
Search ──pet──> Shimeji Pets
Discover ──More Shimeji Pets──> Shimeji Pets
Discover ──Trending pet──> Rewarded/Premium sheet dùng chung ──> Download/verify/unlock ──> bật ở slot Mixed trống đầu tiên
Pet unlock ──Save tên──> thiếu Overlay: disclosure/Grant Permissions ──> đủ quyền bắt buộc: tự start Pet on Screen
                         └─ từ chối: pet vẫn owned + Active trong My Pet; pet unlock kế tiếp hỏi lại
Discover ──Battery theme/icon──> Rewarded/Premium nếu bị khóa ──> Customize Status Bar
Discover ──Mine──> Mine
My Pet ──pet card──> detail panel ──Active/Inactive──> cập nhật Pet overlay foreground service
                              └─ tắt pet Active cuối cùng──> dialog giữ tối thiểu một pet Active
My Pet ──Shimeji Pets icon/Add pet──> Home/Shimeji Pets tab `PETS`
My Pet ──Add pet khi đã sở hữu 5 pet──> dialog hết chỗ; không điều hướng
My Pet ──Food+──> Home/Shimeji Pets tab `FOOD`
Shimeji Pets ──Pet on Screen, chưa sở hữu pet──> dialog ──Browse pets──> tab `PETS`
Shimeji Pets ──Pet on Screen, mọi pet Inactive──> dialog ──Open My Pet──> My Pet Room
Shimeji Pets ──unlock pet mới khi đã sở hữu 5 pet──> dialog hết chỗ ──Manage pets──> My Pet Room
Mine ──My Pet──> My Pet
Mine ──Favourite & Recent──> Favourite & Recent ──favourite theme──> Customize Status Bar
Mine ──Language──> Language Settings
Mine ──Emoji Battery toggle──> Accessibility disclosure ──How to use──> Settings
Mine ──Apps that hide icons──> modal picker ──switch app──> persist local package exclusion
Mine ──Setting Pets──> shared speed/size dialog ──Save──> apply cho toàn bộ pet slots
Mine ──Grant Permissions dashboard──Accessibility chưa cấp──> consent disclosure ──How to use──> Settings
                              └─ quyền đã cấp/permission khác ──> system surface tương ứng ──back──> đọc lại trạng thái
Pet on Screen Grant Permissions ──Go to Settings──> Notification ──> Overlay ──> Battery Optimization? ──> Auto Start?
                                      └─ đủ Overlay + Notification ──> start pet ngay, không chờ hai bước optional
Mine ──Rate/Share/Contact/Privacy──> action tương ứng
Discover/My Pet ──Settings──> Mine ──Language──> Language Settings
Discover/My Pet ──Premium──> Premium(in-app)
Discover/My Pet ──Battery──> Battery Styles ──More──> Battery category ──theme──> Customize Battery Bar
Customize Battery Bar ──Apply khi chưa có quyền──> Accessibility disclosure ──How to use──> Settings
  └─ theme ID khởi tạo cả pet + pin; editor có thể mix hai theme khác nhau
Customize Battery Bar ──locked pet/pin──> Rewarded hoặc Premium ──return──> chọn component
Customize Battery Bar ──custom icon──> Icon Editor ──chỉnh trực tiếp draft/preview ──Back──> overview
Customize Battery Bar ──Emotion──> 8 emotion packs ──pack──> 10 emotion styles ──chọn──> cập nhật draft/preview
Customize Battery Bar ──Apply(service on)──> persist config + accessibility overlay
```

Sau khi pet được verify/cài thành công, Pet Store hiển thị unlock-success overlay và chạy
clip movement `SPECIAL` của chính pack vừa cài (`SPECIAL_2`/thumbnail là fallback), rồi mới
chuyển sang bước đặt tên khi user chạm Continue. Pet mới được bật atomically ở slot Mixed
trống đầu tiên với trạng thái `Active`; flow không đổi cấu hình Swarm và không
thay pet khác nếu toàn bộ roster Mixed đã đầy. Sau khi lưu tên, nếu chưa có quyền Overlay thì
app hiện shared disclosure và dẫn tới Pet Grant Permissions. Từ chối quyền không rollback pack,
tên hoặc slot; mỗi pet mới tiếp theo sẽ hỏi lại khi Overlay vẫn thiếu. Khi Overlay và Notification
(trên API có runtime permission) đều sẵn sàng, service tự start. Switch `Pet on Screen` chuyển ON
ngay ở `PetOverlayRuntime.STARTING`, bỏ qua tap lặp trong lúc khởi động và được service xác nhận
`RUNNING` sau khi controller sẵn sàng; lỗi/timeout rollback về OFF.

## Back stack

- Splash, Language và Intro được remove khỏi stack sau khi hoàn tất bước tương ứng. Permission
  vẫn có destination đầy đủ nhưng tạm không được đưa vào first-run stack.
- Sau onboarding, app đi vào nested route `home_graph`; đây là graph nội bộ, không phải một
  screen analytics. Start destination của graph là Discover với route `discover`.
  Battery Styles, Pet Store và Settings là ba top-level destination còn lại của cùng Home graph.
  Mỗi lần đổi tab dùng `saveState/restoreState` và `launchSingleTop`,
  vì vậy ViewModel, scroll và navigation state của tab được giữ lại. System Back tại bất kỳ
  tab top-level nào cũng được `AppNavGraph` chặn trước default NavHost pop và mở Exit dialog
  ngay; chỉ destination con mới pop về tab nguồn. `MainActivity` không tự cài callback Back
  cạnh tranh với Navigation vì thứ tự đăng ký callback có thể đổi sau recomposition.
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
- Mỗi Battery component editor là một destination nằm trên overview và dùng ViewModel của
  overview. Mười editor Airplane/Ringer/Date/Hotspot/Charge/Clock/Animation/Wi-Fi/Signal/
  Mobile Data dùng preview cố định và state riêng cho từng switch; Clock không còn dùng chung
  size/màu với Date. Không có CTA Done ở các editor con.
- Emotion overview và detail cũng dùng ViewModel của `battery_editor/{themeId}` thay vì scope
  theo destination trung gian. Chọn style cập nhật draft/preview; Back từ detail quay về đúng
  danh sách tám pack. Hai route giữ cùng một native collapsible holder/key ở shell nên không
  reload ad khi push/pop detail.
- Theme selection trong Battery Styles mở editor ngay cả khi chưa có Accessibility. Preview
  nhúng vẫn hoạt động. Mọi chỉnh sửa ở child cập nhật ngay draft chung và preview; Back chỉ pop
  về overview, không rollback. Chỉ Apply tại overview mới hiện disclosure và persist.
- Mọi action xin Accessibility trong Discover, Battery Styles, Mine, Status Bar Editor và Grant
  Permissions dùng cùng bottom-sheet disclosure. `Allow` không mở Settings cho tới khi checkbox
  consent được chọn, sau đó đi qua `accessibility_how_to_use`. CTA tại màn hướng dẫn mới mở
  Android Settings; launcher tắt App Open Ad trước khi rời app. Cấp quyền thành công tự pop về
  đúng source và tiếp tục intent đang chờ; nếu chưa cấp thì giữ màn hướng dẫn để retry.
- Premium onboarding close/success đi thẳng Home trong thời gian bước Permission bị tắt.
- Premium splash-return close/success đi Home.
- Language settings restart activity với `skip_splash=true` sau confirm.

## Onboarding state

`MainViewModel` kết hợp các Flow trong `DataStoreManager` để xác định màn tiếp theo.
`IS_FIRST_PERMISSION_ONBOARDING_ENABLED=false` tạm bỏ Permission khỏi quyết định này và
`destinationAfterIntro()` dùng cùng policy cho cả Intro/Premium. Không ghi completion giả khi
skip để có thể bật lại đúng trạng thái cũ. Khi thêm/bật lại onboarding step phải cập nhật state,
route, popUpTo behavior, process-death behavior và docs này.

## Navigation rules

- Route constant chỉ định nghĩa trong `Routes`.
- Dùng `safeNavigate`/`safePopBackStack`.
- Full-screen ad transition dùng `navigateWithAd` theo policy.
- Bottom navigation và placement `home_mode_bottom` do `ui/home/shell/HomeShell` sở hữu.
  Child `battery_category/{categoryId}` ẩn bottom navigation nhưng tiếp tục dùng cùng
  banner holder để không request/reload banner khi đi từ Battery Styles sang category.
- String argument phải encode; enum argument phải parse an toàn với fallback.
- Không phục hồi route Private Browser cũ nếu chưa có feature spec mới.
- Mọi hand-off sang Accessibility Settings để cấp quyền đi qua màn How to use rồi dùng
  `launchFirstAvailable`: danh sách Accessibility trước để khớp bốn bước hướng dẫn, rồi mới
  fallback tới trang service (`ACCESSIBILITY_DETAILS_SETTINGS`, không phải public API).
  Không màn hình nào được `launch` một intent settings trần — ROM thiếu màn đó sẽ ném
  `ActivityNotFoundException` và hạ cả screen.
- Overlay permission là special access, không phải runtime permission dialog. Mọi entry xin quyền
  (onboarding Permission, Discover/Pet Store và Grant Permissions) phải hiện shared disclosure theo
  Figma trước. `Allow Access` từ feature source điều hướng tới
  `grant_permissions?requiredTarget=overlay`; CTA của card bắt buộc trên màn này mới mở
  `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. Nếu user đã ở Grant Permissions (đi từ Mine),
  disclosure của row overlay mở system special access trực tiếp để không tự stack lại cùng route.
- Với entry Pet on Screen, Overlay và Notification là hai gate product bắt buộc. API 33+ nếu
  Notification bị từ chối thì pet chưa start; dưới API 33 không tồn tại runtime permission nên
  gate này tự đạt. Ngay khi hai gate đạt, Grant Permissions start pet trước khi tiếp tục các bước
  ổn định optional.
- Discover refresh Accessibility ở `ON_RESUME`; intent bật battery được tiếp tục sau khi user
  cấp service. My Pet refresh overlay permission ở `ON_RESUME`; nếu overlay bị thu hồi khi
  service đang chạy, app stop service.
