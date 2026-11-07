# Sparse join aggregate reproduction

`SparseJoinAggregateBenchmark` compares complete executions of:

```sparql
SELECT ?x ?y (COUNT(?z) AS ?count)
WHERE { ?x <urn:sparse-join-benchmark:p> ?z . ?z <urn:sparse-join-benchmark:q> ?y }
GROUP BY ?x ?y
# Optionally: HAVING(COUNT(?z) >= threshold)
```

The default routes compare normal automatic planning against its explicit disabled
baseline and a forced sparse control:

| Route | Sparse property | Route selection |
| --- | --- | --- |
| `auto` | No override; use the production default | Automatic |
| `autoDisabled` | `false` | Automatic |
| `sparseJoinAggregate` | `true` | Forced sparse |
| `packedFtreeAggregate` | No override | Forced packed factorized aggregate |
| `autoSparse` | `true` | Automatic; retained for older command lines |

`auto` clears a caller-supplied sparse property so it measures the normal default.
The separate `autoDisabled` route provides the counterfactual planner baseline.
The optional `packedFtreeAggregate` control remains available through `--routes`.
Historical result directories retain their original meaning: their `auto` route
disabled sparse. The summarizer accepts those CSVs and labels that setting
`legacy_false`; it does not reinterpret historical `auto` rows as default-enabled runs.
Forced routes must appear in the executed explanation or the run fails. All
routes use the same loaded store and full adjacency index. The specialized
`factorizedTail` route cannot admit this shape: its existing admission rule
rejects multiple group keys spanning the prefix and a fresh tail variable. The
packed f-tree implementation admits both grouped endpoints and preserves bag
multiplicities, so it is the appropriate current factorized comparison.
The default `bound` count mode uses `COUNT(?z)`: both mandatory patterns bind
`?z`, so it has exactly the same count as `COUNT(*)`. This choice matters because
the existing serial packed f-tree path declines the full-mapping reducer used by
`COUNT(*)`. Every route in a comparison receives the same query. No route gets
different parallelism or a rewritten aggregate to make it eligible.
`--parallel false` preserves the historical serial default. `--parallel true`
enables parallel execution equally for every route, leaving other competitors
available; the normal automatic planner can then choose their parallel plans.
The matching JMH parameter is `parallel`. Its value is recorded as a separate
CSV/summary dimension, and old CSVs normalize to `false`.
Result caching is
explicitly disabled. No competing aggregate is disabled to make sparse win.

## Bounded sweep

Use Java 25 and run from the repository root:

```bash
scripts/run-sparse-join-aggregate-benchmarks.sh --output target/sparse-join-results
```

The script uses the repository `benchmarks,quick` packaging profile, first
offline and then online if necessary. `MVN` can select a Maven wrapper. Use
`--no-build` only when `core/sail/lmdb/target/jmh-benchmarks.jar` contains the current sources.
The script rejects a results directory containing an earlier run so fewer forks
on a rerun cannot silently mix old and new samples.
A smaller validation run is:

```bash
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --forks 1 --fanouts 1,8 --blocks 1,8 --multiplicities 1,4 \
  --endpoints 64 --join-keys 64 --warmups 1 --iterations 3 \
  --output target/sparse-join-smoke
```

To measure the `COUNT(*)` syntax separately, use its supported controls:

```bash
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --counts star --routes autoDisabled,auto,sparseJoinAggregate --forks 3 \
  --output target/sparse-join-count-star
```

`count_expression` distinguishes these results. Requesting the serial packed
route for `star` intentionally fails its admission check.

Each fork starts a fresh JVM. Each fixture is loaded once and reused across
routes and thresholds; route order rotates across fixtures and forks. For each
case, one first-use query is recorded, an executed explanation proves the route,
warmups are run, and the requested number of warmed samples is recorded. Every
query, including warmup and first-use queries, is checked against an independent
exact integer oracle. The assertion checks endpoint bindings, each count, the
number of groups, and duplicate groups. Validation happens after the timer stops.

Executed-route explanations are sampled after first use, after warmup, and after
the warmed measurement batch.
The CSV `actual_paths` and `sparse_work` fields record those witnesses; the latter
includes sparse multiplication/pruning counters when available. `sparse_plan`
records the runtime orientation, including streamed/retained slots and bindings
when available; `sparse_plan_witnesses` retains their union in the summary. The unforced
planner may adapt between invocations. These are **separate executed explanation
queries**, not instrumentation of every timed query. `route_witness_scope` and
`route_witness_stage` make that distinction explicit. The final explanation is
stored as `phase=witness` with zero timings; it is excluded from all latency
statistics. Forced-route admission still fails loudly if a requested strategy
cannot execute.
The summary unions route witnesses across all recorded forks and phases rather
than assigning the last observed planner choice to an entire case.

