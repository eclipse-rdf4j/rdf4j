# LMDB native engine acceptance ledger

This ledger is maintained by `IMPLEMENTATION.md` and `13-verification.md`. A row closes only when its
correctness gate passes and a paired benchmark or counter artifact meets the stated target.

| Status | Target | Harness | Evidence |
| --- | --- | --- | --- |
| Open | Two concurrent large BGPs each at most 0.6 times sequential | ParallelismBenchmark | Pending |
| Open | Filtered two-pattern join at least 2 times faster | ThemeQueryBenchmark | Pending |
| Open | Batch-versus-parallel overlap at least 2 times faster | ParallelismBenchmark | Pending |
| Open | ORDER BY producer phase scales with cores | OrderByBenchmark | Pending |
| Open | Sort moves three to four times fewer bytes | OrderByBenchmark | Pending |
| Open | Radix-eligible ORDER BY at least 2.5 times faster | OrderByBenchmark | Pending |
| Open | Order-eliminated shape performs zero sort work | OrderByBenchmark | Pending |
| Open | 0.1 percent range filter at least 10 times faster | RangeFilterBenchmark | Pending |
| Open | Scan decode throughput improves at least 15 percent | ScanBenchmark | Pending |
| Open | Probe-bound hash join improves 1.5 to 2 times | HashJoinBenchmark | Pending |
| Open | NOT EXISTS over one-million-row outer improves at least 3 times | HashJoinBenchmark | Pending |
| Open | Triangle probe count follows sum-of-minimum-runs scaling | CsrBenchmark | Pending |
| Open | Correlated accumulate reduces 1,000 executions to one | CorrelatedBenchmark | Pending |
| Open | Repeated-start path traversals reduce from N to D | PathBenchmark | Pending |
| Open | Single-pattern COUNT answers in microseconds | CsrBenchmark | Pending |
| Open | Fifty-million-group aggregation completes within budget | AggregationBenchmark | Pending |
| Open | Query memory ledger returns to zero after every corpus query | Memory leak harness | Pending |

Revision note (2026-07-19, Codex): Created the Phase I acceptance ledger from the program-level targets
in `13-verification.md`.
