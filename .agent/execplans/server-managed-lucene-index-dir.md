# Confine Server-managed Lucene indexes to repository data

This ExecPlan is maintained under `.agent/PLANS.md`. It applies to the isolated worktree `/Users/havardottestad/.codex/worktrees/security-lucene-index-dir/rdf4j` on branch `GH-0000-server-lucene-index-dir`, stacked on draft PR #6246 at `2251a4188ea2d169bc6cad8d64f45711aef67750` (which depends on #6242). The Lucene diff reuses #6246's repository-root-aware Server config hook. No issue number was assigned, so `GH-0000` follows the repository branch convention.

## Purpose / Big Picture

A client permitted to write RDF4J Server repository configurations can choose a Lucene index path. The current Lucene Sail resolves that path against its data directory but accepts absolute paths and `..`, then a bundled Lucene index opens the resulting filesystem directory. After this change, Server-managed Lucene indexes must resolve inside their repository data directory. A live Server test should reject an escaping config before any external directory is opened, while an in-directory index still works. Embedded repository managers retain their existing external-path behavior.

## Progress

- [done] prepare isolated base and build (2026-10-23 08:28Z)
- [done] reproduce managed Lucene directory escapes (2026-10-23 08:40Z)
- [done] rebase tests and rerun focused red (2026-10-23 08:47Z)
- [done] design effective parameter path confinement (2026-10-23 08:49Z)
- [done] implement validated runtime path override (2026-10-23 08:55Z)
- [done] align repaired prerequisite (2026-10-23 09:19Z)
- [done] verify nested reload and embedded behavior (2026-10-23 09:28Z)
- [in_progress] document migration and review candidate
- [todo] format and publish reviewed draft
- [todo] monitor exact published CI head

## Surprises & Discoveries

`LuceneSailConfig` has both a required `lucene.indexDir` value and generic literal parameters. `LuceneSailFactory.getSail` sets `LuceneSail.LUCENE_DIR_KEY` from `getIndexDir()` and then applies generic parameters, so a generic `lucene.lucenedir` value can override the directory used by the index. `LuceneSail.setDataDir` resolves the selected path but does not confine it. Bundled `LuceneIndex` and `LuceneIndexNIOFS` open that path through Lucene filesystem directory APIs. These are source observations awaiting live red tests.

## Decision Log

- Decision: Stack this PR on #6246. Rationale: #6246 passes the managed repository root into the Server config hook, which Lucene confinement needs; the original #6242 hook had no root parameter. Parent approved the stack after source review. Date/Author: 2026-10-23, Codex.
- Decision: Test both `lucene.indexDir` and generic `lucene.lucenedir`. Rationale: the factory's parameter application order makes the latter the effective runtime path when present. Date/Author: 2026-10-23, Codex.
- Decision: Validate the effective configured value for built-in filesystem index classes, then pass a canonical runtime value through either factory assignment while exporting the original RDF spelling. Rationale: `LuceneSailFactory` applies generic parameters after `indexDir`, and validation of only one field could be bypassed. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Not complete. The initial offline root clean install passed. The live 14-case Server regression suite was red on #6242 with nine security-path failures and five passing compatibility cases. The focused test was rerun on #6246's original head before production edits and failed as expected (Failsafe 1/1/0); `rebased-initial-evidence.txt` retains the report. After #6246 incorporated #6242's CI fixture repair, this unpublished branch was rebased onto its new head `2251a4188e`. The candidate passed all 14 live cases and the Lucene API (17), Lucene (66), Server Spring (251), and Server (122, one skipped) module selections on that head. The prerequisite merge changed test fixtures only, not Lucene production behavior.

## Context and Orientation

`tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/manager/LocalRepositoryManagerFactoryBean.java` creates the Server's `LocalRepositoryManager`, a manager that stores each repository under its own data directory. Its config customizer calls `ServerManagedRepositoryConfigPolicy` before creating or reloading a managed repository. `core/sail/lucene-api/src/main/java/org/eclipse/rdf4j/sail/lucene/config/AbstractLuceneSailConfig.java` stores the RDF configuration; `core/sail/lucene/src/main/java/org/eclipse/rdf4j/sail/lucene/impl/config/LuceneSailFactory.java` converts it into a Sail; `LuceneSail.setDataDir` resolves the path; and bundled index implementations open it. A Sail is the storage engine behind a repository. A nested delegate is another Sail or repository wrapped by one above it.

