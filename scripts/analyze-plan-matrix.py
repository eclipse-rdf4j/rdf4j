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
	parser.add_argument("--rca", type=Path, default=None,
		help="Optional path to RCA findings.jsonl for signature split")
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


def load_rca(path: Path | None) -> dict[str, dict[str, str]]:
	if path is None or not path.exists():
		return {}
	by_query: dict[str, dict[str, str]] = defaultdict(dict)
	with path.open("r", encoding="utf-8") as handle:
		for line in handle:
			line = line.strip()
			if not line:
				continue
			row = json.loads(line)
			query_label = f"{row.get('theme', '')}#{row.get('queryIndex', '')}"
			mode = row.get("mode", "")
			stable = row.get("stableSignature", "")
			if query_label and mode:
				by_query[query_label][mode] = stable
	return by_query


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


def regression_signature_split(regressions: list[dict], rca_by_query: dict[str, dict[str, str]]) -> dict[str, int]:
	split = {"same_plan": 0, "different_plan": 0, "unknown": 0}
	for row in regressions:
		query = row["queryLabel"]
		signatures = rca_by_query.get(query)
		if not signatures:
			split["unknown"] += 1
			continue
		dp_sig = signatures.get("DP")
		greedy_sig = signatures.get("Greedy")
		if not dp_sig or not greedy_sig:
			split["unknown"] += 1
			continue
		if dp_sig == greedy_sig:
			split["same_plan"] += 1
		else:
			split["different_plan"] += 1
	return split


def write_summary(stats: dict, all_rows: list[dict], regressions: list[dict], out_path: Path, threshold: float,
		signature_split: dict[str, int] | None = None) -> None:
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

	if signature_split:
		lines.append("")
		lines.append("## Plan Signature Split")
		lines.append(f"- same-plan-but-slower: {signature_split['same_plan']}")
		lines.append(f"- different-plan-slower: {signature_split['different_plan']}")
		lines.append(f"- unknown-signature: {signature_split['unknown']}")

	out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
	args = parse_args()
	records = load_records(args.records)
	rca_by_query = load_rca(args.rca)
	regression_rows, stats = aggregate(records)
	args.out_dir.mkdir(parents=True, exist_ok=True)
	regressions = write_regressions(regression_rows, args.regression_threshold, args.out_dir / "regressions.csv")
	signature_split = regression_signature_split(regressions, rca_by_query) if rca_by_query else None
	write_summary(stats, regression_rows, regressions, args.out_dir / "summary.md", args.regression_threshold,
		signature_split)
	print(f"Wrote {args.out_dir / 'summary.md'}")
	print(f"Wrote {args.out_dir / 'regressions.csv'}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
