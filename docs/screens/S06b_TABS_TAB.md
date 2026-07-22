# S06b - Tabs Tab

## Muc Dich

Hien thi cac WebView dang mo theo Normal/Private mode, cho phep tim, mo, tao, dong va chuyen sang flow chon nhieu tab.

## Navigation

- `TabsTabScreen` nam trong trang thu hai cua `HomeScreen`.
- Bam tab card hoac nut tao tab: mo `BROWSER_WEBVIEW`.
- Bam `More > Select tabs`: mo route rieng `TAB_SELECTION?incognito={Boolean}`.
- Route chon tab khong hien Home bottom navigation, banner ad hay interstitial.

## Man Tabs Thuong

Header gom mode dropdown, Search va More. Danh sach tab dung `HorizontalPager`; action row ben duoi gom xoa tab hien tai, tao tab va quay lai Browser.

### Search mode

Bam Search tren header se chuyen Tabs sang search mode theo Figma nodes `11110:620` va `11110:670`:

- Header doi thanh Back + Normal/Private mode selector; Search va More duoc an.
- Home bottom navigation duoc an, banner ad van giu de search bar nam ngay tren banner va IME.
- Khi search active, window tam chuyen sang `adjustNothing`; `imePadding` la noi duy nhat xu ly chieu cao ban phim. Khi thoat search, window tro lai `adjustPan` cho Browser/WebView.
- Pager doi thanh grid 2 cot, loc realtime theo title hoac URL, khong phan biet hoa/thuong.
- Search field tu dong focus va mo ban phim; query rong hien Mic, co query hien Clear va border focused.
- Bam Mic mo Android speech recognizer; ket qua giong noi duoc dua ve query.
- Bam keyboard Search chi an IME, khong thoat search mode.
- Bam mot ket qua se activate tab, dong search mode va mo Browser.
- Bam Back tren header se clear focus/query, an IME va quay lai Tabs pager. System Back dong IME truoc theo hanh vi Android; neu IME da dong, Back tiep theo se thoat search mode.
- Doi Normal/Private trong luc search giu nguyen query va tiep tuc loc tren mode moi.
- Khong co ket qua hien empty state rieng, khong dung nham empty state "No tabs open".

Menu More theo Figma node `11117:5376`:

| Action | Hanh vi |
|---|---|
| Select tabs | Mo man chon tab cua mode hien tai |
| Close all tabs | Mo dialog xac nhan, sau do dong tat ca tab trong mode hien tai |

Menu rong 132sdp, nen `#333538`, bo goc 12sdp. Menu duoc anchor truc tiep vao icon More de giu dung vi tri khi kich thuoc man hinh thay doi.

## Man Chon Tab

`TabSelectionScreen` theo Figma node `11120:6663`:

- Man full-screen nen `#161718`.
- Header co Back, tieu de `Select tabs` va More.
- Tieu de header doi realtime: chua chon hien `Select tabs`, chon mot hien `1 Tab`, chon nhieu hien `{count} Tabs` theo Figma node `11405:9162`.
- Grid 2 cot, padding ngang 12sdp, gap 9sdp.
- Thumbnail co ti le `158 / 239`, bo goc 12sdp va checkbox tron o goc tren phai.
- Bam card de toggle selection; trang thai selection duoc quan ly bang `Set<Long>`.

Menu More theo Figma node `11322:10856`:

| Action | Enabled | Hanh vi |
|---|---|---|
| Select all tabs / Deselect All | Co tab | Chon tat ca; khi tat ca da duoc chon thi bo chon tat ca |
| Close all tabs | Co selection | Dong cac tab dang chon |
| Add Bookmark | Co selection | Them bookmark hop le va bo qua URL da ton tai |
| Share Tabs | Co selection | Mo Android Sharesheet voi title va URL cua cac tab dang chon |

Khi chua chon tab, ba action cuoi giu trong menu nhung hien opacity 30% va khong nhan click.

## State Va Event

- `TabsTabViewModel`: mode, pager tabs, search, mode dropdown, normal More menu va close-all dialog.
- `TabSelectionViewModel`: danh sach tab theo route mode, selected IDs, popup state va processing state.
- Bookmark, Close va Share phat `TabSelectionEvent` mot lan; Screen xu ly toast, Sharesheet va navigation.
- Selected IDs luon duoc giao voi IDs con ton tai, vi vay tab bi dong tu flow khac khong de lai selection rac.

## Edge Cases

- Khong co tab: grid hien empty state va More bi vo hieu hoa.
- Search query chi co khoang trang: hien tat ca tab cua mode hien tai.
- Voice search khong kha dung: hien toast, khong crash.
- Mot tab hoac it ket qua: SearchBar + banner van nam sat tren IME, khong tao khoang trong bang chieu cao ban phim.
- URL noi bo hoac malformed khong duoc bookmark.
- Bookmark da ton tai khong duoc insert trung.
- Khong co app nhan ACTION_SEND: hien toast thay vi crash.
- Dong mot phan selection: o lai man chon tab.
- Dong toan bo tab cua mode: quay ve Tabs screen.
- Thumbnail khong ton tai: hien icon placeholder.

## Acceptance Criteria

- [x] Normal More menu chi co 2 action dung Figma.
- [x] Select tabs mo route full-screen dung Normal/Private mode.
- [x] Grid 2 cot va checkbox toggle doc lap cho tung tab.
- [x] Select-all doi thanh Deselect All khi tat ca tab da duoc chon.
- [x] Action phu disabled khi selection rong.
- [x] Close, Bookmark va Share dung danh sach selection hien tai.
- [x] Normal close-all co dialog xac nhan.

## Lien Quan

- [F02_INCOGNITO_MODE.md](../features/F02_INCOGNITO_MODE.md)
- [F03_TABS_MANAGER.md](../features/F03_TABS_MANAGER.md)
- [S07_BROWSER_WEBVIEW.md](S07_BROWSER_WEBVIEW.md)
