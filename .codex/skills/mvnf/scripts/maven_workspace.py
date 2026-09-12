#!/usr/bin/env python3

from __future__ import annotations

import contextlib
import datetime
import fcntl
import json
import os
import re
import shlex
import shutil
import signal
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET
from collections.abc import Iterator, Mapping, Sequence
from dataclasses import dataclass
from pathlib import Path


WORKSPACE_ID_PATTERN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,63}\Z")
MAVEN_VERSION_PATTERN = re.compile(r"(?m)^Apache Maven\s+(\d+)\.(\d+)\.(\d+)(?:[-+][^\s]+)?")
THREAD_COUNT_PATTERN = re.compile(r"[1-9][0-9]*\Z")

SAFE_FAIL_STRATEGY_OPTIONS = frozenset({"-fae", "-ff", "-fn"})
OPTIONS_WITH_SEPARATE_VALUES = frozenset(
    {
        "-b",
        "--builder",
        "--color",
        "-gs",
        "--global-settings",
        "--global-toolchains",
        "-pl",
        "--projects",
        "-rf",
        "--resume-from",
        "-s",
        "--settings",
        "-t",
        "--toolchains",
    }
)

OWNED_PROPERTIES = frozenset(
    {
        "maven.repo.local",
        "maven.repo.local.head",
        "rdf4j.build.root",
        "rdf4j.test.outputDirectory",
        "rdf4j.test.tmpRoot",
        "rdf4j.test.tmpDirectory",
        "java.io.tmpdir",
        "formatter.skip",
    }
)

OWNED_PROFILES = frozenset({"workspace-build-root"})

MUTATING_LIFECYCLE_GOALS = frozenset(
    {
        "deploy",
        "site-deploy",
    }
)

MUTATING_PLUGIN_MARKERS = frozenset(
    {
        "flatten",
        "release",
        "scm",
        "versions",
    }
)

SOURCE_GENERATOR_MARKERS = frozenset(
    {
        "antlr",
        "avro",
        "generator",
        "javacc",
        "jjtree",
        "jooq",
        "openapi",
        "protobuf",
    }
)

SOURCE_GENERATOR_GOALS = frozenset(
    {
        "antlr",
        "antlr4",
        "generate",
        "generate-sources",
        "generate-test-sources",
        "javacc",
        "jjtree",
    }
)


class MavenWorkspaceError(RuntimeError):
    """A workspace contract violation that must stop before Maven build work starts."""


class MavenWorkspaceInterrupted(MavenWorkspaceError):
    """The runner received a termination signal while Maven was active."""

    def __init__(self, signum: int):
        self.signum = signum
        super().__init__(f"Maven run interrupted by signal {signum}")


@dataclass(frozen=True)
class WorkspacePaths:
    workspace_id: str
    root: Path
    repository: Path
    build_root: Path
    tmp_dir: Path
    log_dir: Path
    run_id: str


@dataclass(frozen=True)
class ProjectGav:
    group_id: str
    artifact_id: str
    version: str
    module_dir: Path

    def relative_path(self) -> Path:
        return Path(self.group_id) / self.artifact_id / self.version


@dataclass(frozen=True)
class _ProjectModel:
    gav: ProjectGav
    properties: Mapping[str, str]


@dataclass(frozen=True)
class RunRegistration:
    run_id: str
    workspace_id: str | None
    pid: int
    record_path: Path


@dataclass(frozen=True)
class RegisteredChild:
    pid: int
    process_start: str | None
    process_group: int


def resolve_workspace(cli_value: str | None, environment: Mapping[str, str]) -> str | None:
    value = cli_value if cli_value is not None else environment.get("MVNF_WORKSPACE")
    return None if value is None else validate_workspace_id(value)


def validate_workspace_id(value: str) -> str:
    if not isinstance(value, str) or WORKSPACE_ID_PATTERN.fullmatch(value) is None:
        raise MavenWorkspaceError(
            "Invalid workspace ID; expected [A-Za-z0-9][A-Za-z0-9._-]{0,63} exactly "
            "(no whitespace, separators, or traversal)."
        )
    return value


def validate_threads(value: str | int | None) -> str:
    normalized = "1" if value is None else str(value)
    if THREAD_COUNT_PATTERN.fullmatch(normalized) is None:
        raise MavenWorkspaceError("--threads must be a positive integer")
    return normalized


