# F05 — Download Manager & Video Detection

Tự động phát hiện video khi duyệt web, cho phép chọn và tải video với OkHttp streaming, hỗ trợ resume, concurrent downloads, và CDN auth (cookies/headers).

---

## 0. Cập Nhật 2026-06-23 (Rework)

Đợt rework lớn để tải video "ngang CocCoc/IDM". Tóm tắt thay đổi so với bản gốc bên dưới:

| Hạng mục | Trạng thái mới |
|----------|----------------|
| **Pause/Resume** | Sửa lỗi hỏng file: dùng `outputFile.length()` làm offset resume, chỉ `append` khi server trả HTTP **206**; nếu 200 → ghi đè từ đầu |
| **Sticky restart** | Service bị kill rồi restart sẽ **resume** các download dở (`getResumable()`), không còn "zombie RUNNING" |
| **Trùng tên file** | `enqueue()` tự sinh tên duy nhất `tên (1).ext`, không ghi đè |
| **Concurrency** | Giới hạn `MAX_CONCURRENT = 3` + hàng đợi PENDING (`getNextPending`) |
| **Notification** | Hiển thị **tốc độ + ETA** (vd `45% — 12/27 · 1.8 MB/s · 8s left`) |
| **HLS (m3u8)** | **Tải được** (trước đây bị filter): `HlsDownloader` — master→variant, TS concat, giải mã AES-128; fMP4/DRM fail gracefully |
| **Facebook (DASH)** | **Tải được kèm tiếng**: phát hiện track fbcdn qua param `efg`, ghép cặp theo `video_id`, mux audio+video bằng `MediaMuxer` (xem §8 & §15) |
| **Deep link** | `fb://`, `intent://`… mở app ngoài thay vì hiện trang `ERR_UNKNOWN_URL_SCHEME` |
| **Database** | DEV: giữ version 1 và recreate DB khi schema đổi; bắt đầu quản lý migration sau khi chốt baseline version 2 |

### Cập nhật 2026-07-13

- Observer trong WebView lấy `video.poster`, `og:image`, title và source động trên SPA.
- Thumbnail hiện placeholder ngay, tải poster bất đồng bộ và persist vào download row.
- Chỉ sniff tab active, tránh stream của tab nền làm sai danh sách chọn.
- Một channel `downloads`, một foreground summary và notification con có ID riêng cho từng download.
- Notification con có Pause/Cancel; item Failed không biến mất và có thể Retry/resume partial file.
- Giữ User-Agent, Cookie, Referer và Origin của WebView cho request tải.

> Files mới: `service/HlsDownloader.kt`, `service/DashMuxDownloader.kt`.

> 📖 **Per-platform** (TikTok, Facebook, Instagram, Threads… + cách thêm site mới và việc còn lại cho X/Dailymotion): xem [F05b_VIDEO_SITE_HANDLERS.md](F05b_VIDEO_SITE_HANDLERS.md).

---

## 1. Architecture Overview

