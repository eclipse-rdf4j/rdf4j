# Make LMDB single-transaction loading ten times faster

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The goal is to make the LMDB store load the `datagovbe-valid.ttl.gz` benchmark dataset in one transaction at least ten times faster for both `NONE` and `READ_COMMITTED` isolation when `automaticEvaluationStrategy=false`. Two fresh post-adaptive-write baseline runs on macOS JDK 26 with a 2 GiB heap produced pooled means of 780.972 ms/op for `NONE` and 831.080 ms/op for `READ_COMMITTED`. Completion therefore requires repeatable average times at or below 78.10 ms/op and 83.11 ms/op respectively with the same benchmark, dataset, heap, indexes, durability semantics, and isolation settings. LMDB append flags, including `MDB_APPEND` and `MDB_APPENDDUP`, are forbidden.

The result is visible by running `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` for both isolation parameters. Correctness remains visible through focused ingestion tests, `RemoveAddTest`, `LmdbSailStoreTest`, and the complete `core/sail/lmdb` verification. Each independently measured improvement is committed before the next optimization begins.

## Progress

- [x] (2026-07-09 22:24Z) Activated the persistent 10x performance goal and selected the high-performance Java, macOS async-profiler, and Docker CPU-time JFR workflows.
- [x] (2026-07-09 22:26Z) Ran the required root offline clean install; the reactor completed with `BUILD SUCCESS` in 34.293 seconds.
- [x] (2026-07-09 22:28Z) Committed the verified adaptive aligned-write and batch-value-resolution increment as `9516353b92 GH-0000 Speed up adaptive LMDB loading`.
- [x] (2026-07-09 22:33Z) Captured and archived two fresh paired macOS JMH baselines plus a generated comparison under `profiles/lmdb-load-10x/baseline-macos`.
- [x] (2026-07-09 22:45Z) Captured macOS async-profiler wall, CPU, and allocation profiles for `NONE` and `READ_COMMITTED` with `automaticEvaluationStrategy=false`.
- [x] (2026-07-09 22:57Z) Captured end-anchored Linux Java 26 Docker JFR CPU-time profiles for both isolation modes and summarized their hot methods and lost samples.
- [x] (2026-07-09 23:14Z) Added heap-scaled transaction-owned value and namespace ID dictionaries; focused tests and all 21 `ValueStoreTest` tests pass, time improved 4.27-7.25%, and allocation fell 33.89-39.63%.
- [x] (2026-07-09 23:18Z) Re-profiled both isolation modes after transaction caching; native LMDB plus comparison work remains dominant and the primitive dictionary itself stays below 0.5% of CPU samples.
- [x] (2026-07-09 23:30Z) Tested and rejected an empty-dictionary `MDB_NOOVERWRITE` direct-put path after two paired runs pooled to no improvement; removed all production/test changes and kept the evidence.
- [x] (2026-07-09 23:39Z) Reused a transaction-retained ordinary cursor for main-index `MDB_NOOVERWRITE` writes; 35 focused/neighbor tests pass and two paired runs improved the prior checkpoint 5.58-7.53%.
- [x] (2026-07-09 23:48Z) Tested and rejected four-field secondary radix sorting after it regressed both modes by 8.20-10.50%; removed production/test changes and kept the evidence.
- [x] (2026-07-10 00:06Z) Probed an append-free `MDB_DUPSORT | MDB_DUPFIXED` index layout with grouped `MDB_MULTIPLE` writes; pure two-index insertion improved 3.28x, from 701.436 ms to 213.872 ms.
- [x] (2026-07-10 00:18Z) Swept duplicate bucket shapes and all four benchmark indexes; fixed duplicates bottomed out at 139.411 ms, while page-sized immutable packed blocks reduced four-index insertion to 39.476 ms.
- [x] (2026-07-10 00:31Z) Captured post-cursor async-profiler call stacks for both isolation modes and measured Java/lifecycle floors; triple indexes, value resolution, and Sail ingestion each require structural work.
- [x] (2026-07-10 00:40Z) Tested and rejected collision-aware partitioning by `Value.hashCode()`; 2,517 collision buckets plus `DynamicModel` upgrade costs made it slower than equality-map resolution.
- [x] (2026-07-10 00:53Z) Tested and rejected map-backed canonical term identities; all model tests passed, but paired loading regressed 9.34% for `NONE` and 27.95% for `READ_COMMITTED`.
- [x] (2026-07-10 01:08Z) Added and tested a repository-to-sink bulk-ingestion conduit; it is neutral for `NONE`, reduces current-host `READ_COMMITTED` by about 30 ms, and enables an LMDB-specific compact numeric batch next.
- [x] (2026-07-10 01:49Z) Specialized the iterable conduit in `LmdbSailSink`; two paired runs pooled to 683.717 ms/op for `NONE` and 719.733 ms/op for `READ_COMMITTED`.
- [x] (2026-07-10 02:01Z) Re-profiled direct ingestion with macOS async-profiler and Docker Java 26 CPU-time JFR; native cursor puts remain 28% self CPU and equality-based value resolution remains the next removable Java cost.
- [x] (2026-07-10 03:12Z) Tested and rejected operation-wide hash-sorted value resolution after it regressed NONE 41.36% and READ_COMMITTED 44.12%; removed all production/test changes.
- [x] (2026-07-10 03:45Z) Added append-free packed statement blocks for fresh default-evaluator loads, scoped them away from automatic/native evaluation, and reduced the Docker checkpoint to 428.029/493.600 ms/op.
- [x] (2026-07-10 04:53Z) Replaced global value IDs with a packed local value dictionary and fixed NONE namespace ordering; nine focused tests pass and forked Java 26 reaches 98.847/152.981 ms/op.
- [x] (2026-07-10 05:14Z) Halved fresh packed quad records with 32-bit local IDs; a candidate-control-candidate JDK 26 sequence improves pooled time 10.14%/3.73% and removes about 12.69 MB/op.
- [x] (2026-07-10 05:23Z) Tested and rejected direct manual UTF-8 encoding into reserved LMDB buffers; it removed about 65 MB/op but regressed elapsed time 33-52 ms.
- [ ] Rank hotspots by end-to-end share and implement one focused optimization at a time, adding a failing correctness test before behavior changes and committing every benchmark-confirmed improvement.
- [ ] Repeat paired benchmarks and both profiling modes until `NONE <= 78.10 ms/op` and `READ_COMMITTED <= 83.11 ms/op` without append mode.
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