def create_workspace_paths(
    repo_root: Path,
    workspace_id: str,
    *,
    pid: int | None = None,
) -> WorkspacePaths:
    validated_id = validate_workspace_id(workspace_id)
    repository_root = repo_root.resolve()
    workspace_parent = repository_root / ".mvnf" / "workspaces"
    _create_directory_within(workspace_parent, repository_root)

    root = workspace_parent / validated_id
    _create_directory_within(root, workspace_parent)
    process_id = os.getpid() if pid is None else pid
    run_id = _new_run_id(process_id)
    paths = WorkspacePaths(
        workspace_id=validated_id,
        root=root,
        repository=root / "repository",
        build_root=root / "build",
        tmp_dir=root / "tmp" / run_id,
        log_dir=root / "logs" / run_id,
        run_id=run_id,
    )
    for directory in (paths.repository, paths.build_root, paths.tmp_dir, paths.log_dir):
        _create_directory_within(directory, root)
    return paths


def validate_workspace_owned_path(path: Path, boundary: Path) -> Path:
    boundary_path = Path(os.path.abspath(boundary))
    candidate_path = Path(os.path.abspath(path))
    try:
        relative = candidate_path.relative_to(boundary_path)
    except ValueError as exc:
        raise MavenWorkspaceError(
            f"Workspace-owned path escapes its boundary: {candidate_path} (boundary {boundary_path})"
        ) from exc

    current = boundary_path
    if current.is_symlink():
        raise MavenWorkspaceError(f"Workspace boundary must not be a symlink: {current}")
    for component in relative.parts:
        current /= component
        if current.is_symlink():
            raise MavenWorkspaceError(f"Workspace-owned path must not traverse a symlink: {current}")

    resolved_boundary = boundary_path.resolve()
    resolved_candidate = candidate_path.resolve()
    try:
        resolved_candidate.relative_to(resolved_boundary)
    except ValueError as exc:
        raise MavenWorkspaceError(
            f"Workspace-owned path resolves outside its boundary: {candidate_path} -> {resolved_candidate}"
        ) from exc
    return candidate_path


def validate_reused_workspace_trees(paths: WorkspacePaths) -> None:
    workspace_root = paths.root.resolve()
    for tree in (paths.repository, paths.build_root):
        validate_workspace_owned_path(tree, paths.root)
        for current, directory_names, file_names in os.walk(tree, topdown=True, followlinks=False):
            current_path = Path(current)
            for name in tuple(directory_names):
                candidate = current_path / name
                if candidate.is_symlink():
                    _validate_nested_symlink_target(candidate, workspace_root)
                    directory_names.remove(name)
            for name in file_names:
                candidate = current_path / name
                if candidate.is_symlink():
                    _validate_nested_symlink_target(candidate, workspace_root)


def _validate_nested_symlink_target(candidate: Path, workspace_root: Path) -> None:
    resolved_target = candidate.resolve(strict=False)
    try:
        resolved_target.relative_to(workspace_root)
    except ValueError as exc:
        raise MavenWorkspaceError(
            f"Reusable workspace tree contains a symlink outside its workspace: "
            f"{candidate} -> {resolved_target}"
        ) from exc


def _create_directory_within(path: Path, boundary: Path) -> Path:
    validated = validate_workspace_owned_path(path, boundary)
    validated.mkdir(parents=True, exist_ok=True)
    return validate_workspace_owned_path(validated, boundary)


def validate_maven_version(
    output: str,
    minimum: tuple[int, int, int] = (3, 9, 10),
) -> tuple[int, int, int]:
    match = MAVEN_VERSION_PATTERN.search(output or "")
    if match is None:
        raise MavenWorkspaceError("Could not parse `Apache Maven X.Y.Z` from `mvn --version` output")
    version = tuple(int(part) for part in match.groups())
    if version < minimum:
        required = ".".join(str(part) for part in minimum)
        actual = ".".join(str(part) for part in version)
        raise MavenWorkspaceError(
            f"Workspace mode requires Maven {required} or newer for maven.repo.local.head; found {actual}"
        )
    return version


