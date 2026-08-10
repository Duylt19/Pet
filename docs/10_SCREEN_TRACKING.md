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
| Grant Permissions | `grant_permissions` |
| Discover Home | `home` |
| Search | `search` |
| Favourite & Recent | `favourite_recent` |
| My Pet | `my_pet` |
| Pet catalog | `pet_catalog` |
| Pet Store | `pet_store` |
| Pet detail | `pet_detail` |
| Customize Pet | `pet_customization` |
| Edit Pet Swarm | `swarm_customization` |
| Battery styles | `battery_catalog` |
| Battery category | `battery_category` |
| Customize status bar overview | `battery_editor` |
| Battery size editor | `battery_size_editor` |
| Battery appearance editor | `battery_appearance_editor` |
| Battery emoji editor | `battery_emoji_editor` |
| Battery icon editor | `battery_icon_editor` |
| Battery animation editor | `battery_animation_editor` |
| Battery Wi-Fi editor | `battery_wifi_editor` |
| Battery mobile data editor | `battery_data_editor` |
| Battery signal editor | `battery_signal_editor` |
| Battery airplane editor | `battery_airplane_editor` |
| Battery hotspot editor | `battery_hotspot_editor` |
| Battery ringer editor | `battery_ringer_editor` |
| Battery charge editor | `battery_charge_editor` |
| Battery date/time editor | `battery_date_time_editor` |
| Settings | `settings` |
| Premium | `premium` |

## Rules

- Dùng `TrackScreenView(ScreenName.X)` tại screen nhìn thấy.
- Pager chỉ track page đang visible.
- Value lowercase snake_case, unique, ổn định và không quá 100 ký tự.
- Không tái sử dụng screen name cũ cho meaning mới.
- Không log PII, token hoặc nội dung user.
- Khi thêm/xóa screen, cập nhật enum, `ScreenNameTest`, file này và navigation docs.
