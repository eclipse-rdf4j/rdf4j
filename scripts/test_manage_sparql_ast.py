import contextlib
import importlib.util
import io
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("manage-sparql-ast.py")


def load_script_module():
	spec = importlib.util.spec_from_file_location("manage_sparql_ast", SCRIPT_PATH)
	module = importlib.util.module_from_spec(spec)
	sys.modules[spec.name] = module
	spec.loader.exec_module(module)
	return module


class ManageSparqlAstTest(unittest.TestCase):
	@classmethod
	def setUpClass(cls):
		cls.module = load_script_module()

	def test_restore_header_preserves_exact_existing_bytes(self):
		header = (
			b"/*******************************************************************************\r\n"
			b" * Copyright (c) 2012, 2024 Eclipse RDF4J contributors.\r\n"
			b" * SPDX-License-Identifier: BSD-3-Clause\r\n"
			b" *******************************************************************************/\r\n"
		)
		old_source = header + b"/* old generator body */\n"
		generated = b"/* SyntaxTreeBuilder.java */\npackage example;\n"

		extracted = self.module.extract_rdf4j_header(old_source)
		restored = self.module.restore_rdf4j_header(generated, extracted)

		self.assertEqual(header, extracted)
		self.assertEqual(header + generated, restored)

	def test_restore_header_replaces_an_accidental_generated_header(self):
		preserved = (
			b"/*******************************************************************************\n"
			b" * Copyright (c) 2020 Eclipse RDF4J contributors.\n"
			b" *******************************************************************************/\n"
		)
		other = (
			b"/*******************************************************************************\n"
			b" * Copyright (c) 2026 Eclipse RDF4J contributors.\n"
			b" *******************************************************************************/\n"
		)

		self.assertEqual(
			preserved + b"class Generated {}\n",
			self.module.restore_rdf4j_header(other + b"class Generated {}\n", preserved),
		)

	def test_patch_generation_is_stable_and_file_scoped(self):
		relative = Path("module/src/main/java/example/Generated.java")
		baseline = b"class Generated {\n\tint value = 1;\n}\n"
		desired = b"class Generated {\n\tint value = 2;\n}\n"

		first = self.module.create_unified_patch(relative, baseline, desired)
		second = self.module.create_unified_patch(relative, baseline, desired)

		self.assertEqual(first, second)
		self.assertTrue(first.startswith(b"diff --git a/module/src/main/java/example/Generated.java "))
		self.assertIn(b"--- a/module/src/main/java/example/Generated.java\n", first)
		self.assertIn(b"+++ b/module/src/main/java/example/Generated.java\n", first)
		self.assertNotIn(str(Path.cwd()).encode(), first)
		self.assertNotIn(b"1970-", first)
		self.assertNotIn(b"2026-", first)

	def test_checksum_footer_is_preserved_only_for_metadata_only_drift(self):
		old_checksum = b"1" * 32
		new_checksum = b"2" * 32
		previous = (
			b"class Generated {}\n"
			b"/* JavaCC - OriginalChecksum="
			+ old_checksum
			+ b" (do not edit this line) */\n"
		)
		regenerated = previous.replace(old_checksum, new_checksum)

		self.assertEqual(
			previous,
			self.module.preserve_checksum_footer_if_only_difference(
				previous, regenerated
			),
		)
		body_changed = regenerated.replace(b"Generated", b"Changed")
		self.assertEqual(
			body_changed,
			self.module.preserve_checksum_footer_if_only_difference(
				previous, body_changed
			),
		)

	def test_checksum_rejection_reports_expected_and_actual_digest(self):
		with tempfile.TemporaryDirectory() as temp_directory:
			jar = Path(temp_directory) / "javacc.jar"
			jar.write_bytes(b"not JavaCC")
			actual = self.module.sha256_file(jar)

			with self.assertRaises(self.module.AstWorkflowError) as raised:
				self.module.verify_sha256(jar, self.module.JAVACC_SHA256)

		message = str(raised.exception)
		self.assertIn(self.module.JAVACC_SHA256, message)
		self.assertIn(actual, message)

	def test_dirty_generated_file_preflight_rejects_worktree_changes(self):
		calls = []

		def runner(command, cwd):
			calls.append((command, cwd))
			return subprocess.CompletedProcess(
				command,
				0,
				stdout=b" M module/Generated.java\0",
				stderr=b"",
			)

		with tempfile.TemporaryDirectory() as temp_directory:
			repository = Path(temp_directory)
			with self.assertRaises(self.module.AstWorkflowError) as raised:
				self.module.require_clean_paths(
					repository,
					[Path("module/Generated.java")],
					runner,
					"generated outputs",
				)

		self.assertIn("module/Generated.java", str(raised.exception))
		self.assertEqual("git", calls[0][0][0])
		self.assertIn("--porcelain=v1", calls[0][0])

	def test_scope_snapshot_restores_changed_deleted_and_created_files(self):
		with tempfile.TemporaryDirectory() as temp_directory:
			ast_directory = Path(temp_directory)
			existing = ast_directory / "Existing.java"
			deleted = ast_directory / "Deleted.java"
			grammar = ast_directory / "sparql.jjt"
			existing.write_bytes(b"original existing\n")
			deleted.write_bytes(b"original deleted\n")
			grammar.write_bytes(b"original grammar\n")

			with self.assertRaisesRegex(RuntimeError, "generator failed"):
				with self.module.AstDirectoryTransaction(ast_directory):
					existing.write_bytes(b"partial output\n")
					deleted.unlink()
					(ast_directory / "Created.java").write_bytes(b"partial new output\n")
					(ast_directory / "sparql.jj").write_bytes(b"temporary grammar\n")
					raise RuntimeError("generator failed")

			self.assertEqual(b"original existing\n", existing.read_bytes())
			self.assertEqual(b"original deleted\n", deleted.read_bytes())
			self.assertEqual(b"original grammar\n", grammar.read_bytes())
			self.assertFalse((ast_directory / "Created.java").exists())
			self.assertFalse((ast_directory / "sparql.jj").exists())

	def test_successful_transaction_can_keep_regenerated_files(self):
		with tempfile.TemporaryDirectory() as temp_directory:
			ast_directory = Path(temp_directory)
			generated = ast_directory / "Generated.java"
			generated.write_bytes(b"before\n")

			with self.module.AstDirectoryTransaction(ast_directory) as transaction:
				generated.write_bytes(b"after\n")
				transaction.commit()

			self.assertEqual(b"after\n", generated.read_bytes())

	def test_main_returns_zero_for_success_and_one_for_workflow_error(self):
		commands = []

		class SuccessfulManager:
			def __init__(self, **kwargs):
				pass

			def run(self, command):
				commands.append(command)

		class FailingManager(SuccessfulManager):
			def run(self, command):
				raise self_error("deliberate failure")

		self_error = self.module.AstWorkflowError
		with tempfile.TemporaryDirectory() as temp_directory:
			repository = Path(temp_directory)
			self.assertEqual(
				0,
				self.module.main(
					["check", "--offline"],
					manager_factory=SuccessfulManager,
					repository_root=repository,
				),
			)
			with contextlib.redirect_stderr(io.StringIO()):
				self.assertEqual(
					1,
					self.module.main(
						["--offline", "record"],
						manager_factory=FailingManager,
						repository_root=repository,
					),
				)

		self.assertEqual(["check"], commands)

	def test_argparse_keeps_exit_code_two_for_invalid_commands(self):
		with contextlib.redirect_stderr(io.StringIO()):
			with self.assertRaises(SystemExit) as raised:
				self.module.main(["unknown-command"])

		self.assertEqual(2, raised.exception.code)


if __name__ == "__main__":
	unittest.main()
