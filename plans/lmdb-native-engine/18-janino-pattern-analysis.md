# Theme-Query Pattern Analysis for Janino Whole-Stage Codegen (Routine C spike — no code changes)

Date: 2026-07-24. Inputs: `testsuites/benchmark-common/.../ThemeQueryCatalog.java` (143 queries = 11 themes x 13),
latest timings `results-2026-07-23.md` (indices 0–10 per theme), and the measured facts recorded in
`plans/lmdb-native-engine/17-janino-whole-stage-codegen.md` (M0–M4). Nothing was built or run for this analysis.

## 0. Ground rules learned from plan 17 (what "benefit" means)

Measured facts that calibrate every judgment below:

- Fused aggregation kernels (no row materialization) reach **10x–34x** when the data is CSR-served
  (cycle3GroupedInterest 34.35 → 1.02 ms).
- Row-materializing kernels reach **~5x** until batch-form emission exists; the RowState/BindingSet surface
  is the cap, not kernel quality (cycle4: 31.2 vs 14.4 ms ceiling).
- Cursor-bound full scans have **no codegen headroom** (ANALYTICS q11 ideal fused loop: 1.14x). Codegen wins
  come from *what the loop reads* (in-memory CSR arrays), not fewer virtual calls per se.
- Queries under ~1 ms are not worth kernel admission (compile+setup noise dominates).

So "benefits from Janino" below means: interpreter-bound work over CSR-servable patterns, with either
(a) an aggregate/EXISTS consumer (top tier), or (b) large row output that batch emission can serve (second tier).

## 1. The corpus is 5 templates, not 143 queries

The nine domain themes (MEDICAL_RECORDS, SOCIAL_MEDIA, LIBRARY, ENGINEERING, HIGHLY_CONNECTED, TRAIN,
ELECTRICAL_GRID, PHARMA, REAL_ESTATE) instantiate the *same 13 query skeletons* over different vocabularies;
ADAPTIVE_FILTER_PLACEMENT is 13 variants of one skeleton; ANALYTICS is its own family. Per-index skeleton for
the domain themes:

| idx | Skeleton | Consumer |
|-----|----------|----------|
| 0 | class scan + OPTIONAL{chain+BIND alias} + FILTER on optional var (⇒ effectively inner join) + trailing OPTIONAL | COUNT(DISTINCT) |
| 1 | VALUES + UNION of two class branches sharing ?name + FILTER(?name=?target \|\| ?name=const) + OPTIONAL | COUNT(DISTINCT) |
| 2 | class + FILTER IN + OPTIONAL join | GROUP BY + COUNT(DISTINCT) + HAVING |
| 3 | class + OPTIONAL BIND + FILTER cmp + MINUS{pattern + string FILTER (CONTAINS/LCASE/STR)} | COUNT(DISTINCT) |
| 4 | class + FILTER(a \|\| b) + FILTER EXISTS + OPTIONAL | COUNT(DISTINCT) |
| 5 | VALUES scalar + class + FILTER IN + FILTER NOT EXISTS{pattern + FILTER <} | COUNT(DISTINCT) |
| 6 | UNION of two branches + OPTIONAL BIND + FILTER != | GROUP BY + COUNT + HAVING |
| 7 | class + FILTER(=\|\|=) + FILTER EXISTS + MINUS{pattern + FILTER} | COUNT(DISTINCT) |
| 8 | 2–3-hop join chain + OPTIONAL + FILTER + EXISTS | GROUP BY HAVING or COUNT(DISTINCT) |
| 9 | VALUES + 3–4-pattern chain + FILTER IN + EXISTS/NOT EXISTS + OPTIONAL | COUNT(DISTINCT) or GROUP BY |
| 10 | UNION + OPTIONAL + FILTER + MINUS/NOT EXISTS | COUNT(DISTINCT) or GROUP BY |
| 11 | star root + **nested OPTIONAL tree (3 deep)**, SELECT DISTINCT *, ORDER BY | 100k–950k rows out |
| 12 | UNION of two class scans + 4–5 OPTIONAL blocks (some containing UNION) | 25k–950k rows out |

