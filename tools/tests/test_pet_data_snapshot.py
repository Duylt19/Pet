import tempfile
import unittest
import zipfile
from pathlib import Path

from tools.pet_data_snapshot import PNG_SIGNATURE, audit_zip, custom_asset_path, is_safe_zip_name


class PetDataSnapshotTest(unittest.TestCase):
    def test_zip_path_policy_rejects_traversal_and_backslashes(self):
        self.assertTrue(is_safe_zip_name("sprites/shime1.png"))
        self.assertFalse(is_safe_zip_name("../outside.png"))
        self.assertFalse(is_safe_zip_name("sprites\\shime1.png"))
        self.assertFalse(is_safe_zip_name("/absolute.png"))

    def test_custom_asset_url_maps_to_repository_path(self):
        path = custom_asset_path(
            "https://raw.githubusercontent.com/org/repo/refs/heads/main/custom_pet/avatar/cat.webp"
        )
        self.assertEqual(Path("custom_pet/avatar/cat.webp"), path)
        self.assertIsNone(
            custom_asset_path("https://example.test/custom_pet/../../outside.webp")
        )

    def test_zip_audit_reports_frame_range_and_digest(self):
        with tempfile.TemporaryDirectory() as directory:
            archive_path = Path(directory) / "7.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.writestr("shime1.png", PNG_SIGNATURE + b"first")
                archive.writestr("shime46.png", PNG_SIGNATURE + b"last")

            result = audit_zip(archive_path)

            self.assertEqual(2, result.frame_count)
            self.assertEqual(1, result.frame_min)
            self.assertEqual(46, result.frame_max)
            self.assertEqual(list(range(2, 46)), result.missing_frame_numbers)
            self.assertEqual([], result.extra_frame_numbers)
            self.assertEqual([], result.duplicate_frame_numbers)
            self.assertEqual([], result.noncanonical_frame_names)
            self.assertGreater(result.uncompressed_bytes, 0)
            self.assertEqual([], result.errors)
            self.assertEqual(64, len(result.digest.sha256))

    def test_zip_audit_reports_noncanonical_frame_names_for_normalization(self):
        with tempfile.TemporaryDirectory() as directory:
            archive_path = Path(directory) / "8.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.writestr("shime1 (1).png", PNG_SIGNATURE + b"renamed")

            result = audit_zip(archive_path)

            self.assertEqual(1, result.frame_count)
            self.assertEqual(["shime1 (1).png"], result.noncanonical_frame_names)
            self.assertEqual([], result.errors)

    def test_zip_audit_reports_duplicate_candidate_for_normalization(self):
        with tempfile.TemporaryDirectory() as directory:
            archive_path = Path(directory) / "9.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.writestr("shime4.png", PNG_SIGNATURE + b"canonical")
                archive.writestr("shime4b.png", PNG_SIGNATURE + b"alternative")

            result = audit_zip(archive_path)

            self.assertEqual([4], result.duplicate_frame_numbers)
            self.assertEqual(["shime4b.png"], result.noncanonical_frame_names)
            self.assertEqual([], result.errors)


if __name__ == "__main__":
    unittest.main()
