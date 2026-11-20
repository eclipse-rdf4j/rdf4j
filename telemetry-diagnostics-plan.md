# Make LMDB query telemetry explain slow physical execution

This is a living ExecPlan maintained according to `PLANS.md`. It covers the user's request to implement the ten telemetry improvements discussed on 20 November 2026. The starting commit is `2b53e4d637b698b1f5eda9d842e60e384b59ebe9`, shared by upstream and the HASMAC-AS fork's `main`. Implementation is isolated on `GH-0000-lmdb-telemetry-diagnostics`; no issue number was supplied.

## Purpose / Big Picture

After this change a Telemetry explanation should connect slow work to the native physical stage that incurred it, disclose how a selected strategy's estimates compared with its work, retain expensive abandoned attempts, and separate execution from result consumption. Workbench should present the structured evidence and meaningful comparisons. Existing query results, semantic fallback, and ordinary non-telemetry execution must remain correct.

A physical stage is an actual scan, join, expression, materialization, or worker task. Several logical SPARQL operators can be fused into one compiled loop. Logical solution multiplicity is the number of solutions represented by a physical tuple; it must not be confused with the number of tuples or values actually processed. A counter's unit, owner, scope, origin, and completion state must be explicit. An unmeasured value is not zero. Partial execution must not claim a complete cardinality.

## Progress

- [x] (2026-11-20) Inspect implementation and verify upstream snapshot.
- [x] (2026-11-20) Clone HASMAC main and isolate branch.
- [x] (2026-11-20) Establish build and failing regression contracts; the initial root clean install succeeded and native, resource, profile, lifecycle, and frontend regressions failed for the intended missing behavior.
- [x] (2026-11-20) Integrate the shared profile, native/resource producers, lifecycle, transport, and presentation.
- [x] (2026-11-20) Implement structured profiles and truthful metrics; immutable/bounded core contracts and model converter suites pass.
- [x] (2026-11-20) Attribute native stages, attempts, and outliers; 183-test focused native/resource pass is green, including interpreted/compiled parity and cold/warm/pending code generation.
- [x] (2026-11-20) Finish reservation-owner memory breakdown; the 190-test owner/resource/native pass is green, including exact current/peak attribution, capacity bounds, foreign reclamation, and grouped query cleanup.
- [x] (2026-11-20) Integrate lifecycle timing and consumption options; 210 Sail base tests before final review, followed by 11 focused explanation cases after the final fixes, 69 HTTP client cases (4 pre-existing disabled cases), 24 server/transaction cases, and 22 Workbench Java cases pass.
- [x] (2026-11-20) Present findings and semantic profile comparison; 1,163 classic frontend tests pass, followed by focused context/partial-state regressions.
- [x] (2026-11-20) Complete review-driven failure regressions, measure overhead, and pass the final header, format, and full reactor quick-install gates (2m48).
- [x] (2026-11-20) Commit and publish the implementation branch; verify the remote source tree and patch application against the original main tree.

## Surprises & Discoveries

Telemetry already has extensive native path, adjacency, factorization, filter, and parallel aggregate evidence. This work extends and associates it instead of replacing working instrumentation. Generic Telemetry currently coerces missing fixed counters to zero. Its cost error ratio mixes abstract cost with milliseconds, and its confidence fields describe child-q-error dispersion rather than confidence. The Workbench Telemetry heatmap highlights result rows. An aggregate with one output can therefore look cheap.

The native access observer can report whether scan counts are physical or returned-row proxies, but the runtime-plan callback drops that flag and combines all measurements into source-wide buckets. Native decisions retain the first eight invocations; retries replace the previous ranking. Failed attempt counters intentionally never enter successful-result counters, so a separate incurred-work ledger is necessary. Result consumption in SailSourceConnection advances rows without reading their values or serializing them.

Memory reservations reclaim or refuse; they do not wait for another cursor to release memory. Reclamation can concern another query's structures. Parallel worker overlap is a sum of pairwise overlaps, not wall time or CPU use. These distinctions must survive the new report.

