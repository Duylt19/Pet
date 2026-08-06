# 04 — Navigation Flow

## Routes hiện tại

| Route | Screen | Ghi chú |
|---|---|---|
| `splash` | Splash | Entry mặc định |
| `language` | Language onboarding | First-run |
| `language_settings` | Language settings | Mở từ Settings |
| `intro` | Intro pager | First-run |
| `permission` | Permission | Request overlay/notification, có Continue/Skip |
| `home` | Discover | Landing Emoji Battery: battery toggle, catalog preview, quick actions và bottom navigation |
| `search` | Search | Tìm battery theme theo tên/category, chip gợi ý và lưới recommended |
| `my_pet` | My Pet | Mixed/Pet Swarm; enable/disable pet overlay + Catalog/Settings/Premium |
| `pet_catalog/{target}/{slotIndex}` | Pet Catalog | `target=MIXED/SWARM`; lưới owner pet từ GitHub raw/cache, download + SHA-256 + Set |
| `pet_detail/{target}/{slotIndex}/{packKey}` | Pet Detail | Preview metadata, xác nhận pack cho đúng mode/slot và quay lại Catalog |
| `settings` | Settings | My Pet Family roster + app/support hub |
| `pet_customization/{slotIndex}` | Customize Pet | Character, size, speed, touch, speech, messages và position của đúng slot |
| `swarm_customization` | Edit Pet Swarm | Character, count, base size/speed, random variation và vùng di chuyển |
| `battery_catalog` | Battery Styles | Catalog local + category/favorite/Premium gate; Accessibility gate trước editor |
| `battery_editor/{themeId}` | Customize Status Bar | Overview khởi tạo cặp pet+pin, cho phép đổi hai phần độc lập, giữ draft và live preview qua Accessibility |
| `battery_editor_component/{themeId}/{page}` | Battery component editor | Destination riêng cho Size/Appearance/Emoji/Battery và từng status component; dùng chung ViewModel/draft với overview |
| `premium/{startByIndex}` | Premium | Typed source behavior |

## Flow

```text
Splash
  └─ next onboarding step hoặc Home

Language ──confirm──> Intro
Intro ──finish──> Premium(onboarding, optional) ──close──> Permission
Intro ──finish──> Permission
Permission ──continue/skip──> Discover Home

Discover ──Emoji Battery toggle(no access)──> Accessibility disclosure/settings ──back──> enable battery overlay
Discover ──Battery/Theme/Emoji──> Battery Styles hoặc Customize Status Bar
Discover ──My Pet──> My Pet
Discover ──Search──> Search ──theme──> Customize Status Bar
Discover ──Pet Store/Trending──> Catalog/Detail
Discover ──Mine──> Settings
My Pet ──Start(no overlay)──> System Overlay Settings ──back──> My Pet
My Pet ──Start(API 33+, notification missing)──> Notification permission ──result──> Start pet
My Pet ──Start/Stop──> Pet overlay foreground service
My Pet ──Mixed──> bật/tắt từng pet; tối thiểu một pet visible
My Pet ──Mixed slot 1–3──> Catalog(target=MIXED, slot) ──Set/Import──> kích hoạt slot
My Pet ──Mixed slot 4–12──> Catalog reward gate ──Rewarded earned/unavailable──> Set/Import
My Pet ──Mixed slot 4–12 + Premium──> Catalog, bỏ qua Rewarded
My Pet ──Swarm locked──> Rewarded completed ──persist unlock──> chọn một pet + count 1–12
My Pet ──Swarm + Premium──> tự unlock, không hiển thị Rewarded
My Pet ──Swarm configured──> Edit Pet Swarm ──Change character──> Catalog(SWARM)
My Pet ──Add/Change──> Catalog(target, slot) ──search/category──> Download/verify/Set
Settings ──Pet card──> Customize Pet(slot) ──Change character──> Catalog(slot)
Settings ──Add pet──> Catalog(slot trống) ──Rewarded nếu là slot 4–12──> Set/Import
Customize Pet ──Remove──> Settings (shift slot sau, giữ profile riêng)
Catalog ──already prepared pack──> Detail ──Use for Pet──> Catalog
Discover/My Pet ──Settings──> Settings ──Language──> Language Settings
Discover/My Pet ──Premium──> Premium(in-app)
Discover/My Pet ──Battery──> Battery Styles ──theme──> Accessibility gate
Accessibility gate ──enabled/return enabled──> Customize Battery Bar
  └─ theme ID khởi tạo cả pet + pin; editor có thể mix hai theme khác nhau
Customize Battery Bar ──locked pet/pin──> Rewarded hoặc Premium ──return──> chọn component
Customize Battery Bar ──component option──> Component Editor ──Done/Back──> đúng scroll offset của overview
Customize Battery Bar ──Apply(service on)──> persist config + accessibility overlay
```