`tools/server/src/test/java/org/eclipse/rdf4j/http/server/TestServer.java` provides a live embedded Server and a helper to find the managed repository directory. The #6246 base also supplies a refresh helper. A focused integration test next to it now covers 14 path and compatibility cases. The Server hook must walk nested Sail delegates, and it must re-evaluate persisted spelling on reload because a symbolic link can change target between loads. The selected runtime path must be canonical, so a cached index does not reopen an alias that was changed later. Ordinary embedded managers without the Server hook must continue to permit their existing path choices.

## Plan of Work

First add the smallest live Server integration test for an absolute external index path. Capture its Failsafe failure in `initial-evidence.txt` before editing production. Expand red tests to cover relative parent traversal, an external symlink, nested Sail delegates, a generic parameter override, and a persisted alias changed before reload. Include positive relative, absolute, and symbolic-link paths that resolve inside the repository root, plus an embedded manager with an external path.

Then use the existing Server-managed config hook to validate the effective Lucene directory against `manager.getRepositoryDir(config.getID())`. Resolve the same path that the factory will pass to `LuceneSail`, including the generic override, and canonicalize it before the index is initialized. Preserve the original RDF spelling on export rather than writing a machine-specific absolute path into `config.ttl`. Keep the library's default behavior unchanged. Do not rely on a SPARQL-text or HTTP-handler check, since SYSTEM configuration and persisted reload also create repositories.

Finally run focused live tests, affected Lucene/config and Server modules, formatting, copyright, and `git diff --check`. Give the parent a complete candidate diff and evidence for independent review before pushing. After review, publish a scoped draft PR stacked on #6246 and verify local/remote/PR SHA equality, ECA, and exact-head CI.

## Concrete Steps

From `/Users/havardottestad/.codex/worktrees/security-lucene-index-dir/rdf4j`, the initial root clean install already succeeded using the private Maven write head and `maven-build.log`. Continue with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace lucene-index-dir --retain-logs --it ManagedLuceneIndexDirIT#rejectsAbsoluteExternalIndexDirectory
    python3 scripts/agent-evidence.py --command '<same mvnf command>' <Server Failsafe report directory> > initial-evidence.txt

The first command must fail before any production edit. `mvnf` performs its own root quick install and uses a private workspace; tests must never use `-am` or `-q`. After the fix, run the full new class, `core/sail/lucene-api`, `core/sail/lucene`, `tools/server-spring`, and `tools/server`. Preserve full logs and compact reports. Run `scripts/checkCopyrightPresent.sh` and Maven `process-resources` without `-q` before publication.

## Validation and Acceptance

An HTTP repository config containing a path outside the managed root should fail during repository creation or reload and must not create an external index directory. This includes absolute, `..`, symlink, nested, and generic-parameter paths. In-root paths should create a usable index and retain the original RDF configuration spelling. A directly managed embedded repository should still be able to use a configured external path. A cached index should use its approved canonical directory if a configured alias later changes.

## Idempotence and Recovery

Keep all changes in this isolated worktree. Preserve untracked evidence and logs; do not clean/reset the checkout or broadly stage it. Repeated live tests must handle their own test-owned symlink fixtures. If a red test fails during fixture setup instead of path validation, fix that fixture and preserve the original report before attributing the result to production behavior. If offline Maven lacks an artifact, retry the same command online once, then return to offline mode.

## Artifacts and Notes

`maven-build.log` records the successful initial root clean install. `initial-evidence.txt` will hold the first in-repo red Failsafe report. Subsequent focused/module reports, full Maven logs, and the final scoped diff will be kept in this worktree.

## Interfaces and Dependencies

Use the #6246 `ServerManagedRepositoryConfigPolicy.configure(RepositoryConfig, File)` hook for Server-only validation. `AbstractLuceneSailConfig` owns the configured `indexDir` and generic parameter `Properties`; `LuceneSailFactory` applies them in order, and `LuceneSail.LUCENE_DIR_KEY` is the key for the actual filesystem directory. The runtime override may require a separate transient value and configured-value accessors so export continues to serialize the original path. Do not add a new dependency. This document will be updated as tests settle the exact contract.

Plan change note (2026-10-23): created after the isolated baseline build and source trace to keep red-first and runtime-parameter precedence explicit.
