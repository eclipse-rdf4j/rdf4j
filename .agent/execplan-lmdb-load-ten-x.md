# Make LMDB single-transaction loading ten times faster

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The goal is to make the LMDB store load the `datagovbe-valid.ttl.gz` benchmark dataset in one transaction at least ten times faster for both `NONE` and `READ_COMMITTED` isolation when `automaticEvaluationStrategy=false`. The current post-adaptive-write baseline on macOS JDK 26 with a 2 GiB heap is 802.295 ms/op for `NONE` and 1043.529 ms/op for `READ_COMMITTED`. Completion therefore requires repeatable average times at or below 80.23 ms/op and 104.35 ms/op respectively with the same benchmark, dataset, heap, indexes, durability semantics, and isolation settings. LMDB append flags, including `MDB_APPEND` and `MDB_APPENDDUP`, are forbidden.

The result is visible by running `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` for both isolation parameters. Correctness remains visible through focused ingestion tests, `RemoveAddTest`, `LmdbSailStoreTest`, and the complete `core/sail/lmdb` verification. Each independently measured improvement is committed before the next optimization begins.

## Progress

- [x] (2026-07-09 22:24Z) Activated the persistent 10x performance goal and selected the high-performance Java, macOS async-profiler, and Docker CPU-time JFR workflows.
- [x] (2026-07-09 22:26Z) Ran the required root offline clean install; the reactor completed with `BUILD SUCCESS` in 34.293 seconds.
- [x] (2026-07-09 22:28Z) Committed the verified adaptive aligned-write and batch-value-resolution increment as `9516353b92 GH-0000 Speed up adaptive LMDB loading`.
- [ ] Capture a fresh paired macOS JMH baseline and archive result JSON and logs outside `target` so later builds cannot remove them.
- [ ] Capture macOS async-profiler wall, CPU, and allocation profiles for `NONE` and `READ_COMMITTED` with `automaticEvaluationStrategy=false`.
- [ ] Capture Linux Java 26 Docker JFR CPU-time profiles for both isolation modes with the exact same benchmark shape.
- [ ] Rank hotspots by end-to-end share and implement one focused optimization at a time, adding a failing correctness test before behavior changes and committing every benchmark-confirmed improvement.
- [ ] Repeat paired benchmarks and both profiling modes until `NONE <= 80.23 ms/op` and `READ_COMMITTED <= 104.35 ms/op` without append mode.
- [ ] Run focused and complete verification, document remaining unrelated failures, and record the final benchmark/profile comparison.

## Surprises & Discoveries

- Observation: Transaction-adaptive aligned writes alone made `NONE` faster than `READ_COMMITTED`, but not close to the 10x target.
  Evidence: the paired JMH result was 802.295 ms/op and 351,585,522 B/op for `NONE` versus 1043.529 ms/op and 483,288,824 B/op for `READ_COMMITTED`.

- Observation: Resolving an entire 65,536-statement operation as one value-dictionary batch regressed `NONE` to 945.036 ms/op. Limiting dictionary-resolution chunks to 1,024 statements reduced the result to 802.295 ms/op.
  Evidence: `core/sail/lmdb/target/datagov-none-vs-read-committed-false-chunked.json` and the prior task transcript.

- Observation: The macOS profiling prerequisite is installed locally.
  Evidence: `/Users/havardottestad/.codex/skills/async-profiler-java-macos/scripts/resolve_async_profiler_macos.sh` reports async-profiler 4.4 at `/opt/homebrew/Cellar/async-profiler/4.4`.

- Observation: The current branch has two unrelated LMDB module test failures before this 10x iteration begins.
  Evidence: `LmdbSailStoreTest.testExplainExecutedShowsIndexName` expects legacy explanation text, and `LmdbNativeFactorizedScanOnceTest.scanOnceModeStaysCorrectAndEngages` observes zero scan-once transitions. The previous full verify ran 1,456 tests with only those two failures.

## Decision Log

- Decision: Define 10x against the current same-machine paired post-adaptive-write baseline rather than an older pre-feature state.
  Rationale: This is the strongest reproducible starting point and yields unambiguous acceptance thresholds for both isolation modes.
  Date/Author: 2026-07-09 / Codex.