def validate_forwarded_arguments(
    arguments: Sequence[str],
    *,
    workspace_mode: bool,
    allow_project_selection: bool = False,
    test_runner: bool = False,
) -> None:
    if not workspace_mode:
        return
    values = list(arguments)
    index = 0
    while index < len(values):
        argument = values[index]
        property_name: str | None = None
        if argument in {"-D", "--define"}:
            if index + 1 < len(values):
                index += 1
                property_name = values[index].split("=", 1)[0]
        elif argument.startswith("--define="):
            property_name = argument.split("=", 1)[1].split("=", 1)[0]
        elif argument.startswith("-D") and len(argument) > 2:
            property_name = argument[2:].split("=", 1)[0]
        if property_name in OWNED_PROPERTIES:
            raise MavenWorkspaceError(
                f"Workspace mode owns -D{property_name}; remove the override and let the runner isolate it"
            )

        if (
            argument == "-T"
            or argument.startswith("-T") and len(argument) > 2
            or argument == "--threads"
            or argument.startswith("--threads=")
        ):
            raise MavenWorkspaceError("Use the runner's --threads option instead of forwarding Maven -T/--threads")
        if argument in {"-f", "--file"} or (
            argument.startswith("-f") and len(argument) > 2 and argument not in SAFE_FAIL_STRATEGY_OPTIONS
        ):
            raise MavenWorkspaceError(
                f"Workspace mode owns POM selection; `{argument}` can bypass isolated project routing"
            )
        if argument.startswith("--file="):
            raise MavenWorkspaceError(
                f"Workspace mode owns POM selection; `{argument}` can bypass isolated project routing"
            )
        if not allow_project_selection and _is_reactor_selection_argument(argument):
            raise MavenWorkspaceError(
                f"Workspace test mode owns reactor selection; `{argument}` can bypass the selected module"
            )
        if test_runner and argument in {"-q", "--quiet"}:
            raise MavenWorkspaceError("Workspace test runs reject Maven quiet mode so test evidence remains visible")
        if (
            argument in {"-l", "--log-file"}
            or argument.startswith("-l") and len(argument) > 2
            or argument.startswith("--log-file=")
        ):
            raise MavenWorkspaceError(
                "Workspace mode owns Maven log routing; remove -l/--log-file and use the isolated run log"
            )
        if _is_direct_plugin_goal(argument):
            raise MavenWorkspaceError(
                f"Workspace mode rejects direct plugin goal `{argument}`; "
                "run source-mutating or ad hoc plugin work exclusively outside workspace mode"
            )
        if _is_mutating_goal(argument):
            raise MavenWorkspaceError(
                f"Workspace mode rejects source/release mutation `{argument}`; "
                "run it exclusively outside workspace mode"
            )
        profile_value: str | None = None
        if argument in {"-P", "--activate-profiles"} and index + 1 < len(values):
            index += 1
            profile_value = values[index]
        elif argument.startswith("--activate-profiles="):
            profile_value = argument.split("=", 1)[1]
        elif argument.startswith("-P") and len(argument) > 2:
            profile_value = argument[2:]
        if profile_value is not None:
            profiles = [_normalized_profile_id(profile) for profile in profile_value.split(",")]
            owned_profiles = OWNED_PROFILES.intersection(profiles)
            if owned_profiles:
                owned_profile = sorted(owned_profiles)[0]
                raise MavenWorkspaceError(
                    f"Workspace mode owns Maven profile `{owned_profile}`; "
                    "remove explicit activation or deactivation and let rdf4j.build.root activate it"
                )
            if any(any(token in profile for token in ("release", "deploy", "format")) for profile in profiles):
                raise MavenWorkspaceError(
                    f"Workspace mode rejects mutating profile `{argument}`; run it exclusively"
                )
        if argument in OPTIONS_WITH_SEPARATE_VALUES and index + 1 < len(values):
            index += 1
        index += 1


def _normalized_profile_id(profile: str) -> str:
    return profile.strip().lstrip("=!?+-").strip().lower()


def validate_inherited_maven_arguments(
    environment: Mapping[str, str],
    *,
    allow_project_selection: bool = False,
    test_runner: bool = False,
) -> None:
    raw_arguments = environment.get("MAVEN_ARGS")
    if not raw_arguments:
        return
    try:
        arguments = shlex.split(raw_arguments)
    except ValueError as exc:
        raise MavenWorkspaceError(f"Could not parse inherited MAVEN_ARGS: {exc}") from exc
    try:
        validate_forwarded_arguments(
            arguments,
            workspace_mode=True,
            allow_project_selection=allow_project_selection,
            test_runner=test_runner,
        )
    except MavenWorkspaceError as exc:
        raise MavenWorkspaceError(f"Unsafe inherited MAVEN_ARGS: {exc}") from exc


