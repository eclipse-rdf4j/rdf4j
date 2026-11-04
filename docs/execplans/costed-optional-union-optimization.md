# Add guarded, costed OPTIONAL and UNION alternatives


This execution plan is maintained under `PLANS.md` and `AGENTS.md`. Its implementation branch is `GH-0000-lmdb-optional-union-optimization`, starting at `cce41a89a50258459da9aabe9498f91bc4771222` on `eclipse-rdf4j/rdf4j` branch `GH-0000-lmdb-predicate-guarantees`. The publication target is `HASMAC-AS/rdf4j`, with a comparison link targeting that upstream branch. Publication is authorized; creation of a pull request is left to the user.

## Purpose / Big Picture


SPARQL OPTIONAL preserves a left result when its right side has no accepted match. UNION concatenates results, including duplicates. These properties make seemingly ordinary join rewrites capable of changing answers. This change will let the existing cost-based optimizer compare additional equivalent OPTIONAL and UNION plans, while preserving duplicate counts, unbound variables, graph scope, correlation, and evaluation safety.

The observable improvement is that a query can choose between shared work and branch-specific selective work, and can order independent OPTIONAL branches using cost rather than only canonical text order. Eligible native candidate pruning will also compare its setup cost with repeated right-side execution and stay within the query memory budget. New tests must demonstrate that alternatives enter the production search and that results remain the same multiset (the same bindings with the same number of copies). The work does not replace the existing join enumerator or claim exhaustive enumeration of every equivalent SPARQL expression.

## Progress


- [x] (2026-11-04) Verify base and create isolated branch.
- [x] (2026-11-04) Capture logical and weighted-memo failing tests.
- [x] (2026-11-04) Implement and verify bounded logical search and proof registrations.
- [x] (2026-11-04) Complete full root quick clean install: all 125 modules succeeded, 3:00 wall time.
- [x] (2026-11-04) Preserve supported pure shared UNION inputs; retain legacy distribution outside those regions.
- [x] (2026-11-04) Verify OPTIONAL alternatives, hidden-input guards, and both directions of cost selection.
- [x] (2026-11-04) Finish native path, decoded-value accounting, real-repository activation, and memo cleanup after ledger closure.
- [x] (2026-11-04) Evaluate semantics, costs, and regressions against the exact base; resolve both confirmed prepass regressions.
- [x] (2026-11-04) Complete JMH measurement and preserve raw results; end-to-end timing remains inconclusive.
- [x] (2026-11-04) Review the complete implementation and prepare source, patch, and comparison target for publication.

## Surprises & Discoveries


The current packed rule program has cases for joins, filters, projections, groups, and differences, but no explicit UNION or LEFT_JOIN alternative generation. The existing OPTIONAL normalizer proves independence but chooses a canonical fingerprint order. Therefore a new costed search alternative is needed even when normalization already rewrites the source tree.

The experimental `LmdbNativeAccumulateJoin` provider is default-off, and the current integration fixtures use whole-query native fusion instead of that seam. Enabling its property alone would not establish that a real query uses the optimization. The native OPTIONAL execution path must be examined and tested directly before claiming a production benefit.

The checkout starts without a Maven command on PATH and with Java 17 as the default Java. An existing Java 25 and Maven 3.9.11 toolchain is available. The existing Maven cache is copied, not shared for writes, into the new checkout's `.m2_repo`.

The initial offline build lacked Jackson 2.22.3 and 3.2.3 BOMs. Direct Maven networking then failed DNS resolution because Maven did not consume the environment's configured proxy. A private settings file generated from the existing proxy environment resolves dependencies successfully; no repository dependency versions or network policies were changed.

RDF4J's inclusive-default-graph evaluation can return identical full SPO bindings from different contexts. The new differential fixture explicitly demonstrates two original rows and four rows after naive mandatory-BGP injection. Therefore injection additionally requires an explicit constant context and rejects the unqualified default case.

The required copyright script reports twelve pre-existing LMDB source/test header defects. The new test files have the required headers and signatures. Preserve this distinction rather than changing unrelated source files.

