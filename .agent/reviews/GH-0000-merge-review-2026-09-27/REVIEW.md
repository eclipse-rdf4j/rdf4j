# Review: GH-0000-lmdb-predicate-guarantees vs develop (2026-09-27)

Scope: the branch after merging `origin/develop` (merge commit `4354ff137b`). Focus: planner correctness and
robust query planning. Each feature was reviewed separately by an isolated reviewer that wrote reproducer tests;
only findings backed by a failing test that was actually run are marked **CONFIRMED**. Reproducers and evidence are
in `repro/<feature>/` next to this file (they are intentionally red; not added to the module test trees).

Legend: **REG** = regression vs develop · **BR** = introduced on this branch before the merge · **PRE** = also in
develop/upstream · Severity: S1 wrong results/query failure, S2 catastrophic plan / resource, S3 robustness/quality.

## 1. Merge outcome

- 184 develop commits merged; 24 conflicted files (88 hunks) resolved; version now 6.2.0-SNAPSHOT.
- Tests after the merge: queryalgebra-evaluation 1729, model 83, memory 798, sail-base 20, query 243, LMDB unit 2286
  + IT 144 — **0 failures**. SPARQL compliance: 54 failing tests, **identical to the pre-merge branch** (no merge
  regressions; see §3.9).
- Merge-induced failures found and fixed: 4 LMDB tests (stats-logging expectations, FilterOptimizer MINUS push,
  join-order policy interaction, renamed LmdbUtil frame) plus two API renames in tests.
- Key policy decision: `QueryJoinOptimizer` is develop's version verbatim (cost-first, connectivity only as a
  tie-break). Porting the branch's "property paths are reorderable bridges" onto it produced cross products
  (`LmdbEstimateAuditHarnessTest`), so it was reverted. Four branch tests asserting connectivity-first ordering were
  removed — see finding 3.8.5 for why connectivity-first is the more robust policy.
- Copyright check flags 4 branch files (invalid header / missing SPDX): `LmdbDevelopPlanParityIT`,
  `FactorizedTupleExprEvaluator(Test)`, `FactorizedSolutionBagFingerprint`.
- Build still fails `japicmp` for rdf4j-sail-lmdb (BR, commit d13767b49c): public
  `LmdbStoreConfig.get/setSketchEstimator{Subject,Predicate,Object,Context}BucketCount` and
  `…ContextPairSketchesEnabled` were removed but exist in 6.0.0. Restore them as deprecated no-ops.