## Decision Log

Decision: use a shared immutable `QueryExecutionProfile` in `core/query`, with a bounded request-owned collector and typed phase, stage, metric, attempt, and worker records. Rationale: all producers and renderers must use the same data without parsing strings. Existing string diagnostics remain available for compatibility. Date: 2026-11-20.

Decision: expose the collector through `QueryExplanationContext.telemetry()` only for Telemetry. Capture it explicitly in workers and query-owned sources. Producers update primitive local counters and publish snapshots at boundaries or register lazy snapshot suppliers. Rationale: avoid global-counter subtraction, per-row synchronization, and repeated growing text rendering. Date: 2026-11-20.

Decision: preserve query failure propagation and attach collected diagnostic data rather than silently turning evaluation failures into success. Timeout and early completion carry explicit status. Rationale: existing callers and regression tests require original failure semantics. Date: 2026-11-20.

Decision: result consumption is explicit and defaults to advancing rows. Reading values or serializing must be requested and identified in the report. Serialization work remains separate from network delivery. Rationale: profiling must not silently change the workload it measures. Date: 2026-11-20.

Decision: no new third-party dependencies. Use existing JDK, Jackson, RDF4J writers, and frontend utilities. Date: 2026-11-20.

Decision: keep background compiler durations scoped to the compiler thread, while preserving the lookup's original cache/availability decision. Rationale: a pending lookup can become ready before rendering, but its query did not execute that compiled kernel. Only synchronous compilation/wait belongs inside query lookup time. Date: 2026-11-20.

Decision: defer cost-model success reporting for cursor-wrapped native observations until close succeeds. Rationale: a new regression showed failed cleanup altered the learned strategy price after end-of-stream. Diagnostic retention and successful training must remain separate. Date: 2026-11-20.

Decision: guard semantic comparisons using context in both profile properties and physical stage metrics/properties, including adjacency revision and serialization format. Rationale: snapshot context is recorded by the pinned-view producer, not solely on the root. Date: 2026-11-20.

## Outcomes & Retrospective

All ten feature areas are implemented. Shared schema contracts, truthful generic metrics, lifecycle phases, explicit value consumption, native/resource producers, protocol propagation, and Workbench presentation have passed focused regressions. Review-driven regressions now cover timeout races, partial writer cleanup, nested query ownership, elastic parallel worker counts, and unmeasured filter timing. Production edits are frozen. The final combined LMDB regression passed all 217 tests with zero failures, errors, or skips; both exploratory JMH runs completed with all twelve cells and setup assertions passing, and the final reactor quick install passed in 2m48. Header checks, generated signatures, and `git diff --check` also pass. The implementation is published on the HASMAC fork branch. This section records actual coverage and limitations without implying every desired measurement is exact.

Integration evidence now includes real native adjacency row/value runs (128 identical consumed rows; only explicit values consumption forces output lexical resolution), exact JSON UTF-8 byte counts through the registered writer, transaction-thread propagation, partial cancellation status, physical/proxy access evidence, late attempt outliers, worker ownership, and real spill-sort file traffic. Root/module logs retain each initial failure and the corresponding green run. A separate detached baseline worktree at the starting SHA has passed its root quick clean install; the same JMH harness is present on both revisions for the final comparison.

Rendered UI review was attempted with the Browser workflow. The connected extension could not be initialized because the originating browser context is unavailable. The cloud CUA fallback could not reach the local server (`ERR_CONNECTION_REFUSED`), and inspection of its generated error page was blocked because `chrome-error:` is not an allowed protocol. No visual browser pass is claimed. The production renderer, template controls, serializer, and comparison have passing automated regressions, and a standalone QA fixture remains available outside the repository for later visual inspection.