The real native scalar-to-row adapter consumes its remaining multiplicity when `multiplicity()` is read. A recording cursor must transfer this weight exactly once while also expanding duplicates for legacy callers that only use `next()`. An idempotent synthetic getter did not model this boundary; stronger tests cover both weighted and ordinary recording and replay.

Default grouped aggregation can lower a LEFT_JOIN directly into kernel IR, bypassing the factor-algebra seam. The first real repository activation test returned the correct 4,096-row bag but recorded no support reuse. A bounded, costed bridge through the existing PlanRows producer is therefore needed at aggregate lowering. Its admission is deliberately narrow: duplicate-sensitive COUNT, a direct complete VALUES input of two through 256 rows, and a known profitable distinct-key domain. Ordinary SELECT remains independently checked for bag parity; this change does not claim that every SELECT takes the new native route.

The matched evaluation comparison initially showed one additional failing test despite identical aggregate failure totals. `PackedInfeasibleContinuationTest` selected the last UNION in the saturated arena. The new distributed UNION correctly assures the endpoint because both arms contain the mandatory VALUES; the original nullable UNION retains its original masks. The fixture now identifies the unique authored SP/VALUES UNION structurally and retains its binding-boundary and scheduling assertions. Production binding contracts were not changed.

The native module already has `rdf4j.lmdb.test.maxHeap`, allowing its test fork to be capped without changing explicitly small child JVM heaps. `_JAVA_OPTIONS=-Xmx3g` is suitable for the matched core evaluation comparison but overrides those intentional child heap limits in LMDB tests. Final LMDB verification uses its supported heap property and leaves `_JAVA_OPTIONS` unset.

An unvalued constant-flagged statement term can observe an input binding without producing that binding. Ordinary output-variable intersection alone cannot prove safe movement. Differential regressions demonstrate one-to-two OPTIONAL results and three-to-two UNION results under unsafe movement; the pure-fragment guards must reject these hidden input dependencies.

## Decision Log


Decision (2026-11-04, Codex): retain the existing packed memo, proof registry, cost model, native executor, and DPhyp join enumeration. A memo is the optimizer's store of alternative expressions grouped by proven equivalent results. DPhyp is the existing algorithm that enumerates legal join combinations. The new work adds guarded alternatives to those structures instead of adding a second optimizer.

Decision (2026-11-04, Codex): bound logical alternative generation to expressions present when the original query has been encoded. Newly generated intermediates still receive normal physical planning, but do not recursively trigger inverse distribution and factoring. This prevents these rules from recursively enumerating distributive normal form or inverse rewrite cycles. It is a bound on new memo alternatives, not a universal linear-size guarantee for every materialized winner: the existing materializer can unfold shared child-group recipes. Tests and the final report must identify this search limit.

Decision (2026-11-04, Codex): use a shared package-private `PackedAlternativeSink` so separate UNION and OPTIONAL helpers insert alternatives through the existing same-group insertion and registered proof validation. Root alternatives inherit the source's semantic contract. New intermediate expressions receive fresh unknown cost metadata and the correct graph domain, not stale parent estimates or input contracts.

Decision (2026-11-04, Codex): inject a mandatory pattern into an OPTIONAL only for a proven duplicate-free, fully bound basic graph pattern. A basic graph pattern is an unprojected conjunction of statement patterns; it exposes every variable used in those patterns. General bag-valued inputs are excluded: injecting two equal left rows would multiply two outputs into four. UNION on the right of OPTIONAL is never distributed into separate OPTIONAL results, because that would emit unmatched left rows once per branch.

Decision (2026-11-04, Codex): require a constant context on every statement pattern used to prove injection multiplicity. Merely having a default planning graph domain does not prove set-valued statement results in RDF4J's inclusive-default mode.

Decision (2026-11-04, Codex): use no new dependencies. Follow test-first gates, retain initial failing reports, and publish only after semantic and execution-path review.

