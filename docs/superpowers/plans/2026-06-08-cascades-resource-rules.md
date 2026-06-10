# Cascades Resource Rules Migration

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`,
and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The Cascades optimizer should get its rewrite and implementation rules from declarative resource files, not
from handwritten production classes that implement `CascadesRule`. After this work, an engineer can inspect one
YAML file per rule under `src/main/resources/cascades/rules`, see the rule id, phase, pattern, guards, emitted
template, physical properties, and proof facts, and know exactly what the optimizer is allowed to do. Java remains
the engine and intrinsic library: it may parse rule resources, compile them into `CompiledRule`, and provide named
guard/template/physical-builder intrinsics, but production rule definitions no longer live as arbitrary Java classes.

This matters because optimizer behavior has become difficult to reason about. In MEDICAL query 7 and related theme
queries, bad rule interaction made poor finite-anchor placement possible. A resource-backed rule layer gives us
inventory, validation, uniqueness checks, and a direct path to compare/cost alternatives without hidden Java-only
rules.

## Progress

- [x] (2026-06-08 20:14+02:00) Ran the required root quick install before this migration slice. The command was
  `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`;
  `maven-build.log` shows `BUILD SUCCESS` in 02:17.
- [x] (2026-06-08 20:24+02:00) Inventoried current production `CascadesRule` implementations and current Java DSL
  specs. The allowed target class `CompiledRule` exists, but standard and LMDB handwritten rules still dominate.
- [x] (2026-06-08 20:24+02:00) Added `RuleResourceLoaderTest#loadsJoinCommuteResource` and observed the expected
  red failure: `ClassNotFoundException: ...RuleResourceLoader`.
- [x] (2026-06-08 20:30+02:00) Added `RuleResourceLoader`, `RuleIntrinsicRegistry`, and
  `cascades/rules/standard/join-commute.yaml`; `RuleResourceLoaderTest` passed with 2 tests.
- [x] (2026-06-08 20:50+02:00) Changed `RuleRegistry.standardLogicalRules()` to load resource-backed standard specs
  first, then Java DSL fallback ids. `StandardRuleSpecTest` passed with 22 tests and `CascadesRuleEngineTest` passed
  with 77 tests.
- [x] (2026-06-08 21:53+02:00) Routed `LmdbCascadesRuleProvider` through the shared resource-first standard compiled
  DSL helper. `LmdbCascadesOptimizerTest#lmdbRuleRegistryIncludesScopeRemovalAndExtensionPushdown` passed.
- [ ] Convert all existing `StandardRuleSpecs` rules into resource files with parity tests.
- [ ] Convert existing `LmdbRuleSpecs` rules into resource files with parity tests.
- [ ] Add final guardrails that fail if production classes outside `CompiledRule` implement `CascadesRule`.
- [ ] Migrate handwritten standard rule containers to named DSL intrinsics plus resource files.
- [ ] Migrate handwritten LMDB nested rules to named DSL intrinsics plus resource files.
- [ ] Delete or empty the old Java rule-spec/rule-container classes after registries load resources only.
- [ ] Run focused Cascades and LMDB optimizer suites plus theme-query smoke plans.

## Surprises & Discoveries

- Observation: The current tree already has a Java DSL compiler path. `RuleRegistry.standardLogicalRules()` compiles
  `StandardRuleSpecs.allRules()` with `RuleCompiler.compileAll`, and `LmdbCascadesRuleProvider.rules()` compiles
  `LmdbRuleSpecs.physicalRules()` plus standard Java specs. This is the lowest-risk migration starting point because
  these rules already use `RuleSpec` and `CompiledRule`.
  Evidence: `RuleRegistry.java` and `LmdbCascadesRuleProvider.java` both call `RuleCompiler.compileAll(...)`.
