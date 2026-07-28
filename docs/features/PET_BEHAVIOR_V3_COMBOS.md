# Pet Behavior V3 — combo director và social interaction

## Mục tiêu

V3 thay quyết định ngẫu nhiên ở cấp action bằng quyết định ở cấp **combo**. Pet chỉ chọn
một kịch bản mới sau khi đã chạy hết các bước của kịch bản hiện tại. Kết quả vẫn có biến
thiên về thời lượng và lựa chọn, nhưng animation có mở đầu, diễn tiến và phản ứng kết thúc
thay vì nhảy trạng thái rời rạc.

Khi có từ hai pet, một `PetSocialDirector` thuần Kotlin chọn cặp phù hợp, cho chúng tiếp cận
nhau, quay đúng hướng và chạy hai combo bổ sung theo vai. Director dùng chung frame clock
của overlay; không tạo timer, thread hoặc Android window mới.

## Cơ sở research

- Shimeji-ee tách primitive action trong `actions.xml` khỏi behavior có condition/frequency
  trong `behaviors.xml`. `Sequence` và `Select` là cơ sở để ghép action thành một hành vi có
  ngữ nghĩa thay vì chọn từng frame/action độc lập.
- gdx-ai phân tách decision making (state machine/behavior tree) và movement steering.
  V3 dùng cùng nguyên tắc nhưng triển khai model nhỏ thuần Kotlin để không kéo libGDX vào
  Android overlay: combo lifecycle chịu trách nhiệm decision; social director chịu trách
  nhiệm chọn cặp, approach và facing.
- Từ steering `Arrive` và `Separation`, phiên social dùng khoảng cách tâm sprite, ngưỡng gặp
  theo kích thước pet, hướng tiếp cận động và timeout. Đây là implementation clean-room;
  project không copy source của các repository tham khảo.
- Godot `AnimationTree` phân biệt transition `Immediate`, `Sync` và `At End`; V3.1 giữ
  nguyên tắc chỉ sang beat mới ở cuối chu kỳ animation, trừ gesture/physics interrupt.
- Unity Animator cho phép exit time lớn hơn một để thoát sau nhiều vòng lặp. `PetComboBeat`
  dùng duration theo milliseconds cho cùng mục tiêu: pose/locomotion có thể lặp, còn
  performance skill chạy một lượt rồi giữ final pose trước khi transition.

Nguồn:

- https://github.com/gil/shimeji-ee/blob/master/conf/actions.xml
- https://github.com/gil/shimeji-ee/blob/master/conf/behaviors.xml
- https://github.com/libgdx/gdx-ai
- https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/src/com/badlogic/gdx/ai/btree/BehaviorTree.java
- https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/src/com/badlogic/gdx/ai/steer/behaviors/Arrive.java
- https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/src/com/badlogic/gdx/ai/steer/behaviors/Separation.java
- https://docs.godotengine.org/en/stable/tutorials/animation/animation_tree.html
- https://docs.unity3d.com/2021.1/Documentation/ScriptReference/Animations.AnimatorStateTransition.html

## V3.1 — từ action queue sang story beat

Audit runtime trên owner pack cho thấy ở speed 150%:

- `SIT` 1.600 ms chỉ còn khoảng 1.067 ms;
- `WINK` 480 ms chỉ còn khoảng 320 ms;
- `SPECIAL`/`SPECIAL_2` thường hoàn tất dưới hoặc xấp xỉ một giây tùy số frame có thật.

V3 trước đây nối 4–6 action này trực tiếp nên đúng thứ tự nhưng sai nhịp: pet liên tục đổi
pose và tạo cảm giác chớp. V3.1 thay mỗi action bằng `PetComboBeat` với hai policy:

- **Once / `PLAY_ONCE`**: clip chỉ chạy một lần. Dùng cho emote, trip, tap và Special vì
  lặp hoặc đóng băng endpoint của những động tác này thường trông giống lỗi animation.
- **Sustained**: clip/pose được giữ hoặc lặp liền mạch đến một duration seeded. Dùng cho
  sit, idle, look, walk/run/creep, floor-play, sprawl và surface hold.

Mỗi combo đi theo nhịp `anticipation → primary action → recovery`. Khoảng nghỉ tự chủ giữa
hai combo là 4–8 giây; pet không phải lúc nào cũng “diễn”. Duration beat không bị rút ngắn
bởi slider speed. V3.15 còn tách mức ảnh hưởng speed lên frame theo ngữ nghĩa, nên tăng tốc
di chuyển không làm cảm xúc chớp nhanh hơn.

