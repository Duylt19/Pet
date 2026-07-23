# 04 — Navigation Flow

## Routes hiện tại

| Route | Screen | Ghi chú |
|---|---|---|
| `splash` | Splash | Entry mặc định |
| `language` | Language onboarding | First-run |
| `language_settings` | Language settings | Mở từ Settings |
| `intro` | Intro pager | First-run |
| `permission` | Permission | Request overlay/notification, có Continue/Skip |
| `home` | Home | Selected pet + Start/Stop + Catalog/Settings/Premium |
| `pet_catalog/{slotIndex}` | Pet Catalog | Chọn character cho slot typed 0–2; 1,026 owner pets, search/category, Set + secure file import |
| `pet_detail/{slotIndex}/{packKey}` | Pet Detail | Preview metadata và select pack cho đúng slot |
| `settings` | Settings | Mở từ Home |
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
Home ──Choose a pet──> Catalog(slot 0) ──search/category──> Set local owner pet
Settings ──Pet 1/2/3──> Catalog(slot) ──Set──> cập nhật đúng slot
Catalog ──already prepared pack──> Detail
Home ──Settings──> Settings ──Language──> Language Settings
Home ──Premium──> Premium(in-app)
```

## Back stack

- Splash, Language, Intro và Permission được remove khỏi stack sau khi hoàn tất bước tương ứng.
- Settings và Premium in-app pop về Home.
- Pet Detail pop về Catalog; Catalog pop về màn đã mở nó. `slotIndex` là typed Int và `packKey` luôn URI-encode trước navigation.
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
