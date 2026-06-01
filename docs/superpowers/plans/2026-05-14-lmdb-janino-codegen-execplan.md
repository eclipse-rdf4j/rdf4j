# Productionize Selective Janino Codegen for LMDB Hot Paths

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root. It is intentionally self-contained: a future agent should be able to start from this file and the current working tree without reading the prior chat.

## Purpose / Big Picture

LMDB query evaluation currently spends hot-loop work on generic iterator boundaries, reusable projection code, condition checks, and per-row dispatch. The goal is to selectively generate small Java classes for stable LMDB operator shapes so the JVM sees final classes, final methods, primitive locals, predictable branches, and reusable tuple slots. After this work is productionized, a developer can enable an internal LMDB codegen flag, run the existing theme query benchmarks, and observe lower p50/p90 time-to-first-row latency for selected query shapes without changing RDF4J query semantics.

The first milestone was an investigation and proof of feasibility. The current phase productionizes deliberately narrow,
shape-derived eval operators behind internal LMDB controls. Janino is enabled by default for supported safe shapes, while
every unsupported or risky shape falls back to the interpreted path. The generated code may depend on public LMDB
internals that are annotated `@Experimental` and `@InternalUseOnly`; these APIs exist only so generated LMDB operators
can call small, stable helpers without reflection or package-private access hacks.

No implementation in this plan may be hard-coded to a benchmark query, theme name, query index, or exact application
IRI layout. A shortcut is valid only when it is derived from the algebra/query-plan shape and can apply to any query with
the same normalized shape. Query-specific performance wins are acceptable only as evidence that a reusable shape-derived
operator helped that query.

## Progress

- [x] (2026-05-14 12:29Z) Captured initial quick install evidence in `initial-evidence.txt`.
- [x] (2026-05-14 12:34Z) Confirmed the best first hot targets: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator.java`, `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`, and `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/join/LmdbIdBGPQueryEvaluationStep.java`.
- [x] (2026-05-14 12:35Z) Added test-scope Janino dependency to `core/sail/lmdb/pom.xml`.
- [x] (2026-05-14 12:35Z) Added `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbJaninoProjectionPrototypeTest.java`, a Routine C prototype for generated projection iterators.
- [x] (2026-05-14 12:36Z) Added JMH sample-time benchmark methods `timeToFirst10Rows` and `timeToFirst100Rows` to `ThemeQueryBenchmark`.
- [x] (2026-05-14 12:43Z) Verified the prototype and two existing LMDB unit gates with `-DskipITs`.
- [x] (2026-05-14 12:44Z) Dry-ran the first-row benchmark command shape.
- [x] (2026-05-14 18:38Z) Ran the required root quick install before production edits.
- [x] (2026-05-14 18:40Z) Revised this plan for the user decision to expose minimal LMDB internals as public experimental APIs for generated operators.
- [x] (2026-05-14 18:45Z) Added failing production tests for the disabled-by-default generated projection path and the public experimental codegen bridge.
- [x] (2026-05-14 18:58Z) Promoted Janino to a runtime module dependency at `org.codehaus.janino:janino:3.1.12`.
- [x] (2026-05-14 19:05Z) Implemented `LmdbCodegenSettings`, `LmdbCodegenExplain`, and `LmdbGeneratedRecordIteratorFactory`.
- [x] (2026-05-14 19:10Z) Wired projection codegen into `LmdbSailStore` only when enabled and only for supported shapes.
- [x] (2026-05-14 19:55Z) Added a deterministic fallback for repeated projection slots before Janino compilation.
- [x] (2026-05-14 19:56Z) Verified the production codegen test, ID BGP test, and prefix scan test with codegen enabled.
- [x] (2026-05-16 21:10Z) Removed active theme/query-specific aggregate shortcut hooks and kept only reusable eval-side
      ID join, batched exists/filter, and count-shape candidates.
- [x] (2026-05-16 21:18Z) Fixed generic batched ID join binding preservation so grouped queries keep left-side variables
      when a right optional/batched branch produces only right-side IDs.
- [x] (2026-05-16 21:22Z) Recorded non-forked `ThemeQueryBenchmark.executeQuery` samples after the generic fix:
      `MEDICAL_RECORDS q2` mean 9.649 ms/op, `PHARMA q10` mean 74.625 ms/op.
- [x] (2026-05-17 05:09Z) Captured a green root quick install before the eval-side regression fix milestone.
- [ ] Add failing regression tests for oversized EXISTS membership fallback and touched-slot hash-set clearing.
- [ ] Cap broad EXISTS membership builds and fall through to direct ID probes when oversized.
- [ ] Replace full-array `LmdbLongHashSet.clear()` with touched-slot clearing.
- [ ] Verify focused LMDB unit gates, then benchmark and profile MEDICAL_RECORDS q3/q4/q7/q8/q9.
- [ ] Run full baseline JMH matrix for `ThemeQueryBenchmark.executeQuery` and first-row methods across MEDICAL_RECORDS q0/q10, LIBRARY q7, PHARMA q10, and one broad POSC/SPOC-heavy case.
- [ ] Add a plan-derived Janino operator classifier keyed by normalized algebra shape, not query identity.
- [ ] Extend the production path to statement-local ID predicate plans after projection parity passes.
- [ ] Compare codegen enabled/disabled with JIT logs, async-profiler, allocation rate, and correctness parity.

## Surprises & Discoveries

- Observation: Generated code can live in a separate Janino classloader if it only touches public APIs. Package-private LMDB internals such as `TripleStore.TripleIndex` are not safe generated-code dependencies.
  Evidence: `LmdbJaninoProjectionPrototypeTest` compiles a final generated class in package `org.eclipse.rdf4j.sail.lmdb.codegen` and uses only `RecordIterator`, `LmdbValue.UNKNOWN_ID`, and primitive arrays.

- Observation: `mvn ... verify` with only `-Dtest=...` still continues into Failsafe integration tests for the module. Use `-DskipITs` for focused unit verification.
  Evidence: the first focused verify ran the selected Surefire tests green, then entered `LmdbThemeQueryRegressionIT`. The rerun with `-DskipITs` completed with `BUILD SUCCESS`.

- Observation: During the Routine C prototype, the local workspace already contained Janino 3.0.9 in `.m2_repo`, but not Janino 3.1.12. The prototype used 3.0.9 only to keep the spike offline and reversible.
  Evidence: offline quick install and focused verify resolved `org.codehaus.janino:janino:3.0.9` successfully.

- Observation: The current branch is `ID-based-lmdb-joins`, so production work can proceed without starting on main or master.
  Evidence: `git status --short --branch` reported `## ID-based-lmdb-joins`.

