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

Quan sát trực tiếp cho thấy 35/36 là pose đưa tay/giữ vật thể, không có glyph hay text
được raster vào ảnh. `actions.xml` của Shimeji-EE xác nhận:

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
khác. Project chủ động chuyển sequence này thành pose `TALK/PRESENT`: giữ đúng nhịp
`34 → 35 → 34 → 36`, còn nội dung chữ là lớp UI riêng. Đây là adaptation theo nền tảng,
không khẳng định sai rằng PNG gốc chứa text.

## Runtime contract

### Action và combo

- Legacy converter revision 4 thêm clip loop `TALK`, 240 ms/frame, chỉ khi đủ frame
  34/35/36.
- Combo `CHATTER` chạy
  `IDLE 1,5–2,5 s → TALK 4,5–7 s → WINK → SIT 3–5 s`.
- Pack thiếu 34/35/36 không khai báo `TALK`; combo tự loại qua `requiredActions`, không
  dùng ảnh fallback giả làm pose nói.
- Built-in cat có clip code-native tương đương để feature không phụ thuộc riêng owner
  pack.

### Speech director

`PetSpeechDirector` là Kotlin thuần và nhận `PetTransition/PetEffect`, không phụ thuộc
`View`, `Context` hoặc `WindowManager`.

Trigger hiện tại:

| Trigger | Tone | Quy tắc |
|---|---|---|
| Single tap | Affection | ưu tiên cao, có thể ngắt lời ambient |
| Bắt đầu pose `TALK` | Chatter | chỉ phát một lần khi vào action |
| `SOCIAL_HELLO` | Social hello | pet thứ nhất nói trước |
| `SOCIAL_HELLO_REPLY` | Social reply | delay 1,6 s và chờ bubble trước kết thúc |
| Ninja/dance/magic/acrobatic combo | Skill | câu ngắn theo màn biểu diễn |
| Double-tap showcase | Celebration | ưu tiên user |
| Drag/fling | — | đóng bubble của pet ngay |

Pacing:

- toàn scene chỉ có tối đa một bubble;
- queue tối đa bốn câu, social được ưu tiên hơn ambient;
- cùng pet/tone không được xếp trùng;
- thời gian đọc = `2.200 ms + 70 ms × số code point`, clamp 2,8–6,2 giây;
- sau khi nói, pet có cooldown 14 giây;
- director nhớ câu cuối theo tone và tránh lặp ngay khi còn lựa chọn khác.

Các giới hạn này quan trọng hơn việc random thật nhiều câu: chúng tạo turn-taking và
thời gian đọc, tránh cảm giác notification spam hoặc chữ chớp liên tục.

## Bubble overlay

Speech dùng một `TYPE_APPLICATION_OVERLAY` phụ, chỉ tồn tại khi có câu đang hiển thị:

- fixed 220×84 dp, transparent, `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE`;
- không tạo full-screen window và không chặn app bên dưới;
- bám tâm pet, clamp ngang trong viewport;
- mặc định nằm trên pet; khi pet sát trần thì chuyển xuống dưới và đảo hướng đuôi bubble;
- update vị trí bằng cùng shared frame clock, không tạo thread/coroutine/timer riêng;
- text tối đa ba dòng, tương phản cao và có `contentDescription`;
- stop/service destroy remove bubble trước khi remove các pet window.

Text hiện nằm trong Android resources, có English base và Vietnamese. `Pet messages`
trong Settings được persist bằng `pet_messages_enabled`, mặc định bật và áp dụng ở lần
Start pet kế tiếp.

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

- JVM: tap show/hide theo reading time, TALK chỉ trigger một lần, social reply tuần tự,
  drag đóng bubble.
- Pack: contract sequence 34/35/34/36 và immutable owner conversion revision 4.
- Android: bubble trên/dưới pet, clamp hai mép, 1/2/3 pet turn-taking, rotation,
  screen-off/resume, Settings off, Stop không còn window.

## Device verification

Pixel 3 XL / Android 12 / API 31:

- owner pack `Natsu` từ `4.zip` được convert thành `owner.shimeji.4@4`;
- manifest app-private chứa đúng clip loop
  `shime34 → shime35 → shime34 → shime36`, 240 ms/frame;
- tap tạo một `Cute Pet speech` window 770×294 px, tương ứng 220×84 dp ở density
  thiết bị, có `NOT_TOUCHABLE` và bám phía trên pet;
- bubble tự remove sau reading time;
- Stop còn 0 pet/speech window, 0 `PetOverlayService` và logcat không có lỗi feature.
