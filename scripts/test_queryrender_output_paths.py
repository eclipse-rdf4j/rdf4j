#!/usr/bin/env python3

from __future__ import annotations

import re
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
QUERYRENDER_TEST_ROOT = (
    REPOSITORY_ROOT
    / "core"
    / "queryrender"
    / "src"
    / "test"
    / "java"
    / "org"
    / "eclipse"
    / "rdf4j"
    / "queryrender"
)
DIAGNOSTIC_WRITERS = (
    "TupleExprIRRendererTest.java",
    "TupleExprIRRendererExplorationTest.java",
    "BracesEffectTest.java",
    "TupleExprUnionPathScopeShapeTest.java",
)
OUTPUT_DIRECTORY_RESOLUTION = re.compile(
    r"private static final Path SUREFIRE_DIR\s*=\s*Paths\.get\(\s*"
    r"System\.getProperty\(TEST_OUTPUT_DIRECTORY_PROPERTY,\s*\"target\"\),\s*"
    r"\"surefire-reports\"\s*\);",
    re.MULTILINE,
)


class QueryRenderOutputPathContractTest(unittest.TestCase):

    def test_diagnostic_writers_follow_module_test_output_directory(self) -> None:
        for file_name in DIAGNOSTIC_WRITERS:
            with self.subTest(file=file_name):
                source = (QUERYRENDER_TEST_ROOT / file_name).read_text(encoding="utf-8")
                self.assertTrue(
                    'TEST_OUTPUT_DIRECTORY_PROPERTY = "rdf4j.test.outputDirectory"'
                    in source,
                    f"{file_name} must honor the propagated module-specific output directory",
                )
                self.assertRegex(
                    source,
                    OUTPUT_DIRECTORY_RESOLUTION,
                    f"{file_name} must retain target as the non-workspace fallback",
                )
                self.assertNotIn('Paths.get("target", "surefire-reports")', source)
                self.assertNotIn(
                    'Paths.get("core", "queryrender", "target", "surefire-reports")',
                    source,
                )

    def test_renderer_writer_and_purger_share_the_resolved_directory(self) -> None:
        source = (QUERYRENDER_TEST_ROOT / "TupleExprIRRendererTest.java").read_text(
            encoding="utf-8"
        )

        self.assertTrue(
            "Path dir = SUREFIRE_DIR;" in source,
            "the diagnostic writer must use the same resolved directory as cleanup",
        )
        self.assertTrue(
            "SUREFIRE_DIR.resolve(base +" in source,
            "the per-test purge must use the same resolved diagnostic directory",
        )


if __name__ == "__main__":
    unittest.main()
