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


