#!/usr/bin/env python3

from __future__ import annotations

import argparse
import difflib
import hashlib
import os
import re
import shlex
import stat
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path, PurePosixPath
from typing import Callable, Iterable, Sequence


JAVACC_VERSION = "7.0.12"
JAVACC_SHA256 = "067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13"
JAVACC_URL = (
    "https://repo1.maven.org/maven2/net/java/dev/javacc/javacc/"
    f"{JAVACC_VERSION}/javacc-{JAVACC_VERSION}.jar"
)

MODULE_RELATIVE = Path("core/queryparser/sparql")
AST_RELATIVE = (
    MODULE_RELATIVE
    / "src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast"
)
PATCH_DIRECTORY_RELATIVE = MODULE_RELATIVE / "JavaCC/patches"
JAVACC_CACHE_RELATIVE = (
    MODULE_RELATIVE / "target/javacc" / f"javacc-{JAVACC_VERSION}.jar"
)

GENERATED_OUTPUT_NAMES = (
    "JJTSyntaxTreeBuilderState.java",
    "SyntaxTreeBuilder.java",
    "SyntaxTreeBuilderConstants.java",
    "SyntaxTreeBuilderDefaultVisitor.java",
    "SyntaxTreeBuilderTokenManager.java",
    "SyntaxTreeBuilderTreeConstants.java",
    "SyntaxTreeBuilderVisitor.java",
)


class AstWorkflowError(RuntimeError):
    """A user-facing, recoverable workflow failure."""


@dataclass(frozen=True)
class PatchSpec:
    patch_name: str
    target_name: str


PATCH_SPECS = (
    PatchSpec("01-optimize-SyntaxTreeBuilder.diff", "SyntaxTreeBuilder.java"),
    PatchSpec(
        "02-optimize-SyntaxTreeBuilderTokenManager.diff",
        "SyntaxTreeBuilderTokenManager.java",
    ),
)
LEGACY_FIRST_PATCH_NAME = "01-optimize-SyntaxTreeBuilder..diff"


@dataclass(frozen=True)
class FileState:
    data: bytes
    mode: int


CommandRunner = Callable[
    [Sequence[str], Path], subprocess.CompletedProcess[bytes]
]
Downloader = Callable[[str, Path], None]


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as error:
        raise AstWorkflowError(f"Could not read {path}: {error}") from error
    return digest.hexdigest()


def verify_sha256(path: Path, expected: str) -> None:
    actual = sha256_file(path)
    if actual != expected:
        raise AstWorkflowError(
            f"Checksum mismatch for {path}: expected {expected}, got {actual}"
        )


def extract_rdf4j_header(source: bytes) -> bytes:
    start = b"/*******************************************************************************"
    end_marker = b" *******************************************************************************/"
    if not source.startswith(start):
        raise AstWorkflowError("Generated Java source has no RDF4J copyright header")

    marker_index = source.find(end_marker)
    if marker_index < 0:
        raise AstWorkflowError("RDF4J copyright header is not terminated")

    end = marker_index + len(end_marker)
    if source[end : end + 2] == b"\r\n":
        end += 2
    elif source[end : end + 1] == b"\n":
        end += 1
    else:
        raise AstWorkflowError("RDF4J copyright header must end with a newline")
    return source[:end]


def restore_rdf4j_header(generated: bytes, preserved_header: bytes) -> bytes:
    if extract_rdf4j_header(preserved_header + b"body") != preserved_header:
        raise AstWorkflowError("Preserved RDF4J header contains trailing content")

    body = generated
    if generated.startswith(b"/*******************************************************************************"):
        body = generated[len(extract_rdf4j_header(generated)) :]
    return preserved_header + body


_CHECKSUM_FOOTER = re.compile(
    rb"(?m)^/\* JavaCC - OriginalChecksum=[0-9a-f]{32} "
    rb"\(do not edit this line\) \*/\r?$"
)