```
┌─ BrowserScreen ──────────────────────────────────────────────────┐
│  WebView ← VideoSniffer intercepts shouldInterceptRequest        │
│                                                                  │
│  ┌─ VideoDownloadFab ─────────────────────────────┐              │
│  │  Gradient #5B6FFB→#7C5BFB, pulse animation     │              │
│  │  Badge count (khi detect > 1 video)            │              │
│  └────────────────────────────────────────────────┘              │
│                                                                  │
│  ┌─ VideoSelectBottomSheet ───────────────────────┐              │
│  │  "Select items" + "Select All"                 │              │
│  │  Grid 3 cột: poster async → video frame fallback│             │
│  │  Download button (count)                       │              │
│  └────────────────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────────────┘
         │ downloadVideos()
         ▼
┌─ DownloadRepository ─────────────────────────────────────────────┐
│  enqueue(fileName, url, path, mimeType, headers)                 │
│  → DownloadEntity + requestHeaders (JSON serialized Map)         │
└──────────────────────────────────────────────────────────────────┘
         │ start service
         ▼
┌─ DownloadForegroundService ──────────────────────────────────────┐
│  1. Read entity from Room DB                                     │
│  2. Parse stored requestHeaders (JSON → Map)                     │
│  3. Get cookies: CookieManager.getInstance().getCookie(url)      │
│  4. Build OkHttp Request: headers + cookies + User-Agent         │
│  5. Stream bytes → write to /Downloads/PrivateBrowser/           │
│  6. Update DB progress every 500ms                               │
│  7. Foreground summary + child notification riêng từng download  │
│  8. On complete: child đổi sang "Completed" notification         │
│  9. When all done: stopForeground + stopSelf                     │
└──────────────────────────────────────────────────────────────────┘
         │ Room Flow (reactive)
         ▼
┌─ ProgressTabScreen (Downloads tab) ──────────────────────────────┐
│  Tab filter: All / Downloading / Completed                       │
│  Active items: Coil thumbnail + progress bar + pause/resume/cancel│
│  Completed items: Coil thumbnail + click → FileProvider → player │
│  Empty state: "No Downloads Yet" + "Start Browsing" button       │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Video Sniffer

File: `data/browser/VideoSniffer.kt`

### 2.1. Detection Logic

```kotlin
@Singleton
class VideoSniffer @Inject constructor() {
    private val _detectedVideos = MutableStateFlow<List<DetectedVideo>>(emptyList())
    val detectedVideos: StateFlow<List<DetectedVideo>> = _detectedVideos.asStateFlow()

    fun onResourceIntercepted(request: WebResourceRequest, pageUrl: String?) {
        val url = request.url.toString()
        val mime = guessMimeType(url, request.requestHeaders)
        if (isVideoUrl(url, mime)) {
            // Deduplicate by URL
            val video = DetectedVideo(...)
            _detectedVideos.update { current ->
                if (current.none { it.url == url }) current + video else current
            }
        }
    }

