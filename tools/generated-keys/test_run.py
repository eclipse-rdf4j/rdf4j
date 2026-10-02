"""Regression tests for the generated-key runner's source-root handling."""

import importlib.util
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
RUNNER_PATH = Path(__file__).with_name("run.py")


def load_runner():
	spec = importlib.util.spec_from_file_location("generated_keys_run", RUNNER_PATH)
	runner = importlib.util.module_from_spec(spec)
	spec.loader.exec_module(runner)
	return runner


class GeneratedKeysRunnerTest(unittest.TestCase):

	def test_prepare_accepts_repository_source_layout(self):
		runner = load_runner()
		with tempfile.TemporaryDirectory(prefix="generated-keys-repository-") as temporary_directory:
			output = Path(temporary_directory)
			build = runner.prepare(output, REPOSITORY_ROOT)

			manifest = json.loads((output / "build-manifest.json").read_text())
			self.assertIn(
				"org/eclipse/rdf4j/sail/lmdb/evaluation/NativeGeneratedKeyPlan.java",
				manifest,
				)
			self.assertIn(
				"complete production file (repository source layout)",
				manifest["org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelPeerCancelledException.java"]["origin"],
			)
			self.assertEqual(build, output / "build")

	def test_prepare_accepts_legacy_java_source_layout(self):
		runner = load_runner()
		with tempfile.TemporaryDirectory(prefix="generated-keys-legacy-source-") as source_directory:
			source_root = Path(source_directory)
			(source_root / "java").symlink_to(
				REPOSITORY_ROOT / "core" / "sail" / "lmdb" / "src" / "main" / "java",
				target_is_directory=True,
			)
			with tempfile.TemporaryDirectory(prefix="generated-keys-legacy-output-") as output_directory:
				output = Path(output_directory)
				build = runner.prepare(output, source_root)

				manifest = json.loads((output / "build-manifest.json").read_text())
				self.assertIn(
					"org/eclipse/rdf4j/sail/lmdb/evaluation/NativeGeneratedKeyPlan.java",
					manifest,
				)
				self.assertEqual(build, output / "build")

	def test_prepare_uses_peer_test_double_when_legacy_source_omits_peer(self):
		runner = load_runner()
		with tempfile.TemporaryDirectory(prefix="generated-keys-legacy-source-without-peer-") as source_directory:
			source_root = Path(source_directory)
			shutil.copytree(
				REPOSITORY_ROOT / "core" / "sail" / "lmdb" / "src" / "main" / "java",
				source_root / "java",
				ignore=shutil.ignore_patterns("KernelPeerCancelledException.java"),
			)
			with tempfile.TemporaryDirectory(prefix="generated-keys-legacy-output-") as output_directory:
				output = Path(output_directory)
				runner.prepare(output, source_root)

				manifest = json.loads((output / "build-manifest.json").read_text())
				self.assertEqual(
					manifest["org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelPeerCancelledException.java"]["origin"],
					"explicit test double",
				)

	def test_compile_only_targets_project_minimum_java_release(self):
		with tempfile.TemporaryDirectory(prefix="generated-keys-legacy-source-") as source_directory:
			source_root = Path(source_directory)
			(source_root / "java").symlink_to(
				REPOSITORY_ROOT / "core" / "sail" / "lmdb" / "src" / "main" / "java",
				target_is_directory=True,
			)
			with tempfile.TemporaryDirectory(prefix="generated-keys-release-output-") as output_directory:
				result = subprocess.run(
					[
						sys.executable,
						str(RUNNER_PATH),
						"--source",
						str(source_root),
						"--out",
						output_directory,
						"--compile-only",
					],
					capture_output=True,
					text=True,
				)
				self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
				arguments = (Path(output_directory) / "build" / "javac.args").read_text().splitlines()

			self.assertEqual(arguments[2:4], ['"--release"', '"25"'])

	def test_compile_only_accepts_output_paths_with_spaces(self):
		with tempfile.TemporaryDirectory(prefix="generated keys source ") as source_directory:
			source_root = Path(source_directory)
			(source_root / "java").symlink_to(
				REPOSITORY_ROOT / "core" / "sail" / "lmdb" / "src" / "main" / "java",
				target_is_directory=True,
			)
			with tempfile.TemporaryDirectory(prefix="generated keys output ") as output_directory:
				result = subprocess.run(
					[
						sys.executable,
						str(RUNNER_PATH),
						"--source",
						str(source_root),
						"--out",
						output_directory,
						"--compile-only",
					],
					capture_output=True,
					text=True,
				)
				self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

	def test_compile_only_accepts_argfile_metacharacters(self):
		with tempfile.TemporaryDirectory(prefix='generated#source"\n') as source_directory:
			source_root = Path(source_directory)
			(source_root / "java").symlink_to(
				REPOSITORY_ROOT / "core" / "sail" / "lmdb" / "src" / "main" / "java",
				target_is_directory=True,
			)
			with tempfile.TemporaryDirectory(prefix='generated#output"\n') as output_directory:
				result = subprocess.run(
					[
						sys.executable,
						str(RUNNER_PATH),
						"--source",
						str(source_root),
						"--out",
						output_directory,
						"--compile-only",
					],
					capture_output=True,
					text=True,
				)
				self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == "__main__":
	unittest.main()
