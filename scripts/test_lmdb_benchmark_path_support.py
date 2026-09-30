#!/usr/bin/env python3

from __future__ import annotations

import re
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
BENCHMARK_ROOT = (
    REPOSITORY_ROOT
    / "core"
    / "sail"
    / "lmdb"
    / "src"
    / "test"
    / "java"
    / "org"
    / "eclipse"
    / "rdf4j"
    / "sail"
    / "lmdb"
    / "benchmark"
)
PATH_SUPPORT = BENCHMARK_ROOT / "BenchmarkPathSupport.java"
QUERY_PLAN_CAPTURE = (
    REPOSITORY_ROOT
    / "testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/QueryPlanCapture.java"
)
QUERY_PLAN_CAPTURE_CONTEXT = (
    REPOSITORY_ROOT
    / "testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/QueryPlanCaptureContext.java"
)
STORE_ROOTS = {
    "ThemeQueryHexaBenchmark.java": "lmdb-theme-query-hexa-benchmark",
    "ThemeDistinctPredicateBenchmark.java": "lmdb-theme-distinct-predicate-benchmark",
    "ThemeQueryPlanRunBenchmark.java": "lmdb-theme-query-benchmark",
    "ThemeQueryExplain.java": "lmdb-theme-query-benchmark",
    "ThemeQueryBenchmark.java": "lmdb-theme-query-benchmark",
}


def source(file_name: str) -> str:
    return (BENCHMARK_ROOT / file_name).read_text(encoding="utf-8")


def normalized(java_source: str) -> str:
    return " ".join(java_source.split()).replace(" .", ".")


