# F09 - Clear History (Clear Browsing Data)

## 1. Nguyen tac bat buoc

- `HistoryEntity` chi thuoc Normal browsing. Private khong duoc ghi Room history.
- UI phai cho chon ro `NORMAL`, `PRIVATE` hoac `ALL` truoc khi xoa.
- Khong mac dinh dong open tabs. `clearOpenTabs` mac dinh la `false`.
- Khong xoa bookmarks, downloads, DataStore settings hay premium state.
- Giu `PrivateBrowserDatabase` o version 1 trong giai doan development; khong them migration.

## 2. Behavior matrix

| Du lieu | Normal | Private | All |
|---|---|---|---|
| Room history | Xoa neu chon History | Khong ton tai, khong hien option | Xoa Normal history |
| WebView back/forward history | Chi Normal WebView | Khong hien option | Tat ca WebView |
| Cache | Cache dung chung cua app | Cache dung chung cua app | Cache dung chung cua app |
| Cookies/WebStorage | Default profile | Incognito profile | Ca hai profile |
| Open tabs | Chi Normal | Chi Private | Ca hai mode |

Khi xoa Cookies and site data cua Private, tat ca Private tab phai dong truoc khi go
profile `private_browser_incognito`, vi Android WebView khong cho delete profile con WebView song.

`WebView.clearCache(true)` la API theo application, khong the xoa rieng cache Normal/Private.
UI phai canh bao dieu nay khi scope khong phai `ALL`.

## 3. WebView compatibility

`BrowserEngine.supportsProfileIsolation()` kiem tra `WebViewFeature.MULTI_PROFILE`.

- Co ho tro: Normal dung default profile, Private dung `private_browser_incognito`.
- Khong ho tro: app khong duoc tu nhan da tach du lieu hoan toan. UI hien canh bao va ket
  qua clear tra `profileIsolationLimited = true`.
- Private-only tren provider cu: dong Private WebView va xoa kho site data dung chung. UI phai
  bao ro Normal cung bi anh huong; day la cach duy nhat de bao dam request xoa thuc su co hieu luc.
- All tren provider cu: co the xoa global cookie jar vi user da chon ca hai mode.
- Khi provider ho tro `DELETE_BROWSING_DATA`, dung `WebStorageCompat.deleteBrowsingData()` de
  xoa cookie, network cache va JavaScript-readable storage day du. Provider cu fallback ve
  `CookieManager.removeAllCookies()` va `WebStorage.deleteAllData()`.

## 4. Implementation

- Options va scope: `data/usecase/ClearBrowsingDataUseCase.kt`.
- Xoa WebView back/forward history: `TabManager.clearNavigationHistory()`.
- Xoa application-wide WebView cache: `TabManager.clearWebViewCache()`.
- Dong tab theo mode: `TabManager.closeAllInMode()`.
- Dong Private tabs va xoa profile trong cung critical section:
  `TabManager.clearIncognitoBrowsingSession()`.
- Xoa profile thuc te: `BrowserEngine.clearIncognitoProfile()`.
- Bottom sheet: `ui/home/settings/ClearHistorySheet.kt`.
- `HistoryRepository` dung visit generation de callback cua page cu khong ghi lai history sau
  khi `deleteAll()` da chay.

Thu tu khi clear Private site data:

1. Neu chon Cache, clear application-wide WebView cache.
2. Dong tat ca Private tabs va destroy WebView tren main thread.
3. Delete incognito profile truoc khi cho phep tao tab moi.
4. Khong cham vao Room history.

## 5. Verification

- Clear Normal khong dong Private tabs khi `clearOpenTabs = false`.
- Clear Private khong xoa Room history.
- Clear Private site data dong Private tabs truoc khi delete profile.
- Clear All xoa Normal history va du lieu cua ca hai mode.
- Private navigation khong bao gio xuat hien trong History sau restart.
- Doi scope khong tu bat lai History hoac tu tat Open Tabs.
- Callback page load cu khong ghi lai Room history sau khi clear.
- History khong xoa form popup, find-in-page highlight hay SSL preferences.

## 6. Lien quan

- [F02_INCOGNITO_MODE.md](F02_INCOGNITO_MODE.md)
- [F04_BOOKMARKS_HISTORY.md](F04_BOOKMARKS_HISTORY.md)
- [S09_SETTINGS.md](../screens/S09_SETTINGS.md)
