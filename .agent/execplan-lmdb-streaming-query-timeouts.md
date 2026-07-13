# Stream LMDB native row results so query timeouts can cancel work

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance
with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

An unordered LMDB-native `SELECT` currently computes every result into an in-memory `List<BindingSet>` before it
returns the `CloseableIteration` consumed by RDF4J's repository layer. The repository layer installs the configured
`TimeLimitIteration` only after that call returns. A server query with three independent 1,024-row patterns therefore
tries to materialize roughly 1.07 billion projected rows before its one-to-three-second timeout can supervise the
work, and the JVM fails with `OutOfMemoryError: Java heap space` in `NativeRowsStep.project`.

After this change, unordered native row queries are pull-based. Calling `QueryEvaluationStep.evaluate` creates a
lightweight iteration without scanning LMDB. Each consumer request advances at most far enough to expose one logical
result. Closing the iteration, including closure by `TimeLimitIteration`, closes the active LMDB cursors and retained
probes. The factorized executor keeps its primitive tail batches and odometer but no longer pushes every combination
into a result list. The observable server behavior is that the existing timed-query stress test reports timeouts,
does not log an OOM or exhaust LMDB reader handles, and accepts a health query afterward.

## Progress

- [x] (2026-07-12 19:35Z) Classified the supplied failure and traced timeout installation versus eager evaluation.
- [x] (2026-07-12 19:35Z) Inspected the native row, factorized-tail, prefix-run, server-test, and benchmark paths.
- [x] (2026-07-12 19:38Z) Added the deterministic lazy-iteration test and observed it fail because `evaluate()` opened the cursor (`expected: 0`, `was: 1`); evidence is appended to `initial-evidence.txt` and retained in `logs/mvnf/20260712-193747-verify.log`.
- [x] (2026-07-12 19:43Z) Replaced unordered native row list materialization with a closeable pull iteration.
- [x] (2026-07-12 19:46Z) Replaced factorized callback emission with a stateful pull cursor; the focused test and
  all 33 factorized differential tests pass.
- [x] (2026-07-12 19:51Z) Hardened the server stress test and adapted the benchmark to measure full consumption and
  first-row closure. The unchanged stress dimensions produced 120 timeouts, zero reader-handle failures, no OOM,
  and a successful health query.
- [x] (2026-07-12 20:12Z) Benchmarked an isolated pre-production baseline against the candidate on local JDK 25.
  First-row time improved by 95.8%-99.9%; full-consumption variants stayed within the 10% acceptance threshold or
  improved.
- [x] (2026-07-12 20:30Z) Audited the other LMDB-native query roots and blocking child cursors for timeout lifecycle
  gaps. Ordered rows and native aggregation are confirmed follow-ups; property paths, membership/hash builds, and
  filtered-to-empty cursor loops need cancellation checkpoints.
- [x] (2026-07-12 20:49Z) Completed full LMDB-module, formatting, header, root quick-install, and final server
  verification. The LMDB module passed 1,487 tests with zero failures/errors; the final server run passed with 120
  timeouts, zero reader-handle failures, no OOM event, and a successful health query.

## Surprises & Discoveries

- Observation: `SailTupleQuery.evaluate` calls `sailCon.evaluate(...)` before wrapping the returned iteration with
  `enforceMaxQueryTime`; an implementation that does substantial work in `QueryEvaluationStep.evaluate` is therefore
  outside the timeout window.
  Evidence: `core/repository/sail/.../SailTupleQuery.java` assigns `bindingsIter = sailCon.evaluate(...)` and only on
  the following line calls `enforceMaxQueryTime(bindingsIter)`.
- Observation: the reported query takes `LmdbNativeFactorizedRows`, but factorization is not the memory leak. Its three
  uncorrelated branches are compact primitive value batches; `NativeRowsStep.evaluateAll` retains the billion-row
  cross product in both the projected-results list and the DISTINCT key map.
  Evidence: the OOM stack contains `LmdbNativeFactorizedRows.evaluate` -> `NativeRowsStep.evaluateAll` ->
  `NativeRowsStep.project` -> `QueryBindingSet.addBinding`.
- Observation: active uncommitted work changes `LoggingDispatcherServlet` to shut down and rethrow an OOM and owns the
  existing root `initial-evidence.txt`. This plan must neither overwrite that work nor use server shutdown as the query
  timeout fix.
  Evidence: the initial workspace audit shows the servlet and its new test outside the LMDB production module.
