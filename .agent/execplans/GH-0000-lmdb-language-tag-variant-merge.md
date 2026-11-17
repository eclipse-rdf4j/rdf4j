# Merge case-variant language-literal ids in legacy LMDB stores

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` (repository root).

## Purpose / Big Picture

RDF4J compares language tags case-insensitively (`"abc"@en` equals `"abc"@EN`, RDF 1.1 Concepts 3.3), so both spellings are one RDF term. Older LMDB stores looked value ids up byte-exactly on disk but through a case-insensitive in-memory cache, so a store that received both spellings while the cache was cold now holds two value ids for one term. Queries against such a store return different rows depending on which spelling a query uses and on whether the cache is warm; after a restart some rows silently disappear. A previous fix (review finding 3.1.6) made new and collision-free stores key language literals canonically (lower-cased tag) and records this in `store.properties` as `language-tag-key=canonical`, but a store that already holds colliding ids was left in the legacy mode `language-tag-key=byte-exact` with a warning.

After this change, opening such a legacy store through `LmdbStore` merges every group of colliding ids into one surviving id: every statement that mentions a losing id (explicit and inferred, every graph, any position of the quad, and nested inside RDF 1.2 triple terms) is rewritten to the survivor, duplicate statements collapse, the losing values are deleted, the store switches to canonical keys and records `language-tag-key=canonical`. The migration is crash-safe (a process that dies at any point leaves a store that the next open finishes migrating) and idempotent (running it again is a no-op). To see it working, run `LmdbLanguageTagVariantMergeTest`: it builds a legacy store that contains colliding ids, opens it, and checks every query result against a `MemoryStore` that received the same statements, including after simulated crashes at every commit boundary of the migration.

## Progress

- [x] (2026-09-29 08:56Z) Root `-Pquick clean install` green before any change (`maven-build.log`).
- [x] (2026-09-29 09:40Z) Research: storage layout, GC, refcounts, predicate guarantees, Frontier journal, sketch/learned sidecars (see Context and Orientation).
- [x] (2026-09-29 10:10Z) Failing tests written: `LmdbLanguageTagVariantMergeTest` (11 red), `LmdbLanguageTagVariantMergeStorageTest` (2 red); evidence in `initial-evidence.lang-merge.txt`.
- [x] (2026-09-29 10:30Z) `LanguageTagVariantMergePlan` + `ValueStore.planLanguageTagVariantMerge()`.
- [x] (2026-09-29 10:30Z) `TripleStore.rewriteStatementIds(...)`.
- [x] (2026-09-29 10:30Z) `ValueStore.applyLanguageTagVariantMerge(...)`; re-keying refactored into `rekeyLanguageTagKeys`.
- [x] (2026-09-29 10:30Z) `LmdbLanguageTagVariantMerge` coordinator, `LmdbSailStore` wiring, `LmdbQuadSynopsisService.markRebuildRequired`.
- [x] (2026-09-29 10:45Z) Pre-existing NPE found by the crash tests (unused triple-term ids freed at open before the triple-term indexes exist) reproduced by `ValueStoreTest#testGcTripleTermsAfterRestart` and fixed (`freeUnusedIdsOnOpen` after `initTermIndexes`).
- [x] (2026-09-29 11:10Z) Early-commit crash tests (value-map resize mid value-store step) added; they exposed an orphaned duplicate triple term, fixed in the planner; each losing id is now retired as a whole.
- [x] (2026-09-29 11:25Z) Sketch invalidation test (mutation-checked); outdated fixture check in `ValueStoreLanguageTagKeyMigrationTest` updated. Focused selection: 83 tests green.
- [x] (2026-09-29 11:30Z) Formatter on `core/sail/lmdb`.
- [x] (2026-09-29 12:05Z) Copyright check: new files pass; only finding is the pre-existing stray `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbDevelopPlanParityIT.java`.
- [x] (2026-09-29 12:10Z) `core/sail/lmdb` unit suite: 2693 run, 0 failures, 1 error (`LmdbStoreMultiRepoTest` ENOSPC: needs ~400 POSIX semaphores, only 22 free host-wide; not a leak - 22 free before and after the 15 merge tests). Evidence `post-evidence.lang-merge-lmdb-unit.txt`.
- [x] (2026-09-29 12:30Z) `core/sail/lmdb` ITs (Failsafe): 144 run, 0 failures, 0 errors, 98 skipped by their own gates. Evidence `post-evidence.lang-merge-lmdb-it.txt`.
- [x] (2026-09-29 12:30Z) Outcomes & Retrospective.

