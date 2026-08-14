# 11 — Screen and Ad Placement Matrix

Firebase screen tracking và ad placement là hai contract độc lập: màn visible log đúng một
`ScreenName`, còn quảng cáo chỉ render ở các surface đã được product gắn sẵn. Không suy ra hoặc
tự thêm quảng cáo chỉ từ tên route.

| Surface visible | Firebase screen | Ads hiện có |
|---|---|---|
| Splash | `splash` | Banner `splash_bottom`; launcher interstitial |
| Language onboarding/settings | `language_onboarding` / `language_settings` | Native `screen_language`, sau chọn dùng `screen_language_second` |
| Intro page 1 | `intro_page_1` | Native `screen_intro` |
| Intro page 2 | `intro_page_2` | Không có placement |
| Intro page 3 | `intro_page_3` | Native `screen_intro_second` |
| Permission onboarding | `permission` | Native `screen_permission` |
| Grant Permissions | `grant_permissions` | Native `screen_grant_permissions` ghim đáy |
| Accessibility How to use | `accessibility_how_to_use` | Không thêm placement; disclosure trước đó dùng `dialog_accessibility_disclosure` |
| Discover | `home` | Banner shell Home + banner inline `discover_inline` |
| Battery styles | `battery_catalog` | Banner shell Home, native `screen_battery_catalog`; reward sheet dùng `dialog_battery_reward` + Rewarded |
| Battery category | `battery_category` | Banner shell Home + banner inline `battery_category_inline` |
| Shimeji Pets | `pet_store` | Banner shell Home; pet reward dùng `dialog_pet_reward`, food reward dùng `dialog_food_reward` + Rewarded |
| My Pet | `my_pet` | Banner shell Home |
| Mine/Settings | `settings` | Banner shell Home; disclosure dùng `dialog_accessibility_disclosure` hoặc `dialog_overlay_permission` |
| Search | `search` | Native `screen_search` + banner inline `search_inline` |
| Favourite & Recent | `favourite_recent` | Native `screen_favourite_recent` |
| Customize Status Bar overview | `battery_editor` | Native `screen_customize_status_bar` dạng collapsible |
| Battery editor library | Screen library tương ứng | Banner `battery_editor_bottom` |
| Battery editor option/emotion/detail | Screen editor tương ứng | Native `screen_battery_editor` dạng collapsible; discard sheet dùng `dialog_battery_discard` |
| Battery Troll | `battery_troll` | Banner inline `battery_category_inline`; reward sheet dùng `dialog_battery_troll_reward` + Rewarded |
| Premium | `premium` | Không có placement |
| Exit dialog | Giữ screen hiện tại | Native `dialog_exit_app` |

## Native metadata

| Surface | Screen code | Remote Config | Ad-unit string | Layout |
|---|---|---|---|---|
| Language initial | `screen_language` | `is_show_native_language` | `id_emoji_battery_native_language` | `HEIGHT_222_SMALL_CTA` |
| Language selected | `screen_language_second` | `is_show_native_language_second` | `id_emoji_battery_native_language_second` | `HEIGHT_222` |
| Intro page 1 | `screen_intro` | `is_show_native_intro` | `id_emoji_battery_native_intro` | `HEIGHT_222` |
| Intro page 3 | `screen_intro_second` | `is_show_native_intro_second` | `id_emoji_battery_native_intro_second` | `HEIGHT_222` |
| Permission onboarding | `screen_permission` | `is_show_native_permission` | `id_emoji_battery_native_permission` | `HEIGHT_222` |
| Grant Permissions | `screen_grant_permissions` | `is_show_native_grant_permissions` | `id_emoji_battery_native_grant_permissions` | `HEIGHT_222` |
| Accessibility disclosure | `dialog_accessibility_disclosure` | `is_show_native_accessibility_disclosure` | `id_emoji_battery_native_accessibility_disclosure` | `HEIGHT_222` |
| Overlay disclosure | `dialog_overlay_permission` | `is_show_native_overlay_permission` | `id_emoji_battery_native_overlay_permission` | `HEIGHT_222` |
| Search | `screen_search` | `is_show_native_search` | `id_emoji_battery_native_search` | `HEIGHT_222` |
| Favourite & Recent | `screen_favourite_recent` | `is_show_native_favourite_recent` | `id_emoji_battery_native_favourite_recent` | `HEIGHT_222` |
| Battery catalog | `screen_battery_catalog` | `is_show_native_battery_catalog` | `id_emoji_battery_native_battery_catalog` | `HEIGHT_150` |
| Customize Status Bar | `screen_customize_status_bar` | `is_show_native_customize_status_bar` | `id_emoji_battery_native_customize_status_bar` | `COLLAPSE_SMALL` |
| Battery editor options | `screen_battery_editor` | `is_show_native_battery_editor` | `id_emoji_battery_native_battery_editor` | `COLLAPSE_SMALL` |
| Battery reward sheet | `dialog_battery_reward` | `is_show_native_battery_reward` | `id_emoji_battery_native_battery_reward` | `HEIGHT_222` |
| Battery discard sheet | `dialog_battery_discard` | `is_show_native_battery_discard` | `id_emoji_battery_native_battery_discard` | `HEIGHT_222` |
| Pet reward sheet | `dialog_pet_reward` | `is_show_native_pet_reward` | `id_emoji_battery_native_pet_reward` | `HEIGHT_222` |
| Food reward sheet | `dialog_food_reward` | `is_show_native_food_reward` | `id_emoji_battery_native_food_reward` | `HEIGHT_222` |
| Battery Troll reward | `dialog_battery_troll_reward` | `is_show_native_battery_troll_reward` | `id_emoji_battery_native_battery_troll_reward` | `HEIGHT_222` |
| Exit dialog | `dialog_exit_app` | `is_show_native_exit_dialog` | `id_emoji_battery_native_exit_dialog` | `HEIGHT_222` |

`dialog_food_reward`, `dialog_battery_troll_reward` và `dialog_exit_app` hiện dùng chung
AdMob unit với `dialog_pet_reward` theo placement sheet production. Các surface vẫn giữ resource
name và Remote Config key riêng để có thể tách inventory sau này mà không đổi code feature.

## Configuration contract

- Ad-unit production/test nằm trong `ads/src/main/res/values/strings.xml` và không được rỗng
  đối với placement đang có consumer.
- Native dùng `NativeAdPlacementCatalog`: screen code → layout → Remote Config key → string ID.
- Banner dùng ID chung và `is_show_banner_ads`; từng `adPosition` chỉ tách lifecycle/tracking.
- Rewarded dùng ID chung, `is_show_rewarded_ads`, ad-free flag và click-limit policy.
- Open/Interstitial dùng các key `is_show_open_ads`, `is_show_inter_ads` và
  `show_inter_launcher`; mọi key phải có default trong `remote_config_defaults.xml`.
- Premium/ad-free, SDK chưa init, inventory fail hoặc Remote Config off phải collapse/continue
  theo fallback của wrapper; không để placeholder rỗng chặn layout hoặc flow.
