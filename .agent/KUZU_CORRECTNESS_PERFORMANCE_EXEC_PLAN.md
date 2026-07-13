# Correct and streamline the Kuzu Sail

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The Kuzu Sail is RDF4J's Java RDF store inspired by Kùzu's compact storage and factorized execution design. After this work, supported SPARQL queries must return the same results as RDF4J's reference evaluator, factorized operators must preserve multiplicity exactly once, large queries must respect memory and cancellation boundaries, and obsolete implementation fragments must no longer obscure the active paths. A developer can observe the result through focused regression tests, the complete `core/sail/kuzu` Maven verification, and benchmark comparisons for performance claims.

The user explicitly excludes race conditions and transaction durability or atomicity. Therefore this plan does not change shared-read lazy-initialization races, cursor lifetime races, WAL synchronization, crash recovery, checkpoint publication atomicity, or transaction undo. It may retain already-present user changes in those areas, but it will not expand them.

## Progress

- [x] (2026-07-11 06:26Z) Read repository, ExecPlan, performance, complexity, and Maven workflow instructions.
- [x] (2026-07-11 07:03Z) Inventoried report findings against the dirty worktree and classified included, excluded, already implemented, disproven, and unproven groups.
- [x] (2026-07-11 07:39Z) Fixed and verified the five reproduced SPARQL semantic defects: wildcard-distinct counting, anonymous cyclic WCOJ admission, partial-domain MINUS, absent-term zero-length paths, and LANG type errors.
- [x] (2026-07-11 08:21Z) Completed the requested side quest: changed all 125 reactor POM coordinates to `6.1.0-SNAPSHOT` and passed the 124-module offline quick clean install.
- [x] (2026-07-11 08:32Z) Corrected KCSF profile propagation and rejected trusted-bulk duplicate statements before query-ready checkpoint publication, with actionable CLI diagnostics.
- [x] (2026-07-11 08:37Z) Bounded factorized DP planning to seven exact patterns and 50 connected subgraphs per level while retaining a complete native plan for a 16-pattern star.
- [x] (2026-07-11 08:46Z) Bounded aggregate/DISTINCT spill writer fan-out, separated DISTINCT partition/slot hash bits, and replaced unbounded parallel aggregate generations with a bounded completion queue.
- [x] (2026-07-11 09:10Z) Capped ID-to-Value caches, cleaned exceptional spill builds, and added full-value verification after collision-prone literal hash postings.
- [x] (2026-07-11 09:37Z) Formatted the reactor, completed full Kuzu verification with no failures outside the preserved baseline set, and replaced the remaining forbidden WCO random-access spill API.
- [ ] Capture and preserve smallest-scope failing Surefire evidence for each behavior-changing correctness group before editing production code in that group. Completed: first SPARQL semantic group. Remaining: planner, resource, bulk, collision, snapshot, and any later semantic group.
- [ ] Correct storage-read visibility and page-read semantics that are independent of crash durability.
- [ ] Correct factorized multiplicity, empty-group, unsigned-order, and overflow behavior.
- [ ] Correct WCOJ, aggregate, OPTIONAL, MINUS, expression, path, type-constraint, numeric-range, and literal-posting SPARQL semantics.
- [ ] Correct planner cache identity and bound planning work to Kùzu-style exact/approximate limits.
- [ ] Bound aggregation, DISTINCT, sorting, join, cache, and fallback memory; close spill resources reliably; remove forced-GC retry loops.
- [ ] Add cancellation and lazy cursor behavior where current code materializes or drains unbounded input.
- [ ] Validate bulk-load query-ready configuration and duplicate set semantics while leaving crash-resume durability unchanged.
- [ ] Remove dead classes, redundant adapters, obsolete statistics/columnar helpers, and unused imports without altering supported APIs.
- [ ] Run formatting, copyright checks, focused regressions, full Kuzu module verification, and relevant benchmarks.
- [ ] Record final outcomes, benchmark confidence, and deliberately excluded residual risks.

## Surprises & Discoveries

- Observation: The starting worktree already contains 65 modified/deleted tracked Kuzu files and multiple untracked regression tests. These changes are user-owned and must be preserved rather than reset.
  Evidence: `git status --short` lists changes in `StoreState`, `StatementTable`, factorized operators, columnar codecs, and tests including `KuzuDeltaOverlayGatingTest`, `KuzuStatementTablePageMergeTest`, and `RdfFactorizedPositionMultiplicityJoinTest`.

