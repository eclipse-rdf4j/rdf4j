#!/usr/bin/env python3

from __future__ import annotations

import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CHECKER = REPO_ROOT / "scripts" / "checkCopyrightPresent.sh"
VALID_HEADER = """/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
"""


class CheckCopyrightPresentTest(unittest.TestCase):

    def test_reports_checked_directories_not_pom_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            checker = self._copy_checker(root)
            good_dir = self._write_module(root, "good", VALID_HEADER)
            bad_dir = self._write_module(
                root,
                "bad",
                VALID_HEADER.replace("/org/documents/", "/documents/"),
            )
            unchecked_dir = root / "pom-only"
            unchecked_dir.mkdir()
            (unchecked_dir / "pom.xml").touch()

            result = self._run_checker(checker)

            self.assertEqual(result.returncode, 1, result.stdout)
            self.assertIn(f"✓ {good_dir}", result.stdout)
            self.assertNotIn("/pom.xml", result.stdout)
            self.assertNotIn(f"✓ {bad_dir}", result.stdout)
            self.assertNotIn(f"✓ {unchecked_dir}", result.stdout)
            self.assertIn(
                f"Invalid copyright header: {bad_dir}/src/main/java/Example.java",
                result.stdout,
            )

    def test_reports_each_successfully_checked_directory(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            checker = self._copy_checker(root)
            first_dir = self._write_module(root, "first", VALID_HEADER)
            second_dir = self._write_module(root, "second", VALID_HEADER)

            result = self._run_checker(checker)

            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertEqual(result.stdout.count(f"✓ {first_dir}"), 1, result.stdout)
            self.assertEqual(result.stdout.count(f"✓ {second_dir}"), 1, result.stdout)
            self.assertNotIn("/pom.xml", result.stdout)
            self.assertIn("All files have valid copyright headers and SPDX lines.", result.stdout)

    @staticmethod
    def _copy_checker(root: Path) -> Path:
        scripts_dir = root / "scripts"
        scripts_dir.mkdir()
        checker = scripts_dir / CHECKER.name
        shutil.copy2(CHECKER, checker)
        return checker

    @staticmethod
    def _write_module(root: Path, name: str, header: str) -> Path:
        module_dir = root / name
        source_dir = module_dir / "src" / "main" / "java"
        source_dir.mkdir(parents=True)
        (module_dir / "pom.xml").touch()
        (source_dir / "Example.java").write_text(
            f"{header}package example;\n\nclass Example {{}}\n",
            encoding="utf-8",
        )
        return module_dir

    @staticmethod
    def _run_checker(checker: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["bash", str(checker)],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            check=False,
        )


if __name__ == "__main__":
    unittest.main()
