# S06c - Folder Tab

## Visual reference

- Folder: Figma node `11145:8024`.
- Photos list: Figma nodes `11311:9534`, `11311:9742`.

Folder home giu storage card, Vault banner va File Manager grid 2x2: Photos,
Videos, Audio, Files. Moi card navigate toi `media_list/{type}`.
Vault banner van duoc giu trong UI; tap vao banner hoac nut Open Vault hien
`Feature coming soon` cho den khi flow Vault duoc trien khai.
Neu storage permission bi tu choi, storage card duoc thay bang the "Allow access"
thay vi hien 0 GB; category cards van navigate binh thuong va man `media_list/{type}`
tu xu ly permission rieng khi mo.

## Inner screen shell

Header gom Back, title + item count, Search va More. Search thay title bang text
field. More chua sort newest/oldest/name/size va grid/list toggle neu category ho
tro. Banner ad sticky o day.

## Category designs

| Screen | Layout | Visible metadata |
|---|---|---|
| Photos | 4-column square grid | Thumbnail; item actions overlay |
| Videos | 2-column 16:9 grid | Duration, name, size, modified date |
| Audio | Dense list | Name, artist, duration, size, modified date |
| Files | Document list | Name, MIME/icon, size, modified date |

Photos va Videos co list alternative. Moi item co Open, Share, Details va Delete.
Voi `MANAGE_EXTERNAL_STORAGE`/legacy storage da cap, Delete chi hien app confirmation
roi xoa thang; system scoped-storage confirmation chi xuat hien khi thieu broad access.

## States and flow

- Loading, permission denied, partial access, error, empty storage va empty search
  la cac state rieng.
- Android 14 partial photo/video access van render media duoc cap.
- Quay lai tu viewer, permission/settings hoac delete se re-query MediaStore.
- Files co `Browse device files` de mo system document picker.
- Empty Files cung hien CTA Browse thay vi dead end.

## Acceptance criteria

- [x] Bon category cards co destination hoat dong.
- [x] Files khong con TODO/emptyList co dinh.
- [x] Photos theo grid 4 cot cua Figma.
- [x] Search, sort va grid/list state hoat dong.
- [x] Open, share, details va scoped-storage delete duoc handle.
- [x] Runtime permission theo Android version va category.
- [x] Loading/error/permission/partial/empty states khong bi nham lan.