## Combo catalog

Catalog hiện có 25 solo/user story và 13 role/approach social. 12 combo nền được cân lại
theo từng beat:

| Combo | Anticipation | Primary action | Recovery | Mục đích |
|---|---|---|---|---|
| `CURIOUS_SCOUT` | Walk 4–7s | Idle 2–4s → Look 3–5s → Talk-walk 9–11s | Idle 2–4s → Emote once | Đi tuần, quan sát, vừa đi chậm vừa nói rồi phản ứng |
| `COZY_BREAK` | Idle 3–5s | Sit 7–11s → Floor-play 5–8s → Idle 2–3.5s → Talk | Sprawl 6–10s → Idle 3–5s | Một chuỗi nghỉ đầy đủ: ngồi, chơi trên sàn, nói rồi nằm nghỉ |
| `HAPPY_ZOOMIES` | Look 2–3.5s → Emote once | Run 3.5–6s | Walk 4–6s → Idle 3–5s | Nhìn/lấy đà, chạy vui rồi hạ tốc |
| `SHY_SNEAK` | Idle 3–5s | Creep 5–8s → Look 3–5s | Idle 2.5–4.5s | Rón rén có dừng nghe/ngó |
| `CLUMSY_RECOVERY` | Run 2.5–4s | Trip once → Sprawl 4–6s | Sit 5–8s → Talk → Emote once | Vấp, nằm hồi sức, ngồi dậy rồi mới than thở |
| `TINY_PERFORMANCE` | Look 3–5s | Special once → Idle 3–5s → Special 2 once | Emote → Talk → Idle | Mỗi skill phát trọn sequence, có khoảng nghỉ và không freeze frame cuối |
| `DAYDREAM` | Sit 6–10s | Look 4–7s → Sprawl 8–14s | Emote once → Idle 3–5s | Nhịp ngủ mơ yên tĩnh, không tự nói hoặc dùng pose sai surface |
| `BUSY_PATROL` | Walk 6–10s | Idle 2–4s → Run 2.5–4.5s | Look 3–5s | Tuần tra có checkpoint thay vì đi liên tục |
| `PEEK_AND_DASH` | Creep 5–8s | Look 3–5s → pause 1.5–3s | Run 2.5–4.5s | Thăm dò, xác nhận rồi mới chạy |
| `SLOW_MORNING` | Idle 5–9s | Sit 8–14s → Look 3–6s | Walk 4–7s | Nhịp chậm nhất, ưu tiên nghỉ |
| `BRAVE_EXPLORER` | Turn → Look 3–5s | Run 3–5s → pause 2–4s | Creep 4–7s | Đổi hướng có chủ ý, khám phá theo hai tốc độ |
| `CHEERFUL_ENCORE` | Turn → Special 2 once | Idle 3–5s → Special once | Idle → Emote → Talk → Idle | Encore phát clip thật một lượt, không giữ endpoint tùy biến |

V3.2 thêm 8 choreography dùng chính action/frame mà pack đã khai báo:

| Combo | Choreography | Chi tiết tự nhiên |
|---|---|---|
| `WALL_PARKOUR` | Run tới mép → climb → wall-hold → climb burst → wall-hold → jump/fall/bounce → walk/emote/idle | Frame 13 bám tĩnh thay cho floor pose 31–32; có hai nhịp leo |
| `CEILING_EXPEDITION` | Run → climb tới góc → wall-hold → ceiling climb → ceiling-hold → jump/fall/bounce/look/idle | Frame 13/23 là transition đúng surface; direction đảo khi vào trần |
| `WALL_DIVE` | Run → climb → wall-hold → wall jump → fall/bounce → look → Special once → idle/emote | Có anticipation trước jump và focus trước skill |
| `SKY_DIVER` | Look → boosted jump → fall → bounce → sprawl → idle → emote | Landing có nằm hồi sức thay vì lập tức nói |
| `NINJA_SKILL` | Creep → look → sprint → jump/fall/bounce → look → Special once → idle/emote | Hai focus beat tách takeoff, landing và skill |
| `BATTLE_DANCE` | Look → Special once → idle → Special 2 once → emote → idle | Hai skill đủ dài nhờ cadence frame, không freeze endpoint |
| `MAGIC_RITUAL` | Sit → look → Special 2 once → idle → Special once → emote/idle → talk | Chỉ combo phép giữ lời thoại skill |
| `ACROBATIC_FINALE` | Run → jump/fall/bounce → look → Special once → emote/idle | Take-off, landing focus và finale rõ ràng |

