# Kernel IR: all composable whole-stage codegen primitives for the LMDB native engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root).

## Purpose / Big Picture

The LMDB native engine can already compile two fixed query shapes into runtime-generated Java ("Janino kernels"): a probe-chain pipeline and one VALUES-seeded grouped COUNT(DISTINCT) aggregate. Each shape has its own ad-hoc recognizer and its own hand-rolled string emitter, so every new SPARQL construct (OPTIONAL, UNION, EXISTS, general aggregates, DISTINCT, ORDER BY, property paths) would need another bespoke emitter — which does not scale to the full corpus of query shapes, let alone SPARQL 1.2.

This plan replaces the "one emitter per shape" approach with a small **kernel intermediate representation** (kernel IR): a tree of about fourteen composable primitive operations (enumerate keys, probe an adjacency, intersect adjacencies, filter by id, filter by value through an engine callback, optional probe, existence check, union, bind, aggregate, deduplicate, order/limit, transitive path expansion, emit). Any tree built from these primitives can be turned into one generated Java class implementing the existing `JaninoKernel` contract by a single generic emitter. After this change, adding kernel support for a new SPARQL construct means *lowering it to existing IR nodes*, not writing a new code generator.

Observable outcome: a test class constructs IR trees for every primitive (and several compositions such as union-over-optional-into-grouped-aggregate, a two-stage subquery, and a breadth-first `+`-path), compiles them through the real Janino service, runs them against in-memory adjacency fixtures, and asserts exact expected results. All of this is additive: no existing execution path changes behavior, and the two existing kernel families keep working unchanged.

## Progress

- [x] (2026-07-24) Corpus analysis and primitive inventory recorded in `plans/lmdb-native-engine/18-janino-pattern-analysis.md`.
- [x] (2026-07-24) ExecPlan authored (this file).
- [x] (2026-07-24 ~07:00Z) M1: `KernelHooks` + `KernelRuntime` (LongHashSet, LongIntMap, RowSet, sortRows, topKRows) + `KernelContext.hooks` compat constructor; `KernelRuntimeTest` 8/8 green (0.034s).
- [x] (2026-07-24 ~07:10Z) M2+M3: IR node model (`LmdbNativeKernelIr`: Operand, 14 primitive node kinds + Emit/Aggregate terminals with OutputMods, Requirements, Kernel with shapeKey/className/validation) and generic emitter (`LmdbNativeKernelEmitter`: fields-as-columns, one-method-per-node, boolean-mode sub-pipelines for exists, continuation methods for union/left-probe).
- [x] (2026-07-24 ~07:14Z) M4: `LmdbNativeKernelIrEmitterTest` 23/23 green (0.615s) — every primitive plus compositions: union→leftprobe→exists→grouped COUNT(DISTINCT) with HAVING (corpus skeleton-6 shape); subquery-as-domain two-kernel composition; exists-over-union-with-pathexpand; dedup+orderBy+offset+limit stack; value-order-via-hooks vs unsigned-id order.
- [x] (2026-07-24 ~07:20Z) M5 partial: copyright check green, formatter green (only intended files touched).
- [x] (2026-07-24 ~07:45Z) M5 complete: full `core/sail/lmdb` sweep = 2159 tests, 4 failures / 0 errors, confined to 2 classes (`LmdbNativeFeatureFlagForkTest` 3F, `LmdbNativeLeftJoinFilterRewriteTest` 1F) that are independent of this change — proof: the only tracked-file diff is `KernelContext` (+7 inert lines: field + delegating constructor), no production source references any new class (`rg` over `src/main` returns only the new files themselves), and neither failing class references `KernelContext`; `LmdbNativeFeatureFlagForkTest` was modified today by concurrent branch work in another session. Both new test classes green inside the sweep (`KernelRuntimeTest` 8/8, `LmdbNativeKernelIrEmitterTest` 23/23).

## Surprises & Discoveries

Pre-registered risks to watch: Janino language-subset gaps on generated source (keep to assignments, `for`, `if`, flags instead of labeled `continue`, method calls, primitive locals); overload resolution quirks around `null` arguments in generated call sites (always pass fields, never literal null); code-size growth from Union/LeftProbe continuations (mitigated by the one-method-per-node design).

