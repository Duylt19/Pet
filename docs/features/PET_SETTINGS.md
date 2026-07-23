# Pet Settings and Session Policy

## Persistent contract

`PetSettingsRepository` expose một `StateFlow<PetPreferences>` trên DataStore. Settings và pack selection ghi qua repository; Home/catalog/service không truy cập key storage trực tiếp. Service chụp một snapshot lúc Start để session đang chạy không đổi hành vi giữa chừng.

| Setting | Giá trị hợp lệ | Runtime |
|---|---|---|
| Pack keys | đúng 3 installed/built-in key theo slot | slot legacy còn thiếu được khởi tạo từ slot 1; missing/invalid fallback Orange Cat |
| Count | 1–3; low-RAM 1–2 | một window/state machine mỗi pet |
| Size | 75/100/125/150% | kết hợp `defaultScale`, clamp 64–196dp |
| Speed | 50–150%, bước 25% | velocity và locomotion nhận toàn bộ mức speed; physics reaction nhận 50%, expression/skill chỉ nhận 25% ảnh hưởng lên nhịp frame |
| Sound | on/off | reserved; pack v1 chưa hỗ trợ audio |
| Pet messages | on/off | bật/tắt speech director và transient bubble window |
| Custom messages | 0–30 câu, tối đa 80 code point/câu | mỗi câu một dòng; danh sách rỗng dùng catalog có sẵn |
| Interaction | on/off | off thêm `FLAG_NOT_TOUCHABLE` |
| Position | x/y chuẩn hóa 0–1 | lưu khi Stop, restore/clamp theo bounds mới |

Legacy `pet_selected_pack_key` được materialize thành ba slot cùng giá trị khi
`pet_selected_pack_keys` chưa tồn tại hoặc chưa đủ record, sau đó mỗi slot được update
độc lập. Key legacy tiếp tục mirror slot 1 để migration tương thích. Giảm count không xóa
selection của slot ẩn; tăng lại count sẽ restore character đã chọn trước đó.

Reset position xóa toàn bộ tọa độ và tăng `pet_position_reset_revision`. Session chụp
revision lúc Start; thao tác Stop chỉ persist vị trí khi revision chưa đổi, nên Reset từ
Settings trong lúc overlay đang chạy không bị session cũ ghi đè.

Editor lời thoại nằm trong Settings, hỗ trợ lưu, hủy và reset về câu có sẵn. Counter
hiển thị cả số câu và độ dài câu dài nhất; nút Save bị khóa khi vượt 30 câu hoặc 80
Unicode code point/câu nên emoji không bị tính hai lần. Repository chuẩn hóa khoảng
trắng, bỏ câu rỗng/trùng và persist dạng newline-delimited string.
Pet chọn ngẫu nhiên nhưng không lặp ngay câu vừa nói; thay đổi áp dụng ở lần Start tiếp
theo theo cùng snapshot policy với các setting runtime khác.

Speed là mức năng lượng di chuyển, không còn là hệ số tua đều cho mọi animation. `WALK`,
`RUN`, `CREEP`, climb và `TALK_WALK` scale frame timing đầy đủ; bounce/trip/jump/dragged
scale một nửa; idle, sit, wink, look, speech, Special và các pose cảm xúc chỉ scale một
phần tư. Scripted velocity vẫn scale đầy đủ để pet thật sự đi nhanh/chậm theo setting,
nhưng nét mặt và skill không chớp ở 150%.

## Performance/degradation

- Thiết bị thường: tối đa 3 pet, shared clock 30 FPS cho 1–2 pet và 24 FPS cho 3 pet.
- Low-RAM device: tối đa 2 pet và shared clock 24 FPS.
- Bitmap visual/cache được chia sẻ theo pack key; mỗi slot có engine/visual riêng nhưng không decode, parse DataStore hoặc tạo coroutine/thread trong frame loop.
- UI không cho vượt budget; repository vẫn sanitize dữ liệu cũ/corrupt trước khi service dùng.

Không có boot receiver hoặc auto-start. Runtime state không persist; sau process death/reboot user phải chủ động Start lại.