- Integration risk introduced by the merge (develop's fixed LMDB reader pool, 127+1 slots, 30 s wait): branch
  planner code still takes nested *ordinary* read slots while the query holds one (see §3.10).

## 2. Top priorities

| # | Finding | Sev | Origin |
|---|---------|-----|--------|
| 1 | Plan caches reuse data-dependent plans after commits/restart → missing rows (3.3) | S1 | BR |
| 2 | Packed planner: MINUS→NOT EXISTS and EXISTS-dependency bugs change results (3.2) | S1 | BR |
| 3 | LMDB rewrites unsound inside EXISTS/LATERAL; sameTerm treated as `=`; lang-tag anchors (3.1) | S1 | BR |
| 4 | OPTIONAL-with-condition over sub-SELECT leaks outer bindings, all stores (3.4) | S1 | REG |
| 5 | LMDB: any `VALUES … UNDEF` query and SPARQL UPDATE with default-graph dataset throw NPE (3.9) | S1 | BR |
| 6 | Rolled-back transaction data persisted into learned stats as `database_exact` (3.7) | S2 | BR |
| 7 | Frontier statistics can stall permanently at default budget; cold start plans on 1-row guesses (3.6) | S2 | BR |
| 8 | Planning failures surface as query errors (single-flight waiters) (3.8) | S1 | BR |
| 9 | Normalizer pushes VALUES through a scoped BIND → wrong `!BOUND`/`NOT EXISTS` results, all stores (3.11) | S1 | REG |
| 10 | GeoSPARQL/custom-function filters no longer pushed down in any store (3.11) | S2 | REG |
| 11 | Planner nested reads starve on develop's fixed LMDB reader pool (30 s stalls) (3.10) | S2 | merge |

## 3. Findings by feature

### 3.1 LMDB logical rewrites (`core/sail/lmdb/.../Lmdb*Optimizer`, FilterSimplifier) — repro `f1`

All CONFIRMED (LMDB vs standard pipeline on the same store vs hand-computed SPARQL result).

1. **BOUND folding inside correlated scopes** (S1, BR). `LmdbBoundSimplifierOptimizer:57,69,109` treats only
   top-level bindings as incoming but descends into EXISTS/LATERAL:
   `?s :p ?x FILTER EXISTS { ?s :q ?o FILTER(BOUND(?x)) }` → `[]` (expected `s1`); `!BOUND` in NOT EXISTS inverts;
   LATERAL same. Fix: carry outer-visible names into EXISTS bodies and LATERAL right args (or never fold to false in a
   correlated context).
2. **`makeOptionalBindingMandatory` uses possible names** (S1, BR). `LmdbFilterSimplifierOptimizer:519-546`:
   `?s :p ?o OPTIONAL { { ?s :a ?x OPTIONAL { ?x :b ?w } } } FILTER(?w = :c)` returns a spurious `w=:c` row. Fix:
   require `?w` assured by the right arg and not correlated.
3. **Null-rejecting OPTIONAL→INNER in EXISTS/NOT EXISTS/LATERAL bodies** (S1, BR). Witness chosen although the outer
   row binds it (`FilterSimplifier:163,191`, `LmdbNullRejectingOptionalSupport:212-217`); EXISTS → `[]`, NOT EXISTS
   → spurious row. Same fix as 1 (one shared correlation-aware binding context).
4. **`sameTerm` anchors value-expanded and the filter dropped** (S1, BR). `:483`, `expandFiniteAnchor :1215-1218`:
   `sameTerm(?o, 7)` over `"7"^^xsd:int` returns a row. Never value-expand SameTerm.
5. **Boolean lexical variants** (S1, BR). `" true "^^xsd:boolean` with `FILTER(?f = true)` → `[]`; no canonical-boolean
   fact gates the filter drop (`canMaterializeObjectFilterAnchor :1358`). Add CANONICAL_BOOLEAN or keep the filter.
6. **Language-tag case** (S1, BR+PRE). `FILTER(?o = "abc"@EN)` over `"abc"@en` returns `[]` after reopen but works
   warm: anchors treat lang literals as safe, and `ValueStore.getId` is byte-exact on disk but case-insensitive in
   `valueIDCache` (`:1996`) → results differ across restarts. The plain pattern form also fails cold in the standard
   pipeline (storage bug).
7. Refuted: FilterHoist, SetSemantics, OptionalNormalForm rewrites.

### 3.2 Packed Cascades rule program (`optimizer/cascades/packed`) — repro `f2`

1. **MINUS → correlated NOT EXISTS ignores referenced variables** (S1, BR, CONFIRMED e2e).
   `SELECT ?x ?n { VALUES ?x {:a :b} ?x :p ?n MINUS { ?x :q ?m FILTER(?n = ?m) } }` loses `a/1` once the anti-join
   is cheaper. `PackedLogicalRuleProgram:381-421` checks outputs(L)∩outputs(R) only; runtime substitution injects all
   left bindings. Also reachable through the executor's `minus-assured-shared` hint (see 3.4.4). Fix: require
   fv(R) ∩ pv(L) ⊆ shared names (the codec already computes referenced sets, `PackedQueryCodec:1245`).
2. **EXISTS dependencies = outputs only** (S1, BR, CONFIRMED e2e). `addScalarDependencies :3621-3629` → unused-OPTIONAL
   pruning and projection-below-ORDER drop `?b` that `FILTER EXISTS { … FILTER(?c=?b) }` observes: DISTINCT `[]`
   instead of `[a]`, NOT EXISTS `[a,b,c]` instead of `[b,c]`, ORDER BY DESC(EXISTS…) LIMIT 1 wrong row.
3. **Domain-fact lattice mixes "unknown" and "none"** (S1, BR, CONFIRMED e2e). `PackedDomainFacts.widen:198` ORs
   language bits while VALUES-seeded facts leave them 0 = unknown → `langMatches(lang(?o),"*")` over a UNION with
   `VALUES {"a"@en}` becomes EmptySet.
4. **Group-wide fact poisoning** (S1, BR, planner-level). `PackedDomainFacts.put:65-70` intersects facts of all group
   members, so an unsound alternative (e.g. `expandAnchorValue :2750-2762` using `equals` instead of value equality,
   or the code/type rule without a term-identity check) can make an unrelated sibling EmptySet.
5. Standard `FilterOptimizer.FilterRelocator.meet(Join)` (`:349-365`, PRE, all stores) pushes filters using
   *possible* names onto a LeftJoin; LMDB's null-rejection then turns it into an inner join:
   `?s :p ?y OPTIONAL{?s :q ?x} ?s :r ?x FILTER(?x=?y)` loses rows. `PackedFilterRules:90-92` has the same pattern.
6. Refuted: duplicate sensitivity of pruning, uncommitted overlays on tested paths, lang-tag case in packed facts.

### 3.3 Plan caching under updates — repro `f3`

1. **Lossy `equals` authenticates packed cache hits** (S1, BR, CONFIRMED e2e). `PackedPlanCache:849` +
   fingerprint `:308-372`; `ArbitraryLengthPath.equals` ignores `minLength`, `GroupConcat.equals` ignores the
   separator → cached `p*` plan served for `p+` (zero-length rows), `','` separator served for `'|'`.
2. **Stale data-dependent plans reused after commits** (S1, BR, CONFIRMED e2e). Stored-term VALUES anchors and
   guarantee-based filter drops made before Cascades are invisible to cache dependencies
   (`LmdbPipelinePlanCache:246-254, 373-396`; `LmdbPlanDecisionCache:1687-1698` returns UseAndRefresh on drift;
   detached refresh starts from the already-rewritten source). `FILTER(?o=5)` then commit `:c :p "5"^^xsd:int` → `c`
   missing, permanently after refresh. `predicateRangeVersion` hashes config only, not guarantee contents
   (`LmdbEstimatorStorageAccess:246-256`).
3. **Cache version stuck after restart** (S1, BR, CONFIRMED with sketch estimator on). `snapshotVersion()` =
   max(in-memory counter from 0, persisted synopsis version) → committed rows invisible via cached EmptySet.
4. **Exact-probe term cache keyed on sketch mutationVersion** (S1, BR, CONFIRMED with sketch on) — not cleared by
   `reset()`.
5. **Single-flight waiters and templates** (S1, BR, planner-level): `Flight.matches :568-575` lets a waiter reuse
   the owner's EmptySet proof for a different constant; `sameQueryContext :839-845` ignores range version (dirty-txn
   planning can get a committed-data EmptySet template).

