# UI Structure and Flow Map

Tài liệu này là bản đồ nhanh để tìm source UI. Entry screen được chia theo surface runtime;
flow/component dùng lại giữa nhiều surface được giữ ở package domain.

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
│   ├── discover/               # Tab 1 entry
│   ├── battery/                # Tab 2 entry; compose reusable battery catalog flow
│   ├── pet/                    # Tab 3 entry; compose reusable pet store flow
│   └── mine/                   # Tab 4 entry và UI chỉ thuộc Mine
├── battery/
│   ├── catalog/                # Reusable flow/state + root category destination
│   ├── favoriterecent/
│   ├── editor/
│   └── troll/
├── pet/
│   ├── store/                  # Reusable reward/download/content flow
│   └── room/
├── permissions/                # App-wide grant/how-to destinations
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
| Home container | `home_graph` (root destination, không track analytics) | `navigation/HomeNavGraph` + `ui/home/shell` |
| Onboarding | `splash`, `language`, `intro`, `permission` | `ui/onboarding/*` |
| Discover tab | `discover` | `ui/home/discover` |
| Battery tab | `battery_catalog` | `ui/home/battery/BatteryHomeScreen` |
| Battery category | `battery_category/{id}` (root destination) | `ui/battery/catalog` |
| Battery collection | `favourite_recent` | `ui/battery/favoriterecent` |
| Status bar editor | `battery_editor/*` | `ui/battery/editor` |
| Shimeji Pets tab | `pet_store` | `ui/home/pet/ShimejiPetsScreen` |
| My Pet Room | `my_pet` | `ui/pet/room` |
| Mine tab | `settings` | `ui/home/mine/MineScreen` |
| Permission manager | `grant_permissions`, `accessibility_how_to_use` | `ui/permissions` |
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
- Home tab entry chỉ sở hữu lifecycle collection, screen tracking và composition của tab.
  Reward/download/access policy dùng bởi Discover/Search không được đặt trong một tab package.

## Home shell

Bốn tab top-level có entry screen cùng nằm dưới Home, còn reusable flow ở domain package:

```text
ui/home/discover   ui/home/battery   ui/home/pet   ui/home/mine
        \____________ HomeRoute nested NavHost (4 tab) ____________/
                         ui/home/shell Home chrome

BatteryHomeScreen ──uses──> ui/battery/catalog
ShimejiPetsScreen ──uses──> ui/pet/store
```

`AppNavGraph` giữ root `NavController` cho onboarding và các màn độc lập. `HomeRoute` tạo
`NavController` thứ hai chỉ cho bốn tab, còn `ui/home/shell` sở hữu bottom navigation/banner ad.
Vì vậy đổi tab không reload banner, trong khi Search/My Pet/category/editor được push lên root
graph như một surface mới và tự tạo lại ad của chính destination. Root transition bị tắt và
surface luôn opaque để màn trước không xuyên/nháy phía sau màn mới.
