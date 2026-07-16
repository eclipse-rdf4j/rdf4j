#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

python3 - "${SCRIPT_DIR}" "$@" <<'PY'
import argparse
import re
import sys
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

DATED_FILE_RE = re.compile(r"results-(\d{4}-\d{2}-\d{2})(?:-(\d+))?\.md$")
QUERY_BENCHMARK = "ThemeQueryBenchmark.executeQuery"
PLAN_RUN_BENCHMARK = "ThemeQueryPlanRunBenchmark.runQuery"
SUMMARY_ROW_RE = re.compile(
    r"^(?P<benchmark>ThemeQuery(?:Benchmark\.executeQuery|PlanRunBenchmark\.runQuery))\s+"
    r"(?:(?:true|false)\s+)?"
    r"(?P<theme>[A-Z_]+)\s+"
    r"(?P<query_index>\d+)\s+"
    r"avgt\s+"
    r"(?:(?:\d+)\s+)?"
    r"(?P<score>[0-9.]+)"
)
PARAM_RE = re.compile(r"^# Parameters: \(.*themeName = ([A-Z_]+), z_queryIndex = (\d+)\)$", re.MULTILINE)
QUERY_MARKER_RE = re.compile(r"^.*### (Optimized|Telemetry) Query ###\s*$", re.MULTILINE)
SPARQL_START_RE = re.compile(r"^(SELECT|ASK|CONSTRUCT|DESCRIBE)\b")
SCORE_LINE_RE = re.compile(r"^(?:Iteration\s+\d+:\s+)?[0-9.]+(?:\s+±\s+[0-9.]+)?\s+ms/op$")
RESULT_LINE_RE = re.compile(r'^Result "')


@dataclass(frozen=True)
class QueryKey:
    theme: str
    query_index: int

    def label(self) -> str:
        return f"{self.theme} q{self.query_index}"


@dataclass(frozen=True)
class QueryContent:
    optimized_plan: Optional[str]
    optimized_query: Optional[str]
    telemetry_plan: Optional[str] = None
    telemetry_query: Optional[str] = None

    @property
    def query_plan(self) -> Optional[str]:
        return self.optimized_plan

    @property
    def has_plan(self) -> bool:
        return bool(self.optimized_plan or self.telemetry_plan)

    @property
    def has_query(self) -> bool:
        return bool(self.optimized_query or self.telemetry_query)

    @property
    def richness(self) -> int:
        return (
            int(bool(self.optimized_plan))
            + int(bool(self.optimized_query))
            + int(bool(self.telemetry_plan))
            + int(bool(self.telemetry_query))
        )

    def has_plan_for(self, plan_kind: str) -> bool:
        if plan_kind == "optimized":
            return bool(self.optimized_plan)
        if plan_kind == "telemetry":
            return bool(self.telemetry_plan)
        return self.has_plan


@dataclass(frozen=True)
class ResultFile:
    path: Path
    benchmark_rows: Dict[str, Dict[QueryKey, float]]
    benchmark_order: Dict[str, List[QueryKey]]
    query_content: Dict[QueryKey, QueryContent]


@dataclass(frozen=True)
class HistoricalMatch:
    file_name: str
    score: float
    percent_faster: float
    content: QueryContent


