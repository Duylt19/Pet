# Battery Troll — Kế hoạch triển khai

Trạng thái: **ĐÃ TRIỂN KHAI.** Server commit `6b44e78` (branch `agent/battery-troll`, chưa
push). App: 550 unit test xanh, compile + screenshot-test compile pass.

Sau vòng review đối kháng và đợt fix, trạng thái:

| # | Việc | Trạng thái |
|---|---|---|
| 1 | Screenshot golden | **Đã sinh và đã so với Figma.** `validateDebugScreenshotTest` 77/77 pass. |
| 2 | Dialog nhập phần trăm | Vẫn là thiết kế tự dựng vì Figma không có frame. Giới hạn 0–999, chỉ nhận chữ số. Cần owner duyệt hoặc cấp Figma. |
| 3 | Switch nhóm `Emoji` | **Đã giải quyết**: điều khiển `trollShowEmoji`, tắt thì nhân vật biến mất khỏi cả preview lẫn status bar. |
| 4 | Server chưa push | Vẫn `REVIEW_REQUIRED`. Push + merge `master` là hành động phát hành. |
| 5 | Bản quyền | Chưa đổi. 6/10 theme là IP, 4 trong số đó nằm sau paywall. |

### Ghi chú vận hành: screenshot test flaky

`RateAppDialogScreenshotTest` thỉnh thoảng chết với
`Resources_Delegate.initSystem called twice before disposeSystem` — lỗi teardown của
Layoutlib, không phải sai lệch ảnh. Nó lộ ra theo tổng tải render của cả suite chứ không theo
một test cụ thể, và không tái hiện đều: cùng một cây mã có lượt fail rồi ba lượt liền pass.

Đã giảm tải bằng cách hạ preview màn Customize từ `heightDp = 1148` (chiều cao full-scroll của
frame Figma) xuống `800` — đúng khung nhìn thật của điện thoại, và màn này vốn cuộn được. Đánh
đổi: golden không phủ hai hàng picker Emoji/Battery ở nửa dưới. Đừng nâng lại 1148: đã đo,
1000 và 1148 đều làm tỉ lệ fail tăng rõ rệt.

Nếu gặp lại, chạy lại lệnh trước khi đi tìm lỗi UI.

Nguồn: Figma `hjefC57z0ysLDHdP60VqMK`
— UI section `8102:2545` (cụm frame ở `y=1998`), data section `8465:6119`.

---

## 0. Kết luận kiến trúc (quan trọng nhất)

**Battery Troll KHÔNG phải một màn hình full-screen chiếm màn hình hay một overlay mới.
Nó là một chế độ mới của status-bar cover đã ship** (`StatusBarAccessibilityService`).

Bằng chứng, không phải suy đoán:

| Dấu hiệu trong Figma | Đối chiếu code hiện có |
|---|---|
| Frame `Customize` có preview status bar ở đỉnh (pet + pin + `100%`) | `BatteryStatusBarView` render đúng bố cục đó |
| Enable card `Turn on Emoji Battery to get started` | Node dùng lại nguyên `8102:2733` của Battery editor; code là `HomeEnableCard` |
| Slider `Size` đơn vị **dp** | `BatteryStatusConfig.percentSizeDp` đã tồn tại |
| Hai hàng picker `Emoji` (5) và `Battery` (5) | `selectedEmojiThemeId` / `selectedBatteryThemeId` đã tách sẵn |
| Mode `Fake Battery` / `Real Battery`, giá trị `999%` | `BatteryStatusBarView.kt:81` đang `coerceIn(0, 100)` — đây là chỗ duy nhất chặn |

Hệ quả: **bỏ toàn bộ giả thiết về Activity full-screen, keyguard, wake-lock,
`ACTION_POWER_CONNECTED` receiver.** Không cần permission mới, không cần service mới.
Đây là khác biệt lớn nhất về khối lượng công việc — từ "một feature runtime mới"
xuống còn "một chế độ của runtime đã có".

Đổi lại, feature **thừa hưởng luôn hai ràng buộc** của status-bar cover:
- Cần quyền Accessibility (đi qua disclosure + màn How to use đã có).
- `BuildConfig.BATTERY_STATUS_ENABLED` = **false ở release**. Nếu Troll phải lên
  production trước khi cờ này bật, cần quyết định riêng của owner.

---

## 1. Quyết định của owner