## Back stack

- Splash, Language, Intro và Permission được remove khỏi stack sau khi hoàn tất bước tương ứng.
- Discover là root sau onboarding. Search, My Pet, Settings, Catalog, Battery và Premium in-app pop
  về destination đã mở chúng; My Pet không thay thế root Discover.
- Search `Cancel`/Back pop về Discover; chọn theme mở Battery Editor và Back quay lại Search.
- Customize Pet pop về Settings. Pet Detail pop về Catalog; Catalog pop về màn đã mở
  nó. Xác nhận pack trong Detail cũng quay lại Catalog để user thấy selection mới. Add chỉ
  tăng `petCount` sau Set/Import thành công; Back khỏi Catalog không tạo pet.
  `target` parse an toàn về enum, `slotIndex` là typed Int và `packKey` luôn URI-encode
  trước navigation.
- Edit Pet Swarm pop về My Pet; Catalog mở từ màn này pop về Edit Pet Swarm.
- Battery Editor pop về Battery Styles; Battery Styles pop về Home. Premium mở từ
  catalog hoặc picker component trong editor rồi quay lại đúng destination theo back stack;
  editor refresh entitlement và hoàn tất pending component selection khi resume.
- Mỗi Battery component editor là một destination nằm trên overview. Nó dùng ViewModel của
  overview để giữ nguyên draft/live preview; Back hoặc Done chỉ pop destination con, vì vậy
  overview phục hồi đúng scroll offset và không khởi tạo lại catalog/picker.
- Theme selection trong Battery Styles chỉ navigate sau khi Accessibility đang bật.
  Pending theme ID dùng saveable state nên quay lại từ Settings/process recreation vẫn mở
  đúng editor; cancel Settings giữ user ở catalog. Editor vẫn tự gate Apply cho deep route.
- Catalog là boundary authoritative cho Mixed slot Rewarded dù được mở từ Home, Settings
  hay deep route. Chỉ slot kế tiếp được mở; đóng/fail ad không tăng capacity. Premium
  bypass gate và entitlement được refresh khi Catalog resume.
- Premium onboarding close/success đi tiếp Permission.
- Premium splash-return close/success đi Home.
- Language settings restart activity với `skip_splash=true` sau confirm.

## Onboarding state

`MainViewModel` kết hợp các Flow trong `DataStoreManager` để xác định màn tiếp theo. Khi thêm một onboarding step mới phải cập nhật state, route, popUpTo behavior, process-death behavior và docs này.

## Navigation rules

- Route constant chỉ định nghĩa trong `Routes`.
- Dùng `safeNavigate`/`safePopBackStack`.
- Full-screen ad transition dùng `navigateWithAd` theo policy.
- String argument phải encode; enum argument phải parse an toàn với fallback.
- Không phục hồi route Private Browser cũ nếu chưa có feature spec mới.
- Overlay permission được mở qua `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`; đây là special access, không phải runtime permission dialog.
- Notification permission chỉ request trên API 33+; denial không ngăn FGS chạy nhưng notification có thể chỉ hiện trong system task manager.
- Discover refresh Accessibility ở `ON_RESUME`; intent bật battery được tiếp tục sau khi user
  cấp service. My Pet refresh overlay permission ở `ON_RESUME`; nếu overlay bị thu hồi khi
  service đang chạy, app stop service.
