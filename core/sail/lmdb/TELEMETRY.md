# Diagnosing native LMDB queries with Telemetry

`Explanation.Level.Telemetry` includes a structured, query-local execution profile in addition to the logical plan and the existing native diagnostic text. It is intended for investigating slow queries with the native evaluator and direct adjacency enabled. Ordinary query evaluation and the other explanation levels do not create this profile.

## Choose the workload explicitly

The default consumes result rows. It does not deliberately resolve lazy RDF values or run a result writer. Use the same consumption mode when comparing two runs:

```java
TupleQuery query = connection.prepareTupleQuery(sparql);
query.setMaxExecutionTime(60);

Explanation rows = query.explain(Explanation.Level.Telemetry);
Explanation values = query.explain(Explanation.Level.Telemetry, ExplanationOptions.values());
Explanation serialized = query.explain(Explanation.Level.Telemetry,
        ExplanationOptions.serialized("application/sparql-results+json"));

QueryExecutionProfile profile = serialized.toGenericPlanNode().getExecutionProfile();
System.out.println(serialized.toJson());
```

The explanation types are in `org.eclipse.rdf4j.query.explanation`. Values mode reads each binding's lexical value and literal datatype/language. Serialized mode invokes the installed tuple result writer and counts the bytes actually written to a non-retaining sink. The result-IO API supplies the consumer through a service provider; the desired format module must also be installed. Supported streaming formats are JSON, XML, binary, CSV, and TSV. ODS and XLSX are rejected before creating a writer because their resources cannot be closed independently after a failed or cancelled query. JSON and binary result writers can be selected in Workbench. Serialized mode is restricted to tuple queries, and non-default consumption requires the Telemetry level.

Serialization here measures writer work. It does not measure socket writes, HTTP compression, network transfer, or client rendering. Result consumption can trigger deferred dictionary reads; these are included in the query's value counters. Engine expressions can also decode values in rows mode.

The HTTP protocol carries the same options through ordinary, registered, proxied, and transaction explanation requests:

```text
explain=Telemetry
explain-consumption=rows|values|serialized
explain-result-mime=application/sparql-results+json
```

Omit the MIME parameter unless consumption is `serialized`. Omitting both new parameters preserves row-only behavior. Workbench exposes the options beside the Telemetry controls, includes them in the request identity, and marks existing results stale when they change.

## Read the profile before interpreting totals

The root JSON node has `executionProfile`, with schema version 1. The profile contains:

| Field | Meaning |
| --- | --- |
| `status`, `detail` | Terminal execution outcome, including complete, timed out, cancelled, or failed execution. |
| `elapsedNanos` | Elapsed time owned by the explanation context, frozen at the terminal outcome. Parsing before explanation and rendering the report afterwards are outside this interval. |
| `properties` | Consumption mode, input algebra, bindings, dataset description, inference/evaluation settings, runtime version, and the timing contract. |
| `phases` | Source acquisition, optimization, compilation, opening, execution, explicit result consumption, and cleanup. |
| `stages` | Physical operations and resource owners with stable identities, logical origins where available, metrics, and execution-state properties. |
| `attempts` | Retained actual strategy attempts, including declined, abandoned, partial, or failed work, and candidate admission failures. |
| `workers` | Per-worker elapsed work, observed barrier wait, rows, and completion status. |
| `diagnostics` | Bounded findings that identify the supporting stage and metric evidence. |
| `droppedEvents` | Events omitted because a shared report bound was reached. Individual components additionally report their own bounded omissions. |

Each metric has one numeric representation (`count` for a Java `long`, or `value` for a floating-point quantity), together with `unit`, `origin`, `state`, and `scope`. Missing measurements have no numeric value. Read those qualifiers before calculating a ratio or adding durations. The browser preserves long-valued JSON integers rather than treating all counts as JavaScript doubles.

Phase durations can overlap. Execution includes requested result consumption and deferred initialization. Compilation can include native code-generation lookups. Worker durations overlap each other. The profile deliberately does not sum these fields into an invented total.

## Ten diagnostic improvements

### 1. Physical work inside fused execution

Producer and expression sites report their own opens, partial opens, actual cursor reuse, adjacency payload visits/copies, and bind/filter evaluations. Compiled and interpreted kernels use the same site model. Logical solution multiplicity remains separate from physically processed values. Site counters are merged at cursor or worker boundaries; generated loops use local primitive counters.