- Observation: Repeated projection slots, such as a statement shape where subject and object map to the same binding slot, are not a safe generated-code shape yet.
  Evidence: a focused test first produced a generated-source failure path with fallback reason `CompileException`; the factory now rejects that shape before compilation with fallback reason `repeated-projection-slot` and does not consume the base iterator.

- Observation: Batched ID evaluation can silently corrupt grouped results if the right branch reports that it includes all
  input bindings but only materializes right-side variables.
  Evidence: `ThemeQueryBenchmarkSmokeIT` returned one group for `MEDICAL_RECORDS q2` and `PHARMA q1` with ID joins
  enabled, but passed with `-Drdf4j.lmdb.disableIdJoins=true`. The generic fix appends incoming binding names to the
  output `IdBindingInfo`, after which the focused smoke tests and the ID batch/left-join tests pass.

- Observation: Query-plan-derived shortcuts are allowed; benchmark-query-derived shortcuts are not.
  Evidence: the user explicitly clarified on 2026-05-16 that if an optimized eval shortcut can be derived from the query
  plan, the shortcut can be generated for any matching query using Janino, while query-specific evaluation
  optimizations are disallowed.

- Observation: MEDICAL_RECORDS q8 and q4 regressions are dominated by broad subject-predicate membership scans in the
  eval path, not by Janino compile overhead.
  Evidence: Docker JFR CPU-time profiles from 2026-05-17 show `LmdbIdBatchExistsFilterQueryEvaluationStep` and
  `LmdbIdRawExistsDistinctCountQueryEvaluationStep` spending their hottest paths in
  `LmdbIdExistsPatternPlan.buildSubjectPredicateMembership -> LmdbRecordIterator.next -> LMDB.nmdb_cursor_get`.

