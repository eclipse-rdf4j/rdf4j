#!/usr/bin/env python3
"""Build immutable RDF4J LMDB runtimes from exact Git source revisions."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import re
import shutil
import shlex
import subprocess
import sys
import tarfile
import tempfile
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable


RDF4J_RUNTIME_MODULES = (
	"core/sail/lmdb",
	"core/sail/memory",
	"core/repository/sail",
	"core/queryparser/sparql",
)
RUNTIME_ARTIFACTS = (
	"rdf4j-sail-lmdb",
	"rdf4j-sail-memory",
	"rdf4j-repository-sail",
	"rdf4j-queryparser-sparql",
)
FORMATTER_SKIP_FLAGS = (
	"-Dformatter.skip=true",
	"-Dimpsort.skip=true",
	"-Dspotless.apply.skip=true",
	"-Dspotless.check.skip=true",
)
OFFLINE_MISSING_MARKERS = (
	"cannot access",
	"has not been downloaded from it before",
	"not available in offline mode",
	"artifactdescriptorexception",
	"could not find artifact",
	"could not resolve dependencies",
	"failed to read artifact descriptor",
	"cached in the local repository",
)


def sha256(path: Path) -> str:
	digest = hashlib.sha256()
	with path.open("rb") as source:
		for chunk in iter(lambda: source.read(1024 * 1024), b""):
			digest.update(chunk)
	return digest.hexdigest()


def run_logged(command: list[str], cwd: Path, log_path: Path) -> int:
	log_path.parent.mkdir(parents=True, exist_ok=True)
	with log_path.open("ab") as log:
		log.write((f"\ncwd: {cwd.resolve()}\n$ {shlex.join(command)}\n").encode())
		log.flush()
		return subprocess.run(command, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, check=False).returncode


def is_offline_resolution_failure(log_path: Path) -> bool:
	contents = log_path.read_text(errors="replace").lower()
	return any(marker in contents for marker in OFFLINE_MISSING_MARKERS) and "offline" in contents


def run_maven(
	command: list[str],
	cwd: Path,
	log_path: Path,
	description: str,
	command_records: list[dict[str, object]],
	command_records_path: Path,
) -> None:
	def attempt(argv: list[str]) -> int:
		status = run_logged(argv, cwd, log_path)
		command_records.append(
			{
				"stage": description,
				"attempt": sum(record["stage"] == description for record in command_records) + 1,
				"argv": argv,
				"shell_command": shlex.join(argv),
				"cwd": str(cwd.resolve()),
				"log": str(log_path.resolve()),
				"exit_code": status,
			}
		)
		command_records_path.write_text(json.dumps(command_records, indent=2) + "\n", encoding="utf-8")
		return status

	print(f"[{description}] {shlex.join(command)}", flush=True)
	status = attempt(command)
	if status == 0:
		return

	if "-o" in command and is_offline_resolution_failure(log_path):
		online_command = [part for part in command if part != "-o"]
		print(f"[{description}] offline dependency missing; retrying once online", flush=True)
		status = attempt(online_command)
		if status == 0:
			return
	elif "-T" in command:
		serial_command = command.copy()
		thread_flag = serial_command.index("-T")
		del serial_command[thread_flag : thread_flag + 2]
		print(f"[{description}] parallel Maven build failed; retrying once serially", flush=True)
		status = attempt(serial_command)
		if status == 0:
			return

	raise RuntimeError(
		f"{description} failed with exit code {status}; full output is preserved in {log_path}"
	)


def parse_revisions(value: str) -> dict[str, str]:
	result: dict[str, str] = {}
	for item in value.split(","):
		if "=" not in item:
			raise argparse.ArgumentTypeError(f"revision must use label=commit syntax: {item!r}")
		label, revision = item.split("=", 1)
		if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", label):
			raise argparse.ArgumentTypeError(f"unsafe runtime label: {label!r}")
		if not revision:
			raise argparse.ArgumentTypeError(f"revision is empty for {label!r}")
		if label in result:
			raise argparse.ArgumentTypeError(f"duplicate runtime label: {label!r}")
		result[label] = revision
	if not result:
		raise argparse.ArgumentTypeError("at least one label=commit revision is required")
	return result


def git_output(repository: Path, *args: str) -> str:
	return subprocess.check_output(["git", "-C", str(repository), *args], text=True).strip()


def archive_source(repository: Path, revision: str, label_dir: Path) -> tuple[Path, str, str]:
	resolved_revision = git_output(repository, "rev-parse", "--verify", f"{revision}^{{commit}}")
	archive_path = label_dir / "source.tar.gz"
	archive_command = ["git", "-C", str(repository), "archive", "--format=tar", resolved_revision]
	with archive_path.open("wb") as raw_archive:
		process = subprocess.Popen(archive_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
		assert process.stdout is not None
		with gzip.GzipFile(fileobj=raw_archive, mode="wb", compresslevel=1, mtime=0) as compressed:
			shutil.copyfileobj(process.stdout, compressed, length=1024 * 1024)
		stderr = process.stderr.read() if process.stderr is not None else b""
		status = process.wait()
	if status != 0:
		raise RuntimeError(f"git archive failed for {resolved_revision}: {stderr.decode(errors='replace')}")
	return archive_path, resolved_revision, sha256(archive_path)


def extract_archive(archive_path: Path, destination: Path) -> None:
	destination.mkdir(parents=True, exist_ok=False)
	with tarfile.open(archive_path, mode="r:gz") as archive:
		archive.extractall(destination, filter="data")


def project_version(source_root: Path) -> str:
	root = ET.parse(source_root / "pom.xml").getroot()
	namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
	version = root.findtext("m:version", namespaces=namespace)
	if not version:
		raise RuntimeError(f"could not read RDF4J version from {source_root / 'pom.xml'}")
	return version


def write_runtime_pom(source_root: Path, version: str, label: str) -> Path:
	pom_path = source_root / f"roundtrip-runtime-{label}.pom.xml"
	dependencies = "\n".join(
		f"""
		<dependency>
			<groupId>org.eclipse.rdf4j</groupId>
			<artifactId>{artifact_id}</artifactId>
			<version>${{project.version}}</version>
		</dependency>"""
		for artifact_id in RUNTIME_ARTIFACTS
	)
	pom_path.write_text(
		f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.eclipse.rdf4j</groupId>
		<artifactId>rdf4j</artifactId>
		<version>{version}</version>
		<relativePath>pom.xml</relativePath>
	</parent>
	<artifactId>rdf4j-lmdb-roundtrip-runtime-{label}</artifactId>
	<packaging>pom</packaging>
	<dependencies>{dependencies}
	</dependencies>
</project>
""",
		encoding="utf-8",
	)
	return pom_path


