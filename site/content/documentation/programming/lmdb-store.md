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
// enable optional query optimizations and sketch-based join estimation, both disabled by default
config.setExperimentalQueryOptimizations(true);
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

## Dictionary value cleanup

`asyncValueCleanup` is enabled by default, including when an older configuration omits the property. The default uses
bounded best-effort background dictionary cleanup to keep candidate processing off the application commit path. Pending
cleanup hints may be lost on a crash; this does not change committed RDF data.

To retain durable retirement recording, explicitly set `new LmdbStoreConfig().setAsyncValueCleanup(false)` before
startup. This preserves recoverable cleanup work after removing statements, not a guarantee of immediate reclamation.
Readers, unresolved values and unknown bounded liveness may still prevent dictionary records from being reclaimed.

Configure the background cleanup bounds before starting the repository:

```java
LmdbStoreConfig config = new LmdbStoreConfig()
        .setAsyncValueCleanupMaxBytes(8L * 1024 * 1024)
        .setAsyncValueCleanupBatchSize(256)
        .setAsyncValueCleanupAdmissionTimeoutMillis(250)
        .setAsyncValueCleanupShutdownTimeoutMillis(3_500);
```

This changes dictionary reclamation policy, not RDF transaction semantics or the dictionary format. Committed RDF
remains correct if a crash loses pending cleanup candidates. Unused dictionary records may then remain on disk, and
subsequent insertion of the same RDF values can reuse those records. Rollback discards candidates for unpublished work.
The existing `NONE` behavior can publish checkpoints before the final commit or rollback; cleanup for those already
published checkpoints remains eligible. Candidates become eligible only after successful statement publication;
application commits do not reclaim them.

The settings are captured at startup. `LmdbStore` also exposes matching setters, which reject changes while the store
is initialized. Close and reopen the store to change modes or limits. Changes to a caller-held `LmdbStoreConfig` after
initialization do not reconfigure the running cleanup worker.

| Setting | Default | Constraint and meaning |
| --- | ---: | --- |
| `asyncValueCleanup` | `true` | Enables one background cleanup worker; explicit `false` selects durable retirement recording. |
| `asyncValueCleanupMaxCandidates` | Derived from the cleanup byte budget | At least 16; an explicit value additionally bounds retained candidate slots, including producer reservations and worker workspace. |
| `asyncValueCleanupMaxBytes` | 1% of JVM maximum heap, capped at 64 MiB | At least 4096; bounds estimated cleanup memory, including producer reservations and worker workspace. Explicit values override the dynamic default. |
| `asyncValueCleanupBatchSize` | `256` | Positive and no larger than the candidate limit; the worker may use a smaller batch to meet the memory bounds. |
| `asyncValueCleanupMaxPendingProducers` | Byte budget / 1024, at least 1 | Positive upper bound on total active and queued producer streams. The charged byte and candidate budgets may constrain admission sooner. Explicit values override this derived total cap. |
| `asyncValueCleanupPressureTimeoutMillis` | `250` | Nonnegative maximum brief pause for safe new-writer admission under cleanup pressure; zero disables this optional pause. |
| `asyncValueCleanupAdmissionTimeoutMillis` | `250` | Positive interruptible total admission wait before publication, including any pressure pause; not a bound on complete transaction duration. Explicit longer values remain supported. |
| `asyncValueCleanupShutdownTimeoutMillis` | `3500` | Positive best-effort budget for finishing cleanup and saving pending work; active native calls must still finish safely. |

For an explicit 8 MiB byte ceiling, use the same LMDB predicates with either the current or legacy Sail configuration vocabulary:

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#> .

[] lmdb:asyncValueCleanup true ;
   lmdb:asyncValueCleanupMaxBytes 8388608 ;
   lmdb:asyncValueCleanupBatchSize 256 ;
   lmdb:asyncValueCleanupAdmissionTimeoutMillis 250 ;
   lmdb:asyncValueCleanupShutdownTimeoutMillis 3500 .
