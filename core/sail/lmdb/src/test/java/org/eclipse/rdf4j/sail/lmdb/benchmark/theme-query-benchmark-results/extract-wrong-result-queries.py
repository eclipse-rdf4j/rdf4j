#!/usr/bin/env python3
import argparse
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


DEFAULT_RESULTS_FILE = "results-2026-06-30-3.md"

PARAM_RE = re.compile(
    r"^# Parameters: \(themeName = (?P<theme>[A-Z_]+), z_queryIndex = (?P<index>\d+)\)$",
    re.MULTILINE,
)
WRONG_RESULT_RE = re.compile(
    r"Unexpected (?P<kind>\?count binding|result row count) for "
    r"(?P<theme>[A-Z_]+) query (?P<index>\d+) "
    r"\((?P<name>[^)]*)\): expected (?P<expected>.*?) but got (?P<got>[^\n]+)"
)
STOP_LINE_RE = re.compile(
    r"^(?:<failure>|### |# Run complete\.|Result \"|[0-9.]+(?:\s+\S+\s+[0-9.]+)?\s+ms/op$)"
)


@dataclass(frozen=True)
class WrongResult:
    theme: str
    query_index: int
    kind: str
    name: str
    expected: str
    got: str
    query: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Extract theme benchmark queries that failed because their result was wrong."
    )
    parser.add_argument(
        "result_file",
        nargs="?",
        default=DEFAULT_RESULTS_FILE,
        help=f"Markdown results file to scan. Defaults to {DEFAULT_RESULTS_FILE}.",
    )
    parser.add_argument(
        "--summary",
        action="store_true",
        help="Only print theme/query ids and expected/got values.",
    )
    return parser.parse_args()


def parameter_blocks(text: str) -> Iterable[tuple[str, int, str]]:
    matches = list(PARAM_RE.finditer(text))
    for pos, match in enumerate(matches):
        end = matches[pos + 1].start() if pos + 1 < len(matches) else len(text)
        yield match.group("theme"), int(match.group("index")), text[match.start() : end]


def extract_original_query(block: str) -> str:
    marker = "### Original Query ###"
    marker_start = block.find(marker)
    if marker_start < 0:
        return ""

    lines = block[marker_start + len(marker) :].splitlines()
    query_lines: list[str] = []
    in_query = False

    for line in lines:
        stripped = line.strip()
        if not in_query and not stripped:
            continue
        if STOP_LINE_RE.match(stripped):
            break
        in_query = True
        query_lines.append(line.rstrip())

    while query_lines and not query_lines[-1].strip():
        query_lines.pop()

    return "\n".join(query_lines)


def wrong_results(text: str) -> list[WrongResult]:
    failures: list[WrongResult] = []

    for param_theme, param_index, block in parameter_blocks(text):
        match = WRONG_RESULT_RE.search(block)
        if not match:
            continue

        theme = match.group("theme")
        query_index = int(match.group("index"))
        if theme != param_theme or query_index != param_index:
            raise ValueError(
                f"Failure metadata {theme} q{query_index} does not match "
                f"parameter block {param_theme} q{param_index}"
            )

        failures.append(
            WrongResult(
                theme=theme,
                query_index=query_index,
                kind=match.group("kind"),
                name=match.group("name"),
                expected=match.group("expected").strip(),
                got=match.group("got").strip(),
                query=extract_original_query(block),
            )
        )

    return failures


def print_failure(failure: WrongResult, summary: bool) -> None:
    label = f"{failure.theme} q{failure.query_index}"
    print(f"## {label}: {failure.kind}")
    print(f"{failure.name}")
    print(f"expected {failure.expected}, got {failure.got}")
    if summary:
        print()
        return

    print()
    if failure.query:
        print("```sparql")
        print(failure.query)
        print("```")
    else:
        print("_No original query found._")
    print()


def main() -> int:
    args = parse_args()
    path = Path(args.result_file)
    text = path.read_text(encoding="utf-8")
    failures = wrong_results(text)

    print(f"Found {len(failures)} wrong-result failures in {path.name}.")
    print()
    for failure in failures:
        print_failure(failure, args.summary)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