- Decision: Keep the dataset, two default triple indexes, transaction boundary, 2 GiB heap, G1 collector, and automatic evaluation setting fixed throughout optimization comparisons.
  Rationale: Changing workload semantics or indexes would manufacture a benchmark win instead of improving the requested loading path.
  Date/Author: 2026-07-09 / Codex.

- Decision: Prohibit all LMDB append flags and audit each low-level write change for `MDB_APPEND` and `MDB_APPENDDUP`.
  Rationale: The user explicitly disallowed append mode. Sorted batching may still be used to improve locality, but ordinary LMDB put/cursor operations must preserve correctness without append assumptions.
  Date/Author: 2026-07-09 / Codex.

- Decision: Require both macOS async-profiler and Linux Java 26 CPU-time JFR before selecting the first new production optimization.
  Rationale: A 10x target requires cross-environment evidence and Amdahl-aware hotspot selection rather than speculative micro-tuning.
  Date/Author: 2026-07-09 / Codex.

- Decision: Commit each benchmark-confirmed increment separately with the `GH-0000` prefix because no issue number is available and the current branch has no `GH-XXXX` prefix.
  Rationale: Small commits preserve bisectability and comply with the user's explicit request for commits at every progress point.
  Date/Author: 2026-07-09 / Codex.

## Outcomes & Retrospective

The 10x optimization loop is active. The first checkpoint will preserve the already tested adaptive write and batch dictionary work. No claim of 10x completion is made until both isolation thresholds pass repeated paired JMH runs and the correctness suite remains unchanged apart from the two documented pre-existing failures.

## Context and Orientation