@dataclass(frozen=True)
class QueryComparison:
    key: QueryKey
    latest_score: float
    fastest_score: float
    previous_best_score: Optional[float]
    direction: str
    delta_percent: float
    baseline_stat: str = "best"
    baseline_count: int = 0


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Analyze theme query benchmark history. Default mode compares the newest dated result "
            "file against all older files in the same directory."
        )
    )
    parser.add_argument(
        "--results-dir",
        default=argv[0] if argv else ".",
        help="Directory containing results-*.md files. Defaults to the script directory.",
    )
    parser.add_argument("--theme", help="Selected theme for query drill-down mode.")
    parser.add_argument("--query-index", type=int, help="Selected query index for drill-down mode.")
    parser.add_argument(
        "--plans-only",
        action="store_true",
        help="In query drill-down mode, only print result files that contain the selected query plan kind.",
    )
    parser.add_argument(
        "--result-file",
        help="In query drill-down mode, only print the selected results file name.",
    )
    parser.add_argument(
        "--latest-file",
        help="Use this results file as the latest run instead of the newest dated file.",
    )
    parser.add_argument(
        "--today",
        action="store_true",
        help="Use the newest results-YYYY-MM-DD(-N).md file for today's local date as the latest run.",
    )
    parser.add_argument(
        "--plan-kind",
        choices=("all", "optimized", "telemetry"),
        default="all",
        help="In query drill-down mode, print optimized plans, telemetry plans, or both.",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="Print every latest query in the overview, not only queries that are more than 20%% slower than history.",
    )
    parser.add_argument(
        "--sort-regressions",
        action="store_true",
        help="Sort regressing queries from biggest to smallest regression.",
    )
    parser.add_argument(
        "--min-slower-pct",
        type=float,
        default=20.0,
        help="Only print queries where the historical best is at least this percent faster than latest.",
    )
    parser.add_argument(
        "--min-delta-ms",
        type=float,
        default=0.0,
        help="Only print queries where latest is at least this many ms/op slower than historical best.",
    )
    parser.add_argument(
        "--top",
        type=int,
        help="Only print the top N regressing queries. Implies regression sorting.",
    )
    parser.add_argument(
        "--baseline-start",
        help="Only use ThemeQueryBenchmark.executeQuery rows from dated files on or after YYYY-MM-DD.",
    )
    parser.add_argument(
        "--baseline-end",
        help="Only use ThemeQueryBenchmark.executeQuery rows from dated files on or before YYYY-MM-DD.",
    )
    parser.add_argument(
        "--baseline-stat",
        choices=("best", "average"),
        default="best",
        help="Compare latest rows with the historical best score or the arithmetic average of baseline rows.",
    )
    args = parser.parse_args(argv[1:] if argv else [])
    if (args.theme is None) ^ (args.query_index is None):
        parser.error("--theme and --query-index must be supplied together")
    query_mode = args.theme is not None
    query_only_flags = args.plans_only or args.result_file is not None or args.plan_kind != "all"
    if query_only_flags and not query_mode:
        parser.error("--plans-only, --result-file, and --plan-kind require --theme/--query-index")
    if args.latest_file is not None and args.today:
        parser.error("--latest-file and --today cannot be combined")
    if args.top is not None and args.top < 1:
        parser.error("--top must be >= 1")
    if args.min_slower_pct < 0.0 or args.min_slower_pct > 100.0:
        parser.error("--min-slower-pct must be between 0 and 100")
    if args.min_delta_ms < 0.0:
        parser.error("--min-delta-ms must be >= 0")
    if (args.baseline_start is None) ^ (args.baseline_end is None):
        parser.error("--baseline-start and --baseline-end must be supplied together")
    if args.baseline_start is not None:
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", args.baseline_start):
            parser.error("--baseline-start must use YYYY-MM-DD")
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", args.baseline_end):
            parser.error("--baseline-end must use YYYY-MM-DD")
        if args.baseline_start > args.baseline_end:
            parser.error("--baseline-start must be on or before --baseline-end")
    if query_mode and (
        args.sort_regressions
        or args.top is not None
        or args.all
        or args.min_slower_pct != 20.0
        or args.min_delta_ms != 0.0
        or args.baseline_start is not None
        or args.baseline_stat != "best"
    ):
        parser.error("--theme/--query-index cannot be combined with overview-only flags")
    if args.all and args.sort_regressions:
        parser.error("--all cannot be combined with --sort-regressions")
    if args.all and args.top is not None:
        parser.error("--all cannot be combined with --top")
    return args


def load_result_files(results_dir: Path) -> Dict[str, ResultFile]:
    files: Dict[str, ResultFile] = {}
    for path in sorted(results_dir.glob("results-*.md")):
        if not path.is_file():
            continue
        files[path.name] = parse_result_file(path)
    if not files:
        raise SystemExit(f"No result files found in {results_dir}")
    return files