- Observation: The current worktree appears to implement the delta-overlay gate, page-read merge, physical multiplicity radix, stable enum encodings, and several overflow/unsigned fixes, but focused test evidence must be reconciled before treating them as complete.
  Evidence: `StoreState.needsDeltaOverlay`, `StatementTable.pageForRead`, and `FactorizedIdTuple.positionForPhysicalOrdinal` are uncommitted additions.

- Observation: The supplied review claimed ten reviewers initially and eleven later, and it ran no tests. Its findings are leads, not sufficient acceptance evidence.
  Evidence: attachment lines 5 and 117 disagree; line 202 states that nothing was run.

- Observation: The report's checkpoint-boundary append-loss claim is false for the reviewed implementation. `StatementTable.putRaw` calls `presentAt(index)`, and `presentAt` calls `ensureLoadedChunk(index)`, so an append in the partial immutable chunk faults and merges that page before `pageForCheckpoint` can return it.
  Evidence: `KuzuStatementTablePageMergeTest` ran 5 tests with 0 failures, including `putRawAppendIntoCheckpointBoundaryChunkStaysInNextCheckpointPage` and `appendsAfterCheckpointSurviveSecondCheckpointWithoutReads`.

- Observation: The generic complexity scanner reports almost every nested storage loop as high severity and is too noisy to drive edits directly.
  Evidence: the scanner flags ordinary sequential decode loops in bucket readers and caches without proving repeated scans. Its output will be treated only as leads; report-derived hot paths and measurements take priority.

- Observation: Five of twelve focused differential SPARQL queries reproduce the newer review; seven do not reproduce in their strongest practical forms.
  Evidence: `KuzuReportedSparqlCorrectnessTest` fails for `COUNT(DISTINCT *)`, anonymous cyclic WCOJ under `FILTER EXISTS`, partial-domain MINUS, zero-length path from an absent IRI, and `LANG` over an unbound argument. Boolean lexical equality, chained OPTIONAL with an unbound key, REGEX on an IRI, grouped OPTIONAL condition errors, conjunctive `rdf:type`, and checkpoint-backed decimal range counting match MemoryStore.

- Observation: All five reproduced SPARQL defects are now fixed without changing the seven already-correct query shapes.
  Evidence: `KuzuReportedSparqlCorrectnessTest` ran 12 tests with 0 failures after the fixes; retained log `logs/mvnf/20260711-073912-verify.log`.

- Observation: KCSF finalization admitted an index profile but its fan-out helper still read four unrelated static system-property flags, producing zero-row segments for a fully enabled profile.
  Evidence: the corrected profile-specific test failed with `KCSF query-ready finalization produced no columnar segments for 3 materialized statements`, then passed after the profile was threaded through fan-out; logs `20260711-082315-verify.log` and `20260711-082438-verify.log`.

- Observation: Trusted bulk ingestion could publish two identical quads when statement deduplication was disabled, even though RDF storage has set semantics.
  Evidence: the regression first completed successfully with two statements, then failed at the checkpoint boundary once the sorted SPOG stream detected adjacent duplicate term-ID tuples; preserving the nested cause required a second error-propagation fix. The final focused run is green in `logs/mvnf/20260711-083133-verify.log`.

- Observation: Limiting plan buckets alone did not bound planning because the hypergraph constructor eagerly retained up to every connected subset before the DP table applied its separate cap.
  Evidence: `Hypergraph.buildConnectedMasks` previously admitted every connected mask and the default cap was 65,536. The constructor now retains at most 50 masks per level; a 16-pattern all-connected star still returns one native solution in 1.262 seconds (`logs/mvnf/20260711-083633-verify.log`).

- Observation: The default 1,024 spill partitions could each retain a 1 MiB output buffer, and parallel aggregate workers retained every sealed generation until the entire input ended.
  Evidence: both spill implementations allocated writer arrays and lazily opened every touched partition without eviction; `WorkerPartial.sealed` was an unbounded `ArrayList`. Writers are now capped at 16 with 64 KiB buffers and worker generations cross a bounded completion queue. Matching spill tests and the four-test parallel aggregate class are green (`20260711-084202-verify.log`, `20260711-084517-verify.log`).

- Observation: Hash-derived literal postings were treated as authoritative subject matches even though STRING, LANG_STRING, NUMERIC_DECIMAL, and NUMERIC_DOUBLE keys do not encode the full literal value.
  Evidence: a synthetic colliding posting emitted its candidate before the fix (`20260711-085712-verify.log`). Collision-prone candidates now traverse predicate adjacency and use full SPARQL value comparison; verified and rejected candidate tests are green (`20260711-090931-verify.log`).