Several logical operators may share one fused kernel duration. A missing timer for an individual fused operator stays unavailable. Runtime-filter feedback timers cover only the timed observations and are marked partial; generated filters that publish no timer remain unavailable. Use physical work counters alongside the enclosing attempt's active duration; do not assign the whole duration independently to every logical operator.

### 2. Lifecycle and code-generation accounting

The query phases distinguish setup, execution, requested consumption, and cleanup. `CodeGeneration` stages retain the cache state at lookup and kernel availability at return. A pending attempt stays pending in its recorded decision even if the background compiler finishes before the report is rendered.

Lookup time includes synchronous compilation, readiness waiting, and instantiation. Synchronous compilation has source generation, compiler, and class publication breakdowns. Background compilation is reported with `scope=compiler-thread`; it is separate work triggered by the query, and is excluded from query hotspot selection. Warm hits do not inherit a previous query's compilation cost.

### 3. Predictions and incurred attempts

The attempt ledger keeps candidate prices and admission reasons with the binding/strategy decision that used them. Actual work from a discarded strategy is retained separately from the successful strategy's committed output. An alternative that was never opened has no invented runtime or result count.

Actual/predicted time measures prediction error. It is not proof that another unexecuted strategy would have been faster. Sampled active-time observations and partial attempts carry their provenance.

### 4. Repeated bindings and late outliers

The ledger counts all observed attempts and summarizes strategy/bound-mask groups. It retains recent and slow attempts, so a pathological correlated invocation after the first few calls can remain visible. It reports open/complete/partial/discarded counts, bounded latency summaries, observed worst work, and real producer creation/reuse evidence. Equal bound masks alone do not imply resource reuse.

### 5. Adjacency access and pinned-view state

Access stages retain source/index choice, bound shape, scanned/matched/filtered counts, active iterator work, and physical versus returned-row-proxy provenance. The profile records the query's pinned adjacency view, including snapshot/base revision and overlay generations, independently of later process-wide changes.

Payload page/fiber/root visits and copied values describe logical adjacency work. They do not measure physical disk reads or operating-system page-cache misses. The query's pinned view is evidence about its source lifetime, not a claim that the repository was idle throughout the run.

### 6. Values, dictionary work, and expression escapes

Query-owned counters distinguish lazy handles, lazy resolution requests, native decode requests, memo/cache hits, inline/namespace/datatype paths, dictionary reads and record bytes, and fragment decisions/escapes. Workers explicitly inherit the query owner. Shared fragment preparation records query-local compilation/cache reuse without subtracting concurrent process counters.

Compare rows and values modes to determine whether deferred lexical work accounts for latency. Compare values and serialized modes to investigate writer cost. Different modes perform different workloads and are marked as such in comparisons.

### 7. Memory pressure, reclamation, and spill work

Memory evidence reports current and peak reserved bytes, query/global limits, refused reservations, synchronous reclamation duration, and spill/shed outcomes. The `lmdb/memory` stage is the query total. Its `lmdb/memory/owner/<role>` children attribute existing reservations to stable operator roles such as hash-join builds, count-group arenas and spill buffers, parallel worker/merge state, projection buffers, expression domains, and retained values. The `reservationOwner` property names the role. All instances of a role within this query share one bucket; these are not individual logical-plan-instance measurements.

Each role retains current/peak/total reserved capacity, admission/refusal counters, query/global/closed refusal causes, and reclamation evidence after its structures close. Admission counters include initial claims and successful growth operations. A query-limit and global-limit cause can both apply to one denied operation. Owner peaks are **not additive**: different operators can reach their peaks at different times. Use the query peak for the actual maximum simultaneously reserved capacity.

Reclamation separates the requesting operator's synchronous cost and pressure cause from the victim operator's released bytes and spill/shed action. Foreign-query counters distinguish reclaiming another query's structures from work within this query. This lets an expensive reservation identify both the operator requesting capacity and the operator giving it up without charging the victim's released capacity to the requester as an allocation.

Owner attribution retains at most 32 buckets. Missing labels and overflow are explicit in `reservationOwner=unattributed`; `memoryOwnerAttributionOverflows` counts requests grouped there because the label bound was exceeded. Their bytes still contribute to both the unattributed bucket and the query total.

