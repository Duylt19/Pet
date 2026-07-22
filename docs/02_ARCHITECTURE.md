# 02 — Architecture Contract

## Tổng quan

```text
:app
  MainActivity + AppNavGraph
            ↓
  Feature Screen / ViewModel / UiState
            ↓
        Use case (optional)
            ↓
      Repository interface
            ↓
  Repository implementation / DataStore / platform API

:ads
  Ads SDK + remote config + ad UI/utilities
```

Dependency đi từ presentation xuống data boundary. Composable không truy cập storage/network/service trực tiếp.

## Cấu trúc hiện hành

```text
com.asianmobile.privatebrower/
├── BaseApplication.kt
├── MainActivity.kt
├── constant/
├── data/
│   ├── local/                  # DataStoreManager
│   ├── model/                  # Domain/data models nhỏ
│   ├── repository/             # Interface
│   │   └── impl/               # Implementation
│   └── usecase/                # Nghiệp vụ tái sử dụng/testable
├── di/                         # Hilt modules đang có dependency thật
├── navigation/                 # Routes, NavGraph, safe navigation
├── ui/
│   ├── component/              # Shared stateless UI
│   ├── splash/
│   ├── language/
│   ├── intro/
│   ├── permission/
│   ├── home/                   # Home + settings
│   ├── premium/
│   ├── searchengine/
│   ├── main/
│   └── theme/
└── utils/                      # Platform/helper cross-feature nhỏ
```

## Feature template

Mỗi screen mới mặc định:

```text
ui/feature/
├── FeatureScreen.kt
├── FeatureViewModel.kt
└── FeatureUiState.kt
```

- Screen collect `StateFlow` lifecycle-aware, render state, gửi callback/action.
- ViewModel chứa orchestration/business logic, dùng `viewModelScope`.
- UiState immutable và đủ biểu diễn loading/content/empty/error.
- Component tái sử dụng trong feature đặt cùng package; dùng `ui/component` chỉ khi thật sự cross-feature.

## Data boundary

- Interface repository cho phép thay data source và test ViewModel/use case.
- Implementation không leak entity/SDK object lên UI nếu model đó không thuộc UI contract.
- Use case không bắt buộc cho CRUD một dòng; dùng khi logic phối hợp nhiều nguồn, có policy hoặc cần reuse/test riêng.
- DataStore cho key-value nhỏ; Room chỉ thêm lại khi có requirement về dữ liệu quan hệ/offline.
- Service/WorkManager chỉ dùng khi công việc phải sống ngoài lifecycle UI.

## DI

- `@Binds` cho interface → implementation.
- `@Provides` cho object cần factory/configuration.
- Scope phải phản ánh lifetime (`@Singleton` chỉ khi thực sự app-wide).
- Không giữ module rỗng, binding cũ hoặc dependency không dùng.

## Navigation boundary

- `AppNavGraph` sở hữu NavController và route wiring.
- Feature Screen nhận callback như `onBack`, `onOpenSettings`; không nhận NavController nếu không có lý do đặc biệt.
- Back-stack behavior là một phần contract và phải được document/test.

## Cách mở rộng base

1. Chốt requirement/domain boundary.
2. Tạo feature UI contract.
3. Thêm model/repository/use case tối thiểu cần thiết.
4. Wiring Hilt và navigation.
5. Thêm resources/analytics/tests.
6. Cập nhật docs cùng commit.
