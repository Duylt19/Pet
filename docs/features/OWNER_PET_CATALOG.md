# Owner Pet Catalog

## Current local-test contract

The Catalog screen reads the owner-authorized snapshot from app-specific external storage:

```text
/sdcard/Android/data/com.asianmobile.privatebrower/files/pet_catalog/
├── shimeji.json
├── data/<petId>.zip
└── thumb/<petId>.png
```

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
- Selecting an already prepared pet updates DataStore immediately; changes apply to the next overlay Start.
- Tapping an installed catalog card can open its existing pack detail screen.

## On-demand legacy conversion

The raw data is not expanded eagerly. `LegacyShimejiPackInstaller` converts only the selected ZIP into the validated pack-v1 directory:

```text
files/pet_packs/installed/owner.shimeji.<petId>/1/
├── manifest.json
└── frames/<normalized available frame>.png
```

Conversion is staged and atomically promoted. It applies archive size/entry/path/unpacked limits, validates decoded image bounds, prefers canonical frame names, normalizes upper-case/suffixed names, uses a safe fallback for missing numbered frames, and converts the two GIF frames mislabeled as PNG in pack `136` into real PNG files. The pinned owner snapshot is never modified.

## Server migration boundary

`OwnerPetCatalogRepository` is the UI/ViewModel boundary. The current implementation reads local files and delegates conversion to the pack installer. A server implementation should replace only this repository implementation:

1. fetch/cache catalog metadata and category data;
2. download thumbnail/archive files with the snapshot SHA-256 contract;
3. expose the same `OwnerPetCatalogSnapshot`;
4. pass the cached archive through the same validated converter/installer;
5. preserve owner provenance and server version metadata.

No Catalog composable, selection state, DataStore key, or overlay runtime contract needs to change.
