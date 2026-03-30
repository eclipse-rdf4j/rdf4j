#!/usr/bin/env python3
"""Summarize likely query-plan performance regression/improvement signals."""

from __future__ import annotations

import argparse
import re
from pathlib import Path
from typing import Dict, List, Optional

EXECUTION_LINE = re.compile(
    r"runs=(?P<runs>\d+),\s*"
    r"totalMillis=(?P<total>\d+),\s*"
    r"averageMillis=(?P<avg>\d+),\s*"
    r"resultCount=(?P<results>\d+),\s*"
    r"softLimitMillis=(?P<soft_limit>\d+),\s*"
    r"softLimitReached=(?P<soft_reached>true|false),\s*"
    r"maxRunsReached=(?P<max_reached>true|false)"
)

DIFF_LINE = re.compile(
    r"^\s*(?P<level>unoptimized|optimized|executed):\s+"
    r".*structure=(?P<structure>[^,]+),\s*"
    r"joinAlgorithms=(?P<joins>[^,]+),\s*"
    r"actualResultSizes=(?P<actual>[^,]+),\s*"
    r"estimates=(?P<estimates>[^,\s]+)"
)


def parse_execution_metrics(path: Path) -> Dict[str, int]:
    text = path.read_text(encoding="utf-8", errors="replace")
    matches = list(EXECUTION_LINE.finditer(text))
    if not matches:
        raise ValueError(f"No execution verification line found in {path}")
    last = matches[-1]
    return {
        "runs": int(last.group("runs")),
        "total": int(last.group("total")),
        "avg": int(last.group("avg")),
        "results": int(last.group("results")),
    }


def parse_semantic_diff(path: Optional[Path]) -> List[Dict[str, str]]:
    if path is None:
        return []
    text = path.read_text(encoding="utf-8", errors="replace")
    rows: List[Dict[str, str]] = []
    for line in text.splitlines():
        match = DIFF_LINE.search(line)
        if not match:
            continue
        rows.append(
            {
                "level": match.group("level"),
                "structure": match.group("structure").strip(),
                "joins": match.group("joins").strip(),
                "actual": match.group("actual").strip(),
                "estimates": match.group("estimates").strip(),
            }
        )
    return rows


def runtime_classification(delta_percent: Optional[float]) -> str:
    if delta_percent is None:
        return "unknown"
    if delta_percent <= -10.0:
        return "improvement"
    if delta_percent >= 10.0:
        return "regression"
    return "neutral"


def find_diff(rows: List[Dict[str, str]], key: str) -> bool:
    return any(row[key] == "diff" for row in rows)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-log", required=True, type=Path)
    parser.add_argument("--candidate-log", required=True, type=Path)
    parser.add_argument("--comparison-log", type=Path)
    args = parser.parse_args()

    baseline = parse_execution_metrics(args.baseline_log)
    candidate = parse_execution_metrics(args.candidate_log)
    semantic_rows = parse_semantic_diff(args.comparison_log)

    avg_base = baseline["avg"]
    avg_candidate = candidate["avg"]
    delta_percent: Optional[float]
    if avg_base == 0:
        delta_percent = None
    else:
        delta_percent = ((avg_candidate - avg_base) / avg_base) * 100.0

    runtime_signal = runtime_classification(delta_percent)
    result_count_changed = baseline["results"] != candidate["results"]

    structure_changed = find_diff(semantic_rows, "structure")
    joins_changed = find_diff(semantic_rows, "joins")
    actual_changed = find_diff(semantic_rows, "actual")
    estimates_changed = find_diff(semantic_rows, "estimates")

    if result_count_changed:
        verdict = "semantic regression risk: result count changed; runtime delta not comparable"
    elif runtime_signal == "regression" and (structure_changed or joins_changed or actual_changed):
        verdict = "likely performance regression with plan-shape change"
    elif runtime_signal == "improvement" and (structure_changed or joins_changed):
        verdict = "likely performance improvement with optimizer-plan change"
    elif runtime_signal == "regression":
        verdict = "possible performance regression (no semantic diff evidence provided)"
    elif runtime_signal == "improvement":
        verdict = "possible performance improvement"
    elif structure_changed or joins_changed or actual_changed or estimates_changed:
        verdict = "plan changed but runtime signal neutral"
    else:
        verdict = "no clear regression/improvement signal"

    print("QueryPlanSnapshotCli regression summary")
    print(f"- baseline avgMillis: {avg_base}")
    print(f"- candidate avgMillis: {avg_candidate}")
    if delta_percent is None:
        print("- delta: n/a (baseline averageMillis=0)")
    else:
        print(f"- delta: {delta_percent:+.2f}%")
    print(f"- baseline resultCount: {baseline['results']}")
    print(f"- candidate resultCount: {candidate['results']}")
    print(f"- runtime signal: {runtime_signal}")

    if semantic_rows:
        print("- semantic diff:")
        for row in semantic_rows:
            print(
                "  "
                f"{row['level']}: structure={row['structure']}, "
                f"joinAlgorithms={row['joins']}, "
                f"actualResultSizes={row['actual']}, "
                f"estimates={row['estimates']}"
            )
    else:
        print("- semantic diff: not provided")

    print(f"- verdict: {verdict}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
