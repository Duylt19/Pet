# Owner Pet Catalog

## Current local-test contract

The Catalog screen reads the owner-authorized snapshot from app-specific external storage:

```text
/sdcard/Android/data/com.asianmobile.emojibattery.shimeji/files/pet_catalog/
├── shimeji.json
├── data/<petId>.zip
└── thumb/<petId>.png
```

Sau khi đổi package, dữ liệu từng sync dưới thư mục package cũ không tự chuyển sang app mới.
Chạy lại `tools/sync_pet_catalog_to_device.py` để nạp catalog vào đường dẫn canonical ở trên.

This location needs no broad storage permission and keeps the 657 MB payload out of the APK and Git. Run the checked-in sync tool after installing a debug build:

```bash
python3 tools/sync_pet_catalog_to_device.py
```

The tool verifies that the device contains the same archive/thumbnail counts as the local snapshot. `--serial`, `--adb`, `--source`, `--package`, and `--metadata-only` are available for explicit test setups.

## UI behavior

- The screen loads all 1,026 records and their local thumbnail paths.
- Search matches pet name, category, or creator without case sensitivity.
- The category rail contains `All` followed by categories sorted by pet count and name.
- `Set` is disabled when a corresponding local archive is missing.
- Selecting an already prepared pet updates the targeted slot in DataStore immediately;
  nếu đây là Add flow thì slot chỉ được activate sau Set/Import thành công. Back khỏi
  Catalog không tăng số pet. Changes apply to the next overlay Start.
- Tapping an installed catalog card can open its existing pack detail screen.

## On-demand legacy conversion

The raw data is not expanded eagerly. `LegacyShimejiPackInstaller` converts only the selected ZIP into the validated pack-v1 directory:

```text
files/pet_packs/installed/owner.shimeji.<petId>/3/
├── manifest.json
└── frames/<normalized available frame>.png
```

Conversion is staged and atomically promoted. Revision 3 preserves the canonical repeated-frame rhythm for drag, wall/ceiling climb, creep and trip; exposes run, controlled wall descent, look-up, dangle and wall-jump poses; and keeps available Special frames even when a legacy ZIP has a partial 38–46 range. Other optional actions are only advertised when their required frames exist. Safe fallback is limited to mandatory idle/walk compatibility. The converter also applies archive size/entry/path/unpacked limits, validates decoded image bounds, prefers canonical frame names, normalizes upper-case/suffixed names, and converts the two GIF frames mislabeled as PNG in pack `136` into real PNG files. The pinned owner snapshot is never modified, and older installed revisions remain readable.

## Server migration boundary

`OwnerPetCatalogRepository` is the UI/ViewModel boundary. The current implementation reads local files and delegates conversion to the pack installer. A server implementation should replace only this repository implementation:

1. fetch/cache catalog metadata and category data;
2. download thumbnail/archive files with the snapshot SHA-256 contract;
3. expose the same `OwnerPetCatalogSnapshot`;
4. pass the cached archive through the same validated converter/installer;
5. preserve owner provenance and server version metadata.

Catalog keeps the same repository boundary when moving to a server; slot-targeted selection remains app-local state and does not change the server contract.
