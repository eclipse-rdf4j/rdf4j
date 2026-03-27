# Parquet Read-Only Sail

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with [PLANS.md](PLANS.md) at the repository root.

## Purpose / Big Picture

After this change, RDF4J can mount one or more parquet files as a read-only Sail by reading a JSON mapping file. A user can point `ParquetStore` at that mapping file, query parquet-backed data with SPARQL, read namespaces and named graphs exposed by the mapping, and rely on RDF4J’s normal read path without materializing RDF into a side store.

The user-visible proof is in `core/sail/parquet/src/test/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreReadTest.java`: joined parquet files answer SPARQL queries, graph-scoped queries work, FILTER evaluation still works, and the mapping prefixes are exposed as namespaces while all write operations still fail with `SailReadOnlyException`.

## Progress

- [x] (2026-03-27 19:45Z) Added `core/sail/parquet` module and public surface: `ParquetStore`, `ParquetStoreConfig`, `ParquetStoreFactory`, schema vocab, and service registration.
- [x] (2026-03-27 20:05Z) Added read-only connection behavior and initial parquet-backed read test with joined sources.
- [x] (2026-03-27 20:30Z) Implemented JSON mapping load, schema validation, row reads, lazy join indexes, and predicate-grouped statement planning.
- [x] (2026-03-27 20:50Z) Added loader validation tests for missing files, incompatible schemas, join key mismatch, and aggregated warnings.
- [x] (2026-03-27 21:05Z) Added failing namespace exposure test, fixed store initialization to seed namespaces from mapping prefixes, and verified the green rerun.
- [x] (2026-03-27 21:15Z) Added named-graph and FILTER coverage for parquet-backed SPARQL reads.
- [x] (2026-03-27 21:35Z) Split the oversized mapping model into `ParquetStoreMapping.java` plus `TermSpec.java`, then sealed the cross-package access with narrow mapping methods.
- [x] (2026-03-27 21:41Z) Re-ran `ParquetMappingLoaderTest`, `ParquetStoreReadTest`, and full `core/sail/parquet` verify successfully.

## Surprises & Discoveries

- Observation: mapping prefixes were parsed but not exposed through the namespace API.
  Evidence: `ParquetStoreReadTest.mappingPrefixesAreExposedAsNamespaces` initially failed with `expected: "http://example.com/" but was: null`.

- Observation: `mvnf` runs are not safe to execute in parallel in this repository because each run starts a root `-Pquick install`.
  Evidence: a parallel `mvnf` pair collided in `testsuites/benchmark-common` shade output and one run failed before the module test even started.

- Observation: parquet schema metadata is enough to reject missing columns, incompatible schemas, and join key type mismatches during Sail initialization.
  Evidence: `ParquetMappingLoaderTest` covers all three without opening a repository connection.

- Observation: moving term classes out of `ParquetStoreMapping.java` exposed an accidental package boundary leak from `reader` into `mapping`.
  Evidence: the first refactor compile failed with `TermSpec.resolve(...) is defined in an inaccessible class or interface`, which was fixed by adding `resolveSubject`, `resolveObject`, and column-name accessors on the mapping model.

## Decision Log

- Decision: build the Sail on top of `ExtensibleStore` instead of a custom `SailStore`.
  Rationale: reuse RDF4J statement iteration, Sail plumbing, and repository integration already present in `core/sail/extensible-store`.
  Date/Author: 2026-03-27 / Codex

- Decision: keep parquet as live storage and load rows lazily per source with reusable in-memory join indexes.
  Rationale: matches the plan’s “virtual reads, no materialized RDF side store” requirement while keeping the first implementation understandable.
  Date/Author: 2026-03-27 / Codex

- Decision: validate referenced columns and join key types during mapping load.
  Rationale: fail fast at initialization instead of returning partial or confusing query results later.
  Date/Author: 2026-03-27 / Codex

- Decision: expose mapping prefixes as namespaces during store initialization.
  Rationale: the mapping file is the configuration source of truth, and namespace reads are part of the normal RDF4J read surface.
  Date/Author: 2026-03-27 / Codex

- Decision: keep evaluation statistics conservative for now by using `EvaluationStatisticsEnum.constant`.
  Rationale: it avoids incorrect optimizer claims while the parquet-specific stats package is still incomplete.
  Date/Author: 2026-03-27 / Codex

## Outcomes & Retrospective

The module now exists and can answer joined parquet-backed SPARQL reads through RDF4J’s normal Sail and Repository APIs. The packaging polish work is complete: the oversized mapping file was split, the focused guards stayed green, and full module verify passed. The main remaining gaps versus the original plan are breadth rather than viability: optimizer statistics remain conservative instead of parquet-specific, and the broader compliance matrix from the original design is not yet wired up.

The biggest lesson so far is that initialization-time validation buys clarity. The failing namespace test also confirmed that “mapping is the source of truth” applies to namespaces, not just triple generation.

## Context and Orientation

The implementation lives under `core/sail/parquet`. The public API files are:

- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/ParquetStore.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreConnection.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/config/ParquetStoreConfig.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/config/ParquetStoreFactory.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/config/ParquetStoreSchema.java`

The internal runtime currently lives in:

- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/mapping/ParquetMappingLoader.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/mapping/ParquetStoreMapping.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/reader/ParquetDataStructure.java`
- `core/sail/parquet/src/main/java/org/eclipse/rdf4j/sail/parquet/reader/ParquetSourceData.java`

In this repository, an “ExtensibleStore” is RDF4J’s experimental Sail base that delegates storage and namespace behavior to supplied interfaces. Here, `ParquetDataStructure` implements the statement storage view and `SimpleMemoryNamespaceStore` holds read-only namespaces copied from the mapping file.

The tests that prove behavior today are:

- `core/sail/parquet/src/test/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreSurfaceTest.java`
- `core/sail/parquet/src/test/java/org/eclipse/rdf4j/sail/parquet/ParquetStoreReadTest.java`
- `core/sail/parquet/src/test/java/org/eclipse/rdf4j/sail/parquet/mapping/ParquetMappingLoaderTest.java`

## Plan of Work

The implementation work in this ExecPlan is complete for the current slice. The next recommended work is not required to use the module today: create a dedicated `stats` package that computes parquet-aware estimates from source row counts and compiled mapping multiplicity, then add the larger read-only compliance harness promised in the original feature plan.

## Concrete Steps

From the repository root:

1. Refactor the mapping model into smaller files with `apply_patch`, keeping the package `org.eclipse.rdf4j.sail.parquet.mapping`.
2. Run the focused tests that cover the refactor:

       python3 .codex/skills/mvnf/scripts/mvnf.py ParquetMappingLoaderTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py ParquetStoreReadTest --retain-logs

3. Run the module verify:

       python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/parquet --retain-logs

4. If verification passes, update this file with the exact command outcomes and note that the plan is complete. If verification fails, capture the report path and failing snippet here before editing production code again.

## Validation and Acceptance

Acceptance is behavioral:

- `ParquetStoreReadTest` must show joined parquet reads work, named graph reads work, FILTER evaluation still produces the expected row, namespace reads expose mapping prefixes, and writes remain rejected.
- `ParquetMappingLoaderTest` must show initialization fails on missing parquet files, schema mismatches, and join key type mismatches, while unknown JSON fields only emit one aggregated warning.
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/parquet --retain-logs` must pass.

The new Sail is considered usable when a user can instantiate `new ParquetStore(mappingFile)` and retrieve mapped statements through repository queries without any materialized RDF side store.

## Idempotence and Recovery

The mapping and runtime changes are additive and local to `core/sail/parquet`, so the refactor can be repeated safely as long as the same focused tests are rerun after each edit. If a test run fails after a refactor, stop changing behavior, rerun the smallest failing selection, and compare against the current green evidence in `logs/mvnf/` and `initial-evidence.txt`.

Do not run multiple `mvnf` commands in parallel for this repository. Each run performs a root install, and parallel runs can clash in generated artifacts under `testsuites/benchmark-common` and `testsuites/benchmark`.

## Artifacts and Notes

Relevant report files and logs:

- `core/sail/parquet/target/surefire-reports/org.eclipse.rdf4j.sail.parquet.ParquetStoreReadTest.txt`
- `core/sail/parquet/target/surefire-reports/org.eclipse.rdf4j.sail.parquet.mapping.ParquetMappingLoaderTest.txt`
- `logs/mvnf/20260327-205101-verify.log` for the initial red namespace test
- `logs/mvnf/20260327-205219-verify.log` for the green namespace rerun
- `logs/mvnf/20260327-205144-verify.log` for the loader green run
- `logs/mvnf/20260327-214012-verify.log` for the post-refactor loader green run
- `logs/mvnf/20260327-214045-verify.log` for the post-refactor read-path green run
- `logs/mvnf/20260327-214234-verify.log` for the final full module verify

Key evidence snippets:

    [ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    expected: "http://example.com/"
     but was: null

    [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

    [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

    [INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0

## Interfaces and Dependencies

The implementation depends on:

- `org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore` for Sail plumbing
- `org.eclipse.rdf4j.sail.extensiblestore.SimpleMemoryNamespaceStore` for read-only namespace exposure
- `org.apache.parquet:parquet-avro` plus minimal Hadoop reader dependencies for parquet file access
- Jackson tree parsing for the JSON mapping file

The public types that must exist at the end are:

- `org.eclipse.rdf4j.sail.parquet.ParquetStore`
- `org.eclipse.rdf4j.sail.parquet.config.ParquetStoreConfig`
- `org.eclipse.rdf4j.sail.parquet.config.ParquetStoreFactory`

The internal types that must still exist after refactor are:

- `ParquetMappingLoader.load(Path mappingFile)`
- `ParquetDataStructure.getStatements(...)`
- the compiled mapping model used to translate parquet rows into RDF statements

Plan update note: created the initial ExecPlan on 2026-03-27 because this feature is large enough that a restartable, self-contained implementation record is mandatory. Revised it after the refactor and final module verify to capture the completed state and the cross-package access lesson from the split.
