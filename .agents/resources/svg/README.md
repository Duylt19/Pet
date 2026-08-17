# SVG Working Resources

Thư mục này chỉ dùng lưu SVG nguồn tạm thời export từ Figma trước khi convert sang Android VectorDrawable.

## Rules

- File key/node ID lấy từ Figma URL của task hiện tại; không dùng key legacy mặc định.
- Token chỉ đọc từ `FIGMA_ACCESS_TOKEN`, không ghi vào file hoặc Git.
- Export frame container để giữ đúng viewBox/padding.
- Tên SVG trùng drawable đích: `ic_<name>.svg` hoặc `ic_logo_<name>.svg`.
- Không copy SVG trực tiếp vào `res/`; Android drawable phải là VectorDrawable XML hoặc PNG/WebP.
- Xóa working asset không còn dùng; không giữ danh sách asset của product cũ trong tài liệu base.

## Convert bằng Android Studio

1. Chọn `app/src/main/res/drawable/` → New → Vector Asset.
2. Asset Type: Local file.
3. Chọn SVG trong thư mục này.
4. Giữ tên `snake_case`, kiểm tra size/viewBox và preview.
5. Import, sau đó verify drawable trên light/dark background nếu có tint.
