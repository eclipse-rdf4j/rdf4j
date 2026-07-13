# Double sketch-disabled LMDB transaction throughput

> Superseded for packed-storage decisions by `.agent/execplan-lmdb-query-ready-four-index-load.md`. The completed TPS measurements remain historical evidence; new loading work may remove the unshipped packed representation so that ordinary indexes are query-ready at commit.

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must remain current as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

LMDB-backed RDF4J applications should be able to commit synthetic and FOAF write transactions at least twice as fast as merge commit `b2cc8fadd7` while the sketch-based join estimator is disabled. The improvement must preserve transaction isolation, rollback, duplicate suppression, durability configuration, query visibility, reopening persisted data, and the existing public API. Success is visible in a reproducible JMH comparison in which every transaction-per-second benchmark row has candidate throughput greater than or equal to two times its committed baseline score.

The primary tracked TPS contract is the fourteen rows in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/tps.md`: six methods from `TransactionsPerSecondBenchmark` and eight methods from `TransactionsPerSecondBenchmarkFoaf`. The six inherited methods exposed by `TransactionsPerSecondForceSyncBenchmark` are also measured as a durability-bound matrix. They are not present in the tracked `tps.md`, but this plan includes them so that “all TPS benchmarks” is interpreted conservatively rather than silently excluding a class whose name and methods match the goal.

## Progress

- [x] (2026-07-10 17:49Z) Read performance, JFR, comparison, test, and ExecPlan instructions.
- [x] (2026-07-10 17:49Z) Completed the required offline root clean install successfully.
- [x] (2026-07-10 17:49Z) Inventoried fourteen tracked TPS methods and six force-sync methods.
- [x] (2026-07-10 18:15Z) Merged `origin/develop` as `b2cc8fadd7`; resolved sketch-default conflict; ten smoke tests pass.
- [x] (2026-07-10 18:46Z) Captured twenty exact JDK 25 baseline rows and their 2x thresholds.
- [x] (2026-07-10 19:04Z) Profiled small, large, and force-sync representatives with Linux Java 26 JFR.
- [x] (2026-07-11 00:35Z) Implemented direct-addressed packed segments with collision-safe duplicate indexing.
- [x] (2026-07-11 02:35Z) Demonstrated at least 2.00x throughput on all twenty exact acceptance rows.
- [x] (2026-07-11 03:54Z) Completed formatting, final root offline install, and the full 1,586-test LMDB verification with zero failures or errors.

## Surprises & Discoveries

- Observation: The previous scalar scratch-state optimization is not a throughput breakthrough.
  Evidence: Linux Java 26 measured `largerTransactionLevelNone` at 21.192 ± 0.437 ops/s after the change, roughly flat to 1.5% above the stable pre-change iterations, despite about 17% fewer allocation samples per operation.

- Observation: Native persistent work and commit coordination dominate the current profiled transaction.
  Evidence: `profiles/lmdb-scalar-tps/candidate-scratch-reuse/cpu-time-hot-methods.txt` attributes 32.10% to `LmdbSailSink.flush`, 26.87% to native `nmdb_txn_commit`, 22.18% to native `nmdb_cursor_put`, and 6.89% to `Thread.yield0`. Together these account for about 88% of CPU-time samples.

- Observation: The existing three-iteration `tps.md` measurements have very wide error intervals and are unsuitable as the only 2x acceptance evidence.
  Evidence: several error bounds are larger than their point estimates, including the single-statement and ten-statement methods. A new baseline with stable settings must be captured before changing production code.

- Observation: `origin/develop` commit `8d5e6369ac` disables sketches in `ThemeQueryBenchmark` but the branch's sketch-plan smoke tests still need an estimator.
  Evidence: the first merged focused run failed with a null estimator backing store. Resolving the existing property as an explicit opt-in with default `false` made the same focused method pass: one test, zero failures, zero errors.

- Observation: The benchmark helper passes the JMH selector as a regular expression, so an unanchored base method also selects its `LevelNone` suffix variant.
  Evidence: the first attempted `transactions` baseline began a second `transactionsLevelNone` fork. That run was marked invalid and replaced with end-anchored `${method}$` selectors in `profiles/lmdb-tps-double/baseline-b2cc8fadd7-exact/`.

- Observation: Small default-isolation transactions outperform adaptive `NONE` transactions in both data shapes.
  Evidence: synthetic one-statement throughput is 34,279.010 versus 28,203.265 ops/s; FOAF one-statement throughput is 19,709.163 versus 18,021.349 ops/s. This supports a small-transaction handoff/buffering fast-path hypothesis.

- Observation: Large primary rows are stable enough to make their 2x thresholds unambiguous.
  Evidence: synthetic 10,000-statement default throughput is 23.438 ± 0.774 ops/s, FOAF 100,000-statement default is 5.409 ± 0.108 ops/s, and FOAF 100,000-statement `NONE` is 5.706 ± 0.186 ops/s.

- Observation: The memory-overflow spill model originally turned packed exact-statement checks into quadratic work.
  Evidence: a live stack during `transaction100kx` showed `SailSourceModel.contains` repeatedly decoding `PackedSegmentIteration` from the first segment after every spill-model addition. Routing containment through `SailDataset.hasStatements` and the collision-safe fingerprint index restored stable 21.989 and 21.351 ops/s exact results for the two 100,000-statement FOAF rows.

- Observation: A one-statement transaction pays for a 256-row bulk operation and a two-thread handoff.
  Evidence: `profiles/lmdb-tps-double/profiles/baseline-small-transactions.jfr` attributes 60.79% of allocation pressure to `BulkAddQuadsOperation.<init>`. CPU-time samples attribute 30.64% to native commit, 29.24% to `LmdbSailSink.flush`, 12.14% to the background transaction worker, 8.20% to native put, and 6.39% to `Thread.yield0`.

- Observation: Large transactions need a structural native-write improvement rather than the small-operation allocation fix alone.
  Evidence: `profiles/lmdb-tps-double/profiles/baseline-large-none.jfr` attributes 32.07% of CPU time to `LmdbSailSink.flush`, 26.67% to native commit, and 22.55% to native cursor put. `BulkAddQuadsOperation` is only 3.46% of allocation pressure for this workload.

- Observation: Force-sync single-statement throughput is dominated by durable commit wait plus the same handoff and oversized operation costs.
  Evidence: `profiles/lmdb-tps-double/profiles/baseline-force-sync-small.jfr` attributes 61.23% of CPU time to `flush`, 13.24% to yielding, 13.22% to the worker, and 8.70% to native commit; the Docker filesystem score is only 381.589 ± 113.148 ops/s. JFR does not charge the native filesystem wait as CPU time, so force-sync acceptance cannot be inferred from CPU samples alone.

- Observation: Eliminating three of four triple indexes is insufficient to double one-statement throughput.
  Evidence: the temporary one-index diagnostic under `profiles/lmdb-tps-double/diagnostic-one-index/` measured 54,252.770 ± 3,890.594 ops/s, 1.58x the four-index baseline but only 0.79x the 68,558.020 target. The accepted four-index configuration was restored immediately after measurement.

- Observation: Even one canonical index plus inline execution does not double one-statement throughput.
  Evidence: the combined temporary diagnostic under `profiles/lmdb-tps-double/diagnostic-one-index-inline/` measured 63,292.761 ± 2,300.567 ops/s, 1.85x baseline but 7.7% below the 68,558.020 target. The four-index and worker defaults were restored immediately afterward.

- Observation: One shared four-index environment regresses ordinary TPS and improves force-sync without doubling it.
  Evidence: `profiles/lmdb-tps-double/candidate-shared-environment/TransactionsPerSecondBenchmark.transactions.txt` measures 31,885.583 ± 3,037.142 ops/s (0.93x), while the force-sync row measures 3,330.192 ± 470.911 ops/s (1.31x). Combining dirty pages onto one foreground commit removes an fsync but does not remove enough page work.

- Observation: One self-contained LMDB value per transaction segment removes the legacy value-dictionary and multi-index write amplification across all workload sizes.
  Evidence: exact final results range from 2.053x baseline for force-sync `transactionsLevelNone` to 15.329x for force-sync `largerTransactionLevelNone`; all twenty rows are tabulated in `profiles/lmdb-tps-double/candidate-final-exact/README.md`.

- Observation: Duplicate preservation initially erased the packed-layout gain because repeated finite-domain literals triggered full-dataset or full-segment decoding.
  Evidence: the first fingerprint scan candidate fell to 12,589.958 ops/s and a scan cache fell further to 5,314.448 ops/s. Direct quad/value offsets plus a primitive fingerprint-to-ordinal table restored exact duplicate checks and raised the same short probe above 76,000 ops/s.

- Observation: Deferring packed `NONE` writes until flush is required both for throughput and ordered mutation semantics.
  Evidence: pure `NONE` transactions avoid legacy ID arrays and reach 71,922.164 FOAF 1x ops/s, while a pending add followed by a remove can still choose the legacy materialized path; the focused ordering regression test passes.

## Decision Log

- Decision: Use commit `2313cf6a13` as the immutable current-state baseline.
  Rationale: It is the committed and pushed state at the moment the user established this goal, including the earlier scalar scratch reuse and all test fixes.
  Date/Author: 2026-07-10 / Codex.

- Decision: Supersede `2313cf6a13` with the eventual develop merge commit as the immutable measured baseline.
  Rationale: The user explicitly inserted a develop merge before baseline capture. No baseline row had completed, so measuring the resolved merge commit avoids comparing candidates against code no longer present on the branch.
  Date/Author: 2026-07-10 / Codex.

- Decision: Use merge commit `b2cc8fadd7` as the final immutable TPS baseline anchor.
  Rationale: The develop merge and conflict resolution are complete, the affected ten-test smoke class passes, and no production performance candidate has started.
  Date/Author: 2026-07-10 / Codex.

- Decision: Treat this as Routine D and maintain this ExecPlan rather than applying isolated micro-optimizations.
  Rationale: A genuine 2x gain across transaction sizes requires changes to storage-operation count, transaction commit coordination, or representation. Those paths affect IO, concurrency, persistence, and isolation and therefore have high blast radius.
  Date/Author: 2026-07-10 / Codex.

- Decision: Require identical baseline and candidate JVM, benchmark selector, heap, GC, warmup, measurement time, iteration count, fork count, sketch setting, and force-sync setting for each row.
  Rationale: Comparing different harness settings would not prove an implementation gain.
  Date/Author: 2026-07-10 / Codex.

- Decision: Prefer eliminating LMDB operations or safely changing persistent layout before further Java allocation tuning.
  Rationale: the current JFR caps the theoretical benefit of minor Java-only work far below 2x, while native commit and cursor calls dominate.
  Date/Author: 2026-07-10 / Codex.

- Decision: Screen an inline triple-store transaction path before designing adaptive buffering.
  Rationale: disabling the background handoff is the smallest reversible experiment that quantifies the upper bound of removing worker startup, queueing, spinning, and the oversized one-row operation. Small, large, and force-sync benchmarks will determine whether the retained design must be adaptive.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject globally disabling the background transaction worker.
  Rationale: the exact small row improved only from 34,279.010 to 39,848.044 ops/s (1.16x, with high variance), while `largerTransactionLevelNone` fell from 22.669 to 21.066 ops/s (0.93x). Handoff removal is insufficient and sacrifices useful overlap for large writes.
  Date/Author: 2026-07-10 / Codex.

- Decision: Screen the legacy scalar operation path with adaptive bulk buffering disabled.
  Rationale: the small JFR attributes 60.79% of allocation pressure to the 256-row `BulkAddQuadsOperation`. A reversible default-size experiment isolates its throughput contribution before implementing a size-adaptive operation.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject disabling adaptive batching and instead make the batch grow on demand.
  Rationale: bypassing the buffer reduced exact one-statement throughput from 34,279.010 to 28,482.636 ops/s (0.83x). Batched value resolution is beneficial; the waste is reserving 256 rows before knowing that a second row exists.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject the grow-on-demand pending bulk operation.
  Rationale: preserving batched resolution but growing the operation from one row reduced exact one-statement throughput to 30,638.657 ops/s (0.89x). The fixed 256-row buffer is faster despite its allocation footprint, so Java buffer sizing is not the governing bottleneck.
  Date/Author: 2026-07-10 / Codex.

- Decision: Measure a temporary one-index upper bound before changing persistent layout.
  Rationale: the accepted benchmark uses four indexes. A non-acceptance diagnostic with only `spoc` quantifies how much throughput is recoverable by eliminating three native index updates and their dirty pages; the benchmark configuration will be restored immediately afterward.
  Date/Author: 2026-07-10 / Codex.

- Decision: Require the persistent-layout candidate to reduce both index update work and triple transaction cost.
  Rationale: the one-index upper bound reaches only 1.58x. A layout that merely batches secondary writes cannot clear the small-row target; it must also reduce dirty-page/commit work, while large rows still require batched native storage.
  Date/Author: 2026-07-10 / Codex.

- Decision: Measure the combined one-index and inline upper bound.
  Rationale: the isolated four-index inline experiment saved about four microseconds per one-row transaction, while one index reduced total latency to about 18.4 microseconds. Their combined diagnostic determines whether a compact unified index plus adaptive inline path has enough headroom for the 14.6-microsecond target.
  Date/Author: 2026-07-10 / Codex.

- Decision: Unify value and triple writes into one LMDB environment for newly created stores while retaining the legacy two-environment open path.
  Rationale: one index plus inline execution still misses the target, and force-sync currently pays two independent durable commits. A shared write transaction removes one native commit/fsync and makes value/triple visibility atomic. Existing stores need the current physical layout for compatibility.
  Date/Author: 2026-07-10 / Codex.

- Decision: Screen `MDB_WRITEMAP` only for shared force-sync stores.
  Rationale: the shared durable row remains dominated by synchronous dirty-page work. Write-mapped LMDB retains synchronous commit semantics while avoiding copy/writeback overhead; the experiment does not alter non-sync stores and will be rejected if it cannot approach the force-sync target or breaks tests.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject shared force-sync `MDB_WRITEMAP`.
  Rationale: it measured 3,453.259 ± 1,455.390 ops/s, only about 4% above the non-write-mapped shared result and far below the 5,070.624 target. The marginal, noisy gain does not justify retaining the mode.
  Date/Author: 2026-07-10 / Codex.

- Decision: Implement fixed-duplicate statement indexes for fresh shared stores.
  Rationale: one constant key per logical index with 32-byte sorted quad duplicates keeps range-seek semantics, compresses B-tree key structure, and permits `MDB_MULTIPLE` to replace per-row cursor calls in aligned batches. Legacy stores and value-store triple-term indexes keep their current ordinary-key format.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject and remove both the shared-environment and fixed-duplicate-index experiments.
  Rationale: shared environments did not double small or force-sync TPS; fixed duplicates complicated indexing and one native probe crashed. Neither is needed by the final packed-segment design.
  Date/Author: 2026-07-11 / Codex.

- Decision: Retain self-contained append-only packed segments with quads first, direct value offsets, and a collision-safe primitive fingerprint index.
  Rationale: one LMDB entry per segment reduces native write and dirty-page work, direct offsets make exact duplicate checks O(1) in segment size, and dynamic fallback to the standard evaluation strategy preserves query visibility without materializing legacy indexes.
  Date/Author: 2026-07-11 / Codex.

- Decision: Route spill-model containment through `SailDataset.hasStatements` and use the packed fingerprint index for fully bound statement patterns.
  Rationale: `contains` only needs existence, and opening a decoding iterator for every exact probe caused O(n²) work in large FOAF transactions. Fingerprint lookup remains collision-safe because candidate ordinals are verified against the encoded statement bytes.
  Date/Author: 2026-07-11 / Codex.

## Outcomes & Retrospective

The performance objective is achieved. All twenty final exact JMH point estimates exceed twice the `b2cc8fadd7` baseline using identical JDK, heap, GC, fork, warmup, measurement, and sketch settings. Final ratios range from 2.377x to 14.798x; the raw tables and consolidated comparison are under `profiles/lmdb-tps-double/final-current-exact/`. The retained implementation stores transaction-owned values and quads in self-contained append-only segments, uses direct offsets for duplicate comparison, maintains a primitive collision-safe fingerprint index, and routes evaluation through the standard strategy while packed segments exist. Exact spill-model probes use the same collision-safe index rather than rescanning segments. Rejected shared-environment and fixed-duplicate experiments were removed. Formatting, the final root offline install, and the complete `core/sail/lmdb` verification all pass; Maven reports 1,473 unit tests and 113 integration tests, for 1,586 total with zero failures, zero errors, and three skipped.

## Context and Orientation

The Maven module is `core/sail/lmdb`. `TransactionsPerSecondBenchmark.java` opens a fresh LMDB repository for every JMH iteration with `ConfigUtil.createConfig().setForceSync(forceSync).setSketchEstimatorEnabled(false)`. Its methods commit one, ten, ten thousand, or one million synthetic statements. The `NONE` variants request `IsolationLevels.NONE`; the unqualified variants use the repository default isolation level. `TransactionsPerSecondForceSyncBenchmark.java` inherits the same six methods and sets `forceSync=true` before setup.

`TransactionsPerSecondBenchmarkFoaf.java` extends `BenchmarkBaseFoaf.java`. That base also constructs `LmdbStore` with the sketch estimator disabled. Its eight methods commit one or ten FOAF statements, one thousand ten-statement people, or ten thousand ten-statement people, with default and `NONE` isolation variants.

Scalar repository additions flow through `SailRepositoryConnection`, `SailSourceConnection`, and eventually the `LmdbSailSink` nested inside `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`. The sink resolves RDF values through `ValueStore`, accumulates native statement IDs, writes indexes through `TripleStore`, and coordinates an LMDB transaction through `TxnManager`. A cursor write is a native LMDB insertion into one database/index. A commit makes the transaction visible and, when force-sync is enabled, durable according to the configured LMDB synchronization semantics.

The branch also contains a packed fresh-load format optimized for iterable bulk loading. The TPS benchmarks repeatedly mutate a growing store across many transactions. Reusing the fresh-load format is acceptable only if duplicate detection, deletion, query visibility, recovery, and bounded memory remain correct across multiple segments; merely deferring work past the measured invocation is not an optimization.

## Plan of Work

First, capture the current committed matrix before touching production code. Build the JMH jar once, run each method with identical stable settings, preserve raw results under `profiles/lmdb-tps-double/baseline-2313cf6a13/`, and produce a machine-readable comparison basis. Run each method separately so an outlier or long million-statement invocation cannot contaminate another row. Repeat noisy rows until their central score is stable enough that a 2x threshold is meaningful.

Second, profile representative workload shapes using the Docker/Linux Java 26 JFR loop: single statement, ten statements, ten thousand statements, a large FOAF transaction, and a force-sync single statement. Read CPU-time methods, stack samples, lost samples, allocation pressure, parks, and file-force events. Trace the dominant stacks into `LmdbSailStore`, `TripleStore`, `ValueStore`, and `TxnManager`. Estimate each candidate’s maximum end-to-end upside before editing.

Third, implement one candidate at a time. Candidates must reduce the number or cost of native cursor writes, redundant dictionary/index operations, duplicate checks, transaction handoffs, or commit waits while preserving semantics. Each candidate receives focused correctness coverage and a representative benchmark rerun. Reject candidates that change error timing, visibility, isolation, durability, persistent compatibility, or merely move work outside the measured interval.

Fourth, after representative methods cross 2x, run the entire twenty-row matrix with the exact baseline settings. Use the JMH comparison utility to compute score deltas and ratios. Any row below 2.00x becomes the next active bottleneck and is profiled separately. Continue the loop until every primary and force-sync row satisfies the goal.

Finally, run focused tests, the complete `core/sail/lmdb` verification, copyright validation, formatting, and the root offline clean install. Audit tracked and untracked files without deleting user artifacts. Record exact benchmark commands, results, JFR paths, test summaries, retained/rejected candidates, and confidence here.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j`.

