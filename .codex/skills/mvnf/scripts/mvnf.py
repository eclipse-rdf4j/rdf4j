#!/usr/bin/env python3

from __future__ import annotations

import argparse
import datetime
import os
import shlex
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable

from maven_workspace import (
    MavenWorkspaceError,
    MavenWorkspaceInterrupted,
    RunRegistration,
    WorkspacePaths,
    create_workspace_paths,
    default_maven_command,
    find_repo_root,
    isolation_arguments,
    load_project_gav,
    prepare_reactor_tmp_directories,
    project_build_directory,
    registered_run,
    resolve_workspace,
    tracked_maven_process,
    validate_forwarded_arguments,
    validate_inherited_maven_arguments,
    validate_maven_version,
    validate_project_maven_config,
    validate_reused_workspace_trees,
    validate_threads,
    validate_workspace_owned_path,
    write_run_metadata,
)


def _quote_cmd(cmd: list[str]) -> str:
    return " ".join(shlex.quote(part) for part in cmd)


def _maven_build_stream_filter() -> Callable[[str], bool]:
    summary = False

    def should_print(line: str) -> bool:
        nonlocal summary
        if "[WARNING]" in line:
            return False
        if "[ERROR]" in line:
            return True
        if "Reactor Summary" in line:
            summary = True
        return summary

    return should_print


def _find_repo_root() -> Path:
    try:
        return find_repo_root(Path.cwd())
    except MavenWorkspaceError:
        return find_repo_root(Path(__file__).resolve())


def _default_maven_cmd(repo_root: Path) -> list[str]:
    return default_maven_command(repo_root)


def _resolve_module_dir(repo_root: Path, module: str) -> Path | None:
    module_path = (repo_root / module).resolve()
    try:
        module_path.relative_to(repo_root.resolve())
    except ValueError:
        return None
    if not module_path.is_dir():
        return None
    if not (module_path / "pom.xml").is_file():
        return None
    return module_path


def _split_test_selector(selector: str) -> tuple[str, str | None]:
    if "#" not in selector:
        return selector, None
    class_part, method_part = selector.split("#", 1)
    return class_part, method_part


def _is_existing_file_path(repo_root: Path, maybe_path: str) -> Path | None:
    candidate = Path(maybe_path)
    if not candidate.is_absolute():
        candidate = repo_root / candidate
    if candidate.is_file():
        return candidate.resolve()
    return None


_TEST_DISCOVERY_EXCLUDED_DIRECTORIES = frozenset({".git", ".m2_repo", ".mvnf", "target"})


def _find_test_files(search_root: Path, class_name: str) -> list[Path]:
    simple = class_name.split(".")[-1]
    matches: list[Path] = []
    expected_names = {f"{simple}.java", f"{simple}.kt"}

    for current_root, directories, files in os.walk(search_root, topdown=True, followlinks=False):
        directories[:] = sorted(
            directory
            for directory in directories
            if directory not in _TEST_DISCOVERY_EXCLUDED_DIRECTORIES
        )
        current_path = Path(current_root)
        relative_parts = current_path.relative_to(search_root).parts
        if not any(
            relative_parts[index : index + 2] == ("src", "test")
            for index in range(len(relative_parts) - 1)
        ):
            continue
        matches.extend(current_path / filename for filename in files if filename in expected_names)

    unique = sorted({match.resolve() for match in matches})

    if "." in class_name:
        expected_java = "/".join(class_name.split(".")) + ".java"
        expected_kt = "/".join(class_name.split(".")) + ".kt"
        unique = [m for m in unique if m.as_posix().endswith(expected_java) or m.as_posix().endswith(expected_kt)]

    return unique


def _find_nearest_module_dir(repo_root: Path, file_path: Path) -> Path:
    for current in [file_path.parent] + list(file_path.parents):
        if (current / "pom.xml").is_file():
            return current
        if current == repo_root:
            break
    return repo_root


