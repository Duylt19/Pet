# Pet Pack v1 — Runtime and Security Contract

## Phạm vi

Phase 4 hỗ trợ một built-in code-native pet và các pack `.zip` do user import. Pack chỉ chứa `manifest.json` cùng ảnh PNG/WebP; không chứa code, executable, font, audio hoặc network metadata.

Mỗi controller giữ snapshot pack đã load. Khi import/select thành công, service preload
visual đã qua installer validation rồi rebuild controller ngay trong foreground session.
Pack missing/invalid vẫn fallback built-in; lỗi chuẩn bị replacement dừng session sạch
thay vì giữ một renderer nửa cũ nửa mới.

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
| `speechAnchor` | Optional điểm tay cầm hộp TALK chuẩn hóa `x/y` trong khoảng 0–1 |
| `interaction.tapAction` | Action clip chạy khi pet được tap |
| `clips[]` | Action, loop/nextAction và danh sách frame |
| `frames[]` | File, source rect, `durationMs`, optional scripted velocity |

`idle` phải loop và `walk` phải tồn tại. Renderer map metadata sang pure `PetClip` một lần khi service start; action gesture không có clip riêng fallback về frame idle an toàn.

Action names được schema v1 nhận: `idle`, `walk`, `run`, `fall`, `bounce`, `climb_wall`, `climb_down`, `climb_ceiling`, `sit`, `wink`, `look_up`, `dangle`, `creep`, `trip`, `jump`, `special`, `special_2`, `tapped`, `dragged`, `flung`. `idle` và `walk` vẫn là hai clip nền bắt buộc; các action mở rộng là optional để pack v1 cũ tiếp tục hợp lệ. Runtime chỉ đưa một hành vi tự động/boundary vào state machine khi manifest khai báo action đó. Mapper vẫn tạo visual/physics fallback nội bộ để mọi `PetAction` có clip an toàn, nhưng fallback không làm action thiếu asset xuất hiện trong weighted behavior pool.

Các sprite Shimeji legacy trong owner catalog dùng hướng gốc sang trái. Overlay mirror ngang frame khi `PetDirection.RIGHT` và giữ nguyên khi đi trái. Motion polish chỉ biến đổi nhẹ quanh bottom anchor cho locomotion/physics; `SPECIAL`/`SPECIAL_2` không còn scale luân phiên vì chính sprite đã chứa chuyển động. Wall/ceiling pose vẫn giữ orientation gốc, còn hướng đổi vào trong khi pet chuyển biên được quyết định trong pure engine.

Owner pack revision 7 vẫn immutable trên disk và lưu optional `speechAnchor` đã audit
theo từng pet trong server catalog. Pet không có metadata giữ attachment mặc định; app
không suy tọa độ từ bitmap ở runtime. Revision cũ được enrich từ catalog trong memory khi
overlay Start, không rewrite manifest và không yêu cầu user cài lại.
Khi service Start, mapper áp dụng profile nhịp frame tương thích theo prefix
`owner.shimeji.`: engine idle chỉ có một frame và zero velocity; renderer lấy frame đứng
đầu tiên của clip WALK thay cho frame 11 đang ngồi trong raw IDLE. Runtime còn tạo các
alias semantic chỉ cho owner pack: `EMOTE` từ clip `wink` frame 15/17, `FLOOR_PLAY` từ
`dangle` frame 31/32, `SPRAWL` từ cuối clip `creep`, `HOLD_WALL` từ frame bám tường 13
và `HOLD_CEILING` từ frame bám trần 23. Nhờ vậy combo không còn dùng pose chơi chân trên
sàn để giả bám tường/trần.

Owner pack compact 24-frame như supplement WC 2026 không có frame jump 22 và nhiều
pose của contract 46-frame. Mapper nhận diện profile này bằng tập action thực có, rồi
tạo alias runtime từ chính sprite trong pack: drag cho `JUMP`/`FLUNG`, bounce nằm cho
`CREEP`/`FLOOR_PLAY`/`SPRAWL`/`TRIP`, wall-climb 12–14 cho
`CLIMB_CEILING`/`HOLD_CEILING`, và hai sequence Special cho
`SIT`/`LOOK_UP`/`TAPPED`/`EMOTE`. Alias chỉ thay timeline, velocity và semantic action;
không sinh ảnh giả, không sửa manifest trên disk và áp dụng cả với pack revision 7 đã
cài. Vì vậy các pet compact tham gia được combo bay, nhảy và recovery thay vì chỉ đi tới
biên rồi lặp leo tường. Renderer xoay riêng chuỗi wall-climb dẫn xuất `-90°` để cạnh bám
tường trở thành cạnh bám trần. Khi đổi hướng, mirror được ghép trong screen space sau
phép xoay về mặt hình học, nên cả hai hướng vẫn giữ cùng cạnh tiếp xúc phía trên; pack có
frame ceiling gốc vẫn render nguyên trạng.

Khi pet đổi từ `CLIMB_WALL` sang `CLIMB_DOWN`, engine giữ hướng sprite nhìn vào tường
trong suốt đoạn đi xuống. Hướng chỉ quay vào viewport sau khi rời wall action để
fall/walk; điều này tránh mirror frame leo thành tư thế quay lưng vào tường.

Wink/bounce/trip/jump/tapped có nhịp đọc được; Special dùng sequence một chiều
`420/480/560/680/860 ms` và playback `PLAY_ONCE`, sau đó chuyển sang beat recovery riêng
thay vì giữ vô hạn endpoint không ổn định. `SPECIAL_2` loại các frame lặp ngược theo file
ở runtime. Việc normalize này không rewrite archive/manifest và không yêu cầu user Set
lại pet của cùng revision đã cài.

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

- `PetPackRepository` expose `StateFlow` danh sách pack và danh sách selected pack theo slot; các key được DataStore persist/restore độc lập.
- Built-in Orange Cat luôn là fallback nếu installed pack không còn hợp lệ.
- `PetBitmapCache` dùng `LruCache`, budget bằng 1/16 app memory class và clamp 4–24 MiB.
- Bitmap được decode/preload trước frame loop; `PetOverlayView.onDraw` chỉ lấy frame đã chuẩn bị và vẽ source rect.
- Key không còn hợp lệ tự fallback/persist về built-in. Mỗi slot pack đang chạy là snapshot và chỉ đổi ở lần Start kế tiếp.

## Asset ownership

`Sunny Cat` sample sprite được tạo riêng cho project bằng built-in image generation và chroma-key removal. Owner đã xác nhận snapshot Anime Shimeji được kiểm soát/ủy quyền đầy đủ; binary snapshot nằm ngoài Git và được chuyển đổi on-demand theo [`OWNER_PET_CATALOG.md`](OWNER_PET_CATALOG.md). Decompiled source, credential, ad configuration và branding vẫn không được nhập vào runtime.

## Device verification

Pixel 3 XL (`crosshatch`), Android 12 / API 31 đã pass luồng system picker → import ZIP → validate/promote → catalog/detail/select → sprite overlay trên launcher → drag/fling → Stop. Sau Stop, service/window/notification đều bằng 0 và log không có fatal/OOM.