## Decision Log

- Decision: Use Routine D with this ExecPlan, while applying Routine A test-first evidence inside each behavior-changing milestone.
  Rationale: The request spans storage reads, query semantics, planning, execution, resource management, and cleanup, making it a significant refactor. The repository's proportional test-first rule still forbids new behavior patches without a focused failing test.
  Date/Author: 2026-07-11 / Codex

- Decision: Treat the supplied report as the issue inventory, not as proof.
  Rationale: Static inspection establishes plausible mechanisms, but correctness acceptance requires RDF4J differential tests and performance acceptance requires repeatable measurements.
  Date/Author: 2026-07-11 / Codex

- Decision: Exclude concurrency races and durability/atomicity work even when adjacent to included code.
  Rationale: This is the user's explicit scope boundary. Runtime wrong results that do not depend on a race or crash remain included.
  Date/Author: 2026-07-11 / Codex

- Decision: Preserve all existing tracked and untracked work and layer only reviewable patches on top.
  Rationale: Repository instructions identify unknown changes as user-owned. Destructive cleanup, reset, restore, and deletion of unexpected artifacts are forbidden.
  Date/Author: 2026-07-11 / Codex

- Decision: Reject duplicate trusted-bulk statements instead of silently deduplicating them during finalization.
  Rationale: The trusted path explicitly trades deduplication work for throughput. Failing at the already-sorted canonical permutation prevents publishing incorrect set cardinalities while keeping the contract and operational mistake visible.
  Date/Author: 2026-07-11 / Codex

## Outcomes & Retrospective

The included report defects reproduced during this run are fixed: five SPARQL semantic errors, anonymous cache identity, KCSF profile propagation, trusted-bulk duplicate set semantics, literal-posting collision verification, forced-GC probes, exponential DP retention, unbounded parallel aggregate generations, excessive spill writer fan-out, coupled DISTINCT hash bits, unbounded ID-to-Value caching, exceptional spill leaks, and the forbidden WCO random-access spill implementation. Twenty-three obsolete production classes/interfaces are removed.

The full module run executed 877 tests with 21 failures, zero errors, and one skip. The preserved pre-change baseline had 22 failures; every current failure is in `baseline-failing-tests.txt`, so this work added no regression and removed one baseline failure. The remaining baseline failures cover pre-existing logging assertions, legacy/default-index-profile assertions, low-heap concurrent ingestion, two transaction-copy assertions, and explicitly excluded concurrency/WAL behavior. Full evidence is retained in `logs/mvnf/20260711-092825-verify.log`; the one forbidden-API failure from that run was subsequently fixed and its focused test is green.

## Context and Orientation

The relevant Maven module is `core/sail/kuzu`. `StoreState` coordinates term and statement state and chooses between mutable-table scans and checkpoint-backed indexes. `StatementTable` stores encoded RDF statements and exposes cursor and checkpoint-page views. `KuzuNativeQueryEngine`, `KuzuPrimitiveExecutor`, and the `Rdf*Operator` classes implement native SPARQL evaluation. Classes under `internal/exec` implement factorized tuples: one physical row may represent several logical rows through a multiplicity value. Applying multiplicity twice returns too many SPARQL solutions.

Kùzu C++ source lives under `kuzucpp` and is an architectural reference for CSR adjacency, factorized tables, planner limits, memory management, cancellation, MVCC, and checkpointing. It is not the semantic oracle for SPARQL. RDF4J's standard evaluation engine and repository compliance tests are the oracle for OPTIONAL, MINUS, expression errors, aggregates, property paths, literal equality, and RDF dataset semantics.

The attached report identifies these included correctness families: dirty-flag delta visibility; page-backed read resurrection; factorized multiplicity; anonymous-variable WCOJ; `COUNT(DISTINCT *)`; OPTIONAL and MINUS errors; primitive literal value equality; unbound joins; zero-length paths; REGEX/STR/LANG typing; conjunctive `rdf:type`; numeric ranges over all numeric datatypes; collision verification in literal postings; plan-cache anonymous identity; bulk query-ready/profile consistency; duplicate quad handling; signed/unsigned ordering; enum storage stability; hash-table capacity overflow; and empty factorized key groups.

