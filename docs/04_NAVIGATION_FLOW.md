# 04 — Navigation Flow

## Routes hiện tại

| Route | Screen | Ghi chú |
|---|---|---|
| `splash` | Splash | Entry mặc định |
| `language` | Language onboarding | First-run |
| `language_settings` | Language settings | Mở từ Settings |
| `intro` | Intro pager | First-run |
| `permission` | Permission | First-run, có Continue/Skip |
| `home` | Home | Placeholder 2 action |
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

Home ──Settings──> Settings ──Language──> Language Settings
Home ──Premium──> Premium(in-app)
```

## Back stack

- Splash, Language, Intro và Permission được remove khỏi stack sau khi hoàn tất bước tương ứng.
- Settings và Premium in-app pop về Home.
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
