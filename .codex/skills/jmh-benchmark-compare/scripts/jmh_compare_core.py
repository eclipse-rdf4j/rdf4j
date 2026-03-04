#!/usr/bin/env python3
"""Core parse/compare logic for JMH benchmark tables."""

from __future__ import annotations

import datetime as dt
import itertools
import math
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

HEADER_RE = re.compile(r"^Benchmark\b.*\bMode\b.*\bScore\b")
NUM_RE = re.compile(
    r"[-+]?(?:\d+(?:,\d{3})*(?:\.\d+)?|\.\d+)(?:[eE][-+]?\d+)?|[-+]?(?:inf|nan)",
    re.IGNORECASE,
)
METRIC_COLUMNS = {"Score", "Error", "Cnt"}
DATE_PATTERNS = (
    re.compile(
        r"(20\d{2})[-_]?([01]\d)[-_]?([0-3]\d)[Tt _-]?([0-2]\d)[-_:]?([0-5]\d)(?:[-_:]?([0-5]\d))?"
    ),
    re.compile(r"(20\d{2})[-_]?([01]\d)[-_]?([0-3]\d)"),
)


@dataclass
class ParsedFile:
    path: Path
    label: str
    timestamp: dt.datetime
    columns: List[str]
    rows: List[Dict[str, str]]
    key_columns: List[str]
    score_by_key: Dict[Tuple[str, ...], float]
    row_by_key: Dict[Tuple[str, ...], Dict[str, str]]


@dataclass
class TableData:
    columns: List[str]
    rows: List[Dict[str, object]]
    numeric_columns: set
    percent_columns: set


def extract_numeric(text: str) -> Optional[float]:
    match = NUM_RE.search(text or "")
    if not match:
        return None
    raw = match.group(0).replace(",", "")
    try:
        return float(raw)
    except ValueError:
        return None


def looks_like_jmh_output(path: Path, max_bytes: int = 1_000_000) -> bool:
    try:
        text = path.read_text(encoding="utf-8", errors="replace")[:max_bytes]
    except OSError:
        return False
    return any(HEADER_RE.search(line) for line in text.splitlines())


def discover_files(inputs: Sequence[str], recursive: bool, glob_pattern: str) -> List[Path]:
    discovered: List[Path] = []
    for item in inputs:
        path = Path(item).expanduser().resolve()
        if not path.exists():
            raise FileNotFoundError(f"Input does not exist: {path}")
        if path.is_file():
            discovered.append(path)
            continue
        walker = path.rglob(glob_pattern) if recursive or path.is_dir() else path.glob(glob_pattern)
        for candidate in walker:
            if candidate.is_file() and looks_like_jmh_output(candidate):
                discovered.append(candidate.resolve())
    deduped = []
    seen = set()
    for path in sorted(discovered):
        if path not in seen:
            deduped.append(path)
            seen.add(path)
    return deduped


def find_header(lines: List[str]) -> Tuple[int, str]:
    for idx, line in enumerate(lines):
        if line.startswith("Benchmark") and "Mode" in line and "Score" in line:
            return idx, line
    raise ValueError("No JMH header found (need line starting with 'Benchmark' containing 'Mode' and 'Score').")


def column_specs(header_line: str) -> List[Tuple[str, int, Optional[int]]]:
    spans = [(m.group(0), m.start()) for m in re.finditer(r"\S+", header_line)]
    specs = []
    for i, (name, start) in enumerate(spans):
        end = spans[i + 1][1] if i + 1 < len(spans) else None
        specs.append((name, start, end))
    return specs


def parse_row(line: str, specs: Sequence[Tuple[str, int, Optional[int]]]) -> Dict[str, str]:
    parsed: Dict[str, str] = {}
    for name, start, end in specs:
        if start >= len(line):
            parsed[name] = ""
            continue
        parsed[name] = line[start:end].strip() if end is not None else line[start:].strip()
    return parsed


def parse_row_split(line: str, columns: Sequence[str]) -> Optional[Dict[str, str]]:
    parts = re.split(r"\s{2,}", line.strip())
    if len(parts) != len(columns):
        return None
    return {col: part.strip() for col, part in zip(columns, parts)}


