# Coding Rules

Use these rules only for real or suspected hot paths. Outside hot code, keep the code simple.

## Zero-copy rules

- Pass slices, offsets, lengths, cursors, or views instead of copying into new arrays/strings/collections.
- Decode or parse directly from the source buffer when ownership and lifetime allow it.
- Delay conversion to `String`, boxed numbers, or collection objects until a boundary that actually needs them.
- Prefer bulk operations that map to JDK intrinsics when they replace manual copy/compare loops.

## Reuse rules

- Reuse mutable carriers, builders, encoders, decoders, and scratch arrays when one owner controls lifetime.
- Reinitialize reusable state cheaply; do not reconstruct deep object graphs inside the loop.
- Avoid thread-local caches unless the access pattern is proven hot and safe.
- Do not reuse objects across boundaries where aliasing or stale-state bugs become likely.

## Lazy and non-materializing rules

- Stream results directly to the consumer when the consumer can handle incremental delivery.
- Prefer iterators/cursors/sinks over `collect then filter/map`.
- Keep intermediate state as indices, spans, or primitive accumulators instead of wrapper objects.
- Materialize once at a boundary; not at each transformation stage.

## Dispatch and inlining rules

- Prefer `static`, `private`, or effectively final call targets on the inner path.
- Keep call sites monomorphic when possible; push interface selection above the hot loop.
- Split fast path from generic path when one workload dominates.
- Flatten tiny wrapper/helper layers when they prevent clear inlining.
- Treat interface-heavy visitor chains and generic function stacks as suspects until proven free by evidence.

## Intrinsic and vectorization rules

- Prefer primitive arrays and contiguous memory access.
- Write simple counted loops with hoisted bounds and invariant checks.
- Avoid hidden aliasing, side exits, and exception-heavy bodies in vectorizable loops.
- Prefer JDK library methods that HotSpot commonly treats specially over open-coded copies/comparisons/hashes when semantics match.
- Verify vectorization and intrinsic assumptions on the active JDK; do not assume cross-version stability.

## Lambda specialization rules

- Generate or choose workload-specific lambdas/adapters when the hot path only needs one shape.
- Prebind constants and remove unused branches from the inner callback.
- Avoid polymorphic chains of `Function` / `Predicate` / `Consumer` in hot loops when a direct method or specialized adapter will do.
- Prefer one specialized lambda per workload over one generalized lambda with internal branching.

## Anti-patterns

| Anti-pattern | Hot-path cost | Prefer |
| --- | --- | --- |
| Streams in verified hot loops | allocation, boxing, dispatch | direct counted loop |
| Generic visitor/callback towers | missed inline, megamorphism | split fast path + cold fallback |
| Temporary wrappers per item | allocation pressure | primitive fields or reusable carrier |
| Defensive copies on steady-state path | bandwidth + GC | views/slices/ownership checks |
| Materialize then filter/map | memory + latency | lazy cursor/sink pipeline |
| Repeated decode/encode boundary crossings | redundant work | keep native form longer |
| One abstraction for all workloads | branchy hot path | specialized narrow path |

## Decision rule

If a change makes the code uglier but removes copies, allocations, or polymorphism from a measured hot path, it can be worth it.

If the path is not hot, do not apply these rules aggressively.
