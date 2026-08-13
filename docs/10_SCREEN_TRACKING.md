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
| Accessibility How to use | `accessibility_how_to_use` |
| Discover Home | `home` |
| Search | `search` |
| Favourite & Recent | `favourite_recent` |
| My Pet | `my_pet` |
| Shimeji Pets | `pet_store` |
| Battery styles | `battery_catalog` |
| Battery category | `battery_category` |
| Customize status bar overview | `battery_editor` |
| Battery template picker | `battery_template_picker` |
| Battery background theme picker | `battery_background_theme_picker` |
| Battery size editor | `battery_size_editor` |
| Battery appearance editor | `battery_appearance_editor` |
| Battery emoji editor | `battery_emoji_editor` |
| Battery emotion packs | `battery_emotion_editor` |
| Battery emotion group detail | `battery_emotion_detail` |
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
| Battery clock editor | `battery_clock_editor` |
| Battery Troll themes | `battery_troll` |
| Battery Troll customize | `battery_troll_customize` |
| Settings | `settings` |
| Premium | `premium` |

## Rules

- Dùng `TrackScreenView(ScreenName.X)` tại screen nhìn thấy.
- Pager chỉ track page đang visible.
- Mỗi destination/editor page nhìn thấy phải có screen name riêng; không dùng chung event chỉ vì
  hai màn cùng chỉnh một nhóm dữ liệu.
- Value lowercase snake_case, unique, ổn định và không quá 100 ký tự.
- Không tái sử dụng screen name cũ cho meaning mới.
- Không log PII, token hoặc nội dung user.
- Khi thêm/xóa screen, cập nhật enum, `ScreenNameTest`, file này và navigation docs.

## Runtime contract

- `google_analytics_automatic_screen_reporting_enabled=false`; Single-Activity Compose chỉ dùng
  manual `screen_view` để tránh Firebase tự ghi mọi destination thành `MainActivity`.
- `TrackScreenView` chỉ log khi lifecycle owner của destination ở trạng thái `RESUMED`, log lại
  khi user quay về màn sau navigation hoặc app resume, và không log adjacent pager page.
- Intro dùng `PagerState.settledPage`; swipe chưa hoàn tất không được tính là screen view.
- Battery Editor map từng `BatteryEditorPage` sang một `ScreenName` riêng. Picker, option editor,
  emotion detail và Clock không được tái sử dụng event của màn khác.
- `ScreenTrackingCoverageTest` đối chiếu mọi `*Screen()` được gọi trực tiếp trong `NavGraph` với
  source owner có `TrackScreenView`; route mới thiếu tracker sẽ làm unit test fail.
- Dialog/bottom sheet tạm thời không phát `screen_view` vì destination phía sau vẫn là màn visible.
  Nếu cần đo funnel của dialog, dùng action event riêng thay vì giả thành screen event.
