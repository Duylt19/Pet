# 01 — Project Overview

## 1. Sản Phẩm Là Gì

**Private Browser: Safe & Secure** là một ứng dụng trình duyệt web Android tập trung vào quyền riêng tư và tải video tiện lợi. Khác với các browser truyền thống (Chrome, Firefox), app này nhắm vào nhóm user trẻ thường xuyên:

- Lướt mạng xã hội (FB, Instagram, TikTok, Twitter/X, Threads, Dailymotion)
- Tải video về máy để xem offline / repost
- Muốn ẩn dấu vết duyệt web (incognito mạnh, clear lịch sử nhanh)
- Không muốn cài app riêng cho từng mạng xã hội

App có giao diện gọn (light theme, accent màu tím gradient), bottom navigation 4 tab, và onboarding ngắn để giải thích lợi ích.

---

## 2. Đối Tượng Người Dùng (Target Audience)

| Persona | Đặc điểm | Pain point | Giải pháp |
|---------|----------|------------|-----------|
| **Teen/GenZ** | 14-22 tuổi, dùng smartphone Android tầm trung | Muốn tải video TikTok/IG nhanh, không muốn cài thêm app | Quick access + Download manager |
| **Privacy-conscious user** | 22-35 tuổi, lo về tracking | Không muốn lưu lịch sử/cookies | Incognito mode + Clear History 1 chạm |
| **Casual browser user** | Bất kỳ tuổi | Chỉ duyệt vài site quen thuộc | Quick access shortcuts 8 site phổ biến |

**Khu vực ưu tiên:** Vietnam, Indonesia, Brazil, Mexico, India, Nigeria (theo phân bổ ads networks).

---

## 3. USP (Unique Selling Points)

1. **Tải video trực tiếp từ trang web** — Không phải copy URL paste sang app khác. Browser tự sniff video và hiện badge tải.
2. **Incognito 1-chạm** — Toggle ngay từ tab Tabs, không cần vào setting.
3. **Multi search engine** — 6 search engines (Google/Bing/Yahoo/DuckDuckGo/Yandex/Coc Coc) chọn 1-chạm.
4. **Lite** — APK < 30MB, mở nhanh, ít RAM. Phù hợp máy 2-4GB RAM.
5. **Privacy by default** — Cookies/cache có thể xoá nhanh, không sync cloud, không cần đăng nhập.

---

## 4. App ID & Branding

| Thuộc tính | Giá trị |
|-----------|---------|
| **Application ID** | `com.asianmobile.privatebrower` |
| **Ads module namespace** | `com.asianmobile.privatebrower.ads` |
| **App display name** | `Private Browser: Safe & Secure` (xem string `app_name` trong `strings.xml`) |
| **Version (tham chiếu)** | 1.1.9 (theo footer screenshot Settings) |
| **Min SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK** | 36 |
| **Compile SDK** | 36 |
| **Primary brand color** | Tím gradient `#5B6FFB → #7C5BFB` |
| **Secondary accent** | Green `#22C55E`, Orange `#FB923C` (icon trên onboarding) |
| **Theme** | Light only (v1). Dark mode để v2. |

App icon: hình mặt nạ (mask) màu tím trên nền tròn — biểu tượng "private/anonymous".

---

## 5. Reference Visuals — Mapping Screenshot → Spec

Tất cả screenshot demo nằm tại `docs/assets/screenshots/`. Bảng dưới map screenshot → screen spec:

| Screenshot file | Screen ID | Mô tả |
|-----------------|-----------|-------|
| `Screenshot_20260608-094929.png` | S05 | Set as default browser onboarding |
| `Screenshot_20260608-094940.png` | S06a | Home tab — search + bookmarks + quick access |
| `Screenshot_20260608-095023.png` | S06b | Tabs tab — Normal/Incognito tabs |
| `Screenshot_20260608-095034.png` | S06c | Files tab — File Manager |
| `Screenshot_20260608-095040.png` | S06d | Progress tab — empty state download |
| `Screenshot_20260608-095113.png` | S10 | How to Download tutorial |
| `Screenshot_20260608-095125.png` | S09 | Settings |
| `Screenshot_20260608-095406.png` | S08 | Bookmarks tab (empty state) |
| `Screenshot_20260608-095415.png` | S11 | Search engine picker bottom sheet |

---

## 6. Out Of Scope (v1)

Các tính năng **KHÔNG** làm trong v1, để v2+:

- ❌ Sync bookmarks/history qua cloud
- ❌ Multi-device login
- ❌ Password manager / autofill
- ❌ Dark mode toàn app (chỉ light mode v1)
- ❌ Reader mode (clean view)
- ❌ Ad blocker built-in
- ❌ VPN tích hợp
- ❌ HLS merge thành MP4 (chỉ download segment, để v2 dùng ffmpeg merge)
- ❌ Voice search
- ❌ Tablet/foldable optimized layout (responsive cơ bản OK, không tối ưu sâu)

---

## 7. Success Metrics (cho team product)

Tham khảo — không liên quan đến implementation nhưng giúp hiểu priority:

- D1 retention > 30%
- Download attempts / DAU > 0.4
- Bookmark add / WAU > 0.2
- % users set as default browser > 15%
- Ad ARPDAU > $0.05

---

## 8. Glossary

| Thuật ngữ | Định nghĩa |
|-----------|-----------|
| **Onboarding** | Flow lần đầu mở app: Language → Intro → SetDefault → Permission → Home |
| **Tab Home (bottom nav)** | Tab đầu tiên trong bottom nav (browser home + quick access) |
| **Tab "Tabs"** | Tab thứ 2, hiển thị danh sách WebView tab đang mở |
| **Incognito** | Chế độ duyệt không lưu cookie/history |
| **Quick access** | Grid 8 shortcut social media trên tab Home |
| **Sniff video** | Browser tự dò URL video trong page đang load |
| **Search engine picker** | Bottom sheet chọn 1 trong 6 engine search mặc định |
| **WebView** | Native Android component render web page |
| **DataStore** | Lưu trữ preferences key-value (thay SharedPreferences) |
| **Room** | SQLite ORM cho Bookmarks/History/Tabs/Downloads |
| **Hilt** | Dependency injection framework |
| **sdp/ssp** | Scalable dp/sp libraries (intuit) — chia dp/sp Figma cho 1.3 |