def build_revision(
	repository: Path,
	workspace_maven_repo: Path,
	output_root: Path,
	build_root: Path,
	label: str,
	revision: str,
) -> dict[str, object]:
	label_dir = output_root / label
	label_dir.mkdir(parents=True, exist_ok=False)
	archive_path, resolved_revision, archive_sha256 = archive_source(repository, revision, label_dir)
	source_root = build_root / label
	extract_archive(archive_path, source_root)

	local_repo_link = source_root / ".m2_repo"
	if local_repo_link.exists() or local_repo_link.is_symlink():
		raise RuntimeError(f"source archive unexpectedly contains {local_repo_link}")
	local_repo_link.symlink_to(workspace_maven_repo, target_is_directory=True)
	version = project_version(source_root)
	if version not in git_output(repository, "show", f"{resolved_revision}:pom.xml"):
		raise AssertionError("version consistency check failed")

	build_log = label_dir / "maven-build.log"
	command_records: list[dict[str, object]] = []
	command_records_path = label_dir / "commands.json"
	build_command = [
		"mvn",
		"-B",
		"-ntp",
		"-Dmaven.compiler.showWarnings=false",
		"-T",
		"1C",
		"-o",
		"-Dmaven.repo.local=.m2_repo",
		*FORMATTER_SKIP_FLAGS,
		"-pl",
		",".join(RDF4J_RUNTIME_MODULES),
		"-am",
		"-Pquick",
		"clean",
		"install",
	]
	run_maven(
		build_command,
		source_root,
		build_log,
		f"{label} scoped dependencies install",
		command_records,
		command_records_path,
	)

	runtime_dir = label_dir / "runtime"
	library_dir = runtime_dir / "lib"
	library_dir.mkdir(parents=True, exist_ok=False)
	runtime_pom = write_runtime_pom(source_root, version, label)
	copy_log = label_dir / "dependency-copy.log"
	copy_command = [
		"mvn",
		"-B",
		"-ntp",
		"-o",
		"-Dmaven.repo.local=.m2_repo",
		"-f",
		runtime_pom.name,
		"org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies",
		"-DincludeScope=runtime",
		"-Dmdep.useRepositoryLayout=true",
		f"-DoutputDirectory={library_dir}",
	]
	run_maven(
		copy_command,
		source_root,
		copy_log,
		f"{label} runtime dependency package",
		command_records,
		command_records_path,
	)

	jar_paths = sorted(library_dir.rglob("*.jar"))
	if not jar_paths:
		raise RuntimeError(f"Maven copied no runtime JARs for {label}")
	for jar_path in jar_paths:
		if ".m2_repo" in jar_path.parts or "target" in jar_path.parts:
			raise RuntimeError(f"runtime JAR path is not self-contained: {jar_path}")
		jar_path.chmod(0o444)
	artifact_entries = [
		{
			"path": str(jar_path.resolve()),
			"sha256": sha256(jar_path),
			"size_bytes": jar_path.stat().st_size,
		}
		for jar_path in jar_paths
	]
	artifact_manifest_path = runtime_dir / "artifact-manifest.json"
	artifact_manifest_path.write_text(
		json.dumps(
			{
				"label": label,
				"revision": resolved_revision,
				"version": version,
				"source_archive": str(archive_path.resolve()),
				"source_archive_sha256": archive_sha256,
				"artifacts": artifact_entries,
			},
			indent=2,
		)
		+ "\n",
		encoding="utf-8",
	)
	artifact_manifest_path.chmod(0o444)
	for directory, child_dirs, _ in os.walk(library_dir, topdown=False):
		for child_dir in child_dirs:
			Path(directory, child_dir).chmod(0o555)
		Path(directory).chmod(0o555)
	library_dir.chmod(0o555)
	runtime_dir.chmod(0o555)

	classpath = [entry["path"] for entry in artifact_entries]
	for path in classpath:
		if not Path(path).is_file() or not str(path).startswith(str(runtime_dir.resolve()) + os.sep):
			raise RuntimeError(f"runtime classpath escapes packaged runtime: {path}")
	return {
		"revision": resolved_revision,
		"version": version,
		"runtime_dir": str(runtime_dir.resolve()),
		"classpath": classpath,
		"source_archive": str(archive_path.resolve()),
		"source_archive_sha256": archive_sha256,
		"artifacts": artifact_entries,
	}


