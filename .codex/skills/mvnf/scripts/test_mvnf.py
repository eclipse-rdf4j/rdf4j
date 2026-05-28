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


if __name__ == "__main__":
    unittest.main()