### 3.4 Execution operators (all stores) — repro `f5`

1. **OPTIONAL with a condition over a sub-SELECT** (S1, **REG**, CONFIRMED MemoryStore).
   `LeftJoinQueryEvaluationStep:95` only uses the independent hash path without a condition; with a condition it
   falls back to the correlated iterator and `ProjectionQueryEvaluationStep:37` passes the left row into the
   sub-select: `FILTER(?t != ?s)`, `ORDER BY … LIMIT 1`, `COUNT` without GROUP BY all wrong. (develop dropped the
   condition entirely — a different bug.) Fix: hash left join with a residual condition.
2. **MINUS inside OPTIONAL** evaluated with the left row (S1, PRE): `OPTIONAL { ?s :q ?y MINUS { ?y :r ?z } }`
   wrong; the Join-side fix (`containsDifferenceInCurrentScope`) is not applied to LeftJoin.
3. **EXISTS over a sub-SELECT leaks outer bindings** (S1, PRE, all stores); the branch's per-probe
   `BindingAssignerOptimizer` substitution also rewrites Vars inside nested Projection scopes.
4. **`minus-assured-shared` hint trusted blindly** (S1, BR, executor-level): the hinted anti-join probes with
   substitution (NOT EXISTS semantics). Planner accepts FILTER/EXTENSION/PROJECTION/LEFT_JOIN/GROUP bodies.
