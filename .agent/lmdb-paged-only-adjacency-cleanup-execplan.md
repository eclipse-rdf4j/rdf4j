# Remove retired LMDB adjacency bases

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB sail currently contains two implementations of the immutable in-memory adjacency base: the current paged
compact-sparse-fibre (paged-CSF) base and a retired native-arena base that remains selectable through an internal JVM
property. After this change every adjacency base is paged CSF. Operators no longer carry branches, data structures,
tests, or benchmark parameters for the retired implementation. This does not migrate persistent data because direct
adjacency is derived in memory from the authoritative LMDB statement indexes at startup.

## Progress

- [x] (2026-08-26 15:54Z) Confirmed the required root quick clean install succeeds before edits.
- [x] (2026-08-26 16:11Z) Removed the retired builder, locator, inline index, and selection switch.
- [x] (2026-08-26 16:11Z) Simplified lookup, enumeration, and consolidation to paged CSF.
- [x] (2026-08-26 16:11Z) Migrated active tests and benchmarks and removed retired-only coverage.
- [x] (2026-08-26 16:14Z) Purged retired references from tracked and selected untracked Markdown documents.
- [x] (2026-08-26 16:32Z) Ran formatting, focused tests, full LMDB verification, and final audits.

## Surprises & Discoveries

- Observation: the old on-heap CSR implementation was already absent; the remaining compatibility path was a
  selectable native-arena base.
  Evidence: repository history shows the on-heap CSR deletion in commit `77a08d6580`, while the pre-change
  `LmdbDirectAdjacencyStore` still sampled an internal rollback selector and conditionally called a second builder.
- Observation: shared commit overlays still use the adjacency arena and run codecs.
  Evidence: `LmdbAdjacencyDeltaApplier`, `LmdbAdjacencyDeltaGeneration`, and `LmdbPagedCsfConsolidator` allocate and
  decode overlay runs through those types, so removing the old base must not remove the overlay representation.

## Decision Log

- Decision: make paged CSF unconditional and let the retired JVM property become unused rather than reject startup.
  Rationale: the property is an internal rollback seam, not a supported configuration field; silently using the only
  implementation is the least disruptive removal behavior.
  Date/Author: 2026-08-26 / Codex.
- Decision: introduce `LmdbAdjacencyPlane` and `LmdbAdjacencyLookupContext` before deleting the retired locator.
  Rationale: plane identity and query-local CSF lookup state are current semantics that were accidentally hosted by
  a retired data structure.
  Date/Author: 2026-08-26 / Codex.
- Decision: retain the current CSF page and shard layout identifiers.
  Rationale: this task removes implementations, not physical layouts; renumbering an unchanged layout would make
  validation less meaningful.
  Date/Author: 2026-08-26 / Codex.
- Decision: preserve all untracked non-Markdown artifacts.
  Rationale: the user selected tracked source plus Markdown cleanup, while ZIPs, logs, benchmark output, and evidence
  remain useful historical artifacts and are unrelated dirty-worktree state.
  Date/Author: 2026-08-26 / Codex.

## Outcomes & Retrospective

Paged CSF is now the sole immutable adjacency base. The second builder, its sizing workspace, node locator, inline
incoming index, key-index wrapper, selection property, thread-local benchmark override, and all representation
branches have been removed. Plane constants and query-local lookup state now live in focused package-private types.

Dedicated retired-implementation tests and benchmarks were deleted; shared lifecycle, delta, query, consolidation,
node-predicate, and benchmark fixtures now build and inspect paged bases. The paged-builder gate passed 4 tests, the
query gate passed 57, the consolidation gate passed 13, and the complete LMDB verify passed 4,067 tests with 103
skipped and no failures or errors. The final root quick install also passed in 33.907 seconds.

The final scoped source and Markdown searches found none of the deleted symbols or rollback property, and
`git diff --check` is clean. Two tracked implementation plans devoted solely to the retired base and one selected
untracked Markdown design were deleted; maintained plans were rewritten around the paged key domain. The 1,273
untracked non-Markdown artifacts reported by the final audit remain present. Required Maven runs updated their
prescribed build and retained test logs; no non-Markdown artifact was deleted.

## Context and Orientation

All runtime code is in `core/sail/lmdb`. `LmdbDirectAdjacencyStore` owns the asynchronous build and publication
lifecycle. `LmdbInMemoryAdjacencyIndex` is one immutable published base. `LmdbPagedCsfBaseBuilder` scans one pinned
LMDB snapshot and creates `csf/ImmutablePagedQuadCsfIndex`. Committed changes above that base are immutable overlay
generations encoded by `LmdbAdjacencyRunCodec`; those overlays remain. `LmdbPagedCsfConsolidator` folds overlays into
a replacement paged base.

