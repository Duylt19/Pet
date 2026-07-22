# F05b — Video Site Handlers (Per-Platform Download)

Tài liệu nối tiếp [F05_DOWNLOAD_MANAGER.md](F05_DOWNLOAD_MANAGER.md). Ghi lại **cách phát hiện video của từng nền tảng** và **playbook để thêm nền tảng mới**. Dùng khi tiếp tục handle X, Dailymotion, Vimeo…

> Branch đang làm: `feature/download-manager-rework` (chưa merge/push tính tới 2026-06-23).

> Cập nhật 2026-07-13: poster/title được lấy từ DOM và persist; concurrent notifications đã tách ID. Vẫn cần chạy test matrix trên thiết bị thật cho từng website.

---

## 1. Trạng thái từng nền tảng

| Nền tảng | Trạng thái | Cơ chế phát video | Cách handle trong code |
|----------|-----------|-------------------|------------------------|
| **Direct .mp4 / .webm** | ✅ | File progressive | `VideoSniffer.guessMimeType` theo đuôi path |
| **HLS (.m3u8)** | ✅ | Playlist + segment | `HlsDownloader` (master→variant, TS concat, AES-128) |
| **TikTok** | ✅ | MP4 progressive, URL không có đuôi | Nhận `mime_type=video_mp4` trong `guessMimeType` |
| **Facebook** | ✅ | DASH tách audio/video (H.264+AAC) | `handleMetaTrack` → ghép theo `video_id` → `DashMuxDownloader` (MediaMuxer) |
| **Instagram** | ✅ | DASH VP9+AAC → **ép H.264 progressive** | `PREFER_H264_JS` (BrowserEngine) + `handleMetaTrack` (ghép theo `xpv_asset_id`) |
| **Threads** | ✅ | Giống Instagram (Meta CDN) | Như Instagram |
| **Vimeo** | ⚠️ Chặn | Cloudflare Turnstile | Khó pass trong WebView (xem §4) |
| **X (Twitter)** | ⛔ Chưa làm | Login-walled; **đoán: HLS** `video.twimg.com` | Cần đăng nhập / link công khai để lấy URL (xem §3) |
| **Dailymotion** | ⛔ Chưa làm | Login-walled; **đoán: HLS** `*.dmcdn.net` | Cần đăng nhập / link công khai (xem §3) |

### Phạm vi tải phim/stream hiện tại

- Tải được: progressive MP4/WebM/MOV và HLS dùng MPEG-TS; HLS master chọn rendition bandwidth cao nhất, hỗ trợ AES-128.
- `blob:` không phải URL tải. App chỉ tải được nếu `VideoSniffer` bắt được MP4/m3u8 thật ở network phía sau MediaSource.
- Không tải được: Widevine/DRM, SAMPLE-AES, HLS fMP4/CMAF (`EXT-X-MAP`) và generic DASH `.mpd` chưa có handler.
- HLS có audio rendition tách riêng cần mux riêng; không được coi là đã support nếu file kiểm tra không có track audio.

---

## 2. Playbook: thêm một nền tảng mới

Quy trình thực nghiệm đã dùng cho FB/TikTok/IG (KHÔNG đoán mò — phải xem URL thật):

1. **Gắn log chẩn đoán tạm** vào đầu `VideoSniffer.onResourceIntercepted` (snippet ở §5).
2. Build + cài: `./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`.
3. `adb logcat -c`, mở site, phát 1 video ~10s, cuộn 1-2 video.
4. Đọc log: `adb logcat -d -s VidProbe` → xác định **host, có `.mp4`/`.m3u8`/`.mpd` không, có `Sec-Fetch-Dest`, có param đặc thù không**.
5. Phân loại:
   - **Progressive mp4** (1 file, có tiếng) → thêm rule vào `guessMimeType` hoặc nhận theo host/param. Tải thẳng.
   - **HLS (.m3u8)** → thường `HlsDownloader` chạy sẵn; chỉ cần đảm bảo `VideoSniffer` không lọc bỏ.
   - **DASH tách a/v** → cần khóa ghép cặp (id chung của audio+video) + mux `DashMuxDownloader`. Lưu ý codec (xem §4).
6. Implement → build → test tải thật → kiểm tra **phát được + có tiếng** (pull file soi `vide`/`soun` nếu cần: `grep -aoE 'vide|soun' file.mp4`).
7. **Gỡ log chẩn đoán** trước khi commit.

---

## 3. Việc còn lại: X (Twitter) & Dailymotion

**Vướng:** cả hai bắt đăng nhập nên chưa quan sát được URL video.

**Cần để làm tiếp (một trong hai):**
- Đăng nhập trên app rồi phát 1 video (gắn lại log §5), HOẶC
- Mở **link video công khai cụ thể** không cần login:
  - X: `x.com/<user>/status/<id>`
  - Dailymotion: `dailymotion.com/video/<id>`

