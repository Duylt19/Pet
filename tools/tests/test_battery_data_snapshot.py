import base64
import json
import tempfile
import unittest
from pathlib import Path

from tools.battery_data_snapshot import BatterySnapshotError, audit_snapshot


PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB"
    "9Wl2nS8AAAAASUVORK5CYII="
)


class BatteryDataSnapshotTest(unittest.TestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)
        (self.root / "api").mkdir()
        for directory in ("thumbnails", "batteries", "emojis"):
            target = self.root / "remote" / directory
            target.mkdir(parents=True)
            (target / "7.png").write_bytes(PNG_1X1)
        (self.root / "catalog-stats.json").write_text(
            json.dumps(
                {
                    "captured_at": "2026-07-30T03:19:48.346Z",
                    "battery_count": 1,
                }
            ),
            encoding="utf-8",
        )
        (self.root / "api/categories.raw.json").write_text(
            json.dumps(
                {
                    "success": True,
                    "data": [
                        {
                            "id": 3,
                            "name": "Cute",
                            "slug": "cute",
                            "priority": 0,
                            "is_active": True,
                        }
                    ],
                }
            ),
            encoding="utf-8",
        )
        self.write_batteries(
            [
                {
                    "id": 7,
                    "name": "Theme 7",
                    "category_id": 3,
                    "category_name": "Cute",
                    "is_premium": False,
                }
            ]
        )

    def tearDown(self):
        self.temporary_directory.cleanup()

    def write_batteries(self, records):
        (self.root / "api/batteries.raw.json").write_text(
            json.dumps({"success": True, "data": records}),
            encoding="utf-8",
        )

    def test_audit_builds_deterministic_runtime_catalog(self):
        catalog, report = audit_snapshot(self.root)

        self.assertEqual(1, catalog["schemaVersion"])
        self.assertEqual(1, catalog["themeCount"])
        self.assertEqual("thumb/7.png", catalog["themes"][0]["assets"]["thumbnail"]["path"])
        self.assertEqual("battery/7.png", catalog["themes"][0]["assets"]["battery"]["path"])
        self.assertEqual("emoji/7.png", catalog["themes"][0]["assets"]["emoji"]["path"])
        self.assertEqual("REVIEW_REQUIRED", catalog["source"]["distributionStatus"])
        self.assertEqual(3, report["runtimeAssetCount"])

    def test_audit_rejects_missing_asset(self):
        (self.root / "remote/emojis/7.png").unlink()

        with self.assertRaises(BatterySnapshotError):
            audit_snapshot(self.root)

    def test_audit_rejects_duplicate_ids(self):
        duplicate = {
            "id": 7,
            "name": "Duplicate",
            "category_id": 3,
            "category_name": "Cute",
            "is_premium": True,
        }
        self.write_batteries([duplicate, duplicate])
        stats = json.loads((self.root / "catalog-stats.json").read_text())
        stats["battery_count"] = 2
        (self.root / "catalog-stats.json").write_text(json.dumps(stats))

        with self.assertRaises(BatterySnapshotError):
            audit_snapshot(self.root)


if __name__ == "__main__":
    unittest.main()
