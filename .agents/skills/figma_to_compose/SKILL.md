---
name: figma-to-compose
description: Code UI Android (Jetpack Compose) từ link Figma — hỗ trợ tạo màn hình mới và sửa/update UI theo design mới.
---

# Figma-to-Compose UI Coding Skill

Skill này hướng dẫn AI agent code UI Android bằng Jetpack Compose dựa trên link Figma. Hỗ trợ cả **tạo màn hình mới** và **sửa/update UI** theo design.

---

## 0. Nguyên Tắc Chung (General Principles)

### 0.1. Giao Tiếp
- **TẤT CẢ** giao tiếp, giải thích với người dùng **PHẢI** bằng **Tiếng Việt**.
- Code, tên biến, commit message → Tiếng Anh.
- Dịch sát nghĩa, tự nhiên. **KHÔNG** dịch word-by-word.

### 0.2. Tính Khách Quan — KHÔNG ĐOÁN MÒ
- **TUYỆT ĐỐI** không tự ý quyết định khi gặp vấn đề mơ hồ về UI, Logic, hoặc Architecture.
- Nếu chưa rõ → **BẮT BUỘC dừng lại và hỏi** người dùng.
- Chỉ thực thi khi đã nắm rõ yêu cầu và có sự đồng ý.

### 0.3. Xác Định Loại Yêu Cầu
Khi nhận yêu cầu, **PHẢI** xác định thuộc loại nào:

| Loại | Mô tả | Hành động |
|---|---|---|
| **CREATE** | Tạo màn hình/component mới hoàn toàn | Thực hiện **Quy trình A** (Section 1-7) |
| **UPDATE** | Sửa/cập nhật UI theo design Figma mới | Thực hiện **Quy trình B** (Section 8) |
| **BUG FIX** | Sửa lỗi giao diện so với Figma | Thực hiện **Quy trình B** (Section 8) |

### 0.4. Codex Runtime Notes
- Skill này đã được cập nhật cho Codex. Dùng Figma MCP tools hiện tại: `mcp__figma.get_design_context`, `mcp__figma.get_screenshot`, `mcp__figma.get_metadata`, `mcp__figma.get_variable_defs`.
- Tất cả Figma MCP calls cần `fileKey`; nếu user đưa URL thì extract `fileKey` và đổi `node-id=1-2` thành `nodeId=1:2`.
- Khi đọc code hiện tại, dùng `rg`, `sed`, `nl`, `ls` qua shell thay cho tool cũ `view_file` / `grep_search`.
- Khi sửa file, dùng `apply_patch`; không ghi file bằng shell redirection.
- Trên workspace Linux/Codex dùng `./gradlew compileDebugKotlin`. Chỉ dùng `.\gradlew.bat` khi chạy Windows native.

---

## QUY TRÌNH A: TẠO MÀN HÌNH MỚI (CREATE)

---

## 1. BƯỚC 1 — Screenshot & Tổng Quan Design (Overview)

**Mục tiêu:** Xem toàn cảnh design để hiểu bố cục tổng thể trước khi đi vào chi tiết.

### 1.1. Lấy Screenshot
Dùng Figma MCP tool `mcp__figma.get_screenshot` để chụp ảnh tổng quan:

```
Tool: mcp__figma.get_screenshot
Params: { "fileKey": "<file_key>", "nodeId": "<extracted_from_url>", "maxDimension": 2048 }
```

> **Cách extract nodeId từ URL:**
> - URL: `https://figma.com/design/:fileKey/:fileName?node-id=1-2` → nodeId = `1:2`
> - URL chứa `/branch/:branchKey/` → dùng branchKey làm fileKey

### 1.2. Phân Tích Bố Cục Tổng Quan
Sau khi xem screenshot, **PHẢI liệt kê** cho người dùng:
1. **Layout chính** (Column, Row, Box, Scaffold, LazyColumn...)
2. **Các component/section** trong màn hình (Header, Content, Bottom bar...)
3. **Các trạng thái** nếu nhận thấy (empty state, loading, error...)
4. **Các interactive element** (button, input, toggle, clickable items...)

### 1.3. Lấy Metadata (nếu cần)
Nếu design phức tạp hoặc có nhiều layer, dùng `mcp__figma.get_metadata` để xem cấu trúc node tree:

```
Tool: mcp__figma.get_metadata
Params: {
  "fileKey": "<file_key>",
  "nodeId": "<node_id>",
  "clientLanguages": "kotlin",
  "clientFrameworks": "jetpack-compose"
}
```

### 1.4. Trình Bày Phân Tích & Chờ Xác Nhận
**BẮT BUỘC** trình bày kết quả phân tích cho người dùng và **CHỜ XÁC NHẬN** trước khi code:
- Liệt kê các component sẽ tạo
- Đề xuất cấu trúc file (Screen, ViewModel, UiState)
- Hỏi nếu có điểm chưa rõ

