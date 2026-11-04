# Complete automatic sparse join aggregation

This ExecPlan is a living document maintained according to `.agent/PLANS.md`. Its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective sections must be updated as work proceeds.

## Purpose / Big Picture

Users should receive the sparse join aggregation optimization through ordinary LMDB SPARQL evaluation whenever its physical work and resource requirements justify using it. They should not need to enable an experimental property or force a strategy. The existing PR introduces an exact weighted multiplication kernel for two joined statement patterns grouped by their two outer endpoints. The new work makes that kernel a normally available planner alternative, supplies honest physical cost and memory information, and integrates execution, fallback, explanation, and reproducible performance validation.

An adjacency plane stores the neighboring RDF term identifiers for a fixed predicate and direction. A fiber is one endpoint pair together with its multiplicity across selected contexts. Multiplying the two planes sums products of those multiplicities without constructing every joined binding set. The normal evaluator remains responsible for shapes that this exact specialization does not support and for queries whose resource requirements cannot be met.

## Progress

- [x] Review frozen PR and baseline tests.
- [x] Reproduce admission, compiler, and cursor gaps.
- [x] Integrate physical costing and bounded exploration.
- [x] Correct sparse replay probing and cleanup.
- [x] Build and pass focused regression suite.
- [x] Measure beneficial and adverse query workloads.
- [x] Package complete sources and verified patches.

## Surprises & Discoveries

The original `LmdbSparseJoinAggregatePlan.quote` returns null and both `tryCreate` and `handles` require a default-false system property. Merely changing that property's default would not provide normal physical planning. The existing benchmark's `auto` route deliberately disables the feature, so it cannot be reused as evidence for completed automatic integration without updating its route definitions.

The repository already has an aggregate physical producer interface, physical cost vectors, explicit cold-choice proofs, query memory ledgers, private replay output, and clean refusal/fallback. The implementation uses these mechanisms rather than creating a separate optimizer or memory policy.

The first real automatic integration run passed all three cases: fresh-store default selection, explicit disable, and absent adjacency. It did not force the strategy or disable competitors. Broader verification also identified that hand-built RowState fixtures must use the production initializer to seed unknown slots; the new cost and alias fixtures were corrected accordingly.

Exact predicate counts and directional key-domain counts are available through bounded source metadata. They cannot establish overlap or the joint degree distribution; quotes therefore retain zero lower bounds for products and groups and conservative finite upper bounds. Opening run cursors to merge per-key metadata during pricing would duplicate execution work and violate the producer contract, so it is not used.

The first bounded performance grid exposed two pricing integration gaps. The sparse quote used its skew upper bound as the central product estimate: uniform 32-by-32 degrees produced 524,288 updates but were priced at 4,194,304. Its UNKNOWN compatibility proposal also inherited zero adaptive cardinality and fanout bands, allowing materially different workloads with the same physical recipe to share exact-variant timing history. Separate expected-versus-bound estimates and producer-owned workload bands address these local gaps without changing global arbitration policy.

The revised performance grid exposed a separate execution barrier: sparse bounded trials with more than 4,096 output groups abort without a completed or deadline-censored timing, while forced execution completes the same work in less than the admitted probe deadline. `SparseOutput` applies the total row count to a guard intended for a growing private output array, although its actual sink is the ledger-accounted, spillable `NativeResultReplay`. Add a regression before replacing that inappropriate total-output cap with the existing cancellation poll; retain all deadline, ledger, spill, and unpublished-output cleanup rules. The 446-test run before this discovery passed with no failures or errors. A later check of Surefire XML showed that `-DreuseForks=false` did not establish per-class isolation: the module explicitly configures reuse, and the examined classes share one booter command. The 69-test class was separately invoked; that isolation is independently established.

Whole-plane counts can also bound fixed-graph and dataset-filtered execution because filtering only removes rows. Their quotes charge raw traversal and the maximum context comparisons per quad, and their retained-state bound includes up to two fixed context arrays. Whole-plane branching cannot establish filtered contraction, so those candidates receive no cold-choice proof. Context-processing modes are part of physical identity to keep compressed unrestricted and raw filtered executions separate.

`KeyRunCursor.copyFibers` already supports physical compressed multiplicities, but its default scalar implementation can scan a huge repeated fiber without checking cancellation. A per-position capability distinguishes bounded physical copying from scalar/merged fallback. The existing bounded quad adapter remains necessary for the latter and for context filtering.