The output contains raw samples (`fork-*.csv`), route logs, exact commands, JVM
and CPU information, container CPU/memory limits where available, the base commit
and working-tree status/diff, including untracked implementation and benchmark
sources for an uncommitted feature. `summary.csv` and `summary.md` report medians of
per-fork warmed medians. `automatic-comparison.csv` pairs `autoDisabled`/`auto`
ratios within each fork and reports their median and range. This measures the
effect of normal planner integration, including any cases where the planner
selects another route. `comparison.csv` separately pairs each available control
against forced sparse; its historical `sparse_median_ms` column remains available.
A speedup above 1 favors the candidate named in the comparison.
The range across forks is not a confidence interval. No outliers are dropped.
The summaries can be regenerated with Python 3:

```bash
python3 scripts/summarize-sparse-join-aggregate.py target/sparse-join-results
```

The summarizer's small self-check covers old CSV input, asymmetric/skew/overlap/parallel
case separation, paired automatic ratios, transposed orientation witnesses, and
exclusion of witness rows from latency samples:

```bash
python3 scripts/test-summarize-sparse-join-aggregate.py
```

Compare per-fork medians and distributions rather
than combining the fastest sample of each route. Do not run unrelated builds,
tests or benchmark processes concurrently with the measurements.

## Timing boundary

`total_ns` includes fresh SPARQL preparation, optimization, query execution,
per-query sparse endpoint remapping/CSR construction and counter allocation, and
complete result draining into a common collection. `prepare_ns` measures
preparation; `evaluate_drain_ns` includes optimization, kernel setup, aggregation
and collection. Sparse setup is never moved into fixture setup or amortized
across measured queries. The benchmark reports end-to-end cost; it does not claim
to isolate kernel-only or remapping-only CPU time.

`load_ns` covers fixture insertion, exact-oracle generation and commit.
`adjacency_ns` covers the explicit persistent adjacency build after commit.
These costs are recorded once per fixture and excluded from `total_ns`, equally
for all routes. Store initialization is also excluded. `phase=first` means the
first query for a route/threshold on an already loaded, adjacency-built store.
JIT state can be warm from earlier cases. It is **not cold-disk latency**: there
is no filesystem cache eviction. The script's fork count measures variation
between independent processes, not hardware portability.

## Workload controls and exact semantics

`--fanouts` retains the symmetric degree grid. `--fanout-pairs` accepts explicit
left/right degrees such as `1x32,32x1`; the two options are mutually exclusive.
In JMH, `leftFanout` and `rightFanout` independently override `fanout`; zero means
inherit the old `fanout` parameter. Raw CSVs retain `fanout` as the left degree
for older consumers and add unambiguous `left_fanout` and `right_fanout` columns.

Each relation has `joinKeys` distinct join-key values. `--overlaps` selects the
percentage present in both relations, rounded down to a whole number of keys.
The right relation's keys are shifted so the overlap is exactly
`floor(joinKeys * overlap / 100)`. Nonmatching keys remain in both stored relations;
reducing overlap does not simply shrink the input. `shared_join_keys` records the
exact intersection size. At zero overlap, the correct output is empty even when
HAVING is absent, and all timed results are still checked.

The `uniform` distribution preserves the original deterministic mixing function
that selects neighbor intervals. With `hot`, approximately seven eighths of keys
concentrate their neighbor intervals in the first eighth of each endpoint block,
expanded when necessary to accommodate the requested fanout. The remaining keys
use the full block. Left and right concentration choices use independent hashes.
This creates endpoint-frequency skew while preserving each key's distinct degree;
it is a controlled synthetic hotspot, not a claim to model a power-law RDF graph.

`blocks` partitions both endpoint domains and limits joins
to pairs within the same partition. Thus fanout and possible output density can
be varied separately. Blocks must divide both endpoint and join-key counts, and
each fanout must not exceed `endpoints / blocks`. The raw CSV `output_density` is the
actual nonzero count divided by the **nominal domain** `endpoints²`. Some nominal
endpoints have no incidences at low fanout. The summary also reports observed
streamed/retained endpoint counts and compact-domain density using the forced
sparse reference route's `leftRows` and `rightEndpoints` telemetry when available.
These are physical execution roles and may transpose the written-query sides.
The summary names them `observed_streamed_endpoints` and
`observed_retained_endpoints`; the older `observed_left_endpoints` and
`observed_right_endpoints` columns remain deprecated aliases for compatibility.
`dimension_reference_route` identifies the forced sparse reference, including
when its dimensions accompany a control row. If witnessed orientations have
different role counts, scalar counts are left blank and all streamed×retained
pairs appear in `observed_endpoint_dimension_witnesses`. Their common product
still determines compact-domain density because transposition does not change it.

For maximum multiplicity `m`, shared value `z` has left weight `1 + z % m` and
right weight `1 + (z / 3) % m`. Each incidence is repeated in that many distinct
named contexts, producing physical repeated neighbors in unrestricted scans.
Repeating the identical quad would be deduplicated by RDF storage and would not
exercise bag weights. The oracle accumulates the product of the two weights for
each matching pair. The CSV separately reports stored statements, expanded join
cardinality, nonzero groups and retained groups. Multiplicity `1` is the
unweighted control; the default `4` case includes nonuniform weights 1 through 4.
The weight schedule is deliberately deterministic. Because block assignment is
`z % blocks`, weights and partitions are correlated (for blocks 8 and maximum
weight 4, left weights are constant within a block). The dimensions are separate
generator controls, not statistically independent distributions: changes in
block count can also change weight localization and the effectiveness of bounds.

