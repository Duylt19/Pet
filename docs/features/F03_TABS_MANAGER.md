# F03 - Tabs Manager

Quan ly da tab gom add, close, switch, thumbnail, persistence va batch selection.

## State Model

```kotlin
data class TabUi(
    val id: Long,
    val title: String,
    val url: String,
    val thumbnailPath: String?,
    val isActive: Boolean,
    val thumbnailTimestamp: Long,
)

data class TabSelectionUiState(
    val isIncognito: Boolean,
    val tabs: List<TabUi>,
    val selectedTabIds: Set<Long>,
    val showMoreMenu: Boolean,
    val isProcessing: Boolean,
)
```

`areAllTabsSelected` chi true khi danh sach tab khong rong va moi ID hien tai deu nam trong `selectedTabIds`. Selection rong vi vay khong bao gio bi hieu nham la "Deselect All".

## Core Operations

| Operation | Logic |
|---|---|
| `addTab(url, isIncognito)` | Persist Normal tab, tao WebView va set active |
| `ensureActiveTab(url, isIncognito)` | Tra active tab hien tai hoac tao dung mot starter tab neu browser rong |
| `closeTab(id)` | Destroy WebView, xoa Normal tab khoi Room/cache va chon active tab gan nhat |
| `closeAllInMode(isIncognito)` | Chup danh sach ID cua mode roi dong tung tab |
| `switchTo(id)` | Doi `activeTabId`, khong reload trang |
| `captureThumbnail(id)` | Render WebView vao bitmap va luu `cacheDir/tabs/{id}.jpg` |

Gioi han hien tai: 10 Normal tabs va 5 Incognito tabs.

`addTab()` va `ensureActiveTab()` dung chung `tabCreationMutex`. Khi bam `+` o Tabs,
`TabsTabScreen` phai doi `addTab()` hoan tat roi moi navigate sang Browser. Khong duoc khoi tao
tab bang `viewModelScope.launch` roi navigate ngay, vi `BrowserViewModel` co the thay
`activeTabId == null` va tao trung starter tab.

## Selection Flow

`TabsTabScreen` chi quan ly flow tab thuong. `TabSelectionScreen` la navigation destination rieng, nhan mode qua argument `incognito` va co ViewModel rieng.

Search trong `TabsTabScreen` la UI mode noi bo, khong tao navigation route. Khi active, `HomeScreen` an bottom navigation nhung van giu banner va ap dung Compose IME inset cho bottom bar de grid, search bar, banner va ban phim khong de len nhau. Search loc realtime theo title/URL va giu query khi user chuyen Normal/Private mode.

Batch actions luon map selected IDs ve danh sach tab hien tai truoc khi chay:

- Select all: chon moi ID trong mode hien tai.
- Deselect all: xoa selection khi tat ca da duoc chon.
- Close: dong cac tab dang chon; neu dong het mode thi pop man selection.
- Bookmark: chi insert URL HTTP/HTTPS hop le, bo qua bookmark da ton tai va dem dung so row insert thanh cong.
- Share: phat one-shot event de UI mo Android Sharesheet.

Trong luc Close/Bookmark dang chay, click grid va menu bi khoa de ngan double-submit.

## Persistence Va Privacy

### Normal tabs

- Luu trong Room khi tao.
- Cap nhat title, URL va thumbnail khi page load xong.
- Restore sau khi process khoi dong lai.
- Xoa Room row va thumbnail cache khi dong.

### Incognito tabs

- Chi giu in-memory voi ID am.
- Khong insert Room va khong restore sau process death.
- Khi dong private tab cuoi cung, incognito browser profile duoc clear.

## Edge Cases

| Truong hop | Xu ly |
|---|---|
| Selected tab bi dong tu flow khac | UI giao selection voi ID con ton tai |
| Empty mode | Hien empty state; menu actions khong nhan click |
| Close all roi bam `+` | Tao mot tab, doi cap nhat active ID xong moi navigate Browser |
| Thumbnail cache miss | Hien placeholder |
| Title rong | Fallback URL, sau do `New Tab` resource |
| Bookmark URL noi bo | Bo qua |
| Bookmark trung | Khong insert va thong bao khong co bookmark moi |
| Khong co share target | Hien toast, khong crash |

## Lien Quan

- [F01_BROWSER_CORE.md](F01_BROWSER_CORE.md)
- [F02_INCOGNITO_MODE.md](F02_INCOGNITO_MODE.md)
- [S06b_TABS_TAB.md](../screens/S06b_TABS_TAB.md)
- [S07_BROWSER_WEBVIEW.md](../screens/S07_BROWSER_WEBVIEW.md)
