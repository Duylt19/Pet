# Migration and publish runbook

## Phân loại version

| Thay đổi | Version | App code trước server? |
|---|---|---|
| Thêm item/asset, metadata, checksum/format, reorder bằng field có sẵn | `catalogVersion` | Không nếu format đã hỗ trợ |
| Thêm optional field có fallback | tăng schema để audit rõ | Có parser tolerant trước |
| Xóa/đổi field/type/path contract/ID semantics | `schemaVersion` | Bắt buộc đọc được cũ+mới trước |

`schemaVersion` mô tả shape/semantics parser. `catalogVersion` là revision nội dung/cache.

Trong giai đoạn debug trước khi schema v1 được chốt/`APPROVED`, optional Discover curation
(`trendingPetIds`, `trendingEmojiThemeIds`) được coi là một phần của baseline v1. Reader phải có
fallback cho JSON/cache v1 cũ; sau khi baseline được chốt, mọi thay đổi shape mới quay lại policy
tăng schema ở bảng trên. Việc đổi giá trị/thứ tự của hai list đã có chỉ tăng `catalogVersion`.

## Stable ID từ baseline v1

Lần WebP này được coi là tạo baseline mới, không migrate dữ liệu debug cũ. Từ baseline:

1. ID đã publish không đổi nghĩa và không tái sử dụng.
2. Thêm item dùng ID mới.
3. Reorder bằng `order/priority`, không re-index.
4. Xóa item phải định nghĩa fallback; ưu tiên hidden/deprecated.
5. Nếu bắt buộc remap, phải có bảng `oldId -> newId`, migration DataStore/draft và test trước
   server. Không để ID cũ silently thành nội dung mới.

## Quy trình update

1. Đồng bộ app/server với main/master và tạo branch.
2. Xác nhận ownership/license; giữ `REVIEW_REQUIRED` khi chưa duyệt.
3. Import bằng pipeline, không sửa JSON/hash thủ công.
4. Battery: nếu owner tự convert lossless WebP, thực hiện sau cùng và cập nhật integrity metadata.
5. Tăng catalogVersion/capturedAt; cập nhật count/path/bytes/SHA/dimension.
6. Chạy schema, validators và tests.
7. Review ID/order, asset add/delete và dung lượng trước/sau.
8. Test app online fresh, cache, offline, missing/corrupt asset, selection/download.
9. Commit JSON + assets + tool/schema/docs cùng commit rồi push server.
10. Chỉ đổi `APPROVED` sau review; theo dõi lỗi trước khi dọn revision nguồn.

Server verification:

```bash
python3 -m unittest discover -s tools/tests -p 'test_*.py'
python3 tools/catalog_pipeline.py validate
python3 tools/football_pet_pipeline.py validate
python3 tools/battery_catalog_pipeline.py validate
python3 tools/room_catalog_pipeline.py validate
git diff --check
```

App verification:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
git diff --check
```

## Rollback

- Tạo commit mới phục hồi cả catalog và đúng asset set của revision tốt.
- Không chỉ sửa JSON nếu file cũ đã bị xóa.
- Không đổi ID semantics. Đặt catalogVersion rollback mới để ETag/cache revalidate.
- Cache keyed theo checksum giúp app đang chạy tiếp tục dùng revision hợp lệ trong lúc rollback.

## Checklist

- [ ] ID/name/order không đổi nghĩa ngoài kế hoạch.
- [ ] Count, unique ID/path đúng.
- [ ] Preview nhẹ hơn full; alpha/dimension đúng.
- [ ] Battery static là PNG hoặc lossless WebP do owner convert; GIF/Lottie không bị convert.
- [ ] Mọi path tồn tại và khớp bytes/SHA-256.
- [ ] App parser/client hỗ trợ schema/extension trước server.
- [ ] Fresh/cache/offline/corrupt download đã test.
- [ ] Không có token/private source ngoài runtime asset.
- [ ] Distribution status phản ánh đúng trạng thái duyệt.