```

Omitting `asyncValueCleanup` now enables it intentionally, even in configurations written before this option existed
or when it previously defaulted to false. To keep durable retirement recording, add `lmdb:asyncValueCleanup false`.
This applies to both current and legacy Sail configuration vocabularies. Exports omit the default `true` value but
retain explicit `false`, so an explicit durable choice survives export and parsing.

Candidate buffers grow on demand within the configured byte budget; the default candidate and producer limits derive
from that budget, while worker processing stays in bounded batches. A transaction larger than the available queue
memory spills into a temporary, non-durable candidate stream; it does not reserve its entire candidate count in memory.
Runtime spills are best effort and are not forced to disk. Before consuming a queued spilled stream, cleanup validates
the completed stream prefix against its recorded checksum. A corrupt stream is discarded rather than used for
reclamation; this loses optional cleanup hints, not committed RDF data. This check is not a promise against changes
occurring after validation. Shutdown persistence uses the stronger forced, native-fenced protocol described below. Spill-file disk
usage can grow with the transaction's candidate count; the configured candidate and byte limits govern retained cleanup
memory. Admission can fail or be interrupted before publication if capacity remains unavailable. A successful publication
is not reported as rolled back because cleanup handoff later encounters backpressure.

Concurrent producers and waiting callers grow on demand within the charged byte budget and the effective producer-stream
limit; eight is only the initial staging-reservation baseline, not a default concurrency ceiling. Waiting callers are
counted separately from admitted or queued streams, with no more than the effective producer limit waiting at once.
Explicit configured caps remain hard limits. The estimated memory budget includes waiting-call bookkeeping and retained
producer-map capacity; callers fail before publication if these bounds cannot accommodate them.

Transaction change sets that overflow memory use disposable LMDB stores. In async mode, these temporary stores keep
their dictionary records until the entire temporary directory is closed and deleted. They do not create additional
cleanup workers or consume cleanup producer capacity, and retained internal read views cannot prevent change-set writes.

If `valueOverlayEnabled` is also enabled, asynchronous dictionary reclamation retires the current optional value overlay
after a successful transaction. Metadata-only durable continuations do not require this invalidation. Existing leased
views remain valid; subsequent lookups use LMDB until the overlay is explicitly rebuilt. This avoids copying large
lexical values into overlay updates during bounded cleanup, but can change query lookup performance.

Under pressure, eligible new writers briefly yield to cleanup before acquiring writer ownership or native read pins.
The pause targets acknowledgement of at least 10% of the queued work measured at the beginning of that pressure cycle;
concurrent arrivals do not move that target. It ends at the target or the pressure timeout (250 ms by default), because
readers may prevent progress. Acknowledged work is not necessarily physical dictionary reclamation: protections and
unknown liveness can defer records. The pressure pause uses the same admission wait budget, rather than adding another full timeout. If capacity is
still unavailable after that budget and best-effort spill handling, admission fails before publication; already
committed RDF work is not relabelled as failed. Blocking filesystem/native operations must finish safely and are not
forcibly cancelled at the wait deadline, so this is not a hard wall-clock bound. Explicit admission timeouts, including
the former 30-second default, remain supported. Existing observed/pinned
transactions do not wait on this optional pressure gate.

A long-lived reader can indefinitely delay reclamation. Bounded memory therefore does not imply that writers always
make progress: once producer capacity is exhausted, admission is subject to the configured timeout. Close read results
and transactions promptly when sustained deletion pressure matters. The worker continues existing durable retirement
work. Recursive dependency candidates are saved through the existing durable retirement mechanism inside the worker's
maintenance transaction, atomically with the dependency-count updates, instead of waiting to enter its own cleanup queue.

The default statement indexes, `spoc,posc`, do not include an object-leading index. Cleanup uses an object-leading
index when available; otherwise a predicate/object-leading index such as `posc` supports bounded exact probes across
distinct predicates, without scanning all statements. This permits reclamation with the default indexes when the
bounded proof completes. If the seek budget is exhausted or a necessary index is unavailable, the result remains
`UNKNOWN`: cleanup persists or defers the candidate rather than guessing that it is unused. Unused records may still
remain indefinitely under sustained deferral. This setting does not add or change indexes. Context liveness uses the
dedicated context-count table and does not require a context-first index. Reclamation measurements made with an
object-leading index should not be extrapolated to the default index set.

The cleanup byte default is computed from `Runtime.maxMemory()` (normally `-Xmx`), with a 4 KiB minimum.
It is a cleanup-memory ceiling, not a whole-JVM memory budget or an eager allocation. Candidate buffers grow on demand
within that ceiling; worker batches remain bounded independently. An explicitly configured candidate cap may reduce
the usable queue capacity, and producer streams can spill to disk. Omitted configuration stays omitted
on export so a different JVM recomputes its own defaults; explicitly configured byte, candidate and producer limits
are retained even when they equal the current JVM defaults.

Lazy values retained outside a tracked read result also remain protected. If their pinned dictionary revisions would
make cleanup accumulate revision history, the worker saves the affected root candidates through the existing durable
retirement mechanism and defers reclamation until those protections are released. These fallback writes occur in worker
maintenance transactions; if a lazy value keeps its protection for the lifetime of the open store, reclamation may wait
until a later reopen.

Unscoped raw Sail reads can expose lazy values in a way that makes the ValueStore itself retain their dictionary
revision. Dropping the returned value or closing that raw Sail result does not guarantee that cleanup resumes: another
revision transition followed by collection, or a reopen, may be needed. Scoped repository iteration avoids this promotion
to a retained raw dictionary revision.

The shutdown setting budgets cleanup and spooling, not the entire store shutdown. Shutdown stops new producer admission
and new cleanup batches, wakes producer-capacity waiters, and tries to finish the current batch and save remaining
candidates. If the worker is still using LMDB when the budget expires, shutdown reports an exception and retains both
native environments and the store lock; retry shutdown after the worker finishes. Spooling is best effort and may be
abandoned when its remaining budget expires. The budget stops new spool work; filesystem writes, forced synchronization,
and native calls already in progress may take longer. Completed candidate streams and their manifest are forced before
atomic publication. Publishing also forces the paired native store state, including the final value-store close marker,
even when `forceSync` is disabled. Directory synchronization is attempted where supported; cleanup hints are still best
effort rather than a complete durable retirement history.

Recovery requires a valid version-two manifest, the exact store identity and the exact certified native transaction.
Older version-one manifests, stale manifests and bare crash-leftover streams are discarded, never guessed into replay
authority. A missing or corrupt stream does not discard independently validated healthy siblings. Startup removes owned
orphan files even when async cleanup is disabled; failed deletions are retried on a later open. This changes no dictionary
format and does not promise recovery from corruption of the native databases themselves.

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
to `0` skips that grace without disabling the early admission warning or writer drain. Query admission waits are
interruptible: a query's maximum execution time or its cancellation ends them by interrupting the waiting thread. See
the transaction and recovery guide for the phased behavior and retry details.