- Observation: the engine's unbound sentinel is `LmdbValue.UNKNOWN_ID = -1`, not 0 (checked `LmdbNativeRowState.bind`, which trails only UNKNOWN→bound transitions). The IR's null convention follows it: `LmdbNativeKernelIr.NULL_ID = -1L`. `LongHashSet`/`LongIntMap` in `KernelRuntime` therefore must support -1 and 0 as ordinary keys (slot value 0 marks empty with the real key 0 tracked by a side flag).
  Evidence: `LmdbValue.UNKNOWN_ID = -1` (`core/sail/lmdb/.../model/LmdbValue.java:20`); `LmdbNativeRowState.bind` trails only `UNKNOWN -> bound` transitions.
- Observation: no Janino subset problems surfaced at all — all 23 generated kernel shapes (including the k-way leapfrog intersection with local arrays, the BFS path expansion with `continue` in a `while`, and boolean-mode exists methods) compiled on the first attempt. The pre-registered risks (overload resolution around null, labeled-continue) were avoided by construction: the emitter always passes typed casts (`(KernelHooks) null`, `(boolean[]) null`) and uses flags instead of labels.
  Evidence: `LmdbNativeKernelIrEmitterTest` 23/23, zero COMPILE_FAILURES; failure mode would have been `assertNotNull(kernel, "kernel did not compile...")` with the dumped source.
- Observation: the one real emitter bug caught during implementation was long-vs-int literal emission for LIMIT/OFFSET (a large limit like `Long.MAX_VALUE` would have emitted an invalid int literal into `outCount = <limit>;`); fixed by clamping to `Integer.MAX_VALUE` at emission time, which is sound because `outCount` is an int.
  Evidence: `LmdbNativeKernelEmitter.emitFlush` clamps via `(int) Math.min(mods.limit + mods.offset, Integer.MAX_VALUE)`.

## Decision Log

- Decision: Build the IR + emitter + tests first, with lowering from real SPARQL plans deferred to follow-up plans (one lowering step per construct, each adopting the corresponding primitives).
  Rationale: The primitives and their code generator are the enabling substrate and can be verified exhaustively in isolation against synthetic adjacency data; lowering is per-construct recognition work with its own correctness burden (well-designedness of OPTIONAL, MINUS-vs-NOT-EXISTS semantics) that plan 18 sequences by corpus leverage. Bundling both would make this change enormous and unreviewable.
  Date/Author: 2026-07-24 / Claude (Fable) with hmottestad.
- Decision: Generated kernels hold result columns in instance fields (`long v0..vk`), and container primitives (Union, LeftProbe, Exists) emit their continuations as separate no-argument private methods.
  Rationale: Union and LeftProbe both need to run "the rest of the pipeline" from more than one program point (per branch; match-loop and null-arm). With columns in fields, a continuation is a no-arg method and code size stays linear in tree size; with locals, continuations would need every live column as a parameter and inline duplication would double code size per Union. The straight-chain register-allocation win of locals is real but is an optimization for a later plan (emit locals when the tree is a pure chain); correctness and composability come first. Recorded as an explicit perf follow-up.
  Date/Author: 2026-07-24 / Claude.
- Decision: Value-tier operations (general FILTER expressions, computed BIND, ORDER BY value comparison, numeric decode for SUM/AVG/MIN/MAX) go through a new public callback interface `KernelHooks` rather than being emitted inline.
  Rationale: This makes every primitive *complete* today with engine-exact semantics (the engine-side hook implementation can evaluate any SPARQL expression), while leaving the door open for an inline "tier value" emitter later. It mirrors the plan-17 partial-fusion philosophy: the kernel keeps loop control, the hook supplies semantics the kernel cannot prove.
  Date/Author: 2026-07-24 / Claude.
- Decision: Shared data structures used by generated code (hash set, group map, packed-row set, row sorting, top-k) live as ordinary reviewed Java in the public SPI class `KernelRuntime`, not as generated source.
  Rationale: Generated source stays small and conservative (Janino subset), the tricky code (open addressing, growth, sort) is written and unit-tested once, and every kernel shares one JIT-compiled copy instead of per-kernel duplicates.
  Date/Author: 2026-07-24 / Claude.
- Decision: `KernelContext` gains a fifth public field `hooks` via an additional constructor; the existing four-argument constructor delegates with `hooks = null`.
  Rationale: The two existing emitters and their call sites keep compiling unchanged; kernels that need no hooks pass none.
  Date/Author: 2026-07-24 / Claude.
