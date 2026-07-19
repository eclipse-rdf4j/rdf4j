# 08 — Range pushdown and value encoding

Goal: comparison filters become index key-range scans where provably sound; the inline encodings that
block order-preservation (datetime, short strings) get versioned order-preserving successors; and the
per-record varint decode cost drops via wide loads. This is the single largest asymptotic gap identified
against Kuzu — every prerequisite ships today and nothing wires them together.


## Current state

Ordered numeric IDs are on by default (`LmdbStoreConfig.java:107`, recorded at store creation
`LmdbStore.java:291-300`); the encoding is order-preserving (`ValueIds.java:59-74`, `ORDERED_BIAS:74`,
`isOrderedInteger:80-86`); varint keys are lexicographic == numeric (`Varint.java:57-63`) under LMDB's
default unsigned comparator (`LmdbRecordIterator.java:515`); `LmdbKeyRange` carries raw bounds plus
`indexFieldSeq` (`LmdbKeyRange.java:29-43`) and `LmdbRecordIterator` applies them over pattern bounds
(`:138-190`). But `new LmdbKeyRange(...)` occurs in production at exactly two sites — both parallel
partitioning (`LmdbSailStore.java:2899, :2902`); `TripleStore.getTriplesRange` (`:2123`) has only
partition callers; scan bounds otherwise derive solely from the pattern quad (`TripleIndex.getMinKey:288,
getMaxKey:296`); `PatternPlan` has no range field (`LmdbNativePatternPlan.java:28-38`);
`NativeLmdbQuerySource.statements` takes only the quad (`:53-55`) or quad+partition (`:173`). Every
ordered-ID consumer is per-row (`LmdbNativeFilters.java:476-490`, `LmdbNativeRowStep.java:616-619`,
`LmdbNativeValueCodec.java:189-201`). The only filter→scan lowering that exists is equality/IN
(`LmdbJoinPlanSupport.java:341-347`, `LmdbNativeAggregatePatternCompiler.java:214-241`).

**The soundness trap (load-bearing, verified):** a bare low bound of `varint((K+ORDERED_BIAS)<<7)` is NOT
a conservative superset. `createId = value<<7 | type<<1` (`ValueIds.java:122-125`) sorts value-major, so
three families sort BELOW an ordered-integer window and would be silently dropped — false negatives no
residual filter recovers: dictionary-referenced numerics (`T_LITERAL`, `:31`, small ValueStore offsets);
inlined `T_DOUBLE`/`T_DECIMAL`/`T_FLOAT` (`:21, :38-40`); big integers outside the inline range
(`Integers.packOrderedBigInteger` returns 0L, `inlined/Integers.java:200-206`).

Inline encodings with order defects: datetime is byte-reversed against chronological order; short
strings cap at 6 bytes and are not order-preserving; both block their types from any future ordered-scan
or radix treatment. Scan decode: every key decodes all four varint fields byte-at-a-time including
fields the query never reads; `Varint` already contains an 8-byte-load helper family
(`SignificantBytesBE`-style, `:563-565`) used elsewhere.


## Work item 1 — Compare → LmdbKeyRange lowering

Design, in dependency order:

1. **Range field on `PatternPlan`**: `LmdbKeyRange range` (nullable), participating in `equals`/plan
   identity, applied when the pattern's chosen index's FIRST VARYING field is the compared variable's
   position (the range constrains a key prefix continuation; deeper fields cannot bound the scan).
2. **Lowering rule** (runs in the native compiler where equality/IN lowering already lives,
   `LmdbNativeAggregatePatternCompiler` — NOT in the sketch pipeline): for
   `FILTER(?v OP constant)`, OP ∈ {<, <=, >, >=}, where `?v` is produced at exactly one quad position of
   one pattern and the constant is an ordered-integer-encodable numeric: attach bounds
   `[varint((K+bias)<<7), ...]` to that pattern IF the soundness gate holds.
3. **Soundness gate** — one of:
   (a) `allNeighborsOrderedIntegers` from the CSR zone map (plan 07 §1) proves the scanned column holds
   only ordered-integer IDs for this predicate → single bounded scan, residual filter retained (cheap,
   catches boundary semantics like open vs closed);
   (b) no proof → dual scan: bounded ordered-integer window UNION a complement scan restricted to the
   three non-ordered families (their ID ranges are computable from the type bits: `T_LITERAL` region,
   inline-float/decimal/double regions — enumerate the regions from `ValueIds` constants, each a
   contiguous ID window given value-major layout) with the FULL filter applied to complement rows.
   Start with (a) only — it covers the common case (a numeric-only predicate like `:price`, `:year`)
   with zero risk; add (b) behind its own flag once (a) is validated.
