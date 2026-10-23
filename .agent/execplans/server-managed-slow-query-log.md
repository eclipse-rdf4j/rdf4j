# Confine Server-managed slow-query logs to each repository

This ExecPlan is a living document following `.agent/PLANS.md`. Work is isolated in `/Users/havardottestad/.codex/worktrees/security-slow-query-log/rdf4j` on `GH-0000-server-slow-query-log`, stacked on draft PR #6242 at `b53ad9af140e27292f382263d729c288cd4a8f05`. No issue number was assigned, so `GH-0000` is the repository convention.

## Purpose / Big Picture

An RDF4J Server client can submit a Sail repository configuration containing `sail.slowQueryLogFile`. A slow query later causes the Server process to append query text at that path. The current writer accepts absolute locations and relative `..` segments, so a remotely managed repository can write outside its own data directory. After this change, Server-managed Sail configurations may log only inside that repository's data directory, including through nested Sail delegates. Embedded RDF4J applications keep their existing path semantics. A live Server test should show an escaping configuration denied, while an in-repository log remains usable.

## Progress

- [done] build clean isolated baseline (2026-10-23 07:32Z)
- [done] reproduce managed path escapes red (2026-10-23 07:45Z)
- [done] trace nested runtime file resolution (2026-10-23 07:45Z)
- [done] implement root-cause path confinement (2026-10-23 08:02Z)
- [done] verify focused and module behavior (2026-10-23 08:09Z)
- [in_progress] format review and publish draft
- [todo] monitor exact published head

## Surprises & Discoveries

The Server-managed repository boundary is the `LocalRepositoryManager` config customizer introduced by #6242. It runs for HTTP PUT, config update, SYSTEM commits, and persisted config reload. `BaseSailConfig` parses `sail.slowQueryLogFile` and delegates can nest it. Bundled MemoryStore, NativeStore, and LMDB factories pass the path to `AbstractSail`, whose `writeSlowQueryLogEntry` creates parent directories and calls `Files.writeString` with append. The file path is not currently confined.

Live Server tests established the path gap before production edits: the absolute, `../`, external-symlink, and nested SHACL-to-MemoryStore inputs were all accepted. Normal relative and in-root symlink alias paths wrote through the actual slow-query writer. A cached repository followed a test-created symlink after it was repointed outside and created an external log file (`symlink-swap-red-evidence.txt`). Manager `refresh()` reparsed the persisted alias and still accepted its new external target (`reload-red-evidence.txt`). A Sail wrapper forwards `setDataDir` to its delegate, so the nested MemoryStore uses the same per-repository root.

## Decision Log

- Decision: Stack on #6242 and reuse its manager config hook. Rationale: independent Server handler checks would miss SYSTEM commits or persisted reload and duplicate the policy boundary. Date/Author: 2026-10-23, Codex.
- Decision: Preserve configured relative spelling in persisted RDF and resolve a runtime path separately if needed. Rationale: persisting an absolute Server filesystem path would harm portability and disclose local layout. Date/Author: 2026-10-23, Codex.
- Decision: Resolve existing symlink ancestors before accepting the path and pass the canonical path to the Sail at runtime. Rationale: lexical prefix checks fail on symlinks, and a cached Sail must not reopen a mutable alias after validation. Reparse on manager refresh must resolve the original persisted spelling again. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The initial private Maven root quick install and full Server Spring baseline passed: 251 tests, zero failures or errors (`baseline-evidence.txt`). No production files have changed in this worktree. `initial-evidence.txt` preserves the first live red. `path-variants-red-evidence.txt` has six tests, four expected failures and two positive passes. The focused symlink-swap and reload red reports are retained separately. The next milestone is the root-cause implementation and focused green.

## Context and Orientation

`tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/manager/LocalRepositoryManagerFactoryBean.java` creates the Server-only `LocalRepositoryManager`. `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/ServerManagedRepositoryConfigPolicy.java` visits repository and Sail delegates before persistence and after config reload. `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/config/BaseSailConfig.java` stores the path from RDF config. Bundled Sail factories pass it to `core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/AbstractSail.java`. That class resolves relative paths against the Sail data directory and appends slow-query details. `tools/server/src/test/java/org/eclipse/rdf4j/http/server/TestServer.java` provides a live Jetty Server fixture; `RemoteRepositoryManager` submits repository configs over HTTP. A path escape means the canonical destination leaves the repository-specific directory even if its lexical spelling begins inside it.

