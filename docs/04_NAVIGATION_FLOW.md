# 04 — Navigation Flow

## Routes hiện tại

| Route | Screen | Ghi chú |
|---|---|---|
| `splash` | Splash | Entry mặc định |
| `language` | Language onboarding | First-run |
| `language_settings` | Language settings | Mở từ Settings |
| `intro` | Intro pager | First-run |
| `permission` | Permission | Request overlay/notification, có Continue/Skip |
| `home` | Home | Hai mode loại trừ nhau: Mixed và Pet Swarm; enable/disable overlay + Catalog/Settings/Premium |
| `pet_catalog/{target}/{slotIndex}` | Pet Catalog | `target=MIXED/SWARM`; lưới owner pet từ GitHub raw/cache, download + SHA-256 + Set |
| `pet_detail/{target}/{slotIndex}/{packKey}` | Pet Detail | Preview metadata, xác nhận pack cho đúng mode/slot và quay lại Catalog |
| `settings` | Settings | My Pet Family roster + app/support hub |
| `pet_customization/{slotIndex}` | Customize Pet | Character, size, speed, touch, speech, messages và position của đúng slot |
| `swarm_customization` | Edit Pet Swarm | Character, count, base size/speed, random variation và vùng di chuyển |
| `premium/{startByIndex}` | Premium | Typed source behavior |

## Flow

```text
Splash
  └─ next onboarding step hoặc Home

Language ──confirm──> Intro
Intro ──finish──> Premium(onboarding, optional) ──close──> Permission
Intro ──finish──> Permission
Permission ──continue/skip──> Home

Home ──Start(no overlay)──> System Overlay Settings ──back──> Home
Home ──Start(API 33+, notification missing)──> Notification permission ──result──> Start pet
Home ──Start/Stop──> Pet overlay foreground service
Home ──Mixed──> bật/tắt từng pet; tối thiểu một pet visible
Home ──Mixed slot 1–3──> Catalog(target=MIXED, slot) ──Set/Import──> kích hoạt slot
Home ──Mixed slot 4–12──> Catalog reward gate ──Rewarded earned──> Set/Import
Home ──Mixed slot 4–12 + Premium──> Catalog, bỏ qua Rewarded
Home ──Swarm locked──> Rewarded completed ──persist unlock──> chọn một pet + count 1–12
Home ──Swarm + Premium──> tự unlock, không hiển thị Rewarded
Home ──Swarm configured──> Edit Pet Swarm ──Change character──> Catalog(SWARM)
Home ──Add/Change──> Catalog(target, slot) ──search/category──> Download/verify/Set
Settings ──Pet card──> Customize Pet(slot) ──Change character──> Catalog(slot)
Settings ──Add pet──> Catalog(slot trống) ──Rewarded nếu là slot 4–12──> Set/Import
Customize Pet ──Remove──> Settings (shift slot sau, giữ profile riêng)
Catalog ──already prepared pack──> Detail ──Use for Pet──> Catalog
Home ──Settings──> Settings ──Language──> Language Settings
Home ──Premium──> Premium(in-app)
```

## Back stack

- Splash, Language, Intro và Permission được remove khỏi stack sau khi hoàn tất bước tương ứng.
- Settings và Premium in-app pop về Home.
- Customize Pet pop về Settings. Pet Detail pop về Catalog; Catalog pop về màn đã mở
  nó. Xác nhận pack trong Detail cũng quay lại Catalog để user thấy selection mới. Add chỉ
  tăng `petCount` sau Set/Import thành công; Back khỏi Catalog không tạo pet.
  `target` parse an toàn về enum, `slotIndex` là typed Int và `packKey` luôn URI-encode
  trước navigation.
- Edit Pet Swarm pop về Home; Catalog mở từ màn này pop về Edit Pet Swarm.
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
- Home refresh permission ở `ON_RESUME`; nếu overlay bị thu hồi khi service đang chạy, app stop service.