4. **API**: `NativeLmdbQuerySource.statements(quad, LmdbKeyRange)` overload threading to
   `TripleStore.getTriplesRange`; the composite source intersects the range with each member's bounds.
5. **Estimates**: a ranged pattern's `estimate` shrinks by the range fraction (linear interpolation over
   the ordered window against the predicate's [min,max] from the zone map) — feeding plan 01 §2 so join
   order and admission see the selectivity.

Keep the residual filter ALWAYS (open/closed bounds, type-promotion comparisons, NaN discipline).

Tests: soundness corpus — stores salted with dictionary-referenced numerics, inline doubles/decimals,
big integers, ensuring gate (a) refuses and results match the generic evaluator exactly; boundary tests
(`>=` vs `>` at the constant); estimate tests.
Acceptance: `?s :price ?p . FILTER(?p > 10000)` on a 10M-triple all-integer predicate reads only the
matching suffix (store-probe counters), ≥10× on the range benchmark shape; zero result diffs across the
salted corpus.


## Work item 2 — Order-preserving encoding v2 (datetime, short strings)

Both changes alter PERSISTED ID bit layouts → strictly versioned, applied only at store creation, exactly
like ordered-v1 (`LmdbStoreConfig.java:107` precedent; the store records its encoding version at
creation, `LmdbStore.java:291-300`).

1. **Datetime**: replace the byte-reversed inline layout with a chronologically order-preserving one
   (epoch-based, timezone-normalized to UTC with the original offset stored in remaining bits where it
   fits, dictionary fallback where it does not — matching the existing inline-vs-dictionary split
   discipline). Unlocks: `FILTER(?date >= "2020-01-01"^^xsd:date)` range pushdown via work item 1's gate
   extended to a `allNeighborsOrderedTemporal` flag (add alongside the integers flag in the CSR build,
   plan 07 §1), and radix sort on date keys (plan 05 §4's precondition generalizes).
2. **Short strings**: extend the 6-byte non-order-preserving inline to an order-preserving big-endian
   prefix encoding (7 bytes payload + continuation semantics: inline value orders correctly against
   other inlines by prefix; equal-prefix cases fall back to dictionary comparison — the comparator must
   treat inline-vs-inline equality as INCONCLUSIVE beyond the prefix unless lengths fit). If the
   inconclusive-equality complexity proves invasive (it touches every raw-ID equality shortcut), ship
   ONLY the order-preservation for inequality comparisons and keep equality semantics unchanged —
   decide by implementation spike, record in plan 13.
3. Migration: none in place — v2 applies to new stores; existing stores keep v1 behavior bit-for-bit
   (version-gated at every encode/decode site; the version is already plumbed to `ValueIds` consumers).

Tests: total-order property tests per type (encode → compare raw longs == compare values) across
timezones, precision edges, surrogate pairs; cross-version store open tests.
Acceptance: date-range benchmark shape gets pushdown on a v2 store; v1 stores byte-identical behavior.


## Work item 3 — Wide-load varint decode

Use the existing 8-byte-load helpers (`Varint.java:563-565` family) for key positions 0..2 in the scan
hot loop (`Varint.readUnsigned(long address)`, `:428-433`) — `MAX_KEY_LENGTH = 36` makes 8-byte reads
in-bounds for all but the final field; the final field keeps the byte loop or a bounds-checked wide
read. Additionally: skip decoding key fields the consumer never reads — the record iterator knows the
pattern's bound mask; bound fields need only length-skipping (the varint length nibble), not value
reconstruction. Both are pure hot-loop mechanics with no format change.

Tests: decode-equivalence fuzz vs the byte loop across all lengths and alignments (including
segment-boundary keys); JMH microbenchmark for the decode loop.
Acceptance: full-scan throughput (the theme benchmark's scan-bound shape) improves ≥15%; zero behavior
change.


## Dependencies

Item 1 gate (a) depends on plan 07 §1 (zone map consumption). Item 1's estimates feed plan 01 §2. Item 2
extends plan 05 §4's radix eligibility and item 1's gate to temporals. Item 3 is independent, land first.
