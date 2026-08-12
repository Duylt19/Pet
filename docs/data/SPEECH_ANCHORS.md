# Speech anchor data

Speech anchor là metadata vị trí gắn speech bubble lên Pet, không phải asset app tải riêng.

```text
json/speech-anchors.json          # audit/source-of-truth chi tiết
schema/speech-anchors-v1.schema.json
json/pets.json                    # x/y gọn app thực sự đọc
```

Record audit giữ Pet ID, anchor chuẩn hóa, pixel/canvas nguồn và provenance. Pipeline ghép
anchor hợp lệ vào `pets[].speechAnchor`. App chỉ fetch `pets.json`.

- Anchor x/y nằm trong `0..1`.
- Installer copy anchor vào pack manifest khi có TALK data.
- Pack cũ được enrich trong memory từ catalog cache theo Pet ID, không cần cài lại.
- Thiếu anchor dùng manifest/default attachment; runtime không đoán lại từ bitmap.
- Match bằng stable Pet ID, không bằng tên/file order; update audit JSON và Pet catalog cùng commit.

```bash
python3 tools/speech_anchor.py validate
python3 tools/catalog_pipeline.py validate
```
