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
  dùng duration theo milliseconds cho cùng mục tiêu: giữ một pose hoặc performance qua
  nhiều vòng clip trước khi transition.

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
  sit, idle, look, walk/run/creep, dangle và special performance.

Mỗi combo đi theo nhịp `anticipation → primary action → recovery`. Khoảng nghỉ tự chủ giữa
hai combo là 4–8 giây; pet không phải lúc nào cũng “diễn”. Duration beat không bị rút ngắn
bởi slider speed, nên tăng tốc di chuyển không làm cảm xúc chớp nhanh hơn.

## Combo catalog

Catalog giữ 20 solo story. 12 combo nền được cân lại theo từng beat:

| Combo | Anticipation | Primary action | Recovery | Mục đích |
|---|---|---|---|---|
| `CURIOUS_SCOUT` | Walk 4–7s | Idle 2–4s → Look 3–5s | Creep 4–7s | Đi tuần rồi dừng thật sự để quan sát |
| `COZY_BREAK` | Idle 3–5s | Sit 7–12s | Look 3–5s → Sit 4–7s | Một lần nghỉ dài, không phải ngồi rồi bật dậy |
| `HAPPY_ZOOMIES` | Idle 2–3.5s → Wink once | Run 3.5–6s | Idle 3–5s | Lấy đà, chạy vui rồi thở |
| `SHY_SNEAK` | Idle 3–5s | Creep 5–8s → Look 3–5s | Idle 2.5–4.5s | Rón rén có dừng nghe/ngó |
| `CLUMSY_RECOVERY` | Run 2.5–4s | Trip once | Sit 7–11s → Wink once | Vấp một lần, hồi phục lâu rồi trấn an |
| `TINY_PERFORMANCE` | Sit 3–5s | Special 4.5–7s → pause 2–3.5s → Special 2 4.5–7s | Sit 4–7s | Hai tiết mục có nghỉ giữa và pose kết |
| `DAYDREAM` | Sit 6–10s | Look 4–7s → Dangle 4–7s | Sit 5–9s | Một đoạn mơ màng dài, ít transition |
| `BUSY_PATROL` | Walk 6–10s | Idle 2–4s → Run 2.5–4.5s | Look 3–5s | Tuần tra có checkpoint thay vì đi liên tục |
| `PEEK_AND_DASH` | Creep 5–8s | Look 3–5s → pause 1.5–3s | Run 2.5–4.5s | Thăm dò, xác nhận rồi mới chạy |
| `SLOW_MORNING` | Idle 5–9s | Sit 8–14s → Look 3–6s | Walk 4–7s | Nhịp chậm nhất, ưu tiên nghỉ |
| `BRAVE_EXPLORER` | Turn → Look 3–5s | Run 3–5s → pause 2–4s | Creep 4–7s | Đổi hướng có chủ ý, khám phá theo hai tốc độ |
| `CHEERFUL_ENCORE` | Turn → Special 2 4–6.5s | Sit 3–5s → Special 4–6.5s | Idle 3–5s | Encore có pose tách hai tiết mục |

V3.2 thêm 8 choreography dùng chính action/frame mà pack đã khai báo:

| Combo | Choreography | Chi tiết tự nhiên |
|---|---|---|
| `WALL_PARKOUR` | Run tới mép → climb 10–16s → dangle 6–10s → jump vào trong → fall/bounce → sit | Leo nhanh 1,8× rồi giữ pose trên tường đủ lâu; jump đảo hướng tại beat |
| `CEILING_EXPEDITION` | Run tới mép → climb tới trần → ceiling walk 12–20s → dangle 8–14s → jump/fall → bounce/look | Hai chặng tiếp cận hoàn tất bằng collision thật; pet sinh hoạt trên trần 20–34s |
| `WALL_DIVE` | Run tới mép → climb 8–13s → wall jump → fall/bounce → Special | Leo nhanh 1,8×; nếu tường kết thúc sớm, jump thay cho chuyển sai sang ceiling |
| `SKY_DIVER` | Anticipation → boosted jump → fall → bounce → sit/wink | Giữ đầy đủ landing recovery thay vì đổi thẳng sang idle |
| `NINJA_SKILL` | Creep → sprint → boosted jump/fall → Special → sit | Có stealth, burst, skill và cooldown rõ ràng |
| `BATTLE_DANCE` | Look → Special → pause → Special 2 → Dangle → sit | Hai skill có nhịp nghỉ nên không flash liên tục |
| `MAGIC_RITUAL` | Sit → look → Special 2 → Dangle → Special → idle | Ritual dài 25–40s, có charge-up và release |
| `ACROBATIC_FINALE` | Run → boosted jump/fall/bounce → Special → sit | Combo biểu diễn có take-off, landing và final pose |

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
- `DUET_DANCE`: hai pet đối mặt và luân phiên `SPECIAL`/`SPECIAL_2`; vai B vào trễ để tạo
  call-and-response thay vì hai sprite phát cùng một frame.

