# Owner Pet Data Snapshot

## Purpose

`tools/pet_data_snapshot.py` creates and audits an owner-authorized snapshot of the external Anime Shimeji data repository. Large binary data is written under `private_data/`, which is intentionally excluded from Git.

The tool does not feed remote data directly into the Android runtime. Server import and conversion into the validated Cute Pet pack contract remain separate steps.

## Clone

```bash
python3 tools/pet_data_snapshot.py clone private_data/anime-shimeji
```

Clone is shallow and follows only `main`. It refuses to overwrite an existing target. To capture a newer snapshot, use a new target directory so an older server import remains reproducible.

## Audit and inventory

```bash
python3 tools/pet_data_snapshot.py audit private_data/anime-shimeji \
  --report private_data/anime-shimeji-audit.json \
  --inventory private_data/anime-shimeji-inventory.csv \
  --checksums private_data/anime-shimeji-files.sha256
```

The audit:

- records repository URL and exact commit;
- cross-checks every catalog ID against `data/<id>.zip` and `thumb/<id>.png`;
- verifies PNG signatures and ZIP CRCs;
- rejects unsafe ZIP paths, detects duplicate/non-canonical numbered frames and reports missing/extra frame numbers against the app's case-sensitive `shime1.png`–`shime46.png` contract;
- inventories size, SHA-256, frame range and uncompressed size;
- emits a complete `sha256sum`-compatible manifest for every checked-out data file;
- resolves every custom pet avatar/frame URL back to a checked-out repository path;
- reports missing and extra files without deleting anything.

Exit code is `0` when every catalog pack is runtime-ready, `2` when the audit finishes but finds packs that require normalization or missing custom assets, and `1` for a tool/input failure.

## Server handoff

Upload immutable files using their SHA-256 as the integrity contract. Keep the generated report and CSV beside the server import job. Do not expose the nested `.git` directory or use the Git checkout itself as a public web root.

Production handoff is implemented in the private
`Server-Emoji-Battery-Shimeji-Pet-AM` repository. Its `tools/catalog_pipeline.py` imports
only catalog-referenced ZIP/PNG files, regenerates `json/pets.json` with byte size/SHA-256
metadata and validates all runtime assets before push. The pinned source checkout and nested
Git history remain outside that server repository.

## Current snapshot

The owner-authorized snapshot captured on 2026-07-22 is stored at `private_data/anime-shimeji` and pinned to commit `ed39a3d61e1a733b3f21cf6575650a17f359127f`.

- 1,026 catalog ZIPs and 1,026 thumbnails are present.
- 18 custom pets reference 180 assets; all 180 are present.
- The checkout contains 2,237 data files and 657,271,009 bytes excluding `.git`.
- All 1,026 catalog packs pass CRC/SHA-256 transport integrity checks and every thumbnail has a valid PNG signature; 948 packs are immediately runtime-ready.
- 78 packs require a server-side normalization rule while preserving the source snapshot: 46 use upper-case `.PNG`, pack ID `691` uses names such as `shime1 (1).png`, seven packs include alternative duplicate filenames (one overlaps the upper-case group), 24 packs contain missing or extended numbered frames, and pack ID `136` has two GIF frames mislabeled as PNG.
- Exact filenames and frame gaps are recorded in the JSON report and CSV inventory.

Generated handoff files are beside the snapshot under `private_data/`:

- `anime-shimeji-audit.json`
- `anime-shimeji-inventory.csv`
- `anime-shimeji-files.sha256`

## Local Android test sync

After installing a debug build, sync the complete snapshot into app-specific device storage:

```bash
python3 tools/sync_pet_catalog_to_device.py
```

The app reads this directory without a storage permission, displays the full catalog/categories, and converts only the selected raw ZIP into the validated pack-v1 runtime format. See [`../features/OWNER_PET_CATALOG.md`](../features/OWNER_PET_CATALOG.md).