The retired native-arena base consisted of a separate builder, sizing workspace, node locator, inline incoming index,
and key-index wrapper. The locator also hosted the four logical plane constants and query-local lookup state used by
the current implementation. Those current semantics moved to small representation-neutral types before the retired
classes were deleted.

The checkout is intentionally dirty. Before every edit and final handoff, inspect `git status` and preserve unrelated
tracked and untracked files. Never use broad restore, clean, stash, or `git add .` commands.

## Plan of Work

First add package-private `LmdbAdjacencyPlane` with constants for outgoing explicit, incoming explicit, outgoing
inferred, and incoming inferred, plus the count and small validation/direction helpers. Add
`LmdbAdjacencyLookupContext` with an `ImmutablePagedQuadCsfIndex.LookupCursor` and the existing predicate-binding memo.
Move current callers to these types without changing behavior.

Next make `LmdbDirectAdjacencyStore.buildOnce` call `LmdbPagedCsfBaseBuilder` unconditionally using the configured
build-thread count. Remove the retired system property, thread-local override, selection scope, field, log branch, and
all `usesPagedCsf` conditions. Simplify `LmdbInMemoryAdjacencyIndex` so its CSF base is mandatory and its constructors,
row lookup, predicate-enumeration checks, and close path have no locator or inline-index state.

Replace the retired key-index wrapper with direct `ImmutablePagedQuadCsfIndex.KeyDomain` and `KeyCursor` use in
`LmdbDirectNativeAdjacency` and `LmdbDecodedNativeAdjacency`. Simplify `LmdbDirectNodeIterator` to enumerate the
node-predicate projection only, and simplify the store's variable-predicate and inlined-key logic around the fact that
paged incoming planes use raw object IDs. Remove impossible retired consolidation and fallback branches.

Delete the five retired production classes and their dedicated tests and benchmark. Remove obsolete milestone
bootstrap tests that only assert retired implementation types exist. Update shared delta, lifecycle, query,
consolidation, and memory tests to construct paged bases. Make direct-adjacency benchmarks paged-only by removing the
base-format parameter and comparison loop.

Finally search tracked Markdown and untracked Markdown for the retired classes, rollback property, and operational
instructions. Rewrite documents that still describe active behavior to paged-only terminology. Delete a document only
when its sole purpose is the retired implementation; do not rewrite or delete non-Markdown artifacts.

## Concrete Steps

Work from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`. Use `apply_patch` for source and
document changes. After the source compiles, run focused tests with retained logs, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPagedCsfBaseBuilderTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyQueryTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyConsolidationTest --retain-logs

Run copyright validation from `scripts`, then the repository formatter, then the complete module:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

The formatter command is the repository-prescribed exception to the no-quiet-tests rule; it does not run tests.
Finish with the root quick clean install command from `AGENTS.md` and scoped `git grep`/`rg` searches.

## Validation and Acceptance

Focused tests must demonstrate paged construction and lookup for all four planes, explicit and inferred data, full
and selected predicate coverage, inlined object IDs, empty stores, and continuation-page supernodes. Query tests must
cover bound rows, key/root enumeration, node-predicate projection serving and fallback, ordering, contexts, and exact
misses. Lifecycle tests must cover pending commits, overlay precedence, tombstones, consolidation, pinned snapshots,
rebuild/refusal paths, and memory-account release.

The LMDB module verify must have no new failure or error. The final quick install must compile the complete reactor.
A source/document search must find no reference to the deleted production types or rollback property in maintained
source, tests, benchmarks, tracked Markdown, or the selected untracked Markdown. Current CSF page version 3 and the
current shard layout identifier remain unchanged.

## Idempotence and Recovery

All builds and tests are repeatable. If compilation exposes a shared dependency on a retired class, extract only the
current semantic piece instead of restoring the retired implementation. If an offline dependency is missing, rerun
the exact failed Maven command once without `-o`, then return offline. On a failure, preserve Surefire/Failsafe reports
and update this ExecPlan before changing course. Never delete unexpected files.

## Artifacts and Notes

`maven-build.log` holds root install output. Focused and module verify logs belong under `logs/mvnf`. Preserve initial
and post-change evidence files and every pre-existing untracked artifact.

## Interfaces and Dependencies

There is no supported public API or persistent-store migration. `LmdbAdjacencyPlane` and
`LmdbAdjacencyLookupContext` are package-private implementation types. `LmdbInMemoryAdjacencyIndex` keeps its current
name but becomes paged-only. No dependency is added.
