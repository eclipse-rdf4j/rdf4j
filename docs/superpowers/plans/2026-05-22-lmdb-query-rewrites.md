# LMDB Query Rewrites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the missing non-SQL, non-WCOJ query rewrite and estimator support identified from the paper index for LMDB, the sketch-based estimator, and the LMDB planner.

**Architecture:** Keep the existing RDF4J optimizer pipeline intact and add narrowly-scoped optimizer/estimator components behind explicit proof and telemetry annotations. Rewrites must be semantics-preserving under SPARQL bag semantics unless the code explicitly proves set/existence scope, matching the existing `LmdbRewriteProof` model. Estimator work should feed the current `LmdbEvaluationStatistics` and `SketchBasedJoinEstimator` APIs instead of introducing a parallel planner.

**Tech Stack:** Java 25, Maven, RDF4J query algebra, LMDB Sail, sketch estimator, Surefire/Failsafe, existing `mvnf` runner.

---

## Scope

Implement these paper-backed gaps:

1. Well-designed `OPTIONAL` / `UNION` normalization subset.
2. Property-path cardinality estimation and planner annotations.
3. Characteristic-set / shape-style RDF cardinality estimates for star joins.
4. Semantic dependency rewrites for declared uniqueness and functional dependencies.
5. Federated multi-query rewrite adapter only if a clear FedX integration point exists.

Explicitly excluded:

1. SQL-only rewrites from OBDA/SPARQL-to-SQL papers.
2. WCOJ/Leapfrog/Triejoin/Ring work, because LMDB does not currently have the physical algorithm or trie-like indexes.
3. Ontology/sameAs/TBox expansion, unless a later index search finds a concrete local paper and a repository integration point.

## Source Evidence

Use the populated index at `papers/.paper-index/index.sqlite`; ignore the empty top-level `.paper-index/index.sqlite`.

Run these searches before coding and paste the relevant snippets into the implementation notes:

```bash
python3 -m papers.paper_index --root . --output papers/.paper-index search 'well designed OPTIONAL normal form UNION' --limit 12 --json
python3 -m papers.paper_index --root . --output papers/.paper-index search 'SPARQL multi query optimization OPTIONAL UNION VALUES' --limit 12 --json
python3 -m papers.paper_index --root . --output papers/.paper-index search 'property path rewrite cardinality estimation SPARQL' --limit 12 --json
python3 -m papers.paper_index --root . --output papers/.paper-index search 'Waveguide SPARQL property path automata' --limit 12 --json
python3 -m papers.paper_index --root . --output papers/.paper-index search 'shape statistics SPARQL cardinality estimation' --limit 12 --json
python3 -m papers.paper_index --root . --output papers/.paper-index search 'characteristic sets cardinality estimation RDF' --limit 12 --json
python3 -m papers.paper_index --root . --output papers/.paper-index search 'semantic query optimization dependency rewrite' --limit 12 --json
```

Known relevant papers from index searches:

- `graph_triple_store_optimizer_papers_supplement/01_sparql_semantics_and_rewrites/perez_arenas_gutierrez_semantics_complexity_sparql.pdf`
- `graph_triple_store_optimizer_papers_supplement/01_sparql_semantics_and_rewrites/atre_left_bit_right_sparql_optional_patterns.pdf`
- `papers/algorithms_and_analysis_for_the_sparql_constructs_atre_2018.pdf`
- `papers/on_the_formulation_of_performant_sparql_queries_loizou_angles_groth_2015.pdf`
- `graph_triple_store_optimizer_papers_supplement/03_cardinality_estimation_and_statistics/yakovets_godfrey_gryz_waveguide_sparql_property_path_queries.pdf`
- `graph_triple_store_optimizer_papers_supplement/03_cardinality_estimation_and_statistics/yakovets-godfrey-gryz-2016-query-planning-sparql-property-paths.pdf`
- `graph_triple_store_optimizer_papers_supplement/03_cardinality_estimation_and_statistics/aimonier_davat_et_al_join_ordering_sparql_property_path_queries.pdf`
- `graph_triple_store_optimizer_papers_supplement/03_cardinality_estimation_and_statistics/neumann_moerkotte_characteristic_sets_rdf_cardinality_estimation.pdf`
- `graph_triple_store_optimizer_papers_supplement/03_cardinality_estimation_and_statistics/rabbani_lissandrini_hose_optimizing_sparql_shape_statistics.pdf`
- `papers/05_semantic_guarantees_rewrites/20-lindner-et-al-2026-unleashing-data-dependency-based-query-optimization.pdf`

