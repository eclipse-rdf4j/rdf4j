---
name: debug-surefire
description: Debug Maven Surefire unit tests by running them in JDWP "wait for debugger" mode (`-Dmaven.surefire.debug`) and attaching IntelliJ/VS Code/jdb. Use when asked to debug/step through a failing JUnit test, attach a debugger to a Maven test run, or run `mvn test -Dtest=Class[#method]` suspended on a port (including multi-module `-pl` runs).
---

# debug-surefire

Run Maven Surefire tests suspended in JDWP so you can attach a debugger and step through the code.

## Quick start

- Debug a test class:
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test-class MyTest`
- Debug a single test method (quote the `#`):
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test 'MyTest#shouldDoThing'`
- Debug a test in a specific module:
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --module core/sail/shacl --test-class ShaclSailTest`
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --module rdf4j-sail-shacl --test 'ShaclSailTest#testSomething'`

## Notes

- The script runs a fast pre-test install (`-Pquick` into the repo-local `.m2_repo`) and then runs `mvn test` with Surefire in debug mode.
- Use `SUREFIRE_DEBUG_PORT=8000` to change the port (default: `55005`).
- Use `--no-offline` / `--online` if offline (`-o`) resolution fails.
- Everything after `--` is passed to Maven, e.g.:
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test-class MyTest -- -DtrimStackTrace=false -DfailIfNoTests=false -DforkCount=1 -DreuseForks=false`
