# Frontier OmniSketch calibration record

Date: 2026-07-25
JDK: 25
Scope: bounded local LMDB calibration and format-based billion-row sizing

This record separates measurements from projections. It does not claim production accuracy, a required disk size,
or optimizer-regret calibration.

## Persisted payload sizing

The current payload record is twelve 64-bit values, or 96 bytes. The builder uses a conservative 144-byte
per-record selection envelope and targets 75 percent of that record cap. A 64 KiB block adds 48 bytes of framing.
With four lanes and two directions, the current proposed budget tiers model the following residual-center inclusion
rates:

| Rows | Configured budget | Target records | Residual center rate | Wire payload | 1.4x footprint |
|---:|---:|---:|---:|---:|---:|
| 1B | 0.5 GB | 2,604,166 | 0.032552% | 0.250 GB | 0.350 GB |
| 1B | 2.5 GB | 13,020,833 | 0.162760% | 1.251 GB | 1.751 GB |
| 1B | 5 GB | 26,041,666 | 0.325521% | 2.502 GB | 3.503 GB |
| 10B | 5 GB | 26,041,666 | 0.032552% | 2.502 GB | 3.503 GB |
| 10B | 25 GB | 130,208,333 | 0.162760% | 12.509 GB | 17.513 GB |
| 10B | 50 GB | 260,416,666 | 0.325521% | 25.018 GB | 35.026 GB |

To target 0.1, 0.5, and 1.0 percent residual-center inclusion with this format and envelope, configured budgets
would be approximately 1.536, 7.680, and 15.360 GB at 1B rows, or 15.360, 76.800, and 153.600 GB at 10B rows.
Exact-heavy records share the same configured cap and can reduce the residual rate while preserving high-degree mass.
These are deterministic format projections, not materialized billion-row builds.

Reproduce:

```text
python3 scripts/frontier-payload-sizing.py
```

## Bounded LMDB audit

The bounded audit loaded the existing mixed-domain fixture, built a 128 KiB Frontier generation, evaluated the first
30 generated audit queries and all 374 algebra pieces, inspected 210 pieces whose planned source was
`lmdb-frontier`, then committed 16 exact additive insert generations.

```text
queries=30
auditedPieces=374
frontierPieces=210
p95QError=26.588921
worstQError=1330.268222
falseZeros=0
intervalCoverage=not-certified
optimizerRegret=not-measured
buildMillis=67.351
durableBytes=89084
insertP95Millis=58.420
queryMemoryPeakBytes=3882168
```

Evidence:

```text
python3 .codex/skills/mvnf/scripts/mvnf.py \
  LmdbEstimateAuditHarnessTest#reportsBoundedFrontierCalibrationWithoutPromotingDefaults \
  --module core/sail/lmdb --retain-logs
logs/mvnf/20260725-192835-verify.log
initial-evidence.frontier-bounded-calibration-green.txt
```

## Calibration outcome

This audit did not justify promotion:

- False authoritative zeros: pass in this bounded run.
- Query-memory cap: pass; 3.88 MB peak is below 64 MiB.
- p95 q-error below 5: fail; measured 26.59.
- Cached incremental additions below 1 ms p95: fail; the 16-commit end-to-end sample measured 58.42 ms.
- Nominal interval coverage: not eligible; sampled summaries currently publish no certified interval.
- Optimizer runtime regret: not measured; the bounded audit has no exhaustive runtime-plan oracle.
- Billion-row build time and physical disk usage: not measured; only the exact wire-format projection above is
  available.

No production calibration or required budget should be claimed from these results.

## Subsequent rollout decision

On 2026-07-25 the product owner explicitly chose to roll Frontier out as LMDB's primary synopsis despite the failed
quality and insertion-latency gates above. The configured persistent maximum is now 512 MiB and a missing
positive-budget base generation is built during store initialization. This is an operational product decision, not
new calibration evidence: the q-error, latency, certified-interval, optimizer-regret, and billion-row limitations
recorded above remain open rollout risks. OFF and zero-budget configurations remain available, and unsupported,
dirty, or unavailable Frontier evidence continues to use the scalar/sketch fallback.

## Default-on hardening follow-up

The rollout does not publish an unbounded exact payload chain. A store publishes at most eight immutable insert
generations, retains a durable dirty marker after further commits, and consolidates the complete committed snapshot
on the next supported authoritative query. Payload serialization now buffers each bounded block before updating its
checksum and digest, removing the prior per-primitive stream-write cost.

The updated bounded calibration completed with:

```text
queries=30
auditedPieces=374
frontierPieces=206
p95QError=26.588921
worstQError=1330.268222
falseZeros=0
buildMillis=38.432
durableBytes=164076
insertP95Millis=61.079
queryMemoryPeakBytes=533632
```

This follow-up validates bounded maintenance and lazy recovery; it does not change the failed accuracy and latency
promotion gates above. Serializable transactions use the scalar planner because speculative exact Frontier probes
would otherwise widen their observed-state set. Property-path sessions also remain scalar until their retained-state
transform is theorem-safe. The final LMDB module run executed 1,651 tests with zero errors and one unrelated
benchmark-parameter failure.

Evidence:

```text
initial-evidence.frontier-bounded-insert-green.txt
initial-evidence.frontier-serializable-green.txt
initial-evidence.frontier-rollout-audit-green.txt
initial-evidence.frontier-rollout-lmdb-broad-final.txt
```
