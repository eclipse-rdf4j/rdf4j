# Isolate LMDB native query execution as experimental

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB branch contains a new native query planner and execution pipeline mixed into the established `org.eclipse.rdf4j.sail.lmdb` source directory. After this change, that implementation lives under the visibly separate `org.eclipse.rdf4j.sail.lmdb.experimental` package, and the package is annotated with RDF4J's `@Experimental` marker. Maintainers can recognize the unstable implementation boundary from both its filesystem path and generated API metadata, while existing LMDB query behavior remains unchanged.

## Progress

- [x] (2026-07-11 09:45Z) Compared the branch with `origin/main` and identified the newly added native-query implementation and its integration points.
- [x] (2026-07-11 09:45Z) Ran the mandatory root quick clean install; all reactor modules succeeded.
- [x] (2026-07-11 12:35Z) Moved 44 cohesive native-query source files into the experimental source subdirectory without changing their Java package.
- [x] (2026-07-11 12:35Z) Annotated all 111 top-level types in the subdirectory with `@Experimental`.
- [x] (2026-07-11 12:37Z) Formatted sources, passed focused native verification, and passed the final LMDB quick compile.
- [x] (2026-07-11 12:37Z) Audited the final layout and recorded the module-suite limitation.

## Surprises & Discoveries

- Observation: The existing LMDB root package describes the whole store as experimental in Javadoc, but its `package-info.java` does not use `org.eclipse.rdf4j.common.annotation.Experimental`.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/package-info.java` contains only Javadoc and the package declaration.
- Observation: Most native planner types are package-private and share package-private collaborators, so moving only filenames containing `Native` would split a tightly coupled implementation and force unnecessary public visibility.
  Evidence: `LmdbNativeSlotPlan.java`, `LmdbNativeRowPlans.java`, and related files declare package-private plan and cursor types used throughout the pipeline.
- Observation: A trial compile with a real `org.eclipse.rdf4j.sail.lmdb.experimental` package showed that the query implementation intentionally crosses package-private storage seams including `RecordIterator`, `ValueStore`, prefix-run types, and the optimizer pipeline.
  Evidence: The module compile failed on those inaccessible types before any visibility was widened; `maven-build.log` retains the compiler output.
- Observation: Full LMDB module verification did not complete because `ThemeQueryBenchmarkSmokeIT.executeQueryReturnsExpectedCountForPharmaQueryOne` spent about 30 minutes at 100% CPU inside `TripleStore.materializePackedIfNeeded` and native `mdb_put`.
  Evidence: `jcmd 90048 Thread.print` identified the exact test and stack; the run was interrupted after focused tests had passed.

## Decision Log

- Decision: Use an `experimental` source subdirectory while retaining `package org.eclipse.rdf4j.sail.lmdb`, and annotate every top-level type explicitly.
  Rationale: A compile-driven boundary check proved that a Java subpackage would require broadening established storage APIs solely for organization. The source subdirectory and explicit type annotations meet the requested separation and experimental marking without changing visibility or behavior.
  Date/Author: 2026-07-11 / Codex
- Decision: Move the cohesive native query planner, compiler, slot-row runtime, and native query-source abstractions together; leave packed storage codecs and general LMDB storage classes in the root package.
  Rationale: The native query types form one package-private implementation graph, while storage codecs are also used by the established store implementation and are not solely query-planner internals.
  Date/Author: 2026-07-11 / Codex
- Decision: Preserve behavior and use compilation plus existing native-query tests as regression coverage.
  Rationale: This is a package organization refactor with no intended runtime behavior change, handled under Routine D because of its size.
  Date/Author: 2026-07-11 / Codex

## Outcomes & Retrospective

The new native query implementation is grouped in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/experimental` while retaining the existing Java package and package-private integration. All 111 top-level declarations across the 44 moved source files are explicitly annotated with `@Experimental`. A focused native count test passed all 8 tests, formatting and copyright validation passed, and the final LMDB quick compile succeeded. Full module verification was attempted but stopped when the unrelated theme benchmark smoke test remained CPU-bound in LMDB materialization for about 30 minutes.