class LmdbBenchmarkPathSupportContractTest(unittest.TestCase):

    def test_support_prioritizes_output_property_then_preserves_fallback(self) -> None:
        self.assertTrue(
            PATH_SUPPORT.exists(),
            "BenchmarkPathSupport must centralize LMDB benchmark path selection",
        )
        support = PATH_SUPPORT.read_text(encoding="utf-8")

        self.assertTrue(
            "final class BenchmarkPathSupport" in support,
            "BenchmarkPathSupport must remain package-private",
        )
        self.assertTrue(
            'TEST_OUTPUT_DIRECTORY_PROPERTY = "rdf4j.test.outputDirectory"'
            in support,
            "the propagated module output directory property must be authoritative",
        )
        self.assertRegex(
            support,
            re.compile(
                r"String configuredOutputDirectory\s*=\s*System\.getProperty\("
                r"TEST_OUTPUT_DIRECTORY_PROPERTY\);\s*"
                r"if \(configuredOutputDirectory != null\s*"
                r"&& !configuredOutputDirectory\.isBlank\(\)\) \{\s*"
                r"return Path\.of\(configuredOutputDirectory\);\s*\}",
                re.MULTILINE,
            ),
            "nonblank rdf4j.test.outputDirectory must win before any cwd heuristic",
        )
        self.assertTrue(
            'Path.of("target")' in support
            and 'Path.of("core", "sail", "lmdb", "target")' in support,
            "the fallback must retain module-local and IDE target choices",
        )

    def test_all_store_roots_delegate_to_path_support(self) -> None:
        violations: list[str] = []
        for file_name, directory_name in STORE_ROOTS.items():
            java_source = source(file_name)
            expected = (
                f'BenchmarkPathSupport.resolveTarget("{directory_name}").toFile()'
            )
            if expected not in normalized(java_source):
                violations.append(file_name)
            if "TARGET_DIRECTORY_ROOT" in java_source:
                violations.append(f"{file_name}:duplicate-heuristic")

        self.assertFalse(
            violations,
            "benchmark store roots not centralized: " + ", ".join(violations),
        )

    def test_reports_analysis_and_smoke_expectations_delegate(self) -> None:
        forced_report = normalized(source("LmdbForcedMedicalQ9PerformanceIT.java"))
        regression = normalized(source("LmdbRegressionAnalysisSupport.java"))
        smoke = normalized(source("ThemeQueryBenchmarkSmokeIT.java"))
        violations: list[str] = []

        if (
            'BenchmarkPathSupport.resolveTarget("forced-medical-q9-performance.txt")'
            not in forced_report
        ):
            violations.append("forced-medical-report")
        if (
            'BenchmarkPathSupport.resolveTarget("lmdb-regression-plan-capture")'
            not in regression
        ):
            violations.append("regression-analysis-output")
        if (
            'BenchmarkPathSupport.resolveTarget("lmdb-theme-query-benchmark")'
            not in smoke
        ):
            violations.append("smoke-store-cleanup")
        if (
            "BenchmarkPathSupport.resolveTarget( "
            '"lmdb-theme-query-benchmark", "complete", "join-estimator.rjes")'
            not in smoke
        ):
            violations.append("smoke-estimator-expectation")
        for name, java_source in (
            ("forced-medical-report", forced_report),
            ("regression-analysis-output", regression),
            ("smoke-expectations", smoke),
        ):
            if 'Path.of("target"' in java_source:
                violations.append(f"{name}:fixed-target")

        self.assertFalse(
            violations,
            "benchmark output consumers not centralized: " + ", ".join(violations),
        )

    def test_persistent_override_remains_authoritative(self) -> None:
        estimator_support = normalized(source("BenchmarkJoinEstimatorSupport.java"))

        self.assertTrue(
            "String configuredRoot = System.getProperty( "
            "PERSISTENT_THEME_REGRESSION_STORE_ROOT);"
            in estimator_support,
            "the explicit persistent-store property must be read without a default",
        )
        self.assertTrue(
            "Path root = configuredRoot == null "
            "? BenchmarkPathSupport.resolveTarget("
            "DEFAULT_PERSISTENT_THEME_REGRESSION_STORE_ROOT) "
            ": Path.of(configuredRoot);"
            in estimator_support,
            "only an absent override may use the module output directory default",
        )
        self.assertGreaterEqual(
            estimator_support.count("persistentThemeRegressionStoreRoot()"),
            3,
            "resolution and deletion checks must share the same root helper",
        )

    def test_query_plan_capture_prefers_explicit_then_workspace_output(self) -> None:
        capture = QUERY_PLAN_CAPTURE.read_text(encoding="utf-8")
        context = QUERY_PLAN_CAPTURE_CONTEXT.read_text(encoding="utf-8")
        explicit_lookup = (
            "System.getProperty(QueryPlanCaptureContext.OUTPUT_DIRECTORY_PROPERTY)"
        )
        workspace_lookup = "System.getProperty(TEST_OUTPUT_DIRECTORY_PROPERTY)"
        build_root_lookup = "System.getProperty(WORKSPACE_BUILD_ROOT_PROPERTY)"

        self.assertIn(
            'TEST_OUTPUT_DIRECTORY_PROPERTY = "rdf4j.test.outputDirectory"',
            capture,
            "plan capture must recognize the propagated module output directory",
        )
        self.assertIn(
            explicit_lookup,
            capture,
            "the dedicated plan-capture output property must remain authoritative",
        )
        self.assertIn(
            workspace_lookup,
            capture,
            "an absent explicit root must fall back to isolated module output",
        )
        self.assertIn(
            'WORKSPACE_BUILD_ROOT_PROPERTY = "rdf4j.build.root"',
            capture,
            "Maven-launched capture outside Surefire must still recognize workspace mode",
        )
        self.assertIn(
            build_root_lookup,
            capture,
            "workspace build root must prevent source-tree output when no test output is propagated",
        )
        if explicit_lookup in capture and workspace_lookup in capture:
            self.assertLess(
                capture.index(explicit_lookup),
                capture.index(workspace_lookup),
                "explicit plan-capture output must be checked before workspace output",
            )
        self.assertIn(
            'ISOLATED_OUTPUT_DIRECTORY = "query-plan-capture"',
            capture,
            "workspace-default snapshots need a distinct directory below module output",
        )
        self.assertIn(
            "Path.of(testOutputDirectory).resolve(ISOLATED_OUTPUT_DIRECTORY)",
            capture,
            "workspace-default snapshots must use the isolated output directory",
        )
        self.assertIn(
            "private Path outputDirectory = QueryPlanCapture.resolveOutputDirectory();",
            context,
            "context defaults and direct capture resolution must share one precedence contract",
        )


if __name__ == "__main__":
    unittest.main()
