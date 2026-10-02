#!/usr/bin/env python3
"""Compare LMDB compliance reports with a complete, explicit testcase baseline."""

from __future__ import annotations

import argparse
from collections import defaultdict
import json
from dataclasses import dataclass
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BASELINE = REPOSITORY_ROOT / "compliance" / "sparql" / "lmdb-compliance-baseline.json"
OUTCOMES = frozenset({"passed", "failed", "errored", "skipped"})


class BaselineGateError(ValueError):
    """Raised when the baseline or report input cannot support a sound comparison."""


@dataclass(frozen=True)
class Baseline:
    module: str
    required_suites: frozenset[str]
    expected_cases: dict[str, frozenset[str]]
    allowed_failures: frozenset[str]
    allowed_skips: frozenset[str]
    descriptions: dict[str, str]
    skip_descriptions: dict[str, str]
    schema_version: int
    manifest_error: str | None = None

    @property
    def all_expected_cases(self) -> frozenset[str]:
        return frozenset().union(*self.expected_cases.values()) if self.expected_cases else frozenset()


@dataclass(frozen=True)
class Observation:
    reports: tuple[Path, ...]
    observed_suites: frozenset[str]
    outcomes: dict[str, str]
    case_suites: dict[str, frozenset[str]]
    duplicate_cases: frozenset[str]
    contradictory_cases: frozenset[str]
    count_mismatches: dict[str, frozenset[str]]

    @property
    def failures(self) -> frozenset[str]:
        """Compatibility view of failures and errors from the exact outcome map."""
        return frozenset(
            case_id for case_id, outcome in self.outcomes.items() if outcome in {"failed", "errored"}
        )


@dataclass(frozen=True)
class Comparison:
    remaining_failures: set[str]
    resolved_failures: set[str]
    unresolved_failures: set[str]
    unexpected_failures: set[str]
    unexpected_errors: set[str]
    unexpected_skips: set[str]
    outcome_mismatches: set[str]
    missing_suites: set[str]
    unexecuted_suites: set[str]
    missing_cases: set[str]
    unknown_cases: set[str]
    duplicate_cases: set[str]
    contradictory_cases: set[str]
    count_mismatches: set[str]
    manifest_error: str | None

    @property
    def passed(self) -> bool:
        return not any(
            (
                self.manifest_error,
                self.unexpected_failures,
                self.unexpected_errors,
                self.unexpected_skips,
                self.outcome_mismatches,
                self.missing_suites,
                self.unexecuted_suites,
                self.missing_cases,
                self.unknown_cases,
                self.duplicate_cases,
                self.contradictory_cases,
                self.count_mismatches,
            )
        )


def _validate_case_id(case_id: object, required_suites: frozenset[str], label: str) -> str:
    if not isinstance(case_id, str) or not case_id or "#" not in case_id:
        raise BaselineGateError(f"{label} id must be '<class>#<test>'")
    suite, test_name = case_id.split("#", 1)
    if not suite or not test_name:
        raise BaselineGateError(f"{label} id must be '<class>#<test>'")
    if suite not in required_suites:
        raise BaselineGateError(f"{label} suite is not required: {case_id}")
    return case_id


def _load_entries(
    payload: dict[str, object],
    required_suites: frozenset[str],
    field_name: str,
    label: str,
) -> tuple[dict[str, str], frozenset[str]]:
    entries = payload.get(field_name)
    if not isinstance(entries, list):
        raise BaselineGateError(f"baseline {field_name} must be a list")
    descriptions: dict[str, str] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            raise BaselineGateError(f"each baseline {label} must be an object")
        case_id = _validate_case_id(entry.get("id"), required_suites, f"baseline {label}")
        description = entry.get("description")
        if not isinstance(description, str) or not description:
            raise BaselineGateError(f"baseline {label} {case_id} needs a description")
        if case_id in descriptions:
            raise BaselineGateError(f"duplicate baseline {label}: {case_id}")
        descriptions[case_id] = description
    return descriptions, frozenset(descriptions)