## Surprises & Discoveries

- Observation: `ValueStore.resizeMap` commits the active write transaction and starts a new one when the value map must grow (`endTransaction(true, true)` then `startTransaction(false)`). A "single" value-store write transaction is therefore not atomic when it needs more map space.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` `resizeMap`. Consequence: the value-store half of the migration must be correct for every committed prefix, not only atomically (see Idempotence and Recovery).
- Observation: reference-count increments read the persisted count under key `varint(id)` while decrements and `updateRefCounts` use `[ID_KEY][varint(id)]`, so an increment never sees the persisted count and overwrites it with the per-transaction count. Pre-existing, outside this plan; the migration uses the same helpers as normal triple-term creation/deletion and deletes losing ids' own count entries explicitly.
  Evidence: `incrementRefCount(MemoryStack,long,long)` builds `idBuffer(stack)` + `Varint.writeUnsigned`, `decrementRefCount` builds `idBuffer(stack).put(ID_KEY)`.
- Observation: in RDF 1.2 (this branch) `TripleTerm extends Value`, not `Resource`, so neither a literal nor a triple term can be the subject or graph of a statement through the RDF4J API. Colliding literals reach subject/graph positions only at the storage level (raw id quads). The statement rewrite is position-agnostic; subject, predicate and context positions are covered by a storage-level test, object positions (direct and nested in triple terms) end-to-end.
- Observation: `ValueStore` freed ids left unused by a previous session inside `open()`, before `initTermIndexes`; freeing an unused triple-term id dereferences the (still null) `cspo` triple-term index. With value GC on by default (60 s eviction interval), deleting the last statement of a triple term and closing the store before the value revision is collected made the next open fail. Pre-existing, independent of the merge (which marks losing triple terms unused and therefore hit it).
  Evidence: `ValueStoreTest#testGcTripleTermsAfterRestart` red with `NullPointerException: Cannot invoke "TripleIndex.getDB(boolean)" because "this.tripleTermCspoIndex" is null`; fixed by `freeUnusedIdsOnOpen()` after `initTermIndexes`.
- Observation: after an early commit that rewrote a surviving triple term but had not yet retired the losing one, both are stored with identical components, so neither mentions a losing id and a planner that only collects "affected" triple terms leaves the duplicate behind.
  Evidence: `crashAfterAnEarlyCommitOfTheValueStoreMergeIsCompletedOnReopen[1]` failed with "the losing id of <<( :a :says "w"@en )>> must be gone"; the planner now also groups triple terms stored with identical components (adjacent in `spoc`) and collects every triple term with the resolved components.
- Observation: the sketch synopsis is disabled unless `LmdbStoreConfig.setSketchEstimatorEnabled(true)`; the sketch invalidation test enables it explicitly.
- Observation: a map keyed by `Value` collapses case variants (`Literal.equals` ignores tag case); the test fixture keys ids by N-Triples spelling.

## Decision Log

- Decision: Run the merge from the `LmdbSailStore` constructor, right after the `TripleStore` is opened and before any estimator/statistics service is opened.
  Rationale: the merge needs both the value store (`values/`) and the triple store (`triples/`); both are opened there and nothing else can read or write yet. Frontier statistics, the sketch synopsis and learned sidecars open afterwards and see the migrated data.
  Date/Author: 2026-09-29 / Claude
- Decision: Survivor of a language-literal group = smallest id. Survivor of a triple-term group (triple terms whose components become equal after substituting survivors) = smallest id; a surviving triple term whose stored components still mention a losing id has its components rewritten in place.
  Rationale: deterministic from the value store alone (required for resuming after a crash); the smallest id is the spelling stored first unless ids were reused, matching `MemoryStore`, which keeps the first spelling it saw, and the canonical-mode contract "the id-to-data payload keeps the spelling that was stored first".
  Date/Author: 2026-09-29 / Claude