The already completed sanity install was:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Build or run one benchmark with the supported helper:

    scripts/run-single-benchmark.sh \
      --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.TransactionsPerSecondBenchmark \
      --method largerTransactionLevelNone \
      --warmup-iterations 2 \
      --measurement-iterations 5 \
      --forks 1

The six synthetic method names are `transactions`, `transactionsLevelNone`, `mediumTransactionsLevelNone`, `largerTransaction`, `largerTransactionLevelNone`, and `veryLargerTransactionLevelNone`. The eight FOAF method names are `transaction1x`, `transaction1xLevelNone`, `transaction10x`, `transaction10xLevelNone`, `transaction10kx`, `transaction10kxLevelNone`, `transaction100kx`, and `transaction100kxLevelNone`. Repeat the six synthetic selectors with class `TransactionsPerSecondForceSyncBenchmark` for the force-sync matrix.

Profile a fixed selector with Linux Java 26 and CPU-time JFR:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
      --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.TransactionsPerSecondBenchmark \
      --method largerTransactionLevelNone

Read each emitted recording with:

    jfr summary <recording.jfr>
    jfr view cpu-time-hot-methods <recording.jfr>
    jfr print --events jdk.CPUTimeSample,jdk.CPUTimeSamplesLost --stack-depth 20 <recording.jfr>

