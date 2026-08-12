# Server sync runtime

## Endpoint và xác thực

App đọc static data từ private GitHub raw repository:

```text
https://raw.githubusercontent.com/Asian-Mobile-Inc/
Server-Emoji-Battery-Shimeji-Pet-AM/master
```

Catalog production là `json/pets.json`, `json/batteries.json` và `json/rooms.json`.
Private repository cần `Authorization: Bearer <github token>`. Token chỉ lấy từ Firebase
Remote Config key `github_token_pet_server` qua `SafeRemoteConfig`; không đặt token trong
source, docs, test, log, analytics hoặc crash message. Coil interceptor chỉ được gắn token
cho đúng raw host và repository allowlist.

## Luồng catalog chung

Pet, Battery và Room dùng cùng `PetCatalogRefreshPolicy`:

1. Đọc catalog JSON app-private cuối cùng đã hợp lệ và publish ngay để UI không chờ mạng.
2. Đọc `metadata.json`: ETag, thời điểm validation và rate-limit deadline.
3. Nếu catalog mới được validate trong 24 giờ thì không gọi GitHub lại.
4. Hết TTL thì gửi `If-None-Match`.
5. `200`: parse/validate toàn document trước rồi atomic replace cache.
6. `304`: giữ JSON, chỉ cập nhật ETag/thời gian validation.
7. `403/429`: đọc `Retry-After`/`X-RateLimit-Reset`, giữ cache và hoãn retry tối đa 24 giờ.
8. Network/parse/hash lỗi không xóa dữ liệu đang dùng; repository fallback theo từng loại.

Debug nhận catalog `REVIEW_REQUIRED`; release chỉ nhận `APPROVED`.

## Preview, full asset và materialization

- Preview/thumbnail: URL remote được Coil tải lazy theo viewport, dùng memory/disk cache.
- Full runtime asset: repository stream vào temporary file, kiểm tra giới hạn byte, declared
  `sizeBytes` và `sha256`, sau đó atomic rename thành app-private cache.

Battery cache full asset bằng `<sha256>.<extension>`, Pet ZIP bằng
`<petId>-<sha256>.zip`, Room bằng checksum. Nội dung đổi checksum tự tạo cache revision mới;
file temporary/sai hash không được đưa cho renderer hoặc installer.

## Fallback

| Data | Cache cuối hợp lệ | Debug/local | Built-in cuối cùng |
|---|---|---|---|
| Pet | `files/pet_catalog/pets.json` | external/local catalog | Orange Cat pack |
| Battery | `files/battery_catalog_remote/batteries.json` | external + packaged debug | built-in theme/background |
| Room | `files/room_catalog_remote/rooms.json` | cache | bundled default room |
| Speech anchor | nằm trong Pet catalog/cache | audit JSON chỉ dùng server | manifest/default attachment |

Catalog và asset phải được publish cùng một commit. App luôn giữ catalog/selection cũ khi
asset revision mới tải lỗi, nên partial deploy không được biến thành state selected giả.
