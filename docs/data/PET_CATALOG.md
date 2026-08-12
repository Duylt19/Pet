# Pet catalog

## Contract

```text
json/pets.json
data/<petId>.zip
thumb/<petId>.png
schema/catalog-v1.schema.json
```

`pets.json` schema v1 chứa catalog/source version, count, categories và Pet records. Mỗi
record có stable ID, name/category/author, thumbnail, ZIP archive và optional speech anchor.
Asset record giữ path, bytes và SHA-256.

## Runtime

- Pet Store/Search/Discover chỉ tải metadata/thumbnail; không preload ZIP.
- Unlock/chọn Pet mới tải đúng một ZIP.
- ZIP giới hạn 20 MiB, verify bytes/hash, chống path traversal/zip bomb rồi mới qua
  `LegacyShimejiPackInstaller`.
- Installer normalize vào app-private revision bằng staging + atomic promote.
- Pet đã cài chạy từ pack local, không download lại mỗi session.

## Cách update

- Không đổi nghĩa/tái sử dụng Pet ID đã publish.
- Sửa metadata: giữ ID/path/hash nếu binary không đổi; tăng `catalogVersion`.
- Thay ZIP/thumbnail: giữ ID, pipeline cập nhật size/hash; checksum tạo cache revision mới.
- Xóa Pet phải có selection fallback; ưu tiên hidden/deprecated trong schema mới.
- Đổi shape record là breaking migration: ship parser mới trước rồi tăng schema.

```bash
python3 tools/catalog_pipeline.py build --source <authorized-source>
python3 tools/catalog_pipeline.py validate
python3 tools/football_pet_pipeline.py validate
```
