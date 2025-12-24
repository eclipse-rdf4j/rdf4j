---
name: mvnf
description: Run Maven tests in this repo with a consistent workflow (module clean, root -Pquick clean install to refresh .m2_repo, then module verify or a single test class/method). Use when asked to run tests/verify in the rdf4j multi-module build or when the user says mvnf.
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

1. `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> clean`
2. `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`
3. `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify` (optionally with `-Dtest=` / `-Dit.test=`)

If the test run fails, it prints the list of Surefire/Failsafe report files under the module's `target/*-reports/` directories.

## Options

- `--module <path>`: Force the module when the test class name exists in multiple modules.
- `--it`: Treat the selector as an integration test and pass it via `-Dit.test=...`.
- `--no-offline`: Run Maven commands without `-o` (useful if offline resolution fails).
