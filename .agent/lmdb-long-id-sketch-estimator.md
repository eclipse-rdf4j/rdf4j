# LMDB Long-ID Sketch Estimator Rewrite

This ExecPlan is a living document. It follows `.agent/PLANS.md` and must stay self-contained as work proceeds.

## Purpose / Big Picture

RDF4J's LMDB sketch join estimator currently hashes RDF term strings while LMDB already stores every IRI, blank node,
literal, and context as a compact `long` value ID. After this change, sketch ingestion, lookup, cache keys, and LMDB
incremental updates use those `long` IDs directly. A user can observe the result by adding LMDB statements containing
IRIs, blank nodes, stored literals, inlined literals, and default graph statements, then querying estimator
cardinalities with `ValueStore` IDs. Old string-keyed persisted estimator snapshots are rejected and rebuilt.

## Progress

- [x] (2026-05-20 11:58Z) Baseline quick install passed and `initial-evidence.txt` captured.
- [ ] Add long-ID core and LMDB tests that describe the new contract.
- [ ] Replace `SketchStatementSource` with ID-native scan and value resolution methods.
- [ ] Rewrite estimator ingestion, hashing, cache keys, and churn sampling to use primitive IDs.
- [ ] Replace LMDB statement materialization in estimator rebuild/add/delete with raw quad IDs.
- [ ] Bump persisted metadata compatibility so string-keyed snapshots rebuild.
- [ ] Run formatter, targeted tests, and LMDB module verification.

## Surprises & Discoveries

- Observation: The current estimator still queues `Statement` objects, computes `stringValue()` for S/P/O/C, and scans
  `Statement` objects for exact fallbacks.
  Evidence: `SketchBasedJoinEstimator.rebuild()` calls `statementSource.getStatements(...)` and
  `toIngestEvent(str(...))`; incremental batches call `str(statement.getSubject())` and related methods.
- Observation: LMDB add and remove paths already have quad IDs at the point the estimator is called, but the remove
  path materializes a `Statement` only for estimator deletion.
  Evidence: `LmdbSailStore.AddQuadOperation` stores `long s,p,o,c`; `removeStatements(long...)` receives a raw
  `quad` and calls `queueEstimatorRemove(quadToStatement(quad))`.

## Decision Log

- Decision: Make this a hard cutover from string keys to primitive `long` IDs; do not retain string-keyed public sketch
  APIs.
  Rationale: Keeping both key domains risks silently mixing incompatible persisted sketches and defeats the allocation
  and equality benefits of LMDB IDs.
  Date/Author: 2026-05-20 / Codex.
- Decision: Use `SketchStatementSource.UNBOUND_ID` for wildcard pattern components and
  `SketchStatementSource.DEFAULT_CONTEXT_ID` for default graph context.
  Rationale: LMDB uses `-1` as its wildcard sentinel internally, while `0` is the default context ID. The estimator
  needs its own stable wildcard sentinel that cannot be confused with a real term.
  Date/Author: 2026-05-20 / Codex.
- Decision: Bucket selection must mix the full `long` ID before modulo.
  Rationale: LMDB IDs are sequential and type-encoded, so `id % bucketCount` would cluster related IDs and make sketch
  accuracy depend on allocation order.
  Date/Author: 2026-05-20 / Codex.

## Outcomes & Retrospective

Not complete yet. The intended outcome is a compiling codebase where the estimator can rebuild from LMDB raw quads,
incrementally update from raw add/delete IDs, reject old metadata, and pass focused core and LMDB tests.

## Context and Orientation

The estimator lives in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`.
`SketchBasedJoinEstimator` maintains tuple sketches for single components, component complements, and component pairs.
Before this work, its public lookups accepted strings and all ingestion converted RDF4J `Value` objects to
`stringValue()`.

`SketchStatementSource` is the estimator's storage boundary. It currently returns RDF4J `Statement` objects. This plan
changes it to return immutable `StatementIds` records containing subject, predicate, object, and context IDs. A
wildcard pattern component is `UNBOUND_ID`; default graph context is `DEFAULT_CONTEXT_ID`.

The LMDB store integration lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`.
The store already stores triples as quad IDs in `TripleStore`, and `RecordIterator.next()` returns a reusable
`long[]`. Any estimator iterator that exposes those records must copy the four values before returning them.

