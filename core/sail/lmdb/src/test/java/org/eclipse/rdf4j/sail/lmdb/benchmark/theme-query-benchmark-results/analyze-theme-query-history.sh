#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

python3 - "${SCRIPT_DIR}" "$@" <<'PY'
import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

DATED_FILE_RE = re.compile(r"results-(\d{4}-\d{2}-\d{2})(?:-(\d+))?\.md$")
SUMMARY_ROW_RE = re.compile(
    r"^ThemeQueryBenchmark\.executeQuery\s+([A-Z_]+)\s+(\d+)\s+avgt\s+(?:\d+\s+)?([0-9.]+)"
)
PARAM_RE = re.compile(r"^# Parameters: \(themeName = ([A-Z_]+), z_queryIndex = (\d+)\)$", re.MULTILINE)
OPTIMIZED_MARKER_RE = re.compile(r"^.*### Optimized Query ###\s*$", re.MULTILINE)
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
    query_plan: Optional[str]
    optimized_query: Optional[str]

    @property
    def has_plan(self) -> bool:
        return bool(self.query_plan)

    @property
    def has_query(self) -> bool:
        return bool(self.optimized_query)

    @property
    def richness(self) -> int:
        return int(self.has_plan) + int(self.has_query)


@dataclass(frozen=True)
class ResultFile:
    path: Path
    summary_rows: Dict[QueryKey, float]
    summary_order: List[QueryKey]
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
        "--top",
        type=int,
        help="Only print the top N regressing queries. Implies regression sorting.",
    )
    args = parser.parse_args(argv[1:] if argv else [])
    if (args.theme is None) ^ (args.query_index is None):
        parser.error("--theme and --query-index must be supplied together")
    if args.top is not None and args.top < 1:
        parser.error("--top must be >= 1")
    if args.theme is not None and (args.sort_regressions or args.top is not None or args.all):
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
    summary_rows: Dict[QueryKey, float] = {}
    summary_order: List[QueryKey] = []
    for line in text.splitlines():
        match = SUMMARY_ROW_RE.match(line)
        if not match:
            continue
        key = QueryKey(match.group(1), int(match.group(2)))
        if key not in summary_rows:
            summary_order.append(key)
        summary_rows[key] = float(match.group(3))

    query_content: Dict[QueryKey, QueryContent] = {}
    matches = list(PARAM_RE.finditer(text))
    for index, match in enumerate(matches):
        start = match.start()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        block = text[start:end]
        key = QueryKey(match.group(1), int(match.group(2)))
        query_content[key] = extract_query_content(block)

    return ResultFile(path=path, summary_rows=summary_rows, summary_order=summary_order, query_content=query_content)


def extract_query_content(block: str) -> QueryContent:
    marker = OPTIMIZED_MARKER_RE.search(block)
    if not marker:
        return QueryContent(query_plan=None, optimized_query=None)

    after_marker = block[marker.end():].splitlines()
    while after_marker and not after_marker[0].strip():
        after_marker.pop(0)
    if not after_marker:
        return QueryContent(query_plan=None, optimized_query=None)

    query_start = None
    for index, line in enumerate(after_marker):
        if SPARQL_START_RE.match(line):
            query_start = index
            break

    if query_start is None:
        plan_text = trim_block(after_marker)
        return QueryContent(query_plan=plan_text, optimized_query=None)

    plan_lines = after_marker[:query_start]
    query_lines: List[str] = []
    for line in after_marker[query_start:]:
        stripped = line.strip()
        if RESULT_LINE_RE.match(line) or SCORE_LINE_RE.match(stripped):
            break
        if stripped == "# Run complete. Total time:":
            break
        query_lines.append(line)

    return QueryContent(
        query_plan=trim_block(plan_lines),
        optimized_query=trim_block(query_lines),
    )


def trim_block(lines: Iterable[str]) -> Optional[str]:
    collected = list(lines)
    while collected and not collected[0].strip():
        collected.pop(0)
    while collected and not collected[-1].strip():
        collected.pop()
    return "\n".join(collected) if collected else None


def newest_dated_file(files: Dict[str, ResultFile]) -> ResultFile:
    dated: List[Tuple[str, int, str]] = []
    for name in files:
        match = DATED_FILE_RE.match(name)
        if match:
            dated.append((match.group(1), int(match.group(2) or 0), name))
    if not dated:
        raise SystemExit("No dated results-YYYY-MM-DD(-N).md files found")
    return files[sorted(dated)[-1][2]]


def historical_matches(files: Dict[str, ResultFile], latest: ResultFile, key: QueryKey) -> List[HistoricalMatch]:
    latest_score = latest.summary_rows[key]
    threshold = latest_score * 0.8
    matches: List[HistoricalMatch] = []
    for name, result in files.items():
        if name == latest.path.name:
            continue
        score = result.summary_rows.get(key)
        if score is None or score > threshold:
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


def top_runs(files: Dict[str, ResultFile], key: QueryKey) -> List[HistoricalMatch]:
    rows: List[HistoricalMatch] = []
    latest = newest_dated_file(files)
    latest_score = latest.summary_rows.get(key)
    if latest_score is None:
        raise SystemExit(f"{key.label()} not present in latest run {latest.path.name}")
    for name, result in files.items():
        score = result.summary_rows.get(key)
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
    )[:3]


def sort_matches(matches: List[HistoricalMatch]) -> List[HistoricalMatch]:
    return sorted(matches, key=lambda row: (row.score, -row.content.richness, row.file_name))


