# Performance evidence, benchmark modules, and research artifacts

This guide covers the branch's benchmark, profiler, source-comparison, and
historical-result surfaces outside `core/sail/lmdb`. These tools help answer
different questions; their results are not interchangeable. Historical
comparison boundary: merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`.

## What changed and where

| Surface | What it is for | Evidence boundary |
|---|---|---|
| [`testsuites/benchmark`](../../../testsuites/benchmark) and [`benchmark-common`](../../../testsuites/benchmark-common) | JMH/workload code, named benchmark themes, Rio benchmarks, data generation, and query-plan snapshot capture/comparison. | Use the benchmark's declared store, query, theme, result verification, forks, and JMH mode. A plan snapshot describes one captured plan and flags; it is not a latency result. |
| [`tools/generated-keys`](../../../tools/generated-keys) | Isolated generated-key regression, allocation/retained-object diagnostics, and kernel microbenchmark sources. Its README distinguishes copied/extracted production units from compatibility doubles and full-store tests. | Standalone harness results do not include full RDF4J planning, end-to-end query time, real LMDB latency, or all production model allocations unless a specific run says so. |
| [`tools/lmdb-join`](../../../tools/lmdb-join) | Controlled join/hash implementation comparisons, named patch variants, and benchmark/regression sources. | Each patch is an alternative against the README's named baseline. Do not stack historical patches or treat an isolated case as a query-engine result. |
| [`tools/lmdb-packed`](../../../tools/lmdb-packed) | Packed-vector and page I/O regression/timing harness, with source-origin manifests and C2 captures. | Component timings operate on prepared values/native memory and exclude dataset loading/query execution; captured x86-64 code is not evidence about another architecture. |
| [`tools/lmdb-performance`](../../../tools/lmdb-performance) | A paired component suite spanning selected CSF, UTF-8, binding-view, generated-key, and native-sort paths. | The README describes which complete files/methods and explicit API doubles each suite compiles. It is not a complete RDF4J build, Janino query run, or LMDB-backed workload. |
| [`tools/value-overlay`](../../../tools/value-overlay) | Read-only overlay storage audits, regression cases, and component read benchmarks. | Storage semantics and production ownership belong to the [value-overlay guide](storage-value-overlay.md); its isolated measurements do not substitute for a repository query. |
| [`benchmark-results`](../../../benchmark-results) | 135 captured benchmark, comparison, profile, and screening artifacts, dated in August 2026. | Historical evidence only. Its machine, runtime, harness, branch/source bundle, query/data shape, and measurement protocol are part of every result's meaning. |
| [`scripts/run-single-benchmark.sh`](../../../scripts/run-single-benchmark.sh) and [`run-single-benchmark-docker.sh`](../../../scripts/run-single-benchmark-docker.sh) | Reproducible single-benchmark selection wrappers, including optional JFR capture and Docker environment setup. | Read the script and the produced command/configuration together. A JFR profile is diagnostic evidence, not a timing comparison; wrapper availability alone does not establish a measured result. |

The stand-alone `tools/` experiments are developer/research surfaces; the
branch's normal runtime behavior is established by the production source and
its normal wiring, not by a package existing under `tools/`. Conversely,
benchmark and test sources still have maintenance value: they preserve
comparison baselines, failure cases, and workload definitions. The
[query-feature guides](query-routing-and-hosts.md) and
[storage guides](storage-overview.md) describe production behavior and point
to component evidence only where it is relevant.

## Reading a result without overgeneralizing

Before quoting a performance or memory number, record the exact captured
source or commit, workload and input shape, machine/OS/JDK, warmup and fork
scheme, timed region, baseline, and result check. Follow the result's paired
comparison rather than comparing files with different query/data shapes or
different capture protocols. Preserve every fork and label exploratory
screens separately from a selected final measurement; do not discard slow
forks after seeing their values.

Use the metric that answers the question:

* JMH throughput/average-time measures the declared benchmark method and
  includes only work inside that method.
* Component harness wall time can isolate a codec, key table, or probe loop;
  it does not include the unmeasured parser, planning, store, or network path.
* Thread-allocation counters measure allocations attributed to the measured
  thread. They do not include native memory, process RSS, other threads, or
  necessarily an entire application query.
* Object-graph size estimates retained Java objects reachable from the graph
  the harness traverses. It is sensitive to the VM layout and traversal roots
  and should not be called heap/RSS usage for a production workload.
* JFR and HotSpot C2 output help explain a behavior. A profile alone does not
  establish a speedup; a compilation dump is architecture/JVM-specific.

The generated-key, packed-I/O, and common-path READMEs describe fixture
boundaries and source manifests in detail. In particular, compatibility
doubles may supply RDF model or external APIs while production algorithms
remain real source. A report that says “actual production method” therefore
means that method body was included under the harness's declared boundary, not
that a complete RDF4J server or native query was run.

## Existing plan-snapshot workflow

The branch adds benchmark-common capture, JSON snapshot, metadata/flag
collection, and semantic comparison support. The snapshot CLI records query
identity and explanation levels alongside plan representation; its comparison
can show plan-structure/estimate/work changes without executing a query in
compare-existing mode. In capture mode, repeated query execution is part of
the tool's own verification policy, so snapshots from that mode should be
read with its verification status and soft-limit metadata. A stored plan is
not a promise that a later runtime chooses the same adaptive strategy: flags,
statistics, data generation, and source version matter.

The main references are [`QueryPlanCapture`](../../../testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/QueryPlanCapture.java),
[`QueryPlanSnapshotCli`](../../../testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/plan/QueryPlanSnapshotCli.java),
[`QueryPlanSnapshotComparator`](../../../testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/plan/QueryPlanSnapshotComparator.java),
and the repository-specific
[snapshot workflow](../../../.agent/skills/query-plan-snapshot-cli/references/workflow.md).
The skill supplies local capture/compare steps; its procedure is not evidence
that a particular snapshot exists. Use the dated artifact and its provenance
when citing a captured plan.

## Shared generated query corpus

The branch also makes `SparqlComprehensiveStreamingValidTest.generatedQueries()`
a public lazy stream of `GeneratedQuery` records in a stable category/factory
order. Its categories include paths, grouping and aggregates, filters and
`BIND`/`VALUES`, subqueries, datasets and `SERVICE`, construct/describe forms,
ordering/modifiers, built-ins, graph scoping, and deep nesting. The same
source is used by comprehensive rendering coverage and is consumed from the
`rdf4j-queryrender` test JAR by LMDB native generated-query coverage and the
generated-corpus benchmark. The test JAR is deliberately test-scoped in the
LMDB module; this does not put generated queries or test classes on the
production runtime path.

The shared stream is a single fixture definition for multiple test/evidence
consumers, reducing the chance that parser/rendering and native coverage
silently use unrelated query lists. The LMDB coverage test records categories
and checks corpus accounting; the benchmark resolves selected corpus indices
and verifies their categories before assembling its workload. A stream can
represent a large corpus without materializing every query at once, although
consumers that collect or index it still allocate for the subset they retain.
These are source-level fixture and memory observations, not a report that the
coverage tests pass or that benchmark measurements exist.

Sources: [`SparqlComprehensiveStreamingValidTest`](../../../core/queryrender/src/test/java/org/eclipse/rdf4j/queryrender/SparqlComprehensiveStreamingValidTest.java),
the [`queryrender` test-JAR configuration](../../../core/queryrender/pom.xml),
the [LMDB test dependency](../../../core/sail/lmdb/pom.xml),
[`LmdbNativeGeneratedQueryCoverageTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGeneratedQueryCoverageTest.java),
and [`LmdbNativeGeneratedCorpusBenchmark`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGeneratedCorpusBenchmark.java).

The regular benchmark code also changes named theme/query definitions,
dataset generation, and benchmark-resource checks. This is workload coverage,
not a change in RDF4J query semantics. The regression/evidence mapping is in
[compliance and regression map](integration-compliance-and-regression-map.md).

## Historical reports and source authority

The committed `benchmark-results/` files are reports produced by earlier
runs. A saved number establishes that the report recorded that result under
its accompanying conditions; it does not establish a new run or show that the
result applies to current machines, users, or all query shapes. Dated plan
files, archived source ZIP names, prior
implementation reports, generated object dumps, experimental patches, and
profiling text have the same historical boundary.

For current behavior, use current source and tests; the [branch README](README.md)
records the source revision checked for this guide set. For a current
performance claim, use a controlled before/after run whose source pair and
methodology are stated. Do not infer that an implementation is active because
an experiment or flag exists: inspect planner selection and runtime call
sites. The query configuration reference marks allowlisted and disabled
experiments; the native query guides explain which strategy families are
actually reachable.

This guide inventories available evidence and its limits. It reports no new
benchmark or profiler measurement; source links and build checks are not
presented as performance evidence.