Decision (2026-11-04, Codex): keep aggregate routing and runtime admission on the same optimistic cost gate, then price every distinct key from immutable VALUES metadata before selecting PlanRows. All-distinct keys decline before per-key quotation. Charge quotation metadata, buffered rows, and retained RHS payload to the existing query-shared ledger. Runtime reservation refusal uses per-row native evaluation within the selected producer; it does not restart an already selected IR plan. Use the existing owner-close-aware aggregate reservation for memo payload so cancellation can close a partially recorded probe after the owning query ledger closes.

## Outcomes & Retrospective


Implementation and evaluation are complete. The final focused logical selection passes all 118 tests, including 23 UNION tests, 22 OPTIONAL tests, 35 normalizer tests, and the nine required-frame continuation tests. The native support suite passes all 26 tests, including default repository activation, exact weighted bags, memory fallback, decoded-input accounting, cancellation, and partially recorded memo cleanup after owner-ledger closure. All 77 added testcases have passing evidence: 50 in evaluation and 27 in LMDB. JMH confirms the intended activation and cheap-work decline, but its elapsed-time differences are small relative to measurement uncertainty, so no end-to-end speedup is established by this fixture.

The final matched native selection has 515 tests, 55 failures, and three errors. Its exact base has 488 tests with the same 55 failures and three errors. All 58 problem identifiers match; there are no new problems, missing existing tests, or baseline-only problems. Two actual prepass regressions found in an earlier comparison were fixed and pass in the final run. The repository is not a passing full suite at the supplied base, and these results must not be presented as one.

The initial full root quick clean install passed all 125 modules. The final clean reactor compiled and built 123 modules, encountered a transient disappearing file during one WAR assembly, and skipped one dependent packaging module. A reactor resume and a separate install of that skipped module completed successfully, leaving all 125 modules built. No source or build configuration was changed to address the temporary-file races. The final FILTER correction was then compiled, verified, and installed in evaluation, followed by the final LMDB selection.

An independent diagnostic also checked 13 lexical OPTIONAL scenarios against their explicit expected bags in four combinations: exact-base generic, exact-base native, implementation generic, and implementation native. All 52 bags match. This bypasses no semantic guard and does not change the existing tests; it separately reaches result checks that their pre-existing strategy-name assertions stop before. The duplicate-UNION case preserves both copies while selecting common-BGP factoring only on the implementation. This diagnostic used immutable snapshots from the matched native run before the final FILTER-normalizer correction, isolating the factoring change.

The pre-change logical run executed 30 tests with 17 failures and no errors. Missing JOIN/UNION alternatives, OPTIONAL alternatives, and proof registrations failed as expected; the unsafe-case guards passed. One parsed-OPTIONAL fixture incorrectly assumed that its root always carried a scope marker; it was corrected to check actual marker preservation, with an explicit scoped fixture for that guard. The native weighted memo test independently failed with expected multiplicity eight and actual multiplicity two. The native support fixture initially omitted initialization of native slots to UNKNOWN. After correcting the fixture, eight native tests ran with five expected failures: all result bags were correct, but repeated-key cases made 64 probes instead of one (65 instead of two with an unbound row). These failures preceded the support-window implementation. The first complete native run after implementation passed all 16 tests. Review then strengthened weight tests to use the real consuming row adapter, including ordinary unweighted callers; those additional regressions also pass.

## Context and Orientation


The shared optimizer lives in `core/queryalgebra/evaluation`. Immutable expression IDs and their equivalent groups are stored by `PackedExpressionInterner` in `src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed/`. `PackedQueryCodec` encodes the source query and saturates `PackedLogicalRuleProgram`, meaning it repeatedly applies enabled rules until no permitted work remains. `PackedJoinEnumerator` chooses physical plans using estimates, required bindings, and memory limits. The original expression must remain in its group when another expression is added.

`PackedNodeMetadataArena` records binding, graph-domain, replay, and proof information. Replay safety means an expression may be evaluated in a different equivalent plan without changing volatile or externally observable behavior. `PackedRuleProofs`, `PackedRuleGuardValidator`, and `OptimizerRuleCatalog` form a closed registry: each generated alternative must carry a known proof bit backed by an implemented rule descriptor. Unknown or unproved bits must continue to fail validation.