The benchmark measured the same source fixture in a clean base checkout and the branch, with native evaluation and adjacency enabled. Native route labels stayed `indexedCount`, `nestedLoop`, and `batch`. Before/after comparisons are made within evaluation mode because public query iteration and internal explanation consumption have different allocation behavior. All timing intervals overlap widely. Telemetry allocation increased by approximately 18.2% for COUNT, 5.0% for FANOUT, and 0.5% for VALUES; ordinary VALUES allocation increased by approximately 2.5%. These short single-fork measurements establish observed collection cost and a reproducible baseline, not production performance guarantees. Full numbers, uncertainty, settings, and raw JSON are retained with the benchmark report.

## Context and Orientation

The root Maven project requires Java 25. `core/query` contains `GenericPlanNode`, `Explanation`, and the compilation-scoped `QueryExplanationContext`. `core/queryalgebra/model` converts the logical query tree to explanation nodes. `core/queryalgebra/evaluation` implements generic evaluation and iterator instrumentation. `core/sail/base` owns the explain request, source acquisition, optimization, compilation, result consumption, and cleanup.

The native implementation lives under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. `LmdbNativeRuntimePlan` witnesses actual runtime choices. `LmdbNativeExplain` records path and selection details; `LmdbNativeDispatchLoop` opens candidates and handles declines. `LmdbNativeAttemptMetrics` keeps committed work separate from abandoned attempts. `codegen/KernelWorkCounters` contains worker-local primitive counts. The parent LMDB package owns `ValueStore`, `LmdbQueryMemoryManager`, and adjacency read-view acquisition.

Workbench sources live under `tools/workbench/src/main/frontend/classic`; the highlighter renders explanations and `query.ts` owns query interaction and comparison. HTTP explanation transport is in `core/http/client`, `core/http/protocol`, and `tools/server-spring`. The query result writer registry is in `core/queryresultio/api`.

## Plan of Work

### Milestone 1: establish executable contracts and a structured profile

Run the repository root quick install before tests. Add focused regressions that fail against current behavior: unknown versus zero, truthful cost and confidence metrics, per-access provenance, costly failed attempts, later outliers, lifecycle phases, memory peaks, and value consumption. Preserve initial evidence. Add immutable profile records and a bounded collector, with deep snapshot isolation, finite numeric values, stable stage identity, status, and explicit dropped-event counts. Attach the snapshot to the root generic explanation. Render concise text while preserving JSON structure.

### Milestone 2: instrument actual native work and selection

Extend native runtime/access observers compatibly to carry physical/proxy status, access-site identity, completion state, and attributable elapsed work. Retain bounded summaries across all invocations, including costly and recent examples. Keep attempted work separate from committed result counts. Link estimated features and times to observed values with matching units. Add stage identity for kernel access/expression work without breaking fused execution. Snapshot final data without publication throttling. Existing non-telemetry calls must remain cheap and must not register profile sources.

### Milestone 3: attribute resource and result costs

Capture query-owned value handle, dictionary/cache, inline, and lexical decode counters at the responsible operations; distinguish process context from query evidence. Track query high-water reservations, spill/reclaim causes, bytes, and operator ownership. Expose actual adjacency read-view state and acquisition outcomes. Reuse parallel worker timing and work arrays to report worker occupancy, skew, merge costs, and partial termination; do not relabel pairwise overlap as CPU utilization. Register local snapshots once and avoid per-value shared updates.

### Milestone 4: integrate lifecycle timing and explicit consumption

Instrument source acquisition, optimization, native compilation/opening, iteration, requested value consumption, and cleanup in `SailSourceConnection`. Maintain terminal status on success, interruption, timeout, and failure while preserving existing exception identity and cleanup guarantees. Add explicit result-consumption request options with ordinary row advancement as default. Pass options across local query, HTTP, transaction execution, and Workbench paths; reject unsupported combinations clearly. For serialization use the existing writer registry and a counting sink, and report that network delivery is outside the measurement. Record query configuration and consumed-row context needed for comparison.

### Milestone 5: present diagnostic findings and comparable profiles

Use typed stages and metrics for physical-stage inspection, appropriate heatmaps, concise evidence-based findings, and semantic comparison. Keep alternative strategies' estimates distinct from measured alternatives. Mark unknown, proxy, sampled, fused, and partial data clearly. Comparison must align stable semantic origins, disclose route changes, and compare matching units and scopes. Add frontend regressions for count kernels, changed routes, missing data, and values beyond JavaScript's exact integer range.

