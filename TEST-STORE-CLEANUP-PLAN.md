# Close test stores and remove their temporary data

This execution plan follows `PLANS.md` and is maintained as the audit progresses.

## Purpose / Big Picture

Tests must release repositories, store connections and underlying storage handles before their temporary files are removed. A failed assertion or partially completed setup must still reach cleanup. This work audits all test fixtures and benchmark fixtures in RDF4J, fixes uncovered ownership gaps, and leaves production storage behavior unchanged.

## Progress

- [x] (2026-11-18) Confirm HASMAC main and create isolated branch.
- [x] (2026-11-18) Audit fixtures and repair resource ownership.
- [x] (2026-11-18) Compile affected modules and verify cleanup.
- [x] (2026-11-18) Independently review changes and repeat remaining ownership searches.
- [x] (2026-11-18) Commit, push, and prepare upstream comparison.

## Surprises & Discoveries

The starting revision is `be0c4c724f582e22d1a2936b87bb24969cc9154a`, obtained from HASMAC main. Merely finding a store constructor does not establish a leak: a superclass, repository manager, or enclosing repository can own shutdown. Each candidate therefore requires tracing its owner. The initial search outside core and spring-components found 285 creation or temporary-path sites, including inherited compliance factories and deliberately persistent benchmark datasets.

## Decision Log

Use Routine D because this is a broad test-fixture refactor spanning multiple modules. Do not add a global shutdown hook or change production close semantics. Prefer the JUnit version already present, with `@TempDir` for test-managed directories and explicit `finally` or lifecycle teardown for stores. Shutdown must precede deletion, and a failure closing one resource must not prevent attempts to close the remaining resources. Benchmark-created paths must be deleted at teardown; user-supplied benchmark data must be retained.

The audit is divided into disjoint areas: LMDB tests, other Sail tests, other core tests plus Spring/example tests, and testsuites/compliance/tools. New dependencies are unnecessary. The requested branch and commit prefix is `GH-0000`, because no issue number was supplied.

2026-11-18: Two Spring test paths expose resources that cannot be closed from the test because production factories hide their owners: a failed remote lookup loses its repository manager, and connection-factory decorators hide a disposable pool from Spring. Permit narrowly scoped lifecycle fixes for these owners, with regression tests demonstrated to fail before the fixes. These changes must preserve repository behavior and exception causes, add no dependency, and avoid changing storage semantics. Ordinary test-owned resources remain fixed in test fixtures.

## Outcomes & Retrospective

The audit covers tests and benchmarks under core, Spring components, shared testsuites, compliance modules, and tools. Constructor searches were followed through repository managers, superclass fixtures, transferred ownership, and directly owned native resources. Fixes cover normal teardown, assertion failure, and partial setup. A separate review caught teardown-order regressions before delivery and additional externally owned SERVICE resolvers.

The six new shared-compliance cleanup regressions all failed against the original main fixture classes and pass with the fixes. The ten new Spring lifecycle regressions failed before their production fixes and pass afterward. Five of eight new Elasticsearch lifecycle cases failed before its production fix; all eight now pass. These cases exercise real ownership gaps instead of relying on process exit to release resources.

The final Java 25 quick install passed for all 125 reactor modules. All changed Java and JMH sources compile, the repository header check passed, and Spotless formatted the 307 changed Java sources without changing unrelated files. The final Java source tree matches the staged tree exactly.

Final retained test reports include 843 LMDB unit/fixture cases, 1,004 other Sail cases, 583 other core/Spring cases, 245 tools/shared-fixture cases, and 1,293 compliance/HTTP-server cases. These selected reports contain no failures or errors. Their 21 skips are existing disabled tests, opt-in investigations, or federation cases intentionally excluded for a backend mode. Federation selections also ran in all three configured backend executions. The separately selected checkpoint integration test passed. Twelve direct LMDB lifecycle smoke scenarios passed, including partial setup, repeated setup, native handles, worker termination, shutdown exceptions, and temporary-directory deletion.

