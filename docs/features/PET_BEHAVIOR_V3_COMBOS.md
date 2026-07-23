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

- **Once**: clip chỉ chạy một lần. Dùng cho blink/wink, trip, tap vì lặp những động tác này
  trông giống lỗi animation.
- **Sustained**: clip/pose được giữ hoặc lặp liền mạch đến một duration seeded. Dùng cho
  sit, idle, look, walk/run/creep và dangle. Từ V3.15, Special dùng playback riêng
  `HOLD_LAST_FRAME` thay vì lặp cả clip.

Mỗi combo đi theo nhịp `anticipation → primary action → recovery`. Khoảng nghỉ tự chủ giữa
hai combo là 4–8 giây; pet không phải lúc nào cũng “diễn”. Duration beat không bị rút ngắn
bởi slider speed. V3.15 còn tách mức ảnh hưởng speed lên frame theo ngữ nghĩa, nên tăng tốc
di chuyển không làm cảm xúc chớp nhanh hơn.

## Combo catalog

Catalog hiện có 25 solo/user story và 13 role/approach social. 12 combo nền được cân lại
theo từng beat:

| Combo | Anticipation | Primary action | Recovery | Mục đích |
|---|---|---|---|---|
| `CURIOUS_SCOUT` | Walk 4–7s | Idle 2–4s → Look 3–5s → Talk-walk 9–11s | Idle 1.5–2.5s → Creep 4–7s | Đi tuần, nói khi bước chậm rồi dừng trước khi rón rén |
| `COZY_BREAK` | Idle 3–5s | Sit 7–12s → Talk 9–11s | Look 3–5s → Sit 4–7s | Một lần nghỉ dài, không phải ngồi rồi bật dậy |
| `HAPPY_ZOOMIES` | Idle 2–3.5s → Wink once | Run 3.5–6s | Walk 3–5s → Idle 3–5s | Lấy đà, chạy vui, hạ tốc rồi thở |
| `SHY_SNEAK` | Idle 3–5s | Creep 5–8s → Look 3–5s | Idle 2.5–4.5s | Rón rén có dừng nghe/ngó |
| `CLUMSY_RECOVERY` | Run 2.5–4s | Trip once | Sit 7–11s → Talk 9–11s → Wink once | Vấp một lần, hồi phục lâu rồi trấn an |
| `TINY_PERFORMANCE` | Sit 3–5s | Special 4.5–7s → pause 2–3.5s → Special 2 4.5–7s | Talk 9–11s → Sit 4–7s | Hai tiết mục có nghỉ giữa và pose kết |
| `DAYDREAM` | Sit 6–10s | Look 4–7s → Dangle 4–7s → Sit 2.5–4s | Talk → Sit 5–9s | Có pose ngồi hồi phục trước khi nói, không chuyển thẳng từ pose treo |
| `BUSY_PATROL` | Walk 6–10s | Idle 2–4s → Run 2.5–4.5s | Look 3–5s | Tuần tra có checkpoint thay vì đi liên tục |
| `PEEK_AND_DASH` | Creep 5–8s | Look 3–5s → pause 1.5–3s | Run 2.5–4.5s | Thăm dò, xác nhận rồi mới chạy |
| `SLOW_MORNING` | Idle 5–9s | Sit 8–14s → Look 3–6s | Walk 4–7s | Nhịp chậm nhất, ưu tiên nghỉ |
| `BRAVE_EXPLORER` | Turn → Look 3–5s | Run 3–5s → pause 2–4s | Creep 4–7s | Đổi hướng có chủ ý, khám phá theo hai tốc độ |
| `CHEERFUL_ENCORE` | Turn → Special 2 4–6.5s | Sit 3–5s → Special 4–6.5s | Sit 2.5–4s → Talk → Idle 3–5s | Encore có pose tách skill và recovery trước khi nói |

V3.2 thêm 8 choreography dùng chính action/frame mà pack đã khai báo:

