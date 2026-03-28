# Parquet Native Sail Milestone 1

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with [PLANS.md](../PLANS.md) at the repository root.

## Purpose / Big Picture

After this milestone, `ParquetStore` still opens the same JSON mapping file and still behaves as a read-only RDF4J Sail, but the runtime underneath stops pretending parquet is row-oriented storage. The store will move off `ExtensibleStore`, compile mapping triples into physical parquet plans, read only projected columns, push simple bound subject and object equality down into parquet readers, and feed RDF4J’s optimizer better statement-pattern estimates.

The user-visible proof is still the repository query tests, plus two new internal proofs. First, `ParquetStoreSurfaceTest` will show that the public store now sits on the normal `AbstractNotifyingSail` and `SailStore` seam instead of `ExtensibleStore`. Second, `ParquetStoreReadTest` will show that a bound subject query can report a selective physical read count through debug metrics and that parquet-aware cardinality estimates are lower for selective patterns than for broad scans.

## Progress

- [x] (2026-03-27 19:45+01:00) Added `core/sail/parquet` module and the initial read-only parquet Sail surface.
- [x] (2026-03-27 21:41+01:00) Landed the first working read path on `ExtensibleStore`, with mapping validation and joined parquet reads.
- [x] (2026-03-27 22:14+01:00) Re-verified the existing module with `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/parquet --retain-logs` and preserved baseline evidence in `initial-evidence.txt`.
- [x] (2026-03-27 22:18+01:00) Re-read `PLANS.md`, `SailStore`, `SailSourceConnection`, and parquet reader APIs to scope the first native runtime slice.
- [x] (2026-03-28 04:18+01:00) Added failing tests for the class hierarchy swap, selective read metrics, and parquet-aware cardinality estimates.
- [x] (2026-03-28 09:10+01:00) Replaced `ParquetStore` / `ParquetStoreConnection` with a custom read-only `SailStore` pipeline and deleted the old `ParquetDataStructure` / `ParquetSourceData` hot path.
- [x] (2026-03-28 09:10+01:00) Compiled mapping statement plans into parquet physical plans with per-triple projected columns and late RDF statement creation.
- [x] (2026-03-28 09:10+01:00) Added simple equality pushdown, runtime row-read metrics, and source-scan caching.
- [x] (2026-03-28 09:12+01:00) Added parquet-aware `EvaluationStatistics`, passed focused verify, and passed full `core/sail/parquet` verify.

## Surprises & Discoveries

- Observation: the repo-local `PLANS.md` exists, but the AGENTS note that points to `.agent/PLANS.md` is stale.
  Evidence: `ls -la .agent` failed with `No such file or directory`, while `rg --files -g 'PLANS.md'` returned `PLANS.md`.

- Observation: parquet-java already exposes the exact primitive hooks needed for the first slice: requested projection, stats filters, column-index filters, and bloom filters.
  Evidence: `javap` on `ParquetReader$Builder` showed `withFilter(...)`, `useStatsFilter(...)`, `useColumnIndexFilter(...)`, and `useBloomFilter(...)`; `javap` on `AvroReadSupport` showed `setRequestedProjection(...)`.

- Observation: the current parquet reader path fully materializes rows and join indexes before RDF4J ever sees a statement pattern.
  Evidence: `ParquetDataStructure.joinRows(...)` builds complete binding maps, and `ParquetSourceData.loadRows()` reads every field from every record into heap maps.

- Observation: parquet test fixtures emit extremely noisy DEBUG logs during focused verify because parquet writer and reader internals log schema/footer details by default in this module’s test setup.
  Evidence: the focused `verify` output stayed green, but most of the terminal output was parquet footer and page-writer DEBUG noise rather than surefire summaries.

## Decision Log

- Decision: keep the public `ParquetStore(Path)` and config entrypoints stable while replacing only the internal runtime.
  Rationale: repository users should not need code changes to adopt the native path.
  Date/Author: 2026-03-27 / Codex

- Decision: scope milestone 1 to the first shippable native slice instead of the whole multi-phase overhaul.
  Rationale: the full program includes dataset manifests, sidecars, sketches, exact indexes, and rewrite tooling; landing the core runtime first reduces risk and keeps the work verifiable.
  Date/Author: 2026-03-27 / Codex

