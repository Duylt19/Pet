# Pet Behavior V2 — action routines và showcase

## Kết luận research

Snapshot owner-authorized có 1.026 ZIP pet. Contract phổ biến vẫn là 46 ảnh
`shime1.png`–`shime46.png`, nhưng các ảnh không phải 46 trạng thái độc lập. Chúng được
ghép thành action bằng một timeline có frame duration và velocity.

Kết quả audit trực tiếp snapshot:

- 1.024 pack có đủ frame 1–3 để đi/chạy;
- 1.025 pack có đủ frame leo tường 12–14 và leo trần 23–25;
- 1.025 pack có ít nhất một frame Special trong 38–41;
- 1.018 pack có ít nhất một frame Special 2 trong 42–46;
- 1.003 pack có trọn bộ 1–46;
- chỉ pack 663 có frame mở rộng đến 56, không đủ metadata để gán nghĩa tổng quát.

`custompet.json` là một feature khác: 18 body template, mỗi template có 9 ảnh,
`faceSizeRatio` và 9 `headPositions` để ghép khuôn mặt. Đây không phải 9 bộ quần áo và
không được trộn vào runtime Shimeji 46-frame khi chưa có flow chọn/crop ảnh người dùng.

## Đối chiếu mã nguồn mở

Shimeji-ee tách rõ hai lớp:

- `actions.xml` mô tả primitive `Stay`, `Move`, frame/velocity và action phức hợp
  `Sequence`/`Select`;
- `behaviors.xml` gắn frequency và condition theo floor, wall, ceiling;
- action chạy nhanh, leo lên/xuống, crawl, sit/look và các chuỗi walk-then-sit được xem
  là các behavior khác nhau, không chỉ đổi tốc độ timer ngẫu nhiên.

Nguồn tham khảo:

- https://github.com/gil/shimeji-ee/blob/master/conf/actions.xml
- https://github.com/gil/shimeji-ee/blob/master/conf/behaviors.xml
- https://github.com/gil/shimeji-ee/blob/master/readme.txt
- https://github.com/gil/shimeji-ee/blob/master/licence.txt

`wl_shimeji` được dùng để xác nhận state-machine/condition vẫn là hướng triển khai hiện
đại, nhưng repository dùng GPL-2.0 nên project không sao chép code từ đó:
https://github.com/CluelessCatBurger/wl_shimeji

## Contract đã triển khai

- `RUN`: tái sử dụng đúng frame đi 1–3 với cadence và vận tốc cao hơn.
- `CLIMB_DOWN`: đảo velocity của chuỗi frame leo tường; pet có thể đổi ý giữa đường và
  leo xuống, hoặc bám mép trần rồi đi xuống thay vì luôn rơi.
- Routine queue: các action một lần có thể nối tiếp tự nhiên, ví dụ
  `SIT → WINK`, `TRIP → SIT`, `SPECIAL → SPECIAL_2 → WINK`.
- Single tap: phản ứng chạm của pack rồi nối một cử chỉ thân thiện.
- Double tap: chạy showcase `SPECIAL → SPECIAL_2 → WINK → LOOK_UP`, tự bỏ qua action
  mà pack không hỗ trợ.
- Partial Special: importer revision 3 giữ các frame Special thật sự có trong ZIP thay
  vì vô hiệu hóa cả action chỉ vì thiếu một frame. Điều này cho phép pack như Levi chỉ
  có frame 40 vẫn biểu diễn nội dung đặc biệt.
- Sprite motion polish: squash/stretch/lean rất nhẹ quanh bottom anchor; không thay đổi
  tọa độ physics hoặc inset-safe bounds.
- Revision 4 giữ sequence desktop `WalkWithIe` frame 34–36 trong raw manifest. Runtime
  tách frame 34 thành `TALK` đứng yên và cả sequence thành `TALK_WALK` di chuyển chậm;
  chữ được vẽ bằng bubble riêng và được pace bởi speech director. Xem
  [`PET_SPEECH.md`](PET_SPEECH.md).

## Giới hạn có chủ đích

- Frame 47–56 không được suy đoán thành action vì chỉ một pack có và không kèm config.
- Custom face-pet cần phase riêng gồm crop/mask/composite/cache và consent cho ảnh người
  dùng.
- Tương tác với cửa sổ kiểu desktop Shimeji-ee không phù hợp Android overlay hiện tại;
  runtime chỉ dùng các condition floor/wall/ceiling mà hệ điều hành cung cấp ổn định.

Behavior combo và tương tác giữa nhiều pet được mở rộng ở
[`PET_BEHAVIOR_V3_COMBOS.md`](PET_BEHAVIOR_V3_COMBOS.md).