## Context and Orientation

The Maven module is `core/sail/lmdb`. Production sources currently live in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. The new native query implementation includes the `LmdbNative*` compiler, planner, expression, plan, row, aggregation, join, and evaluation-strategy files together with their native-specific collaborators such as `NativeLmdbQuerySource`, `CompositeNativeLmdbQuerySource`, `NativeSlotLayout`, `NativeRowSeeder`, `QueryWideVarLayout`, `RowBindingSetView`, `SlotBindingSetView`, `SlotAwareQueryEvaluationContext`, and `LmdbSyntheticValueSource`.

The root store classes `LmdbStore` and `LmdbSailStore` connect this implementation to RDF4J. All moved sources retain `package org.eclipse.rdf4j.sail.lmdb`, so their package-private integration remains intact. Each top-level class, interface, enum, and record in the subdirectory receives the RDF4J `@Experimental` annotation explicitly.

## Plan of Work

First, create the experimental source directory and move the cohesive native query implementation files into it while retaining their existing package declarations. Add `@Experimental` to every top-level declared type. Do not broaden visibility or alter APIs.

Second, leave tests in their existing package because the production Java package remains unchanged. Run the copyright checker before formatting.

Finally, run the repository formatter, a focused native-query test selection, and the whole `core/sail/lmdb` module verification through the repository's `mvnf` runner. Review status and diff to ensure the unrelated benchmark whitespace edit and all pre-existing untracked artifacts remain untouched.

## Concrete Steps

Work from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

The initial sanity command completed successfully:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Move files with small, reviewable filesystem operations, then update package declarations and imports with `apply_patch`. Compile using the same root quick-install workflow or a module quick install with tests skipped. Never use `-am` or `-q` for tests.

After adding the package descriptor, run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Use `.codex/skills/mvnf/scripts/mvnf.py` for tests after reading its `SKILL.md` completely. Retain logs for final evidence.

The focused verification command completed with 8 tests, 0 failures, and 0 errors:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeCountStarTest --module core/sail/lmdb --retain-logs

The full module command was attempted but interrupted after roughly 30 minutes in the CPU-bound theme benchmark smoke test described above:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

## Validation and Acceptance

Acceptance requires that all native-query production sources selected by this plan reside beneath `.../lmdb/experimental`, retain `package org.eclipse.rdf4j.sail.lmdb`, and mark every top-level declared type with `@Experimental`. No selected native-query source may remain in the root source directory. The module must compile, focused native query behavior tests must pass, and full `core/sail/lmdb` verification must pass. Existing query results and store behavior must remain unchanged.

## Idempotence and Recovery

Package moves and import edits are safe to retry. If compilation reveals that a moved helper is general storage infrastructure, move it back before changing visibility. If compilation reveals a root integration dependency, expose the narrowest constructor, interface, or method needed rather than making the whole implementation public. Do not use destructive Git cleanup or remove untracked files; use the diff to distinguish this work from pre-existing changes.

## Artifacts and Notes

The initial root build output is retained in `maven-build.log`. Its summary ends with:

    [INFO] RDF4J: LmdbStore ................................... SUCCESS [  3.878 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  29.846 s (Wall Clock)

Focused evidence is persisted at repository root in `initial-evidence.txt` and reports:

    Summary: tests=8, failures=0, errors=0, skipped=0, time=0.665s

## Interfaces and Dependencies

The new package depends only on dependencies already present in `core/sail/lmdb`; no new Maven dependency is needed. Its package descriptor uses `org.eclipse.rdf4j.common.annotation.Experimental`. Cross-package integration should be limited to the native evaluation strategy factory used by `LmdbStore` and the native query-source contract implemented by LMDB datasets in `LmdbSailStore`, plus any codec/storage access that compilation proves essential. All other moved implementation types should remain package-private.

Revision note (2026-07-11): Created the plan after repository and dependency-boundary discovery and after the mandatory green root install. Revised it after a compile-driven package-boundary experiment showed that a Java subpackage would require unrelated API widening. Completed it with the final layout, focused verification, and the full-suite CPU-bound limitation.
