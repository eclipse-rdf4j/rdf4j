import tempfile
import unittest
from pathlib import Path
import os

from run_campaign import create_blank_image
from runner_common import create_fresh_directory, guest_classpath, validate_qemu_arguments


class RunnerSafetyTests(unittest.TestCase):
    def test_fresh_directory_refuses_existing_output(self):
        with tempfile.TemporaryDirectory() as parent:
            existing = Path(parent) / "already-there"
            existing.mkdir()
            with self.assertRaises(FileExistsError):
                create_fresh_directory(existing)

    def test_fresh_directory_creates_only_requested_root(self):
        with tempfile.TemporaryDirectory() as parent:
            output = Path(parent) / "new-run"
            create_fresh_directory(output)
            self.assertTrue(output.is_dir())

    def test_create_blank_image_fsyncs_requested_size(self):
        with tempfile.TemporaryDirectory() as parent:
            image = Path(parent) / "data.raw"
            create_blank_image(image, 4096)
            self.assertEqual(image.stat().st_size, 4096)

    def test_canonical_runner_includes_runner_and_protocol_suites(self):
        runner = Path(__file__).with_name("run-tests.sh").read_text(encoding="utf-8")
        self.assertIn("python3 -m unittest -v test_runner_common test_volatile_nbd test_powercut_campaign", runner)

    def test_qemu_arguments_reject_unsafe_cache_modes(self):
        with self.assertRaises(ValueError):
            validate_qemu_arguments(["-drive", "file=disk.raw,cache=unsafe"])
        with self.assertRaises(ValueError):
            validate_qemu_arguments(["-snapshot"])

    def test_qemu_arguments_require_writeback_and_flush_capability(self):
        args = [
            "-drive", "file=nbd://127.0.0.1:10809/crashlab,cache=writeback",
            "-device", "virtio-blk-pci,drive=datadisk,write-cache=on,serial=CRASHLAB-NBD",
        ]
        validate_qemu_arguments(args)

    def test_guest_classpath_maps_read_only_roots_and_filters_other_native_platforms(self):
        with tempfile.TemporaryDirectory() as parent:
            repo = Path(parent) / "checkout"
            test_classes = repo / "core/sail/lmdb/target/test-classes"
            classes = repo / "core/sail/lmdb/target/classes"
            m2 = repo / ".m2_repo"
            test_classes.mkdir(parents=True)
            classes.mkdir(parents=True)
            native_dir = m2 / "org/lwjgl/lwjgl-lmdb/3.3.6"
            native_dir.mkdir(parents=True)
            arm = native_dir / "lwjgl-lmdb-3.3.6-natives-linux-arm64.jar"
            mac = native_dir / "lwjgl-lmdb-3.3.6-natives-macos.jar"
            arm.write_bytes(b"arm64")
            mac.write_bytes(b"macos")
            cp_file = Path(parent) / "classpath.txt"
            cp_file.write_text(os.pathsep.join(map(str, (test_classes, classes, arm, mac))),
                               encoding="utf-8")

            guest_cp, manifest = guest_classpath(cp_file, repo)

            self.assertIn("/mnt/rdf4j-ro/core/sail/lmdb/target/test-classes", guest_cp)
            self.assertIn("/mnt/rdf4j-ro/core/sail/lmdb/target/classes", guest_cp)
            self.assertIn("/mnt/m2-ro/org/lwjgl/lwjgl-lmdb/3.3.6/lwjgl-lmdb-3.3.6-natives-linux-arm64.jar",
                          guest_cp)
            self.assertNotIn("natives-macos.jar", guest_cp)
            self.assertEqual(manifest["filtered_non_arm64_native_jars"], [str(mac.resolve())])


if __name__ == "__main__":
    unittest.main(verbosity=2)