- Observation: MEDICAL_RECORDS q3 and q9 still show avoidable batch join scratch cost.
  Evidence: Docker JFR CPU-time profiles from 2026-05-17 show `LmdbLongHashSet.clear -> Arrays.fill` under
  `LmdbIdBatchJoinIterator.openRightForGroup`.

## Decision Log

- Decision: Keep Janino as a test-scope dependency for the initial investigation.
  Rationale: Production dependency choice needs a current health check and full benchmark evidence. Adding Janino to runtime before proving wins would add classloading and dependency risk without user-visible benefit.
  Date/Author: 2026-05-14 / Codex

- Decision: Promote Janino to a runtime LMDB module dependency for the disabled-by-default production shell.
  Rationale: The user explicitly asked to implement the production plan, and the factory is now production code. The dependency remains isolated to `core/sail/lmdb`, codegen is feature-flagged off by default, and unsupported shapes fall back to the existing iterator path.
  Date/Author: 2026-05-14 / Codex

- Decision: Prototype generated projection first, not the full LMDB cursor scan.
  Rationale: `BindingProjectingIterator.next()` is a smaller boundary than `LmdbRecordIterator.next()`, but it still exercises the important mechanics: final generated class, final `next()`, fixed slot indexes, bound-slot conflict checks, reused scratch array, branch counting, bytecode-size capture, and a stable template key.
  Date/Author: 2026-05-14 / Codex

- Decision: Use source-embedded constants in the prototype.
  Rationale: It gives the JIT the strongest constant-folding opportunity and removes per-row flag checks. A production implementation should still compare source-embedded constants against final instance fields because source embedding increases compile/cache cardinality.
  Date/Author: 2026-05-14 / Codex

- Decision: No default production codegen activation yet.
  Rationale: The production shell proves feasibility, but the acceptance bar still requires JIT evidence, allocation evidence, broad correctness parity, fallback behavior, cache controls, and at least two target shapes with a >=10% p50 or p90 first-row improvement.
  Date/Author: 2026-05-14 / Codex

- Decision: Enable Janino by default only through guarded shape-derived factories.
  Rationale: The user requested default-on Janino. Default-on is acceptable only when the classifier rejects unsupported
  shapes before compilation and the interpreted path remains the semantic fallback. Explain output must make cache hits,
  compile failures, oversized methods, and unsupported predicates visible.
  Date/Author: 2026-05-16 / Codex

- Decision: Ban hand-coded theme/query shortcuts from the eval implementation.
  Rationale: The performance goal is to improve reusable LMDB eval mechanics. Any optimized shortcut must be derivable
  from the algebra/query plan and keyed by a normalized operator fingerprint so it can apply to any query with that same
  shape.
  Date/Author: 2026-05-16 / Codex

- Decision: Cap broad EXISTS membership builds at 4096 scanned rows and fall through to direct ID probes when the cap is
  exceeded.
  Rationale: The profiled regressions come from scanning and hashing large RHS membership sets for each batch or raw
  count path. A capped attempt preserves the win for small reusable sets, while the oversized sentinel prevents repeated
  partial scans and keeps the fallback shape-derived instead of query-specific.
  Date/Author: 2026-05-17 / Codex

- Decision: Keep this regression patch eval-side only and do not alter optimizer/sketch behavior.
  Rationale: q4 and q9 also show plan drift, but the user explicitly asked not to work on the optimizer right now. The
  current patch must improve or explain the eval-side bottlenecks before any optimizer changes are considered.
  Date/Author: 2026-05-17 / Codex

- Decision: Make the minimal LMDB codegen surface public and annotate it with both `@Experimental` and `@InternalUseOnly`.
  Rationale: The user explicitly approved public LMDB internals when annotated experimental. This lets Janino-generated classes live in a normal generated package and call final public helpers without reflection, `MethodHandles`, package-private classloader tricks, or broad field exposure.
  Date/Author: 2026-05-14 / Codex

- Decision: Productionize projection fusion before cursor-loop fusion or ID predicate fusion.
  Rationale: Projection fusion is the smallest production slice that exercises Janino compilation, cache keys, fallback, explain diagnostics, and codegen-enabled parity. Cursor-loop fusion touches native cursor state and transaction renewal; ID predicate fusion must preserve subtle SPARQL value semantics. Those should follow only after the production codegen shell is verified.
  Date/Author: 2026-05-14 / Codex