- Observation: Same-code host drift between two consecutive paired baselines was material even though each fork was internally stable.
  Evidence: `NONE` moved from 804.502 to 757.442 ms/op and `READ_COMMITTED` moved from 876.109 to 786.051 ms/op. Within-run 99.9% errors were 21.417/43.914 ms for `NONE` and 239.381/32.299 ms for `READ_COMMITTED`.

- Observation: Pooled allocation is approximately 358.45 MB/op for `NONE` and 472.91 MB/op for `READ_COMMITTED`; GC consumed only 3-71 ms per five-iteration fork.
  Evidence: `profiles/lmdb-load-10x/baseline-macos/run-1.json` and `run-2.json`.

- Observation: Native LMDB calls dominate CPU on both macOS and Linux. The three Linux wrappers `mdb_get`, `mdb_put`, and `mdb_cursor_put` account for 52.41% of `NONE` and 50.48% of `READ_COMMITTED` CPU-time samples.
  Evidence: `profiles/lmdb-load-10x/docker-jfr/none-exact.jfr`, `read-committed-exact.jfr`, and `profiles/lmdb-load-10x/profiling-summary.md`.

- Observation: READ_COMMITTED has a distinct scalar value-resolution and allocation penalty. Its profile includes `ValueStore.getId`, and allocation has much larger `DirectByteBuffer`, `MDBVal`, `LinkedHashMap.Entry`, and `GenericStatement` shares than `NONE`.
  Evidence: the async-profiler CPU and allocation flat reports under `profiles/lmdb-load-10x/async-profiler`.

- Observation: The Docker benchmark selector is a regex. Passing an unanchored method name also selects `loadDatagovFileSingleTransaction6Indexes` and can overwrite a shared JFR output.
  Evidence: the first NONE Docker run began the six-index fork after completing the intended fork. It was interrupted and its `none.jfr` artifact is marked invalid; exact evidence uses `none-exact.jfr`.

