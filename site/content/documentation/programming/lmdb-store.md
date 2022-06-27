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
// set maximum size of value db to 1 GiB

config.setValueDBSize(1_073_741_824L);
// set maximum size of triple db to 1 GiB
config.setTripleDBSize(1_073_741_824L);

Repository repo = new SailRepository(new LmdbStore(dataDir), config);
```

## Required storage space, RAM size and disk performance
You can expect a footprint of around 120 - 130 bytes per quad when using the LMDB store
with 3 indexes (like spoc, ospc and psoc).
Therefore 120 - 130 GB storage space per 1 billion quads are required.
Please note that the actual footprint also depends largely on the size of IRIs and literals.

Some basic information about LMDB database and RAM sizes can be found in the
[OpenLDAP & LMDB Sizing Guide](https://3bmahv3xwn6030jbn72hlx3j-wpengine.netdna-ssl.com/wp-content/uploads/2018/08/OpenLDAP-LMDB-Sizing-Guide.pdf).

The bottom line is thatt more RAM is better. The best is to have enough RAM to accommodate the

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
