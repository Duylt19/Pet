# F05c — Download Test Matrix (for on-device verification)

Dùng để verify đợt nâng cấp download (bao phủ tối đa website). Chạy trên **thiết bị thật**
(nhiều CDN chặn emulator/IP lạ). Nối tiếp [F05_DOWNLOAD_MANAGER.md](F05_DOWNLOAD_MANAGER.md),
[F05b_VIDEO_SITE_HANDLERS.md](F05b_VIDEO_SITE_HANDLERS.md).

## Có gì mới trong đợt này

| Hạng mục | Trạng thái |
|----------|-----------|
| OkHttp timeout + retry (5xx/mạng chập chờn) | `di/NetworkModule.kt`, `di/RetryInterceptor.kt` |
| Mux dùng chung + tự chọn **MP4/WebM** theo codec | `service/MediaRemuxer.kt` |
| VP9/AV1 + Opus **không còn fail** → xuất `.webm` | qua `MediaRemuxer` |
| HLS **fMP4/CMAF** (`#EXT-X-MAP`) | `service/HlsDownloader.kt` |
| HLS **audio rendition riêng** (`#EXT-X-MEDIA:TYPE=AUDIO`) | có tiếng |
| HLS **`#EXT-X-BYTERANGE`** + chọn variant theo resolution | |
| **MPEG-DASH `.mpd`** generic | `service/MpdDownloader.kt` |
| Detect thêm player hls.js/dash.js/Shaka | `data/browser/BrowserEngine.kt` |
| YouTube: **cố tình chặn** (Play Store policy) | `VideoSniffer` blocklist |

## Chuẩn bị

```bash
# Build + cài bản debug (đã bật diagnostic log)
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

# Xem log routing/fail của download + detection
adb logcat -c
adb logcat -s PBDownload PBSniffer
```

Sau khi tải xong, pull file và soi bằng ffprobe (kiểm codec/duration/track):

```bash
adb pull "/sdcard/Download/PrivateBrowser/<tên file>" .
ffprobe -hide_banner "<tên file>"          # xem có cả stream video + audio, duration đúng
# hoặc nhanh: có track hình + tiếng chưa
ffprobe -v error -show_entries stream=codec_type -of csv "<tên file>"   # kỳ vọng: video + audio
```

## Test matrix

Với mỗi mục: mở trang → phát video ~10s (để player tải manifest/segment) → bấm **FAB tím** →
chọn video → Download → chờ COMPLETED → mở/pull + ffprobe.

| # | Nhóm | Site/URL mẫu | Kỳ vọng |
|---|------|--------------|---------|
| 1 | Progressive MP4 | `w3schools.com/html/html5_video.asp`, link `.mp4` trực tiếp | File `.mp4`, có tiếng |
| 2 | HLS MPEG-TS | site phim TS (vd yanhh3d…) | `.mp4`, có tiếng, đủ thời lượng |
| 3 | HLS **fMP4/CMAF** | Apple HLS example, `bitmovin`/`bitdash` HLS demo | `.mp4`, có tiếng (trước đây fail) |
| 4 | HLS **audio riêng** | stream có `#EXT-X-MEDIA:TYPE=AUDIO` | có tiếng (trước đây mất tiếng) |
| 5 | **MPEG-DASH** | dash.js/`bitmovin`/Shaka demo, một số trang tin | `.mp4` hoặc `.webm`, có tiếng |
| 6 | **VP9/Opus** | Reddit `v.redd.it`, HTML5 `.webm` | File **`.webm`**, có tiếng (trước đây fail) |
| 7 | AV1 (`av01`) | site AV1-only | Best-effort: mux được thì OK, không thì Failed "định dạng không hỗ trợ" (chấp nhận) |
| 8 | TikTok | tiktok.com 1 video | (regression) `.mp4`, có tiếng |
| 9 | Facebook | m.facebook.com/watch | (regression) `.mp4`, có tiếng |
| 10 | Instagram / Threads | reel/post | (regression) `.mp4`, có tiếng |
| 11 | Direct blob/data | trang test blob:/data: | tải được (như cũ) |
| 12 | **YouTube** | youtube.com phát video | **FAB KHÔNG hiện**, không tạo download |
| 13 | Mạng chập chờn | tải file lớn, bật/tắt wifi giây lát | retry, không fail ngay (timeout/retry) |

## Ghi nhận kết quả

Với mỗi dòng, ghi: **PASS/FAIL**, container ra (`.mp4`/`.webm`/`.ts`), có tiếng?, thời lượng đúng?,
và nếu FAIL thì **dán dòng log `PBDownload FAILED …`** (chứa lý do thật) + host từ `PBSniffer`.

### Lưu ý diễn giải

- **MSE capture (Tier 3)** chỉ bắt phần đã phát/buffer → video dài có thể ngắn/thiếu. Nếu site
  có URL trực tiếp (HLS/DASH/MP4) thì app ưu tiên URL đó (đủ thời lượng); chỉ khi không có mới
  fallback capture. Muốn capture đủ → để video phát hết.
- File `.ts` (fallback khi remux fail) vẫn phát được ở VLC/MX Player.
- `.webm` là **kết quả đúng** cho nguồn VP9/Opus, không phải lỗi.
- Widevine/DRM và Cloudflare (Vimeo) **ngoài phạm vi** — Failed là đúng.
