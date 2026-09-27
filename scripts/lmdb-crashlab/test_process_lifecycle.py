import os
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

from runner_common import fence_nbd_then_stop_guest, start_logged_process, stop_process


class ProcessGroupLifecycleTests(unittest.TestCase):
    def test_fences_nbd_before_abrupt_guest_group_shutdown(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            process = start_logged_process(
                [sys.executable, "-c", "import time; time.sleep(60)"], root / "guest.log"
            )
            events = []

            def fence_nbd():
                self.assertIsNone(process.poll(), "guest must still be running when NBD is fenced")
                events.append("NBD fenced")
                return {"off": True, "survival": "drop"}

            result = fence_nbd_then_stop_guest(process, fence_nbd)

            self.assertEqual(events, ["NBD fenced"])
            self.assertEqual(result, {"off": True, "survival": "drop"})
            self.assertIsNotNone(process.poll(), "guest process group must be stopped and reaped")

    def test_cut_failure_still_stops_guest_and_propagates_error(self):
        with tempfile.TemporaryDirectory() as temp:
            process = start_logged_process(
                [sys.executable, "-c", "import time; time.sleep(60)"], Path(temp) / "guest.log"
            )

            def failed_cut():
                self.assertIsNone(process.poll(), "guest must be alive when the backend cut fails")
                raise RuntimeError("backend cut failed")

            try:
                with self.assertRaisesRegex(RuntimeError, "backend cut failed"):
                    fence_nbd_then_stop_guest(process, failed_cut)
                self.assertIsNotNone(process.poll(), "failed NBD cut must not leave the guest alive")
            finally:
                if process.poll() is None:
                    stop_process(process, signal.SIGKILL, timeout=1)

    def test_fault_campaign_fences_nbd_before_stopping_qemu(self):
        script_dir = Path(__file__).parent
        for name in ("run_campaign.py", "run_powercut_campaign.py", "run_calibration.py"):
            with self.subTest(runner=name):
                source = (script_dir / name).read_text(encoding="utf-8")
                self.assertTrue(
                    "fence_nbd_then_stop_guest(" in source,
                    f"{name} must cut/disconnect the NBD device before abrupt guest shutdown",
                )

    def test_stop_process_signals_and_reaps_wrapped_guest_process_group(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            wrapper = root / "qemu-wrapper.py"
            child_pid = root / "child.pid"
            child_terminated = root / "child-terminated"
            log = root / "wrapper.log"
            child_script = root / "qemu-child.py"
            child_script.write_text(
                "import os\nimport signal\nimport sys\nimport time\nfrom pathlib import Path\n"
                "marker = Path(sys.argv[1])\n"
                "signal.signal(signal.SIGTERM, lambda *_: (marker.write_text('term'), sys.exit(0)))\n"
                "Path(sys.argv[2]).write_text(str(os.getpid()))\n"
                "while True:\n    time.sleep(0.05)\n",
                encoding="utf-8",
            )
            wrapper.write_text(
                "import subprocess\nimport sys\n"
                f"child = subprocess.Popen([sys.executable, {str(child_script)!r}, *sys.argv[1:]])\n"
                "child.wait()\n",
                encoding="utf-8",
            )

            process = start_logged_process(
                [sys.executable, str(wrapper), str(child_terminated), str(child_pid)], log
            )
            try:
                deadline = time.monotonic() + 5
                while time.monotonic() < deadline and not child_pid.is_file():
                    if process.poll() is not None:
                        self.fail(f"wrapper exited before starting QEMU child: {process.returncode}; "
                                  f"log={log.read_text(encoding='utf-8', errors='replace')}")
                    time.sleep(0.01)
                self.assertTrue(child_pid.is_file(), "wrapper did not start its QEMU child")
                child_pid_value = int(child_pid.read_text(encoding="utf-8"))
                isolated_group = os.getpgid(process.pid) == process.pid

                stop_process(process, signal.SIGTERM, timeout=2)

                self.assertTrue(child_terminated.is_file(),
                                "stopping the wrapper must also signal the QEMU child")
                self.assertTrue(isolated_group, "wrapped QEMU must own an isolated process group")
                self.assertIsNotNone(process.poll(), "the wrapper process must be reaped")
                self.assertFalse(self._process_exists(child_pid_value),
                                 "the wrapped QEMU process must be gone after cleanup")
            finally:
                if process.poll() is None:
                    stop_process(process, signal.SIGKILL, timeout=1)
                if child_pid.is_file():
                    remaining = int(child_pid.read_text(encoding="utf-8"))
                    if self._process_exists(remaining):
                        try:
                            os.kill(remaining, signal.SIGTERM)
                        except ProcessLookupError:
                            pass
                        cleanup_deadline = time.monotonic() + 2
                        while time.monotonic() < cleanup_deadline and self._process_exists(remaining):
                            time.sleep(0.01)
                        if self._process_exists(remaining):
                            os.kill(remaining, signal.SIGKILL)

    def test_stop_process_cleans_child_after_wrapper_exits_first(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            wrapper = root / "qemu-wrapper.py"
            child_pid = root / "child.pid"
            child_ready = root / "child-ready"
            term_seen = root / "term-seen"
            child_script = root / "qemu-child.py"
            child_script.write_text(
                "import os\nimport signal\nimport sys\nimport time\nfrom pathlib import Path\n"
                "Path(sys.argv[1]).write_text(str(os.getpid()))\n"
                "Path(sys.argv[2]).write_text('ready')\n"
                "def ignore_term(*_):\n"
                "    Path(sys.argv[3]).write_text('term')\n"
                "signal.signal(signal.SIGTERM, ignore_term)\n"
                "while True:\n    time.sleep(0.05)\n",
                encoding="utf-8",
            )
            wrapper.write_text(
                "import subprocess\nimport sys\n"
                f"subprocess.Popen([sys.executable, {str(child_script)!r}, *sys.argv[1:]])\n",
                encoding="utf-8",
            )

            process = start_logged_process(
                [sys.executable, str(wrapper), str(child_pid), str(child_ready), str(term_seen)],
                root / "wrapper.log",
            )
            try:
                self.assertEqual(process.wait(timeout=2), 0, "wrapper should exit before its child")
                deadline = time.monotonic() + 2
                while time.monotonic() < deadline and not child_ready.is_file():
                    time.sleep(0.01)
                self.assertTrue(child_ready.is_file(), "wrapper did not start the child")
                child_pid_value = int(child_pid.read_text(encoding="utf-8"))
                self.assertTrue(self._process_exists(child_pid_value), "child should outlive the wrapper")

                started = time.monotonic()
                stop_process(process, signal.SIGTERM, timeout=0.1)
                elapsed = time.monotonic() - started

                self.assertTrue(term_seen.is_file(), "cleanup should signal the surviving child group")
                self.assertLess(elapsed, 3, "ignored TERM must escalate within a bounded interval")
                self.assertFalse(self._process_exists(child_pid_value), "surviving child must be reaped")
            finally:
                if child_pid.is_file():
                    remaining = int(child_pid.read_text(encoding="utf-8"))
                    if self._process_exists(remaining):
                        try:
                            os.kill(remaining, signal.SIGKILL)
                        except ProcessLookupError:
                            pass

    @staticmethod
    def _process_exists(pid):
        try:
            os.kill(pid, 0)
            return True
        except ProcessLookupError:
            return False
        except PermissionError:
            return True


if __name__ == "__main__":
    unittest.main()