Tính cả approach, user reaction và hai vai social, catalog có 37 combo ID.

## V3.3 — spatial balance

Catalog vẫn giữ toàn bộ story để dùng cho trigger cụ thể, nhưng pool tự chủ mặc định hiện có
16 combo:

- chỉ giữ 6 ground basic khác biệt nhất: `CURIOUS_SCOUT`, `COZY_BREAK`,
  `HAPPY_ZOOMIES`, `CLUMSY_RECOVERY`, `TINY_PERFORMANCE`, `DAYDREAM`;
- loại 6 ground basic trùng nhịp khỏi random pool: `SHY_SNEAK`, `BUSY_PATROL`,
  `PEEK_AND_DASH`, `SLOW_MORNING`, `BRAVE_EXPLORER`, `CHEERFUL_ENCORE`;
- giữ 5 climb (gồm hai hướng wall-to-wall), 3 aerial và 2 skill/dance story;
- climb có tổng weight 54/144, tức 37,5% trước khi áp quota.

Mỗi definition có `PetComboHabitat`: `GROUND`, `AERIAL`, `WALL` hoặc `CEILING`.
`nonClimbComboStreak` tồn tại xuyên qua vòng đời combo. Sau tối đa hai story không leo,
selector chỉ chọn `WALL`/`CEILING` ở lượt kế tiếp. Khi pack không đủ required action cho
climb, selector bỏ quota và fallback về các story tương thích thay vì mắc kẹt.

Multi-pet social bắt đầu sau 12 giây thay vì 6 giây để từng pet có cơ hội chọn autonomous
story đầu tiên; cooldown social tăng từ 10 lên 20 giây để social scene ở mặt đất không chiếm
phần lớn thời gian.

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
4. `WALL_TO_WALL_LEAP` cũ vẫn không có launch velocity nên tiếp tục là biến thể lao xuống.

`crossScreenLaunchVelocityY` chỉ hợp lệ khi âm, hữu hạn và đi cùng một collision-driven
`crossScreenDurationMillis`. Nhờ vậy các combo khác dùng `FALL` vẫn giữ physics rơi hiện
tại, còn upward rise không bị `initialFallSpeed` ghi đè.

## Runtime contract

- `PetState.activeComboId` xác định combo đang chạy; `activeComboBeat` giữ beat hiện tại;
  `pendingComboBeats` giữ các beat còn lại theo đúng thứ tự.
- `comboBeatTargetMillis` được draw deterministic khi bắt đầu beat. One-shot chỉ chuyển ở
  cuối clip; sustained one-shot tự restart liền mạch cho đến target; looping action chuyển
  khi đạt target.
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

## Social state machine

Phiên tương tác có hai pha:

1. `APPROACHING`: chọn cặp pet ở sàn gần nhau nhất, hai pet chạy về phía tâm của nhau và
   cập nhật facing mỗi frame. Khi đạt khoảng cách 1,35 pet-width thì chuyển pha.
2. `PERFORMING`: phát combo theo hai vai của scene, giữ facing phù hợp (đối mặt hoặc cùng
   hướng khi đuổi bắt), chờ cả hai combo hoàn tất rồi cooldown 20 giây trước scene tiếp theo.

Các guardrail gồm: chỉ ghép pet đang rảnh trên cùng mặt sàn, không chiếm pet đang drag,
fling, fall, jump hoặc climb; approach/performance đều có timeout; mất một instance sẽ hủy
session an toàn. Với một pet, director không phát directive social.

## Verification

- JVM test kiểm tra catalog degrade/required-action theo pack, combo loop/one-shot chạy đúng
  thứ tự, combo completion, anti-repeat, long Sit hold và sustained Special.
- JVM test khóa hướng mép gần nhất, run-to-wall, wall-to-ceiling, inward wall jump,
  distance/velocity timeout, fall-to-bounce và việc giữ nguyên combo qua collision.
- JVM test khóa autonomous pool 16 story, climb weight tối thiểu 25%, quota sau hai
  non-climb story, reset streak và fallback khi pack thiếu frame leo.
- JVM test chạy toàn bộ wall-to-wall traversal theo cả hai hướng, xác nhận thời gian
  screen-relative, cạnh đích, facing, opposite-wall catch và combo lifecycle không bị hủy.
- JVM test xác nhận upward variant giữ vận tốc Y âm sau takeoff, giảm tọa độ Y trong lúc
  bay và bắt tường đối diện ở vị trí cao hơn.
- JVM test kiểm tra approach direction, greeting/duet roles, closest-pair selection, bỏ qua
  pet đang climb và no-op khi chỉ có một pet.
- Device smoke test cần chạy với 2–3 pet để quan sát đủ approach và ít nhất hai scene liên
  tiếp; overlay vẫn phải có đúng một foreground service và một shared render clock.
