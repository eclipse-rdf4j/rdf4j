# Implement scope-safe SPARQL algebra rewrites

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must remain current while work proceeds. Maintain this document according to `.agent/PLANS.md`. The earlier `.agent/GH_5905_ALGEBRA_EQUIVALENCE_EXEC_PLAN.md` describes the test-only equivalence module already present on this branch; the production implementation in this plan must not depend on that module.

## Purpose / Big Picture

After this change, RDF4J's production query optimizers can prove that a proposed algebra-tree rewrite preserves variable scope, boundness, error behavior, multiplicity, ordering, correlation, and RDF4J's directional evaluation dependencies before mutating the query. A parsed query carries an immutable description of its original lexical scopes. A query-local optimization session turns that description into primitive facts, answers rewrite guards without allocating on rejected narrow-query candidates, applies accepted changes transactionally, and repairs only affected fact rows.

Users select one of four modes once per query. `OFF`, the production default, executes the existing optimizer pipeline without constructing a fact session and ignores any immutable parser provenance already attached to the query. `AUDIT` retains legacy behavior while comparing the fast fact analysis with an independently written Set-based analyzer. `ENFORCE` allows only proof-backed migrated rewrites. `SHADOW` returns the legacy plan's results while evaluating an `ENFORCE` clone against the same `SailDataset`; a semantic mismatch or candidate failure is recorded as telemetry and the legacy results are returned (fatal only when `shadowStrict=true`), while unsafe, nondeterministic, or oversized comparisons are skipped with a reason code. The behavior is demonstrated by exact SPARQL fixtures on MemoryStore, NativeStore, and LMDB, randomized differential campaigns, fast/slow and incremental/full-rebuild parity tests, and allocation and latency benchmarks.

## Progress