def preserve_checksum_footer_if_only_difference(
    previous: bytes, regenerated: bytes
) -> bytes:
    previous_matches = list(_CHECKSUM_FOOTER.finditer(previous))
    regenerated_matches = list(_CHECKSUM_FOOTER.finditer(regenerated))
    if len(previous_matches) != 1 or len(regenerated_matches) != 1:
        return regenerated

    previous_match = previous_matches[0]
    regenerated_match = regenerated_matches[0]
    placeholder = b"/* JavaCC - OriginalChecksum=<stable> */"
    previous_without_checksum = (
        previous[: previous_match.start()]
        + placeholder
        + previous[previous_match.end() :]
    )
    regenerated_without_checksum = (
        regenerated[: regenerated_match.start()]
        + placeholder
        + regenerated[regenerated_match.end() :]
    )
    if previous_without_checksum != regenerated_without_checksum:
        return regenerated
    return (
        regenerated[: regenerated_match.start()]
        + previous[previous_match.start() : previous_match.end()]
        + regenerated[regenerated_match.end() :]
    )


def new_rdf4j_header(year: int | None = None) -> bytes:
    copyright_year = year if year is not None else datetime.now().year
    return (
        "/*******************************************************************************\n"
        f" * Copyright (c) {copyright_year} Eclipse RDF4J contributors.\n"
        " *\n"
        " * All rights reserved. This program and the accompanying materials\n"
        " * are made available under the terms of the Eclipse Distribution License v1.0\n"
        " * which accompanies this distribution, and is available at\n"
        " * http://www.eclipse.org/org/documents/edl-v10.php.\n"
        " *\n"
        " * SPDX-License-Identifier: BSD-3-Clause\n"
        " *******************************************************************************/\n"
    ).encode("ascii")


def _safe_patch_path(relative_path: Path) -> str:
    path = PurePosixPath(relative_path.as_posix())
    if path.is_absolute() or not path.parts or ".." in path.parts:
        raise AstWorkflowError(f"Patch target must be repository-relative: {relative_path}")
    return path.as_posix()


def create_unified_patch(
    relative_path: Path, baseline: bytes, desired: bytes
) -> bytes:
    path = _safe_patch_path(relative_path)
    if baseline == desired:
        raise AstWorkflowError(f"No customization exists for {path}")
    if not baseline.endswith(b"\n") or not desired.endswith(b"\n"):
        raise AstWorkflowError(f"Patch inputs must end with a newline: {path}")

    try:
        baseline_text = baseline.decode("utf-8")
        desired_text = desired.decode("utf-8")
    except UnicodeDecodeError as error:
        raise AstWorkflowError(f"Patch input is not UTF-8: {path}") from error

    body = "".join(
        difflib.unified_diff(
            baseline_text.splitlines(keepends=True),
            desired_text.splitlines(keepends=True),
            fromfile=f"a/{path}",
            tofile=f"b/{path}",
            lineterm="\n",
        )
    )
    if not body:
        raise AstWorkflowError(f"Could not create a patch for {path}")
    return f"diff --git a/{path} b/{path}\n{body}".encode("utf-8")


def atomic_write(path: Path, data: bytes, mode: int | None = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if mode is None:
        try:
            mode = stat.S_IMODE(path.stat().st_mode)
        except FileNotFoundError:
            mode = 0o644

    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=path.parent
    )
    temporary_path = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as destination:
            destination.write(data)
            destination.flush()
            os.fsync(destination.fileno())
        os.chmod(temporary_path, mode)
        os.replace(temporary_path, path)
    finally:
        try:
            temporary_path.unlink()
        except FileNotFoundError:
            pass


class AstDirectorySnapshot:
    def __init__(self, directory: Path, files: dict[str, FileState]):
        self.directory = directory
        self.files = files

    @classmethod
    def capture(cls, directory: Path) -> "AstDirectorySnapshot":
        if not directory.is_dir():
            raise AstWorkflowError(f"AST directory does not exist: {directory}")
        files = {
            path.name: FileState(
                data=path.read_bytes(), mode=stat.S_IMODE(path.stat().st_mode)
            )
            for path in directory.iterdir()
            if path.is_file()
        }
        return cls(directory, files)

    def java_bytes(self) -> dict[str, bytes]:
        return {
            name: state.data
            for name, state in self.files.items()
            if name.endswith(".java")
        }

    def restore(self) -> None:
        for name, state in self.files.items():
            path = self.directory / name
            current_mode = None
            try:
                current_mode = stat.S_IMODE(path.stat().st_mode)
            except FileNotFoundError:
                pass
            if (
                not path.exists()
                or path.read_bytes() != state.data
                or current_mode != state.mode
            ):
                atomic_write(path, state.data, state.mode)

        original_names = set(self.files)
        for path in self.directory.iterdir():
            if not path.is_file() or path.name in original_names:
                continue
            if path.suffix == ".java" or path.name == "sparql.jj":
                path.unlink()