| Combo | Choreography | Chi tiết tự nhiên |
|---|---|---|
| `WALL_PARKOUR` | Run tới mép → climb 10–16s → dangle 6–10s → jump vào trong → fall/bounce → sit → talk | Leo nhanh 1,8× rồi giữ pose trên tường đủ lâu; chỉ nói sau landing |
| `CEILING_EXPEDITION` | Run tới mép → climb tới trần → ceiling walk 12–20s → dangle 8–14s → jump/fall → bounce/look → talk | Hai chặng tiếp cận hoàn tất bằng collision thật; Look là recovery mặt đất trước khi nói |
| `WALL_DIVE` | Run tới mép → climb 8–13s → wall jump → fall/bounce → Special → sit → talk | Landing, skill và recovery tách rõ; nếu tường kết thúc sớm, jump thay cho chuyển sai sang ceiling |
| `SKY_DIVER` | Anticipation → boosted jump → fall → bounce → sit → talk → wink | Giữ đầy đủ landing và speech recovery thay vì đổi thẳng sang idle |
| `NINJA_SKILL` | Creep → sprint → boosted jump/fall → bounce → Special → sit → talk | Chỉ phát skill sau khi đã landing, có stealth, burst và cooldown rõ ràng |
| `BATTLE_DANCE` | Look → Special → pause → Special 2 → sit → talk | Hai skill có nhịp nghỉ; bỏ Dangle không đúng ngữ nghĩa mặt đất |
| `MAGIC_RITUAL` | Sit → look → Special 2 → idle focus → Special → sit → talk | Ritual dài, hai skill bắt buộc và có charge-up/release/recovery |
| `ACROBATIC_FINALE` | Run → boosted jump/fall/bounce → Special → sit → talk | Combo biểu diễn có take-off, landing và final recovery |

`motionMultiplier` chỉ tăng displacement cho beat được biên đạo (run-to-wall 1,15×,
climb-to-ceiling 2,4×, jump 2,2×), không tăng tốc animation pose hoặc toàn bộ pet.

Ngoài ra có combo riêng cho phản ứng tap/showcase và 6 scene social, mỗi scene có hai vai:

- `GREETING`: một pet chờ 2–3,5s, wink một lần rồi ngồi 5–8s; pet kia nhìn 3–5s,
  phản ứng trễ và ngồi 4–7s;
- `PLAY_CHASE`: leader/follower chạy cùng hướng 5–8,5s rồi có recovery; follower có thể
  trip đúng một lần và ngồi nghỉ;
- `SHOW_AND_REACT`: performer giữ mỗi Special 5–8s, có pause; observer nhìn 4–7s và ngồi
  cổ vũ 7–11s thay vì spam wink;
- `REST_TOGETHER`: hai pet ngồi lệch nhịp tổng khoảng 19–30s;
- `COPYCAT`: pet B trễ 1,5–2,5s rồi mới copy Look/Sit của pet A, tạo call-and-response.
- `DUET_DANCE`: hai pet đối mặt và luân phiên hai lượt `SPECIAL`/`SPECIAL_2`; các khoảng
  Sit/Idle được tính để không có cửa sổ skill chồng nhau, tạo call-and-response thật.

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
`nonClimbComboStreak` tồn tại xuyên qua vòng đời combo. Sau tối đa hai story không leo,
selector chỉ chọn `WALL`/`CEILING` ở lượt kế tiếp. Khi pack không đủ required action cho
climb, selector bỏ quota và fallback về các story tương thích thay vì mắc kẹt.

Multi-pet social bắt đầu sau 12 giây thay vì 6 giây để từng pet có cơ hội chọn autonomous
story đầu tiên. Contract V3.14 bên dưới tiếp tục giảm social occupancy bằng invitation
chance, range guard và cooldown dài hơn.

## V3.4 — wall-to-wall leap

`WALL_TO_WALL_LEAP` thêm một traversal story hoàn chỉnh:

1. chạy tới tường gần nhất và leo cao 12–18 giây;
2. giữ `DANGLE` 3–5 giây làm anticipation;
3. `JUMP` đảo hướng vào màn hình;
4. `FALL` di chuyển ngang hết usable width trong khoảng 1,1 giây;
5. collision cạnh đối diện chuyển thẳng sang `CLIMB_WALL`, không hủy combo;
6. leo tiếp 5–8 giây, treo 6–10 giây, nhảy vào trong rồi fall/bounce/sit.

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
- `comboBeatTargetMillis` được draw deterministic khi bắt đầu beat. One-shot thường chỉ
  chuyển ở cuối clip; sustained pose/locomotion repeat đến target. Beat
  `HOLD_LAST_FRAME` chạy clip đúng một lượt, giữ frame cuối và chỉ chuyển khi target hết.
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

