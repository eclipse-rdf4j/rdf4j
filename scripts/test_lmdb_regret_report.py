#!/usr/bin/env python3
"""Regression tests for reporting every chosen policy outcome, including censored failures."""
import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest


SPEC = importlib.util.spec_from_file_location("lmdb_regret_report", Path(__file__).with_name("lmdb-regret-report.py"))
REPORT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPORT)


class LmdbRegretReportTest(unittest.TestCase):
    def report(self, rows):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "regret.tsv"
            path.write_text("\n".join(rows) + "\n", encoding="utf-8")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                status = REPORT.main([str(path)])
            self.assertEqual(0, status)
            return output.getvalue()

    @staticmethod
    def row(strategy, chosen, median, expected=100, note=""):
        return "\t".join(("query", "spoc,posc", "lmdbOnly", "row dispatch", strategy,
                          "chosen" if chosen else "-", str(median), str(median), "12", str(expected),
                          "0", "1000000", note, "1"))

    def test_failed_chosen_strategy_is_counted_against_completed_rival(self):
        result = self.report([self.row("nestedLoop", True, "NaN", note="failed: timed out"),
                              self.row("irKernel", False, 100)])
        self.assertIn("decisions=1", result)
        self.assertIn("chosenWithin5pctOfFastest=0", result)
        self.assertIn("chosenFailed=1", result)
        self.assertRegex(result, r"inf[x ]+.*chose nestedLoop, fastest irKernel")

    def test_all_failed_measurements_remain_visible(self):
        result = self.report([self.row("nestedLoop", True, "NaN", note="failed: timed out"),
                              self.row("irKernel", False, "NaN", note="declined: unavailable")])
        self.assertIn("query [spoc,posc lmdbOnly] row dispatch", result)
        self.assertIn("decisions=1", result)
        self.assertIn("chosenFailed=1", result)
        self.assertIn("no completed strategy", result)
        self.assertIn("failed: timed out", result)

    def test_declined_rival_does_not_invalidate_completed_chosen_strategy(self):
        result = self.report([self.row("nestedLoop", True, 100),
                              self.row("irKernel", False, "NaN", note="declined: unavailable")])
        self.assertIn("decisions=1", result)
        self.assertIn("chosenWithin5pctOfFastest=1", result)
        self.assertIn("chosenFailed=0", result)

    def test_equal_quotes_do_not_claim_an_order_in_either_input_order(self):
        rows = [self.row("slow", True, 200, expected=100), self.row("fast", False, 100, expected=100)]
        for ordering in (rows, list(reversed(rows))):
            with self.subTest(ordering=ordering):
                result = self.report(ordering)
                self.assertIn("quotedOrderAgreesWithMeasured=0/0 pairs", result)

    def test_distinct_quotes_still_compare_order(self):
        result = self.report([self.row("fast", True, 100, expected=50),
                              self.row("slow", False, 200, expected=100)])
        self.assertIn("quotedOrderAgreesWithMeasured=1/1 pairs", result)

    def test_nonfinite_chosen_time_is_censored_not_completed(self):
        result = self.report([self.row("nestedLoop", True, "Infinity", note="failed: timed out"),
                              self.row("irKernel", False, 100)])
        self.assertIn("chosenFailed=1", result)
        self.assertIn("decisions=1", result)
        self.assertNotIn("nestedLoop                         inf ms", result)

    def test_two_rounded_zero_timings_do_not_divide_by_zero(self):
        result = self.report([self.row("fast", True, 0, expected=50),
                              self.row("alsoFast", False, 0, expected=100)])
        self.assertIn("chosenWithin5pctOfFastest=1", result)
        self.assertIn("quotedOrderAgreesWithMeasured=0/0 pairs", result)


    @staticmethod
    def control_row(comparable=False):
        return "\t".join(("case", "nestedLoop", "NaN", "NaN", "NaN", "NaN", "-1",
                          str(comparable).lower(), "30", "5", "Infinity", "[10.0, 20.0]",
                          "[5.0, 0.0]", "[15.0, null]", "", "", "fitEligible=false"))

    def test_control_sidecar_protocol_is_rejected_as_ordinary_regret(self):
        with self.assertRaisesRegex(ValueError, "control sidecar"):
            self.report(["# fitEligible=false sampleOrder=AB/BA A=automatic B=forced-exact-winner",
                         self.control_row()])

    def test_control_column_header_is_rejected_without_protocol_comment(self):
        with self.assertRaisesRegex(ValueError, "control sidecar"):
            self.report(["# query\tid\tautomaticMedianNanos\tforcedMedianNanos\tautomaticMinNanos"
                         "\tforcedMinNanos\trows\tcomparable\tquoteExpectedNanos\tquoteLowNanos"
                         "\tquoteHighNanos\tquoteExpectedFeatures\tquoteLowFeatures\tquoteHighFeatures"
                         "\tautomaticNote\tforcedNote\tnote", self.control_row()])

    def test_headerless_control_rows_are_not_guessed_as_completed_measurements(self):
        for comparable in (False, True):
            with self.subTest(comparable=comparable):
                with self.assertRaisesRegex(ValueError, "regret schema"):
                    self.report([self.control_row(comparable)])

    def test_unknown_quote_high_preserves_a_finite_completed_runtime(self):
        cells = self.row("nestedLoop", True, 100).split("\t")
        cells[11] = "Infinity"
        result = self.report(["\t".join(cells)])
        self.assertIn("decisions=1", result)
        self.assertIn("chosenWithin5pctOfFastest=1", result)
        self.assertIn("chosenFailed=0", result)


if __name__ == "__main__":
    unittest.main()