---

## 2. BƯỚC 2 — Design Context & Design Tokens

**Mục tiêu:** Lấy chi tiết design (colors, fonts, spacing, layout properties) để code chính xác pixel-perfect.

### 2.1. Lấy Design Context
Dùng `mcp__figma.get_design_context` cho từng component/section chính:

```
Tool: mcp__figma.get_design_context
Params: {
  "fileKey": "<file_key>",
  "nodeId": "<component_node_id>",
  "clientLanguages": "kotlin",
  "clientFrameworks": "jetpack-compose"
}
```

### 2.2. Lấy Variable Definitions (nếu có)
Nếu design sử dụng Figma variables (design system tokens):

```
Tool: mcp__figma.get_variable_defs
Params: {
  "fileKey": "<file_key>",
  "nodeId": "<node_id>"
}
```

### 2.3. Mapping Design Tokens → Android Resources

#### Colors
- **TẤT CẢ** màu sắc **PHẢI** khai báo trong `res/values/colors.xml`
- **KHÔNG** hardcode hex color trong Composable (`Color(0xFF...)` ❌)
- Dùng `colorResource(R.color.colors_XXXXXX)` trong Compose
- **Naming:** `colors_<HEX_CODE>` (ví dụ: `colors_0D0D0D` cho `#0D0D0D`)
- **Ngoại lệ:** Màu primary/special có thể đặt tên theo chức năng

```xml
<!-- colors.xml -->
<color name="colors_0D0D0D">#0D0D0D</color>
<color name="colors_007BFD">#007BFD</color>
```

```kotlin
// Compose
Text(color = colorResource(id = R.color.colors_0D0D0D))
```

#### Strings
- **TẤT CẢ** text **PHẢI** khai báo trong `res/values/strings.xml`
- **KHÔNG** hardcode text trong Composable (`Text("Hello")` ❌)
- Dùng `stringResource(R.string.xxx)` trong Compose
- **Naming:** `snake_case` — `[screen]_[position/purpose]`

```xml
<!-- strings.xml -->
<string name="home_search_placeholder">Search or type URL</string>
```

```kotlin
// Compose
Text(text = stringResource(id = R.string.home_search_placeholder))
```

#### Sizing (sdp/ssp) — QUY TẮC CRITICAL
- **TẤT CẢ** giá trị dp/sp từ Figma **BẮT BUỘC chia cho 1.3** để quy đổi sang sdp/ssp
- Figma `13px` → `_10sdp`, Figma `16px` → `_12sdp`, Figma `20px` → `_15sdp`
- **Kích thước/padding:** `dimensionResource(com.intuit.sdp.R.dimen._Xsdp)`
- **Typography:** `dimensionResource(com.intuit.ssp.R.dimen._Xssp)`
- **Border radius:** `RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._Xsdp))`

```kotlin
// Ví dụ: Figma padding = 16px → 16/1.3 ≈ 12 → _12sdp
Modifier.padding(dimensionResource(com.intuit.sdp.R.dimen._12sdp))

// Ví dụ: Figma font-size = 14sp → 14/1.3 ≈ 11 → _11ssp
fontSize = with(LocalDensity.current) {
    dimensionResource(com.intuit.ssp.R.dimen._11ssp).toSp()
}
```

#### Font Mapping
- Map `Inter` font family → font files trong `res/font/`
- Nếu font chưa có → thêm mới vào `res/font/`

---

## 3. BƯỚC 3 — Export Icons & Assets

**Mục tiêu:** Export tất cả icon/asset cần thiết trước khi code layout.

### 3.1. Phân Loại Asset: Vector Icon vs Image/Logo (CRITICAL)

> ⚠️ **TRƯỚC KHI EXPORT**, phải xác định asset thuộc loại nào để chọn đúng phương pháp export.

**Bảng phân loại:**

| Tiêu chí | Vector Icon | Image / App Logo |
|---|---|---|
| **Số màu** | 1-2 màu (mono/dual-tone) | Nhiều màu, gradient phức tạp |
| **Cấu trúc Figma** | Frame → Vector/Path đơn giản | Frame → nhiều layer, mask, gradient, clip-path |
| **Ví dụ** | Navigation icons, action buttons, tab icons | App logos (Google, Instagram, Facebook...), photos, illustrations |
| **Tên layer Figma** | `solar:home-2-bold`, `eva:mic-fill` | `App logo 088`, `icons8-x 1`, `threads-app-icon 1` |
| **Có rounded rect/circle background riêng?** | Không (hoặc code bằng Compose) | Có (background là phần của logo) |
| **Cần tint trong code?** | ✅ Có (đổi màu theo theme/state) | ❌ Không (giữ nguyên multi-color) |