def parse_result_file(path: Path) -> ResultFile:
    text = path.read_text(encoding="utf-8")
    benchmark_rows: Dict[str, Dict[QueryKey, float]] = {}
    benchmark_order: Dict[str, List[QueryKey]] = {}
    for line in text.splitlines():
        match = SUMMARY_ROW_RE.match(line)
        if not match:
            continue
        benchmark = match.group("benchmark")
        key = QueryKey(match.group("theme"), int(match.group("query_index")))
        rows = benchmark_rows.setdefault(benchmark, {})
        order = benchmark_order.setdefault(benchmark, [])
        if key not in rows:
            order.append(key)
        rows[key] = float(match.group("score"))

    query_content: Dict[QueryKey, QueryContent] = {}
    matches = list(PARAM_RE.finditer(text))
    for index, match in enumerate(matches):
        start = match.start()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        block = text[start:end]
        key = QueryKey(match.group(1), int(match.group(2)))
        query_content[key] = extract_query_content(block)

    return ResultFile(
        path=path,
        benchmark_rows=benchmark_rows,
        benchmark_order=benchmark_order,
        query_content=query_content,
    )


def extract_query_content(block: str) -> QueryContent:
    markers = list(QUERY_MARKER_RE.finditer(block))
    if not markers:
        return QueryContent(optimized_plan=None, optimized_query=None)

    optimized_plan: Optional[str] = None
    optimized_query: Optional[str] = None
    telemetry_plan: Optional[str] = None
    telemetry_query: Optional[str] = None

    for index, marker in enumerate(markers):
        section_start = marker.end()
        section_end = markers[index + 1].start() if index + 1 < len(markers) else len(block)
        plan, query = extract_marked_query_section(block[section_start:section_end])
        kind = marker.group(1)
        if kind == "Optimized":
            optimized_plan = plan
            optimized_query = query
        elif kind == "Telemetry":
            telemetry_plan = plan
            telemetry_query = query

    return QueryContent(
        optimized_plan=optimized_plan,
        optimized_query=optimized_query,
        telemetry_plan=telemetry_plan,
        telemetry_query=telemetry_query,
    )


def extract_marked_query_section(section: str) -> Tuple[Optional[str], Optional[str]]:
    lines = section.splitlines()
    while lines and not lines[0].strip():
        lines.pop(0)
    if not lines:
        return None, None

    query_start = None
    for index, line in enumerate(lines):
        if SPARQL_START_RE.match(line):
            query_start = index
            break

    if query_start is None:
        return trim_block(lines), None

    plan_lines = lines[:query_start]
    query_lines: List[str] = []
    for line in lines[query_start:]:
        stripped = line.strip()
        if RESULT_LINE_RE.match(line) or SCORE_LINE_RE.match(stripped):
            break
        if stripped == "# Run complete. Total time:":
            break
        query_lines.append(line)

    return trim_block(plan_lines), trim_block(query_lines)


def trim_block(lines: Iterable[str]) -> Optional[str]:
    collected = list(lines)
    while collected and not collected[0].strip():
        collected.pop(0)
    while collected and not collected[-1].strip():
        collected.pop()
    return "\n".join(collected) if collected else None


def query_benchmark_rows(result: ResultFile) -> Dict[QueryKey, float]:
    return result.benchmark_rows.get(QUERY_BENCHMARK, {})


def latest_rows(result: ResultFile) -> Dict[QueryKey, float]:
    rows = query_benchmark_rows(result)
    if rows:
        return rows
    return result.benchmark_rows.get(PLAN_RUN_BENCHMARK, {})


def latest_order(result: ResultFile) -> List[QueryKey]:
    if query_benchmark_rows(result):
        return result.benchmark_order.get(QUERY_BENCHMARK, [])
    return result.benchmark_order.get(PLAN_RUN_BENCHMARK, [])


def newest_dated_file(files: Dict[str, ResultFile]) -> ResultFile:
    dated: List[Tuple[str, int, str]] = []
    for name in files:
        match = DATED_FILE_RE.match(name)
        if match:
            dated.append((match.group(1), int(match.group(2) or 0), name))
    if not dated:
        raise SystemExit("No dated results-YYYY-MM-DD(-N).md files found")
    return files[sorted(dated)[-1][2]]


