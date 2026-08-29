# `lmdb-5.3.2-complex-store.zip`

A real LMDB store written by **RDF4J 5.3.2**, used by `LegacyLmdbStoreFixtureTest` to prove that RDF4J 6.0 can open,
query, and extend a 5.3.x store without converting it away from the legacy on-disk format.

The archive contains `lmdbrdf.ver` (`5.3.2`), `namespaces.dat`, `triples/triples.prop`, `triples/data.mdb` and
`values/data.mdb`. The `lock.mdb` files are deliberately omitted; LMDB recreates them.

## What it covers

The store is intentionally varied, so it exercises the parts of the format that the compatibility layer reinterprets:

- legacy `(ordinal << 2) | type` value IDs and legacy value-record prefixes;
- the 5.3.x literal record layout (full 8-bit language length, no base-direction bits);
- literals RDF4J 6.0 would otherwise inline into the ID (small/large ints, doubles, floats, booleans, bytes, shorts);
- typed literals across many datatypes, including two custom datatype IRIs;
- language-tagged literals with non-ASCII labels (`ja`, `ru`, `ar`, emoji) and a long language tag;
- an 8208-character literal, forcing resolution through the hash index rather than a direct value record;
- 20 blank nodes in subject and object position;
- four contexts, one of them a blank-node named graph;
- 451 inferred statements, held in the legacy `-inf` databases;
- a non-default index configuration, `spoc,posc,ospc,cspo`;
- value IDs freed by garbage collection, plus statements written after the free list existed.

Read back by RDF4J 5.3.2 itself: **2507 explicit** statements, **451 inferred**, 5 namespaces, 4 contexts. Those are the
numbers `LegacyLmdbStoreFixtureTest` asserts.

## Regenerating

`lmdb-5.3.2-complex-store-generator.java.txt` is the generator source. It must be compiled and run against the
**5.3.2 jar** — not `target/classes` — because `LmdbStore` reads its version from `META-INF/maven/.../pom.properties`,
and a classes directory makes it write `devel` into `lmdbrdf.ver` instead of `5.3.2`.

```bash
git worktree add --detach /tmp/rdf4j-532 5.3.2
cd /tmp/rdf4j-532 && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl core/sail/lmdb -am install -DskipTests
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl core/sail/lmdb dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
```

Then compile and run the generator with `core/sail/lmdb/target/rdf4j-sail-lmdb-5.3.2.jar` and `/tmp/cp.txt` on the
classpath, passing the output directory as the single argument, and zip the result without the `lock.mdb` files:

```bash
cd <output-dir> && zip -9 -r -X ../lmdb-5.3.2-complex-store.zip . -x "*/lock.mdb" "lock.mdb"
```

Changing the fixture means updating the expected counts in `LegacyLmdbStoreFixtureTest`.