def validate_project_maven_config(
    repo_root: Path,
    *,
    allow_project_selection: bool = False,
    test_runner: bool = False,
) -> None:
    config_path = repo_root / ".mvn" / "maven.config"
    if not config_path.exists():
        return
    try:
        raw_arguments = config_path.read_text(encoding="utf-8")
    except OSError as exc:
        raise MavenWorkspaceError(f"Could not read project Maven configuration {config_path}: {exc}") from exc
    try:
        arguments = shlex.split(raw_arguments, comments=True)
    except ValueError as exc:
        raise MavenWorkspaceError(f"Could not parse project Maven configuration {config_path}: {exc}") from exc
    try:
        validate_forwarded_arguments(
            arguments,
            workspace_mode=True,
            allow_project_selection=allow_project_selection,
            test_runner=test_runner,
        )
    except MavenWorkspaceError as exc:
        raise MavenWorkspaceError(f"Unsafe .mvn/maven.config: {exc}") from exc


def _is_reactor_selection_argument(argument: str) -> bool:
    if argument in {
        "-N",
        "--non-recursive",
        "-pl",
        "--projects",
        "-rf",
        "--resume-from",
        "-am",
        "--also-make",
        "-amd",
        "--also-make-dependents",
    }:
        return True
    return (
        argument.startswith("-pl") and len(argument) > 3
        or argument.startswith("--projects=")
        or argument.startswith("-rf") and len(argument) > 3
        or argument.startswith("--resume-from=")
    )


def _is_direct_plugin_goal(argument: str) -> bool:
    normalized = argument.split("@", 1)[0]
    return not normalized.startswith("-") and len(normalized.split(":")) >= 2


def _is_mutating_goal(argument: str) -> bool:
    normalized = argument.lower()
    if normalized in MUTATING_LIFECYCLE_GOALS or normalized in SOURCE_GENERATOR_GOALS:
        return True
    if normalized.startswith("-") or ":" not in normalized:
        return False

    components = [component for component in normalized.split(":") if component]
    if len(components) < 2:
        return False
    goal = components[-1].split("@", 1)[0]
    plugin_components = components[:-1]
    plugin_identity = ":".join(plugin_components)

    if goal == "deploy":
        return True
    if goal == "deploy-file" and any(
        component == "deploy" or "maven-deploy-plugin" in component for component in plugin_components
    ):
        return True
    if any(marker in plugin_identity for marker in MUTATING_PLUGIN_MARKERS):
        return goal != "help"
    if "spotless" in plugin_identity and goal == "apply":
        return True
    if any(marker in plugin_identity for marker in ("fmt", "formatter", "license")) and goal == "format":
        return True
    if "sortpom" in plugin_identity and goal == "sort":
        return True
    if goal in SOURCE_GENERATOR_GOALS:
        return True
    return any(marker in plugin_identity for marker in SOURCE_GENERATOR_MARKERS) and goal != "help"


def isolation_arguments(repo_root: Path, paths: WorkspacePaths, threads: str | int | None) -> list[str]:
    thread_count = validate_threads(threads)
    root = repo_root.resolve()
    return [
        f"-Dmaven.repo.local={root / '.m2_repo'}",
        f"-Dmaven.repo.local.head={paths.repository}",
        f"-Drdf4j.build.root={paths.build_root}",
        f"-Drdf4j.test.tmpRoot={paths.tmp_dir}",
        "-Dformatter.skip=true",
        "-T",
        thread_count,
    ]


def find_repo_root(start: Path) -> Path:
    candidate = start.resolve()
    if candidate.is_file():
        candidate = candidate.parent
    for current in (candidate, *candidate.parents):
        if (current / "pom.xml").is_file() and (current / ".git").exists():
            return current
    for current in (candidate, *candidate.parents):
        if (current / "pom.xml").is_file():
            return current
    raise MavenWorkspaceError("Could not locate a Maven repository root")


def default_maven_command(repo_root: Path) -> list[str]:
    wrapper = repo_root / "mvnw"
    if wrapper.is_file():
        return [str(wrapper)] if os.access(wrapper, os.X_OK) else ["sh", str(wrapper)]
    return ["mvn"]


def load_project_gav(pom_path: Path) -> ProjectGav:
    return _load_project_model(pom_path.resolve(), {}).gav


def reactor_projects(repo_root: Path) -> list[ProjectGav]:
    root_pom = repo_root.resolve() / "pom.xml"
    pending = [root_pom]
    seen: set[Path] = set()
    models: dict[Path, _ProjectModel] = {}
    projects: list[ProjectGav] = []
    while pending:
        pom_path = pending.pop(0).resolve()
        if pom_path in seen or not pom_path.is_file():
            continue
        seen.add(pom_path)
        model = _load_project_model(pom_path, models)
        projects.append(model.gav)
        root = _parse_xml(pom_path)
        module_values: list[str] = []
        direct_modules = _child(root, "modules")
        if direct_modules is not None:
            module_values.extend(_text(module) for module in _children(direct_modules, "module"))
        profiles = _child(root, "profiles")
        if profiles is not None:
            for profile in _children(profiles, "profile"):
                profile_modules = _child(profile, "modules")
                if profile_modules is not None:
                    module_values.extend(_text(module) for module in _children(profile_modules, "module"))
        for module_value in module_values:
            if not module_value:
                continue
            module_pom = (pom_path.parent / module_value / "pom.xml").resolve()
            if module_pom.is_file():
                pending.append(module_pom)
    return projects


