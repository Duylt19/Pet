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
├── emotion_group/<groupKey>.webp|jpg
└── animation/<name>.gif|json
```

Ảnh tĩnh chấp nhận PNG hoặc WebP; group background còn đọc JPEG để tương thích cache cũ.
Baseline server `battery-webp-2026-08-12-v1` dùng WebP cho toàn bộ ảnh tĩnh. GIF và Lottie
JSON giữ nguyên. Path trong catalog là source of truth; app không tự thay extension.

## Nhóm data và lazy loading

| Nhóm | Persistence | UI tải trước | Khi chọn/runtime |
|---|---|---|---|
| Theme | theme ID; Battery/Emoji độc lập | `thumb` | materialize full `battery`/`emoji` |
| Background | leaf ID; `0` là màu phẳng | `background_preview` | full `background` |
| Emotion | leaf ID; group chỉ taxonomy | `emotion_preview` + group WebP/JPEG | full `emotion` |
| Animation | ID/name/type | GIF theo viewport; Lottie fallback | selected GIF/Lottie local verified |

Asset record có `path`, `sizeBytes`, `sha256`, `width`, `height`. Preview phải nhẹ hơn full.
Theme entitlement là `FREE|PREMIUM`; source status là `REVIEW_REQUIRED|APPROVED`.

`HybridBatteryCatalogRepository` đọc cache trước, revalidate theo TTL/ETag/backoff và map
record thành private GitHub URL. Preview dùng Coil. Full asset được
`GithubBatteryCatalogClient` stream/verify (tối đa 5 MiB) và cache tại
`files/battery_catalog_assets/<sha256>.<extension>`. Renderer không dùng file chưa verify;
download lỗi giữ selection cũ.

## Baseline WebP hiện tại

Owner đã convert toàn bộ ảnh Battery tĩnh sang WebP trong giai đoạn debug v1. Catalog hợp lệ
vẫn có thể chứa hỗn hợp PNG/WebP để rollout và đọc cache cũ; không convert GIF/Lottie.

Sau khi convert thủ công, cập nhật `path`, `sizeBytes`, `sha256`, `width`, `height`, tăng
`catalogVersion`/`capturedAt`, rồi chạy:

```bash
python3 tools/battery_catalog_pipeline.py validate
```

Không xóa file cũ trước khi catalog WebP mới đã cập nhật path/hash/size/dimension và validate.
Với icon/status asset có alpha và nét nhỏ, ưu tiên lossless WebP và kiểm tra decoded pixels.

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