`OptionalBranchReorderSafety` already checks that adjacent conditionless OPTIONAL branches share only bindings assured by the mandatory side, do not introduce overlapping names, and have safe structures. An assured binding is present in every result; a merely possible binding may be absent. This distinction matters because an unbound SPARQL variable can be compatible with many right-side rows.

The LMDB implementation lives in `core/sail/lmdb`. `LmdbNativeAccumulateJoin` is an experimental batch provider. It can collect distinct correlation keys, evaluate a right-side fragment with that key domain, then rejoin the answers to every original left occurrence. Deduplicating candidate keys is safe only when the original result multiplicities are restored and unbound keys have an explicit fallback. Native `LeftJoinPlan` is another relevant execution route and must share the same cost and memory reasoning where practical.

Predicate-range guarantees on the base branch describe stored RDF term domains. They do not imply that every subject has a matching right result or that a predicate is unique. This implementation must not remove OPTIONAL using those assumptions. Conditions, SERVICE, subqueries, graph boundaries, and volatile expressions remain protected unless an existing explicit proof permits movement.

## Plan of Work


### Milestone 1: Establish reproducible failures


Start with the initial root quick clean install on Java 25, using the checkout-local Maven repository. Add focused tests in `PackedUnionAlternativesTest` and `PackedOptionalAlternativesTest` that compile against existing APIs and fail because a legal equivalent root alternative is absent. Evaluate decoded alternatives against explicit expected bags, including duplicates and unbound values. Add native execution tests that distinguish reachable batching from an unused provider. Capture the first Surefire report in `initial-evidence.txt` before subsequent runs overwrite it. Production edits begin only after the intended failures are observed.

### Milestone 2: Add bounded logical alternatives


Create package-private `PackedUnionAlternatives`, `PackedOptionalAlternatives`, and `PackedAlternativeSink` beside `PackedLogicalRuleProgram`. UNION rules offer join distribution across either UNION operand and inverse factoring of an identical common basic graph pattern. Preserve duplicate arms; do not treat UNION as idempotent. Restrict movement to pure, replay-safe expressions with compatible scope and graph domain. OPTIONAL rules offer safe sibling reordering using the existing independence proof and restricted mandatory-BGP injection. Existing forms remain candidates.

Wire helpers into `PackedLogicalRuleProgram` and freeze their original expression-count boundary in `setRootExpression`. Add separate named proof bits and implemented catalog descriptors, explanatory proof text, and any manifest or registry test updates required by the existing catalog. Validate original and generated plan membership, selection under different estimates, bounded growth, and rejection of unsafe cases.

### Milestone 3: Make candidate pruning costed and reachable


Inspect both `LmdbNativeAccumulateJoin` and native `LeftJoinPlan`. The factor-algebra execution seam is `LmdbNativeFactorAlgebra.join`; default aggregate IR lowering can bypass it. Add a narrowly costed aggregate PlanRows bridge using the existing lowering facility, and verify real default execution rather than enabling the bypassed experimental provider. Reuse `RightMemoProbe` for memoizing right results by distinct support key, with costed admission and a bounded outer window. This is per-distinct-key execution and replay, not a claim that all keys use one bulk right scan. SEMI and ANTI retain their existing existence-only short-circuit route; this increment handles INNER and LEFT payload-producing joins.

Use existing per-key native cost estimates, query-memory accounting, cancellation checks, and cleanup mechanisms. Admission must account for collecting outer rows, deduplicating keys, materializing right results, and preserving output multiplicity. Cheap indexed lookups and unknown estimates must retain per-row execution. Budget exhaustion must use a correct remaining-row fallback and release acquired reservations. Tests must hit the actual selected execution path, show reduced fragment or access work on a useful workload, and show that a cheap workload declines the batch route.

### Milestone 4: Evaluate and publish