**Cách nhận biết nhanh:**
1. Dùng `get_screenshot` để xem visual → nếu icon multi-color hoặc có app logo style → **Image**
2. Dùng `get_metadata` → nếu node có nhiều child layers phức tạp (mask, gradient, clip-path) → **Image**
3. Nếu `get_design_context` trả về nhiều SVG fragments (> 2 URLs) → **Image**
4. Nếu tên layer chứa "App logo", "logo", tên app (instagram, facebook...) → **Image**

### 3.2. Quy Tắc Xác Định Node Export (CRITICAL)

> ⚠️ **QUAN TRỌNG:** Designer **LUÔN** đặt icon bên trong 1 frame (container) nhằm đồng bộ kích thước.
> Khi export icon, **BẮT BUỘC** export cả **frame bên ngoài** (node cha chứa icon), **KHÔNG** export riêng vector/path con bên trong.

**Cấu trúc phổ biến trong Figma:**
```
[Frame 24×24 hoặc 32×32]     ← ✅ EXPORT NODE NÀY (node cha = container frame)
  └── [Vector/Group]          ← ❌ KHÔNG export riêng node này
```

**Ví dụ thực tế:**
- Figma URL: `?node-id=11009-43` → Frame `si:copy-line` (24×24)
  - Bên trong chứa: paths/vectors nhỏ hơn
  - → Export nodeId `11009:43` (frame), **KHÔNG** export nodeId con
- Frame đảm bảo kích thước đồng nhất (24dp, 32dp...) cho tất cả icon

**Cách nhận biết node nào là frame container:**
1. Dùng `get_metadata` → tìm node có `name` mô tả icon (vd: `solar:home-2-bold`, `eva:mic-fill`)
2. Node đó thường là frame có kích thước cố định (24×24, 32×32, 42×42...)
3. Các node con bên trong là vector/group chứa path thực tế

**Quy trình xác định đúng node:**
1. Từ `get_metadata`, xác định frame chứa icon
2. Dùng `get_design_context` trên **frame node** để lấy SVG URL
3. SVG URL trả về sẽ bao gồm cả viewBox đúng kích thước frame

### 3.3. Export Icons & Assets (SVG → Android Studio Convert)

> ⚠️ **KHÔNG dùng `svg_to_drawable.js` script** — script gây lỗi với:
> - Icon dùng **stroke** (mất stroke, chỉ hiện fill trống)
> - **Viewport không vuông** (icon méo khi render)
> - **Path format sai** (Android không parse được → hiện icon lỗi)
> 
> **→ Tất cả icons** (cả đơn giản lẫn phức tạp) đều dùng **Figma REST API → SVG → Android Studio Convert**.

#### Lưu ý quan trọng khi code Icon trong Compose:

**1. KHÔNG dùng `.clip(CircleShape)` hoặc `.clip(RoundedCornerShape)` trên Icon:**
```kotlin
// ❌ SAI — clip cắt mất phần icon vượt ra ngoài shape
Icon(modifier = Modifier.size(22.dp).clip(CircleShape))

// ✅ ĐÚNG — không clip icon
Icon(modifier = Modifier.size(22.dp))
```

**2. Icon có dynamic content (số tabs, badge, counter):**
- Trong Figma, frame icon có thể chứa **text node** (vd: số "1" trong icon tabs)
- **VẪN export từ frame** (để giữ đúng viewBox = kích thước frame)
- Sau khi download SVG, **xóa path chứa text** khỏi SVG (text thường là path `fill` riêng, không có `stroke`)
- Render số bằng `Text()` composable overlay lên icon:
```kotlin
Box(contentAlignment = Alignment.Center) {
    Icon(painter = painterResource(R.drawable.ic_feature_counter), ...)
    Text(
        text = tabCount.toString(),
        modifier = Modifier.offset(x = (-1).sdp, y = 2.sdp) // Adjust to match Figma
    )
}
```

### 3.4. Quy Trình Export (Áp dụng cho TẤT CẢ icons/logos)

> Áp dụng cho: **Vector Icon**, **Image / App Logo**, **Vector phức tạp**
> Dùng **Figma REST API** export frame tổng thành 1 SVG hoàn chỉnh.
> Sau đó user convert bằng **Android Studio Vector Asset**.
> **Dynamic text:** Export frame → xóa text path khỏi SVG → code render text bằng `Text()` overlay.

> ⚠️ **Figma MCP** (`mcp__figma.get_design_context`) có thể tách frame phức tạp thành nhiều asset fragments — **KHÔNG** luôn cho 1 SVG đầy đủ.
> **→ Dùng Figma REST API** để export frame tổng thành 1 SVG hoàn chỉnh.

#### Figma Access Token (CRITICAL — Kiểm tra TRƯỚC KHI export)

**TRƯỚC KHI** gọi Figma REST API, **BẮT BUỘC** kiểm tra:

1. **Chưa có token** → **DỪNG LẠI**, hỏi user cung cấp Figma Personal Access Token
2. **Đã có token** → thử gọi API, kiểm tra response

