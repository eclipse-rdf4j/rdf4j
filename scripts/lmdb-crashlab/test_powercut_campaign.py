import importlib.util
import json
import os
from pathlib import Path
import stat
import tempfile
import unittest
from unittest.mock import patch

from run_powercut_campaign import (
    SCENARIOS,
    validate_cutpoint_witness,
    validate_recovery_pair,
)


class GuestWitnessPublicationTests(unittest.TestCase):
    def load_controller(self, root):
        (root / "actual-build-classpath-guest.txt").write_text("test-classpath\n", encoding="utf-8")
        (root / "actual-powercut-scenario.txt").write_text("commit-returned-before-ack\n", encoding="utf-8")
        controller_path = Path(__file__).parent / "guest/powercut-writer-controller.py"
        spec = importlib.util.spec_from_file_location("powercut_writer_controller", controller_path)
        controller = importlib.util.module_from_spec(spec)
        with patch.dict(os.environ, {"RDF4J_CRASHLAB_RESULTS_DIR": str(root),
                                    "RDF4J_CRASHLAB_CLASS_DIR": str(root)}):
            spec.loader.exec_module(controller)
        return controller

    def test_witness_is_not_visible_before_complete_json_is_fsynced(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            controller = self.load_controller(root)
            marker = root / "actual-cutpoint-witness.json"
            value = {"scenario": "commit-returned-before-ack", "nested": {"rows": [1, "snow-雪"]}}
            fsync = os.fsync
            file_forced = False
            directory_forced = False

            def force(fd):
                nonlocal file_forced, directory_forced
                if stat.S_ISREG(os.fstat(fd).st_mode):
                    self.assertFalse(marker.exists(), "host must not observe a witness before its data is forced")
                    file_forced = True
                else:
                    self.assertTrue(file_forced)
                    self.assertEqual(json.loads(marker.read_text(encoding="utf-8")), value)
                    directory_forced = True
                fsync(fd)

            with patch.object(controller.os, "fsync", side_effect=force):
                controller.write_exclusive(marker, value)
            self.assertTrue(file_forced)
            self.assertTrue(directory_forced)
            self.assertEqual(json.loads(marker.read_text(encoding="utf-8")), value)
            self.assertEqual(sorted(path.name for path in root.iterdir()), [
                "actual-build-classpath-guest.txt", "actual-cutpoint-witness.json", "actual-powercut-scenario.txt",
            ])

    def test_failed_force_does_not_publish_partial_witness(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            controller = self.load_controller(root)
            marker = root / "actual-cutpoint-witness.json"
            with patch.object(controller.os, "fsync", side_effect=OSError("force failed")):
                with self.assertRaisesRegex(OSError, "force failed"):
                    controller.write_exclusive(marker, {"scenario": "commit-returned-before-ack"})
            self.assertFalse(marker.exists())
            self.assertEqual(sorted(path.name for path in root.iterdir()), [
                "actual-build-classpath-guest.txt", "actual-powercut-scenario.txt",
            ])

    def test_preexisting_witness_is_preserved(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            controller = self.load_controller(root)
            marker = root / "actual-cutpoint-witness.json"
            marker.write_text("previous evidence\n", encoding="utf-8")
            with self.assertRaises(FileExistsError):
                controller.write_exclusive(marker, {"scenario": "commit-returned-before-ack"})
            self.assertEqual(marker.read_text(encoding="utf-8"), "previous evidence\n")
            self.assertEqual(sorted(path.name for path in root.iterdir()), [
                "actual-build-classpath-guest.txt", "actual-cutpoint-witness.json", "actual-powercut-scenario.txt",
            ])

    def test_serialization_failure_does_not_publish_partial_witness(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            controller = self.load_controller(root)
            marker = root / "actual-cutpoint-witness.json"
            with self.assertRaises(TypeError):
                controller.write_exclusive(marker, {"unsupported": object()})
            self.assertFalse(marker.exists())
            self.assertEqual(sorted(path.name for path in root.iterdir()), [
                "actual-build-classpath-guest.txt", "actual-powercut-scenario.txt",
            ])

    def test_preexisting_dangling_symlink_is_preserved(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            controller = self.load_controller(root)
            marker = root / "actual-cutpoint-witness.json"
            marker.symlink_to("missing-evidence")
            with self.assertRaises(FileExistsError):
                controller.write_exclusive(marker, {"scenario": "commit-returned-before-ack"})
            self.assertTrue(marker.is_symlink())
            self.assertEqual(os.readlink(marker), "missing-evidence")

    def test_publication_failure_removes_only_owned_temporary_file(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            controller = self.load_controller(root)
            marker = root / "actual-cutpoint-witness.json"
            with patch.object(controller.os, "link", side_effect=OSError("publication failed")):
                with self.assertRaisesRegex(OSError, "publication failed"):
                    controller.write_exclusive(marker, {"scenario": "commit-returned-before-ack"})
            self.assertFalse(marker.exists())
            self.assertEqual(sorted(path.name for path in root.iterdir()), [
                "actual-build-classpath-guest.txt", "actual-powercut-scenario.txt",
            ])


class PowerCutContractTests(unittest.TestCase):
    def witness(self, scenario):
        contract = SCENARIOS[scenario]
        witness = {
            "scenario": scenario,
            "cutpoint": contract["cutpoint"],
            "A_acknowledged": True,
            "B_attempted": True,
            "B_commit_invoked": contract["commit_invoked"],
            "B_native_commit_returned": contract["native_commit_returned"],
            "B_acknowledged": False,
        }
        if scenario == "mixed-replay-before-native-commit":
            witness["frozen_replay"] = {
                "frozen_starts": 2,
                "main_capacity_failures": 1,
                "successful_physical_aborts": 1,
                "actual_map_growths": 1,
                "map_before_bytes": 1024,
                "map_after_bytes": 2048,
                "mutation_journal_allocations": 0,
                "explicit_input_rows": 4,
                "inferred_input_rows": 1,
                "expected_explicit_input_rows": 4,
                "expected_inferred_input_rows": 1,
                "current_frozen_decision": True,
                "native_commit_attempts": 0,
                "native_commit_returned": False,
                "marker_sha256": "2" * 64,
            }
        return witness

    def result(self, outcome, digest="same-complete-state"):
        return {"outcome": outcome, "stateSha256": digest}

    def test_replay_cut_is_invoked_before_native_commit_and_requires_exact_old_state(self):
        validate_cutpoint_witness("mixed-replay-before-native-commit",
                                  self.witness("mixed-replay-before-native-commit"),
                                  b_acknowledged=False)
        validate_recovery_pair("mixed-replay-before-native-commit", self.result("A"), self.result("A"))
        with self.assertRaises(ValueError):
            validate_cutpoint_witness("mixed-replay-before-native-commit", {
                **self.witness("mixed-replay-before-native-commit"), "B_commit_invoked": False,
            }, b_acknowledged=False)
        with self.assertRaises(ValueError):
            validate_cutpoint_witness("mixed-replay-before-native-commit", {
                **self.witness("mixed-replay-before-native-commit"), "frozen_replay": None,
            }, b_acknowledged=False)
        invalid_frozen = {**self.witness("mixed-replay-before-native-commit")["frozen_replay"],
                          "mutation_journal_allocations": 1}
        with self.assertRaises(ValueError):
            validate_cutpoint_witness("mixed-replay-before-native-commit", {
                **self.witness("mixed-replay-before-native-commit"), "frozen_replay": invalid_frozen,
            }, b_acknowledged=False)

    def test_dictionary_cut_is_after_invocation_but_before_native_commit(self):
        validate_cutpoint_witness("dictionary-before-triple-commit",
                                  self.witness("dictionary-before-triple-commit"), b_acknowledged=False)
        validate_recovery_pair("dictionary-before-triple-commit", self.result("A"), self.result("A"))
        with self.assertRaises(ValueError):
            validate_recovery_pair("dictionary-before-triple-commit",
                                   self.result("A_PLUS_B"), self.result("A_PLUS_B"))

    def test_returned_commit_is_unknown_but_repeated_outcome_must_match(self):
        validate_cutpoint_witness("commit-returned-before-ack", self.witness("commit-returned-before-ack"),
                                  b_acknowledged=False)
        validate_recovery_pair("commit-returned-before-ack", self.result("A_PLUS_B"), self.result("A_PLUS_B"))
        with self.assertRaises(ValueError):
            validate_recovery_pair("commit-returned-before-ack", self.result("A"), self.result("A"))
        with self.assertRaises(ValueError):
            validate_recovery_pair("commit-returned-before-ack", self.result("A"), self.result("A_PLUS_B"))

    def test_b_acknowledgment_is_never_permitted_at_power_cut(self):
        with self.assertRaises(ValueError):
            validate_cutpoint_witness("commit-returned-before-ack", self.witness("commit-returned-before-ack"),
                                      b_acknowledged=True)

    def test_two_recoveries_must_match_complete_state_hash(self):
        with self.assertRaises(ValueError):
            validate_recovery_pair("commit-returned-before-ack", self.result("A", "hash-1"),
                                   self.result("A", "hash-2"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