Implementation snippets captured on 2026-05-23 before Task 1 coding:

- `well designed OPTIONAL normal form UNION`: `Semantics and Complexity of SPARQL` reports that every
  well-designed graph pattern has an equivalent normal form; `Left Bit Right` reports rewriting
  well-designed BGP-OPT-UNION queries into UNION normal form.
- `SPARQL multi query optimization OPTIONAL UNION VALUES`: `Optimizing Multi Query Evaluation Federated Rdf
  Systems` reports rewrite strategies using UNION, OPTIONAL, and VALUES for federated RDF MQO.
- `property path rewrite cardinality estimation SPARQL`: `Join Ordering of SPARQL Property Path Queries`
  reports a cardinality estimator for property path queries, including random-walk based estimation.
- `Waveguide SPARQL property path automata`: `WAVEGUIDE` and `Towards Query Optimization for SPARQL
  Property Paths` report property-path plans built from automata-style path evaluation.
- `shape statistics SPARQL cardinality estimation`: `Optimizing SPARQL Queries Using Shape Statistics`
  reports SHACL-derived statistics for more accurate join cardinality estimation and q-error evaluation.
- `characteristic sets cardinality estimation RDF`: Neumann/Moerkotte report characteristic sets for
  accurate RDF star-join cardinality estimation.
- `semantic query optimization dependency rewrite`: Lindner et al. 2026 and related dependency papers report
  dependency-based semantic query optimization and rewrite use cases.

## Existing Code Map

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`
  - Adds LMDB-specific optimizers after standard RDF4J normalizers.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewriteProof.java`
  - Extend this with every new rewrite kind.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizer.java`
  - Existing home for OPTIONAL/MINUS/filter-anchor rewrites and join planning.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbTupleExprEstimateAnnotator.java`
  - Currently stamps `ZeroLengthPath` and `ArbitraryLengthPath` with generic statistics.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
  - Bridge from algebra nodes to sketch/planner estimates.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`
  - Add reusable estimator inputs here only when they are not LMDB-specific.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchJoinOrderPlanner.java`
  - Existing planner selection and join-order scoring.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryModelNormalizerOptimizer.java`
  - Existing generic `Join`/`Union` and some well-designed `LeftJoin` normalization.

## Global Rules

- Follow Routine A for behavior-changing work: add the smallest failing test first, capture failure, then patch production.
- Never run tests with `-am` or `-q`.
- Always use `-Dmaven.repo.local=.m2_repo`.
- Before tests, install with:

```bash
mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
```

- Prefer targeted tests with:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs --stream
```

- Add `// Some portions generated by Codex` below the license header for every new Java source file.
- Preserve unrelated dirty files.

---

### Task 1: Add Rewrite Kinds And Test Harness

**Files:**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewriteProof.java`
- Create: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewritePlanTestSupport.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewriteProofKindsTest.java`

- [x] **Step 1: Write the failing enum coverage test**

Create `LmdbRewriteProofKindsTest` with assertions that these new enum values exist:

```java
@Test
void exposesRewriteKindsForPaperBackedRewrites() {
	assertThat(LmdbRewriteProof.RewriteKind.valueOf("OPTIONAL_WELL_DESIGNED_NORMALIZATION")).isNotNull();
	assertThat(LmdbRewriteProof.RewriteKind.valueOf("OPTIONAL_UNION_NORMAL_FORM_BRANCH")).isNotNull();
	assertThat(LmdbRewriteProof.RewriteKind.valueOf("PROPERTY_PATH_COST_ANNOTATION")).isNotNull();
	assertThat(LmdbRewriteProof.RewriteKind.valueOf("CHARACTERISTIC_SET_STAR_ESTIMATE")).isNotNull();
	assertThat(LmdbRewriteProof.RewriteKind.valueOf("DEPENDENCY_FUNCTIONAL_JOIN_ELIMINATION")).isNotNull();
}
```

