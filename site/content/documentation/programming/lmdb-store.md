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

For the user-visible effects of transaction snapshots, map growth, recovery and store upgrades, see
[LMDB transactions, map growth, and recovery](/documentation/programming/lmdb-store-transactions/).

## Dependencies for the LMDB Store and native extensions

To make use of the LMDB Store, you'll need to include the following Maven dependency:

```xml
<dependency>
  <groupId>org.eclipse.rdf4j</groupId>
  <artifactId>rdf4j-sail-lmdb</artifactId>
</dependency>
```

Alternatively you can also rely on the `rdf4j-storage` pom dependency (see (["Which maven artifact?"](/documentation/programming/setup/#which-maven-artifact)), which includes the LMDB Store.

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
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
...
File dataDir = new File("/path/to/datadir/");
Repository repo = new SailRepository(new LmdbStore(dataDir));
```

The above code initializes a new or loads an existing repository at the location specified by `dataDir`.

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
...
File dataDir = new File("/path/to/datadir/");
Repository repo = new SailRepository(new LmdbStore(dataDir));
```

By default, the store uses the two indexes: `spoc` and `posc`.

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
// disable the predicate guarantee index, enabled by default
config.setPredicateGuaranteeIndexEnabled(false);
// disable automatic predicate guarantee rebuilds on startup, enabled by default
config.setPredicateGuaranteeIndexAutoRebuild(false);
// exclude selected predicates from predicate guarantee tracking
config.setPredicateGuaranteeExcludedPredicates("http://example.com/vocab/volatilePredicate");
// enable optional query optimizations, disabled by default
config.setExperimentalQueryOptimizations(true);
// enable sketch-based join estimation, disabled by default
config.setSketchEstimatorEnabled(true);
// set maximum size of value db to 1 GiB

config.setValueDBSize(1_073_741_824L);
// set maximum size of triple db to 1 GiB
config.setTripleDBSize(1_073_741_824L);

Repository repo = new SailRepository(new LmdbStore(dataDir, config));
```

The generic value-ID, namespace and namespace-ID caches can use a selected implementation:

```java
import org.eclipse.rdf4j.sail.lmdb.config.LmdbCacheImplementation;

config.setCacheImplementation(LmdbCacheImplementation.ADAPTIVE_SLOT)
      .setValueIDCacheSize(1024)
      .setNamespaceCacheSize(512)
      .setNamespaceIDCacheSize(32);
config.validate();
```

The default is `LOSSY_CONCURRENT`. The other choices are `CONCURRENT`, `BOUNDED_CONCURRENT`, `ADAPTIVE_SLOT`,
`MEMOIZING_SLOT`, `COMPACT_SLOT`, `SLOT_CAS`, `SLOT_CELL`, and `SLOT_SNAPSHOT`. Specialized value, hash, snapshot
and transaction caches keep their own implementations. Admission is best-effort; cache hits still undergo the
store's existing validity checks, and misses resolve against the native dictionary.

The six slot implementations accept capacities from 1 to 2^25. The other three accept 0 to 2^30. Positive sizes
round up to a power of two with at least four slots. Zero is unbounded for `LOSSY_CONCURRENT` and `CONCURRENT`,
disables `BOUNDED_CONCURRENT`, and is invalid for the slot implementations. Negative size settings retain their
existing meaning of using the default size. Invalid combinations fail validation before native store initialization;
the store does not substitute a different implementation.

For RDF configuration, add the following property to the LMDB Sail configuration node. Enum names are case-sensitive;
omitting the property preserves `LOSSY_CONCURRENT`:

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#> .

[] lmdb:cacheImplementation "ADAPTIVE_SLOT" .
```

`DatagovLoadIsolationBenchmark` includes every selectable implementation through its `cacheImplementation` JMH
parameter. See the module's `cache-competition/README.md` for real LMDB benchmark commands and the distinction
between those runs and the supplied synthetic cache benchmarks.

The optional value hash cache stores precomputed `Value.hashCode()` results in `hashes.dat`. It is disabled by default.
When enabled, LMDB writes a `hashes.dat.integrity` sidecar on clean shutdown and only trusts the cache again on the
next startup if that integrity metadata validates. Invalid or stale hash cache files are discarded automatically and
the store falls back to recomputing hashes lazily.

### Predicate guarantee index

The predicate guarantee index is enabled by default. It records, per predicate, whether all known object values have
the same RDF term domain, for example only IRIs or only literals. The LMDB query planner can use that guarantee to
avoid work for statement patterns whose object position is known to be incompatible with the predicate's object
domain.

The index is maintained during normal writes and is rebuilt on startup when the stored index metadata is missing,
stale, or uses different exclusion settings. Automatic startup rebuilds are enabled by default. Disabling automatic
rebuilds keeps startup predictable for large stores, but LMDB will only use the predicate guarantee index if the
stored metadata is current. If the metadata is stale and rebuilds are disabled, the store ignores the index until it
is rebuilt by starting with automatic rebuilds enabled again.

Predicate guarantees are stored in LMDB for persistence and also loaded into an in-memory copy when the store starts.
Query planning reads the in-memory copy rather than relying on the operating system to keep the LMDB mmap pages hot.
Every committed guarantee write updates both the persisted record and the in-memory copy together, so the planner does
not observe a heap/disk mismatch during normal operation.

Use `LmdbStoreConfig.setPredicateGuaranteeIndexEnabled(false)` to disable the feature completely. When disabled,
LMDB neither records new predicate guarantees nor reads existing ones.

Use `LmdbStoreConfig.setPredicateGuaranteeIndexAutoRebuild(false)` to keep the feature available while preventing
startup from rebuilding missing or stale guarantee data.

Use `LmdbStoreConfig.setPredicateGuaranteeExcludedPredicates(...)` to skip selected predicates. The value is a comma
or whitespace separated list of predicate IRIs. IRIs may be written either as plain strings or wrapped in angle
brackets. Excluded predicates are not recorded, are not used for planning, and changing the exclusion list changes
the metadata version so that automatic rebuilds refresh the remaining guarantees.

Sketch-based join estimation is disabled by default. To enable it, set
both `LmdbStoreConfig.setExperimentalQueryOptimizations(true)` and
`LmdbStoreConfig.setSketchEstimatorEnabled(true)` when creating the store. Repository configuration files can enable it
with both `http://rdf4j.org/config/sail/lmdb#sketchEstimatorEnabled` and
`config:sail.experimentalQueryOptimizations` set to `true`.

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#> .
@prefix config: <tag:rdf4j.org,2023:config/> .

[] lmdb:sketchEstimatorEnabled true ;
   config:sail.experimentalQueryOptimizations true .
```

The legacy per-column subject, predicate, object and context sketch bucket-count accessors are deprecated compatibility
no-ops: getters return `-1`, and setters do not change the Frontier synopsis. The synopsis sizes itself from the configured
sketch-estimator memory budget. The context-pair sketch toggle is also deprecated; its getter remains `false` and its setter
is a no-op. These retired per-column settings do not control estimate memory.

## Store formats and returning to an older release

Existing format-2 repositories keep their format, inlining capabilities and RDF-term encodings when this code opens or
writes them. They are not automatically upgraded. Compatible maintenance metadata may change, but the repository can
still be read and written by the older format-2 implementation. Use the repository's original `inlineLiterals` setting;
a recorded setting cannot be changed by a new configuration. If an existing format-2 repository has no recorded inlining
setting, supply its original configuration when opening it.

This compatibility guarantee covers repository data and format metadata. It does not cover Java-serialized query
algebra across RDF4J versions.

Format-2 metadata cannot guarantee one stored ID per RDF term. ID-keyed native evaluation therefore uses the Java
fallback for these repositories, even when native evaluation is enabled.

Batched RDF additions to existing format-2 repositories continue to use their original encodings and native batch
writes. The standalone bulk loader publishes a new format-6 repository and requires an empty target; it does not
replace or upgrade an existing repository in place. These write paths do not depend on the experimental query profile.

A successful standalone bulk load also publishes `config.ttl`, using the final target directory name as its repository
ID. That name must satisfy the local repository manager's portable-name rules. The configuration records the effective
statement and RDF-star indexes, all selected store settings, and the final native map capacities, including growth during
loading and the selected capacity reserve. Place the target directly in the server's repository directory, or move the
complete directory to another repository manager while retaining its name. If you rename the repository directory,
update its repository ID in `config.ttl` to match.

Choose value-store and triple-store capacity independently with `--value-capacity POLICY` and `--triple-capacity POLICY`.
Interactive setup offers the same choices separately for each store: default headroom, a minimum size, or a factor.
Press Enter to keep the default, or choose a minimum size such as `64MiB` or a total-count factor such as `2`, `3`, or `1.5`.

| Policy | Meaning |
|---|---|
| `default` | Reserve room for the smaller of 100,000 additional items and 10% of the actual loaded distinct count, rounded up. |
| `min:64MiB` | Keep the native map at least 64 MiB. This is a floor, never a limit: larger data still loads and grows normally. Byte counts also accept KiB, GiB and TiB. |
| `factor:2` | Reserve room for twice the total loaded count. `factor:3` means three times the count; `factor:1.5` means one and a half times. Factors must be finite and at least one. |

For example, reserve at least 64 MiB for values and room for twice the loaded statement count:

```bash
./lmdb-bulk-load.sh --store repositories/example --input data.nt \
  --value-capacity min:64MiB --triple-capacity factor:2
```

The value count is the number of unique stored dictionary and RDF-star term identities; inline values do not consume
dictionary entries. The triple count is the number of distinct stored statements, so duplicate input does not increase
that count. RDF-star term indexes share the value store's capacity. Adding a statement can require space in both stores,
and each reserve must cover its own demand. Factors multiply counts, rather than map byte sizes. Minimum-size policies
reserve a byte floor without promising an additional item count. All policies retain the space required for loaded data
and prepared startup, and the generated `config.ttl` records the actual final bytes for both maps.

Count-based reservations use the encoded-size envelope observed while loading and the existing native write estimates.
They assume subsequent items fit that envelope, index families and generated write/batch settings remain unchanged,
and ordinary appends do not keep historical readers pinned across writes. Increasing `bulkOperationSize` later can
increase native admission demand. Arbitrarily larger future literals or pinned transaction histories can require
growth before the selected count is reached. Resuming an interrupted load keeps its recorded policies; an explicitly
conflicting policy is rejected.

An unfinished older workspace that has not published its native output can rebuild using the new default policies.
Already-published output without recorded capacity policies and completed sizing evidence is preserved, and recovery
reports a compatibility error. It remains an older publication under its previous capacity contract; use its original
loader to finish recovery, or retain that store and choose a new empty target for a capacity-managed load.

An item-count percentage is not a percentage of native map bytes. Existing native copy-on-write admission estimates can
require a substantially larger virtual map reserve than the item percentage suggests. Map size reserves address capacity;
physical memory and disk allocation depend on the platform and LMDB mapping mode.

The loader establishes reference-count integrity and reserves startup capacity before publication, using its existing
value and index passes. Its preparation metadata binds both native environments to the same generation and committed
transactions. An unchanged repository opened with its generated configuration skips startup discovery scans,
reference-count reconstruction, value reclamation, retirement recovery and map growth. Clean ordinary writes and shutdown
retain bounded startup authority. Current-format stores can also use native clean metadata when the optional preparation
sidecar is missing: startup checks known database names, manifests and statistics rather than traversing stored RDF values.
Dirty or stale native integrity metadata, unknown writers, invalid formats and real pending reclamation or retirement work
trigger the required validation or recovery. A reusable free-ID pool alone does not require recovery. Explicitly enabled
experimental value-overlay, sketch-estimator and
direct-adjacency caches retain their separate startup build behavior; the standalone defaults leave them disabled.

For the Java bulk-loader API, repository configuration remains application-owned. Its
`preparedGenerationWriter` callback receives the private directory and effective `LmdbStoreConfig`, so application
metadata can be included in the same publication. A callback failure or cancellation leaves the load unpublished, and
native database files must not be modified by the callback. Resuming a current load preserves its recorded settings and
publishes configuration with the completed native generation. Older interrupted workspaces without required integrity
metadata must be restarted from their inputs using a new workspace.

A genuinely new, empty repository uses format 6. It records an older-reader rejection marker and cannot be opened by
the format-2 implementation, even when optional query optimizations and all three accelerators are disabled. To move
format-6 data to an older release, export RDF with a format-6-capable reader, then import it into a fresh repository created
by the older code. A backup of the format-6 directory remains format 6 and cannot be opened by the older reader.

Experimental branch formats 3, 4 and 5 are not supported. Export those repositories using the code that created them,
then import into a new repository. Unsupported versions and inconsistent format metadata are rejected before storage
writers open. Removing or changing format metadata is not a supported conversion.

## Optional query optimizations

`experimentalQueryOptimizations` defaults to `false` for each repository. The default keeps optional planning,
learned estimates, additional shared ordered-index access, seek hints, eager indexed replay and extra calendar
exact-term ordering ties disabled. Existing backend ordering optimizations and explicitly enabled native-engine
physical operators remain available.
Ordinary LMDB query results use the existing materializer with one-row consumption. Fixes required for correct results,
binding scope, multiplicity and error handling remain active in both profiles, as does the configured `STANDARD` or
`STRICT` query evaluation mode.

Set the configuration before constructing the store:

```java
config.setExperimentalQueryOptimizations(true);
```

Alternatively, call `store.setExperimentalQueryOptimizations(true)` before initializing the store.

The RDF option is `config:sail.experimentalQueryOptimizations true` on the base Sail node, as described in
[Sail configuration](/documentation/reference/configuration/#base-sails). Restart the repository to change the profile.
Native evaluation, direct adjacency and the value overlay keep their own independent enable settings; selecting any of
them does not enable this shared query profile. JVM tuning properties also cannot enable it.

Native evaluation, direct adjacency, and the compressed value overlay are independent per-store startup options. They all
default to off. Enable only the accelerators needed by a repository:

```java
config.setNativeEvaluationEnabled(true);
config.setDirectAdjacencyEnabled(true);
config.setValueOverlayEnabled(true);
```

The equivalent repository configuration is:

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#> .

[] lmdb:nativeEvaluationEnabled true ;
   lmdb:directAdjacencyEnabled true ;
   lmdb:valueOverlayEnabled true .
```

With native evaluation disabled, the store uses `DefaultEvaluationStrategyFactory`. Factory selection does not change the
query evaluation mode: the existing `STANDARD` or `STRICT` mode selected by the Sail default or repository configuration
continues to apply.

Legacy adjacency mode/build settings and value-overlay system properties tune their respective accelerators; they do not
turn them on. The `rdf4j.lmdb.nativeQueryEngine.enabled` system property controls routing inside the native strategy,
but does not select that strategy on its own. When value overlay is enabled, absent
`rdf4j.lmdb.valueOverlay.maxBytes` uses the shared retained-memory cap (2 GiB by default) for startup warm-up; an
explicit zero disables automatic warm-up and a positive value overrides the startup base budget. All overlay allocations
remain subject to the shared retained-memory cap.
These options affect in-memory runtime behavior only, so existing LMDB data needs no storage migration. Repositories that
relied on the former automatic native evaluator or adjacency activation should add the matching opt-in setting to their
repository configuration. The three startup options are captured when the store is constructed; restart the repository
after changing any of them.

Native Janino compilation keeps bounded query-shape open counts separately from its compiled-class cache. Set
`-Drdf4j.lmdb.janinoCodegen.maxTrackedShapes=16384` at JVM startup to control the maximum histories **per tracker**.
IR row and aggregate execution share one process-wide tracker; legacy Janino aggregation uses a second, giving a
default maximum of 32,768 retained histories across all repositories. Recently accessed shapes retain their counts while
cold histories are evicted individually; an evicted shape restarts at one and may compile later. This entry limit is
independent of `rdf4j.lmdb.janinoCodegen.maxEntries` (512 compiled classes by default) and query-memory byte limits.
The setting accepts a trimmed positive integer; absent, malformed, overflowing, zero or negative values use 16,384.
Existing trackers capture the setting at construction, so restart the JVM after changing it.

## Background adjacency indexes

Direct adjacency is disabled by default. Opt in with `LmdbStoreConfig.setDirectAdjacencyEnabled(true)` or the persisted
`lmdb:directAdjacencyEnabled true` setting. When enabled, the default mode is `PREFER` and startup building defaults on.
Legacy `directAdjacencyMode`, `directAdjacencyBuildOnStart`, and tuning values alone do not activate the index; they are
ignored for activation while the new flag is false. Setting the flag true together with `DISABLED` is rejected.

When direct adjacency and startup building are enabled, repository initialization schedules adjacency and the configured
node-predicate projection on repository-owned workers and returns. Reads use LMDB until both construction and catch-up
finish, preserving read-your-writes. A map resize can invalidate a pinned LMDB snapshot before publication; a mapped
repository read then fails with a `SNAPSHOT transaction invalidated` error that asks the caller to retry after the resize.
Repository APIs may wrap the underlying `SailException` in a `RepositoryException`.

After construction, a `SNAPSHOT_READ` operation fully served by a built immutable adjacency view can retain its original
query snapshot across map growth while a later query sees committed writes. Adjacency may decline a shape and fall back to
mapped reads, so it does not guarantee every operation will avoid resize invalidation. Callers should continue to handle
the retryable error. Different repositories can initialize and build concurrently, including through `LocalRepositoryManager`.

Writes made during a populated repository's startup accumulate committed deltas. The builder first consumes a fixed
revision range while writes continue. It then fences new backing writes, lets admitted writes hand over their deltas,
and publishes the fully caught-up generation. Readers continue to use LMDB during this transition.

After publication, commits normally return only after their explicit and inferred index changes are published. This rule
applies immediately when the repository is empty at initialization. Primitive mutation batches are prepared asynchronously
while LMDB continues writing; commit waits only for remaining publication work. Uncommitted changes stay private.

Configure the backlog admission threshold in bytes:

```java
config.setDirectAdjacencyBacklogMaxBytes(32L * 1024 * 1024);
```

The corresponding repository configuration is:

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#> .

[] lmdb:directAdjacencyBacklogMaxBytes 33554432 .
```

`0` (the default) selects AUTO: 1% of the effective adjacency memory budget, bounded between 8 MiB and 2 GiB.
Negative values are rejected. Backlog accounting includes unpublished payloads and their queue and preparation metadata.
At the threshold, new backing writes pause; an admitted transaction can finish within its bounded capture allowance.
The overall adjacency memory cap still applies. Reservations are released before commit waits for publication.

The system property `rdf4j.lmdb.directAdjacency.synchronousMaintenance=false` explicitly disables waiting
for index publication. Existing mode, coverage, startup-build and incoming node-predicate settings remain effective.
Transactions exceeding the capture allowance commit to LMDB, mark a revision gap, and schedule an asynchronous rebuild.
Maintenance failures and memory refusal keep LMDB available, wake blocked writers, and report degraded readiness;
recoverable failures use the existing retry policy. Failure of only the optional node-predicate projection preserves
adjacency access for unaffected capabilities. These indexes are derived in memory, so this change requires no persisted
index-format migration.

For diagnostics, `LmdbStore.getDirectAdjacencyReadinessDescription()` includes backlog bytes and limit, peak backlog,
admission block reason, catch-up target, publication revision, cumulative commit wait nanoseconds, and the latest cutover
duration. `awaitDirectAdjacencyReady(timeout, unit)` provides an explicit bounded readiness wait. A backlog pause is resolved
by consumption or degraded fallback; a catch-up pause lasts until admitted writes hand off and the final generation is
published. Inspect `lastBuildFailure` and gap revisions when readiness is degraded.

## Cardinality estimation

The LMDB Store normally estimates statement-pattern cardinalities by reading a bounded number of pages from the
selected LMDB index. This avoids scanning the complete matching range while giving the query optimizer structural
information about the B-tree. If page inspection cannot produce a safe estimate, the store automatically falls back to
the cursor-sampling estimator used by RDF4J 5.3.2.

For diagnosis or emergency compatibility, start the JVM with the following system property before creating the store:

```text
-Dorg.eclipse.rdf4j.sail.lmdb.disablePageWalkingEstimator=true
```

This bypasses page walking and uses the complete RDF4J 5.3.2 estimation path, including its index-selection rules and
cursor sampler. The property is read when each LMDB store is opened; restart or reopen the store after changing it.
Omitting the property, or setting it to any value other than `true`, leaves page walking enabled.

`LmdbStoreConfig.setPageCardinalityEstimator(false)` also selects the complete RDF4J 5.3.2 estimation path. The system
property provides the same compatibility behavior as an operational override when changing repository configuration is
not practical.

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

If it is enabled (which is the default), RDF4J monitors used pages and increases the map size when required. Resizing
requires coordinating native readers while LMDB remaps the environment. Ordinary statement reads preserve their
TripleStore snapshot when only the ValueStore dictionary grows. TripleStore growth, or a read that depends on the
dictionary's triple-term membership, can invalidate a pinned query result or an established
`SNAPSHOT`/`SERIALIZABLE` transaction and require a retry. Writes normally retain a
complete TripleStore replay journal, which may use temporary disk space. Large, lightly filled paired maps with
conservative successful allocation history may omit tracking; unexpected exhaustion then attempts capacity growth and reports
`LmdbTransactionRetryException` through the cause chain. Roll back and retry the entire transaction in that case;
recovery retains tracking. See
[LMDB transactions, map growth, and recovery](/documentation/programming/lmdb-store-transactions/) for retry guidance,
reader lifetime, and persistence details.

The per-store `mapGrowthThreshold` setting (default `0.75`) controls the soft allocated-page fullness trigger for
closing admission and scheduling phased map growth. It also guides growth sizing below that trigger. The threshold
measures allocated high-water pages rather than live RDF statements, and must be finite and strictly between `0` and
`1`. `mapGrowthReadDrainTimeoutMillis` (default `30000`) is the reader grace after logical writers finish; setting it
to `0` skips that grace without disabling the early admission warning or writer drain. Query admission waiters keep the
query's original deadline, and waits without a deadline remain interruptible. See the transaction and recovery guide
for the phased behavior and retry details.