`motionMultiplier` chỉ tăng displacement cho beat được biên đạo (run-to-wall 1,15×,
climb-to-ceiling 2,4×, jump 2,2×), không tăng tốc animation pose hoặc toàn bộ pet.

Ngoài ra có combo riêng cho phản ứng tap/showcase và 6 scene social, mỗi scene có hai vai:

- `GREETING`: một pet nói, emote rồi idle; pet kia đứng chờ 9–11s mới trả lời, emote rồi
  idle, nên không còn hai pet cùng ngồi sau lời chào;
- `PLAY_CHASE`: leader/follower chạy cùng hướng 5–8,5s rồi có recovery; follower có thể
  trip đúng một lần và ngồi nghỉ;
- `SHOW_AND_REACT`: performer giữ mỗi Special 5–8s; observer nhìn rồi idle cổ vũ, không
  dùng SIT như khoảng chờ;
- `REST_TOGETHER`: vai A ngồi/floor-play, vai B idle/sprawl/emote; không còn hai pet cùng
  ngồi một pose;
- `COPYCAT`: pet B trễ 1,5–2,5s rồi mới copy Look/Emote/Floor-play của pet A.
- `DUET_DANCE`: hai pet đối mặt và luân phiên hai lượt `SPECIAL`/`SPECIAL_2`; các khoảng
  Idle được tính để không có cửa sổ skill chồng nhau, tạo call-and-response thật.

Tính cả approach, user reaction và hai vai social, catalog có 38 combo ID.

## V3.3 — spatial balance

Catalog vẫn giữ toàn bộ story để dùng cho trigger cụ thể, nhưng pool tự chủ mặc định hiện có
17 combo:

- chỉ giữ 6 ground basic khác biệt nhất: `CURIOUS_SCOUT`, `COZY_BREAK`,
  `HAPPY_ZOOMIES`, `CLUMSY_RECOVERY`, `TINY_PERFORMANCE`, `DAYDREAM`;
- loại 6 ground basic trùng nhịp khỏi random pool: `SHY_SNEAK`, `BUSY_PATROL`,
  `PEEK_AND_DASH`, `SLOW_MORNING`, `BRAVE_EXPLORER`, `CHEERFUL_ENCORE`;
- thêm `CHATTER`, giữ 5 climb (gồm hai hướng wall-to-wall), 3 aerial và 2 skill/dance
  story;
- climb có tổng weight 54/150, tức 36% trước khi áp quota.

Mỗi definition có `PetComboHabitat`: `GROUND`, `AERIAL`, `WALL` hoặc `CEILING`.
`nonClimbComboStreak` tồn tại xuyên qua vòng đời combo. Sau tối đa ba story không leo,
selector chỉ chọn `WALL`/`CEILING` ở lượt kế tiếp. Khi pack không đủ required action cho
climb, selector bỏ quota và fallback về các story tương thích thay vì mắc kẹt.

Multi-pet social bắt đầu sau 12 giây thay vì 6 giây để từng pet có cơ hội chọn autonomous
story đầu tiên. Contract V3.14 bên dưới tiếp tục giảm social occupancy bằng invitation
chance, range guard và cooldown dài hơn.

## V3.4 — wall-to-wall leap

`WALL_TO_WALL_LEAP` thêm một traversal story hoàn chỉnh:

1. chạy tới tường gần nhất và leo cao 12–18 giây;
2. giữ `HOLD_WALL` 3–5 giây bằng frame bám tường 13 làm anticipation;
3. `JUMP` đảo hướng vào màn hình;
4. `FALL` di chuyển ngang hết usable width trong khoảng 1,1 giây;
5. collision cạnh đối diện chuyển thẳng sang `CLIMB_WALL`, không hủy combo;
6. leo tiếp 5–8 giây, treo 6–10 giây, nhảy vào trong rồi fall/bounce/idle.

`crossScreenDurationMillis` là metadata của beat, không phải velocity pixel cố định. Engine
tính vận tốc từ `bounds.width - pet.width`, vì vậy thời gian băng màn gần như đồng nhất trên
điện thoại dọc, landscape và tablet. Beat chỉ bắt tường khi queue đang chờ `CLIMB_WALL`;
va chạm ngoài choreography vẫn dùng fallback physics hiện tại.