- Decision: No persisted merge journal. The merge plan is recomputed from the value store on every open; the value store is not modified until every statement is rewritten.
  Rationale: the value store and the triple store are two LMDB environments, so no single transaction can span both. Ordering "rewrite statements (idempotent) → delete losers from the value store" makes the value store the durable plan: as long as a losing id still exists in the value store, the plan that references it can be recomputed, and statements rewritten earlier are simply not found again. Every committed prefix of the value-store step is also a valid input for re-planning (see Idempotence and Recovery).
  Date/Author: 2026-09-29 / Claude
- Decision: Within each triple-store batch transaction, insert the rewritten statements before deleting the originals.
  Rationale: `TripleStore` may split a transaction into two durable commits when the map must grow (record-cache path). With inserts first, any durable prefix contains every insert whose original was deleted, so a crash can at worst leave both copies (repaired by the next run), never lose a statement.
  Date/Author: 2026-09-29 / Claude
- Decision: Rewritten statements go through `TripleStore.storeTriple` / `removeTriples` plus the same predicate-guarantee hooks the sail uses (`recordRdfTermDomain` on insert, `recordPredicateObjectRemoval` on delete), and are therefore journaled for Frontier Statistics V2 like any commit.
  Rationale: this keeps context counts, the explicit-supersedes-inferred rule, predicate guarantees and the Frontier mutation journal consistent by construction; guarantees are written in the same transaction as the rows. Frontier deltas are id-only, so they apply after the losing values are gone; a journal overflow or disabled journal already forces a base rebuild. Learned sidecars (filter selectivity, operator feedback) are discarded on load because the journal sequence they are stamped with advances. Predicates marked for a guarantee rebuild are rebuilt at the end of the statement rewrite when auto-rebuild is enabled.
  Date/Author: 2026-09-29 / Claude
- Decision: Invalidate the persisted sketch synopsis by writing its rebuild marker (`join-estimator.rjes/synopsis-v8.rebuild`) before the first statement is rewritten.
  Rationale: the sketch is otherwise maintained only through the sail sink, which the migration bypasses. The marker survives a crash and is honoured by `LmdbQuadSynopsisService.configurePersistence`.
  Date/Author: 2026-09-29 / Claude
- Decision: An inferred statement whose rewrite already exists as an explicit statement is dropped instead of re-inserted into the inferred plane.
  Rationale: mirrors `storeTriple(explicit)` which deletes an inferred copy, and `MemoryStore`, where a statement is either explicit or inferred.
  Date/Author: 2026-09-29 / Claude
- Decision: Retire each losing id as a whole (delete its entries, drop its reference count, mark it unused, clear its stored hash) instead of deleting all losers and then marking them unused.
  Rationale: a value-map resize can commit the value-store step early; with separate passes a crash could leave ids that are deleted but neither unused nor free, which `nextId` may hand out again while `hashes.dat` still holds the old hash.
  Date/Author: 2026-09-29 / Claude
- Decision: Fix the pre-existing "free unused triple-term ids before the triple-term indexes exist" NPE in this change.
  Rationale: the merge marks losing triple terms unused, so a store closed right after a merge could not be reopened; the bug also affects ordinary GC.
  Date/Author: 2026-09-29 / Claude
- Decision: Losing value ids are marked unused under the current value-store revision (the normal two-phase GC path) and become reusable once freed, like ids freed by ordinary garbage collection.
  Rationale: consistent with the existing id lifecycle; no in-memory `LmdbValue` from before the merge can observe a reused id because the merge commit starts a new value-store revision.
  Date/Author: 2026-09-29 / Claude
- Decision: A package-private test seam `LmdbLanguageTagVariantMerge.disabledForTesting` keeps a byte-exact store byte-exact so tests can build legacy fixtures through the real `LmdbStore` write path; there is no user-facing switch.
  Rationale: fixtures must contain colliding ids produced exactly like the legacy code produced them (byte-exact lookups with a cold cache after restart). Adding a public configuration option was not requested.
  Date/Author: 2026-09-29 / Claude
- Decision: Add `rdf4j-sail-memory` as a test-scoped dependency of `core/sail/lmdb`.
  Rationale: the task requires checking results against the Memory store; the module is part of this repository and has no dependency on LMDB.
  Date/Author: 2026-09-29 / Claude

## Outcomes & Retrospective

