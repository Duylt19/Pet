# Battery Status Capsule — Plan Index

> **PLANNED — NOT IMPLEMENTED**

Tài liệu này chuyển 14 screenshot tham chiếu thành một kế hoạch production-ready cho
Cute Pet. Tên sản phẩm dùng trong spec là **Battery Status Capsule**: một thanh trang trí
nhỏ nằm ngay dưới status bar hệ thống, không giả vờ thay thế status bar thật.

## Kết luận chính

- Android app thông thường chỉ tạo được `TYPE_APPLICATION_OVERLAY` nằm dưới các system
  window quan trọng như status bar. Feature không dùng Accessibility, API ẩn hoặc quyền
  hệ thống để che/thay status bar.
- Capsule là một overlay non-touchable, user chủ động bật/tắt, dùng chung foreground
  service host với pet để tránh hai notification/service chạy song song.
- Pin, charging, thời gian, ngày, airplane mode, ringer mode và loại kết nối có thể phản
  ánh dữ liệu thật bằng public API. Một số thành phần trong ảnh chỉ là trang trí hoặc
  không có API ổn định trên toàn bộ min SDK 24; chúng phải có fallback trung thực.
- Tài sản trong screenshot chỉ là reference về capability/layout. Không copy logo,
  character, icon pack, background hoặc quảng cáo của app khác.

## Bộ tài liệu

1. [Phân tích screenshot](01_SCREENSHOT_ANALYSIS.md)
2. [Product và UX specification](02_PRODUCT_UX_SPEC.md)
3. [Khả thi trên Android và research](03_PLATFORM_FEASIBILITY.md)
4. [Architecture và overlay runtime](04_ARCHITECTURE_RUNTIME.md)
5. [Data, catalog và asset contract](05_DATA_ASSET_CONTRACT.md)
6. [UI, navigation và state contract](06_UI_NAVIGATION_STATE.md)
7. [Monetization, analytics và policy](07_MONETIZATION_ANALYTICS_POLICY.md)
8. [Test và release plan](08_TEST_RELEASE_PLAN.md)
9. [Implementation phases](09_IMPLEMENTATION_PHASES.md)

## Source hiện tại ảnh hưởng tới kế hoạch

- `HomeScreen` đã có bottom navigation ba item và Settings đã có lối riêng ở header.
  Battery sẽ thay item Settings ở bottom navigation; Settings vẫn mở từ header.
- `PetOverlayService` đang là `specialUse` FGS, `START_NOT_STICKY`, có notification Stop,
  screen-off suspension và live DataStore updates. Phase runtime phải tổng quát hóa thành
  một overlay host, không tạo thêm FGS độc lập.
- App đã có overlay/notification permissions, Hilt, DataStore, ads/rewarded/premium,
  analytics và remote catalog pattern để tái sử dụng qua boundary phù hợp.

## Điều kiện trước khi code

- Owner duyệt tên/position thực tế của capsule và chấp nhận việc system status bar vẫn
  tồn tại phía trên.
- Có asset do project sở hữu hoặc được cấp phép; không dùng asset trong screenshot.
- Chốt placement ads mới và entitlement asset với owner trước phase monetization.
- Có device matrix ít nhất API 24/28/31/33/35/36, gồm cutout và gesture/3-button navigation.
