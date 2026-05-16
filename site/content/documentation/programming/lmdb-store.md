---
title: "The LMDB Store"
toc: true
weight: 4
autonumbering: true
---

{{< tag "New in RDF4J 4.0" >}}
{{< tag "Experimental" >}}

The RDF4J LMDB Store is a new SAIL database, using the [Symas Lightning
Memory-Mapped Database](https://www.symas.com/lmdb): a fast embeddable
key-value database using memory-mapped IO for great performance and stability.
<!--more-->

The LMDB Store can be used in any RDF4J project that requires persistent
storage that is fast, scalable and reliable.

## Dependencies for the LMDB Store and native extensions

To make use of the LMDB Store, you'll need to include the following Maven dependency:

```xml
<dependency>
  <groupId>org.eclipse.rdf4j</groupId>
  <artifactId>rdf4j-sail-lmdb</artifactId>
</dependency>
```

Alternatively you can also rely on the `rdf4j-storage` pom dependency. See
["Which Maven artifact?"](/documentation/programming/setup/#which-maven-artifact), which includes the LMDB Store.

Because the LMDB Store relies on a third party embedded database (LMDB) that is
not itself a Java library, you'll need two additional runtime dependencies for
native extensions, provided by [LWJGL](https://lwjgl.org/). These dependencies
have an OS-specific classifier that is based on the platform OS you wish to run
on. For example, to run the LMDB Store on a Linux machine, you'll need to
include the following:

```xml
<dependency>
  <groupId>org.lwjgl</groupId>
  <artifactId>lwjgl</artifactId>
  <classifier>natives-linux</classifier>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>org.lwjgl</groupId>
  <artifactId>lwjgl-lmdb</artifactId>
  <classifier>natives-linux</classifier>
  <scope>runtime</scope>
</dependency>
```

The required versions of the native extensions are in the [RDF4J Bill Of
Materials](/documentation/programming/setup/#the-bom-bill-of-materials).

Available extensions for different OS platforms:

| Operating System | native extension classifier |
|------------------|-----------------------------|
| Linux            | `natives-linux`             |
| MS Windows       | `natives-windows`           |
| Mac OS           | `natives-macos`             |
| Mac OS (ARM64)   | `natives-macos-arm64`       |

## Create RDF Repository

The code for creation of an LMDB-based RDF repository is similar to that of the MemoryStore or NativeStore:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
...
File dataDir = new File("/path/to/datadir/");
Repository repo = new SailRepository(new LmdbStore(dataDir));
```

The above code initializes a new or loads an existing repository at the location specified by `dataDir`.

When no indexes are configured explicitly, the store uses the two indexes: `spoc` and `posc`. Repositories created
through the Workbench use the repository template default of `spoc`, `posc`, and `ospc`, so Workbench-created LMDB
repositories start with the additional object-subject-predicate-context index unless that field is changed.

To configure the indexes and other options an instance of `LmdbStoreConfig` can be used.

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
...
File dataDir = new File("/path/to/datadir/");

LmdbStoreConfig config = new LmdbStoreConfig();
// set triple indexes
config.setTripleIndexes("spoc,ospc,psoc");
// always sync to disk, disabled by default
config.setForceSync(true);
// disable autogrow, enabled by default
config.setAutoGrow(false);
// persist value hash codes across restarts, disabled by default
config.setValueHashCacheEnabled(true);
// set maximum size of value db to 1 GiB

config.setValueDBSize(1_073_741_824L);
// set maximum size of triple db to 1 GiB
config.setTripleDBSize(1_073_741_824L);

Repository repo = new SailRepository(new LmdbStore(dataDir), config);
```

The optional value hash cache stores precomputed `Value.hashCode()` results in `hashes.dat`. It is disabled by default.
When enabled, LMDB writes a `hashes.dat.integrity` sidecar on clean shutdown and only trusts the cache again on the
next startup if that integrity metadata validates. Invalid or stale hash cache files are discarded automatically and
the store falls back to recomputing hashes lazily.

## Query planning, estimators and optimizers

LMDB uses two estimator layers for query planning:

- The page cardinality estimator answers single statement-pattern cardinality questions from the LMDB B-tree layout. It
  estimates the size of the best matching index range without scanning every matching statement. If this estimator is
  disabled, LMDB uses exact cardinality counting instead.
- The sketch-based join estimator keeps compact sketches over subject, predicate, object and context values, plus selected
  component pairs. It uses these sketches to estimate join sizes, variable overlap, group/distinct cardinality and filter
  selectivity. The estimator state is persisted below the repository data directory in `join-estimator.rjes`, with filter
  feedback in the `join-estimator.rjes.filters` sidecar.

When the sketch estimator is ready, LMDB switches to its LMDB-specific optimizer pipeline. This optimizer still runs the
standard RDF4J simplification steps, then adds LMDB-specific planning:

- It reorders basic graph pattern joins with a sketch-backed planner.
- It compares LMDB access paths for every join step and models `directLookup`, `prefixScan` and `fullScan` work.
- It can delay or move filters when that lets a more selective pattern or index drive the join.
- It rewrites selected optional and anti-join shapes when the planner can prove a cheaper equivalent plan.
- It annotates `EXPLAIN` output with planner metrics such as `plannerId=lmdb-sketch`, `plannerAlgorithm`,
  `plannedIndexName`, `plannedIndexAccessMode`, `plannedLookupComponents` and `plannedWorkRows`.

The join planner is implemented as a Cascades-style state exploration over join factors and deferred filter actions.
LMDB contributes the physical cost model: for each candidate step it estimates rows and work for the available LMDB
access paths, including direct lookups, prefix scans and full scans. The shared sketch planner then explores logical
alternatives with a small Pareto frontier instead of keeping only a single cheapest prefix. A candidate can stay in the
frontier when it is better on one dimension, such as lower final rows or lower uncertainty, even if another candidate
has lower immediate work.

For small join groups the planner uses full memoized exploration, reported as `plannerAlgorithm=DYNAMIC_PROGRAMMING`
and `mode=pareto-memo` in diagnostics. For larger groups it switches to bounded beam exploration, reported as
`plannerAlgorithm=GREEDY` and `mode=pareto-beam`. Both modes rank plans with the same cost vector: total planned work
rows, final rows, largest intermediate result, uncertainty rows and cartesian work rows. The final selected plan is
still materialized as the normal RDF4J join tree; the Pareto frontier is only an optimizer data structure.

The planner exposes most configuration through repository options, with a few low-level search-budget knobs as JVM
system properties. There is no repository option for selecting `pareto-memo` or `pareto-beam` directly. LMDB uses
memoized dynamic programming for join segments up to the dynamic-programming limit, which defaults to 20 factors, and
switches to greedy beam exploration above that limit. The memo frontier limit defaults to 8 alternatives, and the greedy
beam width also defaults to 8 alternatives.

The sketch estimator is selected automatically when no custom evaluation strategy is configured and the JVM max heap is
at least 2 GiB. Without a custom evaluation strategy, set `sketchEstimatorEnabled` explicitly to force it on or off. If
the estimator has no persisted snapshot, the store starts a background rebuild and uses the standard optimizer until the
sketch is ready. For benchmark setups or applications that need stable plans immediately after startup, call
`LmdbStore.awaitSketchesReady(...)` after initializing the repository.

The optimizer also learns filter selectivity. Runtime filter outcomes are recorded when query evaluation reports them.
If foreground sampling is enabled, the optimizer may spend a small bounded budget sampling a candidate pattern and filter
while planning. Background raw sampling can continue those queued sampling requests between queries. Store mutations reset
learned filter statistics and update or rebuild the sketches from the authoritative LMDB data.

### Configuring estimator, planner and optimizer options

Set these options before initializing the store:

```java
LmdbStoreConfig config = new LmdbStoreConfig()
    .setPageCardinalityEstimator(true)
    .setSketchEstimatorEnabled(true)
    .setSketchEstimatorSubjectBucketCount(4096)
    .setSketchEstimatorPredicateBucketCount(64)
    .setSketchEstimatorObjectBucketCount(4096)
    .setSketchEstimatorContextBucketCount(16)
    .setSketchEstimatorContextPairSketchesEnabled(false)
    .setSketchEstimatorThrottleEveryN(1_048_576L)
    .setSketchEstimatorThrottleMillis(2L)
    .setOptimizerSamplingEnabled(true)
    .setOptimizerSamplingMaxMillis(2L)
    .setOptimizerSamplingMaxRows(4096)
    .setBackgroundRawSamplingEnabled(true)
    .setBackgroundRawSamplingMaxMillisPerCycle(10L);

Repository repo = new SailRepository(new LmdbStore(dataDir, config));
```

The same options are available in repository configuration Turtle:

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#>.

config:sail.impl [
   config:sail.type "rdf4j:LmdbStore" ;
   lmdb:pageCardinalityEstimator true ;
   lmdb:sketchEstimatorEnabled true ;
   lmdb:sketchEstimatorSubjectBucketCount 4096 ;
   lmdb:sketchEstimatorPredicateBucketCount 64 ;
   lmdb:sketchEstimatorObjectBucketCount 4096 ;
   lmdb:sketchEstimatorContextBucketCount 16 ;
   lmdb:sketchEstimatorContextPairSketchesEnabled false ;
   lmdb:sketchEstimatorThrottleEveryN 1048576 ;
   lmdb:sketchEstimatorThrottleMillis 2 ;
   lmdb:optimizerSamplingEnabled true ;
   lmdb:optimizerSamplingMaxMillis 2 ;
   lmdb:optimizerSamplingMaxRows 4096 ;
   lmdb:backgroundRawSamplingEnabled true ;
   lmdb:backgroundRawSamplingMaxMillisPerCycle 10 ;
].
```

Workbench exposes the same fields in the LMDB repository template. If you omit `lmdb:sketchEstimatorEnabled`, LMDB uses
the automatic heap-based selection described above. The Workbench template sets it to `true` by default.

Search-budget properties are JVM system properties, so set them before startup, for example:

```bash
java \
  -Drdf4j.optimizer.sketchPlanner.dynamicProgrammingJoinArgLimit=20 \
  -Drdf4j.optimizer.sketchPlanner.frontierLimit=8 \
  -Drdf4j.optimizer.sketchPlanner.greedyBeamWidth=8 \
  ...
```

Use the configuration options in five groups:

- Enable or disable the LMDB planner with `sketchEstimatorEnabled`. Setting it to `false` keeps the page cardinality
  estimator available but disables the sketch-backed join planner and LMDB-specific sketch optimizer. Leaving it unset
  uses the heap-based automatic selection.
- Tune the planner's estimates with the sketch bucket options and context-pair sketches. These change estimator memory
  use and estimation granularity, but not the planner algorithm.
- Tune filter feedback with `optimizerSampling*` and `backgroundRawSampling*`. Set `optimizerSamplingEnabled` to `false`
  or either foreground sampling budget to `0` to avoid optimizer-time sampling. Set `backgroundRawSamplingEnabled` to
  `false` or `backgroundRawSamplingMaxMillisPerCycle` to `0` to stop background raw sampling.
- Tune Pareto and dynamic-programming search budgets with JVM system properties. Higher values can retain more plan
  alternatives but increase planning CPU and memory use. Invalid values fall back to defaults; the Pareto frontier and
  greedy beam width have a minimum of `1`, while a dynamic-programming limit of `0` forces LMDB's automatic planner to
  use greedy beam exploration for join segments.
- Inspect Pareto and dynamic-programming behavior with diagnostic JVM switches. Set
  `-Drdf4j.optimizer.sketchPlanner.traceDiagnostics=true` for planner traces, or
  `-Drdf4j.optimizer.lmdb.planAlternatives=true` to include costed final-frontier alternatives in diagnostics.

| Option | Default | Use |
|--------|---------|-----|
| `pageCardinalityEstimator` | `true` | Estimate single-pattern cardinality from LMDB pages. Disable only when exact planning counts justify the extra work. |
| `sketchEstimatorEnabled` | automatic | Enables the sketch estimator and LMDB optimizer. A custom evaluation strategy still disables this path. |
| `sketchEstimatorSubjectBucketCount` | `4096` | Number of subject buckets in the sketch layout. Increase for very large, subject-diverse data. |
| `sketchEstimatorPredicateBucketCount` | `64` | Number of predicate buckets. Predicates are usually much less diverse than subjects or objects. |
| `sketchEstimatorObjectBucketCount` | `4096` | Number of object buckets. Increase for object-heavy joins with many distinct values. |
| `sketchEstimatorContextBucketCount` | `16` | Number of context buckets. Increase for graph-heavy repositories with many named graphs. |
| `sketchEstimatorContextPairSketchesEnabled` | `false` | Adds context pair sketches. Enable for graph-heavy joins when extra estimator memory is acceptable. |
| `sketchEstimatorThrottleEveryN` | `1048576` | During a full sketch rebuild, sleep after this many scanned statements. `0` disables the interval check. |
| `sketchEstimatorThrottleMillis` | `2` | Milliseconds to sleep at each rebuild throttle point. `0` disables sleeping. |
| `optimizerSamplingEnabled` | `true` | Allows bounded foreground filter sampling during optimization. |
| `optimizerSamplingMaxMillis` | `2` | Maximum foreground sampling time per candidate, in milliseconds. `0` disables foreground sampling. |
| `optimizerSamplingMaxRows` | `4096` | Maximum rows read by foreground sampling per candidate. `0` disables foreground sampling. |
| `backgroundRawSamplingEnabled` | `true` | Allows queued background sampling requests to improve future filter estimates. |
| `backgroundRawSamplingMaxMillisPerCycle` | `10` | Maximum time for each scheduled background sampling cycle. `0` disables the background sampler. |
| `rdf4j.optimizer.sketchPlanner.dynamicProgrammingJoinArgLimit` | `20` | JVM system property. Maximum join factors for memoized dynamic-programming planning. Above this, LMDB uses greedy beam exploration. |
| `rdf4j.optimizer.sketchPlanner.frontierLimit` | `8` | JVM system property. Maximum Pareto alternatives retained per memo group. |
| `rdf4j.optimizer.sketchPlanner.greedyBeamWidth` | `8` | JVM system property. Beam width for greedy Pareto exploration. |

Most deployments should keep the defaults. Tune the bucket counts only if `EXPLAIN` shows unstable estimates on large
datasets, and prefer small increases before changing several dimensions at once. For write-heavy workloads, increase the
rebuild throttle or disable the sketch estimator if background rebuild work is not acceptable. For read-heavy analytical
workloads, forcing `sketchEstimatorEnabled` to `true` and waiting for sketches to become ready usually gives the best
plans.

For low-level diagnostics, start the JVM with `-Drdf4j.optimizer.sketchPlanner.traceDiagnostics=true` or
`-Drdf4j.optimizer.lmdb.planAlternatives=true` to add planner decision traces to optimizer diagnostics. The generic
`SketchBasedJoinEstimator` also accepts system-property overrides with the prefix
`org.eclipse.rdf4j.query.algebra.evaluation.sketch.SketchBasedJoinEstimator.`, but repository configuration should be
preferred for normal LMDB deployments.

## Required storage space, RAM size and disk performance
You can expect a footprint of around 120 - 130 bytes per quad when using the LMDB store
with 3 indexes (like spoc, ospc and psoc).
Therefore 120 - 130 GB storage space per 1 billion quads are required.
Please note that the actual footprint also depends largely on the size of IRIs and literals.

Some basic information about LMDB database and RAM sizes can be found in the
[OpenLDAP & LMDB Sizing Guide](https://3bmahv3xwn6030jbn72hlx3j-wpengine.netdna-ssl.com/wp-content/uploads/2018/08/OpenLDAP-LMDB-Sizing-Guide.pdf).

The bottom line is that more RAM is better. The best is to have enough RAM to accommodate the

entire database or at least the database's working set.

Another factor is the speed of your disks. You should use SSDs for larger databases.

More up-to-date information about LMDB can be found at: https://www.symas.com/symas-lmdb-tech-info
Especially the [SSD-benchmarks](http://www.lmdb.tech/bench/optanessd/imdt.html) may be of interest.

## Backup and restore
LMDB provides a set of [command line tools](http://www.lmdb.tech/doc/tools.html) that can be used
to backup and restore the value and triple databases.
Those tools can typically be used while the databases are in use as LMDB permits concurrent use
through multiple processes. Please note that it may happen that the backups of the value and triple 
databases may get out of sync if the LMDB store has active writes as each uses its own transaction.

## Control database file size
[LMDB](https://en.wikipedia.org/wiki/Lightning_Memory-Mapped_Database) uses memory-mapped files

for data storage. The size of the memory map needs to be configured and is also the maximum size

of the database. As shown above the map size can be controlled individually for the value and

triple databases via `LmdbStoreConfig.setValueDBSize(...)` and

`LmdbStoreConfig.setTripleDBSize(...)`. The size is automatically aligned
to the system's page size as suggested by the

[LMDB documentation](http://www.lmdb.tech/doc/group__mdb.html).

The database sizes can be increased when re-opening the LmdbStore.
Usually these sizes can be set to large values that must be smaller or equal
to the system's address space. This fact needs to be especially considered on 32-bit systems.

On Linux-based systems the file size grows dynamically according to the
actual size of the used memory pages for the data.
On Windows the file size is entirely allocated and care should be taken when
choosing the value and triple db sizes.

## Autogrow feature
RDF4J implements an **autogrow** feature to simplify the management of memory map sizes.

If it is enabled (which is the default) then RDF4J monitors the actual used pages and

automatically increases the map size if required.

This monitoring only has a very minimal overhead.
The only downsides are:
  - Some kind of stop the world approach is required to set the new map sizes where all running

    transactions are suspended for a short time.
  - A running write transaction may lead to a temporary overflow of data to disk if the
    current map size needs to be increased. This may be an issue with large transactions that

    can get slowed down.