## V3.5 — upward wall-to-wall rise

`WALL_TO_WALL_RISE` giữ nguyên anticipation và cơ chế bắt tường của V3.4 nhưng tạo một
đường bay đi lên thật sự:

1. khi beat băng màn bắt đầu, engine nạp vận tốc Y `-700 px/s`;
2. gravity `900 px/s²` giảm dần đà đi lên, tạo cung đạn đạo thay vì dịch sprite theo
   đường thẳng;
3. trong 1,1 giây băng ngang, pet đạt đỉnh cao hơn khoảng 272 px và bắt tường đối diện
   cao hơn điểm rời tường khoảng 225 px nếu không chạm trần;
4. beat đi lên render action `FLUNG`, dùng đúng pose bay khi user ném pet; chuyển động vẫn
   do metadata cross-screen điều khiển nên velocity trong clip không làm lệch điểm bắt tường;
5. `WALL_TO_WALL_LEAP` cũ vẫn dùng `FALL` và không có launch velocity nên tiếp tục là
   biến thể lao xuống.

`crossScreenLaunchVelocityY` chỉ hợp lệ khi âm, hữu hạn và đi cùng một collision-driven
`crossScreenDurationMillis`. Nhờ vậy các combo khác dùng `FALL` vẫn giữ physics rơi hiện
tại, còn upward rise không bị `initialFallSpeed` ghi đè.

## Runtime contract

- `PetState.activeComboId` xác định combo đang chạy; `activeComboBeat` giữ beat hiện tại;
  `pendingComboBeats` giữ các beat còn lại theo đúng thứ tự.
- `comboBeatTargetMillis` được draw deterministic khi bắt đầu beat. `PLAY_ONCE` chỉ
  chuyển ở cuối clip; sustained pose/locomotion repeat đến target. Beat
  `HOLD_LAST_FRAME` vẫn là primitive engine nhưng không dùng cho owner Special vì endpoint
  frame 41/46 không có ngữ nghĩa ổn định giữa các pack.
- Beat `COLLISION` chạy cho tới khi chạm mục tiêu không gian. Timeout được tính từ khoảng
  cách còn lại, velocity thật của pack và motion multiplier, cộng grace 3 giây; pack có
  velocity lỗi sẽ thoát an toàn thay vì mắc kẹt.
- Collision khớp beat kế tiếp (`RUN → CLIMB_WALL`, `CLIMB_WALL → CLIMB_CEILING`,
  `FALL → BOUNCE`) tiếp tục cùng combo. Wall/ceiling kết thúc trước một beat `JUMP` sẽ ưu
  tiên jump. Collision không khớp vẫn hủy choreography để giữ physics an toàn.
- Chỉ khi queue rỗng và action cuối hoàn tất, engine phát `ComboCompleted`, clear combo rồi
  mới cho phép chọn combo tự chủ tiếp theo.
- `recentComboIds` chống lặp ở cấp câu chuyện, mặc định nhớ ba combo gần nhất.
- Combo được lọc bằng `supportedActions` của pack. Spatial/skill combo còn có
  `requiredActions`; thiếu một primitive bắt buộc thì loại cả combo thay vì degrade thành
  câu chuyện sai. Combo thường vẫn có thể degrade và pack cũ tiếp tục đi/idle an toàn.
- Drag và fling có quyền ngắt combo vì gesture/physics phải ưu tiên. Fall/collision chỉ
  tiếp tục khi khớp choreography như trên.

## V3.10 — pose-gated speech choreography

Speech không còn được phát từ `ComboStarted`, tap hoặc showcase effect. Catalog chia combo
thành hai nhóm rõ ràng:

- combo được phép nói có đúng một speech beat 9–11 giây ở điểm nghỉ tự nhiên;
- combo vận động/social thuần không có `TALK`/`TALK_WALK` và luôn im lặng.

Tap chạy `TAPPED → IDLE recovery → TALK → EMOTE`; showcase nói sau final idle. Các combo
leo, bay và wall-to-wall mặc định im lặng; hành động vật lý tự kể câu chuyện mà không mở
box sau mọi lần landing. Greeting dùng TALK ngay cho vai A, còn vai B idle 9–11 giây trước
TALK để tạo call-and-response bằng choreography thật thay vì timer speech độc lập.