Run focused rule, proof, differential, native, and memory/cancellation tests, then the relevant module suite where feasible. Review actual result bags and plan membership, not only elapsed time. Run a reproducible targeted performance check through `scripts/run-single-benchmark.sh`; measure complete prepared-query invocation cost and report RHS-work counters separately. Evaluation invokes optimization, precompilation, and native lowering again, so this measurement includes their overhead; parsing and fixture construction are excluded. It is not an execution-only or isolated planning benchmark. Inspect the complete diff for unintended changes, run the required copyright and formatting checks, commit with a `GH-0000` prefix, push to the HASMAC fork, verify the remote commit and ancestry, and return the comparison link to the user's exact base branch.

## Concrete Steps


All commands run from repository root, currently `/workspace/scratch/80ebb7463cab/rdf4j-optional-union`. Select Java 25 and Maven 3.9.11 in this environment:

    export JAVA_HOME=/workspace/scratch/0c324f9a00f9/review-tools/jdk-25.0.5+8
    export PATH="$JAVA_HOME/bin:/workspace/scratch/0c324f9a00f9/review-tools/apache-maven-3.9.11/bin:$PATH"

Use an equivalent Java 25 or newer JDK and Maven 3.9.11 on another machine. Initial build:

    set -o pipefail
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

The quick profile skips tests and formatting. The build must finish with `BUILD SUCCESS`. If offline resolution reports missing artifacts, retry the same command once without `-o`, then return to offline execution.

In this environment Maven also needs `-s /workspace/scratch/80ebb7463cab/maven-task-settings.xml`, generated privately from the existing `HTTP_PROXY` and `HTTPS_PROXY` values, and `MAVEN_OPTS='-XX:ActiveProcessorCount=4 -Xmx3g -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'`. The settings file is environment-specific and is not committed. The successful dependency bootstrap uses the same root build without `-o`; subsequent commands return to offline mode.

Focused test-first command:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedUnionAlternativesTest#joinOverUnionHasCostedAlternativeWithoutLosingOriginal --module core/queryalgebra/evaluation --retain-logs

The first run should fail on the absence of the equivalent UNION root. After implementation it should pass with the original JOIN still present. Persist the actual verify log and reports with `scripts/agent-evidence.py` in top-level `initial-evidence.txt`. Run the OPTIONAL and native selections similarly, recording their exact commands and outcomes below as they become available. Tests must never use `-am` or quiet mode. Root installs may use `-am` only when tests are skipped.

Before formatting, run `./checkCopyrightPresent.sh` from `scripts`. Format serially with Maven `process-resources` using `.m2_repo`; use the changed modules to avoid unrelated edits. No raw Maven command may overlap a named Maven workspace. The root agent coordinates builds while agents edit disjoint source files.

## Validation and Acceptance


Correctness requires equality of result bags for each newly admitted alternative, retaining duplicate UNION branches, duplicate left rows, unbound shared keys, and unmatched OPTIONAL rows. Unsafe RHS-UNION distribution, arbitrary bag injection, correlated or dependent OPTIONAL reordering, volatile operations, SERVICE, and unsupported graph/scope crossings must not appear among generated alternatives. A false OPTIONAL condition must preserve the original left row with its unmatched right bindings absent.

Search acceptance requires original and new expressions in the same semantic group, cataloged proof lineage, finite saturation, and a demonstrated cost choice between useful alternatives. A test that merely constructs a helper does not establish production integration. Native pruning acceptance requires a real path selection with work counters, correct memory-limit fallback, and cancellation cleanup. Cheap bound index probes must remain eligible for the existing path.

Performance acceptance is workload-specific: report measured execution work, planning work, elapsed-time methodology, and any regression. Do not promise a general speedup based on one fixture. The final branch must build, have no unexplained new test failures, and be published as a descendant of the verified upstream base.

### Recorded validation

