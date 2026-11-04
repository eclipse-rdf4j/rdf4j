# Sparse join aggregate reproduction

`SparseJoinAggregateBenchmark` compares complete executions of:

```sparql
SELECT ?x ?y (COUNT(?z) AS ?count)
WHERE { ?x <urn:sparse-join-benchmark:p> ?z . ?z <urn:sparse-join-benchmark:q> ?y }
GROUP BY ?x ?y
# Optionally: HAVING(COUNT(?z) >= threshold)
```

The default routes are `auto` (the existing serial planner, with the experimental
sparse candidate disabled), `packedFtreeAggregate` (the existing forced packed factorized
aggregate), and `sparseJoinAggregate` (the experimental forced sparse aggregate).
`autoSparse` optionally measures planner choice with the candidate enabled.
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
  --counts star --routes auto,sparseJoinAggregate --forks 3 \
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

Executed-route explanations are sampled after first use and again after warmup.
The CSV `actual_paths` and `sparse_work` fields record those witnesses; the latter
includes sparse multiplication/pruning counters when available. The unforced
planner may adapt between invocations. Forced-route admission still fails loudly
if a requested strategy cannot execute.
The summary unions route witnesses across all recorded forks and phases rather
than assigning the last observed planner choice to an entire case.

The output contains raw samples (`fork-*.csv`), route logs, exact commands, JVM
and CPU information, container CPU/memory limits where available, the base commit
and working-tree status/diff, including untracked implementation and benchmark
sources for an uncommitted feature. `summary.csv` and `summary.md` report medians of
per-fork warmed medians; `comparison.csv` pairs baseline/sparse ratios within
each fork and reports their median and range. A speedup above 1 favors sparse.
The range across forks is not a confidence interval. No outliers are dropped.
The summaries can be regenerated with Python 3:

```bash
python3 scripts/summarize-sparse-join-aggregate.py target/sparse-join-results
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

Each of `joinKeys` shared values has `fanout` distinct left neighbors and
`fanout` distinct right neighbors. A deterministic mixing function selects
neighbor intervals. `blocks` partitions both endpoint domains and limits joins
to pairs within the same partition. Thus fanout and possible output density can
be varied separately. Blocks must divide both endpoint and join-key counts, and
fanout must not exceed `endpoints / blocks`. The raw CSV `output_density` is the
actual nonzero count divided by the **nominal domain** `endpoints²`. Some nominal
endpoints have no incidences at low fanout. The summary also reports observed
left/right endpoint counts and compact-domain density using the sparse route's
`leftRows` and `rightEndpoints` telemetry when available.

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
must execute the requested route and return exactly zero groups.

## Sustained JMH alternative

The same class also runs under the existing helper. Each JMH invocation includes
query preparation and complete result draining. Exact validation uses JMH
invocation teardown and is excluded from its measured interval.

```bash
scripts/run-single-benchmark.sh --no-build \
  --module core/sail/lmdb \
  --class org.eclipse.rdf4j.sail.lmdb.benchmark.SparseJoinAggregateBenchmark \
  --method completeQuery --forks 3 --warmup-iterations 3 --measurement-iterations 5 \
  --param strategy=auto,packedFtreeAggregate,sparseJoinAggregate \
  --param fanout=1,8,32 --param blocks=1,8 --param multiplicity=1,4 \
  --param having=none,median,empty --param endpoints=256 --param joinKeys=512 \
  -- -rf json -rff target/sparse-join-jmh.json
```

Invocation teardown has harness overhead; retain the raw bounded sweep and JMH
results separately. These synthetic two-pattern workloads do not establish the
speedups reported for Tessera, predict real RDF workload distributions, or test
AMX. Admission, SPARQL fallback semantics, cancellation, memory limits and
overflow are covered by the focused integration/kernel tests.
