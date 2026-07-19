#!/usr/bin/env python3

from __future__ import annotations

import argparse
import os
import shlex
import subprocess
import sys
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
WORKSPACE_SCRIPT_DIRECTORY = REPOSITORY_ROOT / ".codex" / "skills" / "mvnf" / "scripts"
sys.path.insert(0, str(WORKSPACE_SCRIPT_DIRECTORY))

from maven_workspace import (  # noqa: E402
    MavenWorkspaceError,
    MavenWorkspaceInterrupted,
    RunRegistration,
    create_workspace_paths,
    default_maven_command,
    find_repo_root,
    isolation_arguments,
    prepare_reactor_tmp_directories,
    quoted_command,
    registered_run,
    resolve_workspace,
    tracked_maven_process,
    validate_forwarded_arguments,
    validate_inherited_maven_arguments,
    validate_maven_version,
    validate_project_maven_config,
    validate_reused_workspace_trees,
    validate_threads,
    write_run_metadata,
)


def run_logged(
    command: list[str],
    repo_root: Path,
    log_path: Path,
    registration: RunRegistration,
    *,
    capture_output: bool = False,
) -> tuple[int, str]:
    print(f"\n$ {quoted_command(command)}")
    print(f"[mvn-agent] Log: {log_path.relative_to(repo_root).as_posix()}")
    log_path.parent.mkdir(parents=True, exist_ok=True)
    output_parts: list[str] | None = [] if capture_output else None
    with log_path.open("w", encoding="utf-8", errors="replace") as log_file:
        with tracked_maven_process(
            registration,
            command,
            cwd=repo_root,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        ) as process:
            assert process.stdout is not None
            for line in process.stdout:
                log_file.write(line)
                if output_parts is not None:
                    output_parts.append(line)
                print(line, end="")
            return_code = process.wait()
    return return_code, "" if output_parts is None else "".join(output_parts)


def main() -> int:
    raw_arguments = sys.argv[1:]
    if "--" not in raw_arguments:
        print("[mvn-agent] Error: a literal `--` must separate runner options from Maven arguments")
        return 2
    separator = raw_arguments.index("--")
    runner_arguments = raw_arguments[:separator]
    maven_arguments = raw_arguments[separator + 1 :]

    parser = argparse.ArgumentParser(
        prog="mvn-agent",
        description="Run a Maven lifecycle command in a cooperative isolated agent workspace.",
    )
    parser.add_argument("--workspace", help="Required workspace ID (or set MVNF_WORKSPACE).")
    parser.add_argument("--threads", help="Positive Maven reactor thread count (default: 1).")
    parser.add_argument("--mvn", help="Override the Maven command (default: mvn or ./mvnw).")
    args = parser.parse_args(runner_arguments)

    try:
        workspace_id = resolve_workspace(args.workspace, os.environ)
        if workspace_id is None:
            raise MavenWorkspaceError("mvn-agent requires --workspace <id> or MVNF_WORKSPACE")
        if not maven_arguments:
            raise MavenWorkspaceError("No Maven arguments followed the required `--` delimiter")
        validate_threads(args.threads)
        validate_forwarded_arguments(maven_arguments, workspace_mode=True, allow_project_selection=True)
        validate_inherited_maven_arguments(os.environ, allow_project_selection=True)

        try:
            repo_root = find_repo_root(Path.cwd())
        except MavenWorkspaceError:
            repo_root = find_repo_root(Path(__file__).resolve())
        validate_project_maven_config(repo_root, allow_project_selection=True)
        maven_command = shlex.split(args.mvn) if args.mvn else default_maven_command(repo_root)
        validate_forwarded_arguments(maven_command[1:], workspace_mode=True, allow_project_selection=True)
        paths = create_workspace_paths(repo_root, workspace_id)
        build_command = maven_command + isolation_arguments(repo_root, paths, args.threads) + maven_arguments

        print(f"Repo root: {repo_root}")
        print(f"Workspace: {workspace_id} ({paths.root})")
        with registered_run(repo_root, workspace_id, sys.argv) as registration:
            validate_reused_workspace_trees(paths)
            version_code, version_output = run_logged(
                maven_command + ["--version"],
                repo_root,
                paths.log_dir / "maven-version.log",
                registration,
                capture_output=True,
            )
            if version_code != 0:
                raise MavenWorkspaceError(f"Maven version probe failed with exit code {version_code}")
            validate_maven_version(version_output)
            prepare_reactor_tmp_directories(repo_root, paths)
            write_run_metadata(paths, runner="mvn-agent", command=sys.argv)
            return_code, _ = run_logged(
                build_command, repo_root, paths.log_dir / "maven.log", registration
            )
            return return_code
    except MavenWorkspaceInterrupted as exc:
        print(f"[mvn-agent] Interrupted by signal {exc.signum}")
        return 128 + exc.signum
    except MavenWorkspaceError as exc:
        print(f"[mvn-agent] Error: {exc}")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