### Milestone 6: validate and deliver

Run targeted module tests first, then relevant explanation, LMDB native route, cancellation/lifetime, memory, and Workbench tests. Verify rows and solution multiplicities on ordinary, native, and telemetry runs, including aggregate, factorized, GRAPH/OPTIONAL, LIMIT, cancellation, and parallel cases. Add a reproducible benchmark or experiment using existing harnesses to compare telemetry overhead and normal execution; make no speed claim without measurements. Check headers, format, rebuild, and rerun concrete affected regressions. Commit incremental verified work, push to the HASMAC branch, verify remote SHA, and provide a complete diff patch plus the branch/compare URL.

## Concrete Steps

Work in `/workspace/scratch/96ffdb7d9155/rdf4j`. Use `JAVA_HOME=/workspace/scratch/6c7f03e66aab/toolchain/jdk-25` and Maven from `/workspace/scratch/6c7f03e66aab/toolchain/maven/bin`. The copied local dependency cache is `.m2_repo`.

Initial build command, with output retained in `maven-build.log`:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run tests at their owning module after the install, for example:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbNativeDiagnosticTelemetryTest verify

Tests must not use `-am` or `-q`. Build dependencies with tests skipped before focused downstream tests. Coordinate parallel editing with one owner per file and serialize Maven builds that write the same target directory. Frontend tests use the Workbench package scripts after its TypeScript build. Specific test selections, reports, and commands will be appended as each milestone completes.

## Validation and Acceptance

A Telemetry JSON explanation must contain an immutable execution profile with explicit status and stage-associated evidence. A native access with missing physical counts must be reported as proxy/unknown rather than measured zero. An expensive declined strategy must remain visible separately from the winning strategy's committed rows. A later correlated outlier must be retained without unbounded report growth. A timeout must retain available partial evidence and close resources. A normal query with telemetry disabled must produce the same results and must not create a collector.

Value-reading and serialized modes must actually exercise value decoding and the selected writer and report their scope; the default must remain lazy. Memory attribution must distinguish reservations from actual payload, and worker occupancy from CPU time. Workbench must render the structured profile safely and compare stable semantic stages. Acceptance includes green targeted regression reports, explicit coverage limits, and measured overhead for representative queries.

## Idempotence and Recovery

All changes are confined to the new branch and checkout. Never delete unrelated untracked files or modify the other task workspaces. Failed builds retain logs; missing dependencies are fetched by retrying the same command online once, then returning offline. Preserve red-test evidence before reruns overwrite reports. If a later integration breaks a previously verified milestone, fix the underlying mismatch and rerun its focused tests before proceeding. Never force-push or update fork main for this work.

## Artifacts and Notes

The initial build log is `maven-build.log`. Initial regression evidence belongs in `initial-evidence.txt`; subsequent focused logs remain beside it or in task-specific log directories. The final patch is generated from the recorded base SHA and must be checked against a clean base before delivery. Full changed source is available on the pushed branch; the patch is a separate download.

## Interfaces and Dependencies

`QueryExecutionProfile` lives in `org.eclipse.rdf4j.query.explanation`. Its immutable records include `Metric`, `Phase`, `Stage`, `Attempt`, and `Worker`. `Metric` carries a typed number, unit, origin, state, and scope. A stage carries stable semantic identity, logical origin, engine/strategy, terminal state, metrics, and properties. A collector accepts boundary events, stage/worker replacement, bounded attempt history, properties, and lazy `Supplier<Contribution>` snapshots from query-owned producer objects. `QueryExplanationContext.telemetry()` returns this collector or null. Producers must capture the collector explicitly before scheduling workers. No process-global counter snapshot subtraction is used for query-local claims.

Plan created 2026-11-20 to preserve the complete authorized scope and implementation contracts before production edits.

## Verification Evidence

