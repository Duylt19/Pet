# 06 — UI Design System Contract

Base giữ infrastructure/theme và component pattern. Home/Permission hiện là functional MVP cho pet overlay, chưa phải visual design final từ Figma.

Settings dùng cấu trúc pet-first:

- `settings`: roster active pet, Add pet và App & support;
- `pet_customization/{slotIndex}`: identity/change character, Appearance & movement,
  Interaction & speech, reset position và remove của đúng pet;
- không hiển thị Sound khi pack schema v1 chưa có audio;
- Add mở Catalog ở slot trống nhưng chỉ commit `petCount` sau Set/Import thành công.

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
