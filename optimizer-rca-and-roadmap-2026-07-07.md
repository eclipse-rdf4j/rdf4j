# LMDB Query Optimizer — Root Cause Analysis & Roadmap to a State-of-the-Art Engine

*2026-07-07. Routine C investigation (no production code changed). Produced from a 27-agent
orchestrated investigation: plan diffs of `result-latests.txt` vs prior runs, git archaeology
2026-06-03 → HEAD, 12 component audits, 4 research deep-dives (papers2/ + mysql/), adversarial
verification of every root-cause hypothesis, and one instrumented run of the failing
plan-expectation tests.*

---

## 1. Executive summary

**The regression is real, plan-level, and mechanically explained.** Three queries regressed
(q2 ~30x, q7 ~5.7x, q3 ~2.5x cumulative); five improved. All three regressions share one
failure family, and it is exactly the one you suspected: **plan decisions are being made
outside the cost-based competition.** In q2 and q7 the fast plan was silently *never generated*
(rule bail-outs made it vanish from the memo — the cost model never even saw it). In q3 a new
rewrite was *force-selected without costing the original* ("skipped-semantic-rewrite …
cmp=inf"). No amount of cost-model quality can save a search that doesn't contain the good plan
or doesn't run the competition.

**The systemic verdict matches your suspicion.** The audits found no benchmark vocabulary
hardcoded in production code — but they found something functionally equivalent: benchmark-shaped
rule guards, ~254 underived numeric thresholds, six stacked estimate-override layers, five-to-six
coexisting join planners arbitrated by a 1.20x fudge factor, learned feedback that mutates plans
across runs *by default*, and a test suite whose dominant idiom (184 exact-plan-string
assertions; 96 disabled tests, 62 with the literal excuse "Disabled until we can verify if this
test is correct or not"; 16 classes institutionalized as a red baseline) is precisely the
mechanism by which patch-to-green tuning gets locked in.

**The gap to "a mature engine like MySQL" is architectural, not algorithmic.** You already have
the right algorithms — a genuine Cascades memo, a faithful DPhyp enumerator (the single most
principled component in the codebase), real sketches, characteristic sets, guarantees. What
MySQL has and this codebase lacks: *one* cost currency, *one* join planner, a *narrow typed*
statistics boundary, deterministic bounded degradation, and rules that stay out of the
enumerator. MySQL's entire hypergraph optimizer is ~37K lines of C++; this optimizer surface is
~87K lines of Java doing the same job several times in parallel.

---

## 2. Root cause analysis

### 2.1 What was measured

`ThemeQueryPlanRunBenchmark.runQuery`, MEDICAL_RECORDS, catalog queries (`queryVariant=filter`
== `ThemeQueryCatalog.queryFor`), avg ms/op:

| q | 2026-05-26 | 2026-06-03 | 2026-07-06 | verdict |
|---|---|---|---|---|
| 0 | 55.1 | 67.2 | 55.7 | flat |
| 1 | 118.5 | 139.6 | 86.9 | improved (DPhyp) |
| **2** | **0.63** | **1.26** | **36.7** | **regressed ~30x — CONFIRMED cause** |
| **3** | **89.3** | **131.8** | **225.2** | **regressed ~2.5x — high-confidence cause** |
| 4 | 157.0 | 139.1 | 141.3 | flat |
| 5 | 5.9 | 23.5 | 12.9 | improved |
| 6 | 68.2 | 67.8 | 27.8 | improved |
| **7** | 67.9 | **12.0** | **68.8** | **lost June optimization — CONFIRMED cause** |
| 8 | 61.2 | 72.5 | 77.0 | flat |
| 9 | 286.7 | 730.8 | 308.3 | noisy / improved (DPhyp) |
| 10 | 272.9 | 245.9 | 154.3 | improved (DPhyp) |

The 23:27 benchmark binary contained everything up to and including `c854deabc6` ("faster
loading"); classes compiled 23:17/23:27:14. Caveats that must be fixed before the next
comparison: cnt=2 iterations (no error bars), the store + sketches were rebuilt from scratch that
evening, and 1.2MB of learned feedback (`join-estimator.rjes.operators`) accumulates during and
across runs.

### 2.2 q2 — 30x: the date-IN VALUES anchor was legislated out of existence (CONFIRMED)

Query: `?enc a med:Encounter . ?enc med:recordedOn ?date . FILTER(?date IN ("2024-01-01"^^xsd:date,
"2024-02-01"^^xsd:date)) . ?enc med:handledBy ?practitioner`.

- **June plan (1.26 ms):** 2-row `BindingSetAssignment` of the two dates → bound-object lookup on
  `med:recordedOn` (137 work rows) → join type + handledBy. Root work ≈ 1.6K rows.
- **July plan (36.7 ms):** full 24,957-row `posc` scan of `a med:Encounter`, join `recordedOn`
  per subject, then `?date IN (…)` as a post-scan `ListMemberOperator` filter (24.9K → 135 rows,
  pass ratio 0.54%). Root work ≈ 25.0K rows.

**Root cause (verified, mechanism airtight):** commit `4400626f3a` ("cleanup", 2026-07-06 11:19)
changed `GuaranteePlanOptionProvider.calendarAnchorMatchesGuarantee` from
`if (datatype == XSD.TIME) return false` to
**`if (datatype != XSD.DATETIME) return false`** plus a requirement for the new
`RdfTermDomain.Fact.CANONICAL_DATETIME` fact ([GuaranteePlanOptionProvider.java:1341-1352](core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/GuaranteePlanOptionProvider.java)).
`CANONICAL_DATETIME` is only ever set for `xsd:dateTime` (RdfTermDomain.java:637-643), so
**xsd:date literals can never anchor again**. `expandedAnchorValues` returns empty →
`narrowObjectAnchor` returns null → `lmdb-guarantee-options` emits **`generated=0`** (visible in
the July diagnostics; June said `generated=1, selected=finite-anchor:date`). The fast plan is not
in the memo at all — this is *not* a costing or frontier-trimming loss.

**Why nothing rescued it (verified):** every other anchor builder categorically rejects calendar
literals — `FilterInValuesOptimizer.isUnsafeCoreInEqualityValue` (`isCalendarDatatype()` →
unsafe), `LmdbJoinPlanSupport.isSafeValuesAnchorValue:919-930`, `FilterValuesAnchorSupport`. The
guarantee path was the *single point of failure* for date anchors, and
`LmdbFilterSimplifierOptimizer.shouldKeepObjectFilterForCascadesCosting:937` deliberately
suppresses its own anchoring in multi-pattern queries *on the assumption that the guarantee rule
will do it* — an unenforced cross-component contract that broke silently.

Context: `4400626f3a` was the fix for the predicate-guarantees-review calendar-anchor finding (a
real correctness bug for `xsd:dateTime` lexical variants: `24:00:00`, timezone normalization).
The fix overshot: timezone-less `xsd:date` has a unique lexical form per value and the old
`DATE_WITHOUT_TIMEZONE`↔`DATE_WITHOUT_TIMEZONE` guarantee match was sound.

**Fix (Routine A):** introduce `CANONICAL_DATE` (and if needed `CANONICAL_TIME`) facts in
`RdfTermDomain.classifyCalendar` for timezone-less canonical `xsd:date`, accept them in
`calendarAnchorMatchesGuarantee`, bump `PREDICATE_OBJECT_DOMAINS_VERSION` to `rdf-term-domain-v4`
(the mask depends on `Fact.ordinal()`!). Failing test first: a date-IN plan test asserting a
`BindingSetAssignment` anchor exists (plus a benchmark spot-check; expected recovery to ~1ms).

### 2.3 q7 — 12 → 69 ms: the anti-join plan is no longer generated (CONFIRMED, mechanism refined)

Query: medication code-IN + `FILTER EXISTS {?patient hasMedication ?med}` + `MINUS {…dosage…}`.

- **June plan (12 ms):** MINUS rewritten to a *correlated NOT-EXISTS anti-probe* applied right
  after the tiny VALUES⋈code prefix; type pattern joined last as bound lookup. Objective 60.2K.
  The bulk plan was explicitly **rejected** (dominated at score 687K), and the sibling
  `lmdb-minus-bound-rhs-probe` alternative appears in the June rejected list — i.e. the memo
  contained and costed the whole anti-join family.
- **July plan (69 ms):** bulk `Difference` (generic `SPARQLMinusIteration`) fed by a full type
  scan and a full dosage scan (13.8K × 13.8K), placed by `minus-join-prefix-pushdown`, under a
  `lmdb-materialized-exists-semi` EXISTS (114K work rows). ~130M of 187M ns go into the
  Difference's **per-left-row linear compatibility scan that the cost model does not price**
  (it charges L + R linearly: 13.9K + 16.7K).

**Root cause (confirmed at commit level, mechanism established):** the July q7 section contains
**zero** occurrences of `lmdb-correlated-minus-anti-exists` or `lmdb-minus-bound-rhs-probe` —
neither as winner nor among dozens of rejected alternatives. The alternative was *never
generated*: `LmdbCorrelatedMinusAntiExistsRule.apply` silently returns `List.of()` when its
statistics probe comes back empty
([LmdbCascadesRuleProvider.java:6574-6578](core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java)).
What changed under it: the Jul 1–3 LEO rework —
`8e9866cc31` ("wip") flipped `operatorFeedbackTracking` default **false→true** and wired
`leoFeedbackCorrection` into `CascadesCostModel.applyFeedback`; `d343482b74` ("more leo") made
`LeoRolloutProfile` default to `SAFE_CARDINALITY_CORRECTION` (learned cardinality correction ON
with no opt-in) and added `applyLeoPlanReranking` into the planner; `625917151f` added a
JVM-global static `CORRELATED_ANTI_EXISTS_EXACT_CACHE` that **caches empty/unsupported probe
outcomes stickily** (keyed by `System.identityHashCode(tripleStore)`, wiped wholesale when full).
June's winner was priced off the `sketch_nested_not_exists` passRatio≈0 decision-driven estimate;
that path was demoted/replaced (July: `plannedEstimateSource=learned_filter`, dosage-CONTAINS
passRatio=1.00, confidence 0.25→0, uncertainty 9.9K→41.7K). Two alternative mechanisms were
adversarially **refuted**: the `inputBoundVars` physical-property rejection is impossible at HEAD
(containment check direction), and the exact-probe cache post-dates the test breakage.

The 5 red `medicalQ7` tests in `LmdbIndexAwareJoinOrderPlanningTest` (re-run during this
investigation: 33 tests, 6 failures, surefire evidence captured in
`logs/mvnf/20260706-221530-verify.log`) encode the June plan as contract and went red exactly at
the Jul 1–2 commits. The re-run also showed: for the code-VALUES membership the rewrite
alternatives *are* still generated but cost ranking now picks `earlyMembershipJoin` over the
semi-filter variant — same theme, ranking driven by degraded estimates.

**Fix (Routine A/B, three parts):**
1. Never silently drop a legal alternative: when the anti-exists probe is empty, still emit the
   alternative with a conservative estimate (or emit with `UNKNOWN` and let uncertainty costing
   handle it). Silent `return List.of()` on estimator failure is a correctness-of-search bug.
2. Price `Difference`/`SPARQLMinusIteration` honestly: model the per-left-row compatibility scan
   (O(|L|·|R|) worst case), or better, replace the executor with a hash-keyed anti-join when key
   vars are shared (see §5, executor work).
3. Gate LEO: `SAFE_CARDINALITY_CORRECTION` must not be the silent default (see §2.6).
   Gate: the 5 red medicalQ7 tests.

### 2.4 q3 — 89 → 225 ms: a semantic rewrite is preselected without costing the original (high confidence)

The `FILTER(?optValue > 60)` is now enumerated into a 39-value `VALUES {61..99}` anchor
(`boundedIntegerValues`, cap `MAX_RANGE_FILTER_VALUES=256`) and the whole join re-anchored
backward from it, dragging 38.7K rows through 4 joins + a per-row NOT-EXISTS. Three compounding
defects, all quoted from the July plan artifacts:

1. **The original plan was never costed:** `optimizer.guaranteeOptionCandidates` says
   `original[skipped-semantic-rewrite selected=finite-anchor:value total=n/a cmp=inf]` —
   `preselectSemanticRewrite` (LmdbCascadesRuleProvider.java:3826-3848, 4895-4931; gates
   `SEMANTIC_REWRITE_RISK_COMPETE_RATIO=1.50`, `MIN_WORK_IMPROVEMENT_RATIO=0.90`, landed
   `e5a3423b54` 2026-06-09, after the June-03 run) short-circuits the competition.
2. **The winner is under-costed ~1000x:** the `lmdb-finite-binding-lookup` DP step reports
   `work=39, rows=1` for `SP(?obs med:value ?value)` under the anchor while *the same candidate
   record* carries the truth (`anchorAccessRows=38762`) — accessRows are computed and then
   dropped from the comparable-work total (`estimateFiniteBindingLookupCost`,
   LmdbEvaluationStatistics.java:8891-9003).
3. **The good order is over-costed ~40x:** a VALUES assignment placed after bound factors is
   costed as a cartesian `lmdb-binding-set-assignment-bridge` (921,024 rows!) instead of a
   selective semi-filter (landed `bf170dbeb3` 2026-06-02 — also implicated in the May→June
   89→132 drift).

Combined costing distortion in favor of the bad plan: ~10^5. (Not separately re-verified because
the plan artifacts were deleted by a concurrent rebuild at 00:15; the quotes above were captured
before deletion and the actuals survive in `result-latests.txt` lines ~504-553.)

**Fix (Routine A):** delete the preselection short-circuit — semantic rewrites must *compete* in
the memo, never preselect; make `estimateFiniteBindingLookupCost` include anchor access rows in
comparable work; model BSA-after-bound-vars as a semi-filter. This also honors the papers2 corpus
rule you curated yourself: *"Never allow a statistical estimate to authorize a semantic rewrite."*

### 2.5 The improvements, attributed (rollback hazard)

`d800e00e83` ("wip", 22:28 — 65 minutes before the run) **hardcoded
`LmdbHypergraphJoinPlanner.enabled()` to `return true`**, un-gating the day-old DPhyp bushy
planner that `05ae2ecd2f` had explicitly shipped default-off. The improvements (q1 −38%, q6 −59%,
q10 −37%, and q9's better number) correlate with DPhyp plans confirmed in q1/q9/q10 finals, plus
the genuinely good new `lmdb-remove-unused-optional` rule (q2/q5). Consequences:
- Docs, tests (`flagOffKeepsTheExistingPlanner` is now red-by-inspection), the ExecPlan and
  memory notes all still say "default off" — the benchmark ran a different planner stack than
  believed.
- **Do not "fix" the regressions by mass revert**: you would undo the DPhyp wins. The three
  regression fixes above are surgical and independent.
- DPhyp's own integration has a real bug to fix before enabling deliberately: `toTupleExpr`
  drops `JoinPlan.Kind` and never sets `optimizer.joinAlgorithmHint=hash`, so plans *costed* as
  hash joins *execute* as per-row nested loops (LmdbHypergraphJoinPlanner.java:291-304).

### 2.6 Reproducibility: the benchmark measures a moving target

Three independent nondeterminism sources, all confirmed:
1. **Learned feedback on by default** (`8e9866cc31` flipped tracking default;
   `d343482b74` defaulted `SAFE_CARDINALITY_CORRECTION`), persisted to sidecars
   (`.rjes.operators`, 1.2MB) that are written *during* runs, reloaded across sessions, and
   validated by an unrelated file's size+mtime (`metadata.bin`). Worse, learned fanout replaces
   store-backed cardinalities at confidence ≥ 0.55 **bypassing the `operatorFeedbackApply`
   opt-in flag entirely** (LmdbEvaluationStatistics.java:11594).
2. **JVM-global static caches** keyed by `System.identityHashCode(tripleStore)`
   (anti-exists probe cache, statement-pattern cardinality cache) with wholesale-wipe eviction.
3. **Wall-clock budget**: 500 ms cascades timeout + AUTO thresholds make the winner
   load-dependent; the "small query exact tier" is dead because `DEFAULT_SMALL_QUERY_MAX_NODES=16`
   counts *every* QueryModelNode (Vars, constants), so even a 4-pattern query lands in budgeted
   mode and can go "approximate" via frontier-count trimming.

---

## 3. Systemic diagnosis — why this keeps happening

The three regressions are not three unlucky bugs. They are the predictable output of five
structural properties, all confirmed by independent audit agents:

**(a) Alternatives leak out of the search space silently.** Rules bail with `return List.of()`
on empty/failed probes (q7); eligibility guards hard-reject entire datatype families (q2);
mini-optimizers inside rules preselect winners and emit one opaque plan (q3;
`LmdbGuaranteeOptionImplementationRule` is 1,893 lines — 26% of the rule provider — runs its own
JoinOrderPlanner per option, its own risk model, and returns a single pre-picked plan,
subverting the memo). There is no "alternative was lost" telemetry; `generated=0` was recorded
but nothing alerted.

**(b) No single cardinality/cost authority.** Per-pattern cardinality has ≥5 competing sources
(LEO learned fanout > pattern source > page-walk > bucket sampling > generic), join cardinality
another ~5, reconciled by confidence thresholds and *source-string matching*.
`estimateFactorCost` stacks six sequential override layers. Twelve distinct row-estimate carrier
types coexist in one file. The same question — "how much do I trust this sketch?" — is answered
with **eight different hardcoded q-error constants** (1.5/1.75/2.0/2.5/3.0/4.0) at different call
sites. Zero-row sketch verdicts are silently patched back to baseline. Estimator failures are
swallowed by blanket `catch (IOException | RuntimeException) { return -1; }` with no logging (in
at least 9 places).

**(c) Five-to-six join planners coexist** (Cascades connected-DP, DPhyp, bounded-greedy *inside
the statistics class*, legacy sketch planner — 8,062 lines, dead by default, still carrying 931
if-statements — standard RDF4J baseline, plus the guarantee rule's private mini-planner),
arbitrated by an underived 1.20x fudge factor and three mutually-inconsistent cost
representations. `CostVector.objectiveScore` is a weighted kitchen sink with eight underived
constants mixing cost, uncertainty, and evidence-count.

**(d) The learning system can overrule ground truth without consent** (§2.6). LEO "evidence"
counts are inflated by the planner's own candidate enumeration (planning observes itself);
`rankPlanCandidates` compares learned actual-work-rows against cost-objective scores *in one
comparator with incompatible units*; unseen values get fanout estimates by *hashing their LMDB
value ID into a quantile bucket*.

**(e) The test suite enforces shapes, not quality.** 184 exact plan-string assertions; 82 test
methods named after benchmark queries; the only corpus-level estimate-quality gate
(`LmdbEstimateAuditHarnessTest`, 300 queries) is 100% `@Disabled`; 16 red classes are
institutionalized as baseline. Exact-shape tests + threshold farms = each fix is a new patch that
breaks two other shapes. This is the "monkey patched our way to benchmark results" mechanism,
institutionalized.

Also confirmed benchmark-shaped code (no theme vocabulary, but the shape gives it away):
`lmdb-finite-code-type-values-rewrite` fires only on COUNT(DISTINCT) over a Union of *exactly
two* type/code filter branches with a variable literally named "type"
(LmdbCascadesRuleProvider.java:1376-1432); the legacy sketch optimizer assigns any `IN(constants)`
filter a hardcoded selectivity score of 950/1000 with zero data evidence.

God-class league table: `LmdbEvaluationStatistics` 11,613 lines / 6 interfaces (including
`JoinOrderPlanner`!) / 1,380 ifs / ~30 magic constants; `SketchBasedJoinEstimator` 17,391 lines;
`LmdbSketchJoinOptimizer` 8,062 (dead); `LmdbCascadesRuleProvider` 7,147. Pipeline: 20+ passes,
several run 2–3×, ~30 undocumented `rdf4j.optimizer.*` system properties (including the
near-duplicate `leoPlanRanking` vs `leoPlanReranking` — a live typo-gate).

---

## 4. State-of-the-art gap analysis

### 4.1 What MySQL does that this codebase must copy (from mysql/sql/join_optimizer/)

| Discipline | MySQL | This repo today |
|---|---|---|
| Cost currency | ONE struct (`AccessPath`: rows, cost, init_cost, init_once_cost, …) with `kUnknownCost` sentinels and asserts | ≥3 incompatible cost representations, 12 estimate carriers, 0.0-as-unknown |
| Enumeration | Pure header-only DPhyp; receiver = 3 methods; ALL normalization once, in `make_join_hypergraph` | Rules contain estimation, costing, planning; 5–6 enumerators |
| Degradation | Exhaustive up to 100K subgraph pairs, then *deterministic* `GraphSimplifier`, then DP again — a 4-table query can never be "approximate" | count-capped winner frontiers; frontier saturation in ANY group poisons the whole search with "approximate"; 500 ms wall-clock timeout |
| Pruning | Typed dominance (`CompareAccessPaths`: cost/ordering/parameterization) + fuzzy epsilon, never by count | top-16-per-group count trim |
| Statistics boundary | 531-line `estimate_selectivity.cc`; 3 typed sources; `handler::records_in_range`; every constant documented with provenance (`cost_constants.h`, unit = measured µs) | 11,613-line god class; 28 estimate sources; ~254 underived constants |
| Risk policy | Documented directional bias: prefer the *least* selective estimate; damp NDV products (`^0.67`); continuous estimators (`SmoothTransition`) to avoid rounding-flip plan changes | 8 ad-hoc q-error constants; stacked overrides; discontinuous threshold cliffs |
| EXPLAIN | One JSON document per node (estimates + actuals + provenance); text rendered *from* JSON | annotation strings concatenated into plan text; decisions in debug-string side channels |
| Optimizer tests | 8,735-line unit test against *mock engines/stats* — pins enumeration/costing independent of live statistics | exact plan-string tests against a live store with learned state |

### 4.2 What your own papers2 corpus prescribes (and the code ignores)

The corpus's governing principle (papers2/papers/README.md): *"Keep proven facts, estimated
facts, and observed facts separate. Proven facts authorize rewrites. Estimated facts guide cost.
Observed facts repair future estimates."* — and explicitly: *"Never allow a statistical estimate
to authorize a semantic rewrite."* The q3 preselection and q7 probe-bail violate this directly;
LEO overwriting base cardinalities un-gated violates the observed/estimated separation.

Key paper contracts vs implementation:
- **DPhyp** (Moerkotte/Neumann '08): the enumerator is faithful ✅, but there are **no conflict
  rules / TES-hyperedges for non-inner operators** — OPTIONAL/MINUS/EXISTS never enter the
  hypergraph, which is exactly where q7-class regressions live. MySQL's
  `make_join_hypergraph.cc` shows the full recipe.
- **q-error** (Moerkotte/Neumann/Steidl '09): q-error must be *derived from synopsis
  construction* and propagated multiplicatively — not eight hardcoded constants.
- **Leis et al. '15**: cardinality error dominates cost-model error by orders of magnitude;
  defensive rules: never pick non-index NL joins on estimated (not guaranteed) advantages; add a
  *true-cardinality injection mode* to the benchmark to separate search-space wins from
  estimation luck.
- **Characteristic sets** (Neumann/Moerkotte '11): present but incomplete — missing per-predicate
  occurrence annotations and the distinct×∏multiplicities×min-conditional-selectivity contract.
- **LEO** (IBM): the original paper *validates adjustments before applying them* and keeps them
  per-plan-context; the current implementation applies corrections globally, keyed loosely, with
  self-inflating evidence.
- Confirmed absent, prescribed by the corpus: **sideways information passing / runtime bloom
  filters** (RDF-3X-style SIP), **WCOJ/leapfrog** for cyclic/star joins, robust plan selection
  (least-expected-cost / PARQO-style penalty-aware ties), mid-query re-optimization,
  metamorphic result-equivalence testing (NoREC/TLP).

---

## 5. Roadmap

Ordered so that every phase pays for itself; each item names its gate. Phases 0–1 are days,
2–4 are the real re-architecture, 5–6 make it state-of-the-art rather than merely sound.

### Phase 0 — Stop the bleeding (Routine A each, small diffs)
1. **q2:** `CANONICAL_DATE` fact + accept in `calendarAnchorMatchesGuarantee`; bump domain
   version to v4. Gate: new failing date-IN anchor plan test; benchmark q2 ≈ 1 ms.
2. **q7:** never drop alternatives on empty probes (emit with conservative/UNKNOWN estimate);
   price `Difference` superlinearly. Gate: the 5 red medicalQ7 tests.
3. **q3:** remove `preselectSemanticRewrite` short-circuit (rewrites compete, never preselect);
   include `anchorAccessRows` in finite-lookup comparable work; fix the BSA-bridge cartesian
   costing. Gate: new q3-shaped plan test + benchmark.
4. **Un-hardcode `LmdbHypergraphJoinPlanner.enabled()`** (restore the flag; decide default
   explicitly after the hash-hint bug below is fixed). Gate: `flagOffKeepsTheExistingPlanner`.
5. **Make LEO opt-in again** (tracking default off, `SAFE_CARDINALITY_CORRECTION` not default,
   fix the ≥0.55 ungated fanout replacement and the `leoPlanRanking`/`leoPlanReranking` typo).
   Gate: re-run module suite; expect several of the 16 red classes to green.
6. Fix DPhyp `toTupleExpr` dropping `JoinPlan.Kind`/hash hint so hash-costed plans execute as
   hash joins.

### Phase 1 — Determinism & honest measurement (prerequisite for all tuning)
- Deterministic planning mode for benchmarks/tests: learned state off, no wall-clock budget
  (task-count budget only), fixed store snapshot. JMH: ≥3×5s iterations, error columns kept.
- Log-and-count every swallowed estimator exception; forbid silent `catch → -1`.
- "Alternative lost" telemetry: any rule that returns no alternatives for a matched pattern
  records why (q2's `generated=0` should have been an alarm, not a footnote).
- True-cardinality injection mode in the theme benchmark (Leis method) to separate enumeration
  quality from estimation quality.

### Phase 2 — One cost currency, one statistics boundary
- Introduce `PlanCost` (rows, cost, initCost, initOnceCost, rowsBeforeFilter/costBeforeFilter,
  UNKNOWN sentinels, asserts) as the *only* thing estimators return and planners compare.
- `LmdbCostConstants` with MySQL-style provenance: one measured unit cost (µs per cached SPO
  scan row on a reference store) and every constant expressed relative to it, plus a JMH
  calibration harness. Delete per-call-site q-error constants; each estimate carries the q-error
  of its *source rung*.
- Extract the narrow typed statistics SPI (3 small interfaces: pattern counts / join
  surfaces / value distributions) and put `LmdbEvaluationStatistics` behind it, then dismantle
  the god class: statistics, cost model, greedy planner, LEO service, and telemetry become
  separate modules. Explicit documented fallback ladder (exact → per-predicate → characteristic
  set → sketch → sampled → default), each rung tagged with derivation and q-error;
  disagreement policy: least-selective wins (documented, MySQL-style), continuous transitions.

### Phase 3 — One join planner
- Make the hypergraph package (your best code) the *only* join-order planner: add conflict
  rules/TES so OPTIONAL/MINUS/EXISTS/LATERAL enter the hypergraph (MySQL
  `make_join_hypergraph.cc` is the template); typed-dominance Pareto retention (reuse the
  existing `ParetoFrontier`/`JoinCostVector`) instead of count-capped frontiers; deterministic
  graph-simplification fallback instead of wall-clock "approximate".
- Then **delete**: `LmdbSketchJoinOptimizer` (8,062 dead lines), the bounded-greedy planner
  inside the statistics class, the guarantee rule's private mini-optimizer (guarantee options
  become ordinary memo alternatives), `join-commute`/`join-associate-right` for reorderable
  inner islands, and the 1.20x arbitration fudge. Rules shrink to: match → emit alternatives;
  all costing in the cost model, all selection in the memo.
- Pipeline hygiene: each pass runs once, ordering documented and asserted; flags consolidated
  into one documented config class (~30 → ~10).

### Phase 4 — Statistics that earn their keep
- Complete characteristic sets to the paper contract; add per-predicate
  {count, distinctS, distinctO} and small-domain value histograms (equi-height + singleton
  only, MySQL-style, with refresh metadata); LMDB range dives (`records_in_range` analogue) for
  bound prefixes — you have B-tree page-walk machinery already.
- Rebuild LEO on the original paper's contract: observe → *validate* → apply per
  plan-context, execution-only evidence (never planner self-observation), decay + versioned
  invalidation tied to its *own* snapshot token, always-off in deterministic mode.

### Phase 5 — Test strategy that resists monkey-patching
- Replace plan-string assertions with: (a) metamorphic result-equivalence per rewrite rule
  (NoREC/TLP-style), (b) mock-statistics optimizer unit tests (MySQL
  `hypergraph_optimizer-t.cc` pattern) pinning join-tree choice under *hand-set* cardinalities,
  (c) re-enable the 300-query estimate-audit harness with q-error budget gates as CI,
  (d) a plan-quality corpus gate: chosen-plan cost within X of best-known on random queries.
- Triage the 96 `@Disabled` and 16 red classes: each becomes green, deleted, or converted —
  none stay "red baseline".

### Phase 6 — Beyond MySQL (the actual state of the art for RDF)
- Sideways information passing (RDF-3X style) — optimizer-visible runtime filters credited by
  the cost model.
- Robust plan selection using the uncertainty machinery you already carry (QErrorInterval):
  penalize plans whose pessimistic-end cost explodes; risk-aware tie-breaking.
- WCOJ/leapfrog as a physical alternative for cyclic/star islands.
- Mid-query re-optimization for q3-class 10^3+ estimate misses.
- Property-path-aware planning on the existing PropertyPathEstimate hooks.

---

## 6. Evidence appendix

- Regression window commits (ranked): `4400626f3a` (q2, calendar anchor), `8e9866cc31` +
  `d343482b74` (q7/q3 LEO defaults + cost inputs), `d800e00e83` (DPhyp hardcoded on),
  `e5a3423b54` + `bf170dbeb3` (q3 preselection + bridge costing), `625917151f` (sticky probe
  caches), `a2df217c28` (17 rules default-on → frontier pressure), `8fd01ccd3d` (index scoring,
  pipeline order), `327771e45e`/`59ed128ab6` (estimate-cache identity hazards), `c854deabc6`
  (omni ingest rewrite, in the benchmark binary), `e5039c6a59` (set-semantics bail-out).
- Test evidence: `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbIndexAwareJoinOrderPlanningTest
  --retain-logs` → `Tests run: 33, Failures: 6` (surefire:
  `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbIndexAwareJoinOrderPlanningTest.txt`,
  log: `logs/mvnf/20260706-221530-verify.log`).
- Plan artifacts: June plans embedded in
  `.../theme-query-benchmark-results/results-2026-06-03.md`; July plans in `result-latests.txt`
  (the `target/.../plans/` copies were deleted by a concurrent rebuild 2026-07-07 00:15 — quotes
  in this report were captured before deletion).
- Full agent outputs (27 agents, per-area audit JSON with file:line findings):
  `/private/tmp/claude-501/.../tasks/split/` (session-scratch; copy out if wanted).
- Known confound checked and cleared: `sketchEstimatorStrategy=omni` is new as a JMH *parameter*
  but not as behavior — only OMNI is a real strategy in `SketchBasedJoinEstimator`
  (all other strategies hard-return empty), and the June store was omni-built.
- Open items (not closed by this investigation): per-improvement RCA for q5/q6 (attributed only
  collectively), q9's relationship to the red `LmdbThemeQ9EstimateRegressionTest`, executor-side
  audit beyond `SPARQLMinusIteration` (materialized-exists operator, value materialization),
  and the `inlined/`, `model/`, `util/` packages (not audited this round).
