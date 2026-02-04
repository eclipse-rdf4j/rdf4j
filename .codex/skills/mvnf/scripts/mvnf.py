#!/usr/bin/env python3

from __future__ import annotations

import argparse
import datetime
import os
import shlex
import subprocess
import sys
from collections import deque
from pathlib import Path


def _quote_cmd(cmd: list[str]) -> str:
    return " ".join(shlex.quote(part) for part in cmd)


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


def _run(
    cmd: list[str],
    cwd: Path,
    tail_lines: int,
    log_path: Path,
    stream: bool,
) -> tuple[int, list[str]]:
    print(f"\n$ {_quote_cmd(cmd)}")
    print(f"[mvnf] Log: {log_path.relative_to(cwd).as_posix()}")
    sys.stdout.flush()

    output_tail: deque[str] = deque(maxlen=tail_lines)
    log_path.parent.mkdir(parents=True, exist_ok=True)
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
                if stream:
                    print(line, end="")

    tail = list(output_tail)
    if not stream and tail:
        print("[mvnf] Output tail:")
        print("\n".join(tail))

    return proc.returncode, tail


def _delete_logs(log_paths: list[Path]) -> None:
    for log_path in log_paths:
        try:
            log_path.unlink(missing_ok=True)
        except OSError as exc:
            print(f"[mvnf] Warning: could not delete log {log_path}: {exc}")


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


def main() -> int:
    parser = argparse.ArgumentParser(
        prog="mvnf",
        description="Clean module, install everything (quick), then run module verify or a single test.",
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
        "--retain-logs",
        action="store_true",
        help="Keep clean/install/verify logs even when tests pass.",
    )
    parser.add_argument("--tail", type=int, default=200, help="Keep the last N Maven output lines for failures.")
    parser.add_argument("--mvn", help="Override the Maven command (default: mvn or ./mvnw).")
    args = parser.parse_args()

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

    clean_cmd = mvn_cmd + common_flags + ["-pl", module, "clean"]
    install_cmd = mvn_cmd + (offline_flag + ["-T", "1C", "-Dmaven.repo.local=.m2_repo", "-Pquick", "install"])

    verify_cmd = mvn_cmd + common_flags + ["-pl", module]
    if test_selector is not None:
        verify_cmd.append(f"-Dit.test={test_selector}" if args.it else f"-Dtest={test_selector}")
    verify_cmd.append("verify")

    run_id = datetime.datetime.now(datetime.UTC).strftime("%Y%m%d-%H%M%S")
    log_dir = _log_dir(repo_root)
    log_paths = [
        log_dir / f"{run_id}-clean.log",
        log_dir / f"{run_id}-install.log",
        log_dir / f"{run_id}-verify.log",
    ]

    rc, _ = _run(clean_cmd, repo_root, args.tail, log_paths[0], args.stream)
    if rc != 0:
        print("\n[mvnf] Module clean failed.")
        return rc

    rc, _ = _run(install_cmd, repo_root, args.tail, log_paths[1], args.stream)
    if rc != 0:
        print("\n[mvnf] Root clean install failed.")
        return rc

    rc, _ = _run(verify_cmd, repo_root, args.tail, log_paths[2], args.stream)
    if rc == 0:
        print("\n[mvnf] Tests passed.")
        if not args.retain_logs:
            _delete_logs(log_paths)
        return 0

    print("\n[mvnf] Tests failed.")

    reports = _list_report_files(repo_root, module)
    if reports:
        print("\n[mvnf] Reports:")
        for report in reports:
            print(f"- {report.relative_to(repo_root).as_posix()}")
    else:
        print("\n[mvnf] No surefire/failsafe reports found for this module.")

    return rc


if __name__ == "__main__":
    raise SystemExit(main())
