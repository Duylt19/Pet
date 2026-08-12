# Room catalog

## Contract

```text
json/rooms.json
room/bg/BG_<roomId>.png
room/thumb/BG_<roomId>.png
schema/room-catalog-v1.schema.json
```

Mỗi Room có stable ID, name/slug, entitlement, full background và thumbnail. Validator yêu
cầu thumbnail nhỏ và nhẹ hơn full. `defaultRoomId` phải tồn tại; Room mặc định còn có bundled
fallback trong APK.

## Runtime và update

- Grid dùng thumbnail; chọn Room mới tải full, giới hạn 8 MiB, verify bytes/hash rồi mới persist.
- Room bị gỡ sẽ fallback default rồi record đầu tiên.
- Không tái sử dụng ID hoặc đổi `defaultRoomId` khi APK hiện hành còn tham chiếu.
- Giữ naming `BG_<id>.png` trong schema v1; thay background phải tạo lại thumbnail/checksum.

```bash
python3 tools/room_catalog_pipeline.py build <arguments theo README server>
python3 tools/room_catalog_pipeline.py validate
```