def _infer_module_and_selector(repo_root: Path, selector: str, forced_module: str | None) -> tuple[str, str]:
    class_part, method_part = _split_test_selector(selector)
    module_dir: Path | None = None
    if forced_module is not None:
        module_dir = _resolve_module_dir(repo_root, forced_module)
        if module_dir is None:
            raise SystemExit(f"Module not found (expected a path containing pom.xml): {forced_module}")

    test_file = _is_existing_file_path(repo_root, class_part)
    if test_file is None and (class_part.endswith(".java") or class_part.endswith(".kt")):
        raise SystemExit(f"Test file not found: {class_part}")

    if test_file is None:
        matches = _find_test_files(module_dir or repo_root, class_part)
    else:
        matches = [test_file]

    if module_dir is not None:
        matches = [m for m in matches if str(m).startswith(str(module_dir.resolve()) + os.sep)]

    if not matches:
        raise SystemExit(
            f"Could not locate test source for '{class_part}'. "
            "Pass a module path (e.g. core/sail/shacl) or use --module to disambiguate.",
        )

    if len(matches) > 1:
        details: list[str] = []
        for match in matches:
            module_dir = _find_nearest_module_dir(repo_root, match)
            module_rel = module_dir.relative_to(repo_root).as_posix()
            match_rel = match.relative_to(repo_root).as_posix()
            details.append(f"- {module_rel}: {match_rel}")
        detail_text = "\n".join(details)
        raise SystemExit(
            f"Test class '{class_part}' exists in multiple modules:\n{detail_text}\n"
            "Re-run with --module <path> to pick the right one.",
        )

    match = matches[0]
    module_dir = _find_nearest_module_dir(repo_root, match)
    module = module_dir.relative_to(repo_root).as_posix()

    class_name = class_part
    if test_file is not None:
        class_name = match.stem

    final_selector = class_name if method_part is None else f"{class_name}#{method_part}"
    return module, final_selector


def _log_dir(repo_root: Path) -> Path:
    log_dir = repo_root / "logs" / "mvnf"
    log_dir.mkdir(parents=True, exist_ok=True)
    return log_dir


def _run(
    cmd: list[str],
    cwd: Path,
    tail_lines: int,
    log_path: Path,
    stream: bool,
    stream_filter: Callable[[str], bool] | None = None,
    tail_on_success: bool = False,
    registration: RunRegistration | None = None,
) -> tuple[int, list[str]]:
    print(f"\n$ {_quote_cmd(cmd)}")
    print(f"[mvnf] Log: {log_path.relative_to(cwd).as_posix()}")
    sys.stdout.flush()

    output_tail: deque[str] = deque(maxlen=tail_lines)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    returncode = 0
    with log_path.open("w", encoding="utf-8", errors="replace") as log_file:
        if registration is None:
            raise RuntimeError("A registered Maven run is required before starting a child process")
        with tracked_maven_process(
            registration,
            cmd,
            cwd=str(cwd),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        ) as proc:
            assert proc.stdout is not None
            for line in proc.stdout:
                log_file.write(line)
                output_tail.append(line.rstrip("\n"))
                if stream and (stream_filter is None or stream_filter(line)):
                    print(line, end="")
            returncode = proc.wait()

    tail = list(output_tail)
    if not stream and tail and (tail_on_success or returncode != 0):
        print("[mvnf] Output tail:")
        print("\n".join(tail))

    return returncode, tail


def _print_install_success(repo_root: Path, log_path: Path) -> None:
    elapsed = ""
    try:
        with log_path.open("r", encoding="utf-8", errors="replace") as log_file:
            for line in log_file:
                if "Total time:" in line:
                    elapsed = " " + line.split("Total time:", 1)[1].strip()
    except OSError:
        pass

    print(f"\n[mvnf] Root install passed: BUILD SUCCESS{elapsed} ({log_path.relative_to(repo_root).as_posix()})")


def _delete_logs(log_paths: list[Path]) -> None:
    for log_path in log_paths:
        try:
            log_path.unlink(missing_ok=True)
        except OSError as exc:
            print(f"[mvnf] Warning: could not delete log {log_path}: {exc}")


def _delete_module_test_artifacts(
    repo_root: Path,
    module: str,
    build_directory: Path | None = None,
    workspace_build_root: Path | None = None,
) -> bool:
    module_dir = (repo_root / module).resolve()
    target_dir = module_dir / "target" if build_directory is None else build_directory
    if workspace_build_root is not None:
        validate_workspace_owned_path(target_dir, workspace_build_root)
    if target_dir.is_symlink():
        raise RuntimeError(f"Refusing to delete through symlinked module target: {target_dir}")

    artifact_paths = [
        target_dir / "surefire-reports",
        target_dir / "failsafe-reports",
        target_dir / "surefire",
        target_dir / "failsafe",
    ]

    deleted = False
    for artifact_path in artifact_paths:
        try:
            artifact_path.relative_to(target_dir)
        except ValueError as exc:
            raise RuntimeError(f"Refusing to delete outside module target: {artifact_path}") from exc

        if artifact_path.is_symlink() or artifact_path.is_file():
            artifact_path.unlink()
            deleted = True
        elif artifact_path.is_dir():
            shutil.rmtree(artifact_path)
            deleted = True

    return deleted


