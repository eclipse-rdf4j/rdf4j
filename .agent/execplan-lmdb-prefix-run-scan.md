# Implement LMDB Prefix-Run Scans for DISTINCT, REDUCED, and GROUP BY

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

LMDB statement indexes already store statements in lexicographic order by index component, for example predicate, object, subject, context for `POSC`. Queries such as `SELECT DISTINCT ?p WHERE { ?s ?p ?o }` only need one row per predicate prefix, but the current native row path still walks every matching statement and removes duplicates afterward. This change adds a package-private prefix-run cursor that emits or aggregates once for each 1-to-3-component prefix run and jumps to the next prefix with `MDB_SET_RANGE`. A user can see the behavior by running the new LMDB tests: they pass only when supported distinct and grouping queries use the prefix-run path and still return the same public query results.

## Progress

- [x] (2026-07-08 01:20+02:00) Captured baseline root quick install and stored compact evidence in `initial-evidence.txt`.
- [x] (2026-07-08 01:27+02:00) Inspected `TripleIndex`, `TripleStore`, `LmdbRecordIterator`, `NativeLmdbQuerySource`, `LmdbSailStore`, and native aggregate/row planning surfaces.
- [x] (2026-07-08 01:36+02:00) Added failing storage and query tests for the prefix-run contract.
- [x] (2026-07-08 01:44+02:00) Implemented package-private storage-level prefix-run plan and iterator.
- [x] (2026-07-08 01:52+02:00) Exposed prefix-run scans through LMDB native query sources, including single-active-source composite delegation.
- [x] (2026-07-08 02:18+02:00) Wired native DISTINCT, REDUCED, GROUP BY, `COUNT(*)`, and supported `COUNT(DISTINCT ...)` specializations.
- [x] (2026-07-08 02:30+02:00) Fixed unsafe bound-predicate prefix planning discovered by differential fuzz.
- [x] (2026-07-08 02:59+02:00) Verified focused tests, differential fuzz, formatting, copyright, and whitespace checks. Full LMDB module surefire passed; failsafe was stopped during long `LmdbThemeQueryRegressionIT`.
- [x] (2026-07-08 03:08+02:00) Re-verified the final formatted tree with a combined focused selector covering prefix storage, prefix query, differential fuzz, and record-iterator skip-scan tests.

## Surprises & Discoveries

- Observation: `LmdbNativeAggregatePlanner.compileRowRoot` currently treats `Reduced` as a plain stream and only hashes `Distinct` inside `NativeRowsStep.evaluateAll`.
  Evidence: `compileRowRoot` unwraps `Reduced` before projection planning, while `NativeRowsStep.evaluateAll` performs a per-row `GroupKey` hash check for `distinct`.
- Observation: Statement-level scan metrics are already available through `RecordIterator` (`getSourceRowsScannedActual`, `getSourceRowsMatchedActual`, and `getSourceRowsFilteredActual`), so tests can prove scan reduction without changing public RDF4J APIs.
  Evidence: `LmdbRecordIterator` exposes these counters and `NativeRowsStep` already reports source metrics through `NativeQueryEvaluationStatistics`.
- Observation: Normal repository evaluation can present explicit and inferred branches as a `CompositeNativeLmdbQuerySource`; the inferred branch can be inactive, so blindly treating every composite as unsupported hides prefix-run opportunities.
  Evidence: The first query test reached `LmdbNativeAggregatePlanner.tryPrefixRunPlan`, but `TripleStore.prefixRunPlan` was not called until `CompositeNativeLmdbQuerySource` delegated prefix-runs for exactly one active source.
