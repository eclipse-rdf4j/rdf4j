#!/usr/bin/env python3
"""Summarize plan-matrix records and emit regressions/winner report."""

from __future__ import annotations

import argparse
import csv
import json
from collections import defaultdict
from pathlib import Path
from statistics import median


def parse_args() -> argparse.Namespace:
	parser = argparse.ArgumentParser(description="Analyze plan-matrix records")
	parser.add_argument("--records", type=Path, required=True, help="Path to records.jsonl")
	parser.add_argument("--out-dir", type=Path, required=True, help="Output artifact directory")
	parser.add_argument("--regression-threshold", type=float, default=20.0,
		help="Slowdown percentage threshold (default: 20)")
	return parser.parse_args()


def load_records(path: Path) -> list[dict]:
	records = []
	with path.open("r", encoding="utf-8") as handle:
		for line in handle:
			line = line.strip()
			if not line:
				continue
			records.append(json.loads(line))
	return records


def reason_for(row: dict, slowdown_pct: float) -> str:
	uncertainty = float(row.get("selectedTotalUncertainty") or 0.0)
	estimated = row.get("selectedEstimatedCost")
	estimated = float(estimated) if estimated is not None else None
	runtime = float(row.get("runtimeMedianMs") or 0.0)
	if estimated is None or estimated <= 0.0:
		return "missing_estimate"
	if uncertainty >= 1.0:
		return "high_uncertainty"
	if runtime > (estimated * 2.0):
		return "estimate_error"
	if slowdown_pct > 0:
		return "candidate_underperformed"
	return "faster_than_legacy"


def aggregate(records: list[dict]) -> tuple[list[dict], dict]:
	by_key = defaultdict(dict)
	for row in records:
		key = (row["queryLabel"], row["mode"], int(row.get("seed", -1)))
		by_key[key][row["variant"]] = row

	regressions = []
	stats = {
		"total_comparisons": 0,
		"faster_or_equal": 0,
		"slower": 0,
		"winners": defaultdict(int),
	}

	for key, variants in sorted(by_key.items()):
		legacy = variants.get("LEGACY")
		if not legacy:
			continue
		legacy_runtime = float(legacy["runtimeMedianMs"])
		if legacy_runtime <= 0:
			continue
		winner = min(variants.values(), key=lambda item: float(item["runtimeMedianMs"]))
		stats["winners"][winner["variant"]] += 1

		for variant_name in ("DP_TOP1", "DP_TOP2", "DP_TOP3"):
			row = variants.get(variant_name)
			if not row:
				continue
			stats["total_comparisons"] += 1
			runtime = float(row["runtimeMedianMs"])
			slowdown_pct = ((runtime - legacy_runtime) / legacy_runtime) * 100.0
			if slowdown_pct <= 0.0:
				stats["faster_or_equal"] += 1
			else:
				stats["slower"] += 1
			regressions.append({
				"queryLabel": key[0],
				"mode": key[1],
				"seed": key[2],
				"variant": variant_name,
				"runtimeMedianMs": runtime,
				"legacyMedianMs": legacy_runtime,
				"slowdownPct": slowdown_pct,
				"reason": reason_for(row, slowdown_pct),
				"selectedPlanSignature": row.get("selectedPlanSignature", ""),
			})

	return regressions, stats


def write_regressions(rows: list[dict], threshold: float, out_path: Path) -> list[dict]:
	filtered = [row for row in rows if row["slowdownPct"] > threshold]
	with out_path.open("w", encoding="utf-8", newline="") as handle:
		writer = csv.DictWriter(handle, fieldnames=[
			"queryLabel",
			"mode",
			"seed",
			"variant",
			"runtimeMedianMs",
			"legacyMedianMs",
			"slowdownPct",
			"reason",
			"selectedPlanSignature",
		])
		writer.writeheader()
		for row in sorted(filtered, key=lambda item: item["slowdownPct"], reverse=True):
			writer.writerow(row)
	return filtered


def write_summary(stats: dict, all_rows: list[dict], regressions: list[dict], out_path: Path, threshold: float) -> None:
	geomean_ready = [row for row in all_rows if row["legacyMedianMs"] > 0 and row["runtimeMedianMs"] > 0]
	ratios = [row["legacyMedianMs"] / row["runtimeMedianMs"] for row in geomean_ready]
	geomean = 0.0
	if ratios:
		product = 1.0
		for ratio in ratios:
			product *= ratio
		geomean = product ** (1.0 / len(ratios))

	lines = [
		"# Plan Matrix Summary",
		"",
		f"- Total comparisons: {stats['total_comparisons']}",
		f"- Faster or equal vs legacy: {stats['faster_or_equal']}",
		f"- Slower vs legacy: {stats['slower']}",
		f"- Geomean speedup (legacy/runtime): {geomean:.4f}x",
		f"- Regressions above {threshold:.1f}%: {len(regressions)}",
		"",
		"## Winner Counts",
	]
	for variant, count in sorted(stats["winners"].items()):
		lines.append(f"- {variant}: {count}")

	lines.append("")
	lines.append("## Worst Regressions")
	for row in sorted(regressions, key=lambda item: item["slowdownPct"], reverse=True)[:20]:
		lines.append(
			f"- {row['queryLabel']} ({row['mode']} {row['variant']} seed={row['seed']}): "
			f"{row['slowdownPct']:.2f}% ({row['reason']})"
		)

	out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
	args = parse_args()
	records = load_records(args.records)
	regression_rows, stats = aggregate(records)
	args.out_dir.mkdir(parents=True, exist_ok=True)
	regressions = write_regressions(regression_rows, args.regression_threshold, args.out_dir / "regressions.csv")
	write_summary(stats, regression_rows, regressions, args.out_dir / "summary.md", args.regression_threshold)
	print(f"Wrote {args.out_dir / 'summary.md'}")
	print(f"Wrote {args.out_dir / 'regressions.csv'}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
