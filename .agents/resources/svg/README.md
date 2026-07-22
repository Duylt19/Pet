# SVG Resources

Thư mục chứa các file SVG gốc export từ Figma. Tên file **trùng 1:1** với placeholder XML trong `drawable/`.

## Cách convert SVG → VectorDrawable (Android Studio)

1. Chuột phải `app/src/main/res/drawable/` → **New** → **Vector Asset**
2. Chọn **Asset Type: Local file (SVG, PSD)**
3. **Path:** browse đến file `.svg` tương ứng trong thư mục này
4. **Name:** giữ nguyên tên (đã trùng với placeholder XML)
5. **Size:** 43dp × 43dp (= 56px Figma ÷ 1.3)
6. Click **Next** → **Finish** → overwrite file placeholder

## Danh sách files

| SVG File | Drawable XML | Figma Node | Lưu ý |
|---|---|---|---|
| `ic_logo_google.svg` | `ic_logo_google.xml` | 11010:89 | ✅ SVG vector đầy đủ |
| `ic_logo_instagram.svg` | `ic_logo_instagram.xml` | 11010:180 | ✅ SVG đầy đủ (gradient + camera icon) |
| `ic_logo_facebook.svg` | `ic_logo_facebook.xml` | 11010:194 | ⚠️ SVG chứa embedded PNG bitmap (Figma dùng image fill). Backup PNG: `ic_logo_facebook.png` |
| `ic_logo_tiktok.svg` | `ic_logo_tiktok.xml` | 11010:205 | ✅ SVG vector đầy đủ |
| `ic_logo_whatsapp.svg` | `ic_logo_whatsapp.xml` | 11010:211 | ✅ SVG vector đầy đủ |
| `ic_logo_x.svg` | `ic_logo_x.xml` | 11011:219 | ✅ SVG vector đầy đủ |
| `ic_logo_threads.svg` | `ic_logo_threads.xml` | 11010:216 | ✅ SVG vector đầy đủ |
| `ic_logo_tinder.svg` | `ic_logo_tinder.xml` | 11011:225 | ✅ SVG vector đầy đủ |

> **Exported via:** Figma REST API (`/v1/images/{fileKey}?ids={nodeIds}&format=svg`)
> **Facebook:** SVG chứa embedded base64 PNG vì Figma dùng image fill cho background. 
> Android Studio Vector Asset có thể **không import được** file này → dùng file `ic_logo_facebook.png` thay thế.
