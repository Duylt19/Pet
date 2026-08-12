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
| Grant Permissions | `grant_permissions` | Native `screen_permission` ghim đáy |
| Accessibility How to use | `accessibility_how_to_use` | Không thêm placement; disclosure trước đó dùng `screen_permission` |
| Discover | `home` | Banner shell Home + banner inline `discover_inline` |
| Battery styles | `battery_catalog` | Banner shell Home, native `screen_home`, Rewarded khi unlock |
| Battery category | `battery_category` | Banner shell Home + banner inline `battery_category_inline` |
| Shimeji Pets | `pet_store` | Banner shell Home; reward sheet dùng native `screen_home` + Rewarded |
| My Pet | `my_pet` | Banner shell Home |
| Mine/Settings | `settings` | Banner shell Home; permission disclosure dùng native `screen_permission` |
| Search | `search` | Native `screen_home` + banner inline `search_inline` |
| Favourite & Recent | `favourite_recent` | Native `screen_home` |
| Battery editor overview/library | `battery_editor` hoặc screen con tương ứng | Banner `battery_editor_bottom` |
| Battery editor option/emotion/detail | Screen editor tương ứng | Native `screen_home` dạng collapsible theo route policy |
| Premium | `premium` | Không có placement |
| Exit dialog | Giữ screen hiện tại | Native `dialog_exit_app` |

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
