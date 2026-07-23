# Pet Settings and Session Policy

## Persistent contract

`PetSettingsRepository` expose một `StateFlow<PetPreferences>` trên DataStore. Settings
và pack selection ghi qua repository; Home/catalog/service không truy cập key storage
trực tiếp. `PetPreferences` materialize đúng ba `PetSlotPreferences`, còn `petCount` quyết
định bao nhiêu slot active. Service chụp snapshot lúc Start để session đang chạy không
đổi hành vi giữa chừng.

| Setting | Giá trị hợp lệ | Runtime |
|---|---|---|
| Pack key / slot | đúng 3 installed/built-in key | missing/invalid fallback Orange Cat |
| Count | 1–3; low-RAM 1–2 | một window/state machine mỗi pet |
| Size / slot | 75/100/125/150% | kết hợp `defaultScale`, clamp 64–196dp cho đúng pet |
| Speed / slot | 50–150%, bước 25% | locomotion/physics/expression scale từ profile đúng pet |
| Sound | on/off | reserved; pack v1 chưa hỗ trợ audio |
| Pet messages / slot | on/off | pet off không tạo speech director/window; pet khác không bị ảnh hưởng |
| Custom messages / slot | 0–30 câu, tối đa 80 code point/câu | danh sách rỗng dùng catalog có sẵn cho riêng pet đó |
| Interaction / slot | on/off | off chỉ thêm `FLAG_NOT_TOUCHABLE` cho window đúng pet |
| Position / slot | nullable x/y chuẩn hóa 0–1 | lưu khi Stop, restore/clamp theo bounds mới |

Legacy `pet_selected_pack_key` được materialize thành ba slot cùng giá trị khi
`pet_selected_pack_keys` chưa tồn tại hoặc chưa đủ record, sau đó mỗi slot được update
độc lập. Key legacy tiếp tục mirror slot 1 để migration tương thích. Remove từ UI shift
toàn bộ profile/position của slot sau lên trước và append một profile mặc định ở cuối.

Size/speed/messages/custom messages/interaction global cũ là migration fallback: khi key
slot mới chưa có, giá trị cũ được duplicate sang cả ba profile. Lần user chỉnh một pet,
repository ghi đủ ba record và chỉ mutate slot đích.

Reset position đặt record đúng slot về `null` và tăng revision của slot đó. Session chụp
ba revision lúc Start; Stop chỉ merge lại vị trí của slot có revision không đổi, nên
reset pet 2 không bị session cũ ghi đè và cũng không xóa vị trí pet 1/3.

Editor lời thoại nằm trong Customize Pet, hỗ trợ lưu, hủy và reset về câu có sẵn. Counter
hiển thị cả số câu và độ dài câu dài nhất; nút Save bị khóa khi vượt 30 câu hoặc 80
Unicode code point/câu nên emoji không bị tính hai lần. Repository chuẩn hóa khoảng
trắng, bỏ câu rỗng/trùng và persist dạng newline-delimited string.
Mỗi pet chọn từ catalog riêng; thay đổi áp dụng ở lần Start tiếp theo.

## Settings UX

- Settings hub chỉ hiển thị roster + trạng thái tóm tắt và app/support.
- Tap card mở `pet_customization/{slotIndex}`.
- Add mở Catalog ở `slotIndex == petCount`; chỉ khi Set/Import thành công mới tăng count.
- Remove có confirm, shift profile/position của slot sau lên trước, append một profile
  mặc định và invalidate position revision để session cũ không ghi đè thứ tự mới.
- Sound không xuất hiện trong UI cho đến khi schema/runtime có audio thật.

Speed là mức năng lượng di chuyển, không còn là hệ số tua đều cho mọi animation. `WALK`,
`RUN`, `CREEP`, climb và `TALK_WALK` scale frame timing đầy đủ; bounce/trip/jump/dragged
scale một nửa; idle, sit, wink, look, speech, Special và các pose cảm xúc chỉ scale một
phần tư. Scripted velocity vẫn scale đầy đủ để pet thật sự đi nhanh/chậm theo setting,
nhưng nét mặt và skill không chớp ở 150%.

## Performance/degradation

- Thiết bị thường: tối đa 3 pet, shared clock 30 FPS cho 1–2 pet và 24 FPS cho 3 pet.
- Low-RAM device: tối đa 2 pet và shared clock 24 FPS.
- Bitmap visual/cache được chia sẻ theo pack key; mỗi slot có engine/visual/speech director
  và input flag từ profile riêng nhưng không decode, parse DataStore hoặc tạo
  coroutine/thread trong frame loop.
- UI không cho vượt budget; repository vẫn sanitize dữ liệu cũ/corrupt trước khi service dùng.

Không có boot receiver hoặc auto-start. Runtime state không persist; sau process death/reboot user phải chủ động Start lại.