- Observation: ordered native rows still call `evaluateAll(bindings)` inside `NativeRowsStep.evaluate`, before the
  repository can construct `TimeLimitIteration`. An unlimited or DISTINCT `ORDER BY` retains every slot snapshot and
  the projected result list, so it has the same unsupervised-memory failure class as the reported unordered path.
  Evidence: `LmdbNativeRowStep.java` selects `evaluateAll` whenever `orderSlots.length != 0`; that method drains
  `arg.open(row)` into `snapshots`, sorts, projects, and slices before returning an iteration.
- Observation: native aggregation is lazy only at its outer boundary. Its first `hasNext()` calls
  `evaluateAll().iterator()`, while sequential scan cursors, parallel futures, group maps, and result lists remain
  method-local. `NativeGroupIteration.close()` only sets a non-volatile flag and clears the result references, so a
  timeout cannot reach or stop in-flight aggregation resources.
  Evidence: `LmdbNativeGroupStep.java` retains no active `RowCursor`; `LmdbNativeParallelAggregation.java` waits for
  every worker future and workers consult only their shared failure reference, not the timed iteration's close state.
- Observation: the generic native BGP root returns immediately and emits one row at a time, but its timeout close path
  is a data race. `NativePlanIteration.closed` is not volatile and `close()` can mutate each `Frame.cursor` concurrently
  with `findNext()` on the request thread.
  Evidence: `TimeLimitIteration.interrupt()` calls the delegate's `close()` on the timer thread; the native BGP
  iteration has no synchronization or cross-thread-visible cancellation state around `Frame.next()`/`Frame.close()`.
- Observation: several child cursors can still perform input-cardinality work inside one `RowCursor.next()` call.
  Unbound property paths collect all starts (and all vertices for zero-length paths) before their first result;
  existence paths can traverse a full reachable graph; filter, join, OPTIONAL, MINUS, and extension cursors may reject
  the entire input before returning. Membership and OPTIONAL payload accelerators can additionally build sets/maps of
  up to millions of rows during that call. The new parent iteration marks itself closed promptly, but cannot close
  the active child until its synchronized demand call returns.
  Evidence: `PathCursor.initializeStarts`, `PatternMembershipProbe.build`, `PatternPayloadProbe.build`, and the looping
  `next()` implementations contain no cancellation check supplied by the owning iteration.
- Observation: the available local runtime is Azul Zulu OpenJDK 25 LTS (`25+36-LTS`), not JDK 26 as assumed.
  Evidence: `java -version` captured before both baseline and candidate benchmark runs.

## Decision Log

- Decision: use a pull-based interpreted cursor, not a worker thread, queue, new dependency, or runtime-generated code.
  Rationale: the query engine already exposes cursor-shaped storage APIs. A state machine removes the full
  materialization and preserves consumer backpressure without adding scheduling, buffering, or cross-thread ownership.
  Date/Author: 2026-07-12, Codex.
- Decision: keep DISTINCT keys as native `long` tuples and project only when a row survives DISTINCT and OFFSET.
  Rationale: native ids avoid early RDF value materialization; the seen-set is semantically required, while the result
  list is not. Consumer pacing bounds how quickly this required state can grow before a timeout closes it.
  Date/Author: 2026-07-12, Codex.
- Decision: expose factorized output through an internal `FactorizedRowCursor extends RowCursor` whose current row has a
  multiplicity.
  Rationale: COUNT-role branches can preserve multiplicity without inserting the same `BindingSet` into a list many
  times, while ENUM branches retain the existing odometer over primitive value batches.
  Date/Author: 2026-07-12, Codex.
- Decision: limit production changes to unordered native row queries. Ordered rows and native aggregation remain
  blocking and are separate cancellation audits.
  Rationale: the reported query and server regression are unordered projections; extending scope into parallel
  aggregation or sorting would multiply risk without being needed for this root cause.
  Date/Author: 2026-07-12, Codex.
- Decision: retain the server stress dimensions and add explicit OOM observation rather than lowering pressure.
  Rationale: lowering 1,024 statements, 120 requests, eight workers, or the timeout distribution could hide the
  regression instead of proving that streaming cancellation fixes it.
  Date/Author: 2026-07-12, Codex.
