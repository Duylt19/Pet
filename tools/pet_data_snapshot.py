#!/usr/bin/env python3
"""Clone and audit an owner-authorized Anime Shimeji data snapshot."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import subprocess
import sys
import zipfile
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlparse


DEFAULT_REPOSITORY = "https://github.com/ConfigNeko/EmojShimeji.git"
DEFAULT_BRANCH = "main"
FRAME_CANDIDATE_PATTERN = re.compile(r"^shime(\d+).*\.png$", re.IGNORECASE)
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


@dataclass(frozen=True)
class FileDigest:
    path: str
    bytes: int
    sha256: str


@dataclass(frozen=True)
class PackAudit:
    id: int
    zip: FileDigest | None
    thumbnail: FileDigest | None
    frame_count: int
    frame_min: int | None
    frame_max: int | None
    missing_frame_numbers: list[int]
    extra_frame_numbers: list[int]
    duplicate_frame_numbers: list[int]
    noncanonical_frame_names: list[str]
    non_png_frame_names: list[str]
    uncompressed_bytes: int
    errors: list[str]


@dataclass(frozen=True)
class ZipAudit:
    digest: FileDigest
    frame_count: int
    frame_min: int | None
    frame_max: int | None
    missing_frame_numbers: list[int]
    extra_frame_numbers: list[int]
    duplicate_frame_numbers: list[int]
    noncanonical_frame_names: list[str]
    non_png_frame_names: list[str]
    uncompressed_bytes: int
    errors: list[str]


def sha256_file(path: Path) -> FileDigest:
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            size += len(chunk)
            digest.update(chunk)
    return FileDigest(path=path.as_posix(), bytes=size, sha256=digest.hexdigest())


def relative_digest(source: Path, path: Path, digest: FileDigest) -> FileDigest:
    return FileDigest(
        path=path.relative_to(source).as_posix(),
        bytes=digest.bytes,
        sha256=digest.sha256,
    )


def is_safe_zip_name(name: str) -> bool:
    path = PurePosixPath(name)
    return bool(name) and not path.is_absolute() and "\\" not in name and all(
        part not in {"", ".", ".."} for part in path.parts
    )


def audit_zip(path: Path) -> ZipAudit:
    errors: list[str] = []
    frames: list[int] = []
    noncanonical_frame_names: list[str] = []
    non_png_frame_names: list[str] = []
    uncompressed_bytes = 0
    digest = sha256_file(path)
    try:
        with zipfile.ZipFile(path) as archive:
            infos = archive.infolist()
            if not infos:
                errors.append("empty_zip")
            for info in infos:
                uncompressed_bytes += info.file_size
                if not is_safe_zip_name(info.filename):
                    errors.append(f"unsafe_path:{info.filename}")
                match = FRAME_CANDIDATE_PATTERN.fullmatch(info.filename)
                if match and not info.is_dir():
                    frame_number = int(match.group(1))
                    frames.append(frame_number)
                    if info.filename != f"shime{frame_number}.png":
                        noncanonical_frame_names.append(info.filename)
                    with archive.open(info) as frame_stream:
                        if frame_stream.read(len(PNG_SIGNATURE)) != PNG_SIGNATURE:
                            non_png_frame_names.append(info.filename)
            corrupt = archive.testzip()
            if corrupt is not None:
                errors.append(f"crc_error:{corrupt}")
    except (OSError, zipfile.BadZipFile, RuntimeError) as error:
        errors.append(f"invalid_zip:{type(error).__name__}")
    unique_frames = sorted(set(frames))
    duplicate_frames = sorted({number for number in frames if frames.count(number) > 1})
    if not unique_frames:
        errors.append("no_numbered_frame_candidates")
    missing_frames = sorted(set(range(1, 47)) - set(unique_frames))
    extra_frames = sorted(set(unique_frames) - set(range(1, 47)))
    return ZipAudit(
        digest=digest,
        frame_count=len(unique_frames),
        frame_min=unique_frames[0] if unique_frames else None,
        frame_max=unique_frames[-1] if unique_frames else None,
        missing_frame_numbers=missing_frames,
        extra_frame_numbers=extra_frames,
        duplicate_frame_numbers=duplicate_frames,
        noncanonical_frame_names=noncanonical_frame_names,
        non_png_frame_names=non_png_frame_names,
        uncompressed_bytes=uncompressed_bytes,
        errors=errors,
    )


def validate_png(path: Path) -> tuple[FileDigest, list[str]]:
    errors: list[str] = []
    digest = sha256_file(path)
    with path.open("rb") as stream:
        if stream.read(len(PNG_SIGNATURE)) != PNG_SIGNATURE:
            errors.append("invalid_png_signature")
    return digest, errors


def load_json_list(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8") as stream:
        value = json.load(stream)
    if not isinstance(value, list) or not all(isinstance(item, dict) for item in value):
        raise ValueError(f"{path} must contain a JSON object list")
    return value


def custom_asset_path(url: str) -> Path | None:
    parts = PurePosixPath(unquote(urlparse(url).path)).parts
    try:
        index = parts.index("custom_pet")
    except ValueError:
        return None
    relative_parts = parts[index:]
    if any(part in {"", ".", ".."} for part in relative_parts):
        return None
    return Path(*relative_parts)


def git_output(source: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(source), *args],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return result.stdout.strip()


def clone_snapshot(repository: str, branch: str, target: Path) -> None:
    if target.exists():
        raise FileExistsError(f"Refusing to overwrite existing target: {target}")
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            "git",
            "clone",
            "--depth",
            "1",
            "--single-branch",
            "--branch",
            branch,
            repository,
            str(target),
        ],
        check=True,
    )


def audit_snapshot(
    source: Path,
    report_path: Path,
    inventory_path: Path,
    checksums_path: Path,
) -> dict:
    catalog = load_json_list(source / "shimeji.json")
    custom_catalog = load_json_list(source / "custompet.json")
    ids = [item.get("id") for item in catalog]
    if not all(isinstance(item_id, int) and item_id >= 0 for item_id in ids):
        raise ValueError("shimeji.json contains an invalid pet id")
    if len(ids) != len(set(ids)):
        raise ValueError("shimeji.json contains duplicate pet ids")

    audits: list[PackAudit] = []
    for item_id in ids:
        zip_path = source / "data" / f"{item_id}.zip"
        thumb_path = source / "thumb" / f"{item_id}.png"
        errors: list[str] = []
        zip_digest: FileDigest | None = None
        thumb_digest: FileDigest | None = None
        frame_count = 0
        frame_min: int | None = None
        frame_max: int | None = None
        missing_frame_numbers: list[int] = []
        extra_frame_numbers: list[int] = []
        duplicate_frame_numbers: list[int] = []
        noncanonical_frame_names: list[str] = []
        non_png_frame_names: list[str] = []
        uncompressed_bytes = 0
        if zip_path.is_file():
            zip_audit = audit_zip(zip_path)
            zip_digest = relative_digest(source, zip_path, zip_audit.digest)
            frame_count = zip_audit.frame_count
            frame_min = zip_audit.frame_min
            frame_max = zip_audit.frame_max
            missing_frame_numbers = zip_audit.missing_frame_numbers
            extra_frame_numbers = zip_audit.extra_frame_numbers
            duplicate_frame_numbers = zip_audit.duplicate_frame_numbers
            noncanonical_frame_names = zip_audit.noncanonical_frame_names
            non_png_frame_names = zip_audit.non_png_frame_names
            uncompressed_bytes = zip_audit.uncompressed_bytes
            errors.extend(zip_audit.errors)
        else:
            errors.append("missing_zip")
        if thumb_path.is_file():
            thumb_digest, thumb_errors = validate_png(thumb_path)
            thumb_digest = relative_digest(source, thumb_path, thumb_digest)
            errors.extend(thumb_errors)
        else:
            errors.append("missing_thumbnail")
        audits.append(
            PackAudit(
                id=item_id,
                zip=zip_digest,
                thumbnail=thumb_digest,
                frame_count=frame_count,
                frame_min=frame_min,
                frame_max=frame_max,
                missing_frame_numbers=missing_frame_numbers,
                extra_frame_numbers=extra_frame_numbers,
                duplicate_frame_numbers=duplicate_frame_numbers,
                noncanonical_frame_names=noncanonical_frame_names,
                non_png_frame_names=non_png_frame_names,
                uncompressed_bytes=uncompressed_bytes,
                errors=errors,
            )
        )

    expected_custom_paths: set[Path] = set()
    invalid_custom_urls: list[str] = []
    for pet in custom_catalog:
        urls = [pet.get("avatarUrl"), *pet.get("frameUrls", [])]
        for url in urls:
            if not isinstance(url, str):
                invalid_custom_urls.append(str(url))
                continue
            relative = custom_asset_path(url)
            if relative is None:
                invalid_custom_urls.append(url)
            else:
                expected_custom_paths.add(relative)
    missing_custom = sorted(
        path.as_posix() for path in expected_custom_paths if not (source / path).is_file()
    )
    custom_digests = [
        relative_digest(source, source / path, sha256_file(source / path))
        for path in sorted(expected_custom_paths, key=lambda item: item.as_posix())
        if (source / path).is_file()
    ]
    actual_custom_paths = {
        path.relative_to(source)
        for path in (source / "custom_pet").rglob("*")
        if path.is_file()
    }
    snapshot_files = sorted(
        (
            path
            for path in source.rglob("*")
            if path.is_file() and ".git" not in path.relative_to(source).parts
        ),
        key=lambda path: path.relative_to(source).as_posix(),
    )

    actual_zip_ids = {
        int(path.stem) for path in (source / "data").glob("*.zip") if path.stem.isdigit()
    }
    actual_thumb_ids = {
        int(path.stem) for path in (source / "thumb").glob("*.png") if path.stem.isdigit()
    }
    pack_errors = [audit for audit in audits if audit.errors]
    runtime_normalization_packs = [
        audit
        for audit in audits
        if audit.errors
        or audit.missing_frame_numbers
        or audit.extra_frame_numbers
        or audit.duplicate_frame_numbers
        or audit.noncanonical_frame_names
        or audit.non_png_frame_names
    ]
    noncanonical_name_packs = [audit for audit in audits if audit.noncanonical_frame_names]
    non_png_frame_packs = [audit for audit in audits if audit.non_png_frame_names]
    report = {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "source": {
            "path": str(source.resolve()),
            "repository": git_output(source, "remote", "get-url", "origin"),
            "commit": git_output(source, "rev-parse", "HEAD"),
            "branch": git_output(source, "rev-parse", "--abbrev-ref", "HEAD"),
            "catalogs": {
                "shimeji": asdict(
                    relative_digest(
                        source,
                        source / "shimeji.json",
                        sha256_file(source / "shimeji.json"),
                    )
                ),
                "customPets": asdict(
                    relative_digest(
                        source,
                        source / "custompet.json",
                        sha256_file(source / "custompet.json"),
                    )
                ),
            },
        },
        "summary": {
            "catalogPets": len(catalog),
            "zipFiles": len(actual_zip_ids),
            "thumbnailFiles": len(actual_thumb_ids),
            "transportValidCatalogPacks": len(audits) - len(pack_errors),
            "transportInvalidCatalogPacks": len(pack_errors),
            "runtimeReadyCatalogPacks": len(audits) - len(runtime_normalization_packs),
            "runtimeNormalizationPacks": len(runtime_normalization_packs),
            "noncanonicalNamePacks": len(noncanonical_name_packs),
            "nonPngFramePacks": len(non_png_frame_packs),
            "customPets": len(custom_catalog),
            "expectedCustomAssets": len(expected_custom_paths),
            "presentCustomAssets": len(custom_digests),
            "totalSnapshotFiles": len(snapshot_files),
            "totalSnapshotBytes": sum(path.stat().st_size for path in snapshot_files),
        },
        "extra": {
            "zipIdsNotInCatalog": sorted(actual_zip_ids - set(ids)),
            "thumbnailIdsNotInCatalog": sorted(actual_thumb_ids - set(ids)),
        },
        "custom": {
            "invalidUrls": invalid_custom_urls,
            "missingAssets": missing_custom,
            "unreferencedAssets": sorted(
                path.as_posix() for path in actual_custom_paths - expected_custom_paths
            ),
            "assets": [asdict(item) for item in custom_digests],
        },
        "packs": [asdict(audit) for audit in audits],
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    catalog_by_id = {item["id"]: item for item in catalog}
    inventory_path.parent.mkdir(parents=True, exist_ok=True)
    with inventory_path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(
            stream,
            fieldnames=[
                "id",
                "name",
                "category",
                "author",
                "zip_path",
                "zip_bytes",
                "zip_sha256",
                "thumbnail_path",
                "thumbnail_bytes",
                "thumbnail_sha256",
                "frame_count",
                "frame_min",
                "frame_max",
                "missing_frame_numbers",
                "extra_frame_numbers",
                "duplicate_frame_numbers",
                "noncanonical_frame_names",
                "non_png_frame_names",
                "uncompressed_bytes",
                "errors",
            ],
        )
        writer.writeheader()
        for audit in audits:
            metadata = catalog_by_id[audit.id]
            writer.writerow(
                {
                    "id": audit.id,
                    "name": metadata.get("name", ""),
                    "category": metadata.get("category", ""),
                    "author": metadata.get("author", ""),
                    "zip_path": audit.zip.path if audit.zip else "",
                    "zip_bytes": audit.zip.bytes if audit.zip else "",
                    "zip_sha256": audit.zip.sha256 if audit.zip else "",
                    "thumbnail_path": audit.thumbnail.path if audit.thumbnail else "",
                    "thumbnail_bytes": audit.thumbnail.bytes if audit.thumbnail else "",
                    "thumbnail_sha256": audit.thumbnail.sha256 if audit.thumbnail else "",
                    "frame_count": audit.frame_count,
                    "frame_min": audit.frame_min if audit.frame_min is not None else "",
                    "frame_max": audit.frame_max if audit.frame_max is not None else "",
                    "missing_frame_numbers": "|".join(map(str, audit.missing_frame_numbers)),
                    "extra_frame_numbers": "|".join(map(str, audit.extra_frame_numbers)),
                    "duplicate_frame_numbers": "|".join(
                        map(str, audit.duplicate_frame_numbers)
                    ),
                    "noncanonical_frame_names": "|".join(
                        audit.noncanonical_frame_names
                    ),
                    "non_png_frame_names": "|".join(audit.non_png_frame_names),
                    "uncompressed_bytes": audit.uncompressed_bytes,
                    "errors": "|".join(audit.errors),
                }
            )
    checksums_path.parent.mkdir(parents=True, exist_ok=True)
    with checksums_path.open("w", encoding="utf-8") as stream:
        for path in snapshot_files:
            relative = path.relative_to(source).as_posix()
            stream.write(f"{sha256_file(path).sha256}  {relative}\n")
    return report


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    clone = subparsers.add_parser("clone", help="Create a shallow immutable source snapshot")
    clone.add_argument("target", type=Path)
    clone.add_argument("--repository", default=DEFAULT_REPOSITORY)
    clone.add_argument("--branch", default=DEFAULT_BRANCH)
    audit = subparsers.add_parser("audit", help="Validate and inventory a cloned snapshot")
    audit.add_argument("source", type=Path)
    audit.add_argument("--report", type=Path, required=True)
    audit.add_argument("--inventory", type=Path, required=True)
    audit.add_argument("--checksums", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.command == "clone":
            clone_snapshot(args.repository, args.branch, args.target)
            return 0
        report = audit_snapshot(args.source, args.report, args.inventory, args.checksums)
        print(json.dumps(report["summary"], indent=2))
        summary = report["summary"]
        is_runtime_ready = summary["runtimeNormalizationPacks"] == 0
        custom_is_complete = (
            summary["expectedCustomAssets"] == summary["presentCustomAssets"]
            and not report["custom"]["invalidUrls"]
        )
        return 0 if is_runtime_ready and custom_is_complete else 2
    except (FileExistsError, ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