Opening a legacy LMDB store that holds several ids for case variants of one language literal now merges them and switches the store to canonical keys. `LmdbLanguageTagVariantMergeTest` (15 tests) builds such a store through the real LMDB write path (three sessions, cold caches, explicit and inferred statements, default and named graphs, a hash-path literal, a directional literal, a three-way group, a group whose first spelling is upper case, triple terms and a nested triple term over variants, a triple term whose survivor needs rewriting) and compares every statement, context, size and 18 SPARQL queries (with and without inferred statements) against a `MemoryStore` fed the same statements, after the merge, with warm caches and after a restart. It also checks that predicate guarantees equal a forced full rebuild, that Frontier Statistics cover the rewritten statements and answer like a forced base rebuild, that a persisted sketch synopsis is marked stale and rebuilt, and that a simulated crash at every durable step (plan, sketch marker, first and fourth one-row statement batch, all statements rewritten, value store merged but store.properties not saved, and an early value-map-resize commit after each of the three value-store sub-steps, with the partial on-disk state asserted) is completed by the next open. `LmdbLanguageTagVariantMergeStorageTest` covers the subject, predicate, object and context positions in both planes with raw id quads, exact per-context counts, and idempotence.

Two defects outside the merge surfaced and were fixed with their own regression test: unused triple-term ids left by a previous session made the next open fail (`ValueStoreTest#testGcTripleTermsAfterRestart`), and the triple-term planner could orphan a duplicate after an early commit (found by the resize-crash tests). A third, the reference-count key mismatch between increments and the persisted counts (present on `main` too), is left for a separate task because the merge does not depend on it beyond the documented residual risk: refcount deltas of a value-store step that a map resize committed early are lost if the process dies right after (the same holds for any value-store transaction that grows the map).

Lesson: crash tests at transaction boundaries are not enough when the storage layer can commit early on its own (`resizeMap`); simulating those early commits found a real planner gap.

## Context and Orientation

All paths are relative to the repository root `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. The module is `core/sail/lmdb`; sources are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`.

An LMDB store directory contains `store.properties` (a Java properties file, class `StoreProperties`), `values/` (an LMDB environment owned by `ValueStore`, the value dictionary) and `triples/` (an LMDB environment owned by `TripleStore`, the statement indexes). `LmdbStore.initializeInternal` loads `store.properties`, constructs `LmdbSailStore` (which constructs `ValueStore` then `TripleStore`), then saves `store.properties`.

Value dictionary (`ValueStore.java`). Every RDF value has a 64-bit id whose low bits encode its kind (`ValueIds.T_URI`, `T_LITERAL`, `T_BNODE`, `T_TRIPLE`). The main database `dbi` holds, for each value, an id-to-data entry (key `[ID_KEY=5][varint id]`, data = the encoded value) and a data-to-id entry: for encodings of at most 16 bytes (`MAX_KEY_SIZE`) the key is the encoding itself; longer encodings are found through a CRC32 hash chain (`[HASH_KEY][varint hash]` → first id, `[HASHID_KEY][varint hash][id key]` → further ids). A literal is encoded as `[LITERAL_VALUE=1][varint datatype id][direction|language length][language][label]`. Triple terms (RDF 1.2 `<<( s p o )>>`) have no data entry; they live in the triple-term indexes (`tripleTermIndexes`, at least `spoc` and `cspo`, in the same environment) whose keys are the varints `(s, p, o, tripleTermId)` in index order. `refCountsDbi` counts internal references (datatype of a literal, namespace of an IRI, components of a triple term). `unusedDbi` / `freeDbi` implement two-phase garbage collection: an id is first marked unused under a revision, then freed (id-to-data deleted, id put on the free list) when that revision is no longer referenced or at the next open (`open()` calls `freeUnusedIdsAndValues(..., null)`). `hashes.dat` (`ValueStoreHashFile`) caches `Value.hashCode()` per id; 0 means unknown.

Language-tag key policy. `ValueStore.LanguageTagKeyMode` is `CANONICAL` (data-to-id key has the tag lower-cased by `canonicalLanguageKey`, the payload keeps the stored spelling) or `BYTE_EXACT` (legacy). `migrateLanguageTagKeysIfNeeded()` runs in the `ValueStore` constructor when `store.properties` has no `language-tag-key`: it scans every language literal with an upper-case tag, re-keys it under its canonical key and records `canonical`, unless two ids would share one canonical key ("colliding variants"), in which case it records `byte-exact`. A store recorded as `byte-exact` stays byte-exact. `ValueStoreLanguageTagKeyMigrationTest` covers this.