- Decision: use the stock `SailStore` / `SailSourceConnection` seam, not a custom evaluator, for the first milestone.
  Rationale: this already gives RDF4J optimizer integration, namespaces, context iteration, and standard fallback behavior. The first milestone only needs better statement-pattern access and better estimates.
  Date/Author: 2026-03-27 / Codex

- Decision: add package-private debug hooks for runtime metrics and evaluation statistics.
  Rationale: performance changes need deterministic repository tests. Package-private hooks keep the public API stable while giving the test suite direct evidence.
  Date/Author: 2026-03-27 / Codex

- Decision: stop at statement-pattern-native pushdown for this milestone and keep full RDF4J query evaluation above it.
  Rationale: the user-approved long-term plan includes BGP-native execution, but replacing the storage seam, killing eager row materialization, and feeding the optimizer better estimates was the first shippable cut with bounded risk.
  Date/Author: 2026-03-28 / Codex

## Outcomes & Retrospective

The previous milestone proved the feature shape: parquet mappings can answer SPARQL reads without materializing RDF into another store. This milestone removed the architectural dead end. `ParquetStore` now sits directly on the standard `AbstractNotifyingSail` plus `SailStore` seam, the old eager row-materializing classes are gone, the runtime compiles per-triple physical plans, and simple bound subject or object equality can narrow parquet scans before Java joins and RDF value creation.

The main gap versus the full overhaul remains breadth, not direction. This milestone does not yet ship dataset manifests, sidecar persistence, sketches, exact indexes, or a BGP-native evaluator. It does establish the native runtime shape those later phases need.

## Context and Orientation

The parquet Sail module lives in `core/sail/parquet`. The public surface is:

- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/ParquetStore.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreConnection.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/config/ParquetStoreConfig.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/config/ParquetStoreFactory.java`

The previous row-oriented prototype used:

- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/reader/ParquetDataStructure.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/reader/ParquetSourceData.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/mapping/ParquetStoreMapping.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/mapping/TermSpec.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/mapping/ParquetMappingLoader.java`

In RDF4J terms, a `SailStore` is the storage-facing object that provides two `SailSource` views: explicit statements and inferred statements. A `SailDataset` is a read snapshot from a source. `SailSourceConnection` is the standard connection implementation that handles query evaluation, namespaces, contexts, and transaction shells as long as the store provides those lower-level pieces.

The current native runtime now lives in:

- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/mapping/ParquetPhysicalPlanCompiler.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/runtime/ParquetPhysicalPlan.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/runtime/ParquetQueryEngine.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/runtime/ParquetSourceReader.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/runtime/ParquetSailStore.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/runtime/ParquetEvaluationStatistics.java`

For this milestone, the parquet module became a read-only `SailStore`. It still does not include write support, inferred statements, dataset manifests, sidecar persistence, or sketches. It now does include a compiled statement-pattern runtime, late RDF materialization, projection pushdown, simple equality pushdown, and parquet-aware estimates.

## Plan of Work

Replace the `ExtensibleStore` inheritance tree with a small read-only stack under `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet`. `ParquetStore` should extend `AbstractNotifyingSail`, own the loaded `ParquetStoreMapping`, and construct a `ParquetSailStore` during initialization. `ParquetStoreConnection` should extend `SailSourceConnection`, delegate reads to the new store, and throw `SailReadOnlyException` for all write-oriented hooks.

Create a compact runtime package, either under `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/runtime` or `.../reader`, with the following roles. One class compiles each `ParquetStoreMapping.StatementPlan` into a physical plan that records the root source, joined sources, projected columns per source, and enough term metadata to resolve bound subject or object filters without minting RDF values early. Another class executes those plans for a single statement pattern by reading parquet with requested projection and optional filter pushdown, joining row batches in memory only after each source scan has already been narrowed as much as possible. Statement objects should only be created at the dataset iterator boundary.

The first pushdown slice only needs exact equality derived from the incoming statement pattern. A bound subject can become a source-column equality when the mapping subject template has exactly one placeholder. A bound object can become a source-column equality when the object term is a direct column mapping or a one-placeholder template. When equality cannot be proven lossless, fall back to the broader projected scan and post-filter in Java. Enable parquet stats, column-index, and bloom filters on every filtered reader so parquet-java can skip row groups or pages when the file supports it.

Add a `ParquetEvaluationStatistics` class that uses the compiled plans and source row counts to estimate `StatementPattern` cardinality more intelligently than the default constant model. The estimate does not need to be exact. It does need to make selective bound subject and bound object patterns cheaper than broad scans and make nonexistent predicate bindings estimate to zero when possible.

Keep debug evidence in the runtime. Add a small immutable metrics snapshot that counts rows materialized per source during the most recent query. Expose it only through a package-private method on `ParquetStore` so the tests can confirm that a bound subject query reads fewer parquet rows than the wide scan path.

## Concrete Steps

From the repository root:

1. Add the failing tests in:

   - `core/sail/parquet/src/test/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreSurfaceTest.java`
   - `core/sail/parquet/src/test/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreReadTest.java`

   The first test must assert that `ParquetStore` now inherits from `AbstractNotifyingSail`. The second must drive a bound-subject parquet query, read a package-private metrics snapshot reflectively, and assert that the root source only materialized the matching row. A third assertion in the read test should compare broad and selective cardinality estimates through a package-private statistics hook.

2. Run the focused red tests:

       python3 .codex/skills/mvnf/scripts/mvnf.py ParquetStoreSurfaceTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py ParquetStoreReadTest --retain-logs

3. Replace the runtime internals, keeping `ParquetStore(Path)` stable.

4. Re-run the same focused tests until green, then run:

       python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/parquet --retain-logs

5. Update this file with the exact report paths, success snippets, surprises, and any scope changes.

## Validation and Acceptance

Acceptance is behavioral and measurable:

- `ParquetStoreSurfaceTest` shows `ParquetStore` sits on `AbstractNotifyingSail`, proving the `ExtensibleStore` runtime is gone.
- `ParquetStoreReadTest` still shows joined parquet-backed SPARQL reads, namespaces, graph filtering, and read-only behavior all work.
- The new selective-read test shows the debug metrics report fewer materialized parquet rows for a bound subject query than for the wide scan path.
- The new statistics test shows a selective statement pattern has lower estimated cardinality than the same predicate without a bound subject or object.
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/parquet --retain-logs` passes.

