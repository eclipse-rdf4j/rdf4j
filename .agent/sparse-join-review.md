# Independent review: sparse join aggregation

Reviewed 2026-11-04. This note records source inspection and mathematical reasoning, not benchmark results or a substitute for the executing agent's test reports. The reviewer made no production changes and ran no builds.

## Scope and integration contract

The implementation comprises `SparseJoinAggregateKernel`, `LmdbSparseJoinAggregatePlan`, and the new `NativeGroupIteration` candidate. It computes

\[
C(x,y)=\sum_z L(x,z)R(z,y)
\]

for two mandatory triple patterns sharing exactly one subject/object variable, grouped by their other two endpoints. Weights are positive integer solution multiplicities. Each nonzero group is emitted once; a rectangular self-join retains both endpoint orders and diagonal groups.

Admission is deliberately narrow: two plain patterns in a `JoinPlan`, or two pattern children in a `MultiJoinPlan` with no residual filters; fixed predicates; distinct, unbound subject/object slots; no graph-variable slot, repeated slot, range, optional input, or additional join dimension. Every aggregate must be non-distinct `COUNT(*)`, `COUNT` of one of the three necessarily bound pattern variables, or `COUNT` of a known non-null constant. Arbitrary expressions and possibly unbound arguments retain the existing evaluators. This preserves SPARQL error/unbound behavior by declining shapes without a proof.

The integration checks the canonical-ID invariant on the backing store after unwrapping `SyntheticValueSource`. The wrapper itself intentionally does not promise canonical IDs. Legacy sources with multiple IDs for one RDF term must decline: raw-ID joins and grouping cannot repair that equivalence by themselves.

Fixed graph and dataset constraints are taken from each `PatternPlan`. Named-only scope excludes the physical default context. Context filtering occurs before adjacent incidences are combined into weights, and a fully rejected batch is skipped rather than mistaken for end-of-row. The adapter currently reads bounded raw neighbor/context batches; it does not assume that unfiltered adjacency multiplicities remain valid after a graph restriction. A graph variable shared between the patterns would introduce another join condition and is excluded.

## Correctness of accumulation and pruning

### Exact accumulation

The right plane is remapped to compact shared-row and endpoint ordinals. For each left endpoint, the kernel walks matching right runs and adds the product of positive weights directly into an endpoint counter. A touched-index list records the first positive contribution, permits nonzero-only emission, and resets only visited counters between left rows. Checked multiplication/addition prevents wraparound; zero therefore remains a valid untouched marker. IDs use all 64 bits, while ordering checks use unsigned comparison.

Repeated right fibers are combined across input-batch boundaries. The bounded-HAVING path also combines repeated left/shared fibers before calculating the left maximum. Empty right rows are ignored before this left combination, so an irrelevant weight cannot overflow while contributing to no group.

### Row bound

Let \(M_R(z)=\max_y R(z,y)\), with zero for an empty or absent right row. Since weights are nonnegative,

\[
C(x,y)\le U_x=\sum_z L(x,z)M_R(z)
\]

for every endpoint \(y\). An entire left row can be rejected when \(U_x<\tau\), where \(\tau\) is an inclusive minimum count. Accumulating this sum from repeated left input fibers is also valid by distributivity. Saturation at `Long.MAX_VALUE` is safe for this bound: every supported threshold is at most that value, so an overflowed upper bound cannot cause a false rejection.

### Column bound

Let \(A_x=\max_z L(x,z)\) after combining repeated left/shared fibers, and \(S_y=\sum_zR(z,y)\). Then

\[
C(x,y)\le A_xS_y.
\]

For each observed right endpoint, setup computes `requiredLeftWeight[y] = 1 + (minimumCount - 1) / S_y`. The product can be skipped whenever `A_x` is smaller than this requirement. This is exactly the integer form of \(A_xS_y<\tau\), without a potentially overflowing multiplication in the hot loop. Each column has a positive sum because only observed endpoints receive ordinals.

Column sums also saturate safely. If the true sum exceeds `Long.MAX_VALUE`, every representable positive threshold yields a required left weight of one. Every relevant positive left weight passes, so this weaker bound never rejects a valid result. The left maximum must use combined fiber weights; the maximum of separate duplicate entries is not a valid substitute.

The original full HAVING expression remains authoritative. Threshold extraction reuses the existing recognizer for exact integer comparisons, including reversed comparisons and a necessary conjunct of `AND`. Equality supplies only a lower-bound prefilter. Decimal/floating constants, `OR`, and unsupported expressions supply no extra pruning. `COUNT > Long.MAX_VALUE` uses a conservative `Long.MAX_VALUE` prefilter; the retained outer filter rejects its boundary value. Exact counters are never saturated.

## Work and memory

Let \(I_R,I_L\) be raw incidences read, \(E_R\) the compact right fibers, \(K_R\) right shared rows, \(D_R\) distinct right endpoints, \(d_L\) the largest buffered left degree, and \(Z\) emitted groups. Define \(P\) as the number of right-fiber visits induced by the processed left fibers.