Khi solo pet bắt đầu TALK, engine quay pet vào tâm viewport dựa trên vị trí hiện tại. Nhờ
đó box nằm ở vùng màn hình còn trống và cạnh box tiếp tục chạm đúng anchor của frame tay
cầm; các combo social được miễn policy này để hai pet luôn giữ facing với nhau.

`PetComboSpeechPolicy` chỉ map combo đang active sang tone khi engine đã chuyển
vào speech action. Unit test khóa hai chiều: mọi combo trong speaking policy phải có đúng
một `TALK` hoặc `TALK_WALK` beat và mọi combo ngoài policy không được chứa speech action.

## V3.11 — synchronized TALK lifecycle

Bubble không còn có reading-time 4,5–8,5 giây chạy song song với combo. Speech director
chỉ phát `Show` khi engine đi vào TALK và chỉ phát `Hide` trên transition rời TALK,
drag/fling hoặc cleanup. Vì vậy beat TALK 9–11 giây và box luôn bắt đầu/kết thúc cùng
nhau, không có khoảng pet tiếp tục giữ frame 34–36 sau khi text đã tắt.

V3.11 ban đầu serialize speech toàn scene để tránh pet thứ hai preempt bubble đang active.
V3.14 thay contract này bằng session/window độc lập theo pet ID: không preempt và cũng
không bắt một pet đang TALK phải chờ pet khác.

Frame loop cũng không còn gọi `speechDirector.advance(elapsedMillis)`. Unit test tích hợp
chạy engine theo tick 100 ms và khóa một lần Show, 90–110 tick TALK, rồi đúng một lần Hide
trên transition sang action kế tiếp.

## V3.13 — stationary và moving speech pose

Speech action được tách theo chuyển động thay vì dùng toàn bộ chuỗi bước chân cho mọi câu:

- `TALK` loop đúng một frame 34 và zero displacement, dùng cho tap, chatter, social,
  recovery và câu nói sau skill;
- `TALK_WALK` loop `34 → 35 → 34 → 36` ở 24 px/s, chỉ dùng có chủ đích trong
  `CURIOUS_SCOUT`; `HAPPY_ZOOMIES` dùng run → walk → idle và không nói;
- raw owner pack revision 4 không bị rewrite. Mapper nhận clip TALK bốn frame cũ, tạo hai
  runtime clip và expose `TALK_WALK` như supported action nên pet đã cài không cần Set lại;
- speech director xem cả hai là cùng một lifecycle. Chuyển giữa hai speech pose không
  flicker box, rời cả hai mới Hide; bubble follow pet bằng shared frame clock khi đi;
- `TALK_WALK` quay lại ở mép màn hình nhưng được phép đi xuyên pet tự chủ khác.

Unit test khóa frame count, velocity/displacement, legacy normalization, combo mapping và
Show/Hide xuyên qua transition giữa hai speech action.

## V3.14 — independent interaction và surface-safe speech

V3.14 tách ba khái niệm trước đây bị trộn vào nhau:

- **autonomous movement** không phải collision vật lý: `WALK`, `RUN`, `CREEP` và
  `TALK_WALK` được đi xuyên nhau, không bị dịch vị trí hay quay đầu khi sprite giao nhau;
- **overlap repair** chỉ chạy khi hai pet tự chủ đã cùng dừng ở pose nghỉ và overlap sâu
  hơn 55% chiều rộng. Đây là cleanup hiếm, giữ nguyên direction và bỏ qua social pair;
- **social interaction** là một invitation có điều kiện: chỉ cặp rảnh trên sàn, gần hơn
  4,5 pet-width mới được xét; 35% nhận lời, decline nghỉ 18 giây, hoàn tất nghỉ 45 giây.

Session social sở hữu đúng hai combo role. Nếu một pet bị tap/drag/fling, bắt đầu combo
khác, rơi hoặc leo, ownership bị mất và director release cả session ngay; không restart
`SOCIAL_APPROACH` để ghi đè hành động mới. Nhờ vậy social là một lựa chọn thỉnh thoảng,
không phải lực hút luôn bật.

Speech dùng `MutableMap<petId, session/window>` thay cho một active toàn scene. Hai pet vào
`TALK` cùng lúc đều nhận `Show`, mỗi pet tự `Hide` khi rời speech action. Direction của
sprite và placement box cùng lấy từ `PetState.direction`; solo talk quay vào viewport,
social talk giữ hướng director cấp.