def newest_file_for_date(files: Dict[str, ResultFile], date_text: str) -> ResultFile:
    dated: List[Tuple[int, str]] = []
    for name in files:
        match = DATED_FILE_RE.match(name)
        if match and match.group(1) == date_text:
            dated.append((int(match.group(2) or 0), name))
    if not dated:
        raise SystemExit(f"No results file found for {date_text}")
    return files[sorted(dated)[-1][1]]


def selected_latest_file(files: Dict[str, ResultFile], args: argparse.Namespace) -> ResultFile:
    if args.latest_file is not None:
        latest_name = Path(args.latest_file).name
        if latest_name not in files:
            raise SystemExit(f"Latest file not found in results directory: {latest_name}")
        return files[latest_name]
    if args.today:
        return newest_file_for_date(files, date.today().isoformat())
    return newest_dated_file(files)


def dated_file_key(name: str) -> Optional[Tuple[str, int]]:
    match = DATED_FILE_RE.match(name)
    if not match:
        return None
    return match.group(1), int(match.group(2) or 0)


def in_baseline_window(name: str, baseline_start: Optional[str], baseline_end: Optional[str]) -> bool:
    if baseline_start is None:
        return True
    dated = dated_file_key(name)
    if dated is None:
        return False
    date, _ = dated
    return baseline_start <= date <= baseline_end


def baseline_entries(
    files: Dict[str, ResultFile],
    latest: ResultFile,
    key: QueryKey,
    baseline_start: Optional[str],
    baseline_end: Optional[str],
) -> List[HistoricalMatch]:
    entries: List[HistoricalMatch] = []
    latest_score = latest_rows(latest).get(key)
    for name, result in files.items():
        if name == latest.path.name or not in_baseline_window(name, baseline_start, baseline_end):
            continue
        score = query_benchmark_rows(result).get(key)
        if score is None:
            continue
        content = result.query_content.get(key, QueryContent(None, None))
        percent_faster = 0.0
        if latest_score is not None and latest_score != 0.0:
            percent_faster = ((latest_score - score) / latest_score) * 100.0
        entries.append(
            HistoricalMatch(
                file_name=name,
                score=score,
                percent_faster=percent_faster,
                content=content,
            )
        )
    return sort_matches(entries)


def historical_matches(
    files: Dict[str, ResultFile],
    latest: ResultFile,
    key: QueryKey,
    min_slower_pct: float,
    min_delta_ms: float,
    baseline_start: Optional[str] = None,
    baseline_end: Optional[str] = None,
    baseline_stat: str = "best",
) -> List[HistoricalMatch]:
    latest_score = latest_rows(latest)[key]
    if baseline_stat == "average":
        return baseline_entries(files, latest, key, baseline_start, baseline_end)
    threshold = latest_score * (1.0 - (min_slower_pct / 100.0))
    matches: List[HistoricalMatch] = []
    for name, result in files.items():
        if name == latest.path.name or not in_baseline_window(name, baseline_start, baseline_end):
            continue
        score = query_benchmark_rows(result).get(key)
        if score is None or score > threshold or latest_score - score < min_delta_ms:
            continue
        content = result.query_content.get(key, QueryContent(None, None))
        matches.append(
            HistoricalMatch(
                file_name=name,
                score=score,
                percent_faster=((latest_score - score) / latest_score) * 100.0,
                content=content,
            )
        )
    return sort_matches(matches)


def query_runs(files: Dict[str, ResultFile], latest: ResultFile, key: QueryKey) -> List[HistoricalMatch]:
    rows: List[HistoricalMatch] = []
    latest_score = latest_rows(latest).get(key)
    if latest_score is None:
        raise SystemExit(f"{key.label()} not present in latest run {latest.path.name}")
    for name, result in files.items():
        score = latest_rows(result).get(key) if name == latest.path.name else query_benchmark_rows(result).get(key)
        if score is None:
            continue
        content = result.query_content.get(key, QueryContent(None, None))
        rows.append(
            HistoricalMatch(
                file_name=name,
                score=score,
                percent_faster=((latest_score - score) / latest_score) * 100.0,
                content=content,
            )
        )
    return sorted(
        rows,
        key=lambda row: (row.score, -row.content.richness, row.file_name),
    )