5. **HashJoinIteration** leaks the left input if the right `evaluate` throws (CONFIRMED); planned build side is
   drained without limit/spill (S2).
6. `SameTermFilterOptimizer:83` has the EXISTS blind spot too (PRE, all stores).
7. Refuted: hash-join over-claimed assurance (partial-key indexes make it safe), resource-equality prebinding,
   adaptive EXISTS correctness, FilterIteration.close deadlock.

### 3.5 Predicate guarantees and anchors

Covered by 3.1.4–6, 3.2.3–4 and 3.3.2. Additional plausible race: the guarantee cache is not versioned with the
reader's LMDB snapshot (`TripleStore:851-866, 717-719`).

### 3.6 Cardinality estimation (Frontier V2, synopsis) — repro `f6`

1. **Cold start → 1-row estimates** (S2, BR, CONFIRMED). With Frontier AUTHORITATIVE and no generation yet,
   `LmdbStorageEstimatorEvidence:162,269` suppress B-tree and exact counts ("statistics service exists"),
   `EstimateEvidenceResolver:41-42` falls back to 1 row. Observed: datagovbe OPTIONAL query ran >60 s
   (17.6M intermediate rows) instead of seconds; `QueryBenchmarkTest` stalls when it runs before the first build.
2. **Delta publication stalls forever at the default budget** (S2, BR, CONFIRMED). Insert-only 16k-statement commits:
   publication stops after ~112k statements (delta envelope), failures map to RETRY forever
   (`LmdbSailStore:1169-1172, 1254-1263`), planning degrades to the scalar model and then 1-row leaves; restart does
   not recover. Budget drift via `-Xmx` can also cause permanent dimension mismatch (code reading).
3. **Rebuild starvation** under steady small commits (quiet period reset per commit) (S2, CONFIRMED).
4. **Superseded manifests/shards never deleted** (S3, CONFIRMED) — unbounded disk growth.
5. **Omni bridge accepts a zero** instead of trying the reverse orientation (160,000 true vs point 0) (S2, CONFIRMED).
6. **SHADOW mode changes planning evidence** (S3, CONFIRMED); unbounded per-estimate planning work (123 ms for one
   join estimate) (S3, CONFIRMED).
7. Plausible: stale-generation window between LMDB commit and `recordStoreCommit`; AGMS "upper" not a bound;
   quad-synopsis publish races (sketch estimator only).

### 3.7 Learned feedback (LEO) — repro `f7`

1. **Rolled-back data becomes `database_exact`** (S2, BR, CONFIRMED e2e): commit 200 `:p`, add 500 in a txn, query,
   roll back → new connection plans with `rows=700 source=frontier-v2-exact-fact guarantee=database_exact`.
2. **LeoOperatorKey collisions** (S2, BR, CONFIRMED): 7-factor chain = triangle×chain cross product; `FILTER(?a=?b)`
   = `FILTER(?a=?c)`; `DISTINCT ?s` = `DISTINCT ?s ?o`; 32-bit VALUES hash collisions → corrections shared across
   different operators store-wide.
3. **LeoConfidenceModel** (S2, BR, CONFIRMED): NaN samples count as perfect; linear-space EWMA oscillates under plan
   flips (71.4/28.6); an empty result sets correction 1e-6; one outlier zeroes confidence forever; epoch regression
   stops decay.
4. **Rollout profile**: `disabled`/`false`/`off`/`none`/`0` *enable* correction (S3, CONFIRMED).
5. Plausible: every commit halves the whole frontier model; a post-commit hook failure triggers `rollback()` on a
   durable commit (`LmdbSailStore:2388-2393`); observer effect (feedback-tracked nodes disable guard joins).

### 3.8 Cost model and search — repro `f7`

1. **Cost saturation** (S2, CONFIRMED): NaN/Inf/negative → `Double.MAX_VALUE`; a 10¹²-row plan beats a 10-row plan
   on a tie-break. `EstimateVector.filter` can raise the upper bound.
