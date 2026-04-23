#!/usr/bin/env python3
"""Regression tests for JMH compare parsing edge cases."""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import jmh_compare_core as core


class ParseFileRegressionTest(unittest.TestCase):
    def test_missing_cnt_and_error_values_do_not_shift_score(self) -> None:
        repo_root = SCRIPT_DIR.parents[3]
        result_file = (
            repo_root
            / "core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/results-2026-03-01.md"
        )

        parsed = core.parse_file(result_file, "results-2026-03-01", None, "mtime")
        key = ("ThemeQueryBenchmark.executeQuery", "ENGINEERING", "0", "avgt", "ms/op")

        self.assertIn(key, parsed.score_by_key)
        self.assertAlmostEqual(parsed.score_by_key[key], 224.962, places=3)

    def test_plus_minus_error_rows_keep_score_numeric(self) -> None:
        repo_root = SCRIPT_DIR.parents[3]
        result_file = (
            repo_root
            / "core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/results-2026-03-04.md"
        )

        parsed = core.parse_file(result_file, "results-2026-03-04", None, "mtime")
        key = ("ThemeQueryBenchmark.executeQuery", "SOCIAL_MEDIA", "0", "avgt", "ms/op")

        self.assertIn(key, parsed.score_by_key)
        self.assertAlmostEqual(parsed.score_by_key[key], 31.922, places=3)

    def test_compare_uses_column_names_when_key_order_differs(self) -> None:
        left = "\n".join(
            [
                "Benchmark  (themeName)  (z_queryIndex)  Mode  Score  Units",
                "ThemeQueryBenchmark.executeQuery  MEDICAL_RECORDS  0  avgt  10.0  ms/op",
            ]
        )
        right = "\n".join(
            [
                "Benchmark  Mode  (themeName)  (z_queryIndex)  Units  Score",
                "ThemeQueryBenchmark.executeQuery  avgt  MEDICAL_RECORDS  0  ms/op  20.0",
            ]
        )

        with tempfile.TemporaryDirectory() as tmpdir:
            left_file = Path(tmpdir) / "left.txt"
            right_file = Path(tmpdir) / "right.txt"
            left_file.write_text(left, encoding="utf-8")
            right_file.write_text(right, encoding="utf-8")

            left_parsed = core.parse_file(left_file, "left", None, "mtime")
            right_parsed = core.parse_file(right_file, "right", None, "mtime")
            table = core.build_comparison_table(
                [left_parsed, right_parsed], left_parsed, "all", 0.0, None, "lower"
            )

            self.assertEqual(len(table.rows), 1)
            row = table.rows[0]
            self.assertAlmostEqual(row["Score [left]"], 10.0, places=3)
            self.assertAlmostEqual(row["Score [right]"], 20.0, places=3)
            self.assertAlmostEqual(row["Diff % [right - left]"], 100.0, places=3)


if __name__ == "__main__":
    unittest.main()
