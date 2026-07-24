# LMDB native engine acceptance ledger

This ledger is maintained by `IMPLEMENTATION.md` and `13-verification.md`. A row closes only when its
correctness gate passes and a paired benchmark or counter artifact meets the stated target.

| Status | Target | Harness | Evidence |
| --- | --- | --- | --- |
| Open | Two concurrent large BGPs each at most 0.6 times sequential | ParallelismBenchmark | Pending |
| Open | Filtered two-pattern join at least 2 times faster | ThemeQueryBenchmark | Sequential/worker movement parity and four-worker lease ownership green; paired timing pending |
| Open | Batch-versus-parallel overlap at least 2 times faster | ParallelismBenchmark | Pending |
| Open | ORDER BY producer phase scales with cores | OrderByBenchmark | Ordered full-sort parity and composed `parallelPipelines | orderedFullSort` dispatch are green; controlled one-versus-N-core timing pending |
| Open | Sort moves three to four times fewer bytes | OrderByBenchmark | A 12-slot plan records width 12→4 and 256 packed values for 64 rows; the 50,000-row full-sort smoke is 405.866 ms/op, but no paired wide-row control exists |
| Open | Wide-row sort comparisons touch only the key prefix | OrderByBenchmark/async-profiler | Key-first layout, key-only spill merge, and deferred payload reconstruction are green; the available one-column CPU profile is not a qualifying wide-row load profile |
| Open | Radix-eligible ORDER BY at least 2.5 times faster | OrderByBenchmark | Pending |
| Open | Order-eliminated shape performs zero sort work | OrderByBenchmark | Pending |
| Open | 0.1 percent range filter at least 10 times faster | RangeFilterBenchmark | Pending |
| Met | Scan decode throughput improves at least 15 percent | ScanBenchmark | [3.047 → 1.053 ms/op; 65.441% lower time](../../profiles/lmdb-native-engine/scan/wide-varint-comparison.md) |
| Open | Probe-bound hash join improves 1.5 to 2 times | HashJoinBenchmark | Pending |
| Open | NOT EXISTS over one-million-row outer improves at least 3 times | HashJoinBenchmark | Pending |
| Open | Triangle probe count follows sum-of-minimum-runs scaling | CsrBenchmark | Pending |
| Met | Cached hub root partitions avoid LMDB cursors and balance pair work | LmdbCsrPartitionedScanTest | Four requested slices contain `[16, 16, 16, 16]` rows, cover 64 unique pairs, and increment the in-memory CSR root-scan counter four times; 27 CSR/range neighbors green |
| Open | Large-heap CSR cache hit-byte effectiveness improves | CsrBenchmark/cache telemetry | Admission/refusal, close-leak, and concurrent correctness green; paired corpus measurement pending |
| Open | Correlated accumulate reduces 1,000 executions to one | CorrelatedBenchmark | Pending |
| Open | Repeated-start path traversals reduce from N to D | PathBenchmark | Pending |
| Met | Compatible alternation, inverse, and same-variable cycles dispatch natively | LmdbNativePropertyPathTest/LmdbNativePathPlannerTest | Generic parity for alternation, inverse, star, siblings, and same-variable cycles; native engagement increases, while sequence/NPS/graph-variable gates stay generic; 46/46 green |
| Met | Path cost flips monotonically around a selective sibling | LmdbNativePathPlannerTest | Fan-out 2 costs 16 below sibling 32; fan-out 3 costs 81 above it; backward fan-out costs 16, reachable cap is 10,000, and completed memo observation is exactly 3; 4/4 green |
| Open | Deep-hierarchy and hub-heavy frontier paths improve at least 3 times serially | PropertyPathReachabilityBenchmark | Sorted/seek/CSR correctness green; conforming one-million-node paired timing pending |
| Open | Wide-frontier parallel paths scale near-linearly | PropertyPathReachabilityBenchmark | Deterministic SET and worker lifecycle green; conforming multi-core scaling run pending |
| Open | Single-pattern COUNT answers in microseconds | CsrBenchmark | Pending |
| Open | Fifty-million-group aggregation completes within budget | AggregationBenchmark | Pending |
| Open | Eight-key primitive group probes stay within 1.5 times width two | AggregationBenchmark/allocation profile | Eight-key generic parity and `primitiveTupleGroups` dispatch green; timing/allocation pending |
| Open | Query memory ledger returns to zero after every corpus query | Memory leak harness | Concurrent reserve/grow/reclaim and cursor-close unit contract is 6/6 green; operator wiring and corpus harness pending |
| Met | 100 MiB lexical values never enter the shared value cache | ValueStoreCacheTest | Exact 100 MiB retained-entry red; post-fix focused and full cache class green |
| Open | Dictionary-heavy materialization improves default cache hit rates | Materialization benchmark/cache telemetry | Adaptive byte-sized defaults and hit-rate counters pending |

Revision note (2026-07-19, Codex): Created the Phase I acceptance ledger from the program-level targets
in `13-verification.md`.

Revision note (2026-07-20, Codex): Closed the scan-decode row after paired 10-by-2-second JMH runs of
the former byte-at-a-time decoder and exact-width native loads from the same jar and fixture.

Revision note (2026-07-20, Codex): Added plan 07 §4's large-heap cache-effectiveness gate as open. The
policy counters and lifecycle correctness gates are green, but no paired benchmark-corpus measurement exists.

Revision note (2026-07-20, Codex): Closed plan 07 §5's structural cached-partition gate with an exact
prefix-sum skew fixture: four equal 16-row slices cover all 64 pairs and all four opens dispatch through the
in-memory CSR iterator. Throughput remains unclaimed because no paired benchmark or profile was run.

Revision note (2026-07-20, Codex): Recorded plan 02 §4's sequential/parallel movement and worker-lease
correctness evidence against the filtered-join target. The row remains open until paired timing meets the
two-times threshold.

Revision note (2026-07-20, Codex): Added plan 11 §3's serial locality and parallel-scaling gates as open.
Sorted-frontier, CSR, seek, SET-determinism, and worker-close correctness are green, but no conforming
one-million-node or multi-core paired benchmark has been run.

Revision note (2026-07-20, Codex): Closed plan 11 §§4–5's dispatch and planner-cost contracts with native
alternation/inverse/cycle engagement plus a direction-aware 16→81 fan-out cost flip and exact memo value 3.
These are structural/counter gates; deep/hub timing, parallel scaling, allocation, JFR, and JIT remain open.

Revision note (2026-07-20, Codex): Added plan 10 §2's width-eight primitive grouping target as open.
Query-level parity and dispatch are green; no paired allocation profile or probe-throughput comparison was
run.

Revision note (2026-07-20, Codex): Recorded plan 12 §1's core memory-authority contract. Exact concurrent
accounting and cursor-close zeroing are green, but the acceptance row stays open until every query-side
structure is wired and the complete corpus leak harness returns the shared process ledger to zero.

Revision note (2026-07-20, Codex): Closed plan 12 §2's oversized-entry safety gate with the exact 100 MiB
fixture. The separate default-sizing/hit-rate row stays open until byte-aware cache defaults and telemetry
are implemented and measured.

Revision note (2026-07-20, Codex): Recorded plan 05 §§1–3's ordered-producer dispatch, 12→4 dense-row
telemetry, and key/payload spill correctness. A forked 50,000-row full-sort smoke and CPU sample establish
the current workload only; core scaling, a paired wide-row control, and a qualifying key-load profile remain
open, so no performance row is closed.
