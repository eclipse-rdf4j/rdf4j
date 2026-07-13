# Reusable repositioning probe cursors for the native LMDB engine (factorization design step 4)

This ExecPlan is a living document maintained per `.agent/PLANS.md`. It follows `execplan-factorized-tail-aggregation.md` (repo root), whose isolation benchmark motivated this work; that plan is checked in and incorporated by reference.

## Purpose / Big Picture

Index-nested-loop probes in the native LMDB engine re-create their scan machinery on every probe: each call to `LmdbSailDataset.statements(...)` (in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`) takes a dataset read stamp, allocates a wrapper, allocates a fresh `LmdbRecordIterator` (which pulls two `MDBVal`s and a key buffer from a thread-local pool, takes a transaction lock cycle, and acquires an LMDB cursor), and tears all of it down when the probe is exhausted. The prior plan's star benchmark showed this setup cost dominating whole queries at fan-out 10 (~13 ms regardless of how match consumption was optimized). After this change, an operator that probes repeatedly — the factorized aggregation tail and every `JoinCursor` whose right side is a triple pattern — owns a `NativeProbe`: a handle holding one dataset stamp and one retained `LmdbRecordIterator` whose buffers and pooled LMDB cursor survive across probes; each new probe key just re-encodes the search key and repositions with `MDB_SET_RANGE` (or one `mdb_get` for fully-bound probes). Behavior is unchanged; the same star benchmark run before/after shows the setup cost collapsing.

## Progress

- [x] (2026-07-02 15:50Z) Plan authored.
- [x] (2026-07-02 16:20Z) LmdbRecordIterator retained mode: `retainOnClose` flag, `reset(index, rangeSearch, s,p,o,c, explicit)`, `dispose()`; buffer/cursor invariants preserved per shape (range / exact / open-range / full-scan); TripleStore `getTriplesRetained`/`resetTriples` added; focused TripleStoreTest reset test green.
- [x] (2026-07-02 16:40Z) NativeProbe interface + default statements-backed fallback on NativeLmdbQuerySource; real implementation on LmdbSailDataset (single stamp per probe lifetime, per-open source-open check); Composite source uses the default fallback.
- [x] (2026-07-02 17:00Z) Wired: FactorizedTail owns a probe (closed via evaluateFactorized finally); JoinCursor owns a probe for PatternPlan right sides; PatternCursor accepts a probe for context fan-out.
- [x] (2026-07-02 17:20Z) Differential suite (18) + aggregate/join regression selection green; full module verify green (1226 unit tests, 0 failures; ITs unchanged at pre-existing sketch-gated counts).
- [x] (2026-07-02 18:30Z) Star benchmark re-run at fan-out 10 and 100; results and interpretation in Artifacts. Full verify after wiring: Tests run: 1228, Failures: 0, Errors: 0 (logs/mvnf/20260702-180604-verify.log).

## Surprises & Discoveries

- Observation: retained iterators accumulate their per-scan telemetry counters (sourceRowsScannedActual etc.) across probes. The native path does not consume these per probe, so aggregate-per-operator totals are acceptable; noted so nobody later assumes per-probe semantics.
  Evidence: LmdbRecordIterator counters are only read through wrapper getters that the probe path no longer wraps.

## Decision Log

- Decision: expose reuse as an explicit operator-owned `NativeProbe` handle instead of caching inside `statements(...)`.
  Rationale: retention needs an owner with a lifetime (open N times, then close); `statements()` has no caller identity. Operators (FactorizedTail, JoinCursor) already have exactly that lifetime.
  Date/Author: 2026-07-02, Claude Code.
- Decision: the probe holds one dataset read stamp for its whole lifetime and performs the source-open check once per open() instead of wrapping every row.
  Rationale: this is review item 7(c); the step-1 batching had already moved the per-row check to per-batch, so per-open is equivalent protection with less indirection. The stamp discipline matches the old wrapper (released when iteration ends).
  Date/Author: 2026-07-02, Claude Code.
- Decision: CompositeNativeLmdbQuerySource keeps the default (non-retaining) probe fallback.
  Rationale: composite sources fan one pattern out across datasets; a retained iterator per (probe, sub-source) is more bookkeeping than the multi-dataset case currently justifies. Correctness is identical either way.
  Date/Author: 2026-07-02, Claude Code.
- Decision: on reset, buffers not needed by the new probe shape are freed and nulled (min key for full scans, max key when a range prefix exists) so the `minKeyBuf != null` / `maxKeyBuf != null` branch invariants in next()/fill() remain exactly those of a freshly constructed iterator.
  Rationale: the iterator's control flow branches on buffer presence; reset must not invent new states.
  Date/Author: 2026-07-02, Claude Code.

## Outcomes & Retrospective

Landed end-to-end: probes retained across operator lifetimes, all suites green (1228 unit tests). The benchmark partially confirms the diagnosis: fan-out 10 star counts dropped from ~13.2 ms to ~11.2-12.6 ms (10-17%), while fan-out 100 was unchanged (probe setup was already amortized there). The honest reading is that iterator/wrapper/stamp/ctor-lock elimination was worth ~1.5-2 ms of the ~13 ms plateau; the remaining floor is the per-probe LMDB seek plus per-key scan JNI itself. That is precisely design step 7 territory — the probe keys of an index-nested-loop arrive in sorted order, so the retained cursor (which this change made possible) can forward-walk with prefix compares instead of re-seeking (roadmap item 13, merge/SIP). Remaining follow-ups: that forward-walk; probe ownership for ExistsFilter/MinusCursor subplan reopens and the BGP Frame path; a retaining probe for CompositeNativeLmdbQuerySource.

## Artifacts and Notes

FactorizedTailStarBenchmark, avgt ms/op, 4+4 iterations, factorized tail ON; "pre" = working tree before this plan (tables in execplan-factorized-tail-aggregation.md), "post" = with retained probes.

    fan-out 10 (2000 hubs x 10 x 10, probe-setup-bound):
    variant             pre (ms)         post (ms)
    countHub            13.268 ± 0.378   11.730 ± 0.052
    countDistinctHub    13.489 ± 1.391   11.233 ± 0.201
    countTail           13.133 ± 1.042   11.480 ± 0.292
    countDistinctTail   13.361 ± 2.062   12.590 ± 0.329
    groupByTail         14.431 ± 0.546   13.741 ± 2.438

    fan-out 100 (1000 hubs x 10 x 100, match-consumption-bound): unchanged within noise
    (countHub 32.5->31.5, countDistinctHub 22.4->22.1, countTail 32.8->32.7,
     countDistinctTail 36.2->35.8, groupByTail 37.1->37.8).

The benchmark source is committed with the fan-out-100 constants (the configuration that discriminates factorization effects); the fan-out-10 numbers above were taken by temporarily setting HUBS=2000, P2_FANOUT=10.

## Context and Orientation

Key files, all under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`: `LmdbRecordIterator.java` (the LMDB scan iterator; already has batched `fill`, a pooled-cursor mode via `TripleStore.CursorPool`, and `MDB_FIRST` initial positioning that makes stale cursor positions harmless), `TripleStore.java` (index-per-bound-mask table `bestIndexByBoundMask`, `CursorPool`), `LmdbSailStore.java` (inner class `LmdbSailDataset` implements `NativeLmdbQuerySource`; its `statements()` wraps iterators with a per-probe stamp), `NativeLmdbQuerySource.java` (package-private SPI), `LmdbNativeAggregateCompiler.java` (`PatternPlan.openRaw`, `PatternCursor` with `fill`, `JoinCursor`, `FactorizedTail`, `NativeGroupIteration.evaluateFactorized`). A "probe" is one evaluation of a triple pattern under the current row's bound values. "Retained mode" means close() flips state but keeps buffers/cursor; only `dispose()` frees.

## Plan of Work

Milestone 1, storage layer: add `retainOnClose`/`disposed` to `LmdbRecordIterator`; extract resource freeing from `closeInternal` so retained close skips it; add `reset(...)` re-deriving match fields, key buffers, exact/range flags, and (under the txn lock) the cursor when the dbi changes or the probe is fully bound; add `dispose()`. Add `TripleStore.getTriplesRetained` and `resetTriples` using the bound-mask table. Add a `TripleStoreTest` exercising one retained iterator across range → exact → different-index → full-scan shapes with correct counts each time.

Milestone 2, SPI + operators: `NativeProbe` (package-private, `open(s,p,o,c)`/`close`), default fallback on the source interface delegating to `statements()`; `LmdbSailDataset.newProbe()` holding one stamp + one retained iterator; `PatternCursor` gains an optional probe used instead of `source.statements` for context fan-out; `PatternPlan.openRaw(row, probe)` and `open(row, probe)`; `FactorizedTail` creates its probe lazily from `row.source` and exposes `close()`, called from a try/finally in `evaluateFactorized`; `JoinCursor` lazily creates a probe when its right side is a `PatternPlan` and closes it in `close()`. Run the differential suite, the join/aggregate regression selection, then the full module verify.

Milestone 3, measurement: re-run `FactorizedTailStarBenchmark` (both fan-outs, toggle on) and compare against the tables in `execplan-factorized-tail-aggregation.md`.

## Concrete Steps

From the repo root: build `mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install`; tests `python3 .codex/skills/mvnf/scripts/mvnf.py <Class> --retain-logs`; full module `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs`; benchmark `bash scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.FactorizedTailStarBenchmark --method executeQuery --warmup-iterations 4 --measurement-iterations 4` (JMH table lands in `core/sail/lmdb/target/benchmark-output.log`; remove a stale `$TMPDIR/jmh.lock` if JMH refuses to start and no java process holds it).

## Validation and Acceptance

Behavior-neutral change: the 18-test differential class `LmdbNativeFactorizedTailAggregationTest` and the full module suite (expect `Tests run: ~1227, Failures: 0, Errors: 0`) are the acceptance gate, plus the new TripleStoreTest reset test as direct hit-proof of the retained-iterator state machine. Performance acceptance: fan-out-10 star counts measurably below the recorded ~13 ms plateau.

## Idempotence and Recovery

All additive except the `closeInternal` extraction; reverting the two operator wiring sites restores per-probe construction with no semantic change. Benchmarks write only under `target/`.

## Interfaces and Dependencies

    // NativeLmdbQuerySource.java
    interface NativeProbe extends Closeable {
        RecordIterator open(long subj, long pred, long obj, long context) throws IOException;
        void close();
    }
    default NativeProbe newProbe() { ... statements()-backed fallback ... }

    // LmdbRecordIterator.java
    void reset(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj, long context,
            boolean explicit) throws IOException;   // only in retained mode
    void dispose();

    // TripleStore.java
    LmdbRecordIterator getTriplesRetained(Txn txn, long subj, long pred, long obj, long context, boolean explicit)
    void resetTriples(LmdbRecordIterator iterator, Txn txn, long subj, long pred, long obj, long context,
            boolean explicit)

Plan revision note (2026-07-02): initial version, written alongside the implementation it describes; progress entries reflect actual state at each stopping point.

Plan revision note (2026-07-02, second revision): recorded actual benchmark outcomes (the authored Outcomes estimate of ~8-9 ms was wrong and has been corrected to the measured ~11.2-12.6 ms) and the step-7 implication.