- [x] (2026-07-12 19:56Z) Ran the mandatory offline root quick clean install; every reactor module succeeded in 34.406 seconds and the full log is `maven-build.log`.
- [x] (2026-07-12 20:02Z) Created this governing ExecPlan from the approved implementation scope and reconciled it with the current repository and supplied archives.
- [x] (2026-07-12 20:05Z) Preserved the 46-test pre-change model baseline in `initial-evidence.txt`, then observed the focused `BindingSetAssignment` regression fail because assured names incorrectly contained both branch-only variables.
- [x] (2026-07-12 20:10Z) Implemented declared `VALUES` headers, possible-name union, assured-name intersection, one-pass transient caches, invalidation, cache-neutral equality/cloning/serialization, and the `setBindingNames` compatibility alias; five focused tests and all 51 model tests pass.
- [x] (2026-07-12 20:15Z) Added default no-op optimization-tag hooks and one transient packed `long`; focused tests prove custom-node compatibility, serialization reset, equality neutrality, clone origin preservation, and node-ID clearing.
- [x] (2026-07-12 20:25Z) Added immutable dense seed tables and a primitive builder for frames, regions, environments, symbols, origins, CSR occurrences, boundaries, exports, and allowed inputs; `QueryRoot` stores the seed transiently and shares it across clones; all 56 model tests pass.
- [x] (2026-07-12 20:28Z) Reproduced the cloned right-side `MINUS` filter, removed that unsound clone, and passed the focused regression plus all 28 `FilterOptimizerTest` methods.
- [x] (2026-07-12 20:32Z) Added explicit outer/nested projection-marker coverage, constructed parsed projections with `ASTSelect.isSubSelect()`, and passed the focused test plus all 66 `SPARQLParserTest` methods.
- [x] (2026-07-12 20:50Z) Implemented two-phase parser seeds: translation records subquery, `EXISTS`, `MINUS`, lateral, graph, and service boundaries; one final traversal assigns dense origins and symbols, CSR occurrences/exports, frames, regions, environments, hidden temporaries, and declared `VALUES` names; all 125 SPARQL parser tests pass.
- [x] (2026-07-12 22:47Z) Completed dense indexing, seed/fallback resolution, 64/128/wide fact stores, explicit tuple transfers, independent Set parity, experimental function characteristics, closed-world node/function coverage, monomorphic guards, capped shapes, and lazy liveness; all 771 evaluation-module tests pass.
- [x] (2026-07-12 20:50Z) Completed model compatibility hooks, `BindingSetAssignment` semantics, parser scope seeds, explicit projection subquery markers, and the `FilterOptimizer`-over-`Difference` correction through focused test-first groups.
- [x] (2026-07-13 01:36Z) Recreated all fifty `.rq`/`.srx` fixtures, shared TriG data, manifest, catalog, and CSV index from independently authored query/row definitions; deterministic regeneration and catalog integrity checks pass.
- [x] (2026-07-12 22:47Z) Implemented dense IDs, immutable seed import and conservative fallback resolution, the 64-bit/128-bit/wide fact stores, expression and liveness facts, capped shape refinement, function characteristics, and the independent slow analyzer.
- [x] (2026-07-13 00:45Z) Implemented the complete allowed rule catalog, clone-based hygienic candidates, transaction-owned edits, rollback, exact boundary chains, fingerprint prechecks, and exact ancestor-row validation; catalog, transaction, and parsed-seed rewrite tests pass.
- [x] (2026-07-13 01:00Z) Integrated one lazy session across standard and LMDB optimizer islands, migrated the named filter/projection/union/join/normalizer/LMDB optimizers with explicit `OFF` legacy paths, and added scope-safe algebra passes plus invalidation around legacy mutations.
- [x] (2026-07-13 01:54Z) Completed `OFF`, `AUDIT`, `ENFORCE`, and `SHADOW`, query-local configuration, fixed global telemetry counters, reason-coded skips, fatal Sail comparison, periodic audit rebuilds, and unsupported-node barriers; all 50 cases pass in all modes on MemoryStore, NativeStore, and LMDB.
- [x] (2026-07-13 03:23Z) Ran 100,000 narrow, 25,000 dual, and 10,000 wide fact seeds plus 10,000 accepted multi-rewrite sequences; fast/slow and incremental/full-rebuild facts agree exactly.
- [x] (2026-07-13 03:36Z) Cross-checked all 50 authored fixtures with Apache Jena 6.1.0 and recorded RDFLib 7.6.0's 20 disagreements without changing RDF4J/Jena-agreeing expected data; all 56 independent algebra-equivalence tests pass.
- [x] (2026-07-13 03:55Z) Measured narrow/dual/wide guards with JMH, verified linear full-scan wide scaling, met JOL node/seed budgets, and retained JFR plus C2 monomorphic-inline evidence. Whole-query `ENFORCE` overhead remains above default-enablement gates, so `OFF` remains the production default as required.
- [x] (2026-07-13 06:01Z) Required every sampled fixture query to record exact `SHADOW_MATCH` on MemoryStore, NativeStore, and LMDB; completed 809 evaluation tests, 176 SPARQL 1.1 and 66 SPARQL 1.2 compliance cases, and the complete 1,160-test LMDB module including all 12 Failsafe classes.
- [x] (2026-07-13 05:28Z) Completed deterministic fixture checking, copyright/SPDX validation, repository formatting, production-dependency inspection, and the complete offline root quick clean install; every reactor module succeeded in 26.344 seconds.
- [x] (2026-07-13 06:07Z) Prepared the implementation handoff and retained evidence packet, including full LMDB reports and regenerated post-clean JFR/JIT artifacts.
- [x] (2026-07-13 06:35Z) Added ten independently authored adversarial variable-scope fixtures and exact SRX results, fixed nested scope-state leakage in `BindingSetAssignmentInlinerOptimizer`, cross-checked all ten cases with Jena, and passed the combined 720-evaluation store/mode matrix plus all 811 evaluation-module tests.
- [ ] (in progress) Await independent semantics, integration, and performance reviews; default enablement remains explicitly out of scope.

## Surprises & Discoveries

- Observation: Both supplied archives omit the referenced query/result resources, suite index, benchmark source, generators, generated mask adapters, `ScopeLookup`, and generated algebra walker.
  Evidence: Inspection of `/tmp/rdf4j-scope-safety-plan-019f57c3` found catalogs and design prose but no `queries/*.rq`, `results/*.srx`, generator scripts, or complete compilable production tree. These assets must be authored and validated rather than copied.

- Observation: Archive pseudocode is internally inconsistent and cannot be imported as production code.
  Evidence: Guard examples and fact-index implementations use incompatible class names and APIs; effect flag layouts differ between documents; several mutation and rule references point at stale query IDs. The catalogs remain design input, while repository-native tests define the executable contract.

- Observation: RDF4J runtime `Join` is directional, even where abstract SPARQL algebra admits commutation.
  Evidence: the runtime join iterator evaluates the right operand with each mapping produced by the left operand. A right subtree can therefore consume a binding introduced by the left subtree. Production guards must model that dependency and may not import unconditional join-commutativity assumptions from the test-only equivalence module.

