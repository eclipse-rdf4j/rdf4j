---
name: jmh-benchmark-compare
description: Parse JMH result text by finding the first header line that starts with Benchmark and contains Mode and Score, build a structured table for all columns/rows, compare overlapping benchmarks across 2+ files, compute Diff Score and Diff %, filter by deviation or regression thresholds, analyze regressions over time from filename/mtime timestamps, and export sortable reports to txt/md/csv/xlsx/html. Use for benchmark run comparisons, regression triage, and directory-wide historical analysis.
---

# jmh-benchmark-compare

Use this skill when benchmark output comparison must be reproducible, sortable, and exportable.

## Quick start

Run two-file comparison:

```bash
python3 .codex/skills/jmh-benchmark-compare/scripts/jmh_benchmark_compare.py \
  /path/run-a.txt /path/run-b.txt \
  --export-formats txt,md,csv,xlsx,html \
  --output-dir /tmp \
  --output-base jmh-compare
```

Sort by diff percent (descending):

```bash
python3 .codex/skills/jmh-benchmark-compare/scripts/jmh_benchmark_compare.py \
  run-a.txt run-b.txt \
  --sort-column "Diff % [run-b - run-a]" \
  --sort-desc \
  --export-formats md \
  --output /tmp/jmh-diff.md
```

## Core behavior

1. Detect first JMH table header line:
   `line.startswith("Benchmark") and "Mode" in line and "Score" in line`.
2. Derive column boundaries from that header.
3. Parse all following benchmark rows into an internal table.
4. Match overlapping benchmark keys across files.
5. Add derived columns:
   `Diff Score [target - baseline]`, `Diff % [target - baseline]`, `Status [...]`.

Default key columns are all columns except `Cnt`, `Score`, `Error`. Override via `--id-columns`.

## Inputs and overlap

- Pass any mix of files and directories.
- Directory entries are scanned for files that contain a JMH header.
- `--overlap-mode all` keeps only rows present in all files.
- `--overlap-mode any` keeps rows present in at least two files.
- Baseline selection: `--baseline <index-or-label>`.

## Filters and regression shortcuts

- Hide tiny deltas:
  `--min-deviation-pct 1.0`
- Show only regressions above threshold:
  `--regressions-over-pct 3.0`
- Control direction interpretation:
  `--score-direction auto|higher|lower`

## Historical analysis

Analyze trends across many runs:

```bash
python3 .codex/skills/jmh-benchmark-compare/scripts/jmh_benchmark_compare.py \
  /path/bench-history \
  --recursive \
  --glob "*.txt" \
  --timestamp-source auto \
  --analyze-over-time \
  --regressions-over-pct 2.5 \
  --export-formats html,csv \
  --output-dir /tmp \
  --output-base jmh-history
```

Timeline report files are emitted with `-timeline` suffix.

## Exports

- `txt`: aligned plain-text table.
- `md`: valid markdown table.
- `csv`: spreadsheet-friendly CSV.
- `xlsx`: native Excel workbook (single sheet). (`xslx` alias accepted)
- `html`: sortable table (click header), built-in CSS + JS, color theme selector.

If one format and explicit destination needed, use `--output /path/file.ext`.
If multiple formats, use `--output-dir` + `--output-base`.

## Script

`scripts/jmh_benchmark_compare.py`

For timestamp parsing behavior and filename examples, see:
`references/timestamps-and-discovery.md`
