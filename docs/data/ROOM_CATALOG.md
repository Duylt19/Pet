# Room catalog

## Contract

```text
json/rooms.json
room/bg/BG_<roomId>.webp
room/thumb/BG_<roomId>.webp
schema/room-catalog-v1.schema.json
```

Mỗi Room có stable ID, name/slug, entitlement, full background và thumbnail. Validator yêu
cầu thumbnail nhỏ và nhẹ hơn full. `defaultRoomId` phải tồn tại; Room mặc định còn có bundled
fallback trong APK. Parser chấp nhận PNG để đọc cache cũ, còn baseline server
`room-webp-2026-08-12-v1` dùng WebP.

## Runtime và update

- Grid dùng thumbnail; chọn Room mới tải full, giới hạn 8 MiB, verify bytes/hash rồi mới persist.
- Room bị gỡ sẽ fallback default rồi record đầu tiên.
- Không tái sử dụng ID hoặc đổi `defaultRoomId` khi APK hiện hành còn tham chiếu.
- Giữ stem `BG_<id>` trong schema v1; extension lấy từ catalog, không hardcode ở client.
- Thay background phải tạo lại thumbnail và cập nhật path/size/hash/dimension của cả hai.

```bash
python3 tools/room_catalog_pipeline.py build <arguments theo README server>
python3 tools/room_catalog_pipeline.py validate
```
