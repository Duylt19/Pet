# 08 — Agent Coding Guidelines

Nguồn chi tiết bắt buộc: [android developer skill](../.agents/skills/android_developer/SKILL.md).

## Checklist trước khi sửa

- Đọc source và docs liên quan; không dựa vào docs Private Browser lịch sử.
- Xác nhận package name chưa được phép đổi.
- Xác định feature boundary, state ownership và navigation impact.
- Nếu UI từ Figma, phải xem screenshot/design context trước.

## Checklist khi implement

- Screen/ViewModel/UiState đúng package-by-feature.
- Hilt injection, StateFlow immutable, lifecycle-aware collection.
- Navigation callback ở screen boundary; route ở NavGraph.
- Repository/use case cho data/business logic phù hợp.
- String/color/spacing dùng resource.
- Permission/dependency/service chỉ thêm khi feature cần thật.
- Analytics và error/empty/loading state được xử lý.
- Không log hoặc commit secret.

## Checklist docs

- Source tree/layer đổi → architecture docs.
- Route/flow đổi → navigation + screens docs.
- Data/dependency đổi → data/tech docs.
- UI convention/ads/tracking đổi → đúng foundation doc.
- Feature xóa → xóa spec hiện hành hoặc archive có nhãn rõ.

## Checklist hoàn tất

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
git diff --check
```

Review `git status`/diff, sau đó commit bằng English message rõ nghĩa.
