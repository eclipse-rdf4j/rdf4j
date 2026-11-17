# Add an Independent REAL_ESTATE Solution-Bag Oracle

This ExecPlan is a living implementation guide for the REAL_ESTATE oracle milestone. Maintain `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` as work proceeds, following `.agent/PLANS.md`.

## Purpose / Big Picture

After this work, an LMDB developer can run one hermetic test that checks all 13 REAL_ESTATE catalog queries against independently generated Jena results. The checked-in fixture and expected answers are read from LMDB test resources, so a test never depends on a generated file under `/tmp`, a previously run command, or an optional resource that can be skipped. The test compares exact RDF terms and duplicate multiplicities for both the generic and native LMDB paths.

The Jena generator and verifier remain test-side tools in `testsuites/benchmark-common`; Jena stays out of LMDB's dependency graph. Their commands require explicit input or output paths. The same result-bag codec has no Jena dependency, and its unit tests use inline data so ordinary codec tests stay hermetic.

## Progress

- [x] (2026-11-07) Inspected the existing catalog, generator, Jena oracle, LMDB parity test, and result fingerprint helper.
- [x] (2026-11-07) Confirmed seeded REAL_ESTATE counts are jittered and that generated districts and amenities are always named.
- [x] (2026-11-07) Captured a Surefire red for isomorphic input BNodes with arm-specific labels.
- [x] Add and run exact typed result-bag codec controls.
- [x] Select and prove bounded fixture witnesses.
- [x] Add codec adversarial scaling controls.
- [x] Correct strict codec comparison paths.
- [x] Generate and independently verify pinned Jena bundle.
- [in_progress] Diagnose first exact-bag mismatch.
- [ ] Compare all 13 Jena, generic, and native result bags.
- [ ] Format the changed paths and archive evidence.

Exactly one implementation step is in progress: generate and independently verify the pinned Jena bundle. The bounded fixture witness test passed and its report is preserved under `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/realestate-fixture-coverage-green-20261107T155100Z`; the codec's original red and full reports are preserved under `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/realestate-oracle-codec-red-su4v6szu`; current codec qualification is preserved under `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/codec-canonical-final-review-k6vdezp0`; top-level compact evidence is `initial-evidence.realestate-oracle.txt`.

## Surprises & Discoveries

- `ThemeDataSetGenerator.RealEstateConfig` uses a seeded random generator, but `jitterInt` varies each configured entity count by up to half. A fixture manifest must pin the actual generated counts and N-Quads bytes rather than equating requested and actual counts.
- The RealEstate generator creates a name for every generated district and amenity. The query catalog has optional-name branches that cannot be observed as unbound against its unmodified output; the deterministic fixture overlay must add unnamed witnesses.
- `IndependentSparqlOracle` already preserves typed literals, recursive triple terms, unbound result domains, and duplicates, but its `CanonicalBNode` is the raw parser ID and its class documentation explicitly declines cross-store blank-node normalization. A real differential test must show the required global mapping behavior before that comparison path changes.
- `SolutionBagFingerprint` is intentionally an audit fingerprint, not authoritative equality. The new expected-bag codec must store and compare actual rows and multiplicities rather than reuse its hash as the answer.
- The first differential red is in `IndependentSparqlOracleTest#isomorphicInputBlankNodesCompareWithOneGlobalBijection`: its two rows share one blank node, while four equivalent repositories intentionally load distinct blank-node IDs. Surefire reports `tests=1, failures=1, errors=0`; the failure occurs at `IndependentSparqlOracle.assertSameOutcome`, not in setup. The exact output and source patch are archived in the red bundle above.
- The named-workspace offline install first failed because `maven-dependency-plugin:3.11.0` was absent. One online `mvnf --no-offline` retry fetched it, the root quick install passed, and the focused test then reached the intended assertion. Later runs should return to offline mode.
- `SolutionBagCodecTest` now exercises one Jena-free codec and one global blank-node bijection, including nested terms, duplicate multiplicities, unbound values, language/direction metadata, stable encoding, and ordered row semantics. Its final post-format run passed 7/7.
- The existing four-arm `IndependentSparqlOracle` now converts canonical rows into the shared codec and uses its unordered or ordered comparator. The unchanged cross-arm BNode reproduction and the full oracle class both pass (1/1 and 8/8); full-class evidence is under `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/oracle-global-bnode-class-postformat-20261107T142332Z`.
- Root review identified codec edge cases to cover before fixture work: reject fractional JSON integers and language/direction datatype conflicts, and avoid quadratic candidate preprocessing or recursive depth failures on large bags while preserving exact global BNode bijection semantics.