The included performance/resource families are: unbounded parallel aggregate generations; spill partition/hash clustering; one-megabyte buffer fan-out; unbounded value caches; spill file cleanup; unbudgeted join build sides and fallback operators; repeated forced garbage collection; missing cancellation; eager statement/EXISTS materialization; unbounded dynamic-programming enumeration; ignored bulk parallelism; and unnecessarily linear ordinal decoding. Each performance claim remains a hypothesis until measured or, for memory bounds, proven with deterministic budget tests.

Excluded families are: plain-field and `HashMap` lazy-init races, shared-lock publication, transaction-less cursor/checkpoint races, WAL `force`/fsync, corrupt-tail recovery, mid-commit undo, manifest publication, checkpoint-boundary crash durability, resume rollback, and other transaction durability or atomicity behavior.

## Plan of Work

First, reconcile the current uncommitted patch with the issue inventory. Read every new focused test and its saved evidence, then run the complexity scanner over `core/sail/kuzu`. Build an issue matrix in this plan's Artifacts section. Do not edit production code until the smallest failing test for the active correctness group has been run and its Surefire report preserved in `initial-evidence.txt` or a group-specific evidence artifact.

Second, complete correctness work in independently verifiable groups. Begin with storage-read and factorization tests already present, because current production changes appear intended to satisfy them. Then add RDF4J differential tests for WCOJ and SPARQL semantic issues before changing their native admission or execution paths. Prefer a safe fallback to RDF4J when a native operator cannot implement the complete contract without a broad rewrite. Keep native fast paths only when their admission predicates prove the required datatype and binding invariants.

Third, address planning and resource performance. Bound DP state using Kùzu's design: exact enumeration through seven variables, approximate single-extension planning above that, no more than fifty subgraphs per level, and no more than the existing Pareto plan cap per subgraph. For spill paths, limit simultaneously open writers, budget all build-side structures, mix partition and slot hash bits independently, and close/delete resources through one owner. Replace repeated `System.gc()` retries with deterministic reservation failure. Convert statement retrieval and EXISTS to cursors with early termination where the public interface permits it.

Fourth, remove dead code only after `rg` proves no active references and focused tests cover the replacement path. Existing user deletions should be validated, not recreated. Cleanup must remain behavior-neutral and use pre/post matching tests when it is not already covered by a behavior-changing milestone.

Finally, run copyright checks, format the module, compile the reactor with the quick profile, run focused tests, then `mvnf` for the entire Kuzu module. Run existing single-method JMH benchmarks for any path where a speedup is claimed, using identical forks, warmups, and measurement settings before and after. Do not claim performance improvement from code shape alone.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

Run the complexity scanner:

    python3 /Users/havardottestad/.codex/skills/complexity-optimizer/scripts/analyze_complexity.py core/sail/kuzu --format markdown

Run a focused unit method and retain logs:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs

Persist failing or passing evidence immediately:

    python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs" --log logs/mvnf/<verify-log> core/sail/kuzu/target/surefire-reports > initial-evidence.txt

Run the whole module after focused tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/kuzu --retain-logs

Run copyright and formatting checks before final verification:

    cd scripts
    ./checkCopyrightPresent.sh

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command contains `-q` because it is not a test command; test commands must never use `-q` or `-am`.

## Validation and Acceptance

Every correctness issue must have a smallest-scope test that fails on the defective implementation and passes after the patch. SPARQL semantic tests should evaluate the same query against the Kuzu Sail and RDF4J's reference in-memory evaluation and compare complete multisets, including unbound values and duplicate cardinalities. Collision tests must inject or construct colliding hash inputs and prove full-value verification. Numeric tests must cover integer, decimal, float, double, NaN, infinities where SPARQL defines them, and type errors.

Factorized tests must cover multiplicity on probe and build key groups, payload and no-payload joins, empty groups, multiple named graphs, and flattening bounds. Planner tests must show bounded alternative counts for star queries above seven patterns while preserving the chosen result semantics. Resource tests must show bounded open files/buffers and deterministic cleanup after cancellation or exceptions.

The complete acceptance gate is a successful `core/sail/kuzu` module verification with no unexplained failures. Performance changes require a benchmark command, before/after score, allocation result when available, and confidence level. If a change only imposes a deterministic memory bound, tests proving the bound are sufficient, but no throughput claim will be made.

## Idempotence and Recovery

The inspection, scanner, formatter, Maven, and benchmark commands are safe to repeat. Never reset, restore, clean, stash, or delete the existing worktree. If an existing test or patch conflicts with this plan, preserve it, record the conflict in `Surprises & Discoveries`, and adapt the next smallest patch. Maven must always use `.m2_repo`; tests must not use `-am` or `-q`. Missing offline artifacts may be fetched once by rerunning the exact command without `-o`, after which work returns offline.