## Plan of Work

First run the isolated Maven baseline. Add the smallest live Failsafe test that submits an absolute external `slowQueryLogFile` through Server HTTP and observes that the configuration is accepted when it should be denied, saving the report immediately in `initial-evidence.txt`. Add red variants for `../` traversal, existing symlinks that lead outside, nested Sail delegates, and reload or update paths. Add positive cases for relative paths and in-root aliases, plus a direct embedded Sail case proving existing external-file semantics.

Then confine managed config paths at the shared manager boundary. Resolve against the correct per-repository data directory, canonicalize existing symlink ancestors, reject destinations outside, and ensure the Sail opens the approved canonical path while preserving the original configured RDF value. Test actual append behavior and potential local filesystem races to the extent supported by the existing writer; do not claim an untested race guarantee. Keep path validation and runtime opening consistent across bundled Sail factories. Leave unrelated Lucene index directories, FedX files, and optional ElasticsearchStore paths for their separate issues.

Finally run the focused live tests, Server and affected Sail/config module suites, formatter, copyright, and `git diff --check`. Grade compatibility as medium for managed repositories intentionally writing logs outside their data directory and document migration to an in-directory relative path. Publish a draft PR stacked on #6242 only after parent review, then verify local, remote, and PR head equality, ECA, and exact-head CI.

## Concrete Steps

Run from `/Users/havardottestad/.codex/worktrees/security-slow-query-log/rdf4j`. The ignored `.m2_repo` symlink reads the root cache; `mvnf --workspace slow-query-log` keeps new Maven artifacts private. Never run tests with `-am` or `-q`.

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace slow-query-log --retain-logs tools/server-spring
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace slow-query-log --retain-logs --it ManagedSlowQueryLogIT#rejectsAbsoluteExternalLogFile
    python3 scripts/agent-evidence.py --command '<failing command>' <Failsafe report directory> > initial-evidence.txt

The focused test must fail before production edits. The subsequent green run should have zero failures. Retain full logs and reports. Before publication run `scripts/checkCopyrightPresent.sh` and Maven `process-resources` with `-Dmaven.repo.local=.m2_repo` and the private workspace head/build settings, without `-q`.

## Validation and Acceptance

Server HTTP configuration PUT, update, SYSTEM commit, and reload should reject absolute, parent-traversing, or symlink-alias log paths that resolve outside the repository directory. Legitimate nested files and in-root aliases should still work, and the actual log should be written inside the approved root. An embedded application using `MemoryStore` with an external path should still write there. Tests must show the pre-fix failure and post-fix success, and relevant modules must have no new failures.

## Idempotence and Recovery

Do not edit production before the first in-repository red Failsafe report and persisted `initial-evidence.txt`. Preserve all untracked artifacts; never reset, clean, or broadly stage. If the live fixture cannot reach the path, repair the fixture before attributing a failure to the application. Use the isolated worktree for all modifications and private Maven workspace for builds.

## Artifacts and Notes

The baseline log, red Failsafe report, green summaries, exact diff, and CI check inventory will be recorded here as they are produced.

## Interfaces and Dependencies

Reuse `LocalRepositoryManager.setRepositoryConfigCustomizer(Consumer<RepositoryConfig>)` from #6242 and the existing `BaseSailConfig` hierarchy. Avoid new external libraries. A runtime-only resolved path or equivalent mechanism should reach bundled Sail factories without changing exported RDF configuration or embedded application defaults.

Revision note, 2026-10-23: Created the plan after source tracing and before any production edit or test addition. The exact runtime-path mechanism remains subject to the red test and factory inspection.

Revision note, 2026-10-23 07:32Z: The initial isolated root quick install and Server Spring baseline passed. Advanced to writing the smallest live red path test.

Revision note, 2026-10-23 07:45Z: Added and ran path variants plus cached-alias and persisted-reload regressions. Both actual external write and reload acceptance were observed before production edits. Advanced to implementation at the shared manager hook.
