# Optimize LMDB scalar transaction throughput

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current while the work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J applications that build transactions by repeatedly calling `RepositoryConnection.add(Resource, IRI, Value)` should retain LMDB transaction semantics while completing more such transactions per second. The branch already accelerates `RepositoryConnection.add(Iterable)` bulk loads, but the scalar TPS benchmarks do not enter that iterable path. This work identifies the dominant scalar-write cost, implements one minimal optimization at the correct LMDB layer, proves semantic compatibility with focused tests, and demonstrates the effect with the same JMH selector before and after the change.

## Progress

- [x] (2026-07-10 12:45Z) Confirmed that the TPS benchmark uses scalar `connection.add(...)`, while the 10x loader uses `connection.add(Iterable)`.
- [x] (2026-07-10 12:45Z) Selected `TransactionsPerSecondBenchmark.largerTransactionLevelNone` as the fixed profiling selector because each invocation performs 10,000 scalar adds and one commit.
- [x] (2026-07-10 13:35Z) Captured the Linux Java 26 Docker/JFR baseline at 19.041 ± 8.864 ops/s; nine iterations clustered near 20-23 ops/s and one outlier measured 2.564 ops/s.
- [x] (2026-07-10 13:35Z) Read 18,417 CPU-time samples and 318 lost samples; native triple-store commit, cursor writes, and commit-wait spinning dominate CPU samples.
- [x] (2026-07-10 14:05Z) Implemented reusable primitive scalar-resolution scratch state without changing public APIs or bulk ingestion.
- [x] (2026-07-10 14:05Z) Passed six focused scalar transaction tests; the broader 40-test class has one isolated pre-existing query-explanation failure unrelated to this path.
- [x] (2026-07-10 14:05Z) Repeated the identical Docker/JFR selector: candidate 21.192 ± 0.437 ops/s, roughly flat to 1.5% above the baseline's nine stable iterations, with approximately 17% fewer allocation samples per operation.
- [x] (2026-07-10 14:38Z) Passed copyright/SPDX validation, formatting, final root clean install, post-install six-test verify, and diff audit; completed the retrospective.

## Surprises & Discoveries