`isGroundedSurface` là guard chung cho tap, showcase, ground combo, social eligibility và
transition vào speech. Climb/surface-hold/airborne không thể bị chuyển trực tiếp sang TALK.
`DANGLE` legacy được xác định lại đúng là floor-play và được phép ở ground; các story
leo/bay hiện im lặng thay vì gắn speech recovery máy móc.

## V3.15 — semantic action cadence và skill playback

Audit frame trực tiếp trên owner model Natsu cho thấy clip preview gốc dùng khoảng 400
ms/frame và hai skill là các sequence pose có diễn tiến:

- `SPECIAL`: frame `1 → 38 → 39 → 40 → 41`;
- `SPECIAL_2`: frame `42 → 43 → 44 → 45 → 46`, trong khi raw revision 4 có thể chứa thêm
  các file lặp ngược để phục vụ compatibility;
- `IDLE` legacy chứa nhiều pha chân nên loop toàn clip làm pet như bước tại chỗ;
- khi lấy slider 150% chia đều mọi duration, wink/skill/cảm xúc bị tua nhanh hơn trong khi
  các beat combo vẫn kéo dài nhiều giây, dẫn tới lặp đầu-cuối và flicker.

V3.15 giải quyết ở ba lớp:

1. **Semantic speed policy**: locomotion nhận 100% ảnh hưởng speed; physics reaction nhận
   50%; idle/emotion/speech/skill chỉ nhận 25%. Scripted velocity vẫn nhận 100% để khoảng
   cách di chuyển đúng với setting.
2. **Owner runtime profile**: mapper giữ một frame idle; gán cadence riêng cho
   bounce/emote/trip/jump/tapped; từ V3.17 cả hai Special dùng
   420/480/560/680/860 ms trước speed policy. Frame trùng ngược của Special 2 được loại
   theo file ở runtime, không mutate revision 4 trên disk và không yêu cầu reinstall.
3. **Skill playback policy**: V3.15 từng dùng `HOLD_LAST_FRAME`; atlas đa pack ở V3.17
   chứng minh frame cuối có thể là motion blur/portal trung gian nên policy hiện tại là
   `PLAY_ONCE` rồi chuyển sang recovery. Mỗi skill beat vẫn là `requiredAction`, nên pack
   thiếu frame performance sẽ loại story đó thay vì chạy combo khuyết.

Choreography cũng được audit lại theo surface và ý nghĩa frame: Ninja bắt buộc
`FALL → BOUNCE → LOOK → SPECIAL`; Battle Dance và Magic Ritual tách các skill bằng idle;
Happy Zoomies im lặng và hạ nhịp bằng WALK/IDLE. Social Duet dùng timeline hai vai:

- vai A: Idle → Special → Idle dài → Special 2 → Emote → Look;
- vai B: Idle dài → Special 2 → Idle dài → Special → Emote.

`PetState.isHoldingComboBeatFrame` vẫn là primitive engine cho pack tương lai nhưng owner
catalog không dùng nó cho Special.

## V3.16 — standing/rest balance

Audit sau V3.15 phát hiện `SIT` bị nhìn thấy nhiều hơn số action thực tế vì hai nguyên
nhân cộng dồn:

- raw owner `IDLE` dùng sequence frame `11 → 15 → 11 → 17`, trong khi `SIT` cũng dùng
  frame 11. Việc V3.15 giữ frame đầu của IDLE đã vô tình biến mọi nhịp idle thành hình
  ngồi;
- 14/17 story trong autonomous pool và 11 social role chứa ít nhất một beat `SIT`, nhiều
  beat chỉ đóng vai trò delay/recovery chứ không có lý do câu chuyện để ngồi.

V3.16 tách rõ **đứng yên** và **ngồi nghỉ**:

1. Với pack có prefix `owner.shimeji.`, engine IDLE vẫn là một frame, zero velocity và
   cadence chậm; renderer lấy visual frame đầu của `WALK` làm pose đứng. Raw revision 4
   không bị rewrite và pack ngoài owner không bị đổi ý nghĩa IDLE.
2. Autonomous SIT giảm từ 14 xuống 4 story: `COZY_BREAK`, `CLUMSY_RECOVERY`, `DAYDREAM`
   và `MAGIC_RITUAL`. Đây là các trường hợp nghỉ, hồi phục sau vấp, mơ màng hoặc charge-up
   có ngữ nghĩa rõ.
3. Social SIT giảm từ 11 xuống 5 role ở V3.16; V3.17 tiếp tục giảm còn 2 role: follower
   hồi phục sau trip và vai A của `REST_TOGETHER`. Vai B dùng sprawl, Copycat dùng
   Look/Emote/Floor-play.
