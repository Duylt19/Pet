#!/usr/bin/env python3
"""Sync the owner-authorized pet snapshot into Android app-specific test storage."""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


DEFAULT_PACKAGE = "com.asianmobile.emojibattery.shimeji"
DEFAULT_SOURCE = Path("private_data/anime-shimeji")
PUSH_BATCH_SIZE = 128


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--package", default=DEFAULT_PACKAGE)
    parser.add_argument("--serial")
    parser.add_argument("--adb", type=Path)
    parser.add_argument(
        "--metadata-only",
        action="store_true",
        help="Push only shimeji.json for an empty-asset UI check",
    )
    return parser.parse_args()


def resolve_adb(explicit: Path | None, project_root: Path) -> Path:
    if explicit is not None:
        return explicit
    local_properties = project_root / "local.properties"
    if local_properties.is_file():
        for line in local_properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                candidate = Path(line.removeprefix("sdk.dir=")) / "platform-tools/adb"
                if candidate.is_file():
                    return candidate
    return Path("adb")


def adb_command(adb: Path, serial: str | None, *args: str) -> list[str]:
    command = [str(adb)]
    if serial:
        command += ["-s", serial]
    return [*command, *args]


def run(adb: Path, serial: str | None, *args: str, capture: bool = False) -> str:
    result = subprocess.run(
        adb_command(adb, serial, *args),
        check=True,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
    )
    return result.stdout.strip() if capture else ""


def verify_source(source: Path, metadata_only: bool) -> tuple[int, int]:
    catalog = source / "shimeji.json"
    if not catalog.is_file():
        raise FileNotFoundError(f"Missing catalog: {catalog}")
    if metadata_only:
        return 0, 0
    archives = list((source / "data").glob("*.zip"))
    thumbnails = list((source / "thumb").glob("*.png"))
    if not archives or not thumbnails:
        raise FileNotFoundError("Snapshot data/thumb directories are empty")
    return len(archives), len(thumbnails)


def remote_file_count(
    adb: Path,
    serial: str | None,
    directory: str,
    pattern: str,
) -> int:
    output = run(
        adb,
        serial,
        "shell",
        "find",
        directory,
        "-maxdepth",
        "1",
        "-type",
        "f",
        "-name",
        pattern,
        capture=True,
    )
    return len([line for line in output.splitlines() if line.strip()])


def push_files(
    adb: Path,
    serial: str | None,
    files: list[Path],
    remote_directory: str,
) -> None:
    for start in range(0, len(files), PUSH_BATCH_SIZE):
        batch = files[start : start + PUSH_BATCH_SIZE]
        run(
            adb,
            serial,
            "push",
            *(str(path) for path in batch),
            f"{remote_directory}/",
            capture=True,
        )


def main() -> int:
    args = parse_args()
    project_root = Path(__file__).resolve().parents[1]
    source = (
        (project_root / args.source).resolve()
        if not args.source.is_absolute()
        else args.source
    )
    try:
        expected_archives, expected_thumbnails = verify_source(source, args.metadata_only)
        adb = resolve_adb(args.adb, project_root)
        remote_root = f"/sdcard/Android/data/{args.package}/files/pet_catalog"
        run(adb, args.serial, "shell", "mkdir", "-p", remote_root)
        run(adb, args.serial, "push", str(source / "shimeji.json"), f"{remote_root}/")
        if not args.metadata_only:
            run(adb, args.serial, "shell", "mkdir", "-p", f"{remote_root}/data")
            run(adb, args.serial, "shell", "mkdir", "-p", f"{remote_root}/thumb")
            push_files(
                adb,
                args.serial,
                sorted((source / "data").glob("*.zip")),
                f"{remote_root}/data",
            )
            push_files(
                adb,
                args.serial,
                sorted((source / "thumb").glob("*.png")),
                f"{remote_root}/thumb",
            )
            actual_archives = remote_file_count(adb, args.serial, f"{remote_root}/data", "*.zip")
            actual_thumbnails = remote_file_count(
                adb,
                args.serial,
                f"{remote_root}/thumb",
                "*.png",
            )
            if (actual_archives, actual_thumbnails) != (
                expected_archives,
                expected_thumbnails,
            ):
                raise RuntimeError(
                    "Remote count mismatch: "
                    f"archives={actual_archives}/{expected_archives}, "
                    f"thumbnails={actual_thumbnails}/{expected_thumbnails}"
                )
        print(f"Local catalog synced to {remote_root}")
        return 0
    except (FileNotFoundError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