- Observation: Production still has many direct rule implementations. `StandardCascadesRules.AbstractRule` and
  `LmdbCascadesRuleProvider.LmdbRule` implement `CascadesRule`, with many nested subclasses.
  Evidence: `rg -n "implements CascadesRule|extends StandardCascadesRules.AbstractRule|extends LmdbRule" ...`.
- Observation: Existing tests still instantiate handwritten rules directly, especially `CascadesRuleEngineTest`.
  These tests must be migrated incrementally to load rules by id from resources; otherwise the test suite itself will
  preserve the old implementation model.
  Evidence: `CascadesRuleEngineTest` contains calls like `new StandardCascadesRules.FilterConjunctPushdownRule()`.
- Observation: No YAML parser dependency is currently declared in the root or relevant module POMs.
  Evidence: `rg -n "snakeyaml|dataformat-yaml|jackson.*yaml|yaml" pom.xml core/queryalgebra/evaluation/pom.xml`
  returned no dependency hits. The first loader therefore uses a constrained local parser for the rule-resource shape.

## Decision Log

- Decision: Start by resource-loading the existing Java DSL rules before migrating handwritten rule classes.
  Rationale: `StandardRuleSpecs` and `LmdbRuleSpecs` already encode rule definitions as `RuleSpec`, so this slice can
  prove the resource format, loader validation, registry integration, and parity checks without rewriting complex rule
  semantics at the same time.
  Date/Author: 2026-06-08 / Codex.
- Decision: Resource files use a constrained YAML shape parsed by a repository-local loader and resolved through
  named intrinsic registries. The loader must not evaluate Java expressions, lambdas, reflection hooks, or arbitrary
  class names from resources.
  Rationale: Rules should be declarative data. Complex behavior is allowed only behind audited Java intrinsic names
  such as `barrierFree`, `conditionVarsSubset`, `join`, `filterConjunctPushdown`, or
  `lmdbMaterializedMinusAntiSemi`.
  Date/Author: 2026-06-08 / Codex.
- Decision: Final guardrail tests come in two layers. Early migration tests assert resource compilation and registry
  parity for rules already moved. Final contract tests assert that no production class outside `CompiledRule`
  implements `CascadesRule` and that registered rule ids each have exactly one resource file.
  Rationale: A single global red test for the final contract would stay red through most of the migration and hide
  incremental progress. Per-slice tests provide useful red/green feedback while the final guardrail enforces the
  completion contract.
  Date/Author: 2026-06-08 / Codex.

## Outcomes & Retrospective

No migration slice is complete yet. The baseline build is green, the current rule inventory is captured, and this
ExecPlan now defines the staged route. Update this section after each milestone with what changed, what remains, and
which tests/benchmarks prove the behavior.

Milestone 1 outcome, 2026-06-08 / Codex: the resource loader exists, validates the first resource, rejects unknown
guard intrinsics, and the standard registry now prefers migrated resource specs while falling back to unmigrated Java
DSL specs. LMDB now uses the same resource-first standard compiled DSL helper instead of directly compiling
`StandardRuleSpecs`. This is not the full migration: only `join-commute` is resource-backed, `StandardRuleSpecs` and
`LmdbRuleSpecs` still exist, and handwritten rule containers remain.

## Context and Orientation

`CascadesRule` is the optimizer rule interface in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRule.java`.
`CompiledRule` is the desired production implementation in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/dsl/CompiledRule.java`.
It takes a declarative `RuleSpec`, matches a `RulePattern`, evaluates `RuleGuard`s, and emits rule applications through
a `RuleTemplate`.