Tap chạy `TAPPED → IDLE recovery → TALK → WINK`; showcase nói sau final sit. Skill leo,
bay và wall-to-wall chỉ nói sau khi đã landing/recovery, nên box không che lên frame đang
chạy, rơi hoặc leo. Greeting dùng TALK ngay cho vai A, còn vai B ngồi chờ 9–11 giây trước
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
transition vào speech. Climb/dangle/airborne không thể bị chuyển trực tiếp sang TALK.
Catalog còn khóa speech predecessor: các story leo/bay phải landing rồi qua
`BOUNCE`/`SIT`/`LOOK`/ground recovery trước khi nói; `DAYDREAM` thêm SIT giữa DANGLE và
TALK.

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
   bounce/wink/trip/jump/tapped; Special dùng 320/380/420/500/800 ms và Special 2 dùng
   320/360/420/520/800 ms trước speed policy. Frame trùng ngược của Special 2 được loại
   theo file ở runtime, không mutate revision 4 trên disk và không yêu cầu reinstall.
3. **Skill playback policy**: mọi beat `SPECIAL`/`SPECIAL_2` trong catalog dùng
   `HOLD_LAST_FRAME`. Clip phát trọn một lần, giữ final pose cho hết target 4–9 giây rồi
   mới vào recovery. Mỗi skill beat đồng thời là `requiredAction`, nên pack thiếu đúng
   frame performance sẽ loại story đó thay vì chạy một combo bị khuyết. Renderer bỏ scale
   luân phiên trên hai action này.

Choreography cũng được audit lại theo surface và ý nghĩa frame: Ninja bắt buộc
`FALL → BOUNCE → SPECIAL`; Battle Dance và Magic Ritual bỏ Dangle trên mặt đất; Wall Dive,
Encore, social showcase và các skill story có Sit recovery trước TALK; Happy Zoomies im
lặng và hạ nhịp bằng WALK/IDLE. Social Duet dùng timeline hai vai không overlap:

- vai A: Sit → Special → Idle dài → Special 2 → Sit;
- vai B: Idle dài → Special 2 → Sit dài → Special → Wink.

`PetState.isHoldingComboBeatFrame` chỉ tồn tại trong engine thuần Kotlin. Khi đang hold,
frame index và vị trí không restart; elapsed beat vẫn tăng và transition/effect tiếp theo
chỉ phát một lần khi target kết thúc.

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
chance; không chiếm pet đang drag, fling, fall, jump, dangle hoặc climb; approach/performance
đều có timeout; mất instance hoặc mất đúng social combo ownership sẽ hủy session an toàn.
`PetCrowdResolver` không sửa cặp social và không chặn pet đang di chuyển. Với một pet,
director không phát directive social.

## Verification

- JVM test kiểm tra catalog degrade/required-action theo pack, combo loop/one-shot chạy đúng
  thứ tự, combo completion, anti-repeat, long Sit hold và Special play-once/hold-final.
- JVM test khóa hướng mép gần nhất, run-to-wall, wall-to-ceiling, inward wall jump,
  distance/velocity timeout, fall-to-bounce và việc giữ nguyên combo qua collision.
- JVM test khóa autonomous pool 17 story, climb weight tối thiểu 25%, quota sau hai
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
  Ninja landing trước skill, toàn bộ Special beat dùng `HOLD_LAST_FRAME` và hai lượt duet
  không overlap.
- Device smoke test cần chạy với 2–3 pet để quan sát đủ approach và ít nhất hai scene liên
  tiếp; overlay vẫn phải có đúng một foreground service và một shared render clock.

V3.15 đã smoke-test trên Pixel 3 XL / API 31 ở speed 150% với owner pack `Satoru Gojo`
đang chọn sẵn: APK cài đè không mất selection/count, service tạo đúng ba window 238×238
px và tiếp tục chạy trên launcher. Chuỗi capture 15 giây không ghi nhận
`FATAL EXCEPTION`, `BadTokenException` hoặc `OutOfMemoryError`. Contract frame chính xác
(cadence, one-way Special 2, hold-final và transition) được khóa bằng JVM test xác định
thay vì suy đoán state từ screenshot của ba pet tự chủ.