def project_build_directory(paths: WorkspacePaths, gav: ProjectGav) -> Path:
    directory = paths.build_root / gav.relative_path()
    validate_workspace_owned_path(directory, paths.build_root)
    return directory


def prepare_reactor_tmp_directories(repo_root: Path, paths: WorkspacePaths) -> list[Path]:
    directories: list[Path] = []
    for project in reactor_projects(repo_root):
        project_build_directory(paths, project)
        directory = paths.tmp_dir / project.relative_path()
        _create_directory_within(directory, paths.tmp_dir)
        directories.append(directory)
    return directories


def write_run_metadata(
    paths: WorkspacePaths,
    *,
    runner: str,
    command: Sequence[str],
    module: str | None = None,
) -> Path:
    metadata = {
        "runId": paths.run_id,
        "workspace": paths.workspace_id,
        "pid": os.getpid(),
        "createdAt": datetime.datetime.now(datetime.UTC).isoformat(),
        "runner": runner,
        "module": module,
        "command": list(command),
    }
    destination = paths.log_dir / "invocation.json"
    destination.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return destination


@contextlib.contextmanager
def registered_run(
    repo_root: Path,
    workspace_id: str | None,
    command: Sequence[str],
) -> Iterator[RunRegistration]:
    normalized_workspace = None if workspace_id is None else validate_workspace_id(workspace_id)
    resolved_repo_root = repo_root.resolve()
    registry = resolved_repo_root / ".mvnf" / "runs"
    _create_directory_within(registry, resolved_repo_root)
    run_id = _new_run_id(os.getpid())
    record_path = registry / f"{run_id}.json"
    registration = RunRegistration(run_id, normalized_workspace, os.getpid(), record_path)
    with _registry_lock(registry):
        active_records = _active_records(registry)
        active_records.extend(
            _active_pre_workspace_records(
                resolved_repo_root / "target" / "mvnf-runs",
                registration.pid,
            )
        )
        conflict = next(
            (
                record
                for record in active_records
                if normalized_workspace is None
                or _record_is_legacy(record)
                or _record_owns_workspace_storage(record, normalized_workspace, resolved_repo_root)
            ),
            None,
        )
        if conflict is not None:
            requested = "legacy" if normalized_workspace is None else normalized_workspace
            owner = "legacy" if _record_is_legacy(conflict) else conflict.get("workspace", "unknown")
            owner_pid = conflict.get("activePid", conflict.get("pid", "unknown"))
            raise MavenWorkspaceError(
                f"Maven run scope `{requested}` conflicts with active `{owner}` run "
                f"owned by pid {owner_pid}"
            )
        record = {
            "pid": registration.pid,
            "processStart": _process_start_identity(registration.pid),
            "children": [],
            "mode": "legacy" if normalized_workspace is None else "workspace",
            "workspace": normalized_workspace,
            "command": [str(part) for part in command],
            "createdAt": datetime.datetime.now(datetime.UTC).isoformat(),
            "runId": run_id,
        }
        temporary = record_path.with_suffix(".tmp")
        temporary.write_text(json.dumps(record, sort_keys=True) + "\n", encoding="utf-8")
        os.replace(temporary, record_path)
    try:
        yield registration
    finally:
        with _registry_lock(registry):
            record_path.unlink(missing_ok=True)


def _record_is_legacy(record: Mapping[str, object]) -> bool:
    mode = record.get("mode")
    if mode is not None:
        return mode == "legacy"
    return record.get("workspace") == "legacy"


def _record_owns_workspace_storage(
    record: Mapping[str, object],
    workspace_id: str,
    repo_root: Path,
) -> bool:
    owner = record.get("workspace")
    if owner == workspace_id:
        return True
    if not isinstance(owner, str) or WORKSPACE_ID_PATTERN.fullmatch(owner) is None:
        return False
    workspace_root = repo_root / ".mvnf" / "workspaces"
    requested_path = workspace_root / workspace_id
    owner_path = workspace_root / owner
    try:
        return requested_path.samefile(owner_path)
    except OSError:
        return False


