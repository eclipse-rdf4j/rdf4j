# RDF parser optimization and measurement notes

This document preserves measurements from the supplied implementation's earlier
feature revisions. Raw evidence belongs to the original delivery archive and is
not bundled in this repository. These historical results are not fresh
measurements of the GH-6259 integration. The current helper defaults to measuring
the current implementation; a comparison requires an explicit `BASELINE_COMMIT`
that exists locally and contains the standard evaluator's parser runtime package.

## Review correction note

The recorded optimization results describe commit `30b7332`, before the review
corrections. They are historical measurements, not measured speedups for this
corrected revision. The tuple-only set and immutable row optimizations remain.
The corrections additionally use a lazy triple-term component index, exact
lookup for bound whole terms, and a stable dependency-ready queue. Their
correctness and lookup behavior are tested; no new latency claim is inferred
from those tests.

## Scope

The implementation is compared with feature commit
`7afc4b787697dc3f6345e8d667d4083ea6aea57d`, not with an external store or a
native bulk-import API. The same JMH harness is compiled once and run against
separately compiled baseline and optimized production classes. Both variants
use identical RDF4J dependencies, JDK, heap options and fixtures. Production
classes for the other variant are excluded from each benchmark classpath.
No third-party runtime dependency is added; JMH is test-scoped.

## What changed

| Area | Previous implementation | Optimized implementation |
|---|---|---|
| Tuple document materialization | Indexed LinkedHashModel, followed by a context scan | LinkedHashSet with graph validation in the parser handler |
| Triple/quad row representation | Immutable List and backing array | One immutable statement-backed List view |
| Ordinary join-group inspection | Re-flattens nested join subtrees | One iterative flatten per conjunctive group |
| Function argument lists | Rebuilds first/rest maps for every input/output list | One shared pair of maps per group |
| Dependency readiness | Scans remaining calls for each unresolved input | Per-variable producer counts |
| Scalar SERVICE preparation | Configuration extraction clone plus rewrite clone | Configuration extraction clone, then owned rewrite |
| Batch SERVICE preparation | Extracts and rewrites for every input | One batch template, independent clone per input |
| Result projection | Copies every result into a new MapBindingSet | Reuses an already compatible result; copy/merge fallback otherwise |

Set insertion uses statement/term equality, not SPARQL numeric value equality.
Duplicate parser events still consume the configured statement limit. Every
invocation parses a fresh document and allocates fresh blank nodes. Model indexes
remain enabled for SERVICE graph patterns. The public Model-returning parser API
is unchanged.

A DynamicModel substitution alone would not avoid the old costs: in this
pinned RDF4J version both `unmodifiable()` and `contexts()` trigger its indexed
upgrade. An explicit tuple-only storage path avoids accidental upgrades.

## API-binding correctness

The expanded regression suite exposed a chained-call problem: direct evaluator
tests passed when they retained runtime API bindings, but MemoryStore,
NativeStore and LMDB execute optimized plans with an empty input mapping. API
bindings must therefore be represented in the prepared algebra. Query execution now
runs RDF4J's standard, scope-aware BindingAssignerOptimizer on a private clone
of the original SPARQL algebra before introducing synthetic tuple/LATERAL
operators. It does not blindly replace variable names across subquery scopes.

Regression tests execute optimized plans with empty runtime bindings, test both
Minimal and array-based evaluation contexts, preserve prebound output conflicts,
and exercise tuple-to-SERVICE and SERVICE-to-tuple dependency chains. The
benchmark rewrite methods deliberately isolate rewriting without this API-binding
prepass; their speedups are not whole-query preparation measurements.

## Reproduce

Run from a full RDF4J checkout with Java 25 and Maven, after applying the patch:

```sh
bash compliance/rdf-parser/verify.sh
bash compliance/rdf-parser/benchmark.sh
```

The default run requires no historical commit. To compare another runtime
implementation, set `BASELINE_COMMIT` to a locally available commit containing
the evaluator's parser package. The helper compiles that commit's parser sources with
`javac --release 25` against the same current Maven dependency classpath. The
new test/benchmark classes do not replace baseline production classes. Each run
retains a new directory under `target/benchmarks/`.

The checked-in JMH classes also support larger documents (`statements=10000`),
but the CI comparison selects 10 and 1,000 statements. For a longer or differently
sized run, use the generated classpath and `org.openjdk.jmh.Main` directly with
appropriate `-p`, `-wi`, `-i`, `-w` and `-r` options. Do not compare a differently
configured run with the recorded short CI run as though conditions were equal.

