import tempfile
import textwrap
import unittest
from pathlib import Path

from qemu_kvm_preflight import ProbeFailure, run_qemu_probe, run_qemu_process_probes


REPO_ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = REPO_ROOT / ".github/workflows/lmdb-qemu-durability.yml"


class QemuKvmPreflightTests(unittest.TestCase):
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
                startup_timeout=0.5,
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
                               startup_timeout=0.5, stable_duration=0.05,
                               poll_interval=0.01, security_capture=None)

    def test_required_child_exit_after_qmp_socket_before_greeting_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            qemu = self._fake_qemu(root, "exit-after-socket", "late-qemu")

            with self.assertRaisesRegex(ProbeFailure, "after QMP socket appeared but before greeting"):
                run_qemu_probe("primary-group", [str(qemu)], root / "probe", required=True,
                               startup_timeout=0.8, stable_duration=0.05,
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