def format_score(value: float) -> str:
    return f"{value:.3f}"


def comparison_for_key(files: Dict[str, ResultFile], latest: ResultFile, key: QueryKey) -> QueryComparison:
    latest_score = latest.summary_rows[key]
    historical_scores = [
        score for name, result in files.items() if name != latest.path.name if (score := result.summary_rows.get(key)) is not None
    ]
    if not historical_scores:
        return QueryComparison(
            key=key,
            latest_score=latest_score,
            fastest_score=latest_score,
            previous_best_score=None,
            direction="no-history",
            delta_percent=0.0,
        )

    previous_best_score = min(historical_scores)
    if latest_score < previous_best_score:
        return QueryComparison(
            key=key,
            latest_score=latest_score,
            fastest_score=latest_score,
            previous_best_score=previous_best_score,
            direction="faster",
            delta_percent=((previous_best_score - latest_score) / previous_best_score) * 100.0,
        )
    if latest_score == previous_best_score:
        return QueryComparison(
            key=key,
            latest_score=latest_score,
            fastest_score=latest_score,
            previous_best_score=previous_best_score,
            direction="same",
            delta_percent=0.0,
        )
    return QueryComparison(
        key=key,
        latest_score=latest_score,
        fastest_score=previous_best_score,
        previous_best_score=previous_best_score,
        direction="slower",
        delta_percent=((latest_score - previous_best_score) / latest_score) * 100.0,
    )


def is_regression(comparison: QueryComparison) -> bool:
    return comparison.direction == "slower" and comparison.delta_percent >= 20.0


def regression_sort_key(comparison: QueryComparison) -> Tuple[float, float, str, int]:
    return (-comparison.delta_percent, -comparison.latest_score, comparison.key.theme, comparison.key.query_index)


def summary_line(comparison: QueryComparison) -> str:
    prefix = f"  q{comparison.key.query_index}: latest {format_score(comparison.latest_score)} ms/op | fastest {format_score(comparison.fastest_score)} ms/op | "
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


def sorted_regression_keys(comparisons: Dict[QueryKey, QueryComparison]) -> List[QueryKey]:
    return [
        comparison.key
        for comparison in sorted(
            (comparison for comparison in comparisons.values() if is_regression(comparison)),
            key=regression_sort_key,
        )
    ]


def format_sorted_summary(comparisons: Dict[QueryKey, QueryComparison], summary_keys: List[QueryKey]) -> List[str]:
    lines: List[str] = []
    for index, key in enumerate(summary_keys, start=1):
        comparison = comparisons[key]
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
) -> str:
    comparisons = {key: comparison_for_key(files, latest, key) for key in latest.summary_order}
    regression_keys = sorted_regression_keys(comparisons)
    if top_n is not None:
        regression_keys = regression_keys[:top_n]

    if include_all:
        summary_keys = list(latest.summary_order)
    elif sort_regressions or top_n is not None:
        summary_keys = regression_keys
    else:
        summary_keys = [key for key in latest.summary_order if is_regression(comparisons[key])]

    detail_keys = regression_keys if (sort_regressions or top_n is not None) else [key for key in latest.summary_order if is_regression(comparisons[key])]

    lines = [
        f"Latest run: {latest.path.name}",
        "Threshold: 20.0%",
        "",
        "Summary",
    ]

    if not summary_keys:
        lines.append("No queries are more than 20.0% slower than historical best.")
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
            matches = historical_matches(files, latest, key)
            lines.append("")
            lines.append(key.label())
            lines.append(f"  latest: {format_score(latest.summary_rows[key])} ms/op")
            lines.append("  faster runs:")
            for match in matches:
                lines.append(
                    "  - "
                    f"{match.file_name}: {format_score(match.score)} ms/op "
                    f"({match.percent_faster:.1f}% faster) | "
                    f"plan {'yes' if match.content.has_plan else 'no'} | "
                    f"query {'yes' if match.content.has_query else 'no'}"
                )

    return "\n".join(lines)


def render_query_mode(files: Dict[str, ResultFile], latest: ResultFile, key: QueryKey) -> str:
    if key not in latest.summary_rows:
        raise SystemExit(f"{key.label()} not present in latest run {latest.path.name}")

    top = top_runs(files, key)
    lines = [
        f"Latest run: {latest.path.name}",
        f"Query: {key.label()}",
        f"Latest score: {format_score(latest.summary_rows[key])} ms/op",
    ]

    for index, match in enumerate(top, start=1):
        lines.extend(
            [
                "",
                f"{index}. {match.file_name}",
                "   "
                f"Score: {format_score(match.score)} ms/op | "
                f"{delta_label(match.percent_faster)} | "
                f"plan {'yes' if match.content.has_plan else 'no'} | "
                f"query {'yes' if match.content.has_query else 'no'}",
                "",
                "   Optimized query plan:",
            ]
        )
        if match.content.has_plan:
            lines.extend(indent_fence("text", match.content.query_plan))
        else:
            lines.append("   not present in this result file")

        lines.extend(["", "   Optimized query:"])
        if match.content.has_query:
            lines.extend(indent_fence("sparql", match.content.optimized_query))
        else:
            lines.append("   not present in this result file")

    return "\n".join(lines)


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
    latest = newest_dated_file(files)

    if args.theme is not None:
        output = render_query_mode(files, latest, QueryKey(args.theme, args.query_index))
    else:
        output = format_summary(latest, files, args.all, args.sort_regressions, args.top)

    print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
PY
