# Add RDF tuple parsing and private parsed-dataset queries

This is the historical implementation record from the supplied source archive.
Its environment descriptions, commits, progress, and measurements describe that
delivery, not fresh validation of this checkout. The current integration plan
is `.agent/rdf-parser-query-extensions-integration-execplan.md`, maintained under
`.agent/PLANS.md`. Current build commands are in this module's README and
`verify.sh`; historical feature commits need not be present locally.

## Purpose / Big Picture

Users can bind an LLM's RDF response as a string, turn it into SPARQL result
rows, or query it with full graph patterns without persisting it. Normal
CONSTRUCT and INSERT WHERE templates consume the results. The extension itself
never modifies persistent graphs.

## Progress

- [x] (2026-10-22) Inspect pinned main APIs, Rio options, tuple algebra, SERVICE and dataset semantics.
- [x] (2026-10-22) Implement shared bounded parser, fixed-arity tuple functions and correlated algebra rewrite.
- [x] (2026-10-22) Implement local SERVICE, explicit dataset selection, resolver delegation and Sail lifecycle.
- [x] (2026-10-22) Write 23 parser tests and 24 three-backend integration scenarios.
- [x] (2026-10-22) Parse all 13 Java source files with the available compiler frontend: zero syntax errors.
- [x] (2026-10-22) Compile production and test sources in a clean Java 25 Maven reactor.
- [x] (2026-10-22 20:34 UTC) Pass all 95 focused tests: 23 parser cases and 72 three-backend invocations; no failures, errors or skips.
- [x] (2026-10-22) Verify source/patch equivalence and ZIP integrity; record actual CI results in the delivery manifest.

## Surprises & Discoveries

RioConfig.set(setting, null) removes a setting instead of masking a JVM property.
The parser therefore overrides configuration lookup to avoid process-global
Skolemization and permissive defaults. Current main uses TripleTerm and
createTripleTerm, Resource-valued dataset graph names, and explicit LATERAL
input sets. The generic tuple adapter does not check repeated output variables
against earlier columns of the same returned row; the rewriter adds equality
filters. A malformed suffix must be observed before LIMIT can stop iteration.

The local execution environment has Java 21 but no Maven or external DNS.
Compilation and runtime verification therefore use a focused Java 25 GitHub
Actions workflow on the isolated feature branch. The first clean focused run
passed all 23 parser tests and 45 backend integration invocations. The remaining
27 failures shared one cause: the local SERVICE passed an algebra child still
attached to a temporary QueryRoot into an optimizer expecting a root. The
service now wraps its cloned subtree in an independent QueryRoot. The corrected
commit passed the complete 95-invocation focused suite in run 39849251263,
attempt 2. The first attempt of that commit encountered Maven Central HTTP 502
errors before compilation; retrying the same sources succeeded.
Prototype source was written during API investigation; no claim of a pre-change
red test execution is made.

## Decision Log

Use one opt-in Sail rather than SPIN or a grammar change. Existing RDF collection
syntax becomes TupleFunctionCall, with LATERAL dependencies preventing premature
execution. Use a local SERVICE rather than a graph-valued literal so ordinary
SPARQL graph matching remains available. Parse fully before yielding rows:
atomic document errors take precedence over constant-memory streaming. Allow
only four non-fetching formats until XML/JSON-LD loader policies exist. Do not
cache parsed graphs across invocations because fresh blank nodes are part of the
contract. These decisions were made on 2026-10-22.

## Context and Orientation

Baseline is main commit 3d292763558604affd89c7d4256e8df5720bd895, Java 25,
6.2.0-SNAPSHOT. core/sail/pom.xml registers the new module. Source is under
core/sail/rdf-parser/src/main/java/org/eclipse/rdf4j/sail/rdfparser.
RdfDocumentParser parses and limits input. PropertyFunctionRewriter turns
syntactic calls into query-algebra operators. RdfParseService evaluates a cloned
body against ParsedDatasetSource. RdfParserSail installs them while delegating
ordinary store operations. Tests reside in the parallel src/test/java directory.

## Milestones and Plan of Work

The parser milestone provides immutable RDF with fresh blank nodes, structural
limits and deterministic failures. The tuple milestone exposes three/four/one
term rows and inserts only through ordinary update templates. The SERVICE
milestone supports paths and graph joins with explicit default/named graph
separation. The verification milestone compiles the module and runs the same
observable scenarios on MemoryStore, NativeStore and LMDB. It must resolve any
API, algebra-scoping or backend failures before claiming the implementation is
verified.

## Concrete Steps

In a full checkout with this patch, run:

    mvn -B -ntp -U -pl core/sail/rdf-parser -am -DskipTests -DskipITs -Dmaven.javadoc.skip=true clean install
    mvn -B -ntp -nsu -pl core/sail/rdf-parser -Dtest=RdfDocumentParserTest,RdfParserIntegrationTest test

Only the prerequisite build uses -am and its tests are skipped. Record the
Surefire counts and failures. Use the README's bound-document examples to check
that SELECT leaves the repository empty and INSERT writes only its template.

## Validation and Acceptance

The focused suite must compile under Java 25 and pass on all three stores.
Expected total with the initial two classes is 95 invocations (23 + 24*3).
Malformed suffixes must fail even with LIMIT 1. The SERVICE default graph must
not include named statements. Independent parse calls must not share blank
nodes. Explicit rollback must remove inserted data. A failed build is not a
passing parser test; record it separately.