- Expected work is \(O(I_R+I_L+K_R+D_R+E_R+P+Z)\), assuming the existing primitive hash maps' expected constant-time operations. Bounds can skip whole rows or avoid counter work on columns; column rejection still visits the candidate adjacency entries.
- Kernel state is \(O(K_R+D_R+E_R+d_L)\). There is no dense endpoint-pair matrix. State includes ordinal maps, right offsets/columns, optional right weights, endpoint counters/touched indices, conditional bound arrays, and a reusable left-row buffer for thresholds above one.
- Right weights are omitted when every compact fiber has unit weight. Bound arrays are omitted when the minimum count is at most one. Arrays grow geometrically, so capacity and transient copy peaks exceed the logical element count.
- Reservations precede array growth and account for old and replacement arrays together. Input adapter context buffers and kernel state share the query ledger. Output uses the existing bounded replay mechanism and can spill; a large answer is still intrinsically expensive.
- At thresholds zero/one, repeated left fibers crossing adapter batches can cause repeated products; the bounded path combines those fibers. On such inputs, reduced `multiplyAdds` can reflect both coalescing and pruning. Use already compact input or disclose this distinction when interpreting a threshold sweep.

The column bound adds one sum update per right input fiber, one division per distinct right endpoint, an additional bound array, and a bound load/comparison per candidate product. These costs must be included in measured setup and total time. No performance advantage is claimed here.

## Ownership, fallback, and cancellation

The kernel closes its maps before its workspace and leaves caller-owned inputs/account open. Partial output stays private until the whole attempt succeeds. The integration now closes cursors, adjacency views, probe, and temporary accounting before transferring the completed replay to the group owner.

Explicit cleanup uses `KernelRuntime.closeResource`, preserving real cleanup failures even when the primary probe-cancellation control has suppression disabled. Allocation refusal is a clean decline only when no real cleanup failure is attached. Decline does not mutate the shared binding row. Automatic arbitration can choose another existing candidate; an explicitly forced sparse strategy that declines follows the existing forced-strategy-unavailable error contract. Query cancellation remains terminal, while probe cancellation remains a speculative-attempt outcome. Input batches, inner multiplication, bound loops, copying, and output have polling sites; the output sink also honors the probe materialization limit.

## Review findings addressed

- Corrected the initial multiply-add expectation from eight to six.
- Combined repeated left fibers before applying the column bound, including a batch-boundary case.
- Skipped empty right rows before left-weight combination, preventing irrelevant overflow.
- Added a representable-result case with an overflowing global right column sum.
- Strengthened the large-multiplication cancellation test to require zero sink emissions.
- Corrected the test-file copyright URL to the repository template.
- Moved result transfer after successful producer closure and preserved cleanup failures during allocation/probe cancellation.
- Added the ordered-adjacency capability check before accepting a cursor.

The reviewed tests cover all four endpoint orientations, weighted graph incidences, fixed graphs and `FROM` datasets, group-slot order, bound COUNT channels and expression fallback, full-width IDs, overflow, both self-join orders and diagonal, randomized bag parity, memory refusal, cancellation, and conservative rejection of unsupported SPARQL shapes. Public automatic-fallback parity alone does not prove that an unpriced candidate was rejected; direct recognizer assertions provide the admission checks. The final executed suite includes integration-level refusal after partial replay construction, successful replay ownership, and suppression-disabled cancellation with a real cleanup failure. All 153 selected tests pass.

## Remaining limitations

The route is opt-in. It has no fitted cost model, AMX path, native C-interface call, parallel sparse multiply, adaptive orientation, or spillable right matrix. It remaps the entire right plane on each invocation even when few left keys match. Low fanout, dense output, weak HAVING bounds, and setup-dominated workloads can therefore favor the existing factorized aggregate. Its exact counts are limited to `Long.MAX_VALUE`, matching the existing factorized arithmetic policy; overflow is an error. The supported SPARQL fragment is intentionally narrower than arbitrary joins/aggregates.

Benchmark reports should separate raw adjacency consumption, remapping/bounds/setup, multiplication, output handling, and end-to-end execution where available; record cache/warmup policy and compare the real factorized route. Source inspection is not evidence that a benchmark path was selected or that any speedup exists.

## Primary sources

- [Tessera paper, version 1](https://arxiv.org/html/2610.40620v1), especially sparse multiplication, aggregate bounds, engine integration, and methodology sections. [Versioned PDF](https://arxiv.org/pdf/2610.40620v1).
- [Pinned Tessera repository](https://github.com/GyuyeongKim/Tessera-public/tree/b6f6f8d3064c003f742c357ae6272667c7841334).
- [Pinned sparse core](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/tessera.cpp): direct counters, touched positions, weighted cross-relation multiplication, and exact count paths.
- [Pinned query setup](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/query.cpp): dictionaries/ranks/CSR preparation, aggregate-bound preprocessing, and separately reported setup phases.
- [Pinned C interface](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/tessera_c.h) and [reproduction instructions](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/README.md).

This Java adaptation uses the direct-counter sparse multiplication idea. Its per-shared-row maximum bound and per-left-row maximum/column-sum bound are justified above; they should not be presented as a literal port of every Tessera preprocessing or pruning decision. Tessera's reported engine measurements are not RDF4J performance evidence.
