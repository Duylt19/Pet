# 06 — UI Design System Contract

Base giữ infrastructure/theme và component pattern. Product screens từ Home trở đi dùng
visual system Cute Pet; onboarding và Premium vẫn giữ UI hiện tại cho tới task update riêng.

## Cute Pet product direction

- Discover Home dùng nền trắng/gradient pastel, primary pink `#FB3675`, card trắng và
  typography Roboto theo Figma node `8015:1035`. My Pet mode dùng nền xanh-trắng nhẹ,
  primary teal và segmented control rõ selection; Catalog/Settings hiện vẫn dùng warm
  cream/purple của Cute Pet.
- Pet thumbnail thật là visual chính; icon notification chỉ là fallback khi pack chưa có ảnh.
- Corner radius lớn 16–24 sdp, card rõ hierarchy nhưng ít chrome và không dùng dark utility
  dashboard cho product screens.
- Discover là landing tổng hợp; My Pet là pet room và session control; Catalog ưu tiên
  discovery bằng grid; Settings là pet family roster; Customize biểu diễn một hồ sơ pet độc lập.
- Shared primitives nằm ở `ui/component/CutePetComponents.kt`; component dark cũ không được
  dùng cho product screen mới nếu không có lý do tương thích.

Các màn Splash, Language, Intro, Permission và Premium cố ý không đổi trong refresh này.

Settings dùng cấu trúc pet-first:

- `settings`: roster active pet, Add pet và App & support;
- `pet_customization/{slotIndex}`: identity/change character, Appearance & movement,
  Interaction & speech, reset position và remove của đúng pet;
- không hiển thị Sound khi pack schema v1 chưa có audio;
- Add mở Catalog ở slot trống nhưng chỉ commit `petCount` sau Set/Import thành công.

Discover Home contract:

- route `home` là root sau onboarding và hiển thị dữ liệu thật từ owner/battery catalog;
- toggle chính điều khiển `BatteryStatusConfig.enabled`, có disclosure và Accessibility gate;
- Home shell có bốn tab Discover/Battery/Pet Store/Mine. `HomeBottomNavigation` cố định
  trên bottom banner hiện có; từng screen không tự tạo lại bottom chrome;
- Discover và Pet Store dùng chung `HomeHeader` và `HomeEnableCard`: header `43sdp`, search
  `25sdp`, enable card `37sdp`, switch `34×18sdp`. Không copy component rồi đổi metric riêng;
- hero banner và promo creative trong content là presentational slot theo Figma, không gọi ads SDK;
- Battery Themes dùng favorite state thật; Trending hiện dùng thứ tự catalog cho tới khi
  server có ranking riêng.

Pet Store visual contract:

- Pet/Food selector dùng bốn image-fill state riêng từ Figma (`selected`/`unselected`);
  đây là raster artwork nhiều màu nên lưu PNG @3x trong `drawable-nodpi`, không thay bằng
  emoji hoặc icon navigation;
- pet card giữ tỷ lệ `104/142`, image area `104/90`, thumbnail theo tỷ lệ item và crown
  premium 20px tại top-end;
- reward sheet dùng Roboto Medium cho title, Roboto Regular cho action; gradient và stroke
  nút là `#C95DFF → #FB54BB`. Selected Pet Store, video và tape giữ asset vector gốc.

My Pet mode contract:

- `Pet Swarm` và `Mixed Mode` là segmented control loại trừ nhau;
- global switch điều khiển foreground overlay, không dùng để thay pet selection;
- Mixed hiển thị lưới 3 cột × tối đa 4 hàng cho 12 slot; pet đã cấu hình giữ card hiện
  tại, chỉ slot trống kế tiếp có thể thao tác để roster luôn liên tục;
- slot Mixed 1–3 miễn phí; slot 4–12 có trạng thái khóa và Catalog Rewarded gate. Earned
  callback mở đúng slot hiện tại khi ad hiển thị được; unavailable tiếp tục flow, còn
  dismiss sớm dừng lại. Premium bypass gate;
- Mixed dùng icon mắt trực tiếp trên từng card để hiện/ẩn ngay khi overlay đang chạy;
- không cho ẩn pet Mixed cuối cùng, vì global switch đã đảm nhiệm trường hợp không hiện pet;
- Swarm locked hiển thị CTA Rewarded và Premium; Premium bypass Rewarded;
- Swarm unlocked hiển thị một pack, stepper count và Change/Remove.
- Tap Swarm card mở `swarm_customization`; screen riêng giữ teal hierarchy của Home,
  identity card ở đầu, setup/movement sections và CTA Done cố định. Mọi slider/toggle
  persist và cập nhật runtime ngay, Done chỉ đóng màn chứ không phải bước commit.