| # | Vấn đề | Quyết định |
|---|---|---|
| Q1 | **Bản quyền.** 6/10 theme là IP có bản quyền: Spider-Man, Zoro (One Piece), Doraemon, SpongeBob, Messi, bộ sticker corgi "KEJI". Chỉ 4 theme (mèo Xiêm, mèo đen, hải cẩu xanh, gấu mũ đỏ) là nguyên bản. | **Publish cả 10.** `distributionStatus = REVIEW_REQUIRED` làm chốt chặn. Tên hiển thị dùng mô tả trung tính (`Spider Hero`, `Green Swordsman`, `Blue Robot Cat`, `Football Star`, `Yellow Sponge`) chứ không dùng tên thương hiệu. |
| Q2 | Troll chạy trên status-bar cover hiện có? | **Đúng.** Dùng chung quyền Accessibility và `BatteryStatusConfig`. `BATTERY_STATUS_ENABLED` giữ nguyên `false` ở release. |
| Q3 | **Mô hình khoá.** | Giống Battery theme hiện tại: FREE mở thẳng, PREMIUM hiện crown ở góc tile rồi mở reward sheet khi tap. Dùng lại `BatteryThemeAccessPolicy`. Danh sách khoá do agent bốc ngẫu nhiên (seed `battery-troll-2026-08-12`): **FREE = 1, 6, 7, 9**; PREMIUM = 2, 3, 4, 5, 8, 10. |
| Q4 | **Ngữ nghĩa `Random`/`Edit`.** | `Random` = **xoay vòng emoji theo chu kỳ thời gian**, không phụ thuộc mức pin thật. `Edit` mở dialog nhập số 0–999, dựng theo style dialog sẵn có. |

> ⚠️ Rủi ro còn treo: kết quả bốc ngẫu nhiên đặt 4 theme IP (Zoro, Doraemon, Messi,
> SpongeBob) vào nhóm **trả phí**. Thu tiền trực tiếp trên nhân vật có bản quyền nặng hơn
> phát miễn phí nếu bị khiếu nại. Sửa = 1 dòng trong manifest server.

Câu hỏi phụ (không chặn, quyết trong lúc làm):
- Info chip ở mode `Real Battery` giữ nguyên chữ "Display a fake battery percentage…" hay đổi/ẩn?
- Switch `Size` ở nhóm Percentage: ẩn hẳn phần trăm hay chỉ tắt override cỡ chữ?
- Nhóm `Battery` không có switch trong khi `Emoji` có — cố ý?
- Slider min/max/step (Figma chỉ cho thấy `16dp` ở ~79% track).

---

## 2. Data — 10 theme, cấu trúc đồng nhất

Mỗi frame trong section `8465:6119` là **một theme**: một nhân vật ở 5 trạng thái cảm xúc
+ một vỏ pin ở 5 mức. Lưới 3×5 cố định, cột chạy từ đầy → cạn.

| Hàng | Nội dung | Dùng làm gì |
|---|---|---|
| 1 | composite (pin + nhân vật) | **preview only** — runtime tự ghép, không ship |
| 2 | vỏ pin, 5 mức 100/75/50/25/0 | 5 swatch hàng `🔋 Battery` |
| 3 | nhân vật, 5 trạng thái, canvas 210×210 | 5 swatch hàng `🤩 Emoji` |

Đồng nhất tuyệt đối: cả 10 frame đều đúng **15 asset hiển thị**. Chênh lệch số child
(16/17/18/19) là layer ẩn thừa của designer, `visible:false`, không render — bỏ qua.

**Ngoại lệ phải xử lý:**
- Theme 4 (Zoro) vỏ pin **dọc 112×200**, 9 theme còn lại ngang → schema cần trường
  `batteryOrientation`.
- Theme 8 (Messi) dùng PATTERN fill, theme 1 (Spider-Man) dùng raster IMAGE fill →
  hai theme này **bắt buộc export PNG**, không export SVG.
- Theme 7 có một frame nhân vật mồ côi ở ô row2-L5 không render; theme 7/6/9 nhúng
  nhân vật ở kích thước lệch chuẩn. Pipeline phải **key theo vị trí lưới**, không theo
  tên layer (tên layer `Frame 2147224226..44` đánh số không liên tục ở theme 7/8/10).

Không có Lottie/GIF/sprite sequence — toàn bộ tĩnh. **0 TEXT node** trong cả section:
tên theme và cờ premium **phải do owner cung cấp**, Figma không có.

Export đề xuất: nhân vật 420×420, pin/thumb 508×508 (2x), WebP lossless.

---

## 3. Phase 1 — Server: export + pipeline + publish

Repo `../Server-Emoji-Battery-Shimeji-Pet-AM`, branch `agent/battery-troll` cắt từ `origin/master`.

Khuôn mẫu là **rooms**, không phải batteries — vì rooms cũng nguồn Figma và pipeline tự
derive toàn bộ size/sha256/dimension từ một manifest chỉ chứa metadata biên tập.