def prepare_runtimes(repository: Path, output_dir: Path, sources: dict[str, str]) -> dict[str, dict[str, object]]:
	repository = repository.resolve()
	output_dir = output_dir.resolve()
	if not (repository / ".git").exists():
		raise RuntimeError(f"repository has no .git directory: {repository}")
	if not repository.is_dir():
		raise RuntimeError(f"repository does not exist: {repository}")
	workspace_maven_repo = repository / ".m2_repo"
	if not workspace_maven_repo.is_dir():
		raise RuntimeError(f"expected workspace Maven cache at {workspace_maven_repo}")
	if output_dir.exists():
		raise RuntimeError(f"refusing to overwrite existing artifact directory: {output_dir}")
	output_dir.parent.mkdir(parents=True, exist_ok=True)
	output_dir.mkdir(parents=False, exist_ok=False)
	build_tmp = Path("/private/tmp") if Path("/private/tmp").is_dir() else Path("/tmp")
	build_root = Path(
		tempfile.mkdtemp(
			prefix=f"rdf4j-lmdb-roundtrip-build-{uuid.uuid4().hex[:10]}-",
			dir=build_tmp,
		)
	)
	print(f"Build archives and target trees: {build_root}", flush=True)
	runtimes: dict[str, dict[str, object]] = {}
	for label, revision in sources.items():
		runtimes[label] = build_revision(
			repository, workspace_maven_repo, output_dir, build_root, label, revision
		)
	manifest_path = output_dir / "runtimes.json"
	manifest_path.write_text(json.dumps(runtimes, indent=2) + "\n", encoding="utf-8")
	manifest_path.chmod(0o444)
	return runtimes


def main(argv: Iterable[str] | None = None) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--repo-root", type=Path, required=True, help="working RDF4J Git checkout")
	parser.add_argument("--output-dir", type=Path, required=True, help="new, durable output directory")
	parser.add_argument(
		"--revisions",
		type=parse_revisions,
		required=True,
		help="comma-separated label=commit pairs",
	)
	args = parser.parse_args(argv)
	try:
		runtimes = prepare_runtimes(args.repo_root, args.output_dir, args.revisions)
	except Exception as error:  # retain any created archive, logs, and incomplete runtime for diagnosis
		print(f"ERROR: {error}", file=sys.stderr)
		return 1
	print(f"Packaged runtimes: {args.output_dir.resolve() / 'runtimes.json'}")
	for label, runtime in runtimes.items():
		print(
			f"{label}: RDF4J {runtime['version']}, {len(runtime['classpath'])} JARs, "
			f"runtime={runtime['runtime_dir']}"
		)
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
