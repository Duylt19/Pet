# Pet Speech — bubble, dialogue và frame 34–36

## Kết luận forensic cho `4.zip`

File được phân tích:

- path: `private_data/anime-shimeji/data/4.zip`;
- SHA-256: `b54e46d46e6306b93e68eae9980b7ec03ec8a1a9418869fcb4008c2e0f4a5b00`;
- 46 PNG `shime1.png`–`shime46.png`, đều là canvas RGBA 128×128;
- `shime35.png` SHA-256
  `b9bc0a61ab90bd75a4eceba0bc58f5d1d17c350996c5a64748c9f735170cf8b4`;
- `shime36.png` SHA-256
  `85ce2396a6ddcc2ead0c1593c49fa8f2cc5b89cc649c89164a6933e47712bf4f`.

Quan sát trực tiếp cho thấy cả ba frame đều là pose đưa tay/giữ vật thể, không có glyph
hay text được raster vào ảnh. Tuy nhiên chân khác nhau rõ ràng: frame 34 đặt cả hai chân
trên sàn, còn frame 35/36 là hai pha bước chân. `actions.xml` của Shimeji-EE xác nhận:

- `FallWithIe` dùng riêng frame 36;
- `WalkWithIe` dùng `34 → 35 → 34 → 36`, velocity ngang `-2`;
- `RunWithIe` dùng cùng sequence với velocity `-8`;
- `ThrowIe` chuyển sang frame 37.

Vì vậy feature gốc là kéo cửa sổ Internet Explorer trên desktop, không phải speech.
Nguồn đối chiếu chính:

- https://github.com/gil/shimeji-ee/blob/master/conf/actions.xml
- https://github.com/gil/shimeji-ee/blob/master/conf/behaviors.xml
- https://github.com/gil/shimeji-ee

Android không có desktop-window contract tương đương để một overlay app kéo cửa sổ app
khác. Project chủ động chuyển dữ liệu này thành hai speech pose, còn nội dung chữ là lớp
UI riêng:

- `TALK`: đứng yên tuyệt đối bằng một frame 34;
- `TALK_WALK`: nói khi đi chậm bằng nhịp `34 → 35 → 34 → 36`.

Đây là adaptation theo nền tảng, không khẳng định sai rằng PNG gốc chứa text.

## Runtime contract

### Action và combo

- Legacy manifest revision 4 vẫn giữ raw clip `TALK` 34/35/34/36 để tương thích dữ liệu
  đã cài. `PetPackEngineMapper` normalize clip này lúc Start thành `TALK` một frame 34,
  zero velocity và `TALK_WALK` bốn frame với velocity 24 px/s; user không cần cài lại pet.
- Combo `CHATTER` chạy
  `IDLE 1,5–2,5 s → TALK 9–11 s → WINK → IDLE 3–5 s`.
- Speech choreography không còn là effect phát ngay khi combo bắt đầu. Mỗi combo được
  phép nói có đúng một speech beat 9–11 giây tại điểm ngắt tự nhiên: sau quan sát, sau
  landing/recovery hoặc sau màn skill.
- `CURIOUS_SCOUT` dùng `TALK_WALK` rồi có idle recovery trước khi creep để pet tiếp tục
  tiến chậm khi nói mà không đổi pose gấp; tap, chatter, social, recovery và các câu sau
  skill dùng `TALK` đứng yên. `HAPPY_ZOOMIES` là combo vận động im lặng.
- Pack thiếu 34/35/36 không khai báo `TALK`; combo tự loại qua `requiredActions`, không
  dùng ảnh fallback giả làm pose nói. Beat TALK tùy chọn của combo khác cũng được lọc,
  vì vậy pack đó vẫn chạy choreography nhưng không hiện text sai frame.
- Built-in cat có clip code-native tương đương để feature không phụ thuộc riêng owner
  pack.

### Speech director

`PetSpeechDirector` là Kotlin thuần và nhận `PetTransition`, không phụ thuộc
`View`, `Context` hoặc `WindowManager`.

