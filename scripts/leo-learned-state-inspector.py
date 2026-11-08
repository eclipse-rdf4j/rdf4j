#!/usr/bin/env python3
"""Summarize LMDB learned-optimizer export directories.

The Java API writes a small text summary plus binary sidecars. This helper intentionally does not parse binary sidecars;
it gives operators a safe first look at an export without coupling the script to the sidecar wire format.
"""
from __future__ import annotations

import argparse
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspect an exported LMDB LEO learned-optimizer directory")
    parser.add_argument("directory", type=Path, help="directory produced by exportLearnedOptimizerFeedback")
    args = parser.parse_args()
    directory = args.directory
    if not directory.is_dir():
        raise SystemExit(f"not a directory: {directory}")

    summary = directory / "learned-evidence-summary.txt"
    if summary.is_file():
        print(summary.read_text(encoding="utf-8", errors="replace").strip())
    else:
        print("learned-evidence-summary.txt: missing")

    for path in sorted(directory.iterdir()):
        if path.name == "learned-evidence-summary.txt":
            continue
        if path.is_file():
            print(f"{path.name}: {path.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