## Workloads

* `tupleTriples` and `tupleQuads`: fully validate and materialize the document,
  then drain all rows and consume every column with a JMH Blackhole. Turtle and
  single-named-graph TriG fixtures use distinct subjects and literal objects.
* `serviceSelect`: direct local SERVICE call, including parsing, model creation,
  optimization, evaluation and result projection. It excludes the outer
  repository/federation adapter.
* `serviceBatch`: direct `FederatedService.evaluate` call with 16 identical input
  bindings. Every input is reparsed; no dataset cache is used. JMH's
  `@OperationsPerInvocation(16)` normalizes time and allocation per document,
  not per whole batch. This measures the batch SPI specifically; the outer
  evaluator may instead choose scalar `select` or `ask` calls.
* `rewritePlain`: clones and rewrites ordinary groups of 8, 64 and 256 patterns.
* `rewriteCalls`: clones and rewrites groups of 1, 8 and 24 parser calls. It does
  not execute the potentially large cross product of those calls.

The CI script uses two JVM forks, three 500 ms warmup iterations and five 500 ms
measurement iterations per fork; one benchmark thread; a 512 MiB fixed heap;
AverageTime in microseconds; and the JMH GC profiler. It saves JSON measurements
(including raw samples and confidence intervals) and text logs, and prints a
comparison. Fixtures are prepared outside timed regions. Every timed parse
still performs the complete document validation.

## Interpretation and limits

Report allocation as bytes allocated per operation, not retained heap or peak
RSS. Speedup is baseline mean time divided by optimized mean time. Small timing
differences on shared CI hosts are not decisive; inspect the error bars and raw
fork data. Baseline runs precede optimized runs, so order-related host variation
is not eliminated. These short runs are screening measurements, not a substitute
for dedicated-machine repeated, interleaved runs with longer warmup.

The fixtures are not a representative corpus of arbitrary LLM output. They do
not cover long literals, many blank nodes, many named graphs, concurrent query
loads, complex graph joins, disk I/O, native-engine compilation, or insertion
throughput. Backend integration tests establish correctness, not backend
performance. Limits, full-document atomic parse errors, and blank-node isolation
must not be weakened to improve a benchmark score.

## Related implementations and methodology

Apache Jena's GraphMemIndexedSet separates the flat triple set from optional
subject/predicate/object indexes; its MinimalStoreStrategy avoids building those
indexes. That is a useful comparison for separating ingestion/enumeration from
pattern-matching workloads, not a reason to introduce a Jena dependency here.
The optimized implementation uses existing Java and RDF4J APIs.

Sources consulted on 2026-10-22:

- Apache Jena GraphMemIndexedSet API:
  https://jena.apache.org/documentation/javadoc/jena/org.apache.jena.core/org/apache/jena/mem/GraphMemIndexedSet.html
- Apache Jena MinimalStoreStrategy API:
  https://jena.apache.org/documentation/javadoc/jena/org.apache.jena.core/org/apache/jena/mem/store/strategies/MinimalStoreStrategy.html
- RDF4J DynamicModel at the pinned feature baseline:
  https://github.com/HASMAC-AS/rdf4j/blob/7afc4b787697dc3f6345e8d667d4083ea6aea57d/core/model/src/main/java/org/eclipse/rdf4j/model/impl/DynamicModel.java
- OpenJDK JMH dead-code and profiler samples:
  https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_08_DeadCode.java
  https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java


## Recorded screening outcome

The Java 25 run for commit `30b733258cc696d6e4396d3b002b4a1d94818cba`
completed all 14 paired benchmark cases. At 1,000 statements, tuple triples took
2,030.915 -> 1,771.625 microseconds with 56.5% fewer allocated bytes; quads took
1,792.429 -> 1,578.881 microseconds with 60.8% fewer allocated bytes. A 256-pattern
ordinary group rewrote in 486.944 -> 42.254 microseconds (11.52x). A group of 24
parser calls rewrote in 208.061 -> 72.398 microseconds (2.87x).

SERVICE time did not improve decisively: the 1,000-statement scalar case was
2,257.127 -> 2,292.240 microseconds, with overlapping error intervals, while
allocated bytes fell 9.0%. The corresponding batch case allocated 10.0% less.
Do not describe these results as a demonstrated SERVICE latency improvement.
See the delivery's VERIFICATION.md and raw JSON for all cases, allocation totals,
uncertainty and reproducible execution metadata.