def _list_report_files(repo_root: Path, module: str, build_directory: Path | None = None) -> list[Path]:
    module_dir = (repo_root / module).resolve()
    target_dir = module_dir / "target" if build_directory is None else build_directory
    report_dirs = [
        target_dir / "surefire-reports",
        target_dir / "failsafe-reports",
    ]

    files: list[Path] = []
    for report_dir in report_dirs:
        if not report_dir.is_dir():
            continue
        files.extend(sorted(report_dir.glob("*.txt")))
        files.extend(sorted(report_dir.glob("*.xml")))

    unique = sorted({f.resolve() for f in files})
    return unique


@dataclass
class _ReportSummary:
    tests: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0
    time: float = 0.0
    parsed_reports: list[Path] = field(default_factory=list)
    parse_warnings: list[str] = field(default_factory=list)
    problem_lines: list[str] = field(default_factory=list)


def _int_attr(element: ET.Element, name: str) -> int:
    value = element.attrib.get(name)
    if value is None:
        return 0
    try:
        return int(value)
    except ValueError:
        return 0


def _float_attr(element: ET.Element, name: str) -> float:
    value = element.attrib.get(name)
    if value is None:
        return 0.0
    try:
        return float(value)
    except ValueError:
        return 0.0


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _clip(value: str, limit: int = 220) -> str:
    compact = " ".join(value.split())
    if len(compact) <= limit:
        return compact
    return compact[: limit - 3] + "..."


def _test_suites(root: ET.Element) -> list[ET.Element]:
    if _local_name(root.tag) == "testsuite":
        return [root]
    return [element for element in root.iter() if _local_name(element.tag) == "testsuite"]


def _parse_xml_report(report: Path, summary: _ReportSummary) -> bool:
    try:
        root = ET.parse(report).getroot()
    except (ET.ParseError, OSError) as exc:
        summary.parse_warnings.append(f"{report.name}: could not parse XML report ({exc})")
        return False

    suites = _test_suites(root)
    if not suites:
        summary.parse_warnings.append(f"{report.name}: no testsuite elements found")
        return False

    summary.parsed_reports.append(report)
    for suite in suites:
        summary.tests += _int_attr(suite, "tests")
        summary.failures += _int_attr(suite, "failures")
        summary.errors += _int_attr(suite, "errors")
        summary.skipped += _int_attr(suite, "skipped")
        summary.time += _float_attr(suite, "time")

        for testcase in suite.iter():
            if _local_name(testcase.tag) != "testcase":
                continue
            classname = testcase.attrib.get("classname") or suite.attrib.get("name") or "<unknown>"
            name = testcase.attrib.get("name") or "<unknown>"
            for child in testcase:
                child_name = _local_name(child.tag)
                if child_name not in {"failure", "error"}:
                    continue
                message = child.attrib.get("message") or child.text or ""
                summary.problem_lines.append(
                    f"{classname}.{name}: {child_name}: {_clip(message)} [{report.name}]"
                )

    return True


def _summarize_reports(repo_root: Path, module: str, build_directory: Path | None = None) -> _ReportSummary:
    summary = _ReportSummary()
    for report in _list_report_files(repo_root, module, build_directory):
        if report.suffix == ".xml":
            _parse_xml_report(report, summary)
        elif report.suffix == ".txt":
            summary.parsed_reports.append(report)
    return summary


def _format_report_totals(summary: _ReportSummary) -> str:
    return (
        f"tests={summary.tests}, failures={summary.failures}, errors={summary.errors}, "
        f"skipped={summary.skipped}, time={summary.time:.3f}s"
    )


