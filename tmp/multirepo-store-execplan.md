# Implement MultiRepoStore

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows [PLANS.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/PLANS.md) from the repository root and must be maintained in accordance with that file.

## Purpose / Big Picture

After this change RDF4J will have a read-only sail named `rdf4j:MultiRepoStore` that can query across several local repositories without delegating to higher-level federation query splitting. Instead it opens each member repository's internal sail dataset and injects those datasets into one logical union during normal sail query processing. A user will be able to configure the store with repository IDs or filesystem-backed NativeStore/LmdbStore members, run a SPARQL query once, and see results drawn from all members with duplicate statements collapsed.

## Progress

- [x] (2026-03-11 21:20Z) Read `PLANS.md`, confirm Routine D, and inspect existing sail dataset/query code paths.
- [x] (2026-03-11 22:20Z) Run mandatory root quick install: `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`.
- [ ] Add smallest failing tests for config, query union, read-only behavior, and lifecycle.
- [ ] Implement public `SailStoreProvider` support in eligible stores.
- [ ] Add `core/sail/multirepo` module with config, store, connection, composite sources, and stats.
- [ ] Run focused tests, broader module tests, formatter, and final verification.

## Surprises & Discoveries

- Observation: `SailSourceConnection` already evaluates queries against a `SailDataset`, so the clean seam is not query parsing or repository wrappers but supplying a composite `SailStore` whose sources yield union datasets.
  Evidence: `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceConnection.java` calls `branch.dataset(...)`, wraps it in `SailDatasetTripleSource`, and then runs the normal evaluation strategy.

- Observation: existing `UnionSailDataset` is only binary and uses `DualUnionIteration`, which is a bag union, not distinct semantics.
  Evidence: `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/UnionSailDataset.java` delegates to `DualUnionIteration.getWildcardInstance(...)`; `core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/DualUnionIteration.java` documents a bag union.

- Observation: generic `Sail` does not expose `SailStore`, so true low-level federation needs an explicit provider interface rather than reflection or repository-level fallbacks.
  Evidence: public sail APIs expose `SailConnection`, not `SailStore`; concrete stores keep `getSailStore()` package-private.

## Decision Log

- Decision: implement a public `SailStoreProvider` interface in `rdf4j-sail-base` and have `MemoryStore`, `NativeStore`, `LmdbStore`, and `ExtensibleStore` implement it directly.
  Rationale: this preserves the requested low-level dataset injection design and avoids fragile reflection or higher-level query fallback paths.
  Date/Author: 2026-03-11 / Codex

- Decision: v1 remains read-only and rejects write/update operations with `SailReadOnlyException`.
  Rationale: write coordination across multiple underlying stores is semantically ambiguous and not required for the requested outcome.
  Date/Author: 2026-03-11 / Codex

- Decision: direct filesystem members require explicit `sailType` and support only `openrdf:NativeStore` and `rdf4j:LmdbStore`.
  Rationale: explicit typing keeps config predictable and avoids brittle on-disk auto-detection.
  Date/Author: 2026-03-11 / Codex

## Outcomes & Retrospective

- Pending. Fill after the first working milestone and again at completion.

## Context and Orientation

The existing sail execution path lives in `core/sail/base`. `SailSourceConnection` is the standard read/write connection used by MemoryStore, NativeStore, LmdbStore, and ExtensibleStore. It does not query a `Repository`; it asks a `SailStore` for explicit and inferred `SailSource` objects, opens a `SailDataset` snapshot from those sources, and evaluates the query directly against that dataset.

`SailStore` means the low-level object that owns two logical data streams: explicit statements and inferred statements. `SailSource` means a readable or writable view of one of those streams. `SailDataset` means a read-only snapshot-like view used during query execution. These types live in `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/`.

The current concrete stores already expose their internal `SailStore` only through package-private methods:

- `core/sail/memory/src/main/java/org/eclipse/rdf4j/sail/memory/MemoryStore.java`
- `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/NativeStore.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java`
- `core/sail/extensible-store/src/main/java/org/eclipse/rdf4j/sail/extensiblestore/ExtensibleStore.java`

Repository-manager resolution happens in `core/repository/manager`. `LocalRepositoryManager` injects itself into repositories that implement `RepositoryResolverClient`. `SailRepository` forwards that resolver into the first sail in the stack that implements `RepositoryResolverClient`. MultiRepoStore will use this mechanism when members are configured by repository ID.

Config/factory patterns for sails live beside existing stores. `MemoryStoreConfig` and `MemoryStoreFactory` are the simplest examples. `BaseSailConfig` already handles query-evaluation-mode and evaluation-strategy-factory overrides, so MultiRepoStore should extend that base config instead of re-implementing those properties.

## Plan of Work

Start with failing tests. Add a new test class in the multirepo module for query behavior and read-only enforcement, plus a config test covering parse/export/validation. The first query test should construct two small member stores with an overlapping statement and prove that a query against MultiRepoStore returns the union once, not twice. Another test should prove `includeInferred` toggles whether inferred member data participates. A read-only test should prove add/remove or update calls fail. A lifecycle test should distinguish repository-ID members from filesystem-created members by checking manager-owned repositories are still available after MultiRepoStore shutdown while direct filesystem members are closed only internally.

Then add the low-level provider seam. Introduce `SailStoreProvider` in `rdf4j-sail-base` with a single accessor returning the owned `SailStore`. Update the four eligible concrete stores to implement it by returning their current internal store. This is intentionally narrow: wrapped sails that do not themselves implement the interface will be rejected in v1.

