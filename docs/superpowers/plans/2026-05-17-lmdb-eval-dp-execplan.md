# Productionize Eval-Time Dynamic Programming for LMDB Query Evaluation

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows `.agent/PLANS.md` in this repository.

## Purpose / Big Picture

LMDB query evaluation already has several ID-only fast paths that avoid repeated work by batching and caching repeated
probes. This change gives those fast paths an explicit eval-time dynamic programming layer: a shape classifier, a
per-query DP context, a kill switch, and telemetry that proves when repeated work was avoided. After this change, a user
can run a query with repeated EXISTS or join probe states and see `lmdbEvalDp...` metrics in the executed plan while the
fallback path remains available.

Dynamic programming here means storing the result of a repeated evaluation state, such as "this EXISTS subquery with
these outer bindings", and reusing it for later rows with the same state instead of opening the same LMDB cursor again.
The first implementation is LMDB-only and wires the existing membership/probe/batch kernels through the new classifier
and context. It does not replace the existing join-order dynamic programming planner.

## Progress

- [x] (2026-05-17T05:43:34Z) Reviewed the user plan, branch, existing Janino/codegen plan, and LMDB eval hot paths.
- [x] (2026-05-17T05:44:08Z) Ran root quick install: `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`; build succeeded.
- [x] (2026-05-17T05:50:11Z) Added and observed a failing focused telemetry test:
  `LmdbIdBGPEvaluationTest#batchBgpExistsRecordsEvalDpTelemetryWhenEnabled`.
- [x] (2026-05-17T05:56:50Z) Added `LmdbEvalDpClassifier` and `LmdbEvalDpContext` with a property-controlled
  kill switch, shape-derived keys, conservative unsafe-operator fallback, and membership-DP counters.
- [x] (2026-05-17T05:59:05Z) Added classifier tests for opt-out disabled behavior, normalized shape stability, and
  SERVICE fallback.
- [x] (2026-05-17T06:00:15Z) Wired the BGP batch EXISTS membership cache through eval-DP planned/actual telemetry.
- [x] (2026-05-17T06:00:15Z) Ran focused verification:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb '-Dtest=LmdbEvalDpClassifierTest,LmdbIdBGPEvaluationTest' -DskipITs verify`.
- [x] (2026-05-17T06:13:51Z) Wired statement-pattern EXISTS probe DP metrics for single probes, cache hits,
  batch duplicate-key reuse, and runtime unsafe fallback. Focused verification:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb '-Dtest=LmdbIdBGPEvaluationTest#repeatedSinglePatternExistsRecordsEvalDpProbeTelemetryWhenEnabled+batchSinglePatternExistsRecordsEvalDpProbeDedupeTelemetryWhenEnabled' -DskipITs verify`.
- [x] (2026-05-17T06:21:43Z) Added a red classifier test for default-enabled eval-DP, then flipped
  `org.eclipse.rdf4j.sail.lmdb.evaldp.enabled` to opt-out semantics: unset/true enables, explicit false disables.
- [x] (2026-05-17T08:31:00Z) Wired bounded join-group memo telemetry/replay into batch joins and ID aggregate
  count/distinct steps.
- [x] (2026-05-17T09:12:00Z) Changed LMDB theme benchmarks and query-plan snapshots to run with
  `READ_COMMITTED` store isolation so ID joins, eval-DP, and Janino-eligible paths are benchmarked.
- [x] (2026-05-17T09:46:00Z) Added bounded MINUS/anti-join membership DP with
  `org.eclipse.rdf4j.sail.lmdb.evaldp.minus.maxRightRows`; large right sides fall back to the existing evaluator.
- [x] (2026-05-17T10:32:00Z) Tightened join-group memo admission to full-batch-boundary groups only, avoiding
  q9-style zero-hit memo materialization and admission-map churn.
- [x] (2026-05-17T11:07:38Z) Verified read-committed benchmark contracts and q9 off/on controls. Current q9
  read-committed JMH: DP on `267.298 +/- 16.696 ms/op`; DP off `287.581 +/- 58.095 ms/op`.
- [ ] Run the wider MEDICAL_RECORDS matrix after the next idle window; current focused control is q9.

## Surprises & Discoveries

- Observation: Several DP-like kernels already exist: `LmdbIdBGPQueryEvaluationStep` has `existsCache` and
  `membershipCache`, `LmdbIdStatementPatternExistsQueryValueEvaluationStep` batches and dedupes probes, and
  `LmdbIdBatchJoinIterator` groups duplicate left probe keys.
  Evidence: existing tests in `LmdbIdBGPEvaluationTest` assert membership cache hits, oversized membership fallback,
  direct-probe fallback, and batch join grouping.