def is_int_token(text: str) -> bool:
    return bool(re.fullmatch(r"[+-]?\d+", text or ""))


def has_valid_metric_values(row: Dict[str, str], columns: Sequence[str]) -> bool:
    for col in columns:
        value = row.get(col, "")
        if col == "Score":
            if extract_numeric(value) is None:
                return False
        elif col == "Cnt" and value:
            if not is_int_token(value):
                return False
        elif col == "Error" and value:
            if extract_numeric(value) is None:
                return False
    return True


def map_parts_to_columns(parts: Sequence[str], columns: Sequence[str]) -> Optional[Dict[str, str]]:
    if len(parts) < len(columns):
        return None
    overflow = len(parts) - len(columns)
    row = {columns[0]: " ".join(parts[: overflow + 1])}
    for idx, col in enumerate(columns[1:], start=1):
        row[col] = parts[overflow + idx]
    return row


def parse_row_with_optional_metrics(parts: Sequence[str], columns: Sequence[str]) -> Optional[Dict[str, str]]:
    optional_positions = [idx for idx, col in enumerate(columns) if col in {"Cnt", "Error"}]
    for drop_count in range(0, len(optional_positions) + 1):
        for drop_positions in itertools.combinations(optional_positions, drop_count):
            reduced_columns = [col for idx, col in enumerate(columns) if idx not in drop_positions]
            row = map_parts_to_columns(parts, reduced_columns)
            if row is None or not has_valid_metric_values(row, reduced_columns):
                continue
            full_row = {col: "" for col in columns}
            full_row.update(row)
            return full_row

    return None


def parse_row_tokens(line: str, columns: Sequence[str]) -> Optional[Dict[str, str]]:
    parts = [part for part in line.strip().split() if part != "±"]
    return parse_row_with_optional_metrics(parts, columns)


def parse_timestamp_from_filename(name: str) -> Optional[dt.datetime]:
    for regex in DATE_PATTERNS:
        match = regex.search(name)
        if not match:
            continue
        try:
            parts = [int(x) for x in match.groups() if x is not None]
            if len(parts) >= 5:
                year, month, day, hour, minute = parts[:5]
                second = parts[5] if len(parts) > 5 else 0
                return dt.datetime(year, month, day, hour, minute, second, tzinfo=dt.timezone.utc)
            year, month, day = parts[:3]
            return dt.datetime(year, month, day, tzinfo=dt.timezone.utc)
        except ValueError:
            continue
    return None


def parse_timestamp(path: Path, source: str) -> dt.datetime:
    if source in ("auto", "filename"):
        candidate = parse_timestamp_from_filename(path.name)
        if candidate is not None:
            return candidate
        if source == "filename":
            raise ValueError(f"No parseable timestamp in filename: {path.name}")
    return dt.datetime.fromtimestamp(path.stat().st_mtime, tz=dt.timezone.utc)


def derive_label(path: Path, seen: Dict[str, int]) -> str:
    base = path.stem
    seen[base] = seen.get(base, 0) + 1
    return base if seen[base] == 1 else f"{base}#{seen[base]}"


def derive_key_columns(columns: List[str], id_columns: Optional[str]) -> List[str]:
    if id_columns:
        wanted = [c.strip() for c in id_columns.split(",") if c.strip()]
        missing = [c for c in wanted if c not in columns]
        if missing:
            raise ValueError(f"Unknown id columns: {', '.join(missing)}")
        return wanted
    keys = [c for c in columns if c not in METRIC_COLUMNS]
    if "Benchmark" in columns and "Benchmark" not in keys:
        keys.insert(0, "Benchmark")
    return keys


