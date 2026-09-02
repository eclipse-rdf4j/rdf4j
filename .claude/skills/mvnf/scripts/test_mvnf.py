#!/usr/bin/env python3

from __future__ import annotations

import itertools
import json
import os
import stat
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path
from textwrap import dedent
from unittest import mock

import maven_workspace


SCRIPT = Path(__file__).with_name("mvnf.py")
AGENT_SCRIPT = Path(__file__).resolve().parents[4] / "scripts" / "mvn-agent.py"


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


class MvnfWorkspaceTest(unittest.TestCase):

    def test_empty_parent_relative_path_does_not_load_neighbor_pom(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            (root / "pom.xml").write_text(
                "<project><modelVersion>4.0.0</modelVersion><groupId>com.local</groupId>"
                "<artifactId>local-parent</artifactId><version>1.0</version></project>\n",
                encoding="utf-8",
            )
            module = root / "module"
            module.mkdir()
            module_pom = module / "pom.xml"
            module_pom.write_text(
                "<project><modelVersion>4.0.0</modelVersion><parent>"
                "<groupId>com.external</groupId><artifactId>external-parent</artifactId>"
                "<version>9.0</version><relativePath/></parent>"
                "<artifactId>module</artifactId></project>\n",
                encoding="utf-8",
            )

            gav = maven_workspace.load_project_gav(module_pom)

            self.assertEqual("com.external", gav.group_id)
            self.assertEqual("9.0", gav.version)

    def test_cli_workspace_precedence_injects_isolated_paths_and_uses_gav_reports(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            workspace_root = (root / ".mvnf" / "workspaces" / "cli-space").resolve()
            selected_reports = (
                workspace_root / "build" / "com.example" / "module-a" / "1.0-SNAPSHOT" / "surefire-reports"
            )
            selected_reports.mkdir(parents=True)
            stale_report = selected_reports / "stale.txt"
            stale_report.write_text("stale\n", encoding="utf-8")
            neighbor = workspace_root / "build" / "com.example" / "neighbor" / "1.0-SNAPSHOT" / "sentinel.txt"
            neighbor.parent.mkdir(parents=True)
            neighbor.write_text("preserve\n", encoding="utf-8")

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--workspace",
                    "cli-space",
                    "module-path",
                    "--retain-logs",
                    "--mvn",
                    str(fake_mvn),
                ],
                cwd=root,
                env={
                    **os.environ,
                    "MVNF_WORKSPACE": "environment-space",
                    "PYTHONDONTWRITEBYTECODE": "1",
                },
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            invocations = [json.loads(line) for line in calls.read_text(encoding="utf-8").splitlines()]
            self.assertEqual(["--version"], invocations[0])
            self.assertEqual(3, len(invocations), invocations)

            expected_properties = {
                f"-Dmaven.repo.local={(root / '.m2_repo').resolve()}",
                f"-Dmaven.repo.local.head={workspace_root / 'repository'}",
                f"-Drdf4j.build.root={workspace_root / 'build'}",
                "-Dformatter.skip=true",
            }
            for invocation in invocations[1:]:
                self.assertTrue(expected_properties.issubset(set(invocation)), invocation)
                thread_index = invocation.index("-T")
                self.assertEqual("1", invocation[thread_index + 1])
                tmp_arguments = [
                    argument for argument in invocation if argument.startswith("-Drdf4j.test.tmpRoot=")
                ]
                self.assertEqual(1, len(tmp_arguments), invocation)
                self.assertTrue(
                    tmp_arguments[0].startswith(f"-Drdf4j.test.tmpRoot={workspace_root / 'tmp'}/"),
                    invocation,
                )
                self.assertFalse(
                    any(argument.startswith("-Drdf4j.test.outputDirectory=") for argument in invocation),
                    invocation,
                )
                self.assertFalse(any(argument.startswith("-Djava.io.tmpdir=") for argument in invocation), invocation)

            tmp_root = Path(
                next(
                    argument.split("=", 1)[1]
                    for argument in invocations[1]
                    if argument.startswith("-Drdf4j.test.tmpRoot=")
                )
            )
            self.assertTrue((tmp_root / "com.example" / "fake-parent" / "1.0-SNAPSHOT").is_dir())
            self.assertTrue((tmp_root / "com.example" / "module-a" / "1.0-SNAPSHOT").is_dir())

            self.assertFalse((root / ".mvnf" / "workspaces" / "environment-space").exists())
            self.assertFalse(stale_report.exists())
            self.assertEqual("preserve\n", neighbor.read_text(encoding="utf-8"))
            report = (
                workspace_root
                / "build"
                / "com.example"
                / "module-a"
                / "1.0-SNAPSHOT"
                / "surefire-reports"
                / "TEST-WorkspaceExampleTest.xml"
            )
            self.assertTrue(report.is_file(), result.stdout)
            self.assertIn(report.relative_to(root.resolve()).as_posix(), result.stdout)
            self.assertEqual(1, len(list((workspace_root / "logs").iterdir())))
            self.assertEqual(1, len(list((workspace_root / "tmp").iterdir())))

    def test_invalid_workspace_ids_are_rejected_before_maven_starts(self) -> None:
        invalid_ids = ["", "../escape", "a/b", r"a\b", ".hidden", " space", "a" * 65]
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            fake_mvn = self._write_workspace_fake_maven(root, root / "fake-maven-calls.jsonl")

            for workspace_id in invalid_ids:
                with self.subTest(workspace_id=workspace_id):
                    calls = root / "fake-maven-calls.jsonl"
                    calls.unlink(missing_ok=True)
                    result = self._run_mvnf(root, fake_mvn, workspace=workspace_id)

                    self.assertNotEqual(result.returncode, 0, result.stdout)
                    self.assertIn("workspace", result.stdout.lower())
                    self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_requires_maven_3_9_10_before_build_work(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = self._run_mvnf(
                root,
                fake_mvn,
                workspace="old-maven",
                environment={"FAKE_MAVEN_VERSION": "3.9.9"},
            )

            self.assertNotEqual(result.returncode, 0, result.stdout)
            self.assertIn("3.9.10", result.stdout)
            self.assertEqual([["--version"]], self._read_calls(calls))

    def test_workspace_version_check_is_independent_of_failure_tail_length(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            for tail_length in ("1", "0"):
                with self.subTest(tail_length=tail_length):
                    calls.unlink(missing_ok=True)
                    result = self._run_mvnf(
                        root,
                        fake_mvn,
                        workspace=f"short-tail-{tail_length}",
                        runner_arguments=("--tail", tail_length),
                    )

                    self.assertEqual(0, result.returncode, result.stdout)
                    self.assertEqual(3, len(self._read_calls(calls)), result.stdout)

    def test_workspace_rejects_negative_failure_tail_before_maven(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = self._run_mvnf(
                root,
                fake_mvn,
                workspace="negative-tail",
                runner_arguments=("--tail", "-1"),
            )

            self.assertNotEqual(0, result.returncode, result.stdout)
            self.assertIn("--tail", result.stdout)
            self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_rejects_owned_properties_and_mutating_goals_before_maven(self) -> None:
        forbidden_arguments = [
            ("-Dmaven.repo.local=/tmp/hijack",),
            ("-Dmaven.repo.local.head=/tmp/hijack",),
            ("-Drdf4j.build.root=/tmp/hijack",),
            ("-Drdf4j.test.outputDirectory=/tmp/hijack",),
            ("-Drdf4j.test.tmpRoot=/tmp/hijack",),
            ("-Drdf4j.test.tmpDirectory=/tmp/hijack",),
            ("-Djava.io.tmpdir=/tmp/hijack",),
            ("-Dformatter.skip", "false"),
            ("--define=maven.repo.local=/tmp/hijack",),
            ("--define", "rdf4j.build.root=/tmp/hijack"),
            ("-Dharmless", "deploy"),
            ("-T2",),
            ("-P", "formatting"),
            ("-P", "workspace-build-root"),
            ("-P!workspace-build-root",),
            ("-P-workspace-build-root",),
            ("-Pquick,!workspace-build-root",),
            ("--activate-profiles=workspace-build-root",),
            ("--activate-profiles=-workspace-build-root",),
            ("--activate-profiles", "quick,!workspace-build-root"),
            ("-f", "other-pom.xml"),
            ("--file=other-pom.xml",),
            ("-pl", "other-module"),
            ("--projects=other-module",),
            ("-rfmodule-path",),
            ("-fother-pom.xml",),
            ("-am",),
            ("--also-make",),
            ("-q",),
            ("--quiet",),
            ("-l", "outside.log"),
            ("--log-file=outside.log",),
            ("--raw-streams", "deploy"),
            ("deploy",),
            ("site-deploy",),
            ("generate-sources",),
            ("generate-test-sources",),
            ("deploy:deploy",),
            ("deploy:deploy-file",),
            ("site:deploy",),
            ("release:prepare",),
            ("spotless:apply",),
            ("spotless:apply@format-execution",),
            ("spotless:2.44.0:apply",),
            ("com.diffplug.spotless:spotless-maven-plugin:2.44.0:apply",),
            ("com.diffplug.spotless:spotless-maven-plugin:2.44.0:apply@format-execution",),
            ("formatter:format@format-execution",),
            ("sortpom:sort@sort-execution",),
            ("org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy@deploy-execution",),
            ("org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file",),
            ("flatten:flatten",),
            ("help:evaluate",),
            ("org.codehaus.mojo:exec-maven-plugin:3.6.2:exec",),
            ("org.apache.maven.plugins:maven-antrun-plugin:3.2.0:run",),
            ("org.openrewrite.maven:rewrite-maven-plugin:6.22.1:run",),
            ("org.codehaus.mojo:jaxb2-maven-plugin:3.3.0:xjc",),
            ("org.apache.cxf:cxf-codegen-plugin:4.1.3:wsdl2java",),
        ]
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            for forwarded_arguments in forbidden_arguments:
                with self.subTest(arguments=forwarded_arguments):
                    calls.unlink(missing_ok=True)
                    result = self._run_mvnf(
                        root,
                        fake_mvn,
                        workspace="owned-properties",
                        forwarded=forwarded_arguments,
                    )

                    self.assertNotEqual(result.returncode, 0, result.stdout)
                    self.assertFalse(calls.exists(), result.stdout)

    def test_lowercase_toolchains_option_is_not_confused_with_reactor_threads(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = self._run_mvnf(
                root,
                fake_mvn,
                workspace="toolchains-option",
                forwarded=("-t", "toolchains.xml"),
            )

            self.assertEqual(0, result.returncode, result.stdout)
            self.assertIn("-t", self._read_calls(calls)[-1])

    def test_mvnf_allows_safe_fail_strategy_options(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            for option in ("-fae", "-ff", "-fn"):
                with self.subTest(option=option):
                    calls.unlink(missing_ok=True)
                    result = self._run_mvnf(
                        root,
                        fake_mvn,
                        workspace=f"fail-strategy-{option[2:]}",
                        forwarded=(option,),
                    )

                    self.assertEqual(0, result.returncode, result.stdout)
                    self.assertIn(option, self._read_calls(calls)[-1])

    def test_workspace_rejects_hidden_maven_args_before_maven(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            for hidden_arguments in (
                "deploy",
                "-Dmaven.repo.local=/tmp/hijack",
                "-P!workspace-build-root",
                "--activate-profiles=-workspace-build-root",
            ):
                with self.subTest(hidden_arguments=hidden_arguments):
                    calls.unlink(missing_ok=True)
                    result = self._run_mvnf(
                        root,
                        fake_mvn,
                        workspace="hidden-maven-args",
                        environment={"MAVEN_ARGS": hidden_arguments},
                    )

                    self.assertNotEqual(0, result.returncode, result.stdout)
                    self.assertIn("MAVEN_ARGS", result.stdout)
                    self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_rejects_symlinked_path_ancestors_before_maven(self) -> None:
        for symlink_location in (".mvnf", "runs", "tmp", "logs"):
            with self.subTest(symlink_location=symlink_location), tempfile.TemporaryDirectory() as tmp_dir:
                root = Path(tmp_dir)
                self._write_minimal_repo(root)
                calls = root / "fake-maven-calls.jsonl"
                fake_mvn = self._write_workspace_fake_maven(root, calls)
                outside = root / f"outside-{symlink_location.lstrip('.')}"
                outside.mkdir()
                if symlink_location == ".mvnf":
                    (root / ".mvnf").symlink_to(outside, target_is_directory=True)
                elif symlink_location == "runs":
                    mvnf_root = root / ".mvnf"
                    mvnf_root.mkdir()
                    (mvnf_root / "runs").symlink_to(outside, target_is_directory=True)
                else:
                    workspace_root = root / ".mvnf" / "workspaces" / "symlink-space"
                    workspace_root.mkdir(parents=True)
                    (workspace_root / symlink_location).symlink_to(outside, target_is_directory=True)

                result = self._run_mvnf(root, fake_mvn, workspace="symlink-space")

                self.assertNotEqual(0, result.returncode, result.stdout)
                self.assertIn("symlink", result.stdout.lower())
                self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_cleanup_cannot_follow_symlinked_gav_ancestor(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            workspace_build = root / ".mvnf" / "workspaces" / "gav-symlink" / "build"
            workspace_build.mkdir(parents=True)
            outside = root / "outside-gav"
            reports = outside / "module-a" / "1.0-SNAPSHOT" / "surefire-reports"
            reports.mkdir(parents=True)
            sentinel = reports / "sentinel.txt"
            sentinel.write_text("preserve\n", encoding="utf-8")
            (workspace_build / "com.example").symlink_to(outside, target_is_directory=True)

            result = self._run_mvnf(root, fake_mvn, workspace="gav-symlink")

            self.assertNotEqual(0, result.returncode, result.stdout)
            self.assertIn("symlink", result.stdout.lower())
            self.assertTrue(sentinel.is_file(), "Workspace cleanup followed a symlink outside its build root")
            self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_rejects_external_nested_symlinks_in_reused_writable_trees(self) -> None:
        for tree_name in ("repository", "build"):
            with self.subTest(tree=tree_name), tempfile.TemporaryDirectory() as tmp_dir:
                root = Path(tmp_dir)
                self._write_minimal_repo(root)
                workspace_tree = root / ".mvnf" / "workspaces" / "nested-symlink" / tree_name
                workspace_tree.mkdir(parents=True)
                outside = root / f"outside-{tree_name}"
                outside.mkdir()
                (workspace_tree / "redirect").symlink_to(outside, target_is_directory=True)
                calls = root / "fake-maven-calls.jsonl"
                fake_mvn = self._write_workspace_fake_maven(root, calls)

                result = self._run_mvnf(root, fake_mvn, workspace="nested-symlink")

                self.assertNotEqual(0, result.returncode, result.stdout)
                self.assertIn("symlink", result.stdout.lower())
                self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_allows_nested_symlinks_that_remain_inside_its_root(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            repository = root / ".mvnf" / "workspaces" / "internal-symlink" / "repository"
            internal_target = repository / "internal-target"
            internal_target.mkdir(parents=True)
            (repository / "internal-alias").symlink_to(internal_target, target_is_directory=True)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = self._run_mvnf(root, fake_mvn, workspace="internal-symlink")

            self.assertEqual(0, result.returncode, result.stdout)
            self.assertTrue(calls.exists(), result.stdout)

    def test_maven_command_override_cannot_smuggle_owned_arguments(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--workspace",
                    "smuggled-command",
                    "module-path",
                    "--mvn",
                    f"{fake_mvn} -Dmaven.repo.local=/tmp/hijack",
                ],
                cwd=root,
                env=self._environment(),
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertNotEqual(result.returncode, 0, result.stdout)
            self.assertIn("maven.repo.local", result.stdout)
            self.assertFalse(calls.exists(), result.stdout)

    def test_different_workspaces_overlap_and_keep_unique_run_directories(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            barrier = root / "barrier"
            environment = {
                "FAKE_MAVEN_BARRIER_DIR": str(barrier),
                "FAKE_MAVEN_BARRIER_COUNT": "2",
            }

            first = self._start_mvnf(root, fake_mvn, workspace="overlap-a", environment=environment)
            second = self._start_mvnf(root, fake_mvn, workspace="overlap-b", environment=environment)
            first_stdout, _ = first.communicate(timeout=15)
            second_stdout, _ = second.communicate(timeout=15)

            self.assertEqual(0, first.returncode, first_stdout)
            self.assertEqual(0, second.returncode, second_stdout)
            self.assertEqual(2, len(list(barrier.iterdir())))
            for workspace_id in ("overlap-a", "overlap-b"):
                workspace_root = root / ".mvnf" / "workspaces" / workspace_id
                self.assertEqual(1, len(list((workspace_root / "logs").iterdir())))
                self.assertEqual(1, len(list((workspace_root / "tmp").iterdir())))
                report = (
                    workspace_root
                    / "build"
                    / "com.example"
                    / "module-a"
                    / "1.0-SNAPSHOT"
                    / "surefire-reports"
                    / "TEST-WorkspaceExampleTest.xml"
                )
                self.assertTrue(report.is_file(), first_stdout + second_stdout)
            self.assertEqual([], list((root / ".mvnf" / "runs").glob("*.json")))

    def test_same_workspace_conflicts_before_second_maven_process(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            release = root / "release"
            environment = {
                "FAKE_MAVEN_STARTED": str(started),
                "FAKE_MAVEN_RELEASE": str(release),
            }
            first = self._start_mvnf(root, fake_mvn, workspace="same-space", environment=environment)
            try:
                self._wait_for_file(started)
                calls_before = list(self._read_calls(calls))
                result = self._run_mvnf(
                    root,
                    fake_mvn,
                    workspace="same-space",
                    runner_arguments=("--allow-concurrent",),
                    environment=environment,
                )

                self.assertNotEqual(result.returncode, 0, result.stdout)
                self.assertIn("same-space", result.stdout)
                self.assertEqual(calls_before, self._read_calls(calls), result.stdout)
            finally:
                release.write_text("release\n", encoding="utf-8")
                first_stdout, _ = first.communicate(timeout=10)
            self.assertEqual(0, first.returncode, first_stdout)

    def test_live_registry_record_survives_unavailable_start_identity_probe(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            registry = Path(tmp_dir)
            record_path = registry / "live.json"
            record = {
                "pid": os.getpid(),
                "processStart": "recorded-start",
                "workspace": "still-live",
            }
            record_path.write_text(json.dumps(record) + "\n", encoding="utf-8")

            with (
                mock.patch.object(maven_workspace, "_pid_is_running", return_value=True),
                mock.patch.object(maven_workspace, "_process_start_identity", return_value=None),
            ):
                active = maven_workspace._active_records(registry)

            self.assertEqual([record], active)
            self.assertTrue(record_path.is_file(), "An unavailable identity probe must not delete a live lock")

    def test_child_is_registered_before_optional_process_identity_probe(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            registry = Path(tmp_dir)
            record_path = registry / "runner.json"
            record_path.write_text(
                json.dumps(
                    {
                        "pid": os.getpid(),
                        "processStart": None,
                        "children": [],
                        "workspace": "registration-order",
                    }
                )
                + "\n",
                encoding="utf-8",
            )
            registration = maven_workspace.RunRegistration(
                "runner",
                "registration-order",
                os.getpid(),
                record_path,
            )

            with mock.patch.object(
                maven_workspace,
                "_process_start_identity",
                side_effect=AssertionError("child identity probing must not delay registry ownership"),
            ):
                child = maven_workspace._register_child(registration, 424242, 424242)

            registered = json.loads(record_path.read_text(encoding="utf-8"))
            self.assertIsNone(child.process_start)
            self.assertEqual(424242, registered["children"][0]["pid"])

    def test_surviving_maven_child_keeps_workspace_locked_after_runner_is_killed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            child_pid_file = root / "child-pid"
            release = root / "release"
            first = self._start_mvnf(
                root,
                fake_mvn,
                workspace="orphan-guard",
                environment={
                    "FAKE_MAVEN_STARTED": str(started),
                    "FAKE_MAVEN_CHILD_PID": str(child_pid_file),
                    "FAKE_MAVEN_RELEASE": str(release),
                },
            )
            try:
                self._wait_for_file(started)
                self._wait_for_file(child_pid_file)
                child_pid = int(child_pid_file.read_text(encoding="utf-8"))
                calls_before = list(self._read_calls(calls))
                first.kill()
                first.communicate(timeout=5)

                second = self._run_mvnf(root, fake_mvn, workspace="orphan-guard")

                self.assertNotEqual(0, second.returncode, second.stdout)
                self.assertIn(str(child_pid), second.stdout)
                self.assertEqual(calls_before, self._read_calls(calls), second.stdout)
            finally:
                release.write_text("release\n", encoding="utf-8")
                if first.poll() is None:
                    first.kill()
                    first.communicate(timeout=5)
                if child_pid_file.is_file():
                    self._wait_for_pid_exit(int(child_pid_file.read_text(encoding="utf-8")))

    def test_terminating_runner_forwards_signal_to_active_maven_child(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            child_pid_file = root / "child-pid"
            release = root / "release"
            first = self._start_mvnf(
                root,
                fake_mvn,
                workspace="signal-forwarding",
                environment={
                    "FAKE_MAVEN_STARTED": str(started),
                    "FAKE_MAVEN_CHILD_PID": str(child_pid_file),
                    "FAKE_MAVEN_RELEASE": str(release),
                },
            )
            try:
                self._wait_for_file(started)
                self._wait_for_file(child_pid_file)
                child_pid = int(child_pid_file.read_text(encoding="utf-8"))

                first.terminate()
                first.communicate(timeout=5)

                self._wait_for_pid_exit(child_pid)
            finally:
                release.write_text("release\n", encoding="utf-8")
                if first.poll() is None:
                    first.kill()
                    first.communicate(timeout=5)
                if child_pid_file.is_file():
                    self._wait_for_pid_exit(int(child_pid_file.read_text(encoding="utf-8")))

    def test_termination_never_continues_to_the_next_maven_phase(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            release = root / "release"
            first = self._start_mvnf(
                root,
                fake_mvn,
                workspace="cancelled-run",
                environment={
                    "FAKE_MAVEN_STARTED": str(started),
                    "FAKE_MAVEN_RELEASE": str(release),
                    "FAKE_MAVEN_TERM_ZERO": "1",
                },
            )
            try:
                self._wait_for_file(started)
                calls_before = list(self._read_calls(calls))

                first.terminate()
                first_stdout, _ = first.communicate(timeout=10)

                self.assertNotEqual(0, first.returncode, first_stdout)
                self.assertEqual(calls_before, self._read_calls(calls), first_stdout)
            finally:
                release.write_text("release\n", encoding="utf-8")
                if first.poll() is None:
                    first.kill()
                    first.communicate(timeout=5)

    def test_runner_reaps_maven_process_group_descendants_before_unlocking(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            descendant_pid_file = root / "descendant-pid"

            try:
                result = self._run_mvnf(
                    root,
                    fake_mvn,
                    workspace="descendant-reaping",
                    environment={"FAKE_MAVEN_DESCENDANT_PID": str(descendant_pid_file)},
                )

                self.assertEqual(0, result.returncode, result.stdout)
                self._wait_for_file(descendant_pid_file)
                self._wait_for_pid_exit(int(descendant_pid_file.read_text(encoding="utf-8")))
            finally:
                if descendant_pid_file.is_file():
                    descendant_pid = int(descendant_pid_file.read_text(encoding="utf-8"))
                    try:
                        os.kill(descendant_pid, 15)
                    except ProcessLookupError:
                        pass

    def test_legacy_and_workspace_runs_conflict_before_workspace_version_probe(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            release = root / "release"
            environment = {
                "FAKE_MAVEN_STARTED": str(started),
                "FAKE_MAVEN_RELEASE": str(release),
            }
            legacy = self._start_mvnf(root, fake_mvn, workspace=None, environment=environment)
            try:
                self._wait_for_file(started)
                calls_before = list(self._read_calls(calls))
                result = self._run_mvnf(
                    root,
                    fake_mvn,
                    workspace="blocked-by-legacy",
                    environment=environment,
                )

                self.assertNotEqual(result.returncode, 0, result.stdout)
                self.assertIn("legacy", result.stdout.lower())
                self.assertEqual(calls_before, self._read_calls(calls), result.stdout)
            finally:
                release.write_text("release\n", encoding="utf-8")
                legacy_stdout, _ = legacy.communicate(timeout=10)
            self.assertEqual(0, legacy.returncode, legacy_stdout)

    def test_named_legacy_workspace_is_not_treated_as_global_legacy_scope(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            release = root / "release"
            first = self._start_mvnf(
                root,
                fake_mvn,
                workspace="legacy",
                environment={
                    "FAKE_MAVEN_STARTED": str(started),
                    "FAKE_MAVEN_RELEASE": str(release),
                },
            )
            try:
                self._wait_for_file(started)

                second = self._run_mvnf(root, fake_mvn, workspace="ordinary-workspace")

                self.assertEqual(0, second.returncode, second.stdout)
            finally:
                release.write_text("release\n", encoding="utf-8")
                first_stdout, _ = first.communicate(timeout=10)
            self.assertEqual(0, first.returncode, first_stdout)

    def test_case_aliases_conflict_when_the_filesystem_maps_them_to_one_directory(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            started = root / "started"
            release = root / "release"
            first = self._start_mvnf(
                root,
                fake_mvn,
                workspace="CaseAlias",
                environment={
                    "FAKE_MAVEN_STARTED": str(started),
                    "FAKE_MAVEN_RELEASE": str(release),
                },
            )
            try:
                self._wait_for_file(started)
                upper = root / ".mvnf" / "workspaces" / "CaseAlias"
                lower = root / ".mvnf" / "workspaces" / "casealias"
                if not lower.exists() or not upper.samefile(lower):
                    self.skipTest("The test filesystem is case-sensitive")
                calls_before = list(self._read_calls(calls))

                second = self._run_mvnf(root, fake_mvn, workspace="casealias")

                self.assertNotEqual(0, second.returncode, second.stdout)
                self.assertEqual(calls_before, self._read_calls(calls), second.stdout)
            finally:
                release.write_text("release\n", encoding="utf-8")
                first_stdout, _ = first.communicate(timeout=10)
            self.assertEqual(0, first.returncode, first_stdout)

    def test_pre_workspace_legacy_marker_blocks_before_workspace_version_probe(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            marker = root / "target" / "mvnf-runs" / str(os.getpid())
            marker.mkdir(parents=True)
            (marker / "started-at").write_text("2026-07-13T12:00:00+00:00\n", encoding="utf-8")

            result = self._run_mvnf(
                root,
                fake_mvn,
                workspace="blocked-by-pre-workspace-marker",
            )

            self.assertNotEqual(result.returncode, 0, result.stdout)
            self.assertIn("legacy", result.stdout.lower())
            self.assertIn(str(os.getpid()), result.stdout)
            self.assertFalse(calls.exists(), result.stdout)
            self.assertTrue(marker.is_dir(), "A live legacy marker must remain owned by its process")

    def test_stale_pre_workspace_legacy_marker_is_removed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)
            marker = root / "target" / "mvnf-runs" / "999999999"
            marker.mkdir(parents=True)
            (marker / "started-at").write_text("2026-07-13T12:00:00+00:00\n", encoding="utf-8")
            (marker / "argv").write_text("python3 mvnf.py module-path\n", encoding="utf-8")

            result = self._run_mvnf(
                root,
                fake_mvn,
                workspace="stale-pre-workspace-marker",
            )

            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertFalse(marker.exists(), "A dead process must not leave the compatibility lock wedged")
            self.assertEqual(3, len(self._read_calls(calls)), result.stdout)

    def test_allow_concurrent_requires_workspace_and_is_deprecated_with_one(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            legacy_result = self._run_mvnf(
                root,
                fake_mvn,
                workspace=None,
                runner_arguments=("--allow-concurrent",),
            )
            self.assertNotEqual(legacy_result.returncode, 0, legacy_result.stdout)
            self.assertIn("--workspace", legacy_result.stdout)
            self.assertFalse(calls.exists(), legacy_result.stdout)

            workspace_result = self._run_mvnf(
                root,
                fake_mvn,
                workspace="deprecated-flag",
                runner_arguments=("--allow-concurrent",),
            )
            self.assertEqual(0, workspace_result.returncode, workspace_result.stdout)
            self.assertIn("deprecated", workspace_result.stdout.lower())

    def test_sequential_workspace_runs_keep_unique_logs_and_tmp_roots(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            first = self._run_mvnf(root, fake_mvn, workspace="repeatable")
            second = self._run_mvnf(root, fake_mvn, workspace="repeatable")

            self.assertEqual(0, first.returncode, first.stdout)
            self.assertEqual(0, second.returncode, second.stdout)
            workspace_root = root / ".mvnf" / "workspaces" / "repeatable"
            log_runs = sorted((workspace_root / "logs").iterdir())
            tmp_runs = sorted((workspace_root / "tmp").iterdir())
            self.assertEqual(2, len(log_runs))
            self.assertEqual(2, len(tmp_runs))
            self.assertEqual({path.name for path in log_runs}, {path.name for path in tmp_runs})

    def test_maven_agent_forwards_safe_lifecycle_with_shared_isolation_contract(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = subprocess.run(
                [
                    sys.executable,
                    str(AGENT_SCRIPT),
                    "--workspace",
                    "lifecycle",
                    "--threads",
                    "2",
                    "--mvn",
                    str(fake_mvn),
                    "--",
                    "-B",
                    "-ntp",
                    "-o",
                    "-pl",
                    "module-path",
                    "-am",
                    "-Pquick",
                    "clean",
                    "install",
                ],
                cwd=root,
                env=self._environment(),
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(0, result.returncode, result.stdout)
            invocations = self._read_calls(calls)
            self.assertEqual(["--version"], invocations[0])
            self.assertEqual(2, len(invocations), invocations)
            build = invocations[1]
            self.assertIn("install", build)
            self.assertIn("clean", build)
            self.assertEqual("module-path", build[build.index("-pl") + 1])
            self.assertIn("-am", build)
            self.assertEqual("2", build[build.index("-T") + 1])
            workspace_root = (root / ".mvnf" / "workspaces" / "lifecycle").resolve()
            self.assertIn(f"-Drdf4j.build.root={workspace_root / 'build'}", build)
            self.assertTrue(any(argument.startswith("-Drdf4j.test.tmpRoot=") for argument in build))
            self.assertEqual(1, len(list((workspace_root / "logs").iterdir())))

    def test_maven_agent_consumes_values_of_safe_selection_and_config_options(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = subprocess.run(
                [
                    sys.executable,
                    str(AGENT_SCRIPT),
                    "--workspace",
                    "lifecycle-option-values",
                    "--mvn",
                    str(fake_mvn),
                    "--",
                    "-pl",
                    "deploy",
                    "-rf",
                    "deploy",
                    "-t",
                    "deploy",
                    "-s",
                    "deploy",
                    "-fae",
                    "install",
                ],
                cwd=root,
                env=self._environment(),
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                check=False,
            )

            self.assertEqual(0, result.returncode, result.stdout)
            build = self._read_calls(calls)[-1]
            self.assertEqual("deploy", build[build.index("-pl") + 1])
            self.assertEqual("deploy", build[build.index("-rf") + 1])
            self.assertEqual("deploy", build[build.index("-t") + 1])
            self.assertEqual("deploy", build[build.index("-s") + 1])
            self.assertIn("-fae", build)

    def test_maven_agent_rejects_workspace_profile_selection_before_maven(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            for profile_arguments in (
                ("-P!workspace-build-root",),
                ("--activate-profiles", "quick,-workspace-build-root"),
            ):
                with self.subTest(arguments=profile_arguments):
                    calls.unlink(missing_ok=True)
                    result = subprocess.run(
                        [
                            sys.executable,
                            str(AGENT_SCRIPT),
                            "--workspace",
                            "lifecycle-profile-guard",
                            "--mvn",
                            str(fake_mvn),
                            "--",
                            *profile_arguments,
                            "install",
                        ],
                        cwd=root,
                        env=self._environment(),
                        text=True,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT,
                        check=False,
                    )

                    self.assertNotEqual(0, result.returncode, result.stdout)
                    self.assertIn("workspace-build-root", result.stdout)
                    self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_runners_reject_unsafe_project_maven_config_before_maven(self) -> None:
        unsafe_configurations = (
            "-P!workspace-build-root\n",
            "-Dmaven.repo.local=/tmp/hijack\n",
            "org.codehaus.mojo:exec-maven-plugin:3.6.2:exec\n",
        )
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            config_directory = root / ".mvn"
            config_directory.mkdir()
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            for runner, configuration in itertools.product(("mvnf", "mvn-agent"), unsafe_configurations):
                with self.subTest(runner=runner, configuration=configuration.strip()):
                    calls.unlink(missing_ok=True)
                    (config_directory / "maven.config").write_text(configuration, encoding="utf-8")
                    if runner == "mvnf":
                        result = self._run_mvnf(root, fake_mvn, workspace="project-config-guard")
                    else:
                        result = subprocess.run(
                            [
                                sys.executable,
                                str(AGENT_SCRIPT),
                                "--workspace",
                                "project-config-guard",
                                "--mvn",
                                str(fake_mvn),
                                "--",
                                "install",
                            ],
                            cwd=root,
                            env=self._environment(),
                            text=True,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT,
                            check=False,
                        )

                    self.assertNotEqual(0, result.returncode, result.stdout)
                    self.assertIn("maven.config", result.stdout)
                    self.assertFalse(calls.exists(), result.stdout)

    def test_workspace_accepts_safe_project_maven_config(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self._write_minimal_repo(root)
            config_directory = root / ".mvn"
            config_directory.mkdir()
            (config_directory / "maven.config").write_text("# retain Maven version output\n-V\n", encoding="utf-8")
            calls = root / "fake-maven-calls.jsonl"
            fake_mvn = self._write_workspace_fake_maven(root, calls)

            result = self._run_mvnf(root, fake_mvn, workspace="safe-project-config")

            self.assertEqual(0, result.returncode, result.stdout)
            self.assertTrue(calls.exists(), result.stdout)

    def _write_minimal_repo(self, root: Path) -> None:
        (root / ".git").mkdir()
        (root / "pom.xml").write_text(
            dedent(
                """\
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>fake-parent</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <modules>
                    <module>module-path</module>
                  </modules>
                </project>
                """
            ),
            encoding="utf-8",
        )
        module = root / "module-path"
        module.mkdir()
        (module / "pom.xml").write_text(
            dedent(
                """\
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>fake-parent</artifactId>
                    <version>1.0-SNAPSHOT</version>
                  </parent>
                  <artifactId>module-a</artifactId>
                </project>
                """
            ),
            encoding="utf-8",
        )

    def _write_workspace_fake_maven(self, root: Path, calls: Path) -> Path:
        fake_mvn = root / "fake-workspace-maven.py"
        fake_mvn.write_text(
            dedent(
                f"""\
                #!{sys.executable}
                import json
                import os
                import signal
                import subprocess
                import sys
                import time
                from pathlib import Path

                def terminate_successfully(_signum, _frame):
                    raise SystemExit(0)

                arguments = sys.argv[1:]
                with Path({str(calls)!r}).open("a", encoding="utf-8") as output:
                    output.write(json.dumps(arguments) + "\\n")
                if arguments == ["--version"]:
                    print("Apache Maven " + os.environ.get("FAKE_MAVEN_VERSION", "3.9.10") + " (fake)")
                    print("Maven home: /fake/maven")
                    print("Java version: 26, vendor: fake")
                    raise SystemExit(0)
                if "install" in arguments:
                    if os.environ.get("FAKE_MAVEN_TERM_ZERO"):
                        signal.signal(signal.SIGTERM, terminate_successfully)
                    started = os.environ.get("FAKE_MAVEN_STARTED")
                    if started:
                        Path(started).write_text("started\\n", encoding="utf-8")
                    child_pid_file = os.environ.get("FAKE_MAVEN_CHILD_PID")
                    if child_pid_file:
                        Path(child_pid_file).write_text(str(os.getpid()) + "\\n", encoding="utf-8")
                    descendant_pid_file = os.environ.get("FAKE_MAVEN_DESCENDANT_PID")
                    if descendant_pid_file:
                        descendant = subprocess.Popen(
                            [sys.executable, "-c", "import time; time.sleep(30)"],
                            stdin=subprocess.DEVNULL,
                            stdout=subprocess.DEVNULL,
                            stderr=subprocess.DEVNULL,
                        )
                        Path(descendant_pid_file).write_text(str(descendant.pid) + "\\n", encoding="utf-8")
                    barrier_dir = os.environ.get("FAKE_MAVEN_BARRIER_DIR")
                    if barrier_dir:
                        barrier = Path(barrier_dir)
                        barrier.mkdir(parents=True, exist_ok=True)
                        (barrier / str(os.getpid())).write_text("arrived\\n", encoding="utf-8")
                        expected = int(os.environ.get("FAKE_MAVEN_BARRIER_COUNT", "2"))
                        deadline = time.monotonic() + 10
                        while len(list(barrier.iterdir())) < expected and time.monotonic() < deadline:
                            time.sleep(0.02)
                        if len(list(barrier.iterdir())) < expected:
                            raise SystemExit(91)
                    release = os.environ.get("FAKE_MAVEN_RELEASE")
                    if release:
                        deadline = time.monotonic() + 10
                        while not Path(release).exists() and time.monotonic() < deadline:
                            time.sleep(0.02)
                        if not Path(release).exists():
                            raise SystemExit(92)
                if "verify" in arguments:
                    build_arguments = [
                        argument for argument in arguments if argument.startswith("-Drdf4j.build.root=")
                    ]
                    reports = (
                        Path(build_arguments[0].split("=", 1)[1])
                        / "com.example"
                        / "module-a"
                        / "1.0-SNAPSHOT"
                        / "surefire-reports"
                        if build_arguments
                        else Path({str(root)!r}) / "module-path" / "target" / "surefire-reports"
                    )
                    reports.mkdir(parents=True, exist_ok=True)
                    (reports / "TEST-WorkspaceExampleTest.xml").write_text(
                        '<testsuite name="WorkspaceExampleTest" tests="1" failures="0" errors="0" skipped="0" time="0.001" />\\n',
                        encoding="utf-8",
                    )
                """
            ),
            encoding="utf-8",
        )
        fake_mvn.chmod(fake_mvn.stat().st_mode | stat.S_IXUSR)
        return fake_mvn

    def _environment(self, additions: dict[str, str] | None = None) -> dict[str, str]:
        environment = {**os.environ, "PYTHONDONTWRITEBYTECODE": "1"}
        environment.pop("MVNF_WORKSPACE", None)
        if additions:
            environment.update(additions)
        return environment

    def _mvnf_command(
        self,
        fake_mvn: Path,
        workspace: str | None,
        runner_arguments: tuple[str, ...] = (),
        forwarded: tuple[str, ...] = (),
    ) -> list[str]:
        command = [sys.executable, str(SCRIPT)]
        if workspace is not None:
            command.extend(["--workspace", workspace])
        command.extend(["module-path", "--retain-logs", "--mvn", str(fake_mvn)])
        command.extend(runner_arguments)
        if forwarded:
            command.append("--")
            command.extend(forwarded)
        return command

    def _run_mvnf(
        self,
        root: Path,
        fake_mvn: Path,
        *,
        workspace: str | None,
        runner_arguments: tuple[str, ...] = (),
        forwarded: tuple[str, ...] = (),
        environment: dict[str, str] | None = None,
    ) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            self._mvnf_command(fake_mvn, workspace, runner_arguments, forwarded),
            cwd=root,
            env=self._environment(environment),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            check=False,
            timeout=15,
        )

    def _start_mvnf(
        self,
        root: Path,
        fake_mvn: Path,
        *,
        workspace: str | None,
        environment: dict[str, str] | None = None,
    ) -> subprocess.Popen[str]:
        return subprocess.Popen(
            self._mvnf_command(fake_mvn, workspace),
            cwd=root,
            env=self._environment(environment),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )

    def _wait_for_file(self, path: Path) -> None:
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            if path.is_file():
                return
            time.sleep(0.02)
        self.fail(f"Timed out waiting for {path}")

    def _wait_for_pid_exit(self, pid: int) -> None:
        deadline = time.monotonic() + 5
        while time.monotonic() < deadline:
            try:
                os.kill(pid, 0)
            except ProcessLookupError:
                return
            time.sleep(0.02)
        self.fail(f"Timed out waiting for pid {pid} to exit")

    def _read_calls(self, calls: Path) -> list[list[str]]:
        return [json.loads(line) for line in calls.read_text(encoding="utf-8").splitlines()]


if __name__ == "__main__":
    unittest.main()
