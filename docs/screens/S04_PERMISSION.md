# S04 — Permission Request

## Mục Đích

Giải thích và xin quyền quản lý shared storage cho Media Library, File Manager và Vault. Đây là bước cuối onboarding, xuất hiện sau màn Set Default Browser. Notification không được xin tại màn này.

## Navigation

- Route: `Routes.PERMISSION`
- Vào từ: `SET_DEFAULT_BROWSER`, hoặc trực tiếp sau Intro nếu app đã là trình duyệt mặc định
- Ra đến: `HOME`
- Back: bị chặn trong onboarding
- Continue/Grant later: đều lưu `IS_PERMISSION_COMPLETED = true` rồi vào Home

## Ma Trận Quyền

| Android API | Storage | Download notification |
|-------------|---------|-----------------------|
| 24–28 | `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` | Không cần runtime permission |
| 29 | `READ_EXTERNAL_STORAGE` | Không cần runtime permission |
| 30–32 | All files special access | Không cần runtime permission |
| 33–36+ | All files special access | Xin `POST_NOTIFICATIONS` ở download đầu tiên |

Build hiện tại khai báo `MANAGE_EXTERNAL_STORAGE` để tab Files liệt kê và xóa file trong shared storage, đồng thời chuẩn bị cho Vault. Files ngoài phạm vi quản lý vẫn có thể được mở bằng Storage Access Framework. Camera, microphone và location chỉ được xin theo origin khi website yêu cầu; file chooser của website dùng SAF nên không cần storage permission.

### All files access và Vault

Trên API 30+, app mở special-access Settings bằng `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` (fallback `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`) và xác nhận kết quả bằng `Environment.isExternalStorageManager()`; quyền này không đi qua runtime permission launcher. Khi được cấp, Photos/Videos/Audio đọc MediaStore và Files đọc `MediaStore.Files`. User từ chối vẫn vào Home và dùng Browser, Download, SAF bình thường; Media Library, Files và Vault hiển thị trạng thái yêu cầu All Files Access.

## UI

- Illustration, title và mô tả quyền riêng tư
- Một Storage row với switch và trạng thái `Allowed`/`Not allowed`
- API 30+: switch mở special-access Settings
- API 24–29: switch mở runtime storage permission tương ứng
- Continue button và Grant later
- Native ad ở cuối màn hình

## State

`PermissionViewModel` đọc quyền thực tế từ hệ thống mỗi khi màn hình resume. API 30+ dùng `AllFilesAccess.isGranted()`; API cũ kiểm tra runtime permission. `PermissionPolicy` trả về danh sách quyền theo `sdkInt`, giúp logic có thể unit test.

App không dùng granular hoặc partial media permission trên API 30+. Khi runtime storage permission trên API cũ bị từ chối vĩnh viễn, lần request tiếp theo hiển thị dialog mở App Settings.

Số lần request được lưu cho legacy storage permission trên API 24–29. Nếu người dùng từ chối hai lần và system dialog không còn xuất hiện, app kết hợp request count với `shouldShowRequestPermissionRationale()` để chuyển action sang App Settings. API 30+ luôn quay lại special-access Settings.

`POST_NOTIFICATIONS` không nằm trong onboarding. `DownloadForegroundService.start()` và `resume()` phát event về `MainActivity`; từ API 33 app chỉ request ở download đầu tiên nếu chưa được cấp, ghi request count trước khi launch và không hỏi lặp lại nếu user từ chối. Download vẫn chạy bình thường vì notification permission không phải điều kiện khởi động foreground service.

Camera, microphone và location của website có request count riêng cho từng permission trong DataStore. Sau hai lần system request im lặng, dialog theo origin chuyển user sang App Settings; khi quay lại app sẽ grant hoặc deny `WebChromeClient.PermissionRequest` theo trạng thái quyền thực tế.

## Manifest Merge Audit

Quyền app trực tiếp sử dụng gồm Internet; camera/microphone/location cho website; foreground data-sync và notification cho download; legacy storage đến API 29; All files access cho Media Library/Files/Vault. Không có `READ_MEDIA_*`, quyền contacts, SMS, call log, background location, Bluetooth hoặc install packages trong `uses-permission` của app.

Manifest merge còn có các normal permission từ SDK Ads, Analytics, Billing và Install Referrer như `ACCESS_NETWORK_STATE`, advertising/AdServices permissions, `READ_BASIC_PHONE_STATE`, `BILLING`, `VIBRATE`, `ACCESS_WIFI_STATE`, `WAKE_LOCK` và install-referrer binding. Đây là dependency permissions có code sử dụng; không xóa bằng manifest override nếu chưa loại dependency tương ứng. `android:permission="android.permission.INSTALL_PACKAGES"` trên `AdjustReferrerReceiver` là permission bảo vệ caller của receiver, không phải quyền app xin từ user.

## Acceptance Criteria

- [x] Flow là Set Default Browser → Permission → Home
- [x] Không request quyền không tồn tại trên API cũ
- [x] API 30+ xin All files access qua special-access Settings
- [x] API 30+ không khai báo hoặc xin granular media permission
- [x] Từ chối All files access không chặn Browser, Download hoặc SAF
- [x] Notification chỉ xin một lần theo ngữ cảnh download từ API 33
- [x] Resume từ Settings cập nhật trạng thái
- [x] Continue và Grant later không làm lặp onboarding
- [x] Có unit test cho API 24–36+