    fun clearForPage() { _detectedVideos.value = emptyList() }
}
```

### 2.2. Filter Rules

| Type | Action | Extensions/Patterns |
|------|--------|---------------------|
| ✅ Detect | Video files | `.mp4`, `.webm`, `.mov`, `.avi`, `.mkv`, `.flv` |
| ✅ Detect | Video MIME | `video/*` Content-Type |
| ✅ Detect | Facebook CDN | URL chứa cả `"video"` + `"fbcdn"` |
| ❌ Filter | Image files | `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.svg`, `.ico`, `.bmp` |
| ❌ Filter | Image MIME | `image/*` Content-Type |
| ✅ Detect | HLS playlist | `.m3u8` — tải được qua `HlsDownloader` (ghép segment) |
| ✅ Detect | Facebook DASH | fbcdn URL có `efg=` → ghép audio+video theo `video_id` |
| ✅ Detect | Media request | `Sec-Fetch-Dest: video` / `Accept: video/*` (URL không có đuôi) |
| ❌ Filter | HLS segments | `.ts` chứa `seg-`/`segment`/`chunk`/numeric pattern |
| ❌ Filter | DASH ranged | URL có `bytestart=`/`byteend=` (mảnh một phần) |

### 2.3. DetectedVideo Model

```kotlin
data class DetectedVideo(
    val url: String,
    val mimeType: String?,
    val displayName: String,      // Extracted from URL or generated
    val fileExtension: String,     // "mp4", "webm", etc.
    val isHls: Boolean,            // true for m3u8 (filtered, won't appear)
    val headers: Map<String, String>,  // Original request headers from WebView
    val thumbnailUrl: String?,         // video poster / og:image
)
```

---

## 3. Download Repository

```kotlin
interface DownloadRepository {
    fun observeAll(): Flow<List<DownloadEntity>>
    fun observeActive(): Flow<List<DownloadEntity>>
    fun observeCompleted(limit: Int = 50): Flow<List<DownloadEntity>>
    suspend fun enqueue(
        fileName: String, url: String, path: String,
        mimeType: String, headers: Map<String, String> = emptyMap()
    ): Long
    suspend fun updateProgress(id: Long, status: String, bytes: Long)
    suspend fun cancel(id: Long)
    suspend fun delete(id: Long)
}
```

**Headers serialization:**
- `enqueue()` → `JSONObject(headers).toString()` → lưu vào `requestHeaders` column
- Service đọc lại: `JSONObject(entity.requestHeaders)` → parse keys → add to OkHttp Request

---

## 4. Database

### 4.1. DownloadEntity

```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0,
    @ColumnInfo(name = "downloaded_bytes") val downloadedBytes: Long = 0,
    @ColumnInfo(name = "status") val status: String = "PENDING",
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "request_headers", defaultValue = "")
    val requestHeaders: String = "",  // JSON: {"Cookie":"...", "Referer":"..."}
    @ColumnInfo(name = "thumbnail_url", defaultValue = "")
    val thumbnailUrl: String = "",
)
```

### 4.2. Status Values

| Status | Ý nghĩa |
|--------|---------|
| `PENDING` | Đã enqueue, chưa bắt đầu |
| `RUNNING` | Đang tải |
| `PAUSED` | User tạm dừng |
| `COMPLETED` | Tải xong |
| `FAILED` | Lỗi (network, disk, 403, etc.) |

### 4.3. Migration

- Trong giai đoạn development, DB giữ ở version **1** và dùng `fallbackToDestructiveMigration`.
- Không viết migration cho các thay đổi schema hiện tại.
- Sau khi version **2** được chốt làm baseline ổn định, mọi lần tăng version tiếp theo phải có migration tương ứng.

---

## 5. Download Engine (DownloadForegroundService)

File: `service/DownloadForegroundService.kt`

### 5.1. OkHttp Streaming Download

```kotlin
// 1. Read stored headers
val json = JSONObject(entity.requestHeaders)
json.keys().forEach { key -> requestBuilder.addHeader(key, json.getString(key)) }

// 2. Add cookies from WebView session
val cookies = CookieManager.getInstance().getCookie(entity.url)
if (!cookies.isNullOrBlank()) requestBuilder.header("Cookie", cookies)

// 3. Add User-Agent
requestBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 13)...")

// 4. Support resume
if (outputFile.exists() && entity.downloadedBytes > 0) {
    requestBuilder.addHeader("Range", "bytes=${downloadedBytes}-")
}

// 5. Execute + stream
val response = okHttpClient.newCall(request).execute()
val sink = if (downloadedBytes > 0) outputFile.appendingSink() else outputFile.sink()
// ... buffer.read() loop with progress updates
```

### 5.2. Concurrent Downloads

```kotlin
private val activeJobs = ConcurrentHashMap<Long, Job>()
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
```

### 5.3. Actions

| Intent Action | Behavior |
|---------------|----------|
| `ACTION_START` (default) | Start new download |
| `ACTION_PAUSE` | Cancel job, status → PAUSED |
| `ACTION_RESUME` | Re-start from downloadedBytes |
| `ACTION_CANCEL` | Cancel job, delete partial file và download row |

---

## 6. Notification

### 6.1. During Download

- Foreground group summary dùng ID cố định `1001`.
- Mỗi download dùng ID ổn định riêng, có title/progress/speed/ETA và Pause/Cancel.
- Tất cả dùng chung channel `downloads`; channel cấu hình hành vi, không định danh từng job.
- Click notification mở Downloads tab (`navigate_to_downloads` extra).

### 6.2. On Completion

- Notification riêng của download được thay bằng trạng thái non-ongoing Completed.
- `setAutoCancel(true)` — dismiss on click
- Clickable → opens Downloads tab

### 6.3. When All Done

- `stopForeground(STOP_FOREGROUND_REMOVE)` — dismiss progress notification
- `stopSelf()` — stop service

### 6.4. Android 15+ Timeout

- `dataSync` foreground service có giới hạn thời gian khi app ở background.
- `onTimeout()` hủy các job đang chạy, lưu trạng thái `PAUSED`, dọn notification và dừng service.
- User có thể resume từ Downloads thay vì để row bị kẹt ở `RUNNING`.

---

## 7. UI Components

### 7.1. VideoDownloadFab (BrowserScreen.kt)

- Gradient background: `#5B6FFB` → `#7C5BFB`
- Pulse animation: infinite scale `1.0` → `1.15`
- Badge count khi > 1 video
- Positioned `Alignment.BottomEnd` with padding
- Chỉ hiện khi `detectedVideos.isNotEmpty()`

### 7.2. VideoSelectBottomSheet

- ModalBottomSheet dark theme (`#15171E`)
- Header: "Select items" + "Select All" toggle
- Body: `LazyVerticalGrid` 3 cột
- Each item: `VideoThumbnail` (Coil) + extension badge + selection circle
- Footer: Download button (`#5B6FFB`) with count

### 7.3. Video Thumbnails

Sử dụng **Coil `coil-video`** (`VideoFrameDecoder`) — lấy frame tại t=0:

```kotlin
coil.request.ImageRequest.Builder(context)
    .data(videoUrlOrFilePath)
    .decoderFactory(coil.decode.VideoFrameDecoder.Factory())
    .size(128)  // Small thumbnail size
    .memoryCacheKey("thumb_${url.hashCode()}")
    .crossfade(true)
    .build()
```

| Location | Data Source | Fallback |
|----------|------------|----------|
| VideoSelectBottomSheet | Video URL + cookies | `ic_video_file` icon |
| ProgressTab - Active | Video URL | `ic_video_file` icon |
| ProgressTab - Completed | **Local file path** (faster) | `ic_video_file` icon |

### 7.4. ProgressTabScreen (Downloads tab)

- Tab row: All / Downloading / Completed
- Empty state: "No Downloads Yet" icon + "Start Browsing" button
- Active items: `DownloadVideoThumbnail` + progress bar + file size + pause/resume/cancel
- Completed items: `DownloadVideoThumbnail` + timestamp + click → `FileProvider` → `ACTION_VIEW`

---

## 8. Facebook Video (DASH audio+video mux)

Facebook (m.facebook.com/watch) phát video bằng **MSE/DASH với audio và video tách riêng**. URL gốc `playable_url` KHÔNG nằm trong DOM cũng không trong response fetch/XHR đọc được → **không thể scrape bằng JS**. Thay vào đó:

```
1. VideoSniffer.onResourceIntercepted()
   → URL track fbcdn có param efg (base64 JSON)
   → giải mã efg: { video_id, vencode_tag, bitrate }
   → vencode_tag chứa "audio" → track tiếng; còn lại → track hình
   → strip &bytestart=/&byteend= để lấy URL FILE ĐẦY ĐỦ (HTTP 200)
   → nhóm theo video_id: giữ video bitrate cao nhất + audio
   → emit 1 DetectedVideo(url=video, audioUrl=audio, customName="facebook_<id>")

2. BrowserViewModel.downloadVideos()
   → enqueue với audioUrl (DownloadEntity.audioUrl)

3. DownloadForegroundService.startDownload()
   → entity.audioUrl không rỗng → handleMuxDownload()

4. DashMuxDownloader.download()
   → tải track video → temp, track audio → temp
   → MediaMuxer: copy sample H.264 + AAC vào 1 mp4 (KHÔNG re-encode, không cần ffmpeg)
   → xóa temp
```

**Giới hạn**: chỉ xử lý DASH H.264 + AAC không DRM (Widevine/SAMPLE-AES không thể). Feed watch nhiều video → nhiều entry; nên đã verify phát được có tiếng trên thiết bị thật.

### 8.1. CDN Auth (cho video/HLS thường)

Với video không phải FB, headers/cookies từ WebView session được lưu (`requestHeaders` column, JSON) rồi truyền vào OkHttp request + `CookieManager.getCookie(url)` để qua CDN auth (403 → 200).

---

## 9. Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="28" />
```

Trên API 33+, `POST_NOTIFICATIONS` được xin theo ngữ cảnh khi user bắt đầu hoặc
resume download đầu tiên. App lưu request count trước khi mở system dialog và
không hỏi lặp lại nếu user từ chối. Việc từ chối chỉ làm notification không xuất
hiện trong notification drawer; download và foreground service vẫn tiếp tục.
`MANAGE_EXTERNAL_STORAGE` không phải điều kiện của Download Manager: API 29+
luôn publish file bằng `MediaStore.Downloads`.

---

## 10. File Storage

| API | Path | Method |
|-----|------|--------|
| ≤28 | `/sdcard/Download/PrivateBrowser/<fileName>` khi có `WRITE_EXTERNAL_STORAGE`; app-owned fallback nếu user từ chối | Direct file write + MediaScanner |
| 29+ | App-owned pending file → `Download/PrivateBrowser/<fileName>` | Publish bằng `MediaStore.Downloads` + `IS_PENDING`, không cần storage write permission |

File legacy mở bằng `FileProvider`; file API 29+ dùng content URI do MediaStore cấp:
```xml
<provider
    android:authorities="${applicationId}.fileprovider"
    android:grantUriPermissions="true">
    <meta-data android:resource="@xml/file_paths" />
</provider>
```

---

## 11. Edge Cases

| Trường hợp | Xử lý |
|-------------|--------|
| Disk full | `IOException` caught → status `FAILED` + errorMessage |
| Network drop | OkHttp throws → status `FAILED`, có thể resume sau |
| CDN 403 Forbidden | Headers/cookies từ WebView session giải quyết |
| URL redirect | OkHttp `followRedirects = true` (default) |
| Same file name | Tự đặt tên duy nhất `tên (1).ext` (kiểm tra cả đĩa lẫn DB) |
| Pause rồi Resume | Resume theo `outputFile.length()` + Range; chỉ append khi 206 → không hỏng file |
| M3U8/HLS detected | Tải được — ghép segment (`HlsDownloader`); fMP4/DRM fail gracefully |
| Facebook DASH | Tách audio/video → tải 2 track rồi mux MediaMuxer |
| Image URL (jpg/png) | Filter bỏ bởi `IMAGE_PATTERN` |
| App killed | `START_STICKY` → service restart |
| User cancel | Delete partial file + status `FAILED` |
| Facebook CDN | Request headers + cookies + UA → 200 OK |

---

## 12. Dependencies

```toml
# Đã có trong gradle/libs.versions.toml
coil-compose = { module = "io.coil-kt:coil-compose", version = "2.7.0" }
coil-video = { module = "io.coil-kt:coil-video", version = "2.7.0" }
# OkHttp (transitive from Retrofit/Coil)
```

---

## 13. Files Tham Chiếu

**Data Layer:**
- `data/browser/VideoSniffer.kt` — Video detection + filtering
- `data/browser/DetectedVideo.kt` — Video model
- `data/database/entity/DownloadEntity.kt` — Room entity
- `data/database/dao/DownloadDao.kt` — Room DAO
- `data/database/PrivateBrowserDatabase.kt` — DB development version 1
- `data/repository/DownloadRepository.kt` — Interface
- `data/repository/impl/DownloadRepositoryImpl.kt` — Implementation

**Service:**
- `service/DownloadForegroundService.kt` — OkHttp download engine

**UI:**
- `ui/browser/VideoSelectBottomSheet.kt` — Video selection grid
- `ui/browser/BrowserScreen.kt` — VideoDownloadFab
- `ui/browser/BrowserViewModel.kt` — downloadVideos(), showVideoSheet
- `ui/browser/BrowserUiState.kt` — detectedVideos, showVideoSheet
- `ui/home/progresstab/ProgressTabScreen.kt` — Downloads tab UI
- `ui/home/progresstab/ProgressTabViewModel.kt` — Downloads business logic
- `ui/home/progresstab/ProgressTabUiState.kt` — Downloads UI state

**DI:**
- `di/BrowserModule.kt` — TabManager + DownloadRepository injection

**Resources:**
- `res/drawable/ic_video_file.xml` — Video file icon (fallback thumbnail)
- `res/drawable/ic_check_white.xml` — Selection checkmark
- `res/drawable/ic_download_arrow.xml` — Download arrow icon

---

## 14. Liên Quan

- [F01_BROWSER_CORE.md](F01_BROWSER_CORE.md) — VideoSniffer hook in WebViewClient
- [F06_FILE_MANAGER.md](F06_FILE_MANAGER.md) — Downloaded files in Files tab
- [S06d_PROGRESS_TAB.md](../screens/S06d_PROGRESS_TAB.md) — Progress tab screen spec