| Gate | Result | Interpretation |
|---|---|---|
| Root compilation/package/install | 125 modules built across the clean reactor and successful packaging retries | Tests are skipped by `quick`; this is a build gate |
| Final focused logical selection | 118 tests, zero failures/errors | Includes all 50 added evaluation cases and both corrected normalizer/fence fixtures |
| Final support-join class | 26 tests, zero failures/errors | Includes actual default grouped-query activation and memory/weight/cancellation cases |
| Matched native selection | Base: 488 tests, 55 failures, 3 errors; branch: 515 tests, 55 failures, 3 errors | Exactly the same 58 problem IDs, no missing cases, all 27 added native cases pass |
| Broad matched evaluation selection | Base: 7,354 tests, 124 failures, 53 errors; initial branch: 7,403 tests, 124 failures, 53 errors | A generated-UNION fixture-selection issue was subsequently corrected and all nine cases in its class pass; a scheduling-sensitive base cancellation failure did not recur on branch |
| Lexical OPTIONAL diagnostic | 13 scenarios, four revision/engine combinations; 52 exact bags match | Independently checks result assertions hidden behind existing strategy-name failures |
| Generated catalog | Compiled renderer output matches the document byte-for-byte | Existing external research-source hashes remain unchanged |
| Formatting / headers | All 23 changed Java files formatted; all have SPDX/Codex headers | The repository-wide header script still reports 12 unchanged baseline defects |

The broad evaluation run preceded the last enclosing-FILTER regression test. Its 49 then-added cases all passed; the final focused run includes that 50th new case. The final native run fixes the two initially introduced failures, `lmdbCascadesCostsFiniteCodeTypeRewriteForDistinctAggregateUnion` and `lmdbCascadesPresentsFiniteFilterValuesRewriteWithFallbackPolicy`, without altering their assertions. FILTER child traversal temporarily suspends pure-fragment deferral and restores the same invocation-local context in `finally`, retaining ordinary legal distribution and independent sibling choices.

Two exclusions apply to the broad evaluation comparison. `OptimizerRuleCatalogTest#generatedDocumentationMatchesManifestAndResearchInputs` needs `papers2/papers/docs/sparql_query_rewrite_catalog.md` and `papers2/papers/docs/neumann-birler-umbra-query-optimizer-research.md`, which are absent. `PackedFrontierSubsetKernelContractTest` independently exhausts the capped 3 GiB heap at the exact base; the original 16 GiB fork request also exceeds this container's 8 GiB limit. Neither test was weakened or removed. The exact shared selection is:

    -Dtest=Test*,*Test,*Tests,*TestCase,!OptimizerRuleCatalogTest#generatedDocumentationMatchesManifestAndResearchInputs,!PackedFrontierSubsetKernelContractTest

The 24 native classes in the matched implementation selection are:

    LmdbNativeSupportJoinTest,LmdbOptimizerPipelineTest,LmdbNativeLeftJoinSweepTest,LmdbNativeLeftJoinCursorTest,LmdbNativeInnerJoinSweepTest,LmdbNativeAccumulateJoinTest,NativeAliasJoinMemoMultiplicityTest,LmdbNativeFactorizedRowsTest,LmdbNativeFactorizedCostTest,LmdbNativeFactorizedReorderTest,LmdbNativeOrderedFactorizedTest,LmdbNativeLexicalFactorizedRowsTest,NativeLexicalLeftJoinReplayTest,LmdbNativeOptionalFallbackContractTest,LmdbNativeLexicalOptionalConditionTest,LmdbNativeCompositeOptionalDifferentialTest,LmdbNativeScopedUnionValuesTest,NativePureUnionVisibilityBoundaryTest,LmdbQueryMemoryManagerTest,LmdbNativeKernelAggregateTest,LmdbNativeAggregateValuesCompilerTest,LmdbNativeIrAggregateParityMatrixTest,LmdbNativeNonCountAggregateTest,LmdbNativeRowCancellationTest

The base selection removes only the newly added `LmdbNativeSupportJoinTest`; its pipeline class has 58 existing tests versus 59 on the branch. Both use Java 25, Maven 3.9.11, the checkout-local Maven repository, `-DskipITs -Dformatter.skip=true -Dimpsort.skip=true -Djapicmp.skip=true -Denforcer.skip=true`, and `-Drdf4j.lmdb.test.maxHeap=3g -DargLine=-XX:ActiveProcessorCount=4`. Native `_JAVA_OPTIONS` is unset so child-JVM heap fixtures retain their intended settings.