- Observation: `BindingSetAssignment` currently conflates possible and assured names, and parsed `Projection` nodes are not explicitly marked as outer versus subquery projections.
  Evidence: `BindingSetAssignment.getBindingNames()` delegates to an assured-name cache that unions row domains, while the SPARQL builder does not call `Projection.setSubquery(...)`. These are prerequisite correctness defects and receive focused failing tests first.

- Observation: The existing `FilterOptimizer` duplicates a filter over both operands of `Difference` (`MINUS`).
  Evidence: `FilterOptimizer.FilterRelocator#meet(Difference)` clones the filter into the left and right operands. Only the left input supplies result rows, so movement into the right operand is not semantics preserving.

- Observation: The archive's capped-shape pseudocode drops the incoming domain when the cap is first reached, which is an unsafe under-approximation.
  Evidence: its `addDistinct` calls `collapseToMayMust()` and returns before incorporating the new domain. The repository implementation folds the incoming domain into may/must before collapsing, with a focused three-shape/cap-two regression.

- Observation: Recursive fact-side analyses cannot reuse one immediate-child buffer without snapshotting it.
  Evidence: the first liveness implementation lost a `MINUS` right witness because recursive descent overwrote the parent's child list. The retained failing test now proves the snapshot fix while keeping an unused extension target dead.

- Observation: Shadow comparison belongs in `SailSourceConnection`, after optimization but before the returned iteration is interlocked.
  Evidence: this is the only production boundary where the legacy and `ENFORCE` candidate can be precompiled by the same strategy against the same open `SailDataset`. A focused Sail test now proves a divergent candidate is fatal, while capped legacy results retain streaming ownership.

- Observation: Known conservative algebra nodes and genuinely unknown extension nodes require different flags.
  Evidence: treating `Compare`, path nodes, and projection support nodes as `HAS_UNKNOWN_NODE` caused all affected `SHADOW` candidates to be skipped. The final classifier retains their hard rewrite barriers but reserves `HAS_UNKNOWN_NODE` for unclassified extension classes; all 150 sampled store/query candidates now record exact matches.

- Observation: The LMDB theme benchmark disabled the estimator immediately before waiting for estimator readiness.
  Evidence: the complete LMDB run and focused Failsafe method failed at `supportsJoinEstimation()`. Removing the contradictory test-benchmark override restored `ConfigUtil`'s enabled setting; the focused method and all nine smoke integration tests pass.

- Observation: Exact-sized initial fact arrays are required to meet the narrow-node memory budget.
  Evidence: JOL first measured 178.6 bytes per node because bootstrap reserved 25 percent spare capacity. Exact bootstrap capacity with geometric growth only for accepted rewrites brought the representative graph below the 160-byte budget while retaining incremental repair.

## Decision Log

- Decision: Ship the complete system opt-in with `OFF` as the default.
  Rationale: Enabling it by default requires independent semantics, integration, and performance reviews that are external to this implementation. `OFF` must remain the byte-for-byte legacy optimizer path and must not pay seed or fact construction cost.
  Date/Author: 2026-07-12 / Codex

- Decision: Keep `core/queryalgebra/equivalence` test-only and dependency-free from production optimizer modules.
  Rationale: It is useful as independent test support but depends on evaluation and MemoryStore; a production dependency would invert module layering and risk circular dependencies. Production legality is established by primitive guards and transactional boundary checks.
  Date/Author: 2026-07-12 / Codex

- Decision: Treat archive material as a specification aid, not trusted source code or trusted expected output.
  Rationale: The archives are incomplete and inconsistent. Every behavior-changing group therefore starts from an in-repository failing test, and exact results are manually reviewed then cross-checked with independent engines.
  Date/Author: 2026-07-12 / Codex

- Decision: Use a parser seed only as immutable provenance and rebuild mutable facts per optimization session.
  Rationale: Parser provenance answers lexical origin and boundary questions, while optimizer rewrites change current data flow. Separating them permits clones to share the immutable seed and transactions to repair mutable primitive rows safely.
  Date/Author: 2026-07-12 / Codex

- Decision: Specialize each session once into a final 64-bit, 128-bit, or wide guard implementation.
  Rationale: The hot candidate loop remains monomorphic and allocation-free for rejected queries with at most 128 symbols. Wide queries scan `W = ceil(S/64)` primitive words with row-major storage and no per-node `BitSet`.
  Date/Author: 2026-07-12 / Codex

- Decision: Preserve executable unknown nodes and functions while treating them as rewrite barriers.
  Rationale: Extensions must continue to run. A missing classification cannot safely authorize relocation, duplication, or removal, so the conservative result is denial rather than rejection of the query itself.
  Date/Author: 2026-07-12 / Codex