The first 27-case bounded measurement grid found nonempty high-fanout queries where automatic execution retained IR despite a 2–4x forced-sparse advantage. Fresh-route JMH on the hot weighted median-HAVING case measured 112.523 ms disabled versus 27.471 ms automatic and 30.357 ms forced sparse, so the algorithm and fresh automatic path worked. Inspection identified three integration omissions: the central product estimate used a skew upper bound, sparse inherited zero workload-size/fanout bands from an UNKNOWN compatibility proposal, and the strategy was missing from the existing bounded-specialist trial policy. These were addressed with failing contract tests before the corresponding production changes; no global arbitration policy was changed.

## Decision Log

Decision: Preserve the existing opt-out property but make ordinary availability the default once selection is costed and tested. Rationale: administrators retain an escape hatch while normal users need no feature switch. Date/author: 2026-11-04, Codex.

Decision: Use the existing native physical planning and memory contracts. Rationale: an integrated feature must participate in the same selection, telemetry, cancellation, and refusal behavior as other aggregate producers. Date/author: 2026-11-04, Codex.

Decision: Require failing tests before each new externally observable behavior and run all builds and performance measurements serially. Rationale: repository AGENTS.md requires test-first changes, and competing JVM workloads invalidate performance evidence. Date/author: 2026-11-04, Codex.

Decision: Do not add dependencies, change unrelated native strategies, or silently broaden SPARQL semantics. Rationale: this work completes a specific exact optimization; supported shapes must have demonstrated semantic equivalence. Date/author: 2026-11-04, Codex.

Decision: Admit pure constant and guaranteed-bound alias assignments above the join, while rejecting computed expressions, overwritten targets, guarded bindings, and unbound aliases. Rationale: parser-lowered `COUNT(1)` and bound aliases have the same multiplicity as COUNT(*) and should reach the same kernel without executing or discarding expression effects. Date/author: 2026-11-04, Codex.

Decision: Lower aggregate ValueConstant arguments through the existing exact-term constant-binding helper, retaining the hidden binding name in PendingAggregateExpression. Rationale: the compiler previously treated even COUNT(1) as a per-row computed expression, preventing sound recognition. Reusing the ordinary BIND helper preserves plan-local term interning and conservative handling of language-tag variants. Regression coverage also exercises DISTINCT, SUM, AVG, MIN, and GROUP_CONCAT constants. Date/author: 2026-11-04, Codex.

Decision: An optional Producer.blocksBeforeOutput contract marks sparse as fully blocking, and the common quote charges its total work as startup. Rationale: sparse finishes its private replay before returning results, so LIMIT 1 cannot discount the multiplication, allocation, or output preparation. Other producers retain the existing default. Date/author: 2026-11-04, Codex.

Decision: First-choice contraction admission is symmetric under transposition and requires branching in both endpoint/shared domains, or provable context multiplicity, plus a conservative kernel memory bound inside the per-query cap. Rationale: endpoint reuse alone includes ordinary unit-degree joins, and one-sided fanout can still yield only unit-count groups. No timing superiority or overlap is asserted. Date/author: 2026-11-04, Codex.

Decision: Give scoped executions complete conservative quotes from whole-plane metadata, while withholding a cold-choice contraction proof. Rationale: exact filtered cardinalities are unavailable, but raw scans, context comparisons, products, groups, and fixed scratch arrays all have finite bounds. Add each side's context-processing mode and the structural buffer count to physical identity. Reuse the kernel's batch-capacity constant so scratch accounting cannot diverge from execution. Date/author: 2026-11-04, Codex.

Decision: Estimate expected products as Fleft * Fright / max(Sleft, Sright), under uniform degrees and shared-domain containment, capped by the existing safe product bound. Expected groups are capped by expected products and endpoint pairs; zero lower bounds and conservative product/group upper bounds remain unchanged. Rationale: treating the skew upper bound as the central price overstates common uniform work, while the metadata cannot prove overlap or degree distribution. Two regression tests failed on the previous central estimates before this change. Date/author: 2026-11-04, Codex.

Decision: Add an identity-default Producer.variantKey hook receiving the same successful captured Work used for physical pricing. Sparse refines only the existing logarithmic cardinality and fanout bands from expected scan, product, and group counts. Rationale: the compatibility scalar bridge would also change uncertainty and pricing authority; the producer hook separates timing identity from those semantics. Two adaptive-key regression tests failed with both bands zero, while variable-renaming, default-producer, and corrected expected-work controls passed. No source cursor, raw cardinality string, variable name, or global arbiter policy is added. Date/author: 2026-11-04, Codex.