Dedicated shared-fixture and web-fixture temporary roots are empty after completion. Embedded-server runs leave no repository data directories; third-party native-library extraction directories are outside store ownership. The native lifecycle smoke checks verified exact before/after directory equality, and the checkpoint test's default diagnostic directory and its JUnit parent were removed.

The final constrained-heap replay also passed: 300,000 rows, measured child maximum heap of 134,217,728 bytes, one spill, and one spill close. Its default artifact directory and its JUnit parent were absent after the run. The final selected report ledger therefore contains 3,970 cases, zero failures/errors, and 21 expected skips, plus the twelve direct lifecycle smoke scenarios. Running the constrained test required setting its temporary directory at JVM startup on workspace storage; `/tmp` did not meet its existing two-GiB free-space prerequisite. No heap or storage assertion was weakened.

Intentional crash-child termination is preserved because the recovery assertions require it. Default temporary diagnostic stores are now managed by the test; explicit artifact-output paths remain retained. Two pre-existing low-level benchmark policies retain directories when native shutdown itself fails, avoiding deletion of potentially live native mappings. Existing explicit cleanup-retention contract tests remain intact.

The opt-in `StreamingSpillPrototypeTest.measureTransfer` investigation reaches its reopen check and fails because its unchanged low-level helper did not persist `store.properties`. The same failure was reproduced using the exact original main fixture with 128 rows; no storage-format behavior or assertion was changed to conceal it. Its default semantic case passes. An initially broader LMDB verification also launched the unmodified `LmdbPharmaNativeRegressionInvestigationIT`, whose fork exited 137; this resource-heavy investigation is not claimed as passing. Docker-backed Elasticsearch integration tests were unavailable because the Docker socket is absent. Benchmarks were compiled and their lifecycle paths exercised; full performance measurements and every stress suite were not run.

## Reviewed Change Inventory

| Area | Java sources changed |
| --- | ---: |
| LMDB tests and benchmarks | 89 |
| Other Sail modules | 126 |
| Other core modules | 29 |
| Spring components | 12 |
| Reusable testsuites | 18 |
| Compliance fixtures | 8 |
| Tools and embedded servers | 25 |
| Total | 307 |

Of these, 302 are testing infrastructure, tests, or benchmarks. The five runtime files are the documented Spring and Elasticsearch ownership fixes. Six new test-source files and extensions to existing classes add 27 cleanup regression cases. No dependencies or storage-format changes are introduced.

## Reproduction and Evidence

The final root build was `mvn -o -Dmaven.repo.local=.m2_repo -Pquick -DskipFrontend=true -Dformatter.skip=true -Dimpsort.skip=true -Dspotless.skip=true install`, after separate Spotless formatting and header verification. Test selections were run separately without `-am` or `-q`. Full commands and initial/final logs remain in `initial-evidence.txt`, the three per-area evidence notes, `logs/`, and the sibling `evidence/` directory in the working checkout.

The focused selections include all 16 modified sketch-estimator classes, Spring lifecycle regressions and all four explicitly selected `RDF4JTemplateTestsWith*` variants, native WAL/cleanup contracts, 418 SHACL cases, offline Elasticsearch lifecycle/configuration, 53 LMDB unit/fixture classes, real HTTP repository and SERVICE tests, Rio manifest suites, and concrete SPARQL 1.1/1.2 query/update suites. Failure assertions were preserved, and cleanup exceptions remain visible.

## Context and Orientation

The Maven reactor at `pom.xml` contains `core`, `testsuites`, `compliance`, `tools`, and `spring-components`. Test suites under `testsuites/*/src/main/java` are reusable testing infrastructure despite their source directory name. A repository usually owns its Sail stack; a repository connection does not own its repository. Closing the connection is necessary before repository shutdown. Direct low-level stores require their own close calls.

## Plan of Work