- Observation: Retaining resolved value and namespace IDs for the active transaction materially reduces allocation but only modestly improves elapsed time, confirming that ordinary LMDB writes remain the dominant bound.
  Evidence: `profiles/lmdb-load-10x/transaction-cache/run-2-jdk26.json` measures `NONE` at 747.595 ms/op and 236,969,122 B/op and `READ_COMMITTED` at 770.796 ms/op and 285,498,796 B/op. Relative to the pooled baseline, time improves 4.27% and 7.25%, while allocation falls 33.89% and 39.63%.

- Observation: After transaction caching, the cache implementation is cheap while native LMDB and key comparison remain dominant.
  Evidence: the post-cache async-profiler captures under `profiles/lmdb-load-10x/transaction-cache` put `ObjectLongHashMap` probing at 0.35% for `NONE` and 0.14% for `READ_COMMITTED`, versus native LMDB at 29.37% and 23.57% and combined platform/stub `memcmp` at 9.45% and 9.16%.

- Observation: Replacing empty-dictionary preflight gets with conditional puts does not materially improve this workload by itself.
  Evidence: two exact JDK 26 runs under `profiles/lmdb-load-10x/empty-store-direct-put` pooled to 746.536 ms/op for `NONE` and 777.875 ms/op for `READ_COMMITTED`, respectively 0.14% faster and 0.92% slower than the transaction-cache checkpoint. The production experiment was removed.

- Observation: Retaining an ordinary cursor for main-index conditional writes provides a stable gain in both isolation modes without reducing allocation.
  Evidence: two exact JDK 26 runs under `profiles/lmdb-load-10x/main-index-cursor` pooled to 705.864 ms/op for `NONE` and 712.789 ms/op for `READ_COMMITTED`, 5.58% and 7.53% faster than the transaction-cache checkpoint. `TripleStoreTest`, aligned sort/reset, and auto-grow tests all passed.

- Observation: Fully sorting every secondary key is substantially slower than sorting only the leading field.
  Evidence: `profiles/lmdb-load-10x/full-secondary-sort/run-1-jdk26.json` measures 779.954 ms/op for `NONE` and 771.224 ms/op for `READ_COMMITTED`, regressions of 10.50% and 8.20% from the main-cursor checkpoint despite stable iterations. The production experiment was removed.

- Observation: LMDB's fixed-duplicate bulk-write primitive substantially lowers the native two-index insertion floor without append mode, but exact leading-field grouping alone does not reach the end-to-end 10x threshold.
  Evidence: the scratch probe recorded in `profiles/lmdb-load-10x/mdb-multiple-probe/README.md` improved the mean of three pure insertion rounds from 701.436 ms to 213.872 ms (3.280x) by replacing approximately 1.23 million ordinary cursor puts with 66,507 grouped `MDB_MULTIPLE` calls.

- Observation: Fixed-duplicate insertion has a U-shaped bucket-size curve and remains too slow with all four benchmark indexes, whereas writing sorted immutable blocks moves the four-index native floor below half of the end-to-end target.
  Evidence: the expanded probe in `profiles/lmdb-load-10x/mdb-multiple-probe/README.md` measures four-index `MDB_MULTIPLE` insertion at 139.411 ms with 16,384 buckets per index. Four ordinary databases containing 2,048 packed sorted blocks per index complete in 39.476 ms, including fixed-width encoding and transaction commit. `MDB_WRITEMAP` did not materially improve the multi-value result.

- Observation: The remaining load cost is split across three independent layers, so packed indexes alone cannot produce a 10x end-to-end result.
  Evidence: `profiles/lmdb-load-10x/main-index-cursor/call-stack-attribution.md` records aligned triple-write shares of 34.23%/30.25%, value-store shares of 19.52%/19.71%, and Sail ingestion shares of 29.04%/29.97% for `NONE`/`READ_COMMITTED`.

- Observation: Equality-based statement-value resolution alone exceeds the final benchmark budget, while repository lifecycle does not.
  Evidence: the lower-bound probes summarized in `profiles/lmdb-load-10x/main-index-cursor/call-stack-attribution.md` measure model scan/hash at 19.429 ms, new equality-map resolution at 124.345 ms, existing equality-map lookup at 106.831 ms, and empty full repository lifecycle at roughly 2.8-3.8 ms. Identity lookup is invalid because 179,732 equal values appear as 1,226,197 identities.

