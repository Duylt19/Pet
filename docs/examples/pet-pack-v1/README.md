# Sunny Cat — pet pack v1 example

Thư mục này là pack hợp lệ tối thiểu để test importer Phase 4. Nén trực tiếp nội dung thư mục sao cho `manifest.json` nằm ở root của file `.zip`; không nén cả thư mục cha.

Sprite `sprites/sunny-cat.png` được tạo riêng cho Emoji Battery bằng built-in image generation, sau đó loại nền chroma thành PNG RGBA. Pack không sử dụng source hoặc asset của ứng dụng đối thủ.

```bash
cd docs/examples/pet-pack-v1
zip -r ../sunny-cat-v1.zip manifest.json sprites
```
