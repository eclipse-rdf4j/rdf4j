# Algorithms and Data Structures

Use this reference when the main question is algorithmic shape, data-structure choice, or whether a complexity change dominates any JVM-level tuning. Biggest wins usually come from changing the slope before shaving cycles.

## Triage first

Before choosing a structure, answer these:
- Is the workload one-shot, batched, or online?
- Do you need insertion order, sorted order, or just membership?
- Are keys dense integers, sparse integers, strings, tuples, or custom objects?
- Are queries point lookups, range queries, top-k queries, path queries, or aggregate queries?
- Is the structure static after build, append-only, or heavily mutable?
- Can the state stay primitive, bit-packed, or index-based?

## Default data-structure bias

- `int[]`, `long[]`, `byte[]`: best starting point when size is known or can grow geometrically.
- `ArrayList`: good general dynamic array when boxing is acceptable and traversal dominates.
- `ArrayDeque`: default queue/stack/deque. Better cache shape than `LinkedList`.
- `HashMap` / `HashSet`: baseline for sparse membership and counting when boxing cost is acceptable.
- `TreeMap` / `TreeSet`: only when ordered updates and queries are intrinsic. Do not pay `O(log n)` if sort-once plus scan works.
- `BitSet`: excellent for dense integer domains, set algebra, visited flags, and some DP/state compression.

## Primitive-first guidance

If keys/values are primitive and the path is hot:
- Prefer flat arrays when bounds are manageable.
- Prefer primitive maps/sets/heaps over boxed collections when boxing dominates time or memory.
- Use coordinate compression when raw keys are large but the distinct key count is moderate.
- Represent relations as integer ids plus parallel arrays instead of object graphs when traversal dominates.

## Membership, dedup, counting

- Hash table: default for sparse exact membership and frequency counting.
- Sort plus scan: strong when the data is batch-oriented, read-mostly, or you also need grouping/order.
- BitSet / boolean array: best for dense bounded integer keys.
- Bloom filter: prefilter only. Use when false positives are acceptable but false negatives are not.

Red flags:
- Nested membership scans over lists.
- Repeated `contains` on `ArrayList` in hot code.
- Boxing primitive keys when the domain can be compressed.

## Top-k, ranking, scheduling

- Binary heap / priority queue: streaming top-k, best-first search, event scheduling.
- Quickselect: one-shot kth element or top-k partition when full sort is wasteful.
- Bucket/counting approach: when values live in a small bounded domain.
- Monotonic deque: sliding-window min/max in linear time.

Java notes:
- JDK `PriorityQueue` is fine for many cases but boxes primitives.
- For tiny fixed `k`, a sorted small array can beat a heap.

## Prefix, range, and interval workloads

- Prefix sum: immutable range-sum/count queries.
- Difference array: batched range updates with one final sweep.
- Fenwick tree: point updates plus prefix/range aggregates in `O(log n)` with low constants.
- Segment tree: more flexible range updates/queries, but heavier than Fenwick.
- Sparse table: immutable idempotent range queries such as min/max/gcd.
- Sweep line: interval overlap, event merging, skyline, booking, and geometry-style event problems.

Decision rule:
- Static data: prefer prefix sums or sparse tables.
- Dynamic point updates: Fenwick first.
- Complex dynamic range operations: segment tree only when simpler structures fail.

## Graph workloads

- BFS: unweighted shortest path, level order, flood fill.
- 0-1 BFS: edge weights only `0` or `1`.
- Dijkstra: non-negative weighted shortest path.
- Topological sort plus DP: DAG path/count problems.
- Union-find (disjoint set union): connectivity under merges, Kruskal, component grouping.
- Tarjan/Kosaraju: strongly connected components.

Java notes:
- Prefer adjacency as primitive arrays or compact edge lists when the graph is large.
- Avoid per-edge objects on hot traversals.
- Beware recursion depth on DFS; iterative stacks are often safer.

## String and sequence workloads

- Sliding window / two pointers: substring/segment constraints with monotonic boundaries.
- KMP or Z-function: repeated pattern matching in linear time.
- Rolling hash: fast substring comparisons with collision caveat.
- Trie: prefix queries and dictionary walks when the alphabet is manageable.
- Aho-Corasick: multiple pattern matching.
- Patience sorting / tails array: `O(n log n)` LIS.

Java notes:
- Avoid repeated substring materialization in tight loops.
- Work on `byte[]`, `char[]`, offsets, or integer ids when possible.

## Ordered search and offline transforms

- Sort plus binary search: often simpler and faster than maintaining ordered trees.
- Coordinate compression: map large sparse keys into `[0..m)` for arrays, Fenwick trees, and bitsets.
- Offline queries: sort events/queries once, then answer in a sweep.
- Meet-in-the-middle: split exponential search into two half-enumerations.

## Data-structure atlas

### Arrays and flat buffers

Use when:
- Traversal dominates.
- Keys can be mapped to integer indexes.
- You need maximum locality and minimum allocation.

Avoid when:
- Sparse domains would explode memory.
- Mutation semantics need expensive shifting and you cannot batch.

### Hash tables

Use when:
- Exact membership/counting dominates.
- Order is irrelevant or can be restored later.

Avoid when:
- Dense bounded keys fit a bitset or direct array.
- You only need one batch query and sort plus scan is cheaper.

### Heaps

Use when:
- You need repeated access to min/max with incremental updates.
- Best-first exploration or top-k streaming dominates.

Avoid when:
- You only need one final order; sort once instead.
- `k` is tiny and a fixed small array is cheaper.

### Bitsets

Use when:
- The domain is dense or compressible.
- Boolean DP, visited state, set algebra, or fast intersections matter.

Avoid when:
- The domain is too sparse after compression.

### Fenwick and segment trees

Use when:
- Simple arrays are too static.
- Query/update interleaving matters.

Avoid when:
- Prefix sums or difference arrays solve the same problem.
- The workload is too small to justify structural overhead.

### Union-find

Use when:
- The workload is merge-only connectivity.
- You need amortized near-constant component unions/finds.

Avoid when:
- You need deletions or rich path queries.

## Algorithmic red flags

- `O(n^2)` nested scans hidden inside "simple" collection code.
- Re-sorting on every query.
- `LinkedList` for queue/stack workloads.
- Object-per-node or object-per-edge layouts in large graphs or DP tables.
- Recomputing prefix information instead of caching it.
- Dense DP stored as `Map<State, Value>` when a flat array works.
- Maintaining balanced trees when sort-once plus array search is enough.

## Escalation rule

If you can change:
- `O(n^2)` to `O(n log n)` or `O(n)`,
- boxed/object-heavy state to primitive/flat state,
- online mutable work to offline batched work,

do that before micro-tuning loop syntax or arguing about JIT trivia.