**Xử lý lỗi API:**
| HTTP Status | Nguyên nhân | Hành động |
|---|---|---|
| `403 Forbidden` | Token hết hạn hoặc không có quyền | Hỏi user cung cấp token mới |
| `401 Unauthorized` | Token không hợp lệ | Hỏi user kiểm tra lại token |
| `404 Not Found` | File key hoặc node ID sai | Kiểm tra lại Figma URL |
| `429 Too Many Requests` | Rate limit | Chờ và thử lại sau |
| Network error | Không kết nối được | Kiểm tra mạng |

**Cách hỏi user:**
```
🔑 Cần Figma Access Token để export SVG frame phức tạp.
Figma MCP không hỗ trợ export frame tổng — cần gọi Figma REST API trực tiếp.

Vui lòng cung cấp:
1. Figma Personal Access Token (tạo tại: Figma → Settings → Personal Access Tokens)
2. Hoặc export SVG thủ công từ Figma Desktop (Select frame → Export → SVG)
```

> ⚠️ **KHÔNG** tiếp tục export nếu thiếu token hoặc token lỗi.
> **KHÔNG** cố dùng MCP fragments để thay thế — kết quả sẽ không chính xác.

**Quy trình export (sau khi có token hợp lệ):**

**Bước 1:** Dùng **Figma REST API** export frame tổng thành SVG:
```bash
TOKEN="<FIGMA_ACCESS_TOKEN>"
FILE_KEY="<FILE_KEY>"          # Lấy từ Figma URL: figma.com/design/<fileKey>/...
NODE_IDS="11010:180,11010:194" # Có thể export nhiều node cùng lúc

curl -sS -H "X-Figma-Token: $TOKEN" \
  "https://api.figma.com/v1/images/$FILE_KEY?ids=$NODE_IDS&format=svg" \
  -o /tmp/figma_images.json

# Kiểm tra: nếu err không null → token lỗi hoặc file/node sai
node -e 'const j=require("/tmp/figma_images.json"); if (j.err) { console.error(j.err); process.exit(1) }'
```

**Bước 2:** Download SVG từ URL trả về, lưu vào `.agents/resources/svg/`:
```bash
# Tên file PHẢI TRÙNG với XML placeholder (chỉ khác extension)
SVG_URL=$(node -e 'const j=require("/tmp/figma_images.json"); console.log(j.images["11010:180"])')
curl -L "$SVG_URL" -o ".agents/resources/svg/ic_logo_instagram.svg"
```

**Bước 2.1:** Verify SVG — kiểm tra có chứa embedded bitmap không:
```bash
# Kiểm tra SVG có chứa embedded PNG/image không
rg -n "data:image|<image" .agents/resources/svg/ic_logo_*.svg
```
- Nếu **KHÔNG** có `data:image` → SVG thuần vector ✅
- Nếu **CÓ** `data:image` → SVG chứa embedded bitmap ⚠️
  - Figma dùng **image fill** cho node này (không phải vector fill)
  - Android Studio **có thể không import được** file này
  - → Export thêm bản **PNG** backup: thay `format=svg` → `format=png&scale=3` trong API URL
  - → Lưu PNG cùng thư mục: `.agents/resources/svg/ic_logo_<tên>.png`

**Bước 3:** Tạo file placeholder VectorDrawable rỗng trong `drawable/` (để code compile được):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="43dp"
    android:height="43dp"
    android:viewportWidth="56"
    android:viewportHeight="56">
    <!-- PLACEHOLDER: Convert từ .agents/resources/svg/ bằng Android Studio -->
    <!-- File > New > Vector Asset > Local File > chọn SVG tương ứng -->
</vector>
```

**Bước 4:** Thông báo cho user thực hiện convert bằng Android Studio:
```
📋 CẦN CONVERT THỦ CÔNG:
1. Mở Android Studio
2. Chuột phải drawable/ → New → Vector Asset
3. Asset Type: Local file (SVG, PSD)
4. Path: .agents/resources/svg/<tên>.svg
5. Name: <tên> (trùng placeholder)
6. Size: <width>dp × <height>dp
7. Next → Finish → Overwrite
```

**Quy tắc code cho Image/Logo (sau khi convert):**
```kotlin
// ✅ ĐÚNG — Image composable, KHÔNG tint, có clip rounded corners
Image(
    painter = painterResource(R.drawable.ic_logo_google),
    contentDescription = "Google",
    contentScale = ContentScale.Fit,
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._43sdp))
        .clip(RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._9sdp)))
)