Decision: Register sparse with the existing bounded-specialist family matrix. Rationale: an otherwise eligible specialist can remain starved behind family-only timing predictions; the existing matrix provides a deadline-limited first trial with a measured fallback, confirmation only for promising timings, and the established query/scheduler limits. The new pessimistic-family test failed before the one-line registration, while all 22 cost tests passed. This also gives sparse the same structural normal-admission behavior as the other matrix members when a complete quote is unavailable. Date/author: 2026-11-04, Codex.

Decision: Replace the total-output materialization-row guard in SparseOutput with the same per-output cancellation poll. Rationale: its sink is the existing byte-accounted, spillable replay, not an unbounded output array. The test `probeCanCompleteSpillingReplayBeyondPrivateRowBuffer` failed before this change with one failure and no errors: 512 groups, an eight-row probe limit, an 8 KiB replay budget, and a 30-second deadline triggered KernelCancelledException in about one second. The original Surefire report is preserved in `sparse-integration-probe-capacity-red-report.txt` and `initial-evidence.txt`. Existing deadline, terminal cancellation, ledger, spill, cleanup and error contracts are unchanged. Date/author: 2026-11-04, Codex.

## Outcomes & Retrospective

The implementation is complete and locally verified. Sparse aggregation is normally available within the configured native evaluator, with finite metadata-based work quotes, explicit uncertainty and blocking startup, conservative memory orientation, workload-aware adaptive identity, bounded specialist exploration, pure constant/alias lowering, direct and decoded physical-fiber integration, and executed explanations. The replay fix removes an array-specific row ceiling from a spillable output sink while retaining cancellation and memory ownership. Plain store configuration still defaults to the generic evaluator and disabled direct adjacency; no persistent index is built on demand.

The final clean module install with the benchmark profile succeeded. The final focused and adjacent selection passed 447 tests with zero failures, errors or skips, including the new spilling-probe regression and the unchanged descriptor-lifetime assertions. The earlier 411-test descriptor-baseline failure and the separately invoked 69-test passing class remain recorded. The command's requested `reuseForks=false` did not override the module's explicit reuse configuration; no per-class isolation claim is made for the 446- or 447-test reruns.

All five post-fix bounded suites completed: 27 query cases, two process forks, three routes, and five timed samples per case/route/fork, totaling 810 verified warmed samples. Fresh-route JMH supplied five primary cases, followed by a longer-warmup control for the one consistent disjoint-input adverse case. All outputs were verified against the independent oracle. The weighted hot 32-by-32 median-HAVING case averaged 29.779 ms automatically versus 134.816 ms with sparse disabled (4.527 times), close to 28.903 ms forced sparse. Uniform high fanout improved in mean with wider uncertainty. Low fanout, asymmetric input, disjoint input, and short mixed-history runs do not establish a universal win. The longer-warmup disjoint control averaged 0.899 ms automatically versus 0.721 ms disabled; its uncertainty intervals overlap, but the higher observed mean is retained. These results justify selective integration and explicitly quantify remaining planning/learning overhead.

The final benchmark JAR SHA-256 is `2de560b99e7fd9cfdb5e71dfd02579e17935f488cc6d5d152298148bc7a0963f`. No production, test, or benchmark-source changes followed that build. Subsequent edits link the report and clarify preservation of the JMH helper's full log. Final copyright validation and `git diff --check` passed. The complete [integration report](../core/sail/lmdb/sparse-join-aggregate-results/2026-11-04-integration/report.md) preserves source identities, exact commands, raw data, adverse cases, failure history, timing boundaries, and route-witness limitations. Implementation and delivery are complete. Both alternative patches apply to their immutable bases and reproduce the same resulting file tree; the source archive and standalone downloads pass checksum verification. No commit, push, or GitHub comment has been made.

## Context and Orientation

The working checkout is `/workspace/scratch/0c324f9a00f9/rdf4j-review-6369`, branch `GH-0000-sparse-join-aggregate`, initially at PR head `03a16b126371ff8cc523c7b68ee1f875d867f298`. No remote changes are needed to develop or verify this implementation. Preserve unrelated and untracked files.

