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


def _find_git_root(start: Path) -> Path | None:
    for current in [start] + list(start.parents):
        if (current / ".git").exists():
            return current
    return None


def _find_repo_root() -> Path:
    candidates = [Path.cwd(), Path(__file__).resolve()]

    for candidate in candidates:
        start = candidate if candidate.is_dir() else candidate.parent

        git_root = _find_git_root(start)
        if git_root is not None and (git_root / "pom.xml").is_file():
            return git_root

        pom_roots: list[Path] = []
        for current in [start] + list(start.parents):
            if (current / "pom.xml").is_file():
                pom_roots.append(current)
        if pom_roots:
            return pom_roots[-1]

    raise SystemExit("Could not locate a Maven repo root (no pom.xml found in parent dirs).")


def _default_maven_cmd(repo_root: Path) -> list[str]:
    mvnw = repo_root / "mvnw"
    if mvnw.is_file():
        if os.access(mvnw, os.X_OK):
            return [str(mvnw)]
        return ["sh", str(mvnw)]
    return ["mvn"]


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


def _find_test_files(repo_root: Path, class_name: str) -> list[Path]:
    simple = class_name.split(".")[-1]

    patterns = [
        f"**/src/test/java/**/{simple}.java",
        f"**/src/test/java/**/{simple}.kt",
        f"**/src/test/**/{simple}.java",
        f"**/src/test/**/{simple}.kt",
    ]

    matches: list[Path] = []
    for pattern in patterns:
        matches.extend(repo_root.glob(pattern))

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

    test_file = _is_existing_file_path(repo_root, class_part)
    if test_file is None and (class_part.endswith(".java") or class_part.endswith(".kt")):
        raise SystemExit(f"Test file not found: {class_part}")

    if test_file is None:
        matches = _find_test_files(repo_root, class_part)
    else:
        matches = [test_file]

    if forced_module is not None:
        module_dir = _resolve_module_dir(repo_root, forced_module)
        if module_dir is None:
            raise SystemExit(f"Module not found (expected a path containing pom.xml): {forced_module}")
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


@dataclass(frozen=True)
class _ActiveMvnfRun:
    pid: int
    marker: Path
    started_at: str


def _run_registry_dir(repo_root: Path) -> Path:
    return repo_root / "target" / "mvnf-runs"


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


def _cleanup_mvnf_run(marker: Path) -> None:
    try:
        shutil.rmtree(marker)
    except FileNotFoundError:
        pass
    except OSError as exc:
        print(f"[mvnf] Warning: could not remove run marker {marker}: {exc}")


def _active_mvnf_runs(repo_root: Path, current_pid: int) -> list[_ActiveMvnfRun]:
    registry_dir = _run_registry_dir(repo_root)
    if not registry_dir.is_dir():
        return []

    active: list[_ActiveMvnfRun] = []
    for marker in sorted(registry_dir.iterdir()):
        if not marker.is_dir():
            continue
        try:
            pid = int(marker.name)
        except ValueError:
            continue
        if pid == current_pid:
            continue
        if not _pid_is_running(pid):
            _cleanup_mvnf_run(marker)
            continue
        try:
            started_at = (marker / "started-at").read_text(encoding="utf-8").strip()
        except OSError:
            started_at = "unknown"
        active.append(_ActiveMvnfRun(pid=pid, marker=marker, started_at=started_at))
    return active


def _create_mvnf_run_marker(repo_root: Path) -> Path:
    registry_dir = _run_registry_dir(repo_root)
    registry_dir.mkdir(parents=True, exist_ok=True)
    marker = registry_dir / str(os.getpid())
    if marker.exists():
        _cleanup_mvnf_run(marker)
    marker.mkdir()
    (marker / "started-at").write_text(datetime.datetime.now(datetime.UTC).isoformat(), encoding="utf-8")
    (marker / "argv").write_text(_quote_cmd(sys.argv), encoding="utf-8")
    return marker


def _register_mvnf_run(repo_root: Path, allow_concurrent: bool) -> Path | None:
    marker = _create_mvnf_run_marker(repo_root)
    active_runs = _active_mvnf_runs(repo_root, os.getpid())
    if not active_runs:
        return marker

    if allow_concurrent:
        print("[mvnf] Warning: another mvnf.py process appears to be running in this repo.")
        for active in active_runs:
            print(
                f"[mvnf] Active run: pid={active.pid}, started={active.started_at}, "
                f"marker={active.marker.relative_to(repo_root).as_posix()}"
            )
        print("[mvnf] Continuing because --allow-concurrent was supplied.")
        return marker

    print("[mvnf] Error: another mvnf.py process appears to be running in this repo.")
    for active in active_runs:
        print(
            f"[mvnf] Active run: pid={active.pid}, started={active.started_at}, "
            f"marker={active.marker.relative_to(repo_root).as_posix()}"
        )
    print(
        "[mvnf] This PID-based check can false-positive after PID reuse or an unclean exit; "
        "re-run with --allow-concurrent after verifying it is safe."
    )
    _cleanup_mvnf_run(marker)
    return None