- Observation: The public 32-bit `Value.hashCode()` is not selective enough for equality-free bulk resolution, and obtaining distinct Model views changes iteration representation and cost.
  Evidence: the rejected probe documented in `profiles/lmdb-load-10x/main-index-cursor/call-stack-attribution.md` finds 2,517 collision hashes/2,659 colliding values and takes 219-236 ms in stable early rounds. `DynamicModel` upgrades to `LinkedHashModel`; although that canonicalizes identities, scans rise to 43-51 ms and identity resolution to 62-81 ms.

- Observation: Canonical component identities do not help while every downstream LMDB layer still performs its own equality-based indexing.
  Evidence: `profiles/lmdb-load-10x/canonical-dynamic-model/run-1-jdk26.json` measures 771.802 ms/op for `NONE` and 912.047 ms/op for `READ_COMMITTED`, 9.34% and 27.95% slower than the retained main-cursor checkpoint. All 27 model tests passed before the experiment was removed.

- Observation: A single iterable call can cross the repository and Sail layers without changing duplicate, context, listener, ordering, or metrics semantics; holding the `Changeset` lock across that iterable removes a visible READ_COMMITTED cost but does not materially alter NONE.
  Evidence: all 10 `SailRepositoryConnectionTest` and 8 `ChangesetTest` tests pass. Short paired runs under `profiles/lmdb-load-10x/bulk-conduit` measure READ_COMMITTED at 772.575 ms/op before and 742.816 ms/op after the one-lock change, while NONE remains in the current-host 756-772 ms range.

- Observation: Bypassing scalar `LmdbSailSink.approve` materially improves NONE and preserves the current READ_COMMITTED checkpoint, but the remaining cost is still dominated below the Sail API.
  Evidence: two paired JDK 26 runs under `profiles/lmdb-load-10x/lmdb-iterable-direct` pool to 683.717 ms/op for NONE and 719.733 ms/op for READ_COMMITTED, 12.45% and 13.40% below the original durable baseline. The focused Mockito test proves zero scalar `approve` calls, and five neighboring bulk tests pass.

- Observation: After direct ingestion, native cursor puts and collision-safe equality work dominate both isolation modes; READ_COMMITTED changeset materialization is comparatively small.
  Evidence: Docker Java 26 CPU-time JFR assigns 27.67%/27.90% self CPU to `nmdb_cursor_put`, while `String.equals`, primitive-map equality/spread, and `ValueStore.getId` contribute roughly another 8-10%. macOS inclusive stacks put `ValueStore` at 21.49%/19.93%, aligned triples at 29.56%/28.48%, and `Changeset.approveAll` at only 0%/2.30% for NONE/READ_COMMITTED.

- Observation: Operation-wide hash sorting is algorithmically correct but destroys the beneficial streaming overlap and makes `ValueStore.storeValues` batch encoding/sorting substantially more expensive.
  Evidence: `profiles/lmdb-load-10x/hash-sorted-values/run-1-short.json` measures 966.514 ms/op for NONE and 1037.307 ms/op for READ_COMMITTED, regressions of 41.36% and 44.12% from the retained direct-ingestion checkpoint. Focused batch, failure, opt-out, and ordering tests passed before the experiment was removed.

- Observation: A fresh-store local value dictionary plus one reserved LMDB block stream removes nearly all legacy value/index I/O, but multiple RDF namespaces caused NONE to materialize that representation immediately.
  Evidence: async-profiler JFR traces `AbstractRepositoryConnection.add` through `LmdbSailSink.setNamespace` to `TripleStore.materializePackedIfNeeded`; the two-namespace regression expected packed count 10,000 and observed 0 before the fix.

- Observation: Preserving packed triples across namespace-only transactions makes NONE substantially faster than READ_COMMITTED, while allocation remains the dominant Java-side difference.
  Evidence: `profiles/lmdb-load-10x/packed-value-dictionary/forked-paired-after-namespace-fix.json` measures NONE at 98.847 ms/op and 107,660,034 B/op and READ_COMMITTED at 152.981 ms/op and 132,219,959 B/op, 7.90x and 5.43x faster than the durable baselines.

- Observation: Local quad IDs fit in 32 bits and the smaller representation improves both memory traffic and current-host elapsed time despite substantial host drift.
  Evidence: the candidate-control-candidate sequence under `profiles/lmdb-load-10x/packed-int-ids` pools to 110.889/163.734 ms/op for NONE/READ_COMMITTED versus the intervening 123.404/170.083 ms/op 64-bit control, while both candidate runs consistently remove about 12.69 MB/op.