Production classes are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`. `SparseJoinAggregateKernel.java` performs weighted sparse multiplication with checked arithmetic, cancellation, and ledger-accounted primitive arrays. `LmdbSparseJoinAggregatePlan.java` recognizes supported algebra, opens adjacency cursors, adapts weights and graph constraints, and materializes private results. `LmdbNativeGroupStep.java` offers its candidate to the shared strategy arbiter. `NativeAggregatePhysicalPlan.java`, `NativeAggregateCostQuotes.java`, and `LmdbNativeCostVector.java` describe the executable work to the existing pricing model. `LmdbNativeStrategyPreference.java` contains the fallback ordering when measured prices cannot decide. The source abstraction is `NativeLmdbQuerySource.java`; use its revision-valid metadata without consuming rows during pricing.

Tests live in the corresponding `core/sail/lmdb/src/test/java/.../evaluation/` package. The public SPARQL admission test proves routing through a real repository; the semantics test compares results; adapter and kernel tests establish ownership, cancellation, context filtering, arithmetic, and memory behavior. `.../benchmark/SparseJoinAggregateBenchmark.java` and `scripts/run-sparse-join-aggregate-benchmarks.sh` provide a reproducible end-to-end measurement path. Existing reports in `core/sail/lmdb/sparse-join-aggregate-results/2026-11-04/` describe the original opt-in head and must remain historical evidence.

## Plan of Work

First inspect how existing producers quote physical work and prove a safe cold choice. Inspect the source's exact metadata and compressed multiplicity APIs. Distinguish exact facts from estimates: logical statement cardinality alone does not prove overlap, fanout distribution, or density. Choose the smallest sound integration that makes the new strategy usable in normal planning and preserves established fallback behavior.

Add tests that fail against the original head for default availability, valid quotes, selected orientation or compressed traversal where supported, and automatic routing on a workload with demonstrated benefit. Test disabled or unavailable adjacency, unsupported query shapes, and insufficient memory. Run these tests before modifying production sources and preserve the failure log.

Implement the new behavior using the existing producer interface and memory ledger. Ensure physical variant identity includes any structural execution choices so calibrated prices cannot mix different recipes. Avoid scanning data just to price it. Refusal must occur before results escape, and resource cleanup must preserve the original failure. Keep the forceable public strategy for diagnostics.

Extend the benchmark to compare ordinary automatic execution, the feature-disabled baseline, and forced sparse execution. Include low and high fanout, asymmetric plane sizes, context multiplicity, and thresholds. Run correctness checks outside measured query intervals and capture actual execution witnesses. Warm steady-state measurements and first-use timings must remain distinct. Treat unsupported or noisy speed claims as limitations, not established improvements.

Update `core/sail/lmdb/sparse-join-aggregate.md` and the benchmark guide with the actual supported semantics, automatic selection behavior, opt-out, memory/fallback policy, telemetry, and measured tradeoffs. Produce a patch relative to the reviewed PR head plus complete changed source files. Do not commit, push, or post external comments without authorization.

## Concrete Steps

All commands run from the checkout root. This environment has JDK 25 at `/workspace/scratch/0c324f9a00f9/review-tools/jdk-25.0.5+8` and Maven 3.9.11 at `/workspace/scratch/0c324f9a00f9/review-tools/apache-maven-3.9.11`. The wrapper `../review-tools/mvn-review.py` configures that toolchain and the current command's network proxy without exposing credentials. Maven dependencies must use the workspace-local `.m2_repo`.

For a focused test, use:

    python3 ../review-tools/mvn-review.py -o -B -ntp -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=<selected tests> -DskipITs -Dformatter.skip=true -Dspotless.skip=true -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=2G -Djunit.jupiter.execution.parallel.enabled=false test > <verification log> 2>&1

Never add `-am` when tests are enabled. If the offline run fails solely for an uncached dependency, run once online to obtain it and then return offline. For dependency installation with tests skipped, use:

    python3 ../review-tools/mvn-review.py -o -B -ntp -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install > maven-build.log 2>&1

The required initial full-root quick clean install passed before this work. The exact final build and test commands are preserved in `core/sail/lmdb/sparse-join-aggregate-results/2026-11-04-integration/evidence/final-verification/commands.json`; final benchmark commands are preserved in that report's `evidence/final-macro/suite-commands.json` and `evidence/final-jmh/commands.json`. The passing counts and build identity are recorded in Outcomes & Retrospective above.

## Validation and Acceptance

A normal SPARQL query without the sparse enable property must return exactly the same binding multiset as the generic evaluator and expose `sparseJoinAggregate` as its actual native execution path when the selected physical recipe warrants it. Low fanout and unsupported inputs must retain a valid alternative. Forced sparse execution must remain callable for supported queries, and explicit opt-out must prevent its use. A quote must use metadata only and must not open or consume input cursors.

Existing kernel, semantic, and resource tests must remain green. New tests must cover any orientation, compressed context traversal, or supported shape added during this work. Allocation refusal must return through existing fallback with no leaked resources or partial output. The benchmark must validate every result and show both useful wins and adverse cases; its automatic route must no longer require opt-in. Final verification must include formatting, copyright validation for any new Java files, and a clean patch whitespace check.

## Idempotence and Recovery

Tests create isolated temporary stores. Benchmark scripts must write to a new result directory rather than overwrite historical reports. Build and test commands can be repeated safely. Keep all working tree edits local and preserve the original head as the comparison point. If a proposed optimization fails semantic or performance validation, remove that proposal while retaining the independently verified integration; do not weaken assertions or hide failures.

## Artifacts and Notes

Before implementation, `review-tests.log` records 153 tests, zero failures, zero errors, and zero skipped. `maven-build.log` records the original successful full-root quick install. New evidence will use separate logs to keep the baseline identifiable.

The test-first logs are `sparse-integration-admission-red.log` (one expected forced-strategy error with the feature property absent), `sparse-integration-fibers-red.log` (one expected failure: physical fibers must not expand to quad neighbors), `sparse-integration-constants-red.log` (one expected forced-strategy error for COUNT(1)), and `sparse-integration-cost-red.log` (five tests, two expected failures because the producer quote is null). The Python benchmark-summary self-check ran three tests successfully.

After correcting the manually constructed row fixtures, `sparse-integration-cost-contract-red.log` establishes the additional contract failures precisely: 14 cost tests with three failures for one-sided cold admission, orientation-dependent cold admission, and a limited consumer incorrectly discounting blocking work. The same run confirms the COUNT(1) compiler gap and passes the unchanged isolated descriptor-lifetime test that had shown a one-descriptor timing discrepancy in the broader run. The assertions were not weakened.

The subsequent broad run passed 408 tests. `sparse-integration-scoped-red.log` then captured three expected failures for absent scoped quotes and indistinguishable context-processing recipes; the accompanying ordered aggregate slice semantic test passed. The final `maven-build.log` records a successful clean module-and-dependency install with the benchmark profile. `sparse-integration-final-green.log` records 411 tests, one descriptor-count assertion failure, zero errors; every sparse test and all other selected tests passed. `sparse-integration-grouping-isolated.log` records all 69 tests in that unchanged grouping-cost class passing in isolation. The process-wide descriptor counter decreased from 140 to 139 in the broad run; no growing resource count was observed. The original failed output is retained in `initial-evidence.txt` as well. `sparse-benchmark-summary-tests.log` records four passing Python summary self-checks.

## Interfaces and Dependencies

Keep `LmdbSparseJoinAggregatePlan` as a `NativeAggregatePhysicalPlan.Producer` and `SparseJoinAggregateKernel.WeightedRows` as the kernel input interface unless measured source capabilities justify a compatible specialization. Use `KernelRuntime.AggregateMemory` and `MemoryAccount` for all retained primitive state. Pricing must return `NativeAggregatePhysicalPlan.Work` from revision-valid metadata or explicitly decline when that work cannot be established. No new third-party dependencies are authorized or required.

Revision note (2026-11-04): Created this plan to implement the user's requirement for a complete, normally integrated feature after reviewing the opt-in PR.

Final verification note (2026-11-04): `sparse-integration-complete-install.log` records BUILD SUCCESS (1:05). `sparse-integration-complete-tests.log` records 447 tests, zero failures/errors/skips (1:44). The capacity regression passed in its unchanged 13-test adapter class. `target/sparse-integrated-suite-commands.json` and `target/sparse-integrated-jmh-commands.json` record the exact final measurements. The packaging helper checked both alternative patches against their immutable bases in temporary indexes, compared resulting file contents and modes, validated every archive member and checksum, and preserved the real index before handoff.

Completion audit (2026-11-04): On the implementation follow-up, GitHub still reported PR head `03a16b126371ff8cc523c7b68ee1f875d867f298`. All 353 packaged files matched the working tree in content and mode, all standalone checksums passed, and the 370-member archive passed its CRC check. An independent source audit found the planned admission, costing, probe/replay, compiler and physical-fiber integrations present, with no concrete implementation gap. The remaining stale packaging entry and future-tense status were corrected. This follow-up changes documentation only; the existing build, 447-test pass and benchmark results remain applicable to the unchanged implementation.
