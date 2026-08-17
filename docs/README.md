# Emoji Battery/Shimeji — Documentation Index

Đây là nguồn tài liệu hiện hành cho AI agent và developer. Bộ docs cũ mô tả Private Browser đã được loại bỏ để tránh tham chiếu class, route và feature không còn trong source.

> Package canonical duy nhất: `com.asianmobile.emojibattery.shimeji`.
> Xem [PACKAGE_IDENTITY.md](PACKAGE_IDENTITY.md) trước khi sửa namespace, Firebase,
> external app data path hoặc các fully qualified class name.

## Thứ tự đọc

1. [Project overview](01_PROJECT_OVERVIEW.md)
2. [Architecture contract](02_ARCHITECTURE.md)
3. [UI structure](UI_STRUCTURE.md)
4. [Navigation flow](04_NAVIGATION_FLOW.md)
5. [Agent coding guidelines](08_AGENT_CODING_GUIDELINES.md)
6. [Current screens](screens/README.md)
7. Tài liệu chuyên đề liên quan task

Khi cập nhật catalog hoặc asset server, bắt đầu từ
[`data/README.md`](data/README.md). Bộ tài liệu này mô tả kết nối, fetch/cache/download,
contract từng loại data, migration, publish và rollback.

## Foundation

| File | Nội dung |
|---|---|
| [PACKAGE_IDENTITY.md](PACKAGE_IDENTITY.md) | Package canonical, source roots và legacy identifier |
| [01_PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md) | Identity và phạm vi sản phẩm |
| [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | Module, layer, feature template |
| [UI_STRUCTURE.md](UI_STRUCTURE.md) | Cây package UI, ownership và route map |
| [03_TECH_STACK.md](03_TECH_STACK.md) | Stack/dependency hiện tại |
| [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md) | Routes và back stack |
| [05_DATA_MODEL.md](05_DATA_MODEL.md) | DataStore/model/repository còn lại |
| [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md) | Resource, sizing, component rules |
| [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md) | Ads/billing integration contract |
| [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md) | Checklist bắt buộc cho agent |
| [09_IMPLEMENTATION_ROADMAP.md](09_IMPLEMENTATION_ROADMAP.md) | Roadmap phát triển Emoji Battery/Shimeji |
| [10_SCREEN_TRACKING.md](10_SCREEN_TRACKING.md) | Analytics screen names hiện tại |

## Runtime specs

- [Current features](features/README.md)
- [Pet pack v1 contract](features/PET_PACKS.md)
- [Pet settings and session policy](features/PET_SETTINGS.md)
- [Current screens](screens/README.md)

## Research

- [Competitor technical audit](research/COMPETITOR_TECHNICAL_AUDIT.md)
- [Research index](research/README.md)

Research records evidence and clean-room decisions; it is not a runtime specification and contains no decompiled source or binary assets in Git. The separately authorized owner data snapshot is intentionally ignored from source control.

## Content server

App này là **client của một content server riêng**: pet, battery theme và room background không
nằm trong APK mà được publish sang repository
`Asian-Mobile-Inc/Server-Emoji-Battery-Shimeji-Pet-AM` (thường clone cạnh project này) và đọc
qua `raw.githubusercontent.com` nhánh `master`.

| Catalog | Config phía app | File phía server |
|---|---|---|
| Pet | `PetServerConfig` | `json/pets.json`, `data/<id>.zip`, `thumb/<id>.webp` |
| Battery | `BatteryServerConfig` | `json/batteries.json`, `battery/**` |
| Room | `RoomServerConfig` | `json/rooms.json`, `room/bg|thumb/BG_<id>.webp` |

Repo private nên mọi request gửi `Authorization: Bearer <token>`; token lấy từ Remote Config key
`github_token_pet_server` và default trong source luôn rỗng. Đọc cache-first, revalidate TTL 24h
+ ETag + rate-limit backoff, và mọi asset tải về đều verify byte size/SHA-256 theo catalog trước
khi dùng. Release chỉ nhận catalog `APPROVED`.

Thêm/sửa nội dung catalog là **thay đổi ở repo server**, không phải ở đây: build bằng
`tools/*_pipeline.py` của repo đó để size/SHA-256 được tính lại và chạy đúng validation của CI.
Ngoại lệ duy nhất là room `1`: nó vừa nằm trong catalog (`defaultRoomId`) vừa được đóng gói trong
APK qua `PetRoomBundledBackground` để phòng không bao giờ trống khi offline.

## Owner data tools

- [Clone and audit an authorized pet data snapshot](tools/PET_DATA_SNAPSHOT.md)
- [Audit and sync the local Battery snapshot](tools/BATTERY_DATA_SNAPSHOT.md)

## Planned and in-progress features

Các tài liệu dưới `plans/` là đặc tả đã research nhưng chưa phải capability hiện hành:

- [Battery Status Capsule current implementation](features/BATTERY_STATUS.md) và
  [phase plan](plans/battery-status-capsule/README.md): phân tích 14
  screenshot, standard/accessibility overlay modes, giới hạn Android/Play policy,
  product/UX, architecture, data/asset schema, navigation/state, monetization, test matrix
  và roadmap nhiều phase.

## Quy tắc duy trì

- Code và docs phải thay đổi trong cùng commit khi architecture/flow thay đổi.
- Không tạo spec cho feature chưa được owner xác nhận.
- Không để docs của feature đã xóa trong index hiện hành.
- Tài liệu lịch sử, nếu thật sự cần, phải nằm dưới `docs/archive/` với nhãn `ARCHIVED — NOT CURRENT`.
- Không lưu credential/token trong Markdown.