Search Java test and benchmark sources for store and repository construction, initialization, temporary-file creation, delete-on-exit, and manually created data directories. Follow inherited lifecycle hooks and ownership transfer before editing. Convert unowned temporary directories to managed temporary roots where JUnit supports it. Add or harden shutdown in teardown or finally scopes, including initialization and assertion failure paths. Preserve intentional reopen, recovery and lock-contention behavior.

The first milestone is a per-area inventory of confirmed leaks and repairs. The second milestone is an installed build and targeted test runs covering the changed fixtures, including reused test suites through concrete subclasses. The last milestone is a second audit, formatted source review, and a branch published to HASMAC with an upstream PR creation link.

## Concrete Steps

Run commands from the repository root using Java 25 and Maven 3.9. The local dependency repository is `.m2_repo`. First run the root `-Pquick clean install`, with frontend compilation skipped because this task changes Java fixtures only. Run tests separately, never combining enabled tests with `-am` or `-q`. Logs and concise Surefire/Failsafe summaries are retained for the handoff. Exact successful commands and summaries will be added after verification.

## Validation and Acceptance

Affected Java sources and benchmark sources must compile. Selected tests must pass with no new failures, while preserving assertions and failure visibility. Where feasible run selected suites with a dedicated `java.io.tmpdir` and inspect for leaked store directories. Review that cleanup is registered before initialization or other fallible setup, and that recursive deletion occurs after shutdown. Source review must distinguish deliberately persistent benchmark input from temporary output. No new dependency or public API is planned, apart from standard Spring destruction callbacks required by the documented ownership defects.

## Idempotence and Recovery

The checkout is isolated from existing work. Re-run builds and tests within it; retain untracked evidence. Do not delete existing repositories or unrelated temporary directories. The branch can be reverted through ordinary Git commits if necessary. If a test failure is unrelated, reproduce or identify it precisely instead of weakening assertions.

## Artifacts and Notes

Per-area audit notes and build/test logs remain available in the checkout. The final commit will include the reviewed test-fixture changes and this plan; downloaded dependencies and generated logs will not be committed. A Git patch and complete source tree will be accessible from the pushed commit.

## Interfaces and Dependencies

Use existing JUnit lifecycle annotations, `java.nio.file.Files`, existing recursive-deletion utilities, and existing repository/Sail shutdown methods. No runtime-wide resource registry or external library is introduced. The narrowly scoped production exceptions add standard Spring destruction callbacks and complete existing Elasticsearch shutdown ownership; they do not alter query evaluation, persistence formats, or dependency versions.

Publication branch: `HASMAC-AS/rdf4j:GH-0000-test-store-cleanup`, based on `be0c4c724f582e22d1a2936b87bb24969cc9154a`. The HASMAC and upstream main branches were identical at that base. The uploaded Java source tree was checked against the local Git tree before publication. The complete patch and complete contents of changed files are generated from the final published commit.

Plan initialized on 2026-11-18 before fixture edits and updated with the final audit, independent reviews, and verification evidence.

## Elasticsearch lifecycle exception (2026-11-18)

The Sail audit found a production-owned resource that test cleanup cannot currently release: constructing `ElasticsearchStore` with host parameters immediately starts its GC-monitor executor, while inherited `AbstractSail.shutDown()` returns before closing an uninitialized store. Factory configuration tests therefore cannot stop those monitors through the public lifecycle. Initialized shutdown also skips provider closure if underlying store closure fails. Permit a narrow fix in `ElasticsearchStore`: serialize shutdown with the existing initialization lock, close the owned provider even before initialization, preserve AbstractSail preflight rejection behavior, and let the monitor observe the existing atomic shutdown signal without strongly capturing the store. Borrowed providers remain unclosed through the existing wrapper. Add deterministic offline lifecycle regressions and capture failing evidence before editing production, then add explicit factory-test shutdown and verify the same selection. No external Elasticsearch or new dependency is required for these regressions.
