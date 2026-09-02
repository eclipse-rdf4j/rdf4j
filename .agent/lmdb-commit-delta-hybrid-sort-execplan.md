# Compare and combine direct-adjacency commit sorting designs

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Sealing a large LMDB direct-adjacency commit currently decodes each event token during every comparison, recursively copies merge ranges, and then trims three completed columns with additional array copies. Three independently supplied replacements attack that cost differently: cached token-column LSD radix sorting, direct-row MSD American-flag sorting with introsort leaves, and event-ordinal LSD sorting with direction-specific streams. This work will test and benchmark all three on Java 25, explain which mechanisms affect real import performance, and combine the winning mechanisms into one production implementation that preserves the exact unsigned key, signed plane, unsigned predicate ordering.

## Progress

- [x] (2026-08-01 20:29Z) Read the proposal, target class, consumers, memory accounting, and existing direct-adjacency tests.
- [x] (2026-08-01 20:29Z) Ran the mandatory offline root quick clean install; all reactor modules succeeded in 32.177 seconds.
- [x] (2026-08-01 20:37Z) Added and ran deterministic boundary, duplicate, plane, unsigned-value, and logical-size characterization tests; 9 tests passed.
- [x] (2026-08-01 20:44Z) Captured Java 25 pre-change baselines for both seal-path JMH with GC profiling and the representative Datagov batched load.
- [x] (2026-08-01 20:49Z) Implemented candidate A: cached token columns, bottom-up merging, byte-wise stable LSD radix passes, and logical pending-table size.
- [x] (2026-08-01 21:03Z) Verified candidate A with 9 focused sorter tests and 13 real-store commit tests; stopped its non-final module run after 15 minutes in unrelated kernel-decline census work.
- [x] (2026-08-01 21:00Z) Inspected candidate B's direct-row MSD/introsort patch and candidate C's event-ordinal LSD bundle and fuzz harness.
- [x] (2026-08-01 21:06Z) Expanded shared correctness and workload-shape benchmark matrices around every candidate cutoff and added bounded randomized coverage without reflection.
- [x] (2026-08-01 21:14Z) Benchmarked candidate A at all dispatch boundaries, across five 100,000-event shapes, and through the representative Datagov selector.
- [x] (2026-08-01 21:24Z) Implemented candidate B and passed 24 sorter cases plus 13 real-store commit cases; benchmarked identical boundary, shape, and Datagov selectors.
- [x] (2026-08-01 21:31Z) Implemented candidate C and passed 24 sorter cases plus 13 real-store commit cases; benchmarked identical boundary, shape, and Datagov selectors.
- [x] (2026-08-01 21:52Z) Combined B's direct-row introsort/MSD, C's stable event-ordinal LSD, constant-digit skipping, ordered bypass, stream deduplication, and conditional compaction behind an exact entropy-based dispatch; 24 shared cases pass.
- [x] (2026-08-01 23:29Z) Completed broad verification (2,836 unit tests and 51 integration tests green with two diagnosed unrelated exclusions), profiled the final Datagov path with JFR, audited the diff, and recorded outcomes.

## Surprises & Discoveries

- Observation: `LmdbDirectAdjacencyCommitDelta.PendingTable` is shared outside its defining class only as an opaque type. Its consumers call `revision()`, `rowCount()`, `touches(...)`, and `touchesNode(...)`; no consumer reads the backing-array lengths.
  Evidence: `rg -n "PendingTable|pendingTable\\(\\)|rowCount\\(\\)|touchesNode" core/sail/lmdb/src/main core/sail/lmdb/src/test --glob '*.java'` found construction only in `LmdbDirectAdjacencyCommitDelta` and pending-table publication in `LmdbDirectAdjacencyStore`.

- Observation: direct-adjacency planes are the non-negative values 0, 1, 2, and 3, so both `byte` tuple storage and `1 << plane` masks are safe for every supported plane.
  Evidence: `LmdbAdjacencyPlane` defines outgoing explicit, incoming explicit, outgoing inferred, and incoming inferred as 0 through 3 respectively.

- Observation: the existing seal reservation already charges 64 bytes per event for two integer token arrays and the worst-case three-column pending table. The proposed implementation changes when those arrays are populated, not the charged upper bound.
  Evidence: `LmdbDirectAdjacencyCommitDelta.seal(long)` reserves `count * 64` bytes before calling `buildPendingTable()`.

