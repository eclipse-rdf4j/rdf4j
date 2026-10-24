# Differential compliance run: develop vs GH-0000-lmdb-predicate-guarantees (design)

Status: design only (review item 3.11.6, WP9 batch b3). No production code is touched by this plan; every step is
Routine C (measurement) until a divergence is found, at which point the divergence becomes a Routine A red test for
the owning work package.

## Why the current evidence is not a differential run

Review section 1 compares the merge against the *pre-merge branch* (54 failing compliance ids on both), and this
WP's batch-3 run compares the branch *with the WP9 fixes* against that same 54-id set (see
`initial-evidence.wp9.txt`, heading `## b3 3.11.6`). Neither run answers the question the review asks in 3.11.6:
"does the branch's `MaterializedExistsFilterIteration`, `LeftJoinQueryEvaluationStep` (+624 lines),
`HashJoinIteration` (+535) and path evaluation-step rewrites change any compliance outcome relative to
`origin/develop` (5eee576f5a)?" A pre-existing failure on the branch may be green on develop (a branch regression that
predates the merge), and a test that is green on both may still take a different operator path on the branch only
under a specific executor mode. The three layers below close those gaps in increasing depth.

## Layer 1: baseline id parity (develop vs branch, per store)

Inputs: two detached worktrees of the same repository, `origin/develop` (5eee576f5a) and the branch tip to be
compared (the integrated WP fixes, not bare 4354ff137b), each with its own named Maven workspace so they can run at
the same time on one machine without sharing build trees:

```
git worktree add <scratch>/wt-develop 5eee576f5a
git worktree add <scratch>/wt-branch  <integrated-tip>
(in each)  python3 .codex/skills/mvnf/scripts/mvnf.py --workspace <dev|br> --threads 3 compliance/sparql --retain-logs
```

`compliance/sparql` runs everything through Failsafe (`compliance/pom.xml` includes `**/*Test.java` and `**/*IT.java`
in failsafe and skips surefire), so the reports are the
`.mvnf/workspaces/<ws>/build/org.eclipse.rdf4j/rdf4j-sparql-compliance/<version>/failsafe-reports/TEST-*.xml` files.

Id extraction: one `testcase` element per executed test with `classname` and `name`. For the manifest-driven
suites the default Failsafe XML records only the dynamic-test index (`builtinFunction()[20]`, `lateral()[2]`), not
the JUnit display name that carries the manifest IRI (checked on the b2 report
`TEST-org.eclipse.rdf4j.sail.lmdb.LmdbSPARQLComplianceTest.xml`: zero occurrences of `SES869`). Indices are stable
only while both sides run the identical manifest set and the identical `getIgnoredTests()` list; develop and the
branch tip satisfy that today because the branch merged develop's `testsuites/sparql`, but the diff script must
verify `count(ids(dev)) == count(ids(br))` per method before trusting index equality. To get IRIs into the report,
enable Surefire/Failsafe's phrased reporter in the two parity worktrees only
(`statelessTestsetReporter` with `usePhrasedTestCaseMethodName=true` in `compliance/pom.xml`; a throw-away edit that
is not merged), or dump the `index -> displayName` table once with a 20-line test-scope helper that walks
`SPARQLQueryComplianceTest.getTestData()`. Classify each id as PASS / FAIL(assertion) / ERROR(exception type + top
frame) / SKIPPED.

Diff, per store class (Memory, Native, LMDB, ExtensibleStore, FedX):

| set | meaning | action |
|-----|---------|--------|
| `fail(dev) \ fail(br)` | fixed on the branch | record; sanity-check that the fix is intentional (a test can also turn green because the branch stopped executing the interesting operator) |
| `fail(br) \ fail(dev)` | branch regression | one minimal red test per id, handed to the owning WP (3.1/3.2/3.3/3.4/3.11 mapping in REVIEW.md) |
| `fail(dev) ∩ fail(br)` | pre-existing | list with root-cause class; these are the only ids that may stay red |
| `ids(dev) Δ ids(br)` | tests present on one side only | explain (new manifest entries from the merge, or a suite that aborted early) |

