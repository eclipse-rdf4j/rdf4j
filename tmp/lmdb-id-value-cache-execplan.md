# Implement Two-Level LMDB ID-to-Value Cache

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows [PLANS.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/PLANS.md) from the repository root and must be maintained in accordance with that file.

## Purpose / Big Picture

After this change LMDB value lookup in RDF4J will have an on-disk, append-only cache for `id -> raw value bytes` similar in spirit to the new persistent hash cache. The goal is to let `ValueStore.getValue(...)`, lazy `resolveValue(...)`, and namespace lookup avoid an LMDB `ID_KEY -> value` fetch when the cache already knows where the serialized bytes live. A user will not see a new API, but after restart the store should still resolve values correctly even if a test forces `getData(id)` to fail, proving the new first-level index file and second-level data file are serving the bytes.

## Progress

- [x] (2026-03-11 23:39Z) Read `PLANS.md`, confirm Routine D, inspect `ValueStore`, and choose a red-test strategy that proves the cache path without corrupting LMDB files.
- [x] (2026-03-12 06:27Z) Add focused tests in `ValueStoreTest` for eager lookup, lazy resolution, and recycled IDs using a `ThrowingValueStore` subclass whose `getData(id)` throws if the old LMDB path is touched.
- [x] (2026-03-12 06:28Z) Capture the first red evidence in `/Users/havardottestad/Documents/Programming/rdf4j-stf/initial-evidence.txt` from `ValueStoreTest#testGetValueUsesPersistentDataCacheAfterRestart`.
- [x] (2026-03-12 06:33Z) Implement `ValueStoreDataFiles` plus `ValueStore` staging, lazy-fill reads, write-path seeding, and recycled-ID clearing for the new two-level cache.
- [x] (2026-03-12 06:34Z) Re-run `ValueStoreTest#testGetValueUsesPersistentDataCacheAfterRestart` and observe it pass green.
- [x] (2026-03-12 06:33Z) Re-run `ValueStoreTest` and observe all 12 focused low-level tests pass.
- [x] (2026-03-12 06:34Z) Re-run `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` and observe module verify pass with 823 Surefire tests and 1 Failsafe IT.
- [x] (2026-03-12 06:35Z) Run `./checkCopyrightPresent.sh` and `mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources` after the final code changes.

## Surprises & Discoveries

- Observation: `ValueStore.getValue(...)`, `ValueStore.resolveValue(...)`, and `ValueStore.getNamespace(...)` all funnel through `getData(id)`, so one throwing test subclass can prove whether the new cache truly bypasses the LMDB `ID_KEY -> value` lookup.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` uses `getData(id)` in `getValue`, `resolveValue`, and `getNamespace`.

- Observation: namespace IDs are also stored in the same LMDB `ID_KEY -> data` space, so the new cache must cover namespace entries as well as ordinary values or IRI resolution will still hit LMDB.
  Evidence: `data2uri(...)` calls `getNamespace(nsID)`, and `getNamespace(...)` currently calls `getData(id)`.

- Observation: the smallest eager selector fails immediately in `ValueStore.getValue(...)`, so the red test is tight enough to detect any accidental fallback to the old LMDB path.
  Evidence: `initial-evidence.txt` shows `old LMDB ID->value path used for id 8` from `ValueStore.getValue(ValueStore.java:605)`.

## Decision Log

- Decision: implement the feature as a lazy-fill cache, not a startup migration.
  Rationale: this matches the persistent hash cache behavior, keeps reopen fast, and lets upgraded stores populate the new files only when values are actually read or written.
  Date/Author: 2026-03-11 / Codex

- Decision: use a fixed-width first-level index record keyed directly by encoded LMDB ID and an append-only second-level byte file for the serialized value bytes.
  Rationale: the user requested a two-level design where the ID acts as the offset into the first level; a fixed-width index keeps lookup O(1), while an append-only data file avoids moving existing payloads when IDs are added.
  Date/Author: 2026-03-11 / Codex

- Decision: stage cache writes and clears in memory until the surrounding LMDB write transaction commits.
  Rationale: the new files must not advertise values or recycled-ID clears that were rolled back in LMDB.
  Date/Author: 2026-03-11 / Codex

## Outcomes & Retrospective

The two-level cache is implemented and proven by tests that disable the old LMDB `getData(id)` path after restart. `ValueStore` now prefers the on-disk cache for eager lookup, lazy resolution, and namespace expansion, while still falling back to LMDB for upgraded stores and immediately backfilling the new files. Recycled IDs clear both the hash cache and the new value cache, so reused IDs return the new payload rather than stale bytes.

The red-to-green path was tight and useful. The first failing selector proved the old `getValue(...)` path still depended on LMDB, and the full module verify stayed green after the cache wiring. No additional compatibility bridge beyond the existing serialized-revision null handling was needed.

## Context and Orientation

The relevant code lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`. `ValueStore` is the low-level object that translates RDF model objects into compact byte arrays, stores those arrays in LMDB, and resolves IDs back into model objects. The current LMDB layout stores both `value -> id` and `id -> value` mappings in the same database. `getValue(long id)` and lazy `resolveValue(long id, LmdbValue value)` decode stored bytes back into `LmdbIRI`, `LmdbLiteral`, and `LmdbBNode`. `getNamespace(long id)` does the same for namespace IDs so IRI values can be rebuilt.

