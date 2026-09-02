#!/usr/bin/env python3
"""CLI wrapper for JMH benchmark compare skill."""

from __future__ import annotations

import argparse
import sys

import jmh_compare_core as core
from jmh_compare_export import output_targets, write_table


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare JMH benchmark result tables from files or directories.",
    )
    parser.add_argument("inputs", nargs="+", help="Input files and/or directories")
    parser.add_argument(
        "--recursive",
        action="store_true",
        help="Recursively scan directories (default for directories)",
    )
    parser.add_argument("--glob", default="*", help="Glob pattern for directory scan")
    parser.add_argument(
        "--id-columns",
        help="Comma-separated key columns (default: all except Cnt/Score/Error)",
    )
    parser.add_argument(
        "--overlap-mode",
        choices=("all", "any"),
        default="all",
        help="'all': present in all files, 'any': present in >=2 files",
    )
    parser.add_argument("--baseline", default="0", help="Baseline index or label")
    parser.add_argument(
        "--score-direction",
        choices=("auto", "higher", "lower"),
        default="auto",
        help="Interpret score direction for regression labels",
    )
    parser.add_argument("--sort-column", help="Sort output by this column name")
    parser.add_argument("--sort-desc", action="store_true", help="Sort descending")
    parser.add_argument(
        "--min-deviation-pct",
        type=float,
        default=0.0,
        help="Hide rows whose max abs diff %% is below threshold",
    )
    parser.add_argument(
        "--regressions-over-pct",
        type=float,
        default=None,
        help="Show only rows with regression over threshold %% in any diff column",
    )
    parser.add_argument(
        "--analyze-over-time",
        action="store_true",
        help="Generate extra trend summary across timestamps",
    )
    parser.add_argument(
        "--timestamp-source",
        choices=("auto", "filename", "mtime"),
        default="auto",
        help="Timestamp source for ordering/trend analysis",
    )
    parser.add_argument(
        "--export-formats",
        default="txt",
        help="Comma-separated: txt,md,csv,xlsx,html",
    )
    parser.add_argument("--output", help="Single explicit output file path")
    parser.add_argument("--output-dir", default=".", help="Output directory for multi export")
    parser.add_argument("--output-base", default="jmh-benchmark-compare", help="Output basename")
    return parser.parse_args()


def parse_formats(raw: str) -> list[str]:
    formats = [f.strip().lower() for f in raw.split(",") if f.strip()]
    formats = ["xlsx" if f == "xslx" else f for f in formats]
    supported = {"txt", "md", "csv", "xlsx", "html"}
    unknown = [f for f in formats if f not in supported]
    if unknown:
        raise ValueError(f"Unsupported formats: {', '.join(unknown)}")
    return formats


def build_parsed_files(args: argparse.Namespace) -> list[core.ParsedFile]:
    files = core.discover_files(args.inputs, args.recursive, args.glob)
    if len(files) < 2:
        raise ValueError("Need at least 2 JMH result files after discovery.")
    labels_seen: dict[str, int] = {}
    return [
        core.parse_file(path, core.derive_label(path, labels_seen), args.id_columns, args.timestamp_source)
        for path in files
    ]


def write_exports(table: core.TableData, args: argparse.Namespace, formats: list[str], suffix: str = "") -> None:
    targets = output_targets(args.output if not suffix else None, args.output_dir, args.output_base, formats, suffix)
    for fmt, path in targets.items():
        write_table(table, fmt, path)
        print(f"[wrote] {path}")


def main() -> int:
    args = parse_args()
    formats = parse_formats(args.export_formats)
    parsed = build_parsed_files(args)
    baseline = core.resolve_baseline(parsed, args.baseline)

    comparison = core.build_comparison_table(
        parsed,
        baseline,
        args.overlap_mode,
        args.min_deviation_pct,
        args.regressions_over_pct,
        args.score_direction,
    )
    core.sort_table(comparison, args.sort_column, args.sort_desc)
    write_exports(comparison, args, formats)

    if args.analyze_over_time:
        threshold = args.regressions_over_pct if args.regressions_over_pct is not None else 0.0
        timeline = core.build_timeline_table(
            parsed,
            baseline.key_columns,
            args.overlap_mode,
            args.score_direction,
            threshold,
        )
        if args.sort_column and args.sort_column in timeline.columns:
            core.sort_table(timeline, args.sort_column, args.sort_desc)
        write_exports(timeline, args, formats, suffix="-timeline")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        print(f"[error] {exc}", file=sys.stderr)
        raise SystemExit(1)
