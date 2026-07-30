# Battery Status Capsule — Plan Index

> **REFERENCE PLAN — PRODUCT IMPLEMENTATION COMPLETE, EXTERNAL RELEASE GATES PENDING**

Current source contract và trạng thái thật:
[`../../features/BATTERY_STATUS.md`](../../features/BATTERY_STATUS.md).

Tài liệu này chuyển 14 screenshot tham chiếu thành một kế hoạch production-ready cho
Cute Pet. Tên sản phẩm dùng trong spec là **Battery Status Capsule**: một thanh trang trí
che trực quan status bar bằng Accessibility. Sau quyết định sản phẩm ngày 2026-07-30,
source ship luồng cover-only; giá trị below-bar legacy được migrate. App không sửa
SystemUI thật.

## Kết luận chính

- `TYPE_APPLICATION_OVERLAY` chỉ nằm dưới status bar. Quan sát app reference yêu cầu
  Accessibility phù hợp với cơ chế `TYPE_ACCESSIBILITY_OVERLAY`, có layer cao hơn và có
  thể che trực quan status bar bằng một thanh custom.
- Accessibility cover mode có disclosure và affirmative consent; Play declaration và
  device proof vẫn là release gate. Service không đọc screen content hoặc tự động thao tác.
- Full-width opaque cover là high-risk vì có thể che privacy/system indicators. Production
  chỉ ship phần visual cover chứng minh không làm khuất camera/microphone indicator,
  notification hoặc system warning.
- Capsule luôn non-touchable và user chủ động bật/tắt. Pet/application-overlay fallback
  dùng foreground host; accessibility window thuộc lifecycle của AccessibilityService.
- Pin, charging, thời gian, ngày, airplane mode, ringer mode và loại kết nối có thể phản
  ánh dữ liệu thật bằng public API. Một số thành phần trong ảnh chỉ là trang trí hoặc
  không có API ổn định trên toàn bộ min SDK 24; chúng phải có fallback trung thực.
- Một catalog theme là cặp mặc định gồm pet/emoji + pin. Chọn theme khởi tạo cả hai ID;
  editor có thể chọn lại từng phần độc lập. Renderer đặt hai asset cùng trailing anchor,
  vẽ pin trước và pet chồng lên trên. Draft preview chỉ xuất hiện trên Accessibility
  status bar, không tạo capsule giả trong nội dung editor.
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
10. [Accessibility status-cover mode](10_ACCESSIBILITY_STATUS_COVER.md)
11. [Reference flow redesign](11_REFERENCE_FLOW_REDESIGN.md)
12. [Release readiness ledger](12_RELEASE_READINESS.md)

## Source hiện tại ảnh hưởng tới kế hoạch

- `HomeScreen` đã có bottom navigation ba item và Settings đã có lối riêng ở header.
  Battery sẽ thay item Settings ở bottom navigation; Settings vẫn mở từ header.
- `PetOverlayService` đang là `specialUse` FGS, `START_NOT_STICKY`, có notification Stop,
  screen-off suspension và live DataStore updates. Phase runtime tổng quát hóa host này
  cho pet/below-bar backend; cover backend thuộc AccessibilityService và không tạo FGS
  độc lập.
- App đã có overlay/notification permissions, Hilt, DataStore, ads/rewarded/premium,
  analytics và remote catalog pattern để tái sử dụng qua boundary phù hợp.

## Điều kiện còn lại trước khi release completion

- Release scope đã chốt: Accessibility cover-only.
- Nếu ship cover mode, duyệt disclosure/consent, Play declaration và video review trước
  khi implementation được coi là release-ready.
- Có asset do project sở hữu hoặc được cấp phép; không dùng asset trong screenshot.
- Chốt placement ads mới và entitlement asset với owner trước phase monetization.
- Có device matrix ít nhất API 24/28/31/33/35/36, gồm cutout và gesture/3-button navigation.
