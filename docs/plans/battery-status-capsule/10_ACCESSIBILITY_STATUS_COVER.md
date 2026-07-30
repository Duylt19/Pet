# 10 — Accessibility Status-Cover Mode

> **IMPLEMENTED IN DEBUG — release device/policy verification pending**

## Kết luận sau khi đối chiếu reference

Việc app reference yêu cầu bật Accessibility là tín hiệu mạnh cho thấy app dùng
`AccessibilityService` tạo `TYPE_ACCESSIBILITY_OVERLAY`. Cơ chế này có thể đặt một window
trang trí lên trên status bar và tạo cảm giác thanh hệ thống đã được thay thế.

Đây là **visual replacement**, không phải SystemUI replacement:

- status bar thật vẫn được SystemUI render và quản lý phía dưới;
- app không sửa icon, clock, notification hoặc battery controller của hệ thống;
- gesture kéo notification shade vẫn phải đi tới SystemUI;
- Accessibility bị tắt, service bị kill hoặc overlay bị OEM giới hạn thì status bar thật
  xuất hiện lại ngay;
- secure system surfaces vẫn có thể nằm trên accessibility overlay.

## Cơ sở kỹ thuật

Nguồn chính thức:

- [`AccessibilityService` overlays](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [`TYPE_ACCESSIBILITY_OVERLAY`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY)
- [AOSP window layer policy](https://android.googlesource.com/platform/frameworks/base/+/android16-release/services/core/java/com/android/server/policy/WindowManagerPolicy.java)
- [Android 12 untrusted-touch behavior](https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events)
- [Google Play AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Permissions and sensitive APIs policy](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Prominent disclosure guidance](https://support.google.com/googleplay/android-developer/answer/11150561)

AOSP Android 16 hiện xếp layer theo thứ tự rút gọn:

| Window type | Layer tham khảo |
|---|---:|
| `TYPE_APPLICATION_OVERLAY` | 11 |
| `TYPE_STATUS_BAR` | 15 |
| `TYPE_NOTIFICATION_SHADE` | 17 |
| `TYPE_ACCESSIBILITY_OVERLAY` | 31 |
| `TYPE_SECURE_SYSTEM_OVERLAY` | 33 |

Layer là implementation detail, không phải compatibility promise cho mọi OEM/version.
Vì vậy kết luận đúng là: accessibility overlay **có khả năng che trực quan** status bar
trên Android phổ biến, nhưng vẫn cần device matrix và không được quảng cáo là sửa SystemUI.

## Hai runtime mode

```kotlin
enum class CapsuleDisplayMode {
    BELOW_SYSTEM_BAR,
    COVER_SYSTEM_BAR
}
```

| Mode | Window | Permission/special access | Trải nghiệm |
|---|---|---|---|
| `BELOW_SYSTEM_BAR` | `TYPE_APPLICATION_OVERLAY` | Display over other apps | Capsule nằm dưới status bar, fallback ít nhạy cảm |
| `COVER_SYSTEM_BAR` | `TYPE_ACCESSIBILITY_OVERLAY` | Accessibility service | Che trực quan status bar, gần reference hơn |

Không tự động đổi mode. User chọn rõ trong onboarding/editor và có thể quay về fallback
mà không mất theme/config.

## Accessibility service tối thiểu

Target component:

```text
StatusBarAccessibilityService
  ├─ AccessibilityCapsuleWindowBackend
  ├─ StatusCapsuleRenderer
  └─ StatusCapsuleRuntimeCoordinator
```

Service contract:

- khai báo `android.permission.BIND_ACCESSIBILITY_SERVICE`;
- `android:exported="true"` để Android có thể bind qua intent filter của accessibility;
- `android:isAccessibilityTool="false"` vì Cute Pet không phải disability-support tool;
- `android:canRetrieveWindowContent="false"`;
- không đọc accessibility node tree;
- không log package/window content;
- không gọi `dispatchGesture()`, `performGlobalAction()` hoặc automation API;
- chỉ tạo/remove/update một top-bounded `TYPE_ACCESSIBILITY_OVERLAY`;
- overlay là `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`;
- visual bounds chỉ bằng status region, không dùng full-screen transparent window.

API 22+ có thể add View bằng `WindowManager` với `TYPE_ACCESSIBILITY_OVERLAY`. API 34+
có thêm `attachAccessibilityOverlayToDisplay`, nhưng không dùng làm baseline vì min SDK 24
và backend View hiện tại đơn giản hơn.

## Runtime ownership

`TYPE_ACCESSIBILITY_OVERLAY` phải được tạo từ context/lifecycle của
`StatusBarAccessibilityService`; không chuyển window này vào foreground service của pet.

```text
StatusCapsuleRuntimeCoordinator
  ├─ BelowSystemBarBackend → OverlayHostService
  └─ CoverSystemBarBackend → StatusBarAccessibilityService
```

Cả hai backend đọc cùng `appliedConfig` và dùng cùng layout policy/renderer/asset resolver.
Coordinator bảo đảm chỉ một backend active:

1. sanitize applied config;
2. xác định display mode;
3. kiểm tra capability tương ứng;
4. stop backend cũ;
5. start/update backend mới;
6. nếu Accessibility bị tắt, remove accessibility window và hiển thị trạng thái
   `NEEDS_ACCESSIBILITY`; không âm thầm bật fallback khi user chưa đồng ý.

Pet tiếp tục dùng `OverlayHostService` và `SYSTEM_ALERT_WINDOW`. Accessibility mode của
capsule không thay thế overlay permission của pet.

## Permission and consent flow

```text
Apply COVER_SYSTEM_BAR
  → in-app prominent disclosure
  → user đồng ý riêng
  → ACTION_ACCESSIBILITY_SETTINGS
  → user bật Cute Pet service
  → app/service xác nhận enabled
  → persist mode + start accessibility backend
```

Disclosure phải xuất hiện ngay trước khi mở Settings và nói rõ:

- Cute Pet dùng Accessibility để đặt thanh trang trí lên status bar;
- service không đọc nội dung màn hình, không thực hiện thao tác thay user và không thu thập
  dữ liệu accessibility;
- user có thể tắt trong Accessibility Settings bất kỳ lúc nào;
- tắt quyền sẽ dừng status-cover mode;
- link Privacy Policy và nút đồng ý/từ chối độc lập.

Không gọi màn hình này là system permission dialog, không pre-check consent và không dùng
dark pattern buộc user bật.

## System-surface behavior

- Status bar gesture: overlay non-touchable để swipe-down vẫn hoạt động.
- Notification shade: accessibility layer có thể nằm trên shade; backend phải test và
  pause/hide best-effort khi SystemUI panel mở. Không dùng node inspection chỉ để đạt việc
  này; nếu detection không ổn định trên OEM, ghi nhận limitation.
- Keyguard/lock screen: mặc định hide bằng `KeyguardManager`; không hiển thị user theme trên
  lock screen ở MVP.
- Permission/biometric/system confirmation: không che vùng dialog; secure system overlays
  được hệ thống ưu tiên, nhưng vẫn phải device-test.
- Fullscreen/immersive app: status-cover mode giữ top bounds nếu OEM cho phép; có per-app
  pause/allowlist chỉ ở phase sau và không yêu cầu broad package visibility.
- Rotation/landscape: hide mặc định cho tới khi compact landscape layout được duyệt.

## Google Play gate

Google Play cho phép AccessibilityService ngoài disability tools nhưng yêu cầu declaration,
prominent disclosure, affirmative consent và review. App phải dùng API hẹp hơn nếu API đó
đáp ứng được use case.

### Red line: privacy indicators và system notifications

Accessibility API không được dùng để né Android security/privacy controls hoặc che
notifications một cách deceptive. Một status replacement full-width, opaque có thể che:

- camera/microphone privacy indicator;
- screen-recording/casting/safety indicator;
- notification icons hoặc cảnh báo SystemUI;
- nội dung top region của notification shade.

Không có public API ổn định để third-party app biết mọi privacy/system indicator trên mọi
Android version/OEM. Vì vậy **không được mặc định coi full-width opaque replacement là
Play-compliant** chỉ vì nó chạy kỹ thuật.

Release design phải chọn một biện pháp có thể chứng minh:

- capsule có vùng trong suốt/reserved system safety area đủ rõ;
- không phủ vị trí privacy indicator trên supported device matrix;
- tự hide trên system panel/sensitive surface bằng public signal đáng tin cậy; hoặc
- chỉ ship below-bar mode nếu không thể bảo toàn system indicators.

Internal prototype có thể kiểm chứng visual parity, nhưng production rollout vẫn bị chặn
cho tới khi policy review và device evidence xác nhận không che privacy controls.

Justification dự kiến:

```text
Core feature: user-requested visual customization of the system status region.
Why narrower API is insufficient: TYPE_APPLICATION_OVERLAY is layered below the native
status bar and cannot provide the explicitly selected cover-status-bar presentation.
Data access: no accessibility content is read, collected, stored, or shared.
Automation: none.
```

Release artifacts bắt buộc:

- Play Console Accessibility declaration;
- video từ app open → disclosure → consent/decline → Settings enable → feature chạy;
- Privacy Policy/Data Safety đồng bộ;
- service description đúng capability;
- review lại policy tại thời điểm submit;
- remote kill switch để ẩn `COVER_SYSTEM_BAR` nếu review/OEM regression xảy ra.

Không set `isAccessibilityTool=true`. Không tuyên bố chức năng hỗ trợ người khuyết tật nếu
đó không phải mục đích chính.

## Go/no-go trước implementation

Owner phải chọn một trong ba release scope:

1. chỉ `BELOW_SYSTEM_BAR`: ít permission/policy hơn nhưng không giống reference;
2. chỉ `COVER_SYSTEM_BAR`: parity cao, phụ thuộc Accessibility review;
3. dual mode: UX/fallback tốt nhất nhưng tăng implementation và test surface.

Khuyến nghị kiến trúc là giữ backend abstraction cho dual mode, nhưng chỉ ship
`COVER_SYSTEM_BAR` sau khi disclosure, policy declaration và device proof được duyệt.
