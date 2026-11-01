# Repair persistent-adjacency integration failures

This living ExecPlan follows `.agent/PLANS.md`. The user explicitly requested root-cause repairs, commits and pushes on PR6352; preserve the user's merge commit and all logical, ownership, retry and durability assertions.

## Purpose / Big Picture

Restore reliable query-ready in-memory adjacency, snapshot-safe reads, bounded-reader cardinality and transaction retry behavior after the persistent storage-format change. Supported configuration order must not determine whether an incoming adjacency build succeeds. Physical-format tests must exercise equivalent persisted-state contracts rather than removed permutation databases. Keep the PR draft and publish independently validated repair commits.

## Progress

- [x] (2026-11-01 19:16Z) Verified latest remote merge provenance.
- [x] (2026-11-01 19:17Z) Created isolated checkout and cache.
- [x] (2026-11-01 19:24Z) Reproduced grouping and range failures.
- [x] (2026-11-01 19:27Z) Repaired complete snapshot-local grouping selection; independent review and 61 new cases pass.
- [x] (2026-11-01 19:33Z) Validated and published grouping repair421e1593, preserving06f778.
- [in_progress] Repair remaining growth, estimator and loading terminal-state contracts.
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

Reserved-reader and renewal telemetry repairs: pre-fix CardinalityTest had two unchanged five-second timeouts; new scalar/bulk renewal cases exposed scanned3/matched5 and decreasing work totals. Restore doWithPriority; accumulate retired physical-reader work once after successful replacement, preserving lease/version and close/open ordering. Post-fix selection24 cases passes, including all CardinalityTest cases, the new next/fill renewal cases, persistent iterator contracts and direct adjacency snapshots. Independent review found no blocker. The zero-SPO wildcard reproduction also fails and remains a separate repair.

Zero-wildcard and unsigned-boundary repairs: the initial wildcard fixture failed empty, and expanded two-configuration API/reopen checks exposed signed inline ID failures in legacy logical key encoding. Added independent uint64 reference/roundtrip/order tests and real root/incoming partition scans; they failed before extending scalar Varint. The separate list writer then produced corrupt one-byte negative prefixes, reproduced by the actual scanner. Delegating list encoding to scalar fixed that; a small-run codec regression separately reproduced rejection of unsigned bases/deltas before fixing its threshold comparisons. The final gate145 cases passes, including the full paged-CSF explicit/inferred build, negative neighbor lookup, memory-account release, all existing Varint/RunCodec cases, grouping, wildcard APIs, applier, range and iterator contracts. Positive wire bytes remain unchanged; query normalization changes only zero SPO patterns, never graph0, signed IDs, writes or literal seek bounds. Independent review covers each production repair.

The same gate also validates10 retargeted telemetry/cancellation cases after preserving their12-case pre-retarget red selection (two wildcard cases plus10 fixture failures). Retargets use genuine persistent reads, context-residual filtering, explicit/inferred isolation and real native cancellation hit proof, not removed tuple databases. These test-only changes were published separately as 012904fc.

Actual-authority contract validation: 31 cases pass, including all 15 stats cases, nine startup cases, the independent native publication barrier, three compaction failure recovery modes, two pinned-before-clear GC cases and a checked native mutation failure. Namespace-only commits now prove unchanged RDF/value authority plus durable namespace changes rather than demand a removed physical context table. New compaction cases exercise a real page-put failure after delta deletion and retain caller-owned recovery, old-snapshot, rollback and reopen proofs. The five new compaction/GC cases do not by themselves waive the retired reindex test family.

Real mutation failure retarget: all 25 focused fault/ownership/replay cases pass after replacing dead context-hook overrides with actual persistent metadata mutation faults. The helper requires successful outgoing/incoming delta writes, exactly one fault at the requested distinct-context boundary, all captured writer cursors closed before abort, and abort before same-environment resize. Existing caller-owned retry, exactly-once callback, poisoning, cleanup and reopen assertions remain. One formerly false-green bulk replay test now proves its fault and growth occurred. No production hook was restored merely to trigger old fixtures.

Storage-fixture retarget validation: all 32 original failures in the legacy semantic/alias/scoped-union physical-name and count family now pass against actual native catalog, format certificate, merged persisted rows and same-snapshot plane counts. All 50 legacy semantic cases and both scoped-union cases pass; alias17/18 pass, with the untouched direct-object test still demanding160 scanned rows where the new OPSC path actually scans2. No logical bags, write-transaction IDs, dictionary invariants or work thresholds were weakened by this storage precondition increment. A new independent finite-IN telemetry test remains under correction because two legal physical plan orders use different telemetry shapes; it is not included here.