def parse_file(path: Path, label: str, id_columns: Optional[str], timestamp_source: str) -> ParsedFile:
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    header_idx, header_line = find_header(lines)
    specs = column_specs(header_line)
    columns = [name for name, _, _ in specs]
    key_columns = derive_key_columns(columns, id_columns)
    rows: List[Dict[str, str]] = []

    saw_data = False
    for line in lines[header_idx + 1 :]:
        stripped = line.strip()
        if not stripped:
            if saw_data:
                break
            continue
        if stripped.startswith("#"):
            continue
        row = parse_row(line, specs)
        split_row = parse_row_split(line, columns)
        if split_row is not None and has_valid_metric_values(split_row, columns):
            row = split_row
        else:
            token_row = parse_row_tokens(line, columns)
            if token_row is not None and has_valid_metric_values(token_row, columns):
                row = token_row
        if not row.get("Benchmark"):
            if saw_data:
                break
            continue
        score = extract_numeric(row.get("Score", ""))
        if score is None:
            if saw_data and (stripped.startswith("Result") or stripped.startswith("Secondary result")):
                break
            continue
        rows.append(row)
        saw_data = True

    if not rows:
        raise ValueError(f"No benchmark rows parsed from {path}")

    score_by_key: Dict[Tuple[str, ...], float] = {}
    row_by_key: Dict[Tuple[str, ...], Dict[str, str]] = {}
    for row in rows:
        key = tuple(row.get(col, "") for col in key_columns)
        score = extract_numeric(row.get("Score", ""))
        if score is None:
            continue
        score_by_key[key] = score
        row_by_key[key] = row

    return ParsedFile(
        path=path,
        label=label,
        timestamp=parse_timestamp(path, timestamp_source),
        columns=columns,
        rows=rows,
        key_columns=key_columns,
        score_by_key=score_by_key,
        row_by_key=row_by_key,
    )


def resolve_baseline(files: List[ParsedFile], baseline: str) -> ParsedFile:
    try:
        idx = int(baseline)
        if idx < 0 or idx >= len(files):
            raise ValueError
        return files[idx]
    except ValueError:
        for file_entry in files:
            if file_entry.label == baseline:
                return file_entry
        labels = ", ".join(f.label for f in files)
        raise ValueError(f"Unknown baseline '{baseline}'. Valid index 0..{len(files)-1} or labels: {labels}")


def score_direction(mode: str, units: str, direction_flag: str) -> str:
    if direction_flag in {"higher", "lower"}:
        return direction_flag
    m = (mode or "").lower()
    u = (units or "").lower()
    if m == "thrpt" or "op/s" in u or "ops/s" in u:
        return "higher"
    if "/op" in u or "s/op" in u or m in {"avgt", "sample", "ss"}:
        return "lower"
    return "lower"


def classify_pct(diff_pct: Optional[float], direction: str, threshold: float = 0.0) -> str:
    if diff_pct is None:
        return "n/a"
    if direction == "higher":
        if diff_pct <= -threshold:
            return "regression"
        if diff_pct >= threshold:
            return "improvement"
    else:
        if diff_pct >= threshold:
            return "regression"
        if diff_pct <= -threshold:
            return "improvement"
    return "neutral"


def build_overlap_keys(files: List[ParsedFile], overlap_mode: str) -> List[Tuple[str, ...]]:
    key_sets = [set(f.score_by_key.keys()) for f in files]
    if overlap_mode == "all":
        keys = set.intersection(*key_sets)
    else:
        counts: Dict[Tuple[str, ...], int] = {}
        for keys_set in key_sets:
            for key in keys_set:
                counts[key] = counts.get(key, 0) + 1
        keys = {key for key, count in counts.items() if count >= 2}
    return sorted(keys)


def build_overlap_keys_from_maps(
    score_maps: Sequence[Dict[Tuple[str, ...], float]], overlap_mode: str
) -> List[Tuple[str, ...]]:
    key_sets = [set(score_map.keys()) for score_map in score_maps]
    if overlap_mode == "all":
        keys = set.intersection(*key_sets)
    else:
        counts: Dict[Tuple[str, ...], int] = {}
        for keys_set in key_sets:
            for key in keys_set:
                counts[key] = counts.get(key, 0) + 1
        keys = {key for key, count in counts.items() if count >= 2}
    return sorted(keys)


def derive_comparison_key_columns(files: List[ParsedFile], preferred_order: Optional[Sequence[str]] = None) -> List[str]:
    preferred = list(preferred_order) if preferred_order else files[0].key_columns
    preferred = [col for col in preferred if col not in METRIC_COLUMNS]
    shared = [col for col in preferred if all(col in file_entry.columns for file_entry in files)]
    if shared:
        return shared

    fallback = [col for col in files[0].columns if col not in METRIC_COLUMNS]
    shared_fallback = [col for col in fallback if all(col in file_entry.columns for file_entry in files)]
    if shared_fallback:
        return shared_fallback

    raise ValueError("No common key columns found across files.")