def _load_expected_cases(
    payload: dict[str, object], required_suites: frozenset[str]
) -> dict[str, frozenset[str]]:
    manifest = payload.get("requiredTestCases")
    if not isinstance(manifest, dict):
        raise BaselineGateError("baseline requiredTestCases must map each required suite to a string list")
    if set(manifest) != set(required_suites):
        missing = sorted(required_suites - set(manifest))
        extra = sorted(set(manifest) - set(required_suites))
        details = []
        if missing:
            details.append(f"missing suites: {', '.join(missing)}")
        if extra:
            details.append(f"unexpected suites: {', '.join(extra)}")
        raise BaselineGateError("baseline requiredTestCases keys are incomplete (" + "; ".join(details) + ")")

    expected_cases: dict[str, frozenset[str]] = {}
    all_cases: set[str] = set()
    for suite in required_suites:
        cases = manifest[suite]
        if not isinstance(cases, list) or not cases or not all(isinstance(case_id, str) for case_id in cases):
            raise BaselineGateError(f"baseline requiredTestCases[{suite}] must be a non-empty string list")
        validated = {
            _validate_case_id(case_id, required_suites, "baseline required testcase") for case_id in cases
        }
        if len(validated) != len(cases):
            raise BaselineGateError(f"duplicate baseline required testcase in suite: {suite}")
        if any(case_id.split("#", 1)[0] != suite for case_id in validated):
            raise BaselineGateError(f"required testcase is assigned to the wrong suite: {suite}")
        if all_cases.intersection(validated):
            raise BaselineGateError("duplicate baseline required testcase across suites")
        all_cases.update(validated)
        expected_cases[suite] = frozenset(validated)
    return expected_cases


def load_baseline(path: Path) -> Baseline:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise BaselineGateError(f"cannot read baseline {path}: {error}") from error
    if not isinstance(payload, dict):
        raise BaselineGateError("baseline root must be a JSON object")

    schema_version = payload.get("schemaVersion")
    if schema_version not in {1, 2}:
        raise BaselineGateError("baseline schemaVersion must be 1 or 2")
    module = payload.get("module")
    if not isinstance(module, str) or not module:
        raise BaselineGateError("baseline module must be a non-empty string")

    required = payload.get("requiredSuites")
    if not isinstance(required, list) or not required or not all(isinstance(item, str) and item for item in required):
        raise BaselineGateError("baseline requiredSuites must be a non-empty string list")
    required_suites = frozenset(required)
    if len(required_suites) != len(required):
        raise BaselineGateError("baseline requiredSuites contains duplicates")

    descriptions, allowed_failures = _load_entries(payload, required_suites, "failures", "failure")
    if schema_version == 1:
        return Baseline(
            module=module,
            required_suites=required_suites,
            expected_cases={suite: frozenset() for suite in required_suites},
            allowed_failures=allowed_failures,
            allowed_skips=frozenset(),
            descriptions=descriptions,
            skip_descriptions={},
            schema_version=1,
            manifest_error=(
                "baseline schemaVersion 1 has no requiredTestCases manifest; "
                "regenerate it from a trusted full run"
            ),
        )

    expected_cases = _load_expected_cases(payload, required_suites)
    skip_descriptions, allowed_skips = _load_entries(payload, required_suites, "allowedSkips", "allowed skip")
    if allowed_failures.intersection(allowed_skips):
        conflict = sorted(allowed_failures.intersection(allowed_skips))[0]
        raise BaselineGateError(f"baseline testcase cannot be both a failure and an allowed skip: {conflict}")
    all_expected = frozenset().union(*expected_cases.values())
    if not allowed_failures.issubset(all_expected):
        missing = sorted(allowed_failures - all_expected)
        raise BaselineGateError(f"baseline failure is absent from requiredTestCases: {missing[0]}")
    if not allowed_skips.issubset(all_expected):
        missing = sorted(allowed_skips - all_expected)
        raise BaselineGateError(f"baseline allowed skip is absent from requiredTestCases: {missing[0]}")
    return Baseline(
        module=module,
        required_suites=required_suites,
        expected_cases=expected_cases,
        allowed_failures=allowed_failures,
        allowed_skips=allowed_skips,
        descriptions=descriptions,
        skip_descriptions=skip_descriptions,
        schema_version=2,
    )


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
    return [element for element in root.iter() if local_name(element.tag) == "testsuite"]


