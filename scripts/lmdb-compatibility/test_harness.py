#!/usr/bin/env python3
"""No-Maven tests of independent expected-state generation and reproducibility."""
import argparse
import gzip
import importlib.util
import json
import sys
import subprocess
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "run-lmdb-version-roundtrip.py"
SPEC = importlib.util.spec_from_file_location("roundtrip", SCRIPT)
HARNESS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(HARNESS)


class JournalTests(unittest.TestCase):
    def test_direct_maintenance_plan_opens_authentic_v2_before_any_optimize_write(self):
        args = argparse.Namespace(seeds="17011", scenarios="full-corpus", rounds=4, statements=8,
                                  payload_bytes=8, optimize_modes="normal,native,adjacency,overlay",
                                  stop_after_stage=None, direct_maintenance=True)
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-direct-plan-") as scratch:
            output = Path(scratch)
            jobs = HARNESS.plan_campaign(output, args)
            self.assertEqual(24, len(jobs))
            self.assertEqual({"v2"}, {job["format"] for job in jobs})
            self.assertNotIn("optimize-first", {job["stage"] for job in jobs})
            self.assertFalse((output / "repositories").exists())
            for store in {job["store"] for job in jobs}:
                chain = [job for job in jobs if job["store"] == store]
                self.assertEqual(["release", "current", "optimize"],
                                 list(dict.fromkeys(job["runtime"] for job in chain)))
                first, maintenance = chain[:2]
                expected = sorted(Path(first["journal"]).glob("expected-*.tsv.gz"))[-1]
                self.assertEqual(HARNESS.load_expected(expected), HARNESS.load_expected(
                    Path(maintenance["journal"]) / "expected-000-entry.tsv.gz"))
                self.assertTrue((Path(maintenance["journal"]) / "operations.tsv").read_text().startswith("V\t"))

    def test_direct_maintenance_cli_records_the_selected_path(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-direct-cli-") as scratch:
            output = Path(scratch) / "direct"
            command = [sys.executable, str(SCRIPT), "--output-dir", str(output), "--direct-maintenance",
                       "--seeds", "17011", "--rounds", "1", "--statements", "8", "--payload-bytes", "8",
                       "--scenarios", "structural-control", "--optimize-modes", "normal", "--plan-only"]
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
            selection = json.loads((output / "direct-selection.json").read_text())
            self.assertEqual(["release", "current", "optimize"], selection["runtime_path"])
            self.assertEqual(18, selection["jobs"])
            rejected = subprocess.run([sys.executable, str(SCRIPT), "--output-dir", str(Path(scratch) / "bad"),
                                       "--writer-corpus", "--direct-maintenance", "--plan-only"],
                                      capture_output=True, text=True)
            self.assertEqual(2, rejected.returncode)
            self.assertIn("fixed bounded selection", rejected.stderr)

    def original_v6_inputs(self, root):
        original = root / "original"
        original.mkdir()
        runtime = original / "runtime"
        runtime.mkdir()
        jar = runtime / "lmdb.jar"
        jar.write_bytes(b"original immutable optimize runtime")
        archive = original / "source.tar.gz"
        archive.write_bytes(b"original optimize source archive")
        provenance = {"runtime_dir": str(runtime), "revision": HARNESS.SOURCES["optimize"],
                      "classpath": [str(jar)], "lmdb_sha256": HARNESS.sha256(jar),
                      "artifacts": [{"path": str(jar), "sha256": HARNESS.sha256(jar),
                                     "size_bytes": jar.stat().st_size}],
                      "source_archive": str(archive), "source_archive_sha256": HARNESS.sha256(archive)}
        HARNESS.write_json(original / "runtime-provenance.json", {"optimize": provenance})
        name = "seed-17011/full-corpus/v6-optimized-inline-false-ordered-false"
        store = original / "failure-snapshots" / name / "optimize-first" / "overlay"
        (store / "values").mkdir(parents=True)
        (store / "values" / "data.mdb").write_bytes(b"physical original v6 bytes")
        (store / "store.properties").write_text("version=6\nunknown=preserve\n")
        expected = HARNESS.Journal(original / "journals" / name / "optimize-first" / "overlay")
        row = HARNESS.statement(HARNESS.iri("urn:original"), HARNESS.iri("urn:p"), HARNESS.literal("EN", language="EN"))
        expected.begin("SNAPSHOT")
        expected.add(row)
        expected.namespace("original", "urn:original:")
        expected.finish(True)
        expected.check("exit")
        expected.save({})
        confirmed = expected.directory / "expected-000-confirmed-commit.tsv.gz"
        result = {"repository": name, "seed": 17011, "scenario": "full-corpus", "format": "v6",
                  "inline": False, "ordered": False, "profile": "optimized", "stage": "optimize-first",
                  "runtime": "optimize", "mode": "overlay", "mode_index": 3, "operations_completed": True,
                  "status": "failed", "failure_kind": "comparison", "journal": str(expected.directory),
                  "confirmed_expected_state": str(confirmed), "failed_stage_physical_snapshot": str(store),
                  "command": ["java", provenance["revision"], provenance["lmdb_sha256"]]}
        HARNESS.write_json(original / "results.json", [result])
        return original, result, row

    def test_original_v6_plan_uses_confirmed_oracle_and_copies_exact_bytes(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-original-plan-") as scratch:
            root = Path(scratch)
            original, result, row = self.original_v6_inputs(root)
            output = root / "patched"
            output.mkdir()
            args = argparse.Namespace(original_v6_results=original / "results.json", seeds="17011",
                                      scenarios="full-corpus", rounds=4, statements=8, payload_bytes=8,
                                      optimize_modes="normal,native,adjacency,overlay", stop_after_stage=None)
            jobs = HARNESS.plan_original_v6_campaign(output, args)
            self.assertEqual(5, len(jobs))
            self.assertEqual("current", jobs[0]["runtime"])
            self.assertEqual(["normal", "native", "adjacency", "overlay"],
                             [job["mode"] for job in jobs[1:]])
            self.assertEqual(({row}, {"original": "urn:original:"}), HARNESS.load_expected(
                Path(jobs[0]["journal"]) / "expected-000-entry.tsv.gz"))
            self.assertFalse(Path(jobs[0]["store"]).exists())
            copied = HARNESS.copy_original_store(jobs[0], output)
            self.assertEqual(HARNESS.physical_inventory(Path(result["failed_stage_physical_snapshot"])),
                             HARNESS.physical_inventory(Path(jobs[0]["store"])))
            self.assertEqual("copied", copied["status"])
            (Path(jobs[0]["store"]) / "store.properties").write_text("changed only copied directory")
            self.assertEqual("version=6\nunknown=preserve\n", (Path(result["failed_stage_physical_snapshot"]) /
                                                             "store.properties").read_text())

    def test_original_v6_plan_rejects_uncertain_or_modified_inputs(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-original-reject-") as scratch:
            root = Path(scratch)
            original, result, _ = self.original_v6_inputs(root)
            args = argparse.Namespace(original_v6_results=original / "results.json", seeds="17011",
                                      scenarios="full-corpus", rounds=1, statements=8, payload_bytes=8,
                                      optimize_modes="normal", stop_after_stage=None)
            output = root / "patched"
            output.mkdir()
            result["operations_completed"] = False
            HARNESS.write_json(original / "results.json", [result])
            with self.assertRaisesRegex(ValueError, "confirmed completed"):
                HARNESS.plan_original_v6_campaign(output, args)
            result["operations_completed"] = True
            HARNESS.write_json(original / "results.json", [result])
            Path(result["confirmed_expected_state"]).write_bytes(b"modified expected oracle")
            with self.assertRaisesRegex(ValueError, "expected snapshot.*manifest"):
                HARNESS.plan_original_v6_campaign(output, args)

    def test_original_v6_plan_chooses_preserved_failure_copy_before_later_passed_session(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-original-passed-") as scratch:
            root = Path(scratch)
            original, result, _ = self.original_v6_inputs(root)
            later = {**result, "mode_index": 4, "status": "passed", "failure_kind": None}
            later.pop("failed_stage_physical_snapshot")
            HARNESS.write_json(original / "results.json", [result, later])
            output = root / "patched"
            output.mkdir()
            args = argparse.Namespace(original_v6_results=original / "results.json", seeds="17011",
                                      scenarios="full-corpus", rounds=1, statements=8, payload_bytes=8,
                                      optimize_modes="normal", stop_after_stage=None)
            jobs = HARNESS.plan_original_v6_campaign(output, args)
            self.assertEqual(Path(result["failed_stage_physical_snapshot"]).resolve(), Path(jobs[0]["original_store"]))

    def test_writer_cases_are_fresh_by_phase_and_retain_fraction_spellings(self):
        previous = {case["term"] for case in HARNESS.corpus()}
        for stage in HARNESS.WRITER_STAGES:
            cases = HARNESS.writer_corpus_cases(stage)
            terms = {case["term"] for case in cases}
            self.assertEqual(len(cases), len(terms))
            self.assertFalse(previous & terms, stage)
            self.assertTrue(all(case["source_case"] and case["phase"] == stage for case in cases))
            categories = {case["category"] for case in cases}
            self.assertTrue({"boolean", "string", "language", "direction", "decimal", "float", "double",
                             "calendar-dateTime", "calendar-dateTimeStamp"}.issubset(categories))
            for datatype in ("dateTime", "dateTimeStamp"):
                calendars = [case for case in cases if case["datatype"] == HARNESS.XSD + datatype]
                for fraction in (".0Z", ".00Z", ".000Z"):
                    self.assertTrue(any(case["label"].endswith(fraction) for case in calendars), (stage, fraction))
            for case in cases:
                if case["query_numeric"] and case["category"].startswith("integer-"):
                    value = int(case["label"])
                    if case["category"] == "integer-byte":
                        self.assertTrue(-128 <= value <= 127)
                    if case["category"] == "integer-unsignedByte":
                        self.assertTrue(0 <= value <= 255)
                    if case["category"] == "integer-negativeInteger":
                        self.assertLess(value, 0)
                    if case["category"] == "integer-positiveInteger":
                        self.assertGreater(value, 0)
            previous |= terms

    def test_writer_campaign_is_bounded_and_retires_every_fixed_reference(self):
        args = argparse.Namespace(writer_corpus=True)
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-writer-plan-") as scratch:
            output = Path(scratch)
            jobs = HARNESS.plan_campaign(output, args)
            self.assertEqual(20, len(jobs))
            self.assertEqual(6, len({job["store"] for job in jobs}))
            self.assertEqual({"normal"}, {job["mode"] for job in jobs})
            self.assertEqual({"writer-corpus"}, {job["scenario"] for job in jobs})
            self.assertFalse((output / "repositories").exists())
            fixed = set(HARNESS.fixed_corpus_rows(HARNESS.corpus()))
            for job in jobs:
                directory = Path(job["journal"])
                wire = (directory / "operations.tsv").read_text().splitlines()
                self.assertTrue(wire[0].startswith("V\t"))
                self.assertTrue(fixed.issubset({line[2:] for line in wire if line.startswith("D\t")}))
                manifest = json.loads((directory / "expected-manifest.json").read_text())
                self.assertIn("writer-cases.tsv", manifest)
                fresh = HARNESS.writer_corpus_cases(job["stage"])
                self.assertEqual(len(fresh), sum(line.startswith("I\tbefore\t") for line in wire))
                self.assertEqual(len(fresh), sum(line.startswith("I\tafter\t") for line in wire))
                isolation, held = None, False
                for line in wire:
                    if line.startswith("SB\t"):
                        held = True
                    elif line.startswith("SE\t"):
                        held = False
                    elif line.startswith("B\t"):
                        isolation = line.split("\t")[1]
                        if held:
                            self.assertEqual("SNAPSHOT", isolation)
                    elif line == "R":
                        self.assertEqual("SNAPSHOT", isolation)
                self.assertIn("B\tNONE", wire)
                self.assertIn("B\tSNAPSHOT", wire)
            for store in {job["store"] for job in jobs}:
                chain = [job for job in jobs if job["store"] == store]
                self.assertEqual([job["stage"] for job in chain],
                                 list(HARNESS.WRITER_STAGES) if chain[0]["format"] == "v2"
                                 else list(HARNESS.WRITER_STAGES)[1:])

    def test_writer_cli_keeps_the_bounded_selection_explicit(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-writer-cli-") as scratch:
            output = Path(scratch) / "writer"
            result = subprocess.run([sys.executable, str(SCRIPT), "--output-dir", str(output),
                                     "--writer-corpus", "--plan-only"], capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual(20, len(json.loads((output / "campaign-plan.json").read_text())))
            selection = json.loads((output / "writer-selection.json").read_text())
            self.assertEqual(6, selection["repositories"])
            self.assertEqual(["normal"], selection["optimizer_modes"])
            self.assertEqual(32, selection["mutable_rows"])
            self.assertTrue((output / "harness-source-provenance.json").is_file())
            rejected = subprocess.run([sys.executable, str(SCRIPT), "--output-dir", str(Path(scratch) / "bad"),
                                       "--writer-corpus", "--rounds", "3", "--plan-only"], capture_output=True, text=True)
            self.assertEqual(2, rejected.returncode)
            self.assertIn("fixed bounded selection", rejected.stderr)

    def test_writer_recovery_uses_only_the_confirmed_independent_snapshot(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-writer-recovery-") as scratch:
            output = Path(scratch)
            job = HARNESS.plan_campaign(output, argparse.Namespace(writer_corpus=True))[0]
            journal = json.loads((Path(job["journal"]) / "journal.json").read_text())
            commits = [event["arguments"][0] for event in journal["operations"] if event["operation"] == "C"]
            confirmed = Path(job["journal"]) / commits[1]  # Fixed references genuinely absent after confirmed delete.
            state, namespaces = HARNESS.load_expected(confirmed)
            fixed = set(HARNESS.fixed_corpus_rows(HARNESS.corpus()))
            self.assertFalse(state & fixed)
            directory = output / "recovery"
            HARNESS.make_journal(directory, state, namespaces, job, argparse.Namespace(), False, [])
            self.assertEqual((state, namespaces), HARNESS.load_expected(directory / "expected-000-entry.tsv.gz"))
            self.assertTrue(fixed.issubset(HARNESS.load_expected(sorted(directory.glob("*fixed-readd*"))[0])[0]))

    def test_checker_selection_bypasses_all_repository_oracle_planning(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-checker-plan-") as scratch:
            output = Path(scratch) / "checker"
            result = subprocess.run([sys.executable, str(SCRIPT), "--output-dir", str(output),
                                     "--checker-only", "--plan-only"], capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual([], json.loads((output / "campaign-plan.json").read_text()))
            self.assertFalse((output / "journals").exists())
            self.assertTrue((output / "harness-source-provenance.json").exists())

    def test_owned_timeout_preserves_command_and_commit_evidence(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-timeout-") as scratch:
            root = Path(scratch)
            (root / "confirmed-commits.tsv").write_text("3\texpected-confirmed.tsv.gz\n")
            rc = HARNESS.command_run([sys.executable, "-c", "import time; time.sleep(30)"], root / "run.log", timeout=0.1)
            self.assertEqual(124, rc)
            timeout = json.loads((root / "run.timeout.json").read_text())
            self.assertEqual("uncertain", timeout["committed_state"])
            self.assertIn("owned_pid", timeout)
            self.assertEqual("3\texpected-confirmed.tsv.gz\n", (root / "timeout-last-confirmed-commits.tsv").read_text())

    def test_runtime_manifest_rejects_modified_mixed_and_escaped_jars(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-manifest-") as scratch:
            root = Path(scratch)
            runtime_dir = root / "runtime"
            runtime_dir.mkdir()
            jar = runtime_dir / "library.jar"
            jar.write_bytes(b"recorded immutable jar")
            archive = root / "source.tar.gz"
            archive.write_bytes(b"source archive")
            runtime = {"runtime_dir": str(runtime_dir), "revision": "pinned", "classpath": [str(jar)],
                       "artifacts": [{"path": str(jar), "sha256": HARNESS.sha256(jar), "size_bytes": jar.stat().st_size}],
                       "source_archive": str(archive), "source_archive_sha256": HARNESS.sha256(archive)}
            HARNESS.validate_runtime("release", runtime, "pinned")
            jar.write_bytes(b"modified immutable jar")
            with self.assertRaisesRegex(ValueError, "differs from recorded manifest"):
                HARNESS.validate_runtime("release", runtime, "pinned")
            jar.write_bytes(b"recorded immutable jar")
            runtime["classpath"].append(str(root / "mixed.jar"))
            with self.assertRaisesRegex(ValueError, "exactly match"):
                HARNESS.validate_runtime("release", runtime, "pinned")
            runtime["classpath"] = [str(jar)]
            outside = root / "outside.jar"
            outside.write_bytes(jar.read_bytes())
            original_manifest = runtime["artifacts"]
            runtime["classpath"] = [str(outside)]
            runtime["artifacts"] = [{"path": str(outside), "sha256": HARNESS.sha256(outside),
                                      "size_bytes": outside.stat().st_size}]
            with self.assertRaisesRegex(ValueError, "escapes"):
                HARNESS.validate_runtime("release", runtime, "pinned")
            runtime["classpath"], runtime["artifacts"] = [str(jar)], original_manifest
            archive.write_bytes(b"changed source")
            with self.assertRaisesRegex(ValueError, "source archive differs"):
                HARNESS.validate_runtime("release", runtime, "pinned")

    def test_commit_duplicate_delete_and_rollback_have_exact_expected_snapshots(self):
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-journal-") as scratch:
            journal = HARNESS.Journal(Path(scratch) / "journal")
            row = HARNESS.statement(HARNESS.blank("stable"), HARNESS.iri("urn:p"),
                                    HARNESS.literal("042", HARNESS.XSD + "integer"), HARNESS.iri("urn:graph"))
            journal.begin("NONE")
            journal.add(row)
            journal.add(row)
            journal.namespace("test", "urn:test:")
            journal.finish(True)
            journal.check("committed")
            journal.begin("SNAPSHOT")
            journal.remove(row)
            journal.namespace("test", None)
            journal.finish(False)
            journal.check("rollback")
            state, namespaces = journal.save({"seed": 17})
            self.assertEqual({row}, state)
            self.assertEqual({"test": "urn:test:"}, namespaces)
            snapshots = list(journal.directory.glob("expected-*.tsv.gz"))
            self.assertEqual(3, len(snapshots))  # independent commit, check, rollback-check
            self.assertTrue(all(path.read_bytes() == snapshots[0].read_bytes() for path in snapshots))
            contents = gzip.decompress(snapshots[0].read_bytes()).decode()
            self.assertEqual(1, sum(line.startswith("S\t") for line in contents.splitlines()))
            manifest = json.loads((journal.directory / "expected-manifest.json").read_text())
            self.assertEqual(HARNESS.sha256(snapshots[0]), manifest[snapshots[0].name])

    def test_corpus_keeps_zero_fraction_lexical_identity_and_raw_language_case(self):
        cases = HARNESS.corpus()
        self.assertEqual(len(cases), len({case["id"] for case in cases}))
        date_terms = {case["label"]: case["term"] for case in cases
                      if case["datatype"] == HARNESS.XSD + "dateTime"}
        labels = ["2026-10-03T12:00:01" + fraction + "Z" for fraction in ("", ".0", ".00", ".000")]
        self.assertEqual(4, len({date_terms[label] for label in labels}))
        languages = {case["language"] for case in cases if case["category"] == "language"}
        self.assertTrue({"en", "EN", "eN-us"}.issubset(languages))
        self.assertTrue(any(case["direction"] == "RTL" for case in cases))
        self.assertTrue(any(case["label"] == "18446744073709551615" for case in cases))
        calendars = [case for case in cases if case["query_calendar"]]
        self.assertTrue(any(case["label"].endswith(".00Z") for case in calendars))
        self.assertTrue(any(case["label"].endswith(".123456789+00:01") for case in calendars))
        self.assertFalse(any(case["datatype"] == HARNESS.XSD + "dateTimeStamp" and
                             case["label"].endswith("12:00:01") for case in calendars))

    def test_complete_matrix_uses_same_directories_and_separate_immutable_expectations(self):
        args = argparse.Namespace(seeds="17011", scenarios="structural-control", rounds=4, statements=8,
                                  payload_bytes=8, optimize_modes="normal,native,adjacency,overlay", stop_after_stage=None)
        with tempfile.TemporaryDirectory(prefix="lmdb-compat-plan-") as scratch:
            output = Path(scratch)
            jobs = HARNESS.plan_campaign(output, args)
            self.assertEqual(72, len(jobs))  # 56 core jobs plus sixteen named A/B edge jobs
            self.assertFalse((output / "repositories").exists())
            v2 = [j for j in jobs if j["scenario"] == "structural-control" and j["format"] == "v2"]
            self.assertEqual(2, len({j["store"] for j in v2}))
            for store in {j["store"] for j in v2}:
                phases = [j for j in v2 if j["store"] == store]
                self.assertEqual({"release-create", "optimize-first", "current-downgrade", "optimize-reupgrade"},
                                 {j["stage"] for j in phases})
                self.assertEqual({"normal", "native", "adjacency", "overlay"},
                                 {j["mode"] for j in phases if j["runtime"] == "optimize"})
            for job in jobs:
                operations = (Path(job["journal"]) / "operations.tsv").read_text().splitlines()
                self.assertTrue(operations[0].startswith("V\t"))
                self.assertTrue((Path(job["journal"]) / "expected-manifest.json").is_file())
                isolation = None
                held_snapshot = False
                for operation in operations:
                    if operation.startswith("SB\t"):
                        held_snapshot = True
                    elif operation.startswith("SE\t"):
                        held_snapshot = False
                    if operation.startswith("B\t"):
                        isolation = operation.split("\t")[1]
                        if held_snapshot:
                            self.assertEqual("SNAPSHOT", isolation)
                    if operation == "R":
                        self.assertEqual("SNAPSHOT", isolation)
            # Every initial expected snapshot is empty; it was constructed without
            # repository files or observing any actual exported statement.
            first = [j for j in jobs if j["stage_index"] == 0 and j["mode_index"] == 0]
            for job in first:
                snapshot = Path(job["journal"]) / "expected-000-entry.tsv.gz"
                self.assertEqual(b"\n", gzip.decompress(snapshot.read_bytes()))


if __name__ == "__main__":
    unittest.main()
