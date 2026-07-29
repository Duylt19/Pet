# Pet Settings and Session Policy

## Persistent contract

`PetSettingsRepository` expose một `StateFlow<PetPreferences>` trên DataStore. Settings
và pack selection ghi qua repository; Home/catalog/service không truy cập key storage
trực tiếp. Hai mode `MIXED`/`SWARM` loại trừ nhau nhưng giữ state riêng. Service observe
toàn bộ profile; đổi mode, visibility, count và pack đều cập nhật session đang chạy.

| Setting | Giá trị hợp lệ | Runtime |
|---|---|---|
| Pack key / slot | đúng 3 installed/built-in key | missing/invalid fallback Orange Cat |
| Count | 1–3; low-RAM 1–2 | một window/state machine mỗi pet |
| Mixed visibility / slot | on/off; giữ tối thiểu một slot visible | hide/show đúng window và speech ngay |
| Swarm pack | một installed/built-in key riêng | lặp cùng pack cho mọi instance |
| Swarm count | 1–12; low-RAM tối đa 6 | rebuild controller ngay trong session |
| Size / slot | 50–150%, bước 10% | 100% = 84dp trước `defaultScale`, clamp 48–144dp |
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

Reset position đặt record đúng slot về `null`, tăng revision của slot đó và đưa instance
đang chạy về điểm mặc định ngay. Session cập nhật revision sau khi reset; Stop chỉ merge
vị trí theo revision hiện hành nên reset pet 2 không xóa hoặc ghi đè vị trí pet 1/3.

Editor lời thoại nằm trong Customize Pet, hỗ trợ lưu, hủy và reset về câu có sẵn. Counter
hiển thị cả số câu và độ dài câu dài nhất; nút Save bị khóa khi vượt 30 câu hoặc 80
Unicode code point/câu nên emoji không bị tính hai lần. Repository chuẩn hóa khoảng
trắng, bỏ câu rỗng/trùng và persist dạng newline-delimited string.
Mỗi pet chọn từ catalog riêng. Size resize window ngay khi user bấm stepper hoặc kéo qua
một nấc slider; runtime giữ tâm/chân trên
sàn, giữ attachment ở tường/trần và clamp lại vào playground. Speed thay engine timeline
của đúng slot ngay khi user bấm stepper hoặc kéo slider nhưng giữ nguyên `PetState`, nên
action, combo, vị trí và animation cursor không bị reset.
Touch cập nhật `FLAG_NOT_TOUCHABLE` của đúng window. Messages/custom messages thay speech
director và bubble window của đúng pet. Reset position cập nhật instance ngay. Khi pack
hoặc số slot đổi, service preload asset mới rồi rebuild session đang chạy tự động; user
không cần Stop/Start. Đổi character với count không đổi giữ normalized live position của
các pet; add/remove dùng position list đã được repository materialize theo roster mới.

## Settings UX

- Home dùng segmented control cho Mixed/Swarm. Icon mắt trên từng Mixed card thay dropdown
  count và áp dụng ngay; global switch vẫn là quyền bật/tắt toàn bộ overlay.
- Swarm free được unlock sau Rewarded thành công; Premium tự unlock. Unlock không tự Start.
- Settings hub chỉ hiển thị roster + trạng thái tóm tắt và app/support.
- Tap card mở `pet_customization/{slotIndex}`.
- Size editor dùng slider 11 nấc `50–150%` kèm stepper `−/+`; giá trị optimistic trên UI
  trong khi DataStore persist và overlay đang chạy nhận cùng state flow.
- Speed editor dùng slider 5 nấc `50/75/100/125/150%` kèm stepper `−/+`; cả hai điều
  khiển dùng chung optimistic state và cập nhật pet đang chạy ngay lập tức.
- Add mở Catalog ở `slotIndex == petCount`; chỉ khi Set/Import thành công mới tăng count.
- Remove có confirm, shift profile/position của slot sau lên trước, append một profile
  mặc định, invalidate position revision và rebuild ngay session đang chạy.
- Sound không xuất hiện trong UI cho đến khi schema/runtime có audio thật.

Speed là mức năng lượng di chuyển, không còn là hệ số tua đều cho mọi animation. `WALK`,
`RUN`, `CREEP`, climb và `TALK_WALK` scale frame timing đầy đủ; bounce/trip/jump/dragged
scale một nửa; idle, sit, wink, look, speech, Special và các pose cảm xúc chỉ scale một
phần tư. Scripted velocity vẫn scale đầy đủ để pet thật sự đi nhanh/chậm theo setting,
nhưng nét mặt và skill không chớp ở 150%. Năm option `50/75/100/125/150%` giữ nguyên;
thay đổi đang chạy có hiệu lực từ tick kế tiếp.

## Performance/degradation

- Thiết bị thường: tối đa 3 pet, shared clock 30 FPS cho 1–2 pet và 24 FPS cho 3 pet.
- Low-RAM device: tối đa 2 pet và shared clock 24 FPS.
- Swarm: 1–3 pet theo FPS budget hiện tại, 4–6 pet tối đa 20 FPS, 7–12 pet tối đa
  16 FPS; low-RAM clamp 6.
- Bitmap visual/cache được chia sẻ theo pack key; mỗi slot có engine/visual/speech director
  và input flag từ profile riêng nhưng không decode, parse DataStore hoặc tạo
  coroutine/thread trong frame loop.
- UI không cho vượt budget; repository vẫn sanitize dữ liệu cũ/corrupt trước khi service dùng.

Không có boot receiver hoặc auto-start. Runtime state không persist; sau process death/reboot user phải chủ động Start lại.