- Observation: Avoiding encoded value arrays is not useful when it replaces HotSpot's optimized UTF-8 implementation with two Java character scans and byte-at-a-time direct-buffer stores.
  Evidence: `profiles/lmdb-load-10x/direct-packed-encoding/candidate.json` drops allocation from 94,965,668/119,524,052 B/op to 29,846,125/54,397,857 B/op, yet regresses NONE/READ_COMMITTED from 102.174/157.360 ms/op to 155.472/206.957 ms/op.

## Decision Log

- Decision: Define 10x against the pooled means of two fresh same-machine paired post-adaptive-write runs rather than an older pre-feature state or one noisy run.
  Rationale: The pooled 780.972 and 831.080 ms/op baselines are the strongest reproducible starting point and yield stricter, unambiguous thresholds of 78.10 and 83.11 ms/op.
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

- Decision: End-anchor every Docker and macOS benchmark method regex with `$`.
  Rationale: The benchmark class has a six-index sibling whose name starts with the requested method name. Unanchored selectors invalidate timing and profile artifacts.
  Date/Author: 2026-07-09 / Codex.

- Decision: Test a transaction-owned value-ID dictionary before micro-optimizing encoding loops.
  Rationale: Cross-platform profiles put more than half of sampled CPU in LMDB get/put wrappers. The existing 128-entry value cache and 32-entry namespace-ID cache cannot retain a bulk transaction's working set, so repeated values can cross the native boundary again after eviction. Reducing native operation count has greater Amdahl upside than optimizing sub-2% Java leaves.
  Date/Author: 2026-07-09 / Codex.

- Decision: Keep the transaction-owned dictionaries as the first measured increment and re-profile before the next change.
  Rationale: The exact JDK 26 paired run improved both requested isolation modes and removed 121-187 MB of allocation per operation. The remaining 747-771 ms is too large for Java allocation tuning to reach the target, so the next candidate must reduce native write count or comparison work.
  Date/Author: 2026-07-09 / Codex.

- Decision: Reject the empty-store direct-put fast path and preserve only its evidence.
  Rationale: The LMDB contract is valid and correctness tests passed, but the pooled paired result was flat for `NONE` and worse for `READ_COMMITTED`. Removing a dictionary get is insufficient while index puts dominate.
  Date/Author: 2026-07-09 / Codex.

- Decision: Keep main-index cursor reuse as the second production performance increment.
  Rationale: It preserves `MDB_NOOVERWRITE`, duplicate and promotion behavior, and fallback accounting while avoiding repeated root-level put setup. Two runs improved both requested modes and neighboring auto-grow tests passed.
  Date/Author: 2026-07-09 / Codex.

- Decision: Reject full four-field secondary sorting and retain the existing leading-field sort.
  Rationale: Extra Java radix passes dominate any saved native comparison or page-locality work. The regression is large and symmetric enough that another run would not change the decision.
  Date/Author: 2026-07-09 / Codex.

- Decision: Continue structural duplicate-index experiments before committing an on-disk format change.
  Rationale: The 3.280x low-level result proves that native call coalescing is material, while its remaining 213.872 ms floor is still above both full-benchmark acceptance thresholds. Varying block-group granularity can determine whether a query-compatible blocked duplicate layout has enough remaining upside to justify a migration and read-path refactor.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject a fixed-duplicate production conversion and pursue immutable packed index segments instead.
  Rationale: Even at its optimal bucket shape the four-index fixed-duplicate layout takes 139.411 ms before value resolution or repository lifecycle work. Packed blocks reduce that same synthetic four-index phase to 39.476 ms without append flags or `MDB_WRITEMAP`, leaving enough theoretical budget to target the full benchmark while retaining sorted searchable records.
  Date/Author: 2026-07-10 / Codex.

- Decision: Treat bulk entry, value resolution, and packed indexes as one coordinated ingestion architecture rather than sequential micro-optimizations.
  Rationale: Fresh call-stack attribution and lower-bound probes show that each existing layer independently consumes a substantial fraction of the 78-83 ms target. The implementation needs a reusable-iterable bulk entry point, collision-safe distinct-value resolution, and immutable sorted blocks so it does not pay the per-statement Sail, equality-map, or B-tree costs.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject public-hash partitioning and forced `DynamicModel` upgrade; test canonical lightweight model terms instead.
  Rationale: The measured collision rate requires thousands of equality fallback buckets, while the only public distinct-term views force a representation whose iteration is more than twice as slow. Canonicalizing terms while retaining the original insertion-order map can make identity resolution correct without paying the indexed-model cost.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject model-only canonicalization and keep term representation outside the production LMDB change.
  Rationale: Canonical identities are correct and fully tested but do not bypass the Sail changeset, operation-local maps, transaction value map, or LMDB dictionary. A useful bulk API must carry pre-resolved numeric IDs through those layers rather than merely changing object identity upstream.
  Date/Author: 2026-07-10 / Codex.

