# High-Performance Java Libraries

Use this reference when the JDK baseline is known and you need to decide whether a library meaningfully improves layout, primitive support, concurrency, serialization, caching, runtime code generation, or observability.

## Selection rule

Do not add a library because it is "fast" in the abstract. Add it only when it buys at least one concrete property:
- primitive collections without boxing
- better buffer or off-heap control
- lower-contention queues or caches
- tighter binary encoding
- runtime compilation or code-generation support you actually need
- observability or benchmarking you cannot credibly replace

Always compare against the simplest viable JDK baseline first.

## JDK first choices

Start here before adding dependencies:
- `ArrayDeque`: queue/stack/deque default
- `BitSet`: dense boolean/set algebra and bit-parallel state
- `PriorityQueue`: heap baseline
- `ConcurrentHashMap`: baseline concurrent map
- `LongAdder` / `LongAccumulator`: striped counters under contention
- `VarHandle`: low-level atomic/ordered field access
- `ByteBuffer`: baseline direct or heap buffer abstraction
- `javax.tools.JavaCompiler`: baseline full Java compiler when compatibility matters more than minimal footprint
- JMH, JFR, and `jcmd`: measurement and runtime evidence

If these solve the problem with acceptable cost, stop.

## Primitive collections

### fastutil

Use when:
- primitive maps, sets, lists, heaps, or big arrays are needed
- boxing in JDK collections is visible in memory or CPU profiles

Good fit:
- `int -> int`, `long -> long`, and similar dense/sparse maps
- adjacency lists, frequency maps, index maps

Caution:
- still benchmark against flat arrays when keys can be compressed

### HPPC

Use when:
- you want lean primitive collections with a smaller API surface
- hot loops need primitive containers without a broad framework

### Eclipse Collections primitive containers

Use when:
- you already use Eclipse Collections
- you need richer collection operations but want primitive variants

## Buffers, off-heap, and low-latency plumbing

### Agrona

Use when:
- you need direct buffers, ring buffers, counters, or low-latency transport helpers
- you want explicit control over memory layout and flyweight-style access

### Chronicle Bytes / Chronicle Queue / Chronicle Map

Use when:
- off-heap or memory-mapped storage is intrinsic to the design
- inter-process communication or persisted queue semantics matter

Caution:
- operational complexity is much higher than plain on-heap structures

### Netty `ByteBuf`

Use when:
- the stack already uses Netty
- pooled buffers and zero-copy byte handling matter

Avoid when:
- pulling in Netty only for a small standalone buffer need

## Runtime compilation and code generation

### Janino

Use when:
- you need embedded, in-memory compilation of generated Java at runtime
- generated classes stay compact, conservative, and easy to split
- the same generated shapes execute enough times to amortize compile cost
- you want a pragmatic JVM path for expression evaluators or medium-size query-engine pipelines

Good fit:
- generated filters and projections
- scalar expression evaluators
- compact aggregation or join helpers
- metadata or dispatch classes that are expensive to interpret repeatedly

Caution:
- do not use Janino as a reflex
- plan for code-size limits, compile latency, cache keys, classloader lifetime, and fallback
- generated Java should stay conservative and simple
- if the source gets large or needs richer Java support, compare against the JDK compiler or a bytecode-level approach

### ASM / bytecode-generation libraries

Use when:
- source-code generation becomes brittle or expensive
- you need exact bytecode control
- Janino or javac overhead becomes the bottleneck

Caution:
- complexity is much higher than template-based Java generation
- debugging and maintenance costs rise quickly

## Concurrency and queues

### JCTools

Use when:
- single-producer/single-consumer or MPSC queue semantics are well defined
- `java.util.concurrent` queues show contention or allocation issues

### LMAX Disruptor

Use when:
- you have a staged event-processing pipeline
- extremely low latency and mechanical sympathy matter more than API simplicity

Caution:
- only a fit for specific architectures; not a general queue replacement

### Caffeine

Use when:
- you need a production cache with strong hit-rate behavior and concurrency
- cache eviction policy quality matters, not just raw map speed
- you need to cache compiled artifacts, metadata, or other reuse-heavy structures with bounded growth

## Bitmaps and compressed sets

### RoaringBitmap

Use when:
- integer sets are sparse-to-medium density
- you need fast unions, intersections, or membership with lower memory than plain bitsets

Good fit:
- analytics filters
- posting lists
- visited/frontier sets with large sparse ids

## Serialization, parsing, and wire formats

### Jackson

Use when:
- interoperability and ecosystem support matter more than max throughput

Tune before replacing:
- reuse `ObjectMapper`
- avoid tree model on hot paths
- stream when full materialization is unnecessary

### DSL-JSON / jsoniter / specialized parsers

Use when:
- JSON remains required but generic reflection-heavy parsing is too expensive

### Protocol Buffers

Use when:
- schema evolution and interoperability matter

### FlatBuffers / SBE / Chronicle Wire

Use when:
- binary layout, lower-copy reads, or ultra-low latency wire handling matter more than generality

Caution:
- these choices affect interfaces and tooling, not just speed

## Numerics and vector-style work

### JDK Vector API

Use when:
- the workload is data parallel
- you can express operations as bulk lane-wise math

Caution:
- JDK-version-sensitive; validate on the active runtime

### EJML and similar numerics libraries

Use when:
- matrix or numeric kernels dominate and bespoke loops are not the business value

## Benchmarking and profiling

### JMH

Use when:
- you need trustworthy microbenchmarks

### JFR

Use when:
- you need low-overhead production-friendly profiling

### async-profiler

Use when:
- you need CPU, wall, alloc, or lock evidence with low overhead

## Practical defaults

If the bottleneck is:
- boxing in maps/sets: try `fastutil` first
- queue contention: compare JDK queues with `JCTools`
- cache behavior: use `Caffeine`
- sparse integer set algebra: use `RoaringBitmap`
- direct/off-heap buffer control: look at `Agrona`
- repeated compilation of the same generated shapes: use `Caffeine` or an equivalent bounded cache around Janino/JDK compilation
- serious binary wire efficiency: compare Protobuf with FlatBuffers or SBE
- generated Java source getting too large or too fragile: compare Janino with `JavaCompiler` or ASM rather than forcing one tool to fit every case

## Library red flags

- Adding a library before a JDK baseline exists
- Replacing a simple array algorithm with a complex dependency
- Using a concurrency library without matching the actual producer/consumer pattern
- Choosing off-heap because it sounds faster, not because GC or sharing semantics require it
- Adopting a serialization stack without accounting for ecosystem, tooling, and evolution constraints
- Introducing Janino without an explicit cache/fallback/classloader strategy
- Using runtime codegen to paper over a bad algorithm or wrong execution model
