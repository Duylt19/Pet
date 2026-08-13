# UI Structure and Flow Map

Tài liệu này là bản đồ nhanh để tìm source UI. Package được chia theo domain
sản phẩm; bên trong domain mới chia theo screen/flow.

## Cây package

```text
ui/
├── app/
│   └── MainViewModel.kt
├── onboarding/
│   ├── splash/
│   ├── language/
│   ├── intro/
│   └── permission/
├── home/
│   ├── shell/                  # Home chrome, bottom navigation và banner owner
│   └── discover/
├── battery/
│   ├── catalog/
│   ├── favoriterecent/
│   └── editor/
├── pet/
│   ├── store/
│   └── room/
├── settings/
│   ├── mine/
│   └── permissions/
├── search/
├── premium/
└── shared/
    ├── component/
    └── theme/
```

`app/src/test` và `app/src/screenshotTest` mirror đúng package trên. Khi di chuyển
feature phải di chuyển test cùng lúc.

## Route → source

| Flow | Route | Package sở hữu |
|---|---|---|
| App entry | n/a | `ui/app` |
| Home graph | `home_graph` (internal, không phải screen) | `navigation/HomeNavGraph` + `ui/home/shell` |
| Onboarding | `splash`, `language`, `intro`, `permission` | `ui/onboarding/*` |
| Discover tab | `discover` | `ui/home/discover` |
| Battery tab/detail | `battery_catalog`, `battery_category/{id}` | `ui/battery/catalog` |
| Battery collection | `favourite_recent` | `ui/battery/favoriterecent` |
| Status bar editor | `battery_editor/*` | `ui/battery/editor` |
| Shimeji Pets tab | `pet_store` | `ui/pet/store` |
| My Pet Room | `my_pet` | `ui/pet/room` |
| Mine tab | `settings` | `ui/settings/mine` |
| Permission manager | `grant_permissions`, `accessibility_how_to_use` | `ui/settings/permissions` |
| Search | `search` | `ui/search` |
| Premium | `premium/*` | `ui/premium` |

## Luật ownership

- `Screen`, `ViewModel`, `UiState` và component chỉ dùng trong feature nằm cùng package.
- Chỉ đưa UI vào `ui/shared/component` khi có nhiều feature sử dụng hoặc có
  contract tái sử dụng rõ ràng.
- `ui/shared/theme` là nguồn typography/color theme của toàn app; feature không tự
  tạo theme package khác.
- `navigation/AppNavGraph.kt` và `HomeNavGraph.kt` chỉ wiring route/callback. Business state
  vẫn thuộc ViewModel của feature.

## Home shell

Bốn tab top-level vẫn thuộc các domain riêng:

```text
Discover              Battery              Shimeji Pets          Mine
ui/home/discover      ui/battery/catalog   ui/pet/store          ui/settings/mine
        \________________ nested HomeNavGraph (`home_graph`) ______________/
                         ui/home/shell Home chrome
```

`AppNavGraph` giữ một `NavController`; `HomeNavGraph` chỉ nhóm bốn destination root chứ không
tạo NavController thứ hai. `ui/home/shell` sở hữu bottom navigation/banner ad, vì vậy không
di chuyển các composable này vào từng tab. Việc package các tab khác nhau không làm thay đổi
back stack hay lifetime của banner.