- Decision: Keep repeated projection slots on the interpreted fallback path for this slice.
  Rationale: The existing scratch-reuse interpreted projection path is the semantic source of truth for this production slice. Repeated slots need a separate parity decision before generated code can safely enforce or rewrite same-slot equality.
  Date/Author: 2026-05-14 / Codex

## Outcomes & Retrospective

The investigation produced a working Janino prototype, benchmark hooks, and a narrow production projection-codegen shell without changing query semantics. Codegen is now intended to be default-on for supported safe shapes, with explicit fallback for everything else. The production shell has compile/cache/explain plumbing and focused parity tests, but it is not a go for broader generated operators until the benchmark, profile, allocation, and theme-regression gates pass.

The main lesson is that generated code should call a deliberately small public experimental bridge, not package-private internals or mutable cursor fields. The fastest safe path is to specialize around `RecordIterator` first, then move selected cursor helpers into the bridge only if measurements show cursor-loop fusion is worth the extra transaction and native-state complexity.

The second lesson is that shape-derived specialization is the boundary. A generated operator may be very specific to an
algebra shape: BGP graph, fixed constants, optional/exists branch, grouping keys, aggregate kind, filter predicate plan,
nullability, and sorted batch metadata. It may not be specific to `MEDICAL_RECORDS q2`, `PHARMA q10`, or any other named
benchmark query.

## Context and Orientation

The LMDB module is `core/sail/lmdb`. A `RecordIterator` is a small internal iterator interface that returns one `long[]` row at a time, where the row contains internal RDF value IDs. `LmdbRecordIterator` scans LMDB indexes and decodes key bytes into primitive IDs. `BindingProjectingIterator`, located near the end of `LmdbSailStore.java`, wraps a raw `RecordIterator`, projects subject/predicate/object/context IDs into binding slots, and checks conflicts against already-bound slots.

The initial production targets are:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator.java`: cursor scan and match loop.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: `getRecordIterator(...)` wiring and `BindingProjectingIterator`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/join/LmdbIdBGPQueryEvaluationStep.java`: ID-based BGP evaluation and join-stage composition.