- Decision: Keep the default-method bulk-ingestion conduit as an architectural checkpoint and specialize it in LMDB before drawing an end-to-end performance conclusion.
  Rationale: The conduit preserves compatibility and semantics for every existing Sail while eliminating hundreds of thousands of repository/Sail calls for stores that specialize it. Its generic one-lock path helps READ_COMMITTED, and its larger value is that LMDB can now consume one batch without rebuilding a `Changeset` statement model first.
  Date/Author: 2026-07-10 / Codex.

- Decision: Keep the direct LMDB iterable specialization and re-profile before changing value or index layout.
  Rationale: The same-code paired runs retain a substantial NONE improvement and the pooled result improves both modes against the durable baseline. Scalar repository/Sail dispatch is now absent from NONE, making the next profiles more representative of the remaining value resolution and native index costs.
  Date/Author: 2026-07-10 / Codex.

- Decision: Test operation-wide hash-sorted value resolution before the packed-index format change.
  Rationale: Equal values necessarily share a public hash, so a primitive radix ordering can make equality checks sequential and restrict expensive collision handling to the 2,517 known collision hashes. This can remove random hash-map probes while preserving exact equality and the current on-disk format. Packed segments remain necessary afterward because cursor puts alone consume about 28% self CPU.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject hash-sorted value batches and keep the streaming scalar/transaction-cache resolver while pursuing packed index writes.
  Rationale: The measured 41-44% regression overwhelms the 8-10% Java equality cost the experiment targeted. Reducing or reordering value-map probes cannot approach the remaining factor; native per-statement index writes must be collapsed first while preserving the producer/worker overlap.
  Date/Author: 2026-07-10 / Codex.

- Decision: Keep the packed local value dictionary and preserve it across namespace-only transactions.
  Rationale: It reduces NONE to 98.847 ms/op and READ_COMMITTED to 152.981 ms/op without append flags. Namespace changes do not mutate triple/value identity, while any subsequent data mutation in the same store transaction still forces materialization before writing.
  Date/Author: 2026-07-10 / Codex.

- Decision: Encode fresh-load local quad IDs as four 32-bit integers while retaining the 64-bit legacy packed record format.
  Rationale: Dictionary IDs are already Java `int` indices. The candidate halves packed quad bytes, removes roughly 12.69 MB/op of allocation, and wins a bracketing same-host throughput comparison in both isolation modes without changing LMDB flags.
  Date/Author: 2026-07-10 / Codex.

- Decision: Reject the manual direct UTF-8 encoder and retain `String.getBytes(UTF_8)` for packed values.
  Rationale: Allocation fell by about 65 MB/op, but the exact paired result regressed both requested isolation modes by far more than host noise. Any follow-up must preserve JDK bulk encoding or specialize only a proven ASCII representation.
  Date/Author: 2026-07-10 / Codex.

## Outcomes & Retrospective

The 10x optimization loop is active. Commit `9516353b92` preserves the tested adaptive write and batch dictionary work, and the durable two-run baseline fixes the comparison point. No claim of 10x completion is made until both isolation thresholds pass repeated paired JMH runs and the correctness suite remains unchanged apart from the two documented pre-existing failures.

## Context and Orientation