- Decision: at the user's request, audit every LMDB-native query root and cursor family now, but keep additional
  production changes out of this patch.
  Rationale: the audit confirms distinct designs are required for blocking sort, aggregation/parallel-worker
  cancellation, and checkpoint propagation through nested cursors. Folding those into the unordered-row regression
  would violate the plan's stated scope and materially increase semantic and concurrency risk.
  Date/Author: 2026-07-12, Codex.

## Outcomes & Retrospective

The unordered row implementation and acceptance work are complete. The focused regression moved from one failure
(`cursor opens during evaluate(): expected 0 but was 1`) to green, and all 33 factorized differential tests pass. A
second focused close-state regression proved that `HashMap.clear()` retained the DISTINCT table; close now severs the
compiled step, input binding, DISTINCT map/probe, row, value context, current result, and active cursor references in a
`finally` block, and its focused test is green. The full LMDB module passed 1,487 tests with zero failures/errors and
three skips. The final server test completed with 120 timed-out queries, zero reader-handle failures, no OOM event,
and a successful final health query. Header checks, repository formatting, and the root `-Pquick clean install` pass.

Across the five benchmark parameters, first-row closure improved from 7.739-111.147 ms/op to 0.057-0.062 ms/op. Full
consumption improved in three variants, changed by +5.96% for `selectDistinctTail`, and changed from 13.330 +/- 0.237
to 14.460 +/- 0.663 ms/op (+8.48%) for the remeasured `selectTwoHopLeaf`; every candidate meets the 10% threshold.
Allocation and JIT profiles were not collected because the wall-clock acceptance checks passed without a regression
requiring profiling.

The broader audit found two confirmed root-level timeout defects (ordered rows and native aggregation), one generic
BGP cross-thread-close defect, and a shared cancellation-granularity issue in nested cursors. These are deliberately
documented as follow-up work rather than partially repaired here.

## Follow-up Timeout Audit

The audit covered every LMDB-native `QueryEvaluationStep` implementation plus child cursors that can do
input-cardinality work inside one demand call. Scanner output was treated as leads and each result below was confirmed
against the surrounding source.

| Priority | Query path | Current cost and timeout gap | Recommended follow-up | Verification needed |
| --- | --- | --- | --- | --- |
| High | Native `ORDER BY` rows | `evaluate()` performs O(N) scan/materialization plus O(N log N) sort before the timer exists; memory is O(N), or O(offset+limit) only for the bounded non-DISTINCT top-k case. | Return a blocking-sort iteration immediately, retain its active scan cursor, and check cross-thread cancellation while collecting. | Deterministic no-work-in-`evaluate()` test, timeout-close scan test, ordering/DISTINCT/slice parity, large-sort benchmark. |
| High | Native aggregation, including parallel aggregation | First demand performs O(N) scanning and retains O(G) group state/results. Sequential cursors and parallel futures are method-local, so `close()` cannot stop them; parallel workers do not observe iteration closure. | Introduce an aggregation execution object owning cursors, workers, failure/cancel state, and maps; make close publish cancellation and close all sources. | Blocking-cursor close test, worker cancellation test, group/aggregate differential suites, high-cardinality group timeout test. |
| High | Unbound/existence property paths | One `next()` can do O(V+E) work and retain O(V) starts/discovered/queue state before returning. Parent timeout close waits for the synchronized demand call. | Propagate a lightweight cancellation checkpoint into path scans/BFS and avoid collecting all starts before first result where semantics allow. | All-pairs first-row test, unreachable-target timeout test, early-close probe release, path parity suite. |
| High | Filtered-to-empty joins, OPTIONAL, MINUS, and extension | Cursor `next()` methods may reject O(N) or an entire Cartesian product before returning, with no owning-iteration cancellation check. | Add a shared native cancellation token/checkpoint used by looping cursor implementations; keep cursor close idempotent. | Recording cursors that block/reject, timer-thread close visibility tests, existing differential fuzzing. |
| Medium/High | Membership and OPTIONAL payload accelerators | Lazy builds scan O(N) rows and retain O(K) primitive set/map state (configured caps reach millions) inside one `next()` call; build loops have no cancellation check. | Check the shared cancellation token per batch and discard partial structures on close/failure. | Mid-build cancellation test, retained-state release test, membership/left-join parity and benchmark. |
| Medium | Generic native BGP | Evaluation is pull-based and normally O(1) retained row state, but `closed` is not volatile and timer-thread `close()` races with request-thread `Frame.next()`/`Frame.close()`. | Give the iteration a cross-thread-visible cancellation state and serialize or otherwise make frame resource closure safe. | Deterministic concurrent close test under a recording `PatternCursor`, plus BGP differential tests. |