## Idempotence and Recovery

The edits are local to `core/sail/parquet` and this plan file. Re-running initialization and tests is safe. If a refactor leaves the store half-migrated, revert only the local parquet runtime classes you changed, keep the new failing tests, and restart from the red state. Do not run multiple `mvnf` commands in parallel because each invocation performs a root quick install.

## Artifacts and Notes

Baseline evidence already captured before this milestone:

- `initial-evidence.txt`
- `logs/mvnf/20260327-221409-verify.log`

Red and green evidence for this milestone:

- `core/sail/parquet/target/surefire-reports/org.eclipse.rdf4j.sail.parquet.ParquetStoreReadTest.txt`
- `core/sail/parquet/target/surefire-reports/org.eclipse.rdf4j.sail.parquet.ParquetStoreSurfaceTest.txt`
- `logs/mvnf/20260328-030050-verify.log`
- `logs/mvnf/20260328-031809-verify.log`

Key red snippets:

    [ERROR] ParquetStoreReadTest.boundSubjectQueriesUseSelectivePhysicalReadsWithoutScanningUnusedJoins
    java.lang.NoSuchMethodException: org.eclipse.rdf4j.sail.parquet.ParquetStore.snapshotRuntimeMetrics()

    expected: org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail
     but was: org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore

Key green snippets:

    [INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

    [INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0

Former prototype limitation that this milestone removed:

    ParquetDataStructure.joinRows(...) constructs full row bindings before subject/object filtering.
    ParquetSourceData.loadRows() reads every record field into Java heap maps up front.

## Interfaces and Dependencies

The public API that must remain intact:

- `org.eclipse.rdf4j.sail.parquet.ParquetStore`
- `org.eclipse.rdf4j.sail.parquet.config.ParquetStoreConfig`
- `org.eclipse.rdf4j.sail.parquet.config.ParquetStoreFactory`

The first native runtime must end with these internal concepts in place, even if exact class names differ:

- a read-only `SailStore` implementation for parquet
- a read-only `SailSource` / `SailDataset` path for explicit statements
- a physical plan compiler from mapping statement plans to parquet access plans
- a parquet scan executor with projection and simple equality pushdown
- a parquet-aware `EvaluationStatistics`
- a package-private runtime metrics snapshot for tests
- in-memory scan caching keyed by projected columns plus pushed equality filters

Plan update note: rewritten on 2026-03-27 because the original ExecPlan recorded the `ExtensibleStore` prototype as complete. The user then approved a much larger parquet-native overhaul, so the living plan now tracks the first migration milestone instead of the finished prototype.