def _suite_count_mismatch(
    report: Path,
    suite: ET.Element,
    aliases: set[str],
    testcases: list[ET.Element],
) -> tuple[str, frozenset[str]] | None:
    count_aliases = set(aliases)
    count_aliases.update(
        classname
        for testcase in testcases
        if (classname := testcase.attrib.get("classname"))
    )
    actual = {
        "tests": len(testcases),
        "failures": sum(any(local_name(child.tag) == "failure" for child in testcase) for testcase in testcases),
        "errors": sum(any(local_name(child.tag) == "error" for child in testcase) for testcase in testcases),
        "skipped": sum(any(local_name(child.tag) == "skipped" for child in testcase) for testcase in testcases),
    }
    for attribute, expected in actual.items():
        raw = suite.attrib.get(attribute)
        try:
            declared = int(raw) if raw is not None else None
        except ValueError:
            declared = None
        if declared is None or declared < 0 or declared != expected:
            key = f"{report}:{suite.attrib.get('name', '<unnamed>')}:{attribute}"
            return key, frozenset(count_aliases)
    return None


def inspect_reports(paths: list[Path]) -> Observation:
    reports = discover_reports(paths)
    observed_suites: set[str] = set()
    outcomes: dict[str, str] = {}
    case_suites: dict[str, set[str]] = defaultdict(set)
    duplicate_cases: set[str] = set()
    contradictory_cases: set[str] = set()
    count_mismatches: dict[str, frozenset[str]] = {}
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
            suite_aliases = {suite_name} if suite_name else set()
            if suite_name:
                observed_suites.add(suite_name)
            direct_testcases = [element for element in suite if local_name(element.tag) == "testcase"]
            nested_suites = any(local_name(element.tag) == "testsuite" for element in suite)
            count_testcases = (
                [element for element in suite.iter() if local_name(element.tag) == "testcase"]
                if nested_suites
                else direct_testcases
            )
            testcases = direct_testcases
            for testcase in testcases:
                classname = testcase.attrib.get("classname") or suite_name
                name = testcase.attrib.get("name")
                if not classname or not name:
                    raise BaselineGateError(f"testcase lacks classname or name in {report}")
                case_id = f"{classname}#{name}"
                case_aliases = suite_aliases | {classname}
                observed_suites.update(case_aliases)
                case_suites[case_id].update(case_aliases)
                outcome_elements = [
                    child for child in testcase if local_name(child.tag) in {"failure", "error", "skipped"}
                ]
                outcome_kinds = {local_name(child.tag) for child in outcome_elements}
                if len(outcome_elements) > 1:
                    contradictory_cases.add(case_id)
                if "failure" in outcome_kinds:
                    outcome = "failed"
                elif "error" in outcome_kinds:
                    outcome = "errored"
                elif "skipped" in outcome_kinds:
                    outcome = "skipped"
                else:
                    outcome = "passed"
                if case_id in outcomes:
                    duplicate_cases.add(case_id)
                    if outcomes[case_id] != outcome:
                        contradictory_cases.add(case_id)
                else:
                    outcomes[case_id] = outcome
            count_mismatch = _suite_count_mismatch(report, suite, suite_aliases, count_testcases)
            if count_mismatch is not None:
                key, mismatch_aliases = count_mismatch
                count_mismatches[key] = mismatch_aliases
    return Observation(
        reports=reports,
        observed_suites=frozenset(observed_suites),
        outcomes=outcomes,
        case_suites={case_id: frozenset(aliases) for case_id, aliases in case_suites.items()},
        duplicate_cases=frozenset(duplicate_cases),
        contradictory_cases=frozenset(contradictory_cases),
        count_mismatches=count_mismatches,
    )