Compare a baseline and candidate result with:

    python3 .codex/skills/jmh-benchmark-compare/scripts/jmh_benchmark_compare.py \
      <baseline-result.txt> <candidate-result.txt> \
      --score-direction higher \
      --export-formats md,csv \
      --output-dir profiles/lmdb-tps-double/compare \
      --output-base candidate-vs-baseline

Run tests without `-am` and without `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py <FocusedTestClass#method> --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before finalizing:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

The formatter uses `-q` because it is not a test. Tests must never use `-q` or `-am`.

## Validation and Acceptance

Performance acceptance requires a candidate score divided by its matching committed-baseline score of at least 2.00 for every method in the fourteen-row tracked TPS contract and the six-row force-sync matrix. Point estimates must come from identical settings. A row with extreme variance must be repeated; success cannot rest on a single outlier or overlapping results that make 2x implausible. Sketches must remain disabled in both benchmark setup paths.

Correctness acceptance requires tests covering scalar and batched adds, duplicates, add/remove ordering, commit, rollback, reopen, concurrent readers, default isolation, `NONE` isolation, force-sync configuration, and persistent compatibility. All `core/sail/lmdb` Surefire and Failsafe tests must pass. The full root quick install must end with `BUILD SUCCESS`.

The change must not gain benchmark throughput by weakening force-sync behavior, skipping indexes, suppressing exceptions, losing statements, changing public APIs, disabling validation, delaying required work until teardown, or special-casing benchmark classes.

## Idempotence and Recovery

Baseline and candidate artifacts use separate directories and can be regenerated safely. Never overwrite the committed baseline after production edits. If a benchmark fails transiently, retry the exact command with unchanged settings. If Docker or Maven fails because an artifact is missing offline, rerun the exact command once with network access and then return offline. Reject a candidate with a narrow `apply_patch` reversal; never use `git reset`, `git restore`, `git clean`, or delete untracked plans and profiles.

## Artifacts and Notes

The pre-goal scalar profile is under `profiles/lmdb-scalar-tps/`. Immutable exact baseline artifacts and the score/target table are under `profiles/lmdb-tps-double/baseline-b2cc8fadd7-exact/`; the earlier unanchored attempt is invalid and retained separately. Candidate iterations belong in sibling directories named for the hypothesis. Test logs remain under `logs/mvnf/`. The initial root build log is `maven-build.log` and completed with `BUILD SUCCESS` in 2:46 wall-clock time.

## Interfaces and Dependencies

No public API or new external dependency is expected. Production work should remain within existing `LmdbSailStore`, `TripleStore`, `ValueStore`, `TxnManager`, and their private/package-private helpers unless evidence proves a narrower upstream layer is responsible. New internal state must have explicit ownership, bounded memory, and reset behavior on commit, rollback, failure, and close. Persistent representation changes must define versioning or compatibility with existing stores and must be exercised by reopen tests.

Revision note (2026-07-10 17:49Z): Created the self-contained plan for the new 2x goal, established commit `2313cf6a13` as the initial baseline anchor, included all tracked TPS rows plus the force-sync subclass, and recorded the prior JFR evidence that points to native operation and commit-path costs.

Revision note (2026-07-10 18:13Z): Recorded the develop merge side quest, the sketch-default conflict and focused failure/pass evidence, and the decision to re-anchor the immutable TPS baseline to the completed merge commit because baseline capture had not yet produced a row.

Revision note (2026-07-10 18:15Z): Marked the develop merge complete at `b2cc8fadd7`, recorded the ten-test green smoke result, and replaced provisional baseline paths and acceptance wording with the resolved merge commit.

Revision note (2026-07-10 18:46Z): Recorded all twenty exact baseline rows, the regex-selector correction, the small-transaction isolation pattern, stable large-transaction thresholds, and the immutable baseline artifact location.

Revision note (2026-07-10 19:04Z): Recorded the small, large, and force-sync JFR evidence and selected an inline-transaction screening experiment before implementing an adaptive fast path.

Revision note (2026-07-10 19:10Z): Rejected the global inline experiment after exact small and large measurements; preserved results under `profiles/lmdb-tps-double/candidate-inline-all/` and restored the production default.

Revision note (2026-07-10 19:18Z): Rejected disabling adaptive batching after an exact one-row regression; preserved the result under `profiles/lmdb-tps-double/candidate-no-bulk/` and selected an in-place grow-on-demand buffer.

Revision note (2026-07-10 19:26Z): Rejected and removed grow-on-demand buffering after its exact small-row regression; preserved the result under `profiles/lmdb-tps-double/candidate-grow-on-demand/`.

Revision note (2026-07-10 19:30Z): Recorded the temporary one-index 1.58x upper bound, restored the accepted four-index configuration, and established that the retained layout must reduce both index writes and commit work.

Revision note (2026-07-10 19:35Z): Recorded the combined one-index/inline 1.85x upper bound, restored both defaults, and selected a compatibility-preserving shared LMDB environment as the next implementation milestone.

Revision note (2026-07-11 02:45Z): Recorded the retained direct-addressed packed-segment design, rejected and removed the shared/fixed-index experiments, and documented all twenty exact passing TPS ratios under `profiles/lmdb-tps-double/candidate-final-exact/`.

Revision note (2026-07-11 03:54Z): Recorded the spill-model quadratic exact-lookup diagnosis and fix, refreshed all twenty final exact rows under `profiles/lmdb-tps-double/final-current-exact/`, and completed the final 1,586-test zero-failure verification.