class AstDirectoryTransaction:
    def __init__(self, directory: Path):
        self.directory = directory
        self.snapshot: AstDirectorySnapshot | None = None
        self._committed = False

    def __enter__(self) -> "AstDirectoryTransaction":
        self.snapshot = AstDirectorySnapshot.capture(self.directory)
        return self

    def commit(self) -> None:
        if self.snapshot is None:
            raise AstWorkflowError("AST transaction has not started")
        self._committed = True

    def __exit__(self, exception_type, exception, traceback) -> bool:
        if self.snapshot is not None and not self._committed:
            try:
                self.snapshot.restore()
            except Exception as restore_error:
                raise AstWorkflowError(
                    f"Could not restore AST directory {self.directory}: {restore_error}"
                ) from exception
        return False


def run_process(
    command: Sequence[str], cwd: Path
) -> subprocess.CompletedProcess[bytes]:
    print(f"+ {shlex.join(command)}")
    try:
        return subprocess.run(
            list(command),
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
    except OSError as error:
        raise AstWorkflowError(f"Could not execute {command[0]}: {error}") from error


def _decode_output(output: bytes | str | None) -> str:
    if output is None:
        return ""
    if isinstance(output, str):
        return output
    return output.decode("utf-8", errors="replace")


def _combined_output(result: subprocess.CompletedProcess[bytes]) -> str:
    stdout = _decode_output(result.stdout)
    stderr = _decode_output(result.stderr)
    if stdout and stderr and not stdout.endswith("\n"):
        stdout += "\n"
    return stdout + stderr


def _show_output(result: subprocess.CompletedProcess[bytes]) -> None:
    output = _combined_output(result)
    if output:
        print(output, end="" if output.endswith("\n") else "\n")


def _raise_command_failure(
    description: str,
    command: Sequence[str],
    result: subprocess.CompletedProcess[bytes],
) -> None:
    output = _combined_output(result).strip()
    detail = f"\n{output}" if output else ""
    raise AstWorkflowError(
        f"{description} failed with exit code {result.returncode}: "
        f"{shlex.join(command)}{detail}"
    )


def require_clean_paths(
    repository: Path,
    paths: Iterable[Path],
    runner: CommandRunner,
    description: str,
) -> None:
    relative_paths = sorted({_safe_patch_path(path) for path in paths})
    command = [
        "git",
        "status",
        "--porcelain=v1",
        "-z",
        "--untracked-files=all",
        "--",
        *relative_paths,
    ]
    result = runner(command, repository)
    if result.returncode != 0:
        _raise_command_failure("Git dirty-file preflight", command, result)
    if result.stdout:
        entries = [
            entry.decode("utf-8", errors="replace")
            for entry in result.stdout.split(b"\0")
            if entry
        ]
        formatted = "\n  ".join(entries)
        raise AstWorkflowError(
            f"Refusing to overwrite modified {description}:\n  {formatted}"
        )


def download_url(url: str, destination: Path) -> None:
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            with destination.open("wb") as output:
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    output.write(chunk)
    except (OSError, urllib.error.URLError) as error:
        raise AstWorkflowError(f"Could not download {url}: {error}") from error


def resolve_javacc_jar(
    repository: Path,
    requested_jar: Path | None,
    offline: bool,
    downloader: Downloader = download_url,
) -> Path:
    if requested_jar is not None:
        jar = requested_jar.expanduser().resolve()
        if not jar.is_file():
            raise AstWorkflowError(f"JavaCC jar does not exist: {jar}")
        verify_sha256(jar, JAVACC_SHA256)
        return jar

    cached_jar = repository / JAVACC_CACHE_RELATIVE
    if cached_jar.is_file():
        verify_sha256(cached_jar, JAVACC_SHA256)
        return cached_jar
    if offline:
        raise AstWorkflowError(
            f"JavaCC {JAVACC_VERSION} is not cached at {cached_jar}; "
            "rerun without --offline or supply --javacc-jar"
        )

    cached_jar.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{cached_jar.name}.", suffix=".download", dir=cached_jar.parent
    )
    os.close(descriptor)
    temporary_path = Path(temporary_name)
    try:
        print(f"Downloading JavaCC {JAVACC_VERSION} from {JAVACC_URL}")
        downloader(JAVACC_URL, temporary_path)
        verify_sha256(temporary_path, JAVACC_SHA256)
        os.chmod(temporary_path, 0o644)
        os.replace(temporary_path, cached_jar)
    finally:
        try:
            temporary_path.unlink()
        except FileNotFoundError:
            pass
    return cached_jar


