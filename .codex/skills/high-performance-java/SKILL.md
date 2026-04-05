---
name: high-performance-java
description: Use when writing, reviewing, or reshaping HotSpot Java where throughput, latency, allocation rate, zero-copy, lazy evaluation, non-materialization, intrinsics, SuperWord auto-vectorization, or C2 assembly matter. Bias toward specialized hot-path code, then ground claims in benchmarks and JIT evidence.
---

# High-Performance Java

Use this skill for Java hot paths. Default bias: fewer allocations, fewer copies, less polymorphism, narrower code shape, stronger evidence.

HotSpot-only v1. Baseline assumptions:
- repo baseline: JDK 21
- current local runtime may be newer
- low-level claims stay provisional until benchmark + JIT evidence agree

## Core loop

1. Identify the workload shape.
2. Find the hot loop or hot call chain.
3. Write the narrow fast path first.
4. Push generic abstraction, materialization, and dispatch out of the loop.
5. Benchmark before claiming improvement.
6. Inspect HotSpot decisions before claiming JVM-level reasons.

## Default coding bias

- Prefer zero-copy over copy-transform-copy.
- Prefer reuse over per-item allocation.
- Prefer lazy traversal over full materialization.
- Prefer primitives, flat arrays, and tight counted loops in hot paths.
- Prefer monomorphic calls that inline away.
- Prefer specialized lambda/adaptor code for the active workload.
- Prefer one fast path plus one cold fallback over a single generalized hot path.

## Hard rules

- Do not defend a perf change with style arguments alone.
- Do not claim “faster” without a measurement path.
- Do not claim “JIT will optimize this” without checking inlining / compilation evidence.
- Do not keep elegant-but-generic stream pipelines in verified hot loops.
- Do not pay interface / visitor / wrapper overhead inside the hottest loop unless evidence shows it disappears.

## Design checklist

Ask these first:
- What allocates on the steady-state path?
- What copies bytes, chars, arrays, or collections?
- What materializes intermediate state that could stay streamed or cursor-based?
- What dispatch stays virtual or megamorphic in the inner loop?
- What loop shape blocks scalar replacement, inlining, or SuperWord vectorization?
- What “generic” branch handles cases the active workload never uses?

## Workflow

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

### 4) Report honestly

Frame conclusions as:
- hypothesis
- benchmark result
- JIT/profile evidence
- confidence

If assembly is unavailable, say so and fall back to compilation logs, inlining diagnostics, and profile data.

## Trigger examples

Use this skill when the user asks to:
- remove allocation pressure from a parser, iterator, encoder, decoder, or query loop
- make a Java path zero-copy or lazy
- specialize code for one workload instead of many
- explain whether a HotSpot optimization actually happened
- ground a Java perf change in benchmark + C2 evidence

## Reference map

- Coding rules: [references/coding-rules.md](references/coding-rules.md)
- Evidence workflow: [references/evidence-workflow.md](references/evidence-workflow.md)
- JDK version guardrails: [references/jdk-21-26-notes.md](references/jdk-21-26-notes.md)

## Output contract

When you use this skill, the answer should usually include:
- hot-path hypothesis
- concrete code-shape recommendation
- benchmark command or benchmark evidence
- JIT/profile evidence or the missing prerequisite
- a confidence statement tied to the active JDK