Statement indexes (`TripleStore.java`). Statements are quads of ids `(s, p, o, c)`, `c = 0` for the default graph. Each configured index (default `spoc,posc`) has an explicit and an inferred database. `storeTriple(s,p,o,c,explicit)` inserts into every index, increments the per-context count in `contextsDbi`, deletes an inferred copy when an explicit one is inserted, and appends a Frontier mutation. `removeTriples(iterator, explicit, handler)` deletes, decrements the context count and journals. Commits go through `endTransaction(true)`; when the map is full during a transaction, a `TxnRecordCache` spills the rest and the commit becomes two durable LMDB commits.

Predicate guarantees ("RDF term domains"). For every predicate the triple store persists an `RdfTermDomain`: facts true of all its objects (IRI/literal/with-language/datatype...). The sail calls `recordRdfTermDomain(p, object)` on insert and `recordPredicateObjectRemoval(p, object)` on delete; pending updates are flushed in the commit. A removal can mark a predicate for rebuild (`predicate-object-domain-degradations`, flag `PREDICATE_OBJECT_DOMAIN_REBUILD_REQUESTED`); `rebuildMarkedRdfTermDomains()` rebuilds marked predicates. The classification of a language literal does not depend on the case of its tag.

Frontier Statistics V2. The triple store appends every statement mutation, as ids, to a journal database; `LmdbSailStore` opens `LmdbStatisticsService` (directory `frontier-statistics-v2/`) after the stores and replays the journal as signed deltas, or rebuilds the base when the journal has a gap. Learned sidecars (`join-estimator.rjes*`, classes `LmdbFilterSelectivityStats`, `LmdbOperatorFeedbackStats`) persist the journal sequence they were built at and are discarded on load when it differs. The sketch synopsis (`join-estimator.rjes/synopsis-v8.bin`, `LmdbQuadSynopsisService`) is maintained through the sail sink and is rebuilt when the file `synopsis-v8.rebuild` exists next to it.

## Plan of Work

Milestone 1 (tests first). Add `rdf4j-sail-memory` (test scope) to `core/sail/lmdb/pom.xml`. Add `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbLanguageTagVariantMergeTest.java`: a fixture writes `store.properties` with `language-tag-key=byte-exact`, sets `LmdbLanguageTagVariantMerge.disabledForTesting = true`, and loads statements through `LmdbStore` in three sessions (restarting between them so the value-id cache is cold), each session adding case variants of literals stored before: direct objects in the default and named graphs, a hash-path (long) literal, a group whose first spelling is upper case, a three-way group, triple terms and a nested triple term containing variants, inferred statements (including an inferred statement that becomes identical to an explicit one), and duplicates that collapse. The same statements, in the same order, go into a `MemoryStore`. The fixture asserts that the legacy store really holds more statements than the `MemoryStore`. Tests then enable the merge, open the store and compare explicit and inferred statements, context ids and sizes, and SPARQL results (patterns with every spelling, `FILTER(?o = ...)`, `LANG`, triple-term patterns, `GRAPH`, counts) with the `MemoryStore`, cold and warm, and after another restart; check `language-tag-key=canonical`; check that predicate guarantees equal those of a forced full rebuild; and check that Frontier statistics cover the merge and answer the survivor's leaf count like a forced base rebuild. Crash tests set `LmdbLanguageTagVariantMerge.stepObserverForTesting` to throw at each step (`PLANNED`, `SKETCH_INVALIDATED`, after the first and second `STATEMENT_BATCH_COMMITTED` with one-row batches, `STATEMENTS_REWRITTEN`, `VALUES_MERGED`), expect the open to fail, then reopen without the observer and expect the full, correct result. Add `LmdbLanguageTagVariantMergeStorageTest` that builds the fixture directly on `ValueStore` + `TripleStore` with raw id quads that put a losing literal id in subject, predicate, object and context positions of both planes, runs the merge, and asserts the exact resulting quads and context counts. These tests fail to compile (no `LmdbLanguageTagVariantMerge`) and, once a stub exists, fail on assertions.