## Context and Orientation

All paths are repository-relative. The production module is `core/sail/lmdb`, package
`org.eclipse.rdf4j.sail.lmdb`. A slot is one position in `RowState.slots`, a primitive `long[]` containing LMDB value
ids for query variables. `UNKNOWN` means unbound. A `SlotPlan` opens a `RowCursor`; calling `RowCursor.next()` mutates
the shared `RowState` to the next solution. `NativeRowsStep` is the compiled unordered/ordered projection root. It
currently calls `evaluateAll`, which builds all projected rows and then wraps `rows.iterator()`.

`LmdbNativeFactorizedRows` optimizes a multi-pattern join by scanning independent trailing branches once. ENUM branches
store projected primitive ids in value batches, COUNT branches produce only multiplicity, and EXISTS branches stop at
the first match under DISTINCT. Its current `evaluate(RowState, RowSink)` method pushes every odometer combination into
`NativeRowsStep`'s list. The completed but untracked `execplan-factorized-rows-pipeline.md` records why these roles and
memoization are correct; this ExecPlan restates the required behavior and does not depend on that artifact being
tracked.

`tools/server-boot/src/test/java/org/eclipse/rdf4j/tools/serverboot/LmdbTimedOutQueryReadHandleTest.java` starts a real
server, loads 1,024 statements, and sends 120 Cartesian-product queries through eight clients with one-to-three-second
server timeouts. It already checks timeouts, LMDB reader-handle errors, and eventual health, but it does not explicitly
fail when an OOM is logged. `FactorizedRowsStarBenchmark` currently calls `QueryResults.asList`, which introduces a
caller-side list and prevents it from isolating engine streaming.

## Plan of Work

Milestone 1 establishes a deterministic contract before production changes. Add
`LmdbNativeRowStepIterationTest` in the LMDB test package with recording `SlotPlan`, `RowCursor`, and native source
fixtures. Construct an unordered `NativeRowsStep`, call `evaluate`, and assert that neither `open` nor `next` ran. Then
request one result and assert only the necessary cursor advancement occurred; close and assert all resources closed.
Add a factorized Cartesian-product fixture and assert first-row demand does not enumerate the remaining combinations.
The first laziness assertion must fail against the current implementation. Run only that method with `mvnf`, retain
its log, and append a clearly labelled evidence block to the existing `initial-evidence.txt` without replacing its
servlet evidence.

Milestone 2 implements the ordinary and prefix-run pull paths in `LmdbNativeRowStep.java`. For `orderSlots.length ==
0`, return a dedicated closeable iteration immediately. On first demand it seeds `RowState`, creates `AggContext`, and
selects the prefix-run, factorized, or ordinary cursor. Per demand it advances until one projected result survives
DISTINCT and OFFSET. Track LIMIT, remaining multiplicity, and the current projected row as primitive counters/one
stable `QueryBindingSet`; skip OFFSET across multiplicity arithmetically. A zero LIMIT returns empty without opening
LMDB. Closure is idempotent and cross-thread-visible, closes whichever cursor is active, and clears the DISTINCT map
and current row. Keep the existing ordered `evaluateAll` path unchanged.

Milestone 3 changes `LmdbNativeFactorizedRows` from callback push to cursor pull. Add a package-private
`FactorizedRowCursor extends RowCursor` with `long multiplicity()`, and make the factorized plan open one cursor for a
`RowState`. The cursor owns the prefix cursor, tail branch results, odometer indices, current enum count, and current
multiplicity. `next()` first advances an unfinished odometer; when exhausted it restores enum slots, advances the
prefix, applies prefix-only filters, resolves each tail branch, and prepares the next odometer. COUNT multiplication
continues to saturate at `Long.MAX_VALUE`; EXISTS remains 0/1; ENUM values are written to the shared row only for the
current combination. `close()` restores temporary slots, closes the prefix, closes all branch probes, and releases
memo/value arrays. Preserve engagement and memo-bypass counters and kill switches.

