# 03 — Android Platform Feasibility

> **REFERENCE RESEARCH — Accessibility backend đã có ở debug; release gate vẫn áp dụng**

## Research sources

Nguồn ưu tiên là Android/Google Play documentation hiện hành:

- [`TYPE_APPLICATION_OVERLAY`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [`TYPE_ACCESSIBILITY_OVERLAY`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY)
- [`AccessibilityService`](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [AOSP window layer policy](https://android.googlesource.com/platform/frameworks/base/+/android16-release/services/core/java/com/android/server/policy/WindowManagerPolicy.java)
- [Google Play AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Prominent disclosure and consent](https://support.google.com/googleplay/android-developer/answer/11150561)
- [Foreground service types — special use](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)
- [Declare foreground services](https://developer.android.com/develop/background-work/services/fgs/declare)
- [Battery level and charging state](https://developer.android.com/training/monitoring-device-state/battery-monitoring)
- [`ACTION_TIME_TICK`](https://developer.android.com/reference/android/content/Intent#ACTION_TIME_TICK)
- [`ConnectivityManager`](https://developer.android.com/reference/android/net/ConnectivityManager)
- [`TetheringManager`](https://developer.android.com/reference/android/net/TetheringManager)
- [Display cutouts](https://developer.android.com/develop/ui/views/layout/display-cutout)
- [Google Play Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/16273414)
- [Play Console foreground-service declaration](https://support.google.com/googleplay/android-developer/answer/16965181)

## Standard overlay feasibility

`TYPE_APPLICATION_OVERLAY` hiển thị trên application windows nhưng dưới critical system
windows như status bar/IME. Do đó:

- capsule không thể che status bar bằng window type này;
- API 26+ dùng `TYPE_APPLICATION_OVERLAY`; API 24–25 dùng legacy `TYPE_PHONE` theo helper
  overlay hiện tại và vẫn yêu cầu special access;
- window đặt tại top inset hoặc ngay dưới system status bar;
- trong fullscreen app, position có thể sát top display nhưng vẫn không có quyền SystemUI;
- hệ thống có thể đổi position/size/visibility để giảm clutter;
- phải xử lý cutout/rotation/OEM variance bằng runtime metrics, không hardcode status bar height.

## Touch-through

Capsule là `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`. Từ Android 12, untrusted overlay có
quy tắc obscuring opacity đối với touch pass-through. Runtime phải:

- giữ window chỉ cao bằng capsule;
- lấy maximum obscuring opacity từ `InputManager` khi API hỗ trợ;
- dùng `LayoutParams.alpha` không vượt ngưỡng phù hợp;
- test combined opacity khi pet window đi qua capsule;
- không tạo full-screen transparent overlay.

Nếu OEM vẫn chặn touch phía sau, below-bar backend phải giảm height/opacity hoặc cung cấp
quick Stop. Accessibility backend là một product mode độc lập có disclosure/policy riêng,
không phải workaround âm thầm cho touch restriction.

## Accessibility overlay feasibility

`TYPE_ACCESSIBILITY_OVERLAY` chỉ được tạo bởi một enabled `AccessibilityService`. Trong
AOSP Android 16, type này ở layer 31, cao hơn status bar layer 15 và notification shade
layer 17. Vì vậy nó có thể che trực quan status bar và render capsule thay thế về mặt hình
ảnh. Đây không phải SDK guarantee về exact layer trên mọi OEM/version.

Giới hạn:

- SystemUI thật không bị sửa hoặc tắt;
- accessibility overlay phải có lifecycle/service riêng;
- non-touchable top window để gesture tới SystemUI;
- có thể nằm trên notification shade nên cần best-effort pause/hide + OEM test;
- full-width opaque bar có thể che privacy/system indicators và là Play-policy risk cao;
- hide trên keyguard/lock screen trong MVP;
- service disable/revoke phải remove window ngay;
- không dùng screen/node retrieval hoặc automation.

Chi tiết implementation/policy:
[Accessibility status-cover mode](10_ACCESSIBILITY_STATUS_COVER.md).

## Device data capability matrix

| Dữ liệu | Public API | Permission | Độ tin cậy/contract |
|---|---|---|---|
| Battery level | Sticky `ACTION_BATTERY_CHANGED` extras | None | Real 0–100 |
| Charging/full + plug type | `BatteryManager.EXTRA_STATUS/PLUGGED` | None | Real |
| Time/date/timezone | `java.time`, dynamic time/timezone receiver | None | Real |
| Minute tick | dynamic `ACTION_TIME_TICK` | None | Chỉ register runtime, không manifest |
| Airplane mode | system broadcast + readable global state | None | Real on/off |
| Ringer mode | `AudioManager.ringerMode` + callback/broadcast | None | Real normal/vibrate/silent |
| Default transport | `ConnectivityManager.NetworkCallback` | `ACCESS_NETWORK_STATE` | Real Wi‑Fi/cellular/other |
| Internet validated | `NetworkCapabilities` | `ACCESS_NETWORK_STATE` | Real capability, not speed |
| Exact Wi‑Fi strength | Transport info varies/redacted | Often location/nearby constraints | Không yêu cầu trong MVP |
| Exact cellular signal | Telephony callback | Device/API/permission-sensitive | Không yêu cầu trong MVP |
| Cellular generation | `TelephonyDisplayInfo` | API/target/OEM-sensitive | Optional phase; only 2G–5G |
| Hotspot active | `TetheringManager` callback | Public complete contract từ API 36 | Manual/decorative fallback trước API 36 |
| Decorative animation | Validated local asset | None | Không phải device data |
| Emotion | None | None | Decorative only |

## Permission policy

Below-bar MVP chỉ thêm normal permission `ACCESS_NETWORK_STATE`; repository đã có:

- `SYSTEM_ALERT_WINDOW`;
- `FOREGROUND_SERVICE`;
- `FOREGROUND_SERVICE_SPECIAL_USE`;
- `POST_NOTIFICATIONS`.

Không thêm:

- storage permission;
- location;
- `NEARBY_WIFI_DEVICES`;
- `READ_PHONE_STATE`;
- notification listener;
- usage access.

Cover mode thêm một service được bảo vệ bởi `BIND_ACCESSIBILITY_SERVICE`; đây không phải
runtime permission thông thường. User phải bật thủ công trong Accessibility Settings sau
prominent disclosure và affirmative consent. Service khai báo `isAccessibilityTool=false`
và `canRetrieveWindowContent=false`.

Nếu phase sau muốn exact signal/network generation, đó là một product/privacy decision
riêng với permission UX và Play review; không lén thêm vào implementation cơ bản.

## Foreground service

Capsule là feature user-visible, user-started và phải tồn tại khi user rời app. `specialUse`
là type phù hợp khi không có type chuẩn khác, nhưng:

- use case phải được ghi rõ trong manifest property;
- Play Console cần description, user impact và demo video;
- notification phải diễn tả pet/capsule nào đang chạy và có Stop;
- service chỉ chạy khi ít nhất một overlay feature enabled;
- `START_NOT_STICKY`; không tự resurrect hoặc boot-start MVP.

App hiện đã dùng `specialUse` cho pet. Kế hoạch hợp nhất thành một shared overlay host giúp
tránh hai FGS/notification, nhưng manifest explanation và Play declaration phải cập nhật để
bao phủ cả pet lẫn battery capsule.

## Power/performance

- Battery/time/network updates phải event-driven.
- Chỉ animated asset cần clock; target tối đa 12–15 FPS.
- Pause animation và invalidate khi screen off.
- Không dùng WorkManager/timer keep-alive.
- Không decode bitmap, parse JSON hoặc read DataStore trong draw loop.
- Khi battery thấp và không charging, animation có thể hạ FPS hoặc tắt theo policy.

## Known limitations cần hiển thị trung thực

- Below-bar mode: native system status bar vẫn hiện.
- Cover mode: native status bar bị che trực quan nhưng vẫn tồn tại/hoạt động phía dưới.
- Cover mode không được ship nếu che camera/microphone privacy indicator, notification hoặc
  system safety UI trên supported device matrix.
- Capsule có thể bị system/OEM reposition, ẩn hoặc render khác layer dự kiến.
- Exact cellular/Wi‑Fi bars không bảo đảm nếu không xin sensitive permission.
- Hotspot real state chỉ có contract public đầy đủ từ API 36; device cũ dùng manual option.
- Lock screen, desktop/windowed mode, picture-in-picture và fullscreen game cần device
  verification; không hứa capsule hiển thị trên mọi surface.