- Decision: Aggregate DISTINCT per group uses one `KernelRuntime.LongHashSet` per group (array of sets), and multi-column group keys pack through `KernelRuntime.RowSet` group interning.
  Rationale: Matches the corpus (skeletons 2/6/8: COUNT(DISTINCT x) per group with group counts in the tens of thousands); per-group sets are the simplest structure whose cost is proportional to actual distinct pairs. A sorted-run transition-count optimization (for `runsNeighborOrdered` inputs) is noted as follow-up, mirroring the M4 aggregate kernel's trick.
  Date/Author: 2026-07-24 / Claude.
- Decision: The IR models fourteen primitive kinds, with fixed-length property paths deliberately absent (they lower to Probe chains) and `PathExpand` covering only `+`/`*` via breadth-first search with a visited set.
  Rationale: Keeping the IR minimal keeps the emitter total; anything expressible by composition must not be a node.
  Date/Author: 2026-07-24 / Claude.
- Decision: Routine D (ExecPlan) governs this work; tests are written per milestone and the module sweep gates completion, but no pre-change failing-test evidence is required per the house rules for Routine D.
  Rationale: This is a large additive feature; the CLAUDE.md decision quickstart routes complex features to ExecPlans.
  Date/Author: 2026-07-24 / Claude.

## Outcomes & Retrospective