@contextlib.contextmanager
def tracked_maven_process(
    registration: RunRegistration,
    command: Sequence[str],
    **popen_arguments: object,
) -> Iterator[subprocess.Popen[str]]:
    arguments = dict(popen_arguments)
    arguments["start_new_session"] = True
    process = subprocess.Popen(list(command), **arguments)
    try:
        process_group = os.getpgid(process.pid)
    except BaseException:
        _terminate_process_group(process, process.pid)
        raise

    child: RegisteredChild | None = None
    previous_handlers: dict[signal.Signals, object] = {}
    interrupted_signal: int | None = None

    def forward_signal(signum: int, _frame: object) -> None:
        nonlocal interrupted_signal
        if interrupted_signal is None:
            interrupted_signal = signum
        _signal_process_group(process, process_group, signum)

    try:
        for signal_number in (signal.SIGINT, signal.SIGTERM):
            try:
                previous_handlers[signal_number] = signal.getsignal(signal_number)
                signal.signal(signal_number, forward_signal)
            except ValueError:
                for installed_signal, previous_handler in previous_handlers.items():
                    signal.signal(installed_signal, previous_handler)
                previous_handlers.clear()
                break
        child = _register_child(registration, process.pid, process_group)
        child = _enrich_child_identity(registration, child)
        yield process
    finally:
        for signal_number, previous_handler in previous_handlers.items():
            signal.signal(signal_number, previous_handler)
        _terminate_process_group(process, process_group)
        if child is not None:
            _unregister_child(registration, child)
    if interrupted_signal is not None:
        raise MavenWorkspaceInterrupted(interrupted_signal)


def _register_child(registration: RunRegistration, pid: int, process_group: int) -> RegisteredChild:
    child = RegisteredChild(pid, None, process_group)
    registry = registration.record_path.parent
    with _registry_lock(registry):
        record = _read_registration_record(registration.record_path)
        children = record.setdefault("children", [])
        if not isinstance(children, list):
            raise MavenWorkspaceError(f"Invalid child registry in {registration.record_path}")
        children.append(
            {
                "pid": child.pid,
                "processStart": child.process_start,
                "processGroup": child.process_group,
            }
        )
        _write_registration_record(registration.record_path, record)
    return child


def _enrich_child_identity(registration: RunRegistration, child: RegisteredChild) -> RegisteredChild:
    process_start = _process_start_identity(child.pid)
    if process_start is None:
        return child
    registry = registration.record_path.parent
    with _registry_lock(registry):
        if not registration.record_path.is_file():
            return child
        record = _read_registration_record(registration.record_path)
        children = record.get("children", [])
        if not isinstance(children, list):
            return child
        for entry in children:
            if not isinstance(entry, dict):
                continue
            if int(entry.get("pid", -1)) == child.pid and entry.get("processStart") is None:
                entry["processStart"] = process_start
                _write_registration_record(registration.record_path, record)
                return RegisteredChild(child.pid, process_start, child.process_group)
    return child


def _unregister_child(registration: RunRegistration, child: RegisteredChild) -> None:
    registry = registration.record_path.parent
    with _registry_lock(registry):
        if not registration.record_path.is_file():
            return
        record = _read_registration_record(registration.record_path)
        children = record.get("children", [])
        if not isinstance(children, list):
            return
        record["children"] = [
            entry
            for entry in children
            if not isinstance(entry, dict)
            or int(entry.get("pid", -1)) != child.pid
            or entry.get("processStart") != child.process_start
        ]
        _write_registration_record(registration.record_path, record)