- Observation: two unrelated benchmark integration-test files became tracked modifications during this turn, but neither overlaps the commit-delta sorter or its new fixtures.
  Evidence: `git status --short --untracked-files=no` lists `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbThemeFastestRunSnapshotIT.java` and `LmdbThemeQueryRegressionIT.java`; this task leaves both untouched.

- Observation: JMH's time score excludes `@Setup(Level.Invocation)` and `@TearDown(Level.Invocation)`, while the GC profiler's per-operation allocation covers the full invocation lifecycle. Allocation comparisons are still valid because baseline and candidate use the identical fixture, but allocation is not a seal-only number.
  Evidence: the 16-event baseline reports 35,904 bytes per operation, which includes the collector's 1,024-event initial-capacity growth performed in invocation setup.

- Observation: candidate B materializes final row columns directly and uses a 17-level in-place MSD American-flag radix, avoiding row-sized ordering scratch; candidate C keeps three `int[count]` event-ordinal arrays, sorts predicate once, derives plane order by stable partition, sorts outgoing and incoming keys separately, then merges the streams.
  Evidence: `/Users/havardottestad/Downloads/LmdbDirectAdjacencyCommitDelta-radix.patch` defines `msdRadixSortRows`, while `/Users/havardottestad/Downloads/lmdb-direct-adjacency-sort-optimization.zip` defines `buildPendingTableRadix`, `partitionDirections`, and `mergeDirections`.

- Observation: the standalone candidate-C fuzz harness reads private pending-table arrays through reflection. Repository tests must not copy that mechanism.
  Evidence: the bundle's `LmdbDirectAdjacencyCommitDeltaSortFuzzTest.java` calls `getDeclaredField` and `setAccessible(true)`; the in-repository characterization instead checks every expected row through `touches`, every key-plane through `touchesNode`, the independently deduplicated row count, and absent probes.

- Observation: candidate A's first complete LMDB module run did not expose a sorter failure, but an unrelated query-strategy census occupied the Surefire main thread for more than 15 minutes.
  Evidence: a `jcmd` thread dump showed `LmdbNativeKernelDeclineCensusTest.everyShippedStrategyWinsSomeThemeQuery` in `PatternCursor.next`; the already completed sorter and commit reports remained green. The run was interrupted before editing source, and the final winner still requires a fresh full-module gate.

- Observation: candidate A sharply improves both sides of its 4,096-event dispatch, with a larger discontinuous gain when stable LSD radix replaces bottom-up merging at the threshold.
  Evidence: random-input seal time fell from the recursive baseline's 702.207, 731.013, and 681.351 microseconds at 4,095, 4,096, and 4,097 events to 443.811, 227.500, and 221.931 microseconds. Normalized allocation fell from 568,893, 568,973, and 839,509 bytes to 442,259, 443,346, and 713,842 bytes.

- Observation: candidate A benefits substantially when LMDB identifiers share constant radix digits, but its cached token columns retain a shape-independent allocation cost.
  Evidence: at 100,000 events, full-width random, narrow-ID, hot-key, ordered, and duplicate-heavy inputs took 8.002, 4.423, 3.705, 3.189, and 1.604 milliseconds respectively, while every shape allocated about 13.620 megabytes per invocation lifecycle.

- Observation: candidate A improves the representative complete Datagov batched load, although the three-iteration macro estimate is noisier than the seal microbenchmark.
  Evidence: the average moved from 739.502 milliseconds to 711.558 milliseconds per load, a 3.8% reduction; individual candidate-A measurements were 717.222, 726.919, and 690.532 milliseconds.

- Observation: candidate B's direct-row in-place MSD path is dramatically faster for random and already ordered rows and uses less allocation than candidate A, but candidate A remains slightly faster for narrow-ID and duplicate-heavy distributions.
  Evidence: at 100,000 events candidate B measured 4.342, 4.817, 3.981, 1.809, and 1.804 milliseconds for random, narrow-ID, hot-key, ordered, and duplicate-heavy shapes, versus candidate A's 8.002, 4.423, 3.705, 3.189, and 1.604 milliseconds. Candidate B allocated about 12.037 megabytes for every shape versus A's 13.620 megabytes.