// ❌ SAI — Icon composable + tint sẽ phá hủy multi-color
Icon(
    painter = painterResource(R.drawable.ic_logo_google),
    tint = Color.White  // MẤT HẾT MÀU GỐC!
)
```

### 3.5. Quy Tắc Đặt Tên
- **Vector icon:** Prefix `ic_` — ví dụ: `ic_nav_back.xml`, `ic_close.xml`, `ic_search.xml`
- **App logo:** Prefix `ic_logo_` — ví dụ: `ic_logo_google.xml`, `ic_logo_instagram.xml`
- **Image (PNG/WebP):** Prefix `img_` — ví dụ: `img_home_banner.webp`, `img_logo_facebook.png`
- **Naming:** `snake_case`, mô tả chức năng

### 3.6. Xử Lý Icon Phức Tạp (Nhiều Layer)
Khi Figma tách 1 icon thành nhiều SVG fragment:
1. Dùng `get_design_context` trên **frame node** (node cha cấp cao nhất chứa icon) thay vì các vector con
2. Hoặc dùng `get_screenshot` để xem icon hoàn chỉnh, tìm frame node phù hợp
3. Nếu SVG vẫn bị tách → **tự tạo VectorDrawable XML thủ công** bằng cách merge paths

### 3.7. Icon + Background Layer Pattern (CRITICAL)
Figma thường cấu trúc icon với background:
```
[Container Circle/Shape]     ← background bán trong suốt
  └── [Icon Frame]           ← frame chứa icon (có thể export)
        └── [Vector/SVG]     ← path icon thực tế
```

**QUY TẮC cho Vector Icon (1-2 màu):**
- ❌ **KHÔNG** export cả background (circle/shape) cùng icon thành 1 drawable
- ✅ **Export frame chứa icon** → VectorDrawable XML (bao gồm viewBox chuẩn)
- ✅ **Background/Container** → code bằng Compose (`Box` + `Shape`)

**QUY TẮC cho App Logo (multi-color):**
- ✅ **Export CẢ frame bao gồm background** → multi-layer VectorDrawable hoặc PNG
- ✅ Background là phần của logo → giữ nguyên trong drawable
- ❌ **KHÔNG** tách background ra code Compose (vì background là branding)

### 3.8. Lottie Animations
- Lưu JSON vào `res/raw/`

---

## 4. BƯỚC 4 — Code UI Layout (Compose)

**Mục tiêu:** Code Composable UI chính xác theo design.

### 4.1. Cấu Trúc File — MVVM Pattern
Khi tạo màn hình mới, **LUÔN** tạo 3 file trong feature package `ui/<feature>/`:

| File | Nội dung |
|---|---|
| `[Feature]Screen.kt` | Composable UI layout |
| `[Feature]ViewModel.kt` | Business logic (`@HiltViewModel`) |
| `[Feature]UiState.kt` | Data class cho UI state |

### 4.2. Screen Template

```kotlin
package com.asianmobile.emojibattery.shimeji.ui.<feature>

import androidx.compose.runtime.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.asianmobile.emojibattery.shimeji.R

