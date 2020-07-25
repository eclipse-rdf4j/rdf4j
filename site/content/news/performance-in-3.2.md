---
title: "Performance improvements in RDF4J 3.2"
authors:
  - havard
  - jeen
date: 2020-06-14
layout: "single"
categories: ["news"]
---
<div class="big-emoji">&#x1F680;</div>

The release of [RDF4J 3.2.0](/release-notes/3.2.0/) introduced a large number
of performance improvements to the framework. 

One major change was the introduction of a new `Model` implementation, the
`DynamicModel`, and switching to this new model implementation throughout major parts
of the code base. The advantage of the `DynamicModel` over other implementations is 
that it uses a very light-weight internal datastructure initially, only converting
to a more heavily indexed form when necessary to answer particular queries. It
can avoid this upgrade, however, for many use cases where we are adding or 
removing data, iterating over all data, or checking for the existence of a triple.
<!--more-->

Since such simple interaction is a common pattern in transaction handling, the
DynamicModel has a large effect on the transaction isolation overhead in the
MemoryStore. Typical transaction isolation added roughly 100% overhead when
adding data to the store, with the introduction of the DynamicModel this has
been reduced to 25%, as long as there are no queries that cause the
DynamicModel to upgrade to a full LinkedHashModel.

When compared to the latest 3.1 release, refinements to how
`connection.remove(...)` executes on the MemoryStore makes it 40% faster for
bulk transactions (`IsolationLevels.NONE`) and up to 8 times faster for higher
transaction levels. These changes were already introduced to the NativeStore in
3.1.0 which at the time made `connection.remove(...)` approximately 14 times faster
when using IsolationLevels.NONE.

The Native Store has received three performance upgrades in the 3.2 release:
predictive reads, dynamic caching and lower IO for transactions. 

_Predictive reads_ are an improvement in how bytes are read from disk. Some of
the native store data structures store many differently sized blocks of data in
the same file.  We would then have to first read the size of the block, and
then read the block (2 IOPS in total) in order to retrieve a block. Predictive 
reading means that we instead perform a slightly bigger read (1 IOPS) hoping 
to both read the size and the whole block. This is predictive since we have to 
guess the size of the block based on other blocks we have read recently. Guessing
correctly reduces IOPS by 50% while the cost of guessing wrong would still only 
be 2 IOPS per block.

_Dynamic caching_ is a technique where the native store uses a garbage
collection-sensitive cache. It will cache as much data as possible, but cached
items will be removed if the application starts running low on memory.

The _lowering of transaction IO_ has been achieved by various small improvements
in the transaction handling process, including low-level caching of writes for
the various native store files and reducing the IOPS for transaction state logging. 
It gives us around a 15% higher transaction throughput for small transactions. 

Predictive reads and dynamic caching together make queries that read a lot of data up 
to 72% faster. In our benchmarks we saw a 69% performance improvement for a 
query that retrieve all distinct predicates, and nearly 72% for a query that retrieves 
a large number of values and groups and aggregates them.  The dynamic caching 
will help with all queries in general and adapts to the amount of available RAM.  
Typically the dynamic cache uses around 2 GB of RAM for a NativeStore with 250 
million triples.

All in all, these improvements mean that both the native store and the memory
store are significantly faster, and better able to cope with larger datasets,
in release 3.2.0.

For more details on the precise benchmarks we ran, have a look at our [git repository](https://github.com/eclipse/rdf4j), in particular:

  - [memory store benchmark tests](https://github.com/eclipse/rdf4j/tree/master/core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/benchmark)
  - [native store benchmark tests](https://github.com/eclipse/rdf4j/tree/master/core/sail/nativerdf/src/test/java/org/eclipse/rdf4j/sail/nativerdf/benchmark)

If you're interested, you can of course even run these benchmarks yourself, to see how your own hardware scores.
