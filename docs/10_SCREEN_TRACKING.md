# 10 — Screen Tracking

`ScreenName` trong `utils/AnalyticsHelper.kt` là nguồn canonical.

| Screen | Analytics value |
|---|---|
| Splash | `splash` |
| Language onboarding | `language_onboarding` |
| Language settings | `language_settings` |
| Intro page 1 | `intro_page_1` |
| Intro page 2 | `intro_page_2` |
| Intro page 3 | `intro_page_3` |
| Permission | `permission` |
| Home | `home` |
| Pet catalog | `pet_catalog` |
| Pet detail | `pet_detail` |
| Customize Pet | `pet_customization` |
| Settings | `settings` |
| Premium | `premium` |

## Rules

- Dùng `TrackScreenView(ScreenName.X)` tại screen nhìn thấy.
- Pager chỉ track page đang visible.
- Value lowercase snake_case, unique, ổn định và không quá 100 ký tự.
- Không tái sử dụng screen name cũ cho meaning mới.
- Không log PII, token hoặc nội dung user.
- Khi thêm/xóa screen, cập nhật enum, `ScreenNameTest`, file này và navigation docs.