- Observation: candidate B's 768-event crossover validates the supplied cutoff on random data, and its larger-event result is the strongest microbenchmark improvement so far.
  Evidence: the last introsort point at 767 events measured 25.216 microseconds, while 768 and 769-event MSD cases measured 17.059 and 17.340 microseconds. At 4,096 events B measured 128.666 microseconds and 395,201 bytes, compared with A's 227.500 microseconds and 443,346 bytes and the baseline's 731.013 microseconds and 568,973 bytes.

- Observation: candidate B's microbenchmark advantage survives a complete Datagov load and is less noisy than candidate A's macro result.
  Evidence: candidate B's measurements were 685.599, 691.897, and 687.324 milliseconds, averaging 688.273 milliseconds: 6.9% below the 739.502-millisecond baseline and 3.3% below candidate A.

- Observation: candidate C's event-ordinal pipeline is slower than B at every random boundary and allocates more, but its constant-digit skipping is the best large-commit mechanism on low-entropy columns.
  Evidence: C measured 32.409 and 196.917 microseconds at 512 and 4,096 random events versus B's 16.982 and 128.666 microseconds. At 100,000 events, however, C measured 3.044 milliseconds for narrow IDs, 2.186 for hot keys, and 0.822 for duplicates, beating B's 4.817, 3.981, and 1.804 milliseconds.

- Observation: candidate C and B are effectively tied on the synthetic ordered shape, while C's full-width random path is almost twice B's and carries larger ordinal scratch.
  Evidence: C measured 1.768 milliseconds ordered and 8.116 milliseconds random with about 13.252 megabytes allocated; B measured 1.809 and 4.342 milliseconds with about 12.037 megabytes allocated.

- Observation: candidate C did not beat candidate B on the Datagov macro selector despite winning the synthetic narrow-ID seal fixture.
  Evidence: C's 697.617, 687.171, and 731.908 millisecond measurements averaged 705.565 milliseconds, compared with B's 688.273 milliseconds. The wide confidence interval means this macro difference needs a longer final confirmation, but it does not justify selecting C wholesale.

- Observation: the low-entropy crossover depends on the number of stable LSD passes, not transaction size alone. Candidate C beats B from 512 events when few 11/13-bit chunks vary, while B remains much faster at the same sizes on full-width values.
  Evidence: at 512 events C versus B measured 12.647 versus 17.364 microseconds for narrow IDs, 11.761 versus 14.902 for hot keys, and 5.250 versus 12.765 for duplicates; random input measured 32.409 versus 16.982. At 768 events the same direction held. This supports dispatch on exact varying-chunk count rather than a workload name or size-only heuristic.

- Observation: seven or fewer varying subject/object/predicate chunks is a conservative measured stable-LSD crossover, and direct materialization itself can be skipped entirely when its emitted rows are already in tuple order.
  Evidence: the final selector counts nonzero 11/13-bit chunks in aggregate XOR differences, rejects a high-entropy disordered prefix after 64 events, and checks the exact unsigned-key/signed-plane/unsigned-predicate relation for the ordered path. At 100,000 events the hybrid measured 4.132 milliseconds random, 3.164 narrow, 2.202 hot-key, 0.323 ordered, and 1.031 duplicate-heavy.

- Observation: the final hybrid retains candidate B's allocation profile on the random/direct-row path, pays candidate C's ordinal scratch only on low-entropy commits, and trims extremely duplicate-heavy pending tables.
  Evidence: normalized 100,000-event lifecycle allocation was about 12.037 megabytes random and 13.251 megabytes on the stable ordinal paths; the ordered direct path used 12.018 megabytes. Conditional compaction triggers only when fewer than one third of row slots survive deduplication.

- Observation: a reverse-order ten-iteration control confirms a material import improvement beyond the earlier three-sample estimates.
  Evidence: with two warmups and ten measurements, the final hybrid averaged 674.565 ± 27.980 milliseconds per Datagov load. After temporarily rebuilding the untouched recursive source, the identical selector averaged 755.321 ± 56.637 milliseconds. The hybrid is 10.7% faster in that controlled pair and 8.8% faster than the original 739.502-millisecond baseline; the final source was restored immediately afterward.

- Observation: the normal LMDB module unit and integration selections pass with the hybrid, including direct-adjacency query, bootstrap, and supernode-kernel coverage.
  Evidence: the final offline module verify reported 2,836 Surefire tests with zero failures/errors and 51 Failsafe tests with zero failures/errors. `LmdbDirectAdjacencySupernodeKernelTest` passed four tests in 184.8 seconds. `LmdbNativeKernelDeclineCensusTest` and `LmdbThemeQueryRegressionIT` were excluded only after thread dumps showed their independent, fixture-heavy work consuming more than 15 minutes; both files predate and are unrelated to this task.