@Composable
fun <Feature>Screen(
    onNavigateBack: () -> Unit = {},
    viewModel: <Feature>ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    <Feature>Content(
        uiState = uiState,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun <Feature>Content(
    uiState: <Feature>UiState,
    onNavigateBack: () -> Unit
) {
    // Layout code here
}

@Preview(showBackground = true)
@Composable
private fun <Feature>ScreenPreview() {
    <Feature>Content(
        uiState = <Feature>UiState(),
        onNavigateBack = {}
    )
}
```

### 4.3. ViewModel Template

```kotlin
package com.asianmobile.emojibattery.shimeji.ui.<feature>

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class <Feature>ViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(<Feature>UiState())
    val uiState: StateFlow<<Feature>UiState> = _uiState.asStateFlow()
}
```

### 4.4. UiState Template

```kotlin
package com.asianmobile.emojibattery.shimeji.ui.<feature>

data class <Feature>UiState(
    val isLoading: Boolean = false
    // Thêm properties từ design analysis
)
```

### 4.5. Quy Tắc Code Compose

#### Modifier Order (CRITICAL)
Thứ tự modifier chuẩn cho interactive component:
```
.size()          → kích thước
.shadow()        → shadow (nếu có, PHẢI trước clip)
.clip(shape)     → clip shape (CRITICAL: PHẢI trước clickable)
.background()    → màu nền
.border()        → viền (nếu có)
.clickable()     → click + ripple (SẼ bị clip theo shape phía trên)
.padding()       → padding nội dung bên trong
```

#### Ripple Effect — `.clip()` TRƯỚC `.clickable()`
```kotlin
// ❌ SAI — ripple hình vuông
Modifier.size(26.sdp).background(Color.White, CircleShape).clickable(onClick = action)

// ✅ ĐÚNG — ripple hình tròn
Modifier.size(26.sdp).clip(CircleShape).background(Color.White).clickable(onClick = action)
```

#### Clickable Icon Patterns

> **LƯU Ý QUAN TRỌNG:** Vì icon đã được export cả **frame bên ngoài** (Section 3.2),
> drawable đã bao gồm viewBox đúng kích thước frame. Do đó:
> - ❌ **KHÔNG CẦN** wrap `Icon` trong `Box` chỉ để tạo gap/padding nội bộ
> - ❌ **KHÔNG CẦN** set 2 kích thước khác nhau (container vs inner icon)
> - ✅ **CHỈ CẦN** 1 `Modifier.size()` duy nhất = kích thước frame (đã chia 1.3)
> - ✅ **CHỈ CẦN** quan tâm padding/spacing giữa icon và các view xung quanh

**Icon CÓ background (circle/shape) — CẦN Box:**
```kotlin
// Chỉ dùng Box khi Figma design có background shape NHÌN THẤY (circle, rounded rect...)
Box(
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._26sdp))
        .clip(CircleShape)
        .background(colorResource(R.color.colors_FFFFFF).copy(alpha = 0.85f))
        .clickable(onClick = onAction),
    contentAlignment = Alignment.Center
) {
    Icon(
        painter = painterResource(R.drawable.ic_icon_name),
        contentDescription = stringResource(R.string.icon_description),
        tint = colorResource(R.color.colors_808080),
        // Kích thước = frame size (đã chia 1.3) — drawable đã có gap nội bộ
        modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
    )
}
```

**Icon KHÔNG CÓ background (standalone) — KHÔNG cần Box:**
```kotlin
// Frame đã export bao gồm viewBox chuẩn → chỉ cần 1 size duy nhất
// ⚠️ KHÔNG dùng IconButton khi cần kích thước chính xác (min touch target = 48dp)
Icon(
    painter = painterResource(R.drawable.ic_icon_name),
    contentDescription = stringResource(R.string.icon_description),
    tint = colorResource(R.color.colors_FFFFFF),
    modifier = Modifier
        .size(dimensionResource(com.intuit.sdp.R.dimen._18sdp)) // = frame 24px / 1.3
        .clip(CircleShape)
        .clickable(onClick = onAction)
)
```

**Bảng quyết định:**

| Figma Design | Android Code | Sizing |
|---|---|---|
| Icon + circle/shape background hiện rõ | `Box(size=bg)` + `Icon(size=frame)` | Box = bg size, Icon = frame size |
| Icon không background nhưng clickable | `Icon(size=frame)` + `.clip().clickable()` | Chỉ 1 size = frame (đã chia 1.3) |
| Icon không clickable (chỉ hiển thị) | `Icon(size=frame)` | Chỉ 1 size = frame (đã chia 1.3) |

#### Figma Layer → Compose Mapping

| Figma Layer | Compose Code | Thuộc tính |
|---|---|---|
| Frame (Auto Layout - Vertical) | `Column` | spacing, padding, alignment |
| Frame (Auto Layout - Horizontal) | `Row` | spacing, padding, alignment |
| Frame (Fixed) | `Box` | size, position |
| Frame (Scroll) | `LazyColumn` / `LazyRow` | items, spacing |
| Rectangle | `Box` + `background()` | size, color, radius |
| Circle/Ellipse | `Box` + `clip(CircleShape)` + `background()` | size, color |
| Text | `Text` | font, size, color, weight |
| Image | `Image` / `AsyncImage` (Coil) | size, contentScale |
| Line/Divider | `Divider` / `HorizontalDivider` | color, thickness |
| Component Instance | Composable function riêng | reusable |
| Gradient fill | `Brush.linearGradient()` / `Brush.verticalGradient()` | colors, direction |
| Shadow (Drop Shadow) | `Modifier.shadow()` | elevation, shape |
| Opacity | `.alpha()` hoặc `.copy(alpha = X)` | alpha value |

---

## 5. BƯỚC 5 — Navigation Route

**Mục tiêu:** Đăng ký route mới trong Navigation Graph.

### 5.1. Thêm Route vào `Routes` object
File: `navigation/NavGraph.kt`

```kotlin
object Routes {
    // ... existing routes
    const val NEW_FEATURE = "new_feature"
}
```

### 5.2. Thêm Destination vào NavGraph

```kotlin
composable(Routes.NEW_FEATURE) {
    <Feature>Screen(
        onNavigateBack = { navController.safePopBackStack() }
    )
}
```

### 5.3. Navigation Extensions
Sử dụng safe navigation:
- `navController.safeNavigate(Routes.DESTINATION)` — navigate an toàn
- `navController.safePopBackStack()` — pop an toàn
- `navigateWithAd(context) { ... }` — navigate có interstitial ad

---

## 6. BƯỚC 6 — Resources Finalization

**Mục tiêu:** Đảm bảo tất cả resources đã được khai báo đầy đủ.

### 6.1. Checklist Resources

- [ ] **`colors.xml`**: Tất cả màu mới đã thêm, naming đúng quy tắc `colors_<HEX>`
- [ ] **`strings.xml`**: Tất cả text đã thêm, naming đúng `[screen]_[purpose]`
- [ ] **`drawable/`**: Tất cả icons đã export và đặt tên đúng `ic_<name>.xml`
- [ ] **`font/`**: Font mới (nếu có) đã thêm
- [ ] Không có hardcoded string nào trong Composable
- [ ] Không có hardcoded color nào trong Composable

### 6.2. Kiểm Tra Trùng Lặp
Trước khi thêm resource mới, **PHẢI kiểm tra** xem đã tồn tại chưa:

```bash
# Kiểm tra color đã có chưa
rg "colors_FF5722" app/src/main/res/values/colors.xml

