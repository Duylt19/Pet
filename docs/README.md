# Private Browser — Documentation Index

> Bộ tài liệu chính thức cho project **Private Browser** (`com.asianmobile.privatebrower`).
> Đây là nguồn duy nhất (single source of truth) cho mọi AI Agent / developer khi implement tính năng.

---

## Project là gì?

**Private Browser: Safe & Secure** — Trình duyệt Android tập trung vào privacy, có khả năng:
- Browse riêng tư (Incognito mode)
- Multi-tab management
- Tải video trực tiếp từ trang (TikTok, IG, FB, YouTube, v.v.)
- File manager nội bộ (Images / Video / Music)
- Đổi search engine mặc định (Google / Bing / Yahoo / DuckDuckGo / Yandex / Coc Coc)
- Bookmarks + History
- Quick access social shortcuts
- Set as default browser (RoleManager API 29+)
- Premium (Go Ad-Free) via Google Billing

Project kế thừa kiến trúc **Single-Activity + Clean Architecture + MVVM + Jetpack Compose + Hilt** từ base project.

> ✅ **Trạng thái: Đã hoàn thành 12/12 milestones** (M1-M12). Xem [09_IMPLEMENTATION_ROADMAP.md](09_IMPLEMENTATION_ROADMAP.md) và [IMPLEMENTATION_PROGRESS.md](../IMPLEMENTATION_PROGRESS.md).

---

## Cách Sử Dụng Bộ Doc Này

**Thứ tự đọc khuyến nghị cho AI Agent / dev mới:**

1. [README.md](README.md) — file này
2. [01_PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md) — hiểu sản phẩm
3. [02_ARCHITECTURE.md](02_ARCHITECTURE.md) — hiểu kiến trúc
4. [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md) — bản đồ toàn app
5. [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md) — design tokens & components
6. [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md) — quy tắc code bắt buộc
7. Sau đó tra cứu **feature** hoặc **screen** khi cần implement

---

## Bản Đồ Tài Liệu

### Foundation (11 files)