### Measured performance

The complete [raw JMH results](costed-optional-union-jmh.json) contain all four parameter combinations and all 40 measured iteration values. Both modes run this implementation: `auto` uses normal admission; `disabled` uses the existing `rdf4j.lmdb.leftjoin.memo.enabled=false` override. This isolates the native support route. Logical-rule overhead and whole-branch before/after performance are not measured by this comparison.

Each combination uses two fresh JVM forks, one benchmark thread, three one-second warmups, and five one-second measurements per fork, with a 1 GiB fork heap and four active processors. The operation evaluates a prepared grouped COUNT query, consumes every result row, and sums the counts. Optimization, precompilation, lowering, and result consumption are included. Parsing, data loading, trial validation, and external serialization are excluded. Result caching is not requested. Lowering runs afresh before any compiled-kernel class-cache lookup, so the measured mode is not inherited from the other setup validation mode.

| Workload | Auto, ms/op | Memo disabled, ms/op | Auto mean difference |
|---|---:|---:|---:|
| Repeated keys | 19.901 ± 3.246 | 20.071 ± 3.009 | 0.85% lower |
| Cheap distinct keys | 14.904 ± 3.047 | 14.769 ± 3.080 | 0.91% higher |

Uncertainty is JMH's reported 99.9% interval half-width over ten measured iterations. The intervals are much wider than either mean difference. These measurements do not establish an elapsed-time speedup or a tight overhead bound. Per-fork means are 18.951/20.851 ms (repeated auto), 19.610/20.532 ms (repeated disabled), 14.224/15.584 ms (cheap auto), and 15.245/14.294 ms (cheap disabled).

The work/semantics observations are exact. Repeated input has 64 outer rows, four keys, and 32 values per UNION arm: 2,048 result groups of count two, equivalent to 4,096 logical rows. Each enabled trial records one support window and 60 memo reuses. Cheap input has 64 distinct keys and one value per arm: 64 groups of count two, equivalent to 128 logical rows. Each enabled cheap trial records zero windows/reuses and five cost declines across lowering attempts. All trial result bags and grouped counts match with the override enabled and disabled. Separately, the deterministic one-key source test reduces source probes from 64 to one while retaining all 512 logical results.

Reproduction through the supported runner, after selecting the documented JDK/Maven:

    unset _JAVA_OPTIONS
    MAVEN_ARGS='-o -B -ntp' RDF4J_BENCHMARK_MVN_THREADS=2 \
      bash scripts/run-single-benchmark.sh \
      --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeSupportJoinBenchmark \
      --method executeGroupedCount \
      --param mode=auto,disabled --param workload=repeated,cheapDistinct \
      --warmup-iterations 3 --measurement-iterations 5 --forks 2 \
      --jvm-arg -XX:ActiveProcessorCount=4 \
      --jmh-arg -w --jmh-arg 1s --jmh-arg -r --jmh-arg 1s \
      --jmh-arg -rf --jmh-arg json --jmh-arg -rff --jmh-arg support-join-jmh.json \
      --jmh-arg -foe --jmh-arg true

## Idempotence and Recovery


All work is isolated on the new branch. Re-running tests or quick installs is safe; preserve initial evidence and untracked artifacts before a tool refreshes report directories. Do not reset or modify the user's branch. If an alternative cannot meet a semantic guard, reject that alternative and leave the original available. If candidate pruning cannot acquire memory or justify its cost, continue through the supported fallback. A push is non-forcing and targets only the new fork branch.

## Artifacts and Notes


Build and test output, initial red runs, exact-base comparisons, and report snapshots are retained under `logs/optional-union` in the implementation checkout. This plan contains the compact final validation record; the complete JMH JSON is committed beside it. Full source and the commit diff are the publication deliverables. No test result is recorded as successful before its process and reports complete.

## Interfaces and Dependencies


