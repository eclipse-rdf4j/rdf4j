# Differential run: develop vs GH-0000-lmdb-predicate-guarantees (review 3.11.6 / WP9)

Executed 2026-09-29 from `.agent/research/GH-0000-wp9-differential-compliance-run-design.md`.

* develop side: `origin/develop` = `bbb4c01258` (differs from the merge base `5eee576f5a` only in
  `.github/workflows/copilot-setup-steps.yml`).
* branch side: `HEAD` `ad6a5964fa` + the uncommitted working tree (`results/br-uncommitted.patch`, md5
  `a2b047158e71ed4feae3d68f87df5269`) + the 16 untracked source files listed in `results/br-untracked.txt`.
* Each side in its own git worktree (scratchpad `wt-dev`, `wt-br`) and its own named Maven workspace (`diffdev`,
  `diffbr`, `diffsweep`; red tests in the main checkout under `diffred`).

## Headline

* **Layer 1 (compliance/sparql, all stores): no behavioural difference.** Both sides 2648 tests, 2645 pass, 3 skipped,
  0 failures. All 1817 manifest-driven result bags are identical after bnode/path normalisation. The only difference is
  `UnionTest#EmptyUnion` (develop, expects 0 rows) vs `#EmptyOptionalSubselect` (branch, expects 1 row `"HIDDEN"`):
  develop's test encodes a develop bug (D1 below).
* **Layer 2 (forced EXISTS algorithm, memo capacity 1, LeftJoin fallbacks): no mode-specific difference.** 501 Memory
  compliance tests × {none, streaming-correlated, memoized-correlated, materialized-hash} and 2013 Memory/Native/LMDB
  tests × {semiAntiMemoCapacity=1, leftjoin.*=1} pass with byte-identical result dumps; the 4 800-query corpus gives
  identical results on Memory under all five configurations. Hit proof: the stamped hint shows on the executed plan
  (`Filter (…, optimizer.filterAlgorithmHint=materialized-hash)`), and `FilterIterator:341-372` dispatches on it.
* **Layer 3 (seeded corpus, 400 datasets × 12 queries, 3 stores, 7 phases): 8 branch bug families** (2 affect every
  store, 6 LMDB only), 17 develop bug families that the branch fixes, 1 spec-permitted difference, 6 shared
  (non-differential) defects. Red tests filed for every branch bug (5 classes, 17 tests, all red).

## Environment notes

* The host started with 6 free POSIX semaphores (`kern.posix.sem.max` 10000). A branch `LmdbStore` opens three LMDB
  environments eagerly (values, triples, `PersistentSetFactory`) = 6 semaphores, so the first branch compliance run lost
  all 504 LMDB tests to `ENOSPC` at `TripleStore` env open. A tool to reclaim leaked LMDB semaphores by name was refused by
  the permission classifier and not pursued. The machine then rebooted (session interrupted), which freed them; all LMDB
  legs were re-run afterwards, strictly one LMDB suite at a time.
* The first develop quick-install (before `scripts/dev_pom_no_obr.py` was applied to `wt-dev/pom.xml`) let
  `maven-bundle-plugin` rewrite the OBR index `.m2_repo/repository.xml` in the shared repository (no jars were touched).
* Throw-away, identical-on-both-sides instrumentation: `DiffDump` + hooks in the testsuite manifest harness and the
  phrased Failsafe reporter (`scripts/instrument.py`); branch-only Layer 2 seam `scripts/sweep/*.java`. None of it is in
  the main checkout.

## Layer 1 — compliance id and result parity

| store class | develop | branch | result dumps |
|---|---|---|---|
| Memory / Native / LMDB / ExtensibleStore / FedX / W3C / SPARQL 1.2 / HTTP / service / custom-function | 2645 pass, 3 skip | 2645 pass, 3 skip | 1817 identical, 0 differ |