The relevant Maven module is `core/sail/lmdb`. `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/DatagovLoadIsolationBenchmark.java` parses the compressed dataset once at trial setup, then each benchmark invocation creates a fresh temporary LMDB repository, begins the requested isolation level, adds the already parsed RDF4J `Model`, commits, checks that data exists, closes the repository, and deletes the temporary directory. The benchmark therefore measures repository creation, value dictionary work, triple index writes, commit, a final existence check, shutdown, and temporary-directory deletion, but not Turtle parsing.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` connects RDF4J's transaction sink to the LMDB value and triple stores. Its `LmdbSailSink` receives statements, manages transaction ordering, creates `BulkAddQuadsOperation` instances, and optionally executes them on worker threads. A reservation budget bounds the total statement capacity of pending, queued, and executing operations.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` maps RDF values such as IRIs, blank nodes, and literals to numeric IDs. The current uncommitted increment adds sorted batched ID lookup and creation so a chunk shares one LMDB transaction and reusable native carriers.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` writes numeric subject, predicate, object, and context IDs into the configured LMDB indexes. `storeTriplesAligned` sorts aligned primitive arrays into each index's key order before ordinary LMDB writes. "Aligned" means that the same array position across the four arrays describes one statement; it does not mean LMDB append mode.

`NONE` allows individual approvals to be buffered and asynchronously applied inside the store transaction. `READ_COMMITTED` must retain its isolation semantics and currently takes a different higher-level path. The optimization must accelerate both rather than merely shifting work between them.

## Plan of Work

First, preserve the existing adaptive aligned-write increment. Re-run the smallest focused tests that cover batched `NONE` approvals and batched value storage, stage only the four known LMDB files, and commit them. Do not stage unrelated untracked ExecPlans or configuration artifacts. Then add and commit this ExecPlan as the performance campaign record.

Second, establish a durable baseline. Use the exact benchmark method ending in `$` so the six-index sibling method is not selected. Run both isolation values together with five one-second warmups, five one-second measurements, one fork, JDK 26, G1, and a fixed 2 GiB heap. Enable the JMH GC profiler. Copy the JSON and complete output into a new tracked-or-ignored `profiles/lmdb-load-10x/baseline-macos` artifact directory outside Maven `target`; update this plan with the exact paths and scores.

Third, profile before modifying production. Use the RDF4J async-profiler benchmark wrapper in dry-run mode, then capture `slow`, `cpu`, and `alloc` profiles separately for `NONE` and `READ_COMMITTED`. Use a flat CPU output in addition to flamegraphs so hot methods can be ranked numerically. Record profile paths, the dominant thread, top inclusive and self-cost stacks, allocation leaders, and any native LMDB or filesystem cost. If wall time is dominated by deletion or repository lifecycle rather than ingestion, add a benchmark-only phase timer or sibling diagnostic benchmark; do not change the acceptance benchmark.

Fourth, run the Docker JFR CPU-time loop once for each isolation value. The selector is `org.eclipse.rdf4j.sail.lmdb.benchmark.DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction`, with `automaticEvaluationStrategy=false`, one isolation value, and 2 GiB heap arguments. The wrapper supplies Linux Java 26, ten ten-second measurements, debug non-safepoints, 1,024-frame stacks, and CPU-time events. Read each JFR with `jfr summary`, `jfr view cpu-time-hot-methods`, and CPU-time lost-sample checks. Record the JFR paths and compare Linux hotspot order with macOS.

Fifth, choose one optimization whose measured total share can materially move the benchmark. Likely candidate families, to be accepted or rejected by profiles, are eliminating repeated RDF value encoding and hashing, using reusable primitive batch collectors instead of per-chunk arrays and boxed maps, reducing LMDB cursor and transaction setup, transforming per-index sorting into one cache-efficient radix pipeline, coalescing ordinary `mdb_put` operations through persistent cursors without append flags, avoiding redundant duplicate checks where transaction-local deduplication proves equivalence, and separating temporary-directory cleanup from hot ingestion only as a diagnostic rather than an acceptance shortcut. For every behavior change, add the smallest failing in-repository test and preserve its Surefire evidence before production edits. For behavior-neutral hot-loop changes, capture matching pre/post green tests and direct hit proof.

After each candidate, run focused correctness tests and a short paired benchmark. Revert or revise regressions; commit confirmed improvements with a `GH-0000` imperative message. Re-run async-profiler or Docker JFR whenever the dominant hotspot shifts. Periodically run the full five-by-five paired benchmark so scores remain comparable to the acceptance baseline. Continue until both thresholds pass; a gain in only one isolation mode does not complete the goal.

Finally, run formatting, copyright checks, focused tests, `LmdbSailStoreTest`, `RemoveAddTest`, and full module verification. Preserve the two known unrelated failures only if they remain byte-for-byte equivalent in nature. Run the final paired JMH benchmark at least twice and report distributions, allocations, exact JVM, OS, profile hotspot shifts, and every commit created.

## Concrete Steps

Work from `/Users/havardottestad/Documents/Programming/rdf4j`.

Run the required repository install before testing:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run focused tests through the repository runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSailStoreTest#noneIsolationBatchesIndividualApprovals --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py ValueStoreTest#storeValuesStoresMultipleValuesWithSingleReadTransaction --retain-logs

Run the paired macOS benchmark with JDK 26 through `scripts/run-single-benchmark.sh`. Supply `automaticEvaluationStrategy=false`, `isolationLevel=NONE,READ_COMMITTED`, `-Xms2G`, `-Xmx2G`, `-XX:+UseG1GC`, the JMH GC profiler, JSON result output, and method name `loadDatagovFileSingleTransaction$`. Keep all future comparison flags identical.

Dry-run and then execute macOS profiling with:

    /Users/havardottestad/.codex/skills/async-profiler-java-macos/scripts/profile_rdf4j_benchmark_macos.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.DatagovLoadIsolationBenchmark --method 'loadDatagovFileSingleTransaction$' --mode slow --dry-run -- --param automaticEvaluationStrategy=false --param isolationLevel=NONE --jvm-arg -Xms2G --jvm-arg -Xmx2G

Repeat with `--mode cpu --format flat`, `--mode alloc`, and `isolationLevel=READ_COMMITTED`. If the wrapper syntax differs, use its printed help and update this section with the accepted invocation before proceeding.

Dry-run and then execute Linux CPU-time JFR with:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.DatagovLoadIsolationBenchmark --method loadDatagovFileSingleTransaction --param automaticEvaluationStrategy=false --param isolationLevel=NONE --jvm-arg -Xms2G --jvm-arg -Xmx2G

Repeat with `isolationLevel=READ_COMMITTED`. Read each emitted recording with the JDK 26 `jfr` command.

Before every commit, run `git diff --check`, inspect `git status --short`, stage only the intended increment, and use a message beginning with `GH-0000`.

## Validation and Acceptance

The performance goal passes only when two independent final paired JMH runs show average time at or below 80.23 ms/op for `NONE` and 104.35 ms/op for `READ_COMMITTED`, with `automaticEvaluationStrategy=false`, JDK 26, G1, 2 GiB heap, the default two LMDB triple indexes, one transaction, and no append flags. Report iteration values as well as averages so an outlier cannot hide instability. Allocation should not regress materially unless CPU and wall evidence prove a favorable tradeoff within the 2 GiB budget.

Correctness requires the focused batching, duplicate, estimator, rollback, worker failure, and add/remove ordering tests to pass. `RemoveAddTest` must pass. Full module verification must introduce no new failure beyond the two documented branch failures. Search the final diff and relevant source for `MDB_APPEND` and `MDB_APPENDDUP`; neither may be added or activated.

Profile acceptance requires saved macOS async-profiler artifacts for wall, CPU, and allocation behavior and saved Docker JFR recordings for both isolation modes. The final retrospective must name the original and final dominant hotspots and explain why the measured changes plausibly produced the benchmark delta on JDK 26.

## Idempotence and Recovery

Benchmark and profiling commands create Maven target files, temporary repositories, Docker containers, and profile artifacts but do not modify tracked production data. They are safe to rerun. Copy important results outside `target` immediately because `mvnf` and clean builds remove target artifacts. Do not delete unrelated untracked files; they belong to the user.

If a candidate optimization regresses performance, keep the benchmark/profile evidence in this plan, revert only that candidate's own patch with a forward corrective commit if it was already committed, and resume from the last confirmed commit. Do not use `git reset --hard`, `git clean`, or blanket checkout/restore commands. If Docker or profiling fails transiently, retry once; for a non-transient error, change the invocation or document the missing prerequisite rather than repeating it unchanged.

## Artifacts and Notes

Initial failing and passing TDD evidence for adaptive aligned writes is stored in `initial-evidence.txt`. The last complete LMDB verify log before this plan is `logs/mvnf/20260709-222105-verify.log`. The paired post-chunk benchmark JSON is `core/sail/lmdb/target/datagov-none-vs-read-committed-false-chunked.json`; it must be copied to the durable profile directory before any clean build removes it.

The first performance checkpoint is commit `9516353b92`. Immediately before that commit, the two focused tests for batched `NONE` approvals and single-transaction batch value storage passed with zero failures.

Expected baseline summary:

    NONE            802.295 ms/op   351585522.400 B/op
    READ_COMMITTED  1043.529 ms/op  483288824.000 B/op

Target summary:

    NONE            <= 80.23 ms/op
    READ_COMMITTED  <= 104.35 ms/op

## Interfaces and Dependencies

No new public RDF4J API or persisted configuration is planned. Production work should remain inside the LMDB module and preserve `LmdbStoreConfig`, `SailSink`, isolation, and transaction contracts. Existing LWJGL LMDB bindings, JDK primitive arrays, repository sort utilities, JMH, async-profiler 4.4, Docker, and JDK 26 JFR are the default tools. A new dependency is permitted only if profile evidence shows it removes a material cost that in-repository primitives cannot address, and only after a dependency health check.

The key internal interfaces are `LmdbSailStore.LmdbSailSink`, `LmdbSailStore.BulkAddQuadsOperation`, `ValueStore.storeValues(Value[], long[], int)`, and `TripleStore.storeTriplesAligned(long[], long[], long[], long[], int, boolean)`. Their internal implementations may change, but the final system must preserve statement identity, duplicate handling, explicit/inferred separation, context semantics, rollback, ordering, and reservation release.

Revision note (2026-07-09 22:27Z): Created the initial self-contained plan after activating the 10x goal, reading all selected skill instructions, confirming async-profiler availability, and completing the mandatory root build. The plan fixes the workload and acceptance thresholds before further optimization.

Revision note (2026-07-09 22:28Z): Recorded the first committed checkpoint and its focused green verification so future benchmark and profile artifacts have an exact code revision.