## Plan of Work

First, add tests that compile only against the new ID-native API. Then change `SketchStatementSource` and the test
stub to support deterministic value IDs. Next, update `SketchBasedJoinEstimator` so rebuild, incremental add/delete,
hashing, pattern cache keys, and churn sampling all carry primitive IDs. Query-planning methods will resolve RDF4J
`Value` constants once at the algebra boundary through `statementSource.idOf(...)`; an unresolved bound constant
means the pattern has zero rows.

After the core compiles, update LMDB integration. `GuardedEstimatorStatementSource` will scan `TripleStore.getTriples`
directly under the existing estimator guard lock, map the estimator wildcard to `LmdbValue.UNKNOWN_ID`, and return
copied `StatementIds`. Add and remove callbacks will pass stored quad IDs directly to `addStatementIds(...)` and
`deleteStatementIds(...)`.

Finally, bump metadata compatibility so v1/v2 metadata is incompatible with long-ID sketches. Rewrite tests and
benchmarks that call string-keyed APIs so they use source or `ValueStore` IDs.

## Concrete Steps

Run from `/Users/havardottestad/Documents/Programming/rdf4j`.

1. Maintain the baseline install before tests:
   `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`
2. Run focused core work:
   `python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorLongIdTest --retain-logs --stream`
3. Run focused LMDB work:
   `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSailStoreEstimatorLongIdTest --retain-logs --stream`
4. Run existing regression:
   `python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorRebuildParityTest --retain-logs --stream`
5. Broaden:
   `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream`

## Validation and Acceptance

The new core test must prove rebuild and incremental ingestion agree when addressed by long IDs, delete tombstones
zero out an ID cardinality, and default context ID `0` is distinct from named context IDs. The LMDB test must prove
the estimator can answer cardinalities using `ValueStore.getId(...)` for IRI, blank node, stored literal, and inlined
literal terms, and that deleting a statement updates the ID-keyed sketches without materializing strings.

Acceptance also requires `rg -n "cardinalitySingle\\([^,]+, *[^0-9a-zA-Z_]"` style spot checks or equivalent review to
confirm no public sketch API still keys on strings, and `SketchEstimatorMetadata` must reject old string-keyed versions.

## Idempotence and Recovery

The tests and source changes are ordinary code edits and can be rerun safely. If persistence loading fails for an old
snapshot, the estimator already treats incompatible snapshots as rebuildable and resets the store. If a test run fails
because offline dependencies are missing, rerun the same command once without `-o`, then return to offline commands.

## Artifacts and Notes

Initial quick install succeeded on 2026-05-20 and the last 200 lines are in `initial-evidence.txt`.

## Interfaces and Dependencies

At completion, `SketchStatementSource` defines:

    long UNBOUND_ID = Long.MIN_VALUE;
    long DEFAULT_CONTEXT_ID = 0L;
    CloseableIteration<StatementIds> getStatementIds(long subjectId, long predicateId, long objectId, long contextId);
    OptionalLong idOf(SketchBasedJoinEstimator.Component component, Value value);
    record StatementIds(long subject, long predicate, long object, long context) {}

`SketchBasedJoinEstimator` exposes long-keyed lookup methods:

    double cardinalitySingle(Component component, long valueId);
    double cardinalityPair(Pair pair, long leftId, long rightId);
    JoinEstimate estimate(Component joinVar, long subjectId, long predicateId, long objectId, long contextId);
    double estimateCount(Component joinVar, long subjectId, long predicateId, long objectId, long contextId);
    double estimateJoinOn(Component join, Pair a, long ax, long ay, Pair b, long bx, long by);
    double estimateJoinOn(Component join, Component a, long av, Component b, long bv);

No new dependencies are required.