Deviations: SOCIAL 8–10 are 3/4/5-cycles (WCOJ shapes); PHARMA 9 and REAL_ESTATE 7 wrap an aggregating
subquery (AVG + HAVING(AVG)); REAL_ESTATE adds AVG/SUM/MIN/MAX aggregates, ORDER BY DESC LIMIT 25 (top-k),
arithmetic BIND (?price/?area), COALESCE and `|| !BOUND(...)` filters.

Consequence: **general support for ~7 recognizer/emitter capabilities covers essentially the whole corpus**,
because 9 of 11 themes share skeletons. The capabilities, in order of corpus leverage:

1. OPTIONAL (left probe), including the filter-on-optional-var idiom and nested OPTIONAL trees
2. UNION (branch concatenation into a shared tail / shared emit surface)
3. EXISTS / NOT EXISTS / MINUS as short-circuit witness sub-kernels (incl. 2-hop witnesses with filters)
4. General grouped aggregation beyond identical COUNT(DISTINCT): COUNT(*), COUNT(?x), SUM/AVG/MIN/MAX, HAVING
5. Value-level filter SPI (string functions, dates, IN over literals) — decode-and-test inside the loop
6. Batch-form emission + fused DISTINCT / ORDER BY / LIMIT (the row-materializing consumer surface)
7. VALUES domains generalized (already partly there via M4 seeding) + arithmetic BIND on inlined numerics

Today's recognizers (M3: 2–6 constant-predicate chains, no OPTIONAL/UNION/EXISTS, filters residual;
M4: VALUES-seeded identical COUNT(DISTINCT) with all-absorbable filters) match **almost none of the 143
theme queries as written** — the corpus was evidently designed around OPTIONAL/UNION/negation. That is the
gap this analysis maps.

## 2. Categories, members, timings, expected payoff

Timings from `results-2026-07-23.md` (that run covered indices 0–10; the q11/q12 denormalized views are
known-huge from expected counts: TRAIN q12 943,354 rows; GRID q12 621,654; MEDICAL q11/q12 199,461/347,473;
REAL_ESTATE q11/q12 ~100k/109k).

### Category A — Grouped count over CSR chains with witnesses (top tier: aggregation fusion, 10x-class)

Skeletons 2, 6, 8, plus PHARMA 1/2/6/8/10. The M4 kernel family generalized: group slot seeded by a scan or
VALUES, producer patterns g→x over CSR runs, witness EXISTS, HAVING on the count. Needs capabilities 1, 2, 4.

Hot members: LIBRARY q6 **2780 ms** (UNION seed + OPTIONAL + COUNT HAVING), PHARMA q10 **3514 ms**
(VALUES + chain + OPTIONAL + nested 3-pattern EXISTS + COUNT(DISTINCT) HAVING), HC q9 **604 ms**, HC q6
**435 ms**, HC q2 **39 ms** (count=36,767 groups), TRAIN q9 **104 ms**, TRAIN q6 **26 ms**, GRID q6 **34 ms**,
MEDICAL q6 **24 ms**, MEDICAL q8 **41 ms**, ENG q6 **80 ms**, PHARMA q2 **45 ms**. Also every theme's q2
(2–14 ms — marginal alone but free once the family exists).

Why 10x-class: identical structure to the proven cycle3GroupedInterest win — counting/dedup collapses into
per-group existence/transition logic over CSR runs; no result surface to pay. LIBRARY q6 and PHARMA q10 are
the two biggest single-query prizes in the whole corpus.

### Category B — Semi/anti-join filtered COUNT(DISTINCT) (top tier)