- Decision: Capture a rewrite boundary before constructors or setters can reparent children.
  Rationale: RDF4J algebra nodes are mutable and child setters update parent pointers. Capturing afterward loses the original relationship and prevents reliable rollback or exact before/after comparison.
  Date/Author: 2026-07-12 / Codex

## Outcomes & Retrospective

The complete scope-safety system is implemented as one opt-in change. Model/parser provenance, primitive fast facts and an independent slow analyzer, proof-backed transactional rewrites, incremental in-place repair, optimizer islands, all four modes, reason-coded telemetry, fatal exact shadow comparison, deterministic fixtures, randomized campaigns, layout tests, and performance artifacts are present. Production modules do not depend on the test-only algebra-equivalence module or on Jena, RDFLib, or JOL.

Correctness gates are green across focused and broad modules, the 600-evaluation store matrix, two SPARQL compliance suites, randomized parity campaigns, and independent Jena validation. RDFLib disagrees with RDF4J and Jena on 20 catalog cases; that disagreement is retained in `scope-safety/VALIDATION.md` instead of being hidden. Rejected guard measurements show no observed allocation events and full-scan wide cost grows linearly; JOL budgets pass and HotSpot inlines a monomorphic selected guard. JMH's normalized allocation column contains tiny profiler-noise values rather than a printable literal `0.000 B/op`, and the current whole-query `ENFORCE` benchmark is materially slower than `OFF`. Under the archive's failure policy this blocks default enablement but permits the requested experimental modes, so `OFF` remains default pending the three independent human reviews.

## Context and Orientation

RDF4J represents a query as a mutable tree of `QueryModelNode` objects. Tuple-producing nodes such as `Join`, `Projection`, `Filter`, `Difference`, `LeftJoin`, and `Extension` live in `core/queryalgebra/model`. Scalar expressions such as `Var`, function calls, and `Exists` are `ValueExpr` nodes in the same module. The SPARQL parser in `core/queryparser/sparql` translates an abstract syntax tree through `TupleExprBuilder`, and `SPARQLParser` finally wraps the result in `QueryRoot`. Production optimizers and the standard optimizer pipeline live in `core/queryalgebra/evaluation`; LMDB-specific optimizer helpers live in `core/sail/lmdb`. `SailSourceConnection` owns the low-level evaluation path where shadow comparison can use the same `SailDataset` as the returned legacy evaluation.

A *symbol* is one logical variable occurrence identity, not merely a variable-name string. A *frame* is a lexical scope such as a subquery. A *region* records correlation and evaluation boundaries such as `EXISTS` or lateral input. An *environment* records active graph or service context. A *boundary* describes which source symbols may be exported under which target names and which outer inputs are permitted. An *origin* is an immutable parser-assigned identity for a semantic algebra occurrence. A *node ID* is a mutable session-local dense index into primitive arrays. A *scope seed* is the immutable parser-produced collection of origins, symbols, frames, regions, environments, and boundaries. A *fact row* is a set of current data-flow summaries for one tuple or expression node. A *rewrite transaction* owns all mutation, rollback, clone hygiene, and fact repair for one proposed transformation.

For `N` indexed tuple/expression nodes and `S` symbols, define `W = ceil(S/64)`. Bootstrap and fact memory are `O(N x W)`. Each fact row has nine masks: `scopeOut`, `mayBind`, `mustBind`, `reads`, `captures`, `defines`, `requiredInput`, `semanticDependencies`, and `totalRequirements`. Scalars record dense frame, region, and environment IDs, result kind, effect and feature flags, row version, and a fingerprint prefilter. `scopeOut` means names visible at a node's output. `mayBind` and `mustBind` mean variables bound in at least one or every successful row. `reads`, `captures`, and `defines` describe data flow. `requiredInput` describes bindings the node cannot evaluate without. `semanticDependencies` preserves compatibility, error, ordering, and multiplicity inputs even if they are not projected. `totalRequirements` records the variables needed to prove scalar evaluation total rather than erroring.

The configuration is read once per query. Defaults are `mode=off`, `shapeCap=8`, `periodicAuditRebuild=0`, `telemetry=false`, `shadowSampleRate=0.0`, and `shadowMaxRows=10000`. Configuration parsing must be tolerant of absent values and reject malformed or unsafe values with stable tests. No runtime code generation and no new production dependency is allowed. Jena, RDFLib, JOL, and any fixture-validation helper remain test or tool dependencies only.

## Plan of Work