## Decision Log

- Decision: Keep the one checked-in fixture bundle under `core/sail/lmdb/src/test/resources` and avoid adding a test-jar dependency.
  Rationale: LMDB already depends on benchmark-common's main artifact for test support. A test-jar would package all common test classes/resources and add a classifier dependency without improving the permanent test's hermeticity.
  Date/Author: 2026-11-07, Codex.
- Decision: Keep Jena in `testsuites/benchmark-common/pom.xml` test scope and implement the Jena CLI in benchmark-common test sources.
  Rationale: The independent reference engine is used only to create or verify goldens; LMDB consumes a static classpath resource and does not need Jena at runtime or test time.
  Date/Author: 2026-11-07, Codex.
- Decision: Store exact query solution bags, with one row per distinct mapping and an explicit multiplicity.
  Rationale: Counts and fingerprints cannot detect wrong bindings or duplicate loss. Typed term slots and explicit unbound slots preserve the full observable solution mapping.
  Date/Author: 2026-11-07, Codex.
- Decision: Compare blank nodes modulo one bijection shared across every row in a result bag.
  Rationale: Parser and store labels can differ for isomorphic fixtures; per-row relabeling would incorrectly accept inconsistent identities across solutions.
  Date/Author: 2026-11-07, Codex.
- Decision: Implement exactly one blank-node/bag comparator in benchmark-common and adapt `IndependentSparqlOracle` to use it; do not add an independent matcher to the LMDB test helper.
  Rationale: The codec must govern both Jena goldens and existing cross-store result comparisons, or the two oracles could disagree about term and blank-node identity.
  Date/Author: 2026-11-07, Codex.
- Decision: Start fixture discovery with a seeded 256-property, 4-district, 8-agent, 3-amenity config, then retain it only if executable witness checks prove every required branch.
  Rationale: It is far smaller than the existing 4,000-property LMDB fixture and concentrates enough observations for grouped queries. The seed and jitter make expectations deterministic, while mandatory witness checks prevent vacuous coverage.
  Date/Author: 2026-11-07, Codex.

## Outcomes & Retrospective

At completion, summarize the final fixture sizes and hashes, exact 13-query Jena outcomes, generic/native parity totals, any query semantics that required order-aware comparison, and any remaining catalog cells outside this REAL_ESTATE slice.

## Context and Orientation

The query strings and names are in `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java`. The seeded source data generator is `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/rio/util/ThemeDataSetGenerator.java`. The existing count-only Jena comparison lives in `testsuites/benchmark-common/src/test/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalogJenaOracleTest.java`. The existing 4,000-property generic/native check is `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRealEstateNativeParityTest.java`.

