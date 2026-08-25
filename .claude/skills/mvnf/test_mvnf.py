#!/usr/bin/env python3

from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import textwrap
import time
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parent / "scripts" / "mvnf.py"


class MvnfCommandBoundaryTest(unittest.TestCase):

    def test_unit_selector_skips_failsafe_integration_tests(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo = self._write_minimal_repo(Path(tmp))
            calls = repo / "calls.txt"
            fake_mvn = self._write_fake_mvn(repo, calls)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "ExampleTest#runs",
                    "--mvn",
                    str(fake_mvn),
                    "--retain-logs",
                ],
                cwd=repo,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            verify_args = self._verify_args(calls)
            self.assertIn("-Dtest=ExampleTest#runs", verify_args)
            self.assertIn("-DskipITs", verify_args)
            self.assertNotIn("-Dit.test=ExampleTest#runs", verify_args)

    def test_integration_selector_skips_surefire_unit_tests(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo = self._write_minimal_repo(Path(tmp), class_name="ExampleIT")
            calls = repo / "calls.txt"
            fake_mvn = self._write_fake_mvn(repo, calls)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "ExampleIT#runs",
                    "--it",
                    "--mvn",
                    str(fake_mvn),
                    "--retain-logs",
                ],
                cwd=repo,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            verify_args = self._verify_args(calls)
            self.assertIn("-Dit.test=ExampleIT#runs", verify_args)
            self.assertIn("-PskipUnitTests", verify_args)
            self.assertNotIn("-Dtest=ExampleIT#runs", verify_args)

    def test_passthrough_maven_args_are_appended_to_verify_command(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo = self._write_minimal_repo(Path(tmp))
            calls = repo / "calls.txt"
            fake_mvn = self._write_fake_mvn(repo, calls)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "ExampleTest#runs",
                    "--mvn",
                    str(fake_mvn),
                    "--retain-logs",
                    "--",
                    "-Pjacoco",
                    "-Dfoo=bar",
                ],
                cwd=repo,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            install_args, verify_args = self._all_args(calls)
            self.assertNotIn("-Pjacoco", install_args)
            self.assertNotIn("-Dfoo=bar", install_args)
            self.assertIn("-Dtest=ExampleTest#runs", verify_args)
            self.assertIn("-Pjacoco", verify_args)
            self.assertIn("-Dfoo=bar", verify_args)

    def test_concurrent_repo_run_fails_before_invoking_maven(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo = self._write_minimal_repo(Path(tmp))
            calls = repo / "calls.txt"
            started = repo / "mvn-started"
            fake_mvn = self._write_fake_mvn(repo, calls, started)

            first = subprocess.Popen(
                [
                    sys.executable,
                    str(SCRIPT),
                    "ExampleTest#runs",
                    "--mvn",
                    str(fake_mvn),
                    "--retain-logs",
                ],
                cwd=repo,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
            )
            try:
                self._wait_for_file(started)

                result = subprocess.run(
                    [
                        sys.executable,
                        str(SCRIPT),
                        "ExampleTest#runs",
                        "--mvn",
                        str(fake_mvn),
                        "--retain-logs",
                    ],
                    cwd=repo,
                    text=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    check=False,
                    timeout=10,
                )

                self.assertNotEqual(result.returncode, 0, result.stdout)
                self.assertIn("another mvnf", result.stdout)
                self.assertIn("--allow-concurrent", result.stdout)
                self.assertEqual(1, len(calls.read_text(encoding="utf-8").splitlines()))
            finally:
                first.terminate()
                try:
                    first.communicate(timeout=5)
                except subprocess.TimeoutExpired:
                    first.kill()
                    first.communicate(timeout=5)

    def test_allow_concurrent_overrides_repo_run_guard(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            repo = self._write_minimal_repo(Path(tmp))
            calls = repo / "calls.txt"
            started = repo / "mvn-started"
            fake_mvn = self._write_fake_mvn(repo, calls, started)

            first = subprocess.Popen(
                [
                    sys.executable,
                    str(SCRIPT),
                    "ExampleTest#runs",
                    "--mvn",
                    str(fake_mvn),
                    "--retain-logs",
                ],
                cwd=repo,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
            )
            try:
                self._wait_for_file(started)

                result = subprocess.run(
                    [
                        sys.executable,
                        str(SCRIPT),
                        "ExampleTest#runs",
                        "--mvn",
                        str(fake_mvn),
                        "--retain-logs",
                        "--allow-concurrent",
                    ],
                    cwd=repo,
                    text=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    check=False,
                    timeout=10,
                )

                self.assertEqual(result.returncode, 0, result.stdout)
                self.assertIn("Continuing because --allow-concurrent was supplied", result.stdout)
                self.assertEqual(3, len(calls.read_text(encoding="utf-8").splitlines()))
            finally:
                first.terminate()
                try:
                    first.communicate(timeout=5)
                except subprocess.TimeoutExpired:
                    first.kill()
                    first.communicate(timeout=5)

    def _write_minimal_repo(self, repo: Path, class_name: str = "ExampleTest") -> Path:
        (repo / ".git").mkdir()
        (repo / "pom.xml").write_text("<project />\n", encoding="utf-8")
        test_dir = repo / "module" / "src" / "test" / "java"
        test_dir.mkdir(parents=True)
        (repo / "module" / "pom.xml").write_text("<project />\n", encoding="utf-8")
        (test_dir / f"{class_name}.java").write_text(
            "class " + class_name + " {}\n",
            encoding="utf-8",
        )
        return repo

    def _write_fake_mvn(self, repo: Path, calls: Path, started: Path | None = None) -> Path:
        fake_mvn = repo / "fake-mvn.py"
        slow_block = ""
        if started is not None:
            claim = started.with_suffix(".claim")
            slow_block = textwrap.dedent(
                f"""\
                import os
                import time
                try:
                    fd = os.open({str(claim)!r}, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                    os.close(fd)
                    Path({str(started)!r}).write_text("started\\n", encoding="utf-8")
                    time.sleep(30)
                except FileExistsError:
                    pass
                """
            )
        fake_mvn.write_text(
            textwrap.dedent(
                f"""\
                #!{sys.executable}
                import sys
                from pathlib import Path

                with Path({str(calls)!r}).open("a", encoding="utf-8") as out:
                    out.write("\\0".join(sys.argv[1:]) + "\\n")
                """
            )
            + slow_block,
            encoding="utf-8",
        )
        os.chmod(fake_mvn, 0o755)
        return fake_mvn

    def _wait_for_file(self, path: Path) -> None:
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            if path.is_file():
                return
            time.sleep(0.05)
        self.fail(f"Timed out waiting for {path}")

    def _verify_args(self, calls: Path) -> list[str]:
        return self._all_args(calls)[1]

    def _all_args(self, calls: Path) -> tuple[list[str], list[str]]:
        lines = calls.read_text(encoding="utf-8").splitlines()
        self.assertEqual(len(lines), 2, lines)
        return lines[0].split("\0"), lines[1].split("\0")


if __name__ == "__main__":
    unittest.main()
