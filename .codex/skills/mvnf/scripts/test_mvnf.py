#!/usr/bin/env python3

from __future__ import annotations

import os
import stat
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from textwrap import dedent


SCRIPT = Path(__file__).with_name("mvnf.py")


class MvnfStreamTest(unittest.TestCase):

    def test_stream_filters_install_noise_but_keeps_verify_output(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            module = root / "module-a"
            module.mkdir()
            (root / "pom.xml").write_text("<project />\n", encoding="utf-8")
            (module / "pom.xml").write_text("<project />\n", encoding="utf-8")

            fake_mvn = root / "fake-mvn"
            fake_mvn.write_text(
                dedent(
                    """\
                    #!/usr/bin/env bash
                    if [[ " $* " == *" install "* ]]; then
                      echo "[INFO] INSTALL NOISE should be hidden"
                      echo "[WARNING] warning should be hidden"
                      echo "[ERROR] install error should be visible"
                      echo "[INFO] Reactor Summary for fake build:"
                      echo "[INFO] module-a SUCCESS"
                      exit 0
                    fi

                    echo "[INFO] VERIFY OUTPUT should stream"
                    exit 0
                    """
                ),
                encoding="utf-8",
            )
            fake_mvn.chmod(fake_mvn.stat().st_mode | stat.S_IXUSR)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "module-a",
                    "--stream",
                    "--retain-logs",
                    "--mvn",
                    str(fake_mvn),
                ],
                cwd=root,
                env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"},
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertNotIn("INSTALL NOISE should be hidden", result.stdout)
            self.assertNotIn("warning should be hidden", result.stdout)
            self.assertIn("install error should be visible", result.stdout)
            self.assertIn("Reactor Summary for fake build", result.stdout)
            self.assertIn("VERIFY OUTPUT should stream", result.stdout)
            self.assertEqual(
                (root / "maven-build.log").read_text(encoding="utf-8").count("INSTALL NOISE"),
                1,
            )

    def test_success_hides_maven_tail_and_prints_report_summary(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            module = root / "module-a"
            module.mkdir()
            (root / "pom.xml").write_text("<project />\n", encoding="utf-8")
            (module / "pom.xml").write_text("<project />\n", encoding="utf-8")

            fake_mvn = root / "fake-mvn"
            fake_mvn.write_text(
                dedent(
                    """\
                    #!/usr/bin/env bash
                    if [[ " $* " == *" install "* ]]; then
                      echo "[INFO] INSTALL NOISE should be hidden"
                      exit 0
                    fi

                    mkdir -p module-a/target/surefire-reports
                    cat > module-a/target/surefire-reports/TEST-ExampleTest.xml <<'XML'
                    <testsuite name="ExampleTest" tests="2" failures="0" errors="0" skipped="1" time="0.123" />
                    XML
                    echo "[INFO] VERIFY NOISE should be hidden"
                    exit 0
                    """
                ),
                encoding="utf-8",
            )
            fake_mvn.chmod(fake_mvn.stat().st_mode | stat.S_IXUSR)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "module-a",
                    "--retain-logs",
                    "--mvn",
                    str(fake_mvn),
                ],
                cwd=root,
                env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"},
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertNotIn("INSTALL NOISE should be hidden", result.stdout)
            self.assertNotIn("VERIFY NOISE should be hidden", result.stdout)
            self.assertIn("[mvnf] Root install passed: BUILD SUCCESS", result.stdout)
            self.assertIn("maven-build.log", result.stdout)
            self.assertIn("Tests passed", result.stdout)
            self.assertIn("Reports:", result.stdout)
            self.assertIn("TEST-ExampleTest.xml", result.stdout)
            self.assertIn("tests=2", result.stdout)
            self.assertIn("failures=0", result.stdout)
            self.assertIn("errors=0", result.stdout)
            self.assertIn("skipped=1", result.stdout)

    def test_tail_on_success_keeps_previous_success_tail_behavior(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            module = root / "module-a"
            module.mkdir()
            (root / "pom.xml").write_text("<project />\n", encoding="utf-8")
            (module / "pom.xml").write_text("<project />\n", encoding="utf-8")

            fake_mvn = root / "fake-mvn"
            fake_mvn.write_text(
                dedent(
                    """\
                    #!/usr/bin/env bash
                    if [[ " $* " == *" install "* ]]; then
                      echo "[INFO] INSTALL TAIL should be visible"
                      exit 0
                    fi

                    echo "[INFO] VERIFY TAIL should be visible"
                    exit 0
                    """
                ),
                encoding="utf-8",
            )
            fake_mvn.chmod(fake_mvn.stat().st_mode | stat.S_IXUSR)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "module-a",
                    "--retain-logs",
                    "--tail-on-success",
                    "--mvn",
                    str(fake_mvn),
                ],
                cwd=root,
                env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"},
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn("INSTALL TAIL should be visible", result.stdout)
            self.assertIn("VERIFY TAIL should be visible", result.stdout)

    def test_failure_prints_compact_report_summary_without_huge_payload(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            module = root / "module-a"
            module.mkdir()
            (root / "pom.xml").write_text("<project />\n", encoding="utf-8")
            (module / "pom.xml").write_text("<project />\n", encoding="utf-8")

            fake_mvn = root / "fake-mvn"
            fake_mvn.write_text(
                dedent(
                    """\
                    #!/usr/bin/env bash
                    if [[ " $* " == *" install "* ]]; then
                      exit 0
                    fi

                    mkdir -p module-a/target/surefire-reports
                    python3 - <<'PY'
                    from pathlib import Path
                    payload = "A" * 1000
                    Path("module-a/target/surefire-reports/TEST-ExampleTest.xml").write_text(f'''<testsuite name="ExampleTest" tests="1" failures="1" errors="0" skipped="0" time="0.456">
                      <testcase classname="ExampleTest" name="fails" time="0.001">
                        <failure message="expected compact failure">{payload}</failure>
                      </testcase>
                    </testsuite>
                    ''', encoding="utf-8")
                    PY
                    echo "[INFO] VERIFY FAILURE TAIL should remain"
                    exit 1
                    """
                ),
                encoding="utf-8",
            )
            fake_mvn.chmod(fake_mvn.stat().st_mode | stat.S_IXUSR)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "module-a",
                    "--retain-logs",
                    "--mvn",
                    str(fake_mvn),
                ],
                cwd=root,
                env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"},
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 1, result.stdout)
            self.assertIn("Tests failed", result.stdout)
            self.assertIn("ExampleTest.fails", result.stdout)
            self.assertIn("expected compact failure", result.stdout)
            self.assertIn("VERIFY FAILURE TAIL should remain", result.stdout)
            self.assertNotIn("A" * 200, result.stdout)


if __name__ == "__main__":
    unittest.main()