**Dự đoán & hướng:**
- **Dailymotion**: HLS qua `*.dmcdn.net` (master.m3u8). Nhiều khả năng `HlsDownloader` xử lý được ngay — chỉ cần verify.
- **X**: HLS qua `video.twimg.com/.../pl/....m3u8` (có thể tách audio/video ở định dạng mới) + đôi khi mp4 trực tiếp `video.twimg.com/.../vid/....mp4`. Verify HLS trước; nếu tách a/v thì cần mux.

---

## 4. Ghi chú kỹ thuật quan trọng

### Meta (Facebook / Instagram / Threads)
- URL: `https://<host: *.fbcdn.net | scontent*.cdninstagram.com>/o1/v/.../AQ....mp4?...&efg=<base64>&...&bytestart=N&byteend=M`
- `efg` = base64(JSON) chứa: `video_id` (FB có, IG = null), `xpv_asset_id` (khóa ghép cặp dự phòng), `vencode_tag`, `bitrate`, `duration_s`.
- `vencode_tag`:
  - `...progressive...` → **file gộp sẵn** (audio+video) → tải thẳng.
  - `dash_h264-...` / `dash_..._audio` → **DASH tách** → mux.
  - `dash_r2evevp9...` → **VP9** (Instagram reels) → KHÔNG mux được vào MP4.
- **Strip `&bytestart=`/`&byteend=`** → GET trả full file (HTTP 200). FB tải được từ PC; IG khóa IP (403 ngoài app, nhưng OK trong app vì có cookie/session).

### Giới hạn MediaMuxer (codec)
- MP4 output: chỉ nhận **H.264/H.265/MPEG4 video + AAC audio**. KHÔNG nhận VP9.
- WebM output: nhận VP9 + Vorbis/Opus. KHÔNG nhận AAC.
- ⇒ **VP9 video + AAC audio không mux được** vào 1 container chuẩn. Giải pháp đã dùng cho IG: **ép player phục vụ H.264** bằng `PREFER_H264_JS` (override `MediaSource.isTypeSupported`/`canPlayType` báo không hỗ trợ vp9/webm) — chỉ inject trên domain Meta để không phá site khác.
- Nếu gặp site VP9-only (không có bản H.264): lựa chọn là transcode audio AAC→Opus rồi mux WebM (nặng), hoặc đánh dấu không hỗ trợ.

### Cloudflare (Vimeo)
- "Verify you are human" (Turnstile) cần cookies + DOM storage (đã bật ở mọi chế độ — commit `f9a4ae5`).
- Vẫn khó pass vì Cloudflare fingerprint WebView. Không giải quyết dứt điểm từ phía app. Thử: chờ/reload, mở video lẻ; pass 1 lần thì cookie `cf_clearance` được lưu.

---

## 5. Snippet log chẩn đoán (gắn tạm, nhớ gỡ)

Đặt ngay sau `val headers = request.requestHeaders` trong `VideoSniffer.onResourceIntercepted`:

```kotlin
// TEMP diagnostic — remove after analysis.
run {
    val dest = headers["Sec-Fetch-Dest"]?.lowercase()
    val signals = listOf(
        ".mp4", ".m3u8", ".mpd", "master.json", "mime_type=video",
        "cdninstagram", "fbcdn", "twimg", "dmcdn", "vimeocdn", "akamaized",
        "/video/", "/playlist", "/manifest"
    )
    if (dest == "video" || dest == "audio" || signals.any { url.contains(it, ignoreCase = true) }) {
        val host = try { java.net.URI(url).host } catch (_: Exception) { "?" }
        android.util.Log.d("VidProbe",
            "page=${pageUrl?.take(40)} dest=$dest range=${headers["Range"] != null} host=$host url=${url.take(700)}")
    }
}
```

Phân tích log (giải mã efg của Meta):
```bash
adb logcat -d -s VidProbe | grep -oE 'url=https://[^ ]+' | sed 's/url=//' | sort -u > urls.txt
# Meta efg decode (node):
node -e 'require("fs").readFileSync("urls.txt","utf8").split("\n").filter(Boolean).forEach(l=>{const m=l.match(/[?&]efg=([^&]+)/);if(m)try{console.log(JSON.parse(Buffer.from(decodeURIComponent(m[1]),"base64").toString()))}catch(e){}})'
```

---

## 6. Lưu ý chung (nợ kỹ thuật)

- DB đang ở development version 1 và dùng destructive fallback. Chỉ bắt đầu duy trì migration sau khi version 2 được chốt làm baseline ổn định.
- `PREFER_H264_JS` chỉ inject trên domain Meta — nếu mở rộng sang site khác cần cân nhắc ảnh hưởng playback.
