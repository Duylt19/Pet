# Cute Pet Base — Documentation Index

Đây là nguồn tài liệu hiện hành cho AI agent và developer. Bộ docs cũ mô tả Private Browser đã được loại bỏ để tránh tham chiếu class, route và feature không còn trong source.

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
| [01_PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md) | Identity và phạm vi base |
| [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | Module, layer, feature template |
| [03_TECH_STACK.md](03_TECH_STACK.md) | Stack/dependency hiện tại |
| [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md) | Routes và back stack |
| [05_DATA_MODEL.md](05_DATA_MODEL.md) | DataStore/model/repository còn lại |
| [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md) | Resource, sizing, component rules |
| [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md) | Ads/billing integration contract |
| [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md) | Checklist bắt buộc cho agent |
| [09_IMPLEMENTATION_ROADMAP.md](09_IMPLEMENTATION_ROADMAP.md) | Cách phát triển product mới từ base |
| [10_SCREEN_TRACKING.md](10_SCREEN_TRACKING.md) | Analytics screen names hiện tại |

## Runtime specs

- [Current features](features/README.md)
- [Pet pack v1 contract](features/PET_PACKS.md)
- [Current screens](screens/README.md)

## Quy tắc duy trì

- Code và docs phải thay đổi trong cùng commit khi architecture/flow thay đổi.
- Không tạo spec cho feature chưa được owner xác nhận.
- Không để docs của feature đã xóa trong index hiện hành.
- Tài liệu lịch sử, nếu thật sự cần, phải nằm dưới `docs/archive/` với nhãn `ARCHIVED — NOT CURRENT`.
- Không lưu credential/token trong Markdown.
