#!/usr/bin/env python3

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from textwrap import dedent

REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT = REPO_ROOT / "scripts" / "three-tier-report.py"


def jmh_output(rows: str, parameter_columns: str = "(regime)") -> str:
    return dedent(
        f"""\
        # JMH version: 1.37
        # Run complete. Total time: 00:01:00

        Benchmark                                {parameter_columns}  Mode  Cnt   Score    Error  Units
        {rows}
        """
    )


class ThreeTierReportTest(unittest.TestCase):

    def run_script(self, files: list[Path], *extra: str) -> subprocess.CompletedProcess:
        return subprocess.run(
            [sys.executable, str(SCRIPT), *[str(path) for path in files], *extra],
            capture_output=True,
            text=True,
            check=False,
        )

    def write(self, directory: Path, name: str, content: str) -> Path:
        path = directory / name
        path.write_text(content, encoding="utf-8")
        return path

    def assert_summary(self, stdout: str, **counts: int) -> None:
        """Asserts the verdict tally on the summary line, which is the script's stable contract."""
        summary = next((line for line in stdout.splitlines() if line.startswith("cells:")), None)
        self.assertIsNotNone(summary, f"no summary line in:\n{stdout}")
        for name, count in counts.items():
            self.assertIn(f"{name.upper()}: {count}", summary)

    def test_reports_ok_when_each_higher_tier_is_strictly_faster(self) -> None:
        rows = "\n".join(
            [
                "ThreeTierParityBenchmark.cycle3       lmdb  avgt    3  10.000 ±  0.100  ms/op",
                "ThreeTierParityBenchmark.cycle3  adjacency  avgt    3   5.000 ±  0.100  ms/op",
                "ThreeTierParityBenchmark.cycle3     janino  avgt    3   1.000 ±  0.100  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(rows))
            result = self.run_script([first, second], "--fail-on-inverted")

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("cycle3", result.stdout)
        self.assertIn("no inverted cells", result.stdout)
        self.assertNotIn("unpaired", result.stdout)
        self.assert_summary(result.stdout, ok=2, overlap=0, inverted=0)

    def test_flags_an_inverted_cell_and_fails_when_asked(self) -> None:
        rows = "\n".join(
            [
                "ThreeTierParityBenchmark.pointLookupOut       lmdb  avgt    3   1.000 ±  0.010  ms/op",
                "ThreeTierParityBenchmark.pointLookupOut  adjacency  avgt    3   0.500 ±  0.010  ms/op",
                "ThreeTierParityBenchmark.pointLookupOut     janino  avgt    3   4.000 ±  0.010  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(rows))
            result = self.run_script([first, second], "--fail-on-inverted")

        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("INVERTED CELLS (1)", result.stdout)
        self.assertIn("pointLookupOut  janino<both", result.stdout)

    def test_overlapping_intervals_are_not_a_verdict(self) -> None:
        rows = "\n".join(
            [
                "ThreeTierParityBenchmark.nodeEdgeDump       lmdb  avgt    3   1.000 ±  0.200  ms/op",
                "ThreeTierParityBenchmark.nodeEdgeDump  adjacency  avgt    3   0.950 ±  0.200  ms/op",
                "ThreeTierParityBenchmark.nodeEdgeDump     janino  avgt    3   1.050 ±  0.200  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(rows))
            result = self.run_script([first, second])

        self.assertEqual(0, result.returncode, result.stderr)
        self.assert_summary(result.stdout, ok=0, overlap=2, inverted=0)
        self.assertIn("no inverted cells", result.stdout)

    def test_widens_the_interval_across_paired_runs(self) -> None:
        """A cell that looks disjoint in one run but not across two must not earn a verdict."""
        first_rows = "\n".join(
            [
                "ThreeTierParityBenchmark.starJoin       lmdb  avgt    3  10.000 ±  0.100  ms/op",
                "ThreeTierParityBenchmark.starJoin  adjacency  avgt    3   9.000 ±  0.100  ms/op",
            ]
        )
        second_rows = "\n".join(
            [
                "ThreeTierParityBenchmark.starJoin       lmdb  avgt    3   9.000 ±  0.100  ms/op",
                "ThreeTierParityBenchmark.starJoin  adjacency  avgt    3  10.000 ±  0.100  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(first_rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(second_rows))
            single = self.run_script([first])
            paired = self.run_script([first, second])

        self.assertIn("OK", single.stdout)
        self.assertIn("OVERLAP", paired.stdout)
        self.assertIn("missing:janino", paired.stdout)

    def test_infers_the_regime_from_the_file_name_when_there_is_no_param_column(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            files = []
            for regime, score in (("lmdb", "8.000"), ("adjacency", "4.000"), ("janino", "2.000")):
                row = f"ThreeTierParityBenchmark.cycle4  avgt    3   {score} ±  0.050  ms/op"
                files.append(self.write(tmp, f"tier-m0-{regime}-r1.txt", jmh_output(row, parameter_columns="")))
                files.append(self.write(tmp, f"tier-m0-{regime}-r2.txt", jmh_output(row, parameter_columns="")))
            result = self.run_script(files, "--fail-on-inverted")

        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assert_summary(result.stdout, ok=2, inverted=0)

    def test_normalises_units_before_comparing(self) -> None:
        rows = "\n".join(
            [
                "ThreeTierParityBenchmark.doublyBoundProbe       lmdb  avgt    3   1.000 ±  0.001  ms/op",
                "ThreeTierParityBenchmark.doublyBoundProbe  adjacency  avgt    3  10.000 ±  0.001  us/op",
                "ThreeTierParityBenchmark.doublyBoundProbe     janino  avgt    3   5.000 ±  0.001  us/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(rows))
            result = self.run_script([first, second], "--fail-on-inverted")

        self.assertEqual(0, result.returncode, result.stdout)
        self.assert_summary(result.stdout, ok=2, inverted=0)
        self.assertIn("0.010", result.stdout)

    def test_flags_wide_error_bars_on_slow_cells_as_noisy(self) -> None:
        rows = "\n".join(
            [
                "ThreeTierParityBenchmark.themeAnalyticsQ5       lmdb  avgt    3  800.000 ± 400.000  ms/op",
                "ThreeTierParityBenchmark.themeAnalyticsQ5  adjacency  avgt    3  700.000 ± 350.000  ms/op",
                "ThreeTierParityBenchmark.themeAnalyticsQ5     janino  avgt    3  600.000 ± 300.000  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(rows))
            result = self.run_script([first, second])

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("noisy:lmdb", result.stdout)
        self.assertIn("noisy:janino", result.stdout)

    def test_keeps_parameterised_cells_apart(self) -> None:
        rows = "\n".join(
            [
                "ThemeQueryBenchmark.executeQuery  ANALYTICS   5       lmdb  avgt    3  800.000 ±  1.000  ms/op",
                "ThemeQueryBenchmark.executeQuery  ANALYTICS   5  adjacency  avgt    3  700.000 ±  1.000  ms/op",
                "ThemeQueryBenchmark.executeQuery  ANALYTICS   8       lmdb  avgt    3  200.000 ±  1.000  ms/op",
                "ThemeQueryBenchmark.executeQuery  ANALYTICS   8  adjacency  avgt    3  100.000 ±  1.000  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(
                tmp, "tier-m0-r1.txt", jmh_output(rows, parameter_columns="(themeName)  (z_queryIndex)  (regime)")
            )
            result = self.run_script([first])

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("z_queryIndex=5", result.stdout)
        self.assertIn("z_queryIndex=8", result.stdout)
        self.assertEqual(2, len([line for line in result.stdout.splitlines() if "executeQuery[" in line]))

    def test_accepts_rows_that_have_no_error_column(self) -> None:
        """JMH omits the whole '± Error' pair below three iterations; such a run must still parse, and be warned about."""
        rows = "\n".join(
            [
                "ThreeTierParityBenchmark.pointLookupOut       lmdb  avgt    2  0.024  ms/op",
                "ThreeTierParityBenchmark.pointLookupOut  adjacency  avgt    2  0.018  ms/op",
                "ThreeTierParityBenchmark.pointLookupOut     janino  avgt    2  0.012  ms/op",
            ]
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            first = self.write(tmp, "tier-m0-r1.txt", jmh_output(rows))
            second = self.write(tmp, "tier-m0-r2.txt", jmh_output(rows))
            result = self.run_script([first, second], "--fail-on-inverted")

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_summary(result.stdout, ok=2, inverted=0)
        self.assertIn("has no error bar", result.stderr)

    def test_reports_an_error_when_a_file_has_no_result_table(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            empty = self.write(tmp, "tier-m0-lmdb-r1.txt", "# JMH version: 1.37\n<forked VM failed>\n")
            result = self.run_script([empty])

        self.assertEqual(2, result.returncode)
        self.assertIn("no JMH result table found", result.stderr)


if __name__ == "__main__":
    unittest.main()