- Observation: `mvnf` correctly produced the red test once, but a later focused rerun unexpectedly selected
  `LmdbDevelopPlanParityIT`; a thread dump showed the Surefire fork sleeping inside parity timing assertions.
  Recovery: stopped that run and used direct Maven with a fully qualified `-Dtest=...` selector plus `-DskipITs`.

- Observation: `scripts/checkCopyrightPresent.sh` currently fails on pre-existing files unrelated to this change:
  `GenericIRISymmetryContractTest.java`, `LmdbOrderedTest.java`, `LmdbSupportedOrdersTest.java`,
  `UnionSailDatasetComparatorTest.java`, and `SimpleIRISymmetryContractTest.java`.
  Evidence: both copyright-check runs reported the same invalid headers; new files carry the required 2026 header and
  Codex signature.

- Observation: MEDICAL_RECORDS q9 is sensitive to enabled-but-unused DP work. The right side of its MINUS is too large
  for materialized membership DP, and optional `handledBy` can create many unique join probe groups.
  Recovery: cap MINUS right-side materialization and only build join-group memos when a group reaches the batch boundary,
  which is the cheap evidence that the same probe key will likely continue in a later batch.

## Decision Log

- Decision: Implement the first eval-DP layer as classifier/context/telemetry over existing LMDB ID kernels before
  adding new algorithms.
  Rationale: The current code already avoids repeated work in the highest-priority shapes, so the safest first
  behavior-changing step is to make this explicit, bounded, switchable, and observable without destabilizing semantics.
  Date/Author: 2026-05-17 / Codex.

- Decision: Keep the default property `org.eclipse.rdf4j.sail.lmdb.evaldp.enabled` disabled for this slice.
  Rationale: The user plan said rollout starts behind a disabled kill switch and flips only after parity and benchmark
  evidence. Tests can enable it explicitly.
  Date/Author: 2026-05-17 / Codex.

- Decision: Flip `org.eclipse.rdf4j.sail.lmdb.evaldp.enabled` to enabled by default, while preserving explicit
  `false` as the kill switch.
  Rationale: User requested default enablement after the classifier/context and statement-pattern probe telemetry were
  wired and verified. The implementation keeps the rollback path cheap and local.
  Date/Author: 2026-05-17 / Codex.

- Decision: Make LMDB theme benchmarks and plan snapshots use `READ_COMMITTED` by default.
  Rationale: The optimized ID-join/eval-DP paths are intentionally gated by isolation safety. Benchmarking at a stricter
  default isolation was measuring the fallback path and hiding the production path being optimized.
  Date/Author: 2026-05-17 / Codex.

- Decision: Keep MINUS membership DP bounded by an estimated right-side row cap, default `4096`.
  Rationale: Anti-join DP helps when the right-side membership set is small and reused; for q9 the right side is large,
  so materialization costs more than the default evaluator.
  Date/Author: 2026-05-17 / Codex.

- Decision: Admit join-group memo builds only for groups that run to the end of a full batch.
  Rationale: A full-batch-boundary group is strong evidence that the same key repeats across batches. A simple
  seen-before admission map added overhead for high-cardinality optional joins without producing cache hits.
  Date/Author: 2026-05-17 / Codex.

## Outcomes & Retrospective

This slice now covers the first production eval-time DP layer for LMDB BGP batch EXISTS, statement-pattern EXISTS
probes, bounded batch join-group replay, ID count/distinct aggregate fast paths, and bounded MINUS membership DP. It
adds the classifier, the per-evaluation context facade, the enabled-by-default property with explicit false opt-out,
planned/actual DP metrics, classifier safety tests, and parity tests that prove the new `lmdbEvalDp...` counters mirror
the existing cache counters where those counters already existed.

The current focused performance control is MEDICAL_RECORDS q9 with read-committed benchmark isolation: DP on
`267.298 +/- 16.696 ms/op`; DP off `287.581 +/- 58.095 ms/op`. The remaining broader work is the full MEDICAL_RECORDS
off/on matrix plus Janino off/on after the next idle benchmark window, and property-path frontier DP.

## Context and Orientation

The repository root is `/Users/havardottestad/Documents/Programming/rdf4j-stf`. The target module is
`core/sail/lmdb`. `LmdbEvaluationStrategy` prepares query evaluation steps. `LmdbIdBGPQueryEvaluationStep` evaluates a
basic graph pattern, meaning a set of statement patterns joined on shared variables. `LmdbIdBatchExistsFilterQueryEvaluationStep`
evaluates `FILTER EXISTS` for many outer rows at once. `LmdbIdStatementPatternExistsQueryValueEvaluationStep` handles
single statement-pattern EXISTS. `LmdbIdBatchJoinIterator` groups outer rows by probe key before opening right-side
cursors.