Milestone 1 establishes governance and baseline evidence. Preserve the root quick-build output in `maven-build.log`, create this file, run the smallest pre-change model selection through `mvnf`, and immediately persist compact reports in top-level `initial-evidence.txt`. Every subsequent behavior-changing group follows full test-first development: add the smallest failing Surefire test, run it and quote its report, then make the production edit, rerun the identical selection, and only then broaden. The living plan and this progress list must always have one and only one active item.

Milestone 2 recreates the omitted validation assets under the repository's SPARQL test resources. Author q01 through q50 from the supplied catalog and TriG dataset, exact SPARQL XML results, and a suite index whose IDs match `tests/CATALOG.md`. Repair rule and mutation references to those canonical IDs. Add an independently written finite algebra checker and a committed validation report. Expected results are reviewed as data first, then cross-checked with RDF4J, Apache Jena, and RDFLib; no expected file is generated from the optimizer implementation being tested.

Milestone 3 lands model and parser prerequisites. Add default no-op `getOptimizationTag()` and `setOptimizationTag(long)` methods to `QueryModelNode`. Store one transient packed tag in `AbstractQueryModelNode`; the high origin half survives cloning while the low mutable node-ID half becomes zero. Add transient immutable `QueryScopeSeed` sharing to `QueryRoot`. Correct `BindingSetAssignment` so declared `VALUES` header names are separate from possible (row-domain union) and assured (row-domain intersection) names, both derived caches are computed together and invalidated together, and caches do not affect equality or serialization. Keep `setBindingNames` as a compatibility alias.

The parser work uses a two-phase recorder integrated with `TupleExprBuilder`. During translation it records only semantic boundaries and declared names. After the complete algebra exists, one deterministic final traversal assigns origins and symbolic occurrences and creates `QueryScopeSeed`; `SPARQLParser` attaches it after creating `QueryRoot`. A shared declared-variable scanner supplies both wildcard expansion and seed recording so the two cannot disagree. Parsed projections call `setSubquery` explicitly. Tests cover wildcard visibility, hidden property-path variables, projection exports, `EXISTS`, `MINUS`, `Lateral`, graph, service, and nested subquery boundaries. Programmatic algebra and a seed whose origins do not match the current tree use the conservative fallback resolver. Separately, correct filter movement over `Difference` so only the left operand receives the relocated filter.

Milestone 4 builds query-local facts and the independent audit path under `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/scope`. Dense indexing uses iterative traversals and primitive arrays. `FactIndex64` stores one `long` per mask row, `FactIndex128` stores two explicitly addressed words, and `FactIndexWide` stores row-major flat `long[]` columns. One final `ScopeGuard` implementation is chosen by symbol count when a session is created. Guard methods compare words directly and never materialize temporary masks. Add a richer experimental `FunctionCharacteristicsProvider` describing query stability, side effects, invocation partitioning, row identity, failure behavior, duplication, and movement; bridge the existing `mustReturnDifferentResult()` contract. Unknown concrete nodes and shipped functions fail generated coverage tests unless explicitly classified, and unrecognized extensions become barriers.

Write the audit analyzer separately using ordinary immutable `Set` values and independent transfer logic. It may share public fact names and fixture builders, but it must not call production mask operations, fact-transfer helpers, generated adapters, or guard methods. Exact fast/slow parity is required for every algebra classification and randomized seed. Shape refinement stores at most `shapeCap` alternatives per node; exceeding the cap collapses conservatively rather than silently dropping a possible shape. Liveness includes output observation, compatibility keys, filter and extension reads, group/order dependencies, duplicate identity, errors, and RDF4J's directional join input requirements.

Milestone 5 implements proof-backed rewrites. First commit a complete rule catalog containing side conditions and stable denial reasons. Then add each transformation behind one focused primitive guard: directional join dependency ordering; filter movement; distribution involving union, optional, minus, and extension; projection pushdown and identity removal; and strict transparent-subquery flattening. Each accepted candidate is made only through a transaction-owned editor. The transaction snapshots the parent boundary before any constructor or setter can reparent nodes, clones with an explicit copy/freshen policy, performs hygienic symbol remapping, compares exact boundary records, and rolls back on any failure. A fingerprint is only a precheck; acceptance requires exact equality of every affected ancestor fact row after incremental repair versus a full recomputation. Tests include the nearest invalid form of every rule, rollback and parent restoration, clone hygiene, boundary changes, forced fingerprint collisions, and all forty named mutations.