def _prepare_atomic_file(path: Path, data: bytes) -> Path:
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=path.parent
    )
    temporary_path = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as destination:
            destination.write(data)
            destination.flush()
            os.fsync(destination.fileno())
        try:
            mode = stat.S_IMODE(path.stat().st_mode)
        except FileNotFoundError:
            mode = 0o644
        os.chmod(temporary_path, mode)
        return temporary_path
    except BaseException:
        try:
            temporary_path.unlink()
        except FileNotFoundError:
            pass
        raise


def _capture_optional_file(path: Path) -> FileState | None:
    if not path.exists():
        return None
    return FileState(path.read_bytes(), stat.S_IMODE(path.stat().st_mode))


def _restore_optional_files(states: dict[Path, FileState | None]) -> None:
    for path, state in states.items():
        if state is None:
            try:
                path.unlink()
            except FileNotFoundError:
                pass
        else:
            atomic_write(path, state.data, state.mode)


def replace_recorded_patches(
    patch_directory: Path, candidates: dict[str, bytes]
) -> None:
    destinations = {
        spec.patch_name: patch_directory / spec.patch_name for spec in PATCH_SPECS
    }
    expected_names = set(destinations)
    if set(candidates) != expected_names:
        raise AstWorkflowError(
            "Patch replacement requires exactly: " + ", ".join(sorted(expected_names))
        )

    legacy = patch_directory / LEGACY_FIRST_PATCH_NAME
    protected_paths = list(destinations.values()) + [legacy]
    original_states = {
        path: _capture_optional_file(path) for path in protected_paths
    }
    prepared: dict[Path, Path] = {}
    try:
        for name, destination in destinations.items():
            prepared[destination] = _prepare_atomic_file(
                destination, candidates[name]
            )
        for destination in destinations.values():
            os.replace(prepared[destination], destination)
        try:
            legacy.unlink()
        except FileNotFoundError:
            pass
    except BaseException as error:
        try:
            _restore_optional_files(original_states)
        except Exception as restore_error:
            raise AstWorkflowError(
                f"Patch update failed and could not be rolled back: {restore_error}"
            ) from error
        raise
    finally:
        for temporary_path in prepared.values():
            try:
                temporary_path.unlink()
            except FileNotFoundError:
                pass


def _short_byte_diff(path: Path, expected: bytes | None, actual: bytes | None) -> str:
    if expected is None:
        return f"{path}: generated file is not checked in"
    if actual is None:
        return f"{path}: checked-in file disappeared during regeneration"
    expected_hash = hashlib.sha256(expected).hexdigest()
    actual_hash = hashlib.sha256(actual).hexdigest()
    try:
        expected_lines = expected.decode("utf-8").splitlines(keepends=True)
        actual_lines = actual.decode("utf-8").splitlines(keepends=True)
        diff_lines = list(
            difflib.unified_diff(
                expected_lines,
                actual_lines,
                fromfile=f"checked-in/{path.name}",
                tofile=f"regenerated/{path.name}",
                n=2,
                lineterm="\n",
            )
        )
        excerpt = "".join(diff_lines[:80]).rstrip()
    except UnicodeDecodeError:
        excerpt = "binary content differs"
    return (
        f"{path}: expected sha256 {expected_hash}, got {actual_hash}"
        + (f"\n{excerpt}" if excerpt else "")
    )