- [x] **Step 2: Run the failing test**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRewriteProofKindsTest#exposesRewriteKindsForPaperBackedRewrites --retain-logs --stream
```

Expected: FAIL with `No enum constant`.

- [x] **Step 3: Add enum values**

Add to `LmdbRewriteProof.RewriteKind`:

```java
OPTIONAL_WELL_DESIGNED_NORMALIZATION,
OPTIONAL_UNION_NORMAL_FORM_BRANCH,
PROPERTY_PATH_COST_ANNOTATION,
CHARACTERISTIC_SET_STAR_ESTIMATE,
DEPENDENCY_FUNCTIONAL_JOIN_ELIMINATION,
FEDERATED_MULTI_QUERY_PACKING
```

- [x] **Step 4: Add test support**

Create `LmdbRewritePlanTestSupport` with helpers:

```java
static String optimizedPlan(String sparql) {
	try (SailRepository repo = new SailRepository(new LmdbStore(Files.createTempDirectory("lmdb-rewrite-test").toFile()))) {
		repo.init();
		try (SailRepositoryConnection conn = repo.getConnection()) {
			TupleQuery query = conn.prepareTupleQuery(sparql);
			return query.explain(Explanation.Level.Timed).toString();
		}
	}
}

static void assertPlanContains(String plan, String expected) {
	assertThat(plan).contains(expected);
}
```

Adjust imports to existing test patterns if `explain` API usage differs in this module.

- [x] **Step 5: Re-run proof test**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRewriteProofKindsTest#exposesRewriteKindsForPaperBackedRewrites --retain-logs --stream
```

Expected: PASS.

Actual Task 1 evidence:

- Red: `LmdbRewriteProofKindsTest#exposesRewriteKindsForPaperBackedRewrites` failed with
  `No enum constant org.eclipse.rdf4j.sail.lmdb.LmdbRewriteProof.RewriteKind.OPTIONAL_WELL_DESIGNED_NORMALIZATION`.
- Green: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb
  -Dtest=LmdbRewriteProofTest,LmdbRewriteProofKindsTest test` passed with 4 tests, 0 failures.
- Note: `mvnf ... verify` produced the expected Surefire green, then continued into unrelated LMDB failsafe
  work. Use the unit `test` goal for focused proof-kind checks unless the task explicitly needs failsafe.

---

### Task 2: Implement Safe OPTIONAL Normalization Subset

**Files:**
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOptionalNormalFormOptimizer.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOptionalNormalFormOptimizerTest.java`

Implement only these safe shapes:

1. Convert adjacent independent well-designed optionals into a stable nested order:
   `(A OPTIONAL B) OPTIONAL C` where `B` and `C` introduce disjoint variables and both only depend on `A`.
2. Distribute a left-side union through optional only when the existing proof logic can certify both branches:
   `(A UNION B) OPTIONAL C -> (A OPTIONAL C) UNION (B OPTIONAL C)`.
3. Do not implement nullification/best-match globally in this task.

- [x] **Step 1: Write failing test for stable independent optional ordering**

Use a query where two `OPTIONAL` branches are syntactically reversed but semantically independent. Assert the plan contains `OPTIONAL_WELL_DESIGNED_NORMALIZATION`.

