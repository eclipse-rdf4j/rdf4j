---
name: mvnf
description: Run Maven tests in this repo with a consistent workflow (module test-artifact cleanup, root -Pquick install to refresh .m2_repo, then module verify or a single test class/method). Use when asked to run tests/verify in the rdf4j multi-module build or when the user says mvnf.
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

## What it does

1. Deletes stale `target/surefire*` and `target/failsafe*` artifacts for the selected module.
2. `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick install`
3. `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify` (optionally with `-DskipITs -Dtest=...` for unit tests or `-PskipUnitTests -Dit.test=...` for Failsafe ITs)

Root install output is stored in `maven-build.log`. Verify logs use `logs/mvnf/*-verify.log` when retained or when tests fail.

For manual root clean installs, use the same install flags and log filter:

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

If the test run fails, it prints the list of Surefire/Failsafe report files under the module's `target/*-reports/` directories.

## Options

- `--module <path>`: Force the module when the test class name exists in multiple modules.
- `--it`: Treat the selector as an integration test and pass it via `-Dit.test=...`.
- `--no-offline`: Run Maven commands without `-o` (useful if offline resolution fails).
- `--stream`: Stream verify output live; install output is filtered to errors plus the reactor summary while the full install log is kept in `maven-build.log`.

## LMDB regression speedup note

For LMDB theme regression/snapshot tests, enable persistent prepared stores to skip repeated dataset rebuilds:

- `-Drdf4j.lmdb.themeRegression.persistentStore.enabled=true`
- Optional root override: `-Drdf4j.lmdb.themeRegression.persistentStore.root=persistent-lmdb-theme-store`

`mvnf.py` does not forward arbitrary `-D` flags today, so use direct Maven for this mode, for example:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbThemeQueryRegressionTest#socialMediaFiveCycleInterleavesValuesWithFollowsEdges -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true test`