Query/aggregation fixture retarget validation: all 18 ordered-DISTINCT and 11 primitive-grouping cases pass, including actual OPTIONAL order barriers, separated duplicate witnesses, forced legal route hit proofs and exact generic/native bags. Five query-cost methods pass: residual filter work remains visible, finite IN executes exactly three selected bound-P/O LMDB probes with exact results under either legal VALUES/ExactDomain order, absent-row aggregate callbacks remain exact, real native seek work remains within the existing quote, and reduced-page census proves actual multi-quad pages. No query kernel production changes or cost-threshold relaxation.

Retired-contract phase B validated: exact reference liveness performs bounded real metadata lookup across positions/planes and rollback; logical preference changes retain native authority and all24 order semantics; malformed short/oversized certificates fail closed without changing nonvolatile files and recover after restoring the certificate; a real50→51 commit preserves pinned plans/rows and excludes fresh parallel readers from the old snapshot. All21 selected runtime cases pass. Two dead delayed-index GC cases are replaced only by the already-proven pinned-before-clear parameter pair, retaining the existing non-delayed pair. Explicit original→replacement mapping is committed beside this plan; remaining retired-worker growth/publication/crash families are not waived.

Loading terminal-state priority: a new three-case automated test reproduces a pre-existing upstream cleanup failure hazard. Both real run-directory and marker-delete IOException injections occur after both native commits with130 authoritative rows, yet commit was acknowledged/completed1 and restart reset to0; success control preserves130. The narrow repair marks terminal cleanup failure before notifying peers, preserves primary/suppressed failure identity and rejects false success. It does not add a completed-marker protocol or alter existing interrupted, unacknowledged import restart semantics. Red reports are preserved before editing production.

Terminal cleanup validation: the same three-case repro now passes (failed cleanup is unacknowledged, completed0, peer access fails closed; success retains130 rows), and all75 adjacent initial-import/marker-sync/fresh-NONE/acceleration cases pass unchanged. Independent review found no blocker. Additional unchecked and primary-plus-cleanup exception controls are being prepared; they do not change the repair protocol.

Aligned capacity repair: input count no longer masquerades as required native space. Admit one mutation unit, retain the complete pre-write journal, and check measured allocation after batch cursors close but before callbacks/derived publication. Real MAP_FULL and measured reserve exhaustion use the existing replay or caller-owned recovery paths. Ten cases pass: unchanged100k unique pinned-reader and94,683duplicate NONE controls, existing replay/compaction tests, and a new real reserve-only FROZEN test proving all4000 directional/2000 context puts, unchanged512KiB reserve, no callbacks/unsafe resize, explicit rollback and cold reopen. Independent review approved. This does not change the separate post-compaction policy or resolve List/count estimate consistency.

Additional real-path coverage: six terminal cleanup arms now pass, including unchecked RuntimeException/Error identity and actual native sync failure with exact cleanup IOException suppressed. Twelve aligned-write contracts pass using actual persistent DBI/cursor/directional-put observation, complete sparse inputs/callback ordering and reopen, rather than retired per-index sorts. All18 alias query/work cases pass with unchanged exact bags/routes and bounded physical object seeks across doubled unrelated populations. The global legacy prepared-order utility remains a separately reproduced positive-admission failure; its test is not waived. Corrected exact required header URLs and reran repository header validation before formatting.

Capacity-model and phase-C validation:31 cases pass. Generic List and count forecasts now share the fixed-schema model and preserve possible cross-plane duplicates; only a known single-plane NONE batch collapses adjacent semantically equal quads with constant extra memory. A direct red showed that unqualified List→count unification otherwise reintroduced duplicate-input growth. The soft-only snapshot fixture uses5MiB after its measured3,891,200-byte footprint proved4MiB cannot retain the unchanged512KiB reserve; both real soft/hard predicates and all held-writer assertions remain. Nine retired growth/setup cases now prove actual capacity-derived native growth across formats/planes and a real post-drop format-put MAP_FULL with fresh DBI ownership, rollback and reopen. Generic/streaming preflight controls pass. The separate terminal admission publication ordering race is under deterministic test investigation, not yet claimed fixed.
