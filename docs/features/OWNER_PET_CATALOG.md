# Owner Pet Catalog

## Production server contract

Catalog production được đọc từ private GitHub repository:

```text
Server-Emoji-Battery-Shimeji-Pet-AM/
├── json/pets.json
├── data/<petId>.zip
└── thumb/<petId>.png
```

`json/pets.json` schema v1 chứa `catalogVersion`, source commit, 268 category và 1.026
record. Mỗi record giữ identity/name/category/author cùng relative path, byte size và SHA-256
của thumbnail/ZIP.

App dùng raw base URL:

```text
https://raw.githubusercontent.com/Asian-Mobile-Inc/Server-Emoji-Battery-Shimeji-Pet-AM/master
```

Repository private yêu cầu `Authorization: Bearer <token>`. Token chỉ đọc từ Firebase Remote
Config key `github_token_pet_server`; default source rỗng và token không được commit/log.
Coil interceptor chỉ gắn header cho đúng raw host và repository path này để không leak token
sang request khác.

`RemoteOwnerPetCatalogRepository`:

1. fetch và parse catalog server;
2. cache JSON cuối hợp lệ tại `files/pet_catalog/pets.json`;
3. fallback cache khi server/network không khả dụng;
4. expose cùng `OwnerPetCatalogSnapshot` cho UI;
5. tải ZIP vào `cache/pet_catalog_archives` chỉ khi user bấm `Set`;
6. stream download với giới hạn 20 MiB, kiểm tra declared size + SHA-256;
7. chỉ đưa ZIP hợp lệ qua `LegacyShimejiPackInstaller`.

Thumbnail được Coil tải lazy và dùng disk cache. Catalog không preload 1.026 thumbnail/ZIP.

## UI behavior

- Screen hiển thị toàn bộ 1.026 record và 268 category từ remote hoặc cached catalog.
- Search khớp name, category hoặc creator không phân biệt hoa thường.
- Category rail có `All`, sau đó sort theo số pet và tên.
- `Set` tải đúng một ZIP, verify integrity, normalize/install và chọn đúng slot.
- Add flow chỉ tăng `petCount` sau khi Set/Import thành công; Back không tạo pet.
- Pack đã cài tiếp tục mở detail/select không cần download lại.
- Catalog/ZIP lỗi không thay renderer hoặc selection đang chạy.

## On-demand legacy conversion

Raw archive không được expand trên server hoặc khi load catalog. Sau khi download/verify,
`LegacyShimejiPackInstaller` convert đúng pet được chọn vào:

```text
files/pet_packs/installed/owner.shimeji.<petId>/4/
├── manifest.json
└── frames/<normalized available frame>.png
```

Conversion dùng staging + atomic promote, guard path/entry/size/expansion/image bounds, ưu
tiên canonical filenames, normalize upper-case/suffixed filenames và sửa hai GIF mislabeled
PNG của pack `136`. Pinned source ZIP không bị mutate và installed revision cũ vẫn đọc được.

## Source snapshot và server import

Owner-authorized source snapshot vẫn nằm ngoài app Git dưới `private_data/`. Server pipeline
chỉ copy `shimeji.json`-referenced `data/<id>.zip` và `thumb/<id>.png`; không copy nested
`.git`, audit report hoặc custom asset không thuộc runtime catalog.

Server validator đối chiếu:

- schema/catalog version;
- unique ID và metadata bắt buộc;
- đúng 1.026 record, 2.052 asset;
- path theo ID;
- category counts;
- file existence, byte size và SHA-256.

Local device sync tool vẫn tồn tại để audit/debug snapshot độc lập, nhưng không còn là data
source production.