def _run(
    cmd: list[str],
    cwd: Path,
    tail_lines: int,
    log_path: Path,
    stream: bool,
    stream_filter: Callable[[str], bool] | None = None,
    tail_on_success: bool = False,
) -> tuple[int, list[str]]:
    print(f"\n$ {_quote_cmd(cmd)}")
    print(f"[mvnf] Log: {log_path.relative_to(cwd).as_posix()}")
    sys.stdout.flush()

    output_tail: deque[str] = deque(maxlen=tail_lines)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    returncode = 0
    with log_path.open("w", encoding="utf-8", errors="replace") as log_file:
        with subprocess.Popen(
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
        for line in reversed(log_path.read_text(encoding="utf-8", errors="replace").splitlines()):
            if "Total time:" in line:
                elapsed = " " + line.split("Total time:", 1)[1].strip()
                break
    except OSError:
        pass

    print(f"\n[mvnf] Root install passed: BUILD SUCCESS{elapsed} ({log_path.relative_to(repo_root).as_posix()})")


def _delete_logs(log_paths: list[Path]) -> None:
    for log_path in log_paths:
        try:
            log_path.unlink(missing_ok=True)
        except OSError as exc:
            print(f"[mvnf] Warning: could not delete log {log_path}: {exc}")


def _delete_module_test_artifacts(repo_root: Path, module: str) -> bool:
    module_dir = (repo_root / module).resolve()
    target_dir = module_dir / "target"
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


def _list_report_files(repo_root: Path, module: str) -> list[Path]:
    module_dir = (repo_root / module).resolve()
    report_dirs = [
        module_dir / "target" / "surefire-reports",
        module_dir / "target" / "failsafe-reports",
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


def _summarize_reports(repo_root: Path, module: str) -> _ReportSummary:
    summary = _ReportSummary()
    for report in _list_report_files(repo_root, module):
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


def _print_report_summary(repo_root: Path, module: str, include_failures: bool) -> None:
    reports = _list_report_files(repo_root, module)
    if not reports:
        print("\n[mvnf] No surefire/failsafe reports found for this module.")
        return

    summary = _summarize_reports(repo_root, module)
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
        "--allow-concurrent",
        action="store_true",
        help="Allow this mvnf run even if another mvnf.py process appears active in the same repo.",
    )
    args = parser.parse_args(argv)

    repo_root = _find_repo_root()
    mvn_cmd = shlex.split(args.mvn) if args.mvn else _default_maven_cmd(repo_root)

    offline_flag = [] if args.no_offline else ["-o"]
    common_flags = offline_flag + ["-Dmaven.repo.local=.m2_repo"]

    module_dir = _resolve_module_dir(repo_root, args.target.strip())
    if args.module is None and module_dir is not None:
        module = module_dir.relative_to(repo_root).as_posix()
        test_selector = None
    else:
        module, test_selector = _infer_module_and_selector(repo_root, args.target.strip(), args.module)

    print(f"Repo root: {repo_root}")
    print(f"Module: {module}")
    if test_selector is not None:
        print(f"Test selector: {test_selector} ({'failsafe' if args.it else 'surefire'})")

    install_cmd = mvn_cmd + [
        "-B",
        "-ntp",
        "-Dmaven.compiler.showWarnings=false",
        "-T",
        "1C",
    ] + (offline_flag + ["-Dmaven.repo.local=.m2_repo", "-Pquick", "install"])

    verify_cmd = mvn_cmd + common_flags + ["-pl", module]
    if test_selector is not None:
        if args.it:
            verify_cmd.extend(["-PskipUnitTests", f"-Dit.test={test_selector}"])
        else:
            verify_cmd.extend(["-DskipITs", f"-Dtest={test_selector}"])
    verify_cmd.extend(passthrough_args)
    verify_cmd.append("verify")

    run_id = datetime.datetime.now(datetime.UTC).strftime("%Y%m%d-%H%M%S")
    log_dir = _log_dir(repo_root)
    log_paths = [
        repo_root / "maven-build.log",
        log_dir / f"{run_id}-verify.log",
    ]

    run_marker = _register_mvnf_run(repo_root, args.allow_concurrent)
    if run_marker is None:
        return 2

    try:
        if _delete_module_test_artifacts(repo_root, module):
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
        )
        if rc != 0:
            print("\n[mvnf] Root install failed.")
            return rc
        _print_install_success(repo_root, log_paths[0])

        rc, _ = _run(verify_cmd, repo_root, args.tail, log_paths[1], args.stream, tail_on_success=args.tail_on_success)
        if rc == 0:
            print("\n[mvnf] Tests passed.")
            _print_report_summary(repo_root, module, include_failures=False)
            if not args.retain_logs:
                _delete_logs([log_paths[1]])
            return 0

        print("\n[mvnf] Tests failed.")
        _print_report_summary(repo_root, module, include_failures=True)

        return rc
    finally:
        _cleanup_mvnf_run(run_marker)


if __name__ == "__main__":
    raise SystemExit(main())