def _print_report_summary(
    repo_root: Path,
    module: str,
    include_failures: bool,
    build_directory: Path | None = None,
) -> None:
    reports = _list_report_files(repo_root, module, build_directory)
    if not reports:
        print("\n[mvnf] No surefire/failsafe reports found for this module.")
        return

    summary = _summarize_reports(repo_root, module, build_directory)
    print("\n[mvnf] Reports:")
    if summary.tests or summary.parsed_reports:
        print(f"[mvnf] Summary: {_format_report_totals(summary)}")
    for report in reports:
        print(f"- {report.relative_to(repo_root).as_posix()}")

    if include_failures and summary.problem_lines:
        print("\n[mvnf] Top failures/errors:")
        for line in summary.problem_lines[:10]:
            print(f"- {line}")

    if summary.parse_warnings:
        print("\n[mvnf] Report parse warnings:")
        for warning in summary.parse_warnings[:5]:
            print(f"- {warning}")


def main() -> int:
    argv = sys.argv[1:]
    passthrough_args: list[str] = []
    if "--" in argv:
        separator = argv.index("--")
        passthrough_args = argv[separator + 1 :]
        argv = argv[:separator]

    parser = argparse.ArgumentParser(
        prog="mvnf",
        description="Delete stale module test artifacts, install everything (quick), then run module verify or a single test.",
    )
    parser.add_argument(
        "target",
        help="Module path (e.g. core/sail/shacl) OR test class[#method] (e.g. ShaclSailTest#testX).",
    )
    parser.add_argument(
        "--module",
        help="Force the module path when target is a test class/method (useful if duplicates exist).",
    )
    parser.add_argument("--it", action="store_true", help="Run via Failsafe (-Dit.test) instead of Surefire (-Dtest).")
    parser.add_argument("--no-offline", action="store_true", help="Disable Maven offline mode (-o).")
    parser.add_argument("--stream", action="store_true", help="Stream full Maven output to stdout (can be very long).")
    parser.add_argument(
        "--tail-on-success",
        action="store_true",
        help="Print retained Maven output tails even when a phase succeeds (old verbose behavior).",
    )
    parser.add_argument(
        "--retain-logs",
        action="store_true",
        help="Keep install/verify logs even when tests pass.",
    )
    parser.add_argument("--tail", type=int, default=200, help="Keep the last N Maven output lines for failures.")
    parser.add_argument("--mvn", help="Override the Maven command (default: mvn or ./mvnw).")
    parser.add_argument(
        "--workspace",
        help="Use an isolated named Maven output workspace (or set MVNF_WORKSPACE).",
    )
    parser.add_argument(
        "--threads",
        help="Positive Maven reactor thread count in workspace mode (default: 1).",
    )
    parser.add_argument(
        "--allow-concurrent",
        action="store_true",
        help="Deprecated: use --workspace; this flag never bypasses workspace ownership.",
    )
    args = parser.parse_args(argv)

    try:
        repo_root = _find_repo_root()
        workspace_id = resolve_workspace(args.workspace, os.environ)
        if args.tail < 0:
            raise MavenWorkspaceError("--tail must be a non-negative integer")
        if args.allow_concurrent and workspace_id is None:
            raise MavenWorkspaceError(
                "--allow-concurrent no longer bypasses shared Maven outputs; use --workspace <id>"
            )
        if args.threads is not None and workspace_id is None:
            raise MavenWorkspaceError("--threads is available only with --workspace")
        if workspace_id is not None:
            validate_threads(args.threads)
            validate_forwarded_arguments(passthrough_args, workspace_mode=True, test_runner=True)
            validate_inherited_maven_arguments(os.environ, test_runner=True)
            validate_project_maven_config(repo_root, test_runner=True)
            if args.allow_concurrent:
                print(
                    "[mvnf] Warning: --allow-concurrent is deprecated; the named workspace lock remains enforced."
                )

        mvn_cmd = shlex.split(args.mvn) if args.mvn else _default_maven_cmd(repo_root)
        if workspace_id is not None:
            validate_forwarded_arguments(mvn_cmd[1:], workspace_mode=True, test_runner=True)
        offline_flag = [] if args.no_offline else ["-o"]
        module_dir = _resolve_module_dir(repo_root, args.target.strip())
        if args.module is None and module_dir is not None:
            module = module_dir.relative_to(repo_root).as_posix()
            test_selector = None
        else:
            module, test_selector = _infer_module_and_selector(repo_root, args.target.strip(), args.module)

        workspace_paths: WorkspacePaths | None = None
        module_build_directory: Path | None = None
        if workspace_id is not None:
            workspace_paths = create_workspace_paths(repo_root, workspace_id)
            module_gav = load_project_gav(repo_root / module / "pom.xml")
            module_build_directory = project_build_directory(workspace_paths, module_gav)
            isolation_flags = isolation_arguments(repo_root, workspace_paths, args.threads)
            install_flags = isolation_flags
            verify_common_flags = offline_flag + isolation_flags
            log_paths = [
                workspace_paths.log_dir / "install.log",
                workspace_paths.log_dir / "verify.log",
            ]
        else:
            install_flags = ["-T", "1C", "-Dmaven.repo.local=.m2_repo"]
            verify_common_flags = offline_flag + ["-Dmaven.repo.local=.m2_repo"]
            run_id = datetime.datetime.now(datetime.UTC).strftime("%Y%m%d-%H%M%S")
            log_paths = [repo_root / "maven-build.log", _log_dir(repo_root) / f"{run_id}-verify.log"]

        print(f"Repo root: {repo_root}")
        print(f"Module: {module}")
        if workspace_id is not None:
            print(f"Workspace: {workspace_id} ({workspace_paths.root})")
        if test_selector is not None:
            print(f"Test selector: {test_selector} ({'failsafe' if args.it else 'surefire'})")

        install_cmd = mvn_cmd + [
            "-B",
            "-ntp",
            "-Dmaven.compiler.showWarnings=false",
        ] + install_flags + offline_flag + ["-Pquick", "install"]
        verify_cmd = mvn_cmd + verify_common_flags + ["-pl", module]
        if test_selector is not None:
            if args.it:
                verify_cmd.extend(["-PskipUnitTests", f"-Dit.test={test_selector}"])
            else:
                verify_cmd.extend(["-DskipITs", f"-Dtest={test_selector}"])
        verify_cmd.extend(passthrough_args)
        verify_cmd.append("verify")

        with registered_run(repo_root, workspace_id, sys.argv) as registration:
            if workspace_paths is not None:
                validate_reused_workspace_trees(workspace_paths)
                version_log = workspace_paths.log_dir / "maven-version.log"
                version_rc, _ = _run(
                    mvn_cmd + ["--version"],
                    repo_root,
                    args.tail,
                    version_log,
                    False,
                    registration=registration,
                )
                if version_rc != 0:
                    raise MavenWorkspaceError(f"Maven version probe failed with exit code {version_rc}")
                try:
                    version_output = version_log.read_text(encoding="utf-8", errors="replace")
                except OSError as exc:
                    raise MavenWorkspaceError(f"Could not read Maven version log {version_log}: {exc}") from exc
                validate_maven_version(version_output)
                prepare_reactor_tmp_directories(repo_root, workspace_paths)
                write_run_metadata(
                    workspace_paths,
                    runner="mvnf",
                    command=sys.argv,
                    module=module,
                )

            if _delete_module_test_artifacts(
                repo_root,
                module,
                module_build_directory,
                None if workspace_paths is None else workspace_paths.build_root,
            ):
                print("\n[mvnf] Deleted stale module test artifacts.")
            else:
                print("\n[mvnf] No stale module test artifacts found.")

            rc, _ = _run(
                install_cmd,
                repo_root,
                args.tail,
                log_paths[0],
                args.stream,
                _maven_build_stream_filter(),
                args.tail_on_success,
                registration,
            )
            if rc != 0:
                print("\n[mvnf] Root install failed.")
                return rc
            _print_install_success(repo_root, log_paths[0])

            rc, _ = _run(
                verify_cmd,
                repo_root,
                args.tail,
                log_paths[1],
                args.stream,
                tail_on_success=args.tail_on_success,
                registration=registration,
            )
            if rc == 0:
                print("\n[mvnf] Tests passed.")
                _print_report_summary(
                    repo_root,
                    module,
                    include_failures=False,
                    build_directory=module_build_directory,
                )
                if workspace_paths is None and not args.retain_logs:
                    _delete_logs([log_paths[1]])
                return 0

            print("\n[mvnf] Tests failed.")
            _print_report_summary(
                repo_root,
                module,
                include_failures=True,
                build_directory=module_build_directory,
            )
            return rc
    except MavenWorkspaceInterrupted as exc:
        print(f"[mvnf] Interrupted by signal {exc.signum}; no subsequent Maven phase will start")
        return 128 + exc.signum
    except MavenWorkspaceError as exc:
        print(f"[mvnf] Error: {exc}")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