Mỗi card phải cho user thấy nhanh character, size, speed và trạng thái tương tác; option
pet không được lặp ở app-wide Settings hoặc ghi vào global state.

## Resource rules

- User-facing text: `strings.xml`, key `<feature>_<purpose>`.
- Color: `colors.xml`, key `colors_<HEX>` trừ semantic theme token có chủ đích.
- Drawable: `ic_` cho icon, `ic_logo_` cho logo vector, `img_` cho bitmap.
- Asset từ Figma phải ưu tiên SVG và convert thành Android `VectorDrawable`; không export
  icon đơn giản thành PNG. Chỉ dùng bitmap khi node có image fill/raster, SVG không được hỗ
  trợ hoặc quá phức tạp để render ổn định trên Android; bitmap fallback phải export `PNG @3x`.
- Bitmap Figma đặt trong `drawable-nodpi` và luôn có kích thước hiển thị rõ trong Compose để
  Android không dùng kích thước pixel gốc làm layout size.
- Font: tái sử dụng `res/font` và theme; không khai báo trùng trong từng component.
- Không hardcode string/hex color trong Composable.

## Native ad palette

- Native Ads dùng light surface theo Figma node `8047:2973`: nền `#FEFEFE`, viền và
  loading placeholder `#E6E6E6`, nội dung `#000000`.
- Badge `Ad` và CTA dùng gradient ngang `#FF5D7D` → `#FB54BB`; CTA giữ chữ trắng.
- Tất cả Native Ad template trong module `:ads` dùng chung palette này; thay đổi visual
  không được làm thay đổi placement, loading callback hoặc premium/ad-free policy.

## Sizing

Design hiện dùng SDP/SSP. Phải phân loại kích thước trước khi mapping:

- Kích thước cục bộ/cố định như padding, spacing, icon/image, height, radius và typography: `Android sdp/ssp ≈ Figma px ÷ 1.3`.
- Kích thước phụ thuộc viewport như chiều rộng dialog, bottom sheet hoặc card căn theo screen/frame: giữ tỷ lệ Figma `nodeWidth / frameWidth` và dùng `fillMaxWidth(fraction)`. Chỉ dùng `fillMaxHeight(fraction)` khi design xác định rõ tỷ lệ chiều cao theo viewport.

Dùng `dimensionResource` từ `com.intuit.sdp`/`com.intuit.ssp` cho nhóm kích thước cục bộ, làm tròn về resource gần nhất và đối chiếu screenshot. Ví dụ Rate dialog rộng `312px` trong frame `360px` dùng `fillMaxWidth(312f / 360f)`; dialog cảm ơn rộng `320px` dùng `fillMaxWidth(320f / 360f)`, không đổi thành `_240sdp`/`_246sdp`.

## Component hierarchy

- Screen: collect state, effect và wiring action.
- Section/component: stateless nếu có thể.
- Shared component chỉ đặt ở `ui/component` khi có ít nhất hai consumer hoặc có contract reusable rõ.
- Shared Home chrome (`HomeHeader`, `HomeEnableCard`, `HomeBottomNavigation`) do shell/feature
  gọi theo đúng ownership: screen sở hữu header/card, `AppNavGraph` sở hữu bottom navigation.
- Feature-only component giữ cạnh feature để tránh global component folder phình to.

## Modifier và interaction

```text
size → shadow → clip → background → border → clickable → padding
```

- `clip` trước `clickable` để ripple đúng shape.
- Không bọc icon trong Box chỉ để tạo padding nội bộ nếu drawable frame đã có viewBox chuẩn.
- Action icon có content description; decorative image dùng `null`.
- Touch target và visual size phải được cân bằng; khi cần pixel-match Figma vẫn phải đảm bảo accessibility.

## Figma implementation

1. Lấy screenshot và design context.
2. Phân tích hierarchy/alignment/spacing/color/type/radius/layer order.
3. So sánh với code hiện tại.
4. Mapping token vào resource.
5. Implement theo state contract, không nhét logic vào UI.
6. Preview/screenshot và compile verify.

Dialog xin quyền dùng custom Compose card theo Figma thay vì `AlertDialog` mặc định. Golden
image của component được render host-side từ `screenshotTest`, không cần khởi động AVD.