Threshold `none` omits HAVING; `median` uses the median positive exact count;
`empty` uses one above the largest exact count. Integers select literal
thresholds. Median does not imply exactly 50% retention when counts tie, so the
CSV reports the actual threshold and retained group count. Even `empty` cases
must execute the requested route and return exactly zero groups. For a fixture
whose join is already empty, `median` and `empty` both use threshold 1.

## Bounded integration comparison

Use separate, discriminating suites instead of multiplying all generator controls
together. The following commands use two JVM forks, two warmups and five timed
queries per case. Increase those counts for final performance claims. Each suite
has its own output directory and includes `autoDisabled`, normal `auto`, and forced
sparse by default. Do not run these suites concurrently.

```bash
# Symmetric sparse/dense controls: nine query cases.
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --forks 2 --warmups 2 --iterations 5 --fanout-pairs 1x1,8x8,32x32 \
  --blocks 1 --multiplicities 1 --thresholds none,median,empty \
  --endpoints 256 --join-keys 512 --output target/sparse-default-symmetric

# Unequal left/right materialization and expansion: four query cases.
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --forks 2 --warmups 2 --iterations 5 --fanout-pairs 1x32,32x1 \
  --blocks 1 --multiplicities 1 --thresholds none,median \
  --endpoints 256 --join-keys 512 --output target/sparse-default-asymmetric

# Repeated contexts and concentrated endpoint frequencies: six query cases.
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --forks 2 --warmups 2 --iterations 5 --fanout-pairs 8x8,32x32 \
  --distributions hot --blocks 1 --multiplicities 4 --thresholds none,median,empty \
  --endpoints 256 --join-keys 512 --output target/sparse-default-hot-weighted

# Unmatched input work, including an empty join: six query cases.
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --forks 2 --warmups 2 --iterations 5 --fanout-pairs 8x8 --overlaps 25,0 \
  --blocks 1 --multiplicities 1 --thresholds none,median,empty \
  --endpoints 256 --join-keys 512 --output target/sparse-default-overlap
```

For a quick validation of every new generator dimension before measuring, use
small domains and one timed sample. This verifies fixture/oracle/route behavior;
it is not performance evidence:

```bash
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --forks 1 --warmups 0 --iterations 1 --fanout-pairs 1x8,8x1 \
  --distributions uniform,hot --overlaps 0,25,100 --blocks 1 --multiplicities 4 \
  --thresholds none,median,empty --endpoints 32 --join-keys 32 \
  --output target/sparse-generator-smoke
```

After the serial suites, a targeted parallel supplement checks the same low/high
fanout controls with parallel competitors available. It adds only two query cases;
all three routes receive `parallel=true`, and no competing strategy is disabled:

```bash
scripts/run-sparse-join-aggregate-benchmarks.sh --no-build \
  --parallel true --forks 2 --warmups 2 --iterations 5 --fanout-pairs 1x1,32x32 \
  --blocks 1 --multiplicities 1 --thresholds none \
  --endpoints 256 --join-keys 512 --output target/sparse-default-parallel
```

## Sustained JMH alternative

The same class also runs under the existing helper. Each JMH invocation includes
query preparation and complete result draining. Exact validation uses JMH
invocation teardown and is excluded from its measured interval.

```bash
scripts/run-single-benchmark.sh --no-build \
  --module core/sail/lmdb \
  --class org.eclipse.rdf4j.sail.lmdb.benchmark.SparseJoinAggregateBenchmark \
  --method completeQuery --forks 3 --warmup-iterations 3 --measurement-iterations 5 \
  --param strategy=autoDisabled,auto,sparseJoinAggregate \
  --param parallel=false \
  --param fanout=1,8,32 --param blocks=1,8 --param multiplicity=1,4 \
  --param having=none,median,empty --param endpoints=256 --param joinKeys=512 \
  -- -rf json -rff target/sparse-join-jmh.json

# Preserve the default plan guard's full log before another helper invocation overwrites it.
cp core/sail/lmdb/target/benchmark-output.log target/sparse-join-jmh-full.log
```

With the default query-plan risk guard, the helper's console output is compacted;
the full log above retains trial-setup route witnesses that may be omitted from
redirected console output. These are separate setup explanation queries, not
instrumentation of every JMH invocation. Keep a uniquely named full log for each
run, alongside its JSON, command and build identity. Inspect iteration sequences
for continuing warmup or adaptive changes before describing results as steady state.

Invocation teardown has harness overhead; retain the raw bounded sweep and JMH
results separately. These synthetic two-pattern workloads do not establish the
speedups reported for Tessera, predict real RDF workload distributions, or test
AMX. Admission, SPARQL fallback semantics, cancellation, memory limits and
overflow are covered by the focused integration/kernel tests.