Focused suites use the installed artifacts in the checkout-local Maven repository and JDK 25. The final reactor build uses the quick profile, which compiles production and test sources but skips test execution; executed regressions are recorded separately below. Intermediate failed runs are preserved locally as evidence, not included in the delivery patch.

| Area | Latest verified result | Local evidence |
| --- | --- | --- |
| Shared profile, generic plan, consumption options | 55 passed | `telemetry-core-boundaries-final-green.log` |
| Query-model converter | 28 passed | `telemetry-model-final-green.log` |
| Sail base lifecycle | 210 passed before final review; 11 explanation cases passed after final failure-handling changes | `telemetry-base-final-green.log`, `telemetry-review-base-green.log` |
| Actual serialization and unsupported writer handling | 2 passed | `telemetry-review-serialization-green.log` |
| HTTP client propagation | 69 cases, 65 passed and 4 previously disabled | `telemetry-http-options-green.log` |
| Server and transaction propagation | 24 passed, including actual JSON serialization | `telemetry-server-serialized-green.log` |
| Workbench Java | 22 passed | `telemetry-workbench-final-green.log` |
| Final native/adjacency/resource integration | 217 passed, zero skips | `telemetry-resource-boundaries-green.log` |
| Entire Maven reactor | Quick install succeeded in 2m48; tests compiled but not executed by this profile | `telemetry-reactor-final-build.log` |
| Formatting and headers | Explicit Spotless check passed for all ten changed modules; copyright and agent-signature checks passed | `telemetry-format-final.log`, `telemetry-copyright-final.log` |
| Overhead experiment | All twelve cases passed setup checks; time/allocation results and limits documented | [Benchmark report](core/sail/lmdb/benchmark-results/telemetry-2026-11-20/README.md) |
| Classic frontend | 1,163 passed before final context fixes; final 43-test affected selection passed | `telemetry-frontend-classic-suite-path.log`, `telemetry-frontend-final-green.log` |

The initial full frontend invocation omitted the toolchain from PATH and produced 107 environment-related failures. The corrected invocation produced the 1,163-test green result above. This record uses the corrected run and does not imply the failed invocation passed.

Final review regressions were observed before production changes: native cleanup incorrectly trained a successful cost sample; sampled or absent filter timing appeared measured; nested explanations inherited an outer resource owner; balanced invocations with different worker counts produced false skew; timeout races swallowed real failures; and consumer close failures were marked complete. The fixes retain the original exception, keep unsuccessful work separate from learning, isolate query owners, aggregate skew within invocations, and retain unavailable/partial timing states.

## Delivery

The implementation commit is `c7deb179289b48ef3d1e91d45c9cf341479f22c1` on `HASMAC-AS/rdf4j` branch `GH-0000-lmdb-telemetry-diagnostics`, directly based on main commit `2b53e4d637b698b1f5eda9d842e60e384b59ebe9`. GitHub's implementation tree `0990dad496cf12a4ef086c871c4b74180c8925a3` matches the tested local source tree exactly. A final documentation-only commit records this completed delivery.

The local remote had no write credentials, so publication used the user's connected GitHub account. No fork-main update or force push was used. Full source, tests, the guide, and benchmark JSON are included; transient build/test logs remain local.

The complete binary-capable patch was applied to an isolated Git index initialized from the original main commit. Its resulting tree matched the implementation commit exactly. After the documentation-only update, the delivered patch is regenerated from the same base against the final branch head and the same tree-equality check is repeated.