## Artifacts and Notes

The source report is `/Users/havardottestad/.codex/attachments/8473b73f-d05c-46fb-89f6-b916cdf28666/pasted-text.txt`. Existing evidence artifacts include `baseline-failing-tests.txt` and `kuzu-baseline-verify.log`; they predate this ExecPlan and must be interpreted carefully. `maven-build.log` records the successful repository quick install from 2026-07-11.

Issue matrix, first inventory pass:

- Included and already green in the current user patch: delta-overlay gating; page-phase removal visibility; factorized key multiplicity; factorized expand multiplicity; empty factorized key groups; unsigned WCO merge; stable encoded-column and columnar enum IDs; columnar overflow/corruption guards; Bloom-filter sizing overflow; flatten bounds; combined-row multiplicity. The focused selections currently total 63 passing tests with no failures.
- Disproven: checkpoint boundary-chunk append loss. The report missed the `putRaw` -> `presentAt` -> `ensureLoadedChunk` call chain.
- Included and still requiring focused failing tests: anonymous-variable WCOJ; `COUNT(DISTINCT *)`; SUM/AVG empty, type-error, and numeric-promotion semantics; OPTIONAL condition errors; MINUS partial domains; primitive literal value equality; unbound OPTIONAL keys; zero-length property paths; REGEX/STR/LANG/LANGMATCHES typing; conjunctive `rdf:type`; full numeric range admission; literal posting collision verification; IN with unbound operands; STR native admission; plan-cache anonymous identity; DP connected-partition completeness and enumeration caps; WCOJ multiplicity materialization; factorization boundary shape; bulk query-ready/profile consistency; duplicate quad set semantics; namespace/term snapshot isolation.
- Included performance/resource work requiring deterministic tests or measurements: blocking forced GC; unbounded aggregate generations; spill writer fan-out; spill hash clustering; unbounded value caches; spill cleanup; build-side and fallback memory accounting; cancellation; eager statement and EXISTS materialization; filtered range-cursor progress; boxed CSR neighbor filtering; term-key double encoding; O(n) delta tombstone detection; unstable/excessively generic sort only if measurement shows a material loss; generic semi-mask representation only if measurement justifies replacement.
- Included behavior-neutral cleanup requiring matching pre/post coverage: the existing deletion of unused operators, obsolete statistics, redundant columnar dictionaries/stats/exec helpers, stale factorization adapters, and dead WCO helpers. References must be proven absent before accepting each deletion.
- Explicitly excluded: lazy-init and shared-lock races; transaction-less cursor/checkpoint races; WAL and segment fsync; corrupt-tail replay; commit undo; checkpoint/manifest publication; crash recovery; resume rollback; and other durability or atomicity findings.
- Sound architectural divergences are not defects by themselves: on-disk bucket partitioning instead of Kùzu chunk groups, single-threaded hash construction, comparator sorting instead of Kùzu radix keys, and generic masks remain unchanged unless a repository workload benchmark proves a material performance problem.

## Interfaces and Dependencies

No new external dependency is planned. Use RDF4J's existing query evaluation classes as the semantic reference, existing primitive collections and factorized execution types for hot paths, `KuzuQueryMemoryManager` for query-local budgets, `KuzuQueryCancellation` for cooperative cancellation, and the existing `scripts/run-single-benchmark.sh` wrapper for benchmarks. New public APIs are out of scope unless a failing test proves they are necessary; prefer private helpers, stricter native admission predicates, or fallback to the reference evaluator.

Revision note (2026-07-11 06:26Z): Initial ExecPlan created to convert the supplied code-only review into a test-first correctness, performance, resource-safety, and cleanup implementation while honoring the user's concurrency and durability exclusions.

Revision note (2026-07-11 06:56Z): Added the first complete scope matrix, recorded 63 passing tests for the existing user patch, classified architectural differences separately from defects, and disproved the checkpoint boundary-loss claim using both call-chain inspection and focused execution.

Revision note (2026-07-11 07:03Z): Preserved the first new failing SPARQL differential group (12 tests, 5 failures), narrowed the semantic implementation scope to reproduced defects, and recorded seven report claims that did not reproduce.

Revision note (2026-07-11 07:39Z): Completed the first semantic milestone with 12/12 differential tests green and recorded the exact operator-level fixes and retained evidence.

Revision note (2026-07-11 08:21Z): Recorded the user-requested `6.1.0-SNAPSHOT` reactor bump and its successful full quick-build validation.
