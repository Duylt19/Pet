# Pet Settings and Session Policy

## Persistent contract

`PetSettingsRepository` expose một `StateFlow<PetPreferences>` trên DataStore. My Pet Room,
Pet Store và service ghi/đọc qua repository; UI không truy cập key storage trực tiếp.
Các field Mixed/Swarm và profile theo slot còn được sanitize để tương thích dữ liệu/runtime
cũ, nhưng không còn screen quản lý công khai.

| Setting | Giá trị hợp lệ | Runtime |
|---|---|---|
| Pack key / slot | đúng 12 installed key hoặc rỗng | missing/invalid được normalize về rỗng; không cấp pet mặc định |
| Count | Mixed 0–12 | một window/state machine cho mỗi pet active; 0 không tạo overlay |
| Mixed visibility / slot | on/off độc lập, kể cả pet cuối cùng | hide/show đúng window và speech ngay; tất cả off thì service dừng |
| Swarm pack | một installed/built-in key riêng | lặp cùng pack cho mọi instance |
| Swarm count | 1–12; low-RAM tối đa 6 | add/remove phần chênh lệch ngay, không reset pet cũ |
| Swarm base size/speed | cùng range và step của pet thường | cập nhật runtime ngay, không rebuild Swarm |
| Swarm random variation | on/off | variation deterministic ±2 step theo instance |
| Swarm movement insets | mỗi cạnh 0–30%, bước 5% | giới hạn playground ngay; 0% không tạo lề bounds và vẫn giữ screen-edge overflow theo size pet |
| Size / slot | 50–150%, bước 10% | 100% = 84dp trước `defaultScale`, clamp 48–144dp |
| Speed / slot | 50–150%, bước 25% | locomotion/physics/expression scale từ profile đúng pet |
| Sound | on/off | reserved; pack v1 chưa hỗ trợ audio |
| Pet messages / slot | on/off | pet off không tạo speech director/window; pet khác không bị ảnh hưởng |
| Custom messages / slot | 0–30 câu, tối đa 80 code point/câu | danh sách rỗng dùng catalog có sẵn cho riêng pet đó |
| Interaction / slot | on/off | off chỉ thêm `FLAG_NOT_TOUCHABLE` cho window đúng pet |
| Position / slot | nullable x/y chuẩn hóa 0–1 | lưu khi Stop, restore/clamp theo bounds mới |

Legacy `pet_selected_pack_key` được materialize thành 12 slot cùng giá trị khi
`pet_selected_pack_keys` chưa tồn tại hoặc chưa đủ record, sau đó mỗi slot được update
độc lập. Giá trị debug cũ `builtin.orange-cat@1` được migrate thành slot rỗng và
`petCount=0`; key legacy tiếp tục mirror slot 1 để migration tương thích. Remove từ UI shift
toàn bộ profile/position của slot sau lên trước và append một profile mặc định ở cuối.

Size/speed/messages/custom messages/interaction global cũ là migration fallback: khi key
slot mới chưa có, giá trị cũ được duplicate sang cả 12 profile. Lần user chỉnh một pet,
repository ghi đủ 12 record và chỉ mutate slot đích.

Reset position đặt record đúng slot về `null`, tăng revision của slot đó và đưa instance
đang chạy về điểm mặc định ngay. Session cập nhật revision sau khi reset; Stop chỉ merge
vị trí theo revision hiện hành nên reset pet 2 không xóa hoặc ghi đè vị trí pet 1/3.

Repository chuẩn hóa custom message bằng cách bỏ khoảng trắng/câu rỗng/trùng và persist
dạng newline-delimited string để đọc tương thích dữ liệu cũ. Size resize window giữ tâm/chân trên
sàn, giữ attachment ở tường/trần và clamp lại vào playground. Speed thay engine timeline
của đúng slot ngay khi user bấm stepper hoặc kéo slider nhưng giữ nguyên `PetState`, nên
action, combo, vị trí và animation cursor không bị reset.
Touch cập nhật `FLAG_NOT_TOUCHABLE` của đúng window. Messages/custom messages thay speech
director và bubble window của đúng pet. Reset position cập nhật instance ngay. Khi pack
hoặc số slot đổi, service preload asset mới rồi rebuild session đang chạy tự động; user
không cần Stop/Start. Đổi character với count không đổi giữ normalized live position của
các pet; add/remove dùng position list đã được repository materialize theo roster mới.

## UI ownership hiện tại

- Pet Store sở hữu browse, Rewarded/Premium unlock, download/verify và bước đặt tên.
- My Pet Room sở hữu roster đã mở khóa, xóa pet, cho ăn, background phòng và toggle
  `Active`/`Inactive` cho từng pet.
- Scene My Pet Room chỉ dựng các pack `Active`; card `Inactive` vẫn ở roster để user bật lại.
- Roster luôn giữ tối thiểu một pet `Active`: thao tác tắt pet cuối cùng bị chặn bằng dialog. Energy
  chỉ điều khiển care/feeding UI và không phải điều kiện để bật hoặc tắt pet. Nếu cả 12 slot Mixed
  đã được gán, pet chưa có slot không thể bật cho tới khi user xóa một pet và UI phải báo rõ lý do.
- Switch global ở Shimeji Pets không xin quyền hoặc khởi động service khi roster không có pet
  active; UI giữ trạng thái off và không hiển thị pet mặc định.
- Mine chỉ còn app/support; không còn Pet Catalog, Pet Detail, Customize Pet hoặc Pet Swarm editor.

Speed là mức năng lượng di chuyển, không còn là hệ số tua đều cho mọi animation. `WALK`,
`RUN`, `CREEP`, climb và `TALK_WALK` scale frame timing đầy đủ; bounce/trip/jump/dragged
scale một nửa; idle, sit, wink, look, speech, Special và các pose cảm xúc chỉ scale một
phần tư. Scripted velocity vẫn scale đầy đủ để pet thật sự đi nhanh/chậm theo setting,
nhưng nét mặt và skill không chớp ở 150%. Năm option `50/75/100/125/150%` giữ nguyên;
thay đổi đang chạy có hiệu lực từ tick kế tiếp.

## Performance/degradation

- Mixed/Swarm dùng adaptive shared clock: 1–2 pet theo device base budget, 3 pet tối đa
  24 FPS, 4–6 pet tối đa 20 FPS và 7–12 pet tối đa 16 FPS.
- Mixed hỗ trợ đủ 12 pet trên cả normal và low-RAM; Swarm vẫn clamp 6 trên low-RAM để
  giữ profile bay/nhảy dày đặc trong budget.
- Bitmap visual/cache được chia sẻ theo pack key; mỗi slot có engine/visual/speech director
  và input flag từ profile riêng nhưng không decode, parse DataStore hoặc tạo
  coroutine/thread trong frame loop.
- UI không cho vượt budget; repository vẫn sanitize dữ liệu cũ/corrupt trước khi service dùng.

Không có boot receiver hoặc auto-start. Runtime state không persist; sau process death/reboot user phải chủ động Start lại.