Skeletons 0, 3, 4, 5, 7, 9, 10: candidate scan → id/value filters → EXISTS witness → NOT EXISTS/MINUS
anti-witness → COUNT(DISTINCT candidate). Needs capabilities 1, 3, 5 (many witnesses carry string filters:
CONTAINS(LCASE(STR(?name)),...)).

Hot members: **HC q10 5773 ms** (single biggest number in the corpus: weight IN (1..4) + NOT EXISTS
{connectsTo ?n2 . ?n2 weight ?w2 . FILTER(?w2<3)} + MINUS self-loop — kernel form: per node, loop CSR
neighbors, probe neighbor weight, short-circuit; trivially parallel), HC q8 **329 ms** (length-2 path +
EXISTS closing edge), HC q1 **414 ms**, HC q3/q4/q5/q7 (50–128 ms), MEDICAL q3/q4 (33/35 ms), q9 **65 ms**,
q10 **159 ms**, LIBRARY q0 **130 ms**, q7 **179 ms**, q9/q10 (52/53 ms), ENG q0 **40 ms**, q4 **78 ms**,
TRAIN q3 **41 ms**, q4 **144 ms**, q8 **55 ms**, q10 **94 ms**, GRID q3 **101 ms**, q10 **59 ms**,
PHARMA q4/q7/q8 (8–12 ms). The filter-on-optional-var idiom (skeleton 0/3) is semantically an inner
join + filter (unbound ⇒ type error ⇒ dropped) *except* REAL_ESTATE's `|| !BOUND(?x)` variants — the
recognizer must treat these as two distinct shapes.

Why 10x-class: COUNT(DISTINCT) consumer + short-circuit witnesses = the exact register-resident,
no-materialization pattern; expected counts show 25k–130k candidates flowing through interpreted
OPTIONAL/MINUS operators today.

### Category C — UNION dual-branch counts (top tier, needs capability 2)

Skeleton 1 (+ 6/10 union seeds): VALUES + two class branches + equality-OR filter. LIBRARY q1 **237 ms**,
ENG q1 **236 ms**, HC q1 **414 ms**, MEDICAL q1 **18 ms**, TRAIN q1 **59 ms**, GRID q1 **29 ms**. Kernel:
run branch kernels sequentially into one distinct-set/accumulator; slot alignment is the only new problem.

### Category D — Cycle/WCOJ shapes (leapfrog kernels, M5a; gate evidence exists)

SOCIAL q8 **719 ms** (3-cycle + OPTIONAL name + FILTER IN), SOCIAL q9/q10 (VALUES-constrained 4/5-cycles,
currently sub-ms to 4 ms — leave alone), HC q8 (also in B). The 719 ms 3-cycle over the full follows graph
is the theme-corpus twin of the FOAF gate queries: intersection kernels over CSR + grouped-interest
consumer. Benefit already demonstrated on FOAF; this is the transfer target.

### Category E — Denormalized views: nested-OPTIONAL star + DISTINCT + ORDER BY (second tier until batch emission)

Skeletons 11/12 in all nine domain themes (18 queries), 100k–950k output rows. Everything here is
left-join trees over CSR-served stars, then DISTINCT, then ORDER BY. Per the M3 measurement these are
**materialization-bound**: kernels alone give ~2x; the levers are capability 6 (batch-form emission straight
into NativeBatch columns, fused DISTINCT via sorted-run/hash on ids, fused ORDER BY before decode) and the
factorized machinery already on this branch. Expected payoff 3–8x, contingent on batch emission landing.
These queries are also exactly where nested-OPTIONAL correctness is subtle (well-designed-ness, masks) —
they should be the fuzz anchors for capability 1.

### Category F — Expression/scan-bound (low tier — mostly skip)

- ADAPTIVE_FILTER_PLACEMENT (13 × ~24–35 ms): scan → string FILTER (REGEX/CONTAINS/STRSTARTS/SUBSTR/...) →
  join → BIND. Bound by dictionary decode + string ops, not operator dispatch. Codegen value: fusing
  decode+test and skipping Value boxing — worth having via capability 5 as a *shared* facility (B needs it
  for witness filters anyway), but not a headline win. REGEX stays interpreted (Pattern.matcher), called
  from the kernel.