The new persistent hash cache already exists in:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreHashFile.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`

That work is the template for transaction staging, file lifecycle, and lazy-fill behavior. The new `id -> value bytes` cache should be parallel in spirit, but its payload is variable-length. The user requested two levels. In this plan, “first level” means a fixed-width memory-mapped index file where slot `id * recordSize` stores the offset and length of the payload. “Second level” means a separate append-only file that stores the raw serialized bytes exactly as `value2data(...)` and `namespaceData` already produce them.

The main tests live in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreTest.java`. That class already contains focused low-level tests for ID recycling and the persistent hash cache. It is the best place to add new tests because it can reopen the same value-store directory and inspect lazy state directly.

## Plan of Work

Start with the failing tests. Extend `ValueStoreTest` with a private test subclass of `ValueStore` that overrides `getData(long id)` so it throws or records usage when the old LMDB path is touched. Use that subclass only after a normal store/reopen cycle so the new files exist on disk. Add one eager lookup test that stores an IRI, reopens with the throwing subclass, and asserts `getValue(id)` still returns the IRI without calling `getData`. Add a lazy lookup test that stores an IRI or literal, reopens with the throwing subclass, asks a lazy value for its string form, and asserts resolution succeeds without the old path. Add a recycled-ID test that stores value A, forces cache population, garbage-collects A’s ID, stores value B that reuses the same ID, reopens with the throwing subclass, and proves the lookup returns B rather than stale bytes for A.

Then add the new storage helper in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. One helper class should own both files so lifecycle stays centralized. The first file needs a fixed record layout keyed by encoded ID. Use `long` for the data-file offset and `int` for byte length; reserve the remaining bytes in the record for alignment. The second file is append-only and stores the exact byte arrays already used for LMDB `id -> value` mappings. Expose methods to read the cached bytes for one ID, append new bytes and publish the index slot, clear one ID’s slot, force both files, delete both files, and reopen from existing contents after restart.

Wire that helper into `ValueStore`. Add staged pending updates and pending clears, mirroring the hash-cache logic. `getValue`, `resolveValue`, and `getNamespace` should first ask the new helper for raw bytes. If present, decode from those bytes directly. If absent, fall back to the old LMDB `getData(id)` path and immediately cache the bytes so upgraded stores fill lazily. Seed the new cache on write paths too: after `getId(value, create)` or `getNamespaceID(namespace, create)` returns an ID, store the serialized bytes if the cache slot is still empty. When `freeUnusedIdsAndValues(...)` recycles an ID, clear both the hash slot and the new data slot so reused IDs cannot resolve stale bytes. Delete the new files during `clear()` and close them during `close()`. If an on-disk cache read or write fails, log and recreate the cache files empty so correctness falls back to LMDB instead of failing lookups.