- [x] **Step 2: Run the failing test**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptionalNormalFormOptimizerTest#normalizesIndependentWellDesignedOptionals --retain-logs --stream
```

Expected: FAIL because the proof marker is absent.

- [x] **Step 3: Implement optimizer visitor**

Create `LmdbOptionalNormalFormOptimizer`:

- Visit `LeftJoin`.
- Compute assured variables from the left arg.
- Compute introduced variables from each right arg.
- Only reorder when right branches are pure tuple expressions, have no `SERVICE`, no `BINDINGS` side effects, no `Extension` expression dependency on the sibling, and no condition.
- Sort independent right branches by structural fingerprint, then estimated cardinality if available.
- Annotate rewritten node with `optimizer.rewriteProof`.

- [x] **Step 4: Wire optimizer into pipeline**

Insert after `QUERY_MODEL_NORMALIZER` and before `PROJECTION_REMOVAL_OPTIMIZER`:

```java
optimizers.add(new LmdbOptionalNormalFormOptimizer(evaluationStatistics));
```

- [x] **Step 5: Re-run focused optional tests**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptionalNormalFormOptimizerTest --retain-logs --stream
```

Expected: PASS.

- [x] **Step 6: Add negative tests**

Add tests proving no rewrite when:

- right branches share newly introduced variables,
- `LeftJoin` has a condition,
- branch contains `Service`,
- branch contains `Extension` using sibling variables.

Actual Task 2 evidence:

- Red: `LmdbOptionalNormalFormOptimizerTest#normalizesIndependentWellDesignedOptionals` failed with
  `ClassNotFoundException: org.eclipse.rdf4j.sail.lmdb.LmdbOptionalNormalFormOptimizer`.
- Green: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb
  -Dtest=LmdbOptionalNormalFormOptimizerTest test` passed with 5 tests, 0 failures.

---

### Task 3: Add Property Path Estimate Provider

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/PropertyPathEstimate.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/PropertyPathEstimateProvider.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbTupleExprEstimateAnnotator.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbPropertyPathEstimateTest.java`

Start with estimator annotations only. Do not implement Waveguide execution.

- [x] **Step 1: Write failing test for non-generic path estimate**

Create an LMDB repo with a small chain:

```sparql
SELECT ?o WHERE { <urn:a> <urn:p>+ ?o }
```

Assert the explain plan does not report generic path statistics and contains `PROPERTY_PATH_COST_ANNOTATION`.

- [x] **Step 2: Run the failing test**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPropertyPathEstimateTest#annotatesArbitraryLengthPathWithPropertyPathEstimate --retain-logs --stream
```

Expected: FAIL because paths are currently stamped as generic statistics.

- [x] **Step 3: Add estimate record**

`PropertyPathEstimate` fields:

```java
double rows;
double distinctSubjects;
double distinctObjects;
double averagePathFanout;
String method;
```

Use finite non-negative validation in the constructor.

- [x] **Step 4: Add provider interface**

`PropertyPathEstimateProvider` methods:

```java
Optional<PropertyPathEstimate> estimate(ArbitraryLengthPath path, Set<String> boundVars);
Optional<PropertyPathEstimate> estimate(ZeroLengthPath path, Set<String> boundVars);
```

- [x] **Step 5: Implement sketch fallback**

In `SketchBasedJoinEstimator`, add a provider using existing predicate/component sketches for single-predicate paths:

- `p+`: estimate by bounded fanout growth capped by store size.
- `p*`: estimate as `p+` plus zero-length identity rows.
- non-simple path expressions: return `Optional.empty()`.

- [x] **Step 6: Use provider in LMDB annotator**

In `LmdbTupleExprEstimateAnnotator.meet(ArbitraryLengthPath)` and `.meet(ZeroLengthPath)`, ask `LmdbEvaluationStatistics` for a path estimate. If present:

- stamp rows,
- stamp distinct subject/object metrics,
- set `optimizer.rewriteProof` to `PROPERTY_PATH_COST_ANNOTATION`,
- keep generic fallback when absent.

- [x] **Step 7: Re-run path tests**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPropertyPathEstimateTest --retain-logs --stream
```

Expected: PASS.

Actual Task 3 evidence:

- Red: `LmdbPropertyPathEstimateTest#annotatesArbitraryLengthPathWithPropertyPathEstimate` failed with
  `AssertionFailedError` because the `ArbitraryLengthPath` had no `PROPERTY_PATH_COST_ANNOTATION` marker.
