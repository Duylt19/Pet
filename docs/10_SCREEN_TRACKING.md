# Screen Tracking

## 1. Nguồn dữ liệu duy nhất

- Tất cả `screen_view` phải đi qua `TrackScreenView(ScreenName, isVisible)` trong
  `utils/AnalyticsHelper.kt`.
- Không gửi string trực tiếp từ Composable, ViewModel hoặc navigation.
- `ScreenName.value` dùng lowercase `snake_case`, không đổi tên sau khi release nếu không có
  kế hoạch cập nhật dashboard.
- `screen_class` luôn là `MainActivity`.
- Firebase automatic screen reporting đã tắt trong `AndroidManifest.xml`.

## 2. Quy tắc lifecycle và visibility

- Chỉ log khi lifecycle của destination đang ở `RESUMED`.
- Mỗi screen được log một lần trong một chu kỳ visible/resumed.
- Khi user rời màn rồi quay lại, log lại screen đó.
- Page do pager pre-compose nhưng chưa hiển thị phải truyền `isVisible = false`.
- Khi selected page/tab thay đổi, tracker đổi sang đúng `ScreenName`; không log thêm tên của
  container cha.
- Dialog, popup, menu và bottom sheet không phải screen, không gọi `TrackScreenView`.

## 3. Home pager

`HomeScreen` truyền visibility theo `pagerState.currentPage` cho từng page:

| Nội dung đang thấy | screen_name |
|---|---|
| Browser home | `home_browser` |
| Tabs - Normal | `tabs_normal` |
| Tabs - Private | `tabs_private` |
| Search Normal tabs | `tabs_search_normal` |
| Search Private tabs | `tabs_search_private` |
| Downloads - All | `downloads_all` |
| Downloads - Active | `downloads_active` |
| Downloads - Completed | `downloads_completed` |
| Bookmarks | `bookmarks` |
| History | `history` |
| Files home | `files_home` |

Không gửi thêm `home` hoặc `downloads` khi một tab con ở trên đang hiển thị.

## 4. Navigation và content pager

- Intro gửi `intro_page_1`, `intro_page_2`, `intro_page_3` theo page đang thấy.
- Browser gửi `browser_normal` hoặc `browser_private` theo active session.
- Select tabs gửi `tab_selection_normal` hoặc `tab_selection_private`.
- Media list gửi `files_images`, `files_video`, `files_audio`, `files_documents`.
- Media viewer gửi `viewer_image`, `viewer_video`, `viewer_audio`, `viewer_file`.
- Các destination đơn dùng đúng enum tương ứng như `settings`, `premium`, `privacy_policy`.

## 5. Thêm screen mới

1. Thêm enum vào `ScreenName`; không tạo constant/string ở feature.
2. Gọi `TrackScreenView()` tại composable cấp screen.
3. Nếu nằm trong pager, nhận `isVisible` từ container và truyền vào tracker.
4. Nếu có tab con thay toàn bộ nội dung, map từng tab sang một `ScreenName` riêng.
5. Cập nhật expected screen set trong `ScreenNameTest`, sau đó chạy test để kiểm tra thiếu tên,
   trùng tên, format và giới hạn Firebase.

## 6. Phạm vi không dùng `screen_view`

Dialog, popup, menu và bottom sheet không tạo thêm `screen_view`; chúng là overlay của screen
đang visible. Nếu cần đo tương tác trong overlay, dùng custom action event thay vì thêm screen name.
Điều này tránh dashboard bị tách phiên screen khi user chỉ mở More, Information hoặc confirmation.