Contract duy nhất để mở message là transition thật sự đi vào `PetAction.TALK` hoặc
`PetAction.TALK_WALK`.
`Tapped`, `ShowcaseStarted` và `ComboStarted` không còn trực tiếp phát text. Combo ID chỉ
xác định vocabulary/tone sau khi frame TALK đã xuất hiện:

| Combo có TALK | Tone | Vị trí nhịp nói |
|---|---|---|
| `USER_AFFECTION` | Affection | sau tap và recovery, trước wink |
| `USER_SHOWCASE` | Celebration | sau cả hai Special và final idle recovery |
| `CHATTER`, `CURIOUS_SCOUT`, `COZY_BREAK`, `CLUMSY_RECOVERY`, `DAYDREAM` | Chatter | ở điểm nghỉ/ngắm/hồi phục |
| `SOCIAL_HELLO` | Social hello | pet A nói ngay bằng frame TALK |
| `SOCIAL_HELLO_REPLY` | Social reply | pet B đứng yên chờ 9–11 s rồi mới TALK |
| `SOCIAL_SHOW_OFF`, `SOCIAL_ADMIRE` | Celebration | sau performance/observation |
| Wall/ceiling, wall-to-wall, aerial và skill/dance combo | Skill | sau landing/final recovery |
| `TINY_PERFORMANCE`, `CHEERFUL_ENCORE` | Celebration | sau hoạt cảnh chính |

Pacing:

- mỗi pet có tối đa một speech session; 1–3 pet có thể hiện box cùng lúc nếu đều đang
  render `TALK`/`TALK_WALK`;
- session được key bằng `petId`, không có active/queue chung toàn scene và pet này không
  thể preempt hoặc trì hoãn box của pet khác;
- cùng pet không thể mở lặp một session trong khi vẫn ở speech action;
- box và frame dùng cùng lifecycle: `Show` khi vào một speech action, giữ suốt beat
  9–11 giây và `Hide` trên đúng transition rời cả `TALK`/`TALK_WALK`;
- speech director không còn reading timer hoặc lệnh `advance(elapsedMillis)` riêng;
- director nhớ câu cuối toàn scene và tránh lặp ngay khi còn lựa chọn khác.

Các combo vận động thuần (`HAPPY_ZOOMIES`, `SHY_SNEAK`, patrol, chase, rest, copycat,
duet...) không có
TALK và không phát message. Tần suất hội thoại do combo scheduler cùng beat dài kiểm soát,
không dùng cooldown độc lập có thể làm pet giữ frame TALK nhưng không có text.

## Bubble overlay

Mỗi pet đang nói dùng một `TYPE_APPLICATION_OVERLAY` phụ, chỉ tồn tại khi session của pet
đó có câu đang hiển thị:

- kích thước thích ứng trong khoảng 80–260dp × 48–112dp, transparent,
  `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`;
- câu một dòng lấy `usedWidth` glyph thực tế cộng padding thay vì lấy số ký tự; text rất
  ngắn vẫn giữ minimum 80×48dp để box không bị tóp mất cân đối;
- text dài hoặc có xuống dòng được thử theo từng bước width 8dp; policy chọn box nhỏ nhất
  vừa tối đa bốn dòng và ưu tiên tỷ lệ rộng/cao ít nhất 1,65;
- maximum width tiếp tục bị clamp theo usable viewport. Nếu text không thể vừa giới hạn,
  box dùng maximum 260×112dp và renderer ellipsis ở dòng thứ tư;
- không tạo full-screen window và không chặn app bên dưới;
- mọi message dùng attachment gốc của `WalkWithIE`: canvas 128 có `ImageAnchor=64,128`,
  `IeOffsetX=0`, `IeOffsetY=-64`, nên đáy box nằm ở nửa chiều cao pet;
- box là hình chữ nhật góc vuông, không bo tròn và không có tail/tam giác phía dưới;
- box nằm hoàn toàn phía trước pet: cạnh phải chạm anchor khi quay trái, cạnh trái chạm
  anchor khi quay phải; toàn placement được mirror;