1. **Dọn trước:** `battery/thumb.tar.gz` (11 MB untracked) đang nằm trong tree. Validator coi
   mọi file lạ dưới `battery/` là stale asset → CI fail. Xoá trước khi bắt đầu.
2. Export 10 theme × 15 asset từ Figma REST API → PNG → WebP lossless (Pillow 10.2 có sẵn).
3. `json/battery-troll.json` + asset root `troll/`:
   `troll/thumb/TROLL_<id>.webp`, `troll/emoji/TROLL_<id>_<1..5>.webp`,
   `troll/battery/TROLL_<id>_<1..5>.webp`.
4. `schema/troll-catalog-v1.schema.json` mirror `room-catalog-v1`.
5. `tools/troll_catalog_pipeline.py` copy từ `room_catalog_pipeline.py`, giữ nguyên
   `build`/`validate`, staging→validate→replace, stale detection, `TrollCatalogError` riêng.
   Manifest chỉ chứa `{id, name, slug, entitlement, batteryOrientation, order}`;
   pipeline derive mọi path/size/sha/dimension — **không bao giờ sửa tay checksum**.
6. `tools/tests/test_troll_catalog_pipeline.py` (thư mục `tools/tests/` hiện **rỗng**,
   PUBLISH_RUNBOOK trỏ vào đó nhưng không có test nào — sửa luôn trong cùng change).
7. CI: thêm đúng 1 dòng `python3 tools/troll_catalog_pipeline.py validate`.
8. Docs server: `README.md`, `docs/DATA_CATALOGS.md`, `docs/PUBLISH_RUNBOOK.md`.
9. Publish với `distributionStatus = REVIEW_REQUIRED` cho tới khi artwork được duyệt.
   Merge vào `master` chính là release — app đọc thẳng `master`.

Schema mỗi item:

```jsonc
{ "id": 1, "name": "<owner cung cấp>", "slug": "troll_1", "order": 0,
  "entitlement": "FREE|PREMIUM",
  "batteryOrientation": "LANDSCAPE|PORTRAIT",
  "assets": {
    "thumbnail": { "path": "thumb/TROLL_1.webp", "sizeBytes": 0, "sha256": "", "width": 0, "height": 0 },
    "emoji":   [ /* 5 record, level 100→0 */ ],
    "battery": [ /* 5 record, level 100→0 */ ] } }
```

## 4. Phase 2 — App: data layer

- `data/remote/BatteryTrollServerConfig.kt` — 8 dòng, mirror `RoomServerConfig`.
  Dùng lại token `github_token_pet_server`; Coil đã tự gắn `Authorization` cho host này
  qua `BaseApplication.kt:44`, không phải làm gì thêm.
- `data/model/BatteryTrollCatalog.kt` — entry/snapshot/entitlement/error, mirror
  `data/model/BatteryCatalog.kt`.
- `data/remote/GithubBatteryTrollCatalogClient.kt` + parser — mirror
  `GithubBatteryCatalogClient` (cache-first, TTL 24h, ETag, backoff 429, verify size+SHA-256).
- `data/repository/BatteryTrollCatalogRepository` + `impl/Hybrid…` — mirror
  `HybridBatteryCatalogRepository`, giữ gate release chỉ nhận `APPROVED`.
- Bind trong `di/DataModule.kt`.

**Mở rộng `BatteryStatusConfig`** (thêm field, giữ default = hành vi hiện tại):

```kotlin
val trollMode: BatteryTrollMode = BatteryTrollMode.REAL,   // REAL | FAKE
val trollFakePercent: Int = 999,
val trollThemeId: Int = 0,                                  // 0 = không dùng troll theme
val trollEmojiLevelIndex: Int = 0,                          // 0..4
val trollBatteryLevelIndex: Int = 0,                        // 0..4
val trollEmojiRandom: Boolean = false,                      // Custom vs Random
```

Sanitize trong `BatterySettingsPolicy` + key mới trong `DataStoreBatterySettingsRepository`
(dùng chung DataStore `"settings"`, **không** tạo store thứ hai).

## 5. Phase 3 — App: UI

Package `ui/battery/troll/`. Hai route mới trong `navigation/NavGraph.kt`:
`battery_troll` và `battery_troll_customize/{themeId}`, chia sẻ ViewModel qua
`getBackStackEntry` như cặp catalog↔category hiện tại.

**Màn A — `BatteryTrollThemesScreen`** (Figma `8315:7971` / `8315:8359` / `8326:8469`)
Top bar collapse (large 24sp ↔ compact 20sp) + PRO pill, banner inline 328×50,
grid 3 cột tile 101.33×101.33 r=12, banner đáy.

