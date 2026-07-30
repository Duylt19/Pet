# Battery Data Snapshot

## Mục tiêu

Các tool này audit snapshot local `battery-apk-1.0.2`, tạo schema runtime deterministic,
đóng gói generated assets cho debug và vẫn hỗ trợ sync thủ công. Raw snapshot nằm dưới
`private_data/`, bị Git ignore và không được copy vào Android source tree.

## Kết quả audit ngày 2026-07-30

| Hạng mục | Kết quả |
|---|---:|
| Theme | 898 |
| Category active | 34 |
| Free / Premium | 234 / 664 |
| Thumbnail / battery / emoji runtime | 898 / 898 / 898 |
| Runtime asset count | 2.734 |
| Runtime bytes | 100.768.466 |
| Background / emotion | 20 / 20 |
| Photo composite bị loại | 898, khoảng 60 MiB |
| ID | Unique, 1–920 có gap hợp lệ |
| Category reference | 100% hợp lệ |
| Source checksum manifest | 3.718 file pass |

Snapshot có asset/branding bên thứ ba. `distributionStatus` mặc định là
`REVIEW_REQUIRED`; việc crawl thành công không cấp quyền phân phối. Chỉ đổi thành
`APPROVED` sau khi owner có bằng chứng sở hữu/license cho từng asset source.

## Tạo catalog

Từ repository root:

```bash
python3 tools/battery_data_snapshot.py \
  private_data/battery-apk-1.0.2/battery-data \
  --catalog private_data/battery-apk-1.0.2/battery-runtime/catalog.json \
  --report private_data/battery-apk-1.0.2/battery-runtime/audit.json
```

Output:

```text
battery-runtime/
├── catalog.json
└── audit.json
```

Tool fail-closed nếu API snapshot sai, category/ID trùng, reference lệch, PNG invalid,
dimension vượt giới hạn hoặc thiếu asset. Catalog không chứa CDN URL, token hay source
filesystem path; chỉ có path runtime tương đối, byte size, SHA-256 và dimension.

## Unit test tool

```bash
python3 -m unittest tools.tests.test_battery_data_snapshot
```

Test cover happy path, missing asset, duplicate theme ID và 20 background/20 emotion.

## Đưa data vào debug app

### Tự động khi build

`mergeDebugAssets`, `installDebug` và `assembleDebug` phụ thuộc
`auditDebugBatterySnapshot` → `prepareDebugBatteryAssets`. Khi snapshot private tồn tại,
Gradle tự generate catalog và đưa 898 thumbnail/battery/emoji cùng 20 background và 20
emotion vào `build/generated/batteryCatalogAssets/debug`.

Repository ưu tiên catalog app-specific external nếu có; nếu không, debug đọc trực tiếp
`file:///android_asset/battery_catalog/...`. Vì vậy bản debug không còn cần ADB sync để
hiện catalog. Release không khai báo generated asset source và vẫn từ chối
`REVIEW_REQUIRED`.

### Sync thủ công

App debug phải được cài trước. Sau khi generate:

```bash
python3 tools/sync_battery_catalog_to_device.py
```

Chọn device:

```bash
python3 tools/sync_battery_catalog_to_device.py --serial DEVICE_SERIAL
```

Đích:

```text
/sdcard/Android/data/com.asianmobile.emojibattery.shimeji/files/battery_catalog
```

Script sync `catalog.json`, `thumb`, `battery`, `emoji`, `background` và `emotion`. Sau
khi sync, force-stop/mở lại app hoặc bấm refresh catalog. App kiểm tra canonical
containment, size và SHA-256 trước khi đánh dấu asset sẵn sàng.

## Quy tắc promotion

Không commit `private_data`, output runtime hoặc APK decompile. Để dùng production:

1. Lập asset provenance/license inventory.
2. Loại toàn bộ item không đủ quyền.
3. Host catalog/asset trên endpoint owner-controlled.
4. Generate lại checksum/version.
5. Đặt `APPROVED` bằng quy trình release có review, không sửa tay trên device.
6. Chạy parser/security/offline/device tests và có remote kill switch trước rollout.
