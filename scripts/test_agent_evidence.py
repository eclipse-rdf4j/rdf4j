#!/usr/bin/env python3

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from textwrap import dedent


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT = REPO_ROOT / "scripts" / "agent-evidence.py"


class AgentEvidenceTest(unittest.TestCase):

    def test_prints_compact_failure_evidence_from_xml_reports(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            report_dir = tmp / "module-a" / "target" / "surefire-reports"
            report_dir.mkdir(parents=True)
            report = report_dir / "TEST-ExampleTest.xml"
            report.write_text(
                dedent(
                    """\
                    <testsuite name="ExampleTest" tests="2" failures="1" errors="0" skipped="0" time="0.123">
                      <testcase classname="ExampleTest" name="fails" time="0.001">
                        <failure message="expected short failure">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA</failure>
                      </testcase>
                    </testsuite>
                    """
                ),
                encoding="utf-8",
            )
            full_log = tmp / "logs" / "mvnf" / "verify.log"
            full_log.parent.mkdir(parents=True)
            full_log.write_text("full verify output\n", encoding="utf-8")

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--root",
                    str(tmp),
                    "--command",
                    "python3 .codex/skills/mvnf/scripts/mvnf.py ExampleTest#fails",
                    "--log",
                    str(full_log),
                    str(report_dir),
                ],
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            lines = result.stdout.strip().splitlines()
            self.assertGreaterEqual(len(lines), 4, result.stdout)
            self.assertLessEqual(len(lines), 8, result.stdout)
            self.assertIn("Evidence:", lines[0])
            self.assertIn("Command: python3 .codex/skills/mvnf/scripts/mvnf.py ExampleTest#fails", result.stdout)
            self.assertIn("Reports: module-a/target/surefire-reports/TEST-ExampleTest.xml", result.stdout)
            self.assertIn("Summary: tests=2, failures=1, errors=0, skipped=0, time=0.123s", result.stdout)
            self.assertIn("Failure: ExampleTest.fails: failure: expected short failure", result.stdout)
            self.assertIn("Log: logs/mvnf/verify.log", result.stdout)
            self.assertNotIn("A" * 80, result.stdout)


if __name__ == "__main__":
    unittest.main()