- Observation: the final direct-import JFR confirms the selected sorter is material to Datagov without becoming an allocation hotspot.
  Evidence: the pinned `automaticEvaluationStrategy=false`, `isolationLevel=NONE` recording measured 655.767 ± 30.275 milliseconds per load. `msdRadixSortRows` accounted for 4.59% of 7,946 execution samples, while `buildPendingTable` accounted for 0.50% of sampled allocation pressure. Adjacency run encoding remained the largest allocation site at 47.15%, so the remaining import cost is primarily outside this sorter.

- Observation: the adaptive dispatch is JIT-stable in the profiled macro path.
  Evidence: each commit-delta helper recorded at most two deoptimizations over the 107-second direct profile, no helper appeared among the longest compilations, and the two compilation failures were transient `concurrent class loading` events in C1/C2 rather than failures in this code.

- Observation: `DatagovLoadIsolationBenchmark` mixes two materially different workloads when all parameters are left at defaults.
  Evidence: the profiled four-case matrix measured 702.342 and 694.940 milliseconds with automatic evaluation disabled, versus 1,898.905 and 1,218.087 milliseconds with it enabled. Fixed benchmark parameters are therefore required for sorter comparisons; the full matrix remains useful for confirming that evaluation-strategy maintenance can dominate import time.

## Decision Log

- Decision: Use Routine D because replacing the complete sorting algorithm and table representation is a significant hot-path refactor, even though pending-table behavior is intended to remain unchanged.
  Rationale: The change adds two sorting strategies, a dispatch threshold, new logical-size semantics, and multiple primitive passes. A self-contained implementation and validation record is safer than treating it as a local micro-edit.
  Date/Author: 2026-08-01 / Codex

- Decision: Add characterization tests before editing production code and run the same tests afterward.
  Rationale: The change is semantic-preserving, so the tests should already pass against the recursive merge implementation. Running them first establishes the reference behavior without inventing a failure or exposing private arrays.
  Date/Author: 2026-08-01 / Codex

- Decision: Validate sorted-table behavior only through package-visible public behavior: `rowCount`, `touches`, and `touchesNode`.
  Rationale: Checking every generated row through binary search detects comparator, deduplication, unsigned-order, plane, and logical-bound errors while avoiding reflection or a test-only production API.
  Date/Author: 2026-08-01 / Codex

- Decision: Benchmark the full `seal(long)` path with event recording performed in invocation setup.
  Rationale: The requested optimization affects pending-table construction during sealing. Keeping event generation and collector growth outside the measured method isolates the relevant allocations, sorting, and compaction while still exercising the production ownership path.
  Date/Author: 2026-08-01 / Codex

- Decision: Pair the seal microbenchmark with `DatagovLoadIsolationBenchmark.loadDatagovFileInBatches` using `READ_COMMITTED` and `automaticEvaluationStrategy=false`.
  Rationale: The user identified this benchmark, and its 100,000-statement commit batches select the radix path through a real `LmdbStore`. One fixed parameter pair avoids mixing sorter cost with isolation/evaluation-strategy matrix effects while giving an end-to-end load measurement.
  Date/Author: 2026-08-01 / Codex

- Decision: Test and benchmark all three optimized implementations sequentially, then retain or combine mechanisms only when identical Java 25 evidence supports them.
  Rationale: The designs make materially different allocation, cache-locality, pass-count, stability, and cutoff tradeoffs. Applying all patches together would obscure attribution and could stack incompatible representations.
  Date/Author: 2026-08-01 / Codex

- Decision: Expand the seal benchmark to representative random, narrow sequential-ID, hot-key, already-ordered, and duplicate-heavy workloads, and include small, crossover, and large import-sized commits.
  Rationale: Candidate B is designed to terminate MSD work after distinguishing leading bytes, while candidates A and C skip constant LSD digits; a single full-width-random fixture cannot explain which mechanism helps the LMDB import distribution.
  Date/Author: 2026-08-01 / Codex

