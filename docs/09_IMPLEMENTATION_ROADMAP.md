# 09 — Product Development Roadmap

Đây là roadmap phương pháp, không tự giả định feature Cute Pet.

## Phase 0 — Product definition

- Chốt user problem, primary flows và Figma source.
- Chốt application ID mới, branding, icon và store metadata.
- Rà permission/SDK/privacy policy phù hợp sản phẩm mới.

## Phase 1 — Base adaptation

- Update package/root project name khi owner yêu cầu.
- Update theme, app icon, splash/intro copy và localization.
- Thiết kế Home final thay placeholder.
- Quyết định giữ/xóa search engine, clear browsing data và feedback actions legacy.

## Phase 2 — Feature vertical slices

Mỗi feature đi theo một vertical slice hoàn chỉnh:

1. Requirement + acceptance criteria.
2. Screen/ViewModel/UiState.
3. Model/repository/use case/data source.
4. Hilt/navigation/resources/analytics.
5. Loading/empty/error/accessibility.
6. Unit/UI tests.
7. Docs + compile/test + commit.

## Phase 3 — Release hardening

- Permission và privacy audit.
- Ads/billing behavior audit.
- Process death/offline/error testing.
- Accessibility/localization/device-size testing.
- Release compile/ProGuard verification và store configuration.

Không copy feature cũ trở lại chỉ để tiết kiệm thời gian; chỉ tái sử dụng pattern kiến trúc và code thực sự phù hợp requirement mới.