Ids: identical except the renamed `union()` test (3 stores). Evidence: `initial-evidence.diffbr.txt`,
`initial-evidence.diffdev.txt`, reports in `results/reports-l1/{br,dev}`, dumps in `results/dumps/{br,dev}-l1`, diff in
`results/l1-dumpdiff.txt`.

## Layer 2 — operator-mode sweep (branch)

| configuration | compliance (Memory/Native/LMDB) | corpus (Memory, 29 200 executions) |
|---|---|---|
| default | 501 / 2013 pass | reference |
| hint = streaming-correlated | 501 pass, dumps identical | 0 differences |
| hint = memoized-correlated | 501 pass, dumps identical | 0 differences |
| hint = materialized-hash | 501 pass, dumps identical | 0 differences |
| `rdf4j.evaluation.semiAntiMemoCapacity=1` | 2013 pass, 1005 dumps identical | 0 differences |
| `leftjoin.maxMaterializedRightRows/memoized.maxKeys/maxRows=1` | 2013 pass, 1005 dumps identical | 0 differences |

The hint stamping cannot reach LMDB without a production seam (`LmdbEvaluationStrategyFactory` is package-private and
final), so LMDB was swept only with the two property configurations. B1 below reproduces identically in every EXISTS
mode.

## Layer 3 — seeded differential corpus