(2026-07-24, completion.) All fourteen primitive kinds exist as IR nodes, one generic emitter turns any well-formed tree into a compiling, running `JaninoKernel`, and 23 compile-and-run tests cover every primitive plus four cross-primitive compositions (union→leftprobe→exists→grouped COUNT(DISTINCT) with HAVING — the corpus skeleton-6 shape; subquery-as-domain two-kernel composition; exists-over-union-with-`+`-path; dedup+orderBy+offset+limit, including value-order-via-hooks vs unsigned-id order). Runtime helpers are separately unit-tested (8 tests). Zero Janino-subset surprises: every generated shape compiled on the first attempt because the emitter was designed inside the safe subset from the start. The module sweep shows no failure attributable to this change. What this deliberately does NOT do: no production query executes through these primitives yet — the two existing shape-specific emitters remain the only wired kernel paths; lowering from `MultiJoinPlan`/`LmdbNativeGroupStep` to IR is the follow-up sequence in plan 18 §3.7 (exists/left-probe into the aggregate family first). Perf follow-up recorded: straight-chain kernels could re-specialize columns from fields back to locals; the fields+continuation-methods design was chosen for composability and is the reason Union/LeftProbe/Exists needed no special cases. Lessons: (a) method-per-node emission makes local-variable scoping trivial (each node's scratch lives in its own method); (b) putting the hard data structures in reviewed Java (`KernelRuntime`) instead of generated source removed the riskiest failure class outright; (c) synthetic `NativeAdjacency` fixtures made primitive semantics testable without a store, keeping the edit-run loop under a minute.

## Context and Orientation

Everything lives in `core/sail/lmdb` unless stated; paths are repository-relative. Branch: `optimize-lmdb`.

The **native engine** compiles SPARQL algebra into `SlotPlan` operator trees (`src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`). Dictionary-encoded values ("ids", 64-bit longs, unsigned comparison order, `-1` = unknown/unbound) flow through `RowState` slots. The **CSR adjacency views** (`NativeLmdbQuerySource.NativeAdjacency`, a public nested interface of the public interface `NativeLmdbQuerySource`) expose, per predicate and direction, sorted neighbor runs: `denseIdOf(key)` → dense ordinal or -1, `runStart/runEnd(dense)` → half-open range, `neighborAt(i)`, `contextAt(i)`, optional `keyCount()/keyAt(dense)` for key enumeration, and `runsNeighborOrdered()`.

The **Janino kernel tier** (plan `plans/lmdb-native-engine/17-janino-whole-stage-codegen.md`, milestones M0–M4 complete):

- `evaluation/codegen/JaninoKernel.java` — public SPI every generated class implements: `bind(KernelContext)` once, then `int fill(long[] rowBuffer, int maxRows)` returning packed row-major ids (0 = exhausted), `close()`.
- `evaluation/codegen/KernelContext.java` — public runtime inputs: `adjacencies[]`, `constants[]`, `entrySlots[]`, `keyDomains[][]`. This plan adds `hooks`.
- `evaluation/LmdbNativeJaninoCodegen.java` — package-private compile service: shape-keyed per-store caches, single daemon compiler thread, threshold admission, failed-shape memoization, `rdf4j.lmdb.janinoCodegen.dumpDir` source dumping, counters. Used as-is.
- `evaluation/LmdbNativeJaninoPipeline.java` / `LmdbNativeJaninoAggregate.java` — the two existing shape-specific recognizer+emitter pairs. Untouched by this plan; they are the template for emission style (conservative Java, `StringBuilder` source assembly, FNV-hashed class names in package `org.eclipse.rdf4j.sail.lmdb.gen`).

Generated classes load in a child classloader, so they can only touch *public* types — hence everything generated code references lives in the public-but-`@InternalUseOnly` package `evaluation/codegen/`.

"Kernel IR" in this plan means: an in-memory tree of Java objects (package-private classes nested in `evaluation/LmdbNativeKernelIr.java`) describing a query pipeline in terms of the primitives below. It is built programmatically (by tests now, by lowering passes later), then handed to `evaluation/LmdbNativeKernelEmitter.java` which returns Java source implementing `JaninoKernel`.

The fourteen primitives and their exact semantics:

1. **Enumerate** — outermost row producers. Three modes: ADJ_KEYS (iterate a CSR view's key domain via `keyCount/keyAt`, writing the key into a column, optionally also expanding that key's neighbor run into a second column in the same loop — the fused form the existing pipeline emitter calls ROOT_ENUM); DOMAIN (iterate a `long[]` from `context.keyDomains`, e.g. a VALUES list or a subquery result); ENTRY (a single row seeded from `context.entrySlots` — the correlated/nested-invocation form).
2. **Probe** — expand: look up a key operand in a CSR view and loop its neighbor run into a fresh column.
3. **ProbeClose** — both endpoints known: count matching entries in the key's run; emit the continuation that many times (multiplicity semantics identical to the existing pipeline CLOSE step) or, in semi mode, exactly once if any match exists.
4. **Intersect** — worst-case-optimal building block: k key operands over k CSR views; emit each neighbor id present in *all* k runs (sorted-merge intersection; runs are sorted by neighbor id in unsigned order). Result goes into one column. Duplicate neighbors within a run collapse to their multiplicity product only in multiplicity mode; default emits distinct intersection members (set semantics), which is what WCOJ levels need.
5. **FilterId** — id-decidable guards: EQ, NE, IN (membership in a constant id set baked into the context constants), RANGE_UNSIGNED (inclusive unsigned bounds). Soundness of using these (id equality vs SPARQL value equality) is the *lowering*'s obligation; the primitive just emits the comparison.
6. **FilterValue** — semantic escape hatch: `hooks.testFilter(filterId, args...)` with the referenced column/constant/entry ids as arguments; the engine-side hook evaluates the real SPARQL expression (including the error-→-false convention for plain FILTER). The kernel only controls *where* in the loop nest the call sits.
7. **LeftProbe** — OPTIONAL: like Probe, but when the key has no run (or the key operand is NULL_ID = -1), the continuation runs exactly once with the value column set to NULL_ID. Nested OPTIONALs compose as nested LeftProbes because a NULL key yields the null arm again.
8. **Exists / AntiExists** — a nested sub-pipeline compiled into a private boolean method that returns true on the first row reaching its end (short-circuit). Exists guards the continuation on true, AntiExists on false. MINUS lowers to AntiExists over the shared-variable bindings (lowering's choice; same generated shape).
9. **Union** — an ordered list of branches, each a sub-pipeline; branches run sequentially, each followed by the shared continuation. Columns produced by one branch but not another are reset to NULL_ID before each branch so no bindings leak across branches.
10. **Bind** — ALIAS (copy one operand into a new column) or HOOK (`hooks.computeBind(bindId, args...)` returning an id, NULL_ID meaning the BIND produced an error/unbound — the continuation still runs, per SPARQL BIND semantics).
11. **Aggregate** — terminal consumer (replaces Emit). Global or grouped (one or more group columns; single-column groups use `KernelRuntime.LongIntMap`, multi-column groups intern packed keys through `KernelRuntime.RowSet`). Functions per output: COUNT_STAR, COUNT (of a column, skipping NULL_ID), COUNT_DISTINCT (per-group `LongHashSet`), SUM/MIN/MAX/AVG over `hooks.doubleValue(id)` guarded by `hooks.isNumeric(id)` (non-numeric ids are skipped; full SPARQL error semantics stay a lowering/hook concern). Output rows are group columns followed by accumulator results; counts emit as plain longs, double accumulators emit via `Double.doubleToLongBits` (documented in the node; the engine-side consumer decodes). An optional HAVING guard (unsigned compare of a count accumulator against a constant) filters groups at emission.
12. **Dedup** — DISTINCT over emitted rows: a `KernelRuntime.RowSet` membership test wrapped around the emit call.
13. **OrderBy** — post-run sort of the materialized out-buffer by key columns, ascending or descending per key, in unsigned id order or via `hooks.compareValues` when a hook is attached; optional LIMIT/OFFSET truncation (top-k via `KernelRuntime` when a limit is present).
14. **PathExpand** — `+`/`*` property paths: breadth-first search from a source operand over one CSR view using a `LongHashSet` visited set and a growable frontier, writing each reached node into a column (minHops 0 includes the source itself; 1 excludes it). Fixed-length paths are not a primitive — they lower to Probe chains.

**Emit** is the fifteenth node kind but not a primitive of its own semantics: it names the columns of the result row and terminates a pipeline that is not terminated by Aggregate.

A "pipeline" is a Java `List` of these nodes executed as nested loops in order; container nodes (Exists/AntiExists, Union branches) hold sub-pipelines. Operands are (CONST, index into `context.constants`), (ENTRY, index into `context.entrySlots`), or (COL, column index). Columns are `long` instance fields `v0..vk` of the generated class.

## Plan of Work

Milestone 1 (SPI + runtime): create `evaluation/codegen/KernelHooks.java` (public interface: `boolean testFilter(int filterId, long a0, long a1, long a2)` — fixed arity 3 with NULL_ID padding keeps the Janino call site simple; `long computeBind(int bindId, long a0, long a1)`; `int compareValues(long left, long right)`; `boolean isNumeric(long id)`; `double doubleValue(long id)`). Create `evaluation/codegen/KernelRuntime.java` (public final; nested public static classes `LongHashSet` (open addressing, handles -1 and 0, `boolean add(long)`, `boolean contains(long)`, `int size()`), `LongIntMap` (`int getOrInsert(long key)` returning stable ordinals, `int size()`, `long keyAt(int ordinal)`), `RowSet` (interns fixed-stride packed rows: `int addIfAbsent(long[] buf, int offset)` returning ordinal or -1 when already present, `int internOrGet(long[] buf, int offset)` always returning the ordinal, `int size()`, `long value(int ordinal, int column)`), static `sortRows(long[] rows, int rowCount, int stride, int[] keys, boolean[] desc, KernelHooks hooks)` (insertion into index sort then permute; hooks null → unsigned id order), static `topKRows(...)` bounded variant). Extend `KernelContext` with `public final KernelHooks hooks` plus the five-argument constructor; keep the old constructor delegating with null. Add `src/test/java/.../evaluation/KernelRuntimeTest.java` covering: -1/0 keys, growth past initial capacity, RowSet collision behavior with equal prefixes, sort stability and desc order, top-k versus full sort agreement.

Milestone 2 (IR model): create `evaluation/LmdbNativeKernelIr.java` — package-private final class with `NULL_ID = -1L`; nested `Operand` (kind CONST/ENTRY/COL + index); nested node classes exactly matching the primitive list above, each carrying only integers/booleans/int-arrays (no engine references — the IR is pure shape, so its canonical string doubles as the codegen cache key); a `Kernel` root object holding the pipeline list, declared column count, emit/aggregate terminal, and computing `shapeKey()` (a canonical rendering of the whole tree) and `className()` (`org.eclipse.rdf4j.sail.lmdb.gen.IrKernel_` + FNV-64 of the shape key, same convention as the existing emitters); structural validation in constructors (column indices in range, terminal present, Aggregate only terminal, Union branches produce a consistent column story) so malformed trees fail fast at build time, not at compile time.

Milestone 3 (emitter): create `evaluation/LmdbNativeKernelEmitter.java` — `static String emit(LmdbNativeKernelIr.Kernel kernel)`. Emission layout: package `org.eclipse.rdf4j.sail.lmdb.gen`; imports for `NativeLmdbQuerySource`, `JaninoKernel`, `KernelContext`, `KernelHooks`, `KernelRuntime`; fields for adjacency views `a0..`, constants `c0..`, entry slots `e0..`, domains `dom0..`, hooks, columns `v0..`, out-buffer machinery identical to the pipeline emitter (`out/outCount/outPos/ran`, `fill` copies packed rows); `run()` starts pipeline emission. Each pipeline is emitted as a chain of private void methods `p<N>_<i>()` (pipeline N, node i): each node's method contains its loop/guard and calls the next node's method in its body; the terminal method does the emit/aggregate update. This "one method per node, columns in fields" design is what makes Union (call continuation after each branch pipeline), LeftProbe (matched flag; null arm calls continuation once), and Exists (separate `boolean ex<N>()` method family that returns true at its terminal) compose without code duplication. Aggregate terminals write into field-held accumulator structures (`LongIntMap groups`, `long[] countAcc`, `double[] sumAcc`, `LongHashSet[] distinctAcc`, grown geometrically) and a `flush()` step after `run()` materializes group rows (applying the optional HAVING guard) into the out buffer; OrderBy/limit run inside the same flush via `KernelRuntime.sortRows/topKRows`; Dedup wraps emit with a `RowSet` guard. PathExpand emits an in-method BFS: visited `LongHashSet`, frontier `long[]` stack with an index pointer, loop popping until empty, pushing unvisited neighbors, calling the continuation per accepted node (minHops honored by seeding). All generated code stays in the Janino-safe subset: `long/int/boolean/double` locals, `for`, `if`, `continue` with labels avoided (use flags), method calls, `System.arraycopy`.

Milestone 4 (tests): create `src/test/java/.../evaluation/LmdbNativeKernelIrEmitterTest.java` with a small fixture kit: `FixtureAdjacency implements NativeLmdbQuerySource.NativeAdjacency` built from `long[key][neighbors...]` literal tables (sorted, dense ids in insertion order of sorted keys, contexts default 0, `runsNeighborOrdered` true), a `TestHooks implements KernelHooks` with programmable behavior, and a `runKernel(Kernel ir, KernelContext ctx)` helper that compiles via `LmdbNativeJaninoCodegen.kernel(...)` with threshold forced to 0 (system property set/cleared per test) + `awaitKernel`, then drains `fill` into a `List<long[]>`. One focused test per primitive asserting exact expected row multisets (order-insensitive except for OrderBy tests), plus composition tests: (a) Union of two Enumerates → LeftProbe → Exists guard → grouped COUNT_DISTINCT with HAVING (the corpus skeleton-6 shape); (b) subquery-as-domain: run kernel A (grouped count), feed its group column as `keyDomains[0]` of kernel B (Enumerate DOMAIN → Probe → Emit); (c) PathExpand `+` over a graph with a cycle (visited-set termination proof); (d) Dedup + OrderBy desc + limit stack. Every test also implicitly proves compilation (a compile failure returns null → assertion failure).

Milestone 5 (hygiene + sweep): headers + `// Some portions generated by Claude` signature on all new files; `cd scripts && ./checkCopyrightPresent.sh`; formatter `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`; targeted runs `python3 .codex/skills/mvnf/scripts/mvnf.py KernelRuntimeTest` and `... LmdbNativeKernelIrEmitterTest`; full `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` judged by report XMLs against the branch's known pre-existing failures (12–13 across 7 classes, verified pre-existing at HEAD `9fde1f1172` during plan-17 work); update this plan's living sections.

## Concrete Steps

All commands from `/Users/havardottestad/Documents/Programming/rdf4j`.

Root install first (offline, workspace-local repo):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{s=1} s{print}'

Module compile while iterating:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install 2>&1 | tail -5

Tests, one at a time via mvnf:

    python3 .codex/skills/mvnf/scripts/mvnf.py KernelRuntimeTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelIrEmitterTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Formatter + copyright before finalizing:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

## Validation and Acceptance

Run `python3 .codex/skills/mvnf/scripts/mvnf.py KernelRuntimeTest` and expect all tests passed. Run `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelIrEmitterTest` and expect all tests passed (one or more per primitive plus compositions); each test fails before the emitter exists (class not found / kernel null) and passes after. Run the full module sweep and compare failing classes against the known pre-existing set — acceptance is zero classes failing that did not fail at branch HEAD before this change. Behavior of existing query execution is unchanged by construction: no production call site invokes the new classes yet (`LmdbNativeKernelIr`/`LmdbNativeKernelEmitter` are only reachable from tests until a lowering plan wires them in).

## Idempotence and Recovery

Everything is additive: five new main-tree files (two public SPI, two package-private, one modified public SPI class gaining a field + constructor) and two test files. Re-running any step is safe; deleting the new files restores the previous state exactly. The `KernelContext` change is compile-compatible with both existing emitters (old constructor kept). No flags change; the new code is dead until lowered to.

## Artifacts and Notes

Acceptance transcripts (2026-07-24):

    mvnf KernelRuntimeTest:            Summary: tests=8, failures=0, errors=0, skipped=0, time=0.034s
    mvnf LmdbNativeKernelIrEmitterTest: Summary: tests=23, failures=0, errors=0, skipped=0, time=0.615s
    mvnf core/sail/lmdb (full sweep, judged by report XMLs):
      tests=2159 failures=4 errors=0 skipped=3
      failing classes: LmdbNativeFeatureFlagForkTest (3F), LmdbNativeLeftJoinFilterRewriteTest (1F)
      — both independent of this change (no reference to any new class; FeatureFlagForkTest under
      concurrent modification by other branch work this same day); both new test classes green in-sweep.

Generated-source debugging: set `-Drdf4j.lmdb.janinoCodegen.dumpDir=<dir>` — every compiled (or failed)
kernel source is written there; the IR test also embeds the full source in its assertion message on
compile failure.

## Interfaces and Dependencies

No new external dependencies (Janino is already present from plan 17).

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelHooks.java` (public, `@Experimental @InternalUseOnly`):

    public interface KernelHooks {
        boolean testFilter(int filterId, long a0, long a1, long a2);
        long computeBind(int bindId, long a0, long a1);
        int compareValues(long left, long right);
        boolean isNumeric(long id);
        double doubleValue(long id);
    }

In `.../codegen/KernelRuntime.java` (public final): nested `LongHashSet`, `LongIntMap`, `RowSet`; statics `sortRows(long[] rows, int rowCount, int stride, int[] keyColumns, boolean[] descending, KernelHooks hooks)` and `int topKRows(long[] rows, int rowCount, int stride, int[] keyColumns, boolean[] descending, KernelHooks hooks, int limit)`.

In `.../codegen/KernelContext.java`: add `public final KernelHooks hooks;` and the five-argument constructor; four-argument constructor delegates with `null`.

In `.../evaluation/LmdbNativeKernelIr.java` (package-private): `Operand.constant(i)/entry(i)/col(i)`; builders for every node kind (`enumerateAdjKeys`, `enumerateDomain`, `enumerateEntry`, `probe`, `probeClose`, `intersect`, `filterId`, `filterValue`, `leftProbe`, `exists`, `antiExists`, `union`, `bindAlias`, `bindHook`, `pathExpand`, `dedup`, `orderBy`, `emit`, `aggregate` + `AggregateOutput.countStar/count/countDistinct/sum/min/max/avg` + `having`); `Kernel` with `shapeKey()`, `className()`, `columnCount()`.

In `.../evaluation/LmdbNativeKernelEmitter.java` (package-private): `static String emit(LmdbNativeKernelIr.Kernel kernel)`.

---

Revision note (2026-07-24, initial): plan authored on user request to "implement all the composable primitives" from the plan-18 kernel IR design; scoped to IR + emitter + runtime + exhaustive primitive tests, with plan-lowering staged as follow-ups per plan 18 §3.7.

Revision note (2026-07-24, completion): Progress, Surprises & Discoveries, Artifacts and Outcomes updated with as-run results (8 + 23 tests green, sweep verdict with independence proof for the 4 unrelated failures). Interface list reflects as-built signatures; the Dedup and OrderBy primitives are realized as terminal configurations (`Emit.distinct()`, `OutputMods`) rather than standalone pipeline nodes, as the Plan of Work specified.