Milestone 6 integrates optimizer islands. Introduce internal `ContextAwareQueryOptimizer.optimize(TupleExpr, Dataset, BindingSet, OptimizationSession)` without replacing `QueryOptimizer`. Its three-argument entry point creates a temporary session. Migrated optimizers retain an explicit legacy branch used by `OFF`. The standard and LMDB pipelines create one lazy session across consecutive context-aware optimizers. Read-only parent checking preserves it; any other legacy optimizer invalidates current node IDs and facts. The next context-aware optimizer rebuilds once. Migrate `UnionScopeChangeOptimizer`, projection removal, join ordering, both filter passes, scope-aware algebra distribution, and LMDB filter/join helpers. `QueryModelNormalizerOptimizer` uses transactions for proven edits and calls one explicit invalidation when an untracked legacy normalization path remains.

Milestone 7 completes modes and operational behavior. `AUDIT` executes legacy decisions and checks both fast/slow analysis and any migrated decision without changing returned plans. `ENFORCE` executes guards and transactionally accepted rewrites. `SHADOW` clones before optimizing, returns the legacy-plan evaluation, and samples candidate evaluation against the same dataset. It skips volatile, external, `SERVICE`, unsupported, and row-cap cases with reason-coded counters. It compares exact binding domains, RDF values, duplicate multiplicities, errors, and ordering where observable; any actual mismatch throws a fatal diagnostic containing a stable fingerprint and reason without dumping sensitive values. Telemetry is disabled by default and records counters rather than per-query object graphs.

Finish by adding deterministic source generation for repetitive mask adapters and algebra dispatch, but run generation at build time only as a verification of committed sources, never at runtime. Add `ScopeGuardBenchmark`, whole-query optimizer benchmarks, JOL layout checks, and allocation profiling. Run at least five warmups, ten measurements, and three forks through `scripts/run-single-benchmark.sh`. Inspect JFR and HotSpot compilation evidence on JDK 25 before asserting latency, allocation, inlining, or dispatch properties.

## Concrete Steps

Run every command from `/Users/havardottestad/Documents/Programming/rdf4j-stf`. Never use `-am` or `-q` when tests are enabled, and always use the workspace-local `.m2_repo`.

The mandatory root baseline and final quick build are:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '<repository warning/error/summary filter>'

Use the repository runner for focused tests and retain logs whenever evidence is needed:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py path/to/module --retain-logs

Immediately after the first pre-change or failing selection, persist compact evidence without deleting Surefire/Failsafe reports:

    python3 scripts/agent-evidence.py --command "<exact mvnf command>" --log logs/mvnf/<retained-log> <module>/target/surefire-reports <module>/target/failsafe-reports > initial-evidence.txt

After creating or editing Java sources, validate headers and format:

    cd scripts
    ./checkCopyrightPresent.sh
    cd ..
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

For the required benchmark protocol, use the repository helper with one exact benchmark method, at least five warmups, ten measurements, and three forks. Enable JFR for the measured confirmation run and retain its destination:

    scripts/run-single-benchmark.sh <module> <fully.qualified.ScopeGuardBenchmark.method> --enable-jfr -- -wi 5 -i 10 -f 3

At each stopping point inspect `git status --short` and the file-scoped diff. Preserve the unrelated Kùzu artifacts and every other pre-existing untracked file.

## Validation and Acceptance

Model acceptance requires focused tests proving that `VALUES (?x ?y) { (1 UNDEF) (UNDEF 2) }` declares both names, may bind both, and assures neither; row-domain caches must invalidate when rows change, while declared headers survive `UNDEF`. Optimization tags must not affect equality or serialization, cloning must retain the origin half and clear the node-ID half, and `QueryRoot` clones must share the same immutable seed.

Parser acceptance requires exact tests for outer and nested projection markers and for symbol visibility through wildcard, hidden path variables, exports, `EXISTS`, `MINUS`, lateral correlation, graph, and service boundaries. Seeded and conservative fallback analyses must agree on safe programmatic trees and conservatively deny stale or ambiguous cases. The filter regression must show a filter over `Difference` moving into only its left operand.

Analysis acceptance requires generated coverage of every concrete shipped query `QueryModelNode` (including structural projection, order, extension, and group elements) and built-in function classification. The independent Set analyzer and all three primitive fact stores must agree exactly. Random campaigns run 100,000 narrow seeds, 25,000 128-bit seeds, 10,000 wide seeds, and 10,000 multi-rewrite sequences. Every accepted rewrite must preserve original versus optimized evaluation and incremental versus full-rebuild facts.

