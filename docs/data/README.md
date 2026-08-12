# Server data handbook

Thư mục này là tài liệu vận hành chính thức cho dữ liệu runtime nằm ngoài APK. Mục tiêu là
để một người mới có thể biết data nằm ở đâu, app lấy dữ liệu thế nào, file nào được tải
lazy, cách phát hành bản cập nhật và cách tránh đổi nhầm ý nghĩa ID đã persist.

## Bản đồ tài liệu

| Tài liệu | Phạm vi |
|---|---|
| [SERVER_SYNC_RUNTIME.md](SERVER_SYNC_RUNTIME.md) | Kết nối private GitHub, Remote Config, fetch/cache/ETag, download và fallback |
| [PET_CATALOG.md](PET_CATALOG.md) | Pet metadata, thumbnail, ZIP pack và secure installer |
| [BATTERY_CATALOG.md](BATTERY_CATALOG.md) | Theme, background, emotion, animation, preview/full và lossless WebP |
| [ROOM_CATALOG.md](ROOM_CATALOG.md) | My Pet Room background và thumbnail |
| [SPEECH_ANCHORS.md](SPEECH_ANCHORS.md) | Metadata vị trí speech bubble và quan hệ với Pet catalog |
| [MIGRATION_AND_PUBLISH.md](MIGRATION_AND_PUBLISH.md) | Quy trình update, version, ID, validation, publish và rollback |

Repo server tương ứng:

```text
Server-Emoji-Battery-Shimeji-Pet-AM/
├── json/       # catalog app đọc
├── schema/     # JSON Schema
├── tools/      # build/convert/validate pipeline
├── data/       # Pet ZIP
├── thumb/      # Pet thumbnail
├── battery/    # Battery assets
└── room/       # Room assets
```

Không sửa JSON/hash bằng tay rồi push. Luôn dùng pipeline, chạy validation và review diff
theo checklist trong `MIGRATION_AND_PUBLISH.md`.