def compare(baseline: Baseline, observation: Observation) -> Comparison:
    expected_cases = baseline.all_expected_cases
    observed_case_ids = frozenset(observation.outcomes)
    remaining = {
        case_id for case_id in baseline.allowed_failures if observation.outcomes.get(case_id) == "failed"
    }
    resolved = {
        case_id
        for case_id in baseline.allowed_failures
        if observation.outcomes.get(case_id) == "passed"
        and case_id not in observation.duplicate_cases
        and case_id not in observation.contradictory_cases
    }
    unresolved = set(baseline.allowed_failures - resolved)
    unexpected_failures = {
        case_id
        for case_id, outcome in observation.outcomes.items()
        if outcome == "failed" and case_id not in baseline.allowed_failures
    }
    unexpected_errors = {
        case_id
        for case_id, outcome in observation.outcomes.items()
        if outcome == "errored" and case_id not in baseline.allowed_failures
    }
    unexpected_skips = {
        case_id
        for case_id, outcome in observation.outcomes.items()
        if (
            outcome == "skipped"
            and baseline.required_suites.intersection(observation.case_suites.get(case_id, frozenset()))
            and case_id not in baseline.allowed_skips
        )
    }
    outcome_mismatches = {
        case_id
        for case_id, outcome in observation.outcomes.items()
        if (
            case_id in baseline.allowed_failures
            and outcome not in {"failed", "passed"}
        )
        or (case_id in baseline.allowed_skips and outcome not in {"skipped", "passed"})
    }
    executed_suites = {
        suite
        for case_id, outcome in observation.outcomes.items()
        if outcome != "skipped"
        for suite in observation.case_suites.get(case_id, frozenset())
        if suite in baseline.required_suites
    }
    missing_suites = set(baseline.required_suites - observation.observed_suites)
    unexecuted_suites = set(baseline.required_suites - executed_suites)
    unknown_cases = {
        case_id
        for case_id in observed_case_ids
        if baseline.required_suites.intersection(observation.case_suites.get(case_id, frozenset()))
        and case_id not in expected_cases
    }
    relevant_count_mismatches = {
        key
        for key, aliases in observation.count_mismatches.items()
        if aliases.intersection(baseline.required_suites)
    }
    return Comparison(
        remaining_failures=remaining,
        resolved_failures=resolved,
        unresolved_failures=unresolved,
        unexpected_failures=unexpected_failures,
        unexpected_errors=unexpected_errors,
        unexpected_skips=unexpected_skips,
        outcome_mismatches=outcome_mismatches,
        missing_suites=missing_suites,
        unexecuted_suites=unexecuted_suites,
        missing_cases=set(expected_cases - observed_case_ids),
        unknown_cases=unknown_cases,
        duplicate_cases=set(observation.duplicate_cases),
        contradictory_cases=set(observation.contradictory_cases),
        count_mismatches=relevant_count_mismatches,
        manifest_error=baseline.manifest_error,
    )


def _print_ids(label: str, values: set[str]) -> None:
    for value in sorted(values):
        print(f"  {label}: {value}")


def print_comparison(baseline: Baseline, observation: Observation, comparison: Comparison) -> None:
    status = "PASS" if comparison.passed else "FAIL"
    observed_required = len(baseline.required_suites - comparison.missing_suites)
    observed_expected = len(baseline.all_expected_cases - comparison.missing_cases)
    print(f"LMDB compliance baseline gate: {status}")
    print(f"Module: {baseline.module}")
    print(f"Reports parsed: {len(observation.reports)}")
    print(f"Required suites observed: {observed_required}/{len(baseline.required_suites)}")
    print(f"Required testcase coverage: {observed_expected}/{len(baseline.all_expected_cases)}")
    print(
        "Remaining baseline failures: "
        f"{len(comparison.remaining_failures)}/{len(baseline.allowed_failures)}"
    )
    print(f"Resolved baseline failures: {len(comparison.resolved_failures)}")
    if comparison.manifest_error:
        print(f"  baseline manifest: {comparison.manifest_error}")
    _print_ids("resolved", comparison.resolved_failures)
    _print_ids("remaining", comparison.remaining_failures)
    _print_ids("unresolved", comparison.unresolved_failures)
    _print_ids("missing suite", comparison.missing_suites)
    _print_ids("suite has no executed testcase", comparison.unexecuted_suites)
    _print_ids("missing testcase", comparison.missing_cases)
    _print_ids("unknown testcase", comparison.unknown_cases)
    _print_ids("duplicate testcase", comparison.duplicate_cases)
    _print_ids("contradictory testcase", comparison.contradictory_cases)
    _print_ids("stale report count", comparison.count_mismatches)
    _print_ids("unexpected", comparison.unexpected_failures)
    _print_ids("unexpected error", comparison.unexpected_errors)
    _print_ids("unexpected skip", comparison.unexpected_skips)
    _print_ids("outcome mismatch", comparison.outcome_mismatches)


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