- Decision: Select the final algorithm by exact direct-row ordering and the count of varying 11/13-bit chunks across subject, object, and predicate columns.
  Rationale: Size-only thresholds cannot retain both B's full-width advantage and C's narrow/duplicate advantage. Aggregate XOR differences determine exactly which stable radix passes are identities; counting those chunks is cheap, monotonic, representation-independent, and directly predicts the work C performs. Seven passes is the measured conservative crossover, while high-entropy disordered input exits the profile scan after 64 events.
  Date/Author: 2026-08-01 / Codex

- Decision: Keep direct rows for small and high-entropy commits, stable event ordinals for low-entropy commits, and a separate already-ordered direct path.
  Rationale: B is the consistent winner for small and random work, C wins low-entropy work from its 512-event boundary, and neither supplied implementation exploited the fact that B's materialized row stream can sometimes already satisfy final tuple order. The three-way dispatch preserves each measured strength without allocating both representations.
  Date/Author: 2026-08-01 / Codex

- Decision: Treat the reverse-order Datagov pair as the primary macro comparison and use JFR only for mechanism attribution.
  Rationale: the reverse-order pair uses identical warmup, measurement, fork, and fixed parameter settings for baseline and hybrid. JFR deliberately disables warmup and adds recording overhead, so its score is supporting evidence, not a replacement for the controlled timing comparison.
  Date/Author: 2026-08-01 / Codex

- Decision: Complete broad verification with normal Surefire/Failsafe discovery and targeted exclusion properties for the two independently diagnosed long tests.
  Rationale: `-Dtest` overrides normal includes and accidentally discovers generated JMH support classes. `surefire.excludes` and `failsafe.excludes` preserve the module's real test selection while omitting only the two unrelated outliers; all direct-adjacency and import-adjacent coverage remains included.
  Date/Author: 2026-08-01 / Codex

## Outcomes & Retrospective

The completed implementation replaces the recursive comparator merge with an adaptive three-way seal path. Commits below 512 events and high-entropy commits use candidate B's directly materialized rows, introsort leaves, and in-place MSD American-flag radix. Low-entropy commits use candidate C's stable event-ordinal pipeline with constant-digit skipping and 11/13-bit digits. An exact already-ordered check bypasses sorting, and both sorted paths deduplicate while streaming into the output columns and retain capacity unless compaction is materially worthwhile. `PendingTable` now carries an explicit logical size, with runtime bounds validation and no public API change.

All three candidates passed the same 24-case characterization suite and 13 existing real-store commit tests before comparison. The final implementation passes those focused selections, then a broad offline LMDB verify with 2,836 unit tests and 51 integration tests, all with zero failures/errors. The two excluded integration classes are pre-existing, independently CPU-bound theme-query fixtures; targeted commit-delta, direct-adjacency query, bootstrap, and supernode-kernel tests all ran and passed.

The mechanism results were consistent. Candidate A improved Datagov by 3.8% but retained extra token-column allocation. Candidate B was strongest on small and full-width/random data and improved Datagov by 6.9%. Candidate C was strongest on narrow, hot-key, and duplicate-heavy columns but lost on random data and averaged only a 4.6% Datagov improvement. The hybrid retained B's 4.132-millisecond 100,000-event random result, approached C on narrow/hot/duplicates (3.164, 2.202, and 1.031 milliseconds), and added a 0.323-millisecond exact ordered fast path.

The controlled macro result is 674.565 ± 27.980 milliseconds per Datagov load versus 755.321 ± 56.637 milliseconds for the reverse-order untouched implementation, a 10.7% reduction. A separate pinned JFR run measured 655.767 ± 30.275 milliseconds and showed the final in-place MSD sorter at 4.59% of CPU samples but commit-delta construction at only 0.50% of sampled allocation pressure. This supports the causal claim: removing repeated comparator/token decoding and merge copying is large enough to move import time, while the final scratch strategy does not dominate allocation.

The seven-pass entropy cutoff is an empirical performance policy derived from the measured Java 25 crossover matrix; it does not affect correctness, but another CPU or radically different ID distribution could shift the optimal cutoff. The exact ordered check and constant-digit skip are distribution-independent. The implementation, characterization test, JMH fixture, and this plan are the task's intended files. Existing unrelated tracked modifications and all pre-existing untracked artifacts were left untouched.

## Context and Orientation