The existing declarative model is in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/dsl`.
Important files are:

- `RuleSpec.java`: the rule data model. It contains id, kind, phase, promise, pattern, guards, template, proof facts,
  delivered physical properties, required child physical properties, local cost, and reason.
- `RulePattern.java`: captures the input algebra shape, such as a `Join`, `Filter`, `Difference`, or a generic capture.
- `RuleGuard.java`: named predicates over captured nodes, masks, scalar variables, and physical properties.
- `RuleTemplate.java`: named rewrite/build operations that emit the replacement algebra or physical implementation.
- `RuleCompiler.java`: turns a `RuleSpec` into a `CompiledRule`.
- `StandardRuleSpecs.java`: current Java DSL standard rule definitions. These should become resource files first.
- `LmdbRuleSpecs.java`: current Java DSL LMDB rule definitions. These should become resource files after standard
  resources load.

The current handwritten standard rules live in:

- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StandardCascadesRules.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StructuralCascadesRules.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/FilterCascadesRules.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/ProjectionCascadesRules.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/SetCascadesRules.java`

The current handwritten LMDB rules are nested in
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`. The nested base class
`LmdbRule` implements `CascadesRule`; subclasses include connected join planning, access-path implementation, bound
lookup, correlated anti-exists, materialized MINUS anti-semi, materialized EXISTS semi, property-path, star scan, and
set-semantics normalization rules.

Registries currently load a mix of compiled Java DSL rules and handwritten rules:

- `RuleRegistry.standardLogicalRules()` returns compiled `StandardRuleSpecs`.
- `LmdbCascadesRuleProvider.rules(EvaluationStatistics)` returns LMDB Java specs, standard Java specs, and handwritten
  nested LMDB rules.

The new resource roots are:

- `core/queryalgebra/evaluation/src/main/resources/cascades/rules/standard/*.yaml`
- `core/sail/lmdb/src/main/resources/cascades/rules/lmdb/*.yaml`

Each resource file represents one rule id. A resource rule must be immutable data and must not directly name Java rule
classes. It may name known intrinsics, such as a guard intrinsic or an emit/template intrinsic.

## Plan of Work

First, add the resource loader in the queryalgebra evaluation module. The first version only needs to parse enough YAML
to represent rules already expressible in `StandardRuleSpecs` and `LmdbRuleSpecs`: scalar fields, pattern references,
guard intrinsic names with string arguments, template intrinsic names with string arguments, proof facts, local cost,
and physical properties. The loader should validate unique ids, known enum values, known `IrOp` names, known guards,
known templates, known physical-property names, non-empty proof facts, and one-resource-per-id.

Second, add a small set of resource files copied from `StandardRuleSpecs`, starting with `join-commute` and
`filter-values-anchor`. Change `RuleRegistry.standardLogicalRules()` so it can load standard resources. Keep
`StandardRuleSpecs` as a compatibility fallback until all standard Java specs have resource equivalents. The exact
initial registry policy is: load resource-backed standard specs, then append any Java DSL specs whose id is not yet
present, then compile. This preserves behavior while making migrated rules resource-owned.

Third, convert every `StandardRuleSpecs` method to one resource file. Add parity tests that compare ids, kind, phase,
promise, delivered properties, proof facts, and trace behavior for representative rules. Once resource parity is full,
remove the Java-spec fallback from `RuleRegistry.standardLogicalRules()` and either delete `StandardRuleSpecs` or leave
it as a test-only compatibility fixture until the deletion phase.

Fourth, convert `LmdbRuleSpecs` in the same way. Change `LmdbCascadesRuleProvider.rules()` so it loads LMDB resource
specs plus standard resource specs. Keep handwritten LMDB rules unchanged during this milestone so physical behavior is
preserved.

Fifth, add intrinsic registries for handwritten standard rules. Each migrated handwritten rule gets a resource file and
a reusable named Java intrinsic only where a generic template cannot represent it. Migrate structural simplifications
first because they are local and heavily tested: nested filter merge, filter constant, join empty/singleton, union
simplification, and projection merge. Then migrate filter/projection/set rules, then standard alternatives and
enforcers.

Sixth, migrate LMDB handwritten rules. Start with simple physical declarations such as access path, row-preserving
subplan, distinct cursor skip, and property path. Then migrate join and lookup rules. Finish with high-risk physical
alternatives: guarantee options, materialized MINUS anti-semi, materialized EXISTS semi, correlated anti-exists, and
star multi-predicate scan. For high-risk rules, preserve the existing Java implementation behind a named physical
builder intrinsic first, then extract smaller reusable intrinsics only after parity tests pass.

Seventh, turn on final guardrails. Add tests that scan production classes and fail if a production class outside
`CompiledRule` implements `CascadesRule`. Add tests that registered rules are all `CompiledRule`, every registered id
has exactly one resource file, and no duplicate ids exist across standard and LMDB resources. Delete or empty
handwritten rule containers only after these tests are green.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Before test work in a fresh session, run the root quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

For the first implementation slice, add failing tests in
`core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/dsl/RuleResourceLoaderTest.java`.
The first tests should assert that `join-commute.yaml` loads into a `RuleSpec` with id `join-commute`, kind
`TRANSFORMATION`, phase `CHEAP_LOGICAL`, promise `80`, proof facts `innerJoin` and `bagCompatible`, and compiles to a
`CompiledRule` that matches a `Join`. Add a second test that an unknown guard name throws an
`IllegalArgumentException` mentioning the unknown guard. Run:

    python3 .codex/skills/mvnf/scripts/mvnf.py RuleResourceLoaderTest#loadsJoinCommuteResource --retain-logs

Expected before implementation: test fails because `RuleResourceLoader` or the resource file does not exist.

Implement the first resource loader slice by creating:

- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/dsl/RuleResourceLoader.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/dsl/RuleIntrinsicRegistry.java`
- `core/queryalgebra/evaluation/src/main/resources/cascades/rules/standard/join-commute.yaml`

The loader API should be:

    public final class RuleResourceLoader {
        public static List<RuleSpec> loadStandardRules();
        public static List<RuleSpec> loadRules(ClassLoader classLoader, String resourceRoot);
        public static RuleSpec loadRule(InputStream stream, String resourceName);
    }

The intrinsic registry should be deliberately small at first. It must resolve the names used by `join-commute.yaml`:
`join`, `capture`, `ref`, and guard `barrierFree`. Add names only when a resource needs them.

After the first implementation, rerun:

    python3 .codex/skills/mvnf/scripts/mvnf.py RuleResourceLoaderTest --retain-logs

Then add registry integration tests to
`core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/dsl/StandardRuleSpecTest.java`.
The test should assert that the standard registry contains a `CompiledRule` for `join-commute` sourced from resources
and that duplicate ids are rejected when resource and Java fallback define the same id without explicit de-duplication.

For each later standard rule migration, follow this loop:

1. Add one YAML resource file.
2. Add or update one focused test that loads that resource by id and compares behavior with the previous Java spec or
   handwritten rule.
3. Run the focused test and the matching existing `CascadesRuleEngineTest` methods.
4. Remove the corresponding Java-spec registration only after the resource test is green.

For LMDB rule migration, follow the same loop with `LmdbRuleSpecTest`, then run focused LMDB tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRuleSpecTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbIndexAwareJoinOrderPlanningTest --retain-logs

Do not use Maven `-am` for tests. Do not use Maven `-q` for tests. Prefer `mvnf`; it performs the required root quick
install before verification.

## Validation and Acceptance

The first milestone is accepted when `RuleResourceLoaderTest` passes and the standard registry can load at least one
standard rule from `core/queryalgebra/evaluation/src/main/resources/cascades/rules/standard`.

The standard-resource milestone is accepted when all current `StandardRuleSpecs` ids have exactly one corresponding
standard resource file, `RuleRegistry.standardLogicalRules()` loads resources only, and these commands pass:

    python3 .codex/skills/mvnf/scripts/mvnf.py StandardRuleSpecTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs

The LMDB-resource milestone is accepted when all current `LmdbRuleSpecs` ids have exactly one corresponding LMDB
resource file, `LmdbCascadesRuleProvider.rules()` loads LMDB resource specs plus standard resource specs, and these
commands pass:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRuleSpecTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs

The final migration is accepted only when:

- No production class except `CompiledRule` implements `CascadesRule`.
- All registered production rules are `CompiledRule`.
- Every registered rule id has exactly one resource file.
- There are no duplicate rule ids across standard and LMDB resources.
- The focused Cascades unit suite passes.
- The LMDB optimizer focused suite passes.
- MEDICAL q7, LIBRARY q7, and SOCIAL q5 smoke plans still reach the same or better alternatives than before this
  migration.

## Idempotence and Recovery

This migration is staged so each resource file is additive until its matching Java registration is removed. If a resource
file fails to parse, delete or fix only that file and rerun the focused loader test. If a registry parity test fails,
temporarily keep the Java fallback and record the mismatch in `Surprises & Discoveries`. Do not delete handwritten rule
classes until all resources and guardrails are green.

The worktree is already dirty with optimizer changes from the current branch. Treat those as user/previous-agent work.
Do not revert them. Before each edit group, check `git status --short --untracked-files=no` and keep changes small.

## Artifacts and Notes

Initial build evidence is stored in `initial-evidence.txt` and full build output is in `maven-build.log`.

Relevant current inventory:

    core/queryalgebra/evaluation/.../cascades/dsl/CompiledRule.java implements CascadesRule
    core/queryalgebra/evaluation/.../cascades/StandardCascadesRules.java has AbstractRule implements CascadesRule
    core/sail/lmdb/.../LmdbCascadesRuleProvider.java has LmdbRule implements CascadesRule
    core/queryalgebra/evaluation/.../cascades/dsl/StandardRuleSpecs.java contains existing Java DSL standard specs
    core/queryalgebra/evaluation/.../cascades/dsl/LmdbRuleSpecs.java contains existing Java DSL LMDB specs

Expected first resource example:

    id: join-commute
    kind: TRANSFORMATION
    phase: CHEAP_LOGICAL
    promise: 80
    match:
      op: Join
      as: j
      inputs:
        - capture: left
        - capture: right
    guards:
      - name: barrierFree
        args: [j]
    emit:
      name: join
      args:
        - ref: right
        - ref: left
    proof:
      - innerJoin
      - bagCompatible
    reason: JOIN inputs commute under SPARQL bag semantics

## Interfaces and Dependencies

The resource loader should not add a third-party dependency unless the repository already has a YAML parser available
offline in `.m2_repo`. If no parser is already available, implement a tiny constrained parser for the rule-resource
shape used here, and keep it private to `RuleResourceLoader`. The parser only needs maps, lists, strings, integers,
and booleans for this migration. It must reject unknown fields, unknown intrinsic names, duplicate ids, and malformed
nesting with explicit `IllegalArgumentException`s.

The key Java interfaces after the first slice are:

    public final class RuleResourceLoader {
        public static List<RuleSpec> loadStandardRules();
        public static List<RuleSpec> loadRules(ClassLoader classLoader, String resourceRoot);
        public static RuleSpec loadRule(InputStream stream, String resourceName);
    }

    public final class RuleIntrinsicRegistry {
        public RulePattern pattern(Map<String, Object> yaml);
        public RuleGuard guard(String name, List<String> args);
        public RuleTemplate template(String name, List<Object> args);
        public PhysicalProperties physicalProperties(String name);
    }

Resource loading should compile to the existing `RuleSpec` and `CompiledRule` model. Do not create a second rule engine.
Do not let resources name Java classes directly. Do not mutate query semantics while migrating rule definitions.

Revision note, 2026-06-08 / Codex: Created the plan from the user-supplied completion contract and current repository
inventory. The plan starts with resource-loading existing Java DSL specs because that validates the rule-resource path
without entangling it with high-risk LMDB physical-rule migration.