[Complete source](https://github.com/HASMAC-AS/rdf4j/tree/GH-0000-lmdb-telemetry-diagnostics) · [Compare against main](https://github.com/HASMAC-AS/rdf4j/compare/main...GH-0000-lmdb-telemetry-diagnostics)

## Follow-up: PR #6467 merge and CI repair

The user requested resolution of conflicts with main, repair of GitHub CI failures, and verification that telemetry works correctly after integration. The published starting head is `fbbd61f04501397ba43bf7240a59f4bf32475538`; the issue is now #6466 and all follow-up commit messages use `GH-6466`. The existing branch stays attached to PR #6467.

### Follow-up progress

- [x] Inspect upstream merge and CI failures.
- [x] Reproduce failures and preserve test evidence.
- [x] Resolve conflicts and repair underlying causes.
- [ ] in_progress: Verify affected modules and integration contracts.
- [ ] Publish fixes and inspect final CI.
- [ ] Verify delivered source and patch completeness.

At investigation start, GitHub reports PR #6467 as unmergeable (`dirty`). PR verify run `44809140525` failed; mapped-pages, QEMU durability, generated SPARQL sources, and Hugo checks passed. No failures are suppressed or tests disabled to make the branch green. Preserve prior untracked evidence logs. Merge upstream main into the existing branch without rewriting published history; use the connected GitHub account for publication if the shell remote still lacks write credentials.

Follow-up merge target: upstream main `4de0c1d63238640cc024d7c66a744ccf8bb2a871`. Three textual conflicts were resolved in SailSourceConnection, AbstractQueryRequestHandler, and Transaction. Compilation remains before slow-log snapshot capture and retains its Telemetry timer. HTTP requests retain both explanation options and query logging metadata. Transaction explanation tasks use the main branch's context-propagating submitQuery wrapper and activate captured explanation options inside it. The merge remains uncommitted while validation runs.

Initial CI inventory: general unit job136543176709 fails two MemoryStore explanation assertions; slow job136543177521 fails two GraphExplorerNativeVariantsTest access-work bounds. LMDB job136543177297 reports 12,873 tests, five failures and 25 errors: 24 IndexedCountParityTest errors from the dataset description, four LmdbNativeAliasJoinWorkTest failures from duplicated text counters, and one failure plus one error in the native decline census cancellation coverage. All six original browser E2E jobs and frontend-unit-tests passed. The merged root quick clean install succeeded in 2m33; this profile compiles tests but does not execute them.

Local red evidence is preserved separately before each fix. Dataset regressions produced three tests, two failures and one error: a legitimate null/default graph selector caused an NPE, long graph names were truncated, and equivalent graph sets depended on insertion order. Telemetry now describes the complete Resource graph identities canonically, preserving blank-node names, explicit dataset selection and the repository-default distinction. The full Sail base module then passed 662 tests with no failures, errors or skips (`telemetry-ci-base-green.log`).

The unchanged MemoryStore assertions reproduced two failures locally. Their replacement checks real recorded counters, absent unavailable metadata counters, root-only completed consumption, and the absence of invented confidence values at every explanation level. The full QueryPlanRetrievalTest class passed 29 tests with two pre-existing skips (`telemetry-ci-memory-explanation-green.log`). The original general-unit job skipped 36 downstream modules after MemoryStore failed; final verification must cover those modules.

The access-site text regression reproduced a legacy aggregate parser observing 20 matched rows where the aggregate held 10. Per-site breakdown labels are being separated from the established aggregate labels; structured per-site metrics and original physical-work bounds remain unchanged. Native cancellation is being reproduced independently: multiple observations of the same owned cancellation must not become a cleanup failure, while actual cleanup failures and foreign cancellation signals must still propagate.

The access-site regression and unchanged AliasJoinWork suite now pass 28 tests. The native cancellation reproduction produced 25 cases with two failures and seven errors, including the existing compiled-kernel census failure. Additional close-boundary cases exposed a foreign cancellation lost after normalizing an owned signal; genuine cleanup-error assertions already pass. Repair remains confined to ownership-aware native cancellation handling rather than broadening the generic timeout catch.

All 296 server-spring tests passed after the merge. Test-only extensions then passed three focused cases and 33 context cases, checking simultaneous explanation options and HTTP/transaction logging context, caller restoration, and same-worker restoration after both success and failure. No HTTP production change was needed beyond the merge resolution.

The full local MemoryStore command was terminated by the operating system (exit 137) during RealEstateThemeQueryCountTest after 817 reported cases with no assertion failures or errors. Its configured 8 GiB fork ran alongside native compilation/tests in a roughly 10 GiB workspace. Preserve this incomplete result (`telemetry-ci-memory-module-green.log`, `telemetry-ci-memory-module-initial-evidence.txt`); it is not a passing full-module claim. Serialize subsequent large local suites and use final-head GitHub CI's separate runners for complete general and slow-test gates.

Independent review identified a comparison edge case related to context identity: the shared collector bounds long property strings, so equal truncated dataset or workload context is insufficient evidence of compatibility. Add a focused frontend regression and conservatively disable comparison when required context is truncated, retaining report bounds.

The comparison regression failed before the guard (`root.dataset` incorrectly emitted a 2x ratio). The TypeScript build and all 15 profile tests now pass. Each of 11 workload-context keys is tested independently at both root and stage locations; multiple truncated fields emit one bounded explanation, while truncation of an unrelated descriptive property does not block valid ratios.

The first complete focused native run passed 449 of 450 cases, including all 344 indexed-count parity cases and all native ownership/cleanup assertions. It exposed a second timeout boundary in the existing census: IterationWrapper.next explicitly observes thread interruption but throws an untyped NoSuchElementException. Two explanation regressions reproduce that exact error and verify that an ordinary exception with the same message still propagates. Three direct iterator tests then reproduced the missing typed cause. IterationWrapper now preserves the existing exception type/message and attaches an InterruptedException cause; genuine close failures still escape before that conversion. The full common-iterator suite passed 122 tests. This uses the existing Java exception constructor and needs no public exception subtype or broader generic catch.

Native review also found the cooperative false-return path can attach a foreign cancellation during finish without entering the existing catch block. Two focused cases are being reproduced before extending ownership classification to that common finish boundary. Keep all genuine close errors and unrelated cancellation signals observable, and close each input once.

The cooperative-stop reproduction used the corrected fixture that directly constructs NativeBareRowsIteration (the earlier bare-fragment helper could select the bulk row implementation). The two-case red exposed the bare-root path; both common finish boundaries now inspect the retained cancellation outcome after cleanup. The complete native focus passed 108 tests, including all seven unchanged census tests, 30 direct native-root cases, and 71 existing ownership/cleanup cases. The census now reaches all calibration runs and took 414 seconds, rather than aborting at its first leaked cancellation exception. All 344 indexed-count parity cases passed in the preceding run.

The installed common-iterator fix passed the full Sail base suite (664 cases). A further direct regression showed an external interrupt ending hasNext could bypass the loop body's cancellation flag, finalize a writer and publish COMPLETE. Two cases failed before a single post-loop interrupt observation was added. Both focused cases and the full 221-case SnapshotSailStoreTest class now pass: cancellation retains PARTIAL consumption without writer finalization, and a real writer-close error retains its original identity and FAILED status.

Review of the next() boundary identified a matching native GROUP case: a result can be cached by hasNext, cancelled before next, and then rejected with an untyped NoSuchElementException. A six-case regression uses a genuinely materialized COUNT result and preserves the existing stop/no-extra-row semantics, plain exhaustion/explicit-close behavior, and real/foreign cleanup failures. Only a successfully classified cancellation will latch the state used to attach a standard cancellation cause.

The GROUP next-boundary reproduction failed in exactly its two cancellation cases; normal exhaustion, explicit close, genuine cleanup failure and foreign cleanup cancellation already passed. The minimal latched-state fix then passed all six cases and the final 73-case native ownership/cleanup selection. No native exception graph is retained after cancellation. Final source review and copyright validation are complete; formatting and a full incremental quick install precede the unchanged Workbench range77 regression and full GraphExplorer suite.

Final copyright and formatting gates passed; the complete merged root quick install passed in 2m30. The unchanged two-case Workbench range77 regression passed with exact four-row result bags in both native variants. Native-index scans were 35,944 and adjacency scans were 29,625, both below the unchanged 45,500 bound and equal to the true aggregates in the original failed CI output. The full 1,020-case GraphExplorer suite and full LMDB module are the remaining local gates; publish the reviewed merge and repair tree now to start independent final-head GitHub CI.
