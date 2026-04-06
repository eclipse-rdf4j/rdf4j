---
name: high-performance-java
description: Use when writing, reviewing, or reshaping HotSpot Java where algorithmic complexity, data-structure choice, throughput, latency, allocation rate, zero-copy, lazy evaluation, non-materialization, primitive collections, performance libraries, intrinsics, SuperWord auto-vectorization, or C2 assembly matter. Also use for advanced algorithmic problem solving in Java, including dynamic programming, graph/range techniques, and cache-aware code shape. Bias toward asymptotic wins first, then specialized hot-path code, then benchmark and JIT evidence.
---

# High-Performance Java

Use this skill for Java hot paths and algorithm-heavy Java. Default bias: asymptotic win first, then fewer allocations, fewer copies, less polymorphism, narrower code shape, stronger evidence.

HotSpot-only v1. Baseline assumptions:
- repo baseline: JDK 21
- current local runtime may be newer
- low-level claims stay provisional until benchmark + JIT evidence agree
- algorithm/data-structure claims stay provisional until they match the actual workload constraints

## Core loop

1. Identify the workload shape and constraints.
2. Pick the algorithm and data structure that change the slope.
3. Find the hot loop or hot call chain.
4. Write the narrow fast path first.
5. Push generic abstraction, materialization, and dispatch out of the loop.
6. Benchmark before claiming improvement.
7. Inspect HotSpot decisions before claiming JVM-level reasons.

## Default coding bias

- Prefer an algorithmic win over a micro win.
- Prefer data structures that fit the operation mix, memory budget, and key domain.
- Prefer primitive-friendly layouts before boxed object graphs.
- Prefer zero-copy over copy-transform-copy.
- Prefer reuse over per-item allocation.
- Prefer lazy traversal over full materialization.
- Prefer primitives, flat arrays, and tight counted loops in hot paths.
- Prefer monomorphic calls that inline away.
- Prefer specialized lambda/adaptor code for the active workload.
- Prefer one fast path plus one cold fallback over a single generalized hot path.

## Hard rules

- Do not micro-optimize a fundamentally wrong algorithm.
- Do not defend a perf change with style arguments alone.
- Do not claim “faster” without a measurement path.
- Do not claim “JIT will optimize this” without checking inlining / compilation evidence.
- Do not add a specialized library until you know what property it buys: fewer allocations, fewer copies, lower contention, off-heap layout, better primitive support, or a stronger algorithm.
- Do not keep elegant-but-generic stream pipelines in verified hot loops.
- Do not pay interface / visitor / wrapper overhead inside the hottest loop unless evidence shows it disappears.
- Do not default to boxed `Map<K, V>` / `Set<T>` / `List<T>` shapes when primitive collections or flat arrays better fit the dominant path.

## Design checklist

Ask these first:
- What are `N`, `Q`, the update/query ratio, and the memory budget?
- Is the main problem asymptotic complexity, cache locality, allocation pressure, branchiness, contention, or I/O?
- What operation dominates: membership, counting, top-k, range query, join, shortest path, DP transition, parsing, encoding?
- Can the key/value/state space stay primitive or bit-packed?
- Can the workload become offline, batched, sorted, prefix-based, or compressed?
- What allocates on the steady-state path?
- What copies bytes, chars, arrays, or collections?
- What materializes intermediate state that could stay streamed or cursor-based?
- What dispatch stays virtual or megamorphic in the inner loop?
- What loop shape blocks scalar replacement, inlining, or SuperWord vectorization?
- What “generic” branch handles cases the active workload never uses?

## Workflow

### 0) Pick the algorithmic shape

- Estimate the real workload: input size, query count, mutation pattern, latency target, and memory ceiling.
- Choose the algorithm and data structure before tuning loop syntax.
- Favor contiguous, cache-friendly, primitive-heavy representations when semantics allow.
- For dynamic programming, define state, transition cost, base case, iteration order, and whether state compression is possible.
- For graph/range/string problems, look for offline transforms, prefix structures, monotonic structures, or specialized search before hand-tuning.

Read these only when relevant:
- [references/algorithms-data-structures.md](references/algorithms-data-structures.md) for algorithm and data-structure selection.
- [references/advanced-coding-techniques.md](references/advanced-coding-techniques.md) for dynamic programming and advanced problem-solving patterns.

### 1) Shape the code for HotSpot

- Split hot and cold paths.
- Hoist invariant checks and decoding outside the loop.
- Replace generic callback stacks with narrow-path adapters.
- Reuse mutable carriers only when ownership is clear.
- Keep loop bodies predictable, contiguous, and exception-light.

Detailed rules: see [references/coding-rules.md](references/coding-rules.md).

### 2) Measure

If you are in this RDF4J repo, use the local benchmark wrapper first:

```bash
scripts/run-single-benchmark.sh --module <module> --class <fqcn> --method <benchmarkMethod>
```

If you are outside RDF4J, use JMH or an existing reproducible micro/macro benchmark.

Measurement workflow: see [references/evidence-workflow.md](references/evidence-workflow.md).

### 3) Explain with JVM evidence

When a benchmark moves, inspect what HotSpot actually did:
- compilation tier
- inlining success/failure
- intrinsic usage when relevant
- allocation pressure
- assembly / C2 logs when needed

Use sibling skill [hotspot-jit-forensics](../hotspot-jit-forensics/SKILL.md) for method-scoped C2 evidence. Use `async-profiler-java-macos` when wall/cpu/alloc evidence is needed on macOS.

### 4) Use libraries intentionally

- Prefer the JDK first when it is close enough and operationally simpler.
- Reach for specialized libraries when they remove boxing, copies, parser overhead, contention, or off-heap indirection the JDK cannot.
- Check dependency health before adding a new library.
- Benchmark the library choice against the simplest credible in-repo baseline.

Library reference: [references/high-performance-java-libraries.md](references/high-performance-java-libraries.md).

### 5) Report honestly

Frame conclusions as:
- hypothesis
- algorithm/data-structure choice
- benchmark result
- JIT/profile evidence
- confidence

If assembly is unavailable, say so and fall back to compilation logs, inlining diagnostics, and profile data.

## Trigger examples

Use this skill when the user asks to:
- remove allocation pressure from a parser, iterator, encoder, decoder, or query loop
- make a Java path zero-copy or lazy
- choose the right data structure for a Java workload
- solve a dynamic programming, graph, interval, ranking, or range-query problem in Java under performance constraints
- replace boxed collections with primitive or cache-friendly structures
- choose between the JDK and specialized Java performance libraries
- specialize code for one workload instead of many
- explain whether a HotSpot optimization actually happened
- ground a Java perf change in benchmark + C2 evidence

## Reference map

- Algorithms and data structures: [references/algorithms-data-structures.md](references/algorithms-data-structures.md)
- Advanced coding techniques: [references/advanced-coding-techniques.md](references/advanced-coding-techniques.md)
- High-performance Java libraries: [references/high-performance-java-libraries.md](references/high-performance-java-libraries.md)
- Coding rules: [references/coding-rules.md](references/coding-rules.md)
- Evidence workflow: [references/evidence-workflow.md](references/evidence-workflow.md)
- JDK version guardrails: [references/jdk-21-26-notes.md](references/jdk-21-26-notes.md)

## Output contract

When you use this skill, the answer should usually include:
- workload model and asymptotic bottleneck
- algorithm and data-structure recommendation
- hot-path hypothesis
- concrete code-shape recommendation
- library recommendation when a library meaningfully changes the design
- benchmark command or benchmark evidence
- JIT/profile evidence or the missing prerequisite
- a confidence statement tied to the active JDK
