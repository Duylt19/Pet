# Pet Pack v1 — Runtime and Security Contract

## Phạm vi

Phase 4 hỗ trợ một built-in code-native pet và các pack `.zip` do user import. Pack chỉ chứa `manifest.json` cùng ảnh PNG/WebP; không chứa code, executable, font, audio hoặc network metadata.

Pack đang được overlay giữ là snapshot đã load. Import/select pack khác không thay renderer đang chạy; pack mới được áp dụng ở lần Start tiếp theo. Vì vậy lỗi import hoặc file pack hỏng không làm crash pet hiện tại.

## Cấu trúc archive

```text
manifest.json
sprites/
  pet.png
```

`manifest.json` bắt buộc ở root. Mọi đường dẫn dùng `/`, là relative path và không được có segment rỗng, `.`, `..`, backslash hoặc NUL.

Ví dụ đầy đủ: [`docs/examples/pet-pack-v1`](../examples/pet-pack-v1).

## Manifest schema v1

| Field | Contract |
|---|---|
| `schemaVersion` | Bắt buộc bằng `1` |
| `id` | `[a-z0-9][a-z0-9._-]{0,63}` |
| `version` | Integer dương, immutable cho cùng `id` |
| `name`, `author` | Display metadata có giới hạn độ dài |
| `canvas` | Logical width/height và `defaultScale` 0.25–4 |
| `anchor` | Điểm neo chuẩn hóa `x/y` trong khoảng 0–1 |
| `interaction.tapAction` | Action clip chạy khi pet được tap |
| `clips[]` | Action, loop/nextAction và danh sách frame |
| `frames[]` | File, source rect, `durationMs`, optional scripted velocity |

`idle` phải loop và `walk` phải tồn tại. Renderer map metadata sang pure `PetClip` một lần khi service start; action gesture không có clip riêng fallback về frame idle an toàn.

## Installer pipeline

```text
content Uri
  → capped staging archive
  → safe unzip into random app-private staging directory
  → parse schema + inspect bitmap bounds
  → validate manifest/files/rects/budgets
  → atomic rename to files/pet_packs/installed/<id>/<version>
  → repository refresh/select
```

Các guardrail hiện tại:

- Archive tối đa 20 MiB; unpacked tối đa 32 MiB; một entry tối đa 12 MiB.
- Tối đa 256 entry và expansion ratio tối đa 100×.
- Chỉ nhận `manifest.json`, `.png`, `.webp`; reject path traversal và duplicate entry.
- Manifest tối đa 256 KiB; ảnh tối đa 4096×4096; tổng tối đa 16M pixel/64 MiB decoded budget.
- Source rectangle phải nằm hoàn toàn trong bitmap; duration, velocity, clip/frame count đều có bound.
- Version đã cài là immutable. Promote dùng rename trong cùng app-private filesystem; staging luôn được cleanup.

## Repository và cache

- `PetPackRepository` expose `StateFlow` danh sách pack và selected pack process-local.
- Built-in Orange Cat luôn là fallback nếu installed pack không còn hợp lệ.
- `PetBitmapCache` dùng `LruCache`, budget bằng 1/16 app memory class và clamp 4–24 MiB.
- Bitmap được decode/preload trước frame loop; `PetOverlayView.onDraw` chỉ lấy frame đã chuẩn bị và vẽ source rect.
- Persist selection thuộc Phase 5; Phase 4 cố ý reset về built-in sau process restart.

## Asset ownership

`Sunny Cat` sample sprite được tạo riêng cho project bằng built-in image generation và chroma-key removal. Không có source, asset hoặc branding của app đối thủ trong pack mẫu.

## Device verification

Pixel 3 XL (`crosshatch`), Android 12 / API 31 đã pass luồng system picker → import ZIP → validate/promote → catalog/detail/select → sprite overlay trên launcher → drag/fling → Stop. Sau Stop, service/window/notification đều bằng 0 và log không có fatal/OOM.