Create `core/sail/multirepo` and register it in `core/sail/pom.xml`. Add config classes and a factory so RDF config can create the new sail. The config must model members as blank nodes where exactly one of `repositoryId` or `repositoryLocation` is present; `sailType` is required only for location-based members. Validation must fail for missing sources, both sources set, missing `sailType` on a location member, or unsupported `sailType`.

Implement `MultiRepoStore` as an `AbstractNotifyingSail` that also implements `RepositoryResolverClient` and `FederatedServiceResolverClient`. During initialization it resolves member definitions into concrete handles. Repository-ID members must come from the resolver, must be `SailRepository`, and their top sail must implement `SailStoreProvider`. Filesystem members must be built as `SailRepository(new NativeStore(...))` or `SailRepository(new LmdbStore(...))`, initialized by the multirepo store, and later shut down by it. Preserve member order from config. Compute supported isolation levels as the ordered intersection across members and fail initialization if the intersection is empty; default isolation is the first common level.

Implement `MultiRepoSailStore` plus composite source and dataset support. The composite store should expose explicit and inferred `SailSource`s, each backed by an ordered list of member sources. `dataset(...)` must open all member datasets for that stream and return one logical dataset view. `sink(...)` must always throw `SailReadOnlyException`. The composite dataset must return distinct statements, triples, contexts, and namespaces across all members. Prefix lookup is first-member-wins. Ordered statement reads must preserve the requested order across members while suppressing duplicates; unordered reads can filter through a distinct wrapper.

Implement combined evaluation statistics for optimizer use. For `StatementPattern` and `TripleRef`, ask each member store's statistics for cardinality and sum the results. For everything else, delegate to default `EvaluationStatistics` behavior. This keeps join ordering conservative without inventing new optimizer semantics.

Finish with wiring and verification. Add service registration for `MultiRepoStoreFactory`, ensure the module depends on sail-base, repository-sail, repository-manager, native, and lmdb as needed, run focused tests until green, then broader module verifies and formatting.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j-stf` run the following during implementation:

1. Add failing tests with `apply_patch`.
2. Run the most specific failing tests with `python3 .codex/skills/mvnf/scripts/mvnf.py --module core/sail/multirepo <Class#method>` once the module exists, or fall back to a nearby module test selector while bootstrapping.
3. Persist first failing test evidence to `initial-evidence.txt`.
4. Implement the provider seam and multirepo module code.
5. Re-run the same focused tests until they pass, then run the module verify command:

    `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/multirepo`

6. Run the formatter and copyright check:

    `cd /Users/havardottestad/Documents/Programming/rdf4j-stf/scripts && ./checkCopyrightPresent.sh`

    `cd /Users/havardottestad/Documents/Programming/rdf4j-stf && mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources`

Expected test proof at the end:

- a config round-trip test passes;
- a union query test returns one result for duplicated member statements and multiple results for distinct member statements;
- a read-only test throws `SailReadOnlyException`;
- direct filesystem member tests open both NativeStore and LmdbStore directories successfully.

## Validation and Acceptance

Acceptance is behavior, not just compilation. A human should be able to create two small member stores, configure `rdf4j:MultiRepoStore`, run a query such as `SELECT ?s WHERE { ?s <urn:p> <urn:o> }`, and see subjects from both members while duplicate triples appear only once. The same store should reject writes with a clear read-only exception. When configured with `includeInferred = false`, inferred-only member statements must not appear; with `includeInferred = true`, they must appear.

Automated acceptance is:

- run focused tests proving the new behavior fails before the implementation and passes after;
- run `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/multirepo` and expect module verify success;
- run any targeted provider-store tests touched by the new `SailStoreProvider` interface and expect success.

## Idempotence and Recovery

The plan is additive. Re-running the quick install and test commands is safe. Config parsing and export tests should use temporary directories so they do not depend on prior state. Filesystem member tests must create fresh temp directories for NativeStore and LmdbStore data. If a direct-path member fails to initialize because the directory contents are invalid, delete the temp directory created by the test and recreate it through the test setup rather than reusing partially initialized data.

## Artifacts and Notes

Important reference paths:

- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceConnection.java`
- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailStore.java`
- `core/sail/memory/src/main/java/org/eclipse/rdf4j/sail/memory/MemoryStore.java`
- `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/NativeStore.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java`
- `core/repository/sail/src/main/java/org/eclipse/rdf4j/repository/sail/SailRepository.java`
- `core/repository/manager/src/main/java/org/eclipse/rdf4j/repository/manager/LocalRepositoryManager.java`

Initial build proof:

    [INFO] BUILD SUCCESS
    [INFO] Total time: 26.370 s (Wall Clock)

## Interfaces and Dependencies

Define these public types by the end of the work:

- `org.eclipse.rdf4j.sail.base.SailStoreProvider`
  - `SailStore getSailStore()`

- `org.eclipse.rdf4j.sail.multirepo.MultiRepoStore`
- `org.eclipse.rdf4j.sail.multirepo.MultiRepoStoreConnection`
- `org.eclipse.rdf4j.sail.multirepo.config.MultiRepoStoreConfig`
- `org.eclipse.rdf4j.sail.multirepo.config.MultiRepoStoreFactory`
- `org.eclipse.rdf4j.sail.multirepo.config.MultiRepoStoreSchema`

The new module must depend on:

- `rdf4j-sail-base` for `SailStore`, `SailSource`, `SailDataset`, and `SailSourceConnection`
- `rdf4j-repository-sail` for `SailRepository`
- `rdf4j-repository-manager` for `RepositoryResolver`
- `rdf4j-sail-nativerdf` and `rdf4j-sail-lmdb` for direct filesystem members
- standard test dependencies already used by sibling sail modules

Revision note: created this ExecPlan at implementation start to satisfy Routine D and record the chosen low-level federation design before code changes begin.
