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
// enable sketch-based join estimation, disabled by default
config.setSketchEstimatorEnabled(true);
// set maximum size of value db to 1 GiB

config.setValueDBSize(1_073_741_824L);
// set maximum size of triple db to 1 GiB
config.setTripleDBSize(1_073_741_824L);

Repository repo = new SailRepository(new LmdbStore(dataDir, config));
```

The optional value hash cache stores precomputed `Value.hashCode()` results in `hashes.dat`. It is disabled by default.
When enabled, LMDB writes a `hashes.dat.integrity` sidecar on clean shutdown and only trusts the cache again on the
next startup if that integrity metadata validates. Invalid or stale hash cache files are discarded automatically and
the store falls back to recomputing hashes lazily.

Sketch-based join estimation is disabled by default. To enable it, set
`LmdbStoreConfig.setSketchEstimatorEnabled(true)` when creating the store. Repository configuration files can enable it
with `http://rdf4j.org/config/sail/lmdb#sketchEstimatorEnabled` set to `true`.

```turtle
@prefix lmdb: <http://rdf4j.org/config/sail/lmdb#> .

[] lmdb:sketchEstimatorEnabled true .
```

## Background adjacency indexes

When direct adjacency and startup building are enabled, repository initialization schedules adjacency and the configured
node-predicate projection on repository-owned workers and returns. Reads use LMDB until both construction and catch-up
finish, preserving transaction snapshots and read-your-writes. Different repositories can initialize and build concurrently,
including through `LocalRepositoryManager`.

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

If it is enabled (which is the default) then RDF4J monitors the actual used pages and

automatically increases the map size if required.

This monitoring only has a very minimal overhead.
The only downsides are:
  - Some kind of stop the world approach is required to set the new map sizes where all running

    transactions are suspended for a short time.
  - A running write transaction may lead to a temporary overflow of data to disk if the
    current map size needs to be increased. This may be an issue with large transactions that

    can get slowed down.
