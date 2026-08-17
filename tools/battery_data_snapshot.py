#!/usr/bin/env python3
"""Audit and normalize a locally captured Battery catalog snapshot.

The source snapshot stays under private_data/. This tool emits a deterministic runtime
catalog containing only relative paths, byte sizes and SHA-256 digests. It never copies
source assets into the Android source tree.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SCHEMA_VERSION = 1
EXPECTED_ASSET_KINDS = {
    "thumbnail": ("remote/thumbnails", "thumb"),
    "battery": ("remote/batteries", "battery"),
    "emoji": ("remote/emojis", "emoji"),
}
BUNDLED_ASSET_GROUPS = {
    "backgrounds": ("bundled/assets/background_template", "background", 20),
    "emotions": ("bundled/assets/cute_emotion", "emotion", 20),
}
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
CATEGORY_EMOJI_BY_SLUG = {
    "trending": "🔥",
    "wc-2026": "🏆",
    "football": "⚽",
    "cute": "🥰",
    "stitch": "💙",
    "soccer-ball": "⚽",
    "heart": "❤️",
    "sanrio": "🎀",
    "animal": "🐾",
    "pompompurin": "🍮",
    "dog-cat": "🐶🐱",
    "labubu": "🧸",
    "flower": "🌸",
    "cartoon": "📺",
    "zootopia": "🦊",
    "avatar": "🌊",
    "dog": "🐶",
    "fruit": "🍓",
    "huntrix": "🎤⚔️",
    "demon-slayer": "🗡️",
    "christmas": "🎄",
    "k-pop": "🎤",
    "nature": "🌿",
    "snoopy": "🐶",
    "anime": "✨",
    "shin-chan": "😜",
    "love": "💕",
    "food": "🍔",
    "bunnies": "🐰",
    "brainrot": "🌀",
    "actor": "🎬",
    "halloween": "🎃",
    "thanksgiving": "🦃",
    "lunar-new-year": "🧧",
}


class BatterySnapshotError(ValueError):
    """Raised when the captured snapshot does not satisfy the runtime contract."""


@dataclass(frozen=True)
class AuditedAsset:
    source_path: Path
    runtime_path: str
    size_bytes: int
    sha256: str
    width: int
    height: int

    def to_catalog_value(self) -> dict[str, Any]:
        return {
            "path": self.runtime_path,
            "sizeBytes": self.size_bytes,
            "sha256": self.sha256,
            "width": self.width,
            "height": self.height,
        }


def _load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise BatterySnapshotError(f"Unable to parse {path}") from error


def _required_list(document: Any, path: Path) -> list[dict[str, Any]]:
    if not isinstance(document, dict) or document.get("success") is not True:
        raise BatterySnapshotError(f"{path} is not a successful API snapshot")
    data = document.get("data")
    if not isinstance(data, list) or not data:
        raise BatterySnapshotError(f"{path} has no data records")
    if not all(isinstance(item, dict) for item in data):
        raise BatterySnapshotError(f"{path} contains non-object records")
    return data


def _png_dimensions(path: Path, content: bytes) -> tuple[int, int]:
    if len(content) < 24 or content[:8] != PNG_SIGNATURE or content[12:16] != b"IHDR":
        raise BatterySnapshotError(f"{path} is not a valid PNG")
    width, height = struct.unpack(">II", content[16:24])
    if width <= 0 or height <= 0 or width > 4096 or height > 4096:
        raise BatterySnapshotError(f"{path} has unsafe PNG dimensions")
    return width, height


def _audit_asset(
    source_root: Path,
    source_directory: str,
    runtime_directory: str,
    battery_id: int,
) -> AuditedAsset:
    path = source_root / source_directory / f"{battery_id}.png"
    try:
        content = path.read_bytes()
    except OSError as error:
        raise BatterySnapshotError(f"Missing asset: {path}") from error
    width, height = _png_dimensions(path, content)
    return AuditedAsset(
        source_path=path,
        runtime_path=f"{runtime_directory}/{battery_id}.png",
        size_bytes=len(content),
        sha256=hashlib.sha256(content).hexdigest(),
        width=width,
        height=height,
    )


def _audit_bundled_group(
    source_root: Path,
    source_directory: str,
    runtime_directory: str,
    expected_count: int,
) -> list[dict[str, Any]]:
    directory = source_root / source_directory
    if not directory.is_dir():
        return []
    paths = sorted(directory.glob("*.png"))
    if len(paths) != expected_count:
        raise BatterySnapshotError(
            f"{directory} must contain exactly {expected_count} PNG files"
        )
    records: list[dict[str, Any]] = []
    for index, path in enumerate(paths, start=1):
        content = path.read_bytes()
        width, height = _png_dimensions(path, content)
        records.append(
            {
                "id": index,
                "name": path.stem,
                "asset": AuditedAsset(
                    source_path=path,
                    runtime_path=f"{runtime_directory}/{path.name}",
                    size_bytes=len(content),
                    sha256=hashlib.sha256(content).hexdigest(),
                    width=width,
                    height=height,
                ).to_catalog_value(),
            }
        )
    return records


def _audit_animations(source_root: Path) -> list[dict[str, Any]]:
    directory = source_root / "bundled/assets/cute_animation"
    if not directory.is_dir():
        return []
    gif_paths = sorted(
        directory.glob("*.gif"),
        key=lambda path: int(path.stem),
    )
    lottie_paths = sorted(directory.glob("cute_*.json"))
    if len(gif_paths) != 21 or len(lottie_paths) != 5:
        raise BatterySnapshotError(
            f"{directory} must contain 21 GIF and 5 Lottie JSON files"
        )
    records: list[dict[str, Any]] = []
    for index, path in enumerate(gif_paths + lottie_paths, start=1):
        try:
            content = path.read_bytes()
        except OSError as error:
            raise BatterySnapshotError(f"Unable to read animation: {path}") from error
        if path.suffix == ".gif":
            if len(content) < 10 or content[:6] not in (b"GIF87a", b"GIF89a"):
                raise BatterySnapshotError(f"{path} is not a valid GIF")
            width = int.from_bytes(content[6:8], "little")
            height = int.from_bytes(content[8:10], "little")
            animation_type = "GIF"
        else:
            document = _load_json(path)
            try:
                width = int(document["w"])
                height = int(document["h"])
            except (KeyError, TypeError, ValueError) as error:
                raise BatterySnapshotError(
                    f"{path} is not a valid Lottie document"
                ) from error
            animation_type = "LOTTIE"
        if width <= 0 or height <= 0 or width > 4096 or height > 4096:
            raise BatterySnapshotError(f"{path} has unsafe animation dimensions")
        records.append(
            {
                "id": index,
                "name": path.name,
                "type": animation_type,
                "asset": AuditedAsset(
                    source_path=path,
                    runtime_path=f"animation/{path.name}",
                    size_bytes=len(content),
                    sha256=hashlib.sha256(content).hexdigest(),
                    width=width,
                    height=height,
                ).to_catalog_value(),
            }
        )
    return records


def _normalized_category(record: dict[str, Any]) -> dict[str, Any]:
    try:
        category_id = int(record["id"])
        name = str(record["name"]).strip()
        slug = str(record["slug"]).strip()
        priority = int(record["priority"])
    except (KeyError, TypeError, ValueError) as error:
        raise BatterySnapshotError("Invalid category metadata") from error
    if category_id <= 0 or not name or not slug or "/" in slug or "\\" in slug:
        raise BatterySnapshotError(f"Invalid category {category_id}")
    return {
        "id": category_id,
        "name": name,
        "slug": slug,
        "priority": priority,
    }


def _category_display_name(category: dict[str, Any]) -> str:
    name = category["name"]
    emoji = CATEGORY_EMOJI_BY_SLUG.get(category["slug"])
    if emoji is None or name.startswith(emoji):
        return name
    return f"{emoji} {name}"


def audit_snapshot(source_root: Path) -> tuple[dict[str, Any], dict[str, Any]]:
    source_root = source_root.resolve()
    batteries_path = source_root / "api/batteries.raw.json"
    categories_path = source_root / "api/categories.raw.json"
    stats_path = source_root / "catalog-stats.json"

    battery_records = _required_list(_load_json(batteries_path), batteries_path)
    category_records = _required_list(_load_json(categories_path), categories_path)
    stats = _load_json(stats_path)
    captured_at = str(stats.get("captured_at", "")).strip()
    if not captured_at:
        raise BatterySnapshotError("catalog-stats.json is missing captured_at")

    categories = [
        _normalized_category(record)
        for record in category_records
        if record.get("is_active") is True
    ]
    category_ids = [category["id"] for category in categories]
    if len(category_ids) != len(set(category_ids)):
        raise BatterySnapshotError("Duplicate category IDs")
    category_by_id = {category["id"]: category for category in categories}
    category_display_names = {
        category["id"]: _category_display_name(category)
        for category in categories
    }

    ids: list[int] = []
    themes: list[dict[str, Any]] = []
    category_counts: Counter[int] = Counter()
    byte_counts: Counter[str] = Counter()
    dimension_counts: dict[str, Counter[str]] = defaultdict(Counter)
    digest_ids: dict[str, dict[str, list[int]]] = defaultdict(lambda: defaultdict(list))

    for index, record in enumerate(battery_records):
        try:
            battery_id = int(record["id"])
            name = str(record["name"]).strip()
            category_id = int(record["category_id"])
            category_name = str(record["category_name"]).strip()
            is_premium = bool(record["is_premium"])
        except (KeyError, TypeError, ValueError) as error:
            raise BatterySnapshotError(f"Invalid battery record at index {index}") from error
        if battery_id <= 0 or not name:
            raise BatterySnapshotError(f"Invalid battery ID/name at index {index}")
        category = category_by_id.get(category_id)
        if category is None:
            raise BatterySnapshotError(
                f"Battery {battery_id} references unknown category {category_id}"
            )
        if category_name != category["name"]:
            raise BatterySnapshotError(
                f"Battery {battery_id} has mismatched category name"
            )

        assets: dict[str, dict[str, Any]] = {}
        for kind, (source_directory, runtime_directory) in EXPECTED_ASSET_KINDS.items():
            asset = _audit_asset(
                source_root=source_root,
                source_directory=source_directory,
                runtime_directory=runtime_directory,
                battery_id=battery_id,
            )
            assets[kind] = asset.to_catalog_value()
            byte_counts[kind] += asset.size_bytes
            dimension_counts[kind][f"{asset.width}x{asset.height}"] += 1
            digest_ids[kind][asset.sha256].append(battery_id)

        ids.append(battery_id)
        category_counts[category_id] += 1
        themes.append(
            {
                "id": battery_id,
                "name": name,
                "categoryId": category_id,
                "categoryName": category_display_names[category_id],
                "entitlement": "PREMIUM" if is_premium else "FREE",
                "assets": assets,
            }
        )

    if len(ids) != len(set(ids)):
        raise BatterySnapshotError("Duplicate battery IDs")
    if len(themes) != int(stats.get("battery_count", -1)):
        raise BatterySnapshotError("Catalog count does not match catalog-stats.json")
    if sum(category_counts.values()) != len(themes):
        raise BatterySnapshotError("Category counts do not cover the catalog")

    display_categories = [
        {**category, "name": category_display_names[category["id"]]}
        for category in categories
    ]
    ordered_categories = sorted(
        display_categories,
        key=lambda item: (item["priority"], item["id"]),
    )
    ordered_themes = sorted(themes, key=lambda item: item["id"])
    bundled_groups = {
        name: _audit_bundled_group(
            source_root,
            source_directory,
            runtime_directory,
            expected_count,
        )
        for name, (
            source_directory,
            runtime_directory,
            expected_count,
        ) in BUNDLED_ASSET_GROUPS.items()
    }
    animations = _audit_animations(source_root)
    bundled_asset_count = (
        sum(len(records) for records in bundled_groups.values()) + len(animations)
    )
    bundled_asset_bytes = sum(
        record["asset"]["sizeBytes"]
        for records in bundled_groups.values()
        for record in records
    ) + sum(record["asset"]["sizeBytes"] for record in animations)
    catalog = {
        "schemaVersion": SCHEMA_VERSION,
        "catalogVersion": f"battery-apk-1.0.2@{captured_at}+emoji-category-v1",
        "capturedAt": captured_at,
        "source": {
            "packageName": "com.anime.shimeji.petonscreen",
            "versionName": "1.0.2",
            "distributionStatus": "REVIEW_REQUIRED",
        },
        "categoryCount": len(ordered_categories),
        "themeCount": len(ordered_themes),
        "categories": ordered_categories,
        "themes": ordered_themes,
        **bundled_groups,
        "animations": animations,
    }
    report = {
        "schemaVersion": SCHEMA_VERSION,
        "capturedAt": captured_at,
        "categoryCount": len(ordered_categories),
        "themeCount": len(ordered_themes),
        "freeCount": sum(theme["entitlement"] == "FREE" for theme in ordered_themes),
        "premiumCount": sum(theme["entitlement"] == "PREMIUM" for theme in ordered_themes),
        "runtimeAssetCount": (
            len(ordered_themes) * len(EXPECTED_ASSET_KINDS) + bundled_asset_count
        ),
        "runtimeAssetBytes": sum(byte_counts.values()) + bundled_asset_bytes,
        "bundledAssetCount": bundled_asset_count,
        "bundledAssetBytes": bundled_asset_bytes,
        "assetBytesByKind": dict(sorted(byte_counts.items())),
        "dimensionsByKind": {
            kind: dict(sorted(counts.items()))
            for kind, counts in sorted(dimension_counts.items())
        },
        "duplicateFileCountByKind": {
            kind: sum(len(asset_ids) - 1 for asset_ids in digests.values())
            for kind, digests in sorted(digest_ids.items())
        },
        "categoryCounts": {
            str(category["id"]): category_counts[category["id"]]
            for category in ordered_categories
        },
        "distributionStatus": "REVIEW_REQUIRED",
    }
    return catalog, report


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=False) + "\n",
        encoding="utf-8",
    )


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Audit and normalize a captured Battery catalog snapshot."
    )
    parser.add_argument("source", type=Path, help="Path to battery-data")
    parser.add_argument(
        "--catalog",
        type=Path,
        required=True,
        help="Output path for the normalized runtime catalog",
    )
    parser.add_argument(
        "--report",
        type=Path,
        required=True,
        help="Output path for the audit report",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = _build_parser().parse_args(argv)
    try:
        catalog, report = audit_snapshot(args.source)
        _write_json(args.catalog, catalog)
        _write_json(args.report, report)
    except BatterySnapshotError as error:
        print(f"battery snapshot audit failed: {error}", file=sys.stderr)
        return 2
    print(
        "battery snapshot ready: "
        f"{report['themeCount']} themes, "
        f"{report['runtimeAssetCount']} runtime assets, "
        f"{report['runtimeAssetBytes']} bytes"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