LMDB precondition: three plan-cache NPE families (`LmdbPipelinePlanCache$DatasetIdentity.immutable`, `$2.meet` for
`VALUES … UNDEF`, `PatternKeys.predicateKey`) mask 41 LMDB ids on the review's branch. Run the LMDB leg only after
the plan-cache WP has landed, or exclude those ids and say so; otherwise the LMDB parity diff mostly reports the
NPEs. State at the time of writing (batch-3 run of `compliance/sparql` in the WP9 worktree with the b1-b3 fixes,
evidence heading `## b3 3.11.6`): 2648 tests, 49 failing = 32 `DatasetIdentity.immutable` NPEs
(`LmdbPARQL11UpdateComplianceTest`), 8 `$2.meet` NPEs (`LmdbSPARQLComplianceTest` values/optional/lateral/aggregate),
1 `PatternKeys.predicateKey` NPE (`propertyPath()[8]`), `lateral()[2]` on Memory/Native/LMDB and SPARQL 1.2
`tests()[6]` (LATERAL + OPTIONAL) on Memory/Native/LMDB/FedX/plain `SPARQL12QueryTest`. Compared with the review's
54: the five that turned green are `builtinFunction()[20]` (b2 fix) and the four `CustomAggregateFunctionEvaluationTest`
`*WithDistinct` methods (b3, test expectation corrected); no new red. The eight LATERAL ids are the first candidates
for Layer 1 (are they red on develop too?) and Layer 2 (do they flip under a forced mode?).

Reading the code already gives a strong prior for the Layer 1 answer on the LATERAL ids. On develop,
`LeftJoinQueryEvaluationStep.supply` returns `HashJoinIteration(left, right, bs, …)` for every OPTIONAL whose right
operand contains a sub-select. The right operand is therefore evaluated with the LATERAL-substituted `?s`. On the
branch, commit 7f351d65c1 wraps that path in `standard.whenWellDesigned(optimized)`. The LeftJoin that `LATERAL {
OPTIONAL { SELECT * { ?s rdfs:label ?label } LIMIT 1 } }` produces has a `SingletonSet` left argument, so `?s` counts
as an optional variable, and a substituted `?s` sends the row to `BadlyDesignedLeftJoinIterator`. That iterator strips
`?s` from the input, evaluates the `LIMIT 1` sub-select without correlation (one row, `subject1`) and then drops the
rows that are incompatible with the input. The test expects 3 rows and gets 1. The wp4 design traced the failure to the
same place, and wp4 batch b1 finding `3.9-lateral` excludes LATERAL-substituted names from the problem variables.
The 41 LMDB NPEs are wp3 batch b1 findings 3.9.1, 3.9.2 and 3.9.3. So the integrated tree should show zero failing
compliance ids. Any id that is still red after integration is the real input to Layer 1.

Cost: one compliance module run per side, about 10-15 minutes each on this machine when nothing else is running;
they can run concurrently in the two workspaces.

## Layer 2: operator-mode sweep on the branch (roadmap 4.8, "every alternative chosen at least once")

Motivation: the b1 finding 3.11.4 (renameVar of an outer-visible EXISTS variable) was wrong only under the streaming
and memoized EXISTS probes and correct under the materialized probe; the default ADAPTIVE mode picks per query, so a
single compliance run exercises one alternative per test. Force each alternative and rerun the Memory and LMDB
compliance classes under each configuration.

Hooks that already exist in production code (no new flags needed):

* EXISTS strategy: `FilterIterator` reads `filter.getStringMetricPlanned("optimizer.filterAlgorithmHint")`
  (`FilterIterator.java:83-87,228`) with values `streaming-correlated`, `memoized-correlated`, `materialized-hash`.
  A test-only `QueryOptimizer` appended to the pipeline via a wrapping `EvaluationStrategyFactory` in the sweep
  subclass's `newRepository()` stamps the hint on every `Filter` whose condition contains an `Exists`
  (`SameTermFilterOptimizerScopeTest` in `core/queryalgebra/evaluation` already does exactly this per mode and is the
  template).
* Semi/anti-join memo: `-Drdf4j.evaluation.semiAntiMemoCapacity=1` (`FilterIterator.java:91`).
* LeftJoin materialisation/memo fallbacks: `-Dorg.eclipse.rdf4j.query.algebra.evaluation.leftjoin.maxMaterializedRightRows=1`,
  `…leftjoin.memoized.maxKeys=1`, `…leftjoin.memoized.maxRows=1` (`LeftJoinQueryEvaluationStep.java:50-52`, read
  per call through `System.getProperty` at `:268`, so they can be set per JVM without a static-init ordering issue;
  pass them as `-- -DargLine=...` to `mvnf`, one module run per configuration).
