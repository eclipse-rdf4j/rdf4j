#!/usr/bin/env python3
"""Build the three-regime parity matrix from JMH result files.

This is the reporting half of Milestone 0 of ``.agent/three-tier-parity-execplan.md``. The plan's acceptance rule is an
ordering that must hold for every query in the corpus:

* the adjacency regime is never slower than the LMDB-cursor regime, and
* the Janino regime is never slower than either of the other two.

"Never slower" can only be judged against noise, so this script never compares bare scores. For each regime it builds a
conservative interval from every run it was given -- the lowest ``score - error`` and the highest ``score + error`` seen
across the runs -- and only calls a winner when two intervals are disjoint:

* ``OK``       the higher tier is strictly faster (its interval lies entirely below the other's).
* ``INVERTED`` the higher tier is strictly slower. This is the failure the plan exists to eliminate.
* ``OVERLAP``  the intervals touch, so the run cannot tell them apart. Equality satisfies the ordering, which is also
  what a higher tier *declining* a shape looks like; use ``ThreeTierEngagementCensusTest`` to tell a decline from a tie.

Cells whose score is large enough for the branch's known noise floor but whose error bars are wide are additionally
flagged ``noisy``: judge those only after re-running with longer iterations.

Usage::

    python3 scripts/three-tier-report.py benchmark-results/tier-m0-*.txt
    python3 scripts/three-tier-report.py --format md --fail-on-inverted benchmark-results/tier-m0-*.txt

Each input file is ordinary JMH stdout. The regime of a row comes from a ``(regime)`` parameter column when the benchmark
declares one, and otherwise from the file name (a path containing ``-lmdb-``, ``-adjacency-`` or ``-janino-``).
"""

from __future__ import annotations

import argparse
import math
import os
import re
import sys
from dataclasses import dataclass, field

REGIMES = ("lmdb", "adjacency", "janino")

# JMH prints times in whichever unit the benchmark asked for; the matrix normalises everything to milliseconds.
UNIT_TO_MS = {
    "s/op": 1000.0,
    "ms/op": 1.0,
    "us/op": 1.0 / 1000.0,
    "ns/op": 1.0 / 1_000_000.0,
}

DEFAULT_NOISE_FLOOR_MS = 100.0
DEFAULT_NOISY_RELATIVE_ERROR = 0.25


class ParseError(Exception):
    """Raised when a file contains no JMH result table at all."""


@dataclass
class Measurement:
    """One JMH row: a score with its error bar, both in milliseconds, plus where it came from."""

    score_ms: float
    error_ms: float
    source: str

    def low(self) -> float:
        return self.score_ms - self.error_ms

    def high(self) -> float:
        return self.score_ms + self.error_ms


@dataclass
class Interval:
    """The conservative envelope of every run of one (cell, regime) pair."""

    low: float
    high: float
    mean: float
    runs: int
    widest_relative_error: float

    def __str__(self) -> str:
        return f"{self.mean:.3f}±{(self.high - self.low) / 2:.3f}"


@dataclass
class Cell:
    """One matrix row: a benchmark method plus the parameters that are not the regime."""

    name: str
    params: tuple[tuple[str, str], ...]
    measurements: dict[str, list[Measurement]] = field(default_factory=dict)

    def label(self) -> str:
        if not self.params:
            return self.name
        rendered = ",".join(f"{key}={value}" for key, value in self.params)
        return f"{self.name}[{rendered}]"


def regime_from_path(path: str) -> str | None:
    """Infers the regime from a result-file name such as ``tier-m0-adjacency-r1-2026-08-04.txt``."""
    stem = os.path.basename(path)
    matches = {regime for regime in REGIMES if re.search(rf"[-_.]{regime}([-_.]|$)", stem)}
    return matches.pop() if len(matches) == 1 else None


def is_header(line: str) -> bool:
    stripped = line.strip()
    return stripped.startswith("Benchmark") and "Mode" in stripped and "Score" in stripped


def parse_header(line: str) -> list[str]:
    """Returns the parameter names of a JMH table header, in column order."""
    return [token[1:-1] for token in line.split() if token.startswith("(") and token.endswith(")")]