def sort_matches(matches: List[HistoricalMatch]) -> List[HistoricalMatch]:
    return sorted(matches, key=lambda row: (row.score, -row.content.richness, row.file_name))


def format_score(value: float) -> str:
    return f"{value:.3f}"


def comparison_for_key(
    files: Dict[str, ResultFile],
    latest: ResultFile,
    key: QueryKey,
    baseline_start: Optional[str],
    baseline_end: Optional[str],
    baseline_stat: str,
) -> QueryComparison:
    latest_score = latest_rows(latest)[key]
    historical_scores = [entry.score for entry in baseline_entries(files, latest, key, baseline_start, baseline_end)]
    if not historical_scores:
        return QueryComparison(
            key=key,
            latest_score=latest_score,
            fastest_score=latest_score,
            previous_best_score=None,
            direction="no-history",
            delta_percent=0.0,
            baseline_stat=baseline_stat,
            baseline_count=0,
        )

    previous_best_score = min(historical_scores)
    baseline_score = (
        sum(historical_scores) / len(historical_scores)
        if baseline_stat == "average"
        else previous_best_score
    )
    if latest_score < baseline_score:
        return QueryComparison(
            key=key,
            latest_score=latest_score,
            fastest_score=baseline_score if baseline_stat == "average" else latest_score,
            previous_best_score=baseline_score,
            direction="faster",
            delta_percent=((baseline_score - latest_score) / baseline_score) * 100.0,
            baseline_stat=baseline_stat,
            baseline_count=len(historical_scores),
        )
    if latest_score == baseline_score:
        return QueryComparison(
            key=key,
            latest_score=latest_score,
            fastest_score=latest_score,
            previous_best_score=baseline_score,
            direction="same",
            delta_percent=0.0,
            baseline_stat=baseline_stat,
            baseline_count=len(historical_scores),
        )
    return QueryComparison(
        key=key,
        latest_score=latest_score,
        fastest_score=baseline_score,
        previous_best_score=baseline_score,
        direction="slower",
        delta_percent=((latest_score - baseline_score) / latest_score) * 100.0,
        baseline_stat=baseline_stat,
        baseline_count=len(historical_scores),
    )


def is_regression(comparison: QueryComparison, min_slower_pct: float, min_delta_ms: float) -> bool:
    return (
        comparison.direction == "slower"
        and comparison.delta_percent >= min_slower_pct
        and comparison.latest_score - comparison.fastest_score >= min_delta_ms
    )


def regression_sort_key(comparison: QueryComparison) -> Tuple[float, float, str, int]:
    return (-comparison.delta_percent, -comparison.latest_score, comparison.key.theme, comparison.key.query_index)


def summary_line(comparison: QueryComparison) -> str:
    if comparison.baseline_stat == "average":
        prefix = (
            f"  q{comparison.key.query_index}: latest {format_score(comparison.latest_score)} ms/op | "
            f"baseline average {format_score(comparison.fastest_score)} ms/op | "
        )
        if comparison.direction == "no-history":
            return prefix + "no baseline"
        if comparison.direction == "faster":
            return prefix + f"{comparison.delta_percent:.1f}% faster than baseline"
        if comparison.direction == "same":
            return prefix + "matches baseline"
        return prefix + f"{comparison.delta_percent:.1f}% slower than baseline"

    prefix = (
        f"  q{comparison.key.query_index}: latest {format_score(comparison.latest_score)} ms/op | "
        f"fastest {format_score(comparison.fastest_score)} ms/op | "
    )
    if comparison.direction == "no-history":
        return prefix + "no previous run"
    if comparison.direction == "faster":
        return (
            prefix
            + f"{comparison.delta_percent:.1f}% faster than previous best "
            + f"{format_score(comparison.previous_best_score)} ms/op"
        )
    if comparison.direction == "same":
        return prefix + "matches previous best"
    return prefix + f"{comparison.delta_percent:.1f}% slower than best"


