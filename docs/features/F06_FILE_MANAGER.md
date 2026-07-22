# F06 - File Manager

Folder hien thi storage usage va quan ly bon nhom Photos, Videos, Audio, Files.

Storage header lay total/available tren cac external storage volume cung pham vi
voi MediaStore. Photos, Videos, Audio va Files la tong metadata `SIZE` cua tung
category; downloaded media khong duoc cong lap vao Files. Thanh storage co them
segment Other = used - tong bon category, con phan nen toi la free space. So lieu
duoc refresh khi Folder hien lai va khi lifecycle resume.

## Architecture

- `FilesTabScreen`: tong quan storage, Vault banner va 4 category cards.
- `MediaListScreen`: shell UI dung chung cho route `media_list/{type}`.
- `MediaListViewModel`: load, refresh, search, sort, view mode va access state.
- `MediaStoreRepository`: query content URI, metadata va delete app download.

Repository khong dung `MediaColumns.DATA`. Moi item giu content URI, MIME type,
size, modified date, duration/artist/resolution neu co va nguon `MEDIA_STORE` hoac
`APP_DOWNLOAD`.

## Category behavior

| Category | Data source | Default UI |
|---|---|---|
| Photos | `MediaStore.Images` | Grid 4 cot theo Figma `11311:9534` |
| Videos | `MediaStore.Video` | Grid 2 cot 16:9 |
| Audio | `MediaStore.Audio` | List voi artist va duration |
| Files | API 30+ có All files access: `MediaStore.Files`; Android cũ dùng MediaStore | Document list |

Files co them `Browse device files` qua Storage Access Framework. Flow nay cho
phep user mo tai lieu ngoai pham vi MediaStore ma khong can all-files permission.

## Shared features

- Search realtime theo name, artist hoac MIME type.
- Sort newest, oldest, name, size.
- Photos/Videos chuyen duoc grid va list.
- Open bang app phu hop, Share content URI, Details metadata, Delete.
- **Delete khi co `BroadStorageAccess`:** All files access (API 30+ voi
  `Environment.isExternalStorageManager()`) hoac legacy READ+WRITE storage (API <= 29)
  mien tru scoped-storage nen `ContentResolver.delete()` xoa thang MOI category —
  ke ca anh/video/audio do app khac so huu — chi qua app confirm dialog, khong co
  system delete-request dialog. Ap dung cho ca single va batch (`deleteFilesDirectly`
  trong ViewModel). Day la ly do chinh de xin `MANAGE_EXTERNAL_STORAGE`.
- **Fallback khi thieu broad access** (hiem, vi cac man da gate tren granted access):
  - API 30+: `MediaStore.createDeleteRequest()` gom URI, hien system consent 1 lan
    (single) hoac 1 lan cho ca lo (batch).
  - API 29: `ContentResolver.delete()` bat `RecoverableSecurityException` va launch
    system consent tung item.
- App-owned downloads (`APP_DOWNLOAD`) luon xoa truc tiep bang `File.delete()`.
- Refresh khi resume va sau delete.
- Download service media-scan file sau khi hoan tat de category cap nhat ngay.

## Permissions

| API | Permission |
|---|---|
| 24-29 | `READ_EXTERNAL_STORAGE` + `WRITE_EXTERNAL_STORAGE` (ca hai, khong tach rieng 29) |
| 30+ | `MANAGE_EXTERNAL_STORAGE` qua special-access Settings cho mọi category |

API 29 con yeu cau `Environment.isExternalStorageLegacy() == true` (nho
`requestLegacyExternalStorage="true"` trong manifest) truoc khi coi la da duoc cap
broad storage access — xem `LegacyStorageAccess`/`BroadStorageAccess` trong
`utils/permission/PermissionAccess.kt`. API 30+ khong khai bao hoac request
`READ_MEDIA_*`. Photos, Videos, Audio, Files va roadmap Vault dung chung
`MANAGE_EXTERNAL_STORAGE` qua special-access Settings. Khi duoc cap, cac category
media doc MediaStore va Files doc toan bo non-media file da duoc index trong
`MediaStore.Files`; khi bi tu choi, Browser, Download va SAF van hoat dong, con cac
man Media Library/Files hien trang thai yeu cau quyen.

`FilesTabScreen` (man tong quan storage) tu kiem tra `BroadStorageAccess` khi hien
thi va khi resume; neu bi tu choi, storage card duoc thay bang the "Allow access"
thay vi am tham hien 0 GB. Luong request quyen (system dialog → App Settings sau 2
lan tu choi) dung chung `PermissionPolicy`/`DataStoreManager` voi `MediaListScreen`.

## States

- Checking/loading: progress indicator.
- Granted: render data.
- Denied: Allow access + Open settings.
- Query empty: `No matching files`.
- Storage empty: mo ta rieng theo category; Files co Browse device CTA.
- Query failure: retry state, khong gia lam empty state.

## Related

- [F05_DOWNLOAD_MANAGER.md](F05_DOWNLOAD_MANAGER.md)
- [S06c_FILES_TAB.md](../screens/S06c_FILES_TAB.md)
