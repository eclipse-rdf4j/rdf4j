#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from textwrap import dedent
from xml.sax.saxutils import escape


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
SCRIPT = REPOSITORY_ROOT / "scripts" / "check-lmdb-compliance-baseline.py"
CHECKED_IN_BASELINE = REPOSITORY_ROOT / "compliance" / "sparql" / "lmdb-compliance-baseline.json"
QUERY_SUITE = "example.LmdbQueryComplianceTest"
UPDATE_SUITE = "example.LmdbUpdateComplianceTest"


def load_script_module():
    spec = importlib.util.spec_from_file_location("check_lmdb_compliance_baseline", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class LmdbComplianceBaselineTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.module = load_script_module()

    def test_known_failure_subset_passes_and_reports_resolved_entries(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, QUERY_SUITE, ["tests()[1]"], passed=["tests()[2]"], skipped=["tests()[3]"])
            self.write_report(reports, UPDATE_SUITE, [], passed=["tests()[1]", "tests()[2]"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertTrue(comparison.passed)
            self.assertEqual(comparison.remaining_failures, {f"{QUERY_SUITE}#tests()[1]"})
            self.assertEqual(comparison.resolved_failures, {f"{QUERY_SUITE}#tests()[2]"})
            self.assertEqual(comparison.unresolved_failures, {f"{QUERY_SUITE}#tests()[1]"})
            self.assertEqual(comparison.missing_suites, set())
            self.assertEqual(comparison.missing_cases, set())
            self.assertEqual(comparison.unexpected_skips, set())

    def test_all_expected_cases_passing_resolves_every_baseline_failure(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, QUERY_SUITE, [], passed=["tests()[1]", "tests()[2]", "tests()[3]"])
            self.write_report(reports, UPDATE_SUITE, [], passed=["tests()[1]", "tests()[2]"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertTrue(comparison.passed)
            self.assertEqual(
                comparison.resolved_failures,
                {f"{QUERY_SUITE}#tests()[1]", f"{QUERY_SUITE}#tests()[2]"},
            )
            self.assertEqual(comparison.remaining_failures, set())

    def test_empty_required_suite_report_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, QUERY_SUITE, [], passed=[])
            self.write_report(reports, UPDATE_SUITE, [], passed=[])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(comparison.unexecuted_suites, {QUERY_SUITE, UPDATE_SUITE})
            self.assertEqual(len(comparison.missing_cases), 5)

    def test_fully_skipped_required_suite_report_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(
                reports,
                QUERY_SUITE,
                [],
                passed=[],
                skipped=["tests()[1]", "tests()[2]", "tests()[3]"],
            )
            self.write_report(reports, UPDATE_SUITE, [], passed=[], skipped=["tests()[1]", "tests()[2]"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(comparison.unexecuted_suites, {QUERY_SUITE, UPDATE_SUITE})
            self.assertNotIn(f"{QUERY_SUITE}#tests()[1]", comparison.resolved_failures)

    def test_absent_baseline_case_is_not_marked_resolved(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, QUERY_SUITE, [], passed=["unrelated-pass"])
            self.write_report(reports, UPDATE_SUITE, [], passed=["unrelated-pass"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(comparison.resolved_failures, set())
            self.assertEqual(len(comparison.missing_cases), 5)
            self.assertEqual(
                comparison.unknown_cases,
                {f"{QUERY_SUITE}#unrelated-pass", f"{UPDATE_SUITE}#unrelated-pass"},
            )

    def test_skipped_baseline_case_is_not_marked_resolved(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(
                reports,
                QUERY_SUITE,
                [],
                passed=["tests()[2]"],
                skipped=["tests()[1]", "tests()[3]"],
            )
            self.write_report(reports, UPDATE_SUITE, [], passed=["tests()[1]", "tests()[2]"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertNotIn(f"{QUERY_SUITE}#tests()[1]", comparison.resolved_failures)
            self.assertIn(f"{QUERY_SUITE}#tests()[1]", comparison.unresolved_failures)
            self.assertIn(f"{QUERY_SUITE}#tests()[1]", comparison.outcome_mismatches)

    def test_materially_partial_suite_report_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(
                reports,
                QUERY_SUITE,
                [],
                passed=["tests()[1]", "tests()[2]"],
                skipped=["tests()[3]"],
                reported_tests=100,
            )
            self.write_report(
                reports,
                UPDATE_SUITE,
                [],
                passed=["tests()[1]", "tests()[2]"],
                reported_tests=100,
            )

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(len(comparison.count_mismatches), 2)

    def test_new_failure_and_unknown_case_fail_even_below_baseline_count(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(
                reports,
                QUERY_SUITE,
                ["tests()[99]"],
                passed=["tests()[1]", "tests()[2]"],
                skipped=["tests()[3]"],
            )
            self.write_report(reports, UPDATE_SUITE, [], passed=["tests()[1]", "tests()[2]"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(comparison.unexpected_failures, {f"{QUERY_SUITE}#tests()[99]"})
            self.assertEqual(comparison.unknown_cases, {f"{QUERY_SUITE}#tests()[99]"})

    def test_missing_required_suite_report_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, QUERY_SUITE, [], passed=["tests()[1]", "tests()[2]", "tests()[3]"])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(comparison.missing_suites, {UPDATE_SUITE})
            self.assertEqual(comparison.unexecuted_suites, {UPDATE_SUITE})

    def test_duplicate_and_contradictory_reports_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(
                reports,
                QUERY_SUITE,
                [],
                passed=["tests()[1]"],
                report_name="TEST-query-first.xml",
            )
            self.write_report(
                reports,
                QUERY_SUITE,
                ["tests()[1]"],
                passed=[],
                report_name="TEST-query-second.xml",
            )
            self.write_report(reports, QUERY_SUITE, [], passed=["tests()[2]"], skipped=["tests()[3]"])
            self.write_report(reports, UPDATE_SUITE, [], passed=["tests()[1]", "tests()[2]"])

            observation = self.module.inspect_reports([reports])
            comparison = self.module.compare(baseline, observation)

            self.assertFalse(comparison.passed)
            self.assertEqual(comparison.duplicate_cases, {f"{QUERY_SUITE}#tests()[1]"})
            self.assertEqual(comparison.contradictory_cases, {f"{QUERY_SUITE}#tests()[1]"})
            self.assertNotIn(f"{QUERY_SUITE}#tests()[1]", comparison.resolved_failures)

    def test_nested_namespaced_report_assigns_each_case_to_its_owning_suite(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            reports.mkdir()
            query_cases = "".join(
                (
                    f'<testcase classname="{QUERY_SUITE}" name="tests()[{index}]" time="0.001">'
                    + ("<skipped message=\"not executed\" />" if index == 3 else "")
                    + "</testcase>"
                )
                for index in (1, 2, 3)
            )
            # Keep the aggregate suite separate from its child suites. Each testcase
            # must be attributed once, even when the XML namespace is present.
            report = dedent(
                f"""\
                <testsuite xmlns="http://surefire.junit.org" name="aggregate" tests="5" failures="0"
                           errors="0" skipped="1" time="0.001">
                  <testsuite name="{QUERY_SUITE}" tests="3" failures="0" errors="0" skipped="1">
                    {query_cases}
                  </testsuite>
                  <testsuite name="{UPDATE_SUITE}" tests="2" failures="0" errors="0" skipped="0">
                    <testcase classname="{UPDATE_SUITE}" name="tests()[1]" time="0.001" />
                    <testcase classname="{UPDATE_SUITE}" name="tests()[2]" time="0.001" />
                  </testsuite>
                </testsuite>
                """
            )
            (reports / "TEST-nested.xml").write_text(report, encoding="utf-8")

            observation = self.module.inspect_reports([reports])
            comparison = self.module.compare(baseline, observation)

            self.assertTrue(comparison.passed)
            self.assertEqual(len(observation.outcomes), 5)
            self.assertEqual(observation.duplicate_cases, frozenset())
            self.assertEqual(observation.case_suites[f"{QUERY_SUITE}#tests()[1]"], frozenset({QUERY_SUITE}))

    def test_stale_aggregate_count_for_required_descendants_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            reports.mkdir()
            query_cases = "".join(
                f'<testcase classname="{QUERY_SUITE}" name="tests()[{index}]" time="0.001">'
                + ("<skipped message=\"not executed\" />" if index == 3 else "")
                + "</testcase>"
                for index in (1, 2, 3)
            )
            report = dedent(
                f"""\
                <testsuite name="aggregate" tests="4" failures="0" errors="0" skipped="1" time="0.001">
                  <testsuite name="{QUERY_SUITE}" tests="3" failures="0" errors="0" skipped="1">
                    {query_cases}
                  </testsuite>
                  <testsuite name="{UPDATE_SUITE}" tests="2" failures="0" errors="0" skipped="0">
                    <testcase classname="{UPDATE_SUITE}" name="tests()[1]" time="0.001" />
                    <testcase classname="{UPDATE_SUITE}" name="tests()[2]" time="0.001" />
                  </testsuite>
                </testsuite>
                """
            )
            (reports / "TEST-aggregate.xml").write_text(report, encoding="utf-8")

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertTrue(comparison.count_mismatches)

    def test_cli_returns_failure_for_an_unexpected_error(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.write_baseline(root)
            reports = root / "failsafe-reports"
            self.write_report(
                reports,
                QUERY_SUITE,
                [],
                errors=["tests()[99]"],
                passed=["tests()[1]", "tests()[2]"],
                skipped=["tests()[3]"],
            )
            self.write_report(reports, UPDATE_SUITE, [], passed=["tests()[1]", "tests()[2]"])

            result = subprocess.run(
                [sys.executable, str(SCRIPT), "--baseline", str(baseline), str(reports)],
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 1, result.stdout)
            self.assertIn("LMDB compliance baseline gate: FAIL", result.stdout)
            self.assertIn(f"{QUERY_SUITE}#tests()[99]", result.stdout)

    def test_schema_v2_requires_a_complete_expected_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            path = root / "baseline.json"
            path.write_text(
                json.dumps(
                    {
                        "schemaVersion": 2,
                        "module": "compliance/sparql",
                        "requiredSuites": [QUERY_SUITE, UPDATE_SUITE],
                        "requiredTestCases": {QUERY_SUITE: [f"{QUERY_SUITE}#tests()[1]"]},
                        "failures": [],
                        "allowedSkips": [],
                    }
                ),
                encoding="utf-8",
            )

            with self.assertRaises(self.module.BaselineGateError):
                self.module.load_baseline(path)

    def test_checked_in_baseline_preserves_historical_source_and_current_allowlist(self) -> None:
        raw_payload = json.loads(CHECKED_IN_BASELINE.read_text(encoding="utf-8"))
        baseline = self.module.load_baseline(CHECKED_IN_BASELINE)

        self.assertEqual(baseline.module, "compliance/sparql")
        self.assertEqual(len(baseline.required_suites), 5)
        self.assertEqual(len(baseline.allowed_failures), 23)
        self.assertTrue(all(failure.startswith("org.eclipse.rdf4j.sail.lmdb.") for failure in baseline.allowed_failures))
        self.assertEqual(raw_payload["source"]["summary"], "504 tests, 4 failures, 0 errors, 0 skipped")
        self.assertTrue(raw_payload["source"]["workingTreeReviewChanges"])
        self.assertEqual(
            raw_payload["source"]["provenanceNote"],
            "This inventory was generated from the shared working tree with uncommitted review changes. "
            "The source.head value identifies the branch base only; it does not describe a bare-HEAD run.",
        )
        self.assertEqual(
            raw_payload["source"]["historical"]["summary"],
            "2648 tests, 24 failures, 0 errors, 3 skipped",
        )
        self.assertEqual(raw_payload["source"]["historical"]["allowlistCount"], 23)
        self.assertEqual(len(raw_payload["failures"]), 23)
        self.assertEqual(raw_payload["allowedSkips"], [])
        self.assertEqual(baseline.schema_version, 2)
        self.assertIsNone(baseline.manifest_error)
        self.assertEqual(
            {suite: len(cases) for suite, cases in raw_payload["requiredTestCases"].items()},
            {
                "org.eclipse.rdf4j.sail.lmdb.LmbdSPARQL12QueryComplianceTest": 66,
                "org.eclipse.rdf4j.sail.lmdb.LmbdSPARQL12UpdateComplianceTest": 3,
                "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest": 90,
                "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQL11QueryComplianceTest": 176,
                "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest": 169,
            },
        )
        self.assertEqual(len(baseline.all_expected_cases), 504)
        expected_legacy_descriptions = {
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQL11QueryComplianceTest#tests()[31]": (
                "bind10 - BIND scoping - variable in filter not in scope"
            ),
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQL11QueryComplianceTest#tests()[35]": (
                "Post-query VALUES with 2 object variables and 1 row"
            ),
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[13]": "INSERT 01",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[35]": "DELETE INSERT 1",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[36]": "DELETE INSERT 1b",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[37]": "DELETE INSERT 1c",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[38]": "DELETE INSERT 2",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[39]": "DELETE INSERT 4",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[40]": "DELETE INSERT 4b",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[41]": "DELETE INSERT 5b",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[43]": "Simple DELETE WHERE 1",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[47]": "Graph-specific DELETE WHERE 1",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[49]": "Simple DELETE 1",
            "org.eclipse.rdf4j.sail.lmdb.LmdbPARQL11UpdateComplianceTest#getTestData()[53]": "Graph-specific DELETE 1",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#defaultGraph()[1]": "DefaultGraphTest.testSesameNilAsGraph",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#filterScopeTests()[1]": "FilterScopeTest.testScope1",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#filterScopeTests()[5]": "FilterScopeTest.testScope3",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#bind()[8]": "BindTest.testBindScope",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#minus()[1]": "MinusTest.testScopingOfFilterInMinus",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#aggregate()[21]": "AggregateTest.testSES2361UndefCountWildcard",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#builtinFunction()[20]": "BuiltinFunctionTest.testSES869ValueOfNow",
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#minusScope()[12]": (
                "SparqlMinusScopingTests.T15_rhs_filter_referencing_outer_var_is_unbound_and_ignored"
            ),
            "org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest#minusScope()[13]": (
                "SparqlMinusScopingTests.T16_rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset"
            ),
        }
        self.assertEqual(
            {entry["legacyId"]: entry["description"] for entry in raw_payload["failures"]},
            expected_legacy_descriptions,
        )

    @staticmethod
    def write_baseline(root: Path) -> Path:
        path = root / "baseline.json"
        path.write_text(
            json.dumps(
                {
                    "schemaVersion": 2,
                    "module": "compliance/sparql",
                    "requiredSuites": [QUERY_SUITE, UPDATE_SUITE],
                    "requiredTestCases": {
                        QUERY_SUITE: [
                            f"{QUERY_SUITE}#tests()[1]",
                            f"{QUERY_SUITE}#tests()[2]",
                            f"{QUERY_SUITE}#tests()[3]",
                        ],
                        UPDATE_SUITE: [
                            f"{UPDATE_SUITE}#tests()[1]",
                            f"{UPDATE_SUITE}#tests()[2]",
                        ],
                    },
                    "failures": [
                        {"id": f"{QUERY_SUITE}#tests()[1]", "description": "known one"},
                        {"id": f"{QUERY_SUITE}#tests()[2]", "description": "known two"},
                    ],
                    "allowedSkips": [
                        {"id": f"{QUERY_SUITE}#tests()[3]", "description": "known unavailable case"},
                    ],
                }
            ),
            encoding="utf-8",
        )
        return path

    @staticmethod
    def write_report(
        directory: Path,
        suite: str,
        failures: list[str],
        *,
        errors: list[str] | None = None,
        passed: list[str] | None = None,
        skipped: list[str] | None = None,
        reported_tests: int | None = None,
        report_name: str | None = None,
    ) -> None:
        errors = errors or []
        passed = [] if passed is None else passed
        skipped = skipped or []
        directory.mkdir(parents=True, exist_ok=True)
        escaped_suite = escape(suite)
        cases = [
            *(
                f'<testcase classname="{escaped_suite}" name="{escape(name)}" time="0.001" />'
                for name in passed
            ),
            *(
                f'<testcase classname="{escaped_suite}" name="{escape(name)}" time="0.001">'
                '<failure message="known failure" />'
                "</testcase>"
                for name in failures
            ),
            *(
                f'<testcase classname="{escaped_suite}" name="{escape(name)}" time="0.001">'
                '<error message="unexpected error" />'
                "</testcase>"
                for name in errors
            ),
            *(
                f'<testcase classname="{escaped_suite}" name="{escape(name)}" time="0.001">'
                '<skipped message="not executed" />'
                "</testcase>"
                for name in skipped
            ),
        ]
        test_count = len(cases) if reported_tests is None else reported_tests
        report = dedent(
            f"""\
            <testsuite name="{escaped_suite}" tests="{test_count}" failures="{len(failures)}"
                       errors="{len(errors)}" skipped="{len(skipped)}" time="0.001">
              {''.join(cases)}
            </testsuite>
            """
        )
        file_name = report_name or f"TEST-{suite}.xml"
        (directory / file_name).write_text(report, encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
