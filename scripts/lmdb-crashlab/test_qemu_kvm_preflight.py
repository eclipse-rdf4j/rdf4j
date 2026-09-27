import tempfile
import textwrap
import unittest
from pathlib import Path

from qemu_kvm_preflight import (
    ProbeFailure,
    credential_drop_prefix,
    run_qemu_probe,
    run_qemu_process_probes,
    validate_dropped_credentials,
)


REPO_ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = REPO_ROOT / ".github/workflows/lmdb-qemu-durability.yml"


class QemuKvmPreflightTests(unittest.TestCase):
    def test_privilege_drop_sets_runner_uid_and_kvm_primary_group(self):
        prefix = credential_drop_prefix(1001, 993, "1001,4,100,118,999")

        self.assertEqual(prefix[:3], ["sudo", "-n", "setpriv"])
        self.assertIn("--reuid=1001", prefix)
        self.assertIn("--regid=993", prefix)
        self.assertIn("--groups=4,100,118,993,999,1001", prefix)
        self.assertIn("--inh-caps=-all", prefix)
        self.assertIn("--bounding-set=-all", prefix)
        self.assertIn("--no-new-privs", prefix)

    def test_privilege_drop_validation_requires_all_ids_to_be_unprivileged(self):
        status = (
            "Uid:\t1001\t1001\t1001\t1001\n"
            "Gid:\t993\t993\t993\t993\n"
            "Groups:\t4 100 993 1001\n"
            "CapInh:\t0000000000000000\nCapPrm:\t0000000000000000\n"
            "CapEff:\t0000000000000000\nCapBnd:\t0000000000000000\n"
            "CapAmb:\t0000000000000000\nNoNewPrivs:\t1\n"
        )

        validate_dropped_credentials(status, 1001, 993)
        with self.assertRaisesRegex(RuntimeError, "mismatched UID"):
            validate_dropped_credentials(
                status.replace("Uid:\t1001\t1001\t1001\t1001", "Uid:\t1001\t1001\t0\t0"),
                1001,
                993,
            )
        with self.assertRaisesRegex(RuntimeError, "retained Linux capabilities"):
            validate_dropped_credentials(
                status.replace("CapEff:\t0000000000000000", "CapEff:\t0000000000000001"), 1001, 993
            )
        with self.assertRaisesRegex(RuntimeError, "did not set every GID"):
            validate_dropped_credentials(
                status.replace("Gid:\t993\t993\t993\t993", "Gid:\t993\t993\t0\t0"), 1001, 993
            )

    def test_workflow_uses_bounded_probe_controller(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")

        self.assertIn(
            "python3 scripts/lmdb-crashlab/qemu_kvm_preflight.py",
            workflow,
            "QEMU child/QMP lifecycle must be supervised by the tested helper",
        )
        self.assertNotIn("probe_state() {", workflow)
        self.assertNotIn("ps -o stat= -p", workflow)

    def test_optional_child_exit_before_qmp_does_not_skip_required_group_probe(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            direct = self._fake_qemu(root, "exit-before-qmp", "direct-qemu")
            group = self._fake_qemu(root, "qmp", "group-qemu")
            strace = self._fake_strace(root)

            direct_result, group_result = run_qemu_process_probes(
                root / "probes",
                [str(strace), "-f", "-yy", "-e", "trace=%file", "-o",
                 str(root / "probes/direct-user/open.trace"), str(direct)],
                [str(group)],
                startup_timeout=3,
                stable_duration=0.08,
                poll_interval=0.01,
                security_capture=None,
            )

            self.assertFalse(direct_result.passed)
            self.assertEqual(direct_result.status, 23)
            self.assertTrue(group_result.passed)
            self.assertTrue((root / "group-qemu-ran").exists())

    def test_required_child_exit_before_qmp_fails_boundedly(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            qemu = self._fake_qemu(root, "exit-before-qmp", "early-qemu")

            with self.assertRaisesRegex(ProbeFailure, "exited before QMP handshake"):
                run_qemu_probe("primary-group", [str(qemu)], root / "probe", required=True,
                               startup_timeout=2, stable_duration=0.05,
                               poll_interval=0.01, security_capture=None)

    def test_required_child_exit_after_qmp_socket_before_greeting_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            qemu = self._fake_qemu(root, "exit-after-socket", "late-qemu")

            with self.assertRaisesRegex(ProbeFailure, "after QMP socket appeared but before greeting"):
                run_qemu_probe("primary-group", [str(qemu)], root / "probe", required=True,
                               startup_timeout=2, stable_duration=0.05,
                               poll_interval=0.01, security_capture=None)

    @staticmethod
    def _fake_qemu(root, mode, marker_name):
        path = root / marker_name
        source = textwrap.dedent(f"""\
            #!/usr/bin/env python3
            import json
            import socket
            import sys
            import time
            from pathlib import Path

            Path({str(root / (marker_name + '-ran'))!r}).write_text('ran')
            mode = {mode!r}
            if mode == 'exit-before-qmp':
                sys.exit(23)
            qmp = next(value[5:].split(',', 1)[0]
                       for index, value in enumerate(sys.argv[:-1])
                       if value == '-qmp' for value in [sys.argv[index + 1]])
            server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            server.bind(qmp)
            server.listen(1)
            if mode == 'exit-after-socket':
                time.sleep(0.12)
                sys.exit(24)
            connection, _ = server.accept()
            connection.sendall(b'{{"QMP":{{"version":{{}}}},"capabilities":[]}}\\r\\n')
            stream = connection.makefile('rb')
            for line in stream:
                request = json.loads(line)
                command = request['execute']
                response = {{'return': {{}} if command != 'query-status' else {{'status': 'paused'}},
                            'id': request['id']}}
                connection.sendall(json.dumps(response).encode() + b'\\r\\n')
                if command == 'quit':
                    break
            connection.close()
            server.close()
            sys.exit(0)
            """)
        path.write_text(source, encoding="utf-8")
        path.chmod(0o755)
        return path

    @staticmethod
    def _fake_strace(root):
        path = root / "strace"
        path.write_text(textwrap.dedent("""\
            #!/usr/bin/env python3
            import os
            import sys

            arguments = sys.argv[1:]
            output = arguments.index('-o')
            open(arguments[output + 1], 'w').close()
            executable = arguments[output + 2]
            os.execv(executable, [executable, *arguments[output + 3:]])
            """), encoding="utf-8")
        path.chmod(0o755)
        return path


if __name__ == "__main__":
    unittest.main()