- Observation: A bound predicate after a run key can make a prefix run contain mixed predicates. `LmdbNativeDifferentialFuzzTest` caught both an empty `SELECT DISTINCT * WHERE { ?b <p5> ?c }` result and incorrect `GROUP BY ?a` counts for `{ ?c <p3> ?a }`.
  Evidence: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` first failed in `LmdbNativeDifferentialFuzzTest.randomBasicGraphPatterns` and `randomAggregates`; after tightening index eligibility, `LmdbNativeDifferentialFuzzTest` passed.
- Observation: `TripleIndex.keyToQuadMatchStatus` used by both the old record iterator and the new prefix-run iterator returned before storing the mismatching decoded field. That left scratch quads vulnerable to stale prefix data when a cursor crossed a bound prefix boundary.
  Evidence: Review after the fuzz fix found `countCurrentRun()` calling `samePrefix(scratchQuad, quad)` after a rejected key; assigning the decoded field before returning keeps the scratch key representative, and `LmdbRecordIteratorSkipScanTest` still passes.

## Decision Log

- Decision: Keep the prefix-run API package-private and guard automatic planning with system property `rdf4j.lmdb.prefixRun.enabled=false`.
  Rationale: The feature is LMDB-internal and should be easy to disable if a correctness or performance regression appears.
  Date/Author: 2026-07-08 / Codex
- Decision: V1 only supports single-statement native row/group roots where the projected or grouped variables match leading components of a configured statement index after constants are accounted for.
  Rationale: Joins, computed projections, filters, ordering, composite sources, and unsafe repeated-variable patterns have more semantics to validate and can continue using the existing native or generic fallback path.
  Date/Author: 2026-07-08 / Codex
- Decision: Use an interpreted cursor over LMDB keys rather than generated code or new dependencies.
  Rationale: The statement key format already preserves lexicographic numeric order, and the cursor can seek to the next prefix by writing the next target key into an existing `MDB_val`.
  Date/Author: 2026-07-08 / Codex
- Decision: Delegate prefix-run methods through `CompositeNativeLmdbQuerySource` only when exactly one member source reports statements.
  Rationale: This preserves fallback for true multi-source composites while allowing the common explicit-only repository case, where the inferred source is inactive, to use native prefix-runs.
  Date/Author: 2026-07-08 / Codex
- Decision: Reject indexes where a non-context bound constant appears after the prefix run key has started.
  Rationale: Such indexes can interleave unrelated statements inside a prefix run and make one-row emission or run multiplicity unsafe. A later phase can support them with a more sophisticated skip/count cursor, but V1 stays conservative.
  Date/Author: 2026-07-08 / Codex
- Decision: Populate decoded quad fields before `keyToQuadMatchStatus` returns a mismatch status.
  Rationale: Callers that inspect the partially decoded key after a mismatch need the actual mismatching field to detect prefix boundaries reliably.
  Date/Author: 2026-07-08 / Codex

## Outcomes & Retrospective

The implementation now has a package-private LMDB prefix-run layer and native planner specializations for single-statement `DISTINCT`, `REDUCED`, `GROUP BY`, grouped `COUNT(*)`, and scalar `COUNT(DISTINCT slot)` when a configured index can safely serve the requested prefix. Unsupported shapes fall back through the existing native or generic evaluator. The main lesson from verification was that index eligibility must be conservative around bound constants: constants before the run key are useful, but non-context constants after the run key are unsafe in V1.

## Context and Orientation

The affected module is `core/sail/lmdb`. LMDB statement data is stored by `org.eclipse.rdf4j.sail.lmdb.TripleStore`, with each configured index represented by `org.eclipse.rdf4j.sail.lmdb.TripleIndex`. An index order such as `POSC` means predicate first, object second, subject third, context fourth. A prefix run is the contiguous range of LMDB keys that share the same leading one, two, or three index components. For example, all statements with the same predicate are one prefix run in `POSC` when the prefix length is one.

The native query engine enters LMDB-specific planning through `LmdbNativeAggregateCompiler` and `LmdbNativeAggregatePlanner`. Single statement patterns are represented by `PatternPlan`; row projection is executed by `LmdbNativeRowStep.NativeRowsStep`; grouping is executed by `LmdbNativeGroupStep.NativeGroupStep`. `NativeLmdbQuerySource` is the storage abstraction used by the query planner. `LmdbSailStore` implements that source for normal snapshots and parallel snapshots; `SyntheticValueSource` wraps another source when tests or benchmark paths inject synthetic IDs; `CompositeNativeLmdbQuerySource` combines multiple sources and should not use this V1 prefix-run path.

`RecordIterator` already has scan counters. The prefix-run cursor should provide similar accounting so execution statistics and tests can prove that supported queries do not scan every statement in a run.

## Plan of Work

First add tests. `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbPrefixRunIteratorTest.java` will exercise the storage-level cursor over `POSC` for prefix lengths one, two, and three, plus carry behavior when a prefix component overflows and the next seek must carry into the previous component. `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbPrefixRunQueryTest.java` will exercise the query planner for `DISTINCT`, `REDUCED`, `GROUP BY`, `COUNT(*)`, `COUNT(DISTINCT ?p)`, and missing-index fallback. These tests should fail before the production implementation.

Next implement `LmdbPrefixRunPlan` and `LmdbPrefixRunIterator` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. The plan selects a configured `TripleIndex`, stores the prefix component mapping, and exposes package-private counters for tests. The iterator opens the selected LMDB database, seeks to the first matching prefix, validates any bound constants or repeated fields, emits one representative quad, optionally scans the run to compute multiplicity, then seeks to the next lexicographic prefix target.

Then expose the cursor through `TripleStore` and `NativeLmdbQuerySource`. Normal and parallel `LmdbSailStore` sources delegate to `TripleStore`; `SyntheticValueSource` delegates only when no synthetic ID participates; `CompositeNativeLmdbQuerySource` delegates only when exactly one member source has statements, otherwise the native planner falls back.

Then update `LmdbNativeAggregatePlanner`, `NativeRowsStep`, and `NativeGroupStep` to request prefix plans only for safe shapes. For row roots, `DISTINCT` and `REDUCED` over a single statement pattern may use one representative row per prefix when there is no `ORDER BY` and projected fields are exactly the requested prefix variables. For grouping, no-aggregate grouping can emit one row per prefix, `COUNT(*)` scans each run for multiplicity, and scalar `COUNT(DISTINCT ?p)` can count prefix runs. Unsupported aggregates and all semantically risky shapes remain on the existing path.

Finally run the focused new tests with `mvnf`, run formatting/resource processing, run `git diff --check`, and broaden to `core/sail/lmdb` verify if practical.

## Concrete Steps

Work from `/Users/havardottestad/Documents/Programming/rdf4j`.

The baseline command already run was:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

The first red verification should run:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPrefixRunIteratorTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPrefixRunQueryTest --module core/sail/lmdb --retain-logs

After implementation, rerun the same selectors. If they pass, run:

    ./scripts/checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    git diff --check

Do not run Maven tests with `-am` or `-q`.

## Validation and Acceptance

Acceptance is both semantic and behavioral. The public SPARQL query results must match existing semantics for supported and fallback cases. The storage tests must show one representative row per prefix for `?p`, `?p ?o`, and `?p ?o ?s` over `POSC`, and must show correct carry behavior when the last prefix component cannot be incremented. The query tests must show that `DISTINCT`, `REDUCED`, `GROUP BY`, `COUNT(*)`, and `COUNT(DISTINCT ?p)` produce the expected results. Tests must also show that a repository configured without the needed prefix index still returns correct results through fallback.

## Idempotence and Recovery

All edits are additive or narrowly scoped. The kill switch `rdf4j.lmdb.prefixRun.enabled=false` should force existing execution paths without changing public results. If a focused test fails after implementation, inspect the Surefire report under `core/sail/lmdb/target/surefire-reports`, fix the smallest failing behavior, and rerun the same selector. Do not delete untracked artifacts or unrelated benchmark changes in the working tree.

## Artifacts and Notes

Initial evidence was saved at repository root:

    initial-evidence.txt

Focused passing evidence:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPrefixRunIteratorTest --module core/sail/lmdb --retain-logs
    Summary: tests=4, failures=0, errors=0, skipped=0

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPrefixRunQueryTest --module core/sail/lmdb --retain-logs
    Summary: tests=10, failures=0, errors=0, skipped=0

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest --module core/sail/lmdb --retain-logs
    Summary: tests=6, failures=0, errors=0, skipped=0

Final combined focused evidence on the formatted tree:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs -Dtest=LmdbPrefixRunIteratorTest,LmdbPrefixRunQueryTest,LmdbNativeDifferentialFuzzTest,LmdbRecordIteratorSkipScanTest
    Summary: tests=23, failures=0, errors=0, skipped=0

Broad verification note:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    Surefire reached: Tests run: 1439, Failures: 0, Errors: 0, Skipped: 3
    Failsafe before stop reached: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
    The run was stopped by the agent during long-running LmdbThemeQueryRegressionIT and Maven reported exit code 143.

Current unrelated local dirt to preserve:

    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java

## Interfaces and Dependencies

No public RDF4J API changes and no new dependencies are allowed. New classes stay package-private under `org.eclipse.rdf4j.sail.lmdb`. The expected package-private interfaces are:

    final class LmdbPrefixRunPlan {
        static final String ENABLED_PROPERTY = "rdf4j.lmdb.prefixRun.enabled";
        boolean isEnabled();
        TripleIndex index();
        int prefixLength();
    }

    final class LmdbPrefixRunIterator implements AutoCloseable {
        boolean next() throws IOException;
        long[] quad();
        long runRowCount();
        long getSourceRowsScannedActual();
        long getSourceRowsMatchedActual();
    }

`TripleStore` should expose package-private helpers for selecting and opening plans. `NativeLmdbQuerySource` should expose default unsupported methods so non-LMDB and composite sources remain fallback-safe.

Revision note 2026-07-08 / Codex: Created the initial self-contained ExecPlan before adding failing tests, as required for this significant LMDB native-engine refactor.

Revision note 2026-07-08 / Codex: Updated progress, discoveries, decisions, and evidence after implementing prefix-run scans and fixing the bound-predicate safety issue found by differential fuzz.

Revision note 2026-07-08 / Codex: Added final combined focused verification and recorded the matcher scratch-quad safety fix found during closeout review.