def rekey_file(file_entry: ParsedFile, key_columns: Sequence[str]) -> Tuple[Dict[Tuple[str, ...], float], Dict[Tuple[str, ...], Dict[str, str]]]:
    score_by_key: Dict[Tuple[str, ...], float] = {}
    row_by_key: Dict[Tuple[str, ...], Dict[str, str]] = {}
    for row in file_entry.rows:
        key = tuple(row.get(col, "") for col in key_columns)
        score = extract_numeric(row.get("Score", ""))
        if score is None:
            continue
        score_by_key[key] = score
        row_by_key[key] = row
    return score_by_key, row_by_key


def build_comparison_table(
    files: List[ParsedFile],
    baseline: ParsedFile,
    overlap_mode: str,
    min_deviation_pct: float,
    regressions_over_pct: Optional[float],
    direction_flag: str,
) -> TableData:
    key_columns = derive_comparison_key_columns(files, baseline.key_columns)
    score_columns = [f"Score [{f.label}]" for f in files]
    compare_targets = [f for f in files if f.label != baseline.label]
    diff_columns = [f"Diff Score [{t.label} - {baseline.label}]" for t in compare_targets]
    pct_columns = [f"Diff % [{t.label} - {baseline.label}]" for t in compare_targets]
    status_columns = [f"Status [{t.label} vs {baseline.label}]" for t in compare_targets]
    columns = key_columns + score_columns + diff_columns + pct_columns + status_columns
    rows: List[Dict[str, object]] = []
    score_maps: Dict[str, Dict[Tuple[str, ...], float]] = {}
    row_maps: Dict[str, Dict[Tuple[str, ...], Dict[str, str]]] = {}
    for file_entry in files:
        score_maps[file_entry.label], row_maps[file_entry.label] = rekey_file(file_entry, key_columns)
    overlap_keys = build_overlap_keys_from_maps([score_maps[file_entry.label] for file_entry in files], overlap_mode)

    for key in overlap_keys:
        row: Dict[str, object] = {}
        status_by_column: Dict[str, str] = {}
        for idx, col in enumerate(key_columns):
            row[col] = key[idx]
        for file_entry, score_col in zip(files, score_columns):
            row[score_col] = score_maps[file_entry.label].get(key)

        base_score = score_maps[baseline.label].get(key)
        base_row = row_maps[baseline.label].get(key, {})
        direction = score_direction(base_row.get("Mode", ""), base_row.get("Units", ""), direction_flag)
        max_abs_pct = 0.0
        has_regression_over = False

        for target, diff_col, pct_col, status_col in zip(compare_targets, diff_columns, pct_columns, status_columns):
            target_score = score_maps[target.label].get(key)
            if base_score is None or target_score is None:
                diff = None
                pct = None
            else:
                diff = target_score - base_score
                pct = None if base_score == 0 else (diff / base_score) * 100.0
            status = classify_pct(pct, direction, 0.0)
            row[diff_col] = diff
            row[pct_col] = pct
            row[status_col] = status
            status_by_column[diff_col] = status
            status_by_column[pct_col] = status
            if pct is not None:
                max_abs_pct = max(max_abs_pct, abs(pct))
                if regressions_over_pct is not None:
                    has_regression_over = has_regression_over or (
                        classify_pct(pct, direction, regressions_over_pct) == "regression"
                    )

        row["__max_abs_pct"] = max_abs_pct
        row["__has_regression_over"] = has_regression_over
        row["__status_by_column"] = status_by_column
        rows.append(row)

    if min_deviation_pct > 0:
        rows = [r for r in rows if float(r.get("__max_abs_pct", 0.0)) >= min_deviation_pct]
    if regressions_over_pct is not None:
        rows = [r for r in rows if bool(r.get("__has_regression_over", False))]

    numeric_columns = set(score_columns + diff_columns + pct_columns)
    percent_columns = set(pct_columns)
    return TableData(columns=columns, rows=rows, numeric_columns=numeric_columns, percent_columns=percent_columns)