`PackedAlternativeSink` has `int addAlternative(int sourceId, int operator, int payload, int[] children, int count)` and `void addRuleMask(int expressionId, long proof)`. Its implementation delegates to `PackedLogicalRuleProgram` so insertion, metadata inheritance, and proof validation stay centralized.

The UNION helper constructor accepts the relation and payload interners, `PackedNodeMetadataArena metadata`, and `PackedAlternativeSink sink`. The OPTIONAL helper additionally receives `PackedBindingSetArena bindingSets` and the symbol table needed to prove complete binding domains. Both expose `apply(int expressionId)` and a method to capture the original relation count before saturation. Helpers are internal and add no dependencies. `QueryModelNormalizerOptimizer` receives an additive `@InternalUseOnly` boolean constructor: the no-argument constructor retains its previous behavior, while LMDB preserves shared JOIN/UNION forms for costed search.

Revision note (2026-11-04): created the executable plan after base verification and architecture inspection, before production changes. The production reachability finding expands the native inspection to `LeftJoinPlan`; it does not authorize weakening semantic or memory guards.

Revision note (2026-11-04): recorded the inclusive-default-graph counterexample and narrowed the injection proof. Selected the actual factor-algebra execution seam and clarified that its bounded support-key reuse is memoization, with INNER/LEFT scope. Recorded the dependency bootstrap and pre-existing header-check findings.

Revision note (2026-11-04): preserved actual failing reports in `initial-evidence.logical.txt`, `initial-evidence.native.txt`, and the combined `initial-evidence.txt`; began production logical/proof work only after the intended failures. Added a pipeline-level test because eager pre-search UNION distribution can otherwise discard the shared VALUES form that the new BGP factoring rule cannot reconstruct. The initial affected modules compiled after test-fixture API repairs and dependency bootstrap. The subsequent full root quick clean install completed successfully for all 125 modules; this was a compilation/package/install gate with tests skipped, not a full-repository test result.

Revision note (2026-11-04): recorded the successful 16-test native run and subsequent review findings. Strengthened actual consuming-weight and hidden-input regressions before accepting production changes. The UNION cost-selection fixture now uses physical-join cost refinement, since concrete join implementations replace logical local costs.

Revision note (2026-11-04): all 45 new logical tests passed after resolving hidden SP reads, incomplete VALUES headers, and physical-cost fixture refinement. The broader logical run has 112 tests and one missing external research-source fixture, with no assertion failures. The initial root build succeeded for all 125 modules. Native review found missing decoded-input accounting inheritance on the new forks; a focused regression is being run before that fix.

Revision note (2026-11-04): narrowed the normalizer option after two regression tests showed blanket suppression would discard existing FILTER/Extension distribution. A postorder identity-set shape policy now defers only supported pure regions; the original legality proof handles all fallback regions. All 34 normalizer tests pass. The decoded-input inheritance regression first failed at 6,478 expected bytes versus zero; the two fork-policy inheritance calls make it pass.

Revision note (2026-11-04): the broad evaluation selection completed 3,068 tests before the test JVM was killed by the 8 GiB container limit; the repository requests a 16 GiB test heap. An isolated exact-base checkout reproduces the unrelated assertion/error families under a 3 GiB heap, but one existing Frontier subset contract test also exhausts that capped heap. Complete matched comparison therefore isolates that resource-heavy class and the absent external research-source check as explicit exclusions. No test assertions are weakened.

Revision note (2026-11-04): the complete native comparison exposed two actual finite-value rewrite regressions. A focused enclosing-FILTER regression first failed at the missing distributed UNION. Scoped suspension/restoration of pure-fragment deferral restores the existing rewrites while preserving an unfiltered sibling's shared form. Final logical verification passes 118 tests; final native verification has exactly the base's 58 problem IDs, with all 77 added cases passing across the modules. Separate native/generic diagnostics confirm all 52 lexical OPTIONAL bags. JMH completed all four combinations and is preserved in full; its mean timing differences are inconclusive, despite confirmed repeated-key reuse and cheap-work decline.