Milestone 4 hardens acceptance and measures the hot path. Add a bounded test appender to the server test that stores
only the first OOM description by walking throwable causes; assert it remains absent after timed queries and health.
Do not change stress constants. In `FactorizedRowsStarBenchmark`, replace `QueryResults.asList` with iteration and a
primitive row count, then add `firstRowThenClose` to measure lazy startup independently of full consumption. Capture
the pre-production benchmark after the benchmark-only change and the post-production benchmark with identical JDK,
flags, parameters, warmup, measurement count, and forks.

## Concrete Steps

Run all commands from the repository root. Do not use `-am` or `-q` for tests.

Focused failing and passing test:

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LmdbNativeRowStepIterationTest#evaluateDoesNotConsumeRowsBeforeIterationDemand \
      --module core/sail/lmdb --retain-logs

Factorized correctness and LMDB module verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedRowsTest \
      --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Server acceptance:

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LmdbTimedOutQueryReadHandleTest#lmdbRepositoryStillAcceptsQueriesAfterManyTimedOutServerQueries \
      --module tools/server-boot --retain-logs

Formatting and header checks:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Benchmark both `executeQuery` and `firstRowThenClose` using:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.FactorizedRowsStarBenchmark \
      --method <method> --warmup-iterations 4 --measurement-iterations 4 --forks 1

Use the same command and JDK for baseline and candidate. If full-consumption average time regresses by more than 10%
outside overlapping uncertainty, capture JFR or allocation evidence and remove the regression before completion.

## Validation and Acceptance

The new focused test must fail before production edits because `NativeRowsStep.evaluate` opens and drains its cursor,
then pass after the refactor. Differential factorized tests must preserve result multisets, COUNT multiplicity,
DISTINCT/EXISTS behavior, filters, named graphs, LIMIT/OFFSET, and fallback gates. The full LMDB module must introduce
no new failures.

The server test passes only when at least one request times out, no response/log contains `MDB_READERS_FULL`, no OOM
was observed, and the final health query returns HTTP 200. Its original workload remains unchanged. `firstRowThenClose`
must no longer scale with the full result cardinality; full `executeQuery` must remain within the 10% threshold. Report
benchmark delta, allocation delta (or unknown), JIT/profile evidence (or not inspected), exact commands, and confidence
for the local JDK 25; do not generalize unverified JIT claims to other JDKs.

## Idempotence and Recovery

The test and build commands are safe to repeat. Preserve all pre-existing tracked and untracked changes. Before each
edit, inspect the relevant path's diff; if another actor changes one of the LMDB files concurrently, stop and reconcile
rather than overwrite it. Generate new evidence into a temporary file, then append it to the existing root
`initial-evidence.txt`; never truncate that file. The factorized kill switch
`rdf4j.lmdb.factorizedRows.enabled=false` remains the diagnostic fallback, but it is not an acceptable final fix.

## Artifacts and Notes

Initial external evidence: `OutOfMemoryError: Java heap space` allocating `QueryBindingSet` entries from
`NativeRowsStep.project`, reached through `LmdbNativeFactorizedRows.evaluate` and `NativeRowsStep.evaluateAll`. The
repository root already contains servlet-specific failing evidence; append LMDB-specific evidence with its command,
Surefire report, short failure summary, and retained Maven log path.

## Interfaces and Dependencies

No public interface or dependency changes. The internal end state is:

    interface FactorizedRowCursor extends RowCursor {
        long multiplicity();
    }

    final class LmdbNativeFactorizedRows {
        FactorizedRowCursor open(RowState row) throws IOException;
    }

`NativeRowsStep.evaluate(BindingSet)` continues to return `CloseableIteration<BindingSet>`. Ordered queries continue to
use the existing materializing implementation. Existing system properties, physical-plan descriptions, counters, and
generic fallbacks retain their names and defaults.

Plan revision note (2026-07-12): initial self-contained implementation plan created after reproducing the managed-heap
classification and tracing the timeout boundary around the native row executor.

Plan revision note (2026-07-12): broadened the investigation, without broadening production scope, after the user
asked whether other query shapes share the timeout defect. Recorded ordered rows, aggregation, generic BGP closure,
property paths, and nested cursor cancellation as follow-up findings.