2. **Plan depends on query text order** (S3, CONFIRMED): 24 permutations of a 4-star → 24 plans when costs saturate.
3. **Budget latch** (S3, CONFIRMED): one refused optional block marks the whole budget exhausted.
4. **Failure asymmetry** (S1, CONFIRMED): single-flight waiters receive `CompletionException`, LMDB only falls back on
   `IllegalStateException` → the query fails; `join()` without timeout.
5. **Cross products** (S2, CONFIRMED for `QueryJoinOptimizer`): a connected 5-chain with cheap endpoints gets
   Cartesian orders; the packed planner (cross products only between disconnected components) does not. Recommend
   porting the packed policy to `QueryJoinOptimizer` (this reverses the merge-time choice of develop's policy).
6. Exactness inferred from confidence; q-error present → uncertainty 0; semi-join zero keeps 0.9 confidence (S2,
   CONFIRMED). `int` masks with up to 64 factors (`1<<33 == 2`) (plausible).

### 3.9 Pre-existing compliance failures (unchanged by the merge)

54 failing compliance tests on both the pre-merge branch and the merge:
- LMDB `LmdbPipelinePlanCache$DatasetIdentity.immutable:502` NPE (`Set.copyOf` with a null default graph) → 32
  SPARQL UPDATE compliance errors (S1, BR).
- LMDB `LmdbPipelinePlanCache$2.meet:430` NPE (`List.copyOf` with UNDEF values) → **any LMDB query with
  `VALUES … UNDEF` fails** (S1, BR).
- `PatternKeys.predicateKey:36` NPE for a variable predicate with a constant object (S1, BR).
- LATERAL/OPTIONAL (SPARQL 1.2 `tests()[6]`, `lateral()[2]`) in all stores; custom aggregate `DISTINCT` (4);
  LMDB `builtinFunction()[20]`.

### 3.10 Merge-integration risks — repro `f8`

- **Reader pool starvation** (S2, CONFIRMED). Develop's fixed pool (127 ordinary + 1 reserved, 30 s admission wait)
  meets branch planner code that takes nested *ordinary* slots while the query already holds two (explicit +
  inferred dataset), so ~63 concurrent queries saturate it. With only the dataset slots free,
  `?s :p ?o . ?s :q ?n FILTER(?n>150)` blocks >8 s in `TripleStore.sampleTriplesStratified` (via
  `LmdbFilterSelectivityStats.sampleFilterPassRatio`) and then silently loses the estimate after 30 s. Branch-added
  sites: `TripleStore:1419,1506,1810,2106`, `LmdbFiniteJoinSurfaceEstimator:131,173,369`,
  `LmdbFilterSelectivityStats:685,777,1342` (develop's own filter sampler has one such site). One reserved slot is
  not enough and `LmdbFiniteJoinSurfaceEstimator:369` would self-deadlock if converted naively. Fix: pass the query's
  pinned dataset txn into planner estimators (also makes estimates read the execution snapshot); cap the
  `repeatedVariableCardinality`/`exactCardinality` scans.
- **Sampled planning cardinalities labelled `DATABASE_EXACT`** (S3, merge). Accuracy of develop's bounded estimator is
  good (0.99–1.0 on prefix patterns, ~2× on residual probes), but `LmdbPackedCostModel:386-388` stamps
  `finite_binding_probe` rows (now sampled via `planningCardinality`) as exact: a sampled 0 becomes `EXACT_ZERO` and
  misestimates are excluded from learning.

### 3.11 Standard pipeline changes visible to Memory/Native — repro `f8`

1. **VALUES pushed through a scope-changing BIND** (S1, **REG**, CONFIRMED MemoryStore).
   `QueryModelNormalizerOptimizer:102-164` checks only *possible* names:
   `VALUES ?x {:a :b} { GRAPH :g { VALUES ?x {UNDEF} ?s :p ?o FILTER(!BOUND(?x)) } BIND(1 AS ?one) }` → `[]`
   (expected two rows); the `NOT EXISTS` variant returns spurious rows. Fix: also require
   `extension.getArg().getAssuredBindingNames().containsAll(assignmentNames)`.
2. **FilterRelocator pushes into a join side that only possibly binds** (S1, PRE): see 3.2.5; also
   `VALUES ?x {UNDEF :w} ?s :p ?x FILTER(sameTerm(?x,:v))` → `[]`. LMDB exposure grew because the first pipeline pass
   is now `(false,false)`. Fix: push into side S only if each filter var is assured in S or absent from the other side.
3. **`ScalarEvaluationEffects` blocks filter pushdown for non-core functions** (S2, **REG**, CONFIRMED). GeoSPARQL,
   SPIN and user functions are UNKNOWN (package + CodeSource gate, `:122-159`): `geof:sfWithin` stays above the whole
   join, and an unknown conjunct also pins cheap siblings like `STRSTARTS`. A null CodeSource (native image, custom
   classloaders) would disable almost all relocation. Fix: treat functions as REPEATABLE unless they declare
   `mustReturnDifferentResult()` (denylist, not allowlist).
4. `SameTermFilterOptimizer` EXISTS shortcut (~86-92) turns `FILTER EXISTS { … FILTER(sameTerm(?x,:v1)) }` into
   `Exists(EmptySet)` (S1, PRE).
5. Refuted/benign: `OrderLimitOptimizer` (the branch fixes a develop bug), BindingSetAssignment/Extension assurance
   (from develop), dropped `{} MINUS {} → EmptySet` (a correct fix).
6. Not deeply reviewed yet and high-impact for all stores: EXISTS now always goes through
   `MaterializedExistsFilterIteration`; large rewrites of LeftJoin (624 lines), HashJoin (535) and Path (483)
   evaluation steps. Recommend a differential compliance run against develop.

## 4. Robust-planning roadmap (grounded in current practice)

1. **One binding/scope analysis for every rewrite** — possible, assured, *referenced* and *correlated* variables
   computed once (per memo group in the packed planner, per node elsewhere) and consumed by all rules; rules that
   introduce bindings require *assured*; possible names may only block. (Pérez et al. well-designed patterns;
   Cascades/Columbia logical properties.) Fixes 3.1.1–3, 3.2.1–2, 3.2.5, 3.4.1–3.
2. **Rewrite certificates validated at materialization and re-checked at execution** — each data-dependent rewrite
   (EmptySet proof, filter drop, stored-term anchor, exact zero) records its assumption set (term-domain generation,
   data revision, snapshot); caches key on it; operators re-validate against the execution snapshot or fall back to
   the unrewritten subplan (Graefe/Ward choose-plan). Fixes 3.3.
3. **Unknown ≠ 1** — fallback chain Frontier → B-tree range count → bounded exact count → pessimistic ceiling;
   interval upper bounds from degree-sequence norms (Cai et al. SIGMOD'19; LpBound 2025) and max-frequency bounds
   (Hertzschuch et al. CIDR'21) instead of |L|·|R|; bidirectional join sampling (Wander Join; index-based sampling,
   Leis et al. CIDR'17).
4. **Plan under uncertainty** — cost with a confidence-dependent quantile (Babcock & Chaudhuri 2005) in log space;
   never saturate; no cross products except between disconnected components or certified-small inputs; prefer
   robust operators (hash) when upper/point > 100 (Leis et al. VLDB'15).
5. **Regression-safe learning** — publish feedback only from committed snapshots; collision-free 128-bit canonical
   keys over the whole tree; log-space robust estimators with invalid samples as no-ops; keep a baseline plan and
   switch only when the gain exceeds learned uncertainty (Bao, Lero, Eraser); unknown profile values fail closed.
6. **Fail-safe planning** — catch `RuntimeException`/unwrap `CompletionException` and fall back; waiters replan;
   pre-scan unsupported operators; non-latching budgets; relative deadlines; deterministic tie-breaks on a canonical
   plan digest.
7. **Bounded, spilling physical operators** — memory budgets and spill/switch for hash join, EXISTS partitions
   (DuckDB/Umbra style); lazy input opening with suppressed-exception cleanup.
8. **Differential testing** — NoREC/TLP-style (SQLancer) generators comparing optimized vs unoptimized evaluation
   across OPTIONAL/MINUS/EXISTS/VALUES/mixed-literal queries, cold vs warm caches, write→re-query sequences, and a
   mode that forces every rule alternative to be chosen at least once. Every confirmed S1 bug above reproduces with
   2–5 triples.