Finish by re-running the same selectors that failed first, then the `core/sail/lmdb` module. If the module flushes out another path that can legitimately have no live `ValueStore` reference after serialization, record the discovery here and make the cache path degrade gracefully the way the hash-cache bridge now does.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j-stf` run the following during implementation:

1. Add the failing tests in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreTest.java` with `apply_patch`.
2. Run the smallest red selector, likely:

    `python3 .codex/skills/mvnf/scripts/mvnf.py ValueStoreTest#testGetValueUsesPersistentDataCacheAfterRestart --module core/sail/lmdb --retain-logs`

3. Copy the first failing report snippet into `/Users/havardottestad/Documents/Programming/rdf4j-stf/initial-evidence.txt`.
4. Implement the new helper class and `ValueStore` wiring.
5. Re-run the same selector until it passes.
6. Re-run any other focused selectors added for lazy resolution and recycled IDs.
7. Run the full module:

    `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs`

8. Run the formatter and copyright check:

    `cd /Users/havardottestad/Documents/Programming/rdf4j-stf/scripts && ./checkCopyrightPresent.sh`

    `cd /Users/havardottestad/Documents/Programming/rdf4j-stf && mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources`

Expected proof at the end is:

- a focused eager-lookup test passes while `getData(id)` is disabled;
- a focused lazy-resolution test passes while `getData(id)` is disabled;
- a recycled-ID test proves stale bytes are not returned after ID reuse;
- the full `core/sail/lmdb` module verify stays green.

## Validation and Acceptance

Acceptance is behavioral. After storing a value and restarting the same LMDB value store, a test should be able to reopen a `ValueStore` variant whose `getData(id)` method always throws and still resolve the value by ID. That proves the new two-level on-disk cache is serving bytes instead of the old LMDB `ID_KEY -> value` lookup. The same must hold for lazy value initialization through `resolveValue(...)`. After recycling an ID and reusing it for a different value, the same throwing-store test must resolve the new value, not stale bytes from the old value.

Automated acceptance is:

- the new focused test fails before the implementation and passes after;
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` succeeds;
- `ValueStoreTest`, `LmdbStoreConnectionTest`, and `LmdbStoreConsistencyIT` remain green because those are sensitive to value-resolution regressions and restart behavior.

## Idempotence and Recovery

The plan is additive. Re-running the focused tests is safe because they use temporary directories. If the new cache files become inconsistent during development, delete only the temp-directory store created by the test and re-run; do not mutate repository data. If the helper’s read/write code hits an `IOException`, the production path should recreate the cache files empty and continue falling back to LMDB, so retrying the same test after the fix should be safe.

## Artifacts and Notes

Important reference paths:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreHashFile.java`
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreTest.java`
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreCacheTest.java`

The red-test trick is intentional and should stay local to tests. It proves the new cache path directly:

    class ThrowingValueStore extends ValueStore {
        @Override
        protected byte[] getData(long id) throws IOException {
            throw new AssertionError("old LMDB ID->value path used");
        }
    }

If that class breaks focused tests before implementation and passes after, the cache path is demonstrably real.

## Interfaces and Dependencies

Define these internal types and helpers by the end of the work:

- `org.eclipse.rdf4j.sail.lmdb.ValueStoreDataFiles` (or an equivalently clear helper name)
  - owns the first-level index file and second-level byte file
  - supports `get(long id)`, `put(long id, byte[] data)`, `clear(long id)`, `delete()`, `close()`

- New `ValueStore` helpers for:
  - reading cached bytes for one ID;
  - staging cached-byte writes and clears until transaction commit;
  - seeding the cache from write paths;
  - clearing cache entries when IDs are recycled;
  - lazy-filling the cache from LMDB reads when an upgraded store has no entry yet.

No new external dependency is needed. Reuse Java NIO file channels and memory-mapped buffers, mirroring the existing persistent hash cache.

Revision note: created this ExecPlan at implementation start to satisfy Routine D before any production edits for the new two-level ID-to-value cache.
Revision note: updated after the first red selector failed and its evidence was persisted, so the current document reflects the real starting point for implementation.
Revision note: updated after implementation completed so the progress log, outcome summary, and verification steps match the shipped code.
