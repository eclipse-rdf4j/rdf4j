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


REPO_ROOT = Path(__file__).resolve().parents[1]


class TokenEfficientOutputTest(unittest.TestCase):

    def test_query_plan_guard_compact_mode_logs_full_input_and_keeps_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            log_path = Path(tmp_dir) / "guard.log"
            result = subprocess.run(
                [
                    sys.executable,
                    str(REPO_ROOT / "scripts" / "query-plan-risk-guard.py"),
                    "--compact",
                    "--log",
                    str(log_path),
                ],
                input=dedent(
                    """\
                    warmup noise should be logged only
                    Benchmark                         Mode  Cnt  Score   Error  Units
                    ExampleBenchmark.method           avgt    1  1.000          ns/op
                    """
                ),
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotIn("warmup noise should be logged only", result.stdout)
            self.assertIn("Benchmark", result.stdout)
            self.assertIn("ExampleBenchmark.method", result.stdout)
            self.assertIn("warmup noise should be logged only", log_path.read_text(encoding="utf-8"))

    def test_run_single_benchmark_filters_maven_packaging_but_logs_full_output(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            module = tmp / "bench-module"
            module.mkdir()
            fake_bin = tmp / "bin"
            fake_bin.mkdir()

            fake_mvn = fake_bin / "mvn"
            fake_mvn.write_text(
                dedent(
                    """\
                    #!/usr/bin/env bash
                    set -euo pipefail
                    module=""
                    while [[ $# -gt 0 ]]; do
                      if [[ "$1" == "-pl" ]]; then
                        module="$2"
                        shift 2
                      else
                        shift
                      fi
                    done
                    mkdir -p "${module}/target"
                    touch "${module}/target/fake-jmh.jar"
                    echo "[INFO] PACKAGING NOISE should be logged only"
                    echo "[INFO] Reactor Summary for fake benchmark build:"
                    echo "[INFO] fake-module SUCCESS"
                    """
                ),
                encoding="utf-8",
            )
            fake_mvn.chmod(fake_mvn.stat().st_mode | stat.S_IXUSR)

            fake_java = fake_bin / "java"
            fake_java.write_text(
                dedent(
                    """\
                    #!/usr/bin/env bash
                    echo "Benchmark                         Mode  Cnt  Score   Error  Units"
                    echo "ExampleBenchmark.method           avgt    1  1.000          ns/op"
                    """
                ),
                encoding="utf-8",
            )
            fake_java.chmod(fake_java.stat().st_mode | stat.S_IXUSR)

            result = subprocess.run(
                [
                    str(REPO_ROOT / "scripts" / "run-single-benchmark.sh"),
                    "--module",
                    os.path.relpath(module, REPO_ROOT),
                    "--class",
                    "ExampleBenchmark",
                    "--method",
                    "method",
                    "--warmup-iterations",
                    "0",
                    "--measurement-iterations",
                    "1",
                    "--forks",
                    "0",
                ],
                cwd=REPO_ROOT,
                env={
                    **os.environ,
                    "PATH": f"{fake_bin}{os.pathsep}{os.environ['PATH']}",
                    "RDF4J_BENCHMARK_SKIP_FORK_SOCKET_PREFLIGHT": "true",
                    "RDF4J_BENCHMARK_PLAN_GUARD": "false",
                },
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertNotIn("PACKAGING NOISE should be logged only", result.stdout)
            self.assertIn("Reactor Summary for fake benchmark build", result.stdout)
            self.assertIn("ExampleBenchmark.method", result.stdout)
            self.assertIn(
                "PACKAGING NOISE should be logged only",
                (REPO_ROOT / "maven-build.log").read_text(encoding="utf-8"),
            )


if __name__ == "__main__":
    unittest.main()