4. Beat SIT còn lại có duration tối thiểu 5 giây; giảm **tần suất xuất hiện** thay vì làm
   pose ngồi chớp nhanh. Recovery sau landing/skill dùng IDLE 3–5 giây nên vẫn có nhịp thở
   trước TALK.

Unit test khóa cả hai contract: owner visual IDLE phải lấy standing frame nhưng không đổi
WALK clip; danh sách autonomous/social combo được phép chứa SIT phải đúng các story có chủ
đích ở trên.

## V3.17 — frame-semantic choreography và energy curve

Audit V3.17 đối chiếu `actions.xml` chuẩn Shimeji-ee với contact sheet Natsu, Pikachu,
Pusheen và ba biến thể Satoru Gojo. Kết luận:

- frame 31–33 là `SitAndDangleLegs`, tức một **floor animation**, không phải pose treo
  giữa tường/trần; cách dùng cũ làm pet trông như ngồi giữa không khí;
- frame 15/17 thuộc chuỗi `SitAndSpinHeadAction` và được creator tùy biến thành tim, phép,
  biểu cảm hoặc vật thể; tên `WINK` không đủ tổng quát;
- frame 13 là `GrabWall`, frame 23 là `GrabCeiling`, frame 21 là `Sprawl`;
- frame cuối 41/46 có thể là payoff ổn định, nhưng cũng có thể là motion blur, clone hoặc
  portal trung gian; không thể giữ đồng loạt nhiều giây.

Runtime tạo năm semantic alias mà không rewrite owner revision 4/5:

| Action runtime | Nguồn visual/engine | Độ phủ snapshot |
|---|---|---:|
| `EMOTE` | clip legacy `WINK` (15/17) | 1.026/1.026 |
| `FLOOR_PLAY` | clip legacy `DANGLE` (31/32) | 1.018/1.026 |
| `SPRAWL` | frame cuối `CREEP` (21) với zero velocity | 1.024/1.026 |
| `HOLD_WALL` | frame index 3 của clip climb, tương ứng frame 13 | 1.026/1.026 |
| `HOLD_CEILING` | frame index 2 của clip ceiling, tương ứng frame 23 | 1.025/1.026 |

`DANGLE` được xem là ground-safe; mọi wall/ceiling story chuyển sang HOLD đúng surface.
Ground-movement collision ngay trước `JUMP` được phép tiếp tục takeoff thay vì hủy combo.
Special phát sequence khoảng 3 giây ở speed 100%, dùng `PLAY_ONCE`, rồi recovery bằng
Idle/Look/Emote.

Selector thêm `PetComboEnergy` (`CALM`, `CURIOUS`, `ACTIVE`, `STUNT`, `PERFORMANCE`).
Sau stunt/performance, weight calm tăng 70%, curious tăng 35%, còn stunt/performance kế
tiếp giảm còn 45%. Hai combo có speech liên tiếp nhận thêm hệ số 45%. Sau calm, active
tăng 40%. Quota leo chuyển từ hai lên ba non-climb story để bớt nhịp leo có tính cơ học,
trong khi climb vẫn chiếm hơn 25% base weight.

Speech tự chủ được giữ ở sáu story có lý do rõ: Curious, Cozy, Clumsy, Tiny Performance,
Chatter và Magic Ritual. Wall/ceiling/aerial stunt tự kể bằng hình và không mở box sau mọi
lần landing.

## Social state machine

Phiên tương tác có hai pha:

1. `APPROACHING`: chọn cặp pet ở sàn gần nhau nhất, hai pet chạy về phía tâm của nhau và
   chỉ cập nhật facing khi hướng mục tiêu thực sự đổi. Khi đạt khoảng cách 1,35 pet-width
   thì chuyển pha; nếu cặp đã đủ gần, bỏ qua approach để không chạy xuyên nhau một frame.
2. `PERFORMING`: phát combo theo hai vai của scene, giữ facing phù hợp (đối mặt hoặc cùng
   hướng khi đuổi bắt). Facing có dead-zone 0,2 pet-width và không được phát lặp mỗi frame.
   Khi một vai kết thúc, director giải phóng cả cặp khỏi social ownership ngay; combo của
   vai còn lại vẫn tự hoàn tất nhưng không còn ép pet đã rảnh quay qua lại.

