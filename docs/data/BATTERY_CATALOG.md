# Battery catalog

## Contract

```text
json/batteries.json
battery/
├── thumb/<themeId>.webp|png
├── battery/<themeId>.webp|png
├── emoji/<themeId>.webp|png
├── background/<name>.webp|png
├── background_preview/<name>.webp|png
├── emotion/<name>.webp|png
├── emotion_preview/<name>.webp|png
├── emotion_group/<groupKey>.jpg
└── animation/<name>.gif|json
```

Ảnh tĩnh chấp nhận PNG hoặc lossless WebP. GIF, Lottie JSON và JPEG group background giữ
nguyên. Path trong catalog là source of truth; app không tự thay extension.

## Nhóm data và lazy loading

| Nhóm | Persistence | UI tải trước | Khi chọn/runtime |
|---|---|---|---|
| Theme | theme ID; Battery/Emoji độc lập | `thumb` | materialize full `battery`/`emoji` |
| Background | leaf ID; `0` là màu phẳng | `background_preview` | full `background` |
| Emotion | leaf ID; group chỉ taxonomy | `emotion_preview` + group JPEG | full `emotion` |
| Animation | ID/name/type | GIF theo viewport; Lottie fallback | selected GIF/Lottie local verified |

Asset record có `path`, `sizeBytes`, `sha256`, `width`, `height`. Preview phải nhẹ hơn full.
Theme entitlement là `FREE|PREMIUM`; source status là `REVIEW_REQUIRED|APPROVED`.

`HybridBatteryCatalogRepository` đọc cache trước, revalidate theo TTL/ETag/backoff và map
record thành private GitHub URL. Preview dùng Coil. Full asset được
`GithubBatteryCatalogClient` stream/verify (tối đa 5 MiB) và cache tại
`files/battery_catalog_assets/<sha256>.<extension>`. Renderer không dùng file chưa verify;
download lỗi giữ selection cũ.

## Manual lossless WebP

Server hiện vẫn dùng PNG. Khi owner tự convert trong giai đoạn debug, dùng Android Studio
**Convert to WebP → Lossless encoding**, bật skip nếu WebP lớn hơn PNG và giữ nguyên ID/name.
Catalog hợp lệ có thể chứa hỗn hợp PNG/WebP.

Sau khi convert thủ công, cập nhật `path`, `sizeBytes`, `sha256`, `width`, `height`, tăng
`catalogVersion`/`capturedAt`, rồi chạy:

```bash
python3 tools/battery_catalog_pipeline.py validate
```

Không convert GIF/Lottie. Không dùng lossy WebP cho icon/status asset có alpha và nét nhỏ.
Không xóa PNG trước khi catalog mới đã validate.

## Cách update

- Từ baseline này ID không đổi nghĩa. Thêm data dùng ID mới; đưa lên đầu bằng `order`, không
  re-index ID.
- Thay ảnh cùng ID chỉ khi là revision của cùng nội dung; đổi path/checksum/size/dimension và
  `catalogVersion`.
- PNG→WebP giữ ID/name, chỉ thay asset record.
- Preview/full được publish atomically cùng commit.
- Thay field/type/path semantics cần parser app trước và tăng `schemaVersion`; thay item/binary
  chỉ tăng `catalogVersion`.
- Chỉ dùng `APPROVED` sau ownership/license review.
