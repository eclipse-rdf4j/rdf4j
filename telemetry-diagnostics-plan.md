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
- [ ] in_progress: Commit, push, and verify downloadable patch.

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

All ten feature areas are implemented. Shared schema contracts, truthful generic metrics, lifecycle phases, explicit value consumption, native/resource producers, protocol propagation, and Workbench presentation have passed focused regressions. Review-driven regressions now cover timeout races, partial writer cleanup, nested query ownership, elastic parallel worker counts, and unmeasured filter timing. Production edits are frozen. The final combined LMDB regression passed all 217 tests with zero failures, errors, or skips; both exploratory JMH runs completed with all twelve cells and setup assertions passing, and the final reactor quick install passed in 2m48. Header checks, generated signatures, and `git diff --check` also pass. This section records actual coverage and limitations without implying every desired measurement is exact.

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
