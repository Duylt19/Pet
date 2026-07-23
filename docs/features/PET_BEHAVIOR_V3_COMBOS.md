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

Nguồn:

- https://github.com/gil/shimeji-ee/blob/master/conf/actions.xml
- https://github.com/gil/shimeji-ee/blob/master/conf/behaviors.xml
- https://github.com/libgdx/gdx-ai
- https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/src/com/badlogic/gdx/ai/btree/BehaviorTree.java
- https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/src/com/badlogic/gdx/ai/steer/behaviors/Arrive.java
- https://github.com/libgdx/gdx-ai/blob/master/gdx-ai/src/com/badlogic/gdx/ai/steer/behaviors/Separation.java

## Combo catalog

12 combo tự chủ:

1. `CURIOUS_SCOUT`: đi, quan sát, rón rén, nháy mắt.
2. `COZY_BREAK`: nghỉ, ngồi, nhìn lên, nháy mắt.
3. `HAPPY_ZOOMIES`: chào, chạy nhanh hai nhịp xen một nhịp nghỉ.
4. `SHY_SNEAK`: đứng yên, rón rén, nhìn quanh rồi tiếp tục rón rén.
5. `CLUMSY_RECOVERY`: chạy, vấp, ngồi dậy và trấn an.
6. `TINY_PERFORMANCE`: ngồi mở màn, diễn hai special rồi kết bằng biểu cảm.
7. `DAYDREAM`: nghỉ, nhìn lên, đung đưa, nhìn lại và nháy mắt.
8. `BUSY_PATROL`: tuần tra qua nhịp đi–chạy–đi–quan sát.
9. `PEEK_AND_DASH`: rón rén thăm dò rồi chạy đi.
10. `SLOW_MORNING`: nghỉ, ngồi, chào và bắt đầu đi.
11. `BRAVE_EXPLORER`: đổi hướng, quan sát rồi xen chạy/rón rén.
12. `CHEERFUL_ENCORE`: đổi hướng và diễn lại special theo thứ tự khác.

Ngoài ra có combo riêng cho phản ứng tap/showcase và 5 scene social, mỗi scene có hai vai:

- `GREETING`: một pet chào, pet kia nhìn và chào lại;
- `PLAY_CHASE`: leader chọn phía còn nhiều không gian, follower chạy cùng hướng;
- `SHOW_AND_REACT`: một pet biểu diễn special, pet còn lại quan sát/cổ vũ;
- `REST_TOGETHER`: hai pet ngồi nghỉ lệch nhịp để tránh cảm giác clone;
- `COPYCAT`: hai pet bắt chước cùng cử chỉ nhưng đảo thứ tự.

Tính cả approach, user reaction và hai vai social, catalog có 25 combo ID.

## Runtime contract

- `PetState.activeComboId` xác định combo đang chạy; `pendingRoutineActions` giữ các bước
  còn lại theo đúng thứ tự.
- Action one-shot chuyển bước khi timeline kết thúc. Action loop như `IDLE`, `WALK`, `RUN`
  và `CREEP` chuyển bước khi hết duration seeded của chính action đó.
- Chỉ khi queue rỗng và action cuối hoàn tất, engine phát `ComboCompleted`, clear combo rồi
  mới cho phép chọn combo tự chủ tiếp theo.
- `recentComboIds` chống lặp ở cấp câu chuyện, mặc định nhớ ba combo gần nhất.
- Combo được lọc bằng `supportedActions` của pack. Combo không còn ít nhất hai action khác
  nhau sẽ không đủ điều kiện; pack cũ tiếp tục đi/idle an toàn.
- Drag, fling, fall và collision biên có quyền ngắt combo vì gesture/physics phải ưu tiên.

## Social state machine

Phiên tương tác có hai pha:

1. `APPROACHING`: chọn cặp pet ở sàn gần nhau nhất, hai pet chạy về phía tâm của nhau và
   cập nhật facing mỗi frame. Khi đạt khoảng cách 1,35 pet-width thì chuyển pha.
2. `PERFORMING`: phát combo theo hai vai của scene, giữ facing phù hợp (đối mặt hoặc cùng
   hướng khi đuổi bắt), chờ cả hai combo hoàn tất rồi cooldown trước scene tiếp theo.

Các guardrail gồm: chỉ ghép pet đang rảnh trên cùng mặt sàn, không chiếm pet đang drag,
fling, fall, jump hoặc climb; approach/performance đều có timeout; mất một instance sẽ hủy
session an toàn. Với một pet, director không phát directive social.

## Verification

- JVM test kiểm tra catalog degrade theo pack, combo loop/one-shot chạy đúng thứ tự, combo
  completion và anti-repeat.
- JVM test kiểm tra approach direction, paired greeting, closest-pair selection, bỏ qua pet
  đang climb và no-op khi chỉ có một pet.
- Device smoke test cần chạy với 2–3 pet để quan sát đủ approach và ít nhất hai scene liên
  tiếp; overlay vẫn phải có đúng một foreground service và một shared render clock.