Fixture acceptance runs all fifty catalog queries in `OFF`, `AUDIT`, and `ENFORCE` on MemoryStore, NativeStore, and LMDB and compares exact binding domains, RDF values, and duplicate counts; q49 additionally compares ordering. Sampled `SHADOW` returns the same baseline results while recording exact candidate equality. Its skip reasons must be exercised for volatile, external, service, unsupported, and oversized results, and an injected semantic mismatch must fail fatally.

Performance acceptance requires rejected 64-bit and 128-bit guards to measure `0 B/op`, wide guards to scale linearly in `W`, bootstrap and memory to stay `O(N x W)` within the archive's stated budgets, and the selected final guard to remain monomorphic. Claims require retained JMH output plus JFR/JIT evidence on JDK 25. Compile-only success is not evidence of these properties.

The final gate includes copyright validation, repository formatting, focused model/parser/evaluation/equivalence/SPARQL/LMDB tests, complete operator and function coverage, deterministic generator verification, all required corpora and randomized campaigns, the full offline root quick build, and a tracked diff containing only GH-5905 work. The production dependency tree must contain no Jena, RDFLib, JOL, archive binary, runtime code generator, or dependency on `rdf4j-queryalgebra-equivalence`.

## Idempotence and Recovery

All build, test, generator-check, fixture-validation, and benchmark commands are repeatable. Keep supplied archives and extraction under `/tmp`; never overwrite tracked sources with archive contents. When a focused test fails as intended, preserve the report before another run can overwrite it. If production code is accidentally edited before observing the required failure, revert only that specific new patch with a reviewable inverse patch, update Progress to “create failing test,” and restart the group. Do not use `git reset`, `git clean`, `git restore`, manual stash, or deletion.

A transaction failure must leave the original algebra tree, parent pointers, seed reference, and fact generation unchanged. If incremental parity fails, disable acceptance for that rule, keep the regression, rebuild from the immutable seed, and repair the invalidation range before proceeding. If offline Maven resolution alone fails, retry the exact command once without `-o`, then return to offline commands. If a benchmark or randomized campaign is too slow for a focused iteration, keep it as a final gate rather than weakening counts or assertions.

## Artifacts and Notes

The supplied archives are:

    /Users/havardottestad/Library/Mobile Documents/com~apple~CloudDocs/Downloads/SPARQL scope/rdf4j-scope-safety-complete.zip
    /Users/havardottestad/Library/Mobile Documents/com~apple~CloudDocs/Downloads/SPARQL scope/rdf4j-scope-safety-complete/latest/rdf4j-scope-safety-production/Archive.zip

The first is staged for read-only inspection at `/tmp/rdf4j-scope-safety-plan-019f57c3`. Neither archive, `__MACOSX` metadata, nor generated binaries belong in Git. The branch began this implementation phase at `8f895990f94e80c4b62b05a84d6642d2452d0453` with the completed test-only equivalence module. Pre-existing untracked Kùzu sources, plans, logs, reviews, bundles, and `.claude/` are unrelated and must remain untouched.

Evidence files produced by this plan are top-level `initial-evidence.txt`, retained `logs/mvnf/*`, Surefire/Failsafe reports under the tested modules, fixture validation reports, deterministic generation diffs, JMH results, JOL output, JFR recordings, and JIT summaries. Do not make a performance statement unless the corresponding artifact is retained and named in this document.

## Interfaces and Dependencies

In `core/queryalgebra/model`, add internal compatibility hooks while preserving existing callers:

    interface QueryModelNode {
        default long getOptimizationTag() { return 0L; }
        default void setOptimizationTag(long tag) { }
    }

    final class QueryScopeSeed implements Serializable {
        // Immutable primitive/string tables and deterministic lookup accessors.
    }

    class QueryRoot extends UnaryTupleOperator {
        QueryScopeSeed getQueryScopeSeed();
        void setQueryScopeSeed(QueryScopeSeed seed);
    }

`AbstractQueryModelNode` stores one transient packed tag. The upper 32 bits identify immutable origin and survive clone; the lower 32 bits identify the current query-local node and clear on clone. `QueryRoot` stores one transient seed reference and shares it across clones. `BindingSetAssignment` adds declared-name accessors, keeps `setBindingNames` as an alias, returns row-domain union from `getBindingNames`, and row-domain intersection from `getAssuredBindingNames`.

