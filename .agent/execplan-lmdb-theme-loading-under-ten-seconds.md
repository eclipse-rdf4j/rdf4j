# Query-ready theme loading under ten seconds

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

## Purpose / Big Picture

Add a JMH benchmark that rebuilds the LMDB data used by `ThemeQueryBenchmark`, then make the sketches-disabled load complete in less than ten seconds without weakening the query-ready four-index contract. A successful run leaves the ordinary `spoc`, `ospc`, `psoc`, and `posc` indexes and normal value store fully committed; store creation and deletion are excluded from the timed operation, while repository initialization, generated RDF ingestion, value persistence, index construction, and commit remain timed just as they are in `ThemeQueryBenchmark`.

## Progress

- [x] (2026-07-12 09:02 +02:00) Added `ThemeDataLoadingBenchmark` and shared its loader with `ThemeQueryBenchmark`.
- [x] (2026-07-12 09:04 +02:00) Captured a local JDK 25 baseline of 39.472 s/op and a timed diagnostic run of 39.127 s/op.
- [x] (2026-07-12 09:06 +02:00) Captured an async-profiler CPU profile and phase timings.
- [x] (2026-07-12 09:18 +02:00) Added focused contracts for large prepared imports, proactive map growth, compatible value assignment, global index ordering, and duplicate filtering.
- [x] (2026-07-12 09:25 +02:00) Reserved value and triple LMDB capacity before starting their fresh write transactions, retaining record-cache recovery for other paths.
- [x] (2026-07-12 09:31 +02:00) Added bounded fresh-value encoding and globally sorted four-index construction for imports larger than the prepared-operation budget.
- [x] (2026-07-12 09:40 +02:00) Re-ran focused value compatibility, exact index ordering, map growth, arena cleanup, prepared-batch, and query-readiness tests.
- [x] (2026-07-12 09:33 +02:00) Passed five Docker Linux JDK 26 measurements at 8.813--9.536 s/op after one warmup.
- [x] (2026-07-12 13:20 +02:00) Passed final local JDK 25 and Docker Linux JDK 26 five-measurement gates and the complete 1,479-test `core/sail/lmdb` suite.

## Surprises & Discoveries

- The exact workload is much larger than datagov: 13,823,295 statements and 4,741,090 equality-distinct values.
- The new prepared representation is built in 3.077 seconds, so RDF generation and scalar transaction buffering are not the dominant regression.
- The configured triple map starts at 1 GiB, but the finished `triples/data.mdb` is about 2.4 GiB. The write therefore enters `TxnRecordCache`; replay after the first commit costs 21.829 seconds by itself.
- The oversized batch fails the prepared-import budget check and falls back to the normal value-loading path. Value work costs 7.151 seconds and the pre-commit prepared-store phase costs 13.757 seconds in total.
- The async-profiler CPU profile at `profiles/lmdb-theme-load/cpu/java-command-cpu-91024.txt` attributes 44.89% of samples to LMDB native code and shows `TxnRecordCache.getRecordState` and `TripleStore.updateFromCache` in the hot path, corroborating the phase trace.
- Proactive map reservation eliminated transaction replay and reduced the benchmark from 39.127 to 17.831 s/op, proving map growth was the largest single pathology.
- Bounded fresh-value persistence and global four-index construction reduced the local score to 10.342 s/op, but the first Docker gate still measured 10.337--10.705 s/op.
- A Linux JDK 26 JFR profile at `profiles/lmdb-theme-load/linux-jdk26.jfr` attributed 42.02% of sampled Java CPU to `radixSortGlobalField` and `globalFieldRank`. The accessor repeated checked multiplication, a character switch, and long-width radix work for every key visit.
- Resolving each field to an integer offset once and sorting the transaction-local ranks as 32-bit integers reduced a cold local invocation to 8.759 s/op and every measured Docker invocation to less than ten seconds.

## Decision Log

- Decision: benchmark `Mode.SingleShotTime` with invocation-level fresh-store setup and teardown.
  Rationale: a load is one long operation; JMH setup/teardown keeps directory creation, shutdown, and deletion outside the score.
  Date/Author: 2026-07-12 / Codex.
- Decision: call one package-private loader from both theme benchmarks.
  Rationale: this proves the data set, isolation level, transaction boundary, and insertion API cannot drift between the query and loading benchmarks.
  Date/Author: 2026-07-12 / Codex.
- Decision: fix LMDB growth and the oversized fresh-import fallback rather than increasing only the benchmark configuration.
  Rationale: a larger benchmark map would hide the generic auto-grow pathology and would not satisfy the fresh-import design requirement to split inputs at the memory budget.
  Date/Author: 2026-07-12 / Codex.
- Decision: assign all compatible fresh IDs first, then encode value records in bounded batches while the triple writer builds globally ordered indexes.
  Rationale: retaining encoded payloads for 4.7 million values exceeded the bounded-import budget, while early ID assignment preserves exact ordering and lets the separate LMDB environments work concurrently.
  Date/Author: 2026-07-12 / Codex.
- Decision: rank the distinct long IDs once and use 32-bit local ranks for all four stable index sorts.
  Rationale: rank comparison is order-equivalent to long-ID comparison, avoids repeated long-key work, and keeps ordinary LMDB key bytes and query compatibility unchanged.
  Date/Author: 2026-07-12 / Codex.

## Outcomes & Retrospective