| # | File | Mô tả ngắn |
|---|------|-----------|
| 00 | [README.md](README.md) | Index điều hướng (file này) |
| 01 | [01_PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md) | Sản phẩm, user, USP, branding |
| 02 | [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | Multi-module, MVVM, DI, 6 Hilt modules |
| 03 | [03_TECH_STACK.md](03_TECH_STACK.md) | Libraries + versions (cập nhật chính xác) |
| 04 | [04_NAVIGATION_FLOW.md](04_NAVIGATION_FLOW.md) | 13 routes + navigation flow + deep link |
| 05 | [05_DATA_MODEL.md](05_DATA_MODEL.md) | Room 4 entities + 21 DataStore keys |
| 06 | [06_UI_DESIGN_SYSTEM.md](06_UI_DESIGN_SYSTEM.md) | Colors, typography, 12 reusable components |
| 07 | [07_ADS_INTEGRATION.md](07_ADS_INTEGRATION.md) | Ads matrix 16 screens |
| 08 | [08_AGENT_CODING_GUIDELINES.md](08_AGENT_CODING_GUIDELINES.md) | 20 coding rules bắt buộc |
| 09 | [09_IMPLEMENTATION_ROADMAP.md](09_IMPLEMENTATION_ROADMAP.md) | 12 milestones — ✅ all done |
| 10 | [10_SCREEN_TRACKING.md](10_SCREEN_TRACKING.md) | Firebase screen names, lifecycle và pager visibility |

### Features (11 files)

| # | File | Mô tả ngắn |
|---|------|-----------|
| F01 | [features/F01_BROWSER_CORE.md](features/F01_BROWSER_CORE.md) | WebView engine, lifecycle |
| F02 | [features/F02_INCOGNITO_MODE.md](features/F02_INCOGNITO_MODE.md) | Private session, cookie isolation |
| F03 | [features/F03_TABS_MANAGER.md](features/F03_TABS_MANAGER.md) | Tab CRUD, thumbnail capture |
| F04 | [features/F04_BOOKMARKS_HISTORY.md](features/F04_BOOKMARKS_HISTORY.md) | Bookmark + history Room |
| F05 | [features/F05_DOWNLOAD_MANAGER.md](features/F05_DOWNLOAD_MANAGER.md) | Sniff video + download service |
| F06 | [features/F06_FILE_MANAGER.md](features/F06_FILE_MANAGER.md) | MediaStore queries + storage bar |
| F07 | [features/F07_SEARCH_ENGINE.md](features/F07_SEARCH_ENGINE.md) | 6 engines + URL builder |
| F08 | [features/F08_SET_DEFAULT_BROWSER.md](features/F08_SET_DEFAULT_BROWSER.md) | RoleManager flow |
| F09 | [features/F09_CLEAR_HISTORY.md](features/F09_CLEAR_HISTORY.md) | Clear cookie/cache/history |
| F10 | [features/F10_QUICK_ACCESS.md](features/F10_QUICK_ACCESS.md) | Social shortcuts grid |
| F11 | [features/F11_SHARE_FEEDBACK.md](features/F11_SHARE_FEEDBACK.md) | Share intent + mailto |

### Screens (17 files)

| # | File | Screenshot ref |
|---|------|----------------|
| S01 | [screens/S01_SPLASH.md](screens/S01_SPLASH.md) | — |
| S02 | [screens/S02_LANGUAGE.md](screens/S02_LANGUAGE.md) | — |
| S03 | [screens/S03_INTRO.md](screens/S03_INTRO.md) | — |
| S04 | [screens/S04_PERMISSION.md](screens/S04_PERMISSION.md) | — |
| S05 | [screens/S05_SET_DEFAULT_BROWSER.md](screens/S05_SET_DEFAULT_BROWSER.md) | screenshot #1 |
| S06 | [screens/S06_HOME_CONTAINER.md](screens/S06_HOME_CONTAINER.md) | (container 4 tab) |
| S06a | [screens/S06a_HOME_BROWSER_TAB.md](screens/S06a_HOME_BROWSER_TAB.md) | screenshot #2 |
| S06b | [screens/S06b_TABS_TAB.md](screens/S06b_TABS_TAB.md) | screenshot #3 |
| S06c | [screens/S06c_FILES_TAB.md](screens/S06c_FILES_TAB.md) | screenshot #4 |
| S06d | [screens/S06d_PROGRESS_TAB.md](screens/S06d_PROGRESS_TAB.md) | screenshot #5 |
| S07 | [screens/S07_BROWSER_WEBVIEW.md](screens/S07_BROWSER_WEBVIEW.md) | — |
| S08 | [screens/S08_BOOKMARKS_HISTORY.md](screens/S08_BOOKMARKS_HISTORY.md) | screenshot #8 |
| S09 | [screens/S09_SETTINGS.md](screens/S09_SETTINGS.md) | screenshot #7 |
| S10 | [screens/S10_HOW_TO_DOWNLOAD.md](screens/S10_HOW_TO_DOWNLOAD.md) | screenshot #6 |
| S11 | [screens/S11_SEARCH_ENGINE_PICKER.md](screens/S11_SEARCH_ENGINE_PICKER.md) | screenshot #9 |
| S12 | [screens/S12_LANGUAGE_SETTINGS.md](screens/S12_LANGUAGE_SETTINGS.md) | — |
| S13 | [screens/S13_PREMIUM.md](screens/S13_PREMIUM.md) | — |
| S14 | [screens/S14_PRIVACY_POLICY.md](screens/S14_PRIVACY_POLICY.md) | — |

---

## Cách Đọc 1 Screen Spec

Mỗi file `Sxx_*.md` đều theo template chuẩn 8 phần:

1. **Visual reference** — Path tới screenshot + Figma node
2. **Mục đích** — User goal screen này phục vụ
3. **Vị trí trong navigation** — Route + entry/exit + back behavior
4. **Layout breakdown** — Top → bottom, đầy đủ component + spacing
5. **States** — Loading / Empty / Error / Success
6. **ViewModel contract** — Dependencies + UiState + actions
7. **Resources cần thêm** — strings.xml / colors.xml / drawables
8. **Ads** — Loại + vị trí + điều kiện show/skip
9. **Edge cases & accessibility**
10. **Acceptance criteria** — Checklist

---

## Bản Đồ Liên Kết Screen ↔ Feature

| Screen | Feature liên quan |
|--------|-------------------|
| S05 SetDefaultBrowser | F08 |
| S06a Home Browser tab | F07, F10 |
| S06b Tabs tab | F03, F02 |
| S06c Files tab | F06, F05 |
| S06d Progress tab | F05 |
| S07 BrowserWebView | F01, F02, F05 (sniff) |
| S08 Bookmarks/History | F04 |
| S09 Settings | F07, F08, F09, F11 |
| S10 HowToDownload | F05 |
| S11 SearchEnginePicker | F07 |
| S14 PrivacyPolicy | F01, F11 |

---

## Convention Trong Doc

- **Code examples**: viết bằng English
- **Mô tả/giải thích**: viết bằng Tiếng Việt (theo SKILL.md)
- **File paths**: dùng forward slash kể cả khi đang ở Windows: `app/src/main/...`
- **Markdown links**: tương đối, không tuyệt đối
- **Resource naming**: tuân thủ rules trong `08_AGENT_CODING_GUIDELINES.md`
- **Khi gặp ambiguous**: doc luôn ghi rõ "TODO confirm với user" thay vì đoán

---

## Quick Links

- Skill detail: [../.agents/skills/android_developer/SKILL.md](../.agents/skills/android_developer/SKILL.md)
- Progress tracking: [../IMPLEMENTATION_PROGRESS.md](../IMPLEMENTATION_PROGRESS.md)
- ProGuard rules: `app/proguard-rules.pro`
- AndroidManifest: `app/src/main/AndroidManifest.xml`
