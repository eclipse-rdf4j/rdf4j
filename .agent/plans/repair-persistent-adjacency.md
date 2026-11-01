# Repair persistent-adjacency integration failures

This living ExecPlan follows `.agent/PLANS.md`. The user explicitly requested root-cause repairs, commits and pushes on PR6352; preserve the user's merge commit and all logical, ownership, retry and durability assertions.

## Purpose / Big Picture

Restore reliable query-ready in-memory adjacency, snapshot-safe reads, bounded-reader cardinality and transaction retry behavior after the persistent storage-format change. Supported configuration order must not determine whether an incoming adjacency build succeeds. Physical-format tests must exercise equivalent persisted-state contracts rather than removed permutation databases. Keep the PR draft and publish independently validated repair commits.

## Progress

- [x] (2026-11-01 19:16Z) Verified latest remote merge provenance.
- [x] (2026-11-01 19:17Z) Created isolated checkout and cache.
- [x] (2026-11-01 19:24Z) Reproduced grouping and range failures.
- [x] (2026-11-01 19:27Z) Repaired complete snapshot-local grouping selection; independent review and 61 new cases pass.
- [in_progress] Validate and publish grouping repair.
- [ ] Repair reader admission and growth contracts.
- [ ] Retarget retired instrumentation without weakening assertions.
- [ ] Rerun failure ledger and broad gates.

## Context and Orientation

The repair worktree is `/workspace/scratch/bfa8fbb86df5/rdf4j-persistent-adjacency-repairs`, based on `06f778b1d0a2caaba4ec160ce2c6e7c91b5ef8e8`. Its parents are the supplied implementation `0c10448` and upstream main `1dc1a74`; the merge only updates GroupIteratorTest. `core/sail/lmdb` owns storage and the in-memory adjacency builder. `LmdbAdjacencyTripleStoreScanner` currently asks for only a single leading statement field, whereas grouping requires a complete tuple order. `TripleStore` represents logical orders and scans persistent compressed pages, deltas and metadata. Existing CI has 288 failing testcases; this is not 288 distinct bugs. `repair-evidence/failure-ledger.json` records every original case and subsequent evidence.

## Plan of Work

First add scanner tests covering advertised support, actual full and predicate-selected scans, all explicit/inferred directions, contexts and configuration permutations. Add close/reopen coverage at the repository API. Observe red Surefire evidence before modifying production. Then select full grouping order in the pinned transaction and use the same selection for capability checks and actual scans; do not merely loosen admission. Review all range-planning paths for equivalent ordering requirements. Obtain independent review, run focused builder/readiness suites and the existing benchmark CLI test, and publish a small verified commit whose parent includes the latest remote branch.

Then use the failure ledger to address remaining families: reserved reader admission, conservative capacity estimates versus replay/borrowed-snapshot ownership, and dead old-index failure injection. Retarget physical assertions only after identifying their original logical contract. Preserve rollback, crash recovery, reader cleanup, exact results and ownership checks. Every behavior-changing repair must have pre-fix automated evidence and the same post-fix selection. Maintain one `in_progress` step.

## Concrete Steps

Use Java25/Maven from `/tmp/rdf4j-tools/env.sh`, the worktree's independent `.m2_repo`, offline first. Run tests without `-am` or `-q`. A local-only `core/sail/lmdb/pom-repairs.xml` copies the module POM with `reuseForks=false`; CLI sets one fork and a3GiB heap, retaining the Mockito agent and assertions. Example from the worktree:

    mvn -o -B -ntp -Dmaven.repo.local=.m2_repo -f core/sail/lmdb/pom-repairs.xml -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=3g -Dtest=LmdbAdjacencyGroupingOrderTest -DskipITs=true test

Preserve each command/log/exit and reports under `repair-evidence/`, and initial red snippets in root `initial-evidence.txt`. Use `-am` only in compile/install commands with tests explicitly skipped. Keep source and cache idle while another JVM owns the CPU slot.

## Validation and Acceptance

The new grouping tests must fail before the repair and pass afterward. Reopened repositories must become exact at the current revision for all covered orders and return all original statements. Scanners must emit each `(node,predicate,plane)` group exactly once with sorted unique `(neighbor,context)` pairs. Focused existing source-family, range, selected-coverage and CLI readiness tests must pass. Each remaining CI family must receive a named root cause and valid replacement or passing unchanged contract, not a skip or widened timeout. A remote repair commit must match the validated local tree and preserve the current branch head as its ancestor.

## Surprises & Discoveries

Initial static review identifies three independent concerns: generic incoming P-order chooses PSOC rather than POSC; cardinality uses an ordinary rather than reserved reader permit; legacy context/index mutation fault hooks are unreachable in the new writer. These are leads until reproduced in the repair checkout. Existing performance evidence is preserved separately and must not be confused with post-repair measurements.

## Decision Log

- Decision: use a new worktree and copied private cache, preserving the published benchmark checkout and all untracked evidence. Reason: reproducibility and no accidental overwrite of the user's merge. Date:2026-11-01.
- Decision: prioritize complete grouping order, not descriptor-list sorting alone. Reason: arbitrary configured logical orders can share a leading field but interleave groups or neighbor/context pairs. Date:2026-11-01.

## Idempotence and Recovery

Use disposable fixture stores only. Never reset remote changes, force push, restore unrelated files or remove untracked artifacts. Re-fetch the remote branch immediately before publication; reconcile any advance before writing a ref. Save connector receipts and verify exact remote SHA/tree after each incremental push. No merge, approval or release is authorized.

## Outcomes & Retrospective

Initial red evidence:58 cases,29 failures,0errors; added parallel-range selection test:3 cases,1 failure selecting PSCO instead of canonical PSOC. Reports preserved under repair-evidence. Production repair starts only after these reports. Independent maximum-depth review is tracing remaining ownership/growth/cardinality/replay failures. Update this section after each verified milestone.

Grouping validation:61 new cases pass; existing selection53 cases has46 passes and7 separately classified pre-existing failures (six zero-wildcard applier oracle failures, one retired SPOC skipped-row work assertion). Kernel, WCOJ direct frontier, range planning and bootstrap suites pass unchanged. Cross-module CLI readiness validation follows module install.

The exact CI-blocking QueryPlanSnapshotCliTest.directAdjacencyWaitInitializesReusedLmdbStore now passes unchanged (1/1). Serial configuration/reopen matrix also passes with rdf4j.lmdb.prefixRun.enabled=false (58/58), proving optional range planning does not gate support. Module install, formatting and diff checks passed. No claim is made that the remaining failure families are fixed by this commit.