The relevant Maven module is `core/sail/lmdb`. `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/DatagovLoadIsolationBenchmark.java` parses the compressed dataset once at trial setup, then each benchmark invocation creates a fresh temporary LMDB repository, begins the requested isolation level, adds the already parsed RDF4J `Model`, commits, checks that data exists, closes the repository, and deletes the temporary directory. The benchmark therefore measures repository creation, value dictionary work, triple index writes, commit, a final existence check, shutdown, and temporary-directory deletion, but not Turtle parsing.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` connects RDF4J's transaction sink to the LMDB value and triple stores. Its `LmdbSailSink` receives statements, manages transaction ordering, creates `BulkAddQuadsOperation` instances, and optionally executes them on worker threads. A reservation budget bounds the total statement capacity of pending, queued, and executing operations.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` maps RDF values such as IRIs, blank nodes, and literals to numeric IDs. Commit `9516353b92` adds sorted batched ID lookup and creation so a chunk shares one LMDB transaction and reusable native carriers.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` writes numeric subject, predicate, object, and context IDs into the configured LMDB indexes. `storeTriplesAligned` sorts aligned primitive arrays into each index's key order before ordinary LMDB writes. "Aligned" means that the same array position across the four arrays describes one statement; it does not mean LMDB append mode.

`NONE` allows individual approvals to be buffered and asynchronously applied inside the store transaction. `READ_COMMITTED` must retain its isolation semantics and currently takes a different higher-level path. The optimization must accelerate both rather than merely shifting work between them.

## Plan of Work

The first completed milestone preserved the adaptive aligned-write increment. The smallest focused tests covering batched `NONE` approvals and batched value storage passed, the four intended LMDB files were committed as `9516353b92`, and this ExecPlan was committed separately as `f80c490c2f`. Unrelated untracked ExecPlans and configuration artifacts were not staged.

The second completed milestone established a durable baseline. The exact benchmark method ending in `$` excluded the six-index sibling. Both isolation values ran together twice with five one-second warmups, five one-second measurements, one fork, JDK 26, G1, a fixed 2 GiB heap, and the JMH GC profiler. JSON, complete output, and a generated comparison are stored under `profiles/lmdb-load-10x/baseline-macos`.

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

The performance goal passes only when two independent final paired JMH runs show average time at or below 78.10 ms/op for `NONE` and 83.11 ms/op for `READ_COMMITTED`, with `automaticEvaluationStrategy=false`, JDK 26, G1, 2 GiB heap, the default two LMDB triple indexes, one transaction, and no append flags. Report iteration values as well as averages so an outlier cannot hide instability. Allocation should not regress materially unless CPU and wall evidence prove a favorable tradeoff within the 2 GiB budget.

Correctness requires the focused batching, duplicate, estimator, rollback, worker failure, and add/remove ordering tests to pass. `RemoveAddTest` must pass. Full module verification must introduce no new failure beyond the two documented branch failures. Search the final diff and relevant source for `MDB_APPEND` and `MDB_APPENDDUP`; neither may be added or activated.

Profile acceptance requires saved macOS async-profiler artifacts for wall, CPU, and allocation behavior and saved Docker JFR recordings for both isolation modes. The final retrospective must name the original and final dominant hotspots and explain why the measured changes plausibly produced the benchmark delta on JDK 26.

## Idempotence and Recovery

Benchmark and profiling commands create Maven target files, temporary repositories, Docker containers, and profile artifacts but do not modify tracked production data. They are safe to rerun. Copy important results outside `target` immediately because `mvnf` and clean builds remove target artifacts. Do not delete unrelated untracked files; they belong to the user.

If a candidate optimization regresses performance, keep the benchmark/profile evidence in this plan, revert only that candidate's own patch with a forward corrective commit if it was already committed, and resume from the last confirmed commit. Do not use `git reset --hard`, `git clean`, or blanket checkout/restore commands. If Docker or profiling fails transiently, retry once; for a non-transient error, change the invocation or document the missing prerequisite rather than repeating it unchanged.

## Artifacts and Notes

Initial failing and passing TDD evidence for adaptive aligned writes is stored in `initial-evidence.txt`. The last complete LMDB verify log before this plan is `logs/mvnf/20260709-222105-verify.log`. The earlier target-local benchmark was removed by the mandatory clean build. Its durable replacement artifacts are `profiles/lmdb-load-10x/baseline-macos/run-1.json`, `run-1.log`, `run-2.json`, `run-2.log`, and `comparison.md`.

The first performance checkpoint is commit `9516353b92`. Immediately before that commit, the two focused tests for batched `NONE` approvals and single-transaction batch value storage passed with zero failures.

The raw macOS async-profiler artifacts, exact Linux JFR recordings, and a concise interpretation are stored under `profiles/lmdb-load-10x/async-profiler`, `profiles/lmdb-load-10x/docker-jfr`, and `profiles/lmdb-load-10x/profiling-summary.md`. The unanchored `profiles/lmdb-load-10x/docker-jfr/none.jfr` is invalid and is intentionally excluded from evidence commits.

Fresh pooled baseline summary:

    NONE            780.972 ms/op   358449634.800 B/op
    READ_COMMITTED  831.080 ms/op   472907787.600 B/op

Target summary:

    NONE            <= 78.10 ms/op
    READ_COMMITTED  <= 83.11 ms/op

## Interfaces and Dependencies

One source-compatible default bulk method is added to the existing repository-to-Sail ingestion interfaces so stores can specialize an iterable without breaking scalar implementations; no persisted configuration is added. Production work should otherwise remain focused on LMDB and preserve `LmdbStoreConfig`, isolation, and transaction contracts. Existing LWJGL LMDB bindings, JDK primitive arrays, repository sort utilities, JMH, async-profiler 4.4, Docker, and JDK 26 JFR are the default tools. A new dependency is permitted only if profile evidence shows it removes a material cost that in-repository primitives cannot address, and only after a dependency health check.

The key internal interfaces are `LmdbSailStore.LmdbSailSink`, `LmdbSailStore.BulkAddQuadsOperation`, `ValueStore.storeValues(Value[], long[], int)`, and `TripleStore.storeTriplesAligned(long[], long[], long[], long[], int, boolean)`. Their internal implementations may change, but the final system must preserve statement identity, duplicate handling, explicit/inferred separation, context semantics, rollback, ordering, and reservation release.

Revision note (2026-07-09 22:27Z): Created the initial self-contained plan after activating the 10x goal, reading all selected skill instructions, confirming async-profiler availability, and completing the mandatory root build. The plan fixes the workload and acceptance thresholds before further optimization.

Revision note (2026-07-09 22:28Z): Recorded the first committed checkpoint and its focused green verification so future benchmark and profile artifacts have an exact code revision.

Revision note (2026-07-09 22:33Z): Replaced the earlier single-run thresholds with stricter pooled thresholds from two fresh same-code baselines and recorded durable artifact paths and observed host drift.

Revision note (2026-07-09 22:58Z): Recorded the complete macOS and Linux profile matrix, the selector-anchor correction, cross-platform hotspot agreement, lost-sample confidence limits, and the first operation-count optimization hypothesis.

Revision note (2026-07-09 23:14Z): Recorded the first post-profile optimization, its focused red/green evidence, complete value-store test result, and exact JDK 26 time/allocation improvement.

Revision note (2026-07-09 23:18Z): Recorded fresh post-cache CPU profiles and confirmed that the next optimization still needs to reduce native B-tree work.

Revision note (2026-07-09 23:30Z): Recorded and rejected the empty-dictionary conditional-put experiment after a confirmation run disproved the first run's apparent gain.

Revision note (2026-07-09 23:39Z): Recorded the confirmed main-index cursor increment, its focused TDD evidence, neighboring fallback verification, and two paired JDK 26 results.

Revision note (2026-07-09 23:48Z): Recorded and rejected full secondary-key sorting after its first paired run produced a clear two-mode regression.

Revision note (2026-07-10 00:06Z): Recorded the append-free `MDB_MULTIPLE` structural probe, its 3.280x pure insertion gain, and the decision to test block grouping before changing the persisted index format.

Revision note (2026-07-10 00:18Z): Recorded the bucket-shape sweep, the four-index fixed-duplicate floor, the rejected `MDB_WRITEMAP` probe, and the 39.476 ms immutable packed-block result that motivates the next design milestone.

Revision note (2026-07-10 00:31Z): Added fresh two-isolation async-profiler call-stack attribution plus Java model/value and empty-lifecycle lower bounds, establishing that the next milestone must collapse all three ingestion layers together.

Revision note (2026-07-10 00:40Z): Recorded and rejected the public-hash/distinct-view resolver after collision counts and model-upgrade timings disproved its expected advantage.

Revision note (2026-07-10 00:53Z): Recorded the TDD-verified but benchmark-rejected map-backed canonical-term experiment and removed its production/test changes.

Revision note (2026-07-10 05:14Z): Recorded the retained 32-bit local-ID format, its candidate-control-candidate measurements, and its allocation and storage reductions.

Revision note (2026-07-10 05:23Z): Recorded and removed the correctness-tested direct UTF-8 encoder after allocation gains failed to translate into elapsed-time gains.