Native spill-sort instrumentation records completed buffered-file transfers and their durations, including merge reads, alongside I/O failure counts. File-interface bytes are not physical disk traffic, and a failed write can have transferred bytes that the interface does not report.

Reservation accounting is not resident memory or JVM heap attribution. Allocations outside the existing query ledger remain outside these capacity totals; instrumentation adds no reservations and changes no admission policy. Memory admission currently reclaims or refuses; it does not wait for another query to release memory. No admission-wait duration is fabricated.

### 8. Parallel critical path and skew

The parallel profile reports the interval from the first worker start to the final worker finish, aggregate worker elapsed work, the final-worker tail, row balance, and coordinator work after workers. Worker start-barrier waits are named separately. Completed workers' measurements survive a partial parallel invocation. Row skew is the maximum observed per-invocation worker-row maximum/mean; changing the worker count between otherwise balanced invocations does not create artificial imbalance. Individual worker records aggregate by ordinal and disclose how many invocations contributed.

Effective concurrency derives from elapsed worker intervals divided by their wall interval. Those intervals can include blocking; they are not CPU utilization. CPU time and uninstrumented wait causes remain unknown. Aggregate summaries identify repeated invocations so sums cannot be mistaken for a single wall interval.

### 9. Truthful metric names and unavailable values

Unknown fixed counters are not converted to zero. A parent's observed duration is preserved even when child durations overlap or exceed it; exclusive time becomes unavailable when subtraction is not justified. Abstract optimizer costs are not divided by elapsed milliseconds. Child cardinality-error dispersion is not presented as estimate confidence. A derived matched/scanned fraction is called `sourceMatchFractionActual`, while explicit legacy fields can still be read.

### 10. Evidence-driven presentation and comparison

Workbench presents the structured physical profile, its measurement qualifiers, attempts, workers, and evidence-based findings. Physical stage links navigate to logical origins where such a mapping exists. Telemetry heatmaps use attributable measured time when available; an aggregate is no longer ranked solely by its one output row.

Semantic comparison aligns physical identities and discloses route changes. It refuses timing/work ratios across incompatible units, scopes, provenance, partial execution, or changed workload/snapshot context. Unknown or proxy counters do not silently become precise ratios. The original logical plan and textual diagnostics remain available below the profile.

## Partial execution and failure

Timeout results retain the available profile and explicitly identify partial execution. Local explanation failures preserve the original exception and attach an `ExplanationExecutionException` as suppressed diagnostic data. Its `getExecutionProfile()` returns the immutable partial profile. Repository layers can wrap the original failure, so inspect its cause chain as well as suppressed exceptions when collecting diagnostic data programmatically.

Successful query semantics and strategy-learning completion rules remain separate from diagnostic retention. Discarded or incomplete work is useful evidence but must not become a successful cost-model training sample.

## Collection cost and bounds

The shared collector exists only at Telemetry level. Producers capture ownership at construction or execution boundaries and aggregate before rendering. Kernel sites use primitive local counters; report construction is lazy. Memory reservations capture one bounded role accumulator and reuse it for growth and release. Shared profile collections, component sources, memory roles, per-binding groups, retained attempts, worker samples, and code-generation shapes have explicit bounds. Inspect omission counters when a very large or repeatedly correlated query reaches them.

Telemetry still performs additional work. It includes existing iterator instrumentation as well as the new report construction, and explicit values/serialization modes add their requested workload. For an overhead comparison, the repository includes `LmdbTelemetryOverheadBenchmark` with COUNT, fanout, and value-heavy queries in ordinary evaluation and Telemetry modes. Run it through `scripts/run-single-benchmark.sh`; compare identical datasets, warmup, JVM settings, consumption behavior, and revisions. Treat a short single-fork result as an exploratory measurement, not a production performance guarantee.

The [2026-11-20 exploratory comparison](benchmark-results/telemetry-2026-11-20/README.md) records baseline/branch results, raw JMH JSON, observed native routes, exact commands, and allocation/time uncertainty. Compare revisions within each API mode: ordinary evaluation and explanation have different result-consumption paths, so their ratio does not isolate instrumentation cost.