In `core/queryalgebra/evaluation`, add internal session-aware optimization without breaking `QueryOptimizer`:

    interface ContextAwareQueryOptimizer extends QueryOptimizer {
        void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings,
                OptimizationSession session);
    }

    final class OptimizationSession implements AutoCloseable {
        ScopeSafetyMode mode();
        ScopeGuard guard();
        ScopeFacts facts();
        RewriteTransaction beginRewrite(QueryModelNode boundary);
        void invalidate();
    }

    enum ScopeSafetyMode { OFF, AUDIT, ENFORCE, SHADOW }

    interface ScopeGuard {
        RewriteDecision check(RewriteRule rule, int sourceNodeId, int targetNodeId);
    }

The three-argument optimizer method creates a temporary session for direct callers; pipeline code supplies the shared query-local session. Migrated classes expose an explicit legacy method used by `OFF`. `RewriteDecision` contains accepted/denied state plus a stable denial reason and no allocated diagnostic payload on the hot rejected path.

Add an experimental `FunctionCharacteristicsProvider` compatible with the current `FunctionRegistry` and `mustReturnDifferentResult()` behavior. Its immutable characteristics classify stability (`CONSTANT`, `QUERY_STABLE`, `INVOCATION_SENSITIVE`, `EXTERNAL`, `UNKNOWN`), side effects, invocation partitioning, row identity, failure behavior (`TOTAL`, `MAY_ERROR`, `UNKNOWN`), duplication, and movement. Built-ins receive explicit generated coverage; custom unknown functions remain executable barriers.

Production modules may depend only on existing RDF4J modules. Apache Jena and RDFLib are independent fixture-validation tools, and JOL is benchmark/test-only. The complete feature introduces no new production dependency, no runtime source generation, and no dependency from model, parser, evaluation, Sail, or LMDB code to `core/queryalgebra/equivalence`.

Revision note (2026-07-12 20:02Z): Created the governing end-to-end plan after repository and archive inspection, recorded the successful reactor baseline, chose `OFF` as the default pending external reviews, and made the first test-first model slice the only active progress item.

Revision note (2026-07-12 20:05Z): Preserved the green model baseline and the first focused Surefire failure, then advanced the sole active item to the production implementation and matching verification of the `BindingSetAssignment` contract.

Revision note (2026-07-12 20:10Z): Recorded the completed `BindingSetAssignment` TDD slice and its focused/module green evidence, then made optimization-tag compatibility the only active item.

Revision note (2026-07-12 20:15Z): Recorded the completed packed-tag compatibility slice and its focused green evidence, then made immutable root-seed storage the only active item.

Revision note (2026-07-12 20:25Z): Recorded the immutable seed/root milestone and model-module evidence, then made the known `FilterOptimizer` `Difference` regression the only active item.

Revision note (2026-07-12 20:28Z): Recorded the test-first `FilterOptimizer`/`MINUS` correction and class-level green evidence, then made explicit parsed projection markers the only active item.

Revision note (2026-07-12 20:32Z): Recorded explicit parsed projection markers and parser-class green evidence, then made parser seed construction and attachment the only active item.

Revision note (2026-07-12 20:50Z): Recorded completed parser seed construction, shared wildcard declarations, semantic boundary coverage, and the green parser module; primitive fact analysis is now the only active item.

Revision note (2026-07-12 22:47Z): Recorded the completed query-local fact/guard milestone and 771-test evaluation-module green; transactional rewrite capture and rollback are now the only active item.

Revision note (2026-07-13 01:10Z): Recorded completed transactional rewrite and optimizer-island milestones, including the focused red/green Sail shadow boundary. Mode telemetry and audit completion are now the only active item.

Revision note (2026-07-13 01:54Z): Recorded the deterministic 50-query fixture suite and green 600-evaluation MemoryStore/NativeStore/LMDB four-mode matrix. Randomized, generated, performance, and final verification gates are now the only active item.

Revision note (2026-07-13 06:06Z): Recorded randomized and incremental parity, independent engine validation, JOL/JMH/JFR/JIT evidence, exact sampled-shadow telemetry on all three stores, broad evaluation/SPARQL tests, the complete 1,160-test LMDB module, copyright/formatting checks, and the successful final offline reactor build. The only active item is the handoff for external reviews; `OFF` remains default because whole-query performance is not yet eligible for default enablement.

Revision note (2026-07-13 06:35Z): Added a separate ten-case adversarial variable-scope suite covering nested shadowing, `VALUES UNDEF`, hidden `OPTIONAL`/`MINUS` variables, alias overlap, nested `EXISTS`, disjoint `UNION` exports, aggregates, graph scopes, and `LATERAL`. The suite exposed and now guards a binding-inliner state leak across subquery boundaries; combined store/mode coverage is 720 exact evaluations. External reviews remain the only active item.