**Màn B — `BatteryTrollCustomizeScreen`** (Figma `8315:8232` / `8359:6992` / `8359:7165`)
Preview status bar → enable card → nhóm Mode / Percentage / Emoji → Apply panel dính đáy.
State delta đã xác định: `Real Battery` ⇒ disable `Edit`; `Random` ⇒ mờ 30% cả hai picker.

**Tái sử dụng (yêu cầu của owner) — đã đối chiếu tồn tại:**

| Figma | Code có sẵn |
|---|---|
| Top bar collapse + back | `EditorLargeTopBar` / `EditorCompactTopBar` / `EditorBackButton` — `BatteryEditorFigmaScreen.kt:193/238/269` |
| Wallpaper `8016:1072` | `StatusBarEditorWallpaper()` — cùng file `:179` |
| Enable card + switch `10:2349` | `HomeEnableCard` (`HomeChrome.kt:151`) + `AppSwitch` |
| Reward sheet `8326:8543` | `RewardOfferSheet` + `RewardGradientButton` / `RewardOutlineButton` |
| Dialog `Discard Changes?` | `BatteryDiscardChangesSheet.kt:32` (đã có sẵn slot native ad) |
| Apply panel | `StatusBarApplyPanel(enabled, onApply)` — `:980` |
| Slider thumb thanh mảnh | `DesignSlider` — `:857` |
| PRO pill | `PetPremiumBadge.kt` |
| Flow host (reward + permission + effect) | `BatteryCatalogFlowHost` — `BatteryCatalogScreen.kt:67` |

**UI thật sự mới (5 thứ):** segmented control hồng, info chip, hàng `999% + Edit`,
radio row Custom/Random, hàng 5 ô 56×56.

**Asset mới cần export:** `ic_logo_battery_emoji.xml` (`8359:5887`),
`img_emoji_love.webp` (`8359:5817`), `ic_info_rounded.xml` (`8359:5523`).

**Entry point:** `DiscoverBatteryTrollBanner` (`DiscoverScreen.kt:507`) hiện `onClick = onBattery`
→ đổi sang route mới. Golden `DiscoverBatteryTrollBannerScreenshotTest` phải refresh.

## 6. Phase 4 — Runtime

Ba điểm chạm duy nhất trong `BatteryStatusBarView.kt`:
- `:81` `coerceIn(0, 100)` — tách "mức pin thật để vẽ độ đầy" khỏi "số hiển thị",
  cho phép số hiển thị vượt 100.
- `:95` chuỗi phần trăm — lấy từ `trollFakePercent` khi mode = FAKE.
- `:768` độ đầy icon — khi có troll theme thì không vẽ vector nữa mà đổ asset
  `battery[trollBatteryLevelIndex]`.

Toàn bộ policy chọn asset/level tách thành `battery/troll/BatteryTrollPolicy.kt` **Kotlin thuần**
để unit-test được, theo đúng tiền lệ `BatteryThemeAccessPolicy` / `BatterySettingsPolicy`.

## 7. Phase 5 — Docs + test + commit

Bắt buộc cập nhật trong **cùng change**:
`docs/04_NAVIGATION_FLOW.md`, `docs/screens/README.md`, `docs/UI_STRUCTURE.md`,
`docs/10_SCREEN_TRACKING.md`, `docs/02_ARCHITECTURE.md`, `docs/05_DATA_MODEL.md`,
`docs/06_UI_DESIGN_SYSTEM.md`, `docs/07_ADS_INTEGRATION.md`, `docs/data/`,
`utils/AnalyticsHelper.kt`.

⚠️ **Cổng cứng:** `app/src/test/.../utils/ScreenNameTest.kt:22-57` assert **đúng tập 34 giá trị**
`ScreenName`. Thêm screen mà quên sửa test này là fail `testDebugUnitTest`.
`HomeTabNavigationTest.kt` assert route builder + banner predicate.

Verify: `./gradlew compileDebugKotlin` + `./gradlew testDebugUnitTest` + `git diff --check`.

---

## 8. Thứ tự thực thi và khả năng song song

```
Q1..Q4  ──►  Phase 1 (server, export+pipeline)  ─┐
                                                 ├─►  Phase 4 (runtime)  ─►  Phase 5
        ──►  Phase 2 (data layer)  ──►  Phase 3 (UI)  ─┘
```

Phase 1 và Phase 2 độc lập hoàn toàn → chạy song song bằng subagent.
Phase 3 chia được theo màn (A và B độc lập). Phase 4 phải chờ Phase 2 vì cần config mới.
Phase 5 luôn do main agent làm để tránh nhiều agent sửa cùng file docs.
