#!/usr/bin/env python3
"""Summarise LEO shadow benchmark logs containing planned/actual metric lines.

This deliberately accepts loose text logs. It scans for q-error-like and learned-evidence fields emitted by explain
plans or telemetry, and reports a compact median/p95/max summary when present.
"""
from __future__ import annotations

import math
import re
import statistics
import sys
from pathlib import Path

PATTERNS = {
    "leo_confidence": re.compile(r"plannedLeoEvidenceConfidence[=:]\s*([0-9.]+)"),
    "leo_rows": re.compile(r"plannedLeoEvidenceRows[=:]\s*([0-9.]+)"),
    "actual_rows": re.compile(r"(?:actualRows|OUTPUT_ROWS_ACTUAL)[=:]\s*([0-9.]+)"),
    "work_qerror": re.compile(r"plannedLeoEvidenceWorkQErrorMax[=:]\s*([0-9.]+)"),
    "row_qerror": re.compile(r"plannedLeoEvidenceRowQErrorMax[=:]\s*([0-9.]+)"),
}


def percentile(values: list[float], pct: float) -> float:
    if not values:
        return float("nan")
    values = sorted(values)
    idx = min(len(values) - 1, max(0, math.ceil((pct / 100.0) * len(values)) - 1))
    return values[idx]


def scan(path: Path) -> dict[str, list[float]]:
    values = {key: [] for key in PATTERNS}
    for line in path.read_text(errors="replace").splitlines():
        for key, pattern in PATTERNS.items():
            match = pattern.search(line)
            if match:
                try:
                    values[key].append(float(match.group(1)))
                except ValueError:
                    pass
    return values


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print("usage: leo-shadow-benchmark-summary.py <log> [<log> ...]", file=sys.stderr)
        return 2
    merged = {key: [] for key in PATTERNS}
    for arg in argv[1:]:
        for key, vals in scan(Path(arg)).items():
            merged[key].extend(vals)
    for key, vals in merged.items():
        if not vals:
            print(f"{key}: count=0")
            continue
        print(
            f"{key}: count={len(vals)} median={statistics.median(vals):.4g} "
            f"p95={percentile(vals, 95):.4g} max={max(vals):.4g}"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
