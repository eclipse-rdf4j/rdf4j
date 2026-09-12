#!/usr/bin/env python3
"""Fail when SPARQL compliance reports exceed the frozen LMDB failure baseline."""

from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BASELINE = REPOSITORY_ROOT / "compliance" / "sparql" / "lmdb-compliance-baseline.json"


class BaselineGateError(ValueError):
    """Raised when the baseline or report input cannot support a sound comparison."""


@dataclass(frozen=True)
class Baseline:
    module: str
    required_suites: frozenset[str]
    allowed_failures: frozenset[str]
    descriptions: dict[str, str]


@dataclass(frozen=True)
class Observation:
    reports: tuple[Path, ...]
    observed_suites: frozenset[str]
    failures: frozenset[str]


@dataclass(frozen=True)
class Comparison:
    remaining_failures: set[str]
    resolved_failures: set[str]
    unexpected_failures: set[str]
    missing_suites: set[str]

    @property
    def passed(self) -> bool:
        return not self.unexpected_failures and not self.missing_suites


def load_baseline(path: Path) -> Baseline:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise BaselineGateError(f"cannot read baseline {path}: {error}") from error

    if payload.get("schemaVersion") != 1:
        raise BaselineGateError("baseline schemaVersion must be 1")
    module = payload.get("module")
    if not isinstance(module, str) or not module:
        raise BaselineGateError("baseline module must be a non-empty string")

    required = payload.get("requiredSuites")
    if not isinstance(required, list) or not required or not all(isinstance(item, str) for item in required):
        raise BaselineGateError("baseline requiredSuites must be a non-empty string list")
    required_suites = frozenset(required)
    if len(required_suites) != len(required):
        raise BaselineGateError("baseline requiredSuites contains duplicates")

    failures = payload.get("failures")
    if not isinstance(failures, list):
        raise BaselineGateError("baseline failures must be a list")
    descriptions: dict[str, str] = {}
    for entry in failures:
        if not isinstance(entry, dict):
            raise BaselineGateError("each baseline failure must be an object")
        failure_id = entry.get("id")
        description = entry.get("description")
        if not isinstance(failure_id, str) or "#" not in failure_id:
            raise BaselineGateError("each baseline failure id must be '<class>#<test>'")
        if not isinstance(description, str) or not description:
            raise BaselineGateError(f"baseline failure {failure_id} needs a description")
        suite, _ = failure_id.split("#", 1)
        if suite not in required_suites:
            raise BaselineGateError(f"baseline failure suite is not required: {failure_id}")
        if failure_id in descriptions:
            raise BaselineGateError(f"duplicate baseline failure: {failure_id}")
        descriptions[failure_id] = description

    return Baseline(module, required_suites, frozenset(descriptions), descriptions)


def discover_reports(paths: list[Path]) -> tuple[Path, ...]:
    reports: set[Path] = set()
    for path in paths:
        if path.is_dir():
            reports.update(candidate.resolve() for candidate in path.glob("TEST-*.xml"))
        elif path.is_file() and path.name.startswith("TEST-") and path.suffix == ".xml":
            reports.add(path.resolve())
        else:
            raise BaselineGateError(f"report path is not a report directory or TEST-*.xml file: {path}")
    if not reports:
        raise BaselineGateError("no TEST-*.xml reports found")
    return tuple(sorted(reports))


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def test_suites(root: ET.Element) -> list[ET.Element]:
    if local_name(root.tag) == "testsuite":
        return [root]
    return [element for element in root.iter() if local_name(element.tag) == "testsuite"]


def inspect_reports(paths: list[Path]) -> Observation:
    reports = discover_reports(paths)
    observed_suites: set[str] = set()
    failures: set[str] = set()
    for report in reports:
        try:
            root = ET.parse(report).getroot()
        except (ET.ParseError, OSError) as error:
            raise BaselineGateError(f"cannot parse report {report}: {error}") from error
        suites = test_suites(root)
        if not suites:
            raise BaselineGateError(f"report contains no testsuite: {report}")
        for suite in suites:
            suite_name = suite.attrib.get("name")
            if suite_name:
                observed_suites.add(suite_name)
            for testcase in suite.iter():
                if local_name(testcase.tag) != "testcase":
                    continue
                classname = testcase.attrib.get("classname") or suite_name
                name = testcase.attrib.get("name")
                if not classname or not name:
                    raise BaselineGateError(f"testcase lacks classname or name in {report}")
                observed_suites.add(classname)
                if any(local_name(child.tag) in {"failure", "error"} for child in testcase):
                    failures.add(f"{classname}#{name}")
    return Observation(reports, frozenset(observed_suites), frozenset(failures))


def compare(baseline: Baseline, observation: Observation) -> Comparison:
    remaining = set(observation.failures & baseline.allowed_failures)
    return Comparison(
        remaining_failures=remaining,
        resolved_failures=set(baseline.allowed_failures - observation.failures),
        unexpected_failures=set(observation.failures - baseline.allowed_failures),
        missing_suites=set(baseline.required_suites - observation.observed_suites),
    )


def print_comparison(baseline: Baseline, observation: Observation, comparison: Comparison) -> None:
    status = "PASS" if comparison.passed else "FAIL"
    observed_required = len(baseline.required_suites - comparison.missing_suites)
    print(f"LMDB compliance baseline gate: {status}")
    print(f"Module: {baseline.module}")
    print(f"Reports parsed: {len(observation.reports)}")
    print(f"Required suites observed: {observed_required}/{len(baseline.required_suites)}")
    print(
        "Remaining baseline failures: "
        f"{len(comparison.remaining_failures)}/{len(baseline.allowed_failures)}"
    )
    print(f"Resolved baseline failures: {len(comparison.resolved_failures)}")
    if comparison.resolved_failures:
        for failure in sorted(comparison.resolved_failures):
            print(f"  resolved: {failure} — {baseline.descriptions[failure]}")
    if comparison.missing_suites:
        for suite in sorted(comparison.missing_suites):
            print(f"  missing suite: {suite}")
    if comparison.unexpected_failures:
        for failure in sorted(comparison.unexpected_failures):
            print(f"  unexpected: {failure}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--baseline",
        type=Path,
        default=DEFAULT_BASELINE,
        help=f"Frozen baseline JSON (default: {DEFAULT_BASELINE})",
    )
    parser.add_argument("reports", nargs="+", type=Path, help="Failsafe report directories or TEST-*.xml files")
    args = parser.parse_args(argv)
    try:
        baseline = load_baseline(args.baseline)
        observation = inspect_reports(args.reports)
    except BaselineGateError as error:
        print(f"LMDB compliance baseline gate: ERROR\n{error}", file=sys.stderr)
        return 2
    comparison = compare(baseline, observation)
    print_comparison(baseline, observation, comparison)
    return 0 if comparison.passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