Generator `scripts/gen.py` (seed 20260929): datasets of 2–5 triples (optional named graph `:g`) over `"1"^^xsd:integer`,
`"01"^^xsd:integer`, `"1"^^xsd:int`, `"01"^^xsd:int`, `1.0`, `1.0e0`, `"a"`, `"a"@en`, `"a"@EN`, dates, booleans; queries
mix OPTIONAL (with conditions), MINUS, FILTER (NOT) EXISTS (also inside `||` and `BIND`), VALUES with UNDEF, sub-SELECTs
(DISTINCT, aggregates, ORDER BY + LIMIT), UNION, GRAPH, LATERAL and property paths; each case has one of 12 seed queries
(design seeds + review shapes) and a DELETE DATA + INSERT DATA write. Runner `scripts/DiffRunner.java` (same source for
both sides, compiled against each side's recorded Failsafe classpath) runs per store:

* `cold` fresh store per query; `seq1`, `seq2` all queries twice on one store (warm plan/learned state);
  `write1`, `write2` after the write on that warm store; `reopen` after shutdown + reopen (Native, LMDB);
  `postcold` fresh store, load, write, query.
* Oracles: develop, Jena ARQ 6.1.0 (`scripts/JenaRunner.java`, RDF4J union-default-graph emulation) and spec
  reasoning. Jena is **not** reliable for FILTER + `VALUES … UNDEF` in one group (it returns 1 row for
  `{ ?o :r :a . FILTER(?o = :a) VALUES (?o ?y) { (:a "01"^^xsd:integer) (UNDEF UNDEF) } }`, spec: 2) nor for `!=`
  between incomparable literals (it treats them as unequal); those verdicts were decided by hand.
* Every divergence was shrunk (`scripts/shrink.py`, delta debugging over query parts and data triples; sequence
  shrinker for L4).

| store | executions compared | differing | distinct queries | abandoned |
|---|---|---|---|---|
| Memory | 28 800 | 330 | 60 | 0 |
| Native | 33 600 | 380 | 59 (⊂ Memory's) | 0 |
| LMDB | 33 418 | 495 | 84 | 4 cases (planner blow-up, B4) |

(LMDB rows require an explicit `LmdbStoreConfig("spoc,posc")`; see B8.)

### A. Branch bugs (red tests filed)

Prefix `:` = `http://ex/` in the tests, `http://ex.org/` in the corpus.

**B1 — expression errors inside a correlated EXISTS body escape as exceptions or flip the result (all stores, 8 corpus
queries, every EXISTS mode).**
`FilterIterator.evaluateSubstitutedExists` substitutes the outer solution into the body and precompiles it per probe
(`DefaultEvaluationStrategy.precompileRuntimeSubtree`); `prepare(MathExpr)` then constant-folds e.g. `:b + 1` and the
`ValueExprEvaluationException` leaves precompilation instead of meaning "BIND unbound" / "condition false".
Data `:b :q :a .`

| query | develop = spec | branch |
|---|---|---|
| `SELECT ?o { ?o :q ?z FILTER EXISTS { ?o :q ?y . BIND(?o + 1 AS ?s) } }` | `o=:b` | `ValueExprEvaluationException` |
| `SELECT ?o { ?o :q ?z FILTER EXISTS { BIND(?o + 1 AS ?s) } }` | `o=:b` | no rows |
| `SELECT ?o ?e { ?o :q ?z BIND(EXISTS { ?o :r ?y . BIND(?o + 1 AS ?s) } AS ?e) }` | `e=false` | `?e` unbound |
| data `:b :p :d .` `SELECT * { ?o :p ?x FILTER NOT EXISTS { ?o :r ?o . BIND(?o + 1 AS ?w) } }` | 1 row | exception |
| data `:a :p "a" .` `SELECT * { ?s :p ?z MINUS { ?x :p ?z . BIND(COALESCE(?z,"d") AS ?o) FILTER NOT EXISTS { BIND(?o + 1 AS ?y) ?y :r ?x } } }` | no rows | 1 row |
| OPTIONAL condition `?z + 1 = 2` inside `EXISTS { GRAPH ?g { … } }` | 2 rows | exception |

Latent develop defect underneath (shared, S3): `SELECT * { ?o :q ?z BIND(:b + 1 AS ?s) }` fails with
`QueryEvaluationException` on both sides; the branch's substitution makes ordinary correlated EXISTS queries hit it.
Tests: `core/sail/memory/.../ExistsSubstitutionExpressionErrorTest` (6), `LmdbDifferentialRegressionTest#bindErrorOnCorrelatedVariableInsideExistsLeavesVariableUnbound`.

**B2 — GRAPH variable bound to a literal by a join → `ClassCastException` (Memory c280, LMDB c160).**
Data `:a :p :c . :c :p "a" .` `SELECT * { GRAPH ?g { ?y :p :c . :b :p ?s . } { ?s :p ?g . MINUS { } } UNION { } }`:
develop no rows, branch `MemLiteral cannot be cast to Resource`. LMDB: data `:b :p "a"^^xsd:string .`
`SELECT ?s (COUNT(DISTINCT ?y) AS ?agg) { { } UNION { GRAPH :g { GRAPH ?g { ?z :p ?s } } OPTIONAL { } } ?g ^:p ?z } GROUP BY ?s`
→ develop `agg=0`, branch `LmdbLiteral cannot be cast`. Latent develop defect (S4): develop throws the same exception for
`GRAPH ?g { ?y :p :c . :b :p ?s . } { ?s :p ?g . MINUS { } }` (both sides) and for corpus case c363 (branch fixed that
shape); the branch changes which join orders expose it. Tests: `GraphVariableBoundToLiteralTest`,
`LmdbDifferentialRegressionTest#graphVariableBoundToLiteralDoesNotMatch`.

**B3 — LMDB: `BIND(lang(?s) AS ?z)` hides the VALUES binding of `?z` (c75).**
Data `:a :p :b . :b :p :b . :c :p "01"^^xsd:integer .`
`SELECT * { ?s :p ?o . BIND(lang(?s) AS ?z) VALUES (?z ?o) { (UNDEF :b) ("2020-01-01"^^xsd:date :b) } }`: expected 4
rows, two with `z="2020-01-01"^^xsd:date` (lang(IRI) is an error, `?z` stays unbound, both VALUES rows join); branch LMDB
returns 4 rows with `?z` never bound (2 with DISTINCT). Only when `:p` has a literal object; with IRI objects it is
correct. Test: `LmdbDifferentialRegressionTest#bindOfErroringLangDoesNotHideValuesBindingOfSameVariable`.

**B4 — LMDB: packed planner never terminates (heap exhaustion) on tiny queries (c229, c263, c271, c273).**
`PackedQueryCodec$Builder.deriveFactsAndSaturateRules` → `PackedLogicalRuleProgram.addPayloadTerms`/`addRelationOutputs`
(thread dump `results/hang-br-lmdb.jstack.txt`, reached from `PackedQueryFamilyIdentity.create` in the plan cache).

| data | query | develop LMDB | branch LMDB |
|---|---|---|---|
| `:c :r "01"^^xsd:int .` | `SELECT * { LATERAL { :c :r ?s . } FILTER(?s = "b") }` | 0 rows, ms | OOM (2 GiB heap) |
| `:b :q "2020-01-01"^^xsd:date .` | `SELECT * { LATERAL { ?y :q ?z . ?y :q ?z . FILTER(?z = "b") } }` | 0 rows | OOM |
| `:c :q "true"^^xsd:boolean .` | `SELECT ?z (MAX(?o) AS ?agg) { ?x :r ?y . OPTIONAL { ?o :q ?s . FILTER((?s IN (:d,"b") && ?y = :a)) } } GROUP BY ?z` | 0 rows | OOM |
| `:b :p 1.0 . :b :r "2"^^xsd:integer .` | `SELECT * { GRAPH :g { OPTIONAL { LATERAL { :a :p ?z . } FILTER(?z = "b") } } }` | 1 row | OOM |

Trigger: equality/IN filter on a variable whose predicate has typed-literal objects, inside LATERAL or an OPTIONAL
condition; `:c :r :d` or `:c :r "b"` plan fine. In the 6 GiB corpus runner the query thread spun >13 min.
Test: `LmdbPackedPlanningBlowupTest` (3 tests, each in a 512 MiB child JVM with a 60 s limit).

**B5 — LMDB: zero-or-one path with a `sameTerm` on the subject constant loses the zero-length match (c93).**
Data `:c :p :d .` `SELECT ?z { :c :p? ?z . FILTER(sameTerm(?z, :c)) }` → develop / branch Memory / Jena `z=:c`, branch
LMDB no rows. The constant is substituted into the path, which hits the shared defect S2 (`ASK { :c :p? :c }` is false on
both sides; Jena and the spec: true). Test: `LmdbDifferentialRegressionTest#zeroOrOnePathWithSameTermOnTheSubjectConstantMatchesZeroLength`.

**B6 — LMDB: stale answer after a committed INSERT on a warm store (c388; S1 severity: wrong results after writes).**
Data `:c :p "1"^^xsd:int . :c :r :a .`; run twice A = `SELECT * { ?x :p ?y . FILTER(?y < 2) }`,
B = `SELECT * { FILTER EXISTS { LATERAL { MINUS { ?y :r ?y . ?s :p ?y . } } } }`, T = `SELECT ?o { ?o :p :c }`; then
`INSERT DATA { :b :p :c }`, B, T → branch LMDB T = no rows; develop, fresh/reopened branch store: `o=:b`. Needs both
A and B warm and B after the write (sequence delta-debugged from the 12-query case). The outcome depends on JVM-wide
state: inside `LmdbDifferentialRegressionTest` (after other LMDB tests in the same fork) the sequence came out right,
in isolation it is red. That cross-store dependence is a second finding. Test: `LmdbStaleAnswerAfterInsertTest` (child JVM).

**B7 — LMDB: outer binding leaks into a zero-length path inside an absent named graph (c269).**
Data `:b :p "1.0e0"^^xsd:double .`
`SELECT ?x ?y { ?x :p ?s . GRAPH :g { ?x :q* ?y . } FILTER(?y = ?x || EXISTS { }) }` → develop/Jena no rows, branch
LMDB `x=:b y=:b`; the same query without the filter returns no rows on the branch too (plan-shape dependent semantics).
Test: `LmdbDifferentialRegressionTest#zeroLengthPathInsideAbsentNamedGraphDoesNotMatchTheOuterBinding`.

**B8 — LMDB: a store created with `new LmdbStore(dir)` cannot be reopened (shared with develop; branch fix incomplete).**
Default `LmdbStoreConfig` has `tripleIndexes == null`; on reopen `ValueStore.initTermIndexes` runs
`properties.setTripleIndexes(config.getTripleIndexes())`, overwriting the loaded `spoc,posc` with null before
`TripleStore.getIndexSpecs` validates it (`triple-indexes missing in store.properties file`). Develop fails the same
reopen one step earlier (`triple-term-indexes` never persisted; `StoreProperties` setters assign `dirty = !equals(…)`
instead of or-ing it, so a later no-op setter cancels a pending save — with an explicit index config develop writes no
`store.properties` at all). The branch fixed the dirty flag and the term-index persistence but not the null overwrite.
Found by the harness itself (the corpus runner uses `LmdbStoreConfig("spoc,posc")` to keep the reopen phase).
Test: `LmdbDifferentialRegressionTest#storeCreatedWithDefaultConfigCanBeReopened`.

### B. Develop bugs the branch fixes (branch = spec)

| id | minimal reproducer | develop | branch |
|---|---|---|---|
| D1 | `SELECT * { OPTIONAL { SELECT ?var { :s a :MyType } } }` (empty sub-select) — 34 corpus queries per store; develop's `UnionTest#EmptyUnion` asserts it | no rows | 1 empty row |
| D2 | `:c :p "b" . :c :q "1.0e0"^^xsd:double` `SELECT * { ?s :p ?o OPTIONAL { { SELECT ?s ?x { ?s :q ?x } } FILTER(?x != ?o) } }` — OPTIONAL condition ignored when the right side is a sub-select (also with `FILTER(?x = ?o)`) | x bound | x unbound |
| D3 | `:c :p :b . :c :p :d` `SELECT * { ?z :p ?s . FILTER(COALESCE(?s,:c) = :d) VALUES (?s) { (UNDEF) (:d) } }` — FILTER applied to VALUES rows before the join (also `IN`, `?o = ?o` shapes) | 1 row | 2 rows |
| D4 | `:c :p :d . :c :q :b . :c :r :b` `SELECT * { ?s :p ?x FILTER EXISTS { ?s :q ?o . ?s :r ?x FILTER(sameTerm(?o, ?x)) } }` (review 3.11.4 on develop) | 1 row | no rows |
| D5 | `:a :q :a` `SELECT * { ?x :q ?z . { ?x (:p/:p)* ?y } UNION { VALUES … LATERAL { } } }` | no rows | 1 row |
| D6 | `:a :p :a . :a :q :a . :c :r 2` `SELECT ?y (MIN(?y) AS ?agg) { ?y :r ?z . FILTER EXISTS { ?x :q ?s . OPTIONAL { ?x :p ?z } } } GROUP BY ?y` | no rows | `:c` |
| D7 | `:c :p :b` `SELECT * { BIND(?x + 1 AS ?y) { SELECT ?x ?y { ?y :p ?x } } }` | no rows | 1 row |
| D8 | `:a :p :c . :c :r :b` `SELECT * { ?x :r ?s . { ?y :p :c . FILTER NOT EXISTS { ?s :r ?s } } UNION { ?x :r ?b } }` (outer ?s leaks into the UNION branch) | 1 row | 2 rows |
| D9 | `:b :p :b` `SELECT * { ?x :p ?z . { LATERAL { ?x :p ?x } } UNION { ?z :p ?o } }`; also `{ FILTER EXISTS { LATERAL {…} } } UNION { }` | no rows / 2 | 2 / 4 rows |
| D10 | `:g { :c :r 2 }` `SELECT * { ?o :r 2 . { VALUES (?y ?o) { (:a UNDEF) (2 true) } } UNION { } }` | 1 row | 2 rows |
| D11 | c363 (GRAPH ?g after a join binds ?g to a literal) | ClassCastException | ok |
| D12 | `{ } UNION { ?y :q :a . BIND(EXISTS {…} AS ?b) } GROUP BY ?y`; `{ SELECT ?z { :b (:p/:p)* ?z FILTER(sameTerm(?z,:a)) BIND(…) } }` | IllegalStateException "Node has no parent" | ok |
| D13 | `:b (:p/:p)* ?s . FILTER NOT EXISTS { BIND(?s + 1 AS ?b) :b :r ?b }` (after write `:b :r :c`) | 1 row | no rows |
| D14 (LMDB) | `:c :q :c` `ASK { ?x :q ?s . FILTER NOT EXISTS { VALUES (?s) { (2) } } }` — EXISTS over a VALUES-only body (8 queries) | false | true |
| D15 (LMDB) | `:a :q :a` `SELECT * { MINUS { :b :q ?x . VALUES (?o ?x) { (UNDEF :b) } } ?x :q ?x }` | no rows | 1 row |
| D16 (LMDB) | `SELECT * { VALUES (?y) { (:b) } BIND(?y + 1 AS ?w) }` (S3 on develop LMDB) | QueryEvaluationException | 1 row |
| D17 (LMDB) | c363 q4: after the write, `FILTER NOT EXISTS { ?o :p "a"@EN }` with `:a :p "a"@EN` present | 1 row | no rows |

### C. Spec-permitted differences

* Branch LMDB canonicalises language tags (`language-tag-key=canonical`, 3.1.6): `"a"@EN` is returned as `"a"@en`
  (c202) and `?z :p "a"@EN` matches a stored `"a"@en` (c290). RDF 1.1 Concepts allows lower-casing language tags and
  defines their value space in lower case. Develop LMDB is byte-exact.

### D. Shared defects (identical on both sides, not differential)

* S1 `COUNT(*)` ignores the empty solution: `SELECT (COUNT(*) AS ?c) { { } UNION { ?y :q :a } }` → 0 (spec 1).
* S2 `ASK { :c :p? :c }` → false (spec true; `:p*` is right). Exposed on the branch by B5.
* S3 Prepare-time constant folding throws for erroring constant expressions: `BIND(:b + 1 AS ?s)` →
  `QueryEvaluationException` (spec: `?s` unbound). Root of B1.
* S4 GRAPH variable bound to a literal by a join → ClassCastException (develop shapes). Root of B2.
* S5 `SELECT * { GRAPH ?g { } }` with no named graphs → 1 row (spec: none).
* S6 MemoryStore unifies `"a"@EN` with an existing `"a"@en` after writes (warm vs cold results differ within one
  side) — spec-permitted.

## Red tests (Routine A, evidence in `initial-evidence.diffred.txt`)

| class | tests | failure (branch) |
|---|---|---|
| `core/sail/memory/.../ExistsSubstitutionExpressionErrorTest` | 6 | B1 |
| `core/sail/memory/.../GraphVariableBoundToLiteralTest` | 1 | B2 |
| `core/sail/lmdb/.../LmdbDifferentialRegressionTest` | 6 | B1, B2, B3, B5, B7, B8 |
| `core/sail/lmdb/.../LmdbStaleAnswerAfterInsertTest` | 1 | B6 |
| `core/sail/lmdb/.../LmdbPackedPlanningBlowupTest` | 3 | B4 |

Every expected value was checked against develop (same query, same store type) and, where Jena is reliable, Jena.

## Reproduce

```
python3 scripts/gen.py 20260929 400 > results/l3.corpus
scripts/run_l3.sh <br|dev> <memory|native|lmdb> results/l3.corpus results/l3      # DIFF_JAVA_OPTS=-Ddiff.lmdbIndexes=spoc,posc for lmdb
python3 scripts/l3diff.py results results/l3.corpus memory,native,lmdb
python3 scripts/shrink.py results/l3.corpus <case> <q> <store> <cold|post|warm|write> out.json
python3 scripts/probe.py <store> <mode> '<ttl body>' '<query> ||| <query>'          # develop / branch / Jena side by side
```

Classpaths: `results/{br,dev,jena}.cp` (from the Layer-1 Failsafe reports); compiled runners in `build`, `build2`.