## Idempotence and Recovery

All production changes are additive except the module entry. Applying/reverting
this patch does not migrate storage. Experiments belong on an isolated branch,
never main. Repository transactions remain caller-owned. Do not overwrite local
untracked files when testing. Close result iterators and repositories.

## Outcomes & Retrospective

Both interfaces and their test sources are implemented. Syntax validation and
a clean Java 25 compilation have passed. Runtime verification and its exact
counts are recorded in the delivery VERIFICATION.md. The
implementation intentionally does not add an LLM client, automatic SHACL
validation, XML/JSON-LD support, global caching or empty named-graph preservation.

## Artifacts and Notes

The delivery must include complete changed files, a unified Git patch against
the pinned commit, and a manifest. Source and patch must reconstruct identical
files. Any CI source-transfer helper is transport only and not part of the
production API.

## Interfaces and Dependencies

The public entry point is RdfParserSail(Sail, RdfParseOptions). RdfDocumentParser
accepts three RDF Values and returns an immutable Model. RdfParseTupleFunction
implements TupleFunction. RdfParseService implements FederatedService.
Dependencies are existing RDF4J Rio/evaluation/Sail modules; no new third-party
runtime library is introduced.

Revision 2026-10-22: document implementation choices, bounded materialization,
local environment limitations, and successful clean Java 25 compilation plus
all 95 focused test invocations on commit 7afc4b787697dc3f6345e8d667d4083ea6aea57d.


## Optimization continuation (2026-10-22)

The continuation keeps the public interfaces and document-error/blank-node
semantics unchanged. Tuple consumers now use an insertion-ordered statement set
rather than graph indexes, and triple/quad rows no longer allocate backing
arrays. Ordinary conjunctive groups are flattened once; collection indexes are
shared within a group; dependency readiness uses producer counts. SERVICE
preparation avoids a redundant clone, its batch SPI prepares a template once,
and projection reuses rows only when no merge or filtering is necessary.

The new regression sources add 25 direct test invocations and four scenarios
parameterized over MemoryStore, NativeStore and LMDB, for 132 intended total
executions. Runtime counts, failures and eventual outcomes are recorded in the
delivery verification report, not inferred from source annotations.

A first CI attempt compiled production sources but rejected two new test visitor
overrides: TupleFunctionCall dispatches through meetOther(QueryModelNode), not a
specialized meet(TupleFunctionCall). The test visitors were corrected; tests were
not disabled. Commit 514b717b4581cec7a13e54bcae7ae47368a2d9e3 contains the correction.

RdfParserBenchmark and benchmark.sh compare the previous production commit with
the optimized implementation using the same harness and dependencies. The
benchmark covers tuple parsing, scalar and batch SERVICE, ordinary-group
rewriting and parser-call rewriting. Allocation measurements and their limits
are described in PERFORMANCE.md. No native-engine, adjacency-index, cross-store,
application-side-import or full upstream-suite performance claim follows from
these microbenchmarks.

The Java 25 workflow retains the full test reports, JMH JSON and logs, and the
formatter-produced source files. The source bundle includes both cumulative and
incremental patches, each applied to its declared baseline and checked against
all delivered file bytes. Keep the invocation-local data ownership and complete
parse-before-output rules when making further changes.


### API-bound chain regression

The new chained-call integration scenario failed on all three stores while all
95 original invocations and the other new scenarios passed. Four direct evaluator
variants initially passed because they supplied runtime bindings. Inspection of
SailSourceConnection showed that real stores optimize with API bindings and then
execute with EmptyBindingSet. The binding-assignment pass now runs on the original
SPARQL scopes before the parser rewrite introduces its dependency operators.
The direct tests now reproduce empty-runtime-binding execution when optimized.
Two further three-backend scenarios cover prebound output constraints and mixed
tuple/SERVICE chains. The suite now defines 142 invocations; execution evidence,
not this source count, determines the final reported status.


### Verified outcome

Commit 30b733258cc696d6e4396d3b002b4a1d94818cba passed a clean Java 25 build
and all 142 test invocations (zero failures, errors or skips) in GitHub Actions
run 39861588266, job 120113378306. All 14 baseline/optimized JMH comparisons
completed. Tuple allocation fell 52–61% for the measured fixtures; rewriting a
256-pattern ordinary group was 11.52 times faster. SERVICE allocation improved,
but these short runs do not establish a SERVICE time improvement. Full raw
measurements, confidence intervals and limitations are retained in the delivery.
The complete upstream suite and native-engine/adjacency performance were not run.


## Review correction pass (2026-10-23)

Executed the seven proposed review tests against the unchanged optimized
production classes on Java 25: six failed and the ill-typed-literal control
passed. Corrected native triple-term adaptation, Rio recursion and serialization
syntax boundaries, assured-input scheduling, reachable-list validation, and
explicit SERVICE configuration selection. Added regression tests on all three
backends and raw Rio depth tests independent of the wrapper preflight.

Expanded term-pattern tests also exposed repeated-variable double binding and
invalid bound-type casts in the shared native evaluator. Corrected both and
replaced wildcard scans of bound whole terms with exact component lookups while
retaining source-membership checks. Changes stay on the isolated review branch
until verification; the final delivery report records the exact tested commit,
commands and results. Complete upstream-suite and end-to-end performance testing
remain outside this focused verification.