- ANALYTICS q0–q5, q9, q10 (full ?s ?p ?o scans + aggregate): measured cursor-bound (1.14x ceiling) — do
  not target with codegen; the prefix-run parallel group engine already owns these. Possible exceptions:
  q7 (out-degree histogram: subquery GROUP BY → outer GROUP BY — double-grouping fuses into one pass over
  the SPO index since subject runs are contiguous) and q8 (class-linkage matrix: two type-joins around a
  scan — CSR-servable joins around the scan). Treat as measurement-gated.

### Category G — Aggregate breadth + top-k + arithmetic (REAL_ESTATE + PHARMA/ANALYTICS subqueries)

- AVG/SUM/MIN/MAX with HAVING over numeric properties: RE q0/q1/q7/q8/q10 (RE absent from the 07-23 run;
  historically tens-of-ms class). Numeric ids are inlined in the dictionary encoding, so accumulators run
  on decoded doubles from ids without dictionary hits — capability 4+7.
- Subquery-aggregate-then-outer-consume: PHARMA q9 (AVG HAVING subquery → EXISTS/OPTIONAL/IN outer),
  RE q7, ANALYTICS q6 — two-stage fused kernels (inner group table becomes the outer kernel's domain).
- Top-k: RE q2 (ORDER BY DESC LIMIT 25) — bounded-heap emit fused into the scan; avoids full sort +
  materialize. Small but architecturally cheap once capability 6 exists.

### Summary: where the milliseconds are

Top-10 single-query prizes (07-23 run, ms): HC q10 5773 (B), PHARMA q10 3514 (A), LIBRARY q6 2780 (A),
SOCIAL q8 719 (D), HC q9 604 (A), HC q6 436 (A), HC q1 415 (C), HC q8 329 (B), LIBRARY q1 237 (C),
ENG q1 236 (C). Categories A+B+C alone account for >80% of total corpus time in that run; all three are
aggregation-consumer shapes — the proven 10x-class kernel family — blocked today only by OPTIONAL / UNION /
negation / value-filter support in the recognizer-emitter.

## 3. Design sketch: a general query-kernel compiler for SPARQL 1.2

The M3/M4 experience says per-shape ad-hoc recognizers do not scale to this corpus, let alone the spec.
The generalization is a small **kernel IR** with closed composition rules, a lowering pass from the existing
`SlotPlan`/`MultiJoinPlan` trees, and *partial fusion* so unsupported constructs never block the supported
subtree around them.

### 3.1 Kernel IR: ~14 composable primitives

Every kernel is a tree of these nodes; the emitter walks the tree producing nested loops in one Java class
(one helper method per node, respecting the bytecode budget):

| Primitive | Role | SPARQL constructs it lowers |
|-----------|------|------------------------------|
| `Enumerate` | key-domain scan (CSR keyAt), range scan, VALUES domain, full-index scan | BGP roots, VALUES, GRAPH ?g enumeration |
| `Probe` | CSR adjacency expansion fwd/bwd, with multiplicity/context arena | triple patterns, quoted-triple (RDF 1.2 triple term) patterns once ids cover them |
| `Intersect` | leapfrog over k sorted adjacency runs | WCOJ shapes, cycles |
| `FilterId` | id-level predicates: =, !=, IN, range over inlined numerics | sameTerm, id-decidable =/!=, numeric cmp on inlined values |
| `FilterValue` | decode slot → typed scratch → compiled expression; error ⇒ drop (or keep, per context) | general FILTER expressions via the expression tier (3.3) |
| `LeftProbe` | Probe that emits null-row with mask bit when empty; owns its condition | OPTIONAL, incl. nested trees (well-designed check at lowering) |
| `Exists` / `AntiExists` | short-circuit witness sub-kernel (boolean method) | FILTER (NOT) EXISTS; MINUS lowered separately (compatible-bindings semantics — distinct node flavor, `AntiJoinCompat`) |
| `Union` | run branch subtrees sequentially into aligned slots (absent slots masked) | UNION |
| `Bind` | alias copy or computed expression → new slot (id or scratch value) | BIND, expression projections |
| `Aggregate` | grouped/global accumulators in locals or group-table arrays; COUNT/SUM/AVG/MIN/MAX/SAMPLE/GROUP_CONCAT, DISTINCT per spec; HAVING as post-filter | GROUP BY, HAVING, aggregate projections |
| `Dedup` | DISTINCT/REDUCED: sorted-run transition counting when input order allows, hash-set of packed rows otherwise | SELECT DISTINCT |
| `Order`/`TopK` | sort keys on ids-then-decoded values; bounded heap when LIMIT known | ORDER BY, LIMIT/OFFSET fusion |
| `PathExpand` | fixed-length unrolled to Probes; `*`/`+` as BFS with visited bitset over CSR; negated sets as scan-with-exclusion | property paths |
| `Emit` | batch-form into NativeBatch columns (preferred) or packed row buffer (current fill contract) | projection/result surface |

Composition rules are what make it "arbitrary combinations": every primitive consumes and produces the same
abstract row context (slot registers + mask), so any tree the lowering pass can type-check is emittable.
Subqueries are a pipeline breaker: the inner tree runs to a materialized domain (group table or sorted id
array) that becomes an `Enumerate` source for the outer tree — this covers PHARMA q9 / RE q7 / ANALYTICS q6
and the general sub-SELECT case without kernel-calls-kernel complexity.

### 3.2 Lowering and partial fusion (the generality mechanism)

A single pass over the compiled plan marks each node kernelizable-or-not with a recorded decline reason
(extending the existing `janinoCodegen.debug` discipline). Then instead of all-or-nothing admission, the
planner fuses **maximal kernelizable subtrees** and stitches them to the interpreter at batch boundaries:
a kernel can be a source (feeding interpreted operators via its Emit), a sink (consuming an interpreted
cursor via an `Enumerate`-from-batch adapter), or the whole stage. This is how SERVICE, exotic functions,
full-text, custom aggregates, LATERAL-style correlation, and anything else outside the IR degrade
gracefully instead of disabling codegen for the query — the fallback story required to claim "complete
SPARQL 1.2 support": *everything* runs; kernels cover what they can prove.

### 3.3 Two-tier expression compiler

- **Tier id**: comparisons decidable on dictionary ids — sameTerm, =/!= with resource-assured operands
  (M3's rule, kept), numeric cmp/arith on inlined numerics, IN over constant id sets. Emitted inline.
- **Tier value**: decode slot to a typed scratch register (long/double/int-datatype-tag/CharSequence view),
  then emitted Java for the SPARQL operator tables: type promotion lattice, three-valued logic
  (`&&`/`||`/error per spec — errors are values in the emitted code, an `int` {TRUE,FALSE,ERR} not an
  exception), string ops (STR/LCASE/CONTAINS/STRSTARTS/SUBSTR/STRBEFORE/STRAFTER/STRLEN over the
  dictionary's UTF-8 bytes where possible, avoiding String allocation), datetime comparisons on normalized
  encodings, LANG/DATATYPE/isIRI/isLiteral/isBlank as id-metadata checks. Anything outside the emitted
  vocabulary (REGEX, ENCODE_FOR_URI, IRI(), NOW, RAND, BNODE, custom functions) compiles to a call into
  the existing interpreted `ValueExprEvaluation` via a public SPI hook — correct by construction, still
  loop-resident. Nondeterministic functions (RAND/UUID/STRUUID/BNODE/NOW) force per-row hook calls and
  are never constant-folded.

### 3.4 SPARQL 1.2 coverage matrix (kernelize vs interpret-in-loop vs stitch-out)

- **Kernel-native (IR)**: BGPs, joins incl. WCOJ, FILTER, OPTIONAL, UNION, MINUS, EXISTS/NOT EXISTS, BIND,
  VALUES, GROUP BY + all builtin aggregates + HAVING, DISTINCT/REDUCED, ORDER BY/LIMIT/OFFSET, subqueries,
  fixed and recursive property paths, GRAPH with named-graph enumeration (context column already exists in
  the arena), ASK (Exists at the root), COUNT-only CONSTRUCT/DESCRIBE feeding.
- **Hook-in-loop (interpreted calls from generated code)**: exotic/custom functions, REGEX, hash functions,
  IRI/BNODE constructors, LangMatches edge cases, custom aggregates.
- **Stitch-out (partial fusion boundary)**: SERVICE, federation, full-text sail interop, property functions,
  update expressions, anything touching the transaction write path.
- **RDF 1.2 specifics**: triple terms/quoted triples need dictionary ids for embedded triples; once the id
  space covers them, `Probe` treats them like any constant/variable term — flag as a lowering precondition,
  not an IR change. Version/reification-related builtins land in Tier value or the hook.

### 3.5 Lifecycle (inherit, don't reinvent)

Shape-keyed cache with constants as fields, async single-thread compile, threshold admission, failed-shape
memoization, LRU + byte budgets, dumpDir, counters, explain tags — all proven in M2/M3/M4 and reused as-is.
Constant-inlining recompile tier stays the M5 escalation for ultra-hot shapes. Parallelism composes at the
`Enumerate` root (partition key domain across the existing parallel-pipelines substrate) — HC q10 and the
Category E views are the obvious beneficiaries.

### 3.6 Correctness strategy

Per-primitive differential fuzz rounds (the M3/M4 pattern: forced-on, threshold 0, multiset compare vs
interpreter), with dedicated corpora for the known semantic traps: filter-on-optional-var vs `!BOUND`
variants, MINUS-vs-NOT-EXISTS divergence (shared-variable and no-shared-variable cases), nested OPTIONAL
well-designed-ness, error propagation in `||`/`&&`, aggregate empty-group and error-in-aggregate rules,
DISTINCT over unbound columns, ORDER BY across type buckets. The 18 Category E view queries become standing
parity anchors. Compliance baseline (`COMPLIANCE-BASELINE.md`, 24 known pre-existing failures) re-checked
per capability landing.

### 3.7 Suggested build order (leverage per unit of work)

1. **Exists/AntiExists + LeftProbe (capabilities 1+3) into the M4 aggregate family** → unlocks Categories
   A and B cores: HC q10, PHARMA q10, LIBRARY q6 and ~60 more queries. Highest ratio in the corpus.
2. **Union branch concatenation (2)** → Category C + the union-seeded A members.
3. **Value-filter tier (5), string subset first** → removes the residual-filter wrapper cost from A/B
   witnesses; makes ADAPTIVE coverage free.
4. **Aggregate breadth (4+7): COUNT(*)/SUM/AVG/MIN/MAX + HAVING + numeric BIND** → REAL_ESTATE, PHARMA q9
   two-stage.
5. **Batch-form Emit + fused Dedup/Order/TopK (6)** → Category E views and every SELECT-* consumer;
   this is the already-identified M5 lever and the largest single engineering item.
6. **Leapfrog kernels (D)** and the two ANALYTICS join exceptions — measurement-gated as plan 17 M5 states.

Each step is admission-gated and flag-guarded exactly like M3/M4; none blocks the others.

## 4. Explicit non-targets

Sub-millisecond queries (most SOCIAL 0–7, GRID q2/q4, ENG q7), cursor-bound ANALYTICS full scans
(q0–q5, q9–q11 — measured 1.14x ceiling), and REGEX-dominated filters. Recording these here so future
work does not re-litigate them without new measurements.