def sorted_regression_keys(
    comparisons: Dict[QueryKey, QueryComparison],
    min_slower_pct: float,
    min_delta_ms: float,
) -> List[QueryKey]:
    return [
        comparison.key
        for comparison in sorted(
            (
                comparison
                for comparison in comparisons.values()
                if is_regression(comparison, min_slower_pct, min_delta_ms)
            ),
            key=regression_sort_key,
        )
    ]


def format_sorted_summary(comparisons: Dict[QueryKey, QueryComparison], summary_keys: List[QueryKey]) -> List[str]:
    lines: List[str] = []
    for index, key in enumerate(summary_keys, start=1):
        comparison = comparisons[key]
        if comparison.baseline_stat == "average":
            lines.append(
                f"{index}. {key.label()}: latest {format_score(comparison.latest_score)} ms/op | "
                f"baseline average {format_score(comparison.fastest_score)} ms/op | "
                f"{comparison.delta_percent:.1f}% slower than baseline"
            )
            continue
        lines.append(
            f"{index}. {key.label()}: latest {format_score(comparison.latest_score)} ms/op | "
            f"fastest {format_score(comparison.fastest_score)} ms/op | "
            f"{comparison.delta_percent:.1f}% slower than best"
        )
    return lines


def format_summary(
    latest: ResultFile,
    files: Dict[str, ResultFile],
    include_all: bool,
    sort_regressions: bool,
    top_n: Optional[int],
    min_slower_pct: float,
    min_delta_ms: float,
    baseline_start: Optional[str],
    baseline_end: Optional[str],
    baseline_stat: str,
) -> str:
    latest_summary_order = latest_order(latest)
    latest_summary_rows = latest_rows(latest)
    comparisons = {
        key: comparison_for_key(files, latest, key, baseline_start, baseline_end, baseline_stat)
        for key in latest_summary_order
    }
    regression_keys = sorted_regression_keys(comparisons, min_slower_pct, min_delta_ms)
    if top_n is not None:
        regression_keys = regression_keys[:top_n]

    if include_all:
        summary_keys = list(latest_summary_order)
    elif sort_regressions or top_n is not None:
        summary_keys = regression_keys
    else:
        summary_keys = [
            key for key in latest_summary_order if is_regression(comparisons[key], min_slower_pct, min_delta_ms)
        ]

    detail_keys = (
        regression_keys
        if (sort_regressions or top_n is not None)
        else [
            key
            for key in latest_summary_order
            if is_regression(comparisons[key], min_slower_pct, min_delta_ms)
        ]
    )

    lines = [
        f"Latest run: {latest.path.name}",
        f"Threshold: {min_slower_pct:.1f}%",
    ]
    if baseline_start is not None or baseline_stat != "best":
        window = (
            f"from {baseline_start} to {baseline_end}"
            if baseline_start is not None
            else "from all historical files"
        )
        lines.append(f"Baseline: {baseline_stat} {QUERY_BENCHMARK} {window}")
    if min_delta_ms > 0.0:
        lines.append(f"Minimum delta: {format_score(min_delta_ms)} ms")
    lines.extend(["", "Summary"])

    if not summary_keys:
        lines.append(f"No queries match the {min_slower_pct:.1f}% / {format_score(min_delta_ms)} ms thresholds.")
    elif sort_regressions or top_n is not None:
        lines.extend(format_sorted_summary(comparisons, summary_keys))
    else:
        grouped: Dict[str, List[QueryKey]] = {}
        for key in summary_keys:
            grouped.setdefault(key.theme, []).append(key)
        for theme, keys in grouped.items():
            lines.append(theme)
            for key in keys:
                lines.append(summary_line(comparisons[key]))

    if detail_keys:
        lines.extend(["", "Details"])
        for key in detail_keys:
            matches = historical_matches(
                files,
                latest,
                key,
                min_slower_pct,
                min_delta_ms,
                baseline_start,
                baseline_end,
                baseline_stat,
            )
            lines.append("")
            lines.append(key.label())
            lines.append(f"  latest: {format_score(latest_summary_rows[key])} ms/op")
            lines.append("  baseline runs:" if baseline_stat == "average" else "  faster runs:")
            for match in matches:
                if baseline_stat == "average":
                    delta = delta_label(match.percent_faster)
                    lines.append(
                        "  - "
                        f"{match.file_name}: {format_score(match.score)} ms/op "
                        f"({delta}) | "
                        f"plan {'yes' if match.content.has_plan else 'no'} | "
                        f"query {'yes' if match.content.has_query else 'no'}"
                    )
                    continue
                lines.append(
                    "  - "
                    f"{match.file_name}: {format_score(match.score)} ms/op "
                    f"({match.percent_faster:.1f}% faster) | "
                    f"plan {'yes' if match.content.has_plan else 'no'} | "
                    f"query {'yes' if match.content.has_query else 'no'}"
                )

    return "\n".join(lines)


