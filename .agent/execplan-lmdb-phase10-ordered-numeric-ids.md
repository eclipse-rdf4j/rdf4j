# Value-ordered numeric ID encoding, decode-free compares, range pushdown, min/max stats

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB store inlines small literals directly into their 64-bit value ids (bit 0 = double flag, bits 1-6 = type code, bits 7+ = value; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueIds.java`). Today's integer encodings use ZigZag (`inlined/Integers.java:37-49`), so the raw long order of two ids does NOT match numeric order, and every `FILTER(?v > 60)` or ORDER BY over numerics must bit-decode each id (allocating BigDecimals in the general path). This phase adds ORDERED numeric type codes whose value field is biased/offset-binary (`value + 2^55`), making same-code id comparison a raw long compare — and, because index keys are order-preserving unsigned varints of the compound id, making `FILTER(?v > const)` on a pattern's object translatable into a bounded index range scan (reusing Phase 7's `LmdbKeyRange`/`getTriplesRange`).

Backwards compatible by construction: ids are self-describing via their type bits, so old (ZigZag) and new (ordered) ids can coexist; the persisted store property decides only what NEW writes use; existing stores are never rewritten (they keep legacy encoding forever); `LmdbStore.VERSION` bumps to 3 so pre-change RDF4J refuses stores that may contain the new codes. Rollout per user decision: new stores default ON.

After this change, on a freshly created store: `FILTER(?value > 60)` over an integer-valued predicate evaluates without decoding candidate ids (compile-time-encoded per-code thresholds), and (M3) the pattern scan itself is bounded to the qualifying object-id range. Observable via plan snapshots (bounded scan), benchmarks, and new counters.

## Progress

- [x] (2026-07-15 21:20Z) Design finalized; free type codes verified: 5-15 and 36-63; store version gate verified (`LmdbStore.java:278-299`, `StoreProperties`).
- [x] (2026-07-16 00:20Z) M0 DONE (Routine A): the `>> 2` was a real bug — nextId is the VALUE-space counter but recovery kept 5 extra bits, inflating the id space ~32x per reopen. Failing test `ValueStoreTest#reopenRecoversCompactNextIdCounter` (pre-fix: first=2 second=65), fix = `ValueIds.getValue(compound) + 1`, post-fix ValueStoreTest 26/26 green.
- [x] (2026-07-16 00:45Z) M1 DONE: `T_ORD_INTEGER..T_ORD_NON_POSITIVE_INTEGER` (codes 36-44, signed family only — the unsigned family turned out to use ZigZag too, so it stays legacy), biased encode/decode in `Integers`, `Values.packLiteral(literal, ordered)` + unpack cases, writer gate via `StoreProperties.NUMERIC_ID_ENCODING_KEY` written at creation (`LmdbStore`, `VERSION = 3`), `ValueStore.orderedNumericIds` from the persisted property, codec decode cases, `numericType` extension in the expression compiler (IsNumeric). `LmdbOrderedNumericIdsTest` 6/6; fuzz green on ordered-default stores. TWO pre-existing bugs found and fixed along the way (see Surprises).
- [x] (2026-07-16 01:00Z) M2 DONE: `orderCompare` raw-compare fast path for two ordered ids; `CachedCompareFilter` gains `constantIntegerValue` (exact integer constants, incl. ordered-id constants) and compares `ValueIds.orderedIntegerValue(id)` as one long compare before memo/decode. Fuzz 18/18 + LmdbNativeOrderedFactorizedTest 20/20 green.
- [ ] M3 DEFERRED to the successor plan (see Decision Log): sound pushdown REQUIRES per-predicate type stats — implemented in M4 — and the better design discovered (serve eligible root scans wholly from CSR entries) subsumes pushdown for cached predicates.
- [x] (2026-07-16 01:10Z) M4 (stats half) DONE: CSR build sweeps record `neighborMinId`/`neighborMaxId`/`allNeighborsOrderedIntegers` per entry (the pushdown soundness gate); consumers land with M3 in the successor plan.

## Surprises & Discoveries

- Observation: The naive M3 range pushdown is UNSOUND without type statistics. A pushed integer-window
  scan restricts candidates to ids whose value fields fall in the window — but matching doubles, floats,
  decimals, and non-inlined (stored) numerics live elsewhere in id space, and the residual filter can only
  REMOVE rows, never restore dropped ones. Soundness gate = "every object of this predicate is an
  ordered-integer id", which is exactly the M4 stat now recorded on CSR entries.
  Evidence: reasoning over id layout — biased integer value fields share one axis; T_DOUBLE ids (bit 0 set)
  and stored-literal reference ids interleave arbitrarily on that axis.
- Observation: A second, better design emerged: for predicates with a valid BY_SUBJECT CSR entry, an
  eligible ROOT scan (`?s p ?v`) can be served ENTIRELY from the entry (all keys × runs, spoc order,
  zero LMDB access) — strictly stronger than bounding the LMDB scan, for cached predicates.
- Observation (pre-existing bug #1, fixed): `StoreProperties` setters OVERWROTE the dirty flag
  (`dirty = !equals(...)`), so the last same-value setter call wiped pending changes and `save()` no-oped —
  new stores never persisted `store.properties` at all. Fixed to accumulate (`dirty = dirty || ...`);
  pinned by `LmdbOrderedNumericIdsTest#newStoreRecordsOrderedEncodingAndServesQueries`.
- Observation (pre-existing bug #2, fixed): `ValueStore.initTermIndexes` persisted the RAW config
  triple-term-indexes string (possibly null) instead of the EFFECTIVE spec set (defaults applied), so any
  actually-persisted properties file failed `getTripleTermIndexSpecs()` on the next open. Latent while bug
  #1 suppressed the file write; exposed the moment persistence worked. Fixed to persist the effective set;
  pinned by `LmdbOrderedNumericIdsTest#legacyStoreKeepsLegacyEncodingAcrossUpgrade`.

## Decision Log

- Decision: Per-subtype ordered codes (not one unified code) so ids keep round-tripping to their exact xsd datatype; cross-subtype filter compares stay decode-free anyway because a compile-time constant can be pre-encoded once per ordered code.
  Rationale: preserves datatype fidelity with zero storage change.
  Date/Author: 2026-07-15 / approved plan.
- Decision: New stores only, default ON (persisted property + VERSION 3); no migration tool in v1; mixed-encoding comparisons fall back to decode.
  Rationale: user decision.
  Date/Author: 2026-07-15 / user choice.
- Decision: M3 (range-filter pushdown) deferred to the successor plan; M4's stats half landed now on CSR entries.
  Rationale: (1) soundness requires the per-predicate all-ordered-integers stat, which only exists where CSR
  entries exist — so pushdown's natural v1 home is coupled to Phase 8 warmth anyway; (2) the discovered
  serve-root-scans-from-CSR design is strictly stronger for cached predicates and belongs in one coherent
  successor design rather than two overlapping mechanisms rushed in; (3) planner-adjacent surgery under time
  pressure is where silent correctness bugs live — the retrospective mandate explicitly asks what SHOULD be
  built, and this is it.
  Date/Author: 2026-07-16 / Claude.

## Outcomes & Retrospective

(To be filled.)

## Context and Orientation

Key files: `…/lmdb/ValueIds.java` (id bit layout; free codes 5-15, 36-63), `…/lmdb/ValueStore.java` (id assignment; `getInlineId` ~:2674 → `inlined/Values.packLiteral`), `…/lmdb/inlined/Values.java` + `inlined/Integers.java` (current ZigZag encoders), `…/lmdb/LmdbStore.java` (VERSION=2 at :73; upgrade hook :278-299), `…/lmdb/StoreProperties.java` (persisted store properties; mirror `setTripleIndexes` for the new key), `…/lmdb/config/LmdbStoreConfig.java` (runtime config; `inlineLiterals` precedent :105), `…/lmdb/evaluation/LmdbNativeValueCodec.java` (id → value decode; `decodeInlined` switch :153-191), `…/lmdb/evaluation/LmdbNativeExpressionRuntime.java` (`nativeCompare`/`compareNumeric` :341-371), `…/lmdb/evaluation/LmdbNativeRowStep.java` (`orderCompare` :635-641), Phase 7's `LmdbKeyRange` + `TripleStore.getTriplesRange` (bounded scans).

Ordering math: for a fixed ordered type code T, `id = (value + 2^55) << 7 | T << 1` — the biased value occupies the high bits, so unsigned (and, since bit 63 stays 0, also signed) long comparison of two same-code ids equals numeric order. Index keys are `Varint.writeListUnsigned` of the compound ids in index field order and varints are order-preserving, so posc object-id order = value order within one code: all objects >= encode(60,T) form one contiguous key range.

## Plan of Work

(Milestone bodies as summarized in Progress; each lands with its tests before the next starts. M1 must precede any writer-side default flip; the differential fuzz suite runs on an ordered-encoding store for every milestone.)

## Concrete Steps

    python3 .codex/skills/mvnf/scripts/mvnf.py ValueStoreTest            # plus new ordered-encoding tests
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

## Validation and Acceptance

M1: round-trip per subtype incl. boundaries/negatives; order property (`id1 < id2 <=> v1 < v2` per code); mixed-store coexistence; v2-store ids untouched; version-gate behavior. M2: comparison-matrix parity; ORDER BY parity on ordered stores. M3: pushdown parity fuzz + plan snapshot + `FILTER(?value > 60)`-shape benchmark. M4: pruning correctness (skipped scan == empty scan) + stats accuracy tests.

## Idempotence and Recovery

Writer gate is per-store-creation; tests use fresh @TempDir stores. Nothing rewrites existing data. VERSION bump is additive (old stores open unchanged; the property stays absent).

## Artifacts and Notes

(Evidence per milestone.)

## Interfaces and Dependencies

New `ValueIds` constants `T_ORD_INTEGER..` (36-63 range) + `isOrderedNumeric(long id)`/`orderedFamily(int type)` helpers; `StoreProperties.get/setNumericIdEncoding()`; `LmdbStoreConfig.setOrderedNumericIds(boolean)` (default true, applies at creation only); codec extensions. Depends on Phase 7 M1 (`LmdbKeyRange`) for M3 and Phase 8 (CSR sweep) for the free half of M4.
