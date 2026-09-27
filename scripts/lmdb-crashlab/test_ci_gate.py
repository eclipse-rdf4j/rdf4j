import json
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

from ci_gate import (
    FAILSAFE_IT_CLASSES,
    POWER_CUT_SCENARIOS,
    SUREFIRE_TEST_CLASSES,
    validate_campaign_reports,
    validate_selected_test_reports,
)
from run_ci_campaigns import build_campaign_plan
from run_powercut_campaign import SCENARIOS


def calibration_report():
    return {
        "result": "PASS",
        "baseline_flush_done_count": 1,
        "baseline_fua_done_count": 0,
        "raw_overwrite_ack_before_cut": {"event": "WRITE", "length": 4096, "fua": False},
        "durability_events_after_raw_overwrite_ack": [],
        "device_cut": {"off": True, "survival": "drop", "kept_sectors": 0},
        "pre_recovery_image": {"sha256": "a" * 64},
        "recovered_file_oracle": "PASS synced baseline remained exact; volatile overwrite absent",
        "read_only_inputs_unchanged": True,
    }


def namespace_report():
    return {
        "result": "PUBLIC_ORACLE_PASS",
        "recovery_oracle_findings": [],
        "public_index_context_sparql_checks_completed": True,
        "writer_backend_flush_done_count": 1,
        "writer_backend_fua_done_count": 0,
        "namespace_only_commit": "commit returned; witness was fsynced to independent share; no guest filesystem sync before cut",
        "last_data_write_followed_by_no_flush_or_fua": True,
        "post_ack_action_order": ["read_backend_status", "nbd_cut", "sigkill_guest"],
        "writer_guest_alive_when_nbd_cut_started": True,
        "writer_guest_reaped_after_nbd_cut": True,
        "data_device_cut": {"off": True, "survival": "drop", "kept_sectors": 0},
        "external_witness": {"sha256": "b" * 64},
        "namespace_witness": {"sha256": "c" * 64},
        "pre_recovery_image": {"sha256": "d" * 64},
        "pre_recovery_image_sha256_after_run": "d" * 64,
        "read_only_source_inputs_unchanged": True,
        "compiled_artifacts_unchanged": True,
        "classpath_artifacts_unchanged": True,
        "os_base_image_unchanged": True,
        "backend_source_unchanged": True,
    }


def powercut_report(scenario):
    contract = SCENARIOS[scenario]
    outcome = "A_PLUS_B" if scenario == "commit-returned-before-ack" else "A"
    oracle = lambda recovery_id: {
        "outcome": outcome,
        "stateSha256": "e" * 64,
        "publicIndexChecks": "COMPLETED",
        "recoveryId": recovery_id,
    }
    return {
        "result": "POWER_CUT_RECOVERY_STABLE",
        "scenario": scenario,
        "scenario_contract": json.loads(json.dumps(contract)),
        "cutpoint_witness": {
            "scenario": scenario,
            "cutpoint": contract["cutpoint"],
            "A_acknowledged": True,
            "B_attempted": True,
            "B_commit_invoked": contract["commit_invoked"],
            "B_native_commit_returned": contract["native_commit_returned"],
            "B_acknowledged": False,
        },
        "cut_report": {"off": True, "survival": "drop", "kept_sectors": 0},
        "pre_recovery_image": {"sha256": "f" * 64},
        "pre_recovery_image_sha256_after_both_recoveries": "f" * 64,
        "external_witnesses": {
            name: {"sha256": "1" * 64}
            for name in (
                "actual-A-ack-witness.tsv",
                "actual-B-attempt-witness.tsv",
                "actual-cutpoint-witness.json",
            )
        },
        "writer_nbd_flush_done_count": 1,
        "writer_nbd_fua_done_count": 0,
        "writer_backend_status_before_cut": {"ignore_flush": False},
        "recovery_1": {"oracle": oracle("1")},
        "recovery_2": {"oracle": oracle("2")},
        "same_outcome_on_both_recoveries": True,
        "same_full_state_hash_on_both_recoveries": True,
        "B_acknowledged": False,
    }