`IndependentSparqlOracle` in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation` builds four repositories and compares rows, but stores blank-node IDs directly. `SolutionBagFingerprint` in `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan` fingerprints rows for diagnostics; it is not an exact bag. The new codec belongs to benchmark-common main source so LMDB can use it through the existing test-scope dependency. It must not depend on Jena.

The one static fixture directory will contain a deterministic N-Quads dataset, one manifest, and an exact expected-bag ledger for indices 0 through 12. The Jena CLI receives its file paths explicitly. The permanent LMDB test loads resources from its classpath and fails if a resource is missing.

## Plan of Work

First add a regression to `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/IndependentSparqlOracleTest.java`. Its loader will supply the same two-blank-node graph structure to every oracle arm while assigning different blank-node IDs to each arm. `assertEquivalent` must accept this isomorphic bag. A paired negative case will use row structures that require contradictory renamings and must remain unequal. Run the positive method against the existing implementation and save its Surefire failure before changing comparison behavior. This is the genuine pre-fix behavioral red.

Next add `SolutionBagCodecTest` in `testsuites/benchmark-common/src/test/java/org/eclipse/rdf4j/benchmark/common/oracle`. Cover IRIs, blank nodes, literals with lexical form/datatype/language/base direction, recursive RDF-star `TripleTerm`, projected variables that are unbound in a row, repeated solution multiplicity, and byte-for-byte stable encoding. The blank-node tests compare bags using one bidirectional map over all rows: renamed identities in a consistent graph pass, while a case that would pass only if each row chose a new mapping fails. Write all these tests before the codec implementation. The existing `SolutionBagFingerprintTest` is useful term-encoding precedent but remains a separate lossy diagnostic.

Implement the smallest Jena-free codec in `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/oracle`. Model a bag as a complete variable header and immutable rows whose slots are either an unbound marker or a recursive typed RDF term, plus a positive `long` multiplicity. Use deterministic JSON with stable key and row order; reject duplicate schema names, undeclared bindings, malformed term records, nonpositive multiplicities, and unsupported values. Blank-node equivalence must be one global bijection across the full bag, recursively including triple-term children. Keep hashing only for file-integrity pins, never for deciding bag equality.

Add `RealEstateOracleFixtureProfile` and its coverage tests in benchmark-common test sources. Generate a small seeded base model, add a deterministic witness overlay for missing name optionals, a tied top-list price, and a property appearing in both q10 UNION branches, and serialize the result in a deterministic sorted N-Quads form. Record actual jittered counts. Coverage tests must establish the intended branch witnesses rather than accept a result count alone. At minimum verify: q0 surviving and rejected average groups plus bound/unbound ratings; q1 nonempty numeric aggregate plus year optional states; q2 more than 25 eligible listings plus the equal-price IRI tie; q3 DISTINCT collapse and sold/cheap exclusions; q4 price-only and area-only COALESCE cases plus MINUS; q5 both filter arms and qualifying/rejected HAVING groups; q6 matching and decoy worksIn cases plus phone states; q7 both price clauses plus sold/offer exclusions; q8 passing and rejected feedback groups plus phone states; q9 low, high and excluded middle offer amounts plus distinct offers and unnamed district; q10 sold, offer and overlapping UNION rows, named/unnamed amenities and a HAVING reject; q11 full and absent optionals with a viewing-by-offer product; q12 both UNION entity types, varied optionals and repeated viewing rows.

The Jena-side tool is a test-source class such as `ThemeOracleBundleCli` in benchmark-common. `generate --output-dir <required-path>` writes the fixture, manifest, and all 13 bags. `verify --input-dir <required-path>` checks the checked-in fixture and recomputes all 13 Jena bags. It has no implicit output directory and cannot skip when files are missing. The benchmark-common codec tests use inline values only; they never read the LMDB fixture. Run the CLI serially outside workspace mode because direct Maven plugin goals are not allowed in workspace mode. One planned invocation after source implementation is:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark-common test-compile org.codehaus.mojo:exec-maven-plugin:3.6.4:java -Dexec.classpathScope=test -Dexec.mainClass=org.eclipse.rdf4j.benchmark.common.ThemeOracleBundleCli -Dexec.args='generate --output-dir /ABSOLUTE/REPO/core/sail/lmdb/src/test/resources/org/eclipse/rdf4j/sail/lmdb/realestate-oracle'

Run the same command with `verify --input-dir` set to that directory. If a required offline plugin/dependency is missing, follow repository policy: one online run to fetch it, then return offline.

Add `LmdbRealEstateExactBagParityTest` in LMDB test sources. Load N-Quads and bag ledger from required classpath resources, verify file/config/query/codec/Jena pins, and compare all 13 queries against the golden bag and between generic and native evaluation. `@TempDir` may hold LMDB database files; it must not hold or generate fixture/golden inputs. For q2 compare its fully ordered result because price-descending then unique property IRI is a total order. For q11 compare exact bags and require nondecreasing order keys, comparing rows as a bag inside any equal-key group. No other query may pass based only on a row count. For COUNT aggregates assert their numeric value is positive; for q1 require bound SUM/MIN/MAX; for grouped cells require actual qualifying groups. This avoids a one-row `COUNT=0` being counted as a successful nonvacuous test.

The manifest pins bundle schema, codec schema/version, canonical config and seed, requested and actual jittered entity counts, fixture byte count/statement count/SHA-256, deterministic overlay identity, all 13 exact query SHA-256 values, Jena ARQ 6.1.0, and expected-ledger SHA-256. The ledger has exactly one entry for each index; no missing-row skip. It stores typed values and multiplicities, not only counts or fingerprints. For later 182-cell work, use explicit statuses such as `SUPPORTED`, `UNSUPPORTED_SYNTAX`, `UNSUPPORTED_ALGEBRA`, `RESOURCE_LIMIT`, `CANCELLED`, or `ERROR`; an empty supported bag is valid only when pinned and justified by fixture coverage.

For other catalog queries, preserve SPARQL's permitted nondeterminism in the comparator. `SAMPLE` can return any input member of the group. `GROUP_CONCAT` can order its contributed lexical values arbitrarily, so compare the delimiter-separated contributors as a multiset while retaining duplicate pieces. `LIMIT`/`OFFSET` with non-total sort keys may select any legal boundary subset and permute within ties. Current RealEstate q2 has a unique property IRI tiebreak; q11 can still have equal order-key groups. Never make all result rows unordered if the query has an observable total order.

## Concrete Steps

After source/build lock ownership is confirmed, capture the initial blank-node red with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace realestate-oracle IndependentSparqlOracleTest#isomorphicInputBlankNodesCompareWithOneGlobalBijection --module core/sail/lmdb --retain-logs -- -DskipITs -DskipFrontend=true

Save its exact verify log and Surefire text/XML report under a new evidence bundle and append the compact command and failure to `initial-evidence.realestate-oracle.txt`. The failure must show the current raw-ID difference in result bags, not a setup or compilation failure.

Then add the benchmark-common codec tests and run:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace realestate-oracle SolutionBagCodecTest --module testsuites/benchmark-common --retain-logs -- -DskipFrontend=true

After the codec implementation, rerun that exact selector and save the passing report. Run fixture coverage separately:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace realestate-oracle RealEstateOracleFixtureTest --module testsuites/benchmark-common --retain-logs -- -DskipFrontend=true

Generate and verify the bundle using the required explicit paths. The first permanent LMDB comparison is:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace realestate-oracle LmdbRealEstateExactBagParityTest --module core/sail/lmdb --retain-logs -- -DskipITs -DskipFrontend=true

Do not use `-am` or `-q` with any test invocation. `mvnf` performs its root `-Pquick install` before test verification and uses the same named workspace repository/build tree. The direct CLI invocation is serialized outside workspace mode. After targeted tests, run the directly relevant existing Jena oracle and LMDB parity tests, then exact-path header/Spotless checks for changed Java files, `git diff --check`, and a final source/resource hash audit. Do not stage or commit.

## Validation and Acceptance

The pre-fix global-BNode test must produce a Surefire assertion failure caused by different raw blank-node labels from structurally identical arm datasets. After the codec and oracle integration, the same test passes and its negative inconsistent-mapping control remains rejected. All codec tests pass, including stable byte encoding, typed-term distinctions, unbound versus empty literal, duplicate count, recursive TripleTerm, and one global blank-node bijection.

The fixture coverage test reports actual seeded/jittered entity counts and proves every stated q0–q12 witness. The Jena verifier recomputes exactly the 13 pinned bags from the checked-in N-Quads. The LMDB test loads all resources from the test classpath without a skip; both generic and native evaluation match each Jena bag exactly and duplicate multiplicities match. Missing resources, query-hash drift, unexpected unsupported/error/resource outcomes, zero-count aggregates, and bag mismatches fail with the query index/name and expected/actual row details. Formatting and `git diff --check` pass for changed sources.

## Handoff and Evidence

For each focused selector, retain the command, source hashes, full Maven log, and exact Surefire report before the next invocation can replace reports. Write compact evidence to `initial-evidence.realestate-oracle.txt`. Before implementing the codec, provide the parent the exact pre-fix BNode assertion snippet. After codec tests go green, freeze a patch/hash for review while working only on the disjoint fixture and LMDB test files; do not alter codec source during review.
