---
name: mvnf
description: Run Maven tests in this repo with a consistent workflow, optionally using an isolated named Maven workspace for cooperative concurrency. Use when asked to run tests/verify in the rdf4j multi-module build or when the user says mvnf.
---

# mvnf

Run Maven tests with repeatable commands and useful failure pointers.

## Quick start

- Run a module's full test suite:
  - `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/shacl`
- Run a unit test class or method (module auto-detected):
  - `python3 .codex/skills/mvnf/scripts/mvnf.py ShaclSailTest`
  - `python3 .codex/skills/mvnf/scripts/mvnf.py ShaclSailTest#testSomething`
- Run an integration test (Failsafe):
  - `python3 .codex/skills/mvnf/scripts/mvnf.py --it ShaclSailIT#testSomething`
- Run a focused test in an isolated named workspace:
  - `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace shacl-a ShaclSailTest#testSomething`
- Run an isolated lifecycle build:
  - `python3 scripts/mvn-agent.py --workspace shacl-a -- -B -ntp -o -Pquick clean install`

## What it does

Without `--workspace`, `mvnf` preserves the legacy workflow:

1. Deletes stale `target/surefire*` and `target/failsafe*` artifacts for the selected module.
2. `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick install`
3. `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify` (optionally with `-DskipITs -Dtest=...` for unit tests or `-PskipUnitTests -Dit.test=...` for Failsafe ITs)

In legacy mode, root install output is stored in `maven-build.log`. On success, `mvnf` prints one root-install summary line with `BUILD SUCCESS` and the log path. Verify logs use `logs/mvnf/*-verify.log` when retained or when tests fail. By default, successful Maven phases do not print their output tail; `mvnf` prints compact Surefire/Failsafe report totals and report paths instead. Failed phases still print the retained Maven tail plus compact report failure details.

## Isolated workspace mode

Workspace mode is opt-in through `--workspace <id>` or `MVNF_WORKSPACE=<id>`; an explicit CLI value takes precedence
over the environment. It requires Maven 3.9.10 or newer. Different workspace IDs may run concurrently, while the same
ID remains exclusive. A legacy workspace-less run conflicts with every live cooperative workspace run.

Workspace Maven reactors default to one thread. Use runner option `--threads <positive-integer>` to override that
default; do not forward Maven `-T`. Both the root quick install and the selected verify use the same isolation contract:

- Shared read-through repository tail: `.m2_repo/`
- Private writable repository head: `.mvnf/workspaces/<id>/repository/`
- Full-GAV build root: `.mvnf/workspaces/<id>/build/<groupId>/<artifactId>/<version>/`
- Surefire/Failsafe reports: the matching full-GAV build root's `surefire-reports/` and `failsafe-reports/`
- Per-run logs and metadata: `.mvnf/workspaces/<id>/logs/<run-id>/`
- Per-run, per-GAV temporary data: `.mvnf/workspaces/<id>/tmp/<run-id>/<groupId>/<artifactId>/<version>/`

For ordinary lifecycle work, use `scripts/mvn-agent.py --workspace <id> -- <Maven arguments>`. The wrapper validates
the forwarded command and applies the same repository, build, temp, log, and lock rules as `mvnf`.

For serialized manual root clean installs, keep full output in `maven-build.log` and print only errors plus the reactor summary:

```bash
mvn -B -ntp \
  -Dmaven.compiler.showWarnings=false \
  -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
  | tee maven-build.log \
  | awk '
     /\[WARNING\]/ { next }
      /\[ERROR\]/ { print; next }

      /Reactor Summary/ { summary=1 }
      summary { print }
    '
```

For install without clean, replace `clean install` with `install`. For a module install, add `-pl <module> -am` before `-Pquick`. Keep the `2>&1 | tee maven-build.log | awk ...` tail on every install variant.

If a legacy test run fails, it prints the list of Surefire/Failsafe report files under the module's
`target/*-reports/` directories. For a handoff-ready block from retained reports, run
`python3 scripts/agent-evidence.py <module>/target/surefire-reports <module>/target/failsafe-reports`.

For workspace `<id>`, persist compact evidence as top-level `initial-evidence.<id>.txt`. Use the exact
`.mvnf/workspaces/<id>/logs/<run-id>/verify.log` and full-GAV report directories printed by `mvnf`; do not read or
summarize legacy `target/` reports or another workspace's reports.

## Options

- `--module <path>`: Force the module when the test class name exists in multiple modules.
- `--it`: Treat the selector as an integration test and pass it via `-Dit.test=...`.
- `--no-offline`: Run Maven commands without `-o` (useful if offline resolution fails).
- `--stream`: Stream verify output live for hang/no-progress debugging; install output is filtered to errors plus the reactor summary while the full install log is kept in `maven-build.log`.
- `--tail-on-success`: Print Maven output tails on successful phases too (old verbose behavior).
- `--retain-logs`: Keep verify logs on success.
- `--workspace <id>`: Use a validated named workspace. If absent, `MVNF_WORKSPACE` is consulted.
- `--threads <count>`: Set a positive workspace reactor thread count; workspace default is `1`.
- `--allow-concurrent`: Deprecated. Without a workspace it fails and directs the caller to `--workspace`; with a
  workspace it only prints a warning and never bypasses workspace ownership.
- `-- <maven args>`: Append extra Maven flags/profiles to the verify command while preserving selector behavior.

## Concurrency and cleanup limits

- Raw `mvn` does not participate in the cooperative registry under `.mvnf/runs/`. Never overlap raw Maven with
  workspace commands; use `scripts/mvn-agent.py` for isolated lifecycle builds.
- Workspace mode isolates Maven outputs, not the source checkout. Use separate Git worktrees when concurrent edits
  need stable source snapshots.
- Formatting, release, deploy, and source-generator operations remain serialized outside workspace mode because they
  mutate shared source or release state. The workspace runners reject these routes and owned-path overrides.
- Workspace mode accepts Maven lifecycle phases through `install`, not direct plugin goals. Even apparently read-only
  `help:*` or `dependency:*` invocations remain serialized outside workspace mode because plugins can perform
  arbitrary writes. Explicit arguments, inherited `MAVEN_ARGS`, and `.mvn/maven.config` are all validated before
  Maven starts.
- Workspace repositories, build trees, logs, and temporary data are not automatically deleted. Reusing an ID reuses
  its repository/build roots and creates new run-specific log/tmp directories. `mvnf` only clears stale test-report
  directories for the selected GAV before its verify.

## LMDB regression speedup note

For LMDB theme regression/snapshot tests, enable persistent prepared stores to skip repeated dataset rebuilds:

- `-Drdf4j.lmdb.themeRegression.persistentStore.enabled=true`
- Optional root override: `-Drdf4j.lmdb.themeRegression.persistentStore.root=persistent-lmdb-theme-store`

Pass extra Maven flags after `--`, for example:

- `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbThemeQueryRegressionTest#socialMediaFiveCycleInterleavesValuesWithFollowsEdges --module core/sail/lmdb -- -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true`
