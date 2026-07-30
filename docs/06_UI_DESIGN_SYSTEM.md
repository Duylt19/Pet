# 06 — UI Design System Contract

Base giữ infrastructure/theme và component pattern. Product screens từ Home trở đi dùng
visual system Cute Pet; onboarding và Premium vẫn giữ UI hiện tại cho tới task update riêng.

## Cute Pet product direction

- Home mode dùng nền xanh-trắng nhẹ, primary teal và segmented control rõ selection;
  Catalog/Settings hiện vẫn dùng warm cream/purple của Cute Pet.
- Pet thumbnail thật là visual chính; icon notification chỉ là fallback khi pack chưa có ảnh.
- Corner radius lớn 16–24 sdp, card rõ hierarchy nhưng ít chrome và không dùng dark utility
  dashboard cho product screens.
- Home là pet room và session control; Catalog ưu tiên discovery bằng grid; Settings là pet
  family roster; Customize biểu diễn một hồ sơ pet độc lập.
- Shared primitives nằm ở `ui/component/CutePetComponents.kt`; component dark cũ không được
  dùng cho product screen mới nếu không có lý do tương thích.

Các màn Splash, Language, Intro, Permission và Premium cố ý không đổi trong refresh này.

Settings dùng cấu trúc pet-first:

- `settings`: roster active pet, Add pet và App & support;
- `pet_customization/{slotIndex}`: identity/change character, Appearance & movement,
  Interaction & speech, reset position và remove của đúng pet;
- không hiển thị Sound khi pack schema v1 chưa có audio;
- Add mở Catalog ở slot trống nhưng chỉ commit `petCount` sau Set/Import thành công.

Home mode contract:

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
- Font: tái sử dụng `res/font` và theme; không khai báo trùng trong từng component.
- Không hardcode string/hex color trong Composable.

## Sizing

Design hiện dùng SDP/SSP. Mapping Figma mặc định:

```text
Android sdp/ssp ≈ Figma px ÷ 1.3
```

Dùng `dimensionResource` từ `com.intuit.sdp`/`com.intuit.ssp`; làm tròn về resource gần nhất và đối chiếu screenshot.

## Component hierarchy

- Screen: collect state, effect và wiring action.
- Section/component: stateless nếu có thể.
- Shared component chỉ đặt ở `ui/component` khi có ít nhất hai consumer hoặc có contract reusable rõ.
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