# Kiểm tra string đã có chưa
rg "home_title" app/src/main/res/values/strings.xml

# Kiểm tra icon đã có chưa
ls app/src/main/res/drawable/ic_close*
```

---

## 7. BƯỚC 7 — Verify & Commit

### 7.1. Compile Check
Chạy compile để verify không có lỗi:

```bash
./gradlew compileDebugKotlin
```

> ⚠️ **KHÔNG** chạy `assembleRelease` hoặc `assembleDebug` (mất 4-5 phút).
> Dùng `compileDebugKotlin` (~15-20 giây) là đủ.

### 7.2. Fix Lỗi Compile
Nếu có lỗi:
1. Đọc kỹ error message
2. Sửa lỗi
3. Chạy compile lại
4. Lặp lại cho đến khi pass

### 7.3. Git Commit
Sau khi compile thành công, **TỰ ĐỘNG** tạo git commit:

```bash
git add -A
git commit -m "Handle UI <Feature Name>"
```

**Pattern commit message:**
- `Handle UI [Screen Name]` — code UI mới
- `Handle feature [Feature Name]` — code feature mới
- `Fix bug UI [Screen/Component]` — sửa lỗi giao diện
- `Update UI [Screen/Component]` — cập nhật UI theo design mới

---

## QUY TRÌNH B: SỬA / UPDATE UI (UPDATE / BUG FIX)

---

## 8. Quy Trình Sửa/Update UI Theo Figma

### 8.0. Mandatory Analysis Protocol (KHÔNG ĐOÁN MÒ)

**QUY TẮC TỐI THƯỢNG:** Khi nhận yêu cầu sửa lỗi hoặc update UI, **BẮT BUỘC** thực hiện các bước sau TRƯỚC KHI code:

#### Bước 1: Làm rõ yêu cầu
- Đọc kỹ yêu cầu → hiểu mục tiêu cuối cùng
- Nếu mơ hồ → **PHẢI hỏi**

#### Bước 2: Screenshot Figma — BẮT BUỘC (CRITICAL)

> ⚠️ **TUYỆT ĐỐI KHÔNG** bắt đầu code fix khi chưa xem ảnh Figma.
> **PHẢI** download/screenshot design Figma để có hình ảnh cụ thể trước khi phân tích.

**Quy trình lấy ảnh Figma:**

1. **Export ảnh PNG từ Figma REST API** cho node liên quan:
```bash
# Export ảnh design
wget --header="X-FIGMA-TOKEN: <TOKEN>" -O - \
  "https://api.figma.com/v1/images/<FILE_KEY>?ids=<NODE_IDS>&format=png&scale=2"