The investigation and production-slice artifacts are:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbJaninoProjectionPrototypeTest.java`: compiles and tests generated projection source.
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbJaninoCodegenProductionTest.java`: verifies the disabled fallback, public experimental bridge, generated projection path, and repeated-slot fallback.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbGeneratedRecordIteratorFactory.java`: compiles and caches the first production generated projection iterator.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCodegenSettings.java` and `LmdbCodegenExplain.java`: hold internal flags and explain diagnostics.
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`: contains `executeQuery`, now also `timeToFirst10Rows` and `timeToFirst100Rows`.
- `core/sail/lmdb/pom.xml`: contains the Janino 3.1.12 dependency used by the production factory.

## Plan of Work

Milestone 1 is complete. It proves Janino can generate a final class implementing `RecordIterator` without touching production code. The generated class uses final methods, primitive locals, fixed slot indexes, source-embedded constants, a reused scratch array, and plain `if`/`while` control flow. It records an explain-like object with template key, class name, source length, bytecode size, static branch count, compile time, and cache-hit status.

Milestone 2 adds production infrastructure behind an internal disabled-by-default flag. Add a small `LmdbCodegenSettings` internal settings holder, a `LmdbCodegenExplain` value object, and a `LmdbGeneratedRecordIteratorFactory` that receives a stable operator shape and either returns a generated `RecordIterator` or records a fallback reason. The first production operator only replaces `BindingProjectingIterator` for simple projection shapes. It must not change query results or public RDF4J query APIs.

Milestone 3 makes the minimal LMDB codegen surface public and experimental. Add `@Experimental` to `RecordIterator`, make `LmdbRecordIterator` public and `@Experimental @InternalUseOnly`, and make only the `TripleStore.TripleIndex` methods needed by generated code public and annotated or documented for generated LMDB operators. Do not expose mutable cursor fields. If generated code needs a cursor operation, expose it through a tiny final public helper method or a new `LmdbGeneratedScanSupport` class.

Milestone 4 adds a debug-only codegen explain artifact. The artifact includes template key, generated class name, source length, bytecode size, static branch count, compile time, cache hit/miss, and fallback reason. Inlining decisions are not collected in normal runtime; collect them only from JMH runs with JIT logging.

Milestone 5 adds correctness parity tests with codegen enabled and disabled. The tests run projection conflict cases, generated-code fallback cases, existing LMDB ID join cases, and representative theme regression cases and assert identical counts and bindings.

Milestone 6 benchmarks before adding cursor fusion. Run baseline and projection-codegen variants for time-to-first 10 rows, time-to-first 100 rows, full count time, and allocation rate. If projection-only codegen does not show a meaningful win, stop before adding cursor-level complexity.

Milestone 7, only if Milestone 6 passes, prototypes fused scan/filter/project around `LmdbRecordIterator.next()`. This phase specializes by access shape, bound mask, filter opcode set, nullability mode, order requirement, and explicit/inferred flag. It includes hard fallbacks for unsupported filters, transaction overlays, generated-method size risk, compile failure, and cache pressure.

Milestone 8 adds plan-derived Janino eval operators. Build a classifier that consumes the already-optimized algebra tree
inside `LmdbEvaluationStrategy.prepare(...)` and emits a stable operator fingerprint. The fingerprint may include:
operator kind, flattened BGP patterns, variable equivalence graph, constants, left/right optional or exists branch shape,
group variables, aggregate operator, distinct variable, projection slots, local ID predicate plans, nullability mode,
and incoming batch metadata such as sorted-by variable. The generated Java class is keyed only by this fingerprint and
must be reusable for every query with the same fingerprint. The first generated classes should target hot eval-side loops
already represented by hand-written generic operators: batch merge/projection, EXISTS probing, and grouped/distinct ID
aggregation. Do not change optimizer rules in this milestone.

Milestone 9 fixes the 2026-05-17 MEDICAL_RECORDS eval-side regressions before adding more generated code. Add tests that
prove oversized subject-predicate membership candidates do not force broad membership scans, then cap membership
collection at `org.eclipse.rdf4j.sail.lmdb.exists.membership.maxRows` with default `4096`. When the cap is exceeded,
cache an oversized sentinel and continue through the sorted direct-probe EXISTS path or the existing fallback path for
raw distinct count. Add metrics that distinguish real membership builds, cache hits, oversized fallbacks, and direct
probes. Separately, change `LmdbLongHashSet` so `clear()` zeros only slots touched by inserts, eliminating full-array
clears from batch join group transitions.

## Concrete Steps

Run all commands from repository root: `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

Before any tests, publish current modules to the workspace-local Maven repo:

    mvn -o -Dmaven.repo.local=.m2_repo -Pquick install | tail -200

Run the completed prototype and nearby unit gates:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbJaninoProjectionPrototypeTest,LmdbIdBGPEvaluationTest,LmdbRecordIteratorPrefixScanTest verify | tail -500

Expected transcript excerpt:

    Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- LmdbRecordIteratorPrefixScanTest
    Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- LmdbIdBGPEvaluationTest
    Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- LmdbJaninoProjectionPrototypeTest
    BUILD SUCCESS

Dry-run a first-row benchmark command:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method timeToFirst10Rows --param themeName=MEDICAL_RECORDS --param z_queryIndex=0 --warmup-iterations 3 --measurement-iterations 5 --forks 1 --dry-run

Expected transcript excerpt:

    java -jar /Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/target/jmh.jar -wi 3 -i 5 -f 1 -p themeName=MEDICAL_RECORDS -p z_queryIndex=0 org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.timeToFirst10Rows

For real baseline measurements, run the same script without `--dry-run` for `executeQuery`, `timeToFirst10Rows`, and `timeToFirst100Rows` across the target shapes. Record the JMH output and keep the generated store/cache paths stable between codegen-off and codegen-on runs.

For JIT and CPU evidence, use the async-profiler wrapper already available in this workspace:

    $CODEX_HOME/skills/async-profiler-java-macos/scripts/profile_rdf4j_benchmark_macos.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery

## Validation and Acceptance