class AstManager:
    def __init__(
        self,
        repository_root: Path,
        javacc_jar: Path | None = None,
        offline: bool = False,
        runner: CommandRunner = run_process,
        downloader: Downloader = download_url,
    ):
        self.repository = repository_root.resolve()
        self.requested_jar = javacc_jar
        self.offline = offline
        self.runner = runner
        self.downloader = downloader
        self.ast_directory = self.repository / AST_RELATIVE
        self.patch_directory = self.repository / PATCH_DIRECTORY_RELATIVE
        self.temporary_grammar = self.ast_directory / "sparql.jj"

    def run(self, command: str) -> None:
        self._validate_layout()
        jar = resolve_javacc_jar(
            self.repository, self.requested_jar, self.offline, self.downloader
        )
        if command == "check":
            self._check(jar)
        elif command == "regenerate":
            self._regenerate(jar)
        elif command == "record":
            self._record(jar)
        else:
            raise AstWorkflowError(f"Unsupported command: {command}")

    def _validate_layout(self) -> None:
        if not (self.repository / ".git").exists():
            raise AstWorkflowError(f"Not an RDF4J Git checkout: {self.repository}")
        grammar = self.ast_directory / "sparql.jjt"
        if not grammar.is_file():
            raise AstWorkflowError(f"SPARQL JJTree grammar does not exist: {grammar}")

    def _preflight_temporary_grammar(self) -> None:
        if self.temporary_grammar.exists():
            raise AstWorkflowError(
                f"Refusing to overwrite pre-existing temporary grammar: "
                f"{self.temporary_grammar}"
            )

    def _git_status(self) -> bytes:
        command = [
            "git",
            "status",
            "--porcelain=v1",
            "-z",
            "--untracked-files=all",
        ]
        result = self.runner(command, self.repository)
        if result.returncode != 0:
            _raise_command_failure("Git status", command, result)
        return result.stdout

    def _run_checked(
        self, command: Sequence[str], cwd: Path, description: str
    ) -> subprocess.CompletedProcess[bytes]:
        result = self.runner(command, cwd)
        if result.returncode != 0:
            _raise_command_failure(description, command, result)
        _show_output(result)
        return result

    def _check(self, jar: Path) -> None:
        status_before = self._git_status()
        failure: BaseException | None = None
        try:
            self._preflight_temporary_grammar()
            with AstDirectoryTransaction(self.ast_directory) as transaction:
                snapshot = self._transaction_snapshot(transaction)
                touched = self._generate_formatted_baseline(jar, snapshot)
                self._apply_patches(
                    [self.patch_directory / spec.patch_name for spec in PATCH_SPECS]
                )
                self._require_snapshot_match(
                    snapshot,
                    touched,
                    "SPARQL AST patches are stale; run the record command after "
                    "reviewing the checked-in generated code",
                )
        except BaseException as error:
            failure = error

        status_after = self._git_status()
        if status_after != status_before:
            restoration_error = AstWorkflowError(
                "The check command changed Git status despite its restoration guarantee"
            )
            if failure is not None:
                raise restoration_error from failure
            raise restoration_error
        if failure is not None:
            raise failure
        print("SPARQL AST is reproducible; check left the working tree unchanged.")

    def _regenerate(self, jar: Path) -> None:
        self._preflight_temporary_grammar()
        require_clean_paths(
            self.repository,
            [AST_RELATIVE / name for name in GENERATED_OUTPUT_NAMES],
            self.runner,
            "generated SPARQL AST outputs",
        )
        with AstDirectoryTransaction(self.ast_directory) as transaction:
            snapshot = self._transaction_snapshot(transaction)
            touched = self._generate_formatted_baseline(jar, snapshot)
            self._apply_patches(
                [self.patch_directory / spec.patch_name for spec in PATCH_SPECS]
            )
            self._require_patch_targets_touched(touched)
            transaction.commit()
        print(
            f"Regenerated {len(touched)} JavaCC-touched files and applied "
            f"{len(PATCH_SPECS)} patches."
        )

    def _record(self, jar: Path) -> None:
        self._preflight_temporary_grammar()
        candidates: dict[str, bytes] = {}
        with AstDirectoryTransaction(self.ast_directory) as transaction:
            snapshot = self._transaction_snapshot(transaction)
            touched = self._generate_formatted_baseline(jar, snapshot)
            self._require_patch_targets_touched(touched)

            target_names = {spec.target_name for spec in PATCH_SPECS}
            unexpected = []
            for path in touched:
                if path.name in target_names:
                    continue
                desired = snapshot.files.get(path.name)
                actual = path.read_bytes() if path.exists() else None
                if desired is None or desired.data != actual:
                    unexpected.append(
                        _short_byte_diff(
                            path,
                            desired.data if desired is not None else None,
                            actual,
                        )
                    )
            if unexpected:
                raise AstWorkflowError(
                    "Unexpected custom differences exist outside the two patch targets; "
                    "record refuses to omit them:\n" + "\n".join(unexpected)
                )

            for spec in PATCH_SPECS:
                target = self.ast_directory / spec.target_name
                desired = snapshot.files.get(spec.target_name)
                if desired is None:
                    raise AstWorkflowError(
                        f"Cannot record missing checked-in target: {target}"
                    )
                candidates[spec.patch_name] = create_unified_patch(
                    AST_RELATIVE / spec.target_name,
                    target.read_bytes(),
                    desired.data,
                )

            with tempfile.TemporaryDirectory(prefix="rdf4j-sparql-ast-patches-") as temp:
                temporary_patch_directory = Path(temp)
                patch_paths = []
                for spec in PATCH_SPECS:
                    path = temporary_patch_directory / spec.patch_name
                    path.write_bytes(candidates[spec.patch_name])
                    patch_paths.append(path)
                self._apply_patches(patch_paths)

            self._require_snapshot_match(
                snapshot,
                touched,
                "Newly recorded patches did not exactly replay the customized outputs",
            )

        replace_recorded_patches(self.patch_directory, candidates)
        print(
            "Recorded two deterministic SPARQL AST patches and verified their exact replay."
        )

    @staticmethod
    def _transaction_snapshot(
        transaction: AstDirectoryTransaction,
    ) -> AstDirectorySnapshot:
        if transaction.snapshot is None:
            raise AstWorkflowError("AST transaction did not capture a snapshot")
        return transaction.snapshot

    def _generate_formatted_baseline(
        self, jar: Path, snapshot: AstDirectorySnapshot
    ) -> tuple[Path, ...]:
        before = snapshot.java_bytes()
        jjtree_command = ["java", "-cp", str(jar), "jjtree", "sparql.jjt"]
        javacc_command = ["java", "-cp", str(jar), "javacc", "sparql.jj"]

        try:
            self._run_checked(jjtree_command, self.ast_directory, "JJTree generation")
            self._run_checked(javacc_command, self.ast_directory, "JavaCC generation")
        finally:
            try:
                self.temporary_grammar.unlink()
            except FileNotFoundError:
                pass

        after = {
            path.name: path.read_bytes()
            for path in self.ast_directory.glob("*.java")
            if path.is_file()
        }
        touched_names = sorted(
            name
            for name in set(before) | set(after)
            if before.get(name) != after.get(name)
        )
        if not touched_names:
            raise AstWorkflowError("JavaCC did not touch or create any Java sources")

        touched = tuple(self.ast_directory / name for name in touched_names)
        for path in touched:
            if not path.is_file():
                raise AstWorkflowError(f"JavaCC removed generated source: {path}")
            previous = snapshot.files.get(path.name)
            preserved_header = (
                extract_rdf4j_header(previous.data)
                if previous is not None
                else new_rdf4j_header()
            )
            atomic_write(
                path,
                restore_rdf4j_header(path.read_bytes(), preserved_header),
            )

        self._format_touched_files(touched)
        for path in touched:
            previous = snapshot.files.get(path.name)
            if previous is None:
                continue
            formatted = path.read_bytes()
            stable = preserve_checksum_footer_if_only_difference(
                previous.data, formatted
            )
            if stable != formatted:
                atomic_write(path, stable)
        return touched

    def _format_touched_files(self, touched: tuple[Path, ...]) -> None:
        patterns = ",".join(
            f"^{re.escape(str(path.resolve()))}$" for path in sorted(touched)
        )
        command = [
            "mvn",
            "-o",
            "-Dmaven.repo.local=.m2_repo",
            "-pl",
            MODULE_RELATIVE.as_posix(),
            f"-DspotlessFiles={patterns}",
            "spotless:apply",
        ]
        result = self.runner(command, self.repository)
        if result.returncode == 0:
            _show_output(result)
            return

        output = _combined_output(result).lower()
        offline_resolution_markers = (
            "offline mode",
            "cannot access central",
            "has not been downloaded from it before",
            "could not resolve plugin",
            "could not resolve dependencies",
        )
        resolution_failure = any(
            marker in output for marker in offline_resolution_markers
        )
        if self.offline or not resolution_failure:
            _raise_command_failure("Targeted Spotless formatting", command, result)

        print("Spotless dependencies are missing locally; retrying Maven online.")
        online_command = [argument for argument in command if argument != "-o"]
        self._run_checked(
            online_command, self.repository, "Targeted Spotless formatting"
        )

    def _apply_patches(self, patches: Iterable[Path]) -> None:
        for patch in patches:
            if not patch.is_file():
                raise AstWorkflowError(f"SPARQL AST patch does not exist: {patch}")
            check_command = ["git", "apply", "--check", str(patch)]
            self._run_checked(
                check_command,
                self.repository,
                f"Patch preflight for {patch.name}",
            )
            apply_command = ["git", "apply", str(patch)]
            self._run_checked(
                apply_command,
                self.repository,
                f"Patch replay for {patch.name}",
            )

    def _require_patch_targets_touched(self, touched: tuple[Path, ...]) -> None:
        touched_names = {path.name for path in touched}
        missing = [
            spec.target_name
            for spec in PATCH_SPECS
            if spec.target_name not in touched_names
        ]
        if missing:
            raise AstWorkflowError(
                "JavaCC did not regenerate required patch targets: " + ", ".join(missing)
            )

    @staticmethod
    def _require_snapshot_match(
        snapshot: AstDirectorySnapshot,
        touched: tuple[Path, ...],
        description: str,
    ) -> None:
        differences = []
        for path in touched:
            expected = snapshot.files.get(path.name)
            actual = path.read_bytes() if path.exists() else None
            expected_bytes = expected.data if expected is not None else None
            if expected_bytes != actual:
                differences.append(_short_byte_diff(path, expected_bytes, actual))
        if differences:
            raise AstWorkflowError(description + ":\n" + "\n".join(differences))