def _read_registration_record(record_path: Path) -> dict[str, object]:
    try:
        value = json.loads(record_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise MavenWorkspaceError(f"Could not read Maven run registry record {record_path}: {exc}") from exc
    if not isinstance(value, dict):
        raise MavenWorkspaceError(f"Invalid Maven run registry record {record_path}")
    return value


def _write_registration_record(record_path: Path, record: Mapping[str, object]) -> None:
    temporary = record_path.with_suffix(f".tmp-{uuid.uuid4().hex[:8]}")
    temporary.write_text(json.dumps(record, sort_keys=True) + "\n", encoding="utf-8")
    os.replace(temporary, record_path)


def _signal_process_group(process: subprocess.Popen[str], process_group: int, signum: int) -> None:
    try:
        os.killpg(process_group, signum)
    except ProcessLookupError:
        return
    except (AttributeError, PermissionError, OSError):
        try:
            process.send_signal(signum)
        except ProcessLookupError:
            pass


def _terminate_process_group(process: subprocess.Popen[str], process_group: int) -> None:
    _signal_process_group(process, process_group, signal.SIGTERM)
    try:
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        _signal_process_group(process, process_group, signal.SIGKILL)
        process.wait()

    deadline = time.monotonic() + 5
    while _process_group_is_running(process_group) and time.monotonic() < deadline:
        time.sleep(0.02)
    if _process_group_is_running(process_group):
        _signal_process_group(process, process_group, signal.SIGKILL)
        deadline = time.monotonic() + 5
        while _process_group_is_running(process_group) and time.monotonic() < deadline:
            time.sleep(0.02)


def _new_run_id(pid: int) -> str:
    timestamp = datetime.datetime.now(datetime.UTC).strftime("%Y%m%dT%H%M%S.%fZ")
    return f"{timestamp}-{pid}-{uuid.uuid4().hex[:8]}"


@contextlib.contextmanager
def _registry_lock(registry: Path) -> Iterator[None]:
    lock_path = validate_workspace_owned_path(registry / ".lock", registry)
    with lock_path.open("a+", encoding="utf-8") as lock_file:
        fcntl.flock(lock_file.fileno(), fcntl.LOCK_EX)
        try:
            yield
        finally:
            fcntl.flock(lock_file.fileno(), fcntl.LOCK_UN)


def _active_records(registry: Path) -> list[dict[str, object]]:
    active: list[dict[str, object]] = []
    for record_path in sorted(registry.glob("*.json")):
        try:
            record = json.loads(record_path.read_text(encoding="utf-8"))
            if not isinstance(record, dict):
                raise TypeError("record must be an object")
            pid = int(record["pid"])
            children = record.get("children", [])
            if not isinstance(children, list):
                raise TypeError("children must be a list")
        except (OSError, ValueError, TypeError, KeyError, json.JSONDecodeError) as exc:
            quarantine = record_path.with_name(record_path.name + f".corrupt-{uuid.uuid4().hex[:8]}")
            try:
                record_path.replace(quarantine)
            except OSError:
                pass
            raise MavenWorkspaceError(
                f"Corrupt Maven run registry record {record_path}; quarantined as {quarantine.name}. "
                "Inspect it before retrying."
            ) from exc
        if _recorded_process_is_active(pid, record.get("processStart")):
            active.append(record)
            continue

        active_child_pid: int | None = None
        for child_record in children:
            try:
                if not isinstance(child_record, dict):
                    raise TypeError("child must be an object")
                child_pid = int(child_record["pid"])
                child_group = int(child_record.get("processGroup", child_pid))
            except (ValueError, TypeError, KeyError) as exc:
                raise MavenWorkspaceError(f"Corrupt Maven child registry record in {record_path}") from exc
            if _recorded_process_is_active(child_pid, child_record.get("processStart")):
                active_child_pid = child_pid
                break
            if not _pid_is_running(child_pid) and _process_group_is_running(child_group):
                active_child_pid = child_pid
                break

        if active_child_pid is None:
            record_path.unlink(missing_ok=True)
            continue
        child_owned_record = dict(record)
        child_owned_record["activePid"] = active_child_pid
        active.append(child_owned_record)
    return active


def _active_pre_workspace_records(registry: Path, current_pid: int) -> list[dict[str, object]]:
    if not registry.is_dir():
        return []

    active: list[dict[str, object]] = []
    for marker in sorted(registry.iterdir()):
        if not marker.is_dir():
            continue
        try:
            pid = int(marker.name)
        except ValueError:
            continue
        if pid == current_pid:
            continue
        if not _pid_is_running(pid):
            _remove_stale_pre_workspace_marker(marker)
            continue
        try:
            started_at = (marker / "started-at").read_text(encoding="utf-8").strip()
        except OSError:
            started_at = "unknown"
        active.append(
            {
                "pid": pid,
                "mode": "legacy",
                "workspace": None,
                "createdAt": started_at,
                "legacyMarker": str(marker),
            }
        )
    return active


def _remove_stale_pre_workspace_marker(marker: Path) -> None:
    try:
        if marker.is_symlink():
            marker.unlink()
        else:
            shutil.rmtree(marker)
    except FileNotFoundError:
        pass
    except OSError:
        # A stale compatibility marker is not a lock owner. Preserve the old
        # runner's best-effort cleanup semantics if another process races us.
        pass


def _pid_is_running(pid: int) -> bool:
    if pid <= 0:
        return False
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    except OSError:
        return False


def _recorded_process_is_active(pid: int, recorded_start: object) -> bool:
    if not _pid_is_running(pid):
        return False
    current_start = _process_start_identity(pid)
    if recorded_start is not None and current_start is not None and current_start != recorded_start:
        return False
    return True


def _process_group_is_running(process_group: int) -> bool:
    if process_group <= 0:
        return False
    try:
        os.killpg(process_group, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    except OSError:
        return False


def _process_start_identity(pid: int) -> str | None:
    stat_path = Path("/proc") / str(pid) / "stat"
    try:
        stat_value = stat_path.read_text(encoding="utf-8")
        fields_after_name = stat_value.rsplit(") ", 1)[1].split()
        return fields_after_name[19]
    except (OSError, IndexError):
        try:
            result = subprocess.run(
                ["ps", "-p", str(pid), "-o", "lstart="],
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL,
                check=False,
                timeout=2,
            )
        except (OSError, subprocess.SubprocessError):
            return None
        identity = result.stdout.strip()
        return identity or None


def _load_project_model(pom_path: Path, cache: dict[Path, _ProjectModel]) -> _ProjectModel:
    cached = cache.get(pom_path)
    if cached is not None:
        return cached
    root = _parse_xml(pom_path)
    parent_element = _child(root, "parent")
    parent_model: _ProjectModel | None = None
    parent_group = _text(_child(parent_element, "groupId")) if parent_element is not None else ""
    parent_version = _text(_child(parent_element, "version")) if parent_element is not None else ""
    if parent_element is not None:
        relative_path_element = _child(parent_element, "relativePath")
        relative_path = "../pom.xml" if relative_path_element is None else _text(relative_path_element)
        if relative_path:
            parent_pom = (pom_path.parent / relative_path).resolve()
            if parent_pom.is_file() and parent_pom != pom_path:
                parent_model = _load_project_model(parent_pom, cache)
                parent_group = parent_model.gav.group_id
                parent_version = parent_model.gav.version

    properties: dict[str, str] = {}
    if parent_model is not None:
        properties.update(parent_model.properties)
    properties_element = _child(root, "properties")
    if properties_element is not None:
        for property_element in list(properties_element):
            properties[_local_name(property_element.tag)] = _text(property_element)

    raw_group = _text(_child(root, "groupId")) or parent_group
    raw_artifact = _text(_child(root, "artifactId"))
    raw_version = _text(_child(root, "version")) or parent_version
    coordinates = {
        "project.groupId": raw_group,
        "pom.groupId": raw_group,
        "project.artifactId": raw_artifact,
        "pom.artifactId": raw_artifact,
        "project.version": raw_version,
        "pom.version": raw_version,
        "project.parent.groupId": parent_group,
        "project.parent.version": parent_version,
    }
    resolved_properties = {**properties, **coordinates}
    group_id = _resolve_value(raw_group, resolved_properties)
    artifact_id = _resolve_value(raw_artifact, resolved_properties)
    version = _resolve_value(raw_version, resolved_properties)
    for coordinate_name, coordinate_value in (
        ("groupId", group_id),
        ("artifactId", artifact_id),
        ("version", version),
    ):
        _validate_coordinate(coordinate_name, coordinate_value, pom_path)
    model = _ProjectModel(ProjectGav(group_id, artifact_id, version, pom_path.parent), properties)
    cache[pom_path] = model
    return model


def _resolve_value(value: str, properties: Mapping[str, str]) -> str:
    result = value
    for _ in range(20):
        replaced = re.sub(r"\$\{([^}]+)}", lambda match: properties.get(match.group(1), match.group(0)), result)
        if replaced == result:
            break
        result = replaced
    return result


def _validate_coordinate(name: str, value: str, pom_path: Path) -> None:
    if not value or "${" in value or value in {".", ".."} or "/" in value or "\\" in value or "\0" in value:
        raise MavenWorkspaceError(f"Cannot derive safe {name} from {pom_path}: {value!r}")


def _parse_xml(pom_path: Path) -> ET.Element:
    try:
        return ET.parse(pom_path).getroot()
    except (ET.ParseError, OSError) as exc:
        raise MavenWorkspaceError(f"Could not parse Maven model {pom_path}: {exc}") from exc


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _child(element: ET.Element | None, name: str) -> ET.Element | None:
    if element is None:
        return None
    return next((child for child in list(element) if _local_name(child.tag) == name), None)


def _children(element: ET.Element, name: str) -> list[ET.Element]:
    return [child for child in list(element) if _local_name(child.tag) == name]


def _text(element: ET.Element | None) -> str:
    return "" if element is None or element.text is None else element.text.strip()


def quoted_command(command: Sequence[str]) -> str:
    return " ".join(shlex.quote(str(part)) for part in command)
