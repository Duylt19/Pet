# Cute Pet/Shimeji — Documentation Index

Đây là nguồn tài liệu hiện hành cho AI agent và developer. Bộ docs cũ mô tả Private Browser đã được loại bỏ để tránh tham chiếu class, route và feature không còn trong source.

> Package canonical duy nhất: `com.asianmobile.emojibattery.shimeji`.
> Xem [PACKAGE_IDENTITY.md](PACKAGE_IDENTITY.md) trước khi sửa namespace, Firebase,
> external app data path hoặc các fully qualified class name.

## Thứ tự đọc

1. [Project overview](01_PROJECT_OVERVIEW.md)
2. [Architecture contract](02_ARCHITECTURE.md)
3. [Navigation flow](04_NAVIGATION_FLOW.md)
4. [Agent coding guidelines](08_AGENT_CODING_GUIDELINES.md)
5. [Current screens](screens/README.md)
6. Tài liệu chuyên đề liên quan task

## Foundation

| File | Nội dung |
|---|---|
| [PACKAGE_IDENTITY.md](PACKAGE_IDENTITY.md) | Package canonical, source roots và legacy identifier |
| [01_PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md) | Identity và phạm vi sản phẩm |
| [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | Module, layer, feature template |
| [03_TECH_STACK.md](03_TECH_STACK.md) | Stack/dependency hiện tại |
| [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md) | Routes và back stack |
| [05_DATA_MODEL.md](05_DATA_MODEL.md) | DataStore/model/repository còn lại |
| [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md) | Resource, sizing, component rules |
| [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md) | Ads/billing integration contract |
| [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md) | Checklist bắt buộc cho agent |
| [09_IMPLEMENTATION_ROADMAP.md](09_IMPLEMENTATION_ROADMAP.md) | Roadmap phát triển Cute Pet/Shimeji |
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

## Owner data tools

- [Clone and audit an authorized pet data snapshot](tools/PET_DATA_SNAPSHOT.md)

## Quy tắc duy trì

- Code và docs phải thay đổi trong cùng commit khi architecture/flow thay đổi.
- Không tạo spec cho feature chưa được owner xác nhận.
- Không để docs của feature đã xóa trong index hiện hành.
- Tài liệu lịch sử, nếu thật sự cần, phải nằm dưới `docs/archive/` với nhãn `ARCHIVED — NOT CURRENT`.
- Không lưu credential/token trong Markdown.