- Observation: The existing TPS result files are too noisy for a strong acceptance claim. The latest table has 13 of 14 favorable point estimates, but none of the current/main reported intervals are disjoint.
  Evidence: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/tps.md` and `tps-main.md` each contain only three measurements per benchmark.

- Observation: The packed bulk-load implementation cannot explain scalar TPS movement.
  Evidence: `TransactionsPerSecondBenchmark` calls scalar `connection.add(...)`; `DatagovLoadIsolationBenchmark.loadOnce` calls `connection.add(data)`. Only the latter reaches `SailRepositoryConnection.addWithoutCommit(Iterable)`, `SailSourceConnection.addStatementsInternal`, `LmdbSailSink.approveAll`, and packed statement buffering.

- Observation: The scalar path is already adaptively batched, so the profile is dominated by persistent LMDB work rather than per-call Sail dispatch.
  Evidence: The Java 26 CPU-time view reports `LmdbSailSink.flush` at 32.00%, native `nmdb_txn_commit` at 26.11%, native `nmdb_cursor_put` at 22.93%, and `Thread.yield0` at 6.71%. Value lookup, hashing, and encoding methods are individually near or below 1%.

- Observation: The scalar resolution chunks allocate replaceable scratch state even though each `LmdbSailSink` owns the work serially under `sinkStoreAccessLock`.
  Evidence: The JFR allocation view attributes 7.60% to `int[]`, 6.31% to `HashMap$Node`, 4.23% to boxed `Integer`, and 2.09% to `Value[]`. `resolvePendingApproveValues` creates four index arrays, one value array, and a boxed `HashMap<Value,Integer>` for every chunk.

- Observation: Reusing the fresh-load packed representation is not a safe steady-state TPS optimization.
  Evidence: The benchmark performs repeated commits into one growing store. The packed format is retained only for a fresh load; a subsequent mutation materializes it into legacy indexes. Enabling it for the first TPS invocation would shift extra materialization work into the next invocation instead of accelerating the steady state.

- Observation: The allocation candidate moved its intended evidence without moving native hotspots.
  Evidence: `HashMap$Node` fell from 6.31% allocation pressure to below the candidate report cutoff, boxed `Integer` from 4.23% to 0.80%, `Value[]` from 2.09% to below the cutoff, and `int[]` from 7.60% to 5.45%. CPU-time shares for flush, native commit, cursor put, and yield remained approximately 32%, 26-27%, 22-23%, and 7%.

- Observation: The complete `LmdbSailStoreTest` class is not green on this branch for a query-explanation reason.
  Evidence: 39 of 40 methods pass. `testExplainExecutedShowsIndexName` fails both in the class and in isolation because it expects `index=` while the native plan reports `NativeRows(...)`. The scalar candidate does not touch optimizer or explanation code.

## Decision Log

- Decision: Treat this as a significant IO-path optimization under Routine D rather than a behavior-neutral micro-refactor.
  Rationale: Scalar transaction writes touch LMDB native IO, transaction ordering, duplicate semantics, rollback, isolation, and persistent state. A local-looking shortcut can therefore have a large semantic blast radius.
  Date/Author: 2026-07-10 / Codex.

- Decision: Profile `org.eclipse.rdf4j.sail.lmdb.benchmark.TransactionsPerSecondBenchmark.largerTransactionLevelNone` and keep that selector fixed through the candidate run.
  Rationale: It exercises 10,000 scalar additions per transaction with `IsolationLevels.NONE`, amortizes JMH invocation overhead, and avoids the multi-second million-statement invocation that can reduce sampling responsiveness.
  Date/Author: 2026-07-10 / Codex.

- Decision: Prefer an algorithmic or operation-count reduction over Java loop syntax changes.
  Rationale: The hot path crosses repository, Sail, LMDB value dictionary, triple indexes, and native LMDB calls. Removing repeated work or batching an internal operation has materially more upside than cosmetic source reshaping.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject packed-segment reuse for repeated scalar transactions and retain legacy index semantics.
  Rationale: Supporting multiple immutable packed transaction segments would require new cross-segment duplicate, deletion, promotion, query, and compaction semantics. The existing single fresh segment intentionally materializes before later mutation. Extending that representation is a separate storage-engine project, not a surgical scalar-method optimization.
  Date/Author: 2026-07-10 / Codex.

- Decision: Implement one allocation-focused candidate by reusing scalar resolution arrays and using `ObjectIntHashMap<Value>` scratch lookup.
  Rationale: This removes boxed entries and repeated chunk arrays from a directly measured allocation source, uses an existing dependency already employed by packed loading, stays transaction-local, and does not alter native operation ordering. The expected throughput upside is modest because native commit and cursor work remain dominant; retention depends on benchmark and allocation evidence.
  Date/Author: 2026-07-10 / Codex.

## Outcomes & Retrospective

The retained production change replaces per-resolution-chunk boxed maps and throwaway index/value arrays with scratch state owned by one `LmdbSailSink`. Six directly relevant semantic tests pass. The candidate benchmark is 21.192 ± 0.437 ops/s; compared with the baseline's nine stable iterations it is approximately 1.5% higher, while allocation samples normalized by completed operations are approximately 17% lower. The intended `HashMap$Node`, boxed `Integer`, `Value[]`, and `int[]` sources all fall in the candidate allocation view. Native commit and cursor hotspots do not move, so this checkpoint is an allocation win with flat-to-small throughput improvement, not a large TPS breakthrough. The candidate is retained because it is bounded, reversible, uses an existing primitive collection, and removes measured allocation without changing public or persistent semantics. JIT evidence was not inspected because the changed lookup is only 0.14% of candidate CPU-time samples and native LMDB work dominates; low-level throughput confidence remains low, while allocation confidence is medium.

## Context and Orientation

The Maven module is `core/sail/lmdb`. The benchmark class `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/TransactionsPerSecondBenchmark.java` opens an LMDB repository at iteration scope. Its `largerTransactionLevelNone` method begins one transaction at isolation level `NONE`, performs 10,000 scalar `connection.add(subject, predicate, object)` calls, and commits.

The scalar repository call is implemented by `core/repository/sail/src/main/java/org/eclipse/rdf4j/repository/sail/SailRepositoryConnection.java`. It calls `SailConnection.addStatement`. The base Sail implementation in `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceConnection.java` maintains a transaction dataset and sink, records the statement, and eventually flushes changes into `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`. LMDB dictionary and index work continues through `ValueStore.java`, `TripleStore.java`, `TxnManager.java`, and their helpers. The JFR baseline determines which of these components deserves a change.

The existing 10x path is intentionally separate. Iterable additions can reach `LmdbSailSink.approveAll`, create `PackedStatementList`, and persist packed blocks when the store is fresh and at least `TripleStore.PACKED_BULK_MIN_STATEMENTS` statements are available. The scalar TPS method never presents an iterable, so this plan must not claim reuse of that path unless a future design can preserve streaming scalar-call semantics without buffering unbounded user data or changing when errors become observable.

## Plan of Work

First, run the repository-required clean quick install and then the Docker/JFR wrapper against the fixed selector. Preserve the benchmark output and `.jfr` path. Read the recording with `jfr summary`, `jfr view cpu-time-hot-methods`, and a bounded `jfr print` for CPU-time samples and lost samples. If CPU is not the limiting resource, inspect lock, allocation, or native wait evidence instead of forcing a CPU hypothesis.

Second, trace the hottest repeated stack into the smallest responsible production method. Estimate its maximum end-to-end upside from its sample share. Choose one candidate that reduces native operations, repeated hashing/encoding, allocation, copying, locking, or generic dispatch. Do not alter public APIs. Do not redirect scalar calls into the packed iterable path unless error timing, listener behavior, duplicate handling, isolation, rollback, and transaction memory bounds can all be preserved.

Third, implement the candidate in a small patch. Add or extend focused semantic coverage before relying on the candidate. Because this is Routine D, validation is milestone-based rather than a mandatory failing-test-first sequence, but production code is not accepted until focused transaction tests and the module verification pass.

Fourth, rebuild and repeat the exact Docker/JFR command. Compare benchmark throughput and CPU hotspot share. Retain only a candidate that is repeatably faster or has a strong profile-backed operation-count improvement with no semantic regression. If the benchmark is flat or slower, revert only the candidate patch while preserving all user-owned and untracked artifacts.

Finally, run the repository formatter, copyright check for touched sources, root quick install, final status, and diff audit. Update every living section in this ExecPlan with evidence and a clear retain/reject decision.

## Concrete Steps

Run from `/Users/havardottestad/Documents/Programming/rdf4j`.

The repository sanity install is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

The fixed baseline and candidate command is:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
      --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.TransactionsPerSecondBenchmark \
      --method largerTransactionLevelNone

After the wrapper prints the recording path, inspect it with:

    jfr summary <recording.jfr>
    jfr view cpu-time-hot-methods <recording.jfr>
    jfr print --events jdk.CPUTimeSample,jdk.CPUTimeSamplesLost --stack-depth 20 <recording.jfr>

Use the repository test runner for focused and module tests. Never add `-am` or `-q` to test commands:

    python3 .codex/skills/mvnf/scripts/mvnf.py <FocusedTestClass#method> --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before finalizing touched Java files:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command contains `-q` because it is not a test run. Test commands must remain free of `-q` and `-am`.

## Validation and Acceptance

Functional acceptance requires focused tests covering scalar adds, duplicate statements, commit, rollback, reopening persistent data, and the affected isolation behavior. The `core/sail/lmdb` module verification must finish with no new Surefire or Failsafe failures.

Performance acceptance requires the candidate to use the identical Docker image, Java 26 runtime, selector, JFR settings, warmup count, measurement count, and fork count as the baseline. Report both ops/s and the dominant CPU-time stacks. A small single-run gain is low-confidence. Prefer a repeated candidate/control sequence if the first delta is within ordinary run noise. Reject a candidate that slows the fixed selector materially, shifts cost into GC or locks, or accelerates only by changing observable transaction behavior.

The bulk-load benchmark must remain healthy because the scalar optimization must not disable or materialize the packed iterable path.

## Idempotence and Recovery

All inspection and benchmark commands are repeatable. Docker/JFR outputs must use distinct generated paths and must not overwrite user artifacts. Preserve all untracked files. If Maven offline resolution fails for a missing artifact, rerun the exact install once without `-o`, then return to offline operation. If a benchmark or Docker command fails transiently, retry it once with the same selector and arguments. If a candidate is rejected, remove only lines introduced by that candidate with `apply_patch`; never use `git reset`, `git restore`, `git clean`, or broad deletion.

## Artifacts and Notes

The prior TPS comparison is `/private/tmp/lmdb-tps-current-vs-main.md`. The repository build log is `maven-build.log`, and compact pre-change evidence is `initial-evidence.txt`. The baseline recording is `profiles/lmdb-scalar-tps/baseline/baseline.jfr`; its summary, CPU-time hot-method view, and expanded CPU-time samples are in the same directory. Candidate artifacts are under `profiles/lmdb-scalar-tps/candidate-scratch-reuse/`. The checkpoint summary is `profiles/lmdb-scalar-tps/README.md`. The final focused-test log is `logs/mvnf/20260710-scalar-scratch-final-verify.log`, the compact report is `profiles/lmdb-scalar-tps/final-evidence.txt`, and the Surefire report records six passing tests. The final root quick clean install passed in 2:20 wall-clock time.

## Interfaces and Dependencies

No new external dependency or public API is expected. Production changes should remain inside existing RDF4J repository/Sail/LMDB interfaces. Candidate helper state must have an explicit transaction owner and reset on commit, rollback, close, and failure. Any cache must preserve RDF value equality, revision semantics, and memory bounds. Any internal batching must preserve statement listeners, duplicate handling, context handling, update ordering, isolation, and the point at which user-visible exceptions are raised.

Revision note (2026-07-10 12:45Z): Created the initial self-contained plan after confirming that scalar TPS bypasses iterable packed ingestion. The implementation hypothesis remained intentionally open until the baseline JFR identified a material hotspot.

Revision note (2026-07-10 13:35Z): Added the baseline score and JFR attribution, rejected packed-segment reuse for repeated TPS transactions, selected transaction-owned primitive scratch reuse as the first bounded candidate, and recorded focused pre-change coverage.

Revision note (2026-07-10 14:05Z): Recorded the retained implementation, candidate benchmark and allocation evidence, six-test semantic selection, deterministic unrelated class failure, artifact locations, and current outcome/confidence.

Revision note (2026-07-10 14:38Z): Completed copyright, formatting, clean-install, post-install focused verification, evidence preservation, and final scope audit. Removed the formatter's incidental wrap of a pre-existing benchmark line so the tracked production diff remains task-local.