Milestone 2 (value-store planning). New file `LanguageTagVariantMergePlan.java` holding `Long2LongOpenHashMap survivorByLoser` (default `LmdbValue.UNKNOWN_ID`), a `resolve(long id)` that follows the map to the final survivor, the losing literal ids, the losing triple terms with their stored components, and the surviving triple terms whose components must be rewritten. In `ValueStore`, `planLanguageTagVariantMerge()` runs inside a write transaction that is rolled back (so lookups share one snapshot): group every language literal with an upper-case tag by its canonical key, add the entry stored under the exact canonical key (if any, and if it is not the entry itself), survivor = smallest id; then iterate triple terms to a fixed point: group triple terms whose resolved components are equal (plus the existing triple term with exactly those components), survivor = smallest id, record component rewrites for survivors whose stored components differ.

Milestone 3 (statement rewrite). `TripleStore.rewriteStatementIds(LongUnaryOperator survivorOf, int batchRows, StatementRewriteObserver observer)`: for the explicit plane, then the inferred plane, collect every quad with a component `x` where `survivorOf(x) != x` (one full scan of the first index in a read transaction), then per batch: start a transaction, insert the rewritten quads (`storeTriple`; skip an inferred quad whose rewrite exists explicitly; call `recordRdfTermDomain` for each inserted quad), delete the originals (`removeTriples` with `recordPredicateObjectRemoval`), commit, notify the observer. At the end, rebuild predicates marked for a guarantee rebuild when auto-rebuild is enabled.

Milestone 4 (value-store merge). `ValueStore.applyLanguageTagVariantMerge(plan)`: one write transaction (after an up-front `resizeMap` sized for the whole step) that rewrites surviving triple terms' index entries (reference counts moved from old to new components, stored hash cleared), deletes losing triple terms' index entries (component counts decremented), deletes losing literals' data-to-id entry (byte-exact key or hash association), id-to-data entry and datatype reference, deletes the losers' own reference-count entries, marks every loser unused, then re-keys all remaining upper-case language literals canonically (the existing re-keying, refactored into `rekeyLanguageTagKeys` returning a result instead of flipping the mode); if re-keying still finds a collision the transaction is rolled back and an `IOException` is thrown. The commit starts a new revision (clears caches). Only after the commit the mode becomes `CANONICAL` and `store.properties` gets `canonical`.

Milestone 5 (coordinator). `LmdbLanguageTagVariantMerge.mergeIfRequired(ValueStore, TripleStore, Path sketchDirectory)`: return if the mode is `CANONICAL` or `disabledForTesting`; plan (`PLANNED`); if the plan has losers, write the sketch rebuild marker when the sketch directory exists (`SKETCH_INVALIDATED`), rewrite statements (`STATEMENT_BATCH_COMMITTED` per batch, `STATEMENTS_REWRITTEN`); apply (`VALUES_MERGED`); log one INFO line with counts. Call it from the `LmdbSailStore` constructor right after `new TripleStore(...)`. Adjust the byte-exact warnings in `ValueStore` to say that `LmdbStore` merges the variants on open. Add `LmdbQuadSynopsisService.markRebuildRequired(Path)`.

## Concrete Steps

Working directory: repository root.

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbLanguageTagVariantMergeTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbLanguageTagVariantMergeStorageTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py ValueStoreLanguageTagKeyMigrationTest --module core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbLanguageTagTermIdentityTest --module core/sail/lmdb
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    (cd scripts && ./checkCopyrightPresent.sh)
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -PskipUnitTests

Run LMDB suites one at a time: the host has few free POSIX semaphores (LMDB takes two per open environment on macOS).

## Validation and Acceptance

`LmdbLanguageTagVariantMergeTest` and `LmdbLanguageTagVariantMergeStorageTest` fail before the change and pass after it. Acceptance: after opening a legacy store with colliding variants, every compared result equals the `MemoryStore`'s, `store.properties` records `language-tag-key=canonical`, no losing id resolves to a value, and every crash point followed by a reopen yields the same final state. Existing language-tag tests stay green; the `core/sail/lmdb` unit suite and ITs show no new failures.

## Idempotence and Recovery

