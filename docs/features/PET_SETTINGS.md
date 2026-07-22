# Pet Settings and Session Policy

## Persistent contract

`PetSettingsRepository` expose một `StateFlow<PetPreferences>` trên DataStore. Settings và pack selection ghi qua repository; Home/catalog/service không truy cập key storage trực tiếp. Service chụp một snapshot lúc Start để session đang chạy không đổi hành vi giữa chừng.

| Setting | Giá trị hợp lệ | Runtime |
|---|---|---|
| Pack key | installed hoặc built-in key | fallback Orange Cat khi missing/invalid |
| Count | 1–3; low-RAM 1–2 | một window/state machine mỗi pet |
| Size | 75/100/125/150% | kết hợp `defaultScale`, clamp 64–196dp |
| Speed | 50–150%, bước 25% | map duration/velocity một lần khi Start |
| Sound | on/off | reserved; pack v1 chưa hỗ trợ audio |
| Interaction | on/off | off thêm `FLAG_NOT_TOUCHABLE` |
| Position | x/y chuẩn hóa 0–1 | lưu khi Stop, restore/clamp theo bounds mới |

## Performance/degradation

- Thiết bị thường: tối đa 3 pet, shared clock 30 FPS cho 1–2 pet và 24 FPS cho 3 pet.
- Low-RAM device: tối đa 2 pet và shared clock 24 FPS.
- Bitmap visual/cache dùng chung giữa instance; không decode, parse DataStore hoặc tạo coroutine/thread trong frame loop.
- UI không cho vượt budget; repository vẫn sanitize dữ liệu cũ/corrupt trước khi service dùng.

Không có boot receiver hoặc auto-start. Runtime state không persist; sau process death/reboot user phải chủ động Start lại.