- Green: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb
  -Dtest=LmdbPropertyPathEstimateTest test` passed with 1 test, 0 failures.

---

### Task 4: Add Characteristic Set Star Estimates

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/CharacteristicSetEstimate.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCharacteristicSetEstimateTest.java`

Implement star-join CE only. Do not build full SHACL support in this task.

- [x] **Step 1: Write failing star-join CE test**

Load data where `?s :p1 ?a ; :p2 ?b ; :p3 ?c` has strong correlation. Assert the planned cardinality is closer to the true star count than independent product fallback and plan contains `CHARACTERISTIC_SET_STAR_ESTIMATE`.

- [x] **Step 2: Run failing test**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCharacteristicSetEstimateTest#usesCharacteristicSetForCorrelatedSubjectStar --retain-logs --stream
```

Expected: FAIL because no characteristic-set estimate exists.

- [x] **Step 3: Add characteristic-set summary**

Represent each subject star by sorted predicate IDs. Store counts in the sketch estimator refresh cycle:

```java
record CharacteristicSetEstimate(Set<Integer> predicates, long subjectCount, double rows) {}
```

Cap memory with a configurable top-N plus overflow bucket. Default top-N should be conservative, e.g. `4096`.

- [x] **Step 4: Detect subject stars**

In join planning, detect connected `StatementPattern`s sharing the same subject variable and constant predicates.

- [x] **Step 5: Feed star estimate into planner**

When a star group exists, use characteristic-set rows before independent multiplication. Annotate:

```text
optimizer.rewriteProof=CHARACTERISTIC_SET_STAR_ESTIMATE
plannedEstimateSource=lmdb-characteristic-set
```

- [x] **Step 6: Re-run characteristic-set tests**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCharacteristicSetEstimateTest --retain-logs --stream
```

Expected: PASS.

Actual Task 4 evidence:

- Red: `LmdbCharacteristicSetEstimateTest#usesCharacteristicSetForCorrelatedSubjectStar` failed with
  `AssertionFailedError: expected: not <null>` because no join carried `CHARACTERISTIC_SET_STAR_ESTIMATE`.
- Green: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb
  -Dtest=LmdbCharacteristicSetEstimateTest test` passed with 1 test, 0 failures.

---

### Task 5: Add Declared Dependency Rewrites

**Files:**
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSemanticDependencyOptimizer.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSemanticDependencies.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSemanticDependencyOptimizerTest.java`

Only implement explicitly declared dependencies. Do not infer dependencies from data yet.

- [x] **Step 1: Write failing unique-property join elimination test**

Use a declared dependency:

```text
property urn:ssn is functional by subject
```

Query:

```sparql
SELECT ?s ?x WHERE {
  ?s <urn:ssn> ?x .
  ?s <urn:ssn> ?y .
  FILTER(?x = ?y)
}
```

Expected rewrite: eliminate the duplicate pattern or convert the equality into a no-op because functionality proves `?x == ?y`.

- [x] **Step 2: Run failing dependency test**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSemanticDependencyOptimizerTest#eliminatesDuplicateFunctionalPropertyJoin --retain-logs --stream
```

Expected: FAIL because no dependency optimizer exists.

- [x] **Step 3: Add dependency model**

`LmdbSemanticDependencies` should support:

```java
boolean isFunctionalBySubject(Value predicate);
boolean isInverseFunctionalByObject(Value predicate);
```

Start with test-only injection and no public config. Add config only after behavior is proven.

- [x] **Step 4: Add optimizer**

`LmdbSemanticDependencyOptimizer` visits `Join` groups and detects duplicate constant-predicate statement patterns:

- same subject var, same functional predicate,
- same object var or equality filter proving object compatibility,
- no context mismatch,
- no graph scope uncertainty.

Replacement:

- keep one pattern,
- keep all projected variable bindings by adding `Extension` only if needed,
- annotate proof with `DEPENDENCY_FUNCTIONAL_JOIN_ELIMINATION`.

- [x] **Step 5: Wire optimizer behind empty/default dependency provider**

Pipeline should add optimizer only when provider is non-empty, to avoid overhead and accidental behavior changes.

- [x] **Step 6: Add negative tests**

No rewrite when:

- predicate is not declared functional,
- subjects differ,
- contexts differ,
- object equality is absent and both object vars are projected independently.

Actual Task 5 evidence:

- Red: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb
  -Dtest=LmdbSemanticDependencyOptimizerTest#eliminatesDuplicateFunctionalPropertyJoin test` failed with
  `ClassNotFoundException: org.eclipse.rdf4j.sail.lmdb.LmdbSemanticDependencyOptimizer`.