Order of durable effects: (1) sketch rebuild marker file; (2) statement batches in `triples/`, each an LMDB commit (possibly split in two by the record cache, inserts before deletes); (3) the value-store merge in `values/` (one commit unless the value map must grow); (4) `store.properties` saved by `LmdbStore`. The plan is recomputed from `values/` on every open while the mode is byte-exact: after a crash in (1) or (2) the same losers exist, so the same plan results and only the statements not yet rewritten are found; after a crash inside (3) (possible only through an intermediate `resizeMap` commit) the prefix is one of: some surviving triple terms rewritten (they now have the resolved components; the planner groups triple terms stored with identical components, so the not-yet-retired losers are found again), some losing triple terms or literals retired, each as a whole - entries deleted, marked unused, hash cleared - so an id is never deleted without being freed later (they are no longer referenced by any statement or triple term, since (2) finished and triple terms are processed before literals), some entries re-keyed (re-keying skips entries already found under their canonical key); reference-count deltas of such a prefix are lost, as for any value-store transaction a resize commits early; after a crash between (3) and (4) the value store has no losers, so the next open plans nothing, re-keys nothing and records `canonical`. If `store.properties` never recorded a policy, the `ValueStore` constructor's own scan reaches the same conclusions. Nothing is rolled back; the migration always rolls forward. Back up the store directory before opening a legacy store with a new version if you want to keep the byte-exact layout.

## Artifacts and Notes

Red before the change (`initial-evidence.lang-merge.txt`):

    LmdbLanguageTagVariantMergeStorageTest: tests=2, failures=2 (expected: <1> but was: <0>)
    LmdbLanguageTagVariantMergeTest: tests=11, failures=11 (expected: <CANONICAL> but was: <BYTE_EXACT>; crash not reached)
    ValueStoreTest#testGcTripleTermsAfterRestart: NullPointerException ... "this.tripleTermCspoIndex" is null

Green after (`post-evidence.lang-merge-focused.txt`, `post-evidence.lang-merge-lmdb-unit.txt`):

    LmdbLanguageTagVariantMergeTest: tests=15, failures=0, errors=0
    related selection (merge tests, ValueStoreLanguageTagKeyMigrationTest, LmdbLanguageTagTermIdentityTest,
      ValueStoreLanguageTagIdentityTest, ValueStoreTest): 83 green
    core/sail/lmdb unit suite: 2693 run, 0 failures, 1 error (LmdbStoreMultiRepoTest, No space left on device)
    core/sail/lmdb ITs: 144 run, 0 failures, 0 errors, 98 skipped

## Interfaces and Dependencies

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLanguageTagVariantMerge.java` (package-private, final):

    enum Step { PLANNED, SKETCH_INVALIDATED, STATEMENT_BATCH_COMMITTED, STATEMENTS_REWRITTEN, VALUES_MERGED }
    interface StepObserver { void reached(Step step) throws IOException; }
    static volatile StepObserver stepObserverForTesting;
    static volatile boolean disabledForTesting;
    static volatile int statementBatchRowsForTesting; // 0 = default
    record Outcome(int variantGroups, int mergedValueIds, long rewrittenStatements) { }
    static Outcome mergeIfRequired(ValueStore valueStore, TripleStore tripleStore, Path sketchDirectory) throws IOException

In `LanguageTagVariantMergePlan.java` (package-private, final): `boolean isEmpty()`, `long resolve(long id)`, `int variantGroups()`, `int loserCount()`.

In `ValueStore`: `LanguageTagVariantMergePlan planLanguageTagVariantMerge() throws IOException`, `void applyLanguageTagVariantMerge(LanguageTagVariantMergePlan plan) throws IOException`.

In `TripleStore`: `interface StatementRewriteObserver { void batchCommitted(long rewrittenSoFar) throws IOException; }`, `long rewriteStatementIds(LongUnaryOperator survivorOf, int batchRows, StatementRewriteObserver observer) throws IOException`.

In `org.eclipse.rdf4j.sail.lmdb.estimation.LmdbQuadSynopsisService`: `public static void markRebuildRequired(Path directory) throws IOException`.

Revision note (2026-09-29): recorded the implementation outcome, the two fixed side defects (reopen NPE for unused triple terms, orphaned duplicate triple term after an early commit), the switch to per-id retirement, the sketch configuration gate, and verification results.