def render_query_mode(
    files: Dict[str, ResultFile],
    latest: ResultFile,
    key: QueryKey,
    plans_only: bool,
    result_file: Optional[str],
    plan_kind: str,
) -> str:
    latest_summary_rows = latest_rows(latest)
    if key not in latest_summary_rows:
        raise SystemExit(f"{key.label()} not present in latest run {latest.path.name}")

    matches = query_runs(files, latest, key)
    if result_file is not None:
        matches = [match for match in matches if match.file_name == result_file]
    if plans_only:
        matches = [match for match in matches if match.content.has_plan_for(plan_kind)]

    lines = [
        f"Latest run: {latest.path.name}",
        f"Query: {key.label()}",
        f"Latest score: {format_score(latest_summary_rows[key])} ms/op",
    ]
    if result_file is not None:
        lines.append(f"Result file: {result_file}")
    if plans_only:
        lines.append(f"Plan filter: {plan_kind}")
    if not matches:
        lines.extend(["", "No matching runs."])
        return "\n".join(lines)

    for index, match in enumerate(matches, start=1):
        lines.extend(
            [
                "",
                f"{index}. {match.file_name}",
                "   "
                f"Score: {format_score(match.score)} ms/op | "
                f"{delta_label(match.percent_faster)} | "
                f"plan {'yes' if match.content.has_plan else 'no'} | "
                f"query {'yes' if match.content.has_query else 'no'}",
            ]
        )

        if plan_kind in ("all", "optimized"):
            append_named_query_sections(
                lines,
                "Optimized",
                match.content.optimized_plan,
                match.content.optimized_query,
            )
        if plan_kind in ("all", "telemetry") and (
            plan_kind == "telemetry" or match.content.telemetry_plan or match.content.telemetry_query
        ):
            append_named_query_sections(
                lines,
                "Telemetry",
                match.content.telemetry_plan,
                match.content.telemetry_query,
            )

    return "\n".join(lines)


def append_named_query_sections(
    lines: List[str],
    section_name: str,
    plan: Optional[str],
    query: Optional[str],
) -> None:
    lines.extend(["", f"   {section_name} query plan:"])
    lines.extend(indent_fence("text", plan))
    lines.extend(["", f"   {section_name} query:"])
    lines.extend(indent_fence("sparql", query))


def indent_fence(info: str, body: Optional[str]) -> List[str]:
    if not body:
        return ["   not present in this result file"]
    lines = [f"   ```{info}"]
    lines.extend(f"   {line}" if line else "   " for line in body.splitlines())
    lines.append("   ```")
    return lines


def delta_label(percent_faster: float) -> str:
    if percent_faster >= 0:
        return f"{percent_faster:.1f}% faster than latest"
    return f"{abs(percent_faster):.1f}% slower than latest"


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    results_dir = Path(args.results_dir).resolve()
    files = load_result_files(results_dir)
    latest = selected_latest_file(files, args)

    if args.theme is not None:
        output = render_query_mode(
            files,
            latest,
            QueryKey(args.theme, args.query_index),
            args.plans_only,
            args.result_file,
            args.plan_kind,
        )
    else:
        output = format_summary(
            latest,
            files,
            args.all,
            args.sort_regressions,
            args.top,
            args.min_slower_pct,
            args.min_delta_ms,
            args.baseline_start,
            args.baseline_end,
            args.baseline_stat,
        )

    print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
PY