# Download ảnh về thư mục artifacts
wget -O <artifact_dir>/figma_reference_<component>.png "<URL_TRẢ_VỀ>"
```

2. **Hoặc dùng Figma MCP `mcp__figma.get_screenshot`** nếu có:
```
Tool: mcp__figma.get_screenshot
Params: { "fileKey": "<FILE_KEY>", "nodeId": "<NODE_ID>", "maxDimension": 2048 }
```
Tool trả về screenshot URL và curl instruction; tải ảnh về artifact bằng `curl -L "<URL>" -o <artifact_dir>/figma_reference_<component>.png`.

3. **Embed ảnh vào artifact** để so sánh trực quan:
```markdown
## Figma Reference
![Figma design](absolute/path/to/figma_reference.png)
```

**Nếu KHÔNG thể download ảnh** (API lỗi, token hết hạn...):
- Dùng `mcp__figma.get_design_context` để lấy thông tin chi tiết layout
- **PHẢI** phân tích kỹ output: position, size, gap, alignment, colors
- **KHÔNG** dựa vào trí nhớ hoặc giả định về design

#### Bước 3: So sánh Screenshot App vs Figma (CRITICAL)

**BẮT BUỘC** đặt cạnh nhau và liệt kê **TỪNG ĐIỂM** khác biệt:

| Thuộc tính | Figma Design | App hiện tại | Cần sửa? |
|---|---|---|---|
| Vị trí icon X | Trên cùng, căn giữa | Góc phải thumbnail | ✅ |
| Màu nền popup | #333538 | Trắng (Material default) | ✅ |
| Khoảng cách items | gap: 12px | gap: 6sdp | ✅ |
| Font weight title | SemiBold 600 | Medium 500 | ✅ |

> ⚠️ **KHÔNG** chỉ nhìn sơ qua — PHẢI phân tích từng pixel:
> - **Position/Alignment**: top/center/bottom, start/center/end
> - **Spacing**: gap, padding, margin (Figma px ÷ 1.3 = sdp)
> - **Size**: width, height của từng element
> - **Colors**: background, text, icon tint, border
> - **Typography**: font family, weight, size, line height
> - **Corner radius**: border radius
> - **Layer order**: z-index, overlay vs stacked

#### Bước 4: Phân tích Code hiện tại
- Dùng `rg`, `sed`, `nl` hoặc file reads trong Codex để tìm code đang quản lý UI đó
- Phân tích code: đơn vị (dp/sdp), padding/margin, layout structure
- **Đối chiếu thông số code vs Figma:**
  - "Figma yêu cầu gap 16px (~12sdp) nhưng code đang dùng 8sdp"
  - "Figma font weight Bold nhưng code đang dùng Medium"
  - "Figma layout = Column(alignItems: center) nhưng code dùng Box(align: TopEnd)"

#### Bước 5: Lập kế hoạch sửa
- Trình bày kết quả phân tích cho người dùng
- Đề xuất phương án sửa cụ thể (file nào, dòng nào, giá trị mới)
- **CHỜ XÁC NHẬN** trước khi thực hiện (nếu thay đổi lớn)
- Nếu chỉ fix nhỏ rõ ràng → có thể tiến hành luôn nhưng PHẢI giải thích

### 8.1. Thực Hiện Sửa Đổi
Sau khi người dùng xác nhận:
1. Sửa code theo kế hoạch
2. Cập nhật resources nếu cần (colors, strings)
3. Export lại icons nếu design đổi icon

### 8.2. Verify & Commit
Thực hiện giống Bước 7 của Quy trình A:
1. `./gradlew compileDebugKotlin`
2. Fix lỗi nếu có
3. Git commit với message phù hợp:
   - `Fix bug UI [Component]` — sửa lỗi
   - `Update UI [Component]` — update theo design mới

---

## 9. Quy Tắc Bổ Sung (Additional Rules)

### 9.1. Preview Functions
**LUÔN** thêm `@Preview` cho tất cả major screens và components:

```kotlin
@Preview(showBackground = true)
@Composable
private fun <Component>Preview() {
    <Component>(
        // Default/sample parameters
    )
}
```

### 9.2. Keep Composables Pure
- Extract side effects vào `LaunchedEffect` hoặc ViewModel events
- Composable chỉ nhận state và emit events
- Tách Screen (có ViewModel) và Content (stateless) composable

### 9.3. Tái Sử Dụng Components
- Components dùng chung → đặt trong `ui/component/`
- Components chỉ dùng trong 1 screen → đặt cùng package với screen đó
- Components dùng cho 1 feature area → đặt trong `ui/<feature>/component/`

### 9.4. Ads Integration
Khi design có vị trí quảng cáo:
- **Native Ad:** `NativeAdInternal(screenCode = SCREEN_XXX)`
- **Banner Ad:** `BannerAd(modifier = Modifier.fillMaxWidth())`
- Tham khảo skill `android_developer` Section 5 cho chi tiết

### 9.5. Accessibility
- **TẤT CẢ** Icon clickable phải có `contentDescription`
- `contentDescription` lấy từ `stringResource()`
- Icon decorative (không clickable, không mang ý nghĩa) → `contentDescription = null`

---

## 10. Checklist Tổng Hợp (Final Checklist)

Trước khi hoàn tất, kiểm tra:

### Code Quality
- [ ] Không hardcode string nào trong Composable
- [ ] Không hardcode color nào trong Composable
- [ ] Tất cả sizing dùng sdp/ssp (đã chia 1.3 từ Figma)
- [ ] Modifier order đúng chuẩn (clip trước clickable)
- [ ] Có @Preview cho tất cả major screens/components
- [ ] Composables pure — side effects trong LaunchedEffect/ViewModel

### Architecture
- [ ] Có đủ 3 file: Screen, ViewModel, UiState
- [ ] ViewModel dùng @HiltViewModel + @Inject constructor
- [ ] Screen tách thành Screen (có VM) + Content (stateless)
- [ ] Navigation route đã thêm vào NavGraph

### Resources
- [ ] colors.xml — tất cả màu mới, naming `colors_<HEX>`
- [ ] strings.xml — tất cả text, naming `[screen]_[purpose]`
- [ ] drawable/ — tất cả icons exported, naming `ic_<name>`
- [ ] Không resource nào bị trùng lặp

### Verification
- [ ] `compileDebugKotlin` pass thành công
- [ ] Git commit đã tạo với message đúng pattern