* `MaterializedExistsFilterIteration.PROBE_LIMIT = 32` (`:51`) is a compile-time constant with no switch. The sweep
  covers it only indirectly (small compliance datasets never exceed 32 probes); if the owning WP (wp4) wants it swept
  they need a package-private test seam, which is a production edit and therefore out of this design's scope.

Configurations (each is one `mvnf --workspace br compliance/sparql` run with the flags above; the hint stamping is a
separate sweep test class per store in `compliance/sparql/src/test/java/org/eclipse/rdf4j/sail/<store>/`, parameterised
by mode, extending the existing `*SPARQL11QueryComplianceTest` / `*SPARQL12QueryComplianceTest` bases):

1. default (baseline, identical to Layer 1 branch side);
2. hint = `streaming-correlated`;
3. hint = `memoized-correlated`;
4. hint = `materialized-hash`;
5. `semiAntiMemoCapacity=1` with the default hint;
6. the three `leftjoin.*=1` properties together (forces the spill / non-memoized LeftJoin paths).

Any id that fails in one configuration and passes in another is a mode-specific executor bug (file to wp4 with a
2-5 triple repro). Seed queries that must be in the sweep because they are known mode-sensitive:
`?s :p ?x FILTER EXISTS { ?s :q ?o . ?s :r ?x FILTER(sameTerm(?o,?x)) }` and its `sameTerm(?x,?o)` mirror
(critique of 3.11.4), plus the f5 (`OperatorContractReviewReproTest`) and f8 (`StandardPipelineReviewReproTest`)
shapes; the b1 tests `SameTermExistsScopeTest` / `LmdbSameTermExistsScopeTest` already pin the first two under all
four hints and are the model for adding further seeds.

Cost: 6 module runs, about an hour sequentially; the classes are independent so they can be split over workspaces.

## Layer 3: seeded oracle differential (NoREC/TLP style)

A generator test in `core/queryalgebra/evaluation` (fixed seeds, modelled on `SPARQLMinusIterationFuzzTest`) that
builds random 2-5-triple datasets with mixed literal forms (`"1"^^xsd:int`, `"01"^^xsd:int`, `1.0`, `"a"@en`,
`"a"@EN`, `VALUES … UNDEF`) and random shapes over OPTIONAL (with and without condition, over a sub-SELECT with
LIMIT or an aggregate), MINUS (inside OPTIONAL, with a right-only filter), EXISTS / NOT EXISTS (over a sub-SELECT, a
UNION body, inside a joined group; sameTerm with an outer-correlated variable), VALUES with UNDEF, property paths
`:p*`, `:p+`, `:p?` and zero-length paths with constants on cyclic data, and LATERAL. The reference is
`StrictEvaluationStrategy` over the *unoptimized* algebra (the pattern used by the b1 soundness tests
`FilterPlacementSoundnessTest`, `QueryModelNormalizerScopedBindPushTest`, `SameTermFilterOptimizerScopeTest`);
the systems under test are (i) the standard pipeline on `MemoryStore` and (ii) `LmdbStore` (same generator, lmdb
module). Bags are compared, not sets. Optional third oracle: Jena ARQ through the existing
`ThemeQueryCatalogJenaOracleTest` harness in `testsuites/benchmark-common`.

Every divergence is shrunk to a minimal dataset + query and lands as a permanent red test in the owning module
(never as a fuzz-only failure), following the b1/b2 pattern of one algebra-level test plus one MemoryStore and one
LMDB end-to-end test.

## Deliverables

1. Per store: the four Layer-1 id sets with root-cause class per pre-existing id.
2. Per configuration: the Layer-2 failing-id matrix; each mode-only failure with its 2-5 triple repro.
3. Layer-3 seed list and the shrunk red tests handed to owners.

## Non-goals

No production change, no new flags, no changes to the compliance manifests. Tests that assert an
implementation-defined order without ORDER BY (the 3.9 custom-aggregate family) are corrected in the test, not in the
engine, and are therefore not differential signals.