Các guardrail gồm: chỉ ghép pet đang rảnh trên cùng mặt sàn, đủ gần và vượt qua invitation
chance; không chiếm pet đang drag, fling, fall, jump, surface-hold hoặc climb;
approach/performance đều có timeout; mất instance hoặc mất đúng social combo ownership
sẽ hủy session an toàn.
`PetCrowdResolver` không sửa cặp social và không chặn pet đang di chuyển. Với một pet,
director không phát directive social.

## Verification

- JVM test kiểm tra catalog degrade/required-action theo pack, combo loop/one-shot chạy đúng
  thứ tự, combo completion, anti-repeat, long Sit hold và Special `PLAY_ONCE`.
- JVM test khóa hướng mép gần nhất, run-to-wall, wall-to-ceiling, inward wall jump,
  distance/velocity timeout, fall-to-bounce và việc giữ nguyên combo qua collision.
- JVM test khóa autonomous pool 17 story, climb weight tối thiểu 25%, quota sau ba
  non-climb story, reset streak và fallback khi pack thiếu frame leo.
- JVM test chạy toàn bộ wall-to-wall traversal theo cả hai hướng, xác nhận thời gian
  screen-relative, cạnh đích, facing, opposite-wall catch và combo lifecycle không bị hủy.
- JVM test xác nhận upward variant giữ vận tốc Y âm sau takeoff, giảm tọa độ Y trong lúc
  bay bằng action `FLUNG` và bắt tường đối diện ở vị trí cao hơn.
- JVM test kiểm tra approach direction, invitation decline, maximum range, greeting/duet
  roles, closest-pair selection, bỏ qua pet climb, facing dead-zone, release khi ownership
  bị ngắt và no-op khi chỉ có một pet.
- JVM test khóa mover pass-through, không đổi direction, nearby rest no-op, chỉ repair deep
  resting overlap và không can thiệp social/airborne pet.
- JVM test khóa tap/social không ngắt wall climb, mọi speech predecessor đều là ground-safe,
  hai pet TALK nhận session riêng và mỗi Hide chỉ tác động đúng owner.
- JVM test khóa semantic speed influence, owner frame sequence/duration normalization,
  derived surface/rest/emote actions, Ninja landing trước skill, toàn bộ Special beat dùng
  `PLAY_ONCE` và hai lượt duet có recovery.
- JVM test khóa owner IDLE visual sang standing frame, autonomous SIT đúng 4/17 story,
  social SIT đúng 2 role và mọi autonomous SIT còn lại kéo dài ít nhất 5 giây.
- Device smoke test cần chạy với 2–3 pet để quan sát đủ approach và ít nhất hai scene liên
  tiếp; overlay vẫn phải có đúng một foreground service và một shared render clock.

V3.15 đã smoke-test trên Pixel 3 XL / API 31 ở speed 150% với owner pack `Satoru Gojo`
đang chọn sẵn: APK cài đè không mất selection/count, service tạo đúng ba window 238×238
px và tiếp tục chạy trên launcher. Chuỗi capture 15 giây không ghi nhận
`FATAL EXCEPTION`, `BadTokenException` hoặc `OutOfMemoryError`. Contract frame chính xác
(cadence, one-way Special 2 và transition) được khóa bằng JVM test xác định
thay vì suy đoán state từ screenshot của ba pet tự chủ.

V3.16 được cài đè tiếp trên cùng thiết bị và giữ nguyên ba `Satoru Gojo`. Capture launcher
xác nhận pet dừng ở bottom dùng pose đứng thay vì frame 11 ngồi; hai mẫu liên tiếp còn
ghi nhận một window đứng nguyên X trong khi các pet khác tiếp tục di chuyển/nói. Service
vẫn có đúng ba window 238×238 px và log không có fatal, bad-token hoặc OOM.

V3.17 được cài đè trên Pixel 3 XL / API 31 và giữ nguyên selection ba `Satoru Gojo`.
Các capture nhẹ theo mốc thời gian ghi nhận ba pet tách nhịp độc lập, một pet leo lên
tường trong khi pet khác ở ground, Special có frame skill rõ và speech vẫn có window
riêng. Sau chu kỳ quan sát, WindowManager còn đúng ba overlay 238×238 px ở ba tọa độ
khác nhau, service vẫn foreground và log không có fatal, bad-token, OOM hoặc ANR.
Policy một vòng của Special, kể cả clip import khai báo loop, được khóa thêm bằng JVM
regression test vì screenshot không phải nguồn đủ chính xác để suy ra lifecycle frame.