The Maven module `core/sail/lmdb` contains RDF4J's LMDB-backed store. During one authoritative write transaction, `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyCommitDelta.java` records each statement mutation in flat primitive event arrays. `seal(long)` transfers those event arrays into an immutable `SealedDirectDelta` and constructs a `PendingTable`. The pending table contains one outgoing row and one incoming row for every event, sorted by unsigned node key, then plane, then unsigned predicate identifier, with exact duplicate rows removed. The store publishes that table before its asynchronous adjacency applier catches up, allowing queries to identify precisely which rows must fall back to authoritative LMDB.

A token is an integer encoding an event index and direction: `token >>> 1` is the event, an even token is outgoing, and an odd token is incoming. The current comparator repeatedly decodes this token and reads subjects, objects, predicates, and flags. The current top-down merge sort recursively sorts ranges, copies each full range to scratch at every merge level, and copies the final deduplicated key, predicate, and plane columns again with `Arrays.copyOf`.

Candidate A constructs token-indexed key, predicate, and plane columns. Ordinary commits use insertion-sorted runs followed by bottom-up ping-pong merging; commits with at least 8,192 direction tokens use stable byte-wise LSD passes over predicate, plane, then key. Candidate B directly materializes the final row columns, uses introsort below 1,536 rows, and otherwise recursively partitions in place by the 17 key bytes from most to least significant. Candidate C uses comparison sorting below 512 events; above that point it stable-radix-sorts event ordinals by predicate, partitions them into direction-specific plane streams, stable-radix-sorts subject and object streams, then merges and deduplicates them. It uses 11-bit digits below 4,096 events and 13-bit digits at and above that boundary.

Because deduplication now compacts into the already allocated tuple columns, those arrays keep the original token capacity. `PendingTable` must therefore store a separate logical `size` and use it for fences, row counts, masks, and binary-search bounds. The existing three-argument constructor remains and delegates with `rawKeys.length`, preserving package-local construction of already compact tables.

## Plan of Work

Extend `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyCommitDeltaSortTest.java` with deterministic cases immediately around 16-token insertion runs, candidate C's 512-event radix threshold, candidate B's 768-event row threshold, 1,024-event capacity growth, and every 4,096-event large-radix threshold. Cover full-width random values, narrow IDs, hot keys, already ordered inputs, all-identical and duplicate-heavy rows, all four planes, and unsigned boundaries. Add bounded randomized cases derived from the supplied fuzz harness without reflection. Build the expected row set independently, assert its count, check every row and key-plane through `PendingTable`, reject absent probes, close the sealed delta, and confirm memory charges return to zero.

Extend `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyCommitDeltaBenchmark.java` with a workload-shape parameter while keeping trial input generation and invocation setup outside the measured `seal(long)` method. Run identical selected parameter sets for each candidate and save JSON plus GC allocation results under distinct names. Pair each candidate's seal measurements with the fixed-parameter Datagov batched-load selector.

Run the new unit test against the current implementation. Then run the JMH method with the repository benchmark wrapper and retain its result as the baseline. The unit test is expected to pass because behavior is unchanged; the benchmark records current cost rather than acting as a correctness gate.

Evaluate candidate A first, then replace only the sorting/table-construction section with candidate B and candidate C in turn. Run the same focused tests and performance commands after each implementation. Record timing, normalized allocation, data-shape sensitivity, code/memory complexity, and end-to-end Datagov results. Design the final implementation from the measured winner, borrowing another candidate's small-commit path, ordered-input check, deduplication retention policy, radix width, or memory accounting only when a focused rerun proves the combination helps.