The implementation reduced the 39.127 s/op diagnostic baseline to a final mean of 8.731 s/op on local JDK 25 and 8.761 s/op on Docker Linux JDK 26. All ten final measurements passed: local values were 9.036, 9.070, 9.148, 8.413, and 7.989 s/op; Docker values were 8.842, 8.815, 8.798, 9.026, and 8.322 s/op. The complete `core/sail/lmdb` verification passed 1,479 tests with zero failures and zero errors (three skipped). Temporary phase instrumentation was removed. The side quest is complete.

## Context and Orientation

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java` owns the established load workload. It calls `ThemeDataSetGenerator.generate` for every `Theme` through one `RDFInserter` in one `IsolationLevels.NONE` transaction. `ThemeDataLoadingBenchmark.java` times that shared method on a fresh four-index store with sketches disabled.

Scalar repository additions are coalesced by `Changeset` into `PreparedStatementBatch`. At flush, `LmdbSailStore.LmdbSailSink` resolves values and submits primitive quad arrays to `TripleStore`. `ValueStore.FreshValueSession` is the empty-store path that can assign compatible IDs without repeated database lookups. `TripleStore` owns the single LMDB writer and uses `TxnRecordCache` only as the recovery path when an active write transaction runs out of map space.

## Plan of Work

First, add narrow tests that characterize two missing invariants: an oversized prepared import remains on the fresh-value path while being divided into bounded operations, and a large fresh write reserves enough map capacity before the writer begins so it does not spill into `TxnRecordCache`. Keep existing tests for duplicate filtering, inline-ID vectors, triple terms, hash collisions, rollback, reopen, worker failure, and query-ready indexes unchanged.

Second, give `TripleStore` a conservative capacity estimator based on current LMDB page use, statement count, active ordinary-index count, maximum encoded key size, and B-tree entry overhead. `LmdbSailStore` will provide the pending prepared statement count before the writer transaction starts. Grow the map under the existing transaction-manager write lock, with readers deactivated, and preserve the existing record-cache auto-grow fallback for estimates that are insufficient or for non-prepared mutations.

Third, replace the all-or-nothing prepared-import budget check. Traverse the prepared local-ID quads in bounded chunks. Resolve each previously unseen local value once through the existing `FreshValueSession`, fill one primitive quad operation, start its secondary-index workers, submit it to the single triple writer, and persist that chunk's prepared value records in the value environment. Continue while the previous triple operation runs so value persistence, index encoding, and triple writes overlap. Retain one transaction-wide local-ID-to-long-ID array, reuse the fresh session across chunks, and publish caches only after both LMDB environments commit.

Finally, run the smallest correctness selections, the complete `core/sail/lmdb` suite through `mvnf`, and the new benchmark with one warmup and at least five single-shot measurements. Repeat on local JDK 25 and Docker Linux JDK 26. A result passes only when every sketches-disabled sample is below ten seconds and a post-load SPARQL query reads the committed ordinary indexes without changing LMDB transaction IDs.

## Concrete Steps

From the repository root:

    python3 .codex/skills/mvnf/scripts/mvnf.py <focused-test> --retain-logs
    ./scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeDataLoadingBenchmark --method loadThemeDatasets --warmup-iterations 1 --measurement-iterations 5 --forks 1

Use `-Drdf4j.lmdb.loadTiming=true` only for diagnostic runs and remove the temporary instrumentation before final verification. Append compact test evidence to `initial-evidence.txt`; never overwrite it.

## Validation and Acceptance

Correctness acceptance requires all four ordinary indexes to contain the committed statement count, duplicate additions to remain filtered, literal inline IDs to stay bit-identical, rollback/reopen to work, and first-query evaluation to be read-only. Performance acceptance requires every individual local JDK 25 and Docker Linux JDK 26 measurement of `ThemeDataLoadingBenchmark.loadThemeDatasets` with sketches disabled to be less than 10.000 seconds.

## Idempotence and Recovery

The benchmark creates a new child directory per invocation and deletes it after repository shutdown, so interrupted runs leave only removable directories under `core/sail/lmdb/target` or `target`. Proactive map growth is advisory: the existing `TxnRecordCache` recovery remains authoritative if the estimate is too small. A failed prepared chunk cancels its worker tasks, rolls back both environments, and discards only provisional fresh-session entries.

## Artifacts and Notes

The current JMH output is retained in `core/sail/lmdb/target/benchmark-output.log` until the next benchmark run. CPU evidence is retained at `profiles/lmdb-theme-load/cpu/java-command-cpu-91024.txt`, and Linux JFR evidence is retained at `profiles/lmdb-theme-load/linux-jdk26.jfr`. The baseline diagnostic phase summary was:

    prepared-builder statements=13823295 values=4741090 ms=3077.380
    prepared-store statements=13823295 values=4741090 valueMs=7150.706 prepareSubmitMs=686.447 totalMs=13757.137
    fresh-triple-commit closeMs=0.025 lockMs=0.013 lmdbMs=102.216 resetMs=21829.467 totalMs=21931.720
    ThemeDataLoadingBenchmark.loadThemeDatasets  ss  39.127 s/op

The final passing gates were:

    Local JDK 25:  ThemeDataLoadingBenchmark.loadThemeDatasets  ss  5  8.731 ± 1.958 s/op
    individual measurements: 9.036, 9.070, 9.148, 8.413, 7.989 s/op
    Docker JDK 26: ThemeDataLoadingBenchmark.loadThemeDatasets  ss  5  8.761 ± 1.007 s/op
    individual measurements: 8.842, 8.815, 8.798, 9.026, 8.322 s/op
