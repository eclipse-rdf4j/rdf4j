# Timestamp and discovery notes

## Directory scan rules

- Inputs may be files or directories.
- Directory scan includes files that contain a line matching:
  `^Benchmark\b.*\bMode\b.*\bScore\b`
- Use `--glob` to narrow file candidates.
- Use `--recursive` for recursive walk.

## Timestamp source

`--timestamp-source` controls run ordering:

- `auto`: filename parse first, fallback to file mtime.
- `filename`: require timestamp in filename.
- `mtime`: use file modification time only.

## Filename timestamp patterns

Supported patterns in names:

- `YYYYMMDD`
- `YYYY-MM-DD`
- `YYYYMMDD-HHMM`
- `YYYY-MM-DD_HHMM`
- `YYYYMMDD-HHMMSS`
- `YYYY-MM-DDTHH:MM:SS`

Examples:

- `jmh-20260301.txt`
- `benchmark_2026-03-01_0915.log`
- `run_20260301-091530.txt`