- trước speech, pet solo ở nửa trái/phải tự quay vào tâm viewport để box có đủ chỗ và
  cạnh vẫn chạm đúng hand/anchor thay vì bị horizontal clamp đẩy xuyên qua sprite;
  `TALK_WALK` đi theo hướng này và tự quay lại nếu chạm mép; social TALK giữ nguyên facing;
- session của pet bị hủy trên chính transition rời cả hai speech action;
  chuyển nội bộ `TALK ↔ TALK_WALK` không đóng/mở lại box;
- mọi placement vẫn clamp trong usable viewport;
- update vị trí bằng cùng shared frame clock nên box follow liên tục khi `TALK_WALK`,
  không tạo thread/coroutine/timer riêng;
- text tối đa bốn dòng, căn giữa theo cả hai trục, tương phản cao và có
  `contentDescription`;
- stop/service destroy duyệt và remove toàn bộ bubble trước khi remove các pet window.

## Multi-pet và direction contract V3.14

- `PetSpeechDirector.activeByPet` và `PetOverlayController.speechWindows` đều được key
  bằng cùng `petId`; mỗi `Show/Hide` chỉ mutate đúng owner.
- Hai pet cùng TALK nhận hai window độc lập. Một pet kết thúc/drag không đóng box còn lại.
- `PetOverlayView` mirror sprite và `PetSpeechPlacementPolicy` đều đọc
  `PetState.direction`. Khi direction đổi, controller update cả pet window và đúng speech
  window trong cùng render pass.
- Solo speech quay vào tâm viewport; social speech nhận direction hướng tới partner. Box
  luôn nằm trước mặt pet theo direction cuối cùng, không giữ một hướng cache riêng.
- Tap/showcase/social ground combo bị bỏ qua khi action hoặc tọa độ cho thấy pet không ở
  sàn. Guard cuối trong `changeAction` từ chối `TALK` off-ground, clear combo và trả pet về
  `FALL`/ground fallback.
- Combo catalog không cho speech đi ngay sau climb/dangle/jump/fall/flung; wall, ceiling và
  aerial story phải landing/recovery trước. `DAYDREAM` dùng IDLE recovery giữa DANGLE và
  TALK.

Catalog có sẵn hiện có 48 câu trong Android resources: tám câu cho mỗi tone, với English
base và Vietnamese. `Pet messages` trong Settings được persist bằng
`pet_messages_enabled`, mặc định bật.

`Custom message list` cho nhập mỗi câu một dòng, tối đa 30 câu và 80 Unicode code
point/câu. Counter trong Settings hiển thị số câu và độ dài câu dài nhất; Save bị khóa
khi vượt giới hạn. DataStore lưu qua `pet_custom_messages`; parser chuẩn hóa khoảng trắng,
bỏ câu rỗng/trùng và không cắt giữa surrogate pair/emoji. Khi list không rỗng, mọi
trigger dùng list này và pet chọn ngẫu nhiên không lặp ngay; `Use built-in` xóa list để
trở lại catalog 48 câu. Cả toggle và list mới áp dụng ở lần Start pet kế tiếp.

## Hướng mở rộng server

Không đưa text chưa tin cậy vào manifest v1 chỉ để phục vụ local demo. Khi backend sẵn
sàng, nên thêm `SpeechCatalogRepository` độc lập với pack binary:

1. record gồm stable line ID, locale BCP-47, tone, text, revision;
2. giới hạn code point/line count và loại control/Bidi override không hợp lệ ở parser;
3. cache catalog đã ký/versioned, fallback về resource local;
4. director tiếp tục chỉ nhận `PetSpeechCatalog`, nên scheduler/renderer không đổi;
5. entitlement hoặc character-specific copy được resolve ở repository, không nhét vào
   frame loop.

## Verification matrix

- JVM: TALK đứng yên một frame/zero displacement, TALK_WALK bốn frame/24 px/s, legacy
  runtime normalization, lifecycle engine–speech giữ box xuyên suốt 90–110 tick,
  adaptive sizing cho short/single-line/explicit newline/long/fallback/narrow viewport,
  placement trái/phải theo `IeOffset`, hủy đúng owner khi pose kết thúc,
  speaking/silent combo mapping, simultaneous multi-pet Show/independent Hide và drag chỉ
  đóng bubble của pet đó.