def sort_table(table: TableData, sort_column: Optional[str], descending: bool) -> None:
    if not sort_column:
        return
    if sort_column not in table.columns:
        available = ", ".join(table.columns)
        raise ValueError(f"Sort column '{sort_column}' not found. Available: {available}")

    def sort_key(row: Dict[str, object]) -> Tuple[int, object]:
        value = row.get(sort_column)
        if value is None or value == "":
            return (1, "")
        if isinstance(value, (int, float)):
            return (0, float(value))
        return (0, str(value).lower())

    table.rows.sort(key=sort_key, reverse=descending)


def format_value(column: str, value: object, percent_columns: set) -> str:
    if value is None:
        return ""
    if isinstance(value, float):
        if math.isnan(value):
            return "NaN"
        if math.isinf(value):
            return "Inf" if value > 0 else "-Inf"
        if column in percent_columns:
            return f"{value:.3f}%"
        return f"{value:.9g}"
    return str(value)


def build_timeline_table(
    files: List[ParsedFile],
    key_columns: List[str],
    overlap_mode: str,
    direction_flag: str,
    regression_threshold: float,
) -> TableData:
    ordered = sorted(files, key=lambda f: (f.timestamp, f.label))
    key_columns = derive_comparison_key_columns(ordered, key_columns)
    score_maps: Dict[str, Dict[Tuple[str, ...], float]] = {}
    row_maps: Dict[str, Dict[Tuple[str, ...], Dict[str, str]]] = {}
    for file_entry in ordered:
        score_maps[file_entry.label], row_maps[file_entry.label] = rekey_file(file_entry, key_columns)
    keys = build_overlap_keys_from_maps([score_maps[file_entry.label] for file_entry in ordered], overlap_mode)
    threshold_col = f"Regressions > {regression_threshold:.3f}%"
    columns = key_columns + [
        "First Timestamp",
        "Last Timestamp",
        "Data Points",
        "First Score",
        "Latest Score",
        "Total Change %",
        threshold_col,
        "Worst Step Regression %",
        "Best Step Improvement %",
    ]
    rows: List[Dict[str, object]] = []
    numeric = {
        "Data Points",
        "First Score",
        "Latest Score",
        "Total Change %",
        threshold_col,
        "Worst Step Regression %",
        "Best Step Improvement %",
    }
    percent = {"Total Change %", "Worst Step Regression %", "Best Step Improvement %"}

    for key in keys:
        series = []
        for file_entry in ordered:
            score = score_maps[file_entry.label].get(key)
            if score is None:
                continue
            row = row_maps[file_entry.label].get(key, {})
            direction = score_direction(row.get("Mode", ""), row.get("Units", ""), direction_flag)
            series.append((file_entry.timestamp, score, direction))
        if len(series) < 2:
            continue

        first_ts, first_score, direction = series[0]
        last_ts, last_score, _ = series[-1]
        total_pct = None if first_score == 0 else ((last_score - first_score) / first_score) * 100.0
        regressions = 0
        worst_reg = 0.0
        best_imp = 0.0
        for (_, prev, _), (_, curr, _) in zip(series, series[1:]):
            if prev == 0:
                continue
            pct = ((curr - prev) / prev) * 100.0
            if classify_pct(pct, direction, regression_threshold) == "regression":
                regressions += 1
                worst_reg = max(worst_reg, abs(pct))
            if classify_pct(pct, direction, 0.0) == "improvement":
                best_imp = max(best_imp, abs(pct))

        row: Dict[str, object] = {}
        for idx, col in enumerate(key_columns):
            row[col] = key[idx]
        row["First Timestamp"] = first_ts.isoformat()
        row["Last Timestamp"] = last_ts.isoformat()
        row["Data Points"] = float(len(series))
        row["First Score"] = first_score
        row["Latest Score"] = last_score
        row["Total Change %"] = total_pct
        row[threshold_col] = float(regressions)
        row["Worst Step Regression %"] = worst_reg
        row["Best Step Improvement %"] = best_imp
        rows.append(row)

    return TableData(columns=columns, rows=rows, numeric_columns=numeric, percent_columns=percent)