- Green: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb
  -Dtest=LmdbSemanticDependencyOptimizerTest,LmdbOptimizerPipelineTest#lmdbPipelineAddsSemanticDependencyOptimizerOnlyWhenConfigured
  test` passed with 7 tests, 0 failures.

---

### Task 6: Decide Federated MQO Integration

**Files:**
- Inspect: `tools/federation`, `core/queryalgebra/evaluation`, and any `fedx` module present in this branch.
- Create only if integration point exists: `core/.../FederatedMultiQueryRewriteOptimizer.java`
- Test only if integration point exists: `.../FederatedMultiQueryRewriteOptimizerTest.java`

This is a decision task. Do not force MQO into LMDB if the only available surface is a single local query tree.

- [x] **Step 1: Search for FedX/federated integration**

```bash
rg -n "FedX|SERVICE|federated|Federation|SourceSelection|NUnion|VALUES" core tools
```

- [x] **Step 2: Record decision**

If there is no local batching layer where multiple independent query trees are visible together, write:

```text
Federated MQO deferred: LMDB optimizer sees one query algebra tree at a time; no multi-query batch boundary is available.
```

If there is a batch boundary, continue.

- [x] **Step 3: Write failing packing test**

Given three compatible remote subqueries, expect one packed query using `VALUES` or `UNION` and a result splitter.

- [x] **Step 4: Implement conservative packer**

Only pack queries with:

- same endpoint,
- same projected variable set,
- same basic graph pattern shape,
- finite differing constants that can be represented by `VALUES`.

Annotate with `FEDERATED_MULTI_QUERY_PACKING`.

- [x] **Step 5: Add negative tests**

No packing for different endpoints, incompatible projections, `OPTIONAL` with different variable scope, or non-finite constants.

Actual Task 6 decision:

Federated MQO deferred: LMDB optimizer sees one query algebra tree at a time, and the user explicitly instructed
to ignore FedX for this sweep. No federated packer or tests were added.

---

### Task 7: Module Verification

**Files:**
- No new files unless failures reveal missing tests.

- [ ] **Step 1: Run copyright check**

```bash
cd scripts && ./checkCopyrightPresent.sh
```

Expected: no missing headers.

- [ ] **Step 2: Run formatter**

```bash
mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
```

Expected: success.

- [ ] **Step 3: Install quick profile**

```bash
mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
```

Expected: success.

- [ ] **Step 4: Run LMDB module tests**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream
```

Expected: success.

- [ ] **Step 5: Persist initial/final evidence**

If green:

```bash
tail -200 "$(ls -t logs/mvnf/*-verify.log | head -1)" > initial-evidence.txt
```

If failing:

```bash
find . -type f \( -path "*/target/surefire-reports/*.txt" -o -path "*/target/failsafe-reports/*.txt" \) -print0 | xargs -0 cat > initial-evidence.txt
```

---

## Suggested Commit Slices

1. `feat: add lmdb rewrite proof kinds`
2. `feat: normalize safe lmdb optional patterns`
3. `feat: estimate lmdb property paths`
4. `feat: add characteristic-set star estimates`
5. `feat: add declared dependency rewrites`
6. `docs: record federated mqo decision`

## Final Handoff Checklist

- SQL-only rewrites remain excluded.
- WCOJ remains excluded.
- Every behavior-changing task has a failing pre-fix test snippet.
- Every new rewrite has a proof kind and an explain-plan marker.
- Negative tests cover unsafe SPARQL bag-semantics cases.
- `core/sail/lmdb` tests pass or failures are documented with report paths.