def parse_row(tokens: list[str], parameter_names: list[str]) -> tuple[str, tuple[str, ...], float, float, str] | None:
    """Splits one JMH data row into (benchmark, parameter values, score, error, units), or None when it is not a row.

    JMH prints ``Benchmark <params...> Mode Cnt Score ± Error Units`` once it has enough iterations for a confidence
    interval, and drops the ``± Error`` pair entirely below three, leaving ``... Score Units``. Both shapes appear in real
    result files, so both are accepted -- and a missing error bar is reported as NaN rather than guessed at.
    """
    if "±" in tokens:
        if len(tokens) != len(parameter_names) + 7:
            return None
        marker = tokens.index("±")
        score, error, units = tokens[marker - 1], tokens[marker + 1], tokens[-1]
    else:
        if len(tokens) != len(parameter_names) + 5:
            return None
        score, error, units = tokens[-2], "nan", tokens[-1]
    if units not in UNIT_TO_MS:
        return None
    try:
        score_value = float(score)
    except ValueError:
        return None
    try:
        error_value = float(error)
    except ValueError:
        error_value = math.nan
    values = tuple(tokens[1 : 1 + len(parameter_names)])
    return tokens[0], values, score_value, error_value, units


def read_file(path: str, cells: dict[tuple[str, tuple[tuple[str, str], ...]], Cell], warnings: list[str]) -> int:
    """Adds every result row of one file to ``cells`` and returns how many rows it contributed."""
    with open(path, encoding="utf-8", errors="replace") as handle:
        lines = handle.read().splitlines()

    fallback_regime = regime_from_path(path)
    parameter_names: list[str] | None = None
    rows = 0
    for line in lines:
        if is_header(line):
            parameter_names = parse_header(line)
            continue
        if parameter_names is None:
            continue
        tokens = line.split()
        if not tokens:
            continue
        parsed = parse_row(tokens, parameter_names)
        if parsed is None:
            continue
        benchmark, values, score, error, units = parsed
        name = benchmark.rsplit(".", 1)[-1]
        parameters = dict(zip(parameter_names, values))
        regime = parameters.pop("regime", None) or fallback_regime
        if regime is None:
            warnings.append(
                f"{path}: cannot tell which regime '{name}' belongs to; add a (regime) parameter column or put"
                " the regime in the file name"
            )
            continue
        regime = regime.strip().lower()
        if regime not in REGIMES:
            warnings.append(f"{path}: unknown regime '{regime}' for '{name}'")
            continue
        scale = UNIT_TO_MS[units]
        key = (name, tuple(sorted(parameters.items())))
        cell = cells.setdefault(key, Cell(name=name, params=key[1]))
        if math.isnan(error):
            warnings.append(f"{path}: '{name}' ({regime}) has no error bar; treating it as zero width")
            error = 0.0
        cell.measurements.setdefault(regime, []).append(
            Measurement(score_ms=score * scale, error_ms=abs(error) * scale, source=os.path.basename(path))
        )
        rows += 1
    if parameter_names is None:
        raise ParseError(f"{path}: no JMH result table found (no header line starting with 'Benchmark')")
    return rows


def envelope(measurements: list[Measurement]) -> Interval:
    mean = sum(measurement.score_ms for measurement in measurements) / len(measurements)
    widest = max(
        (measurement.error_ms / measurement.score_ms if measurement.score_ms > 0 else 0.0)
        for measurement in measurements
    )
    return Interval(
        low=min(measurement.low() for measurement in measurements),
        high=max(measurement.high() for measurement in measurements),
        mean=mean,
        runs=len(measurements),
        widest_relative_error=widest,
    )


def verdict(higher: Interval | None, lower: Interval | None) -> str:
    """Compares a higher tier against a lower one. Lower time is better, so a lower interval means faster."""
    if higher is None or lower is None:
        return "MISSING"
    if higher.high < lower.low:
        return "OK"
    if higher.low > lower.high:
        return "INVERTED"
    return "OVERLAP"


def worse_of(first: Interval | None, second: Interval | None) -> Interval | None:
    """The slower of two intervals: what the Janino regime has to beat to satisfy 'faster than both'."""
    if first is None:
        return second
    if second is None:
        return first
    return first if first.mean >= second.mean else second


@dataclass
class Row:
    cell: Cell
    intervals: dict[str, Interval | None]
    adjacency_verdict: str
    janino_verdict: str
    notes: list[str]


def build_rows(cells: dict, noise_floor_ms: float, noisy_relative_error: float) -> list[Row]:
    rows = []
    for key in sorted(cells):
        cell = cells[key]
        intervals: dict[str, Interval | None] = {}
        for regime in REGIMES:
            measurements = cell.measurements.get(regime)
            intervals[regime] = envelope(measurements) if measurements else None

        notes = []
        single_run = [regime for regime in REGIMES if intervals[regime] and intervals[regime].runs < 2]
        if single_run:
            notes.append("unpaired:" + "/".join(single_run))
        for regime in REGIMES:
            interval = intervals[regime]
            if (
                interval
                and interval.mean >= noise_floor_ms
                and interval.widest_relative_error >= noisy_relative_error
            ):
                notes.append(f"noisy:{regime}")
        missing = [regime for regime in REGIMES if intervals[regime] is None]
        if missing:
            notes.append("missing:" + "/".join(missing))

        rows.append(
            Row(
                cell=cell,
                intervals=intervals,
                adjacency_verdict=verdict(intervals["adjacency"], intervals["lmdb"]),
                janino_verdict=verdict(intervals["janino"], worse_of(intervals["lmdb"], intervals["adjacency"])),
                notes=notes,
            )
        )
    return rows


