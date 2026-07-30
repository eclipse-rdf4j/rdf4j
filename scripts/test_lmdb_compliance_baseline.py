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


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
SCRIPT = REPOSITORY_ROOT / "scripts" / "check-lmdb-compliance-baseline.py"
CHECKED_IN_BASELINE = (
    REPOSITORY_ROOT / "compliance" / "sparql" / "lmdb-compliance-baseline.json"
)


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
            baseline_path = self.write_baseline(root)
            reports = root / "failsafe-reports"
            self.write_report(reports, "example.LmdbQueryComplianceTest", ["tests()[1]"])
            self.write_report(reports, "example.LmdbUpdateComplianceTest", [])

            baseline = self.module.load_baseline(baseline_path)
            observation = self.module.inspect_reports([reports])
            comparison = self.module.compare(baseline, observation)

            self.assertTrue(comparison.passed)
            self.assertEqual(
                comparison.remaining_failures,
                {"example.LmdbQueryComplianceTest#tests()[1]"},
            )
            self.assertEqual(
                comparison.resolved_failures,
                {"example.LmdbQueryComplianceTest#tests()[2]"},
            )
            self.assertEqual(comparison.unexpected_failures, set())
            self.assertEqual(comparison.missing_suites, set())

    def test_new_failure_fails_even_below_the_baseline_count(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, "example.LmdbQueryComplianceTest", ["tests()[99]"])
            self.write_report(reports, "example.LmdbUpdateComplianceTest", [])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(
                comparison.unexpected_failures,
                {"example.LmdbQueryComplianceTest#tests()[99]"},
            )

    def test_missing_required_suite_report_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.module.load_baseline(self.write_baseline(root))
            reports = root / "failsafe-reports"
            self.write_report(reports, "example.LmdbQueryComplianceTest", [])

            comparison = self.module.compare(baseline, self.module.inspect_reports([reports]))

            self.assertFalse(comparison.passed)
            self.assertEqual(
                comparison.missing_suites,
                {"example.LmdbUpdateComplianceTest"},
            )

    def test_cli_returns_failure_for_an_unexpected_error(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            baseline = self.write_baseline(root)
            reports = root / "failsafe-reports"
            self.write_report(reports, "example.LmdbQueryComplianceTest", [], errors=["tests()[3]"])
            self.write_report(reports, "example.LmdbUpdateComplianceTest", [])

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--baseline",
                    str(baseline),
                    str(reports),
                ],
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 1, result.stdout)
            self.assertIn("LMDB compliance baseline gate: FAIL", result.stdout)
            self.assertIn("example.LmdbQueryComplianceTest#tests()[3]", result.stdout)

    def test_checked_in_baseline_freezes_exactly_twenty_four_failures(self) -> None:
        baseline = self.module.load_baseline(CHECKED_IN_BASELINE)

        self.assertEqual(baseline.module, "compliance/sparql")
        self.assertEqual(len(baseline.required_suites), 5)
        self.assertEqual(len(baseline.allowed_failures), 24)
        self.assertTrue(
            all(failure.startswith("org.eclipse.rdf4j.sail.lmdb.") for failure in baseline.allowed_failures)
        )

    @staticmethod
    def write_baseline(root: Path) -> Path:
        path = root / "baseline.json"
        path.write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "module": "compliance/sparql",
                    "requiredSuites": [
                        "example.LmdbQueryComplianceTest",
                        "example.LmdbUpdateComplianceTest",
                    ],
                    "failures": [
                        {
                            "id": "example.LmdbQueryComplianceTest#tests()[1]",
                            "description": "known one",
                        },
                        {
                            "id": "example.LmdbQueryComplianceTest#tests()[2]",
                            "description": "known two",
                        },
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
    ) -> None:
        errors = errors or []
        directory.mkdir(parents=True, exist_ok=True)
        cases = [
            f'<testcase classname="{suite}" name="passes" time="0.001" />',
            *(
                f'<testcase classname="{suite}" name="{name}" time="0.001">'
                '<failure message="known failure" />'
                "</testcase>"
                for name in failures
            ),
            *(
                f'<testcase classname="{suite}" name="{name}" time="0.001">'
                '<error message="unexpected error" />'
                "</testcase>"
                for name in errors
            ),
        ]
        report = dedent(
            f"""\
            <testsuite name="{suite}" tests="{len(cases)}" failures="{len(failures)}"
                       errors="{len(errors)}" skipped="0" time="0.001">
              {''.join(cases)}
            </testsuite>
            """
        )
        (directory / f"TEST-{suite}.xml").write_text(report, encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