The existing join-order dynamic programming planner chooses the order of joins before execution. This plan adds
eval-time dynamic programming during execution, where repeated runtime states are memoized or grouped.

Janino remains an inner-loop specialization tool only. Existing generated batch EXISTS and batch join merger code should
stay behind the existing `org.eclipse.rdf4j.sail.lmdb.codegen.enabled` controls.

## Plan of Work

First, add tests that fail because eval-DP telemetry and kill-switch behavior do not exist. The focused tests should use
small in-repo LMDB stores and assert both query results and metrics, so the tests prove behavior and observability.

Second, add `LmdbEvalDpClassifier` in `org.eclipse.rdf4j.sail.lmdb`. The classifier should consume
`LmdbPlanDerivedCodegenShape.Descriptor` plus an operator kind, reject unsafe or disabled shapes, and return a small
immutable result containing mode, shape key, and fallback reason. It must classify from algebra shape, not query text.
Status: done for membership/probe/join/aggregate/path mode decisions; membership, probe, join, aggregate, and MINUS
membership modes have runtime metrics in this slice.

Third, add `LmdbEvalDpContext` in `org.eclipse.rdf4j.sail.lmdb`. It should be query-evaluation scoped, bounded, and
counter-oriented. For this slice it can expose reusable counters and shape decisions while the existing operator-local
caches continue to hold the actual memoized values. Later slices can move those value caches into the context.

Fourth, wire the existing batch EXISTS and BGP membership paths through the context. When eval-DP is enabled and the
classifier accepts a shape, record planned/actual metrics such as `lmdbEvalDpMode`, `lmdbEvalDpShape`,
`lmdbEvalDpMembershipLookupsActual`, `lmdbEvalDpMembershipCacheHitsActual`, and fallback reasons. When disabled, record
a clear disabled fallback metric and keep current behavior.
Status: done.

Fifth, extend the same context markers to statement-pattern EXISTS, BGP batch joins, and ID distinct/grouped aggregates
only where current code already has a repeated-state fast path. Do not add broad new algorithms in this slice.
Status: done for statement-pattern EXISTS probe/cache/dedupe metrics, BGP batch join-group memo telemetry, and ID
aggregate count/distinct telemetry.

## Concrete Steps

Run from `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

Before tests, install current modules:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

Run the focused red test:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbIdBGPEvaluationTest#batchBgpExistsRecordsEvalDpTelemetryWhenEnabled --retain-logs --stream

After implementation, rerun the same focused test, then broaden to:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb '-Dtest=LmdbEvalDpClassifierTest,LmdbIdBGPEvaluationTest' -DskipITs verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb '-Dtest=ThemeQueryBenchmark#firstRowBenchmarksUseReadCommitted' -DskipITs verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark '-Dtest=QueryPlanSnapshotCliTest#lmdbRunPersistsPageCardinalityEstimatorFeatureFlag' -DskipITs verify

Before finalizing, format and quick-install:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

## Validation and Acceptance

Acceptance requires query results to remain unchanged and executed-plan or node metrics to show eval-DP decisions by
default. With `org.eclipse.rdf4j.sail.lmdb.evaldp.enabled=false`, current behavior should continue and metrics should
show a disabled fallback. With the property unset or set to true, repeated BGP EXISTS tests should report one real
membership build, later membership cache hits, and eval-DP actual counters matching the existing membership counters.
LMDB theme benchmarks and query-plan snapshots must expose `lmdbStore.defaultIsolationLevel=READ_COMMITTED`.

## Idempotence and Recovery

The tests create temporary LMDB stores and can be repeated safely. The new system property must be restored in each test
finally block. If a focused test fails because a previous run overwrote Surefire reports, rerun the same method and use
the latest `target/surefire-reports` file as evidence.

## Artifacts and Notes

Initial quick install succeeded in 26.483 seconds with `BUILD SUCCESS`. The initial failing Surefire report was
persisted to `initial-evidence.txt`.

## Interfaces and Dependencies

Add `org.eclipse.rdf4j.sail.lmdb.LmdbEvalDpClassifier` with package-private static classification methods and immutable
result objects. Add `org.eclipse.rdf4j.sail.lmdb.LmdbEvalDpContext` with package-private counters and metric recording
helpers. No new dependency is required. Janino remains available through the existing LMDB dependency and is not used for
cache policy or context lifecycle.