Run copyright validation and formatting. Re-run the exact focused test, then `LmdbDirectAdjacencyCommitTest`, then the complete `core/sail/lmdb` module through the repository's `mvnf` runner. Re-run the same JMH selector on Java 25. Compare baseline and candidate results without claiming a precise cause beyond the observed algorithm and allocation changes unless profiling or JIT evidence supports it. Finally inspect tracked and untracked status, the file-scoped diff, and update this plan's progress, discoveries, decisions, outcomes, and bottom revision note.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`.

The mandatory initial build has already run:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Its expected terminal result is `BUILD SUCCESS` with the `RDF4J: LmdbStore` reactor row also reporting `SUCCESS`.

After adding the focused unit test, validate headers and capture the pre-change characterization with:

    (cd scripts && ./checkCopyrightPresent.sh)
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyCommitDeltaSortTest --retain-logs

Expect the focused Surefire report to show zero failures and errors.

Build and run the pre-change benchmark with:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.LmdbDirectAdjacencyCommitDeltaBenchmark --method seal

The benchmark output must identify Java 25 and print average-time scores for each configured event count.

Capture the pre-change end-to-end batch-load baseline from the already built benchmark jar with:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.DatagovLoadIsolationBenchmark --method loadDatagovFileInBatches --no-build --warmup-iterations 1 --measurement-iterations 3 --param isolationLevel=READ_COMMITTED --param automaticEvaluationStrategy=false

Expect three measured load operations after one warmup. Repeat the identical parameter selection after the production change.

After the production edit, format and test with:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyCommitDeltaSortTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyCommitTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

The formatter command uses the repository-prescribed quiet formatter invocation, not a test run. Every `mvnf` test selection is expected to report zero failures and errors. Repeat the benchmark command unchanged for a comparable candidate result.

## Validation and Acceptance

Correctness is accepted for each candidate when deterministic and bounded randomized characterization passes immediately around 8-event insertion runs, 512-event radix dispatch, 768-event direct-MSD dispatch, 1,024-event capacity growth, and 4,096-event large-radix dispatch; every independently generated unique row is found through `PendingTable.touches`, every unique key-plane pair is found through `touchesNode`, selected missing rows are rejected, the logical row count is exact, and all memory charges are released. The final combined implementation must also pass the existing direct-adjacency commit test and complete LMDB module.

Performance is accepted when every candidate runs the exact same selected seal JMH parameters and representative Datagov batch-load selector on Java 25. The final design must materially improve Datagov import over the 739.502 ms/op recursive-sort baseline without regressing ordinary 5-to-50-event transactions. Mechanism claims require matching workload-shape timing, allocation, or profile evidence; noisy results remain explicitly inconclusive.

## Idempotence and Recovery

The test, formatter, Maven runner, and benchmark commands are safe to repeat. The new collector and sealed delta are closed in test and benchmark cleanup so repeated execution does not retain pending-memory charges. Do not remove any pre-existing untracked files. If offline dependency resolution fails, rerun the exact build once without `-o`, then return to offline operation. If production code is accidentally edited before characterization has run, revert only this task's hunk with `apply_patch`, restore the test-first order, and leave unrelated workspace content untouched.

## Artifacts and Notes

The initial build transcript is retained in `maven-build.log`. Focused Maven verification logs will be retained under `logs/mvnf/`. Benchmark output will be retained using the benchmark wrapper's reported result location or copied into a task-specific top-level evidence file without modifying prior evidence artifacts. No initial `target` report is treated as durable because later test runs overwrite reports.

The Java 25 baseline is stored at `benchmark-results/commit-delta-sort-baseline.json`. With two one-second warmups and four one-second measurements, average seal times were 0.758 microseconds for 16 events, 15.410 microseconds for 256 events, 702.207 microseconds for 4,095 events, 731.013 microseconds for 4,096 events, and 681.351 microseconds for 4,097 events. Corresponding full-invocation normalized allocations were 35,904, 55,320, 568,893, 568,973, and 839,509 bytes per operation.

The matching Datagov baseline is stored at `benchmark-results/commit-delta-datagov-baseline.json`. For `READ_COMMITTED` with `automaticEvaluationStrategy=false`, the one warmup measured 1,109.856 milliseconds and the three measured operations were 742.798, 732.546, and 743.163 milliseconds, averaging 739.502 milliseconds per complete batched load.

Candidate A's boundary results are stored at `benchmark-results/commit-delta-candidate-a-boundaries.json`; its five import-sized workload shapes are at `benchmark-results/commit-delta-candidate-a-shapes.json`; and its complete-load result is at `benchmark-results/commit-delta-datagov-candidate-a.json`. The 100,000-event shape scores were 8.002 milliseconds for random, 4.423 for narrow IDs, 3.705 for hot keys, 3.189 for already ordered input, and 1.604 for duplicate-heavy input. The Datagov average was 711.558 milliseconds per operation.

Candidate B's matching artifacts are `benchmark-results/commit-delta-candidate-b-boundaries.json`, `benchmark-results/commit-delta-candidate-b-shapes.json`, and `benchmark-results/commit-delta-datagov-candidate-b.json`. Its 100,000-event shape scores were 4.342 milliseconds for random, 4.817 for narrow IDs, 3.981 for hot keys, 1.809 for ordered rows, and 1.804 for duplicate-heavy rows. The Datagov average was 688.273 milliseconds per operation.

Candidate C's matching artifacts are `benchmark-results/commit-delta-candidate-c-boundaries.json`, `benchmark-results/commit-delta-candidate-c-shapes.json`, and `benchmark-results/commit-delta-datagov-candidate-c.json`. Its 100,000-event shape scores were 8.116 milliseconds for random, 3.044 for narrow IDs, 2.186 for hot keys, 1.768 for ordered rows, and 0.822 for duplicate-heavy rows. The Datagov average was 705.565 milliseconds per operation.

The final hybrid artifacts are `benchmark-results/commit-delta-hybrid-boundaries.json`, `benchmark-results/commit-delta-hybrid-final-shapes.json`, and `benchmark-results/commit-delta-datagov-hybrid-final.json`. The reverse-order original control is `benchmark-results/commit-delta-datagov-baseline-control.json`. Final 100,000-event scores were 4.132 milliseconds random, 3.164 narrow, 2.202 hot-key, 0.323 ordered, and 1.031 duplicate-heavy. The ten-measurement complete-load average was 674.565 milliseconds versus the reverse-order original's 755.321 milliseconds.

The final JFR recordings are `benchmark-results/commit-delta-datagov-hybrid-final.jfr` for the last fork of the full isolation/evaluation-strategy matrix and `benchmark-results/commit-delta-datagov-hybrid-direct.jfr` for the fixed direct-import comparison workload. The latter contains 7,946 execution samples over 107 seconds and is the recording used for sorter CPU, allocation, deoptimization, and compilation analysis.

Candidate A's dispatch is 8,192 tokens, or 4,096 events. Candidate B dispatches to MSD at 1,536 rows, or 768 events. Candidate C dispatches to event-ordinal radix at 512 events and expands from 11-bit to 13-bit digits at 4,096 events. These boundaries and their immediate neighbors belong in both correctness and focused benchmark selection.

## Interfaces and Dependencies

No new external dependency is needed. Every candidate uses primitive arrays, `java.util.Arrays`, and existing plane constants. The final implementation must keep all sorting helpers private, retain the package-visible compact three-array `PendingTable` constructor, add a logical-size constructor, and make no public RDF4J API change.

Revision note (2026-08-01 20:29Z): Created the initial self-contained plan after source and consumer inspection so characterization, implementation, module verification, and Java 25 performance measurement can proceed reproducibly.

Revision note (2026-08-01 20:37Z): Recorded the green pre-change characterization and the unrelated tracked-file boundary before starting the baseline benchmark.

Revision note (2026-08-01 20:41Z): Added the Java 25 JMH baseline, clarified profiler scope, and moved the plan into production implementation.

Revision note (2026-08-01 20:43Z): Added the user-requested Datagov batched-load benchmark as a fixed-parameter macro baseline before production editing.

Revision note (2026-08-01 20:44Z): Recorded the completed Datagov baseline and unblocked production implementation.

Revision note (2026-08-01 20:49Z): Recorded the formatted production implementation and moved into focused correctness verification.

Revision note (2026-08-01 20:50Z): Recorded green focused and real-store commit suites before broadening to the complete LMDB module.

Revision note (2026-08-01 21:00Z): Expanded the plan from one proposed sorter to a three-candidate correctness, workload-shape, allocation, and Datagov import comparison, with a measured final combination as the outcome.

Revision note (2026-08-01 21:06Z): Recorded candidate A's focused verification, the unrelated broad-run interruption, and the expanded cross-candidate fuzz and workload-shape fixtures.

Revision note (2026-08-01 21:14Z): Recorded candidate A's boundary, workload-shape, allocation, and Datagov results before replacing it with candidate B.

Revision note (2026-08-01 21:24Z): Recorded candidate B's green shared correctness suites and its boundary, shape, allocation, and Datagov advantage before candidate C.

Revision note (2026-08-01 21:31Z): Recorded candidate C's green shared correctness suites, its low-entropy wins, its random/allocation losses, and its noisier Datagov result before hybrid design.

Revision note (2026-08-01 21:52Z): Recorded the adaptive hybrid, low-entropy crossover evidence, final shape matrix, and ten-iteration reverse-order Datagov control before broad verification and profiling.

Revision note (2026-08-01 23:29Z): Recorded the successful 2,836-unit/51-integration broad verify, fixed-parameter and matrix JFR evidence, final diff audit, completed outcomes, and the empirical threshold caveat.