- Pack: raw contract sequence 34/35/34/36 và immutable owner conversion revision 4.
- Android: rectangular TALK box theo pet và hướng mirror, không tail, clamp hai mép,
  1/2/3 per-pet window, rotation, screen-off/resume, Settings off và Stop không còn
  window.

## Device verification

Pixel 3 XL / Android 12 / API 31:

- owner pack `Natsu` từ `4.zip` được convert thành `owner.shimeji.4@4`;
- manifest app-private chứa đúng clip loop
  `shime34 → shime35 → shime34 → shime36`, 240 ms/frame;
- baseline V3.10 dùng `Cute Pet speech` window 770×294 px, tương ứng 220×84 dp ở
  density thiết bị, có `NOT_TOUCHABLE`; fixed size này đã được thay bằng adaptive
  V3.12;
- tap không tạo window trong `TAPPED/IDLE`; window chỉ xuất hiện khi Natsu chuyển sang
  đúng pose TALK 34–36 với tay đưa ra;
- screenshot xác nhận box có bốn góc vuông, không tail/tam giác và bám phía trước vùng
  tay theo hướng pet;
- edge-case bên trái xác nhận pet tự mirror vào trong: owner window `[-98, 196]` có
  anchor X `49`, speech window bắt đầu đúng X `49` thay vì bị clamp xuyên qua sprite;
- bubble tự remove trên cùng transition kết thúc TALK;
- Stop còn 0 pet/speech window, 0 `PetOverlayService` và logcat không có lỗi feature.

Adaptive layout V3.12 được xác minh tiếp trên cùng thiết bị:

- hai custom message ban đầu được giữ nguyên; Settings hiển thị giới hạn 80 ký tự và
  counter `2/30 messages • longest 14/80 characters`;
- câu ngắn `a Duy đẹp trai` tạo window 345×168 px, tương ứng khoảng 98,6×48dp, thay vì
  luôn chiếm 770×294 px như fixed layout cũ;
- tap tạo một chu kỳ speech hiển thị 10.052 ms (~10,05 giây); window biến mất ở cuối
  TALK và pet tiếp tục action kế tiếp không còn giữ frame speech;
- session test được Stop sạch và cấu hình ban đầu đã được restore về ba pet.

Stationary/moving speech V3.13 được xác minh với chính owner pack revision 4 đã cài,
không reinstall:

- autonomous `TALK_WALK` giữ bubble trong lúc window pet đổi X từ 1196 sang 1236 px
  trong khoảng một giây ở speed 150%;
- tap vào pet tạo speech sau 2.311 ms; năm mẫu liên tiếp cách nhau 400 ms đều giữ nguyên
  window `(701, 2274)`;
- hai full-screen capture trong cùng stationary beat, cách nhau một giây, có cùng
  SHA-256 `ecaaef5ccfc5b55dfe56b87a5f993ff1bfea85b1af69c96b7e29a20fb8462df7`,
  xác nhận cả vị trí, frame chân và bubble không thay đổi;
- sau smoke test đã restore ba pet/size 75%, force-stop dọn sạch service và mọi overlay
  window.

Independent multi-pet speech V3.14 được xác minh tiếp trên cùng thiết bị:

- APK mới chạy với ba pet, giữ nguyên size 75% và speed 150%;
- tap pet đang wall traversal không tạo `Cute Pet speech`; pet tiếp tục wall movement,
  xác nhận ground guard không cắt climb thành TALK;
- khi hai grounded pet được trigger sát nhau, `dumpsys window` ghi nhận đồng thời hai
  title riêng `Cute Pet speech 1` và `Cute Pet speech 2`; window của pet sau không remove
  window của pet trước;
- smoke test không có `FATAL EXCEPTION`, `BadTokenException` hoặc lỗi add/remove speech
  window; force-stop dọn sạch toàn bộ overlay/service.