class CiGateTests(unittest.TestCase):
    def setUp(self):
        self.reports = [powercut_report(scenario) for scenario in POWER_CUT_SCENARIOS]

    def test_complete_report_matrix_is_accepted(self):
        validate_campaign_reports(calibration_report(), namespace_report(), self.reports)

    def test_ci_campaign_plan_always_runs_calibration_ack_and_all_cuts_under_x86_kvm(self):
        from argparse import Namespace

        args = Namespace(
            repo_root=Path("/checkout"),
            scratch_root=Path("/scratch/crashlab"),
            os_base_image=Path("/images/ubuntu-arm64.qcow2"),
            firmware_code=Path("/firmware/code.fd"),
            firmware_vars_template=Path("/firmware/vars.fd"),
            qemu=Path("/usr/bin/qemu-system-x86_64"),
            qemu_img=Path("/usr/bin/qemu-img"),
            genisoimage=Path("/usr/bin/genisoimage"),
            host_classpath_file=Path("/scratch/classpath.txt"),
            machine="q35",
            accel="kvm",
            cpu="host",
            native_classifier="linux",
            port_base=23000,
        )
        plan = build_campaign_plan(args)
        self.assertEqual([step.name for step in plan], ["calibration", "namespace", *POWER_CUT_SCENARIOS])
        for step in plan:
            self.assertIn("--machine", step.command)
            self.assertEqual(step.command[step.command.index("--machine") + 1], "q35")
            self.assertIn("--accel", step.command)
            self.assertEqual(step.command[step.command.index("--accel") + 1], "kvm")
            self.assertIn("--cpu", step.command)
            self.assertEqual(step.command[step.command.index("--cpu") + 1], "host")
            if step.name != "calibration":
                self.assertIn("--native-classifier", step.command)
                self.assertEqual(step.command[step.command.index("--native-classifier") + 1], "linux")

    def test_missing_campaign_and_duplicate_scenario_are_rejected(self):
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), namespace_report(), self.reports[:-1])
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), namespace_report(),
                                      self.reports + [self.reports[0]])

    def test_failed_or_incomplete_calibration_is_rejected(self):
        report = calibration_report()
        report["result"] = "RECOVERY_FAILED"
        with self.assertRaises(ValueError):
            validate_campaign_reports(report, namespace_report(), self.reports)
        report = calibration_report()
        report["durability_events_after_raw_overwrite_ack"] = [{"event": "FLUSH_DONE"}]
        with self.assertRaises(ValueError):
            validate_campaign_reports(report, namespace_report(), self.reports)

    def test_skipped_or_incomplete_namespace_campaign_is_rejected(self):
        report = namespace_report()
        report["result"] = "SKIPPED"
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), report, self.reports)

    def test_namespace_campaign_requires_unchanged_image_and_public_checks(self):
        report = namespace_report()
        report["pre_recovery_image_sha256_after_run"] = "e" * 64
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), report, self.reports)
        report = namespace_report()
        report["public_index_context_sparql_checks_completed"] = False
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), report, self.reports)

    def test_namespace_campaign_accepts_commit_flush_before_live_guest_cut(self):
        report = namespace_report()
        report["last_data_write_followed_by_no_flush_or_fua"] = False
        report["post_ack_action_order"] = ["read_backend_status", "nbd_cut", "sigkill_guest"]
        report["writer_guest_alive_when_nbd_cut_started"] = True
        report["writer_guest_reaped_after_nbd_cut"] = True
        validate_campaign_reports(calibration_report(), report, self.reports)

    def test_namespace_campaign_rejects_post_ack_guest_sync_before_cut(self):
        report = namespace_report()
        report["post_ack_action_order"] = [
            "read_backend_status", "guest_filesystem_sync", "nbd_cut", "sigkill_guest"
        ]
        report["writer_guest_alive_when_nbd_cut_started"] = True
        report["writer_guest_reaped_after_nbd_cut"] = True
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), report, self.reports)

        report = namespace_report()
        report["post_ack_action_order"] = ["read_backend_status", "nbd_cut", "sigkill_guest"]
        report["writer_guest_alive_when_nbd_cut_started"] = False
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), report, self.reports)

    def test_wrong_scenario_outcome_or_incomplete_recovery_is_rejected(self):
        wrong_name = [dict(report) for report in self.reports]
        wrong_name[0]["scenario"] = "unexpected-scenario"
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), namespace_report(), wrong_name)
        returned = next(report for report in self.reports
                        if report["scenario"] == "commit-returned-before-ack")
        returned["recovery_1"]["oracle"]["outcome"] = "A"
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), namespace_report(), self.reports)
        returned["recovery_1"]["oracle"]["outcome"] = "A_PLUS_B"
        returned["recovery_2"]["oracle"].pop("stateSha256")
        with self.assertRaises(ValueError):
            validate_campaign_reports(calibration_report(), namespace_report(), self.reports)

    def test_selected_java_reports_must_be_present_and_have_zero_skips(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            surefire = root / "surefire"
            failsafe = root / "failsafe"
            surefire.mkdir()
            failsafe.mkdir()
            for class_name in SUREFIRE_TEST_CLASSES:
                (surefire / f"TEST-org.example.{class_name}.xml").write_text(
                    f'<testsuite name="org.example.{class_name}" tests="1" failures="0" '
                    'errors="0" skipped="0"/>\n', encoding="utf-8")
            for class_name in FAILSAFE_IT_CLASSES:
                (failsafe / f"TEST-org.example.{class_name}.xml").write_text(
                    f'<testsuite name="org.example.{class_name}" tests="1" failures="0" '
                    'errors="0" skipped="0"/>\n', encoding="utf-8")
            validate_selected_test_reports(surefire, failsafe)
            selected = SUREFIRE_TEST_CLASSES[0]
            path = next(surefire.glob(f"*{selected}.xml"))
            path.write_text(path.read_text(encoding="utf-8").replace('skipped="0"', 'skipped="1"'),
                            encoding="utf-8")
            with self.assertRaises(ValueError):
                validate_selected_test_reports(surefire, failsafe)
            path.write_text(path.read_text(encoding="utf-8").replace('skipped="1"', 'skipped="0"')
                            .replace('failures="0"', 'failures="1"'), encoding="utf-8")
            with self.assertRaises(ValueError):
                validate_selected_test_reports(surefire, failsafe)
            path.write_text(path.read_text(encoding="utf-8").replace('failures="1"', 'failures="0"'),
                            encoding="utf-8")
            path.unlink()
            with self.assertRaises(ValueError):
                validate_selected_test_reports(surefire, failsafe)

    def test_pull_request_workflow_is_unconditional_and_selects_every_gate_case(self):
        workflow = Path(__file__).parents[2] / ".github/workflows/lmdb-qemu-durability.yml"
        source = workflow.read_text(encoding="utf-8")
        self.assertRegex(source, r"(?m)^on:\s*\n\s+pull_request:\s*$")
        self.assertIn("runs-on: ubuntu-24.04", source)
        self.assertNotIn("runs-on: ubuntu-24.04-arm", source)
        self.assertIn("qemu-system-x86_64", source)
        self.assertIn("/dev/kvm", source)
        helper = Path(__file__).with_name("qemu_kvm_preflight.py").read_text(encoding="utf-8")
        self.assertIn("q35,accel=kvm", helper)
        self.assertNotIn("tcg", (source + helper).lower())
        self.assertLess(source.index("Preflight KVM acceleration"), source.index("Build exact Java 25 artifacts"))
        self.assertIn("run_ci_campaigns.py", source)
        self.assertIn("if: always()", source)
        self.assertNotIn("paths:", source)
        self.assertNotIn("continue-on-error:", source)
        for scenario in POWER_CUT_SCENARIOS:
            self.assertIn(scenario, source)
        for test_class in (*SUREFIRE_TEST_CLASSES, *FAILSAFE_IT_CLASSES):
            self.assertIn(test_class, source)
        gate = Path(__file__).with_name("ci_gate.py").read_text(encoding="utf-8")
        self.assertIn("Linux x86_64 under QEMU KVM", gate)

    def test_kvm_preflight_grants_only_job_user_and_starts_real_qemu_accelerator(self):
        workflow = Path(__file__).parents[2] / ".github/workflows/lmdb-qemu-durability.yml"
        source = workflow.read_text(encoding="utf-8")
        helper = Path(__file__).with_name("qemu_kvm_preflight.py").read_text(encoding="utf-8")
        self.assertIn("acl", source)
        self.assertIn("setfacl", source)
        self.assertIn("getfacl", source)
        self.assertIn("getfacl --numeric", source)
        self.assertIn('"u:$runner_uid:rw" /dev/kvm', source)
        self.assertIn('kvm_group=$(stat -c %G /dev/kvm)', source)
        self.assertIn('kvm_gid=$(stat -c %g /dev/kvm)', source)
        self.assertIn('exec sudo -n setpriv --reuid %q --regid %q --groups %q --inh-caps=-all', source)
        self.assertNotIn('exec sudo -n -u %q -g %q', source)
        self.assertIn('--device-gid "$kvm_gid"', source)
        self.assertIn('--runner-groups "$runner_groups"', source)
        self.assertIn('qemu_wrapper="$CRASHLAB_ROOT/qemu-kvm"', source)
        self.assertIn('python3 scripts/lmdb-crashlab/qemu_kvm_preflight.py', source)
        self.assertIn("KVM_GET_API_VERSION", helper)
        self.assertIn("KVM_CREATE_VM", helper)
        self.assertIn('"udevadm", "info", "--query=all", "--name=/dev/kvm"', helper)
        self.assertIn('"udev-rules.txt"', helper)
        self.assertIn('"-f", "-yy", "-e", "trace=%file"', helper)
        self.assertIn('"getcap", qemu_binary', helper)
        self.assertIn('"journalctl", "-k"', helper)
        self.assertIn('apparmor-profiles.txt', helper)
        self.assertIn('kernel-denials.txt', helper)
        self.assertIn('audit-denials.txt', helper)
        self.assertIn('run_qemu_process_probes(', helper)
        self.assertIn('"primary-group"', helper)
        self.assertIn('required=True', helper)
        self.assertNotIn('ps -o stat= -p', source)
        self.assertEqual(source.count('"$CRASHLAB_ROOT/qemu-kvm"'), 3)
        self.assertIn('"q35,accel=kvm"', helper)
        self.assertIn('f"unix:{qmp_path},server=on,wait=off"', helper)

    def test_workflow_inline_python_is_valid_and_runner_groups_are_deduplicated(self):
        workflow = Path(__file__).parents[2] / ".github/workflows/lmdb-qemu-durability.yml"
        source = workflow.read_text(encoding="utf-8")
        programs = re.findall(r"python3 -c '([^']*)'", source)
        self.assertTrue(programs, "workflow must not hide unvalidated inline Python")
        for program in programs:
            compile(program, str(workflow), "exec")

        self.assertEqual(len(programs), 1, "review any additional inline Python command")
        result = subprocess.run(
            ["python3", "-c", programs[0], "1001 4 1001", "108"],
            check=True,
            capture_output=True,
            text=True,
        )
        self.assertEqual(result.stdout, "4,108,1001\n")

    def test_provisioner_preserves_arm_pl011_and_x86_serial_consoles(self):
        provisioner = Path(__file__).with_name("provision-linux-guest.sh").read_text(encoding="utf-8")
        self.assertIn("guest_serial=ttyAMA0", provisioner)
        self.assertIn("guest_serial=ttyS0", provisioner)
        self.assertIn("/dev/__GUEST_SERIAL__", provisioner)
        self.assertIn("qemu_machine=virt", provisioner)
        self.assertIn("qemu_machine=q35", provisioner)
        self.assertIn("virtio-net-pci,netdev=net0,romfile=", provisioner)

    def test_linux_guest_provisioner_supports_x86_64_ovmf(self):
        provisioner = Path(__file__).with_name("provision-linux-guest.sh").read_text(encoding="utf-8")
        self.assertIn("x86_64", provisioner)
        self.assertIn("ttyS0", provisioner)
        self.assertIn("file=$firmware_code", provisioner)
        self.assertIn("file=$vars_file", provisioner)
        self.assertIn("qemu_machine=q35", provisioner)

    def test_ci_artifact_excludes_regenerable_guest_os_images(self):
        workflow = Path(__file__).parents[2] / ".github/workflows/lmdb-qemu-durability.yml"
        source = workflow.read_text(encoding="utf-8")
        self.assertIn("package_ci_artifact.py", source)
        self.assertIn("rdf4j-lmdb-qemu-artifact/**", source)
        self.assertNotIn("rdf4j-lmdb-qemu/**", source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