def discover_repository_root() -> Path:
    return Path(__file__).resolve().parent.parent


def _add_shared_options(
    parser: argparse.ArgumentParser, suppress_defaults: bool = False
) -> None:
    default = argparse.SUPPRESS if suppress_defaults else None
    parser.add_argument(
        "--javacc-jar",
        type=Path,
        default=default,
        help=(
            f"use this JavaCC {JAVACC_VERSION} jar after verifying its SHA-256"
        ),
    )
    parser.add_argument(
        "--offline",
        action="store_true",
        default=argparse.SUPPRESS if suppress_defaults else False,
        help="forbid JavaCC download and Maven's online Spotless retry",
    )


def build_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Regenerate the SPARQL JavaCC AST with restored RDF4J headers, "
            "targeted Spotless formatting, and ordered customization patches."
        )
    )
    _add_shared_options(parser)
    subparsers = parser.add_subparsers(dest="command", required=True)
    help_text = {
        "check": "verify exact reproduction and restore the working tree",
        "regenerate": "replace clean generated outputs with the patched replay",
        "record": "record current customized outputs as deterministic patches",
    }
    for command, description in help_text.items():
        subparser = subparsers.add_parser(command, help=description)
        _add_shared_options(subparser, suppress_defaults=True)
    return parser


def main(
    argv: Sequence[str] | None = None,
    *,
    manager_factory: type[AstManager] = AstManager,
    repository_root: Path | None = None,
) -> int:
    arguments = build_argument_parser().parse_args(argv)
    try:
        manager = manager_factory(
            repository_root=(repository_root or discover_repository_root()),
            javacc_jar=getattr(arguments, "javacc_jar", None),
            offline=getattr(arguments, "offline", False),
        )
        manager.run(arguments.command)
    except AstWorkflowError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