Correctness acceptance for the current production slice is the focused unit verify command above passing eight tests. Broader production acceptance is stricter: with codegen enabled and disabled, existing LMDB ID join tests, targeted theme regression tests, and parity checks must return identical counts and bindings.

Performance acceptance for production is: warm cached codegen improves at least two target shapes by >=10% on p50 or p90 first-row metrics and does not regress full-query time, allocation rate, or correctness on control shapes.

JIT acceptance is: the generated `next()` or future `runNext()` method appears as a flat top-level frame in CPU/wall profiles, tiny generated helpers inline in `PrintInlining` or compiler logs, and no megamorphic call sites appear inside generated loops.

Code-size acceptance is: generated source and bytecode are dumped for inspection, helper instruction counts remain tiny, and the generator rejects or falls back before any generated hot method approaches JVM method-size risk.

Fallback acceptance is: Janino compile failure, unsupported filters, oversized generated code, transaction overlays, and cache pressure all fall back to the existing interpreted path with an explainable fallback reason.

## Idempotence and Recovery

The current production slice is additive and safe to rerun. It does not alter default query execution because the codegen flag is disabled by default. If Janino resolution fails offline, fetch the chosen version once with a non-offline quick install, then return to offline builds.

If a focused verify accidentally enters Failsafe integration tests, stop and rerun the intended unit command with `-DskipITs`. Do not use `-am` with tests. Do not use `-q` with tests. Always run the quick install before tests so the workspace-local `.m2_repo` has current module artifacts.

If production codegen causes any correctness mismatch, disable the internal flag, keep the generated-source dump, and reduce the shape until the mismatch is isolated. The interpreted path must remain the semantic source of truth.

## Artifacts and Notes

Initial quick install evidence is stored at repository root in `initial-evidence.txt`.

Focused verification evidence:

    Command: mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dorg.eclipse.rdf4j.sail.lmdb.codegen.enabled=true -Dtest=LmdbJaninoCodegenProductionTest,LmdbIdBGPEvaluationTest,LmdbRecordIteratorPrefixScanTest verify | tail -500
    Reports:
      core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbJaninoCodegenProductionTest.txt
      core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbIdBGPEvaluationTest.txt
      core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbRecordIteratorPrefixScanTest.txt
    Result:
      Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
      BUILD SUCCESS

Benchmark dry-run evidence:

    Command: scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method timeToFirst10Rows --param themeName=MEDICAL_RECORDS --param z_queryIndex=0 --warmup-iterations 3 --measurement-iterations 5 --forks 1 --dry-run
    Result:
      java -jar /Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/target/jmh.jar -wi 3 -i 5 -f 1 -p themeName=MEDICAL_RECORDS -p z_queryIndex=0 org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.timeToFirst10Rows

## Interfaces and Dependencies

The production slice uses `org.codehaus.janino:janino:3.1.12`. The first offline quick install failed because 3.1.12 was not in `.m2_repo`; a one-time non-offline quick install fetched Janino and `commons-compiler`, after which offline builds succeeded again. Janino compiles Java source strings into classes at runtime. In this plan, the generated class implements `org.eclipse.rdf4j.sail.lmdb.RecordIterator`, whose important methods are:

    long[] next();
    void close();

The first production factory should expose an internal method conceptually equivalent to:

    Optional<RecordIterator> tryCreateProjectionIterator(
        RecordIterator base,
        long[] binding,
        long[] scratch,
        ProjectionShape shape,
        CodegenExplainSink explainSink);

`ProjectionShape` must be stable and cacheable. Its key must include index order, bound mask, projection slot indexes, filter opcode set if present, nullability mode, order requirement, explicit/inferred flag, and constant policy. `CodegenExplainSink` must be debug-only or disabled by default and must not do expensive work in the hot loop.

The public experimental bridge must include javadocs that say the API is for LMDB generated operators only and is not a stable RDF4J application API.

Revision note, 2026-05-14 / Codex: created this ExecPlan after completing the Routine C prototype. The plan records the evidence, the no-production-activation decision, and the exact next productionization milestones.

Revision note, 2026-05-14 / Codex: revised for the user-approved public experimental LMDB internals boundary. The production slice now starts with projection codegen plus a minimal public bridge, before cursor-loop or ID predicate fusion.