def render(rows: list[Row], output_format: str) -> str:
    header = ["cell", "lmdb (ms)", "adjacency (ms)", "janino (ms)", "adj vs lmdb", "janino vs both", "notes"]
    table = [header]
    for row in rows:
        table.append(
            [
                row.cell.label(),
                str(row.intervals["lmdb"]) if row.intervals["lmdb"] else "-",
                str(row.intervals["adjacency"]) if row.intervals["adjacency"] else "-",
                str(row.intervals["janino"]) if row.intervals["janino"] else "-",
                row.adjacency_verdict,
                row.janino_verdict,
                " ".join(row.notes) if row.notes else "",
            ]
        )

    if output_format == "md":
        widths = [0] * len(header)
    else:
        widths = [max(len(line[column]) for line in table) for column in range(len(header))]

    lines = []
    if output_format == "md":
        lines.append("| " + " | ".join(header) + " |")
        lines.append("|" + "|".join(["---"] * len(header)) + "|")
        for line in table[1:]:
            lines.append("| " + " | ".join(line) + " |")
    else:
        for index, line in enumerate(table):
            lines.append("  ".join(value.ljust(widths[column]) for column, value in enumerate(line)).rstrip())
            if index == 0:
                lines.append("  ".join("-" * widths[column] for column in range(len(header))))
    return "\n".join(lines)


def summarise(rows: list[Row]) -> tuple[str, int]:
    inverted = [
        (row, comparison)
        for row in rows
        for comparison, value in (("adjacency<lmdb", row.adjacency_verdict), ("janino<both", row.janino_verdict))
        if value == "INVERTED"
    ]
    counts = {verdict_name: 0 for verdict_name in ("OK", "OVERLAP", "INVERTED", "MISSING")}
    for row in rows:
        counts[row.adjacency_verdict] += 1
        counts[row.janino_verdict] += 1

    lines = [
        "",
        f"cells: {len(rows)}   comparisons: {sum(counts.values())}   "
        + "   ".join(f"{name}: {count}" for name, count in counts.items()),
    ]
    if inverted:
        lines.append("")
        lines.append(f"INVERTED CELLS ({len(inverted)}) -- a higher tier engaged and lost:")
        for row, comparison in inverted:
            lines.append(f"  {row.cell.label()}  {comparison}")
    else:
        lines.append("no inverted cells")
    return "\n".join(lines), len(inverted)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Build the three-regime parity matrix from JMH result files.")
    parser.add_argument("files", nargs="+", help="JMH result files (stdout captures), two or more runs per regime")
    parser.add_argument("--format", choices=("text", "md"), default="text", help="output format (default: text)")
    parser.add_argument(
        "--noise-floor-ms",
        type=float,
        default=DEFAULT_NOISE_FLOOR_MS,
        help=f"scores at or above this are flagged when their error bars are wide (default: {DEFAULT_NOISE_FLOOR_MS})",
    )
    parser.add_argument(
        "--noisy-relative-error",
        type=float,
        default=DEFAULT_NOISY_RELATIVE_ERROR,
        help=f"relative error that counts as noisy (default: {DEFAULT_NOISY_RELATIVE_ERROR})",
    )
    parser.add_argument("--fail-on-inverted", action="store_true", help="exit non-zero when any cell is inverted")
    arguments = parser.parse_args(argv)

    cells: dict = {}
    warnings: list[str] = []
    total_rows = 0
    for path in arguments.files:
        try:
            total_rows += read_file(path, cells, warnings)
        except (OSError, ParseError) as error:
            print(f"error: {error}", file=sys.stderr)
            return 2

    if not cells:
        print(f"error: parsed {total_rows} result rows from {len(arguments.files)} file(s)", file=sys.stderr)
        for warning in warnings:
            print(f"warning: {warning}", file=sys.stderr)
        return 2

    rows = build_rows(cells, arguments.noise_floor_ms, arguments.noisy_relative_error)
    print(render(rows, arguments.format))
    summary, inverted = summarise(rows)
    print(summary)
    for warning in warnings:
        print(f"warning: {warning}", file=sys.stderr)
    return 1 if inverted and arguments.fail_on_inverted else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
