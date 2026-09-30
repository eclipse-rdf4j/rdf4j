import argparse
import json
import os
from pathlib import Path
import shutil
import signal
import re
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from runner_common import start_logged_process, stop_process, wait_for_path


REPO_ROOT = Path(__file__).resolve().parents[2]
GUEST_DIR = REPO_ROOT / "scripts/lmdb-crashlab/guest"
CONTROLLER = GUEST_DIR / "powercut-writer-controller.py"
SCENARIOS = {
    "test_mixed_replay_before_native_commit": (
        "mixed-replay-before-native-commit",
        "AFTER_FULL_REPLAY_BEFORE_AUTHORITATIVE_COMMIT",
        False,
    ),
    "test_dictionary_before_triple_commit": (
        "dictionary-before-triple-commit",
        "AFTER_DICTIONARY_COMMIT_BEFORE_TRIPLESTORE_COMMIT",
        False,
    ),
    "test_commit_returned_before_ack": (
        "commit-returned-before-ack",
        "AFTER_CONNECTION_COMMIT_RETURNED_BEFORE_B_ACK",
        True,
    ),
}


class GuestWriterCutpointTests(unittest.TestCase):
    host_classpath_file = None
    host_classpath = None
    class_dir = None
    class_dir_temp = None
    java = None

    @classmethod
    def configure(cls, host_classpath_file):
        cls.host_classpath_file = host_classpath_file

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        if cls.host_classpath_file is None or not cls.host_classpath_file.is_file():
            raise RuntimeError("--host-classpath-file must name the captured LMDB test classpath")
        cls.host_classpath = cls.host_classpath_file.read_text(encoding="utf-8").strip()
        if not cls.host_classpath:
            raise RuntimeError("captured LMDB test classpath is empty")

        java_home = os.environ.get("JAVA_HOME")
        javac = Path(java_home) / "bin/javac" if java_home else Path(shutil.which("javac") or "")
        java = Path(java_home) / "bin/java" if java_home else Path(shutil.which("java") or "")
        if not javac.is_file() or not java.is_file():
            raise RuntimeError("a JDK with javac and java on PATH or JAVA_HOME is required")
        cls.java = str(java)

        cls.class_dir_temp = tempfile.TemporaryDirectory(prefix="rdf4j-powercut-guest-classes-")
        cls.class_dir = Path(cls.class_dir_temp.name)
        sources = [GUEST_DIR / "CrashPowerCutFixtures.java", GUEST_DIR / "CrashPowerCutWriterMain.java",
                   GUEST_DIR / "CrashPowerCutOracleMain.java", GUEST_DIR / "CrashPowerCutOracleTestMain.java"]
        result = subprocess.run(
            [
                str(javac),
                "--release",
                "25",
                "-classpath",
                cls.host_classpath,
                "-d",
                str(cls.class_dir),
                *(str(source) for source in sources),
            ],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode != 0:
            raise RuntimeError(
                "failed to compile the actual power-cut guest sources with --release 25:\n"
                + result.stdout
                + result.stderr
            )

    @classmethod
    def tearDownClass(cls):
        if cls.class_dir_temp is not None:
            cls.class_dir_temp.cleanup()
        super().tearDownClass()

    def _assert_cutpoint(self, scenario, cutpoint, native_commit_returned):
        with tempfile.TemporaryDirectory(prefix="rdf4j-powercut-cutpoint-") as temporary:
            root = Path(temporary)
            results = root / "results"
            data = root / "store"
            results.mkdir()
            data.mkdir()
            (results / "actual-powercut-scenario.txt").write_text(scenario + "\n", encoding="utf-8")
            (results / "actual-build-classpath-guest.txt").write_text(
                self.host_classpath + "\n", encoding="utf-8"
            )

            env = {
                "RDF4J_CRASHLAB_RESULTS_DIR": str(results),
                "RDF4J_CRASHLAB_DATA_DIR": str(data),
                "RDF4J_CRASHLAB_CLASS_DIR": str(self.class_dir),
                "RDF4J_CRASHLAB_JAVA": self.java,
            }
            controller_log = results / "controller-process.log"
            with patch.dict(os.environ, env):
                process = start_logged_process([sys.executable, str(CONTROLLER)], controller_log)
            try:
                outcome = wait_for_path(
                    process,
                    [results / "actual-cutpoint-witness.json", results / "actual-powercut-writer-failed"],
                    timeout=90,
                    process_may_exit=True,
                )
                if outcome.name != "actual-cutpoint-witness.json":
                    failure = outcome.read_text(encoding="utf-8", errors="replace").strip()
                    self.fail(f"{scenario} did not reach {cutpoint}: {failure}")

                witness = json.loads(outcome.read_text(encoding="utf-8"))
                self.assertEqual(witness["scenario"], scenario)
                self.assertEqual(witness["cutpoint"], cutpoint)
                self.assertIs(witness["A_acknowledged"], True)
                self.assertIs(witness["B_attempted"], True)
                self.assertIs(witness["B_commit_invoked"], True)
                self.assertIs(witness["B_native_commit_returned"], native_commit_returned)
                self.assertIs(witness["B_acknowledged"], False)
                self.assertTrue((results / "actual-A-ack-witness.tsv").is_file())
                self.assertTrue((results / "actual-B-attempt-witness.tsv").is_file())
                self.assertTrue((results / "actual-B-commit-invoked-witness.marker").is_file())
                self.assertTrue((results / "actual-cutpoint-child-witness.txt").is_file())
                self.assertFalse((results / "actual-B-acknowledged-witness.tsv").exists())
                self.assertFalse((results / "actual-powercut-writer-failed").exists())
            finally:
                stop_process(process, signal.SIGTERM, timeout=5)


    def test_mixed_replay_before_native_commit(self):
        self._assert_cutpoint(*SCENARIOS["test_mixed_replay_before_native_commit"])

    def test_dictionary_before_triple_commit(self):
        self._assert_cutpoint(*SCENARIOS["test_dictionary_before_triple_commit"])

    def test_commit_returned_before_ack(self):
        self._assert_cutpoint(*SCENARIOS["test_commit_returned_before_ack"])

    def _assert_running(self, trial, replay):
        with tempfile.TemporaryDirectory(prefix="rdf4j-running-guest-") as temporary:
            root = Path(temporary)
            results = root / "results"
            results.mkdir()
            (results / "actual-powercut-scenario.txt").write_text(trial + "\n", encoding="utf-8")
            (results / "actual-build-classpath-guest.txt").write_text(self.host_classpath + "\n", encoding="utf-8")
            env = {"RDF4J_CRASHLAB_RESULTS_DIR": str(results),
                   "RDF4J_CRASHLAB_DATA_DIR": str(root / "store"),
                   "RDF4J_CRASHLAB_CLASS_DIR": str(self.class_dir),
                   "RDF4J_CRASHLAB_JAVA": self.java}
            with patch.dict(os.environ, env):
                process = start_logged_process([sys.executable, str(CONTROLLER)], results / "controller-process.log")
            try:
                marker = wait_for_path(process, [results / "actual-running-ready.json",
                                                results / "actual-powercut-writer-failed"], timeout=90)
                self.assertEqual(marker.name, "actual-running-ready.json")
                ready = json.loads(marker.read_text())
                self.assertGreaterEqual(ready["returned_generation"], 2)
                self.assertEqual(ready["trial"], trial)
                if replay:
                    self.assertGreater(ready["spilled_replays"], 0)
                self.assertIsNone(process.poll(), "continuous writer must still run")
                self.assertFalse((results / "actual-cutpoint-child.marker").exists())
                wait_for_path(process, [results / "running-3-returned.json"], timeout=90)
            finally:
                stop_process(process, signal.SIGKILL, timeout=5)
            self.assertGreaterEqual(len(list(results.glob("running-*-returned.json"))), 2)
            events = (results / "actual-powercut-writer-events.jsonl").read_text()
            self.assertEqual(events.count('"event": "A_ACK_WITNESS_FSYNCED"'), 1)
            self.assertNotIn("B_ATTEMPT_WITNESS_FSYNCED", events)
            # Process-death coverage uses the same exact oracle, with its weaker filesystem boundary explicit.
            (results / "running-999-returned.json.tmp").write_text('{"unfinished":')
            observations = []
            for index in (1, 2):
                store_copy = root / f"recovery-{index}"
                shutil.copytree(root / "store", store_copy)
                oracle = subprocess.run([self.java, "-cp", str(self.class_dir) + ":" + self.host_classpath,
                                         "org.eclipse.rdf4j.sail.lmdb.CrashPowerCutOracleMain", str(store_copy),
                                         str(results), trial, str(index), "--process-only"],
                                        capture_output=True, text=True, timeout=90)
                self.assertEqual(oracle.returncode, 0, oracle.stdout + oracle.stderr)
                observations.append(json.loads((results / f"actual-recovery-{index}-oracle.json").read_text()))
            self.assertEqual(observations[0]["generation"], observations[1]["generation"])
            self.assertEqual(observations[0]["stateSha256"], observations[1]["stateSha256"])
            from running_contract import read_running_witnesses
            bounds = read_running_witnesses(results, trial)
            generation = observations[0]["generation"]
            optional = root / "optional-frontier-witnesses"
            optional.mkdir()
            for path in results.iterdir():
                if not path.is_file() or path.name.startswith("actual-recovery-"):
                    continue
                match = re.fullmatch(r"running-([0-9]+)-(attempt.tsv|returned.json)", path.name)
                if match and (int(match[1]) > generation or (match[2] == "returned.json" and int(match[1]) == generation)):
                    continue
                shutil.copyfile(path, optional / path.name)
            (optional / f"running-{generation}-returned.json.tmp").write_text('{"unfinished":')
            command = [self.java, "-cp", str(self.class_dir) + ":" + self.host_classpath,
                       "org.eclipse.rdf4j.sail.lmdb.CrashPowerCutOracleMain"]
            accepted = subprocess.run([*command, str(root / "recovery-1"), str(optional), trial, "1", "--process-only"],
                                      capture_output=True, text=True, timeout=90)
            self.assertEqual(accepted.returncode, 0, accepted.stdout + accepted.stderr)
            self.assertEqual(json.loads((optional / "actual-recovery-1-oracle.json").read_text())["generation"], generation)
            for action, message in (("remove-stable", "partial transaction"), ("rewind-generation", "loses acknowledged state")):
                damaged = root / action
                shutil.copytree(root / "recovery-1", damaged)
                changed = subprocess.run([self.java, "-cp", str(self.class_dir) + ":" + self.host_classpath,
                                          "org.eclipse.rdf4j.sail.lmdb.CrashPowerCutOracleTestMain", str(damaged),
                                          action, str(bounds["returned_generation"] - 1)],
                                         capture_output=True, text=True, timeout=90)
                self.assertEqual(changed.returncode, 0, changed.stdout + changed.stderr)
                rejected = subprocess.run([*command, str(damaged), str(results), trial, "1", "--process-only"],
                                          capture_output=True, text=True, timeout=90)
                self.assertNotEqual(rejected.returncode, 0)
                self.assertIn(message, rejected.stderr)

    def test_continuous_preflight_writer_has_no_generation_controller_release(self):
        self._assert_running("running-normal-preflight-write", replay=False)

    def test_continuous_replay_writer_observes_spilled_replay_without_pausing(self):
        self._assert_running("running-journal-replay-write", replay=True)

    def test_continuous_preflight_persistence_trial(self):
        self._assert_running("running-normal-preflight-persistence", replay=False)

    def test_continuous_replay_persistence_trial(self):
        self._assert_running("running-journal-replay-persistence", replay=True)


def main():
    parser = argparse.ArgumentParser(description="Exercise the compiled LMDB QEMU guest writer cutpoints.")
    parser.add_argument("--host-classpath-file", type=Path, required=True)
    arguments, unittest_arguments = parser.parse_known_args()
    GuestWriterCutpointTests.configure(arguments.host_classpath_file)
    unittest.main(argv=[sys.argv[0], *unittest_arguments])


if __name__ == "__main__":
    main()
